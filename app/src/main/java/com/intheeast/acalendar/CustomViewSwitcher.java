package com.intheeast.acalendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class CustomViewSwitcher extends LinearLayout {

	View mPreviousView;
	View mCurrentView;
	View mNextView;
	
	public CustomViewSwitcher(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public CustomViewSwitcher(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public CustomViewSwitcher(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	public View getPreviousView() {
		return mPreviousView;
	}
	
	public View getCurrentView() {
		return mCurrentView;
	}
	
	public View getNextView() {
		return mNextView;
	}

}
