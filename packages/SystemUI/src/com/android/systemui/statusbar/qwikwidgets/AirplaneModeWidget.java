package com.android.systemui.statusbar.qwikwidgets;

import java.util.ArrayList;
import java.util.List;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class AirplaneModeWidget extends ToggleOnly {

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.AIRPLANE_MODE_ON));
    }

    public AirplaneModeWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_AIRPLANEMODE;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_airplane_title;
            mIconId = R.drawable.widget_airplane_icon;
        }
    }

    @Override
    protected void updateState() {
        mState = getAirplaneModeState(mWidgetView.getContext()) ?
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

    @Override
    protected void toggleState() {
        Context context = mWidgetView.getContext();
        boolean state = getAirplaneModeState(context);
        Settings.System.putInt(context.getContentResolver(),
            Settings.System.AIRPLANE_MODE_ON, state ? 0 : 1);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", !state);
        context.sendBroadcast(intent);
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent("android.settings.AIRPLANE_MODE_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    protected List<Uri> getObservedUris() {
        return OBSERVED_URIS;
    }

    private static boolean getAirplaneModeState(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                 Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

}
