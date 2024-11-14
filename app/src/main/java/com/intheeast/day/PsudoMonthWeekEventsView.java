
package com.intheeast.day;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.SelectedDateOvalDrawable;
import com.intheeast.month.MonthWeekView;


public class PsudoMonthWeekEventsView extends View {
	private static boolean INFO = true;
    private static final String TAG = "PsudoMonthWeekEventsView";   
    
    
    public static final int NORMAL_WEEK_PATTERN = 0;    
    public static final int TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 1;   
    public static final int FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN = 2;    
    public static final int LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN = 3;                            
    public static final int NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN = 4;                             
    public static final int MONTH_MONTHIDICATOR_WEEK_PATTERN = 5;
    
    public static final int SELECTED_NORMAL_WEEK_PATTERN = 11;
    public static final int SELECTED_PREVIOUS_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN = 12;    
    public static final int SELECTED_NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN = 13;
    public static final int SELECTED_LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN = 14;


    //private static final boolean DEBUG_LAYOUT = false;   

    /* NOTE: these are not constants, and may be multiplied by a scale factor */
    private static int TEXT_SIZE_MONTH_NUMBER = 32;   
    
    private static int MIN_WEEK_WIDTH = 50;

    private static int TODAY_HIGHLIGHT_WIDTH = 2;

    private static int SPACING_WEEK_NUMBER = 24;
    private static boolean mInitialized = false;
    //private static boolean mShowDetailsInMonth;

    //int mTodayIndex;
    public Calendar mToday = GregorianCalendar.getInstance();
    public boolean mHasToday = false;
    public int mTodayIndex = -1;    
    public List<ArrayList<Event>> mEvents = null;
    public ArrayList<Event> mUnsortedEvents = null;
    

    public static StringBuilder mStringBuilder = new StringBuilder(50);
    // TODO recreate formatter when locale changes
    public static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

    public Paint mMonthNamePaint;
    
    public Drawable mTodayDrawable;

    public int mMonthNumHeight;
    public int mMonthNumAscentHeight;
    
    public int mMonthBGTodayColor;
    public int mMonthNumColor;
    
    public int mMonthNumTodayColor;
    public int mMonthNameColor;
    
    private int mSelectedDayIndex = -1;
    public int mTodayAnimateColor;
        
    int mViewMode = 0;
    int mOriginalListViewHeight = 0;
    int mNormalWeekHeight = 0;
    //int mPreviousWeekHeight = 0;
    int mMonthIndicatorHeight = 0;
    int mDateTextSize = 0;
    int mDateTextTopPadding = 0;
    int mNormalModeDateTextTopPadding = 0;
    float mMonthTextSize = 0;
    float mMonthTextBottomPadding = 0;
    int mEventCircleTopPadding = 0;
    int mNormalModeEventCircleTopPadding = 0;
    int mWeekPattern;
    int mNumCells;
    String mTzId;
    int mHeight;
    
    //int mSelectedDay;
    //boolean mHasSelectedDay;
    String[] mDayNumbers;
    int mWeek;
    //int mWeekStart;
    //int mFirstJulianDay;
    //int mLastJulianDay;
    Calendar mFirstMonthTime;
    Calendar mLastMonthTime;
    int mFirstMonth;
    int mLastMonth;
    int mPrvMonthWeekDaysNum;
    
    // The left edge of the selected day
    int mSelectedLeft = -1;
    // The right edge of the selected day
    int mSelectedRight = -1;
    
    Context mContext;
    
    float mDateIndicatorY;
    int mWeek_saturdayColor;
	int mWeek_sundayColor;
    //TextView mDateIndicatorTextView;
    /**
     * Shows up as an error if we don't include this.
     */
    public PsudoMonthWeekEventsView(Context context) {
        super(context);
        
        mContext = context;
        
        initView();
    }

    // Sets the list of events for this week. Takes a sorted list of arrays
    // divided up by day for generating the large month version and the full
    // arraylist sorted by start time to generate the dna version.
    public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        // The MIN_WEEK_WIDTH is a hack to prevent the view from trying to
        // generate dna bits before its width has been fixed.
        //createDna(unsortedEvents);//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        createEventExistenceCircle(unsortedEvents);
    }  
    
    boolean mEventExistence[] = null;
    int mWidth;
    public void createEventExistenceCircle(ArrayList<Event> unsortedEvents) {
    	if (unsortedEvents == null || /*mWidth <= MIN_WEEK_WIDTH || */getContext() == null) {
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

    int mNumDays;
    @SuppressLint("Range")
    public void setEvents(List<ArrayList<Event>> sortedEvents) {
        mEvents = sortedEvents;
        if (sortedEvents == null) {
            return;
        }
        if (sortedEvents.size() != mNumDays) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.wtf(TAG, "Events size must be same as days displayed: size="
                        + sortedEvents.size() + " days=" + mNumDays);
            }
            mEvents = null;
            return;
        }
    }

    public void loadColors(Context context) {
        Resources res = context.getResources();
        
        mMonthNumColor = res.getColor(R.color.month_day_number);        
        mMonthNumTodayColor = res.getColor(R.color.month_today_number);
        mMonthNameColor = mMonthNumColor;        
        mMonthBGTodayColor = res.getColor(R.color.month_today_bgcolor);        
        mTodayAnimateColor = res.getColor(R.color.today_highlight_color);
        mTodayDrawable = res.getDrawable(R.drawable.today_blue_week_holo_light);
        mWeek_saturdayColor = getResources().getColor(R.color.week_saturday);
        mWeek_sundayColor = getResources().getColor(R.color.week_sunday);
    }

    Rect r = new Rect();
    //Paint p = new Paint();
    Paint mMonthNumPaint;
    Paint mEventExisteneCirclePaint;
    Paint mUpperLinePaint;
    Paint mSelectedWeekAlphaPaint;
    Paint mDateIndicatorPaint;
    float mScale;
    int mFirstDayOfWeek;
    public void initView() {
    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);      
    	
        mEventExisteneCirclePaint = new Paint();    
        mEventExisteneCirclePaint.setAntiAlias(true);        
        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
        mEventExisteneCirclePaint.setStyle(Style.FILL);        
        
        mUpperLinePaint = new Paint();        
        mUpperLinePaint.setAntiAlias(true);        
        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
        //mUpperLinePaint.setColor(Color.RED); // for test
        mUpperLinePaint.setStyle(Style.STROKE);
        float strokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
        mUpperLinePaint.setStrokeWidth(strokeWidth);

        if (!mInitialized) {
            Resources resources = getContext().getResources();            
            TEXT_SIZE_MONTH_NUMBER = resources.getInteger(R.integer.text_size_month_number);
            
            if (mScale != 1) {               
                SPACING_WEEK_NUMBER *= mScale;
                TEXT_SIZE_MONTH_NUMBER *= mScale;                         
                TODAY_HIGHLIGHT_WIDTH *= mScale;
            }
            
            mInitialized = true;
        }
        
        loadColors(getContext());
        // TODO modify paint properties depending on isMini

        mMonthNumPaint = new Paint();
        mMonthNumPaint.setFakeBoldText(false);
        mMonthNumPaint.setAntiAlias(true);
        mMonthNumPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);
        mMonthNumPaint.setColor(mMonthNumColor);
        mMonthNumPaint.setStyle(Style.FILL);        
        mMonthNumPaint.setTextAlign(Align.CENTER);
        mMonthNumPaint.setTypeface(Typeface.DEFAULT);        
        
        mSelectedWeekAlphaPaint = new Paint();
        mSelectedWeekAlphaPaint.setFakeBoldText(false);
        mSelectedWeekAlphaPaint.setAntiAlias(true);
        mSelectedWeekAlphaPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);
        mSelectedWeekAlphaPaint.setColor(mMonthNumColor);
        mSelectedWeekAlphaPaint.setStyle(Style.FILL);        
        mSelectedWeekAlphaPaint.setTextAlign(Align.CENTER);
        mSelectedWeekAlphaPaint.setTypeface(Typeface.DEFAULT);      
        
        
        mDateIndicatorPaint = new Paint();
        mDateIndicatorPaint.setAntiAlias(true);
        mDateIndicatorPaint.setTextSize(getResources().getDimension(R.dimen.dateindicator_text_height)); 
        
        mDateIndicatorPaint.setColor(getResources().getColor(R.color.secondaryActionBarDateIndicatorTextColor));        
        mDateIndicatorPaint.setTextAlign(Align.CENTER);
		
        mMonthNumAscentHeight = (int) (-mMonthNumPaint.ascent() + 0.5f);
        mMonthNumHeight = (int) (mMonthNumPaint.descent() - mMonthNumPaint.ascent() + 0.5f);    	
    }  
    
    TimeZone mTimeZone;
    long mGMToff;
    public void setWeekParams(HashMap<String, Integer> params, String tz) {           
        
    	mTzId = tz;
    	mTimeZone = TimeZone.getTimeZone(mTzId);
    	mGMToff = mTimeZone.getRawOffset() / 1000;
        // We keep the current value for any params not present
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_HEIGHT)) {
            mHeight = params.get(MonthWeekView.VIEW_PARAMS_HEIGHT);   
            
            int monthDayHeaderHeight = (int) (mHeight * CalendarViewsSecondaryActionBar.MONTHDAY_HEADER_HEIGHT_RATIO);
        	int dateIndicatorHeight = (int) (mHeight * CalendarViewsSecondaryActionBar.DATEINDICATOR_HEADER_HEIGHT_RATIO);
        	int halfOfdateIndicatorHeight = dateIndicatorHeight / 2;
        	
        	float textAscent = mDateIndicatorPaint.ascent();
        	float textDescent = mDateIndicatorPaint.descent();
        	float textHeight = Math.abs(textAscent) + textDescent; 
        	int halfOfTextHeight = (int) (textHeight / 2);        	
        	
        	mDateIndicatorY = monthDayHeaderHeight + halfOfdateIndicatorHeight + halfOfTextHeight;    	            
                     
        }
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_NUM_DAYS)) {
            mNumDays = params.get(MonthWeekView.VIEW_PARAMS_NUM_DAYS);
        }   
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_START)) {
        	// VIEW_PARAMS_WEEK_START -> mFirstDayOfWeek
        	mFirstDayOfWeek = params.get(MonthWeekView.VIEW_PARAMS_WEEK_START);
        }    
        
        mNumCells = mNumDays;
        
        mFirstMonthTime = GregorianCalendar.getInstance(mTimeZone);
        mLastMonthTime = GregorianCalendar.getInstance(mTimeZone);
        Calendar time = GregorianCalendar.getInstance(mTimeZone);
        
        mFirstMonthTime.setFirstDayOfWeek(mFirstDayOfWeek);
        mLastMonthTime.setFirstDayOfWeek(mFirstDayOfWeek);
        time.setFirstDayOfWeek(mFirstDayOfWeek);
        
        // Allocate space for caching the day numbers and focus values
        mDayNumbers = new String[mNumCells];        
        mWeek = params.get(MonthWeekView.VIEW_PARAMS_WEEK);///////////////////////////////////////////////////////////////
        int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(mWeek);
               
        long julianMondayMillis = ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek);
        time.setTimeInMillis(julianMondayMillis);
        ETime.adjustStartDayInWeek(time);
                    
        mFirstMonthTime.setTimeInMillis(time.getTimeInMillis());                
        mFirstMonth = time.get(Calendar.MONTH);  
        
        Calendar today = GregorianCalendar.getInstance(mTimeZone);
        today.setTimeInMillis(System.currentTimeMillis());
        mHasToday = false;
        mTodayIndex = -1;
        
        int i = 0;        
        while(true) {
        	                        
            if (time.get(Calendar.YEAR) == today.get(Calendar.YEAR) && time.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                mHasToday = true;   
                mTodayIndex = i;
            }
            
            mDayNumbers[i] = Integer.toString(time.get(Calendar.DAY_OF_MONTH));
            
            i++;
            
            if (i < mNumCells)
            	time.add(Calendar.DAY_OF_MONTH, 1);
            else 
            	break;
        }
        
        
        mLastMonth = time.get(Calendar.MONTH);
        mLastMonthTime.setTimeInMillis(time.getTimeInMillis());
        
        setTag(params);   
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_MODE)) {
        	mViewMode = params.get(MonthWeekView.VIEW_PARAMS_MODE);           
        } 
        else {
        	mViewMode = MonthWeekView.NORMAL_MODE_VIEW_PATTERN;
        }
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT)) {
        	mOriginalListViewHeight = params.get(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT);
        	mDateTextSize = (int) (mOriginalListViewHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT);
        	// mOriginalListViewHeight * 0.08f : original month indicator height
        	mMonthTextSize = (mOriginalListViewHeight * 0.08f) * 0.5f;        	
        }
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT)) {
        	mMonthIndicatorHeight = params.get(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT);
        	// mListViewMonthIndicatorHeight * (1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT)
        	mMonthTextBottomPadding = (float)(mMonthIndicatorHeight * 
        			(1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT)); // month indicator height�� 75%
        } 
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT)) {
        	mNormalWeekHeight = params.get(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT);
        	if (mViewMode == MonthWeekView.NORMAL_MODE_VIEW_PATTERN) {             	
            	mDateTextTopPadding = (int) (mNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);            	
            	mEventCircleTopPadding = (int) (mNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            	
            }
            else {
            	mNormalModeDateTextTopPadding = (int) (mNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            	mDateTextTopPadding = (int) (mNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);  
            	mNormalModeEventCircleTopPadding = (int) (mNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            	mEventCircleTopPadding = (int) (mNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            }        	
        }        
        
        if (params.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN)) {
        	mWeekPattern = params.get(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN);
        }     
    }

    
    
    boolean mSetWillDraw = false;
    int mDayOfWeekTextWidth;
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;        
        
        mDayOfWeekTextWidth = mWidth / 7;  
    }
    
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight);
    }
    
    
    @Override
    public void onDraw(Canvas canvas) {  
    	//super.onDraw(canvas);    	
    	
    	//////////////////////////////////////////////////////////////////////////////////////////////
    	// mSelectedDayIndex != -1 �� �ƴ϶�� ����
    	// �ش� Week View�� selectedWeekView��� ���� �ǹ��Ѵ�
    	//////////////////////////////////////////////////////////////////////////////////////////////
    	
    	drawWeekNums(canvas);        
        
        /*if (mHasToday) {
        	   		
        	// ���� �߻� : 2015/06/28~2015/07/04 Week���� selectedDay�� 06/30�� �� 
        	//          �� selectedWeekView�� ������
        	//          07/03 today�� �����Ǵ� ������ �߻�
    		if (mSelectedDayIndex != -1) {
    			
	        	Calendar todayCalendar = GregorianCalendar.getInstance(mTimeZone);
	        	Calendar selectedDayCalendar = GregorianCalendar.getInstance(mTimeZone);
	        	
	        	todayCalendar.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
	        	todayCalendar.add(Calendar.DAY_OF_MONTH, mTodayIndex);
	        	
	        	selectedDayCalendar.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
	        	selectedDayCalendar.add(Calendar.DAY_OF_MONTH, mSelectedDayIndex);
	        	
	        	if (todayCalendar.get(Calendar.YEAR) == selectedDayCalendar.get(Calendar.YEAR) &&
	        			todayCalendar.get(Calendar.MONTH) == selectedDayCalendar.get(Calendar.MONTH) &&
	        			todayCalendar.get(Calendar.DAY_OF_MONTH) == selectedDayCalendar.get(Calendar.DAY_OF_MONTH) ) {//if (todayJulainDay != selectedJulainDay)	        		
	        		
	        		// ���� selected day�� today�� ���ٸ� drawSelectedDayCircle�� ȣ���ؼ��� �ȵȴ�
	        		drawToday(canvas, false); // ���� selected day�� today�� ���ٸ� alpah blending�� �ؼ��� �ȵȴ�	
	        	}
	        	else {    		
	        		
	        		drawToday(canvas, true); // today alpha blending�� �ؾ� �Ѵ� ���� �����̳� �ƴ� ����� Ŀ���� anim�̳�?: �׳� alpha blending�� ����
                    // �׷��� ��� �� ���ΰ�? �ش� ��� selectedWeekView�̱� ������ mSelectedWeekAlphaValue�� ����ϸ�ȴ� 		                         
	        		
	        		drawSelectedDayCircle(canvas);    	     
	        	}
    		}
    		else {
    			drawToday(canvas, false); // �׷��� alpha animation ���� ��� ���� ���ΰ�? 
                						  // �ش� ��� selectedWeekView�� �ƴϱ� ������ mSelectedWeekAlphaValue�� ����� �� ����
    			                          // :���� ���� �غ��� alpha anim�� user�� �� �� ���� ���̴�
    			                          //  selectedWeekView�� ��� �ʹݺ��� �������� �ʱ� �����̴�
    		}  		
    		
        } 
        else {*/
        	if (mSelectedDayIndex != -1) {
        		drawSelectedDayCircle(canvas);
        	}
        //}
         
        if (mEventExistence != null) {     	
        	drawEventExistenceCircle(canvas); 
        }
        
    }
	
    
    public void drawWeekNums(Canvas canvas) {
    	float startX = 0;
    	float startY = 0;
    	float stopX = 0;
    	float stopY = 0;
    	
    	if (mWeekPattern == NORMAL_WEEK_PATTERN || mWeekPattern == LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {            
            startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
			stopX = mWidth;		
			
			drawUpperLine(canvas, startX, startY, stopX, stopY);		
			
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays;
			int textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);  
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}
        }    	
    	else if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
    		startY = stopY = (int)mMonthIndicatorHeight; 	     
			stopX = mWidth;
			
			drawUpperLine(canvas, startX, startY, stopX, stopY);
			        
			float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
			drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mFirstMonthTime.getTimeInMillis()), 0, monthIndicatorTextY);		
			
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays;
			int textY = mMonthIndicatorHeight + mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, mMonthIndicatorHeight, false);
	        	}
			}
        }    	
    	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		startY = stopY = (int)mUpperLinePaint.getStrokeWidth();   		
  		  
    		calPrvMonthWeekDaysNum();	 
    		stopX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;    		
    		
    		drawUpperLine(canvas, startX, startY, stopX, stopY);
    		
    		startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
    		stopX = mWidth;
    		startY = stopY =  mNormalWeekHeight + mMonthIndicatorHeight;
    		
    		drawUpperLine(canvas, startX, startY, stopX, stopY);
    		    		           
    		float monthIndicatorTextY = mNormalWeekHeight + mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY);    		
			
			int firstDayIndex = 0;
			int lastDayIndex = mPrvMonthWeekDaysNum;
			int textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}
						
			firstDayIndex = mPrvMonthWeekDaysNum;
			lastDayIndex = mNumDays;
			textY = mNormalWeekHeight + mMonthIndicatorHeight + mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, mNormalWeekHeight + mMonthIndicatorHeight, false);
	        	}
			}
        }    	
    	else if (mWeekPattern == MONTH_MONTHIDICATOR_WEEK_PATTERN) {
    		// �� ���� ��Ȳ���� MONTH_MONTHIDICATOR_WEEK_PATTERN�� ����ȴ�
    		// 1.mSelectedWeekPattern == PsudoMonthWeekEventsView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN    		
    		// 2.mSelectedWeekPattern == PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN
    		
    		float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
    		if (mFirstMonthTime.get(Calendar.MONTH) != mLastMonthTime.get(Calendar.MONTH)) {
    			calPrvMonthWeekDaysNum();                  
        		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY); 
    		}
    		else {
    			if ( (mFirstMonthTime.get(Calendar.DAY_OF_MONTH) == 1) ) {
    				//FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN
    				drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mFirstMonthTime.getTimeInMillis()), 0, monthIndicatorTextY);	
    			}  			
    		}            
    	}    
    	else if (mWeekPattern == NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN){
    		//drawNextMonthWeekNumsOfTDMWPattern(canvas);
    		calPrvMonthWeekDaysNum();             
            float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY); 
    		
    		startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
    		stopX = mWidth;
    		startY = stopY =  mMonthIndicatorHeight;    		
    		drawUpperLine(canvas, startX, startY, stopX, stopY);
    		    		
    		int firstDayIndex = mPrvMonthWeekDaysNum;
			int lastDayIndex = mNumDays;
			float textY = mMonthIndicatorHeight + mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, (int)textY, false, 0);
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, mMonthIndicatorHeight, false);
	        	}
			}
    	}     		
    	else if (mWeekPattern == SELECTED_NORMAL_WEEK_PATTERN) {
    		//drawSelectedNormalWeekNums(canvas);
    		             
            startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
			stopX = mWidth;
			
			if (mDrawUpperLineOfSelectedWeekView)
				drawUpperLine(canvas, startX, startY, stopX, stopY);	
                        
            int firstDayIndex = 0;
			int lastDayIndex = mNumDays;
			int textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0); 
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}
            
            if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {            	
            	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
            	float dateIndicatorX = mWidth / 2;        	
            	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);        	
            }
            
    	}    	
    	else if (mWeekPattern == SELECTED_PREVIOUS_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN) {    		
            calPrvMonthWeekDaysNum();     
            
            if (mDrawUpperLineOfSelectedWeekView) {            	
            	startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
    			stopX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
    			drawUpperLine(canvas, startX, startY, stopX, stopY);           	
            }       
            
        	// prv Month�� last week�� ���� day�� ���õǾ����Ƿ�
        	int firstDayIndex = 0;
			int lastDayIndex = mPrvMonthWeekDaysNum;
			int textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0); 	        	
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}

        	// next Month�� first week�� ������ ������� �Ѵ�
        	firstDayIndex = mPrvMonthWeekDaysNum;
			lastDayIndex = mNumDays;
			textY = mDateTextTopPadding;
			if (INFO) Log.i(TAG, "SPMW:mSelectedWeekAlphaValue=" + String.valueOf(mSelectedWeekAlphaValue));
			
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, true, mSelectedWeekAlphaValue);   
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}

        	if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {        		
            	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
            	float dateIndicatorX = mWidth / 2;        	
            	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);            	
            }
    	}
    	else if (mWeekPattern == SELECTED_NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN) {    		
            calPrvMonthWeekDaysNum();
            
            if (mDrawUpperLineOfSelectedWeekView) {
            	startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
            	stopX = mWidth;
            	startY = stopY = (int)mUpperLinePaint.getStrokeWidth();    			
    			drawUpperLine(canvas, startX, startY, stopX, stopY);   			
            }       
            
        	// prv Month�� last week�� ���� day�� ���õǾ����Ƿ�
        	int firstDayIndex = 0;
			int lastDayIndex = mPrvMonthWeekDaysNum;
			int textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, true, mSelectedWeekAlphaValue); 	
			
			if (INFO) Log.i(TAG, "SNMW:mSelectedWeekAlphaValue=" + String.valueOf(mSelectedWeekAlphaValue));
			
        	// next Month�� first week�� ������ ������� �Ѵ�
        	firstDayIndex = mPrvMonthWeekDaysNum;
			lastDayIndex = mNumDays;
			textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);  
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}

            if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {        	
            	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
            	float dateIndicatorX = mWidth / 2;        	
            	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);            	
            }
    	}
    	else if (mWeekPattern == SELECTED_LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {    		                        
            if (mDrawUpperLineOfSelectedWeekView) {
            	startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
            	stopX = mWidth;		
			
            	drawUpperLine(canvas, startX, startY, stopX, stopY);
            }
			
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays;
			int textY = mDateTextTopPadding;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);  
			if (mHasToday) {
				if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
					drawToday(canvas, 0, false);
	        	}
			}

            if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {           	
            	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
            	float dateIndicatorX = mWidth / 2;        	
            	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);            	
            }
    	}
    }
    // TODO move into MonthWeekView
    // Computes the x position for the left side of the given day    
    private int computeDayLeftPosition(int day) {
    	int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;	
		
		x = (day * dayOfWeekTextWidth);
        return x;
    }
    
    private int computeTextXPosition(int day) {
    	int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
        return x;
    }
    

    SelectedDateOvalDrawable mSelectedDateDrawable;
    SelectedDateOvalDrawable mSelectedDateTodayDrawable;

    int mSelectedCircleDrawableRadius;
	int mSelectedCircleDrawableCenterY;
	int mSelectedCircleDrawableSize;
    public void makeSelectedCircleDrawableDimens(int offsetY) {
		float radius = (mNormalWeekHeight / 2) / 2;
		mSelectedCircleDrawableRadius = (int)radius;
		mSelectedCircleDrawableSize = (int)(mSelectedCircleDrawableRadius * 2);
		
		Paint paint = new Paint();
    	paint.setAntiAlias(true);        
    	paint.setColor(Color.GREEN);
    	paint.setStyle(Style.STROKE);
    	paint.setStrokeWidth(0); // hair line���� �׽�Ʈ�ؾ� �ǹ̰� �ִ�
        
    	float baseLineY = mDateTextTopPadding;
    	
    	float textAscent = mMonthNumPaint.ascent();
    	float textDescent = mMonthNumPaint.descent();
    	
    	float textTopY = baseLineY + textAscent; 	
    	
    	//float textBottomY = baseLineY + textDescent;
    	
    	// text height = Math.abs(textAscent) + textDescent
    	// text center y pos = textTopY + (text height / 2)
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = offsetY + (textTopY + (textHeight / 2));
    	
    	mSelectedCircleDrawableCenterY = (int) textCenterY;   	
	} 

       
    
    
    public void calPrvMonthWeekDaysNum() {
		int maxMonthDays = mFirstMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);       
        mPrvMonthWeekDaysNum = (maxMonthDays - mFirstMonthTime.get(Calendar.DAY_OF_MONTH)) + 1;   
	}
		
	public long calMonthIndicatorTimeMillis(long millis) {
		Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);        
		monthIndicatorTime.setTimeInMillis(millis);     
		monthIndicatorTime.set(monthIndicatorTime.get(Calendar.YEAR), monthIndicatorTime.get(Calendar.MONTH), 1);      
		long monthIndicatorTimeMillis = monthIndicatorTime.getTimeInMillis();
		
		return monthIndicatorTimeMillis;
	}
	
	public void drawMonthIndicator(Canvas canvas, long monthIndicatorTimeMillis, int textXPosition, float textY) {
		
        String monthText = buildMonth(monthIndicatorTimeMillis, monthIndicatorTimeMillis);
        
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(mMonthNumColor);
        monthTextPaint.setTextAlign(Align.CENTER);        
        monthTextPaint.setTextSize(mMonthTextSize);
        
        float textX = computeTextXPosition(textXPosition);        
        canvas.drawText(monthText, textX, textY, monthTextPaint);           
	}
	
	public void drawUpperLine(Canvas canvas, float startX, float startY, float stopX, float stopY) {
		canvas.drawLine(startX, startY, stopX, stopY, mUpperLinePaint);		
	}
    
	public void drawDayOfMonth(Canvas canvas, int firstDayIndex, int lastDayIndex, int textY, boolean alphaBlending, int alphaValue) {
		       
        int i = firstDayIndex;   
		int end = lastDayIndex;
		int y = textY; 
		
		mMonthNumPaint.setTextSize(mDateTextSize);     
        
        int prvAlphaValue = 0;
        if (alphaBlending) {
        	prvAlphaValue = mMonthNumPaint.getAlpha();        	
        }          
        
        for (; i < end; i++) {    
        	int color;
        	final int column = i % 7;
            if (Utils.isSaturday(column, mFirstDayOfWeek)) {
                color = mWeek_saturdayColor;
            } else if (Utils.isSunday(column, mFirstDayOfWeek)) {
                color = mWeek_sundayColor;
            }
            else {
            	color = mMonthNumColor;
            }
            
            // mSelectedDayIndex !=-1��� ���� 
            // ���� PsudoMonthWeekEventsView�� slected week��� ���̴�
            if (mHasToday && mSelectedDayIndex !=-1) {            	
            	if (mTodayIndex == i && mTodayIndex != mSelectedDayIndex) {
            		color = Color.RED;
            	}
            }
            
            mMonthNumPaint.setColor(color);
            
            if (alphaBlending) {
            	mMonthNumPaint.setAlpha(alphaValue);
            }
            
            canvas.drawText(mDayNumbers[i], computeTextXPosition(i), y, mMonthNumPaint);            
        } 
        
        
        
        if (alphaBlending) {
        	mMonthNumPaint.setAlpha(prvAlphaValue);
        }        
        
	}
    
    private String buildMonth(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			getContext(),
                mFormatter,
                startMillis,
                endMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_NO_YEAR, mTzId).toString();
    	
    	return date;    	
    }
    
    public void drawFirstMonthDayWeekNums(Canvas canvas) {
    	int y;

        int i = 0;        
        int x = 0;
        int numCount = mNumDays;        
                
        mMonthNumPaint.setTextSize(mDateTextSize);        
        
        Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);//Time monthIndicatorTime = new Time(mTimeZone);
        //long firstJulianDayMillis = ETime.getMillisFromJulianDay(mFirstJulianDay, mTimeZone);
        monthIndicatorTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());//monthIndicatorTime.setJulianDay(mFirstJulianDay);
        
        long mills = monthIndicatorTime.getTimeInMillis();
        String monthText = buildMonth(mills, mills);//ETime.buildMonth(mills, mTimeZone);
        
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(Color.BLACK);
        monthTextPaint.setTextAlign(Align.CENTER);
        
        monthTextPaint.setTextSize(mMonthTextSize);
        float textX = computeTextXPosition(0);
        float textY = mMonthIndicatorHeight - mMonthTextBottomPadding;        
        canvas.drawText(monthText, textX, textY, monthTextPaint);        
        
        int upperLineY = mMonthIndicatorHeight;
        canvas.drawLine(0, upperLineY, mWidth, upperLineY, mUpperLinePaint);
        
        y = mMonthIndicatorHeight + mDateTextTopPadding;
        
        boolean isBold = false;
        mMonthNumPaint.setColor(mMonthNumColor);
        for (; i < numCount; i++) {                           
            mMonthNumPaint.setColor(mMonthNumColor);            
            x = computeTextXPosition(i);
            
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }
    }
    
    
    public void drawTwoDiffMonthDayWeekNums(Canvas canvas) {
    	mPrvMonthWeekDaysNum = 0; // reset
    	
    	int prvWeekY;
    	int nextWeekY;

    	int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		
        int i = 0;       
        
        int x = 0;
        int numCount = mNumDays;        
        
        mMonthNumPaint.setTextSize(mDateTextSize);
                
        Calendar prvWeekMonthTime = GregorianCalendar.getInstance(mTimeZone);//Time prvWeekMonthTime = new Time(mTimeZone);
        //long firstJulianDayMillis = ETime.getMillisFromJulianDay(mFirstJulianDay, mTimeZone);
        prvWeekMonthTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());//prvWeekMonthTime.setJulianDay(mFirstJulianDay);
        
        
        int maxMonthDays = prvWeekMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);       
        mPrvMonthWeekDaysNum = (maxMonthDays - prvWeekMonthTime.get(Calendar.DAY_OF_MONTH)) + 1;
        prvWeekY = mDateTextTopPadding;
        int weekLineY = (int)mUpperLinePaint.getStrokeWidth();
        canvas.drawLine(0, weekLineY, dayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mUpperLinePaint);
        
        for (; i < mPrvMonthWeekDaysNum; i++) {            
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, prvWeekY, mMonthNumPaint);            
        }            
        
        Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);//Time monthIndicatorTime = new Time(mTimeZone);
        monthIndicatorTime.setTimeInMillis(prvWeekMonthTime.getTimeInMillis());
        
        monthIndicatorTime.add(Calendar.DAY_OF_MONTH, mPrvMonthWeekDaysNum);//monthIndicatorTime.monthDay += mPrvMonthWeekDaysNum;
        
        long mills = monthIndicatorTime.getTimeInMillis();
        String monthText = buildMonth(mills, mills);//ETime.buildMonth(mills, mTimeZone);
        
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(Color.BLACK);
        monthTextPaint.setTextAlign(Align.CENTER);        
        monthTextPaint.setTextSize(mMonthTextSize);
        float textX = computeTextXPosition(mPrvMonthWeekDaysNum);
        float textY = mNormalWeekHeight + mMonthIndicatorHeight - mMonthTextBottomPadding;
        canvas.drawText(monthText, textX, textY, monthTextPaint);        
        weekLineY = mNormalWeekHeight + mMonthIndicatorHeight;
        canvas.drawLine(dayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mWidth, weekLineY, mUpperLinePaint);        
        
        nextWeekY = mNormalWeekHeight + mMonthIndicatorHeight + mDateTextTopPadding;
        
        boolean isBold = false;
        mMonthNumPaint.setColor(mMonthNumColor);
        for (; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, nextWeekY, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }
    }
    
    public void testMonthDayTextHeight(Canvas canvas, int y) {
    	Paint paint = new Paint();
    	paint.setAntiAlias(true);        
    	paint.setColor(Color.GREEN);
    	paint.setStyle(Style.STROKE);
    	paint.setStrokeWidth(0); // hair line���� �׽�Ʈ�ؾ� �ǹ̰� �ִ�
        
    	// y ���� baseline�� y���̴�
    	canvas.drawLine(0, y, mWidth, y, paint);  // for test
    	
    	float textAscent = mMonthNumPaint.ascent();
    	float textDescent = mMonthNumPaint.descent();
    	
    	float textTopY = y + textAscent;
    	paint.setColor(Color.RED);
    	canvas.drawLine(0, textTopY, mWidth, textTopY, paint); 
    	
    	float textBottomY = y + textDescent;
    	paint.setColor(Color.BLUE);
    	canvas.drawLine(0, textBottomY, mWidth, textBottomY, paint);     	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = textTopY + (textHeight / 2);
    	paint.setColor(Color.BLACK);
    	canvas.drawLine(0, textCenterY, mWidth, textCenterY, paint); 
    }
    
    public void drawNormalWeekNums(Canvas canvas) {
    	
        
        int numCount = mNumDays;        
        
        int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	        
	    canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);  
                
        int y = mDateTextTopPadding;        
        
        boolean isBold = false;  
        mMonthNumPaint.setTextSize(mDateTextSize);
        mMonthNumPaint.setColor(mMonthNumColor);
        for (int i=0; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);            
            
            canvas.drawText(mDayNumbers[i], computeTextXPosition(i), y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }       
    }   
        
    boolean mDrawUpperLineOfSelectedWeekView = false;
    
    public boolean getDrawUpperLineFlagStatusOfSelectedWeekView() {
    	return mDrawUpperLineOfSelectedWeekView;
    }
    
    public void setDrawUpperLineFlagOfSelectedWeekView() {
    	mDrawUpperLineOfSelectedWeekView = true;
    }
    
    boolean mNoDrawDateIndicatorTextFlagOfSelectedWeekView = false;
    public boolean getNoDrawDateIndicatorTextFlagOfSelectedWeekView() {
    	return mNoDrawDateIndicatorTextFlagOfSelectedWeekView;
    }
    
    public void setNoDrawDateIndicatorTextOfSelectedWeekView () {
    	mNoDrawDateIndicatorTextFlagOfSelectedWeekView = true;
    	//mDateIndicatorTextView.setVisibility(View.GONE);
    }   
    
    public void drawSelectedNormalWeekNums(Canvas canvas) {
    	int y;
        int i = 0;        
        
        int x = 0;
        int numCount = mNumDays;
        
        if (mDrawUpperLineOfSelectedWeekView) {
        	int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	        
        	canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);  
        }
        
        mMonthNumPaint.setTextSize(mDateTextSize);               
        y = mDateTextTopPadding;        
        
        boolean isBold = false;        
        mMonthNumPaint.setColor(mMonthNumColor);
        for (; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }  
        
        if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {
        	
        	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
        	float dateIndicatorX = mWidth / 2;        	
        	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);        	
        }
    }
    
    
    public void drawLastDayIsMaxMonthDayWeekNums(Canvas canvas) {
    	int y;
        int i = 0;        
        int x = 0;
        int numCount = mNumDays;
        
        mMonthNumPaint.setTextSize(mDateTextSize);
        
	    int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	        
	    canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);  
                
        y = mDateTextTopPadding;        
        
        boolean isBold = false;        
        mMonthNumPaint.setColor(mMonthNumColor);
        for (; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);
                       
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }       
    }   
    
    
    public void drawSelectedLastDayIsMaxMonthDayWeekNums(Canvas canvas) {
    	int y;
        int i = 0;        
        int x = 0;
        int numCount = mNumDays;
        
        mMonthNumPaint.setTextSize(mDateTextSize);
        
        if (mDrawUpperLineOfSelectedWeekView) {
        	int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	        
        	canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);  
        }
        
	    //int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	        
	    //canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);  
                
        y = mDateTextTopPadding;        
        
        boolean isBold = false;        
        mMonthNumPaint.setColor(mMonthNumColor);
        for (; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);
                       
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }    
        
        if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {
        	//mDateIndicatorTextView.setAlpha(mSelectedWeekAlphaValue);
        	
        	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
        	float dateIndicatorX = mWidth / 2;        	
        	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);
        	
        }
    }
    
    Calendar mPsudoWeekTime;
    public void drawPsudoTwoDiffFirstWeekNums(Canvas canvas) {    	
    	int numCount = mNumDays;
    	int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;    	
    	    	    	
		Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);//Time monthIndicatorTime = new Time(mTimeZone);
		//long lastJulianDayMillis = ETime.getMillisFromJulianDay(mLastJulianDay, mTimeZone);
		monthIndicatorTime.setTimeInMillis(mLastMonthTime.getTimeInMillis());//monthIndicatorTime.setJulianDay(mLastJulianDay);        
		monthIndicatorTime.set(monthIndicatorTime.get(Calendar.YEAR), monthIndicatorTime.get(Calendar.MONTH), 1);//monthIndicatorTime.set(1, monthIndicatorTime.month, monthIndicatorTime.year);        
                
        long mills = monthIndicatorTime.getTimeInMillis();
        String monthText = buildMonth(mills, mills);//ETime.buildMonth(mills, mTimeZone);
       
        int weekDay = monthIndicatorTime.get(Calendar.DAY_OF_WEEK);//monthIndicatorTime.weekDay;
    	int fristWeekMonthDays = Utils.getDayNumbersOfWeek(mFirstDayOfWeek, weekDay);    
        
    	int prvWeekNumDays = 7 - fristWeekMonthDays;
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(Color.BLACK);
        monthTextPaint.setTextAlign(Align.CENTER);        
        monthTextPaint.setTextSize(mMonthTextSize);
        float textX = computeTextXPosition(prvWeekNumDays);///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        float textY = mMonthIndicatorHeight - mMonthTextBottomPadding;        
        canvas.drawText(monthText, textX, textY, monthTextPaint);        
        
        canvas.drawLine(dayOfWeekTextWidth * prvWeekNumDays, mMonthIndicatorHeight, mWidth, mMonthIndicatorHeight, mUpperLinePaint);        
        
        int nextWeekY = mMonthIndicatorHeight + mNormalModeDateTextTopPadding;
        
        boolean isBold = false;        
        mMonthNumPaint.setTextSize(mDateTextSize);
        mMonthNumPaint.setColor(mMonthNumColor);
        int i = prvWeekNumDays; //////////////////////////////////////////////
        int x = 0;
        for (; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);
                        
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, nextWeekY, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }
    }
    
    
    public void drawPsudoFirstMonthDayWeekNums(Canvas canvas) {
    	        
        int x = 0;
        int numCount = mNumDays;
        
        mMonthNumPaint.setTextSize(mDateTextSize);
                
        Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);//Time monthIndicatorTime = new Time(mTimeZone);
        //long firstJulianDayMillis = ETime.getMillisFromJulianDay(mFirstJulianDay, mTimeZone);
        monthIndicatorTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());//monthIndicatorTime.setJulianDay(mFirstJulianDay);
        
        long mills = monthIndicatorTime.getTimeInMillis();
        String monthText = buildMonth(mills, mills);//ETime.buildMonth(mills, mTimeZone);
        
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(Color.BLACK);
        monthTextPaint.setTextAlign(Align.CENTER);
        monthTextPaint.setTextSize(mMonthTextSize);
        float textX = computeTextXPosition(0);
        float textY = mMonthIndicatorHeight - mMonthTextBottomPadding;
        canvas.drawText(monthText, textX, textY, monthTextPaint);       
             
        int upperLineY = mMonthIndicatorHeight;       
        canvas.drawLine(0, upperLineY, mWidth, upperLineY, mUpperLinePaint);        
        int y = mMonthIndicatorHeight + mNormalModeDateTextTopPadding;
        
        boolean isBold = false;
        mMonthNumPaint.setColor(mMonthNumColor);
        for (int i = 0; i < numCount; i++) {
            mMonthNumPaint.setColor(mMonthNumColor);
                       
            x = computeTextXPosition(i);
            
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }
    }
    
    public void drawPsudoNormalWeekNums(Canvas canvas) {
    	
        int x = 0;
        int numCount = mNumDays;
                
        mMonthNumPaint.setTextSize(mDateTextSize);
        
        int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 
        
        canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);        
        
        int y = mNormalModeDateTextTopPadding;        
        boolean isBold = false;        
        mMonthNumPaint.setColor(mMonthNumColor);
        for (int i = 0;    i < numCount; i++) {
                         
            mMonthNumPaint.setColor(mMonthNumColor);
                       
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }    	
    }        
    
    
    
    public void drawSelectedPrvMonthLastWeekOfTDMWPattern (Canvas canvas) {    	    	
    	int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		//int weekLineY = (int)mUpperLinePaint.getStrokeWidth();
		
        int prvWeekX = 0;
        int nextWeekX = 0;
        int y = mDateTextTopPadding;
        mMonthNumPaint.setTextSize(mDateTextSize);
        mMonthNumPaint.setColor(mMonthNumColor);        
       
        mSelectedWeekAlphaPaint.setTextSize(mDateTextSize);
        mSelectedWeekAlphaPaint.setColor(mMonthNumColor);
        mSelectedWeekAlphaPaint.setAlpha(mSelectedWeekAlphaValue);
                
        Calendar prvWeekMonthTime = GregorianCalendar.getInstance(mTimeZone);//Time prvWeekMonthTime = new Time(mTimeZone);
        //long firstJulianDayMillis = ETime.getMillisFromJulianDay(mFirstJulianDay, mTimeZone);
        prvWeekMonthTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());//prvWeekMonthTime.setJulianDay(mFirstJulianDay);
                
        int maxMonthDays = prvWeekMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);     
        mPrvMonthWeekDaysNum = (maxMonthDays - prvWeekMonthTime.get(Calendar.DAY_OF_MONTH)) + 1;
        
        if (mDrawUpperLineOfSelectedWeekView) {
        	int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	        
        	//canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);  
        	canvas.drawLine(0, weekLineY, dayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mUpperLinePaint); // alpha �� �����ؾ� �Ѵ�
        }       
        
    	// prv Month�� last week�� ���� day�� ���õǾ����Ƿ�
    	for (int i=0; i < mPrvMonthWeekDaysNum; i++) {  
    		prvWeekX = computeTextXPosition(i);
    		canvas.drawText(mDayNumbers[i], prvWeekX, y, mMonthNumPaint);
    	}
    	// next Month�� first week�� ������ ������� �Ѵ�
    	for (int i=mPrvMonthWeekDaysNum; i<this.mNumDays; i++) {
    		nextWeekX = computeTextXPosition(i);
    		canvas.drawText(mDayNumbers[i], nextWeekX, y, mSelectedWeekAlphaPaint);
    	}
    	
    	if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {
    		//mDateIndicatorTextView.setAlpha(mSelectedWeekAlphaValue);
        	
        	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
        	float dateIndicatorX = mWidth / 2;        	
        	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);
        	
        }
    }
    
    
    //final int PRV_MONTH_LAST_WEEK_APPLY_ALPHA_VALUE = 1;
    //final int NEXT_MONTH_FIRST_WEEK_APPLY_ALPHA_VALUE = 2;
    //SL_NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFF_FIRSTWEEK_PATTERN
    public void drawSelectedNextMonthWeekOfTDMWPattern(Canvas canvas) {    	    	
    			
        int prvWeekX = 0;
        int nextWeekX = 0;
        int y = mDateTextTopPadding;
        mMonthNumPaint.setTextSize(mDateTextSize);
        mMonthNumPaint.setColor(mMonthNumColor);        
       
        mSelectedWeekAlphaPaint.setTextSize(mDateTextSize);
        mSelectedWeekAlphaPaint.setColor(mMonthNumColor);
        mSelectedWeekAlphaPaint.setAlpha(mSelectedWeekAlphaValue);
                
        Calendar prvWeekMonthTime = GregorianCalendar.getInstance(mTimeZone);//Time prvWeekMonthTime = new Time(mTimeZone);
        //long firstJulianDayMillis = ETime.getMillisFromJulianDay(mFirstJulianDay, mTimeZone);
        prvWeekMonthTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());//prvWeekMonthTime.setJulianDay(mFirstJulianDay);
                
        int maxMonthDays = prvWeekMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);    
        mPrvMonthWeekDaysNum = (maxMonthDays - prvWeekMonthTime.get(Calendar.DAY_OF_MONTH)) + 1;
        
        if (mDrawUpperLineOfSelectedWeekView) {
        	int effectiveWidth = mWidth;
        	int dayOfWeekTextWidth = effectiveWidth / mNumDays;        	
        	int weekLineY = (int)mUpperLinePaint.getStrokeWidth(); 	   
        	canvas.drawLine(dayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mWidth, weekLineY, mUpperLinePaint); 
        }
        
        // prv Month�� last week�� ������ ������� �Ѵ�
        for (int i=0; i < mPrvMonthWeekDaysNum; i++) {
        	prvWeekX = computeTextXPosition(i);
        	canvas.drawText(mDayNumbers[i], prvWeekX, y, mSelectedWeekAlphaPaint);
        }
        // next Month�� first week�� ���� day�� ���õǾ����Ƿ�
        for (int i=mPrvMonthWeekDaysNum; i<mNumDays; i++) {
        	nextWeekX = computeTextXPosition(i);
        	canvas.drawText(mDayNumbers[i], nextWeekX, y, mMonthNumPaint);
        }               
        
        if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView) {        	
        	mDateIndicatorPaint.setAlpha(mSelectedWeekAlphaValue);
        	float dateIndicatorX = mWidth / 2;        	
        	canvas.drawText(mDateIndicatorText, dateIndicatorX, mDateIndicatorY, mDateIndicatorPaint);
        	
        }
    }
    
    
    
    
    
    public void drawNextMonthWeekNumsOfTDMWPattern(Canvas canvas) {    
    	mPrvMonthWeekDaysNum = 0; // reset
    	
    	int numCount = mNumDays;
    	int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / mNumDays;    	
    	    	    	
		Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);//Time monthIndicatorTime = new Time(mTimeZone);
		//long lastJulianDayMillis = ETime.getMillisFromJulianDay(mLastJulianDay, mTimeZone);
		monthIndicatorTime.setTimeInMillis(mLastMonthTime.getTimeInMillis());//monthIndicatorTime.setJulianDay(mLastJulianDay);        
        
		monthIndicatorTime.set(monthIndicatorTime.get(Calendar.YEAR), monthIndicatorTime.get(Calendar.MONTH), 1);//monthIndicatorTime.set(1, monthIndicatorTime.month, monthIndicatorTime.year);        
           
        long mills = monthIndicatorTime.getTimeInMillis();
        String monthText = buildMonth(mills, mills);//ETime.buildMonth(mills, mTimeZone);
       
        int weekDay = monthIndicatorTime.get(Calendar.DAY_OF_WEEK);//monthIndicatorTime.weekDay;
    	int fristWeekMonthDays = Utils.getDayNumbersOfWeek(this.mFirstDayOfWeek, weekDay);       
    	mPrvMonthWeekDaysNum = mNumDays - fristWeekMonthDays;
    	    	
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(Color.BLACK);
        monthTextPaint.setTextAlign(Align.CENTER);        
        monthTextPaint.setTextSize(mMonthTextSize);
        float textX = computeTextXPosition(mPrvMonthWeekDaysNum);///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        float textY = mMonthIndicatorHeight - mMonthTextBottomPadding;
        canvas.drawText(monthText, textX, textY, monthTextPaint);        
        
        canvas.drawLine(dayOfWeekTextWidth * mPrvMonthWeekDaysNum, mMonthIndicatorHeight, mWidth, mMonthIndicatorHeight, mUpperLinePaint);	
		
        int nextWeekY = mMonthIndicatorHeight + mDateTextTopPadding;
        
        boolean isBold = false;        
        mMonthNumPaint.setTextSize(mDateTextSize);
        mMonthNumPaint.setColor(mMonthNumColor);
        int i = mPrvMonthWeekDaysNum; //////////////////////////////////////////////
        int x = 0;
        for (; i < numCount; i++) {                           
            mMonthNumPaint.setColor(mMonthNumColor);           
            x = computeTextXPosition(i);
            canvas.drawText(mDayNumbers[i], x, nextWeekY, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }
        }
    }    
    
    public void drawMonthIndicatorWeekPattern(Canvas canvas) { 	 	
    	    	    	
		Calendar monthIndicatorTime = GregorianCalendar.getInstance(mTimeZone);//Time monthIndicatorTime = new Time(mTimeZone);
		//long lastJulianDayMillis = ETime.getMillisFromJulianDay(mLastJulianDay, mTimeZone);
		monthIndicatorTime.setTimeInMillis(mLastMonthTime.getTimeInMillis());//monthIndicatorTime.setJulianDay(mLastJulianDay);    
		
		monthIndicatorTime.set(monthIndicatorTime.get(Calendar.YEAR), monthIndicatorTime.get(Calendar.MONTH), 1);//monthIndicatorTime.set(1, monthIndicatorTime.month, monthIndicatorTime.year); 
                 
        long mills = monthIndicatorTime.getTimeInMillis();
        String monthText = buildMonth(mills, mills);//ETime.buildMonth(mills, mTimeZone);
       
        int weekDay = monthIndicatorTime.get(Calendar.DAY_OF_WEEK);//monthIndicatorTime.weekDay;
    	int fristWeekMonthDays = Utils.getDayNumbersOfWeek(this.mFirstDayOfWeek, weekDay);       
    	mPrvMonthWeekDaysNum = mNumDays - fristWeekMonthDays;
    	    	
        Paint monthTextPaint = new Paint();
        monthTextPaint.setAntiAlias(true);
        monthTextPaint.setColor(Color.BLACK);
        monthTextPaint.setTextAlign(Align.CENTER);        
        monthTextPaint.setTextSize(mMonthTextSize);
        float textX = computeTextXPosition(mPrvMonthWeekDaysNum);///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        float textY = mMonthIndicatorHeight - mMonthTextBottomPadding;
        canvas.drawText(monthText, textX, textY, monthTextPaint);        
        
        /*
        int effectiveWidth = mWidth;
        int dayOfWeekTextWidth = effectiveWidth / mNumDays;   
        float lineY = mMonthIndicatorHeight - mUpperLinePaint.getStrokeWidth(); // mUpperLinePaint.getStrokeWidth()�� �����ϸ� �ȵ�        
        canvas.drawLine(dayOfWeekTextWidth * mPrvMonthWeekDaysNum, lineY, mWidth, lineY, mUpperLinePaint); 
        */   
    }
    
    public void drawToday(Canvas canvas, boolean mustAlphaBlending) {
    	
        Calendar todayCalendar = GregorianCalendar.getInstance(mTimeZone);
        todayCalendar.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        todayCalendar.add(Calendar.DAY_OF_MONTH, mTodayIndex);        
        
        int todayDateNum = todayCalendar.get(Calendar.DAY_OF_MONTH);
        
        int offsetY = 0;            
        if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {       				        		
        	offsetY = mMonthIndicatorHeight;                  		
        }
    	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		    		
    		if (mPrvMonthWeekDaysNum > mTodayIndex) {
    			offsetY = 0;                    
    		}
    		else {
    			offsetY = mNormalWeekHeight + mMonthIndicatorHeight;                    
    		}        		
        }    	
    	else if (mWeekPattern == NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN) {		    		
    		
    		int MaxPrvMonthWeekDayIndex = mPrvMonthWeekDaysNum - 1;
    		// today�� next month first week�� �ƴ� prv month last week�� ���� �ִٸ�,,,
    		if (mTodayIndex <= MaxPrvMonthWeekDayIndex)
    			return;
    		
    		offsetY = mMonthIndicatorHeight;  
    	}
        
        makeSelectedCircleDrawableDimens(offsetY);
        
        mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
        mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
        mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
        mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);	
        
        if (mustAlphaBlending) {
        	int alphaValue = 255 - mSelectedWeekAlphaValue;        	
        	mSelectedDateTodayDrawable.setAlpha(alphaValue);			
        }
		
		// x�� text�� x�� �� ���̴�
		int x = computeTextXPosition(mTodayIndex);
		int left = x - mSelectedCircleDrawableRadius;	    			
		int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
		int right = left + mSelectedCircleDrawableSize;
		int bottom = top + mSelectedCircleDrawableSize;
		
		mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);	
		mSelectedDateTodayDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
		mSelectedDateTodayDrawable.setDayOfMonth(todayDateNum);
		mSelectedDateTodayDrawable.draw(canvas);   		
    }
    
    public void drawToday(Canvas canvas, int offsetY, boolean mustAlphaBlending) {
    	
        Calendar todayCalendar = GregorianCalendar.getInstance(mTimeZone);
        todayCalendar.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        todayCalendar.add(Calendar.DAY_OF_MONTH, mTodayIndex);        
        
        int todayDateNum = todayCalendar.get(Calendar.DAY_OF_MONTH);
                
        makeSelectedCircleDrawableDimens(offsetY);
        
        mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
        mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
        mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
        mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);	
        
        if (mustAlphaBlending) {
        	int alphaValue = 255 - mSelectedWeekAlphaValue;        	
        	mSelectedDateTodayDrawable.setAlpha(alphaValue);			
        }
		
		// x�� text�� x�� �� ���̴�
		int x = computeTextXPosition(mTodayIndex);
		int left = x - mSelectedCircleDrawableRadius;	    			
		int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
		int right = left + mSelectedCircleDrawableSize;
		int bottom = top + mSelectedCircleDrawableSize;
		
		mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);	
		mSelectedDateTodayDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
		mSelectedDateTodayDrawable.setDayOfMonth(todayDateNum);
		mSelectedDateTodayDrawable.draw(canvas);   		
    }
    
    public void drawEventExistenceCircle(Canvas canvas) {
    	/*
    	if (mWeekPattern == SELECTED_NORMAL_WEEK_NONE_UPPERLINE_PATTERN ||     			
    			mWeekPattern == SELECTED_LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN ||
    			mWeekPattern == SELECTED_PREVIOUS_MONTH_WEEK_OF_TWO_DIFF_FIRSTWEEK_PATTERN ||
    			mWeekPattern == SELECTED_NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFF_FIRSTWEEK_PATTERN) {
    		
    		if (!mNoDrawDateIndicatorTextFlagOfSelectedWeekView)
    			return;
    	}
    	*/
    	
    	//
    	// week pattern�� �߿��ϴ�
    	// circle center�� y��ǥ�� top���� week height�� 65% offset
    	// circle diameter�� week height�� 10%
    	//if (mEventExistence != null) {     		
    		float radius = (mNormalWeekHeight * 0.1f) / 2;
    		
	    	if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {        		
	    		float monthIndicatorHeightSize = mMonthIndicatorHeight;	    		
	    		float cy = monthIndicatorHeightSize + mEventCircleTopPadding;
	    		
	    		for (int i=0; i<mNumDays; i++) {
					if (mEventExistence[i]) {
						float cx = computeTextXPosition(i);
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}      		
	        }
	    	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {	    		  		        		
	    		float prvWeekHeight = mNormalWeekHeight;        		
	    		float monthIndicatorRectHeight = mMonthIndicatorHeight;    		  		
	    			    		
	    		for (int i=0; i<mNumDays; i++) {
	    			float cx = computeTextXPosition(i);
	    			
					if (mEventExistence[i]) {
						float cy = 0;
						
						if (i < mPrvMonthWeekDaysNum) {
							cy = mEventCircleTopPadding;
						}
						else {
							cy = prvWeekHeight + monthIndicatorRectHeight + mEventCircleTopPadding;
						}				
						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}    		    		
	        }
	    	else if (mWeekPattern == NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN) {
	    		float monthIndicatorHeightSize = mMonthIndicatorHeight;	    		
	    		float cy = monthIndicatorHeightSize + mEventCircleTopPadding;
	    		
	    		for (int i=0; i<mNumDays; i++) {
					if (mEventExistence[i]) {
						float cx = computeTextXPosition(i);
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}      		
	    	}
	    	else if (mWeekPattern == SELECTED_PREVIOUS_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN) {
	    		float cy = mEventCircleTopPadding;
	    		for (int i=0; i<mPrvMonthWeekDaysNum; i++) {
					if (mEventExistence[i]) {
						float cx = computeTextXPosition(i);
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}	    			
	        }
	    	else {	    		
	    		float cy = mEventCircleTopPadding;
	    		for (int i=0; i<mNumDays; i++) {
					if (mEventExistence[i]) {
						float cx = computeTextXPosition(i);
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}
	    	}
    	//}    	
    }
    
    // ���� �����...drawWeekNums���� �̹� �ش� ��¥�� drawing �ߴٴ� ����
	// �׷��� �� drawable�� ���� ������ �����ص� drawWeekNums���� drawing�� ��¥�� ������ ����???
	// :������ canvas��...drawWeekNums���� �׷ȴ� ��¥�� �������ڱ���...
    private void drawSelectedDayCircle(Canvas canvas) {
        //if (mSelectedDayIndex != -1) {
        	//int selectedJulainDay = mFirstJulianDay + mSelectedDayIndex;
        	
        	Calendar time = GregorianCalendar.getInstance(mTimeZone);
        	//long selectedJulainDayMillis = ETime.getMillisFromJulianDay(selectedJulainDay, mTimeZone);
        	time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());//time.setJulianDay(todayJulainDay);
        	time.add(Calendar.DAY_OF_MONTH, mSelectedDayIndex);
        	       	
            int dateNum = time.get(Calendar.DAY_OF_MONTH);//time.monthDay;
            
            int offsetY = 0;            
            if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {       				        		
            	offsetY = mMonthIndicatorHeight;                  		
            }
        	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
        		    		
        		if (mPrvMonthWeekDaysNum > mSelectedDayIndex) {
        			offsetY = 0;                    
        		}
        		else {
        			offsetY = mNormalWeekHeight + mMonthIndicatorHeight;                    
        		}        		
            }
            
            makeSelectedCircleDrawableDimens(offsetY);
            
        	mSelectedDateDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
    		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
    		mSelectedDateDrawable.getPaint().setAntiAlias(true);
    		mSelectedDateDrawable.getPaint().setStyle(Style.FILL);
    		mSelectedDateDrawable.getPaint().setColor(Color.BLACK);	
    		//mSelectedDateDrawable.getPaint().setAlpha(mSelectedWeekAlphaValue);	
    		mSelectedDateDrawable.setAlpha(mSelectedWeekAlphaValue);
    		// �� �� setAlpha�� ��ǻ� ������ ȿ���� �ְ� �ִ�
    		if (mSelectedWeekAlphaValue < 102)  { // 255 * 0.4
    			int textAlpha = 255 - mSelectedWeekAlphaValue;
    			mSelectedDateDrawable.setTextColorAlpha(textAlpha, Color.BLACK);
    		}
    		
			
    		// x�� text�� x�� �� ���̴�
    		int x = computeTextXPosition(mSelectedDayIndex);
			int left = x - mSelectedCircleDrawableRadius;	    			
			int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
			int right = left + mSelectedCircleDrawableSize;
			int bottom = top + mSelectedCircleDrawableSize;
			
			mSelectedDateDrawable.setBounds(left, top, right, bottom);	
			mSelectedDateDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
			mSelectedDateDrawable.setDayOfMonth(dateNum);
			mSelectedDateDrawable.draw(canvas);   		
        //}
    } 
    
    
    public int getDayIndexFromLocation(float x, float y) {
    	int dayStart = 0;
    	
    	// mHeight
    	// mParentHeight
    	if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
    		//MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT
    		float monthIndicatorHeightSize = mMonthIndicatorHeight;
    		//float validRegionTop = getTop();
    		//float validRegionTop = 0;
    		float validRegionBottom = /*validRegionTop + */monthIndicatorHeightSize;
    		if ( /*y < validRegionTop) ||*/  y < validRegionBottom)
    			return -1;
        }
    	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		// �ټ� ���� ������ �����Ѵ�
    		// :1)valid prv week region
    		//  2)invalid prv week region------------------
    		//  3)month indicator region-------------------
    		//  4)invalid next week region-----------------
    		//  5)valid next week region
    		// PRV_WEEK_SECTION_HEIGHT
    		// MONTH_INDICATOR_SECTION_HEIGHT
    		// NEXT_WEEK_SECTION_HEIGHT
    		int effectiveWidth = mWidth;
    		int dayOfWeekTextWidth = effectiveWidth / 7;
    		//int nextWeekNumDays = mNumDays - mPrvMonthWeekDaysNum;
    		
    		float prvWeekHeight = mNormalWeekHeight;
    		int invalidPrvWeekRectLeft = mPrvMonthWeekDaysNum * dayOfWeekTextWidth;
    		Rect invalidPrvWeekRect = new Rect(invalidPrvWeekRectLeft, 0, mWidth, (int)prvWeekHeight);
    		
    		float monthIndicatorRectHeight = mMonthIndicatorHeight;
    		Rect monthIndicatorRect = new Rect(0, (int)prvWeekHeight, mWidth, (int)(prvWeekHeight + monthIndicatorRectHeight));
    		
    		float nextWeekRectHeight = mNormalWeekHeight;    		
    		Rect invalidNextWeekRect = new Rect(0, monthIndicatorRect.bottom, invalidPrvWeekRectLeft, (int)(monthIndicatorRect.bottom + nextWeekRectHeight));    		
    		
    		if(invalidPrvWeekRect.contains((int)x, (int)y))
    			return -1;
    		else if(monthIndicatorRect.contains((int)x, (int)y))
    			return -1;
    		else if(invalidNextWeekRect.contains((int)x, (int)y))
    			return -1;
    		
        }
    	else if (mWeekPattern == NORMAL_WEEK_PATTERN || mWeekPattern == LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {
    		/*
            if (x < dayStart || x > mWidth - mPadding) {
                return -1;
            }
            */
        }    	
        
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
    	//return ((int) ((x - dayStart) * mNumDays / (800 - dayStart))); // for test
        return ((int) ((x - dayStart) * mNumDays / (mWidth - dayStart))); // original code must recovery
    }

    public Calendar getDayFromLocation(float x, float y) {
        int dayPosition = getDayIndexFromLocation(x, y);
        if (dayPosition == -1) {
            return null;
        }
        //int day = mFirstJulianDay + dayPosition;

        Calendar time = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZone);
        time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        time.add(Calendar.DAY_OF_MONTH, dayPosition);
        
        if (mWeek == 0) {
            // This week is weird...
        	/*
            if (day < ETime.ECALENDAR_EPOCH_JULIAN_DAY) {
            	time.add(Calendar.DAY_OF_MONTH, 1);//day++;
            } else if (day == ETime.ECALENDAR_EPOCH_JULIAN_DAY) {
                time.set(1900, 0, 1);                
                return time;
            }
            */
        }

        //long dayMillis = ETime.getMillisFromJulianDay(day, mTimeZone);
        //time.setTimeInMillis(dayMillis);//time.setJulianDay(day);
        
        return time;
    }

    Calendar mDayViewSelectedDayTime = null;
    public void setDayViewSelectedDayTime(Calendar time) {
    	mDayViewSelectedDayTime = time;  
    	mDayViewSelectedDayTime = GregorianCalendar.getInstance(time.getTimeZone());
    	mDayViewSelectedDayTime.setFirstDayOfWeek(time.getFirstDayOfWeek());
    	mDayViewSelectedDayTime.setTimeInMillis(time.getTimeInMillis());
    	mDateIndicatorText = buildFullDate(mDayViewSelectedDayTime.getTimeInMillis());    	
    }   
    
    public void setSelectedDayIndex(int index) {
    	mSelectedDayIndex = index;        
    }     
    
    /*    
    public int getClickedDayIndex() {
    	return mClickedDayIndex;
    }

    public void setClickedDay(float xLocation, float yLocation) {
        mClickedDayIndex = getDayIndexFromLocation(xLocation, yLocation);
        invalidate();
    }
    public void clearClickedDay() {
        mClickedDayIndex = -1;
        invalidate();
    }    
    */
    
    /**
     * Returns the month of the first day in this week
     *
     * @return The month the first day of this view is in
     */
    public int getFirstMonth() {
        return mFirstMonth;
    }

    /**
     * Returns the month of the last day in this week
     *
     * @return The month the last day of this view is in
     */
    public int getLastMonth() {
        return mLastMonth;
    }
    
    public Calendar getFirstMonthTime() {
        return mFirstMonthTime;
    }
    
    public Calendar getLastMonthTime() {
        return mLastMonthTime;
    }

    /**
     * Returns the julian day of the first day in this view.
     *
     * @return The julian day of the first day in the view.
     */
    /*
    public int getFirstJulianDay() {
        return mFirstJulianDay;
    }
	*/
    /**
     * Returns the julian day of the last day in this view.
     *
     * @return The julian day of the last day in the view.
     */
    /*
    public int getLastJulianDay() {
    	return mLastJulianDay;
    }
    */
    public int getWeekNumber() {
    	return mWeek;
    }
    
    public int getWeekPattern() {
    	return mWeekPattern;
    }
    
    public int getPrvMonthWeekDaysNum() {
    	return mPrvMonthWeekDaysNum;
    }    
    
    //public boolean getSelectedWeekStatus() {
    	//return mSelectedWeekToTwoDiffWeeksFormat;
    //}
    
    //boolean mSelectedWeekToTwoDiffWeeksFormat = false;
    //public void setSelectedWeekToTwoDiffWeeksFormat() {
    	//mSelectedWeekToTwoDiffWeeksFormat = true;
    //}
    
    int mSelectedWeekAlphaValue = 255;
    public int getSelectedWeekAlphaValue() {
    	return mSelectedWeekAlphaValue;
    }
    
    public void setSelectedWeekAlphaValue(int alphaValue) {
    	mSelectedWeekAlphaValue = alphaValue;
    }
    
    String mDateIndicatorText;
    private String buildFullDate(long milliTime) {
        mStringBuilder.setLength(0);
        String date = ETime.formatDateTimeRange(mContext, mFormatter, milliTime, milliTime,
        		ETime.FORMAT_SHOW_WEEKDAY | ETime.FORMAT_SHOW_DATE | ETime.FORMAT_SHOW_YEAR, mTzId).toString();
        return date;
    }
}
