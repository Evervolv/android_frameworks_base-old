package com.android.systemui.statusbar.qwikwidgets;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.R;

public class GpsWidget extends ToggleOnly {

    public static final String TAG = "GpsWidget";

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED));
    }

    public GpsWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_GPS;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_gps_title;
            mIconId = R.drawable.widget_gps_icon;
        }
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    protected void toggleState() {
        Context context = mWidgetView.getContext();
        ContentResolver resolver = context.getContentResolver();
        boolean enabled = getGpsState(context);
        Settings.Secure.setLocationProviderEnabled(resolver,
                LocationManager.GPS_PROVIDER, !enabled);
    }

    @Override
    protected void updateState() {
        mState = getGpsState(mWidgetView.getContext()) ?
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
    protected List<Uri> getObservedUris() {
        return OBSERVED_URIS;
    }

    private static boolean getGpsState(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.Secure.isLocationProviderEnabled(resolver,
                LocationManager.GPS_PROVIDER);
    }

}
