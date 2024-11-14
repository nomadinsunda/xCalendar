/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intheeast.month;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.AbsListView.LayoutParams;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.CalendarViewsLowerActionBar;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.CalendarViewsUpperActionBarFragment;
import com.intheeast.acalendar.CentralActivity;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.EventLoader;
import com.intheeast.acalendar.FastEventInfoFragment;
import com.intheeast.acalendar.EventInfoLoader;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;
import com.intheeast.etc.ETime;



import com.intheeast.month.MonthListView.FlingParameter;
import com.intheeast.month.MonthListView.MonthDimensionBelowVisibleLastWeek;
import com.intheeast.month.MonthListView.UnVisibleMonthPartOfVisibleFirstWeek;
import com.intheeast.settings.SettingsFragment;
import com.intheeast.year.YearPickerFragment;

@SuppressLint("ValidFragment")
public class MonthFragment extends ListFragment implements
        CalendarController.EventHandler, LoaderManager.LoaderCallbacks<Cursor>, OnScrollListener/*, OnTouchListener*/ {	
	
    private static final String TAG = "MonthFragment";
    private static boolean INFO = true;
    
    public enum ListViewState {
    	NoneState,    	
		Run,
		Run_FlingState,
        Finished
	}
    
    public enum ExpandEventListViewState {
    	NoneState,
    	ListViewMonthIndicatorTopAdjustmentScrollState,
		PrepareEventsViewModeEntranceAnimState,
		EventsViewModeEntranceAnimState,		
		ListViewFirstWeekTopAdjustmentScrollStateInEventsViewMode,
		Run,
		PrepareEventsViewModeExitAnimState,
		EventsViewModeExitAnimState,
		ListViewFirstWeekTopAdjustmentScrollStateForNormalMode,
        Finished
	}
        
    private static final int MINIMUM_SNAP_VELOCITY = 2200;    
    public static final float SWITCH_SUB_PAGE_VELOCITY = 2200 * 1.25f;
    public static final float SWITCH_MAIN_PAGE_VELOCITY = 2200 * 1.5f;  
    public final int ADJUST_TOP_VELOCITY = 5000;
    
    public final int GO_TO_TODAY_VELOCITY = 8000;
    
    public final int UPWARD_GO_TO_TODAY = 1;
    public final int DOWNWARD_GO_TO_TODAY = 2;
    public final int UPWARD_GO_TO_TODAY_BUT_NOT_EVENT_LOAD = 3;
    public final int DOWNWARD_GO_TO_TODAY_BUT_NOT_EVENT_LOAD = 4;

    static final int SCROLL_HYST_WEEKS = 2;
    
    private static final int WEEKS_BUFFER = 1;
    // How long to wait after scroll stops before starting the loader
    // Using scroll duration because scroll state changes don't update
    // correctly when a scroll is triggered programmatically.
    private static final int LOADER_DELAY = 100;
    //private static final int LOADER_DELAY = 5000; // for test
    // The minimum time between requeries of the data if the DB is
    // changing
    private static final int LOADER_THROTTLE_DELAY = 500;
    
    public static final int DAYS_PER_WEEK = 7;
	public static final int GOTO_SCROLL_DURATION = 500;

    // Selection and selection args for adding event queries
    private static final String WHERE_CALENDARS_VISIBLE = Calendars.VISIBLE + "=1";
    private static final String INSTANCES_SORT_ORDER = 
    		Instances.START_DAY + "," + Instances.START_MINUTE + "," + Instances.TITLE; 
    
    ECalendarApplication mApp;
    Context mContext;
    CentralActivity mActivity;
    CalendarController mController;
    
    CalendarViewsUpperActionBarFragment mUpperActionBarFrag;    
    
    RelativeLayout mFrameLayout;
    ViewSwitcher mFrameLayoutViewSwitcher;
    CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar; 
    RelativeLayout mMonthViewLayout;
    MonthListView mListView;
    EventsListView mEventsListView;    
       
    CalendarViewsLowerActionBar mLowerActionBar;   
    //TextView mTodayTextView;
	//TextView mCalendarsView;
	//TextView mInboxView;
		
	
	CalendarViewsLowerActionBar mMonthviewPsudoLowerActionBar; 
	
	
	FastEventInfoFragment mEventInfoFragment;
	
	
	PsudoNonTargetMonthView mPsudoPrvMonthView;	
	PsudoTargetMonthView mPsudoTargetMonthView;    
     
    
    PsudoTargetMonthViewAtEventListEntrance mPsudoTargetMonthViewAtEventListEntrance = null;
    PsudoTargetMonthViewAtEventListExit mPsudoTargetMonthViewAtEventListExit = null;
    LinearLayout mPsudoNextMonthRegionContainer = null;
    
    LinearLayout mPsudoUpperLisViewContainer = null;
    LinearLayout mPsudoUpperLisViewTailContainer = null;
    
    PsudoExtendedDayView mPsudoExtendedDayView = null;
    PsudoYearView mPsudoYearView = null;
    
    
    MonthAdapter mAdapter;
    EventItemsAdapter mEventItemsAdapter;   
    
    public boolean mHideDeclined;

    public int mFirstLoadedJulianDay;
    public int mLastLoadedJulianDay;

    

    private CursorLoader mLoader;
    private Uri mEventUri;
    // mDesiredDay�� �뵵��?
    

    //private volatile boolean mShouldLoad = true;    
    private boolean mIsDetached;  

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*********************************************************************************************************************/
    
    public ListViewState mListViewState = ListViewState.NoneState;
    public ExpandEventListViewState mExpandEventListViewState = ExpandEventListViewState.NoneState;
    
    String mTzId;
    TimeZone mTimeZone;
    long mGmtoff;
    
    //Time mSelectedDay = new Time();
    Calendar mCurrentMonthTimeDisplayed = GregorianCalendar.getInstance();
    //Time mCurrentMonthTime = new Time();
    Calendar mTempTime = GregorianCalendar.getInstance();
    //Time mFirstDayOfMonth = new Time();
    Calendar mFirstVisibleDay = GregorianCalendar.getInstance();
    
    int mFirstDayOfWeek;
    int mDaysPerWeek = 7;
    
    
    //final Time mDesiredDay = new Time();
    
    int mAnticipationMonthViewLayoutHeight;
    int mAnticipationMonthListViewWidth;
    int mAnticipationMonthListViewHeight;
    
    int mCurrentMonthFirstJulianDay = 0;   
    
    //int mNumWeeks = 6;
    int mNumWeeks = 5;
    
    
    
    int mCurrentMonthDisplayed;
    // used for tracking during a scroll
    //public long mPreviousScrollPosition;
    long mPreviousFirstVisiblePosition;
    long mPreviousFirstChildBottom;
    // used for tracking which direction the view is scrolling
    boolean mIsScrollingUp = false;
    // used for tracking what state listview is in
    int mPreviousScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    // used for tracking what state listview is in
    int mCurrentScrollState = OnScrollListener.SCROLL_STATE_IDLE;    
    
    boolean mUserScrolled = false;
    //public boolean mExpandEventViewMode = false;
    
    
    int mCalendarViewsSecondaryActionBarId = 0;
        
    int mCurrentMonthViewMode;
    
    
    public EventLoader mOneDayEventLoaderForEventListView;    
    final ArrayList<Event> mOneDayEventsFromEventLoaderForEventListView = new ArrayList<Event>();
	public ArrayList<Event> mOneDayEventsForEventListView = null;    
	
    
    
    String[] mDayLabels;   
    
    
    
    
    CollapseEventViewInfo mCollapseEventViewInfo = null;
    
    int mRecoveryMonthHeight = 0;
    
    
	
	float mWidth;
	
	float mMinimumFlingVelocity;
	Calendar mSelectedDayInEPMode = GregorianCalendar.getInstance();
	public static int LIST_TOP_OFFSET = 0;
	int WEEK_MIN_VISIBLE_HEIGHT = 12;
	int BOTTOM_BUFFER = 20;
	int mSaturdayColor = 0;
	int mSundayColor = 0;
	int mDayNameColor = 0;
	static float mScale = 0;
    
	
    
	private static final String KEY_CURRENT_TIME = "current_time";
	
	//private static final String KEY_CURRENT_SELECTED_DAY_IN_EEMODE = "current_selected_day_in_eemode";
	
	public EventLoader mEventLoaderForEventListView;
	public EventInfoLoader mEventInfoLoaderForEventListView;     
	
	Handler mHandler;
      
    
    float mFriction = 0.5f;    
    
	
	
    public ArrayList<ArrayList<Event>> mExpandEventModeTargetMonthEventsList = new ArrayList<ArrayList<Event>>();
    public boolean[] mExpandEventModeTargetMonthEventExsitence;   
	
    ExpandEventViewInfo mExpandEventViewInfo = null;
	
	
    //int mMonthListViewItemMoveDelta = 0;
    long mMonthListViewAnimDuration = 0;
	
    EventListViewEntranceAnimCompletionCallback mEventListViewEntranceAnimCompletionCallback = null;
    EventListViewExitAnimCompletionCallback mEventListViewExitAnimCompletionCallback = null;
    
    public ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();
    
    
    int mCurrentEventListHeight;
    int mTargetEventListHeight;
    float mTotalValue;
    float mTotalAbsValue;    
    ValueAnimator mScaleValueAnimator;
    long mEventListViewAnimDuration;
    
    
    //float mMonthListViewTopOffset;
    int mMonthListViewMonthIndicatorHeight;
    int mMonthListViewNormalWeekHeight;
    
    float mMonthListMonthDayTextBaselineY;
    float mMonthListViewMonthIndicatorTextHeight;
    float mMonthListViewMonthIndicatorTextBaselineY;
    float mMonthListMonthDayTextHeight;
    
    int mAnticipationMonthListViewHeightInEPMode;
    int mMonthListViewMonthIndicatorHeightInEPMode;
    int mMonthListViewNormalWeekHeightInEPMode;
    
    float mMonthListMonthDayTextBaselineYInEPMode;
    float mMonthListViewMonthIndicatorTextHeightInEPMode;
    float mMonthListViewMonthIndicatorTextBaselineYInEPMode;
    float mMonthListMonthDayTextHeightInEPMode;
    
    int mAnticipationYearListViewWidth;
    int mAnticipationYearListViewHeight;
    int mAnticipationYearIndicatorHeight;
    int mAnticipationYearIndicatorTextHeight;
    int mAnticipationYearIndicatorTextBaseLineY;
    
    int mAnticipationMiniMonthWidth;
    int mAnticipationMiniMonthHeight;
    int mAnticipationMiniMonthLeftMargin;
    int mAnticipationMiniMonthIndicatorTextHeight;
    int mAnticipationMiniMonthIndicatorTextBaseLineY;
    int mAnticipationMiniMonthDayTextBaseLineY;
    int mAnticipationMiniMonthDayTextHeight;    
    
    int mDayHeaderHeight;
    
    
    long mLastReloadMillis = 0;
    
    
    public boolean mGoToExitAnim = false;
    
    /*********************************************************************************************************************/
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public DataSetObserver mObserver = new DataSetObserver() {
		// notifyDataSetChanged ȣ�⿡ ���� ȣ��ȴ�
        @Override
        public void onChanged() {            
            /*
            Time day = mAdapter.getSelectedDay();
            if (day.year != mSelectedDay.year || day.yearDay != mSelectedDay.yearDay) {            	
                goTo(true, false);
            }
            */
        }
    };
    
    public DataSetObserver mEventItemAdapterObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
        	 	            
        }
    };
    
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTzId  = Utils.getTimeZone(mContext, mTZUpdater);
            mTimeZone = TimeZone.getTimeZone(mTzId);
            mGmtoff = mTimeZone.getRawOffset() / 1000;
            ETime.switchTimezone(mCurrentMonthTimeDisplayed, mTimeZone);//mCurrentMonthTimeDisplayed.timezone = tz;            
            //mCurrentMonthTimeDisplayed.normalize(true);
            ETime.switchTimezone(mTempTime, mTimeZone);//mTempTime.timezone = tz;            
            ETime.switchTimezone(mFirstVisibleDay, mTimeZone);//mFirstVisibleDay.timezone = tz;
            
            if (mAdapter != null) {
            	Log.i("tag", "mTZUpdater : call refresh");
                mAdapter.refresh();
            }
        }
    };    
     
    // This causes an update of the view at midnight
    public Runnable mTodayUpdater = new Runnable() {
        @Override
        public void run() {
            Calendar midnight = GregorianCalendar.getInstance(mFirstVisibleDay.getTimeZone());//new Time(mFirstVisibleDay.timezone);
            midnight.setTimeInMillis(System.currentTimeMillis());//midnight.setToNow();
            long currentMillis = midnight.getTimeInMillis();

            midnight.set(Calendar.HOUR_OF_DAY, 0);//midnight.hour = 0;
            midnight.set(Calendar.MINUTE, 0);//midnight.minute = 0;
            midnight.set(Calendar.SECOND, 0);//midnight.second = 0;
            midnight.add(Calendar.DAY_OF_MONTH, 1);//midnight.monthDay++;
            long millisToMidnight = midnight.getTimeInMillis() - currentMillis;
            mHandler.postDelayed(this, millisToMidnight);

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };
    
    
    
    
    // Used to load the events when a delay is needed
    /*
    Runnable mLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsDetached) {
            	Log.i("tag", "mLoadingRunnable.run");
                mLoader = (CursorLoader) getLoaderManager().initLoader(0, null,
                        MonthFragment.this);
            }
        }
    };
    */
    
    /*
    OnClickListener mTodayClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Time t = null;
	        int viewType = ViewType.CURRENT;
	        //long extras = CalendarController.EXTRA_GOTO_TIME;
	        //extras |= CalendarController.EXTRA_GOTO_TODAY;
	        
	        viewType = ViewType.CURRENT;
            t = new Time(mTimeZoneID);
            t.setToNow();
            long extras = CalendarController.EXTRA_GOTO_TODAY;
            CalendarController controller = CalendarController.getInstance(mContext);    
            controller.sendEvent(MonthFragment.this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
		}		
	};
	
	OnClickListener mCalendarClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			CentralActivity activity = (CentralActivity) MonthFragment.this.getActivity();
			View contentView = activity.getContentView();
			Utils.makeContentViewBitmap(activity, contentView);			
			
            mController.sendEvent(MonthFragment.this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                    0, 0);
		}		
	};
	*/
    
    Runnable mListViewFirstWeekTopAdjustmentScrollRunnable = new Runnable() {

		@Override
		public void run() {		
			
			mExpandEventListViewState = ExpandEventListViewState.ListViewFirstWeekTopAdjustmentScrollStateInEventsViewMode;
			//mAdapter.setExpandEventViewState(mExpandEventListViewState);
			int position = ETime.getWeeksSinceEcalendarEpochFromJulianDay(mExpandEventViewInfo.mFirstDayJulianDayInMonth, mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(mExpandEventViewInfo.mFirstDayJulianDayInMonth, mFirstDayOfWeek); 
        	int weekPattern = MonthAdapter.calWeekPattern(position, mTzId, mFirstDayOfWeek);
        	int listTopOffset = 0;
        	if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {							
				listTopOffset = mListView.getEEMNormalWeekHeight() + mListView.getEEMMonthIndicatorHeight(); // -������ �����ؾ� list �׸���� �ö󰣴�			
        	}
        	else {
        		listTopOffset = mListView.getEEMMonthIndicatorHeight(); 			
        	}
        	
        	listTopOffset = -listTopOffset; // -������ �����ؾ� list �׸���� �ö󰣴�	
        	//mPreviousScrollState = OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
        	
        	if (INFO) Log.i(TAG, "mListViewFirstWeekTopAdjustmentScrollRunnable ");       	
        	
        	mListView.setSelectionFromTop(position, listTopOffset);         	        	
        	
        	
        	// �Ʒ� ������δ� ȿ���� ����...�� �ϱ�?...
        	// mListViewFirstWeekTopAdjustmentScrollRunnable�� MonthListView.OnGlobalLayoutListener���� ȣ��ȴ�
        	// notifyDataSetChanged�� ȣ������ ������ ListView�� ������ �ȴ�
			//mAdapter.notifyDataSetChanged();
        	
        	// �Ƹ��� ���� ���ƿ��� setSelectionFromTop�� ȣ���ϰ� notifyDataSetChanged�� ȣ���ؼ� listview�� ������â�� �ǳ� ����
        	// setSelectionFromTop�� �׷��� ����� �۵����� �ʳ� ��
        	// ��ſ� onScrollStateChanged�� ȣ���ؼ� mUpdateLoader�� �� ������ �ƴ϶� �ٸ� ����?���� notifyDataSetChanged�� ȣ���ؼ� �ƹ��� ������ ���� �ʳ� �ʹ�
        	// :���� ��Ȯ�� ���� �� ����
        	//  ->�׷��ٸ� � �ַ���� ��� �ϴ°�? runable�� ����ؾ� �ϴ� ��...
		}    	
    };
    
    // ������ �ָ� ����� �� ����
    // ���� ��Ȯ�� ���� setSelectionFromTop�� ���� onScroll�� ���� �Ϸ�ǰ�
    // �Ʒ� �۾����� �����ϴ� ���̴�...
    // onScroll�� ���� ���Ǵ� ������ ����
    // :Callback method to be invoked when the list or grid has been scrolled. This will be called after the scroll has completed
    //  ->��ũ���� �Ϸ�� �Ŀ� ȣ��ȴٰ� �Ѵ�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //    �׷��ٸ� �츮�� Ư�� flag�� ����ؼ� ��� �� �� ���°�???
    // *setSelectionFromTop �Ŀ� onScroll�� �� �ѹ� ȣ��Ǵ� ������ ���δ� : �ѹ� �� Ȯ���� ����-> �ѹ� ȣ��Ǵ� �� �´�!!!
    private final Runnable mTextXXX = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
            	mExpandEventListViewState = ExpandEventListViewState.Run; // �� �ڵ尡 ListView�� ���?����� �Ѵٰ� �����Ѵ�
            	                                                          // expand mode�� �����ϰ� ���� �Ʒ� notifyDataSetChanged ȣ��� ���� getView�� ó�� ȣ��� ��
            	                                                          // mPsudoTargetMonthViewAtEventListEntrance�� GONE ���·�
            															  // mListView�� VISIBLE ���·� ����� ������
            															  // �� �ڵ尡 ������� �ʴ´ٸ� mListView�� ����ؼ� INVISIBLE ���·� ���� �ְ� �����ν� 
            															  // ��ġ mListView�� ���?�Ȱ�ó�� ��������
				//mAdapter.setExpandEventViewState(mExpandEventListViewState);
   				//CalendarController controller = CalendarController.getInstance(mContext);
   				mController.sendEvent(this, EventType.EXPAND_EVENT_VIEW_OK, null, null, -1, ViewType.MONTH);  
   				
            	mAdapter.notifyDataSetChanged();  
            }
        }
    };
    
    Runnable mExpandEventListModeFinishedRunnable = new Runnable() {

		@Override
		public void run() {						
			mExpandEventListViewState = ExpandEventListViewState.ListViewFirstWeekTopAdjustmentScrollStateForNormalMode;
			
			int position = ETime.getWeeksSinceEcalendarEpochFromJulianDay(mCurrentMonthFirstJulianDay, mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(mCurrentMonthFirstJulianDay, mFirstDayOfWeek); 
        	int weekPattern = MonthAdapter.calWeekPattern(position, mTzId, mFirstDayOfWeek);
        	int listTopOffset = 0;
        	if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {					
        		listTopOffset = mListView.getNMNormalWeekHeight();
				listTopOffset = -listTopOffset;		
        	}
        	        	
        	//mPreviousScrollState = OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
        	
        	if (INFO) Log.i(TAG, "mExpandEventListModeFinishedRunnable ");        	
        	
        	mListView.setSelectionFromTop(position, listTopOffset); 	 
        	
		}    	
    };    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    	
	public MonthFragment(Context context, long initialTime, boolean isMiniMonth, CalendarViewsUpperActionBarFragment upperActionBar) {
		if (INFO) Log.i(TAG, "MonthFragment Constructor ");
		
		mTzId  = Utils.getTimeZone(mContext, mTZUpdater);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
        
		mCurrentMonthTimeDisplayed.setTimeInMillis(initialTime);		
		mCurrentMonthTimeDisplayed.set(mCurrentMonthTimeDisplayed.get(Calendar.YEAR), mCurrentMonthTimeDisplayed.get(Calendar.MONTH), 1);
		//mCurrentMonthTimeDisplayed.normalize(true);
		
		// �ǹ̰� �ִ°�?
		// if the fragment is not in the resumed state, �׳� �ƹ��� �۾��� ���� �ʰ� �����Ѵ�
		// �׷��� ���⼭ ȣ���ϴ� ������...
    	goTo(true, true);
        
    	mHandler = new Handler();
            	
    	mUpperActionBarFrag = upperActionBar;    	
    }
	
	int mPreviousViewType;
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	if (INFO) Log.i(TAG, "onAttach");
    	
    	
    	mActivity = (CentralActivity) activity; 
    	mApp = (ECalendarApplication) activity.getApplication();
    	
        mContext = activity.getApplicationContext();
        
        mController = CalendarController.getInstance(mActivity);//CalendarController.getInstance(mContext);
        
        mPreviousViewType = mController.getPreviousViewType();
        
        mWidth = getResources().getDisplayMetrics().widthPixels;
        
        mCurrentMonthViewMode = Utils.getSharedPreference(mContext, SettingsFragment.KEY_MONTH_VIEW_LAST_MODE, SettingsFragment.NORMAL_MONTH_VIEW_MODE);
                
        mTzId = ETime.getCurrentTimezone();
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        mOneDayEventLoaderForEventListView = new EventLoader(mContext);
        mOneDayEventLoaderForEventListView.startBackgroundThread();
		
        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        mMinimumFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();

        calcFrameLayoutHeight();
        
        // Ensure we're in the correct time zone
        ETime.switchTimezone(mCurrentMonthTimeDisplayed, mTimeZone);//mCurrentMonthTimeDisplayed.switchTimezone(mTimeZoneID);                
        ETime.switchTimezone(mSelectedDayInEPMode, mTimeZone);//mSelectedDayInEPMode.switchTimezone(mTimeZoneID);
        ETime.switchTimezone(mFirstVisibleDay, mTimeZone);//mFirstVisibleDay.timezone = mTimeZoneID;
        ETime.switchTimezone(mTempTime, mTimeZone);//mTempTime.timezone = mTimeZoneID;//////////////////////////���� �߻�

        Resources res = activity.getResources();
        mSaturdayColor = res.getColor(R.color.month_saturday);
        mSundayColor = res.getColor(R.color.month_sunday);
        mDayNameColor = res.getColor(R.color.month_day_names_color);

        // Adjust sizes for screen density
        if (mScale == 0) {
            mScale = mActivity.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_MIN_VISIBLE_HEIGHT *= mScale;
                BOTTOM_BUFFER *= mScale;
                LIST_TOP_OFFSET *= mScale;
            }
        }        
        
        calcTodayRedCircleDimen();
                
        ////////////////////////////////////////////////////////////////////////////////
        setUpAdapter(false);  
        
        setListAdapter(mAdapter);         
        ///////////////////////////////////////////////////////////////////////////////////////////////////           
        
        mTZUpdater.run();
        
        mIsDetached = false;
               
    	mEventInfoFragment = new FastEventInfoFragment(mContext, activity, this, ViewType.MONTH, mWidth);
    	
    	mEventInfoFragment.onAttach(activity);
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (INFO) Log.i(TAG, "onCreate");
        
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CURRENT_TIME)) {
        	long curTimeMills = savedInstanceState.getLong(KEY_CURRENT_TIME);
        	mCurrentMonthTimeDisplayed.setTimeInMillis(curTimeMills);  		
        	
            goTo(true, true);
        }
                
        mEventLoaderForEventListView = new EventLoader(mContext);
    	  
    	mEventInfoLoaderForEventListView = new EventInfoLoader(mContext); 
    }    
            
    
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	if (INFO) Log.i(TAG, "onCreateView");
    	
    	mFrameLayout = (RelativeLayout) inflater.inflate(R.layout.unified_month_fragment_layout, container, false);
    	// android:layout_above="@+id/monthview_lower_actionbar" ������
    	mFrameLayoutViewSwitcher = (ViewSwitcher) mFrameLayout.findViewById(R.id.month_fragment_switcher);    	
    	mLowerActionBar = (CalendarViewsLowerActionBar) mFrameLayout.findViewById(R.id.monthview_lower_actionbar);
    	mLowerActionBar.init(this, mTzId, mController, false);
        mLowerActionBar.setWhoName("monthview_lower_actionbar");
        
        mMonthViewLayout = (RelativeLayout) inflater.inflate(R.layout.monthview_layout, null);    
        mPsudoUpperLisViewContainer = (LinearLayout) mMonthViewLayout.findViewById(R.id.psudo_month_upperlist);
        mPsudoUpperLisViewTailContainer = (LinearLayout) mMonthViewLayout.findViewById(R.id.psudo_month_upperlist_tail);
        
        mCalendarViewsSecondaryActionBar = (CalendarViewsSecondaryActionBar)mMonthViewLayout.findViewById(R.id.monthview_secondary_actionbar);
        mCalendarViewsSecondaryActionBarId = mCalendarViewsSecondaryActionBar.getId();
        
        mCalendarViewsSecondaryActionBar.init(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);         
        
        mEventsListView = (EventsListView) mMonthViewLayout.findViewById(R.id.eventslist);        
        
        mPsudoYearView = (PsudoYearView) mMonthViewLayout.findViewById(R.id.psudo_yearview);
        mPsudoPrvMonthView = (PsudoNonTargetMonthView) mMonthViewLayout.findViewById(R.id.psudo_prv_month_view);
        mPsudoTargetMonthView = (PsudoTargetMonthView) mMonthViewLayout.findViewById(R.id.psudo_target_month_view);
        mPsudoExtendedDayView = (PsudoExtendedDayView) mMonthViewLayout.findViewById(R.id.psudo_extended_dayview);
        mPsudoNextMonthRegionContainer = (LinearLayout) mMonthViewLayout.findViewById(R.id.psudo_month_lowerlist);
        
        // mMonthviewPsudoLowerActionBar�� ������ �ʾƵ� android:visibility="invisible"�� ������ ������
        // layout�� ȿ�������� ��ġ�ϱ� ���ؼ���
        mMonthviewPsudoLowerActionBar = (CalendarViewsLowerActionBar)mMonthViewLayout.findViewById(R.id.monthview_psudo_lower_actionbar);
        mMonthviewPsudoLowerActionBar.init(this, mTzId, mController, true);
        mMonthviewPsudoLowerActionBar.setWhoName("monthview_psudo_lower_actionbar");
        //mMonthviewPsudoLowerActionBar.setBackgroundColor(Color.GREEN); // for test
    	
        mFrameLayoutViewSwitcher.addView(mMonthViewLayout);
        
        mEventInfoFragment.onCreateView(inflater, container, savedInstanceState, 
        		mFrameLayoutViewSwitcher, 
        		mMonthViewLayout,
        		mUpperActionBarFrag,
        		true, mCalendarViewsSecondaryActionBar,
        		mLowerActionBar, mMonthviewPsudoLowerActionBar,
        		mTimeZone.getID());
        
        return mFrameLayout;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (INFO) Log.i(TAG, "onActivityCreated");
        
        setUpListView();
        
        MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);
        if (child != null) {
        	//int julianDay = child.getFirstJulianDay();
        	//long julianDayMills = ETime.getMillisFromJulianDay(julianDay, mTimeZone);
        	mFirstVisibleDay.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mFirstVisibleDay.setJulianDay(julianDay);
            // set the title to the month of the second week
        	//long mills = ETime.getMillisFromJulianDay(julianDay + DAYS_PER_WEEK, mTimeZone);
        	       	
        	mTempTime.setTimeInMillis(mFirstVisibleDay.getTimeInMillis());//mTempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        	mTempTime.add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK);
        	
            setMonthDisplayed(mTempTime);
        }  
                
        mListView.setSelector(new StateListDrawable());
        //mListView.setOnTouchListener(this);    
        
        // To get a smoother transition when showing this fragment, delay loading of events until
        // the fragment is expended fully and the calendar controls are gone.        	
        
        //mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this); // �� �ٷ� onCreateLoader�� ȣ��ȴ�
                                                                               // onCreateLoader�� ������ CursorLoader�� 
            																   // mLoader�� �����Ѵ�        
        
        mAdapter.setListView(this, mListView);///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    } 
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (INFO) Log.i(TAG, "onResume");
        
        mEventLoaderForEventListView.startBackgroundThread();
    	  
    	mEventInfoLoaderForEventListView.startBackgroundThread();
    	
        doResumeUpdates();
        
        setUpAdapter(mPaused);
        
        if (mPaused)
        	mPaused = false;     
                
        goTo(false, false);               
        
    }
    
    @Override
	public void onStart() {		
		super.onStart();
		
		if (INFO) Log.i(TAG, "onStart");
	}

	private boolean mPaused = false;
    @Override
    public void onPause() {        
        if (INFO) Log.i(TAG, "onPause ");
        
        mPaused = true;
        
        mHandler.removeCallbacks(mTodayUpdater);
                
        mEventLoaderForEventListView.stopBackgroundThread();
    	  
    	mEventInfoLoaderForEventListView.stopBackgroundThread();
        
        super.onPause();
    }   
    
    @Override
	public void onStop() {
    	if (INFO) Log.i(TAG, "onStop");
		super.onStop();
	}
    
    
    // Detach from list view
    @Override
	public void onDestroyView() {
    	if (INFO) Log.i(TAG, "onDestroy");
		super.onDestroyView();
	}


	@Override
	public void onDestroy() {
		if (INFO) Log.i(TAG, "onDestroy");
		super.onDestroy();
	}
    
    @Override
    public void onDetach() {
    	if (INFO) Log.i(TAG, "onDetach ");
    	
        mIsDetached = true;
        
        if (mEventItemsAdapter != null)
			mEventItemsAdapter.closeEventCursors(); // cursor�� close ���� ������,
    	                                            // :could not allocate cursorwindow due to error 12 �߻��Ѵ�      
               
        if (mListView.getExpandEventViewModeState())
        	mCurrentMonthViewMode = SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE;
        else
        	mCurrentMonthViewMode = SettingsFragment.NORMAL_MONTH_VIEW_MODE;
        
        Utils.setSharedPreference(mContext, SettingsFragment.KEY_MONTH_VIEW_LAST_MODE, mCurrentMonthViewMode);
        
        super.onDetach();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {   	
    	
    	if (INFO) Log.i(TAG, "onSaveInstanceState ");
    	
        outState.putLong(KEY_CURRENT_TIME, mCurrentMonthTimeDisplayed.getTimeInMillis());
                
    } 
    

    


	

	// ���δ�� ���� �̺�Ʈ �������� �ε��Ϸ� ���� ����
    // ���� mFirstLoadedJulianDay ���� Ȯ���� 
    // mFirstLoadedJulianDay �� mLastLoadedJulianDay���� ������!!!
    // :���� �׷� �ʿ䰡 ������?
    //  ���� ��ġ�� �κ��� �ִٸ� ��ġ�� �κ��� ���ϰ� �̺�Ʈ���� �ε��ϰ� �ٽ� ��ġ�� ������ �����ؾ� �ϱ� �����̴�
    int mFlingTargetMonthJulianDay;
    public static final String ALREADY_SET_LOAD_JULIANDAY_VALUES = "already_set_load_julianday_values";    
    public void loadEventsByFling(Calendar targetMonthCalendar) {
    	synchronized (mUpdateLoader) {
    		/*
    		stopLoader(); o
	        mEventUri = updateUriByJulianDay(julianDay);
	        mLoader.setUri(mEventUri);
	        mLoader.startLoading();
	        mLoader.onContentChanged(); 
	        */
    		
    		mHandler.removeCallbacks(mUpdateLoader);    		
    		
			mTempTime.setTimeInMillis(targetMonthCalendar.getTimeInMillis());
    		
    		int targetMonthJulianDay = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    		
    		mFlingTargetMonthJulianDay = targetMonthJulianDay;
    		int prvExtendDays = mNumWeeks * 7;                 
            mFirstLoadedJulianDay = targetMonthJulianDay - prvExtendDays;   
            int visibleDays = mNumWeeks * 7;
            int nextExtendDays = mNumWeeks * 7;
            mLastLoadedJulianDay = mFirstLoadedJulianDay + prvExtendDays + visibleDays + nextExtendDays;    
            
    		// Loader�� init�� ���� �ʴ� ��찡 �ִ�
    		// :�Ʒ� �ڵ尡 ������ ��Ÿ�� ������ �߻��Ѵ�    		
    		if (mLoader == null) {                     
                Bundle initBundle = new Bundle();
                initBundle.putBoolean(ALREADY_SET_LOAD_JULIANDAY_VALUES, true);
    			mLoader = (CursorLoader) getLoaderManager().initLoader(0, initBundle, this);    			
    		}	
    		else {
    			mLoader.stopLoading();  
    			               
                // -1 to ensure we get all day events from any time zone
    			long mills = ETime.getMillisFromJulianDay(mFirstLoadedJulianDay - 1, mTimeZone, mFirstDayOfWeek);
    			mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
                long start = mTempTime.getTimeInMillis();                        
                  
                // +1 to ensure we get all day events from any time zone
                mills = ETime.getMillisFromJulianDay(mLastLoadedJulianDay + 1, mTimeZone, mFirstDayOfWeek);
                mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mLastLoadedJulianDay + 1);
                long end = mTempTime.getTimeInMillis();

                // Create a new uri with the updated times
                Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, start);
                ContentUris.appendId(builder, end);
                mEventUri = builder.build();            

                mLoader.setUri(mEventUri);
                mLoader.startLoading();
                mLoader.onContentChanged();          
    		} 		
    		
    	}
    }
    
	
	public void excuteGoToTodayWorkInNMode(int delta, long duration) {		
		
		mListView.smoothScrollBy(delta, (int) duration); 			
	}

	public void excuteGoToTodayWorkInEEMode(int delta, long duration, boolean runEventListViewScaleHeight) {		
		
		mListView.smoothScrollBy(delta, (int) duration); 	
		
		if (runEventListViewScaleHeight) {
			EventListViewScaleHeightRunnable kickOff2 = new EventListViewScaleHeightRunnable();
			mEventsListView.postOnAnimation(kickOff2);
		}		
	}
	
	boolean mFirstCallOfUpdateLoader = true;
    /*
    Callback method to be invoked while the list view or grid view is being scrolled. 
    If the view is being scrolled, this method will be called before the next frame of the scroll is rendered. 
    In particular, 
    it will be called before any calls to Adapter.getView(int, View, ViewGroup).
   */
    @Override
   	public void onScrollStateChanged(AbsListView view, int scrollState) {
    	//if (INFO) Log.i(TAG, "onScrollStateChanged");
    	
    	synchronized (mUpdateLoader) {    		
    		
    		if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {    	
    			if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) { 
    				// 1.scroll �߻�
    				//
    				// 2.fling �߻� :
    				//   1)MonthListView.doFlingInNMode���� onScrollStateChanged(this, OnScrollListener.SCROLL_STATE_FLING)�� ȣ����
    				//   2)smoothScrollBy ȣ��� ���� fling �߻�!    				
    				//     -go to today
    				//     -fling�߿� EXPAND_EVENT_LISTVIEW�� �߻��ϸ� ȣ���     				
    				stopLoader();    			
    			}
               
			} else { //SCROLL_STATE_IDLE
				//if (INFO) Log.i(TAG, "SCROLL_STATE_IDLE");
				if (mFirstCallOfUpdateLoader) {
            		// �츮�� Day Fragment�� Year Fragment�� ��û�� ����
            		// Month Fragment�� �����Ǿ��ٸ�,
            		// �и�  Day Fragment�� Year Fragment���� Target Month�� ���� event ��������
            		// ECalendarApplication�� cache ���� ������ ������ �� �ִ�
            		// ������ �� ĳ���� �̺�Ʈ �������� Ȱ���Ѵ�
            		mFirstCallOfUpdateLoader = false;
            		
            		// �׷���
            		// CentralActivity�� ���� �����Ǹ鼭 ȣ��Ȱ���
            		// Day Fragment�� Year Fragment�� ��û�� ���� �����Ȱ����� ��� �Ǵ��� �� �ִ°�?     
            		boolean waitingProcessEventsChangedFlag = mActivity.getWaitingProcessEventsChangedFlag();
            		if ( (mPreviousViewType == ViewType.DAY || mPreviousViewType == ViewType.YEAR) &&
            				!waitingProcessEventsChangedFlag) {
            			
            			// ECalendarApplication���� �̺�Ʈ ������ ��������
            			ArrayList<Event> events = mApp.getEvents();
            			
            			// mFirstLoadedJulianDay�� mLastLoadedJulianDay�� ����
            			// ExitDayViewEnterMonthViewAnim���� ������ ���� �Ⱓ���� �����Ѵ�
            			//int firstJulianDay = mApp.getFirstLoadedJulianDay();
            			//int lastJulianDay = mApp.getLastLoadedJulianDay();
            			//int numDays = lastJulianDay - firstJulianDay + 1;
            			//mAdapter.setEvents(firstJulianDay, numDays, events);
            			
            			mFirstLoadedJulianDay = mApp.getFirstLoadedJulianDay();
            			mLastLoadedJulianDay = mApp.getLastLoadedJulianDay();
            			int numDays = mLastLoadedJulianDay - mFirstLoadedJulianDay + 1;
            			mAdapter.setEvents(mFirstLoadedJulianDay, numDays, events);
            		}
            		else {
            			if (waitingProcessEventsChangedFlag) {
                			mActivity.setWaitingProcessEventsChangedFlag(false);
                		}
            			// ������ onActivityCreated���� �ʱ�ȭ ��Ű�� ����
            			// onScrollStateChanged�� ������ ������
            			// :initLoader�� ȣ��Ǹ� ������ �ε��� ���۵Ǳ� �����̴�
            			//  (mLoader.forceLoad ȣ�� ���̵� ���̴�)
            			if (mLoader == null) {
            				mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this);            				
            			}
            		}
            		
            	}
				else {				
					// ���� ��Ȳ�� mFirstCallOfUpdateLoader�� true ����,
					// mPreviousViewType�� ViewType.DAY �Ǵ� ViewType.YEAR ������
					// CursorLoader�� �ʱ�ȭ ������ ����. �׷��Ƿ� �ʱ�ȭ�� ���Ѿ� �Ѵ�
					if (mLoader == null) {
						mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this);
						//mLoader.setUpdateThrottle(1);
					}
					else {						
						// mPreviousScrollState ���� Fling�� �ƴ϶� ��ũ�Ѹ����� ���ǵǴ� �����?....
						// setSelectionFromTop�� scrolling���� �ؼ��ǳ�?
						if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
							if (mPreviousScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
								mHandler.removeCallbacks(mUpdateLoader);							
								//if (INFO) Log.i(TAG, "SCROLL_STATE_IDLE, PRV:SCROLL_STATE_TOUCH_SCROLL");								
								mHandler.post(mUpdateLoader);
							}
						}
					}
				}
    		}
    	}
       
    	if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {    	
    		
    		// ���� mUserScrolled �÷��״� �ƹ��� �ǹ̰� ����
    		mUserScrolled = true;            
    	}
    	else if (scrollState == OnScrollListener.SCROLL_STATE_FLING) { 
    		
    		// ���� mUserScrolled �÷��״� �ƹ��� �ǹ̰� ����
    		mUserScrolled = true;       	
    	}
    	else if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) { 
    		//if (INFO) Log.i(TAG, "onScrollStateChanged:SCROLL_STATE_IDLE");    			
    	}

    	mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
       
    	if (mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {
    		if (mExpandEventListViewState == ExpandEventListViewState.ListViewMonthIndicatorTopAdjustmentScrollState) {
    			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
    				
    				mExpandEventListViewState = ExpandEventListViewState.PrepareEventsViewModeEntranceAnimState;
    				// Event View Entrance Animation Runnable�� ������Ű��
    				mHandler.post(mPrePareEventListViewEntranceRunnable);        			
    			}
    		} 
    		else if (mExpandEventListViewState == ExpandEventListViewState.ListViewFirstWeekTopAdjustmentScrollStateInEventsViewMode) { // �� �̻� ������ �ʴ´�
    			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
    				
    				mExpandEventListViewState = ExpandEventListViewState.Run;    				  			
    				//mAdapter.setExpandEventViewState(mExpandEventListViewState);
       				//CalendarController controller = CalendarController.getInstance(mContext);
       				mController.sendEvent(this, EventType.EXPAND_EVENT_VIEW_OK, null, null, -1, ViewType.MONTH);       			
    			}
    		}
    		// ExpandEventListViewState.Finished
    		else if (mExpandEventListViewState == ExpandEventListViewState.ListViewFirstWeekTopAdjustmentScrollStateForNormalMode) { // �� �̻� ������ �ʴ´�...
    			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
    				
    				mExpandEventListViewState = ExpandEventListViewState.Finished;    						
    				//mAdapter.setExpandEventViewState(mExpandEventListViewState);       			        			
    				//CalendarController controller = CalendarController.getInstance(mContext);
    				mController.sendEvent(this, EventType.COLLAPSE_EVENT_VIEW_OK, null, null, -1, ViewType.MONTH);     
    				mCurrentMonthViewMode = SettingsFragment.NORMAL_MONTH_VIEW_MODE;
    			}
    		}
    	}
    }
    
    public void onScrollStateChangedInNMode(AbsListView view, int scrollState) {
    	
    }
    
    @Override
    public void onScroll(
           AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {    	
    	
   		MonthWeekView child = (MonthWeekView)view.getChildAt(0);
   		if (child == null) {
   			if (INFO) Log.i(TAG, "onScroll: child null");
   			return;
   		}       
       
   		// Figure out where we are
   		// �츮�� ��Ȯ�� �ð���� �����Ѵ�    	
   		long currFirstVisiblePosition = view.getFirstVisiblePosition();   		
   		int currFirstChildBottom = child.getBottom();               
   		
   		mFirstVisibleDay.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());     
       
   		if (currFirstVisiblePosition < mPreviousFirstVisiblePosition) { // downward[finger up->down direction] fling
   			mIsScrollingUp = true;
   		} else if (currFirstVisiblePosition > mPreviousFirstVisiblePosition) {
   			mIsScrollingUp = false;
   		} else { //currFirstVisiblePosition == mPreviousFirstVisiblePosition       	
   			if (currFirstChildBottom < mPreviousFirstChildBottom) {
   				mIsScrollingUp = true;
   			}
   			else if (currFirstChildBottom > mPreviousFirstChildBottom) {
   				mIsScrollingUp = false;
   			}
   			else { //currFirstChildBottom == mPreviousFirstChildBottom
	       		
   				if (mListViewState != ListViewState.Run_FlingState) {
   					// Run_FlingState ���¿����� return �ϸ� �ȵȴ� 
   					// �츮�� ����ϴ� time rate Interpolation�� 
   					// fling distance�� ����� ������ ���� ���� �����ϱ⵵ �ϱ� �����̴�    					
   					return;
   				}	       		
   			}            
       }
   		
       mPreviousFirstVisiblePosition = currFirstVisiblePosition;
       mPreviousFirstChildBottom = currFirstChildBottom;
       
       if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
    	       	   
    	   if (mListViewState == ListViewState.Run_FlingState) {    		   
    		   postFlingComputationRunnable();
    	   }
    	   
    	   updateVisibleCurrentMonthInNMode(); 
       }  
       else if (mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {
    	   
    	   if (mExpandEventListViewState == ExpandEventListViewState.ListViewFirstWeekTopAdjustmentScrollStateInEventsViewMode) {
        	   mExpandEventListViewState = ExpandEventListViewState.Run; // �� �ڵ尡 ListView�� ���?����� �Ѵٰ� �����Ѵ�
               															 // expand mode�� �����ϰ� ���� �Ʒ� notifyDataSetChanged ȣ��� ���� getView�� ó�� ȣ��� ��
               															 // mPsudoTargetMonthViewAtEventListEntrance�� GONE ���·�
    																     // mListView�� VISIBLE ���·� ����� ������
    																     // �� �ڵ尡 ������� �ʴ´ٸ� mListView�� ����ؼ� INVISIBLE ���·� ���� �ְ� �����ν� 
    																     // ��ġ mListView�� ���?�Ȱ�ó�� ��������
        	   
        	   if (mPsudoTargetMonthViewAtEventListEntrance.getVisibility() != View.GONE) {
    				mPsudoTargetMonthViewAtEventListEntrance.setVisibility(View.GONE); 				
    			}
    			if (mListView.getVisibility() != View.VISIBLE) {    				
    				mListView.setVisibility(View.VISIBLE);   				
    			}
    			
        	   //CalendarController controller = CalendarController.getInstance(mContext);
        	   mController.sendEvent(this, EventType.EXPAND_EVENT_VIEW_OK, null, null, -1, ViewType.MONTH);  

        	   mAdapter.notifyDataSetChanged();  
           }
           
    	   if (mExpandEventListViewState == ExpandEventListViewState.ListViewFirstWeekTopAdjustmentScrollStateForNormalMode) {
        	       	   
        	   mExpandEventListViewState = ExpandEventListViewState.NoneState;  
        	   mCurrentMonthViewMode = SettingsFragment.NORMAL_MONTH_VIEW_MODE;
        	   
        	   mPsudoTargetMonthViewAtEventListExit.setVisibility(View.GONE);
        	   
        	   if (mPsudoTargetMonthViewAtEventListExit.mLaunchNextMonthRegionContainer) {
        		   mPsudoNextMonthRegionContainer.setVisibility(View.GONE);
        		   mPsudoNextMonthRegionContainer.removeAllViews();		
        	   }
    		
        	   if (mListView.getVisibility() != View.VISIBLE) {    				
        		   mListView.setVisibility(View.VISIBLE);   				
        	   }  	   		
        	   	        			
        	   //CalendarController controller = CalendarController.getInstance(mContext);
        	   mController.sendEvent(this, EventType.COLLAPSE_EVENT_VIEW_OK, null, null, -1, ViewType.MONTH);     
    	    
        	   mAdapter.notifyDataSetChanged();  
           }
       }    	   
    } 
   
   
    public class ScrollStateRunnable implements Runnable {
       private int mNewState;

       /**
        * Sets up the runnable with a short delay in case the scroll state
        * immediately changes again.
        *
        * @param view The list view that changed state
        * @param scrollState The new state it changed to
        */
       public void doScrollStateChange(AbsListView view, int scrollState) {
    	         	
           mHandler.removeCallbacks(this);
           mNewState = scrollState;
           
           mHandler.post(this);
       }

       public void run() {
    	   
           mCurrentScrollState = mNewState;
                      
           if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {	
        	   
	            if (mNewState == OnScrollListener.SCROLL_STATE_IDLE
	                    && mPreviousScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
	                mPreviousScrollState = mNewState;           
	                
	            } else {
	                mPreviousScrollState = mNewState;
	            }
           }
           else { //mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE
        	   if (mExpandEventListViewState == ExpandEventListViewState.Run) {
        		   
	           		if (mNewState == OnScrollListener.SCROLL_STATE_IDLE
	   	                    && mPreviousScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
	   	                
	   	                if (mPreviousScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
	   	                	adjustmentMonthListTopItemPositionAfterScrollingInEEMode();	   	                	
	   	                }
	   	                else if (mPreviousScrollState == OnScrollListener.SCROLL_STATE_FLING) {	   	                	
	   	                }
	   	                
	   	                mPreviousScrollState = mNewState;
	   	            }
	           		
	           		else {
	   	                mPreviousScrollState = mNewState;
	   	            }
	           		
           	   }
           }            
       }
    }

    
    public void adjustmentMonthListTopItemPositionAfterScrollingInEEMode() {    	
    	    	
    	int monthIndicatorHeight = mListView.getEEMMonthIndicatorHeight();
   		MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);		
		//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();		
		
		//long mills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
		mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
		Calendar targetMonthCalendar = GregorianCalendar.getInstance(mTimeZone);
		targetMonthCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
		targetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
		
		int delta = 0;
		if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {            		
			/*int maximumMonthDays = targetMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
			int remainingMonthDays = maximumMonthDays - targetMonthCalendar.get(Calendar.DAY_OF_MONTH);
			int remainingWeeks = remainingMonthDays / 7;        	
			int lastWeekDays = remainingMonthDays % 7;       	
			if (lastWeekDays != 0) { // ��κ��� �̷� ���̴�
				++remainingWeeks;            	
			}*/			
			int remainingWeeks = ETime.getRemainingWeeks(targetMonthCalendar, false);
			
			targetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(targetMonthCalendar);
   		
			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
			int visibleMonthHeight = child.getBottom();		
           
			visibleMonthHeight = visibleMonthHeight + (remainingWeeks * mListView.getEEMNormalWeekHeight());
           
			float mFV = (float)monthHeight;
			float vFV = (float)visibleMonthHeight;
       	
			float visibleMonthHeightRatio = vFV / mFV;   	
       	
			if (visibleMonthHeightRatio >= 0.5f) {
				// stop�� week�� month�� top�� ��ġ�Ѵ�
				int DownwardMove = monthHeight - visibleMonthHeight;   
				delta = -DownwardMove + monthIndicatorHeight;
			}
			else {
				// next month�� week�� top�� ��ġ�Ѵ�                		
				int UpwardMove = visibleMonthHeight;
				delta = UpwardMove + monthIndicatorHeight;
       		
				targetMonthCalendar.add(Calendar.MONTH, 1);				
			}                    
		}
		else {
			targetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
   		
			if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {    			
				//int lastWeekHeight = mListView.getEEVMLastWeekHeight(); // lastWeekHeight�� �ٷ� previous month�� last week�� �ǹ��Ѵ�
				int lastWeekHeight = mListView.getEEMNormalWeekHeight();
				int topFromListView = Math.abs(child.getTop());
       		
				if (topFromListView > lastWeekHeight) {
					int DownwardMove = topFromListView - lastWeekHeight;
					delta = -DownwardMove + monthIndicatorHeight;///////////////////////////////////////////
				}else if (lastWeekHeight > topFromListView) {
					int UpwardMove = lastWeekHeight - topFromListView;
					delta = UpwardMove + monthIndicatorHeight;///////////////////////////////////////////
				}else {
					// adjustment�� �� �ʿ䰡 ���� ���� :�Ƹ����� �κ��̴�        			
				}                		
			}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {         		
				int DownwardMove = Math.abs(child.getTop());
				delta = -DownwardMove + monthIndicatorHeight;///////////////////////////////////////////
       		
			}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {        		
				int UpwardMove = mListView.getEEMNormalWeekHeight() - Math.abs(child.getTop()); 
				delta = UpwardMove + monthIndicatorHeight;///////////////////////////////////////////        		
				targetMonthCalendar.add(Calendar.MONTH, 1);
			}
		}
   	
		if (delta !=0) {
   		
			//mMonthListViewItemMoveDelta = delta;
			// event list view�� height ũ�⸦ �����ؾ� �Ѵٸ� �ؾ� �Ѵ�!!!   		
			mCurrentEventListHeight = mEventsListView.getHeight();   		
			int weekNumbersOfTargetMonth = ETime.getWeekNumbersOfMonth(targetMonthCalendar);
			int targetMonthHeight = weekNumbersOfTargetMonth * mListView.getEEMNormalWeekHeight();
			mTargetEventListHeight = mListView.getOriginalListViewHeight() - targetMonthHeight;
   		
			boolean mustScaleEventListViewHeight = false;
			if (mCurrentEventListHeight != mTargetEventListHeight) {   	
				mustScaleEventListViewHeight = true;
				ScaleEventListViewHeight();    			
			}  		
			
			mMonthListViewAnimDuration = calculateDuration(Math.abs(delta), mAnticipationMonthListViewHeightInEPMode, DEFAULT_VELOCITY);		
			
			boolean updateMonthDisplayed = false;
			Calendar targetMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mCurrentMonthTimeDisplayed.timezone);
			targetMonthTime.setTimeInMillis(targetMonthCalendar.getTimeInMillis());		
			ObjectAnimator fadingIn = null;
			ObjectAnimator fadingOut = null;
			if (ETime.compare(mCurrentMonthTimeDisplayed, targetMonthTime) != 0) {
				mTempTime.setTimeInMillis(System.currentTimeMillis());
				
				updateMonthDisplayed = true;
				
				fadingOut = hasMonthListViewPrvTargetMonthWeekDay(mAdapter.getSelectedDayInEEMode(), mMonthListViewAnimDuration);
				
				fadingIn = hasMonthListViewTargetMonthWeekDay(targetMonthCalendar, mMonthListViewAnimDuration);
				if (fadingIn == null) {				    	
			    	
					if (targetMonthTime.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && targetMonthTime.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {						
						mAdapter.setSelectedDayAlphaAnimUnderNoExistenceInEEMode(mTempTime.getTimeInMillis());	
					}
					else {						
						mAdapter.setSelectedDayAlphaAnimUnderNoExistenceInEEMode(targetMonthTime.getTimeInMillis());	
					}					
				}

				int targetJualianDayOfEventListView;				
				if (targetMonthTime.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && targetMonthTime.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {						
					targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
				}
				else {						
					targetJualianDayOfEventListView = ETime.getJulianDay(targetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
				}	
				
				mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);
			}					
			
			// smoothScrollBy�� SCROLL_STATE_FLING�� �߻���Ű�°�?
			// :�߻���Ų��	
			//  ������ ���� adjustmentMonthListTopItemPositionAfterScrollingInEEMode���� �����ϱ⵵ ����
			//  SCROLL_STATE_FLING�� �߻��Ѵٴ� ���̴�
			mListView.smoothScrollBy(delta, (int) mMonthListViewAnimDuration);		
			
			if (mustScaleEventListViewHeight) {			
				mScaleValueAnimator.start();
			}
			
			if (updateMonthDisplayed) {
				if (fadingOut != null)
					fadingOut.start();
				
				if (fadingIn != null)
					fadingIn.start();	
			
				setMonthDisplayed(targetMonthTime);
				
				// event load�� �����ؾ� �Ѵ�
				// �ƴϴ� �̷��� �ȵȴ�...
				// �̹� target month�� �˱⿡ NMode�� ����� ��ȿ�����̴�
				//mHandler.post(mUpdateLoaderInEEMode);
				updateLoaderInEEMode(ETime.getJulianDay(targetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
				
			}
   		}    	
    }    
    
    // target month�� month listview�� child item���� �����ϴ°� �ƴѰ��� �Ǻ��ؾ� �Ѵ�
    // 1.�������� �ʴ´ٸ� animateChangeTargetMonthInEEMode�� �̿��ؼ� MonthAdapter���� MonthWeekView�� ������ �� �ִϸ��̼� ȿ���� �����ϸ� �ȴ�
    // 2.�����Ѵٸ�, �ش� MonthWeekView�� ã�ƾ� �Ѵ�        
    public ObjectAnimator hasMonthListViewTargetMonthWeekDay(Calendar targetMonthCalendar, long animDuration) {
    	
    	int weekCounts = mListView.getChildCount();
    	int firstWeek = mListView.getFirstVisiblePosition();
    	int lastWeek = mListView.getLastVisiblePosition();
    	int firstWeekFirstJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(firstWeek);//Utils.getJulianMondayFromWeeksSinceEpoch(firstWeek);    	
    	int lastWeekLastJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(lastWeek) + 6;//Utils.getJulianMondayFromWeeksSinceEpoch(lastWeek) + 6;
    	
    	Calendar targetMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mCurrentMonthTimeDisplayed.timezone);
    	targetMonthTime.setTimeInMillis(targetMonthCalendar.getTimeInMillis());
		
    	// ���⼭ �츮�� target month�� today month���� �ƴ����� Ȯ���ؾ� �Ѵ�!!!    	
    	mTempTime.setTimeInMillis(System.currentTimeMillis());
    	
    	int targetMonthJulianToday = 0;
		if (targetMonthTime.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && targetMonthTime.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {
			targetMonthJulianToday = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		}
		else {
			targetMonthJulianToday = ETime.getJulianDay(targetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		}		
    	  
		long targetMonthJulianTodayMills = ETime.getMillisFromJulianDay(targetMonthJulianToday, mTimeZone, mFirstDayOfWeek);
		targetMonthTime.setTimeInMillis(targetMonthJulianTodayMills);//targetMonthTime.setJulianDay(targetMonthJulianToday);
		
    	if (targetMonthJulianToday >= firstWeekFirstJulianday && targetMonthJulianToday <= lastWeekLastJulianday) {
    		// adapter�� selectdDay�� update �� ��� �Ѵ�
    		////////////////////////////////////////////////////////////////////////
    		mAdapter.setSelectedDayInEEMode(targetMonthTime.getTimeInMillis());
    		////////////////////////////////////////////////////////////////////////
    		
    		ObjectAnimator animator = null;
    		// ���� ã�ƾ� �Ѵ�...�ش� �ָ�...
    		for (int i=0; i<weekCounts; i++) {
    			MonthWeekView weekView = (MonthWeekView) mListView.getChildAt(i);
    			int firstJulianDay = ETime.getJulianDay(weekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    			int lastJulianDay = firstJulianDay + 6;
    			if (targetMonthJulianToday >= firstJulianDay && targetMonthJulianToday <= lastJulianDay) {
    				// bingo     				
    				animator = weekView.setSelectedDayAlphaAnimUnderExistenceInEEMode(targetMonthTime.get(Calendar.DAY_OF_WEEK), animDuration);    	
    				break;
    			}
    		}
    		
    		return animator;
    	}
    	else 
    		return null;
    }
    
    public ObjectAnimator hasMonthListViewPrvTargetMonthWeekDay(Calendar prvSelectedDayTime, long animDuration) {
    	
    	int weekCounts = mListView.getChildCount();
    	int firstWeek = mListView.getFirstVisiblePosition();
    	int lastWeek = mListView.getLastVisiblePosition();
    	int firstWeekFirstJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(firstWeek);//Utils.getJulianMondayFromWeeksSinceEpoch(firstWeek);    	
    	int lastWeekLastJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(lastWeek) + 6;//Utils.getJulianMondayFromWeeksSinceEpoch(lastWeek) + 6;
    	    	
    	int prvSelectedJulianDay = ETime.getJulianDay(prvSelectedDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
    	if (prvSelectedJulianDay >= firstWeekFirstJulianday && prvSelectedJulianDay <= lastWeekLastJulianday) {  		
    		
    		ObjectAnimator animator = null;
    		
    		for (int i=0; i<weekCounts; i++) {
    			MonthWeekView weekView = (MonthWeekView) mListView.getChildAt(i);
    			int firstJulianDay = ETime.getJulianDay(weekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    			int lastJulianDay = firstJulianDay + 6;
    			if (prvSelectedJulianDay >= firstJulianDay && prvSelectedJulianDay <= lastJulianDay) {
    				// bingo     				
    				animator = weekView.setPrvSelectedDayAlphaAnimUnderExistenceInEEMode(animDuration);    	
    				break;
    			}
    		}
    		
    		return animator;
    	}
    	else 
    		return null;
    } 
    
    

    private void updateVisibleCurrentMonthInNMode() {    
   	
    	MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);
    	if (child == null) {
    		return;
    	}

       // Figure out where we are
       int offset = child.getBottom() < WEEK_MIN_VISIBLE_HEIGHT ? 1 : 0;
       
       // hysteresis : ��ü�� ���Ż��¿� ������°� ����
       // Use some hysteresis for checking which month to highlight. This
       // causes the month to transition when two full weeks of a month are
       // visible.
       child = (MonthWeekView) mListView.getChildAt(SCROLL_HYST_WEEKS + offset);

       if (child == null) {
           return;
       }

       // Find out which month we're moving into
       int month;
       if (mIsScrollingUp) {    	   
           	month = child.getFirstMonth();            
       } else {
       		month = child.getLastMonth();
       }

       // �Ʒ��� ������...
       // �ʹ� ���� ��ũ�� �ӵ��� ���� �⵵�� ������Ʈ �� ��Ȳ������ �⵵�� Ʋ���� ���� ���� ��찡 �߻��� �� �ִ�
       // :������ ������ ���� ��ũ�� �ӵ��� ���� �̵� ������ �ִ� ���� ���� �������� �ʾұ� �����̴�
       //  ->����ν�� �̷� ���� �߻����� �ʴ´�...
       //    ����ߴ� month�� mCurrentMonthDisplayed�� ������ ũ�� �������� ���� ������ onScroll �Լ��� ����� �߻��Ѵ�      
       int monthDiff;
       if (mCurrentMonthDisplayed == 11 && month == 0) {
           	monthDiff = 1;
       } else if (mCurrentMonthDisplayed == 0 && month == 11) {
           	monthDiff = -1;
       } else {
           	monthDiff = month - mCurrentMonthDisplayed;
       }

       // Only switch months if we're scrolling away from the currently
       // selected month
       if (monthDiff != 0) {      	   
       		
           Calendar curTimeCalendar = (Calendar) GregorianCalendar.getInstance(mTimeZone);       	   
       	   curTimeCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());
       	   curTimeCalendar.add(Calendar.MONTH, monthDiff);
       	   
           mTempTime.setTimeInMillis(curTimeCalendar.getTimeInMillis());           
           
           setMonthDisplayed(mTempTime);  ////////////////////////////////////////////////////////////////////////          
       }        
   }  

   public void setMonthDisplayed(Calendar time) {
   		mCurrentMonthDisplayed = time.get(Calendar.MONTH);   		
   		
   		mCurrentMonthTimeDisplayed.setTimeInMillis(time.getTimeInMillis());
   		   		
   		mAdapter.setCurrentMonthTimeDisplayed(mCurrentMonthTimeDisplayed);   		
       
   		//CalendarController controller = CalendarController.getInstance(mActivity);  		
       
   		long newTime = mCurrentMonthTimeDisplayed.getTimeInMillis();
   		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   		// �߸��Ǿ��� ���ľ� �Ѵ�
   		// ���� newTime�� offset�� ������ �ʿ䰡 ����
   		// :���� 
   		if (newTime != mController.getTime()/* && mUserScrolled*/) {
   			//long offset = DateUtils.WEEK_IN_MILLIS * mNumWeeks / 3;
   			mController.setTime(newTime);
   			
   			mCurrentMonthFirstJulianDay = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
   	       
   			if (INFO) Log.i(TAG, "setMonthDisplayed:" + ETime.format2445(mCurrentMonthTimeDisplayed).toString());
   			mController.sendEvent(this, EventType.UPDATE_TITLE, time, time, time, -1,
   	               ViewType.CURRENT, DateUtils.FORMAT_SHOW_YEAR, null, null);
   		}
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
          		
   		
       
   }
    
   public static StringBuilder mStringBuilder = new StringBuilder(50);
   // TODO recreate formatter when locale changes
   public static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
   // callers 
   // -MonthFragment Constructor
   // -onCreate AT the fragment is being re-created from a previous saved state
   // -onResume
   // -handleEvent : centralActivity�� onCreate����
   //                initFragments�� ȣ���� initFragments�� ������ ���� �ڵ带 ȣ����
   //                mController.sendEvent(this, EventType.GO_TO, t, null, -1, viewType);
   //                ������ MonthFragemnt�� the resumed state�� �ƴϱ� ������ goTo�� ����� ������� �ʴ´�
   // -DataSetObserver mObserver     
    public boolean goTo(boolean forceScroll, boolean forceCurrentMonthTime) {
        // If this view isn't returned yet we won't be able to load the lists
        // current position, so return after setting the selected day.
    	
    	// ������ goodbyeMonthFragment���� ���� EventType.GO_TO�� ����� ���´�
    	// goodbyeMonthFragment���� ���� EventType.GO_TO�� mPreviousViewType�� MONTH�� �����ϰ� mViewType�� DAY�� �����Ͽ��µ�,
    	// �׷��� setMonthDisplayed�� ���� EventType.UPDATE_TITLE�� 
    	// mPreviousViewType�� DAY�� �����ϴ� ������ �߻��Ѵ�...
    	// :�׷��� DayFragment���� mPreviousViewType�� DAY�� �����Ǵ� ������ �߻��ȴ�
    	
        if (!isResumed()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "We're not visible yet");
            }
            return false;
        }		
        
        long millis = mCurrentMonthTimeDisplayed.getTimeInMillis();
        
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_SHOW_TIME | ETime.FORMAT_24HOUR;
        
        String formatString;
        mStringBuilder.setLength(0);
        formatString = ETime.formatDateTimeRange(mContext, mFormatter, millis, millis, flags, mTzId).toString();          
        
        // Get the week we're going to
        // TODO push Util function into Calendar public api.
        //int targetMonthJulianDay = ETime.getJulianDay(millis, mGmtoff);    
        //int positionX = ETime.getWeeksSinceEcalendarEpochFromJulianDay(targetMonthJulianDay, mFirstDayOfWeek); 
        //if (INFO) Log.i(TAG, "goTo, MonthTime=" + formatString.toString() + ", tMJD=" + String.valueOf(targetMonthJulianDay) + ", posX=" + String.valueOf(positionX));
        
        int position = ETime.getWeeksSinceEcalendarEpochFromMillis(millis, mTimeZone, mFirstDayOfWeek);        
        int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);
        if (INFO) Log.i(TAG, "goTo, pos=" + String.valueOf(position) + ", julianMonday=" + String.valueOf(julianMonday));
        Calendar cal = GregorianCalendar.getInstance(mTimeZone);
        cal.setFirstDayOfWeek(mFirstDayOfWeek);
        cal.setTimeInMillis(ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek));
        ETime.adjustStartDayInWeek(cal);
        
        formatString = "";
        mStringBuilder.setLength(0);
        formatString = ETime.formatDateTimeRange(mContext, mFormatter, cal.getTimeInMillis(), cal.getTimeInMillis(), flags, mTzId).toString();
        if (INFO) Log.i(TAG, "goTo, first day in Week = " + formatString.toString());
        
        
        // position�� ���� week�� � ���������� Ȯ���ؾ� �Ѵ�
        View child;
        int i = 0;
        int top = 0;
        // Find a child that's completely in the view
        do {
            child = mListView.getChildAt(i++);
            if (child == null) { // onResume �ÿ� ���� ������ child view���� ���� ������
            	                 // �ᱹ firstPosition�� 0, lastPosition�� 5�� �����ȴ�
                break;
            }
            top = child.getTop();
            
        } while (top < 0);

        // Compute the first and last position visible
        int firstPosition;
        if (child != null) {
            firstPosition = mListView.getPositionForView(child);
        } else {
            firstPosition = 0;
        }
        
        int lastPosition = firstPosition + mNumWeeks - 1;
        if (top > BOTTOM_BUFFER) {
            lastPosition--;                             
        }
                
        setMonthDisplayed(mCurrentMonthTimeDisplayed);
        
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it        
        // onResume���� ȣ���ߴµ� �Ʒ� ���ǹ��� true�� �Ǵ� ������
        // onResume ���¿����� mListView�� child���� �������� �ʱ� �빮��
        // firstPosition ���� 0�� �ȴ� position�� 0���� ���� ���� ������ ���� �Ұ����ϹǷ�,
        // position < firstPosition �� ���ǿ� ���յȴ�
        // �̰��� �ᱹ onScrollStateChanged(mListView, OnScrollListener.SCROLL_STATE_IDLE); �� ȣ���ϰ� �Ǿ        
        // mHandler.postDelayed(mUpdateLoader, LOADER_DELAY); ȣ���
        // :�̴� mUpdateLoader�� �����ϰ� �Ǵ� ����� ����
        if (position < firstPosition || 
        		position > lastPosition || 
        		forceScroll) {
            
			int listTopOffset = LIST_TOP_OFFSET;			
			julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//Time.getJulianMondayFromWeeksSinceEpoch(position);
			Calendar timeOfFirstDayOfThisWeek = GregorianCalendar.getInstance(mTimeZone);//new Time(mCurrentMonthTimeDisplayed.timezone);
			timeOfFirstDayOfThisWeek.setFirstDayOfWeek(mFirstDayOfWeek);
			long julianMondayMills = ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek);
			timeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMills);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);	
			ETime.adjustStartDayInWeek(timeOfFirstDayOfThisWeek);
						
			Calendar timeOfLastDayOfThisWeek = GregorianCalendar.getInstance(mTimeZone);
			timeOfLastDayOfThisWeek.setTimeInMillis(timeOfFirstDayOfThisWeek.getTimeInMillis());			   
			timeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);//timeOfLastDayOfThisWeek.monthDay += 6;			
			
			if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {	
				
				if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {					
					listTopOffset = (int) mMonthListViewNormalWeekHeight;						
				}	
												
				listTopOffset = -listTopOffset;
				
			}
			else {								
				if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {					
					listTopOffset = (int) (mMonthListViewNormalWeekHeightInEPMode + mMonthListViewMonthIndicatorHeightInEPMode);						
				}	
				else {
					listTopOffset = (int) mMonthListViewMonthIndicatorHeightInEPMode;	
				}
				
				listTopOffset = -listTopOffset;
			}		
			
            
            mListView.setSelectionFromTop(position, listTopOffset);
                        
            // event���� load�ϱ� ���� ȣ���ε�...
            // :�� �Žñ� �ϴ�...
            onScrollStateChanged(mListView, OnScrollListener.SCROLL_STATE_IDLE); 
            
        }
        else {
        	// else�� �� �� �ִ� ��Ȳ�� � ��Ȳ�ΰ�???
        }
        
        return false;
    }
    
    int mTopOfTodayRedCircleInsideWeekView;    
    int mSelectedCircleDrawableSize;    
    public void calcTodayRedCircleDimen() {
    	int offsetY = 0;
    	
    	int dateTextSize = (int) (mAnticipationMonthListViewHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT);
    	
    	Paint monthNumPaint = new Paint();
    	monthNumPaint.setFakeBoldText(false);
    	monthNumPaint.setAntiAlias(true);
    	monthNumPaint.setTextSize(dateTextSize);        
        
    	monthNumPaint.setStyle(Style.FILL);        
    	monthNumPaint.setTextAlign(Align.CENTER);
    	monthNumPaint.setTypeface(Typeface.DEFAULT);  
        
    	int dateTextTopPadding = (int) (mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);    
    	int selectedCircleDrawableCenterY = 0;
    	int selectedCircleDrawableRadius = 0;
    	
    	
    	float radius = (mMonthListViewNormalWeekHeight / 2) / 2;
    	selectedCircleDrawableRadius = (int)radius;
    	mSelectedCircleDrawableSize = (int)(selectedCircleDrawableRadius * 2);
		
    	float baseLineY = dateTextTopPadding;
    	
    	float textAscent = monthNumPaint.ascent();
    	float textDescent = monthNumPaint.descent();
    	
    	float textTopY = baseLineY + textAscent; 	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    	
    	selectedCircleDrawableCenterY = (int) textCenterY;     	
    	
    	mTopOfTodayRedCircleInsideWeekView = selectedCircleDrawableCenterY - selectedCircleDrawableRadius;		
    }
    
    
    public void goToToday(boolean animate) {
    	    	
        // If this view isn't returned yet we won't be able to load the lists
        // current position, so return after setting the selected day.
        if (!isResumed()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "We're not visible yet");
            }
            return;
        }		
        
        long millis = mCurrentMonthTimeDisplayed.getTimeInMillis();       
        
        int itemViewCount = mListView.getChildCount();
        int lastVisibleWeekViewIndex = itemViewCount - 1;
        MonthWeekView firstVisibleWeekView = (MonthWeekView) mListView.getChildAt(0);         
        MonthWeekView lastVisibleWeekView = (MonthWeekView) mListView.getChildAt(lastVisibleWeekViewIndex);        
        
        int firstJulianDayOfFirstVisibleWeek = ETime.getJulianDay(firstVisibleWeekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//firstVisibleWeekView.getFirstJulianDay();
        int lastJulianDayOfLastVisibleWeek = ETime.getJulianDay(lastVisibleWeekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//lastVisibleWeekView.getLastJulianDay();        
        
        // �ϴ� handleEvent���� +/- 4���� �̳����� scrolling animation�� ����ϰ� �ִ�                
        int position = ETime.getWeeksSinceEcalendarEpochFromMillis(millis, mTimeZone, mFirstDayOfWeek);     
        
        Calendar todayTime = GregorianCalendar.getInstance(mTimeZone);
        todayTime.setTimeInMillis(System.currentTimeMillis());      
        int todayJulianDay = ETime.getJulianDay(todayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        
        // ���� if ������ EEMode������ �Ϻ��ϴ�
        // :EEMode���� Month ListView�� �׻� Month�� ù �ֿ� ���ĵǾ� �ֱ� �����̴�
        // �׷��� NMode������ 1�� ���͸� ���ҹۿ� ���� ���Ѵ�
        // :�׷��Ƿ� else �������� �ٽ� today red circle�� ��ġ�� Ȯ���ؾ� �Ѵ�
        // todayJulianDay < firstJulianDayOfFirstVisibleWeek : �����̰� ���� ���÷��� �ǰ� �ִ� ù��° ���� ù ������ ���� �� -> firstJulianDayOfFirstVisibleWeek�� ������ ���� �̷���
        // todayJulianDay > lastJulianDayOfLastVisibleWeek : �����̰� ���� ���÷��� �ǰ� �ִ� ������ ���� ������ ������ ���� �� -> lastJulianDayOfLastVisibleWeek�� ������ ���� ������
        // :�� �� �� ���� OR�� ���� ���� ���÷��̵ǰ� �ִ� Week�鿡 �����̰� ���ٴ� ����
        if (todayJulianDay < firstJulianDayOfFirstVisibleWeek || todayJulianDay > lastJulianDayOfLastVisibleWeek) {
        	
            if (animate) {
            	
            	if (todayJulianDay < firstJulianDayOfFirstVisibleWeek) {
            		// downward fling�� �߻��ؾ� �Ѵ�!!!
                	mGoToTodayDirection = DOWNWARD_GO_TO_TODAY;                	
                }
                else {
                	// upward fling�� �߻��ؾ� �Ѵ�!!!
                	mGoToTodayDirection = UPWARD_GO_TO_TODAY;
                }
            	
            	if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {	
            		mListView.postOnAnimation(goToTodayflingRunInNMode);
            	}
            	else {
            		mListView.postOnAnimation(goToTodayflingRunInEEMode);
            		setMonthDisplayed(mCurrentMonthTimeDisplayed);       
            	}           		
            	
                return;
            } else {
            	
				//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				int listTopOffset = LIST_TOP_OFFSET;			
				int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//Time.getJulianMondayFromWeeksSinceEpoch(position);
				Calendar timeOfFirstDayOfThisWeek = GregorianCalendar.getInstance(mTimeZone);//new Time(mCurrentMonthTimeDisplayed.timezone);
				timeOfFirstDayOfThisWeek.setFirstDayOfWeek(mFirstDayOfWeek);
				long julianMondayMills = ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek);
				timeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMills);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);	
				ETime.adjustStartDayInWeek(timeOfFirstDayOfThisWeek);
				
				
				Calendar timeOfLastDayOfThisWeek = GregorianCalendar.getInstance(mTimeZone);
				timeOfLastDayOfThisWeek.setTimeInMillis(timeOfFirstDayOfThisWeek.getTimeInMillis());
				
				timeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);//timeOfLastDayOfThisWeek.monthDay += 6;
				
				
				if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {	
				
					if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {					
						listTopOffset = (int) mMonthListViewNormalWeekHeight;						
					}	
					
					listTopOffset = -listTopOffset;
				
					updateLoaderInNMode(ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
				}
				else {	
					
					if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {					
						listTopOffset = (int) (mMonthListViewNormalWeekHeightInEPMode + mMonthListViewMonthIndicatorHeightInEPMode);						
					}	
					else {
						listTopOffset = (int) mMonthListViewMonthIndicatorHeightInEPMode;	
					}
					
					listTopOffset = -listTopOffset;
					
					// 1. event listview�� height�� �����ؾ� �Ѵ�
		    		// 2. event item adapter�� ���� event��� �ε��ؾ� �Ѵ�
					
		    		mTempTime.setTimeInMillis(System.currentTimeMillis());
		    		int targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		    		///////////////////////////////////////////////////////////////////////////////////////
		    		mAdapter.setSelectedDayInEEMode(mTempTime.getTimeInMillis());
		    		///////////////////////////////////////////////////////////////////////////////////////		 
		    		
		        	Calendar targetMonthCalendar = GregorianCalendar.getInstance(mTempTime.getTimeZone());        
		        	targetMonthCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
		        	targetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());       
		        	targetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);  		    		
		    		
		    		mCurrentEventListHeight = mEventsListView.getHeight();   		
					int weekNumbersOfXXXMonth = ETime.getWeekNumbersOfMonth(targetMonthCalendar);
					int targetMonthHeight = weekNumbersOfXXXMonth * mListView.getEEMNormalWeekHeight();
					mTargetEventListHeight = mListView.getOriginalListViewHeight() - targetMonthHeight;
		   		
					boolean mustScaleEventListViewHeight = false;
					if (mCurrentEventListHeight != mTargetEventListHeight) {   	
						mustScaleEventListViewHeight = true;
						ScaleEventListViewHeight();    			
					}  			    		
		    		
		    		mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);
		    		if (mustScaleEventListViewHeight) {   	
						mScaleValueAnimator.start();
					}
		    		
		    		updateLoaderInEEMode(ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
				}			
				
				///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                mListView.setSelectionFromTop(position, listTopOffset);                
            }
        }
        else { // ���� ���÷��̵ǰ� �ִ� Week�鿡 �����̰� �ִٴ� ����
        	   // �׷���, ���� today�� 2015/06/29 �̰�
        	   // today�� ��ġ�� prv month week�� month listview�� top ���� ���� ��ġ�ؼ� ������ �ʴ´ٸ�,,,,
        	   // ����
        	   // Month ListView�� Top�� Bottom�� ������ ���� �� �ִ� Today Red Circle�� Ȯ���ؾ� �Ѵ�        	
        	if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {	
	        	for (int i=0; i<itemViewCount; i++) {
	        		MonthWeekView weekView = (MonthWeekView) mListView.getChildAt(i);     
	        		int firstJulianDay = ETime.getJulianDay(weekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//weekView.getFirstJulianDay();
	        		int lastJulianDay = firstJulianDay + 6;//weekView.getLastJulianDay();
	        		if (firstJulianDay <= todayJulianDay && todayJulianDay <= lastJulianDay) {
	        			if (INFO) Log.i(TAG, "goToToday:TODAY WEEK=" + String.valueOf(i));
	        			
	        			int todayRedCircleTop = 0;
	        			int todayRedCircleBottom = 0;
	        			int monthListViewTop = 0;
	        			int monthListViewBottom = mListView.getHeight();
	        			int weekViewTop = weekView.getTop();
	        			int weekPattern = weekView.getWeekPattern();
	        			if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
	        				// today�� prv Month Week�� ��ġ�� �ִ��� next Month Week�� ��ġ�� �ִ��� Ȯ���ؾ� �Ѵ�
	        				// ���� �� ���� Time.month ���� ���̴�
	        				int prvMonth = weekView.getFirstMonth();
	        				//int nextMonth = weekView.getLastMonth();
	        				
	        				if (todayTime.get(Calendar.MONTH) == prvMonth) {
	        					todayRedCircleTop = weekViewTop + mTopOfTodayRedCircleInsideWeekView;        					
	        				}
	        				else {
	        					todayRedCircleTop = weekViewTop + mListView.getNMNormalWeekHeight() + mListView.getNMMonthIndicatorHeight() + mTopOfTodayRedCircleInsideWeekView;
	        				}        				
	        			}
	        			else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
	        				todayRedCircleTop = weekViewTop + mListView.getNMMonthIndicatorHeight() + mTopOfTodayRedCircleInsideWeekView;
	        			}
	        			else {
	        				todayRedCircleTop = weekViewTop + mTopOfTodayRedCircleInsideWeekView;
	        			}
	        			
	        			todayRedCircleBottom = todayRedCircleTop + mSelectedCircleDrawableSize;
	        			
	        			if (todayRedCircleTop >= monthListViewTop && todayRedCircleBottom <= monthListViewBottom) {
	        				if (INFO) Log.i(TAG, "goToToday:GO TO DAY FRAGMENT");///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        				//CentralActivity activity = (CentralActivity) getActivity();	 
	        				mGoToExitAnim = true;
	        				ExitMonthViewEnterDayViewAnim test = new ExitMonthViewEnterDayViewAnim(mApp, mContext, this, mController, weekView,
	        						todayTime, mCurrentMonthTimeDisplayed.getTimeZone().getID());
	        			}
	        			else {
	        				if (INFO) Log.i(TAG, "goToToday:MUST GO TO TODAY MOVE");
	        				
        					if (todayRedCircleTop < monthListViewTop) {
        	            		// downward fling�� �߻��ؾ� �Ѵ�!!!, �׷��� event load�� ���� �ʴ´�
        	                	mGoToTodayDirection = DOWNWARD_GO_TO_TODAY_BUT_NOT_EVENT_LOAD;                	
        	                }
        	                else {
        	                	// upward fling�� �߻��ؾ� �Ѵ�!!!, �׷��� event load�� ���� �ʴ´�
        	                	mGoToTodayDirection = UPWARD_GO_TO_TODAY_BUT_NOT_EVENT_LOAD;
        	                }
        	            	
        					// �̷� ���� event load�� �� �ʿ䰡 ����
        	            	mListView.postOnAnimation(goToTodayflingRunInNMode);        	            	      				
	        			}
	        			
	        			break;
	        		}
	        	}
        	}
        	else {
        		// today�� �����ϴ� weekView�� ã�ƾ� �Ѵ�
        		// �����̴� �ִ� ȿ���� �ش�
        		// if, selected day�� 2015/06/5�̰�
        		//    today�� 2015/06/25�� ���...
        		//    ->�̶��� 2015/06/25�� selected day�� ����Ǿ�� �Ѵ�
        		Calendar selectedDayTime = GregorianCalendar.getInstance(mAdapter.getSelectedDayInEEMode().getTimeZone());
        		selectedDayTime.setTimeInMillis(mAdapter.getSelectedDayInEEMode().getTimeInMillis());
        		if (selectedDayTime.get(Calendar.YEAR) == todayTime.get(Calendar.YEAR) && selectedDayTime.get(Calendar.MONTH) == todayTime.get(Calendar.MONTH) && selectedDayTime.get(Calendar.DAY_OF_MONTH) == todayTime.get(Calendar.DAY_OF_MONTH)) {
        		//if (Time.compare(todayTime, mAdapter.getSelectedDayInEEMode()) == 0) { // todayTime�� ���� �ð��� ��/��/�ʱ��� ���Ǿ����� getSelectedDayInEEMode���� �����ϴ� �ð��� ��/��/�ϱ����� �����Ǿ� �ִ�
        			for (int i=0; i<itemViewCount; i++) {
    	        		MonthWeekView weekView = (MonthWeekView) mListView.getChildAt(i);     
    	        		int firstJulianDay = ETime.getJulianDay(weekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//weekView.getFirstJulianDay();
    	        		int lastJulianDay = firstJulianDay + 6;//weekView.getLastJulianDay();
    	        		if (firstJulianDay <= todayJulianDay && todayJulianDay <= lastJulianDay) {
    	        			weekView.setTodayRedCircleScaleAnimInEEMode(200);
    	        			break;
    	        		}
            		}
        		}
        		else {  // ���� today�� 2015/06/29 �̰� 2015/06/29�� ��ġ�� �ְ� prv month week���,
                		// today�� ��ġ�� prv month week�� month listview�� top ���� ���� ��ġ�ؼ� ������ �ʴ´ٸ�,,,,
        			    // :���� �����ѵ�...
        			if (INFO) Log.i(TAG, "goToToday:WHAT HELL????");
        			mAdapter.setSelectedDayInEEMode(todayTime.getTimeInMillis());
        			mAdapter.notifyDataSetChanged();
        			setEventsOfEventListView(ETime.getJulianDay(todayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
        			//mEventItemsAdapter.reloadEventsOfEventListView(Time.getJulianDay(todayTime.getTimeInMillis(), todayTime.gmtoff));
        		}        		
        	}        	
        	
        }       
        
        setMonthDisplayed(mCurrentMonthTimeDisplayed);         
        return;
    }
	
    // callers
    // -onAttach
    // -onResume
    public void setUpAdapter(boolean paused) {
    	    	
    	if (INFO) Log.i(TAG, "setUpAdapter ");        
        
        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(MonthAdapter.WEEK_PARAMS_NUM_WEEKS, mNumWeeks);        
        weekParams.put(MonthAdapter.WEEK_PARAMS_WEEK_START, mFirstDayOfWeek);
        
        weekParams.put(MonthAdapter.WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED,
                ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
        weekParams.put(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK, mDaysPerWeek);
        
        mTempTime.setTimeInMillis(System.currentTimeMillis());
        
        if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {        	        	
        	mListViewState = ListViewState.Run;        	
        }
        else {      	
        	
        	if (!paused) { // ó������ �ſ� 1���� �����Ǿ�� �Ѵ�
        		// �׷��� today�� Ȯ���� ���� �Ѵ�
        		// mCurrentMonthTimeDisplayed�� today�� month��� ��Ȳ�� �޶�����        		
        		if (mCurrentMonthTimeDisplayed.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && mCurrentMonthTimeDisplayed.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {
        			weekParams.put(MonthAdapter.WEEK_PARAMS_SELECTED_JULIAN_DAYS_IN_EEMODE, ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));                    
                }
        		else 
        			weekParams.put(MonthAdapter.WEEK_PARAMS_SELECTED_JULIAN_DAYS_IN_EEMODE, ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));    
        	}
        	else { // pause�� �� ���� resume �Ǹ� �Ʒ�ó�� �ؾ� ��
        		// MonthAdapter�� ��ü������ �����ϰ� �ִ� mSelectedDayInEEMode�� ���� ������ ���̹Ƿ�
        		// WEEK_PARAMS_SELECTED_JULIAN_DAYS_IN_EEMODE�� update �ؼ��� �ȵȴ�
        	}
        }
        
        if (mAdapter == null) { // onAttach�� ���� ����Ǵ� �� ����        	
            mAdapter = new MonthAdapter(mApp, mContext, (Activity)mActivity, weekParams);
            mAdapter.registerDataSetObserver(mObserver);
        } else {
        	// onResume�� ���� ����ȴ�
            mAdapter.updateParams(weekParams);            
        }         
        
        
        if(mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {
        	if (mEventItemsAdapter == null) { // onAttach�� ���� ����Ǵ� �� ����              		
        		mEventItemsAdapter = new EventItemsAdapter(getActivity(), mFirstDayOfWeek, mAnticipationMonthListViewWidth, (int)mMonthListViewNormalWeekHeightInEPMode);
        		mEventItemsAdapter.registerDataSetObserver(mEventItemAdapterObserver);
        		mEventItemsAdapter.setListView(this, mEventsListView);           		
        	}
        	else {  // onResume�� ���� ����ȴ�          		
        		// ���⼭ setEventsOfEventListView�� ȣ���ϴ� ���� �� �̻� �ǹ̰� ���� 
        		//setEventsOfEventListView(Time.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mCurrentMonthTimeDisplayed.gmtoff));   
        		int targetJualianDayOfEventListView;				
				if (mCurrentMonthTimeDisplayed.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && mCurrentMonthTimeDisplayed.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {						
					targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
				}
				else {						
					targetJualianDayOfEventListView = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
				}	
							
				boolean forceReload = true;
				if ( mPreviousViewType == ViewType.YEAR && !mActivity.getWaitingProcessEventsChangedFlag()) {        			
        			
        			ArrayList<Event> events = mApp.getEvents();       			
        			
        			int firstJulianDay = mApp.getFirstLoadedJulianDay();
        			int lastJulianDay = mApp.getLastLoadedJulianDay();
        			int numDays = lastJulianDay - firstJulianDay + 1;        			
        			
        	        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        	        for (int i = 0; i < numDays; i++) {
        	            eventDayList.add(new ArrayList<Event>());
        	        }

        	        if (events != null) {
        	        	if ( events.size() != 0) {        	        
	        	        	// Compute the new set of days with events
		        	        for (Event event : events) {
		        	            int startDay = event.startDay - firstJulianDay;
		        	            int endDay = event.endDay - firstJulianDay + 1;
		        	            if (startDay < numDays || endDay >= 0) {
		        	                if (startDay < 0) {
		        	                    startDay = 0;
		        	                }
		        	                if (startDay > numDays) {
		        	                    continue;
		        	                }
		        	                if (endDay < 0) {
		        	                    continue;
		        	                }
		        	                if (endDay > numDays) {
		        	                    endDay = numDays;
		        	                }
		        	                for (int j = startDay; j < endDay; j++) {
		        	                    eventDayList.get(j).add(event);
		        	                }
		        	            }
		        	        }
		        	        
		        	        if (eventDayList.size() != 0) {
		        	        	forceReload = false;
		            	        int index = targetJualianDayOfEventListView - firstJulianDay;
		            	        ArrayList<Event> targetDayEventList = eventDayList.get(index);
		            	        
		            	        mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView, targetDayEventList);     
		        	        }
        	        	}
        	            
        	        }        	               	        
        		}
				
				if (forceReload)
        		{
        			mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);        			
            		
                	//mEventItemsAdapter.notifyDataSetChanged();
        		}
				
				mEventItemsAdapter.notifyDataSetChanged();
        	}        	
        }       
        
    }   
    
    
    public void setUpListView() {
    	if (INFO) Log.i(TAG, "setUpListView ");
    	
        mListView = (MonthListView)getListView();        
        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        mListView.setMonthListViewDimension(mAnticipationMonthListViewWidth, mAnticipationMonthListViewHeight);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////        
        mListView.setListFragment(this);
        
        // Transparent background on scroll
        mListView.setCacheColorHint(0);
        // No dividers
        mListView.setDivider(null);
        // Items are clickable
        mListView.setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
        mListView.setFastScrollEnabled(/*true*/false); // �����ʿ� �н�Ʈ ��ũ�ѹٰ� �����ȴ� : �н�Ʈ ��ũ�ѿ� ���� ���� ������ �𸥴�
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setOnScrollListener(this);
        mListView.setFadingEdgeLength(0);
        // Make the scrolling behavior nicer
        mListView.setFriction(ViewConfiguration.getScrollFriction() * mFriction);
                
        if(mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {
        	mListView.setExpandEventViewMode(true);	    		    	
	    	mListView.setExpandEventViewModeHeight((int)mAnticipationMonthListViewHeightInEPMode);
	    	
	    	RelativeLayout.LayoutParams monthListViewParamsInEPMode = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int)mAnticipationMonthListViewHeightInEPMode); 
	    	monthListViewParamsInEPMode.addRule(RelativeLayout.BELOW, mCalendarViewsSecondaryActionBarId);    	
	    	mListView.setLayoutParams(monthListViewParamsInEPMode);     	
	    	mListView.setDrawSelectorOnTop(false);
	    	
	    	mEventsListView.setAdapter(mEventItemsAdapter); 	    	
	    	mEventsListView.setCacheColorHint(0);	        
	    	mEventsListView.setDivider(null);	        
	    	mEventsListView.setItemsCanFocus(true);	        
	    	mEventsListView.setFastScrollEnabled(false); 
	    	mEventsListView.setVerticalScrollBarEnabled(false);	    	
	    	mEventsListView.setFadingEdgeLength(0);	        
	    	mEventsListView.setFriction(ViewConfiguration.getScrollFriction() * mFriction);  	    	
	    	mEventsListView.setVisibility(View.VISIBLE);
	    	
	    	Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
			TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek); 			
			TempCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());
			TempCalendar.set(Calendar.DAY_OF_MONTH, 1); // �� ����� �Ѵ� getWeekNumbersOfMonth������ �ش� Ķ���� ��ü�� DAY_OF_MONTH�� 1�� �����Ǿ� �ִٴ� �����Ͽ� ����ϱ� ����
						
			int parentViewHeight = mAnticipationMonthViewLayoutHeight;
			int secondaryUpperActionBarHeight = mDayHeaderHeight; 
			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
			int monthListViewVisualableRegionHeight = mListView.getEEMNormalWeekHeight() * weekNumbersOfMonth;
			int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);	    	
			int height = parentViewHeight - (secondaryUpperActionBarHeight + monthListViewVisualableRegionHeight + lowerActionBarHeight);
			
			RelativeLayout.LayoutParams eventListViewParams = (RelativeLayout.LayoutParams) mEventsListView.getLayoutParams();
			eventListViewParams.height = height;	    	
	    	mEventsListView.setLayoutParams(eventListViewParams);   		    	
	    				
			mExpandEventListViewState = ExpandEventListViewState.Run;
        }
        else {
        	RelativeLayout.LayoutParams monthListViewParams = (RelativeLayout.LayoutParams) mListView.getLayoutParams();
        	monthListViewParams.height = mAnticipationMonthListViewHeight;
        	mListView.setLayoutParams(monthListViewParams);
    		mListView.setBackgroundColor(getResources().getColor(R.color.month_bgcolor)); 
    	}        
    }
    
    public int expandEventsListView() {    
    	int eventListViewHeight = 0;
    	if (mEventItemsAdapter != null)
			mEventItemsAdapter.closeEventCursors(); // cursor�� close ���� ������,
    	                                            // :could not allocate cursorwindow due to error 12 �߻��Ѵ�
    	
		mEventItemsAdapter = new EventItemsAdapter(getActivity(), mFirstDayOfWeek, mListView.getWidth(), mListView.getEEMNormalWeekHeight());
		mEventItemsAdapter.registerDataSetObserver(mEventItemAdapterObserver);
		mEventItemsAdapter.setListView(this, mEventsListView);   	
    	
    	mEventsListView.setAdapter(mEventItemsAdapter);    	
    	    	
    	mEventsListView.setCacheColorHint(0);
        // No dividers
    	mEventsListView.setDivider(null);
        // Items are clickable
    	mEventsListView.setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
    	mEventsListView.setFastScrollEnabled(false); 
    	mEventsListView.setVerticalScrollBarEnabled(false);
    	
    	mEventsListView.setFadingEdgeLength(0);
        // Make the scrolling behavior nicer
    	mEventsListView.setFriction(ViewConfiguration.getScrollFriction() * mFriction);   		
    	
    	// ���⼭ �ϸ� �ȵȴ�
    	// PusdoLowerListViewTranslateAnimationListener.EventListModeEntrancePsudoMonthView���� 
    	// Lower ������ ������ �� �Ѵ�
    	// mEventsListView.setVisibility(View.VISIBLE);    	
    	
    	Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
		TempCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());
		TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
		/*String xxxxx = mCurrentMonthTime.format3339(true);
		Date aaaaa = TempCalendar.getTime();
		String zzzz = aaaaa.toString();*/
		
		int parentViewHeight = mAnticipationMonthViewLayoutHeight;
		int secondaryUpperActionBarHeight = mDayHeaderHeight; 
		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
		int monthListViewVisualableRegionHeight = mListView.getEEMNormalWeekHeight() * weekNumbersOfMonth;
		int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);	    	
		int height = parentViewHeight - (secondaryUpperActionBarHeight + monthListViewVisualableRegionHeight + lowerActionBarHeight);
		    	
		RelativeLayout.LayoutParams eventListViewParams = (RelativeLayout.LayoutParams) mEventsListView.getLayoutParams();
		eventListViewParams.height = height;    	
    	mEventsListView.setLayoutParams(eventListViewParams);
    	
    	mTempTime.setTimeInMillis(System.currentTimeMillis());
		int targetJualianDayOfEventListView;		
		if (mCurrentMonthTimeDisplayed.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && mCurrentMonthTimeDisplayed.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {						
			targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		}
		else {						
			targetJualianDayOfEventListView = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		}	
    	mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);       			
		mEventItemsAdapter.notifyDataSetChanged();
		
		eventListViewHeight = height;
		return eventListViewHeight;	
    }
    
    public void doResumeUpdates() {
        //mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        //mShowWeekNumber = Utils.getShowWeekNumber(mContext);
        boolean prevHideDeclined = mHideDeclined;
        mHideDeclined = Utils.getHideDeclinedEvents(mContext);
        if (prevHideDeclined != mHideDeclined && mLoader != null) {
            mLoader.setSelection(updateWhere());
        }
        
        mDaysPerWeek = Utils.getDaysPerWeek(mContext);        
        
        mTZUpdater.run(); // MonthAdapter.refresh�� ȣ��ȴ�...�� �׷��� �ϴ�
        mTodayUpdater.run(); // MonthAdapter.refresh�� ȣ��ȴ�...���� �� �׷���
        
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
        
    
    @Override
    public void eventsChanged() {
    	// TODO remove this after b/3387924 is resolved
    	if (INFO) Log.i(TAG, "eventsChanged");
    	if (mLoader != null) {
    		Log.i("tag", "MonthByWeekFragment.eventsChanged");
    		mLoader.forceLoad();
    	}
    }

    @Override
    public long getSupportedEventTypes() {
    	return EventType.GO_TO | 
    			EventType.EVENTS_CHANGED | 
    			EventType.EXPAND_EVENT_LISTVIEW | 
    			EventType.COLLAPSE_EVENT_VIEW |
    			EventType.GO_TO_YEAR_ANIM;
    }

     
    @Override
    public void handleEvent(EventInfo event) {    	
    	     
    	// ������ �ٲ���...
    	// '����' �� ���õǸ�,
    	// ������ red circle�� ��ġ�� Ȯ������
    	// 1.red circle�� �Ϻ��ϰ� month listview ��[inside]�� ��ġ�Ѵٸ�,
    	//   ������ ��¥ day fragment�� ��ȯ����
    	// 2.�׷��� �ʴٸ� scrolling�� �Ѵ�
    	if (event.eventType == EventType.GO_TO) { // today�� �����Ͽ��� ��� ����� ���̴�  
    		if (INFO) Log.i(TAG, "handleEvent:EventType.GO_TO");
    		
    		// goodbyeMonthFragment���� �߼��ϴ� EventType.GO_TO�� ó���ؼ��� �ȵȴ�    		// 
    		// :DayFragment�� ��ȯ�ϱ� ���� ���̱� �����̴�
    		if ((event.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0) {
    			//int test = -1;
    			//test = 0;
    			return;
    		}
    		
    		boolean animate = true;
    		
            int howFarAway = Math.abs(
                    ETime.getJulianDay(event.selectedTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek) - 
                    ETime.getJulianDay(mFirstVisibleDay.getTimeInMillis(), mTimeZone, mFirstDayOfWeek) - 
                    mDaysPerWeek * mNumWeeks / 4);
    		if (mDaysPerWeek * mNumWeeks * 4 < howFarAway) {
    			animate = false;
    		}
               		
    		//boolean animateToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
    		    		
    		mCurrentMonthTimeDisplayed.set(event.selectedTime.get(Calendar.YEAR), event.selectedTime.get(Calendar.MONTH), 1);
    		    		
    		if ( (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0) {
    			goToToday(animate);
    		}
    		else {
    			goTo(false, true);
    		}   		
    		
    	} else if (event.eventType == EventType.EVENTS_CHANGED) {
    		
    		eventsChanged();
    		
    	} else if (event.eventType == EventType.EXPAND_EVENT_LISTVIEW) {        	
    		// event view�� expand �Ѵٸ�,
    		// ��� motion action �߻��� ȸ���ؾ� �Ѵ�
    		// :��1�ܰ谡 �ٷ� MonthListView�� onInterceptTouchEvent���� ȸ���ϴ� ���ϰԴ�        	
    		//CalendarController controller = CalendarController.getInstance(mContext);        
       	
    		// ��ũ�� �߿��� expand event view�� �� �� ���� 
    		if (mCurrentScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
    			// upper actionbar ���� expand event view�� �� �� ������ feedback�� �ؾ� �Ѵ�        		
    			mController.sendEvent(this, EventType.EXPAND_EVENT_VIEW_NO_OK, null, null, -1, ViewType.MONTH);
    			return ;
    		}
    		else {
    			// 1. fling ���̶�� fling�� �ߴܽ�Ų��
    			//   :��ũ�� �߿��� �� �ǰ� fling �߿��� ������ ������ 
    			//    fling ���̶�� ���� ��ġ �гο� ��� �׼ǵ� �������� ���� �ʱ� �����̴� 
    			if (mCurrentScrollState == OnScrollListener.SCROLL_STATE_FLING) {
    				mListView.stopFlingInNMode();
    				stopFlingComputation();
    				onScrollStateChanged(mListView, OnScrollListener.SCROLL_STATE_IDLE);
    			} 
       		
    			// 2. ���� Month Listview�� ����(First visible week�� ��¥)�� ��� �´�
    			MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);        		
    			//int lastJulianDay = child.getLastJulianDay();
    			int weekPattern = child.getWeekPattern();        		   		
    			int position = child.getWeekNumber();
       		
    			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
    			Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
    			TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
       		
    			int delta = 0;
    			if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {    
    				/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
    				int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);
    				int remainingWeeks = remainingMonthDays / 7;        	
	               	int lastWeekDays = remainingMonthDays % 7;       	
	                if (lastWeekDays != 0) { 
	                   	++remainingWeeks;            	
	                }*/
    				int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, false);	                
    				
    				TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    				int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
           		
    				// *monthHeight
    				//  :mListView.getChildAt(0)�� ��ġ�� Week�� Month Height�� �ǹ��Ѵ�
    				int monthHeight = mListView.getNMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
    				//int visibleMonthRegionHeight = child.getBottom();         	
                   
	                // *visibleMonthRegionHeight
	                //  :mListView.getChildAt(0)�� ��ġ�� Week�� Month�� Month ListView ������ ���̴� ������ Height�� �ǹ���
	                int visibleMonthRegionHeight = child.getBottom() + (remainingWeeks * mListView.getNMNormalWeekHeight());
	                 
	                float mFV = (float)monthHeight;
	               	float vFV = (float)visibleMonthRegionHeight;
	               	
	               	float visibleMonthHeightRatio = vFV / mFV;   	
	               	
	               	if (visibleMonthHeightRatio >= 0.5f) {
	               		// mListView.getChildAt(0)�� ��ġ�� week�� Month�� top�� ��ġ�Ѵ�
	               		int DownwardMove = monthHeight - visibleMonthRegionHeight;   
	               		delta = -DownwardMove;                		
	               	}
	               	else {
	               		// Next month�� Top�� ��ġ�Ѵ�                		
	               		int UpwardMove = visibleMonthRegionHeight;    
	               		delta = UpwardMove;            
	               		TempCalendar.add(Calendar.MONTH, 1);
	               	}                    
    			}
    			else {
    				if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {            			
               		            			
    					int normalWeekHeight = mListView.getNMNormalWeekHeight();
    					int lastWeekHeight = normalWeekHeight;
    					int topFromListView = Math.abs(child.getTop());
               		
    					if (topFromListView > lastWeekHeight) { // prv month�� last week�� ���� ������ �ʴ´ٴ� ���� �ǹ��ϴ� �� �Ӹ� �ƴ϶�
               			                                    	// next month�� ���� ������ ������ �ʰ� �ִٴ� ���� �ǹ���
               			                                    	// ��, next month�� indicator ���� next month�� � �������� ������ �ʴ´ٴ� ���� �ǹ���
               			                                    	// �̴� next month�� month indicator�� top�� ������Ѽ� Month ListView�� Top�� ����� �Ѵٴ� ���� �ǹ���. 
               			
    						// delta ��� ���� topFromListView���� lastWeekHeight[prv month�� last week]�� ����
    						// next month�� ������ �ʴ� ������ Height�� ����� �� ����
    						// �� ������ �ʴ� ������ ���� �� �ֵ��� ���� �̵��ؾ� ��
    						int DownwardMove = topFromListView - lastWeekHeight;
    						delta = -DownwardMove;
    					}else if (lastWeekHeight > topFromListView) { // prv month�� last week�� ���� �κи� ������ �ʴ� �ٴ� ���� �ǹ��Ѵ�
               			                                          // �׷��Ƿ� next month�� month indicator�� top�� ���� ���Ѽ� Month ListView�� Top�� ����� �Ѵٴ� ���� �ǹ���.
               			
    						// topFromListView�� lastWeekHeight �� ������ �ʴ� ������ Height�� �ǹ��Ѵ�
    						// lastWeekHeight - topFromListView ���� lastWeekHeight�� ���̴� ������ Height�� �ǹ��Ѵ�
    						// lastWeekHeight�� ���̴� ������ Height ��ŭ ���� �̵��ؾ� 
    						// next month�� month indicator�� top�� Month ListView�� Top�� ��ġ��ų �� �ִٴ� ���� �ǹ��Ѵ�
    						int UpwardMove = lastWeekHeight - topFromListView;
    						delta = UpwardMove;
    					}else {
    						// adjustment�� �� �ʿ䰡 ���� ����
    						// next month�� month indicator�� top�� ��Ȯ�ϰ� Month ListView�� Top�� ��ġ�� �ִٴ� ���� �ǹ��Ѵ�
    					}                		
    				}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {                		
    					int DownwardMove = Math.abs(child.getTop());
    					delta = -DownwardMove;
               		
    				}/*else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {                		
               			int UpwardMove = mListView.getNMLastWeekHeight() - Math.abs(child.getTop());    
               			delta = UpwardMove;
               			TempCalendar.add(Calendar.MONTH, 1);
               		}*/
    			}                
           	
    			mCurrentMonthViewMode = SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE;///////////////////////////////////////////////////////////
    			mExpandEventViewInfo = new ExpandEventViewInfo(TempCalendar);
    			// 3. �ʿ��ϴٸ� layout�� adjustment �ؾ��Ѵ�    			
    			if (delta !=0) {
    				// ��带 �����Ͽ� onScrollStateChanged���� SCROLL_STATE_IDLE ������ �����ϸ�
    				// expand event view�� ������ �۾��� �����ؾ� �Ѵ�        
    				mExpandEventListViewState = ExpandEventListViewState.ListViewMonthIndicatorTopAdjustmentScrollState;
    				// smoothScrollBy ���� ���, SCROLL_STATE_FLING�� �߻��ȴ�
    				long duration = MonthListView.calculateDurationForFling(Math.abs(delta), mListView.getHeight(), ADJUST_TOP_VELOCITY);
    				mListView.smoothScrollBy(delta, (int) duration); // �ٵ� �Ϸ�Ǵ� ������ �𸥴�?////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
           	                                              // onScrollStateChanged���� SCROLL_STATE_IDLE�� ��ȯ�Ǵ� ���� Ȯ���ϸ� �Ǵ� ���ΰ�? : yes
    			} 
    			else {
    				mExpandEventListViewState = ExpandEventListViewState.PrepareEventsViewModeEntranceAnimState;
    				// Event View Entrance Animation Runnable�� ������Ű��
    				mHandler.post(mPrePareEventListViewEntranceRunnable);
           		}            	
       		}
    	}
    	else if (event.eventType == EventType.COLLAPSE_EVENT_VIEW) {     
       	
       		mExpandEventListViewState = ExpandEventListViewState.PrepareEventsViewModeExitAnimState;        	
       		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
   			TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
   			TempCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());
   			
       		mCollapseEventViewInfo = new CollapseEventViewInfo(TempCalendar);        	
       	
       		mHandler.post(mPrePareEventListViewExitRunnable);        	
       	}  
       	else if (event.eventType == EventType.GO_TO_YEAR_ANIM) {
       		
       		// GO_TO_YEAR_ANIM���� selectedDay�� �޴� ���� ���???
       		mGoToExitAnim = true;
       		ExitMonthViewEnterYearViewAnim animObj = new ExitMonthViewEnterYearViewAnim(mContext, this, mCurrentMonthTimeDisplayed);
       		animObj.startMonthExitAnim();
       	}
    }
    
        
    private final Runnable mUpdateLoader = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
            	if (INFO) Log.i(TAG, "mUpdateLoader");
                // Stop any previous loads while we update the uri
                stopLoader();

                // Start the loader again
                mEventUri = updateUri();

                mLoader.setUri(mEventUri);
                mLoader.startLoading();
                mLoader.onContentChanged();                
            }
        }
    };
    
    
    // <updateLoaderInEEMode Callers>
    // 1.MonthFragment
    //   -adjustmentMonthListTopItemPositionAfterScrollingInEEMode
    //   -goToToday
    //   -doGoToTodayFlingInEEMode

    // 2.MonthListView
    //  -adjustmentMonthListTopItemPositionFlingInEEMode : MonthListView.doFlingInEEMode���� ȣ��ȴ�
    public void updateLoaderInEEMode(int julianDay) {
    	synchronized (mUpdateLoader) {
	    	stopLoader();
	        mEventUri = updateUriByJulianDay(julianDay);
	        mLoader.setUri(mEventUri);
	        mLoader.startLoading();
	        mLoader.onContentChanged(); 
    	}
    };
    
    // <updateLoaderInNMode Callers>
    // 1.MonthFragment
    //  -goToToday
    //  -doGoToTodayFlingInNMode
    public void updateLoaderInNMode(int julianDay) {
    	synchronized (mUpdateLoader) {
	    	stopLoader();
	        mEventUri = updateUriByJulianDay(julianDay);
	        mLoader.setUri(mEventUri);
	        mLoader.startLoading();
	        mLoader.onContentChanged(); 
    	}
    };
        
    /*
   	onActivityCreated
	->mLoader = (CursorLoader) getLoaderManager().initLoader�� ���� onCreateLoader�� ȣ��ȴ�
    */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	
        CursorLoader loader;
        synchronized (mUpdateLoader) {
        	if (args != null) {
        		if (args.getBoolean(ALREADY_SET_LOAD_JULIANDAY_VALUES)) {
	                // -1 to ensure we get all day events from any time zone
        			long mills = ETime.getMillisFromJulianDay(mFirstLoadedJulianDay - 1, mTimeZone, mFirstDayOfWeek);
        			mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
	                long start = mTempTime.getTimeInMillis();        
	                
	                // +1 to ensure we get all day events from any time zone
	                mills = ETime.getMillisFromJulianDay(mLastLoadedJulianDay + 1, mTimeZone, mFirstDayOfWeek);
	                mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mLastLoadedJulianDay + 1);
	                long end = mTempTime.getTimeInMillis();           
	                
	                // Create a new uri with the updated times
	                Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
	                ContentUris.appendId(builder, start);
	                ContentUris.appendId(builder, end);               
	                
	                mEventUri = builder.build(); 
        		}
        		else {
        			int CurrentMonthDisplayedJulianDay = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); // for test
                	//int prvExtendDays = mNumWeeks * 7 / 2; // 21���� �������� �߰��ȴ�        	
                	int prvExtendDays = mNumWeeks * 7; // 42���� �������� �߰��ȴ�
                	mFirstLoadedJulianDay = CurrentMonthDisplayedJulianDay - prvExtendDays;        		         
                	        
                	mEventUri = updateUri();
        		}
            }
        	else {
        		int CurrentMonthDisplayedJulianDay = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); // for test
            	//int prvExtendDays = mNumWeeks * 7 / 2; // 21���� �������� �߰��ȴ�        	
            	int prvExtendDays = mNumWeeks * 7; // 42���� �������� �߰��ȴ�
            	mFirstLoadedJulianDay = CurrentMonthDisplayedJulianDay - prvExtendDays;        		         
            	        
            	mEventUri = updateUri();
            	
        	}
        	
        	String where = updateWhere();

        	loader = new CursorLoader(
        				getActivity(), mEventUri, Event.EVENT_PROJECTION, where,
        				null /* WHERE_CALENDARS_SELECTED_ARGS */, INSTANCES_SORT_ORDER);
        	//loader.setUpdateThrottle(LOADER_THROTTLE_DELAY);
        }        
        
        return loader;
    }
       
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {  	
    	
    	synchronized (mUpdateLoader) {
    		    	
    		if (INFO) Log.i(TAG, "onLoadFinished");
    		
    		CursorLoader cLoader = (CursorLoader) loader;
    		if (mEventUri == null) {
    			
    			mEventUri = cLoader.getUri();
    			updateLoadedDays();
    		}
           
    		if (cLoader.getUri().compareTo(mEventUri) != 0) {
    			// We've started a new query since this loader ran so ignore the
    			// result
    			
    			if (INFO) Log.i(TAG, "onLoadFinished:why return???");
    			return;
    		}
           
    		ArrayList<Event> mEvents = new ArrayList<Event>();
    		// updateUri���� mFirstLoadedJulianDay�� mLastLoadedJulianDay�� ������Ʈ ��
    		Event.buildEventsFromCursor(
    				mEvents, data, mContext, mFirstLoadedJulianDay, mLastLoadedJulianDay);
           
    		// fling ���� ���� event �ε���,
    		// setEvents�� �Ҷ� ������ refresh�� ���� ����...
    		// ���� ��� listview�� ���� ù��° �Ǵ� ������ event�� ���� mFirstLoadedJulianDay �� mLastLoadedJulianDay��
    		// ����?���� �ʴ´ٸ� ...�׳� events�� �Ѱ� ����
    		MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);
            if (child != null) {
                int firstJulianDay = ETime.getJulianDay(child.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//child.getFirstJulianDay();
                int lastJulianDay = firstJulianDay + 6;//child.getLastJulianDay();
                
                if (mCurrentScrollState == OnScrollListener.SCROLL_STATE_FLING) {	
                	if (INFO) Log.i(TAG, "onLoadFinished:SCROLL_STATE_FLING");
                	
	                if (firstJulianDay <= mFlingTargetMonthJulianDay && mFlingTargetMonthJulianDay <= lastJulianDay ) {
	                	if (INFO) Log.i(TAG, "Already ListView moved to Target item");
	                	
	                	mAdapter.setEvents(mFirstLoadedJulianDay,
	            				mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, mEvents);
	                }
	                else {
	                	if (INFO) Log.i(TAG, "NOT Yet listview moved target item");
	                	
	                	mAdapter.setOnlyEvents(mFirstLoadedJulianDay,
	            				mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, mEvents);
	                }
	                
                }
                else {
                	if (INFO) Log.i(TAG, "onLoadFinished:NOT SCROLL_STATE_FLING");
                	mAdapter.setEvents(mFirstLoadedJulianDay,
            				mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, mEvents);
                }
                
            }  
            else {
            	if (INFO) Log.i(TAG, "onLoadFinished:child == null");
            	mAdapter.setEvents(mFirstLoadedJulianDay,
        				mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, mEvents);
            }
    		
           
       	}
   	}

   	@Override
   	public void onLoaderReset(Loader<Cursor> loader) {
   	}
   
   
   
    /**
     * Updates the uri used by the loader according to the current position of
     * the listview.
     *
     * @return The new Uri to use
     */
    private Uri updateUri() {
    	int prvExtendDays = mNumWeeks * 7;
    	
        MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);
        if (child != null) {
            int julianDay = ETime.getJulianDay(child.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//child.getFirstJulianDay();
            
            Calendar cal = GregorianCalendar.getInstance(child.getFirstMonthTime().getTimeZone());
            cal.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());
            mFirstLoadedJulianDay = julianDay - prvExtendDays;
        }        
         
        // -1 to ensure we get all day events from any time zone
        long mills = ETime.getMillisFromJulianDay(mFirstLoadedJulianDay - 1, mTimeZone, mFirstDayOfWeek);
        mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
        long start = mTempTime.getTimeInMillis();        
        
        int visibleDaysInView = mNumWeeks * 7;
        int nextExtendDays = mNumWeeks * 7;
        mLastLoadedJulianDay = mFirstLoadedJulianDay + prvExtendDays + visibleDaysInView + nextExtendDays;
        //mLastLoadedJulianDay = mFirstLoadedJulianDay + (mNumWeeks + 2 * WEEKS_BUFFER) * 7;   
        // +1 to ensure we get all day events from any time zone
        mills = ETime.getMillisFromJulianDay(mLastLoadedJulianDay + 1, mTimeZone, mFirstDayOfWeek);
        mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mLastLoadedJulianDay + 1);
        long end = mTempTime.getTimeInMillis();

        // Create a new uri with the updated times
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return builder.build();
    }
    
    private Uri updateUriByJulianDay(int julianDay) {
    	int prvExtendDays = mNumWeeks * 7;
    	
    	mFlingTargetMonthJulianDay = julianDay;
    	mFirstLoadedJulianDay = julianDay - prvExtendDays;
         
        // -1 to ensure we get all day events from any time zone
    	long mills = ETime.getMillisFromJulianDay(mFirstLoadedJulianDay - 1, mTimeZone, mFirstDayOfWeek);
    	mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
        long start = mTempTime.getTimeInMillis();        
        
        int visibleDaysInView = mNumWeeks * 7;
        int nextExtendDays = mNumWeeks * 7;
        
        
        mLastLoadedJulianDay = mFirstLoadedJulianDay + prvExtendDays + visibleDaysInView + nextExtendDays;
        mills = ETime.getMillisFromJulianDay(mLastLoadedJulianDay + 1, mTimeZone, mFirstDayOfWeek);
        // +1 to ensure we get all day events from any time zone
        mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mLastLoadedJulianDay + 1);
        long end = mTempTime.getTimeInMillis();

        // Create a new uri with the updated times
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return builder.build();
    }
       

    // Extract range of julian days from URI
    private void updateLoadedDays() {
        List<String> pathSegments = mEventUri.getPathSegments();
        int size = pathSegments.size();
        if (size <= 2) {
            return;
        }
        long first = Long.parseLong(pathSegments.get(size - 2));
        long last = Long.parseLong(pathSegments.get(size - 1));
        mTempTime.setTimeInMillis(first);
        mFirstLoadedJulianDay = ETime.getJulianDay(first, mTimeZone, mFirstDayOfWeek);
        mTempTime.setTimeInMillis(last);
        mLastLoadedJulianDay = ETime.getJulianDay(last, mTimeZone, mFirstDayOfWeek);
    }

    public String updateWhere() {
        // TODO fix selection/selection args after b/3206641 is fixed
        String where = WHERE_CALENDARS_VISIBLE;
        if (mHideDeclined) {
            where += " AND " + Instances.SELF_ATTENDEE_STATUS + "!="
                    + Attendees.ATTENDEE_STATUS_DECLINED;
        }
        return where;
    }

    private void stopLoader() {
        synchronized (mUpdateLoader) {
            mHandler.removeCallbacks(mUpdateLoader);
            
            if (mLoader != null) {
                mLoader.stopLoading();                
            }
        }
    }
        
    // callers    
    // :MonthAdapter.onTouch
    public void setEventsOfEventListView(int selectedJulianDay) {
    	int targetMonthFirstJulianDay = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    	
    	// mAdapter.getEvents()�� ����� events�� �����ϰ� �ִ��� ��� �Ǵ��� �� �ִ°�?
    	//int numDays = mLastLoadedJulianDay - mFirstLoadedJulianDay + 1;
    	ArrayList<Event> events = mAdapter.getEvents();
    	
    	Calendar temp = GregorianCalendar.getInstance(mTimeZone);
    	long targetMonthFirstJulianDayMills = ETime.getMillisFromJulianDay(targetMonthFirstJulianDay, mTimeZone, mFirstDayOfWeek);
        temp.setTimeInMillis(targetMonthFirstJulianDayMills);//temp.setJulianDay(targetMonthFirstJulianDay);    
    	
    	Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);    	
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
		TempCalendar.setTimeInMillis(temp.getTimeInMillis());
				
		int maxMonthDays = mCurrentMonthTimeDisplayed.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		mExpandEventModeTargetMonthEventExsitence = new boolean[maxMonthDays];
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < maxMonthDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        // 
        if (events == null || events.size() == 0) {
            if(Log.isLoggable("tag", Log.DEBUG)) {
                Log.d("tag", "No events. Returning early--go schedule something fun.");
            }
            mExpandEventModeTargetMonthEventsList = eventDayList;            
            return;
        }
                
        // Compute the new set of days with events
        for (Event event : events) {        
            int startDay = event.startDay - targetMonthFirstJulianDay;
            int endDay = event.endDay - targetMonthFirstJulianDay + 1; // 1�� ���ϴ� ���� �Ʒ� for ������ looping ���ǹ��� ���� ��                                                                       
            //if (startDay < maxMonthDays || endDay >= 0) {
            if (startDay < maxMonthDays || endDay > 0) {
                if (startDay < 0) {  // �� ���� ��Ȳ�� �ִ�
                	                 // 1. event duration time�� 2014/09/30/23 ~ 2014/10/01/02 �� ��� : duration time�� 24hour �̻��� �ƴ�����, event�� ��Ʋ�� ��ġ�� ���
                	                 // 2. event duration time�� 2014/09/27/23 ~ 2014/10/02/02 �� ��� : duration time�� 24hour �̻��� ���, 
                	                 //    ���� 2014/09/27/�� 2014/10/01�� ���� week position�� �ƴ� ��� : ���Ե� calendar utils�� �̸� ������ ������ �ش� 
                	startDay = 0;
                }
                                
                if (startDay >= maxMonthDays) { // ���� ���� event���� �������� �ʴ´�
                    continue;
                }
                
                if (endDay < 0) { // target month�� ���� ���� starttime�̳� endtime�� ���� ���!!!
                    continue;
                }                
                
                if (endDay > maxMonthDays) { // �̴� event�� startday�� �ش� month day������, endday�� next month day�� ���,
                	                         // �ش� month�� ������ �������� event�� ǥ���ϱ� ���� ��ġ                    
                	endDay = maxMonthDays;
                }
                
                if (!mExpandEventModeTargetMonthEventExsitence[startDay])
                	mExpandEventModeTargetMonthEventExsitence[startDay] = true;
                
                for (int j = startDay; j < endDay; j++) {
                    eventDayList.get(j).add(event);
                }
            }
        }
        if(Log.isLoggable("tag", Log.DEBUG)) {
            Log.d("tag", "Processed " + events.size() + " events.");
        }
        mExpandEventModeTargetMonthEventsList = eventDayList;
                
        
        if (mEventItemsAdapter != null)
			mEventItemsAdapter.closeEventCursors();
    	
    	targetMonthFirstJulianDay = ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
    	int index = selectedJulianDay - targetMonthFirstJulianDay;	
    	if (mExpandEventModeTargetMonthEventExsitence[index]) { // standby -> wakeup �� ������ ���� ���� �߻�
    															// java.lang.ArrayIndexOutOfBoundsException: length=31; index=-580
    		ArrayList<Event> targetDayEvents = mExpandEventModeTargetMonthEventsList.get(index); 
    		mEventItemsAdapter.setEvents(/*targetMonthFirstJulianDay, */selectedJulianDay, targetDayEvents);
    	}
    	else {
    		mEventItemsAdapter.setNonEvents();
    	}    	 
    }    
       
	
	public void launchFastEventInfoView(long eventId, long startMillis, long endMillis) {		
		
    	EventCursors targetObj = mEventItemsAdapter.getEventCursors(eventId);/////////////////////////////////////////////////////////////////// 
    	
    	mEventInfoFragment.launchFastEventInfoView(ViewType.MONTH, eventId, 0, startMillis, endMillis, targetObj);
    	
    	
    	Log.i("tag", "-launchFastEventInfoView");    
	}
	   
       
    Runnable mPrePareEventListViewEntranceRunnable = new Runnable() {
        @Override
        public void run() {
        	int test = mFrameLayoutViewSwitcher.getHeight();
            // � ������ �ʿ��Ѱ�?
        	// �ش� ���� ������ �ʿ��ϴ�
        	int normalModeMonthListViewHeight = mListView.getHeight();
        	int normalModeMonthIndicatorHeight = mListView.getNMMonthIndicatorHeight();
        	int normalModeFirstWeekHeight = mListView.getNMFirstWeekHeight();
        	int normalModeNormalWeekHeight = mListView.getNMNormalWeekHeight();
        	int eventlistModeNormalWeekHeight = mListView.getEEMNormalWeekHeight();   
        	       	
        	mTempTime.setTimeInMillis(System.currentTimeMillis());
        	// ���⼭ �츮�� �ش� ����� �������� ����� ���������� Ȯ���ؾ� �Ѵ�
        	int yearOfTargetMonth = mExpandEventViewInfo.mCalendar.get(Calendar.YEAR);
        	int monthOfTargetMonth = mExpandEventViewInfo.mCalendar.get(Calendar.MONTH);
        	if (yearOfTargetMonth == mTempTime.get(Calendar.YEAR) && monthOfTargetMonth == mTempTime.get(Calendar.MONTH)) {
        		mAdapter.setSelectedDayInEEMode(ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));                    
            }
    		else 
    			mAdapter.setSelectedDayInEEMode(mExpandEventViewInfo.mFirstDayJulianDayInMonth);
        	
        	
        	int weekNumbers = ETime.getWeekNumbersOfMonth(mExpandEventViewInfo.mCalendar);
        	int maxMonthDays = mExpandEventViewInfo.mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        	mExpandEventViewInfo.mCalendar.set(Calendar.DAY_OF_MONTH, maxMonthDays);////////////////////////////////////////////////////////////////
        	
        	//int lastJulianDayInMonth = ETime.getJulianDay(mExpandEventViewInfo.mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	int lastWeekNumber = ETime.getWeeksSinceEcalendarEpochFromMillis(mExpandEventViewInfo.mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//Time.getWeeksSinceEpochFromJulianDay(lastJulianDayInMonth, mFirstDayOfWeek);       	
        	// ���⼭ �ش� ���� last week ������ �Ǵ��ؾ� �Ѵ�        	
        	int julianMondayInLastWeek = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(lastWeekNumber);//Time.getJulianMondayFromWeeksSinceEpoch(lastWeekNumber);
            Calendar time = GregorianCalendar.getInstance(mTimeZone);
            time.setFirstDayOfWeek(mFirstDayOfWeek);
            long julianMondayInLastWeekMills = ETime.getMillisFromJulianDay(julianMondayInLastWeek, mTimeZone, mFirstDayOfWeek);
            time.setTimeInMillis(julianMondayInLastWeekMills);//time.setJulianDay(julianMondayInLastWeek);
            ETime.adjustStartDayInWeek(time);
            
            // fling �� ���۽��� �ߴ����� month ������ ��� �����Ǵ��� �ٽ� �ѹ� Ȯ���� ����!!!!
            // �Ʒ� �ڵ忡 ������ �ִ°�?
            //int firstJulianDayInLastWeekDay = ETime.getJulianDay(time.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
            Calendar firstJulianDayInLastWeekDayTime = GregorianCalendar.getInstance(mTimeZone);
            firstJulianDayInLastWeekDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
            //long firstJulianDayInLastWeekDayMills = ETime.getMillisFromJulianDay(firstJulianDayInLastWeekDay, mTimeZone, mFirstDayOfWeek);
            firstJulianDayInLastWeekDayTime.setTimeInMillis(time.getTimeInMillis());//firstJulianDayInLastWeekDayTime.setJulianDay(firstJulianDayInLastWeekDay);
            
            //int lastJulianDayInLastWeekDay = firstJulianDayInLastWeekDay + 6;            
            Calendar lastJulianDayInLastWeekDayTime = GregorianCalendar.getInstance(mTimeZone);
            lastJulianDayInLastWeekDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
            //long lastJulianDayInLastWeekDayMills = ETime.getMillisFromJulianDay(lastJulianDayInLastWeekDay, mTimeZone, mFirstDayOfWeek);
            lastJulianDayInLastWeekDayTime.setTimeInMillis(firstJulianDayInLastWeekDayTime.getTimeInMillis());//lastJulianDayInLastWeekDayTime.setJulianDay(lastJulianDayInLastWeekDay);   
            lastJulianDayInLastWeekDayTime.add(Calendar.DAY_OF_MONTH, 6);
            
            int first_week_pattern = 0;            
            if (firstJulianDayInLastWeekDayTime.get(Calendar.MONTH) != lastJulianDayInLastWeekDayTime.get(Calendar.MONTH)) {
            	first_week_pattern = MonthWeekView.PSUDO_FIRSTWEEK_OF_TWO_DIFFWEEKS_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE;
            }
            else {
            	first_week_pattern = MonthWeekView.PSUDO_FIRSTDAY_FIRSTWEEK_PATTERN_AT_EXPAND_EVENTVIEW_ENTRANCE;
            	lastWeekNumber++;
            }       	
        	
            int realTargetMonthHeight = normalModeMonthIndicatorHeight + (weekNumbers * normalModeNormalWeekHeight);  // psudoLowerListViewTop�� ���� �����ϴ� ��
            int targetMonthHeightBeforeScale = normalModeMonthIndicatorHeight + (weekNumbers * normalModeNormalWeekHeight); // psudo month view�� height�� ���� �����ϴ� ��
                                                                                                                         // �̷� ���� psudo lower list view�� ������ ħ���ϰ� �ȴ�
                                                                                                                         // �׷��� �Ѵ� ��׶��� ���� ����̶� user�� �˾� ���� �� ����
            
            // ������ ������� �����Ǿ���
    		RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, targetMonthHeightBeforeScale); 
    		listViewParams.addRule(RelativeLayout.BELOW, mCalendarViewsSecondaryActionBar.getId());
    		mPsudoTargetMonthViewAtEventListEntrance = new PsudoTargetMonthViewAtEventListEntrance(mContext);    
    		mPsudoTargetMonthViewAtEventListEntrance.setLayoutParams(listViewParams);
    		mPsudoTargetMonthViewAtEventListEntrance.setFragment(MonthFragment.this);  
    		mPsudoTargetMonthViewAtEventListEntrance.setId(R.id.psudo_target_monthview_at_eventlistview_entrance);
        	
    		if (weekNumbers < 6) {
    			mPsudoNextMonthRegionContainer.setVisibility(View.VISIBLE);  
    			mPsudoTargetMonthViewAtEventListEntrance.IsMonthWithLessThanSixWeeks(true);
        		        		
    			boolean mWillAddSecondWeek = false;
    			if (weekNumbers == 4)
        			mWillAddSecondWeek = true;
    			
    			int oriListViewHeight = mListView.getOriginalListViewHeight();  
    			    			
    			//int parentViewHeight = normalModeMonthListViewHeight;    			 					
    			int psudoNextMonthRegionHeight = normalModeMonthListViewHeight - (realTargetMonthHeight);
    			RelativeLayout.LayoutParams pusdoViewContainerParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, psudoNextMonthRegionHeight);   
    			pusdoViewContainerParams.addRule(RelativeLayout.ABOVE, mMonthviewPsudoLowerActionBar.getId());	
    			mPsudoNextMonthRegionContainer.setLayoutParams(pusdoViewContainerParams);  
    			mPsudoNextMonthRegionContainer.setOrientation(LinearLayout.VERTICAL);    
    			    			
            	MonthWeekView firstWeekView = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams firstWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, normalModeNormalWeekHeight);
            	firstWeekView.setLayoutParams(firstWeekViewParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> firstWeekDrawingParams = new HashMap<String, Integer>();
                
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, oriListViewHeight);                
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, normalModeMonthIndicatorHeight);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, normalModeNormalWeekHeight);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, normalModeFirstWeekHeight);            
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);                
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, lastWeekNumber);            
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, first_week_pattern);   
                
                firstWeekView.setWeekParams(firstWeekDrawingParams, mTzId);
                mAdapter.sendEventsToView(firstWeekView);                
                mPsudoNextMonthRegionContainer.addView(firstWeekView);                
               
                if (mWillAddSecondWeek) {
                	MonthWeekView secondWeekView = new MonthWeekView(mContext);
                	LinearLayout.LayoutParams secondWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, normalModeNormalWeekHeight);     
                	secondWeekView.setLayoutParams(secondWeekViewParams);////////////////////////////////////////////////////////////////////////////
                    HashMap<String, Integer> secondWeekDrawingParams = new HashMap<String, Integer>();
                    
                    int second_week_pattern = MonthWeekView.PSUDO_NORMALWEEK_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE;
                    
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, oriListViewHeight);                    
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, normalModeMonthIndicatorHeight);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, normalModeNormalWeekHeight);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, normalModeNormalWeekHeight);            
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);                    
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, ++lastWeekNumber);            
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, second_week_pattern);
                    
                    secondWeekView.setWeekParams(secondWeekDrawingParams, mTzId);
                    mAdapter.sendEventsToView(secondWeekView);                    
                    
                    mPsudoNextMonthRegionContainer.addView(secondWeekView);
                }
                
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////                
                mPsudoTargetMonthViewAtEventListEntrance.setPsudoNextMonthRegionContainer(mPsudoNextMonthRegionContainer);
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        	}
        	else {
        		mPsudoTargetMonthViewAtEventListEntrance.IsMonthWithLessThanSixWeeks(false);
        	}
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////               
                
    		if (mEventListViewEntranceAnimCompletionCallback == null)
    			mEventListViewEntranceAnimCompletionCallback = new EventListViewEntranceAnimCompletionCallback();
    		mPsudoTargetMonthViewAtEventListEntrance.setAnimCompletionCallback(mEventListViewEntranceAnimCompletionCallback);
    		
    		HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_LISTVIEW_NM_HEIGHT, normalModeMonthListViewHeight);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_LISTVIEW_MONTHINDICATOR_HEIGHT, normalModeMonthIndicatorHeight);
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_LISTVIEW_NORMAL_WEEK_HEIGHT, normalModeNormalWeekHeight);
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_EVM_NORMAL_WEEK_HEIGHT, eventlistModeNormalWeekHeight);
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_TARGET_MONTH_HEIGHT_WITHIN_NMODE_MONTHLISTVIEW, targetMonthHeightBeforeScale);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_FIRSTJULIANDAY_IN_MONTH, mExpandEventViewInfo.mFirstDayJulianDayInMonth);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_WEEK_START, mFirstDayOfWeek);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListEntrance.MONTHVIEW_PARAMS_NUM_DAYS, 7);        		
    		mPsudoTargetMonthViewAtEventListEntrance.setMonthParam(drawingParams, mTzId);    		
    		mAdapter.sendEventsToView(mPsudoTargetMonthViewAtEventListEntrance);
    		
    		int eventListViewHeight = expandEventsListView();    		
    		mPsudoTargetMonthViewAtEventListEntrance.setEventListViewHeight(eventListViewHeight);
    		
    		mListView.setVisibility(View.GONE);
    		
    		// mMonthViewLayout�� mPsudoTargetMonthViewAtEventListEntrance�� add �����ν�
    		// mPsudoTargetMonthViewAtEventListEntrance�� onDraw �Լ��� ȣ��ǰ�
    		// ���� psudo month listview�� ������ϰ� animation�� ������ ���̴�
    		// :�� addView�� ��ǻ� entrance animation�� launch �ϴ� ���̴�
    		// �������� �ִ�...���� ���� animation�� ���۵Ǵ��� �� �� ����
    		// �ذ� ����� �����ϴ�...onDraw�κ��� Ư�� �޽����� ������ �ȴ�
    		mMonthViewLayout.addView(mPsudoTargetMonthViewAtEventListEntrance);
        	
        	mExpandEventListViewState = ExpandEventListViewState.EventsViewModeEntranceAnimState;
        }
    };    
    
    
    
    
    
    
    
    Runnable mPrePareEventListViewExitRunnable = new Runnable() {
        @Override
        public void run() {
            // � ������ �ʿ��Ѱ�?
        	// �ش� ���� ������ �ʿ��ϴ�
        	int normalModeMonthListViewHeight = mListView.getOriginalListViewHeight();
        	int normalModeMonthIndicatorHeight = mListView.getNMMonthIndicatorHeight();
        	int normalModeFirstWeekHeight = mListView.getNMFirstWeekHeight();
        	int normalModeNormalWeekHeight = mListView.getNMNormalWeekHeight();
        	        	
        	int eventlistModeMonthIndicatorHeight = mListView.getEEMMonthIndicatorHeight();
        	int eventlistModeNormalWeekHeight = mListView.getEEMNormalWeekHeight();        	
        	
        	int weekNumbers = ETime.getWeekNumbersOfMonth(mCollapseEventViewInfo.mCalendar);
        	int maxMonthDays = mCollapseEventViewInfo.mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        	mCollapseEventViewInfo.mCalendar.set(Calendar.DAY_OF_MONTH, maxMonthDays);////////////////////////////////////////////////////////////////
        	
        	//int lastJulianDayInMonth = ETime.getJulianDay(mCollapseEventViewInfo.mCalendar.getTimeInMillis(), mGmtoff);
        	int lastWeekNumber = ETime.getWeeksSinceEcalendarEpochFromMillis(mCollapseEventViewInfo.mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//Time.getWeeksSinceEpochFromJulianDay(lastJulianDayInMonth, mFirstDayOfWeek);       	
        	// ���⼭ �ش� ���� last week ������ �Ǵ��ؾ� �Ѵ�        	
        	int julianMondayInLastWeek = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(lastWeekNumber);//Time.getJulianMondayFromWeeksSinceEpoch(lastWeekNumber);
            Calendar time = GregorianCalendar.getInstance(mTimeZone);
            time.setFirstDayOfWeek(mFirstDayOfWeek);
            long julianMondayInLastWeekMills = ETime.getMillisFromJulianDay(julianMondayInLastWeek, mTimeZone, mFirstDayOfWeek);
            time.setTimeInMillis(julianMondayInLastWeekMills);//time.setJulianDay(julianMondayInLastWeek);
            ETime.adjustStartDayInWeek(time);
            /*
            if (time.weekDay != mFirstDayOfWeek) {
                int diff = time.weekDay - mFirstDayOfWeek;
                if (diff < 0) {
                    diff += 7;
                }
                time.monthDay -= diff;
                time.normalize(true);
            }*/

            // fling �� ���۽��� �ߴ����� month ������ ��� �����Ǵ��� �ٽ� �ѹ� Ȯ���� ����!!!!
            // �Ʒ� �ڵ忡 ������ �ִ°�?
            //int firstJulianDayInLastWeekDay = ETime.getJulianDay(time.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
            Calendar firstJulianDayInLastWeekDayTime = GregorianCalendar.getInstance(mTimeZone);
            firstJulianDayInLastWeekDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
            //long firstJulianDayInLastWeekDayMills = ETime.getMillisFromJulianDay(firstJulianDayInLastWeekDay, mTimeZone, mFirstDayOfWeek);
            firstJulianDayInLastWeekDayTime.setTimeInMillis(time.getTimeInMillis());//firstJulianDayInLastWeekDayTime.setJulianDay(firstJulianDayInLastWeekDay);
            
            //int lastJulianDayInLastWeekDay = firstJulianDayInLastWeekDay + 6;            
            Calendar lastJulianDayInLastWeekDayTime = GregorianCalendar.getInstance(mTimeZone);
            lastJulianDayInLastWeekDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
            //long lastJulianDayInLastWeekDayMills = ETime.getMillisFromJulianDay(lastJulianDayInLastWeekDay, mTimeZone, mFirstDayOfWeek);
            lastJulianDayInLastWeekDayTime.setTimeInMillis(firstJulianDayInLastWeekDayTime.getTimeInMillis());//lastJulianDayInLastWeekDayTime.setJulianDay(lastJulianDayInLastWeekDay);   
            lastJulianDayInLastWeekDayTime.add(Calendar.DAY_OF_MONTH, 6);
            
            int first_week_pattern = 0;            
            if (firstJulianDayInLastWeekDayTime.get(Calendar.MONTH) != lastJulianDayInLastWeekDayTime.get(Calendar.MONTH)) {
            	first_week_pattern = MonthWeekView.PSUDO_FIRSTWEEK_OF_TWO_DIFFWEEKS_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE;
            }
            else {
            	first_week_pattern = MonthWeekView.PSUDO_FIRSTDAY_FIRSTWEEK_PATTERN_AT_EXPAND_EVENTVIEW_ENTRANCE;
            	lastWeekNumber++;
            }       	
        	
            //int realRecoveryMonthHeight = normalModeMonthIndicatorHeight + (weekNumbers * normalModeNormalWeekHeight);  
            mRecoveryMonthHeight = normalModeMonthIndicatorHeight + (weekNumbers * normalModeNormalWeekHeight); 
            //mRecoveryMonthHeight = realRecoveryMonthHeight;            
            
            //int currentEEVMMonthHeight = eventlistModeMonthIndicatorHeight + (weekNumbers * eventlistModeNormalWeekHeight);
            int currentVisualableEPModeMonthListViewHeight = weekNumbers * eventlistModeNormalWeekHeight;
    		RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, currentVisualableEPModeMonthListViewHeight); 
    		listViewParams.addRule(RelativeLayout.BELOW, mCalendarViewsSecondaryActionBar.getId());
    		mPsudoTargetMonthViewAtEventListExit = new PsudoTargetMonthViewAtEventListExit(mContext);    
    		mPsudoTargetMonthViewAtEventListExit.setLayoutParams(listViewParams);
    		mPsudoTargetMonthViewAtEventListExit.setFragment(MonthFragment.this);  
    		
    		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
    		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
    		TempCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());    		
        	
    		if (weekNumbers < 6) {
    			int oriListViewHeight = mListView.getOriginalListViewHeight();  
    			
    			mPsudoNextMonthRegionContainer.setVisibility(View.VISIBLE);   
    			int psudoNextMonthRegionHeight = normalModeMonthListViewHeight - mRecoveryMonthHeight;
    			RelativeLayout.LayoutParams pusdoViewContainerParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, psudoNextMonthRegionHeight);    			 			
    			pusdoViewContainerParams.addRule(RelativeLayout.ABOVE, mMonthviewPsudoLowerActionBar.getId());	
    			mPsudoNextMonthRegionContainer.setLayoutParams(pusdoViewContainerParams);
    			mPsudoNextMonthRegionContainer.setOrientation(LinearLayout.VERTICAL);  		
    			
    			
    			mPsudoTargetMonthViewAtEventListExit.IsMonthWithLessThanSixWeeks(true);
        		        		
    			boolean mWillAddSecondWeek = false;
    			if (weekNumbers == 4)
        			mWillAddSecondWeek = true;    					
        		
            	MonthWeekView firstWeekView = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams firstWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	firstWeekView.setLayoutParams(firstWeekViewParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> firstWeekDrawingParams = new HashMap<String, Integer>();                               
                
                //int weekHeight = normalModeFirstWeekHeight;
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, oriListViewHeight);
                //firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_LISTVIEW_HEIGHT, parentNormalModeHeight);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, normalModeMonthIndicatorHeight);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, normalModeNormalWeekHeight);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, normalModeFirstWeekHeight);            
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);                
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, lastWeekNumber);            
                firstWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, first_week_pattern);   
                
                firstWeekView.setWeekParams(firstWeekDrawingParams, mTzId);
                mAdapter.sendEventsToView(firstWeekView);                
                mPsudoNextMonthRegionContainer.addView(firstWeekView);               
                
                if (mWillAddSecondWeek) {
                	MonthWeekView secondWeekView = new MonthWeekView(mContext);
                	LinearLayout.LayoutParams secondWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);     
                	secondWeekView.setLayoutParams(secondWeekViewParams);////////////////////////////////////////////////////////////////////////////
                    HashMap<String, Integer> secondWeekDrawingParams = new HashMap<String, Integer>();
                    
                    
                    int second_week_pattern = MonthWeekView.PSUDO_NORMALWEEK_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE;
                    //weekHeight = normalModeNormalWeekHeight;
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, oriListViewHeight);
                    //secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_LISTVIEW_HEIGHT, parentNormalModeHeight);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, normalModeMonthIndicatorHeight);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, normalModeNormalWeekHeight);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, normalModeNormalWeekHeight);            
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);                    
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, ++lastWeekNumber);            
                    secondWeekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, second_week_pattern);
                    
                    secondWeekView.setWeekParams(secondWeekDrawingParams, mTzId);
                    mAdapter.sendEventsToView(secondWeekView);                    
                    
                    mPsudoNextMonthRegionContainer.addView(secondWeekView);
                }
                                
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                mPsudoTargetMonthViewAtEventListExit.setPusdoLowerLisViewContainer(/*mFrameLayout, */mPsudoNextMonthRegionContainer);
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        	}
        	else {
        		mPsudoTargetMonthViewAtEventListExit.IsMonthWithLessThanSixWeeks(false);
        	}
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////                
    		   
    		if (mEventListViewExitAnimCompletionCallback == null)
    			mEventListViewExitAnimCompletionCallback = new EventListViewExitAnimCompletionCallback();
    		mPsudoTargetMonthViewAtEventListExit.setAnimCompletionCallback(mEventListViewExitAnimCompletionCallback);
    		
    		HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_NM_HEIGHT, normalModeMonthListViewHeight);    		
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_NM_MONTHINDICATOR_HEIGHT, normalModeMonthIndicatorHeight); 
    		
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_NM_NORMAL_WEEK_HEIGHT, normalModeNormalWeekHeight);    		
    		
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_EVM_MONTHINDICATOR_HEIGHT, eventlistModeMonthIndicatorHeight);    		
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_EVM_NORMAL_WEEK_HEIGHT, eventlistModeNormalWeekHeight);
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_RECOVERY_MONTH_HEIGHT, mRecoveryMonthHeight);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_FIRSTJULIANDAY_IN_MONTH, mCollapseEventViewInfo.mFirstDayJulianDayInMonth);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_WEEK_START, mFirstDayOfWeek);   
    		drawingParams.put(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_NUM_DAYS, 7);        		
    		mPsudoTargetMonthViewAtEventListExit.setMonthParam(drawingParams, mTzId);    		
    		mAdapter.sendEventsToView(mPsudoTargetMonthViewAtEventListExit);
    		
    		//int eventListViewHeight = expandEventsListView();
    		int parentViewHeight = mAnticipationMonthViewLayoutHeight;
    		int secondaryUpperActionBarHeight = mDayHeaderHeight; 
    		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    		int monthListViewVisualableRegionHeight = mListView.getEEMNormalWeekHeight() * weekNumbersOfMonth;
    		int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);	    	
    		int eventListViewHeight = parentViewHeight - (secondaryUpperActionBarHeight + monthListViewVisualableRegionHeight + lowerActionBarHeight);
    		
    		    		
    		mPsudoTargetMonthViewAtEventListExit.setEventListViewHeight(eventListViewHeight);
    		
    		mListView.setVisibility(View.GONE);
    		//if (mEventListModeExitPsudoMonthView.mLaunchPsudoListViewLowerWeek)
    			//mPsudoNextMonthRegionContainer.setVisibility(View.VISIBLE);    		
    		
    		mMonthViewLayout.addView(mPsudoTargetMonthViewAtEventListExit); 
        	
        	mExpandEventListViewState = ExpandEventListViewState.EventsViewModeExitAnimState;
        }
    };  
    
    public class EventListViewEntranceAnimCompletionCallback implements Runnable {
    	int mListViewScaleHeight = 0;
    	
    	public void setListViewScaleHeight(int height) {
    		//mListViewScaleHeight = height;    		
    	}
    	
        @Override
        public void run() {          	
        	  	
        	mListView.setExpandEventViewMode(true);//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        	mListView.setExpandEventViewModeHeight(mListView.getEEMNormalWeekHeight() * 6);
        	
        	mListView.setVisibility(View.INVISIBLE);
        	
        	// ExpandEventViewMode������ listview�� height�� �׻� mListView.getNormalWeekHeight() * 6 �̴�
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, mListView.getEEMNormalWeekHeight() * 6); 
			params.addRule(RelativeLayout.BELOW, mCalendarViewsSecondaryActionBarId);
								   
			mListView.setEEVMGlobalLayoutListener(mListViewFirstWeekTopAdjustmentScrollRunnable);
        	mListView.setLayoutParams(params);///////////////////////////////////////////////////////////////////////////////////////////////////////////////        	
        	mListView.setDrawSelectorOnTop(false);  
        	
        }
    };
    
    public class EventListViewExitAnimCompletionCallback implements Runnable {
    	int mListViewScaleHeight = 0;
    	
    	public void setListViewScaleHeight(int height) {
    		//mListViewScaleHeight = height;    		
    	}
    	
        @Override
        public void run() {        	
        	
        	mListView.setExpandEventViewMode(false);      
        	
        	mListView.setVisibility(View.INVISIBLE);  
        	
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mListView.getLayoutParams();
			params.height = mAnticipationMonthListViewHeight;			    
											   
			mListView.setRecoveryNMGlobalLayoutListener(mExpandEventListModeFinishedRunnable);
			mListView.setLayoutParams(params);///////////////////////////////////////////////////////////////////////////////////////////////////////////////    	
        	mListView.setDrawSelectorOnTop(false);  
        	mListView.setBackgroundColor(getResources().getColor(R.color.month_bgcolor));    
        	    	    	
        }
    };        
    
    public void ScaleEventListViewHeight() {
    	    	
		mTotalValue = mCurrentEventListHeight - mTargetEventListHeight;
		mTotalAbsValue = Math.abs(mTotalValue);		
		
		mScaleValueAnimator = ValueAnimator.ofInt(mCurrentEventListHeight, mTargetEventListHeight);
		
		if (mTotalValue > 0)
			mEventListViewAnimDuration = calculateDuration(mTotalAbsValue, mCurrentEventListHeight, DEFAULT_VELOCITY);
		else
			mEventListViewAnimDuration = calculateDuration(mTotalAbsValue, mTargetEventListHeight, DEFAULT_VELOCITY);
		
		mScaleValueAnimator.setInterpolator(new ScaleTimeInterpolator(mScaleValueAnimator, mTotalAbsValue));		
		mScaleValueAnimator.setDuration(mEventListViewAnimDuration);			

		mScaleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height				
				int value = (Integer) valueAnimator.getAnimatedValue();
				int eventListViewScaleHeight = value;							
				
				RelativeLayout.LayoutParams eventListViewParams = (RelativeLayout.LayoutParams) mEventsListView.getLayoutParams();
				eventListViewParams.height = eventListViewScaleHeight;
				mEventsListView.setLayoutParams(eventListViewParams);    				
			}
		});
		
		mScaleValueAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {				
			}

			@Override
			public void onAnimationStart(Animator animator) {	
				//mListView.smoothScrollBy(mListViewScrollValue, (int) mAnimDuration);
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		}); 		
		
    }	
    
    public class ExpandEventViewInfo {
    	Calendar mCalendar;
    	int mFirstDayJulianDayInMonth;
    	int mPosition;
    	
    	public ExpandEventViewInfo(Calendar calendar) {
    		mPosition = 0;
    		mFirstDayJulianDayInMonth = 0;    		
    		mCalendar = GregorianCalendar.getInstance(calendar.getTimeZone());
    		mCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
    		mCalendar.setTimeInMillis(calendar.getTimeInMillis());
    		mCalendar.set(Calendar.DAY_OF_MONTH, 1);   
    		// Time.getJulianDay�� 2nd Parameter : gmtoff -> the offset from UTC in "seconds"    		
    		// Calendar.ZONE_OFFSET : Field number for get and set indicating the raw offset from GMT in "milliseconds".
    		// UTC�� �׸���ġ ��ս�(GMT)�� �Ҹ��⵵ �ϴµ�, UTC�� GMT�� ���� �Ҽ��� ���������� ���̰� ���� ������ �ϻ󿡼��� ȥ��Ǿ� ���
    		mFirstDayJulianDayInMonth = ETime.getJulianDay(mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    	}
    	
    	public ExpandEventViewInfo(Calendar calendar, int position) {    		   		
    		mCalendar = GregorianCalendar.getInstance(calendar.getTimeZone());
    		mCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
    		mCalendar.setTimeInMillis(calendar.getTimeInMillis());
    		mCalendar.set(Calendar.DAY_OF_MONTH, 1);
    		mFirstDayJulianDayInMonth = ETime.getJulianDay(mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    		mPosition = position;
    	}    	
    }
    
    public class CollapseEventViewInfo {
    	Calendar mCalendar;
    	int mFirstDayJulianDayInMonth;
    	int mPosition;
    	
    	public CollapseEventViewInfo(Calendar calendar) {
    		mPosition = 0;
    		mFirstDayJulianDayInMonth = 0;    		
    		mCalendar = GregorianCalendar.getInstance(calendar.getTimeZone());
    		mCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
    		mCalendar.setTimeInMillis(calendar.getTimeInMillis());
    		mCalendar.set(Calendar.DAY_OF_MONTH, 1);   
    		// Time.getJulianDay�� 2nd Parameter : gmtoff -> the offset from UTC in "seconds"    		
    		// Calendar.ZONE_OFFSET : Field number for get and set indicating the raw offset from GMT in "milliseconds".
    		// UTC�� �׸���ġ ��ս�(GMT)�� �Ҹ��⵵ �ϴµ�, UTC�� GMT�� ���� �Ҽ��� ���������� ���̰� ���� ������ �ϻ󿡼��� ȥ��Ǿ� ���
    		mFirstDayJulianDayInMonth = ETime.getJulianDay(mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    	}
    	
    	public CollapseEventViewInfo(Calendar calendar, int position) {    		   		
    		mCalendar = GregorianCalendar.getInstance(calendar.getTimeZone());
    		mCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
    		mCalendar.setTimeInMillis(calendar.getTimeInMillis());
    		mCalendar.set(Calendar.DAY_OF_MONTH, 1);
    		mFirstDayJulianDayInMonth = ETime.getJulianDay(mCalendar.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    		mPosition = position;
    	}    	
    }  
    
    
    
    
    private final Runnable goToTodayflingRunInNMode = new Runnable() {
        public void run() { 
        	doGoToTodayFlingInNMode();
        }
    };
    
    int mGoToTodayDirection = -1;
    private void doGoToTodayFlingInNMode() {
    	
        if (mGoToTodayDirection == DOWNWARD_GO_TO_TODAY || mGoToTodayDirection == DOWNWARD_GO_TO_TODAY_BUT_NOT_EVENT_LOAD) {
        	// downward fling              	
        	computeGoToTodayDownwardFling();
        }
        else {        	
        	// upward fling         	
        	computeGoToTodayUpwardFling();      	
        }  
        
        if (mGoToTodayDirection == DOWNWARD_GO_TO_TODAY || mGoToTodayDirection == UPWARD_GO_TO_TODAY)
        	updateLoaderInNMode(ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
    }  
    
    
    private void computeGoToTodayDownwardFling() {
    			
    	UnVisibleMonthPartOfVisibleFirstWeek obj = calcUnvisibleMonthPartOfVisibleFirstWeek();
    	int accumulatedDistance = obj.mUnvisibleMonthHeight;    	        
    	
    	Calendar weekTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
    	weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        weekTimeCalendar.setTimeInMillis(obj.mCompletionMonthMillis);       
        weekTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);       
        
    	Calendar toDayCalendar = GregorianCalendar.getInstance(mTimeZone);     
    	toDayCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());    	
    	
    	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {
    		if (INFO) Log.i(TAG, "computeGoToTodayDownwardFling:total delta=" + String.valueOf(accumulatedDistance));
    		
    		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
        	
    		float delta = flingObj.mDistance;
        	float maxDelta = delta * 2; 
        	long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);
    		
        	excuteGoToTodayWorkInNMode((int)-delta, duration);       	
        	
    		return;
    	}   
    	
        
        boolean continuing = true;
    	while (continuing) {        		        
        	weekTimeCalendar.add(Calendar.MONTH, -1);  
        	
        	int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);  
        	
        	int monthHeight = mListView.getNMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
        	
        	accumulatedDistance = accumulatedDistance + monthHeight;
        	
        	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {  
        		if (INFO) Log.i(TAG, "computeGoToTodayDownwardFling:total delta=" + String.valueOf(accumulatedDistance));
        		
        		
        		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
            	
        		float delta = flingObj.mDistance;
            	float maxDelta = delta * 2; 
            	long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);
        		
            	excuteGoToTodayWorkInNMode((int)-delta, duration);
            	
                continuing = false;        		
        	}           
        }    	
    }
    
    private void computeGoToTodayUpwardFling() { 	
				
		MonthDimensionBelowVisibleLastWeek obj = calcMonthDimensionBelowVisibleLastWeek();		
    	int accumulatedDistance = obj.mMonthHeightBelowVisibleLastWeek;    	
    	
    	
    	Calendar weekTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
        weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        weekTimeCalendar.setTimeInMillis(obj.mCompletionMonthMillis);       
        weekTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);  
        int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        int monthHeight = mListView.getNMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
        
        Calendar toDayCalendar = GregorianCalendar.getInstance(mTimeZone);     
    	toDayCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());     	
    	
    	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {    		
    		
    		accumulatedDistance = obj.mVisibleLastWeekTop + accumulatedDistance - monthHeight;    	    		
    		
    		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
    		
    		float delta = flingObj.mDistance;
    		float maxDelta = delta * 2; //float maxDelta = delta; 
    		long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);
    		
        	excuteGoToTodayWorkInNMode((int)delta, duration);
        	
    		return;
    	}    	
        
        boolean continuing = true;        
        
        while (continuing) {
        	weekTimeCalendar.add(Calendar.MONTH, 1); 
        	        	
        	weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        	
        	monthHeight = mListView.getNMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
        	
        	accumulatedDistance = accumulatedDistance + monthHeight;
        	
        	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {        		
        		
        		accumulatedDistance = obj.mVisibleLastWeekTop + accumulatedDistance - monthHeight;         		
        		
        		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
            	
        		float delta = flingObj.mDistance;
        		float maxDelta = delta * 2; //float maxDelta = delta; 
        		long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);
        		
            	excuteGoToTodayWorkInNMode((int)delta, duration);
            	
                continuing = false;                
        	}           
        }         	
    } 
    
    private UnVisibleMonthPartOfVisibleFirstWeek calcUnvisibleMonthPartOfVisibleFirstWeek() {
    	int monthIndicatorHeight = mListView.getNMMonthIndicatorHeight();
    	MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);		
    	//int firstJulianDay = child.getFirstJulianDay();
		//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();		
		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);		
		
		UnVisibleMonthPartOfVisibleFirstWeek obj = null;
		int visibleMonthHeight = 0;
		int unVisibleMonthHeight = 0;
    	if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {   
    		
    		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    		mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
    		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    		
    		/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
        	int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);    		
        	int remainingWeeks = remainingMonthDays / 7;        	
        	int lastWeekDays = remainingMonthDays % 7;       	
            if (lastWeekDays > 0) { // ��κ��� �̷� ���̴�
            	++remainingWeeks;            	
            }*/
            int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, false);	  
            
            visibleMonthHeight = child.getBottom();
            visibleMonthHeight = visibleMonthHeight + (remainingWeeks * mListView.getNMNormalWeekHeight());  
            
            TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
            
			unVisibleMonthHeight = monthHeight - visibleMonthHeight;
    	}
    	else {
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {          		
        		int lastWeekHeightOfPrvMonth = mListView.getNMNormalWeekHeight();
        		int topFromListView = Math.abs(child.getTop()); // visbile first week�� getTop ������ (+)���� ���� �� ���� : 0 or (-)��
        		                                                // (-)���� ����ؼ� ���밪���� ó���Ѵ�
        		
        		if (topFromListView < lastWeekHeightOfPrvMonth) {
        			
        			// ó���ؾ� �� �κ��� prv month���� �ǹ��Ѵ� : next month�� first week�� �Ű� �� �ʿ䰡 ����
        			// -prv month�� last week �κ��� listview�� top line�� ���� ������ �ǹ��Ѵ�
        			//  :�� getTop �κа� getBottom �� ������ ��еǾ� ������ �ǹ��Ѵ�        			
        			visibleMonthHeight = child.getBottom() - mListView.getNMFirstWeekHeight();
        			
        			//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        			mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
            		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
        			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        			
        			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
        			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
        			unVisibleMonthHeight = monthHeight - visibleMonthHeight;
        			       			
        		}else {
        			
        			// ó���ؾ� �� �κ��� next month�� first week���� �ǹ��Ѵ�  
        			// -next month�� (month indicator + first week) ������ listview�� top line�� ���� ������ �ǹ��Ѵ�
        			// �Ʒ�ó�� prv month�� last week ������ ����� �Ѵ�
        			unVisibleMonthHeight = topFromListView - lastWeekHeightOfPrvMonth;
        			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
        			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
            		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
        			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);		     			
        		}              		
        	}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
        		
        		int topFromListView = Math.abs(child.getTop());
        		unVisibleMonthHeight = topFromListView;
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
        		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);		 
        			
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {  
        		
        		int topFromListView = Math.abs(child.getTop());
        		unVisibleMonthHeight = topFromListView;
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
        		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);	    			
        	}
    	}  
    	
    	long completionMonthMillis = TempCalendar.getTimeInMillis();
        obj = new UnVisibleMonthPartOfVisibleFirstWeek(unVisibleMonthHeight, completionMonthMillis);
        
    	return obj;
    }    
    
    
    private MonthDimensionBelowVisibleLastWeek calcMonthDimensionBelowVisibleLastWeek() {
    	
    	int monthIndicatorHeight = mListView.getNMMonthIndicatorHeight();
    	
    	int firstVisiblePosition = mListView.getFirstVisiblePosition();
    	int lastVisiblePosition = mListView.getLastVisiblePosition();
    	int lastChildViewIndex = lastVisiblePosition - firstVisiblePosition;    	
    	
    	MonthWeekView child = (MonthWeekView) mListView.getChildAt(lastChildViewIndex);
    	
    	//int firstJulianDay = child.getFirstJulianDay();
    	//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();	
		 
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek); 		
		
		int childTop = -1;		
		int unVisibleMonthHeight = 0;		
		
		if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {
					
			childTop = child.getTop();			
			unVisibleMonthHeight = mListView.getNMNormalWeekHeight();
			
			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
			
			/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
        	int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);     		
        	int remainingWeeks = remainingMonthDays / 7;        	
        	int lastWeekDays = remainingMonthDays % 7;       	
            if (lastWeekDays > 0) { // ��κ��� �̷� ���̴�
            	remainingWeeks++;            	
            }*/
            int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, true);	  
            
            unVisibleMonthHeight = unVisibleMonthHeight + (remainingWeeks * mListView.getNMNormalWeekHeight()); 
            TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    	}
		else {
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) { 
    			   			
    			childTop = child.getTop();			
    			unVisibleMonthHeight = mListView.getNMNormalWeekHeight(); // prv month week height
    			
    			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
    			
    			unVisibleMonthHeight = unVisibleMonthHeight + monthHeight;        
        		
        	}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
        		
        		// month height���� ���� visible ������ ���������ν� unvisible month height�� ����� �� �ִ�
        		childTop = child.getTop();	
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay); // firstJulianDay�� 1�� ���̴�
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getNMNormalWeekHeight());
    			unVisibleMonthHeight = monthHeight;        			
        		      		
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {  
        		
        		childTop = child.getTop();	
        		unVisibleMonthHeight = mListView.getNMNormalWeekHeight();
        		
        		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);    			
        	}
    	} 		
		
		MonthDimensionBelowVisibleLastWeek obj = new MonthDimensionBelowVisibleLastWeek(childTop, unVisibleMonthHeight, TempCalendar.getTimeInMillis());
		return obj;
    }
    
    
    private final Runnable goToTodayflingRunInEEMode = new Runnable() {
        public void run() { 
        	doGoToTodayFlingInEEMode();
        }
    };
    
    
    private void doGoToTodayFlingInEEMode() {
    	
        if (mGoToTodayDirection == DOWNWARD_GO_TO_TODAY) {
        	// downward fling              	
        	computeGoToTodayDownwardFlingInEEMode();
        }
        else {        	
        	// upward fling         	
        	computeGoToTodayUpwardFlingInEEMode();      	
        }  
        
        updateLoaderInEEMode(ETime.getJulianDay(mCurrentMonthTimeDisplayed.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
    }  
    
    private void computeGoToTodayDownwardFlingInEEMode() {
		
    	UnVisibleMonthPartOfVisibleFirstWeek obj = calcUnvisibleMonthPartOfVisibleFirstWeekInEEMode();
    	int accumulatedDistance = obj.mUnvisibleMonthHeight;    	
        
    	
    	Calendar weekTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
    	weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        weekTimeCalendar.setTimeInMillis(obj.mCompletionMonthMillis);       
        weekTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);       
        
    	Calendar toDayCalendar = GregorianCalendar.getInstance(mTimeZone);     
    	toDayCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());    	
    	
    	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {
    		mTempTime.setTimeInMillis(System.currentTimeMillis());
    		int targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    		///////////////////////////////////////////////////////////////////////////////////////
    		mAdapter.setSelectedDayInEEMode(mTempTime.getTimeInMillis());
    		///////////////////////////////////////////////////////////////////////////////////////		
			
    		accumulatedDistance = accumulatedDistance - mListView.getEEMMonthIndicatorHeight();
    		
    		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
        	
    		float delta = flingObj.mDistance;
        	float maxDelta = delta * 2;              	
        	
        	mCurrentEventListHeight = mEventsListView.getHeight();
        	int weekNumbersOfXXXMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
    		int targetMonthHeight = weekNumbersOfXXXMonth * mListView.getEEMNormalWeekHeight();
    		mTargetEventListHeight = mListView.getOriginalListViewHeight() - targetMonthHeight;
    		
    		boolean mustScaleEventListViewHeight = false;
    		if (mCurrentEventListHeight != mTargetEventListHeight) {   	
    			mustScaleEventListViewHeight = true;
    			ScaleEventListViewHeight();    			
    		}
    		
    		long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);    		
    		
    		mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);   		
    		
    		excuteGoToTodayWorkInEEMode((int)-delta, duration, mustScaleEventListViewHeight);  
    		
    		return;
    	}   
    	
        
        boolean continuing = true;
    	while (continuing) {        		        
        	weekTimeCalendar.add(Calendar.MONTH, -1);  
        	
        	int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);  
        	
        	int monthHeight = mListView.getEEMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
        	
        	accumulatedDistance = accumulatedDistance + monthHeight;
        	
        	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {  
        		mTempTime.setTimeInMillis(System.currentTimeMillis());
        		int targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        		///////////////////////////////////////////////////////////////////////////////////////
        		mAdapter.setSelectedDayInEEMode(mTempTime.getTimeInMillis());
        		///////////////////////////////////////////////////////////////////////////////////////		
        		
        		accumulatedDistance = accumulatedDistance - mListView.getEEMMonthIndicatorHeight();
        		
        		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
            	
        		float delta = flingObj.mDistance;
            	float maxDelta = delta * 2; 
        		
            	mCurrentEventListHeight = mEventsListView.getHeight();
            	int weekNumbersOfXXXMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        		int targetMonthHeight = weekNumbersOfXXXMonth * mListView.getEEMNormalWeekHeight();
        		mTargetEventListHeight = mListView.getOriginalListViewHeight() - targetMonthHeight;
        		
        		boolean mustScaleEventListViewHeight = false;
        		if (mCurrentEventListHeight != mTargetEventListHeight) {   	
        			mustScaleEventListViewHeight = true;
        			ScaleEventListViewHeight();    			
        		}
        		
        		long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);    		
        		
        		mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);   		
        		
        		excuteGoToTodayWorkInEEMode((int)-delta, duration, mustScaleEventListViewHeight);
        		            		
                continuing = false;        		
        	}           
        }    	
    }
    
    private void computeGoToTodayUpwardFlingInEEMode() { 	
				
		MonthDimensionBelowVisibleLastWeek obj = calcMonthDimensionBelowVisibleLastWeekInEEMode();		
    	int accumulatedDistance = obj.mMonthHeightBelowVisibleLastWeek;    	
    	    	
    	Calendar weekTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
        weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        weekTimeCalendar.setTimeInMillis(obj.mCompletionMonthMillis);       
        weekTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);  
        int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        int monthHeight = mListView.getEEMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
        
        Calendar toDayCalendar = GregorianCalendar.getInstance(mTimeZone);     
    	toDayCalendar.setTimeInMillis(mCurrentMonthTimeDisplayed.getTimeInMillis());     	
    	
    	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) {    		
    		mTempTime.setTimeInMillis(System.currentTimeMillis());
    		int targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    		///////////////////////////////////////////////////////////////////////////////////////
    		mAdapter.setSelectedDayInEEMode(mTempTime.getTimeInMillis());
    		///////////////////////////////////////////////////////////////////////////////////////		
    		
    		accumulatedDistance = obj.mVisibleLastWeekTop + accumulatedDistance - monthHeight;  
    		accumulatedDistance = accumulatedDistance + mListView.getEEMMonthIndicatorHeight();
    		
    		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
    		
    		float delta = flingObj.mDistance;
    		float maxDelta = delta * 2; //float maxDelta = delta; 
    		
    		mCurrentEventListHeight = mEventsListView.getHeight();
        	int weekNumbersOfXXXMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
    		int targetMonthHeight = weekNumbersOfXXXMonth * mListView.getEEMNormalWeekHeight();
    		mTargetEventListHeight = mListView.getOriginalListViewHeight() - targetMonthHeight;
    		
    		boolean mustScaleEventListViewHeight = false;
    		if (mCurrentEventListHeight != mTargetEventListHeight) {   	
    			mustScaleEventListViewHeight = true;
    			ScaleEventListViewHeight();    			
    		}
    		
    		long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);    		
    		
    		mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);   		
    		
    		excuteGoToTodayWorkInEEMode((int)delta, duration, mustScaleEventListViewHeight);
    		
    		return;
    	}    	
        
        boolean continuing = true;        
        
        while (continuing) {
        	weekTimeCalendar.add(Calendar.MONTH, 1); 
        	        	
        	weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        	
        	monthHeight = mListView.getEEMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
        	
        	accumulatedDistance = accumulatedDistance + monthHeight;
        	
        	if (toDayCalendar.compareTo(weekTimeCalendar) == 0) { 
        		mTempTime.setTimeInMillis(System.currentTimeMillis());
        		int targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        		///////////////////////////////////////////////////////////////////////////////////////
        		mAdapter.setSelectedDayInEEMode(mTempTime.getTimeInMillis());
        		///////////////////////////////////////////////////////////////////////////////////////	       		
        		
        		accumulatedDistance = obj.mVisibleLastWeekTop + accumulatedDistance - monthHeight;   
        		accumulatedDistance = accumulatedDistance + mListView.getEEMMonthIndicatorHeight();
        		
        		FlingParameter flingObj = new FlingParameter(GO_TO_TODAY_VELOCITY, accumulatedDistance);
            	
        		float delta = flingObj.mDistance;
        		float maxDelta = delta * 2; //float maxDelta = delta; 
        		
        		mCurrentEventListHeight = mEventsListView.getHeight();
            	int weekNumbersOfXXXMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        		int targetMonthHeight = weekNumbersOfXXXMonth * mListView.getEEMNormalWeekHeight();
        		mTargetEventListHeight = mListView.getOriginalListViewHeight() - targetMonthHeight;
        		
        		
        		boolean mustScaleEventListViewHeight = false;
        		if (mCurrentEventListHeight != mTargetEventListHeight) {   	
        			mustScaleEventListViewHeight = true;
        			ScaleEventListViewHeight();    			
        		}
        		
        		long duration = MonthListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity);    		
        		
        		mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);   		
        		
        		excuteGoToTodayWorkInEEMode((int)delta, duration, mustScaleEventListViewHeight);
        		            		
                continuing = false;                
        	}           
        }         	
    } 
    
    int mFlingDirection;
	int mFlingDistance;
	long mDurationTimeMillis;
	long mStartTimeMillis;
	int mPrvAcculmulatedFlingDistance;
	
	public void setNormalModeFlingContext(int direction, int flingDistance, long durationTime, long startTime) {
		mFlingDirection = direction;
		mFlingDistance = flingDistance;
		mDurationTimeMillis = durationTime;
		mStartTimeMillis = startTime;
		mPrvAcculmulatedFlingDistance = 0;	
		
		mListViewState = ListViewState.Run_FlingState;
		if (INFO) Log.i(TAG, "mFlingDistance=" + String.valueOf(mFlingDistance));
	}
	
	public void stopFlingComputation() {
		if (mNormalModeFlingComputationRunnable != null) {
			//mListView.removeCallbacks(mNormalModeFlingComputationRunnable);
			mHandler.removeCallbacks(mNormalModeFlingComputationRunnable);
		}
		
		mListViewState = ListViewState.Run;
			
		mListView.smoothScrollBy(0, 0);	
	}
	
	public float getInterpolation(float t) {
		
        t -= 1.0f;
        t = t * t * t * t * t + 1;
        
        if ((1 - t) * mFlingDistance < 1) {
            t = 1;
        }
        
        return t;
    }	
	
	public int calcAccumulatedDistance() {
		long curTimeMillis = System.currentTimeMillis();
		float elapsedTimeMillis = curTimeMillis - mStartTimeMillis;
		
		float totalTime = mDurationTimeMillis;
		
		float normaledTimeRate = elapsedTimeMillis / totalTime;
		if (normaledTimeRate > 1) {
			if (INFO) Log.i(TAG, "calcAccumulatedDistance:OVER 1 TIME RATE!!!!");
		}
		
		float timeRate = getInterpolation(normaledTimeRate);
		
		int accumulatedFlingDistance = (int) (mFlingDistance * timeRate);		
		
		return accumulatedFlingDistance;
	}
	
	public void computeFlingInNMode() {
				
		if (mPrvAcculmulatedFlingDistance == mFlingDistance) {
			
			mListViewState = ListViewState.Run;
			mListView.smoothScrollBy(0, 0);	
			return;
		}
		
		int accumulatedFlingDistance = calcAccumulatedDistance();		
		if (accumulatedFlingDistance > mFlingDistance) {			
			accumulatedFlingDistance = mFlingDistance;					
		}
		
		int delta = mPrvAcculmulatedFlingDistance - accumulatedFlingDistance;					
		
		if (mFlingDirection == MonthListView.UPWARD_FLING_NORMAL_MODE) {						
			
			int firstVisiblePosition = mListView.getFirstVisiblePosition();
	    	int lastVisiblePosition = mListView.getLastVisiblePosition();
	    	int lastChildViewIndex = lastVisiblePosition - firstVisiblePosition;    	
	    	
	    	MonthWeekView lastVisibleWeekChild = (MonthWeekView) mListView.getChildAt(lastChildViewIndex);				
			
			int lastChildTop = lastVisibleWeekChild.getTop();
			lastChildTop = lastChildTop + delta;					
					
			mListView.setSelectionFromTop(lastVisiblePosition, lastChildTop);					
		}
		else {
			delta = Math.abs(delta);			
			
			int firstVisiblePosition = mListView.getFirstVisiblePosition();			
			MonthWeekView firstVisibleWeekChild = (MonthWeekView)mListView.getChildAt(0); 	
			
			int firstChildTop = firstVisibleWeekChild.getTop();				
			firstChildTop = firstChildTop + delta;		
			
			mListView.setSelectionFromTop(firstVisiblePosition, firstChildTop);			
		}
					
		mPrvAcculmulatedFlingDistance = accumulatedFlingDistance;
	}
	
	private class NormalModeFlingComputationRunnable implements Runnable {

		@Override
		public void run() {			
			computeFlingInNMode();
		}
		
	}
	
	private class EventListViewScaleHeightRunnable implements Runnable {

		@Override
		public void run() {			
			mScaleValueAnimator.start();
		}
		
	}
	
	
	public void kickOffFlingComputationRunnable() {		
		NormalModeFlingComputationRunnable kickOff = new NormalModeFlingComputationRunnable();		
		mHandler.post(kickOff);
		//mListView.post(kickOff);
	}
	
	
	NormalModeFlingComputationRunnable mNormalModeFlingComputationRunnable = null;
	public void postFlingComputationRunnable() {
		mNormalModeFlingComputationRunnable = new NormalModeFlingComputationRunnable();		
		mHandler.post(mNormalModeFlingComputationRunnable);
		//mListView.post(mNormalModeFlingComputationRunnable);
	}
    
    private UnVisibleMonthPartOfVisibleFirstWeek calcUnvisibleMonthPartOfVisibleFirstWeekInEEMode() {
    	
    	MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);		
    	//int firstJulianDay = child.getFirstJulianDay();
		//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();		
		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);		
		
		UnVisibleMonthPartOfVisibleFirstWeek obj = null;
		int visibleMonthHeight = 0;
		int unVisibleMonthHeight = 0;
    	if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {   
    		
    		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    		mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
    		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    		
    		/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
        	int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);
        	int remainingWeeks = remainingMonthDays / 7;        	
        	int lastWeekDays = remainingMonthDays % 7;       	
            if (lastWeekDays > 0) { // ��κ��� �̷� ���̴�
            	++remainingWeeks;            	
            }*/
            int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, false);	  
            
            visibleMonthHeight = child.getBottom();
            visibleMonthHeight = visibleMonthHeight + (remainingWeeks * mListView.getEEMNormalWeekHeight());  
            
            TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
			int monthHeight = mListView.getEEMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
            
			unVisibleMonthHeight = monthHeight - visibleMonthHeight;
    	}
    	else {
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {          		
        		int lastWeekHeightOfPrvMonth = mListView.getEEMNormalWeekHeight();
        		int topFromListView = Math.abs(child.getTop()); // visbile first week�� getTop ������ (+)���� ���� �� ���� : 0 or (-)��
        		                                                // (-)���� ����ؼ� ���밪���� ó���Ѵ�
        		
        		if (topFromListView < lastWeekHeightOfPrvMonth) {
        			
        			// ó���ؾ� �� �κ��� prv month���� �ǹ��Ѵ� : next month�� first week�� �Ű� �� �ʿ䰡 ����
        			// -prv month�� last week �κ��� listview�� top line�� ���� ������ �ǹ��Ѵ�
        			//  :�� getTop �κа� getBottom �� ������ ��еǾ� ������ �ǹ��Ѵ�        			
        			visibleMonthHeight = child.getBottom() - mListView.getEEMFirstWeekHeight();
        			
        			//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        			mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
            		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
        			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        			
        			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
        			int monthHeight = mListView.getEEMMonthIndicatorHeight() + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
        			unVisibleMonthHeight = monthHeight - visibleMonthHeight;
        			       			
        		}else {
        			
        			// ó���ؾ� �� �κ��� next month�� first week���� �ǹ��Ѵ�  
        			// -next month�� (month indicator + first week) ������ listview�� top line�� ���� ������ �ǹ��Ѵ�
        			// �Ʒ�ó�� prv month�� last week ������ ����� �Ѵ�
        			unVisibleMonthHeight = topFromListView - lastWeekHeightOfPrvMonth;
        			
        			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
        			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
            		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
        			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);		     			
        		}              		
        	}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
        		
        		int topFromListView = Math.abs(child.getTop());
        		unVisibleMonthHeight = topFromListView;
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
        		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);		 
        			
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {  
        		
        		int topFromListView = Math.abs(child.getTop());
        		unVisibleMonthHeight = topFromListView;
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
        		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);	    			
        	}
    	}  
    	
    	long completionMonthMillis = TempCalendar.getTimeInMillis();
        obj = new UnVisibleMonthPartOfVisibleFirstWeek(unVisibleMonthHeight, completionMonthMillis);
        
    	return obj;
    }    
    
    
    private MonthDimensionBelowVisibleLastWeek calcMonthDimensionBelowVisibleLastWeekInEEMode() {
    	
    	int monthIndicatorHeight = mListView.getEEMMonthIndicatorHeight();
    	
    	int firstVisiblePosition = mListView.getFirstVisiblePosition();
    	int lastVisiblePosition = mListView.getLastVisiblePosition();
    	int lastChildViewIndex = lastVisiblePosition - firstVisiblePosition;    	
    	
    	MonthWeekView child = (MonthWeekView) mListView.getChildAt(lastChildViewIndex);
    	
    	//int firstJulianDay = child.getFirstJulianDay();
    	//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();	
		 
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek); 		
		
		int childTop = -1;		
		int unVisibleMonthHeight = 0;		
		
		if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {
					
			childTop = child.getTop();			
			unVisibleMonthHeight = mListView.getEEMNormalWeekHeight();
			
			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
			
			/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
        	int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);       		
        	int remainingWeeks = remainingMonthDays / 7;        	
        	int lastWeekDays = remainingMonthDays % 7;       	
            if (lastWeekDays > 0) { // ��κ��� �̷� ���̴�
            	remainingWeeks++;            	
            }*/
            int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, false);	  
            
            unVisibleMonthHeight = unVisibleMonthHeight + (remainingWeeks * mListView.getEEMNormalWeekHeight()); 
            TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    	}
		else {
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) { 
    			   			
    			childTop = child.getTop();			
    			unVisibleMonthHeight = mListView.getEEMNormalWeekHeight(); // prv month week height
    			
    			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
    			
    			unVisibleMonthHeight = unVisibleMonthHeight + monthHeight;        
        		
        	}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
        		
        		// month height���� ���� visible ������ ���������ν� unvisible month height�� ����� �� �ִ�
        		childTop = child.getTop();	
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay); // firstJulianDay�� 1�� ���̴�        		
        		
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * mListView.getEEMNormalWeekHeight());
    			unVisibleMonthHeight = monthHeight;        			
        		      		
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {  
        		
        		childTop = child.getTop();	
        		unVisibleMonthHeight = mListView.getEEMNormalWeekHeight();
        		
        		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);    			
        	}
    	} 		
		
		MonthDimensionBelowVisibleLastWeek obj = new MonthDimensionBelowVisibleLastWeek(childTop, unVisibleMonthHeight, TempCalendar.getTimeInMillis());
		return obj;
    }
  
    public void setEventDayListsToApp() {
    	mApp.setEventDayList(mAdapter.getEventDayList());
    	
    	mApp.setEventDayListFirstJulianDay(mFirstLoadedJulianDay);
    	mApp.setEventDayListLastJulianDay(mLastLoadedJulianDay);
    }    
    
    
    public void calcFrameLayoutHeight() {
    	int deviceHeight = getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = mActivity.getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	int StatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
    	// upper action bar : 48dip ->calendar_view_upper_actionbar_height
    	// day header : 16dip ->day_header_height
    	// lower action bar : 48dip ->calendar_view_lower_actionbar_height
    	//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ?, getResources().getDisplayMetrics());
    	int upperActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_upper_actionbar_height);    	
    	mDayHeaderHeight = (int)getResources().getDimension(R.dimen.day_header_height);
    	int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);
    	
    	mAnticipationMonthViewLayoutHeight = deviceHeight - StatusBarHeight - upperActionBarHeight;
    	
    	mAnticipationMonthListViewWidth = rectgle.right - rectgle.left;
    	mAnticipationMonthListViewHeight = mAnticipationMonthViewLayoutHeight - mDayHeaderHeight - lowerActionBarHeight;   	        	
    	//mMonthListViewTopOffset = mAnticipationMonthListViewHeight * MonthWeekView.MONTH_LIST_VIEW_TOP_OFFSET;
    	
    	mAnticipationYearListViewWidth = mAnticipationMonthListViewWidth;
    	mAnticipationYearListViewHeight = deviceHeight - StatusBarHeight - upperActionBarHeight - lowerActionBarHeight; 	
    	
    	
    	mMonthListViewMonthIndicatorHeight = (int) (mAnticipationMonthListViewHeight * MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT);
    	
    	mMonthListViewNormalWeekHeight = (int) (mAnticipationMonthListViewHeight * MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT);
    	    	
    	mMonthListMonthDayTextBaselineY = mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT;
    	
    	float monthListViewHeightForCalcingMiniMonthHeight = mMonthListViewMonthIndicatorHeight +
    			(mMonthListViewNormalWeekHeight * 5) +
    			mMonthListMonthDayTextBaselineY;    	
    	
    	mMonthListViewMonthIndicatorTextBaselineY = mMonthListViewMonthIndicatorHeight * MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT;    	
    	
    	mMonthListMonthDayTextHeight = mAnticipationMonthListViewHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT;
    	mMonthListViewMonthIndicatorTextHeight = mMonthListMonthDayTextHeight;
    	
    	mAnticipationYearIndicatorHeight = (int) (mAnticipationYearListViewHeight * YearPickerFragment.YEARINDICATOR_HEIGHT_RATIO_BY_LISTVIEW_HEIGHT);    	
    	mAnticipationYearIndicatorTextHeight = (int) (mAnticipationYearIndicatorHeight * YearPickerFragment.YEARINDICATOR_TEXT_HEIGHT_RATIO_BY_YEARINDICATOR_HEIGHT);
    	mAnticipationYearIndicatorTextBaseLineY = (int) (mAnticipationYearIndicatorHeight * YearPickerFragment.YEARINDICATOR_TEXT_BASELINE_Y_RATIO_BY_YEARINDICATOR_HEIGHT);
    	
    	mAnticipationMiniMonthHeight = (mAnticipationYearListViewHeight - mAnticipationYearIndicatorHeight) / 4;
    	//mAnticipationMiniMonthHeight = (int) (monthListViewHeightForCalcingMiniMonthHeight * YearPickerFragment.MINIMONTH_HEIGHT_RATIO_BY_MONTH_LISTVIEW_HEIGHT);
    	mAnticipationMiniMonthIndicatorTextHeight = (int) (mMonthListViewMonthIndicatorTextHeight * 
    			YearPickerFragment.MINIMONTH_INDICATOR_TEXT_HEIGHT_RATIO_BY_MONTH_LISTVIEW_MONTHINDICATOR_TEXT_HEIGHT);
    	mAnticipationMiniMonthIndicatorTextBaseLineY = (int) (mMonthListViewMonthIndicatorTextBaselineY * 
    			YearPickerFragment.MINIMONTH_INDICATOR_TEXT_BASELINE_Y_RATIO_BY_MONTH_LISTVIEW_MONTHINDICATOR_TEXT_BASELINE_Y);	
    	mAnticipationMiniMonthDayTextBaseLineY = (int) (mMonthListMonthDayTextBaselineY * 
    			YearPickerFragment.MINIMONTH_TEXT_BASELINE_Y_RATIO_BY_MONTH_LISTVIEW_MONTH_TEXT_BASELINE_Y);
    	mAnticipationMiniMonthDayTextHeight = (int) (mMonthListMonthDayTextHeight * YearPickerFragment.MINIMONTH_TEXT_HEIGHT_RATIO_BY_MONTH_LISTVIEW_MONTH_TEXT_HEIGHT);
    	
    	mAnticipationMiniMonthWidth = (int) (mAnticipationYearListViewWidth * YearPickerFragment.MINIMONTH_WIDTH_RATIO_BY_LISTVIEW_WIDTH);
    	mAnticipationMiniMonthLeftMargin = (int) (mAnticipationYearListViewWidth * YearPickerFragment.MINIMONTH_LEFTMARGIN_RATIO_BY_LISTVIEW_WIDTH);     	
    	    	
    	mMonthListViewNormalWeekHeightInEPMode = (int) (mMonthListViewNormalWeekHeight * MonthListView.EEVM_WEEK_HEIGHT_RATIO_BY_NM_WEEK_HEIGHT);    
    	mAnticipationMonthListViewHeightInEPMode = mMonthListViewNormalWeekHeightInEPMode * 6;
    	    	
    	mMonthListViewMonthIndicatorHeightInEPMode = (int) (mMonthListViewMonthIndicatorHeight * MonthListView.EEVM_MONTHINDICATOR_HEIGHT_RATIO_BY_NM_MONTHINDICATOR_HEIGHT);        
    	mMonthListViewMonthIndicatorTextHeightInEPMode = mMonthListViewMonthIndicatorTextHeight;    	
    	mMonthListViewMonthIndicatorTextBaselineYInEPMode = mMonthListViewMonthIndicatorHeightInEPMode * MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT;     	
        mMonthListMonthDayTextHeightInEPMode = mMonthListMonthDayTextHeight;        
    	mMonthListMonthDayTextBaselineYInEPMode = mMonthListViewNormalWeekHeightInEPMode * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT;    
    }
        
    public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
    
    
    private static final int MINIMUM_VELOCITY = 2200;    
	private static final int DEFAULT_VELOCITY = MINIMUM_VELOCITY; // ������ ��Ʈ
    //private static final int DEFAULT_VELOCITY = 5500; // ������ S4
    private static long calculateDuration(float delta, float width, float velocity) {
        
    	/*
         * Here we compute a "distance" that will be used in the computation of
         * the overall snap duration. This is a function of the actual distance
         * that needs to be traveled; we keep this value close to half screen
         * size in order to reduce the variance in snap duration as a function
         * of the distance the page needs to travel.
         */
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        velocity = Math.max(MINIMUM_VELOCITY, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }    
    
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
    
    private static class ScaleTimeInterpolator implements TimeInterpolator {
		ValueAnimator mAnimator;
    	float mAnimationDistance;
    	
        public ScaleTimeInterpolator(ValueAnimator animator, float animationDistance) {
        	mAnimator = animator;
        	mAnimationDistance = animationDistance;
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
        
        private void cancelAnimation() {        	
        	mAnimator.setDuration(0);        	     	
        }
    }
    
}
