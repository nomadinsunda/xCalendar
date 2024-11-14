package com.intheeast.month;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.SelectedDateOvalDrawable;
import com.intheeast.year.YearMonthTableLayout;
import com.intheeast.year.YearsAdapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.OvalShape;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PsudoTargetMonthView extends View {
	
	public static final String PSUDO_MINIMONTH_PARAMS_WEEK_UPPERLINE_ALPHA_VALUE = "psudo_minimonth_week_upperline_alpha_value"; 
	public static final String PSUDO_MINIMONTH_PARAMS_TEXT_ALPHA_VALUE = "psudo_minimonth_text_alpha_value"; 
	public static final String PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE = "psudo_minimonth_event_circle_disappear_alpha_value"; 
	Context mContext;
	Paint mMonthNumPaint;
	Paint mMonthNumPaintBeforeChangeMiniMonth;
	Paint mNextMonthNumPaint;
	Paint mMinMonthMonthDayPaint;
	Paint mMonthMonthDayPaint;
	Paint mUpperLinePaint;
	Paint mEventExisteneCirclePaint;
	
	float mUpperLineStrokeWidth;	
	
	boolean mInitialized = false;
	boolean mSetDrawingParams = false;
	
	float mScaleWidthSize;
	float mScaleHeightSize;
	
	int mWidth;
	int mHeight;
	
	int mMonthViewMode;
	int mMonthListViewWidth;
	int mMonthListViewHeight;
	
	int mOriginalMiniMonthWidth;
	int mOriginalMiniMonthHeight;
	int mMiniMonthWidth;
	int mMiniMonthHeight;
	
	int mMiniMonthLeftMargin;
	int mMiniMonthIndicatorTextHeight;
	int mMiniMonthIndicatorTextBaseLineY;
	int mMiniMonthDayTextHeight;
	int mMiniMonthDayTextBaseLineY;
	int mMiniMonthIndicatorHeight;
	int mMiniMonthNormalWeekHeight;
	
	int mMonthListViewNormalWeekHeight;
	//int mMonthListViewLastWeekHeight;
	
	String mMonthText;
	
	float mMonthTextX = 0;	
	int mMonthDayTextLeftOffset;
	
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
    int mPsudoMiniMonthDayAlphaValue;
    int mPsudoMiniMonthWeekUpperlineAlphaValue = 255;
    int mPsudoMiniMonthEventCircleDisappearAlphaValue = 255;
    
    public PsudoTargetMonthView(Context context) {
		super(context);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
	}
    
    public PsudoTargetMonthView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
	}
    
    public PsudoTargetMonthView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
    }
    
    
    public void initView(Context context) {    	
    	
		if (!mInitialized) {
			mContext = context;
	    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
	    	
			mMonthNumPaint = new Paint();
	        mMonthNumPaint.setFakeBoldText(false);
	        mMonthNumPaint.setAntiAlias(true);        
	        mMonthNumPaint.setColor(Color.RED);             
	        mMonthNumPaint.setTextAlign(Align.LEFT);
	        //mMonthNumPaint.setTextAlign(Align.CENTER);
	        mMonthNumPaint.setTypeface(Typeface.MONOSPACE);
	        
	        mMonthNumPaintBeforeChangeMiniMonth = new Paint();
	        mMonthNumPaintBeforeChangeMiniMonth.setFakeBoldText(false);
	        mMonthNumPaintBeforeChangeMiniMonth.setAntiAlias(true);	        
	        mMonthNumPaintBeforeChangeMiniMonth.setColor(getResources().getColor(R.color.month_day_number));
	        mMonthNumPaintBeforeChangeMiniMonth.setStyle(Style.FILL);        
	        mMonthNumPaintBeforeChangeMiniMonth.setTextAlign(Align.LEFT);
	        mMonthNumPaintBeforeChangeMiniMonth.setTypeface(Typeface.DEFAULT);  
	        
	        mNextMonthNumPaint = new Paint();
	        mNextMonthNumPaint.setFakeBoldText(false);
	        mNextMonthNumPaint.setAntiAlias(true);	        
	        mNextMonthNumPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mNextMonthNumPaint.setStyle(Style.FILL);        
	        mNextMonthNumPaint.setTextAlign(Align.CENTER);
	        mNextMonthNumPaint.setTypeface(Typeface.DEFAULT);  
	        
	        mMinMonthMonthDayPaint = new Paint();
	        mMinMonthMonthDayPaint.setFakeBoldText(true);
	        mMinMonthMonthDayPaint.setAntiAlias(true);        
	        mMinMonthMonthDayPaint.setColor(Color.BLACK);            
	        mMinMonthMonthDayPaint.setTextAlign(Align.CENTER);
	        mMinMonthMonthDayPaint.setTypeface(Typeface.MONOSPACE);	 
	        
	        mMonthMonthDayPaint = new Paint();
	        mMonthMonthDayPaint.setFakeBoldText(false);
	        mMonthMonthDayPaint.setAntiAlias(true);	        
	        mMonthMonthDayPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mMonthMonthDayPaint.setStyle(Style.FILL);        
	        mMonthMonthDayPaint.setTextAlign(Align.CENTER);
	        mMonthMonthDayPaint.setTypeface(Typeface.DEFAULT);  
	        
	        mUpperLinePaint = new Paint();        
	        mUpperLinePaint.setAntiAlias(true);        
	        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUpperLinePaint.setStyle(Style.STROKE);
	        mUpperLineStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
	        mUpperLinePaint.setStrokeWidth(mUpperLineStrokeWidth);
	        
	        mEventExisteneCirclePaint = new Paint();    
	        mEventExisteneCirclePaint.setAntiAlias(true);        
	        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
	        mEventExisteneCirclePaint.setStyle(Style.FILL);        
	        
	        
	        mInitialized = true;
		}
	}
    
    public int getFirstJulianDay() {    	
    	return mFirstJulianDay;
    }
    
    public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        
        createEventExistenceCircle(unsortedEvents);
    }  
    
    List<ArrayList<Event>> mEvents = null;
    public void setEvents(List<ArrayList<Event>> sortedEvents) {    	
        mEvents = sortedEvents;
        if (sortedEvents == null) {
            return;
        }
        if (sortedEvents.size() != mNumDays) {
            if (Log.isLoggable("tag", Log.ERROR)) {
                Log.wtf("tag", "Events size must be same as days displayed: size="
                        + sortedEvents.size() + " days=" + mNumDays);
            }
            mEvents = null;
            return;
        }
    }
    
    boolean mEventExistence[] = null;
    ArrayList<Event> mUnsortedEvents = null;
    public void createEventExistenceCircle(ArrayList<Event> unsortedEvents) {
    	if (unsortedEvents == null) {
            // Stash the list of events for use when this view is ready, or
            // just clear it if a null set has been passed to this view
            mUnsortedEvents = unsortedEvents;            
            mEventExistence = null;
            return;
        } else {
            // clear the cached set of events since we're ready to build it now
            mUnsortedEvents = null;
        }       
        
    	int numDays = mEvents.size();
    	mEventExistence = new boolean[numDays];
    	for (int i=0; i<numDays; i++) {
    		ArrayList<Event> dayEvents = mEvents.get(i);
    		if (dayEvents.size() !=0)
    			mEventExistence[i] = true;
    		else
    			mEventExistence[i] = false;
    	}
    }
    
    boolean mLaunchScaleDrawingMiniMonthView = false;
	public void launchScaleDrawingMiniMonthView() {
		mLaunchScaleDrawingMiniMonthView = true;
		
		mMinMonthMonthDayPaint.setFakeBoldText(true);
		mMinMonthMonthDayPaint.setTextAlign(Align.CENTER);
		mMinMonthMonthDayPaint.setTypeface(Typeface.MONOSPACE); 
	}
	
	
	public void setMonthDrawingParams(HashMap<String, Integer> params) {
		if (!mSetDrawingParams) {
						
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE)) {
				mMonthViewMode = params.get(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE);						
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH)) {
				mMonthListViewWidth = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH);
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT)) {
				mMonthListViewHeight = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT);
	        }		
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_WIDTH)) {
				mOriginalMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_WIDTH);
				mScaleWidthSize = mMonthListViewWidth - mOriginalMiniMonthWidth;
	        }
			
			//
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_HEIGHT)) {
				mOriginalMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_HEIGHT);
				mScaleHeightSize = mMonthListViewHeight - mOriginalMiniMonthHeight;
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
				mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);				
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
	        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
	        }			
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
	        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
	        } 
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT)) {
	        	mMiniMonthIndicatorHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT);
	        }
			
			if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
	        	mMiniMonthIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
	        	mMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight); 
	        	mMonthNumPaintBeforeChangeMiniMonth.setTextSize(mMiniMonthIndicatorTextHeight);
	        	mNextMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight);
	        }
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
	        	mMiniMonthIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
	        }
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
	        	mMiniMonthDayTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
	        }
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
	        	mMiniMonthDayTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);  
	        	mMinMonthMonthDayPaint.setTextSize(mMiniMonthDayTextHeight); 
	        	mMonthMonthDayPaint.setTextSize(mMiniMonthDayTextHeight);
	        }        
        	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT)) {
	        	mMonthListViewNormalWeekHeight = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT); 	        	
	        }    
	        
	        /*if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT)) {
	        	mMonthListViewLastWeekHeight = params.get(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT); 	        	
	        }*/ 	        
	        
	        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT)) {
	        	mMiniMonthNormalWeekHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT);
	        }
	        
	        mSetDrawingParams = true;
		}
	}
	
	
	Calendar mGoToMonthTime;
	int mFirstJulianDay;
	public int mNumDays;
	int mPrvMonthLastWeekDaysNumOfGoToMonth;
	float mGoToMonthTextFinalX;
	
	String mTzId;
	TimeZone mTimeZone;
	int mFirstDayOfWeek;
	long mGmtoff;
	String mGoToMonthText;
	float mGoToMonthTextWidth;	
	ArrayList<String[]> mGoToMonthWeekDayNumberList = null;
	boolean mHasToday;
	int mDayOfMonthOfToday;
	public void setMonthInfo(Calendar goToMonthTime, ArrayList<String[]> goToMonthWeekDayNumberList) {		
				
		mTimeZone = goToMonthTime.getTimeZone();
		mTzId = mTimeZone.getID();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mGoToMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mGoToMonthTime.setTimeInMillis(goToMonthTime.getTimeInMillis());
		mFirstJulianDay = ETime.getJulianDay(mGoToMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		mNumDays = mGoToMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);
		Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
		TodayCal.setTimeInMillis(System.currentTimeMillis());
		
		if (mGoToMonthTime.get(Calendar.YEAR) == TodayCal.get(Calendar.YEAR) &&
				mGoToMonthTime.get(Calendar.MONTH) == TodayCal.get(Calendar.MONTH)) {
			mHasToday = true;
			mDayOfMonthOfToday = TodayCal.get(Calendar.DAY_OF_MONTH);
			initDayOfMonthCircleDrawable(Color.RED);			
		}
		else {
			if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {
				initDayOfMonthCircleDrawable(Color.BLACK);
			}
		}		
		
		mGoToMonthWeekDayNumberList = goToMonthWeekDayNumberList;
				
		mGoToMonthText = buildMonth(mGoToMonthTime.getTimeInMillis(), mGoToMonthTime.getTimeInMillis());//ETime.buildMonth(mGoToMonthTime.getTimeInMillis(), mTimeZone);
		mGoToMonthTextWidth = mMonthNumPaintBeforeChangeMiniMonth.measureText(mGoToMonthText);
		mPrvMonthLastWeekDaysNumOfGoToMonth = 0;
		String[] dayNumbersOfGoToMonth = mGoToMonthWeekDayNumberList.get(0);
		for (int j=0; j<7; j++) {
			if (dayNumbersOfGoToMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
				mPrvMonthLastWeekDaysNumOfGoToMonth++;
		}		
		mGoToMonthTextFinalX = computeTextXPositionOfMonthListView(mPrvMonthLastWeekDaysNumOfGoToMonth);	
				
		mMonthTextX = 0;
		
		mMonthDayTextLeftOffset = (int)mMonthTextX;
	}	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void setMonthScaleDrawingParams(HashMap<String, Integer> params) {
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
        }			
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        } 
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
        	mMiniMonthIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
        	mMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight); 
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
        	mMiniMonthIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT)) {
        	mMiniMonthIndicatorHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT)) {
        	mMiniMonthNormalWeekHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
        	mMiniMonthDayTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
        	mMiniMonthDayTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);  
        	mMinMonthMonthDayPaint.setTextSize(mMiniMonthDayTextHeight); 
        	mMonthMonthDayPaint.setTextSize(mMiniMonthDayTextHeight);
        }	
        
        if (params.containsKey(PSUDO_MINIMONTH_PARAMS_WEEK_UPPERLINE_ALPHA_VALUE)) {
        	mPsudoMiniMonthWeekUpperlineAlphaValue = params.get(PSUDO_MINIMONTH_PARAMS_WEEK_UPPERLINE_ALPHA_VALUE);         	
        }
        
        if (params.containsKey(PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE)) {
        	mPsudoMiniMonthEventCircleDisappearAlphaValue = params.get(PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE);        	
        } 
        
        if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {        	
        		setOvalDrawableOfDayOfMonthDimens();    			
		}        
        else {    		
        	if (mHasToday) {
        		setOvalDrawableOfDayOfMonthDimens();	       		
        	}        				
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void setMiniMonthScaleDrawingParams(HashMap<String, Integer> params) {
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT);
        }			
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        } 
		
		if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
        	mMiniMonthIndicatorTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
        	mMonthNumPaint.setTextSize(mMiniMonthIndicatorTextHeight); 
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
        	mMiniMonthIndicatorTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
        	mMiniMonthDayTextBaseLineY = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
        	mMiniMonthDayTextHeight = params.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);  
        	mMinMonthMonthDayPaint.setTextSize(mMiniMonthDayTextHeight); 
        }		
        
        //PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE
        if (params.containsKey(PSUDO_MINIMONTH_PARAMS_TEXT_ALPHA_VALUE)) {
        	mPsudoMiniMonthDayAlphaValue = params.get(PSUDO_MINIMONTH_PARAMS_TEXT_ALPHA_VALUE);        	
        } 
        
        //
        if (params.containsKey(PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE)) {
        	mPsudoMiniMonthEventCircleDisappearAlphaValue = params.get(PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE);        	
        } 
        
        if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {        	
    		setOvalDrawableOfDayOfMonthDimens();    			
		}        
	    else {    		
	    	if (mHasToday) {
	    		setOvalDrawableOfDayOfMonthDimens();	       		
	    	}        				
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		//Log.i("tag", "onSizeChanged:mWidth=" + String.valueOf(mWidth) + ", mHeight=" + String.valueOf(mHeight));
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if (!mLaunchScaleDrawingMiniMonthView) {
			drawingScalingMonthListView(canvas);
		}
		else {			
			drawingScalingMiniMonthView(canvas);
		}
		
	}	
	
	float mFraction = 0;
	public void setFraction(float fraction) {
		mFraction = fraction;
	}
	
	int mEventCircleTopPadding;
	public void drawingScalingMonthListView(Canvas canvas) {
		int effectiveWidth = mMiniMonthWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		
		float originalNormalWeekHeight = mMonthListViewNormalWeekHeight;
		float currentScaleNormalWeekHeight = mMiniMonthNormalWeekHeight;		
		float normalWeekAspectRatio = currentScaleNormalWeekHeight / originalNormalWeekHeight;
		
		float radius = 0;
		if (mEventExistence != null) {   
			radius = (currentScaleNormalWeekHeight * 0.1f) / 2;	
			mEventCircleTopPadding = (int) (currentScaleNormalWeekHeight * 0.65f);
		}
		
		float currentScaleMonthHeight = mMiniMonthHeight - mOriginalMiniMonthHeight;//float currentScaleMonthWidth = mMiniMonthWidth - mOriginalMiniMonthWidth;		
		float monthScaleAspectRatio = currentScaleMonthHeight / mScaleHeightSize;//currentScaleMonthWidth / mScaleWidthSize;
		mMonthTextX = mGoToMonthTextFinalX * (1 - mFraction);//monthScaleAspectRatio;
			
		canvas.drawText(mGoToMonthText, mMonthTextX, mMiniMonthIndicatorTextBaseLineY, mMonthNumPaintBeforeChangeMiniMonth);
				
		int monthDayTextTopOffset = mMiniMonthIndicatorHeight;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = mGoToMonthWeekDayNumberList.size();	
		
		int weekLineY = 0;
		int targetDayCount = 0;
		for (int i=0; i<weekNumbers; i++) {			
			
			String[] dayNumbers = mGoToMonthWeekDayNumberList.get(i);		
			monthDayTextY = monthDayTextTopOffset + (i * mMiniMonthNormalWeekHeight) + mMiniMonthDayTextBaseLineY;
			
			int upperLineWidth = 0;			
			for (int j=0; j<7; j++) {
				monthDayTextX = computeTextXPosition(j);				
								
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {					
					canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthMonthDayPaint);
					
					if (mEventExistence != null) {						
						if (mEventExistence[targetDayCount]) {/////////////////////////////////////////////////////////�̰� ������...
							float cx = monthDayTextX;
							float cy = monthDayTextTopOffset + (i * mMiniMonthNormalWeekHeight) + mEventCircleTopPadding;
							mEventExisteneCirclePaint.setAlpha(mPsudoMiniMonthWeekUpperlineAlphaValue);	
							canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
						}
					}
					
					upperLineWidth = upperLineWidth + dayOfWeekTextWidth;
					targetDayCount++;
				}				
			}
			
			if (i==0) {				
				float lineWidth = mUpperLineStrokeWidth * normalWeekAspectRatio;				
				mUpperLinePaint.setStrokeWidth(lineWidth);
				mUpperLinePaint.setAlpha(mPsudoMiniMonthWeekUpperlineAlphaValue);				
				weekLineY = mMiniMonthIndicatorHeight + (int)mUpperLinePaint.getStrokeWidth();
				canvas.drawLine(dayOfWeekTextWidth * mPrvMonthLastWeekDaysNumOfGoToMonth, weekLineY, mMiniMonthWidth, weekLineY, mUpperLinePaint);		        
			}
			else {
				weekLineY = weekLineY + mMiniMonthNormalWeekHeight;
				canvas.drawLine(0, weekLineY, upperLineWidth, weekLineY, mUpperLinePaint);		
			}			
		}	
		
		if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {	
			
			if (mHasToday) {
				Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
				TodayCal.setTimeInMillis(System.currentTimeMillis());
				
				int monthweekNumbers = ETime.getWeekNumbersOfMonth(TodayCal);
				
				int firstDayOfMonthOfSecondWeek = (7 - mPrvMonthLastWeekDaysNumOfGoToMonth) + 1;
				if (mDayOfMonthOfToday < firstDayOfMonthOfSecondWeek) {
					makeSelectedCircleDrawable(canvas, mMonthMonthDayPaint, mDayOfMonthOfToday, mPrvMonthLastWeekDaysNumOfGoToMonth + mDayOfMonthOfToday, mMiniMonthIndicatorHeight);						
				}
				else {
					int firstDayInWeek = firstDayOfMonthOfSecondWeek;
					for (int i=1; i<monthweekNumbers; i++) {
						int lastDayInWeek = firstDayInWeek + 6;
						if (mDayOfMonthOfToday < lastDayInWeek) {
							int todayIndex = mDayOfMonthOfToday - firstDayInWeek;
							int offsetY = mMiniMonthIndicatorHeight + (i * mMiniMonthNormalWeekHeight);
							makeSelectedCircleDrawable(canvas, mMonthMonthDayPaint, mDayOfMonthOfToday, todayIndex, offsetY);
							break;
						}
						else {
							firstDayInWeek = lastDayInWeek + 1;
						}
					}
				}				
			}
			else {
				makeSelectedCircleDrawable(canvas, mMinMonthMonthDayPaint, 1, mPrvMonthLastWeekDaysNumOfGoToMonth, mMiniMonthIndicatorHeight, mPsudoMiniMonthEventCircleDisappearAlphaValue);
				//makeSelectedCircleDrawable(canvas, mMonthMonthDayPaint, 1, mPrvMonthLastWeekDaysNumOfGoToMonth, mMiniMonthIndicatorHeight);
			}
		}
		else {
			if (mHasToday) {
				Calendar TodayCal = GregorianCalendar.getInstance(mTimeZone);
				TodayCal.setTimeInMillis(System.currentTimeMillis());
								
				int monthweekNumbers = ETime.getWeekNumbersOfMonth(TodayCal);
				
				int firstDayOfMonthOfSecondWeek = (7 - mPrvMonthLastWeekDaysNumOfGoToMonth) + 1;
				if (mDayOfMonthOfToday < firstDayOfMonthOfSecondWeek) {
					makeSelectedCircleDrawable(canvas, mMonthMonthDayPaint, mDayOfMonthOfToday, mPrvMonthLastWeekDaysNumOfGoToMonth + mDayOfMonthOfToday, mMiniMonthIndicatorHeight);						
				}
				else {
					int firstDayInWeek = firstDayOfMonthOfSecondWeek;
					for (int i=1; i<monthweekNumbers; i++) {
						int lastDayInWeek = firstDayInWeek + 6;
						if (mDayOfMonthOfToday < lastDayInWeek) {
							int todayIndex = mDayOfMonthOfToday - firstDayInWeek;
							int offsetY = mMiniMonthIndicatorHeight + (i * mMiniMonthNormalWeekHeight);
							makeSelectedCircleDrawable(canvas, mMonthMonthDayPaint, mDayOfMonthOfToday, todayIndex, offsetY);
							break;
						}
						else {
							firstDayInWeek = lastDayInWeek + 1;
						}
					}
				}					
			}
		}
	}
	
	
	public void drawingScalingMiniMonthView(Canvas canvas) {		
		float currentScaleMonthHeight = mMiniMonthHeight - mOriginalMiniMonthHeight;
		//float currentScaleMonthWidth = mMiniMonthWidth - mOriginalMiniMonthWidth;
		//float monthScaleAspectRatio = currentScaleMonthHeight / mScaleHeightSize;//currentScaleMonthWidth / mScaleWidthSize;
		
		mMinMonthMonthDayPaint.setAlpha(mPsudoMiniMonthDayAlphaValue);
		
		mMonthTextX = mGoToMonthTextFinalX * (1 - mFraction);//monthScaleAspectRatio;//monthScaleAspectRatio;
		canvas.drawText(mGoToMonthText, mMonthTextX, mMiniMonthIndicatorTextBaseLineY, mMonthNumPaint);
				
		int monthDayTextTopOffset = mMiniMonthIndicatorTextBaseLineY;
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = mGoToMonthWeekDayNumberList.size();
		for (int i=0; i<weekNumbers; i++) {
			String[] dayNumbers = mGoToMonthWeekDayNumberList.get(i);		
			monthDayTextY = monthDayTextTopOffset + ((i + 1) * mMiniMonthDayTextBaseLineY);
			
			for (int j=0; j<7; j++) {
				monthDayTextX = computeTextXPosition(j);				
								
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {
					int dayOfMonth = Integer.parseInt(dayNumbers[j]);
					int offsetY = mMiniMonthIndicatorTextBaseLineY + (i * mMiniMonthDayTextBaseLineY);
					if (mHasToday) {						
						if (dayOfMonth == mDayOfMonthOfToday) {							
							makeSelectedCircleDrawable(canvas, mMinMonthMonthDayPaint, mDayOfMonthOfToday, j, offsetY);
						}
						else {
							canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMinMonthMonthDayPaint);
						}
					}
					else {
						if (dayOfMonth == 1) {
							if (mMonthViewMode == YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE) {	
								makeSelectedCircleDrawable(canvas, mMinMonthMonthDayPaint, mDayOfMonthOfToday, j, offsetY, mPsudoMiniMonthEventCircleDisappearAlphaValue);
							}
						}
						//else {
						canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMinMonthMonthDayPaint);
						//}										
					}					
					
					//canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMinMonthMonthDayPaint);
				}
			}
		}
		
		
	}
	
	SelectedDateOvalDrawable mOvalDrawableOfDayOfMonth;
    int mOvalDrawableOfDayOfMonthRadius;	
	int mOvalDrawableOfDayOfMonthSize;
	
	public void initDayOfMonthCircleDrawable(int color) {
		mOvalDrawableOfDayOfMonth = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthMonthDayPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
		mOvalDrawableOfDayOfMonth.getPaint().setAntiAlias(true);
		mOvalDrawableOfDayOfMonth.getPaint().setStyle(Style.FILL);
			
		mOvalDrawableOfDayOfMonth.getPaint().setColor(color);		
	}
	
	public void setOvalDrawableOfDayOfMonthDimens(/*Paint paint*/) {
		//mOvalDrawableOfDayOfMonth.setTextSize(paint.getTextSize());
		
		//float radius = (mMiniMonthNormalWeekHeight / 2) / 2;
		float radius = 0;
		if (!mLaunchScaleDrawingMiniMonthView) {
			mOvalDrawableOfDayOfMonth.setTextSize(mMonthMonthDayPaint.getTextSize());
			radius = (mMiniMonthNormalWeekHeight / 2) / 2;
		}
		else {			
			mOvalDrawableOfDayOfMonth.setTextSize(mMinMonthMonthDayPaint.getTextSize());
			radius = mMiniMonthDayTextBaseLineY * 0.4f;
		}		
		
		mOvalDrawableOfDayOfMonthRadius = (int)radius;
		mOvalDrawableOfDayOfMonthSize = (int)(mOvalDrawableOfDayOfMonthRadius * 2);		
	}
	
	public void makeSelectedCircleDrawable (Canvas canvas, Paint paint, int dateNum, int xPos, int offsetY) {
		float baseLineY = mMiniMonthDayTextBaseLineY;
    	
    	float textAscent = paint.ascent();
    	float textDescent = paint.descent();
    	
    	float textTopY = baseLineY + textAscent;//float textTopY = Math.abs(baseLineY + textAscent); 	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    			
		int x = computeTextXPosition(xPos);
		int left = x - mOvalDrawableOfDayOfMonthRadius;	    			
		int top = (int) (textCenterY - mOvalDrawableOfDayOfMonthRadius);
		int right = left + mOvalDrawableOfDayOfMonthSize;
		int bottom = top + mOvalDrawableOfDayOfMonthSize;
		
		//mOvalDrawableOfDayOfMonth.setTextAlpha(mPsudoMiniMonthAlphaValue);
		
		//mOvalDrawableOfDayOfMonth.getPaint().setAlpha(mPsudoMiniMonthAlphaValue);
		mOvalDrawableOfDayOfMonth.setBounds(left, top, right, bottom);			       
		mOvalDrawableOfDayOfMonth.setDrawTextPosition(x, offsetY + mMiniMonthDayTextBaseLineY);
		mOvalDrawableOfDayOfMonth.setDayOfMonth(dateNum);
		mOvalDrawableOfDayOfMonth.draw(canvas);   		
	}
	
	public void makeSelectedCircleDrawable (Canvas canvas, Paint paint, int dateNum, int xPos, int offsetY, int alpha) {
		float baseLineY = mMiniMonthDayTextBaseLineY;
    	
    	float textAscent = paint.ascent();
    	float textDescent = paint.descent();
    	
    	float textTopY = baseLineY + textAscent;
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    			
		int x = computeTextXPosition(xPos);
		int left = x - mOvalDrawableOfDayOfMonthRadius;	    			
		int top = (int) (textCenterY - mOvalDrawableOfDayOfMonthRadius);
		int right = left + mOvalDrawableOfDayOfMonthSize;
		int bottom = top + mOvalDrawableOfDayOfMonthSize;
		
		mOvalDrawableOfDayOfMonth.setTextAlpha(alpha);
		
		mOvalDrawableOfDayOfMonth.getPaint().setAlpha(alpha);
		mOvalDrawableOfDayOfMonth.setBounds(left, top, right, bottom);			       
		mOvalDrawableOfDayOfMonth.setDrawTextPosition(x, offsetY + mMiniMonthDayTextBaseLineY);
		mOvalDrawableOfDayOfMonth.setDayOfMonth(dateNum);
		mOvalDrawableOfDayOfMonth.draw(canvas);   		
	}
	
	
	private int computeTextXPosition(int day) {
		int x = 0;
		int effectiveWidth = mMiniMonthWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
	    return x;
	}
	
	private int computeTextXPositionOfMonthListView(int day) {
		// ������ ����� �h�� month view�� month indicator text�� text align�� 
		// mini month�� month indicator text�� aling�� �ٸ��Ƿ�
		// �߰��� text align�� �����ϸ� month indicator text�� �ڷ� �̵��Ͽ��ٰ� �ٽ� �����̹Ƿ�
		// �ʹݿ� �ƿ� ������ ������
		// �ش� ���� width�� �����ؼ� ������
		// �׷��� mini month�� text align�� ������!!!
		int x = 0;
		int effectiveWidth = mMonthListViewWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
		int halfWidth = (int) (mGoToMonthTextWidth / 2);
		x = x - halfWidth;
	    return x;
	}
	
	
	private String buildMonth(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			mContext,
                mFormatter,
                startMillis,
                endMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_NO_YEAR, mTzId).toString();
    	
    	return date;    	
    }	  
}
