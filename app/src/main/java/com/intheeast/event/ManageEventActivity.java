package com.intheeast.event;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.AbstractCalendarActivity;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.acalendar.R;
import com.intheeast.etc.ETime;
//import com.intheeast.event.EditEvent.ScrollInterpolator;
import com.intheeast.settings.SettingsFragment;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
//import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ManageEventActivity extends AbstractCalendarActivity {
	
	private static final String TAG = "ManageEventActivity";
	private static boolean INFO = true;
	
	public static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    public static final String EXTRA_EVENT_COLOR = "event_color";
    public static final String EXTRA_EVENT_REMINDERS = "reminders";
    
    private static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float ENTRY_ANIMATION_VELOCITY = MINIMUM_SNAP_VELOCITY * 3; 
	public static final float EXIT_ANIMATION_VELOCITY = MINIMUM_SNAP_VELOCITY * 4; 
	
	ECalendarApplication mApp;
    ImageView mCallerActivityBitmapContainer;	
    Bitmap mCallerEntireRegionBitmap;
    
    LinearLayout mEditEventRealLayout;
	FrameLayout mEditEventActionbarFrameLayout;
	FrameLayout mEditEventMainPaneFrameLayout;
    
    TranslateAnimation mEnterTranslateAnim;
	TranslateAnimation mExitTranslateAnim;	
    
	private EventInfo mEventInfo;
     
    boolean mIsReadOnly = false;
    EditEventFragment mEditFragment;
    EditEventActionBarFragment mActionbarFragment;
    
    int mWindowWidth;
    int mWindowHeight;
    int mMainFragmentHeight;
    
    public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
    
    
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
    	mWindowWidth = rectgle.right - rectgle.left;
    	
    	mMainFragmentHeight = (int) (mWindowHeight - getResources().getDimension(R.dimen.settings_view_upper_actionbar_height));
	}	
	
	boolean mDestroyBecauseOfSingleTaskRoot = false;
    // savedInstanceState : If the activity is being re-initialized after previously being shut down 
    // then this Bundle contains the data it most recently supplied in onSaveInstanceState.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		if (INFO) Log.i(TAG, "onCreate");
		
		mApp = (ECalendarApplication) getApplication();
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  //���� ����
		
		calcFrameLayoutHeight();
		
		setContentView(R.layout.edit_event_layout);			
		
		mEditFragment = new EditEventFragment(this, false);
		
		//mEventInfo = getEventInfoFromIntent(savedInstanceState);        
		       
		mDestroyBecauseOfSingleTaskRoot = mApp.getRootLaunchedAnotherActivityFlag();
        if (mDestroyBecauseOfSingleTaskRoot) {
        	mApp.reSetRootLaunchedAnotherActivityFlag();
        	
        	// CentralActivity.onResume���� mRootLaunchedAnotherActivity�� true �̱� ������
            // ManageEventActivity�� launc�� ���̶��,,,
        	// : �Ʒ� �� ���� ��Ȳ���� ManageEventActivity�� destroy�� ������ ������ �� �ִ�
        	//   1.create new event
        	//   2.edit event	
        	mEditFragment.setBundleFromAppBundle(mApp.getBundle());
        	
        	mEventInfo = new EventInfo();
        	mEventInfo.id = mEditFragment.mEventBundle.id;
        	mEventInfo.eventTitle = null;
        	mEventInfo.calendarId = -1;
            mEventInfo.extraLong = 0;    
            
        	String tzId = Utils.getTimeZone(this, null);
    		TimeZone timeZone = TimeZone.getTimeZone(tzId);
        	mEventInfo.startTime = GregorianCalendar.getInstance(timeZone);
        	mEventInfo.startTime.setTimeInMillis(mEditFragment.mEventBundle.start);       
        	
        	mEventInfo.endTime = GregorianCalendar.getInstance(timeZone);
        	mEventInfo.endTime.setTimeInMillis(mEditFragment.mEventBundle.end);        	               
        }
        else {
        	// � ���� EventInfo.startTime�� �����Ǿ�����
        	//   1.create new event
        	//   2.edit event	
        	mEventInfo = getEventInfoFromIntent(savedInstanceState);        
        }
                
        mEditFragment.makeEditEvent(mEventInfo, mWindowWidth);
        mEditFragment.makeSubPage();
        
        mActionbarFragment = new EditEventActionBarFragment(this, SettingsFragment.MAIN_VIEW_FRAGMENT_STATE, mEditFragment);        
                
        prepareEntranceExitAnimation();        
	}	
	
	
	@Override
	protected void onStart() {		
		super.onStart();
		
		if (INFO) Log.i(TAG, "onStart");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		
		if (INFO) Log.i(TAG, "onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (INFO) Log.i(TAG, "onResume");
		
		/*if (mPaused) {
			mPaused = false;
			mMustEntranceAnimation = false;
		}*/
		
	}
	
	boolean mMustEntranceAnimation = true;
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
	   super.onWindowFocusChanged(hasFocus);
	   
	   if (INFO) Log.i(TAG, "onWindowFocusChanged");
	   
	   if (mMustEntranceAnimation) {		   
		   if (hasFocus) {			   
			   mEditEventRealLayout.setVisibility(View.VISIBLE);
			   mEditEventRealLayout.startAnimation(mEnterTranslateAnim);
		   }
		   
	   }
	}
	
	@Override
    protected void onUserLeaveHint() {
    	if (INFO) Log.i(TAG, "onUserLeaveHint"); 
    	
        //mController.sendEvent(this, EventType.USER_HOME, null, null, -1, ViewType.CURRENT);
    	mApp.setRootLaunchedAnotherActivityFlag();
    	mApp.setWhichActivityLaunchedByRoot(1);
        super.onUserLeaveHint();
    }
	
	//boolean mPaused = false;
	@Override
	protected void onPause() {		
		super.onPause();
		if (INFO) Log.i(TAG, "onPause");
		
		//mPaused = true;
	}

	@Override
	protected void onStop() {		
		super.onStop();
		if (INFO) Log.i(TAG, "onStop");
		//mCallerEntireRegionBitmap.recycle();
	}

	@Override
	protected void onDestroy() {		
		super.onDestroy();
		if (INFO) Log.i(TAG, "onDestroy");
	}

	public void prepareEntranceExitAnimation() {		
		
		
		mCallerEntireRegionBitmap = mApp.getCalendarEntireRegionBitmap();	
		
		// ���� �ð��밡 ������ �Ѿ�� ���� ���� bitmap�̶��,,,
		// exit �� old bitmap���� ����...
		// ������[Ķ������ ��Ī�ϴ� ���� �ƴ϶� �۾�������]ó��... 
		mCallerActivityBitmapContainer = (ImageView) findViewById(R.id.caller_activity_bitmap_container);
		mCallerActivityBitmapContainer.setImageBitmap(mCallerEntireRegionBitmap);
		
		mEditEventRealLayout = (LinearLayout) findViewById(R.id.editevent_real_layout);		
		mEditEventActionbarFrameLayout = (FrameLayout) mEditEventRealLayout.findViewById(R.id.editevent_actionbar_framelayout);
		mEditEventMainPaneFrameLayout = (FrameLayout) mEditEventRealLayout.findViewById(R.id.editevent_main_pane_framelayout);
		  		
		mEditEventActionbarFrameLayout.addView(mActionbarFragment.mFrameLayout);
		
		mEditEventMainPaneFrameLayout.addView(mEditFragment.mEditEvent.mMainPageView);		
		
		if (mDestroyBecauseOfSingleTaskRoot) {		
			
			mEditFragment.setModelFromBundle();
			
			mEditEventActionbarFrameLayout.setVisibility(View.VISIBLE);
			mEditEventMainPaneFrameLayout.setVisibility(View.VISIBLE);
			mEditEventRealLayout.setVisibility(View.VISIBLE);//////////////////////////////////////////////////////////////////////////////////
			
			mCallerActivityBitmapContainer.setVisibility(View.GONE);
			
			mMustEntranceAnimation = false;
			
			initFragments();
		}
		else {
			mEditFragment.setModel();
			
			mEditEventActionbarFrameLayout.setVisibility(View.INVISIBLE);
			mEditEventMainPaneFrameLayout.setVisibility(View.INVISIBLE);
			mEditEventRealLayout.setVisibility(View.INVISIBLE);//////////////////////////////////////////////////////////////////////////////////
			
			makeEntranceAnimation();
		
		}
		
		
		makeExitAnimation();	
	}
		
	
	private void initFragments() {
		
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.replace(R.id.editevent_actionbar_framelayout, mActionbarFragment);
        
        ft.replace(R.id.editevent_main_pane_framelayout, mEditFragment);
        
        ft.commit();        
    }

	

	public EventInfo getEventInfoFromIntent(Bundle icicle) {
        EventInfo info = new EventInfo();
        long eventId = -1;
        Intent intent = getIntent();
        // getData : Retrieve data this intent is operating on. This URI specifies the name of the data
        Uri data = intent.getData(); //The URI of the data this intent is targeting or null.
        // data�� null �̶�� ���� ���ο� �̺�Ʈ�� �����Ϸ��� ���� �ǹ��Ѵ�
        if (data != null) {
            try {
                eventId = Long.parseLong(data.getLastPathSegment());
            } catch (NumberFormatException e) {
                
            }
        } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_EVENT_ID)) {        	
            eventId = icicle.getLong(BUNDLE_KEY_EVENT_ID);
        }

        boolean allDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);

        long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
        if (end != -1) {
            info.endTime = GregorianCalendar.getInstance();//new Time();
            if (allDay) {
                info.endTime = ETime.switchTimezone(info.endTime, TimeZone.getTimeZone(ETime.TIMEZONE_UTC));//info.endTime.timezone = Time.TIMEZONE_UTC;
            }
            info.endTime.setTimeInMillis(end);
        }
        if (begin != -1) {
            info.startTime = GregorianCalendar.getInstance();//new Time();
            if (allDay) {
            	info.startTime = ETime.switchTimezone(info.startTime, TimeZone.getTimeZone(ETime.TIMEZONE_UTC));//info.startTime.timezone = Time.TIMEZONE_UTC;
            }
            info.startTime.setTimeInMillis(begin);
        }
        
        info.id = eventId;//////////////////////////////////////////////////////
        info.eventTitle = intent.getStringExtra(Events.TITLE);
        info.calendarId = intent.getLongExtra(Events.CALENDAR_ID, -1);

        if (allDay) {
            info.extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
        } else {
            info.extraLong = 0;
        }       
        
        return info;
    }
	
	@SuppressWarnings("unchecked")
    public ArrayList<ReminderEntry> getReminderEntriesFromIntent() {
        Intent intent = getIntent();
        return (ArrayList<ReminderEntry>) intent.getSerializableExtra(EXTRA_EVENT_REMINDERS);
    }	
	
	public void goodByeActivity() {
		if (INFO) Log.i(TAG, "goodByeActivity");
		mCallerActivityBitmapContainer.setVisibility(View.VISIBLE);
		mEditEventRealLayout.startAnimation(mExitTranslateAnim);
	}
	
		
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
	
	AnimationListener mEntranceAnimlistener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			mEditEventRealLayout.setVisibility(View.VISIBLE);	
			mEditEventActionbarFrameLayout.setVisibility(View.VISIBLE);
			mEditEventMainPaneFrameLayout.setVisibility(View.VISIBLE);
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
			mEditEventRealLayout.setVisibility(View.GONE);
			finish();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
	
	
}
