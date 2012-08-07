/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.BatteryController";

    private Context mContext;
    private ContentResolver mCr;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();

    private BatterySettingsObserver mObserver = null;
    private int mBatteryStyle;
    private int mLastBatteryLevel;
    private boolean mLastPluggedState;
    private static final int BATT_STOCK = 0;
    private static final int BATT_PERCENT = 1;
    private static final int BATT_HIDDEN = 2;

    public BatteryController(Context context) {
        mContext = context;
        mCr = mContext.getContentResolver();

        mBatteryStyle = Settings.System.getInt(mCr, Settings.System.STATUSBAR_BATT_STYLE, BATT_PERCENT);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
        mObserver = new BatterySettingsObserver(new Handler());
        mObserver.observe();
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    public void onReceive(Context context, Intent intent) {
        batteryChange(context, intent);
    }

    private void batteryChange(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            final int icon;
            final boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            mLastPluggedState = plugged;
            mLastBatteryLevel = level;
            if (mBatteryStyle == BATT_PERCENT) {
                icon = plugged ? R.drawable.stat_sys_battery_charge
                                         : R.drawable.stat_sys_battery_mod;
            } else {
                icon = plugged ? R.drawable.stat_sys_battery_charge
                                         : R.drawable.stat_sys_battery;
            }
            int N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageResource(icon);
                v.setImageLevel(level);
                if (mBatteryStyle == BATT_HIDDEN) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                }
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        level));
            }
            N = mLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mLabelViews.get(i);
                v.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format,
                        level));
                if (mBatteryStyle == BATT_HIDDEN) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void batteryChange() {
        final int icon;
        if (mBatteryStyle == BATT_PERCENT) {
            icon = mLastPluggedState ? R.drawable.stat_sys_battery_charge
                                     : R.drawable.stat_sys_battery_mod;
        } else {
            icon = mLastPluggedState ? R.drawable.stat_sys_battery_charge
                                     : R.drawable.stat_sys_battery;
        }
        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setImageResource(icon);
            v.setImageLevel(mLastBatteryLevel);
            if (mBatteryStyle == BATT_HIDDEN)
                v.setVisibility(View.GONE);
            else
                v.setVisibility(View.VISIBLE);
            v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                    mLastBatteryLevel));
        }
        N = mLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mLabelViews.get(i);
            v.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format,
                    mLastBatteryLevel));
            if (mBatteryStyle == BATT_HIDDEN)
                v.setVisibility(View.GONE);
            else
                v.setVisibility(View.VISIBLE);
        }
    }

    private class BatterySettingsObserver extends ContentObserver {
        public BatterySettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            mCr.registerContentObserver(Settings.System.getUriFor(Settings
                    .System.STATUSBAR_BATT_STYLE), false, this);
            mCr.registerContentObserver(Settings.System.getUriFor(Settings
                    .System.DISABLE_TOOLBOX), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (Settings.System.getInt(mCr, Settings.System.DISABLE_TOOLBOX, 0) == 1) {
                mBatteryStyle = BATT_STOCK;
            } else {
                mBatteryStyle = Settings.System.getInt(mCr,
                    Settings.System.STATUSBAR_BATT_STYLE, BATT_PERCENT);
            }
            batteryChange();
        }
    }
}
