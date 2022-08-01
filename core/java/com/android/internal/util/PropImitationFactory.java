/*
 * Copyright (C) 2020 The Pixel Experience Project
 * Copyright (C) 2022 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.android.internal.R;

public class PropImitationFactory {

    private static final String TAG = "PropImitationFactory";
    private static final boolean DEBUG = false;

    private static volatile boolean mIsCallerPackageGms;

    public static final String USER_PACKAGE_LIST = "vendor.property.imitation.packagelist";

    public static final String PACKAGE_GMS = "com.google.android.gms";
    public static final String PACKAGE_SETTINGS_INTEL = "com.google.android.settings.intelligence";

    private final Map<String, Object> mStockProperties;
    private final Map<String, Object> mFeatureProperties;
    private Set<String> mSkipPackages = new HashSet<>();
    private Set<String> mFeaturePackages = new HashSet<>();
    private Set<String> mStockPackages = new HashSet<>();

    private Context mContext;
    private String mPackagePrefix;
    private boolean mSkipImitation;

    public PropImitationFactory(Context context) {
        mContext = context;

        // List of devices that do not need imitation
        String[] supportedDeviceList = context.getResources().getStringArray(R.array.config_propImitationSkipDeviceList);
        mSkipImitation = Arrays.asList(supportedDeviceList).contains(SystemProperties.get("ro.product.system.device"));

        // Device information used for GMS
        mStockProperties = new HashMap<>();
        final String[] stockProps = context.getResources().getStringArray(R.array.config_propImitationStockValues);
        if (stockProps.length > 0) {
            mStockProperties.put("BRAND", stockProps[0]);
            mStockProperties.put("MANUFACTURER", stockProps[1]);
            mStockProperties.put("DEVICE", stockProps[2]);
            mStockProperties.put("PRODUCT", stockProps[3]);
            mStockProperties.put("MODEL", stockProps[4]);
            mStockProperties.put("FINGERPRINT", stockProps[5]);
        }

        // Device information used to imitate
        mFeatureProperties = new HashMap<>();
        final String[] featureProps = context.getResources().getStringArray(R.array.config_propImitationFeatureValues);
        if (featureProps.length > 0) {
            mFeatureProperties.put("BRAND", featureProps[0]);
            mFeatureProperties.put("MANUFACTURER", featureProps[1]);
            mFeatureProperties.put("DEVICE", featureProps[2]);
            mFeatureProperties.put("PRODUCT", featureProps[3]);
            mFeatureProperties.put("MODEL", featureProps[4]);
            mFeatureProperties.put("FINGERPRINT", featureProps[5]);
        }

        // List of packages to process for imitation
        String[] stockPackages = context.getResources().getStringArray(R.array.config_propImitationStockPackages);
        if (stockPackages.length > 0) {
            mStockPackages = new HashSet<>(Arrays.asList(stockPackages));
        }

        String[] featurePackages = context.getResources().getStringArray(R.array.config_propImitationFeaturePackages);
        if (featurePackages.length > 0) {
            mFeaturePackages = new HashSet<>(Arrays.asList(featurePackages));
        }

        String[] skipPackages = context.getResources().getStringArray(R.array.config_propImitationSkipPackages);
        if (skipPackages.length > 0) {
            mSkipPackages = new HashSet<>(Arrays.asList(skipPackages));
        }

        // Get the prefix of applications to filter. If empty, no apps will be processed for imitation.
        mPackagePrefix = context.getResources().getString(R.string.config_propImitationPackagePrefix);
    }

    private void setPackageProps(String packageName) {
        if (!TextUtils.isEmpty(mPackagePrefix)) {
            if (packageName.startsWith(mPackagePrefix)) {
                if (!mStockProperties.isEmpty()) {
                    for (Map.Entry<String, Object> prop : mStockProperties.entrySet()) {
                        if (mSkipPackages.contains(packageName))
                            continue;
                        if (mFeaturePackages.contains(packageName))
                            continue;
                        if (DEBUG) Log.d(TAG, "Defining " + prop.getKey() + " prop for: " + packageName);
                        setPropValue(prop.getKey(), prop.getValue());
                    }
                }
            }
        }

        if (mStockPackages.contains(packageName)) {
            if (!mStockProperties.isEmpty()) {
                for (Map.Entry<String, Object> prop : mStockProperties.entrySet()) {
                    if (mSkipPackages.contains(packageName))
                        continue;
                    if (mFeaturePackages.contains(packageName))
                        continue;
                    if (DEBUG) Log.d(TAG, "Defining " + prop.getKey() + " prop for: " + packageName);
                    setPropValue(prop.getKey(), prop.getValue());
                }
            }
        }

        if (mFeaturePackages.contains(packageName)) {
            if (!mFeatureProperties.isEmpty()) {
                for (Map.Entry<String, Object> prop : mFeatureProperties.entrySet()) {
                    if (mSkipPackages.contains(packageName))
                        continue;
                    if (DEBUG) Log.d(TAG, "Defining " + prop.getKey() + " prop for: " + packageName);
                    setPropValue(prop.getKey(), prop.getValue());
                }
            }
        }
    }

    public void setProps(Application app) {
        final String packageName = app.getPackageName();
        final String processName = app.getProcessName();

        if (packageName == null) {
            return;
        }

        mIsCallerPackageGms = packageName.equals(PACKAGE_GMS) &&
                processName.equals(PACKAGE_GMS + ".unstable");
        if (mIsCallerPackageGms) {
            if (mSkipImitation) {
                setPropValue("MODEL", Build.MODEL + " ");
            } else if (!mStockProperties.isEmpty()) {
                for (Map.Entry<String, Object> prop : mStockProperties.entrySet()) {
                    if (DEBUG) Log.d(TAG, "Defining " + prop.getKey() + " prop for: " + packageName);
                    if (prop.getKey().equals("MODEL")) {
                        setPropValue(prop.getKey(), prop.getValue().toString() + " ");
                    } else {
                        setPropValue(prop.getKey(), prop.getValue());
                    }
                }
            }
            return;
        }

        if (!mSkipImitation) {
            setPackageProps(packageName);
        }

        if (packageName.equals(PACKAGE_SETTINGS_INTEL)) {
            setPropValue("FINGERPRINT", Build.VERSION.INCREMENTAL);
        }
    }

    private void setPropValue(String key, Object value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (mIsCallerPackageGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }
    }
}
