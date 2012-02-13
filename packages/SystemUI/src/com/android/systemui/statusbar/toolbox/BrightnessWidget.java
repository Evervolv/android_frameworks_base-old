package com.android.systemui.statusbar.toolbox;

import android.content.Context;
import android.util.Log;

import com.android.systemui.R;

public class BrightnessWidget extends ToggleWithSlider {

    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    private static final int MINIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_DIM + 10;
    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;

    private Context mContext;

    public BrightnessWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_BRIGHTNESS;
            mStyle = STYLE_TOGGLE_SLIDER;
            mLabelId = R.string.status_bar_settings_auto_brightness_label;
            mIndicId = R.drawable.widget_indic_on;
            mSliderLabelId = R.string.widget_brightness_title;
        }

        //mContext = mWidgetView.getContext();

        //boolean automaticAvailable = mContext.getResources().getBoolean(com
        //        .android.internal.R.bool.config_automatic_brightness_available);

    }

    @Override
    protected boolean handleLongClick() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void toggleState() {
        Log.d(TAG, "toggleState()");
    }

    @Override
    protected void updateState() {
        // TODO Auto-generated method stub

    }

}
