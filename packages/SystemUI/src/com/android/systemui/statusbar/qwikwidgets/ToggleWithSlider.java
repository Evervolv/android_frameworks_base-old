package com.android.systemui.statusbar.qwikwidgets;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.systemui.R;

public abstract class ToggleWithSlider extends QwikWidget implements SeekBar.OnSeekBarChangeListener {

    protected RelativeLayout mToggleFrame;
    protected TextView mToggleLabel;
    protected ImageView mToggleIcon;
    protected ImageView mToggleIndic;
    protected TextView mSliderLabel;
    protected SeekBar mSlider;

    protected int mLabelId;
    protected int mIconId;
    protected int mIndicId;
    protected int mSliderLabelId;

    @Override
    protected void updateWidgetView() {
        Log.d(TAG, "updateWidgetView");
        mToggleFrame = (RelativeLayout) mWidgetView.findViewById(R.id.toggle_frame);
        mToggleFrame.setTag(mType); // This is a hack to make onClick work properly.
        mToggleLabel = (TextView) mWidgetView.findViewById(R.id.toggle_label);
        mToggleLabel.setText(mLabelId);
        mToggleIcon = (ImageView) mWidgetView.findViewById(R.id.toggle_icon);
        mToggleIcon.setImageResource(mIconId);
        mToggleIndic = (ImageView) mWidgetView.findViewById(R.id.toggle_indic);
        mToggleIndic.setBackgroundResource(mIndicId);

        mSlider = (SeekBar) mWidgetView.findViewById(R.id.slider);
        mSliderLabel = (TextView) mWidgetView.findViewById(R.id.slider_label);
        mSliderLabel.setText(mSliderLabelId);
    }

    @Override
    protected void setupWidget(View view) {
        super.setupWidget(view);

        mSlider.setOnSeekBarChangeListener(this);
        mToggleFrame.setOnClickListener(mClickListener);
        mToggleFrame.setOnLongClickListener(mLongClickListener);
    }

    protected void updateProgress(int progress) { /*do nothing*/ }

    protected void updateAfterProgress() { /*do nothing*/ }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateProgress(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        updateAfterProgress();
    }

}
