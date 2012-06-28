package com.android.systemui.statusbar.qwikwidgets;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.systemui.R;

public class AutoRotateWidget extends ToggleOnly {

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION));
    }

    public AutoRotateWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_AUTOROTATE;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_auto_rotate_title;
            mIconId = R.drawable.widget_auto_rotate_icon;
        }
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent("android.settings.DISPLAY_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onChangeUri(Uri uri) {
        updateState();
    }

    @Override
    protected void toggleState() {
        toggleOrientationState(mWidgetView.getContext());
    }

    @Override
    protected void updateState() {
        mState = getAutoRotationState(mWidgetView.getContext()) ?
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

    protected static void toggleOrientationState(Context context) {
        setAutoRotation(!getAutoRotationState(context));
    }

    private static boolean getAutoRotationState(Context context) {
        ContentResolver cr = context.getContentResolver();
        return 0 != Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0);
    }

    @Override
    protected List<Uri> getObservedUris() {
        return OBSERVED_URIS;
    }

    private static void setAutoRotation(final boolean autorotate) {
        AsyncTask.execute(new Runnable() {
                public void run() {
                    try {
                        IWindowManager wm = IWindowManager.Stub.asInterface(
                                ServiceManager.getService(Context.WINDOW_SERVICE));
                        if (autorotate) {
                            wm.thawRotation();
                        } else {
                            wm.freezeRotation(-1);
                        }
                    } catch (RemoteException exc) {
                        Log.w(TAG, "Unable to save auto-rotate setting");
                    }
                }
            });
    }
}
