package com.intheeast.acalendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;

public class CommonLinearLayoutItemContainer extends LinearLayout {

	public static final int NOT_BOLDER_LINE_POSITION = -1;
    public static final int UPPER_BOLDER_LINE_POSITION = 0;
    public static final int LOWER_BOLDER_LINE_POSITION = 1;
    public static final int BOTHEND_BOLDER_LINE_POSITION = 2;
    
    DisplayMetrics mDisplayMetrics;
	Paint mPaint;
	float mStrokeWidth;
	int mPaddingLeft;
	int mBolderLinePosition;
	
	int mWidth;
	int mHeight;
	
	boolean mSetWillDraw = false;
	
	public CommonLinearLayoutItemContainer(Context context) {
		super(context);
		
		getDisplayMetrics();
		init(context);
		
	}
	
	public CommonLinearLayoutItemContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		getDisplayMetrics();
		init(context, attrs, 0);
	}
	
	public CommonLinearLayoutItemContainer(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		getDisplayMetrics();
		init(context, attrs, defStyle);
	}
	
	public CommonLinearLayoutItemContainer(Context context, int paddingLeft, int bolderLinePosition) {
		super(context);
		
		getDisplayMetrics();
		init(context, paddingLeft, bolderLinePosition);
	}

	public void getDisplayMetrics() {		
		mDisplayMetrics = getResources().getDisplayMetrics();
	}
	
	public void init(Context context) {
		mPaddingLeft = 0;
		mBolderLinePosition = NOT_BOLDER_LINE_POSITION;		
		
    	mPaint = new Paint();
		mPaint.setAntiAlias(true);		
		mPaint.setColor(getResources().getColor(R.color.selectCalendarViewSeperatorOutLineColor));
		mPaint.setStyle(Style.STROKE);		
		mStrokeWidth = getResources().getDimension(R.dimen.selectCalendarViewSeperatorOutLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); 		
	}
	
	public void init(Context context, AttributeSet attrs, int defStyle) {		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CustomView, defStyle, 0);		
		mPaddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ta.getInteger( R.styleable.CustomView_paddingLeft, 0), mDisplayMetrics);
		mBolderLinePosition = ta.getInteger( R.styleable.CustomView_bolderLinePosition, 0);		
		
    	mPaint = new Paint();
		mPaint.setAntiAlias(true);
		
		mPaint.setColor(getResources().getColor(R.color.selectCalendarViewSeperatorOutLineColor));
		//mPaint.setColor(Color.BLACK); // for test
		
		mPaint.setStyle(Style.STROKE);
		// <dimen name="eventItemLayoutUnderLineHeight">1dp</dimen>
		//The stroke width is defined in pixels
		mStrokeWidth = getResources().getDimension(R.dimen.selectCalendarViewSeperatorOutLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); 				
	}
	
	public void init(Context context, int paddingLeft, int bolderLinePosition) {
		mPaddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingLeft, mDisplayMetrics);
		mBolderLinePosition = bolderLinePosition;		
		
    	mPaint = new Paint();
		mPaint.setAntiAlias(true);
		
		mPaint.setColor(getResources().getColor(R.color.selectCalendarViewSeperatorOutLineColor));
		//mPaint.setColor(Color.BLACK);
		
		mPaint.setStyle(Style.STROKE);
		// <dimen name="eventItemLayoutUnderLineHeight">1dp</dimen>
		//The stroke width is defined in pixels
		mStrokeWidth = getResources().getDimension(R.dimen.selectCalendarViewSeperatorOutLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); 		
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	
    	mWidth = w;
		mHeight = h;
		
		// RelativeLayout �� �� �ʿ䰡 ����??? : �ִ� �� �Ǵ� ��찡 �ִ� �� ���� -> response_menu!!!
		if (mBolderLinePosition != NOT_BOLDER_LINE_POSITION) {
			if (!mSetWillDraw) {
				mSetWillDraw = true;
				setWillNotDraw (false);
			}
		}
		else {
			mSetWillDraw = false;
			setWillNotDraw (true);
		}
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	/*
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}
	*/
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if (mSetWillDraw) {
	        float startX = mPaddingLeft;
	        float stopX = mWidth;
	        
	        float startY = 0;        
	        float stopY = 0;
	        	        
	        if (mBolderLinePosition == UPPER_BOLDER_LINE_POSITION || mBolderLinePosition == LOWER_BOLDER_LINE_POSITION) {
	        	if (mBolderLinePosition == UPPER_BOLDER_LINE_POSITION) {
	        		startY = 0;        
			        stopY = startY;
	        	}
	        	else {
	        		startY = mHeight - mStrokeWidth;        
			        stopY = startY;
	        	}
	        		
	        	canvas.drawLine(startX, startY, stopX, stopY, mPaint);
	        }
	        else {
	        	startY = 0;        
		        stopY = startY;
	        	canvas.drawLine(startX, startY, stopX, stopY, mPaint);
	        	
	        	startY = mHeight - mStrokeWidth;        
		        stopY = startY;
	        	canvas.drawLine(startX, startY, stopX, stopY, mPaint);
	        }
		
	        
		}
        
		super.onDraw(canvas);
	}

	
}