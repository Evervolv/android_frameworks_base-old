package com.oneplus.threekey;

import android.content.Context;
import android.os.Vibrator;
import android.util.Slog;

import com.oneplus.os.IThreeKeyPolicy;

public class ThreeKeyVibratorPolicy extends IThreeKeyPolicy.Stub {

    private final static String TAG = "ThreeKeyVibratorPolicy";
    private boolean DEBUG = false;

    private Context mContext;
    private Vibrator mVibrator;
    private boolean mInit = false;

    public ThreeKeyVibratorPolicy(Context context) {
        mContext = context;
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void setUp() {
        if(mInit) return;
        if(DEBUG) Slog.d(TAG,"set mode slient");
        mVibrator.vibrate(300);
    }

    @Override
    public void setMiddle() {
        if(mInit) return;
        if(DEBUG) Slog.d(TAG,"set mode dontdisturb");
        mVibrator.vibrate(50);
    }

    @Override
    public void setDown() {
        if(mInit) return;
        if(DEBUG) Slog.d(TAG,"set mode ring");
    }

    @Override
    public void setInitMode(boolean isInit) {
        mInit = isInit;
    }

}
