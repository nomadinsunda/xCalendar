package com.intheeast.settings;

import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AlertTimeOverrideView extends RelativeLayout {

	private static final String TAG = "AlertTimeOverrideView";
	private static final boolean INFO = true;
	
	Context mContext;
	
	AlertTimeSettingPane mPaneView;
	
	LockableScrollView	mScrollView;
	LinearLayout mContainer;	
	
	CommonRelativeLayoutItemContainer mBirthDayAlertTimeSettingContainer;	
	CommonRelativeLayoutItemContainer mEventAlertTimeSettingContainer;	
	CommonRelativeLayoutItemContainer mAlldayEventAlertTimeSettingContainer;
	RelativeLayout mSurplusRegionView;
	
	int mMainViewSurplusRegionHeight;
	
	public AlertTimeOverrideView(Context context) {
		super(context);
		mContext = context;
	}
	
	public AlertTimeOverrideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public AlertTimeOverrideView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	TextView mSelectedBirthDayEventAlerttimeSettingText;
	TextView mSelectedEventAlerttimeSettingText;
	TextView mSelectedAllDayEventAlerttimeSettingText;
	
	View mSwitchDimmingLayout;
	public void initView(AlertTimeSettingPane paneView, int frameLayoutHeight) {
		mPaneView = paneView;
		
		mMainViewSurplusRegionHeight = calcMainViewSurplusRegion(mContext.getResources(), frameLayoutHeight);
		
		mScrollView = (LockableScrollView) findViewById(R.id.alerttime_setting_scrollview);
		mContainer = (LinearLayout) mScrollView.findViewById(R.id.alerttime_setting_items_container);
		
		mSwitchDimmingLayout = findViewById(R.id.settingalert_mainview_switch_dimming_layout);    
		
		mBirthDayAlertTimeSettingContainer = (CommonRelativeLayoutItemContainer) mContainer.findViewById(R.id.birthday_alerttime_setting_container);
		mSelectedBirthDayEventAlerttimeSettingText = (TextView) mBirthDayAlertTimeSettingContainer.findViewById(R.id.selected_birthday_alerttime_setting_text);
		mEventAlertTimeSettingContainer = (CommonRelativeLayoutItemContainer) mContainer.findViewById(R.id.event_alerttime_setting_container);
		mSelectedEventAlerttimeSettingText = (TextView) mEventAlertTimeSettingContainer.findViewById(R.id.selected_event_alerttime_setting_text);
		mAlldayEventAlertTimeSettingContainer = (CommonRelativeLayoutItemContainer) mContainer.findViewById(R.id.allday_event_alerttime_setting_container);
		mSelectedAllDayEventAlerttimeSettingText = (TextView) mAlldayEventAlertTimeSettingContainer.findViewById(R.id.selected_allday_event_alerttime_setting_text);
		
		mSurplusRegionView = (RelativeLayout) mContainer.findViewById(R.id.alerttime_setting_mainview_surplus_region);
		View shapeView = mSurplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams shapeViewParams = (RelativeLayout.LayoutParams) shapeView.getLayoutParams();
		shapeViewParams.height = mMainViewSurplusRegionHeight;
		shapeView.setLayoutParams(shapeViewParams);
		
		mBirthDayAlertTimeSettingContainer.setOnTouchListener(mItemTouchListener);
		mEventAlertTimeSettingContainer.setOnTouchListener(mItemTouchListener);
		mAlldayEventAlertTimeSettingContainer.setOnTouchListener(mItemTouchListener);
		mBirthDayAlertTimeSettingContainer.setOnClickListener(mAlertTimeCaseItemContainerClickListener);
		mEventAlertTimeSettingContainer.setOnClickListener(mAlertTimeCaseItemContainerClickListener);
		mAlldayEventAlertTimeSettingContainer.setOnClickListener(mAlertTimeCaseItemContainerClickListener);
		
		configSelectedAlertTimeTexts();
	}
	
	public View getDimmingLayout() {
		return mSwitchDimmingLayout;
	}
	
	public void configSelectedAlertTimeTexts() {
		configSelectedBirthDayEventAlertTimeText();
		configSelectedEventAlertTimeText();
		configSelectedAllDayEventAlertTimeText();		
	}
	
	public void configSelectedBirthDayEventAlertTimeText() {
		mSelectedBirthDayEventAlerttimeSettingText.setText(mPaneView.mDefaultBirthDayEventReminder.getEntry());
	}
	
	public void configSelectedEventAlertTimeText() {
		mSelectedEventAlerttimeSettingText.setText(mPaneView.mDefaultEventReminder.getEntry());
	}

	public void configSelectedAllDayEventAlertTimeText() {
		mSelectedAllDayEventAlerttimeSettingText.setText(mPaneView.mDefaultAllDayEventReminder.getEntry());	
	}
	
	OnTouchListener mItemTouchListener = new OnTouchListener() {

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
	
	OnClickListener mAlertTimeCaseItemContainerClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int pageCase = 0;
			
			switch(v.getId()) {
			case R.id.birthday_alerttime_setting_container:
				//if (INFO) Log.i(TAG, "birthday_alerttime_setting_container");
				pageCase = AlertTimeOverrideSubView.BIRTHDAY_ALERTTIME_SUBPAGE;
				break;
			case R.id.event_alerttime_setting_container:
				//if (INFO) Log.i(TAG, "event_alerttime_setting_container");
				pageCase = AlertTimeOverrideSubView.EVENT_ALERTTIME_SUBPAGE;
				break;
			case R.id.allday_event_alerttime_setting_container:
				//if (INFO) Log.i(TAG, "allday_event_alerttime_setting_container");
				pageCase = AlertTimeOverrideSubView.ALLDAYEVEENT_ALERTTIME_SUBPAGE;
				break;
			}
			
			mPaneView.switchSubView(pageCase);
		}		
	};
	
	
	ValueAnimator mItemTouchDownReleaseVA;
	public void makeItemTouchDownReleaseValueAnimator(int currentPage, long enterAnimationDuration) {
		CommonRelativeLayoutItemContainer targetItemContainer = null;
		switch(currentPage) {
		case AlertTimeOverrideSubView.BIRTHDAY_ALERTTIME_SUBPAGE:			
			targetItemContainer = mBirthDayAlertTimeSettingContainer;			
			break;
		case AlertTimeOverrideSubView.EVENT_ALERTTIME_SUBPAGE:
			targetItemContainer = mEventAlertTimeSettingContainer;			
			break;
		case AlertTimeOverrideSubView.ALLDAYEVEENT_ALERTTIME_SUBPAGE:
			targetItemContainer = mAlldayEventAlertTimeSettingContainer;			
			break;
		}
		
		if (targetItemContainer == null) {
			return; // �̷� ���� �߻��� �� ���� �ѵ�...
		}
		
		final CommonRelativeLayoutItemContainer itemContainer = targetItemContainer;
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
				
				itemContainer.setBackgroundColor(Color.HSVToColor(hsv));
			}
		});
	}
	
	public final int MAINVEW_ITEMS_COUNT = 3;
	public int calcMainViewSurplusRegion(Resources res, int frameLayoutHeight) {
		
		int firstSeperatorHeight = (int)res.getDimension(R.dimen.selectCalendarViewSeperatorHeight);		
		int itemHeight = (int)res.getDimension(R.dimen.selectCalendarViewItemHeight); 
		int entireItemsHeight = itemHeight * MAINVEW_ITEMS_COUNT;
		int mainViewSurplusRegionHeight = frameLayoutHeight - (firstSeperatorHeight + entireItemsHeight);
		
		return mainViewSurplusRegionHeight;
	}
}
