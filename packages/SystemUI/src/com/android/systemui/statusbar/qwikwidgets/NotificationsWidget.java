package com.android.systemui.statusbar.qwikwidgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Prefs;

public class NotificationsWidget extends ToggleOnly implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "NotificationsWidget";

    private SharedPreferences mPrefs;

    public NotificationsWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_NOTIFICATIONS;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_notifications_title;
            mIconId = R.drawable.widget_notifications_icon;
        }
    }

    @Override
    protected void toggleState() {
        final boolean dnd = mPrefs.getBoolean(Prefs.DO_NOT_DISTURB_PREF, Prefs.DO_NOT_DISTURB_DEFAULT);
        SharedPreferences.Editor editor = Prefs.edit(mWidgetView.getContext());
        editor.putBoolean(Prefs.DO_NOT_DISTURB_PREF, !dnd);
        editor.apply();
    }

    @Override
    protected boolean handleLongClick() { return false; }

    @Override
    protected void updateState() {
        //TODO: Figure a way to initialize mPrefs somewhere else during the widgets setup.
        mPrefs = Prefs.read(mWidgetView.getContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        final boolean dnd = mPrefs.getBoolean(Prefs.DO_NOT_DISTURB_PREF,
                Prefs.DO_NOT_DISTURB_DEFAULT);
        mState = dnd ? STATE_ENABLED : STATE_DISABLED;
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
        //Why do I have to call this?
        updateWidgetView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        updateState();
    }

}
