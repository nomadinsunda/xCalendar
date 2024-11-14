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

public class EditEventVisibilitySettingSubView extends LinearLayout {

	public static final int EVENT_VISIBILITY_DEFAULT = 0;
	public static final int EVENT_VISIBILITY_PRIVATE = 1;
	public static final int EVENT_VISIBILITY_PUBLIC = 2;	
	
	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;
	//EditEventActionBarFragment mActionBar;
	EditEvent mEditEvent;
	
	RelativeLayout mVisibilityDefaultLayout;
	RelativeLayout mVisibilityPrivateLayout;
	RelativeLayout mVisibilityPublicLayout;
	
	ImageView mVisibilityPageItemSelectionIcon;
	RelativeLayout.LayoutParams mVisibilityPageItemSelectionIconImageViewLayout;
	
	public EditEventVisibilitySettingSubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventVisibilitySettingSubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventVisibilitySettingSubView (Context context, AttributeSet attrs, int defStyle) {
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
		
		mVisibilityDefaultLayout =(RelativeLayout)findViewById(R.id.event_visibility_default_layout);
		mVisibilityDefaultLayout.setOnClickListener(mVisibilityPageItemClickListener);
		
		mVisibilityPrivateLayout =(RelativeLayout)findViewById(R.id.event_visibility_private_layout);
		mVisibilityPrivateLayout.setOnClickListener(mVisibilityPageItemClickListener);	
		
		mVisibilityPublicLayout =(RelativeLayout)findViewById(R.id.event_visibility_public_layout);
		mVisibilityPublicLayout.setOnClickListener(mVisibilityPageItemClickListener);	
		
		mVisibilityPageItemSelectionIcon = new ImageView(mActivity);
		mVisibilityPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mVisibilityPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		
		mVisibilityPageItemSelectionIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mVisibilityPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mVisibilityPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		
		mEditEvent.mVisibilityValue = EVENT_VISIBILITY_DEFAULT;
		attachVisibilityPageItemSelectionIcon(EVENT_VISIBILITY_DEFAULT);  
	}
		
	
	public void attachVisibilityPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EVENT_VISIBILITY_DEFAULT:
			mVisibilityDefaultLayout.addView(mVisibilityPageItemSelectionIcon, mVisibilityPageItemSelectionIconImageViewLayout);
			break;			
		case EVENT_VISIBILITY_PRIVATE:
			mVisibilityPrivateLayout.addView(mVisibilityPageItemSelectionIcon, mVisibilityPageItemSelectionIconImageViewLayout);
			break;		
		case EVENT_VISIBILITY_PUBLIC:
			mVisibilityPublicLayout.addView(mVisibilityPageItemSelectionIcon, mVisibilityPageItemSelectionIconImageViewLayout);
			break;	
		}		
	}
	
	public void detachVisibilityPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EVENT_VISIBILITY_DEFAULT:
			mVisibilityDefaultLayout.removeView(mVisibilityPageItemSelectionIcon);
			break;			
		case EVENT_VISIBILITY_PRIVATE:
			mVisibilityPrivateLayout.removeView(mVisibilityPageItemSelectionIcon);
			break;		
		case EVENT_VISIBILITY_PUBLIC:
			mVisibilityPublicLayout.removeView(mVisibilityPageItemSelectionIcon);
			break;			
		}		
	}
	
	View.OnClickListener mVisibilityPageItemClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v){
		
			int selectedVisibilityValue = 0;
			int viewID = v.getId();        	
			switch(viewID) {
			case R.id.event_visibility_default_layout:
				selectedVisibilityValue = EVENT_VISIBILITY_DEFAULT;        		
				break;
			case R.id.event_visibility_private_layout:
				selectedVisibilityValue = EVENT_VISIBILITY_PRIVATE;        		
				break;
			case R.id.event_visibility_public_layout:
				selectedVisibilityValue = EVENT_VISIBILITY_PUBLIC;        		
				break;
			default:
				return;
			}
			
			if (selectedVisibilityValue == mEditEvent.mVisibilityValue) {
				return;
			}        	
			
			detachVisibilityPageItemSelectionIcon(mEditEvent.mVisibilityValue); 
			mEditEvent.mVisibilityValue = selectedVisibilityValue;			
			
			updateEventVisibilityItem(mEditEvent.mVisibilityValue);        	
			attachVisibilityPageItemSelectionIcon(mEditEvent.mVisibilityValue);        	
			//settingViewSwitching(false, EVENT_VISIBILITY_PAGE);        	
			//mFragment.getActionBar().setMainViewActionBar(mResources.getString(R.string.editevent_visibilitypage_actionbar_title));
			//mCurrentViewingPage = EVENT_MAIN_PAGE;
		}
	};
	
	public void updateEventVisibilityItem(int itemValue) {		
		switch(itemValue) {
		case EVENT_VISIBILITY_DEFAULT:
			mEditEvent.mEventVisibilityText.setText(R.string.event_visibility_default);
			break;			
		case EVENT_VISIBILITY_PRIVATE:
			mEditEvent.mEventVisibilityText.setText(R.string.event_visibility_private);
			break;		
		case EVENT_VISIBILITY_PUBLIC:
			mEditEvent.mEventVisibilityText.setText(R.string.event_visibility_public);
			break;		
		}		
	}    
}
