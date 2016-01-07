package com.oneplus.threekey;

import static com.oneplus.Actions.TRI_STATE_KEY_INTENT;
import static com.oneplus.Actions.TRI_STATE_KEY_BOOT_INTENT;
import static com.oneplus.Actions.TRI_STATE_KEY_INTENT_EXTRA;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.provider.Settings;
import android.os.Handler;
import android.util.Slog;

import com.android.server.notification.ManagedServices.UserProfiles;

import com.oneplus.os.ThreeKeyManager;
import com.oneplus.os.IThreeKeyPolicy;

public class ThreeKeyAudioPolicy extends IThreeKeyPolicy.Stub {

    private final static String TAG = "ThreeKeyAudioPolicy";

    private final static boolean DEBUG = true;
    private final static int MAX = 100;
    private final Object mThreeKeySettingsLock = new Object();
    private Context mContext;
    private NotificationManager mNotificationManager;
    private AudioManager mAudioManager;
    private ThreeKeyManager mThreeKeyManager;

    // setting parameter
    private boolean mMuteMediaFlag;
    private boolean mVibrateFlag;
    private boolean mOptionChangeFlag;

    private boolean mInitFlag = false;

    private SettingsObserver mSettingsObserver;

    public ThreeKeyAudioPolicy(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mThreeKeyManager = (ThreeKeyManager) context.getSystemService(Context.THREEKEY_SERVICE);
        mSettingsObserver = new SettingsObserver();
        mSettingsObserver.observe();
        mOptionChangeFlag = false;

    }

    @Override
    public void setUp() {
       synchronized (mThreeKeySettingsLock) {
        setSlient();
       }
    }

    @Override
    public void setMiddle() {
       synchronized (mThreeKeySettingsLock) {
        setDontDisturb();
       }
    }

    @Override
    public void setDown() {
       synchronized (mThreeKeySettingsLock) {
          setRing();
       }      
    }

    @Override
    public void setInitMode(boolean isInit) {
        mInitFlag = isInit;
    }

    public void setSlient() {
        if(DEBUG) {
            Slog.d(TAG,"set mode slient");
            Slog.d(TAG,"mVibrateFlag " + mVibrateFlag + " mMuteMediaFlag " + mMuteMediaFlag);
        }
   
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_ALARMS, null, TAG);
        Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.ZEN_MODE, Settings.Global.ZEN_MODE_ALARMS);             
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);

        // we don't change the music stream volume if this operation is cased by option changed
        if(mOptionChangeFlag) {
            mOptionChangeFlag = false;
            return;
        }

        // we don't change the music stream volume when we are booting
        if(mInitFlag) {
            return;
        }
    }

    public void setDontDisturb() {
        if(DEBUG) {
            Slog.d(TAG,"set mode dontdisturb");
            Slog.d(TAG,"mVibrateFlag " + mVibrateFlag + " mMuteMediaFlag " + mMuteMediaFlag);
        }

        cleanAbnormalState();
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
        Settings.Global.putInt(mContext.getContentResolver(),
                         Settings.Global.ZEN_MODE, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        if(mOptionChangeFlag) {
            mOptionChangeFlag = false;
            return;
        }
    }

    public void setRing() {
        if(DEBUG) {
            Slog.d(TAG,"set mode ring");
            Slog.d(TAG,"mVibrateFlag " + mVibrateFlag + " mMuteMediaFlag " + mMuteMediaFlag);
        }

        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF,null,TAG);
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.ZEN_MODE, Settings.Global.ZEN_MODE_OFF);  

        if(mOptionChangeFlag) {
            mOptionChangeFlag = false;
            return;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE = Settings.Global.getUriFor(Settings.Global.ZEN_MODE);       

        public SettingsObserver() {
            super(new Handler());
        }

        public void observe() {
            final ContentResolver resolver = mContext.getContentResolver();         
            resolver.registerContentObserver(ZEN_MODE, false /*notifyForDescendents*/, this);

        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mOptionChangeFlag = true;
        }
    }

    private void muteSpeakerMediaVolume() {
        //TODO
    }

    private void restoreSpeakerMediaVolume() {
        //TODO
    }

    private void cleanAbnormalState() {
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING,AudioManager.ADJUST_UNMUTE,0);
    }
}
