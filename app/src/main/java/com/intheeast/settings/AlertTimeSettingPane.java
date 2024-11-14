package com.intheeast.settings;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.CustomListPreference;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

public class AlertTimeSettingPane extends LinearLayout implements
	AlertTimeOverrideSubView.OnDefaultBirthDayEventAlertTimeSetListener, AlertTimeOverrideSubView.OnDefaultEventAlertTimeSetListener, 
	AlertTimeOverrideSubView.OnDefaultAllDayEventAlertTimeSetListener {

	private static final String TAG = "AlertTimeSettingPane";
	private static final boolean INFO = true;
	
	Context mContext;
	LayoutInflater mInflater;
	
	SettingsFragment mFragment;
	SettingsActionBarFragment mActionBar;
	
	ViewSwitcher mViewSwitcher;
	
	AlertTimeOverrideView mMainView;
	AlertTimeOverrideSubView mSubView;
	
	TranslateAnimation mMainViewEnterAnimation;
	TranslateAnimation mMainViewExitAnimation;	
	
	TranslateAnimation mSubViewEnterAnimation;
	TranslateAnimation mSubViewExitAnimation;
	
	CustomListPreference mDefaultBirthDayEventReminder;
	CustomListPreference mDefaultEventReminder;
	CustomListPreference mDefaultAllDayEventReminder;
		
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
	
	
	AnimationListener mSubViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			// actionbar animation�� start �ؾ� �Ѵ�
			mActionBar.startActionBarAnim();			
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			//mSubView.makeEnableState();
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
	
	public AlertTimeSettingPane (Context context) {
		super(context);	
		mContext = context;
	}
	
	public AlertTimeSettingPane (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
	}
	
	public AlertTimeSettingPane (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	View mMainViewSwitchDimmingLayout;
	public void initView(SettingsFragment fragment, SettingsActionBarFragment actionBar) {
		mFragment = fragment;
		mActionBar = actionBar;		
		
		makeCustomListPreference();
		
		mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mViewSwitcher = (ViewSwitcher) findViewById(R.id.alerttime_override_view_switcher);
		
		mMainView = (AlertTimeOverrideView) mInflater.inflate(R.layout.settings_alerttime_override_mainview, null);
		mMainView.initView(this, mFragment.getFrameLayoutHeight());
		mMainViewSwitchDimmingLayout = mMainView.getDimmingLayout();
		
		mSubView = (AlertTimeOverrideSubView) mInflater.inflate(R.layout.settings_alerttime_override_subview, null);
		mSubView.init(this, mMainView, mFragment.getFrameLayoutHeight());///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// sub view�� �� page�鿡 ��Ȯ�� ������ ���� �����ؾ� �Ѵ�	
		
		mSubView.setOnDefaultBirthDayEventAlertTimeSetListener(this);
		mSubView.setOnDefaultEventAlertTimeSetListener(this);
		mSubView.setOnDefaultAllDayEventAlertTimeSetListener(this);
		
		mViewSwitcher.addView(mMainView);	
	}
	
	public void makeCustomListPreference() {
		mDefaultBirthDayEventReminder = new CustomListPreference(mContext, mContext.getSharedPreferences(SettingsFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE), 
				SettingsFragment.KEY_DEFAULT_BIRTHDAY_EVENT_REMINDER);
		mDefaultBirthDayEventReminder.setEntries(R.array.default_special_event_reminder_labels);
		mDefaultBirthDayEventReminder.setEntryValues(R.array.default_special_event_reminder_values);
		
		String defaultBirthDayEventReminderValue = Utils.getSharedPreference(mContext, SettingsFragment.KEY_DEFAULT_BIRTHDAY_EVENT_REMINDER, 
				mContext.getResources().getString(R.string.preferences_birthday_event_default_reminder_default));		
		mDefaultBirthDayEventReminder.setValue(defaultBirthDayEventReminderValue);		
		
		
		mDefaultEventReminder = new CustomListPreference(mContext, mContext.getSharedPreferences(SettingsFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE), 
				SettingsFragment.KEY_DEFAULT_EVENT_REMINDER);
		mDefaultEventReminder.setEntries(R.array.ecalendar_default_reminder_labels);
		mDefaultEventReminder.setEntryValues(R.array.ecalendar_default_reminder_values);
		
		String defaultEventReminderValue = Utils.getSharedPreference(mContext, SettingsFragment.KEY_DEFAULT_EVENT_REMINDER, 
				mContext.getResources().getString(R.string.preferences_event_default_reminder_default));		
		mDefaultEventReminder.setValue(defaultEventReminderValue);
		
		
		mDefaultAllDayEventReminder = new CustomListPreference(mContext, mContext.getSharedPreferences(SettingsFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE), 
				SettingsFragment.KEY_DEFAULT_ALLDAY_EVENT_REMINDER);
		mDefaultAllDayEventReminder.setEntries(R.array.default_special_event_reminder_labels);
		mDefaultAllDayEventReminder.setEntryValues(R.array.default_special_event_reminder_values);
		
		String defaultAllDayEventReminderValue = Utils.getSharedPreference(mContext, SettingsFragment.KEY_DEFAULT_ALLDAY_EVENT_REMINDER, 
				mContext.getResources().getString(R.string.preferences_allday_event_default_reminder_default));		
		mDefaultAllDayEventReminder.setValue(defaultAllDayEventReminderValue);
	}
	
	
	
	AlphaAnimation mMainViewEnterDimmingAnimation;
	public void switchMainView() {
				
		float enterAnimationDistance = getWidth() * 0.4f;
    	float exitAnimationDistance = getWidth(); // 
    	
    	float velocity = SettingsFragment.SWITCH_SUB_PAGE_EXIT_VELOCITY;
    	long exitAnimationDuration = ITEAnimationUtils.calculateDuration(exitAnimationDistance, getWidth(), SettingsFragment.MINIMUM_SNAP_VELOCITY, velocity); 	    	
    	long enterAnimationDuration = exitAnimationDuration;
    	
    	mMainView.makeItemTouchDownReleaseValueAnimator(mSubView.getCurrentSubPage(), enterAnimationDuration);
    	
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
		 mFragment.mViewState = SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
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
	public void switchSubView(int pageCase) {
		// ���� ���⼭ �� ���� sub page�� ���� �׸���� �����ϰ�
		// sub page�� �����ؾ� �Ѵ�
		mSubView.configPage(pageCase);
		
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
    	
    	mViewSwitcher.addView(mSubView);
    				
		int fromView = mFragment.mViewState;
		mFragment.mViewState = SettingsFragment.ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE;
		int toView = mFragment.mViewState;			
				
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance, pageCase);	
		
		mViewSwitcher.showNext();		
	}

	@Override
	public void OnDefaultAllDayEventAlertTimeSet(Object newValue) {
		mDefaultAllDayEventReminder.setValue((String) newValue);			
	}

	@Override
	public void OnDefaultEventAlertTimeSet(Object newValue) {
		//mDefaultEventAlertTimeSetListener.OnDefaultEventAlertTimeSet(newValue);
		mDefaultEventReminder.setValue((String) newValue);
	}

	@Override
	public void OnDefaultBirthDayEventAlertTimeSet(Object newValue) {	
		mDefaultBirthDayEventReminder.setValue((String) newValue);
	}
	
}
