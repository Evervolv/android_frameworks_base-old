package com.android.systemui.statusbar.widget;

import com.android.systemui.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;


public class WidgetButton extends ImageButton implements OnClickListener {
	
	private static final String TAG = "WidgetButton";
	
	private static final int wifiWidget = 1;
	private static final int wimaxWidget = 2;
	private static final int gpsWidget = 3;
	private static final int btWidget = 4;
	private static final int syncWidget = 5;
	
	
	private int widgetType;
	
	
	
	public WidgetButton(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public WidgetButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
		loadAttrs(attrs);
	}

	
	private void loadAttrs(AttributeSet attrs) {
		if (attrs != null) {
			
			String namespace = "http://com.android.systemui.statusbar.widgetbutton";
			
			widgetType = getWidgetInt(attrs.getAttributeValue(
					namespace, "widgetType"));
			
		} else {
			
			Log.d(TAG, "Attrs: Null");
			
		}
		loadWidget(widgetType);
	}
	
	private void loadWidget(int widgetType) {
		switch (widgetType) {
			case wifiWidget:
				setImageResource(R.drawable.widget_wifi);
			case wimaxWidget:
				setImageResource(R.drawable.widget_wimax);
			case gpsWidget:
				setImageResource(R.drawable.widget_gps);
			case btWidget:
				setImageResource(R.drawable.widget_bluetooth);
			case syncWidget:
				setImageResource(R.drawable.widget_sync);
		}
		
	}
	
	private int getWidgetInt(String widget) {
		
		if (widget == "toggleWifi") {
			return wifiWidget;
		} else if (widget == "toggleWimax") {
			return wimaxWidget;
		} else if (widget == "toggleGps") {
			return gpsWidget;
		} else if (widget == "toggleBt") {
			return btWidget;
		} else if (widget == "toggleSync") {
			return syncWidget;
		}
		
		return 0;
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	}


}
