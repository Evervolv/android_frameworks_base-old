package com.oneplus.os;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.oneplus.os.IOemExService;
import com.oneplus.os.IThreeKeyPolicy;

/** @hide */
public class ThreeKeyManager {
    private static final String TAG = "ThreeKeyManager";
    private static IOemExService sService;

    public ThreeKeyManager(Context context) {
        
    }

    public void disableDefaultThreeKey() {
        android.util.Log.d(TAG,"[disableDefaultThreeKey]");
        try {
            getService().disableDefaultThreeKey();
        } catch(RemoteException e) {
            android.util.Log.e(TAG,"[test] service is unavailable");
        }
    }

    public void enalbeDefaultThreeKey() {
        android.util.Log.d(TAG,"[enalbeDefaultThreeKey]");
        try {
            getService().enalbeDefaultThreeKey();
        } catch(RemoteException e) {
            android.util.Log.e(TAG,"[test] service is unavailable");
        }
    }

    public void addThreeKeyPolicy(IThreeKeyPolicy policy) {
        android.util.Log.d(TAG,"[setThreeKeyPolicyHelper]");
        try {
            getService().addThreeKeyPolicy(policy);
        } catch (RemoteException e) {
            android.util.Log.e(TAG,"[setThreeKeyPolicyHelper] service is unavailable");
        }
    }

    public void removeThreeKeyPolicy(IThreeKeyPolicy policy) {
        android.util.Log.d(TAG,"[removeThreeKeyPolicyHelper]");
        try {
            getService().removeThreeKeyPolicy(policy);
        } catch (RemoteException e) {
            android.util.Log.e(TAG,"[removeThreeKeyPolicyHelper]");
        }
    }

    public void resetThreeKey() {
        android.util.Log.d(TAG,"[resetThreeKey]");
        try {
            getService().resetThreeKey();
        } catch (RemoteException e) {
            android.util.Log.e(TAG,"[resetThreeKey]");
        }
    }

    public int getThreeKeyStatus() {
        android.util.Log.d(TAG,"[getThreeKeyStatus]");
	try {
            return getService().getThreeKeyStatus();
        } catch (RemoteException e) {
            android.util.Log.e("TAG","[getThreeKeyStatus]");
	}
        return 0;
    }
    /** @hide */
    static public IOemExService getService()
    {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService("OEMExService");
        sService = IOemExService.Stub.asInterface(b);
        return sService;
    }
}
