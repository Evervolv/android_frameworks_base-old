package com.android.systemui.statusbar.widget;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.android.systemui.R;

public class DockPanel extends LinearLayout {

	// =========================================
	// Private members
	// =========================================

	private static final String TAG = "DockPanel";
	private DockPosition position;
	private int contentLayoutId;
	private int handleButtonDrawableId;
	private Boolean isOpen;
	private Boolean animationRunning;
	private FrameLayout contentPlaceHolder;
	private ImageButton toggleButton;
	private int animationDuration;
	
	private ViewFlipper mToolboxFlipper;
	private TextView mIndicText;
	private Button mNextButton;
	private Button mPrevButton;
	
	private Context mContext;
	private GridView mWidgetGrid;
    private List<WidgetItems> widgetList = new ArrayList<WidgetItems>();
	// =========================================
	// Constructors
	// =========================================

	public DockPanel(Context context, int contentLayoutId,
			int handleButtonDrawableId, Boolean isOpen) {
		super(context);
		
		this.contentLayoutId = contentLayoutId;
		this.handleButtonDrawableId = handleButtonDrawableId;
		this.isOpen = isOpen;
		mContext = context;
		
		Init(null);
	}

	public DockPanel(Context context, AttributeSet attrs) {
		super(context, attrs);

		// to prevent from crashing the designer
		try {
			Init(attrs);
			mContext = context;
		} catch (Exception ex) {
		}
	}

	// =========================================
	// Initialization
	// =========================================

	private void Init(AttributeSet attrs) {
		setDefaultValues(attrs);

		createHandleToggleButton();

		// create the handle container
		FrameLayout handleContainer = new FrameLayout(getContext());
		handleContainer.addView(toggleButton);

		// create and populate the panel's container, and inflate it
		contentPlaceHolder = new FrameLayout(getContext());
		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(
				infService);
		li.inflate(contentLayoutId, contentPlaceHolder, true);

		// setting the layout of the panel parameters according to the docking
		// position
		if (position == DockPosition.LEFT || position == DockPosition.RIGHT) {
			handleContainer.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.FILL_PARENT, 1));
			contentPlaceHolder.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.FILL_PARENT, 1));
		} else {
			handleContainer.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
			contentPlaceHolder.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		}

		// adding the view to the parent layout according to docking position
		if (position == DockPosition.RIGHT || position == DockPosition.BOTTOM) {
			this.addView(handleContainer);
			this.addView(contentPlaceHolder);
		} else {
			this.addView(contentPlaceHolder);
			this.addView(handleContainer);
		}

		if (!isOpen) {
			contentPlaceHolder.setVisibility(GONE);
		}

		initButtons();
		mWidgetGrid = (GridView) findViewById(R.id.widget_grid);
		mToolboxFlipper = (ViewFlipper) findViewById(R.id.toolbox_flipper);
		mIndicText = (TextView) findViewById(R.id.view_indic);
		setViewIndic(mToolboxFlipper.getCurrentView());
		loadWidgets();
	}

    private void loadWidgets() {
		for (String widget: getResources().getStringArray(R.array.widget_types)) {
			WidgetItems widgets = new WidgetItems(widget);
			widgetList.add(widgets);
		}
		
		WidgetAdapter adapter = new WidgetAdapter(
				getContext(), R.layout.widget_button, widgetList);
		
		mWidgetGrid.setAdapter(adapter);
    }
	
	private void initButtons() {
		mNextButton = (Button) findViewById(R.id.next_button);
		mNextButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	nextView();
		    }
		});
		
		mPrevButton = (Button) findViewById(R.id.prev_button);
		mPrevButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	previousView();
		    }
		});

	}
	
	private void setDefaultValues(AttributeSet attrs) {
		// set default values
		isOpen = true;
		animationRunning = false;
		animationDuration = 500;
		setPosition(DockPosition.RIGHT);

		// Try to load values set by xml markup
		if (attrs != null) {
			String namespace = "http://com.android.systemui.statusbar.widget";

			animationDuration = attrs.getAttributeIntValue(namespace,
					"animationDuration", 500);
			contentLayoutId = attrs.getAttributeResourceValue(namespace,
					"contentLayoutId", 0);
			handleButtonDrawableId = attrs.getAttributeResourceValue(
					namespace, "handleButtonDrawableResourceId", 0);
			isOpen = attrs.getAttributeBooleanValue(namespace, "isOpen", true);

			// Enums are a bit trickier (needs to be parsed)
			try {
				position = DockPosition.valueOf(attrs.getAttributeValue(
						namespace, "dockPosition").toUpperCase());
				setPosition(position);
			} catch (Exception ex) {
				// Docking to the left is the default behavior
				setPosition(DockPosition.LEFT);
			}
		}
	}

	private void createHandleToggleButton() {
		toggleButton = new ImageButton(getContext());
		toggleButton.setPadding(0, 0, 0, 0);
		toggleButton.setLayoutParams(new FrameLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.CENTER));
		toggleButton.setBackgroundColor(Color.TRANSPARENT);
		toggleButton.setImageResource(handleButtonDrawableId);
		toggleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggle();
			}
		});
	}

	private void setPosition(DockPosition position) {
		this.position = position;
		switch (position) {
		case TOP:
			setOrientation(LinearLayout.VERTICAL);
			setGravity(Gravity.TOP);
			break;
		case RIGHT:
			setOrientation(LinearLayout.HORIZONTAL);
			setGravity(Gravity.RIGHT);
			break;
		case BOTTOM:
			setOrientation(LinearLayout.VERTICAL);
			setGravity(Gravity.BOTTOM);
			break;
		case LEFT:
			setOrientation(LinearLayout.HORIZONTAL);
			setGravity(Gravity.LEFT);
			break;
		}
	}

	// =========================================
	// Public methods
	// =========================================

	public int getAnimationDuration() {
		return animationDuration;
	}

	public void setAnimationDuration(int milliseconds) {
		animationDuration = milliseconds;
	}

	public Boolean getIsRunning() {
		return animationRunning;
	}

	public void open() {
		if (!animationRunning) {
			Log.d(TAG, "Opening...");

			Animation animation = createShowAnimation();
			this.setAnimation(animation);
			animation.start();

			isOpen = true;
		}
	}

	public void close() {
		if (!animationRunning) {
			Log.d(TAG, "Closing...");

			Animation animation = createHideAnimation();
			this.setAnimation(animation);
			animation.start();
			isOpen = false;
		}
	}

	public void toggle() {
		if (isOpen) {
			close();
		} else {
			open();
		}
	}

	// =========================================
	// Private methods
	// =========================================

	private Animation createHideAnimation() {
		Animation animation = null;
		switch (position) {
		case TOP:
			animation = new TranslateAnimation(0, 0, 0, -contentPlaceHolder
					.getHeight());
			break;
		case RIGHT:
			animation = new TranslateAnimation(0, contentPlaceHolder
					.getWidth(), 0, 0);
			break;
		case BOTTOM:
			animation = new TranslateAnimation(0, 0, 0, contentPlaceHolder
					.getHeight());
			break;
		case LEFT:
			animation = new TranslateAnimation(0, -contentPlaceHolder
					.getWidth(), 0, 0);
			break;
		}

		animation.setDuration(animationDuration);
		animation.setInterpolator(new AccelerateInterpolator());
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				animationRunning = true;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				contentPlaceHolder.setVisibility(View.GONE);
				animationRunning = false;
			}
		});
		return animation;
	}

	private Animation createShowAnimation() {
		Animation animation = null;
		switch (position) {
		case TOP:
			animation = new TranslateAnimation(0, 0, -contentPlaceHolder
					.getHeight(), 0);
			break;
		case RIGHT:
			animation = new TranslateAnimation(contentPlaceHolder.getWidth(),
					0, 0, 0);
			break;
		case BOTTOM:
			animation = new TranslateAnimation(0, 0, contentPlaceHolder
					.getHeight(), 0);
			break;
		case LEFT:
			animation = new TranslateAnimation(-contentPlaceHolder.getWidth(),
					0, 0, 0);
			break;
		}
		Log.d(TAG, "Animation duration: " + animationDuration);
		animation.setDuration(animationDuration);
		animation.setInterpolator(new DecelerateInterpolator());
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				animationRunning = true;
				contentPlaceHolder.setVisibility(View.VISIBLE);
				Log.d(TAG, "\"Show\" Animation started");
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				animationRunning = false;
				Log.d(TAG, "\"Show\" Animation ended");
			}
		});
		return animation;
	}

	private void previousView() {
		mToolboxFlipper.setInAnimation(this.getContext(), R.anim.in_animation1);
		mToolboxFlipper.setOutAnimation(this.getContext(), R.anim.out_animation1);
		mToolboxFlipper.showPrevious();
		setViewIndic(mToolboxFlipper.getCurrentView());
	}
	private void nextView() {
		
		mToolboxFlipper.setInAnimation(this.getContext(), R.anim.in_animation);
		mToolboxFlipper.setOutAnimation(this.getContext(), R.anim.out_animation);
		mToolboxFlipper.showNext();
		setViewIndic(mToolboxFlipper.getCurrentView());
	}

	private void setViewIndic(View currentView) {
		if (currentView == findViewById(R.id.flipper_ll_widgets)) {
			mIndicText.setText("Power Widgets");
		} else if (currentView == findViewById(R.id.flipper_ll_media)) {
			mIndicText.setText("Sound & Media");
		}
	}

}