package com.intheeast.year;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarViewsLowerActionBar;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.CalendarViewsUpperActionBarFragment;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.FastEventInfoFragment;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthListView;
import com.intheeast.month.MonthWeekView;


import com.intheeast.settings.SettingsFragment;
import com.intheeast.year.YearListView.FlingParameter;
import com.intheeast.year.YearListView.UnvisibleMonthTablePart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.AbsListView.OnScrollListener;

@SuppressLint("ValidFragment")
public class YearPickerFragment extends ListFragment implements CalendarController.EventHandler,
	OnScrollListener, OnTouchListener{
	
	private static final String TAG = "YearPickerFragment";
    private static boolean INFO = true;
    
	public enum ListViewState {
    	NoneState,    	
		Run,
		Run_FlingState,
        Finished
	}
	
	public ListViewState mListViewState = ListViewState.NoneState;
	
	private static final String KEY_CURRENT_TIME = "current_time";
	static final int GOTO_SCROLL_DURATION = 500;
	public static int LIST_TOP_OFFSET = 0;
	int BOTTOM_BUFFER = 20;
	
	ECalendarApplication mApp;	
	Activity mActivity;
	Context mContext;
	RelativeLayout mFrameLayout;
	RelativeLayout mYearViewLayout;
	CalendarViewsUpperActionBarFragment mUpperActionBar;
	//CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
	CalendarViewsLowerActionBar mLowerActionBar;
	
	CalendarController mController;
	FastEventInfoFragment mEventInfoFragment;
	
	//TextView mTodayTextView;
	//TextView mCalendarsView;
	//TextView mInboxView;
	
	Handler mHandler;
	
	YearsAdapter mAdapter;
	YearListView mListView;
	
	int mMonthRows = 4;
	public static final int DAYS_PER_WEEK = 7;
	int mNumWeeks = 6;
	int mDaysPerWeek = 7;
	
	float mFriction = 0.5f;
	
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	// CentralActivity.handleEvent�� EventType.GO_TO ���ǿ� ����
	//  setMainPane���� 4th para�� ���޵� event.startTime.toMillis�� ���� 
	//  ������ �ð��̴�
	Calendar mSelectedYear;//new Time();
	//final Calendar mDesiredDay = GregorianCalendar.getInstance();//new Time();
	Calendar mTempTime;//new Time();
	Calendar mFirstVisibleDay;//new Time();
	
	
	int mFirstDayOfWeek;
	
	int mPreviousScrollState = OnScrollListener.SCROLL_STATE_IDLE;
	int mCurrentScrollState = OnScrollListener.SCROLL_STATE_IDLE;
	
	int mAnticipationMonthListViewWidth;
	int mAnticipationMonthListViewHeight;
	
	int mAnticipationListViewWidth;	
	int mAnticipationListViewHeight;
	int mAnticipationYearIndicatorHeight;
	int mAnticipationYearIndicatorTextHeight;
	int mAnticipationYearIndicatorTextBaseLineY;
	
	int mAnticipationMiniMonthWidth;
	int mAnticipationMiniMonthHeight;
	int mAnticipationMiniMonthIndicatorTextHeight;
	int mAnticipationMiniMonthIndicatorTextBaseLineY;
	int mAnticipationMiniMonthDayTextBaseLineY;
	int mAnticipationMiniMonthDayTextHeight;
	
	int mAnticipationMiniMonthLeftMargin;
		
	long mPreviousFirstVisiblePosition;
	long mPreviousFirstChildBottom;
	
	boolean mIsScrollingUp = false;
	
	static float mScale = 0;
	
	float mMinimumFlingVelocity;
	
	boolean mIsDetached;
	
	private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTzId = Utils.getTimeZone(mContext, mTZUpdater);
            mTimeZone = TimeZone.getTimeZone(mTzId);
            mGmtoff = mTimeZone.getRawOffset() / 1000;            
            
            ETime.switchTimezone(mSelectedYear, mTimeZone);//mSelectedYear.timezone = tz;
            
            ETime.switchTimezone(mTempTime, mTimeZone);//mTempTime.timezone = tz;
           
            ETime.switchTimezone(mFirstVisibleDay, mTimeZone);//mFirstVisibleDay.timezone = tz;
            
            if (mAdapter != null) {
                mAdapter.refresh();
            }
        }
    };
    
    protected Runnable mTodayUpdater = new Runnable() {
        @Override
        public void run() {
        	Calendar midnight = GregorianCalendar.getInstance(mFirstVisibleDay.getTimeZone());//new Time(mFirstVisibleDay.timezone);
            midnight.setTimeInMillis(System.currentTimeMillis());
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
    
    
	
	public YearPickerFragment(Context context, long initialTime, boolean isMiniMonth, CalendarViewsUpperActionBarFragment upperActionBar) {
		mTzId = Utils.getTimeZone(context, mTZUpdater);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;    
        
        mSelectedYear = GregorianCalendar.getInstance(mTimeZone);
        
        mTempTime = GregorianCalendar.getInstance(mTimeZone);
       
        mFirstVisibleDay = GregorianCalendar.getInstance(mTimeZone);
        
		goTo(initialTime, false, true, true);
	    mHandler = new Handler();
	        
	    mUpperActionBar = upperActionBar;
	}
	
	
	
	@Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mApp = (ECalendarApplication) activity.getApplication();
		mActivity = activity;
		mContext = activity.getApplicationContext();
		
		mController = CalendarController.getInstance(mActivity);
		       
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        ViewConfiguration viewConfig = ViewConfiguration.get(mContext);
        mMinimumFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();

        // Ensure we're in the correct time zone
        ETime.switchTimezone(mSelectedYear, mTimeZone);//mSelectedYear.switchTimezone(tz);       
        
        ETime.switchTimezone(mFirstVisibleDay, mTimeZone);//mFirstVisibleDay.timezone = tz;
        
        ETime.switchTimezone(mTempTime, mTimeZone);//mTempTime.timezone = tz;
        
        calcFrameLayoutHeight();
        
        // Adjust sizes for screen density
        if (mScale == 0) {
            mScale = activity.getResources().getDisplayMetrics().density;
            if (mScale != 1) {                
                BOTTOM_BUFFER *= mScale;
                LIST_TOP_OFFSET *= mScale;
            }
        }
        setUpAdapter();
        setListAdapter(mAdapter);
                
        mTZUpdater.run();
        if (mAdapter != null) {
            //mAdapter.setSelectedDay(mSelectedDay);
        }
        mIsDetached = false;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CURRENT_TIME)) {
            goTo(savedInstanceState.getLong(KEY_CURRENT_TIME), false, true, true);
        }               
    }
	
	CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
	PsudoMonthView mPsudoUpperMonthView;
	ScaleMiniMonthView mScaleTargetMiniMonthView;	
	PsudoMonthView mPsudoLowerMonthView;
	PsudoEventListView mPsudoMonthExpandModeEventsListView;
	CalendarViewsLowerActionBar mYearviewPsudoLowerActionBar;
	public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
    	   	
		
		
    	mFrameLayout = (RelativeLayout) inflater.inflate(R.layout.unified_year_fragment_layout, container, false);   	      
    	
    	mLowerActionBar = (CalendarViewsLowerActionBar) mFrameLayout.findViewById(R.id.yearview_lower_actionbar);
    	mLowerActionBar.init(this, mTzId, mController, false);
        mLowerActionBar.setWhoName("yearview_lower_actionbar");
        
    	mYearViewLayout = (RelativeLayout) mFrameLayout.findViewById(R.id.yearview);    
    	mYearViewLayout.setClipChildren(false);
    	mCalendarViewsSecondaryActionBar = (CalendarViewsSecondaryActionBar)mYearViewLayout.findViewById(R.id.yearview_secondary_actionbar);        
        mCalendarViewsSecondaryActionBar.init(mSelectedYear.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); 
        mPsudoUpperMonthView = (PsudoMonthView)mYearViewLayout.findViewById(R.id.psudo_uppermonthview);    
        mScaleTargetMiniMonthView = (ScaleMiniMonthView)mYearViewLayout.findViewById(R.id.scale_target_mini_monthview);        
        mPsudoLowerMonthView = (PsudoMonthView)mYearViewLayout.findViewById(R.id.psudo_lowermonthview);   
        mPsudoMonthExpandModeEventsListView = (PsudoEventListView) mYearViewLayout.findViewById(R.id.psudo_expand_mode_eventlist);   
        // ���� ������� �ʴ´� invisible ���·� �ϴ� �κ� ������ ��� �ִ�
        // :���� mLowerActionBar�� ���� �������� �ʴ´�
        mYearviewPsudoLowerActionBar = (CalendarViewsLowerActionBar)mYearViewLayout.findViewById(R.id.yearview_psudo_lower_actionbar);
        mYearviewPsudoLowerActionBar.init(this, mTzId, mController, true);
        mYearviewPsudoLowerActionBar.setWhoName("yearview_psudo_lower_actionbar");
        
        return mFrameLayout;
    }
    
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setUpListView();
                
        MonthWeekView child = (MonthWeekView) mListView.getChildAt(0);
        if (child != null) {
        	//int julianDay = child.getFirstJulianDay();        	
        	//long mills = ETime.getMillisFromJulianDay(julianDay, mTimeZone);
        	mFirstVisibleDay.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mFirstVisibleDay.setJulianDay(julianDay);
            // set the title to the month of the second week
        	//mills = ETime.getMillisFromJulianDay(julianDay + DAYS_PER_WEEK, mTimeZone);
        	mTempTime.setTimeInMillis(mFirstVisibleDay.getTimeInMillis());//mTempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        	mTempTime.add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK);
        }       
        
        //mListView.setSelector(new StateListDrawable());
        mListView.setOnTouchListener(this);

        mAdapter.setListView(this, mListView);
    }
	
	@Override
    public void onResume() {
        super.onResume();
        
        long startTimeMillis = System.currentTimeMillis();
		Log.i("tag", "startTimeMillis=" + String.valueOf(startTimeMillis));
        setUpAdapter();
        doResumeUpdates();
        
    }
	
	public void doResumeUpdates() {
		
        //mShowWeekNumber = Utils.getShowWeekNumber(mContext);
        
        mDaysPerWeek = Utils.getDaysPerWeek(mContext);
        //updateHeader();
        //mAdapter.setSelectedDay(mSelectedDay);
        mTZUpdater.run();
        mTodayUpdater.run();
        goTo(mSelectedYear.getTimeInMillis(), false, true, false);
    }
	
	@Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTodayUpdater);        
    }   
	
	@Override
	public void onDetach() {
    	super.onDetach();
    	
    	 mIsDetached = true;
	}
	
	public boolean goTo(long time, boolean animate, boolean setSelected, boolean forceScroll) {
        if (time == -1) {
            //Log.e(TAG, "time is invalid");
            return false;
        }

        // Set the selected day
        if (setSelected) {
        	mSelectedYear.setTimeInMillis(time);   
        	int yearOfSelectedYear = mSelectedYear.get(Calendar.YEAR);
        	
        	Calendar todayCal = GregorianCalendar.getInstance(mTimeZone);
        	todayCal.setTimeInMillis(System.currentTimeMillis());
        	int yearOfToday = todayCal.get(Calendar.YEAR);
        	
        	if (yearOfToday == yearOfSelectedYear) {
        		mSelectedYear.setTimeInMillis(todayCal.getTimeInMillis());   
        	}
        }

        // If this view isn't returned yet we won't be able to load the lists
        // current position, so return after setting the selected day.
        if (!isResumed()) {            
            return false;
        }		
        
        /*
        if) position == 799,
			799 * 3 = 2397 months
			2397 / 12 = 199.75 years 
			2397 % 12 = 9 months ->
			1900 + 199 year/ 9 months + 1
			-> 2099/10 ~ 12
    	*/
        int targetYear = mSelectedYear.get(Calendar.YEAR);
        int yearsFromEpoch = targetYear - YearsAdapter.EPOCH_YEAR;
        int monthsFromEpoch = yearsFromEpoch * 12;
        int position = monthsFromEpoch / 3;     
        
        View child;
        int i = 0;
        int top = 0;
        // Find a child that's completely in the view
        do {
            child = mListView.getChildAt(i++);
            if (child == null) {
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
        
        int lastPosition = firstPosition + mMonthRows - 1;
        if (top > BOTTOM_BUFFER) {
            lastPosition--; // � ��Ȳ�� �߻��ϴ°�? : 
                            // :-����̽��� sleep ���¿��� wakeup �Ǿ��� ��
                            //  -�Ƹ��� today�� �����Ͽ��� ���?
        }      
        
        mController.setTime(mSelectedYear.getTimeInMillis());
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        if (position < firstPosition || position > lastPosition || forceScroll) {
            /*mFirstDayOfMonth.set(mTempTime);
            mFirstDayOfMonth.monthDay = 1;
            millis = mFirstDayOfMonth.normalize(true);           
            
            position = Utils.getWeeksSinceEpochFromJulianDay(
                    Time.getJulianDay(millis, mFirstDayOfMonth.gmtoff), mFirstDayOfWeek);            
            
            mPreviousScrollState = OnScrollListener.SCROLL_STATE_FLING;
            if (animate) {
                mListView.smoothScrollToPositionFromTop(
                        position, listTopOffset, GOTO_SCROLL_DURATION);
                return true;
            } else {
                mListView.setSelectionFromTop(position, listTopOffset);
                // Perform any after scroll operations that are needed
                Log.i("tag", "goTo call onScrollStateChanged(OnScrollListener.SCROLL_STATE_IDLE)");
                onScrollStateChanged(mListView, OnScrollListener.SCROLL_STATE_IDLE);
            }*/
        	
        	mListView.setSelectionFromTop(position, 0);
        } else if (setSelected) {
            // Otherwise just set the selection
            //setMonthDisplayed(mSelectedDay, true);
        }
        return false;
    }
	
	
	public boolean goToToday(long time, boolean animate, boolean setSelected, boolean forceScroll) {        
		int childCount = mListView.getChildCount();		
		int lastChildCountIndex = childCount - 1;
		int firstVisiblePositon = mListView.getFirstVisiblePosition();		
		int lastVisiblePositon = mListView.getLastVisiblePosition();		
		int lastVisibleChildIndex = lastVisiblePositon - firstVisiblePositon;
		
		if (lastVisibleChildIndex > (lastChildCountIndex)) {
			return false;
		}
		// ������ �߻��Ѵ�!!!!!!!!!!!!!!!!!!!!!!!
		// :Ư���⵵�� ��Ȯ�ϰ� listview�� top�� ��ġ ���ѵ�,
		//  ������ s4�� ���
		//  Ư���⵵ ���� ���� �⵵�� 1 �ȼ��� listview�� �������� �ȴ�
		//  ->�ذ�å�� ��Ȯ�� today number�� top ���̴�
		//int lastTop = lastVisibleChild.getTop();
		//int listBottom = mListView.getBottom();
		//int xxx = mAnticipationListViewHeight;
		
		YearMonthTableLayout firstVisibleChild = (YearMonthTableLayout) mListView.getChildAt(0);   	
		Calendar firstMonthTimeOfFirstVisibleChild = GregorianCalendar.getInstance(firstVisibleChild.getFirstMonthTime().getTimeZone());//child.getFirstMonthTime();		
		firstMonthTimeOfFirstVisibleChild.setTimeInMillis(firstVisibleChild.getFirstMonthTime().getTimeInMillis());
				
		YearMonthTableLayout lastVisibleChild = (YearMonthTableLayout) mListView.getChildAt(lastVisibleChildIndex);		
		Calendar lastMonthTimeOfLastVisibleChild = GregorianCalendar.getInstance(lastVisibleChild.getFirstMonthTime().getTimeZone());//child.getFirstMonthTime();		
		lastMonthTimeOfLastVisibleChild.setTimeInMillis(lastVisibleChild.getFirstMonthTime().getTimeInMillis());
		lastMonthTimeOfLastVisibleChild.add(Calendar.MONTH, 2);
		lastMonthTimeOfLastVisibleChild.set(Calendar.DAY_OF_MONTH, lastMonthTimeOfLastVisibleChild.getActualMaximum(Calendar.DAY_OF_MONTH));    	
		
		long todayMillis = System.currentTimeMillis();
		Calendar todayCal = GregorianCalendar.getInstance(mTimeZone);
		todayCal.clear();
		todayCal.setFirstDayOfWeek(mFirstDayOfWeek);
		todayCal.setTimeInMillis(todayMillis);
		// today�� ���� view���� ���������� �� ���̴����� Ȯ���ؾ� �Ѵ�
		if (checkTimeMillisAtPeriod(firstMonthTimeOfFirstVisibleChild.getTimeInMillis(), lastMonthTimeOfLastVisibleChild.getTimeInMillis(), todayMillis)) {
						
			YearMonthTableLayout child = null;
			Calendar toCal = GregorianCalendar.getInstance(mTimeZone);
			boolean escapeLooping = false;
			for (int i=0; i<childCount; i++) {
				child = (YearMonthTableLayout) mListView.getChildAt(i);
				
				toCal.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());
				toCal.add(Calendar.MONTH, 2);
				toCal.set(Calendar.DAY_OF_MONTH, toCal.getActualMaximum(Calendar.DAY_OF_MONTH));	
				
				if (checkTimeMillisAtPeriod(child.getFirstMonthTime().getTimeInMillis(), toCal.getTimeInMillis(), todayMillis)) {
					for (int j=0; j<3; j++) {
						toCal.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());
						if (j != 0)
							toCal.add(Calendar.MONTH, j);
						toCal.set(Calendar.DAY_OF_MONTH, toCal.getActualMaximum(Calendar.DAY_OF_MONTH));	
						if (checkTimeMillisAtPeriod(child.getFirstMonthTime().getTimeInMillis(), toCal.getTimeInMillis(), todayMillis)) {
							escapeLooping = true;
							break;
						}
					}
					
					if (escapeLooping)
						break;
				}				
				
			}
			
			if (child == null) {
				return false;
			}
			else {				
				int childTop = child.getTop();
				int childPattern = child.getMonthsPattern();
				int monthRegionTop = childTop;/////////////////////////////////////////////////////////////////////////
				if (childPattern == YearMonthTableLayout.YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {
					monthRegionTop = monthRegionTop + mAnticipationYearIndicatorHeight;
				}
				
				// ���� ������ today�� day of month�� ����°�� �ִ°� �ϴ°��̴�...
				int weekNumberOfToday = todayCal.get(Calendar.WEEK_OF_MONTH);
				int topOffset = monthRegionTop + mAnticipationMiniMonthIndicatorTextBaseLineY + ((weekNumberOfToday - 1) * mAnticipationMiniMonthDayTextBaseLineY);
								
		    	float textCenterY = topOffset + mDayOfMonthTextCenterOffsetY;
		    					
		    	int top = (int) (textCenterY - mOvalDrawableOfTodaySize);
		    	int bottom = top + mOvalDrawableOfTodaySize;
		    	
		    	if (0 <= top && bottom <= mListView.getBottom()) {
		    				    		
		    		Calendar goToMonth = GregorianCalendar.getInstance(mTimeZone);
		    		goToMonth.clear();
		    		goToMonth.setTimeInMillis(todayCal.getTimeInMillis());
		    		goToMonth.set(Calendar.DAY_OF_MONTH, 1);
		    		goToMonth.set(Calendar.HOUR_OF_DAY, 0);
		    		goToMonth.set(Calendar.MINUTE, 0);
		    		goToMonth.set(Calendar.SECOND, 0);		
		    		goToMonth.set(Calendar.MILLISECOND, 0);	
		    		
		    		int currentMonthViewMode = Utils.getSharedPreference(mContext, SettingsFragment.KEY_MONTH_VIEW_LAST_MODE, SettingsFragment.NORMAL_MONTH_VIEW_MODE);
		            
		    		YearViewExitAnimObject yearViewExitAnimObject = new YearViewExitAnimObject(mContext, 
		    				this, 
		        			this.mAdapter, 
		        			mListView, 
		        			mController, 
		        			mTzId, 
		        			currentMonthViewMode);
		    		
		    		if (currentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {     
		    			// today�� �����ϸ� �ش���� 1���� �ƴ� today�� day of month�� �����ؾ� �Ѵ�
                		yearViewExitAnimObject.startOneDayEventsLoad(ETime.getJulianDay(todayCal.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
                	}		    		
		    		
		    		int monthNumber = goToMonth.get(Calendar.MONTH);
		    		int columnNumber = monthNumber % 3;
		    		switch(columnNumber) {		    		
		    		case 0:
		    			child.mClickedMonthColumnIndex = YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX;
		    			break;
		    		case 1:
		    			child.mClickedMonthColumnIndex = YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX;
		    			break;
		    		case 2:
		    			child.mClickedMonthColumnIndex = YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX;
		    			break;
		    		default:
		    			return false;
		    		}
		    		
		    		MiniMonthInYearRow mTodayMiniMonth = child.getClickedMonthView();
		    		mTodayMiniMonth.setVisibility(View.INVISIBLE);
		    		
		    		yearViewExitAnimObject.init(child, goToMonth);
		    		yearViewExitAnimObject.startListViewScaleAnim();
		        	
		    		return true;
		    	}
		    	    	
			}
		}
		
		// Set the selected day
        if (setSelected) {
        	mSelectedYear.setTimeInMillis(time);        	
        }
        
        mController.setTime(mSelectedYear.getTimeInMillis());
        
		// today�� ���� view���� �� ��������
		// -> +/- 4�� �̳��� �ִٸ�, goto animation�� �����Ѵ�
		// -> +/- 4�� �̳��� ���ٸ�, goto animation�� �������� �ʴ´�
		int yearOfToday = todayCal.get(Calendar.YEAR);
		int yearOfFirstVisibleChild = firstMonthTimeOfFirstVisibleChild.get(Calendar.YEAR);
				
		boolean animateToday = false;
		int diff = yearOfToday - yearOfFirstVisibleChild;
		if (diff > 0) {
			if (diff < 5) {				
				// upward fling �ִϸ��̼��� �����Ѵ�
				animateToday = true;
				
				UnvisibleMonthTablePart obj = mListView.calcUnvisibleMonthTablePartOfVisibleLastMonthTable();
				int accumulatedDistance = obj.mUnvisibleYearHeight; 		    	
		    	Calendar YearTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
		    	YearTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
		    	YearTimeCalendar.setTimeInMillis(obj.mCompletionYearMillis);
		    		    			    	
		    	int howMuchYear = diff - 1;
		    	if (howMuchYear != 0) {
		    		int oneYearHeight = mAnticipationYearIndicatorHeight + (4 * mAnticipationMiniMonthHeight);
		    		
		    		accumulatedDistance = accumulatedDistance + (howMuchYear * oneYearHeight);		    		
		    	}		    	
		    	
		    	int velocity = mListView.getSplineFlingAbsVelocity(accumulatedDistance);   	
		    	FlingParameter flingObj = new FlingParameter(velocity, accumulatedDistance);
		    	float delta = flingObj.mDistance;
            	float maxDelta = delta * 2;  
        		                   
            	setFlingContext(YearListView.UPWARD_FLING_MODE, flingObj.mDistance, YearListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());        		
                
                kickOffFlingComputationRunnable(); 		                       
			}
		}
		else if (diff == 0) {
			// ������ downward fling �ִϸ��̼��� �����Ѵ�					
			animateToday = true;
			
			UnvisibleMonthTablePart obj = mListView.calcUnvisibleMonthTablePartOfVisibleFirstMonthTable();
	    	int accumulatedDistance = obj.mUnvisibleYearHeight;  	
	    	
	    	Calendar YearTimeCalendar = GregorianCalendar.getInstance(mTempTime.getTimeZone());        
	    	YearTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
	    	YearTimeCalendar.setTimeInMillis(obj.mCompletionYearMillis);  
	    	
	    	int velocity = mListView.getSplineFlingAbsVelocity(accumulatedDistance);   	
	    	FlingParameter flingObj = new FlingParameter(velocity, accumulatedDistance);
	    	float delta = flingObj.mDistance;
        	float maxDelta = delta * 2;  
    		                   
        	setFlingContext(YearListView.DOWNWARD_FLING_MODE, flingObj.mDistance, YearListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());        		
            
            kickOffFlingComputationRunnable(); 		
			
		}
		else { // diff < 0
			if (Math.abs(diff) < 5) {
				// downward fling �ִϸ��̼��� �����Ѵ�
				animateToday = true;
				
				UnvisibleMonthTablePart obj = mListView.calcUnvisibleMonthTablePartOfVisibleFirstMonthTable();
		    	int accumulatedDistance = obj.mUnvisibleYearHeight;  	
		    	
		    	Calendar YearTimeCalendar = GregorianCalendar.getInstance(mTempTime.getTimeZone());        
		    	YearTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
		    	YearTimeCalendar.setTimeInMillis(obj.mCompletionYearMillis);    
				
		    	int howMuchYear = Math.abs(diff);
		    	if (howMuchYear != 0) {
		    		int oneYearHeight = mAnticipationYearIndicatorHeight + (4 * mAnticipationMiniMonthHeight);
		    		
		    		accumulatedDistance = accumulatedDistance + (howMuchYear * oneYearHeight);		    		
		    	}		    	
		    	
		    	int velocity = mListView.getSplineFlingAbsVelocity(accumulatedDistance);   	
		    	FlingParameter flingObj = new FlingParameter(velocity, accumulatedDistance);
		    	float delta = flingObj.mDistance;
            	float maxDelta = delta * 2;  
        		                   
            	setFlingContext(YearListView.DOWNWARD_FLING_MODE, flingObj.mDistance, YearListView.calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());        		
                
                kickOffFlingComputationRunnable(); 		      
			}
		}
		        
	
		if (!animateToday) {	
	        int targetYear = mSelectedYear.get(Calendar.YEAR);
	        int yearsFromEpoch = targetYear - YearsAdapter.EPOCH_YEAR;
	        int monthsFromEpoch = yearsFromEpoch * 12;
	        int position = monthsFromEpoch / 3;     
	        
	        View child;
	        int i = 0;
	        int top = 0;
	        // Find a child that's completely in the view
	        do {
	            child = mListView.getChildAt(i++);
	            if (child == null) {
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
	        
	        int lastPosition = firstPosition + mMonthRows - 1;
	        if (top > BOTTOM_BUFFER) {
	            lastPosition--; // � ��Ȳ�� �߻��ϴ°�? : 
	                            // :-����̽��� sleep ���¿��� wakeup �Ǿ��� ��
	                            //  -�Ƹ��� today�� �����Ͽ��� ���?
	        }      
	        
	        // Check if the selected day is now outside of our visible range
	        // and if so scroll to the month that contains it
	        if (position < firstPosition || position > lastPosition || forceScroll) {
	            
	        	mListView.setSelectionFromTop(position, 0);
	        }
        }
        return false;
    }
	
	public boolean checkTimeMillisAtPeriod(long fromMillis, long toMillis, long checkTimeMillis) {
		Calendar fromTime = GregorianCalendar.getInstance(mTimeZone);
		fromTime.clear();
		fromTime.setTimeInMillis(fromMillis);
		fromTime.set(Calendar.HOUR_OF_DAY, 0);
		fromTime.set(Calendar.MINUTE, 0);
		fromTime.set(Calendar.SECOND, 0);
		fromTime.set(Calendar.MILLISECOND, 0);
    			
		
		Calendar toTime = GregorianCalendar.getInstance(mTimeZone);
		toTime.clear();
		toTime.setTimeInMillis(toMillis);		
		toTime.set(Calendar.HOUR_OF_DAY, 23);
		toTime.set(Calendar.MINUTE, 59);
		toTime.set(Calendar.SECOND, 59);
		toTime.set(Calendar.MILLISECOND, 999);
    					
		if (fromTime.getTimeInMillis() <= checkTimeMillis && checkTimeMillis <= toTime.getTimeInMillis()) {			
			return true;
		}
		else {
			return false;
		}
	}

	public void setUpAdapter() {
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        HashMap<String, Integer> monthParams = new HashMap<String, Integer>();
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH, mAnticipationMonthListViewWidth);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT, mAnticipationMonthListViewHeight);
        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mAnticipationMiniMonthWidth);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mAnticipationMiniMonthHeight);
        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mAnticipationYearIndicatorHeight);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mAnticipationYearIndicatorTextHeight);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mAnticipationYearIndicatorTextBaseLineY);
        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, mAnticipationMiniMonthIndicatorTextHeight);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, mAnticipationMiniMonthIndicatorTextBaseLineY);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, mAnticipationMiniMonthDayTextBaseLineY);
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, mAnticipationMiniMonthDayTextHeight);
        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN, mAnticipationMiniMonthLeftMargin);
        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT, (int) mMonthListViewNormalWeekHeight);
        
        //monthParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT, (int) mMonthListViewLastWeekHeight);
                
        monthParams.put(YearsAdapter.MONTHS_PARAMS_NUM_WEEKS, mNumWeeks);        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_WEEK_START, mFirstDayOfWeek);        
        monthParams.put(YearsAdapter.MONTHS_PARAMS_PARAMS_SELECTED_YEAR_JULIAN_DAY,
                ETime.getJulianDay(mSelectedYear.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
        monthParams.put(YearsAdapter.MONTHS_PARAMS_DAYS_PER_WEEK, mDaysPerWeek);        
        
        mListViewState = ListViewState.Run;   
        
        if (mAdapter == null) {
            mAdapter = new YearsAdapter(mContext, mActivity, monthParams);
            //mAdapter.registerDataSetObserver(mObserver);
        } else {
            mAdapter.updateParams(monthParams);
        }
        //mAdapter.notifyDataSetChanged();
    }
	
	protected void setUpListView() {
        // Configure the listview
        mListView = (YearListView)getListView();
        
        RelativeLayout.LayoutParams yearListViewParams = (RelativeLayout.LayoutParams) mListView.getLayoutParams();
        yearListViewParams.height = mAnticipationListViewHeight;
    	mListView.setLayoutParams(yearListViewParams);
    	
        mListView.setListFragment(this);
        
        // No dividers
        mListView.setDivider(null);
        // Items are clickable
        mListView.setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
        mListView.setFastScrollEnabled(false); // �����ʿ� �н�Ʈ ��ũ�ѹٰ� �����ȴ� : �н�Ʈ ��ũ�ѿ� ���� ���� ������ �𸥴�
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setOnScrollListener(this);
        mListView.setFadingEdgeLength(0);
        
        mListView.setClipChildren(false);
        
        mListView.setBackgroundColor(Color.WHITE);
        
        // Make the scrolling behavior nicer
        mListView.setFriction(ViewConfiguration.getScrollFriction() * mFriction);
    }
	
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			//if (INFO) Log.i(TAG, "SCROLL_STATE_IDLE");
			
			int childCount = view.getChildCount();
			for (int i=0; i<childCount; i++) {
				YearMonthTableLayout child = (YearMonthTableLayout) view.getChildAt(i);
				
				if (i == 0) {
					if (child == null) {			        	
			        	if (INFO) Log.i(TAG, "onScrollStateChanged:child null");
			            return;
			        }
				}
				
				if (child.getTop() < 0) {
					continue;
				}
				
				if (child.getFirstMonthTime().getTimeInMillis() != mController.getTime()) {
					
					mController.setTime(child.getFirstMonthTime().getTimeInMillis());
					String date = ETime.format2445(child.getFirstMonthTime());
					if (INFO) Log.i(TAG, "SCROLL_STATE_IDLE:" + date.toString());
					break;					
				}		
			}
			
		}
		/*else if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			if (INFO) Log.i(TAG, "SCROLL_STATE_TOUCH_SCROLL");
		}
		else if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
			if (INFO) Log.i(TAG, "SCROLL_STATE_FLING");
		}*/		
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
				
		YearMonthTableLayout child = (YearMonthTableLayout) view.getChildAt(0);
		if (child == null) {
        	Log.i("tag", "YearPickerFragment::onScroll: child null");
            return;
        }
		
		//long childTopMillis = child.getFirstMonthTime().getTimeInMillis();
		//setYearDisplayed(child.getFirstMonthTime());
		
		if (mListViewState == ListViewState.Run_FlingState) {    		   
 		   postFlingComputationRunnable();
 	   	}	   
		
	}
	

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		//mDesiredDay.setTimeInMillis(System.currentTimeMillis());
		
		return false;
	}
	
	@Override
	public void eventsChanged() {
        
    }

	@Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO;
    }
	
	@Override
	public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.GO_TO) {
            if ((event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0) {
            	goToToday(event.selectedTime.getTimeInMillis(), true, true, false);
            }
            else {
            	goTo(event.selectedTime.getTimeInMillis(), false, true, false);
            }
        } 
	}
	
	//int mCurrentYearDisplayed;
	public void setYearDisplayed(Calendar time) {
		//mCurrentYearDisplayed = time.get(Calendar.MONTH);   		
   		      
   		long newTime = time.getTimeInMillis();
   		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   		
   		if (newTime != mController.getTime()) {
   			long diff = Math.abs(newTime - mController.getTime());
   			long offset = (DateUtils.YEAR_IN_MILLIS) / 2;
   			if (diff > offset)
   				mController.setTime(newTime);
   		}
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
       
   }
	
	/*
	@Override
	public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.GO_TO) {
            boolean animate = true;
            if (mDaysPerWeek * mNumWeeks * 2 < Math.abs(
                    ETime.getJulianDay(event.selectedTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek)
                    - ETime.getJulianDay(mFirstVisibleDay.getTimeInMillis(), mTimeZone, mFirstDayOfWeek)
                    - mDaysPerWeek * mNumWeeks / 2)) {
                animate = false;
            }
            
            mDesiredDay.setTimeInMillis(event.selectedTime.getTimeInMillis());                     
            
            Log.i("tag", "MBWF::handleEvent : mDesiredDay=" + ETime.format2445(mDesiredDay));
            
            boolean animateToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
            // today�� �����Ͽ��� ��� ����� ���̴�
            boolean delayAnimation = goTo(event.selectedTime.getTimeInMillis(), animate, true, false);
            if (animateToday) {
                // If we need to flash today start the animation after any
                // movement from listView has ended.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((YearsAdapter) mAdapter).animateToday();
                        mAdapter.notifyDataSetChanged();
                    }
                }, delayAnimation ? GOTO_SCROLL_DURATION : 0);
            }
        } 
	}
	*/
	
	int mFlingDirection;
	int mFlingDistance;
	long mDurationTimeMillis;
	long mStartTimeMillis;
	int mPrvAcculmulatedFlingDistance;
	
	public void setFlingContext(int direction, int flingDistance, long durationTime, long startTime) {
		mFlingDirection = direction;
		mFlingDistance = flingDistance;
		mDurationTimeMillis = durationTime;
		mStartTimeMillis = startTime;
		mPrvAcculmulatedFlingDistance = 0;	
		
		mListViewState = ListViewState.Run_FlingState;		
	}
	
	public void kickOffFlingComputationRunnable() {		
		FlingComputationRunnable kickOff = new FlingComputationRunnable();		
		mHandler.post(kickOff);		
	}
	
	
	FlingComputationRunnable mFlingComputationRunnable = null;
	public void postFlingComputationRunnable() {
		mFlingComputationRunnable = new FlingComputationRunnable();		
		mHandler.post(mFlingComputationRunnable);		
	}
	
	public void stopFlingComputation() {
		if (mFlingComputationRunnable != null) {			
			mHandler.removeCallbacks(mFlingComputationRunnable);
		}
		
		mListViewState = ListViewState.Run;
			
		mListView.smoothScrollBy(0, 0);	
	}
	
	private class FlingComputationRunnable implements Runnable {

		@Override
		public void run() {			
			computeFling();
		}		
	}	
	
	public void computeFling() {
		
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
		
		if (mFlingDirection == YearListView.UPWARD_FLING_MODE) {					
			//if (INFO) Log.i(TAG, "YearListView.UPWARD_FLING_MODE");
			int firstVisiblePosition = mListView.getFirstVisiblePosition();
	    	int lastVisiblePosition = mListView.getLastVisiblePosition();
	    	int lastChildViewIndex = lastVisiblePosition - firstVisiblePosition;    	
	    	
	    	YearMonthTableLayout lastChild = (YearMonthTableLayout) mListView.getChildAt(lastChildViewIndex);				
			
			int lastChildTop = lastChild.getTop();
			lastChildTop = lastChildTop + delta;					
					
			mListView.setSelectionFromTop(lastVisiblePosition, lastChildTop);
					
		}
		else {
			//if (INFO) Log.i(TAG, "YearListView.DOWNWARD_FLING_MODE");
			
			delta = Math.abs(delta);
			
			int firstVisiblePosition = mListView.getFirstVisiblePosition();
			YearMonthTableLayout firstView = (YearMonthTableLayout) mListView.getChildAt(0); 	
						
			int firstChildTop = firstView.getTop(); //Top position of this view relative to its parent		
			firstChildTop = firstChildTop + delta;		
			
			mListView.setSelectionFromTop(firstVisiblePosition, firstChildTop);
			
		}
					
		mPrvAcculmulatedFlingDistance = accumulatedFlingDistance;
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
			//if (INFO) Log.i(TAG, "calcAccumulatedDistance:OVER 1 TIME RATE!!!!");
		}
		
		float timeRate = getInterpolation(normaledTimeRate);
		
		int accumulatedFlingDistance = (int) (mFlingDistance * timeRate);		
		
		return accumulatedFlingDistance;
	}
	
	
	public static final float YEARINDICATOR_HEIGHT_RATIO_BY_LISTVIEW_HEIGHT = 0.09f;
	public static final float YEARINDICATOR_TEXT_HEIGHT_RATIO_BY_YEARINDICATOR_HEIGHT = 0.6f;
	public static final float YEARINDICATOR_TEXT_BASELINE_Y_RATIO_BY_YEARINDICATOR_HEIGHT = 0.85f;
	
	public static final float MINIMONTH_HEIGHT_RATIO_BY_MONTH_LISTVIEW_HEIGHT = 0.25f;
	public static final float MINIMONTH_INDICATOR_TEXT_HEIGHT_RATIO_BY_MONTH_LISTVIEW_MONTHINDICATOR_TEXT_HEIGHT = 0.875f;
	public static final float MINIMONTH_INDICATOR_TEXT_BASELINE_Y_RATIO_BY_MONTH_LISTVIEW_MONTHINDICATOR_TEXT_BASELINE_Y = 0.75f;
	public static final float MINIMONTH_TEXT_BASELINE_Y_RATIO_BY_MONTH_LISTVIEW_MONTH_TEXT_BASELINE_Y = 0.5f;
	public static final float MINIMONTH_TEXT_HEIGHT_RATIO_BY_MONTH_LISTVIEW_MONTH_TEXT_HEIGHT = 0.4f;
	
	public static final float MINIMONTH_WIDTH_RATIO_BY_LISTVIEW_WIDTH = 0.28f;
	public static final float MINIMONTH_LEFTMARGIN_RATIO_BY_LISTVIEW_WIDTH = 0.04f;
	
	int mMonthListViewNormalWeekHeight;
	//float mMonthListViewLastWeekHeight;
	int mMonthListViewMonthIndicatorHeight;
	float mMonthListViewMonthIndicatorTextHeight;
	float mMonthListViewMonthIndicatorTextBaselineY;
	float mMonthListMonthDayTextBaselineY;
	float mMonthListMonthDayTextHeight;
	
	int mMonthListViewMonthIndicatorHeightInEPMode;
    int mMonthListViewNormalWeekHeightInEPMode;
    //float mMonthListViewLastWeekHeightInEPMode;
    float mMonthListMonthDayTextBaselineYInEPMode;
    float mMonthListViewMonthIndicatorTextHeightInEPMode;
    float mMonthListViewMonthIndicatorTextBaselineYInEPMode;
    float mMonthListMonthDayTextHeightInEPMode;
    
    int mDayHeaderHeight;
    
    Paint mMonthDayPaint;
    float mDayOfMonthTextAscent;
	float mDayOfMonthTextDescent;
	
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
    	// lower action bar : 48dip ->calendar_view_lower_actionbar_height
    	//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ?, getResources().getDisplayMetrics());
    	int upperActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_upper_actionbar_height);  
    	mDayHeaderHeight = (int)getResources().getDimension(R.dimen.day_header_height);
    	int lowerActionBarHeight = (int)getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);
    	
    	mAnticipationMonthListViewHeight = deviceHeight - StatusBarHeight - upperActionBarHeight - mDayHeaderHeight - lowerActionBarHeight;      	
    	mAnticipationMonthListViewWidth = mAnticipationListViewWidth = rectgle.right - rectgle.left;
    	    	
    	mAnticipationListViewHeight = deviceHeight - StatusBarHeight - upperActionBarHeight - lowerActionBarHeight;    	
    	
    	mMonthListViewMonthIndicatorHeight = (int) (mAnticipationMonthListViewHeight * MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT);
    	
    	mMonthListViewNormalWeekHeight = (int) (mAnticipationMonthListViewHeight * MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT);
    	
    	mMonthListMonthDayTextBaselineY = mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT;
    	
    	float monthListViewHeightForCalcingMiniMonthHeight = mMonthListViewMonthIndicatorHeight +
    			(mMonthListViewNormalWeekHeight * 5) +
    			mMonthListMonthDayTextBaselineY;
    	    	
    	mMonthListViewMonthIndicatorTextBaselineY = mMonthListViewMonthIndicatorHeight * MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT;
    	    	
    	mMonthListMonthDayTextHeight = mAnticipationMonthListViewHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT;
    	mMonthListViewMonthIndicatorTextHeight = mMonthListMonthDayTextHeight;
    	
    	mAnticipationYearIndicatorHeight = (int) (mAnticipationListViewHeight * YEARINDICATOR_HEIGHT_RATIO_BY_LISTVIEW_HEIGHT);    	
    	mAnticipationYearIndicatorTextHeight = (int) (mAnticipationYearIndicatorHeight * YEARINDICATOR_TEXT_HEIGHT_RATIO_BY_YEARINDICATOR_HEIGHT);
    	mAnticipationYearIndicatorTextBaseLineY = (int) (mAnticipationYearIndicatorHeight * YEARINDICATOR_TEXT_BASELINE_Y_RATIO_BY_YEARINDICATOR_HEIGHT);    	
    	
    	mAnticipationMiniMonthHeight = (mAnticipationListViewHeight - mAnticipationYearIndicatorHeight) / 4;
    	//mAnticipationMiniMonthHeight = (int) (monthListViewHeightForCalcingMiniMonthHeight * MINIMONTH_HEIGHT_RATIO_BY_MONTH_LISTVIEW_HEIGHT);
    	mAnticipationMiniMonthIndicatorTextHeight = (int) (mMonthListViewMonthIndicatorTextHeight * 
    			MINIMONTH_INDICATOR_TEXT_HEIGHT_RATIO_BY_MONTH_LISTVIEW_MONTHINDICATOR_TEXT_HEIGHT);
    	mAnticipationMiniMonthIndicatorTextBaseLineY = (int) (mMonthListViewMonthIndicatorTextBaselineY * 
    			MINIMONTH_INDICATOR_TEXT_BASELINE_Y_RATIO_BY_MONTH_LISTVIEW_MONTHINDICATOR_TEXT_BASELINE_Y);	
    	mAnticipationMiniMonthDayTextBaseLineY = (int) (mMonthListMonthDayTextBaselineY * 
    			MINIMONTH_TEXT_BASELINE_Y_RATIO_BY_MONTH_LISTVIEW_MONTH_TEXT_BASELINE_Y);
    	mAnticipationMiniMonthDayTextHeight = (int) (mMonthListMonthDayTextHeight * MINIMONTH_TEXT_HEIGHT_RATIO_BY_MONTH_LISTVIEW_MONTH_TEXT_HEIGHT);
    	
    	mAnticipationMiniMonthWidth = (int) (mAnticipationListViewWidth * MINIMONTH_WIDTH_RATIO_BY_LISTVIEW_WIDTH);
    	mAnticipationMiniMonthLeftMargin = (int) (mAnticipationListViewWidth * MINIMONTH_LEFTMARGIN_RATIO_BY_LISTVIEW_WIDTH);   
    	    	
    	mMonthListViewNormalWeekHeightInEPMode = (int) (mMonthListViewNormalWeekHeight * MonthListView.EEVM_WEEK_HEIGHT_RATIO_BY_NM_WEEK_HEIGHT);    	
    	mMonthListViewMonthIndicatorHeightInEPMode = (int) (mMonthListViewMonthIndicatorHeight * MonthListView.EEVM_MONTHINDICATOR_HEIGHT_RATIO_BY_NM_MONTHINDICATOR_HEIGHT);        
    	mMonthListViewMonthIndicatorTextHeightInEPMode = mMonthListViewMonthIndicatorTextHeight;    	
    	mMonthListViewMonthIndicatorTextBaselineYInEPMode = mMonthListViewMonthIndicatorHeightInEPMode * MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT;     	
        mMonthListMonthDayTextHeightInEPMode = mMonthListMonthDayTextHeight;        
    	mMonthListMonthDayTextBaselineYInEPMode = mMonthListViewNormalWeekHeightInEPMode * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT;
    
    	mMonthDayPaint = new Paint();
        mMonthDayPaint.setFakeBoldText(true);
        mMonthDayPaint.setAntiAlias(true);   		        
        mMonthDayPaint.setTextAlign(Align.CENTER);
        mMonthDayPaint.setTypeface(Typeface.MONOSPACE); 
        mMonthDayPaint.setTextSize(mAnticipationMiniMonthDayTextHeight);
        
        mDayOfMonthTextAscent = mMonthDayPaint.ascent();
        mDayOfMonthTextDescent = mMonthDayPaint.descent();
        
        //float baseLineY = mAnticipationMiniMonthDayTextBaseLineY;		    	
    	
        float dayOfMonthTextTopY = mAnticipationMiniMonthDayTextBaseLineY + mDayOfMonthTextAscent;//float textTopY = Math.abs(baseLineY + textAscent); 	
    	
        float dayOfMonthTextHeight = Math.abs(mDayOfMonthTextAscent) + mDayOfMonthTextDescent;
        
        mDayOfMonthTextCenterOffsetY = dayOfMonthTextTopY + (dayOfMonthTextHeight / 2);
        
        float ovalDrawableOfTodayRadius = mAnticipationMiniMonthDayTextBaseLineY * 0.4f;
    	
        mOvalDrawableOfTodaySize = (int)(ovalDrawableOfTodayRadius * 2);		
    }
	
	//float mDayOfMonthTextTopY;
	//float mDayOfMonthTextHeight;
	
	float mDayOfMonthTextCenterOffsetY;
	int mOvalDrawableOfTodaySize;
	
	
	
	public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
	
	

}
