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

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS = PACKAGE_GMS + ".unstable";

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;

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

        if (sIsGms || sIsFinsky) {
            if (!Build.IS_USER) {
                setBuildField("TYPE", "user");
            }
            if (!Build.TAGS.equals("release-keys")) {
                setBuildField("TAGS", "release-keys");
            }
        }

        if (sIsGms) {
            final boolean attestationEnabled =
                    context.getResources().getBoolean(R.bool.config_deviceUseAttestationHooks);
            if (attestationEnabled) {
                /* Set certified properties for GMSCore if supplied */
                setBuildField("FINGERPRINT", "google/marlin/marlin:7.1.2/NJH47F/4146041:user/release-keys");
                setBuildField("PRODUCT", "marlin");
                setBuildField("DEVICE", "marlin");
                setBuildField("MODEL", "Pixel XL");
                setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.N_MR1);
            } else {
                // Alter model name to avoid hardware attestation enforcement
                setBuildField("MODEL", Build.MODEL + "\u200b");
                if (Build.VERSION.DEVICE_INITIAL_SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.S_V2);
                }
            }
        }
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
