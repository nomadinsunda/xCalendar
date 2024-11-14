package com.intheeast.month;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
//import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.BaseAdapter;
import android.widget.AbsListView.LayoutParams;

public class MonthAdapter extends BaseAdapter implements OnTouchListener {

	private static final String TAG = "MonthAdapter";
	private static boolean INFO = true;
	
    /**
     * The number of weeks to display at a time.
     */
    public static final String WEEK_PARAMS_NUM_WEEKS = "num_weeks";
    /**
     * Which month should be in focus currently.
     */
    //public static final String WEEK_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * Whether the week number should be shown. Non-zero to show them.
     */
    public static final String WEEK_PARAMS_SHOW_WEEK = "week_numbers";

    public static final String WEEK_PARAMS_WEEK_START = "week_start";
    /**
     * The Julian day to highlight as selected.
     */
    public static final String WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED = "current_month_julianday_displayed";
    /**
     * How many days of the week to display [1-7].
     */
    public static final String WEEK_PARAMS_DAYS_PER_WEEK = "days_per_week";
    
    public static final String WEEK_PARAMS_SELECTED_JULIAN_DAYS_IN_EEMODE = "selected_day_in_eemode";
    
    

    public static final int WEEK_COUNT = CalendarController.MAX_CALENDAR_WEEK
            - CalendarController.MIN_CALENDAR_WEEK;
    public static int DEFAULT_NUM_WEEKS = 6;
    public static int DEFAULT_CURRENT_MONTH = 0;
    public static int DEFAULT_DAYS_PER_WEEK = 7;
    public static int DEFAULT_WEEK_HEIGHT = 32;
    public static int WEEK_7_OVERHANG_HEIGHT = 7;

    public static float mScale = 0;
    public Context mContext;
    // The day to highlight as selected
    public Calendar mCurrentMonthTimeDisplayed;
    public Calendar mTimeOfFirstDayOfThisWeek;
    public Calendar mTimeOfLastDayOfThisWeek;
    // The week since 1970 that the selected day is in
    
    
    // mSelectedDayInEEMode�� ������
    // ���� Event listView�� ������ �̺�Ʈ���� ��¥�̴�!!!
    public Calendar mSelectedDayInEEMode;
    public int mSelectedWeekInEEMode;
    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    public static int mFirstDayOfWeek;
    public boolean mShowWeekNumber = false;
    public GestureDetector mGestureDetector;
    public int mNumWeeks = DEFAULT_NUM_WEEKS;
    public int mDaysPerWeek = DEFAULT_DAYS_PER_WEEK;
    //public int mCurrentMonthDisplayed = DEFAULT_CURRENT_MONTH;
    
    public String mHomeTzId;
    public TimeZone mHomeTimeZone;
    MonthWeekView mClickedViewInEPMode = null;
    float mClickedXLocationInEPMode;                // Used to find which day was clicked
    float mClickedYLocationInEPMode;                // Used to find which day was clicked

    
    protected static int DEFAULT_QUERY_DAYS = 7 * 8; // 8 weeks
    private static final long ANIMATE_TODAY_TIMEOUT = 500;

    protected CalendarController mController;
    
    protected Calendar mTempTime;
    protected Calendar mToday;
    protected int mFirstJulianDay;
    protected int mQueryDays;
    
    protected int mOrientation = Configuration.ORIENTATION_LANDSCAPE;
    private final boolean mShowAgendaWithMonth;

    protected ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
    protected ArrayList<Event> mEvents = null;

    private boolean mAnimateChangeTargetMonthInEEMode = false;
    private long mAnimateTime = 0;

    
    MonthWeekView mDownedView = null;
    MonthWeekView mClickedView = null;
    MonthWeekView mPreviousClickedView = null;
    MonthWeekView mSingleTapUpView;
    MonthWeekView mLongClickedView;

    float mClickedXLocation;                // Used to find which day was clicked
    float mClickedYLocation;                // Used to find which day was clicked
    long mClickTime;                        // Used to calculate minimum click animation time
    // Used to insure minimal time for seeing the click animation before switching views
    private static final int mOnTapDelay = 100;
    // Minimal time for a down touch action before stating the click animation, this insures that
    // there is no click animation on flings
    private static int mOnDownDelay;
    private static int mTotalClickDelay;
    // Minimal distance to move the finger in order to cancel the click animation
    private static float mMovedPixelToCancel; 
    
    
    ECalendarApplication mApp;
    Activity mActivity;
    public MonthAdapter(ECalendarApplication app, Context context, Activity activity, HashMap<String, Integer> params) {
    	mApp = app;
    	mContext = context;
    	mActivity = activity;

        // Get default week start based on locale, subtracting one for use with android Time.
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        mFirstDayOfWeek = cal.getFirstDayOfWeek();

        if (mScale == 0) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_7_OVERHANG_HEIGHT *= mScale;
            }
        }
        
        init();
        
        updateParams(params);
        
        mShowAgendaWithMonth = Utils.getConfigBool(context, R.bool.show_agenda_with_month);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        mMovedPixelToCancel = vc.getScaledTouchSlop();
        mTotalClickDelay = mOnDownDelay + mOnTapDelay;
    }
    
    /**
     * Set up the gesture detector and selected time
     */
    public void init() {
                     
        mGestureDetector = new GestureDetector(mContext, new CalendarGestureListener());
        mController = CalendarController.getInstance(mActivity);//CalendarController.getInstance(mContext);
        mHomeTzId = Utils.getTimeZone(mContext, null);
        mHomeTimeZone = TimeZone.getTimeZone(mHomeTzId);
        //mCurrentMonthTimeDisplayed = ETime.switchTimezone(mCurrentMonthTimeDisplayed, mHomeTimeZone);//mCurrentMonthTimeDisplayed.switchTimezone(mHomeTimeZone);
        mCurrentMonthTimeDisplayed = GregorianCalendar.getInstance(mHomeTimeZone);//new Time();
        mTimeOfFirstDayOfThisWeek = GregorianCalendar.getInstance(mHomeTimeZone);
        mTimeOfLastDayOfThisWeek = GregorianCalendar.getInstance(mHomeTimeZone);
        
        mToday = GregorianCalendar.getInstance(mHomeTimeZone);//new Time(mHomeTimeZone);
        mToday.setTimeInMillis(System.currentTimeMillis());
        mSelectedDayInEEMode = GregorianCalendar.getInstance(mHomeTimeZone);//new Time(mHomeTimeZone);
        mTempTime = GregorianCalendar.getInstance(mHomeTimeZone);//new Time(mHomeTimeZone);
        
        
    }
    
    /**
     * Parse the parameters and set any necessary fields. See
     * {@link #WEEK_PARAMS_NUM_WEEKS} for parameter details.
     *
     * @param params A list of parameters for this adapter
     */
    public void updateParams(HashMap<String, Integer> params) {
        if (params == null) {
            Log.e(TAG, "WeekParameters are null! Cannot update adapter.");
            return;
        }
        
        if (params.containsKey(WEEK_PARAMS_NUM_WEEKS)) {
            mNumWeeks = params.get(WEEK_PARAMS_NUM_WEEKS);
        }
        if (params.containsKey(WEEK_PARAMS_SHOW_WEEK)) {
            mShowWeekNumber = params.get(WEEK_PARAMS_SHOW_WEEK) != 0;
        }
        if (params.containsKey(WEEK_PARAMS_WEEK_START)) {
            mFirstDayOfWeek = params.get(WEEK_PARAMS_WEEK_START);
        }
        if (params.containsKey(WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED)) {
            int julianDay = params.get(WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED);
            long julianDayMills = ETime.getMillisFromJulianDay(julianDay, mHomeTimeZone, mFirstDayOfWeek);
            mCurrentMonthTimeDisplayed.setTimeInMillis(julianDayMills);//mCurrentMonthTimeDisplayed.setJulianDay(julianDay);            
        }
        if (params.containsKey(WEEK_PARAMS_SELECTED_JULIAN_DAYS_IN_EEMODE)) {
        	int selectedJulianDay = params.get(WEEK_PARAMS_SELECTED_JULIAN_DAYS_IN_EEMODE);
        	long selectedJulianDayMills = ETime.getMillisFromJulianDay(selectedJulianDay, mHomeTimeZone, mFirstDayOfWeek);
        	mSelectedDayInEEMode.setTimeInMillis(selectedJulianDayMills);//mSelectedDayInEEMode.setJulianDay(selectedJulianDay);
        	mSelectedWeekInEEMode = ETime.getWeeksSinceEcalendarEpochFromMillis(selectedJulianDayMills, mHomeTimeZone, mFirstDayOfWeek);//mSelectedWeekInEEMode = Utils.getWeeksSinceEpochFromJulianDay(selectedJulianDay, mFirstDayOfWeek);
        }
        if (params.containsKey(WEEK_PARAMS_DAYS_PER_WEEK)) {
            mDaysPerWeek = params.get(WEEK_PARAMS_DAYS_PER_WEEK);
        }
                
        refresh();
    }
    
    public void refresh() {    	
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mShowWeekNumber = Utils.getShowWeekNumber(mContext);
        mHomeTzId = Utils.getTimeZone(mContext, null);
        mHomeTimeZone = TimeZone.getTimeZone(mHomeTzId);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        updateTimeZones();
        notifyDataSetChanged();        
    }
    
    private void updateTimeZones() {
    	mCurrentMonthTimeDisplayed = ETime.switchTimezone(mCurrentMonthTimeDisplayed, mHomeTimeZone);//mCurrentMonthTimeDisplayed.timezone = mHomeTimeZone;    	
    	mTimeOfFirstDayOfThisWeek = ETime.switchTimezone(mTimeOfFirstDayOfThisWeek, mHomeTimeZone);
    	mTimeOfLastDayOfThisWeek = ETime.switchTimezone(mTimeOfFirstDayOfThisWeek, mHomeTimeZone);
    	
    	mToday = ETime.switchTimezone(mToday, mHomeTimeZone);//mToday.timezone = mHomeTimeZone;
        mToday.setTimeInMillis(System.currentTimeMillis());//mToday.setToNow();
        mTempTime = ETime.switchTimezone(mTempTime, mHomeTimeZone);//mTempTime.switchTimezone(mHomeTimeZone);
    }
    
    // Callers
    // -MonthFragment.setMonthDisplayed    
    // ��ǻ� �ϴ� ������ MonthAdapter���� ����ϴ� Time�� timezone ������ �����ϴ� ���ҹۿ� ����...
    // :���Ŀ� ��������...
    public void setCurrentMonthTimeDisplayed(Calendar currentMonthTime) {
    	mCurrentMonthTimeDisplayed.setTimeInMillis(currentMonthTime.getTimeInMillis());
        
    }
    
    // callers
    // -setSelectedDayAlphaAnimUnderNoExistenceInEEMode
    // -MonthFragment.hasMonthListViewTargetMonthWeekDay
    // -MonthFragment.goToToday
    // -MonthFragment.computeGoToTodayDownwardFlingInEEMode
    // -MonthFragment.computeGoToTodayDownwardFlingInEEMode
    // -MonthFragment.computeGoToTodayUpwardFlingInEEMode
    // -MonthFragment.computeGoToTodayUpwardFlingInEEMode
    // -MonthListView.hasMonthListViewTargetMonthWeekDay
    public void setSelectedDayInEEMode(long selectedTimeMills) {
    	mSelectedDayInEEMode.setTimeInMillis(selectedTimeMills);    	
        //long millis = mSelectedDayInEEMode.getTimeInMillis();
        //long gmtoff = mSelectedDayInEEMode.getTimeZone().getRawOffset();
        //int julianDay = ETime.getJulianDay(millis, gmtoff);
        mSelectedWeekInEEMode = ETime.getWeeksSinceEcalendarEpochFromMillis(selectedTimeMills, mHomeTimeZone, mFirstDayOfWeek);
        //mSelectedWeekInEEMode = Utils.getWeeksSinceEpochFromJulianDay(julianDay, mFirstDayOfWeek);
    }
    
    // callers
    // -onTouch
    public void setSelectedDayInEEMode(long millis, int weekNumber) {
    	//long selectedJulianDayMills = ETime.getMillisFromJulianDay(selectedJulianDay, mSelectedDayInEEMode.getTimeZone());
    	mSelectedDayInEEMode.setTimeInMillis(millis);//mSelectedDayInEEMode.setJulianDay(selectedJulianDay);
    	mSelectedWeekInEEMode = weekNumber;//mSelectedWeekInEEMode = Utils.getWeeksSinceEpochFromJulianDay(selectedJulianDay, mFirstDayOfWeek);
    }
    
    // callers
    // -MonthFragment.adjustmentMonthListTopItemPositionAfterScrollingInEEMode
    // -MonthFragment.adjustmentMonthListTopItemPositionFlingInEEMode
    public Calendar getSelectedDayInEEMode() {
        return mSelectedDayInEEMode;
    }     
    
	@Override
	public int getCount() {
		//Log.i(TAG, "getCount ");
		return WEEK_COUNT;
	}

	@Override
    public Object getItem(int position) {
        return null;
    }

	@Override
    public long getItemId(int position) {
        return position;
    }
	
	public void setOnlyEvents(int firstJulianDay, int numDays, ArrayList<Event> events) {
		mEvents = events;
        mFirstJulianDay = firstJulianDay;
        mQueryDays = numDays;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        if (events == null || events.size() == 0) {
            
            mEventDayList = eventDayList;
            refresh();
            return;
        }

        // Compute the new set of days with events
        for (Event event : events) {
            int startDay = event.startDay - mFirstJulianDay;
            int endDay = event.endDay - mFirstJulianDay + 1;
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
        
        mEventDayList = eventDayList;        
	}
	
    public void setEvents(int firstJulianDay, int numDays, ArrayList<Event> events) {
        
        mEvents = events;
        mFirstJulianDay = firstJulianDay;
        mQueryDays = numDays;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        if (events == null || events.size() == 0) {
            
            mEventDayList = eventDayList;
            refresh();
            return;
        }

        // Compute the new set of days with events
        for (Event event : events) {
            int startDay = event.startDay - mFirstJulianDay;
            int endDay = event.endDay - mFirstJulianDay + 1;
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
        
        mEventDayList = eventDayList;        
        
        refresh();
    } 
    
    public ArrayList<ArrayList<Event>> getEventDayList() {
    	return mEventDayList;
    }
    
    public ArrayList<Event> getEvents() {
    	return mEvents;
    }
     
        
    /*
     * (non-Javadoc)
     * @see com.intheeast.month.SimpleWeeksAdapter#getView(int, android.view.View, android.view.ViewGroup)
     * getView() is called for each item in the list you pass to your adapter. 
     * It is called when you set adapter. 
     * When getView() is finished the next line after setAdapter(myAdapter) is called. 
     * In order to debug getView() you must toggle a breakpoint on it because you can't step into getView() from setAdapter(myAdapter). 
     * getView() is also called after notifyDataSetChanged() and on scrolling.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {    	
    	    	
    	//if (INFO) Log.i(TAG, "getView");
    	
    	MonthWeekView v;
    	
    	if (mListView.getExpandEventViewModeState()) {   		
    		v = getViewExpandEventViewMode(position, convertView, parent);    		
    	}
    	else {    		
    		v = getViewNormalMode(position, convertView, parent);    
    	}

        return v;
    }
    
    public static int calWeekPattern(int position, String tzId, int firstDayOfWeek) {
    	TimeZone timezone = TimeZone.getTimeZone(tzId);
    	int weekPattern = MonthWeekView.NORMAL_WEEK_PATTERN;
    	
    	int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//Time.getJulianMondayFromWeeksSinceEpoch(position);
		Calendar timeOfFirstDayOfThisWeek = GregorianCalendar.getInstance(timezone);//new Time(timezone);
		timeOfFirstDayOfThisWeek.setFirstDayOfWeek(firstDayOfWeek);
		long julianMondayMills = ETime.getMillisFromJulianDay(julianMonday, timezone, mFirstDayOfWeek);
		timeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMills);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);
		ETime.adjustStartDayInWeek(timeOfFirstDayOfThisWeek);		
		
		Calendar timeOfLastDayOfThisWeek = GregorianCalendar.getInstance(timezone);//new Time(timezone);		
		timeOfLastDayOfThisWeek.setTimeInMillis(timeOfFirstDayOfThisWeek.getTimeInMillis());//timeOfLastDayOfThisWeek.setJulianDay(firstJulianDayOfThisWeek + 6);
		timeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);
		
		int maxMonthDay = timeOfLastDayOfThisWeek.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {
			weekPattern = MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN;
		}
		else {
			if ( (timeOfFirstDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == 1) /*&& (timeOfFirstDayOfThisWeek.weekDay == mFirstDayOfWeek)*/ ) {
				weekPattern = MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN; 
			}
			else if (timeOfLastDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == maxMonthDay) {	    	
				weekPattern = MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN;
			}
		// else if ... LAST_WEEK_SECTION_HEIGHT �� ������ ���� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN��!!!
		// �׷��Ƿ� ���� ó���� �ʿ䰡 ����
		}
		
		return weekPattern;
    }
    
    
    @SuppressWarnings("unchecked")
	public MonthWeekView getViewNormalMode(int position, View convertView, ViewGroup parent) {    	
		    	
		int week_pattern = MonthWeekView.NORMAL_WEEK_PATTERN;
		int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//Time.getJulianMondayFromWeeksSinceEpoch(position);
				
		long julianMondayMillis = ETime.getMillisFromJulianDay(julianMonday, mCurrentMonthTimeDisplayed.getTimeZone(), mFirstDayOfWeek);
		mTimeOfFirstDayOfThisWeek.setFirstDayOfWeek(mFirstDayOfWeek);
		mTimeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMillis);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);
		ETime.adjustStartDayInWeek(mTimeOfFirstDayOfThisWeek);		    
		
		mTimeOfLastDayOfThisWeek.setFirstDayOfWeek(mFirstDayOfWeek);
		mTimeOfLastDayOfThisWeek.setTimeInMillis(mTimeOfFirstDayOfThisWeek.getTimeInMillis());
		mTimeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);	
		
		int maxMonthDay = mTimeOfLastDayOfThisWeek.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		if (mTimeOfFirstDayOfThisWeek.get(Calendar.MONTH) != mTimeOfLastDayOfThisWeek.get(Calendar.MONTH)) {
			week_pattern = MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN;
		}
		else {
			if ( (mTimeOfFirstDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == 1) /*&& (timeOfFirstDayOfThisWeek.weekDay == mFirstDayOfWeek)*/ ) {
				week_pattern = MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN; 
			}
			else if (mTimeOfLastDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == maxMonthDay) {	    	
				// �ش� ������ �ϳ⿡ �� �ι� ���� �ۿ� ������ �ʴ´�.
				// �׷��Ƿ� convertview reuse�� ���� �״��� �Ű澲�� �ʾƵ� �� �� ���� 
				week_pattern = MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN;
			}
			// else if ... LAST_WEEK_SECTION_HEIGHT �� ������ ���� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN��!!!
			// �׷��Ƿ� ���� ó���� �ʿ䰡 ����
		}
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		MonthWeekView v;
		// height �κ��� �߸��Ǿ���!!!
		// : �ƴϴ� MonthWeekView::onMeasure���� 
		//   setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight)�� ȣ���Ѵ�
		//   2nd para�� mHeight�� VIEW_PARAMS_HEIGHT�� ���� �����ȴ�
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		// drawingParams�� ����� �������� ������ dumy value�� �����ǹǷ�
		// �Ʒ� �ּ� ó���� �κп� �����ؾ� ��!!!
		HashMap<String, Integer> drawingParams = null;
		
		if (convertView != null) {
			v = (MonthWeekView) convertView;		
			drawingParams = (HashMap<String, Integer>) v.getTag();
			int viewMode = 0;
			if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_MODE)) {
				viewMode = drawingParams.get(MonthWeekView.VIEW_PARAMS_MODE);
			}
			
			if (viewMode == MonthWeekView.NORMAL_MODE_VIEW_PATTERN) {
			
				int weekPatternOfConvertView = MonthWeekView.NORMAL_WEEK_PATTERN;
				if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN)) {
					weekPatternOfConvertView = drawingParams.get(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN);
				}
			
				if (week_pattern != weekPatternOfConvertView) {
					v = new MonthWeekView(mContext);
					// �Ʒ� �ڵ带 �������� ������, ��Ȥ dummy value�� �����Ǿ
					// fling ��, ��Ȯ�ϰ� distance�� ������ �� ���� �ȴ�
					drawingParams.clear();
					drawingParams = null;
				}			
			}
		} else {
			v = new MonthWeekView(mContext);
		}
		
		if (drawingParams == null) {
			drawingParams = new HashMap<String, Integer>();
		}        
		drawingParams.clear();
		
		v.setLayoutParams(params);////////////////////////////////////////////////////////////////////////////
		v.setClickable(true);/////////////////////////////////////////////////////////////////////////////////
		v.setOnTouchListener(this);///////////////////////////////////////////////////////////////////////////
		
		
		int weekHeight = 0;
		if (week_pattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
		
			weekHeight = mListView.getNMTwoMonthWeekHeight(); //mTwoMonthWeekHeight;        	
		}
		else if (week_pattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
		
			weekHeight = mListView.getNMFirstWeekHeight();//mFfweekHeight;      	
		}
		/*else if (week_pattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {
		
			weekHeight = mListView.getNMLastWeekHeight();//mLastWeekHeight;      
		}*/
		else {        	
			weekHeight = mListView.getNMNormalWeekHeight();//mNormalWeekHeight;      	
		}
		
		drawingParams.put(MonthWeekView.VIEW_PARAMS_MODE, MonthWeekView.NORMAL_MODE_VIEW_PATTERN);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, mListView.getOriginalListViewHeight());		
		drawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, weekHeight);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, mListView.getNMNormalWeekHeight());
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_PREVIOUS_WEEK_HEIGHT, mListView.getNMPrvMonthWeekHeight());
		drawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, mListView.getNMMonthIndicatorHeight());				  
		drawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, position);		
		drawingParams.put(MonthWeekView.VIEW_PARAMS_ORIENTATION, mOrientation);
		//
		drawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, week_pattern);        
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_FIRST_MONTHDAY_WEEK, ETime.getJulianDay(mTimeOfFirstDayOfThisWeek.getTimeInMillis(), mTimeOfFirstDayOfThisWeek.getTimeZone().getRawOffset()/1000));        
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_LAST_MONTHDAY_WEEK, ETime.getJulianDay(mTimeOfLastDayOfThisWeek.getTimeInMillis(), mTimeOfFirstDayOfThisWeek.getTimeZone().getRawOffset()/1000));        
				
		v.setWeekParams(drawingParams, mCurrentMonthTimeDisplayed.getTimeZone().getID());
		sendEventsToView(v);
				
		return v;
    }
    
    @SuppressWarnings("unchecked")
	public MonthWeekView getViewExpandEventViewMode(int position, View convertView, ViewGroup parent) {
    	MonthWeekView v;    	
    	
    	int week_pattern = MonthWeekView.NORMAL_WEEK_PATTERN;
		int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//Time.getJulianMondayFromWeeksSinceEpoch(position);		
		long julianMondayMillis = ETime.getMillisFromJulianDay(julianMonday, mCurrentMonthTimeDisplayed.getTimeZone(), mFirstDayOfWeek);		
		mTimeOfFirstDayOfThisWeek.setFirstDayOfWeek(mFirstDayOfWeek);
		mTimeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMillis);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);
		ETime.adjustStartDayInWeek(mTimeOfFirstDayOfThisWeek);		    
		
		mTimeOfLastDayOfThisWeek.setFirstDayOfWeek(mFirstDayOfWeek);
		mTimeOfLastDayOfThisWeek.setTimeInMillis(mTimeOfFirstDayOfThisWeek.getTimeInMillis());
		mTimeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);	
				
		int maxMonthDay = mTimeOfLastDayOfThisWeek.getActualMaximum(Calendar.DAY_OF_MONTH);
		int weekHeight = mListView.getEEMNormalWeekHeight();
		if (mTimeOfFirstDayOfThisWeek.get(Calendar.MONTH) != mTimeOfLastDayOfThisWeek.get(Calendar.MONTH)) {
			week_pattern = MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN;
			weekHeight = mListView.getEEMTwoMonthWeekHeight(); //mTwoMonthWeekHeight;       
		}
		else {
			if ( (mTimeOfFirstDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == 1) /*&& (timeOfFirstDayOfThisWeek.weekDay == mFirstDayOfWeek)*/ ) {
				week_pattern = MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN; 
				weekHeight = mListView.getEEMFirstWeekHeight();//mFfweekHeight;  
			}
			else if (mTimeOfLastDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == maxMonthDay) {	    	
				week_pattern = MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN;
				//weekHeight = mListView.getEEVMLastWeekHeight();//mLastWeekHeight;
			}
		    //else if ... LAST_WEEK_SECTION_HEIGHT �� ������ ���� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN��!!!
		    //�׷��Ƿ� ���� ó���� �ʿ䰡 ����
		}		
		
		int selectedWeekDay = -1;
		if (mSelectedWeekInEEMode == position) {
			selectedWeekDay = mSelectedDayInEEMode.get(Calendar.DAY_OF_WEEK);
		}  
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
		
		// height �κ��� �߸��Ǿ���!!!
		// : �ƴϴ� MonthWeekView::onMeasure���� 
		//   setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight)�� ȣ���Ѵ�
		//   2nd para�� mHeight�� VIEW_PARAMS_HEIGHT�� ���� �����ȴ�
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);		
		// drawingParams�� ����� �������� ������ dumy value�� �����ǹǷ�
		// �Ʒ� �ּ� ó���� �κп� �����ؾ� ��!!!
		HashMap<String, Integer> drawingParams = null;
		
		if (convertView != null) {
			v = (MonthWeekView) convertView;	
			
        	drawingParams = (HashMap<String, Integer>) v.getTag();
        	
			int viewMode = 0;
			if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_MODE)) {
				viewMode = drawingParams.get(MonthWeekView.VIEW_PARAMS_MODE);
			}
			
			if (viewMode == MonthWeekView.EXPAND_EVENTS_VIEW_MODE_VIEW_PATTERN) {
				int weekPatternOfConvertView = MonthWeekView.NORMAL_WEEK_PATTERN; // 
				
				if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN)) {
					weekPatternOfConvertView = drawingParams.get(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN);
				}
			
				if (week_pattern != weekPatternOfConvertView) {
					v = new MonthWeekView(mContext);
					// �Ʒ� �ڵ带 �������� ������, ��Ȥ dummy value�� �����Ǿ
					// fling ��, ��Ȯ�ϰ� distance�� ������ �� ���� �ȴ�
					drawingParams.clear();
					drawingParams = null;
				} 
			}            	
			
		} else {
			v = new MonthWeekView(mContext);
		}
		
		if (drawingParams == null) {
			drawingParams = new HashMap<String, Integer>();
		}        
		drawingParams.clear();
		
		v.setLayoutParams(params);////////////////////////////////////////////////////////////////////////////
		v.setClickable(true);/////////////////////////////////////////////////////////////////////////////////
		v.setOnTouchListener(this);///////////////////////////////////////////////////////////////////////////			 
		
		drawingParams.put(MonthWeekView.VIEW_PARAMS_MODE, MonthWeekView.EXPAND_EVENTS_VIEW_MODE_VIEW_PATTERN);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, mListView.getOriginalListViewHeight());
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_LISTVIEW_HEIGHT, mListView.getEEVMListViewHeight());		
		drawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, weekHeight);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, mListView.getEEMNormalWeekHeight());
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_PREVIOUS_WEEK_HEIGHT, mListView.getEEVMLastWeekHeight());
		drawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, mListView.getEEMMonthIndicatorHeight());
		////////////////////////////////////////////////////////////////////////////
		drawingParams.put(MonthWeekView.VIEW_PARAMS_SELECTED_DAY_OF_WEEK_IN_EEMODE, selectedWeekDay);//
		////////////////////////////////////////////////////////////////////////////
		drawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);
		drawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, position);		
		drawingParams.put(MonthWeekView.VIEW_PARAMS_ORIENTATION, mOrientation);
		//
		drawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, week_pattern);        
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_FIRST_MONTHDAY_WEEK, ETime.getJulianDay(mTimeOfFirstDayOfThisWeek.getTimeInMillis(), gmtoff));        
		//drawingParams.put(MonthWeekView.VIEW_PARAMS_LAST_MONTHDAY_WEEK, ETime.getJulianDay(mTimeOfLastDayOfThisWeek.getTimeInMillis(), gmtoff));        
		
		if (mAnimateChangeTargetMonthInEEMode) {
			long currentTime = System.currentTimeMillis();
            // If it's been too long since we tried to start the animation
            // don't show it. This can happen if the user stops a scroll
            // before reaching today.
            if (currentTime - mAnimateTime > ANIMATE_TODAY_TIMEOUT) {
            	mAnimateChangeTargetMonthInEEMode = false;
                mAnimateTime = 0;
            } 
            else {
            	// mChangeTargetMonthInMillis
            	// position�� ������ �ش� week view�� 1���� ������ �ִ��� �ƴ����� �Ǵ�����...     
                
                Calendar time = GregorianCalendar.getInstance(mCurrentMonthTimeDisplayed.getTimeZone());
                time.setFirstDayOfWeek(mFirstDayOfWeek);                
                time.setTimeInMillis(mTimeOfFirstDayOfThisWeek.getTimeInMillis()); 
                
                long gmtoff = mCurrentMonthTimeDisplayed.getTimeZone().getRawOffset() / 1000;
                int firstJulianDay = ETime.getJulianDay(time.getTimeInMillis(), mHomeTimeZone, mFirstDayOfWeek);
                int lastJulianDay = firstJulianDay + 6;
                 
                int targetMonthJulianToday = ETime.getJulianDay(mSelectedDayInEEMode.getTimeInMillis(), mHomeTimeZone, mFirstDayOfWeek);             
                
                boolean isOk = false;
                if (targetMonthJulianToday >= firstJulianDay && targetMonthJulianToday <= lastJulianDay) 
                	isOk = true;                     
                
            	if (isOk) {
            		drawingParams.put(MonthWeekView.VIEW_PARAMS_ANIMATE_CHANGE_TARGET_MONTH_IN_EEMODE, 1);
        			mAnimateChangeTargetMonthInEEMode = false;	// reset!!!
            	}
            	
            }
		}
		
		v.setWeekParams(drawingParams, mCurrentMonthTimeDisplayed.getTimeZone().getID());
		
		sendEventsToView(v);
		
    	return v;
    			
    }
	
	MonthFragment mFragment;
    MonthListView mListView;
    int mListViewHeight = 0;
    
    public void setListView(MonthFragment fragment, MonthListView lv) {
    	mFragment = fragment;
        mListView = lv;        
    }
    
    
    public void sendEventsToView(MonthWeekView v) {
        if (mEventDayList.size() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        
        int viewJulianDay = ETime.getJulianDay(v.getFirstMonthTime().getTimeInMillis(), mHomeTimeZone, mFirstDayOfWeek);//v.getFirstJulianDay();
        
        int start = viewJulianDay - mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Week is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }
    
    public void sendEventsToView(PsudoTargetMonthViewAtEventListEntrance v) {
        if (mEventDayList.size() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        
        int viewJulianDay = v.getFirstJulianDayInTargetMonth();
        
        int start = viewJulianDay - mFirstJulianDay;/////////////////////////////////////////////////////////////////////////////////////////////
        int end = start + v.mMaxMonthDay;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Month is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }
    
    public void sendEventsToView(PsudoTargetMonthViewAtEventListExit v) {
        if (mEventDayList.size() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        
        int viewJulianDay = v.getFirstJulianDayInTargetMonth();
        
        int start = viewJulianDay - mFirstJulianDay;/////////////////////////////////////////////////////////////////////////////////////////////
        int end = start + v.mMaxMonthDay;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Month is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }
	
	
    public void setSelectedDayAlphaAnimUnderNoExistenceInEEMode(long changeTargetMonthInMillis) {
    	
    	setSelectedDayInEEMode(changeTargetMonthInMillis);
    	
    	mAnimateChangeTargetMonthInEEMode = true;
        mAnimateTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {    	
    	
        if (!(v instanceof MonthWeekView)) {
        	/*
        	if (mGestureDetector.onTouchEvent(event)) {
                MonthWeekView view = (MonthWeekView) v;
                Time day = ((MonthWeekView)v).getDayFromLocation(event.getX(), event.getY());
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Touched day at Row=" + view.mWeek + " day=" + day.toString());
                }
                
                if (day != null) {
                    onDayTapped(day);
                }
                return true;
            }
        	*/
            return false;
        }

        int action = event.getAction();

        // Event was tapped - switch to the detailed view making sure the click animation
        // is done first.
        if (mGestureDetector.onTouchEvent(event)) {
        	// mDoSingleTapUp���� ����ϱ� ����,
        	// mSingleTapUpView�� �����Ѵ�
        	if (!mListView.mExpandEventViewMode) {       
        		mSingleTapUpView = (MonthWeekView) v;
        		//mSingleTapUpView.invalidate();
        		long delay = System.currentTimeMillis() - mClickTime;
        		// Make sure the animation is visible for at least mOnTapDelay - mOnDownDelay ms
        		mListView.postDelayed(mDoSingleTapUp,
        				delay > mTotalClickDelay ? 
        						0 : mTotalClickDelay - delay);
        	}
        	else {
        		//mClickedViewInEPMode.invalidate();
        		int firstJulainDay = ETime.getJulianDay(mClickedViewInEPMode.getFirstMonthTime().getTimeInMillis(), mHomeTimeZone, mFirstDayOfWeek);
        		int weekNumber = mClickedViewInEPMode.getWeekNumber();        		
        		int dayIndex = mClickedViewInEPMode.getDayIndexFromLocation(mClickedXLocationInEPMode, mClickedYLocationInEPMode);
                int selectedJulainDay = firstJulainDay + dayIndex;
                long millis = ETime.getMillisFromJulianDay(selectedJulainDay, mHomeTimeZone, mFirstDayOfWeek);
                
                setSelectedDayInEEMode(millis, weekNumber);
                            	
                notifyDataSetChanged();/////////////////////////////////////////////////////////////////////////////////////////////////////////////
                
                mFragment.setEventsOfEventListView(selectedJulainDay);
        	}
        	
            return true;
        } else {
            // Animate a click - on down: show the selected day in the "clicked" color.
            // On Up/scroll/move/cancel: hide the "clicked" color.
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                	// ���⿡ ���� �ִ�...
                	// eev mode�� ��츦 ���� Ȯ�� ��,,, mClickedView�� null�� �ƴϸ�,,,
                	// clearClickedDay�� ȣ������
                	if (mListView.mExpandEventViewMode) {     
                		mClickedViewInEPMode = (MonthWeekView)v;
                		mClickedXLocationInEPMode = event.getX();
                		mClickedYLocationInEPMode = event.getY();	                	
                	}
                	else {                	
	                    mClickedView = (MonthWeekView)v;
	                    mClickedXLocation = event.getX();
	                    mClickedYLocation = event.getY();
	                    mClickTime = System.currentTimeMillis();
	                    // delay�� �ʿ��� ������?
	                    mListView.postDelayed(mDoClick, mOnDownDelay);
                	}
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_SCROLL:
                case MotionEvent.ACTION_CANCEL:
                	if (!mListView.mExpandEventViewMode) {  
                		clearClickedView((MonthWeekView)v);
                	}
                    break;
                case MotionEvent.ACTION_MOVE:
                	if (!mListView.mExpandEventViewMode) {  
	                    // No need to cancel on vertical movement, ACTION_SCROLL will do that.
	                    if (Math.abs(event.getX() - mClickedXLocation) > mMovedPixelToCancel) {
	                        clearClickedView((MonthWeekView)v);
	                    }
                	}
                    break;
                default:
                    break;
            }
        }
        
        // Do not tell the frameworks we consumed the touch action so that fling actions can be
        // processed by the fragment.
        return false;
    }
    
    
       
    // Clear the visual cues of the click animation and related running code.
    private void clearClickedView(MonthWeekView v) {
        mListView.removeCallbacks(mDoClick);
        synchronized(v) {
            v.clearClickedDay();
        }
        mClickedView = null;
    }

    // Perform the tap animation in a runnable to allow a delay before showing the tap color.
    // This is done to prevent a click animation when a fling is done.
    private final Runnable mDoClick = new Runnable() {
        @Override
        public void run() {
            if (mClickedView != null) {
                synchronized(mClickedView) {
                    mClickedView.setClickedDayIndex(mClickedXLocation, mClickedYLocation);
                }
                //mLongClickedView = mClickedView;
                mClickedView = null;
                // This is a workaround , sometimes the top item on the listview doesn't refresh on
                // invalidate, so this forces a re-draw.
                //mListView.invalidate();
            }
        }
    };
	
    
    /**
     * Maintains the same hour/min/sec but moves the day to the tapped day.
     *
     * @param day The day that was tapped
     */
    
    public void onDayTapped(Calendar day) {
        
        if (!mListView.mExpandEventViewMode) {
        	if (INFO) Log.i(TAG, "onDayTapped: Call ExitMonthViewEnterDayViewAnim");
        	mFragment.mGoToExitAnim = true;
        	ExitMonthViewEnterDayViewAnim test = new ExitMonthViewEnterDayViewAnim(mApp, mContext, mFragment, mFragment.mController, mSingleTapUpView,
        			day, mCurrentMonthTimeDisplayed.getTimeZone().getID());	        	
	        
        }
        else {
        	//if (mExpandEventViewState == ExpandEventViewState.Run) { 
        		//setSelectedDay(day);
        		//mFragment.setSelectedDayInExpandEventsViewMode(Time.getJulianDay(day.toMillis(true), day.gmtoff));
        	//}
        }
    }
    
    boolean mOccuredLongPress = false;
	public class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {        	   		
            return true;
        }
        
    }
	
	
	// Performs the single tap operation: go to the tapped day.
    // This is done in a runnable to allow the click animation to finish before switching views
	private final Runnable mDoSingleTapUp = new Runnable() {
        @Override
        public void run() {
            if (mSingleTapUpView != null) {
                Calendar day = mSingleTapUpView.getDayFromLocation(mClickedXLocation, mClickedYLocation);////////////////////////////////////
                
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Touched day at Row=" + mSingleTapUpView.mWeek + " day=" + day.toString());
                }                
                
                if (day != null) {
                    onDayTapped(day);////////////////////////////////////////////////////////////////////////////////////////////////
                }
                
                
                if (mListView.mExpandEventViewMode)
                	mSingleTapUpView = null;
            }
        }
    };

}
