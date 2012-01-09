package com.android.systemui.widget;

public class WidgetItems {
	public String widgetType;
	public int widgetTitleId;

    public int onResId;
    public int offResId;
    public int tweenResId;
    public int fourthResId;
    
	public int widgetState;


	public WidgetItems() {
		widgetState = StateTracker.STATE_DISABLED;
	}
	
    public WidgetItems(String widget, int name, int onRes, int offRes) {
        this.widgetType = widget;
        this.widgetTitleId = name;
        this.onResId = onRes;
        this.offResId = offRes;
    }
    
	public WidgetItems(String widget, int name, int onRes, int offRes,
	        int tweenRes) {
		this.widgetType = widget;
		this.widgetTitleId = name;
		this.onResId = onRes;
		this.offResId = offRes;
		this.tweenResId = tweenRes;
	}

    public WidgetItems(String widget, int name, int onRes, int offRes,
            int tweenRes, int fourthState) {
        this.widgetType = widget;
        this.widgetTitleId = name;
        this.onResId = onRes;
        this.offResId = offRes;
        this.tweenResId = tweenRes;
    }

	public void setWidgetState(int state) {
		this.widgetState = state;
	}
	
    public String getWidgetType() { return this.widgetType; }

    public int getWidgetTitleId() { return this.widgetTitleId; }

    public int getOnResId() { return this.onResId; }

    public int getOffResId() { return this.offResId; }

    public int getTweenResId() { return this.tweenResId; }

    public int getFourthResId() { return this.tweenResId; }
}
