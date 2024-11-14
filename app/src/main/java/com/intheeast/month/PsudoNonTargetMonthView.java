package com.intheeast.month;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.etc.ETime;
import com.intheeast.year.YearMonthTableLayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PsudoNonTargetMonthView extends View {

	Context mContext;
	Paint mMonthNumPaint;
	//Paint mMonthNumPaintAfterChange;
	//Paint mNextMonthNumPaint;
	//Paint mMinMonthMonthDayPaint;
	Paint mMonthDayPaint;
	Paint mUpperLinePaint;
	Paint mEventExisteneCirclePaint;
	
	float mUpperLineStrokeWidth;	
	
	boolean mInitialized = false;
	boolean mSetDrawingParams = false;
	
	//float mScaleWidthSize;
	int mOriginalMonthListViewHeight;
	int mWidth;
	int mHeight;
		
	int mMonthListViewMonthIndicatorHeight;
	int mMonthListViewNormalWeekHeight;
	//int mMonthListViewLastWeekHeight;	
	float mMonthTextSize;
	float mMonthDayTextHeight;
	float mMonthTextBottomPadding;
	float mMonthDayTextBaselineY;
	int mEventCircleTopPadding;
	
	String mMonthText;
		
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
    float mEventCircleRadius;
    
	public PsudoNonTargetMonthView(Context context) {
		super(context);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView();
	}
	
	public PsudoNonTargetMonthView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView();
	}
	
	public void initView() {
		if (!mInitialized) {
			mMonthNumPaint = new Paint();
	        mMonthNumPaint.setFakeBoldText(false);
	        mMonthNumPaint.setAntiAlias(true);	        
	        mMonthNumPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mMonthNumPaint.setStyle(Style.FILL);        
	        mMonthNumPaint.setTextAlign(Align.CENTER);
	        mMonthNumPaint.setTypeface(Typeface.DEFAULT);           
	        
	        mMonthDayPaint = new Paint();
	        mMonthDayPaint.setFakeBoldText(false);
	        mMonthDayPaint.setAntiAlias(true);	        
	        mMonthDayPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mMonthDayPaint.setStyle(Style.FILL);        
	        mMonthDayPaint.setTextAlign(Align.CENTER);
	        mMonthDayPaint.setTypeface(Typeface.DEFAULT);  
	        
	        mUpperLinePaint = new Paint();        
	        mUpperLinePaint.setAntiAlias(true);        
	        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUpperLinePaint.setStyle(Style.STROKE);
	        mUpperLineStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
	        mUpperLinePaint.setStrokeWidth(mUpperLineStrokeWidth);
	        
	        mEventExisteneCirclePaint = new Paint();    
	        mEventExisteneCirclePaint.setAntiAlias(true);        
	        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
	        mEventExisteneCirclePaint.setStyle(Style.FILL); 
	        
	        mInitialized = true;
		}
	}
		
	public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        // The MIN_WEEK_WIDTH is a hack to prevent the view from trying to
        // generate dna bits before its width has been fixed.
        //createDna(unsortedEvents);//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
	
	
	public void setMonthDrawingParams(MonthListView monthListView) {
		if (!mSetDrawingParams) {			
			mOriginalMonthListViewHeight = monthListView.getOriginalListViewHeight();
			mMonthListViewMonthIndicatorHeight = monthListView.getNMMonthIndicatorHeight();
			mMonthListViewNormalWeekHeight = monthListView.getNMNormalWeekHeight();
			
			mMonthTextSize = mMonthDayTextHeight = mOriginalMonthListViewHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT;
			mMonthTextBottomPadding =  (float)(mMonthListViewMonthIndicatorHeight * 
        			(1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT));		
			mMonthDayTextBaselineY = mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT;	        	
        	mEventCircleTopPadding = (int) (mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);		
			
        	mMonthNumPaint.setTextSize(mMonthTextSize);
        	mMonthDayPaint.setTextSize(mMonthDayTextHeight);
        	
        	mEventCircleRadius = (mMonthListViewNormalWeekHeight * 0.1f) / 2;  	
			
	        mSetDrawingParams = true;
		}
	}
	
	Calendar mMonthTime;
	//int mFirstJulainDay;
	int mNumDays;
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	int mPrvMonthLastWeekDaysNumOfMonth;
	int mStartDrawingWeekIndex;
	int mEndDrawingWeekIndex;
			
	ArrayList<String[]> mMonthWeekDayNumberList = null;
	float mMonthTextFinalX;	
	
	public void setMonthInfo(Calendar monthTime, int numDays, ArrayList<String[]> monthWeekDayNumberList, int startDrawingWeekIndex, int endDrawingWeekIndex) {					
		//mFirstJulainDay = firstJulianDay;
		mTimeZone = monthTime.getTimeZone();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mTzId = mTimeZone.getID();
		mMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mMonthTime.setTimeInMillis(monthTime.getTimeInMillis());//monthTime;
		mNumDays = numDays;		
		
		mMonthWeekDayNumberList = monthWeekDayNumberList;
		mStartDrawingWeekIndex = startDrawingWeekIndex;
		mEndDrawingWeekIndex = endDrawingWeekIndex;
				
		mMonthText = buildMonth(mMonthTime.getTimeInMillis(), mMonthTime.getTimeInMillis());//ETime.buildMonth(mMonthTime.getTimeInMillis(), mTimeZone);
		mPrvMonthLastWeekDaysNumOfMonth = 0;
		String[] dayNumbersOfMonth = mMonthWeekDayNumberList.get(0);
		for (int j=0; j<7; j++) {
			if (dayNumbersOfMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
				mPrvMonthLastWeekDaysNumOfMonth++;
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
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;		
		
		int monthDayTextTopOffset = 0;
		float currentWeekTop = 0;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		
				
		int drawingWeekNumbers = 0;
		int weekLineY = (int) mUpperLineStrokeWidth;
		int i = mStartDrawingWeekIndex;		
		int end = mEndDrawingWeekIndex;
		mMonthTextFinalX = computeTextXPositionOfMonthListView(mPrvMonthLastWeekDaysNumOfMonth);
		
		int targetDayCount = 0;
		for (; i<end; i++) {	
			if (i==0) {		
				float monthIndicatorTextY = mMonthListViewMonthIndicatorHeight - mMonthTextBottomPadding;
				canvas.drawText(mMonthText, mMonthTextFinalX, monthIndicatorTextY, mMonthNumPaint);
				weekLineY = mMonthListViewMonthIndicatorHeight - weekLineY;
				canvas.drawLine(dayOfWeekTextWidth * mPrvMonthLastWeekDaysNumOfMonth, weekLineY, mWidth, weekLineY, mUpperLinePaint);
				monthDayTextTopOffset = mMonthListViewMonthIndicatorHeight;
				
				currentWeekTop = monthDayTextTopOffset + (drawingWeekNumbers * mMonthListViewNormalWeekHeight);
						
				String[] dayNumbers = mMonthWeekDayNumberList.get(i);			
				monthDayTextY = currentWeekTop + mMonthDayTextBaselineY;
				
				for (int j=0; j<7; j++) {
					monthDayTextX = computeTextXPosition(j);				
									
					if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {					
						canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthDayPaint);	
						
						if (mEventExistence != null) {						
							if (mEventExistence[targetDayCount]) {/////////////////////////////////////////////////////////�̰� ������...
								float cx = monthDayTextX;									
								float cy = currentWeekTop + mEventCircleTopPadding;
								canvas.drawCircle(cx, cy, mEventCircleRadius, mEventExisteneCirclePaint);
							}
						}
						
						targetDayCount++;
					}
				}	
			}
			else {
				String[] dayNumbers = mMonthWeekDayNumberList.get(i);		
				currentWeekTop = monthDayTextTopOffset + (drawingWeekNumbers * mMonthListViewNormalWeekHeight);				
				monthDayTextY = currentWeekTop + mMonthDayTextBaselineY;
				
				int upperLineWidth = 0;
				for (int j=0; j<7; j++) {
					monthDayTextX = computeTextXPosition(j);				
									
					if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {					
						canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthDayPaint);
						if (mEventExistence != null) {						
							if (mEventExistence[targetDayCount]) {/////////////////////////////////////////////////////////�̰� ������...
								float cx = monthDayTextX;									
								float cy = currentWeekTop + mEventCircleTopPadding;
								canvas.drawCircle(cx, cy, mEventCircleRadius, mEventExisteneCirclePaint);
							}
						}				
						
						upperLineWidth = upperLineWidth + dayOfWeekTextWidth;
						targetDayCount++;
					}
				}	
				
				weekLineY = weekLineY + mMonthListViewNormalWeekHeight;
				canvas.drawLine(0, weekLineY, upperLineWidth, weekLineY, mUpperLinePaint);	
			}		
			
			drawingWeekNumbers++;
		}		
	}
	
	
	private int computeTextXPosition(int day) {
		int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
	    return x;
	}
	
	private int computeTextXPositionOfMonthListView(int day) {
		int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
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
