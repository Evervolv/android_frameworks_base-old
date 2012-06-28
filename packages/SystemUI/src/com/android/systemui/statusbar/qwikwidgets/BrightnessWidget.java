package com.android.systemui.statusbar.qwikwidgets;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;

import com.android.systemui.R;

public class BrightnessWidget extends ToggleWithSlider {

    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;

    private static final int AUTO_OFF = 0;
    private static final int AUTO_ON = 1;

    private static final Uri MODE_URI = Settings.System.getUriFor(
            Settings.System.SCREEN_BRIGHTNESS_MODE);
    private static final Uri BRIGHTNESS_URI = Settings.System.getUriFor(
            Settings.System.SCREEN_BRIGHTNESS);

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(MODE_URI);
        OBSERVED_URIS.add(BRIGHTNESS_URI);
    }

    private int minBright;
    private int curProgress;

    public BrightnessWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_BRIGHTNESS;
            mStyle = STYLE_TOGGLE_SLIDER;
            mLabelId = R.string.status_bar_settings_auto_brightness_label;
            mIndicId = R.drawable.widget_indic_on;
            mSliderLabelId = R.string.widget_brightness_title;
        }
    }

    @Override
    public void onChangeUri(Uri uri) {
        // ToolboxWidget calls updateState() on MODE_URI change so only
        // need to update seekbar progress on BRIGHTNESS_URI change
        if (uri.equals(BRIGHTNESS_URI)) {
            mSlider.setProgress(getBrightnessValue(minBright) - minBright);
        }
    }

    @Override
    protected boolean handleLongClick() {
        return false;
    }

    @Override
    protected void toggleState() {
        int mode = getBrightnessMode(AUTO_OFF);
        int toggle = (mode == 1) ? AUTO_OFF : AUTO_ON;
        Settings.System.putInt(mWidgetView.getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, toggle);
    }

    @Override
    public void setupWidget(View view) {
        super.setupWidget(view);
        minBright = mWidgetView.getContext().getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessDim);
        mSlider.setMax(MAXIMUM_BACKLIGHT - minBright);
        mSlider.setProgress(getBrightnessValue(minBright) - minBright);
    }

    @Override
    protected void updateState() {
        mState = (getBrightnessMode(AUTO_OFF) == AUTO_ON) ?
                STATE_ENABLED : STATE_DISABLED;
        if (mState == STATE_ENABLED) {
            mSlider.setEnabled(false);
            mIndicId = R.drawable.widget_indic_on;
        } else {
            mSlider.setEnabled(true);
            mIndicId = 0;
        }
    }

    @Override
    protected void updateProgress(int progress) {
        // seekbar progress is changing
        curProgress = progress + minBright;
        setBrightnessValue(curProgress);

    }

    @Override
    protected void updateAfterProgress() {
        // stopped tracking touch so now update global brightness level setting
        Settings.System.putInt(mWidgetView.getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, curProgress);
    }

    private int getBrightnessMode(int defaultValue) {
        int brightnessMode = defaultValue;
        try {
            brightnessMode = Settings.System.getInt(
                    mWidgetView.getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return brightnessMode;
    }

    private int getBrightnessValue(int defaultValue) {
        int curBrightness = defaultValue;
        try {
            curBrightness = Settings.System.getInt(
                    mWidgetView.getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException snfe) {
        }
        return curBrightness;
    }

    private void setBrightnessValue(int brightness) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                power.setBacklightBrightness(brightness);
            }
        } catch (RemoteException doe) {

        }
    }

    @Override
    protected List<Uri> getObservedUris() {
        return OBSERVED_URIS;
    }
}
