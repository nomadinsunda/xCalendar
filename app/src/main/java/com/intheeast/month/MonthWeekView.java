package com.intheeast.month;

import java.security.InvalidParameterException;
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.OvalShape;
//import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.View;

public class MonthWeekView extends View {
	
	Context mContext;
	
	private static final String TAG = "MonthWeekView";
    private static final boolean INFO = true;
    
    //public static final float MONTH_LIST_VIEW_TOP_OFFSET = 0.086f;
    // month indicator : 8%
    //public static final float MONTH_INDICATOR_SECTION_HEIGHT = 0.08f;
    public static final float MONTH_INDICATOR_SECTION_HEIGHT = 0.06f;
    
    // normal week height : 16%
    public static final float NORMAL_WEEK_SECTION_HEIGHT = 0.16f;
    
    // prv week height : 12%
    //public static final float PRV_WEEK_SECTION_HEIGHT = 0.16f;
	
	// next week height : 16%
    //public static final float NEXT_WEEK_SECTION_HEIGHT = NORMAL_WEEK_SECTION_HEIGHT;
    
    // prv week height : 12%
    //public static final float LAST_WEEK_SECTION_HEIGHT = PRV_WEEK_SECTION_HEIGHT;
    
    
    /**
     * These params can be passed into the view to control how it appears.
     * {@link #VIEW_PARAMS_WEEK} is the only required field, though the default
     * values are unlikely to fit most layouts correctly.
     */
    /**
     * This sets the height of this week in pixels
     */
    public static final String VIEW_PARAMS_MODE = "view_mode";
    
    public static final String VIEW_PARAMS_HEIGHT = "height";
    
    public static final String VIEW_PARAMS_WIDTH = "width";
    
    public static final String VIEW_PARAMS_NORMAL_WEEK_HEIGHT = "normal_week_height";
    
    //public static final String VIEW_PARAMS_PREVIOUS_WEEK_HEIGHT = "previous_week_height";
    
    public static final String VIEW_PARAMS_MONTH_INDICATOR_HEIGHT = "month_indicator_height";
    
    //public static final String VIEW_PARAMS_LISTVIEW_HEIGHT = "listview_height";
    
    public static final String VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT = "original_listview_height";
    
    /**
     * This specifies the position (or weeks since the epoch) of this week,
     * calculated using {@link Utils#getWeeksSinceEpochFromJulianDay}
     */
    public static final String VIEW_PARAMS_WEEK = "week";

    public static final String VIEW_PARAMS_SELECTED_DAY_OF_WEEK_IN_EEMODE = "selected_day_of_week_in_eemode";

    public static final String VIEW_PARAMS_WEEK_START = "week_start";

    public static final String VIEW_PARAMS_NUM_DAYS = "num_days";
    /**
     * Which month is currently in focus, as defined by {@link Time#month}
     * [0-11].
     */
    //public static final String VIEW_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * If this month should display week numbers. false if 0, true otherwise.
     */
    //public static final String VIEW_PARAMS_SHOW_WK_NUM = "show_wk_num";
    
    public static final String VIEW_PARAMS_WEEK_PATTERN = "week_pattern";
    
    //public static final String VIEW_PARAMS_FIRST_MONTHDAY_WEEK_MILLIS = "first_monthday_week";
    
    //public static final String VIEW_PARAMS_LAST_MONTHDAY_WEEK = "last_monthday_week";
    
    public static final int NORMAL_MODE_VIEW_PATTERN = 1;
    public static final int EXPAND_EVENTS_VIEW_MODE_VIEW_PATTERN = 2;
    
    
    public static final int NORMAL_WEEK_PATTERN = 0;
    public static final int NORMAL_WEEK_NONE_UPPERLINE_PATTERN = 1;
    public static final int TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 2;
    public static final int FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN = 3;
    public static final int MONTHIDICATOR_AND_UPPERLINE_OF_FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN = 4;
    public static final int LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN = 5;        
    public static final int PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 6;
    public static final int PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 7;
    public static final int NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 8;    
    public static final int NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 9;
    public static final int NEXT_MONTH_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN = 10;
    
    public static final int PSUDO_FIRSTWEEK_OF_TWO_DIFFWEEKS_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE = 11;
    public static final int PSUDO_FIRSTDAY_FIRSTWEEK_PATTERN_AT_EXPAND_EVENTVIEW_ENTRANCE = 12;
    public static final int PSUDO_NORMALWEEK_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE = 13;
        
    public static final float MONTHLISTVIEW_MONTH_TEXT_SIZE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT = 0.5f;
    
    public static final float MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT = 0.04f;
    
    //public static final float MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT = 0.75f;
    public static final float MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT = 0.6f;
    
    public static final float MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT = 0.4f;
    
    public static final float MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT = 0.45f;
    //public static final float MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT = 0.4f;
    
    public static final float MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT = 0.65f;
    
    public static final float MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT = 0.7f;
    //public static final float MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT = 0.65f;
    
    public static int DEFAULT_HEIGHT = 32;
    public static int MIN_HEIGHT = 10;
    public static final int DEFAULT_SELECTED_DAY = -1;
    public static final int DEFAULT_WEEK_START = Calendar.SUNDAY;
    public static final int DEFAULT_NUM_DAYS = 7;
    public static final int DEFAULT_SHOW_WK_NUM = 0;
    public static final int DEFAULT_FOCUS_MONTH = -1;

    //public static int DAY_SEPARATOR_WIDTH = 1;

    public static int MINI_DAY_NUMBER_TEXT_SIZE = 14;
    public static int MINI_WK_NUMBER_TEXT_SIZE = 12;
    public static int MINI_TODAY_NUMBER_TEXT_SIZE = 18;
    public static int MINI_TODAY_OUTLINE_WIDTH = 2;
    public static int WEEK_NUM_MARGIN_BOTTOM = 4;

    // used for scaling to the device density
    public static float mScale = 0;

    // affects the padding on the sides of this view
    //public int mPadding = 0;

    public Rect r = new Rect();
    public Paint p = new Paint();
    public Paint mMonthNumPaint;
    public Paint mEventExisteneCirclePaint;
    public Paint mUpperLinePaint;
    public Paint mSelectedWeekAlphaPaint;
    //public Drawable mSelectedDayLine;

    // Cache the number strings so we don't have to recompute them each time
    public String[] mDayNumbers;
    // Quick lookup for checking which days are in the focus month
    //public boolean[] mFocusDay;
    // Quick lookup for checking which days are in an odd month (to set a different background)
    //public boolean[] mOddMonth; // ������� 31���� ��
    // The Julian day of the first day displayed by this item[week]
    //public int mFirstJulianDay = -1;
    // The Julian day of the last day displayed by this item[week]
    //public int mLastJulianDay = -1;
    
    // The month of the first day in this week
    public int mFirstMonth = -1;
    // The month of the last day in this week
    public int mLastMonth = -1;
    
 // The month of the first day in this week
    public Calendar mFirstMonthTime;
    // The month of the last day in this week
    public Calendar mLastMonthTime;
    
    // The position of this week, equivalent to weeks since the week of Jan 1st,
    // 1970
    public int mWeek = -1;
    
    public int mWeekPattern = NORMAL_WEEK_PATTERN;
    
    
    // Quick reference to the width of this view, matches parent
    public int mWidth;
    // The height this view should draw at in pixels, set by height param
    public int mHeight = DEFAULT_HEIGHT;
    
    // Whether the week number should be shown
    //public boolean mShowWeekNum = false;
    // If this view contains the selected day
    // �� Month Normal Mode���� mHasSelectedDay�� �ʿ��Ѱ�?
    // Expand EventListView Mode������ �ʿ��ϰ�����...
    public boolean mHasSelectedDayInEEMode = false;
    // If this view contains the today
    public boolean mHasToday = false;
    // Which day is selected [0-6] or -1 if no day is selected
    public int mSelectedDayOfWeekInEEMode = DEFAULT_SELECTED_DAY;
    // Which day is today [0-6] or -1 if no day is today
    //public int mToday = DEFAULT_SELECTED_DAY;
    // Which day of the week to start on [1-7]
    public int mFirstDayOfWeek = DEFAULT_WEEK_START;
    // How many days to display
    public int mNumDays = DEFAULT_NUM_DAYS;
    // The number of days + a spot for week number if it is displayed
    public int mNumCells = mNumDays;
    // The left edge of the selected day
    public int mSelectedLeft = -1;
    // The right edge of the selected day
    public int mSelectedRight = -1;
    // The timezone to display times/dates in (used for determining when Today
    // is)
    public String mTzId = ETime.getCurrentTimezone();
    TimeZone mTimeZone = TimeZone.getTimeZone(mTzId);
    long mGmtoff = mTimeZone.getRawOffset() / 1000;
    
    public Calendar mToday = GregorianCalendar.getInstance();

    public int mBGColor;
    public int mSelectedWeekBGColor;    
    public int mWeekNumColor;
    
    public int mPrvMonthWeekDaysNum = 0;
    
    public boolean mDontDrawWeekUpperLine = false;
    
    public boolean mDrawSecondaryActionBarMonthDayHeader = false;
    public int mDrawSecondaryActionBarMonthDayHeaderAlphaValue;
    
    public boolean mDontDrawEventExistenceCircle = false;
    public int mDontDrawEventExistenceCircleAlphaValue;
    
    public static final String VIEW_PARAMS_ORIENTATION = "orientation";
    public static final String VIEW_PARAMS_ANIMATE_CHANGE_TARGET_MONTH_IN_EEMODE = "change_target_month_in_eemode";

    /* NOTE: these are not constants, and may be multiplied by a scale factor */
    public static int TEXT_SIZE_MONTH_NUMBER = 32;
    
    private static int DAY_SEPARATOR_INNER_WIDTH = 1;
    
    private static int MIN_WEEK_WIDTH = 50;

    private static int TODAY_HIGHLIGHT_WIDTH = 2;

    private static int SPACING_WEEK_NUMBER = 24;
    private static boolean mInitialized = false;
    //private static boolean mShowDetailsInMonth;

    
    public int mTodayIndex = -1;
    public int mOrientation = Configuration.ORIENTATION_LANDSCAPE;
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
    
    
    public int mClickedDayIndex = -1;
    public int mClickedDayAlpha = 255;
    private int mClickedDayColor;
    private static final int mClickedAlpha = 128;

    public int mTodayAnimateColor;
    
    private int mAnimateChangeTargetMonthInEEModeAlpha = 255;
    private ObjectAnimator mChangeTargetMonthInEEModeAnimator = null;
    
    int mViewMode = 0;
    int mOriginalListViewHeight = 0;
    //int mListViewHeight = 0;
    int mNormalWeekHeight = 0;
    //int mPreviousWeekHeight = 0;
    int mMonthIndicatorHeight = 0;
    int mDateTextSize = 0;
    int mDateTextTopPadding = 0;
    //int mNormalModeDateTextTopPadding = 0;
    float mMonthTextSize = 0;
    float mMonthTextBottomPadding = 0;
    int mEventCircleTopPadding = 0;
    //int mNormalModeEventCircleTopPadding = 0;
    
    private final ChangeTargetMonthInEEModeAnimatorListener mAnimatorListener = new ChangeTargetMonthInEEModeAnimatorListener();

    class ChangeTargetMonthInEEModeAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mFadingIn = false;
        
        @Override
        public void onAnimationStart(Animator animation) {
        	if (mFadingIn) {
        		if (INFO) Log.i(TAG, "mFadingIn:onAnimationStart");
        	}
        	else {
        		if (INFO) Log.i(TAG, "mFadingOut:onAnimationStart");
        	}
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            synchronized (this) {
                if (mAnimator != animation) {
                    animation.removeAllListeners();
                    animation.cancel();
                    return;
                }
                
                if (mFadingIn) {
                	if (INFO) Log.i(TAG, "mFadingIn:onAnimationEnd");
                	
                    if (mChangeTargetMonthInEEModeAnimator != null) {
                    	mChangeTargetMonthInEEModeAnimator.removeAllListeners();
                    	mChangeTargetMonthInEEModeAnimator.cancel();
                    }
                    
                } else {
                	if (INFO) Log.i(TAG, "mFadingOut:onAnimationEnd");
                	
                	if (mChangeTargetMonthInEEModeAnimator != null) {
                    	mChangeTargetMonthInEEModeAnimator.removeAllListeners();
                    	mChangeTargetMonthInEEModeAnimator.cancel();
                    }
                	
                	mHasSelectedDayInEEMode = false;
                	mSelectedDayOfWeekInEEMode = DEFAULT_SELECTED_DAY;                	
                    mAnimateChangeTargetMonthInEEModeAlpha = 255; // �߿��ϴ� ���� getView���� �� ���� ��쿡,
                                                                  // �ʱⰪ���� ������ �Ѵ�               	
                }
            }
        }

        public void setAnimator(Animator animation) {
            mAnimator = animation;
        }

        public void setFadingIn(boolean fadingIn) {
            mFadingIn = fadingIn;
        }

    }

    private int[] mDayXs;
	
	public MonthWeekView(Context context) {
		super(context);
		
		mContext = context;
		
		Resources res = context.getResources();

        mBGColor = res.getColor(R.color.month_bgcolor);        
        mWeekNumColor = res.getColor(R.color.month_week_num_color);        

        if (mScale == 0) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                DEFAULT_HEIGHT *= mScale;
                MIN_HEIGHT *= mScale;
                MINI_DAY_NUMBER_TEXT_SIZE *= mScale;
                MINI_TODAY_NUMBER_TEXT_SIZE *= mScale;
                MINI_TODAY_OUTLINE_WIDTH *= mScale;
                WEEK_NUM_MARGIN_BOTTOM *= mScale;                
                MINI_WK_NUMBER_TEXT_SIZE *= mScale;
            }
        }

        // Sets up any standard paints that will be used
        initView();
	}
	
	int mWeek_saturdayColor;
	int mWeek_sundayColor;
	/**
     * Sets up the text and style properties for painting. Override this if you
     * want to use a different paint.
     */
    public void initView() {
    	mWeek_saturdayColor = getResources().getColor(R.color.week_saturday);
        mWeek_sundayColor = getResources().getColor(R.color.week_sunday);
        
        p.setFakeBoldText(false);
        p.setAntiAlias(true);
        p.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        p.setStyle(Style.FILL);

        mEventExisteneCirclePaint = new Paint();    
        mEventExisteneCirclePaint.setAntiAlias(true);        
        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
        mEventExisteneCirclePaint.setStyle(Style.FILL);        
        
        mUpperLinePaint = new Paint();        
        mUpperLinePaint.setAntiAlias(true);        
        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
        mUpperLinePaint.setStyle(Style.STROKE);
        mUpperLinePaint.setStrokeWidth(getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight));
        
        if (!mInitialized) {
            Resources resources = getContext().getResources();            
            TEXT_SIZE_MONTH_NUMBER = resources.getInteger(R.integer.text_size_month_number);
            
            if (mScale != 1) {                
                
                SPACING_WEEK_NUMBER *= mScale;
                TEXT_SIZE_MONTH_NUMBER *= mScale;                
                DAY_SEPARATOR_INNER_WIDTH *= mScale;                
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

        mMonthNumAscentHeight = (int) (-mMonthNumPaint.ascent() + 0.5f);
        mMonthNumHeight = (int) (mMonthNumPaint.descent() - mMonthNumPaint.ascent() + 0.5f); 
       
    }
    
    public void loadColors(Context context) {
        Resources res = context.getResources();
        
        mMonthNumColor = res.getColor(R.color.month_day_number);        
        mMonthNumTodayColor = res.getColor(R.color.month_today_number);
           
        mMonthBGTodayColor = res.getColor(R.color.month_today_bgcolor);        
        mTodayAnimateColor = res.getColor(R.color.today_highlight_color);
        mClickedDayColor = res.getColor(R.color.day_clicked_background_color);
        mTodayDrawable = res.getDrawable(R.drawable.today_blue_week_holo_light);
    }
    
    /**
     * Sets all the parameters for displaying this week. The only required
     * parameter is the week number. Other parameters have a default value and
     * will only update if a new value is included, except for focus month,
     * which will always default to no focus month if no value is passed in. See
     * {@link #VIEW_PARAMS_HEIGHT} for more info on parameters.
     *
     * @param params A map of the new parameters, see
     *            {@link #VIEW_PARAMS_HEIGHT}
     * @param tz The time zone this view should reference times in
     */
    /*
    if target month is 2014-09-11,
    ///////////////////////////////////////////////////////
    mDayNumbers = [31, 1, 2, 3, 4, 5, 6]
	mFocusDay = [false, true, true, true, true, true, true]	
	mOddMonth = [true, false, false, false, false, false, false]	
	mWeek = 2331	
	mFirstMonth = 8
	mLastMonth = 8 [0 ~ 11]	
	///////////////////////////////////////////////////////
	mDayNumbers = [7, 8, 9, 10, 11, 12, 13]	
	mFocusDay = [true, true, true, true, true, true, true]	
	mOddMonth = [false, false, false, false, false, false, false]	
	mWeek = 2332	
	mFirstMonth = 8
	mLastMonth = 8 [0 ~ 11]	
	///////////////////////////////////////////////////////
	mDayNumbers = [28, 29, 30, 1, 2, 3, 4]	
	mFocusDay = [true, true, true, false, false, false, false]	
	mOddMonth = [false, false, false, true, true, true, true]	
	mWeek = 2335	
	mFirstMonth = 9
	mLastMonth = 9 [0 ~ 11]
	///////////////////////////////////////////////////////
    */
    public void setWeekParams(HashMap<String, Integer> params, String tz) {
    	mTodayIndex = -1;
    	//mClickedDayIndex = -1;
    	
    	if (!params.containsKey(VIEW_PARAMS_WEEK)) {
            throw new InvalidParameterException("You must specify the week number for this view");
        }  
        
    	if (params.containsKey(VIEW_PARAMS_WEEK_PATTERN)) {
        	mWeekPattern = params.get(VIEW_PARAMS_WEEK_PATTERN);
        	if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
        		int test = -1;
        		test = 0;
        	}
        }
    	
        setTag(params);/////////////////////////////////////////////////////////////////        
        
        mTzId = tz;
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;
        
        if (params.containsKey(VIEW_PARAMS_MODE)) {
        	mViewMode = params.get(VIEW_PARAMS_MODE);  
        	
        	if (params.containsKey(VIEW_PARAMS_SELECTED_DAY_OF_WEEK_IN_EEMODE)) {
        		mSelectedDayOfWeekInEEMode = params.get(VIEW_PARAMS_SELECTED_DAY_OF_WEEK_IN_EEMODE);
            }
            
        	mHasSelectedDayInEEMode = mSelectedDayOfWeekInEEMode != -1;
        	
        } 
        else {
        	mViewMode = NORMAL_MODE_VIEW_PATTERN;
        }
        
        // We keep the current value for any params not present
        if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
            mHeight = params.get(VIEW_PARAMS_HEIGHT);            
        }       
        
        
        if (params.containsKey(VIEW_PARAMS_NUM_DAYS)) {
            mNumDays = params.get(VIEW_PARAMS_NUM_DAYS);
        }
        
        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
        	// VIEW_PARAMS_WEEK_START -> mFirstDayOfWeek
        	mFirstDayOfWeek = params.get(VIEW_PARAMS_WEEK_START);
        }
        
        
        mNumCells = mNumDays;

        // Allocate space for caching the day numbers and focus values
        mDayNumbers = new String[mNumCells];        
        mWeek = params.get(VIEW_PARAMS_WEEK);///////////////////////////////////////////////////////////////
        int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(mWeek);//Utils.getJulianMondayFromWeeksSinceEpoch(mWeek);
        Calendar time = GregorianCalendar.getInstance(mTimeZone);
        time.setFirstDayOfWeek(mFirstDayOfWeek);
        long julianMondayMills = ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek);
        time.setTimeInMillis(julianMondayMills);//time.setJulianDay(julianMonday);
        ETime.adjustStartDayInWeek(time);        

        //mFirstJulianDay = ETime.getJulianDay(time.getTimeInMillis(), mGmtoff);
        //mLastJulianDay = mFirstJulianDay + 6; 
        mFirstMonth = time.get(Calendar.MONTH);
        mFirstMonthTime = GregorianCalendar.getInstance(mTimeZone);
        mFirstMonthTime.setTimeInMillis(time.getTimeInMillis());

        // Figure out what day today is
        Calendar today = GregorianCalendar.getInstance(mTimeZone);
        today.setTimeInMillis(System.currentTimeMillis());
        mHasToday = false;
        
        int i = 0;        
        while(true) {
        	                        
            if (time.get(Calendar.YEAR) == today.get(Calendar.YEAR) && time.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                mHasToday = true;                
            }
            //time.add(Calendar.DAY_OF_MONTH, 1);
            mDayNumbers[i] = Integer.toString(time.get(Calendar.DAY_OF_MONTH));
            
            i++;
            
            if (i < mNumCells)
            	time.add(Calendar.DAY_OF_MONTH, 1);
            else 
            	break;
        }
        
        // We do one extra add at the end of the loop, if that pushed us to a
        // new month undo it
        //if (time.get(Calendar.DAY_OF_MONTH) == 1) {
        	//time.add(Calendar.DAY_OF_MONTH, -1);//time.monthDay--;            
        //}
        
        mLastMonth = time.get(Calendar.MONTH);//time.month;
        mLastMonthTime = GregorianCalendar.getInstance(mTimeZone);
        mLastMonthTime.setTimeInMillis(time.getTimeInMillis());
        
        
        updateSelectionPositions();        
        
        
        if (params.containsKey(VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT)) {
        	mOriginalListViewHeight = params.get(VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT);
        	mDateTextSize = (int) (mOriginalListViewHeight * MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT);
        	mMonthTextSize = mDateTextSize;
        	//mMonthTextSize = (mOriginalListViewHeight * MONTH_INDICATOR_SECTION_HEIGHT) * MONTHLISTVIEW_MONTH_TEXT_SIZE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT;
        }
        
        /*if (params.containsKey(SimpleWeekView.VIEW_PARAMS_PREVIOUS_WEEK_HEIGHT)) {
        	mPreviousWeekHeight = params.get(SimpleWeekView.VIEW_PARAMS_PREVIOUS_WEEK_HEIGHT);
        }*/ 
        
        if (params.containsKey(VIEW_PARAMS_MONTH_INDICATOR_HEIGHT)) {
        	mMonthIndicatorHeight = params.get(VIEW_PARAMS_MONTH_INDICATOR_HEIGHT);
        	
        	mMonthTextBottomPadding = (float)(mMonthIndicatorHeight * 
        			(1 - MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT)); // month indicator height�� 75%
        } 
        
        if (params.containsKey(VIEW_PARAMS_NORMAL_WEEK_HEIGHT)) {
        	mNormalWeekHeight = params.get(VIEW_PARAMS_NORMAL_WEEK_HEIGHT);
        	float radius = (mNormalWeekHeight / 2) / 2;
    		mSelectedCircleDrawableRadius = (int)radius;
    		mSelectedCircleDrawableSize = (int)(mSelectedCircleDrawableRadius * 2);
        	
        	
        	if (mViewMode == NORMAL_MODE_VIEW_PATTERN) {             	
            	mDateTextTopPadding = (int) (mNormalWeekHeight * MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);            	
            	mEventCircleTopPadding = (int) (mNormalWeekHeight * MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            	
            }
            else {
            	mDateTextTopPadding = (int) (mNormalWeekHeight * MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);  
            	mEventCircleTopPadding = (int) (mNormalWeekHeight * MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            	
            	//mNormalModeDateTextTopPadding = (int) (mNormalWeekHeight * MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);            	
            	//mNormalModeEventCircleTopPadding = (int) (mNormalWeekHeight * MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
            	
            }        	
        }        
        
        if (params.containsKey(VIEW_PARAMS_WEEK_PATTERN)) {
        	mWeekPattern = params.get(VIEW_PARAMS_WEEK_PATTERN);
        }
        
        if (params.containsKey(VIEW_PARAMS_ORIENTATION)) {
            mOrientation = params.get(VIEW_PARAMS_ORIENTATION);
        }

        updateToday(tz);
        mNumCells = mNumDays;//mNumCells = mNumDays + 1;

        // �� ���� Downward Move �� ���,
        // ���� �������� ���� MonthWeekView�� ��쿡 �ִϸ��̼��� ����ȴ�
        if (params.containsKey(VIEW_PARAMS_ANIMATE_CHANGE_TARGET_MONTH_IN_EEMODE)) {
        	if (INFO) Log.i(TAG, "VIEW_PARAMS_ANIMATE_CHANGE_TARGET_MONTH_IN_EEMODE");
        	// ���� ���⼭ �߿��� ����....
        	// change target month�� Ÿ�� day of month�� 1�� �ƴϸ� today ��� ���̴�
        	// �׷��Ƿ� �ش� .....
        	
        	// �׸��� mAnimateChangeTargetMonthInEEModeAlpha�� ����ϴ� ���� ����...
        	// ...
            synchronized (mAnimatorListener) {
                if (mChangeTargetMonthInEEModeAnimator != null) {
                	mChangeTargetMonthInEEModeAnimator.removeAllListeners();
                	mChangeTargetMonthInEEModeAnimator.cancel();
                }
                mChangeTargetMonthInEEModeAnimator = ObjectAnimator.ofInt(this, "animateChangeTargetMonthInEEModeAlpha",
                        0/*Math.max(mAnimateChangeTargetMonthInEEModeAlpha, 80)*/, 255);
                mChangeTargetMonthInEEModeAnimator.setDuration(500);
                mAnimatorListener.setAnimator(mChangeTargetMonthInEEModeAnimator);
                mAnimatorListener.setFadingIn(true);
                mChangeTargetMonthInEEModeAnimator.addListener(mAnimatorListener);                
                mChangeTargetMonthInEEModeAnimator.start();
            }
        }
    } 
    
    
    public ObjectAnimator setSelectedDayAlphaAnimUnderExistenceInEEMode(int selectedDayOfWeek, long animDuration) {    	
    	mHasSelectedDayInEEMode = true;
    	mSelectedDayOfWeekInEEMode = selectedDayOfWeek;
    	
    	synchronized (mAnimatorListener) {
            if (mChangeTargetMonthInEEModeAnimator != null) {
            	mChangeTargetMonthInEEModeAnimator.removeAllListeners();
            	mChangeTargetMonthInEEModeAnimator.cancel();
            }
            mChangeTargetMonthInEEModeAnimator = ObjectAnimator.ofInt(this, "animateChangeTargetMonthInEEModeAlpha",
                    100, 255);
            mChangeTargetMonthInEEModeAnimator.setDuration(animDuration);
            mAnimatorListener.setAnimator(mChangeTargetMonthInEEModeAnimator);
            mAnimatorListener.setFadingIn(true);
            mChangeTargetMonthInEEModeAnimator.addListener(mAnimatorListener);
                        
            return mChangeTargetMonthInEEModeAnimator;
        }
    }
    
    // setter for the 'box +n' alpha text used by the animator
    public void setAnimateChangeTargetMonthInEEModeAlpha(int alpha) {
    	if (INFO) Log.i(TAG, "setAnimateChangeTargetMonthInEEModeAlpha=" + String.valueOf(alpha) );
    	mAnimateChangeTargetMonthInEEModeAlpha = alpha;
        invalidate();///////////////////////////////////////////////////////////////////////////////////////////////////////
    }
    
    public ObjectAnimator setPrvSelectedDayAlphaAnimUnderExistenceInEEMode(long animDuration) {    	
    	
    	synchronized (mAnimatorListener) {
            if (mChangeTargetMonthInEEModeAnimator != null) {
            	mChangeTargetMonthInEEModeAnimator.removeAllListeners();
            	mChangeTargetMonthInEEModeAnimator.cancel();
            }
            mChangeTargetMonthInEEModeAnimator = ObjectAnimator.ofInt(this, "animateChangeTargetMonthInEEModeAlpha",
                    100, 0);
            mChangeTargetMonthInEEModeAnimator.setDuration(animDuration);
            mAnimatorListener.setAnimator(mChangeTargetMonthInEEModeAnimator);
            mAnimatorListener.setFadingIn(false);
            mChangeTargetMonthInEEModeAnimator.addListener(mAnimatorListener);
                        
            return mChangeTargetMonthInEEModeAnimator;
        }
    }
    
    int mDayOfWeekTextWidth;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        
        mDayOfWeekTextWidth = mWidth / 7;  
        updateSelectionPositions();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight);
    }
    
    

    @Override    
    protected void onDraw(Canvas canvas) {  
    	if (mViewMode == NORMAL_MODE_VIEW_PATTERN) {
    		onDrawInNRMode(canvas);    
        }
        else {
        	onDrawInEPMode(canvas);
        }
    }
    
    boolean mIsSelectedWeekViewForExitMonthEnterDay = false;
    public void onDrawInNRMode(Canvas canvas) {
    	drawWeekNums(canvas);    	
        
        drawEventExistenceCircle(canvas);   
        
    }
    
    public final int DRAW_TODAY_CIRCLE_TYPE_DONT_CARE = 0;
    public final int DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER = 1;
    public final int DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER = 2;
    
    
    
    public void onDrawInEPMode(Canvas canvas) {
    	drawWeekNums(canvas);    
    	
    	drawEventExistenceCircle(canvas); 
    	
    	drawSelectedDayInEPMode(canvas);
    }
    
    public boolean getEventExistenceStatus() {
    	if (mEventExistence != null) {
    		return true;
    	}
    	else 
    		return false;
    }
    
    public void drawEventExistenceCircle(Canvas canvas) {
    	
    	
    	// week pattern�� �߿��ϴ�
    	// circle center�� y��ǥ�� top���� week height�� 65% offset
    	// circle diameter�� week height�� 10%
    	if (mEventExistence != null) {
    		
    		int prvAlphaValue = 255;
    		
    		if (mDontDrawEventExistenceCircle) {
    			prvAlphaValue = mEventExisteneCirclePaint.getAlpha();
    			int alphaValue = mDontDrawEventExistenceCircleAlphaValue;
    			mEventExisteneCirclePaint.setAlpha(alphaValue);
        	}
    		
    		float radius = (mNormalWeekHeight * 0.1f) / 2;    		
    		
    		if (mWeekPattern == NORMAL_WEEK_PATTERN || mWeekPattern == NORMAL_WEEK_NONE_UPPERLINE_PATTERN || mWeekPattern == LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN){	    		
	    		float cy = mEventCircleTopPadding;
	    		for (int i=0; i<mNumDays; i++) {
					if (mEventExistence[i]) {
						float cx = computeTextXPosition(i);
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}
	    	}
    		else if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {        		
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
	    		// �ټ� ���� ������ �����Ѵ�
	    		// :1)valid prv week region--------------------
	    		//  2)invalid prv week region
	    		//  3)month indicator region
	    		//  4)invalid next week region
	    		//  5)valid next week region-------------------
	    		// PRV_WEEK_SECTION_HEIGHT
	    		// MONTH_INDICATOR_SECTION_HEIGHT
	    		// NEXT_WEEK_SECTION_HEIGHT        		        		
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
			else if (mWeekPattern == PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN || 
					mWeekPattern == PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
				float cy = mEventCircleTopPadding; 		  		
	    			    		
	    		for (int i=0; i<mPrvMonthWeekDaysNum; i++) {
	    			float cx = computeTextXPosition(i);
	    			
					if (mEventExistence[i]) {												
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}   		
			}			
			else if (mWeekPattern == NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
				  		
	    		float cy = mMonthIndicatorHeight + mEventCircleTopPadding;
	    		
	    		for (int i=mPrvMonthWeekDaysNum; i<mNumDays; i++) {
	    			float cx = computeTextXPosition(i);
	    			
					if (mEventExistence[i]) {										 
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}    		 
			}	
			else if (mWeekPattern == NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
				
	    		float cy = mEventCircleTopPadding;
	    		
	    		for (int i=mPrvMonthWeekDaysNum; i<mNumDays; i++) {
	    			float cx = computeTextXPosition(i);
	    			
					if (mEventExistence[i]) {										 
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
				}    	
			}
	    		    	
	    	if (mDontDrawEventExistenceCircle) {
	    		mEventExisteneCirclePaint.setAlpha(prvAlphaValue);
	    	}
    	}    	
    }
    
    private void drawClick(Canvas canvas, int offsetY) {
        
        	Calendar time = GregorianCalendar.getInstance(mTimeZone);        	
        	
        	time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());        	
        	time.add(Calendar.DAY_OF_MONTH, mClickedDayIndex);
            int dateNum = time.get(Calendar.DAY_OF_MONTH);
            
            makeSelectedCircleDrawableDimens(offsetY);
            
        	mSelectedDateDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
    		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
    		mSelectedDateDrawable.getPaint().setAntiAlias(true);
    		mSelectedDateDrawable.getPaint().setStyle(Style.FILL);
    		mSelectedDateDrawable.getPaint().setColor(Color.BLACK);		    		
    		
    		mSelectedDateDrawable.getPaint().setAlpha(mClickedDayAlpha);
			
    		// x�� text�� x�� �� ���̴�
    		int x = computeTextXPosition(mClickedDayIndex);
			int left = x - mSelectedCircleDrawableRadius;	    			
			int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
			int right = left + mSelectedCircleDrawableSize;
			int bottom = top + mSelectedCircleDrawableSize;
			
			mSelectedDateDrawable.setBounds(left, top, right, bottom);	
			mSelectedDateDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
			mSelectedDateDrawable.setDayOfMonth(dateNum);
			mSelectedDateDrawable.draw(canvas);   		
        
    }
    /*
    private void drawClick(Canvas canvas) {
        if (mClickedDayIndex != -1) {
        	Calendar time = GregorianCalendar.getInstance(mTimeZone);        	
        	
        	time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());        	
        	time.add(Calendar.DAY_OF_MONTH, mClickedDayIndex);
            int dateNum = time.get(Calendar.DAY_OF_MONTH);
            
            int offsetY = 0;            
            if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {       				        		
            	offsetY = mMonthIndicatorHeight;                  		
            }
        	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
        		    		
        		if (mPrvMonthWeekDaysNum > mClickedDayIndex) {
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
    		
    		if (mClickedDayIndex == mTodayIndex) {
    			mSelectedDateDrawable.getPaint().setColor(Color.RED);	
    		}
    		else 
    			mSelectedDateDrawable.getPaint().setColor(Color.BLACK);		
    		
    		
    		mSelectedDateDrawable.getPaint().setAlpha(mClickedDayAlpha);
			
    		// x�� text�� x�� �� ���̴�
    		int x = computeTextXPosition(mClickedDayIndex);
			int left = x - mSelectedCircleDrawableRadius;	    			
			int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
			int right = left + mSelectedCircleDrawableSize;
			int bottom = top + mSelectedCircleDrawableSize;
			
			mSelectedDateDrawable.setBounds(left, top, right, bottom);	
			mSelectedDateDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
			mSelectedDateDrawable.setDayOfMonth(dateNum);
			mSelectedDateDrawable.draw(canvas);   		
        }
    }
    */
    private void drawSelectedDayInEPMode(Canvas canvas) {
    	if (mHasSelectedDayInEEMode) {
    		Calendar time = GregorianCalendar.getInstance(mTimeZone);
    		//int selectedJulainDay = mFirstJulianDay + mSelectedDayOfWeekInEEMode;    		
    		//long selectedJulainDayMills = ETime.getMillisFromJulianDay(selectedJulainDay, mTimeZone);
    		//time.setTimeInMillis(selectedJulainDayMills);//time.setJulianDay(selectedJulainDay);
    		time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
    		
    		int selectedDayOfWeekToMonthWeekDayIndex = convertDayOfWeekOfCalendarToDayOfWeekIndexOfMonthWeekView(mSelectedDayOfWeekInEEMode);
    		time.add(Calendar.DAY_OF_MONTH, selectedDayOfWeekToMonthWeekDayIndex);
            int dateNum = time.get(Calendar.DAY_OF_MONTH);
            
            int offsetY = 0;            
            if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {       				        		
            	offsetY = mMonthIndicatorHeight;                  		
            }
        	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
        		    		
        		if (mPrvMonthWeekDaysNum > selectedDayOfWeekToMonthWeekDayIndex) //mSelectedDayOfWeekInEEMode) {
        		{
        			offsetY = 0;                    
        		}
        		else {
        			offsetY = mNormalWeekHeight + mMonthIndicatorHeight;                    
        		}        		
            }
            
            
            makeSelectedCircleDrawableDimens(offsetY);
            
            if (mHasToday && (mTodayIndex == selectedDayOfWeekToMonthWeekDayIndex)) {            
            	mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
        		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
                mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
                mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);    
                if (INFO) Log.i(TAG, "1:mAnimateChangeTargetMonthInEEModeAlpha=" + String.valueOf(mAnimateChangeTargetMonthInEEModeAlpha));
                mSelectedDateTodayDrawable.getPaint().setAlpha(mAnimateChangeTargetMonthInEEModeAlpha);
                mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);/////////////////////////////////////////////////////////////////////////////////////////////////////////////		
        		
        		// x�� text�� x�� �� ���̴�
        		int x = computeTextXPosition(mTodayIndex);
        		int left = x - mSelectedCircleDrawableRadius;	    			
        		int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
        		int right = left + mSelectedCircleDrawableSize;
        		int bottom = top + mSelectedCircleDrawableSize;
        		
        		mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);	
        		mSelectedDateTodayDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
        		mSelectedDateTodayDrawable.setDayOfMonth(dateNum);
        		mSelectedDateTodayDrawable.draw(canvas);   		
            }
            else {
            	mSelectedDateDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
        		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
        		mSelectedDateDrawable.getPaint().setAntiAlias(true);
        		mSelectedDateDrawable.getPaint().setStyle(Style.FILL);
        		if (INFO) Log.i(TAG, "2:mAnimateChangeTargetMonthInEEModeAlpha=" + String.valueOf(mAnimateChangeTargetMonthInEEModeAlpha));        		
        		mSelectedDateDrawable.getPaint().setColor(Color.BLACK);	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////		
        		mSelectedDateDrawable.getPaint().setAlpha(mAnimateChangeTargetMonthInEEModeAlpha);
        		
        		// x�� text�� x�� �� ���̴�
        		int x = computeTextXPosition(selectedDayOfWeekToMonthWeekDayIndex);
    			int left = x - mSelectedCircleDrawableRadius;	    			
    			int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
    			int right = left + mSelectedCircleDrawableSize;
    			int bottom = top + mSelectedCircleDrawableSize;
    			
    			mSelectedDateDrawable.setBounds(left, top, right, bottom);	
    			mSelectedDateDrawable.setDrawTextPosition(x, offsetY + mDateTextTopPadding);
    			mSelectedDateDrawable.setDayOfMonth(dateNum);
    			mSelectedDateDrawable.draw(canvas);   		
            }        	
    	}
    }
    	
    final int DRAW_MONTH_INDICATOR = 1;
    final int DRAW_UPPER_LINE = 2;
    final int DRAW_DAYS_OF_MONTH = 4;
    
    
	public void drawWeekNums(Canvas canvas) {
		float startX = 0;
		float startY = 0;
		float stopX = 0;
		float stopY = 0;
		
		int dayCircleOffsetY = 0;
		// Upper line�� startY/stopY ���� ������ ���� �� ���� ��찡 �ִ�
		// -startY = stopY = (int)mUpperLinePaint.getStrokeWidth();
		// -startY = stopY = (int)mMonthIndicatorHeight;
		// -startY = stopY =  mNormalWeekHeight + mMonthIndicatorHeight;
		
		// day of month�� drawing ��Ҵ� ������ ���� �� ���� ��찡 �ִ�
		// int firstDayIndex = 0;
		// int lastDayIndex = mNumDays;
		// int textY = mDateTextTopPadding;
		// ------------------------------------------------------------------------
		// int firstDayIndex = 0;
		// int lastDayIndex = mNumDays;
		// int textY = mMonthIndicatorHeight + mDateTextTopPadding;
		// ------------------------------------------------------------------------
		// int firstDayIndex = mPrvMonthWeekDaysNum;
		// int lastDayIndex = mNumDays;
		// int textY = mMonthIndicatorHeight + mDateTextTopPadding;
		// ------------------------------------------------------------------------		
		// int firstDayIndex = 0;
		// int lastDayIndex = mPrvMonthWeekDaysNum;
		// int textY = mDateTextTopPadding;
		// ------------------------------------------------------------------------
		// firstDayIndex = mPrvMonthWeekDaysNum;
		// lastDayIndex = mNumDays;
		// textY = mNormalWeekHeight + mMonthIndicatorHeight + mDateTextTopPadding;
		// ------------------------------------------------------------------------
		// int firstDayIndex = mPrvMonthWeekDaysNum;
		// int lastDayIndex = mNumDays;
		// int textY = mDateTextTopPadding;
		// ------------------------------------------------------------------------
		
		// month indicator�� text y ��ǥ���� ������ ���� �� ���� ��찡 �ִ�
		// -float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
		// -float monthIndicatorTextY = mNormalWeekHeight + mMonthIndicatorHeight - mMonthTextBottomPadding;
		if (mWeekPattern == NORMAL_WEEK_PATTERN || mWeekPattern == LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {
			startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
			stopX = mWidth;
			
			if (!mDontDrawWeekUpperLine)
				drawUpperLine(canvas, startX, startY, stopX, stopY);		
			
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays - 1;
			int textY = mDateTextTopPadding;
			dayCircleOffsetY = 0;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
        }
		else if (mWeekPattern == PSUDO_NORMALWEEK_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE) {
			// NORMAL_WEEK_PATTERN�� �� ����
			startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
			stopX = mWidth;
			
			if (!mDontDrawWeekUpperLine)
				drawUpperLine(canvas, startX, startY, stopX, stopY);		
			
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays - 1;
			int textY = mDateTextTopPadding;
			dayCircleOffsetY = 0;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    	}   
		else if (mWeekPattern == NORMAL_WEEK_NONE_UPPERLINE_PATTERN) {    		
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays - 1;
			int textY = mDateTextTopPadding;
			dayCircleOffsetY = 0;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    	}   
		else if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
			startY = stopY = mMonthIndicatorHeight; 	     
			stopX = mWidth;
			
			if (!mDontDrawWeekUpperLine)
				drawUpperLine(canvas, startX, startY, stopX, stopY);
			        
			float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
			drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mFirstMonthTime.getTimeInMillis()), 0, monthIndicatorTextY);		
			
			int firstDayIndex = 0;
			int lastDayIndex = mNumDays - 1;
			int textY = mMonthIndicatorHeight + mDateTextTopPadding;
			dayCircleOffsetY = mMonthIndicatorHeight;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
        }
		else if (mWeekPattern == MONTHIDICATOR_AND_UPPERLINE_OF_FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
			startY = stopY = mMonthIndicatorHeight; 	     
			stopX = mWidth;
			
			if (!mDontDrawWeekUpperLine)
				drawUpperLine(canvas, startX, startY, stopX, stopY);
			        
			float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
			drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mFirstMonthTime.getTimeInMillis()), 0, monthIndicatorTextY);					
        }
		else if (mWeekPattern == PSUDO_FIRSTDAY_FIRSTWEEK_PATTERN_AT_EXPAND_EVENTVIEW_ENTRANCE) {
			// FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN�� �� ����
			startY = stopY = mMonthIndicatorHeight; 	     
			stopX = mWidth;
			
			if (!mDontDrawWeekUpperLine)
				drawUpperLine(canvas, startX, startY, stopX, stopY);
			
			float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mFirstMonthTime.getTimeInMillis()), 0, monthIndicatorTextY);   
    		    		
    		int firstDayIndex = 0;
			int lastDayIndex = mNumDays - 1;
			int textY = mMonthIndicatorHeight + mDateTextTopPadding;
			dayCircleOffsetY = mMonthIndicatorHeight;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);		
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
        }
		else if (mWeekPattern == PSUDO_FIRSTWEEK_OF_TWO_DIFFWEEKS_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE) {
			// NEXT_MONTH_WEEK_OF_TWO_DIFF_FIRSTWEEK_PATTERN�� �� ����???
			
			// PSUDO_FIRSTWEEK_OF_TWO_DIFFWEEKS_PATTERN_AT_EXPAND_EVENTLISTVIEW_ENTRANCE
    		// :���� ���̹��� �߸��� �� �ϴ�...
    		//  �ش� ���� ù��° ���� first day�� �ش� ���� day of month�� �ƴ� ���� ���� day of month��
    		//  �ش� ���� ù��° �ָ� �ǹ��ϴ� �� ����
    		
    		calPrvMonthWeekDaysNum();            
            
            float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY); 
            
    		startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
            startY = stopY = mMonthIndicatorHeight;
            stopX = mWidth;
    		if (!mDontDrawWeekUpperLine) {            
    			drawUpperLine(canvas, startX, startY, stopX, stopY);    
    		}
    		
    		int firstDayIndex = mPrvMonthWeekDaysNum;
			int lastDayIndex = mNumDays - 1;
			int textY = mMonthIndicatorHeight + mDateTextTopPadding;
			dayCircleOffsetY = mMonthIndicatorHeight;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);    
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    	}		
    	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		startY = stopY = (int)mUpperLinePaint.getStrokeWidth();   		
    		  
    		calPrvMonthWeekDaysNum();	 
    		stopX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
    		
    		if (!mDontDrawWeekUpperLine)
    			drawUpperLine(canvas, startX, startY, stopX, stopY);
    		
    		startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
    		stopX = mWidth;
    		startY = stopY =  mNormalWeekHeight + mMonthIndicatorHeight;
    		
    		if (!mDontDrawWeekUpperLine)
    			drawUpperLine(canvas, startX, startY, stopX, stopY);
    		    		           
    		float monthIndicatorTextY = mNormalWeekHeight + mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY);    		
			
			int firstDayIndex = 0;
			int lastDayIndex = mPrvMonthWeekDaysNum - 1;
			int textY = mDateTextTopPadding;
			dayCircleOffsetY = 0;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
						
			firstDayIndex = mPrvMonthWeekDaysNum;
			lastDayIndex = mNumDays - 1;
			textY = mNormalWeekHeight + mMonthIndicatorHeight + mDateTextTopPadding;
			dayCircleOffsetY = mNormalWeekHeight + mMonthIndicatorHeight;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
        }
    	else if (mWeekPattern == PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		calPrvMonthWeekDaysNum(); 
    		
    		startY = stopY = (int)mUpperLinePaint.getStrokeWidth(); 	     
			stopX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
			if (!mDontDrawWeekUpperLine)
				drawUpperLine(canvas, startX, startY, stopX, stopY);   	  
			
    		if (!mDrawSecondaryActionBarMonthDayHeader) {   		    
    			int firstDayIndex = 0;
    			int lastDayIndex = mPrvMonthWeekDaysNum - 1;
    			int textY = mDateTextTopPadding;
    			dayCircleOffsetY = 0;
    			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
    			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    		}
    		else {
    			drawSecondaryActionBarMonthDayHeader(canvas);
    		}
    	}
    	else if (mWeekPattern == PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) { 
    		calPrvMonthWeekDaysNum();	
    		           
    		if (!mDrawSecondaryActionBarMonthDayHeader) {
    			        		
    			int firstDayIndex = 0;
    			int lastDayIndex = mPrvMonthWeekDaysNum - 1;
    			int textY = mDateTextTopPadding;
    			dayCircleOffsetY = 0;
    			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
    			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    		}
    		else {    			
    			drawSecondaryActionBarMonthDayHeader(canvas);   
    		}           
    	}    	
    	else if (mWeekPattern == NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		
    		calPrvMonthWeekDaysNum(); 
    		
    		if(!mDrawSecondaryActionBarMonthDayHeader) {
    			int firstDayIndex = mPrvMonthWeekDaysNum;
    			int lastDayIndex = mNumDays - 1;
    			int textY = mDateTextTopPadding;
    			dayCircleOffsetY = 0;
    			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);
    			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    		}
    		else {
    			drawSecondaryActionBarMonthDayHeader(canvas);
    		}
    		
    	}
    	else if (mWeekPattern == NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN){    		
    		calPrvMonthWeekDaysNum();            
            
            float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY); 
            
            startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
            startY = stopY = mMonthIndicatorHeight;
            stopX = mWidth;
            if (!mDontDrawWeekUpperLine)
            	drawUpperLine(canvas, startX, startY, stopX, stopY);    		
    		
    		int firstDayIndex = mPrvMonthWeekDaysNum;
			int lastDayIndex = mNumDays - 1;
			int textY = mMonthIndicatorHeight + mDateTextTopPadding;
			dayCircleOffsetY = mMonthIndicatorHeight;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);	
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
    	}  
    	else if (mWeekPattern == NEXT_MONTH_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		calPrvMonthWeekDaysNum();   		
    		
    		float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
    		drawMonthIndicator(canvas, calMonthIndicatorTimeMillis(mLastMonthTime.getTimeInMillis()), mPrvMonthWeekDaysNum, monthIndicatorTextY); 
    		
    		startX = mDayOfWeekTextWidth * mPrvMonthWeekDaysNum;
            startY = stopY = mMonthIndicatorHeight;
            stopX = mWidth;
            if (!mDontDrawWeekUpperLine)
            	drawUpperLine(canvas, startX, startY, stopX, stopY);            
    	}    	
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
		int end = lastDayIndex + 1;
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
            
            mMonthNumPaint.setColor(color);
            
            if (alphaBlending) {
            	mMonthNumPaint.setAlpha(alphaValue);
            }            
            
            if (i == mTodayIndex || i == mClickedDayIndex) {
            	continue;
            }            
            	
            canvas.drawText(mDayNumbers[i], computeTextXPosition(i), y, mMonthNumPaint);            
        }   
        
        if (alphaBlending) {
        	mMonthNumPaint.setAlpha(prvAlphaValue);
        }        
        
	}
		
	// ���� mTodayIndex != -1�� üũ���� ������ mToday �����ؼ� �۾��� �����ϰ� �ִ� ...
	// �ٽ� �ѹ� drawSecondaryActionBarMonthDayHeader ȣ���ϴ� ���� ������ �����غ��� �Ѵ�
	public void drawSecondaryActionBarMonthDayHeader(Canvas canvas) {
		
		int firstDayIndex;
		int lastDayIndex;
        int textY = mDateTextTopPadding;  	                
        int dayCircleOffsetY = 0;
        if (mClickedDayIndex < mPrvMonthWeekDaysNum) {        	
        	   	        	
        	// false�� ��� today�� 2016/01/01�̰� 2016/01/02�� �����Ͽ��� ���
			// today red circle�� ������� today ��¥�� ���������� ���ϰ� �ȴ�
        	
        	// prv Month�� last week�� ���� day�� ���õǾ����Ƿ�    	        	
			firstDayIndex = 0;
			lastDayIndex = mPrvMonthWeekDaysNum - 1;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);    
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
			
        	// next Month�� first week�� ������ ��Ÿ���� �Ѵ�
        	firstDayIndex = mPrvMonthWeekDaysNum;
			lastDayIndex = mNumDays - 1;
			// ���� ������ ��Ÿ���� �ϴ� day�߿� today�� �մٸ�,,,
			// :�̶� red cicrle�� drawing �Ǵ� �������� �߻��Ѵ�....
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, true, mDrawSecondaryActionBarMonthDayHeaderAlphaValue);    
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);		
			   	        	
        }
        else {   
        	// next Month�� first week�� ���� day�� ���õǾ����Ƿ�
			firstDayIndex = mPrvMonthWeekDaysNum;
			lastDayIndex = mNumDays - 1;
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, false, 0);		
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
			
        	// prv Month�� last week�� ������ ��Ÿ���� �Ѵ�
        	firstDayIndex = 0;
			lastDayIndex = mPrvMonthWeekDaysNum - 1;			
			drawDayOfMonth(canvas, firstDayIndex, lastDayIndex, textY, true, mDrawSecondaryActionBarMonthDayHeaderAlphaValue);		
			drawDayCircle(canvas, firstDayIndex, lastDayIndex, textY, dayCircleOffsetY);
        } 
	}
		
	
    final int PRV_MONTH_LAST_WEEK_APPLY_ALPHA_VALUE = 1;
    final int NEXT_MONTH_FIRST_WEEK_APPLY_ALPHA_VALUE = 2;
    public void drawRecoverySelectedWeekToOriWeekFormat(Canvas canvas) {
    	    	
    	int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int weekLineY = (int)mUpperLinePaint.getStrokeWidth();
		
        int prvWeekX = 0;
        int nextWeekX = 0;
        int y = mDateTextTopPadding;
        mMonthNumPaint.setTextSize(mDateTextSize);
        mMonthNumPaint.setColor(mMonthNumColor);        
       
        mSelectedWeekAlphaPaint.setTextSize(mDateTextSize);
        mSelectedWeekAlphaPaint.setColor(mMonthNumColor);
        mSelectedWeekAlphaPaint.setAlpha(mDrawSecondaryActionBarMonthDayHeaderAlphaValue);
                
        Calendar prvWeekMonthTime = GregorianCalendar.getInstance(mTimeZone);        
        prvWeekMonthTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        
        int maxMonthDays = prvWeekMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);     
        mPrvMonthWeekDaysNum = (maxMonthDays - prvWeekMonthTime.get(Calendar.DAY_OF_MONTH)) + 1;
        
        if (mClickedDayIndex < mPrvMonthWeekDaysNum) {        
        	canvas.drawLine(0, weekLineY, dayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mUpperLinePaint); // alpha �� �����ؾ� �Ѵ�
        	// prv Month�� last week�� ���� day�� ���õǾ����Ƿ�
        	for (int i=0; i < mPrvMonthWeekDaysNum; i++) {  
        		prvWeekX = computeTextXPosition(i);
        		canvas.drawText(mDayNumbers[i], prvWeekX, y, mMonthNumPaint);
        	}
        	// next Month�� first week�� ������ ��Ÿ���� �Ѵ�
        	for (int i=mPrvMonthWeekDaysNum; i<this.mNumDays; i++) {
        		nextWeekX = computeTextXPosition(i);
        		canvas.drawText(mDayNumbers[i], nextWeekX, y, mSelectedWeekAlphaPaint);
        	}
        }
        else {        	
        	canvas.drawLine(dayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mWidth, weekLineY, mUpperLinePaint); // alpha �� �����ؾ� �Ѵ�
        	// prv Month�� last week�� ������ ��Ÿ���� �Ѵ�
        	for (int i=0; i < mPrvMonthWeekDaysNum; i++) {  
        		prvWeekX = computeTextXPosition(i);
        		canvas.drawText(mDayNumbers[i], prvWeekX, y, mSelectedWeekAlphaPaint);
        	}
        	// next Month�� first week�� ���� day�� ���õǾ����Ƿ�
        	for (int i=mPrvMonthWeekDaysNum; i<this.mNumDays; i++) {
        		nextWeekX = computeTextXPosition(i);
        		canvas.drawText(mDayNumbers[i], nextWeekX, y, mMonthNumPaint);
        	}        	
        }        
    }
    
    
    public void drawToday(Canvas canvas, int offsetY, int alpha) {        
        
    	Calendar time = GregorianCalendar.getInstance(mTimeZone);
    	
        time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        time.add(Calendar.DAY_OF_MONTH, mTodayIndex);
        int todayDateNum = time.get(Calendar.DAY_OF_MONTH);
                
        makeSelectedCircleDrawableDimens(offsetY);
        
        mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
        mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
        mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
        mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);	
        
        mSelectedDateTodayDrawable.getPaint().setAlpha(alpha);
		
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
    
    public void drawToday(Canvas canvas, int dayCircleOffsetY, int textY, float fraction) {        
        
    	Calendar time = GregorianCalendar.getInstance(mTimeZone);
    	
        time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        time.add(Calendar.DAY_OF_MONTH, mTodayIndex);
        int todayDateNum = time.get(Calendar.DAY_OF_MONTH);
                
        makeSelectedCircleDrawableDimens(dayCircleOffsetY);
        
        mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthNumPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
        mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
        mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
        mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);	
        
        int ovalAlpha = 255 - (int)(255 * fraction);
        mSelectedDateTodayDrawable.getPaint().setAlpha(ovalAlpha);
		
		// x�� text�� x�� �� ���̴�
		int x = computeTextXPosition(mTodayIndex);
		int left = x - mSelectedCircleDrawableRadius;	    			
		int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
		int right = left + mSelectedCircleDrawableSize;
		int bottom = top + mSelectedCircleDrawableSize;
		
		mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);	
		mSelectedDateTodayDrawable.setDrawTextPosition(x, dayCircleOffsetY + mDateTextTopPadding);
		mSelectedDateTodayDrawable.setDayOfMonth(todayDateNum);
		mSelectedDateTodayDrawable.draw(canvas);   	
				
		int prvColor = mMonthNumPaint.getColor();
		int prvAlpha = mMonthNumPaint.getAlpha();
		mMonthNumPaint.setColor(Color.RED);
		int textAlpha = (int) (255 * fraction);
		mMonthNumPaint.setAlpha(textAlpha);
		canvas.drawText(mDayNumbers[mTodayIndex], computeTextXPosition(mTodayIndex), textY, mMonthNumPaint);
		mMonthNumPaint.setColor(prvColor);
		mMonthNumPaint.setAlpha(prvAlpha);
		
    }
    
    public void drawToday(Canvas canvas, int textY, float fraction) {				
		int prvColor = mMonthNumPaint.getColor();
		int prvAlpha = mMonthNumPaint.getAlpha();
		
		mMonthNumPaint.setColor(Color.RED);
		int textAlpha = (int) (255 * fraction);
		mMonthNumPaint.setAlpha(textAlpha);
		canvas.drawText(mDayNumbers[mTodayIndex], computeTextXPosition(mTodayIndex), textY, mMonthNumPaint);
		
		mMonthNumPaint.setColor(prvColor);
		mMonthNumPaint.setAlpha(prvAlpha);
		
    }
      
    
    SelectedDateOvalDrawable mSelectedDateDrawable;
    SelectedDateOvalDrawable mSelectedDateTodayDrawable;

    int mSelectedCircleDrawableRadius;
	int mSelectedCircleDrawableCenterY;
	int mSelectedCircleDrawableSize;
    public void makeSelectedCircleDrawableDimens(int offsetY) {
				
    	float baseLineY = mDateTextTopPadding;
    	
    	float textAscent = mMonthNumPaint.ascent();
    	float textDescent = mMonthNumPaint.descent();
    	
    	float textTopY = baseLineY + textAscent; 	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float halfOfTextHeight = textHeight / 2;
    	float textCenterY = offsetY + (textTopY + halfOfTextHeight);
    	
    	mSelectedCircleDrawableCenterY = (int) textCenterY;   	
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
    
    boolean mEventExistence[] = null;
    public void createEventExistenceCircle(ArrayList<Event> unsortedEvents) {
    	//Context context = getContext();
    	// getView���� ���� ������ MonthWeekView�� Adapter���� setEvents��  ȣ���� ���
    	// createEventExistenceCircle���� mWidth ���� �翬�� 0�̴�...
    	// mWidth <= MIN_WEEK_WIDTH ... �����ε�...�� �̷� �ڵ带 ���� ���ΰ�?
    	// :�׳� ��������
    	
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
    
    
    /**
     * Returns the month of the first day in this week
     *
     * @return The month the first day of this view is in
     */
    public Calendar getFirstMonthTime() {
        return mFirstMonthTime;
    }

    /**
     * Returns the month of the last day in this week
     *
     * @return The month the last day of this view is in
     */
    public Calendar getLastMonthTime() {
        return mLastMonthTime;
    }

    /**
     * Returns the julian day of the first day in this view.
     *
     * @return The julian day of the first day in the view.
     */
    /*public int getFirstJulianDay() {
        return mFirstJulianDay;
    }*/

    /**
     * Returns the julian day of the last day in this view.
     *
     * @return The julian day of the last day in the view.
     */
    /*public int getLastJulianDay() {
    	return mLastJulianDay;
    }*/
    
    public int getWeekNumber() {
    	return mWeek;
    }
    
    public int getWeekPattern() {
    	return mWeekPattern;
    }
    
    public int getPrvMonthWeekDaysNum() {
    	return mPrvMonthWeekDaysNum;
    }    
    
    //mDontDrawUpperLine
    public void setDontDrawWeekUpperLine() {
    	mDontDrawWeekUpperLine = true;
    }
    
    public boolean getDrawSecondaryActionBarMonthDayHeader() {
    	return mDrawSecondaryActionBarMonthDayHeader;
    }
    
    public void setDrawSecondaryActionBarMonthDayHeader() {
    	mDrawSecondaryActionBarMonthDayHeader = true;
    }
    
    public void setDrawSecondaryActionBarMonthDayHeaderAlphaValue(int alphaValue) {
    	mDrawSecondaryActionBarMonthDayHeaderAlphaValue = alphaValue;
    }
    
    public boolean getDontDrawEventExistenceCircle() {
    	return mDontDrawEventExistenceCircle;
    }
    
    public void setDontDrawEventExistenceCircle() {
    	mDontDrawEventExistenceCircle = true;
    }
    
    public void setDontDrawEventExistenceCircleAlphaValue(int alphaValue) {
    	mDontDrawEventExistenceCircleAlphaValue = alphaValue;
    }
    
    float mAnimationFraction = 0;
    public void setAnimationFraction(float fraction) {
    	mAnimationFraction = fraction;
    }
    
    
    
    public int getDayIndexFromLocation(float x, float y) {
    	int dayStart = 0;
    	
    	// mHeight
    	// mParentHeight
    	if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
    		//SimpleWeekView.MONTH_INDICATOR_SECTION_HEIGHT
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
    		int nextWeekNumDays = mNumDays - mPrvMonthWeekDaysNum;
    		
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
    
    
    /**
     * Calculates the day that the given x position is in, accounting for week
     * number. Returns a Time referencing that day or null if
     *
     * @param x The x position of the touch event
     * @return A time object for the tapped day or null if the position wasn't
     *         in a day
     */
    public Calendar getDayFromLocation(float x, float y) {
    	int dayPosition = getDayIndexFromLocation(x, y);
        if (dayPosition == -1) {
            return null;
        }          

        Calendar time = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZone);
        time.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
        time.add(Calendar.DAY_OF_MONTH, dayPosition);
        if (mWeek == 0) {
            // This week is weird...
        	/*
            if (day < ETime.ECALENDAR_EPOCH_JULIAN_DAY_FOR_BEFORE_UNIX_TIME) {
                day++;
            } else if (day == ETime.ECALENDAR_EPOCH_JULIAN_DAY_FOR_BEFORE_UNIX_TIME) {
                time.set(CalendarController.MIN_CALENDAR_YEAR, 0, 1, 0, 0, 0);//time.set(1, 0, 1970);
                //time.normalize(true);
                return time;
            }
            */
        }        
        return time;
    }
    
    
    // callers
    // -setWeekParams
    public boolean updateToday(String tz) {
    	Calendar firstMonthTime = GregorianCalendar.getInstance(TimeZone.getTimeZone(tz));
    	Calendar lastMonthTime = GregorianCalendar.getInstance(TimeZone.getTimeZone(tz));
    	
    	firstMonthTime.setTimeInMillis(mFirstMonthTime.getTimeInMillis());
    	lastMonthTime.setTimeInMillis(mLastMonthTime.getTimeInMillis());
    	
    	firstMonthTime.set(Calendar.HOUR_OF_DAY, 0);
    	lastMonthTime.set(Calendar.MINUTE, 0);
    	lastMonthTime.set(Calendar.HOUR_OF_DAY, 23);
    	lastMonthTime.set(Calendar.MINUTE, 59);
    	
        mToday = GregorianCalendar.getInstance(TimeZone.getTimeZone(tz));
        mToday.setTimeInMillis(System.currentTimeMillis());
        //mToday.set(Calendar.MONTH, Calendar.JANUARY);
        //mToday.set(Calendar.DAY_OF_MONTH, 1);
        /*if (INFO) Log.i(TAG, "*********************************************");
        if (INFO) Log.i(TAG, "*********************************************");
        if (INFO) Log.i(TAG, "updateToday:TODAY:" + ETime.format2445(mToday));
        if (INFO) Log.i(TAG, "updateToday:FIRST:" + ETime.format2445(firstMonthTime));
        if (INFO) Log.i(TAG, "updateToday:LAST:" + ETime.format2445(lastMonthTime));*/
        
        if (mToday.getTimeInMillis() >= firstMonthTime.getTimeInMillis() && 
        		mToday.getTimeInMillis() <= lastMonthTime.getTimeInMillis())         		
        {        	
        	mHasToday = true;        	
        	//mTodayIndex = mToday.get(Calendar.DAY_OF_MONTH) - mFirstMonthTime.get(Calendar.DAY_OF_MONTH);// �߸��Ǿ���
        	       
        	mTodayIndex = convertDayOfWeekOfCalendarToDayOfWeekIndexOfMonthWeekView(mToday.get(Calendar.DAY_OF_WEEK));
        	//if (mToday.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek) {        		
        		//mTodayIndex = convertDayOfWeekOfCalendarToDayOfWeekOfMonthWeekView(mToday.get(Calendar.DAY_OF_WEEK));        		
        	//}
        	
        	//if (INFO) Log.i(TAG, "updateToday:TODAY BINGO");
        }
        else {
        	
        }
        /*if (INFO) Log.i(TAG, "*********************************************");
        if (INFO) Log.i(TAG, "*********************************************");*/
        return mHasToday;
    }
    
    
    public boolean updateSelectedDayInEEMode(String tz) {
    	return true;
    }
    
    public void updateSelectionPositions() {
        if (mHasSelectedDayInEEMode) {
            //int selectedPosition = mSelectedDayOfWeekInEEMode - mFirstDayOfWeek;
            //if (selectedPosition < 0) {
                //selectedPosition += 7;
            //}
        	int selectedPosition = convertDayOfWeekOfCalendarToDayOfWeekIndexOfMonthWeekView(mSelectedDayOfWeekInEEMode);
            int effectiveWidth = mWidth;
            effectiveWidth -= SPACING_WEEK_NUMBER;
            mSelectedLeft = selectedPosition * effectiveWidth / mNumDays;
            mSelectedRight = (selectedPosition + 1) * effectiveWidth / mNumDays;
            mSelectedLeft += SPACING_WEEK_NUMBER;
            mSelectedRight += SPACING_WEEK_NUMBER;
        }
    }
    
    
    int mClickedDayIndexInEEMode = -1; // �̰��� mSelectedDayOfWeekInEEMode�� �����ϴ� ����� ã�ƾ� ��
    
    // MonthWeekView�� ��ġ �̺�Ʈ �����ʴ� MonthAdapter���� �����Ѵ�
    // MonthAdapter.onTouch���� launch�� mDoClick Runable����
    // onTouch���� ������ event�� x/y ��ǥ ������ � weekDay�� ���õǾ������� �Ǻ��ϴ� ���̴�
    public void setClickedDayIndex(float xLocation, float yLocation) {
    	if (mViewMode == NORMAL_MODE_VIEW_PATTERN) {
    		mClickedDayIndex = getDayIndexFromLocation(xLocation, yLocation);
    		mClickedDayAlpha = 64;
    	}
    	else {
    		mHasSelectedDayInEEMode = true;
    		mClickedDayIndexInEEMode = getDayIndexFromLocation(xLocation, yLocation);    		
    	}
    	        
    	invalidate();
    }
    
    public int getClickedDayIndex() {
    	if (mViewMode == NORMAL_MODE_VIEW_PATTERN) {
    		return mClickedDayIndex;
    	}
    	else {
    		if (mHasSelectedDayInEEMode) {
    			return mClickedDayIndexInEEMode;
    		}
    		else
    			return -1;
    	}
    	
    }   
    
    
    public void clearClickedDay() {
    	if (mViewMode == NORMAL_MODE_VIEW_PATTERN) {
    		mClickedDayIndex = -1;
    		mClickedDayAlpha = 255;
    	}
    	else {
    		mHasSelectedDayInEEMode = false;
    		mClickedDayIndexInEEMode = -1;
    	}
    	        
        invalidate();
    }
    
    
    private int computeTextXPosition(int day) {
    	int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
        return x;
    }
    
    
    
    private ObjectAnimator mScaleTodayRedCircleInEEModeAnimator = null;
    
    public void setTodayRedCircleScaleAnimInEEMode(long animDuration) {    	
    	
    	synchronized (mScaleTodayRedCircleAnimatorListener) {
            if (mScaleTodayRedCircleInEEModeAnimator != null) {
            	mScaleTodayRedCircleInEEModeAnimator.removeAllListeners();
            	mScaleTodayRedCircleInEEModeAnimator.cancel();
            }
            
            int startValue = mNormalWeekHeight;
            int endValue = mNormalWeekHeight + (int) (mNormalWeekHeight * 0.25f);
            mScaleTodayRedCircleInEEModeAnimator = ObjectAnimator.ofInt(this, "animateScaleTodayRedCircleInEEMode",
            		startValue, endValue);
            mScaleTodayRedCircleInEEModeAnimator.setDuration(animDuration);
            mScaleTodayRedCircleAnimatorListener.setAnimator(mScaleTodayRedCircleInEEModeAnimator);
            mScaleTodayRedCircleAnimatorListener.setInflate(true);
            mScaleTodayRedCircleInEEModeAnimator.addListener(mScaleTodayRedCircleAnimatorListener);
                        
            mScaleTodayRedCircleInEEModeAnimator.start();
        }
    }

    // setter for ? used by the mScaleTodayRedCircleInEEModeAnimator
    public void setAnimateScaleTodayRedCircleInEEMode(int delta) {
    	if (INFO) Log.i(TAG, "setAnimateScaleTodayRedCircleInEEMode=" + String.valueOf(delta) );
    	
    	float radius = (delta / 2) / 2;
		mSelectedCircleDrawableRadius = (int)radius;
		mSelectedCircleDrawableSize = (int)(mSelectedCircleDrawableRadius * 2);
		
        invalidate();///////////////////////////////////////////////////////////////////////////////////////////////////////
    }
    
    private final ScaleTodayRedCircleInEEModeAnimatorListener mScaleTodayRedCircleAnimatorListener = new ScaleTodayRedCircleInEEModeAnimatorListener();
    class ScaleTodayRedCircleInEEModeAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mInflated = false;
        
        @Override
        public void onAnimationStart(Animator animation) {
        	if (mInflated) {
        		if (INFO) Log.i(TAG, "Inflated:onAnimationStart");
        	}
        	else {
        		if (INFO) Log.i(TAG, "Deflated:onAnimationStart");
        	}
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            synchronized (this) {
                if (mAnimator != animation) {
                    animation.removeAllListeners();
                    animation.cancel();
                    return;
                }
                
                if (mInflated) {
                	if (INFO) Log.i(TAG, "Inflated:onAnimationEnd");
                	
                    if (mScaleTodayRedCircleInEEModeAnimator != null) {
                    	mScaleTodayRedCircleInEEModeAnimator.removeAllListeners();
                    	mScaleTodayRedCircleInEEModeAnimator.cancel();
                    }                         
                    
                    int startValue = mNormalWeekHeight + (int) (mNormalWeekHeight * 0.25f);
                    int endValue = MonthWeekView.this.mNormalWeekHeight;
                    
                    mScaleTodayRedCircleInEEModeAnimator = ObjectAnimator.ofInt(MonthWeekView.this,
                            "animateScaleTodayRedCircleInEEMode", startValue, endValue);
                    mAnimator = mChangeTargetMonthInEEModeAnimator;
                    mInflated = false;
                    mScaleTodayRedCircleInEEModeAnimator.addListener(this);
                    mScaleTodayRedCircleInEEModeAnimator.setDuration(200);
                    mScaleTodayRedCircleInEEModeAnimator.start();
                    
                } else {
                	if (INFO) Log.i(TAG, "Deflated:onAnimationEnd");
                	
                	if (mScaleTodayRedCircleInEEModeAnimator != null) {
                		mScaleTodayRedCircleInEEModeAnimator.removeAllListeners();
                		mScaleTodayRedCircleInEEModeAnimator.cancel();
                    }                	
                	
                	float radius = (MonthWeekView.this.mNormalWeekHeight / 2) / 2;
            		mSelectedCircleDrawableRadius = (int)radius;
            		mSelectedCircleDrawableSize = (int)(mSelectedCircleDrawableRadius * 2);
            		
                    mAnimator.removeAllListeners();
                    mAnimator = null;
                    mScaleTodayRedCircleInEEModeAnimator = null;
                    
                    invalidate();
                }
            }
        }

        public void setAnimator(Animator animation) {
            mAnimator = animation;
        }

        public void setInflate(boolean inflated) {
        	mInflated = inflated;
        }

    }
    
    
    private String buildMonth(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			mContext,
                mFormatter,
                startMillis,
                endMillis,
                ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_MONTH_DAY
                        | ETime.FORMAT_NO_YEAR, mTzId).toString();
    	
    	return date;
    	
    }
    
    
    public int convertDayOfWeekOfCalendarToDayOfWeekIndexOfMonthWeekView(int dayOfWeekOfCalendar) {
    	// sat / sun / m / t / w / t / f 
		// if today : sun[1], first day of week = saturday[7] = -6 -> -6 + 7 = 1
		// if today : mon[2], first day of week = saturday[7] = -5 -> -5 + 7 = 2
		// if today : tue[3], first day of week = saturday[7] = -4 -> -4 + 7 = 3
		// if today : wen[4], first day of week = saturday[7] = -3 -> -3 + 7 = 4	
		// if today : thr[5], first day of week = saturday[7] = -2 -> -2 + 7 = 5	
		// if today : fri[6], first day of week = saturday[7] = -1 -> -1 + 7 = 6	
		// if today : sat[7], first day of week = saturday[7] = 0	
		
		// s / m / t / w / t / f / s / sat
		// if today : sun[1], first day of week = sunday[1] = 0
		// if today : mon[2], first day of week = sunday[1] = 1
		// if today : tue[3], first day of week = sunday[1] = 2
		// if today : wen[4], first day of week = sunday[1] = 3	
		// if today : thr[5], first day of week = sunday[1] = 4	
		// if today : fri[6], first day of week = sunday[1] = 5	
		// if today : sat[7], first day of week = sunday[1] = 6	
		
		// m / t / w / t / f / s / sun
		// if today : sun[1], first day of week = monday[2] = -1 -> -1 + 7 = 6
		// if today : mon[2], first day of week = monday[2] = 0
		// if today : tue[3], first day of week = monday[2] = 1
		// if today : wen[4], first day of week = monday[2] = 2	
		// if today : thr[5], first day of week = monday[2] = 3	
		// if today : fri[6], first day of week = monday[2] = 4	
		// if today : sat[7], first day of week = monday[2] = 5	
    	int diff = dayOfWeekOfCalendar - mFirstDayOfWeek;
		if (diff < 0) {
			diff += ETime.NUM_DAYS;
		}
		
		return diff;
    }
    
    
    
    public int getDrawTodayTypeOfSelectedWeekViewForExitMonthEnterDay() {
    	//public final int DRAW_TODAY_CIRCLE_TYPE_DONT_CARE = 0;
        //public final int DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER = 1;
        //public final int DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER = 2;
    	
    	//mClickedDayIndex != mTodayIndex
    	int todayCircleType = 0;
    	if (mWeekPattern == NORMAL_WEEK_PATTERN || mWeekPattern == LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {
			// DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
        }
		
		else if (mWeekPattern == NORMAL_WEEK_NONE_UPPERLINE_PATTERN) {    		
			// DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
			todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    	}   
		else if (mWeekPattern == FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
			// DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
			todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
        }					
    	else if (mWeekPattern == TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {  // selected week�� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN ������ �߻��� �� ����   		
    		
        }
    	else if (mWeekPattern == PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		// today�� prv week�� �����ϴ°�?
    		//DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		    		
    		// today�� next week�� �����ϴ°�?
    		//DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		if (mTodayIndex < mPrvMonthWeekDaysNum) {
    			todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    		else {
    			todayCircleType = DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    	}
    	else if (mWeekPattern == PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) { 
    		// today�� prv week�� �����ϴ°�?
    		//DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		// today�� next week�� �����ϴ°�?
    		//DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		if (mTodayIndex < mPrvMonthWeekDaysNum) {
    			todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    		else {
    			todayCircleType = DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    	} 
    	else if (mWeekPattern == NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN){    		
    		// today�� next week�� �����ϴ°�?
    		// DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		// today�� prv week�� �����ϴ°�?
    		// DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		if (mPrvMonthWeekDaysNum <= mTodayIndex) {
    			todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    		else {
    			todayCircleType = DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    		
    	}  
    	else if (mWeekPattern == NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
    		// today�� next week�� �����ϴ°�?
    		// DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		// today�� prv week�� �����ϴ°�?
    		// DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER
    		
    		if (mPrvMonthWeekDaysNum <= mTodayIndex) {
    			todayCircleType = DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    		else {
    			todayCircleType = DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER;
    		}
    	}
    	
    	return todayCircleType;
    	  	
    }
    
    public void drawDayCircle(Canvas canvas, int firstDayIndex, int lastDayIndex, int textY, int dayCircleOffsetY) {
    	boolean drawToday = false;
    	boolean drawClickedDay = false;
    	if (mTodayIndex != -1 || mClickedDayIndex != -1) {
			
			if (firstDayIndex <= mTodayIndex && mTodayIndex <= lastDayIndex) {
				drawToday = true;
			}			
			
			if (firstDayIndex <= mClickedDayIndex && mClickedDayIndex <= lastDayIndex) {
				drawClickedDay = true;
			}
						
			if (drawToday || drawClickedDay) {
				
				if (mIsSelectedWeekViewForExitMonthEnterDay) { // mClickedDayIndex != -1
		    		if (mHasToday) { // mTodayIndex != -1
		    			if (mClickedDayIndex == mTodayIndex) { // click �� day�� today�� ���
							// drawToday:O, drawClick:X
							drawToday(canvas, dayCircleOffsetY, 255);
						}
						else { // click �� day�� today�� �ƴ� ���
							
							// mClickedDayIndex != -1 ���� �ƴ����� üũ�� �ʿ䰡 ���� : mIsSelectedWeekViewForExitMonth�� true��� ���� click�� day�� �����Ѵٴ� ���� �ǹ��Ѵ�
							drawClick(canvas, dayCircleOffsetY);	
							
							int drawTodayCircleType = getDrawTodayTypeOfSelectedWeekViewForExitMonthEnterDay();
								
							if (drawTodayCircleType == DRAW_DISAPPEAR_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER) {
								// �� ���� today�� 2016/01/01�̰� 2016/01/02�� �����Ͽ��� ���
								// today red circle�� ������� today ��¥�� ���������� ���ϰ� �ȴ�
								drawToday(canvas, dayCircleOffsetY, textY, mAnimationFraction);		
							}					
							else if (drawTodayCircleType == DRAW_NOT_TODAY_CIRCLE_BUT_DRAW_TODAY_DAY_NUMBER) {
								// �� ���� today�� 2016/01/01�̰� 2015/12/31�� �����Ͽ��� ���
								// today ��¥�� ���������� ���ϸ鼭 ���ܾ� �Ѵ�
								drawToday(canvas, textY, mAnimationFraction);
							}				
						}
		    		}
		    		else { // mIsSelectedWeekViewForExitMonth�� true��� ���� click�� day�� �����Ѵٴ� ���� �ǹ��Ѵ�
		    			drawClick(canvas, dayCircleOffsetY);	
		    		}
		    	}
		    	else {
		    		if (mHasToday) {    			
		    			if (mClickedDayIndex != -1) {
		    				if (mClickedDayIndex == mTodayIndex) { // click �� day�� today�� ���
		    					// drawToday:O, drawClick:X
		    					drawToday(canvas, dayCircleOffsetY, mClickedDayAlpha); // ������ alpha ���� ����Ǿ�� �Ѵ�
		    				}
		    				else { // click �� day�� today�� �ƴ� ���
		    					// �Ʒ� if (drawToday) ���ǹ��� ���ٸ�,
		    					// TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN���� next week�� today�� ������ ��쿡,
		    					// prv week�� day�� click �Ѵٸ�, 
		    					// next week�� prv week ���� ���� clicked�� ���Ͽ��� clicked circle�� drawing �Ǵ� �������� �߻��Ѵ�
		    					if (drawClickedDay)
		    						drawClick(canvas, dayCircleOffsetY);	// ������ alpha ���� ����Ǿ�� �Ѵ�
		    					
		    					// �Ʒ� if (drawToday) ���ǹ��� ���ٸ�,
		    					// TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN���� next week�� today�� ������ ��쿡,
		    					// prv week�� day�� click �Ѵٸ�, 
		    					// prv week�� next week ���� ���� today ���Ͽ��� today red circle�� drawing �Ǵ� �������� �߻��Ѵ�
		    					if (drawToday)
		    						drawToday(canvas, dayCircleOffsetY, 255);					
		    				}
		    			}
		    			else {
		    				// drawToday:O, drawClick:X
		    				drawToday(canvas, dayCircleOffsetY, 255);
		    			}    			
		    		}
		    		else { 
		    			if (mClickedDayIndex != -1)
		    				drawClick(canvas, dayCircleOffsetY);	// ������ alpha ���� ����Ǿ�� �Ѵ�
		    		}
					    		
		    	}
				
			}
		}
    	
    	
    }
    
    
    
}
