package com.android.systemui.statusbar.toolbox;

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

    private int mOldBrightness;
    private int mOldAutomatic;

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE));
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS));
    }

    private Context mContext;

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
        if (uri.equals(Settings.System.SCREEN_BRIGHTNESS)) {
            updateProgress(getBrightnessValue(mWidgetView.getContext(), 0));
        } else {
            updateState();
        }
    }

    @Override
    protected boolean handleLongClick() {

        return false;
    }

    @Override
    protected void toggleState() {
        int mode = getBrightnessMode(mWidgetView.getContext(), 0);
        int toggle = (mode == 1) ? AUTO_OFF : AUTO_ON;
        Settings.System.putInt(mWidgetView.getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, toggle);
        updateState();
    }

    @Override
    public void setupWidget(View view) {
        super.setupWidget(view);
        mOldBrightness = getBrightnessValue(mWidgetView.getContext(), 0);
        mOldAutomatic = getBrightnessMode(mWidgetView.getContext(), 0);
        mSlider.setMax(MAXIMUM_BACKLIGHT);
        mSlider.setProgress(getBrightnessValue(mWidgetView.getContext(), 0));
    }

    @Override
    protected void updateState() {
        mState = (getBrightnessMode(mWidgetView.getContext(), 0) == 1) ?
                STATE_ENABLED : STATE_DISABLED;
        switch (mState) {
            case STATE_DISABLED:
                mSlider.setEnabled(true);
                setBrightnessValue(mSlider.getProgress());
                mIndicId = 0;
                break;
            case STATE_ENABLED:
                mSlider.setEnabled(false);
                mIndicId = R.drawable.widget_indic_on;
                break;
            default:
                mSlider.setEnabled(true);
                mIndicId = 0;
                break;
        }
    }

    @Override
    protected void updateProgress(int progress) {
        Settings.System.putInt(mWidgetView.getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        setBrightnessValue(progress);
    }

    private int getBrightnessMode(Context context, int defaultValue) {
        int brightnessMode = defaultValue;
        try {
            brightnessMode = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return brightnessMode;
    }

    private int getBrightnessValue(Context context, int defaultValue) {
        int value;
        try {
            value = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException ex) {
            value = MAXIMUM_BACKLIGHT;
        }
        return value;
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
