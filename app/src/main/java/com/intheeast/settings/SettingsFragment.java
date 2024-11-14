package com.intheeast.settings;

import java.util.Calendar;
import java.util.LinkedHashSet;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.ITESwitch;
import com.intheeast.timezone.TimeZoneData;
import com.intheeast.timezone.TimeZonePickerUtils;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.CalendarCache;
import android.provider.CalendarContract.Calendars;
//import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.LinearLayout.LayoutParams;

@SuppressLint("ValidFragment")
public class SettingsFragment extends Fragment implements TimeZoneSettingPane.OnHomeTimeZoneEnableSetListener,
	TimeZoneSettingPane.OnHomeTimeZoneSetListener {
	
	private static final String TAG = SettingsFragment.class.getSimpleName();
	private static boolean INFO = true;
	
	private static final String ACCOUNT_UNIQUE_KEY = "ACCOUNT_KEY";
	private MatrixCursor mAccountsCursor = null;	
	private static final String[] PROJECTION = new String[] {
        Calendars._ID,
        Calendars.ACCOUNT_TYPE,
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE + " || " + Calendars.ACCOUNT_NAME + " AS " +
                ACCOUNT_UNIQUE_KEY,
        Calendars.CALENDAR_DISPLAY_NAME,
    };
	
	public static final String CALENDARS_WHERE_WRITEABLE = Calendars.CALENDAR_ACCESS_LEVEL + ">=" + Calendars.CAL_ACCESS_CONTRIBUTOR;
	
	// The name of the shared preferences file. This name must be maintained for historical
	// reasons, as it's what PreferenceManager assigned the first time the file was created.
	//static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
	//static final String SHARED_PREFS_NAME_NO_BACKUP = "com.android.calendar_preferences_no_backup";	
	public static final String SHARED_PREFS_NAME = "com.intheeast.ecalendar_preferences";
	public static final String SHARED_PREFS_NAME_NO_BACKUP = "com.intheeast.ecalendar_preferences_no_backup";	
	
	// Preference keys
	public static final String KEY_IS_FIRST_RUNNING = "preferences_first_running_state";		
	
	public static final String KEY_HIDE_DECLINED = "preferences_hide_declined";
	public static final String KEY_WEEK_START_DAY = "preferences_week_start_day";
	public static final String KEY_SHOW_WEEK_NUM = "preferences_show_week_num";
	public static final String KEY_DAYS_PER_WEEK = "preferences_days_per_week";
	public static final String KEY_SKIP_SETUP = "preferences_skip_setup";
	
	public static final String KEY_CLEAR_SEARCH_HISTORY = "preferences_clear_search_history";
	
	public static final String KEY_STATUS_BAR_HEIGHT = "statusbar_height";
	
	public static final String KEY_ALERTS_CATEGORY = "preferences_alerts_category";
	public static final String KEY_ALERTS = "preferences_alerts";
	public static final String KEY_ALERTS_VIBRATE = "preferences_alerts_vibrate";
	public static final String KEY_ALERTS_RINGTONE = "preferences_alerts_ringtone";
	public static final String KEY_ALERTS_POPUP = "preferences_alerts_popup";
	
	public static final String KEY_SHOW_CONTROLS = "preferences_show_controls";
	
	public static final String KEY_DEFAULT_REMINDER = "preferences_default_reminder";
	public static final int NO_REMINDER = -1;
	public static final String NO_REMINDER_STRING = "-1";
	public static final int REMINDER_DEFAULT_TIME = 10; // in minutes
	
	public static final String KEY_DEFAULT_CELL_HEIGHT = "preferences_default_cell_height";	
	public static final String KEY_DEFAULT_EVENT_FONT_SIZE = "preferences_default_event_font_size";
	public static final String KEY_VERSION = "preferences_version";
	
	/** Key to SharePreference for default view (CalendarController.ViewType) */
	public static final String KEY_START_VIEW = "preferred_startView";
	/**
	*  Key to SharePreference for default detail view (CalendarController.ViewType)
	*  Typically used by widget
	*/
	public static final String KEY_DETAILED_VIEW = "preferred_detailedView";
	public static final String KEY_DEFAULT_CALENDAR_ID = "preference_defaultCalendar_id";
	public static final String KEY_DEFAULT_CALENDAR_ACCOUNT_NAME = "preference_defaultCalendar_account_name";
	public static final String KEY_DEFAULT_CALENDAR_ACCOUNT_TYPE = "preference_defaultCalendar_account_type";
	public static final String KEY_DEFAULT_CALENDAR_OWNER_ACCOUNT = "preference_defaultCalendar"; //Calendars.OWNER_ACCOUNT
	public static final String KEY_DEFAULT_CALENDAR_COLOR = "preference_defaultCalendar_color";
	public static final String KEY_DEFAULT_CALENDAR_DISP_NAME = "preference_defaultCalendar_displayname";
	public static final String KEY_DEFAULT_CALENDAR_MAX_REMINDERS = "preference_defaultCalendar_max_reminders";
	public static final String KEY_DEFAULT_CALENDAR_ALLOWED_REMINDERS = "preference_defaultCalendar_allowed_reminders";
	public static final String KEY_DEFAULT_CALENDAR_ALLOWED_ATTENDEE_TYPES = "preference_defaultCalendar_allowed_attendee_types";
	public static final String KEY_DEFAULT_CALENDAR_ALLOWED_AVAILABILITY = "preference_defaultCalendar_allowed_availability";
	
	public static final String DEFAULT_CALENDAR = "Default Calendar";
	public static final String DEFAULT_CALENDAR_DISPLAYNAME = "Ķ����";	
	public static final String DEFAULT_ECALENDAR_CALENDAR_DISPLAYNAME = "Ķ����";	
	
	// These must be in sync with the array preferences_week_start_day_values
	/*
	public static final String WEEK_START_DEFAULT = "-1";
	public static final String WEEK_START_SATURDAY = "7";
	public static final String WEEK_START_SUNDAY = "1";
	public static final String WEEK_START_MONDAY = "2";
	*/	
	public static final int WEEK_START_DEFAULT = -1;
	public static final int WEEK_START_SATURDAY = 7;
	public static final int WEEK_START_SUNDAY = 1;
	public static final int WEEK_START_MONDAY = 2;
	
	static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
    static final String KEY_HOME_TZ = "preferences_home_tz";
            
    public static final String KEY_DEFAULT_BIRTHDAY_EVENT_REMINDER = "preferences_default_birthday_event_reminder";
    public static final String KEY_DEFAULT_EVENT_REMINDER = "preferences_default_event_reminder";
    public static final String KEY_DEFAULT_ALLDAY_EVENT_REMINDER = "preferences_default_allday_event_reminder";
	
	
	// Default preference values
	public static final int DEFAULT_START_VIEW = CalendarController.ViewType.DAY;
	public static final int DEFAULT_DETAILED_VIEW = CalendarController.ViewType.DAY;
	public static final boolean DEFAULT_SHOW_WEEK_NUM = false;
	// This should match the XML file.
	public static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";	
	
	
	public static final String KEY_MONTH_VIEW_LAST_MODE = "preferences_month_view_last_mode";
	public static final int NORMAL_MONTH_VIEW_MODE = 1;
	public static final int EXPAND_EVENT_LIST_MONTH_VIEW_MODE = 2;	
	
	
	public static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float SWITCH_MAIN_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f; 
	public static final float SWITCH_SUB_PAGE_EXIT_VELOCITY = SWITCH_MAIN_PAGE_VELOCITY * 1.25f; 
	
	public static final int MAIN_VIEW_FRAGMENT_STATE = 1;
	public static final int TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE = 2;
	public static final int TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE = 3;
	public static final int ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE = 4;
	public static final int ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE = 5;
	public static final int WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE = 6;
	public static final int DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE = 7;
	
	
	Activity mActivity;
	Context mContext;
    DisplayMetrics mDisplayMetrics;
	LayoutInflater mInflater;
	static Resources mResources;
	
	LinearLayout mMainFragmentLayout;
	ViewSwitcher mMainFragmentLayoutViewSwitcher;
	
	RelativeLayout mMainView;
	//AnimationSet mMainViewExitAnimationSet;
	//AnimationSet mMainViewEnterAnimationSet;
	
	/*
	TranslateAnimation mMainViewExitAnimation;
	TranslateAnimation mMainViewEnterAnimation;		
	AlphaAnimation mMainViewExitDimmingAnimation;
	AlphaAnimation mMainViewEnterDimmingAnimation;
	*/
	
	TranslateAnimation mSubViewEnterAnimation;
	TranslateAnimation mSubViewExitAnimation;	
	
	CommonRelativeLayoutItemContainer mTimeZoneSettingItem;
	CommonRelativeLayoutItemContainer mWeekNumberSettingItem;
	CommonRelativeLayoutItemContainer mHideRejectedEventSettingItem;
	CommonRelativeLayoutItemContainer mAlertTimeSettingItem;
	CommonRelativeLayoutItemContainer mWeekStartDaySettingItem;
	CommonRelativeLayoutItemContainer mDefaultCalendarSettingItem;
	RelativeLayout mSurplusRegionView;
	
	TimeZoneSettingPane mTimeZoneSettingPane;	
	AlertTimeSettingPane mAlertTimeSettingPane;
	StartWeekOnSettingView mStartWeekOnSettingView;
	DefaultCalendarSettingView mDefaultCalendarSettingView;
	
	TimeZonePickerUtils mTzPickerUtils;
	
	int mViewState;
	
	String mTimeZoneId;
	
	TextView mSelectedTimeZoneText;
	String mSelectedTimeZoneName;
	ITESwitch mWeekNumberSettingItemSwitchButton;
	ITESwitch mhideRejectedEventSettingItemSwitchButton;
	
	TextView mDefaultCalendarDisplayNameText;
	String mDefaultCalendarDisplayName;
	int mDefaultCalendarId;
	//Cursor mDefaultCalendarCursor = null;  
	
	TextView mWeekStartDayText;
	
	public static final int MAINVEW_ITEMS_COUNT = 6;
	int mFrameLayoutHeight;	
	int mMainViewSurplusRegionHeight;
	
	/** Return a properly configured SharedPreferences instance */
	public static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
	}
	
	/** Set the default shared preferences in the proper context */
	public static void setDefaultValues(Context context) {
		// last para :  Whether to re-read the default values. 
		//              If false, this method will set the default values only if this method has never been called in the past 
		//              (or if the KEY_HAS_SET_DEFAULT_VALUES in the default value shared preferences file is false). 
		//              To attempt to set the default values again bypassing this check, set readAgain to true. 
		//PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
				//R.xml.general_preferences, false);
		
		boolean isFirstRunningState = Utils.getSharedPreference(
				context, SettingsFragment.KEY_IS_FIRST_RUNNING, true);
        if (isFirstRunningState) {
        	Utils.setSharedPreference(context, SettingsFragment.KEY_IS_FIRST_RUNNING, false);
        	        	        	
        	// ���� ECalendar�� ������ �� �ֵ��� �Ͽ���
        	// �̴� Settings���� ������ �߱� ��ų �� �ִ�
        	// �ֳ��ϸ� ECalendar �Ǵ� ���� ������ Calendar�� �����Ͽ��� ���,
        	// �ش� Calendar�� Default ���� �ƴ����� �Ǻ����� �ʰ�
        	// KEY_DEFAULT_CALENDAR_ID�� ���� ������Ʈ�� ���� �ʱ� �����̴�
        	// �̴� findDefaultCalendarPosition �ڵ带 Ȱ���� 
        	// ��ü ����Ʈ Ķ���� ������ �ۼ��ؾ� �Ѵ�
        	Utils.makeDefaultECalendar(context);        	
        	
        	//Utils.setSharedPreference
        	// preferences_hide_declined : false
        	// preferences_show_week_num : false        	
        	// preferences_home_tz_enabled : false
        	// preferences_alerts_vibrate : false
        	// preferences_alerts : true
        	// preferences_alerts_popup : true        	
        	
        	// preferences_week_start_day : @string/preferences_week_start_day_default
        	// preferences_home_tz : @string/preferences_home_tz_default
        	// preferences_default_reminder : @string/preferences_default_reminder_default        	
        	// preferences_alerts_ringtone : "content://settings/system/notification_sound"
        	// preferences_quick_responses : 
        	
        	// preferences_clear_search_history : �츮�� �������� ���� ���̴�        	
        	
        	SharedPreferences prefs = getSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_HIDE_DECLINED, false);
            editor.putBoolean(KEY_SHOW_WEEK_NUM, false);
            editor.putBoolean(KEY_HOME_TZ_ENABLED, false);
            editor.putBoolean(KEY_ALERTS_VIBRATE, false);            
            editor.putBoolean(KEY_ALERTS, true);
            editor.putBoolean(KEY_ALERTS_POPUP, true);
            
            int weekStartDay = Calendar.getInstance().getFirstDayOfWeek();
            editor.putInt(KEY_WEEK_START_DAY, weekStartDay);
            //editor.putString(KEY_WEEK_START_DAY, String.valueOf(weekStartDay));
            //editor.putString(KEY_WEEK_START_DAY, context.getResources().getString(R.string.preferences_week_start_day_default));    
            // preferences_home_tz_default = America/Los_Angeles
            // :�׷���, �츮�� ����̽��� Ÿ���� ����Ʈ ȨŸ�������� �����Ѵ�
            editor.putString(KEY_HOME_TZ, ETime.getCurrentTimezone());
            // ������ó�� �����δ��� ���� ���� ����Ʈ��!!!
            editor.putString(KEY_DEFAULT_REMINDER, NO_REMINDER_STRING/*context.getResources().getString(R.string.preferences_default_reminder_default)*/);
            editor.putString(KEY_ALERTS_RINGTONE, DEFAULT_RINGTONE);
            
            editor.putString(KEY_DEFAULT_BIRTHDAY_EVENT_REMINDER, context.getResources().getString(R.string.preferences_birthday_event_default_reminder_default));
            editor.putString(KEY_DEFAULT_EVENT_REMINDER, context.getResources().getString(R.string.preferences_event_default_reminder_default));
            editor.putString(KEY_DEFAULT_ALLDAY_EVENT_REMINDER, context.getResources().getString(R.string.preferences_allday_event_default_reminder_default));
            
            String[] quickResponseDefaultValues = context.getResources().getStringArray(R.array.quick_response_defaults);
            LinkedHashSet<String> set = new LinkedHashSet<String>();
            for (String value : quickResponseDefaultValues) {
                set.add(value);
            }
            editor.putStringSet(Utils.KEY_QUICK_RESPONSES, set);
            
            editor.apply();
        }
	}
	
	AlphaAnimation mItemTouchDownAlphaAnim;
	AnimationListener mItemTouchDownAlphaAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			int bgColor = Color.argb(255, 224, 224, 224);
			mTimeZoneSettingItem.setBackgroundColor(bgColor);			
		}

		@Override
		public void onAnimationEnd(Animation animation) {			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
	AnimationListener mItemTouchDownReleaseAlphaAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			int bgColor = Color.argb(255, 255, 255, 255);
			mTimeZoneSettingItem.setBackgroundColor(bgColor);		
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
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
	
	OnClickListener mItemOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int viewId = v.getId();
			switch(viewId) {
			case R.id.timezone_setting_item_container:
				switchingToSubView(TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE);	
				break;
			case R.id.alert_time_setting_item_container:
				switchingToSubView(ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE);	
				break;
			case R.id.start_day_setting_item_container:
				switchingToSubView(WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE);	
				break;
			case R.id.default_calendar_select_setting_item_container:
				switchingToSubView(DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE);	
				break;
			}
			
		}
		
	};
	
	
	OnClickListener mmWeekNumberSettingItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			boolean currentChecked = Utils.getSharedPreference(mContext, KEY_SHOW_WEEK_NUM, false);
			if (currentChecked)
				currentChecked = false;
			else
				currentChecked = true;
			
			Utils.setSharedPreference(mContext, KEY_SHOW_WEEK_NUM, currentChecked);
			mWeekNumberSettingItemSwitchButton.setChecked(currentChecked);
		}
		
	};
	
	
	OnClickListener mhideRejectedEventSettingItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			boolean currentChecked = Utils.getSharedPreference(mContext, KEY_HIDE_DECLINED, false);
			if (currentChecked)
				currentChecked = false;
			else
				currentChecked = true;
			
			Utils.setSharedPreference(mContext, KEY_HIDE_DECLINED, currentChecked);
			mhideRejectedEventSettingItemSwitchButton.setChecked(currentChecked);
		}
		
	};
			
	AnimationListener mMainViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			mActionBar.startActionBarAnim();
			int bgColor = Color.argb(30, 0, 0, 0);
	    	mSwitchDimmingLayout.setBackgroundColor(bgColor);
			mSwitchDimmingLayout.startAnimation(mMainViewEnterDimmingAnimation);
			
			mItemTouchDownReleaseVA.start();
			
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			int bgColor = Color.argb(0, 0, 0, 0);
	    	mSwitchDimmingLayout.setBackgroundColor(bgColor);
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
	    	mSwitchDimmingLayout.setBackgroundColor(bgColor);
			mSwitchDimmingLayout.startAnimation(mMainViewExitDimmingAnimation);
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
			if (INFO) Log.i(TAG, "mSelectTimeZoneSubViewEnterAnimationListener:onAnimationStart");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			if (INFO) Log.i(TAG, "mSelectTimeZoneSubViewEnterAnimationListener:onAnimationEnd");
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	TimeZoneData mTimeZoneData;
	public SettingsFragment(TimeZoneData timeZoneData, TimeZonePickerUtils tzPickerUtils) {
		mTimeZoneData = timeZoneData;
		mTzPickerUtils = tzPickerUtils;
		
		mViewState = MAIN_VIEW_FRAGMENT_STATE;
	}
	
	SettingsActionBarFragment mActionBar;
	public void setActionBarFragment(SettingsActionBarFragment actionBar) {
    	mActionBar = actionBar;
    }
	
	//CustomListPreference mDefaultEventReminder;
	//CharSequence[] mEntries;
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		mActivity = activity;
		mContext = activity.getApplicationContext();
		mResources = getResources();
		mDisplayMetrics = getResources().getDisplayMetrics();
		mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mFrameLayoutHeight = calcFrameLayoutHeight(activity);
		mMainViewSurplusRegionHeight = calcMainViewSurplusRegion(mResources, mFrameLayoutHeight);		
	}
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		// Make sure to always use the same preferences file regardless of the package name
        // we're running under
        //final PreferenceManager preferenceManager = new PreferenceManager(mActivity, 100);
        //final SharedPreferences sharedPreferences = getSharedPreferences(mActivity);
        //preferenceManager.setSharedPreferencesName(SHARED_PREFS_NAME);		
		mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);		
		
		mTimeZoneId = Utils.getTimeZone(mActivity, null);
				
        CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(getActivity(), mTimeZoneId,
                System.currentTimeMillis(), false);
        // timezoneName�� �ѱ� ǥ�ؽ�  GMT+9�� �����ȴ�..........................................................................................................
        mSelectedTimeZoneName = timezoneName.toString();        
	}
		
	View mSwitchDimmingLayout;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (INFO) Log.i(TAG, "onCreateView");		
		
		mMainFragmentLayout = (LinearLayout) inflater.inflate(R.layout.settings_main_fragment_layout, null);
		mMainFragmentLayoutViewSwitcher = (ViewSwitcher) mMainFragmentLayout.findViewById(R.id.settings_main_fragment_switcher);
        
        mMainView = (RelativeLayout) inflater.inflate(R.layout.settings_mainview_layout, null);
        mSwitchDimmingLayout = mMainView.findViewById(R.id.mainview_switch_dimming_layout);     
        mTimeZoneSettingPane = (TimeZoneSettingPane) inflater.inflate(R.layout.settings_timezone_override_pane, null);
        mAlertTimeSettingPane = (AlertTimeSettingPane) inflater.inflate(R.layout.settings_alerttime_override_pane, null);
        mStartWeekOnSettingView = (StartWeekOnSettingView) inflater.inflate(R.layout.settings_startweekon_override_mainview, null);   
        mDefaultCalendarSettingView = (DefaultCalendarSettingView) inflater.inflate(R.layout.settings_defaultcalendar_override_mainview, null);           
        
        mTimeZoneSettingItem = (CommonRelativeLayoutItemContainer) mMainView.findViewById(R.id.timezone_setting_item_container);   
        mWeekNumberSettingItem = (CommonRelativeLayoutItemContainer) mMainView.findViewById(R.id.week_number_setting_item_container);
        mHideRejectedEventSettingItem = (CommonRelativeLayoutItemContainer) mMainView.findViewById(R.id.hide_rejected_event_setting_item_container);		
        mAlertTimeSettingItem = (CommonRelativeLayoutItemContainer) mMainView.findViewById(R.id.alert_time_setting_item_container);        
        mWeekStartDaySettingItem = (CommonRelativeLayoutItemContainer) mMainView.findViewById(R.id.start_day_setting_item_container);
        mDefaultCalendarSettingItem = (CommonRelativeLayoutItemContainer) mMainView.findViewById(R.id.default_calendar_select_setting_item_container);        
        mSurplusRegionView = (RelativeLayout) mMainView.findViewById(R.id.settings_mainview_surplus_region);		
        
		mSelectedTimeZoneText = (TextView) mTimeZoneSettingItem.findViewById(R.id.timezone_setting_item_selected_timezone_text);
		mWeekNumberSettingItemSwitchButton = (ITESwitch) mWeekNumberSettingItem.findViewById(R.id.week_number_setting_item_switch);
		reAdjustSwithButtonMargin(mResources, mWeekNumberSettingItemSwitchButton);
		mhideRejectedEventSettingItemSwitchButton = (ITESwitch) mHideRejectedEventSettingItem.findViewById(R.id.hide_rejected_event_setting_item_switch);
		reAdjustSwithButtonMargin(mResources, mhideRejectedEventSettingItemSwitchButton);		
		mWeekStartDayText = (TextView) mWeekStartDaySettingItem.findViewById(R.id.start_day_setting_item_text);		
		mDefaultCalendarDisplayNameText = (TextView) mDefaultCalendarSettingItem.findViewById(R.id.default_calendar_select_setting_item_text);		
        
		View shapeView = mSurplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams shapeViewParams = (RelativeLayout.LayoutParams) shapeView.getLayoutParams();
		shapeViewParams.height = mMainViewSurplusRegionHeight;
		shapeView.setLayoutParams(shapeViewParams);
		
		
		mTimeZoneSettingItem.setOnTouchListener(mItemTouchListener);
		mAlertTimeSettingItem.setOnTouchListener(mItemTouchListener);		
		mWeekStartDaySettingItem.setOnTouchListener(mItemTouchListener);
		//mDefaultCalendarSettingItem.setOnTouchListener(mItemTouchListener);
		
		//mItemOnClickListener
		mTimeZoneSettingItem.setOnClickListener(mItemOnClickListener);
		mAlertTimeSettingItem.setOnClickListener(mItemOnClickListener);		
		mWeekStartDaySettingItem.setOnClickListener(mItemOnClickListener);
		//mDefaultCalendarSettingItem.setOnClickListener(mItemOnClickListener);		
		
        //mTimeZoneSettingItem.setOnClickListener(mTimeZoneSettingItemOnClickListener);
        mWeekNumberSettingItemSwitchButton.setOnClickListener(mmWeekNumberSettingItemClickListener);
        mhideRejectedEventSettingItemSwitchButton.setOnClickListener(mhideRejectedEventSettingItemClickListener);
		//mAlertTimeSettingItem.setOnClickListener(mAlertTimeSettingItemOnClickListener);
		//mWeekStartDaySettingItem.setOnClickListener(mWeekStartDaySettingItemOnClickListener);		
		
		if (Utils.getSharedPreference(mContext, KEY_HOME_TZ_ENABLED, false))
			mSelectedTimeZoneText.setText(mSelectedTimeZoneName);
		else 
			mSelectedTimeZoneText.setText("Off");
		
		mWeekNumberSettingItemSwitchButton.setChecked(Utils.getSharedPreference(mContext, KEY_SHOW_WEEK_NUM, false));		
		mhideRejectedEventSettingItemSwitchButton.setChecked(Utils.getSharedPreference(mContext, KEY_HIDE_DECLINED, false));		
		mWeekStartDayText.setText(Utils.getFristDayOfWeekString(mContext));	
		mDefaultCalendarDisplayNameText.setText(mDefaultCalendarDisplayName);		
        
        mTimeZoneSettingPane.initView(this, mActionBar, mTimeZoneId, mTimeZoneData, mTzPickerUtils);
        mTimeZoneSettingPane.setOnHomeTimeZoneEnableSetListener(this);
        mTimeZoneSettingPane.setOnHomeTimeZoneSetListener(this);       
        mAlertTimeSettingPane.initView(this, mActionBar);
        mStartWeekOnSettingView.initView(this, mActionBar, mFrameLayoutHeight);      
        
        
        mMainFragmentLayoutViewSwitcher.addView(mMainView);        
        
        return mMainFragmentLayout;
	}		
	
	
	
	@Override
	public void onResume() {		
		super.onResume();		
		
		if (INFO) Log.i(TAG, "onResume");
		
		new AsyncQueryHandler(mContext.getContentResolver()) {
			
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                mAccountsCursor = Utils.matrixCursorFromCursor(cursor);                 
                
                mDefaultCalendarSettingView.initView(SettingsFragment.this, mActionBar, mAccountsCursor);                
                //mDefaultCalendarSettingItem.setClickable(true);
                // setClickable(false)�� �����ص� 
                // setOnClickListener�� �����ϸ� setClickable(false)�� ��������...��???
                // :�Ʒ� ó�� isClickable()�� false�� �����ϸ� setClickable(true)�� ������
                //  public void setOnClickListener(OnClickListener l) {
			        	//if (!isClickable()) {
			            	//setClickable(true);
			        	//}
			        	//getListenerInfo().mOnClickListener = l;
    			//  }
                mDefaultCalendarSettingItem.setBackground(mResources.getDrawable(R.drawable.common_layout_selector)); 
                //mDefaultCalendarSettingItem.setOnClickListener(mDefaultCalendarSettingItemOnClickListener); 
                mDefaultCalendarSettingItem.setOnTouchListener(mItemTouchListener);
                mDefaultCalendarSettingItem.setOnClickListener(mItemOnClickListener);
            }
        }.startQuery(0, null, Calendars.CONTENT_URI, PROJECTION,
                "1) GROUP BY (" + ACCOUNT_UNIQUE_KEY, //Cheap hack to make WHERE a GROUP BY query
                null ,
                Calendars.ACCOUNT_NAME); // account�� group���� �ؼ� �� �ϳ��� Ķ���� ���� Result�� �����Ѵ�    
        
	}
	
	
	

	@Override
	public void onPause() {
		
		mDefaultCalendarSettingItem.setBackgroundColor(Color.WHITE);
		mDefaultCalendarSettingItem.setOnTouchListener(null);
		mDefaultCalendarSettingItem.setOnClickListener(null);
		
		super.onPause();
	}
	
	@Override
	public void onStop() {		
		super.onStop();		
		/*
		if ( mDefaultCalendarCursor != null && !mDefaultCalendarCursor.isClosed()) {
			mDefaultCalendarCursor.close();
        }
		*/
		
		mDefaultCalendarSettingView.closeChildrenCursors();
        
        if (mAccountsCursor != null && !mAccountsCursor.isClosed()) {
            mAccountsCursor.close();
        }
	}
	
	public void makeItemTouchDownReleaseValueAnimator(CommonRelativeLayoutItemContainer v, long enterAnimationDuration) {
		final CommonRelativeLayoutItemContainer itemContainer = v;
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

	
	AlphaAnimation mMainViewEnterDimmingAnimation;
	ValueAnimator mItemTouchDownReleaseVA;
	public void switchMainView() {
		float enterAnimationDistance = mMainFragmentLayout.getWidth() * 0.4f;
    	float exitAnimationDistance = mMainFragmentLayout.getWidth(); // 
    	
    	float velocity = SWITCH_SUB_PAGE_EXIT_VELOCITY;
    	long exitAnimationDuration = ITEAnimationUtils.calculateDuration(exitAnimationDistance, mMainFragmentLayout.getWidth(), MINIMUM_SNAP_VELOCITY, velocity); 	    	
    	long enterAnimationDuration = exitAnimationDuration;
    	
    	CommonRelativeLayoutItemContainer v = null;
		switch(mViewState) {
		case TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			v = mTimeZoneSettingItem;			
			break;
		case ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			v = mAlertTimeSettingItem;
			break;
		case WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			String day = Utils.getFristDayOfWeekString(mContext);
			mWeekStartDayText.setText(day);
			v = mWeekStartDaySettingItem;
			break;			
		case DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			mDefaultCalendarDisplayNameText.setText(mDefaultCalendarDisplayName);
			v = mDefaultCalendarSettingItem;
			break;
		}		
		
		if (v != null)
			makeItemTouchDownReleaseValueAnimator(v, enterAnimationDuration);
		else {
			// ����ó���� ����!!!!
		}
		
    	
    	mSubViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, 1, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	mSubViewExitAnimation.setDuration(exitAnimationDuration);  
    	ITEAnimInterpolator exitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mSubViewExitAnimation);  
    	mSubViewExitAnimation.setInterpolator(exitInterpolator);    	
    	
    	
    	TranslateAnimation mainViewEnterSlideAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
		mainViewEnterSlideAnimation.setDuration(enterAnimationDuration);
		ITEAnimInterpolator enterSlideInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mainViewEnterSlideAnimation); 
		mainViewEnterSlideAnimation.setInterpolator(enterSlideInterpolator);
		mainViewEnterSlideAnimation.setAnimationListener(mMainViewEnterAnimationListener);		
		
    	mMainViewEnterDimmingAnimation = new AlphaAnimation(1.0f, 0.0f); 
    	mMainViewEnterDimmingAnimation.setDuration(enterAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mMainViewEnterDimmingAnimation);   	
    	mMainViewEnterDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);		
    	
		mMainFragmentLayoutViewSwitcher.setInAnimation(mainViewEnterSlideAnimation);
    	mMainFragmentLayoutViewSwitcher.setOutAnimation(mSubViewExitAnimation);       	
		
		int fromView = mViewState;
		mViewState = MAIN_VIEW_FRAGMENT_STATE;
		int toView = mViewState;				
				
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance, 0);	
				
		mMainFragmentLayoutViewSwitcher.showPrevious();
		// �����ؾ� �Ѵ�
		switch(fromView) {
		case TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			mMainFragmentLayoutViewSwitcher.removeView(mTimeZoneSettingPane);
			break;
		case ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			mMainFragmentLayoutViewSwitcher.removeView(mAlertTimeSettingPane);
			break;
		case WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			mMainFragmentLayoutViewSwitcher.removeView(mStartWeekOnSettingView);
			break;
		case DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			mMainFragmentLayoutViewSwitcher.removeView(mDefaultCalendarSettingView);
			break;
		}
	}
	
	AlphaAnimation mMainViewExitDimmingAnimation;
	private void switchingToSubView(int subPage) {
		float enterAnimationDistance = mMainFragmentLayout.getWidth(); 
		float exitAnimationDistance = mMainFragmentLayout.getWidth() * 0.4f; 
		
		float velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mMainFragmentLayout.getWidth(), MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
		
    	mSubViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	mSubViewEnterAnimation.setDuration(enterAnimationDuration);
    	ITEAnimInterpolator subViewEnterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mSubViewEnterAnimation);   	
    	mSubViewEnterAnimation.setInterpolator(subViewEnterInterpolator);
    	mSubViewEnterAnimation.setAnimationListener(mSubViewEnterAnimationListener);    	
    	
    	mMainViewExitDimmingAnimation = new AlphaAnimation(0.0f, 1.0f); 
    	mMainViewExitDimmingAnimation.setDuration(exitAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mMainViewExitDimmingAnimation);   	
    	mMainViewExitDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);
    	    	
    	TranslateAnimation mainViewExitSlideAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);		
    	ITEAnimInterpolator mainViewExitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mainViewExitSlideAnimation); 
    	mainViewExitSlideAnimation.setInterpolator(mainViewExitInterpolator);
    	mainViewExitSlideAnimation.setDuration(exitAnimationDuration);    	
    	mainViewExitSlideAnimation.setAnimationListener(mMainViewExitAnimationListener);
    	
    	mMainFragmentLayoutViewSwitcher.setInAnimation(mSubViewEnterAnimation);
    	mMainFragmentLayoutViewSwitcher.setOutAnimation(mainViewExitSlideAnimation);
    	
    	int fromView = mViewState;
    	switch(subPage) {
    	case TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
    		mViewState = TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
    		mMainFragmentLayoutViewSwitcher.addView(mTimeZoneSettingPane);
    		break;
    	case ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
    		mViewState = ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
    		mMainFragmentLayoutViewSwitcher.addView(mAlertTimeSettingPane);
    		break;
    	case WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
    		mViewState = WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
    		mMainFragmentLayoutViewSwitcher.addView(mStartWeekOnSettingView);
    		break;
    	case DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
    		mViewState = DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE;
    		mMainFragmentLayoutViewSwitcher.addView(mDefaultCalendarSettingView);
    		break;
    	}
    	int toView = mViewState;				
				
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance, 0);			
		
		mMainFragmentLayoutViewSwitcher.showNext();	
	}
	
	
			
	
	public void notifyedMsgByActionBar(int msg) {
		switch(msg) {
		case SettingsActionBarFragment.SELECTED_PREVIOUS_ACTION:
			// ���� ���⼭ �츮�� ViewState ���� Ȯ������!!!
			switch(mViewState) {
			case TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
				switchMainView();
				break;
			case TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE:
				mTimeZoneSettingPane.switchMainView();
				break;
			case ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
				switchMainView();
				break;
			case ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE:
				mAlertTimeSettingPane.switchMainView();
				break;
			case WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
				switchMainView();
				break;			
			case DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
				switchMainView();
				break;
			}
			
			break;
		}
	}
	
	@Override
	public void onHomeTimeZoneEnableSet(boolean enabled, String homeTZId) {
		if (enabled) {
			//String homeTZId = Utils.getSharedPreference(mContext, SettingsFragment.KEY_HOME_TZ, Time.getCurrentTimezone());			
			Utils.setTimeZone(mContext, homeTZId);
			
			CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(mContext, homeTZId,
	                System.currentTimeMillis(), false);        
	        String homeTZName = timezoneName.toString();   
	        mSelectedTimeZoneText.setText(homeTZName);
		}
		else {
			Utils.setTimeZone(mContext, CalendarCache.TIMEZONE_TYPE_AUTO);
			
			mSelectedTimeZoneText.setText("Off");
		}
		
	}

	@Override
	public void onHomeTimeZoneSet(String tzId) {	
		if (tzId != null) {
			mTimeZoneId = tzId;
			
	        CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(getActivity(), mTimeZoneId,
	                System.currentTimeMillis(), false);
	        
	        mSelectedTimeZoneName = timezoneName.toString();  
	        mSelectedTimeZoneText.setText(mSelectedTimeZoneName);
	        
	        Utils.setTimeZone(getActivity(), tzId);    
		}		
	}
	
	public ITESwitch makeCustomSwitch(int itemContainerId) {
		int itemContainerHeight = (int) getResources().getDimension(R.dimen.selectCalendarViewItemHeight); //48dp
		
		String switchOnText = mResources.getString(R.string.selectcalendars_show_declined_events_on);
		String switchOffText = mResources.getString(R.string.selectcalendars_show_declined_events_off);
		int textColorUnChecked = mResources.getColor(R.color.selectCalendarViewSwitchOffTextColor);
		int textColorChecked = mResources.getColor(R.color.selectCalendarViewSwitchOnTextColor);
		
		Drawable switchBGDrawable = mResources.getDrawable(R.drawable.switch_bg_holo_light); 
		// switch drawable�� ������ switch_thumb_activated_holo_light�� ������� ����
		// ���� ���̼����� ���� �̹����� ����϶�
		// �׸��� ���� ���̼����� �̹��� �̸� �տ� added[added_switch_thumb_activated_holo_light]�� �߰��Ͽ���
		// :�̷��� ���� ������ switch button�� width�� half�� ������ on/off�� ������ ���� ���� ���Ѵ�
		Drawable switchDrawable = mResources.getDrawable(R.drawable.added_switch_thumb_activated_holo_light);
		int switchDrawableHeight = switchDrawable.getIntrinsicHeight(); // explorer������ height�� 34�̴� �׷��� getIntrinsicHeight������ 43�̴� ��???
		
		ITESwitch switchButton = new ITESwitch(mContext, switchOnText, switchOffText,
				textColorUnChecked, textColorChecked,
				switchBGDrawable, 
				switchDrawable);		
		
		RelativeLayout.LayoutParams switchButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);		
		switchButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		int parentHeightHalf = itemContainerHeight / 2;
		int switchHeightHalf = switchDrawableHeight / 2;
		int topMargin = parentHeightHalf - switchHeightHalf;		
		//int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewSwitchCommonRightMargin);
		int rightMargin = 20; // for test
		switchButtonLayoutParams.setMargins(0, topMargin, rightMargin, 0);				
		switchButton.setLayoutParams(switchButtonLayoutParams);	
		
		if (itemContainerId != -1)
			switchButton.setTag(itemContainerId);
		return switchButton;
		
	}
	
	public static void reAdjustSwithButtonMargin(Resources resources, ITESwitch switchButton) {
		int itemContainerHeight = (int) resources.getDimension(R.dimen.selectCalendarViewItemHeight); //48dp
		Drawable switchDrawable = resources.getDrawable(R.drawable.added_switch_thumb_activated_holo_light);
		int switchDrawableHeight = switchDrawable.getIntrinsicHeight(); // explorer������ height�� 34�̴� �׷��� getIntrinsicHeight������ 43�̴� ��???
		
		RelativeLayout.LayoutParams switchButtonLayoutParams = (RelativeLayout.LayoutParams) switchButton.getLayoutParams();		
		
		int parentHeightHalf = itemContainerHeight / 2;
		int switchHeightHalf = switchDrawableHeight / 2;
		int topMargin = parentHeightHalf - switchHeightHalf;		
		
		switchButtonLayoutParams.topMargin = topMargin;	
		switchButton.setLayoutParams(switchButtonLayoutParams);	
	}
	
	public void setDefaultCalendarInfo(int defaultCalendarId, String defaultCalendarDispName) {
		mDefaultCalendarId = defaultCalendarId;
		mDefaultCalendarDisplayName = defaultCalendarDispName;
	}
	
	public static String getDefaultCalendarDisplayName(ContentResolver cr, int defaultCalendarId) {
		Cursor cursor = queryDefaultCalendar(cr, defaultCalendarId);
		
		// ���� count���� 1�� �ƴϸ�???...
		int count = cursor.getCount();
		cursor.moveToPosition(-1);
		cursor.moveToNext();
        	
        int displayNameColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
        String displayName = cursor.getString(displayNameColumn);
        cursor.close();
        
        return displayName;	            
	}
	
	public static Cursor queryDefaultCalendar(ContentResolver cr, int defaultCalendarId) {
		Cursor cursor = null;  	
		Cursor newCursor = null; 
		Uri defaultCalendarQueryUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, defaultCalendarId);  
				
		try {
			cursor = cr.query(defaultCalendarQueryUri, PROJECTION, null, null, null);
		
			newCursor = Utils.matrixCursorFromCursor(cursor);
		
		} catch (Exception e) {
			Log.i(TAG, e.toString());				
		} finally {
			cursor.close();
		}
		
		return newCursor;
	}
	
	public int getFrameLayoutHeight() {
		return mFrameLayoutHeight;
	}	
	
	public static int calcFrameLayoutHeight(Activity activity) {		
		int deviceHeight = activity.getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = activity.getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	int StatusBarHeight = Utils.getSharedPreference(activity.getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(activity.getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight(activity.getResources()));        	
        }
    	
    	int upperActionBarHeight = (int)activity.getResources().getDimension(R.dimen.settings_view_upper_actionbar_height);      	
    	
    	int frameLayoutHeight = deviceHeight - StatusBarHeight - upperActionBarHeight;  
    	return frameLayoutHeight;
	}	
	
	public static int calcMainViewSurplusRegion(Resources res, int frameLayoutHeight) {
		
		int firstSeperatorHeight = (int)res.getDimension(R.dimen.selectCalendarViewSeperatorHeight);     
		int itemHeight = (int)res.getDimension(R.dimen.selectCalendarViewItemHeight); 
		int entireItemsHeight = itemHeight * MAINVEW_ITEMS_COUNT;
		int mainViewSurplusRegionHeight = frameLayoutHeight - (firstSeperatorHeight + entireItemsHeight);
		
		return mainViewSurplusRegionHeight;
	}
	
	public static int getStatusBarHeight(Resources res) {    	
    	int result = 0;
    	int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = res.getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
	
	
}
