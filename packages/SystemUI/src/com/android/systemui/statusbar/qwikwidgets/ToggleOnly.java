package com.android.systemui.statusbar.qwikwidgets;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public abstract class ToggleOnly extends QwikWidget {

    protected TextView mWidgetLabel;
    protected ImageView mWidgetIcon;
    protected ImageView mWidgetIndic;

    protected int mLabelId;
    protected int mIconId;
    protected int mIndicId;

    @Override
    protected void updateWidgetView() {
        mWidgetLabel = (TextView) mWidgetView.findViewById(R.id.widget_label);
        mWidgetIcon = (ImageView) mWidgetView.findViewById(R.id.widget_icon);
        mWidgetIcon.setImageResource(mIconId);
        mWidgetIndic = (ImageView) mWidgetView.findViewById(R.id.widget_indic);
        mWidgetIndic.setBackgroundResource(mIndicId);
        
        int lineMax =  Settings.System.getInt(mWidgetView.getContext().getContentResolver(),
                Settings.System.MAX_WIDGETS_PER_LINE, 3);
        
        if ((QwikWidgetsHelper.isTablet(mWidgetView.getContext()) && lineMax < 4)  || mIconId == 0) {
            mWidgetLabel.setText(mLabelId);
        } else {
            mWidgetIcon.setVisibility(View.VISIBLE);
            mWidgetLabel.setVisibility(View.GONE);
        }
    }

    @Override
    protected void setupWidget(View view) {
        super.setupWidget(view);
        mWidgetView.setOnClickListener(mClickListener);
        mWidgetView.setOnLongClickListener(mLongClickListener);
    }
}
