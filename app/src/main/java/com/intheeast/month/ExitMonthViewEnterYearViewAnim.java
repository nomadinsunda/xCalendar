package com.intheeast.month;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;
import com.intheeast.year.YearMonthTableLayout;
import com.intheeast.year.YearMonthsView;
import com.intheeast.year.YearsAdapter;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
//import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

public class ExitMonthViewEnterYearViewAnim {

	private static final String TAG = "ExitMonthViewEnterYearViewAnim";
    private static boolean INFO = true;
    
	Context mContext;
	MonthFragment mFragment;
	PsudoYearView mPsudoYearView;
	PsudoNonTargetMonthView mPsudoPrvMonthView;	
	PsudoTargetMonthView mPsudoTargetMonthView;
	PsudoNonTargetMonthView mPsudoNextMonthView;
	
	Calendar mSelectedYear;
	String mTzId;
	TimeZone mTimeZone;
	int mFirstDayOfWeek;
	
	ArrayList<ArrayList<Event>> mEventDayList;
	ArrayList<Event> mEvents = null;
	
	float mMiniMonthRatio;
	float mMonthListViewDisplayAspectRatio;
	float mMiniMonthViewScaleRatio;
	float mMiniMonthScaleFromX;
	float mMiniMonthScaleToX;
	float mMiniMonthScaleFromY;
	float mMiniMonthScaleToY;
	
	float mScaleDownMonthIndicatorHeight;
	float mScaleDownMonthNormalWeekHeight;	
	
	float mScaleYAxisTotalRateChange;
	float mScaleXAxisTotalRateChange;
	
	//float mScaleAspectRatioPerMS;
	
	float mHeightOfChangePsudoTargetMonthViewToMiniMonthView;
	
	boolean mChangeMonthListViewLayoutFormat = false;
	
	long mYearListViewScaleDuration = 0;
	long mTargetMonthScaleDuration = 0;
	long mNonTargetMonthScaleDuration = 0;
	
	CalendarController mController;
	
	public ExitMonthViewEnterYearViewAnim(Context context, MonthFragment fragment, Calendar selectedYear) {
		mContext = context;		
		mFragment = fragment;
		mController = mFragment.mController;
		mPsudoYearView = mFragment.mPsudoYearView;
		mPsudoPrvMonthView = mFragment.mPsudoPrvMonthView;
		mPsudoTargetMonthView = mFragment.mPsudoTargetMonthView;
		
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
		mTimeZone = selectedYear.getTimeZone();
		mTzId = mTimeZone.getID();
		mSelectedYear = GregorianCalendar.getInstance(mTimeZone);//selectedYear;
		mSelectedYear.setTimeInMillis(selectedYear.getTimeInMillis());
		
		init();
	}
	
	public void init() {
		mEventDayList = mFragment.mAdapter.getEventDayList();
		mEvents = mFragment.mAdapter.getEvents();
		
		mFragment.mListView.setVisibility(View.GONE);	
		mFragment.mCalendarViewsSecondaryActionBar.setVisibility(View.GONE);		
		
		makePsudoYearView();
		mPsudoYearView.setVisibility(View.VISIBLE);
		
		// ���� ��带 Ȯ���ؼ� ��忡 �´� view���� �����ؾ� �Ѵ�
		if (mFragment.mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {		
			
			makeNormalModePsudoMonthListView();
			
			makeNormalModeTargetMonthViewScaleToMiniMonthAnimation();
			mPsudoTargetMonthView.setVisibility(View.VISIBLE);
			
			makeNonTargetMonthsExitAnimation();
		}
		else {
			makeExpandModePsudoMonthListView();
			
			makeExpandModeTargetMonthViewScaleToMiniMonthAnimation();
			mPsudoTargetMonthView.setVisibility(View.VISIBLE);
			
			makeEventListViewExitAnim();
		}
		
		makePsudoYearViewEnterAnimation();
		
		mFragment.mUpperActionBarFrag.setSwitchFragmentAnim(ViewType.YEAR, mSelectedYear.getTimeInMillis(), mTargetMonthScaleDuration, mFragment.mCurrentMonthViewMode, mUpperActionBarSwitchFragmentAnimCompletionCallBack);
	}
	
	public void sendEventsToView(PsudoTargetMonthView v) {
        if (mEventDayList.size() == 0) {
            if (Log.isLoggable("tag", Log.DEBUG)) {
                Log.d("tag", "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        
        int viewJulianDay = v.getFirstJulianDay();
        
        int start = viewJulianDay - mFragment.mAdapter.mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable("tag", Log.DEBUG)) {
                Log.d("tag", "Week is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFragment.mAdapter.mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }
	
	public void sendEventsToView(PsudoNonTargetMonthView v) {
		if (mEventDayList.size() == 0) {
            if (Log.isLoggable("tag", Log.DEBUG)) {
                Log.d("tag", "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        
        int viewJulianDay = ETime.getJulianDay(v.mMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//v.getFirstJulianDay();
        
        int start = viewJulianDay - mFragment.mAdapter.mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable("tag", Log.DEBUG)) {
                Log.d("tag", "Week is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFragment.mAdapter.mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }	
	
	
	public void startMonthExitAnim() {
		
		if (mFragment.mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {			
			startNonPsudoTargetMonth();
			mNMPsudoMonthListViewScaleAnimator.start();
			
		}
		else {
			mFragment.mEventsListView.startAnimation(mEventsListViewExitTranslateAnim);
			mEMPsudoMonthListViewScaleAnimator.start();
		}
		
		mPsudoYearView.startAnimation(mYearListViewAnimationSet);	
		
		mFragment.mUpperActionBarFrag.startSwitchFragmentAnim(mFragment.mCurrentMonthViewMode);		
	}	
	
	
	public void makePsudoYearView() {
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mPsudoYearView.setLayoutParams(params);
		/*
        if) position == 267,
			267 * 3 = 801 months
			801 / 12 = 66 years 
			801 % 12 = 9 months ->
			1970 + 66 year/ 9 months + 1
    	*/
        int targetYear = mSelectedYear.get(Calendar.YEAR);
        int yearsFromEpoch = targetYear - YearsAdapter.EPOCH_YEAR;
        int monthsFromEpoch = yearsFromEpoch * 12;
        int position = monthsFromEpoch / 3;  
        
    	HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
    	drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mFragment.mAnticipationYearIndicatorHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mFragment.mAnticipationYearIndicatorTextHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mFragment.mAnticipationYearIndicatorTextBaseLineY);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mFragment.mAnticipationMiniMonthWidth);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mFragment.mAnticipationMiniMonthHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, mFragment.mAnticipationMiniMonthIndicatorTextHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, mFragment.mAnticipationMiniMonthDayTextBaseLineY);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, mFragment.mAnticipationMiniMonthDayTextHeight);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN, mFragment.mAnticipationMiniMonthLeftMargin);
		
		drawingParams.put(YearMonthsView.VIEW_PARAMS_POSITION, position);    		
		drawingParams.put(YearMonthsView.VIEW_PARAMS_WEEK_START, mFragment.mFirstDayOfWeek);
		
		mPsudoYearView.setMonthsParams(drawingParams, mSelectedYear);		
	}
			
	float mYearListViewPivotXAbsPx;
	float mYearListViewPivotX;
	float mYearListViewPivotYAbsPx;
	float mYearListViewPivotY;
	float mYearListViewScaleFromX;
	float mYearListViewScaleToX;
	float mYearListViewScaleFromY;
	float mYearListViewScaleToY;
	float mYearListViewDisplayAspectRatio;
	float mYearListViewScaleRatio;
	ScaleAnimation mYearListViewScaleAnimation;
	AlphaAnimation mYearListViewAlphaAnimation;
	AnimationSet mYearListViewAnimationSet;
	
	public void makePsudoYearViewEnterAnimation() {		    	
    	float width = mFragment.mAnticipationYearListViewWidth;    	
    	float height = mFragment.mAnticipationYearListViewHeight;
    	mYearListViewDisplayAspectRatio = width / height;    	
    	float miniMonthHeight = mFragment.mAnticipationMiniMonthHeight;  
    	mYearListViewScaleRatio = height / miniMonthHeight;
    	//mYearListViewScaleRatio = mYearListViewScaleRatio / 2;
    	mYearListViewScaleFromX = mYearListViewScaleRatio * mYearListViewDisplayAspectRatio;
    	mYearListViewScaleToX = 1;
    	mYearListViewScaleFromY = mYearListViewScaleRatio;
    	mYearListViewScaleToY = 1;    	
    	
    	int selectedMonth = mSelectedYear.get(Calendar.MONTH);    	
    	int rowNumber = selectedMonth / 3;
    	int columnNumber = selectedMonth % 3;
    	
    	int selectedMonthsViewTop = 0;
    	int selectedMonthsViewBottom = 0;
    	switch(rowNumber) {
    	case 0:
    		selectedMonthsViewTop = mFragment.mAnticipationYearIndicatorHeight;
    		selectedMonthsViewBottom = selectedMonthsViewTop + mFragment.mAnticipationMiniMonthHeight;
    		break;
    	case 1:
    		selectedMonthsViewTop = mFragment.mAnticipationYearIndicatorHeight + mFragment.mAnticipationMiniMonthHeight;
    		selectedMonthsViewBottom = selectedMonthsViewTop + mFragment.mAnticipationMiniMonthHeight;
    		break;
    	case 2:
    		selectedMonthsViewTop = mFragment.mAnticipationYearIndicatorHeight + (mFragment.mAnticipationMiniMonthHeight * rowNumber);
    		selectedMonthsViewBottom = selectedMonthsViewTop + mFragment.mAnticipationMiniMonthHeight;
    		break;
    	case 3:
    		selectedMonthsViewTop = mFragment.mAnticipationYearIndicatorHeight + (mFragment.mAnticipationMiniMonthHeight * rowNumber);
    		selectedMonthsViewBottom = selectedMonthsViewTop + mFragment.mAnticipationMiniMonthHeight;
    		break;
    	}    	
    	
    	float availableUpperScaleRegion = 0;
    	float availableLowerScaleRegion = 0;
    	
    	availableUpperScaleRegion = selectedMonthsViewTop;
    	    	
    	availableLowerScaleRegion = height - selectedMonthsViewBottom;
    	// if availableUpperScaleRegion == 329,
    	//    availableLowerScaleRegion == 472 
    	//    : 329A + 472A = 236[mMiniMonthHeight]
    	//    : 801A = 236
    	//    : A = 236 / 801 = 0.2946
    	//    : 329 * A[0.2946] = 96.9338
    	//    : (329 + 96.9338) / 1038[ListView Height] = 425.9338 / 1038 = 0.41034
    	// *pivotY = 0.41034
    	float verticalA = 0;    	
    	
    	if (availableLowerScaleRegion <= 0) {
    		availableLowerScaleRegion = 0; // �� ���� pivotY�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		height = selectedMonthsViewBottom; // height�� ��������� �Ѵ�    		
    	}   	
    	
    	verticalA = miniMonthHeight / (availableUpperScaleRegion + availableLowerScaleRegion);
    	mYearListViewPivotYAbsPx = availableUpperScaleRegion + (availableUpperScaleRegion * verticalA);
    	mYearListViewPivotY = mYearListViewPivotYAbsPx / height; 	
    	
    	float availableLeftScaleRegion = 0;
    	float availableRightScaleRegion = 0;
    	
    	switch(columnNumber) {
		case 0:
			availableLeftScaleRegion = mFragment.mAnticipationMiniMonthLeftMargin; // �� ���� pivotX�� 0�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableRightScaleRegion = (mFragment.mAnticipationMiniMonthLeftMargin * 3) + (mFragment.mAnticipationMiniMonthWidth * 2);
			break;
		case 1:
			availableLeftScaleRegion = mFragment.mAnticipationMiniMonthWidth + (mFragment.mAnticipationMiniMonthLeftMargin * 2);
    		availableRightScaleRegion = (mFragment.mAnticipationMiniMonthLeftMargin *2) + mFragment.mAnticipationMiniMonthWidth;
			break;
		case 2:
			availableRightScaleRegion = mFragment.mAnticipationMiniMonthLeftMargin; // �� ���� pivotX�� 1�� �����ؾ� �ϴ� ���!!!!!!!!!!!!!!!!
    		availableLeftScaleRegion = (mFragment.mAnticipationMiniMonthLeftMargin * 3) + (mFragment.mAnticipationMiniMonthWidth * 2);
			break;
		}    	
    	
    	float horizontalA = mFragment.mAnticipationMiniMonthWidth / (availableLeftScaleRegion + availableRightScaleRegion);
    	mYearListViewPivotXAbsPx = availableLeftScaleRegion + (availableLeftScaleRegion * horizontalA);
    	mYearListViewPivotX = mYearListViewPivotXAbsPx / width;    	
    	
    	int pivotXType = Animation.RELATIVE_TO_SELF;
    	int pivotYType = Animation.ABSOLUTE;
    	//int pivotYType = Animation.RELATIVE_TO_SELF;
    	mYearListViewScaleAnimation = new ScaleAnimation(
    			mYearListViewScaleFromX, mYearListViewScaleToX, 
    			mYearListViewScaleFromY, mYearListViewScaleToY,
    			pivotXType, mYearListViewPivotX, 
    			pivotYType, mYearListViewPivotYAbsPx);
    	
    	mYearListViewAlphaAnimation = new AlphaAnimation(0, 1.0f);
    	
    	//float xxx= 6500 * mYearListViewScaleRatio;
    	//mYearListViewScaleDuration = ITEAnimationUtils.calculateDuration(height, height, 3300, xxx);
    	mYearListViewScaleDuration = 500;
    	
    	mYearListViewAnimationSet = new AnimationSet(true);    	
    	mYearListViewAnimationSet.addAnimation(mYearListViewScaleAnimation);
    	mYearListViewAnimationSet.addAnimation(mYearListViewAlphaAnimation);
    	mYearListViewAnimationSet.setDuration(mYearListViewScaleDuration);
    	
    	//ITEAnimInterpolator scaleInterpolator = new ITEAnimInterpolator(mFragment.mAnticipationMonthListViewHeight - mFragment.mAnticipationMiniMonthHeight, mYearListViewAnimationSet);
    	//mYearListViewAnimationSet.setInterpolator(scaleInterpolator);   
    	mYearListViewAnimationSet.setInterpolator(new DecelerateInterpolator(2));    	
    	mYearListViewAnimationSet.setAnimationListener(mYearListViewAnimationSetListener);
    	mYearListViewAnimationSet.setFillAfter(true);
    	mYearListViewAnimationSet.setFillEnabled(true);   	
	}    	
	AnimationListener mYearListViewAnimationSetListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}
		
	};    	
	
	public int mMonthsLayout = EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE;
	public static final int ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE = 0;
	public static final int EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE = 1;
	public static final int ADDED_ONLY_PRV_MONTH_AT_NORMALMODE = 2;
	public static final int ADDED_ONLY_NEXT_MONTH_AT_NORMALMODE = 3;
	// mMustDrawNextMonthRegion�� true�̰�
	// mVisualNextMonthWeekNumbers�� 0�̶��,
	// next month visual region�� �ܼ��� month indicator text�� �׷��� �Ѵ�
	
	int mVisualNextMonthWeekNumbers = 0;
	public void makeNormalModePsudoMonthListView() {
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFragment.mListView.getFirstDayOfWeek());  
		TempCalendar.setTimeInMillis(mSelectedYear.getTimeInMillis());	    		     	
    	TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
		int targetMonthHeight = mFragment.mListView.getNMMonthIndicatorHeight() + 
				(mFragment.mListView.getNMNormalWeekHeight() * weekNumbersOfMonth );				
		
		MonthWeekView child = (MonthWeekView) mFragment.mListView.getChildAt(0);
		int childTop = child.getTop();
		int weekPattern = child.getWeekPattern();
		
		int prvMonthWeekNumbers = makePrvMonthWeekView();
    	if (prvMonthWeekNumbers == 0) {    		
    		
    		int upperUnvisibleDayNumbers =0;
    		int realVisualTop = 0;    		
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {    			
    			realVisualTop = childTop + mFragment.mListView.getNMNormalWeekHeight(); 		  		
        	}
    		else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
    			realVisualTop = childTop; 	
    		}
    		else {
    			//int firstJulianDay = child.getFirstJulianDay();        		
        		Calendar firstDayTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
        		//long firstJulianDayMillis = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		firstDayTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//firstDayTime.setJulianDay(firstJulianDay);        		
        		
        		upperUnvisibleDayNumbers = firstDayTime.get(Calendar.DAY_OF_MONTH) - 1;   
        		int upperUnvisibleWeekNumbers = upperUnvisibleDayNumbers / 7;
        		int remainingDays = upperUnvisibleDayNumbers % 7;
        		
        		if (remainingDays > 0)
    				upperUnvisibleWeekNumbers++;
        		
        		int upperUnvisibleWeeksHeight = mFragment.mListView.getNMMonthIndicatorHeight() + 
	    				(mFragment.mListView.getNMNormalWeekHeight() * upperUnvisibleWeekNumbers);
	    		
	    		realVisualTop = childTop - upperUnvisibleWeeksHeight; 
    		}
    		    		
    		makeNormalModeTargetMonthWeekView(realVisualTop, targetMonthHeight);
    		
    		int targetMonthBottom = realVisualTop + targetMonthHeight;    		
    		if (targetMonthBottom < mFragment.mListView.getOriginalListViewHeight()) {
    					
    			int remaingSpace = mFragment.mListView.getOriginalListViewHeight() - targetMonthBottom;
    			
    			if (remaingSpace > mFragment.mListView.getNMMonthIndicatorHeight()) {
    				//remaingSpace = remaingSpace - mFragment.mListView.getNMMonthIndicatorHeight();
    				int nextMonthVisualHeight = mFragment.mListView.getNMMonthIndicatorHeight();
    				
    				while (true) {
    					mVisualNextMonthWeekNumbers++;
    					nextMonthVisualHeight = nextMonthVisualHeight + mFragment.mListView.getNMNormalWeekHeight();						
						
    					if (remaingSpace <= nextMonthVisualHeight) {
    						break;
    					}    					
    				}
    				
    				makeNextMonthWeekView(targetMonthBottom);
    				mMonthsLayout = ADDED_ONLY_NEXT_MONTH_AT_NORMALMODE;
    			}    			
    		}
    		else {    		
    			mMonthsLayout = EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE; 
    		}
    	}
    	else {
    		// prv month�� week���� drawing �ؾ� �Ѵٸ�,
    		// prv month�� visual region height ���� �˾ƾ� �Ѵ�!!!
    		int prvMonthWeekVisibleRegionHeight = mPrvMonthWeekInfo.topOfVisibleFirstWeek +
    				mPrvMonthWeekInfo.prvMonthWeekRegionHeight;
    		
    		makeNormalModeTargetMonthWeekView(prvMonthWeekVisibleRegionHeight, targetMonthHeight);
    		
    		int targetMonthBottom = prvMonthWeekVisibleRegionHeight + targetMonthHeight;
    		if (targetMonthBottom < mFragment.mListView.getOriginalListViewHeight()) {    			
    				
    			int remaingSpace = mFragment.mListView.getOriginalListViewHeight() - targetMonthBottom;
    			int nextMonthVisualHeight = mFragment.mListView.getNMMonthIndicatorHeight();
    			while (true) {
					mVisualNextMonthWeekNumbers++;
					nextMonthVisualHeight = nextMonthVisualHeight + mFragment.mListView.getNMNormalWeekHeight();						
					
					if (remaingSpace <= nextMonthVisualHeight) {
						break;
					}    					
				}   			
    			
    			mMonthsLayout = ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE;
				makeNextMonthWeekView(targetMonthBottom);    			
    		}
    		else {
    			// next month�� visual region�� �����Ƿ� drawing ���� �ʴ´�!!!
    			mMonthsLayout = ADDED_ONLY_PRV_MONTH_AT_NORMALMODE;
    		}        		
    	}
	}
	
	PrvMonthWeekInfo mPrvMonthWeekInfo;
	
	public int makePrvMonthWeekView() {
		// ���� listview�� ����(first visible week�� ��¥)�� Ȯ���Ѵ�
		MonthWeekView child = (MonthWeekView) mFragment.mListView.getChildAt(0);
		
		int childTop = child.getTop();		
		int weekPattern = child.getWeekPattern();
		int position = child.getWeekNumber();		
		Calendar firstJulainDayTime = GregorianCalendar.getInstance(mTimeZone);		
		firstJulainDayTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());  
		if (firstJulainDayTime.get(Calendar.MONTH) == mSelectedYear.get(Calendar.MONTH)) {
			// first child�� first day of week�� month�� selected year�� month�� �����ϴٴ� ����,
			// prv month�� week���� �ϳ��� �������� ���� �ʴٴ� ���� �ǹ��Ѵ�
			if (INFO) Log.i(TAG, "same month");
			return 0;
		}			
		else if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
			
			if (Math.abs(childTop) >= mFragment.mListView.getNMNormalWeekHeight()) {
				// prv month last week�� ��� �κе� ���� ������ �ʴ� ����̴�
				if (INFO) Log.i(TAG, "not display prv month last week");
				return 0;				
			}
		}		
								
		Calendar prvMonthCalendar = Calendar.getInstance(mTimeZone);
		prvMonthCalendar.setFirstDayOfWeek(mFragment.mListView.getFirstDayOfWeek());  
		prvMonthCalendar.setTimeInMillis(firstJulainDayTime.getTimeInMillis());
		int maximumMonthDays = prvMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		int firstMonthDay = prvMonthCalendar.get(Calendar.DAY_OF_MONTH);
		int remainingMonthDays = (maximumMonthDays - firstMonthDay) + 1;		
		int prvMonthWeeks = ETime.getRemainingWeeks(prvMonthCalendar, true);
		
    	prvMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
    	int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(prvMonthCalendar);		
		
    	if (weekPattern != MonthWeekView.NORMAL_WEEK_PATTERN) {   
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) { 
    			if (INFO) Log.i(TAG, "TDMCWP=1");
    			prvMonthWeeks = 1;     			
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {
        		if (INFO) Log.i(TAG, "LIMWP=1");
        		prvMonthWeeks = 1;
        	}          
    	}
    	else {
    		if (INFO) Log.i(TAG, "NWP=" + String.valueOf(prvMonthWeeks));
    	}
    	
    	
    	if (prvMonthWeeks != 0) {
    		mPrvMonthWeekInfo = new PrvMonthWeekInfo();
    		mPrvMonthWeekInfo.topOfVisibleFirstWeek = childTop;    		
    		mPrvMonthWeekInfo.positionOfVisibleFirstWeek = position;
    		mPrvMonthWeekInfo.prvMonthWeeks = prvMonthWeeks;
    		mPrvMonthWeekInfo.prvMonthWeekRegionHeight = mFragment.mListView.getNMNormalWeekHeight() * prvMonthWeeks;    		
    		/*
    		 android:layout_alignParentTop="true"
        	 android:layout_width="match_parent"
			 android:layout_height="match_parent"		
			 android:layout_marginTop="@dimen/day_header_height"
    		 */
    		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mPsudoPrvMonthView.getLayoutParams();
    		params.height = mPrvMonthWeekInfo.prvMonthWeekRegionHeight;
    		mPsudoPrvMonthView.setLayoutParams(params);    		
    		mPsudoPrvMonthView.setY(mPrvMonthWeekInfo.topOfVisibleFirstWeek);   
    		mPsudoPrvMonthView.setVisibility(View.VISIBLE);
    		
    		mPsudoPrvMonthView.setMonthDrawingParams(mFragment.mListView);	
    		
    		firstJulainDayTime.setTimeInMillis(prvMonthCalendar.getTimeInMillis());
    		//firstJulainDayTime.normalize(true);
    		ArrayList<String[]> monthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(firstJulainDayTime, mTzId, mTimeZone, mFragment.mFirstDayOfWeek);
    				
    		int startDrawingWeekIndex = weekNumbersOfMonth - prvMonthWeeks;
    		int endDrawingWeekIndex = weekNumbersOfMonth;
    		// startDrawingWeekIndex�� number ordering�� zero ���ʹ�
    		mPsudoPrvMonthView.setMonthInfo(firstJulainDayTime, remainingMonthDays, monthWeekDayNumberList, startDrawingWeekIndex, endDrawingWeekIndex); 
    		sendEventsToView(mPsudoPrvMonthView);
    	}
    	else {
    		mPrvMonthWeekInfo = null;
    	}
    	
    	return prvMonthWeeks;		
	}
	
	
	int mTargetMonthTop;
	int mTargetMonthHeight;
	
	public void makeNormalModeTargetMonthWeekView(int top, int targetMonthHeight) {
		mTargetMonthTop = top;
		mTargetMonthHeight = targetMonthHeight;
		
		//mPsudoTargetMonthView = new PsudoTargetMonthView(mContext);
		//mPsudoTargetMonthView.setVisibility(View.VISIBLE);
		mPsudoTargetMonthView.setY(top); 
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mFragment.mListView.getWidth(), targetMonthHeight);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.setMargins(0, mFragment.mDayHeaderHeight, 0, 0);
		mPsudoTargetMonthView.setLayoutParams(params);		
		
		HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE, YearsAdapter.MONTHVIEW_NORMAL_MODE);
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH, mFragment.mListView.getWidth());	
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT, mTargetMonthHeight);	
				 
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_WIDTH, mFragment.mAnticipationMiniMonthWidth);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_HEIGHT, mFragment.mAnticipationMiniMonthHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mFragment.mListView.getWidth());		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mTargetMonthHeight);
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mFragment.mAnticipationYearIndicatorHeight);			
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mFragment.mAnticipationYearIndicatorTextHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mFragment.mAnticipationYearIndicatorTextBaseLineY);
				
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT, (int) mFragment.mListView.getNMMonthIndicatorHeight());		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, (int) mFragment.mMonthListViewMonthIndicatorTextHeight);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, (int) mFragment.mMonthListViewMonthIndicatorTextBaselineY);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, (int) mFragment.mMonthListMonthDayTextBaselineY);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, (int) mFragment.mMonthListMonthDayTextHeight);			
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT, mFragment.mListView.getNMNormalWeekHeight());
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN, mFragment.mAnticipationMiniMonthLeftMargin);
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT, mFragment.mListView.getNMNormalWeekHeight());
		//drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT, mFragment.mListView.getNMLastWeekHeight());		
		
		drawingParams.put(YearMonthTableLayout.VIEW_PARAMS_WEEK_START, mFragment.mFirstDayOfWeek);
		
		mPsudoTargetMonthView.setMonthDrawingParams(drawingParams);
		
		Calendar targetMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		targetMonthTime.setTimeInMillis(mSelectedYear.getTimeInMillis());
		targetMonthTime.set(Calendar.DAY_OF_MONTH, 1);//targetMonthTime.monthDay = 1;
		//targetMonthTime.normalize(true);
		ArrayList<String[]> monthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(targetMonthTime, mTzId, mTimeZone, mFragment.mFirstDayOfWeek);
		// ���⼭ monthlist�� month indicator text�� height�� ����������
		mPsudoTargetMonthView.setMonthInfo(targetMonthTime, monthWeekDayNumberList);
		
		sendEventsToView(mPsudoTargetMonthView);
		//mFragment.mMonthViewLayout.addView(mPsudoTargetMonthView);
	}
	
	
	
	public void makeNextMonthWeekView(int targetMonthBottom) {
				
		Calendar nextMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		nextMonthTime.setTimeInMillis(mSelectedYear.getTimeInMillis());   
		//nextMonthTime.normalize(true);
		nextMonthTime.add(Calendar.MONTH, 1);//nextMonthTime.month = nextMonthTime.month + 1;
		//nextMonthTime.normalize(true);
		nextMonthTime.set(nextMonthTime.get(Calendar.YEAR), nextMonthTime.get(Calendar.MONTH), 1);//nextMonthTime.set(1, nextMonthTime.month, nextMonthTime.year);
		//nextMonthTime.normalize(true);    	
		long gmtoff = mTimeZone.getRawOffset() / 1000;
		//int firstJulianDay = ETime.getJulianDay(nextMonthTime.getTimeInMillis(), gmtoff);//int firstJulianDay = Time.getJulianDay(nextMonthTime.toMillis(true), nextMonthTime.gmtoff);
    	
		int psudoNextMonthVisualHeight = mFragment.mListView.getNMMonthIndicatorHeight() + (mFragment.mListView.getNMNormalWeekHeight() * mVisualNextMonthWeekNumbers);
		mPsudoNextMonthView = new PsudoNonTargetMonthView(mContext);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mFragment.mListView.getWidth(), psudoNextMonthVisualHeight);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.setMargins(0, mFragment.mDayHeaderHeight, 0, 0);
		mPsudoNextMonthView.setLayoutParams(params);
		mPsudoNextMonthView.setY(targetMonthBottom);   		
		
		mPsudoNextMonthView.setMonthDrawingParams(mFragment.mListView);			
		
		ArrayList<String[]> monthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(nextMonthTime, mTzId, mTimeZone, mFragment.mFirstDayOfWeek);
				
		int dayNums = 0;
		int size = monthWeekDayNumberList.size();
		for (int i=0; i<size; i++) {
			String[] dayNumbersOfMonth = monthWeekDayNumberList.get(i);
			
			for (int j=0; j<7; j++) {
				if (!dayNumbersOfMonth[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) 
					dayNums++;
			}	
		}
		
		int startDrawingWeekIndex = 0;
		int endDrawingWeekIndex = mVisualNextMonthWeekNumbers;
		// startDrawingWeekIndex�� number ordering�� zero ���ʹ�		
		mPsudoNextMonthView.setMonthInfo(nextMonthTime, dayNums, monthWeekDayNumberList, startDrawingWeekIndex, endDrawingWeekIndex); 
		sendEventsToView(mPsudoNextMonthView);
		
		mFragment.mMonthViewLayout.addView(mPsudoNextMonthView);		
	}	
	
	ValueAnimator mNMPsudoMonthListViewScaleAnimator;
	float mMonthTextHeightScaleDownRatio;
	float mMonthTextBaseLineScaleDownRatio;
	float mMonthDayTextHeightDownRatio;
	float mMonthDayTextBaseLineScaleDownRatio;
	int mPrvScalingTargetMonthHeight = -1;
	int mPrvScalingTargetMonthWidth = -1;
	float mTargetMonthSizeRatio;
	public void makeNormalModeTargetMonthViewScaleToMiniMonthAnimation() {
		
		makeMiniMonthDimension();		
    	
    	float miniMonthWidth = mFragment.mAnticipationMiniMonthWidth; 
    	float miniMonthHeight = mFragment.mAnticipationMiniMonthHeight;	
    	
		float width = mFragment.mAnticipationMonthListViewWidth;    	
    	float height = mPrvScalingTargetMonthHeight = mTargetMonthHeight;
    	mTargetMonthSizeRatio = width / height;
    	
    	//mNonTargetMonthScaleDuration = mTargetMonthScaleDuration = ITEAnimationUtils.calculateDuration(mFragment.mAnticipationMonthListViewHeight - mFragment.mAnticipationMiniMonthHeight
    			//, mFragment.mAnticipationMonthListViewHeight, 3300, 8000);
    	
    	mTargetMonthScaleDuration = 500;    	
    	mNonTargetMonthScaleDuration = mTargetMonthScaleDuration / 2;
    	
    	mMonthListViewDisplayAspectRatio = width / height; 
    	
    	mMiniMonthViewScaleRatio = height / miniMonthHeight; 
    	    	
    	mMiniMonthScaleFromX = width / miniMonthWidth;    	
    	mMiniMonthScaleToX = 1;
    	    	
    	mMiniMonthScaleFromY = mMiniMonthViewScaleRatio;
    	mMiniMonthScaleToY = 1;     	   	    	
    					
		mMiniMonthRatio = miniMonthWidth / miniMonthHeight;
		
		mScaleDownMonthIndicatorHeight = (int) (mFragment.mMonthListViewMonthIndicatorHeight - mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY);
		mScaleDownMonthNormalWeekHeight = (int) (mFragment.mListView.getNMNormalWeekHeight() - mFragment.mAnticipationMiniMonthDayTextBaseLineY);		
		
		mMonthTextHeightScaleDownRatio = 1 - (mFragment.mAnticipationMiniMonthIndicatorTextHeight / mFragment.mMonthListViewMonthIndicatorTextHeight);
		mMonthTextBaseLineScaleDownRatio = 1 - 
				(mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY / mFragment.mMonthListViewMonthIndicatorTextBaselineY);
		mMonthDayTextHeightDownRatio = 1 - 
				(mFragment.mAnticipationMiniMonthDayTextHeight / mFragment.mMonthListMonthDayTextHeight);
		
		mHeightOfChangePsudoTargetMonthViewToMiniMonthView = mFragment.mAnticipationMiniMonthHeight * 1.8f;
		
		mScaleXAxisTotalRateChange = mMiniMonthScaleFromX - mMiniMonthScaleToX;
		mScaleYAxisTotalRateChange = mMiniMonthScaleFromY - mMiniMonthScaleToY;		
		
		mNMPsudoMonthListViewScaleAnimator = ValueAnimator.ofFloat(mMiniMonthScaleFromY, mMiniMonthScaleToY);
		mNMPsudoMonthListViewScaleAnimator.setDuration(mTargetMonthScaleDuration);
		ITEAnimInterpolator scaleInterpolator = new ITEAnimInterpolator(mFragment.mAnticipationMonthListViewHeight - mFragment.mAnticipationMiniMonthHeight, mNMPsudoMonthListViewScaleAnimator);
		//mNMPsudoMonthListViewScaleAnimator.setInterpolator(scaleInterpolator);		
		mNMPsudoMonthListViewScaleAnimator.setInterpolator(new DecelerateInterpolator(2));	
		
		makeLinearFuncitonForTransmation(miniMonthWidth, miniMonthHeight);
		
		mNMPsudoMonthListViewScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float fraction = valueAnimator.getAnimatedFraction();
				
				float scaleRatioYAxis = (Float) valueAnimator.getAnimatedValue();
				if (scaleRatioYAxis == mMiniMonthScaleFromY)
					return;					
				
				int height = (int) (mFragment.mAnticipationMiniMonthHeight * scaleRatioYAxis);
				if (mPrvScalingTargetMonthHeight == height) {					
					return;
				}
				
				int width = 0;
				// ���� �������� mTargetMonthSizeRatio�� �۾����� �Ѵ�
				if (height > mHeightOfChangePsudoTargetMonthViewToMiniMonthView) {					
					width = (int) (height * mTargetMonthSizeRatio);
				}
				else {					
					float scaleRatioXAxis = mMiniMonthScaleFromX - (mScaleXAxisTotalRateChange * fraction);
					width = (int) (mFragment.mAnticipationMiniMonthWidth * scaleRatioXAxis);								
				}				
				
				mPrvScalingTargetMonthHeight = height;	
				mPrvScalingTargetMonthWidth = width;
				
				float newCenterX = getPsudoTargetMonthCenterX(fraction);
				float newCenterY = getPsudoTargetMonthCenterY(newCenterX, fraction);
							
				float newLeft = newCenterX - (width / 2);
				float newTop = newCenterY - (height / 2);
				//float newTop = newCenterY - (height / 2) + mFragment.mDayHeaderHeight;
				//mPsudoTargetMonthView.setX(newLeft); 
				//mPsudoTargetMonthView.setY(newTop); // �߸���...������ ������ setY �����ؼ� �ٽ� �ѹ� �����ְ� üũ�ؾ� �Ѵ�
				mPsudoTargetMonthView.setTranslationX(newLeft);
				mPsudoTargetMonthView.setTranslationY(newTop);
				
				setScalePsudoTargetMonth(width, height);	
				
				mPsudoTargetMonthView.setFraction(fraction);
				
				if (height > mHeightOfChangePsudoTargetMonthViewToMiniMonthView) {
					setScalingNMPsudoMonthView(fraction, width, height);					
				}
				else {
					if (!mChangeMonthListViewLayoutFormat) {
						mPsudoTargetMonthView.launchScaleDrawingMiniMonthView();
						mChangeMonthListViewLayoutFormat = true;												
					}
					
					setScalingNMPsudoMiniMonthView(fraction, scaleRatioYAxis, width, height);					
				}					
			}
		});
		
		mNMPsudoMonthListViewScaleAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {	
				notifyExitAnimCompletion(MONTH_EXIT_ANIM_COMPLETION);
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
	
	public void startNonPsudoTargetMonth() {
		switch(mMonthsLayout) {
		case ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE:
			Log.i("tag", "ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE");
			mPsudoNextMonthView.startAnimation(mNextNonTargetMonthTransAnim);
			mPsudoPrvMonthView.startAnimation(mPrvNonTargetMonthTransAnim);
			break;
		case ADDED_ONLY_PRV_MONTH_AT_NORMALMODE:
			Log.i("tag", "ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE");
			mPsudoPrvMonthView.startAnimation(mPrvNonTargetMonthTransAnim);
			break;
		case ADDED_ONLY_NEXT_MONTH_AT_NORMALMODE:
			Log.i("tag", "ADDED_ONLY_NEXT_MONTH_AT_NORMALMODE");
			mPsudoNextMonthView.startAnimation(mNextNonTargetMonthTransAnim);
			break;
		case EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE:
			Log.i("tag", "EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE");
			break;		
		}
	}
	
	int mEEVMBGColor;
	int mEEVMBGColor_RED;
	int mEEVMBGColor_GREEN;
	int mEEVMBGColor_BLUE;
	public void makeExpandModePsudoMonthListView() {
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFragment.mListView.getFirstDayOfWeek());  
		TempCalendar.setTimeInMillis(mSelectedYear.getTimeInMillis());	    		     	
    	TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
		int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
		int targetMonthHeight = mFragment.mListView.getEEMMonthIndicatorHeight() + 
				(mFragment.mListView.getEEMNormalWeekHeight() * weekNumbersOfMonth);
				
		mTargetMonthTop = (int) -mFragment.mMonthListViewMonthIndicatorHeightInEPMode;		
				
		mTargetMonthHeight = targetMonthHeight;
		
		mPsudoTargetMonthView.setY(mTargetMonthTop);
		
		RelativeLayout.LayoutParams psudoMonthListViewParams = (RelativeLayout.LayoutParams) mPsudoTargetMonthView.getLayoutParams();
		psudoMonthListViewParams.width = mFragment.mListView.getWidth();
		psudoMonthListViewParams.height = targetMonthHeight;
		mPsudoTargetMonthView.setLayoutParams(psudoMonthListViewParams);
		
		HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHVIEW_MODE, YearsAdapter.MONTHVIEW_EXPAND_EVENTLIST_MODE);
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_WIDTH, mFragment.mListView.getWidth());
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_HEIGHT, mTargetMonthHeight);	
				 
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_WIDTH, mFragment.mAnticipationMiniMonthWidth);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_ORIGINAL_MINIMONTH_HEIGHT, mFragment.mAnticipationMiniMonthHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, mFragment.mListView.getWidth());	
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mTargetMonthHeight);//drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, mFragment.mListView.getOriginalListViewHeight()); 
				
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEAR_INDICATOR_HEIGHT, mFragment.mAnticipationYearIndicatorHeight);			
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_SIZE, mFragment.mAnticipationYearIndicatorTextHeight);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_YEARINDICATOR_TEXT_BASELINE_Y, mFragment.mAnticipationYearIndicatorTextBaseLineY);
				
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT, (int) mFragment.mMonthListViewMonthIndicatorHeightInEPMode);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, (int) mFragment.mMonthListViewMonthIndicatorTextHeightInEPMode);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, (int) mFragment.mMonthListViewMonthIndicatorTextBaselineYInEPMode);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, (int) mFragment.mMonthListMonthDayTextBaselineYInEPMode);		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, (int) mFragment.mMonthListMonthDayTextHeightInEPMode);			
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT, (int)mFragment.mMonthListViewNormalWeekHeightInEPMode);
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_LEFTMARGIN, (int) mFragment.mAnticipationMiniMonthLeftMargin);
		
		drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_NORMALWEEK_HEIGHT, (int)mFragment.mMonthListViewNormalWeekHeightInEPMode);
		//drawingParams.put(YearsAdapter.MONTHS_PARAMS_MONTHLISTVIEW_LASTWEEK_HEIGHT, (int)mFragment.mMonthListViewLastWeekHeightInEPMode);		
		
		drawingParams.put(YearMonthTableLayout.VIEW_PARAMS_WEEK_START, mFragment.mFirstDayOfWeek);
		
		mPsudoTargetMonthView.setMonthDrawingParams(drawingParams);
		
		Calendar targetMonthTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		targetMonthTime.setTimeInMillis(mSelectedYear.getTimeInMillis());
		targetMonthTime.set(Calendar.DAY_OF_MONTH, 1);//targetMonthTime.monthDay = 1;
		//targetMonthTime.normalize(true);
		ArrayList<String[]> monthWeekDayNumberList = YearMonthTableLayout.makeDayNumStrings(targetMonthTime, mTzId, mTimeZone, mFragment.mFirstDayOfWeek);
		// ���⼭ monthlist�� month indicator text�� height�� ����������
		mPsudoTargetMonthView.setMonthInfo(targetMonthTime, monthWeekDayNumberList);
		
		sendEventsToView(mPsudoTargetMonthView);		
	}
	
	
	
	ValueAnimator mEMPsudoMonthListViewScaleAnimator;
	float mMonthTextHeightScaleDownRatioInEPMode;
	float mMonthTextBaseLineScaleDownRatioInEPMode;
	float mMonthDayTextHeightDownRatioInEPMode;
	float mMonthDayTextBaseLineScaleDownRatioInEPMode;
	public void makeExpandModeTargetMonthViewScaleToMiniMonthAnimation() {
		makeMiniMonthDimension();	
    	
    	float miniMonthWidth = mFragment.mAnticipationMiniMonthWidth; 
    	float miniMonthHeight = mFragment.mAnticipationMiniMonthHeight;	
    	
		float width = mFragment.mAnticipationMonthListViewWidth;    	
    	float height = mPrvScalingTargetMonthHeight = mTargetMonthHeight;
    	mTargetMonthSizeRatio = width / height;
    	
    	//mTargetMonthScaleDuration = ITEAnimationUtils.calculateDuration(height, mFragment.mAnticipationMonthListViewHeight, 3300, 6500);
    	mTargetMonthScaleDuration = 500;
    	mEventsListViewExitTranslateAnimDuration = mTargetMonthScaleDuration / 2;
    	//mTargetMonthScaleDuration = 30000;
    	
    	mMonthListViewDisplayAspectRatio = width / height; 
    	
    	mMiniMonthViewScaleRatio = height / miniMonthHeight; 
    	    	
    	mMiniMonthScaleFromX = width / miniMonthWidth;    	
    	mMiniMonthScaleToX = 1;
    	    	
    	mMiniMonthScaleFromY = mMiniMonthViewScaleRatio;
    	mMiniMonthScaleToY = 1;     	   	    	
    					
		mMiniMonthRatio = miniMonthWidth / miniMonthHeight;
		
		mScaleDownMonthIndicatorHeight = (int) (mFragment.mMonthListViewMonthIndicatorHeightInEPMode - mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY);
		mScaleDownMonthNormalWeekHeight = (int) (mFragment.mMonthListViewNormalWeekHeightInEPMode - mFragment.mAnticipationMiniMonthDayTextBaseLineY);
		
		mMonthTextHeightScaleDownRatioInEPMode = 1 - (mFragment.mAnticipationMiniMonthIndicatorTextHeight / mFragment.mMonthListViewMonthIndicatorTextHeightInEPMode);
		mMonthTextBaseLineScaleDownRatioInEPMode = 1 - 
				(mFragment.mAnticipationMiniMonthIndicatorTextBaseLineY / mFragment.mMonthListViewMonthIndicatorTextBaselineYInEPMode);
		mMonthDayTextHeightDownRatioInEPMode = 1 - 
				(mFragment.mAnticipationMiniMonthDayTextHeight / mFragment.mMonthListMonthDayTextHeightInEPMode);
		
		
		mHeightOfChangePsudoTargetMonthViewToMiniMonthView = mFragment.mAnticipationMiniMonthHeight * 2;
		
		mScaleXAxisTotalRateChange = mMiniMonthScaleFromX - mMiniMonthScaleToX;
		mScaleYAxisTotalRateChange = mMiniMonthScaleFromY - mMiniMonthScaleToY;		
		
		mEMPsudoMonthListViewScaleAnimator = ValueAnimator.ofFloat(mMiniMonthScaleFromY, mMiniMonthScaleToY);
		mEMPsudoMonthListViewScaleAnimator.setDuration(mTargetMonthScaleDuration);
		//ITEAnimInterpolator scaleInterpolator = new ITEAnimInterpolator(mTargetMonthHeight, mEMPsudoMonthListViewScaleAnimator);
		//mEMPsudoMonthListViewScaleAnimator.setInterpolator(scaleInterpolator);	
		mEMPsudoMonthListViewScaleAnimator.setInterpolator(new DecelerateInterpolator(2));	
		
		makeLinearFuncitonForTransmation(miniMonthWidth, miniMonthHeight);
		
		mEMPsudoMonthListViewScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float fraction = valueAnimator.getAnimatedFraction();
				float scaleRatioYAxis = (Float) valueAnimator.getAnimatedValue();
				if (scaleRatioYAxis == mMiniMonthScaleFromY)
					return;					
						
				int height = (int) (mFragment.mAnticipationMiniMonthHeight * scaleRatioYAxis);
				if (mPrvScalingTargetMonthHeight == height) {					
					return;
				}				
				
				int width = 0;
				// ���� �������� mTargetMonthSizeRatio�� �۾����� �Ѵ�
				if (height > mHeightOfChangePsudoTargetMonthViewToMiniMonthView) {					
					width = (int) (height * mTargetMonthSizeRatio);
				}
				else {					
					float scaleRatioXAxis = mMiniMonthScaleFromX - (mScaleXAxisTotalRateChange * fraction);
					width = (int) (mFragment.mAnticipationMiniMonthWidth * scaleRatioXAxis);								
				}			
				
				mPrvScalingTargetMonthHeight = height;	
				mPrvScalingTargetMonthWidth = width;
								
				float newCenterX = getPsudoTargetMonthCenterX(fraction);
				float newCenterY = getPsudoTargetMonthCenterY(newCenterX, fraction);
							
				float newLeft = newCenterX - (width / 2);				
				float newTop = newCenterY - (height / 2);
				
				mPsudoTargetMonthView.setTranslationX(newLeft);
				mPsudoTargetMonthView.setTranslationY(newTop);
				
				setScalePsudoTargetMonth(width, height);
				
				mPsudoTargetMonthView.setFraction(fraction);
				
				if (height > mHeightOfChangePsudoTargetMonthViewToMiniMonthView) {
					setScalingEMPsudoMonthView(fraction, width, height);					
				}
				else {
					if (!mChangeMonthListViewLayoutFormat) {
						mPsudoTargetMonthView.launchScaleDrawingMiniMonthView();
						mChangeMonthListViewLayoutFormat = true;	
						
						mPsudoTargetMonthView.setBackgroundColor(Color.TRANSPARENT);
					}
					
					setScalingEMPsudoMiniMonthView(fraction, scaleRatioYAxis, width, height);					
				}					
			}
		});
		mEMPsudoMonthListViewScaleAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {	
				notifyExitAnimCompletion(MONTH_EXIT_ANIM_COMPLETION);
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
	
	TranslateAnimation mEventsListViewExitTranslateAnim;
	long mEventsListViewExitTranslateAnimDuration;
	public void makeEventListViewExitAnim() {
		//mFragment.mEventsListView
		mEventsListViewExitTranslateAnim = new TranslateAnimation(
				Animation.ABSOLUTE, 0, //fromXValue 
				Animation.ABSOLUTE, 0,   //toXValue
                Animation.RELATIVE_TO_SELF, 0, 
                Animation.RELATIVE_TO_SELF, 1);
		mEventsListViewExitTranslateAnim.setDuration(mEventsListViewExitTranslateAnimDuration);
		//mEventsListViewExitTranslateAnim.setFillAfter(true);
		//mEventsListViewExitTranslateAnim.setFillEnabled(true);
		
		mEventsListViewExitTranslateAnim.setAnimationListener(mEventsListViewExitTranslateAnimListener);
	}
	AnimationListener mEventsListViewExitTranslateAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mFragment.mEventsListView.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}
		
	};
	
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
	
	float mIncrementXOfLinearFuncitonForTransmation;
	float mIncrementYOfLinearFuncitonForTransmation;
	
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
		
		//int prvTop = (int) mPsudoTargetMonthView.getY();
				
		mPsudoTargetMonthCenterX = mFragment.mListView.getWidth() / 2;
		mPsudoTargetMonthCenterY = mTargetMonthTop + (mTargetMonthHeight / 2);
		
		float incrementX = mPsudoTargetMonthCenterX - mMiniTargetMonthCenterX;
		float incrementY = mPsudoTargetMonthCenterY - mMiniTargetMonthCenterY;
		mIncrementXOfLinearFuncitonForTransmation = Math.abs(incrementX);
		mIncrementYOfLinearFuncitonForTransmation = Math.abs(incrementY);
		
		mSlopeOfLinearFuncitonForTransmation = incrementY / incrementX;
		mInterceptOfLinearFuncitonForTransmation = mPsudoTargetMonthCenterY - (mSlopeOfLinearFuncitonForTransmation * mPsudoTargetMonthCenterX);
		
		if (incrementX > 0) {
			mXValueScalarType = X_CONSTANT_DECREMENT;
		}
		else if (incrementX < 0){
			mXValueScalarType = X_CONSTANT_INCREMENT;
		}
		else {
			mXValueScalarType = NO_X_CONTANT_RATE;
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
	
	public float getPsudoTargetMonthCenterX(float fraction) {
		float newCenterX = 0;
		
		// psudoTargetMonth�� center �������� left�� ��ġ��
		if (mXValueScalarType == X_CONSTANT_DECREMENT) {
			newCenterX = mPsudoTargetMonthCenterX - (mIncrementXOfLinearFuncitonForTransmation * fraction);			
		} // psudoTargetMonth�� center �������� right�� ��ġ��
		else if (mXValueScalarType == X_CONSTANT_INCREMENT) {
			newCenterX = mPsudoTargetMonthCenterX + (mIncrementXOfLinearFuncitonForTransmation * fraction);			
		}
		else {
			// 2,5,8,11 mini month�� �߾ӿ� ��ġ�Ѵ�
			newCenterX = mPsudoTargetMonthCenterX;							
		}	
		
		return newCenterX;
	}
	
	public float getPsudoTargetMonthCenterY(float newCenterX, float fraction) {
		float newCenterY = 0;
		
		if (mXValueScalarType == X_CONSTANT_DECREMENT) {			
			newCenterY = (mSlopeOfLinearFuncitonForTransmation * newCenterX) + mInterceptOfLinearFuncitonForTransmation;
		} // psudoTargetMonth�� center �������� right�� ��ġ��
		else if (mXValueScalarType == X_CONSTANT_INCREMENT) {			
			newCenterY = (mSlopeOfLinearFuncitonForTransmation * newCenterX) + mInterceptOfLinearFuncitonForTransmation;
		}
		else {
			// 2,5,8,11 mini month�� �߾ӿ� ��ġ�Ѵ�			
			if (mYValueScalarType == Y_CONSTANT_DECREMENT) {						
				newCenterY = mPsudoTargetMonthCenterY - (mIncrementYOfLinearFuncitonForTransmation * fraction);
			} // psudoTargetMonth�� center �������� right�� ��ġ��
			else if (mYValueScalarType == Y_CONSTANT_INCREMENT) {						
				newCenterY = mPsudoTargetMonthCenterY + (mIncrementYOfLinearFuncitonForTransmation * fraction);
			}
			else {
				// 2,5,8,11�� mini month�� �߾ӿ� ��ġ�Ѵ�
				newCenterY = mPsudoTargetMonthCenterY;					
			}					
		}	
		
		return newCenterY;
	}
	
	public void setScalePsudoTargetMonth(int width, int height) {
		RelativeLayout.LayoutParams psudoMonthListViewParams = (RelativeLayout.LayoutParams) mPsudoTargetMonthView.getLayoutParams();
		psudoMonthListViewParams.width = width;
		psudoMonthListViewParams.height = height;				
		mPsudoTargetMonthView.setLayoutParams(psudoMonthListViewParams);	
	}
	
	public void setScalingNMPsudoMonthView(float scaleRatioYAxisDelta, int width, int height) {		
		HashMap<String, Integer> miniMonthDrawingParams = new HashMap<String, Integer>();		
		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);	
		
		float scaleDownScalar = mScaleDownMonthIndicatorHeight * scaleRatioYAxisDelta;
		int monthIndicatorHeight = (int) (mFragment.mMonthListViewMonthIndicatorHeight - scaleDownScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT, monthIndicatorHeight);		
		
		// miniMonthScalingMonthIndicatorTextHeight�� 
		// mMonthListViewMonthIndicatorHeight�� 0.875 �̴�
		// �׷��Ƿ� month indicator text�� animation ���� 0.125 ��ŭ ��ҵǾ�� �Ѵ�
		float deductionTextHeightRate = mMonthTextHeightScaleDownRatio * scaleRatioYAxisDelta;
		float textHeightScaleRatio = 1 - deductionTextHeightRate;
		int miniMonthScalingMonthIndicatorTextHeight = (int) (mFragment.mMonthListViewMonthIndicatorTextHeight * textHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthScalingMonthIndicatorTextHeight);		
		
		// miniMonthScalingMonthIndicatorTextBaseLineY��  
		// mMonthListViewMonthIndicatorTextBaselineY�� 0.75 �̴�
		// �׷��Ƿ� month indicator text baseline y�� animation ���� 0.25 ��ŭ ��ҵǾ�� �Ѵ�
		float deductionTextBaseLineRate = mMonthTextBaseLineScaleDownRatio * scaleRatioYAxisDelta;
		float textBaseLineScaleRatio = 1 - deductionTextBaseLineRate;
		int miniMonthScalingMonthIndicatorTextBaseLineY = (int) (mFragment.mMonthListViewMonthIndicatorTextBaselineY * textBaseLineScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, miniMonthScalingMonthIndicatorTextBaseLineY);	
		
		//mScaleDownMonthNormalWeekHeight = (int) (mFragment.mListView.getNMNormalWeekHeight() - mFragment.mAnticipationMiniMonthDayTextBaseLineY);	
		// mini month�� normal week height�� ��� mini month day text baseline y���� �����ϴ�
		// :mini month�� normal week�� ���� month day text�� drawing �ϱ� ������...		
		scaleDownScalar = mScaleDownMonthNormalWeekHeight * scaleRatioYAxisDelta;		
		int normalWeekHeight = (int) (mFragment.mMonthListViewNormalWeekHeight - scaleDownScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT, normalWeekHeight);		
				
		float deductionMonthDayTextHeightRate = mMonthDayTextHeightDownRatio * scaleRatioYAxisDelta;
		float monthDayTextHeightScaleRatio = 1 - deductionMonthDayTextHeightRate;
		int monthScalingMonthDayTextHeight = (int) (mFragment.mMonthListMonthDayTextHeight * monthDayTextHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, monthScalingMonthDayTextHeight);

		int lastMiniMonthScalingMonthDayTextBaseLineY = (int) (normalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);	
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, lastMiniMonthScalingMonthDayTextBaseLineY);
		
		float totalChangeValue = mTargetMonthHeight - mHeightOfChangePsudoTargetMonthViewToMiniMonthView;
		float changeScalarHeight = height - mHeightOfChangePsudoTargetMonthViewToMiniMonthView;
		int weekUpperlineAlpha = (int) (255 * (changeScalarHeight / totalChangeValue)); 
		miniMonthDrawingParams.put(PsudoTargetMonthView.PSUDO_MINIMONTH_PARAMS_WEEK_UPPERLINE_ALPHA_VALUE, weekUpperlineAlpha);
		
		mPsudoTargetMonthView.setMonthScaleDrawingParams(miniMonthDrawingParams);		
	}	
	
	public void setScalingNMPsudoMiniMonthView(float fraction, float scaleRatioYAxis, int width, int height) {		
				
		HashMap<String, Integer> miniMonthDrawingParams = new HashMap<String, Integer>();		
		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);		
		
		float deductionMonthIndicatorTextHeightRate = mMonthTextHeightScaleDownRatio * fraction;
		float monthIndicatorTextHeightRateHeightScaleRatio = 1 - deductionMonthIndicatorTextHeightRate;		
		int miniMonthScalingMonthIndicatorTextHeight = (int) (mFragment.mMonthListViewMonthIndicatorTextHeight * monthIndicatorTextHeightRateHeightScaleRatio);		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthScalingMonthIndicatorTextHeight);		
		
		float deductionMonthIndicatorTextBaseLineRate = mMonthTextBaseLineScaleDownRatio * fraction;
		float monthIndicatorTextBaseLineScaleRatio = 1 - deductionMonthIndicatorTextBaseLineRate;		
		int miniMonthScalingMonthIndicatorTextBaseLineY = (int) (mFragment.mMonthListViewMonthIndicatorTextBaselineY * monthIndicatorTextBaseLineScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, miniMonthScalingMonthIndicatorTextBaseLineY);			
				
		float deductionMonthDayTextHeightRate = mMonthDayTextHeightDownRatio * fraction;
		float monthDayTextHeightScaleRatio = 1 - deductionMonthDayTextHeightRate;
		int monthScalingMonthDayTextHeight = (int) (mFragment.mMonthListMonthDayTextHeight * monthDayTextHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, monthScalingMonthDayTextHeight);
		
		int miniMonthDayTextBaseLineY = (int) (mFragment.mAnticipationMiniMonthDayTextBaseLineY * scaleRatioYAxis);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthDayTextBaseLineY);		
		
		int psudoMiniMonthAlpahValue = (int) (255 * fraction);
		miniMonthDrawingParams.put(PsudoTargetMonthView.PSUDO_MINIMONTH_PARAMS_TEXT_ALPHA_VALUE, psudoMiniMonthAlpahValue);
		mPsudoTargetMonthView.setMiniMonthScaleDrawingParams(miniMonthDrawingParams);
	}
	
	
	public void setScalingEMPsudoMiniMonthView(float fraction, float scaleRatioYAxis, int width, int height) {		
				
		HashMap<String, Integer> miniMonthDrawingParams = new HashMap<String, Integer>();		
		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);		
		
		float deductionMonthIndicatorTextHeightRate = mMonthTextHeightScaleDownRatioInEPMode * fraction;
		float monthIndicatorTextHeightRateHeightScaleRatio = 1 - deductionMonthIndicatorTextHeightRate;
		int miniMonthScalingMonthIndicatorTextHeight = (int) (mFragment.mMonthListViewMonthIndicatorTextHeightInEPMode * monthIndicatorTextHeightRateHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthScalingMonthIndicatorTextHeight);
		
		float deductionMonthIndicatorTextBaseLineRate = mMonthTextBaseLineScaleDownRatioInEPMode * fraction;
		float monthIndicatorTextBaseLineScaleRatio = 1 - deductionMonthIndicatorTextBaseLineRate;
		int miniMonthScalingMonthIndicatorTextBaseLineY = (int) (mFragment.mMonthListViewMonthIndicatorTextBaselineYInEPMode * monthIndicatorTextBaseLineScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, miniMonthScalingMonthIndicatorTextBaseLineY);
		
		float deductionMonthDayTextHeightRate = mMonthDayTextHeightDownRatioInEPMode * fraction;
		float monthDayTextHeightScaleRatio = 1 - deductionMonthDayTextHeightRate;
		int monthScalingMonthDayTextHeight = (int) (mFragment.mMonthListMonthDayTextHeightInEPMode * monthDayTextHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, monthScalingMonthDayTextHeight);
		
		int miniMonthDayTextBaseLineY = (int) (mFragment.mAnticipationMiniMonthDayTextBaseLineY * scaleRatioYAxis);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthDayTextBaseLineY);		
		
		int psudoMiniMonthAlpahValue = (int) (255 * fraction);
		miniMonthDrawingParams.put(PsudoTargetMonthView.PSUDO_MINIMONTH_PARAMS_TEXT_ALPHA_VALUE, psudoMiniMonthAlpahValue);
		
		int viewAlpha = 255- (int) (255 * fraction);		
		miniMonthDrawingParams.put(PsudoTargetMonthView.PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE, viewAlpha);
		
		mPsudoTargetMonthView.setMiniMonthScaleDrawingParams(miniMonthDrawingParams);
	}
		
	
	public void setScalingEMPsudoMonthView(float fraction, int width, int height) {		
		HashMap<String, Integer> miniMonthDrawingParams = new HashMap<String, Integer>();		
		
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_WIDTH, width);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_HEIGHT, height);		
		
		float scaleDownScalar = mScaleDownMonthIndicatorHeight * fraction;
		int monthIndicatorHeight = (int) (mFragment.mMonthListViewMonthIndicatorHeightInEPMode - scaleDownScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_HEIGHT, monthIndicatorHeight);	
		
		// miniMonthScalingMonthIndicatorTextHeight�� 
		// mMonthListViewMonthIndicatorHeight�� 0.875 �̴�
		// �׷��Ƿ� month indicator text�� animation ���� 0.125 ��ŭ ��ҵǾ�� �Ѵ�
		// :�� ��Ȳ�� normal mode ��Ȳ������ ����� ���̴�
		//  expand mode������ mMonthTextScaleDownRatioInEPMode�� �����ؾ� �Ѵ�
		float deductionMonthIndicatorTextHeightRate = mMonthTextHeightScaleDownRatioInEPMode * fraction;
		float monthIndicatorTextHeightRateHeightScaleRatio = 1 - deductionMonthIndicatorTextHeightRate;
		int miniMonthScalingMonthIndicatorTextHeight = (int) (mFragment.mMonthListViewMonthIndicatorTextHeightInEPMode * monthIndicatorTextHeightRateHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_HEIGHT, miniMonthScalingMonthIndicatorTextHeight);	
		
		float deductionMonthIndicatorTextBaseLineRate = mMonthTextBaseLineScaleDownRatioInEPMode * fraction;
		float monthIndicatorTextBaseLineScaleRatio = 1 - deductionMonthIndicatorTextBaseLineRate;
		int lastMiniMonthScalingMonthIndicatorTextBaseLineY = (int) (mFragment.mMonthListViewMonthIndicatorTextBaselineYInEPMode * monthIndicatorTextBaseLineScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHINDICATOR_TEXT_BASELINE_Y, lastMiniMonthScalingMonthIndicatorTextBaseLineY);	
		
		scaleDownScalar = mScaleDownMonthNormalWeekHeight * fraction;		
		int normalWeekHeight = (int) (mFragment.mMonthListViewNormalWeekHeightInEPMode - scaleDownScalar);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_NORMAL_WEEK_HEIGHT, normalWeekHeight);		
				
		float deductionMonthDayTextHeightRate = mMonthDayTextHeightDownRatioInEPMode * fraction;
		float monthDayTextHeightScaleRatio = 1 - deductionMonthDayTextHeightRate;
		int monthScalingMonthDayTextHeight = (int) (mFragment.mMonthListMonthDayTextHeightInEPMode * monthDayTextHeightScaleRatio);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_HEIGHT, monthScalingMonthDayTextHeight);

		int miniMonthScalingMonthDayTextBaseLineY = (int) (normalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_EMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
		miniMonthDrawingParams.put(YearsAdapter.MONTHS_PARAMS_MINIMONTH_MONTHDAY_TEXT_BASELINE_Y, miniMonthScalingMonthDayTextBaseLineY);
		
		float totalChangeValue = mTargetMonthHeight - mHeightOfChangePsudoTargetMonthViewToMiniMonthView;
		float changeScalarHeight = height - mHeightOfChangePsudoTargetMonthViewToMiniMonthView;
		int weekUpperlineAlpha = (int) (255 * (changeScalarHeight / totalChangeValue)); 
		miniMonthDrawingParams.put(PsudoTargetMonthView.PSUDO_MINIMONTH_PARAMS_WEEK_UPPERLINE_ALPHA_VALUE, weekUpperlineAlpha);
		
		int viewAlpha = 255- (int) (255 * fraction);		
		miniMonthDrawingParams.put(PsudoTargetMonthView.PSUDO_MINIMONTH_PARAMS_EVENT_CIRCLE_DISAPPEAR_ALPHA_VALUE, viewAlpha);
		
		mPsudoTargetMonthView.setMonthScaleDrawingParams(miniMonthDrawingParams);		
	}	
	
	
		
	float mSelectedMiniMonthLeft;
	float mSelectedMiniMonthTop;
	float mSelectedMiniMonthBottom;
	int mSelectedMiniMonthColumnIndex;
	public void makeMiniMonthDimension() {
		//mSelectedYear.	
		if ( (mSelectedYear.get(Calendar.MONTH) == 0) ||
    			(mSelectedYear.get(Calendar.MONTH) == 3) || 
    			(mSelectedYear.get(Calendar.MONTH) == 6) || 
    			(mSelectedYear.get(Calendar.MONTH) == 9) ) {
			mSelectedMiniMonthColumnIndex = YearMonthTableLayout.FIRST_COLUMN_MONTH_INDEX;
			mSelectedMiniMonthLeft = mFragment.mAnticipationMiniMonthLeftMargin;
    	}
    	else if ( (mSelectedYear.get(Calendar.MONTH) == 1) ||
    			(mSelectedYear.get(Calendar.MONTH) == 4) || 
    			(mSelectedYear.get(Calendar.MONTH) == 7) || 
    			(mSelectedYear.get(Calendar.MONTH) == 10) ) {
    		mSelectedMiniMonthColumnIndex = YearMonthTableLayout.SECOND_COLUMN_MONTH_INDEX;
    		mSelectedMiniMonthLeft = mFragment.mAnticipationMiniMonthLeftMargin + 
    				mFragment.mAnticipationMiniMonthWidth + 
    				mFragment.mAnticipationMiniMonthLeftMargin;
    	}
    	else if ( (mSelectedYear.get(Calendar.MONTH) == 2) ||
    			(mSelectedYear.get(Calendar.MONTH) == 5) || 
    			(mSelectedYear.get(Calendar.MONTH) == 8) || 
    			(mSelectedYear.get(Calendar.MONTH) == 11) ) {
    		mSelectedMiniMonthColumnIndex = YearMonthTableLayout.THIRD_COLUMN_MONTH_INDEX;
    		mSelectedMiniMonthLeft = mFragment.mAnticipationMiniMonthLeftMargin + 
    				mFragment.mAnticipationMiniMonthWidth + 
    				mFragment.mAnticipationMiniMonthLeftMargin + 
    				mFragment.mAnticipationMiniMonthWidth + 
    				mFragment.mAnticipationMiniMonthLeftMargin;
    	}    
		///////////////////////////////////////////////////////////////////////////////////////////////
		
		if ( (mSelectedYear.get(Calendar.MONTH) == 0) ||
    			(mSelectedYear.get(Calendar.MONTH) == 1) || 
    			(mSelectedYear.get(Calendar.MONTH) == 2) ) {
			mSelectedMiniMonthTop = mFragment.mAnticipationYearIndicatorHeight;
			mSelectedMiniMonthBottom = mSelectedMiniMonthTop + mFragment.mAnticipationMiniMonthHeight;
    	}
		else if ( (mSelectedYear.get(Calendar.MONTH) == 3) ||
    			(mSelectedYear.get(Calendar.MONTH) == 4) || 
    			(mSelectedYear.get(Calendar.MONTH) == 5) ) {
			mSelectedMiniMonthTop = mFragment.mAnticipationYearIndicatorHeight + mFragment.mAnticipationMiniMonthHeight;
			mSelectedMiniMonthBottom = mSelectedMiniMonthTop + mFragment.mAnticipationMiniMonthHeight;
    	}
		else if ( (mSelectedYear.get(Calendar.MONTH) == 6) ||
    			(mSelectedYear.get(Calendar.MONTH) == 7) || 
    			(mSelectedYear.get(Calendar.MONTH) == 8) ) {
			mSelectedMiniMonthTop = mFragment.mAnticipationYearIndicatorHeight + (mFragment.mAnticipationMiniMonthHeight * 2);
			mSelectedMiniMonthBottom = mSelectedMiniMonthTop + mFragment.mAnticipationMiniMonthHeight;
    	}
		else if ( (mSelectedYear.get(Calendar.MONTH) == 9) ||
    			(mSelectedYear.get(Calendar.MONTH) == 10) || 
    			(mSelectedYear.get(Calendar.MONTH) == 11) ) {
			mSelectedMiniMonthTop = mFragment.mAnticipationYearIndicatorHeight + (mFragment.mAnticipationMiniMonthHeight * 3);
			mSelectedMiniMonthBottom = mSelectedMiniMonthTop + mFragment.mAnticipationMiniMonthHeight;
    	}		
	}
	
	TranslateAnimation mPrvNonTargetMonthTransAnim;
	TranslateAnimation mNextNonTargetMonthTransAnim;
	public void makeNonTargetMonthsExitAnimation() {
		
		switch(mMonthsLayout) {
		case ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE:
			//if (INFO) Log.i(TAG, "ADDED_BOTH_PRV_AND_NEXT_MONTH_AT_NORMALMODE");
			
			mPrvNonTargetMonthTransAnim = new TranslateAnimation(
					Animation.ABSOLUTE, 0, //fromXValue 
					Animation.ABSOLUTE, 0,   //toXValue
	                Animation.RELATIVE_TO_SELF, 0, 
	                Animation.RELATIVE_TO_SELF, -1);
			mPrvNonTargetMonthTransAnim.setDuration(mNonTargetMonthScaleDuration);			
			mPrvNonTargetMonthTransAnim.setAnimationListener(mPrvNonTargetMonthTransAnimListener);
			
			mNextNonTargetMonthTransAnim = new TranslateAnimation(
					Animation.ABSOLUTE, 0, //fromXValue 
					Animation.ABSOLUTE, 0,   //toXValue
	                Animation.RELATIVE_TO_SELF, 0, 
	                Animation.RELATIVE_TO_SELF, 1);
			mNextNonTargetMonthTransAnim.setDuration(mNonTargetMonthScaleDuration);			
			mNextNonTargetMonthTransAnim.setAnimationListener(mNextNonTargetMonthTransAnimListener);
			break;
		case ADDED_ONLY_PRV_MONTH_AT_NORMALMODE:
			//if (INFO) Log.i(TAG, "ADDED_ONLY_PRV_MONTH_AT_NORMALMODE");
			
			mPrvNonTargetMonthTransAnim = new TranslateAnimation(
					Animation.ABSOLUTE, 0, //fromXValue 
					Animation.ABSOLUTE, 0,   //toXValue
					Animation.RELATIVE_TO_SELF, 0, 
	                Animation.RELATIVE_TO_SELF, -1);
			mPrvNonTargetMonthTransAnim.setDuration(mNonTargetMonthScaleDuration);			
			mPrvNonTargetMonthTransAnim.setAnimationListener(mPrvNonTargetMonthTransAnimListener);
			break;
		case ADDED_ONLY_NEXT_MONTH_AT_NORMALMODE:
			//if (INFO) Log.i(TAG, "ADDED_ONLY_NEXT_MONTH_AT_NORMALMODE");
			mNextNonTargetMonthTransAnim = new TranslateAnimation(
					Animation.ABSOLUTE, 0, //fromXValue 
					Animation.ABSOLUTE, 0,   //toXValue
	                Animation.RELATIVE_TO_SELF, 0, 
	                Animation.RELATIVE_TO_SELF, 1);
			mNextNonTargetMonthTransAnim.setDuration(mNonTargetMonthScaleDuration);			
			mNextNonTargetMonthTransAnim.setAnimationListener(mNextNonTargetMonthTransAnimListener);
			break;
		case EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE:
			//if (INFO) Log.i(TAG, "EXIST_ONLY_TARGET_MONTH_AT_NORMALMODE");
			break;		
		}
	}
	
	AnimationListener mPrvNonTargetMonthTransAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoPrvMonthView.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}
		
	};
	
	AnimationListener mNextNonTargetMonthTransAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoNextMonthView.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}
		
	};
	
	
	public int getWeekPattern(int position) {
		int week_pattern = MonthWeekView.NORMAL_WEEK_PATTERN;
		int julianMonday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(position);//Time.getJulianMondayFromWeeksSinceEpoch(position);
		Calendar timeOfFirstDayOfThisWeek = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);
		timeOfFirstDayOfThisWeek.setFirstDayOfWeek(mFragment.mFirstDayOfWeek);
		long julianMondayMills = ETime.getMillisFromJulianDay(julianMonday, mTimeZone, mFirstDayOfWeek);
		timeOfFirstDayOfThisWeek.setTimeInMillis(julianMondayMills);//timeOfFirstDayOfThisWeek.setJulianDay(julianMonday);
		ETime.adjustStartDayInWeek(timeOfFirstDayOfThisWeek);				     
		
		Calendar timeOfLastDayOfThisWeek = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZoneString);		
		timeOfLastDayOfThisWeek.setTimeInMillis(timeOfFirstDayOfThisWeek.getTimeInMillis());//timeOfLastDayOfThisWeek.setJulianDay(firstJulianDayOfThisWeek + 6);
		timeOfLastDayOfThisWeek.add(Calendar.DAY_OF_MONTH, 6);
		
		int maxMonthDay = timeOfLastDayOfThisWeek.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		if (timeOfFirstDayOfThisWeek.get(Calendar.MONTH) != timeOfLastDayOfThisWeek.get(Calendar.MONTH)) {
			week_pattern = MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN;
		}
		else {
			if ( (timeOfFirstDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == 1) /*&& (timeOfFirstDayOfThisWeek.weekDay == mFirstDayOfWeek)*/ ) {
				week_pattern = MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN; 
			}
			else if (timeOfLastDayOfThisWeek.get(Calendar.DAY_OF_MONTH) == maxMonthDay) {	    	
				week_pattern = MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN;
			}		
		}
		
		return week_pattern;
	}
	
	public class PrvMonthWeekInfo {
		int topOfVisibleFirstWeek;
		//int firstJulainDayOfVisibleFirstWeek;		
		int positionOfVisibleFirstWeek;
		int prvMonthWeeks;
		int prvMonthWeekRegionHeight;
		
		public PrvMonthWeekInfo() {
			
		}
		
	}
	
	public final int MONTH_EXIT_ANIM_COMPLETION = 1;
	public final int CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION = 2;
	private final int ENTIRE_ANIMATIONS_COMPLETION = MONTH_EXIT_ANIM_COMPLETION | 
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
		int viewType = ViewType.YEAR;
		mController.setTime(mSelectedYear.getTimeInMillis());
		mController.sendEvent(mFragment, EventType.GO_TO, mSelectedYear, null, -1, viewType);
		
	}
}
