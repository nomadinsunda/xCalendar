package com.intheeast.year;

import java.util.HashMap;

import com.intheeast.acalendar.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class YearIndicatorRegion extends View {
	
	Context mContext;
	Paint mYearIndicatorPaint;
	Paint mUnderLinePaint;
	
	int mWidth;
	int mMiniMonthLeftMargin;
	int mYearIndicatorHeight;
	int mYearIndicatorTextHeight;
	int mYearIndicatorTextBaseLineY;
	
	String mYearIndicatorText;
	boolean mInitialized = false;
	boolean mSetDrawingParams = false;
	
	public YearIndicatorRegion(Context context) {
		super(context);
		mContext = context;
		initView();
	}
	
	public YearIndicatorRegion(Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		initView();
	}	
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		
		mWidth = w;
		
		super.onSizeChanged(w, h, oldw, oldh);
	}

	public void initView() {
		if (!mInitialized) {
			mYearIndicatorPaint = new Paint();
			mYearIndicatorPaint.setFakeBoldText(false);
			mYearIndicatorPaint.setAntiAlias(true);		
			mYearIndicatorPaint.setColor(Color.BLACK);		  
			mYearIndicatorPaint.setTextAlign(Align.LEFT);
			mYearIndicatorPaint.setTypeface(Typeface.SERIF);
			
			mUnderLinePaint = new Paint();        
	        mUnderLinePaint.setAntiAlias(true);        
	        mUnderLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUnderLinePaint.setStyle(Style.STROKE);
	        mUnderLinePaint.setStrokeWidth(getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight));	
	        
	        mInitialized = true;
		}	
	}
	
	public void setDrawingParams(HashMap<String, Integer> params) {
		if (!mSetDrawingParams) {
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
	        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
	        } 
			
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
        
	        mSetDrawingParams = true;
		}
	}
	
	public void setYearText(String text) {
		mYearIndicatorText = text;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		float yearTextX = mMiniMonthLeftMargin;
		float yearTextY = mYearIndicatorTextBaseLineY;
		canvas.drawText(mYearIndicatorText, yearTextX, yearTextY, mYearIndicatorPaint);
				
		int underLineStartX = mMiniMonthLeftMargin;
		int underLineStopX = mWidth;
		int underLineY = mYearIndicatorHeight - (int)mUnderLinePaint.getStrokeWidth();
		canvas.drawLine(underLineStartX, underLineY, underLineStopX, underLineY, mUnderLinePaint);
	}
	
}
