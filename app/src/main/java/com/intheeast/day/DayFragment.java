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

package com.intheeast.day;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;

import android.app.Activity;
import android.app.Fragment;

import android.content.Context;

import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseIntArray;

import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import android.animation.Animator;
import android.animation.ValueAnimator;

import com.intheeast.day.ExtendedDayView.ExtendedDayViewDimension;

import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar.HScrollInterpolator;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar.OnStartDayViewSnapAnimationListener;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarViewsLowerActionBar;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.CalendarViewsUpperActionBarFragment;
import com.intheeast.acalendar.CentralActivity;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.EventInfoLoader;
import com.intheeast.acalendar.EventInfoViewUpperActionBarFragment;
import com.intheeast.acalendar.EventLoader;
import com.intheeast.acalendar.FastEventInfoFragment;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.SelectedEventRect;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;

import com.intheeast.etc.CommonRoundedCornerLinearLayout;
import com.intheeast.etc.ETime;
import com.intheeast.event.EventTimeModificationManager;

import com.intheeast.settings.SettingsFragment;

/**
 * This is the base class for Day and Week Activities.
 */
@SuppressLint("ValidFragment")
public class DayFragment extends /*CalendarViewBaseFragment*/Fragment implements CalendarController.EventHandler,
	FastEventInfoFragment.OnCallerEnterAnimationEndListener, 
	 OnTouchListener/*, CalendarViewsSecondaryActionBar.OnStartDayViewSnapAnimationListener*/ {
	
	private static String TAG = "DayFragment";
    private static boolean INFO = true;
	
	/**
     * The view id used for all the views we create. It's OK to have all child
     * views have the same ID. This ID is used to pick which view receives
     * focus when a view hierarchy is saved / restore
     */
    public static final int VIEW_ID = 1;

    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";

    protected ProgressBar mProgressBar;
    //protected ViewSwitcher mDayViewSwitcher;
    RelativeLayout mExtendedDayViewSwitcher = null;
    protected Animation mInAnimationForward;
    protected Animation mOutAnimationForward;
    protected Animation mInAnimationBackward;
    protected Animation mOutAnimationBackward;   
    
    //EventLoader mCurrentDayViewEventLoader;
    //EventLoader mPreviousDayViewEventLoader;
    //EventLoader mNextDayViewEventLoader;
    //private final LinkedHashMap<Integer,EventLoader> eventLoaders = new LinkedHashMap<Integer,EventLoader>(3);
    
    EventLoader mEventLoaderPerWeek;
    
    EventInfoLoader mCurrentDayViewEventInfoLoader;    
    EventInfoLoader mPreviousDayViewEventInfoLoader;    
    EventInfoLoader mNextDayViewEventInfoLoader;  
    private final LinkedHashMap<Integer,EventInfoLoader> mEventInfoLoaders = new LinkedHashMap<Integer,EventInfoLoader>(3);
    
    
    EventTimeModificationManager mEventTimeModificationManager;
    //Time mCurrentDayViewBaseTime;   
    Calendar mCurrentDayViewBaseTime;
    
    public boolean mGoToExitAnim = false;
    
    String mTimeZoneString;
    private final Runnable mTodayTimeUpdater = new Runnable() {
        @Override
        public void run() {
        	mTimeZoneString = Utils.getTimeZone(getActivity(), mTodayTimeUpdater);
        }
    };
    
    TimeZone mTimeZone;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            if (!DayFragment.this.isAdded()) {
                return;
            }
            
            String tz = Utils.getTimeZone(mContext, mTZUpdater);
            mTimeZone = TimeZone.getTimeZone(tz);
            
            mCurrentDayViewBaseTime.setTimeZone(mTimeZone);            
            
            //mCurrentDayViewBaseTime.timezone = tz;
            //mCurrentDayViewBaseTime.normalize(true);            
            
            mCalendarViewsSecondaryActionBar.updateTimeZone(mTimeZone);
        }
    };
    
    boolean mLaunchedByAlertActivity;
    private int mNumDays;    
    EventInfoViewUpperActionBarFragment mEventInfoUpperActionBarFrag = null;
        
    long mGmtoff;
    public DayFragment(Context context, long timeMillis, int numOfDays, CalendarViewsUpperActionBarFragment upperActionBar) {
    	mContext = context;   
    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
    	
    	String tz = Utils.getTimeZone(mContext, mTZUpdater);
    	mTimeZone = TimeZone.getTimeZone(tz);
    	mGmtoff = mTimeZone.getRawOffset() / 1000;
    	
    	mCurrentDayViewBaseTime = GregorianCalendar.getInstance(mTimeZone);
    	//mCurrentDayViewBaseTime.setTimeZone(mTimeZone);   
    	mCurrentDayViewBaseTime.setFirstDayOfWeek(mFirstDayOfWeek);
    	
        
        mTimeZoneString = Utils.getTimeZone(getActivity(), mTodayTimeUpdater);  
        
        if (timeMillis == 0) {
        	mCurrentDayViewBaseTime.setTimeInMillis(System.currentTimeMillis());
        	//mCurrentDayViewBaseTime.setToNow();
        } else {
        	mCurrentDayViewBaseTime.setTimeInMillis(timeMillis);
        }     
        
        mNumDays = numOfDays;
        
        mUpperActionBar = upperActionBar;
        
        mDateTextStringBuilder = new StringBuilder(50);
        mDateTextFormatter = new Formatter(mDateTextStringBuilder, Locale.getDefault());        
                
        mEventLoaderPerWeek = new EventLoader(context);
        mEventLoaderPerWeek.startBackgroundThread();
        
        mHandler = new Handler();
        
        mLaunchedByAlertActivity = false;
        
        //xxxxx();
    }   
    
    
    long mEventIdByAlertActivity;
    public DayFragment(Context context, long timeMillis, int numOfDays, CalendarViewsUpperActionBarFragment upperActionBar, long eventId) {
    	mContext = context;
    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
    	
    	String tz = Utils.getTimeZone(mContext, mTZUpdater);
    	mTimeZone = TimeZone.getTimeZone(tz);
    	mGmtoff = mTimeZone.getRawOffset() / 1000;
    	
    	mCurrentDayViewBaseTime = GregorianCalendar.getInstance(mTimeZone);
    	//mCurrentDayViewBaseTime.setTimeZone(mTimeZone);        
    	mCurrentDayViewBaseTime.setFirstDayOfWeek(mFirstDayOfWeek);
    	
        mTimeZoneString = Utils.getTimeZone(getActivity(), mTodayTimeUpdater);  
        
        if (timeMillis == 0) {
        	mCurrentDayViewBaseTime.setTimeInMillis(System.currentTimeMillis());
        	//mCurrentDayViewBaseTime.setToNow();
        } else {
        	mCurrentDayViewBaseTime.setTimeInMillis(timeMillis);
        }     
        
        mNumDays = numOfDays;
        
        mUpperActionBar = upperActionBar;
        
        mEventIdByAlertActivity = eventId;       
        
        mDateTextStringBuilder = new StringBuilder(50);
        mDateTextFormatter = new Formatter(mDateTextStringBuilder, Locale.getDefault());        
        
        mEventLoaderPerWeek = new EventLoader(context);
        mEventLoaderPerWeek.startBackgroundThread();
        
        mHandler = new Handler();
        
        mLaunchedByAlertActivity = true;
    }   
    
    public void loadEntireWeekEvents() {
    	        
    	/*
    	 * ���� 1970���� ù�ֿ� �����ߴٴ���
    	 * 2036 ������ �ֿ� ������ ���
    	 * 9���� weeks���� event���� load�� �� ����
    	 * �̿� ���� ��� �־�� �Ѵ�...
    	 */
    	
        loadEntireSeperateWeekEvents();
        setTodayTime();
        loadTodayWeeks();
        
        //mHandler.postDelayed(mTestXXXRunnable, 10); // for eventChange test
        mHandler.post(mUpdateCurrentTime);
    }
    
        
    FastEventInfoFragment mEventInfoFragment; 
    CalendarViewsUpperActionBarFragment mUpperActionBar;
    CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
    LinearLayout mPsudoMonthListViewUpperRegionContainer;
    LinearLayout mPsudoMonthListViewLowerRegionContainer;
    RelativeLayout mFrameLayout;
    ViewSwitcher mFrameLayoutViewSwitcher;
    CalendarViewsLowerActionBar mLowerActionBar; 
      
	RelativeLayout mDayViewLayout;
    OnLayoutChangeListener mDayViewLayoutOnLayoutChangeListener = new OnLayoutChangeListener() {

		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			// TODO Auto-generated method stub			
			//if (INFO) Log.i(TAG, "mDayViewLayoutOnLayoutChangeListener");
		}    	
    };
    
    
    ECalendarApplication mApp;
    CentralActivity mActivity;
    CalendarController mController;
    Context mContext;
    LayoutInflater mInflater;
    float mWidth;
    
    int mFirstDayOfWeek;
    int mPreviousViewType;
    @Override
	public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	if (INFO) Log.i(TAG, "onAttach");
    	mActivity = (CentralActivity)activity;
    	mApp = (ECalendarApplication) activity.getApplication();
    	mContext = mActivity.getApplicationContext();    	
    	
    	mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
    	mController = CalendarController.getInstance(mActivity);//CalendarController.getInstance(mContext);
    	
    	mWidth = getResources().getDisplayMetrics().widthPixels;
    	// EventInfoViewUpperActionBarFragment upperActionBar�� �����ؾ� �Ѵ�    	
    	mEventInfoFragment = new FastEventInfoFragment(mContext, activity, this, ViewType.DAY, mWidth);
    	mEventInfoFragment.onAttach(activity);	
    	mEventInfoFragment.setOnCallerEnterAnimationEndListener(this);
    	
    	
    	mHScrollInterpolator = new ScrollInterpolator();
    	
    	int first = mApp.getEventDayListFirstJulianDay();
    	int last = mApp.getEventDayListLastJulianDay();
    	mPreviousViewType = mController.getPreviousViewType();    	
    	    	
    	boolean waitingProcessEventsChangedFlag = mActivity.getWaitingProcessEventsChangedFlag();
    	if (mPreviousViewType == ViewType.MONTH && !waitingProcessEventsChangedFlag) {
    		//if (INFO) Log.i(TAG, "onAttach:app have events");
    		
    		//int currentTargetJulianDay = ETime.getJulianDay(mCurrentDayViewBaseTime.getTimeInMillis(), mGmtoff);
        	int currentWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(currentWeekPosition); // first day�� monday��� �� ��������,
        	                                                                                 //�̰� �ƴϴ� current Week�� first day�� ã�ƾ� �Ѵ�
        	    	
        	// �츮�� �ʿ��� start day�� weekPosition�� FirstDayOfWeek julian day�̴�
        	/*
    		Time startDayTime = new Time(mTimeZoneString);
    		startDayTime.setJulianDay(julianMonday);
        	
        	if (startDayTime.weekDay != mFirstDayOfWeek) {
    			int diff = startDayTime.weekDay - mFirstDayOfWeek;
    			if (diff < 0) {
    				diff += ManageEventsPerWeek.NUM_DAYS;
    			}
    			startDayTime.monthDay -= diff;
    			startDayTime.normalize(true);
    		}  
        	*/
        	
        	Calendar startDayTimeCal = GregorianCalendar.getInstance(mTimeZone);
        	startDayTimeCal.setFirstDayOfWeek(mFirstDayOfWeek);
        	startDayTimeCal.setTimeInMillis(ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek));
        	ETime.adjustStartDayInWeek(startDayTimeCal);
        	
        	int curWeekStartJulianDay = ETime.getJulianDay(startDayTimeCal.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	int curWeekEndJulianDay = curWeekStartJulianDay + ManageEventsPerWeek.NUM_DAYS - 1;
        	if (first <= curWeekStartJulianDay && curWeekEndJulianDay <= last) {
        		ArrayList<ArrayList<Event>> eventDayList = mApp.getEventDayList();
        		
        		loadEntireWeekEventsFromAppEvents(first, last, eventDayList, currentWeekPosition, startDayTimeCal, curWeekStartJulianDay, curWeekEndJulianDay);        		
        	}
        	else {
        		// current week�� ������ ������ �� �ʿ����!
        		//if (INFO) Log.i(TAG, "onAttach:app NO have Cur Week events");
        		loadEntireWeekEvents();
        	}
        	
        	setTodayTime();
            loadTodayWeeks();
            
            //mHandler.postDelayed(mTestXXXRunnable, 10); // for eventChange test
            mHandler.post(mUpdateCurrentTime);  		
    	}
    	else {
    		//if (INFO) Log.i(TAG, "onAttach:app NO have events");
    		if (waitingProcessEventsChangedFlag) {
    			mActivity.setWaitingProcessEventsChangedFlag(false);
    		}
    		
    		loadEntireWeekEvents();  		
    	}
    	
    	
    	//test();//testCalendarJulianDay();//gregoriX();
	}
        
    public void loadEntireWeekEventsFromAppEvents(int firstJulianDay, int lastJulianDay, ArrayList<ArrayList<Event>> eventDayList, int currentWeekPosition, Calendar startDayTime, int curWeekStartJulianDay, int curWeekEndJulianDay) {	
    	    	
    	/*
    	 * ���� 1970���� ù�ֿ� �����ߴٴ���
    	 * 2036 ������ �ֿ� ������ ���
    	 * 9���� weeks���� event���� load�� �� ����
    	 * �̿� ���� ��� �־�� �Ѵ�...
    	 */
    	/*Time firstJulianDayTime = new Time(mTimeZoneString);
    	firstJulianDayTime.setJulianDay(firstJulianDay);
    	Time lastJulianDayTime = new Time(mTimeZoneString);
    	lastJulianDayTime.setJulianDay(lastJulianDay);*/
    	
    	
    	ManageEventsPerWeek curWeek = new ManageEventsPerWeek();
    	curWeek.mWhichSideWeek = CURRENT_WEEK_EVENTS;
    	curWeek.mTimezone = mTimeZone;
    	curWeek.mFirstDayOfWeek = mFirstDayOfWeek;
    	curWeek.mWeekPosition = currentWeekPosition;
    	curWeek.mStartDayTime = GregorianCalendar.getInstance(mTimeZone);
    	curWeek.mStartDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
    	curWeek.mStartDayTime.setTimeInMillis(startDayTime.getTimeInMillis());
    	curWeek.mWeekStartJulianDay = curWeekStartJulianDay;
    	curWeek.mWeekEndJulianDay = curWeekEndJulianDay;      
    	
		int fromIndex = curWeekStartJulianDay - firstJulianDay;
		int toIndex = curWeekEndJulianDay - firstJulianDay;
		int end = toIndex + 1;
		for (int i=fromIndex; i<end; i++) {
			curWeek.mEventDayList.add(eventDayList.get(i));
		}
		setManageEventsPerWeekEvent(curWeek.mWhichSideWeek, curWeek); 
		
		
		for (int i=1; i<5; i++) {
			int whichSideWeek = -i;
			if (!getEventsFromAppEventDayList(whichSideWeek, firstJulianDay, lastJulianDay, currentWeekPosition, curWeekStartJulianDay, eventDayList)) {
				// PREVIOUS_X_WEEK_EVENTS�� �Ⱓ�� ���ٴ� ����,
				// �� ������ �ȵȴٴ� ���� PREVIOUS_(X-1...)_WEEK_EVENTS���� ���ٴ� ��?
				//if (INFO) Log.i(TAG, "loadEntireWeekEventsFromAppEvents:No=" + String.valueOf(whichSideWeek));
				break;
			}	
		}
				
		for (int i=1; i<5; i++) {
			int whichSideWeek = i;
			if (!getEventsFromAppEventDayList(whichSideWeek, firstJulianDay, lastJulianDay, currentWeekPosition, curWeekStartJulianDay, eventDayList)) {
				// NEXT_1_WEEK_EVENTS�� �Ⱓ�� ���ٴ� ����,
				// �� ������ �ȵȴٴ� ���� NEXT_(X+1...)_WEEK_EVENTS���� ���ٴ� ��?
				//if (INFO) Log.i(TAG, "loadEntireWeekEventsFromAppEvents:No=" + String.valueOf(whichSideWeek));
				break;				
			}			
		}
		
		// ���� ���⼭ ���� �ֵ��� ã�Ƽ� load�ؾ� ��...
		final int NotOk = 0;
		int check[] = new int[9];
		for (int i=0; i<9; i++) {
			check[i] = NotOk;
		}
		
		/*
		 *check array�� index ������ ������ ����
			PREVIOUS_4_WEEK_EVENTS = -4; -4 + 4 = 0
			PREVIOUS_3_WEEK_EVENTS = -3; -3 + 4 = 1
			PREVIOUS_2_WEEK_EVENTS = -2; -2 + 4 = 2
			PREVIOUS_1_WEEK_EVENTS = -1; -1 + 4 = 3   
		    CURRENT_WEEK_EVENTS = 0;      0 + 4 = 4
		    NEXT_1_WEEK_EVENTS = 1;       1 + 4 = 5
		    NEXT_2_WEEK_EVENTS = 2;       2 + 4 = 6
		    NEXT_3_WEEK_EVENTS = 3;       3 + 4 = 7
		    NEXT_4_WEEK_EVENTS = 4;       4 + 4 = 8
	    */
		final int Ok = 1;
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		int key = entry.getKey();    		
    		int index = key + 4;
    		check[index] = Ok;
    	}	
		
		for (int i=0; i<9; i++) {
			if (check[i] == NotOk) {
				int whichSideWeek = i - 4;
				int weekPosition = currentWeekPosition + whichSideWeek;
				
				
		    	
					ManageEventsPerWeek targetWeek = new ManageEventsPerWeek(mTimeZone, whichSideWeek, weekPosition, mFirstDayOfWeek);  
					//if (INFO) Log.i(TAG, "loadEntireWeekEventsFromAppEvents:Must=" + String.valueOf(whichSideWeek));
					if (whichSideWeek == PREVIOUS_1_WEEK_EVENTS || whichSideWeek == NEXT_1_WEEK_EVENTS) {
						// direct�� ȣ���ؾ� �Ѵ�!!!					   	
				    	callDirectEventLoadAndCallback(targetWeek);    	
					}
					else {					
						loadEventsPerWeek(targetWeek);
					}
				
			}
		}
		
		
    }
    
    public boolean getEventsFromAppEventDayList(int whichSide, int firstJulianDay, int lastJulianDay, int curWeekPosition, int curWeekStartJulianDay, ArrayList<ArrayList<Event>> eventDayList) {
    	int weekPosition = curWeekPosition + whichSide;
    	 	
    	
    	int offsetDay = whichSide * 7;
    	int weekStartJulianDay = curWeekStartJulianDay + offsetDay;
		int weekEndJulianDay = weekStartJulianDay + 6;
		
		if (firstJulianDay <= weekStartJulianDay && weekEndJulianDay <= lastJulianDay) {
			//Time startDayTime = new Time(mTimeZoneString);
			//startDayTime.setJulianDay(weekStartJulianDay);
			Calendar startDayTime = GregorianCalendar.getInstance(mTimeZone);
			startDayTime.setTimeInMillis(ETime.getMillisFromJulianDay(weekStartJulianDay, mTimeZone, mFirstDayOfWeek));
			
			ManageEventsPerWeek weekObj = new ManageEventsPerWeek();
			weekObj.mWhichSideWeek = whichSide;
			weekObj.mTimezone = mTimeZone;
			weekObj.mFirstDayOfWeek = mFirstDayOfWeek;
			weekObj.mWeekPosition = weekPosition; 
			weekObj.mStartDayTime = GregorianCalendar.getInstance(mTimeZone);
			weekObj.mStartDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
			weekObj.mStartDayTime.setTimeInMillis(startDayTime.getTimeInMillis());			
			weekObj.mWeekStartJulianDay = weekStartJulianDay;
			weekObj.mWeekEndJulianDay = weekEndJulianDay;      
	    	
			int fromIndex = weekStartJulianDay - firstJulianDay;
			int toIndex = weekEndJulianDay - firstJulianDay;
			int end = toIndex + 1;
			for (int i=fromIndex; i<end; i++) {
				weekObj.mEventDayList.add(eventDayList.get(i));
			}		
			setManageEventsPerWeekEvent(weekObj.mWhichSideWeek, weekObj); 
			
			return true;
		}
		else {
			return false;
		}
				
    }
    
    
    long mGoToAnimationDuration;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        if (INFO) Log.i(TAG, "onCreate");

        Context context = getActivity();

        //mTimeZoneString = Utils.getTimeZone(getActivity(), mTodayTimeUpdater);        
        
        calcFrameLayoutDimension();
        
        mGoToAnimationDuration = CalendarViewsSecondaryActionBar.calculateDuration(mAnticipationMonthDayListViewWidth, 
        		mAnticipationMonthDayListViewWidth, 
        		CalendarViewsSecondaryActionBar.MINIMUM_ANIMATION_VELOCITY);
        
               
        //mCurrentDayViewEventLoader = new EventLoader(context);
        //mPreviousDayViewEventLoader = new EventLoader(context);
        //mNextDayViewEventLoader = new EventLoader(context);
        
        //eventLoaders.put(CURRENT_DAY_EVENTS, mCurrentDayViewEventLoader);
        //eventLoaders.put(PREVIOUS_DAY_EVENTS, mPreviousDayViewEventLoader);
        //eventLoaders.put(NEXT_DAY_EVENTS, mNextDayViewEventLoader);
        
        mCurrentDayViewEventInfoLoader = new EventInfoLoader(context);     
        mPreviousDayViewEventInfoLoader = new EventInfoLoader(context);     
        mNextDayViewEventInfoLoader = new EventInfoLoader(context);   
        
        mEventInfoLoaders.put(CURRENT_DAY_EVENTS, mCurrentDayViewEventInfoLoader);
        mEventInfoLoaders.put(PREVIOUS_DAY_EVENTS, mPreviousDayViewEventInfoLoader);
        mEventInfoLoaders.put(NEXT_DAY_EVENTS, mNextDayViewEventInfoLoader);
        
        mEventTimeModificationManager = new EventTimeModificationManager(getActivity());
    }    
       
    public void makeGoToForwardAnimation() {    	
        
        mInAnimationForward = new TranslateAnimation(        		
                Animation.RELATIVE_TO_SELF, 1,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        mInAnimationForward.setDuration(mGoToAnimationDuration);
        HScrollInterpolator inFowardScrollInterpolator = new HScrollInterpolator(mInAnimationForward, mAnticipationMonthDayListViewWidth);
        mInAnimationForward.setInterpolator(inFowardScrollInterpolator);
                
        mOutAnimationForward = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -1,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        mOutAnimationForward.setDuration(mGoToAnimationDuration);
        HScrollInterpolator OutForwardScrollInterpolator = new HScrollInterpolator(mOutAnimationForward, mAnticipationMonthDayListViewWidth);
        mOutAnimationForward.setInterpolator(OutForwardScrollInterpolator);        
        
    }
    
    
    public void makeGoToBackwardAnimation() {
    	
        mInAnimationBackward = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        mInAnimationBackward.setDuration(mGoToAnimationDuration);
        HScrollInterpolator InBackwardScrollInterpolator = new HScrollInterpolator(mInAnimationBackward, mAnticipationMonthDayListViewWidth);
        mInAnimationBackward.setInterpolator(InBackwardScrollInterpolator);
        
        
        mOutAnimationBackward = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        mOutAnimationBackward.setDuration(mGoToAnimationDuration);
        HScrollInterpolator OutBackwardScrollInterpolator = new HScrollInterpolator(mOutAnimationBackward, mAnticipationMonthDayListViewWidth);
        mOutAnimationBackward.setInterpolator(OutBackwardScrollInterpolator);
    }
    
    
    LinearLayout mSelectedEventPanel;
    SelectedEventRect mSelectedEventRect;
    private Event mSelectedEvent;
    CalendarViewsLowerActionBar mDayViewPsudoLowerActionBar;  
    RelativeLayout mDayViewMainPanel;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {    	
    	    	
    	if (INFO) Log.i(TAG, "onCreateView");
    	
    	mFrameLayout = (RelativeLayout)inflater.inflate(R.layout.day_fragment_layout, null);
    	mFrameLayoutViewSwitcher = (ViewSwitcher) mFrameLayout.findViewById(R.id.day_fragment_switcher); 
    	
    	// ���� psudo delete menu text�� �߰��ؾ� �Ѵ�!!!    	
    	mLowerActionBar = (CalendarViewsLowerActionBar) mFrameLayout.findViewById(R.id.dayview_lower_actionbar);
    	mLowerActionBar.init(this, mTimeZoneString, mController, false);
    	mLowerActionBar.setWhoName("dayview_lower_actionbar");
    	
    	mDayViewLayout = (RelativeLayout) inflater.inflate(R.layout.dayview_layout, null);
		mDayViewLayout.addOnLayoutChangeListener(mDayViewLayoutOnLayoutChangeListener);
				
		mPsudoMonthListViewUpperRegionContainer = (LinearLayout) mDayViewLayout.findViewById(R.id.psudo_month_upperlist_of_dayview);		
		mPsudoMonthListViewLowerRegionContainer = (LinearLayout) mDayViewLayout.findViewById(R.id.psudo_month_lowerlist_of_dayview);
    	mCalendarViewsSecondaryActionBar = (CalendarViewsSecondaryActionBar)mDayViewLayout.findViewById(R.id.dayview_secondary_actionbar);
    	mCalendarViewsSecondaryActionBar.init(this, 
    			mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek, 
    			mController, 
    			mAnticipationMonthDayListViewHeight,
    			mAnticipationMonthDayListViewNormalWeekHeight);     	 
    	mCalendarViewsSecondaryActionBar.setOnStartDayViewSnapAnimationListener(mOnStartDayViewSnapAnimationListener);
    	
    	
    	//dayview_main_panel
    	mDayViewMainPanel = (RelativeLayout) mDayViewLayout.findViewById(R.id.dayview_main_panel);
    	mDayViewPsudoLowerActionBar = (CalendarViewsLowerActionBar)mDayViewMainPanel.findViewById(R.id.dayview_psudo_lower_actionbar);
    	mDayViewPsudoLowerActionBar.init(this, mTimeZoneString, mController, true);
    	mDayViewPsudoLowerActionBar.setWhoName("dayview_psudo_lower_actionbar");
    	
        //mDayViewSwitcher = (ViewSwitcher) mDayViewLayout.findViewById(R.id.dayview_switcher);
    	
    	mExtendedDayViewSwitcher = (RelativeLayout) mDayViewMainPanel.findViewById(R.id.extended_dayview_switcher);
        
        makeSelectedEventPanel();
        
        // Sets the factory used to create the TWO VIEWS between which the ViewSwitcher will flip
        //mDayViewSwitcher.setFactory(this); // call makeView
        //getCurrentDayView().requestFocus();
        
        // CalendarViewsUpperActionBarFragment�� actionbar_indicator_date_text�� update ��Ų��
        //////////////////////////////////////////////////////////
        //((DayView) getCurrentDayView()).updateTitle();
        //////////////////////////////////////////////////////////
        
        mEventInfoFragment.onCreateView(inflater, container, savedInstanceState, 
        		mFrameLayoutViewSwitcher, 
        		mDayViewLayout,
        		mUpperActionBar,
        		true, mCalendarViewsSecondaryActionBar,
        		mLowerActionBar, mDayViewPsudoLowerActionBar,
        		mTimeZoneString);
                        
        if (!mLaunchedByAlertActivity)
        	mFrameLayoutViewSwitcher.addView(mDayViewLayout);
        else {
        	mFrameLayoutViewSwitcher.addView(mEventInfoFragment.mEventInfoView);
        }
         
        makeEventTimeModificationOkDB();
        
        return mFrameLayout;
    }
    
    
    
    ExtendedDayViewDimension mExtendedDayViewDimension;
    public void makeThreeDaysView() {     	
    	    	
    	Calendar nextDay = GregorianCalendar.getInstance(mTimeZone);
    	ETime.copyCalendar(mCurrentDayViewBaseTime, nextDay);
    	nextDay.add(Calendar.DAY_OF_MONTH, 1);
    	    	
    	Calendar previousDay = GregorianCalendar.getInstance(mTimeZone);
    	ETime.copyCalendar(mCurrentDayViewBaseTime, previousDay);
    	previousDay.add(Calendar.DAY_OF_MONTH, -1);
    	
    	ExtendedDayView mExtendedDayView1 = new ExtendedDayView(mContext, mActivity, this, mController, 
    			mCurrentDayViewBaseTime, CURRENT_DAY_EVENTS, mExtendedDayViewDimension);
    	ExtendedDayView mExtendedDayView2 = new ExtendedDayView(mContext, mActivity, this, mController, 
    			previousDay, PREVIOUS_DAY_EVENTS, mExtendedDayViewDimension);
    	ExtendedDayView mExtendedDayView3 = new ExtendedDayView(mContext, mActivity, this, mController, 
    			nextDay, NEXT_DAY_EVENTS, mExtendedDayViewDimension);    	
    	
    	LayoutParams curParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	mExtendedDayView1.setLayoutParams(curParams);
    	
    	LayoutParams prvParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	mExtendedDayView2.setLayoutParams(prvParams);
    	
    	LayoutParams nextParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	mExtendedDayView3.setLayoutParams(nextParams);
    	
    	mExtendedDayViewSwitcher.addView(mExtendedDayView1);
    	mExtendedDayViewSwitcher.addView(mExtendedDayView2);
    	mExtendedDayViewSwitcher.addView(mExtendedDayView3);
    	   	
    	// �� views���� ���� �ٿ��� �Ѵ�
    	mExtendedDayViewsMap.put(CURRENT_DAY_EVENTS, mExtendedDayView1);
    	mExtendedDayViewsMap.put(PREVIOUS_DAY_EVENTS, mExtendedDayView2);
    	mExtendedDayViewsMap.put(NEXT_DAY_EVENTS, mExtendedDayView3);    	   	
    }
    
    public void setThreeDaysViewEvents() {
    		
    	if (mCircularThreeDaysEventsInfoMap.size() != 0) {
    		mCircularThreeDaysEventsInfoMap.remove(CURRENT_DAY_EVENTS);
    		mCircularThreeDaysEventsInfoMap.remove(PREVIOUS_DAY_EVENTS);
    		mCircularThreeDaysEventsInfoMap.remove(NEXT_DAY_EVENTS);  
    	}
    	
    	CircularDayEventsInfo currentDayEvents = new CircularDayEventsInfo();
    	currentDayEvents.mWhichDay = CURRENT_DAY_EVENTS; 	
    	
    	long gmtoff = mTimeZone.getRawOffset() / 1000;    	
    	int curJulianDay = ETime.getJulianDay(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    	currentDayEvents.mFirstJulianDay = curJulianDay;
    	ManageEventsPerWeek curWeekObj = mManageEventsPerWeekHashMap.get(CURRENT_WEEK_EVENTS);
    	int curDayEventsArrayIndex = curJulianDay - curWeekObj.mWeekStartJulianDay;
    	
    	// ���⼭ ���� �߻���
    	// :New Event activity�� �˾��ǰ� ���� ��Ű�� ������ �߻��Ǵ� �� ����!!!
    	currentDayEvents.mEvents = curWeekObj.mEventDayList.get(curDayEventsArrayIndex);
    	currentDayEvents.mAllDayEvents = getAllDayEvents(currentDayEvents.mEvents);
    	
    	mCircularThreeDaysEventsInfoMap.put(CURRENT_DAY_EVENTS, currentDayEvents);      	
    	
    	DayView curView = (DayView) getCurrentDayView();   
    	
    	curView.setEventsX(currentDayEvents.mEvents);
    	setAllDayEventListView(currentDayEvents.mAllDayEvents, CURRENT_DAY_EVENTS);    	
    	
    	curView.handleOnResume();
    	curView.restartCurrentTimeUpdates();

    	reloadDayViewEventsInfo(currentDayEvents, curJulianDay);   	
    	
    	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	CircularDayEventsInfo previousDayEvents = new CircularDayEventsInfo();
    	previousDayEvents.mWhichDay = PREVIOUS_DAY_EVENTS;    	
    	int prvJulianDay = curJulianDay - 1;
    	previousDayEvents.mFirstJulianDay = prvJulianDay;   
    	
    	mCircularThreeDaysEventsInfoMap.put(PREVIOUS_DAY_EVENTS, previousDayEvents);     	
    	
    	DayView preView = (DayView) getPreviousDayView();   	
    	
    	// ���� curJulianDay�� mFirstDayOfWeek julian day���� �۴ٸ�...
    	// :���� ��� mFirstDayOfWeek�� �Ͽ����̰� curJulianDay�� �Ͽ����̶��,
    	//  previous week�� ������� previous day view�� �����Ǿ�� �Ѵ�!!!    	
		
    	if (curWeekObj.mWeekStartJulianDay == curJulianDay) {
    		ManageEventsPerWeek prv1WeekObj = mManageEventsPerWeekHashMap.get(PREVIOUS_1_WEEK_EVENTS);
    		
    		int prvDayEventsArrayIndex = prvJulianDay - prv1WeekObj.mWeekStartJulianDay;
    		
    		previousDayEvents.mEvents = prv1WeekObj.mEventDayList.get(prvDayEventsArrayIndex);
    		previousDayEvents.mAllDayEvents = getAllDayEvents(previousDayEvents.mEvents);
    		
    			
        	preView.setEventsX(prv1WeekObj.mEventDayList.get(prvDayEventsArrayIndex));
        	setAllDayEventListView(previousDayEvents.mAllDayEvents, PREVIOUS_DAY_EVENTS);      	
    	}
    	else {
    		int prvDayEventsArrayIndex = prvJulianDay - curWeekObj.mWeekStartJulianDay;
    		
    		previousDayEvents.mEvents = curWeekObj.mEventDayList.get(prvDayEventsArrayIndex);
    		previousDayEvents.mAllDayEvents = getAllDayEvents(previousDayEvents.mEvents);
    		
        	preView.setEventsX(curWeekObj.mEventDayList.get(prvDayEventsArrayIndex));
        	setAllDayEventListView(previousDayEvents.mAllDayEvents, PREVIOUS_DAY_EVENTS);      
    	}   	
    	
    	preView.handleOnResume();
    	preView.restartCurrentTimeUpdates(); 
        
    	reloadDayViewEventsInfo(previousDayEvents, prvJulianDay); 	
    	
    	
    	////////////////////////////////////////////////////////////////////////////////////////////
    	CircularDayEventsInfo nextDayEvents = new CircularDayEventsInfo();
    	nextDayEvents.mWhichDay = NEXT_DAY_EVENTS;     	
    	int nextJulianDay = curJulianDay + 1;
    	nextDayEvents.mFirstJulianDay = nextJulianDay;
    	
    	mCircularThreeDaysEventsInfoMap.put(NEXT_DAY_EVENTS, nextDayEvents);		
    	
    	
    	DayView nextView = (DayView) getNextDayView();
    	
    	if (nextJulianDay > curWeekObj.mWeekEndJulianDay) {
    		ManageEventsPerWeek next1WeekObj = mManageEventsPerWeekHashMap.get(NEXT_1_WEEK_EVENTS);
    		
    		int nextDayEventsArrayIndex = nextJulianDay - next1WeekObj.mWeekStartJulianDay;
    		
    		nextDayEvents.mEvents = next1WeekObj.mEventDayList.get(nextDayEventsArrayIndex);
    		nextDayEvents.mAllDayEvents = getAllDayEvents(nextDayEvents.mEvents);
    		    			
        	nextView.setEventsX(next1WeekObj.mEventDayList.get(nextDayEventsArrayIndex));
        	setAllDayEventListView(nextDayEvents.mAllDayEvents, NEXT_DAY_EVENTS);
    	}
    	else {
    		int nextDayEventsArrayIndex = nextJulianDay - curWeekObj.mWeekStartJulianDay;
    		
    		nextDayEvents.mEvents = curWeekObj.mEventDayList.get(nextDayEventsArrayIndex);
    		nextDayEvents.mAllDayEvents = getAllDayEvents(nextDayEvents.mEvents);
    		
    		nextView.setEventsX(curWeekObj.mEventDayList.get(nextDayEventsArrayIndex));
    		setAllDayEventListView(nextDayEvents.mAllDayEvents, NEXT_DAY_EVENTS);
    	}
    	
    	nextView.handleOnResume();
    	nextView.restartCurrentTimeUpdates(); 
    	
    	reloadDayViewEventsInfo(nextDayEvents, nextJulianDay);       
    }
       
    
    boolean mSwitchingDayViewBySelectedEventRect = false;
    
    public void makeSelectedEventPanel() {
    	mSelectedEventPanel = (LinearLayout) mDayViewMainPanel.findViewById(R.id.selected_event_panel);
    	mSelectedEventPanel.setBackgroundColor(Color.TRANSPARENT);
    	mSelectedEventPanel.setOnTouchListener(this);
    	
    	mSelectedEventRect = (SelectedEventRect) mSelectedEventPanel.findViewById(R.id.selected_event_rectangle);       
    	mSelectedEventRect.setBackgroundColor(Color.TRANSPARENT);
    }
    
    public View getSelectedEventPanel() {
    	return mSelectedEventPanel;
    }
    
    public SelectedEventRect getSelectedEventRect() {
    	return mSelectedEventRect;
    }
    
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (INFO) Log.i(TAG, "onActivityCreated");				
	}
	
	boolean mFirstStarted = true;
	@Override
	public void onStart() {
		super.onStart();
		if (INFO) Log.i(TAG, "onStart");          
	}

	@Override
    public void onResume() {
        super.onResume();
        
        if (INFO) Log.i(TAG, "onResume");                 
               
        if (mPaused) {
        	mPaused = false;
        	mEventLoaderPerWeek.startBackgroundThread();
        	mTZUpdater.run();
        	mHandler.post(mUpdateCurrentTime);
        }        
        
        mCurrentDayViewEventInfoLoader.startBackgroundThread(); 
        mPreviousDayViewEventInfoLoader.startBackgroundThread();    
        mNextDayViewEventInfoLoader.startBackgroundThread();
        
        mEventTimeModificationManager.startBackgroundThread();
        mTZUpdater.run();    
        
        if (mFirstStarted) {
        	makeThreeDaysView(); 
        	setThreeDaysViewEvents();
        }               
        
        if (mFirstStarted && mLaunchedByAlertActivity)  {       	
        	        	
        	Event selectedEvent = ((DayView) getCurrentDayView()).getEvent(mEventIdByAlertActivity);
        	int startDay = ((DayView) getCurrentDayView()).getSelectedJulianDay();
        	if (selectedEvent != null) {	        	
	        	launchFastEventInfoViewByAlertActivity(mEventIdByAlertActivity, startDay, selectedEvent.startMillis, selectedEvent.endMillis);
        	}
        	else {
        		mFrameLayoutViewSwitcher.addView(mDayViewLayout);
        	}
        }
    	else {    		
    		
    	}
        
        if (mFirstStarted) 
        	mFirstStarted = false;       
    }
	
	public void launchFastEventInfoViewByAlertActivity(long eventId, int startDay, long startMillis, long endMillis) {		
		
    	EventCursors targetObj = getEventCursors(eventId);      	
    	
    	//if (INFO) Log.i(TAG, "-launchFastEventInfoViewByAlertActivity");    	
	}	

	public EventCursors getEventCursors(long eventId) {
		CircularDayEventsInfo events = mCircularThreeDaysEventsInfoMap.get(CURRENT_DAY_EVENTS);
    	int size = events.mEventCursors.size();
    	EventCursors targetObj = null;
    	for (int i=0; i<size; i++) {
    		EventCursors obj = events.mEventCursors.get(i);
    		if (eventId == obj.mEventId) {
    			targetObj = obj;
    			break;
    		}                			
    	}
    	
    	return targetObj;
    }
	
	////////////////////////////////////////////////////////////////////////////////////////
	boolean mPaused = false;
	@Override
    public void onPause() {    
        
        if (INFO) Log.i(TAG, "onPause");
        
        mPaused = true;
        
        mHandler.removeCallbacks(mUpdateCurrentTime);
        
        DayView view = (DayView) getCurrentDayView();
        view.cleanup();
        view = (DayView) getNextDayView();
        view.cleanup();
                
        mEventLoaderPerWeek.stopBackgroundThread();
               
        
        mCurrentDayViewEventInfoLoader.stopBackgroundThread(); 
        mPreviousDayViewEventInfoLoader.stopBackgroundThread();    
        mNextDayViewEventInfoLoader.stopBackgroundThread();
        
        mEventTimeModificationManager.stopBackgroundThread();
                
        // Stop events cross-fade animation
        view.stopEventsAnimation();
        ((DayView) getNextDayView()).stopEventsAnimation();
        // ���� mSelectedEventRect�� new selected event�� ���� launchFastEventInfoView�� ȣ��� ���¿� ���� 
        // onPause�� ȣ��Ǿ��ٸ� onPause���� mSelectedEventRect�� GONE ���·� ����� ���� �ʹ� ������
        // onStop���� �ؾ���
        /*
        if ( mSelectedEventRect.getVisibility() != View.GONE ) {
			mSelectedEventRect.setVisibility(View.GONE);
		}
        */
        super.onPause();
    }
	
	@Override
	public void onStop() {
		if (INFO) Log.i(TAG, "onStop");
		
		if ( mSelectedEventRect.getVisibility() != View.GONE ) {
			mSelectedEventRect.setVisibility(View.GONE);
		}
		
		super.onStop();
	}	

	@Override
	public void onDestroyView() {
		if (INFO) Log.i(TAG, "onDestroyView");
		super.onDestroyView();
	}


	@Override
	public void onDestroy() {
		if (INFO) Log.i(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		if (INFO) Log.i(TAG, "onDetach");
		super.onDetach();
	}    

	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (INFO) Log.i(TAG, "onSaveInstanceState");
        
        long time = getSelectedTimeInMillis();
        if (time != -1) {
            outState.putLong(BUNDLE_KEY_RESTORE_TIME, time);
        }
    }
	
	
	public void launchFastEventInfoView(long eventId, int startDay, long startMillis, long endMillis, Runnable run) {	
		//if (INFO) Log.i(TAG, "+launchFastEventInfoView");
		
		EventCursors targetObj = getEventCursors(eventId);    
		
		mEventInfoFragment.launchFastEventInfoView(ViewType.DAY, eventId, startDay, startMillis, endMillis, targetObj, run);
		
        
    	//if (INFO) Log.i(TAG, "-launchFastEventInfoView");    	
	}
	
	
    void startProgressSpinner() {
        // start the progress spinner
        mProgressBar.setVisibility(View.VISIBLE);
    }

    void stopProgressSpinner() {
        // stop the progress spinner
        mProgressBar.setVisibility(View.GONE);
    }

    //public final static int UPDATE_DAYVIEW_TYPE_BY_SEC_ACTIONBAR_SINGLETAP = 0;
    public final static int UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP = 1;
    public final static int UPDATE_DAYVIEW_TYPE_BY_TODAY = 2;
    
    public interface OnStartMonthDayHeaderAnimationListener {
    	void startMonthDayHeaderAnimation();
    }
    
    public void setOnStartMonthDayHeaderAnimationListener(OnStartMonthDayHeaderAnimationListener l) {
    	mOnStartMonthDayHeaderAnimationListener = l;
    }
    
    public OnStartMonthDayHeaderAnimationListener mOnStartMonthDayHeaderAnimationListener;
        
    // goTo�� DayView.updateSecondaryTitle���� ȣ���ϰ� �ִ�    
    public void secondaryActionBarGoToDate(int selectedTimeType, Calendar gotoDateTime, long animationDuration) {
    	mCalendarViewsSecondaryActionBar.goToDate(selectedTimeType, gotoDateTime, animationDuration);
    }
    
    // *EXTRA_GOTO_TIME�� EXTRA_GOTO_DATE�� ������
    // :DayView::doSingleTapUp �� �� ���� ����� ��Ȳ�� �°� ����ϰ� �ִ�
    // *goTo call context
    //  1.CalendarViewsSecondaryActionBar::sendGoToEvent���� CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR
    //   :CalendarViewsSecondaryActionBar���� �ڽ��� update�Ͽ��� �����̴�
    //  2.DayView::switchViews���� posting�ϴ� DayView::GotoBroadcaster ���� CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR
    //   :DayView::switchViews���� updateSecondaryActionBar(DayFragment.UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP, start, duration)�� ȣ���Ͽ� 
    //    CalendarViewsSecondaryActionBar�� �ڽ��� update�Ͽ��� �����̴�
    //  3.mTodayClickListener���� CalendarController.EXTRA_GOTO_TIME |CalendarController.EXTRA_GOTO_TODAY
    // *DayView.doScroll�� DayView.doFling���� ȣ���ϴ� DayView.cancelAnimation�� ����
    //  goTo���� viewswitch�� �̻� ����[viewswitcher�� view�� �߿� �ϳ��� in/out animation�� �������� ����]��
    @SuppressWarnings("unchecked")
	private void goTo(Calendar goToTime, boolean ignoreTime, boolean animateToday, long extraLong) {
        if (mExtendedDayViewSwitcher == null) {
            // The view hasn't been set yet. Just save the time and use it later.        	
        	//CentralActivity.onCreate
        	//->.initFragments
        	  //->DayFragment.handleEvent
        	    //->.goTo(goToTime,,,)

        	//CentralActivity.onCreate
        	//->.initFragments
        	  //->.setMainPane(, , , timeMillis, );
        	    //->DayFragment::DayFragment(long timeMillis,,)
        	
        	// :�Ѵ� CentralActivity.onCreate ���ƿ��� ȣ��Ǹ� 
        	//  ����� goToTime�̳� timeMillis�� ������ ��¥�� ��Ÿ����
        	        	
        	//mTZUpdater.run();
        	
        	/*
        	Time a = new Time(mCurrentDayViewBaseTime);
        	a.normalize(true);
        	a.set(0, 0, 0, a.monthDay, a.month, a.year);
        	a.normalize(true);
        	
        	Time b = new Time(mCurrentDayViewBaseTime.timezone);        	
        	b.set(0, 0, 0, goToTime.monthDay, goToTime.month, goToTime.year);
        	a.normalize(true);
        	if (Time.compare(a, b) != 0) {
	        	mCurrentDayViewBaseTime.set(goToTime);
	        	mCurrentDayViewBaseTime.normalize(true);            
        	}
        	*/        	
            return;
        }               
        
        // if �� ���� Low ActionBar���� Today�� �����Ͽ��� ����̴�
        if ( (extraLong & CalendarController.EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR) == 0) {
	        if ( (extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS_FORCE) != 0 ) {
				mCalendarViewsSecondaryActionBar.goToBacked(goToTime.getTimeInMillis());
			}	        
			else if ( (extraLong & CalendarController.EXTRA_GOTO_TODAY) !=0 ) {    			
				// calendar low actionbar today�� ���õǾ��� ���			
				secondaryActionBarGoToDate(UPDATE_DAYVIEW_TYPE_BY_TODAY, goToTime, 300); // �Ʒ��� view switcher�� animation duration�� ���߰� �ִ�         			  
			}
        }
        else {
        	// extraLong & CalendarController.EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR != 0�� ����,
        	// ->extraLong�� EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR�� ������ ���
        	//   :-
            // CalendarViewsSecondaryActionBar���� Selected Day ������ �߻��Ͽ� ������ ���� �޽����� �߼��Ͽ��ٴ� ���� �ǹ���
            // EventType.GO_TO, EXTRA_GOTO_DATE | EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR 
        	// :CalendarViewsSecondaryActionBar���� Selected Day ���� ���� �۾��� �̹� �Ϸ��߱⿡ �ƹ��� ó���f ���� ����!!!
        }
        
        DayView currentView = (DayView) getCurrentDayView();
               
        boolean switchViewsAnimation = false;
        boolean forward = true;
        // How does goTo time compared to what's already displaying?
        int diff = currentView.compareToVisibleTimeRange(goToTime);

        if (diff == 0) {
            // In visible range. No need to switch view
        	//if (INFO) Log.i(TAG, "diff==0");
            //currentView.setSelected(goToTime, ignoreTime, animateToday);
        } else {   
        	// else ������ goTo�� �߻��� �� �ִ� ��Ȳ����?
        	// Month day header���� current day�� �ƴ� day���� singleTap�� �߻��� ��� : current day�� ����ȴ� -> ��ü weeks �����ȿ��� �߻���
        	// Month day header���� ��/��� scolling���� ���� �ڵ� prv/next view�� �̵� �Ǵ� fling�� �߻��� ��� : current week�� ����ȴ�   -> ��ü weeks �����ȿ��� �߻���
        	// low actionbar���� Today�� ���õǾ��� ��� : ��ü weeks �����ȿ��� �߻��� ���� �����ۿ����� �߻��� �� �ִ�
        	
        	/////////////////////////////
        	mCurrentDayViewBaseTime.setTimeInMillis(goToTime.getTimeInMillis());
        	
        	/////////////////////////////
        	
        	int goToTimeJulianDay = ETime.getJulianDay(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	Calendar goToTimeCalendar = GregorianCalendar.getInstance(mTimeZone);
        	goToTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        	goToTimeCalendar.setTimeInMillis(mCurrentDayViewBaseTime.getTimeInMillis());
        	
        	Calendar todayTimeCalendar = GregorianCalendar.getInstance(mTimeZone);
        	todayTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        	todayTimeCalendar.setTimeInMillis(System.currentTimeMillis());
        	
        	
        	int newCurrentWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	
        	ManageEventsPerWeek curWeekEventsObj = mManageEventsPerWeekHashMap.get(CURRENT_WEEK_EVENTS);        	
        	ManageEventsPerWeek prv4WeekEventsObj = mManageEventsPerWeekHashMap.get(PREVIOUS_4_WEEK_EVENTS);
        	ManageEventsPerWeek next4WeekEventsObj = mManageEventsPerWeekHashMap.get(NEXT_4_WEEK_EVENTS);
        	
        	int prvEndsJulianDay = prv4WeekEventsObj.mWeekStartJulianDay;
        	int nextEndsJulianDay = next4WeekEventsObj.mWeekEndJulianDay;        	
        	
        	if (goToTimeJulianDay == mTodayJulianDay) {
        		if (goToTimeJulianDay < prvEndsJulianDay  || goToTimeJulianDay > nextEndsJulianDay) { // ��ü weeks ���� �ۿ� today�� �����ϴ� �����
        			
        			// 1.today�� ���Ե� week position�� �˾� ����
        			//   :�翬�� ���� ��ü week�鿡 ���Ե��� �ʾ��� ���̴�
        			// 2.���ο� prv weeks���� week positio�� �˾Ƴ���
        			//   :���� �����ǰ� �ִ� prv week�� �߿� ������ week position�� �����ϴ� ���� Ȯ������
        			//    ->���� �����Ѵٸ� ������ prv week���� whichside ���� �����ؾ� �Ѵ�
        			// 3.���ο� next weeks���� week positio�� �˾Ƴ���
        			//   :���� �����ǰ� �ִ� next week�� �߿� ������ week position�� �����ϴ� ���� Ȯ������
        			//    ->���� �����Ѵٸ� ������ prv week���� whichside ���� �����ؾ� �Ѵ�        			
        			
        			// �츮�� �׻� today�� ���� �ִ� week�� �� �� week �׸��� �� �� week�� load�� �ִ�
        			int prv2WeekPosition = newCurrentWeekPosition - 2;
        			int next2WeekPosition = newCurrentWeekPosition + 2;
        			int prv3WeekPosition = newCurrentWeekPosition - 3;
        			int next3WeekPosition = newCurrentWeekPosition + 3;
        			int prv4WeekPosition = newCurrentWeekPosition - 4;
        			int next4WeekPosition = newCurrentWeekPosition + 4;        			
        			
        			SparseIntArray newWeeksPositions = new SparseIntArray();        			
        			// �츮�� CURRENT_WEEK_EVENTS/PREVIOUS_1_WEEK_EVENTS/NEXT_1_WEEK_EVENTS��
        			// mManageEventsPerTodayWeekHashMap�� ���� copy�ϵ��� �Ѵ�
        			// ���� load �ؾ� �� week���� which side�� position ���� �ʱ�ȭ�Ѵ�
        			// :���� ���� �����ϰ� �ִ� +4/-4 week���߿� �ִٸ� array���� ������ ���̴�
        			newWeeksPositions.put(PREVIOUS_2_WEEK_EVENTS, prv2WeekPosition);
        			newWeeksPositions.put(NEXT_2_WEEK_EVENTS, next2WeekPosition);
        			newWeeksPositions.put(PREVIOUS_3_WEEK_EVENTS, prv3WeekPosition);
        			newWeeksPositions.put(NEXT_3_WEEK_EVENTS, next3WeekPosition);
        			newWeeksPositions.put(PREVIOUS_4_WEEK_EVENTS, prv4WeekPosition);
        			newWeeksPositions.put(NEXT_4_WEEK_EVENTS, next4WeekPosition);
        			
        			SparseIntArray canReUseWeeksPositions = new SparseIntArray();
        			ArrayList <ManageEventsPerWeek> canReUseObjList = new ArrayList <ManageEventsPerWeek>();
        			// ���� �����ϰ� �ִ� week�� �߿� today week�� ��ȯ�ϸ鼭 
        			// +4/-4 weeks ������ ���ԵǾ� �ִ� week�� ������ Ȯ���ؾ� �Ѵ�
        			for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
        	    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
        			 
        	    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();        	    		
        	    		ManageEventsPerWeek valueObj = entry.getValue();        	    		
        	    		
        	    		for (int i=0; i<newWeeksPositions.size(); i++) {
        	    			int newEntrykey = newWeeksPositions.keyAt(i);
        	    			int newEntryValue = newWeeksPositions.get(newEntrykey);
        	    			if (valueObj.mWeekPosition == newEntryValue) { // +4/-4 weeks ������ �ִ� Ư�� week�� today +4/-4 week�� ���ԵǾ� �ִٴ� ����       	    				
            	    			canReUseWeeksPositions.put(newEntrykey, valueObj.mWeekPosition);
            	    			valueObj.mWhichSideWeek = newEntrykey; // today +4/-4 week������ week side�� �������ش�
            	    			canReUseObjList.add(valueObj); 
            	    			newWeeksPositions.removeAt(i); // today +4/-4 week�� ���ԵǱ� ������ ���� load�� �ʿ䰡 �����Ƿ� �����Ѵ�
            	    			break;
            	    		}
        	    		}        		      		
        	    	}       			
        			
        			// clear �ϸ� �ȵȴ�...
        			//mManageEventsPerWeekHashMap.clear();
        			removeManageEventsPerWeekHashMap();	
        			        			
        			// �׻� ������Ʈ �����Ǵ� today week�� prv/next week���� �̺�Ʈ���� ��� �´�
        			ManageEventsPerWeek todayWeekEvents = mManageEventsPerTodayWeekHashMap.get(TODAY_WEEK_EVENTS);
        			ManageEventsPerWeek previousWeekEventsOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
        			ManageEventsPerWeek nextWeekEventsOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(NEXT_WEEK_EVENTS_OF_TODAY_WEEK);
        			
        			ManageEventsPerWeek curWeekEvents = new ManageEventsPerWeek();
        			ManageEventsPerWeek prvWeekEvents = new ManageEventsPerWeek();
        			ManageEventsPerWeek nextWeekEvents = new ManageEventsPerWeek();
        			
        			curWeekEvents.mWhichSideWeek = CURRENT_WEEK_EVENTS;
        			prvWeekEvents.mWhichSideWeek = PREVIOUS_1_WEEK_EVENTS;
        			nextWeekEvents.mWhichSideWeek = NEXT_1_WEEK_EVENTS;
        			
        			copyManageEventsPerWeekObj(curWeekEvents, todayWeekEvents);
        			copyManageEventsPerWeekObj(prvWeekEvents, previousWeekEventsOfTodayWeek);
        			copyManageEventsPerWeekObj(nextWeekEvents, nextWeekEventsOfTodayWeek);        			 
        	    	
        			mManageEventsPerWeekHashMap.put(CURRENT_WEEK_EVENTS, curWeekEvents);
        			mManageEventsPerWeekHashMap.put(PREVIOUS_1_WEEK_EVENTS, prvWeekEvents);
        			mManageEventsPerWeekHashMap.put(NEXT_1_WEEK_EVENTS, nextWeekEvents);
        			
        			// ���� �����ϰ� �ִ� +4/-4 week���� ������ �� �ִٸ� map�� put �ض�
        			for (int i=0; i<canReUseObjList.size(); i++) {
        				ManageEventsPerWeek obj = canReUseObjList.get(i);
        				mManageEventsPerWeekHashMap.put(obj.mWhichSideWeek, obj);
        			}
        			
        			// ���� load�ؾ� �� week event���� load�Ѵ�
        			for (int i=0; i<newWeeksPositions.size(); i++) {
    	    			int newEntrykey = newWeeksPositions.keyAt(i);
    	    			int newEntryValue = newWeeksPositions.get(newEntrykey);
    	    			
    	    			ManageEventsPerWeek newObj = new ManageEventsPerWeek(mTimeZone, newEntrykey, newEntryValue, mFirstDayOfWeek);   
    	    			loadEventsPerWeek(newObj);    	    			
    	    		}        			
        		}        		
        		else if (goToTimeJulianDay >= prvEndsJulianDay  && goToTimeJulianDay <= nextEndsJulianDay) { // ��ü weeks �����ȿ� today�� �����ϴ� �����
        			if (newCurrentWeekPosition != curWeekEventsObj.mWeekPosition){ // today�� current week�� ���� �ʴ�
	        			int direction = newCurrentWeekPosition - curWeekEventsObj.mWeekPosition;
	    				
	        			if (direction > 0 && direction < 2) { // today�� ���� current week�� �ٷ� ���ֿ� ���� �ִ�
	        				switchViewsAnimation = true;	        				
	        				setSwitchNextWeek();       	    		
	        			}	        			
	        			else if (direction > -2  &&  direction < 0){ // today�� ���� current week�� �ٷ� ���ֿ� ���� �ִ�
	        				switchViewsAnimation = true;
	        				forward = false;
	        				setSwitchPreviousWeek();       	    		
	        			}
	        			else {	        				
	        				if (direction > 0) { // today�� ���� current week�� ���� ���� �ֵ鿡 ���� �ִ�
	        					// �翬�� 1���� ���� ���
	        					setGoToRightSideWeek(Math.abs(direction));    
	        				}	        				
	        				else { // today�� ���� current week�� ���� ���� �ֵ鿡 ���� �ִ�
	        					// �翬�� -1���� ���� ���
	        					setGoToLeftSideWeek(Math.abs(direction));           					
	        				}            				
	        			}  
        			}
        			else if (newCurrentWeekPosition == curWeekEventsObj.mWeekPosition) {  // today�� current week�� �ִ�
        				
        				// �̴� reSetThreeDaysViewsBaseTime / setThreeDaysViewEvents ���� ȣ�� �۾����θ� ������
        				switchViewsAnimation = true;
        				//int curViewJulianDay = Time.getJulianDay(currentView.mBaseDate.toMillis(true), currentView.mBaseDate.gmtoff);
        				if (currentView.mFirstJulianDay > goToTimeJulianDay) 
        					forward = false;
        				
            		}
        		}        		
        	}
        	else {        		
        		
    			int direction = newCurrentWeekPosition - curWeekEventsObj.mWeekPosition;
    			// fling�̳� scrolling �� �ڵ� prv/next week �̵��� ũ��� one week�� ���ѵǾ� �ִ�
    			if (direction > 0) { // fling�̳� scrolling �� �ڵ����� next week�� goto day �Ѵٴ� ���� ����   
    				switchViewsAnimation = true;
    				setSwitchNextWeek();       	    		
    			}
    			else if (direction < 0){ // fling�̳� scrolling �� �ڵ����� prv week�� goto day �Ѵٴ� ���� ���� 
    				switchViewsAnimation = true;
    				forward = false;
    				setSwitchPreviousWeek();       	    		
    			}
    			else {
    				// direction�� 0�̶�� ���� newCurrentWeekPosition == curWeekEventsObj.mWeekPosition�� �ǹ��Ѵ�
    				// �� ���� current week���� day�� ���õǾ��ٴ� ���̴�
    				// :�̴� reSetThreeDaysViewsBaseTime / setThreeDaysViewEvents ���� ȣ��� �ذ�ȴ�.
    				switchViewsAnimation = true;
    				//int curViewJulianDay = Time.getJulianDay(currentView.mBaseDate.toMillis(true), currentView.mBaseDate.gmtoff);
    				if (currentView.mFirstJulianDay > goToTimeJulianDay) 
    					forward = false;
    			}        		       	
        	}        	
        	        	   
        	
        	if (switchViewsAnimation) {       		
        		
        		ExtendedDayView curView =  getCurrentExtendedDayView(); // animation �Ϸ��� prv �Ǵ� next view�� �Ǿ�� �Ѵ�  		
        		
                ExtendedDayView nextView =  getNextExtendedDayView();
                ExtendedDayView previousView =  getPreviousExtendedDayView();  
                
        		float inFromXValue, inToXValue;
                float outFromXValue, outToXValue;
                                
                int curHour = curView.mDayView.getFirstVisibleHour();
        		int curHourOffset = curView.mDayView.getFirstVisibleHourOffset();
        		
        		ManageEventsPerWeek goToDayWeekObj = mManageEventsPerWeekHashMap.get(CURRENT_WEEK_EVENTS);
            	ManageEventsPerWeek targetWeekObj = goToDayWeekObj;
            	
                if (forward) {
                	//if (INFO) Log.i(TAG, "FORWARD");              	
                	
            		// forward�� ���,
                	// cur view�� �������� �����ϰ�, ���� cur view�� �����ʿ��� �������� �����Ѵ�
                	
            		// *���� nextView�� ���� goto view[���� current view�� �ֱ�]�� �����Ѵ�
                	// :�׸��� animation �Ϸ� �Ŀ� cur view�� �ȴ�
                	//////////////////////////////////////////////////////////////////////////////////////////////////
            		nextView.mDayView.setBaseTime(mCurrentDayViewBaseTime);
            		nextView.mDayView.setFirstVisibleHour(curHour);
            		nextView.mDayView.setFirstVisibleHourOffset(curHourOffset);
            		                	
                	CircularDayEventsInfo currentDayEvents = makeCircularDayEventsInfo(CURRENT_DAY_EVENTS, goToTimeJulianDay, targetWeekObj);                	
                	setCircularDayEventsInfo(nextView, CURRENT_DAY_EVENTS, currentDayEvents);              	   	
                	
                	nextView.mDayView.handleOnResume();
                	nextView.mDayView.restartCurrentTimeUpdates();                	
                	//////////////////////////////////////////////////////////////////////////////////////////////////
                	// *���� prv view�� animation�� ������ �ʿ䰡 ������ next view�� �̸� �����Ѵ�                	                	
                	previousView.mDayView.setBaseTime(makeThreeDayTime(mCurrentDayViewBaseTime, NEXT_DAY_EVENTS));                	
                	
                	int nextJulianDay = goToTimeJulianDay + 1;
                	
                	if (nextJulianDay > goToDayWeekObj.mWeekEndJulianDay) {
                		targetWeekObj = mManageEventsPerWeekHashMap.get(NEXT_1_WEEK_EVENTS);
                	}
                	                	
                	CircularDayEventsInfo nextDayEvents = makeCircularDayEventsInfo(NEXT_DAY_EVENTS, nextJulianDay, targetWeekObj);
                	setCircularDayEventsInfo(previousView, NEXT_DAY_EVENTS, nextDayEvents);                	
                	
                	previousView.mDayView.handleOnResume();
                	previousView.mDayView.restartCurrentTimeUpdates();
                	
                	outFromXValue = 0;
                    outToXValue = -mDayViewWidth;
                	       	
                    inFromXValue = mDayViewWidth;
                    inToXValue = 0.0f;
                                        
                } else {
                	//if (INFO) Log.i(TAG, "BACKWARD");
                	
                	// backward ���,
                	// cur view�� ���������� �����ϰ�, ���� cur view�� ���ʿ��� ���������� �����Ѵ�
                	
                	// *���� previousView�� ���� goto view[���� current view�� �ֱ�]�� �����Ѵ�
                	// :�׸��� animation �Ϸ� �Ŀ� cur view�� �ȴ�
                	previousView.mDayView.setBaseTime(mCurrentDayViewBaseTime);
                	previousView.mDayView.setFirstVisibleHour(curHour);
                	previousView.mDayView.setFirstVisibleHourOffset(curHourOffset);
                	
                	CircularDayEventsInfo currentDayEvents = makeCircularDayEventsInfo(CURRENT_DAY_EVENTS, goToTimeJulianDay, targetWeekObj);
                	setCircularDayEventsInfo(previousView, CURRENT_DAY_EVENTS, currentDayEvents); 
                	
                	previousView.mDayView.handleOnResume();
                	previousView.mDayView.restartCurrentTimeUpdates();
                	//////////////////////////////////////////////////////////////////////////////////////////////////
                	// *���� next view�� animation�� ������ �ʿ䰡 ������ previous view�� �̸� �����Ѵ�                	                	
                	nextView.mDayView.setBaseTime(makeThreeDayTime(mCurrentDayViewBaseTime, PREVIOUS_DAY_EVENTS));                	
            		
                	int prvJulianDay = goToTimeJulianDay - 1;
                	
                	if (goToDayWeekObj.mWeekStartJulianDay == goToTimeJulianDay) {
                		targetWeekObj = mManageEventsPerWeekHashMap.get(PREVIOUS_1_WEEK_EVENTS); 
                	}               	
                	
                	CircularDayEventsInfo prvDayEvents = makeCircularDayEventsInfo(PREVIOUS_DAY_EVENTS, prvJulianDay, targetWeekObj);
                	setCircularDayEventsInfo(nextView, PREVIOUS_DAY_EVENTS, prvDayEvents); 
                	                	
                	nextView.mDayView.handleOnResume();
                	nextView.mDayView.restartCurrentTimeUpdates();   
                	//////////////////////////////////////////////////////////////////////////////////////////////////
                	
                	outFromXValue = 0;
                    outToXValue = mDayViewWidth;
                	    	
                    inFromXValue = -mDayViewWidth;
                    inToXValue = 0.0f;
                }                
                
        		TranslateAnimation outAnimation = new TranslateAnimation(
                		Animation.ABSOLUTE, outFromXValue,
                		Animation.ABSOLUTE, outToXValue,
                        Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f);
                
                TranslateAnimation inAnimation = new TranslateAnimation(
                		Animation.ABSOLUTE, inFromXValue,
                		Animation.ABSOLUTE, inToXValue,
                        Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f);      

                long duration = calculateDuration(mDayViewWidth, mDayViewWidth, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
                
                inAnimation.setDuration(duration);
                inAnimation.setInterpolator(mHScrollInterpolator);        
                
                outAnimation.setDuration(duration);
                outAnimation.setInterpolator(mHScrollInterpolator);      
                
                outAnimation.setAnimationListener(new GoToBroadcaster(forward, curHour, curHourOffset));
                
                curView.setX(0);  
                
            	if (forward) {
                    nextView.mDayView.mRemeasure = true;
                    nextView.mDayView.invalidate();
                    		  
            		nextView.setX(0);
            		previousView.setX(mDayViewWidth);
            		
            		nextView.mDayView.updateTitle();
                	 		
                	curView.startAnimation(outAnimation);
                	nextView.startAnimation(inAnimation);        		     	
                }
                else {
                	previousView.mDayView.mRemeasure = true;
                	previousView.mDayView.invalidate();
                	    	    
                	previousView.setX(0);
                	nextView.setX(-mDayViewWidth);
                	
                	previousView.mDayView.updateTitle();
                	
                	curView.startAnimation(outAnimation);
                	previousView.startAnimation(inAnimation);            		
                }                                
        		
        	}
        	else {
        		reSetThreeDaysViewsBaseTime();
    			
        		setThreeDaysViewEvents(); 	   			
        	}
        	
        	mOnStartMonthDayHeaderAnimationListener.startMonthDayHeaderAnimation();    
        }
    }
    
    public CircularDayEventsInfo makeCircularDayEventsInfo(int whichDay, int targetJulianDay, ManageEventsPerWeek targetWeekObj) {    	
		
		CircularDayEventsInfo targetDayEvents = new CircularDayEventsInfo();
		targetDayEvents.mWhichDay = whichDay;    	  
		targetDayEvents.mFirstJulianDay = targetJulianDay;
    	
    	int targetDayEventsArrayIndex = targetJulianDay - targetWeekObj.mWeekStartJulianDay;                	
    	
    	targetDayEvents.mEvents = targetWeekObj.mEventDayList.get(targetDayEventsArrayIndex);
    	targetDayEvents.mAllDayEvents = getAllDayEvents(targetDayEvents.mEvents);
    	
    	return targetDayEvents;
    }
    
    public void setCircularDayEventsInfo(ExtendedDayView extendedDayView, int whichSideDay, CircularDayEventsInfo targetDayEvents) {
    	extendedDayView.mDayView.setEventsX(targetDayEvents.mEvents);
    	
    	// ���⼭ setAllDayEventListView(targetDayEvents.mAllDayEvents, whichSideDay)�� ����ϸ� �ȵȴ�    	
    	// :goTo�� animation���� ȣ���� �� �����Ǵ� extendedDayView��  
    	//  ���� ExtendedDayViewMap�� ���εǾ� �ִ� key�� extendedDayView�� �ٸ��� ������
    	if (targetDayEvents.mAllDayEvents.isEmpty()) {
    		extendedDayView.setAllDayEventListView(0, null);						
		}
		else {
			extendedDayView.setAllDayEventListView(targetDayEvents.mAllDayEvents.size(), targetDayEvents.mAllDayEvents);	
		}   	  
    	
    	mCircularThreeDaysEventsInfoMap.remove(whichSideDay);
    	
    	mCircularThreeDaysEventsInfoMap.put(whichSideDay, targetDayEvents);     
    }
    
    public Calendar makeThreeDayTime(Calendar baseTime, int whichSideDay) {
    	
    	Calendar dayTime = GregorianCalendar.getInstance(baseTime.getTimeZone());
    	dayTime.setFirstDayOfWeek(baseTime.getFirstDayOfWeek());
    	dayTime.setTimeInMillis(baseTime.getTimeInMillis());
    	
    	if (whichSideDay == NEXT_DAY_EVENTS) {    		
    		dayTime.add(Calendar.DAY_OF_MONTH, 1); //dayTime.monthDay = dayTime.monthDay + 1;
    	}
    	else {
    		dayTime.add(Calendar.DAY_OF_MONTH, -1); //dayTime.monthDay = dayTime.monthDay - 1;
    	}
    	
    	
    	
    	return dayTime;
    }

    /**
     * Returns the selected time in milliseconds. The milliseconds are measured
     * in UTC milliseconds from the epoch and uniquely specifies any selectable
     * time.
     *
     * @return the selected time in milliseconds
     */
    public long getSelectedTimeInMillis() {
        if (mExtendedDayViewSwitcher == null) {
            return -1;
        }
        DayView view = (DayView) getCurrentDayView();
        if (view == null) {
            return -1;
        }
        return view.getSelectedTimeInMillis();
    }

    public void eventsChanged() {
    	if (INFO) Log.i(TAG, "eventsChanged");
    	
        if (mExtendedDayViewSwitcher == null) {
            return;
        }        
        
        // today runnable�� �ߴܽ��Ѿ� �Ѵ�!!!
        mHandler.removeCallbacks(mUpdateCurrentTime);
        
        mEventLoaderPerWeek.putImmediatelyCancelAllLoadRequests(ImmediatelyCancelAllLoadRequestsCallback);               
    }
    
    Runnable ImmediatelyCancelAllLoadRequestsCallback = new Runnable(){

		@Override
		public void run() {
			//if (INFO) Log.i(TAG, "ImmediatelyCancelAllLoadRequestsCallback");
			
			DayFragment.this.mHandler.post(mLoadEntireSeperateWeekEventsRunnable);
		}
    	
    };
    
    
    Runnable mLoadEntireSeperateWeekEventsRunnable = new Runnable(){

		@Override
		public void run() {		
			//if (INFO) Log.i(TAG, "mLoadEntireSeperateWeekEventsRunnable");
			
			loadEntireSeperateWeekEvents();
			setTodayTime();
            loadTodayWeeks();
			mHandler.post(mUpdateCurrentTime);			
			
			//reSetThreeDaysViewsBaseTime(); 
	    	setThreeDaysViewEvents();
		}
    	
    };
    
    /*
    Runnable mTestXXXRunnable = new Runnable(){

		@Override
		public void run() {		
			if (INFO) Log.i(TAG, "mTestXXXRunnable");
			
			eventsChanged();
		}
    	
    };
    */   
    
    Event getNewEvent() {
        DayView view = (DayView) getCurrentDayView();
        return view.getNewEvent();
    }

    public DayView getNextView() {
        return (DayView) getNextDayView();
    }

    public long getSupportedEventTypes() {
        //return EventType.GO_TO | EventType.EVENTS_CHANGED;
    	return EventType.GO_TO | EventType.EVENTS_CHANGED | EventType.VIEW_EVENT | EventType.GO_TO_MONTH_ANIM;
    }

    public void handleEvent(EventInfo msg) {
        if (msg.eventType == EventType.GO_TO) {	
        	//if (INFO) Log.i(TAG, "handleEvent:EventType.GO_TO");
        	
            goTo(msg.selectedTime, 
            	 (msg.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0, //boolean ignoreTime
                 (msg.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0, //boolean animateToday
                 msg.extraLong);
            
        } else if (msg.eventType == EventType.EVENTS_CHANGED) {
            eventsChanged();
        }
        else if (msg.eventType == EventType.VIEW_EVENT) {
        	//if (INFO) Log.i(TAG, "DayFragment:hE:EventType.VIEW_EVENT");
        	// ���⼭ �츮�� �ϴ�  secondary actionbar�� unvisible ���·� ��ȯ�ؾ� �Ѵ�!!!
        	// 1. dayofweek_header
        	// 2. monthday_header
        	// 3. dateindicator_header
        	mCalendarViewsSecondaryActionBar.setInVisibleBar(ViewType.DAY);
        }
        else if (msg.eventType == EventType.GO_TO_MONTH_ANIM) {
        	// secondary action bar�� ���� current week ������ ���;� �Ѵ�
        	// �׸��� secondary action bar�� month day indicator�� date indicator�� gone ���·� ��ȯ��Ų��        	
        	Calendar selectedDayTime = mCalendarViewsSecondaryActionBar.getMonthDayHeaderViewSelectedDayTime();
        	int firstJulianDay = mCalendarViewsSecondaryActionBar.getMonthDayHeaderViewFirstJulianDay();
        	int lastJulianDay = mCalendarViewsSecondaryActionBar.getMonthDayHeaderViewLastJulianDay();
        	
			//calcFrameLayoutHeight();
        	        	
        	mGoToExitAnim = true;
			ExitDayViewEnterMonthViewAnim obj = new ExitDayViewEnterMonthViewAnim(mApp, mContext, this, selectedDayTime, firstJulianDay, lastJulianDay);
        	
			// ���� ���⼭ app���� events�� �Ѱ� ����...
			
        	obj.startMonthExitAnim();        	
        }
    }
     
    
	
	
	
	private final Formatter mDateTextFormatter;
    private final StringBuilder mDateTextStringBuilder;    
    public String makeDateText(int julianDay, boolean makeDayOfWeek) {
    	Calendar date = GregorianCalendar.getInstance(mTimeZone);
    	long millis = ETime.getMillisFromJulianDay(julianDay, mTimeZone, mFirstDayOfWeek);
    	date.setTimeInMillis(millis);
    	//long millis = date.setJulianDay(julianDay);      	

    	mDateTextStringBuilder.setLength(0);
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_YEAR;
        String dateViewText = ETime.formatDateTimeRange(mContext, mDateTextFormatter, millis, millis,
                flags, mTimeZoneString).toString();  
        
        if (makeDayOfWeek) {
        	SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            Date d = new Date();
            d.setTime(date.getTimeInMillis());
            String dayViewText;
        	dayViewText = " (" + sdf.format(d) + ")";
        	dateViewText = dateViewText + dayViewText;
        }        
        
        return dateViewText;
    }


	@Override
	public void onCallerReEntranceAnimEndSet() {
		DayView view = (DayView) getCurrentDayView();
        view.handleOnResume();
        view.restartCurrentTimeUpdates();

        view = (DayView) getNextDayView();
        view.handleOnResume();
        view.restartCurrentTimeUpdates();		
	}
    
		
	AnimationListener mInAnimationForwardListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			//if(INFO) Log.i(TAG, "mInAnimationForwardListener:Start");
			//mOnStartMonthDayHeaderAnimationListener.startMonthDayHeaderAnimation();
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			//if(INFO) Log.i(TAG, "mInAnimationForwardListener:End");			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}
		
	};
	
	AnimationListener mOutAnimationForwardListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			//if(INFO) Log.i(TAG, "mOutAnimationForwardListener:Start");			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			//if(INFO) Log.i(TAG, "mOutAnimationForwardListener:End");			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}
		
	};
	
	AnimationListener mInAnimationBackwardListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			//if(INFO) Log.i(TAG, "mInAnimationBackwardListener:Start");	
			//mOnStartMonthDayHeaderAnimationListener.startMonthDayHeaderAnimation();
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			//if(INFO) Log.i(TAG, "mInAnimationBackwardListener:End");			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}
		
	};
	
	AnimationListener mOutAnimationBackwardListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			//if(INFO) Log.i(TAG, "mOutAnimationBackwardListener:Start");			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			//if(INFO) Log.i(TAG, "mOutAnimationBackwardListener:End");			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}
		
	};
	
	OnStartDayViewSnapAnimationListener mOnStartDayViewSnapAnimationListener = new OnStartDayViewSnapAnimationListener() {

		@Override
		public void startDayViewSnapAnimation() {
			// ���� ����ϰ� ���� ����ϰ� ���� �ʴ�			
		}
		
	};
	
	ArrayList<ExtendedDayView> mExtendedDayViewList = new ArrayList<ExtendedDayView>();
	Map<Integer, ExtendedDayView> mExtendedDayViewsMap = new HashMap<Integer, ExtendedDayView>(); 
	// makeSelectedEventPanel���� 
	// mSelectedEventPanel.setOnTouchListener(this)�� �����ϰ� ����
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// current extended day view�� allday event ������ ����
		// event�� x,y ��ǥ�� allday event listview�� ������ ���ԵǴ����� �����ؾ� �Ѵ�       
		ExtendedDayView eDayView = getCurrentExtendedDayView();
		if (eDayView.mAllDayEventCounts > 0) {
			int x = (int)event.getX();
	        int y = (int)event.getY();
	        
	        Rect allDayEventListViewRec = new Rect(eDayView.mAllDayEventLists.getLeft(), 
	        		eDayView.mAllDayEventLists.getTop(), 
	        		eDayView.mAllDayEventLists.getRight(), 
	        		eDayView.mAllDayEventLists.getBottom());
	        if (allDayEventListViewRec.contains(x, y))
	        	return false;
		}       
        	
		
		DayView curView = (DayView) getCurrentDayView();	
		
		return curView.onTouchEventX(event);
	}
	
	
	
	
	
	public void moveDayViews(int deltaX) {
		// mViewStartX�� ��ȣ�� ����? ��Ű�� ������
		// user�� finger�� left -> right�� �̵��ϸ�
		// onScroll�� deltaX �Ķ���ʹ� (-)������ ���޵ȴ�
		// �׷��� moveDayViews�� ��ü�� current day view��
		// user�� finger�� �̵��� ���� �����̱� ���ؼ�
		// ��ȣ ����?�� �� �ָ� ���� setX �Լ��� ���� ������ �� �ֱ� �����̴�
		//int deltaX = -mViewStartX;
		//if (INFO) Log.i(TAG, "mViewStartX=" + String.valueOf(mViewStartX) + ", deltaX=" + String.valueOf(deltaX));
        
        //boolean switchForward;
        // deltaX�� initNextView�� ȣ���� ������
        // ���� ���� -���� �ٿ� ��ȣ�� �ݴ�� ������
        //if (deltaX > 0) {  // onScroll�� ������ ���� -deltaX :finger�� right�� �̵� : �ð��밡 backward[����]�� �̵���
        	// previous day�� ingoing �ϰ�
        	// current day�� right side�� outgoing��...            
            //switchForward = false;
            ExtendedDayView curView = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
            curView.setX(deltaX);
            
            ExtendedDayView prvView = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
            prvView.setX(curView.getX() - mDayViewWidth);
            
            ExtendedDayView nextView = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
            nextView.setX(curView.getX() + mDayViewWidth);
            
        //} else {
        	// next day�� ingoing �ϰ�
        	// current day�� left side�� outgoing��...            
            //switchForward = true;
            
            //ExtendedDayView curView = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
            //curView.setX(deltaX);
            
            //ExtendedDayView nextView = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
            //nextView.setX(curView.getX() + mDayViewWidth);
            
            //ExtendedDayView prvView = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
            //prvView.setX(curView.getX() - mDayViewWidth);            
        //}               
    }
	
	
	
	
	
	public int mSelectionMode = DayView.SELECTION_HIDDEN;
	
	int mPrvNewOrSelectedEventLeft = 0;
	int mPrvNewOrSelectedEventRight = 0;
	
	int mPrvNewOrSelectedEventTop = 0;
	int mPrvNewOrSelectedEventBottom = 0;
	
	int mPrvNewOrSelectedEventMoveX = 0;
	int mPrvNewOrSelectedEventMoveY = 0;
	
	public void setPrvNewOrSelectedEventLeft(int left) {
		mPrvNewOrSelectedEventLeft = left;
	}
	
	public int getPrvNewOrSelectedEventLeft() {
		return mPrvNewOrSelectedEventLeft;
	}
	
	public void setPrvNewOrSelectedEventTop(int top) {
		mPrvNewOrSelectedEventTop = top;
	}
	
	public int getPrvNewOrSelectedEventTop() {
		return mPrvNewOrSelectedEventTop;
	}
	
	public void setPrvNewOrSelectedEventRight(int right) {
		mPrvNewOrSelectedEventRight = right;
	}
	
	public int getPrvNewOrSelectedEventRight() {
		return mPrvNewOrSelectedEventRight;
	}
	
	public void setPrvNewOrSelectedEventBottom(int bottom) {
		mPrvNewOrSelectedEventBottom = bottom;
	}
	
	public int getPrvNewOrSelectedEventBottom() {
		return mPrvNewOrSelectedEventBottom;
	}
	
	
	public void setPrvNewOrSelectedEventMoveX(int x) {
		mPrvNewOrSelectedEventMoveX = x;
	}
	
	public int getPrvNewOrSelectedEventMoveX() {
		return mPrvNewOrSelectedEventMoveX;
	}
	
	public void setPrvNewOrSelectedEventMoveY(int y) {
		mPrvNewOrSelectedEventMoveY = y;
	}
	
	public int getPrvNewOrSelectedEventMoveY() {
		return mPrvNewOrSelectedEventMoveY;
	}
	
	
	
	public void setSelectionMode(int mode, int motion) {
		/*
		if (INFO) Log.i(TAG, "*******************************************************");
		switch(mSelectionMode) {
		case DayView.SELECTION_HIDDEN:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_HIDDEN");
			break;
		case DayView.SELECTION_NEW_EVENT:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_NEW_EVENT");
			break;
		case DayView.SELECTION_NEW_EVENT_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_NEW_EVENT_MOVED");
			break;
		case DayView.SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED");
			break;
		case DayView.SELECTION_EVENT_PRESSED:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_EVENT_PRESSED");
			break;
		case DayView.SELECTION_EVENT_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_EVENT_MOVED");
			break;
		case DayView.SELECTION_EVENT_ADJUST_POSITION_AFTER_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_EVENT_ADJUST_POSITION_AFTER_MOVED");
			break;
		case DayView.SELECTION_EVENT_FLOATING:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_EVENT_FLOATING");
			break;
		case DayView.SELECTION_EVENT_EXPAND_SELECTION:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_EVENT_EXPAND_SELECTION");
			break;
		case DayView.SELECTION_EVENT_EXPANDING:
			if (INFO) Log.i(TAG, "setSelectionMode:cur:SELECTION_EVENT_EXPANDING");
			break;
		
		}
		
		
		switch(motion) {
		case DayView.SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE:
			if (INFO) Log.i(TAG, "setSelectionMode:motion:NOT_BOTH_TOUCHEVENT_AND_GESTURE");
			break;
		case DayView.SELECTION_ACTION_MOVE_TOUCHEVENT:
			if (INFO) Log.i(TAG, "setSelectionMode:motion:ACTION_MOVE_TOUCHEVENT");
			break;
		case DayView.SELECTION_ACTION_UP_TOUCHEVENT:
			if (INFO) Log.i(TAG, "setSelectionMode:motion:ACTION_UP_TOUCHEVENT");
			break;
		case DayView.SELECTION_DOWN_GESTURE:
			if (INFO) Log.i(TAG, "setSelectionMode:motion:DOWN_GESTURE");
			break;
		case DayView.SELECTION_SINGLETAPUP_GESTURE:
			if (INFO) Log.i(TAG, "setSelectionMode:motion:SINGLETAPUP_GESTURE");
			break;
		case DayView.SELECTION_LONGPRESS_GESTURE:
			if (INFO) Log.i(TAG, "setSelectionMode:motion:LONGPRESS_GESTURE");
			break;
		}
		*/
		
		mSelectionMode = mode;	
		/*
		switch(mSelectionMode) {
		case DayView.SELECTION_HIDDEN:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_HIDDEN");
			break;
		case DayView.SELECTION_NEW_EVENT:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_NEW_EVENT");
			break;
		case DayView.SELECTION_NEW_EVENT_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_NEW_EVENT_MOVED");
			break;
		case DayView.SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED");
			break;
		case DayView.SELECTION_EVENT_PRESSED:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_EVENT_PRESSED");
			break;
		case DayView.SELECTION_EVENT_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_EVENT_MOVED");
			break;
		case DayView.SELECTION_EVENT_ADJUST_POSITION_AFTER_MOVED:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_EVENT_ADJUST_POSITION_AFTER_MOVED");
			break;
		case DayView.SELECTION_EVENT_FLOATING:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_EVENT_FLOATING");
			break;
		case DayView.SELECTION_EVENT_EXPAND_SELECTION:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_EVENT_EXPAND_SELECTION");
			break;
		case DayView.SELECTION_EVENT_EXPANDING:
			if (INFO) Log.i(TAG, "setSelectionMode:new:SELECTION_EVENT_EXPANDING");
			break;		
		}
		
		if (INFO) Log.i(TAG, "*******************************************************");
		*/
	}
	
	public int getSelectionMode() {
		return mSelectionMode;
	}
	
	// -DayView.setSelected
	//  :-DayFragment.makeView
	//   -DayFragment.goTo
	//
	// -DayView.initView : �ϴ� setSelectedEvent ȣ�� �κ��� �ּ����� ó����
	//  :-DayView.initNextView
	//    :-DayView.doScroll
	//     -DayView.mDeltaOfReturnInPlaceTranslateAnimator.addUpdateListener
	//
	// -DayView.resetSelectedHour :�ϴ� setSelectedEvent ȣ�� �κ��� �ּ����� ó����
	//  
	// -DayView.computeAllDayNeighbors
	//
	// -DayView.computeNeighbors
	//
	// -DayView.findSelectedEvent
	//  :-DayView.setSelectionFromPosition
	
	
	EventCursors mSelectedEventObj;
	public void setSelectedEvent(Event e) {
    	if (e == null) {
    		//if (INFO) Log.i(TAG, "setSelectedEvent:Event is NULL");
    		mSelectedEvent = null;
    	}
    	else {    	
    		//if (INFO) Log.i(TAG, "setSelectedEvent:Event is SET");    		
	        mSelectedEvent = e;
	        
	        mEventTimeModificationManager.loadEventInfoInBackgroundForEditEventTime(e);        
	        
	        //DayView curView = (DayView) getCurrentDayView();	
	        mSelectedEventObj = getEventCursors(mSelectedEvent.id);
    	}
    	
    }
	
	public Event getSelectedEvent() {
    	
        return mSelectedEvent;
        
    }
	
	public EventCursors getSelectedEventCursors() {
		return mSelectedEventObj;
	}
	
	public boolean mSelectedEventRequiredUpdate = false;
	public boolean mSelectedEventUpdateCompletion = false;
	
	public void resetSelectedEventRequireUpdateFlags() {
		mSelectedEventRequiredUpdate = false;
		mSelectedEventUpdateCompletion = false;
	}
	
	public void removeSelectedEventPanelTouchListener() {
		mSelectedEventPanel.setOnTouchListener(null);		
	}
	
	public void resetSelectedEventPanelTouchListener() {
		mSelectedEventPanel.setOnTouchListener(this);
	}
	
	/*
	public void testSeperateHeight() {		
				
		int frameHeight = mFrameLayout.getHeight();

        int frameLayoutViewSwitcherHeight = mFrameLayoutViewSwitcher.getHeight();

        int lowerActionBarHeight = mLowerActionBar.getHeight();

        
        // Height of Child Views Inner DayView
        int dayViewLayoutHeight = mDayViewLayout.getHeight();
        
        int calendarViewsSecondaryActionBarHeight = mCalendarViewsSecondaryActionBar.getHeight();

        int dayViewSwitcherHeight = mDayViewSwitcher.getHeight();
        
        DayView curView = (DayView) getCurrentDayView();	
        int curDayViewHeight = curView.getHeight();
        
        int dayviewPsudoLowerActionBarHeight = mDayViewPsudoLowerActionBar.getHeight();
        
        int selectedEventPanelHeight = mSelectedEventPanel.getHeight();
        int selectedEventRectHeight = mSelectedEventRect.getHeight();
        
        if (INFO) Log.i(TAG, "testSeperateHeight");
	}
	*/
		
	RelativeLayout mEventTimeModificationPane;
	CommonRoundedCornerLinearLayout mEventTimeModificationDb;
	LinearLayout mEventTimeModificationDbThisEventUpdateMenu;
	LinearLayout mEventTimeModificationDbAllSeriesEventUpdateMenu;
	LinearLayout mEventTimeModificationDbThisAndFutureEventUpdateMenu;
	
	TextView mEventTimeModificationDbThisEventUpdateTextView; //this_event_update_text_label
	TextView mEventTimeModificationDbAllSeriesEventUpdateTextView;
	TextView mEventTimeModificationDbThisAndFutureUpdateTextView; //this_and_future_event_update_text_label
	
	TextView mEventTimeModificationDbCancelTextView;
	
	public void makeEventTimeModificationOkDB() {
		calcDialogBoxwidth();
				
		int width = mAppWidth;
		int height = mAppHeight;	
		
		mEventTimeModificationPane = (RelativeLayout)mInflater.inflate(R.layout.event_time_modification_layout, null);
		LayoutParams paneParams = new LayoutParams(width, height); 	
		mEventTimeModificationPane.setLayoutParams(paneParams);			
				
		LayoutParams dialogParams = new LayoutParams(mEventTimeModificationDbWidth, LayoutParams.WRAP_CONTENT); 		
		mEventTimeModificationDb = (CommonRoundedCornerLinearLayout) mInflater.inflate(R.layout.event_time_modification_db_layout, null, false);
		mEventTimeModificationDb.setLayoutParams(dialogParams);				
		mEventTimeModificationPane.addView(mEventTimeModificationDb);		
		
		mEventTimeModificationDbThisEventUpdateMenu = (LinearLayout)mEventTimeModificationDb.findViewById(R.id.this_event_modify);
		mEventTimeModificationDbThisEventUpdateTextView = (TextView) mEventTimeModificationDbThisEventUpdateMenu.findViewById(R.id.this_event_modify_text);
		mEventTimeModificationDbThisEventUpdateTextView.setOnClickListener(mEventTimeModificationDbThisUpdateTextViewClickListener);
		
		mEventTimeModificationDbAllSeriesEventUpdateMenu = (LinearLayout)mEventTimeModificationDb.findViewById(R.id.all_series_modify);
		mEventTimeModificationDbAllSeriesEventUpdateTextView = (TextView) mEventTimeModificationDbAllSeriesEventUpdateMenu.findViewById(R.id.all_series_event_modify_text);
		mEventTimeModificationDbAllSeriesEventUpdateTextView.setOnClickListener(mEventTimeModificationDbAllSeriesUpdateTextViewClickListener);
		
		mEventTimeModificationDbThisAndFutureEventUpdateMenu = (LinearLayout)mEventTimeModificationDb.findViewById(R.id.this_and_future_modify);
		mEventTimeModificationDbThisAndFutureUpdateTextView = (TextView) mEventTimeModificationDbThisAndFutureEventUpdateMenu.findViewById(R.id.this_and_future_event_modify_text);
		mEventTimeModificationDbThisAndFutureUpdateTextView.setOnClickListener(mEventTimeModificationDbThisAndFutureUpdateTextViewClickListener);
				
		mEventTimeModificationDbCancelTextView = (TextView) mEventTimeModificationDb.findViewById(R.id.modification_cancel_text_label);
		mEventTimeModificationDbCancelTextView.setOnClickListener(mEventTimeModificationDbCancelTextViewClickListener);	
	}
	
	public void popUpEventTimeModificationOkDB(long newStartMillis, long newEndMillis) {
		long id = mEventTimeModificationManager.getEventIdLoadedEventInfo();
		if (id != this.getSelectedEvent().id) {
			// �̷� ��� �ٽ�...load request�� ��û�ؾ� �Ѵ�...
			// 
			return;
		}
		
		if (mEventTimeModificationManager.getLoadEventJopCompletionStatus()) {
			
		}
		else {
			if (mEventTimeModificationManager.isCanceledLoadEventJob()) {
				// cancel �� ������ �˰� retry�ؾ� ���� �ʴ°�?
				return;
			}
			else {
				// ���� ó���ϰ� �ִ� ���̶� �ֱ��ΰ�?
				// ó�����̶�� ��� �ؾ� �ϴ°�?
				
				return;
			}
		}
		
		mEventTimeModificationManager.setNewEventTime(newStartMillis, newEndMillis);	
		
		setEventTimeModificationOkDBMenu();
		
		mEventTimeModificationDb.setResizeView(mEventTimeModificationDbScale);	
		
		RelativeLayout contentView = (RelativeLayout) mActivity.getContentView();
		contentView.addView(mEventTimeModificationPane);		
		
		setEventModificationDBEnterAnim();
	}
	
	int mEventTimeModificationOkDBMenuID;
	public void setEventTimeModificationOkDBMenu() {
		mEventTimeModificationOkDBMenuID = mEventTimeModificationManager.getMenuInDB();
		switch(mEventTimeModificationOkDBMenuID) {
		case 1:
			// modify_all �� �����Ѵ�
			if (mEventTimeModificationDbAllSeriesEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbAllSeriesEventUpdateMenu.setVisibility(View.VISIBLE);
			
			mEventTimeModificationDbThisEventUpdateMenu.setVisibility(View.GONE);
			mEventTimeModificationDbThisAndFutureEventUpdateMenu.setVisibility(View.GONE);
			
			break;
		case 2:
			// modify_all
            // modify_all_following			
            // �� ���� �����ϰ� �ȴ�
			if (mEventTimeModificationDbAllSeriesEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbAllSeriesEventUpdateMenu.setVisibility(View.VISIBLE);
			
			if (mEventTimeModificationDbThisAndFutureEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbThisAndFutureEventUpdateMenu.setVisibility(View.VISIBLE);
			
			mEventTimeModificationDbThisEventUpdateMenu.setVisibility(View.GONE);
			break;
		case 3:
			// modify_event
            // modify_all
			// �� ���� �����ϰ� �ȴ�
			if (mEventTimeModificationDbThisEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbThisEventUpdateMenu.setVisibility(View.VISIBLE);
			
			if (mEventTimeModificationDbAllSeriesEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbAllSeriesEventUpdateMenu.setVisibility(View.VISIBLE);
			
			mEventTimeModificationDbThisAndFutureEventUpdateMenu.setVisibility(View.GONE);
			
			break;
		case 4:
			// modify_event
            // modify_all
            // modify_all_following
            // �� ���� �����ϰ� �ȴ�
			if (mEventTimeModificationDbThisEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbThisEventUpdateMenu.setVisibility(View.VISIBLE);
			
			if (mEventTimeModificationDbAllSeriesEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbAllSeriesEventUpdateMenu.setVisibility(View.VISIBLE);
			
			if (mEventTimeModificationDbThisAndFutureEventUpdateMenu.getVisibility() != View.VISIBLE)
				mEventTimeModificationDbThisAndFutureEventUpdateMenu.setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}
	}
	
	//int mEventTimeModificationDbWidth;
	//int mEventTimeModificationDbHeight;
	final float mEventTimeModificationDbScaleRatio = 1.25f;
	float mEventTimeModificationDbScaleRatioDelta = 0;
	final float mEventTimeModificationPaneAlphaRatio = 0.6f;
	int mEventTimeModificationPaneAlpha = 0;
	Runnable mEventTimeModificationDbScale = new Runnable() {

		@Override
		public void run() {			
			//mEventTimeModificationDbHeight = mEventTimeModificationDb.getHeight();
			
			mEventTimeModificationDb.setScaleX(mEventTimeModificationDbScaleRatio);
			mEventTimeModificationDb.setScaleY(mEventTimeModificationDbScaleRatio);			
			
			mEnterEventModificationDBVA.start();
		}
		
	};
	
	ValueAnimator mEnterEventModificationDBVA;
	public void setEventModificationDBEnterAnim() {
		mEventTimeModificationDbScaleRatioDelta = mEventTimeModificationDbScaleRatio - 1;
		mEventTimeModificationPaneAlpha = (int) (255 * mEventTimeModificationPaneAlphaRatio);
		
		mEnterEventModificationDBVA = ValueAnimator.ofFloat(1, 0);
		
		//long duration = calculateDuration(Math.abs(mDeltaOfReturnInPlace), mViewWidth, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
		long duration = 300;
		mEnterEventModificationDBVA.setDuration(duration);
		mEnterEventModificationDBVA.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float value = (Float) valueAnimator.getAnimatedValue();
				float scaleRatio = mEventTimeModificationDbScaleRatio - (mEventTimeModificationDbScaleRatioDelta - (mEventTimeModificationDbScaleRatioDelta * value));
				
				// where 1.0 means fully opaque and 0.0 means fully transparent.
				// alphaValue�� 0.0 ~ 0.6����...
				// 0 -> 153[60%]�� �����ؾ� �Ѵ�				
				float alphaRatio = 1 - value;
				int alphaValue = (int) (mEventTimeModificationPaneAlpha * alphaRatio);				
				int color = Color.argb(alphaValue, 0, 0, 0);
				mEventTimeModificationPane.setBackgroundColor(color);			
				
				mEventTimeModificationDb.setScaleX(scaleRatio);
				mEventTimeModificationDb.setScaleY(scaleRatio);
				
			}
		});
		
		mEnterEventModificationDBVA.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {			
				
			}

			@Override
			public void onAnimationStart(Animator animator) {				
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});		
		
	}
	
	AnimationListener mEventTimeModificationPaneAlphaAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			//android:background="#99000000"
			// alpha 255 is fully opaque
			int color = Color.argb(127, 0, 0, 0);
			mEventTimeModificationPane.setBackgroundColor(color);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}		
	};
	
	
	
	public void EventTimeModificationDbDismiss() {
			
		RelativeLayout contentView = (RelativeLayout) mActivity.getContentView();
		
		contentView.removeView(mEventTimeModificationPane);		
	}
	
	OnClickListener mEventTimeModificationDbThisUpdateTextViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mEventTimeModificationManager.modifyEvent(Utils.MODIFY_SELECTED);
			EventTimeModificationDbDismiss();
			resetSelectedEventPanelTouchListener();
			setSelectionMode(DayView.SELECTION_EVENT_FLOATING, DayView.SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
		}
	};
	
	OnClickListener mEventTimeModificationDbAllSeriesUpdateTextViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {		
			mEventTimeModificationManager.modifyEvent(Utils.MODIFY_ALL);
			EventTimeModificationDbDismiss();
			resetSelectedEventPanelTouchListener();
			setSelectionMode(DayView.SELECTION_EVENT_FLOATING, DayView.SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
		}
	};
	
	OnClickListener mEventTimeModificationDbThisAndFutureUpdateTextViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mEventTimeModificationManager.modifyEvent(Utils.MODIFY_ALL_FOLLOWING);
			EventTimeModificationDbDismiss();
			resetSelectedEventPanelTouchListener();
			setSelectionMode(DayView.SELECTION_EVENT_FLOATING, DayView.SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
		}
	};
	
	
	OnClickListener mEventTimeModificationDbCancelTextViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
						
			// ���� �ڸ� �Ǵ� ������� ���ư��� �ؾ� ��!!!
			DayView curDayView = (DayView) getCurrentDayView();
			curDayView.cancelSelectedEventMovedOrExpandingModification();
			
			EventTimeModificationDbDismiss();
			
		}
	};
	
	
    
    private final Runnable mCurrentDayViewEventInfoLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	Log.i("tag", "Called EventInfoLoaderCancelCallback");
        }
    };
    
    private final Runnable mPreviousDayEventInfoLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	Log.i("tag", "Called EventInfoLoaderCancelCallback");
        }
    };
    
    private final Runnable mNextDayViewEventInfoLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	Log.i("tag", "Called EventInfoLoaderCancelCallback");
        }
    };
    
    
    
    public static final int CURRENT_DAY_EVENTS = 0;
	public static final int PREVIOUS_DAY_EVENTS = 1;
	public static final int NEXT_DAY_EVENTS = 2;
	public class CircularDayEventsInfo/* implements Runnable*/ {
		int mWhichDay;
		int mFirstJulianDay;
		int mEventsLoadedFirstJulianDay = 0;
		long mLastEventsReloadMillis = 0;
		//boolean mRemeasureDayView = false;
		
		ArrayList<Event> mEvents = null;
		ArrayList<Event> mAllDayEvents = null;
		ArrayList<EventCursors> mEventCursors = null;
		
		//EventInfoLoader mEventInfoLoader;
		//Runnable mEventInfoLoaderSuccessCallback;
		//Runnable mEventInfoLoaderCancelCallback;
		
		CircularEventsInfoLoadSuccessCallback mCircularEventsInfoLoadSuccessCallback = new CircularEventsInfoLoadSuccessCallback();
		CircularEventsInfoLoadCancelCallback mCircularEventsInfoLoadCancelCallback = new CircularEventsInfoLoadCancelCallback();
		
		Object mSynced = new Object();	
		
		public CircularDayEventsInfo() {
			mWhichDay = 0;	
			mFirstJulianDay = 0;
			//mEventInfoLoader = null;
			//mEventInfoLoaderSuccessCallback = null;
			//mEventInfoLoaderCancelCallback = null;
			
			mEvents = new ArrayList<Event>();
			mAllDayEvents = new ArrayList<Event>();
			mEventCursors = new ArrayList<EventCursors>();
		}
		
		public CircularDayEventsInfo(boolean notForceUpdated) {
			mWhichDay = 0;	
			mFirstJulianDay = 0;
			//mEventInfoLoader = null;
			//mEventInfoLoaderSuccessCallback = null;
			//mEventInfoLoaderCancelCallback = null;
			
			mEvents = new ArrayList<Event>();
			mAllDayEvents = new ArrayList<Event>();
			mEventCursors = new ArrayList<EventCursors>();		
		}
		
		public CircularDayEventsInfo(int whichDay, int firstJulianDay, EventInfoLoader eventInfoLoader) {
			mWhichDay = whichDay;	
			mFirstJulianDay = firstJulianDay;
			//mEventInfoLoader = eventInfoLoader;
			//mEventInfoLoaderSuccessCallback = eventInfoLoaderSuccessCallback;
			//mEventInfoLoaderCancelCallback = eventInfoLoaderCancelCallback;
			
			mEvents = new ArrayList<Event>();
			mAllDayEvents = new ArrayList<Event>();
			mEventCursors = new ArrayList<EventCursors>();
		}	
		
		public boolean getReloadCompletionStatus() {
			
			if (mEventsLoadedFirstJulianDay == mFirstJulianDay)
				return true;
			else
				return false;			
		}		
		
		
		public class CircularEventsInfoLoadSuccessCallback implements Runnable {

			@Override
			public void run() {
				
				// ����ȭ�� ���� Object�� �����ؾ� �Ѵ�
				synchronized(mSynced)
				{				
					mEventsLoadedFirstJulianDay = mFirstJulianDay;  	
					
				}
				
			}
	    	
	    }
		
		
		public class CircularEventsInfoLoadCancelCallback implements Runnable {

			@Override
			public void run() {
				mLastEventsReloadMillis = 0;				
			}
	    	
	    }
	    
	}
	
	//ArrayList<CircularEvents> mCircularEventsList = new ArrayList<CircularEvents>();
	Map<Integer, CircularDayEventsInfo> mCircularThreeDaysEventsInfoMap = new HashMap<Integer, CircularDayEventsInfo>();  
	
	public class EventInfoLoaderSuccessCallback implements Runnable {

		int mWhichDay;
		ArrayList<Event> mEvents = null;
		ArrayList<EventCursors> mEventCursors;
		
		public EventInfoLoaderSuccessCallback(int whichDay, ArrayList<Event> events, ArrayList<EventCursors> eventCursors) {
			mWhichDay = whichDay;
			mEvents = events;
			mEventCursors = eventCursors;
		}
		
		@Override
		public void run() {
			
			boolean empty = mEventCursors.isEmpty();
        	if (empty)
        		Log.i("tag", "mEventInfoLoaderSuccessCallback:what???");
        	else {
        		Log.i("tag", "mEventInfoLoaderSuccessCallback:SUCCESS");
        		
        		if (mWhichDay == DayFragment.CURRENT_DAY_EVENTS) {
        		// ���⼭ �츮�� selected event�� id�� mEventCursors�� �ִ� ���� Ȯ���Ѵٸ�...
	        		if (getSelectionMode() == DayView.SELECTION_EVENT_FLOATING) {
	        			// �츮�� ���� mSelectedEvents�� ��Ȯ�ϰ� update�ϰ� ���� �ʴ�.
	        			// ������ mSelectedEvents�� clear�ϰ�
	        			// mSelectedEvnet �� ���� add�ϰ� �ִ�
	        			// :���� ��Ȯ�� mSelectedEvents�� �뵵�� �𸣰� �ִ�
	        			//  �̴� allday ��Ʈ�� �����ϸ鼭 ��Ȯ�� �ľ��� �� �ۿ� ����
	        			long targetEventId = getSelectedEvent().id;
	        			int numEvents = mEvents.size();
	        			for (int i = 0; i < numEvents; i++) {
	        	            Event event = mEvents.get(i);
	        	            
	        	            if (targetEventId == event.id) {
	        	            	if (mSelectedEventRequiredUpdate)
	        	            		mSelectedEventUpdateCompletion = true;
	        	            	        	            	
	        	            	DayView view = (DayView) DayFragment.this.getCurrentDayView();
	        	            	view.mSelectedEvents.clear();
	        	            	view.mSelectedEvents.add(event);
	        	            	setSelectedEvent(event);        	            	
	        	            }       	            
	        	        }
	        		}
        		}
        	}
		}
    	
    }
	
	
	public void reloadDayViewEventsInfo(CircularDayEventsInfo events, int julianDay/*Time baseDate*/) { 				
        // Make sure our time zones are up to date
        //mTZUpdater.run(); // DayView�� mTZUpdater�� �����Ǿ�� �Ѵ�

        // The start date is the beginning of the week at 12am
		//String timezoneid = Utils.getTimeZone(mContext, mTZUpdater);
		//TimeZone timezone = TimeZone.getTimeZone(timezoneid);
		//Calendar weekStart = GregorianCalendar.getInstance(timezone);
		
		long julianDayMillis = ETime.getMillisFromJulianDay(julianDay, mTimeZone, mFirstDayOfWeek);
		//weekStart.setTimeInMillis(julianDayMillis);
		
        //weekStart.setJulianDay(julianDay);             
        //weekStart.set(baseDate);
        //weekStart.hour = 0;
        //weekStart.minute = 0;
        //weekStart.second = 0;
        //long millis = weekStart.normalize(true /* ignore isDst */);

		long millis = julianDayMillis;
        // Avoid reloading events unnecessarily.
        if (millis == events.mLastEventsReloadMillis) {
            return;
        }
        events.mLastEventsReloadMillis = millis;
        
        events.mEventCursors.clear();
        
        EventInfoLoader eiL = mEventInfoLoaders.get(events.mWhichDay);
        eiL.loadEventsInfoInBackground(events.mEventCursors, events.mEvents, events.mCircularEventsInfoLoadSuccessCallback, events.mCircularEventsInfoLoadCancelCallback);              
    }	
	
	public static final int NON_SWAP_EVENTS = 0;
	public static final int SWAP_EVENTS_FORWARD_MOVE = 1;
	public static final int SWAP_EVENTS_BACKWARD_MOVE = 2;	
	
	CircularDayEventsInfo mNewDayEventsInfoWillUpdated;
	public void reloadNewDayViewEventsInfoBeforeSwapEvents(int whichDay) {
		
		mNewDayEventsInfoWillUpdated = new CircularDayEventsInfo(true);
		mNewDayEventsInfoWillUpdated.mWhichDay = whichDay;
		
		ManageEventsPerWeek curWeekEventsObj = mManageEventsPerWeekHashMap.get(CURRENT_WEEK_EVENTS);
		
		if (whichDay == PREVIOUS_DAY_EVENTS) {
			Calendar newPreviousDay = GregorianCalendar.getInstance(mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS).mDayView.mBaseDate.getTimeZone());//new Time(mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS).mDayView.mBaseDate);
			newPreviousDay.setFirstDayOfWeek(mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS).mDayView.mBaseDate.getFirstDayOfWeek());
			newPreviousDay.setTimeInMillis(mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS).mDayView.mBaseDate.getTimeInMillis());
			newPreviousDay.add(Calendar.DAY_OF_MONTH, -1); //newPreviousDay.monthDay = newPreviousDay.monthDay - 1;
			long gmtoff = mTimeZone.getRawOffset() / 1000;			
	    	int newPreviousDayViewFirstJulianDay = ETime.getJulianDay(newPreviousDay.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
	    	
	    	if (curWeekEventsObj.mWeekStartJulianDay > newPreviousDayViewFirstJulianDay) {
	    		// current week�� prv1Week�� �ǰ�
	    		// prv4Week�� event���� ���� load �ؾ� �Ѵ�...
	    		// �׸��� next4Week event���� clear �ؾ� �Ѵ�
	    		// :next4Week�� ���� prv4Week�� �Ǿ�� �Ѵ�
	    		/*
	    		 PREVIOUS_4_WEEK_EVENTS -> PREVIOUS_3_WEEK_EVENTS
	    		 PREVIOUS_3_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS	    		 
	    	     PREVIOUS_2_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS
	    	     PREVIOUS_1_WEEK_EVENTS -> CURRENT_WEEK_EVENTS	    	     
	    	     CURRENT_WEEK_EVENTS -> NEXT_1_WEEK_EVENTS
	    	     NEXT_1_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS
	    	     NEXT_2_WEEK_EVENTS -> NEXT_3_WEEK_EVENTS
	    	     NEXT_3_WEEK_EVENTS -> NEXT_4_WEEK_EVENTS
	    	     NEXT_4_WEEK_EVENTS -> new PREVIOUS_4_WEEK_EVENTS
	    	    */
	    		
	    		//PREVIOUS_4_WEEK_EVENTS�� -��[-4]�̴�
	    		// ���� +1�� ���ϸ�...?
	    		// :�� NEXT_4_WEEK_EVENTS�� ��ȣ�� ���� ���� �ش�...	    		
	    		int UpdateParaValue = 1;
	    		int willBeLoadNewWeek = PREVIOUS_4_WEEK_EVENTS;
	    		int willBeLoadNewWeekPosition = 0;
	    		int thrownOutWeek = NEXT_4_WEEK_EVENTS;
	    		ArrayList <ManageEventsPerWeek> objList = new ArrayList <ManageEventsPerWeek>();
	    		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
	        			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
	    		 
	        		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
	        		int key = entry.getKey();
	        		ManageEventsPerWeek valueObj = entry.getValue();
	        		if (key == thrownOutWeek) {	     
	        			// NEXT_4_WEEK_EVENTS[4] -> PREVIOUS_4_WEEK_EVENTS[-4]
	        			valueObj.mWhichSideWeek = -(valueObj.mWhichSideWeek);			        			
	        		}	        		
	        		else { 
	        			if (key == willBeLoadNewWeek) {
	        				willBeLoadNewWeekPosition = valueObj.mWeekPosition - 1;	        				
		        		}
	        			
	        			valueObj.mWhichSideWeek = valueObj.mWhichSideWeek + UpdateParaValue;        			
	        		}
	        		
	        		objList.add(valueObj);	        		   		
	        	}	   
	    		
	    		removeManageEventsPerWeekHashMap();  
	    		
	    		reMapManageEventsPerWeekHashMap(objList, willBeLoadNewWeek, willBeLoadNewWeekPosition);			
	    	}
	    	
	    	setNewDayEventsInfo(newPreviousDayViewFirstJulianDay);
	    	
	    	reloadDayViewEventsInfo(mNewDayEventsInfoWillUpdated, newPreviousDayViewFirstJulianDay);
		}
		else if (whichDay == NEXT_DAY_EVENTS) {
			Calendar newNextDay = GregorianCalendar.getInstance(mExtendedDayViewsMap.get(NEXT_DAY_EVENTS).mDayView.mBaseDate.getTimeZone());//Time newNextDay = new Time(mExtendedDayViewsMap.get(NEXT_DAY_EVENTS).mDayView.mBaseDate);
			newNextDay.setFirstDayOfWeek(mExtendedDayViewsMap.get(NEXT_DAY_EVENTS).mDayView.mBaseDate.getFirstDayOfWeek());
			newNextDay.setTimeInMillis(mExtendedDayViewsMap.get(NEXT_DAY_EVENTS).mDayView.mBaseDate.getTimeInMillis());
			newNextDay.add(Calendar.DAY_OF_MONTH, 1); //newNextDay.monthDay = newNextDay.monthDay + 1;
			
			long gmtoff = mTimeZone.getRawOffset() / 1000;	
	    	int newNextDayViewFirstJulianDay = ETime.getJulianDay(newNextDay.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
	    	
	    	if (newNextDayViewFirstJulianDay > curWeekEventsObj.mWeekEndJulianDay) {
	    		// current week�� next1Week�� �ǰ�
	    		// next4Week�� event���� ���� load �ؾ� �Ѵ�...
	    		// �׸��� prv4Week event���� clear �ؾ� �Ѵ�
	    		// :prv4Week�� ���� next4Week�� �Ǿ�� �Ѵ�
	    		/*
	    		 PREVIOUS_4_WEEK_EVENTS -> new NEXT_4_WEEK_EVENTS
	    		 PREVIOUS_3_WEEK_EVENTS -> PREVIOUS_4_WEEK_EVENTS	    		 
	    	     PREVIOUS_2_WEEK_EVENTS -> PREVIOUS_3_WEEK_EVENTS
	    	     PREVIOUS_1_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS	    	     
	    	     CURRENT_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS
	    	     NEXT_1_WEEK_EVENTS -> CURRENT_WEEK_EVENTS
	    	     NEXT_2_WEEK_EVENTS -> NEXT_1_WEEK_EVENTS
	    	     NEXT_3_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS
	    	     NEXT_4_WEEK_EVENTS -> NEXT_3_WEEK_EVENTS
	    	    */
	    		
	    		// ���� -1�� ���ϸ�...?
	    		// :�� PREVIOUS_4_WEEK_EVENTS�� ��ȣ�� ���� ���� �ش�...
	    		int UpdateParaValue = -1;
	    		int willBeLoadNewWeek = NEXT_4_WEEK_EVENTS;
	    		int willBeLoadNewWeekPosition = 0;
	    		int thrownOutWeek = PREVIOUS_4_WEEK_EVENTS;
	    		ArrayList <ManageEventsPerWeek> objList = new ArrayList <ManageEventsPerWeek>();
	    		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
	        			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
	    		 
	        		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
	        		int key = entry.getKey();
	        		ManageEventsPerWeek valueObj = entry.getValue();
	        		if (key == thrownOutWeek) {	        			
	        			valueObj.mWhichSideWeek = -(valueObj.mWhichSideWeek);			        		
	        		}	        		
	        		else {   
	        			if (key == willBeLoadNewWeek) {
	        				willBeLoadNewWeekPosition = valueObj.mWeekPosition + 1;	        				
		        		}
	        			
	        			valueObj.mWhichSideWeek = valueObj.mWhichSideWeek + UpdateParaValue;        			
	        		}
	        		
	        		objList.add(valueObj);	        		       		
	        	}	    		   		
	    		
	    		removeManageEventsPerWeekHashMap();   
	    		
	    		reMapManageEventsPerWeekHashMap(objList, willBeLoadNewWeek, willBeLoadNewWeekPosition);	    		
	    	}
	    	
	    	setNewDayEventsInfo(newNextDayViewFirstJulianDay);
	    	
	    	reloadDayViewEventsInfo(mNewDayEventsInfoWillUpdated, newNextDayViewFirstJulianDay);
		}	
	}
	
	public void setNewDayEventsInfo(int newDayViewFirstJulianDay) {
		mNewDayEventsInfoWillUpdated.mLastEventsReloadMillis = 0;
    	mNewDayEventsInfoWillUpdated.mEventsLoadedFirstJulianDay = 0;
    	mNewDayEventsInfoWillUpdated.mFirstJulianDay = newDayViewFirstJulianDay;	    	   
    	
    	int weekPositionOfNewDay = ETime.getWeeksSinceEcalendarEpochFromJulianDay(newDayViewFirstJulianDay, mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(newDayViewFirstJulianDay, mFirstDayOfWeek);     	
    	
    	mNewDayEventsInfoWillUpdated.mEvents = getEventsOfTargetDayInWeek(weekPositionOfNewDay, mNewDayEventsInfoWillUpdated.mFirstJulianDay);
    	mNewDayEventsInfoWillUpdated.mAllDayEvents = getAllDayEvents(mNewDayEventsInfoWillUpdated.mEvents);  
    	
	}
	
	public void swapEvents(int swapCase) {
		CircularDayEventsInfo currentEvents = mCircularThreeDaysEventsInfoMap.get(CURRENT_DAY_EVENTS);
		CircularDayEventsInfo nextEvents = mCircularThreeDaysEventsInfoMap.get(NEXT_DAY_EVENTS);
		CircularDayEventsInfo previousEvents = mCircularThreeDaysEventsInfoMap.get(PREVIOUS_DAY_EVENTS);
				
		ExtendedDayView curExtendedDayView = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
		ExtendedDayView prvExtendedDayView = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
		ExtendedDayView nextExtendedDayView = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
		
		switch(swapCase) {
		case SWAP_EVENTS_FORWARD_MOVE:
			mExtendedDayViewsMap.remove(CURRENT_DAY_EVENTS);
	    	mExtendedDayViewsMap.remove(PREVIOUS_DAY_EVENTS);
	    	mExtendedDayViewsMap.remove(NEXT_DAY_EVENTS);
	    	
	    	// nextEvents�� CURRENT_DAY_EVENTS�� �����Ǿ�� �Ѵ�
	    	nextEvents.mWhichDay = CURRENT_DAY_EVENTS;
	    				
			// currentEvents�� PREVIOUS_DAY_EVENTS�� �����Ǿ�� �Ѵ�
			currentEvents.mWhichDay = PREVIOUS_DAY_EVENTS;			
			
			// ���ο� mNextDayViewEvents�� load �ؾ� �Ѵ�
			// ������ previousEvents�� �� �̻� �ʿ� ���� ������
			// previousEvents�� ���ο� NEXT_DAY_EVENTS�� ��������
			previousEvents.mWhichDay = NEXT_DAY_EVENTS;
			//CircularEvents newNextEvents = previousEvents;
			Calendar newNextDay = GregorianCalendar.getInstance(nextExtendedDayView.mDayView.mBaseDate.getTimeZone());//new Time(nextExtendedDayView.mDayView.mBaseDate);
			newNextDay.setFirstDayOfWeek(nextExtendedDayView.mDayView.mBaseDate.getFirstDayOfWeek());
			newNextDay.setTimeInMillis(nextExtendedDayView.mDayView.mBaseDate.getTimeInMillis());
			newNextDay.add(Calendar.DAY_OF_MONTH, 1); //newNextDay.monthDay = newNextDay.monthDay + 1;
						
	    	// PREVIOUS_DAY_EVENTS�� mCurrentDayViewEvents�� �����Ǿ�� �Ѵ�
	    	mExtendedDayViewsMap.put(PREVIOUS_DAY_EVENTS, curExtendedDayView);
	    				
	    	// CURRENT_DAY_EVENTS�� nextExtendedDayView�� �����Ǿ�� �Ѵ�
			mExtendedDayViewsMap.put(CURRENT_DAY_EVENTS, nextExtendedDayView);			
			
			// NEXT_DAY_EVENTS�� prvExtendedDayView�� �����Ǿ�� �Ѵ�
			mExtendedDayViewsMap.put(NEXT_DAY_EVENTS, prvExtendedDayView);
			
			nextExtendedDayView = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
			curExtendedDayView = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
			prvExtendedDayView = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);	
			
			mCurrentDayViewBaseTime.setTimeInMillis(curExtendedDayView.mDayView.mBaseDate.getTimeInMillis());
			
			
			nextExtendedDayView.setX(mDayViewWidth);	
			curExtendedDayView.setX(0);
			prvExtendedDayView.setX(-mDayViewWidth);    	
				
			nextExtendedDayView.mDayView.setBaseTime(newNextDay);
			nextExtendedDayView.mDayView.setEventsX(mNewDayEventsInfoWillUpdated.mEvents/*newNextDayEvents*/);		
			setAllDayEventListView(mNewDayEventsInfoWillUpdated.mAllDayEvents, NEXT_DAY_EVENTS);    	
	    				
			mCircularThreeDaysEventsInfoMap.remove(CURRENT_DAY_EVENTS);
			mCircularThreeDaysEventsInfoMap.remove(PREVIOUS_DAY_EVENTS);
			mCircularThreeDaysEventsInfoMap.remove(NEXT_DAY_EVENTS);		    	
	    	
			mCircularThreeDaysEventsInfoMap.put(PREVIOUS_DAY_EVENTS, currentEvents);
			mCircularThreeDaysEventsInfoMap.put(CURRENT_DAY_EVENTS, nextEvents);		
			mCircularThreeDaysEventsInfoMap.put(NEXT_DAY_EVENTS, mNewDayEventsInfoWillUpdated);
			
			break;
		
		case SWAP_EVENTS_BACKWARD_MOVE:		
			mExtendedDayViewsMap.remove(CURRENT_DAY_EVENTS);
	    	mExtendedDayViewsMap.remove(PREVIOUS_DAY_EVENTS);
	    	mExtendedDayViewsMap.remove(NEXT_DAY_EVENTS);
	    	
			// currentEvents�� NEXT_DAY_EVENTS�� �����Ǿ�� �Ѵ�
			currentEvents.mWhichDay = NEXT_DAY_EVENTS;			
			
			// previousEvents�� ���� CURRENT_DAY_EVENTS�� �����Ǿ�� �Ѵ�
			previousEvents.mWhichDay = CURRENT_DAY_EVENTS;
			
			// ���ο� previousEvents�� load �ؾ� �Ѵ�
			// ������ nextEvents�� �� �̻� �ʿ� ���� ������
			// ������ nextEvents�� ���ο� PREVIOUS_DAY_EVENTS�� ��������
			nextEvents.mWhichDay = PREVIOUS_DAY_EVENTS;			
			
			Calendar newPreviousDay = GregorianCalendar.getInstance(prvExtendedDayView.mDayView.mBaseDate.getTimeZone());//new Time(prvExtendedDayView.mDayView.mBaseDate);
			newPreviousDay.setFirstDayOfWeek(prvExtendedDayView.mDayView.mBaseDate.getFirstDayOfWeek());
			newPreviousDay.setTimeInMillis(prvExtendedDayView.mDayView.mBaseDate.getTimeInMillis());
			newPreviousDay.add(Calendar.DAY_OF_MONTH, -1);
				    	
			mExtendedDayViewsMap.put(CURRENT_DAY_EVENTS, prvExtendedDayView);			
			
			mExtendedDayViewsMap.put(NEXT_DAY_EVENTS, curExtendedDayView);			
			
			mExtendedDayViewsMap.put(PREVIOUS_DAY_EVENTS, nextExtendedDayView);		
			
			curExtendedDayView = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
			prvExtendedDayView = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
			nextExtendedDayView = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
			
			mCurrentDayViewBaseTime.setTimeInMillis(curExtendedDayView.mDayView.mBaseDate.getTimeInMillis());
			
			curExtendedDayView.setX(0);
			prvExtendedDayView.setX(-mDayViewWidth);
			nextExtendedDayView.setX(mDayViewWidth);
			
			prvExtendedDayView.mDayView.setBaseTime(newPreviousDay); 	
			prvExtendedDayView.mDayView.setEventsX(mNewDayEventsInfoWillUpdated.mEvents/*newPreviousDayEvents*/);			
			setAllDayEventListView(mNewDayEventsInfoWillUpdated.mAllDayEvents, PREVIOUS_DAY_EVENTS);  
	    	
			mCircularThreeDaysEventsInfoMap.remove(CURRENT_DAY_EVENTS);
			mCircularThreeDaysEventsInfoMap.remove(PREVIOUS_DAY_EVENTS);
			mCircularThreeDaysEventsInfoMap.remove(NEXT_DAY_EVENTS);
	    	    	
			mCircularThreeDaysEventsInfoMap.put(CURRENT_DAY_EVENTS, previousEvents);
			mCircularThreeDaysEventsInfoMap.put(NEXT_DAY_EVENTS, currentEvents);		
			mCircularThreeDaysEventsInfoMap.put(PREVIOUS_DAY_EVENTS, mNewDayEventsInfoWillUpdated);			
						
			break;
		}
	}
	
	
	
	
	int mEventTimeModificationDbWidth;
	int mEventTimeModificationDbHeight;
	
	public void calcDialogBoxwidth() { 	
        
        float viewWidth = mAppWidth * 0.85f;
        mEventTimeModificationDbWidth = (int) viewWidth;
    }
	
	public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
	  
    public DayView getCurrentDayView() {
    	ExtendedDayView view = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
    	return view.mDayView;
    }
    
    public DayView getNextDayView() {
    	ExtendedDayView view = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
    	return view.mDayView;
    }
    
    public DayView getPreviousDayView() {
    	ExtendedDayView view = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
    	return view.mDayView;
    }
    
    public ExtendedDayView getCurrentExtendedDayView() {
    	
    	return mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
    }
    
    public ExtendedDayView getNextExtendedDayView() {
    	
    	return mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);
    }
    
    public ExtendedDayView getPreviousExtendedDayView() {
    	
    	return mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
    }
    
	
	float mScale;
	int HOURS_TOP_MARGIN = 2;
	int GRID_LINE_TOP_MARGIN;
	int mHoursWidth;
	int mCellWidth;
	int mRealHoursTextHeight;
	int mAlldayEventListViewMaxHeight;
    int mAlldayEventListViewItemHeight;
    int mAlldayEventListViewFirstItemTopMargin;
    int mAlldayEventListViewLastItemBottomMargin;
    int mAlldayEventListViewItemGap;
    public void setAlldayEventListViewDimension() {    	
    	
    	Paint p = new Paint();
    	p.setAntiAlias(true);
    	p.setTextSize(getResources().getDimension(R.dimen.hours_text_size));
        p.setTypeface(null);
        int hoursTextHeight = (int)Math.abs(p.ascent());    
        int hourDescent = (int) (p.descent() + 0.5f);
        mRealHoursTextHeight = hoursTextHeight + hourDescent;
    	
    	mScale = getResources().getDisplayMetrics().density;
    	
    	HOURS_TOP_MARGIN *= mScale;
    	GRID_LINE_TOP_MARGIN = (DayView.HOUR_GAP + HOURS_TOP_MARGIN) + ( mRealHoursTextHeight / 2);
    	
    	
    	float AMPM_TEXT_SIZE = (int) getResources().getDimension(R.dimen.ampm_text_size);
    	p.setTextSize(AMPM_TEXT_SIZE);
    	
    	String amString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
        String pmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
        String space = " ";        
    	
        String[] fullAmPm = {
        		amString + space + "12:59",
        		pmString + space + "11:59"
        };
    	int maxHourStringWidth = DayView.computeMaxStringWidth(0, fullAmPm, p);     
    	mHoursWidth = DayView.NEW_HOURS_LEFT_MARGIN + maxHourStringWidth + DayView.NEW_HOURS_RIGHT_MARGIN;
    	
    	int gridAreaWidth = mDayViewWidth - mHoursWidth;
        mCellWidth = (gridAreaWidth - (mNumDays * DayView.DAY_GAP)) / mNumDays; 
        
    	// all day event listview�� max height : dayview height�� 0.15�� �����Ѵ�
    	// all day event listview�� item height�� : all day event listview�� max height�� 0.35�� �����Ѵ�
    	// all day event listview�� first item top margin : GRID_LINE_TOP_MARGIN�� 0.5
    	// all day event listview�� last item bottom margin : GRID_LINE_TOP_MARGIN�� 0.5
    	// all day event listview�� item gap : first item top margin�� 0.5
    	
    	mAlldayEventListViewMaxHeight = (int) (mDayViewHeight * 0.15f);
    	mAlldayEventListViewItemHeight = (int) (mAlldayEventListViewMaxHeight * 0.35f);
    	mAlldayEventListViewFirstItemTopMargin = (int) (GRID_LINE_TOP_MARGIN * 0.5f);
    	mAlldayEventListViewLastItemBottomMargin = mAlldayEventListViewFirstItemTopMargin;
    	mAlldayEventListViewItemGap = (int) (mAlldayEventListViewFirstItemTopMargin * 0.5f);   
    	
    	mExtendedDayViewDimension = new ExtendedDayViewDimension(mDayViewWidth, mHoursWidth, mCellWidth, mAlldayEventListViewMaxHeight,
    			mAlldayEventListViewItemHeight, mAlldayEventListViewFirstItemTopMargin, mAlldayEventListViewLastItemBottomMargin, mAlldayEventListViewItemGap);
    }
    		
	int mDeviceWidth;
	int mDeviceHeight;
	int mStatusBarHeight;
	int mAppWidth;
	int mAppHeight;
	int mActionBarHeight;
	int mAnticipationFragmentFrameLayoutWidth;
	int mAnticipationFragmentFrameLayoutHeight;
	
	int DAY_HEADER_HEIGHT;
	int MONTHDAY_HEADER_HEIGHT;
    int DATEINDICATOR_HEADER_HEIGHT;
	int mDayViewSecondaryActionBarHeight;
	
	int mDayViewWidth;
    int mDayViewHeight;
	
	int mLowerActionBarHeight;
	
	int mAnticipationMonthDayListViewWidth;
	int mAnticipationMonthDayListViewHeight;
	int mAnticipationMonthDayListViewNormalWeekHeight;
	
	public void calcFrameLayoutDimension() {
		mDayViewWidth = mAnticipationFragmentFrameLayoutWidth = mAppWidth = mDeviceWidth = getResources().getDisplayMetrics().widthPixels;    	
				
		mDeviceHeight = getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = mActivity.getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle);    	
    	    	
    	mStatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (mStatusBarHeight == -1) {
        	Utils.setSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
    	
    	mAppHeight = mDeviceHeight - mStatusBarHeight;
		
		mActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_upper_actionbar_height);    
		
		mAnticipationFragmentFrameLayoutHeight = mAppHeight - mActionBarHeight;
		
		DAY_HEADER_HEIGHT = (int)getResources().getDimension(R.dimen.day_header_height);
    	mLowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);
    	
    	mAnticipationMonthDayListViewWidth = rectgle.right - rectgle.left;
    	mAnticipationMonthDayListViewHeight = mDeviceHeight - mStatusBarHeight - mActionBarHeight - DAY_HEADER_HEIGHT - mLowerActionBarHeight;  
    	
    	mAnticipationMonthDayListViewNormalWeekHeight = (int) (mAnticipationMonthDayListViewHeight * 0.16f);
    	
    	MONTHDAY_HEADER_HEIGHT = (int) (mAnticipationMonthDayListViewNormalWeekHeight * CalendarViewsSecondaryActionBar.MONTHDAY_HEADER_HEIGHT_RATIO);     	
    	DATEINDICATOR_HEADER_HEIGHT = (int) (mAnticipationMonthDayListViewNormalWeekHeight * CalendarViewsSecondaryActionBar.DATEINDICATOR_HEADER_HEIGHT_RATIO);
    	mDayViewSecondaryActionBarHeight = DAY_HEADER_HEIGHT + MONTHDAY_HEADER_HEIGHT + DATEINDICATOR_HEADER_HEIGHT;  
    	
    	mDayViewHeight = mAnticipationFragmentFrameLayoutHeight - mDayViewSecondaryActionBarHeight - mLowerActionBarHeight;
    	
    	setAlldayEventListViewDimension();
	}	
	
	private float mAnimationDistance = 0;
	private ScrollInterpolator mHScrollInterpolator;
	
	public void initNextView(int deltaX) {
		boolean switchForward;
        if (deltaX > 0) {            
            switchForward = false;
        } else {            
            switchForward = true;
        }
        
        if (switchForward) {
        	// current view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�        	
        	// next view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�     
        	DayView entranceView = getNextDayView();
        	initView(entranceView);
        }
        else {        	
        	// current view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�            	
        	// previous view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�
        	DayView entranceView = getPreviousDayView();
        	initView(entranceView);
        }
	}
	
	public void initView(DayView view) {
		//if (INFO) Log.i(TAG, "initView");
		
		DayView curView = getCurrentDayView();
		// �ܼ��� TOUCH_MODE_HSCROLL�� ���� View �����̹Ƿ� selected hour�� ��ũ ���� �ʿ䰡 ����!!!
        //view.setSelectedHour(curView.getSelectedHour());
        //view.setSelectedMinutes(curView.getSelectedMinutes());                
        //view.mSelectedEvents.clear();
        //view.mComputeSelectedEvents = true;
        
        view.setFirstVisibleHour(curView.getFirstVisibleHour());
        view.setFirstVisibleHourOffset(curView.getFirstVisibleHourOffset());       
        view.mViewStartY = curView.mViewStartY;
        view.remeasure(curView.getWidth(), curView.getHeight());
        view.invalidate();       
    }
	
	public void switchViews(boolean forward, boolean switchViewsBySelectedEvent, float xOffSet, float width) {
    	    	
    	if (switchViewsBySelectedEvent) {
    		mSwitchingDayViewBySelectedEventRect = true;
    	}
    	else {
    		if (getSelectionMode() == DayView.SELECTION_EVENT_FLOATING) {
    			getSelectedEventRect().setVisibility(View.GONE);	
    			resetSelectedEventRequireUpdateFlags();    			
    			setSelectionMode(DayView.SELECTION_HIDDEN, DayView.SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
    		}
    	}
    	
        mAnimationDistance = width - Math.abs(xOffSet);        
                
        ExtendedDayView curView =  getCurrentExtendedDayView();
        ExtendedDayView nextView =  getNextExtendedDayView();
        ExtendedDayView previousView =  getPreviousExtendedDayView();        
        
        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;
        int swapCase;
        Calendar start;
        if (forward) {
        	reloadNewDayViewEventsInfoBeforeSwapEvents(NEXT_DAY_EVENTS);
        	
        	// current view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�
        	outFromXValue = curView.getX();
            outToXValue = -mDayViewWidth;
        	// next view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�        	
            inFromXValue = nextView.getX();
            inToXValue = 0.0f;
            
            swapCase = SWAP_EVENTS_FORWARD_MOVE;
            
            // next view�� current view�� �ȴ�
            start = GregorianCalendar.getInstance(getNextExtendedDayView().mDayView.mBaseDate.getTimeZone());//new Time(getNextExtendedDayView().mDayView.mBaseDate);
            start.setFirstDayOfWeek(getNextExtendedDayView().mDayView.mBaseDate.getFirstDayOfWeek());
            long nextDayMills = getNextExtendedDayView().mDayView.mBaseDate.getTimeInMillis();
            start.setTimeInMillis(nextDayMills);
        } else {
        	reloadNewDayViewEventsInfoBeforeSwapEvents(PREVIOUS_DAY_EVENTS);
        	// current view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�   
        	outFromXValue = curView.getX();
            outToXValue = mDayViewWidth;
        	// previous view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�        	
            inFromXValue = previousView.getX();
            inToXValue = 0.0f;
            
            swapCase = SWAP_EVENTS_BACKWARD_MOVE;
            
            // previous view�� current view�� �ȴ�                        
            start = GregorianCalendar.getInstance(getPreviousExtendedDayView().mDayView.mBaseDate.getTimeZone());//new Time(getNextExtendedDayView().mDayView.mBaseDate);
            start.setFirstDayOfWeek(getPreviousExtendedDayView().mDayView.mBaseDate.getFirstDayOfWeek());
            start.setTimeInMillis(getPreviousExtendedDayView().mDayView.mBaseDate.getTimeInMillis());
        }
        
        mController.setTime(start.getTimeInMillis());
        
        if (mNumDays == 7) {            
            ETime.adjustStartDayInWeek(start);
        }

        
        final Calendar end = GregorianCalendar.getInstance(start.getTimeZone());
        end.setFirstDayOfWeek(start.getFirstDayOfWeek());
        end.setTimeInMillis(start.getTimeInMillis());
        end.add(Calendar.DAY_OF_MONTH, mNumDays - 1);//end.monthDay += mNumDays - 1;        
        
        TranslateAnimation outAnimation = new TranslateAnimation(
        		Animation.ABSOLUTE, outFromXValue,
        		Animation.ABSOLUTE, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        TranslateAnimation inAnimation = new TranslateAnimation(
        		Animation.ABSOLUTE, inFromXValue,
        		Animation.ABSOLUTE, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);      

        long duration = calculateDuration(mAnimationDistance, width, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
        
        inAnimation.setDuration(duration);
        inAnimation.setInterpolator(mHScrollInterpolator);        
        
        outAnimation.setDuration(duration);
        outAnimation.setInterpolator(mHScrollInterpolator);           
        
        if (!switchViewsBySelectedEvent)
        	outAnimation.setAnimationListener(new SwitchViewBroadcaster(start, end, swapCase));////////////////////////////////////////////////////////////////////////
        else {
        	outAnimation.setAnimationListener(new SwitchViewBroadcasterBySelectedEvent(start, end, swapCase));////////////////////////////////////////////////////////////////////////
        }        
        
        DayView prvCurrentView = getCurrentDayView();	
        prvCurrentView.cleanup(); 
        
        if (switchViewsBySelectedEvent) {        	
            
        	if (forward) {     
            	DayView nextViewwillBeCurrentView = getNextDayView();
            	nextViewwillBeCurrentView.setSelectedPrvCurrentView(
            			prvCurrentView.getSelectedHour(), 
                		prvCurrentView.getSelectedMinutes(),
            			prvCurrentView.getViewStartY(), 
            			prvCurrentView.mAutoVerticalScrollingByNewEventRect, prvCurrentView.mAutoVerticalScrollingType,
            			prvCurrentView.mScrolling, prvCurrentView.mScrollStartY,
            			prvCurrentView.mMinutuePosPixelAtSelectedEventMove);       		  		  
        		
            }
            else {
            	DayView previousWillBeCurrentView = getPreviousDayView();
            	previousWillBeCurrentView.setSelectedPrvCurrentView(
            			prvCurrentView.getSelectedHour(), 
                		prvCurrentView.getSelectedMinutes(),
            			prvCurrentView.getViewStartY(), 
            			prvCurrentView.mAutoVerticalScrollingByNewEventRect, prvCurrentView.mAutoVerticalScrollingType,
            			prvCurrentView.mScrolling, prvCurrentView.mScrollStartY,
            			prvCurrentView.mMinutuePosPixelAtSelectedEventMove);             	   
            	
            }             
        }
        
        secondaryActionBarGoToDate(DayFragment.UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP, start, duration); // �Ʒ��� view switcher�� animation duration�� ���߰� �ִ� 
        
        // GotoBroadcaster�� onAnimationStart���� �Ʒ� �Լ��� ȣ���ϸ�,
        // �� �� ���� ��?�� �߻���        
        mOnStartMonthDayHeaderAnimationListener.startMonthDayHeaderAnimation();
                               
        // initNextView���� �������� �ʾҴ°�???......................................................................................................................
    	//newCurrentView.setSelected(newSelected, true, false);           
        curView.setX(0);  
        
    	if (forward) {   
    		// ���� �Ʒ� �� �Լ�[setFirstVisibleHour, setFirstVisibleHourOffset]�� ȣ���� �ʿ䰡 ������?
    		// :�̹� initView���� ȣ���Ͽ���.
    		//  switchViews�� ȣ��Ǳ� ���� horizontal scrolling�̳� �߻��ɰ��ε�...
    		//  �̶� initView�� ȣ��Ǳ� �����̴�
    		//nextView.mDayView.setFirstVisibleHour(curView.mDayView.getFirstVisibleHour());
            //nextView.mDayView.setFirstVisibleHourOffset(curView.mDayView.getFirstVisibleHourOffset());
            nextView.mDayView.mRemeasure = true;
            nextView.mDayView.invalidate();
            // ���� nextView.invalidate�� ���� �ʾƵ� �Ǵ� ������ �����ΰ�?
            
    		// current view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�        		
        	// next view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�
    		
    		// Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�    		  
    		nextView.setX(0);
    		previousView.setX(-mDayViewWidth);
    		
    		nextView.mDayView.updateTitle();
        	 		
        	curView.startAnimation(outAnimation);
        	nextView.startAnimation(inAnimation);        		     	
        }
        else {            	
        	//previousView.mDayView.setFirstVisibleHour(curView.mDayView.getFirstVisibleHour());
        	//previousView.mDayView.setFirstVisibleHourOffset(curView.mDayView.getFirstVisibleHourOffset());    
        	previousView.mDayView.mRemeasure = true;
        	previousView.mDayView.invalidate();
        	// current view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�            	
        	// previous view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�
        	
        	// Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�        	    
        	previousView.setX(0);
        	nextView.setX(mDayViewWidth);
        	
        	previousView.mDayView.updateTitle();
        	
        	curView.startAnimation(outAnimation);
        	previousView.startAnimation(inAnimation);            		
        }        
    }
		
	/*
	public void switchViews(Time baseDate, boolean forward, boolean switchViewsBySelectedEvent, float xOffSet, float width) {
    	if (INFO) Log.i(TAG, "switchViews"); 
    	
    	if (switchViewsBySelectedEvent) {
    		mSwitchingDayViewBySelectedEventRect = true;
    	}
    	else {
    		if (getSelectionMode() == DayView.SELECTION_EVENT_FLOATING) {
    			getSelectedEventRect().setVisibility(View.GONE);	
    			resetSelectedEventRequireUpdateFlags();    			
    			setSelectionMode(DayView.SELECTION_HIDDEN, DayView.SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
    		}
    	}
    	
        mAnimationDistance = width - Math.abs(xOffSet);        
                
        ExtendedDayView curView =  getCurrentExtendedDayView();
        ExtendedDayView nextView =  getNextExtendedDayView();
        ExtendedDayView previousView =  getPreviousExtendedDayView();        
        
        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;
        int swapCase;
        Time start;
        if (forward) {
        	reloadNewDayViewEventsBeforeSwapEvents(NEXT_DAY_EVENTS);
        	
        	// current view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�
        	outFromXValue = curView.getX();
            outToXValue = -mDayViewWidth;
        	// next view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�        	
            inFromXValue = nextView.getX();
            inToXValue = 0.0f;
            
            swapCase = SWAP_EVENTS_FORWARD_MOVE;
            
            // next view�� current view�� �ȴ�
            start = new Time(getNextExtendedDayView().mDayView.mBaseDate);
            
        } else {
        	reloadNewDayViewEventsBeforeSwapEvents(PREVIOUS_DAY_EVENTS);
        	// current view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�   
        	outFromXValue = curView.getX();
            outToXValue = mDayViewWidth;
        	// previous view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�        	
            inFromXValue = previousView.getX();
            inToXValue = 0.0f;
            
            swapCase = SWAP_EVENTS_BACKWARD_MOVE;
            
            // previous view�� current view�� �ȴ�
            start = new Time(getPreviousExtendedDayView().mDayView.mBaseDate);
        }
        
        mController.setTime(start.normalize(true));
        
        if (mNumDays == 7) {
            //newSelected = new Time(start);
            adjustToBeginningOfWeek(start);
        }

        final Time end = new Time(start);
        end.normalize(true);
        end.monthDay += mNumDays - 1;
        end.normalize(true);
        
        TranslateAnimation outAnimation = new TranslateAnimation(
        		Animation.ABSOLUTE, outFromXValue,
        		Animation.ABSOLUTE, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        TranslateAnimation inAnimation = new TranslateAnimation(
        		Animation.ABSOLUTE, inFromXValue,
        		Animation.ABSOLUTE, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);      

        long duration = calculateDuration(mAnimationDistance, width, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
        
        inAnimation.setDuration(duration);
        inAnimation.setInterpolator(mHScrollInterpolator);        
        
        outAnimation.setDuration(duration);
        outAnimation.setInterpolator(mHScrollInterpolator);           
        
        if (!switchViewsBySelectedEvent)
        	outAnimation.setAnimationListener(new GotoBroadcaster(start, end, swapCase));////////////////////////////////////////////////////////////////////////
        else {
        	outAnimation.setAnimationListener(new GotoBroadcasterBySelectedEvent(start, end, swapCase));////////////////////////////////////////////////////////////////////////
        }        
        
        if (!switchViewsBySelectedEvent) {
        	DayView prvCurrentView = getCurrentDayView();	
            prvCurrentView.cleanup();     
            
            secondaryActionBarGoToDate(DayFragment.UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP, start, duration); // �Ʒ��� view switcher�� animation duration�� ���߰� �ִ� 
                       
            // GotoBroadcaster�� onAnimationStart���� �Ʒ� �Լ��� ȣ���ϸ�,
            // �� �� ���� ��?�� �߻���        
            mOnStartMonthDayHeaderAnimationListener.startMonthDayHeaderAnimation();
                                   
            // initNextView���� �������� �ʾҴ°�???......................................................................................................................
        	//newCurrentView.setSelected(newSelected, true, false);           
    		
        	if (forward) {           		
        		// current view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�        		
            	// next view�� �������� �̵��ϰ� �־��� ������ �������� �����ؾ� �Ѵ�
        		
        		// Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�
        		curView.setX(0);    
        		nextView.setX(0);
        		previousView.setX(-mDayViewWidth);
        		
        		nextView.mDayView.updateTitle();
            	 		
            	curView.startAnimation(outAnimation);
            	nextView.startAnimation(inAnimation);        		     	
            }
            else {            	
            	// current view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�            	
            	// previous view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�
            	
            	// Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�
            	curView.setX(0);              
            	previousView.setX(0);
            	nextView.setX(mDayViewWidth);
            	
            	previousView.mDayView.updateTitle();
            	
            	curView.startAnimation(outAnimation);
            	previousView.startAnimation(inAnimation);            		
            }
        	
        }
        else {
        	
        	DayView prvCurrentView = getCurrentDayView();	
            prvCurrentView.cleanup();          
            
            secondaryActionBarGoToDate(UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP, start, duration); // �Ʒ��� view switcher�� animation duration�� ���߰� �ִ�        	
            
            // GotoBroadcaster�� onAnimationStart���� �Ʒ� �Լ��� ȣ���ϸ�,
            // �� �� ���� ��?�� �߻���        
            mOnStartMonthDayHeaderAnimationListener.startMonthDayHeaderAnimation();   
            
            if (forward) {     
            	DayView nextViewwillBeCurrentView = getNextDayView();
            	nextViewwillBeCurrentView.setSelectedPrvCurrentView(prvCurrentView.getSelectedHour(), 
                		prvCurrentView.getSelectedMinutes(),
            			prvCurrentView.getViewStartY(), 
            			prvCurrentView.mAutoVerticalScrollingByNewEventRect, prvCurrentView.mAutoVerticalScrollingType,
            			prvCurrentView.mScrolling, prvCurrentView.mScrollStartY,
            			prvCurrentView.mMinutuePosPixelAtSelectedEventMove); 
            	
            	nextView.mDayView.setFirstVisibleHour(curView.mDayView.getFirstVisibleHour());
                nextView.mDayView.setFirstVisibleHourOffset(curView.mDayView.getFirstVisibleHourOffset());                
        		
        		curView.setX(0);
        		nextView.setX(0);        		
            	
        		nextView.mDayView.updateTitle();            	
            	
            	curView.startAnimation(outAnimation);
            	nextView.startAnimation(inAnimation);        		  
        		
            }
            else {
            	DayView previousWillBeCurrentView = getPreviousDayView();
            	previousWillBeCurrentView.setSelectedPrvCurrentView(prvCurrentView.getSelectedHour(), 
                		prvCurrentView.getSelectedMinutes(),
            			prvCurrentView.getViewStartY(), 
            			prvCurrentView.mAutoVerticalScrollingByNewEventRect, prvCurrentView.mAutoVerticalScrollingType,
            			prvCurrentView.mScrolling, prvCurrentView.mScrollStartY,
            			prvCurrentView.mMinutuePosPixelAtSelectedEventMove); 
            	
            	curView.setX(0);
            	previousView.setX(0);
            	// current view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�            	
            	// previous view�� ���������� �̵��ϰ� �־��� ������ ���������� �����ؾ� �Ѵ�
            	          	
            	previousView.mDayView.updateTitle();
            	
            	//previousView.setAnimation(inAnimation);
            	curView.startAnimation(outAnimation);
            	previousView.startAnimation(inAnimation);            	
            }             
        }        
    }
    */
	
	public void makeInPlaceAnimation() {		
		// ���� current view�� x ���� �ܼ��̴�
		ExtendedDayView curView =  getCurrentExtendedDayView();
		float curViewX = curView.getX();
        
        ExtendedDayView viewWithCurView = null;
        ExtendedDayView noneViewWithCurView = null;
        
        // �� �ڸ��� ���ƿ��� ��� �̹Ƿ� current view�� ���� x�� ���밪�� 
        // �� �ڸ��� ���ư� �Ÿ��̴�
        mAnimationDistance = Math.abs(curViewX);    
        
        float curViewFromXValue = curViewX;
        float curViewToXValue = 0;
        
        float neighborViewFromXValue = 0;
        float neighborViewToXValue = 0;
        
        InPlaceAnimationForwardListener curInPlaceAnimationForwardListener = new InPlaceAnimationForwardListener(CURRENT_DAY_EVENTS);
        //curViewInPlaceAnimation.setAnimationListener(curInPlaceAnimationForwardListener);
        
        InPlaceAnimationForwardListener neighborInPlaceAnimationForwardListener = null;
        //curViewInPlaceAnimation.setAnimationListener(curInPlaceAnimationForwardListener);
        
        // curViewX�� 0���� �̵��ؾ� �Ѵ�
        if (curViewX > 0) {
        	// previousViewX�� �Ϻκ��� ���� ���δٴ� �ֱ���
        	// :previousViewX�� -800���� �̵��ؾ� �Ѵ�
        	viewWithCurView = getPreviousExtendedDayView();     
        	neighborViewFromXValue = viewWithCurView.getX();
        	neighborViewToXValue = -mDayViewWidth;
        	neighborInPlaceAnimationForwardListener = new InPlaceAnimationForwardListener(PREVIOUS_DAY_EVENTS);
        	
        	
        	noneViewWithCurView = getNextExtendedDayView();
        	noneViewWithCurView.setX(mDayViewWidth);
        	
        	DayView nextView = getNextDayView();
            nextView.mViewStartX = 0;    
            
        }
        else if (curViewX < 0) {        	
        	// nextViewX�� �Ϻκ��� ���� ���δٴ� �ֱ���
        	// :nextViewX�� 800���� �̵��ؾ� �Ѵ�
        	viewWithCurView = getNextExtendedDayView();
        	neighborViewFromXValue = viewWithCurView.getX();
        	neighborViewToXValue = mDayViewWidth;
        	neighborInPlaceAnimationForwardListener = new InPlaceAnimationForwardListener(NEXT_DAY_EVENTS);
        	
        	noneViewWithCurView = getPreviousExtendedDayView();
        	noneViewWithCurView.setX(-mDayViewWidth);
        	
            DayView prvView = getPreviousDayView();
            prvView.mViewStartX = 0;
        }
        
        TranslateAnimation curViewInPlaceAnimation = new TranslateAnimation(
        		Animation.ABSOLUTE, curViewFromXValue,
        		Animation.ABSOLUTE, curViewToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        TranslateAnimation neighborViewInPlaceAnimation = new TranslateAnimation(
        		Animation.ABSOLUTE, neighborViewFromXValue,
        		Animation.ABSOLUTE, neighborViewToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        long duration = calculateDuration(mDayViewWidth, mDayViewWidth, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
        duration = 2000; // for test
        
        curViewInPlaceAnimation.setDuration(duration);  
        curViewInPlaceAnimation.setInterpolator(mHScrollInterpolator);
        curViewInPlaceAnimation.setAnimationListener(curInPlaceAnimationForwardListener);
        // setFillEnabled�� ������� ���� ���� �� �� ��´� ����....
        //curViewInPlaceAnimation.setFillEnabled(false); 
        //curViewInPlaceAnimation.setFillAfter(true);        
        
        neighborViewInPlaceAnimation.setDuration(duration);
        neighborViewInPlaceAnimation.setInterpolator(mHScrollInterpolator);
        neighborViewInPlaceAnimation.setAnimationListener(neighborInPlaceAnimationForwardListener);
        //neighborViewInPlaceAnimation.setFillEnabled(false); 
        //neighborViewInPlaceAnimation.setFillAfter(true);        
        		
		// setX�� �Ķ���͸� 0���� �����ϴ� ������ 
		// ���� Animation.ABSOLUTE, fromXDelta ����
		// ���� view�� left ���� 0�� ���� �������� offset?���� ����? �Ǿ��� �����̴�
		// ���� mCurrentLeftX ��[���� view�� left ��]�� �������� �Ѵٸ�
		// fromXDelta�� 0�� �� ���̰�,
		// toXDelta�� View width - mCurrentLeftX ���� �ɰ��̴�
        curView.setX(0);
        viewWithCurView.setX(0);
        
        curView.startAnimation(curViewInPlaceAnimation);
        viewWithCurView.startAnimation(neighborViewInPlaceAnimation);     
        
		return ;			
	}
	
	
	
	
	private class InPlaceAnimationForwardListener implements Animation.AnimationListener {
        
		int mWhichSideView;
        public InPlaceAnimationForwardListener(int whichSideView) {
        	mWhichSideView = whichSideView;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        	//if(INFO) Log.i(TAG, "InPlaceAnimationForwardListener:onAnimationEnd");	
        	       	
            
            switch(mWhichSideView) {
            case DayFragment.CURRENT_DAY_EVENTS:
            	getCurrentExtendedDayView().setX(0); 
            	
            	DayView curView = getCurrentDayView();
                curView.mViewStartX = 0;
                
            	break;
            case DayFragment.PREVIOUS_DAY_EVENTS:
            	getPreviousExtendedDayView().setX(-DayFragment.this.mDayViewWidth); 
            	
            	DayView prvView = getPreviousDayView();
                prvView.mViewStartX = 0;
                
            	break;
            case DayFragment.NEXT_DAY_EVENTS:
            	getNextExtendedDayView().setX(DayFragment.this.mDayViewWidth); 
            	
            	DayView nextView = getNextDayView();
                nextView.mViewStartX = 0;    
                
            	break;
            }             
            
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {        	
        }
    }
	
	
	private static final int MINIMUM_SNAP_VELOCITY = 2200;

    private class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
    }
    
    private void cancelAnimation() {
    	/*
        Animation in = mViewSwitcher.getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = mViewSwitcher.getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
        */
    }
    
	private long calculateDuration(float delta, float width, float velocity) {
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
        velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }

    /*
     * We want the duration of the page snap animation to be influenced by the
     * distance that the screen has to travel, however, we don't want this
     * duration to be effected in a purely linear fashion. Instead, we use this
     * method to moderate the effect that the distance of travel has on the
     * overall snap duration.
     */
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
    
    
    
    private class GoToBroadcaster implements Animation.AnimationListener {        
        boolean forward = true;
        int firstHour;
        int firstHourOffset;
        
        public GoToBroadcaster(boolean forward, int firstHour, int firstHourOffset) {           
            this.forward = forward;
            this.firstHour = firstHour;
            this.firstHourOffset = firstHourOffset;
        }

        
        @Override
        public void onAnimationEnd(Animation animation) {
        	//if(INFO) Log.i(TAG, "GotoBroadcaster:onAnimationEnd");	
        	DayView curView = getCurrentDayView();
            curView.mViewStartX = 0;
            DayView nextView = getNextDayView();
            nextView.mViewStartX = 0;                
            DayView prvView = getPreviousDayView();
            prvView.mViewStartX = 0;
            
            ExtendedDayView curExtendedDayView = mExtendedDayViewsMap.get(CURRENT_DAY_EVENTS);
    		ExtendedDayView prvExtendedDayView = mExtendedDayViewsMap.get(PREVIOUS_DAY_EVENTS);
    		ExtendedDayView nextExtendedDayView = mExtendedDayViewsMap.get(NEXT_DAY_EVENTS);       
            
    		//long gmtoff = mTimeZone.getRawOffset() / 1000;
            int curJulianDay = ETime.getJulianDay(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
            ManageEventsPerWeek curWeekObj = mManageEventsPerWeekHashMap.get(CURRENT_WEEK_EVENTS);
            
        	if (forward) { 
        		// *���� cur view�� ���� �� prv view�� �ȴ�
        		curExtendedDayView.setX(-DayFragment.this.mDayViewWidth);
        		nextExtendedDayView.setX(0);
        		
        		/*
                Time newPrvDay = new Time(mCurrentDayViewBaseTime);
            	newPrvDay.normalize(true);
            	newPrvDay.monthDay = newPrvDay.monthDay - 1;
            	newPrvDay.normalize(true);        
            	*/
        		
            	Calendar newPrvDay = GregorianCalendar.getInstance(mTimeZone);
            	ETime.copyCalendar(mCurrentDayViewBaseTime, newPrvDay);
            	
                curView.setBaseTime(newPrvDay);
                curView.setFirstVisibleHour(this.firstHour);
                curView.setFirstVisibleHourOffset(this.firstHourOffset);
                
                mCircularThreeDaysEventsInfoMap.remove(PREVIOUS_DAY_EVENTS);
        		
                CircularDayEventsInfo previousDayEvents = new CircularDayEventsInfo();
            	previousDayEvents.mWhichDay = PREVIOUS_DAY_EVENTS;    	
            	int prvJulianDay = curJulianDay - 1;
            	previousDayEvents.mFirstJulianDay = prvJulianDay;              	
            	
            	mCircularThreeDaysEventsInfoMap.put(PREVIOUS_DAY_EVENTS, previousDayEvents);            	
        		
            	if (curWeekObj.mWeekStartJulianDay == curJulianDay) {
            		ManageEventsPerWeek prv1WeekObj = mManageEventsPerWeekHashMap.get(PREVIOUS_1_WEEK_EVENTS);
            		
            		int prvDayEventsArrayIndex = prvJulianDay - prv1WeekObj.mWeekStartJulianDay;
            		
            		previousDayEvents.mEvents = prv1WeekObj.mEventDayList.get(prvDayEventsArrayIndex);
            		previousDayEvents.mAllDayEvents = getAllDayEvents(previousDayEvents.mEvents);
            		
            			
            		curView.setEventsX(prv1WeekObj.mEventDayList.get(prvDayEventsArrayIndex));
                	setAllDayEventListView(previousDayEvents.mAllDayEvents, PREVIOUS_DAY_EVENTS);      	
            	}
            	else {
            		int prvDayEventsArrayIndex = prvJulianDay - curWeekObj.mWeekStartJulianDay;
            		
            		previousDayEvents.mEvents = curWeekObj.mEventDayList.get(prvDayEventsArrayIndex);
            		previousDayEvents.mAllDayEvents = getAllDayEvents(previousDayEvents.mEvents);
            		
            		curView.setEventsX(curWeekObj.mEventDayList.get(prvDayEventsArrayIndex));
                	setAllDayEventListView(previousDayEvents.mAllDayEvents, PREVIOUS_DAY_EVENTS);      
            	}   	
            	
            	curView.handleOnResume();
            	curView.restartCurrentTimeUpdates();            	 		
        		
            	mExtendedDayViewsMap.remove(CURRENT_DAY_EVENTS);
            	mExtendedDayViewsMap.remove(PREVIOUS_DAY_EVENTS);
            	mExtendedDayViewsMap.remove(NEXT_DAY_EVENTS);
            	
            	mExtendedDayViewsMap.put(CURRENT_DAY_EVENTS, nextExtendedDayView);
            	mExtendedDayViewsMap.put(PREVIOUS_DAY_EVENTS, curExtendedDayView);
            	mExtendedDayViewsMap.put(NEXT_DAY_EVENTS, prvExtendedDayView);          		
        	}
        	else {
        		// *���� cur view�� ���� �� next view�� �ȴ�
        		curExtendedDayView.setX(DayFragment.this.mDayViewWidth);
        		prvExtendedDayView.setX(0);
        		
        		/*
                Time newNextDay = new Time(mCurrentDayViewBaseTime);
                newNextDay.normalize(true);
                newNextDay.monthDay = newNextDay.monthDay + 1;
                newNextDay.normalize(true);            	
            	*/
        		
        		Calendar newNextDay = GregorianCalendar.getInstance(mTimeZone);
            	ETime.copyCalendar(mCurrentDayViewBaseTime, newNextDay);
            	
                curView.setBaseTime(newNextDay);
                curView.setFirstVisibleHour(this.firstHour);
                curView.setFirstVisibleHourOffset(this.firstHourOffset);
                
                mCircularThreeDaysEventsInfoMap.remove(NEXT_DAY_EVENTS);
        		
                CircularDayEventsInfo nextDayEvents = new CircularDayEventsInfo();
                nextDayEvents.mWhichDay = NEXT_DAY_EVENTS;    	
            	int nextJulianDay = curJulianDay + 1;
            	nextDayEvents.mFirstJulianDay = nextJulianDay;              	
            	
            	mCircularThreeDaysEventsInfoMap.put(NEXT_DAY_EVENTS, nextDayEvents);            	
        		
            	if (nextJulianDay > curWeekObj.mWeekEndJulianDay) {
            		ManageEventsPerWeek next1WeekObj = mManageEventsPerWeekHashMap.get(NEXT_1_WEEK_EVENTS);
            		
            		int nextDayEventsArrayIndex = nextJulianDay - next1WeekObj.mWeekStartJulianDay;
            		
            		nextDayEvents.mEvents = next1WeekObj.mEventDayList.get(nextDayEventsArrayIndex);
            		nextDayEvents.mAllDayEvents = getAllDayEvents(nextDayEvents.mEvents);
            		    			
            		curView.setEventsX(next1WeekObj.mEventDayList.get(nextDayEventsArrayIndex));
                	setAllDayEventListView(nextDayEvents.mAllDayEvents, NEXT_DAY_EVENTS);
            	}
            	else {
            		int nextDayEventsArrayIndex = nextJulianDay - curWeekObj.mWeekStartJulianDay;
            		
            		nextDayEvents.mEvents = curWeekObj.mEventDayList.get(nextDayEventsArrayIndex);
            		nextDayEvents.mAllDayEvents = getAllDayEvents(nextDayEvents.mEvents);
            		
            		curView.setEventsX(curWeekObj.mEventDayList.get(nextDayEventsArrayIndex));
            		setAllDayEventListView(nextDayEvents.mAllDayEvents, NEXT_DAY_EVENTS);
            	}
            	
            	curView.handleOnResume();
            	curView.restartCurrentTimeUpdates();            		
        		
            	mExtendedDayViewsMap.remove(CURRENT_DAY_EVENTS);
            	mExtendedDayViewsMap.remove(PREVIOUS_DAY_EVENTS);
            	mExtendedDayViewsMap.remove(NEXT_DAY_EVENTS);
            	
            	mExtendedDayViewsMap.put(CURRENT_DAY_EVENTS, prvExtendedDayView);
            	mExtendedDayViewsMap.put(PREVIOUS_DAY_EVENTS, nextExtendedDayView);
            	mExtendedDayViewsMap.put(NEXT_DAY_EVENTS, curExtendedDayView);  
        	}
        	
        	
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {        	
        }
    }
    
    private static int sCounter = 0;
    
    
    private class SwitchViewBroadcaster implements Animation.AnimationListener {
        private int mCounter;
        private Calendar mStart;
        private Calendar mEnd;
        private final int mSwapCase;
        
        public SwitchViewBroadcaster() {           
            mSwapCase = NON_SWAP_EVENTS;
        }

        public SwitchViewBroadcaster(Calendar start, Calendar end, int swapCase) {
            mCounter = ++sCounter;
            mStart = start;
            mEnd = end;
            mSwapCase = swapCase;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        	//if(INFO) Log.i(TAG, "GotoBroadcaster:onAnimationEnd");	
        	
        	float prvX = getPreviousExtendedDayView().getX();  
            float nextX = getNextExtendedDayView().getX();
            
            DayView curView = getCurrentDayView();
            curView.mViewStartX = 0;
            
            DayView nextView = getNextDayView();
            nextView.mViewStartX = 0;
            
            DayView prvView = getPreviousDayView();
            prvView.mViewStartX = 0;
            
            if (mSwapCase != NON_SWAP_EVENTS) {
	            if (mSwapCase == DayFragment.SWAP_EVENTS_FORWARD_MOVE)
	            	swapEvents(DayFragment.SWAP_EVENTS_FORWARD_MOVE);    
	            else 
	            	swapEvents(DayFragment.SWAP_EVENTS_BACKWARD_MOVE);    
	            
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {        	
        }
    }
    
    
    private class SwitchViewBroadcasterBySelectedEvent implements Animation.AnimationListener {
        private final int mCounter;
        private final Calendar mStart;
        private final Calendar mEnd;
        private final int mSwapCase;

        public SwitchViewBroadcasterBySelectedEvent(Calendar start, Calendar end, int swapCase) {
            mCounter = ++sCounter;
            mStart = start;
            mEnd = end;
            mSwapCase = swapCase;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        	      
            
        	DayView curView = getCurrentDayView();
            curView.mViewStartX = 0;
            
            DayView nextView = getNextDayView();
            nextView.mViewStartX = 0;
            
            DayView prvView = getPreviousDayView();
            prvView.mViewStartX = 0;
            
            if (mSwapCase == DayFragment.SWAP_EVENTS_FORWARD_MOVE)
            	swapEvents(DayFragment.SWAP_EVENTS_FORWARD_MOVE);    
            else 
            	swapEvents(DayFragment.SWAP_EVENTS_BACKWARD_MOVE);    
            
            mSwitchingDayViewBySelectedEventRect = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        	
        }
    }
    
    Map<Integer, ManageEventsPerWeek> mManageEventsPerWeekHashMap = new HashMap<Integer, ManageEventsPerWeek>(9);
    Map<Integer, ManageEventsPerWeek> mManageEventsPerTodayWeekHashMap = new HashMap<Integer, ManageEventsPerWeek>(3);   
    
	public void loadEntireSeperateWeekEvents() {
		        
		//mManageEventsPerWeekHashMap.clear(); //���ʿ��ϴ�. setManageEventsPerWeekEvent���� ������ mapping�� �����Ѵٸ� ������ ��, ���� mapping �ϱ� �����̴�
		long gmtoff = mTimeZone.getRawOffset() / 1000;		
        //int targetJulainDay = ETime.getJulianDay(mCurrentDayViewBaseTime.getTimeInMillis(), gmtoff);
        int currentWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(targetJulainDay, mFirstDayOfWeek);    	   	
    	
    	ManageEventsPerWeek curretWeekEvents = new ManageEventsPerWeek(mTimeZone, CURRENT_WEEK_EVENTS, currentWeekPosition, mFirstDayOfWeek);     	
    	callDirectEventLoadAndCallback(curretWeekEvents);    	
    	
    	int prv1WeekPosition = currentWeekPosition - 1;
    	
    		ManageEventsPerWeek prv1WeekEvents = new ManageEventsPerWeek(mTimeZone, PREVIOUS_1_WEEK_EVENTS, prv1WeekPosition, mFirstDayOfWeek);    	
        	callDirectEventLoadAndCallback(prv1WeekEvents);
    	  	
    	
    	int next1WeekPosition = currentWeekPosition + 1;
    	  		
        	ManageEventsPerWeek next1WeekEvents = new ManageEventsPerWeek(mTimeZone, NEXT_1_WEEK_EVENTS, next1WeekPosition, mFirstDayOfWeek);    	
        	callDirectEventLoadAndCallback(next1WeekEvents);
    	 	
    	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	int prv2WeekPosition = currentWeekPosition - 2;
    	
    		ManageEventsPerWeek prv2WeekEvents = new ManageEventsPerWeek(mTimeZone, PREVIOUS_2_WEEK_EVENTS, prv2WeekPosition, mFirstDayOfWeek);   
    		loadEventsPerWeek(prv2WeekEvents);
    	 	 	
    	
    	int next2WeekPosition = currentWeekPosition + 2;
    	
    		ManageEventsPerWeek next2WeekEvents = new ManageEventsPerWeek(mTimeZone, NEXT_2_WEEK_EVENTS, next2WeekPosition, mFirstDayOfWeek);    	
    		loadEventsPerWeek(next2WeekEvents);
    	
    	
    	int prv3WeekPosition = currentWeekPosition - 3;
    	
    		ManageEventsPerWeek prv3WeekEvents = new ManageEventsPerWeek(mTimeZone, PREVIOUS_3_WEEK_EVENTS, prv3WeekPosition, mFirstDayOfWeek); 
    		loadEventsPerWeek(prv3WeekEvents);
    	  	   	
    	
    	int next3WeekPosition = currentWeekPosition + 3;
    	
    		ManageEventsPerWeek next3WeekEvents = new ManageEventsPerWeek(mTimeZone, NEXT_3_WEEK_EVENTS, next3WeekPosition, mFirstDayOfWeek);    
    		loadEventsPerWeek(next3WeekEvents);
    	  		
    	
    	int prv4WeekPosition = currentWeekPosition - 4;
    	
    		ManageEventsPerWeek prv4WeekEvents = new ManageEventsPerWeek(mTimeZone, PREVIOUS_4_WEEK_EVENTS, prv4WeekPosition, mFirstDayOfWeek); 
    		loadEventsPerWeek(prv4WeekEvents);
    	
    	
    	int next4WeekPosition = currentWeekPosition + 4;
    	
    		ManageEventsPerWeek next4WeekEvents = new ManageEventsPerWeek(mTimeZone, NEXT_4_WEEK_EVENTS, next4WeekPosition, mFirstDayOfWeek); 
    		loadEventsPerWeek(next4WeekEvents);  
    	
    	
    	// ���࿡ current day�� today�� ������ week position�̶��???
    	// �Ʒ� �ڵ�� ���ʿ��ϰ� �ι��� week event���� load�ϰ� �Ǵ� ���̴�...
    	// :���� �����???   	
    	//  ...
    }
    
  //private Handler mHandler = new Handler();
    public void callDirectEventLoadAndCallback(ManageEventsPerWeek obj) {
    	    	
    	AtomicInteger sequenceNumber = new AtomicInteger();
    	
    	Event.loadEvents(mContext, obj.mEvents, obj.mWeekStartJulianDay,
    			ManageEventsPerWeek.NUM_DAYS, 0, sequenceNumber);
    	
    	obj.mManageEventsPerWeekEventsLoadSuccessCallback.run();
    	
    	// post�� �ϸ� fragment ȣ�� ���� ���� ������ �Լ�[onResume]�� ȣ��� �� ����ȴ�...
    	//mHandler.post(obj.mManageEventsPerWeekEventsLoadSuccessCallback);
    }   
    
    
    //ManageEventsPerWeek mCurrentWeekEvents;
    
    Object mManageEventsPerWeekHashMapSyncObj = new Object();
    
    public void loadEventsPerWeek(ManageEventsPerWeek obj) {    	    	
    	
    	//final ArrayList<Event> xEvents = new ArrayList<Event>();
    	mEventLoaderPerWeek.loadEventsInBackground(ManageEventsPerWeek.NUM_DAYS, obj.mEvents, obj.mWeekStartJulianDay, 
    			obj.mManageEventsPerWeekEventsLoadSuccessCallback, obj.mManageEventsPerWeekEventsLoadCancelCallback);        
    	
    	// DayFragment���� ����ϴ� Uri�� Instances.CONTENT_BY_DAY_URI    	
    	// selection�� �� ���� ���� ����ϴµ� 
    	// *���� Calendars.VISIBLE�� "visible" �ؽ�Ʈ�� �����ϰ� �ִ� ��Ʈ���� 
    	// -(dispAllday=0) AND visible=?
    	// -(dispAllday=1) AND visible=?
    	// selectionArgs�� 1
    	// sort order�� dispAllday=0 selection�� �� "begin ASC, end DESC, title ASC"
    	// sort order�� dispAllday=1 selection�� �� "startDay ASC, endDay DESC, title ASC"
    	// *begin : The beginning time of the instance, in UTC milliseconds
    	
    	// MonthFragment���� ����ϴ� Uri�� Instances.CONTENT_URI 
    	// :MonthFragment�� Instances.CONTENT_URI�� ����Ѵٰ� �ؼ� path segments �� 
    	//  Ư�� �ð����� millis�� ����ϴ� ���� �ƴ϶�,
    	//  DayFragment�� �����ϰ� Ư������ julian day�� millis�� ����ϱ� ������
    	//  ��ǻ� DayFragment���� ����ϴ� Instances.CONTENT_BY_DAY_URI�� �����ϴ�    	
    	// selection�� ���� �ϳ� visible=1
    	// selectionArgs�� null
    	// sort order�� "startDay, startMinute, title"
    	// *startDay : The Julian start day of the instance, relative to the local time zone
    	
    	// �� �� fragment���� ������ �� ����ϴ� project�� Event.EVENT_PROJECTION�� �����ϴ�
    	// �׷��Ƿ� �����Ǵ� event���� �ν��Ͻ��� ��� ���� ����, ������ event���� ���� ����� �ٸ� ��...
    	// 
    	
    	/**
         * The content:// style URL for querying an instance range by Julian
         * Day. The start and end day should be added as path segments if this
         * is used directly.
         */
    	//Uri.Builder builder = Instances.CONTENT_BY_DAY_URI.buildUpon();       
        
    	/**
         * The content:// style URL for querying an instance range. The begin
         * and end of the range to query should be added as path segments if
         * this is used directly.
         */
        //Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
                   
        
        // ���� �ֺ��� �����͸� ��Ҵ�...
    	// 
    }
        
    
    public class ManageEventsPerWeek{
    	public static final int NUM_DAYS = 7;
    	
    	int mWhichSideWeek;
    	TimeZone mTimezone;
    	int mFirstDayOfWeek;
    	int mWeekPosition;
    	//Time mStartDayTime;
    	Calendar mStartDayTime;
    	int mWeekStartJulianDay;
    	int mWeekEndJulianDay;
    	
    	ArrayList<Event> mEvents = new ArrayList<Event>();
    	ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>(); 
    	
    	ManageEventsPerWeekEventsLoadSuccessCallback mManageEventsPerWeekEventsLoadSuccessCallback = new ManageEventsPerWeekEventsLoadSuccessCallback();
    	ManageEventsPerWeekEventsLoadCancelCallback mManageEventsPerWeekEventsLoadCancelCallback = new ManageEventsPerWeekEventsLoadCancelCallback();
    	
    	public ManageEventsPerWeek() {
    		
    	}
    	
    	public ManageEventsPerWeek(TimeZone timezone, int whichWeek, int weekPosition, int firstDayOfWeek) {
    		mTimezone = timezone;
    		mWhichSideWeek = whichWeek;
    		mWeekPosition = weekPosition;
    		mFirstDayOfWeek = firstDayOfWeek;
    		
    		makeStartJulianDay();
    	}
    	
    	public void makeStartJulianDay() {
    		int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(mWeekPosition);
        	
        	// �츮�� �ʿ��� start day�� weekPosition�� FirstDayOfWeek julian day�̴�
    		mStartDayTime = GregorianCalendar.getInstance(mTimezone);
    		mStartDayTime.setFirstDayOfWeek(mFirstDayOfWeek);
    		mStartDayTime.setTimeInMillis(ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek));
    		
    		ETime.adjustStartDayInWeek(mStartDayTime);
        	
        	long gmtoff = mTimezone.getRawOffset() / 1000;       	
        	mWeekStartJulianDay = ETime.getJulianDay(mStartDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	mWeekEndJulianDay = mWeekStartJulianDay + NUM_DAYS - 1;
    	}
    	
    	public void makeStartJulianDay(int weekPosition) {
    		mWeekPosition = weekPosition;
    		int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(mWeekPosition);
        	
        	// �츮�� �ʿ��� start day�� weekPosition�� FirstDayOfWeek julian day�̴�    		
    		mStartDayTime.setTimeInMillis(ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek));
    		
    		ETime.adjustStartDayInWeek(mStartDayTime);
        	
        	long gmtoff = mTimezone.getRawOffset() / 1000;       	
        	mWeekStartJulianDay = ETime.getJulianDay(mStartDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        	mWeekEndJulianDay = mWeekStartJulianDay + NUM_DAYS - 1;
    	}

    	public class ManageEventsPerWeekEventsLoadSuccessCallback implements Runnable {
			@Override
			public void run() {
				
				ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
				
	            for (int i = 0; i < NUM_DAYS; i++) {
	                eventDayList.add(new ArrayList<Event>());
	            }
	
	            if (mEvents == null || mEvents.size() == 0) {
	            	//if (INFO) Log.i(TAG, "ManageEventsPerWeekEventsLoadSuccessCallback:" + String.valueOf(mWhichSideWeek) + ", but no events");
	            	
	                //mEventDayList = eventDayList;                    
	                //return;
	            }
	            else {
	            	//if (INFO) Log.i(TAG, "ManageEventsPerWeekEventsLoadSuccessCallback:" + String.valueOf(mWhichSideWeek));
	            	
		            for (Event event : mEvents) {
		                int startDay = event.startDay - mWeekStartJulianDay;
		                int endDay = event.endDay - mWeekStartJulianDay + 1;
		                if (startDay < NUM_DAYS || endDay >= 0) {
		                    if (startDay < 0) {
		                        startDay = 0;
		                    }
		                    if (startDay > NUM_DAYS) {
		                        continue;
		                    }
		                    if (endDay < 0) {
		                        continue;
		                    }
		                    if (endDay > NUM_DAYS) {
		                        endDay = 7;
		                    }
		                    for (int j = startDay; j < endDay; j++) {
		                        eventDayList.get(j).add(event);
		                    }
		                }
		            }
	            }
	            
	            mEventDayList = eventDayList;	
	                        
	            if (mWhichSideWeek == TODAY_WEEK_EVENTS || mWhichSideWeek == PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK || mWhichSideWeek == NEXT_WEEK_EVENTS_OF_TODAY_WEEK) {
	            	setManageEventsPerTodayWeekEvent(mWhichSideWeek, ManageEventsPerWeek.this); 
	            }	            	           
	            else {
	            	setManageEventsPerWeekEvent(mWhichSideWeek, ManageEventsPerWeek.this); 
	            }
			}
    	}
    	
    	public class ManageEventsPerWeekEventsLoadCancelCallback implements Runnable {
			@Override
			public void run() {			
	            
	            //if (INFO) Log.i(TAG, "ManageEventsPerWeekEventsLoadCancelCallback:" + String.valueOf(mWhichSideWeek));
			}
    	}
    }
    
    
    public synchronized void setManageEventsPerWeekEvent(int whichSideWeek, ManageEventsPerWeek obj) { 
    	//if (INFO) Log.i(TAG, "setManageEventsPerWeekEvent:" + String.valueOf(whichSideWeek));
    	
    	ManageEventsPerWeek oldObj = mManageEventsPerWeekHashMap.get(whichSideWeek);
    	if (oldObj != null) {
    		mManageEventsPerWeekHashMap.remove(whichSideWeek);
    	}
    	
    	mManageEventsPerWeekHashMap.put(whichSideWeek, obj);
    	
    	for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		int key = entry.getKey();
    		ManageEventsPerWeek valueObj = entry.getValue();
    		//if (INFO) Log.i(TAG, "setManageEventsPerWeekEvent current status:" + String.valueOf(key));
    	}	
    			 
    }
    
    
    public synchronized void setManageEventsPerTodayWeekEvent(int whichSideWeek, ManageEventsPerWeek obj) { 
    	//if (INFO) Log.i(TAG, "setManageEventsPerTodayWeekEvent:" + String.valueOf(whichSideWeek));
    	ManageEventsPerWeek oldObj = mManageEventsPerTodayWeekHashMap.get(whichSideWeek);
    	if (oldObj != null) {
    		mManageEventsPerTodayWeekHashMap.remove(whichSideWeek);
    	}
    	
    	mManageEventsPerTodayWeekHashMap.put(whichSideWeek, obj);
    	
    	for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerTodayWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		int key = entry.getKey();
    		ManageEventsPerWeek valueObj = entry.getValue();
    		//if (INFO) Log.i(TAG, "setManageEventsPerTodayWeekEvent current status:" + String.valueOf(key));
    	}	
    			 
    }
    
    public static ArrayList<Event> getAllDayEvents(ArrayList<Event> curDayEvents) {
    	ArrayList<Event> allDayEvents = new ArrayList<Event>();
    	for (Event e : curDayEvents) {
            if (e.drawAsAllday()) {
            	allDayEvents.add(e);
            }
        }
    	
    	return allDayEvents;
    }
    
    public void setAllDayEventListView(ArrayList <Event> allDayEvents, int whichSideDay) {
    	if (allDayEvents.isEmpty()) {
			mExtendedDayViewsMap.get(whichSideDay).setAllDayEventListView(0, null);						
		}
		else {
			mExtendedDayViewsMap.get(whichSideDay).setAllDayEventListView(allDayEvents.size(), allDayEvents);	
		}
    }
    
    public ArrayList<Event> getEventsOfTargetDayInWeek(int targetWeekPosition, int targetDayJulianDay) {
    	ManageEventsPerWeek targetWeekObj = null;
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();    		
    		ManageEventsPerWeek valueObj = entry.getValue();
    		if (targetWeekPosition == valueObj.mWeekPosition) {
    			targetWeekObj = valueObj;
    			break;
    		}
    	}	
		
		int targetDayEventListIndex = targetDayJulianDay - targetWeekObj.mWeekStartJulianDay;
		ArrayList<Event> targetDayEvents = targetWeekObj.mEventDayList.get(targetDayEventListIndex);
		return targetDayEvents;
    }
    
        
    public ManageEventsPerWeek getManageEventsPerWeek(int targetWeekPosition) {
    	ManageEventsPerWeek targetWeekObj = null;
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();    		
    		ManageEventsPerWeek valueObj = entry.getValue();
    		if (targetWeekPosition == valueObj.mWeekPosition) {
    			targetWeekObj = valueObj;
    			break;
    		}
    	}
		
		return targetWeekObj;
    }
    
    public void setSwitchNextWeek() {
    	//if (INFO) Log.i(TAG, "setSwitchNextWeek");
    	
    	// next week�� �̵��ϴ� ���
		// current week�� next1Week�� �ǰ�
		// next4Week�� event���� ���� load �ؾ� �Ѵ�...
		// �׸��� prv4Week event���� clear �ؾ� �Ѵ�
		// :prv4Week�� ���� next4Week�� �Ǿ�� �Ѵ�
		/*
   		 PREVIOUS_4_WEEK_EVENTS -> PREVIOUS_3_WEEK_EVENTS
   		 PREVIOUS_3_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS	    		 
   	     PREVIOUS_2_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS
   	     PREVIOUS_1_WEEK_EVENTS -> CURRENT_WEEK_EVENTS	    	     
   	     CURRENT_WEEK_EVENTS -> NEXT_1_WEEK_EVENTS
   	     NEXT_1_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS
   	     NEXT_2_WEEK_EVENTS -> NEXT_3_WEEK_EVENTS
   	     NEXT_3_WEEK_EVENTS -> NEXT_4_WEEK_EVENTS
   	     NEXT_4_WEEK_EVENTS -> ������ PREVIOUS_4_WEEK_EVENTS ��ü���� ���ο� NEXT_4_WEEK_EVENTS ��ü ������ �����Ѵ�
   	    */        	    		
		
		int UpdateParaValue = -1;
		int willBeLoadNewWeek = NEXT_4_WEEK_EVENTS;
		int willBeLoadNewWeekPosition = 0;
		int thrownOutWeek = PREVIOUS_4_WEEK_EVENTS;
		ArrayList <ManageEventsPerWeek> objList = new ArrayList <ManageEventsPerWeek>();
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		int key = entry.getKey();
    		ManageEventsPerWeek valueObj = entry.getValue();
    		if (key == thrownOutWeek) {	        			
    			valueObj.mWhichSideWeek = -(valueObj.mWhichSideWeek);			        		
    		}	        		
    		else {   
    			if (key == willBeLoadNewWeek) {
    				willBeLoadNewWeekPosition = valueObj.mWeekPosition + 1;	        				
        		}
    			
    			valueObj.mWhichSideWeek = valueObj.mWhichSideWeek + UpdateParaValue;        			
    		}
    		
    		objList.add(valueObj);
    		       		
    	}	    		   		
		
		removeManageEventsPerWeekHashMap(); 
		
		int size = objList.size();
		for (int i=0; i<size; i++) {
			ManageEventsPerWeek Obj = objList.get(i);
			if (Obj.mWhichSideWeek == willBeLoadNewWeek) {
				Obj.makeStartJulianDay(willBeLoadNewWeekPosition);
				Obj.mEvents.clear();
				Obj.mEventDayList.clear();	
				
				loadEventsPerWeek(Obj);
    		}
			
			if (Obj.mWhichSideWeek == CURRENT_WEEK_EVENTS) {
				if (Obj != null) {
					//if (INFO) Log.i(TAG, "CURRENT_WEEK_EVENTS OK!!!");
				}
				else {
					if (INFO) Log.i(TAG, "CURRENT_WEEK_EVENTS NULL OOOOOOOOOOOOOOps!!!");
				}
			}
			
			
			mManageEventsPerWeekHashMap.put(Obj.mWhichSideWeek, Obj);	    			
		}
    	
    }
    
    public void setGoToRightSideWeek(int howManyWeekgoTo) {
    	/*
		PREVIOUS_4_WEEK_EVENTS 
   		PREVIOUS_3_WEEK_EVENTS  	    		 
   	    PREVIOUS_2_WEEK_EVENTS 
   	    PREVIOUS_1_WEEK_EVENTS 	    	     
   	    CURRENT_WEEK_EVENTS 
   	    NEXT_1_WEEK_EVENTS 
   	    NEXT_2_WEEK_EVENTS : ���� today�� ���� ��ġ�ϰ� �ִٸ�....
   	    NEXT_3_WEEK_EVENTS 
   	    NEXT_4_WEEK_EVENTS 
   	    
   	    PREVIOUS_4_WEEK_EVENTS -> ? : �� �̻� �ʿ� ���� [���ο� NEXT_4_WEEK_EVENTS�� ���� �������� ���ȴ�]
   		PREVIOUS_3_WEEK_EVENTS -> ? : �� �̻� �ʿ� ���� [���ο� NEXT_3_WEEK_EVENTS�� ���� �������� ���ȴ�]
   	    PREVIOUS_2_WEEK_EVENTS -> PREVIOUS_4_WEEK_EVENTS
   	    PREVIOUS_1_WEEK_EVENTS -> PREVIOUS_3_WEEK_EVENTS	    	     
   	    CURRENT_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS [0 -> 2]
   	    NEXT_1_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS [0 -> 2] 
   	    NEXT_2_WEEK_EVENTS -> CURRENT_WEEK_EVENTS [0 -> 2]
   	    NEXT_3_WEEK_EVENTS -> NEXT_1_WEEK_EVENTS [1 -> 3]
   	    NEXT_4_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS [2 -> 4]
   	                     ? -> NEXT_3_WEEK_EVENTS : ������ PREVIOUS_3_WEEK_EVENTS�� ���� load�ؾ� �Ѵ�
   	                     ? -> NEXT_4_WEEK_EVENTS : ������ PREVIOUS_4_WEEK_EVENTS������ load�ؾ� �Ѵ�
   	                     
   	    PREVIOUS_4_WEEK_EVENTS 
   		PREVIOUS_3_WEEK_EVENTS  	    		 
   	    PREVIOUS_2_WEEK_EVENTS 
   	    PREVIOUS_1_WEEK_EVENTS 	    	     
   	    CURRENT_WEEK_EVENTS 
   	    NEXT_1_WEEK_EVENTS 
   	    NEXT_2_WEEK_EVENTS
   	    NEXT_3_WEEK_EVENTS 
   	    NEXT_4_WEEK_EVENTS : ���� today�� ���� ��ġ�ϰ� �ִٸ�.... 
   	    
   	    PREVIOUS_4_WEEK_EVENTS -> ? : �� �̻� �ʿ� ���� [���ο� NEXT_4_WEEK_EVENTS�� ���� �������� ���ȴ�]
   		PREVIOUS_3_WEEK_EVENTS -> ? : �� �̻� �ʿ� ���� [���ο� NEXT_3_WEEK_EVENTS�� ���� �������� ���ȴ�]
   	    PREVIOUS_2_WEEK_EVENTS -> ? : �� �̻� �ʿ� ���� [���ο� NEXT_2_WEEK_EVENTS�� ���� �������� ���ȴ�]
   	    PREVIOUS_1_WEEK_EVENTS -> ? : �� �̻� �ʿ� ���� [���ο� NEXT_1_WEEK_EVENTS�� ���� �������� ���ȴ�]	    	     
   	    CURRENT_WEEK_EVENTS -> PREVIOUS_4_WEEK_EVENTS [-4 -> 0] 
   	    NEXT_1_WEEK_EVENTS -> PREVIOUS_3_WEEK_EVENTS [-3 -> 1] 
   	    NEXT_2_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS [-2 -> 2] 
   	    NEXT_3_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS [-1 -> 3] 
   	    NEXT_4_WEEK_EVENTS -> CURRENT_WEEK_EVENTS [0 -> 4]
   	                     ? -> NEXT_1_WEEK_EVENTS : ������ PREVIOUS_1_WEEK_EVENTS�� ���� load�ؾ� �Ѵ� -->�̹� ������ ���̴�
   	                     ? -> NEXT_2_WEEK_EVENTS : ������ PREVIOUS_2_WEEK_EVENTS������ load�ؾ� �Ѵ�
   	                     ? -> NEXT_3_WEEK_EVENTS : ������ PREVIOUS_3_WEEK_EVENTS�� ���� load�ؾ� �Ѵ�
   	                     ? -> NEXT_4_WEEK_EVENTS : ������ PREVIOUS_4_WEEK_EVENTS�� ���� load�ؾ� �Ѵ�
   	    */    	
    	int newCurrentWeekPosition = 0;    	
    	
		ArrayList <ManageEventsPerWeek> objList = new ArrayList <ManageEventsPerWeek>();
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		//int key = entry.getKey();
    		ManageEventsPerWeek valueObj = entry.getValue();
    		int newWhichSideWeekNumber = valueObj.mWhichSideWeek + (-howManyWeekgoTo);
    		valueObj.mWhichSideWeek = newWhichSideWeekNumber;
    		if (valueObj.mWhichSideWeek == CURRENT_WEEK_EVENTS) {
    			newCurrentWeekPosition = valueObj.mWeekPosition;
    		}
    		    		
    		objList.add(valueObj);    		       		
    	}	    		   		
		
		removeManageEventsPerWeekHashMap();
		
		int size = objList.size();
		for (int i=0; i<size; i++) {
			ManageEventsPerWeek Obj = objList.get(i);
			if (Obj.mWhichSideWeek < -4) {
				// �� for �������� �� �̻� previous week�� �ʿ� ���� �͵��� -4���� ū ������ ������ 
								
				// �� �̻� previous week�� �ʿ� ���� object���� ���� load �Ǿ�� �ϴ� new next week���� ���� object��� Ȱ��Ǿ�� �ϴµ�
				// ���� �ѹ��� ���� howManyWeekgoTo�� ���ϰ� ���ȭ�� �ϸ� 
				// new next week side ���� ��
				Obj.mWhichSideWeek = -(Obj.mWhichSideWeek + howManyWeekgoTo);
				int willBeLoadNewWeekPosition = newCurrentWeekPosition + Obj.mWhichSideWeek;
						
				Obj.makeStartJulianDay(willBeLoadNewWeekPosition);
				Obj.mEvents.clear();
				Obj.mEventDayList.clear();	
				
				loadEventsPerWeek(Obj);
    		}
			
			mManageEventsPerWeekHashMap.put(Obj.mWhichSideWeek, Obj);	    			
		}
    }
    
    public void setSwitchPreviousWeek() {
    	//if (INFO) Log.i(TAG, "setSwitchPreviousWeek");
    	// prv week�� �̵��ϴ� ���
		// current week�� prv1Week�� �ǰ�
		// prv4Week�� event���� ���� load �ؾ� �Ѵ�...
		// �׸��� next4Week event���� clear �ؾ� �Ѵ�
		// :next4Week�� ���� prv4Week�� �Ǿ�� �Ѵ�
		/*
   		 PREVIOUS_4_WEEK_EVENTS -> new NEXT_4_WEEK_EVENTS
   		 PREVIOUS_3_WEEK_EVENTS -> PREVIOUS_4_WEEK_EVENTS	    		 
   	     PREVIOUS_2_WEEK_EVENTS -> PREVIOUS_3_WEEK_EVENTS
   	     PREVIOUS_1_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS	    	     
   	     CURRENT_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS
   	     NEXT_1_WEEK_EVENTS -> CURRENT_WEEK_EVENTS
   	     NEXT_2_WEEK_EVENTS -> NEXT_1_WEEK_EVENTS
   	     NEXT_3_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS
   	     NEXT_4_WEEK_EVENTS -> NEXT_3_WEEK_EVENTS
   	    */
		int UpdateParaValue = 1;
		int willBeLoadNewWeek = PREVIOUS_4_WEEK_EVENTS;
		int willBeLoadNewWeekPosition = 0;
		int thrownOutWeek = NEXT_4_WEEK_EVENTS;
		ArrayList <ManageEventsPerWeek> objList = new ArrayList <ManageEventsPerWeek>();
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		int key = entry.getKey();
    		ManageEventsPerWeek valueObj = entry.getValue();
    		if (key == thrownOutWeek) {	     
    			// NEXT_4_WEEK_EVENTS[4] -> PREVIOUS_4_WEEK_EVENTS[-4]
    			valueObj.mWhichSideWeek = -(valueObj.mWhichSideWeek);			        			
    		}	        		
    		else { 
    			if (key == willBeLoadNewWeek) {
    				willBeLoadNewWeekPosition = valueObj.mWeekPosition - 1;	        				
        		}
    			
    			valueObj.mWhichSideWeek = valueObj.mWhichSideWeek + UpdateParaValue;        			
    		}
    		
    		objList.add(valueObj);        	        		      		
    	}	   
		
		removeManageEventsPerWeekHashMap();  
		
		int size = objList.size();
		for (int i=0; i<size; i++) {
			ManageEventsPerWeek Obj = objList.get(i);
			if (Obj.mWhichSideWeek == willBeLoadNewWeek) {
				Obj.makeStartJulianDay(willBeLoadNewWeekPosition);
				Obj.mEvents.clear();
				Obj.mEventDayList.clear();	
				
				loadEventsPerWeek(Obj);
    		}
			
			if (Obj.mWhichSideWeek == CURRENT_WEEK_EVENTS) {
				if (Obj != null) {
					//if (INFO) Log.i(TAG, "CURRENT_WEEK_EVENTS OK!!!");
				}
				else
					if (INFO) Log.i(TAG, "CURRENT_WEEK_EVENTS NULL OOOOOOOOOOOOOOps!!!");
			}
			
			mManageEventsPerWeekHashMap.put(Obj.mWhichSideWeek, Obj);	    			
		} 
    }
    
    public void setGoToLeftSideWeek(int howManyWeekgoTo) {
    	/*
		PREVIOUS_4_WEEK_EVENTS 
   		PREVIOUS_3_WEEK_EVENTS  	    		 
   	    PREVIOUS_2_WEEK_EVENTS : ���� today�� ���� ��ġ�ϰ� �ִٸ�....
   	    PREVIOUS_1_WEEK_EVENTS 	    	     
   	    CURRENT_WEEK_EVENTS 
   	    NEXT_1_WEEK_EVENTS 
   	    NEXT_2_WEEK_EVENTS 
   	    NEXT_3_WEEK_EVENTS 
   	    NEXT_4_WEEK_EVENTS 
   	    
   	                         ? -> PREVIOUS_4_WEEK_EVENTS : ������ NEXT_4_WEEK_EVENTS�� ���� load�ؾ� �Ѵ�
   	                         ? -> PREVIOUS_3_WEEK_EVENTS : ������ NEXT_3_WEEK_EVENTS�� ���� load�ؾ� �Ѵ�
   	    PREVIOUS_4_WEEK_EVENTS -> PREVIOUS_2_WEEK_EVENTS [-4 -> -2]
   		PREVIOUS_3_WEEK_EVENTS -> PREVIOUS_1_WEEK_EVENTS [-3 -> -1] 
   	    PREVIOUS_2_WEEK_EVENTS -> CURRENT_WEEK_EVENTS [-2 -> 0]
   	    PREVIOUS_1_WEEK_EVENTS -> NEXT_1_WEEK_EVENTS [-1 -> 1]	    	     
   	    CURRENT_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS [0 -> 2]
   	    NEXT_1_WEEK_EVENTS -> NEXT_3_WEEK_EVENTS
   	    NEXT_2_WEEK_EVENTS -> NEXT_4_WEEK_EVENTS
   	    NEXT_3_WEEK_EVENTS -> �� �̻� �ʿ� ���� [���ο� PREVIOUS_3_WEEK_EVENTS�� ���� �������� ���ȴ�]
   	    NEXT_4_WEEK_EVENTS -> �� �̻� �ʿ� ���� [���ο� PREVIOUS_4_WEEK_EVENTS�� ���� �������� ���ȴ�] 
   	    */   	
    	
    	int newCurrentWeekPosition = 0;    	    	
		ArrayList <ManageEventsPerWeek> objList = new ArrayList <ManageEventsPerWeek>();
		for (Iterator<Entry<Integer, ManageEventsPerWeek>> mgpw =
    			mManageEventsPerWeekHashMap.entrySet().iterator(); mgpw.hasNext();) {
		 
    		Entry<Integer, ManageEventsPerWeek> entry = mgpw.next();
    		//int key = entry.getKey();
    		ManageEventsPerWeek valueObj = entry.getValue();
    		int newWhichSideWeekNumber = valueObj.mWhichSideWeek + howManyWeekgoTo; //ex)CURRENT_WEEK_EVENTS -> NEXT_2_WEEK_EVENTS [0 -> 2]
    		valueObj.mWhichSideWeek = newWhichSideWeekNumber;
    		if (valueObj.mWhichSideWeek == CURRENT_WEEK_EVENTS) {
    			newCurrentWeekPosition = valueObj.mWeekPosition;
    		}
    		    		
    		objList.add(valueObj);    		       		
    	}	    		   		
		
		removeManageEventsPerWeekHashMap(); 
		
		int size = objList.size();
		for (int i=0; i<size; i++) {
			ManageEventsPerWeek Obj = objList.get(i);
			if (Obj.mWhichSideWeek > 4) {
				// �� for �������� �� �̻� next week�� �ʿ� ���� �͵��� 4���� ū ������ ������ 
				
				// �� �̻� next week�� �ʿ� ���� object���� ���� load �Ǿ�� �ϴ� new previous week���� ���� object��� Ȱ��Ǿ�� �ϴµ�
				// ���� �ѹ��� ���� howManyWeekgoTo�� ���� ����ȭ�� �ϸ� 
				// new previous week side ���� ��
				
				Obj.mWhichSideWeek = -(Obj.mWhichSideWeek - howManyWeekgoTo);
				int willBeLoadNewWeekPosition = newCurrentWeekPosition + Obj.mWhichSideWeek;
						
				Obj.makeStartJulianDay(willBeLoadNewWeekPosition);
				Obj.mEvents.clear();
				Obj.mEventDayList.clear();	
				
				loadEventsPerWeek(Obj);
    		}
			
			mManageEventsPerWeekHashMap.put(Obj.mWhichSideWeek, Obj);	    			
		}
    }
    
    public void reSetThreeDaysViewsBaseTime() {
    	mCircularThreeDaysEventsInfoMap.clear();
		
		DayView curView = (DayView) getCurrentDayView();   
		DayView preView = (DayView) getPreviousDayView();  
		DayView nextView = (DayView) getNextDayView();
		
		int curHour = curView.getFirstVisibleHour();
		int curHourOffset = curView.getFirstVisibleHourOffset();
		
		curView.setBaseTime(mCurrentDayViewBaseTime);
		
		/*
		Time newPrvDay = new Time(mCurrentDayViewBaseTime);
    	newPrvDay.normalize(true);
    	newPrvDay.monthDay = newPrvDay.monthDay - 1;
    	newPrvDay.normalize(true);
    	*/
		Calendar newPrvDay = GregorianCalendar.getInstance(mTimeZone);
		ETime.copyCalendar(mCurrentDayViewBaseTime, newPrvDay);
		newPrvDay.add(Calendar.DAY_OF_MONTH, -1);
    	preView.setBaseTime(newPrvDay);
    	
    	/*
    	Time newNextDay = new Time(mCurrentDayViewBaseTime);
		newNextDay.normalize(true);
		newNextDay.monthDay = newNextDay.monthDay + 1;
		newNextDay.normalize(true);
		*/
    	Calendar newNextDay = GregorianCalendar.getInstance(mTimeZone);
		ETime.copyCalendar(mCurrentDayViewBaseTime, newNextDay);
		newNextDay.add(Calendar.DAY_OF_MONTH, 1);
		nextView.setBaseTime(newNextDay);
		
		// ���� �� �ʿ䰡 ���� ������
		// :���� curView�� curView�� ������ ���̱� ������...
		curView.setFirstVisibleHour(curHour);
		curView.setFirstVisibleHourOffset(curHourOffset); 
    }
    
    public void reMapManageEventsPerWeekHashMap(ArrayList <ManageEventsPerWeek> objList, int willBeLoadNewWeek, int willBeLoadNewWeekPosition) {
    	int size = objList.size();
		for (int i=0; i<size; i++) {
			ManageEventsPerWeek Obj = objList.get(i);
			if (Obj.mWhichSideWeek == willBeLoadNewWeek) {
				Obj.makeStartJulianDay(willBeLoadNewWeekPosition);
				Obj.mEvents.clear();
				Obj.mEventDayList.clear();	
				
				loadEventsPerWeek(Obj);
    		}
			
			mManageEventsPerWeekHashMap.put(Obj.mWhichSideWeek, Obj);	    			
		}
    }
    
    
    
    public static final int PREVIOUS_1_WEEK_EVENTS = -1;
    public static final int PREVIOUS_2_WEEK_EVENTS = -2; 
    public static final int PREVIOUS_3_WEEK_EVENTS = -3;
    public static final int PREVIOUS_4_WEEK_EVENTS = -4;
    public static final int CURRENT_WEEK_EVENTS = 0;
    public static final int NEXT_1_WEEK_EVENTS = 1;
    public static final int NEXT_2_WEEK_EVENTS = 2;
    public static final int NEXT_3_WEEK_EVENTS = 3;
    public static final int NEXT_4_WEEK_EVENTS = 4;
    
    public final int TODAY_WEEK_EVENTS = 0xF; //mManageEventsOfTodayWeek
    public final int PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK = 0xE; //mManageEventsOfTodayWeek
    public final int NEXT_WEEK_EVENTS_OF_TODAY_WEEK = 0x10; //mManageEventsOfTodayWeek
    
    public static final int UPDATE_CURRENT_TIME_DELAY = 300000;
    private Handler mHandler;    
    private final UpdateCurrentTime mUpdateCurrentTime = new UpdateCurrentTime();
    
    int mTodayJulianDay;
    class UpdateCurrentTime implements Runnable {

        public void run() {
        	
        	if (!DayFragment.this.mPaused) {
	            long currentTime = System.currentTimeMillis();
	            //Time CurrentTime = new Time(DayFragment.this.mTimeZoneString);
	            Calendar CurrentTime = GregorianCalendar.getInstance(DayFragment.this.mTimeZone); 
	            CurrentTime.setFirstDayOfWeek(DayFragment.this.mFirstDayOfWeek);
	            CurrentTime.setTimeInMillis(currentTime);    
	            //Log.i(TAG, "UpdateCurrentTime:" + CurrentTime.format2445());
	            
	            long delayMillis = UPDATE_CURRENT_TIME_DELAY - (currentTime % UPDATE_CURRENT_TIME_DELAY);
	            mHandler.postDelayed(mUpdateCurrentTime, delayMillis);	   
	            long gmtoff = DayFragment.this.mTimeZone.getRawOffset() / 1000;
	            int todayJulainDay = ETime.getJulianDay(currentTime, mTimeZone, mFirstDayOfWeek);
	            if (mTodayJulianDay != todayJulainDay) {
	            	mTodayJulianDay = todayJulainDay;
	            		            	
	            	ManageEventsPerWeek manageEventsOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(TODAY_WEEK_EVENTS);
	            	ManageEventsPerWeek manageEventsPreviousWeekOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
	            	ManageEventsPerWeek manageEventsNextWeekOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(TODAY_WEEK_EVENTS);
	            	
	            	if (mTodayJulianDay < manageEventsOfTodayWeek.mWeekStartJulianDay && mTodayJulianDay <= manageEventsPreviousWeekOfTodayWeek.mWeekStartJulianDay) {            		
	            		
	            		//ManageEventsPerWeek manageEventsPreviousWeekOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
	            		manageEventsPreviousWeekOfTodayWeek.mWhichSideWeek = TODAY_WEEK_EVENTS;
	            		            		
	            		manageEventsOfTodayWeek.mWhichSideWeek = NEXT_WEEK_EVENTS_OF_TODAY_WEEK;            		
	            		
	            		// ���ο� previous today week�� ���ؾ� �Ѵ�
	            		// ����� next today week�� �ȴ�
	            		ManageEventsPerWeek manageEventsNewPreviousWeekOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(NEXT_WEEK_EVENTS_OF_TODAY_WEEK);
	            		manageEventsNewPreviousWeekOfTodayWeek.mWhichSideWeek = PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK;
	            		manageEventsNewPreviousWeekOfTodayWeek.mWeekPosition = manageEventsPreviousWeekOfTodayWeek.mWeekPosition - 1;
	            		
	            		mManageEventsPerTodayWeekHashMap.remove(TODAY_WEEK_EVENTS);
	            		mManageEventsPerTodayWeekHashMap.remove(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
	            		mManageEventsPerTodayWeekHashMap.remove(NEXT_WEEK_EVENTS_OF_TODAY_WEEK);            		
	            		
	            		mManageEventsPerTodayWeekHashMap.put(TODAY_WEEK_EVENTS, manageEventsPreviousWeekOfTodayWeek);
	            		mManageEventsPerTodayWeekHashMap.put(NEXT_WEEK_EVENTS_OF_TODAY_WEEK, manageEventsOfTodayWeek);            		
	            		
	            		loadEventsPerWeek(manageEventsNewPreviousWeekOfTodayWeek);		
	            		
	            	}
	            	else if (mTodayJulianDay > manageEventsOfTodayWeek.mWeekEndJulianDay && mTodayJulianDay <= manageEventsNextWeekOfTodayWeek.mWeekEndJulianDay) {
	            		//ManageEventsPerWeek manageEventsNextWeekOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(NEXT_WEEK_EVENTS_OF_TODAY_WEEK);
	            		manageEventsNextWeekOfTodayWeek.mWhichSideWeek = TODAY_WEEK_EVENTS;
	            		            		
	            		manageEventsOfTodayWeek.mWhichSideWeek = PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK;            		
	            		
	            		// ���ο� next today week�� ���ؾ� �Ѵ�
	            		// ����� previous today week�� �ȴ�
	            		ManageEventsPerWeek manageEventsNewNextWeekOfTodayWeek = mManageEventsPerTodayWeekHashMap.get(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
	            		manageEventsNewNextWeekOfTodayWeek.mWhichSideWeek = NEXT_WEEK_EVENTS_OF_TODAY_WEEK;
	            		manageEventsNewNextWeekOfTodayWeek.mWeekPosition = manageEventsNextWeekOfTodayWeek.mWeekPosition + 1;
	            		
	            		mManageEventsPerTodayWeekHashMap.remove(TODAY_WEEK_EVENTS);
	            		mManageEventsPerTodayWeekHashMap.remove(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
	            		mManageEventsPerTodayWeekHashMap.remove(NEXT_WEEK_EVENTS_OF_TODAY_WEEK);            		
	            		
	            		mManageEventsPerTodayWeekHashMap.put(TODAY_WEEK_EVENTS, manageEventsNextWeekOfTodayWeek);
	            		mManageEventsPerTodayWeekHashMap.put(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK, manageEventsOfTodayWeek);            		
	            		
	            		loadEventsPerWeek(manageEventsNewNextWeekOfTodayWeek);		
	            		
	            	}
	            	else {
	            		// �� �� ���ǿ� �������� �����Ƿ�
	            		// ���� �� ���� week �̺�Ʈ���� load �ؾ� �Ѵ�
	            		// ...
	            		mManageEventsPerTodayWeekHashMap.remove(TODAY_WEEK_EVENTS);
	            		mManageEventsPerTodayWeekHashMap.remove(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK);
	            		mManageEventsPerTodayWeekHashMap.remove(NEXT_WEEK_EVENTS_OF_TODAY_WEEK);            
	            		
	            		int todayWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(System.currentTimeMillis(), mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(mTodayJulianDay, mFirstDayOfWeek); 
	                	manageEventsOfTodayWeek = new ManageEventsPerWeek(DayFragment.this.mTimeZone, TODAY_WEEK_EVENTS, todayWeekPosition, mFirstDayOfWeek);     	
	                	
	                	int previousWeekPositionOfTodayWeek = todayWeekPosition - 1;
	                	manageEventsPreviousWeekOfTodayWeek = new ManageEventsPerWeek(DayFragment.this.mTimeZone, PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK, previousWeekPositionOfTodayWeek, mFirstDayOfWeek);  
	                	
	                	int nextWeekPositionOfTodayWeek = todayWeekPosition + 1;
	                	manageEventsNextWeekOfTodayWeek = new ManageEventsPerWeek(DayFragment.this.mTimeZone, NEXT_WEEK_EVENTS_OF_TODAY_WEEK, nextWeekPositionOfTodayWeek, mFirstDayOfWeek);
	                	    	
	                	mManageEventsPerTodayWeekHashMap.put(TODAY_WEEK_EVENTS, manageEventsOfTodayWeek);
	                	mManageEventsPerTodayWeekHashMap.put(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK, manageEventsPreviousWeekOfTodayWeek);
	                	mManageEventsPerTodayWeekHashMap.put(NEXT_WEEK_EVENTS_OF_TODAY_WEEK, manageEventsNextWeekOfTodayWeek);
	                	
	                	loadEventsPerWeek(manageEventsOfTodayWeek);
	                	loadEventsPerWeek(manageEventsPreviousWeekOfTodayWeek);
	                	loadEventsPerWeek(manageEventsNextWeekOfTodayWeek);           		
	            	}            	
	            }             
        	}            
        }
    }
    
    public void removeManageEventsPerWeekHashMap() {
    	mManageEventsPerWeekHashMap.remove(PREVIOUS_4_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(PREVIOUS_3_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(PREVIOUS_2_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(PREVIOUS_1_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(CURRENT_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(NEXT_1_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(NEXT_2_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(NEXT_3_WEEK_EVENTS);	   
		mManageEventsPerWeekHashMap.remove(NEXT_4_WEEK_EVENTS);	   
    }
    
    
    
	public void copyManageEventsPerWeekObj(ManageEventsPerWeek destination, ManageEventsPerWeek source) {
    		
    	
    	destination.mTimezone = mTimeZone;
    	destination.mFirstDayOfWeek = source.mFirstDayOfWeek;
    	destination.mWeekPosition = source.mWeekPosition;    	
    	destination.mStartDayTime = GregorianCalendar.getInstance(mTimeZone);
    	destination.mStartDayTime.setFirstDayOfWeek(source.mStartDayTime.getFirstDayOfWeek());
    	destination.mStartDayTime.setTimeInMillis(source.mStartDayTime.getTimeInMillis());
    	destination.mWeekStartJulianDay = source.mWeekStartJulianDay;
    	destination.mWeekEndJulianDay = source.mWeekEndJulianDay;      
    	destination.mEvents.addAll(source.mEvents);
    	destination.mEventDayList.addAll(source.mEventDayList);
    	
    	//destination.mEvents = (ArrayList<Event>) source.mEvents.clone();    	
    	//destination.mEventDayList = (ArrayList<ArrayList<Event>>) source.mEventDayList.clone(); 
    	
    	
    	/*
    	int eventCounts = source.mEvents.size();
    	destination.mEvents = new ArrayList<Event>(eventCounts);
    	int allDayEventCounts = source.mEventDayList.size();
    	destination.mEventDayList = new ArrayList<ArrayList<Event>>(allDayEventCounts);    	
    	
    	Collections.copy(destination.mEvents, source.mEvents);
    	Collections.copy(destination.mEventDayList, source.mEventDayList);
    	*/
    }
	
	public void setTodayTime() {
		long gmtoff = mTimeZone.getRawOffset() / 1000;
    	mTodayJulianDay = ETime.getJulianDay(System.currentTimeMillis(), mTimeZone, mFirstDayOfWeek);
	}
	
	public void loadTodayWeeks() {
		// ���⼭ �츮�� today�� ���� week position�� current week position�� �����ϴٸ�
		// �Ʒ��� �۾��� �ƴ�...current week position���� copy�ϴ� �۾��� ����Ǿ�� �Ѵ�.		
		int todayWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(System.currentTimeMillis(), mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(mTodayJulianDay, mFirstDayOfWeek); 
		long gmtoff = mTimeZone.getRawOffset() / 1000;
		int targetJulainDay = ETime.getJulianDay(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        int currentWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(mCurrentDayViewBaseTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//Utils.getWeeksSinceEpochFromJulianDay(targetJulainDay, mFirstDayOfWeek); 
        
        if (todayWeekPosition != currentWeekPosition) {
	    	ManageEventsPerWeek manageEventsOfTodayWeek = new ManageEventsPerWeek(mTimeZone, TODAY_WEEK_EVENTS, todayWeekPosition, mFirstDayOfWeek);     	
	    	
	    	int previousWeekPositionOfTodayWeek = todayWeekPosition - 1;
	    	ManageEventsPerWeek manageEventsPreviousWeekOfTodayWeek = new ManageEventsPerWeek(mTimeZone, PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK, previousWeekPositionOfTodayWeek, mFirstDayOfWeek);  
	    	
	    	int nextWeekPositionOfTodayWeek = todayWeekPosition + 1;
	    	ManageEventsPerWeek manageEventsNextWeekOfTodayWeek = new ManageEventsPerWeek(mTimeZone, NEXT_WEEK_EVENTS_OF_TODAY_WEEK, nextWeekPositionOfTodayWeek, mFirstDayOfWeek);
	    	    	
	    	loadEventsPerWeek(manageEventsOfTodayWeek);
	    	loadEventsPerWeek(manageEventsPreviousWeekOfTodayWeek);
	    	loadEventsPerWeek(manageEventsNextWeekOfTodayWeek);
        }
        else {
        	    			 
        	ManageEventsPerWeek manageEventsOfTodayWeek = new ManageEventsPerWeek(mTimeZone, TODAY_WEEK_EVENTS, todayWeekPosition, mFirstDayOfWeek);     	
	    	
	    	int previousWeekPositionOfTodayWeek = todayWeekPosition - 1;
	    	ManageEventsPerWeek manageEventsPreviousWeekOfTodayWeek = new ManageEventsPerWeek(mTimeZone, PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK, previousWeekPositionOfTodayWeek, mFirstDayOfWeek);  
	    	
	    	int nextWeekPositionOfTodayWeek = todayWeekPosition + 1;
	    	ManageEventsPerWeek manageEventsNextWeekOfTodayWeek = new ManageEventsPerWeek(mTimeZone, NEXT_WEEK_EVENTS_OF_TODAY_WEEK, nextWeekPositionOfTodayWeek, mFirstDayOfWeek);
	    	    	
        	ManageEventsPerWeek curWeekObj = mManageEventsPerWeekHashMap.get(CURRENT_WEEK_EVENTS);
        	ManageEventsPerWeek prvWeekObj = mManageEventsPerWeekHashMap.get(PREVIOUS_1_WEEK_EVENTS);
        	ManageEventsPerWeek NextObj = mManageEventsPerWeekHashMap.get(NEXT_1_WEEK_EVENTS);
						
			copyManageEventsPerWeekObj(manageEventsOfTodayWeek, curWeekObj);
			copyManageEventsPerWeekObj(manageEventsPreviousWeekOfTodayWeek, prvWeekObj);
			copyManageEventsPerWeekObj(manageEventsNextWeekOfTodayWeek, NextObj);    
			
			setManageEventsPerTodayWeekEvent(TODAY_WEEK_EVENTS, manageEventsOfTodayWeek); 
			setManageEventsPerTodayWeekEvent(PREVIOUS_WEEK_EVENTS_OF_TODAY_WEEK, manageEventsPreviousWeekOfTodayWeek); 
			setManageEventsPerTodayWeekEvent(NEXT_WEEK_EVENTS_OF_TODAY_WEEK, manageEventsNextWeekOfTodayWeek);       	
        }
	}
	
	
		
    
 
    
    
    /*
    public void test() {
    	TimeZone gmtTZ = TimeZone.getTimeZone("GMT");    	
    	long offsetMillis = gmtTZ.getRawOffset();
    	
    	//Time epochTime = new Time(gmtTZ.getID());    
    	//epochTime.set(0); // 1970/01/01/00:00:00�� ��Ÿ����
    	
    	
    	// millis�� ���ؾ� �Ѵ�
    	// 2457339 : 2015/11/12/00:00:00
    	// 2415020.5 -> 2415021 : 1900/01/01/00:00:00
    	int targetJulianDay = 2415021;
    	// millis��� ���� epoch�� �������� ����/���� ����� �ð��̱� ������
    	// ������ ���� targetJulianDay�� Time.EPOCH_JULIAN_DAY�� ����� �Ѵ�
    	int calJulianDay = targetJulianDay - ETime.EPOCH_JULIAN_DAY;      	
    	long targetJulianDayMillis = (calJulianDay * DateUtils.DAY_IN_MILLIS) - offsetMillis;
    	
    	Calendar startDayCalendar = GregorianCalendar.getInstance(gmtTZ);     
    	startDayCalendar.setTimeInMillis(targetJulianDayMillis);  	
    	
    }
	*/
    
    /*
    public void testCalendarJulianDay() {
    	// �츮�� Ecalendar�� 1900 - 2100year ���� Ȯ�븦 ����
    	// ���Ŀ� Time class�� ������� �ʴ� �ڵ带 ���������� ����� ���̴�
    	// ->Calendar�� ���� ������ ���̴�...
    	//   :�׷��� Time.getJulianDay�� �˰����� ����� ���̴�!!!
    	//    year Max �ѵ��� ������� ��Ȯ�� julian day ���� ����Ѵ�
    	// Time.set�� normalize�� ȣ���ϴ� JNI �Լ��� ���������� 2037year �̻��� �⵵�� �������� �ʴ� �� ����
    	// :����ϸ� ������ �߻��Ѵ�
    	// http://www.onlineconversion.com/julian_date.htm �� julain day Calculator�� ���� ��
    	// ������ ���� Universal Time�� ��[GMT]�� �Ѵ�
    	TimeZone gmtTZ = TimeZone.getTimeZone("GMT");    	
    	long gmtoff = gmtTZ.getRawOffset() / 1000;
    	
    	Calendar ecalendarEpochYearCal = GregorianCalendar.getInstance(gmtTZ);     	
    	ecalendarEpochYearCal.set(Calendar.YEAR, 1900);
    	ecalendarEpochYearCal.set(Calendar.MONTH, Calendar.JANUARY);
    	ecalendarEpochYearCal.set(Calendar.DAY_OF_MONTH, 1);
    	ecalendarEpochYearCal.set(Calendar.HOUR_OF_DAY, 0);
    	ecalendarEpochYearCal.set(Calendar.MINUTE, 0);
    	ecalendarEpochYearCal.set(Calendar.SECOND, 0);
    	ecalendarEpochYearCal.set(Calendar.MILLISECOND, 0);
    	    	
    	long ecalendarEpochYearCalMillis = ecalendarEpochYearCal.getTimeInMillis();
    	int ecalendarEpochYearCalMillisYearTimeJulianDay = Time.getJulianDay(ecalendarEpochYearCalMillis, gmtoff);  
    	
    	Time epochTime = new Time(gmtTZ.getID());    
    	epochTime.set(0);
    	//epochTime.setJulianDay(ecalendarEpochYearCalMillisYearTimeJulianDay); // �̷��� ����� �� ����...203x��� ���͸� ���� ���� �̴� Time.setJulianDay�� ���������� Time.set�� ȣ���ϱ� �����̴�
    	    	
    	Calendar lastYearCal = GregorianCalendar.getInstance(gmtTZ);    	
    	lastYearCal.set(Calendar.YEAR, 2065);
    	lastYearCal.set(Calendar.MONTH, Calendar.JANUARY);
    	lastYearCal.set(Calendar.DAY_OF_MONTH, 1);
    	lastYearCal.set(Calendar.HOUR_OF_DAY, 0);
    	lastYearCal.set(Calendar.MINUTE, 0);
    	lastYearCal.set(Calendar.SECOND, 0);
    	lastYearCal.set(Calendar.MILLISECOND, 0);
    	
    	long lastYearCalMillis = lastYearCal.getTimeInMillis();
    	int lastYearTimeJulianDay = Time.getJulianDay(lastYearCalMillis, gmtoff);
    	
    	
    	int test = -1;
    	test = 0;
    }
    
    // ���� �����(Coordinated Universal Time, UTC)�� 1972�� 1�� 1�Ϻ��� ����� ���� ǥ�ؽ��̴�. UTC�� �������ڽÿ� ���� ������ ������� ǥ��ȭ�Ǿ���.
    // UTC�� �׸���ġ ��ս�(GMT)�� �Ҹ��⵵ �ϴµ�, UTC�� GMT�� ���� �Ҽ��� ���������� ���̰� ���� ������ �ϻ󿡼��� ȥ��Ǿ� ���ȴ�. ������� ǥ�⿡���� UTC�� ���ȴ�.
    // ���н� �ð�(Unix time)�� �ð��� ��Ÿ���� ����̴�. POSIX �ð��̳� Epoch �ð��̶�� �θ��⵵ �Ѵ�. 
    // 1970�� 1�� 1�� 00:00:00 ���� �����(UTC) ������ ��� �ð��� �ʷ� ȯ���Ͽ� ������ ��Ÿ�� ���̴�.
    // ���� Time�� UTC Millis�� �����ϴ� ���� ����...
    int mEcalendarEpochWeekTimeJulianDay;
    public void gregoriX() {
    	Time epochTime = new Time(mTimeZoneString);    	
    	epochTime.set(0, 0, 0, 1, 0, 1910);
    	epochTime.normalize(true);
    	long epochTimeMillis = epochTime.toMillis(true);
    	int epochJulianDay = Time.getJulianDay(epochTime.toMillis(true), epochTime.gmtoff);
    	//int epochWeeks = Time.getWeeksSinceEpochFromJulianDay(epochJulianDay, mFirstDayOfWeek);
    	int epochWeeks = ETime.getWeeksSinceEcalendarEpochFromJulianDay(epochJulianDay, mFirstDayOfWeek);
    	
    	Time lastYearTime = new Time(mTimeZoneString);    	
    	lastYearTime.set(0, 0, 0, 1, 0, 2037);
    	lastYearTime.normalize(true);
    	long lastYearTimeMillis = lastYearTime.toMillis(true);
    	//int lastYearTimeJulianDay = Time.getJulianDay(lastYearTimeMillis, lastYearTime.gmtoff);
    	//int lastYearTimeWeeks = Time.getWeeksSinceEpochFromJulianDay(lastYearTimeJulianDay, mFirstDayOfWeek);
    	//int lastYearTimeWeeks = ETime.getWeeksSinceEcalendarEpochFromJulianDay(lastYearTimeJulianDay, mFirstDayOfWeek);
    	
    	TimeZone gmtTZ = TimeZone.getTimeZone("GMT");    	
    	Time xxx = new Time(gmtTZ.getID());
    	Calendar ecalendarEpochYearCal = GregorianCalendar.getInstance(gmtTZ);       	
    	ecalendarEpochYearCal.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
    	long ecalendarEpochYearCalMillis = ecalendarEpochYearCal.getTimeInMillis();
    	int ecalendarEpochYearCalMillisYearTimeJulianDay = Time.getJulianDay(ecalendarEpochYearCalMillis, xxx.gmtoff);
    	//epochTime.set(ecalendarEpochYearCalMillis);
    	ecalendarEpochYearCal.setTimeInMillis(ecalendarEpochYearCalMillis);
    	
    	Calendar lastYearCal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));    	
    	lastYearCal.set(2100, Calendar.JANUARY, 1, 0, 0, 0);
    	long lastYearCalMillis = lastYearCal.getTimeInMillis();
    	int lastYearTimeJulianDay = Time.getJulianDay(lastYearCalMillis, lastYearTime.gmtoff);
    	
    	
    	
    	Time epochPrvDay = new Time(mTimeZoneString);    	
    	epochPrvDay.set(0, 0, 0, 31, 11, 1969);
    	epochPrvDay.normalize(true);    	
    	long epochPrvDayTimeMillis = epochPrvDay.toMillis(true);
    	int epochPrvDayJulianDay = Time.getJulianDay(epochPrvDay.toMillis(true), epochPrvDay.gmtoff);
    	//int epochPrvDayWeeks = getWeeksSinceEcalendarEpochFromJulianDay(epochPrvDayJulianDay, mFirstDayOfWeek);
    	  	
    	
    	Time ecalendarEpochWeekTime = new Time(mTimeZoneString);    	
    	ecalendarEpochWeekTime.set(0, 0, 0, 1, 0, 1910);
    	
    	ecalendarEpochWeekTime.normalize(true);    	
    	long epochPrvWeeksTimeMillis = ecalendarEpochWeekTime.toMillis(true);
    	mEcalendarEpochWeekTimeJulianDay = Time.getJulianDay(ecalendarEpochWeekTime.toMillis(true), ecalendarEpochWeekTime.gmtoff);
    	//int epochPrvWeeks = getWeeksSinceEcalendarEpochFromJulianDay(mEcalendarEpochWeekTimeJulianDay, mFirstDayOfWeek);
    	
    	int minJulianDayForMeasure = mEcalendarEpochWeekTimeJulianDay;
    	
    	minJulianDayForMeasure--;
    	int minJulianDay = 0;
    	long prvMillis = 0;
    	while(true) {
    		Time time = new Time(mTimeZoneString);    
    		time.hour = 0;
    		time.normalize(true);
    		long curMillis = setEcalendarJulianDay(time, minJulianDayForMeasure);    		
    		//long curMillis = time.toMillis(true); 
    		if (curMillis == -2147418000000L) {
    			if (INFO) Log.i(TAG, time.format2445());
    		}
    		else if (curMillis == -1) { 
    			if (INFO) Log.i(TAG, "curMillis == -1:" + time.format2445());
    			//break;
    		}
    		else if (curMillis == 2147266800000L) {
    			break;
    		}
    		else {    			
    			if (INFO) Log.i(TAG, time.format2445());
    			if (INFO) Log.i(TAG, "curMillis:" + String.valueOf(curMillis));
    		}
    		minJulianDay = minJulianDayForMeasure;
    		prvMillis = curMillis;
    		minJulianDayForMeasure--;    		
    	}   
    	
    	Time minTime = new Time(mTimeZoneString);
    	minTime.setJulianDay(minJulianDay);
    	
    	
    }
    
    
    public long setEcalendarJulianDay(Time time, int julianDay) {
        // Don't bother with the GMT offset since we don't know the correct
        // value for the given Julian day.  Just get close and then adjust
        // the day.
        long millis = (julianDay - Time.EPOCH_JULIAN_DAY) * DateUtils.DAY_IN_MILLIS;
        time.set(millis); // ���⼭���� ������ �߻��Ѵ�...

        // Figure out how close we are to the requested Julian day.
        // We can't be off by more than a day.
        int approximateDay = Time.getJulianDay(millis, time.gmtoff);
        int diff = julianDay - approximateDay;
        time.monthDay += diff;

        // Set the time to 12am and re-normalize.
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        millis = time.normalize(true);
        return millis;
    }
    */
    
    /*
    : /frameworks/base/core/jni/Time.cpp
    void Time::set(int64_t millis)
    {
        time_t seconds = millis / 1000;
        localtime_tz(&seconds, &(this->t), this->timezone);
    }
    
    
    : /bionic/libc/tzcode/localtime.c
    void localtime_tz(const time_t* const timep, struct tm* tmp, const char* tz) {
		struct state st;
	  	if (__bionic_tzload_cached(tz, &st, TRUE) != 0) {
	   		// TODO: not sure what's best here, but for now, we fall back to gmt.
	    	gmtload(&st);
	  	}
	  	localsub(timep, 0L, tmp, &st);
	}
	
	
static struct tm *localsub(const time_t * const timep, const int_fast32_t offset,
         struct tm * const tmp, const struct state * sp) // android-changed: added sp.
{
    register const struct ttinfo *  ttisp;
    register int            i;
    register struct tm *        result;
    const time_t            t = *timep;

    // BEGIN android-changed: support user-supplied sp.
    if (sp == NULL) {
        sp = lclptr;
    }
    // END android-changed
#ifdef ALL_STATE
    if (sp == NULL)
        return gmtsub(timep, offset, tmp, sp); // android-changed: added sp.
#endif 
    if ((sp->goback && t < sp->ats[0]) ||
        (sp->goahead && t > sp->ats[sp->timecnt - 1])) {
            time_t          newt = t;
            register time_t     seconds;
            register time_t     tcycles;
            register int_fast64_t   icycles;

            if (t < sp->ats[0])
                seconds = sp->ats[0] - t;
            else    
            	seconds = t - sp->ats[sp->timecnt - 1];
            --seconds;
            tcycles = seconds / YEARSPERREPEAT / AVGSECSPERYEAR;
            ++tcycles;
            icycles = tcycles;
            if (tcycles - icycles >= 1 || icycles - tcycles >= 1)
                return NULL;
            seconds = icycles;
            seconds *= YEARSPERREPEAT;
            seconds *= AVGSECSPERYEAR;
            if (t < sp->ats[0])
                newt += seconds;
            else    newt -= seconds;
            if (newt < sp->ats[0] ||
                newt > sp->ats[sp->timecnt - 1])
                    return NULL;    
            result = localsub(&newt, offset, tmp, sp); // android-changed: added sp.
            if (result == tmp) {
                register time_t newy;

                newy = tmp->tm_year;
                if (t < sp->ats[0])
                    newy -= icycles * YEARSPERREPEAT;
                else    newy += icycles * YEARSPERREPEAT;
                tmp->tm_year = newy;
                if (tmp->tm_year != newy)
                    return NULL;
            }
            return result;
    }
    
    if (sp->timecnt == 0 || t < sp->ats[0]) {
        i = sp->defaulttype;
    } else {
        register int    lo = 1;
        register int    hi = sp->timecnt;

        while (lo < hi) {
            register int mid = (lo + hi) >> 1;

            if (t < sp->ats[mid])
                hi = mid;
            else    lo = mid + 1;
        }
        i = (int) sp->types[lo - 1];
    }
    ttisp = &sp->ttis[i];
    
    // To get (wrong) behavior that's compatible with System V Release 2.0
    // you'd replace the statement below with
    //  t += ttisp->tt_gmtoff;
    //  timesub(&t, 0L, sp, tmp);
    
    result = timesub(&t, ttisp->tt_gmtoff, sp, tmp);
    tmp->tm_isdst = ttisp->tt_isdst;
    tzname[tmp->tm_isdst] = &sp->chars[ttisp->tt_abbrind];
#ifdef TM_ZONE
    tmp->TM_ZONE = &sp->chars[ttisp->tt_abbrind];
#endif 
    return result;
}
   */
    
    
    
    // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
    public static int JGREG= 15 + 31*(10+12*1582);
    public static double HALFSECOND = 0.5;

    public static double toJulian(int[] ymd) {
    	
    	int year=ymd[0];
	    int month=ymd[1]; // jan=1, feb=2,...
	    int day=ymd[2];
	    int julianYear = year;
	    if (year < 0) julianYear++;
	    int julianMonth = month;
	    if (month > 2) {
	    	julianMonth++;
	    }
	    else {
	        julianYear--;
	        julianMonth += 13;
	    }
	
	    double julian = (java.lang.Math.floor(365.25 * julianYear)
	          + java.lang.Math.floor(30.6001*julianMonth) + day + 1720995.0);
	    if (day + 31 * (month + 12 * year) >= JGREG) {
	        // change over to Gregorian calendar
	        int ja = (int)(0.01 * julianYear);
	        julian += 2 - ja + (0.25 * ja);
	    }
	    
	    return java.lang.Math.floor(julian);
   }

   /**
   * Converts a Julian day to a calendar date
   * ref :
   * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
   */
   public static int[] fromJulian(double injulian) {
     int jalpha,ja,jb,jc,jd,je,year,month,day;
     double julian = injulian + HALFSECOND / 86400.0;
     ja = (int) julian;
     if (ja>= JGREG) {
       jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
       ja = ja + 1 + jalpha - jalpha / 4;
     }

     jb = ja + 1524;
     jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
     jd = 365 * jc + jc / 4;
     je = (int) ((jb - jd) / 30.6001);
     day = jb - jd - (int) (30.6001 * je);
     month = je - 1;
     if (month > 12) month = month - 12;
     year = jc - 4715;
     if (month > 2) year--;
     if (year <= 0) year--;

     return new int[] {year, month, day};
    }
}
