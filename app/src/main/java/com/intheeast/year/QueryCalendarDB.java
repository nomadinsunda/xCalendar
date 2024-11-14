package com.intheeast.year;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.CalendarViewExitAnim;
import com.intheeast.etc.ETime;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
//import android.text.format.Time;
import android.util.Log;

public class QueryCalendarDB implements LoaderManager.LoaderCallbacks<Cursor> {
	/*
	public final String[] EVENT_PROJECTION = new String[] {
	        Instances.TITLE,                 // 0        
	        Instances.EVENT_ID,              // 1
	        Instances.BEGIN,                 // 2
	        Instances.END,                   // 3
	        Instances._ID,                   // 4
	        Instances.START_DAY,             // 5
	        Instances.END_DAY,               // 6        
		};
	*/
	
	private static final String DISPLAY_AS_ALLDAY = "dispAllday";
	
	public static final String[] EVENT_PROJECTION = new String[] {
        Instances.TITLE,                 // 0
        Instances.EVENT_LOCATION,        // 1
        Instances.ALL_DAY,               // 2
        Instances.CALENDAR_COLOR,        // 3 If SDK < 16, set to Instances.CALENDAR_COLOR.
        Instances.EVENT_TIMEZONE,        // 4
        Instances.EVENT_ID,              // 5
        Instances.BEGIN,                 // 6
        Instances.END,                   // 7
        Instances._ID,                   // 8
        Instances.START_DAY,             // 9
        Instances.END_DAY,               // 10    
        Instances.START_MINUTE           // 11
	};
	
	// The indices for the projection array above.
    private static final int PROJECTION_TITLE_INDEX = 0;
    private static final int PROJECTION_LOCATION_INDEX = 1;
    private static final int PROJECTION_ALL_DAY_INDEX = 2;
    private static final int PROJECTION_COLOR_INDEX = 3;
    private static final int PROJECTION_TIMEZONE_INDEX = 4;
    private static final int PROJECTION_EVENT_ID_INDEX = 5;
    private static final int PROJECTION_BEGIN_INDEX = 6;
    private static final int PROJECTION_END_INDEX = 7;
    private static final int PROJECTION_START_DAY_INDEX = 9;
    private static final int PROJECTION_END_DAY_INDEX = 10;
    private static final int PROJECTION_START_MINUTE_INDEX = 11;

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[PROJECTION_COLOR_INDEX] = Instances.CALENDAR_COLOR;
        }
    }
	
	private final String WHERE_CALENDARS_VISIBLE = Calendars.VISIBLE + "=1";
    private final String INSTANCES_SORT_ORDER = Instances.START_DAY + ","
            + Instances.START_MINUTE + "," + Instances.TITLE;
    
	final int mDaysPerWeek = 7;
    final int mNumWeeks = 6;
    private static final int WEEKS_BUFFER = 1;
    
	private CursorLoader mLoader;
	Context mContext;
	Fragment mFragment;
	Calendar mTargetMonthTime;
	Calendar mTempTime;
	
	int mFirstLoadedJulianDay;
	int mLastLoadedJulianDay;
	
	private Uri mEventUri;
	
	CalendarViewExitAnim mAnimObj;
	Handler mHandler;
	
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	int mFirstDayOfWeek;
	
	// caller�� object�� �ʿ��ϴ�
	// : db query�� ��������� �ϱ� �����̴�
	public QueryCalendarDB(Context context, Fragment fragment, CalendarViewExitAnim animObj, String timeZone) {
		mContext = context;
		mFragment = fragment;
		
		mTzId = timeZone;
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
		
		mTempTime = GregorianCalendar.getInstance(mTimeZone);//new Time(timeZone);
		
		mAnimObj = animObj;
		
		mHandler = new Handler();
		
		//initLoader();
	}
	
	public void initLoader(Calendar targetMonthTime) {
		mTargetMonthTime = targetMonthTime;
		mLoader = (CursorLoader) mFragment.getLoaderManager().initLoader(0, null, this); // �� �ٷ� onCreateLoader�� ȣ��ȴ�
		 																				 // onCreateLoader�� ������ CursorLoader�� 
			 																			 // mLoader�� �����Ѵ�			
	}
	
	boolean mShouldLoad = false;
	public void launchEventLoading() {
		mShouldLoad = true;
		mEventUri = updateUri();

        mLoader.setUri(mEventUri);
        mLoader.startLoading();
        mLoader.onContentChanged();   
        //mHandler.post(mUpdateLoader);
	}
	
	private void stopLoader() {
        synchronized (mUpdateLoader) {
            mHandler.removeCallbacks(mUpdateLoader);
            if (mLoader != null) {
                mLoader.stopLoading();                
            }
        }
    }
    
	private final Runnable mUpdateLoader = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
            	Log.i("tag", "mUpdateLoader.run");
                if (!mShouldLoad || mLoader == null) {
                    return;
                }
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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.i("tag", "onCreateLoader");
		CursorLoader loader;
        synchronized (mUpdateLoader) {        	 
        	int prvExtendDays = mNumWeeks * 7;
        	
            mFirstLoadedJulianDay = ETime.getJulianDay(mTargetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek) - prvExtendDays;  
                       
            mEventUri = updateUri();
            String where = updateWhere();

            loader = new CursorLoader(mContext, 
            		mEventUri, 
            		EVENT_PROJECTION, 
            		where,
                    null /* WHERE_CALENDARS_SELECTED_ARGS */, 
                    INSTANCES_SORT_ORDER);
            //loader.setUpdateThrottle(LOADER_THROTTLE_DELAY);
        }
        
        return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.i("tag", "onLoadFinished");
        synchronized (mUpdateLoader) {
                        
            CursorLoader cLoader = (CursorLoader) loader;
            /*if (mEventUri == null) {
                mEventUri = cLoader.getUri();
                updateLoadedDays();
            }*/
            
            if (cLoader.getUri().compareTo(mEventUri) != 0) {
                // We've started a new query since this loader ran so ignore the
                // result
                return;
            }
            
            ArrayList<Event> events = new ArrayList<Event>();
            buildEventsFromCursor(events, data, mContext, mFirstLoadedJulianDay, mLastLoadedJulianDay);
            
            mAnimObj.setEvents(mFirstLoadedJulianDay, mLastLoadedJulianDay, 
                    mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, events);           
                   
       }		
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub		
	}
	
	private Uri updateUri() {		
		
        // -1 to ensure we get all day events from any time zone
		long mills = ETime.getMillisFromJulianDay(mFirstLoadedJulianDay - 1, mTimeZone, mFirstDayOfWeek);
		mTempTime.setTimeInMillis(mills);//mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
        long start = mTempTime.getTimeInMillis();
        
        int prvExtendDays = mNumWeeks * 7;
        int visibleDaysInView = mNumWeeks * 7;
        int nextExtendDays = mNumWeeks * 7;        
        mLastLoadedJulianDay = mFirstLoadedJulianDay + prvExtendDays + visibleDaysInView + nextExtendDays;
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
	
	boolean mHideDeclined;
	protected String updateWhere() {
        // TODO fix selection/selection args after b/3206641 is fixed
        String where = WHERE_CALENDARS_VISIBLE;
        if (mHideDeclined) {
            where += " AND " + Instances.SELF_ATTENDEE_STATUS + "!="
                    + Attendees.ATTENDEE_STATUS_DECLINED;
        }
        return where;
    }
	
	String mNoTitleString;
	public void buildEventsFromCursor(
            ArrayList<Event> events, Cursor cEvents, Context context, int startDay, int endDay) {
    	
        if (cEvents == null || events == null) {
            //Log.e(TAG, "buildEventsFromCursor: null cursor or null events list!");
            return;
        }

        int count = cEvents.getCount();

        if (count == 0) {
            return;
        }

        Resources res = context.getResources();
        mNoTitleString = res.getString(R.string.no_title_label);
        
        // Sort events in two passes so we ensure the allday and standard events
        // get sorted in the correct order
        cEvents.moveToPosition(-1);
        while (cEvents.moveToNext()) {
            Event e = generateEventFromCursor(cEvents);
            if (e.startDay > endDay || e.endDay < startDay) {
                continue;
            }
            events.add(e);
        }
    }
	
	private Event generateEventFromCursor(Cursor cEvents) {
        Event e = new Event();

        e.id = cEvents.getLong(PROJECTION_EVENT_ID_INDEX);
        e.title = cEvents.getString(PROJECTION_TITLE_INDEX);        
        e.location = cEvents.getString(PROJECTION_LOCATION_INDEX);
        e.allDay = cEvents.getInt(PROJECTION_ALL_DAY_INDEX) != 0;
        
        if (e.title == null || e.title.length() == 0) {
            e.title = mNoTitleString;
        }
        
        if (!cEvents.isNull(PROJECTION_COLOR_INDEX)) {
            // Read the color from the database
            e.color = Utils.getDisplayColorFromColor(cEvents.getInt(PROJECTION_COLOR_INDEX));
        } else {
            e.color = Color.WHITE;
        }
        
        long eStart = cEvents.getLong(PROJECTION_BEGIN_INDEX);
        long eEnd = cEvents.getLong(PROJECTION_END_INDEX);

        e.startMillis = eStart;        
        e.startTime = cEvents.getInt(PROJECTION_START_MINUTE_INDEX);
        e.startDay = cEvents.getInt(PROJECTION_START_DAY_INDEX);

        e.endMillis = eEnd;        
        e.endDay = cEvents.getInt(PROJECTION_END_DAY_INDEX);

        return e;
    }
}
