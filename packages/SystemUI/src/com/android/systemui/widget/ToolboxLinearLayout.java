package com.android.systemui.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.R;

public class ToolboxLinearLayout extends LinearLayout {

	private static final String TAG = "ToolboxLinearLayout";
	
	ViewFlipper mToolboxFlipper;
	
	private GridView mWidgetGrid;
	private WidgetAdapter mWidgetAdapter;
	private List<WidgetItems> widgetList = new ArrayList<WidgetItems>();
    
	private static ImageView mBatteryIndic;
	private static ImageView mBatteryCharging;
	private static TextView mBatteryText;
	
	private AudioManager audioManager;
	private SeekBar mediaVolume;
	private SeekBar ringVolume;
	private ImageButton mPlayIcon;
	private ImageButton mPauseIcon;
	private ImageButton mPrevIcon;
	private ImageButton mNextIcon;
	private Switch mSilentRingtone;
	
	private Handler mHandler = new Handler();

    private TextView mStatus;
    private TextView mPower;
    private TextView mLevel;
    private TextView mScale;
    private TextView mHealth;
    private TextView mVoltage;
    private TextView mTemperature;
    private TextView mTechnology;
	
	private ToolboxBroadcastReceiver mBroadcastReceiver = 
		new ToolboxBroadcastReceiver();
	
	private WidgetSettingsObserver mObserver = 
		new WidgetSettingsObserver(mHandler);
	
    public ToolboxLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWidgetGrid = (GridView) findViewById(R.id.widget_grid);
		mToolboxFlipper = (ViewFlipper) findViewById(R.id.toolbox_flipper);
		loadWidgets();
	    mObserver.observe();
		initMediaTool();
		initPowerTool();
		getContext().registerReceiver(mBroadcastReceiver,
				getBroadcastFilter());
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

	private void initPowerTool() {
		mBatteryIndic = (ImageView) findViewById(R.id.batteryTab);
		mBatteryCharging = (ImageView) findViewById(R.id.batteryTabCharging);
		mBatteryText = (TextView) findViewById(R.id.batteryTabText);
		
        mStatus = (TextView)findViewById(R.id.status);
        mPower = (TextView)findViewById(R.id.power);
        mLevel = (TextView)findViewById(R.id.level);
        mScale = (TextView)findViewById(R.id.scale);
        mHealth = (TextView)findViewById(R.id.health);
        mTechnology = (TextView)findViewById(R.id.technology);
        mVoltage = (TextView)findViewById(R.id.voltage);
        mTemperature = (TextView)findViewById(R.id.temperature);
	}
	
	private void initMediaTool() {
	    audioManager = (AudioManager) getContext().getSystemService(
	    		Context.AUDIO_SERVICE);
	    
	    mediaVolume = (SeekBar)findViewById(R.id.media_seekbar);
	    mediaVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mediaVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
	    mediaVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	        @Override
	        public void onStopTrackingTouch(SeekBar arg0) {
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar arg0) {
	        }

	        @Override
	        public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
	            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
	        }
	    });
	    
	    ringVolume = (SeekBar)findViewById(R.id.ringtone_seekbar);
	    ringVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        ringVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_RING));
	    ringVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	        @Override
	        public void onStopTrackingTouch(SeekBar arg0) {
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar arg0) {
	        }

	        @Override
	        public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
	            audioManager.setStreamVolume(AudioManager.STREAM_RING, arg1, 0);
	        }
	    });

	    mSilentRingtone = (Switch) findViewById(R.id.silent_ring_switch);
	    mSilentRingtone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (!isChecked) {
					boolean vibeInSilent = (Settings.System.getInt(
							mContext.getContentResolver(),
		                    Settings.System.VIBRATE_IN_SILENT, 1) == 1);
	                audioManager.setRingerMode(
	                		vibeInSilent ? AudioManager.RINGER_MODE_VIBRATE
	                                     : AudioManager.RINGER_MODE_SILENT);
					ringVolume.setEnabled(false);
				} else {
					audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					ringVolume.setEnabled(true);
				}
            }
        });

	    mPlayIcon = (ImageButton) findViewById(R.id.imageView2);
        mPlayIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!audioManager.isMusicActive()) {
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    updateMusicControls();
                }
            }
        });
        
	    mPauseIcon = (ImageButton) findViewById(R.id.imageView3);
        mPauseIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioManager.isMusicActive()) {
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    updateMusicControls();
                }
            }  
        });
        
	    mPrevIcon = (ImageButton) findViewById(R.id.imageView1);
        mPrevIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        });
        
        mNextIcon = (ImageButton) findViewById(R.id.imageView4);
        mNextIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        });

        updateMusicControls();
	}

	private void updateMusicControls() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {

		}

		if (audioManager.isMusicActive()) {
	        mPlayIcon.setVisibility(View.GONE);
	        mPauseIcon.setVisibility(View.VISIBLE);
	        mPrevIcon.setVisibility(View.VISIBLE);
	        mNextIcon.setVisibility(View.VISIBLE);
		} else {
	        mPlayIcon.setVisibility(View.VISIBLE);
	        mPauseIcon.setVisibility(View.GONE);
	        mPrevIcon.setVisibility(View.GONE);
	        mNextIcon.setVisibility(View.GONE);
		}
	}

    private void sendMediaButtonEvent(int code) {
        long eventtime = SystemClock.uptimeMillis();

        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, code, 0);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        getContext().sendOrderedBroadcast(downIntent, null);

        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, code, 0);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        getContext().sendOrderedBroadcast(upIntent, null);
    }

    private void loadWidgets() {
        
        String widgetsList = Settings.System.getString(getContext()
                .getContentResolver(), Settings.System.SELECTED_TOOLBOX_WIDGETS);
        if (widgetsList == null) {
            widgetsList = getResources().getString(R.string.default_toolbox_widgets);
        }
        /* Clear the list! */
        widgetList.clear();
		for (String widget: getWidgetListFromString(widgetsList)) {
		    WidgetItems widgets = null;
			if (widget.equals("")) {
			    continue;
			} else if (widget.equals("toggleWifi")) {
			    widgets = new WidgetItems(widget, R.string.widget_wifi_title,
			            R.drawable.widget_wifi_on,
			            R.drawable.widget_wifi_off,
			            R.drawable.widget_wifi_tween);
			} else if (widget.equals("toggleGps")) {
                widgets = new WidgetItems(widget, R.string.widget_gps_title,
                        R.drawable.widget_gps_on,
                        R.drawable.widget_gps_off);
			} else if (widget.equals("toggleWimax")) {
                widgets = new WidgetItems(widget, R.string.widget_wimax_title,
                        R.drawable.widget_wimax_on,
                        R.drawable.widget_wimax_off,
                        R.drawable.widget_wimax_tween);
			} else if (widget.equals("toggleBt")) {
                widgets = new WidgetItems(widget, R.string.widget_bt_title,
                        R.drawable.widget_bt_on,
                        R.drawable.widget_bt_off,
                        R.drawable.widget_bt_tween);
			} else if (widget.equals("toggleSync")) {
                widgets = new WidgetItems(widget, R.string.widget_sync_title,
                        R.drawable.widget_sync_on,
                        R.drawable.widget_sync_off);
			} else if (widget.equals("toggleWifiAp")) {
                widgets = new WidgetItems(widget, R.string.widget_wifiap_title,
                        R.drawable.widget_wifiap_on,
                        R.drawable.widget_wifiap_off,
                        R.drawable.widget_wifiap_tween);
			} else if (widget.equals("toggleMobileData")) {
                widgets = new WidgetItems(widget, R.string.widget_mobdata_title,
                        R.drawable.widget_mobdata_on,
                        R.drawable.widget_mobdata_off);
			} else if (widget.equals("toggleUsbTether")) {
                widgets = new WidgetItems(widget, R.string.widget_usbtether_title,
                        R.drawable.widget_usbtether_on,
                        R.drawable.widget_usbtether_available,0,
                        R.drawable.widget_usbtether_unavailable);
			} else if (widget.equals("toggleAutoRotate")) {
                widgets = new WidgetItems(widget, R.string.widget_autorotate_title,
                        R.drawable.widget_autorotate_on,
                        R.drawable.widget_autorotate_off);
			} else if (widget.equals("toggleAirplaneMode")) {
                widgets = new WidgetItems(widget, R.string.widget_airplane_title,
                        R.drawable.widget_airplane_on,
                        R.drawable.widget_airplane_off);
			}
			widgetList.add(widgets);
		}

		mWidgetAdapter = new WidgetAdapter(getContext(),
				R.layout.widget_button, widgetList);

		mWidgetGrid.setAdapter(mWidgetAdapter);
		mWidgetGrid.setOnItemClickListener(new OnItemClickListener() {
		      public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		    	  WidgetHelper.onWidgetClick(widgetList.get(position).widgetType, getContext());
		      }
		});

        mWidgetGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent_view, View button, int position, long id) {
            	WidgetHelper.onWidgetLongClick(widgetList.get(position).widgetType, getContext());
                return true;
            }
        });

		getContext().registerReceiver(mBroadcastReceiver,
				getBroadcastFilter());

		updateAllWidgetViews();
    }

    public static ArrayList<String> getWidgetListFromString(String widgets) {
        return new ArrayList<String>(Arrays.asList(widgets.split("\\|")));
    }

    private void updateAllWidgetViews() {
    	Context context = getContext();
    	for (int i = 0; i < widgetList.size(); i++) {
    		String type = widgetList.get(i).widgetType;
    		updateWidgetView(type, WidgetHelper.getCurrStatus(type, context));
    	}
    }
    
    private int getPositionOfWidget(String type) {
    	
    	for (int i = 0; i < widgetList.size(); i++) {
    		if (widgetList.get(i).widgetType.equals(type)) {
    			return i;
    		}
    	}
    	
    	return -1;
    }
    
    private void updateWidgetView(String whichWidget, int state) {
    	widgetList.get(getPositionOfWidget(whichWidget)).setWidgetState(state);
    	mWidgetGrid.setAdapter(mWidgetAdapter);
    	mWidgetGrid.invalidateViews();
    }

    public static void updateBatteryUI(int status, int level) {
    	
    	mBatteryIndic.setImageResource(getBatteryResource(level));
    	
    	mBatteryText.setText(level + "%");
    	
    	if (status == BatteryManager.BATTERY_PLUGGED_AC || 
    			status == BatteryManager.BATTERY_PLUGGED_USB) {
    		mBatteryCharging.setVisibility(View.VISIBLE);
    	} else {
    		mBatteryCharging.setVisibility(View.INVISIBLE);
    	}
    }

    public static int getBatteryResource(Integer status) {
        if (status == null)
            return R.drawable.batt_0;
        if (status <= 10)
            return R.drawable.batt_10;
        if (status <= 20)
            return R.drawable.batt_20;
        if (status <= 30)
            return R.drawable.batt_30;
        if (status <= 40)
            return R.drawable.batt_40;
        if (status <= 50)
            return R.drawable.batt_50;
        if (status <= 60)
            return R.drawable.batt_60;
        if (status <= 70)
            return R.drawable.batt_70;
        if (status <= 80)
            return R.drawable.batt_80;
        if (status <= 90)
            return R.drawable.batt_90;
        
        return R.drawable.batt_100;
    }
    
    /**
     * Format a number of tenths-units as a decimal string without using a
     * conversion to float.  E.g. 347 -> "34.7"
     */
    private final String tenthsToFixedString(int x) {
        int tens = x / 10;
        return Integer.toString(tens) + "." + (x - 10 * tens);
    }
    
    private class ToolboxBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(AudioManager.VOLUME_CHANGED_ACTION) || 
        			intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
        		mediaVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        		ringVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_RING));
                boolean silent = (audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
                mSilentRingtone.setChecked(!silent);
        	} else if (intent.getAction().equals("com.android.music.playstatechanged") || 
        			intent.getAction().equals("com.nullsoft.winamp.playstatechanged")) {
        		updateMusicControls();
        	} else if (intent.getAction().equals("com.amazon.mp3.playstatechanged")) {
        		updateMusicControls();
            } else if (intent.getAction().equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION")) {
                updateMusicControls();
            } else if (intent.getAction().equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")) {
                updateMusicControls();
        	} else if (intent.getAction().equals(WifiManager
        			.WIFI_STATE_CHANGED_ACTION)){
        		updateWidgetView("toggleWifi", WidgetHelper
        				.getCurrStatus("toggleWifi", context));
//        	} else if (intent.getAction().equals(WimaxManagerConstants
//        			.WIMAX_ENABLED_CHANGED_ACTION)) {
//        		updateWidgetView("toggleWimax", WidgetHelper
//        				.getCurrStatus("toggleWimax", context));
        	} else if (intent.getAction().equals(BluetoothAdapter
        			.ACTION_STATE_CHANGED)) {
        		updateWidgetView("toggleBt", WidgetHelper
        				.getCurrStatus("toggleBt", context));
        	} else if (intent.getAction().equals(LocationManager
        			.PROVIDERS_CHANGED_ACTION)) {
        		updateWidgetView("toggleGps", WidgetHelper
        				.getCurrStatus("toggleGps", context));
        	} else if (intent.getAction().equals(WifiManager
        			.WIFI_AP_STATE_CHANGED_ACTION)) {
        		updateWidgetView("toggleWifiAp", WidgetHelper
        				.getCurrStatus("toggleWifiAp", context));
        	} else if (intent.getAction().equals(TelephonyIntents
        			.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
        		updateWidgetView("toggleMobileData", WidgetHelper
        				.getCurrStatus("toggleMobileData", context));
        	} else if (intent.getAction().equals(
        			Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        		updateWidgetView("toggleAirplaneMode", WidgetHelper
        				.getCurrStatus("toggleAirplaneMode", context));
        	} else if (intent.getAction().equals("com.android.sync.SYNC_CONN_STATUS_CHANGED")) {
        		updateWidgetView("toggleSync", WidgetHelper
        				.getCurrStatus("toggleSync", context));
        	} else if (intent.getAction().equals(UsbManager.ACTION_USB_STATE)) {
        		updateWidgetView("toggleUsbTether", WidgetHelper
        				.getCurrStatus("toggleUsbTether", getContext(),
        				        intent.getBooleanExtra(UsbManager.USB_CONNECTED, false)));
        	} else if (intent.getAction().equals(Intent.ACTION_MEDIA_SHARED)) {
        		updateWidgetView("toggleUsbTether", WidgetHelper
        				.getCurrStatus("toggleUsbTether", context));
        	} else if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int plugType = intent.getIntExtra("plugged", 0);
                Resources res = context.getResources();
                mLevel.setText("" + intent.getIntExtra("level", 0));
                mScale.setText("" + intent.getIntExtra("scale", 0));
                mVoltage.setText("" + intent.getIntExtra("voltage", 0) + " "
                        + res.getString(R.string.battery_info_voltage_units));
                mTemperature.setText("" + tenthsToFixedString(intent.getIntExtra("temperature", 0))
                        + res.getString(R.string.battery_info_temperature_units));
                mTechnology.setText("" + intent.getStringExtra("technology"));
                
                int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
                String statusString;
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    statusString = res.getString(R.string.battery_info_status_charging);
                    if (plugType > 0) {
                        statusString = statusString + " " + res.getString(
                                (plugType == BatteryManager.BATTERY_PLUGGED_AC)
                                        ? R.string.battery_info_status_charging_ac
                                        : R.string.battery_info_status_charging_usb);
                    }
                } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    statusString = res.getString(R.string.battery_info_status_discharging);
                } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    statusString = res.getString(R.string.battery_info_status_not_charging);
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    statusString = res.getString(R.string.battery_info_status_full);
                } else {
                    statusString = res.getString(R.string.battery_info_status_unknown);
                }
                mStatus.setText(statusString);

                switch (plugType) {
                    case 0:
                        mPower.setText(res.getString(R.string.battery_info_power_unplugged));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        mPower.setText(res.getString(R.string.battery_info_power_ac));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        mPower.setText(res.getString(R.string.battery_info_power_usb));
                        break;
                    case (BatteryManager.BATTERY_PLUGGED_AC|BatteryManager.BATTERY_PLUGGED_USB):
                        mPower.setText(res.getString(R.string.battery_info_power_ac_usb));
                        break;
                    default:
                        mPower.setText(res.getString(R.string.battery_info_power_unknown));
                        break;
                }
                
                int health = intent.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN);
                String healthString;
                if (health == BatteryManager.BATTERY_HEALTH_GOOD) {
                    healthString = res.getString(R.string.battery_info_health_good);
                } else if (health == BatteryManager.BATTERY_HEALTH_OVERHEAT) {
                    healthString = res.getString(R.string.battery_info_health_overheat);
                } else if (health == BatteryManager.BATTERY_HEALTH_DEAD) {
                    healthString = res.getString(R.string.battery_info_health_dead);
                } else if (health == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE) {
                    healthString = res.getString(R.string.battery_info_health_over_voltage);
                } else if (health == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE) {
                    healthString = res.getString(R.string.battery_info_health_unspecified_failure);
                } else if (health == BatteryManager.BATTERY_HEALTH_COLD) {
                    healthString = res.getString(R.string.battery_info_health_cold);
                } else {
                    healthString = res.getString(R.string.battery_info_health_unknown);
                }

                mHealth.setText(healthString);

                Bundle extras = intent.getExtras();
                int batteryStatus; int batteryLevel;

                batteryStatus = extras.getInt("plugged", 0);
                batteryLevel = extras.getInt("level");

                ToolboxLinearLayout.updateBatteryUI(batteryStatus, batteryLevel);
        	}
        }
    };
    
    private IntentFilter getBroadcastFilter() {
        IntentFilter filter = new IntentFilter();
    	for (int i = 0; i < widgetList.size(); i++) {
    		if (widgetList.get(i).widgetType.equals("toggleWifi")) {
    			filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    		} else if (widgetList.get(i).widgetType.equals("toggleBt")) {
    			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    		} else if (widgetList.get(i).widgetType.equals("toggleGps")) {
    			filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
    		} else if (widgetList.get(i).widgetType.equals("toggleWifiAp")) {
    			filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
    		} else if (widgetList.get(i).widgetType.equals("toggleSync")) {
    			filter.addAction("com.android.sync.SYNC_CONN_STATUS_CHANGED");
    		} else if (widgetList.get(i).widgetType.equals("toggleUsbTether")) {
    	        filter.addAction(UsbManager.ACTION_USB_STATE);
    	        filter.addAction(Intent.ACTION_MEDIA_SHARED);
    	        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
    		} else if (widgetList.get(i).widgetType.equals("toggleAirplaneMode")) {
    			filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    		} else if (widgetList.get(i).widgetType.equals("toggleMobileData")) {
    			filter.addAction(TelephonyIntents
    					.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
//    		} else if (widgetList.get(i).widgetType.equals("toggleWimax")) {
//            	filter.addAction(WimaxManagerConstants
//            			.WIMAX_ENABLED_CHANGED_ACTION);
    		}
    		
    		//Music intents
            filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filter.addAction("com.android.music.playstatechanged");
            filter.addAction("com.amazon.mp3.playstatechanged");
            filter.addAction("com.nullsoft.winamp.playstatechanged");
            filter.addAction("org.abrantix.rockon.rockonnggl.playstatechanged");
            filter.addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
            filter.addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
            //Battery intent
    		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
    	}
        return filter;
    }
    
    private class WidgetSettingsObserver extends ContentObserver {
        public WidgetSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = getContext().getContentResolver();
        	for (int i = 0; i < widgetList.size(); i++) {
        		if (widgetList.get(i).widgetType.equals("toggleAutoRotate")) {
		            resolver.registerContentObserver(
		                    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
		                            false, this);
        		}
        	}
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SELECTED_TOOLBOX_WIDGETS),
                            false, this);
        }

        public void unobserve() {
            ContentResolver resolver = getContext().getContentResolver();

            resolver.unregisterContentObserver(this);
        }

        public void onChangeUri(Uri uri, boolean selfChange) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION))) {
        		updateWidgetView("toggleAutoRotate", WidgetHelper
        				.getCurrStatus("toggleAutoRotate", getContext()));
            } else if (uri.equals(Settings.System.getUriFor(Settings.System
                    .SELECTED_TOOLBOX_WIDGETS))) {
                loadWidgets();
            }
        }
    }

}
