package com.intheeast.acalendar;



import com.intheeast.day.DayView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class SelectedEventRect extends View {
	private static String TAG = "SelectedEventRect";    
    private static boolean INFO = true;
	
	// SELECTION_EVENT_PRESSED
	// SELECTION_NEW_EVENT
	int mEventRectType;
	
	private int POINT_TWOFIVE_CENTIMETERS_TO_PIXELS = 0;
    
    private final float POINT_TWOFIVE_CENTIMETERS_TO_INCHES = 0.098425f;
    
    private String mEventTitle;
    private String mEventLocation;
	
	public SelectedEventRect(Context context) {
		super(context);
		init(context);
	}
	
	public SelectedEventRect(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SelectedEventRect(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	int mCalendarGridAreaSelected;
	Paint mPaint;
	public void init(Context context) {
		
		Activity activity = (Activity) context;
		
		DisplayMetrics metrics = new DisplayMetrics();
		
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float mXDpi = metrics.xdpi;
        POINT_TWOFIVE_CENTIMETERS_TO_PIXELS = Math.round(POINT_TWOFIVE_CENTIMETERS_TO_INCHES * mXDpi);
        
		mCalendarGridAreaSelected = context.getResources().getColor(R.color.calendar_grid_area_selected);
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(mCalendarGridAreaSelected);
	}
	
	public void setEventRectType(int type) {
		mEventRectType = type;
	}
	
	float mRadiusExpandCircle;
	public float mRadiusExpandInnerCircle;
	int mTopMargin;
	public void setTopMargin(int margin) {
		mTopMargin = margin;
		mRadiusExpandCircle = mTopMargin;
		mRadiusExpandInnerCircle = mRadiusExpandCircle * 0.75f;
	}
	
	int mBottomMargin;
	public void setBottomMargin(int margin) {
		mBottomMargin = margin;
	}
	
	public int getTopMargin() {
		return mTopMargin;
	}
	
	public int getBottomMargin() {
		return mBottomMargin;
	}
	
	public int getUpperExpandCircleCenterX() {
		return mUpperExpandCircleCenterX;
	}
	
	public int getLowerExpandCircleCenterX() {
		return mLowerExpandCircleCenterX;
	}
	
	public int getUpperExpandCircleCenterY() {
		return mUpperExpandCircleCenterY;
	}
	
	public int getLowerExpandCircleCenterY() {
		return mLowerExpandCircleCenterY;
	}
	
	public Rect getUpperExpandCircleRect() {
		return mUpperExpandCircle;
	}
	
	public Rect getLowerExpandCircleRect() {
		return mLowerExpandCircle;
	}
	
	public int getRadiusExpandCircle() {
		return (int)mRadiusExpandCircle;
	}
	
	public int getRadiusExpandInnerCircle() {
		return (int)mRadiusExpandInnerCircle;
	}
	
	public int getInterpolation() {
		return POINT_TWOFIVE_CENTIMETERS_TO_PIXELS;
	}
	
	
	Paint mEventTitlePaint;
	Paint mEventLocationPaint;
	//int mEventTitleLineHeight;
	//float mEventTitleAscent;
	//float mEventTitleDescent;
	//float mEventLocationDescent;
	//float mEventOneLineTextY;
	//int mTwoLineTotalHeight;
	//float mEventTitleTextWidth;
	//float mSpaceWidth;
	
	float mEventTitle_X = 0;
	float mEventTitle_Y = 0;
	
	float mEventLocation_X = 0;
	float mEventLocation_Y = 0;
	
	
	
	boolean mTwoEventTextLine = true;
	public void setEventTextLines(boolean twoLines) {
		mTwoEventTextLine = twoLines;
	}
	
	public void setEventTitleText(String text) {
		mEventTitle = text;
	}
	
	public void setEventLocationText(String text) {
		mEventLocation = text;
	}
	
	public void setEventRectViewContext(boolean twoLines, String titleText, String LocationText, Paint paint, int eventRectHeight) {
		mTwoEventTextLine = twoLines;
		mEventTitle = titleText;
		mEventLocation = LocationText;	
		
		mEventTitlePaint = new Paint(paint);
		mEventLocationPaint = new Paint(paint);
		
		mEventTitlePaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		mEventLocationPaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));		
		
		float eventTitleAscent = mEventTitlePaint.ascent();
        float eventTitleDescent = mEventTitlePaint.descent();
    	int eventTitleLineHeight = (int) (Math.abs(eventTitleAscent) + Math.abs(eventTitleDescent));    	    	
    	
    	float titleTextFontSize = mEventTitlePaint.getTextSize();
    	float locationFontSize = titleTextFontSize * 0.9f;
    	mEventLocationPaint.setTextSize(locationFontSize);
    	float locationAscent = mEventLocationPaint.ascent();
    	float eventLocationDescent = mEventLocationPaint.descent();
    	int locationLineHeight = (int) (Math.abs(locationAscent) + Math.abs(eventLocationDescent));        	
    	
    	if (mTwoEventTextLine) {	
    		mEventTitle_X = DayView.EVENT_TEXT_LEFT_MARGIN;
    		mEventTitle_Y = mTopMargin + DayView.EVENT_TEXT_TOP_MARGIN + eventTitleLineHeight - eventTitleDescent;
    		
    		mEventLocation_X = DayView.EVENT_TEXT_LEFT_MARGIN;
    		int twoLineTotalHeight = eventTitleLineHeight + DayView.EVENT_TEXT_PAD_BETWEEN_LINES + locationLineHeight; 
	    	mEventLocation_Y = mTopMargin + DayView.EVENT_TEXT_TOP_MARGIN + twoLineTotalHeight - eventLocationDescent;
    	}
    	else {
    		String space = " ";
        	float spaceWidth = mEventTitlePaint.measureText(space);	
        	
    		int topMarginOfEventTitleText = (eventRectHeight - eventTitleLineHeight) / 2; 
        	float eventOneLineTextY = topMarginOfEventTitleText + Math.abs(eventTitleAscent);
        	
    		mEventTitle_X = DayView.EVENT_TEXT_LEFT_MARGIN;
    		mEventTitle_Y = mTopMargin + /*DayView.EVENT_TEXT_TOP_MARGIN + */eventOneLineTextY;
    		
    		float eventTitleTextWidth = mEventTitlePaint.measureText(mEventTitle);
    		
    		mEventLocation_X = DayView.EVENT_TEXT_LEFT_MARGIN + eventTitleTextWidth + spaceWidth;
	    	mEventLocation_Y = mTopMargin + /*DayView.EVENT_TEXT_TOP_MARGIN + */eventOneLineTextY;
    	}
	}
		
	
	int mWidth;
	int mHeight;
	int mUpperExpandCircleCenterX;
	int mUpperExpandCircleCenterY;
	int mLowerExpandCircleCenterX;
	int mLowerExpandCircleCenterY;
	
	Rect mUpperExpandCircle = new Rect();
	Rect mLowerExpandCircle = new Rect();
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {    	
    	mWidth = w;
		mHeight = h;
				
		mUpperExpandCircleCenterX = POINT_TWOFIVE_CENTIMETERS_TO_PIXELS; //mUpperExpandCircleCenterX = (int) (mWidth * 0.9f);
		mUpperExpandCircleCenterY = mTopMargin;
		mLowerExpandCircleCenterX = mWidth - POINT_TWOFIVE_CENTIMETERS_TO_PIXELS; //mLowerExpandCircleCenterX = (int) (mWidth * 0.1f);
		mLowerExpandCircleCenterY = mHeight - mBottomMargin;
		
		mUpperExpandCircle.left = (int) (mUpperExpandCircleCenterX - mRadiusExpandCircle);
		mUpperExpandCircle.top = (int) (mUpperExpandCircleCenterY - mRadiusExpandCircle);
		mUpperExpandCircle.right = (int) (mUpperExpandCircleCenterX + mRadiusExpandCircle);
		mUpperExpandCircle.bottom = (int) (mUpperExpandCircleCenterY + mRadiusExpandCircle);
		
		mLowerExpandCircle.left = (int) (mLowerExpandCircleCenterX - mRadiusExpandCircle);
		mLowerExpandCircle.top = (int) (mLowerExpandCircleCenterY - mRadiusExpandCircle);
		mLowerExpandCircle.right = (int) (mLowerExpandCircleCenterX + mRadiusExpandCircle);
		mLowerExpandCircle.bottom = (int) (mLowerExpandCircleCenterY + mRadiusExpandCircle);
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
		
	
	@Override
	protected void onDraw(Canvas canvas) {
		mPaint.setColor(mCalendarGridAreaSelected);
		
		if (mEventRectType == DayView.SELECTION_NEW_EVENT) {
			Rect r = new Rect();
			r.left = 0;
			r.top = 0;
			r.right = mWidth;
			r.bottom = mHeight;
			
			canvas.drawRect(r, mPaint);     
		}
		else {
			Rect r = new Rect();
			r.left = 0;
			r.top = mTopMargin;
			r.right = mWidth;
			r.bottom = mHeight - mBottomMargin;			
			
			canvas.drawRect(r, mPaint); 
			
			// ��� circle
			canvas.drawCircle(mUpperExpandCircleCenterX, mUpperExpandCircleCenterY, mRadiusExpandCircle, mPaint);
			
			mPaint.setColor(Color.WHITE);
			canvas.drawCircle(mUpperExpandCircleCenterX, mUpperExpandCircleCenterY, mRadiusExpandInnerCircle, mPaint); // inner cirlce
			
			// �ϴ� cicle
			mPaint.setColor(mCalendarGridAreaSelected);
			canvas.drawCircle(mLowerExpandCircleCenterX, mLowerExpandCircleCenterY, mRadiusExpandCircle, mPaint);
			
			mPaint.setColor(Color.WHITE);
			canvas.drawCircle(mLowerExpandCircleCenterX, mLowerExpandCircleCenterY, mRadiusExpandInnerCircle, mPaint); // inner cirlce
				    		    	
			if (mTwoEventTextLine) {				
				canvas.drawText(mEventTitle, mEventTitle_X, mEventTitle_Y, mEventTitlePaint);
				canvas.drawText(mEventLocation, mEventLocation_X, mEventLocation_Y, mEventLocationPaint);
			}
			else {			
	        	
		        canvas.drawText(mEventTitle, mEventTitle_X, mEventTitle_Y, mEventTitlePaint);  		        	        
		        
		        if (!mEventLocation.isEmpty()) {		        		        
		        
		        	canvas.drawText(mEventLocation, mEventLocation_X, mEventLocation_Y, mEventLocationPaint);	        	
		        }
			}
		}
		//super.onDraw(canvas);
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mEventRectType == DayView.SELECTION_NEW_EVENT)
			return false;
		
		int action = ev.getAction();       

		int x = (int) ev.getX();
        int y = (int) ev.getY();  
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            	int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            	//xxx = ev.getActionMasked();
            	int id = ev.getActionIndex();
            	int pointerId = ev.getPointerId(id);
            	            	
            	if (INFO) Log.i(TAG, "ACTION_DOWN:" + String.valueOf(System.currentTimeMillis()));
            	
            	if (mUpperExpandCircle.contains(x, y)) {
            		if (INFO) Log.i(TAG, "ACTION_DOWN:mUpperExpandCircle");   
            		return true;
            	}
            	
            	if (mLowerExpandCircle.contains(x, y)) {
            		if (INFO) Log.i(TAG, "ACTION_DOWN:mLowerExpandCircle");   
            		return true;
            	}
            	
                return false;

            case MotionEvent.ACTION_MOVE:
            	//if (INFO) Log.i(TAG, "ACTION_MOVE");          
            	
                return false;

            case MotionEvent.ACTION_UP:       
            	if (INFO) Log.i(TAG, "ACTION_UP");   
            	
                return false;

                // This case isn't expected to happen.
            case MotionEvent.ACTION_CANCEL:
                //if (INFO) Log.i(TAG, "ACTION_CANCEL");                
                return false;

            default:                
                return false;
        }
    
	}
	*/
	

}
