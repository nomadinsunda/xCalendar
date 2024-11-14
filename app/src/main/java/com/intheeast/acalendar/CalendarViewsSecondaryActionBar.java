package com.intheeast.acalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.day.DayFragment;
import com.intheeast.day.DayView;
import com.intheeast.day.DayFragment.OnStartMonthDayHeaderAnimationListener;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
//import com.intheeast.ecalendar.DayView.CalendarGestureListener;

import com.intheeast.etc.ETime;
import com.intheeast.etc.SelectedDateOvalDrawable;
//import com.intheeast.event.EditEventView;






import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
//import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Handler;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*
 
 dayview 그리고 lower actionbar의 today 선택시 날짜 싱크임...
 1.month day header에서 날짜 변경이 발생하는 경우들
   1) single Tap      : day fragment에 event type은 GO_TO extra는 EXTRA_GOTO_DATE | EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR
   2) after scrolling : day fragment에 event type은 GO_TO extra는 EXTRA_GOTO_DATE | EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR 
   3) fling           : day fragment에 event type은 GO_TO extra는 EXTRA_GOTO_DATE | EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR, updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone에서 selected day 변경
   
   **또한 불필요하게 CalendarController를 사용해서 CalendarViewsSecondaryActionBar와 DayFragment와 통신하고 있다는 점.
     :물론 CalendarViewsSecondaryActionBar를 DAY / MONTH / YEAR에서 사용하다 보니...근데 이게 빙신같은 생각임...Month day header는 day fragment에서만 사용하고 잇음...씨바...
   **sendGoToEvent/processSingleTapUp 에서 CalendarController.sendEvent를 호출함으로써 CalendarController의 mTime을 설정하고 있음
 2.day view에서 날짜 변경이 발생하는 경우는
   -오로지 day view의 switchViews를 통해...
 3.lower actionbar에서 today를 선택하였을 경우
   -day fragment에서 goTo를 통해...    
   
 ***month day header와 day view간의 애니메이션 시작점을 맞추자...이건 CalendarController를 사용하지 말고 하자...:interface를 활용해 보자
 
 **수정할 작업들
   -1.month day header에서 single Tap 작업 내용을 변경하자 : 완료
   -2.animation 시작점을 맞추자...
      :누가 kick off를 할것인가 문제임->day fragment로 결정하자!!!
 */
public class CalendarViewsSecondaryActionBar extends LinearLayout /*implements 
	DayFragment.OnStartMonthDayHeaderAnimationListener*/ {
	private static String TAG = "CalendarViewsSecondaryActionBar";
    private static boolean INFO = true;
    
	public static int DAY_HEADER_HEIGHT = 45;
    //private static int MULTI_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;      
    public static int MONTHDAY_HEADER_HEIGHT = 45;
    public static int DATEINDICATOR_HEADER_HEIGHT = 20;
    
    public static final int PREVIOUS_WEEK_SIDE_VIEW = 1;
    public static final int VISIBLE_WEEK_VIEW = 2;
    public static final int NEXT_WEEK_SIDE_VIEW = 4;
    
    public static final float MONTHDAY_HEADER_HEIGHT_RATIO = 0.6f;
    public static final float DATEINDICATOR_HEADER_HEIGHT_RATIO = 0.4f;
        
    public enum ViewState {
		IDLE,
		GO_TO_DATE_ANIMATION,
		TRANSLATE_IN_PLACE_ANIMATION,
		TRANSLATE_UPDATE_VISIBLE_WEEK_ANIMATION,		
        Finished
	}
    
    private ViewState mViewState;
    /**
     * The initial state of the touch mode when we enter this view.
     */
    private static final int TOUCH_MODE_INITIAL_STATE_MONTHDAYHEADER_LAYOUT = 0;

    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN_MONTHDAYHEADER_LAYOUT = 1;

    /**
     * Indicates the touch gesture is a vertical scroll
     */
    //private static final int TOUCH_MODE_VSCROLL_MONTHDAYHEADER_LAYOUT = 0x20;

    /**
     * Indicates the touch gesture is a horizontal scroll
     */
    //private static final int TOUCH_MODE_HSCROLL_MONTHDAYHEADER_LAYOUT = 0x40;
    
    private static final float GO_TO_DATE_THRESHOLD_MONTHDAYHEADER_VIEW = 0.4f;
        
    public static final int SUPPORTED_DURATION = 1;    
    private static final int USE_DEFAULT_ANIMATION_VELOCITY = 2;
    
    public static final int MINIMUM_ANIMATION_VELOCITY = 3300;    
    //private static final int MINIMUM_SNAP_VELOCITY = 500; // for test    
    public static final int DEFAULT_ANIMATION_VELOCITY = MINIMUM_ANIMATION_VELOCITY;
    
    public static final float FlING_THRESHOLD_VELOCITY = MINIMUM_ANIMATION_VELOCITY * 0.3f;
    
    public static final int ANIM_DEFAULT_DURATION = 300;
    
    Context mContext;
	DayView mParentView;
	
	public LinearLayout mDayHeaderLayout;
    public DayHeaderView mDayHeaderView;
    RelativeLayout mMonthDayHeaderLayout;
    ArrayList<CircularMonthDayHeaderView> mCircularMonthDayHeaderViewList = new ArrayList<CircularMonthDayHeaderView>();    
    TextView mDateIndicatorTextView;
    
	private CalendarController mController;
	Resources mResources;
	private GestureDetector mGestureDetectorMonthDayHeaderLayout;	
	
	public TimeZone mTimeZone;
	//static Time mSelectedDay = new Time();	
	public Calendar mSelectedDay;
	
	public SelectedDateOvalDrawable mSelectedDateDrawable;
	public SelectedDateOvalDrawable mSelectedDateTodayDrawable;
	public SelectedDateOvalDrawable mNewSelectedDateDrawable;
	public SelectedDateOvalDrawable mNewSelectedDateTodayDrawable;
	public int mSelectedCircleDrawableRadius;
	public int mSelectedCircleDrawableCenterY;
	public int mSelectedCircleDrawableSize;
	
	public int mFirstDayOfWeek;
	public int mPreviousDayOfFirstDayOfWeek;
	public int mLastDayOfWeek;
	public int mNextDayOfLastDayWeek;
	public int mTodayJulianDay;    
    
	public int mBackgroundColor;
    public int mWeek_saturdayColor;
    public int mWeek_sundayColor;
    //private static int mCalendarDateBannerTextColor;
    
    
    float mScale = 0;
    public int mDateTextTopPaddingOfMonthDayHeader;
    public int TEXT_SIZE_MONTH_NUMBER;
	
		
    public int mDayOfWeekTextColor;
    public int mMonthDayTextColor;
    
    private Formatter mFormatter;
    private StringBuilder mStringBuilder;
	private Handler mMidnightHandler = null; // Used to run a time update every midnight
	
	public interface OnStartDayViewSnapAnimationListener {
    	void startDayViewSnapAnimation();
    }
	
	public void setOnStartDayViewSnapAnimationListener(OnStartDayViewSnapAnimationListener l) {
		mOnStartDayViewSnapAnimationListener = l;
	}
	
	public OnStartDayViewSnapAnimationListener mOnStartDayViewSnapAnimationListener;
	
	
	// Used to define the look of the menu button according to the current view:
    // Day view: show day of the week + full date underneath
    // Week view: show the month + year
    // Month view: show the month + year
    // Agenda view: show day of the week + full date underneath
    
	// Updates time specific variables (time-zone, today's Julian day).
	// 사실상 타임존이 변경된다고 호출되지 않는 구조로 생각하고 있다
	// 단지, 최초 처음 타임 존 쿼리 결과(쿼리 시간이 좀 걸림)를 통지 받는 형식임!
	// 그러므로 실제 캘린더앱 구동 도중 타임존이 변경되었을 때 수행하는 런어블이 캘린더 앱에 구현 되어있는지 없는지를 찾아서 있다면 적용해야 함!!!
	// : 그리고 앱 구동중에 타임존이 변경될 수 있는 상황은 어떤 건지 알아 봐야 함!!!
	// 그리고 mMidnightHandler의 런어블로도 사용되고 있음을 잊지 말아야 한다!!!
	int mMonthDayHeaderLayoutWidth = 0;
	float mMonthDayHeaderViewSwitchingDelta;
	float mMonthDayHeaderViewAnimationDistance;
	OnLayoutChangeListener mMonthDayHeaderLayoutChangeListener = new OnLayoutChangeListener() {

		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			if (mMonthDayHeaderLayoutWidth == 0) {
				mMonthDayHeaderLayoutWidth = right - left;
				
				float xOffSet = mMonthDayHeaderLayoutWidth / 2;
	    		mMonthDayHeaderViewSwitchingDelta = mMonthDayHeaderLayoutWidth - xOffSet;
	    		mMonthDayHeaderViewAnimationDistance = mMonthDayHeaderLayoutWidth - xOffSet;
			}			
		}
		
	};
		
    private final Runnable mMidnightTimeUpdater = new Runnable() {
        @Override
        public void run() {
        	refreshToday(mContext);
        }
    };    
    
    public void refreshToday(Context context) {         
        long now = System.currentTimeMillis();
        long gmtoff = mTimeZone.getRawOffset() / 1000;
        mTodayJulianDay = ETime.getJulianDay(now, mTimeZone, mFirstDayOfWeek);
        
        setMidnightHandler();         
    }    
    
    // Sets a thread to run 1 second after midnight and update the current date
    // This is used to display correctly the date of yesterday/today/tomorrow
    private void setMidnightHandler() {
        mMidnightHandler.removeCallbacks(mMidnightTimeUpdater);
        // Set the time updater to run at 1 second after midnight
        long now = System.currentTimeMillis();
        Calendar cal = GregorianCalendar.getInstance(mTimeZone);
        cal.setFirstDayOfWeek(mFirstDayOfWeek);
        cal.setTimeInMillis(now);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        
        long runInMillis = (24 * 3600 - hour * 3600 - minute * 60 -
                second + 1) * 1000;
        mMidnightHandler.postDelayed(mMidnightTimeUpdater, runInMillis);
    }
    
    
	public CalendarViewsSecondaryActionBar(Context context) {
		super(context);	
		mContext = context;			
	}
	
	public CalendarViewsSecondaryActionBar(Context context, AttributeSet attrs) {
		super(context, attrs);		
		mContext = context;			
	}
	
	public CalendarViewsSecondaryActionBar(Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle);		
		mContext = context;			
	}
    
	boolean mGoToBacked;
    boolean mInitedActionBarLayout = false;
    public void init(long millis, TimeZone timezone, int firstOfWeek) {	
    	mResources = mContext.getResources();
    	
    	mUnderLinePaint = new Paint();
    	mUnderLinePaint = new Paint();
    	mUnderLinePaint.setAntiAlias(true);		
    	mUnderLinePaint.setColor(mResources.getColor(R.color.eventViewItemUnderLineColor));
    	mUnderLinePaint.setStyle(Style.STROKE);		
    	mUnderLineStrokeWidth = mResources.getDimension(R.dimen.eventItemLayoutUnderLineHeight);
		mUnderLinePaint.setStrokeWidth(mUnderLineStrokeWidth); 	
		
		mGoToBacked = false;
		
		mSelectedDay = GregorianCalendar.getInstance(timezone);
		mSelectedDay.setFirstDayOfWeek(firstOfWeek); 
		mSelectedDay.setTimeInMillis(millis);		 
		mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDay.set(Calendar.MINUTE, 0);
		mSelectedDay.set(Calendar.SECOND, 0);
		mSelectedDay.set(Calendar.MILLISECOND, 0);
        mTimeZone = timezone;

        
        long now = System.currentTimeMillis();        
        long gmtoff = mTimeZone.getRawOffset() / 1000;
        mTodayJulianDay = ETime.getJulianDay(now, mTimeZone, mFirstDayOfWeek);
        
		mMidnightHandler = new Handler();
		setMidnightHandler();
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		
        
		//MULTI_DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.day_header_height); //16dip
    	DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.day_header_height);/*MULTI_DAY_HEADER_HEIGHT;*/    
    	
    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
    	if (mFirstDayOfWeek == Calendar.SATURDAY) {
    		mLastDayOfWeek = Calendar.FRIDAY;
    		mPreviousDayOfFirstDayOfWeek = Calendar.FRIDAY;
    		mNextDayOfLastDayWeek = Calendar.SATURDAY;
    	}
    	else if (mFirstDayOfWeek == Calendar.MONDAY) {
    		mLastDayOfWeek = Calendar.SUNDAY;
    		mPreviousDayOfFirstDayOfWeek = Calendar.SUNDAY;
    		mNextDayOfLastDayWeek = Calendar.MONDAY;
    	}
    	else if (mFirstDayOfWeek == Calendar.SUNDAY) {// Time.SUNDAY
    		mLastDayOfWeek = Calendar.SATURDAY;
    		mPreviousDayOfFirstDayOfWeek = Calendar.SATURDAY;
    		mNextDayOfLastDayWeek = Calendar.SUNDAY;
    	}
    	else {
    		// Ooops!!!
    		// what the hell???
    		// 예외를 발생시켜야 한다
    	}
    	
    	mBackgroundColor = mResources.getColor(R.color.manageevent_actionbar_background);
        mWeek_saturdayColor = mResources.getColor(R.color.week_saturday);
        mWeek_sundayColor = mResources.getColor(R.color.week_sunday);
        
        mDayOfWeekTextColor = mResources.getColor(R.color.secondaryActionBarDayOfWeekTextColor);         
        
        mDayHeaderLayout = (LinearLayout)findViewById(R.id.dayofweek_header);
    	mDayHeaderView = new DayHeaderView(mContext, this, mSelectedDay);
    	mDayHeaderLayout.addView(mDayHeaderView); 
    	
    	mInitedActionBarLayout = true;
    	
    	mViewState = ViewState.IDLE;
	}
    
    Paint mUnderLinePaint;
    float mUnderLineStrokeWidth;
    float mStartXOfUnderLine = 0;
    float mStopXOfUnderLine;
    float mStartYOfUnderLine = 0;        
    float mStopYOfUnderLine = 0;     
    
    int mAnticipationMonthDayListViewHeight;
    int mAnticipationMonthDayListViewNormalWeekHeight;
    
    
	public void init(DayFragment fragment, long millis, TimeZone timezone, int firstOfWeek, CalendarController controller, 
			int anticipationMonthDayListViewHeight,
			int anticipationMonthDayListViewNormalWeekHeight) {		
		
		mResources = mContext.getResources();
		
		mUnderLinePaint = new Paint();
    	mUnderLinePaint = new Paint();
    	mUnderLinePaint.setAntiAlias(true);		
    	mUnderLinePaint.setColor(mResources.getColor(R.color.selectCalendarViewSeperatorOutLineColor));	
    	mUnderLinePaint.setStyle(Style.STROKE);		
    	mUnderLineStrokeWidth = mResources.getDimension(R.dimen.selectCalendarViewSeperatorOutLineHeight);
		mUnderLinePaint.setStrokeWidth(mUnderLineStrokeWidth); 	
		
		mGestureDetectorMonthDayHeaderLayout = new GestureDetector(mContext, new MonthDayHeaderViewGestureListener());
		
		mController = controller;
		mAnticipationMonthDayListViewHeight = anticipationMonthDayListViewHeight;
		mAnticipationMonthDayListViewNormalWeekHeight = anticipationMonthDayListViewNormalWeekHeight;
				
		mGoToBacked = false;
		
		mSelectedDay = GregorianCalendar.getInstance(timezone);
		mSelectedDay.setFirstDayOfWeek(firstOfWeek);
		mSelectedDay.setTimeInMillis(millis);
		mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDay.set(Calendar.MINUTE, 0);
		mSelectedDay.set(Calendar.SECOND, 0);
		mSelectedDay.set(Calendar.MILLISECOND, 0);
        mTimeZone = timezone;
                      
        long now = System.currentTimeMillis();        
        long gmtoff = mTimeZone.getRawOffset() / 1000;
        mTodayJulianDay = ETime.getJulianDay(now, mTimeZone, mFirstDayOfWeek);
        
		mMidnightHandler = new Handler();
		setMidnightHandler();
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		
    	DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.day_header_height);
    	MONTHDAY_HEADER_HEIGHT = (int) (mAnticipationMonthDayListViewNormalWeekHeight * MONTHDAY_HEADER_HEIGHT_RATIO);     	
    	DATEINDICATOR_HEADER_HEIGHT = (int) (mAnticipationMonthDayListViewNormalWeekHeight * DATEINDICATOR_HEADER_HEIGHT_RATIO);
    	
    	TEXT_SIZE_MONTH_NUMBER = (int) (mAnticipationMonthDayListViewHeight * 0.04f);
    	
    	mDateTextTopPaddingOfMonthDayHeader = (int) (mAnticipationMonthDayListViewNormalWeekHeight * 0.4f);  
    	
    	makeSelectedCircleDrawableDimens();
		makeSelectedCircleDrawable(); 	    	
    	
    	mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
    	if (mFirstDayOfWeek == Calendar.SATURDAY) {
    		mLastDayOfWeek = Calendar.FRIDAY;
    		mPreviousDayOfFirstDayOfWeek = Calendar.FRIDAY;
    		mNextDayOfLastDayWeek = Calendar.SATURDAY;
    	}
    	else if (mFirstDayOfWeek == Calendar.MONDAY) {
    		mLastDayOfWeek = Calendar.SUNDAY;
    		mPreviousDayOfFirstDayOfWeek = Calendar.SUNDAY;
    		mNextDayOfLastDayWeek = Calendar.MONDAY;
    	}
    	else if (mFirstDayOfWeek == Calendar.SUNDAY) {// Time.SUNDAY
    		mLastDayOfWeek = Calendar.SATURDAY;
    		mPreviousDayOfFirstDayOfWeek = Calendar.SATURDAY;
    		mNextDayOfLastDayWeek = Calendar.SUNDAY;
    	}
    	else {
    		// Ooops!!!
    		// what the hell???
    		// 예외를 발생시켜야 한다
    	}  		
		
    	
    	mBackgroundColor = mResources.getColor(R.color.manageevent_actionbar_background);
        mWeek_saturdayColor = mResources.getColor(R.color.week_saturday);
        mWeek_sundayColor = mResources.getColor(R.color.week_sunday);
        
        mDayOfWeekTextColor = mResources.getColor(R.color.secondaryActionBarDayOfWeekTextColor);         
        mMonthDayTextColor = mResources.getColor(R.color.secondaryActionBarMonthDayTextColor); 
        
        mDayHeaderLayout = (LinearLayout)findViewById(R.id.dayofweek_header);
    	mDayHeaderView = new DayHeaderView(mContext, this, mSelectedDay);
    	mDayHeaderLayout.addView(mDayHeaderView); 
    	
    	mMonthDayHeaderLayout = (RelativeLayout)findViewById(R.id.monthday_header);  
    	LinearLayout.LayoutParams monthDayHeaderLayoutParams = (LinearLayout.LayoutParams) mMonthDayHeaderLayout.getLayoutParams();
    	monthDayHeaderLayoutParams.height = MONTHDAY_HEADER_HEIGHT;
    	mMonthDayHeaderLayout.setLayoutParams(monthDayHeaderLayoutParams);		
    	mMonthDayHeaderLayout.addOnLayoutChangeListener(mMonthDayHeaderLayoutChangeListener);    	
		mMonthDayHeaderLayout.setOnTouchListener(mMonthDayHeaderLayoutTouchListener);
		
		
		// 아래 코드를 해 주지 않으면 이전에 생성된 mMonthDayHeaderLayout가 남아 있다?
		// 왜 그런지 이유를 알 수 없다...
		// :static으로 선언해서 그런지...아마도 그러하다...
		if (mMonthDayHeaderLayout.getChildCount() != 0)
			mMonthDayHeaderLayout.removeAllViews();
		
		// static으로 선언해서인지...
		// CalendarViewsSecondaryActionBar 객체가 제거가 된 후에도 살아? 있나보다		
		mCircularMonthDayHeaderViewList.clear();
		
		CircularMonthDayHeaderView visibleWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, VISIBLE_WEEK_VIEW);
		mMonthDayHeaderLayout.addView(visibleWeekView); 
		mCircularMonthDayHeaderViewList.add(visibleWeekView);
		
		CircularMonthDayHeaderView previousWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, PREVIOUS_WEEK_SIDE_VIEW);
		mMonthDayHeaderLayout.addView(previousWeekView); 
		mCircularMonthDayHeaderViewList.add(previousWeekView);
		
		CircularMonthDayHeaderView nextWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, NEXT_WEEK_SIDE_VIEW);
		mMonthDayHeaderLayout.addView(nextWeekView); 
		mCircularMonthDayHeaderViewList.add(nextWeekView);
		
    	mDateIndicatorTextView = (TextView)findViewById(R.id.dateindicator_header);
    	LinearLayout.LayoutParams dateIndicatorTextViewParams = (LinearLayout.LayoutParams) mDateIndicatorTextView.getLayoutParams();
    	dateIndicatorTextViewParams.height = DATEINDICATOR_HEADER_HEIGHT;
    	mDateIndicatorTextView.setLayoutParams(dateIndicatorTextViewParams);
    	mDateIndicatorTextView.setVisibility(View.VISIBLE);
    	mDateIndicatorTextView.setText(buildFullDate(mSelectedDay.getTimeInMillis()));    	
    	
    	
    	OnLayoutChangeListener listener = new OnLayoutChangeListener() {

			@Override
			public void onLayoutChange(View v, int left, int top, int right,
					int bottom, int oldLeft, int oldTop, int oldRight,
					int oldBottom) {
				// TODO Auto-generated method stub
								
				float testY = mDayHeaderLayout.getY(); // 0
				int height = mDayHeaderLayout.getHeight(); // 48
				
				testY = mMonthDayHeaderLayout.getY(); // 48
				height = mMonthDayHeaderLayout.getHeight(); // 144
				
				testY = mDateIndicatorTextView.getY(); // 192
				height = mDateIndicatorTextView.getHeight(); // 96
				
				int test = -1;
				test = 0;
				
			}
    		
    	};
		mDateIndicatorTextView.addOnLayoutChangeListener(listener);
    	mInitedActionBarLayout = true;
    	
    	fragment.setOnStartMonthDayHeaderAnimationListener(mOnStartMonthDayHeaderAnimationListener);
    	
    	mViewState = ViewState.IDLE;
    	
	}
		
	
	int mWidth;
	
	int mHeight;
	boolean mSetWillDraw = false;
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	
    	mWidth = w;
		mHeight = h;		
		
		mStopXOfUnderLine = mWidth;
		mStartYOfUnderLine = mHeight - mUnderLineStrokeWidth;        
	    mStopYOfUnderLine = mStartYOfUnderLine;	  
		
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false);
		}		
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	boolean mDontDrawUnderLine = false; 
	@Override
	protected void onDraw(Canvas canvas) {	             	
	        		
		if (!mDontDrawUnderLine)
			canvas.drawLine(mStartXOfUnderLine, mStartYOfUnderLine, mStopXOfUnderLine, mStopYOfUnderLine, mUnderLinePaint);	        
        
		super.onDraw(canvas);
	}
	
	public void setDontDrawUnderLine() {
		mDontDrawUnderLine = true;
		
		invalidate();
	}
	
	
	public int getMonthDayIndicatorHeight() {
		return MONTHDAY_HEADER_HEIGHT;
	}
	
	public int getDateIndicatorHeight() {
		return DATEINDICATOR_HEADER_HEIGHT;
	}
	
	public Calendar getMonthDayHeaderViewSelectedDayTime() {		
		CircularMonthDayHeaderView visibleHeader = mCircularMonthDayHeaderViewList.get(0);
		return visibleHeader.getSelectedDayTime();
	}
	
	public int getMonthDayHeaderViewFirstJulianDay() {
		CircularMonthDayHeaderView visibleHeader = mCircularMonthDayHeaderViewList.get(0);
		return visibleHeader.getFirstJulianDay();
	}
	
	public int getMonthDayHeaderViewLastJulianDay() {
		CircularMonthDayHeaderView visibleHeader = mCircularMonthDayHeaderViewList.get(0);
		return visibleHeader.getLastJulianDay();
	}
	
	public void setMonthDayHeaderLayoutGoneStatus() {
		mMonthDayHeaderLayout.setVisibility(View.GONE);
	}
	
	public void setDateIndicatorTextViewGoneStatus() {
		mDateIndicatorTextView.setVisibility(View.GONE);
	}
	
	public void makeSelectedCircleDrawableDimens() {
		float radius = (mAnticipationMonthDayListViewNormalWeekHeight / 2) / 2;
		mSelectedCircleDrawableRadius = (int)radius;
		mSelectedCircleDrawableSize = (int)(mSelectedCircleDrawableRadius * 2);		
		
		float baseLineY = mDateTextTopPaddingOfMonthDayHeader;
    	
		Paint monthNumPaint = new Paint();
		monthNumPaint.setFakeBoldText(false);
		monthNumPaint.setAntiAlias(true);	
		
		monthNumPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);		
		monthNumPaint.setStyle(Style.FILL);        
		monthNumPaint.setTextAlign(Align.CENTER);
		monthNumPaint.setTypeface(Typeface.DEFAULT);
        
    	float textAscent = monthNumPaint.ascent();
    	float textDescent = monthNumPaint.descent();
    	
    	float textTopY = baseLineY + textAscent;
    	
    	float textHeight = Math.abs(textAscent) + textDescent;
    	float textCenterY = textTopY + (textHeight / 2);
    	
    	mSelectedCircleDrawableCenterY = (int) textCenterY;  	
		
	}
	
	public void makeSelectedCircleDrawable() {			
		mSelectedDateDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape());		
		mSelectedDateDrawable.getPaint().setAntiAlias(true);
		mSelectedDateDrawable.getPaint().setStyle(Style.FILL);
		mSelectedDateDrawable.getPaint().setColor(Color.BLACK);		
				
		mSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape());		
		mSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
		mSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
		mSelectedDateTodayDrawable.getPaint().setColor(Color.RED);			
		
		mNewSelectedDateDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape());
		mNewSelectedDateDrawable.getPaint().setAntiAlias(true);
		mNewSelectedDateDrawable.getPaint().setStyle(Style.FILL);
		mNewSelectedDateDrawable.getPaint().setColor(Color.BLACK);		
		
		mNewSelectedDateTodayDrawable = new SelectedDateOvalDrawable(mContext, new OvalShape());
		mNewSelectedDateTodayDrawable.getPaint().setAntiAlias(true);
		mNewSelectedDateTodayDrawable.getPaint().setStyle(Style.FILL);
		mNewSelectedDateTodayDrawable.getPaint().setColor(Color.RED);		
	}
	
	public void setInVisibleBar(int viewType) {
		mDayHeaderLayout.setVisibility(View.INVISIBLE);
		
		if (viewType == ViewType.DAY) {
			mMonthDayHeaderLayout.setVisibility(View.INVISIBLE);
			mDateIndicatorTextView.setVisibility(View.INVISIBLE);
		}
	}
	
	public void setVisibleBar(int viewType) {
		mDayHeaderLayout.setVisibility(View.VISIBLE);
		
		if (viewType == ViewType.DAY) {
			mMonthDayHeaderLayout.setVisibility(View.VISIBLE);
			mDateIndicatorTextView.setVisibility(View.VISIBLE);
		}
	}
	
	public void goToBacked(long millis) {
		mSelectedDay.setTimeInMillis(millis);
		mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDay.set(Calendar.MINUTE, 0);
		mSelectedDay.set(Calendar.SECOND, 0);
		mSelectedDay.set(Calendar.MILLISECOND, 0);
			
    	
    	int size = mCircularMonthDayHeaderViewList.size();
    	for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);    		
    		obj.setSelectedTime(millis);
    		obj.invalidate();    		
    	}
    	
    	mDateIndicatorTextView.setText(buildFullDate(mSelectedDay.getTimeInMillis()));  
    	
    	mGoToBacked = true;
	}
	
	public void yyyy() {
		mGoToBacked = false;
		//TranslateAnimation ta = makeSlideAnimation(2000, Animation.RELATIVE_TO_SELF, -0.5f, Animation.RELATIVE_TO_SELF, 0);    	
		//ta.setBackgroundColor(mBackgroundColor); // 그 용도가 아닌가 보다???ㅡㅡ;
    	//startAnimation(ta);
		
		TranslateAnimation ta = makeSlideAnimation(500, Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0);  
		mDateIndicatorTextView.startAnimation(ta);
	}
	
	public boolean xxxx() {
		return mGoToBacked;
	}
	
	boolean mStartingScrollMonthDayHeaderLayout = false;
	boolean mScrollingMonthDayHeaderLayout = false;
	// fling이 식별되면 scroll후 수행되는 animation을 막기 위한 목적이 큼
	// 즉, UP 이벤트 후 fling이 식별되면 fling animation을 lauch하고 바로 리턴하게 하기 위함
	boolean mOnFlingCalledMonthDayHeaderLayout = false;	
	int mInPlaceAnimationCompletion = 0;
	
	final int IN_PLACE_ANIMATION_COMPLETE = PREVIOUS_WEEK_SIDE_VIEW | VISIBLE_WEEK_VIEW | NEXT_WEEK_SIDE_VIEW;
	public void updateViewStateIfInPlaceAnimationDone(int token) {
		mInPlaceAnimationCompletion |= token;
		if (mInPlaceAnimationCompletion == IN_PLACE_ANIMATION_COMPLETE) {
			mInPlaceAnimationCompletion = 0;
			
			makeCircularMonthDayHeaderViewList(IN_PLACE_ANIMATION_COMPLETE);
			/*mViewState = ViewState.IDLE;
			mMonthDayHeaderLayout.setOnTouchListener(mMonthDayHeaderLayoutTouchListener);*/
		}
	}
	
	public void makeInPlaceAnimation() {
		int size = mCircularMonthDayHeaderViewList.size();				
		for (int i=0; i<size; i++) {
			CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
			obj.makeInPlaceAnimation();				
		}		
	}
	
	public void startInPlaceAnimation() {
		int size = mCircularMonthDayHeaderViewList.size();
		for (int i=0; i<size; i++) {
			CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);			
			obj.startInPlaceAnimation();					
		}		
	}
	
    int mNextWeekToVisibleWeekAnimation = 0;    
    public final static int TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE = 1;
    public final static int TRANSLATE_PREVIOUS_WEEK_ANIMATION_COMPLETE = 2;
    public final static int TRANSLATE_NEXT_WEEK_ANIMATION_COMPLETE = 4;    
    
    final static int NEXTWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE = TRANSLATE_PREVIOUS_WEEK_ANIMATION_COMPLETE | TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE;     
    final static int PREVIOUSWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE = TRANSLATE_NEXT_WEEK_ANIMATION_COMPLETE | TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE;
    public synchronized void updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone(int token) {
    	if (INFO) Log.i(TAG, "updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone");
    	
    	if (token == TRANSLATE_PREVIOUS_WEEK_ANIMATION_COMPLETE) {
    		if (INFO) Log.i(TAG, "TRANSLATE_PREVIOUS_WEEK_ANIMATION_COMPLETE");
    	}
    	else if (token == TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE) {
    		if (INFO) Log.i(TAG, "TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE");
    	}
    	else {
    		if (INFO) Log.i(TAG, "updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone:???");
    	}
    	
    	mNextWeekToVisibleWeekAnimation |= token;
    	if (mNextWeekToVisibleWeekAnimation == NEXTWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE) {
    		mNextWeekToVisibleWeekAnimation = 0;       		
    		
    		if (INFO) Log.i(TAG, "NEXTWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE");    		
        	
        	makeCircularMonthDayHeaderViewList(NEXTWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE);
        	/*
        	mMonthDayHeaderLayout.removeAllViews();
        	mCircularMonthDayHeaderViewList.clear();
        	
        	CircularMonthDayHeaderView visibleWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, VISIBLE_WEEK_VIEW);
    		mMonthDayHeaderLayout.addView(visibleWeekView); 
    		mCircularMonthDayHeaderViewList.add(visibleWeekView);
    		
    		CircularMonthDayHeaderView previousWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, PREVIOUS_WEEK_SIDE_VIEW);
    		mMonthDayHeaderLayout.addView(previousWeekView); 
    		mCircularMonthDayHeaderViewList.add(previousWeekView);
    		
    		CircularMonthDayHeaderView nextWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, NEXT_WEEK_SIDE_VIEW);
    		mMonthDayHeaderLayout.addView(nextWeekView); 
    		mCircularMonthDayHeaderViewList.add(nextWeekView);
        	
            mViewState = ViewState.IDLE;
            
            mMonthDayHeaderLayout.setOnTouchListener(mMonthDayHeaderLayoutTouchListener);
            */
    	}    	
    }    
    
    public synchronized void makeCircularMonthDayHeaderViewList(int update) {
    	
    	if (update == NEXTWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE) {
    		//updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone  		
    		
    		CircularMonthDayHeaderView newVisible = null;
    		CircularMonthDayHeaderView newPrevious = null;
    		CircularMonthDayHeaderView oldPrevious = null;
    		
    		// next -> visible
    		// visible -> previous
    		// make new next!!!
    		int size = mCircularMonthDayHeaderViewList.size();
        	for (int i=0; i<size; i++) {
        		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
        		if (obj.mWhichView == NEXT_WEEK_SIDE_VIEW) {
        			mSelectedDay.setTimeInMillis(obj.mSelectedDayOfMonthDayHeader.getTimeInMillis());
        			mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
        			mSelectedDay.set(Calendar.MINUTE, 0);
        			mSelectedDay.set(Calendar.SECOND, 0);
        			mSelectedDay.set(Calendar.MILLISECOND, 0); 
        				
        			newVisible = obj;        			
        			
        			//obj.reset(mSelectedDay, VISIBLE_WEEK_VIEW);            		
        		}     
        		else if (obj.mWhichView == VISIBLE_WEEK_VIEW) {        			
        			newPrevious = obj;        			
        			//obj.reset(mSelectedDay, PREVIOUS_WEEK_SIDE_VIEW);
        		}     
        		else {
        			oldPrevious = obj;
        		}
        		
        	}        	
        	
        	// obj.reset를 여기서 해야 한다
        	// 왜냐하면, mSelectedDay를 먼저 설정해야 하기 때문이다...
        	// ...
        	
        	
        	
        	mMonthDayHeaderLayout.removeView(oldPrevious);        	
        	mCircularMonthDayHeaderViewList.clear();    		
        	
        	
    		mCircularMonthDayHeaderViewList.add(newVisible);     		
    		mCircularMonthDayHeaderViewList.add(newPrevious);    		
    		
    		CircularMonthDayHeaderView newNext = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, NEXT_WEEK_SIDE_VIEW);
    		mMonthDayHeaderLayout.addView(newNext); 
    		mCircularMonthDayHeaderViewList.add(newNext);
        	
    		
    	}
    	else if (update == PREVIOUSWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE) {
    		//updateSelectedTimeIfPreviousWeekToVisibleWeekAnimationDone
    		CircularMonthDayHeaderView newVisible = null;
    		CircularMonthDayHeaderView newNext = null;
    		CircularMonthDayHeaderView oldNext = null;
    		
    		// previous -> visible
    		// visible -> next
    		// make new previous!!!
    		int size = mCircularMonthDayHeaderViewList.size();
        	for (int i=0; i<size; i++) {
        		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
        		if (obj.mWhichView == PREVIOUS_WEEK_SIDE_VIEW) {
        			mSelectedDay.setTimeInMillis(obj.mSelectedDayOfMonthDayHeader.getTimeInMillis());
        			mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
        			mSelectedDay.set(Calendar.MINUTE, 0);
        			mSelectedDay.set(Calendar.SECOND, 0);
        			mSelectedDay.set(Calendar.MILLISECOND, 0);
            		
        			newVisible = obj;        			
        			
        			obj.reset(mSelectedDay, VISIBLE_WEEK_VIEW);   
        		}
        		else if (obj.mWhichView == VISIBLE_WEEK_VIEW) {
        			
        			newNext = obj;        			
        			
        			obj.reset(mSelectedDay, NEXT_WEEK_SIDE_VIEW);   
        		}
        		else {
        			oldNext = obj;
        		}
        	}
        	
        	mMonthDayHeaderLayout.removeView(oldNext);        	
        	mCircularMonthDayHeaderViewList.clear();          	
        	
    		mCircularMonthDayHeaderViewList.add(newVisible);     		
    		
    		CircularMonthDayHeaderView newPrevious = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, PREVIOUS_WEEK_SIDE_VIEW);
    		mMonthDayHeaderLayout.addView(newPrevious); 
    		mCircularMonthDayHeaderViewList.add(newPrevious);
    		
    		mCircularMonthDayHeaderViewList.add(newNext);
    		
    		
    	}
    	else { // IN_PLACE_ANIMATION_COMPLETE
    		//updateViewStateIfInPlaceAnimationDone
    	}	
		
        mViewState = ViewState.IDLE;
        
        mMonthDayHeaderLayout.setOnTouchListener(mMonthDayHeaderLayoutTouchListener);
    }
    
	public CircularMonthDayHeaderView makeNextWeekToVisibleWeek(int whatVelocity, float velocityOrDuration) {
		
		CircularMonthDayHeaderView willBeVisibleWeek = null;
		// *next week가 visible week로 이동해야 함
		//   :글로벌  base date이 변경되어야 함
		//   :next week의 mWhichView = VISIBLE_WEEK_VIEW로 수정
		//   :next week의 mCurrentLeftX & obj.mKickOffLeftX가 VISIBLE_WEEK_VIEW의 값으로 변경되어야 함
		// *visible week가 previous week로 이동해야 함
		//   :visible week의 mWhichView = PREVIOUS_WEEK_SIDE_VIEW로 수정
		//   :visible week의 mCurrentLeftX & obj.mKickOffLeftX가 PREVIOUS_WEEK_SIDE_VIEW의 값으로 변경되어야 함
		// *previous week가 next week로 이동해야 함
		//   :previous week의 mWhichView = NEXT_WEEK_SIDE_VIEW로 수정
		//   :local base date이 next week의 local base date + 7로 수정되어야 함
		//   :previous week의 mCurrentLeftX & obj.mKickOffLeftX가 NEXT_WEEK_SIDE_VIEW의 값으로 변경되어야 함
		int size = mCircularMonthDayHeaderViewList.size();	
		for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);                            		
    		switch(obj.mWhichView) {
    		case NEXT_WEEK_SIDE_VIEW:// next week는  right incoming을 수행해야 한다
    			obj.makeIncomingFromRightAnimation(whatVelocity, velocityOrDuration);  
    			willBeVisibleWeek = obj;
    			break;
    		case VISIBLE_WEEK_VIEW:// main은 left outgoing 상태가 되어야 한다
    			obj.makeOutgoingToLeftAnimation(whatVelocity, velocityOrDuration);
    			break;
    		case PREVIOUS_WEEK_SIDE_VIEW:    			
    			break;                           			
    		}                           		
    	}
				
		return willBeVisibleWeek;
	}
	public void startNextWeekToVisibleWeek() {
		int size = mCircularMonthDayHeaderViewList.size();
		
		for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);                            		
    		switch(obj.mWhichView) {
    		case NEXT_WEEK_SIDE_VIEW:
    			obj.startIncomingFromRightAnimation();
    			break;
    		case VISIBLE_WEEK_VIEW:
    			obj.startOutgoingToLeftAnimation();
    			break;
    		case PREVIOUS_WEEK_SIDE_VIEW: // NEXT_WEEK_SIDE_VIEW가 되어야 한다    			
    			break;                           			
    		}                           		
    	}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	int mPreviousWeekToVisibleWeekAnimation = 0;
	public void updateSelectedTimeIfPreviousWeekToVisibleWeekAnimationDone(int token) {
		
		if (token == TRANSLATE_NEXT_WEEK_ANIMATION_COMPLETE) {
    		if (INFO) Log.i(TAG, "TRANSLATE_NEXT_WEEK_ANIMATION_COMPLETE");
    	}
    	else if (token == TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE) {
    		if (INFO) Log.i(TAG, "TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE");
    	}
    	else {
    		if (INFO) Log.i(TAG, "updateSelectedTimeIfPreviousWeekToVisibleWeekAnimationDone:???");
    	}
		
		mPreviousWeekToVisibleWeekAnimation |= token;
    	if (mPreviousWeekToVisibleWeekAnimation == PREVIOUSWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE) {
    		
    		mPreviousWeekToVisibleWeekAnimation = 0;   	 	
        	
        	
        	makeCircularMonthDayHeaderViewList(PREVIOUSWEEK_TO_VISIBLEWEEK_ANIMATION_COMPLETE);
        	/*
        	mMonthDayHeaderLayout.removeAllViews();
        	mCircularMonthDayHeaderViewList.clear();
        	
        	CircularMonthDayHeaderView visibleWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, VISIBLE_WEEK_VIEW);
    		mMonthDayHeaderLayout.addView(visibleWeekView); 
    		mCircularMonthDayHeaderViewList.add(visibleWeekView);
    		
    		CircularMonthDayHeaderView previousWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, PREVIOUS_WEEK_SIDE_VIEW);
    		mMonthDayHeaderLayout.addView(previousWeekView); 
    		mCircularMonthDayHeaderViewList.add(previousWeekView);
    		
    		CircularMonthDayHeaderView nextWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, NEXT_WEEK_SIDE_VIEW);
    		mMonthDayHeaderLayout.addView(nextWeekView); 
    		mCircularMonthDayHeaderViewList.add(nextWeekView);
    		
            mViewState = ViewState.IDLE;
            
            mMonthDayHeaderLayout.setOnTouchListener(mMonthDayHeaderLayoutTouchListener);
            */
    	}    	
    }
	
	public CircularMonthDayHeaderView makePreviousWeekToVisibleWeek(int whatVelocity, float velocityOrDuration) {
		int size = mCircularMonthDayHeaderViewList.size();		
		
		CircularMonthDayHeaderView willBeVisibleWeek = null;
		for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
    		switch(obj.mWhichView) {
    		case VISIBLE_WEEK_VIEW: // visible week는 right outgoing 상태가 되어야 한다
    			obj.makeOutgoingToRightAnimation(whatVelocity, velocityOrDuration);
    			break;
    		case PREVIOUS_WEEK_SIDE_VIEW: // prv week는 left incoming을 수행해야 한다
    			obj.makeIncomingFromLeftAnimation(whatVelocity, velocityOrDuration);    	
    			willBeVisibleWeek = obj;
    			break;
    		case NEXT_WEEK_SIDE_VIEW: // PREVIOUS_WEEK_SIDE_VIEW가 되어야 한다    			
    			break;
    		}
    	}
		
		return willBeVisibleWeek;
	}	
	public void startPreviousWeekToVisibleWeek() {
		int size = mCircularMonthDayHeaderViewList.size();
		for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
    		switch(obj.mWhichView) {
    		case VISIBLE_WEEK_VIEW:
    			obj.startOutgoingToRightAnimation();
    			break;
    		case PREVIOUS_WEEK_SIDE_VIEW:
    			obj.startIncomingFromLeftAnimation();
    			break;
    		case NEXT_WEEK_SIDE_VIEW:    			 			
    			break;
    		}
    	}
	}
	
	// 현재 상태를 정의하자!!!
	OnTouchListener mMonthDayHeaderLayoutTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();			
			if (mViewState == ViewState.IDLE) {				
				switch (action) {
	            case MotionEvent.ACTION_DOWN: 
	            	if (INFO) Log.i(TAG, "ACTION_DOWN");
	            	mStartingScrollMonthDayHeaderLayout = true;            	
	            	mGestureDetectorMonthDayHeaderLayout.onTouchEvent(event);
	            	return true;
	            	
	            case MotionEvent.ACTION_MOVE:     
	            	if (INFO) Log.i(TAG, "ACTION_MOVE");
	            		            	
	            	mGestureDetectorMonthDayHeaderLayout.onTouchEvent(event);
	                return true;
	
	            case MotionEvent.ACTION_UP:	            	
	            	if (INFO) Log.i(TAG, "ACTION_UP");
	            		            	
	            	mStartingScrollMonthDayHeaderLayout = false;
	            	
	            	if (INFO) Log.i(TAG, "ACTION_UP1");
	            	mGestureDetectorMonthDayHeaderLayout.onTouchEvent(event);  // onFling or onSingleTapUp ???          	
	            	if (INFO) Log.i(TAG, "ACTION_UP2");
	            	
	                if (mOnFlingCalledMonthDayHeaderLayout) {
	                	mOnFlingCalledMonthDayHeaderLayout = false;
	                    return true;
	                }
	
	                // If we were scrolling, then reset the selected hour so that it
	                // is visible.
	                if (mScrollingMonthDayHeaderLayout) {
	                	mScrollingMonthDayHeaderLayout = false; 
	                	
	                	if (INFO) Log.i(TAG, "ACTION_UP3");
	                	processAfterScrolling();	     
	                	
	                	if (INFO) Log.i(TAG, "ACTION_UP4");
	                }	                
	                return true;
	
	            default:                
	                return false;
				}
			}
			else {
				if (INFO) Log.i(TAG, "return true");
				return true;
			}
		}		
	};
	
	public void processAfterScrolling() {
		float currentXPos = 0;
    	int size = mCircularMonthDayHeaderViewList.size();
    	for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
    		if (obj.mWhichView == VISIBLE_WEEK_VIEW) {
    			currentXPos = obj.getPositionX();
    			break;
    		}
    	}
    	
    	if (currentXPos != 0) {    		
    		
    		float gotoThreshold = GO_TO_DATE_THRESHOLD_MONTHDAYHEADER_VIEW * mMonthDayHeaderLayoutWidth;
    		
    		if (currentXPos < 0) {
    			float gotoNextWeekThreshold = 0 - gotoThreshold;
    			
    			if (currentXPos <= gotoNextWeekThreshold) { // 둘 다 음수 값이다. 착각하지 마라
    				                                        // visible week가 왼졲으로 임계값을 초과하여 이동하였기 때문에
    				                                        // next week가 visible week로 이동해야 한다
    				mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK_BY_SF;
					mWhichViewVisibleWeek = NEXT_WEEK_SIDE_VIEW;
					
    				mViewState = ViewState.TRANSLATE_UPDATE_VISIBLE_WEEK_ANIMATION;	                				
    				// DayView.switchViews에서 
    				// mController.setTime(start.normalize(true)); 호출하고 있다...
    				// 우리도 호출해야 하는 것이 아닌가???    			
    				// :...
    				// animation duratin을 day view snap animation duration과 싱크를 맞춰야하지 않을까?    				
    				CircularMonthDayHeaderView willBeVisibleWeek = makeNextWeekToVisibleWeek(USE_DEFAULT_ANIMATION_VELOCITY, DEFAULT_ANIMATION_VELOCITY);     
    				
    				willBeVisibleWeek.setSelectedTimeForTranslateAnimation(7);
    				int translationAnimDirOfDateIndicator = 0;
    				
    	    		if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) < 0) { //DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE
    	    			translationAnimDirOfDateIndicator = DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE;
    	    		}
    	    		else if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) > 0) { //
    	    			translationAnimDirOfDateIndicator = DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE;
    	    		}
    	    		
    				updateDateIndicator(willBeVisibleWeek.mTranslateIncomingFromRightAnimation.getDuration(), 
    						translationAnimDirOfDateIndicator, willBeVisibleWeek.mSelectedDayOfMonthDayHeader);
    				
    				//startNextWeekToVisibleWeek();	                				
    				sendGoToEvent(7);
    				
    			}
    			else {
    				// 자기 자리로 
    				mViewState = ViewState.TRANSLATE_IN_PLACE_ANIMATION;
    				makeInPlaceAnimation();
    				startInPlaceAnimation();	                				
    			}
    		}
    		else {
    			float gotoPrvWeekThreshold = 0 + gotoThreshold;
    			if (gotoPrvWeekThreshold <= currentXPos) {
    				mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK_BY_SF;
					mWhichViewVisibleWeek = PREVIOUS_WEEK_SIDE_VIEW;
					
    				mViewState = ViewState.TRANSLATE_UPDATE_VISIBLE_WEEK_ANIMATION;
    				
    				CircularMonthDayHeaderView willBeVisibleWeek = makePreviousWeekToVisibleWeek(USE_DEFAULT_ANIMATION_VELOCITY, DEFAULT_ANIMATION_VELOCITY);                 				
    				willBeVisibleWeek.setSelectedTimeForTranslateAnimation(-7);
    				int translationAnimDirOfDateIndicator = 0;
    	    		if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) < 0) { //DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE
    	    			translationAnimDirOfDateIndicator = DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE;
    	    		}
    	    		else if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) > 0) { //
    	    			translationAnimDirOfDateIndicator = DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE;
    	    		}
    	    		
    				updateDateIndicator(willBeVisibleWeek.TranslateIncomingFromLeftAnimation.getDuration(), 
    						translationAnimDirOfDateIndicator, willBeVisibleWeek.mSelectedDayOfMonthDayHeader);
    				//startPreviousWeekToVisibleWeek();
    				sendGoToEvent(-7);	                				
    			}
    			else {
    				// 자기 자리로
    				mViewState = ViewState.TRANSLATE_IN_PLACE_ANIMATION;
    				makeInPlaceAnimation();
    				startInPlaceAnimation();	                				
    			}
    		}
    	}
    	else { // 즉   현재 Visible View의 CurrentX가 0이라는 애기...굳이 아래 코드를 수행할 필요가 있는가???	
    		for (int i=0; i<size; i++) {
        		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
        		obj.resetPositionX();
        	}
    	}    	
	}
	
	
	class MonthDayHeaderViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {            
        	doSingleTapUpMonthDayHeaderLayout(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
                  	
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {                     
            doScrollMonthDayHeaderLayout(e1, e2, distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {            
            doFlingMonthDayHeaderLayout(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {            
        	doDownMonthDayHeaderLayout(ev);
            return true;
        }
    }
	
	private int mTouchModeMonthDayHeaderLayout = TOUCH_MODE_INITIAL_STATE_MONTHDAYHEADER_LAYOUT;	
	private void doDownMonthDayHeaderLayout(MotionEvent ev) {    	
		mTouchModeMonthDayHeaderLayout = TOUCH_MODE_DOWN_MONTHDAYHEADER_LAYOUT;        
        //mOnFlingCalledMonthDayHeaderLayout = false; // 문제가 있다...우리는 fling을 중도에 취소할 시나리오는 적용하지 않는다!!!
                                                    // 그러므로 이것이 필요한가를???
	}	
	
	private void doSingleTapUpMonthDayHeaderLayout(MotionEvent ev) {
		if (INFO) Log.i(TAG, "+doSingleTapUpMonthDayHeaderLayout");
		
        if (mScrollingMonthDayHeaderLayout || mOnFlingCalledMonthDayHeaderLayout) {
        	if (INFO) Log.i(TAG, "doSingleTapUpMonthDayHeaderLayout:return");
            return;
        }        
        
        if (mTouchModeMonthDayHeaderLayout == TOUCH_MODE_DOWN_MONTHDAYHEADER_LAYOUT) {
        	if (INFO) Log.i(TAG, "doSingleTapUpMonthDayHeaderLayout:1");
        	
	        int size = mCircularMonthDayHeaderViewList.size();
	    	for (int i=0; i<size; i++) {
	    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
	    		if (obj.mWhichView == VISIBLE_WEEK_VIEW) {
	    			if (INFO) Log.i(TAG, "doSingleTapUpMonthDayHeaderLayout:break");
	    			int monthDayIndex = -1;
	    	        int x = (int) ev.getX();
	    	        monthDayIndex = obj.selectedMonthDaysIndex(x);
	            	int selectedJulianDay = obj.getFirstJulianDay() + monthDayIndex;
	            		            	
	            	Calendar selectedTime = GregorianCalendar.getInstance(mTimeZone);
	            	long mills = ETime.getMillisFromJulianDay(selectedJulianDay, mTimeZone, mFirstDayOfWeek);
	            	selectedTime.setTimeInMillis(mills);
	            	selectedTime.set(Calendar.HOUR_OF_DAY, 0);
	            	selectedTime.set(Calendar.MINUTE, 0);
	            	selectedTime.set(Calendar.SECOND, 0);
	            	selectedTime.set(Calendar.MILLISECOND, 0);
	            	
	                int translationAnimDirOfDateIndicator = 0;
    	    		if (ETime.compare(mSelectedDay, selectedTime) < 0) { //DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE
    	    			translationAnimDirOfDateIndicator = DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE;
    	    		}
    	    		else if (ETime.compare(mSelectedDay, selectedTime) > 0) { //
    	    			translationAnimDirOfDateIndicator = DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE;
    	    		}
    	    		
    	    		mGoToAnimationCase = GO_TO_DATE_INSIDE_WEEK;
    	    		updateSelectedDayInSideWeek(selectedTime, (long) 300); 
    	    		
	    			updateDateIndicator(300, translationAnimDirOfDateIndicator, selectedTime); 			    			
	    			
	    			sendGoToEvent(selectedTime);
	    			break;
	    		}
	    	}
	    	
	    	mTouchModeMonthDayHeaderLayout = TOUCH_MODE_INITIAL_STATE_MONTHDAYHEADER_LAYOUT;
        }
        
        if (INFO) Log.i(TAG, "-doSingleTapUpMonthDayHeaderLayout");
	}
	
	
    float mPrvSrollingPosXMonthDayHeaderLayout;	    
    private void doScrollMonthDayHeaderLayout(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {        
        if (mStartingScrollMonthDayHeaderLayout) { // 시발점은 doDown이다
        	mStartingScrollMonthDayHeaderLayout = false; 	
        	mScrollingMonthDayHeaderLayout = true;
        	
        	mPrvSrollingPosXMonthDayHeaderLayout = e1.getX();       		
		    int size = mCircularMonthDayHeaderViewList.size();
		    for (int i=0; i<size; i++) {
        		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
        		obj.resetPositionX(); // 굳이 할 필요가 있는가???
        	}
        }
        else {
        	// animation after scrolling 수행 도중 scrolling이 발생하였다면,,,
        	// 수행하면 안된다
        	if (!mScrollingMonthDayHeaderLayout)
        		return;
        }        
           	
    	float lastScrollingX = e2.getX();
    	float dX = lastScrollingX - mPrvSrollingPosXMonthDayHeaderLayout;
    	mPrvSrollingPosXMonthDayHeaderLayout = lastScrollingX;       	
    	
	    int size = mCircularMonthDayHeaderViewList.size();
	    for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
    		obj.setPositionX(dX);
    	}
	    		    
	    for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
    		obj.invalidate();
    	}
        	     
    }
    
    private void doFlingMonthDayHeaderLayout(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {      	
    	//mOnFlingCalledMonthDayHeaderLayout = true;
    	if (INFO) Log.i(TAG, "+doFlingMonthDayHeaderLayout");
    	if (mScrollingMonthDayHeaderLayout) {
    		mScrollingMonthDayHeaderLayout = false;
    		if (INFO) Log.i(TAG, "1");
    		
    		mOnFlingCalledMonthDayHeaderLayout = true;
    		
    		mTouchModeMonthDayHeaderLayout = TOUCH_MODE_INITIAL_STATE_MONTHDAYHEADER_LAYOUT;        	
        	
        	float velocityXAbs = Math.abs(velocityX);
        	
        	if (velocityXAbs > FlING_THRESHOLD_VELOCITY) {
        	
        		if (INFO) Log.i(TAG, "1-1");
        		
	        	int deltaX = (int) e2.getX() - (int) e1.getX();
	        	if (deltaX < 0) {      
	        		
	        		if (INFO) Log.i(TAG, "1-1-1");
	        		mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK_BY_SF;
					mWhichViewVisibleWeek = NEXT_WEEK_SIDE_VIEW;
					
	        		mViewState = ViewState.TRANSLATE_UPDATE_VISIBLE_WEEK_ANIMATION;
	        		CircularMonthDayHeaderView willBeVisibleWeek = makeNextWeekToVisibleWeek(USE_DEFAULT_ANIMATION_VELOCITY, DEFAULT_ANIMATION_VELOCITY);
	        		
	        		willBeVisibleWeek.setSelectedTimeForTranslateAnimation(7);  
	        		
	        		int translationAnimDirOfDateIndicator = 0;
		    		if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) < 0) { //DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE
		    			translationAnimDirOfDateIndicator = DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE;
		    		}
		    		else if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) > 0) { //
		    			translationAnimDirOfDateIndicator = DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE;
		    		}
		    		
	        		updateDateIndicator(willBeVisibleWeek.mTranslateIncomingFromRightAnimation.getDuration(), 
	        				translationAnimDirOfDateIndicator, willBeVisibleWeek.mSelectedDayOfMonthDayHeader);
	        			
					sendGoToEvent(7);
	        	}
	        	else {  
	        		
	        		if (INFO) Log.i(TAG, "1-1-2");
	        		mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK_BY_SF;
					mWhichViewVisibleWeek = PREVIOUS_WEEK_SIDE_VIEW;
					
	        		mViewState = ViewState.TRANSLATE_UPDATE_VISIBLE_WEEK_ANIMATION;
	        		CircularMonthDayHeaderView willBeVisibleWeek = makePreviousWeekToVisibleWeek(USE_DEFAULT_ANIMATION_VELOCITY, DEFAULT_ANIMATION_VELOCITY);
	        		willBeVisibleWeek.setSelectedTimeForTranslateAnimation(-7);   
	        		int translationAnimDirOfDateIndicator = 0;
		    		if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) < 0) { //DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE
		    			translationAnimDirOfDateIndicator = DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE;
		    		}
		    		else if (ETime.compare(mSelectedDay, willBeVisibleWeek.mSelectedDayOfMonthDayHeader) > 0) { //
		    			translationAnimDirOfDateIndicator = DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE;
		    		}
		    		
	        		updateDateIndicator(willBeVisibleWeek.TranslateIncomingFromLeftAnimation.getDuration(), 
	        				translationAnimDirOfDateIndicator, willBeVisibleWeek.mSelectedDayOfMonthDayHeader);
							
					sendGoToEvent(-7);
	        	} 
        	}
        	else {
        		if (INFO) Log.i(TAG, "1-2");
        		mViewState = ViewState.TRANSLATE_IN_PLACE_ANIMATION;
				makeInPlaceAnimation();
				startInPlaceAnimation();	   
        	}
        	
        	if (INFO) Log.i(TAG, "1-3");
        	// 여기서 이제 touch event 발생을 막자
        	// ...
        	mMonthDayHeaderLayout.setOnTouchListener(null);
    	}
    	else {
    		if (INFO) Log.i(TAG, "2");
    		mOnFlingCalledMonthDayHeaderLayout = false;
    	}
    	
    	if (INFO) Log.i(TAG, "-doFlingMonthDayHeaderLayout");
    	return;
    }
    	
	public void setParentView(DayView parent) {
		mParentView = parent;
	}
	
	public void updateTimeZone(TimeZone tz) {
    	mTimeZone = tz;
    	mSelectedDay.setTimeZone(mTimeZone);   	
    	
        long now = System.currentTimeMillis();
        //long gmtoff = mTimeZone.getRawOffset() / 1000;
        mTodayJulianDay = ETime.getJulianDay(now, mTimeZone, mFirstDayOfWeek);
        
    	setMidnightHandler();
    	
    	int size = mCircularMonthDayHeaderViewList.size();
    	for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);
    		obj.updateTimeZone(tz);
    	}
    }	
	
	
	public int mGoToAnimationCase = 0;
	private final int GO_TO_DATE_INSIDE_WEEK = 1; // 현재 current visible week 내에서 goToDate이 발생한 것   
    private final int GO_TO_DATE_OUTSIDE_WEEK = 2; // 현재 current visible week에서 prv/next week로 selected day가 변경[토/일 에서 일/월]
    private final int GO_TO_DATE_OUTSIDE_WEEK_BY_SF = 3; 
    //private final int GO_TO_DATE_FROM_ANOTHER_WEEK = 3; // prv/next week의 토/일이 selected day가 됨
	
    public int mWhichViewVisibleWeek = 0;
    public int GO_TO_DATE_OUTSIDE_WEEK_WILL_BE_PRV_WEEK_TO_VISIBLE_WEEK = 1;
    public int GO_TO_DATE_OUTSIDE_WEEK_WILL_BE_NEXT_WEEK_TO_VISIBLE_WEEK = 2;
    
    Runnable mUpdateSelectedDayInSideWeekRunnable = null;
    Runnable mUpdateSelectedDayOutSideWeekRunnable = null;
    Runnable mWillBeVisibleWeekRunnable = null;
	// goToDate call context
	// 1.DayView::updateSecondaryActionBar->UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP,,,duration:?ms
	//   ->DayFragment::secondaryActionBarGoToDate
	// 2.DayFragment::goTo->UPDATE_DAYVIEW_TYPE_BY_OUTER,,, duration:300ms
	//   ->DayFragment::secondaryActionBarGoToDate
	public void goToDate(int selectedTimeType, Calendar goToDateTime, long animationDuration) { 
		
		int whatVelocity = 0;
		float duration = 0;
		
		whatVelocity = SUPPORTED_DURATION;
		duration = animationDuration;		
		
		float velocityOrDuration = duration;
		
		int translationAnimDirOfDateIndicator = 0;
		if (ETime.compare(mSelectedDay, goToDateTime) < 0) { //DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE
			translationAnimDirOfDateIndicator = DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE;
		}
		else if (ETime.compare(mSelectedDay, goToDateTime) > 0) { //
			translationAnimDirOfDateIndicator = DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE;
		}
		
		if (selectedTimeType == DayFragment.UPDATE_DAYVIEW_TYPE_BY_TODAY) {
			//long gmtoff = mTimeZone.getRawOffset() / 1000;
			int todayJulianDay = ETime.getJulianDay(goToDateTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
			boolean existTodayAtMonthDayHeaderViewList = false;
			CircularMonthDayHeaderView todayExistMonthDayHeaderView = null;
			
			int size = mCircularMonthDayHeaderViewList.size();
			for (int i=0; i<size; i++) {
	    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i); 
	    		int firstJulianDay = obj.getFirstJulianDay();
	    		int lastJulianDay = obj.getLastJulianDay();
	    		
	    		if (firstJulianDay <= todayJulianDay && todayJulianDay <= lastJulianDay) {
	    			existTodayAtMonthDayHeaderViewList = true;
	    			todayExistMonthDayHeaderView = obj;
	    			break;
	    		}
	    	}
			
			if (!existTodayAtMonthDayHeaderViewList) {
				mGoToAnimationCase = 0;
				// 어떠한 animation도 없이 업데이트해야 함				
				mSelectedDay.setTimeInMillis(goToDateTime.getTimeInMillis());
				mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
				mSelectedDay.set(Calendar.MINUTE, 0);
				mSelectedDay.set(Calendar.SECOND, 0);
				mSelectedDay.set(Calendar.MILLISECOND, 0);
				
				mMonthDayHeaderLayout.removeAllViews();
	        	mCircularMonthDayHeaderViewList.clear();
	        	
	        	CircularMonthDayHeaderView visibleWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, VISIBLE_WEEK_VIEW);
	    		mMonthDayHeaderLayout.addView(visibleWeekView); 
	    		mCircularMonthDayHeaderViewList.add(visibleWeekView);
	    		
	    		CircularMonthDayHeaderView previousWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, PREVIOUS_WEEK_SIDE_VIEW);
	    		mMonthDayHeaderLayout.addView(previousWeekView); 
	    		mCircularMonthDayHeaderViewList.add(previousWeekView);
	    		
	    		CircularMonthDayHeaderView nextWeekView = new CircularMonthDayHeaderView(mContext, this, mMonthDayHeaderLayout, mSelectedDay, NEXT_WEEK_SIDE_VIEW);
	    		mMonthDayHeaderLayout.addView(nextWeekView); 
	    		mCircularMonthDayHeaderViewList.add(nextWeekView);	    		
	    		
	        	mDateIndicatorTime = GregorianCalendar.getInstance(mTimeZone);
	        	mDateIndicatorTime.setFirstDayOfWeek(goToDateTime.getFirstDayOfWeek());
	        	mDateIndicatorTime.setTimeInMillis(goToDateTime.getTimeInMillis());
	        	mDateIndicatorTime.set(Calendar.HOUR_OF_DAY, 0);
	        	mDateIndicatorTime.set(Calendar.MINUTE, 0);
	        	mDateIndicatorTime.set(Calendar.SECOND, 0);
	        	mDateIndicatorTime.set(Calendar.MILLISECOND, 0);
	    		mDateIndicatorTextView.setText(buildFullDate(mDateIndicatorTime.getTimeInMillis()));
				return;
			}
			else {			
				
				if (todayExistMonthDayHeaderView.mWhichView == VISIBLE_WEEK_VIEW) {
					mGoToAnimationCase = GO_TO_DATE_INSIDE_WEEK;
					updateSelectedDayInSideWeek(goToDateTime, (long) duration); 					
					updateDateIndicator((long) duration, translationAnimDirOfDateIndicator, goToDateTime); // 동기화에 별 영향을 미치지 않는다					
					
					return;
				}
				else if (todayExistMonthDayHeaderView.mWhichView == PREVIOUS_WEEK_SIDE_VIEW) {
					mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK;
					mWhichViewVisibleWeek = PREVIOUS_WEEK_SIDE_VIEW;
					CircularMonthDayHeaderView willBeVisibleWeek = makePreviousWeekToVisibleWeek(whatVelocity, velocityOrDuration);  
					updateSelectedDayOutsideWeek(PREVIOUS_WEEK_SIDE_VIEW, goToDateTime, (long) duration);						
					updateDateIndicator(willBeVisibleWeek.TranslateIncomingFromLeftAnimation.getDuration(), translationAnimDirOfDateIndicator, goToDateTime);
					willBeVisibleWeek.setSelectedTimeForGoToToday(goToDateTime.getTimeInMillis());/////////수정해줘야 한다					
						
					return;
				}
				else if (todayExistMonthDayHeaderView.mWhichView == NEXT_WEEK_SIDE_VIEW) {
					mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK;
					mWhichViewVisibleWeek = NEXT_WEEK_SIDE_VIEW;
					CircularMonthDayHeaderView willBeVisibleWeek = makeNextWeekToVisibleWeek(whatVelocity, velocityOrDuration);
					updateSelectedDayOutsideWeek(NEXT_WEEK_SIDE_VIEW, goToDateTime, (long) duration);						
					updateDateIndicator(willBeVisibleWeek.mTranslateIncomingFromRightAnimation.getDuration(), translationAnimDirOfDateIndicator, goToDateTime);
					willBeVisibleWeek.setSelectedTimeForGoToToday(goToDateTime.getTimeInMillis());////////////////////////				
					
					return;
				}
			}
		}		
		else if (selectedTimeType == DayFragment.UPDATE_DAYVIEW_TYPE_BY_DAYVIEW_SNAP) {
			int weekDay = mSelectedDay.get(Calendar.DAY_OF_WEEK);
			if (weekDay == mFirstDayOfWeek) {
				// 이는 더 이상 visible week가 visible week가 아니라,,,
				// previous week가 visible week가 되어야 한다는 것을 의미한다
				// ex) first day of week가 일요일이고 
				//     선택된 날짜가 바로 그 전날이라면,,,
				Calendar previousDayOfSelectedDay = GregorianCalendar.getInstance(mTimeZone);//new Time(mSelectedDay);
				previousDayOfSelectedDay.setFirstDayOfWeek(mSelectedDay.getFirstDayOfWeek());
				previousDayOfSelectedDay.setTimeInMillis(mSelectedDay.getTimeInMillis());
				
				previousDayOfSelectedDay.add(Calendar.DAY_OF_MONTH, -1);				
				
				
				if ( goToDateTime.get(Calendar.DAY_OF_MONTH) == previousDayOfSelectedDay.get(Calendar.DAY_OF_MONTH)) {	
					mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK;
					mWhichViewVisibleWeek = PREVIOUS_WEEK_SIDE_VIEW;
					// 현재 visible week의 selected day에 alpha blending이 적용되어야 한다
					// 또한 prv week의 new selected day에 alpha blending이 적용되어야 한다
					// : 지금 현재 visible week와 prv week의 translation animation만 적용되어 있다
					CircularMonthDayHeaderView willBeVisibleWeek = makePreviousWeekToVisibleWeek(whatVelocity, velocityOrDuration);  
					updateSelectedDayOutsideWeek(PREVIOUS_WEEK_SIDE_VIEW, goToDateTime, (long) duration);	
					
					updateDateIndicator(willBeVisibleWeek.TranslateIncomingFromLeftAnimation.getDuration(), translationAnimDirOfDateIndicator, goToDateTime);
					willBeVisibleWeek.setSelectedTimeForTranslateAnimation(-1);				
						
					return;
				}
			}
			else if (weekDay == mLastDayOfWeek) {
				
				// 이는 더 이상 visible week가 visible week가 아니라,,,
				// next week가 visible week가 되어야 한다는 것을 의미한다
				// ex) first day of week가 일요일이고 
				//     선택된 날짜가 바로 그 다음일이라면,,,
				//Time nextDayOfSelectedDay = new Time(mSelectedDay);			
				
				Calendar nextDayOfSelectedDay = GregorianCalendar.getInstance(mTimeZone);//new Time(mSelectedDay);
				nextDayOfSelectedDay.setFirstDayOfWeek(mSelectedDay.getFirstDayOfWeek());
				nextDayOfSelectedDay.setTimeInMillis(mSelectedDay.getTimeInMillis());
				nextDayOfSelectedDay.add(Calendar.DAY_OF_MONTH, 1);	
				
				if ( goToDateTime.get(Calendar.DAY_OF_MONTH) == nextDayOfSelectedDay.get(Calendar.DAY_OF_MONTH)) {
					mGoToAnimationCase = GO_TO_DATE_OUTSIDE_WEEK;
					mWhichViewVisibleWeek = NEXT_WEEK_SIDE_VIEW;
					// alpha blending을 적용해야 한다
					CircularMonthDayHeaderView willBeVisibleWeek = makeNextWeekToVisibleWeek(whatVelocity, velocityOrDuration);
					updateSelectedDayOutsideWeek(NEXT_WEEK_SIDE_VIEW, goToDateTime, (long) duration);	
					
					updateDateIndicator(willBeVisibleWeek.mTranslateIncomingFromRightAnimation.getDuration(), translationAnimDirOfDateIndicator, goToDateTime);
					willBeVisibleWeek.setSelectedTimeForTranslateAnimation(1);				
					
					return;
				}
			}
					
			mGoToAnimationCase = GO_TO_DATE_INSIDE_WEEK;
			updateSelectedDayInSideWeek(goToDateTime, (long) duration); 
			
			updateDateIndicator((long) duration, translationAnimDirOfDateIndicator, goToDateTime); // 동기화에 별 영향을 미치지 않는다					
			
		}
		
    }    
    
	public void updateSelectedDayInSideWeek(Calendar time, long animationDuration) {
		mSelectedDay.setTimeInMillis(time.getTimeInMillis());
		mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDay.set(Calendar.MINUTE, 0);
		mSelectedDay.set(Calendar.SECOND, 0);
		mSelectedDay.set(Calendar.MILLISECOND, 0);
		
    	int size = mCircularMonthDayHeaderViewList.size();
    	for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);    		
    		if (obj.mWhichView == VISIBLE_WEEK_VIEW) {    			
    			mUpdateSelectedDayInSideWeekRunnable = obj.goToDateInsideWeekAnimation(time, animationDuration);
    		}
    		else {
    			// PRV/NEXT WEEK VEIW는 
    			// VISIBLE_WEEK_VIEW에서 새로 선택된 Selected Day의 요일로 설정해야 됨
    			// :month day header에서 발생할 scrolling이나 fling에 대비해 미리 준비함
    			//  너무 이른거 아닌지...
    			obj.setSelectedTime(time.getTimeInMillis());    			
    		}
    	}
    	
	}
	
	public void updateSelectedDayOutsideWeek(int willBeVisibleWeek, Calendar time, long animationDuration) {
		mSelectedDay.setTimeInMillis(time.getTimeInMillis());
		mSelectedDay.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDay.set(Calendar.MINUTE, 0);
		mSelectedDay.set(Calendar.SECOND, 0);
		mSelectedDay.set(Calendar.MILLISECOND, 0);
		
    	int size = mCircularMonthDayHeaderViewList.size();
    	for (int i=0; i<size; i++) {
    		CircularMonthDayHeaderView obj = mCircularMonthDayHeaderViewList.get(i);    		
    		if (obj.mWhichView == VISIBLE_WEEK_VIEW) {    			
    			mUpdateSelectedDayOutSideWeekRunnable = obj.goToDateInOutsideWeekAnimation(time, animationDuration);
    		}
    		else if (obj.mWhichView == willBeVisibleWeek) {
    			mWillBeVisibleWeekRunnable = obj.goToDateFromAnotherWeekAnimation(time, animationDuration);
    		}
    		else {    			
    			obj.setSelectedTime(time.getTimeInMillis());    	
    			obj.invalidate();
    		}
    	}
	}	
	
	
	Calendar mDateIndicatorTime;
	TranslateAnimation mUpdateDateIndicatorTranslateAnim = null;
	AnimationListener mUpdateDateIndicatorAlphaAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mDateIndicatorTextView.setText(buildFullDate(mDateIndicatorTime.getTimeInMillis()));
			
			mDateIndicatorTextView.startAnimation(mUpdateDateIndicatorTranslateAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	public static final int DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE = 1;
	public static final int DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE = 2;	
	
	AlphaAnimation mUpdateIndicatorAlphaAnim = null;
	public void updateDateIndicator(long animationDuration, int dirTranslate, Calendar newTime) {
		// 우리는 DayView의 animation 또는 MonthDay HeaderView 보다 더 빠르길 바란다
		float adjustDuration = (float)animationDuration * 0.8f;
		float alphaDuration = adjustDuration * 0.3f;
		float translateDuration = adjustDuration * 0.7f;
		
    	// negative result if mSelectedDay is earlier : right to self position 		
    	if (dirTranslate == DATE_INDICATOR_RIGHT_TO_SELF_TRANSLATE) {
    		
    		mUpdateIndicatorAlphaAnim = makeAlphaAnimation((long)alphaDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.8f, 0.4f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		mUpdateIndicatorAlphaAnim.setAnimationListener(mUpdateDateIndicatorAlphaAnimlistener);
    		
    		mUpdateDateIndicatorTranslateAnim = makeSlideAnimation((long)translateDuration, Animation.RELATIVE_TO_SELF, 0.25f, Animation.RELATIVE_TO_SELF, 0);    		
    	}
    	// positive result if mSelectedDay is lately : left to self position
    	else if (dirTranslate == DATE_INDICATOR_LEFT_TO_SELF_TRANSLATE) {    		
    		mUpdateIndicatorAlphaAnim = makeAlphaAnimation((long)alphaDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.8f, 0.4f, ANIMATION_INTERPOLATORTYPE_DECELERATE);    		
    		mUpdateIndicatorAlphaAnim.setAnimationListener(mUpdateDateIndicatorAlphaAnimlistener);
    		
    		mUpdateDateIndicatorTranslateAnim = makeSlideAnimation((long)translateDuration, Animation.RELATIVE_TO_SELF, -0.25f, Animation.RELATIVE_TO_SELF, 0);   		
    	}
    	else {
    		// selectedTime과 newTime이 같을 수 있는 경우가 발생할 수 있는가?
    		return;
    	}
            	
        if (mInitedActionBarLayout == true) {
        	mDateIndicatorTime = GregorianCalendar.getInstance(mTimeZone);
        	mDateIndicatorTime.setFirstDayOfWeek(mFirstDayOfWeek);
        	mDateIndicatorTime.setTimeInMillis(newTime.getTimeInMillis()); 
        	mDateIndicatorTime.set(Calendar.HOUR_OF_DAY, 0);
        	mDateIndicatorTime.set(Calendar.MINUTE, 0);
        	mDateIndicatorTime.set(Calendar.SECOND, 0);
        	mDateIndicatorTime.set(Calendar.MILLISECOND, 0);
        	//mDateIndicatorTextView.startAnimation(mUpdateIndicatorAlphaAnim);
        }    	
    }
	
	public void refreshDateIndicator(boolean supportAnimationDuration, long animationDuration, Calendar selectedTime, Calendar newTime) {		
		TranslateAnimation ta = null;
    	// negative result if mSelectedDay is earlier : right to self position 
		if (supportAnimationDuration) {
	    	if (ETime.compare(selectedTime, newTime) < 0) {
	    		ta = makeSlideAnimation(animationDuration, Animation.RELATIVE_TO_SELF, 0.25f, Animation.RELATIVE_TO_SELF, 0);
	    		
	    	}
	    	// positive result if mSelectedDay is lately : left to self position
	    	else if (ETime.compare(selectedTime, newTime) > 0) {
	    		ta = makeSlideAnimation(animationDuration, Animation.RELATIVE_TO_SELF, -0.25f, Animation.RELATIVE_TO_SELF, 0);
	    	}		
		}
		else {
			float velocity = (float)animationDuration;
			float animationDistance = 0;
			float delta = 0;
			float width = 0;
			
			if (ETime.compare(selectedTime, newTime) < 0) {
	    		ta = makeSlideAnimation(animationDistance, delta, width, velocity, Animation.RELATIVE_TO_SELF, 0.25f, Animation.RELATIVE_TO_SELF, 0);
	    		
	    	}
	    	// positive result if mSelectedDay is lately : left to self position
	    	else if (ETime.compare(selectedTime, newTime) > 0) {
	    		ta = makeSlideAnimation(animationDistance, delta, velocity, width, Animation.RELATIVE_TO_SELF, -0.25f, Animation.RELATIVE_TO_SELF, 0);
	    	}				
		}
        
        if (mInitedActionBarLayout == true) {
        	
        	mDateIndicatorTextView.setText(buildFullDate(selectedTime.getTimeInMillis()));       	
        	
        	// 0 if they are equal
        	if (ta == null) {
        		// OOOOOOOOOOOOOOOOppppssss!!!
        		return;
        	}
        	
        	mDateIndicatorTextView.startAnimation(ta);
        }  		
    }
	
	public void sendGoToEvent(int goToDateOffset) {
		Calendar selectedTime = GregorianCalendar.getInstance(mTimeZone); //new Time(mSelectedDay);
		selectedTime.setFirstDayOfWeek(mSelectedDay.getFirstDayOfWeek());
		selectedTime.setTimeInMillis(mSelectedDay.getTimeInMillis());
		
		selectedTime.add(Calendar.DAY_OF_MONTH, goToDateOffset); //selectedTime.monthDay += goToDateOffset;
		
		mController.sendEvent(this, EventType.GO_TO, null, null, selectedTime, -1,
        		ViewType.CURRENT, CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR, 
        		null, null);
	}
	
	public void sendGoToEvent(Calendar selectedTime) {
		
		mController.sendEvent(this, EventType.GO_TO, null, null, selectedTime, -1,
        		ViewType.CURRENT, CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_DATE_NOT_UPDATE_SEC_ACTIONBAR, 
        		null, null);
	}
	
	// Builds strings with different formats:
    // Full date: Month,day Year
    // Month year
    // Month day
    // Month
    // Week:  month day-day or month day - month day
    public String buildFullDate(long milliTime) {
        mStringBuilder.setLength(0);
        String date = ETime.formatDateTimeRange(mContext, mFormatter, milliTime, milliTime,
        		ETime.FORMAT_SHOW_WEEKDAY | ETime.FORMAT_SHOW_DATE | ETime.FORMAT_SHOW_YEAR, mTimeZone.getID()).toString();
        return date;
    }

    
	
    
		
	
    
    
    public static TranslateAnimation makeSlideAnimation(long duration, int fromXType, float fromXDelta, int toXType, float toXDelta) { 
    	
    	TranslateAnimation translateAnimation = new TranslateAnimation(
    			fromXType, fromXDelta, //fromXValue 
    			toXType, toXDelta,   //toXValue
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);        
        
        translateAnimation.setDuration(duration);      
        
        return translateAnimation;        
	}
    
    
    public static TranslateAnimation makeSlideAnimation(float animationDistance, float delta, float width, float velocity, 
    		int fromXType, float fromXDelta, int toXType, float toXDelta) {     	
    	
    	TranslateAnimation translateAnimation = new TranslateAnimation(
    			fromXType, fromXDelta, //fromXValue 
    			toXType, toXDelta,   //toXValue
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        long duration = calculateDuration(delta, width, velocity);
        translateAnimation.setDuration(duration);
        HScrollInterpolator scrollInterpolator = new HScrollInterpolator(translateAnimation, animationDistance);
        translateAnimation.setInterpolator(scrollInterpolator);
        
        return translateAnimation;        
	}    
    
    public static final int ALPHA_OPAQUE_TO_TRANSPARENT = 1;
	public static final int ALPHA_TRANSPARENT_TO_OPAQUE = 2;
	public static final int ANIMATION_INTERPOLATORTYPE_ACCELERATE = 1;
	public static final int ANIMATION_INTERPOLATORTYPE_DECELERATE = 2;
	public AlphaAnimation makeAlphaAnimation(long duration, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {    	
	 	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);      
        
        alphaAnimation.setDuration(duration);
        if (InterpolatorType == ANIMATION_INTERPOLATORTYPE_ACCELERATE)
        	alphaAnimation.setInterpolator(new AccelerateInterpolator());
        else
        	alphaAnimation.setInterpolator(new DecelerateInterpolator());
        
        return alphaAnimation;        
	}	
    public AlphaAnimation makeAlphaAnimation(float delta, float width, float velocity, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {    	
    	 	
    	long duration = calculateDuration(delta, width, velocity);
    	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);        
        
        alphaAnimation.setDuration(duration);
        if (InterpolatorType == ANIMATION_INTERPOLATORTYPE_ACCELERATE)
        	alphaAnimation.setInterpolator(new AccelerateInterpolator());
        else
        	alphaAnimation.setInterpolator(new DecelerateInterpolator());
        
        return alphaAnimation;        
	}
    
    public static long calculateDuration(float delta, float width, float velocity) {
        
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
        velocity = Math.max(MINIMUM_ANIMATION_VELOCITY, velocity);

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
    
        
    public static class HScrollInterpolator implements Interpolator {
    	Animation mAnimation;
    	float mAnimationDistance;
    	
        public HScrollInterpolator(Animation animation, float animationDistance) {
        	mAnimation = animation;
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
        	mAnimation.scaleCurrentDuration(0);    
        	//Log.i("tag", "ccc");
        }
    }

    // 이것도 꽝임
    // 아마도 각 animation의 duration이 문제가 되는 것이 아닐까?
    // 문제라기 보다 이미 duration이 경과되었을 것으로 판단되기 때문에...
    private Runnable XXX = new Runnable() {
        @Override
        public void run() {
        	if (mGoToAnimationCase == GO_TO_DATE_INSIDE_WEEK) {
        		if (INFO) Log.i(TAG, "GO_TO_DATE_INSIDE_WEEK");
        		
    			mUpdateSelectedDayInSideWeekRunnable.run();					
    		}
    		else if (mGoToAnimationCase == GO_TO_DATE_OUTSIDE_WEEK) {
    			
    			mUpdateSelectedDayOutSideWeekRunnable.run();
    			mWillBeVisibleWeekRunnable.run();
    					
    			if (mWhichViewVisibleWeek == PREVIOUS_WEEK_SIDE_VIEW) {	
    				if (INFO) Log.i(TAG, "GO_TO_DATE_OUTSIDE_WEEK:PREVIOUS_WEEK_SIDE_VIEW");
    				startPreviousWeekToVisibleWeek();
    			}
    			else if (mWhichViewVisibleWeek == NEXT_WEEK_SIDE_VIEW) {
    				if (INFO) Log.i(TAG, "GO_TO_DATE_OUTSIDE_WEEK:NEXT_WEEK_SIDE_VIEW");
    				startNextWeekToVisibleWeek();
    			}		
    		}
    		else if (mGoToAnimationCase == GO_TO_DATE_OUTSIDE_WEEK_BY_SF) {
    			// 여기서 하면 문제가 발생된다...
    			
    			if (mWhichViewVisibleWeek == PREVIOUS_WEEK_SIDE_VIEW) {	
    				if (INFO) Log.i(TAG, "GO_TO_DATE_OUTSIDE_WEEK_BY_SF:PREVIOUS_WEEK_SIDE_VIEW");
    				startPreviousWeekToVisibleWeek();
    			}
    			else if (mWhichViewVisibleWeek == NEXT_WEEK_SIDE_VIEW) {	
    				if (INFO) Log.i(TAG, "GO_TO_DATE_OUTSIDE_WEEK_BY_SF:NEXT_WEEK_SIDE_VIEW");
    				startNextWeekToVisibleWeek();
    			}
    				
    		}
    		else {
    			// DON'T animation!!!
    			// goToDate의 if(!existTodayAtMonthDayHeaderViewList)인 경우임!!!
    			return;
    		}
    		
    		if (mInitedActionBarLayout == true) {	        	
            	mDateIndicatorTextView.startAnimation(mUpdateIndicatorAlphaAnim);
            }  		
        }
    };    
    
    
    OnStartMonthDayHeaderAnimationListener mOnStartMonthDayHeaderAnimationListener = new OnStartMonthDayHeaderAnimationListener() {

		@Override
		public void startMonthDayHeaderAnimation() {
			// TODO Auto-generated method stub
			CalendarViewsSecondaryActionBar.this.postOnAnimation(XXX);	// 아마도 다른 곳?에서 animation을 시작했다면,,,postOnAnimation를 사용해서 animation을 해야 하나봄
																		// 안 그럼 뻑? 발생함...
			//CalendarViewsSecondaryActionBar.this.post(XXX); // 이상하게 뻑? 남... 
		}
    	
    };
       
    
    
}
