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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.view.View;

public class YearMonthsView extends View {

	/*
	 * Year Indicator height : 0.09% of mAnticipationListViewHeight
	 * Year Indicator Text Height : 0.6% of Year Indicator height
	 * Year Indicator Text Baseline : 0.85% of Year Indicator height

	 * ���� month region�� height�� Month View�� �Ʒ� height�� 4��� ��ҵ� ũ���̴�  
	   Month Indicator
	   +
	   Normal Week
	   +
	   Normal Week
	   +
	   Normal Week
	   +
	   Normal Week
	   +
	   Normal Week
	   +
	   Month Day Text�� Baseline Offset[Normal Week�� 0.4%]
	 
	 *Month indicator �� Month Number�� baseline : 1.48 -> 1.11   ----------> 0.75%�� ��� ��Ŵ
	  -> base line�� Month indicator�� lower line���� Pad�� ������Ŵ

	 *Normal Week�� MonthDay baseline�� 1.50 -> 0.75 -----------------------> 0.5%�� ��� ��Ŵ
	  -> baseline �Ʒ��� �׿� ������ ������Ŵ

	 *MonthDay Text�� ũ�⸦  0.74 -> 0.34 --------------------------------------> 0.45%�� ��� ��Ŵ

	 *left margin 0.04 of WIDTH �׸��� �� month region�� GAP ���� 0.04
	 
	 *year month width about 0.28 of WIDTH
	
	 *month number height�� Month View�� month number�� 0.875% �� ���̴�
	 
	 *month day height�� Month View�� month day height�� 0.45 �� ���̴�
	*/
	public static final String VIEW_PARAMS_YEAR_MONTHS_PATTERN = "year_months_pattern";
	
	public static final String VIEW_PARAMS_HEIGHT = "view_height";
	
	public static final String VIEW_PARAMS_POSITION = "view_position";
	
	public static final String VIEW_PARAMS_WEEK_START = "week_start";
	
	public static final String VIEW_PARAMS_TARGETMONTH_JULIANDAY = "target_month_julianday";
	
	public static final int NORMAL_YEAR_MONTHS_PATTERN = 0;
	
	public static final int YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN = 1;

    //private static int TEXT_SIZE_MONTH_NUMBER = 32;
    
    public static final String NOT_MONTHDAY = "XXX";

	Context mContext;
	Paint mYearIndicatorPaint;
	Paint mMonthNumPaint;
	Paint mMonthDayPaint;
	Paint mUnderLinePaint;
	
	
	String mTzId = ETime.getCurrentTimezone();
	TimeZone mTimeZone = TimeZone.getTimeZone(mTzId);
	long mGmtoff = mTimeZone.getRawOffset() / 1000;
	Calendar mFristMonthTime;
	Calendar mSecondMonthTime;
	Calendar mThirdMonthTime;
	
	int mFristMonthJulianDay;
	
	int mWidth;
	int mHeight;
	
	int mYearIndicatorHeight;
	int mYearIndicatorTextHeight;
	int mYearIndicatorTextBaseLineY;
	int mMiniMonthHeight;
	int mMiniMonthIndicatorTextHeight;
	int mMiniMonthIndicatorTextBaseLineY;	
	int mMiniMonthDayTextBaseLineY;
	int mMiniMonthDayTextHeight;
	int mMiniMonthWidth;
	int mMiniMonthLeftMargin;
	
	int mPosition;
	int mFirstDayOfWeek;
	
	boolean mInitialized = false;
	float mScale = 0;
	
	int mMonthsPattern;
	
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
    ArrayList<String[]> m1stMonthWeekDayNumberList = new ArrayList<String[]>(); 
	ArrayList<String[]> m2ndMonthWeekDayNumberList = new ArrayList<String[]>(); 
	ArrayList<String[]> m3rdMonthWeekDayNumberList = new ArrayList<String[]>(); 
	
	String mYearIndicatorText;
	String mFirstMonthText;
	String mSecondMonthText;
	String mThirdMonthText;
	
	public YearMonthsView(Context context) {
		super(context);
		
		mContext = context;
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        
		initView();
	}
	
	public void initView() {
		if (!mInitialized) {            
            mInitialized = true;      
		
			mYearIndicatorPaint = new Paint();
			mYearIndicatorPaint.setFakeBoldText(false);
			mYearIndicatorPaint.setAntiAlias(true);		
			mYearIndicatorPaint.setColor(Color.BLACK);			     
			mYearIndicatorPaint.setTextAlign(Align.LEFT);
			mYearIndicatorPaint.setTypeface(Typeface.SERIF);		
			
			mMonthNumPaint = new Paint();
	        mMonthNumPaint.setFakeBoldText(false);
	        mMonthNumPaint.setAntiAlias(true);        
	        mMonthNumPaint.setColor(Color.RED);	              
	        mMonthNumPaint.setTextAlign(Align.LEFT);
	        mMonthNumPaint.setTypeface(Typeface.MONOSPACE);
	        
	        mMonthDayPaint = new Paint();
	        mMonthDayPaint.setFakeBoldText(true);
	        mMonthDayPaint.setAntiAlias(true);        
	        mMonthDayPaint.setColor(Color.BLACK);	              
	        mMonthDayPaint.setTextAlign(Align.CENTER);
	        mMonthDayPaint.setTypeface(Typeface.MONOSPACE);      
	        
	        mUnderLinePaint = new Paint();        
	        mUnderLinePaint.setAntiAlias(true);        
	        mUnderLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUnderLinePaint.setStyle(Style.STROKE);
	        mUnderLinePaint.setStrokeWidth(getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight));
		}
	}
	
	public void setMonthsParams(HashMap<String, Integer> params, String tz) {
		this.setTag(params);
		
		mTzId = tz;		
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT)) {
        	mYearIndicatorHeight = params.get(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE)) {
        	mYearIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE);
    		mYearIndicatorPaint.setTextSize(mYearIndicatorTextHeight); 
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y)) {
        	mYearIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
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
            mMonthDayPaint.setTextSize(mMiniMonthDayTextHeight);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        }   
        
        if (params.containsKey(VIEW_PARAMS_YEAR_MONTHS_PATTERN)) {
			mMonthsPattern = params.get(VIEW_PARAMS_YEAR_MONTHS_PATTERN);
			
			if (mMonthsPattern == YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {
				mHeight = mYearIndicatorHeight + mMiniMonthHeight;				
			}
			else {
				mHeight = mMiniMonthHeight;
			}
        }
        
        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
        	mFirstDayOfWeek = params.get(VIEW_PARAMS_WEEK_START);
			
        }
		
        if (params.containsKey(VIEW_PARAMS_POSITION)) {
			mPosition = params.get(VIEW_PARAMS_POSITION);
			calcMonthsTime(mPosition); // ���� position ���� �������� �ʴ´ٸ� ���ܸ� �߻����Ѿ� �Ѵ�
        }	
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;        
    }
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight);
    }


	@Override
	protected void onDraw(Canvas canvas) {
		if (mMonthsPattern == YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {
			drawYearIndicatorNormalYearMonths(canvas);
		}
		else if (mMonthsPattern == NORMAL_YEAR_MONTHS_PATTERN) {
			drawNormalYearMonths(canvas);
		}
	}
	
	public void drawYearIndicatorNormalYearMonths(Canvas canvas) {
		//mYearIndicatorPaint
		float yearTextX = mMiniMonthLeftMargin;
		float yearTextY = mYearIndicatorTextBaseLineY;
		canvas.drawText(mYearIndicatorText, yearTextX, yearTextY, mYearIndicatorPaint);
				
		int underLineStartX = mMiniMonthLeftMargin;
		int underLineStopX = mWidth;
		int underLineY = mYearIndicatorHeight - (int)mUnderLinePaint.getStrokeWidth();
		canvas.drawLine(underLineStartX, underLineY, underLineStopX, underLineY, mUnderLinePaint);
		
		int topOffset = mYearIndicatorHeight;
		drawMonth(canvas, 1, topOffset);
		drawMonth(canvas, 2, topOffset);
		drawMonth(canvas, 3, topOffset);
	}
	
	public void drawNormalYearMonths(Canvas canvas) {
		drawMonth(canvas, 1, 0);
		drawMonth(canvas, 2, 0);
		drawMonth(canvas, 3, 0);
	}	
	
	public void drawMonth(Canvas canvas, /*Calendar monthTime, */int whichMonth, int topOffset) {
		int monthDayTextLeftOffset = 0;	
		float monthTextX = 0;		
		String monthText = null;
		ArrayList<String[]> monthWeekDayNumberList = null; 
		
		switch(whichMonth) {
		case YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX:	
			monthTextX = mMiniMonthLeftMargin * whichMonth;
			monthDayTextLeftOffset = (int) monthTextX;
			monthText = mFirstMonthText;
			monthWeekDayNumberList = m1stMonthWeekDayNumberList;
			break;
		case YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX:
			monthTextX = mMiniMonthLeftMargin * whichMonth + mMiniMonthWidth;
			monthDayTextLeftOffset = (int) monthTextX;
			monthText = mSecondMonthText;
			monthWeekDayNumberList = m2ndMonthWeekDayNumberList;
			break;
		case YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX:
			monthTextX = mMiniMonthLeftMargin * whichMonth + (mMiniMonthWidth * 2);
			monthDayTextLeftOffset = (int) monthTextX;
			monthText = mThirdMonthText;
			monthWeekDayNumberList = m3rdMonthWeekDayNumberList;
			break;
		}		
			
		float monthTextY = topOffset + mMiniMonthIndicatorTextBaseLineY;
		canvas.drawText(monthText, monthTextX, monthTextY, mMonthNumPaint);		
		
		int monthDayTextTopOffset = topOffset + mMiniMonthIndicatorTextBaseLineY;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = monthWeekDayNumberList.size();
		for (int i=0; i<weekNumbers; i++) {
			String[] dayNumbers = monthWeekDayNumberList.get(i);		
			monthDayTextY = monthDayTextTopOffset + ((i + 1) * mMiniMonthDayTextBaseLineY);
			
			for (int j=0; j<7; j++) {
				int x = computeTextXPosition(j);
				monthDayTextX = monthDayTextLeftOffset + x;			
				
				if (!dayNumbers[j].equalsIgnoreCase(NOT_MONTHDAY)) {
					canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthDayPaint);
				}
			}
		}		
	}	
	
	private int computeTextXPosition(int day) {
    	int x = 0;
		int effectiveWidth = mMiniMonthWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
        return x;
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
				return mFristMonthTime;
			}
			else if (secondMonthRec.contains(x, y)) {
				return mSecondMonthTime;
			}
			else if (thirdMonthRec.contains(x, y)) {
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
				return mFristMonthTime;
			}
			else if (secondMonthRec.contains(x, y)) {
				return mSecondMonthTime;
			}
			else if (thirdMonthRec.contains(x, y)) {
				return mThirdMonthTime;
			}
			
		}
		
		return null;		
	}	
	
	public void calcMonthsTime(int position) {
		mFristMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		
		//EPOCH_JULIAN_DAY
		int monthsAfterEpoch = position * YearsAdapter.MONTH_NUMBERS_PER_CHILDVIEW;
		int yearsAfterEpoch = monthsAfterEpoch / 12;
		int targetYear = YearsAdapter.EPOCH_YEAR + yearsAfterEpoch;
		//int firstMonth = (monthsAfterEpoch % 12) + 1;
		int firstMonth = monthsAfterEpoch % 12;
		mFristMonthTime.set(targetYear, firstMonth, 1);
			
		mYearIndicatorText = buildYear(mFristMonthTime);
		mFirstMonthText = buildMonth(mFristMonthTime.getTimeInMillis(), mFristMonthTime.getTimeInMillis());//ETime.buildMonth(mFristMonthTime.getTimeInMillis(), mTimeZone);
		m1stMonthWeekDayNumberList = makeDayNumStrings(mFristMonthTime);
		
		mSecondMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		mSecondMonthTime.set(targetYear, firstMonth + 1, 1);
		
		mSecondMonthText = buildMonth(mSecondMonthTime.getTimeInMillis(), mSecondMonthTime.getTimeInMillis());//ETime.buildMonth(mSecondMonthTime.getTimeInMillis(), mTimeZone);
		m2ndMonthWeekDayNumberList = makeDayNumStrings(mSecondMonthTime);
		
		mThirdMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mThirdMonthTime.set(targetYear, firstMonth + 2, 1);
		
		mThirdMonthText = buildMonth(mThirdMonthTime.getTimeInMillis(), mThirdMonthTime.getTimeInMillis());//ETime.buildMonth(mThirdMonthTime.getTimeInMillis(), mTimeZone);
		m3rdMonthWeekDayNumberList = makeDayNumStrings(mThirdMonthTime);
	}	
	
	public ArrayList<String[]> makeDayNumStrings(Calendar monthTime) {
		int targetMonthNumber = monthTime.get(Calendar.MONTH);
		// Figure out what day today is
		Calendar today = GregorianCalendar.getInstance(mTimeZone);
        today.setTimeInMillis(System.currentTimeMillis());
        //mHasToday = false;
        //mToday = -1;
		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
		TempCalendar.setTimeInMillis(monthTime.getTimeInMillis());		
		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
		//int monthFirstJulianDay = ETime.getJulianDay(monthTime.getTimeInMillis(), mGmtoff);
		
		ArrayList<String[]> monthWeekDayNumberList = new ArrayList<String[]>(); 
		
		for (int i=0; i<weekNumbersOfMonth; i++) {
			String[] dayNumbers = new String[7];
			monthWeekDayNumberList.add(dayNumbers);
		}
		
		for (int i=0; i<weekNumbersOfMonth; i++) {
			String[] dayNumbers = monthWeekDayNumberList.get(i);
			Calendar timeForWeek = GregorianCalendar.getInstance(mTimeZone);//new Time(timeZoneString);
			timeForWeek.setTimeInMillis(monthTime.getTimeInMillis());
			timeForWeek.add(Calendar.DAY_OF_MONTH, (i * 7)); //int targetWeekJulianDay = monthFirstJulianDay + (i * 7);	
				
			//int week = ETime.getWeeksSinceEcalendarEpochFromJulianDay(targetWeekJulianDay, mFirstDayOfWeek);//Time.getWeeksSinceEpochFromJulianDay(targetWeekJulianDay, mWeekStart);
			//int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(week);//Utils.getJulianMondayFromWeeksSinceEpoch(week);
	        Calendar time = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
	        time.setFirstDayOfWeek(mFirstDayOfWeek);
	        //long mills = ETime.getMillisFromJulianDay(julianMonday, mTimeZone);
	        time.setTimeInMillis(timeForWeek.getTimeInMillis());//time.setJulianDay(julianMonday);	        
	        ETime.adjustStartDayInWeek(time);
	        	               
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
	
	
	
	public int getMonthsPattern() {
		return mMonthsPattern;
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
