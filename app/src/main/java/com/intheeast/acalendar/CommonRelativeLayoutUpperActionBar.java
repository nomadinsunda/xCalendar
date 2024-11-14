package com.intheeast.acalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class CommonRelativeLayoutUpperActionBar extends RelativeLayout {

	Paint mPaint;
	float mStrokeWidth;
	int mPaddingLeft;
	
	int mWidth;
	int mHeight;
	
	public CommonRelativeLayoutUpperActionBar(Context context) {
		super(context);
		
		init(context);
	}
	
	public CommonRelativeLayoutUpperActionBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init(context);
	}
	
	public CommonRelativeLayoutUpperActionBar(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		init(context);
	}
	
	public void init(Context context) {		
	    
    	mPaint = new Paint();
		mPaint.setAntiAlias(true);
		
		mPaint.setColor(getResources().getColor(R.color.selectCalendarViewSeperatorOutLineColor));
		//mPaint.setColor(Color.BLACK);
		
		mPaint.setStyle(Style.STROKE);
		// <dimen name="eventItemLayoutUnderLineHeight">1dp</dimen>
		//The stroke width is defined in pixels
		mStrokeWidth = getResources().getDimension(R.dimen.selectCalendarViewSeperatorOutLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); 		
		mPaddingLeft = 0;
	}
	
	boolean mSetWillDraw = false;
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	
    	mWidth = w;
		mHeight = h;
		
		// RelativeLayout 는 할 필요가 없음??? : 있다 안 되는 경우가 있는 것 같다 -> response_menu!!!
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false);
		}
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		
        float startX = mPaddingLeft;
        float startY = mHeight - mStrokeWidth;
        float stopX = mWidth;
        float stopY = startY;
		
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);
        
		super.onDraw(canvas);
	}

	String mWho;
	public void setWhoName(String name) {
		mWho = name;
	}
}
