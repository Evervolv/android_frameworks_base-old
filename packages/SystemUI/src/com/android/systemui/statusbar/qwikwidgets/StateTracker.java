package com.android.systemui.statusbar.qwikwidgets;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The state machine for Wifi and Bluetooth toggling, tracking reality versus
 * the user's intent. This is necessary because reality moves relatively slowly
 * (turning on &amp; off radio drivers), compared to user's expectations.
 */
public abstract class StateTracker {
    // Is the state in the process of changing?
    private boolean mInTransition = false;

    private Boolean mActualState = null; // initially not set

    private Boolean mIntendedState = null; // initially not set

    // Did a toggle request arrive while a state update was
    // already in-flight? If so, the mIntendedState needs to be
    // requested when the other one is done, unless we happened to
    // arrive at that state already.
    private boolean mDeferredStateChangeRequestNeeded = false;

    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_TURNING_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_INTERMEDIATE = 5;
    public static final int STATE_UNKNOWN = 6;
    public static final int STATE_UNAVAILABLE = 7;

    public final void toggleState(Context context) {
        int result = getActualState(context);
        if (result == STATE_DISABLED){
            requestStateChange(context,true);
        } else if (result == STATE_ENABLED){
            requestStateChange(context,false);
        } else {
            // we must be between on and off so we do nothing
        }
    }

    /**
     * Update internal state from a broadcast state change.
     */
    public abstract void onActualStateChange(Context context, Intent intent);

    /**
     * Sets the value that we're now in. To be called from onActualStateChange.
     *
     * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
     *            STATE_TURNING_OFF, STATE_UNKNOWN
     */
    protected final void setCurrentState(Context context, int newState) {
        final boolean wasInTransition = mInTransition;
        switch (newState) {
            case STATE_DISABLED:
                mInTransition = false;
                mActualState = false;
                break;
            case STATE_ENABLED:
                mInTransition = false;
                mActualState = true;
                break;
            case STATE_TURNING_ON:
                mInTransition = true;
                mActualState = false;
                break;
            case STATE_TURNING_OFF:
                mInTransition = true;
                mActualState = true;
                break;
        }

        if (wasInTransition && !mInTransition) {
            if (mDeferredStateChangeRequestNeeded) {
                Log.v("StateTracker", "processing deferred state change");
                if (mActualState != null && mIntendedState != null
                        && mIntendedState.equals(mActualState)) {
                    Log.v("StateTracker", "... but intended state matches, so no changes.");
                } else if (mIntendedState != null) {
                    mInTransition = true;
                    requestStateChange(context, mIntendedState);
                }
                mDeferredStateChangeRequestNeeded = false;
            }
        }
    }

    /**
     * If we're in a transition mode, this returns true if we're transitioning
     * towards being enabled.
     */
    public final boolean isTurningOn() {
        return mIntendedState != null && mIntendedState;
    }

    /**
     * Returns simplified 3-state value from underlying 5-state.
     *
     * @param context
     * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
     */
    public final int getTriState(Context context) {
        /*
         * if (mInTransition) { // If we know we just got a toggle request
         * recently // (which set mInTransition), don't even ask the //
         * underlying interface for its state. We know we're // changing. This
         * avoids blocking the UI thread // during UI refresh post-toggle if the
         * underlying // service state accessor has coarse locking on its //
         * state (to be fixed separately). return
         * PowerButton.STATE_INTERMEDIATE; }
         */
        switch (getActualState(context)) {
            case STATE_DISABLED:
                return STATE_DISABLED;
            case STATE_ENABLED:
                return STATE_ENABLED;
            default:
                return STATE_INTERMEDIATE;
        }
    }

    /**
     * Gets underlying actual state.
     *
     * @param context
     * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
     *         or or STATE_UNKNOWN.
     */
    public abstract int getActualState(Context context);

    /**
     * Actually make the desired change to the underlying radio API.
     */
    protected abstract void requestStateChange(Context context, boolean desiredState);
}