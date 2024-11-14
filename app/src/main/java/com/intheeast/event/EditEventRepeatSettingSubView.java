package com.intheeast.event;


import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class EditEventRepeatSettingSubView extends LinearLayout {
	
	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;	
	//EditEvent mEditEvent;
	EditEventRepeatSettingSwitchPageView mParentView;
	
	RelativeLayout mInnerLinearLayoutOfScrollView;
	
    RelativeLayout mNoRepeatLayout;
    RelativeLayout mEveryDayRepeatLayout;
    RelativeLayout mEveryWeekRepeatLayout;
    
    RelativeLayout mEveryMonthRepeatLayout;
    RelativeLayout mEveryYearRepeatLayout;   
    
    RelativeLayout mUserSettingRepeatLayout;
    
    
    ImageView mRepeatPageItemSelectionIcon;
	RelativeLayout.LayoutParams mRepeatPageItemSelectionIconImageViewLayout;
	
	RelativeLayout mSurplusRegionView;	
    	
	int mViewSurplusRegionHeight;
	
	//RecurrenceContext mRRuleContext = null;
	
	public EditEventRepeatSettingSubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventRepeatSettingSubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventRepeatSettingSubView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	} 
    
	public void initView(Activity activity, EditEventFragment fragment, EditEventRepeatSettingSwitchPageView parentView, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
		mParentView = parentView;
		//mRRuleContext = mFragment.mEditEvent.mRRuleContext;			
		//mEditEvent = fragment.mEditEvent;
		
		LockableScrollView scrollView = (LockableScrollView)findViewById(R.id.editevent_repeatpage_scroll_view);
		mInnerLinearLayoutOfScrollView = (RelativeLayout)scrollView .findViewById(R.id.editevent_repeatpage_scrollview_inner_layout);
		
		mNoRepeatLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_repeat_no_repeat_layout);
		mNoRepeatLayout.setOnClickListener(mRepeatItemClickListener);
		
		mEveryDayRepeatLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_repeat_everyday_repeat_layout);
		mEveryDayRepeatLayout.setOnClickListener(mRepeatItemClickListener);
		
		mEveryWeekRepeatLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_repeat_everyweek_repeat_layout);
		mEveryWeekRepeatLayout.setOnClickListener(mRepeatItemClickListener);		
				
		mEveryMonthRepeatLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_repeat_everymonth_repeat_layout);
		mEveryMonthRepeatLayout.setOnClickListener(mRepeatItemClickListener);
		
		mEveryYearRepeatLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_repeat_everyyear_repeat_layout);
		mEveryYearRepeatLayout.setOnClickListener(mRepeatItemClickListener);
		
		mUserSettingRepeatLayout = (RelativeLayout) mInnerLinearLayoutOfScrollView.findViewById(R.id.event_repeat_user_settings_layout);
		mUserSettingRepeatLayout.setOnClickListener(mRepeatUserSettingsItemClickListener);
		
		mRepeatPageItemSelectionIcon = new ImageView(mActivity);
		mRepeatPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mRepeatPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);	
		
		mRepeatPageItemSelectionIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mRepeatPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		mRepeatPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);	
	}
	
	int mSelectedRepeatItemLayout =- 1;
	View.OnClickListener mRepeatItemClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){    
        	int prvSelectedRepeatItemLayout = mSelectedRepeatItemLayout;
        	
        	int viewID = v.getId();        	
        	switch(viewID) {
        	case R.id.event_repeat_no_repeat_layout:
        		mSelectedRepeatItemLayout = EditEventRepeatSettingSwitchPageView.EVENT_NO_RECURRENCE;           		
        		break;
        	case R.id.event_repeat_everyday_repeat_layout:
        		mSelectedRepeatItemLayout = EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_DAY;          		
        		break;
        	case R.id.event_repeat_everyweek_repeat_layout:
        		mSelectedRepeatItemLayout = EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_WEEK;         		
        		break;
        	
        	case R.id.event_repeat_everymonth_repeat_layout:
        		mSelectedRepeatItemLayout = EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_MONTH;          		
        		break;
        	case R.id.event_repeat_everyyear_repeat_layout:
        		mSelectedRepeatItemLayout = EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_YEAR;           		
        		break;
        	
        	default:
        		
        		return;
        	}       	
        	        	
        	if (mSelectedRepeatItemLayout == prvSelectedRepeatItemLayout) {        		
        		mParentView.switchMainView();
        		return;
        	}  
        	
        	detachRepeatPageItemSelectionIcon(prvSelectedRepeatItemLayout); // ������ repeat done icon�� �����Ѵ�
        	
        	mParentView.adjustMainViewLayout(mSelectedRepeatItemLayout);
        	
        	mParentView.updateEventRepeatItem(mSelectedRepeatItemLayout);        	
        	attachRepeatPageItemSelectionIcon(mSelectedRepeatItemLayout);  // run-time error occur  
        	
        	mParentView.switchMainView();
        }
    };
    
    View.OnClickListener mRepeatUserSettingsItemClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){    
        	int prvSelectedRepeatItemLayout = mSelectedRepeatItemLayout;
        	mSelectedRepeatItemLayout = EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_USER_SETTING;   
        	
        	detachRepeatPageItemSelectionIcon(prvSelectedRepeatItemLayout); // ������ repeat done icon�� �����Ѵ�
        	
        	// �����ؾ� �Ѵ�!!!!!!!!!!!!!!!!!!!!!!!!!!
        	mParentView.updateEventRepeatItem(-1);
        	//////////////////////////////////////
        	
    		attachRepeatPageItemSelectionIcon(mSelectedRepeatItemLayout);
    		mParentView.switchSubView(true);
        }
    };
    
    public void attachRepeatPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EditEventRepeatSettingSwitchPageView.EVENT_NO_RECURRENCE:			
			mNoRepeatLayout.addView(mRepeatPageItemSelectionIcon, mRepeatPageItemSelectionIconImageViewLayout);
			
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_DAY:			
			mEveryDayRepeatLayout.addView(mRepeatPageItemSelectionIcon, mRepeatPageItemSelectionIconImageViewLayout);
			
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_WEEK:			
			mEveryWeekRepeatLayout.addView(mRepeatPageItemSelectionIcon, mRepeatPageItemSelectionIconImageViewLayout);
			
			break;			
		
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_MONTH:			
			mEveryMonthRepeatLayout.addView(mRepeatPageItemSelectionIcon, mRepeatPageItemSelectionIconImageViewLayout);
			
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_YEAR:			
			mEveryYearRepeatLayout.addView(mRepeatPageItemSelectionIcon, mRepeatPageItemSelectionIconImageViewLayout);
			
			break;
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_USER_SETTING:
			mUserSettingRepeatLayout.addView(mRepeatPageItemSelectionIcon, mRepeatPageItemSelectionIconImageViewLayout);
			break;
		default:
			
			break;
		}		
	}
	
	public void detachRepeatPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EditEventRepeatSettingSwitchPageView.EVENT_NO_RECURRENCE:
			mNoRepeatLayout.removeView(mRepeatPageItemSelectionIcon);			
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_DAY:
			mEveryDayRepeatLayout.removeView(mRepeatPageItemSelectionIcon);			
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_WEEK:
			mEveryWeekRepeatLayout.removeView(mRepeatPageItemSelectionIcon);			
			break;			
			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_MONTH:
			mEveryMonthRepeatLayout.removeView(mRepeatPageItemSelectionIcon);			
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_YEAR:
			mEveryYearRepeatLayout.removeView(mRepeatPageItemSelectionIcon);			
			break;
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_USER_SETTING:
			mUserSettingRepeatLayout.removeView(mRepeatPageItemSelectionIcon);		
			break;
		default:
			
			break;
		}		
	}
	
	
}
