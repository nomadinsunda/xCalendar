package com.intheeast.day;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class AllDayEventListView extends ListView {

	int mWidth;
	int mHeight;
	
	public AllDayEventListView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public AllDayEventListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	
	public AllDayEventListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	/*
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(ev);
	}
	*/
}
