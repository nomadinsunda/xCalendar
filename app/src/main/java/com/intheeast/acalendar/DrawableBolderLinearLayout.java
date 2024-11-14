package com.intheeast.acalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class DrawableBolderLinearLayout extends LinearLayout {

	Paint mPaint;
	float mStrokeWidth;
	
	int mWidth;
	int mHeight;
	int mPaddingLeft;
	
	public DrawableBolderLinearLayout(Context context) {
		super(context);
		
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
	
	public DrawableBolderLinearLayout(Context context, AttributeSet attrs) {
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

	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i("tag", "DrawableBolderLinearLayout:onDraw");
		// TODO Auto-generated method stub       
		
        //float startX = getResources().getDimension(R.dimen.event_item_layout_paddingleft);
		float startX = mPaddingLeft;
        float startY = mHeight - mStrokeWidth; // y ���� �� ��������� �h��...�� �׷��� �翬�� ©����
        float stopX = mWidth;
        float stopY = startY;
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);
        
		super.onDraw(canvas);
	}

	boolean mSetWillDraw = false;
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		//Log.i("tag", "DrawableBolderLinearLayout:onLayout");
		
		mWidth = right - left;
		mHeight = bottom - top;
		
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false); //LinearLayout�� ����� �Ѵ�
		}
		
		//invalidate(); // setWillNotDraw ȣ��� ���� ȣ���� �ʿ䰡 ����
	}		

}
