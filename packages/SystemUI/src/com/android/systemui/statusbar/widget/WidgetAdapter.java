package com.android.systemui.statusbar.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;

public class WidgetAdapter extends ArrayAdapter<WidgetItems>  {

	private Context context;
	private ImageView widgetIcon;
	private TextView widgetName;
	private List<WidgetItems> widgets = new ArrayList<WidgetItems>();

	
	public WidgetAdapter(Context context, int textViewResourceId,
			List<WidgetItems> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.widgets = objects;
		Log.d("WidgetAdapter", "NEW WIDGETADAPTER");
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

		widgetIcon = (ImageView) row.findViewById(R.id.widget_icon);
		widgetName = (TextView) row.findViewById(R.id.widget_name);
		
		Log.d("WidgetAdapter", "widgetType: " + widget.widgetType);
		if (widget.widgetType.equals("toggleWimax")) {
			widgetIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.widget_wimax));
			widgetName.setText("Wimax");
		} else if (widget.widgetType.equals("toggleWifi")) {
			widgetIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.widget_wifi));
			widgetName.setText("Wi-Fi");
		} else if (widget.widgetType.equals("toggleGps")) {
			widgetIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.widget_gps));
			widgetName.setText("Gps");
		} else if (widget.widgetType.equals("toggleSync")) {
			widgetIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.widget_sync));
			widgetName.setText("Sync");
		}
		
		return row;
	}
    
}
