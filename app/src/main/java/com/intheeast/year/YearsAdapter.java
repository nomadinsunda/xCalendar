package com.intheeast.year;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
//import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.AbsListView.LayoutParams;

public class YearsAdapter extends BaseAdapter implements OnTouchListener {

	private static final String TAG = "YearsAdapter";
    private static boolean INFO = true;
    
	public static final int EPOCH_YEAR = 1900;
	public static final int MONTH_NUMBERS_PER_CHILDVIEW = 3;
	public static final int POSITION_COUNT = 800;
	
	public static final int MONTHVIEW_NORMAL_MODE = 1;
	public static final int MONTHVIEW_EXPAND_EVENTLIST_MODE = 2;
	
	public static final String MONTHS_PARAMS_MONTHVIEW_MODE = "monthview_mode";
	
	public static final String MONTHS_PARAMS_ORIGINAL_MINIMONTH_WIDTH = "original_minimonth_width";
	public static final String MONTHS_PARAMS_ORIGINAL_MINIMONTH_HEIGHT = "original_minimonth_height";
	
	public static final String MONTHS_PARAMS_MINIMONTH_WIDTH = "minimonth_width";
	
	public static final String MONTHS_PARAMS_MINIMONTH_HEIGHT = "minimonth_height";	
	
	public static final String MONTHS_PARAMS_NUM_WEEKS = "num_weeks";
	
	public static final String MONTHS_PARAMS_WEEK_START = "week_start";
	
	public static final String MONTHS_PARAMS_PARAMS_SELECTED_YEAR_JULIAN_DAY = "selected_year_julianday";
	
	public static final String MONTHS_PARAMS_DAYS_PER_WEEK = "days_per_week";	
	   
	public static final String MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT = "yearindicator_height";
	
	public static final String MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE = "yearindicator_text_size";	
	
	public static final String MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y = "yearindicator_text_baseline_y";	
	
	public static final String MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT = "minimonth_monthindicator_text_height";	
	
	public static final String MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y = "minimonth_monthindicator_text_baseline_y";
	
	public static final String MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y = "minimonth_monthday_text_baseline_y";	
	
	public static final String MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT = "minimonth_monthday_text_height";	
	
	public static final String MONTHS_PARAMS_MINIMONTH_LEFTMARGIN = "minimonth_leftmargin";	
	
	public static final String MONTHS_PARAMS_MONTHLISTVIEW_WIDTH = "monthview_width";
	
	public static final String MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT = "monthview_height";	
	
	//public static final String MONTHS_PARAMS_MONTHLISTVIEW_TOP_OFFSET = "monthview_topoffset";	
	
	public static final String MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT = "monthview_normal_week_height";
	
	//public static final String MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT = "monthview_last_week_height";
	
	public static final String MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT = "monthview_monthindicator_height";
	
	public static final String MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT = "monthview_normal_week_height";
	
	public static final String MONTHS_PARAMS_MONTHINDICATOR_TEXT_COLOR = "monthview_monthindicator_text_color";
	
	public static final String MONTHS_PARAMS_EVENT_CIRCLE_TOP_PADDING = "monthview_eventcircle_top_padding";
	
	
	
	Context mContext;
	Activity mActivity;
	LayoutInflater mInflater;
	
	CalendarController mController;
	YearMonthTableLayout mSingleTapUpView;
	YearMonthTableLayout mClickedView;
	GestureDetector mGestureDetector;
	long mClickTime;
	float mClickedXLocation;
	float mClickedYLocation;
	
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	Calendar mTempTime;
	Calendar mToday;
	
	int mFirstDayOfWeek;
	Calendar mSelectedYear;
	int mSelectedWeek;
	int mDaysPerWeek;
	int mSelectedYearJulainDay;
	
	int mMonthListViewWidth;
	int mMonthListViewHeight;
	
	int mMiniMonthWidth;
	int mMiniMonthHeight;
	int mYearIndicatorHeight;
	int mYearIndicatorTextHeight;
	int mYearIndicatorTextBaseLineY;
	
	int mMiniMonthIndicatorTextHeight;
	int mMiniMonthIndicatorTextBaseLineY;	
	int mMiniMonthDayTextBaseLineY;
	int mMiniMonthDayTextHeight;	
	int mMiniMonthLeftMargin;
	
	int mMonthListViewNormalWeekHeight;
	//int mMonthListViewLastWeekHeight;
	
	// Used to insure minimal time for seeing the click animation before switching views
    private static final int mOnTapDelay = 100;
    // Minimal time for a down touch action before stating the click animation, this insures that
    // there is no click animation on flings
    private static int mOnDownDelay;
    private static int mTotalClickDelay;
    // Minimal distance to move the finger in order to cancel the click animation
    private static float mMovedPixelToCancel;
    
    //boolean mAnimateToday;
    //long mAnimateTime = 0;
    
    YearViewExitAnimObject mYearViewExitAnimObject;
	
	public YearsAdapter(Context context, Activity activity, HashMap<String, Integer> params) {
		mContext = context;
		mActivity = activity;
		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Calendar cal = Calendar.getInstance(Locale.getDefault());
        mFirstDayOfWeek = cal.getFirstDayOfWeek();
        
        init();
        updateParams(params);
        
        ViewConfiguration vc = ViewConfiguration.get(context);
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        mMovedPixelToCancel = vc.getScaledTouchSlop();
        mTotalClickDelay = mOnDownDelay + mOnTapDelay;        
	}
	
	protected void init() {
		mGestureDetector = new GestureDetector(mContext, new CalendarGestureListener());
		mSelectedYear = GregorianCalendar.getInstance();//new Time();
		mSelectedYear.setTimeInMillis(System.currentTimeMillis());
        
        mController = CalendarController.getInstance(mActivity);
        mTzId = Utils.getTimeZone(mContext, null);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;
        ETime.switchTimezone(mSelectedYear, mTimeZone);//mSelectedYear.switchTimezone(mHomeTimeZone);
        mToday = GregorianCalendar.getInstance(mTimeZone);
        mToday.setTimeInMillis(System.currentTimeMillis());
        mTempTime = GregorianCalendar.getInstance(mTimeZone); 
    }
	
	public void updateParams(HashMap<String, Integer> params) {
        if (params == null) {            
            return;
        } 
        
        if (params.containsKey(MONTHS_PARAMS_MONTHLISTVIEW_WIDTH)) {
        	mMonthListViewWidth = params.get(MONTHS_PARAMS_MONTHLISTVIEW_WIDTH);
        }
        
        if (params.containsKey(MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT)) {
        	mMonthListViewHeight = params.get(MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT);
        }
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_WIDTH)) {
        	mMiniMonthWidth = params.get(MONTHS_PARAMS_MINIMONTH_WIDTH);
        }
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_HEIGHT)) {
        	mMiniMonthHeight = params.get(MONTHS_PARAMS_MINIMONTH_HEIGHT);
        }
        
        if (params.containsKey(MONTHS_PARAMS_WEEK_START)) {
            mFirstDayOfWeek = params.get(MONTHS_PARAMS_WEEK_START);
        }
        
        if (params.containsKey(MONTHS_PARAMS_DAYS_PER_WEEK)) {
            mDaysPerWeek = params.get(MONTHS_PARAMS_DAYS_PER_WEEK);
        }
        
        if (params.containsKey(MONTHS_PARAMS_PARAMS_SELECTED_YEAR_JULIAN_DAY)) {
        	mSelectedYearJulainDay = params.get(MONTHS_PARAMS_PARAMS_SELECTED_YEAR_JULIAN_DAY);
        	long mills = ETime.getMillisFromJulianDay(mSelectedYearJulainDay, mTimeZone, mFirstDayOfWeek);
        	mSelectedYear.setTimeInMillis(mills);//mSelectedYear.setJulianDay(mSelectedYearJulainDay);
        }
        
        if (params.containsKey(MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT)) {
        	mYearIndicatorHeight = params.get(MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT);
        }
        
        if (params.containsKey(MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE)) {
        	mYearIndicatorTextHeight = params.get(MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE);
        }
        
        if (params.containsKey(MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y)) {
        	mYearIndicatorTextBaseLineY = params.get(MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y);
        }        
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT)) {
        	mMiniMonthIndicatorTextHeight = params.get(MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT);
        }
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y)) {
        	mMiniMonthIndicatorTextBaseLineY = params.get(MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y)) {
        	mMiniMonthDayTextBaseLineY = params.get(MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y);
        }
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT)) {
        	mMiniMonthDayTextHeight = params.get(MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);
        }        
        
        if (params.containsKey(MONTHS_PARAMS_MINIMONTH_LEFTMARGIN)) {
        	mMiniMonthLeftMargin = params.get(MONTHS_PARAMS_MINIMONTH_LEFTMARGIN);
        }       
                
        if (params.containsKey(MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT)) {
        	mMonthListViewNormalWeekHeight = params.get(MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
        }   
        
        /*if (params.containsKey(MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT)) {
        	mMonthListViewLastWeekHeight = params.get(MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT);
        }*/   
        
        refresh();
    }
	
	public void refresh() {
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        mTzId = Utils.getTimeZone(mContext, null); 
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;
        updateTimeZones();
        notifyDataSetChanged();
    }
	
	private void updateTimeZones() {
		ETime.switchTimezone(mSelectedYear, mTimeZone);//mSelectedYear.timezone = mHomeTimeZone;
		
		ETime.switchTimezone(mToday, mTimeZone);//mToday.timezone = mHomeTimeZone;
        mToday.setTimeInMillis(System.currentTimeMillis());
        
        ETime.switchTimezone(mTempTime, mTimeZone);//mTempTime.switchTimezone(mHomeTimeZone);
    }	
	
	@Override
	public int getCount() {
				
		// 1/1/1900 ~ 1/1/2100
    	// 2100 - 1900 = 200 years
    	// 200 years = 200 * 12 = 2400 months
    	// 1 position draw 3 months
    	// so, need 800 positions
    	// but position number ordering start ZERO!!!
    	// so, position number is from 0 to 799
		/*
		if) position == 0,
			0 * 3 = 0 months
			0 / 12 = 0 years 
			0 % 12 = 0 months ->
			1900 + 0 year/ 0 months + 1
			-> 1900/1 ~ 3
			
		if) position == 3,
			3 * 3 = 9 months
			9 / 12 = 0 years 
			9 % 12 = 9 months ->
			1900 + 0 year/ 9 months + 1
			-> 1900/10 ~ 12
		
		if) position == 5,
			5 * 3 = 15 months
			15 / 12 = 1 years 
			15 % 12 = 3 months ->
			1900 + 1 year/ 3 months + 1
			-> 1901/4 ~ 6
		
		if) position == 799,
			799 * 3 = 2397 months
			2397 / 12 = 199.75 years 
			2397 % 12 = 9 months ->
			1900 + 199 year/ 9 months + 1
			-> 2099/10 ~ 12
		*/
		return POSITION_COUNT;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@SuppressWarnings("unchecked")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
						
		YearMonthTableLayout v;
		
		Calendar firstMonthTime = calcFirstMonthFirstTime(position);		
		
		int monthsViewPattern = YearMonthTableLayout.NORMAL_YEAR_MONTHS_PATTERN;
		if (firstMonthTime.get(Calendar.MONTH) == 0) {
			monthsViewPattern = YearMonthTableLayout.YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN;
		}		
		
		boolean invalidate = false;
		if (convertView != null) {			
			v = (YearMonthTableLayout) convertView;	
			invalidate = true;
		}
		else {
			v = (YearMonthTableLayout) mInflater.inflate(R.layout.monthrow_for_yearview, null);			
			v.setBackgroundColor(Color.WHITE);
			v.setClickable(true);
			
			HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mMiniMonthWidth);
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mMiniMonthHeight);
			
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mYearIndicatorHeight);			
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mYearIndicatorTextHeight);
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mYearIndicatorTextBaseLineY);
			
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, mMiniMonthIndicatorTextHeight);
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, mMiniMonthIndicatorTextBaseLineY);
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, mMiniMonthDayTextBaseLineY);
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, mMiniMonthDayTextHeight);
			
			drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN, mMiniMonthLeftMargin);
			drawingParams.put(YearMonthTableLayout.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
			v.setMonthsParams(drawingParams, mSelectedYear.getTimeZone().getID());			
		}			
		
		v.setOnTouchListener(this);
		
		v.setMonthInfo(invalidate, monthsViewPattern, position);
			
		return v;
	}
		
	public Calendar calcFirstMonthFirstTime(int position) {
		Calendar firstMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mHomeTimeZone);
		// 466
		// monthsAfterEpoch = 1398
		// yearsAfterEpoch = 116.5
		// targetYear = 2016
		// firstMonth = 6 (7��)
		int monthsAfterEpoch = position * MONTH_NUMBERS_PER_CHILDVIEW;
		int yearsAfterEpoch = monthsAfterEpoch / 12;
		int targetYear = YearsAdapter.EPOCH_YEAR + yearsAfterEpoch;		
		int firstMonth = monthsAfterEpoch % 12;
		firstMonthTime.set(targetYear, firstMonth, 1);
		//firstMonthTime.normalize(true);		
		
		if (position == 466) {
			int test = -1;
			test = 0;
		}
		
		return firstMonthTime;		
	}
	
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	//Log.i("tag", "MonthByWeekAdapter : onTouch");
    	boolean consumed = false;
    	
        if (!(v instanceof YearMonthTableLayout)) {
            return false;
        }
             
        
        //if (mGestureDetector.onTouchEvent(event)) {
        	
        	//mSingleTapUpView = (YearMonthTableLayout)v;
        	//mSingleTapUpView.invalidate();
        	//long delay = System.currentTimeMillis() - mClickTime;
    		// Make sure the animation is visible for at least mOnTapDelay - mOnDownDelay ms
    		//mListView.postDelayed(mDoSingleTapUp, delay > mTotalClickDelay ? 0 : mTotalClickDelay - delay);
        //}
        //else {
        
	        int action = event.getAction();   
	            
	        switch (action) {
	            case MotionEvent.ACTION_DOWN:   
	            	//if (INFO) Log.i(TAG, "ACTION_DOWN");
	                mClickedView = (YearMonthTableLayout)v;
	                mClickedXLocation = event.getX();
	                mClickedYLocation = event.getY();
	                mClickTime = System.currentTimeMillis();
	                int clickedMonthColumnIndex = 
	                		mClickedView.getClickedMonthColumnIndex((int)mClickedXLocation, (int)mClickedYLocation);
	                
	                if (clickedMonthColumnIndex != YearMonthTableLayout.INVALIDE_COLUMN_MONTH_INDEX) {
	                	mListView.post(mDoClickDown);
	                }
	                else {
	                	mClickedView = null;
	                }
	            	
	                break;
	            case MotionEvent.ACTION_UP:
	            	//if (INFO) Log.i(TAG, "ACTION_UP");
	            	if (mClickedView != null) {                		
	            		mListView.post(mDoClickUp);
	            		consumed = true;
	            	}
	            	break;
	            	
	            // g4���� down -> up �Ǵ� ���� �Ʒ��� �̺�Ʈ�� �ϳ�[MOVE]�� �߻��Ǿ� clearClickedView ȣ��� ����
	            // mClickedView�� null�� �Ǵ� �������� �߻��ߴ� :singleTapup���� ����� �ٲ�� �ϴ°�?
	            //case MotionEvent.ACTION_SCROLL:
	            	//if (INFO) Log.i(TAG, "ACTION_SCROLL");
	            	//clearClickedView((YearMonthTableLayout)v);
	            	//break;
	            //case MotionEvent.ACTION_MOVE:
	            	//if (INFO) Log.i(TAG, "ACTION_MOVE");            	
	                //clearClickedView((YearMonthTableLayout)v);
	                //break;
	            case MotionEvent.ACTION_CANCEL:
	            	//if (INFO) Log.i(TAG, "ACTION_CANCEL");
	            	clearClickedView((YearMonthTableLayout)v);
	            	break;
	            
	            
	            default:
	                break;
	        }
        //}
        
        // Do not tell the frameworks we consumed the touch action so that fling actions can be
        // processed by the fragment.
        return consumed;
    }
        
    AlphaAnimation mClickDownMonthAlphaAnimation;
    MiniMonthInYearRow mClickeMiniMonth;
    private final Runnable mDoClickDown = new Runnable() {
        @Override
        public void run() {
            if (mClickedView != null) {
                synchronized(mClickedView) {    
                	
                	//if (INFO) Log.i(TAG, "mDoClickDown");
                		
                	mClickDownMonthAlphaAnimation = new AlphaAnimation(1, 0.4f);
                	mClickDownMonthAlphaAnimation.setDuration(200);
                	mClickDownMonthAlphaAnimation.setFillAfter(true);
                	mClickDownMonthAlphaAnimation.setFillEnabled(true);
                	
                	mClickeMiniMonth = mClickedView.getClickedMonthView();
                	if (mClickeMiniMonth == null) {
                		mClickedView = null;
                		if (INFO) Log.i(TAG, "mDoClickDown:mClickedView = null");
                		return; 
                	}                	
                	
                	if (mYearViewExitAnimObject.mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {
                		Calendar clkMonth = mClickedView.getClickedMonthTime();  
                		Calendar month = GregorianCalendar.getInstance(clkMonth.getTimeZone());
                		month.setTimeInMillis(clkMonth.getTimeInMillis());
                		//// today�� �����ϸ� �ش���� 1���� �ƴ� today�� day of month�� �����ؾ� �Ѵ�
                		if (mToday.get(Calendar.YEAR) == month.get(Calendar.YEAR) && 
                				mToday.get(Calendar.MONTH) == month.get(Calendar.MONTH)) {
                			mYearViewExitAnimObject.startOneDayEventsLoad(ETime.getJulianDay(mToday.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
                		}
                		else
                			mYearViewExitAnimObject.startOneDayEventsLoad(ETime.getJulianDay(month.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
                	}
                	
                	mClickeMiniMonth.startAnimation(mClickDownMonthAlphaAnimation);
                }                
            }
        }
    };
    
    AlphaAnimation mRecoveryClickedMonthAlphaAnimationForEscape;
    long mRecoveryAnimationDurationForEscape = 100;
    private final Runnable mDoClickUp = new Runnable() {
        @Override
        public void run() {
            if (mClickedView != null) {
                synchronized(mClickedView) {                 	
                	mRecoveryClickedMonthAlphaAnimationForEscape = new AlphaAnimation(0.4f, 1);
                	mRecoveryClickedMonthAlphaAnimationForEscape.setDuration(mRecoveryAnimationDurationForEscape);
                	//mRecoveryClickedMonthAlphaAnimationForEscape.setFillAfter(true);
                	//mRecoveryClickedMonthAlphaAnimationForEscape.setFillEnabled(true);
                	
                	mRecoveryClickedMonthAlphaAnimationForEscape.setAnimationListener(listener);
                	mClickeMiniMonth.startAnimation(mRecoveryClickedMonthAlphaAnimationForEscape);  
                }                          
            }
        }
    };
       
    AnimationListener listener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mClickeMiniMonth.setVisibility(View.INVISIBLE);
						
			Calendar clkMonth = mClickedView.getClickedMonthTime();  
    		Calendar month = GregorianCalendar.getInstance(clkMonth.getTimeZone());
    		month.setTimeInMillis(clkMonth.getTimeInMillis());
        	
        	if (month != null) {                
        		goToMonthView(month);
            } 			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}
    	
    };
    
    private void clearClickedView(YearMonthTableLayout v) {
        mListView.removeCallbacks(mDoClickDown);
        synchronized(v) {
            v.clearClickedMonth();
        }        
        
        //if (INFO) Log.i(TAG, "clearClickedView");
        
        mClickedView = null;        
        
        if (mClickDownMonthAlphaAnimation != null) {
        	if (mClickDownMonthAlphaAnimation.getDuration() > 0) {
        		mClickDownMonthAlphaAnimation.setDuration(0);
        	}        	
        }        
        
        mListView.post(mRecoveryClickDowned);
    }
    
    
    AlphaAnimation mRecoveryClickDownedMonthAlphaAnimation;
    long mRecoveryAnimationDuration = 200;
    private final Runnable mRecoveryClickDowned = new Runnable() {
        @Override
        public void run() {
            if (mClickeMiniMonth != null) {
                synchronized(mClickeMiniMonth) {                	
                	mRecoveryClickDownedMonthAlphaAnimation = new AlphaAnimation(0.4f, 1);
                	mRecoveryClickDownedMonthAlphaAnimation.setDuration(mRecoveryAnimationDuration);
                	mRecoveryClickDownedMonthAlphaAnimation.setFillAfter(true);
                	mRecoveryClickDownedMonthAlphaAnimation.setFillEnabled(true);
                	
                	mClickeMiniMonth.startAnimation(mRecoveryClickDownedMonthAlphaAnimation);                	
                }                
            }
        }
    };
        
    public void goToMonthView(Calendar month) {   	    	
    	mYearViewExitAnimObject.init(mClickedView, month);
    	mYearViewExitAnimObject.startListViewScaleAnim();
    }
    
    float mListViewScaleRatio;
    float mListViewDisplayAspectRatio;    
    float mListViewPivotXAbsPx = 0; 
    float mListViewPivotX = 0;    
    float mListViewPivotYAbsPx = 0; 
    float mListViewPivotY = 0; 
    ScaleAnimation mListViewScaleAnimation;
    AlphaAnimation mListViewAlphaAnimation;
    AnimationSet mListViewAnimationSet;
    ScaleInterpolator mListViewScaleInterpolator;
    long mListViewScaleDuration = 5000;
    
    protected void onMonthTapped(Calendar month) {
    	//setMonthParameters(month);	       
    	
    	Log.i("tag", "selected month : " + ETime.format2445(month));
    	
    	//mClickeMiniMonth.setVisibility(View.INVISIBLE);
    	
    	mFragment.mCalendarViewsSecondaryActionBar.mDayHeaderLayout.setVisibility(View.VISIBLE);
    	mFragment.mCalendarViewsSecondaryActionBar.mDayHeaderView.setVisibility(View.VISIBLE);
    	
    	float width = mFragment.mAnticipationListViewWidth;    	
    	float height = mListView.getHeight();
    	mListViewDisplayAspectRatio = width / height;    	
    	float miniMonthHeight = mMiniMonthHeight;  
    	mListViewScaleRatio = height / miniMonthHeight;
    	float fromX = 1;
    	float toX = mListViewScaleRatio * mListViewDisplayAspectRatio;
    	float fromY = 1;
    	float toY = mListViewScaleRatio;
    	
    	int monthsPattern = mClickedView.getMonthsPattern();
    	int selectedMonthsViewTop = mClickedView.getTop();
    	int selectedMonthsViewBottom = mClickedView.getBottom();
    	
    	float availableUpperScaleRegion = 0;
    	float availableLowerScaleRegion = 0;
    	if (monthsPattern == YearMonthTableLayout.YEAR_INDICATOR_NORMAL_YEAR_MONTHS_PATTERN) {
    		availableUpperScaleRegion = selectedMonthsViewTop + mYearIndicatorHeight;
    	}
    	else {
    		availableUpperScaleRegion = selectedMonthsViewTop;
    	}
    	
    	availableLowerScaleRegion = height - selectedMonthsViewBottom;
    	// if availableUpperScaleRegion == 329,
    	//    availableLowerScaleRegion == 472 
    	//    : 329A + 472A = 236[mMiniMonthHeight]
    	//    : 801A = 236
    	//    : A = 236 / 801 = 0.2946
    	//    : 329 * A = 96.9338
    	//    : (329 + 96.9338) / 1038[ListView Height] = 425.9338 / 1038 = 0.41034
    	// *pivotY = 0.41034
    	float verticalA = 0;
    	
    	//float pivotYValue = 0;
    	if (availableLowerScaleRegion <= 0) {
    		availableLowerScaleRegion = 0; // �� ���� pivotY�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		height = selectedMonthsViewBottom; // height�� ��������� �Ѵ�    		
    	}   	
    	
    	verticalA = miniMonthHeight / (availableUpperScaleRegion + availableLowerScaleRegion);
    	mListViewPivotYAbsPx = availableUpperScaleRegion + (availableUpperScaleRegion * verticalA);
    	mListViewPivotY = mListViewPivotYAbsPx / height;    	
    	
    	float availableLeftScaleRegion = 0;
    	float availableRightScaleRegion = 0;
    	if ( (month.get(Calendar.MONTH) == 0) ||
    			(month.get(Calendar.MONTH) == 3) || 
    			(month.get(Calendar.MONTH) == 6) || 
    			(month.get(Calendar.MONTH) == 9) ) {
    		availableLeftScaleRegion = mMiniMonthLeftMargin; // �� ���� pivotX�� 0�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableRightScaleRegion = (mMiniMonthLeftMargin * 3) + (mMiniMonthWidth * 2);
    	}
    	else if ( (month.get(Calendar.MONTH) == 1) ||
    			(month.get(Calendar.MONTH) == 4) || 
    			(month.get(Calendar.MONTH) == 7) || 
    			(month.get(Calendar.MONTH) == 10) ) {
    		availableLeftScaleRegion = mMiniMonthWidth + (mMiniMonthLeftMargin * 2);
    		availableRightScaleRegion = (mMiniMonthLeftMargin *2) + mMiniMonthWidth;
    	}
    	else if ( (month.get(Calendar.MONTH) == 2) ||
    			(month.get(Calendar.MONTH) == 5) || 
    			(month.get(Calendar.MONTH) == 8) || 
    			(month.get(Calendar.MONTH) == 11) ) {
    		availableRightScaleRegion = mMiniMonthLeftMargin; // �� ���� pivotX�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableLeftScaleRegion = (mMiniMonthLeftMargin * 3) + (mMiniMonthWidth * 2);
    	}    	
    	
    	float horizontalA = mMiniMonthWidth / (availableLeftScaleRegion + availableRightScaleRegion);
    	mListViewPivotXAbsPx = availableLeftScaleRegion + (availableLeftScaleRegion * horizontalA);
    	mListViewPivotX = mListViewPivotXAbsPx / width;    	
    	
    	int pivotXType = Animation.RELATIVE_TO_SELF;
    	int pivotYType = Animation.ABSOLUTE;
    	//int pivotYType = Animation.RELATIVE_TO_SELF;
    	mListViewScaleAnimation = new ScaleAnimation(
    			fromX, toX, 
    			fromY, toY, 
    			pivotXType, mListViewPivotX, 
    			pivotYType, mListViewPivotYAbsPx);    	
    	
    	mListViewAlphaAnimation = new AlphaAnimation(1, 0.0f);
    	
    	mListViewAnimationSet = new AnimationSet(true);    	
    	mListViewAnimationSet.addAnimation(mListViewScaleAnimation);
    	mListViewAnimationSet.addAnimation(mListViewAlphaAnimation);
    	mListViewAnimationSet.setDuration(mListViewScaleDuration);
    	mListViewAnimationSet.setInterpolator(new DecelerateInterpolator());
    	mListViewAnimationSet.setFillAfter(true);
    	mListViewAnimationSet.setFillEnabled(true);
    	mListView.startAnimation(mListViewAnimationSet);
    }    
    
    private static class ScaleInterpolator implements Interpolator { 	
    	
        public float getInterpolation(float t) {            
        	// easing�� ���Ŀ� ������ ����
            return t;
        }        
    }
        
    public void onUpdateScale(YearListView view, float scale, LayoutParams params) {
        //params.leftMargin = (int) (view.getX() * scale);
        //params.topMargin = (int) (view.getY() * scale);
        params.width = (int) (view.getWidth() * scale);
        params.height = (int) (view.getHeight() * scale);
    }
    
    /*
    public void setMonthParameters(Calendar month) {
    	
    	//ETime.switchTimezone(month, mTimeZone);//month.timezone = mHomeTimeZone;    	
    	
    	Calendar currTime = GregorianCalendar.getInstance(mTimeZone);
        currTime.setTimeInMillis(mController.getTime());
        
        month.set(Calendar.HOUR_OF_DAY, currTime.get(Calendar.HOUR_OF_DAY));//month.hour = currTime.hour;
        month.set(Calendar.MINUTE, currTime.get(Calendar.MINUTE));//month.minute = currTime.minute;
        //month.allDay = false;
        
    }
	*/
	
    public class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }        
    }
	
	YearPickerFragment mFragment;
    YearListView mListView;
    int mListViewHeight = 0;
    int mCurrentMonthViewMode;
    public void setListView(YearPickerFragment fragment, YearListView lv) {
    	mFragment = fragment;
        mListView = lv;    
        
        mCurrentMonthViewMode = Utils.getSharedPreference(mContext, SettingsFragment.KEY_MONTH_VIEW_LAST_MODE, SettingsFragment.NORMAL_MONTH_VIEW_MODE);
        
        mYearViewExitAnimObject = new YearViewExitAnimObject(mContext, 
        		mFragment, 
    			this, 
    			mListView, 
    			mController, 
    			mTzId, 
    			mCurrentMonthViewMode);
    }    
    
    /*
    public void animateToday() {
        mAnimateToday = true;
        mAnimateTime = System.currentTimeMillis();
    }
	*/
}
