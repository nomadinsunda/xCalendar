package com.intheeast.month;

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
import com.intheeast.etc.SelectedDateOvalDrawable;
import com.intheeast.year.YearMonthsView;
import com.intheeast.year.YearsAdapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.OvalShape;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;

public class PsudoYearView extends View {

public static final String VIEW_PARAMS_YEAR_MONTHS_PATTERN = "year_months_pattern";
	
	Context mContext;
	Paint mYearIndicatorPaint;
	Paint mMonthNumPaint;
	Paint mMonthDayPaint;
	Paint mUnderLinePaint;
	
	Calendar mSelectedYearTime;
	
	String mTzId = ETime.getCurrentTimezone();
	TimeZone mTimeZone;
	long mGmtoff;
	
	int mSelectedMonthNumber;
	int mFirstDayOfWeek;
	
	int mFirstPosition;
		
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
	
	String mYearIndicatorText;
	
	ArrayList<MonthRowInfo> mMonthRowInfoList = new ArrayList<MonthRowInfo>();
		
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
	public PsudoYearView(Context context) {
		super(context);
		
		mContext = context;
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        initView();
	}
	
	public PsudoYearView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        initView();
	}
	
	public void initView() {
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
	
	boolean mHasToday = false;
	int mMonthOfToday;
	int mDayOfMonthOfToday;
	public void setMonthsParams(HashMap<String, Integer> params, Calendar selectedYearTime) {
		this.setTag(params);
		
		mTimeZone = selectedYearTime.getTimeZone();
		mTzId = mTimeZone.getID();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		
		mSelectedYearTime = GregorianCalendar.getInstance(mTimeZone);
		mSelectedYearTime.setTimeInMillis(selectedYearTime.getTimeInMillis());		
		mSelectedMonthNumber = mSelectedYearTime.get(Calendar.MONTH);
		
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
                
        if (params.containsKey(YearMonthsView.VIEW_PARAMS_WEEK_START)) {
        	mFirstDayOfWeek = params.get(YearMonthsView.VIEW_PARAMS_WEEK_START);			
        }
		
        if (params.containsKey(YearMonthsView.VIEW_PARAMS_POSITION)) {
        	mFirstPosition = params.get(YearMonthsView.VIEW_PARAMS_POSITION);
        
        	makeYearIndicatorText(mFirstPosition);
        	
        	int position = mFirstPosition;
        	for (int i=0; i<4; i++) {     
        		MonthRowInfo obj = new MonthRowInfo();
        		mMonthRowInfoList.add(i, obj);
        		calcMonthsTime(position++, i, obj); // ���� position ���� �������� �ʴ´ٸ� ���ܸ� �߻����Ѿ� �Ѵ�
        	}
        }	
        
        Calendar today = GregorianCalendar.getInstance(mSelectedYearTime.getTimeZone());
		today.setTimeInMillis(System.currentTimeMillis());
		
		if (today.get(Calendar.YEAR) == mSelectedYearTime.get(Calendar.YEAR)) {
			// selected year�� month�� PsudoTargetMonthView�� drawing �ϱ� ������
			// today.get(Calendar.MONTH) == mSelectedYearTime.get(Calendar.MONTH) ��Ȳ�� �����ؾ� �Ѵ�
			// :PsudoYearView������ mSelectedMonthNumber�� ������ month�� drawing ���� �ʴ´�!!!
			if (today.get(Calendar.MONTH) != mSelectedMonthNumber) {
				mHasToday = true;
				mMonthOfToday = today.get(Calendar.MONTH);
				mDayOfMonthOfToday = today.get(Calendar.DAY_OF_MONTH);
				
				initTodayCircleDrawable();
				setOvalDrawableOfTodayDimens();
			}		
		}
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;        
    }
		
	@Override
	protected void onDraw(Canvas canvas) {
		int topOffset = 0;
		for (int i=0; i<4; i++) {					
			if (i==0) {
				float yearTextX = mMiniMonthLeftMargin;
				float yearTextY = mYearIndicatorTextBaseLineY;
				canvas.drawText(mYearIndicatorText, yearTextX, yearTextY, mYearIndicatorPaint);
						
				int underLineStartX = mMiniMonthLeftMargin;
				int underLineStopX = mWidth;
				int underLineY = mYearIndicatorHeight - (int)mUnderLinePaint.getStrokeWidth();
				canvas.drawLine(underLineStartX, underLineY, underLineStopX, underLineY, mUnderLinePaint);
				
				topOffset = mYearIndicatorHeight;
			}
			else {
				topOffset = topOffset + mMiniMonthHeight;/////////////////////////////////////////////���� Ȯ���� ����
			}
			
			MonthRowInfo obj = mMonthRowInfoList.get(i);
			int monthDayTextLeftOffset = 0;	
			float monthTextX = 0;		
			String monthText = null;
						
			for (int j=0; j<3; j++) {		
				if (obj.mExistSelectedMonth && (obj.mSelectedMonthColumnNumber == j) ) {
					continue;
				}
				
				ArrayList<String[]> monthWeekDayNumberList = null; 
				
				if (j==0) {
					monthTextX = mMiniMonthLeftMargin * 1;
					monthDayTextLeftOffset = (int) monthTextX;
					monthText = obj.mFirstMonthText;
					monthWeekDayNumberList = obj.m1stMonthWeekDayNumberList;
				}
				else if (j==1) {
					monthTextX = (mMiniMonthLeftMargin * 2) + mMiniMonthWidth;
					monthDayTextLeftOffset = (int) monthTextX;
					monthText = obj.mSecondMonthText;
					monthWeekDayNumberList = obj.m2ndMonthWeekDayNumberList;
				}
				else if (j==2) {
					monthTextX = (mMiniMonthLeftMargin * 3) + (mMiniMonthWidth * 2);
					monthDayTextLeftOffset = (int) monthTextX;
					monthText = obj.mThirdMonthText;
					monthWeekDayNumberList = obj.m3rdMonthWeekDayNumberList;
				}
							
				// Month number�� �������
				// month number = (i*3) + j;
				// i = 1, j = 2
				// 3 = ( (1*3) + 2) + 1 = 6
				if (mHasToday) {
					int willDrawingMonthNumber = (i * 3) + j;
					if (mMonthOfToday == willDrawingMonthNumber) {
						drawMonth(canvas, true, monthDayTextLeftOffset, monthTextX, monthText, monthWeekDayNumberList, topOffset);
						continue;
					}
				}
				
				drawMonth(canvas, monthDayTextLeftOffset, monthTextX, monthText, monthWeekDayNumberList, topOffset);				
			}
		}
	}	
	
	public void drawMonth(Canvas canvas, int monthDayTextLeftOffset, float monthTextX, String monthText, ArrayList<String[]> monthWeekDayNumberList, int topOffset) {
					
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
				
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthsView.NOT_MONTHDAY)) {
					canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthDayPaint);
				}
			}
		}		
	}	
	
	public void drawMonth(Canvas canvas, boolean hasToday, int monthDayTextLeftOffset, float monthTextX, String monthText, ArrayList<String[]> monthWeekDayNumberList, int topOffset) {
		
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
				
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthsView.NOT_MONTHDAY)) {
					
					int dayOfMonth = Integer.parseInt(dayNumbers[j]);
					if (dayOfMonth == mDayOfMonthOfToday) {
						int offsetY = (int) monthDayTextY;//topOffset + mMiniMonthIndicatorTextBaseLineY + (i * mMiniMonthDayTextBaseLineY);
						makeTodayCircleDrawable(canvas, mDayOfMonthOfToday, (int)monthDayTextX, offsetY);
					}
					else {
						canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthDayPaint);
					}					
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
	
	
	public void calcMonthsTime(int position, int rowIndex, MonthRowInfo obj) {
		obj.mRowIndex = rowIndex;
		
		obj.mFristMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		obj.mSecondMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		obj.mThirdMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
				
		int monthsAfterEpoch = position * YearsAdapter.MONTH_NUMBERS_PER_CHILDVIEW;
		int yearsAfterEpoch = monthsAfterEpoch / 12;
		int targetYear = YearsAdapter.EPOCH_YEAR + yearsAfterEpoch;		
		int firstMonth = monthsAfterEpoch % 12;
		if (mSelectedMonthNumber != firstMonth) {
			obj.mFristMonthTime.set(targetYear, firstMonth, 1);				
			obj.mFirstMonthText = buildMonth(obj.mFristMonthTime.getTimeInMillis(), obj.mFristMonthTime.getTimeInMillis());//ETime.buildMonth(obj.mFristMonthTime.getTimeInMillis(), mTimeZone);
			obj.m1stMonthWeekDayNumberList = makeDayNumStrings(obj.mFristMonthTime);
		}
		else {
			obj.mExistSelectedMonth = true;
			obj.mSelectedMonthColumnNumber = 0;
		}
		
		
		int secondMonth = firstMonth + 1;
		if (mSelectedMonthNumber != secondMonth) {
			obj.mSecondMonthTime.set(targetYear, secondMonth, 1);			
			obj.mSecondMonthText = buildMonth(obj.mSecondMonthTime.getTimeInMillis(), obj.mSecondMonthTime.getTimeInMillis());//ETime.buildMonth(obj.mSecondMonthTime.getTimeInMillis(), mTimeZone);
			obj.m2ndMonthWeekDayNumberList = makeDayNumStrings(obj.mSecondMonthTime);			
		}
		else {
			obj.mExistSelectedMonth = true;
			obj.mSelectedMonthColumnNumber = 1;
		}
		
		int thirdMonth = firstMonth + 2;
		if (mSelectedMonthNumber != thirdMonth) {
			obj.mThirdMonthTime.set(targetYear, thirdMonth, 1);			
			obj.mThirdMonthText = buildMonth(obj.mThirdMonthTime.getTimeInMillis(), obj.mThirdMonthTime.getTimeInMillis());//ETime.buildMonth(obj.mThirdMonthTime.getTimeInMillis(), mTimeZone);
			obj.m3rdMonthWeekDayNumberList = makeDayNumStrings(obj.mThirdMonthTime);		
		}
		else {
			obj.mExistSelectedMonth = true;
			obj.mSelectedMonthColumnNumber = 2;
		}
		
	}	
	
	public void makeYearIndicatorText(int position) {
		int monthsAfterEpoch = position * YearsAdapter.MONTH_NUMBERS_PER_CHILDVIEW;
		int yearsAfterEpoch = monthsAfterEpoch / 12;
		int targetYear = YearsAdapter.EPOCH_YEAR + yearsAfterEpoch;		
		int firstMonth = monthsAfterEpoch % 12;
		Calendar firstMonthTime = GregorianCalendar.getInstance(mTimeZone);
		firstMonthTime.set(targetYear, firstMonth, 1);		
		mYearIndicatorText = buildYear(firstMonthTime);
	}
	
	public ArrayList<String[]> makeDayNumStrings(Calendar monthTime) {
		int targetMonthNumber = monthTime.get(Calendar.MONTH);
		// Figure out what day today is
		Calendar today = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
        today.setTimeInMillis(System.currentTimeMillis());
        		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);  
		TempCalendar.setTimeInMillis(monthTime.getTimeInMillis());		
		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);//getWeekNumbersOfMonth(TempCalendar);
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
			time.setTimeInMillis(timeForWeek.getTimeInMillis()); //time.setJulianDay(julianMonday);
			ETime.adjustStartDayInWeek(time);
	        
	               
	        for (int j=0; j < 7; j++) {	        	                        
	        	    	
	        	if (time.get(Calendar.MONTH) == targetMonthNumber) {
	        		dayNumbers[j] = Integer.toString(time.get(Calendar.DAY_OF_MONTH));
	        	}
	        	else
	        		dayNumbers[j] = YearMonthsView.NOT_MONTHDAY;
	        	
	        	time.add(Calendar.DAY_OF_MONTH, 1);//time.monthDay++;	        	
	        }
		}

        return monthWeekDayNumberList;        
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
                /*DateUtils.FORMAT_SHOW_DATE |*/ DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_SHOW_YEAR,
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
	
	SelectedDateOvalDrawable mOvalDrawableOfToday;    

    int mOvalDrawableOfTodayRadius;	
	int mOvalDrawableOfTodaySize;
	
	public void initTodayCircleDrawable() {
		mOvalDrawableOfToday = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthDayPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
		mOvalDrawableOfToday.getPaint().setAntiAlias(true);
		mOvalDrawableOfToday.getPaint().setStyle(Style.FILL);			
		mOvalDrawableOfToday.getPaint().setColor(Color.RED);			
	}
	
	public void setOvalDrawableOfTodayDimens() {
		mOvalDrawableOfToday.setTextSize(mMonthDayPaint.getTextSize());
		
		float radius = mMiniMonthDayTextBaseLineY * 0.4f;
		mOvalDrawableOfTodayRadius = (int)radius;
		mOvalDrawableOfTodaySize = (int)(mOvalDrawableOfTodayRadius * 2);		
	}
	
	public void makeTodayCircleDrawable (Canvas canvas, int dateNum, int textX, int offsetY) {
		
    	float textAscent = mMonthDayPaint.ascent();
    	float textDescent = mMonthDayPaint.descent();
    	
    	float textTopY = /*baseLineY +*/ textAscent;
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    			
		//int x = computeTextXPosition(xPos);
		int left = textX - mOvalDrawableOfTodayRadius;	    			
		int top = (int) (textCenterY - mOvalDrawableOfTodayRadius);
		int right = left + mOvalDrawableOfTodaySize;
		int bottom = top + mOvalDrawableOfTodaySize;		
		
		mOvalDrawableOfToday.setBounds(left, top, right, bottom);			       
		mOvalDrawableOfToday.setDrawTextPosition(textX, offsetY);
		mOvalDrawableOfToday.setDayOfMonth(dateNum);
		mOvalDrawableOfToday.draw(canvas);   		
	}
	
	public class MonthRowInfo {
		boolean mExistSelectedMonth = false;
		int mSelectedMonthColumnNumber = -1;
		int mRowIndex;
		
		Calendar mFristMonthTime;
		Calendar mSecondMonthTime;
		Calendar mThirdMonthTime;
		
		ArrayList<String[]> m1stMonthWeekDayNumberList = new ArrayList<String[]>(); 
		ArrayList<String[]> m2ndMonthWeekDayNumberList = new ArrayList<String[]>(); 
		ArrayList<String[]> m3rdMonthWeekDayNumberList = new ArrayList<String[]>(); 	
		
		String mFirstMonthText;
		String mSecondMonthText;
		String mThirdMonthText;
		
		public MonthRowInfo() {
			
		}
	}
		
	
}
