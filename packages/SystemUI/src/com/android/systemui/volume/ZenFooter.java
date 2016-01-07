/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.ZenModeController;

import java.util.Objects;

/**
 * Zen mode information (and end button) attached to the bottom of the volume dialog.
 */
public class ZenFooter extends LinearLayout {
    private static final String TAG = Util.logTag(ZenFooter.class);

    private final Context mContext;
    private final SpTexts mSpTexts;

    private ImageView mIcon;
    private ImageView mSettingsIcon;
    private TextView mSummaryLine1;
    private int mZen = -1;
    private ZenModeConfig mConfig;
    private SystemUI mSysui;
    private VolumeDialog mDialog;

    public ZenFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mSpTexts = new SpTexts(mContext);
        final LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(new ValueAnimator().getDuration() / 2);
        setLayoutTransition(layoutTransition);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (ImageView) findViewById(R.id.volume_zen_icon);
        mSettingsIcon = (ImageView) findViewById(R.id.volume_zen_settings_icon);
        mSummaryLine1 = (TextView) findViewById(R.id.volume_zen_summary_line_1);
        mSpTexts.add(mSummaryLine1);
    }

    public void init(VolumeDialog dialog, final ZenModeController controller, SystemUI sysui) {
        mDialog = dialog;
        mSysui = sysui;
        mZen = controller.getZen();
        mConfig = controller.getConfig();
        mSettingsIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSysui.getComponent(PhoneStatusBar.class).startActivityDismissingKeyguard(
                        ZenModePanel.ZEN_PRIORITY_SETTINGS, true , true);
                mDialog.dismissWaitForRipple(Events.DISMISS_REASON_SETTINGS_CLICKED);
            }

        });
        update();
    }

    private void setZen(int zen) {
        if (mZen == zen) return;
        mZen = zen;
        update();
    }

    private void setConfig(ZenModeConfig config) {
        if (Objects.equals(mConfig, config)) return;
        mConfig = config;
        update();
    }

    public void update() {
        String text = mContext.getString(R.string.zen_interruption_level_all);
        int iconRes = R.drawable.ic_zen_all_24;
        switch (mZen) {
            case Global.ZEN_MODE_ALARMS:
                text = mContext.getString(R.string.zen_no_interruptions);
                iconRes = R.drawable.stat_sys_dnd_total_silence_24;
                break;
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                text = mContext.getString(R.string.zen_important_interruptions);
                iconRes = R.drawable.stat_sys_dnd_24;
                break;
        }
        mIcon.setImageResource(iconRes);
        Util.setText(mSummaryLine1, text);
        mSettingsIcon.setVisibility(mZen == Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS ?
                View.VISIBLE : View.GONE);
    }

    public void onConfigurationChanged() {
        // Empty
    }

    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        @Override
        public void onZenChanged(int zen) {
            setZen(zen);
        }
        @Override
        public void onConfigChanged(ZenModeConfig config) {
            setConfig(config);
        }
    };
}
