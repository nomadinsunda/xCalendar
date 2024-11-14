package com.intheeast.year;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;





import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.SelectedDateOvalDrawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.OvalShape;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ScaleMiniMonthView extends View {

	public static final String PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE = "psudo_minimonth_alpha_value";
	
	Context mContext;
	Paint mMonthNumPaint;	
	Paint mNextMonthNumPaint;
	Paint mMiniMonthMonthDayPaintOfYearView;
	Paint mMonthViewMonthDayPaint;
	Paint mUpperLinePaint;
	Paint mEventExisteneCirclePaint;
	
	float mUpperLineStrokeWidth;	
	
	boolean mInitialized = false;
	boolean mSetDrawingParams = false;
	
	int mWidth;
	int mHeight;
	
	int mMonthViewMode;
	int mMonthListViewWidth;
	int mMonthListViewHeight;
	
	int mMonthWeekViewNormalWeekHeightPerMode;
	
	int mOriginalMiniMonthWidth;
	int mMiniMonthWidth;
	int mMiniMonthHeight;
	
	int mMiniMonthLeftMargin;	
	int mMiniMonthIndicatorTextHeight;
	int mMiniMonthIndicatorTextBaseLineY;
	int mMiniMonthDayTextHeight;
	int mMiniMonthDayTextBaseLineY;
	int mMiniMonthIndicatorHeight;
	int mMiniMonthNormalWeekHeight;
	int mEventCircleTopPadding;
	
	int mFirstDayOfWeek;
	
	String mMonthText;
	
	float mMonthTextX = 0;	
	int mMonthDayTextLeftOffset;
	
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
    int mPsudoMiniMonthAlphaValue = 0;
    
    Calendar mGoToMonthTime;
	int mPrvMonthLastWeekDaysNumOfGoToMonth;
	float mGoToMonthTextFinalX;
	Calendar mPrvToMonthTime;
	int mPrvMonthLastWeekDaysNumOfPrvMonth;
	Calendar mNextToMonthTime;
	int mPrvMonthLastWeekDaysNumOfNextMonth;
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	String mGoToMonthText;
	String mPrvMonthText;
	String mNextMonthText;
	float mGoToMonthTextWidth;
	ArrayList<String[]> mGoToMonthWeekDayNumberList = null;
	ArrayList<String[]> mGoToPrvMonthWeekDayNumberList = null;
	ArrayList<String[]> mGoToNextMonthWeekDayNumberList = null;
	
	int mFirstJulianDay;
	int mNumDays;
	boolean mHasToday = false;
	int mDayOfMonthOfToday = 0;
	
	float mScaleWidthSize;
	
	public ScaleMiniMonthView(Context context) {
		super(context);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
	}
	
	public ScaleMiniMonthView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
	}
	
	public void initView(Context context) {
		if (!mInitialized) {
			mContext = context;
			mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
			
			mMonthNumPaint = new Paint();
	        mMonthNumPaint.setFakeBoldText(false);
	        mMonthNumPaint.setAntiAlias(true);        
	        mMonthNumPaint.setColor(Color.RED);             
	        mMonthNumPaint.setTextAlign(Align.LEFT);
	        mMonthNumPaint.setTypeface(Typeface.MONOSPACE);	  
	        	        
	        	        
	        mNextMonthNumPaint = new Paint();
	        mNextMonthNumPaint.setFakeBoldText(false);
	        mNextMonthNumPaint.setAntiAlias(true);	        
	        mNextMonthNumPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mNextMonthNumPaint.setStyle(Style.FILL);        
	        mNextMonthNumPaint.setTextAlign(Align.CENTER);
	        mNextMonthNumPaint.setTypeface(Typeface.DEFAULT);  
	        
	        mMiniMonthMonthDayPaintOfYearView = new Paint();
	        mMiniMonthMonthDayPaintOfYearView.setFakeBoldText(true);
	        mMiniMonthMonthDayPaintOfYearView.setAntiAlias(true);        
	        mMiniMonthMonthDayPaintOfYearView.setColor(Color.BLACK);            
	        mMiniMonthMonthDayPaintOfYearView.setTextAlign(Align.CENTER);
	        mMiniMonthMonthDayPaintOfYearView.setTypeface(Typeface.MONOSPACE);	 
	        
	        mMonthViewMonthDayPaint = new Paint();
	        mMonthViewMonthDayPaint.setFakeBoldText(false);
	        mMonthViewMonthDayPaint.setAntiAlias(true);	        
	        mMonthViewMonthDayPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mMonthViewMonthDayPaint.setStyle(Style.FILL);        
	        mMonthViewMonthDayPaint.setTextAlign(Align.CENTER);
	        mMonthViewMonthDayPaint.setTypeface(Typeface.DEFAULT);  
	        
	        mUpperLinePaint = new Paint();        
	        mUpperLinePaint.setAntiAlias(true);        
	        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUpperLinePaint.setStyle(Style.STROKE);
	        mUpperLineStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
	        //mUpperLinePaint.setStrokeWidth(mUpperLineStrokeWidth);
	        
	        mEventExisteneCirclePaint = new Paint();    
	        mEventExisteneCirclePaint.setAntiAlias(true);        
	        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
	        mEventExisteneCirclePaint.setStyle(Style.FILL);   
	        
	        mInitialized = true;
		}
	}
	
	public void setExpandEventListModeMonthInfo(int MonthListViewWidth, float MonthListMonthNumberTextSize, Calendar goToMonthTime, ArrayList<String[]> goToMonthWeekDayNumberList) {	
		// computeTextXPositionOfMonthListView���� mMonthListViewWidth�� ����Ѵ�
		// :�׷��� mMonthListViewWidth�� ���� �����Ǿ� ���� �ʴ�...
		mMonthListViewWidth = MonthListViewWidth;		
		
		mTimeZone = goToMonthTime.getTimeZone();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mTzId = mTimeZone.getID();
		
		mGoToMonthTime = GregorianCalendar.getInstance(mTimeZone);//goToMonthTime;
		mGoToMonthTime.setTimeInMillis(goToMonthTime.getTimeInMillis());
		mFirstJulianDay = ETime.getJulianDay(mGoToMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		mNumDays = mGoToMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);		
		
		Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
		TodayCal.setTimeInMillis(System.currentTimeMillis());
		if (mGoToMonthTime.get(Calendar.YEAR) == TodayCal.get(Calendar.YEAR) &&
				mGoToMonthTime.get(Calendar.MONTH) == TodayCal.get(Calendar.MONTH)) {
			mHasToday = true;
			mDayOfMonthOfToday = TodayCal.get(Calendar.DAY_OF_MONTH);			
			initDayOfMonthCircleDrawable(Color.RED);
		}
		else {
			initDayOfMonthCircleDrawable(Color.BLACK);
		}
				
		mGoToMonthWeekDayNumberList = goToMonthWeekDayNumberList;
				
		mGoToMonthText = buildMonth(mGoToMonthTime.getTimeInMillis(), mGoToMonthTime.getTimeInMillis());//ETime.buildMonth(mGoToMonthTime.getTimeInMillis(), mTimeZone);//buildMonth(mGoToMonthTime.getTimeInMillis(), mGoToMonthTime.getTimeInMillis());
		// mMonthNumPaint.setTextSize(textSize)�� ���� �����Ǿ� ���� �ʴ�
		// �׷���...textSize�� Month ListView�� ���� Month Number�� size�� ������ ��, �����ؾ� ���� �ʴ°�?
		Paint paint = new Paint();
		paint.setFakeBoldText(false);
		paint.setAntiAlias(true);  		   
		paint.setTextSize(MonthListMonthNumberTextSize);
		paint.setTypeface(Typeface.DEFAULT);        
		mGoToMonthTextWidth = paint.measureText(mGoToMonthText);
		
		mPrvMonthLastWeekDaysNumOfGoToMonth = 0;
		String[] dayNumbersOfGoToMonth = mGoToMonthWeekDayNumberList.get(0);
		for (int j=0; j<7; j++) {
			if (dayNumbersOfGoToMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
				mPrvMonthLastWeekDaysNumOfGoToMonth++;
		}		
		mGoToMonthTextFinalX = computeTextXPositionOfMonthListView(mPrvMonthLastWeekDaysNumOfGoToMonth);	
				
		mMonthTextX = 0;
		
		mMonthDayTextLeftOffset = (int)mMonthTextX;
	}	
	
	public void setNormalModeMonthInfo(int MonthListViewWidth, float MonthListMonthNumberTextSize, Calendar goToMonthTime, ArrayList<String[]> goToMonthWeekDayNumberList,
			ArrayList<String[]> goToPrvMonthWeekDayNumberList, ArrayList<String[]> goToNextMonthWeekDayNumberList) {		
		// computeTextXPositionOfMonthListView���� mMonthListViewWidth�� ����Ѵ�
		// :�׷��� mMonthListViewWidth�� ���� �����Ǿ� ���� �ʴ�...
		mMonthListViewWidth = MonthListViewWidth;
		
		mTimeZone = goToMonthTime.getTimeZone();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mTzId = mTimeZone.getID();
		
		mGoToMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mGoToMonthTime.setTimeInMillis(goToMonthTime.getTimeInMillis());//goToMonthTime;
		mFirstJulianDay = ETime.getJulianDay(mGoToMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		mNumDays = mGoToMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);
				
		Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
		TodayCal.setTimeInMillis(System.currentTimeMillis());
		if (mGoToMonthTime.get(Calendar.YEAR) == TodayCal.get(Calendar.YEAR) &&
				mGoToMonthTime.get(Calendar.MONTH) == TodayCal.get(Calendar.MONTH)) {
			mHasToday = true;
			mDayOfMonthOfToday = TodayCal.get(Calendar.DAY_OF_MONTH);
			initDayOfMonthCircleDrawable(Color.RED);			
		}
		
		mGoToMonthWeekDayNumberList = goToMonthWeekDayNumberList;
		mGoToPrvMonthWeekDayNumberList = goToPrvMonthWeekDayNumberList;
		mGoToNextMonthWeekDayNumberList = goToNextMonthWeekDayNumberList;
		
		mGoToMonthText = buildMonth(mGoToMonthTime.getTimeInMillis(), mGoToMonthTime.getTimeInMillis());//ETime.buildMonth(mGoToMonthTime.getTimeInMillis(), mTimeZone);
		Paint paint = new Paint();
		paint.setFakeBoldText(false);
		paint.setAntiAlias(true);  		   
		paint.setTextSize(MonthListMonthNumberTextSize);
		paint.setTypeface(Typeface.DEFAULT);        
		mGoToMonthTextWidth = paint.measureText(mGoToMonthText);
		
		mPrvMonthLastWeekDaysNumOfGoToMonth = 0;
		String[] dayNumbersOfGoToMonth = mGoToMonthWeekDayNumberList.get(0);
		for (int j=0; j<7; j++) {
			if (dayNumbersOfGoToMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
				mPrvMonthLastWeekDaysNumOfGoToMonth++;
		}		
		mGoToMonthTextFinalX = computeTextXPositionOfMonthListView(mPrvMonthLastWeekDaysNumOfGoToMonth);		
		
		mPrvToMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(this.mTimeZoneString);
		mPrvToMonthTime.setTimeInMillis(mGoToMonthTime.getTimeInMillis());
		mPrvToMonthTime.add(Calendar.MONTH, -1);//mPrvToMonthTime.month = mPrvToMonthTime.month - 1;
		
		mPrvMonthText = buildMonth(mPrvToMonthTime.getTimeInMillis(), mPrvToMonthTime.getTimeInMillis());//ETime.buildMonth(mPrvToMonthTime.getTimeInMillis(), mTimeZone);
		mPrvMonthLastWeekDaysNumOfNextMonth = 0;
		String[] dayNumbersOfPrvMonth = mGoToPrvMonthWeekDayNumberList.get(0);
		for (int j=0; j<7; j++) {
			if (dayNumbersOfPrvMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
				mPrvMonthLastWeekDaysNumOfPrvMonth++;
		}		
		
		mNextToMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mNextToMonthTime.setTimeInMillis(mGoToMonthTime.getTimeInMillis());
		mNextToMonthTime.add(Calendar.MONTH, 1);//month = mNextToMonthTime.month + 1;
		
		mNextMonthText = buildMonth(mNextToMonthTime.getTimeInMillis(), mNextToMonthTime.getTimeInMillis());//ETime.buildMonth(mNextToMonthTime.getTimeInMillis(), mTimeZone);		
		mPrvMonthLastWeekDaysNumOfNextMonth = 0;
		String[] dayNumbersOfNextMonth = mGoToNextMonthWeekDayNumberList.get(0);
		for (int j=0; j<7; j++) {
			if (dayNumbersOfNextMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
				mPrvMonthLastWeekDaysNumOfNextMonth++;
		}		
		
		mMonthTextX = 0;
		
		mMonthDayTextLeftOffset = (int)mMonthTextX;
	}	
		
	
	public void setMiniMonthDrawingParams(HashMap<String, Integer> params) {
		if (!mSetDrawingParams) {
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE)) {
				mMonthViewMode = params.get(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE);						
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH)) {
				mMonthListViewWidth = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH);
	        }			
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT)) {
				mMonthListViewHeight = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT);
	        }	
									
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
				mOriginalMiniMonthWidth = mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
				mScaleWidthSize = mMonthListViewWidth - mOriginalMiniMonthWidth;
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
	        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
	        }			
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
	        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
	        } 
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
	        	mMiniMonthIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
	        	mMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight); 	        	
	        	mNextMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight);
	        }
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
	        	mMiniMonthIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
	        }
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
	        	mMiniMonthDayTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
	        }
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
	        	mMiniMonthDayTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);  
	        	mMiniMonthMonthDayPaintOfYearView.setTextSize(mMiniMonthDayTextHeight); 
	        }        
        	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT)) {
	        	mMonthWeekViewNormalWeekHeightPerMode = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT); 	        	
	        }    
	        	        
	        mSetDrawingParams = true;
		}
	}
	
	
	public void setMiniMonthScaleDrawingParams(HashMap<String, Integer> params) {
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
        }			
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        } 
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
        	mMiniMonthIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
        	mMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight); 
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
        	mMiniMonthIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
        	mMiniMonthDayTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
        	mMiniMonthDayTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);  
        	mMiniMonthMonthDayPaintOfYearView.setTextSize(mMiniMonthDayTextHeight); 
        }		
	}
	
	public void setMonthViewScaleDrawingParams(HashMap<String, Integer> params) {
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
        }			
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        } 
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
        	mMiniMonthIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
        	mMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
        	mMiniMonthIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT)) {
        	mMiniMonthIndicatorHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT);        	
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT)) {
        	mMiniMonthNormalWeekHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
        	mMiniMonthDayTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_EVENT_CIRCLE_TOP_PADDING)) {
        	mEventCircleTopPadding = params.get(YearsAdapter.MONTHS_PARAMS_EVENT_CIRCLE_TOP_PADDING);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
        	mMiniMonthDayTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);         	
        	mMonthViewMonthDayPaint.setTextSize(mMiniMonthDayTextHeight);        	
        	mNextMonthNumPaint.setTextSize(mMiniMonthDayTextHeight);
        }
        
        if (params.containsKey(PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE)) {
        	mPsudoMiniMonthAlphaValue = params.get(PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE);         	
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHINDICATOR_TEXT_COLOR)) {
        	mMonthNumPaint.setColor(params.get(YearsAdapter.MONTHS_PARAMS_MONTHINDICATOR_TEXT_COLOR));
        }
        
        if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {   
        	setOvalDrawableOfDayOfMonthDimens();        			
		}        
        else {    		
        	if (mHasToday)
        		setOvalDrawableOfDayOfMonthDimens();				
		}
        
        mMonthNumPaint.setAlpha(mPsudoMiniMonthAlphaValue);
        mMonthViewMonthDayPaint.setAlpha(mPsudoMiniMonthAlphaValue);
        mEventExisteneCirclePaint.setAlpha(mPsudoMiniMonthAlphaValue);
        mUpperLinePaint.setAlpha(mPsudoMiniMonthAlphaValue);        
	}
	
	boolean mLaunchScaleDrawingMonthListView = false;
	public void launchScaleDrawingMonthListView() {
		mLaunchScaleDrawingMonthListView = true;
		
		mMonthNumPaint.setFakeBoldText(false);
		
		mMonthNumPaint.setStyle(Style.FILL);  
		mMonthNumPaint.setTypeface(Typeface.DEFAULT); 
		
		mNextMonthNumPaint.setTextSize(mMonthNumPaint.getTextSize());
		
		mMonthViewMonthDayPaint.setTextSize(mMiniMonthMonthDayPaintOfYearView.getTextSize());		
	}
	
	public static final int PSUDO_MONTHLISTVIEW_NORMAL_MODE = 1;
	public static final int PSUDO_MONTHLISTVIEW_EXPAND_EVENT_LIST_MODE = 2;
	public int mMonthListViewMode = PSUDO_MONTHLISTVIEW_NORMAL_MODE;
	
	public void setMonthListViewMode(int mode) {
		mMonthListViewMode = mode;
	}
	
	public int getFirstJulianDay() {
		return mFirstJulianDay;
	}
	
	
	public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        
        createEventExistenceCircle(unsortedEvents);
    }  
    
    List<ArrayList<Event>> mEvents = null;
    public void setEvents(List<ArrayList<Event>> sortedEvents) {    	
        mEvents = sortedEvents;
        if (sortedEvents == null) {
            return;
        }
        if (sortedEvents.size() != mNumDays) {
            if (Log.isLoggable("tag", Log.ERROR)) {
                Log.wtf("tag", "Events size must be same as days displayed: size="
                        + sortedEvents.size() + " days=" + mNumDays);
            }
            mEvents = null;
            return;
        }
    }
    
	boolean mEventExistence[] = null;
    ArrayList<Event> mUnsortedEvents = null;
    public void createEventExistenceCircle(ArrayList<Event> unsortedEvents) {
    	if (unsortedEvents == null) {
            // Stash the list of events for use when this view is ready, or
            // just clear it if a null set has been passed to this view
            mUnsortedEvents = unsortedEvents;            
            mEventExistence = null;
            return;
        } else {
            // clear the cached set of events since we're ready to build it now
            mUnsortedEvents = null;
        }       
        
    	int numDays = mEvents.size();
    	mEventExistence = new boolean[numDays];
    	for (int i=0; i<numDays; i++) {
    		ArrayList<Event> dayEvents = mEvents.get(i);
    		if (dayEvents.size() !=0)
    			mEventExistence[i] = true;
    		else
    			mEventExistence[i] = false;
    	}
    }
	

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		//Log.i("tag", "onSizeChanged:mWidth=" + String.valueOf(mWidth) + ", mHeight=" + String.valueOf(mHeight));
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if (!mLaunchScaleDrawingMonthListView) {
			drawingScalingMiniMonthView(canvas);
		}
		else {
			drawingScalingMonthListView(canvas);
		}		
	}	
	
	
	public void drawingScalingMonthListView(Canvas canvas) {
		int effectiveWidth = mMiniMonthWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		
		float originalNormalWeekHeightPerMode = mMonthWeekViewNormalWeekHeightPerMode;
		float currentScaleNormalWeekHeight = mMiniMonthNormalWeekHeight;
		float normalWeekAspectRatio = currentScaleNormalWeekHeight / originalNormalWeekHeightPerMode;
		
		float radius = 0;
		if (mEventExistence != null) {   
			radius = (currentScaleNormalWeekHeight * 0.1f) / 2;				
		}
		
		float currentScaleMonthWidth = mMiniMonthWidth - mOriginalMiniMonthWidth;		
		float monthScaleAspectRatio = currentScaleMonthWidth / mScaleWidthSize;
		mMonthTextX = mGoToMonthTextFinalX * monthScaleAspectRatio;
			
		canvas.drawText(mGoToMonthText, mMonthTextX, mMiniMonthIndicatorTextBaseLineY, mMonthNumPaint);
				
		int monthDayTextTopOffset = mMiniMonthIndicatorHeight;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = mGoToMonthWeekDayNumberList.size();	
		
		float weekLineY = 0;
		int targetDayCount = 0;
		
		for (int i=0; i<weekNumbers; i++) {			
			
			String[] dayNumbers = mGoToMonthWeekDayNumberList.get(i);		
			monthDayTextY = monthDayTextTopOffset + (i * mMiniMonthNormalWeekHeight) + mMiniMonthDayTextBaseLineY;
			
			int upperLineWidth = 0;		
			for (int j=0; j<7; j++) {
				monthDayTextX = computeTextXPosition(j);				
								
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {		
								        
					int color;
		        	final int column = j % 7;
		            if (Utils.isSaturday(column, mFirstDayOfWeek)) {
		                color = getResources().getColor(R.color.week_saturday);
		            } else if (Utils.isSunday(column, mFirstDayOfWeek)) {
		                color = getResources().getColor(R.color.week_sunday);
		            }
		            else {
		            	color = getResources().getColor(R.color.month_day_number);
		            }		            
		            
		            mMonthViewMonthDayPaint.setColor(color);
		            mMonthViewMonthDayPaint.setAlpha(mPsudoMiniMonthAlphaValue);
		            
					canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthViewMonthDayPaint);
					
					if (mEventExistence != null) {						
						if (mEventExistence[targetDayCount]) {/////////////////////////////////////////////////////////�̰� ������...
							float cx = monthDayTextX;
							float cy = mMiniMonthIndicatorHeight + (i * mMiniMonthNormalWeekHeight) + mEventCircleTopPadding;//float cy = monthDayTextTopOffset + (i * mMiniMonthNormalWeekHeight) + mEventCircleTopPadding;							
							canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
						}
					}
					
					upperLineWidth = upperLineWidth + dayOfWeekTextWidth;
					targetDayCount++;				
				}
			}
			
			if (i==0) {				
				float lineWidth = mUpperLineStrokeWidth * normalWeekAspectRatio;				
				mUpperLinePaint.setStrokeWidth(lineWidth);				
				weekLineY = mMiniMonthIndicatorHeight;		
				canvas.drawLine(dayOfWeekTextWidth * mPrvMonthLastWeekDaysNumOfGoToMonth, weekLineY, mMiniMonthWidth, weekLineY, mUpperLinePaint);			
			}
			else {
				weekLineY = mMiniMonthIndicatorHeight + (i * mMiniMonthNormalWeekHeight) + mUpperLinePaint.getStrokeWidth();
				canvas.drawLine(0, weekLineY, upperLineWidth, weekLineY, mUpperLinePaint);		
			}
		}	
		
		
		
		if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {	
			
			if (mHasToday) {
				Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
				TodayCal.setTimeInMillis(System.currentTimeMillis());
				
				int monthweekNumbers = ETime.getWeekNumbersOfMonth(TodayCal);
				
				int firstDayOfMonthOfSecondWeek = (7 - mPrvMonthLastWeekDaysNumOfGoToMonth) + 1;
				if (mDayOfMonthOfToday < firstDayOfMonthOfSecondWeek) {
					makeSelectedCircleDrawable(canvas, mDayOfMonthOfToday, mPrvMonthLastWeekDaysNumOfGoToMonth + mDayOfMonthOfToday, mMiniMonthIndicatorHeight);						
				}
				else {
					int firstDayInWeek = firstDayOfMonthOfSecondWeek;
					for (int i=1; i<monthweekNumbers; i++) {
						int lastDayInWeek = firstDayInWeek + 6;
						if (mDayOfMonthOfToday < lastDayInWeek) {
							int todayIndex = mDayOfMonthOfToday - firstDayInWeek;
							int offsetY = mMiniMonthIndicatorHeight + (i * mMiniMonthNormalWeekHeight);
							makeSelectedCircleDrawable(canvas, mDayOfMonthOfToday, todayIndex, offsetY);
							break;
						}
						else {
							firstDayInWeek = lastDayInWeek + 1;
						}
					}
				}				
			}
			else {
				makeSelectedCircleDrawable(canvas, 1, mPrvMonthLastWeekDaysNumOfGoToMonth, mMiniMonthIndicatorHeight);
			}
		}
		else {
			if (mHasToday) {
				Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
				TodayCal.setTimeInMillis(System.currentTimeMillis());
								
				int monthweekNumbers = ETime.getWeekNumbersOfMonth(TodayCal);
				
				int firstDayOfMonthOfSecondWeek = (7 - mPrvMonthLastWeekDaysNumOfGoToMonth) + 1;
				if (mDayOfMonthOfToday < firstDayOfMonthOfSecondWeek) {
					makeSelectedCircleDrawable(canvas, mDayOfMonthOfToday, mPrvMonthLastWeekDaysNumOfGoToMonth + mDayOfMonthOfToday, mMiniMonthIndicatorHeight);						
				}
				else {
					int firstDayInWeek = firstDayOfMonthOfSecondWeek;
					for (int i=1; i<monthweekNumbers; i++) {
						int lastDayInWeek = firstDayInWeek + 6;
						if (mDayOfMonthOfToday < lastDayInWeek) {
							int todayIndex = mDayOfMonthOfToday - firstDayInWeek;
							int offsetY = mMiniMonthIndicatorHeight + (i * mMiniMonthNormalWeekHeight);
							makeSelectedCircleDrawable(canvas, mDayOfMonthOfToday, todayIndex, offsetY);
							break;
						}
						else {
							firstDayInWeek = lastDayInWeek + 1;
						}
					}
				}					
			}
		}
		
	}

	public void drawingScalingMiniMonthView(Canvas canvas) {
		// ��� month text�� �̵� ��ų ���ΰ�?
		//
		// miniWidth�� ���ΰ�????
		float currentScaleMonthWidth = mMiniMonthWidth - mOriginalMiniMonthWidth;		
		float monthScaleAspectRatio = currentScaleMonthWidth / mScaleWidthSize;
		mMonthTextX = mGoToMonthTextFinalX * monthScaleAspectRatio;
		canvas.drawText(mGoToMonthText, mMonthTextX, mMiniMonthIndicatorTextBaseLineY, mMonthNumPaint);
				
		int monthDayTextTopOffset = mMiniMonthIndicatorTextBaseLineY;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = mGoToMonthWeekDayNumberList.size();
		for (int i=0; i<weekNumbers; i++) {
			String[] dayNumbers = mGoToMonthWeekDayNumberList.get(i);		
			monthDayTextY = monthDayTextTopOffset + ((i + 1) * mMiniMonthDayTextBaseLineY);
			
			for (int j=0; j<7; j++) {
				monthDayTextX = computeTextXPosition(j);				
								
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {
					canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMiniMonthMonthDayPaintOfYearView);
				}
			}
		}		
	}

	SelectedDateOvalDrawable mOvalDrawableOfDayOfMonth;
    int mOvalDrawableOfDayOfMonthRadius;	
	int mOvalDrawableOfDayOfMonthSize;
	
	public void initDayOfMonthCircleDrawable(int color) {
		mOvalDrawableOfDayOfMonth = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthViewMonthDayPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
		mOvalDrawableOfDayOfMonth.getPaint().setAntiAlias(true);
		mOvalDrawableOfDayOfMonth.getPaint().setStyle(Style.FILL);
			
		mOvalDrawableOfDayOfMonth.getPaint().setColor(color);		
	}
	
	public void setOvalDrawableOfDayOfMonthDimens() {
		mOvalDrawableOfDayOfMonth.setTextSize(mMonthViewMonthDayPaint.getTextSize());
		
		float radius = (mMiniMonthNormalWeekHeight / 2) / 2;
		mOvalDrawableOfDayOfMonthRadius = (int)radius;
		mOvalDrawableOfDayOfMonthSize = (int)(mOvalDrawableOfDayOfMonthRadius * 2);		
	}
	
	public void makeSelectedCircleDrawable (Canvas canvas, int dateNum, int xPos, int offsetY) {
		float baseLineY = mMiniMonthDayTextBaseLineY;
    	
    	float textAscent = mMonthViewMonthDayPaint.ascent();
    	float textDescent = mMonthViewMonthDayPaint.descent();
    	
    	float textTopY = baseLineY + textAscent;//float textTopY = Math.abs(baseLineY + textAscent); 	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    			
		int x = computeTextXPosition(xPos);
		int left = x - mOvalDrawableOfDayOfMonthRadius;	    			
		int top = (int) (textCenterY - mOvalDrawableOfDayOfMonthRadius);
		int right = left + mOvalDrawableOfDayOfMonthSize;
		int bottom = top + mOvalDrawableOfDayOfMonthSize;
		
		mOvalDrawableOfDayOfMonth.setTextAlpha(mPsudoMiniMonthAlphaValue);
		
		mOvalDrawableOfDayOfMonth.getPaint().setAlpha(mPsudoMiniMonthAlphaValue);
		mOvalDrawableOfDayOfMonth.setBounds(left, top, right, bottom);			       
		mOvalDrawableOfDayOfMonth.setDrawTextPosition(x, offsetY + mMiniMonthDayTextBaseLineY);
		mOvalDrawableOfDayOfMonth.setDayOfMonth(dateNum);
		mOvalDrawableOfDayOfMonth.draw(canvas);   		
	}
	
    
    
	private int computeTextXPosition(int day) {
		int x = 0;
		int effectiveWidth = mMiniMonthWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
	    return x;
	}
	
	private int computeTextXPositionOfMonthListView(int day) {
		int x = 0;
		int effectiveWidth = mMonthListViewWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
		int halfWidth = (int) (mGoToMonthTextWidth / 2);
		x = x - halfWidth;
	    return x;
	}
	
	
	private String buildMonth(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			mContext,
                mFormatter,
                startMillis,
                endMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_NO_YEAR, mTzId).toString();
    	
    	return date;    	
    }
	
}
