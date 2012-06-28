package com.android.systemui.statusbar.qwikwidgets;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wimax.WimaxHelper;
import android.net.wimax.WimaxManagerConstants;
import android.os.AsyncTask;
import android.util.Log;

import com.android.systemui.R;

public class WimaxWidget extends ToggleOnly {

    private static final StateTracker sWimaxState = new WimaxStateTracker();

    public WimaxWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_WIMAX;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_wimax_title;
            mIconId = R.drawable.widget_wimax_icon;
        }
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent("android.settings.WIMAX_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    protected void toggleState() {
        sWimaxState.toggleState(mWidgetView.getContext());
    }

    @Override
    protected void updateState() {
        mState = sWimaxState.getActualState(mWidgetView.getContext());
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
    public void onReceive(Context context, Intent intent) {
        sWimaxState.onActualStateChange(context, intent);
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION);
        return filter;
    }

    /**
     * Subclass of StateTracker to get/set WiMAX state.
     */
    private static final class WimaxStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            if (WimaxHelper.isWimaxSupported(context)) {
                return wimaxStateToFiveState(WimaxHelper.getWimaxState(context));
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(final Context context,
                final boolean desiredState) {
            if (!WimaxHelper.isWimaxSupported(context)) {
                Log.e(TAG, "WiMAX is not supported");
                return;
            }

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    WimaxHelper.setWimaxEnabled(context, desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            String action = intent.getAction();
            int wimaxState;

            if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION)) {
                wimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_4G_STATE,
                                                WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            } else if (action.equals(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION)) {
                wimaxState = intent.getIntExtra(WimaxManagerConstants.CURRENT_WIMAX_ENABLED_STATE,
                                                WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            } else {
                return;
            }
            int widgetState = wimaxStateToFiveState(wimaxState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts Wimax4GManager's state values into our
         * WiMAX-common state values.
         * Also compatible with WimaxController state values.
         */
        private static int wimaxStateToFiveState(int wimaxState) {
            switch (wimaxState) {
                case WimaxManagerConstants.NET_4G_STATE_DISABLED:
                    return STATE_DISABLED;
                case WimaxManagerConstants.NET_4G_STATE_ENABLED:
                    return STATE_ENABLED;
                case WimaxManagerConstants.NET_4G_STATE_ENABLING:
                    return STATE_TURNING_ON;
                case WimaxManagerConstants.NET_4G_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }

    }


}
