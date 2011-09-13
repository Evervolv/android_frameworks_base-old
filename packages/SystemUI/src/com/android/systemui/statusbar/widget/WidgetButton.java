package com.android.systemui.statusbar.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;

public abstract class WidgetButton {

	public static final String TAG = "WidgetButton";
	
	public static final String BUTTON_WIFI = "toggleWifi";
	public static final String BUTTON_WIMAX = "toggleWimax";
	public static final String BUTTON_GPS = "toggleGps";
	public static final String BUTTON_SYNC = "toggleSync";
	public static final String BUTTON_UNKNOWN = "unknown";
	
    // this is a list of all of our buttons and their corresponding classes
    private static final HashMap<String, Class<? extends WidgetButton>> BUTTONS = new HashMap<String, Class<? extends WidgetButton>>();
    static {
        BUTTONS.put(BUTTON_WIMAX, WidgetWimax.class);
        BUTTONS.put(BUTTON_WIFI, WidgetWifi.class);
        BUTTONS.put(BUTTON_GPS, WidgetGps.class);
        BUTTONS.put(BUTTON_SYNC, WidgetSync.class);
    }
    
    // this is a list of our currently loaded buttons
    private static final HashMap<String, WidgetButton> BUTTONS_LOADED = new HashMap<String, WidgetButton>();

    protected int mIcon;
    protected int mState;
    protected String mName;
    protected View mView;
    protected String mType = BUTTON_UNKNOWN;
    
    // a static onclicklistener that can be set to register a callback when ANY button is clicked
    private static View.OnClickListener GLOBAL_ON_CLICK_LISTENER = null;

    // a static onlongclicklistener that can be set to register a callback when ANY button is long clicked
    private static View.OnLongClickListener GLOBAL_ON_LONG_CLICK_LISTENER = null;
    
    // we use this to ensure we update our views on the UI thread
    private Handler mViewUpdateHandler = new Handler() {
            public void handleMessage(Message msg) {
                // this is only used to update the view, so do it
                if(mView != null) {
                    Context context = mView.getContext();
                    Resources res = context.getResources();
                    int buttonLayout = R.id.widget_layout;
                    int buttonIcon = R.id.widget_icon;
                    int buttonState = R.id.widget_indic;
                    int buttonName = R.id.widget_name;
                    ImageView indic = (ImageView)mView.findViewById(R.id.widget_indic);

                    updateImageView(buttonIcon, mIcon);

                    /* Button State */
                    switch(mState) {
                        case StateTracker.STATE_ENABLED:
                            updateImageView(buttonState,
                                    R.drawable.widget_indic_on);
                            break;
                        case StateTracker.STATE_DISABLED:
                            updateImageView(buttonState,
                                    R.drawable.widget_indic_off);
                            break;
                        default:
                            updateImageView(buttonState,
                                    R.drawable.widget_indic_tween);
                            break;
                    }
                }
            }
    };
        
    protected void updateView() {
        mViewUpdateHandler.sendEmptyMessage(0);
    }

    private void updateImageView(int id, int resId) {
        ImageView imageIcon = (ImageView)mView.findViewById(id);
        imageIcon.setImageResource(resId);
    }
    
    protected abstract void updateState();
    protected abstract void toggleState();
    protected abstract boolean handleLongClick();

    protected void update() {
        updateState();
        updateView();
    }

    protected void onReceive(Context context, Intent intent) {
        // do nothing as a standard, override this if the button needs to respond
        // to broadcast events from the StatusBarService broadcast receiver
    }

    protected void onChangeUri(Uri uri) {
        // do nothing as a standard, override this if the button needs to respond
        // to a changed setting
    }

    protected IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }

    protected List<Uri> getObservedUris() {
        return new ArrayList<Uri>();
    }

    protected void setupButton(View view) {
        mView = view;
        if(mView != null) {
            mView.setTag(mType);
            mView.setOnClickListener(mClickListener);
            mView.setOnLongClickListener(mLongClickListener);
        }
    }
    
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            String type = (String)v.getTag();

            for(Map.Entry<String, WidgetButton> entry : BUTTONS_LOADED.entrySet()) {
                if(entry.getKey().equals(type)) {
                    entry.getValue().toggleState();
                    break;
                }
            }

            // call our static listener if it's set
            if(GLOBAL_ON_CLICK_LISTENER != null) {
                GLOBAL_ON_CLICK_LISTENER.onClick(v);
            }
        }
    };

    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            boolean result = false;
            String type = (String)v.getTag();
            for (Map.Entry<String, WidgetButton> entry : BUTTONS_LOADED.entrySet()) {
                if(entry.getKey().endsWith(type)) {
                    result = entry.getValue().handleLongClick();
                    break;
                }
            }

            if(result && GLOBAL_ON_LONG_CLICK_LISTENER != null) {
                GLOBAL_ON_LONG_CLICK_LISTENER.onLongClick(v);
            }
            return result;
        }
    };
    
    public static boolean loadButton(String key, View view) {
        // first make sure we have a valid button
        if(BUTTONS.containsKey(key) && view != null) {
            synchronized (BUTTONS_LOADED) {
                if(BUTTONS_LOADED.containsKey(key)) {
                    // setup the button again
                    BUTTONS_LOADED.get(key).setupButton(view);
                } else {
                    try {
                        // we need to instantiate a new button and add it
                        WidgetButton pb = BUTTONS.get(key).newInstance();
                        // set it up
                        pb.setupButton(view);
                        // save it
                        BUTTONS_LOADED.put(key, pb);
                    } catch(Exception e) {
                        Log.e(TAG, "Error loading button: " + key, e);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static void unloadButton(String key) {
        synchronized (BUTTONS_LOADED) {
            // first make sure we have a valid button
            if(BUTTONS_LOADED.containsKey(key)) {
                // wipe out the button view
                BUTTONS_LOADED.get(key).setupButton(null);
                // remove the button from our list of loaded ones
                BUTTONS_LOADED.remove(key);
            }
        }
    }

    public static void unloadAllButtons() {
        synchronized (BUTTONS_LOADED) {
            // cycle through setting the buttons to null
            for(WidgetButton pb : BUTTONS_LOADED.values()) {
                pb.setupButton(null);
            }

            // clear our list
            BUTTONS_LOADED.clear();
        }
    }

    public static void updateAllButtons() {
        synchronized (BUTTONS_LOADED) {
            // cycle through our buttons and update them
            for(WidgetButton pb : BUTTONS_LOADED.values()) {
                pb.update();
            }
        }
    }

    // glue for broadcast receivers
    public static IntentFilter getAllBroadcastIntentFilters() {
        IntentFilter filter = new IntentFilter();

        synchronized(BUTTONS_LOADED) {
            for(WidgetButton button : BUTTONS_LOADED.values()) {
                IntentFilter tmp = button.getBroadcastIntentFilter();

                // cycle through these actions, and see if we need them
                int num = tmp.countActions();
                for(int i = 0; i < num; i++) {
                    String action = tmp.getAction(i);
                    if(!filter.hasAction(action)) {
                        filter.addAction(action);
                    }
                }
            }
        }

        // return our merged filter
        return filter;
    }

    // glue for content observation
    public static List<Uri> getAllObservedUris() {
        List<Uri> uris = new ArrayList<Uri>();

        synchronized(BUTTONS_LOADED) {
            for(WidgetButton button : BUTTONS_LOADED.values()) {
                List<Uri> tmp = button.getObservedUris();

                for(Uri uri : tmp) {
                    if(!uris.contains(uri)) {
                        uris.add(uri);
                    }
                }
            }
        }

        return uris;
    }

    public static void handleOnReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // cycle through power buttons
        synchronized(BUTTONS_LOADED) {
            for(WidgetButton button : BUTTONS_LOADED.values()) {
                // call "onReceive" on those that matter
                if(button.getBroadcastIntentFilter().hasAction(action)) {
                    button.onReceive(context, intent);
                }
            }
        }
    }

    public static void handleOnChangeUri(Uri uri) {
        synchronized(BUTTONS_LOADED) {
            for(WidgetButton button : BUTTONS_LOADED.values()) {
                if(button.getObservedUris().contains(uri)) {
                    button.onChangeUri(uri);
                }
            }
        }
    }

    public static void setGlobalOnClickListener(View.OnClickListener listener) {
        GLOBAL_ON_CLICK_LISTENER = listener;
    }

    public static void setGlobalOnLongClickListener(View.OnLongClickListener listener) {
        GLOBAL_ON_LONG_CLICK_LISTENER = listener;
    }

    protected static WidgetButton getLoadedButton(String key) {
        synchronized(BUTTONS_LOADED) {
            if(BUTTONS_LOADED.containsKey(key)) {
                return BUTTONS_LOADED.get(key);
            } else {
                return null;
            }
        }
    }
}
