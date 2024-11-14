package com.intheeast.event;

import com.intheeast.acalendar.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class EditEventAvailabilitySubView extends LinearLayout {

	public static final int EVENT_AVAILABILITY_BUSY = 0;
	public static final int EVENT_AVAILABILITY_FREE = 1;
	
	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;
	//EditEventActionBarFragment mActionBar;
	EditEvent mEditEvent;
		
	RelativeLayout mAvailabilityBusyLayout;
	RelativeLayout mAvailabilityFreeLayout;	

	ImageView mAvailabilityPageItemSelectionIcon;
	RelativeLayout.LayoutParams mAvailabilityPageItemSelectionIconImageViewLayout;
	
	int mAvailabilityValue;
	
	public EditEventAvailabilitySubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventAvailabilitySubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventAvailabilitySubView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	} 
    
	public void initView(Activity activity, EditEventFragment fragment, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
		//mActionBar = fragment.getActionBar();
		//mActivity = fragment.getActivity();
		
		mEditEvent = fragment.mEditEvent;
		
		mAvailabilityBusyLayout =(RelativeLayout)findViewById(R.id.event_availability_busy_layout);
		mAvailabilityBusyLayout.setOnClickListener(mAvailabilityPageItemClickListener);
		
		mAvailabilityFreeLayout =(RelativeLayout)findViewById(R.id.event_availability_free_layout);
		mAvailabilityFreeLayout.setOnClickListener(mAvailabilityPageItemClickListener);	
		
		mAvailabilityPageItemSelectionIcon = new ImageView(mActivity);
		mAvailabilityPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mAvailabilityPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		
		mAvailabilityPageItemSelectionIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mAvailabilityPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mAvailabilityPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		
		mAvailabilityValue = EVENT_AVAILABILITY_BUSY;
		attachAvailabilityPageItemSelectionIcon(mAvailabilityValue);     
	}
	
	public void attachAvailabilityPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EVENT_AVAILABILITY_BUSY:
			mAvailabilityBusyLayout.addView(mAvailabilityPageItemSelectionIcon, mAvailabilityPageItemSelectionIconImageViewLayout);
			break;			
		case EVENT_AVAILABILITY_FREE:
			mAvailabilityFreeLayout.addView(mAvailabilityPageItemSelectionIcon, mAvailabilityPageItemSelectionIconImageViewLayout);
			break;			
		
		}		
	}

	public void detachAvailabilityPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EVENT_AVAILABILITY_BUSY:
			mAvailabilityBusyLayout.removeView(mAvailabilityPageItemSelectionIcon);
			break;			
		case EVENT_AVAILABILITY_FREE:
			mAvailabilityFreeLayout.removeView(mAvailabilityPageItemSelectionIcon);
			break;		
		}		
	}

	View.OnClickListener mAvailabilityPageItemClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v){
		
			int selectedAvailabilityValue = 0;
			int viewID = v.getId();        	
			switch(viewID) {
			case R.id.event_availability_busy_layout:
				selectedAvailabilityValue = EVENT_AVAILABILITY_BUSY;        		
				break;
			case R.id.event_availability_free_layout:
				selectedAvailabilityValue = EVENT_AVAILABILITY_FREE;        		
				break;				
			default:
				return;
			}
		
			if (selectedAvailabilityValue == mAvailabilityValue) {
				return;
			}        	
			
			detachAvailabilityPageItemSelectionIcon(mAvailabilityValue); 
			mAvailabilityValue = selectedAvailabilityValue;			
			
			updateEventAvailabilityItem(mAvailabilityValue);        	
			attachAvailabilityPageItemSelectionIcon(mAvailabilityValue);        	
			//settingViewSwitching(false, EVENT_AVAILABILITY_PAGE);        	
			//mFragment.getActionBar().setMainViewActionBar(mResources.getString(R.string.editevent_availabilitypage_actionbar_title));
			//mCurrentViewingPage = EVENT_MAIN_PAGE;
		}
	};

	public void updateEventAvailabilityItem(int itemValue) {		
		switch(itemValue) {
		case EVENT_AVAILABILITY_BUSY:
			mEditEvent.mEventAvailabilityText.setText(R.string.event_availability_busy);
			break;			
		case EVENT_AVAILABILITY_FREE:
			mEditEvent.mEventAvailabilityText.setText(R.string.event_availability_free);
			break;			
		}		
	}
}
