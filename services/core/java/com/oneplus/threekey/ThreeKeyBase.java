package com.oneplus.threekey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.Slog;

import com.oneplus.Actions;
import com.oneplus.threekey.ThreeKeyInterface;

public class ThreeKeyBase implements ThreeKeyInterface{

    private static final String TAG = "ThreeKeyBase";

    public static final int SWITCH_STATE_ON = 1;
    public static final int SWITCH_STATE_MIDDLE = 2;
    public static final int SWITCH_STATE_DOWN = 3;

    private int mThreeKeyMode = -1;
    private Context mContext;
    private final BroadcastReceiver mReceiver = new ThreeKeyBroadcastReceiver();

    public ThreeKeyBase(Context context) {
        mContext = context;
        register();
    }

    public boolean isUp() {
        return mThreeKeyMode == SWITCH_STATE_ON;
    }

    public boolean isMiddle() {
        return mThreeKeyMode == SWITCH_STATE_MIDDLE;
    }

    public boolean isDown() {
        return mThreeKeyMode == SWITCH_STATE_DOWN;
    }

    public void init(int switchState) {
        mThreeKeyMode = switchState;
        setSwitchState(mThreeKeyMode);
    }

    public void reset() {
        Slog.d(TAG,"[reset]");
        init(mThreeKeyMode);
    }

    protected void setSwitchState(int switchState) {
        switch (switchState) {
        case SWITCH_STATE_ON:
            setUp();
            break;
        case SWITCH_STATE_MIDDLE:
            setMiddle();
            break;
        case SWITCH_STATE_DOWN:
            setDown();
            break;
        default:
            Slog.e(TAG,"invalid switchState");
            return;
        }
        mThreeKeyMode = switchState;
    }

    // for over ride
    protected void setUp() {}
    protected void setMiddle() {}
    protected void setDown() {}

    private void register() {
        IntentFilter intentFilter = new IntentFilter(Actions.TRI_STATE_KEY_INTENT);
        mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    private class ThreeKeyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Actions.TRI_STATE_KEY_INTENT)) {
                int state = intent.getIntExtra(Actions.TRI_STATE_KEY_INTENT_EXTRA, -1);
                setSwitchState(state);
            }
        }
    }
}
