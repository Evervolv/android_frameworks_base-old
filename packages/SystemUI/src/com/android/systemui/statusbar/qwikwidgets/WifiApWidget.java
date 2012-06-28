package com.android.systemui.statusbar.qwikwidgets;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class WifiApWidget extends ToggleOnly {

    private static final StateTracker sWifiApState = new WifiApStateTracker();

    public static final String TAG = "WifiApWidget";

    public WifiApWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_WIFIAP;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_wifiap_title;
            mIconId = R.drawable.widget_wifiap_icon;
        }
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    protected void toggleState() {
        toggleRadioState(sWifiApState,mWidgetView.getContext());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sWifiApState.onActualStateChange(context, intent);
        updateState();
    }

    public static void toggleRadioState(StateTracker tracker, Context context) {
        int result = tracker.getActualState(context);
        if (result == StateTracker.STATE_DISABLED){
            tracker.requestStateChange(context,true);
        } else if (result == StateTracker.STATE_ENABLED){
            tracker.requestStateChange(context,false);
        } else {
            // we must be between on and off so we do nothing
        }
    }

    @Override
    protected void updateState() {
        mState = sWifiApState.getActualState(mWidgetView.getContext());
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
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        return filter;
    }

    /**
    * Subclass of StateTracker to get/set Wifi AP state.
    */
    private static final class WifiApStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiApStateToFiveState(wifiManager.getWifiApState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {

            final WifiManager wifiManager = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d("WifiAPManager", "No wifiManager.");
                return;
            }
            Log.i("WifiAp", "Setting: " + desiredState);

            // Actually request the Wi-Fi AP change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                    * Disable Wifi if enabling tethering
                    */
                    int wifiState = wifiManager.getWifiState();
                    if (desiredState
                            && ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
                        wifiManager.setWifiEnabled(false);
                    }

                    wifiManager.setWifiApEnabled(null, desiredState);
                    Log.i("WifiAp", "Async Setting: " + desiredState);
                    return null;
                }
            }.execute();
        }
        @Override
        public void onActualStateChange(Context context, Intent intent) {

            if (!WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent
                    .getAction())) {
                return;
            }
            int wifiState = intent
                .getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, -1);
            int widgetState=wifiApStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
* Converts WifiManager's state values into our Wifi/WifiAP/Bluetooth-common
* state values.
*/
        private static int wifiApStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_AP_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_AP_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_AP_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_AP_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }


    }
}
