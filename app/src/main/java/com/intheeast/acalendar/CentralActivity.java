package com.intheeast.acalendar;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
//import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnSuggestionListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.intheeast.agenda.AgendaFragment;
import com.intheeast.day.DayFragment;
import com.intheeast.day.DayOfMonthDrawable;
import com.intheeast.acalendar.CalendarController.EventHandler;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.FragmentTag;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.etc.ETime;
import com.intheeast.event.ManageEventActivity;
import com.intheeast.month.MonthFragment;
import com.intheeast.settings.SettingsFragment;
import com.intheeast.year.YearPickerFragment;

import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

public class CentralActivity extends AbstractCalendarActivity implements EventHandler,
        OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, ActionBar.TabListener,
        OnSuggestionListener {
	private static final String TAG = CentralActivity.class.getSimpleName();    
    private static final boolean INFO = true;
    
    private static final String EVENT_INFO_FRAGMENT_TAG = "EventInfoFragment";
    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    private static final String BUNDLE_KEY_RESTORE_VIEW = "key_restore_view";
    private static final String BUNDLE_KEY_CHECK_ACCOUNTS = "key_check_for_accounts";
    private static final int HANDLER_KEY = 0;

    // Indices of buttons for the drop down menu (tabs replacement)
    // Must match the strings in the array buttons_list in arrays.xml and the
    // OnNavigationListener
    private static final int BUTTON_DAY_INDEX = 0;
    private static final int BUTTON_WEEK_INDEX = 1;
    private static final int BUTTON_MONTH_INDEX = 2;
    private static final int BUTTON_AGENDA_INDEX = 3;

    ECalendarApplication mApp;
    
    RelativeLayout mContentView;
    
    private CalendarController mController;   
        
    private boolean mOnSaveInstanceStateCalled = false;
    private boolean mBackToPreviousView = false;
    private ContentResolver mContentResolver;
    private int mPreviousView;
    private int mCurrentView;
    private boolean mPaused = true;
    private boolean mUpdateOnResume = false;
    //private boolean mHideControls = false;
    
    private String mTzId;
    private TimeZone mTimeZone;
    int mFirstDayOfWeek;
    //private boolean mShowCalendarControls;
    //private boolean mShowEventInfoFullScreenAgenda;
    //private boolean mShowEventInfoFullScreen;
    //private boolean mUseEventInfoActivity = false;
    

    private long mViewEventId = -1;
    private boolean mDayFramgentLaunchByAlertActivity = false;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private int mIntentAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
    private boolean mIntentAllDay = false;

    
    
    private SearchView mSearchView;
    private MenuItem mSearchMenu;
       
    private QueryHandler mHandler;
    private boolean mCheckForAccounts = true;
    

    DayOfMonthDrawable mDayOfMonthIcon;

    int mOrientation;
    
    BothEndsEventLoader mForemostEventLoader;
    BothEndsEventLoader mBackmostEventLoader;
    
    Event mCurrentForemostEvent = new Event(); 
    Event mCurrentBackmostEvent = new Event();    
    
    Event mPrvForemostEvent = new Event(); 
    Event mPrvBackmostEvent = new Event();    
    
    int mBothEndsEventQueryStartDay;
    int mBothEndsEventQueryEndDay;
    
    
    private AllInOneMenuExtensionsInterface mExtensions = ExtensionsFactory
            .getAllInOneMenuExtensions();   
    
    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mCheckForAccounts = false;
            try {
                // If the query didn't return a cursor for some reason return
                if (cursor == null || cursor.getCount() > 0 || isFinishing()) {
                    return;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Bundle options = new Bundle();
            options.putCharSequence("introMessage",
                    getResources().getString(R.string.create_an_account_desc));
            options.putBoolean("allowSkip", true);

            AccountManager am = AccountManager.get(CentralActivity.this);
            am.addAccount("com.google", CalendarContract.AUTHORITY, null, options,
            		CentralActivity.this,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            if (future.isCancelled()) {
                                return;
                            }
                            try {
                                Bundle result = future.getResult();
                                boolean setupSkipped = result.getBoolean("setupSkipped");

                                if (setupSkipped) {
                                    Utils.setSharedPreference(CentralActivity.this,
                                            SettingsFragment.KEY_SKIP_SETUP, true);
                                }

                            } catch (OperationCanceledException ignore) {
                                // The account creation process was canceled
                            } catch (IOException ignore) {
                            } catch (AuthenticatorException ignore) {
                            }
                        }
                    }, null);
        }
    }

    private final Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mTzId = Utils.getTimeZone(CentralActivity.this, mHomeTimeUpdater);
            mTimeZone = TimeZone.getTimeZone(mTzId);
            //updateSecondaryTitleFields(-1);
            CentralActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTzId);
        }
    };

    // runs every midnight/time changes and refreshes the today icon
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
        	mTzId = Utils.getTimeZone(CentralActivity.this, mHomeTimeUpdater);
            mTimeZone = TimeZone.getTimeZone(mTzId);
            CentralActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTzId);
        }
    };


    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @SuppressLint("SuspiciousIndentation")
        @Override
        public void onChange(boolean selfChange) {
        	if (INFO) Log.i(TAG, "onChange");
            eventsChanged();
        }
    };

    BroadcastReceiver mCalIntentReceiver;

    @Override
    protected void onNewIntent(Intent intent) {
    	if (INFO) Log.i(TAG, "onNewIntent"); 
    	
        String action = intent.getAction();
        Bundle extraBundle = intent.getExtras();
        
        //if (INFO) Log.i(TAG, "New intent received " + intent.toString());
        // Don't change the date if we're just returning to the app's home
        if (Intent.ACTION_VIEW.equals(action)
                && !intent.getBooleanExtra(Utils.INTENT_KEY_HOME, false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                millis = Utils.timeFromIntentInMillis(intent);
            }
            
            if (millis != -1 && mViewEventId == -1 && mController != null) {
                Calendar time = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZone);
                time.setTimeInMillis(millis);
                
                mController.sendEvent(this, EventType.GO_TO, time, time, -1, ViewType.CURRENT);
            }
        }
        else {
        	if (action == null) {
        		
        	}
        }     
        
    }

    
    public void queryBothEndsEvent() {          
        
        mPrvForemostEvent.copyTo(mCurrentForemostEvent);
        mPrvBackmostEvent.copyTo(mCurrentBackmostEvent);
        
        mForemostEventLoader.loadBothEndsEventsInBackground(mBothEndsEventQueryStartDay, mBothEndsEventQueryEndDay, mCurrentForemostEvent, 
        		mForemostEventLoaderSuccessCallback, mForemostEventLoaderCancelCallback);
                
        mBackmostEventLoader.loadBothEndsEventsInBackground(mBothEndsEventQueryStartDay, mBothEndsEventQueryEndDay, mCurrentBackmostEvent, 
        		mBackmostEventLoaderSuccessCallback, mBackmostEventLoaderCancelCallback);
    }
    
    public RelativeLayout  getContentView() {
    	return mContentView;
    }
    
    Context mContext;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (INFO) Log.i(TAG, "onCreate");
        
        mContext = getApplicationContext();
        mApp = (ECalendarApplication) getApplication();
        
        Looper mainLooper = getMainLooper();
        Thread mainThread = mainLooper.getThread();
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        mTzId = Utils.getTimeZone(this, mHomeTimeUpdater);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        // for test
        //Utils.deleteDefaultCalendar(getApplicationContext());
        //+for test
        /*
        boolean doAlertActivityPopUp = Utils.getSharedPreference(
                getApplicationContext(), SettingsFragment.KEY_ALERTS_POPUP, false);
        if (!doAlertActivityPopUp)
        	Utils.setSharedPreference(getApplicationContext(), SettingsFragment.KEY_ALERTS_POPUP, true);
        
        doAlertActivityPopUp = Utils.getSharedPreference(
                getApplicationContext(), SettingsFragment.KEY_ALERTS_POPUP, false);
        */
        //-for test
                
        /* for development 
        {
         
        	Utils.setSharedPreference(
                    getApplicationContext(), SettingsFragment.KEY_IS_FIRST_RUNNING, true);
        	
        	Utils.deleteCalendar(getApplicationContext(), );
        }
        */
        
        /*
        int defaultCalendarId = Utils.getSharedPreference(
                getApplicationContext(), SettingsFragment.KEY_DEFAULT_CALENDAR_ID, -1);
        if (defaultCalendarId == -1) {        	        	
        	Time currentTime = new Time(mTimeZone);        	
        	currentTime.set(System.currentTimeMillis());
        	String curTime2445 = currentTime.format2445();
        	
        	
        	Cursor cursor = Utils.queryCalendarGroup(getApplicationContext());
        	cursor.moveToPosition(-1);        	
        	while (cursor.moveToNext()) {
        		int idColumn = cursor.getColumnIndexOrThrow(Calendars._ID);
        		int accountColumn = cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
        		int accountTypeColumn = cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);  
        		
        		int calendarId = cursor.getInt(idColumn);
                String account = cursor.getString(accountColumn);
                String accountType = cursor.getString(accountTypeColumn);
                String xxx = cursor.getString(Utils.CALENDAR_GROUP_PROJECTION_ACCOUNT_TYPE_OR_ACCOUNT_NAME_AS_ACCOUNT_UNIQUE_KEY);
        	}
        	
        	cursor.close();
        }
        */
        
        
        Calendar startDayTime = GregorianCalendar.getInstance(mTimeZone);
        //long gmtoff = (TimeZone.getTimeZone(mTimeZone).getRawOffset()) / 1000;
        startDayTime.set(1900, 0, 1, 0, 0, 0);
        mBothEndsEventQueryStartDay = ETime.getJulianDay(startDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        Calendar endDayTime = GregorianCalendar.getInstance(mTimeZone);
        endDayTime.set(2100, 0, 1, 0, 0, 0);
        //endDayTime.normalize(true);
        mBothEndsEventQueryEndDay = ETime.getJulianDay(endDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        
        mForemostEventLoader = new BothEndsEventLoader(this, BothEndsEventLoader.FOREMOST_EVENT_LOADER);
        mBackmostEventLoader = new BothEndsEventLoader(this, BothEndsEventLoader.BACKMOST_EVENT_LOADER);
        
        mForemostEventLoader.startBackgroundThread();
        mBackmostEventLoader.startBackgroundThread();        
        
        if (icicle != null && icicle.containsKey(BUNDLE_KEY_CHECK_ACCOUNTS)) {
            mCheckForAccounts = icicle.getBoolean(BUNDLE_KEY_CHECK_ACCOUNTS);
        }
        // Launch add google account if this is first time and there are no
        // accounts yet
        if (mCheckForAccounts
                && !Utils.getSharedPreference(this, SettingsFragment.KEY_SKIP_SETUP, false)) {

            mHandler = new QueryHandler(this.getContentResolver());
            mHandler.startQuery(0, null, Calendars.CONTENT_URI, new String[] {
                Calendars._ID
            }, null, null /* selection args */, null /* sort order */);
        }

        // This needs to be created before setContentView        
        mController = CalendarController.getInstance(this);       
        
        // Get time from intent or icicle
        long timeMillis = -1;
        
        int viewType = -1;
        //viewType = ViewType.DAY; // for debug
        
        final Intent intent = getIntent();
        if (icicle != null) {
            timeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            viewType = icicle.getInt(BUNDLE_KEY_RESTORE_VIEW, -1);
        } else {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                // Open EventInfo later
                timeMillis = parseViewAction(intent);
            }

            if (timeMillis == -1) {
            	// intent에 EXTRA_EVENT_BEGIN_TIME의 값이 없다면,
            	// 현재 시간으로 설정된다
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }

        if (viewType == -1 || viewType > ViewType.MAX_VALUE) {
            viewType = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        
        viewType = ViewType.MONTH; // for test!!!
        
        Calendar t = GregorianCalendar.getInstance(mTimeZone);
        t.setTimeInMillis(timeMillis);

        /*
        if (INFO) {
            if (icicle != null && intent != null) {
                Log.i(TAG, "both, icicle:" + icicle.toString() + "  intent:" + intent.toString());
            } else {
                Log.i(TAG, "not both, icicle:" + icicle + " intent:" + intent);
            }
        }
		*/
        
        Resources res = getResources();
        
        mOrientation = res.getConfiguration().orientation;
               
        //STATUS_BAR_HEIGHT
        int statusBarHeight = Utils.getSharedPreference(this, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
        if (statusBarHeight == -1) {
        	Utils.setSharedPreference(this, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
        
        // setContentView must be called before configureActionBar
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);    	
        mContentView = (RelativeLayout) inflater.inflate(R.layout.all_in_one, null, false);
        setContentView(mContentView);
        
        //setContentView(R.layout.all_in_one);         
        
        // all_in_one.xml에 VISIBLE이 GONE으로 설정되어 있으므로 사용여부가 확실치 않다
        // 
        /*mHomeTime = (TextView) findViewById(R.id.home_time);
        mMiniMonth = findViewById(R.id.mini_month);
        if (mIsTabletConfig && mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mMiniMonth.setLayoutParams(new RelativeLayout.LayoutParams(mControlsAnimateWidth,
                    mControlsAnimateHeight));
        }
        mCalendarsList = findViewById(R.id.calendar_list);
        mMiniMonthContainer = findViewById(R.id.mini_month_container);
        mSecondaryPane = findViewById(R.id.secondary_pane);*/

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);
        
        configureActionBar(viewType, timeMillis);
        
        initFragments(timeMillis, viewType, icicle);

        // Listen for changes that would require this to be refreshed
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mContentResolver = getContentResolver();
        
        queryBothEndsEvent();
        
    }
    
    
    
    
    
    @Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		
		if (INFO) Log.i(TAG, "onRestart");
	}


	@Override
	protected void onStop() {
		if (INFO) Log.i(TAG, "onStop");
		super.onStop();
	}


	public void issueBothEndsEventQuery() {
    	
    }

	private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.valueOf(data.getLastPathSegment());
                    if (mViewEventId != -1) {
                    	mDayFramgentLaunchByAlertActivity = intent.getBooleanExtra(Utils.EXTRA_EVENT_VIEW_DISPLAY_FOR_ALERT, false);
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        mIntentAttendeeResponse = intent.getIntExtra(
                            ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);
                        mIntentAllDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);
                        timeMillis = mIntentEventStartMillis;
                    }
                } catch (NumberFormatException e) {
                    // Ignore if mViewEventId can't be parsed
                }
            }
        }
        return timeMillis;
    }      
    
    
    public void setDayViewActionBar() {    	
    }
    
    CalendarViewsUpperActionBarFragment mUpperActionBarFrag = null;
    
    EventInfoViewUpperActionBarFragment mEventInfoUpperActionBarFrag = null;
    private void configureActionBar(int viewType, long milliTime ) {
    	
    	mUpperActionBarFrag = new CalendarViewsUpperActionBarFragment(mContext);    
    	
    	mController.registerEventHandler(R.id.calendar_upper_actionbar, (EventHandler) mUpperActionBarFrag);
    }
    
    private int getActionBarHeight() {
    	int ActionBarHeight = 0;    	
    	TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
        	ActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        
        return ActionBarHeight;
    }    
       
    
    @Override
	protected void onStart() {        
		super.onStart();
		if (INFO) Log.i(TAG, "onStart");   
	}

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();

        if (INFO) Log.i(TAG, "onResume"); 
        
        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);
        mController.registerEventHandler(R.id.calendar_upper_actionbar, (EventHandler) mUpperActionBarFrag);
        
        mOnSaveInstanceStateCalled = false;
        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);        
        
        if (mUpdateOnResume) {
        	if (INFO) Log.i(TAG, "onResume:call initFragments"); 
            initFragments(mController.getTime(), mController.getViewType(), null);
            mUpdateOnResume = false;
        }
        
        Calendar t = GregorianCalendar.getInstance(mTimeZone);
        t.setTimeInMillis(mController.getTime());
        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, -1, ViewType.CURRENT,
                mController.getDateFlags(), null, null);
        
        
        mPaused = false;

        // 우리는 단독으로 event info view를 day fragment에서 보여줄 것이기 때문에 아래 코드는 필요없다
        // :주석 처리하지 않으면 런타임 에러가 발생함
        /*
        if (mViewEventId != -1 && mIntentEventStartMillis != -1 && mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > mIntentEventStartMillis && currentMillis < mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            mController.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, mViewEventId,
                    mIntentEventStartMillis, mIntentEventEndMillis, -1, -1,
                    EventInfo.buildViewExtraLong(mIntentAttendeeResponse,mIntentAllDay),
                    selectedTime);
            mViewEventId = -1;
            mIntentEventStartMillis = -1;
            mIntentEventEndMillis = -1;
            mIntentAllDay = false;
        }
        */
        Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTzId);
        
        mCalIntentReceiver = Utils.setTimeChangesReceiver(this, mTimeChangesUpdater);        
        
        if (mApp.getRootLaunchedAnotherActivityFlag()) {
        	
        	switch(mApp.getWhichActivityLaunchedByRoot()) {
        	case 1:
        		Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(mContext, ManageEventActivity.class);
                startActivity(intent);     
        	}
        }
    	
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (INFO) Log.i(TAG, "onPause"); 

        mController.deregisterEventHandler(HANDLER_KEY);
        
        mPaused = true;
        
        //mHomeTime.removeCallbacks(mHomeTimeUpdater);
                
        mContentResolver.unregisterContentObserver(mObserver);
        if (isFinishing()) {
            // Stop listening for changes that would require this to be refreshed
            SharedPreferences prefs = SettingsFragment.getSharedPreferences(this);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        // FRAG_TODO save highlighted days of the week;
        if (mController.getViewType() != ViewType.EDIT) {
            Utils.setDefaultView(this, mController.getViewType());
        }
        
        Utils.resetMidnightUpdater(mHandler, mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, mCalIntentReceiver);
    }

    // Called as part of the activity lifecycle when an activity is about to go into the background as the result of user choice
    // 1.user home button click
    // 2.상태바를 드롭다운해서 설정을 선택하였을 경우
    // 3.이클립스에서 타 app을 launch 시켜서 background로 진입할 때
    // **타이머를 설정하고 타이머가 expire되어서 타이머 알람창이 떠도 호출되지 않는다
    // ** incoming call 상황에서는 호출되지 않는다
    // ** alarm app이 foreground로 진입한 경우
    @Override
    protected void onUserLeaveHint() {
    	if (INFO) Log.i(TAG, "onUserLeaveHint"); 
    	
        mController.sendEvent(this, EventType.USER_HOME, null, null, -1, ViewType.CURRENT);
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putInt(BUNDLE_KEY_RESTORE_VIEW, mCurrentView);
        if (mCurrentView == ViewType.EDIT) {
            outState.putLong(BUNDLE_KEY_EVENT_ID, mController.getEventId());
        } else if (mCurrentView == ViewType.AGENDA) {
            FragmentManager fm = getFragmentManager();
            Fragment f = fm.findFragmentById(R.id.main_pane);
            if (f instanceof AgendaFragment) {
                outState.putLong(BUNDLE_KEY_EVENT_ID, ((AgendaFragment)f).getLastShowEventId());
            }
        }
        outState.putBoolean(BUNDLE_KEY_CHECK_ACCOUNTS, mCheckForAccounts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (INFO) Log.i(TAG, "onDestroy");
        
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        mController.deregisterAllEventHandlers();

        CalendarController.removeInstance(this);
    }

    private void initFragments(long timeMillis, int viewType, Bundle icicle) {
        if (INFO) Log.i(TAG, "initFragments to " + timeMillis + " for view " + viewType);
        
        FragmentTransaction ft = getFragmentManager().beginTransaction();
                
        EventInfo info = null;
        if (viewType == ViewType.EDIT) {
            mPreviousView = SettingsFragment.getSharedPreferences(this).getInt(
                    SettingsFragment.KEY_START_VIEW, SettingsFragment.DEFAULT_START_VIEW);

            long eventId = -1;
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data != null) {
                try {
                    eventId = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    if (INFO) Log.i(TAG, "Create new event");                    
                }
            } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_EVENT_ID)) {
                eventId = icicle.getLong(BUNDLE_KEY_EVENT_ID);
            }

            long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
            long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
            info = new EventInfo();
            if (end != -1) {
                info.endTime = GregorianCalendar.getInstance(mTimeZone);
                info.endTime.setTimeInMillis(end);
            }
            if (begin != -1) {
                info.startTime = GregorianCalendar.getInstance(mTimeZone);
                info.startTime.setTimeInMillis(begin);
            }
            info.id = eventId;
            // We set the viewtype so if the user presses back when they are
            // done editing the controller knows we were in the Edit Event
            // screen. Likewise for eventId
            mController.setViewType(viewType);
            mController.setEventId(eventId);
        } 
        /*else if(viewType == ViewType.DAY_EVENT_INFO) {
        	mPreviousView = ViewType.DAY;
        }*/
        else {
            mPreviousView = viewType;
        }
        
        mUpperActionBarFrag.refresh(this);
        
        /*if(viewType == ViewType.DAY_EVENT_INFO) {
        	mUpperActionBarFrag.setMainView(ViewType.DAY);
        }
        else*/
        	mUpperActionBarFrag.setMainView(viewType);
        	
    	mUpperActionBarFrag.setTime(timeMillis);
    	
    	ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    	ft.replace(R.id.calendar_upper_actionbar, mUpperActionBarFrag);
        
    	ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    	//ft.replace(R.id.lower_actionbar, mLowerActionBarFrag);  	
        
        setMainPane(ft, R.id.main_pane, viewType, timeMillis, true);
        ft.commit(); // this needs to be after setMainPane()

        Calendar t = GregorianCalendar.getInstance(mTimeZone);
        t.setTimeInMillis(timeMillis);
        
        // ViewType.DAY_EVENT_INFO를 새로 정의하지 말고
        // DayFragment에서 필터링하도록 하자
        /////////////////////////////////////////
        /*if(viewType == ViewType.DAY_EVENT_INFO) {
        	viewType = ViewType.DAY;
        }*/
        /////////////////////////////////////////
        
        if (viewType == ViewType.AGENDA && icicle != null) {
            mController.sendEvent(this, EventType.GO_TO, t, null,
                    icicle.getLong(BUNDLE_KEY_EVENT_ID, -1), viewType);
        } else if (viewType != ViewType.EDIT) {
        	// CalendarController.mViewType/mDetailViewType/mPreviousViewType가 최초 처음으로 설정되는 문맥
            mController.sendEvent(this, EventType.GO_TO, t, null, -1, viewType);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentView == ViewType.EDIT || mBackToPreviousView) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, mPreviousView);
        } else {
            super.onBackPressed();
        }
    }

    public void createLowerActionBar() {
    	
    }
    
    Bitmap mContentViewToBitmap;
	public void makeContentViewBitmap() {
		mContentView.destroyDrawingCache();
		
		mContentView.setDrawingCacheEnabled(true);

		mContentView.buildDrawingCache();

		mContentViewToBitmap = mContentView.getDrawingCache();
	}
	
    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
    		if (INFO) Log.i(TAG, "onKeyDown : KEYCODE_MENU");
    		
    		/*
    		makeContentViewBitmap();
    		ECalendarApplication app = (ECalendarApplication) getApplication();
			app.storeCalendarEntireRegionBitmap(mContentViewToBitmap);
			*/
			Utils.makeContentViewBitmap(this, mContentView);
			mController.sendEvent(this, EventType.SETTINGS, null, null, -1, ViewType.CURRENT, 0, null,
					getComponentName());	
    	}
    	
		return super.onKeyDown(keyCode, event);    	
    }
    
    
    
    
    DayFragment mDayFrag;
    MonthFragment mMonthFrag;
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	if (INFO) Log.i(TAG, "onSharedPreferenceChanged"); 
    	
        if (key.equals(SettingsFragment.KEY_WEEK_START_DAY)) {
            if (mPaused) {
                mUpdateOnResume = true;
            } else {
                initFragments(mController.getTime(), mController.getViewType(), null);
            }
        }
    }
        
    private void setMainPane(
            FragmentTransaction ft, int viewId, int viewType, long timeMillis, boolean force) {
    	
        if (mOnSaveInstanceStateCalled) {
            return;
        }
        if (!force && mCurrentView == viewType) {
            return;
        }

        // Remove this when transition to and from month view looks fine.
        //boolean doTransition = viewType != ViewType.MONTH && mCurrentView != ViewType.MONTH;
        FragmentManager fragmentManager = getFragmentManager();
        // Check if our previous view was an Agenda view
        // TODO remove this if framework ever supports nested fragments
        if (mCurrentView == ViewType.AGENDA) {
            // If it was, we need to do some cleanup on it to prevent the
            // edit/delete buttons from coming back on a rotation.
            Fragment oldFrag = fragmentManager.findFragmentById(viewId);
            if (oldFrag instanceof AgendaFragment) {
                ((AgendaFragment) oldFrag).removeFragments(fragmentManager);
            }
        }

        boolean doTransition = false;
        if (viewType != mCurrentView) {
            // The rules for this previous view are different than the
            // controller's and are used for intercepting the back button.
        	if (mCurrentView == ViewType.DAY) {
        		if (viewType == ViewType.AGENDA)
        			doTransition = true;
        	}
        	else if (mCurrentView == ViewType.AGENDA) {
        		if (viewType == ViewType.DAY)
        			doTransition = true;
        	}
        	        	
        	/*
        	if (viewType == ViewType.AGENDA) {
        		if (mCurrentView == ViewType.DAY)
        			doTransition = true;
        	}
        	else if (viewType == ViewType.DAY) {
        		if (mCurrentView == ViewType.AGENDA)
        			doTransition = true;
        	}
        	*/
        	
            if (mCurrentView != ViewType.EDIT && mCurrentView > 0) {
                mPreviousView = mCurrentView;
            }
            
            mCurrentView = viewType;
        }
        
        // Create new fragment
        Fragment frag = null;
        Fragment secFrag = null;
        String tag = null;
        switch (viewType) {
        
            case ViewType.AGENDA:
                
                frag = new AgendaFragment(timeMillis, mUpperActionBarFrag);
                ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("agenda");
                break;
            case ViewType.DAY:
            	
                //frag = new DayFragment(timeMillis, 1);
            	tag = FragmentTag.DAY_TAG;
                if (!mDayFramgentLaunchByAlertActivity)
                	frag = new DayFragment(mContext, timeMillis, 1, mUpperActionBarFrag);
                else {
                	frag = new DayFragment(mContext, timeMillis, 1, mUpperActionBarFrag, mViewEventId);
                	//doTransition = true;
                }
                mDayFrag = (DayFragment) frag;
                
                //ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("day");
                break;
            
            case ViewType.MONTH:        
            	
            	frag = new MonthFragment(mContext, timeMillis, false, mUpperActionBarFrag);
            	mMonthFrag = (MonthFragment)frag;
            	
                break;
            case ViewType.YEAR:
            	frag = new YearPickerFragment(getApplicationContext(), timeMillis, false, mUpperActionBarFrag);
            	YearPickerFragment yearFrag = (YearPickerFragment) frag;
            	
            	break;
            case ViewType.WEEK:
            default:
                
                break;
        }        
        
        // Clear unnecessary buttons from the option menu when switching from the agenda view
        /*
        if (viewType != ViewType.AGENDA) {
            clearOptionsMenu();
        }
        */

        boolean doCommit = false;
        if (ft == null) {
            doCommit = true;
            ft = fragmentManager.beginTransaction();
        }

        /*
        if (doTransition) {        	
        	ft.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out);        	
        }
        */
        
        ft.replace(viewId, frag, tag);        
        
        if (INFO) Log.i(TAG, "Adding handler with viewId " + viewId + " and type " + viewType);
                
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // If the key is already registered this will replace it////////////////////////////////////////////////////////////////
        // 이전 fragment의 handler는 어떻게 deregisterEventHandler가 하고 궁금했는데...
        // 동일한 key 값 R.id.main_pane을 사용하여 eventHandlers에 put 함으로써 이전 fragment의 handler는 삭제된다
        // :그러나 곧바로 삭제되지 않는다...만약 setMainPane이 handleEvent의 EventType.GO_TO 처리문에 호출된 것이라면,
        //  CalendarController의 mDispatchInProgressCounter가 최소 0[1이상의 값]보다 크기 때문에
        //  mToBeAddedEventHandlers에 먼저 캐싱되었다가 mDispatchInProgressCounter가 0이 되면 
        //  eventHandlers에 등록된다        
        mController.registerEventHandler(viewId, (EventHandler) frag);//////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        if (secFrag != null) {
            mController.registerEventHandler(viewId, (EventHandler) secFrag);
        }

        if (doCommit) {
            if (INFO) Log.i(TAG, "setMainPane AllInOne=" + this + " finishing:" + this.isFinishing());
            
            ft.commit();
        }
        
        
        
    }
    
    long mDayFragSelectedTime = 0;
    @SuppressLint("ResourceType")
    private void recoveryMainPane(int viewType) {
        if (mOnSaveInstanceStateCalled) {
            return;
        }        
        
        FragmentManager fragmentManager = getFragmentManager();        
        FragmentTransaction ft = fragmentManager.beginTransaction();   
        Fragment frag = null;
        String tag = null;
        switch (viewType) {
        
            case ViewType.AGENDA:
                
                break;
            case ViewType.DAY:       
            	mUpperActionBarFrag = new CalendarViewsUpperActionBarFragment(mContext); 
                mUpperActionBarFrag.refresh(this);
                mUpperActionBarFrag.setMainView(ViewType.DAY);
                mUpperActionBarFrag.setTime(mDayFragSelectedTime);
                
            	tag = FragmentTag.DAY_TAG;
            	mDayFrag = new DayFragment(mContext, mDayFragSelectedTime, 1, mUpperActionBarFrag);
                
                //mDayFrag.alternativeFragmentNonDefaultConstructor(mDayFragSelectedTime, 1, mUpperActionBarFrag);          
                
                break;
            case ViewType.MONTH:
                
                break;
            case ViewType.WEEK:
            default:
               
                break;
        }        
        
        FragmentTransaction ftActionBar = fragmentManager.beginTransaction();       
        ftActionBar.replace(R.id.calendar_upper_actionbar, mUpperActionBarFrag);                     	
        ftActionBar.commit(); 
        
    	Fragment prvF = fragmentManager.findFragmentById(R.id.main_pane);
    	ft.setCustomAnimations(R.anim.fragment_slide_right_incoming, R.anim.fragment_slide_right_outgoing);  
    	//ft.hide(prvF);
    	//ft.add(R.id.main_pane, frag);     
    	ft.replace(R.id.main_pane, frag, tag);
                
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // If the key is already registered this will replace it////////////////////////////////////////////////////////////////
        mController.registerEventHandler(R.id.main_pane, (EventHandler) frag);//////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////      
    	//
    	//mController.deregisterEventHandler(); // 언제 호출해야 하는가???
        ft.commit();
        
    }

    // 예를 들면,,,
    // DayView::switchViews(boolean forward, float xOffSet, float width, float velocity)
    // ->DayView::updateTitle
    //   ->EventType.UPDATE_TITLE
    //     ->handleEvent
    private void setTitleInActionBar(EventInfo event) {
    	/*
        if (event.eventType != EventType.UPDATE_TITLE || mActionBar == null) {
            return;
        }

        final long start = event.startTime.toMillis(false);
        final long end;
        if (event.endTime != null) {
            end = event.endTime.toMillis(false);
        } else {
            end = start;
        }
        */

        /*final String msg = Utils.formatDateRange(this, start, end, (int) event.extraLong);
        CharSequence oldDate = mDateRange.getText();
        
        mDateRange.setText(msg);
        
        updateSecondaryTitleFields(event.selectedTime != null ? event.selectedTime.toMillis(true)
                : start);
        
        if (!TextUtils.equals(oldDate, msg)) {
            mDateRange.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            if (mShowWeekNum && mWeekTextView != null) {
                mWeekTextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }*/
    }

    /*
    private void updateSecondaryTitleFields(long visibleMillisSinceEpoch) {
        mShowWeekNum = Utils.getShowWeekNumber(this);
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        if (visibleMillisSinceEpoch != -1) {
            int weekNum = Utils.getWeekNumberFromTime(visibleMillisSinceEpoch, this);
            mWeekNum = weekNum;
        }

        if (mShowWeekNum && (mCurrentView == ViewType.WEEK) && mIsTabletConfig
                && mWeekTextView != null) {
            String weekString = getResources().getQuantityString(R.plurals.weekN, mWeekNum,
                    mWeekNum);
            mWeekTextView.setText(weekString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (visibleMillisSinceEpoch != -1 && mWeekTextView != null
                && mCurrentView == ViewType.DAY && mIsTabletConfig) {
            Time time = new Time(mTimeZone);
            time.set(visibleMillisSinceEpoch);
            int julianDay = Time.getJulianDay(visibleMillisSinceEpoch, time.gmtoff);
            time.setToNow();
            int todayJulianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
            String dayString = Utils.getDayOfWeekString(julianDay, todayJulianDay,
                    visibleMillisSinceEpoch, this);
            mWeekTextView.setText(dayString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (mWeekTextView != null && (!mIsTabletConfig || mCurrentView != ViewType.DAY)) {
            mWeekTextView.setVisibility(View.GONE);
        }

        if (mHomeTime != null
                && (mCurrentView == ViewType.DAY || mCurrentView == ViewType.WEEK
                        || mCurrentView == ViewType.AGENDA)
                && !TextUtils.equals(mTimeZone, Time.getCurrentTimezone())) {
            Time time = new Time(mTimeZone);
            time.setToNow();
            long millis = time.toMillis(true);
            boolean isDST = time.isDst != 0;
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            // Formats the time as
            String timeString = (new StringBuilder(
                    Utils.formatDateRange(this, millis, millis, flags))).append(" ").append(
                    TimeZone.getTimeZone(mTimeZone).getDisplayName(
                            isDST, TimeZone.SHORT, Locale.getDefault())).toString();
            mHomeTime.setText(timeString);
            mHomeTime.setVisibility(View.VISIBLE);
            // Update when the minute changes
            mHomeTime.removeCallbacks(mHomeTimeUpdater);
            mHomeTime.postDelayed(
                    mHomeTimeUpdater,
                    DateUtils.MINUTE_IN_MILLIS - (millis % DateUtils.MINUTE_IN_MILLIS));
        } else if (mHomeTime != null) {
            mHomeTime.setVisibility(View.GONE);
        }
    }
	*/
        
    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.VIEW_EVENT | EventType.UPDATE_TITLE | 
        		EventType.EXPAND_EVENT_VIEW_OK | EventType.EXPAND_EVENT_VIEW_NO_OK | 
        		EventType.COLLAPSE_EVENT_VIEW_OK | EventType.COLLAPSE_EVENT_VIEW_NO_OK;
    }

    public static final String EXTRA_CALLER_VIEW = "ecalendar_caller_view";
    @Override
    public void handleEvent(EventInfo event) {
        
        if (event.eventType == EventType.GO_TO) {   
        	if (INFO) Log.i(TAG, "handleEvent:EventType.GO_TO");
        	
            if ((event.extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS) != 0) {
                mBackToPreviousView = true;
                if (INFO) Log.i("tag","EventType.GO_TO, EXTRA_GOTO_BACK_TO_PREVIOUS : time=" + String.valueOf(System.currentTimeMillis()));
            } else if (event.viewType != mController.getPreviousViewType()
                    && event.viewType != ViewType.EDIT) {
                // Clear the flag is change to a different view type
                mBackToPreviousView = false; 
            }
            
            // EXTRA_GOTO_BACK_TO_PREVIOUS_FORCE는 EventInfoFragment의 doBack에서 설정되지만
            // 현재 EventInfoFragment를 사용하지 않고 있다
            if ((event.extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS_FORCE) != 0) {
            	recoveryMainPane(event.viewType);
            	return;
            }                           
            
            setMainPane(
            	null, R.id.main_pane, event.viewType, event.startTime.getTimeInMillis(), false);
                        
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
                        
            
        	if (mUpperActionBarFrag != null) {
        		mUpperActionBarFrag.setMainView(event.viewType);
        		mUpperActionBarFrag.setTime(mController.getTime());
        	}           
            
        } 
        //이제 Event Info View는 더 이상 단독 Activity로 수행되지 않기 때문에
        //아래 조건문에서 과거 Event Info View 관련 부분을 삭제하였다
        else if (event.eventType == EventType.VIEW_EVENT) {

            // If in Agenda view and "show_event_details_with_agenda" is "true",
            // do not create the event info fragment here, it will be created by the Agenda
            // fragment

            if (mCurrentView == ViewType.AGENDA) {
                if (event.startTime != null && event.endTime != null) {
                    // Event is all day , adjust the goto time to local time
                    if (event.isAllDay()) {
                        Utils.convertAlldayUtcToLocal(
                                event.startTime, event.startTime.getTimeInMillis(), mTzId);
                        Utils.convertAlldayUtcToLocal(
                                event.endTime, event.endTime.getTimeInMillis(), mTzId);
                    }
                    mController.sendEvent(this, EventType.GO_TO, event.startTime, event.endTime,
                            event.selectedTime, event.id, ViewType.AGENDA,
                            CalendarController.EXTRA_GOTO_TIME, null, null);
                } else if (event.selectedTime != null) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                        event.selectedTime, event.id, ViewType.AGENDA);
                }
            } else {
                // TODO Fix the temp hack below: && mCurrentView !=
                // ViewType.AGENDA
                if (event.selectedTime != null && mCurrentView != ViewType.AGENDA) {
                	
                	//Log.i("tag", "CentralActivity:handleEvent:before sendEvent GO_TO!!!");
                	
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                            event.selectedTime, -1, ViewType.CURRENT);
                } // 왜? GO_TO 메시지를 보내는가???
                
            }            
            //displayTime = event.startTime.toMillis(true);
            
        } else if (event.eventType == EventType.UPDATE_TITLE) {            
        	if (mUpperActionBarFrag != null)
        		mUpperActionBarFrag.setTime(mController.getTime());
            
        } else if (event.eventType == EventType.EXPAND_EVENT_VIEW_OK | event.eventType == EventType.EXPAND_EVENT_VIEW_NO_OK) {
        	if (mUpperActionBarFrag != null)
        		mUpperActionBarFrag.responseExpandEventView(event.eventType);
        } else if (event.eventType == EventType.COLLAPSE_EVENT_VIEW_OK | event.eventType == EventType.COLLAPSE_EVENT_VIEW_NO_OK) {
        	if (mUpperActionBarFrag != null)
        		mUpperActionBarFrag.responseCollapseEventView(event.eventType);
        }
        
        //updateSecondaryTitleFields(displayTime); // 우리에게는 쓸모가 없다
    }

    // Needs to be in proguard whitelist
    // Specified as listener via android:onClick in a layout xml
    public void handleSelectSyncedCalendarsClicked(View v) {
        mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, null, 0, 0,
                CalendarController.EXTRA_GOTO_TIME, null,
                null);
    }

    boolean mWaitingProcessEventsChanged = false;
    @Override
    public void eventsChanged() {
    	// 여기서 다시 한번 both ends event를 쿼리하자
    	queryBothEndsEvent();
    	
    	// 가장 골치 아픈 시나리오는 fragment 전환시 발생된다
    	// 각 fragment들이 캐싱한 이벤트들의 재사용을 위해 
    	// 종료되는 fragment가 캐싱된 이벤트들을 App에 전달한다
    	// fragment 전환시 누구에게 EVENTS_CHANGED가 전달될까?
    	// fragment 전환시라면,
    	// 잠시 EVENTS_CHANGED를 어디에 캐싱해 뒀다가
    	// new fragment에게 notify 하는 방법은 없는가?
    	if (mCurrentView == ViewType.DAY) {
    		if (mDayFrag.mGoToExitAnim) {
    			setWaitingProcessEventsChangedFlag(true);
    			return;
    		}
    	}
    	else if (mCurrentView == ViewType.MONTH) {    		
    		if (mMonthFrag.mGoToExitAnim) {
    			setWaitingProcessEventsChangedFlag(true);
    			return;
    		}
    			
    	}
    	
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }
    
    public synchronized boolean getWaitingProcessEventsChangedFlag() {
    	return mWaitingProcessEventsChanged;
    }
    
    public synchronized void setWaitingProcessEventsChangedFlag(boolean flagStaus) {
    	mWaitingProcessEventsChanged = flagStaus;
    }
    
    /*
    public synchronized void resetWaitingProcessEventsChangedFlag() {
    	mWaitingProcessEventsChanged = false;
    }
	*/
    
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchMenu.collapseActionView();
        mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, query,
                getComponentName());
        return true;
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
    	/*
        Log.w(TAG, "TabSelected AllInOne=" + this + " finishing:" + this.isFinishing());
        if (tab == mDayTab && mCurrentView != ViewType.DAY) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
        } else if (tab == mWeekTab && mCurrentView != ViewType.WEEK) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
        } else if (tab == mMonthTab && mCurrentView != ViewType.MONTH) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
        } else if (tab == mAgendaTab && mCurrentView != ViewType.AGENDA) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
        } else {
            Log.w(TAG, "TabSelected event from unknown tab: "
                    + (tab == null ? "null" : tab.getText()));
            Log.w(TAG, "CurrentView:" + mCurrentView + " Tab:" + tab.toString() + " Day:" + mDayTab
                    + " Week:" + mWeekTab + " Month:" + mMonthTab + " Agenda:" + mAgendaTab);
        }
        */
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }


    

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        mSearchMenu.collapseActionView();
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        if (mSearchMenu != null) {
            mSearchMenu.expandActionView();
        }
        return false;
    }

    // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    // 위와 같이 설정된 경우에는 아래 함수는 동작하지 않는다는 점 유의!!!
    public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}

    final Runnable mForemostEventLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	if (INFO) Log.i(TAG, "***********mForemostEventLoaderSuccessCallback**************");
        	
        	if ( (mPrvForemostEvent.id == mCurrentForemostEvent.id) && (mPrvForemostEvent.startDay == mCurrentForemostEvent.startDay) ){
        		return;
        	}        	
        	
        	//printBothEndsEventContext(BothEndsEventLoader.FOREMOST_EVENT_LOADER);
        	
        	CentralActivity.this.mApp.setBothEndsEvent(BothEndsEventLoader.FOREMOST_EVENT_LOADER, mCurrentForemostEvent);
        	mController.sendEvent(this, EventType.BOTHENDS_EVENTS_QUERY_COMPLETION, null, null, null, -1,
            		ViewType.CURRENT, CalendarController.EXTRA_BOTHENDS_FOREMOST_EVENT, 
            		null, null);
        	if (INFO) Log.i(TAG, "**********************************************************");
        }
    };
    
    final Runnable mForemostEventLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	if (INFO) Log.i(TAG, "Called mForemostEventLoaderCancelCallback");
        }
    };    
    
    
    final Runnable mBackmostEventLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	if (INFO) Log.i(TAG, "***********mBackmostEventLoaderSuccessCallback**************");
        	if ( (mPrvBackmostEvent.id == mCurrentBackmostEvent.id) && (mPrvBackmostEvent.endDay == mCurrentBackmostEvent.endDay) ){
        		return;
        	} 
        	
        	//printBothEndsEventContext(BothEndsEventLoader.BACKMOST_EVENT_LOADER);
        	
        	CentralActivity.this.mApp.setBothEndsEvent(BothEndsEventLoader.BACKMOST_EVENT_LOADER, mCurrentBackmostEvent);
        	mController.sendEvent(this, EventType.BOTHENDS_EVENTS_QUERY_COMPLETION, null, null, null, -1,
            		ViewType.CURRENT, CalendarController.EXTRA_BOTHENDS_BACKMOST_EVENT, 
            		null, null);
        	if (INFO) Log.i(TAG, "**********************************************************");
        }
    };
    
    final Runnable mBackmostEventLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	//if (INFO) Log.i(TAG, "Called mBackmostEventLoaderCancelCallback");
        }
    };    
    
    public void printBothEndsEventContext(int type) {
    	
    	if (INFO) Log.i(TAG, "**********************************************************");
    	
    	Calendar startTime = GregorianCalendar.getInstance(mTimeZone);
    	Calendar endTime = GregorianCalendar.getInstance(mTimeZone);
    	Calendar startJualianDayTime = GregorianCalendar.getInstance(mTimeZone);
    	Calendar endJualianDayTime = GregorianCalendar.getInstance(mTimeZone);
    	
    	if (type == BothEndsEventLoader.FOREMOST_EVENT_LOADER) {    		
	    	if (INFO) Log.i(TAG, "event id=" + String.valueOf(mCurrentForemostEvent.id));
	    	if (INFO) Log.i(TAG, "event title=" + mCurrentForemostEvent.title);
	    	if (INFO) Log.i(TAG, "event loaction=" + mCurrentForemostEvent.location);
	    	if (INFO) Log.i(TAG, "event allDay=" + String.valueOf(mCurrentForemostEvent.allDay));     	
	    	
	    	startTime.setTimeInMillis(mCurrentForemostEvent.startMillis);
	    	//if (INFO) Log.i(TAG, "event begin time=" + startTime.format2445());	    	
	    	
	    	endTime.setTimeInMillis(mCurrentForemostEvent.endMillis);
	    	//if (INFO) Log.i(TAG, "event end time=" + endTime.format2445());	    	
	    		    	
	    	long startJualianDayTimeMillis = ETime.getMillisFromJulianDay(mCurrentForemostEvent.startDay, mTimeZone, mFirstDayOfWeek);
	    	startJualianDayTime.setTimeInMillis(startJualianDayTimeMillis);
	    	//if (INFO) Log.i(TAG, "event start julianDay=" + startJualianDayTime.format3339(true));	    	
	    	
	    	long endJualianDayTimeMillis = ETime.getMillisFromJulianDay(mCurrentForemostEvent.startDay, mTimeZone, mFirstDayOfWeek);
	    	endJualianDayTime.setTimeInMillis(endJualianDayTimeMillis);
	    	//if (INFO) Log.i(TAG, "event end julianDay=" + endJualianDayTime.format3339(true));
    	}
    	else if (type == BothEndsEventLoader.BACKMOST_EVENT_LOADER) {    		
	    	if (INFO) Log.i(TAG, "event id=" + String.valueOf(mCurrentBackmostEvent.id));
	    	if (INFO) Log.i(TAG, "event title=" + mCurrentBackmostEvent.title);
	    	if (INFO) Log.i(TAG, "event loaction=" + mCurrentBackmostEvent.location);
	    	if (INFO) Log.i(TAG, "event allDay=" + String.valueOf(mCurrentBackmostEvent.allDay));      	
	    	
	    	startTime.setTimeInMillis(mCurrentBackmostEvent.startMillis);
	    	// (INFO) Log.i(TAG, "event begin time=" + startTime.format2445());	    	
	    	
	    	endTime.setTimeInMillis(mCurrentBackmostEvent.endMillis);
	    	//if (INFO) Log.i(TAG, "event end time=" + endTime.format2445());	    	
	    	
	    	long startJualianDayTimeMillis = ETime.getMillisFromJulianDay(mCurrentBackmostEvent.startDay, mTimeZone, mFirstDayOfWeek);
	    	startJualianDayTime.setTimeInMillis(startJualianDayTimeMillis);
	    	//if (INFO) Log.i(TAG, "event start julianDay=" + startJualianDayTime.format3339(true));    	
	    	
	    	long endJualianDayTimeMillis = ETime.getMillisFromJulianDay(mCurrentBackmostEvent.startDay, mTimeZone, mFirstDayOfWeek);
	    	endJualianDayTime.setTimeInMillis(endJualianDayTimeMillis);
	    	//if (INFO) Log.i(TAG, "event end julianDay=" + endJualianDayTime.format3339(true));
    	}
    	if (INFO) Log.i(TAG, "**********************************************************");
    }
    
    public CalendarViewsUpperActionBarFragment getUpperActionBarFragment() {
    	return mUpperActionBarFrag;
    }
}

