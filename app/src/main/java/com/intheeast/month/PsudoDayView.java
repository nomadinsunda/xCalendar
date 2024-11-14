/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intheeast.month;


import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import android.provider.CalendarContract.Attendees;

import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.EllipsizeCallback;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import android.widget.LinearLayout;

import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarData;
import com.intheeast.acalendar.Event;
import com.intheeast.day.EventGeometry;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * View for multi-day view. So far only 1 and 7 day have been tested.
 */
public class PsudoDayView extends LinearLayout {
    private static String TAG = "PsudoDayView";    
    private static boolean INFO = true;      

    private static float mScale = 0; // Used for supporting different screen densities  
   
    public static int DEFAULT_CELL_HEIGHT = 64;   
    
    /**
     * ID of the last event which was displayed with the toast popup.
     *
     * This is used to prevent popping up multiple quick views for the same event, especially
     * during calendar syncs. This becomes valid when an event is selected, either by default
     * on starting calendar or by scrolling to an event. It becomes invalid when the user
     * explicitly scrolls to an empty time slot, changes views, or deletes the event.
     */    

    protected Context mContext;
    protected Activity mActivity;    

    // Make this visible within the package for more informative debugging
    public Calendar mBaseDate;
    private Calendar mCurrentTime;
    //Update the current time line every five minutes if the window is left open that long
    
    
    private int mTodayJulianDay;

    private final Typeface mBold = Typeface.DEFAULT_BOLD;
    public int mFirstJulianDay;    
    private int mLastJulianDay;
    
    private int mFirstVisibleDate;
    private int mFirstVisibleDayOfWeek;
    private int[] mEarliestStartHour;    // indexed by the week day offset   
       

    private int mEventsAlpha = 255;
    private ObjectAnimator mEventsCrossFadeAnimation;

    protected static StringBuilder mStringBuilder = new StringBuilder(50);
    // TODO recreate formatter when locale changes
    protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());    

    private ArrayList<Event> mEvents = new ArrayList<Event>();
    
    private int mSelectionDay;        // Julian day
    private int mSelectionStartHour;
    private int mSelectionStartMinutes;  


    /** Width of a day or non-conflicting event */
    private int mCellWidth;

    // Pre-allocate these objects and re-use them
    private final Rect mRect = new Rect();
    private final Rect mDestRect = new Rect();
    private final Rect mSelectionRect = new Rect();
    // This encloses the more allDay events icon
    //private final Rect mExpandAllDayRect = new Rect();
    // TODO Clean up paint usage
    private final Paint mPaint = new Paint();
    private final Paint mEventTextPaint = new Paint();
    //private final Paint mSelectionPaint = new Paint();
    private float[] mLines;

    private int mFirstDayOfWeek; // First day of the week
    
    public boolean mRemeasure = false;   
    
    protected final EventGeometry mEventGeometry;

    private static int GRID_LINE_TOP_MARGIN = 0;
    private static int GRID_LINE_BOTTOM_MARGIN = 0;
    private static float GRID_LINE_LEFT_MARGIN = 0;
    
    private static final float GRID_LINE_INNER_WIDTH = 0;
    
    public static final int DAY_GAP = 1;
    public static final int HOUR_GAP = 1;    
    
    private static int HOURS_TOP_MARGIN = 2;
    
    public static int NEW_HOURS_LEFT_MARGIN = 8;
    
    public static int NEW_HOURS_RIGHT_MARGIN = 8;     
    

    private static int CURRENT_TIME_LINE_SIDE_BUFFER = 4;
    private static int CURRENT_TIME_LINE_TOP_OFFSET = 2;

    /* package */ static final int MINUTES_PER_HOUR = 60;
    /* package */ static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * 24;
    /* package */ static final int MILLIS_PER_MINUTE = 60 * 1000;
    /* package */ static final int MILLIS_PER_HOUR = (3600 * 1000);
    /* package */ static final int MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;   
    
    
    private static float EVENT_TEXT_FONT_SIZE = 12;
    private static float HOURS_TEXT_SIZE = 12;
    public static float AMPM_TEXT_SIZE = 9;
    
    
    private static final int MAX_EVENT_TEXT_LEN = 500;
    // smallest height to draw an event with
    private static float MIN_EVENT_HEIGHT = 24.0F; // in pixels
    
    private static int EVENT_RECT_TOP_MARGIN = 1;
    private static int EVENT_RECT_BOTTOM_MARGIN = 0;
    private static int EVENT_RECT_LEFT_MARGIN = 1;
    
    private static int EVENT_RECT_STROKE_WIDTH = 2;
    public static int EVENT_TEXT_TOP_MARGIN = 2;
    private static int EVENT_TEXT_BOTTOM_MARGIN = 2;
    public static int EVENT_TEXT_PAD_BETWEEN_LINES = 2;
    public static int EVENT_TEXT_LEFT_MARGIN = 6;
    private static int EVENT_TEXT_RIGHT_MARGIN = 6;    
    
    private static int mEventTextColor;
    
    private static int mCalendarAmPmLabel;
    private static int mCalendarGridAreaSelected;
    private static int mCalendarGridLineInnerHorizontalColor;
    private static int mCalendarGridLineInnerVerticalColor;
    
    private static int mCalendarHourLabelColor;
    
    public int mViewStartX;
    public int mViewStartY;
    private int mMaxViewStartY;
    private int mViewHeight;
    private int mViewWidth;
    private int mGridAreaHeight = -1;
    private static int mCellHeight = 0; // shared among all DayViews
    
    private static int mOneQuartersOfHourPixelHeight = 0;   
   
    private static int mMinCellHeight = 32;
    public int mScrollStartY;
    
    /**
     * Flag to decide whether to handle the up event. Cases where up events
     * should be ignored are 1) right after a scale gesture and 2) finger was
     * down before app launch
     */
    //private boolean mHandleActionUp = true;

    private int mHoursTextHeight;
    private int mRealHoursTextHeight;
    
    /**
     * The height of the day names/numbers
     */
    private static int DAY_HEADER_HEIGHT = 45;
    /**
     * The height of the day names/numbers for multi-day views
     */
    private static int MULTI_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;
    /**
     * The height of the day names/numbers when viewing a single day
     */
    private static int ONE_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;    
    

    protected int mNumDays = 7;
    private int mNumHours = 10;

    /** Width of the time line (list of hours) to the left. */
    private int mMaxHourDecimalTextWidth;
    private int mHoursWidth;
    private int mAMPMTextHeight;
    
    
    /** Top of the scrollable region i.e. below date labels and all day events */
    private int mFirstCell;
    /** First fully visibile hour */
    private int mFirstHour = -1;
    /** Distance between the mFirstCell and the top of first fully visible hour. */
    private int mFirstHourOffset;
    private String[] mHourStrs;    
    private boolean mIs24HourFormat;
    
    protected final Resources mResources;
    protected final Drawable mCurrentTimeLine;
    protected final Drawable mCurrentTimeAnimateLine;
    
    private String mAmString;
    private String mPmString;

    private final CalendarController mController;      
    
    public PsudoDayView(Activity activity, CalendarController controller, int numDays) {
    	
        super(activity.getApplicationContext());
        mContext = activity.getApplicationContext();
        mActivity = activity;        
        
        setBackgroundColor(Color.argb(255, 255, 255, 255));
        
        mResources = mContext.getResources();
       
        mNumDays = numDays;           
        
        ONE_DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.one_day_header_height);        
        
        HOURS_TEXT_SIZE = (int) mResources.getDimension(R.dimen.hours_text_size);
        AMPM_TEXT_SIZE = (int) mResources.getDimension(R.dimen.ampm_text_size);
                
        MULTI_DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.day_header_height);
        
        EVENT_TEXT_TOP_MARGIN = (int) mResources.getDimension(R.dimen.event_text_vertical_margin);
        EVENT_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
        EVENT_TEXT_PAD_BETWEEN_LINES = EVENT_TEXT_TOP_MARGIN;
        
        EVENT_TEXT_LEFT_MARGIN = (int) mResources
                .getDimension(R.dimen.event_text_horizontal_margin);
        EVENT_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        
        if (mScale == 0) {
            mScale = mResources.getDisplayMetrics().density;
            if (mScale != 1) {                
                
                GRID_LINE_LEFT_MARGIN *= mScale;
                HOURS_TOP_MARGIN *= mScale;
                                
                CURRENT_TIME_LINE_SIDE_BUFFER *= mScale;
                CURRENT_TIME_LINE_TOP_OFFSET *= mScale;
                
                DEFAULT_CELL_HEIGHT *= mScale;
                DAY_HEADER_HEIGHT *= mScale;                
                
                EVENT_RECT_TOP_MARGIN *= mScale;
                EVENT_RECT_BOTTOM_MARGIN *= mScale;
                
                EVENT_RECT_LEFT_MARGIN *= mScale;
                
                EVENT_RECT_STROKE_WIDTH *= mScale;                
            }
        }
                
        DAY_HEADER_HEIGHT = mNumDays == 1 ? ONE_DAY_HEADER_HEIGHT : MULTI_DAY_HEADER_HEIGHT;
        
        DisplayMetrics metrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        mCurrentTimeLine = mResources.getDrawable(R.drawable.timeline_indicator_holo_light);
        mCurrentTimeAnimateLine = mResources
                .getDrawable(R.drawable.timeline_indicator_activated_holo_light);        

        mEventGeometry = new EventGeometry();
        mEventGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
        mEventGeometry.setHourGap(HOUR_GAP);
        mEventGeometry.setCellMargin(DAY_GAP);        
        
        mController = controller;        
        
        if (mCellHeight == 0) {
            mCellHeight = Utils.getSharedPreference(mContext,
                    SettingsFragment.KEY_DEFAULT_CELL_HEIGHT, DEFAULT_CELL_HEIGHT);            
            
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            float deviceHeight = getResources().getDisplayMetrics().heightPixels;                   	
        	float StatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        	float appHeight = (deviceHeight - StatusBarHeight);
        	float cellHeight = appHeight * 0.087f;
        	mCellHeight = (int) cellHeight; //mCellHeight = 107;//for test!!!            
            
            //KEY_DEFAULT_EVENT_FONT_SIZE�� ���Ŀ� �����ؾ� �Ѵ�!!!
            
        }
        else {
        	
        }
        
        mOneQuartersOfHourPixelHeight = mCellHeight / 4;        
        
        MIN_EVENT_HEIGHT = mOneQuartersOfHourPixelHeight;
        
        makeEventRectFontInfo(mCellHeight);             
        
        init();          
    }
    
    TimeZone mTimeZone;
    long mGMToff;
    @SuppressWarnings("deprecation")
	private void init() {   
        
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mTimeZone = TimeZone.getTimeZone(Utils.getTimeZone(mContext, null));
        mGMToff = (mTimeZone.getRawOffset()) / 1000;
        mCurrentTime = GregorianCalendar.getInstance(mTimeZone);//new Time(Utils.getTimeZone(mContext, mTZUpdater));
        mCurrentTime.setFirstDayOfWeek(mFirstDayOfWeek);
        long currentTime = System.currentTimeMillis();
        mCurrentTime.setTimeInMillis(currentTime);
        mTodayJulianDay = ETime.getJulianDay(currentTime, mTimeZone, mFirstDayOfWeek);
        
        mCalendarAmPmLabel = mResources.getColor(R.color.calendar_ampm_label);
        mCalendarGridAreaSelected = mResources.getColor(R.color.calendar_grid_area_selected);
        mCalendarGridLineInnerHorizontalColor = mResources
                .getColor(R.color.calendar_grid_line_inner_horizontal_color);
        mCalendarGridLineInnerVerticalColor = mResources
                .getColor(R.color.calendar_grid_line_inner_vertical_color);
        mCalendarHourLabelColor = mResources.getColor(R.color.calendar_hour_label);
        
        mEventTextColor = mResources.getColor(R.color.calendar_event_text_color);
        
        mEventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
        mEventTextPaint.setTextAlign(Paint.Align.LEFT);
        mEventTextPaint.setAntiAlias(true);        

        Paint p = mPaint;
        p.setAntiAlias(true);
        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(null);
        
        handleOnResume();        
        mMaxHourDecimalTextWidth = computeMaxStringWidth(0, mHourStrs, p);
        
        mHoursTextHeight = (int)Math.abs(p.ascent());    
        int hourDescent = (int) (p.descent() + 0.5f);
        mRealHoursTextHeight = mHoursTextHeight + hourDescent;

        mAmString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
        mPmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
        
        String space = " ";
        p.setTextSize(AMPM_TEXT_SIZE);
        String[] fullAmPm = {
        		mAmString + space + "12:59",
        		mPmString + space + "11:59"
        };
        int maxHourStringWidth = computeMaxStringWidth(0, fullAmPm, p);        
        mAMPMTextHeight = (int)(Math.abs(p.ascent()) + p.descent());
        
        mHoursWidth = NEW_HOURS_LEFT_MARGIN + maxHourStringWidth + NEW_HOURS_RIGHT_MARGIN;
        // GRID_LINE_LEFT_MARGIN�� 
        // ���� �ð��� ��ü ���̷� �����
        // :ex) ���� 12:59
        GRID_LINE_LEFT_MARGIN = mHoursWidth; // +added by intheeast        
        GRID_LINE_TOP_MARGIN = (HOUR_GAP + HOURS_TOP_MARGIN) + ( mRealHoursTextHeight / 2);
        GRID_LINE_BOTTOM_MARGIN = GRID_LINE_TOP_MARGIN;
        
        mBaseDate = GregorianCalendar.getInstance(mTimeZone);
        mBaseDate.setFirstDayOfWeek(mFirstDayOfWeek);
        long millis = System.currentTimeMillis();
        mBaseDate.setTimeInMillis(millis);

        mEarliestStartHour = new int[mNumDays];
        //mHasAllDayEvent = new boolean[mNumDays];

        // mLines is the array of points used with Canvas.drawLines() in
        // drawGridBackground() and drawAllDayEvents().  Its size depends
        // on the max number of lines that can ever be drawn by any single
        // drawLines() call in either of those methods.
        final int maxGridLines = (24 + 1)  // max horizontal lines we might draw
                + (mNumDays + 1); // max vertical lines we might draw
        mLines = new float[maxGridLines * 4];        
    }
    
    public final int KELEGANT_ASCENT_VALUE = 1900;
    public final int KELEGANT_DESCENT_VALUE = -500;		
    //public final float DEFAULT_EVENT_TEXT_HEIGHT_RATIO = 0.25f; // 0.20 �ۿ� ������ �ʴ´�...
    public final float DEFAULT_EVENT_TEXT_HEIGHT_RATIO = 0.3f; // �������� cell height�� text height ������ ���� ����� ����...    
    int mAvailEventRectHeightByDefaultEventTextHeight; // Event title�� location�� one line�� ������ �� ����
    int mMinEventRectHeightTwoLinesEventText;
    //float MIN_EVENT_HEIGHT_FONT_SIZE;
    public void makeEventRectFontInfo(float cellHeight) {
    	EVENT_TEXT_FONT_SIZE = getEventFontSize(cellHeight, DEFAULT_EVENT_TEXT_HEIGHT_RATIO);
    	
    	Paint testPaint = new Paint();
    	testPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    	testPaint.setTextAlign(Paint.Align.LEFT);
    	testPaint.setAntiAlias(true);
    	
    	testPaint.setTextSize(EVENT_TEXT_FONT_SIZE);  		
		
		float eventTitleAscent = testPaint.ascent();
		float eventTitleDescent = testPaint.descent();
		int eventTitleLineHeight = Math.round(Math.abs(eventTitleAscent) + Math.abs(eventTitleDescent));
    	
		float locationFontSize = EVENT_TEXT_FONT_SIZE * 0.9f;
		testPaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		testPaint.setTextSize(locationFontSize);
    	float eventLocationAscent = testPaint.ascent();
    	float eventLocationDescent = testPaint.descent();
    	int eventLocationLineHeight = Math.round(Math.abs(eventLocationAscent) + Math.abs(eventLocationDescent));  
    	
    	// ���� mDefaultEventTextHeight�� ������ which event rect height ���� cover�� �Ǵ����� ��������..
    	// event height 
    	// mDefaultEventTextHeight�� ������ ��� ������ Event Height�� [mDefaultEventTextHeight * 2] �̴�
		mAvailEventRectHeightByDefaultEventTextHeight = eventTitleLineHeight * 2;
    	// FROM mAvailDefaultEventTextHeightEventHeight TO MIN_EVENT_HEIGHT �� ��� �� ���ΰ�?    	
    	
    	mMinEventRectHeightTwoLinesEventText = EVENT_TEXT_TOP_MARGIN + 
    			eventTitleLineHeight +
    			EVENT_TEXT_PAD_BETWEEN_LINES +
    			eventLocationLineHeight + 
    			EVENT_TEXT_BOTTOM_MARGIN;       	
    }
    
    public float getEventFontSize(float eventHeight, float ratio) {
    	
    	float targetEventTextHeight = eventHeight * ratio;
    	float roundTargetEventTextHeight = (int) Math.round(targetEventTextHeight);
    	    	
    	float eventFontSize = (roundTargetEventTextHeight * 2048.0f) / (float)(Math.abs(KELEGANT_ASCENT_VALUE) + Math.abs(KELEGANT_DESCENT_VALUE));
    	    	
    	return eventFontSize;    	
    }
    
    public float getEventFontSize(int startFontSize, float eventHeight, float ratio) {
    	//ratio = ratio + 0.1f; // ������ �𸣰����� 0.1�� �����ϸ�...
    	float eventFontSize = 0;
    	float targetDefaultEventTextHeightF = eventHeight * ratio;
    	int targetDefaultEventTextHeight = (int) Math.round(targetDefaultEventTextHeightF);
    	Paint testPaint = new Paint();
    	testPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    	testPaint.setTextAlign(Paint.Align.LEFT);
    	testPaint.setAntiAlias(true);   		
    	
    	int prvEventFontSize = 0;
    	int prvCalcDefaultEventTextHeight = 0;
    	int fontSize = startFontSize;   
    	while(true) {
    		testPaint.setTextSize(fontSize);    		
    		
    		float Ascent = -fontSize * KELEGANT_ASCENT_VALUE / 2048;//float Ascent = testPaint.ascent();
    		float Descent = -fontSize * KELEGANT_DESCENT_VALUE / 2048;//float Descent = testPaint.descent();
	    	int curCalcedEventTextHeight = (int) (Math.abs(Ascent) + Math.abs(Descent));     	
	    	
	    	if (curCalcedEventTextHeight > targetDefaultEventTextHeight) {
	    		// ���⼭ i�� prvFontSize �� �� ���� �� targetDefaultEventTextHeight�� ���������� üũ�ؾ� �Ѵ�
	    		int x1 = curCalcedEventTextHeight - targetDefaultEventTextHeight;
	    		int x2 = targetDefaultEventTextHeight - prvCalcDefaultEventTextHeight;
	    		if (x1 > x2) { // prvFontSize�� prvCalcDefaultEventTextHeight ���� targetDefaultEventTextHeight�� �� �����ߴٴ� ���� �ǹ���
	    			eventFontSize = prvEventFontSize;	    			
	    		}
	    		else if (x2 > x1) {
	    			eventFontSize = fontSize;	    			
	    		}
	    		else {	    			
	    			eventFontSize = fontSize;	    			
	    		}   		
	    		
	    		break;
	    	}
	    	else if (curCalcedEventTextHeight == targetDefaultEventTextHeight) {	    		
	    		eventFontSize = fontSize;
	    		break;
	    	}
	    	
	    	prvEventFontSize = fontSize;
	    	prvCalcDefaultEventTextHeight = curCalcedEventTextHeight;
	    	fontSize--;
    	}
    	
    	return eventFontSize;
    }
    
    
    public int getHoursWidth() {
    	return mHoursWidth;
    }

    public int getCalendarGridAreaSelected() {
    	return mCalendarGridAreaSelected;
    }
    
    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
    }

    public void handleOnResume() {        
        mIs24HourFormat = DateFormat.is24HourFormat(mContext);
        mHourStrs = mIs24HourFormat ? CalendarData.s24Hours : CalendarData.s12HoursNoAmPm;
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);        
    }    

    int getFirstVisibleHour() {
        return mFirstHour;
    }
    
    int getFirstVisibleHourOffset() {        
        return mFirstHourOffset;
    }
    
    void setFirstVisibleHour(int firstHour) {
        mFirstHour = firstHour;        
    }
    
    void setFirstVisibleHourOffset(int firstHourOffset) {        
        mFirstHourOffset = firstHourOffset;
    }
    
    // ���⼭ upper secondary actionbar �� ��������
    //private String mTimeZone; // +added by intheeast
    public void initBaseDateTime(Calendar time, boolean ignoreTime, boolean animateToday) {
        mBaseDate.setTimeInMillis(time.getTimeInMillis());
        
        mFirstHour = mBaseDate.get(Calendar.HOUR_OF_DAY) - mNumHours / 5;
        if (mFirstHour < 0) {
            mFirstHour = 0;
        } else if (mFirstHour + mNumHours > 24) {
            mFirstHour = 24 - mNumHours;
        }
        
        mFirstHourOffset = 0;                
                        
        int gotoY = Integer.MIN_VALUE;

        if (!ignoreTime && mGridAreaHeight != -1) {
            int lastHour = 0;

            if (mBaseDate.get(Calendar.HOUR_OF_DAY) < mFirstHour) {
                // Above visible region
                gotoY = mBaseDate.get(Calendar.HOUR_OF_DAY) * (mCellHeight + HOUR_GAP);
            } else {
                lastHour = (mGridAreaHeight - mFirstHourOffset) / (mCellHeight + HOUR_GAP)
                        + mFirstHour;

                if (mBaseDate.get(Calendar.HOUR_OF_DAY) >= lastHour) {
                    // Below visible region

                    // target hour + 1 (to give it room to see the event) -
                    // grid height (to get the y of the top of the visible
                    // region)
                    gotoY = (int) ((mBaseDate.get(Calendar.HOUR_OF_DAY) + 1 + mBaseDate.get(Calendar.MINUTE) / 60.0f)
                            * (mCellHeight + HOUR_GAP) - mGridAreaHeight);
                }
            }            

            if (gotoY > mMaxViewStartY) {
                gotoY = mMaxViewStartY;
            } else if (gotoY < 0 && gotoY != Integer.MIN_VALUE) {
                gotoY = 0;
            }
        }

        recalc();////////////////////////////////////////////////////////////////////////////////////////////////////////       
    }
    
    public void setBaseTime(Calendar time) {
    	mBaseDate.setTimeInMillis(time.getTimeInMillis());//(time);   	
    	    	
    	recalc();////////////////////////////////    		
    }    
        
    public int getViewStartY() {
    	return mViewStartY;
    }
    
    
    public void updateTitle() {
        Calendar start = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
        start.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
        start.setTimeInMillis(mBaseDate.getTimeInMillis());
        
        Calendar end = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
        end.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
        end.setTimeInMillis(start.getTimeInMillis());
        end.add(Calendar.DAY_OF_MONTH, (mNumDays - 1));
        end.add(Calendar.MINUTE, 1);// Move it forward one minute so the formatter doesn't lose a day
        

        long formatFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
        if (mNumDays != 1) {
            // Don't show day of the month if for multi-day view
            formatFlags |= DateUtils.FORMAT_NO_MONTH_DAY;

            // Abbreviate the month if showing multiple months
            if (start.get(Calendar.MONTH) != end.get(Calendar.MONTH)) {
                formatFlags |= DateUtils.FORMAT_ABBREV_MONTH;
            }
        }

        mController.sendEvent(this, EventType.UPDATE_TITLE, start, end, null, -1, ViewType.CURRENT,
                formatFlags, null, null);
    }
        
    /**
     * return a negative number if "time" is comes before the visible time
     * range, a positive number if "time" is after the visible time range, and 0
     * if it is in the visible time range.
     */
    public int compareToVisibleTimeRange(Calendar time) {

        int savedHour = mBaseDate.get(Calendar.HOUR_OF_DAY);//mBaseDate.hour;
        int savedMinute = mBaseDate.get(Calendar.MINUTE);//mBaseDate.minute;
        int savedSec = mBaseDate.get(Calendar.SECOND);//mBaseDate.second;

        mBaseDate.set(Calendar.HOUR_OF_DAY, 0);//hour = 0;
        mBaseDate.set(Calendar.MINUTE, 0);//mBaseDate.minute = 0;
        mBaseDate.set(Calendar.SECOND, 0);//mBaseDate.second = 0;

        // Compare beginning of range
        int diff = ETime.compare(time, mBaseDate);
        if (diff > 0) {
            // Compare end of range
        	mBaseDate.add(Calendar.DAY_OF_MONTH, mNumDays); // mBaseDate.monthDay += mNumDays;
            
            diff = ETime.compare(time, mBaseDate);
            
            mBaseDate.add(Calendar.DAY_OF_MONTH, -mNumDays); //mBaseDate.monthDay -= mNumDays;
            
            if (diff < 0) {
                // in visible time
                diff = 0;
            } else if (diff == 0) {
                // Midnight of following day
                diff = 1;
            }
        }
        
        mBaseDate.set(Calendar.HOUR_OF_DAY, savedHour);//hour = 0;
        mBaseDate.set(Calendar.MINUTE, savedMinute);//mBaseDate.minute = 0;
        mBaseDate.set(Calendar.SECOND, savedSec);//mBaseDate.second = 0;
        
        return diff;
    }

    public void recalc() {
        // Set the base date to the beginning of the week if we are displaying
        // 7 days at a time.
        if (mNumDays == 7) {
        	ETime.adjustStartDayInWeek(mBaseDate);
        }

        final long start = mBaseDate.getTimeInMillis();
        mFirstJulianDay = ETime.getJulianDay(start, mTimeZone, mFirstDayOfWeek);
        mLastJulianDay = mFirstJulianDay + mNumDays - 1;
        
        mFirstVisibleDate = mBaseDate.get(Calendar.DAY_OF_MONTH);//mBaseDate.monthDay;
        mFirstVisibleDayOfWeek = mBaseDate.get(Calendar.DAY_OF_WEEK);//mBaseDate.weekDay;
    }

    public int getFirstDayOfWeek() {
    	return mFirstDayOfWeek;
    }
    
    public int getFirstJulianDay() {
    	return mFirstJulianDay;
    }
    
    public int getFirstVisibleDate() {
    	return mFirstVisibleDate;
    }
    
    public int getFirstVisibleDayOfWeek() {
    	return mFirstVisibleDayOfWeek;
    }   

    
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    	if (INFO) Log.i(TAG, "onSizeChanged");
    	
        mViewWidth = width;
        mViewHeight = height;
        
        int gridAreaWidth = width - mHoursWidth;
        mCellWidth = (gridAreaWidth - (mNumDays * DAY_GAP)) / mNumDays;        
    }   
    
    
    /**
     * Measures the space needed for various parts of the view after
     * loading new events.  This can change if there are all-day events.
     */    
    //<remeasure>
    //-mNumDays : DayView�� weekly ������ �ƴ� one day ������ ��� 1�̴�
    //-DAY_HEADER_HEIGHT : �츮�� �� ���� �ʿ� �����Ƿ� 0���� ������
    //-mCellHeight : 0��->��� �����Ǵ��� Ȯ�ο��?
    //-mFirstCell : all day event�� �������� ������ DAY_HEADER_HEIGHT[0]�� �����ǰ�
    //              �����Ѵٸ�, DAY_HEADER_HEIGHT + allDayHeight + ALLDAY_TOP_MARGIN �� ������
    //-mGridAreaHeight : onSizeChanged���� ���޵� height ���� mFirstCell�� ������ ���� �����ȴ�
    //-mNumHours : mGridAreaHeight / (mCellHeight + HOUR_GAP) �ε� mCellHeight + HOUR_GAP�� 
    //             ���������� 1 hour�� �����ϴ� height ���� 
    //             -> day view�� ���÷����� �� �ִ� hours�� ������
    //-mMaxViewStartY : ��� ���� �ִ��� �Ʒ��� ��ũ�ѵ� �� �ִ� Y ������ ��
    //#initFirstHour���� �����ϴ� mSelectionStartHour�� ���� ��� �����Ǵ°�? : ���� ���� �´�
    // :�� initFirstHour���� mFirstHour = mSelectionStartHour - mNumHours / 5; �� ���ϴ°�?
    //  �� mSelectionStartHour���� mNumHours / 5 ���� ���ִ°�? �Ѥ�;
    //  �׸��� mNumHours / 5 �� ���� �ǹ��ΰ�?
    //-mViewStartY = mFirstHour * (mCellHeight + HOUR_GAP) - mFirstHourOffset;
    public void remeasure(int width, int height) {
    	    	    	
        // First, clear the array of earliest start times, and the array
        // indicating presence of an all-day event.
        for (int day = 0; day < mNumDays; day++) {
            mEarliestStartHour[day] = 25;  // some big number
            //mHasAllDayEvent[day] = false;
        }      

        // The min is where 24 hours cover the entire visible area
        mMinCellHeight = Math.max((height - DAY_HEADER_HEIGHT) / 24, (int) MIN_EVENT_HEIGHT);
        if (mCellHeight < mMinCellHeight) {
            mCellHeight = mMinCellHeight;
        }

        // Calculate mAllDayHeight
        mFirstCell = DAY_HEADER_HEIGHT;        

        // dayview�� daily ������ ��쿡�� �׸��� ���°� �ƴϴ�
        // �׷��� weekly ������ ��쿡�� ���ڹ��̰� �Ǳ� ������ Grid Area��� ���̹���
        mGridAreaHeight = (int) (height - (mFirstCell));       

        // mNumHours�� �̷��� ����ϴ� ���� �´°�?
        // ���� am 12:00�� ���ʷ� �׷����� �Ѵٸ�,,,
        // GRID_LINE_TOP_MARGIN ������ �ʿ��ϱ� ������...
        // ���� ���� 12:00�� �׷��� �Ѵٸ�..
        // �Ʒ� GRID_LINE_BOTTOM_MARGIN�� �ʿ��ϱ� �����̴�...        
        // �Ʒ��� ������ ���� �ʴ�...
        // ���� �Ʒ� ������ �����ϰ� �ʹٸ�...
        mNumHours = mGridAreaHeight / (mCellHeight + HOUR_GAP);/////////////////////////////////////////////////
        mEventGeometry.setHourHeight(mCellHeight);
        
        // ��ģ ���� ���� ������ �������...
        // ��Ȯ�� ����
        // 60min : 107pixels[cell height] = ?min : 48pixels[min event height]
        // :�� cell height��� ���� 1hour�� height�ε� 1hour�� 60min�̴�
        //  �׷��ϱ� 60min�� 107�ȼ��� �Ҵ�Ǹ� 48�ȼ��̸� �� min�ΰ� �ϴ� ���̴�
        //  �̶� min�� mills�� ġȯ?�ؼ� ������� �ϸ�
        // (60 * 60000) : 107 = ? : 48
        // ? * 107 =  (60 * 60000) * 48
        // ? = ((60 * 60000) * 48) / 107
        // ?�̰� minimumDurationMillis ���̴�
        // �ٵ� ��ģ ������ �� �Ʒ�ó�� ������ �ߴ��� ������
        // �׷��� ���� ������ �� ����...
        final long minimumDurationMillis = (long)
                (MIN_EVENT_HEIGHT * DateUtils.MINUTE_IN_MILLIS / (mCellHeight / 60.0f));
        if (INFO) Log.i(TAG, "remeasure:Call Event.computePositions");
        Event.computePositions(mEvents, minimumDurationMillis);

        // Compute the top of our reachable view
        // Max View Height = GRID_LINE_TOP_MARGIN + HOUR_GAP + (24 * (mCellHeight + HOUR_GAP)) + GRID_LINE_BOTTOM_MARGIN
        mMaxViewStartY = (GRID_LINE_TOP_MARGIN + HOUR_GAP + (24 * (mCellHeight + HOUR_GAP)) + GRID_LINE_BOTTOM_MARGIN) - mGridAreaHeight;
        
        // ���⼭ ������....
        // :�Ʒ� ��Ȳ�� �� �߻��Ǵ°� ����........................................................................................................................
        if (mViewStartY > mMaxViewStartY) {
            mViewStartY = mMaxViewStartY;
            computeFirstHour();
        }

        if (mFirstHour == -1) {
            initFirstHour();
            mFirstHourOffset = 0;
        }
        
        mViewStartY = ( GRID_LINE_TOP_MARGIN + HOUR_GAP + (mFirstHour * (mCellHeight + HOUR_GAP)) ) - mFirstHourOffset; 
    }


    private void initFirstHour() {
    	// �� mNumHours�� 5�� ������ ��?
    	// ������ selection hour�� view start y�� �� �°� ��ġ�ϵ��� �ϴ°� �ƴ϶�
    	// selection hour�� view start y���� ��� ���� �������� �ؼ� �ð������� ���� ���ϰ�? �ϱ� ����???
    	// ���� scroll view�� scale ������ �����̸��� mNumHours�� 17������ �����ϴ�
    	// �츮�� scale ����� ������ �����̹Ƿ� mNumHours / 5�� ������ �������� ��������
    	
    	// nNumHours�� 5�� ������ ����
    	// nNumHours�� 20% ������ height ������ selection hour ���� �ð���� ���� �ְڴٴ� ��
        mFirstHour = mSelectionStartHour - mNumHours / 5;
        if (mFirstHour < 0) {
            mFirstHour = 0;
        } else if (mFirstHour + mNumHours > 24) {
            mFirstHour = 24 - mNumHours;
        }
    }

    /**
     * Recomputes the first full hour that is visible on screen after the
     * screen is scrolled.
     */
    
    private void computeFirstHour() {
            	
    	// mViewStartY�� 0�̶�� ���� visible first hour�� am12�� �ǹ��ϰ� am12 �ð��� ���� GRID_LINE_TOP_MARGIN ������ ù��° hour line�� �����ؾ� ��
    	if (mViewStartY == 0) {
    		mFirstHour = 0; // am 12�ø� �ǹ���
    		mFirstHourOffset = GRID_LINE_TOP_MARGIN + HOUR_GAP;
    	}
    	else {
    		mFirstHour = (mViewStartY + mCellHeight + HOUR_GAP) / (mCellHeight + HOUR_GAP);
    		mFirstHourOffset =  ( GRID_LINE_TOP_MARGIN + HOUR_GAP + (mFirstHour * (mCellHeight + HOUR_GAP)) ) - mViewStartY;
    	}    	
        
        //if (INFO) Log.i(TAG, "computeFirstHour:mSelectionStartHour=" + String.valueOf(mSelectionStartHour) + ", First H=" + String.valueOf(mFirstHour) + ", Offset=" + String.valueOf(mFirstHourOffset));
    }
    
    void clearCachedEvents() {
        //mLastReloadMillis = 0;
    }       
    
    public void setEventsX(ArrayList<Event> events) {
    	mEvents = events;
    	
        computeEventRelations();

        mRemeasure = true;//////////////////////////////////////////////////////////////////////////////////////////////////////////
        //mComputeSelectedEvents = true;        
        
        invalidate();////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }    
    
    public void setEventsAlpha(int alpha) {
        mEventsAlpha = alpha;
        invalidate();
    }

    public int getEventsAlpha() {
        return mEventsAlpha;
    }

    public void stopEventsAnimation() {
        if (mEventsCrossFadeAnimation != null) {
            mEventsCrossFadeAnimation.cancel();
        }
        mEventsAlpha = 255;
    }

    // callers
    // -reloadEvents
    // -reloadEventsWithoutCallback
    private void computeEventRelations() {
        // Compute the layout relation between each event before measuring cell
        // width, as the cell width should be adjusted along with the relation.
        //
        // Examples: A (1:00pm - 1:01pm), B (1:02pm - 2:00pm)
        // We should mark them as "overwapped". Though they are not overwapped logically, but
        // minimum cell height implicitly expands the cell height of A and it should look like
        // (1:00pm - 1:15pm) after the cell height adjustment.

        // Compute the space needed for the all-day events, if any.
        // Make a pass over all the events, and keep track of the maximum
        // number of all-day events in any one day.  Also, keep track of
        // the earliest event in each day.
        int maxAllDayEvents = 0;
        final ArrayList<Event> events = mEvents;
        final int len = events.size();
        // Num of all-day-events on each day.
        final int eventsCount[] = new int[mLastJulianDay - mFirstJulianDay + 1];
        Arrays.fill(eventsCount, 0);
        for (int ii = 0; ii < len; ii++) {
            Event event = events.get(ii);
            if (event.startDay > mLastJulianDay || event.endDay < mFirstJulianDay) {
                continue;
            }
            if (event.drawAsAllday()) {
                // Count all the events being drawn as allDay events
                final int firstDay = Math.max(event.startDay, mFirstJulianDay);
                final int lastDay = Math.min(event.endDay, mLastJulianDay);
                for (int day = firstDay; day <= lastDay; day++) {
                    final int count = ++eventsCount[day - mFirstJulianDay];
                    if (maxAllDayEvents < count) {
                        maxAllDayEvents = count;
                    }
                }

                int daynum = event.startDay - mFirstJulianDay;
                int durationDays = event.endDay - event.startDay + 1;
                if (daynum < 0) {
                    durationDays += daynum;
                    daynum = 0;
                }
                if (daynum + durationDays > mNumDays) {
                    durationDays = mNumDays - daynum;
                }
                                
            } else {
                int daynum = event.startDay - mFirstJulianDay;
                int hour = event.startTime / 60;
                if (daynum >= 0 && hour < mEarliestStartHour[daynum]) {
                    mEarliestStartHour[daynum] = hour;
                }

                // Also check the end hour in case the event spans more than
                // one day.
                daynum = event.endDay - mFirstJulianDay;
                hour = event.endTime / 60;
                if (daynum < mNumDays && hour < mEarliestStartHour[daynum]) {
                    mEarliestStartHour[daynum] = hour;
                }
            }
        }        
    }   
    
    @Override
    protected void onDraw(Canvas canvas) {    	
    		
        if (mRemeasure) {        	
            remeasure(getWidth(), getHeight());
            mRemeasure = false;
        }
        canvas.save();

        //float yTranslate = -mViewStartY + DAY_HEADER_HEIGHT + mAlldayHeight;
        float yTranslate = -mViewStartY + mFirstCell;
        // offset canvas by the current drag and header position        
        // Ŭ���� ������ ���� view�� �׸��� ����!!!
        // translate�� dy ���� -���� �ָ�
        // Ŭ���� ������ ���� ���� �ø��� ȿ��?�� ��
        // [0,0]�� ���� �̵�?�ϱ� ������...
        canvas.translate(0, yTranslate);//canvas.translate(-mViewStartX, yTranslate);
                
        // clip to everything below the allDay area
        Rect dest = mDestRect;
        dest.top = (int) (mFirstCell - yTranslate);        
        dest.bottom = (int) (mViewHeight - yTranslate); // �� �̷��� ����������? �ܼ��� �Ʒ�ó�� dest.top�� mViewHeight�� �� �ϸ� �� �Ž�...
                                                          // :�ƴϴ� ���� mAlldayHeight�� 0�� �ƴ϶�� ������ �ȴ�
        //dest.bottom = dest.top + mViewHeight;
        dest.left = 0;
        dest.right = mViewWidth;
        canvas.save();
        // Intersect the current clip with the specified rectangle, which is expressed in local coordinates
        canvas.clipRect(dest);
        
        // Draw the movable part of the view
        int dontDrawHour = doDraw(canvas);///////////////////////////////////////////////////////////////////////////////////
        
        // restore to having no clip
        canvas.restore();
        
        canvas.translate(0, yTranslate);//canvas.translate(mViewStartX, -yTranslate);
        
        // Draw the fixed areas (that don't scroll) directly to the canvas.
        drawAfterScroll(canvas, dontDrawHour);
        
        canvas.restore();
    }

    private void drawAfterScroll(Canvas canvas, int dontDrawHour) {
        Paint p = mPaint;
        Rect r = mRect;  

        // all-day�� �����ϸ� all-day ������ ���� Ÿ�Ӷ��� ����(��ũ�� ���� ����)�� ������ ��� �Ѵ�
        drawScrollLine(r, canvas, p);        

        // Draw the AM and PM indicators if we're in 12 hour mode
        if (!mIs24HourFormat) {
            drawAmPm(canvas, p, dontDrawHour);
        }
    }
  
    
    private void drawScrollLine(Rect r, Canvas canvas, Paint p) {
        final int right = computeDayLeftPosition(mNumDays);
        final int y = mFirstCell - 1;

        p.setAntiAlias(false);
        p.setStyle(Style.FILL);

        p.setColor(mCalendarGridLineInnerHorizontalColor);
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        canvas.drawLine(GRID_LINE_LEFT_MARGIN, y, right, y, p);
        p.setAntiAlias(true);
    }

    // Computes the x position for the left side of the given day (base 0)
    private int computeDayLeftPosition(int day) {
        int effectiveWidth = mViewWidth - mHoursWidth;
        return day * effectiveWidth / mNumDays + mHoursWidth;
    }   

    private void drawAmPm(Canvas canvas, Paint p, int dontDrawHour) {
    	boolean drawAm = true;
    	boolean drawPm = true;
    	
    	if (dontDrawHour != -1) {
    		if (dontDrawHour < 12) {
        		drawAm = false;
            }
        	else {
        		drawPm = false;
        	}   
        }        

        p.setColor(mCalendarAmPmLabel);
        p.setTextSize(AMPM_TEXT_SIZE);
        p.setTypeface(mBold);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.RIGHT);
        
        String text = mAmString;
        if (mFirstHour >= 12) {
            text = mPmString;
        }
        
        float x = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN - mMaxHourDecimalTextWidth - (NEW_HOURS_RIGHT_MARGIN / 2);          
        int y = mFirstCell + mFirstHourOffset + mHoursTextHeight + HOUR_GAP;        
        //int y = mFirstCell + mFirstHourOffset + 2 * mHoursTextHeight + HOUR_GAP;       
        
        if (drawAm || drawPm)
        	canvas.drawText(text, x, y, p);
        
        if (drawPm)
	        if (mFirstHour < 12 && mFirstHour + mNumHours > 12) {
	            // Also draw the "PM"
	            text = mPmString;
	            
	            y = mFirstCell + mFirstHourOffset + (12 - mFirstHour) * (mCellHeight + HOUR_GAP)
	                    + mHoursTextHeight + HOUR_GAP;            
	            canvas.drawText(text, x, y, p);
	        }
    }   
    
    private void drawCurrentTimeTextAndLine(Rect r, final int day, final int top, Canvas canvas,
            Paint p) {
    	Calendar curTimeCalendar = (Calendar) GregorianCalendar.getInstance(mCurrentTime.getTimeZone());    	
    	curTimeCalendar.setTimeInMillis(mCurrentTime.getTimeInMillis());
    	
    	p.setColor(Color.RED);
        p.setTextSize(AMPM_TEXT_SIZE);
        p.setTypeface(mBold);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.RIGHT);        
        
        String meridiem = mAmString;
        if (curTimeCalendar.get(Calendar.AM_PM) == Calendar.PM) {
        	meridiem = mPmString;
        }        
        String space = " ";
        String hour = String.valueOf(curTimeCalendar.get(Calendar.HOUR_OF_DAY));
        String colon = ":";
        String minute = String.valueOf(curTimeCalendar.get(Calendar.MINUTE));
        if (curTimeCalendar.get(Calendar.MINUTE) < 10) {
        	minute = "0" + minute;
        }        
        String currentTime = meridiem + space + hour + colon + minute;
        float text_x = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN;
        float text_y = top - CURRENT_TIME_LINE_TOP_OFFSET + (mAMPMTextHeight / 2);
        
        canvas.drawText(currentTime, text_x, text_y, p);    	
    	
        r.left = computeDayLeftPosition(day) - CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        r.right = computeDayLeftPosition(day + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        r.top = top - CURRENT_TIME_LINE_TOP_OFFSET;
        r.bottom = r.top + mCurrentTimeLine.getIntrinsicHeight();

        mCurrentTimeLine.setBounds(r);
        mCurrentTimeLine.draw(canvas);        
    }

    private int doDraw(Canvas canvas) {
        Paint p = mPaint;
        Rect r = mRect;
        
        drawGridBackground(r, canvas, p);
        int dontDrawHour = drawHours(r, canvas, p);

        // Draw each day
        int cell = mFirstJulianDay;
        p.setAntiAlias(false);
        int alpha = p.getAlpha();
        p.setAlpha(mEventsAlpha);
        for (int day = 0; day < mNumDays; day++, cell++) {
            // TODO Wow, this needs cleanup. drawEvents loop through all the
            // events on every call.
            //drawEvents(cell, day, HOUR_GAP, canvas, p); // +commented by intheeast
        	drawEvents(cell, day, GRID_LINE_TOP_MARGIN + HOUR_GAP, canvas, p); // +added by intheeast
            // If this is today
            if (cell == mTodayJulianDay) {
                /*int lineY = mCurrentTime.hour * (mCellHeight + HOUR_GAP)
                        + ((mCurrentTime.minute * mCellHeight) / 60) + 1;*/ // +commented by intheeast
            	int lineY = GRID_LINE_TOP_MARGIN 
            			+ HOUR_GAP
            			+ mCurrentTime.get(Calendar.HOUR_OF_DAY) * (mCellHeight + HOUR_GAP)
                        + ((mCurrentTime.get(Calendar.MINUTE) * mCellHeight) / 60); // +added by intheeast
                // And the current time shows up somewhere on the screen
                if (lineY >= mViewStartY && lineY < mViewStartY + mViewHeight - 2) {
                    //drawCurrentTimeLine(r, day, lineY, canvas, p); // +commented by intheeast
                	drawCurrentTimeTextAndLine(r, day, lineY, canvas, p); // +added by intheeast                	
                }
            }
        }
        p.setAntiAlias(true);
        p.setAlpha(alpha);

        return dontDrawHour;
    }    
    
    // +added by intheeast
    // 1. cell height�� ũ�⸦ ���Ѵ�
    // 2. �ش� view�� ���� ���ϰ� ��ġ�ϴ°�?
    private int drawHours(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);

        int dontDrawHour = -1;        
        
    	if (mFirstJulianDay == mTodayJulianDay) {
    		//checkCurrentTimeText = true;        		
    		// mHoursTextHeight
    		// mAMPMTextHeight
    		// ������ ����ϸ� �ȵȴ�!!!
    		float cellHeight = (float)mCellHeight;
    		float hourGap = (float)HOUR_GAP;
    		
    		float oneMinuteHeight = (cellHeight + hourGap) / 60;
    		float curMinutePos = mCurrentTime.get(Calendar.MINUTE) * oneMinuteHeight;
    		// mHoursTextHeight��
    		// mHoursTextHeight = (int)Math.abs(p.ascent());  
    		// �� ����������...intheeast�� mHoursTextHeight�� ������� �ʰ� 
    		// mRealHoursTextHeight�� ����/�����Ͽ� ����Ѵ�
    		float realHoursTextHeight = (float)mRealHoursTextHeight;
    		float upperLimitMinute = (realHoursTextHeight / 2); // 0�п��� ?�б���
    		float lowerLimitMinute = (cellHeight + hourGap) - (realHoursTextHeight / 2); // ���� hour + 1 �ð����� ���� hour:?minute ����
    		
    		if (curMinutePos < upperLimitMinute)
    			dontDrawHour = mCurrentTime.get(Calendar.HOUR_OF_DAY);
    		else if (curMinutePos >= lowerLimitMinute)
    			dontDrawHour = mCurrentTime.get(Calendar.HOUR_OF_DAY) + 1;
    	}        
        
        float x = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN; 
        int y = HOUR_GAP + mHoursTextHeight + HOURS_TOP_MARGIN;

        for (int i = 0; i < 25; i++) {
        	if (/*checkCurrentTimeText &&*/ (dontDrawHour != -1)) {
        		if (dontDrawHour == i) {
        			y += mCellHeight + HOUR_GAP;
        			continue;
        		}
        	}
        	
            String time = mHourStrs[i];
            
            canvas.drawText(time, x, y, p);  
            y += mCellHeight + HOUR_GAP;
        }
        
        return dontDrawHour;
    }

    private void setupHourTextPaint(Paint p) {
        p.setColor(mCalendarHourLabelColor);
        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);        
        p.setAntiAlias(true);
    }
        
    private void drawGridBackground(Rect r, Canvas canvas, Paint p) {
        Paint.Style savedStyle = p.getStyle();

        final float stopX = computeDayLeftPosition(mNumDays);
        float y = GRID_LINE_TOP_MARGIN + HOUR_GAP;
        final float deltaY = mCellHeight + HOUR_GAP;
        int linesIndex = 0;
        final float startY = 0;
        final float stopY = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP);
        float x = mHoursWidth;

        // Draw the inner horizontal grid lines        
        p.setColor(mCalendarGridLineInnerHorizontalColor);
        //p.setColor(Color.RED); // for test
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        p.setAntiAlias(false);
        //y = 0; // �� ���� GRID_LINE_TOP_MARGIN�� ������ �� �ٽ� 0���� �����ϴ� ���ΰ�???
        linesIndex = 0;
        for (int hour = 0; hour <= 24; hour++) {
            mLines[linesIndex++] = GRID_LINE_LEFT_MARGIN;
            mLines[linesIndex++] = y;
            mLines[linesIndex++] = stopX;
            mLines[linesIndex++] = y;
            y += deltaY;
        }
        
        
        if (mCalendarGridLineInnerVerticalColor != mCalendarGridLineInnerHorizontalColor) {
            canvas.drawLines(mLines, 0, linesIndex, p);
            linesIndex = 0;
            p.setColor(mCalendarGridLineInnerVerticalColor);
        }

        // Draw the inner vertical grid lines
        if (mNumDays != 1) {
	        for (int day = 0; day <= mNumDays; day++) {
	            x = computeDayLeftPosition(day);
	            mLines[linesIndex++] = x;
	            mLines[linesIndex++] = startY;
	            mLines[linesIndex++] = x;
	            mLines[linesIndex++] = stopY;
	        }
        }
        
        canvas.drawLines(mLines, 0, linesIndex, p);

        // Restore the saved style.
        p.setStyle(savedStyle);
        p.setAntiAlias(true);
    }    

    public static int computeMaxStringWidth(int currentMax, String[] strings, Paint p) {
        float maxWidthF = 0.0f;

        int len = strings.length;
        for (int i = 0; i < len; i++) {
            float width = p.measureText(strings[i]);
            maxWidthF = Math.max(width, maxWidthF);
        }
        int maxWidth = (int) (maxWidthF + 0.5); // �� 0.5�� ���ϴ°�? 
                                                // �Ǽ��� ������ ��ȯ�ϱ� ���� �Ǽ����� �ݿø��Ѵ�
        if (maxWidth < currentMax) {
            maxWidth = currentMax;
        }
        return maxWidth;
    }

    private void setupTextRect(Rect r) {
        if (r.bottom <= r.top || r.right <= r.left) {
            r.bottom = r.top;
            r.right = r.left;
            return;
        }

        if (r.bottom - r.top > EVENT_TEXT_TOP_MARGIN + EVENT_TEXT_BOTTOM_MARGIN) {
            r.top += EVENT_TEXT_TOP_MARGIN;
            r.bottom -= EVENT_TEXT_BOTTOM_MARGIN;
        }
        if (r.right - r.left > EVENT_TEXT_LEFT_MARGIN + EVENT_TEXT_RIGHT_MARGIN) {
            r.left += EVENT_TEXT_LEFT_MARGIN;
            r.right -= EVENT_TEXT_RIGHT_MARGIN;
        }
    }	
    
    int mEllipsizedLeft;
    int mEllipsizedRight;
    EllipsizeCallback mEllipsizeCallback = new EllipsizeCallback() {

		@Override
		public void ellipsized(int start, int end) {			
			mEllipsizedLeft = start;
			mEllipsizedRight = end;				
		}    	
    };
    
    
    private String getEventTitle(Event event, Paint paint, float avail) {
               	
    	TextPaint textPaint = new TextPaint(paint);     	        	
    	
        float availWidth = avail;    
        
        if (event.title != null) {            	
        	
            // MAX - 1 since we add a space!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!        	
        	String saitizedTitleText = drawTextSanitizer(event.title.toString(), MAX_EVENT_TEXT_LEN - 1);       	    	
        	/*
            text : the text to truncate
            p : the Paint with which to measure the text
            availWidth : the horizontal width available for the text                
            */            	            	
        	String outputString = TextUtils.ellipsize(saitizedTitleText, textPaint, availWidth, TruncateAt.END, false, mEllipsizeCallback).toString();
        	// empty ��Ȳ�� �� ���� ��쿡 �߻��Ѵ�
        	// availWidth�� saitizedText ��ü�� ������ �� ��� saitizedText�� �Ϻκ��� ellipsis Ư�� ���ڷ� ġȯ�ؼ� �����ؾ� �ϴ� ��쿡
        	// 1.availWidth�� ellipsis Ư�� ���� ���� ������ �� ���� ��ŭ ���� ��쿡...
        	// 2.availWidth�� ellipsis Ư�� ���ڴ� ������ �� ������ 
        	//   availWidth���� ellipsis Ư�� ���ڸ� ������ ������ ������ remaining availWidth����
        	//   left = mt.breakText(len, true, availWidth) �� ������ ���
        	//   [���� len�� 1�̰� availWidth�� 1pixel�̰� �ش� �ؽ�Ʈ�� ù��° char�� ���̰� 1pixel���� ũ�ٸ�... 
        	//    ->left�� 0�� �ǰ� right�� 1�� �ȴ�],            	
        	//   remaining[len - (right - left)] == 0 �� ��찡 �߻��� ���...   
        	//   :remaining�� ellipsis�� ó������ ���� saitizedText�� �ٸ� �κ��� ������ ������ ���ٴ� ���� �ǹ�
        	//    ���� left�� ellipsis�� ó���� string�� start �κ��̰� right�� �κ��� end�� �ǹ��ϴµ�
        	//    right - left�� ellipsis�� ó���� string�� ���̸� �ǹ��Ѵ�
        	//    ��, len == (right - left)��� ����,
        	//    saitizedText ��ü�� ellipsis�� ó���ȴٴ� ���� �ǹ��Ѵٰ� �� ���� �ִµ�...
        	//    �ƽ��Ե� saitizedText ��ü�� ellipsis�� ó���� ������ �������� �ʰ� empty string�� �����Ѵ�
        	   
        	if (outputString.isEmpty()) {
        		String ellipsis = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
        		float ellipsiswid = textPaint.measureText(ellipsis);
        		float xAvailWidth = avail;     
        		xAvailWidth -= ellipsiswid;
        		
    			if (xAvailWidth < 0) {
    				ellipsis = "\u2025"; // \u2025�� 0x2025�� ��Ÿ���� TWO DOT LEADER 
    				ellipsiswid = textPaint.measureText(ellipsis);
    				xAvailWidth = avail;     
    				xAvailWidth -= ellipsiswid;
        			if (xAvailWidth < 0) {
        				//ellipsis = "\u2024"; // \U2024�� 0x2024�� ��Ÿ���� ONE DOT LEADER : android�� one dot leader�� ǥ������ ���ϴ� �� ����  
        				ellipsis = ".";
        				ellipsiswid = textPaint.measureText(ellipsis);
        				xAvailWidth = avail;     
        				xAvailWidth -= ellipsiswid;
            			if (xAvailWidth >= 0) {
            				outputString = ellipsis.toString();
            			}
        			}
        			else {
        				outputString = ellipsis.toString();
        			}
    			}
    			else {
    				outputString = ellipsis.toString();
    			}    			
    			
        	}        	
        	
            return outputString;
        }  
        else {
        	return null;
        }       
    }

    private String getEventLocation(Event event, Paint paint, float avail) {
       	
    	TextPaint textPaint = new TextPaint(paint); 
    	
    	float titleTextFontSize = textPaint.getTextSize();
    	float fontSize = titleTextFontSize * 0.9f;
    	//float fontSize = titleTextFontSize * 0.8f;
    	textPaint.setTextSize(fontSize);
    	
        float availWidth = avail;    
        
        if (event.location != null) {            	
        	
            // MAX - 1 since we add a space!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!        	
        	String saitizedLocationText = drawTextSanitizer(event.location.toString(), MAX_EVENT_TEXT_LEN - 1);       	    	
        	  	            	
        	String outputString = TextUtils.ellipsize(saitizedLocationText, textPaint, availWidth, TruncateAt.END, false, mEllipsizeCallback).toString();
        	        	   
        	if (outputString.isEmpty()) {
        		String ellipsis = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
        		float ellipsiswid = textPaint.measureText(ellipsis);
        		float xAvailWidth = avail;     
        		xAvailWidth -= ellipsiswid;
    			if (xAvailWidth < 0) {
    				ellipsis = "\u2025"; // \u2025�� 0x2025�� ��Ÿ���� TWO DOT LEADER 
    				ellipsiswid = textPaint.measureText(ellipsis);
    				xAvailWidth = avail;     
    				xAvailWidth -= ellipsiswid;
        			if (xAvailWidth < 0) {
        				//ellipsis = "\u2024"; // \U2024�� 0x2024�� ��Ÿ���� ONE DOT LEADER : android�� one dot leader�� ǥ������ ���ϴ� �� ����    
        				ellipsis = ".";
        				ellipsiswid = textPaint.measureText(ellipsis);
        				xAvailWidth = avail;     
        				xAvailWidth -= ellipsiswid;
            			if (xAvailWidth >= 0) {
            				outputString = ellipsis.toString();
            			}
        			}
        			else {
        				outputString = ellipsis.toString();
        			}
    			}
    			else {
    				outputString = ellipsis.toString();
    			}    			
        	}       	
        	
            return outputString;
        }  
        else {
        	return null;
        }       
    }    
    
    private void drawEvents(int date, int dayIndex, int top, Canvas canvas, Paint p) {
        Paint eventTextPaint = mEventTextPaint;
        int left = computeDayLeftPosition(dayIndex) + 1;
        int cellWidth = computeDayLeftPosition(dayIndex + 1) - left + 1;
        int cellHeight = mCellHeight;

        // Use the selected hour as the selection region
        Rect selectionArea = mSelectionRect;
        selectionArea.top = top + mSelectionStartHour * (cellHeight + HOUR_GAP);
        selectionArea.bottom = selectionArea.top + cellHeight;
        selectionArea.left = left;
        selectionArea.right = selectionArea.left + cellWidth;

        final ArrayList<Event> events = mEvents;
        int numEvents = events.size();
        EventGeometry geometry = mEventGeometry;

        //final int viewEndY = mViewStartY + mViewHeight - DAY_HEADER_HEIGHT - mAlldayHeight - GRID_LINE_TOP_MARGIN;
        final int viewEndY = mViewStartY + mViewHeight;
        
        int alpha = eventTextPaint.getAlpha();
        eventTextPaint.setAlpha(mEventsAlpha);
        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
                continue;
            }

            // Don't draw it if it is not visible
            if (event.bottom < mViewStartY || event.top > viewEndY) {
                continue;
            }

            Rect r = drawEventRect(event, canvas, p, eventTextPaint, mViewStartY, viewEndY);
            setupTextRect(r);

            // Don't draw text if it is not visible
            if (r.top > viewEndY || r.bottom < mViewStartY) {
                continue;
            }            
            
            boolean twoLines = true;
        	int eventHeight = (int) (event.bottom - event.top);
        	// event rect height�� two text line�� ������ �� ���ٸ� one text line���� ���� �Ѵ�
        	if (eventHeight < mMinEventRectHeightTwoLinesEventText) { 
        		twoLines = false;
            }
        	
        	// event rect height�� one text line�� �����ϴµ�..
        	// EVENT_TEXT_FONT_SIZE �� text height�� ������ �� ���� ����
        	// event rect height�� �°� event font size�� �缳���ؾ� �Ѵ�
        	if (eventHeight < mAvailEventRectHeightByDefaultEventTextHeight) { 
        		// event height�� 50% size�� ����� : 
        		float eventFontSize = getEventFontSize(eventHeight, 0.5f);
        		if (eventFontSize > EVENT_TEXT_FONT_SIZE) {
        			if (INFO) Log.i(TAG, "What the hell..����������");
        		}
        		
        		eventTextPaint.setTextSize(eventFontSize);            		
            }
        	     	
        	
            float availWidth = r.width();         	
            String title = getEventTitle(event, eventTextPaint, availWidth);                
            
            // title�� ONE DOT LEADER�� ��ü�� �� ���� ��ŭ ������ �����ϴٴ� ���̹Ƿ�
            // �ƿ� �ؽ�Ʈ�� ������� �õ����� ���ʿ��ϴ�
            if (!title.isEmpty()) {
            	switch (event.selfAttendeeStatus) {
                case Attendees.ATTENDEE_STATUS_INVITED:
                	eventTextPaint.setColor(event.color);
                    break;
                case Attendees.ATTENDEE_STATUS_DECLINED:
                	eventTextPaint.setColor(mEventTextColor);
                	eventTextPaint.setAlpha(Utils.DECLINED_EVENT_TEXT_ALPHA);
                    break;
                case Attendees.ATTENDEE_STATUS_NONE: // Your own events
                case Attendees.ATTENDEE_STATUS_ACCEPTED:
                case Attendees.ATTENDEE_STATUS_TENTATIVE:
                default:
                	eventTextPaint.setColor(mEventTextColor);
                    break;
            	}                 	                            	
            	
            	float locationTextAvailWidth = availWidth;
            	if (!twoLines) {
            		float titleTextConsumedWidth = eventTextPaint.measureText(title);
            		String space = " ";
            		float spaceWidth = eventTextPaint.measureText(space);
            		locationTextAvailWidth = availWidth - titleTextConsumedWidth - spaceWidth;            		
            	}            	            	
            	
            	String location = getEventLocation(event, eventTextPaint, locationTextAvailWidth);
            	
	            // TODO: not sure why we are 4 pixels off
	            drawEventTitleLocationText(title, location, twoLines, r, canvas, eventTextPaint, 
	            		mViewStartY/* + 4*/, 
	            		mViewStartY + mViewHeight - DAY_HEADER_HEIGHT);      
	            
            }
                        
        	eventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
            
        }
        
        eventTextPaint.setAlpha(alpha);        
    }   

    private Rect drawEventRect(Event event, Canvas canvas, Paint p, Paint eventTextPaint,
            int visibleTop, int visibleBot) {
        // Draw the Event Rect
        Rect r = mRect;
        r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN, visibleTop);
        r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN, visibleBot);
        r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right;

        int color;        
        color = event.color; //CALENDAR_COLOR �̴�        
        
        int bg = 0xffffffff;
        int a = 153;
        int red = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
        color = (0xff000000) | ((red | g | b) >> 8);        
		
        switch (event.selfAttendeeStatus) {
            case Attendees.ATTENDEE_STATUS_INVITED:
                
                p.setStyle(Style.STROKE);
                
                break;
            case Attendees.ATTENDEE_STATUS_DECLINED:
                
                //color = Utils.getDeclinedColorFromColor(color);
                
            case Attendees.ATTENDEE_STATUS_NONE: // Your own events
            case Attendees.ATTENDEE_STATUS_ACCEPTED:
            case Attendees.ATTENDEE_STATUS_TENTATIVE:
            default:
                p.setStyle(Style.FILL_AND_STROKE);
                break;
        }

        p.setAntiAlias(false);

        int floorHalfStroke = (int) Math.floor(EVENT_RECT_STROKE_WIDTH / 2.0f);
        int ceilHalfStroke = (int) Math.ceil(EVENT_RECT_STROKE_WIDTH / 2.0f);
        r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN + ceilHalfStroke, visibleTop);
        r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN - floorHalfStroke,
                visibleBot);
        r.left += ceilHalfStroke;
        r.right -= floorHalfStroke;
        p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        p.setColor(color);
        int alpha = p.getAlpha();
        p.setAlpha(mEventsAlpha);        
        canvas.drawRect(r, p);///////////////////////////////////////////////////////////////////////////           
        
        
        p.setAlpha(alpha);
        Rect dnaStrand = new Rect();
        dnaStrand.left = r.left;
        dnaStrand.top = r.top;
        dnaStrand.right = dnaStrand.left + (int)(mCellWidth * 0.0025f);
        dnaStrand.bottom = r.bottom;
        p.setColor(event.color);
        p.setAlpha(255);
        canvas.drawRect(dnaStrand, p);
        
        p.setStyle(Style.FILL);        
        
        // Setup rect for drawEventText which follows
        r.top = (int) event.top + EVENT_RECT_TOP_MARGIN;
        r.bottom = (int) event.bottom - EVENT_RECT_BOTTOM_MARGIN;        
        r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right;//r.right = (int) event.right - EVENT_RECT_RIGHT_MARGIN;
        return r;
    }

    // \t : Ű���� ������ TabŰ, ����8���� �ش��ϴ� �����̴�. (��쿡 ���� ����2~4���� �ش�� ���� �ִ�.)
    private final Pattern drawTextSanitizerFilter = Pattern.compile("[\t\n],");

    // Sanitize a string before passing it to drawText or else we get little squares. 
    // For newlines and tabs before a comma, delete the character.
    // Otherwise, just replace them with a space.
    private String drawTextSanitizer(String string, int maxEventTextLen) {
        Matcher m = drawTextSanitizerFilter.matcher(string);
        string = m.replaceAll(",");

        int len = string.length();
        if (maxEventTextLen <= 0) {
            string = "";
            len = 0;
        } else if (len > maxEventTextLen) {
            string = string.substring(0, maxEventTextLen);
            len = maxEventTextLen;
        }

        return string.replace('\n', ' ');
    }  
    
    
    //ZWNBS : Zero width no break space
    // ZWNBS is also known as the byte order mark (BOM) if used at the beginning of a Unicode file. 
    // It was originally used in the middle of Unicode files in rare instances where there was an invisible join between two characters where a line break must not occur. 
    // A new code joiner is being implemented U+2060 WORD JOINER.
    
    private void drawEventTitleLocationText(String title, String location, boolean twoLines, Rect rect, Canvas canvas, Paint paint, int top,
            int bottom) {   	
            
    	Paint titlePaint = new Paint(paint);
    	Paint locationPaint = new Paint(paint);    	
    	
    	titlePaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    	locationPaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
    	
    	int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;         
        
        float titleAscent = titlePaint.ascent();
    	float titleDescent = titlePaint.descent();
    	int titleLineHeight = (int) (Math.abs(titleAscent) + Math.abs(titleDescent));    	
    	
    	//paint.setTypeface(savePrvType);
    	float titleTextFontSize = titlePaint.getTextSize();
    	float locationFontSize = titleTextFontSize * 0.9f;
    	locationPaint.setTextSize(locationFontSize);
    	float locationAscent = locationPaint.ascent();
    	float locationDescent = locationPaint.descent();
    	int locationLineHeight = (int) (Math.abs(locationAscent) + Math.abs(locationDescent));   	
    	
    	int totalLineHeight = titleLineHeight + EVENT_TEXT_PAD_BETWEEN_LINES + locationLineHeight;            

        // Use a StaticLayout to format the string.
        canvas.save();
        //  canvas.translate(rect.left, rect.top + (rect.bottom - rect.top / 2));
        //int padding = center? (rect.bottom - rect.top - totalLineHeight) / 2 : 0;
        //canvas.translate(rect.left, rect.top + padding);
        canvas.translate(rect.left, rect.top);
        
        rect.left = 0;
        rect.top = 0;
        rect.right = width;        
        rect.bottom = totalLineHeight;

        // There's a bug somewhere. If this rect is outside of a previous
        // cliprect, this becomes a no-op. What happens is that the text draw
        // past the event rect. The current fix is to not draw the staticLayout
        // at all if it is completely out of bound.
        canvas.clipRect(rect);           
                
        if (twoLines) {        	
        	// y ��ǥ�� title text�� descent�� �ǹ��Ѵ�
	        canvas.drawText(title, rect.left, titleLineHeight - titleDescent, titlePaint);  
	        
	        // for test : title text�� ascent line�� drawing �Ѵ� 
	        //canvas.drawLine(0, titleLineHeight - titleDescent + titleAscent, rect.left + rect.width(), titleLineHeight - titleDescent + titleAscent, titlePaint);
	        // for test : title text�� descent line�� drawing �Ѵ� 
	        //canvas.drawLine(0, titleLineHeight - titleDescent, rect.left + rect.width(), titleLineHeight - titleDescent, titlePaint);	        
	        //canvas.drawLine(0, titleLineHeight, rect.left + rect.width(), titleLineHeight, titlePaint);	        
	    	
	        canvas.drawText(location, rect.left, totalLineHeight - locationDescent, locationPaint);        
        }
        else {    
        	//Rect bounds = new Rect();
	        //titlePaint.getTextBounds(title, 0, title.length(),bounds);
        	//int titleTextHeight = bounds.height();
        	// ������ event rect�� center�� �ؽ�Ʈ�� ��ġ��Ű�� ���ؼ� ������ ���� ������
        	int topMargin = (height - titleLineHeight) / 2; 
        	float textY = topMargin + Math.abs(titleAscent);
        	
	        canvas.drawText(title, rect.left, textY, titlePaint);  
	        float titleTextWidth = titlePaint.measureText(title);	        
	        
	        if (!location.isEmpty()) {
	        	String space = " ";
		        float spaceWidth = titlePaint.measureText(space);		        
	        
	        	canvas.drawText(location, rect.left + titleTextWidth + spaceWidth, textY, locationPaint);	        	
	        }
        }
        canvas.restore();
    }    

    @Override
    protected void onDetachedFromWindow() {
        
        super.onDetachedFromWindow();
    }

    
}
