package com.intheeast.year;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.R;
import com.intheeast.etc.ETime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
//import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class YearMonthTableLayout extends LinearLayout {
	
	public final static int INVALIDE_COLUMN_MONTH_INDEX = -1;
	public final static int FIRST_COLUMN_MONTH_INDEX = 1;
	public final static int SECOND_COLUMN_MONTH_INDEX = 2;
	public final static int THIRD_COLUMN_MONTH_INDEX = 3;
	
	public static final String VIEW_PARAMS_YEAR_MONTHS_PATTERN = "year_months_pattern";
	
	public static final String VIEW_PARAMS_HEIGHT = "view_height";
	
	public static final String VIEW_PARAMS_POSITION = "view_position";
	
	public static final String VIEW_PARAMS_WEEK_START = "week_start";
	
	public static final String VIEW_PARAMS_TARGETMONTH_JULIANDAY = "target_month_julianday";
	
	public static final int NORMAL_YEAR_MONTHS_PATTERN = 0;
	
	public static final int YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN = 1;	
	
    public static final String NOT_MONTHDAY = "XXX";
    
	Context mContext;
	
	LinearLayout mMonthRow;
	YearIndicatorRegion mYearIndicator;
	MiniMonthInYearRow mFirstColumnMonth;
	MiniMonthInYearRow mSecondColumnMonth;
	MiniMonthInYearRow mThirdColumnMonth;	
	
	
	String mTzId = ETime.getCurrentTimezone();
	TimeZone mTimeZone = TimeZone.getTimeZone(mTzId);
	long mGmtoff;
	
	Calendar mFristMonthTime;
	Calendar mSecondMonthTime;
	Calendar mThirdMonthTime;
	
	int mFristMonthJulianDay;
	
	static int mWidth;
	int mHeight;
	
	static int mYearIndicatorHeight;
	int mYearIndicatorTextHeight;
	static int mYearIndicatorTextBaseLineY;
	int mMiniMonthHeight;
	int mMiniMonthIndicatorTextHeight;
	int mMiniMonthIndicatorTextBaseLineY;	
	int mMiniMonthDayTextBaseLineY;
	int mMiniMonthDayTextHeight;
	static int mMiniMonthWidth;
	static int mMiniMonthLeftMargin;
	
	int mPosition;
	int mWeekStart;
		
	int mMonthsPattern;
	boolean mInitialized = false;
	
	
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
    ArrayList<String[]> m1stMonthWeekDayNumberList = new ArrayList<String[]>(); 
	ArrayList<String[]> m2ndMonthWeekDayNumberList = new ArrayList<String[]>(); 
	ArrayList<String[]> m3rdMonthWeekDayNumberList = new ArrayList<String[]>(); 
	
	static String mYearIndicatorText;
	String mFirstMonthText;
	String mSecondMonthText;
	String mThirdMonthText;
	
	
	public YearMonthTableLayout(Context context) {
		super(context);
		
		mContext = context;		
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        
        setBackgroundColor(Color.TRANSPARENT);
	}
	
	//
	public YearMonthTableLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        
        setBackgroundColor(Color.TRANSPARENT);
	}		
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;        
    }
	
	public void setMonthsParams(HashMap<String, Integer> params, String tz) {
		setTag(params);
		
		mTzId = tz;
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT)) {
        	mYearIndicatorHeight = params.get(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        } 
		
		if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
        	mWeekStart = params.get(VIEW_PARAMS_WEEK_START);			
        }
		
		setChildViewLayout();        
	}
	
	public void setChildViewLayout() {		
		mYearIndicator = (YearIndicatorRegion) findViewById(R.id.year_indicator);
		LayoutParams yearIndicatorParams = new LayoutParams(LayoutParams.MATCH_PARENT, mYearIndicatorHeight);
		mYearIndicator.setLayoutParams(yearIndicatorParams);
		mYearIndicator.setVisibility(View.GONE);		
		
		mMonthRow = (LinearLayout) findViewById(R.id.months_row);		
		mFirstColumnMonth = (MiniMonthInYearRow) findViewById(R.id.first_column_month);
		LayoutParams firstMiniMonthParams = new LayoutParams(mMiniMonthWidth, mMiniMonthHeight);		
		firstMiniMonthParams.setMargins(mMiniMonthLeftMargin, 0, 0, 0);
		mFirstColumnMonth.setLayoutParams(firstMiniMonthParams);	
		
			
		mSecondColumnMonth = (MiniMonthInYearRow) findViewById(R.id.second_column_month);
		LayoutParams secondMiniMonthParams = new LayoutParams(mMiniMonthWidth, mMiniMonthHeight);
		secondMiniMonthParams.setMargins(mMiniMonthLeftMargin, 0, 0, 0);
		mSecondColumnMonth.setLayoutParams(secondMiniMonthParams);
		
		mThirdColumnMonth = (MiniMonthInYearRow) findViewById(R.id.third_column_month);
		LayoutParams thirdMiniMonthParams = new LayoutParams(mMiniMonthWidth, mMiniMonthHeight);	
		thirdMiniMonthParams.setMargins(mMiniMonthLeftMargin, 0, 0, 0);
		mThirdColumnMonth.setLayoutParams(thirdMiniMonthParams);
	}
	
	
	public void setMonthInfo(boolean forceInvalidate, int monthPattern, int position) {
		mMonthsPattern = monthPattern;
		
		if (mMonthsPattern == YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {								
			mHeight = mYearIndicatorHeight + mMiniMonthHeight;
		}
		else {				
			mHeight = mMiniMonthHeight;
		}
		
		mPosition = position;
		calcMonthsTime(mPosition); 
		
		HashMap<String, Integer> drawingParams = (HashMap<String, Integer>) getTag();
		setYearIndicator(drawingParams);
        setChildAtMonthRow(drawingParams, forceInvalidate);        
        
	}
	
	public void setYearIndicator(HashMap<String, Integer> params) {
		if (mMonthsPattern == YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {		
			if (mYearIndicator.getVisibility() != View.VISIBLE)
				mYearIndicator.setVisibility(View.VISIBLE);
			
			mYearIndicator.setDrawingParams(params);
			mYearIndicator.setYearText(mYearIndicatorText);		
			mYearIndicator.invalidate();
		}
		else {	
			if (mYearIndicator.getVisibility() != View.GONE)
				mYearIndicator.setVisibility(View.GONE);
		}
	}
	
	public void setChildAtMonthRow(HashMap<String, Integer> params, boolean forceInvalidate) {		
		mFirstColumnMonth.setDrawingParams(params);
		mSecondColumnMonth.setDrawingParams(params);
		mThirdColumnMonth.setDrawingParams(params);
		
		mFirstColumnMonth.setMonthInfo(mFristMonthTime, mFirstMonthText, m1stMonthWeekDayNumberList, 1);
		mSecondColumnMonth.setMonthInfo(mSecondMonthTime, mSecondMonthText, m2ndMonthWeekDayNumberList, 2);
		mThirdColumnMonth.setMonthInfo(mThirdMonthTime, mThirdMonthText, m3rdMonthWeekDayNumberList, 3);	
		
		if (forceInvalidate) {
			mFirstColumnMonth.invalidate();
			mSecondColumnMonth.invalidate();
			mThirdColumnMonth.invalidate();
		}
	}
	
	public void calcMonthsTime(int position) {
		mFristMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mSecondMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mThirdMonthTime = GregorianCalendar.getInstance(mTimeZone);
		
		/*
		if) position == 5,
			5 * 3 = 15 months
			15 / 12 = 1 years 
			15 % 12 = 3 months 
			year : 1900 + 1 year
			month : 3 months + 1
			-> 1901/4 ~ 6
		*/		
		int monthsSinceEpoch = position * YearsAdapter.MONTH_NUMBERS_PER_CHILDVIEW;
		int yearsSinceEpoch = monthsSinceEpoch / 12;
		int targetYear = YearsAdapter.EPOCH_YEAR + yearsSinceEpoch;		
		int firstMonth = monthsSinceEpoch % 12;
		
		//mFristMonthTime.set(targetYear, firstMonth, 1);
		mFristMonthTime.clear();
		mFristMonthTime.set(targetYear, firstMonth, 1, 0, 0, 0);
		mFristMonthTime.set(Calendar.MILLISECOND, 0);
		//mFristMonthTime.add(Calendar.MILLISECOND, mTimeZone.getRawOffset());		
				
		mYearIndicatorText = buildYear(mFristMonthTime);
		mFirstMonthText = buildMonth(mFristMonthTime.getTimeInMillis(), mFristMonthTime.getTimeInMillis());//ETime.buildMonth(mFristMonthTime.getTimeInMillis(), mTimeZone);
		m1stMonthWeekDayNumberList = makeDayNumStrings(mFristMonthTime, mTzId, mTimeZone, mWeekStart);		
		
		//mSecondMonthTime.set(targetYear, firstMonth + 1, 1);
		mSecondMonthTime.clear();
		mSecondMonthTime.set(targetYear, firstMonth + 1, 1, 0, 0, 0);
		mSecondMonthTime.set(Calendar.MILLISECOND, 0);
		//mSecondMonthTime.add(Calendar.MILLISECOND, mTimeZone.getRawOffset());	
		
		mSecondMonthText = buildMonth(mSecondMonthTime.getTimeInMillis(), mSecondMonthTime.getTimeInMillis());//ETime.buildMonth(mSecondMonthTime.getTimeInMillis(), mTimeZone);
		m2ndMonthWeekDayNumberList = makeDayNumStrings(mSecondMonthTime, mTzId, mTimeZone, mWeekStart);		
		
		//mThirdMonthTime.set(targetYear, firstMonth + 2, 1);
		mThirdMonthTime.clear();	
		mThirdMonthTime.set(targetYear, firstMonth + 2, 1, 0, 0, 0);
		mThirdMonthTime.set(Calendar.MILLISECOND, 0);
		//mThirdMonthTime.add(Calendar.MILLISECOND, mTimeZone.getRawOffset());
		
		mThirdMonthText = buildMonth(mThirdMonthTime.getTimeInMillis(), mThirdMonthTime.getTimeInMillis());//ETime.buildMonth(mThirdMonthTime.getTimeInMillis(), mTimeZone);
		m3rdMonthWeekDayNumberList = makeDayNumStrings(mThirdMonthTime, mTzId, mTimeZone, mWeekStart);
	}	
	
	/*
	public static ArrayList<String[]> makeDayNumStrings(Calendar monthTime, String tzId, TimeZone timeZone, int weekStart) {
		int targetMonthNumber = monthTime.get(Calendar.MONTH);
		// Figure out what day today is
		Calendar today = GregorianCalendar.getInstance(timeZone);
        today.setTimeInMillis(System.currentTimeMillis());
        //mHasToday = false;
        //mToday = -1;
		
		Calendar TempCalendar = Calendar.getInstance(timeZone);
		TempCalendar.setFirstDayOfWeek(weekStart);  
		TempCalendar.setTimeInMillis(monthTime.getTimeInMillis());		
		int weekNumbersOfMonth = getWeekNumbersOfMonth(TempCalendar, weekStart);
		int monthFirstJulianDay = ETime.getJulianDay(monthTime.getTimeInMillis(), timeZone.getRawOffset() / 1000);
		
		ArrayList<String[]> monthWeekDayNumberList = new ArrayList<String[]>(); 
		
		for (int i=0; i<weekNumbersOfMonth; i++) {
			String[] dayNumbers = new String[7];
			monthWeekDayNumberList.add(dayNumbers);
		}
		
		for (int i=0; i<weekNumbersOfMonth; i++) {
			String[] dayNumbers = monthWeekDayNumberList.get(i);
			int targetWeekJulianDay = monthFirstJulianDay + (i * 7);
				
			int week = ETime.getWeeksSinceEcalendarEpochFromJulianDay(targetWeekJulianDay, weekStart);//Time.getWeeksSinceEpochFromJulianDay(targetWeekJulianDay, weekStart);
			int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(week);//Utils.getJulianMondayFromWeeksSinceEpoch(week);
	        Calendar time = GregorianCalendar.getInstance(timeZone);//new Time(timeZoneString);
	        time.setFirstDayOfWeek(weekStart);
	        long mills = ETime.getMillisFromJulianDay(julianMonday, timeZone);
	        time.setTimeInMillis(mills);//time.setJulianDay(julianMonday);
	        ETime.adjustStartDayInWeek(time);//ETime.switchTimezone(time, timeZone);
	        	               
	        for (int j=0; j < 7; j++) {	        	                        
	        	//if (time.year == today.year && time.yearDay == today.yearDay) {
	        		//mHasToday = true;
	        		//mToday = i;
	        	//}	        	
	        	if (time.get(Calendar.MONTH) == targetMonthNumber) {
	        		dayNumbers[j] = Integer.toString(time.get(Calendar.DAY_OF_MONTH));
	        	}
	        	else
	        		dayNumbers[j] = NOT_MONTHDAY;
	        	
	        	time.add(Calendar.DAY_OF_MONTH, 1);//time.monthDay++;	        	
	        }
		}

        return monthWeekDayNumberList;        
	}	
	*/
	
	public static ArrayList<String[]> makeDayNumStrings(Calendar monthTime, String tzId, TimeZone timeZone, int weekStart) {
		int targetMonthNumber = monthTime.get(Calendar.MONTH);
		// Figure out what day today is
		Calendar today = GregorianCalendar.getInstance(timeZone);
        today.setTimeInMillis(System.currentTimeMillis());
        		
		Calendar TempCalendar = Calendar.getInstance(timeZone);
		TempCalendar.setFirstDayOfWeek(weekStart);  
		TempCalendar.setTimeInMillis(monthTime.getTimeInMillis());		
		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
		//int monthFirstJulianDay = ETime.getJulianDay(monthTime.getTimeInMillis(), timeZone.getRawOffset() / 1000);
		//ETime.adjustStartDayInWeek(monthTime);
		
		ArrayList<String[]> monthWeekDayNumberList = new ArrayList<String[]>(); 
		
		for (int i=0; i<weekNumbersOfMonth; i++) {
			String[] dayNumbers = new String[7];
			monthWeekDayNumberList.add(dayNumbers);
		}
		
		for (int i=0; i<weekNumbersOfMonth; i++) {
			String[] dayNumbers = monthWeekDayNumberList.get(i);
			Calendar timeForWeek = GregorianCalendar.getInstance(timeZone);//new Time(timeZoneString);
			timeForWeek.setTimeInMillis(monthTime.getTimeInMillis());
			timeForWeek.add(Calendar.DAY_OF_MONTH, (i * 7)); //int targetWeekJulianDay = monthFirstJulianDay + (i * 7);			
				
			//int week = ETime.getWeeksSinceEcalendarEpochFromJulianDay(targetWeekJulianDay, weekStart);//Time.getWeeksSinceEpochFromJulianDay(targetWeekJulianDay, weekStart);
			//int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(week);//Utils.getJulianMondayFromWeeksSinceEpoch(week);
	        Calendar time = GregorianCalendar.getInstance(timeZone);//new Time(timeZoneString);
	        time.setFirstDayOfWeek(weekStart);
	        //long mills = ETime.getMillisFromJulianDay(julianMonday, timeZone);
	        time.setTimeInMillis(timeForWeek.getTimeInMillis());//time.setJulianDay(julianMonday);
	        ETime.adjustStartDayInWeek(time);//ETime.switchTimezone(time, timeZone);
	        	               
	        for (int j=0; j < 7; j++) {	        	                        
	        	     	
	        	if (time.get(Calendar.MONTH) == targetMonthNumber) {
	        		dayNumbers[j] = Integer.toString(time.get(Calendar.DAY_OF_MONTH));
	        	}
	        	else
	        		dayNumbers[j] = NOT_MONTHDAY;
	        	
	        	time.add(Calendar.DAY_OF_MONTH, 1);//time.monthDay++;	        	
	        }
		}

        return monthWeekDayNumberList;        
	}	
	
	
	
	
	public int getMonthsPattern() {
		return mMonthsPattern;
	}
	
	public int getPositionNumber() {
		return mPosition;
	}
	
	public Calendar getFirstMonthTime() {
		return mFristMonthTime;
	}
	
	public class ClickedMonthInfo {
		Calendar mSelectedMonthTime;
		int mSelectedMonthColumnNumber;
		
		public ClickedMonthInfo(Calendar selectedMonthTime, int selectedMonthColumnNumber) {
			mSelectedMonthTime = GregorianCalendar.getInstance(selectedMonthTime.getTimeZone());//selectedMonthTime;
			mSelectedMonthTime.setTimeInMillis(selectedMonthTime.getTimeInMillis());
			mSelectedMonthColumnNumber = selectedMonthColumnNumber;
		}
	}
	
	
	int mClickedMonthColumnIndex;
	public int getClickedMonthColumnIndex (int x, int y) {
		mClickedMonthColumnIndex = INVALIDE_COLUMN_MONTH_INDEX;
		
		int monthRegionTop = 0;
		if (mMonthsPattern == YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {
			monthRegionTop = mYearIndicatorHeight;
		}
				
		switch(getClickedMonthColumnNumber(monthRegionTop, x, y)) {		
		case FIRST_COLUMN_MONTH_INDEX:
			mClickedMonthColumnIndex = FIRST_COLUMN_MONTH_INDEX;		
			break;
		case SECOND_COLUMN_MONTH_INDEX:
			mClickedMonthColumnIndex = SECOND_COLUMN_MONTH_INDEX;			
			break;
		case THIRD_COLUMN_MONTH_INDEX:
			mClickedMonthColumnIndex = THIRD_COLUMN_MONTH_INDEX;		
			break;
		default:
			break;
		}
		
		return mClickedMonthColumnIndex;
	}
	
	public int getValidClickedMonthColumnIndex() {
		return mClickedMonthColumnIndex;
	}
	
	public MiniMonthInYearRow getClickedMonthView() {
		switch(mClickedMonthColumnIndex) {		
		case FIRST_COLUMN_MONTH_INDEX:			
			return mFirstColumnMonth;			
			
		case SECOND_COLUMN_MONTH_INDEX:			
			return mSecondColumnMonth;			
			
		case THIRD_COLUMN_MONTH_INDEX:			
			return mThirdColumnMonth;
			
		default:
			return null;
		}
	}
	
	public Calendar getClickedMonthTime() {
		switch(mClickedMonthColumnIndex) {		
		case FIRST_COLUMN_MONTH_INDEX:			
			return mFristMonthTime;			
			
		case SECOND_COLUMN_MONTH_INDEX:			
			return mSecondMonthTime;			
			
		case THIRD_COLUMN_MONTH_INDEX:			
			return mThirdMonthTime;
			
		default:
			return null;
		}
	}
	
	public ArrayList<String[]> getClickedMonthWeekDayNumberList() {
		switch(mClickedMonthColumnIndex) {		
		case FIRST_COLUMN_MONTH_INDEX:			
			return m1stMonthWeekDayNumberList;			
			
		case SECOND_COLUMN_MONTH_INDEX:			
			return m2ndMonthWeekDayNumberList;			
			
		case THIRD_COLUMN_MONTH_INDEX:			
			return m3rdMonthWeekDayNumberList;
			
		default:
			return null;
		}
	}
	
	public ArrayList<String[]> getMonthWeekDayNumberList(int columnNumber) {
		switch(columnNumber) {		
		case FIRST_COLUMN_MONTH_INDEX:			
			return m1stMonthWeekDayNumberList;			
			
		case SECOND_COLUMN_MONTH_INDEX:			
			return m2ndMonthWeekDayNumberList;			
			
		case THIRD_COLUMN_MONTH_INDEX:			
			return m3rdMonthWeekDayNumberList;
			
		default:
			return null;
		}
	}
	
	public void clearClickedMonth() {
		mClickedMonthColumnIndex = INVALIDE_COLUMN_MONTH_INDEX;
	}
	
	public int getClickedMonthColumnNumber(int monthRegionTop, int x, int y) {		
		int monthRegionBottom = mHeight;
		
		int firstMonthLeft = mMiniMonthLeftMargin;
		int firstMonthRight = mMiniMonthLeftMargin + mMiniMonthWidth;
		Rect firstMonthRec = new Rect();
		firstMonthRec.set(firstMonthLeft, monthRegionTop, 
				firstMonthRight, monthRegionBottom);
		
		int secondMonthLeft = firstMonthRight + mMiniMonthLeftMargin;
		int secondMonthRight = secondMonthLeft + mMiniMonthWidth;
		Rect secondMonthRec = new Rect();
		secondMonthRec.set(secondMonthLeft, monthRegionTop, 
				secondMonthRight, monthRegionBottom);
		
		int thirdMonthLeft = secondMonthRight + mMiniMonthLeftMargin;
		int thirdMonthRight = thirdMonthLeft + mMiniMonthWidth;
		Rect thirdMonthRec = new Rect();
		thirdMonthRec.set(thirdMonthLeft, monthRegionTop, 
				thirdMonthRight, monthRegionBottom);
		
		if (firstMonthRec.contains(x, y)) {			
			return FIRST_COLUMN_MONTH_INDEX;
		}
		else if (secondMonthRec.contains(x, y)) {			
			return SECOND_COLUMN_MONTH_INDEX;
		}
		else if (thirdMonthRec.contains(x, y)) {			
			return THIRD_COLUMN_MONTH_INDEX;
		}
		else
			return INVALIDE_COLUMN_MONTH_INDEX;
	}
	
	public Calendar getMonthFromLocation(int x, int y) {		
		
		if (mMonthsPattern == YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {
			Rect yearIndicatorRec = new Rect();
			yearIndicatorRec.set(0, 0, 
					mWidth, mYearIndicatorHeight);
			
			int monthRegionTop = mYearIndicatorHeight;
			int monthRegionBottom = mHeight;
			
			int firstMonthLeft = mMiniMonthLeftMargin;
			int firstMonthRight = mMiniMonthLeftMargin + mMiniMonthWidth;
			Rect firstMonthRec = new Rect();
			firstMonthRec.set(firstMonthLeft, monthRegionTop, 
					firstMonthRight, monthRegionBottom);
			
			int secondMonthLeft = firstMonthRight + mMiniMonthLeftMargin;
			int secondMonthRight = secondMonthLeft + mMiniMonthWidth;
			Rect secondMonthRec = new Rect();
			secondMonthRec.set(secondMonthLeft, monthRegionTop, 
					secondMonthRight, monthRegionBottom);
			
			int thirdMonthLeft = secondMonthRight + mMiniMonthLeftMargin;
			int thirdMonthRight = thirdMonthLeft + mMiniMonthWidth;
			Rect thirdMonthRec = new Rect();
			thirdMonthRec.set(thirdMonthLeft, monthRegionTop, 
					thirdMonthRight, monthRegionBottom);
			
			if (yearIndicatorRec.contains(x, y)) {
				return null;
			}
			else if (firstMonthRec.contains(x, y)) {
				mFirstColumnMonth.setVisibility(View.INVISIBLE);
				return mFristMonthTime;
			}
			else if (secondMonthRec.contains(x, y)) {
				mSecondColumnMonth.setVisibility(View.INVISIBLE);
				return mSecondMonthTime;
			}
			else if (thirdMonthRec.contains(x, y)) {
				mThirdColumnMonth.setVisibility(View.INVISIBLE);
				return mThirdMonthTime;
			}
			
		}
		else if (mMonthsPattern == NORMAL_YEAR_MONTHS_PATTERN) {
			int monthRegionTop = 0;
			int monthRegionBottom = mHeight;
			
			int firstMonthLeft = mMiniMonthLeftMargin;
			int firstMonthRight = mMiniMonthLeftMargin + mMiniMonthWidth;
			Rect firstMonthRec = new Rect();
			firstMonthRec.set(firstMonthLeft, monthRegionTop, 
					firstMonthRight, monthRegionBottom);
			
			int secondMonthLeft = firstMonthRight + mMiniMonthLeftMargin;
			int secondMonthRight = secondMonthLeft + mMiniMonthWidth;
			Rect secondMonthRec = new Rect();
			secondMonthRec.set(secondMonthLeft, monthRegionTop, 
					secondMonthRight, monthRegionBottom);
			
			int thirdMonthLeft = secondMonthRight + mMiniMonthLeftMargin;
			int thirdMonthRight = thirdMonthLeft + mMiniMonthWidth;
			Rect thirdMonthRec = new Rect();
			thirdMonthRec.set(thirdMonthLeft, monthRegionTop, 
					thirdMonthRight, monthRegionBottom);
			
			if (firstMonthRec.contains(x, y)) {
				mFirstColumnMonth.setVisibility(View.INVISIBLE);
				return mFristMonthTime;
			}
			else if (secondMonthRec.contains(x, y)) {
				mSecondColumnMonth.setVisibility(View.INVISIBLE);
				return mSecondMonthTime;
			}
			else if (thirdMonthRec.contains(x, y)) {
				mThirdColumnMonth.setVisibility(View.INVISIBLE);
				return mThirdMonthTime;
			}			
		}
		
		return null;		
	}	
	
	
	private String buildMonth(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			mContext,
                mFormatter,
                startMillis,
                endMillis,
                ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_MONTH_DAY
                        | ETime.FORMAT_NO_YEAR, mTzId).toString();
    	
    	return date;    	
    }
	
	
	// ���� 2014�� �� �ȵǳ�?
	private String buildYear(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			mContext,
                mFormatter,
                startMillis,
                endMillis,
                /*DateUtils.FORMAT_SHOW_DATE |*/ ETime.FORMAT_NO_MONTH_DAY | ETime.FORMAT_SHOW_YEAR,
                mTzId).toString();
    	
    	return date;    	
    }
	
	// �� �̶� �ؽ�Ʈ�� ��µ��� �ʴ´�
	private String buildYear(Calendar yearTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
		Date yyyy = new Date();		
		yyyy.setTime(yearTime.getTimeInMillis());
		String year = sdf.format(yyyy);
		return year;
	}
	
}
