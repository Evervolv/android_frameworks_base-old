/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2013-2016, OnePlus Technology Co., Ltd.
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

package com.android.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.os.BackgroundThread;
import com.android.server.input.InputManagerService;
import com.android.server.wm.WindowManagerService;

import com.oneplus.os.IOemExService;
import com.oneplus.os.IThreeKeyPolicy;
import com.oneplus.threekey.ThreeKey;
import com.oneplus.threekey.ThreeKeyAudioPolicy;
import com.oneplus.threekey.ThreeKeyBase;
import com.oneplus.threekey.ThreeKeyHw;
import com.oneplus.threekey.ThreeKeyHw.ThreeKeyUnsupportException;
import com.oneplus.threekey.ThreeKeyVibratorPolicy;

import java.io.File;

public final class OemExService extends IOemExService.Stub {
    private static final String TAG = "OemExService";

    // For message handler
    private static final int MSG_SYSTEM_READY = 1;

    private final Object mLock = new Object();
    private Context mContext;

    // held while there is a pending state change.
    private final WakeLock mWakeLock;

    private volatile boolean mSystemReady = false;

    private ThreeKeyHw mThreeKeyHw;
    private ThreeKey mThreeKey;
    private IThreeKeyPolicy mThreeKeyAudioPolicy;
    private IThreeKeyPolicy mThreeKeyVibratorPolicy;

    private final Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message msg) {
            int newState = msg.arg1;
            int oldState = msg.arg2;

            switch (msg.what) {
                case MSG_SYSTEM_READY:
                    onSystemReady();
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    public OemExService(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mContext = context;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OemExService");
    }

    public void systemRunning() {
        synchronized (mLock) {
            // This wakelock will be released by handler
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }

            // Use message to aovid blocking system server
            Message msg = mHandler.obtainMessage(MSG_SYSTEM_READY, 0, 0, null);
            mHandler.sendMessage(msg);
        }
    }

    private void onSystemReady() {
        Slog.d(TAG, "systemReady");
        mSystemReady = true;

        mThreeKeyHw = new ThreeKeyHw(mContext);
        // it happen in 14001 such device has no mThreeKey
        // do some thing instead with a software-mThreeKey
        if(mThreeKeyHw.isSupportThreeKey()) {
            mThreeKeyHw.init();

            mThreeKeyAudioPolicy = new ThreeKeyAudioPolicy(mContext);
            mThreeKeyVibratorPolicy = new ThreeKeyVibratorPolicy(mContext);

            try {
                mThreeKey= new ThreeKey(mContext);
                mThreeKey.addThreeKeyPolicy(mThreeKeyAudioPolicy);
                mThreeKey.addThreeKeyPolicy(mThreeKeyVibratorPolicy);
                mThreeKey.init(mThreeKeyHw.getState());
            } catch (ThreeKeyUnsupportException e) {
                Slog.e(TAG,"device is not support mThreeKey");
                mThreeKey = null;
            }
        }
    }

    public void disableDefaultThreeKey() {
        mThreeKey.removeThreeKeyPolicy(mThreeKeyAudioPolicy);
        Slog.d(TAG,"[disableDefaultThreeKey]");
    }

    public void enalbeDefaultThreeKey() {
        mThreeKey.addThreeKeyPolicy(mThreeKeyAudioPolicy);
        Slog.d(TAG,"[enableDefaultThreeKey]");
    }

    public void addThreeKeyPolicy(IThreeKeyPolicy policy) {
        Slog.d(TAG,"[setThreeKeyPolicy]");
        mThreeKey.addThreeKeyPolicy(policy);
    }

    public void removeThreeKeyPolicy(IThreeKeyPolicy policy) {
        Slog.d(TAG,"[removeThreeKeyPolicy]");
        mThreeKey.removeThreeKeyPolicy(policy);
    }

    public void resetThreeKey() {
        Slog.d(TAG,"[resetThreeKey]");
        mThreeKey.reset();
    }

    public int getThreeKeyStatus() {
	Slog.d(TAG,"[getThreeKeyStatus]");
        try {
            return mThreeKeyHw.getState();
        } catch (ThreeKeyUnsupportException e) {
            Slog.e(TAG,"system unsupport for mThreeKey");
        }
        return 0;
    }
}
