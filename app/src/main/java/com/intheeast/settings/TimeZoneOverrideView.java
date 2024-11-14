package com.intheeast.settings;

import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.ITESwitch;
import com.intheeast.etc.LockableScrollView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TimeZoneOverrideView extends RelativeLayout {

	Context mContext;
	LockableScrollView	mScrollView;
	LinearLayout mContainer;
	CommonRelativeLayoutItemContainer mOnOffContainer;
	ITESwitch mSwitchButton;
	
	CommonRelativeLayoutItemContainer mTimeZoneSelectContainer;
	
	TimeZoneSettingPane mPaneView;
	
	OnHomeTimeZoneEnableSetListener mOnHomeTimeZoneEnableSetListener;
	public interface OnHomeTimeZoneEnableSetListener {
        void onHomeTimeZoneEnableSet(boolean enabled, String homeTZId);
    }
	
	public void setOnHomeTimeZoneEnableSetListener(OnHomeTimeZoneEnableSetListener l) {
		mOnHomeTimeZoneEnableSetListener = l;
    }
	
	
	
	
	OnClickListener mUseHomTZSwitchButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			boolean useHomTZSwitchButtonState = Utils.getSharedPreference(mContext, SettingsFragment.KEY_HOME_TZ_ENABLED, false);
			
			if (useHomTZSwitchButtonState) {
				mOnHomeTimeZoneEnableSetListener.onHomeTimeZoneEnableSet(false, null);
				
				mCurrentSelectedTimeZoneNameTextView.setText("Off");
				
				mTimeZoneSelectContainer.setOnClickListener(null);
				mTimeZoneSelectContainer.setClickable(false);				
			}
			else {				
				String homeTZId = Utils.getSharedPreference(mContext, SettingsFragment.KEY_HOME_TZ, ETime.getCurrentTimezone());
				
				mOnHomeTimeZoneEnableSetListener.onHomeTimeZoneEnableSet(true, homeTZId);	
				
				CharSequence timezoneName = mPaneView.mTzPickerUtils.getGmtDisplayName(mContext, homeTZId,
		                System.currentTimeMillis(), false);        
		        String homeTZName = timezoneName.toString();      
		        
				mCurrentSelectedTimeZoneNameTextView.setText(homeTZName);
				
				mTimeZoneSelectContainer.setOnClickListener(TimeZoneSelectContainerClickListener);
			}				
		}
		
	};
	
	OnTouchListener mTimeZoneSelectContainerTouchListener = new OnTouchListener() {

		// ������ ���� ��� ��
		// Custom view com/intheeast/ecalendar/CommonRelativeLayoutItemContainer has setOnTouchListener called on it but does not override performClick
		// :onTouch�� false�� �����ϸ� onClick�� ȣ��ȴ�
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:	
				final CommonRelativeLayoutItemContainer itemContainer = (CommonRelativeLayoutItemContainer) v;
				final float[] from = new float[3], to = new float[3];

				Color.colorToHSV(Color.parseColor("#FFFFFFFF"), from);   // from white
				Color.colorToHSV(Color.parseColor("#FFE8E8E8"), to);     // to red

				ValueAnimator anim = ValueAnimator.ofFloat(0, 1);   // animate from 0 to 1
				anim.setDuration(400);                              // for 400 ms

				final float[] hsv  = new float[3];                  // transition color
				anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
					@Override 
					public void onAnimationUpdate(ValueAnimator animation) {
						// Transition along each axis of HSV (hue, saturation, value)
						hsv[0] = from[0] + (to[0] - from[0])*animation.getAnimatedFraction();
						hsv[1] = from[1] + (to[1] - from[1])*animation.getAnimatedFraction();
						hsv[2] = from[2] + (to[2] - from[2])*animation.getAnimatedFraction();
						
						itemContainer.setBackgroundColor(Color.HSVToColor(hsv));
					}
				});

				anim.start(); // start animation
				
				break;				
			default:
				break;
			}		
			
			return false;
		}
		
	};
	
	OnClickListener TimeZoneSelectContainerClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mPaneView.switchSubView();
		}
		
	};
	
	ValueAnimator mItemTouchDownReleaseVA;
	public void makeItemTouchDownReleaseValueAnimator(long enterAnimationDuration) {
		
		final float[] from = new float[3], to = new float[3];

		Color.colorToHSV(Color.parseColor("#FFE8E8E8"), from);    
		Color.colorToHSV(Color.parseColor("#FFFFFFFF"), to);     

		mItemTouchDownReleaseVA = ValueAnimator.ofFloat(0, 1);   // animate from 0 to 1
		mItemTouchDownReleaseVA.setDuration(enterAnimationDuration);                              

		final float[] hsv = new float[3];                  // transition color
		mItemTouchDownReleaseVA.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
			@Override 
			public void onAnimationUpdate(ValueAnimator animation) {
				// Transition along each axis of HSV (hue, saturation, value)
				hsv[0] = from[0] + (to[0] - from[0])*animation.getAnimatedFraction();
				hsv[1] = from[1] + (to[1] - from[1])*animation.getAnimatedFraction();
				hsv[2] = from[2] + (to[2] - from[2])*animation.getAnimatedFraction();
				
				mTimeZoneSelectContainer.setBackgroundColor(Color.HSVToColor(hsv));
			}
		});
	}
	
	public TimeZoneOverrideView(Context context) {
		super(context);
		mContext = context;
	}
	
	public TimeZoneOverrideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public TimeZoneOverrideView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	TextView mCurrentSelectedTimeZoneNameTextView;
	int mMainViewSurplusRegionHeight;
	RelativeLayout mSurplusRegionView;
	View mSwitchDimmingLayout;
	public void initView(TimeZoneSettingPane paneView, int frameLayoutHeight) {
		mPaneView = paneView;
		
		mMainViewSurplusRegionHeight = calcMainViewSurplusRegion(mContext.getResources(), frameLayoutHeight);
				
		mScrollView = (LockableScrollView) findViewById(R.id.timezone_setting_scrollview);
		mContainer = (LinearLayout) mScrollView.findViewById(R.id.timezone_setting_item_container);
		
		mSwitchDimmingLayout = findViewById(R.id.settintimezone_mainview_switch_dimming_layout);
		
		mOnOffContainer = (CommonRelativeLayoutItemContainer) mContainer.findViewById(R.id.timezone_override_onoff_container);
		makeCustomSwitch();		
		mSwitchButton.setOnClickListener(mUseHomTZSwitchButtonClickListener);
		boolean useHomTZSwitchButtonState = Utils.getSharedPreference(mContext, SettingsFragment.KEY_HOME_TZ_ENABLED, false);
		mSwitchButton.setChecked(useHomTZSwitchButtonState);
		mOnOffContainer.addView(mSwitchButton);
		
		mTimeZoneSelectContainer = (CommonRelativeLayoutItemContainer) mContainer.findViewById(R.id.timezone_select_container);
		mCurrentSelectedTimeZoneNameTextView = (TextView) mTimeZoneSelectContainer.findViewById(R.id.selected_timezone_text);
				
		if (useHomTZSwitchButtonState) {
			CharSequence timezoneName = mPaneView.mTzPickerUtils.getGmtDisplayName(mContext, mPaneView.mCurrentSelectedTimeZoneId,
	                System.currentTimeMillis(), false);        
	        String selectedTimeZoneName = timezoneName.toString();        
			mCurrentSelectedTimeZoneNameTextView.setText(selectedTimeZoneName);
			
			mTimeZoneSelectContainer.setOnTouchListener(mTimeZoneSelectContainerTouchListener);
			mTimeZoneSelectContainer.setOnClickListener(TimeZoneSelectContainerClickListener);
		}
		else {
			mCurrentSelectedTimeZoneNameTextView.setText("Off");
			
			mTimeZoneSelectContainer.setOnTouchListener(null);
			mTimeZoneSelectContainer.setOnClickListener(null);
			mTimeZoneSelectContainer.setClickable(false);
		}		
		
		mSurplusRegionView = (RelativeLayout) mContainer.findViewById(R.id.timezone_setting_mainview_surplus_region);
		View shapeView = mSurplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams shapeViewParams = (RelativeLayout.LayoutParams) shapeView.getLayoutParams();
		shapeViewParams.height = mMainViewSurplusRegionHeight;
		shapeView.setLayoutParams(shapeViewParams);
	}
	
	public View getDimmingLayout() {
		return mSwitchDimmingLayout;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
	}
	
	public void refresh() {
		if (mPaneView.mCurrentSelectedTimeZoneId != null) {
			CharSequence timezoneName = mPaneView.mTzPickerUtils.getGmtDisplayName(mContext, mPaneView.mCurrentSelectedTimeZoneId,
	                System.currentTimeMillis(), false);        
	        String selectedTimeZoneName = timezoneName.toString();        
			mCurrentSelectedTimeZoneNameTextView.setText(selectedTimeZoneName);
		}		
	}
	
	
	public void makeCustomSwitch(/*int itemContainerId*/) {
		int itemContainerHeight = (int) getResources().getDimension(R.dimen.selectCalendarViewItemHeight);
		
		String switchOnText = getResources().getString(R.string.selectcalendars_show_declined_events_on);
		String switchOffText = getResources().getString(R.string.selectcalendars_show_declined_events_off);
		int textColorUnChecked = getResources().getColor(R.color.selectCalendarViewSwitchOffTextColor);
		int textColorChecked = getResources().getColor(R.color.selectCalendarViewSwitchOnTextColor);
		
		Drawable switchBGDrawable = getResources().getDrawable(R.drawable.switch_bg_holo_light); 
		
		Drawable switchDrawable = getResources().getDrawable(R.drawable.added_switch_thumb_activated_holo_light);
		int switchDrawableHeight = switchDrawable.getIntrinsicHeight(); 
		
		mSwitchButton = new ITESwitch(getContext(), switchOnText, switchOffText,
				textColorUnChecked, textColorChecked,
				switchBGDrawable, 
				switchDrawable);		
		
		RelativeLayout.LayoutParams switchButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);		
		switchButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		int parentHeightHalf = itemContainerHeight / 2;
		int switchHeightHalf = switchDrawableHeight / 2;
		int topMargin = parentHeightHalf - switchHeightHalf;		
		//int rightMargin = (int) getResources().getDimension(R.dimen.selectCalendarViewSwitchCommonRightMargin);
		int rightMargin = 20; // for test
		switchButtonLayoutParams.setMargins(0, topMargin, rightMargin, 0);				
		mSwitchButton.setLayoutParams(switchButtonLayoutParams);		
		//switchButton.setTag(itemContainerId);		
	}
	
	public final int MAINVEW_ITEMS_COUNT = 2;
	public int calcMainViewSurplusRegion(Resources res, int frameLayoutHeight) {
		
		int firstSeperatorHeight = (int)res.getDimension(R.dimen.selectCalendarViewSeperatorHeight);     
		int secondSeperatorHeight = (int)res.getDimension(R.dimen.selectCalendarViewSeperatorHeight);     
		int itemHeight = (int)res.getDimension(R.dimen.selectCalendarViewItemHeight); 
		int entireItemsHeight = itemHeight * MAINVEW_ITEMS_COUNT;
		int mainViewSurplusRegionHeight = frameLayoutHeight - (firstSeperatorHeight + secondSeperatorHeight + entireItemsHeight);
		
		return mainViewSurplusRegionHeight;
	}
	

}
