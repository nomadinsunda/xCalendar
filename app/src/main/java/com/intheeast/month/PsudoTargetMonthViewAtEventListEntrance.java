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
import com.intheeast.month.MonthFragment.EventListViewEntranceAnimCompletionCallback;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class PsudoTargetMonthViewAtEventListEntrance extends View {
	
	public static final String MONTHVIEW_PARAMS_LISTVIEW_NM_HEIGHT = "listview_normal_mode_height";
	
	public static final String MONTHVIEW_PARAMS_LISTVIEW_MONTHINDICATOR_HEIGHT = "listview_monthindicator_height";
	
	public static final String MONTHVIEW_PARAMS_LISTVIEW_NORMAL_WEEK_HEIGHT = "listview_normal_week_height";
	
	public static final String MONTHVIEW_PARAMS_TARGET_MONTH_HEIGHT_WITHIN_NMODE_MONTHLISTVIEW = "target_month_height";
    
    public static final String MONTHVIEW_PARAMS_NORMAL_WEEK_HEIGHT = "normal_week_height";
    
    public static final String MONTHVIEW_PARAMS_PREVIOUS_WEEK_HEIGHT = "previous_week_height";
    
    public static final String MONTHVIEW_PARAMS_MONTH_INDICATOR_HEIGHT = "month_indicator_height";
    
    public static final String MONTHVIEW_PARAMS_FIRSTJULIANDAY_IN_MONTH = "firstjulianday_in_month";
    
    public static final String MONTHVIEW_PARAMS_WEEK_START = "week_start";
    
    public static final String MONTHVIEW_PARAMS_NUM_DAYS = "num_days";
    
    //public final long DOWNSIZE_ANIM_DURATION_TIME = 400;
  	private static final int MINIMUM_VELOCITY = 3300;    
  	private static final int DEFAULT_VELOCITY = 4400; // ������ ��Ʈ
    //private static final int DEFAULT_VELOCITY = 5500; // ������ S4
    
	protected static final int DEFAULT_WEEK_START = Calendar.SUNDAY;
    protected static final int DEFAULT_NUM_DAYS = 7;
	protected static int MINI_DAY_NUMBER_TEXT_SIZE = 14;
	private static int TEXT_SIZE_MONTH_NUMBER = 32;
	private static int SPACING_WEEK_NUMBER = 24;
	
	Context mContext;
	Resources mResources;
	MonthFragment mMonthFragment = null;
	LinearLayout mPsudoNextMonthRegionContainer = null;	
	
	protected Drawable mTodayDrawable;
	SelectedDateOvalDrawable mSelectedDateTodayDrawable;
	Paint mMonthIndicatorTextPaint;
	Paint mMonthDateTextPaint;
	Paint mEventExisteneCirclePaint;
	Paint mUpperLinePaint;
	
	ValueAnimator mScaleValueAnimator;
	TranslateAnimation mPsudoNextMonthRegionContainerTranslateAnimation = null;	
	
	protected static StringBuilder mStringBuilder = new StringBuilder(50);    
    protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
    
    EventListViewEntranceAnimCompletionCallback mAnimCompletionCallback = null;
	
	    
    protected String mTzId = ETime.getCurrentTimezone();
    protected TimeZone mTimeZone = TimeZone.getTimeZone(mTzId);
    protected long mGmtoff = mTimeZone.getRawOffset() / 1000;
    // Which day of the week to start on [1-7]
    protected int mFirstDayOfWeek = DEFAULT_WEEK_START;
    // How many days to display
    protected int mNumDays = DEFAULT_NUM_DAYS;
    // The number of days + a spot for week number if it is displayed
    protected int mNumCells = mNumDays;
    protected String[] mDayNumbers;
    Calendar mToday = GregorianCalendar.getInstance();//new Time();
	int mJulianToday;	
    
    protected List<ArrayList<Event>> mEvents = null;
    protected ArrayList<Event> mUnsortedEvents = null;        
    boolean mEventExistence[] = null;
    
	// used for scaling to the device density
    protected static float mScale = 0;
    
    int mMaxMonthDay;
    int mWeekNumbers;
	int mIndexFirstDayOfMonth;
	
	int mFirstJulianDayInTargetMonth;
	long mTargetMonthFirstDayMills;
    
	String mMonthIndicatorText;
	
	protected int mBGColor;
    protected int mEEVMBGColor;
    protected int mEEVMTransparentBGColor;
    protected int mWeekNumColor;
	
	protected int mMonthDateTextColor;
	protected int mMonthNameColor;
    protected int mMonthBGTodayColor;
    protected int mTodayAnimateColor;
    
    final int mEEVMBGColor_RED;
    final int mEEVMBGColor_GREEN;
    final int mEEVMBGColor_BLUE;
    /////////////////////////////////////////////////////////////////////////////////////////
    // Dimens Variables    
    protected int mWidth;
    protected int mHeight;
    
    protected int mMonthListViewNModeHeight = 0;
    protected int mMonthListViewMonthIndicatorHeight = 0; // NMode�̳� EMode �̰ų� ������ dimen value�� ����Ѵ�
    protected int mMonthListViewNModeWeekHeight = 0;
    protected int mMonthListViewEModeWeekHeight = 0;
    int mUpdatedMonthListViewAnimWeekHeight;    
    
    int mWillBeScaleDownMonthListViewHeight;
    protected int mTargetMonthHeightWithinMonthListViewAtNMode = 0; 
    
    int mDayOfWeekTextWidth;
    float mMonthListViewDateTextSize;
    
    float mMonthTextBottomPadding;    
    int mMonthListViewNModeDateTextTopPadding;
	int mMonthListViewEModeDateTextTopPadding;	
	int mMonthListViewNModeEventCircleTopPadding;
	int mMonthListViewEModeEventCircleTopPadding;
	
	float mScalingTotalValue;	
	int mScaleDownTotalNormalWeekSize;	
	int mScaleTotalUpwardOffsetFromTop;
	int mUpwardTopOffsetDelta = 0;		
	
	int mScaleDownTotalDateTextTopPaddingSize;
	int mScaleDownDateTextTopPaddingDelta;
	
	int mScaleDownTotalEventCircleTopPaddingSize;
	int mScaleDownEventCircleTopPaddingDelta;	
	
	int mSelectedCircleDrawableRadius;
	int mSelectedCircleDrawableCenterY;
	int mSelectedCircleDrawableSize;
	
	int mEventListViewHeight;
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	private static boolean mInitialized = false;
    boolean mScaleAnimationEnd = false;
    boolean mLaunchNextMonthRegionContainer = false;
    boolean mDrawScaleMonth = false;
	boolean mStartScaleHeight = false;
	
	int mWeek_saturdayColor;
	int mWeek_sundayColor;
	
	public PsudoTargetMonthViewAtEventListEntrance(Context context) {
		super(context);
		
		mContext = context;
		mResources = context.getResources();
        mBGColor = mResources.getColor(R.color.month_bgcolor);        
        mWeekNumColor = mResources.getColor(R.color.month_week_num_color);  
        mEEVMBGColor = getResources().getColor(R.color.manageevent_actionbar_background);         
        
        mEEVMBGColor_RED = Color.red(mEEVMBGColor);
        mEEVMBGColor_GREEN = Color.green(mEEVMBGColor);
        mEEVMBGColor_BLUE = Color.blue(mEEVMBGColor);
        
        mEEVMTransparentBGColor = Color.argb(0, mEEVMBGColor_RED, mEEVMBGColor_GREEN, mEEVMBGColor_BLUE);

        if (mScale == 0) {
            mScale = context.getResources().getDisplayMetrics().density;            
        }
        
        initView();
	}
		
	protected void initView() {        

        if (!mInitialized) {        	         
            TEXT_SIZE_MONTH_NUMBER = mResources.getInteger(R.integer.text_size_month_number);
            
            if (mScale != 1) {                
                SPACING_WEEK_NUMBER *= mScale;
                TEXT_SIZE_MONTH_NUMBER *= mScale;  
                MINI_DAY_NUMBER_TEXT_SIZE *= mScale;
            }
            
            mInitialized = true;
        }
        
        loadColors(getContext());          
        
        mMonthIndicatorTextPaint = new Paint(); 
        mMonthIndicatorTextPaint.setAntiAlias(true);
        mMonthIndicatorTextPaint.setColor(Color.BLACK);
        mMonthIndicatorTextPaint.setTextAlign(Align.CENTER);
        
        mMonthDateTextPaint = new Paint();
        mMonthDateTextPaint.setFakeBoldText(false);
        mMonthDateTextPaint.setAntiAlias(true);
        mMonthDateTextPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);
        mMonthDateTextPaint.setColor(mMonthDateTextColor);
        mMonthDateTextPaint.setStyle(Style.FILL);        
        mMonthDateTextPaint.setTextAlign(Align.CENTER);
        mMonthDateTextPaint.setTypeface(Typeface.DEFAULT);        
        
        mEventExisteneCirclePaint = new Paint();    
        mEventExisteneCirclePaint.setAntiAlias(true);        
        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
        mEventExisteneCirclePaint.setStyle(Style.FILL);        
        
        mUpperLinePaint = new Paint();        
        mUpperLinePaint.setAntiAlias(true);        
        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
        mUpperLinePaint.setStyle(Style.STROKE);
        mUpperLinePaint.setStrokeWidth(getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight));
    }	
	
	protected void loadColors(Context context) {
                
		mMonthDateTextColor = mResources.getColor(R.color.month_day_number);          
        mMonthNameColor = mMonthDateTextColor;        
        mMonthBGTodayColor = mResources.getColor(R.color.month_today_bgcolor);        
        mTodayAnimateColor = mResources.getColor(R.color.today_highlight_color);        
        mTodayDrawable = mResources.getDrawable(R.drawable.today_blue_week_holo_light);
        
        mWeek_saturdayColor = getResources().getColor(R.color.week_saturday);
        mWeek_sundayColor = getResources().getColor(R.color.week_sunday);
    }	
	
	public void setFragment(MonthFragment monthFragment) {
		mMonthFragment = monthFragment;
	}	
	
	
	public void setEventListViewHeight(int height) {
		mEventListViewHeight = height;
	}
	
	
	public void IsMonthWithLessThanSixWeeks(boolean isOk) {
		mLaunchNextMonthRegionContainer = isOk;
	}
		
	
	public void setMonthParam(HashMap<String, Integer> params, String tz) {
		mTzId = tz;	
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		setToday();
		//setTodayTest();
		
		if (params.containsKey(MONTHVIEW_PARAMS_LISTVIEW_NM_HEIGHT)) {
			mMonthListViewNModeHeight = params.get(MONTHVIEW_PARAMS_LISTVIEW_NM_HEIGHT); 
			// �����޵��� ����
			mMonthListViewDateTextSize = (int) (mMonthListViewNModeHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT);
        }		
		
		if (params.containsKey(MONTHVIEW_PARAMS_LISTVIEW_MONTHINDICATOR_HEIGHT)) {
			mMonthListViewMonthIndicatorHeight = params.get(MONTHVIEW_PARAMS_LISTVIEW_MONTHINDICATOR_HEIGHT);  
			// �����޵��� ����
			mMonthTextBottomPadding = mMonthListViewMonthIndicatorHeight * (1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT); 
		}
		
		if (params.containsKey(MONTHVIEW_PARAMS_LISTVIEW_NORMAL_WEEK_HEIGHT)) {
			mMonthListViewNModeWeekHeight = params.get(MONTHVIEW_PARAMS_LISTVIEW_NORMAL_WEEK_HEIGHT);  
			// �Ʒ� �� �� ���� �޵��� ����
			mMonthListViewNModeDateTextTopPadding = (int) (mMonthListViewNModeWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);	
			mMonthListViewNModeEventCircleTopPadding = (int) (mMonthListViewNModeWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);	
		}		
		
		if (params.containsKey(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_EVM_NORMAL_WEEK_HEIGHT)) {
			mMonthListViewEModeWeekHeight = params.get(PsudoTargetMonthViewAtEventListExit.MONTHVIEW_PARAMS_LISTVIEW_EVM_NORMAL_WEEK_HEIGHT); 
			// �Ʒ� �� �� ���� �޵��� ����
			mMonthListViewEModeDateTextTopPadding = (int) (mMonthListViewEModeWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
			mMonthListViewEModeEventCircleTopPadding = (int) (mMonthListViewEModeWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
		}
		
		if (params.containsKey(MONTHVIEW_PARAMS_TARGET_MONTH_HEIGHT_WITHIN_NMODE_MONTHLISTVIEW)) {
			mTargetMonthHeightWithinMonthListViewAtNMode = params.get(MONTHVIEW_PARAMS_TARGET_MONTH_HEIGHT_WITHIN_NMODE_MONTHLISTVIEW);            
        }
		
		if (params.containsKey(MONTHVIEW_PARAMS_NUM_DAYS)) {
            mNumDays = params.get(MONTHVIEW_PARAMS_NUM_DAYS);
        }
		
		if (params.containsKey(MONTHVIEW_PARAMS_WEEK_START)) {        	
        	mFirstDayOfWeek = params.get(MONTHVIEW_PARAMS_WEEK_START);
        }
		
		mNumCells = mNumDays;
		
		mFirstJulianDayInTargetMonth = params.get(MONTHVIEW_PARAMS_FIRSTJULIANDAY_IN_MONTH);
        Calendar time = GregorianCalendar.getInstance(mTimeZone);
        long mills = ETime.getMillisFromJulianDay(mFirstJulianDayInTargetMonth, mTimeZone, mFirstDayOfWeek);
        time.setTimeInMillis(mills);//time.setJulianDay(mFirstJulianDayInTargetMonth);
        mTargetMonthFirstDayMills = time.getTimeInMillis();
        
        mMonthIndicatorText = buildMonth(mTargetMonthFirstDayMills, mTargetMonthFirstDayMills); //ETime.buildMonth(mTargetMonthFirstDayMills, mTimeZone);         
        
        mMaxMonthDay = time.getActualMaximum(Calendar.DAY_OF_MONTH);
    	mDayNumbers = new String[mMaxMonthDay];  
    	
        
        
        // target month�� first day�� firstdayofweek�� ���� week���� ���° �ε����� ��ġ�ϴ����� ����ؾ� �Ѵ�
        // �ٷ� diff ���� �ε���(ordering�� 0)�� �ش��Ѵ�
        if (time.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek) {
            int diff = time.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek;
            if (diff < 0) {
                diff += 7;
            }
            mIndexFirstDayOfMonth = diff;            
        }
        else {
        	mIndexFirstDayOfMonth = 0;
        }        
        
        for (int i=0; i<mMaxMonthDay; i++) {
        	int dayNumber = i + 1;
        	mDayNumbers[i] = String.valueOf(dayNumber);
        }
        
        
        Calendar temp = GregorianCalendar.getInstance(mTimeZone);
        temp.setFirstDayOfWeek(mFirstDayOfWeek);
        temp.setTimeInMillis(mTargetMonthFirstDayMills);
        
        mWeekNumbers = ETime.getWeekNumbersOfMonth(temp);
	}
	
	
	public void setPsudoNextMonthRegionContainer(LinearLayout pusdoLowerLisViewContainer) {		
		mPsudoNextMonthRegionContainer = pusdoLowerLisViewContainer;
	}	
	
	
	public void setAnimCompletionCallback(EventListViewEntranceAnimCompletionCallback callback) {
		mAnimCompletionCallback = callback;
	}
	
	
	
	public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        
        createEventExistenceCircle(unsortedEvents);
    }
	
	
	public void setEvents(List<ArrayList<Event>> sortedEvents) {    	
        mEvents = sortedEvents;
        if (sortedEvents == null) {
            return;
        }
        if (sortedEvents.size() != mMaxMonthDay) {
            if (Log.isLoggable("tag", Log.ERROR)) {
                Log.wtf("tag", "Events size must be same as days displayed: size="
                        + sortedEvents.size() + " days=" + mMaxMonthDay);
            }
            mEvents = null;
            return;
        }
    }
	
	
    public void createEventExistenceCircle(ArrayList<Event> unsortedEvents) {
    	if (unsortedEvents == null || getContext() == null) {
            // Stash the list of events for use when this view is ready, or
            // just clear it if a null set has been passed to this view
            mUnsortedEvents = unsortedEvents;            
            mEventExistence = null;
            return;
        } else {
            // clear the cached set of events since we're ready to build it now
            mUnsortedEvents = null;
        }       
        
    	int maxMonthDay = mEvents.size();
    	mEventExistence = new boolean[maxMonthDay];
    	for (int i=0; i<maxMonthDay; i++) {
    		ArrayList<Event> dayEvents = mEvents.get(i);
    		if (dayEvents.size() !=0)
    			mEventExistence[i] = true;
    		else
    			mEventExistence[i] = false;
    	}
    }
	
    
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;  
        
        mDayOfWeekTextWidth = mWidth / 7;
    }
	
	
	
	@Override
    protected void onDraw(Canvas canvas) {    	
		
		if (!mDrawScaleMonth) {
			drawMonth(canvas);
			
			if (!mStartScaleHeight) {				
				mStartScaleHeight = true;
				mDrawScaleMonth = true;
				
				post(mScaleMonthViewAnimatorLauchRunnable);
			}
		}
		else {		
			if (!mScaleAnimationEnd)
				drawScaleMonth(canvas);
			else {
				drawScaleMonth(canvas);				
				mAnimCompletionCallback.setListViewScaleHeight(mWillBeScaleDownMonthListViewHeight);				
				post(mAnimCompletionCallback);	
			}
		}
        		
    }	
	
	
	public void drawMonth(Canvas canvas) { 		
		mMonthIndicatorTextPaint.setTextSize(mMonthListViewDateTextSize);
        mMonthDateTextPaint.setTextSize(mMonthListViewDateTextSize);          
        
        float textX = computeTextXPosition(mIndexFirstDayOfMonth);
        float textY = mMonthListViewMonthIndicatorHeight - mMonthTextBottomPadding;
        canvas.drawText(mMonthIndicatorText, textX, textY, mMonthIndicatorTextPaint);        
         
        float normalWeekHeightSize = mMonthListViewNModeWeekHeight;
		float radius = (normalWeekHeightSize * 0.1f) / 2;			
		        
        int dayIndex = 0;
        float weekLineY = 0;
        for (int i=0; i<mWeekNumbers; i++) {
        	textY = mMonthListViewMonthIndicatorHeight + (mMonthListViewNModeWeekHeight * i) + mMonthListViewNModeDateTextTopPadding;        	
        	weekLineY = mMonthListViewMonthIndicatorHeight + (mMonthListViewNModeWeekHeight * i);
        	float cy = mMonthListViewMonthIndicatorHeight + (mMonthListViewNModeWeekHeight * i) + mMonthListViewNModeEventCircleTopPadding;
        	
        	if (i == 0) { // first week 
        		int firstJulianDayOfWeek = mFirstJulianDayInTargetMonth + dayIndex;        		
        		int numDays = mNumDays - mIndexFirstDayOfMonth;
        		// if) today�� 2015/06/04,
        		// todayIndex�� 3�� �ȴ� �̴� numDays[2015/06/04�� ���� ���� numDays�� 6��]���� index[index�� 0�������� ����]��
        		// �׷��Ƿ� todayIndex�� �������ؾ� �Ѵ�
            	int todayIndex = hasToday(firstJulianDayOfWeek, numDays);            	
            	
        		canvas.drawLine(mDayOfWeekTextWidth * mIndexFirstDayOfMonth, weekLineY, mWidth, weekLineY, mUpperLinePaint); 
        		int j = mIndexFirstDayOfMonth;
        		        		
        		for (; j<mNumCells; j++) {
        			textX = computeTextXPosition(j);
        			setDayOfMonthTextColor(j);
        			canvas.drawText(mDayNumbers[dayIndex], textX, textY, mMonthDateTextPaint);
        			
        			if ( (mEventExistence != null) &&(mEventExistence[dayIndex]) ) {
						float cx = textX;						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
        			
        			dayIndex++;
        		}
        		
        		if (todayIndex != -1) {
            		todayIndex = mIndexFirstDayOfMonth + todayIndex;
            		drawTodayRedCircle(canvas, todayIndex, mMonthListViewNModeWeekHeight, textY);
            	}
        	}
        	else if (i == (mWeekNumbers - 1)) { // last week        		
        		int end = mMaxMonthDay - dayIndex;
        		int numDays = end;
        		int firstJulianDayOfWeek = mFirstJulianDayInTargetMonth + dayIndex;
            	int todayIndex = hasToday(firstJulianDayOfWeek, numDays); 
            	
        		weekLineY = weekLineY + (int)mUpperLinePaint.getStrokeWidth();
        		canvas.drawLine(0, weekLineY, mDayOfWeekTextWidth * end, weekLineY, mUpperLinePaint); 
        		
        		for (int j=0; j<end; j++) {
        			textX = computeTextXPosition(j);
        			setDayOfMonthTextColor(j);
        			canvas.drawText(mDayNumbers[dayIndex], textX, textY, mMonthDateTextPaint);
        			
        			if ( (mEventExistence != null) &&(mEventExistence[dayIndex]) ) {
						float cx = textX;						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
        			
        			dayIndex++;
        		} 
        		
        		if (todayIndex != -1) {            		
        			drawTodayRedCircle(canvas, todayIndex, mMonthListViewNModeWeekHeight, textY);
            	}
        	}
        	else {         		
        		int firstJulianDayOfWeek = mFirstJulianDayInTargetMonth + dayIndex;
            	int todayIndex = hasToday(firstJulianDayOfWeek);
            	
        		weekLineY = weekLineY + (int)mUpperLinePaint.getStrokeWidth();
        		canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);     
        		for (int j=0; j<mNumCells; j++) {
        			textX = computeTextXPosition(j);
        			setDayOfMonthTextColor(j);
        			canvas.drawText(mDayNumbers[dayIndex], textX, textY, mMonthDateTextPaint);
        			
        			if ( (mEventExistence != null) &&(mEventExistence[dayIndex]) ) {
						float cx = textX;						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
        			
        			dayIndex++;
        		}
        		
        		if (todayIndex != -1) {            		
        			drawTodayRedCircle(canvas, todayIndex, mMonthListViewNModeWeekHeight, textY);
            	}
        	}
        }        
	}
	
	/*
	 * ����  drawMonth�� drawScaleMonth ���̿���
	 * drawMonth : dateTextTopPadding = (int) (mNormalWeekHeight * 0.4f)
	 * drawScaleMonth : dateTextTopPadding = (int) (mNormalWeekHeight * 0.45f)
	 * ���̷� ���� drawMonth���� drawScaleMonth�� �Ѿ �� date text�� �Ʒ��� �ⷷ�ŷȴٰ� �ö󰡴� ������ �߻���
	 * �ذ�å�� week height�� ��� ������ dateTextTopPadding ������ ���� ���� : 0.75��
	 */
	public void drawScaleMonth(Canvas canvas) {   
		
		int monthIndicatorTop = -mUpwardTopOffsetDelta;
        
		int dateTextTopPadding = mMonthListViewNModeDateTextTopPadding - mScaleDownDateTextTopPaddingDelta;
		int eventCircleTopPadding = mMonthListViewNModeEventCircleTopPadding - mScaleDownEventCircleTopPaddingDelta;              
        
        mMonthDateTextPaint.setTextSize(mMonthListViewDateTextSize); // ���⼭ ���� init�ܿ��� ��������       
                
        mMonthIndicatorTextPaint.setTextSize(mMonthListViewDateTextSize);        
        float textX = computeTextXPosition(mIndexFirstDayOfMonth);
        float textY = monthIndicatorTop + mMonthListViewMonthIndicatorHeight - mMonthTextBottomPadding;
        canvas.drawText(mMonthIndicatorText, textX, textY, mMonthIndicatorTextPaint);        
        
        float normalWeekHeightSize = mUpdatedMonthListViewAnimWeekHeight;
		float radius = (normalWeekHeightSize * 0.1f) / 2;			
		        
        int dayIndex = 0;
        float weekLineY = 0;
        for (int i=0; i<mWeekNumbers; i++) {
        	textY = monthIndicatorTop + mMonthListViewMonthIndicatorHeight + (mUpdatedMonthListViewAnimWeekHeight * i) + dateTextTopPadding;        	
        	weekLineY = monthIndicatorTop + mMonthListViewMonthIndicatorHeight + (mUpdatedMonthListViewAnimWeekHeight * i);
        	float cy = monthIndicatorTop + mMonthListViewMonthIndicatorHeight + (mUpdatedMonthListViewAnimWeekHeight * i) + eventCircleTopPadding;        	
        	
        	
        	if (i == 0) { // first week        		 
        		int firstJulianDayOfWeek = mFirstJulianDayInTargetMonth + dayIndex;        		
        		int numDays = mNumDays - mIndexFirstDayOfMonth;
        		// if) today�� 2015/06/04,
        		// todayIndex�� 3�� �ȴ� �̴� numDays[2015/06/04�� ���� ���� numDays�� 6��]���� index[index�� 0�������� ����]��
        		// �׷��Ƿ� todayIndex�� �������ؾ� �Ѵ�
            	int todayIndex = hasToday(firstJulianDayOfWeek, numDays);            	
            	
        		canvas.drawLine(mDayOfWeekTextWidth * mIndexFirstDayOfMonth, weekLineY, mWidth, weekLineY, mUpperLinePaint); 
        		int j = mIndexFirstDayOfMonth;
        		for (; j<mNumCells; j++) {
        			textX = computeTextXPosition(j);
        			setDayOfMonthTextColor(j);
        			canvas.drawText(mDayNumbers[dayIndex], textX, textY, mMonthDateTextPaint);
        			
        			if ( (mEventExistence != null) &&(mEventExistence[dayIndex]) ) {
						float cx = textX;						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
        			
        			dayIndex++;
        		}
        		
        		if (todayIndex != -1) {
            		todayIndex = mIndexFirstDayOfMonth + todayIndex;
            		drawTodayRedCircle(canvas, todayIndex, mUpdatedMonthListViewAnimWeekHeight, textY);
            	}
        	}
        	else if (i == (mWeekNumbers - 1)) { // last week///////////////////////////////////////////////////////////  
        		int end = mMaxMonthDay - dayIndex;
        		int numDays = end;
        		int firstJulianDayOfWeek = mFirstJulianDayInTargetMonth + dayIndex;
            	int todayIndex = hasToday(firstJulianDayOfWeek, numDays);            	
        		
        		weekLineY = weekLineY + (int)mUpperLinePaint.getStrokeWidth();
        		canvas.drawLine(0, weekLineY, mDayOfWeekTextWidth * end, weekLineY, mUpperLinePaint); 
        		
        		for (int j=0; j<end; j++) {
        			textX = computeTextXPosition(j);
        			setDayOfMonthTextColor(j);
        			canvas.drawText(mDayNumbers[dayIndex], textX, textY, mMonthDateTextPaint);
        			
        			if ( (mEventExistence != null) &&(mEventExistence[dayIndex]) ) {
						float cx = textX;						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
        			
        			dayIndex++;
        		} 
        		
        		if (todayIndex != -1) {            		
        			drawTodayRedCircle(canvas, todayIndex, mUpdatedMonthListViewAnimWeekHeight, textY);
            	}
        	}
        	else {         		
        		int firstJulianDayOfWeek = mFirstJulianDayInTargetMonth + dayIndex;
            	int todayIndex = hasToday(firstJulianDayOfWeek);
            	
        		weekLineY = weekLineY + (int)mUpperLinePaint.getStrokeWidth();
        		canvas.drawLine(0, weekLineY, mWidth, weekLineY, mUpperLinePaint);     
        		for (int j=0; j<mNumCells; j++) {
        			textX = computeTextXPosition(j);
        			setDayOfMonthTextColor(j);
        			canvas.drawText(mDayNumbers[dayIndex], textX, textY, mMonthDateTextPaint);
        			
        			if ( (mEventExistence != null) &&(mEventExistence[dayIndex]) ) {
						float cx = textX;						
						canvas.drawCircle(cx, cy, radius, mEventExisteneCirclePaint);
					}
        			
        			dayIndex++;
        		}
        		
        		if (todayIndex != -1) {            		
        			drawTodayRedCircle(canvas, todayIndex, mUpdatedMonthListViewAnimWeekHeight, textY);
            	}
        	}
        }        
	}
	
	
	Runnable mScaleMonthViewAnimatorLauchRunnable = new Runnable() {
        @Override
        public void run() {
        	mUpwardTopOffsetDelta = 0;
    		mScaleTotalUpwardOffsetFromTop = PsudoTargetMonthViewAtEventListEntrance.this.mMonthListViewMonthIndicatorHeight;    		
    		mScaleDownTotalNormalWeekSize = PsudoTargetMonthViewAtEventListEntrance.this.mMonthListViewNModeWeekHeight - 
    				(int)(PsudoTargetMonthViewAtEventListEntrance.this.mMonthListViewNModeWeekHeight * 0.75f);
    		
    		mWillBeScaleDownMonthListViewHeight = mWeekNumbers * mMonthListViewEModeWeekHeight;    		
    		
    		mScaleDownTotalDateTextTopPaddingSize = mMonthListViewNModeDateTextTopPadding - mMonthListViewEModeDateTextTopPadding;
    		mScaleDownTotalEventCircleTopPaddingSize = mMonthListViewNModeEventCircleTopPadding - mMonthListViewEModeEventCircleTopPadding;
    		
    		mScalingTotalValue = mTargetMonthHeightWithinMonthListViewAtNMode - mWillBeScaleDownMonthListViewHeight;
    		
    		mScaleValueAnimator = ValueAnimator.ofInt(mTargetMonthHeightWithinMonthListViewAtNMode, mWillBeScaleDownMonthListViewHeight);
    		
    		long duration = calculateDuration(mScalingTotalValue, mTargetMonthHeightWithinMonthListViewAtNMode, DEFAULT_VELOCITY);
    		mScaleValueAnimator.setInterpolator(new ScaleTimeInterpolator(mScaleValueAnimator, mScalingTotalValue));		
    		mScaleValueAnimator.setDuration(duration);
    		
    		if (mLaunchNextMonthRegionContainer)
    			setPusdoLowerLisViewAnim(duration);

    		mScaleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
    			@Override
    			public void onAnimationUpdate(ValueAnimator valueAnimator) {
    				//Update Height
    				// value�� 800 ~ 600���� ���̵ȴ�
    				int value = (Integer) valueAnimator.getAnimatedValue();
    				
    				float delta = value - mWillBeScaleDownMonthListViewHeight;    				
    				float ratio = 1 - (delta / mScalingTotalValue);
    				int alpha = (int) (255 * ratio);    								
    				
    				int normalWeekDownSizeDelta = (int)(mScaleDownTotalNormalWeekSize * ratio);
    				
    				mUpwardTopOffsetDelta = (int)(mScaleTotalUpwardOffsetFromTop * ratio);    										
    				mUpdatedMonthListViewAnimWeekHeight = PsudoTargetMonthViewAtEventListEntrance.this.mMonthListViewNModeWeekHeight - normalWeekDownSizeDelta;    
    				mScaleDownDateTextTopPaddingDelta = (int) (mScaleDownTotalDateTextTopPaddingSize * ratio);
    				mScaleDownEventCircleTopPaddingDelta = (int) (mScaleDownTotalEventCircleTopPaddingSize * ratio);
    				    				
    				// monthIndicatorTop�� 0 ~ -60���� �����ؾ� ��
    				int monthIndicatorTop = -PsudoTargetMonthViewAtEventListEntrance.this.mUpwardTopOffsetDelta;
    				int monthListViewscaledHeight = monthIndicatorTop + PsudoTargetMonthViewAtEventListEntrance.this.mMonthListViewMonthIndicatorHeight + 
    						(mWeekNumbers * PsudoTargetMonthViewAtEventListEntrance.this.mUpdatedMonthListViewAnimWeekHeight);
    							
    				ViewGroup.LayoutParams layoutParams = PsudoTargetMonthViewAtEventListEntrance.this.getLayoutParams();
    				layoutParams.height = monthListViewscaledHeight;
    				PsudoTargetMonthViewAtEventListEntrance.this.setLayoutParams(layoutParams);
    				
    				int bgColor = Color.argb(alpha, mEEVMBGColor_RED, mEEVMBGColor_GREEN, mEEVMBGColor_BLUE);////////////////////////////////////////////
    				PsudoTargetMonthViewAtEventListEntrance.this.setBackgroundColor(bgColor);
    				
    				// event listview�� height�� �ٲ��൵ event listview�� height scale animation�� �����ϴ� ��ó�� ���̰� �ȴ�
    				// �׷��Ƿ� ������ event listview scale animation�� �ʿ����
    				int eventListViewScaleHeight = mMonthListViewNModeHeight - monthListViewscaledHeight;
    				//RelativeLayout.LayoutParams eventListViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, eventListViewScaleHeight);    				 		    	
    				//eventListViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);  				
    				//mMonthByWeekFragment.mEventsListView.setLayoutParams(eventListViewParams);       				
    				
    				RelativeLayout.LayoutParams eventListViewParams = (RelativeLayout.LayoutParams) mMonthFragment.mEventsListView.getLayoutParams();
    				eventListViewParams.height = eventListViewScaleHeight;    	
    				mMonthFragment.mEventsListView.setLayoutParams(eventListViewParams);
    			}
    		});
    		
    		mScaleValueAnimator.addListener(new Animator.AnimatorListener() {
    			@Override
    			public void onAnimationStart(Animator animator) {	
    				if (!mLaunchNextMonthRegionContainer) {    					
	    				PsudoTargetMonthViewAtEventListEntrance.this.setBackgroundColor(mEEVMTransparentBGColor);
	    				mMonthFragment.mEventsListView.setVisibility(View.VISIBLE);			
    				}
    			}
    			
    			@Override
    			public void onAnimationEnd(Animator animator) {    				
    				mScaleAnimationEnd = true;    				
    				mUpwardTopOffsetDelta = mScaleTotalUpwardOffsetFromTop;    						
    				PsudoTargetMonthViewAtEventListEntrance.this.mUpdatedMonthListViewAnimWeekHeight = (int)(PsudoTargetMonthViewAtEventListEntrance.this.mMonthListViewNModeWeekHeight * 0.75f);   
    				PsudoTargetMonthViewAtEventListEntrance.this.setBackgroundColor(mEEVMBGColor);    				
    			}    			

    			@Override
    			public void onAnimationCancel(Animator animator) {
    			}

    			@Override
    			public void onAnimationRepeat(Animator animator) {
    			}
    		});   		
    		
    		if (mLaunchNextMonthRegionContainer)
    			mPsudoNextMonthRegionContainer.startAnimation(mPsudoNextMonthRegionContainerTranslateAnimation);
    		else
    			mScaleValueAnimator.start();	// ���� ���ƿ��� onAnimationStart�� ȣ��ȴ�
        	
        }
	};	
	
	public void setPusdoLowerLisViewAnim(long duration) {		
		float lowActionBarTop = mMonthFragment.mLowerActionBar.getTop();
		float lowerListViewTop = mPsudoNextMonthRegionContainer.getTop();
		float lowerListViewHeight = mPsudoNextMonthRegionContainer.getHeight();		
		float toYValue = (lowActionBarTop - lowerListViewTop) / lowerListViewHeight;		
		
		mPsudoNextMonthRegionContainerTranslateAnimation = new TranslateAnimation(
				Animation.ABSOLUTE, 0, //fromXValue 
				Animation.ABSOLUTE, 0,   //toXValue
                Animation.RELATIVE_TO_SELF, 0, /////////////////////////////////////////////////////////////////
                Animation.RELATIVE_TO_SELF, toYValue);
		
		long durationTime = (long) (duration * 0.30f);		
		//long durationTime = (long) (duration);	// FOR Test////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		mPsudoNextMonthRegionContainerTranslateAnimation.setDuration(durationTime);		
		mPsudoNextMonthRegionContainerTranslateAnimation.setAnimationListener(PusdoLowerListViewTranslateAnimationListener);
	}
	
	AnimationListener PusdoLowerListViewTranslateAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
			mScaleValueAnimator.start();			
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			// psudo lower list view�� ���� ���[�ش޿��� 6�ָ� ������ �ִ� ���!!!]/////////////////////////////////////////////////////////////////////////////////////////////////////////
			PsudoTargetMonthViewAtEventListEntrance.this.setBackgroundColor(mEEVMTransparentBGColor);
			mMonthFragment.mEventsListView.setVisibility(View.VISIBLE);
			mPsudoNextMonthRegionContainer.setVisibility(View.GONE);	
			mPsudoNextMonthRegionContainer.removeAllViews();			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};		
	
	public int getFirstJulianDayInTargetMonth() {
        return mFirstJulianDayInTargetMonth;
    }
	
	public void setToday() {
		 mToday = GregorianCalendar.getInstance(mTimeZone);
		 mToday.setTimeInMillis(System.currentTimeMillis());
	     
	     mJulianToday = ETime.getJulianDay(mToday.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
	}
	
	
	public int hasToday(int firstJulianDay) {
		
		int todayIndex = -1;
        if (mJulianToday >= firstJulianDay && mJulianToday < firstJulianDay + mNumDays) {            
        	todayIndex = mJulianToday - firstJulianDay;
        } 
        
        return todayIndex;
    }
	
	public int hasToday(int firstJulianDay, int numDays) {
		
		int todayIndex = -1;
        if (mJulianToday >= firstJulianDay && mJulianToday < firstJulianDay + numDays) {            
        	todayIndex = mJulianToday - firstJulianDay;
        } 
        
        return todayIndex;
    }
	
	
	private void drawTodayRedCircle(Canvas canvas, int todayIndex, int weekHeight, float dateTextY) {
		
        int dateNum = mToday.get(Calendar.DAY_OF_MONTH);
        
        makeSelectedCircleDrawableDimens(weekHeight, (int) dateTextY);
                  
    	mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape(), mMonthDateTextPaint.getTextSize());
		// Oval�� drawing�ϴ� �θ��� Paint �����̴�
        mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
        mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
        mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);			
		
		// x�� text�� x�� �� ���̴�
		int x = computeTextXPosition(todayIndex);
		int left = x - mSelectedCircleDrawableRadius;	    			
		int top = mSelectedCircleDrawableCenterY - mSelectedCircleDrawableRadius;
		int right = left + mSelectedCircleDrawableSize;
		int bottom = top + mSelectedCircleDrawableSize;
		
		mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);	
		mSelectedDateTodayDrawable.setDrawTextPosition(x, dateTextY);
		mSelectedDateTodayDrawable.setDayOfMonth(dateNum);
		mSelectedDateTodayDrawable.draw(canvas);   		     
    	
    }
	
	
	
	
    public void makeSelectedCircleDrawableDimens(int weekHeight, int dateTextY) {
		float radius = (weekHeight / 2) / 2;
		mSelectedCircleDrawableRadius = (int)radius;
		mSelectedCircleDrawableSize = (int)(mSelectedCircleDrawableRadius * 2);
		
    	float baseLineY = dateTextY;
    	
    	float textAscent = mMonthDateTextPaint.ascent();
    	float textDescent = mMonthDateTextPaint.descent();
    	
    	float textTopY = baseLineY + textAscent; 	
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = (textTopY + (textHeight / 2));
    	
    	mSelectedCircleDrawableCenterY = (int) textCenterY;   	
	}    
	
	
    private int computeTextXPosition(int day) {
    	int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
        return x;
    }
	
	
	public void setDayOfMonthTextColor(int dayIndex) {
		int color;
		
		final int column = dayIndex % 7;
        if (Utils.isSaturday(column, mFirstDayOfWeek)) {
            color = mWeek_saturdayColor;
        } else if (Utils.isSunday(column, mFirstDayOfWeek)) {
            color = mWeek_sundayColor;
        }
        else {
        	color = mMonthDateTextColor;
        }
        
        
		mMonthDateTextPaint.setColor(color);
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
	
	
	private static class ScaleTimeInterpolator implements TimeInterpolator {
		ValueAnimator mAnimator;
    	float mAnimationDistance;
    	
        public ScaleTimeInterpolator(ValueAnimator animator, float animationDistance) {
        	mAnimator = animator;
        	mAnimationDistance = animationDistance;
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
        
        private void cancelAnimation() {        	
        	mAnimator.setDuration(0);        	     	
        }
    }
	
    private static long calculateDuration(float delta, float width, float velocity) {
        
    	/*
         * Here we compute a "distance" that will be used in the computation of
         * the overall snap duration. This is a function of the actual distance
         * that needs to be traveled; we keep this value close to half screen
         * size in order to reduce the variance in snap duration as a function
         * of the distance the page needs to travel.
         */
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        velocity = Math.max(MINIMUM_VELOCITY, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }    
    
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
    
    
}
