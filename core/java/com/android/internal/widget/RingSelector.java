/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.internal.widget;
import android.provider.Settings;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.internal.R;

/**
 * A special widget containing two (or three) Rings.  Moving either ring beyond
 * the threshold will cause the registered OnRingTriggerListener.onTrigger() to be called with
 * whichRing being {@link OnRingTriggerListener#LEFT_RING}, {@link OnRingTriggerListener#RIGHT_RING},
 * or {@link OnRingTriggerListener#MIDDLE_RING}.
 * Equivalently, selecting a ring will result in a call to
 * {@link OnRingTriggerListener#onGrabbedStateChange(View, int)} with one of these three states. Releasing
 * the ring will result in whichRing being {@link OnRingTriggerListener#NO_RING}.
 */
public class RingSelector extends ViewGroup {
    private static final String TAG = "RingSelector";
    private static final boolean DBG = false;
    private static final int HORIZONTAL = 0; // as defined in attrs.xml
    private static final int VERTICAL = 1;

    private final float mThresholdRadiusDIP;
    private final float mThresholdRadius;
    private final float mThresholdRadiusSq;

    private static final int ANIM_CENTER_FADE_TIME = 250; //fade time for center ring (ms)
    private static final int ANIM_DURATION = 250; // Time for most animations (in ms)
    private static final int ANIM_TARGET_TIME = 500; // Time to show targets (in ms)

    private OnRingTriggerListener mOnRingTriggerListener;
    private int mGrabbedState = OnRingTriggerListener.NO_RING;
    private boolean mTriggered = false;
    private Vibrator mVibrator;

    private float mDensity; // used to scale dimensions for bitmaps.
    private float mDensityScaleFactor=1;

    private final int mBottomOffsetDIP;
    private final int mCenterOffsetDIP;
    private final int mSecRingBottomOffsetDIP;
    private final int mSecRingCenterOffsetDIP;
    private final int mBottomOffset;
    private final int mCenterOffset;
    private final int mSecRingBottomOffset;
    private final int mSecRingCenterOffset;
    
   

    private boolean mUseMiddleRing = true;

    /**
     * Either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    private int mOrientation;
    private int mRingAlignment = ALIGN_CENTER;

    private Ring mMiddleRing;

    private Ring mCurrentRing;
    private boolean mTracking;
    private boolean mAnimating;

    private SecRing[] mSecRings;
    
    private int mMiddleRingY;

    /**
     * Ring alignment - determines where the ring should be drawn
     */

    public static final int ALIGN_RIGHT = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_BOTTOM = 2;
    public static final int ALIGN_CENTER = 3;
    public static final int ALIGN_BOTTOMRIGHT = 4;
    public static final int ALIGN_BOTTOMLEFT = 5;

    //to be coded.
    public static final int ALIGN_CUSTOM = 6;
    public static final int ALIGN_PHONE = 7; // will be obsolete once the option is added.
    public static final int ALIGN_UNKNOWN = 8;
    
    
    /**
     * Listener used to reset the view when the current animation completes.
     */
    private final AnimationListener mAnimationDoneListener = new AnimationListener() {
        public void onAnimationStart(Animation animation) {

        }

        public void onAnimationRepeat(Animation animation) {

        }

        public void onAnimationEnd(Animation animation) {
            onAnimationDone();
        }
    };

    /**
     * Interface definition for a callback to be invoked when a ring is triggered
     * by moving it beyond a threshold.
     */
    public interface OnRingTriggerListener {
        /**
         * The interface was triggered because the user let go of the ring without reaching the
         * threshold.
         */
        public static final int NO_RING = 0;

        /**
         * The interface was triggered because the user grabbed the middle ring and moved it to
         * a custom secondary ring.
         */
        public static final int MIDDLE_RING = 3;

        /**
         * Called when the user moves a ring beyond the threshold.
         *
         * @param v The view that was triggered.
         * @param whichRing  Which ring the user grabbed,
         *        either {@link #LEFT_RING}, {@link #RIGHT_RING}, or {@link MIDDLE_RING}.
         * @param whichSecRing Which secondary ring (0-3) the user triggered with the middle ring.
         *        -1 if ring wasn't the middle one.
         */
        void onRingTrigger(View v, int whichRing, int whichSecRing);

        /**
         * Called when the "grabbed state" changes (i.e. when the user either grabs or releases
         * one of the rings.)
         *
         * @param v the view that was triggered
         * @param grabbedState the new state: {@link #NO_RING}, {@link #LEFT_RING},
         * {@link #RIGHT_RING}, or {@link MIDDLE_RING}.
         */
        void onGrabbedStateChange(View v, int grabbedState);
    }

    /**
     * Simple container class for all things pertinent to a ring.
     * A slider consists of 2 Views:
     *
     * {@link #ring} is the ring shown on the screen in the default state.
     * {@link #target} is the target the user must drag the ring away from to trigger the ring.
     *
     */
    private class Ring {

        /**
         * States for the view.
         */
        private static final int STATE_NORMAL = 0;
        private static final int STATE_PRESSED = 1;
        private static final int STATE_ACTIVE = 2;

        private boolean isHidden = false;

        private final ImageView ring;
        private final ImageView target;
        private int currentState = STATE_NORMAL;
        private int alignment = ALIGN_UNKNOWN;
        private int alignCenterX;
        private int alignCenterY;

        /**
         * Constructor
         *
         * @param parent the container view of this one
         * @param ringId drawable for the ring
         * @param targetId drawable for the target
         */
        Ring(ViewGroup parent, int ringId, int targetId) {
            // Create ring
            ring = new ImageView(parent.getContext());
            ring.setBackgroundResource(ringId);
            ring.setScaleType(ScaleType.CENTER);
            ring.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));

            // Create target
            target = new ImageView(parent.getContext());
            target.setImageResource(targetId);
            target.setScaleType(ScaleType.CENTER);
            target.setLayoutParams(
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            target.setVisibility(View.INVISIBLE);

            parent.addView(target); // this needs to be first - relies on painter's algorithm
            parent.addView(ring);
        }

        void setHiddenState(boolean hidden) {
            isHidden = hidden;
            if (isHidden) {
                ring.setVisibility(View.GONE);
            } else {
                reset(false);
            }
        }

        void setIcon(int iconId) {
            ring.setImageResource(iconId);
        }

        void setIcon(Bitmap icon) {
            ring.setImageBitmap(icon);
        }

        void setRingBackgroundResource(int ringId) {
            ring.setBackgroundResource(ringId);
        }

        void hide() {
            if (isHidden) return;
            if (ring.getVisibility() == View.INVISIBLE) return;

            int centerX = (ring.getLeft() + ring.getRight()) / 2;
            int centerY = (ring.getTop() + ring.getBottom()) / 2;
            int targetX = alignCenterX;
            int targetY = alignCenterY;

            if (alignment == ALIGN_LEFT) {
                targetX -= 2 * ring.getWidth() / 3;
            } else if (alignment == ALIGN_RIGHT) {
                targetX += 2 * ring.getWidth() / 3;
            } else if (alignment == ALIGN_BOTTOM) {
                targetY += 2 * ring.getHeight() / 3;
            }

            int dx = targetX - centerX;
            int dy = targetY - centerY;

            ring.offsetLeftAndRight(dx);
            ring.offsetTopAndBottom(dy);

            Animation trans = new TranslateAnimation(-dx, 0, -dy, 0);
            trans.setDuration(ANIM_DURATION);
            trans.setFillAfter(true);
            ring.startAnimation(trans);
            ring.setVisibility(View.INVISIBLE);
            target.setVisibility(View.INVISIBLE);
        }

        void show(boolean animate) {
            if (isHidden) return;

            int centerX = (ring.getLeft() + ring.getRight()) / 2;
            int centerY = (ring.getTop() + ring.getBottom()) / 2;
            int dx = alignCenterX - centerX;
            int dy = alignCenterY - centerY;

            ring.offsetLeftAndRight(dx);
            ring.offsetTopAndBottom(dy);

            if (ring.getVisibility() == View.VISIBLE) return;

            ring.setVisibility(View.VISIBLE);
            if (animate) {
                Animation trans = new TranslateAnimation(-dx, 0, -dy, 0);
                trans.setFillAfter(true);
                trans.setDuration(ANIM_DURATION);
                ring.startAnimation(trans);
            }
        }

        void setState(int state) {
            ring.setPressed(state == STATE_PRESSED);
            if (state == STATE_ACTIVE) {
                final int[] activeState = new int[] {com.android.internal.R.attr.state_active};
                if (ring.getBackground().isStateful()) {
                    ring.getBackground().setState(activeState);
                }
            }
            currentState = state;
        }

        void showTarget() {
            AlphaAnimation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
            alphaAnim.setDuration(ANIM_TARGET_TIME);
            target.startAnimation(alphaAnim);
            target.setVisibility(View.VISIBLE);
        }

        void reset(boolean animate) {
            if (isHidden) return;

            setState(STATE_NORMAL);
            target.setVisibility(View.INVISIBLE);

            int centerX = (ring.getLeft() + ring.getRight()) / 2;
            int centerY = (ring.getTop() + ring.getBottom()) / 2;
            int dx = alignCenterX - centerX;
            int dy = alignCenterY - centerY;

            ring.offsetLeftAndRight(dx);
            ring.offsetTopAndBottom(dy);

            /*
             * setScaleX/Y were introducted in 3.0, so can't use them directly.
             * Instead, have a ScaleAnimation rescale the ring (with 0 duration)
             */
            ScaleAnimation scaleAnim = new ScaleAnimation(1.0f, 1.0f, 1.0f, 1.0f);
            scaleAnim.setDuration(0);
            scaleAnim.setFillAfter(true);
            ring.startAnimation(scaleAnim);

            ring.setVisibility(View.VISIBLE);
            
            if (animate) {
                TranslateAnimation trans = new TranslateAnimation(-dx, 0, -dy, 0);
                trans.setDuration(ANIM_DURATION);
                trans.setInterpolator(new OvershootInterpolator());
                trans.setFillAfter(false);
                ring.startAnimation(trans);
            } else {
                ring.clearAnimation();
                target.clearAnimation();
            }
        }

        void setTarget(int targetId) {
            target.setImageResource(targetId);
        }

        /**
         * Layout the given widgets within the parent.
         *
         * @param l the parent's left border
         * @param t the parent's top border
         * @param r the parent's right border
         * @param b the parent's bottom border
         * @param alignment which side to align the widget to
         */
        void layout(int l, int t, int r, int b, int alignment) {
            this.alignment = alignment;

            final int parentWidth = r - l;
            final int parentHeight = b - t;

            final Drawable ringBackground = ring.getBackground();
            final int ringWidth = ringBackground.getIntrinsicWidth();
            final int ringHeight = ringBackground.getIntrinsicHeight();
            final int hRingWidth = ringWidth / 2;
            final int hRingHeight = ringHeight / 2;

            final Drawable targetDrawable = target.getDrawable();
            final int targetWidth = targetDrawable.getIntrinsicWidth();
            final int targetHeight = targetDrawable.getIntrinsicHeight();
            final int hTargetWidth = targetWidth / 2;
            final int hTargetHeight = targetHeight / 2;

            if (mOrientation == HORIZONTAL) { //Used for landscape
            	// TODO: landscape code.
            } else if (mOrientation == VERTICAL) { // used for portrait
            	
            	if (alignment == ALIGN_PHONE || alignment == ALIGN_BOTTOM) {
                    alignCenterX = parentWidth / 2;
                    alignCenterY = parentHeight;
                } else if (alignment == ALIGN_RIGHT) {
                    alignCenterX = parentWidth;
                    alignCenterY = parentHeight / 2;
                } else if (alignment == ALIGN_LEFT) {
                    alignCenterX = 0;
                    alignCenterY = parentHeight / 2;
                } else if (alignment == ALIGN_BOTTOMRIGHT) {
                    alignCenterX = parentWidth;
                    alignCenterY = parentHeight;
                } else if (alignment == ALIGN_BOTTOMLEFT) {
                    alignCenterX = 0;
                    alignCenterY = parentHeight;
                } else { // ALIGN_CENTER or undefined
                    alignCenterX = parentWidth / 2;
                    alignCenterY = parentHeight * 3 / 5;
                }
            	
            }     
            
            ring.layout(alignCenterX - hRingWidth, alignCenterY - hRingHeight,
                    alignCenterX + hRingWidth, alignCenterY + hRingHeight);
            target.layout(alignCenterX - hTargetWidth, alignCenterY - hTargetHeight,
                    alignCenterX + hTargetWidth, alignCenterY + hTargetHeight);
        }

        public void updateDrawableStates() {
            setState(currentState);
        }

        /**
         * Ensure all the dependent widgets are measured.
         */
        public void measure() {
            ring.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        }

        /**
         * Get the measured ring width. Must be called after {@link Ring#measure()}.
         * @return width of the ring
         */
        public int getRingWidth() {
            return ring.getMeasuredWidth();
        }

        /**
         * Get the measured ring height. Must be called after {@link Ring#measure()}.
         * @return height of the ring.
         */
        public int getRingHeight() {
            return ring.getMeasuredHeight();
        }

        /**
         * Start animating the ring.
         *
         * @param anim1
         */
        public void startAnimation(Animation anim1) {
            ring.startAnimation(anim1);
        }

        public void hideTarget() {
            target.clearAnimation();
            target.setVisibility(View.INVISIBLE);
        }

        public boolean contains(int x, int y) {
            final Drawable ringBackground = ring.getBackground();
            final int ringWidth = ringBackground.getIntrinsicWidth();
            final int ringHeight = ringBackground.getIntrinsicHeight();
            final int hRingWidth = ringWidth / 2;
            final int hRingHeight = ringHeight / 2;
            final int r = (hRingWidth + hRingHeight) / 2;

            final int centerX = ring.getLeft() + (ring.getWidth() / 2);
            final int centerY = ring.getTop() + (ring.getHeight() / 2);

            final int dx = x - centerX;
            final int dy = y - centerY;

            return (dx * dx + dy * dy < r * r);
        }
    }

    private class SecRing {

        private final ImageView ring;

        private int alignCenterX;
        private int alignCenterY;
        
        private boolean isHidden = false;
        private boolean isActive = false;

        public SecRing(ViewGroup parent, int ringId) {
            ring = new ImageView(parent.getContext());
            ring.setBackgroundResource(ringId);
            ring.setScaleType(ScaleType.CENTER);
            ring.setVisibility(View.INVISIBLE);
            ring.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));

            parent.addView(ring);
        }

        void setHiddenState(boolean hidden) {
            isHidden = hidden;
            if (hidden) hide();
        }

        boolean isHidden() {
            return isHidden;
        }

        void hide() {
            if (ring.getVisibility() == View.INVISIBLE) return;
            AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
            alphaAnim.setDuration(ANIM_CENTER_FADE_TIME);
            alphaAnim.setInterpolator(new DecelerateInterpolator());
            ring.startAnimation(alphaAnim);
            ring.setVisibility(View.INVISIBLE);
        }

        void show() {
            if (ring.getVisibility() == View.VISIBLE || isHidden) return;
            AlphaAnimation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
            alphaAnim.setDuration(ANIM_CENTER_FADE_TIME);
            ring.startAnimation(alphaAnim);
            ring.setVisibility(View.VISIBLE);
        }

        void activate() {
            if (isActive) return;
            isActive = true;

            ScaleAnimation scaleAnim = new ScaleAnimation(1.0f, 1.5f, 1.0f, 1.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnim.setInterpolator(new DecelerateInterpolator());
            scaleAnim.setDuration(ANIM_CENTER_FADE_TIME);
            scaleAnim.setFillAfter(true);
            ring.startAnimation(scaleAnim);
        }

        void deactivate() {
            if (!isActive) return;
            isActive = false;

            ScaleAnimation scaleAnim = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnim.setInterpolator(new DecelerateInterpolator());
            scaleAnim.setDuration(ANIM_CENTER_FADE_TIME);
            scaleAnim.setFillAfter(true);
            ring.startAnimation(scaleAnim);
        }

        void reset(boolean animate) {
            if (animate) {
                hide();
            }
            ring.setVisibility(View.INVISIBLE);
        }

        void setIcon(int iconId) {
            ring.setImageResource(iconId);
        }

        void setIcon(Bitmap icon) {
            ring.setImageBitmap(icon);
        }

        void setRingBackgroundResource(int ringId) {
            ring.setBackgroundResource(ringId);
        }

        /**
         * Layout the given widgets within the parent.
         *
         * @param l the parent's left border
         * @param t the parent's top border
         * @param r the parent's right border
         * @param b the parent's bottom border
         * @param orientation orientation of screen (HORIZONTAL or VERTICAL)
         * @param ringNum ring number (0-[totalRings-1])
         * @param totalRings the total number of secondary rings (1-4)
         */
        void layout(int l, int t, int r, int b, int orientation, int ringNum, int totalRings , int alignment) {
            final int parentWidth = r - l;
            final int parentHeight = b - t;

            final Drawable ringBackground = ring.getBackground();
            final int ringWidth = ringBackground.getIntrinsicWidth();
            final int ringHeight = ringBackground.getIntrinsicHeight();
            final int hRingWidth = ringWidth / 2;
            final int hRingHeight = ringHeight / 2;
            
            if (mOrientation == HORIZONTAL) { //Used for landscape
            	// TODO: landscape code.
            } else if (orientation == VERTICAL) {
                int spacingW = parentWidth / 20;
                int spacingH = parentHeight / 16;

                if (alignment == ALIGN_PHONE) {
                    // answer ring(0) and decline ring(1)
                    alignCenterY = parentHeight * 3 / 4;
                    alignCenterX = (ringNum * 2 + 1) * parentWidth / 4;

                } else if (alignment == ALIGN_BOTTOM) {
                    if (ringNum <= 3) { // app rings
                        alignCenterY = parentHeight * 3 / 4 - hRingHeight;
                        alignCenterX = ringNum * 2 * spacingW + (ringNum + 1) * 3 * spacingW;
                        if ((ringNum % 3) == 0) { // shift first and fourth apps down
                            alignCenterY += ringHeight;
                        }
                        if ((ringNum % 2) != 0) { // horizontal spacing correction for second and fourth apps
                            alignCenterX -= spacingW;
                        }
                    } else { // unlock ring(4) and sound toggle ring(5)
                        alignCenterY = parentHeight / 2;
                        alignCenterX = (ringNum * -1 + 5) * parentWidth + (ringNum * 2 - 9) * mSecRingBottomOffset;
                    }
                } else if (alignment == ALIGN_LEFT || alignment == ALIGN_RIGHT) { // right(0) and left(1)
                    if (ringNum <= 3) { // app rings
                        alignCenterY = ringNum * 4 * spacingH + spacingH * 2;
                        alignCenterX = ((alignment - 1) * -2 + 1) * parentWidth / 4 + (alignment * -2 + 1) * hRingWidth;
                        if ((ringNum % 3) != 0) { // shift second and third apps over
                            alignCenterX += (alignment * 2 - 1) * ringWidth;
                        }
                    } else { // unlock ring(4) and sound toggle ring(5)
                        alignCenterX = parentWidth / 2;
                        alignCenterY = (ringNum * -1 + 5) * parentHeight + (ringNum * 2 - 9) * mSecRingBottomOffset;
                    }
                } else if (alignment == ALIGN_BOTTOMRIGHT || alignment == ALIGN_BOTTOMLEFT) { // bottomright(4) and bottomleft(5)
                    if (ringNum == 4) { // unlock ring
                        alignCenterX = (alignment - 4) * parentWidth - (alignment * 2 - 9) * ((parentWidth * 3 / 5 - parentWidth / 5) / 2 + parentWidth / 5);
                        alignCenterY = parentHeight - ((alignment - 4) * parentWidth - (alignment * 2 - 9) * alignCenterX - parentWidth / 5 + 3 * spacingW);
                    } else if (ringNum == 5) { // sound toggle ring
                        alignCenterY = parentHeight - ((parentWidth * 4 / 5 - parentWidth * 2 / 5) / 2 + parentWidth * 2 / 5);
                        alignCenterX = (alignment - 4) * parentWidth - (alignment * 2 - 9) * (parentHeight - alignCenterY - 3 * spacingW + parentWidth / 5);
                    } else if ((ringNum % 2) == 0) { // app rings one and three
                        alignCenterX = (alignment * -1 + 5) * parentWidth + (alignment * 2 - 9) * parentWidth * (4 - ringNum) / 5;
                        alignCenterY = parentHeight - 3 * spacingW;
                    } else { // app rings two and four
                        alignCenterX = (alignment * -1 + 5) * parentWidth + (alignment * 2 - 9) * 3 * spacingW;
                        alignCenterY = parentHeight - parentWidth * (5 - ringNum) / 5;
                    }
                } else { // ALIGN_CENTER or undefined
                    if (ringNum <= 3) { // app rings
                        alignCenterY = parentHeight * 3 / 5;
                        alignCenterX = parentWidth / 2;
                        if ((ringNum % 3) != 0) { // shift second app left and third app right
                            alignCenterX += ((ringNum - 1) * 2 - 1) * 7 * spacingW;
                        } else { // shift first app up and fourth app down by same amount second/third apps shifted L/R
                            alignCenterY += ((ringNum - 1) * -2 + 1) / 3 * 7 * spacingW;
                        }
                    } else { // unlock ring(4) and sound toggle ring(5)
                        alignCenterY = parentHeight - mSecRingBottomOffset;
                        alignCenterX = (ringNum - 5) * -1 * parentWidth + (ringNum * 2 - 9) * mSecRingBottomOffset;
                    }
                }
            }

            ring.layout(alignCenterX - hRingWidth, alignCenterY - hRingHeight,
                    alignCenterX + hRingWidth, alignCenterY + hRingHeight);
        }

        /**
         * Ensure all the dependent widgets are measured.
         */
        public void measure() {
            ring.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        }

        public boolean contains(int x, int y) {
            final Drawable ringBackground = ring.getBackground();
            final int ringWidth = ringBackground.getIntrinsicWidth();
            final int ringHeight = ringBackground.getIntrinsicHeight();
            final int hRingWidth = ringWidth / 2;
            final int hRingHeight = ringHeight / 2;
            final int r = (hRingWidth + hRingHeight) / 2;

            final int centerX = ring.getLeft() + (ring.getWidth() / 2);
            final int centerY = ring.getTop() + (ring.getHeight() / 2);

            final int dx = x - centerX;
            final int dy = y - centerY;

            return (dx * dx + dy * dy < r * r);
        }
    }

    public RingSelector(Context context) {
        this(context, null);
    }

    /**
     * Constructor used when this widget is created from a layout file.
     */
    public RingSelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingTab);
        mOrientation = a.getInt(R.styleable.SlidingTab_orientation, HORIZONTAL);
        a.recycle();

        Resources r = getResources();
        mDensity = r.getDisplayMetrics().density;
        if (DBG) log("- Density: " + mDensity);

        int densityDpi = r.getDisplayMetrics().densityDpi;

        /* --copied from RotarySelector.java ;)
         *
         * this hack assumes people change build.prop for increasing
         * the virtual size of their screen by decreasing dpi in
         * build.prop file. this is often done especially for hd
         * phones. keep in mind changing build.prop and density
         * isnt officially supported, but this should do for most cases
         */
        if (densityDpi < 240 && densityDpi > 180)
            mDensityScaleFactor = (float) (240.0 / densityDpi);
        if (densityDpi < 160 && densityDpi > 120)
            mDensityScaleFactor = (float) (160.0 / densityDpi);

        mThresholdRadiusDIP = context.getResources().getInteger(R.integer.config_ringThresholdDIP);
        mThresholdRadius = mDensity * mDensityScaleFactor * mThresholdRadiusDIP;
        mThresholdRadiusSq = mThresholdRadius * mThresholdRadius;

        mBottomOffsetDIP = context.getResources().getInteger(R.integer.config_ringBaselineBottomDIP);
        mCenterOffsetDIP = context.getResources().getInteger(R.integer.config_ringCenterOffsetDIP);
        mSecRingBottomOffsetDIP = context.getResources().getInteger(R.integer.config_ringSecBaselineOffsetDIP);
        mSecRingCenterOffsetDIP = context.getResources().getInteger(R.integer.config_ringSecCenterOffsetDIP);
        mBottomOffset = (int) (mDensity * mDensityScaleFactor * mBottomOffsetDIP);
        mCenterOffset = (int) (mDensity * mDensityScaleFactor * mCenterOffsetDIP);
        mSecRingBottomOffset = (int) (mDensity * mDensityScaleFactor * mSecRingBottomOffsetDIP);
        mSecRingCenterOffset = (int) (mDensity * mDensityScaleFactor * mSecRingCenterOffsetDIP);

        mSecRings = new SecRing[] {
                new SecRing(this, R.drawable.jog_ring_secback_normal), // app1 & also used for phone answer
                new SecRing(this, R.drawable.jog_ring_secback_normal), // app2 & also used for phone decline
                new SecRing(this, R.drawable.jog_ring_secback_normal), // app3
                new SecRing(this, R.drawable.jog_ring_secback_normal), // app4
                new SecRing(this, R.drawable.jog_ring_secback_normal), // unlock ring
                new SecRing(this, R.drawable.jog_ring_secback_normal)  // sound toggle ring
        };

        mMiddleRing = new Ring(this,
                R.drawable.jog_ring_big,
                R.drawable.jog_tab_target_gray);

        mVibrator = (android.os.Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        // setBackgroundColor(0x80808080);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            Log.e("RingSelector", "RingSelector cannot have UNSPECIFIED MeasureSpec"
                    +"(wspec=" + widthSpecMode + ", hspec=" + heightSpecMode + ")",
                    new RuntimeException(TAG + "stack:"));
        }


        mMiddleRing.measure();
        final int middleRingWidth = mMiddleRing.getRingWidth();
        final int middleRingHeight = mMiddleRing.getRingHeight();
        final int width;
        final int height;

        if (isHorizontal()) {
            width = widthSpecSize;
            height = heightSpecSize;
        } else {
            width = widthSpecSize;
            height = heightSpecSize;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        if (mAnimating) {
            return false;
        }

        boolean middleHit = mUseMiddleRing ? mMiddleRing.contains((int) x, (int) y) : false;

        if (!mTracking && !(middleHit)) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mTracking = true;
                mTriggered = false;
                vibrate();
                if (middleHit) {
                    mCurrentRing = mMiddleRing;
                    Log.d(TAG, "mSecRings: " + mSecRings.length);
                    for (SecRing secRing : mSecRings) {
                    	
                        secRing.show();
                    }

                    setGrabbedState(OnRingTriggerListener.MIDDLE_RING);
                }
                mCurrentRing.setState(Ring.STATE_PRESSED);
                mCurrentRing.showTarget();

                setKeepScreenOn(true);

                break;
            }
        }

        return true;
    }

    /**
     * Reset the rings to their original state and stop any existing animation.
     * Animate them back into place if animate is true.
     *
     * @param animate
     */
    public void reset(boolean animate) {
        mMiddleRing.reset(animate);
        if (!animate) {
            mAnimating = false;
        }
    }

    @Override
    public void setVisibility(int visibility) {
        // Clear animations so sliders don't continue to animate when we show the widget again.
        if (visibility != getVisibility() && visibility == View.INVISIBLE) {
           reset(false);
        }
        super.setVisibility(visibility);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTracking) {
            final int action = event.getAction();
            final float x = event.getX();
            final float y = event.getY();

            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    moveRing(x, y);
                    if (mUseMiddleRing && mCurrentRing == mMiddleRing) {
                        for (int q = 0; q < 6; q++) {
                            if (!mSecRings[q].isHidden() && mSecRings[q].contains((int) x, (int) y)) {
                                mSecRings[q].activate();
                            } else {
                                mSecRings[q].deactivate();
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    int secIdx = -1;
                    boolean thresholdReached = false;

                    if (mCurrentRing != mMiddleRing) {
                        int dx = (int) x - mCurrentRing.alignCenterX;
                        int dy = (int) y - mCurrentRing.alignCenterY;
                        thresholdReached = (dx * dx + dy * dy) > mThresholdRadiusSq;
                    }
                    else if (mUseMiddleRing) {
                        for (int q = 0; q < 6; q++) {
                            if (!mSecRings[q].isHidden() && mSecRings[q].contains((int) x, (int) y)) {
                                thresholdReached = true;
                                secIdx = q;
                                break;
                            }
                        }
                    }

                    if (!mTriggered && thresholdReached) {
                        mTriggered = true;
                        mTracking = false;
                        mCurrentRing.setState(Ring.STATE_ACTIVE);

                        dispatchTriggerEvent(OnRingTriggerListener.MIDDLE_RING, secIdx);
                        startAnimating();
                        
                        for (SecRing secRing : mSecRings) {
                            secRing.hide();
                        }
                        
                        setGrabbedState(OnRingTriggerListener.NO_RING);
                        setKeepScreenOn(false);
                        break;
                    }
                    //fall through -- released ring without triggerring
                case MotionEvent.ACTION_CANCEL:
                    mTracking = false;
                    mTriggered = false;
                    mCurrentRing.reset(true);
                    mCurrentRing.hideTarget();
                    mCurrentRing = null;

                    for (SecRing secRing : mSecRings) {
                        secRing.hide();
                    }

                    setGrabbedState(OnRingTriggerListener.NO_RING);
                    setKeepScreenOn(false);
                    break;
            }
        }

        return mTracking || super.onTouchEvent(event);
    }

    void startAnimating() {
        mAnimating = true;
        final Animation trans1, trans2;
        final AnimationSet transSet;
        final Ring ring = mCurrentRing;

        trans1 = new ScaleAnimation(1.0f, 7.5f, 1.0f, 7.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans1.setDuration(ANIM_DURATION);
        trans1.setInterpolator(new AccelerateInterpolator());
        trans1.setFillAfter(true);

        trans2 = new AlphaAnimation(1.0f, 0.2f);
        trans2.setDuration(ANIM_DURATION);
        trans2.setInterpolator(new AccelerateInterpolator());
        trans2.setFillAfter(true);

        transSet = new AnimationSet(false);
        transSet.setDuration(ANIM_DURATION);
        transSet.setAnimationListener(mAnimationDoneListener);
        transSet.addAnimation(trans1);
        transSet.addAnimation(trans2);

        ring.hideTarget();
        ring.startAnimation(transSet);
    }

    private void onAnimationDone() {
        resetView();
        mAnimating = false;
    }

    private boolean isHorizontal() {
        return mOrientation == HORIZONTAL;
    }

    private void resetView() {
        mMiddleRing.reset(false);

        for (SecRing secRing : mSecRings) {
            secRing.reset(false);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed) return;

        // Realign the rings
        mMiddleRing.layout(l, t, r, b, mRingAlignment);

        int nSecRings = 0;
        for (SecRing secRing : mSecRings) {
            if (!secRing.isHidden()) nSecRings++;
        }

        if (nSecRings != 0) {
            for (int q = 0; q < 6; q++) {
                mSecRings[q].layout(l, t, r, b, isHorizontal() ? HORIZONTAL : VERTICAL,
                		q, nSecRings, mRingAlignment);
            }
        }
    }

    private void moveRing(float x, float y) {
        final View ring = mCurrentRing.ring;
        int deltaX = (int) x - ring.getLeft() - (ring.getWidth() / 2);
        int deltaY = (int) y - ring.getTop() - (ring.getHeight() / 2);
        ring.offsetLeftAndRight(deltaX);
        ring.offsetTopAndBottom(deltaY);
        invalidate();
    }

    /**
     * Sets the right ring icon to a given resource.
     *
     * The resource should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param iconId the resource ID of the icon drawable
     * @param targetId the resource of the target drawable
     * @param ringId the resource of the ring drawable
     */
    public void setMiddleRingResources(int targetId, int ringId) {
        mMiddleRing.setTarget(targetId);
        mMiddleRing.setRingBackgroundResource(ringId);
        mMiddleRing.updateDrawableStates();
    }

    /**
     * Sets a certain secondary ring icon to a given resource.
     *
     * The resource should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param ringNum which secondary ring to change (0-3)
     * @param iconId the resource ID of the icon drawable
     * @param ringId the resource of the ring drawable
     */
    public void setSecRingResources(int ringNum, int iconId, int ringId) {
        if (ringNum < 0 || ringNum > 5) return;
        mSecRings[ringNum].setIcon(iconId);
        mSecRings[ringNum].setRingBackgroundResource(ringId);
    }

    public void setSecRingResources(int ringNum, Bitmap icon, int ringId) {
        if (ringNum < 0 || ringNum > 5) return;
        mSecRings[ringNum].setIcon(icon);
        mSecRings[ringNum].setRingBackgroundResource(ringId);
    }

    public void setRingAlignment(int alignment) {
    	mRingAlignment = alignment;
    }
    
    public void hideSecRing(int ringNum) {
        mSecRings[ringNum].setHiddenState(true);

        boolean allHidden = true;
        for (SecRing ring : mSecRings) {
            if (!ring.isHidden()) {
                allHidden = false;
                break;
            }
        }

        if (allHidden) {
            enableMiddleRing(false);
        }

        requestLayout();
    }

    public void showSecRing(int ringNum) {
        mSecRings[ringNum].setHiddenState(false);
        enableMiddleRing(true);
        requestLayout();
    }

    public void enableMiddleRing(boolean enable) {
        mUseMiddleRing = enable;
        mMiddleRing.setHiddenState(!enable);
    }

    /**
     * Triggers haptic feedback.
     */
    private synchronized void vibrate() {
        ContentResolver cr = mContext.getContentResolver();
        final boolean hapticsEnabled = Settings.System.getInt(cr, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 1;
        if (hapticsEnabled) {
            long[] hapFeedback = Settings.System.getLongArray(cr, Settings.System.HAPTIC_DOWN_ARRAY, new long[] { 0 });
            mVibrator.vibrate(hapFeedback, -1);
        }
    }

    /**
     * Registers a callback to be invoked when the user triggers an event.
     *
     * @param listener the OnDialTriggerListener to attach to this view
     */
    public void setOnRingTriggerListener(OnRingTriggerListener listener) {
        mOnRingTriggerListener = listener;
    }

    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichRing the handle that triggered the event.
     */
    private void dispatchTriggerEvent(int whichRing, int whichSecRing) {
        vibrate();
        if (mOnRingTriggerListener != null) {
            mOnRingTriggerListener.onRingTrigger(this, whichRing, whichSecRing);
        }
    }

    /**
     * Sets the current grabbed state, and dispatches a grabbed state change
     * event to our listener.
     */
    private void setGrabbedState(int newState) {
        if (newState != mGrabbedState) {
            mGrabbedState = newState;
            if (mOnRingTriggerListener != null) {
                mOnRingTriggerListener.onGrabbedStateChange(this, mGrabbedState);
            }
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
