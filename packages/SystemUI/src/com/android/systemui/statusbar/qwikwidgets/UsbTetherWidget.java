package com.android.systemui.statusbar.qwikwidgets;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

public class UsbTetherWidget extends ToggleOnly {

    public static final String TAG = "UsbTetherWidget";

    private static int UsbTetherStatus; // Going to be STATE_DISABLED, STATE_ENABLED, STATE_UNAVAILABLE
    private static String[] mUsbRegexs;
    private boolean mUsbConnected;
    private boolean mMassStorageActive;

    public UsbTetherWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_USBTETHER;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_usb_tether_title;
            mIconId = R.drawable.widget_usb_tether_icon;
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
        toggleUsbTetherState(mWidgetView.getContext());
    }

    @Override
    protected void updateState() {
        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        mState = getUsbTetherState(mWidgetView.getContext());
        switch (mState) {
            case STATE_DISABLED:
                mIconId = R.drawable.widget_usb_tether_icon;
                mIndicId = 0;
                break;
            case STATE_ENABLED:
                mIconId = R.drawable.widget_usb_tether_icon;
                mIndicId = R.drawable.widget_indic_on;
                break;
            case STATE_UNAVAILABLE:
                mIconId = R.drawable.widget_usb_tether_icon_unavailable;
                mIndicId = 0;
                break;
            default:
                mIconId = R.drawable.widget_usb_tether_icon;
                mIndicId = 0;
                break;
        }
    }

    private void updateStateExtra(Intent intent) {
        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
        getUsbTetherState(mWidgetView.getContext());
        switch (mState) {
            case STATE_DISABLED:
                mIconId = R.drawable.widget_usb_tether_icon;
                mIndicId = 0;
                break;
            case STATE_ENABLED:
                mIconId = R.drawable.widget_usb_tether_icon;
                mIndicId = R.drawable.widget_indic_on;
                break;
            case STATE_UNAVAILABLE:
                mIconId = R.drawable.widget_usb_tether_icon_unavailable;
                mIndicId = 0;
                break;
            default:
                mIconId = R.drawable.widget_usb_tether_icon;
                mIndicId = 0;
                break;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(UsbManager.ACTION_USB_STATE)) {
            updateStateExtra(intent);
        } else {
            updateState();
        }
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        return filter;
    }

    private void toggleUsbTetherState(Context context) {
        ConnectivityManager cm =
               (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // have to do this, otherwise UsbTetherStatus will equal 0... Why?
        getUsbTetherState(context);

       if (UsbTetherStatus == StateTracker.STATE_DISABLED) {
           cm.setUsbTethering(true);
       } else if (UsbTetherStatus == StateTracker.STATE_ENABLED) {
           cm.setUsbTethering(false);
       } else {
        Log.d(TAG, "toggleState: else");
       }
    }

    private int getUsbTetherState(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        String[] available = cm.getTetherableIfaces();
        String[] tethered = cm.getTetheredIfaces();
        String[] errored = cm.getTetheringErroredIfaces();
        return getUsbTetherState(context, available, tethered, errored);
    }

    private int getUsbTetherState(Context context, String[] available, String[] tethered,
            String[] errored) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mUsbRegexs = cm.getTetherableUsbRegexs();
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;

        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = cm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }

        if (usbTethered) {
             UsbTetherStatus = StateTracker.STATE_ENABLED;
             return StateTracker.STATE_ENABLED;
        } else if (usbAvailable) {
             UsbTetherStatus = StateTracker.STATE_DISABLED;
             return StateTracker.STATE_DISABLED;
        } else if (usbErrored) {
             UsbTetherStatus = StateTracker.STATE_UNAVAILABLE;
             return StateTracker.STATE_UNAVAILABLE;
        } else if (mMassStorageActive) {
             UsbTetherStatus = StateTracker.STATE_UNAVAILABLE;
             return StateTracker.STATE_UNAVAILABLE;
        } else {
             UsbTetherStatus = StateTracker.STATE_UNAVAILABLE;
             return StateTracker.STATE_UNAVAILABLE;
        }
    }

}
