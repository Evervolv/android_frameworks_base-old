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

package com.android.systemui.statusbar.qwikwidgets;

import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.systemui.R;


public class SettingsView extends LinearLayout implements View.OnClickListener {
    static final String TAG = "SettingsView";

    private Context mContext;
    private LayoutInflater mInflater;

    private static final LinearLayout.LayoutParams WIDGET_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);

    private static final LinearLayout.LayoutParams DIVIDER_VERT_PARAMS = new LinearLayout.LayoutParams(
            2, ViewGroup.LayoutParams.MATCH_PARENT);

    private static final LinearLayout.LayoutParams DIVIDER_HORIZ_PARAMS = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 2);

    public static final String WIDGET_DELIMITER = "|";

    private WidgetBroadcastReceiver mBroadcastReceiver = null;
    private WidgetSettingsObserver mObserver = null;

    private Handler mHandler = new Handler();

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    class WidgetLayout extends LinearLayout {
        public WidgetLayout(Context context) {
            super(context);
            setOrientation(LinearLayout.HORIZONTAL);
            if (QwikWidgetsHelper.isTablet(mContext)) {
                setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 64));
            } else {
                setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 94));
            }

        }
    }

    /* Used for a dividers in the widget view */
    class Divider extends LinearLayout {
        public Divider(Context context) {
            super(context);
            this.setBackgroundColor(0xD9000000);
            setOrientation(LinearLayout.HORIZONTAL);
            setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 2));
        }
    }

    public void setupQwikWidgets() {
        removeAllViews();

        String defaults = getResources().getString(R.string
                .default_qwik_widgets);
        String widgets = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.SELECTED_QWIK_WIDGETS);
        int lineMax =  Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.MAX_WIDGETS_PER_LINE, 3);

        boolean oneLine = false;
        if(widgets == null) {
            Log.i(TAG, "Default widgets being loaded");
            widgets = defaults;
        }

        LinearLayout ll = null;

        int lineCount = 0, total = 0, numDummy = 0;

        int numOf = widgets.split("\\|").length;
        String widgetArray[] = widgets.split("\\|");

        for(int i = 0; i < numOf; i++) {
            total++;
            View widgetView = null;

            if (widgetArray[i].equals("toggleBrightness")) {
                widgetView = mInflater.inflate(R.
                        layout.qwik_widgets_slider_with_toggle, null, false);
                oneLine = true;
            } else {
                widgetView = mInflater.inflate(R.
                        layout.qwik_widgets_toggle_only, null, false);
                oneLine = false;
            }

            if (oneLine) {
                if (lineCount > 0) {
                    numDummy = lineMax - lineCount;
                    for(int j = 0; j < numDummy; j++) {
                        ll.addView(new WidgetLayout(mContext),
                                WIDGET_LAYOUT_PARAMS); // add a dummy view
                    }
                    addView(ll);
                    addView(new Divider(mContext),
                            DIVIDER_HORIZ_PARAMS); // add a bottom divider
                }

                ll = new WidgetLayout(mContext);

                if(QwikWidget.loadWidget(widgetArray[i], widgetView)) {
                    ll.addView(widgetView, WIDGET_LAYOUT_PARAMS);
                    addView(ll);
                    addView(new Divider(mContext), DIVIDER_HORIZ_PARAMS); // add a bottom divider
                    lineCount = 0;
                } else {
                    Log.e(TAG, "Error setting up widget (oneLine): " + widgetArray[i]);
                }

            } else {
                if (lineCount == 0) { ll = new WidgetLayout(mContext); }

                if(QwikWidget.loadWidget(widgetArray[i], widgetView)) {
                    ll.addView(widgetView, WIDGET_LAYOUT_PARAMS);
                    lineCount++;
                    if (lineCount < lineMax) {
                        ll.addView(new Divider(mContext), DIVIDER_VERT_PARAMS); //add a middle divider
                        if (numOf == total) {
                            numDummy = lineMax - lineCount;
                            for(int k = 0; k < numDummy; k++) {
                                ll.addView(new WidgetLayout(mContext),
                                        WIDGET_LAYOUT_PARAMS); // add a dummy view
                            }
                            addView(ll);
                        }
                    } else {
                        addView(ll);
                        if (numOf != total) {
                            addView(new Divider(mContext),
                                    DIVIDER_HORIZ_PARAMS); // add a bottom divider
                            lineCount = 0;
                        }
                    }
                } else {
                    Log.e(TAG, "Error setting up widget: " + widgetArray[i]);
                }
            }
        }

        setupBroadcastReceiver();
        setupSettingsObserver(mHandler);
        IntentFilter filter = QwikWidget.getAllBroadcastIntentFilters();
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    private void setupBroadcastReceiver() {
        if(mBroadcastReceiver == null) {
            mBroadcastReceiver = new WidgetBroadcastReceiver();
        }
    }

    public void setupSettingsObserver(Handler handler) {
        if(mObserver == null) {
            mObserver = new WidgetSettingsObserver(handler);
            mObserver.observe();
        }
    }

    private class WidgetBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            QwikWidget.handleOnReceive(context, intent);
            QwikWidget.updateAllWidgets();
        }
    };

    private class WidgetSettingsObserver extends ContentObserver {
        public WidgetSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            //Register for changes to our widget list
            resolver.registerContentObserver(Settings.System.getUriFor(Settings
                    .System.SELECTED_QWIK_WIDGETS), false, this);
            //Register for changes to our widget max per line
            resolver.registerContentObserver(Settings.System.getUriFor(Settings
                    .System.MAX_WIDGETS_PER_LINE), false, this);

            for(Uri uri : QwikWidget.getAllObservedUris()) {
                resolver.registerContentObserver(uri, false, this);
            }
        }

        public void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChangeUri(Uri uri, boolean selfChange) {

            if(uri.equals(Settings.System.getUriFor(Settings.System
                    .SELECTED_QWIK_WIDGETS))) {
                setupQwikWidgets();
            } else if (uri.equals(Settings.System.getUriFor(Settings.System
                    .MAX_WIDGETS_PER_LINE))) {
                setupQwikWidgets();
            }
            QwikWidget.handleOnChangeUri(uri);
            QwikWidget.updateAllWidgets();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext = getContext();
        mInflater = (LayoutInflater)mContext.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);

        setupQwikWidgets();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private StatusBarManager getStatusBarManager() {
        return (StatusBarManager)getContext().getSystemService(Context.STATUS_BAR_SERVICE);
    }

    @Override
    public void onClick(View v) { }
}

