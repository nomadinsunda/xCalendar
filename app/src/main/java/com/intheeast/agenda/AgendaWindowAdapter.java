/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
//import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.TextView;










import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.intheeast.agenda.AgendaByDayAdapter.RowInfo;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthListView;
import com.intheeast.month.MonthWeekView;
import com.intheeast.settings.SettingsFragment;

/*
Bugs Bugs Bugs:
- At rotation and launch time, the initial position is not set properly. This code is calling
 listview.setSelection() in 2 rapid secessions but it dropped or didn't process the first one.
- Scroll using trackball isn't repositioning properly after a new adapter is added.
- Track ball clicks at the header/footer doesn't work.
- Potential ping pong effect if the prefetch window is big and data is limited
- Add index in calendar provider

ToDo ToDo ToDo:
Get design of header and footer from designer

Make scrolling smoother.
Test for correctness
Loading speed
Check for leaks and excessive allocations
 */

public class AgendaWindowAdapter extends BaseAdapter {

    static final boolean BASICLOG = false;
    static final boolean DEBUGLOG = false;
    private static final boolean INFO = false;
    private static final String TAG = "AgendaWindowAdapter";

    private static final String AGENDA_OLDEST_NEWEST_SORT_ORDER =
            CalendarContract.Instances.START_DAY + " ASC, " +
            CalendarContract.Instances.BEGIN + " ASC";
    
    public static final int AGENDA_OLDEST_NEWEST_INDEX_INSTANCE_ID = 0;
    public static final int AGENDA_OLDEST_NEWEST_INDEX_BEGIN = 1;
    public static final int AGENDA_OLDEST_NEWEST_INDEX_EVENT_ID = 2;
    public static final int AGENDA_OLDEST_NEWEST_INDEX_START_DAY = 3;
    private static final String[] OLDEST_NEWEST_PROJECTION = new String[] {
        Instances._ID, // 0        
        Instances.BEGIN, // 1  
        Instances.EVENT_ID, // 2
        Instances.START_DAY, // 3 Julian start day        
    };

    
    private static final String AGENDA_SORT_ORDER =
            CalendarContract.Instances.START_DAY + " ASC, " +
            CalendarContract.Instances.BEGIN + " ASC, " +
            CalendarContract.Events.TITLE + " ASC";

    public static final int INDEX_INSTANCE_ID = 0;
    public static final int INDEX_TITLE = 1;
    public static final int INDEX_EVENT_LOCATION = 2;
    public static final int INDEX_ALL_DAY = 3;
    public static final int INDEX_HAS_ALARM = 4;
    public static final int INDEX_COLOR = 5;
    public static final int INDEX_RRULE = 6;
    public static final int INDEX_RDATE = 7;
    public static final int INDEX_BEGIN = 8;
    public static final int INDEX_END = 9;
    public static final int INDEX_EVENT_ID = 10;
    public static final int INDEX_START_DAY = 11;
    public static final int INDEX_START_MINUTE = 12;
    public static final int INDEX_END_DAY = 13;
    public static final int INDEX_END_MINUTE = 14;
    public static final int INDEX_SELF_ATTENDEE_STATUS = 15;
    public static final int INDEX_ORGANIZER = 16;
    public static final int INDEX_OWNER_ACCOUNT = 17;
    public static final int INDEX_CAN_ORGANIZER_RESPOND= 18;
    public static final int INDEX_TIME_ZONE = 19;
    public static final int INDEX_GUESTS_CAN_MODIFY = 20;
    
    private static final String[] PROJECTION = new String[] {
        Instances._ID, // 0
        Instances.TITLE, // 1
        Instances.EVENT_LOCATION, // 2
        Instances.ALL_DAY, // 3
        Instances.HAS_ALARM, // 4
        Instances.DISPLAY_COLOR, // 5 If SDK < 16, set to Instances.CALENDAR_COLOR.
        Instances.RRULE, // 6
        Instances.RDATE, // 7
        Instances.BEGIN, // 8
        Instances.END, // 9
        Instances.EVENT_ID, // 10
        Instances.START_DAY, // 11 Julian start day
        Instances.START_MINUTE, // 12 -----------------------------------
        Instances.END_DAY, // 13 Julian end day
        Instances.END_MINUTE, // 14 ---------------------------------------
        Instances.SELF_ATTENDEE_STATUS, // 15
        Instances.ORGANIZER, // 16
        Instances.OWNER_ACCOUNT, // 17
        Instances.CAN_ORGANIZER_RESPOND, // 18
        Instances.EVENT_TIMEZONE, // 19
        Events.GUESTS_CAN_MODIFY, // 20
        
    };
        
    
    //Instances.RDATE, �߰��ؾ� �Ѵ�
    static {
        if (!Utils.isJellybeanOrLater()) {
            PROJECTION[INDEX_COLOR] = Instances.CALENDAR_COLOR;
        }
    }

    // Listview may have a bug where the index/position is not consistent when there's a header.
    // position == positionInListView - OFF_BY_ONE_BUG
    // TODO Need to look into this.
    //private static final int OFF_BY_ONE_BUG = 1;
        
    private static final int OFF_BY_ONE_BUG = 0;
    private static final int MAX_NUM_OF_ADAPTERS = 10;
    private static final int IDEAL_NUM_OF_EVENTS = 200;
    private static final int MIN_QUERY_DURATION = 14; // days
    private static final int MAX_QUERY_DURATION = 120; // days   
    private static final int PREFETCH_BOUNDARY = 40;    
    private static final int MAX_QUERY_RETRY_DURATION = 365; // days : 1year

    /** Times to auto-expand/retry query after getting no data */
    private static final int RETRIES_ON_NO_DATA = 1;

    private final Context mContext;
    private final Resources mResources;
    private final QueryHandler mQueryHandler;
    
    private final AgendaListView mAgendaListView;

    /** The sum of the rows in all the adapters */
    private int mRowCount = 0;

    /** The number of times we have queried and gotten no results back */
    //private int mEmptyCursorCount;

    /** Cached value of the last used adapter */
    private DayAdapterInfo mLastUsedInfo;

    private final LinkedList<DayAdapterInfo> mAdapterInfos =
            new LinkedList<DayAdapterInfo>();
    private final ConcurrentLinkedQueue<QuerySpec> mQueryQueue =
            new ConcurrentLinkedQueue<QuerySpec>();
    //private final TextView mHeaderView;
    //private final TextView mFooterView;
    //private boolean mDoneSettingUpHeaderFooter = false;

    private final boolean mIsTabletConfig;

    boolean mCleanQueryInitiated = false;
    int mCleanQueryInitiatedFlag = 0;
    
    //private int mStickyHeaderSize = 44; // Initial size big enough for it to work

    /**
     * When the user scrolled to the top, a query will be made for older events
     * and this will be incremented. Don't make more requests if
     * mOlderRequests > mOlderRequestsProcessed.
     */
    private int mOlderRequests;

    /** Number of "older" query that has been processed. */
    private int mOlderRequestsProcessed;
    
    private boolean mOlderRequestNoProcessing = true;

    /**
     * When the user scrolled to the bottom, a query will be made for newer
     * events and this will be incremented. Don't make more requests if
     * mNewerRequests > mNewerRequestsProcessed.
     */
    private int mNewerRequests;

    /** Number of "newer" query that has been processed. */
    private int mNewerRequestsProcessed;
    
    boolean mOccurenceWaitingOlderQueryUnderScrolling = false;
    boolean mOccurenceWaitingNewerQueryUnderScrolling = false;

    // Note: Formatter is not thread safe. Fine for now as it is only used by the main thread.
    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    private String mTzId;
    private TimeZone mTimeZone;
    private long mGmtOff;
    static int mFirstDayOfWeek;
    
    // defines if to pop-up the current event when the agenda is first shown
    private final boolean mShowEventOnStart;

    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
        	mTzId = Utils.getTimeZone(mContext, this);
        	mTimeZone = TimeZone.getTimeZone(mTzId);
        	mGmtOff = mTimeZone.getRawOffset() / 1000;
        	
            if (INFO) Log.i(TAG, "mTZUpdater call notifyDataSetChanged");
            notifyDataSetChanged();
        }
    };

    private final Handler mDataChangedHandler = new Handler();
    private final Runnable mDataChangedRunnable = new Runnable() {
        @Override
        public void run() {
        	if (INFO) Log.i(TAG, "mDataChangedRunnable call notifyDataSetChanged");
            notifyDataSetChanged();
        }
    };

    private boolean mShuttingDown;
    private boolean mHideDeclined;

    // Used to stop a fling motion if the ListView is set to a specific position
    int mListViewScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /** The current search query, or null if none */
    private String mSearchQuery;

    /*
    private long mSelectedInstanceId = -1;
    private final int mSelectedItemBackgroundColor;
    private final int mSelectedItemTextColor;
    private final float mItemRightMargin;
	*/  
    
    // Types of Query
    private static final int QUERY_TYPE_OLDER = 0; // Query for older events
    private static final int QUERY_TYPE_NEWER = 1; // Query for newer events
    private static final int QUERY_TYPE_CLEAN = 2; // Delete everything and query around a date

    private static class QuerySpec {
        long queryStartMillis;
        Calendar goToTime = null;
        int start = 0;
        int end = 0;
        String searchQuery = null;
        int queryType;
        long id;
        int TestId;
        boolean queryReTry = false;

        public QuerySpec(int queryType) {
            this.queryType = queryType;
            id = -1;
            TestId = -1;
        }
        
        public QuerySpec(int queryType, int testId) {
            this.queryType = queryType;
            id = -1;
            TestId = testId;
        }
        
        public void setRetry(boolean retry) {
        	queryReTry = retry;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result + (int) (queryStartMillis ^ (queryStartMillis >>> 32));
            result = prime * result + queryType;
            result = prime * result + start;
            if (searchQuery != null) {
                result = prime * result + searchQuery.hashCode();
            }
            if (goToTime != null) {
                long goToTimeMillis = goToTime.getTimeInMillis();
                result = prime * result + (int) (goToTimeMillis ^ (goToTimeMillis >>> 32));
            }
            result = prime * result + (int)id;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            QuerySpec other = (QuerySpec) obj;
            if (end != other.end || queryStartMillis != other.queryStartMillis
                    || queryType != other.queryType || start != other.start
                    || Utils.equals(searchQuery, other.searchQuery) || id != other.id) {
                return false;
            }

            if (goToTime != null) {
                if (goToTime.getTimeInMillis() != other.goToTime.getTimeInMillis()) {
                    return false;
                }
            } else {
                if (other.goToTime != null) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Class representing a list item within the Agenda view.  Could be either an instance of an
     * event, or a header marking the specific day.
     *
     * The begin and end times of an AgendaItem should always be in local time, even if the event
     * is all day.  buildAgendaItemFromCursor() converts each event to local time.
     */
    static class AgendaItem {
        long begin;
        long end;
        long id;
        int startDay;
        boolean allDay;
    }

    static class DayAdapterInfo {
        Cursor cursor;
        AgendaByDayAdapter dayAdapter;
        int start; // start day of the cursor's coverage
        int end; // end day of the cursor's coverage
        int offset; // offset in position in the list view
        int size; // dayAdapter.getCount()
        boolean mUsedForSearch;

        public DayAdapterInfo(Context context) {
            dayAdapter = new AgendaByDayAdapter(context, mEventItemDimensionParams);
        }
        
        public DayAdapterInfo(Context context, boolean usedForSearch) {
            dayAdapter = new AgendaByDayAdapter(context, mEventItemDimensionParams, usedForSearch);
        }

        @Override
        public String toString() {
            // Static class, so the time in this toString will not reflect the
            // home tz settings. This should only affect debugging.
            Calendar time = GregorianCalendar.getInstance();
            StringBuilder sb = new StringBuilder();
            long startMillis = ETime.getMillisFromJulianDay(start, time.getTimeZone(), mFirstDayOfWeek);
            time.setTimeInMillis(startMillis);            
            sb.append("Start:").append(time.toString());
            
            long endMillis = ETime.getMillisFromJulianDay(end, time.getTimeZone(), mFirstDayOfWeek);
            time.setTimeInMillis(endMillis);            
            
            sb.append(" End:").append(time.toString());
            sb.append(" Offset:").append(offset);
            sb.append(" Size:").append(size);
            return sb.toString();
        }
    }

    public static final String EVENT_DAYHEADER_HEIGHT = "event_dayheader_height";
    public static final String EVENT_ITEM_WIDTH = "event_item_width";
    public static final String EVENT_ITEM_HEIGHT = "event_item_height";
    public static final String EVENT_ITEM_TIMECONTAINER_WIDTH = "event_item_time_container_width";
    public static final String EVENT_ITEM_TIMECONTAINER_HEIGHT = "event_item_time_container_height";
    public static final String EVENT_ITEM_TITLELOCATIONCONTAINER_WIDTH = "event_item_titlelocation_container_width";
    public static final String EVENT_ITEM_TITLELOCATIONCONTAINER_HEIGHT = "event_item_titlelocation_container_height";
    public static final String EVENT_ITEM_TIMECONTAINER_LEFTPADDING = "event_item_time_container_left_padding";
    public static final String EVENT_ITEM_TITLELOCATIONCONTAINER_LEFTPADDING = "event_item_titlelocation_container_left_padding";
    public static final String EVENT_ITEM_TIME_TEXTVIEW_WIDTH = "event_item_time_textview_width";
    public static final String EVENT_ITEM_TIME_TEXTVIEW_HEIGHT = "event_item_time_textview_height";
    public static final String EVENT_ITEM_TITLE_TEXTVIEW_HEIGHT = "event_item_title_textview_height";
    public static final String EVENT_ITEM_DIVIDER_WIDTH = "event_item_divider_width";
    public static final String EVENT_ITEM_DIVIDER_HEIGHT = "event_item_divider_height";
    public static final String EVENT_ITEM_DIVIDER_TOPMARGIN = "event_item_divider_top_margin";
    public static final String EVENT_ITEM_TOPDIVIDER_HIGHT = "event_item_topdivider_height";
    //public static final String EVENT_ITEM_ = " ";    
    
    public static final float EVENT_DAY_HEADER_HEIGHT_RATIO = 0.65f;
    int mEventDayHeaderHeight;
    int mEventItemWidth;
    int mEventItemHeight;
    int mEventItemTimeContainerWidth;
    int mEventItemTimeContainerHeight;
    int mEventItemTitleLocationContainerHeight;
    
    int mEventItemTimeContainerLeftPadding;
    int mEventItemTitleLocaitonContainerLeftPadding;    
    
    int mEventItemTimeTextViewWidth;
    int mEventItemTimeTextViewHeight;
    
    int mEventItemTitleTextViewHeight;
    
    int mEventItemDividerTopMargin;
    int mEventItemDividerWidth;
    int mEventItemDividerHeight;
    
    int mEventItemTopDividerHeight;
    //final int mMaxYearTimeJulainDay;
    public static HashMap<String, Integer> mEventItemDimensionParams = null;
    boolean mUsedForSearch;
    Calendar mRefreshGoToTime;
    
    public AgendaWindowAdapter(Context context,
            AgendaListView agendaListView, boolean usedForSearch, boolean showEventOnStart) {
        mContext = context;
        mResources = context.getResources();
        /*
        mSelectedItemBackgroundColor = mResources
                .getColor(R.color.agenda_selected_background_color);
        mSelectedItemTextColor = mResources.getColor(R.color.agenda_selected_text_color);
        mItemRightMargin = mResources.getDimension(R.dimen.agenda_item_right_margin);
        */
        mIsTabletConfig = Utils.getConfigBool(mContext, R.bool.tablet_config);

            	
    	mTzId = Utils.getTimeZone(context, mTZUpdater);
    	mTimeZone = TimeZone.getTimeZone(mTzId);
    	mGmtOff = mTimeZone.getRawOffset() / 1000;
        //CalendarController.MAX_CALENDAR_YEAR = 2036;
        
        mRefreshGoToTime = GregorianCalendar.getInstance(mTimeZone);
        
        mAgendaListView = agendaListView;
        mUsedForSearch = usedForSearch;
        
        mQueryHandler = new QueryHandler(context.getContentResolver());
        

        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        mEventItemStringBuilder = new StringBuilder(50);
    	mEventItemFormatter = new Formatter(mEventItemStringBuilder, Locale.getDefault());
    	
        mShowEventOnStart = showEventOnStart;

        // Implies there is no sticky header
        //if (!mShowEventOnStart) {
            //mStickyHeaderSize = 0;
        //}
        mSearchQuery = null;
        
        makeEventItemLayoutDimension();
        makeEventItemDimensionParams();   
        
        setDefaultBothEndsEventYear();
               
    }
    
    public void setDefaultBothEndsEventYear() {
    	mForemostEventYearJulainDay = ETime.ECALENDAR_EPOCH_JULIAN_DAY;
    	
    	Calendar maxYearTime = GregorianCalendar.getInstance(mTimeZone);        
        maxYearTime.set(CalendarController.MAX_CALENDAR_YEAR, 0, 1);
        
        mBackmostEventYearJulainDay = ETime.getJulianDay(maxYearTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);    	
    }
    
    int mForemostEventYearJulainDay;
    public void setForemostEventYear(int foremostEventEnd) {
    	Calendar minYearTime = GregorianCalendar.getInstance(mTimeZone);     
    	long foremostEventEndMillis = ETime.getMillisFromJulianDay(foremostEventEnd, mTimeZone, mFirstDayOfWeek);    	
    	minYearTime.setTimeInMillis(foremostEventEndMillis);//minYearTime.setJulianDay(foremostEventEnd);
    	
    	int prvYearOfMaxYear = 0;
    	if (minYearTime.get(Calendar.YEAR) < 1900)
    		prvYearOfMaxYear = 1900;
    	else {
    		prvYearOfMaxYear = minYearTime.get(Calendar.YEAR);
    	}
    	
        // ���� 2nd Para : month the zero-based month number (in the range [0,11])
        minYearTime.set(prvYearOfMaxYear, 0, 1);
        
        synchronized (mQueryQueue) {
        	mForemostEventYearJulainDay = ETime.getJulianDay(minYearTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);    	
        }
        
        Calendar temp = GregorianCalendar.getInstance(mTimeZone);  
        long foremostEventYearJulainDayMillis = ETime.getMillisFromJulianDay(mForemostEventYearJulainDay, mTimeZone, mFirstDayOfWeek);    	
        temp.setTimeInMillis(foremostEventYearJulainDayMillis);//temp.setJulianDay(mForemostEventYearJulainDay);       
    }
    
    int mBackmostEventYearJulainDay;
    public void setBackmostEventYear(int backmostEventEnd) {
    	Calendar maxYearTime = GregorianCalendar.getInstance(mTimeZone);   
    	long backmostEventEndMillis = ETime.getMillisFromJulianDay(backmostEventEnd, mTimeZone, mFirstDayOfWeek);    	
    	maxYearTime.setTimeInMillis(backmostEventEndMillis);//maxYearTime.setJulianDay(backmostEventEnd);
    	
    	
        int maxYear = maxYearTime.get(Calendar.YEAR);//maxYearTime.year;        
        maxYearTime.set(maxYear, 0, 1);
        
        synchronized (mQueryQueue) {
        	mBackmostEventYearJulainDay = ETime.getJulianDay(maxYearTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);    	
        }
        
        Calendar temp = GregorianCalendar.getInstance(mTimeZone); 
        long backmostEventYearJulainDayMillis = ETime.getMillisFromJulianDay(mBackmostEventYearJulainDay, mTimeZone, mFirstDayOfWeek);    	
        temp.setTimeInMillis(backmostEventYearJulainDayMillis);//temp.setJulianDay(mBackmostEventYearJulainDay);        
    }
    
    
    // Method in Adapter
    @Override
    public int getViewTypeCount() {
        return AgendaByDayAdapter.TYPE_LAST;
    }

    // Method in BaseAdapter
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    // Method in Adapter
    @Override
    public int getItemViewType(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItemViewType(position - info.offset);
        } else {
        	if (INFO) Log.i(TAG, "getItemViewType: info null, so return null");
            return -1;
        }
    }

    // Method in BaseAdapter
    @Override
    public boolean isEnabled(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.isEnabled(position - info.offset);
        } else {
            return false;
        }
    }

    
    // Abstract Method in BaseAdapter
    @Override
    public int getCount() {
    	//Log.i("tag", "getCount:mRowCount=" + String.valueOf(mRowCount));
    	//if (INFO) Log.i(TAG, "getCount:mRowCount=" + String.valueOf(mRowCount));
    	
    	if (mRowCount > 0)
    		mAgendaListView.setAgendaDayIndicatorVisibleState(true);
    	else
    		mAgendaListView.setAgendaDayIndicatorVisibleState(false);
        return mRowCount;
    }

    // Abstract Method in BaseAdapter
    @Override
    public Object getItem(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItem(position - info.offset);
        } else {
        	if (INFO) Log.i(TAG, "getItem: info null, so return null");
            return null;
        }
    }

    // Method in BaseAdapter
    @Override
    public boolean hasStableIds() {
        return true;
    }

    // Abstract Method in BaseAdapter
    @Override
    public long getItemId(int position) {
    	//long ret = 0;
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int curPos = info.dayAdapter.getCursorPosition(position - info.offset);
            if (curPos == Integer.MIN_VALUE) {
            	//Log.i("tag", "getItemId: info != null, so return -1");
            	if (INFO) Log.i(TAG, "getItemId: curPos == Integer.MIN_VALUE, so return -1");
                return -1;
            }
            
            // Regular event
            if (curPos >= 0) {
                info.cursor.moveToPosition(curPos);
                /*ret = info.cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID) << 20 +
                        info.cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
                Log.i("tag", "getItemId: Regular event, so return " + String.valueOf(ret));
                return ret;*/
                return info.cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID) << 20 +
                    info.cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
            }
            
            // Day Header
            /*ret = info.dayAdapter.findJulianDayFromPosition(position);
            Log.i("tag", "getItemId: Day Header, so return " + String.valueOf(ret));
            return ret;*/
            return info.dayAdapter.findJulianDayFromPosition(position);

        } else {        	
        	if (INFO) Log.i(TAG, "getItemId: info null, so return -1");
            return -1;
        }
    }
    
    
    
    public void resetScrollingQueryProcessFlag() {
    	mOccurenceWaitingOlderQueryUnderScrolling = false;
    	
    	mOccurenceWaitingNewerQueryUnderScrolling = false;
    }
    
    public void queryUnderScrollingState(boolean IsScrollingUp, int firstVisibilePosition, int lastVisiblePosition) {
    	
        if (!IsScrollingUp) {
        	DayAdapterInfo first = mAdapterInfos.getFirst();
        	if (first.start == mForemostEventYearJulainDay) {
        		return;
        	}
        	
	    	if (firstVisibilePosition < PREFETCH_BOUNDARY) {  
	    		// mOccurenceWaitingOlderQueryUnderScrolling ������ RESET��
	    		// AgendaFragment.onScrollStateChanged�� IDLE ���¿��� ����
	    		if (!mOccurenceWaitingOlderQueryUnderScrolling) {
	    			mOccurenceWaitingOlderQueryUnderScrolling = true;
	    			
	    			int QNumber = mQueryQueue.size();
		        	Random random = new Random();
		        	int id = random.nextInt(1000);
	    			queueQuery(new QuerySpec(QUERY_TYPE_OLDER, id));
    			}
	    		else {
	    			return;
	    		}	
	    	}
        }
        else {   	
        	DayAdapterInfo last = mAdapterInfos.getLast();
        	if (last.end == mBackmostEventYearJulainDay) {
        		return;
        	}
    		// lastVisiblePosition�� (-)���� �Ǵ� ���� ���� ���̴�
    		// �׷��Ƿ� mRowCount < PREFETCH_BOUNDARY �� ��Ȳ������ mRowCount - PREFETCH_BOUNDARY ���� (-)���̴�
    		// �׷��� mRowCount < PREFETCH_BOUNDARY ��Ȳ�� ������ QUERY_TYPE_NEWER�� �ؾ� �ϴ� ��Ȳ�̴�
    		if (lastVisiblePosition >= (mRowCount - PREFETCH_BOUNDARY)) {    		
    			if (!mOccurenceWaitingNewerQueryUnderScrolling) {
    				mOccurenceWaitingNewerQueryUnderScrolling = true;
	    			
	    			int QNumber = mQueryQueue.size();
		        	Random random = new Random();
		        	int id = random.nextInt(1000);
	    			queueQuery(new QuerySpec(QUERY_TYPE_NEWER, id));
    			}
	    		else {
	    			return;
	    		}	
        	} 
        }
    	//}   	
    }

    // Abstract Method in BaseAdapter
    public View getView(int position, View convertView, ViewGroup parent) {
    	//if (INFO) Log.i(TAG, "getView:mRowCount=" + String.valueOf(mRowCount) + ", position=" + String.valueOf(position));    
    	    	
        final View v;
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int offset = position - info.offset;
            v = info.dayAdapter.getView(offset, convertView,
                    parent);            
            
        } else {
            // TODO
        	if (INFO) Log.i(TAG, "BUG: getAdapterInfoByPosition returned null!!! " + position);
            TextView tv = new TextView(mContext);
            tv.setText("Bug! " + position);
            v = tv;
        }

        return v;        
    }
    
    public RowInfo getRowInfo(int position) {
    	DayAdapterInfo info = getAdapterInfoByPosition(position);
    	if (info != null) {
	    	int offset = position - info.offset;
	    	RowInfo row = info.dayAdapter.getRowInfo(offset);
	    	return row;
    	}
    	else
    		return null;
    }

    //private AgendaAdapter.ViewHolder mSelectedVH = null;

    private int findEventPositionNearestTime(Calendar time, long id) {
        DayAdapterInfo info = getAdapterInfoByTime(time);
        int pos = -1;
        if (info != null) {
            pos = info.offset + info.dayAdapter.findEventPositionNearestTime(time, id);
        }
        
        if (INFO) Log.i(TAG, "findEventPositionNearestTime " + time + " id:" + id + " =" + pos);
        return pos;
    }

    public DayAdapterInfo getAdapterInfoByPosition(int position) {
        synchronized (mAdapterInfos) {
            if (mLastUsedInfo != null && mLastUsedInfo.offset <= position
                    && position < (mLastUsedInfo.offset + mLastUsedInfo.size)) {
                return mLastUsedInfo;
            }
            
            for (DayAdapterInfo info : mAdapterInfos) {
                if (info.offset <= position
                        && position < (info.offset + info.size)) {
                    mLastUsedInfo = info;
                    return info;
                }
            }
        }
        return null;
    }

    private DayAdapterInfo getAdapterInfoByTime(Calendar time) {
    	
    	Calendar tmpTime = GregorianCalendar.getInstance(time.getTimeZone());//new Time(time);
    	tmpTime.setFirstDayOfWeek(time.getFirstDayOfWeek());
    	tmpTime.setTimeInMillis(time.getTimeInMillis());
        long timeInMillis = tmpTime.getTimeInMillis();
        int day = ETime.getJulianDay(timeInMillis, mTimeZone, mFirstDayOfWeek);    	
        synchronized (mAdapterInfos) {
            for (DayAdapterInfo info : mAdapterInfos) {
                if (info.start <= day && day <= info.end) {
                    return info;
                }
            }
        }
        return null;
    }

    public AgendaItem getAgendaItemByPosition(final int positionInListView) {
        return getAgendaItemByPosition(positionInListView, true);
    }

    /**
     * Return the event info for a given position in the adapter
     * @param positionInListView
     * @param returnEventStartDay If true, return actual event startday. Otherwise
     *        return agenda date-header date as the startDay.
     *        The two will differ for multi-day events after the first day.
     * @return
     */
    public AgendaItem getAgendaItemByPosition(final int positionInListView,
            boolean returnEventStartDay) {
    	if (INFO) Log.i(TAG, "getEventByPosition " + positionInListView);
        if (positionInListView < 0) {
            return null;
        }

        final int positionInAdapter = positionInListView - OFF_BY_ONE_BUG;
        DayAdapterInfo info = getAdapterInfoByPosition(positionInAdapter);
        if (info == null) {
            return null;
        }        
        

        int cursorPosition = info.dayAdapter.getCursorPosition(positionInAdapter - info.offset);
        if (cursorPosition == Integer.MIN_VALUE) {
            return null;
        }

        boolean isDayHeader = false;
        if (cursorPosition < 0) {
            cursorPosition = -cursorPosition;
            isDayHeader = true;
        }

        if (cursorPosition < info.cursor.getCount()) {
            AgendaItem item = buildAgendaItemFromCursor(info.cursor, cursorPosition, isDayHeader);
            if (!returnEventStartDay && !isDayHeader) {
                item.startDay = info.dayAdapter.findJulianDayFromPosition(positionInAdapter -
                        info.offset);
            }
            return item;
        }
        return null;
    }

    private AgendaItem buildAgendaItemFromCursor(final Cursor cursor, int cursorPosition,
            boolean isDayHeader) {
        if (cursorPosition == -1) {
            cursor.moveToFirst();
        } else {
            cursor.moveToPosition(cursorPosition);
        }
        AgendaItem agendaItem = new AgendaItem();
        agendaItem.begin = cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
        agendaItem.end = cursor.getLong(AgendaWindowAdapter.INDEX_END);
        agendaItem.startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);
        agendaItem.allDay = cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
        if (agendaItem.allDay) { // UTC to Local time conversion
            /*Time time = new Time(mTimeZone);
            time.setJulianDay(Time.getJulianDay(agendaItem.begin, 0));
            agendaItem.begin = time.toMillis(false);*/
            
            Calendar time = GregorianCalendar.getInstance(mTimeZone); 
        	int beginUTCJulianDay = ETime.getJulianDay(agendaItem.begin, mTimeZone, mFirstDayOfWeek);    	
        	long beginUTCJulianDayMillis = ETime.getMillisFromJulianDay(beginUTCJulianDay, mTimeZone, mFirstDayOfWeek);    	
            time.setTimeInMillis(beginUTCJulianDayMillis);
            agendaItem.begin = time.getTimeInMillis();
        } else if (isDayHeader) { // Trim to midnight.
        	Calendar time = GregorianCalendar.getInstance(mTimeZone); 
            time.setTimeInMillis(agendaItem.begin);
            time.set(Calendar.HOUR_OF_DAY, 0);//time.hour = 0;
            time.set(Calendar.MINUTE, 0);//time.minute = 0;
            time.set(Calendar.SECOND, 0);//time.second = 0;
            
            agendaItem.begin = time.getTimeInMillis();
        }

        // If this is not a day header, then it's an event.
        if (!isDayHeader) {
            agendaItem.id = cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID);
            if (agendaItem.allDay) {
                /*Time time = new Time(mTimeZone);
                time.setJulianDay(Time.getJulianDay(agendaItem.end, 0));
                agendaItem.end = time.toMillis(false);*/                
                
                Calendar time = GregorianCalendar.getInstance(mTimeZone); 
            	int endUTCJulianDay = ETime.getJulianDay(agendaItem.end, mTimeZone, mFirstDayOfWeek);    	
            	long endUTCJulianDayMillis = ETime.getMillisFromJulianDay(endUTCJulianDay, mTimeZone, mFirstDayOfWeek);    	
                time.setTimeInMillis(endUTCJulianDayMillis);
                agendaItem.end = time.getTimeInMillis();
            }
        }
        return agendaItem;
    }

    /**
     * Ensures that any all day events are converted to UTC before a VIEW_EVENT command is sent.
     */
    private void sendViewEvent(AgendaItem item, long selectedTime) {
        long startTime;
        long endTime;
        if (item.allDay) {
            startTime = Utils.convertAlldayLocalToUTC(null, item.begin, mTzId);
            endTime = Utils.convertAlldayLocalToUTC(null, item.end, mTzId);
        } else {
            startTime = item.begin;
            endTime = item.end;
        }
        if (INFO) Log.i(TAG, "Sent (AgendaWindowAdapter): VIEW EVENT: " + new Date(startTime));
        
        CalendarController.getInstance(mContext)
        .sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT,
                item.id, startTime, endTime, 0,
                0, CalendarController.EventInfo.buildViewExtraLong(
                        Attendees.ATTENDEE_STATUS_NONE,
                        item.allDay), selectedTime);
    }

    
    /*     
	*AgendaFragment���� ȣ���ϴ� ���
	-onResume
	 ->AgendaListView.goTo
	-eventsChanged
	 ->mAgendaListView.refresh
	  -->refresh
	-handleEvent
	 ->goTo
      -->AgendaListView.goTo
    -search
	 ->AgendaListView.goTo

	*AgendaListView
    -mMidnightUpdater
	-goTo
	 ->refresh
     */
    
    public void refresh(Calendar goToTime, long id, String searchQuery, boolean forced,
            boolean refreshEventInfo) {
    	
    	if (INFO) Log.i(TAG, "***refresh***");
    	
    	mRefreshGoToTime.setTimeInMillis(goToTime.getTimeInMillis());
    	
        if (searchQuery != null) {
            mSearchQuery = searchQuery;
        }

        if (DEBUGLOG) {
            Log.e(TAG, this + ": refresh " + goToTime.toString() + " id " + id
                    + ((searchQuery != null) ? searchQuery : "")
                    + (forced ? " forced" : " not forced")
                    + (refreshEventInfo ? " refresh event info" : ""));
        }

        int startDay = ETime.getJulianDay(goToTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);    	

        if (!forced && isInRange(startDay, startDay)) {
            // No need to re-query
            if (!mAgendaListView.isAgendaItemVisible(goToTime, id)) {
                int gotoPosition = findEventPositionNearestTime(goToTime, id);
                if (gotoPosition > 0) {
                    mAgendaListView.setSelectionFromTop(gotoPosition +
                            OFF_BY_ONE_BUG, 0);
                    if (mListViewScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                        mAgendaListView.smoothScrollBy(0, 0);
                    }                    
                }

                Calendar actualTime = GregorianCalendar.getInstance(mTimeZone); 
                actualTime.setTimeInMillis(goToTime.getTimeInMillis());
                CalendarController.getInstance(mContext).sendEvent(this, EventType.UPDATE_TITLE,
                        actualTime, actualTime, -1, ViewType.CURRENT);
            }
            return;
        }

        // If AllInOneActivity is sending a second GOTO event(in OnResume), ignore it.
        if (!mCleanQueryInitiated || searchQuery != null) {
            // Query for a total of MIN_QUERY_DURATION days
            int endDay = startDay + MIN_QUERY_DURATION;

            //mSelectedInstanceId = -1;
            
            mCleanQueryInitiated = true;
            queueQuery(startDay, endDay, goToTime, searchQuery, QUERY_TYPE_CLEAN, id);

            // Pre-fetch more data to overcome a race condition in AgendaListView.shiftSelection
            // Queuing more data with the goToTime set to the selected time skips the call to
            // shiftSelection on refresh.
            //mOlderRequests++;
            queueQuery(0, 0, goToTime, searchQuery, QUERY_TYPE_OLDER, id);
            //mNewerRequests++;
            queueQuery(0, 0, goToTime, searchQuery, QUERY_TYPE_NEWER, id);
        }
    }

    public void close() {
        mShuttingDown = true;
        pruneAdapterInfo(QUERY_TYPE_CLEAN);
        if (mQueryHandler != null) {
            mQueryHandler.cancelOperation(0);
        }        
    }

    private DayAdapterInfo pruneAdapterInfo(int queryType) {
        synchronized (mAdapterInfos) {
            DayAdapterInfo recycleMe = null;
            
            if (!mAdapterInfos.isEmpty()) {            	
            	// Day adapter���� ������ ������ ���� �Ѱ�ġ�� �ʰ��� ���
                if (mAdapterInfos.size() >= MAX_NUM_OF_ADAPTERS) {                  	
                    if (queryType == QUERY_TYPE_NEWER) {                    	
                        recycleMe = mAdapterInfos.removeFirst();                        
                    } else if (queryType == QUERY_TYPE_OLDER) {   
                    	//�� older�� event���� ������ �����ؾ� �ϱ� ������ ���� newer�� first day adapter�� list���� �����Ѵ�
                        recycleMe = mAdapterInfos.removeLast(); 
                        // Keep the size only if the oldest items are removed.
                        // But NO need the size of the removed newest items
                        recycleMe.size = 0;                        
                    }
                    
                    if (recycleMe != null) {
                        if (recycleMe.cursor != null) {
                            recycleMe.cursor.close();
                        }
                        return recycleMe;
                    }
                }
                else {
                	if(INFO) Log.i(TAG, "pruneAdapterInfo : mAdapterInfos.size() < MAX_NUM_OF_ADAPTERS");
                }
                
                // if) mAdapterInfos.size() < MAX_NUM_OF_ADAPTERS and queryType == QUERY_TYPE_NEWER or QUERY_TYPE_OLDER,
                //     ->���� �����ϴ°�???
                if (mRowCount == 0 || queryType == QUERY_TYPE_CLEAN) {
                	if(INFO) Log.i(TAG, "mRowCount == 0 || queryType == QUERY_TYPE_CLEAN");
                	
                    mRowCount = 0;
                    int deletedRows = 0;
                    DayAdapterInfo info;
                    do {
                        info = mAdapterInfos.poll(); //This method is equivalent to pollFirst().

                        if (info != null) {
                            // TODO the following causes ANR's. Do this in a thread.
                            info.cursor.close();
                            deletedRows += info.size;
                            recycleMe = info; ////////////////////
                        }
                    } while (info != null);

                    // ���� mAdapterInfos�� empty�� �ƴ� ��Ȳ������ 
                    // ���������� poll�� element�� ���ϵȴ�
                    if (recycleMe != null) {
                        recycleMe.cursor = null;
                        recycleMe.size = deletedRows;
                    }
                }
            }
            
            return recycleMe;
        }
    }

    private String buildQuerySelection() {
        // Respect the preference to show/hide declined events

        if (mHideDeclined) {
            return Calendars.VISIBLE + "=1 AND "
                    + Instances.SELF_ATTENDEE_STATUS + "!="
                    + Attendees.ATTENDEE_STATUS_DECLINED;
        } else {        	
        	return Calendars.VISIBLE + "=1";
        }
    }
    
    private String buildOldestEventQuerySelection() {
    	
    	//SELECT MIN(_id) FROM tableName;
    	//return "MIN(" + Instances.START_DAY + ") AND " + Calendars.VISIBLE + "=1";       
    	return "MIN(" + Instances.START_DAY + ")";       
        
    }

    private Uri buildQueryUri(int start, int end, String searchQuery) {
        Uri rootUri = searchQuery == null ?
                Instances.CONTENT_BY_DAY_URI :
                Instances.CONTENT_SEARCH_BY_DAY_URI;
        
        Uri.Builder builder = rootUri.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        if (searchQuery != null) {
            builder.appendPath(searchQuery);
        }
        return builder.build();
    }
    
    
    private Uri buildOldestQueryUri() {
        Uri rootUri = Instances.CONTENT_BY_DAY_URI;
        
        Uri.Builder builder = rootUri.buildUpon();
        
        return builder.build();
    }
	
    
    private boolean isInRange(int queryType, int start, int end) {
        synchronized (mAdapterInfos) {
            if (mAdapterInfos.isEmpty()) {            	
                return false;
            }
            
            if (queryType == QUERY_TYPE_OLDER) {
            	DayAdapterInfo first = mAdapterInfos.getFirst();
            	if (first.start == mForemostEventYearJulainDay) {
            		return true;
            	}
            }
            else if (queryType == QUERY_TYPE_NEWER) {
            	DayAdapterInfo last = mAdapterInfos.getLast();
    			if (last.end == mBackmostEventYearJulainDay) {
    				return true;
    			}
            }        	
			
            boolean isIn = mAdapterInfos.getFirst().start <= start && end <= mAdapterInfos.getLast().end;
            
            return isIn;
            
            //return mAdapterInfos.getFirst().start <= start && end <= mAdapterInfos.getLast().end;
        }
    }
    
    private boolean isInRange(int start, int end) {
        synchronized (mAdapterInfos) {
            if (mAdapterInfos.isEmpty()) {
            	//Log.i("tag", "mAdapterInfos.isEmpty()");
                return false;
            }
                       
			
            /*boolean ret = mAdapterInfos.getFirst().start <= start && end <= mAdapterInfos.getLast().end;
            if (ret)
            	Log.i("tag", "isInRange : return true");
            else
            	Log.i("tag", "isInRange : return false");
            
            return ret;
            */
            return mAdapterInfos.getFirst().start <= start && end <= mAdapterInfos.getLast().end;
        }
    }

    private int calculateQueryDuration(int start, int end) {
        int queryDuration = MAX_QUERY_DURATION;
        if (mRowCount != 0) {
            queryDuration = IDEAL_NUM_OF_EVENTS * (end - start + 1) / mRowCount;
        }

        if (queryDuration > MAX_QUERY_DURATION) {
            queryDuration = MAX_QUERY_DURATION;
        } else if (queryDuration < MIN_QUERY_DURATION) {
            queryDuration = MIN_QUERY_DURATION;
        }

        return queryDuration;
    }

    private boolean queueQuery(int start, int end, Calendar goToTime,
            String searchQuery, int queryType, long id) {
        QuerySpec queryData = new QuerySpec(queryType);
        queryData.goToTime = GregorianCalendar.getInstance(goToTime.getTimeZone());//new Time(goToTime);    // Creates a new time reference per QuerySpec.
        goToTime.setTimeInMillis(goToTime.getTimeInMillis());
        queryData.start = start;
        queryData.end = end;
        queryData.searchQuery = searchQuery;
        queryData.id = id;
        return queueQuery(queryData);
    }

    private boolean queueQuery(QuerySpec queryData) {
        queryData.searchQuery = mSearchQuery;
        Boolean queuedQuery;
        synchronized (mQueryQueue) {
            queuedQuery = false;
            Boolean doQueryNow = mQueryQueue.isEmpty();
            mQueryQueue.add(queryData);
            queuedQuery = true;
            if (doQueryNow) {
                doQuery(queryData);
            }
        }
        return queuedQuery;
    }
    
    private boolean queueQueryWithoutBlocking(QuerySpec queryData) {
        queryData.searchQuery = mSearchQuery;
        Boolean queuedQuery;
        
        queuedQuery = false;
        Boolean doQueryNow = mQueryQueue.isEmpty();
        mQueryQueue.add(queryData);
        queuedQuery = true;
        if (doQueryNow) {
            doQuery(queryData);
        }
        
        return queuedQuery;
    }

    private void doQuery(QuerySpec queryData) {
        if (!mAdapterInfos.isEmpty()) {
        	       	        	
        	if (!queryData.queryReTry) {
        				
        		int start = mAdapterInfos.getFirst().start;
                int end = mAdapterInfos.getLast().end;
                
                // �Ʒ� calculateQueryDuration�� �� �̻� ������� �ʴ´�
                //int queryDuration = calculateQueryDuration(start, end);
                int queryDuration = end - start;
	            switch(queryData.queryType) {
	                case QUERY_TYPE_OLDER:
	                    queryData.end = start - 1;
	                    queryData.start = queryData.end - queryDuration;	                    
	                    break;
	                case QUERY_TYPE_NEWER:
	                    queryData.start = end + 1;
	                    queryData.end = queryData.start + queryDuration; 
	                    break;	                
	            }	            
	            
	            if (queryData.start < mForemostEventYearJulainDay) {
	            	queryData.start = mForemostEventYearJulainDay;
	            	
	            	if (queryData.end < queryData.start) {
	            		queryData.end = mForemostEventYearJulainDay;	            		
	            	}	            	
	            }
	        	
	        	if (mBackmostEventYearJulainDay < queryData.end) {
	            	queryData.end = mBackmostEventYearJulainDay;	   
	            	
	            	if (queryData.end < queryData.start) {
	            		queryData.start = mBackmostEventYearJulainDay;	            		
	            	}	            	
	        	}	        	
        	}        
        }

        if (BASICLOG) {
        	Calendar time = GregorianCalendar.getInstance(mTimeZone);             
            long startMillis = ETime.getMillisFromJulianDay(queryData.start, mTimeZone, mFirstDayOfWeek);   
            time.setTimeInMillis(startMillis);//time.setJulianDay(queryData.start);
            
            Calendar time2 = GregorianCalendar.getInstance(mTimeZone);            
            long endMillis = ETime.getMillisFromJulianDay(queryData.end, mTimeZone, mFirstDayOfWeek);   
            time2.setTimeInMillis(endMillis);//time2.setJulianDay(queryData.end);
            Log.v(TAG, "startQuery: " + time.toString() + " to "
                    + time2.toString() + " then go to " + queryData.goToTime);
        }
        
        /*
        Time time = new Time(mTimeZone);
        time.setJulianDay(queryData.start);
        Time time2 = new Time(mTimeZone);
        time2.setJulianDay(queryData.end);
        Log.i("tag", "startQuery: from " + time.format2445() + " to " + time2.format2445());
        Log.i("tag", " then go to " + queryData.goToTime);
		*/
        mQueryHandler.cancelOperation(0);
        //if (BASICLOG) queryData.queryStartMillis = System.nanoTime();

        Uri queryUri = buildQueryUri(
                queryData.start, queryData.end, queryData.searchQuery);
        mQueryHandler.startQuery(0, queryData, queryUri,
                PROJECTION, buildQuerySelection(), null,
                AGENDA_SORT_ORDER);
    }    

    /*
    private String formatDateString(int julianDay) {
    	Calendar time = GregorianCalendar.getInstance(mTimeZone);             
        long julianDayMillis = ETime.getJulianDay(julianDay, mGmtOff);
        time.setTimeInMillis(julianDayMillis);//time.setJulianDay(julianDay);
        
        long millis = time.getTimeInMillis();
        mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(mContext, mFormatter, millis, millis,
                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_ABBREV_MONTH, mTzId).toString();
    }
	*/
    
    boolean mRETRIES_ON_NO_DATA = false;
    int mRetryNumbers = 0;
    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (INFO) Log.i(TAG, "(+)onQueryComplete");
                        
            //Log.i("tag", "(+)onQueryComplete");            
            
            QuerySpec data = (QuerySpec)cookie;
            /*
	            switch(data.queryType) {
	            case QUERY_TYPE_OLDER:
	            	Log.i("tag", "onQueryComplete : QUERY_TYPE_OLDER : TestId=" + String.valueOf(data.TestId));                    
	                break;
	            case QUERY_TYPE_NEWER:
	            	Log.i("tag", "onQueryComplete : QUERY_TYPE_NEWER : TestId=" + String.valueOf(data.TestId));   
	                break;  
	            case QUERY_TYPE_CLEAN:
	            	Log.i("tag", "onQueryComplete : QUERY_TYPE_CLEAN : TestId=" + String.valueOf(data.TestId));   
	                break;  
	        	}
            */
                        
            if (cursor == null) {
              if (mAgendaListView != null && mAgendaListView.getContext() instanceof Activity) {
            	  ((Activity) mAgendaListView.getContext()).finish();
              }
              
              //Log.i("tag", "onQueryComplete : cursor null");    
              return;
            }

            if (BASICLOG) {
                long queryEndMillis = System.nanoTime();
                Log.e(TAG, "Query time(ms): "
                        + (queryEndMillis - data.queryStartMillis) / 1000000
                        + " Count: " + cursor.getCount());
            }

            if (data.queryType == QUERY_TYPE_CLEAN) {
                mCleanQueryInitiated = false;
            }

            if (mShuttingDown) {
                cursor.close();
                return;
            }

            // Notify Listview of changes and update position
            int cursorSize = cursor.getCount();
            // ������ �� : �ش� �Ⱓ�� EVENT�� ���� ����?
            if (cursorSize > 0 || 
            		mAdapterInfos.isEmpty() || 
            		data.queryType == QUERY_TYPE_CLEAN) {
            	
                final int listPositionOffset = processNewCursor(data, cursor);
                if(INFO) Log.i(TAG, "onQueryComplete : listPositionOffset=" + String.valueOf(listPositionOffset));
                
                // downward fling ���̰� 
                // Ư�� older query ����� cursorSize�� 1���, �� �̻� older query�� �������� �ʴ´�                
                // ���� �� ���� older event�� �ʿ��� ��Ȳ����  �� �̻� older query�� ������� �ʴ� ������ �߻��Ѵ�
                                           
                // �������� ���ƾ� �ϴ� ����
                // Typical Scrolling/Fling type query ���¿����� listview item shift�� �����Ѵٴ� ���̴�
                int newPosition = -1;
                if (data.goToTime == null) { // Typical Scrolling/Fling type query  
                	if (INFO) Log.i(TAG, "onQueryComplete : data.goToTime == null");
                	/*
                	 getView�� entrance�� ���� �� ������
                	 -queueQuery(new QuerySpec(QUERY_TYPE_NEWER));
					 -queueQuery(new QuerySpec(QUERY_TYPE_OLDER));
					 :�� �� ��쿡�� goToTime�� �������� �ʴ´�
                	 */
                	if (mListViewScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                		if (INFO) Log.i(TAG, "onQueryComplete.a : SCROLL_STATE_TOUCH_SCROLL");
                    } else if (mListViewScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                    	//mAgendaListView.smoothScrollBy(0, 0); // ����ϸ� �ȵȴ�!!!  
                		if (INFO) Log.i(TAG, "onQueryComplete.a : SCROLL_STATE_FLING");
                    }                	
                	
                    notifyDataSetChanged();/////////////////////////////////////////////////////
                    if (listPositionOffset != 0) {                    	
                        mAgendaListView.shiftSelection(listPositionOffset);
                    }
                } else { // refresh() called. Go to the designated position
                	if (INFO) Log.i(TAG, "onQueryComplete : data.goToTime != null");
                	
                    final Calendar goToTime = GregorianCalendar.getInstance(data.goToTime.getTimeZone());//data.goToTime;   
                    goToTime.setTimeInMillis(data.goToTime.getTimeInMillis());
                    boolean mustNotify = true;
                    
                    /*
                    if ( mAdapterInfos.getFirst().start == mForemostEventYearJulainDay &&
                    		mBackmostEventYearJulainDay == mAdapterInfos.getLast().end) {
                    	//if (mRowCount < 20) {
                    		mustNotify = true;
                    	//}
                    }
                    else {
	                    if (mRowCount > 20) { // ������ �ȴ� ���� event ������ 20�� �����̸� ��� �ؾ� �ϳ�?
	                    	                  // event ������ db���� �о� �;� �Ѵ�
	                    	                  // :�д� ����� ã�� ���ߴ�
	                    	                  //  ��ſ� 
	                    	                  //  mAdapterInfos.getFirst().start <= mForemostEventYearJulainDay && mBackmostEventYearJulainDay <= mAdapterInfos.getLast().end
	                    	                  //  �� Ȱ���� ����
	                    	
	                    	mustNotify = true;
	                    }
                    }
                    */
                    int count = mRowCount;
                    // �������� �߻���
                    // :foremost event�� ���������� ������ �Ǵµ�
                    //  backmose event�� foremost event ������ ���ĵǴ� ���� �߻�
                    /*if (mRowCount > 20) { // ������ �ȴ� ���� event ������ 20�� �����̸� ��� �ؾ� �ϳ�?
  	                  					  // event ������ db���� �о� �;� �Ѵ�
  	                  // :�д� ����� ã�� ���ߴ�
  	                  //  ��ſ� 
  	                  //  mAdapterInfos.getFirst().start <= mForemostEventYearJulainDay && mBackmostEventYearJulainDay <= mAdapterInfos.getLast().end
  	                  //  �� Ȱ���� ����
  	
                    	mustNotify = true;
                    }*/
                    
                    if (mustNotify) {
                    	notifyDataSetChanged();///////////////////////////////////////////////////////
	                    newPosition = findEventPositionNearestTime(goToTime, data.id);
	                    if (newPosition >= 0) {
	                    	// refresh�� �ؾ� �ϴ� ��Ȳ���� fling ���¶�� fling�� �ߴ��ϴ� �� ���� 
	                        if (mListViewScrollState == OnScrollListener.SCROLL_STATE_FLING) {
	                        	mAgendaListView.smoothScrollBy(0, 0);                        	
	                        	if (INFO) Log.i(TAG, "onQueryComplete.b : SCROLL_STATE_FLING");                        	                       
	                        } else if (mListViewScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
	                    		if (INFO) Log.i(TAG, "onQueryComplete.b : SCROLL_STATE_TOUCH_SCROLL");
	                        }	                        
	                        
	                        newPosition = newPosition + OFF_BY_ONE_BUG;
	                        //Log.i("tag", "onQueryComplete : newPosition=" + String.valueOf(newPosition));                        
	                        
	                        mAgendaListView.setSelectionFromTop(newPosition, 0);
	                        Calendar actualTime = GregorianCalendar.getInstance(mTimeZone);
	                        actualTime.setTimeInMillis(goToTime.getTimeInMillis());
	                        if (DEBUGLOG) {
	                            Log.d(TAG, "onQueryComplete: Updating title...");
	                        }
	                        
	                        CalendarController.getInstance(mContext).sendEvent(this,
	                                EventType.UPDATE_TITLE, actualTime, actualTime, -1,
	                                ViewType.CURRENT);
	                    }
                    }
                    
                    if (DEBUGLOG) {
                        Log.e(TAG, "Setting listview to " +
                                "findEventPositionNearestTime: " + (newPosition + OFF_BY_ONE_BUG));
                    }
                }                
                
            } else {
                cursor.close();
                //Log.i("tag", "onQueryComplete : called cursor.close");
            }
            
            
            synchronized (mQueryQueue) {
                // ���⼭ mRowCount ���� Ȯ���� ����
            	// 
                if (cursorSize != 0) {                    
                	//Log.i("tag", "onQueryComplete:Remove the query that just completed");
                	                	                	
                	// Remove the query that just completed
                    QuerySpec x = mQueryQueue.poll();  
                    
                    if (data.queryType == QUERY_TYPE_NEWER) {
                        mNewerRequestsProcessed++;
                    } else if (data.queryType == QUERY_TYPE_OLDER) {
                        mOlderRequestsProcessed++;
                    }
                    
                } else { // CursorSize == 0 
                	// cursor size�� 0�̸�,
                	// �ش� query�� �Ⱓ�� �� �����ؼ� �� �ٷ� �����Ѵ� : �ش� query�� queue list�� head�� ��ġ�� ����
                	
                	// peek : Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty
                    QuerySpec querySpec = mQueryQueue.peek();

                    // �ش� query�� ���� ���� event�� ������ ������� �Ⱓ�� �����ؾ� �Ѵ�
                    // Update Adapter Info with new start and end date range
                    if (!mAdapterInfos.isEmpty()) {
                        DayAdapterInfo first = mAdapterInfos.getFirst();
                        DayAdapterInfo last = mAdapterInfos.getLast();

                        if (first.start - 1 <= querySpec.end && querySpec.start < first.start) {
                            first.start = querySpec.start;
                        }

                        if (querySpec.start <= last.end + 1 && last.end < querySpec.end) {
                            last.end = querySpec.end;
                        }                       
                    } 

                    // ���� ���� query�� ���� �Ⱓ�� �� ����[19700101/20361231]�� �����ϰ� �ִٸ�
                    // �� �̻� query�� �������� �ʴ´�                    
                    boolean retryQuery = true;          
                    if (querySpec.queryType == QUERY_TYPE_OLDER) {
	                    if (querySpec.start == mForemostEventYearJulainDay) {               	
	                    	retryQuery = false;
	                    	DayAdapterInfo last = mAdapterInfos.getLast();
	                    	Log.i("tag", "onQueryComplete:first date == mForemostEventYearJulainDay");
	                    	Calendar temp = GregorianCalendar.getInstance(mTimeZone);
	                    	long endMillis = ETime.getMillisFromJulianDay(last.end, mTimeZone, mFirstDayOfWeek);   
	                    	temp.setTimeInMillis(endMillis);//temp.setJulianDay(last.end);
	                    	
	                    }
                    }
                    else if (querySpec.queryType == QUERY_TYPE_NEWER) {
                    	if (querySpec.end == mBackmostEventYearJulainDay) {                		
                    		retryQuery = false;
                    		DayAdapterInfo first = mAdapterInfos.getFirst();
                    		Calendar temp = GregorianCalendar.getInstance(mTimeZone);                    		
                    		long startMillis = ETime.getMillisFromJulianDay(first.start, mTimeZone, mFirstDayOfWeek);   
                    		temp.setTimeInMillis(startMillis);//temp.setJulianDay(first.start);
                        	
                    	}
                    }   
                    else if (querySpec.queryType == QUERY_TYPE_CLEAN) {
                    	// ó������ �ʾƵ� �Ǵ� ������ �����ΰ�???
                    }
                	
                	
                	if (retryQuery) {
                		querySpec.setRetry(true);
                		Log.i("tag", "onQueryComplete:RetryQuery:id=" + String.valueOf(querySpec.TestId));
                		
	                    // Update query specification with expanded search range
	                    // and maybe rerun query
	                    switch (querySpec.queryType) {
	                        case QUERY_TYPE_OLDER:	           
	                        	querySpec.end = querySpec.start - 1;	
	                            querySpec.start -= MAX_QUERY_RETRY_DURATION;	
	                            
	                            if (querySpec.start < mForemostEventYearJulainDay) {
	    	                    	querySpec.start = mForemostEventYearJulainDay;	
	    	    	            	if (querySpec.end < mForemostEventYearJulainDay) {
	    	    	            		querySpec.end = mForemostEventYearJulainDay;
	    	    	            	}    	            	
	    	    	            }
	                            break;
	                        case QUERY_TYPE_NEWER:	        
	                        	querySpec.start = querySpec.end + 1;
	                            querySpec.end += MAX_QUERY_RETRY_DURATION;
	                            
	                            if (querySpec.end > mBackmostEventYearJulainDay) {
	    	    	        		querySpec.end = mBackmostEventYearJulainDay;
	    	    	            	if (querySpec.start > mBackmostEventYearJulainDay) {
	    	    	            		querySpec.start = mBackmostEventYearJulainDay;
	    	    	            	}	    	            	
	    	    	        	}  
	                            break;
	                        case QUERY_TYPE_CLEAN:	                            
	                            querySpec.start -= MAX_QUERY_RETRY_DURATION / 2;
	                            querySpec.end += MAX_QUERY_RETRY_DURATION / 2;
	                            
	                            if (querySpec.start < mForemostEventYearJulainDay) {
	    	                    	querySpec.start = mForemostEventYearJulainDay;	
	    	    	            	if (querySpec.end < mForemostEventYearJulainDay) {
	    	    	            		querySpec.end = mForemostEventYearJulainDay;
	    	    	            	}    	            	
	    	    	            }
	                            
	                            if (querySpec.end > mBackmostEventYearJulainDay) {
	    	    	        		querySpec.end = mBackmostEventYearJulainDay;
	    	    	            	if (querySpec.start > mBackmostEventYearJulainDay) {
	    	    	            		querySpec.start = mBackmostEventYearJulainDay;
	    	    	            	}	    	            	
	    	    	        	}  
	                            break;
	                    }    	        	                  
                	}
                	else {
                		 mQueryQueue.poll();
                	}                	                    
                }                
                
                // Fire off the next query if any
                Iterator<QuerySpec> it = mQueryQueue.iterator();
                while (it.hasNext()) {
                    QuerySpec queryData = it.next();
                                        
                    // ���� refresh���� queuing �� QUERY_TYPE_OLDER/QUERY_TYPE_NEWER ���
                    // queryData.start, queryData.end�� �������� ���� �ʾ���
                    // queueQuery(0, 0, goToTime, searchQuery, QUERY_TYPE_OLDER, id);
                    // queueQuery(0, 0, goToTime, searchQuery, QUERY_TYPE_NEWER, id);
                    // isInRange���� ��� ó���ϴ°�?
                    // :start�� end ������ 0���� �����ϸ� 
                    //  mAdapterInfos.getFirst().start <= start && end <= mAdapterInfos.getLast().end ���ֿ� ������ �ʴ´�
                    if (queryData.queryType == QUERY_TYPE_CLEAN || !isInRange(queryData.queryType, queryData.start, queryData.end)) {
                        // Query accepted                    	
                        doQuery(queryData);
                        break;////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    } else {                    	
                        // Query rejected
                    	//Log.i("tag", "onQueryComplete:Reject Query:id=" + String.valueOf(queryData.TestId));     
                        it.remove();                       
                        
                        if (DEBUGLOG) Log.e(TAG, "Query rejected. QueueSize:" + mQueryQueue.size());
                    }
                }
                
                // ������[1]�� data.goToTime == null �� ����̴�
                // 2015/10/01�� goToTime�� ��� row count�� 20�̸��̱� ������
                // older type�� query�� �����ϴµ� �� �� goToTime�� null�� �����Ǳ� ������
                // findEventPositionNearestTime�Լ��� ȣ������ �ʴ´�!!!
                if (mQueryQueue.isEmpty()) {
                	int firstVisibilePosition = mAgendaListView.getFirstVisiblePosition();
                	int lastVisiblePosition = mAgendaListView.getLastVisiblePosition();
                	
                	DayAdapterInfo first = mAdapterInfos.getFirst();
                	DayAdapterInfo last = mAdapterInfos.getLast(); 
                	
                	//int start = first.start;
                    //int end = last.end;	   
                	
                	if (firstVisibilePosition < PREFETCH_BOUNDARY) {
                		boolean mustQueryOlderType = true;
	            		if (first.start == mForemostEventYearJulainDay) {
	            			mustQueryOlderType = false;
	                	}       
	            		
	            		if (mustQueryOlderType) {	            			                    
	                        //int queryDuration = last.end - first.start;	
	            			int queryDuration = MAX_QUERY_RETRY_DURATION;
	        	            
	            			QuerySpec queryData = new QuerySpec(QUERY_TYPE_OLDER);	                    	
	            			queryData.end = first.start - 1;
    	                    queryData.start = queryData.end - queryDuration;	                    	
	                    	queryData.id = -1;
	                    	// ������[1]�� �ذ��ϱ� ���ؼ�
	                    	//if (data.goToTime != null)
	                    		//queryData.goToTime.set(data.goToTime);
	                    	
	                    	queryData.setRetry(true);
	                    	
	                    	if (queryData.start < mForemostEventYearJulainDay) {
	                    		queryData.start = mForemostEventYearJulainDay;	
    	    	            	if (queryData.end < mForemostEventYearJulainDay) {
    	    	            		queryData.end = mForemostEventYearJulainDay;
    	    	            	}    	            	
    	    	            }
	                    	
	                    	if(!isInRange(queryData.start, queryData.end))
	                    		queueQueryWithoutBlocking(queryData);	  
	            		}
                	}
                	
                	
                	if (lastVisiblePosition >= (mRowCount - PREFETCH_BOUNDARY)) {
                		boolean mustQueryNewerType = true;
                		if (last.end == mBackmostEventYearJulainDay) {
                			mustQueryNewerType = false;
                    	}   
                		
                		if (mustQueryNewerType) {
                			//int queryDuration = last.end - first.start;	        	                
                			int queryDuration = MAX_QUERY_RETRY_DURATION;
                			
	            			QuerySpec queryData = new QuerySpec(QUERY_TYPE_NEWER);	  
	            			queryData.start = last.end + 1;
		                    queryData.end = queryData.start + queryDuration; 		                    	            			          	
	                    	queryData.id = -1;
	                    	//if (data.goToTime != null)
	                    		//queryData.goToTime.set(data.goToTime);
	                    	queryData.setRetry(true);
	                    	
	                    	if (queryData.end > mBackmostEventYearJulainDay) {
	                    		queryData.end = mBackmostEventYearJulainDay;
    	    	            	if (queryData.start > mBackmostEventYearJulainDay) {
    	    	            		queryData.start = mBackmostEventYearJulainDay;
    	    	            	}	    	            	
    	    	        	}  
	                    	
	                    	if(!isInRange(queryData.start, queryData.end))
	                    		queueQueryWithoutBlocking(queryData);	                			
                		}
                	} 	        	        	               	             	
                }                                
            }
            
            if (BASICLOG) {
                for (DayAdapterInfo info3 : mAdapterInfos) {
                    Log.e(TAG, "> " + info3.toString());
                }
            }
            
            /*
            Time startTime = new Time(mTimeZone);
            Time endTime = new Time(mTimeZone);
            startTime.setJulianDay(mAdapterInfos.getFirst().start);                
            endTime.setJulianDay(mAdapterInfos.getLast().end);
            Log.i("tag", "Entire start=" + startTime.format2445());
            Log.i("tag", "Entire end=" + endTime.format2445());   
            
            Log.i("tag", "(-)onQueryComplete");
            */
        }

        /*
         * Update the adapter info array with a the new cursor. Close out old
         * cursors as needed.
         *
         * @return number of rows removed from the beginning
         */
        private int processNewCursor(QuerySpec data, Cursor cursor) {
            synchronized (mAdapterInfos) {
                // Remove adapter info's from adapterInfos as needed
                DayAdapterInfo info = pruneAdapterInfo(data.queryType);
                
                int listPositionOffset = 0;
                if (info == null) {   
                	// ���⼭ �츮�� agenda fragment ���� ��ü��
                	// central activity���� search activity������ �����ؾ� �Ѵ�
                	// �̴� ��ǻ�,
                	// AgendaByDayAdapter.calculateDays���� ó���ϴ� �Ⱓ�߿� today�� ���ԵǾ� ���� ���,
                	// central activity���� ������ agenda fragment����
                	// today�� �ƹ��� event�� �������� �ʴ� ��쿡 ���� todday header�� �߰��ϴ� �ڵ带 ȸ���ϱ� ���� �κ��̴�
                    info = new DayAdapterInfo(mContext, mUsedForSearch);
                } else {                    
                    listPositionOffset = -info.size;
                }

                // Setup adapter info
                info.start = data.start;
                info.end = data.end;
                info.cursor = cursor;////////////////////////////////////////
                info.dayAdapter.changeCursor(info);
                info.size = info.dayAdapter.getCount();

                // Insert into adapterInfos
                // listPositionOffset�� ��� �뵵�� list item�� shift�� �ִ�
                // *mAdapterInfos.isEmpty()�� ��쿡 �� shift�� �ʿ��Ѱ�?
                // ;��???
                // *data.end <= mAdapterInfos.getFirst().start �� ����, 
                // ;cursor�� ������ �ִ� event���� ���� older�� event�鿡 ���� ������ ������ �����Ƿ�
                //  ���� ������ DayAdapterInfo�� list�� ù��°�� �����ؾ� �Ѵ�
                if (mAdapterInfos.isEmpty() || data.end <= mAdapterInfos.getFirst().start) {
                    mAdapterInfos.addFirst(info);
                    listPositionOffset += info.size;
                    if (INFO) Log.i(TAG, "processNewCursor listPositionOffsetB=" + listPositionOffset);
                
                } else {
                    mAdapterInfos.addLast(info);
                }

                // Update offsets in adapterInfos
                mRowCount = 0;
                for (DayAdapterInfo info3 : mAdapterInfos) {
                    info3.offset = mRowCount;
                    mRowCount += info3.size;
                }
                mLastUsedInfo = null;

                return listPositionOffset;
            }
        }
    }

    static String getViewTitle(View x) {
        String title = "";
        if (x != null) {
            Object yy = x.getTag();
            if (yy instanceof AgendaAdapter.ViewHolder) {
                TextView tv = ((AgendaAdapter.ViewHolder) yy).title;
                if (tv != null) {
                    title = (String) tv.getText();
                }
            } else if (yy != null) {
                TextView dateView = ((AgendaByDayAdapter.ViewHolder) yy).dateView;
                if (dateView != null) {
                    title = (String) dateView.getText();
                }
            }
        }
        return title;
    }

    
    public void onResume() {
    	mOccurenceWaitingOlderQueryUnderScrolling = false;
    	mOccurenceWaitingNewerQueryUnderScrolling = false;
        mTZUpdater.run();
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        mHideDeclined = hideDeclined;
    }
    
    public void setCleanAdapterState() {
    	mCleanQueryInitiated = false;
    	
    	pruneAdapterInfo(QUERY_TYPE_CLEAN);
    	
    	mLastUsedInfo = null;
    	
    	if (mQueryHandler != null) {
    		mQueryHandler.cancelOperation(0);
    	}
    	
    	notifyDataSetChanged();
    }

    /*
    public void setSelectedView(View v) {
        if (v != null) {
            Object vh = v.getTag();
            if (vh instanceof AgendaAdapter.ViewHolder) {
                mSelectedVH = (AgendaAdapter.ViewHolder) vh;
                if (mSelectedInstanceId != mSelectedVH.instanceId) {
                    mSelectedInstanceId = mSelectedVH.instanceId;
                    notifyDataSetChanged();
                }
            }
        }
    }
	*/
    
    /*
    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return mSelectedVH;
    }
	    
    public long getSelectedInstanceId() {
        return mSelectedInstanceId;
    }

    public void setSelectedInstanceId(long selectedInstanceId) {
        mSelectedInstanceId = selectedInstanceId;
        mSelectedVH = null;
    }
	*/
    
    private long findInstanceIdFromPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getInstanceId(position - info.offset);
        }
        return -1;
    }

    private long findStartTimeFromPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getStartTime(position - info.offset);
        }
        return -1;
    }


    private Cursor getCursorByPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.cursor;
        }
        return null;
    }

    private int getCursorPositionByPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getCursorPosition(position - info.offset);
        }
        return -1;
    }

    // Implementation of HeaderIndexer interface for StickyHeeaderListView

    // Returns the location of the day header of a specific event specified in the position
    // in the adapter
    /*
    @Override
    public int getHeaderPositionFromItemPosition(int position) {

        // For phone configuration, return -1 so there will be no sticky header
        if (!mIsTabletConfig) {
            return -1;
        }

        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int pos = info.dayAdapter.getHeaderPosition(position - info.offset);
            return (pos != -1)?(pos + info.offset):-1;
        }
        return -1;
    }

    // Returns the number of events for a specific day header
    @Override
    public int getHeaderItemsNumber(int headerPosition) {
        if (headerPosition < 0 || !mIsTabletConfig) {
            return -1;
        }
        DayAdapterInfo info = getAdapterInfoByPosition(headerPosition);
        if (info != null) {
            return info.dayAdapter.getHeaderItemsCount(headerPosition - info.offset);
        }
        return -1;
    }

    @Override
    public void OnHeaderHeightChanged(int height) {
        //mStickyHeaderSize = height;
    }

    public int getStickyHeaderHeight() {
        return 0;
    }
    */

    public void setScrollState(int state) {
        mListViewScrollState = state;
    }  
    
    public int getScrollState() {
        return mListViewScrollState;
    }
    
    
    LayoutParams mEventItemLayoutParams;
    LayoutParams mEventItemTimeContainerParams;
    LayoutParams mEventItemDividerParams;
    LayoutParams mEventItemTitleLocationContainerParams;
    LayoutParams mEventItemTimeTextViewParams;
    LayoutParams mEventItemTitleLocationTextViewParams;
    
    private final Formatter mEventItemFormatter;
    private final StringBuilder mEventItemStringBuilder;
    
    public void makeEventItemLayoutDimension() {
    	calcEventItemDimension();
    	calcEventItemLeftPadding();
    	calcEventTimeTextWidth();
    	calcEventItemViewsDimension();
    	
    	//makeEventItemLayoutParams();
    	//makeEventItemViewsLayoutParams();
    }
    
    public void calcEventItemDimension() {
    	/*WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	mEventItemWidth = size.x;*/
    	
    	mEventItemWidth = mResources.getDisplayMetrics().widthPixels;
    	
    	int deviceHeight = mResources.getDisplayMetrics().heightPixels;
        int StatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
        if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
        int upperActionBarHeight = (int)mResources.getDimension(R.dimen.calendar_view_upper_actionbar_height);    	
        int mDayHeaderHeight = (int)mResources.getDimension(R.dimen.day_header_height);
        int lowerActionBarHeight = (int)mResources.getDimension(R.dimen.calendar_view_lower_actionbar_height);

        int anticipationMonthListViewHeight = deviceHeight - StatusBarHeight - upperActionBarHeight - mDayHeaderHeight - lowerActionBarHeight;   
        int monthListViewNormalWeekHeight = (int) (anticipationMonthListViewHeight * MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT);
        int monthListViewNormalWeekHeightInEPMode = (int) (monthListViewNormalWeekHeight * MonthListView.EEVM_WEEK_HEIGHT_RATIO_BY_NM_WEEK_HEIGHT);            
        mEventItemHeight = monthListViewNormalWeekHeightInEPMode; 
        mEventDayHeaderHeight = (int) (mEventItemHeight * EVENT_DAY_HEADER_HEIGHT_RATIO);
    }    
    
    public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = mResources.getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = mResources.getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
    
    public void calcEventItemLeftPadding() {
    	mEventItemTimeContainerLeftPadding = (int) (mEventItemWidth * 0.05f);
    	mEventItemTitleLocaitonContainerLeftPadding = (int) (mEventItemTimeContainerLeftPadding * 0.6f);	
    }
    
    public void calcEventItemViewsDimension() {
    	mEventItemTimeTextViewHeight = mEventItemHeight / 2;
    	mEventItemTitleTextViewHeight = mEventItemTimeTextViewHeight;
		mEventItemTimeContainerWidth  = (int) (mEventItemTimeContainerLeftPadding + mEventItemTimeTextViewWidth);
		
		mEventItemTopDividerHeight = (int) mResources.getDimension(R.dimen.eventItemLayoutUnderLineHeight);
        
		mEventItemTimeContainerHeight = mEventItemHeight - mEventItemTopDividerHeight;
		mEventItemTitleLocationContainerHeight = mEventItemTimeContainerHeight;
		
		mEventItemDividerTopMargin = (int) mResources.getDimension(R.dimen.container_divider_top_margin);
    	mEventItemDividerWidth = (int) mResources.getDimension(R.dimen.container_divider_width);
    	mEventItemDividerHeight = mEventItemHeight - (mEventItemDividerTopMargin * 2);
    }
    
    public void calcEventTimeTextWidth() {    	
        
		TextView calTimeTextWidth = new TextView(mContext);
        Paint calTimeTextWidthPaint = calTimeTextWidth.getPaint();		
        calTimeTextWidthPaint.setTextSize(mResources.getDimension(R.dimen.meev_starttime_text_size));	     
        
        int flags = 0;
        String amTime;
                
        long begin = 0;
        
        flags = ETime.FORMAT_SHOW_TIME;
        String tzString = Utils.getTimeZone(mContext, mTZUpdater);
        TimeZone timeZone = TimeZone.getTimeZone(tzString);
        Calendar tempTime = GregorianCalendar.getInstance(timeZone);
        long now = System.currentTimeMillis();
        tempTime.setTimeInMillis(now);
        
        begin = Utils.getNextMidnight(tempTime, tempTime.getTimeInMillis(), tzString);        
        mEventItemStringBuilder.setLength(0);
        amTime = ETime.formatDateTimeRange(mContext, mEventItemFormatter, begin, begin, flags, tzString)
                .toString();
        
        /*
        begin = Utils.getNextMidnight(tempTime, tempTime.toMillis(true), tzString);
        mStringBuilder.setLength(0);
        pmTime = DateUtils.formatDateRange(mContext, mFormatter, begin, begin, flags, tzString)
                .toString();
        */
        
        String padding = "0"; // padding ũ�⸦ '0'�� char�� ���Ѵ�
        amTime = padding + amTime + padding;
        float amTimeStrSize = calTimeTextWidthPaint.measureText(amTime); 
        mEventItemTimeTextViewWidth = (int) (amTimeStrSize + 0.5f);
        
		/*float pmTimeStrSize = calTimeTextWidthPaint.measureText(pmTime);  
		
		if (amTimeStrSize > pmTimeStrSize)
			mTimeTextViewWidth = (int) (amTimeStrSize + 0.5f);
		else if (pmTimeStrSize > amTimeStrSize)
			mTimeTextViewWidth = (int) (pmTimeStrSize + 0.5f);
		else
			mTimeTextViewWidth = (int) (amTimeStrSize + 0.5f);
		*/
	}
    
    public void makeEventItemDimensionParams() {    	        
        mEventItemDimensionParams = new HashMap<String, Integer>();
        
        mEventItemDimensionParams.put(EVENT_DAYHEADER_HEIGHT, mEventDayHeaderHeight);
        mEventItemDimensionParams.put(EVENT_ITEM_WIDTH, mEventItemWidth);
        mEventItemDimensionParams.put(EVENT_ITEM_HEIGHT, mEventItemHeight);
        mEventItemDimensionParams.put(EVENT_ITEM_TIMECONTAINER_WIDTH, mEventItemTimeContainerWidth);
        mEventItemDimensionParams.put(EVENT_ITEM_TIMECONTAINER_HEIGHT, mEventItemTimeContainerHeight);
        //mEventItemDimensionParams.put(EVENT_ITEM_TITLELOCATIONCONTAINER_WIDTH, );
        mEventItemDimensionParams.put(EVENT_ITEM_TITLELOCATIONCONTAINER_HEIGHT, mEventItemTitleLocationContainerHeight);
        mEventItemDimensionParams.put(EVENT_ITEM_TIMECONTAINER_LEFTPADDING, mEventItemTimeContainerLeftPadding);
        mEventItemDimensionParams.put(EVENT_ITEM_TITLELOCATIONCONTAINER_LEFTPADDING, mEventItemTitleLocaitonContainerLeftPadding);
        mEventItemDimensionParams.put(EVENT_ITEM_TIME_TEXTVIEW_WIDTH, mEventItemTimeTextViewWidth);
        mEventItemDimensionParams.put(EVENT_ITEM_TIME_TEXTVIEW_HEIGHT, mEventItemTimeTextViewHeight);
        mEventItemDimensionParams.put(EVENT_ITEM_TITLE_TEXTVIEW_HEIGHT, mEventItemTitleTextViewHeight);
        mEventItemDimensionParams.put(EVENT_ITEM_DIVIDER_WIDTH, mEventItemDividerWidth);
        mEventItemDimensionParams.put(EVENT_ITEM_DIVIDER_HEIGHT, mEventItemDividerHeight);
        mEventItemDimensionParams.put(EVENT_ITEM_DIVIDER_TOPMARGIN, mEventItemDividerTopMargin);
        mEventItemDimensionParams.put(EVENT_ITEM_TOPDIVIDER_HIGHT, mEventItemTopDividerHeight);        
    }
    
    public int getEventDayHeaderHeight() {
    	return mEventDayHeaderHeight;
    }
    
    public int getEventItemHeight() {
    	return mEventItemHeight;
    }
    
    // mEventDayHeaderBottomLineHeight
    public int getEventItemTopDividerHeight() {
    	return mEventItemTopDividerHeight;
    }
    
    
    public void makeEventItemLayoutParams() {
    	mEventItemLayoutParams = new LayoutParams(mEventItemWidth, mEventItemHeight);
    }
    
    public void makeEventItemViewsLayoutParams() {
    	mEventItemTimeContainerParams = new LayoutParams(mEventItemTimeContainerWidth, mEventItemTimeContainerHeight);    	
    	
		mEventItemDividerParams = new LayoutParams(mEventItemDividerWidth, mEventItemDividerHeight);
				
		// width ���� �̻��ϴ� �ٽ� �ѹ� üũ����
		mEventItemTitleLocationContainerParams = new LayoutParams(LayoutParams.MATCH_PARENT, mEventItemTitleLocationContainerHeight);
		
		mEventItemTimeTextViewParams = new LayoutParams(mEventItemTimeTextViewWidth, mEventItemTimeTextViewHeight);	
		
		mEventItemTitleLocationTextViewParams = new LayoutParams(LayoutParams.MATCH_PARENT, mEventItemTitleTextViewHeight);			
    }
    
    ArrayList<EventCursors> mEventCursors = new ArrayList<EventCursors>();
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
}
