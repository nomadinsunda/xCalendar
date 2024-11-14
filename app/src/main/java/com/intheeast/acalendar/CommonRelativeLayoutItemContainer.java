package com.intheeast.acalendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RelativeLayout;

public class CommonRelativeLayoutItemContainer extends RelativeLayout {
	
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
	
	public CommonRelativeLayoutItemContainer(Context context) {
		super(context);
		
		createDisplayMetrics();
		init(context);
	}
	
	public CommonRelativeLayoutItemContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		createDisplayMetrics();
		init(context, attrs, 0);
	}
	
	// 3rd Para : defStyleAttr 
	// ->An attribute in the current theme that contains a reference to a style resource that supplies defaults values for the TypedArray. 
	//   Can be 0 to not look for defaults.
	public CommonRelativeLayoutItemContainer(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		createDisplayMetrics();
		init(context, attrs, defStyle);
	}
	
	public CommonRelativeLayoutItemContainer(Context context, int paddingLeft, int bolderLinePosition) {
		super(context);
		
		createDisplayMetrics();
		init(context, paddingLeft, bolderLinePosition);
	}
	
	
	public void createDisplayMetrics() {		
		mDisplayMetrics = getResources().getDisplayMetrics();
	}
	
	public void init(Context context) {
		mPaddingLeft = 0;
		mBolderLinePosition = NOT_BOLDER_LINE_POSITION;		
		
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

	public void init(Context context, AttributeSet attrs, int defStyle) {		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CustomView, defStyle, 0);		
		mPaddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ta.getInteger( R.styleable.CustomView_paddingLeft, 0), mDisplayMetrics);
		mBolderLinePosition = ta.getInteger( R.styleable.CustomView_bolderLinePosition, -1);	// 	
		
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
		//mPaddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingLeft, mDisplayMetrics);
		mPaddingLeft = paddingLeft;
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
		
		// RelativeLayout 는 할 필요가 없음??? : 있다 안 되는 경우가 있는 것 같다 -> response_menu!!!
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

	String mWho;
	public void setWhoName(String name) {
		mWho = name;
	}
	
	/*
	public static class LayoutParams extends RelativeLayout.LayoutParams {
		int x;
		int y;
		
		public int horizontalSpacing;
		public boolean breakLine;

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			
			
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams);
			try {
				
			} finally {
				a.recycle();
			}
			
		}

		public LayoutParams(int w, int h) {
			super(w, h);
		}
	}
	*/
}
