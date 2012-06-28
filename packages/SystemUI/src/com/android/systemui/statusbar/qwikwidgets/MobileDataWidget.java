package com.android.systemui.statusbar.qwikwidgets;

import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.util.Log;

public class MobileDataWidget extends ToggleOnly {

    public static final String TAG = "MobileDataWidget";

    public MobileDataWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_MOBILEDATA;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_mobile_data_title;
            mIconId = R.drawable.widget_mobile_data_icon;
        }
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    protected void toggleState() {
        toggleDataState(mWidgetView.getContext());
    }

    @Override
    protected void updateState() {
        mState = getDataState(mWidgetView.getContext()) ?
                STATE_ENABLED : STATE_DISABLED;
        switch (mState) {
            case STATE_DISABLED:
                mIndicId = 0;
                break;
            case STATE_ENABLED:
                mIndicId = R.drawable.widget_indic_on;
                break;
            default:
                mIndicId = 0;
                break;
        }
    }

    public static void toggleDataState(Context context) {
        boolean enabled = getDataState(context);

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (enabled) {
            cm.setMobileDataEnabled(false);
        } else {
         cm.setMobileDataEnabled(true);

        }
    }

    private static boolean getDataState(Context context) {
     ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            /* Make sure the state change propagates */
            Thread.sleep(100);
        } catch (java.lang.InterruptedException ie) {
        }
        return cm.getMobileDataEnabled();
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        return filter;
    }

}
