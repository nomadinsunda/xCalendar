package com.intheeast.year;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import com.intheeast.etc.SelectedDateOvalDrawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

public class MiniMonthInYearRow extends View {

	Context mContext;
	Paint mMonthNumPaint;
	Paint mMonthDayPaint;
	
	boolean mInitialized = false;
	boolean mSetDrawingParams = false;
	
	int mWidth;
	int mHeight;
	
	int mMiniMonthWidth;
	int mMiniMonthHeight;
	
	int mMiniMonthLeftMargin;
	int mMiniMonthIndicatorTextHeight;
	int mMiniMonthIndicatorTextBaseLineY;
	int mMiniMonthDayTextHeight;
	int mMiniMonthDayTextBaseLineY;
	
	String mMonthText;
	ArrayList<String[]> mMonthWeekDayNumberList;
	int mMonthColumnNumber;
	float mMonthTextX = 0;	
	int mMonthDayTextLeftOffset;
	
	public MiniMonthInYearRow(Context context) {
		super(context);
		
		initView();
	}
	
	public MiniMonthInYearRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		initView();
	}
	
	public void initView() {
		if (!mInitialized) {
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
	        
	        setBackgroundColor(Color.TRANSPARENT);
	        mInitialized = true;
		}
	}
	
	public void setDrawingParams(HashMap<String, Integer> params) {
		if (!mSetDrawingParams) {
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
	        	mMonthDayPaint.setTextSize(mMiniMonthDayTextHeight); 
	        }        
        
	        mSetDrawingParams = true;
		}
	}
	
	Calendar mMonthTime;
	boolean mHasToday;
	int mDayOfMonthOfToday;
	public void setMonthInfo(Calendar monthTime, String monthText, ArrayList<String[]> monthWeekDayNumberList, int monthColumnNumber) {
		mHasToday = false;
		
		mMonthTime = GregorianCalendar.getInstance(monthTime.getTimeZone());
		mMonthTime.setTimeInMillis(monthTime.getTimeInMillis());
		mMonthText = monthText;
		mMonthWeekDayNumberList = monthWeekDayNumberList;
		mMonthColumnNumber = monthColumnNumber;		
		
		mMonthTextX = 0;
		
		mMonthDayTextLeftOffset = (int)mMonthTextX;
		
		Calendar today = GregorianCalendar.getInstance(monthTime.getTimeZone());
		today.setTimeInMillis(System.currentTimeMillis());
		
		if (today.get(Calendar.YEAR) == mMonthTime.get(Calendar.YEAR) && 
				today.get(Calendar.MONTH) == mMonthTime.get(Calendar.MONTH)) {
			mHasToday = true;
			mDayOfMonthOfToday = today.get(Calendar.DAY_OF_MONTH);
			
			initTodayCircleDrawable();
			setOvalDrawableOfTodayDimens();
		}
	}	
	

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		
		canvas.drawText(mMonthText, mMonthTextX, mMiniMonthIndicatorTextBaseLineY, mMonthNumPaint);		
		
		int monthDayTextTopOffset = mMiniMonthIndicatorTextBaseLineY;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = mMonthWeekDayNumberList.size();
		for (int i=0; i<weekNumbers; i++) {
			String[] dayNumbers = mMonthWeekDayNumberList.get(i);		
			monthDayTextY = monthDayTextTopOffset + ((i + 1) * mMiniMonthDayTextBaseLineY);
			
			for (int j=0; j<7; j++) {
				monthDayTextX = computeTextXPosition(j);				
								
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {
					if (mHasToday) {
						int dayOfMonth = Integer.parseInt(dayNumbers[j]);
						if (dayOfMonth == mDayOfMonthOfToday) {
							int offsetY = mMiniMonthIndicatorTextBaseLineY + (i * mMiniMonthDayTextBaseLineY);
							makeTodayCircleDrawable(canvas, mDayOfMonthOfToday, j, offsetY);
						}
						else {
							canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthDayPaint);
						}
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
	
	
	SelectedDateOvalDrawable mOvalDrawableOfToday;    

    int mOvalDrawableOfTodayRadius;	
	int mOvalDrawableOfTodaySize;
	
	public void initTodayCircleDrawable() {
		mOvalDrawableOfToday = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthDayPaint.getTextSize());
		// Oval을 drawing하는 부모의 Paint 설정이다
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
	
	public void makeTodayCircleDrawable (Canvas canvas, int dateNum, int xPos, int offsetY) {
		float baseLineY = mMiniMonthDayTextBaseLineY;
    	
    	float textAscent = mMonthDayPaint.ascent();
    	float textDescent = mMonthDayPaint.descent();
    	
    	float textTopY = baseLineY + textAscent;//float textTopY = Math.abs(baseLineY + textAscent); 	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    			
		int x = computeTextXPosition(xPos);
		int left = x - mOvalDrawableOfTodayRadius;	    			
		int top = (int) (textCenterY - mOvalDrawableOfTodayRadius);
		int right = left + mOvalDrawableOfTodaySize;
		int bottom = top + mOvalDrawableOfTodaySize;		
		
		mOvalDrawableOfToday.setBounds(left, top, right, bottom);			       
		mOvalDrawableOfToday.setDrawTextPosition(x, offsetY + mMiniMonthDayTextBaseLineY);
		mOvalDrawableOfToday.setDayOfMonth(dateNum);
		mOvalDrawableOfToday.draw(canvas);   		
	}

}
