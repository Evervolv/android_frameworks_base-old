package com.android.systemui.widget;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

public class WidgetHelper {
	
	private static final String TAG = "WidgetHelper";
	
    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_TURNING_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_INTERMEDIATE = 5;
    public static final int STATE_UNKNOWN = 6;
    
    public static final int UNKNOWN_WIDGET = -1;
    
    private static final StateTracker sWifiState = new WifiStateTracker();
//    private static final StateTracker sWimaxState = new WimaxStateTracker();
    private static final StateTracker sBluetoothState = new BluetoothStateTracker();
    private static final StateTracker sWifiApState = new WifiApStateTracker();
    
    private static int UsbTetherStatus; // Going to be STATE_DISABLED, STATE_ENABLED, STATE_UNAVAILABLE
    private static String[] mUsbRegexs;
    private static boolean mUsbConnected;
    
	public static void onWidgetClick(String widgetType, Context context) {
		if (widgetType.equals("toggleWifi")) {		
			toggleRadioState(sWifiState, context);
//		} else if (widgetType.equals("toggleWimax")) {
//			toggleRadioState(sWimaxState, context);
		} else if (widgetType.equals("toggleBt")) {
	    	toggleRadioState(sBluetoothState, context);
		} else if (widgetType.equals("toggleWifiAp")) {
			toggleRadioState(sWifiApState, context);
		} else if (widgetType.equals("toggleGps")) {
			toggleGpsState(context);
		} else if (widgetType.equals("toggleSync")) {
			toggleSyncState(context);
		} else if (widgetType.equals("toggleMobileData")) {
			toggleDataState(context);
		} else if (widgetType.equals("toggleUsbTether")) {
			toggleUsbTetherState(context);
		} else if (widgetType.equals("toggleAutoRotate")) {
			toggleOrientationState(context);
		} else if (widgetType.equals("toggleAirplaneMode")) {
			toggleAirplaneModeState(context);
		}
	}
	
	public static void toggleRadioState(StateTracker tracker, Context context) {
    	int result = tracker.getActualState(context);
    	if (result == StateTracker.STATE_DISABLED){
    		tracker.requestStateChange(context,true);
	    } else if (result == StateTracker.STATE_ENABLED){
	    	tracker.requestStateChange(context,false);
	    } else {
	        // we must be between on and off so we do nothing
	    }
	}
	
	public static void onWidgetLongClick(String widgetType, Context context) {
		Log.d(TAG, "onWidgetLongClick: " + widgetType);
		//What do we want to do for long clicks? Open the appropriate settings?
	}
	
	public static int getCurrStatus(String type, Context context) {
		if (type.equals("toggleWifi")) {
			return sWifiState.getActualState(context);
		} else if (type.equals("toggleWifiAp")) {
			return sWifiApState.getActualState(context);
//		} else if (type.equals("toggleWimax")) {
//			return sWimaxState.getActualState(context);
		} else if (type.equals("toggleBt")) {
			return sBluetoothState.getActualState(context);
		} else if (type.equals("toggleUsbTether")) {
			return getUsbTetherState(context);
		} else if (type.equals("toggleGps")) {
			return getGpsState(context) ? STATE_ENABLED : STATE_DISABLED;
		} else if (type.equals("toggleSync")) {
			return getSyncState(context) ? STATE_ENABLED : STATE_DISABLED;
		} else if (type.equals("toggleMobileData")) {
			return getDataState(context) ? STATE_ENABLED : STATE_DISABLED;
		} else if (type.equals("toggleAutoRotate")) {
			return getAutoRotation(context) ? STATE_ENABLED : STATE_DISABLED;
		} else if (type.equals("toggleAirplaneMode")) {
			return getAirplaneModeState(context) ? STATE_ENABLED : STATE_DISABLED;
		}
		
		return UNKNOWN_WIDGET;
	}
	
	
   public static int getCurrStatus(String type, Context context, boolean extra) {
       if (type.equals("toggleUsbTether")) {
           mUsbConnected = extra;
           return getUsbTetherState(context);
       }
       return UNKNOWN_WIDGET;
   }
	//AIRPLANE MODE SPECIFIC METHODS

    public static void toggleAirplaneModeState(Context context) {
        boolean state = getAirplaneModeState(context);
        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
                state ? 0 : 1);
        // notify change
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", state);
        context.sendBroadcast(intent);
    }

	private static boolean getAirplaneModeState(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

	//AUTO ROTATE SPECIFIC METHODS
    protected static void toggleOrientationState(Context context) {
        setAutoRotation(!getAutoRotation(context));
    }

    private static boolean getAutoRotation(Context context) {
        ContentResolver cr = context.getContentResolver();
        return 0 != Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0);
    }

    private static void setAutoRotation(final boolean autorotate) {
        AsyncTask.execute(new Runnable() {
                public void run() {
                    try {
                        IWindowManager wm = IWindowManager.Stub.asInterface(
                                ServiceManager.getService(Context.WINDOW_SERVICE));
                        if (autorotate) {
                            wm.thawRotation();
                        } else {
                            wm.freezeRotation(-1);
                        }
                    } catch (RemoteException exc) {
                        Log.w(TAG, "Unable to save auto-rotate setting");
                    }
                }
            });
    }

	//SYNC SPECIFIC METHODS
    
    /* Need to find new methods for this, 
     * some of these methods have been deprecated
     */
    protected static void toggleSyncState(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean backgroundData = getBackgroundDataState(context);
        boolean sync = ContentResolver.getMasterSyncAutomatically();

        // four cases to handle:
        // setting toggled from off to on:
        // 1. background data was off, sync was off: turn on both
        if (!backgroundData && !sync) {
            connManager.setBackgroundDataSetting(true);
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // 2. background data was off, sync was on: turn on background data
        if (!backgroundData && sync) {
            connManager.setBackgroundDataSetting(true);
        }

        // 3. background data was on, sync was off: turn on sync
        if (backgroundData && !sync) {
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // setting toggled from on to off:
        // 4. background data was on, sync was on: turn off sync
        if (backgroundData && sync) {
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }
	
    private static boolean getBackgroundDataState(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getBackgroundDataSetting();
    }

    private static boolean getSyncState(Context context) {
        boolean backgroundData = getBackgroundDataState(context);
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        return backgroundData && sync;
    }
    
	// GPS SPECIFIC METHODS
    protected static void toggleGpsState(Context context) {
        ContentResolver resolver = context.getContentResolver();
        boolean enabled = getGpsState(context);
        Settings.Secure.setLocationProviderEnabled(resolver,
                LocationManager.GPS_PROVIDER, !enabled);
    }
	
    private static boolean getGpsState(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.Secure.isLocationProviderEnabled(resolver,
                LocationManager.GPS_PROVIDER);
    }
    
    //MOBILE DATA SPECIFIC METHODS
    public static void toggleDataState(Context context) {
        boolean enabled = getDataState(context);

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (enabled) {
            cm.setMobileDataEnabled(false);
        } else {
        	cm.setMobileDataEnabled(true);

        }   
    }
    
    private static boolean getDataState(Context context) {
    	ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            /* Make sure the state change propagates */
            Thread.sleep(100);
        } catch (java.lang.InterruptedException ie) {
        }
        return cm.getMobileDataEnabled();
    }
    
	// WIFI SPECIFIC CLASS
	/**
     * Subclass of StateTracker to get/set Wifi state.
     */
    private static final class WifiStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {
            final WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(TAG, "No wifiManager.");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable tethering if enabling Wifi
                     */
                    int wifiApState = wifiManager.getWifiApState();
                    if (desiredState
                            && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                        wifiManager.setWifiApEnabled(null, false);
                    }

                    wifiManager.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent
                    .getAction())) {
                return;
            }
            int wifiState = intent
                .getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            int widgetState=wifiStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WifiManager's state values into our Wifi/Bluetooth-common
         * state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }

    }
 
	// WIMAX SPECIFIC CLASS
    /**
     * Subclass of StateTracker to get/set WiMAX state.
     */
    /**
     * Converts WimaxController's state values into our
     * WiMAX-common state values.
     */
 /*
    private static final class WimaxStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            if (WimaxHelper.isWimaxSupported(context)) {
                return wimaxStateToFiveState(WimaxHelper.getWimaxState(context));
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(final Context context,
                final boolean desiredState) {
            if (!WimaxHelper.isWimaxSupported(context)) {
                Log.e(TAG, "WiMAX is not supported");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    WimaxHelper.setWimaxEnabled(context, desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wimaxState = intent.getIntExtra(WimaxManagerConstants.CURRENT_WIMAX_ENABLED_STATE, WimaxManagerConstants.WIMAX_ENABLED_STATE_UNKNOWN);
            int widgetState = wimaxStateToFiveState(wimaxState);
            setCurrentState(context, widgetState);
        }

        private static int wimaxStateToFiveState(int wimaxState) {
            switch (wimaxState) {
                case WimaxManagerConstants.WIMAX_ENABLED_STATE_DISABLED:
                    return STATE_DISABLED;
                case WimaxManagerConstants.WIMAX_ENABLED_STATE_ENABLED:
                    return STATE_ENABLED;
                case WimaxManagerConstants.WIMAX_ENABLED_STATE_ENABLING:
                    return STATE_TURNING_ON;
                case WimaxManagerConstants.WIMAX_ENABLED_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }
*/
	// BLUETOOTH SPECIFIC CLASS
    private static final class BluetoothStateTracker extends StateTracker {

        @Override
        public int getActualState(Context context) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                return STATE_UNKNOWN; // On emulator?
            }
            return bluetoothStateToFiveState(mBluetoothAdapter
                    .getState());
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {
            // Actually request the Bluetooth change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if(mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                    } else {
                        mBluetoothAdapter.enable();
                    }
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent
                    .getAction())) {
                return;
            }
            int bluetoothState = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        }

        /**
         * Converts BluetoothAdapter's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    return STATE_DISABLED;
                case BluetoothAdapter.STATE_ON:
                    return STATE_ENABLED;
                case BluetoothAdapter.STATE_TURNING_ON:
                    return STATE_TURNING_ON;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

	// WIFIAP SPECIFIC CLASS
    /**
     * Subclass of StateTracker to get/set Wifi AP state.
     */
    private static final class WifiApStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiApStateToFiveState(wifiManager.getWifiApState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {

            final WifiManager wifiManager = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d("WifiAPManager", "No wifiManager.");
                return;
            }
            Log.i("WifiAp", "Setting: " + desiredState);

            // Actually request the Wi-Fi AP change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable Wif if enabling tethering
                     */
                    int wifiState = wifiManager.getWifiState();
                    if (desiredState
                            && ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
                        wifiManager.setWifiEnabled(false);
                    }

                    wifiManager.setWifiApEnabled(null, desiredState);
                    Log.i("WifiAp", "Async Setting: " + desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {

            if (!WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent
                    .getAction())) {
                return;
            }
            int wifiState = intent
                .getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, -1);
            int widgetState=wifiApStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WifiManager's state values into our Wifi/WifiAP/Bluetooth-common
         * state values.
         */
        private static int wifiApStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_AP_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_AP_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_AP_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_AP_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }
    
    //USB TETHER WIDGET SPECIFIC METHODS
    
    private static void toggleUsbTetherState(Context context) {
    	ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	
    	// have to do this, otherwise UsbTetherStatus will equal 0... Why?
    	getUsbTetherState(context);
    	
        if (UsbTetherStatus == StateTracker.STATE_DISABLED) {
            cm.setUsbTethering(true);
        } else if (UsbTetherStatus == StateTracker.STATE_ENABLED) {
            cm.setUsbTethering(false);
        } else {
        	Log.d(TAG, "toggleState: else");
        }
    }
    
    private static int getUsbTetherState(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        String[] available = cm.getTetherableIfaces();
        String[] tethered = cm.getTetheredIfaces();
        String[] errored = cm.getTetheringErroredIfaces();
        return getUsbTetherState(context, available, tethered, errored);
    }
    
    private static int getUsbTetherState(Context context, String[] available, String[] tethered,
            String[] errored) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        mUsbRegexs = cm.getTetherableUsbRegexs();
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        boolean massStorageActive =
                Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        boolean usbAvailable = mUsbConnected && !massStorageActive;

        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = cm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }

        if (usbTethered) {
        	UsbTetherStatus = StateTracker.STATE_ENABLED;
        	return StateTracker.STATE_ENABLED;
        } else if (usbAvailable) {
        	UsbTetherStatus = StateTracker.STATE_DISABLED;
        	return StateTracker.STATE_DISABLED;
        } else if (usbErrored) {
        	UsbTetherStatus = StateTracker.STATE_UNAVAILABLE;
        	return StateTracker.STATE_UNAVAILABLE;
        } else if (massStorageActive) {
        	UsbTetherStatus = StateTracker.STATE_UNAVAILABLE;
        	return StateTracker.STATE_UNAVAILABLE;
        } else {
        	UsbTetherStatus = StateTracker.STATE_UNAVAILABLE;
        	return StateTracker.STATE_UNAVAILABLE;
        }
    }
    
}
