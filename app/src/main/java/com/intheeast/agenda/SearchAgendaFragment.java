/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.intheeast.agenda;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;









import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.agenda.AgendaByDayAdapter.RowInfo;
import com.intheeast.agenda.AgendaWindowAdapter.DayAdapterInfo;
import com.intheeast.acalendar.BothEndsEventLoader;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.GOT_BOTHENDS_EVENT_TYPE;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;
import com.intheeast.acalendar.EventLoader;
import com.intheeast.acalendar.CalendarViewsLowerActionBar;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.FastEventInfoFragment;
import com.intheeast.acalendar.EventInfoLoader;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.SearchViewUpperActionBarFragment;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthListView;
import com.intheeast.month.MonthWeekView;
import com.intheeast.settings.SettingsFragment;


@SuppressLint("ValidFragment")
public class SearchAgendaFragment extends Fragment implements CalendarController.EventHandler,
        OnScrollListener {

    private static final String TAG = SearchAgendaFragment.class.getSimpleName();
    private static boolean INFO = true;

    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    protected static final String BUNDLE_KEY_RESTORE_INSTANCE_ID = "key_restore_instance_id";

    private AgendaListView mAgendaListView;
    private Activity mActivity;
    private final Calendar mTime;
    private String mTzId;
    private TimeZone mTimeZone;
    private long mGmtOff;
    int mFirstDayOfWeek;
    private final long mInitialTimeMillis;
    private boolean mShowEventDetailsWithAgenda;
    private CalendarController mController;
    //private EventInfoFragment mEventFragment;
    private String mQuery;
    //private boolean mUsedForSearch = false;
    private boolean mIsTabletConfig;
    private EventInfo mOnAttachedInfo = null;
    private boolean mOnAttachAllDay = false;
    private AgendaWindowAdapter mAdapter = null;
    private boolean mForceReplace = true;
    private long mLastShownEventId = -1;
    

    // Tracks the time of the top visible view in order to send UPDATE_TITLE messages to the action
    // bar.
    int  mJulianDayOnTop = -1;
    
    Event mForemostEvent = new Event(); 
    Event mBackmostEvent = new Event();    
    
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
        	mTzId = Utils.getTimeZone(mContext, this);
        	mTimeZone = TimeZone.getTimeZone(mTzId);
        	mGmtOff = mTimeZone.getRawOffset() / 1000;
            mTime.setTimeZone(mTimeZone);
        }
    };

    SearchViewUpperActionBarFragment mSearchUpperActionBar;    
    
    public SearchAgendaFragment(long timeMillis, SearchViewUpperActionBarFragment upperActionBarFrag) {
        mInitialTimeMillis = timeMillis;
        mTime = GregorianCalendar.getInstance();
        mLastHandledEventTime = GregorianCalendar.getInstance();

        if (mInitialTimeMillis == 0) {
            mTime.setTimeInMillis(System.currentTimeMillis());
        } else {
            mTime.setTimeInMillis(mInitialTimeMillis);
        }
        
        mLastHandledEventTime.setTimeInMillis(mTime.getTimeInMillis());
        //mUsedForSearch = true;
        
        mSearchUpperActionBar = upperActionBarFrag;
        
        mAgendaDayHeaderStringBuilder = new StringBuilder(50);
        mAgendaDayHeaderFormatter = new Formatter(mAgendaDayHeaderStringBuilder, Locale.getDefault());
        
        
    }     
    
    Context mContext;
    ECalendarApplication mApp;
    FastEventInfoFragment mEventInfoFragment;
    private static String mNoTitleString;
    private static int mNoColorColor;
    float mWidth;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if (INFO) Log.i(TAG, "onAttach");         
        
        mContext = activity.getApplicationContext();
        mApp = (ECalendarApplication) activity.getApplication();        
                
        mTzId = Utils.getTimeZone(activity, mTZUpdater);
    	mTimeZone = TimeZone.getTimeZone(mTzId);
    	mGmtOff = mTimeZone.getRawOffset() / 1000;
    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);

    	mTime.setTimeZone(mTimeZone);
    	
        mActivity = activity;
        mController = CalendarController.getInstance(mActivity);
        if (mOnAttachedInfo != null) {
            showEventInfo(mOnAttachedInfo, mOnAttachAllDay, true);
            mOnAttachedInfo = null;
        }
        
        calcFrameLayoutHeight();
        
        Resources res = mContext.getResources();
        mNoTitleString = res.getString(R.string.no_title_label);
        mNoColorColor = res.getColor(R.color.event_center);
        
        mWidth = getResources().getDisplayMetrics().widthPixels;
        
    	mEventInfoFragment = new FastEventInfoFragment(mContext, activity, this, ViewType.AGENDA, mWidth);
        
        //mEventInfoFragment = new FastEventInfoFragment(mContext, activity, this, mRecoveryCallbackFromEventInfo, ViewType.AGENDA);
    	mEventInfoFragment.onAttach(activity);
    }

    EventInfoLoader mEventInfoLoader; 
    EventLoader mEventLoader;
    
   
    /*
    SQLite�� MAX/MIN Function�� 
    SELECT ������ ���Ǵ� ���̴�. WHERE ������ ���Ǹ� �ȵȴ�(misuse��� ���� �޽����� Ȯ���� �� �ִ�)
    */    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (INFO) Log.i(TAG, "onCreate");
        
        
        mShowEventDetailsWithAgenda =
            Utils.getConfigBool(mActivity, R.bool.show_event_details_with_agenda);
        mIsTabletConfig =
            Utils.getConfigBool(mActivity, R.bool.tablet_config);
        if (icicle != null) {
            long prevTime = icicle.getLong(BUNDLE_KEY_RESTORE_TIME, -1);
            if (prevTime != -1) {
                mTime.setTimeInMillis(prevTime);
                if (INFO) Log.i(TAG, "Restoring time to " + mTime.toString());                
            }
        }
        
        mEventInfoLoader = new EventInfoLoader(mContext); 
        mEventLoader = new EventLoader(mContext);     
        
    }
     

    /*
    OnClickListener mTodayClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Time t = null;
	        int viewType = ViewType.CURRENT;
	        //long extras = CalendarController.EXTRA_GOTO_TIME;
	        //extras |= CalendarController.EXTRA_GOTO_TODAY;
	        
	        viewType = ViewType.CURRENT;
            t = new Time(mTimeZone);
            t.setToNow();
            long extras = CalendarController.EXTRA_GOTO_TODAY;
            CalendarController controller = CalendarController.getInstance(mContext);    
            controller.sendEvent(SearchAgendaFragment.this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
		}		
	};
	
	OnClickListener mCalendarClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			           
            mController.sendEvent(SearchAgendaFragment.this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                    0, 0);
		}		
	};
	*/
    
    RelativeLayout mFrameLayout;
    ViewSwitcher mFrameLayoutViewSwitcher;
    CalendarViewsLowerActionBar mLowerActionBar;
    //TextView mTodayTextView;
	//TextView mCalendarsView;
	//TextView mInboxView;
	RelativeLayout mAgendaViewLayout;
	LinearLayout mAgendaDayIndicator;
	
	TextView mResponseYes;
	TextView mResponseMaybe;
	TextView mResponseNo;
	TextView mDeleteTextView;
	
	CalendarViewsLowerActionBar mAgendaViewPsudoLowerActionBar; 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

    	if (INFO) Log.i(TAG, "onCreateView"); 
    	    	
        mFrameLayout = (RelativeLayout) inflater.inflate(R.layout.unified_agenda_fragment_layout, null);
        mFrameLayoutViewSwitcher = (ViewSwitcher) mFrameLayout.findViewById(R.id.agenda_fragment_switcher);       
        
        mLowerActionBar = (CalendarViewsLowerActionBar) mFrameLayout.findViewById(R.id.agendaview_lower_actionbar);
        mLowerActionBar.init(this, mTzId, mController, false);
        mLowerActionBar.setWhoName("agendaview_lower_actionbar");        
        
    	mResponseYes = (TextView)mLowerActionBar.findViewById(R.id.response_yes);
    	mResponseMaybe = (TextView)mLowerActionBar.findViewById(R.id.response_maybe);
    	mResponseNo = (TextView)mLowerActionBar.findViewById(R.id.response_no); 
    	mDeleteTextView = (TextView)mLowerActionBar.findViewById(R.id.delete_menu);     	 	
    	
    	// agenda_fragment_switcher    	
    	mAgendaViewLayout = (RelativeLayout) inflater.inflate(R.layout.agendaview_layout, null);      
    	mAgendaDayIndicator = (LinearLayout) mAgendaViewLayout.findViewById(R.id.agenda_day_indicator_container);      	   	
    	makeAgendaDayIndicators();  
    	mAgendaDayIndicator.setVisibility(View.GONE);
        mAgendaListView = (AgendaListView)mAgendaViewLayout.findViewById(R.id.agenda_events_list);          
        
        mAgendaViewPsudoLowerActionBar = (CalendarViewsLowerActionBar)mAgendaViewLayout.findViewById(R.id.agendaview_psudo_lower_actionbar);
        mAgendaViewPsudoLowerActionBar.init(this, mTzId, mController, true);
        mAgendaViewPsudoLowerActionBar.setWhoName("agendaview_psudo_lower_actionbar");
        
        mFrameLayoutViewSwitcher.addView(mAgendaViewLayout);
        
        mEventInfoFragment.onCreateView(inflater, container, savedInstanceState, 
        		mFrameLayoutViewSwitcher, 
        		mAgendaViewLayout,
        		mSearchUpperActionBar,
        		false, null,
        		mLowerActionBar, mAgendaViewPsudoLowerActionBar,
        		mTzId);
        
        
        if (savedInstanceState != null) {        	
            //long instanceId = savedInstanceState.getLong(BUNDLE_KEY_RESTORE_INSTANCE_ID, -1);
            //if (instanceId != -1) {
                //mAgendaListView.setSelectedInstanceId(instanceId);
            //}            
        }       
        
        return mFrameLayout;
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);    	
    	
    	mAdapter = mAgendaListView.setUpList(this, true);
    	
    	mAgendaListView.setOnScrollListener(this);
	}
    
    // Called when the Fragment is visible to the user. 
    // This is generally tied to Activity.onStart of the containing Activity's lifecycle.
    
    @Override
	public void onStart() {
    	super.onStart();
    	if (INFO) Log.i(TAG, "onStart");    		
	}

    long mGotBothEndsEventFlag = 0;
	@Override
    public void onResume() {
        super.onResume();
        
        if (INFO) Log.i(TAG, "OnResume");
        
        mGotBothEndsEventFlag = 0;
        
        mEventInfoLoader.startBackgroundThread();
        mEventLoader.startBackgroundThread();
        
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(
                getActivity());
        boolean hideDeclined = prefs.getBoolean(
                SettingsFragment.KEY_HIDE_DECLINED, false);

        mAgendaListView.setHideDeclinedEvents(hideDeclined);        
        
        mAgendaListView.onResume();

//        // Register for Intent broadcasts
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_TIME_CHANGED);
//        filter.addAction(Intent.ACTION_DATE_CHANGED);
//        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
//        registerReceiver(mIntentReceiver, filter);
//
//        mContentResolver.registerContentObserver(Events.CONTENT_URI, true, mObserver);
    }

	@Override
    public void onPause() {
        super.onPause();
        
        if (INFO) Log.i(TAG, "onPause");

        mEventInfoLoader.stopBackgroundThread();
        mEventLoader.stopBackgroundThread();        
        
        mAgendaListView.onPause();

//        mContentResolver.unregisterContentObserver(mObserver);
//        unregisterReceiver(mIntentReceiver);

        // Record Agenda View as the (new) default detailed view.
//        Utils.setDefaultView(this, CalendarApplication.AGENDA_VIEW_ID);
    }
	
	
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (INFO) Log.i(TAG, "onSaveInstanceState");
        
        if (mAgendaListView == null) {
            return;
        }
        if (mShowEventDetailsWithAgenda) {
            long timeToSave;
            if (mLastHandledEventTime != null) {
                timeToSave = mLastHandledEventTime.getTimeInMillis();
                mTime.setTimeInMillis(mLastHandledEventTime.getTimeInMillis());
            } else {
                timeToSave =  System.currentTimeMillis();
                mTime.setTimeInMillis(timeToSave);
            }
            outState.putLong(BUNDLE_KEY_RESTORE_TIME, timeToSave);
            mController.setTime(timeToSave);
        } else {
            AgendaWindowAdapter.AgendaItem item = mAgendaListView.getFirstVisibleAgendaItem();
            if (item != null) {
                long firstVisibleTime = mAgendaListView.getFirstVisibleTime(item);
                if (firstVisibleTime > 0) {
                    mTime.setTimeInMillis(firstVisibleTime);
                    mController.setTime(firstVisibleTime);
                    outState.putLong(BUNDLE_KEY_RESTORE_TIME, firstVisibleTime);
                }
                // Tell AllInOne the event id of the first visible event in the list. The id will be
                // used in the GOTO when AllInOne is restored so that Agenda Fragment can select a
                // specific event and not just the time.
                mLastShownEventId = item.id;
            }
        }      

        /*
        long selectedInstance = mAgendaListView.getSelectedInstanceId();
        if (selectedInstance >= 0) {
            outState.putLong(BUNDLE_KEY_RESTORE_INSTANCE_ID, selectedInstance);
        }
        */
    }
       

    @Override
	public void onStop() {
    	if (INFO) Log.i(TAG, "onStop");
    	
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

	/**
     * This cleans up the event info fragment since the FragmentManager doesn't
     * handle nested fragments. Without this, the action bar buttons added by
     * the info fragment can come back on a rotation.
     *
     * @param fragmentManager
     */
    public void removeFragments(FragmentManager fragmentManager) {
        if (getActivity().isFinishing()) {
            return;
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment f = fragmentManager.findFragmentById(R.id.agenda_event_info);
        if (f != null) {
            ft.remove(f);
        }
        ft.commit();
    }   
    
    //LinearLayout mAgendaPreviousDayView;
    LinearLayout mAgendaCurrentDayView;
    LinearLayout mAgendaNextDayView;
    int mEventDayHeaderHeight;
    int mEventItemHeight;
    int mEventDayHeaderBottomLineHeight;
    int mEventDayHeaderDateViewHeight;
    public void makeAgendaDayIndicators() {
    	mEventDayHeaderBottomLineHeight = (int) getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
    	   	
    	int deviceHeight = getResources().getDisplayMetrics().heightPixels;
        int StatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
        if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
        int upperActionBarHeight = (int)getResources().getDimension(R.dimen.search_view_upper_actionbar_height);    	
        int mDayHeaderHeight = (int)getResources().getDimension(R.dimen.day_header_height);
        int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);

        int anticipationMonthListViewHeight = deviceHeight - StatusBarHeight - upperActionBarHeight - mDayHeaderHeight - lowerActionBarHeight;   
        int monthListViewNormalWeekHeight = (int) (anticipationMonthListViewHeight * MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT);
        int monthListViewNormalWeekHeightInEPMode = (int) (monthListViewNormalWeekHeight * MonthListView.EEVM_WEEK_HEIGHT_RATIO_BY_NM_WEEK_HEIGHT);            
        mEventItemHeight = monthListViewNormalWeekHeightInEPMode; 
        mEventDayHeaderHeight = (int) (mEventItemHeight * AgendaWindowAdapter.EVENT_DAY_HEADER_HEIGHT_RATIO);
    	    	
    	mEventDayHeaderDateViewHeight = mEventDayHeaderHeight - mEventDayHeaderBottomLineHeight;    	
    	
    	LayoutParams containerParams = (LayoutParams) mAgendaDayIndicator.getLayoutParams();
    	containerParams.height = mEventDayHeaderHeight;
    	mAgendaDayIndicator.setLayoutParams(containerParams);    	
    	
    	LayoutInflater inflater = (LayoutInflater)mContext.getSystemService
    		      (Context.LAYOUT_INFLATER_SERVICE);    	
    	
        mAgendaCurrentDayView = (LinearLayout) inflater.inflate(R.layout.agenda_day_header, null, false);
        LinearLayout.LayoutParams curDayParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, mEventDayHeaderHeight);
        mAgendaCurrentDayView.setLayoutParams(curDayParams);
        
        TextView curDateView = (TextView) mAgendaCurrentDayView.findViewById(R.id.date);
        // agenda_day�� dateó�� width�� WRAP_CONTENT�� ����ؼ��� �ȵǴ� ������
        // onScroll���� �����Ǵ� text size�� width�� ��Ȱ�ϰ� �������� �ʱ� ������ �� ���� 
        LinearLayout.LayoutParams curDateViewParams = (LinearLayout.LayoutParams) curDateView.getLayoutParams();
        curDateViewParams.height = mEventDayHeaderDateViewHeight;        
        curDateView.setLayoutParams(curDateViewParams);        
        View curUnderLine = mAgendaCurrentDayView.findViewById(R.id.bottom_divider_simple);
        LinearLayout.LayoutParams curUnderLineParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, mEventDayHeaderBottomLineHeight);
        curUnderLine.setLayoutParams(curUnderLineParams);
        
        mAgendaCurrentDayView.setVisibility(View.VISIBLE);        
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        mAgendaNextDayView = (LinearLayout) inflater.inflate(R.layout.agenda_day_header, null, false);
        LinearLayout.LayoutParams nextDayParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, mEventDayHeaderHeight);
        mAgendaNextDayView.setLayoutParams(nextDayParams);
        
        TextView nextDateView = (TextView) mAgendaNextDayView.findViewById(R.id.date);
        LinearLayout.LayoutParams nextDateViewParams = (LinearLayout.LayoutParams) nextDateView.getLayoutParams();
        nextDateViewParams.height = mEventDayHeaderDateViewHeight;                
        nextDateView.setLayoutParams(nextDateViewParams);        
        View nextUnderLine = mAgendaNextDayView.findViewById(R.id.bottom_divider_simple);
        LinearLayout.LayoutParams nextUnderLineParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, mEventDayHeaderBottomLineHeight);
        nextUnderLine.setLayoutParams(nextUnderLineParams);
        
        mAgendaNextDayView.setVisibility(View.GONE);
        
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //mAgendaDayIndicator.addView(mAgendaPreviousDayView);
        mAgendaDayIndicator.addView(mAgendaCurrentDayView);
        mAgendaDayIndicator.addView(mAgendaNextDayView);
        
        //mAgendaDayIndicator.setBackgroundColor(Color.RED); // for test
        if (INFO) Log.i(TAG, "-makeAgendaDayIndicators");
    }
    
    private void goTo(EventInfo event, boolean animate) {
        if (event.selectedTime != null) {
            mTime.setTimeInMillis(event.selectedTime.getTimeInMillis());
        } else if (event.startTime != null) {
            mTime.setTimeInMillis(event.startTime.getTimeInMillis());
        }
        if (mAgendaListView == null) {
            // The view hasn't been set yet. Just save the time and use it
            // later.
            return;
        }
        mAgendaListView.goTo(mTime, event.id, mQuery, false,
                ((event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0  &&
                        mShowEventDetailsWithAgenda) ? true : false);
        /*
        AgendaAdapter.ViewHolder vh = mAgendaListView.getSelectedViewHolder();
        // Make sure that on the first time the event info is shown to recreate it
        if (INFO) Log.i(TAG, "selected viewholder is null: " + (vh == null));
        showEventInfo(event, vh != null ? vh.allDay : false, mForceReplace);
        */
        mForceReplace = false;
    }

    private void search(String query, Calendar time) {
        mQuery = query;
        if (time != null) {
            mTime.setTimeInMillis(time.getTimeInMillis());
        }
        if (mAgendaListView == null) {
            // The view hasn't been set yet. Just return.
            return;
        }
        mAgendaListView.goTo(time, -1, mQuery, true, false);
    }
    
    private void makeEmptyListView() {
       
        if (mAgendaListView == null) {
            // The view hasn't been set yet. Just return.
            return;
        }
        
        mAgendaListView.setEmptyListViewState();
    }

    //mGotBothEndsEventFlag
    public void getBothEndsEvent(EventInfo eventInfo) {
    	if ((eventInfo.extraLong & CalendarController.EXTRA_BOTHENDS_FOREMOST_EVENT) != 0) {  
    		if (INFO) Log.i(TAG, "getBothEndsEvent : EXTRA_BOTHENDS_FOREMOST_EVENT");
    		
    		Event event = mApp.getBothEndsEvent(BothEndsEventLoader.FOREMOST_EVENT_LOADER);    		
    		
    		mForemostEvent = new Event();   
	    	
	    	mForemostEvent.id = event.id;            	
	    	mForemostEvent.startMillis = event.startMillis;
	    	mForemostEvent.endMillis = event.endMillis;
	    	mForemostEvent.startDay = event.startDay;
	    	mForemostEvent.endDay = event.endDay;                    
	                
	    	mForemostEvent.title = event.title;
	    	mForemostEvent.location = event.location;
	    	mForemostEvent.allDay = event.allDay;	 
	    	
	    	mGotBothEndsEventFlag = mGotBothEndsEventFlag | GOT_BOTHENDS_EVENT_TYPE.GOT_FOREMOST_EVENT;
	    	
	    	//printBothEndsEventContext(BothEndsEventLoader.FOREMOST_EVENT_LOADER);    	
        	
    	} else if ((eventInfo.extraLong & CalendarController.EXTRA_BOTHENDS_BACKMOST_EVENT) != 0) {  
    		if (INFO) Log.i(TAG, "getBothEndsEvent : EXTRA_BOTHENDS_BACKMOST_EVENT");
    		
    		Event event = mApp.getBothEndsEvent(BothEndsEventLoader.BACKMOST_EVENT_LOADER);    
    		
    		mBackmostEvent = new Event();
	    	
    		mBackmostEvent.id = event.id;            	
    		mBackmostEvent.startMillis = event.startMillis;
    		mBackmostEvent.endMillis = event.endMillis;
    		mBackmostEvent.startDay = event.startDay;
    		mBackmostEvent.endDay = event.endDay;                    
	                
    		mBackmostEvent.title = event.title;
    		mBackmostEvent.location = event.location;
    		mBackmostEvent.allDay = event.allDay;    
    		
    		mGotBothEndsEventFlag = mGotBothEndsEventFlag | GOT_BOTHENDS_EVENT_TYPE.GOT_BACKMOST_EVENT;
    		
    		//printBothEndsEventContext(BothEndsEventLoader.BACKMOST_EVENT_LOADER);
    	}
    	else {
    		if (INFO) Log.i(TAG, "getBothEndsEvent??? Oooops!!!");
    		return ;
    	}
    	
    	// ���⼭ ��� both ends event�� �����ߴ����� Ȯ���ؾ� �Ѵ�
    	if (mGotBothEndsEventFlag == GOT_BOTHENDS_EVENT_TYPE.GOT_ALL_BOTHENDS_EVENT) {
    		// ���⼭ holding�� ��� ���¸� Ǯ����� �Ѵ�!!!
    		if (INFO) Log.i(TAG, "getBothEndsEvent : GOT_ALL_BOTHENDS_EVENT !!!!!!!!!!!!!!!!");
    		
    		// ���⼭ agendawindowadapter�� �˷��� �Ѵ�
    		mAdapter.setForemostEventYear(mForemostEvent.startDay);
    		mAdapter.setBackmostEventYear(mBackmostEvent.endDay);
    		   		
    		mController.sendEvent(this, EventType.GOT_ALL_BOTHENDS_EVENTS, null, null, -1, ViewType.CURRENT);   		  		
    				
    	}
    }    
    
    
    @Override
    public void eventsChanged() {
        if (mAgendaListView != null) {
            mAgendaListView.refresh(true);
        }
    }
    
    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.EVENTS_CHANGED | EventType.BOTHENDS_EVENTS_QUERY_COMPLETION |
        		EventType.SEARCH | EventType.SEARCH_LISTVIEW_EMPTY;
    }

    private long mLastHandledEventId = -1;
    private Calendar mLastHandledEventTime = null;
    @Override
    public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.GO_TO) {
            // TODO support a range of time
            // TODO support event_id
            // TODO figure out the animate bit
        	if (INFO) Log.i(TAG, "handleEvent : EventType.GO_TO");
        	
            mLastHandledEventId = event.id;
            mLastHandledEventTime =
                    (event.selectedTime != null) ? event.selectedTime : event.startTime;
            goTo(event, true);
        } else if (event.eventType == EventType.SEARCH) {
        	if (INFO) Log.i(TAG, "handleEvent : EventType.SEARCH");
            search(event.query, event.startTime);
        } else if (event.eventType == EventType.SEARCH_LISTVIEW_EMPTY) {
        	if (INFO) Log.i(TAG, "handleEvent : EventType.SEARCH_LISTVIEW_EMPTY");
        	makeEmptyListView();
        } else if (event.eventType == EventType.EVENTS_CHANGED) {
        	if (INFO) Log.i(TAG, "handleEvent : EventType.EVENTS_CHANGED");
            eventsChanged();
        } else if (event.eventType == EventType.BOTHENDS_EVENTS_QUERY_COMPLETION) {        	
        	getBothEndsEvent(event);
        }
        
    }

    public long getLastShowEventId() {
        return mLastShownEventId;
    }

    // Shows the selected event in the Agenda view
    private void showEventInfo(EventInfo event, boolean allDay, boolean replaceFragment) {

        // Ignore unknown events
        if (event.id == -1) {
            if (INFO) Log.i(TAG, "showEventInfo, event ID = " + event.id);
            return;
        }

        mLastShownEventId = event.id;

        // Create a fragment to show the event to the side of the agenda list
        /*if (mShowEventDetailsWithAgenda) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                // Got a goto event before the fragment finished attaching,
                // stash the event and handle it later.
                mOnAttachedInfo = event;
                mOnAttachAllDay = allDay;
                return;
            }
            FragmentTransaction ft = fragmentManager.beginTransaction();

            if (allDay) {
                event.startTime.timezone = Time.TIMEZONE_UTC;
                event.endTime.timezone = Time.TIMEZONE_UTC;
            }

            if (INFO) {
                if (INFO) Log.i(TAG, "***");
                if (INFO) Log.i(TAG, "showEventInfo: start: " + new Date(event.startTime.toMillis(true)));
                if (INFO) Log.i(TAG, "showEventInfo: end: " + new Date(event.endTime.toMillis(true)));
                if (INFO) Log.i(TAG, "showEventInfo: all day: " + allDay);
                if (INFO) Log.i(TAG, "***");
            }

            long startMillis = event.startTime.toMillis(true);
            long endMillis = event.endTime.toMillis(true);
            EventInfoFragment fOld =
                    (EventInfoFragment)fragmentManager.findFragmentById(R.id.agenda_event_info);
            if (fOld == null || replaceFragment || fOld.getStartMillis() != startMillis ||
                    fOld.getEndMillis() != endMillis || fOld.getEventId() != event.id) {
                mEventFragment = new EventInfoFragment(mActivity, event.id,
                        startMillis, endMillis,
                        Attendees.ATTENDEE_STATUS_NONE, false,
                        EventInfoFragment.DIALOG_WINDOW_STYLE, null);
                ft.replace(R.id.agenda_event_info, mEventFragment);
                ft.commit();
            } else {
                fOld.reloadEvents();
            }
        }*/
    }

    // OnScrollListener implementation to update the date on the pull-down menu of the app
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Save scroll state so that the adapter can stop the scroll when the
        // agenda list is fling state and it needs to set the agenda list to a new position
    	
    	switch(scrollState) {
    	case OnScrollListener.SCROLL_STATE_IDLE:
    		if (INFO) Log.i(TAG, "SearchAgendaFragment::onScrollStateChanged: IDLE************");
    		View firstChild = view.getChildAt(0);
        	if (firstChild == null) {
        		if (INFO) Log.i(TAG, "SearchAgendaFragment::onScrollStateChanged: child null");
                return;
            }           	
        	int firstVisiblePos = view.getFirstVisiblePosition();
        	int lastVisiblePos = view.getLastVisiblePosition();
    		getVisibleDetailEventsInfo(firstVisiblePos, lastVisiblePos);	
    		
    		break;
    	case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
    		if (INFO) Log.i(TAG, "SearchAgendaFragment::onScrollStateChanged: SCROLL");
    		break;
    	case OnScrollListener.SCROLL_STATE_FLING:
    		if (INFO) Log.i(TAG, "SearchAgendaFragment::onScrollStateChanged: FLING");
    		break;
    	}
    	
    	
        if (mAdapter != null) {
            mAdapter.setScrollState(scrollState);
        }
    }

    
    private ArrayList<Event> mEventsInVisibleRegion = new ArrayList<Event>();
    // Gets the time of the first visible view. If it is a new time, send a message to update
    // the time on the ActionBar
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    	if (INFO) Log.i(TAG, "onScroll");
    	
    	View firstChild = view.getChildAt(0);
    	if (firstChild == null) {
    		if (INFO) Log.i(TAG, "onScroll: child null");
            return;
        }       
    	    	
        int julianDay = mAgendaListView.getJulianDayFromPosition(firstVisibleItem
                - mAgendaListView.getHeaderViewsCount());
        // On error - leave the old view
        if (julianDay == 0) {
        	if (INFO) Log.i(TAG, "onScroll : julianDay == 0");
            return;
        }
        
        
        int firstVisiblePos = view.getFirstVisiblePosition();
    	int lastVisiblePos = view.getLastVisiblePosition();
    	
    	// SCROLL_STATE_IDLE ��Ȳ������ ó���ؾ� �ϴ°�????
    	translateAgendaDayHeader(view, firstChild, firstVisiblePos);
    	    	
    	//if (INFO) Log.i(TAG, "onScroll : day header text=" + dayHeaderDate);
    	
        //boolean idleStatus = false;
        int scrollState = mAdapter.getScrollState();
        if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {        	
        	mAdapter.queryUnderScrollingState(true, firstVisiblePos,lastVisiblePos);
        }
        else {
        	// ���� ó������ ȣ��Ǵ°�????
        	if (INFO) Log.i(TAG, "onScroll : SCROLL_STATE_IDLE");
        	//getVisibleDetailEventsInfo(firstVisiblePos, lastVisiblePos);        	
        }
        
        /*
    	switch(scrollState) {
    	case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
    		if (INFO) Log.i(TAG, "SearchAgendaFragment::onScroll: SCROLL");
    		break;
    	case OnScrollListener.SCROLL_STATE_FLING:
    		if (INFO) Log.i(TAG, "SearchAgendaFragment::onScroll: FLING");
    		break;
    	}
        */    	     
    	
    	//mAdapter.queryMachine(firstVisiblePos,lastVisiblePos);
        
        // If the day changed, update the ActionBar
        if (mJulianDayOnTop != julianDay) {
            mJulianDayOnTop = julianDay;
            Calendar t = GregorianCalendar.getInstance(mTimeZone);   
            long julianDayOnTopMillis = ETime.getMillisFromJulianDay(mJulianDayOnTop, mTimeZone, mFirstDayOfWeek);
            t.setTimeInMillis(julianDayOnTopMillis);//t.setJulianDay(mJulianDayOnTop);
            mController.setTime(t.getTimeInMillis());
            // Cannot sent a message that eventually may change the layout of the views
            // so instead post a runnable that will run when the layout is done
            if (!mIsTabletConfig) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                    	Calendar t = GregorianCalendar.getInstance(mTimeZone);   
                    	
                    	long julianDayOnTopMillis = ETime.getMillisFromJulianDay(mJulianDayOnTop, mTimeZone, mFirstDayOfWeek);
                    	t.setTimeInMillis(julianDayOnTopMillis);//t.setJulianDay(mJulianDayOnTop);
                        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, null, -1,
                                ViewType.CURRENT, 0, null, null);
                    }
                });
            }
        }
    }
    
    boolean mIsScrollingUp = false;
    int mPreviousFirstVisiblePosition;
    int mPreviousFirstChildBottom;
    
    public void setAgendaDayIndicatorVisibleState(boolean visible) {
    	if (visible) {
    		mAgendaDayIndicator.setVisibility(View.VISIBLE);
    	}
    	else {
    		mAgendaDayIndicator.setVisibility(View.GONE);
    	}
    		
    }
    
    public void translateAgendaDayHeader (AbsListView listView, View child, int firstVisiblePos) {
    	//if (INFO) Log.i(TAG, "***************************************");
    	int currFirstChildTop = child.getTop();
    	int currFirstChildBottom = child.getBottom();
                        
    	RowInfo firstRowInfo = mAdapter.getRowInfo(firstVisiblePos);    
    	int currentDayHeaderJulianDay = firstRowInfo.mDay;
    	String currentDayHeaderDate = makeAgendaDayHeaderText(currentDayHeaderJulianDay, true);
    	TextView currentDHDateView = (TextView) mAgendaCurrentDayView.findViewById(R.id.date);
    	if (firstRowInfo.mTodayDayType) {
    		currentDHDateView.setTextColor(Color.RED);
        }
        else {
        	int color = mContext.getResources().getColor(R.color.agenda_item_standard_color);
        	currentDHDateView.setTextColor(color);
        }
    	currentDHDateView.setText(currentDayHeaderDate);
    	
        if (firstRowInfo.mType == AgendaByDayAdapter.TYPE_DAY) {        	
        	
    		int nextVisiblePos = firstVisiblePos + 1;
    		RowInfo secondRowInfo = mAdapter.getRowInfo(nextVisiblePos);  
    		if (secondRowInfo != null) {
	    		// day header ������ �� �ٷ� newer day header�� �ִٴ� ���� ���� �پ� �ִٴ� ���� �ǹ��Ѵ�
	    		if (secondRowInfo.mType == AgendaByDayAdapter.TYPE_DAY) {        			        			
	    			setDayHeaderViewsTopBottom(currFirstChildTop, currFirstChildBottom);
	    			setNextDayHeaderView(secondRowInfo);     			
	    		} 
    		}
        }
        else if (firstRowInfo.mType == AgendaByDayAdapter.TYPE_MEETING) {        	
        	
    		int nextVisiblePos = firstVisiblePos + 1;
    		RowInfo secondRowInfo = mAdapter.getRowInfo(nextVisiblePos);   
    		if (secondRowInfo != null) {
	    		if (secondRowInfo.mType == AgendaByDayAdapter.TYPE_DAY) {
	    			View nextChildView = listView.getChildAt(1);
	    			
	    			if (nextChildView.getTop() < mEventDayHeaderHeight) {			
	        			int currentDayViewTop = currFirstChildBottom - mEventDayHeaderHeight;            			
	        			setDayHeaderViewsTopBottom(currentDayViewTop, currFirstChildBottom);
	        			setNextDayHeaderView(secondRowInfo);            			
	    			}        			
	    		} 
    		}
        }       
    	
    	mAgendaCurrentDayView.setTag(firstRowInfo.mDay);
    	//if (INFO) Log.i(TAG, "***************************************");
    }
    
    public void setDayHeaderViewsTopBottom(int currentDayHeaderViewTop, int nextDayHeaderViewTop) {
    	mAgendaCurrentDayView.setTop(currentDayHeaderViewTop);
    	mAgendaCurrentDayView.setBottom(currentDayHeaderViewTop + mEventDayHeaderHeight);
		mAgendaNextDayView.setTop(nextDayHeaderViewTop);
		mAgendaNextDayView.setBottom(nextDayHeaderViewTop + mEventDayHeaderHeight);
    }
    
    public void setNextDayHeaderView(RowInfo secondRowInfo) {
    	//mAgendaPreviousDayView.setVisibility(View.GONE);
    	if (mAgendaNextDayView.getVisibility() != View.VISIBLE)
    		mAgendaNextDayView.setVisibility(View.VISIBLE);
		
    	int nextDayHeaderJulianDay = secondRowInfo.mDay;
    	String nextDayHeaderDate = makeAgendaDayHeaderText(nextDayHeaderJulianDay, true);
    	TextView nextDHDateView = (TextView) mAgendaNextDayView.findViewById(R.id.date);
    	if (secondRowInfo.mTodayDayType) {
    		nextDHDateView.setTextColor(Color.RED);
        }
        else {
        	int color = mContext.getResources().getColor(R.color.agenda_item_standard_color);
        	nextDHDateView.setTextColor(color);
        }
    	nextDHDateView.setText(nextDayHeaderDate);
    }
    
    private final Formatter mAgendaDayHeaderFormatter;
    private final StringBuilder mAgendaDayHeaderStringBuilder;    
    public String makeAgendaDayHeaderText(int julianDay, boolean makeDayOfWeek) {
    	Calendar date = GregorianCalendar.getInstance(mTimeZone);    
    	//long millis = date.setJulianDay(julianDay);     
    	
    	long millis = ETime.getMillisFromJulianDay(julianDay, mTimeZone, mFirstDayOfWeek);
    	date.setTimeInMillis(millis);

        mAgendaDayHeaderStringBuilder.setLength(0);
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_YEAR;
        String dateViewText = ETime.formatDateTimeRange(mContext, mAgendaDayHeaderFormatter, millis, millis,
                flags, mTzId).toString();  
        
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
    
    ArrayList<EventCursors> mEventCursors = new ArrayList<EventCursors>();
    Runnable mEventInfoLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	boolean empty = mEventCursors.isEmpty();
        	if (empty)
        		if (INFO) Log.i(TAG, "mEventInfoLoaderSuccessCallback:what???");
        	else {
        		if (INFO) Log.i(TAG, "mEventInfoLoaderSuccessCallback:SUCCESS");        		
        	}
        }
    };
    
    private final Runnable mEventInfoLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	if (INFO) Log.i(TAG, "Called EventInfoLoaderCancelCallback");
        }
    };    
    
    public void getVisibleDetailEventsInfo(int firstVisiblePos, int lastVisiblePos) {
    	if (INFO) Log.i(TAG, "+getVisibleDetailEventsInfo");
    	// ���⼭ �츮�� event info ������ �����ؾ� �Ѵ�
    	// visible ������ �ִ� �� meeting�� ���� cursor ������ �����ؾ� �Ѵ�
    	//int lastChildViewIndex = lastVisiblePos - firstVisiblePos;
    	mEventsInVisibleRegion.clear();
    	        	
    	int end = lastVisiblePos + 1;
    	
    	for (int i=firstVisiblePos; i<end; i++) {
    		DayAdapterInfo info = mAdapter.getAdapterInfoByPosition(i);
    		int curPos = i - info.offset;
        	RowInfo row = info.dayAdapter.getRowInfo(curPos);
        	        	        		
    		if (row.mType == AgendaByDayAdapter.TYPE_MEETING) { 
    			info.cursor.moveToPosition(row.mPosition);
    			Event e = generateEventFromCursor(info.cursor);
    			mEventsInVisibleRegion.add(e);
        	}
    	}
    	
    	// ���� ������ ������ ��������
    	mEventCursors.clear();
    	mEventInfoLoader.loadEventsInfoInBackground(mEventCursors, mEventsInVisibleRegion, mEventInfoLoaderSuccessCallback, mEventInfoLoaderCancelCallback);
    	
    	if (INFO) Log.i(TAG, "-getVisibleDetailEventsInfo");
    }
    
    public EventCursors getEventCursors(long eventId) {
    	int size = mEventCursors.size();
    	EventCursors targetObj = null;
    	for (int i=0; i<size; i++) {
    		EventCursors obj = mEventCursors.get(i);
    		if (eventId == obj.mEventId) {
    			targetObj = obj;
    			break;
    		}                			
    	}
    	
    	return targetObj;
    }
    
    int mAnticipationAgendaListViewWidth;
    int mAnticipationAgendaListViewHeight;
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
    	int upperActionBarHeight = (int)getResources().getDimension(R.dimen.search_view_upper_actionbar_height);    	
    	int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);
    	
    	mAnticipationAgendaListViewWidth = rectgle.right - rectgle.left;
    	mAnticipationAgendaListViewHeight = deviceHeight - StatusBarHeight - upperActionBarHeight - lowerActionBarHeight;   	    
    }
    
    public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
    
	// �ش� event�� date text�� ��� �;� �Ѵ�
	public void launchFastEventInfoView(long eventId, int startDay, long startMillis, long endMillis) {		
		if (INFO) Log.i(TAG, "+launchFastEventInfoView");
		
		mSearchUpperActionBar.setSwitchViewState();
		
		EventCursors targetObj = getEventCursors(eventId);	
		
		mEventInfoFragment.launchFastEventInfoView(ViewType.SEARCH, eventId, startDay, startMillis, endMillis, targetObj);
		
    	if (INFO) Log.i(TAG, "-launchFastEventInfoView");    
	}
	
	
    public static final int ALPHA_OPAQUE_TO_TRANSPARENT = 1;
	public static final int ALPHA_TRANSPARENT_TO_OPAQUE = 2;
	public static final int ANIMATION_INTERPOLATORTYPE_ACCELERATE = 1;
	public static final int ANIMATION_INTERPOLATORTYPE_DECELERATE = 2;
	public AlphaAnimation makeAlphaAnimation(long duration, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {    	
	 	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);      
        
        alphaAnimation.setDuration(duration);
        if (InterpolatorType == ANIMATION_INTERPOLATORTYPE_ACCELERATE)
        	alphaAnimation.setInterpolator(new AccelerateInterpolator());
        else
        	alphaAnimation.setInterpolator(new DecelerateInterpolator());
        
        return alphaAnimation;        
	}	
	
	private static final int MINIMUM_SNAP_VELOCITY = 2200;    
    public static final float SWITCH_SUB_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.25f;
    public static final float SWITCH_MAIN_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f;   
    public static final float SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY = SWITCH_MAIN_PAGE_VELOCITY * 2;   

    
    
        
    
    
    
    private Event generateEventFromCursor(Cursor cursor) {
        Event e = new Event();

        e.id = cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID);
        e.title = cursor.getString(AgendaWindowAdapter.INDEX_TITLE);
        e.location = cursor.getString(AgendaWindowAdapter.INDEX_EVENT_LOCATION);
        e.allDay = cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
        e.organizer = cursor.getString(AgendaWindowAdapter.INDEX_ORGANIZER);
        e.guestsCanModify = cursor.getInt(AgendaWindowAdapter.INDEX_GUESTS_CAN_MODIFY) != 0;

        if (e.title == null || e.title.length() == 0) {
            e.title = mNoTitleString;
        }

        if (!cursor.isNull(AgendaWindowAdapter.INDEX_COLOR)) {
            // Read the color from the database
            e.color = Utils.getDisplayColorFromColor(cursor.getInt(AgendaWindowAdapter.INDEX_COLOR));
        } else {
            e.color = mNoColorColor;
        }

        long eStart = cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
        long eEnd = cursor.getLong(AgendaWindowAdapter.INDEX_END);

        e.startMillis = eStart;
        e.startTime = cursor.getInt(AgendaWindowAdapter.INDEX_START_MINUTE);
        e.startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);

        e.endMillis = eEnd;
        e.endTime = cursor.getInt(AgendaWindowAdapter.INDEX_END_MINUTE);
        e.endDay = cursor.getInt(AgendaWindowAdapter.INDEX_END_DAY);

        e.hasAlarm = cursor.getInt(AgendaWindowAdapter.INDEX_HAS_ALARM) != 0;

        // Check if this is a repeating event
        String rrule = cursor.getString(AgendaWindowAdapter.INDEX_RRULE);
        String rdate = cursor.getString(AgendaWindowAdapter.INDEX_RDATE); // �߰��ϱ�� ������
        if (!TextUtils.isEmpty(rrule) || !TextUtils.isEmpty(rdate)) {
            e.isRepeating = true;
        } else {
            e.isRepeating = false;
        }

        e.selfAttendeeStatus = cursor.getInt(AgendaWindowAdapter.INDEX_SELF_ATTENDEE_STATUS);
        return e;
    }
    
    /*
    public void printBothEndsEventContext(int type) {
    	
    	if (INFO) Log.i(TAG, "**********************************************************");
    	
    	Time startTime = new Time(mTimeZone);
    	Time endTime = new Time(mTimeZone);
    	Time startJualianDayTime = new Time(mTimeZone);
    	Time endJualianDayTime = new Time(mTimeZone);
    	
    	if (type == BothEndsEventLoader.FOREMOST_EVENT_LOADER) {    		
	    	if (INFO) Log.i(TAG, "event id=" + String.valueOf(mForemostEvent.id));
	    	if (INFO) Log.i(TAG, "event title=" + mForemostEvent.title);
	    	if (INFO) Log.i(TAG, "event loaction=" + mForemostEvent.location);
	    	if (INFO) Log.i(TAG, "event allDay=" + String.valueOf(mForemostEvent.allDay));     	
	    	
	    	startTime.set(mForemostEvent.startMillis);
	    	if (INFO) Log.i(TAG, "event begin time=" + startTime.format2445());	    	
	    	
	    	endTime.set(mForemostEvent.endMillis);
	    	if (INFO) Log.i(TAG, "event end time=" + endTime.format2445());	    	
	    	
	    	startJualianDayTime.setJulianDay(mForemostEvent.startDay);
	    	if (INFO) Log.i(TAG, "event start julianDay=" + startJualianDayTime.format3339(true));	    	
	    	
	    	endJualianDayTime.setJulianDay(mForemostEvent.endDay);
	    	if (INFO) Log.i(TAG, "event end julianDay=" + endJualianDayTime.format3339(true));
    	}
    	else if (type == BothEndsEventLoader.BACKMOST_EVENT_LOADER) {    		
	    	if (INFO) Log.i(TAG, "event id=" + String.valueOf(mBackmostEvent.id));
	    	if (INFO) Log.i(TAG, "event title=" + mBackmostEvent.title);
	    	if (INFO) Log.i(TAG, "event loaction=" + mBackmostEvent.location);
	    	if (INFO) Log.i(TAG, "event allDay=" + String.valueOf(mBackmostEvent.allDay));      	
	    	
	    	startTime.set(mBackmostEvent.startMillis);
	    	if (INFO) Log.i(TAG, "event begin time=" + startTime.format2445());	    	
	    	
	    	endTime.set(mBackmostEvent.endMillis);
	    	if (INFO) Log.i(TAG, "event end time=" + endTime.format2445());	    	
	    	
	    	startJualianDayTime.setJulianDay(mBackmostEvent.startDay);
	    	if (INFO) Log.i(TAG, "event start julianDay=" + startJualianDayTime.format3339(true));    	
	    	
	    	endJualianDayTime.setJulianDay(mBackmostEvent.endDay);
	    	if (INFO) Log.i(TAG, "event end julianDay=" + endJualianDayTime.format3339(true));
    	}
    	if (INFO) Log.i(TAG, "**********************************************************");
    }
    */
}
