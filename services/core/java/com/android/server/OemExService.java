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

import static android.provider.Settings.Global.ZEN_MODE_ALARMS;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_OFF;
import static com.oneplus.Actions.TRI_STATE_KEY_INTENT;
import static com.oneplus.Actions.TRI_STATE_KEY_BOOT_INTENT;
import static com.oneplus.Actions.TRI_STATE_KEY_INTENT_EXTRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.util.Slog;

import com.android.server.input.InputManagerService;
import com.oneplus.os.IOemExService;


public final class OemExService extends IOemExService.Stub {
    private static final String TAG = "OemExService";
    static final boolean DEBUG = true;
    static final boolean DEBUG_OEM_OBSERVER = DEBUG | false;
    static final boolean DEBUG_OEM_ZENMODE = DEBUG | false;

    // For message handler
    private static final int MSG_SYSTEM_READY = 1;
    private static final int MSG_ZENMODE = 2;

    // For udevice name
    private static final String UDEV_NAME_ZENMODE = "tri-state-key";

    // For H2 Zen mode state
    private static final int ZENMODE_NO_INTERRUPTIONS = 1;
    private static final int ZENMODE_IMPORTANT_INTERRUPTIONS = 2;
    private static final int ZENMODE_OFF = 3;

    private final Object mLock = new Object();
    private Context mContext;

    // held while there is a pending state change.
    private final WakeLock mWakeLock;

    private static int sZenModeState = 0;

    // Observe the oem uevent
    private final OemUEventObserver mObserver;

    private volatile boolean mSystemReady = false;

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
                case MSG_ZENMODE:
                    handleZenModeChanged(newState, oldState);
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    break;
            }

        }
    };

    public OemExService(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mContext = context;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OemExService");
        mObserver = new OemUEventObserver();

    }

    public void systemRunning() {
        synchronized (mLock) {
            // This wakelock will be released by handler
            mWakeLock.acquire();

            // Use message to aovid blocking system server
            Message msg = mHandler.obtainMessage(MSG_SYSTEM_READY, 0, 0, null);
            mHandler.sendMessage(msg);
        }
    }

    private void onSystemReady() {
        Slog.d(TAG, "systemReady");
        mSystemReady = true;

        mObserver.init();

        // Send initial states
        sendBroadcastForZenModeChanged(sZenModeState);
    }

    private void sendBroadcastForZenModeChanged(int state) {
        Intent intentZenMode;
        intentZenMode = new Intent(TRI_STATE_KEY_INTENT);
        intentZenMode.putExtra(TRI_STATE_KEY_INTENT_EXTRA, state);
        mContext.sendBroadcastAsUser(intentZenMode, UserHandle.ALL);
    }

    class OemUEventObserver extends UEventObserver {
        private final List<UEventInfo> mUEventInfo;

        public OemUEventObserver() {
            mUEventInfo = makeObservedUEventList();
        }

        void init() {
            synchronized (mLock) {
                if (DEBUG_OEM_OBSERVER) {
                    Slog.d(TAG, "init()");
                }
                char[] buffer = new char[1024];

                for (int i=0; i<mUEventInfo.size(); ++i) {
                    UEventInfo uei = mUEventInfo.get(i);
                    try {
                        int curState;
                        FileReader file = new FileReader(uei.getSwitchStatePath());
                        int len = file.read(buffer, 0, 1024);
                        file.close();
                        curState = Integer.valueOf(new String(buffer, 0, len).trim());

                        if (curState > 0) {
                            // Be sure that the initial state is set correctly
                            // Otherwise we get a state change that is not real
                            sZenModeState = curState;
                            updateStateLocked(uei.getDevPath(), uei.getDevName(), curState);
                        }

                    } catch(FileNotFoundException e) {
                        Slog.w(TAG, uei.getSwitchStatePath() +
                                "not found while attempting to determine initial switch state.");
                    } catch (Exception e) {
                        Slog.e(TAG, "", e);
                    }
                }
            }


            // Observer OEM UEvent devices
            for (int i=0; i < mUEventInfo.size(); ++i) {
                UEventInfo uei = mUEventInfo.get(i);
                startObserving("DEVPATH=" + uei.getDevPath());
            }
        }

        private List<UEventInfo> makeObservedUEventList() {
            List<UEventInfo> retVal = new ArrayList<UEventInfo>();
            UEventInfo uei;

            // Monitor zen mode state
            uei = new UEventInfo(UDEV_NAME_ZENMODE);
            if (uei.checkSwitchExists()) {
                retVal.add(uei);
            } else {
                Slog.w(TAG, "This kernel does not have tri-key support");
            }

            return retVal;
        }

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (DEBUG_OEM_OBSERVER) {
                Slog.d(TAG, "OEM UEVENT: " + event.toString());
            }

            try {
                String devPath = event.get("DEVPATH");
                String name = event.get("SWITCH_NAME");
                int state = Integer.parseInt(event.get("SWITCH_STATE"));
                synchronized(mLock) {
                    updateStateLocked(devPath, name, state);
                }
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Could not parse switch state from event " + event);
            }


        }

        private void updateStateLocked(String devPath, String name, int state) {
            if (!mSystemReady) {
                return;
            }

            Message msg;

            // This wakelock will be released by handler
            mWakeLock.acquire();
            switch(name) {
                case UDEV_NAME_ZENMODE:
                    msg = mHandler.obtainMessage(MSG_ZENMODE, state, sZenModeState, null);
                    /* trigger state update only if it changes */
                    if (state != sZenModeState) {
                        sZenModeState = state;
                        mHandler.sendMessage(msg);
                    } else {
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                    }
                    break;
                default:
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    break;
            }
        }

        private final class UEventInfo {
            private final String mDevName;

            public UEventInfo(String devName) {
                mDevName = devName;
            }

            public String getDevName() {
                return mDevName;
            }

            public String getDevPath() {
                return String.format(Locale.US, "/devices/virtual/switch/%s", mDevName);
            }

            public String getSwitchStatePath() {
                return String.format(Locale.US, "/sys/class/switch/%s/state", mDevName);
            }

            public boolean checkSwitchExists() {
                File f = new File(getSwitchStatePath());
                return f.exists();
            }
        }
    }

    void handleZenModeChanged(int newState, int oldState) {
        if (DEBUG_OEM_ZENMODE) {
            Slog.d(TAG, "handleZenModeChanged: " + "newState: " + newState
                    + ", oldState: " + oldState);
        }

        // Update Zen mode configuration to notification manager
        ZenModeConfig configZenMode = getZenModeConfig();
        configZenMode.allowMessages = true;
        configZenMode.allowEvents = true;
        configZenMode.allowCalls = true;

        switch (newState) {
            case ZEN_MODE_ALARMS:
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.ZEN_MODE, ZEN_MODE_ALARMS);
                configZenMode.allowCallsFrom = ZenModeConfig.SOURCE_CONTACT;
                configZenMode.allowMessagesFrom = ZenModeConfig.SOURCE_CONTACT;
                break;
            case ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                configZenMode.allowCallsFrom = ZenModeConfig.SOURCE_STAR;
                configZenMode.allowMessagesFrom = ZenModeConfig.SOURCE_STAR;
                break;
            case ZEN_MODE_OFF:
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.ZEN_MODE, ZEN_MODE_OFF);
                configZenMode.allowCallsFrom = ZenModeConfig.SOURCE_ANYONE;
                configZenMode.allowMessagesFrom = ZenModeConfig.SOURCE_ANYONE;
                break;
            default:
                break;
        }

        if (oldState != 0) {
            sendBroadcastForZenModeChanged(newState);
        }
    }

    private ZenModeConfig getZenModeConfig() {
        final NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            return nm.getZenModeConfig();
        } catch (Exception e) {
            Slog.w(TAG, "Error calling NoMan", e);
            return new ZenModeConfig();
        }
    }
}
