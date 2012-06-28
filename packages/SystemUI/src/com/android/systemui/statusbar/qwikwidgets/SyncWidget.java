package com.android.systemui.statusbar.qwikwidgets;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.android.systemui.R;

public class SyncWidget extends ToggleOnly {

    public static final String TAG = "SyncWidget";

    public SyncWidget() {
        if (mType == WIDGET_UNKNOWN) {
            mType = WIDGET_SYNC;
            mStyle = STYLE_TOGGLE;
            mLabelId = R.string.widget_sync_title;
            mIconId = R.drawable.widget_sync_icon;
        }
    }

    @Override
    protected boolean handleLongClick() {
        Intent intent = new Intent("android.settings.SYNC_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mWidgetView.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateState();
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.sync.SYNC_CONN_STATUS_CHANGED");
        return filter;
    }

    @Override
    protected void toggleState() {
        ConnectivityManager connManager = (ConnectivityManager)mWidgetView
                .getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean backgroundData = getBackgroundDataState(mWidgetView.getContext());
        boolean sync = ContentResolver.getMasterSyncAutomatically();

        // four cases to handle:
        // setting toggled from off to on:
        // 1. background data was off, sync was off: turn on both
        if (!backgroundData && !sync) {
            connManager.setBackgroundDataSetting(true);
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // 2. background data was off, sync was on: turn on background data
        if (!backgroundData && sync) {
            connManager.setBackgroundDataSetting(true);
        }

        // 3. background data was on, sync was off: turn on sync
        if (backgroundData && !sync) {
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // setting toggled from on to off:
        // 4. background data was on, sync was on: turn off sync
        if (backgroundData && sync) {
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }

    @Override
    protected void updateState() {
        mState = getSyncState(mWidgetView.getContext()) ?
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

    private static boolean getSyncState(Context context) {
        boolean backgroundData = getBackgroundDataState(context);
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        return backgroundData && sync;
    }

    private static boolean getBackgroundDataState(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getBackgroundDataSetting();
    }

}
