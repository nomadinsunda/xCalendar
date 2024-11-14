package com.intheeast.settings;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.CommonRelativeLayoutUpperActionBar;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.ITESwitch;
import com.intheeast.timezone.TimeZoneData;
import com.intheeast.timezone.TimeZonePickerUtils;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SettingsActivity extends Activity {
	
	private static final String TAG = SettingsActivity.class.getSimpleName();
	private static boolean INFO = true;
	
	Bitmap mCallerEntireRegionBitmap;
	
	private CalendarController mController;
	ImageView mCallerActivityBitmapContainer;	
	LinearLayout mSettinsRealLayout;
	FrameLayout mSettingsActionbar;
	FrameLayout mSettingsMainPane;
	
	TranslateAnimation mEnterTranslateAnim;
	TranslateAnimation mExitTranslateAnim;

	private static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float ENTRY_ANIMATION_VELOCITY = MINIMUM_SNAP_VELOCITY * 3; 
	public static final float EXIT_ANIMATION_VELOCITY = MINIMUM_SNAP_VELOCITY * 4; 
	
	public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
	
	int mWindowHeight;
	public void calcFrameLayoutHeight() {
    	int deviceHeight = getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	int StatusBarHeight = Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
    	
    	mWindowHeight = deviceHeight - StatusBarHeight;
	}	
	
	private void finishActivity() {
		finish();
	}
	
	AnimationListener mEntranceAnimlistener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			mSettinsRealLayout.setVisibility(View.VISIBLE);	
			mSettingsActionbar.setVisibility(View.VISIBLE);
			mSettingsMainPane.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mCallerActivityBitmapContainer.setVisibility(View.GONE);
			
			initFragments();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
	AnimationListener mExitAnimlistener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mSettinsRealLayout.setVisibility(View.GONE);
			finishActivity();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
	public void makeEntranceAnimation() {
		float velocity = ENTRY_ANIMATION_VELOCITY;
		float enterAnimationDistance = mWindowHeight;
		long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mWindowHeight, MINIMUM_SNAP_VELOCITY, velocity); 
		
		mEnterTranslateAnim = new TranslateAnimation(
				Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0);	
		
		ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mEnterTranslateAnim);  
		
		mEnterTranslateAnim.setDuration(enterAnimationDuration);
		mEnterTranslateAnim.setInterpolator(enterInterpolator);		
		mEnterTranslateAnim.setAnimationListener(mEntranceAnimlistener);
	}
	
	public void makeExitAnimation() {
		float velocity = EXIT_ANIMATION_VELOCITY;
		float enterAnimationDistance = mWindowHeight;
		long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mWindowHeight, MINIMUM_SNAP_VELOCITY, velocity); 
		
		mExitTranslateAnim = new TranslateAnimation(
				Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, 1);	
		
		ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mExitTranslateAnim);  
		
		mExitTranslateAnim.setDuration(enterAnimationDuration);
		mExitTranslateAnim.setInterpolator(enterInterpolator);
		mExitTranslateAnim.setAnimationListener(mExitAnimlistener);
	}
	
	public void prepareEntranceExitAnimation() {
		String defaultCalendarDisplayName = "None";
		
		int defaultCalendarId = Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_DEFAULT_CALENDAR_ID, -1);
		
		if (defaultCalendarId != -1) {			
			defaultCalendarDisplayName = SettingsFragment.getDefaultCalendarDisplayName(getContentResolver(), defaultCalendarId);           
		}
		
		mMainFragment.setDefaultCalendarInfo(defaultCalendarId, defaultCalendarDisplayName);
		
		ECalendarApplication app = (ECalendarApplication) getApplication();
		mCallerEntireRegionBitmap = app.getCalendarEntireRegionBitmap();	
		
		// ���� �ð��밡 ������ �Ѿ�� ���� ���� bitmap�̶��,,,
		// exit �� old bitmap���� ����...
		// ������[Ķ������ ��Ī�ϴ� ���� �ƴ϶� �۾�������]ó��... 
		mCallerActivityBitmapContainer = (ImageView) findViewById(R.id.caller_activity_bitmap_container);
		mCallerActivityBitmapContainer.setImageBitmap(mCallerEntireRegionBitmap);
		
		mSettinsRealLayout = (LinearLayout) findViewById(R.id.settins_real_layout);		
		mSettingsActionbar = (FrameLayout) mSettinsRealLayout.findViewById(R.id.settings_actionbar);
		mSettingsMainPane = (FrameLayout) mSettinsRealLayout.findViewById(R.id.settings_main_pane);
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);    	
		
		CommonRelativeLayoutUpperActionBar actionbar = (CommonRelativeLayoutUpperActionBar) inflater.inflate(R.layout.settings_actionbar, null, false);    		
		mSettingsActionbar.addView(actionbar);
		
		RelativeLayout mainview = (RelativeLayout) inflater.inflate(R.layout.settings_mainview_layout, null, false);
		
		TextView timezoneNameTextView = (TextView) mainview.findViewById(R.id.timezone_setting_item_selected_timezone_text);		
		CommonRelativeLayoutItemContainer weekNumberSettingItem = (CommonRelativeLayoutItemContainer) mainview.findViewById(R.id.week_number_setting_item_container);
		ITESwitch mWeekNumberSettingItemSwitchButton = (ITESwitch) weekNumberSettingItem.findViewById(R.id.week_number_setting_item_switch);
		SettingsFragment.reAdjustSwithButtonMargin(getResources(), mWeekNumberSettingItemSwitchButton);			
		CommonRelativeLayoutItemContainer hideRejectedEventSettingItem = (CommonRelativeLayoutItemContainer) mainview.findViewById(R.id.hide_rejected_event_setting_item_container);
		ITESwitch mhideRejectedEventSettingItemSwitchButton = (ITESwitch) hideRejectedEventSettingItem.findViewById(R.id.hide_rejected_event_setting_item_switch);
		SettingsFragment.reAdjustSwithButtonMargin(getResources(), mhideRejectedEventSettingItemSwitchButton);
				
		TextView weekStartDayText = (TextView) mainview.findViewById(R.id.start_day_setting_item_text);		
			
		TextView defaultCalendarTextView = (TextView) mainview.findViewById(R.id.default_calendar_select_setting_item_text);		
		
		RelativeLayout surplusRegionView = (RelativeLayout) mainview.findViewById(R.id.settings_mainview_surplus_region);
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		boolean mUseHomeTZ = Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_HOME_TZ_ENABLED, false);
		if (mUseHomeTZ)
			timezoneNameTextView.setText(mTimeZoneName);
		else
			timezoneNameTextView.setText("Off");
		
		mWeekNumberSettingItemSwitchButton.setChecked(Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_SHOW_WEEK_NUM, false));
		mhideRejectedEventSettingItemSwitchButton.setChecked(Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_HIDE_DECLINED, false));
		weekStartDayText.setText(Utils.getFristDayOfWeekString(getApplicationContext()));
		defaultCalendarTextView.setText(defaultCalendarDisplayName);
		
		View shapeView = surplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams shapeViewParams = (RelativeLayout.LayoutParams) shapeView.getLayoutParams();
		shapeViewParams.height = mMainViewSurplusRegionHeight;
		shapeView.setLayoutParams(shapeViewParams);
		
		mSettingsMainPane.addView(mainview);
		
		mSettingsActionbar.setVisibility(View.INVISIBLE);
		mSettingsMainPane.setVisibility(View.INVISIBLE);
		mSettinsRealLayout.setVisibility(View.INVISIBLE);//////////////////////////////////////////////////////////////////////////////////
		
		makeEntranceAnimation();
		makeExitAnimation();
		
	}
	
	SettingsFragment mMainFragment;
	SettingsActionBarFragment mActionbarFragment;
	private void initFragments() {
		
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        mActionbarFragment = new SettingsActionBarFragment(SettingsFragment.MAIN_VIEW_FRAGMENT_STATE, mMainFragment);  
        
        ft.replace(R.id.settings_actionbar, mActionbarFragment);
        
        ft.replace(R.id.settings_main_pane, mMainFragment);
        
        ft.commit();        
    }
	
	TimeZonePickerUtils mTzPickerUtils;
	String mDeviceTimeZoneId;
	String mTimeZoneId;
	String mTimeZoneName;
	String[] mTzIds;
	int mMainFrameLayoutHeight;
	int mMainViewSurplusRegionHeight;
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		if (INFO) Log.i(TAG, "onCreate");
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  //���� ����
		
		calcFrameLayoutHeight();
		
		mController = CalendarController.getInstance(this);			
		
		setContentView(R.layout.settings_layout);		
		
		boolean enabledStatus = Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_HOME_TZ_ENABLED, false);
        String homeTimeZone = Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_HOME_TZ, ETime.getCurrentTimezone());
		
		getTimeZoneInfomation();
		
		mDeviceTimeZoneId = ETime.getCurrentTimezone();
		mTimeZoneId = Utils.getTimeZone(this, null);
		
		if (mTzPickerUtils == null) {
            mTzPickerUtils = new TimeZonePickerUtils(getApplicationContext());
        }
        CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(getApplicationContext(), mTimeZoneId,
                System.currentTimeMillis(), false);
        // timezoneName�� �ѱ� ǥ�ؽ�  GMT+9�� �����ȴ�..........................................................................................................
        mTimeZoneName = timezoneName.toString();
		
        mMainFragment = new SettingsFragment(mTimezoneData, mTzPickerUtils);
        
        mMainFrameLayoutHeight = SettingsFragment.calcFrameLayoutHeight(this);
        mMainViewSurplusRegionHeight = SettingsFragment.calcMainViewSurplusRegion(getResources(), mMainFrameLayoutHeight);
        
		prepareEntranceExitAnimation();		
				
		//getCityNameByOridnate();
		
	}
	
	@Override
	protected void onStart() {		
		super.onStart();
		
		if (INFO) Log.i(TAG, "onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();	
		
		if (INFO) Log.i(TAG, "onResume");
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
	   super.onWindowFocusChanged(hasFocus);
	   
	   if (INFO) Log.i(TAG, "onWindowFocusChanged");
	   
	   if (hasFocus) {
		   //mSettinsRealLayout.setVisibility(View.VISIBLE);
		   mSettinsRealLayout.startAnimation(mEnterTranslateAnim);
	   }
	   else {
		   // ���⼭ exit animation�� ������ ����
		   // :�ǹ� ���� ��� �䰡 ����� �� ȣ��ȴ�
	   }
	}
	
	@Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
    		if (INFO) Log.i(TAG, "onKeyDown : KEYCODE_MENU");
    		
    		mCallerActivityBitmapContainer.setVisibility(View.VISIBLE);
 		    mSettinsRealLayout.startAnimation(mExitTranslateAnim);
    		return true;
    	}
    	else if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (INFO) Log.i(TAG, "onKeyDown : KEYCODE_BACK");
    		
    		if (mMainFragment.mViewState == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
    		
	    		mCallerActivityBitmapContainer.setVisibility(View.VISIBLE);
	 		    mSettinsRealLayout.startAnimation(mExitTranslateAnim);	 		    
	    		return true;
    		}
    	}
    	
		return super.onKeyDown(keyCode, event);    	
    }
	
	
	@Override
	protected void onPause() {	
		overridePendingTransition(0, 0);
		super.onPause();		
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	TimeZoneData mTimezoneData;
	public void getTimeZoneInfomation() {
		
		//mTzIds = TimeZone.getAvailableIDs();
		
		long timeMillis = System.currentTimeMillis();
		String timeZone = Utils.getTimeZone(this, null);
		
		mTimezoneData = new TimeZoneData(getApplicationContext(), timeZone, timeMillis);		
		
	}
	
	
	public void getCityNameByOridnate() {		
	    		
		// Location.convert�� �����ϴ� ������ ���� �� ���� �����
		// FORMAT_DEGREES : DDD.DDDDD
		// FORMAT_MINUTES : DDD:MM.MMMMM
	    // FORMAT_SECONDS : DDD:MM:SS.SSSSS
		// �츮�� ������ �ִ� �Ʒ��� ��ǥ ������
		//      +-DDMMSS+-DDDMMSS �̴�
		// US	+340308-1181434	America/Los_Angeles	Pacific Time
		// �׷��Ƿ� FORMAT_SECONDS ���� convert �Լ��� ��Ʈ���� �����Ѵ�
		// �׷���
		// degree�� ����� ��� +��ȣ�� ���̸� ���ܰ� �߻��Ѵ�->+034
		// :�׷��� ������ ��� -��ȣ�� ���δ�
	    double latitude = Location.convert("34:03:08.00000");
    	double longitude = Location.convert("-118:14:34.00000");
    	
	    String cityName = null;
		Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
		List<Address> addresses;
		try {
			
		    addresses = gcd.getFromLocation(latitude, longitude, 1);   
		    
		    if (addresses.size() > 0) {
		    	cityName = addresses.get(0).getLocality();
		    	if (INFO) Log.i(TAG,"getCityNameByOridnate:City Name : " + cityName);
		    }
		    
		    
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
	}	
	
}
