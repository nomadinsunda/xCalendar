package com.intheeast.settings;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.R;
import com.intheeast.timezone.TimeZoneData;
import com.intheeast.timezone.TimeZonePickerUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

public class TimeZoneSettingPane extends LinearLayout implements TimeZoneOverrideView.OnHomeTimeZoneEnableSetListener, 
	TimeZoneOverrideSubView.OnHomeTimeZoneSetListener {
	
	private static final String TAG = "TimeZoneSettingPane";
	private static final boolean INFO = true;
	
	LayoutInflater mInflater;
	ViewSwitcher mViewSwitcher;
	
	TimeZoneOverrideView mMainView = null;
	TimeZoneOverrideSubView mSubView = null;
		
	String mCurrentSelectedTimeZoneId;
	TimeZonePickerUtils mTzPickerUtils;
	
	IMMStatusHandler mIMMStatusHandler;
	//int mViewState;	
	
	private OnHomeTimeZoneSetListener mHomeTimeZoneSetListener;
	
	public interface OnHomeTimeZoneSetListener {
        void onHomeTimeZoneSet(String tzID);
    }
	
	public void setOnHomeTimeZoneSetListener(OnHomeTimeZoneSetListener l) {
		mHomeTimeZoneSetListener = l;
    }
		
	
	private OnHomeTimeZoneEnableSetListener mHomeTimeZoneEnableSetListener;
	
	public interface OnHomeTimeZoneEnableSetListener {
        void onHomeTimeZoneEnableSet(boolean enabled, String homeTZId);
    }
	
	public void setOnHomeTimeZoneEnableSetListener(OnHomeTimeZoneEnableSetListener l) {
		mHomeTimeZoneEnableSetListener = l;
    }
	
		
	public TimeZoneSettingPane(Context context) {
		super(context);		
	}
	
	public TimeZoneSettingPane(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}
	
	public TimeZoneSettingPane(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);		
	}
	
	SettingsFragment mFragment;
	SettingsActionBarFragment mActionBar;
	View mMainViewSwitchDimmingLayout;
	public void initView(SettingsFragment fragment, SettingsActionBarFragment actionBar, String currentSelectedTimeZoneId, 
			TimeZoneData timeZoneData, TimeZonePickerUtils tzPickerUtils) {		
		mFragment = fragment;
		mActionBar = actionBar;
		mCurrentSelectedTimeZoneId = currentSelectedTimeZoneId;
		mTzPickerUtils = tzPickerUtils;
		
		mIMMStatusHandler = new IMMStatusHandler();
		
		mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mViewSwitcher = (ViewSwitcher) findViewById(R.id.timezone_override_view_switcher);
		
		mMainView = (TimeZoneOverrideView) mInflater.inflate(R.layout.settings_timezone_override_mainview, null);
		mMainView.initView(this, mFragment.getFrameLayoutHeight());
		mMainView.setOnHomeTimeZoneEnableSetListener(this);
		mMainViewSwitchDimmingLayout = mMainView.getDimmingLayout();
		
		
		mSubView = (TimeZoneOverrideSubView) mInflater.inflate(R.layout.settings_timezone_override_subview, null);
		mSubView.initView(currentSelectedTimeZoneId, timeZoneData, tzPickerUtils, this, mIMMStatusHandler);
		
		//�߸��� ������
        //mViewState = SettingsActionBarFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
        mViewSwitcher.addView(mMainView);
	}
	
	TranslateAnimation mMainViewEnterAnimation;
	TranslateAnimation mMainViewExitAnimation;
	AnimationListener mMainViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			int bgColor = Color.argb(30, 0, 0, 0);
			mMainViewSwitchDimmingLayout.setBackgroundColor(bgColor);
			mMainViewSwitchDimmingLayout.startAnimation(mMainViewEnterDimmingAnimation);
			mMainView.mItemTouchDownReleaseVA.start();
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			int bgColor = Color.argb(0, 0, 0, 0);
			mMainViewSwitchDimmingLayout.setBackgroundColor(bgColor);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};
	AnimationListener mMainViewExitAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			int bgColor = Color.argb(15, 0, 0, 0);
			mMainViewSwitchDimmingLayout.setBackgroundColor(bgColor);
			mMainViewSwitchDimmingLayout.startAnimation(mMainViewExitDimmingAnimation);		
		}

		@Override
		public void onAnimationEnd(Animation animation) {			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};
	
	TranslateAnimation mSubViewEnterAnimation;
	TranslateAnimation mSubViewExitAnimation;
	AnimationListener mSubViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			// actionbar animation�� start �ؾ� �Ѵ�
			mActionBar.startActionBarAnim();			
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			mSubView.makeEnableState();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};
	AnimationListener mSubViewExitAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			//mSubView.makeDisableState();
			// actionbar animation�� start �ؾ� �Ѵ�
			mActionBar.startActionBarAnim();			
		}

		@Override
		public void onAnimationEnd(Animation animation) {			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};
	
	public void switchMainView() {
		if (mSubView.makeDisableState() == true) {			
			return;
		}
		
		excuteSwitchMainView();		
		
	}
	
	AlphaAnimation mMainViewEnterDimmingAnimation;
	public void excuteSwitchMainView() {
		float enterAnimationDistance = getWidth() * 0.4f;
    	float exitAnimationDistance = getWidth(); // 
    	
    	float velocity = SettingsFragment.SWITCH_SUB_PAGE_EXIT_VELOCITY;
    	long exitAnimationDuration = ITEAnimationUtils.calculateDuration(exitAnimationDistance, getWidth(), SettingsFragment.MINIMUM_SNAP_VELOCITY, velocity); 	    	
    	long enterAnimationDuration = exitAnimationDuration;
    	
    	mMainView.makeItemTouchDownReleaseValueAnimator(enterAnimationDuration);
    	
    	mSubViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, 1, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	ITEAnimInterpolator exitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mSubViewExitAnimation); 
    	mSubViewExitAnimation.setInterpolator(exitInterpolator);   	
    	mSubViewExitAnimation.setDuration(exitAnimationDuration);
    	mSubViewExitAnimation.setAnimationListener(mSubViewExitAnimationListener);
    	
    	mMainViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);   	
    	ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mMainViewEnterAnimation);  
    	mMainViewEnterAnimation.setInterpolator(enterInterpolator);
    	mMainViewEnterAnimation.setDuration(enterAnimationDuration);
    	mMainViewEnterAnimation.setAnimationListener(mMainViewEnterAnimationListener);    			
    	
    	mMainViewEnterDimmingAnimation = new AlphaAnimation(1.0f, 0.0f); 
    	mMainViewEnterDimmingAnimation.setDuration(enterAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mMainViewEnterDimmingAnimation);   	
    	mMainViewEnterDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);    	
    	
    	mViewSwitcher.setInAnimation(mMainViewEnterAnimation);
    	mViewSwitcher.setOutAnimation(mSubViewExitAnimation);       	
		
		int fromView = mFragment.mViewState;
		 mFragment.mViewState = SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
		int toView =  mFragment.mViewState;		
		
		long animDuration;
		float distance;		
		if (enterAnimationDuration > exitAnimationDuration) {
			animDuration = enterAnimationDuration;
			distance = enterAnimationDistance;
		}
		else {
			animDuration = exitAnimationDuration;
			distance = exitAnimationDistance;
		}
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance, 0);	
				
		mViewSwitcher.showPrevious();
		mViewSwitcher.removeView(mSubView);
	}
	
	AlphaAnimation mMainViewExitDimmingAnimation;
	public void switchSubView() {
		
		float enterAnimationDistance = getWidth(); 
		float exitAnimationDistance = getWidth() * 0.4f; 
		
		float velocity = SettingsFragment.SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, getWidth(), SettingsFragment.MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
		
    	mSubViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mSubViewEnterAnimation);  
    	mSubViewEnterAnimation.setInterpolator(enterInterpolator);
    	mSubViewEnterAnimation.setDuration(enterAnimationDuration);
    	mSubViewEnterAnimation.setAnimationListener(mSubViewEnterAnimationListener);    	
    	
    	mMainViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);     	
    	ITEAnimInterpolator exitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mMainViewExitAnimation);      	
    	mMainViewExitAnimation.setInterpolator(exitInterpolator);   	    	
    	mMainViewExitAnimation.setDuration(exitAnimationDuration);
    	mMainViewExitAnimation.setAnimationListener(mMainViewExitAnimationListener);
    	
    	mMainViewExitDimmingAnimation = new AlphaAnimation(0.0f, 1.0f); 
    	mMainViewExitDimmingAnimation.setDuration(exitAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mMainViewExitDimmingAnimation);   	
    	mMainViewExitDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);    	
    	
    	mViewSwitcher.setInAnimation(mSubViewEnterAnimation);
    	mViewSwitcher.setOutAnimation(mMainViewExitAnimation); 
    	
    	mSubView.refresh(mCurrentSelectedTimeZoneId);
    	mViewSwitcher.addView(mSubView);
    				
		int fromView = mFragment.mViewState;
		mFragment.mViewState = SettingsFragment.TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE;
		int toView = mFragment.mViewState;			
				
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance, 0);	
		
		mViewSwitcher.showNext();		
	}
	
	
	@SuppressLint("HandlerLeak")
	public class IMMStatusHandler extends Handler {
		@Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            
            if (INFO) Log.i(TAG, "IMMStatusHandler:handleMessage");
            
            switch (msg.what) {  
            case InputMethodManager.RESULT_HIDDEN:
            	if (INFO) Log.i(TAG, "IMMStatusHandler:handleMessage:RESULT_HIDDEN");
            	excuteSwitchMainView();		
            	
            	break;
            }
		}
	}
	
	
	@Override
	public void onHomeTimeZoneEnableSet(boolean enabled, String homeTZId) {
		
		if (enabled) {
			//mCurrentSelectedTimeZoneId�� �����ؾ� �Ѵ�
			//:������ subview�� timezone search edit text view�� �ؽ�Ʈ�� �����Ǳ� �����̴�
			mCurrentSelectedTimeZoneId = homeTZId;
		}
		else {
			
		}	
		
		mHomeTimeZoneEnableSetListener.onHomeTimeZoneEnableSet(enabled, homeTZId);
		
	}
	
	@Override
	public void onHomeTimeZoneSet(String tzID) {
		
		mCurrentSelectedTimeZoneId = tzID;
		mMainView.refresh();
		
		if (mHomeTimeZoneSetListener != null) {
			mHomeTimeZoneSetListener.onHomeTimeZoneSet(tzID);
        }
			
		if (tzID != null)
			switchMainView();
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (INFO) Log.i(TAG, "onKeyDown");
		
		if(KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			if (INFO) Log.i(TAG, "onKeyDown:KEYCODE_BACK");
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (INFO) Log.i(TAG, "onKeyUp");
		
		if(KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			if (INFO) Log.i(TAG, "onKeyUp:KEYCODE_BACK");
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	

}
