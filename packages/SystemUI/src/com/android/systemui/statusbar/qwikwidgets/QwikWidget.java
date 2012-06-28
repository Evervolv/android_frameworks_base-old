package com.android.systemui.statusbar.qwikwidgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

public abstract class QwikWidget {

    public static final String TAG = "ToolboxWidget";

    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_TURNING_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_INTERMEDIATE = 5;
    public static final int STATE_UNKNOWN = 6;
    public static final int STATE_UNAVAILABLE = 7;

    public static final String WIDGET_WIFI = "toggleWifi";
    public static final String WIDGET_WIFIAP = "toggleWifiAp";
    public static final String WIDGET_BLUETOOTH = "toggleBluetooth";
    public static final String WIDGET_SYNC = "toggleSync";
    public static final String WIDGET_AUTOROTATE = "toggleAutoRotate";
    public static final String WIDGET_AIRPLANEMODE = "toggleAirplaneMode";
    public static final String WIDGET_GPS = "toggleGps";
    public static final String WIDGET_MOBILEDATA = "toggleMobileData";
    public static final String WIDGET_USBTETHER = "toggleUsbTether";
    public static final String WIDGET_WIMAX = "toggleWimax";
    public static final String WIDGET_NOTIFICATIONS = "toggleNotifications";

    public static final String WIDGET_BRIGHTNESS = "toggleBrightness";
    public static final String WIDGET_UNKNOWN = "unknown";

    public static final int STYLE_UNKNOWN = -1;
    public static final int STYLE_TOGGLE = 1;
    public static final int STYLE_TOGGLE_SLIDER = 2;

    private static final HashMap<String, Class<? extends QwikWidget>> WIDGETS = new HashMap<String, Class<? extends QwikWidget>>();
    static {
        WIDGETS.put(WIDGET_WIFI, WifiWidget.class);
        WIDGETS.put(WIDGET_WIFIAP, WifiApWidget.class);
        WIDGETS.put(WIDGET_BLUETOOTH, BluetoothWidget.class);
        WIDGETS.put(WIDGET_SYNC, SyncWidget.class);
        WIDGETS.put(WIDGET_AUTOROTATE, AutoRotateWidget.class);
        WIDGETS.put(WIDGET_AIRPLANEMODE, AirplaneModeWidget.class);
        WIDGETS.put(WIDGET_BRIGHTNESS, BrightnessWidget.class);
        WIDGETS.put(WIDGET_GPS, GpsWidget.class);
        WIDGETS.put(WIDGET_MOBILEDATA, MobileDataWidget.class);
        WIDGETS.put(WIDGET_USBTETHER, UsbTetherWidget.class);
        WIDGETS.put(WIDGET_WIMAX, WimaxWidget.class);
        WIDGETS.put(WIDGET_NOTIFICATIONS, NotificationsWidget.class);
    }

    protected static final HashMap<String, QwikWidget> WIDGETS_LOADED = new HashMap<String, QwikWidget>();

    protected View mWidgetView;

    protected String mType = WIDGET_UNKNOWN;
    protected int mStyle = STYLE_UNKNOWN;

    protected int mState;

    protected void onReceive(Context context, Intent intent) { /*do nothing*/ }
    protected void onChangeUri(Uri uri) { /*do nothing*/ }
    protected void updateWidgetView() { /*do nothing*/ }

    protected abstract void updateState();
    protected abstract void toggleState();
    protected abstract boolean handleLongClick();

    public static void updateAllWidgets() {
        synchronized (WIDGETS_LOADED) {
            for(QwikWidget widget : WIDGETS_LOADED.values()) {
                widget.update();
            }
        }
    }

    private Handler mViewUpdateHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(mWidgetView != null) {
                updateWidgetView();
            }
        }
    };

    protected void update() {
        updateState();
        updateView();
    }

    protected void updateView() {
        mViewUpdateHandler.sendEmptyMessage(0);
    }

    protected void setupWidget(View view) {
        mWidgetView = view;
        updateWidgetView();
        if(mWidgetView != null) {
            mWidgetView.setTag(mType);
        }
        update();
    }

    public static boolean loadWidget(String key, View view) {
        if(WIDGETS.containsKey(key) && view != null) {
            synchronized (WIDGETS_LOADED) {
                if(WIDGETS_LOADED.containsKey(key)) {
                    WIDGETS_LOADED.get(key).setupWidget(view);
                } else {
                    try {
                        QwikWidget widget = WIDGETS.get(key).newInstance();
                        widget.setupWidget(view);
                        WIDGETS_LOADED.put(key, widget);
                    } catch(Exception e) {
                        Log.e(TAG, "Error loading widget: " + key, e);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    protected IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }

    protected List<Uri> getObservedUris() {
        return new ArrayList<Uri>();
    }

    public static IntentFilter getAllBroadcastIntentFilters() {
        IntentFilter filter = new IntentFilter();

        synchronized(WIDGETS_LOADED) {
            for(QwikWidget widget : WIDGETS_LOADED.values()) {
                IntentFilter tmp = widget.getBroadcastIntentFilter();
                int num = tmp.countActions();
                for(int i = 0; i < num; i++) {
                    String action = tmp.getAction(i);
                    if(!filter.hasAction(action)) {
                        filter.addAction(action);
                    }
                }
            }
        }
        return filter;
    }

    public static List<Uri> getAllObservedUris() {
        List<Uri> uris = new ArrayList<Uri>();
        synchronized(WIDGETS_LOADED) {
            for(QwikWidget widget : WIDGETS_LOADED.values()) {
                List<Uri> tmp = widget.getObservedUris();
                for(Uri uri : tmp) {
                    if(!uris.contains(uri)) {
                        uris.add(uri);
                    }
                }
            }
        }
        return uris;
    }

    public static void unloadButton(String key) {
        synchronized (WIDGETS_LOADED) {
            if(WIDGETS_LOADED.containsKey(key)) {
                WIDGETS_LOADED.get(key).setupWidget(null);
                WIDGETS_LOADED.remove(key);
            }
        }
    }

    public static void unloadAllWidgets() {
        synchronized (WIDGETS_LOADED) {
            for(QwikWidget widget : WIDGETS_LOADED.values()) {
                widget.setupWidget(null);
            }
            WIDGETS_LOADED.clear();
        }
    }

    public static void handleOnReceive(Context context, Intent intent) {
        String action = intent.getAction();
        synchronized(WIDGETS_LOADED) {
            for(QwikWidget widget : WIDGETS_LOADED.values()) {
                if(widget.getBroadcastIntentFilter().hasAction(action)) {
                    widget.onReceive(context, intent);
                }
            }
        }
    }

    public static void handleOnChangeUri(Uri uri) {
        synchronized(WIDGETS_LOADED) {
            for(QwikWidget widget : WIDGETS_LOADED.values()) {
                if(widget.getObservedUris().contains(uri)) {
                    widget.onChangeUri(uri);
                }
            }
        }
    }

    protected View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            String type = (String)v.getTag();
            for(Map.Entry<String, QwikWidget> entry : WIDGETS_LOADED.entrySet()) {
                if(entry.getKey().equals(type)) {

                    entry.getValue().toggleState();
                    break;
                }
            }
        }
    };

    protected View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            boolean result = false;
            String type = (String)v.getTag();
            for (Map.Entry<String, QwikWidget> entry : WIDGETS_LOADED.entrySet()) {
                if(entry.getKey().endsWith(type)) {
                    result = entry.getValue().handleLongClick();
                    break;
                }
            }
            return result;
        }
    };

}
