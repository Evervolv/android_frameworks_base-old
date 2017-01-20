package com.oneplus.threekey;

import android.content.IntentFilter;
import com.oneplus.Actions;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.UserHandle;
import com.oneplus.threekey.ThreeKeyInterface;
import android.util.Slog;
import android.content.Context;

public class ThreeKeyBase implements ThreeKeyInterface{
    private static final String TAG = "ThreeKeyBase";
    private static final String ACTION_THREE_KEY = Actions.TRI_STATE_KEY_INTENT;
    private static final String ACTION_THREE_KEY_EXTRA = Actions.TRI_STATE_KEY_INTENT_EXTRA;

    public static final int SWITCH_STATE_ON = 1;
    public static final int SWITCH_STATE_MIDDLE = 2;
    public static final int SWITCH_STATE_DOWN = 3;
    public static final int SWITCH_STATE_UNINIT = -1;

    private int mThreeKeyMode = SWITCH_STATE_UNINIT;
    private Context mContext;
    private final BroadcastReceiver mReceiver = new ThreeKeyBroadcastReceiver();

    public ThreeKeyBase(Context context) {
        mContext = context;
        register();
    }

    public boolean isUp() { return mThreeKeyMode == SWITCH_STATE_ON; }
    public boolean isMiddle()  { return mThreeKeyMode == SWITCH_STATE_MIDDLE;}
    public boolean isDown() { return mThreeKeyMode == SWITCH_STATE_DOWN; }

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
        IntentFilter intentFilter = new IntentFilter(ACTION_THREE_KEY);
        mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, intentFilter, null, null);
        //mContext.registerReceiver(mReceiver,intentFilter, null, null);
    }

    private class ThreeKeyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_THREE_KEY)) {
                int state = intent.getIntExtra(ACTION_THREE_KEY_EXTRA, -1);
                setSwitchState(state);
            }
        }
    }
}
