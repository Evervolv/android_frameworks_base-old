/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.gmscompat;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Binder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Calendar;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PACKAGE_PHOTOS = "com.google.android.apps.photos";
    private static final String PROCESS_GMS = PACKAGE_GMS + ".unstable";
    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            PACKAGE_GMS  + "/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static final String NEXUS_EXCLUSIVE_FEATURE = "com.google.android.apps.photos.NEXUS_PRELOAD";

    private static final int PIXEL_BASE_FEATURE_YEAR = 2016;
    private static final String PIXEL_EXCLUSIVE_FEATURES[] = {
        "com.google.android.feature.PIXEL_%s_PRELOAD",
        "com.google.android.feature.PIXEL_%s_MIDYEAR_PRELOAD",
        "com.google.android.feature.PIXEL_%s_EXPERIENCE",
        "com.google.android.feature.PIXEL_%s_MIDYEAR_EXPERIENCE"
    };

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;
    private static volatile boolean sIsPhotos = false;

    private AttestationHooks() { }

    public static void initApplicationBeforeOnCreate(Context context) {
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName)) {
            Log.e(TAG, "Null package or process name");
            return;
        }

        sIsGms = PACKAGE_GMS.equals(packageName)
                && PROCESS_GMS.equals(processName);
        sIsFinsky = PACKAGE_FINSKY.equals(packageName);
        sIsPhotos = PACKAGE_PHOTOS.equals(packageName);

        boolean isSystemApp = false;
        PackageManager pm = context.getPackageManager();
        try {
            isSystemApp = (pm.getApplicationInfo(packageName, 0).flags
                    & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (NameNotFoundException e) {
            isSystemApp = false;
        }

        boolean overrideSystemApp = (isSystemApp
                && packageName.startsWith("com.google."))
                && sIsGms || sIsFinsky;
        if (!isSystemApp || overrideSystemApp) {
            if (!Build.IS_USER) {
                setBuildField("TYPE", "user");
            }
            if (!Build.TAGS.equals("release-keys")) {
                setBuildField("TAGS", "release-keys");
            }
        }

        if (sIsGms || sIsPhotos) {
            final boolean skipCurrentActivity = sIsGms && isGmsAccountActivity();
            final TaskStackListener taskStackListener = new TaskStackListener() {
                @Override
                public void onTaskStackChanged() {
                    final boolean skipActivity = isGmsAccountActivity();
                    if (skipActivity ^ skipCurrentActivity) {
                        Process.killProcess(Process.myPid());
                    }
                }
            };

            final boolean attestationEnabled =
                    context.getResources().getBoolean(R.bool.config_deviceUseAttestationHooks);
            if (!skipCurrentActivity || sIsPhotos) {
                if (attestationEnabled) {
                    /* Set certified properties for GMSCore if supplied */
                    setBuildField("FINGERPRINT", "google/bullhead/bullhead:8.0.0/OPR6.170623.013/4283548:user/release-keys");
                    setBuildField("PRODUCT", "bullhead");
                    setBuildField("DEVICE", "bullhead");
                    setBuildField("MODEL", "Nexus 5X");
                    setBuildField("BRAND", "google");
                    setBuildField("MANUFACTURER", "LGE");
                    setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.N);
                } else {
                    // Alter model name to avoid hardware attestation enforcement
                    setBuildField("MODEL", Build.MODEL + "\u200b");
                    if (Build.VERSION.DEVICE_INITIAL_SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.S_V2);
                    }
                }
            }

            if (sIsGms) {
                try {
                    ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
                } catch (Exception e) { }
            }
        } else if (packageName.equals("com.google.android.settings.intelligence")) {
            // Set proper indexing fingerprint
            setBuildField("FINGERPRINT", Build.VERSION.INCREMENTAL);
        }
    }

    public static boolean hasSystemFeature(String name, boolean hasFeature) {
        if (!sIsPhotos) {
            return hasFeature;
        }

        if (hasFeature) {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            for (int year = PIXEL_BASE_FEATURE_YEAR; year <= currentYear; year++) {
                for (String feature : PIXEL_EXCLUSIVE_FEATURES) {
                    final String formattedFeature = feature.replace("%s", String.valueOf(year));
                    if (name.equalsIgnoreCase(formattedFeature)) {
                        return false;
                    }
                }
            }
        }
        return hasFeature || name.equalsIgnoreCase(NEXUS_EXCLUSIVE_FEATURE);
    }

    private static boolean isGmsAccountActivity() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) { }
        return false;
    }

    public static boolean isPackageUidGms(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final int gmsUid;
        try {
            gmsUid = context.getPackageManager().getApplicationInfo(PACKAGE_GMS, 0).uid;
        } catch (Exception e) {
            return false;
        }
        return gmsUid == callingUid;
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            throw new UnsupportedOperationException();
        }
    }

    private static void setBuildField(String key, String value) {
        try {
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionField(String key, Integer value) {
        try {
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build.VERSION." + key, e);
        }
    }
}
