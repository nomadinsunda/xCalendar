package com.intheeast.acalendar;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class CalendarViewsLowerActionBar extends RelativeLayout {	
	
	Context mContext;
	Fragment mFragment;
	CalendarController mController;
	
	TextView mTodayTextView;
	TextView mCalendarsView;
	TextView mInboxView;
	
	Paint mPaint;
	float mStrokeWidth;
	int mPaddingLeft;
	
	int mWidth;
	int mHeight;	
    
	public CalendarViewsLowerActionBar(Context context) {
		super(context);
		mContext = context;
		//init(context);
	}
	
	public CalendarViewsLowerActionBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		//init(context);
	}
	
	public CalendarViewsLowerActionBar(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		//init(context);
	}
	
	
	public void init(Fragment fragment, String timezone, CalendarController controller, boolean psudoActionBar) {	
		mFragment = fragment;
		mTimeZone = timezone;		
		mController = controller;
		
		if (!psudoActionBar) {
			mTodayTextView = (TextView) findViewById(R.id.today_textview);
			mCalendarsView = (TextView) findViewById(R.id.calendar_textview);
			mInboxView = (TextView) findViewById(R.id.inbox_textview);
			
			mTodayTextView.setOnClickListener(mTodayClickListener);		
			mCalendarsView.setOnClickListener(mCalendarClickListener);
		}
		
    	mPaint = new Paint();
		mPaint.setAntiAlias(true);		
		mPaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));			
		mPaint.setStyle(Style.STROKE);		
		mStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
		mPaint.setStrokeWidth(mStrokeWidth); 		
		mPaddingLeft = 0;
	}	
	
	
	String mTimeZone;
	OnClickListener mTodayClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
	        int viewType = ViewType.CURRENT;
	                                
            Calendar t = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
            t.setTimeInMillis(System.currentTimeMillis());
            long extras = CalendarController.EXTRA_GOTO_TODAY;
            
            mController.sendEvent(CalendarViewsLowerActionBar.this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
		}		
	};
	
	
	OnClickListener mCalendarClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
			CentralActivity activity = (CentralActivity) mFragment.getActivity();
			
			View contentView = activity.getContentView();
			Utils.makeContentViewBitmap(activity, contentView);    
			
            mController.sendEvent(CalendarViewsLowerActionBar.this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                    0, 0);
		}		
	};
	
	boolean mSetWillDraw = false;
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	
    	mWidth = w;
		mHeight = h;
		
		//Log.i("tag", mWho + " : CalendarViewsLowerActionBar:onSizeChanged:HEIGHT=" + String.valueOf(mHeight));	
		
		// RelativeLayout 는 할 필요가 없음??? : 있다 안 되는 경우가 있는 것 같다 -> response_menu!!!
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false);
		}
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	/*
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);		
		
		mWidth = right - left;
		mHeight = bottom - top;	
		
		Log.i("tag", mWho + " : CalendarViewsLowerActionBar:onLayout:HEIGHT=" + String.valueOf(mHeight));		
		
		// RelativeLayout 는 할 필요가 없음??? : 있다 안 되는 경우가 있는 것 같다 -> response_menu!!!
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false);
		}	
	}
	*/
	
	@Override
	protected void onDraw(Canvas canvas) {
		
        float startX = mPaddingLeft;
        float startY = 0;
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
