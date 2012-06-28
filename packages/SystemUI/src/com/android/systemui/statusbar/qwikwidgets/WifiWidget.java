package com.android.systemui.statusbar.qwikwidgets;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.R;

public class WifiWidget extends ToggleOnly {

    public static final String TAG = "WifiWidget";

    public WifiWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_WIFI;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_wifi_title;
            mIconId = R.drawable.widget_wifi_icon;
        }
    }

    private static final StateTracker sWifiState = new WifiStateTracker();

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sWifiState.onActualStateChange(context, intent);
        updateState();
    }

    @Override
    protected void toggleState() {
        sWifiState.toggleState(mWidgetView.getContext());
    }

    @Override
    protected void updateState() {
        mState = sWifiState.getActualState(mWidgetView.getContext());
        switch (mState) {
            case STATE_DISABLED:
                mIndicId = 0;
                break;
            case STATE_ENABLED:
                mIndicId = R.drawable.widget_indic_on;
                break;
            case STATE_TURNING_ON:
                mIndicId = R.drawable.widget_indic_tween;
                break;
            case STATE_TURNING_OFF:
                mIndicId = R.drawable.widget_indic_tween;
                break;
            case STATE_UNKNOWN:
                mIndicId = 0;
                break;
            default:
                mIndicId = 0;
                break;
        }
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        return filter;
    }

    /**
     * Subclass of StateTracker to get/set Wifi state.
     */
    private static final class WifiStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {
            final WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(TAG, "No wifiManager.");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable tethering if enabling Wifi
                     */
                    int wifiApState = wifiManager.getWifiApState();
                    if (desiredState
                            && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                        wifiManager.setWifiApEnabled(null, false);
                    }

                    wifiManager.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent
                    .getAction())) {
                return;
            }
            int wifiState = intent
                .getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            int widgetState=wifiStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WifiManager's state values into our Wifi/Bluetooth-common
         * state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }

    }
}
