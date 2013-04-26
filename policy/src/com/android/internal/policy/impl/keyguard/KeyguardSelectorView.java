/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.internal.policy.impl.keyguard;

import android.animation.ObjectAnimator;
import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.RotarySelector;
import com.android.internal.widget.SlidingTab;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.MultiWaveView;
import com.android.internal.widget.multiwaveview.GlowPadView.OnTriggerListener;
import com.android.internal.R;

public class KeyguardSelectorView extends LinearLayout implements KeyguardSecurityView {
    private static final boolean DEBUG = KeyguardHostView.DEBUG;
    private static final String TAG = "SecuritySelectorView";
    private static final String ASSIST_ICON_METADATA_NAME =
        "com.android.systemui.action_assist_icon";

    private static final int LOCK_STYLE_JB = 0;
    private static final int LOCK_STYLE_ICS = 1;
    private static final int LOCK_STYLE_GB = 2;
    private static final int LOCK_STYLE_ECLAIR = 3;

    private KeyguardSecurityCallback mCallback;
    private GlowPadView mGlowPadView;
    private ObjectAnimator mAnim;
    private View mFadeView;
    private boolean mIsBouncing;
    private boolean mCameraDisabled;
    private boolean mSearchDisabled;
    private LockPatternUtils mLockPatternUtils;
    private SecurityMessageDisplay mSecurityMessageDisplay;
    private Drawable mBouncerFrame;

    private boolean mSilentMode;
    private AudioManager mAudioManager;
    private final boolean mHasVibrator;

    private TextView mCarrier;
    private MultiWaveView mMultiWaveView;
    private SlidingTab mSlidingTabView;
    private RotarySelector mRotarySelectorView;

    // Get the style from settings
    private int mLockscreenStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE, LOCK_STYLE_JB);

    OnTriggerListener mOnTriggerListener = new OnTriggerListener() {

        public void onTrigger(View v, int target) {
            final int resId = mGlowPadView.getResourceIdForTarget(target);
            switch (resId) {
                case com.android.internal.R.drawable.ic_action_assist_generic:
                    Intent assistIntent =
                            ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                            .getAssistIntent(mContext, UserHandle.USER_CURRENT);
                    if (assistIntent != null) {
                        mActivityLauncher.launchActivity(assistIntent, false, true, null, null);
                    } else {
                        Log.w(TAG, "Failed to get intent for assist activity");
                    }
                    mCallback.userActivity(0);
                    break;

                case com.android.internal.R.drawable.ic_lockscreen_camera:
                    mActivityLauncher.launchCamera(null, null);
                    mCallback.userActivity(0);
                    break;

                case com.android.internal.R.drawable.ic_lockscreen_unlock_phantom:
                case com.android.internal.R.drawable.ic_lockscreen_unlock:
                    mCallback.userActivity(0);
                    mCallback.dismiss(false);
                break;
            }
        }

        public void onReleased(View v, int handle) {
            if (!mIsBouncing) {
                doTransition(mFadeView, 1.0f);
            }
        }

        public void onGrabbed(View v, int handle) {
            mCallback.userActivity(0);
            doTransition(mFadeView, 0.0f);
        }

        public void onGrabbedStateChange(View v, int handle) { }

        public void onFinishFinalAnimation() { }

    };

    SlidingTab.OnTriggerListener mTabTriggerListener = new SlidingTab.OnTriggerListener() {

        @Override
        public void onTrigger(View v, int whichHandle) {
            if (whichHandle == SlidingTab.OnTriggerListener.LEFT_HANDLE) {
                mCallback.userActivity(0);
                mCallback.dismiss(false);
            } else if (whichHandle == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
                toggleRingMode();
                updateResources();
                mCallback.userActivity(0);
            }
        }

        @Override
        public void onGrabbedStateChange(View v, int grabbedState) {
            if (grabbedState != SlidingTab.OnTriggerListener.NO_HANDLE) {
                mCallback.userActivity(0);
            }
        }
    };

    RotarySelector.OnDialTriggerListener mDialTriggerListener = new
            RotarySelector.OnDialTriggerListener() {

        @Override
        public void onDialTrigger(View v, int whichHandle) {
            if (whichHandle == RotarySelector.OnDialTriggerListener.LEFT_HANDLE) {
                mCallback.userActivity(0);
                mCallback.dismiss(false);
            } else if (whichHandle == RotarySelector.OnDialTriggerListener.RIGHT_HANDLE) {
                toggleRingMode();
                updateResources();
                mCallback.userActivity(0);
            }
        }

        @Override
        public void onGrabbedStateChange(View v, int grabbedState) { }
    };

    MultiWaveView.OnTriggerListener mWaveTriggerListener = new
            MultiWaveView.OnTriggerListener() {

        @Override
        public void onGrabbedStateChange(View v, int grabbedState) {
            // Don't poke the wake lock when returning to a state where the handle is
            // not grabbed since that can happen when the system (instead of the user)
            // cancels the grab.
            if (grabbedState == MultiWaveView.OnTriggerListener.CENTER_HANDLE) {
                mCallback.userActivity(0);
            }
        }

        @Override
        public void onTrigger(View v, int target) {
            if (target == 0 || target == 1) { // 0 = unlock/portrait, 1 = unlock/landscape
                mCallback.dismiss(false);
            } else if (target == 2 || target == 3) { // 2 = alt/portrait, 3 = alt/landscape
                if (!mCameraDisabled) {
                    // Start the Camera
                    mActivityLauncher.launchCamera(null, null);
                    mCallback.dismiss(false);
                } else {
                    toggleRingMode();
                    updateResources();
                    mCallback.userActivity(0);
                }
            }
        }

        @Override
        public void onGrabbed(View v, int handle) { }

        @Override
        public void onReleased(View v, int handle) {
            mMultiWaveView.ping();
        }

        @Override
        public void onFinishFinalAnimation() { }
    };

    KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onDevicePolicyManagerStateChanged() {
            updateTargets();
        }

        @Override
        public void onSimStateChanged(State simState) {
            updateTargets();
        }
    };

    private final KeyguardActivityLauncher mActivityLauncher = new KeyguardActivityLauncher() {

        @Override
        KeyguardSecurityCallback getCallback() {
            return mCallback;
        }

        @Override
        LockPatternUtils getLockPatternUtils() {
            return mLockPatternUtils;
        }

        @Override
        Context getContext() {
            return mContext;
        }};

    public KeyguardSelectorView(Context context) {
        this(context, null);
    }

    public KeyguardSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLockPatternUtils = new LockPatternUtils(getContext());
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mHasVibrator = vibrator == null ? false : vibrator.hasVibrator();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(mOnTriggerListener);
        mMultiWaveView = (MultiWaveView) findViewById(R.id.multiwave_view);
        mMultiWaveView.setOnTriggerListener(mWaveTriggerListener);
        mSlidingTabView = (SlidingTab) findViewById(R.id.sliding_tab_view);
        mSlidingTabView.setOnTriggerListener(mTabTriggerListener);
        mRotarySelectorView = (RotarySelector) findViewById(R.id.rotary_selector_view);
        mRotarySelectorView.setOnDialTriggerListener(mDialTriggerListener);
        updateTargets();

        switch (mLockscreenStyle) {
            case LOCK_STYLE_JB:
                mGlowPadView.setVisibility(View.VISIBLE);
                mMultiWaveView.setVisibility(View.GONE);
                mSlidingTabView.setVisibility(View.GONE);
                mRotarySelectorView.setVisibility(View.GONE);
                break;
            case LOCK_STYLE_ICS:
                mGlowPadView.setVisibility(View.GONE);
                mMultiWaveView.setVisibility(View.VISIBLE);
                mSlidingTabView.setVisibility(View.GONE);
                mRotarySelectorView.setVisibility(View.GONE);
                break;
            case LOCK_STYLE_GB:
                mGlowPadView.setVisibility(View.GONE);
                mMultiWaveView.setVisibility(View.GONE);
                mSlidingTabView.setVisibility(View.VISIBLE);
                mRotarySelectorView.setVisibility(View.GONE);

                mSlidingTabView.setHoldAfterTrigger(true, false);
                mSlidingTabView.setLeftHintText(R.string.lockscreen_unlock_label);
                mSlidingTabView.setLeftTabResources(
                        R.drawable.ic_jog_dial_unlock,
                        R.drawable.jog_tab_target_green,
                        R.drawable.jog_tab_bar_left_unlock,
                        R.drawable.jog_tab_left_unlock);
                break;
            case LOCK_STYLE_ECLAIR:
                mGlowPadView.setVisibility(View.GONE);
                mMultiWaveView.setVisibility(View.GONE);
                mSlidingTabView.setVisibility(View.GONE);
                mRotarySelectorView.setVisibility(View.VISIBLE);

                mRotarySelectorView.setLeftHandleResource(R.drawable.ic_jog_dial_unlock);
                break;
            default:
                Log.e(TAG, "Error: Unknown lockscreen style.");
        }
        mSilentMode = isSilentMode();

        mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        View bouncerFrameView = findViewById(R.id.keyguard_selector_view_frame);
        mBouncerFrame = bouncerFrameView.getBackground();
    }

    public void setCarrierArea(View carrierArea) {
        mFadeView = carrierArea;
        mCarrier = (TextView) mFadeView.findViewById(R.id.carrier_text);
    }

    public boolean isTargetPresent(int resId) {
        return mGlowPadView.getTargetPosition(resId) != -1;
    }

    private boolean isSilentMode() {
        return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
    }

    private void toggleRingMode() {
        // toggle silent mode
        mSilentMode = !mSilentMode;

        String message = mSilentMode ? getContext().getString(
                R.string.global_action_silent_mode_on_status) : getContext().getString(
                R.string.global_action_silent_mode_off_status);

        final int toastIcon = mSilentMode ? R.drawable.ic_lock_ringer_off
                : R.drawable.ic_lock_ringer_on;

        final int toastColor = mSilentMode ? getContext().getResources().getColor(
                R.color.keyguard_text_color_soundoff) : getContext().getResources().getColor(
                R.color.keyguard_text_color_soundon);
        toastMessage(mCarrier, message, toastColor, toastIcon);

        if (mSilentMode) {
            mAudioManager.setRingerMode(mHasVibrator
                ? AudioManager.RINGER_MODE_VIBRATE
                : AudioManager.RINGER_MODE_SILENT);
        } else {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }

    @Override
    public void showUsabilityHint() {
        mGlowPadView.ping();
        mMultiWaveView.ping();
    }

    private void updateTargets() {
        int currentUserHandle = mLockPatternUtils.getCurrentUser();
        DevicePolicyManager dpm = mLockPatternUtils.getDevicePolicyManager();
        int disabledFeatures = dpm.getKeyguardDisabledFeatures(null, currentUserHandle);
        boolean secureCameraDisabled = mLockPatternUtils.isSecure()
                && (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA) != 0;
        boolean cameraDisabledByAdmin = dpm.getCameraDisabled(null, currentUserHandle)
                || secureCameraDisabled;
        final KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(getContext());
        boolean disabledBySimState = monitor.isSimLocked();
        boolean cameraTargetPresent =
            isTargetPresent(com.android.internal.R.drawable.ic_lockscreen_camera);
        boolean searchTargetPresent =
            isTargetPresent(com.android.internal.R.drawable.ic_action_assist_generic);

        if (cameraDisabledByAdmin) {
            Log.v(TAG, "Camera disabled by Device Policy");
        } else if (disabledBySimState) {
            Log.v(TAG, "Camera disabled by Sim State");
        }
        boolean currentUserSetup = 0 != Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE,
                0 /*default */,
                currentUserHandle);
        boolean searchActionAvailable =
                ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(mContext, UserHandle.USER_CURRENT) != null;
        mCameraDisabled = cameraDisabledByAdmin || disabledBySimState || !cameraTargetPresent
                || !currentUserSetup;
        mSearchDisabled = disabledBySimState || !searchActionAvailable || !searchTargetPresent
                || !currentUserSetup;
        updateResources();
    }

    public void updateResources() {
        if (mLockscreenStyle == LOCK_STYLE_GB) {
            boolean vibe = mSilentMode
                    && (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);

            mSlidingTabView.setRightTabResources(
                    mSilentMode ? ( vibe ? R.drawable.ic_jog_dial_vibrate_on
                                         : R.drawable.ic_jog_dial_sound_off )
                                : R.drawable.ic_jog_dial_sound_on,
                    mSilentMode ? R.drawable.jog_tab_target_yellow
                                : R.drawable.jog_tab_target_gray,
                    mSilentMode ? R.drawable.jog_tab_bar_right_sound_on
                                : R.drawable.jog_tab_bar_right_sound_off,
                    mSilentMode ? R.drawable.jog_tab_right_sound_on
                                : R.drawable.jog_tab_right_sound_off);
        } else if (mLockscreenStyle == LOCK_STYLE_ECLAIR) {
            boolean vibe = mSilentMode
                    && (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);

            int iconId = mSilentMode ? (vibe ? R.drawable.ic_jog_dial_vibrate_on
                    : R.drawable.ic_jog_dial_sound_off) : R.drawable.ic_jog_dial_sound_on;

            mRotarySelectorView.setRightHandleResource(iconId);
        } else if (mLockscreenStyle == LOCK_STYLE_ICS) {
            int resId;
            if (mCameraDisabled) {
                // Fall back to showing ring/silence if camera is disabled by DPM...
                resId = mSilentMode ? R.array.lockscreen_targets_when_silent
                    : R.array.lockscreen_targets_when_soundon;
            } else {
                resId = R.array.lockscreen_targets_with_camera;
            }
            mMultiWaveView.setTargetResources(resId);
        } else {
            // Update the search icon with drawable from the search .apk
            if (!mSearchDisabled) {
                Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                        .getAssistIntent(mContext, UserHandle.USER_CURRENT);
                if (intent != null) {
                    // XXX Hack. We need to substitute the icon here but haven't formalized
                    // the public API. The "_google" metadata will be going away, so
                    // DON'T USE IT!
                    ComponentName component = intent.getComponent();
                    boolean replaced = mGlowPadView.replaceTargetDrawablesIfPresent(component,
                            ASSIST_ICON_METADATA_NAME + "_google",
                            com.android.internal.R.drawable.ic_action_assist_generic);

                    if (!replaced && !mGlowPadView.replaceTargetDrawablesIfPresent(component,
                                ASSIST_ICON_METADATA_NAME,
                                com.android.internal.R.drawable.ic_action_assist_generic)) {
                            Slog.w(TAG, "Couldn't grab icon from package " + component);
                    }
                }
            }

            mGlowPadView.setEnableTarget(com.android.internal.R.drawable
                    .ic_lockscreen_camera, !mCameraDisabled);
            mGlowPadView.setEnableTarget(com.android.internal.R.drawable
                    .ic_action_assist_generic, !mSearchDisabled);
        }
    }

    void doTransition(View view, float to) {
        if (mAnim != null) {
            mAnim.cancel();
        }
        mAnim = ObjectAnimator.ofFloat(view, "alpha", to);
        mAnim.start();
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    public void reset() {
        mGlowPadView.reset(false);
        mMultiWaveView.reset(false);
        mSlidingTabView.reset(false);
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mInfoCallback);
    }

    @Override
    public void onResume(int reason) {
        KeyguardUpdateMonitor.getInstance(getContext()).registerCallback(mInfoCallback);
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void showBouncer(int duration) {
        mIsBouncing = true;
        KeyguardSecurityViewHelper.
                showBouncer(mSecurityMessageDisplay, mFadeView, mBouncerFrame, duration);
    }

    @Override
    public void hideBouncer(int duration) {
        mIsBouncing = false;
        KeyguardSecurityViewHelper.
                hideBouncer(mSecurityMessageDisplay, mFadeView, mBouncerFrame, duration);
    }

    /**
    * Displays a message in a text view and then restores the previous text.
    * @param textView The text view.
    * @param text The text.
    * @param color The color to apply to the text, or 0 if the existing color should be used.
    * @param iconResourceId The left hand icon.
    */
    private void toastMessage(final TextView textView, final String text, final int color, final int iconResourceId) {
        if (mPendingR1 != null) {
            textView.removeCallbacks(mPendingR1);
            mPendingR1 = null;
        }
        if (mPendingR2 != null) {
            mPendingR2.run(); // fire immediately, restoring non-toasted appearance
            textView.removeCallbacks(mPendingR2);
            mPendingR2 = null;
        }

        final String oldText = textView.getText().toString();
        final ColorStateList oldColors = textView.getTextColors();

        mPendingR1 = new Runnable() {
            public void run() {
                textView.setText(text);
                if (color != 0) {
                    textView.setTextColor(color);
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(0, iconResourceId, 0, 0);
            }
        };

        textView.postDelayed(mPendingR1, 0);
        mPendingR2 = new Runnable() {
            public void run() {
                textView.setText(oldText);
                textView.setTextColor(oldColors);
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        };
        textView.postDelayed(mPendingR2, 3500);
    }
    private Runnable mPendingR1;
    private Runnable mPendingR2;
}
