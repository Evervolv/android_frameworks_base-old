package com.android.systemui.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class WidgetAdapter extends ArrayAdapter<WidgetItems>  {

	private Context mContext;
	private ImageView widgetIcon;
	private TextView widgetName;
	
	private List<WidgetItems> widgets = new ArrayList<WidgetItems>();
	
	public WidgetAdapter(Context context, int textViewResourceId,
			List<WidgetItems> objects) {
		super(context, textViewResourceId, objects);
		this.mContext = context;
		this.widgets = objects;
	}

	public int getCount() {
		return this.widgets.size();
	}

	public WidgetItems getItem(int index) {
		return this.widgets.get(index);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.widget_button, parent, false);
		}

		WidgetItems widget = getItem(position);
		Resources res = mContext.getResources();
		
		widgetIcon = (ImageView) row.findViewById(R.id.widget_icon);
		widgetName = (TextView) row.findViewById(R.id.widget_name);
		widgetName.setText(res.getString(widget.getWidgetTitleId()));
		
        switch (widget.widgetState) {
              case StateTracker.STATE_ENABLED:
                  widgetIcon.setImageDrawable(res.getDrawable(
                          widget.getOnResId()));
                  break;
              case StateTracker.STATE_DISABLED:
                  widgetIcon.setImageDrawable(res.getDrawable(
                          widget.getOffResId()));
                  break;
              case StateTracker.STATE_TURNING_ON:
                  widgetIcon.setImageDrawable(res.getDrawable(
                          widget.getTweenResId()));
                  break;
              case StateTracker.STATE_TURNING_OFF:
                  widgetIcon.setImageDrawable(res.getDrawable(
                          widget.getTweenResId()));
                  break;
              case StateTracker.STATE_UNKNOWN:
                  if (widget.getFourthResId() == 0) { 
                      widgetIcon.setImageDrawable(res.getDrawable(
                              widget.getOffResId()));
                  } else {
                      widgetIcon.setImageDrawable(res.getDrawable(
                              widget.getFourthResId()));
                  }
                  break;
              case StateTracker.STATE_UNAVAILABLE:
                  if (widget.getFourthResId() == 0) { 
                      widgetIcon.setImageDrawable(res.getDrawable(
                              widget.getOffResId()));
                  } else {
                      widgetIcon.setImageDrawable(res.getDrawable(
                              widget.getFourthResId()));
                  }
                  break;
        }

		return row;
	}
}
