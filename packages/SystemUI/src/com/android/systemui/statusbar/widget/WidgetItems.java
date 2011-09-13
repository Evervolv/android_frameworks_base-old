package com.android.systemui.statusbar.widget;

public class WidgetItems {
	public String widgetType;
	
	public WidgetItems() {
		
	}

	public WidgetItems(String widget) {
		this.widgetType = widget;
	}
	
	public String getWidgetType() {
		return this.widgetType;
	}
	
}
