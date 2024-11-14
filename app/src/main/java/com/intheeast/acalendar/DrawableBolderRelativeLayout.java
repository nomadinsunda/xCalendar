package com.intheeast.acalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class DrawableBolderRelativeLayout extends RelativeLayout {

	public static final int UPPER_LINE_SIDE = 1;
	public static final int UNDER_LINE_SIDE = 2;
	int mLineSide = UNDER_LINE_SIDE;
	
	int mWidth;
	int mHeight;
	
	Paint mPaint;
	float mStrokeWidth;
	int mPaddingLeft;
	
	public DrawableBolderRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		//eventViewItemUnderLineColor
		mPaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
		
		mPaint.setStyle(Style.STROKE);
		// <dimen name="eventItemLayoutUnderLineHeight">1dp</dimen>
		//The stroke width is defined in pixels
		mStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); 		
		mPaddingLeft = getPaddingLeft();		
	}
	
	// 어떤 상황에서 호출되는가???
	public DrawableBolderRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		//eventViewItemUnderLineColor
		mPaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));	
		
		mPaint.setStyle(Style.STROKE);
		// <dimen name="eventItemLayoutUnderLineHeight">1dp</dimen>
		//The stroke width is defined in pixels
		mStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); // 기존의 언더라인뷰의 언더라인보다 더 얇게 그려진다
		mPaddingLeft = getPaddingLeft();
	}	
	
	
	public void setLineSide(int lineSide) {
		mLineSide = lineSide;		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i("tag", "DrawableBolderRelativeLayout:onDraw");
		// TODO Auto-generated method stub        
		//<dimen name="event_item_layout_paddingleft">12dip</dimen>
		//<item name="android:paddingLeft">@dimen/event_item_layout_paddingleft</item>
        //float startX = getResources().getDimension(R.dimen.event_item_layout_paddingleft);
				
		float startX = 0;
        float startY = 0;
        float stopX = 0;
        float stopY = 0;
        
		if (mLineSide == UNDER_LINE_SIDE) {
			startX = mPaddingLeft;
	        startY = mHeight - mStrokeWidth;
	        stopX = mWidth;
	        stopY = startY;
		}
		else {
			startX = mPaddingLeft;
	        startY = 0;
	        stopX = mWidth;
	        stopY = startY;
		}
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);
        
		super.onDraw(canvas);
	}

	boolean mSetWillDraw = false;
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		Log.i("tag", "DrawableBolderRelativeLayout:onLayout");		
		mWidth = right - left;
		mHeight = bottom - top;	
		
		// RelativeLayout 는 할 필요가 없음??? : 있다 안 되는 경우가 있는 것 같다 -> response_menu!!!
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false);
		}
		//invalidate();		
	}

	
	
}
