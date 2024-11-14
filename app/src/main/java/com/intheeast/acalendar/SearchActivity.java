package com.intheeast.acalendar;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.agenda.SearchAgendaFragment;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.etc.ETime;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Events;
//import android.text.format.Time;
import android.util.Log;

public class SearchActivity extends Activity implements CalendarController.EventHandler{

	private static final String TAG = SearchActivity.class.getSimpleName();
    
    private static final boolean INFO = true;
    
    private static final int HANDLER_KEY = 0;

    SearchViewUpperActionBarFragment mUpperActionBarFrag;
    SearchTranslucentFragment mSearchTranslucentFragment;
    
    private CalendarController mController;
    
    private Handler mHandler;
    private BroadcastReceiver mTimeChangesReceiver;
    private ContentResolver mContentResolver;
        
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };
    
    // runs when a timezone was changed and updates the today icon
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
        	mTzId = Utils.getTimeZone(SearchActivity.this, mTimeChangesUpdater);
        	mTimeZone = TimeZone.getTimeZone(mTzId);
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTzId);            
        }
    };
    
    private String mTzId;   
    private TimeZone mTimeZone;
    int mFirstDayOfWeek;
    
    ECalendarApplication mApp;
    Context mContext;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);        
        
        mApp = (ECalendarApplication) getApplication();
        // �������� �ʱ�...
        // Activity�� Context�� the context of the single, global Application object of the current process �̴�
        // �׷��� fragment�� Context�� ���� �츮�� Activity�� �����ϰ� �մ�
        // �׷��Ƿ� CalendarController�� ������ �� getInstance�� parameter�� Activity�� �����ؾ� �Ѵ�
        // ������ Activity������ parameter�� getApplicationContext()�� �����ϰ�
        // Activity�� fragment������ parameter�� Activity�� �����Ѵٸ� ���������� event handler���� ���� �ʴ� sideEffect�� �߻�
        // �׷��Ƿ� �׻� �����ؾ� ��!!!
        mContext = getApplicationContext();
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        mController = CalendarController.getInstance(this);
        mHandler = new Handler();        
        
        setContentView(R.layout.search);
        
        mContentResolver = getContentResolver();        
        
        mController.setViewType(ViewType.SEARCH);
        
        // Must be the first to register because this activity can modify the
        // list of event handlers in it's handle method. This affects who the
        // rest of the handlers the controller dispatches to are.
        mController.registerEventHandler(HANDLER_KEY, this); 
        
        initFragments();      
        
        mForemostEventLoader = new BothEndsEventLoader(this, BothEndsEventLoader.FOREMOST_EVENT_LOADER);
        mBackmostEventLoader = new BothEndsEventLoader(this, BothEndsEventLoader.BACKMOST_EVENT_LOADER);
        
    }    
    
    
    BothEndsEventLoader mForemostEventLoader;
    BothEndsEventLoader mBackmostEventLoader;
    long mGMToff;
    @Override
	protected void onStart() {
		super.onStart();
		if (INFO) Log.i(TAG, "onStart");
	}

	@Override
    protected void onResume() {
        super.onResume();
        
        if (INFO) Log.i(TAG, "onResume");
        
        mTzId = Utils.getTimeZone(this, mTimeChangesUpdater);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGMToff = (mTimeZone.getRawOffset()) / 1000;
        
		Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTzId);        
		mTimeChangesReceiver = Utils.setTimeChangesReceiver(this, mTimeChangesUpdater);		
        
		issueBothEndsEventRequest();
        
        // Make sure the today icon is up to date
        invalidateOptionsMenu();
                
        mContentResolver.registerContentObserver(Events.CONTENT_URI, true, mObserver);
        // �� �̵� ����?????????????????????????????????????????????????????
        // We call this in case the user changed the time zone
        //eventsChanged();///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
	
	public void issueBothEndsEventRequest() {
		mForemostEventLoader.startBackgroundThread();
        mBackmostEventLoader.startBackgroundThread();
        
        Calendar startDayTime = GregorianCalendar.getInstance(mTimeZone);//Time startDayTime = new Time(mTimeZone);
        int startDay = ETime.getJulianDay(startDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        
        Calendar endDayTime = GregorianCalendar.getInstance(mTimeZone);//Time startDayTime = new Time(mTimeZone);
        endDayTime.set(2100, 0, 1);
        
        int endDay = ETime.getJulianDay(endDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        
        mForemostEventLoader.loadBothEndsEventsInBackground(startDay, endDay, mForemostEvent, mForemostEventLoaderSuccessCallback, mForemostEventLoaderCancelCallback);
        
        mBackmostEventLoader.loadBothEndsEventsInBackground(startDay, endDay, mBackmostEvent, mBackmostEventLoaderSuccessCallback, mBackmostEventLoaderCancelCallback);
	}

    @Override
    protected void onPause() {    
        
        if (INFO) Log.i(TAG, "onPause");
        
        mForemostEventLoader.stopBackgroundThread();
        mBackmostEventLoader.stopBackgroundThread();
        
        Utils.resetMidnightUpdater(mHandler, mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, mTimeChangesReceiver);
        mContentResolver.unregisterContentObserver(mObserver);
        
        super.onPause();
    }    
        
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);
    }      
    
    SearchAgendaFragment mSearchResultsFragment;
    private void initFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        mUpperActionBarFrag = new SearchViewUpperActionBarFragment(/*mController*/);  
        ft.replace(R.id.search_upper_actionbar, mUpperActionBarFrag);
        mController.registerEventHandler(R.id.search_upper_actionbar, mUpperActionBarFrag);
        
        // ���� ������ �ٲ������ �Ʒ� SearchTranslucentFragment�� ������� �ʴ´�
        /*mSearchTranslucentFragment = new SearchTranslucentFragment();
        ft.replace(R.id.search_main_pane, mSearchTranslucentFragment);*/
        mSearchResultsFragment = new SearchAgendaFragment(mController.getTime(), mUpperActionBarFrag);
        ft.replace(R.id.search_main_pane, mSearchResultsFragment);
        mController.registerEventHandler(R.id.search_main_pane, mSearchResultsFragment);
        
        ft.commit();        
    }
        
    @Override
    public void eventsChanged() {
    	// 
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.VIEW_EVENT | EventType.DELETE_EVENT;
    }

    @Override
    public void handleEvent(EventInfo event) {
        long endTime = (event.endTime == null) ? -1 : event.endTime.getTimeInMillis();
        /*if (event.eventType == EventType.VIEW_EVENT) {
            showEventInfo(event);
        } else if (event.eventType == EventType.DELETE_EVENT) {
            deleteEvent(event.id, event.startTime.toMillis(false), endTime);
        }*/
    }
        	
    Event mForemostEvent = new Event();      
    final Runnable mForemostEventLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	if (INFO) Log.i(TAG, "***********mForemostEventLoaderSuccessCallback**************");
        	
        	SearchActivity.this.mApp.setBothEndsEvent(BothEndsEventLoader.FOREMOST_EVENT_LOADER, mForemostEvent);
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
    
    Event mBackmostEvent = new Event();    
    final Runnable mBackmostEventLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	if (INFO) Log.i(TAG, "***********mBackmostEventLoaderSuccessCallback**************");
        	
        	SearchActivity.this.mApp.setBothEndsEvent(BothEndsEventLoader.BACKMOST_EVENT_LOADER, mBackmostEvent);
        	mController.sendEvent(this, EventType.BOTHENDS_EVENTS_QUERY_COMPLETION, null, null, null, -1,
            		ViewType.CURRENT, CalendarController.EXTRA_BOTHENDS_BACKMOST_EVENT, 
            		null, null);
        	if (INFO) Log.i(TAG, "**********************************************************");
        }
    };
    
    final Runnable mBackmostEventLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	if (INFO) Log.i(TAG, "Called mBackmostEventLoaderCancelCallback");
        }
    };    
    
    
    
    
    

}
