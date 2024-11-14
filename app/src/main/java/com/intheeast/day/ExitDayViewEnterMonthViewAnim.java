package com.intheeast.day;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.day.DayFragment.ManageEventsPerWeek;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthAdapter;
import com.intheeast.month.MonthWeekView;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
//import android.text.format.Time;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.RelativeLayout;

public class ExitDayViewEnterMonthViewAnim {
	private static String TAG = "ExitDayViewEnterMonthViewAnim";
    private static boolean INFO = true;
    
	public final String[] EVENT_PROJECTION = new String[] {
        Instances.TITLE,                 // 0        
        Instances.EVENT_ID,              // 1
        Instances.BEGIN,                 // 2
        Instances.END,                   // 3
        Instances._ID,                   // 4
        Instances.START_DAY,             // 5
        Instances.END_DAY,               // 6        
	};
	
	// The indices for the projection array above.
    private final int PROJECTION_TITLE_INDEX = 0;    
    private final int PROJECTION_EVENT_ID_INDEX = 1;
    private final int PROJECTION_BEGIN_INDEX = 2;
    private final int PROJECTION_END_INDEX = 3;
    
    private final int PROJECTION_START_DAY_INDEX = 5;
    private final int PROJECTION_END_DAY_INDEX = 6;
    
	
	private final String WHERE_CALENDARS_VISIBLE = Calendars.VISIBLE + "=1";
    private final String INSTANCES_SORT_ORDER = Instances.START_DAY + ","
            + Instances.START_MINUTE + "," + Instances.TITLE;
    
    ECalendarApplication mApp;
	Context mContext;
	DayFragment mFragment;
	
	CalendarController mController;
	RelativeLayout mDayViewLayout;
	CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
    LinearLayout mPsudoMonthListViewUpperRegionContainer;
    LinearLayout mPsudoMonthListViewLowerRegionContainer;
    View mPsudoSecondaryMonthDayAndDateIndicatorRegion;
    PsudoMonthWeekEventsView mSelectedWeekView;
    
    int mFirstDayOfWeek;
    final int mDaysPerWeek = 7;
    final int mNumWeeks = 5;
    Calendar mSelectedDayTime;
    int mSelectedJulianDay;
    int mSelectedWeekPosition;
    int mSelectedWeekPattern;
    int mFirstJulianDayOfSelectedWeek;
    int mLastJulianDayOfSelectedWeek;
    
    
    Calendar mTargetMonthTime;
    //int mFirstJulianDayOfTargetMonthFristWeek;
    //int mLastJulianDayOfTargetMonthFristWeek;    
    int mFirstJulianDayOfTargetMonthLastWeek;
    int mLastJulianDayOfTargetMonthLastWeek;
    int mTargetMonthFirstWeekPosition;
    int mTargetMonthFirstWeekPattern;
    int mTargetMonthLastWeekPosition;    
    
    private Uri mEventUri;
    
    PsudoMonthAdapter mPsudoMonthByWeekAdapter;
    
    private int mFirstJulianDay;
        
    TimeZone mTimeZone;
    long mGMToff;
    private Calendar mTempTime = GregorianCalendar.getInstance();
    private static final int WEEKS_BUFFER = 1;
    
    private Handler mHandler;
    //boolean mHideDeclined;
    boolean mShouldLoad;    
    
    int mWeekNumbersOfTargetMonth;
    int mNextMonthVisibleWeekNumbers = 0;
    
    
	public ExitDayViewEnterMonthViewAnim(ECalendarApplication app, Context context, DayFragment fragment, Calendar selectedDay, int firstJulainDay, int lastJulainDay) {
		mApp = app;
				
		mContext = context;
		mFragment = fragment;
		
		mTimeZone = selectedDay.getTimeZone();
		mGMToff = (mTimeZone.getRawOffset()) / 1000;
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
		mSelectedDayTime = GregorianCalendar.getInstance(mTimeZone);
		mSelectedDayTime.setFirstDayOfWeek(selectedDay.getFirstDayOfWeek());
		mSelectedDayTime.setTimeInMillis(selectedDay.getTimeInMillis());	
		mSelectedJulianDay = ETime.getJulianDay(mSelectedDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
		mTempTime.setTimeZone(mTimeZone);
		mTempTime.setFirstDayOfWeek(selectedDay.getFirstDayOfWeek());
				
		mFirstJulianDayOfSelectedWeek = firstJulainDay;
		mLastJulianDayOfSelectedWeek = lastJulainDay;
		
		init();
	}
	
	public void init() {
		// ù��° �켱 �۾��� �ش� ���� events �����̴�
		// :���� �ش� ���� �� event ������ ��õ�����?...���� ����
		//  �ش��Ͽ� event ���� ������ Ȯ���� �� �ִ� �������� ������ �� ������?
		
		mHandler = new Handler();		
		
		mTargetMonthTime = GregorianCalendar.getInstance(mTimeZone);
		mTargetMonthTime.setTimeInMillis(mSelectedDayTime.getTimeInMillis());
		mTargetMonthTime.set(mTargetMonthTime.get(Calendar.YEAR), mTargetMonthTime.get(Calendar.MONTH), 1);		
		
		mFirstJulianDay = ETime.getJulianDay(mTargetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
		transferEventsToApp();
				
		mController = mFragment.mController;
		mDayViewLayout = mFragment.mDayViewLayout;
		mCalendarViewsSecondaryActionBar = mFragment.mCalendarViewsSecondaryActionBar;
		mPsudoMonthListViewUpperRegionContainer = mFragment.mPsudoMonthListViewUpperRegionContainer;
		mPsudoMonthListViewLowerRegionContainer = mFragment.mPsudoMonthListViewLowerRegionContainer;
		
		makePsudoMonthByWeekAdapter();		
		//launchEventLoading();
		
		//mFragment.mCalendarViewsSecondaryActionBar.setVisibility(View.GONE);
		//mFragment.mDayViewSwitcher.setVisibility(View.GONE);
		//mFragment.mExtendedDayViewSwitcher.setVisibility(View.GONE);/////////////////////////////////////////////////////////////
		
		makePsudoSecondaryMonthDayIndicator();
		
		makeSelectedWeekView();
		
		makeAnimCase();		
		
		configPsudoMonthAdapter();
		
		calcTranslateValueAnimDuration();
		
		mFragment.mUpperActionBar.setSwitchFragmentAnim(ViewType.MONTH, mTargetMonthTime.getTimeInMillis(), mSelectedWeekTranslateDuration, -1, mUpperActionBarSwitchFragmentAnimCompletionCallBack);
		
	}
	
	
	public void startMonthExitAnim() {		
		
		setPsudoSecondaryMonthDayIndicatorAnim(mSelectedWeekTranslateDuration);
		
		setPsudoUpperLisViewValueAnimator(mSelectedWeekTranslateDuration);
		setSelectedWeekViewValueAnimator(mSelectedWeekTranslateDuration);
		setPsudoLowerLisViewValueAnimator(mSelectedWeekTranslateDuration);
		
		setExtendedDayViewSwitcherTranslateAnim(mSelectedWeekTranslateDuration);
		
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start();
		
		//mSelectedWeekViewTranslateAnimator.start();
		mPsudoUpperLisViewTranslateAnimator.start();
		mPsudoLowerLisViewTranslateAnimator.start();
		mFragment.mExtendedDayViewSwitcher.startAnimation(mExtendedDayViewSwitcherTranslateAnimSet);
		//mExtendedDayViewSwitcherTranslateAnimator.start();
		
		mFragment.mUpperActionBar.startSwitchFragmentAnim(-1);
	}
		
	
	
	public void makePsudoMonthByWeekAdapter() {
		mPsudoMonthByWeekAdapter = new PsudoMonthAdapter(mContext, mFragment.mAnticipationMonthDayListViewHeight);
		
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        //weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, mNumWeeks);  // no need      
        weekParams.put(MonthAdapter.WEEK_PARAMS_WEEK_START, mFirstDayOfWeek);        
        weekParams.put(MonthAdapter.WEEK_PARAMS_CURRENT_MONTH_JULIAN_DAY_DISPLAYED,
                ETime.getJulianDay(mSelectedDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
        weekParams.put(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK, mDaysPerWeek);
		
		mPsudoMonthByWeekAdapter.updateParams(weekParams);
	}
	
	public void makeSelectedWeekView() {	
		// day header �ٷ� �ؿ��� ��ġ�ؼ� ����ϱ� �����̴�?
		int marginTop = (int) mContext.getResources().getDimension(R.dimen.day_header_height);
		mSelectedWeekView = new PsudoMonthWeekEventsView(mContext);
		LayoutParams selectedWeekParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		selectedWeekParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		selectedWeekParams.setMargins(0, marginTop, 0, 0);
		mSelectedWeekView.setLayoutParams(selectedWeekParams);	
		mSelectedWeekView.setVisibility(View.GONE);
		mSelectedWeekView.setDayViewSelectedDayTime(mSelectedDayTime);
		mSelectedWeekView.setSelectedDayIndex(mSelectedJulianDay - mFirstJulianDayOfSelectedWeek);		
	}
	
	int mMonthDayIndicatorHeightOfSecondaryActionBar;
	int mDateIndicatorHeightOfSecondaryActionBar;
	int mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight;
	//Paint 
	public void makePsudoSecondaryMonthDayIndicator() {
		//int test = mCalendarViewsSecondaryActionBar.getHeight(); //192
		
		mMonthDayIndicatorHeightOfSecondaryActionBar = mCalendarViewsSecondaryActionBar.getMonthDayIndicatorHeight();
		mDateIndicatorHeightOfSecondaryActionBar = mCalendarViewsSecondaryActionBar.getDateIndicatorHeight();
		mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight = mMonthDayIndicatorHeightOfSecondaryActionBar + mDateIndicatorHeightOfSecondaryActionBar;
		
		
		//mFragment.mCalendarViewsSecondaryActionBar�� month day header view�� date indicator view�� GONE ���·� ������ �Ѵ�
		mCalendarViewsSecondaryActionBar.setMonthDayHeaderLayoutGoneStatus();
		mCalendarViewsSecondaryActionBar.setDateIndicatorTextViewGoneStatus();		
		
		mPsudoSecondaryMonthDayAndDateIndicatorRegion = new View(mContext);		
		LayoutParams PsudoSecondaryMonthDayAndDateIndicatorRegionParams = new LayoutParams(LayoutParams.MATCH_PARENT, 
				/*mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight*/LayoutParams.MATCH_PARENT);
		PsudoSecondaryMonthDayAndDateIndicatorRegionParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		//mCalendarViewsSecondaryActionBar.getHeight()�� ȣ���ϸ� �ȵȴ�
		// dimen/day_header_height�� �����ؾ� �Ѵ�
		int marginTop = (int) mContext.getResources().getDimension(R.dimen.day_header_height); //32
		PsudoSecondaryMonthDayAndDateIndicatorRegionParams.setMargins(0, marginTop, 0, 0);
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setLayoutParams(PsudoSecondaryMonthDayAndDateIndicatorRegionParams);
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setBackgroundColor(mContext.getResources().getColor(R.color.manageevent_actionbar_background));
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setVisibility(View.GONE);
		//mPsudoSecondaryMonthDayAndDateIndicatorRegion.setVisibility(View.VISIBLE); // for test
		mDayViewLayout.addView(mPsudoSecondaryMonthDayAndDateIndicatorRegion);
	}	
	
	int mAnimCase;
	public final int SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE = 1;
	public final int UPPERREGION_AND_SELECTEDWEEK_ANIM_CASE = 2;
	public final int UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_ANIM_CASE = 3;
	public final int UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE = 4;
	public final int UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE = 5;
	
	int mPsudoUpperRegionHeight;
	int mSelectedWeekViewTop;
	int mSelectedWeekVisibleHeight;
	int mPsudoLowerLisViewContainerTop;
	int mPsudoLowerLisViewContainerHeight;
	
	int mPsudoUpperWeekViewCounts = 0;
	public void makeAnimCase() {
		// ������ ���� ���� day view�� ���� ���÷����ϰ� �ִ� Ư�� ��¥[selectedDay]��  TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN ������ �ֿ� ���ԵǾ� �ִٸ�,
    	// Ư�� ��¥ ��, selectedDay�� ���� target month�� 
    	// TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN�� 
    	// previous month 
    	// �Ǵ� next month�� �� �� �ִ�
    	// ���� 2014/11/01�� ���õǾ��ٸ�,
    	// mSelectedWeekPosition�� mTargetMonth[11��]�� FirstWeekPosition�� �ǰ�
    	// 2014/10/31�� �����Ͽ��ٸ�
    	// mSelectedWeekPosition�� mTargetMonth[10��]�� LastWeekPosition�� �ȴ�!!!
		
		// week number�� ����ؾ� �Ѵ�
		// : target month�� ����ؾ� �ϴ��� next month�� ����ؾ� �ϴ����� �Ǵ��ؾ� �Ѵ�
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
    	TempCalendar.set(mTargetMonthTime.get(Calendar.YEAR), mTargetMonthTime.get(Calendar.MONTH), 1);        	   	
    	mWeekNumbersOfTargetMonth = ETime.getWeekNumbersOfMonth(TempCalendar);  	
		
    	// mSelectedWeekPosition��
    	// DayFragment.CalendarViewsSecondaryActionBar.mMonthDayHeaderLayout���� drawing�� week�� week position �� 
		mSelectedWeekPosition = ETime.getWeeksSinceEcalendarEpochFromJulianDay(mFirstJulianDayOfSelectedWeek, mTimeZone, mFirstDayOfWeek);
		mSelectedWeekPattern = PsudoMonthAdapter.calWeekPattern(mSelectedWeekPosition, mTargetMonthTime.getTimeZone().getID(), mFirstDayOfWeek);
		
		mTargetMonthFirstWeekPosition = ETime.getWeeksSinceEcalendarEpochFromMillis(mTargetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
		// 1�� �����ؾ� target month�� last week position ���� ���´�
		mTargetMonthLastWeekPosition = (mTargetMonthFirstWeekPosition + mWeekNumbersOfTargetMonth) - 1;
		
		// 1.mSelectedWeekPosition�� target month�� ù��° ���� week position�� ��ġ��
		if (mSelectedWeekPosition == mTargetMonthFirstWeekPosition) {
			// target month�� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN�� next month week�� next month��
			// �׷��Ƿ� prv month last week �κ��� alpha blending ó�� �Ǹ鼭 ������� ��
			if (mSelectedWeekPattern == PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
				int visibleWeekNumber = 0;				
				// upper region�� month indicator ��				
				PsudoMonthWeekEventsView monthIndicatorViewUpperRegion = new PsudoMonthWeekEventsView(mContext);
				LinearLayout.LayoutParams  monthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				monthIndicatorViewUpperRegion.setLayoutParams(monthIndicatorViewParams);
				HashMap<String, Integer> monthIndicatorDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,							
						PsudoMonthWeekEventsView.MONTH_MONTHIDICATOR_WEEK_PATTERN,
						mPsudoMonthByWeekAdapter.mMonthIndicatorHeight);
				monthIndicatorViewUpperRegion.setWeekParams(monthIndicatorDrawingParams, mSelectedDayTime.getTimeZone().getID());
				//mPsudoMonthByWeekAdapter.sendEventsToView(monthIndicatorViewUpperRegion);
				mPsudoMonthListViewUpperRegionContainer.addView(monthIndicatorViewUpperRegion);
				mPsudoMonthListViewUpperRegionContainer.setVisibility(View.VISIBLE);				
				// upper region�� month indicator�� �����ϱ� ������ upper week count�� 0���� ������	
				mPsudoUpperWeekViewCounts = 0;
				
				int Top = 0;					
				mPsudoUpperRegionHeight = mPsudoMonthByWeekAdapter.mMonthIndicatorHeight;    
				mPsudoUpperRegionVisibleRegionHeight = mPsudoUpperRegionHeight;						
				mPsudoMonthListViewUpperRegionContainer.setY(Top);		
				
				mSelectedWeekViewTop = mPsudoUpperRegionHeight;////////////////////////////////////////////////////////////////////////
				
				// NEXT_MONTH_MONTHIDICATOR_OF_TWO_DIFF_FIRSTWEEK_PATTERN���� under line�� drawing �ϱ� ������
				// selected week���� upper line�� drawing�� �ʿ䰡 ����
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,
						PsudoMonthWeekEventsView.SELECTED_NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN, 
						mPsudoMonthByWeekAdapter.mNormalWeekHeight);
				
				mSelectedWeekView.setVisibility(View.VISIBLE);
				mSelectedWeekView.setY(mSelectedWeekViewTop);
				mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mSelectedDayTime.getTimeZone().getID());
				//mPsudoMonthByWeekAdapter.sendEventsToView(mSelectedWeekView);
				//mSelectedWeekView.setVisibility(View.VISIBLE);
				mDayViewLayout.addView(mSelectedWeekView);
				
				visibleWeekNumber++;
				
				mSelectedWeekVisibleHeight = mPsudoMonthByWeekAdapter.mNormalWeekHeight;
				mPsudoLowerLisViewContainerTop = mPsudoUpperRegionHeight + mSelectedWeekVisibleHeight;				
                
				int nextWeekPositionAfterDrawingSelectedWeek = mSelectedWeekPosition + 1;				
				int canDrawWeekNumber = 6 - visibleWeekNumber;				
				makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, canDrawWeekNumber);		
				//int lastWeekPosittionLowerRegion = (nextWeekPositionAfterDrawingSelectedWeek + canDrawWeekNumber) - 1;
				//makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, lastWeekPosittionLowerRegion, canDrawWeekNumber);				
					
				mPsudoMonthListViewLowerRegionContainer.setVisibility(View.VISIBLE);    		
	    	}
	    	else if (mSelectedWeekPattern == PsudoMonthWeekEventsView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) { // 2015/02���޿��� ������ �߻��Ͽ���
	    		int visibleWeekNumber = 0;
	    		PsudoMonthWeekEventsView monthIndicatorViewUpperRegion = new PsudoMonthWeekEventsView(mContext);
				LinearLayout.LayoutParams  monthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				monthIndicatorViewUpperRegion.setLayoutParams(monthIndicatorViewParams);
				HashMap<String, Integer> monthIndicatorDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,							
						PsudoMonthWeekEventsView.MONTH_MONTHIDICATOR_WEEK_PATTERN,
						mPsudoMonthByWeekAdapter.mMonthIndicatorHeight/*mListView.getNMMonthIndicatorHeight()*/);
				monthIndicatorViewUpperRegion.setWeekParams(monthIndicatorDrawingParams, mSelectedDayTime.getTimeZone().getID());				
				mPsudoMonthListViewUpperRegionContainer.addView(monthIndicatorViewUpperRegion);
				mPsudoMonthListViewUpperRegionContainer.setVisibility(View.VISIBLE);
				
				mPsudoUpperWeekViewCounts = 0;
				
				int Top = 0;					
				mPsudoUpperRegionHeight = mPsudoMonthByWeekAdapter.mMonthIndicatorHeight;    
				mPsudoUpperRegionVisibleRegionHeight = Top + mPsudoUpperRegionHeight;						
				mPsudoMonthListViewUpperRegionContainer.setY(Top);		
				
				mSelectedWeekViewTop = mPsudoUpperRegionHeight;////////////////////////////////////////////////////////////////////////
				
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,
						PsudoMonthWeekEventsView.SELECTED_NORMAL_WEEK_PATTERN, 
						mPsudoMonthByWeekAdapter.mNormalWeekHeight);
				
				mSelectedWeekView.setVisibility(View.VISIBLE);
				mSelectedWeekView.setY(mSelectedWeekViewTop);
				mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mSelectedDayTime.getTimeZone().getID());
				//mPsudoMonthByWeekAdapter.sendEventsToView(mSelectedWeekView);
				//mSelectedWeekView.setVisibility(View.VISIBLE);
				mDayViewLayout.addView(mSelectedWeekView);
				
				visibleWeekNumber++;
				
				mSelectedWeekVisibleHeight = mPsudoMonthByWeekAdapter.mNormalWeekHeight;
				mPsudoLowerLisViewContainerTop = mPsudoUpperRegionHeight + mSelectedWeekVisibleHeight;
				
				int nextWeekPositionAfterDrawingSelectedWeek = mSelectedWeekPosition + 1;
				// ���� �����ϴ�... �ִ� ������� �� �ִ� ���� ������ 6�ַ� �ϴ� ���� ����� ��Ȳ
				// :��Ȯ�ϰ� ����ؾ� �Ѵ�
				int canDrawWeekNumber = 6 - visibleWeekNumber;		
				makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, canDrawWeekNumber);
				//int lastWeekPosittionLowerRegion = (nextWeekPositionAfterDrawingSelectedWeek + canDrawWeekNumber) - 1;
				//makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, lastWeekPosittionLowerRegion, canDrawWeekNumber);				
				mPsudoMonthListViewLowerRegionContainer.setVisibility(View.VISIBLE);    
	    	}
		}
		// 2.mSelectedWeekPosition�� target month�� ������ �ֿ� ��ġ��
		else if (mSelectedWeekPosition == mTargetMonthLastWeekPosition) {
			
			int visibleWeekNumber = makeUpperRegionView();
			mPsudoUpperWeekViewCounts = visibleWeekNumber;
			int nextWeekPositionAfterDrawingSelectedWeek = 0;
			mPsudoMonthListViewUpperRegionContainer.setVisibility(View.VISIBLE);
			
			mSelectedWeekViewTop = mPsudoUpperRegionHeight;
			
			HashMap<String, Integer> selectedWeekDrawingParams = null;
 
			// target month�� TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN�� prv month week�� prv month��
			// �׷��Ƿ� next month�� first week �κ��� alpha blending ó�� �Ǹ鼭 ������� ��
			if (mSelectedWeekPattern == PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {							
				selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,
						PsudoMonthWeekEventsView.SELECTED_PREVIOUS_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN, 
						mPsudoMonthByWeekAdapter.mNormalWeekHeight);
				mSelectedWeekVisibleHeight = mPsudoMonthByWeekAdapter.mNormalWeekHeight;
				nextWeekPositionAfterDrawingSelectedWeek = mSelectedWeekPosition;
				
			}
			// target month�� ������ �ְ� LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN ���¶��
			// �� ����...���� �Ұ� ���� �� ������...
			// 2015�� 2�� 28�Ϸ� �׽�Ʈ �� ���� : ���ݱ��� �� �̻��� ����
			else if (mSelectedWeekPattern == PsudoMonthWeekEventsView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {
				selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,
						PsudoMonthWeekEventsView.SELECTED_LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN, 
						mPsudoMonthByWeekAdapter.mNormalWeekHeight);
				mSelectedWeekVisibleHeight = mPsudoMonthByWeekAdapter.mNormalWeekHeight;		
				nextWeekPositionAfterDrawingSelectedWeek = mSelectedWeekPosition + 1;				
			}			
			
			mSelectedWeekView.setVisibility(View.VISIBLE);
			mSelectedWeekView.setY(mSelectedWeekViewTop);
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mSelectedDayTime.getTimeZone().getID());
			//mPsudoMonthByWeekAdapter.sendEventsToView(mSelectedWeekView);
			//mSelectedWeekView.setVisibility(View.VISIBLE);
			mDayViewLayout.addView(mSelectedWeekView);
			
			visibleWeekNumber++;
			
			mPsudoLowerLisViewContainerTop = mPsudoUpperRegionVisibleRegionHeight + mSelectedWeekVisibleHeight;			
			
			// ������� target month�� visible week�� �� �׷ȴ�
			int canDrawWeekNumber = 6 - visibleWeekNumber;
			if (canDrawWeekNumber == 0)
				return;
			//int lastWeekPosittionLowerRegion = (nextWeekPositionAfterDrawingSelectedWeek + canDrawWeekNumber) - 1;
			//makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, lastWeekPosittionLowerRegion, canDrawWeekNumber);
			makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, canDrawWeekNumber);
			
			mPsudoMonthListViewLowerRegionContainer.setVisibility(View.VISIBLE);			
		}
		// 3.mSelectedWeekPosition�� target month�� ù��°�� ������ �� �߰��� ��ġ��
		else {
					
			int visibleWeekNumber = makeUpperRegionView();
			mPsudoUpperWeekViewCounts = visibleWeekNumber;
			mPsudoMonthListViewUpperRegionContainer.setVisibility(View.VISIBLE);			
			
			mSelectedWeekViewTop = mPsudoUpperRegionHeight;
			
			// MonthDayIndicator��  height[bottom?]�� �ֱ������� üũ�ؼ� 
			// selected week view�� top�� MonthDayIndicator�� bottom���� ũ��
			// upper line�� �׷� ������ �ؾ� �Ѵ�
			// :�ƴ� �� �� �ִ� MonthDayIndicator ���� selected week view�� ������ �ִٸ�? 
			//  ��� �Ǵ��� Ȯ���غ��� �Ѵ�
			HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedWeekPosition,
					PsudoMonthWeekEventsView.SELECTED_NORMAL_WEEK_PATTERN, 
					mPsudoMonthByWeekAdapter.mNormalWeekHeight);
			mSelectedWeekVisibleHeight = mPsudoMonthByWeekAdapter.mNormalWeekHeight;			
			mSelectedWeekView.setVisibility(View.VISIBLE);
			mSelectedWeekView.setY(mSelectedWeekViewTop);
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mSelectedDayTime.getTimeZone().getID());
			//mPsudoMonthByWeekAdapter.sendEventsToView(mSelectedWeekView);
			//mSelectedWeekView.setVisibility(View.VISIBLE);
			mDayViewLayout.addView(mSelectedWeekView);
			
			mPsudoLowerLisViewContainerTop = mPsudoUpperRegionVisibleRegionHeight + mSelectedWeekVisibleHeight;
			
			int nextWeekPositionAfterDrawingSelectedWeek = mSelectedWeekPosition + 1;
			
			visibleWeekNumber++;
			
			int canDrawWeekNumber = 6 - visibleWeekNumber;
			if (canDrawWeekNumber == 0)
				return;
			
			//int lastWeekPosittionLowerRegion = (nextWeekPositionAfterDrawingSelectedWeek + canDrawWeekNumber) - 1;
			//makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, lastWeekPosittionLowerRegion, canDrawWeekNumber);
			makeLowerRegionView(mSelectedWeekPosition, nextWeekPositionAfterDrawingSelectedWeek, canDrawWeekNumber);
			
			mPsudoMonthListViewLowerRegionContainer.setVisibility(View.VISIBLE);	
		}    	
	}
		
	
	int mPsudoUpperRegionVisibleRegionHeight;
	//int mPsudoUpperRegionBottom;
	public int makeUpperRegionView() {	
		int visibleWeekNumber = 0;
		int weeksHeightSum = 0;
		int upperRegionWeekNumbers = mSelectedWeekPosition - mTargetMonthFirstWeekPosition;		
		int upperPosition = mTargetMonthFirstWeekPosition;
		int[] weekPattern = new int[upperRegionWeekNumbers];
		int[] weekHeight = new int[upperRegionWeekNumbers];
		for (int i=0; i<upperRegionWeekNumbers; i++) {
			weekPattern[i] = PsudoMonthAdapter.calWeekPattern(upperPosition++, mTargetMonthTime.getTimeZone().getID(), mFirstDayOfWeek);						
			weekHeight[i] = getHeightByWeekPattern(weekPattern[i]);
			
			if (i==0) {
				if (weekPattern[0] == PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
					// prv Month Last Week�� �߶� ����
					weekPattern[0] = PsudoMonthWeekEventsView.NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN;
					// �Ʒ��� ������ ������ �ִ�
					// �츮�� ���ϴ� weekHeight�� next month�� indicator height + next month�� first normal week height�̴�
					// �׷��� �̸� ������ ����� ���� ������ ���ݰ� height���� ���� FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN�� ������ ����
					// ���ذ� ���⸦...
					weekHeight[0] = getHeightByWeekPattern(PsudoMonthWeekEventsView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN);					
				}				
			}	
			
			weeksHeightSum = weeksHeightSum + weekHeight[i];	
			
			visibleWeekNumber++;
		}		
				
		int Top = 0;					
		mPsudoUpperRegionHeight = weeksHeightSum;    
		mPsudoUpperRegionVisibleRegionHeight = Top + mPsudoUpperRegionHeight;						
		mPsudoMonthListViewUpperRegionContainer.setY(Top);		
		
		upperPosition = mTargetMonthFirstWeekPosition;
				
		for (int i=0; i<upperRegionWeekNumbers; i++) {		
			
			PsudoMonthWeekEventsView psudoMonthUpperWeekEventsView = new PsudoMonthWeekEventsView(mContext);
			LinearLayout.LayoutParams psudoMonthUpperWeekEventsViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			psudoMonthUpperWeekEventsView.setLayoutParams(psudoMonthUpperWeekEventsViewParams);
			
			int pattern = weekPattern[i];
			int height = weekHeight[i];
						
			HashMap<String, Integer> psudoMonthLowerWeekEventsViewDrawingParams = makeWeekViewDrawingParams(upperPosition++,
					pattern,
					height);
			psudoMonthUpperWeekEventsView.setWeekParams(psudoMonthLowerWeekEventsViewDrawingParams, mTargetMonthTime.getTimeZone().getID());
			
			//mPsudoMonthByWeekAdapter.sendEventsToView(psudoMonthUpperWeekEventsView);
			mPsudoMonthListViewUpperRegionContainer.addView(psudoMonthUpperWeekEventsView);			
		}		
		
		return visibleWeekNumber;
	}
	
	public void makeLowerRegionView(int selectedWeekPosition, int firstLowerPositionOffsetfromSelectedWeekPosition) {
		int weeksHeightSum = 0;
		int lowerPosition = selectedWeekPosition + firstLowerPositionOffsetfromSelectedWeekPosition;
		int[] weekPattern = new int[6];
		int[] weekHeight = new int[6];
		
		int end = 6;
		
		for (int i=firstLowerPositionOffsetfromSelectedWeekPosition; i<end; i++) {
			// parent view�� mDayViewLayout�̴� 
			// mDayViewLayout�� height�� ���� �����ó�Ʈ �������� 1006 px��
			// 1006px�� �Ѿ�� �ȵȴ�...�׷��Ƿ� üũ�� �ؾ� �Ѵ�!!!
			// ...
			weekPattern[i] = PsudoMonthAdapter.calWeekPattern(lowerPosition++, mTargetMonthTime.getTimeZone().getID(), mFirstDayOfWeek);
			weekHeight[i] = getHeightByWeekPattern(weekPattern[i]);
			weeksHeightSum = weeksHeightSum + weekHeight[i];		
		}
		
		mPsudoLowerLisViewContainerHeight = weeksHeightSum;	
		mPsudoMonthListViewLowerRegionContainer.setY(mPsudoLowerLisViewContainerTop);
		
		lowerPosition = selectedWeekPosition + firstLowerPositionOffsetfromSelectedWeekPosition;
		
		for (int i= firstLowerPositionOffsetfromSelectedWeekPosition; i<end; i++) {
			PsudoMonthWeekEventsView psudoMonthLowerWeekEventsView = new PsudoMonthWeekEventsView(mContext);
			LinearLayout.LayoutParams psudoMonthLowerWeekEventsViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			psudoMonthLowerWeekEventsView.setLayoutParams(psudoMonthLowerWeekEventsViewParams);
			
			HashMap<String, Integer> psudoMonthLowerWeekEventsViewDrawingParams = makeWeekViewDrawingParams(lowerPosition++,
					weekPattern[i],
					weekHeight[i]);
			psudoMonthLowerWeekEventsView.setWeekParams(psudoMonthLowerWeekEventsViewDrawingParams, mTargetMonthTime.getTimeZone().getID());
			
			//mPsudoMonthByWeekAdapter.sendEventsToView(psudoMonthLowerWeekEventsView);
			mPsudoMonthListViewLowerRegionContainer.addView(psudoMonthLowerWeekEventsView);
		}	
	}
	
	int mPsudoLowerWeekViewCounts = 0;
	private void makeLowerRegionView(int selectedWeekPosition, int firstWeekPositionOfLowerRegion, int weekNumbers) {
		int weeksHeightSum = 0;
		int lowerPosition = firstWeekPositionOfLowerRegion;	
		mPsudoLowerWeekViewCounts = weekNumbers;
		
		int[] weekPattern = new int[weekNumbers];
		int[] weekHeight = new int[weekNumbers];
		
		for (int i=0; i<weekNumbers; i++) {
			weekPattern[i] = PsudoMonthAdapter.calWeekPattern(lowerPosition++, mTargetMonthTime.getTimeZone().getID(), mFirstDayOfWeek);		
			weekHeight[i] = getHeightByWeekPattern(weekPattern[i]);					

			if (i == 0) {
				if ( (weekPattern[0] == PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) && 
						(selectedWeekPosition == firstWeekPositionOfLowerRegion) ){					
					// prv Month Last Week�� �߶� ����
					weekPattern[0] = PsudoMonthWeekEventsView.NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN;
					weekHeight[0] = getHeightByWeekPattern(PsudoMonthWeekEventsView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN);	
				}				
			}
			
			weeksHeightSum = weeksHeightSum + weekHeight[i];
		}
		
		mPsudoLowerLisViewContainerHeight = weeksHeightSum;
								
		mPsudoMonthListViewLowerRegionContainer.setY(mPsudoLowerLisViewContainerTop);
		
		lowerPosition = firstWeekPositionOfLowerRegion;
		for (int i=0; i<weekNumbers; i++) {
			PsudoMonthWeekEventsView psudoMonthLowerWeekEventsView = new PsudoMonthWeekEventsView(mContext);
			LinearLayout.LayoutParams psudoMonthLowerWeekEventsViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			psudoMonthLowerWeekEventsView.setLayoutParams(psudoMonthLowerWeekEventsViewParams);
			
			HashMap<String, Integer> psudoMonthLowerWeekEventsViewDrawingParams = makeWeekViewDrawingParams(lowerPosition++,
					weekPattern[i],
					weekHeight[i]);
			psudoMonthLowerWeekEventsView.setWeekParams(psudoMonthLowerWeekEventsViewDrawingParams, mTargetMonthTime.getTimeZone().getID());
			
			//mPsudoMonthByWeekAdapter.sendEventsToView(psudoMonthLowerWeekEventsView);
			mPsudoMonthListViewLowerRegionContainer.addView(psudoMonthLowerWeekEventsView);
		}		
	}
	
	// PREVIOUS_MONTH_WEEK_OF_TWO_DIFF_FIRSTWEEK_PATTERN�� �������� �ʾƵ� ��?
	public int getHeightByWeekPattern(int weekPattern) {
		int weekHeight = 0;
		switch(weekPattern) {
		case PsudoMonthWeekEventsView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN:
			weekHeight = mPsudoMonthByWeekAdapter.mTwoMonthWeekHeight;
			break;
		case PsudoMonthWeekEventsView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN:
			weekHeight = mPsudoMonthByWeekAdapter.mFirstWeekHeight;
			break;		
		case PsudoMonthWeekEventsView.NEXT_MONTH_WEEK_OF_TWO_DIFF_MONTH_WEEK_PATTERN:			
		case PsudoMonthWeekEventsView.NORMAL_WEEK_PATTERN:
		case PsudoMonthWeekEventsView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN:
			weekHeight = mPsudoMonthByWeekAdapter.mNormalWeekHeight;
			break;
		default:
			break;
		}
		return weekHeight;
	}
	
	public HashMap<String, Integer> makeWeekViewDrawingParams(int weekPosition, int weekPattern, int weekHeight) {
		HashMap<String, Integer> weekDrawingParams = new HashMap<String, Integer>();                               
        
		// fix params
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, mPsudoMonthByWeekAdapter.mAnticipationListViewHeight);		
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, mPsudoMonthByWeekAdapter.mMonthIndicatorHeight);
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, mPsudoMonthByWeekAdapter.mNormalWeekHeight);
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);  
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
		
		// variable params
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, weekPosition);				  
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, weekPattern); 
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, weekHeight);
		
		return weekDrawingParams;
	}
	
	
	//boolean mSecondaryMonthDayIndicatorAnimCompletion = false;
	int mCurrentXHeightByAnim;
	ValueAnimator mPsudoSecondaryMonthDayIndicatorTranslateAnimator;
	public void setPsudoSecondaryMonthDayIndicatorAnim(long duration) {	
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setVisibility(View.VISIBLE);
		
		mCurrentXHeightByAnim = mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight;
		float distance = mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight;
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator = ValueAnimator.ofInt(mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight, 0);
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.setDuration(duration);
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mPsudoSecondaryMonthDayIndicatorTranslateAnimator, distance));
				
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				mCurrentXHeightByAnim = (Integer) valueAnimator.getAnimatedValue();
				
				// ���⼭ 
				//int height = mPsudoSecondaryMonthDayAndDateIndicatorRegion.getHeight();
				//Log.i("tag", "mPsudoSecondaryMonthDayAndDateIndicatorRegion:height=" + String.valueOf(height));
				
				RelativeLayout.LayoutParams PsudoSecondaryMonthDayAndDateIndicatorRegionParams = (LayoutParams) mPsudoSecondaryMonthDayAndDateIndicatorRegion.getLayoutParams();
				PsudoSecondaryMonthDayAndDateIndicatorRegionParams.height = mCurrentXHeightByAnim;
				mPsudoSecondaryMonthDayAndDateIndicatorRegion.setLayoutParams(PsudoSecondaryMonthDayAndDateIndicatorRegionParams);		
			}
		});
		
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {			
				mPsudoSecondaryMonthDayAndDateIndicatorRegion.setVisibility(View.GONE);
				notifyAnimCompletion(SECONDARY_MONTHDAY_INDICATOR_ANIM_COMPLETION);
			}

			@Override
			public void onAnimationStart(Animator animator) {
				mSelectedWeekViewTranslateAnimator.start();
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});		
	}
	
	//boolean mUpperListViewAnimCompletion = false;
	ValueAnimator mPsudoUpperLisViewTranslateAnimator;
	public void setPsudoUpperLisViewValueAnimator(long duration) {	
		
		float distance = mPsudoUpperRegionVisibleRegionHeight;
		int start = -mPsudoUpperRegionVisibleRegionHeight;
		int end = (int) mPsudoMonthListViewUpperRegionContainer.getY();
		mPsudoUpperLisViewTranslateAnimator = ValueAnimator.ofInt(start, end);
		mPsudoUpperLisViewTranslateAnimator.setDuration(duration);
		mPsudoUpperLisViewTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mPsudoUpperLisViewTranslateAnimator, distance));
		
		/*
		Log.i("tag", "setPusdoUpperLisViewValueAnimator distance=" + String.valueOf(distance));
		Log.i("tag", "setPusdoUpperLisViewValueAnimator start=" + String.valueOf(start));
		Log.i("tag", "setPusdoUpperLisViewValueAnimator end=" + String.valueOf(end));
		*/
		mPsudoUpperLisViewTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update translation y coordinate
				int value = (Integer) valueAnimator.getAnimatedValue();
				//Log.i("tag", "mPusdoUpperLisViewTranslateAnimator:value=" + String.valueOf(value));
				mPsudoMonthListViewUpperRegionContainer.setTranslationY(value);
			}
		});
		
		mPsudoUpperLisViewTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {		
				//mPsudoMonthListViewUpperRegionContainer.setVisibility(View.GONE);
				notifyAnimCompletion(UPPER_LIST_VIEW_REGIONT_ANIM_COMPLETION);
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
	
	int mSelectedWeekXAlphaValue;
	ValueAnimator mSelectedWeekViewTranslateAnimator;
	TranslateInterpolator mSelectedWeekTranslateInterpolator;
	public void setSelectedWeekViewValueAnimator(long duration) {	
		mSelectedWeekView.setY(0);
		
		float distance = Math.abs(mSelectedWeekViewTop);
		
		if (distance < mDateIndicatorHeightOfSecondaryActionBar)
			mSelectedWeekXAlphaValue = (int) distance;
		else 
			mSelectedWeekXAlphaValue = mDateIndicatorHeightOfSecondaryActionBar;
		
		mSelectedWeekViewTranslateAnimator = ValueAnimator.ofInt(0, mSelectedWeekViewTop);
		mSelectedWeekViewTranslateAnimator.setDuration(duration);		
		mSelectedWeekTranslateInterpolator = new TranslateInterpolator(mSelectedWeekViewTranslateAnimator, distance);
		mSelectedWeekViewTranslateAnimator.setInterpolator(mSelectedWeekTranslateInterpolator);		
		
		mSelectedWeekViewTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float fraction = valueAnimator.getAnimatedFraction();
				int value = (Integer) valueAnimator.getAnimatedValue();
				
				// ���� mSelectedWeekViewTop�� mDateIndicatorHeightOfSecondaryActionBar ���� ���� ���� ����?
				// ���� ��� 2015/07/01 �ְ� selected week�� �Ǹ� �̷� ��Ȳ�� �߻��ȴ�
				// :mSelectedWeekViewTop==60, mDateIndicatorHeightOfSecondaryActionBar==64
				// value�� mDateIndicatorHeightOfSecondaryActionBar ���� �۴ٴ� ����
				// selected week view�� date indicator top�� secondary action bar�� date indicator bottom ��ġ�� �ʰ��ߴٴ� ���� �ǹ�
				float delta = value;
				float height = mSelectedWeekXAlphaValue;
				float ratio = delta / height;
				if (ratio > 1.0f)
					ratio = 1;
				
				float alpha = 255 - (255 * ratio);
									
				mSelectedWeekView.setSelectedWeekAlphaValue((int)alpha);
				
				if (value >= mSelectedWeekXAlphaValue) {
					if (!mSelectedWeekView.getNoDrawDateIndicatorTextFlagOfSelectedWeekView()) {
						mSelectedWeekView.setNoDrawDateIndicatorTextOfSelectedWeekView();						
					}	
				}
				
				/*
				if (value < mSelectedWeekXAlphaValue) {
					float delta = value;
					float height = mSelectedWeekXAlphaValue;
					float ratio = delta / height;
					float alpha = 255 - (255 * ratio);
										
					mSelectedWeekView.setSelectedWeekAlphaValue((int)alpha);
					mSelectedWeekView.invalidate();
				}
				else {						
					if (!mSelectedWeekView.getNoDrawDateIndicatorTextFlagOfSelectedWeekView()) {
						mSelectedWeekView.setNoDrawDateIndicatorTextOfSelectedWeekView();
						mSelectedWeekView.invalidate();
					}					
				}
				*/
				// �������� �߻���...
				// �ִϸ��̼� �ʹݿ� mPsudoSecondaryMonthDayAndDateIndicatorRegion�� height ���� 0�� ����
				// :���� mPsudoSecondaryMonthDayAndDateIndicatorRegion�� �ִϸ��̼��� ���۵��� ������ �ǹ�
				//  �׷��Ƿ� mPsudoSecondaryMonthDayAndDateIndicatorRegion�� start �����ʿ��� mSelectedWeekViewTranslateAnimator�� �����ؾ� ��
				//  �׸��� mPsudoSecondaryMonthDayAndDateIndicatorRegion.getHeight �Լ��� �̿��� ���� �ƴ϶�
				//  mPsudoSecondaryMonthDayAndDateIndicatorRegion �ִϸ����Ͱ� �����ϴ� ������ ���� ����
				int xHeight = mCurrentXHeightByAnim;
				//Log.i("tag", "*monthdayAnddateindicator bottom=" + String.valueOf(xHeight));
				//Log.i("tag", "*selectedweekview top=" + String.valueOf(value));
				//Log.i("tag", "*dateindicator top Of selectedweekview=" + String.valueOf(value + mPsudoSecondaryMonthDayAndDateIndicatorRegionHeight));
				
				// selected week view�� top�� mPsudoSecondaryMonthDayAndDateIndicatorRegion�� bottom ���� ũ�ٶ�� ����
				// selected week view�� top�� mPsudoSecondaryMonthDayAndDateIndicatorRegion���� ���� ���ٴ� ���� �ǹ���
				if (value> xHeight) {
					if (!mSelectedWeekView.getDrawUpperLineFlagStatusOfSelectedWeekView()) {						
						mSelectedWeekView.setDrawUpperLineFlagOfSelectedWeekView();
						//mSelectedWeekView.invalidate();
					}
				}
				
				mSelectedWeekView.invalidate();
				mSelectedWeekView.setTranslationY(value); // top position�� ������� ���̴�
				                                          // ��, top position�� ���� 0���� �ƴ϶�,
				                                          // top margin�� mCalendarViewsSecondaryActionBar�� height�� �����Ǿ� �����Ƿ�,
				                                          // ���� translate y ���� 0���� �����ؾ�  mCalendarViewsSecondaryActionBar �ٷ� �Ʒ����� �̵��Ѵ�
			}
		});
		
		mSelectedWeekViewTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {
				//mSelectedWeekViewAnimCompletion = true;
				notifyAnimCompletion(SELECTED_WEEK_VIEW_ANIM_COMPLETION);
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
	
	//boolean mLowerListViewAnimCompletion = false;
	ValueAnimator mPsudoLowerLisViewTranslateAnimator;
	public void setPsudoLowerLisViewValueAnimator(long duration) {	
		//Log.i("tag", "mPsudoUpperLisViewContainer.getY()=" + String.valueOf(mPsudoUpperLisViewContainer.getY()));
		//Log.i("tag", "mPusdoUpperListViewTranslateToYValue=" + String.valueOf(mPusdoUpperListViewTranslateToYValue));
		int start = (int) (mPsudoMonthListViewLowerRegionContainer.getY() + mPsudoLowerLisViewContainerHeight); 
		int end = (int) mPsudoMonthListViewLowerRegionContainer.getY();
		float distance = mPsudoLowerLisViewContainerHeight;
		mPsudoLowerLisViewTranslateAnimator = ValueAnimator.ofInt(start, end);
		mPsudoLowerLisViewTranslateAnimator.setDuration(duration);
		mPsudoLowerLisViewTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mPsudoLowerLisViewTranslateAnimator, distance));
		
		mPsudoLowerLisViewTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update translation y coordinate
				int value = (Integer) valueAnimator.getAnimatedValue();
				mPsudoMonthListViewLowerRegionContainer.setTranslationY(value);
			}
		});
		
		mPsudoLowerLisViewTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {		
				notifyAnimCompletion(LOWER_LIST_VIEW_REGIONT_ANIM_COMPLETION);
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

	//ValueAnimator mExtendedDayViewSwitcherTranslateAnimator;
	//ExtendedDayView mCurExtendedDayView;
	AnimationSet mExtendedDayViewSwitcherTranslateAnimSet;
	AnimationListener mExtendedDayViewSwitcherTranslateAnimSetListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {	
			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mFragment.mExtendedDayViewSwitcher.setVisibility(View.GONE);
			notifyAnimCompletion(EXTENDED_DAYVIEW_SWITCHER_ANIM_COMPLETION);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			
		}
		
	};
	
	public void setExtendedDayViewSwitcherTranslateAnim(long duration) {
		///mCurExtendedDayView = mFragment.mExtendedDayViewsMap.get(DayFragment.CURRENT_DAY_EVENTS);
		
		int start = (int) mFragment.mDayViewMainPanel.getY();//(int) mFragment.mExtendedDayViewSwitcher.getY(); 		
		int end = start + mFragment.mExtendedDayViewSwitcher.getHeight();
		float distance = mFragment.mExtendedDayViewSwitcher.getHeight();
		
		mExtendedDayViewSwitcherTranslateAnimSet = new AnimationSet(true);				
		AlphaAnimation alphaAnim = new AlphaAnimation(1, 0);		
		alphaAnim.setDuration(duration/2);
		//alphaAnim.setInterpolator(new ITEAnimInterpolator(distance, alphaAnim));
		
		TranslateAnimation downAnimation = new TranslateAnimation(
                Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, start,  // from
                Animation.ABSOLUTE, end); // to
		downAnimation.setDuration(duration);
		//downAnimation.setInterpolator(new ITEAnimInterpolator(distance, downAnimation));
		
		mExtendedDayViewSwitcherTranslateAnimSet.addAnimation(alphaAnim);
		mExtendedDayViewSwitcherTranslateAnimSet.addAnimation(downAnimation);
		//mExtendedDayViewSwitcherTranslateAnimSet.setDuration(duration);
		mExtendedDayViewSwitcherTranslateAnimSet.setInterpolator(new ITEAnimInterpolator(distance, mExtendedDayViewSwitcherTranslateAnimSet));	
		mExtendedDayViewSwitcherTranslateAnimSet.setAnimationListener(mExtendedDayViewSwitcherTranslateAnimSetListener);	
		/*
		mExtendedDayViewSwitcherTranslateAnimator = ValueAnimator.ofInt(start, end);
		mExtendedDayViewSwitcherTranslateAnimator.setDuration(duration);
		mExtendedDayViewSwitcherTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mExtendedDayViewSwitcherTranslateAnimator, distance));
		
		mExtendedDayViewSwitcherTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update translation y coordinate
				float fraction = valueAnimator.getAnimatedFraction();
				int value = (Integer) valueAnimator.getAnimatedValue();								
				mFragment.mExtendedDayViewSwitcher.setTranslationY(value);
				
				int alpha = 255 - (int)(255 * fraction);
				
				if (mCurExtendedDayView.mAllDayEventCounts != 0)
					mCurExtendedDayView.mAllDayEventLists.setAlpha(alpha);
				
				mCurExtendedDayView.mDayView.setAlpha(alpha);
				mFragment.mExtendedDayViewSwitcher.setAlpha(alpha);
				//mFragment.mExtendedDayViewSwitcher.setY(value);
			}
		});
		
		mExtendedDayViewSwitcherTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {	
				mFragment.mExtendedDayViewSwitcher.setVisibility(View.GONE);
				notifyAnimCompletion(EXTENDED_DAYVIEW_SWITCHER_ANIM_COMPLETION);
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
		*/	
	}
	
	long mSelectedWeekTranslateDuration;
	public void calcTranslateValueAnimDuration() {
		float width = mPsudoMonthByWeekAdapter.mAnticipationListViewHeight;
		float delta = mSelectedWeekViewTop;
		//mSelectedWeekTranslateDuration = 5000;
		mSelectedWeekTranslateDuration = calculateDuration(delta, width, DEFAULT_VELOCITY);
		//Log.i("tag", "mSelectedWeekTranslateDuration=" + String.valueOf(mSelectedWeekTranslateDuration));
	}
	
	
	private static final int MINIMUM_VELOCITY = 3300;    
	//private static final int MINIMUM_VELOCITY = 100;    
	//private static final int DEFAULT_VELOCITY = MINIMUM_VELOCITY; // ������ ��Ʈ
	//private static final int DEFAULT_VELOCITY = 4400; // ������ ��Ʈ
    private static final int DEFAULT_VELOCITY = 5500; // ������ ��Ʈ
    //private static final int DEFAULT_VELOCITY = 100; // test
    private static long calculateDuration(float delta, float width, float velocity) {
           	
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        velocity = Math.max(MINIMUM_VELOCITY, velocity);
                
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }
    
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
    
	private static class TranslateInterpolator implements TimeInterpolator {
		ValueAnimator mAnimator;
		float mAnimationDistance;
	
		public TranslateInterpolator(ValueAnimator animator, float animationDistance) {
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

	
    ArrayList<Event> mWiilBeSupportEvents;
    int mFirstLoadedJulianDayOfSupportedEvents;
    int mLastLoadedJulianDayOfSupportedEvents;
    public void transferEventsToApp() {    	
    	int targetMonthJulianDay = ETime.getJulianDay(mTargetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    	
    	//ArrayList<ArrayList<Event>> entrieEventDayList = new ArrayList<ArrayList<Event>>(); 
    	mWiilBeSupportEvents = new ArrayList<Event>();
    	
    	//ManageEventsPerWeek curWeekObj = mFragment.mManageEventsPerWeekHashMap.get(DayFragment.CURRENT_WEEK_EVENTS);
    	//ManageEventsPerWeek lastPrvWeekObj = mFragment.mManageEventsPerWeekHashMap.get(DayFragment.PREVIOUS_4_WEEK_EVENTS);
    	//ManageEventsPerWeek lastNextWeekObj = mFragment.mManageEventsPerWeekHashMap.get(DayFragment.NEXT_4_WEEK_EVENTS);   	
    	
    	/*		 
		PREVIOUS_4_WEEK_EVENTS = -4;  0 - 4 = -4
		PREVIOUS_3_WEEK_EVENTS = -3;  1 - 4 = -3
		PREVIOUS_2_WEEK_EVENTS = -2;  2 - 4 = -2
		PREVIOUS_1_WEEK_EVENTS = -1;  3 - 4 = -1  
	    CURRENT_WEEK_EVENTS = 0;      4 - 4 = 0
	    NEXT_1_WEEK_EVENTS = 1;       5 - 4 = 1
	    NEXT_2_WEEK_EVENTS = 2;       6 - 4 = 2
	    NEXT_3_WEEK_EVENTS = 3;       7 - 4 = 3
	    NEXT_4_WEEK_EVENTS = 4;       8 - 4 = 4
	    */
    	mFirstLoadedJulianDayOfSupportedEvents = 0;
    	mLastLoadedJulianDayOfSupportedEvents = 0;
    	
    	for (int i=0; i<9; i++) {
			
			int whichSideWeek = i - 4;
			ManageEventsPerWeek Obj = mFragment.mManageEventsPerWeekHashMap.get(whichSideWeek);
			
			if (whichSideWeek == DayFragment.PREVIOUS_4_WEEK_EVENTS) {
				mFirstLoadedJulianDayOfSupportedEvents = Obj.mWeekStartJulianDay;
			}
			else if (whichSideWeek == DayFragment.NEXT_4_WEEK_EVENTS) {
				mLastLoadedJulianDayOfSupportedEvents = Obj.mWeekEndJulianDay;
			}
			
			//entrieEventDayList.addAll(Obj.mEventDayList);
			
			for (int j=0; j<Obj.mEventDayList.size(); j++) {
				ArrayList<Event> events = Obj.mEventDayList.get(j);
				mWiilBeSupportEvents.addAll(events);				
	    	}			
		}
    	
    	//mApp.loadRemainderEvents(mTargetMonthTime.timezone, targetMonthJulianDay, firstJulianDayOfSupportedEvents, lastJulianDayOfSupportedEvents, entrieEventDayList);
    	mApp.loadRemainderEvents(mTargetMonthTime.getTimeZone().getID(), targetMonthJulianDay, mFirstDayOfWeek,
    			mFirstLoadedJulianDayOfSupportedEvents, mLastLoadedJulianDayOfSupportedEvents, mWiilBeSupportEvents);
    	
    }
    
	
	public void configPsudoMonthAdapter() {
		
		mPsudoMonthByWeekAdapter.setEvents(mFirstJulianDay,
				mLastLoadedJulianDayOfSupportedEvents - mFirstLoadedJulianDayOfSupportedEvents + 1,                
                mWiilBeSupportEvents); 
			
        if (mPsudoUpperWeekViewCounts != 0) {
        	int count = mPsudoMonthListViewUpperRegionContainer.getChildCount();
        	for (int i=0; i<mPsudoUpperWeekViewCounts; i++) {
        		PsudoMonthWeekEventsView weekViewObj = (PsudoMonthWeekEventsView) mPsudoMonthListViewUpperRegionContainer.getChildAt(i);
        		int weekPattern = weekViewObj.getWeekPattern();
        		if (weekPattern != PsudoMonthWeekEventsView.MONTH_MONTHIDICATOR_WEEK_PATTERN) {
        			mPsudoMonthByWeekAdapter.sendEventsToView(weekViewObj);
        			weekViewObj.invalidate();
        		}
        	}
        }
        
        mPsudoMonthByWeekAdapter.sendEventsToView(mSelectedWeekView);
        mSelectedWeekView.invalidate();
        
        if (mPsudoLowerWeekViewCounts != 0) {
        	int count = mPsudoMonthListViewLowerRegionContainer.getChildCount();
        	for (int i=0; i<mPsudoLowerWeekViewCounts; i++) {
        		PsudoMonthWeekEventsView weekViewObj = (PsudoMonthWeekEventsView) mPsudoMonthListViewLowerRegionContainer.getChildAt(i);            		
        		mPsudoMonthByWeekAdapter.sendEventsToView(weekViewObj);              		
        		weekViewObj.invalidate();
        	}            	
        }         
	}
	
	String mNoTitleString;
	public void buildEventsFromCursor(
            ArrayList<Event> events, Cursor cEvents, Context context, int startDay, int endDay) {
    	
        if (cEvents == null || events == null) {
            //Log.e(TAG, "buildEventsFromCursor: null cursor or null events list!");
            return;
        }

        int count = cEvents.getCount();

        if (count == 0) {
            return;
        }

        Resources res = context.getResources();
        mNoTitleString = res.getString(R.string.no_title_label);
        
        // Sort events in two passes so we ensure the allday and standard events
        // get sorted in the correct order
        cEvents.moveToPosition(-1);
        while (cEvents.moveToNext()) {
            Event e = generateEventFromCursor(cEvents);
            if (e.startDay > endDay || e.endDay < startDay) {
                continue;
            }
            events.add(e);
        }
    }
	
	private Event generateEventFromCursor(Cursor cEvents) {
        Event e = new Event();

        e.id = cEvents.getLong(PROJECTION_EVENT_ID_INDEX);
        e.title = cEvents.getString(PROJECTION_TITLE_INDEX);
        
        if (e.title == null || e.title.length() == 0) {
            e.title = mNoTitleString;
        }

        
        long eStart = cEvents.getLong(PROJECTION_BEGIN_INDEX);
        long eEnd = cEvents.getLong(PROJECTION_END_INDEX);

        e.startMillis = eStart;        
        e.startDay = cEvents.getInt(PROJECTION_START_DAY_INDEX);

        e.endMillis = eEnd;        
        e.endDay = cEvents.getInt(PROJECTION_END_DAY_INDEX);

        return e;
    }
	
	private final int SECONDARY_MONTHDAY_INDICATOR_ANIM_COMPLETION = 1; //1
    private final int UPPER_LIST_VIEW_REGIONT_ANIM_COMPLETION = 2; //2
    private final int SELECTED_WEEK_VIEW_ANIM_COMPLETION = 4;      //4
    private final int LOWER_LIST_VIEW_REGIONT_ANIM_COMPLETION = 8; //8
    private final int CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION = 16; //16
    private final int EXTENDED_DAYVIEW_SWITCHER_ANIM_COMPLETION = 32; //32
    private final int ENTIRE_ANIMATIONS_COMPLETION = SECONDARY_MONTHDAY_INDICATOR_ANIM_COMPLETION |
    		UPPER_LIST_VIEW_REGIONT_ANIM_COMPLETION |
    		SELECTED_WEEK_VIEW_ANIM_COMPLETION |
    		LOWER_LIST_VIEW_REGIONT_ANIM_COMPLETION |
    		CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION |
    		EXTENDED_DAYVIEW_SWITCHER_ANIM_COMPLETION;
    
    private int mExitAnimationCompletionStatus = 0;
    
    public synchronized void notifyAnimCompletion(int flag) {
    	mExitAnimationCompletionStatus |= flag;
		if (mExitAnimationCompletionStatus == ENTIRE_ANIMATIONS_COMPLETION) {
			goodByeFragment();
		}
	}
    
	Runnable mUpperActionBarSwitchFragmentAnimCompletionCallBack = new Runnable() {

		@Override
		public void run() {	
			notifyAnimCompletion(CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION);
		}
	};
	    
	public void goodByeFragment() {
		
		mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
		
	}
}
