package com.oneplus.threekey;

import static com.oneplus.Actions.TRI_STATE_KEY_INTENT;
import static com.oneplus.Actions.TRI_STATE_KEY_BOOT_INTENT;
import static com.oneplus.Actions.TRI_STATE_KEY_INTENT_EXTRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.util.Slog;

public class ThreeKeyHw {
    private static final String TAG = "ThreeKeyHw";
    private static final boolean debug = true;

    private static final String UDEV_NAME_THREEKEY = "tri-state-key";
    private static int ThreeKeyModeState = 0;
    private UEventInfo mThreeKeyUEventInfo = new UEventInfo(UDEV_NAME_THREEKEY);
    private OemUEventObserver mOemUEventObserver = new OemUEventObserver();
    private Context mContext;

    public ThreeKeyHw(Context context) {
        mContext = context;
    }

    public int getState() throws ThreeKeyUnsupportException {
        if(!isSupportThreeKey()) {
            throw new ThreeKeyUnsupportException();
        }

        int threeKeyState = -1;
        try {
            char[] buffer = new char[1024];
            FileReader file = new FileReader(mThreeKeyUEventInfo.getSwitchStatePath());
            int len = file.read(buffer, 0, 1024);
            file.close();
            threeKeyState = Integer.valueOf(new String(buffer, 0, len).trim());
        } catch (Exception e) {
            // it should not happen here,we has check the file in isSupportThreeKey()
            // but for safe we give a warning log and throw the exception
            Slog.e(TAG,mThreeKeyUEventInfo.getSwitchStatePath() +
                "not found while attempting to get switch state");
            throw new ThreeKeyUnsupportException();
        }
        return threeKeyState;
    }

    public boolean isSupportThreeKey() {
        return true;
    }

    public void init() {
        mOemUEventObserver.startMonitor();
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


    class OemUEventObserver extends UEventObserver {

        void startMonitor() {
             this.startObserving("DEVPATH=" + mThreeKeyUEventInfo.getDevPath());
        }

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (debug) {
                Slog.d(TAG, "OEM UEVENT: " + event.toString());
            }

            try {
                String devPath = event.get("DEVPATH");
                String name = event.get("SWITCH_NAME");
                int state = Integer.parseInt(event.get("SWITCH_STATE"));
                sendBroadcastForZenModeChanged(state);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Could not parse switch state from event " + event);
            }
        }
    }

    public static class ThreeKeyUnsupportException extends Exception
    {
        public ThreeKeyUnsupportException()
        {
            super();
        }
    }

    private void sendBroadcastForZenModeChanged(int state) {
        Intent intentZenMode = new Intent(TRI_STATE_KEY_INTENT);
        intentZenMode.putExtra(TRI_STATE_KEY_INTENT_EXTRA, state);
        mContext.sendBroadcastAsUser(intentZenMode, UserHandle.ALL);
    }
}
