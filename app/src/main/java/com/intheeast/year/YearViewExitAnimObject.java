package com.intheeast.year;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import com.intheeast.day.PsudoMonthAdapter;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.EventLoader;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;
import com.intheeast.etc.CalendarViewExitAnim;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthWeekView;
import com.intheeast.month.MonthAdapter;
import com.intheeast.settings.SettingsFragment;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
//import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.RelativeLayout;

public class YearViewExitAnimObject extends CalendarViewExitAnim {

	private static final String TAG = "YearViewExitAnimObject";
    private static boolean INFO = true;
    
	Context mContext;
	YearPickerFragment mFragment;
	YearsAdapter mAdapter;
	YearListView mListView;
	YearMonthTableLayout mClickedMonthRowLayout;
	PsudoEventListView mPsudoMonthExpandModeEventsListView;
	CalendarController mController;
	Calendar mGoToMonthTime;
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	int mFirstDayOfWeek;
	
	PsudoMonthAdapter mPsudoMonthByWeekAdapter;
	PsudoMonthView mPsudoUpperMonthView;
	ScaleMiniMonthView mScaleTargetMiniMonthView;		
	PsudoMonthView mPsudoLowerMonthView;
	
	float mListViewDisplayAspectRatio;
	float mListViewScaleRatio;
	
	float mListViewPivotYAbsPx;
	float mListViewPivotY;
	float mListViewPivotXAbsPx;
	float mListViewPivotX;
		
	float mSelectedMiniMonthCurrentWidth;
	float mSelectedMiniMonthCurrentHeight;
	
	ScaleAnimation mListViewScaleAnimation;
	AlphaAnimation mListViewAlphaAnimation;
	AnimationSet mListViewAnimationSet;
	long mYearListViewScaleDuration = 300;
	long mTargetMonthScaleDuration = 600;
	//long mListViewScaleDuration = 10000;	
	//boolean mExpandEventViewMode = true;
	private EventLoader mOneDayEventLoader;    
    ArrayList<EventCursors> mOneDayEventCursors = new ArrayList<EventCursors>();
    int mCurrentMonthViewMode;
    Calendar mToday;
    public YearViewExitAnimObject(Context context, YearPickerFragment fragment, 
			YearsAdapter adapter, YearListView listview, CalendarController controller, String timeZoneStr, int mode) {
		mContext = context;
		mFragment = fragment;
		mAdapter = adapter;
		mListView = listview;		
		mController = controller;		
		mTzId = timeZoneStr;
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
		
		mToday = GregorianCalendar.getInstance(mTimeZone);
		mToday.setFirstDayOfWeek(mFirstDayOfWeek);
		mToday.setTimeInMillis(System.currentTimeMillis());
		
		mCurrentMonthViewMode = mode;
		
		mScaleTargetMiniMonthView = mFragment.mScaleTargetMiniMonthView;
		
		mPsudoMonthByWeekAdapter = new PsudoMonthAdapter(mContext);
				
		if (mCurrentMonthViewMode == SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE) {
			mOneDayEventLoader = new EventLoader(mContext);
			mOneDayEventLoader.startBackgroundThread();
		}
		
		mQueryCalendarDB = new QueryCalendarDB(mContext, mFragment, this, mTzId);		
	}	
    	
    int mMonthViewMonthIndicatorTextSize;
    int mMonthIndicatorTextColorAlpha;
	int mMonthIndicatorTextColorRed;
	int mMonthIndicatorTextColorGreen;
	int mMonthIndicatorTextColorBlue;
	
	int mMiniMonthIndicatorTextColorAlpha;
	int mMiniMonthIndicatorTextColorRed;
	int mMiniMonthIndicatorTextColorGreen;
	int mMiniMonthIndicatorTextColorBlue;
	
	//int mMonthIndicaotor;
	//int mMonthIndicaotorRedDiff;
	//int mMonthIndicaotorGreenDiff;
	//int mMonthIndicaotorBlueDiff;
	public void init(YearMonthTableLayout clickedView, Calendar month) {
		mClickedMonthRowLayout = clickedView;
				
		mTimeZone = month.getTimeZone();
		mTzId = mTimeZone.getID();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mGoToMonthTime = GregorianCalendar.getInstance(mTimeZone);//month;
		mGoToMonthTime.setTimeInMillis(month.getTimeInMillis());
		
		setMonthInfo();
		
		HashMap<String, Integer> weekParams = new HashMap<String, Integer>();           
        weekParams.put(MonthAdapter.WEEK_PARAMS_WEEK_START, mAdapter.mFirstDayOfWeek);        
        weekParams.put(MonthAdapter.WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED,
                ETime.getJulianDay(mGoToMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
        weekParams.put(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK, mAdapter.mDaysPerWeek);
		
		mPsudoMonthByWeekAdapter.updateParams(weekParams);	
		
		intQueryDB();
		
		mFragment.mListView.setVisibility(View.GONE);	
		mFragment.mCalendarViewsSecondaryActionBar.setVisibility(View.VISIBLE);		
		
		int miniMonthIndicatorTextColor = Color.RED;
		mMiniMonthIndicatorTextColorAlpha = Color.alpha(miniMonthIndicatorTextColor);
		mMiniMonthIndicatorTextColorRed = Color.red(miniMonthIndicatorTextColor);
		mMiniMonthIndicatorTextColorGreen = Color.green(miniMonthIndicatorTextColor);
		mMiniMonthIndicatorTextColorBlue = Color.blue(miniMonthIndicatorTextColor);
		
		int monthViewMonthIndicatorTextColor = mFragment.getResources().getColor(R.color.month_day_number);
		mMonthIndicatorTextColorAlpha = Color.alpha(monthViewMonthIndicatorTextColor);
		mMonthIndicatorTextColorRed = Color.red(monthViewMonthIndicatorTextColor);
		mMonthIndicatorTextColorGreen = Color.green(monthViewMonthIndicatorTextColor);
		mMonthIndicatorTextColorBlue = Color.blue(monthViewMonthIndicatorTextColor);
				
		//mMonthIndicaotorRedDiff = mMiniMonthIndicatorTextColorRed - mMonthIndicatorTextColorRed;
		//mMonthIndicaotorGreenDiff = mMiniMonthIndicatorTextColorGreen - mMonthIndicatorTextColorGreen;
		//mMonthIndicaotorBlueDiff = mMiniMonthIndicatorTextColorBlue - mMonthIndicatorTextColorBlue;
		
		mMonthViewMonthIndicatorTextSize = (int) (mFragment.mAnticipationMonthListViewHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT);
		
		// ���� ��带 Ȯ���ؼ� ��忡 �´� view���� �����ؾ� �Ѵ�
		if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {		
			
			makeNormalModePsudoTargetMonthView();
			
			makePsudoUpperAndLowerMonthViews(mContext);
			
			makeMiniMonthScaleAnimationToNormalModeTargetMonthView();			
		}
		else {		
			// listview�� �� item�� background�� ����� �ϳ���...�Ѥ�;
			//int bgColor = mContext.getResources().getColor(R.color.manageevent_actionbar_background);			
			//mFragment.mYearViewLayout.setBackgroundColor(bgColor);
			//mListView.setBackgroundColor(bgColor); // ���� �� �� �ʿ䰡 ����...�Ʒ� child�� background�� �����ϱ� ������
			/*
			int childCount = mListView.getChildCount();
			for (int i=0; i<childCount; i++) {
				View v = mListView.getChildAt(i);
				v.setBackgroundColor(bgColor);
			}
			*/
			// �� ������ ����� �θ���� ��׶��� ���� ������ ���������ν� �ذ��
			// �׷��ٸ� ���� �ڽĺ���� ��׶��尡 Ʈ�����۷�Ʈ�� �����Ǿ� �ִ� ���ΰ�?
			// :��׶��带 �������� �ʾҴ�...�׷��ٸ� ����Ʈ�� Ʈ�����۷�Ʈ��� ���ΰ�???
			//  �׷��� ����..�׸��� �θ���� ��׶��尡 ���� null�� �����Ǿ� �մ�
			makeExpandModePsudoTargetMonthView();
			
			makePsudoUpperAndLowerMonthViews(mContext);
			
			makeExpandModeEventsListView();
			
			makeMiniMonthScaleAnimationToExpandModeTargetMonthView();	
			
			// ������ �����ϸ� �ȵȴ�...
			//mListView.setBackgroundColor(Color.RED/*mEEVMBGColor*/);
		}
		
		makeYearListViewAnimation();
		
		mFragment.mUpperActionBar.setSwitchFragmentAnim(ViewType.MONTH, mGoToMonthTime.getTimeInMillis(), mTargetMonthScaleDuration, mCurrentMonthViewMode, mUpperActionBarSwitchFragmentAnimCompletionCallBack);
	}	
	
	int mUpperMonthWeekNumbers = 0;	
	int mLowerMonthWeekNumbers = 0;
	public void makePsudoUpperAndLowerMonthViews(Context context) {
		Calendar upperMonthTime = GregorianCalendar.getInstance(mGoToMonthTime.getTimeZone());
		upperMonthTime.setTimeInMillis(mGoToMonthTime.getTimeInMillis());
		upperMonthTime.add(Calendar.MONTH, -1);
		
		mUpperMonthWeekNumbers = ETime.getWeekNumbersOfMonth(upperMonthTime);
		
		ArrayList<String[]> upperMonthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(upperMonthTime, mTzId, mTimeZone, mAdapter.mFirstDayOfWeek);
				
		mPsudoUpperMonthView = mFragment.mPsudoUpperMonthView;
		
		mPsudoUpperMonthView.setMonthInfo(upperMonthTime, upperMonthWeekDayNumberList);	
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		Calendar lowerMonthTime = GregorianCalendar.getInstance(mGoToMonthTime.getTimeZone());
		lowerMonthTime.setTimeInMillis(mGoToMonthTime.getTimeInMillis());
		lowerMonthTime.add(Calendar.MONTH, 1);
		
		mLowerMonthWeekNumbers = ETime.getWeekNumbersOfMonth(lowerMonthTime);
		
		ArrayList<String[]> lowerMonthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(lowerMonthTime, mTzId, mTimeZone, mAdapter.mFirstDayOfWeek);
				
		mPsudoLowerMonthView = mFragment.mPsudoLowerMonthView;
		
		mPsudoLowerMonthView.setMonthInfo(lowerMonthTime, lowerMonthWeekDayNumberList);				
	}
	
	QueryCalendarDB mQueryCalendarDB;
	public void intQueryDB() {
		mQueryCalendarDB.initLoader(mGoToMonthTime);
		mQueryCalendarDB.launchEventLoading();		
	}	
	
	final ArrayList<Event> mOneDayEventsFromEventLoader = new ArrayList<Event>();
	public ArrayList<Event> mOneDayEvents = null; 
	long mLastReloadMillis = 0;
	// YearsAdapter.mDoClickDown���� ȣ��ȴ�...
	public void startOneDayEventsLoad(int firstJulianDay) {	
		Calendar weekStart = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		long mills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone, mFirstDayOfWeek);
		weekStart.setTimeInMillis(mills);//weekStart.setJulianDay(firstJulianDay);        
        weekStart.set(Calendar.HOUR_OF_DAY, 0);//.hour = 0;
        weekStart.set(Calendar.MINUTE, 0);//weekStart.minute = 0;
        weekStart.set(Calendar.SECOND, 0);//weekStart.second = 0;
        long millis = weekStart.getTimeInMillis();

        // Avoid reloading events unnecessarily.
        if (millis == mLastReloadMillis) {
            return;
        }
        mLastReloadMillis = millis;
		
		mOneDayEventsFromEventLoader.clear();
		
		if (mOneDayEvents != null)
			mOneDayEvents.clear();
		
		mOneDayEventLoader.loadEventsInBackground(1, 
				mOneDayEventsFromEventLoader, 
				firstJulianDay, 
				mCallBackOneDayEventsLoader, 
				mCancelCallback);		
	}
	
	boolean mOneDayEventsLoadedCompletion = false;
	private final Runnable mCallBackOneDayEventsLoader = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
            	mOneDayEvents = mOneDayEventsFromEventLoader;             	
            	
            	if (mCurrentEventListViewStatus == EVENTLISTVIEW_ALEADY_INITED_STATUS) {
            		            		
            		if (mOneDayEvents.size() != 0) {
            			int targetMonthFirstJulianDay;
    					if (mToday.get(Calendar.YEAR) == mGoToMonthTime.get(Calendar.YEAR) && 
                				mToday.get(Calendar.MONTH) == mGoToMonthTime.get(Calendar.MONTH)) {						
    						targetMonthFirstJulianDay = ETime.getJulianDay(mToday.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); 	
                		}
    					else {
    						targetMonthFirstJulianDay = ETime.getJulianDay(mGoToMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); 	
    					}					
    		    		
    		    		mEventItemsAdapter.setEvents(targetMonthFirstJulianDay, targetMonthFirstJulianDay, mOneDayEvents);
			    	}
			    	else {
			    		mEventItemsAdapter.setNonEvents();
			    	}	            		
            		
            		mPsudoEventListTransAnim.start();
	            	
	            	mCurrentEventListViewStatus = EVENTLISTVIEW_LAUNCHED_TRANSLATE_ANIMATION_STATUS;	            	
            	}
            	
            	mOneDayEventsLoadedCompletion = true;
            }
        }
    };
	
	private final Runnable mCancelCallback = new Runnable() {
        public void run() { 
        	clearCachedEvents();
        }
    };
    
    void clearCachedEvents() {
        mLastReloadMillis = 0;
    }
	
	float mScaleFromX;
	float mScaleToX;
	float mScaleFromY;
	float mScaleToY;
	
	public void makeYearListViewAnimation() {
				
		mFragment.mCalendarViewsSecondaryActionBar.mDayHeaderLayout.setVisibility(View.VISIBLE);
    	mFragment.mCalendarViewsSecondaryActionBar.mDayHeaderView.setVisibility(View.VISIBLE);
    	
    	float width = mFragment.mAnticipationListViewWidth;    	
    	float height = mListView.getHeight();
    	mListViewDisplayAspectRatio = width / height;    	
    	float miniMonthHeight = mAdapter.mMiniMonthHeight;  
    	mListViewScaleRatio = height / miniMonthHeight;
    	mScaleFromX = 1;
    	mScaleToX = mListViewScaleRatio * mListViewDisplayAspectRatio;
    	mScaleFromY = 1;
    	mScaleToY = mListViewScaleRatio;   
    	
    	int selectedMonthsViewTop = (int) (mClickedMonthRowLayout.getY() + mClickedMonthRowLayout.mMonthRow.getY());
    	int selectedMonthsViewBottom = mClickedMonthRowLayout.getBottom();
    	
    	float availableUpperScaleRegion = 0;
    	float availableLowerScaleRegion = 0;
    	
    	availableUpperScaleRegion = selectedMonthsViewTop;
    	    	
    	availableLowerScaleRegion = height - selectedMonthsViewBottom;
    	// makeMiniMonthScaleAnimationToNormalModeTargetMonthView�ʹ� �ٸ� ������� pivot Y�� ���ϰ� �ִ�
    	// :��� 329A + 472A = 236[mMiniMonthHeight] ���� �����Ǿ����� ����...�� õ���ΰ�? ����
    	//  �޸����� �ʾƼ� ��� �����Ǿ����� ������ �𸣰ڴ�...���� 
    	//  ��·�� makeMiniMonthScaleAnimationToNormalModeTargetMonthView�� ���� ������� �����Ѵ�
    	// if availableUpperScaleRegion == 329,
    	//    availableLowerScaleRegion == 472 
    	    	
    	//    : 329A + 472A = 236[mMiniMonthHeight]
    	//    : 801A = 236
    	//    : A = 236 / 801 = 0.2946
    	//    : 329 * A[0.2946] = 96.9338
    	//    : (329 + 96.9338) / 1038[ListView Height] = 425.9338 / 1038 = 0.41034
    	// *pivotY = 0.41034
    	float verticalA = 0;    	
    	
    	if (availableLowerScaleRegion < 0) {
    		availableLowerScaleRegion = 0; // �� ���� pivotY�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		height = selectedMonthsViewBottom; // height�� ��������� �Ѵ�    		
    	}   	
    	
    	verticalA = miniMonthHeight / (availableUpperScaleRegion + availableLowerScaleRegion);
    	mListViewPivotYAbsPx = availableUpperScaleRegion + (availableUpperScaleRegion * verticalA);
    	//mListViewPivotY = mListViewPivotYAbsPx / height; 	
    	
    	float availableLeftScaleRegion = 0;
    	float availableRightScaleRegion = 0;
    	
    	switch(mClickedMonthRowLayout.getValidClickedMonthColumnIndex()) {
		case YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX:
			availableLeftScaleRegion = mAdapter.mMiniMonthLeftMargin; // �� ���� pivotX�� 0�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableRightScaleRegion = (mAdapter.mMiniMonthLeftMargin * 3) + (mAdapter.mMiniMonthWidth * 2);
			break;
		case YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX:
			availableLeftScaleRegion = mAdapter.mMiniMonthWidth + (mAdapter.mMiniMonthLeftMargin * 2);
    		availableRightScaleRegion = (mAdapter.mMiniMonthLeftMargin *2) + mAdapter.mMiniMonthWidth;
			break;
		case YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX:
			availableRightScaleRegion = mAdapter.mMiniMonthLeftMargin; // �� ���� pivotX�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableLeftScaleRegion = (mAdapter.mMiniMonthLeftMargin * 3) + (mAdapter.mMiniMonthWidth * 2);
			break;
		}
    	    	
    	float horizontalA = mAdapter.mMiniMonthWidth / (availableLeftScaleRegion + availableRightScaleRegion);
    	mListViewPivotXAbsPx = availableLeftScaleRegion + (availableLeftScaleRegion * horizontalA);
    	mListViewPivotX = mListViewPivotXAbsPx / width;    	
    	
    	int pivotXType = Animation.RELATIVE_TO_SELF;
    	int pivotYType = Animation.ABSOLUTE;
    	//int pivotYType = Animation.RELATIVE_TO_SELF;
    	mListViewScaleAnimation = new ScaleAnimation(
    			mScaleFromX, mScaleToX, 
    			mScaleFromY, mScaleToY, 
    			pivotXType, mListViewPivotX, 
    			pivotYType, mListViewPivotYAbsPx);
    	
    	mListViewAlphaAnimation = new AlphaAnimation(1, 0.0f);
    	
    	mListViewAnimationSet = new AnimationSet(true);    	
    	mListViewAnimationSet.addAnimation(mListViewScaleAnimation);
    	mListViewAnimationSet.addAnimation(mListViewAlphaAnimation);
    	
    	//float xxx= 6500 * mListViewScaleRatio;
    	//mYearListViewScaleDuration = ITEAnimationUtils.calculateDuration(height, height, 3300, xxx);
    	//mYearListViewScaleDuration = 1000;
    	mListViewAnimationSet.setDuration(mYearListViewScaleDuration);
    	//ITEAnimInterpolator scaleAnimInterpolator = new ITEAnimInterpolator(height, mListViewAnimationSet);
    	//mListViewAnimationSet.setInterpolator(scaleAnimInterpolator);    	
		mListViewAnimationSet.setAnimationListener(mListViewAnimationSetListener);
    	mListViewAnimationSet.setFillAfter(true);
    	mListViewAnimationSet.setFillEnabled(true);   	
	}
	/*
	public void makeYearListViewAnimation() {
		
		mFragment.mCalendarViewsSecondaryActionBar.mDayHeaderLayout.setVisibility(View.VISIBLE);
    	mFragment.mCalendarViewsSecondaryActionBar.mDayHeaderView.setVisibility(View.VISIBLE);
    	
    	float width = mFragment.mAnticipationListViewWidth;    	
    	float height = mListView.getHeight();
    	mListViewDisplayAspectRatio = width / height;    	
    	float miniMonthHeight = mAdapter.mMiniMonthHeight;  
    	float miniMonthWidth = mAdapter.mMiniMonthWidth;
    	mListViewScaleRatio = height / miniMonthHeight;
    	mScaleFromX = 1;
    	mScaleToX = mListViewScaleRatio * mListViewDisplayAspectRatio;
    	mScaleFromY = 1;
    	mScaleToY = mListViewScaleRatio;   
    	
    	int selectedMonthsViewTop = (int) (mClickedMonthRowLayout.getY() + mClickedMonthRowLayout.mMonthRow.getY());
    	int selectedMonthsViewBottom = mClickedMonthRowLayout.getBottom();
    	
    	float availableUpperScaleRegion = 0;
    	float availableLowerScaleRegion = 0;
    	
    	availableUpperScaleRegion = selectedMonthsViewTop;
    	    	
    	availableLowerScaleRegion = height - selectedMonthsViewBottom;    	
    	if (availableLowerScaleRegion < 0) {
    		// �̷� ��Ȳ�� �߻��� �� �ִ°�???
    		availableLowerScaleRegion = 0; 	
    		height = selectedMonthsViewBottom;
    	}      	
    	
    	Y1 = (miniMonthHeight * availableUpperScaleRegion) / (height - miniMonthHeight);
    	mListViewPivotYAbsPx = availableUpperScaleRegion + Y1;    	
    	mListViewPivotY = mListViewPivotYAbsPx / height;    	
    	
    	float availableLeftScaleRegion = 0;    	
    	
    	switch(mClickedMonthRowLayout.getValidClickedMonthColumnIndex()) {
		case YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX:
			availableLeftScaleRegion = mAdapter.mMiniMonthLeftMargin; // �� ���� pivotX�� 0�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!    		
			break;
		case YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX:
			availableLeftScaleRegion = mAdapter.mMiniMonthWidth + (mAdapter.mMiniMonthLeftMargin * 2);    		
			break;
		case YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX:			
    		availableLeftScaleRegion = (mAdapter.mMiniMonthLeftMargin * 3) + (mAdapter.mMiniMonthWidth * 2);
			break;
		}    	    	
    	
    	X1 = (miniMonthWidth * availableLeftScaleRegion) / (width - miniMonthWidth);
    	mListViewPivotXAbsPx = availableLeftScaleRegion + X1;
    	mListViewPivotX = mListViewPivotXAbsPx / width;    	
    	
    	int pivotXType = Animation.RELATIVE_TO_SELF;
    	int pivotYType = Animation.ABSOLUTE;
    	
    	mListViewScaleAnimation = new ScaleAnimation(
    			mScaleFromX, mScaleToX, 
    			mScaleFromY, mScaleToY, 
    			pivotXType, mListViewPivotX, 
    			pivotYType, mListViewPivotYAbsPx);
    	
    	mListViewAlphaAnimation = new AlphaAnimation(1, 0.0f);
    	
    	mListViewAnimationSet = new AnimationSet(true);    	
    	mListViewAnimationSet.addAnimation(mListViewScaleAnimation);
    	mListViewAnimationSet.addAnimation(mListViewAlphaAnimation);
    	
    	//float xxx= 6500 * mListViewScaleRatio;
    	//mYearListViewScaleDuration = ITEAnimationUtils.calculateDuration(height, height, 3300, xxx);
    	//mYearListViewScaleDuration = 1000;
    	mListViewAnimationSet.setDuration(mYearListViewScaleDuration);
    	//ITEAnimInterpolator scaleAnimInterpolator = new ITEAnimInterpolator(height, mListViewAnimationSet);
    	//mListViewAnimationSet.setInterpolator(scaleAnimInterpolator);    	
		mListViewAnimationSet.setAnimationListener(mListViewAnimationSetListener);
    	mListViewAnimationSet.setFillAfter(true);
    	mListViewAnimationSet.setFillEnabled(true);   	
	}
	*/
	AnimationListener mListViewAnimationSetListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			mScaleTargetMiniMonthView.setVisibility(View.VISIBLE);
			if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE)
				mPsudoMonthListViewScaleAnimator.start();
			else {
				
				mEMPsudoMonthListViewScaleAnimator.start();
			}
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mListView.setVisibility(View.GONE); // listview�� gone ���°� �Ǹ� yearview_layout�� ������ �ȴ�...��???	
			                                    // :yearview_layout�� android:layout_height="wrap_content"�� �����߱� �����̴�
			                                    //  match_parent�� ������
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}
		
	};
	
	public void startListViewScaleAnim() {
		if (INFO) Log.i(TAG, "startListViewScaleAnim");		
		
		mListView.startAnimation(mListViewAnimationSet);	
		
		mFragment.mUpperActionBar.startSwitchFragmentAnim(mCurrentMonthViewMode);
	}
	
	HashMap<String, Integer> mMiniMonthDrawingParams;
	public void makeNormalModePsudoTargetMonthView() {		
		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mAdapter.mFirstDayOfWeek);  
		TempCalendar.setTimeInMillis(mGoToMonthTime.getTimeInMillis());	    		     	
    	TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    	mTargetMonthWeekNumbers = ETime.getWeekNumbersOfMonth(TempCalendar);
		int monthIndicatorHeight = (int) mFragment.mMonthListViewMonthIndicatorHeight;
		int normalWeekHeight = (int) mFragment.mMonthListViewNormalWeekHeight;		
		mTargetMonthHeight = monthIndicatorHeight + 
				(normalWeekHeight * mTargetMonthWeekNumbers);	
		
		RelativeLayout.LayoutParams psudoMonthListViewParams = new RelativeLayout.LayoutParams(mAdapter.mMiniMonthWidth, mAdapter.mMiniMonthHeight);	
		//int topMargin = (int) mContext.getResources().getDimension(R.dimen.day_header_height);
		//psudoMonthListViewParams.setMargins(0, topMargin, 0, 0);
		mScaleTargetMiniMonthView.setLayoutParams(psudoMonthListViewParams);
				
		mMiniMonthDrawingParams = new HashMap<String, Integer>();
		//
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE, YearsAdapter.MONTHVIEW_NORMAL_MODE);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH, mAdapter.mMonthListViewWidth);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT, mAdapter.mMonthListViewHeight);
		//mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_TOP_OFFSET, (int)mFragment.mAnticipationMonthListViewTopOffset);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mAdapter.mMiniMonthWidth);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mAdapter.mMiniMonthHeight);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mAdapter.mYearIndicatorHeight);			
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mAdapter.mYearIndicatorTextHeight);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mAdapter.mYearIndicatorTextBaseLineY);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, mAdapter.mMiniMonthIndicatorTextHeight);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, mAdapter.mMiniMonthIndicatorTextBaseLineY);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, mAdapter.mMiniMonthDayTextBaseLineY);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, mAdapter.mMiniMonthDayTextHeight);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT, mAdapter.mMonthListViewNormalWeekHeight);
		//mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT, mAdapter.mMonthListViewLastWeekHeight);
		
		mScaleTargetMiniMonthView.setMiniMonthDrawingParams(mMiniMonthDrawingParams);	
				
		mSelectedMiniMonthLeft = mClickedMonthRowLayout.getClickedMonthView().getX();		
		mSelectedMiniMonthTop = mClickedMonthRowLayout.getY() + mClickedMonthRowLayout.mMonthRow.getY();
		mScaleTargetMiniMonthView.setX(mSelectedMiniMonthLeft);
		mScaleTargetMiniMonthView.setY(mSelectedMiniMonthTop);		
	}
	
	
	public void setMonthInfo() {		
		int columnIndex = mClickedMonthRowLayout.getValidClickedMonthColumnIndex();
		ArrayList<String[]> goToMonthWeekDayNumberList = mClickedMonthRowLayout.getClickedMonthWeekDayNumberList();		
		ArrayList<String[]> goToPrvMonthWeekDayNumberList = null;
		ArrayList<String[]> goToNextMonthWeekDayNumberList = null;
		
		if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
			switch(columnIndex) {
			case YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX:
				Calendar prvMonthTime = GregorianCalendar.getInstance(mGoToMonthTime.getTimeZone());//new Time(mGoToMonthTime.timezone);
				prvMonthTime.setTimeInMillis(mGoToMonthTime.getTimeInMillis());
				prvMonthTime.add(Calendar.MONTH, -1);//prvMonthTime.month = prvMonthTime.month - 1;
				
				goToPrvMonthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(prvMonthTime, mTzId, mTimeZone, mAdapter.mFirstDayOfWeek);
				
				goToNextMonthWeekDayNumberList = mClickedMonthRowLayout.getMonthWeekDayNumberList(YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX);
				break;
			case YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX:
				goToPrvMonthWeekDayNumberList = mClickedMonthRowLayout.getMonthWeekDayNumberList(YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX);
				goToNextMonthWeekDayNumberList = mClickedMonthRowLayout.getMonthWeekDayNumberList(YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX);
				break;
			case YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX:
				goToPrvMonthWeekDayNumberList = mClickedMonthRowLayout.getMonthWeekDayNumberList(YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX);
				
				Calendar nextMonthTime = GregorianCalendar.getInstance(mGoToMonthTime.getTimeZone());//new Time(mGoToMonthTime.timezone);
				nextMonthTime.setTimeInMillis(mGoToMonthTime.getTimeInMillis());
				nextMonthTime.add(Calendar.MONTH, 1);//nextMonthTime.month = nextMonthTime.month + 1;
				
				goToNextMonthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(nextMonthTime, mTzId, mTimeZone, mAdapter.mFirstDayOfWeek);
				break;
			}
			
			mScaleTargetMiniMonthView.setNormalModeMonthInfo(mFragment.mAnticipationListViewWidth, mFragment.mMonthListViewMonthIndicatorTextHeight,
					mGoToMonthTime,
					goToMonthWeekDayNumberList,
					goToPrvMonthWeekDayNumberList, goToNextMonthWeekDayNumberList);
			
			
		}
		else {
			mScaleTargetMiniMonthView.setExpandEventListModeMonthInfo(mFragment.mAnticipationListViewWidth, mFragment.mMonthListViewMonthIndicatorTextHeightInEPMode,
					mGoToMonthTime, goToMonthWeekDayNumberList);
		}
	}	
	
	
	float mMonthListViewDisplayAspectRatio;
	float mMiniMonthViewScaleRatio;
	float mMiniMonthScaleFromX;
	float mMiniMonthScaleToX;
	float mMiniMonthScaleFromY;
	float mMiniMonthScaleToY;
	float mMiniMonthViewPivotXAbsPx;
	float mMiniMonthViewPivotYAbsPx;
	float mScaleAspectRatioPerMS;
	float mMiniMonthMarginTopAtScalePoint;
	boolean mChangeMonthListViewLayoutFormat = false;
	
	//float mScaleYAxisTotalRateChange;
		
	int mChangeMonthViewLayoutLimit;
	
	float mScaleUpMonthIndicatorHeight;
	float mScaleUpMonthNormalWeekHeight;
	
	ValueAnimator mPsudoMonthListViewScaleAnimator;
	float mMonthTextHeightScaleUpScalar;
	float mMonthTextBaseLineScaleUpScalar;
	float mMonthDayTextHeightScaleUpScalar;
	float mMonthDayTextBaseLineScaleDownRatio;
	float X1;
	float Y1;
	float mHeightOfRegionAbovePivotYInnerMiniMonth;
	float mRegionWidthLeftSidePivotXInnerMiniMonth;
	int mPrvScalingTargetMonthHeight = -1;
	int mPrvScalingTargetMonthWidth = -1;
	
	public void makeMiniMonthScaleAnimationToNormalModeTargetMonthView() {	
		
		float dayHeaderHeight = mContext.getResources().getDimension(R.dimen.day_header_height);		
		mMiniMonthMarginTopAtScalePoint = dayHeaderHeight;
		
		float availableUpperScaleRegion = 0; 
		float availableLowerScaleRegion = 0;
    	float availableLeftScaleRegion = 0;
    	float availableRightScaleRegion = 0;
    	
		mMiniMonthViewPivotXAbsPx = 0;
    	mMiniMonthViewPivotYAbsPx = 0;
    	
    	float miniMonthWidth = mAdapter.mMiniMonthWidth; 
    	float miniMonthHeight = mAdapter.mMiniMonthHeight;	
    	
		float width = mFragment.mAnticipationListViewWidth;      	
		float height = mFragment.mAnticipationMonthListViewHeight;
    	mMonthListViewDisplayAspectRatio = width / height; 
    	
    	float selectedMonthsViewTop = mSelectedMiniMonthTop;    	
    	int selectedMonthsViewBottom = mClickedMonthRowLayout.getBottom();
    	
    	availableUpperScaleRegion = selectedMonthsViewTop - mMiniMonthMarginTopAtScalePoint;    	
    	availableLowerScaleRegion = mFragment.mAnticipationListViewHeight - selectedMonthsViewBottom;
    	  
    	// *������ ���� month view�� day header layout[48size]�� �ִٴ� ���̴�
    	// if) month view height =1509,
    	//     mini month top = 848.0,
    	//     mini month view height = 354,
    	//     upper available increment size = 800,
    	//     lower available increment size = 355,
    	//     total increment size = 1509,
    	//     scale height ratio = 4.262712
    	//     mHeightOfRegionAbovePivotYInnerMiniMonth = 245.1948
    	//     mMiniMonthViewPivotYAbsPx = 1093.1948
    	//
    	// *mini month height�� 4.262712 scale �Ǿ��ٴ� ����,
    	//  mini month ���ο� ��ġ�� pivot Y ���� ����[mRegionHeightAbovePivotYInnerMiniMonth]�� �Ʒ� ������ ���� �� �ִٴ� ���� �����ȴ�
    	//  - ( availableUpperScaleRegion + mRegionHeightAbovePivotYInnerMiniMonth ) / mRegionHeightAbovePivotYInnerMiniMonth = scaleHeightRatio
    	//    mRegionHeightAbovePivotYInnerMiniMonth = availableUpperScaleRegion / (scaleHeightRatio - 1) 
    	//  :�� mRegionHeightAbovePivotYInnerMiniMonth�� 4.262712 scale �Ǿ �� scale�� ���� 
    	//   availableUpperScaleRegion + mRegionHeightAbovePivotYInnerMiniMonth �̶�� ���̴�
    	
    	float totalScaleHeight = availableUpperScaleRegion + miniMonthHeight + availableLowerScaleRegion;
    	float scaleHeightRatio = totalScaleHeight / miniMonthHeight; 
    	mHeightOfRegionAbovePivotYInnerMiniMonth = availableUpperScaleRegion / (scaleHeightRatio - 1);
    	mMiniMonthViewPivotYAbsPx = selectedMonthsViewTop + mHeightOfRegionAbovePivotYInnerMiniMonth;
    	        
    	switch(mClickedMonthRowLayout.getValidClickedMonthColumnIndex()) {
		case YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX:
			availableLeftScaleRegion = mAdapter.mMiniMonthLeftMargin; // �� ���� pivotX�� 0�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableRightScaleRegion = (mAdapter.mMiniMonthLeftMargin * 3) + (mAdapter.mMiniMonthWidth * 2);
			break;
		case YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX:
			availableLeftScaleRegion = mAdapter.mMiniMonthLeftMargin + mAdapter.mMiniMonthWidth + mAdapter.mMiniMonthLeftMargin;
    		availableRightScaleRegion = availableLeftScaleRegion;
			break;
		case YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX:
			availableRightScaleRegion = mAdapter.mMiniMonthLeftMargin; // �� ���� pivotX�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableLeftScaleRegion = (mAdapter.mMiniMonthLeftMargin * 3) + (mAdapter.mMiniMonthWidth * 2);
			break;
		}
                
        float totalScaleWidth = availableLeftScaleRegion + miniMonthWidth + availableRightScaleRegion;
        float scaleWidthRatio = totalScaleWidth / miniMonthWidth; 
        mRegionWidthLeftSidePivotXInnerMiniMonth = availableLeftScaleRegion / (scaleWidthRatio - 1);        
        mMiniMonthViewPivotXAbsPx = availableLeftScaleRegion + mRegionWidthLeftSidePivotXInnerMiniMonth;
    	    	
    	mMiniMonthViewScaleRatio = height / miniMonthHeight; 
    	
    	mMiniMonthScaleFromX = 1;    	
    	mMiniMonthScaleToX = width / miniMonthWidth;
    	
    	mMiniMonthScaleFromY = 1;
    	mMiniMonthScaleToY = mMiniMonthViewScaleRatio;     	   	    	
    			
		mSelectedMiniMonthCurrentWidth = mAdapter.mMiniMonthWidth;
		mSelectedMiniMonthCurrentHeight = miniMonthHeight;		
		
		mScaleUpMonthIndicatorHeight = (int) (mFragment.mMonthListViewMonthIndicatorHeight - mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY);
		mScaleUpMonthNormalWeekHeight = (int) (mFragment.mMonthListViewNormalWeekHeight - mFragment.mAnticipationMiniMonthDayTextBaseLineY);
		
		mMonthTextHeightScaleUpScalar = mFragment.mMonthListViewMonthIndicatorTextHeight - mFragment.mAnticipationMiniMonthIndicatorTextHeight;
		mMonthTextBaseLineScaleUpScalar = mFragment.mMonthListViewMonthIndicatorTextBaselineY - mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY;
		mMonthDayTextHeightScaleUpScalar =  mFragment.mMonthListMonthDayTextHeight - mFragment.mAnticipationMiniMonthDayTextHeight;
		
		mChangeMonthViewLayoutLimit = (int) (mFragment.mAnticipationMiniMonthHeight * 1.5f);
		
		//mScaleYAxisTotalRateChange = mMiniMonthScaleToY - mMiniMonthScaleFromY;
		
		mPsudoMonthListViewScaleAnimator = ValueAnimator.ofFloat(mMiniMonthScaleFromY, mMiniMonthScaleToY);		
		mPsudoMonthListViewScaleAnimator.setDuration(mTargetMonthScaleDuration);		
		mPsudoMonthListViewScaleAnimator.setInterpolator(new DecelerateInterpolator(1.5f));		
			
		mPsudoMonthListViewScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float fraction = valueAnimator.getAnimatedFraction();
				float scaleRatioYAxis = (Float) valueAnimator.getAnimatedValue();
				if (scaleRatioYAxis == mMiniMonthScaleFromY)
					return;			
				
				int height = (int) (mSelectedMiniMonthCurrentHeight * scaleRatioYAxis);			
				if (mPrvScalingTargetMonthHeight == height) {
					//if (INFO) Log.i(TAG, "mPrvScalingTargetMonthHeight == height");
					return;
				}
				
				mPrvScalingTargetMonthHeight = height;
				
				int width = (int) (height * mMonthListViewDisplayAspectRatio);				
				
				float diff = mMiniMonthScaleFromX - mMiniMonthScaleToX;
				float scaleRatioXAxis = mMiniMonthScaleFromX - (diff * fraction);
							
				float sy1 = mHeightOfRegionAbovePivotYInnerMiniMonth * scaleRatioYAxis;
				float sx1 = mRegionWidthLeftSidePivotXInnerMiniMonth * scaleRatioXAxis;				
								
				float newX = mMiniMonthViewPivotXAbsPx - sx1;
				float newY = mMiniMonthViewPivotYAbsPx - sy1;								
								
				mScaleTargetMiniMonthView.setX(newX);
				// mScaleTargetMiniMonthView�� top margin�� dayHeaderHeight�� �����Ͽ�����???
				mScaleTargetMiniMonthView.setY(newY);	
				
				RelativeLayout.LayoutParams psudoMonthListViewParams = new RelativeLayout.LayoutParams(width, height);					
				mScaleTargetMiniMonthView.setLayoutParams(psudoMonthListViewParams);	
				
				
					if (!mChangeMonthListViewLayoutFormat) {						
						mScaleTargetMiniMonthView.launchScaleDrawingMonthListView();
						mChangeMonthListViewLayoutFormat = true;		
						mPsudoUpperMonthView.setVisibility(View.VISIBLE);						
						mPsudoLowerMonthView.setVisibility(View.VISIBLE);
					}
								
					HashMap<String, Integer> monthDrawingParams = setScalingNMPsudoTargetMonthView(width, height, fraction);
					int monthIndicatorHeight = monthDrawingParams.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT);
					int monthNormalWeekHeight = monthDrawingParams.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT);
					int dateTextSize = monthDrawingParams.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);
					int viewAlpha = monthDrawingParams.get(ScaleMiniMonthView.PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE);
					
					mPsudoUpperMonthView.setDimension(width, monthIndicatorHeight, monthNormalWeekHeight, dateTextSize, viewAlpha);					
					mPsudoLowerMonthView.setDimension(width, monthIndicatorHeight, monthNormalWeekHeight, dateTextSize, viewAlpha);										
							
					int upperMonthViewHeight = monthIndicatorHeight + (monthNormalWeekHeight * mUpperMonthWeekNumbers);		
					int targetMonthViewHeight = monthIndicatorHeight + (monthNormalWeekHeight * mTargetMonthWeekNumbers);		
					int lowerMonthViewHeight = monthIndicatorHeight + (monthNormalWeekHeight * mLowerMonthWeekNumbers);		
										
					int psudoUpperMonthTop = (int) (newY - upperMonthViewHeight);
					int psudoLowerMonthTop = (int) (newY + targetMonthViewHeight);
					
					int upperMonthTopClip = (int) (upperMonthViewHeight - newY);
					mPsudoUpperMonthView.setClipRect(true, upperMonthTopClip, false, 0);
					int lowerMonthBottomClip = mFragment.mAnticipationListViewHeight - psudoLowerMonthTop;
					mPsudoLowerMonthView.setClipRect(false, 0, true, lowerMonthBottomClip);
					
					//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					mPsudoUpperMonthView.setX(newX);					
					mPsudoUpperMonthView.setY(psudoUpperMonthTop);
					
					RelativeLayout.LayoutParams psudoUpperMonthViewParams = new RelativeLayout.LayoutParams(width, upperMonthViewHeight);		
					mPsudoUpperMonthView.setLayoutParams(psudoUpperMonthViewParams);		
					
					/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					mPsudoLowerMonthView.setX(newX);					
					mPsudoLowerMonthView.setY(psudoLowerMonthTop);
					
					RelativeLayout.LayoutParams psudoLowerMonthViewParams = new RelativeLayout.LayoutParams(width, lowerMonthViewHeight);		
					mPsudoLowerMonthView.setLayoutParams(psudoLowerMonthViewParams);				
						
			}
		});
		
		mPsudoMonthListViewScaleAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {		
				notifyExitAnimCompletion(YEAR_EXIT_ANIM_COMPLETION);
				//int viewType = ViewType.MONTH;
				//mController.setTime(mGoToMonthTime.getTimeInMillis());
				//mController.sendEvent(mAdapter, EventType.GO_TO, mGoToMonthTime, null, -1, viewType);
			}

			@Override
			public void onAnimationStart(Animator animator) {			
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});		
	}
	
	int mLastMiniMonthScalingMiniMonthIndicatorTextBaseLineY;
	public void setScalingPsudoMiniMonthView(int width, int height, float animatedFraction) {
		/*
		 * target month�� month indicator�� �������� �̵��ؾ� �Ѵ�!!!
		 * �� miniMonthView�� size�� ������� �ڱ� �ڸ��� ã�� ������
		 * target month�� �ڱ� �ڸ��� minMonthView������ ã�ư��� �Ѵ�		
		 */
		
		
		HashMap<String, Integer> miniMonthDrawingParams = new HashMap<String, Integer>();		
		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);				
				
		int testA = mAdapter.mMiniMonthIndicatorTextHeight;
		int testB = (int) mFragment.mMonthListViewMonthIndicatorTextHeight;
		float monthTextHeightScaleScalar = mMonthTextHeightScaleUpScalar * animatedFraction;		
		int miniMonthIndicatorTextHeight = (int) (mAdapter.mMiniMonthIndicatorTextHeight + monthTextHeightScaleScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthIndicatorTextHeight);		
		
		float monthTextBaseLineScaleScalar = mMonthTextBaseLineScaleUpScalar * animatedFraction;
		mLastMiniMonthScalingMiniMonthIndicatorTextBaseLineY = (int) (mAdapter.mMiniMonthIndicatorTextBaseLineY + monthTextBaseLineScaleScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, mLastMiniMonthScalingMiniMonthIndicatorTextBaseLineY);			
		
		int miniMonthDayTextBaseLineY = (int) (mFragment.mAnticipationMiniMonthDayTextBaseLineY * animatedFraction);			
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthDayTextBaseLineY);	
		
		float miniMonthDayTextHeightScaleScalar = mMonthDayTextHeightScaleUpScalar * animatedFraction;
		int miniMonthDayTextHeight = (int) (mFragment.mAnticipationMiniMonthDayTextHeight + miniMonthDayTextHeightScaleScalar);		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, miniMonthDayTextHeight);		
		
		mScaleTargetMiniMonthView.setMiniMonthScaleDrawingParams(miniMonthDrawingParams);		
	}	
	
	public HashMap setScalingNMPsudoTargetMonthView(int width, int height, float animatedFraction) {
		/*
		 * target month�� month indicator�� �������� �̵��ؾ� �Ѵ�!!!
		 * �� miniMonthView�� size�� ������� �ڱ� �ڸ��� ã�� ������
		 * target month�� �ڱ� �ڸ��� minMonthView������ ã�ư��� �Ѵ�		
		 */			
		
		HashMap<String, Integer> monthDrawingParams = new HashMap<String, Integer>();		
		
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);		
		
		int monthIndicatorHeight = (int) (height * MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT, monthIndicatorHeight);		
			
		float monthTextBottomPadding = (float)(monthIndicatorHeight * (1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT)); // month indicator height�� 75%		
		float miniMonthIndicatorTextBaseLineY = monthIndicatorHeight - monthTextBottomPadding;//int miniMonthIndicatorTextBaseLineY = (int) (monthIndicatorHeight * MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, (int) miniMonthIndicatorTextBaseLineY);		
		
		int normalWeekHeight = (int) (height * MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT, normalWeekHeight);	
		
		int miniMonthDayTextBaseLineY = (int) (normalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);			
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthDayTextBaseLineY);	
		
		int eventCircleTopPadding = (int) (normalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_EVENT_CIRCLE_TOP_PADDING, eventCircleTopPadding);
				
		int monthDayTextHeight = (int) (height * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_SIZE_BY_MONTHLISTVIEW_OVERALL_HEIGHT);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, monthDayTextHeight);
				
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, mMonthViewMonthIndicatorTextSize);		
				
		int viewAlpha = (int) (255 * animatedFraction);
		if (viewAlpha < 102)
			viewAlpha = 102;
		monthDrawingParams.put(ScaleMiniMonthView.PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE, viewAlpha);
		
		int red = colorChange(mMiniMonthIndicatorTextColorRed, mMonthIndicatorTextColorRed, animatedFraction); 	
		int green = colorChange(mMiniMonthIndicatorTextColorGreen, mMonthIndicatorTextColorGreen, animatedFraction); 
		int blue = colorChange(mMiniMonthIndicatorTextColorBlue, mMonthIndicatorTextColorBlue, animatedFraction); 
		int monthIndicatorTextColor = Color.argb(mMonthIndicatorTextColorAlpha, red, green, blue);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHINDICATOR_TEXT_COLOR, monthIndicatorTextColor);
			
		mScaleTargetMiniMonthView.setMonthViewScaleDrawingParams(monthDrawingParams);		
		
		
				
		return monthDrawingParams;
	}	
	
	public int colorChange(int start, int end, float fraction) {
		int diff = start - end;
		int xColor = (int) (start - (diff * fraction));
		return xColor;
	}
	
	int mTargetMonthTop;
	int mTargetMonthHeight;
	int mTargetMonthWeekNumbers;
	int mEEVMBGColor;
	int mEEVMBGColor_RED;
	int mEEVMBGColor_GREEN;
	int mEEVMBGColor_BLUE;
	public void makeExpandModePsudoTargetMonthView() {		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mAdapter.mFirstDayOfWeek);  
		TempCalendar.setTimeInMillis(mGoToMonthTime.getTimeInMillis());	    		     	
    	TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    	mTargetMonthWeekNumbers = ETime.getWeekNumbersOfMonth(TempCalendar);//getWeekNumbersOfMonth(TempCalendar);
		int monthIndicatorHeight = (int) mFragment.mMonthListViewMonthIndicatorHeightInEPMode;
		int normalWeekHeight = (int) mFragment.mMonthListViewNormalWeekHeightInEPMode;
		//int lastWeekHeight = (int) mFragment.mMonthListViewLastWeekHeightInEPMode;
		int targetMonthHeight = monthIndicatorHeight + 
				(normalWeekHeight * mTargetMonthWeekNumbers);				
		
		mTargetMonthTop = (int) (mFragment.mDayHeaderHeight - mFragment.mMonthListViewMonthIndicatorHeightInEPMode);
		mTargetMonthHeight = targetMonthHeight;	
				
		RelativeLayout.LayoutParams psudoMonthListViewParams = new RelativeLayout.LayoutParams(mAdapter.mMiniMonthWidth, mAdapter.mMiniMonthHeight);		
		mScaleTargetMiniMonthView.setLayoutParams(psudoMonthListViewParams);
		
		mEEVMBGColor = mContext.getResources().getColor(R.color.manageevent_actionbar_background);         
        
        mEEVMBGColor_RED = Color.red(mEEVMBGColor);
        mEEVMBGColor_GREEN = Color.green(mEEVMBGColor);
        mEEVMBGColor_BLUE = Color.blue(mEEVMBGColor);
        //int bgColor = Color.argb(0, mEEVMBGColor_RED, mEEVMBGColor_GREEN, mEEVMBGColor_BLUE);
        //mScaleTargetMiniMonthView.setBackgroundColor(bgColor);
		
		mMiniMonthDrawingParams = new HashMap<String, Integer>();
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE, YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH, mFragment.mAnticipationListViewWidth);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT, mTargetMonthHeight);
		// expand mode������ ������ ������ �����Ѵ�
		//mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_TOP_OFFSET, (int)mFragment.mAnticipationMonthListViewTopOffset);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mAdapter.mMiniMonthWidth);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mAdapter.mMiniMonthHeight);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mAdapter.mYearIndicatorHeight);			
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mAdapter.mYearIndicatorTextHeight);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mAdapter.mYearIndicatorTextBaseLineY);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, mAdapter.mMiniMonthIndicatorTextHeight);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, mAdapter.mMiniMonthIndicatorTextBaseLineY);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, mAdapter.mMiniMonthDayTextBaseLineY);
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, mAdapter.mMiniMonthDayTextHeight);
		
		mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT, (int) mFragment.mMonthListViewNormalWeekHeightInEPMode);
		//mMiniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT, (int) mFragment.mMonthListViewLastWeekHeightInEPMode);
		
		mScaleTargetMiniMonthView.setMiniMonthDrawingParams(mMiniMonthDrawingParams);	
		
		/*ArrayList<String[]> goToMonthWeekDayNumberList = mClickedMonthRowLayout.getClickedMonthWeekDayNumberList();
		mPsudoMonthListView.setMonthInfo(mGoToMonthTime, goToMonthWeekDayNumberList);*/
		
		mScaleTargetMiniMonthView.setMonthListViewMode(ScaleMiniMonthView.PSUDO_MONTHLISTVIEW_EXPAND_EVENT_LIST_MODE);		
				
		mSelectedMiniMonthLeft = mClickedMonthRowLayout.getClickedMonthView().getX();
		mSelectedMiniMonthTop = mClickedMonthRowLayout.getY() + mClickedMonthRowLayout.mMonthRow.getY();
		mSelectedMiniMonthBottom = mSelectedMiniMonthTop + mFragment.mAnticipationMiniMonthHeight;
		mSelectedMiniMonthColumnIndex = mClickedMonthRowLayout.getValidClickedMonthColumnIndex();
		
		mScaleTargetMiniMonthView.setX(mSelectedMiniMonthLeft);
		mScaleTargetMiniMonthView.setY(mSelectedMiniMonthTop);
	}
	
	
	ValueAnimator mEMPsudoMonthListViewScaleAnimator;
	float mMonthTextHeightScaleUpRatioInEPMode;
	float mMonthTextBaseLineScaleUpRatioInEPMode;
	float mMonthDayTextHeightUpRatioInEPMode;
	float mMonthDayTextBaseLineScaleDownRatioInEPMode;
	
	float mSelectedMiniMonthLeft;
	float mSelectedMiniMonthTop;
	float mSelectedMiniMonthBottom;
	int mSelectedMiniMonthColumnIndex;
	
	float mMiniMonthRatio;
	
	//float mScaleXAxisTotalRateChange;
	//float scaleRatioYAxisRateInEPMode;
	
	public void makeMiniMonthDimension() {
		mSelectedMiniMonthLeft = mClickedMonthRowLayout.getClickedMonthView().getX();
		mSelectedMiniMonthTop = mClickedMonthRowLayout.getY() + mClickedMonthRowLayout.mMonthRow.getY();
		mSelectedMiniMonthBottom = mSelectedMiniMonthTop + mFragment.mAnticipationMiniMonthHeight;
		mSelectedMiniMonthColumnIndex = mClickedMonthRowLayout.getValidClickedMonthColumnIndex();
	}
	
	public void makeMiniMonthScaleAnimationToExpandModeTargetMonthView() {	
		    	
    	float miniMonthWidth = mFragment.mAnticipationMiniMonthWidth; 
    	float miniMonthHeight = mFragment.mAnticipationMiniMonthHeight;	
    	
		float width = mFragment.mAnticipationMonthListViewWidth;    	
    	float height = mTargetMonthHeight;
    	mMonthListViewDisplayAspectRatio = width / height; 
    	
    	mMiniMonthViewScaleRatio = height / miniMonthHeight; 
    	    	
    	mMiniMonthScaleFromX = 1;    	
    	mMiniMonthScaleToX = width / miniMonthWidth;
    	    	
    	mMiniMonthScaleFromY = 1;
    	mMiniMonthScaleToY = mMiniMonthViewScaleRatio;     	   	    	
    	    					
		mMiniMonthRatio = miniMonthWidth / miniMonthHeight;
		
		//int normalWeekHeight = mFragment.mMonthListViewNormalWeekHeightInEPMode;
		mScaleUpMonthIndicatorHeight = mFragment.mMonthListViewMonthIndicatorHeightInEPMode - mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY;
		mScaleUpMonthNormalWeekHeight = mFragment.mMonthListViewNormalWeekHeightInEPMode - mFragment.mAnticipationMiniMonthDayTextBaseLineY;
		
		mMonthTextHeightScaleUpScalar = mFragment.mMonthListViewMonthIndicatorTextHeightInEPMode - mFragment.mAnticipationMiniMonthIndicatorTextHeight;
		mMonthTextBaseLineScaleUpScalar = mFragment.mMonthListViewMonthIndicatorTextBaselineYInEPMode - mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY;
		mMonthDayTextHeightScaleUpScalar = mFragment.mMonthListMonthDayTextHeightInEPMode - mFragment.mAnticipationMiniMonthDayTextHeight;		
		
		// �߸��� �� ����
		// expand mode�� normal week�� height�� Ȯ���� ����
		mChangeMonthViewLayoutLimit = mFragment.mAnticipationMiniMonthHeight * 2;
				
		mEMPsudoMonthListViewScaleAnimator = ValueAnimator.ofFloat(mMiniMonthScaleFromY, mMiniMonthScaleToY);
		mEMPsudoMonthListViewScaleAnimator.setDuration(mTargetMonthScaleDuration);		
		mEMPsudoMonthListViewScaleAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
		
		// makeMiniMonthScaleAnimationToNormalModeTargetMonthView�ʹ� �� �ٸ� scale ����� ����ϰ� �ִ�
		// :1�� �Լ��� �̿��Ͽ� mini month�� �̵� / Ȯ���ϰ� �ִ�
		makeLinearFuncitonForTransmation(miniMonthWidth, miniMonthHeight);
		
		mEMPsudoMonthListViewScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float fraction = valueAnimator.getAnimatedFraction();
				float scaleRatioYAxis = (Float) valueAnimator.getAnimatedValue();
				if (scaleRatioYAxis == mMiniMonthScaleFromY) {
					// ���ʿ��ϰ� ������ �� �ʿ䰡 ����
					return;					
				}
							
				int height = (int) (mFragment.mAnticipationMiniMonthHeight * scaleRatioYAxis);
				if (mPrvScalingTargetMonthHeight == height) {					
					return;
				}
				
				mPrvScalingTargetMonthHeight = height;
				
				int width = (int) (height * mMonthListViewDisplayAspectRatio);
				
				float newCenterX = 0;
				float newCenterY = 0;
				
				if (mXValueScalarType == X_CONSTANT_DECREMENT) { // selected mini month : 3/6/9/11
					                                             // selected mini month�� �������� �̵��ϸ鼭 Ȯ��ȴ�
					newCenterX = mMiniTargetMonthCenterX - (mXIncrementAmountOfLinearFuncitonForTransmation * fraction);
					// y = ax + b
					newCenterY = (mSlopeOfLinearFuncitonForTransmation * newCenterX) + mInterceptOfLinearFuncitonForTransmation;
				} 
				else if (mXValueScalarType == X_CONSTANT_INCREMENT) { // selected mini month : 1/4/7/10, 2/5/8/11
					                                                  // selected mini month�� ���������� �̵��ϸ鼭 Ȯ��ȴ�
					newCenterX = mMiniTargetMonthCenterX + (mXIncrementAmountOfLinearFuncitonForTransmation * fraction);
					// y = ax + b
					newCenterY = (mSlopeOfLinearFuncitonForTransmation * newCenterX) + mInterceptOfLinearFuncitonForTransmation;
				}
				else { // �̷� ��찡 �߻��� �� �ִ°�? : mini month�� �ڽ��� ��ġ�� ���� ���� ���� x ��ǥ�� �׻� �����Ǿ� �ֱ� ������ �� �� ���ǹ��� ��� ���Ե� �� ������...					
					newCenterX = mMiniTargetMonthCenterX;
					if (mYValueScalarType == Y_CONSTANT_DECREMENT) {						
						newCenterY = mMiniTargetMonthCenterY - (mYIncrementAmountOfLinearFuncitonForTransmation * fraction);
					} 
					else if (mYValueScalarType == Y_CONSTANT_INCREMENT) {						
						newCenterY = mMiniTargetMonthCenterY + (mYIncrementAmountOfLinearFuncitonForTransmation * fraction);
					}
					else {						
						newCenterY = mMiniTargetMonthCenterY;					
					}					
				}			
												
				float newLeft = newCenterX - (width / 2);				
				float scaleTargetMonthViewTop = newCenterY - (height / 2);				
				
				mScaleTargetMiniMonthView.setX(newLeft);
				mScaleTargetMiniMonthView.setY(scaleTargetMonthViewTop);
				RelativeLayout.LayoutParams psudoMonthListViewParams = new RelativeLayout.LayoutParams(width, height);					
				mScaleTargetMiniMonthView.setLayoutParams(psudoMonthListViewParams);
				
				//if (height < mChangeMonthViewLayoutLimit) {
					//setScalingEMPsudoMiniMonthView(scaleRatioXAxis, scaleRatioYAxis, width, height);										
				//}
				//else {
					if (!mChangeMonthListViewLayoutFormat) {
						
						mScaleTargetMiniMonthView.launchScaleDrawingMonthListView();
						mChangeMonthListViewLayoutFormat = true;	
						
						mPsudoUpperMonthView.setVisibility(View.VISIBLE);						
						mPsudoLowerMonthView.setVisibility(View.VISIBLE);
					}					
					
					HashMap<String, Integer> monthDrawingParams = setScalingEMPsudoTargetMonthView(width, height, fraction);	
					int monthIndicatorHeight = monthDrawingParams.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT);
					int monthNormalWeekHeight = monthDrawingParams.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT);
					int dateTextSize = monthDrawingParams.get(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT);
					int viewAlpha = monthDrawingParams.get(ScaleMiniMonthView.PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE);
					
					int bgColor = Color.argb(viewAlpha, mEEVMBGColor_RED, mEEVMBGColor_GREEN, mEEVMBGColor_BLUE);
					//mScaleTargetMiniMonthView.setBackgroundColor(bgColor);
					//mPsudoUpperMonthView.setBackgroundColor(bgColor);
					//mPsudoLowerMonthView.setBackgroundColor(bgColor);
					//mListView.setBackgroundColor(bgColor);
					mFragment.mYearViewLayout.setBackgroundColor(bgColor);
					
					mPsudoUpperMonthView.setDimension(width, monthIndicatorHeight, monthNormalWeekHeight, dateTextSize, viewAlpha);					
					mPsudoLowerMonthView.setDimension(width, monthIndicatorHeight, monthNormalWeekHeight, dateTextSize, viewAlpha);										
							
					int upperMonthViewHeight = monthIndicatorHeight + (monthNormalWeekHeight * mUpperMonthWeekNumbers);		
					int targetMonthViewHeight = monthIndicatorHeight + (monthNormalWeekHeight * mTargetMonthWeekNumbers);		
					int lowerMonthViewHeight = monthIndicatorHeight + (monthNormalWeekHeight * mLowerMonthWeekNumbers);		
										
					int psudoUpperMonthTop = (int) (scaleTargetMonthViewTop - upperMonthViewHeight);
					int psudoLowerMonthTop = (int) (scaleTargetMonthViewTop + targetMonthViewHeight);
					
					int upperMonthTopClip = (int) (upperMonthViewHeight - scaleTargetMonthViewTop);
					mPsudoUpperMonthView.setClipRect(true, upperMonthTopClip, false, 0);
										
					int psudoMonthExpandModeEventsListViewTop = (int) mPsudoMonthExpandModeEventsListView.getY();					
					int lowerMonthBottomClip = mFragment.mAnticipationListViewHeight - psudoLowerMonthTop - (mFragment.mAnticipationListViewHeight - psudoMonthExpandModeEventsListViewTop) ;
					mPsudoLowerMonthView.setClipRect(false, 0, true, lowerMonthBottomClip);
					
					//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					mPsudoUpperMonthView.setX(newLeft);					
					mPsudoUpperMonthView.setY(psudoUpperMonthTop);
					
					RelativeLayout.LayoutParams psudoUpperMonthViewParams = new RelativeLayout.LayoutParams(width, upperMonthViewHeight);		
					mPsudoUpperMonthView.setLayoutParams(psudoUpperMonthViewParams);
					
					/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					mPsudoLowerMonthView.setX(newLeft);					
					mPsudoLowerMonthView.setY(psudoLowerMonthTop);
					
					RelativeLayout.LayoutParams psudoLowerMonthViewParams = new RelativeLayout.LayoutParams(width, lowerMonthViewHeight);		
					mPsudoLowerMonthView.setLayoutParams(psudoLowerMonthViewParams);							
				//}					
			}
		});
		mEMPsudoMonthListViewScaleAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {	
				notifyExitAnimCompletion(YEAR_EXIT_ANIM_COMPLETION);
				//int viewType = ViewType.MONTH;
				//mController.sendEvent(mAdapter, EventType.GO_TO, mGoToMonthTime, null, -1, viewType);

			}

			@Override
			public void onAnimationStart(Animator animator) {
				synchronized(mCallBackOneDayEventsLoader) {
					if (mCurrentEventListViewStatus == EVENTLISTVIEW_CONFIG_LAYOUT_COMPLETION_STATUS) {
						Log.i("tag", "mEMPsudoMonthListViewScaleAnimator:eventlist start anim");											
						mPsudoEventListTransAnim.start();
					}
					else {
						Log.i("tag", "mEMPsudoMonthListViewScaleAnimator:NONE eventlist start anim");
					}
				}
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});	
	}
	
	
	
	public void setScalingEMPsudoMiniMonthView(int width, int height, float animatedFraction) {		
		
		
		HashMap<String, Integer> miniMonthDrawingParams = new HashMap<String, Integer>();		
		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);		
				
		float monthTextHeightScaleScalar = mMonthTextHeightScaleUpScalar * animatedFraction;		
		int miniMonthIndicatorTextHeight = (int) (mAdapter.mMiniMonthIndicatorTextHeight + monthTextHeightScaleScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthIndicatorTextHeight);
		
		float monthTextBaseLineScaleScalar = mMonthTextBaseLineScaleUpScalar * animatedFraction;
		int miniMonthScalingMiniMonthIndicatorTextBaseLineY = (int) (mAdapter.mMiniMonthIndicatorTextBaseLineY + monthTextBaseLineScaleScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, miniMonthScalingMiniMonthIndicatorTextBaseLineY);
		
		float miniMonthDayTextHeightScaleScalar = mMonthDayTextHeightScaleUpScalar * animatedFraction;
		int miniMonthDayTextHeight = (int) (mFragment.mAnticipationMiniMonthDayTextHeight + miniMonthDayTextHeightScaleScalar);	
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, miniMonthDayTextHeight);
		
		int miniMonthDayTextBaseLineY = (int) (mFragment.mAnticipationMiniMonthDayTextBaseLineY * animatedFraction);			
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthDayTextBaseLineY);		
		
		mScaleTargetMiniMonthView.setMiniMonthScaleDrawingParams(miniMonthDrawingParams);
	}
	
	public HashMap setScalingEMPsudoTargetMonthView(int width, int height, float animatedFraction) {	
		
		HashMap<String, Integer> monthDrawingParams = new HashMap<String, Integer>();		
		
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);		
		
		float monthIndicatorHeightScaleUpScalar = mScaleUpMonthIndicatorHeight * animatedFraction;
		int monthIndicatorHeight = (int) (mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY + monthIndicatorHeightScaleUpScalar);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT, monthIndicatorHeight);			
		
		float monthTextHeightScaleScalar = mMonthTextHeightScaleUpScalar * animatedFraction;		
		int miniMonthIndicatorTextHeight = (int) (mAdapter.mMiniMonthIndicatorTextHeight + monthTextHeightScaleScalar);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthIndicatorTextHeight);		
		
		float monthTextBottomPadding = (float)(monthIndicatorHeight * (1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT));		
		float miniMonthIndicatorTextBaseLineY = monthIndicatorHeight - monthTextBottomPadding;
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, (int) miniMonthIndicatorTextBaseLineY);		
		
		float normalWeekHeightScaleUpScalar = mScaleUpMonthNormalWeekHeight * animatedFraction;
		int normalWeekHeight = (int) (mFragment.mAnticipationMiniMonthDayTextBaseLineY + normalWeekHeightScaleUpScalar);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT, normalWeekHeight);		
		
		int miniMonthDayTextBaseLineY = (int) (normalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);			
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthDayTextBaseLineY);	
		
		int eventCircleTopPadding = (int) (normalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_EVENT_CIRCLE_TOP_PADDING, eventCircleTopPadding);
		
		float miniMonthDayTextHeightScaleScalar = mMonthDayTextHeightScaleUpScalar * animatedFraction;
		int miniMonthDayTextHeight = (int) (mFragment.mAnticipationMiniMonthDayTextHeight + miniMonthDayTextHeightScaleScalar);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, miniMonthDayTextHeight);
		
		int viewAlpha = (int) (255 * animatedFraction);		
		monthDrawingParams.put(ScaleMiniMonthView.PSUDO_MINIMONTH_PARAMS_ALPHA_VALUE, viewAlpha);		
		
		int red = colorChange(mMiniMonthIndicatorTextColorRed, mMonthIndicatorTextColorRed, animatedFraction); 	
		int green = colorChange(mMiniMonthIndicatorTextColorGreen, mMonthIndicatorTextColorGreen, animatedFraction); 
		int blue = colorChange(mMiniMonthIndicatorTextColorBlue, mMonthIndicatorTextColorBlue, animatedFraction); 
		int monthIndicatorTextColor = Color.argb(mMonthIndicatorTextColorAlpha, red, green, blue);
		monthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHINDICATOR_TEXT_COLOR, monthIndicatorTextColor);
				
		if (animatedFraction == 1) {
			if (INFO) Log.i(TAG, "width=" + String.valueOf(width));
			if (INFO) Log.i(TAG, "height=" + String.valueOf(height));
			if (INFO) Log.i(TAG, "monthIndicatorHeight=" + String.valueOf(monthIndicatorHeight));
			if (INFO) Log.i(TAG, "miniMonthIndicatorTextHeight=" + String.valueOf(miniMonthIndicatorTextHeight));
			if (INFO) Log.i(TAG, "miniMonthIndicatorTextBaseLineY=" + String.valueOf(miniMonthIndicatorTextBaseLineY));
			if (INFO) Log.i(TAG, "normalWeekHeight=" + String.valueOf(normalWeekHeight));
			if (INFO) Log.i(TAG, "miniMonthDayTextBaseLineY=" + String.valueOf(miniMonthDayTextBaseLineY));
			if (INFO) Log.i(TAG, "eventCircleTopPadding=" + String.valueOf(eventCircleTopPadding));
			if (INFO) Log.i(TAG, "miniMonthDayTextHeight=" + String.valueOf(miniMonthDayTextHeight));			
		}
		
		mScaleTargetMiniMonthView.setMonthViewScaleDrawingParams(monthDrawingParams);
		
		return monthDrawingParams;
		
	}	
	
	public static final int NO_X_CONTANT_RATE = 0;
	public static final int X_CONSTANT_INCREMENT = 1;
	public static final int X_CONSTANT_DECREMENT = 2;
	
	public static final int NO_Y_CONTANT_RATE = 0;
	public static final int Y_CONSTANT_INCREMENT = 1;
	public static final int Y_CONSTANT_DECREMENT = 2;
	float mSlopeOfLinearFuncitonForTransmation;
	float mInterceptOfLinearFuncitonForTransmation;
	
	float mMiniTargetMonthCenterX;
	float mMiniTargetMonthCenterY;
	
	float mPsudoTargetMonthCenterX;
	float mPsudoTargetMonthCenterY;
	
	float mXIncrementAmountOfLinearFuncitonForTransmation;
	float mYIncrementAmountOfLinearFuncitonForTransmation;
	
	int mXValueScalarType;
	int mYValueScalarType;
	/*
	 y = ax + b
	 a : slope
	 b : intercept
	 */
	public void makeLinearFuncitonForTransmation(float miniMonthWidth, float miniMonthHeight) {		
		mMiniTargetMonthCenterX = mSelectedMiniMonthLeft + (miniMonthWidth / 2);
		// psudoTargetMonth�� top margin�� �������� y������ �̵��Ѵ�
		// �׷��� psudoMiniTargetMonth�� top margin�� �������� �ʾ����Ƿ� ��������� �Ѵ�
		mMiniTargetMonthCenterY = mSelectedMiniMonthTop + (miniMonthHeight / 2) - mFragment.mDayHeaderHeight;
		
		mPsudoTargetMonthCenterX = mFragment.mListView.getWidth() / 2;
		mPsudoTargetMonthCenterY = mTargetMonthTop + (mTargetMonthHeight / 2);
		
		float incrementX = mMiniTargetMonthCenterX - mPsudoTargetMonthCenterX;
		float incrementY = mMiniTargetMonthCenterY - mPsudoTargetMonthCenterY;
		mXIncrementAmountOfLinearFuncitonForTransmation = Math.abs(incrementX);
		mYIncrementAmountOfLinearFuncitonForTransmation = Math.abs(incrementY);
		
		// y = ax + b ���� a�� ���ϴ� ���̴�
		mSlopeOfLinearFuncitonForTransmation = incrementY / incrementX;
		// y = ax + b ���� b�� ���ϴ� ���̴�
		// y - ax = b
		// b = y - ax
		mInterceptOfLinearFuncitonForTransmation = mPsudoTargetMonthCenterY - (mSlopeOfLinearFuncitonForTransmation * mPsudoTargetMonthCenterX);
		
		if (incrementX > 0) {
			mXValueScalarType = X_CONSTANT_DECREMENT; // selected mini month : 3/6/9/11
		}
		else if (incrementX < 0){ 
			mXValueScalarType = X_CONSTANT_INCREMENT;  // selected mini month : 1/4/7/10, 2/5/8/11
		}
		else {
			mXValueScalarType = NO_X_CONTANT_RATE; // �̷� ��찡 �߻��� �� �ִ°�? : mini month ���� x ��ǥ�� �׻� �����Ǿ� �ֱ� ������... 
		}
			
		if (incrementY > 0) {
			mYValueScalarType = Y_CONSTANT_DECREMENT;
		}
		else if (incrementY < 0) { 
			mYValueScalarType = Y_CONSTANT_INCREMENT;
		}
		else {
			mYValueScalarType = NO_Y_CONTANT_RATE;
		}	
	}		
		
	public ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
    public ArrayList<Event> mEvents = null;  
	int mFirstLoadedJulianDay;
	int mLastLoadedJulianDay;
	int mQueryDays;
	@Override	
	public void setEvents(int firstJulianDay, int lastJulianDay, int numDays, ArrayList<Event> events) {
        
        mEvents = events;
        mFirstLoadedJulianDay = firstJulianDay;
        mLastLoadedJulianDay = lastJulianDay;
        mQueryDays = numDays;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        if (events == null || events.size() == 0) {            
            mEventDayList = eventDayList;            
            return;
        }

        // Compute the new set of days with events
        for (Event event : events) {
            int startDay = event.startDay - mFirstLoadedJulianDay;
            int endDay = event.endDay - mFirstLoadedJulianDay + 1;
            if (startDay < numDays || endDay >= 0) {
                if (startDay < 0) {
                    startDay = 0;
                }
                if (startDay > numDays) {
                    continue;
                }
                if (endDay < 0) {
                    continue;
                }
                if (endDay > numDays) {
                    endDay = numDays;
                }
                for (int j = startDay; j < endDay; j++) {
                    eventDayList.get(j).add(event);
                }
            }
        }
        
        mEventDayList = eventDayList;         
        
        sendEventsToView(mScaleTargetMiniMonthView);
        
        sendEventsToView(mPsudoUpperMonthView);
        
        sendEventsToView(mPsudoLowerMonthView);
        
        mFragment.mApp.setFirstLoadedJulianDay(mFirstLoadedJulianDay);
        mFragment.mApp.setLastLoadedJulianDay(mLastLoadedJulianDay);
        mFragment.mApp.setEvents(mEvents);
        
    } 
		
	public void sendEventsToView(ScaleMiniMonthView v) {
        if (mEventDayList.size() == 0) {            
            v.setEvents(null, null);
            return;
        }
        
        int viewJulianDay = v.getFirstJulianDay();                
        int start = viewJulianDay - mFirstLoadedJulianDay;
        //int start = 0;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
        
        Log.i("tag", "sendEventsToView");
    }
	
	public void sendEventsToView(PsudoMonthView v) {
        if (mEventDayList.size() == 0) {            
            v.setEvents(null, null);
            return;
        }        
        
        int viewJulianDay = v.getFirstJulianDay();                
        int start = viewJulianDay - mFirstLoadedJulianDay;
        //int start = 0;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
        
        Log.i("tag", "sendEventsToView");
    }
	
	
		
	PsudoEventItemsAdapter mEventItemsAdapter = null;	
	//TranslateAnimation mPsudoEventListTransAnim;
	ValueAnimator mPsudoEventListTransAnim;
	
	public static final int NONE_INIT_EVENTLISTVIEW_STATUS = 0;
	public static final int EVENTLISTVIEW_ALEADY_INITED_STATUS = 1;
	public static final int EVENTLISTVIEW_CONFIG_LAYOUT_COMPLETION_STATUS = 2;
	public static final int EVENTLISTVIEW_LAUNCHED_TRANSLATE_ANIMATION_STATUS = 3;
	int mCurrentEventListViewStatus = NONE_INIT_EVENTLISTVIEW_STATUS;
	
	
	public void makeExpandModeEventsListView() {   
		synchronized(mCallBackOneDayEventsLoader) {			
	        
			mPsudoMonthExpandModeEventsListView = mFragment.mPsudoMonthExpandModeEventsListView;
			
	    	int normalWeekHeightInEPMode = (int) mFragment.mMonthListViewNormalWeekHeightInEPMode;
			mEventItemsAdapter = new PsudoEventItemsAdapter(mContext, mFragment.mFirstDayOfWeek, mFragment.mAnticipationListViewWidth, normalWeekHeightInEPMode);
			mEventItemsAdapter.setListView(mPsudoMonthExpandModeEventsListView);		
	    	
			mPsudoMonthExpandModeEventsListView.setAdapter(mEventItemsAdapter);      	
	    	    	
			mPsudoMonthExpandModeEventsListView.setCacheColorHint(0);
	        // No dividers
			mPsudoMonthExpandModeEventsListView.setDivider(null);
	        // Items are clickable
			mPsudoMonthExpandModeEventsListView.setItemsCanFocus(false);
			mPsudoMonthExpandModeEventsListView.setClickable(false);
	    	
	        // The thumb gets in the way, so disable it
			mPsudoMonthExpandModeEventsListView.setFastScrollEnabled(false); 
			mPsudoMonthExpandModeEventsListView.setVerticalScrollBarEnabled(false);    	
			mPsudoMonthExpandModeEventsListView.setFadingEdgeLength(0);
	    	
	    	int dayHeaderHeight = (int) mFragment.getResources().getDimension(R.dimen.day_header_height);
			int top = dayHeaderHeight + (normalWeekHeightInEPMode * mTargetMonthWeekNumbers);  
	    	mPsudoEventListTransAnim = ValueAnimator.ofFloat(mFragment.mAnticipationListViewHeight, top);
	    	mPsudoEventListTransAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator valueAnimator) {
					
					float top = (Float) valueAnimator.getAnimatedValue();
					
					mPsudoMonthExpandModeEventsListView.setY(top);
					
				}
	    	});
	    	
	    	mPsudoMonthExpandModeEventsListView.setY(mFragment.mAnticipationListViewHeight);
	    		    	
	    	mPsudoMonthExpandModeEventsListView.setVisibility(View.VISIBLE);	
	    	// scaleRatioYAxisRateInEPMode
			// mTargetMonthScaleDuration
			/*float remainingAnimDuration = mTargetMonthScaleDuration - (mTargetMonthScaleDuration * scaleRatioYAxisRateInEPMode);
			long duration = (long) remainingAnimDuration;
			mPsudoEventListTransAnim.setDuration(duration);*/
			mPsudoEventListTransAnim.setDuration(mTargetMonthScaleDuration);
			//ITEAnimInterpolator scaleAnimInterpolator = new ITEAnimInterpolator(height, mPsudoEventListTransAnim);
			mPsudoEventListTransAnim.setInterpolator(new DecelerateInterpolator(1.5f));	    	
		
			if (mOneDayEventsLoadedCompletion) {
				
				// mOneDayEvents�� null�� ��Ȳ�� �߻��Ѵ�
				// :�׷��ٸ� one day event load�� �Ϸ���� ���ߴٴ� ���� �ǹ��Ѵ�
				// -> �ذ� ����� yearAdapter�� �ʱ�ȭ�� �� EventListView�� �ʱ�ȭ����
				//    ���� load ���̶�� non event�� drawing�� ���� �ʰ�
				//    drawing�� callback �Լ����� �ñ���
				
				
				if (mOneDayEvents.size() != 0) {
					int targetMonthFirstJulianDay;
					if (mToday.get(Calendar.YEAR) == mGoToMonthTime.get(Calendar.YEAR) && 
            				mToday.get(Calendar.MONTH) == mGoToMonthTime.get(Calendar.MONTH)) {						
						targetMonthFirstJulianDay = ETime.getJulianDay(mToday.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); 	
            		}
					else {
						targetMonthFirstJulianDay = ETime.getJulianDay(mGoToMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek); 	
					}					
		    		
		    		mEventItemsAdapter.setEvents(targetMonthFirstJulianDay, targetMonthFirstJulianDay, mOneDayEvents);
		    	}
		    	else {
		    		mEventItemsAdapter.setNonEvents();
		    	}					
		    	
				mCurrentEventListViewStatus = EVENTLISTVIEW_CONFIG_LAYOUT_COMPLETION_STATUS;
			}
			else {
				mCurrentEventListViewStatus = EVENTLISTVIEW_ALEADY_INITED_STATUS;
				return;
			}    
			
		}
    }
	
	public final int YEAR_EXIT_ANIM_COMPLETION = 1;
	public final int CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION = 2;
	private final int ENTIRE_ANIMATIONS_COMPLETION = YEAR_EXIT_ANIM_COMPLETION | 
			CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION;
	
	int mExitAnimationCompletionStatus = 0;
	public synchronized void notifyExitAnimCompletion(int flag) {
		mExitAnimationCompletionStatus |= flag;
		if (mExitAnimationCompletionStatus == ENTIRE_ANIMATIONS_COMPLETION) {
			goodByeFragment();
		}
	}
	
	Runnable mUpperActionBarSwitchFragmentAnimCompletionCallBack = new Runnable() {

		@Override
		public void run() {	
			notifyExitAnimCompletion(CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION);
		}
	};

	public void goodByeFragment() {
		int viewType = ViewType.MONTH;
		mController.setTime(mGoToMonthTime.getTimeInMillis());
		mController.sendEvent(mAdapter, EventType.GO_TO, mGoToMonthTime, null, -1, viewType);
		
	}
}
