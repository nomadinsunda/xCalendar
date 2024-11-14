package com.intheeast.acalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.View;

public class DayHeaderView extends View {
	private final Typeface mBold = Typeface.DEFAULT_BOLD; 
	
	private static float DAY_HEADER_FONT_SIZE;
    private static int DAY_HEADER_BOTTOM_MARGIN; 	   
    
    Calendar mBaseDate;
	int mViewWidth;    	
	int mViewHeight;    	
	int mDayOfWeekTextWidth;
	int mLeftSideMargin;    	
	
	private String[] mDayStrs;
    private String[] mDayStrs2Letter;
    
    private final Paint mPaint = new Paint();
    
    CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
	public DayHeaderView(Context context, CalendarViewsSecondaryActionBar actionBar, Calendar time) {
		super(context);
		
		mCalendarViewsSecondaryActionBar = actionBar;
		
		mBaseDate = GregorianCalendar.getInstance(time.getTimeZone());
		mBaseDate.setFirstDayOfWeek(time.getFirstDayOfWeek());
		mBaseDate.setTimeInMillis(time.getTimeInMillis());
		mBaseDate.set(Calendar.HOUR_OF_DAY, 0);
		mBaseDate.set(Calendar.MINUTE, 0);
		mBaseDate.set(Calendar.SECOND, 0);
		mBaseDate.set(Calendar.MILLISECOND, 0);
		
		DAY_HEADER_FONT_SIZE = (int) context.getResources().getDimension(R.dimen.day_label_text_size); //11sp
    	DAY_HEADER_BOTTOM_MARGIN = (int) context.getResources().getDimension(R.dimen.day_header_bottom_margin); //4dip				
		
		mDayStrs = new String[14];
        mDayStrs2Letter = new String[14];
        
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            int index = i - Calendar.SUNDAY;
            // e.g. Tue for Tuesday
            mDayStrs[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_MEDIUM).toUpperCase();
            mDayStrs[index + 7] = mDayStrs[index];
            // e.g. Tu for Tuesday
            mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORT).toUpperCase();

            // If we don't have 2-letter day strings, fall back to 1-letter.
            if (mDayStrs2Letter[index].equals(mDayStrs[index])) {
                mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORTEST);
            }

            mDayStrs2Letter[index + 7] = mDayStrs2Letter[index];
        }	        
	}
	
	@Override
    protected void onAttachedToWindow() {
        //Log.i("tag", "DayHeaderView : onAttachedToWindow");
        super.onAttachedToWindow();
    }
	
	
	@Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {						
		mViewWidth = width;
		
		int effectiveWidth = mViewWidth;
		mDayOfWeekTextWidth = effectiveWidth / 7;
		mLeftSideMargin = mDayOfWeekTextWidth / 2;
		
    }
	
	@Override
    protected void onDraw(Canvas canvas) {
			
		drawDayHeaderLoop(canvas);			
	}
	
	public void updateTimeZone(TimeZone tz) {
		mBaseDate.setTimeZone(tz);			
		recalc();
	}
	
	int mFirstVisibleDayOfWeek;
	public void recalc() {
        // Set the base date to the beginning of the week 
    	// if we are displaying 7 days at a time.
        adjustToBeginningOfWeek();	        
        mFirstVisibleDayOfWeek = mBaseDate.get(Calendar.DAY_OF_WEEK);//mFirstVisibleDayOfWeek = mBaseDate.weekDay; 
    }
	
	private void adjustToBeginningOfWeek() {
        int dayOfWeek = mBaseDate.get(Calendar.DAY_OF_WEEK);
        int diff = dayOfWeek - mCalendarViewsSecondaryActionBar.mFirstDayOfWeek;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            
            mBaseDate.add(Calendar.DAY_OF_MONTH, -diff);
            //mBaseDate.monthDay -= diff;	            
        }
    }
	
	private void drawDayHeaderLoop(Canvas canvas) {   
       
		mPaint.setTextSize(DAY_HEADER_FONT_SIZE);
        mPaint.setTypeface(Typeface.DEFAULT);
		mPaint.setTextAlign(Paint.Align.CENTER);			
		mPaint.setAntiAlias(true);
        
    	String[] dayNames;
        dayNames = mDayStrs2Letter;        
        
        for (int day = 0; day < 7; day++) {
            int dayOfWeek = day + mFirstVisibleDayOfWeek;
            if (dayOfWeek >= 14) {
                dayOfWeek -= 14;
            }

            int color = mCalendarViewsSecondaryActionBar.mDayOfWeekTextColor;
            
            final int column = day % 7;
            if (Utils.isSaturday(column, mCalendarViewsSecondaryActionBar.mFirstDayOfWeek)) {
                color = mCalendarViewsSecondaryActionBar.mWeek_saturdayColor;
            } else if (Utils.isSunday(column, mCalendarViewsSecondaryActionBar.mFirstDayOfWeek)) {
                color = mCalendarViewsSecondaryActionBar.mWeek_sundayColor;
            }	            

            mPaint.setColor(color);
            drawDayHeader(dayNames[dayOfWeek], day, canvas);
        }     
    }
	
	private void drawDayHeader(String dayStr, int day, Canvas canvas) {	        
        float y = CalendarViewsSecondaryActionBar.DAY_HEADER_HEIGHT - DAY_HEADER_BOTTOM_MARGIN;

        int x = computeDayLeftPosition(day);            
        canvas.drawText(dayStr, x, y, mPaint);
    }		
	
	private int computeDayLeftPosition(int day) {				
        return mLeftSideMargin + (day * mDayOfWeekTextWidth);			
    }		
}
