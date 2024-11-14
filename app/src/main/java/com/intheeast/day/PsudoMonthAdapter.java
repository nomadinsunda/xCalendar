

package com.intheeast.day;

import android.content.Context;
//import android.text.format.Time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthAdapter;
import com.intheeast.month.MonthWeekView;



public class PsudoMonthAdapter {
    
    public static int DEFAULT_QUERY_DAYS = 7 * 8; // 8 weeks
    
    Context mContext;
    
    public CalendarController mController;
    public String mHomeTimeTzId;
    public TimeZone mHomeTimeZone;
    public Calendar mTempTime;
    public Calendar mToday;
    public int mFirstJulianDay;
    //public int mQueryDays;
        
    int mFirstDayOfWeek;
    int mDaysPerWeek;
    Calendar mSelectedDay;

    public ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
    public ArrayList<Event> mEvents = null;   
    
    public int mAnticipationListViewHeight;
    //int mPrvMonthWeekHeight;
    public int mMonthIndicatorHeight;
    //int mNextMonthWeekHeight;
    public int mTwoMonthWeekHeight;
    public int mFirstWeekHeight;
    //int mLastWeekHeight;
    public int mNormalWeekHeight;
    
    public PsudoMonthAdapter(Context context) {
    	mContext = context;     	
    	initForYearFragment();
    }
    
    public void initForYearFragment() {       
        mController = CalendarController.getInstance(mContext);
        mHomeTimeTzId = Utils.getTimeZone(mContext, null);
        mHomeTimeZone = TimeZone.getTimeZone(mHomeTimeTzId);
        
        mToday = GregorianCalendar.getInstance(mHomeTimeZone); //new Time(mHomeTimeZone);
        mToday.setTimeInMillis(System.currentTimeMillis()); //mToday.setToNow();
        
        mTempTime = GregorianCalendar.getInstance(mHomeTimeZone);
        
        mSelectedDay = GregorianCalendar.getInstance(mHomeTimeZone);    
    }   
    
    // For YearFragment
    public void calcWeekViewHeights(int anticipationListViewHeight) {
    	mAnticipationListViewHeight = anticipationListViewHeight;
    	
    	//float heightRatio = MonthWeekView.PRV_WEEK_SECTION_HEIGHT;
		//mPrvMonthWeekHeight = (int)(mAnticipationListViewHeight * heightRatio); 
		
		float heightRatio = MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT;
		mMonthIndicatorHeight = (int)(mAnticipationListViewHeight * heightRatio); 
		
		//heightRatio = MonthWeekView.NEXT_WEEK_SECTION_HEIGHT;
		//mNextMonthWeekHeight = (int)(mAnticipationListViewHeight * heightRatio); 
		
		heightRatio = MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT;
		mNormalWeekHeight = (int)(mAnticipationListViewHeight * heightRatio);
		
		mTwoMonthWeekHeight = mNormalWeekHeight + mMonthIndicatorHeight + mNormalWeekHeight;       
    							
		mFirstWeekHeight = mMonthIndicatorHeight + mNormalWeekHeight; 
    	
		//heightRatio = MonthWeekView.LAST_WEEK_SECTION_HEIGHT;
		//mLastWeekHeight = (int)(mAnticipationListViewHeight * heightRatio);
    }    
    
    public PsudoMonthAdapter(Context context, int anticipationListViewHeight/*, HashMap<String, Integer> params, Handler handler*/) {
    	mContext = context;  
    	mAnticipationListViewHeight = anticipationListViewHeight;
    	init();
    }   
    
    public void init() {       
        mController = CalendarController.getInstance(mContext);
        mHomeTimeTzId = Utils.getTimeZone(mContext, null);
        mHomeTimeZone = TimeZone.getTimeZone(mHomeTimeTzId);
        
        mToday = GregorianCalendar.getInstance(mHomeTimeZone); //new Time(mHomeTimeZone);
        mToday.setTimeInMillis(System.currentTimeMillis()); //mToday.setToNow();
        
        mTempTime = GregorianCalendar.getInstance(mHomeTimeZone);
        
        mSelectedDay = GregorianCalendar.getInstance(mHomeTimeZone);  
        
        calcWeekViewHeights();
    }   
    
    public void calcWeekViewHeights() {
    	//float heightRatio = MonthWeekView.PRV_WEEK_SECTION_HEIGHT;
		//mPrvMonthWeekHeight = (int)(mAnticipationListViewHeight * heightRatio); 
		
		float heightRatio = MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT;
		mMonthIndicatorHeight = (int)(mAnticipationListViewHeight * heightRatio); 
		
		//heightRatio = MonthWeekView.NEXT_WEEK_SECTION_HEIGHT;
		//mNextMonthWeekHeight = (int)(mAnticipationListViewHeight * heightRatio); 
		
		heightRatio = MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT;
		mNormalWeekHeight = (int)(mAnticipationListViewHeight * heightRatio);
		
		mTwoMonthWeekHeight = mNormalWeekHeight + mMonthIndicatorHeight + mNormalWeekHeight;       
    							
		mFirstWeekHeight = mMonthIndicatorHeight + mNormalWeekHeight; 
    	
		//heightRatio = MonthWeekView.LAST_WEEK_SECTION_HEIGHT;
		//mLastWeekHeight = (int)(mAnticipationListViewHeight * heightRatio);
    }
    
    
    public void updateParams(HashMap<String, Integer> params) {        
        
        if (params.containsKey(MonthAdapter.WEEK_PARAMS_WEEK_START)) {
            mFirstDayOfWeek = params.get(MonthAdapter.WEEK_PARAMS_WEEK_START);
        }
        if (params.containsKey(MonthAdapter.WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED)) {
            int julianDay = params.get(MonthAdapter.WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED);
            long julianDayMillis = ETime.getMillisFromJulianDay(julianDay, mHomeTimeZone, mFirstDayOfWeek);
            mSelectedDay.setTimeInMillis(julianDayMillis);//mSelectedDay.setJulianDay(julianDay);
            //mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(julianDay, mFirstDayOfWeek);
        }
        if (params.containsKey(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK)) {
            mDaysPerWeek = params.get(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK);
        }
        
    }
    
    public void setEvents(int firstJulianDay, int numDays, ArrayList<Event> events) {
        
    	// mEvents�� �� �信 mUnsortedEvents�� ����Ǵµ� ���� �ϴ� ������ ����
        mEvents = events;
        mFirstJulianDay = firstJulianDay;
        
        //mQueryDays = needNumDays/*numDays*/;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        if (events == null || events.size() == 0) {            
            mEventDayList = eventDayList;            
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

    public void setEvents(int firstLoadedJulianDay, int numDays, int firstJulianDay, int needNumDays, ArrayList<Event> events) {
        
    	// mEvents�� �� �信 mUnsortedEvents�� ����Ǵµ� ���� �ϴ� ������ ����
        mEvents = events;
        mFirstJulianDay = firstJulianDay;
        
        //mQueryDays = needNumDays/*numDays*/;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        if (events == null || events.size() == 0) {            
            mEventDayList = eventDayList;            
            return;
        }

        // Compute the new set of days with events
        for (Event event : events) {
            int startDay = event.startDay - firstLoadedJulianDay;
            int endDay = event.endDay - firstLoadedJulianDay + 1;
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
        
        //mEventDayList = (ArrayList<ArrayList<Event>>) eventDayList.subList(start, end); //subList�� ArrayList�� �ƴ� List�� �����ϹǷ� ��Ÿ�� ������ �߻��Ѵ� 
        int start = numDays - needNumDays;
        int end = numDays; 
        for (int i=start; i<end; i++) {
        	mEventDayList.add(i - start, eventDayList.get(i));
        }
        
        //int test = 0;
        //test = -1;
        //test = 3;        
        
        //mEventDayList = eventDayList;        
    } 
    
    public ArrayList<Event> getEvents() {
    	return mEvents;
    }
   
    
    public static int calWeekPattern(int position, String tzId, int firstDayOfWeek) {
    	int weekPattern = PsudoMonthWeekEventsView.NORMAL_WEEK_PATTERN;
    	int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//int julianMonday = Time.getJulianMondayFromWeeksSinceEpoch(position);
		Calendar timeOfFirstDayOfThisWeek = GregorianCalendar.getInstance(TimeZone.getTimeZone(tzId)); //new Time(tzId);
		timeOfFirstDayOfThisWeek.setFirstDayOfWeek(firstDayOfWeek);
		long julianMondayMillis = ETime.getMillisFromJulianDay(julianMonday, TimeZone.getTimeZone(tzId), firstDayOfWeek);
		timeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMillis);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);
		ETime.adjustStartDayInWeek(timeOfFirstDayOfThisWeek);
				
		Calendar timeOfLastDayOfThisWeek = GregorianCalendar.getInstance(TimeZone.getTimeZone(tzId)); //new Time(tzId);		
		timeOfLastDayOfThisWeek.setTimeInMillis(timeOfFirstDayOfThisWeek.getTimeInMillis());//timeOfLastDayOfThisWeek.setJulianDay(firstJulianDayOfThisWeek + 6);
		timeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);
		
		int maxMonthDay = timeOfLastDayOfThisWeek.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {
			weekPattern = PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN;
		}
		else {
			if ( (timeOfFirstDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == 1) /*&& (timeOfFirstDayOfThisWeek.weekDay == mFirstDayOfWeek)*/ ) {
				weekPattern = PsudoMonthWeekEventsView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN; 
			}
			else if (timeOfLastDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == maxMonthDay) {	    	
				weekPattern = PsudoMonthWeekEventsView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN;
			}
			// else if ... LAST_WEEK_SECTION_HEIGHT �� ������ ���� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN��!!!
			// �׷��Ƿ� ���� ó���� �ʿ䰡 ����
		}
		
		return weekPattern;
    }
    
       

    public void sendEventsToView(PsudoMonthWeekEventsView v) {
        if (mEventDayList.size() == 0) {            
            v.setEvents(null, null);
            return;
        }
        
        
        int weekPattern = v.getWeekPattern();
        if (weekPattern == PsudoMonthWeekEventsView.NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN /*||
        		weekPattern == PsudoMonthWeekEventsView.SELECTED_NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFF_FIRSTWEEK_PATTERN*/) {
        	// �ش� �ְ� month�� ù��° ���̰�
            // two different month day�� �����ϴ� week���,
            // prv month�� month day�� event circle�� drawing �ؼ��� �ȵȴ�
            // ���� viewJulianDay�� target month�� ù��° month day�� julianday�� �����ؾ� �Ѵ�
        	
        	int viewJulianDay = mFirstJulianDay; // mFirstJulianDay�� �ſ� 1�Ϸ� �����ȴ�
        	// mEventDayList�� Ư�� ���� 1�Ϻ��� xx�� �����̴�
        	// mEventDayList.subList(start, end)�� ������..
        	// v.setEvents�� 1st para�� List<ArrayList<Event>> sortedEvents�� ���� ������ �Ѵ�
        	ArrayList<ArrayList<Event>> sortedEvents = new ArrayList<ArrayList<Event>>();
        	        	 
        	int size = sortedEvents.size();
        	Calendar weekTimeCalendar = Calendar.getInstance(mHomeTimeZone);
        	weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  	
        	long firstJulianDayMillis = ETime.getMillisFromJulianDay(mFirstJulianDay, mHomeTimeZone, mFirstDayOfWeek);
        	mTempTime.setTimeInMillis(firstJulianDayMillis);//mTempTime.setJulianDay(mFirstJulianDay);
        	weekTimeCalendar.setTimeInMillis(mTempTime.getTimeInMillis());		
        	
        	// 1�� ���ִ� ������ Calendar�� DAY_OF_WEEK �ʵ��� �������� 1[Calendar.SUNDAY]���� ���� 
        	// �׷��� 1�� ���־ Time.weekDay�� �����ֱ� ���� �͵� �ְ�
        	// PsudoMonthWeekEventsView�� cell ������ �ε����� �����ֱ� ���Ե� �ִ�
        	int weekDay = weekTimeCalendar.get(Calendar.DAY_OF_WEEK) - 1;        	
        	
        	// weekDay�� 1 �̶��,
        	// index�� 0�� �Ǿ�� �Ѵ�
        	for (int i=0; i<v.mNumDays; i++) {        		
        		if (i < weekDay) {
        			sortedEvents.add(i, new ArrayList<Event>());
        		}
        		else {
        			int index = i - weekDay;
        			ArrayList<Event> xxx = mEventDayList.get(index);
        			sortedEvents.add(i, xxx);      
        		}
        	}
        	
        	v.setEvents(sortedEvents, mEvents);
        	
        }        
        else {
        	int viewJulianDay = ETime.getJulianDay(v.getFirstMonthTime().getTimeInMillis(), mHomeTimeZone, mFirstDayOfWeek);//v.getFirstJulianDay();             
            int start = viewJulianDay - mFirstJulianDay; 
            int end = start + v.mNumDays;
                    
            if (start < 0 || end > mEventDayList.size()) {            
                v.setEvents(null, null);
                return;
            }
            
            v.setEvents(mEventDayList.subList(start, end), mEvents);
        }   
        
    }

    private void setDayParameters(Calendar day) {
        //day.timezone = mHomeTimeZone;
    	day = ETime.switchTimezone(day, mHomeTimeZone);
        
        Calendar currTime = GregorianCalendar.getInstance(mHomeTimeZone);//new Time(mHomeTimeZone);
        currTime.setTimeInMillis(mController.getTime());//currTime.set(mController.getTime());
        
        
        day.set(Calendar.HOUR_OF_DAY, currTime.get(Calendar.HOUR_OF_DAY));//day.hour = currTime.hour;
        day.set(Calendar.MINUTE, currTime.get(Calendar.MINUTE));//day.minute = currTime.minute;
        //day.set(Calendar., );//day.allDay = false;
        //day.normalize(true);
    }


}
