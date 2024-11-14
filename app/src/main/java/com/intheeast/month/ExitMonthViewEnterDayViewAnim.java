package com.intheeast.month;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.day.DayFragment;
import com.intheeast.day.DayView;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.month.PsudoExtendedDayView.ExtendedDayViewDimension;
import com.intheeast.settings.SettingsFragment;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class ExitMonthViewEnterDayViewAnim {
	private static String TAG = "ExitMonthViewEnterDayViewAnim";
    private static boolean INFO = false;
    
	Context mContext;
	MonthFragment mFragment;
	CalendarController mController;
	RelativeLayout mMonthViewLayout;
	Calendar mSelectedDayTime;
	int mSelectedJulainDay;
	MonthListView mListView;
	MonthWeekView mSingleTapUpView;
	DrawableBolderLinearLayoutWithoutPadding mPsudoSecondaryMonthDayAndDateIndicatorRegion;
	// The "implicit" max height of the layout is the height of its parent. 
	// Since you're using wrap_content on both layouts, 
	// that means the parent is effectively the screen area, minus whatever other views you're using (such as the TextView). 
	// Unless you place it in a scrolling container, such as a ScrollView, it won't ever exceed the size of the screen.
	LinearLayout mPsudoUpperLisViewContainer;
	LinearLayout mPsudoUpperLisViewTailContainer;
	LinearLayout mPsudoLowerLisViewContainer;
	MonthWeekView mSelectedWeekView;	
	PsudoExtendedDayView mPsudoExtendedDayView;
	
	String mTzId;
	TimeZone mTimeZone;
	int mFirstDayOfWeek;
	
	int mFirstVisiblePosition;
	int mLastVisiblePosition;
	int mSelectedPosition;
	MonthWeekView mFirstVisibleWeekChild;
	MonthWeekView mLastVisibleWeekChild;
	
	
	ECalendarApplication mApp;
	final int mNumDays = 1;
	public ExitMonthViewEnterDayViewAnim(ECalendarApplication app, Context context, MonthFragment fragment, CalendarController controller, MonthWeekView singleTapUpView, 
			Calendar selectedDay, String tzId) {
		mContext = context;
		mFragment = fragment;
		mController = controller;
		mSingleTapUpView = singleTapUpView;
		
		mSelectedDayTime = GregorianCalendar.getInstance(selectedDay.getTimeZone());//selectedDay;
		mSelectedDayTime.setTimeInMillis(selectedDay.getTimeInMillis());
		mTzId = tzId;
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
		
		//long gmtoff = selectedDay.getTimeZone().getRawOffset() / 1000;
		mSelectedJulainDay = ETime.getJulianDay(mSelectedDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        
		init();
	}
	
	// 1.secondary monthday indicator�� dayview�� ���� �������� ���ÿ� ������
	//   ���� ������ monthday indicator bottom�̴�
	//   :�ٵ� �������� dayview�� allday ������ �ִٸ�?
	//    ->������ �� ����
	// 2.dayview�� ���÷����ϴ� �ð���� ���� �ð��� ���õǾ� �ִ�
	// 3.dayview�� ���÷����ϴ� �ð��뿡 �̺�Ʈ�� �����Ѵٸ�,
	//   �̺�Ʈ�� ǥ���Ͽ��� �Ѵ�
	public void init() {
		mMonthViewLayout = mFragment.mMonthViewLayout;
		mListView = (MonthListView) mFragment.getListView();
		
		mPsudoUpperLisViewContainer = mFragment.mPsudoUpperLisViewContainer;
		mPsudoUpperLisViewTailContainer = mFragment.mPsudoUpperLisViewTailContainer;
		mPsudoLowerLisViewContainer = mFragment.mPsudoNextMonthRegionContainer;		
		
		mFirstVisiblePosition  = mListView.getFirstVisiblePosition();
		mLastVisiblePosition = mListView.getLastVisiblePosition();
		
		mSelectedPosition = mSingleTapUpView.getWeekNumber(); // �� ������ ��ܰ� �ϴ� psudo view�� �����ϴ� ������ �Ǵ°�?
		
		mFirstVisibleWeekChild = (MonthWeekView) mListView.getChildAt(0);		
		mLastVisibleWeekChild = (MonthWeekView) mListView.getChildAt(mLastVisiblePosition - mFirstVisiblePosition);
		
		mListView.setVisibility(View.GONE);	
		
		makePsudoExtendedDayView();		
		
		makeSelectedWeekView();
		
		makePsudoSecondaryMonthDayIndicator();
		
		makeAnimCase();				
				
		mFragment.setEventDayListsToApp();
		
		calcTranslateValueAnimDuration();
		
		mFragment.mUpperActionBarFrag.setSwitchFragmentAnim(ViewType.DAY, mSelectedDayTime.getTimeInMillis(), mSelectedWeekTranslateDuration, -1, mUpperActionBarSwitchFragmentAnimCompletionCallBack);
		
		startMonthExitAnim();
	}
	
	public ArrayList<Event> makeSelectedDayEventList() {
		ArrayList<Event> events = mFragment.mAdapter.getEvents();
		
		if (events == null || events.size() == 0) {                                
            return null;
        }
		
		ArrayList<Event> selectedDayEventList = new ArrayList<Event>();
		
		// Compute the new set of days with events
        for (Event event : events) {        
            int startDay = event.startDay;
            int endDay = event.endDay;                                                                       
           
            if ((startDay == mSelectedJulainDay) || (endDay == mSelectedJulainDay) )
            	selectedDayEventList.add(event);
        }
        
        return selectedDayEventList;		
	}
	
	public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = mFragment.getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = mFragment.getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
	
	
	int DAY_HEADER_HEIGHT; 
	int MONTHDAY_HEADER_HEIGHT;     	
	int DATEINDICATOR_HEADER_HEIGHT; // normal week height�� 0.4%�̴�
	public void makePsudoExtendedDayView() {	
		//int normalWeekHeight = mListView.getNMNormalWeekHeight(); // 241
		//int secondaryActionBarHeight = mFragment.mCalendarViewsSecondaryActionBar.getHeight(); // 48
		
		DAY_HEADER_HEIGHT = (int) mFragment.getResources().getDimension(R.dimen.day_header_height); // 48
    	MONTHDAY_HEADER_HEIGHT = (int) (mListView.getNMNormalWeekHeight() * CalendarViewsSecondaryActionBar.MONTHDAY_HEADER_HEIGHT_RATIO); // 144     	
    	DATEINDICATOR_HEADER_HEIGHT = (int) (mListView.getNMNormalWeekHeight() * CalendarViewsSecondaryActionBar.DATEINDICATOR_HEADER_HEIGHT_RATIO); // 96
			
		mPsudoExtendedDayView = mFragment.mPsudoExtendedDayView;
		RelativeLayout.LayoutParams PsudoDayViewParams = (LayoutParams) mPsudoExtendedDayView.getLayoutParams();		
		PsudoDayViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		// 
		// mPsudoExtendedDayView�� marginTop�� DayFragment�� ����ϰ� �� SecondaryActionBar�� �ٷ� �Ʒ��� �����Ѵ�
		// DayFragment�� SecondaryActionBar��
		// mDayHeaderLayout / mMonthDayHeaderLayout / mDateIndicatorTextView ��� ����Ѵ�		
		// mSelectedWeekViewAnimToYValue ���� ������ �׻� 0�̹Ƿ� 
		// setTranslationY�� �������� 0���� �����Ǿ
		// SecondaryActionBar�� mDateIndicatorTextView �ٷ� �Ʒ����� �̵��ϰ� �ȴ�
		int marginTop = DAY_HEADER_HEIGHT + MONTHDAY_HEADER_HEIGHT + DATEINDICATOR_HEADER_HEIGHT;
		PsudoDayViewParams.setMargins(0, marginTop, 0, 0);
		mPsudoExtendedDayView.setLayoutParams(PsudoDayViewParams);
		
		mPsudoExtendedDayView.init(mFragment.getActivity(), mController, 
				mSelectedDayTime, DayFragment.CURRENT_DAY_EVENTS, setAlldayEventListViewDimension());
		
		ArrayList<Event> events = makeSelectedDayEventList();
		ArrayList<Event> allDayEvents = DayFragment.getAllDayEvents(events);
		
		mPsudoExtendedDayView.mDayView.setEventsX(events);
		mPsudoExtendedDayView.setAllDayEventListView(allDayEvents.size(), allDayEvents);		
	}
	
	public ExtendedDayViewDimension setAlldayEventListViewDimension() {    	
		calcFrameLayoutDimension();
		
    	Paint p = new Paint();
    	p.setAntiAlias(true);
    	p.setTextSize(mFragment.getResources().getDimension(R.dimen.hours_text_size));
        p.setTypeface(null);
        int hoursTextHeight = (int)Math.abs(p.ascent());    
        int hourDescent = (int) (p.descent() + 0.5f);
        int mRealHoursTextHeight = hoursTextHeight + hourDescent;
    	
    	float mScale = mFragment.getResources().getDisplayMetrics().density;
    	
    	int HOURS_TOP_MARGIN = 2;
    	HOURS_TOP_MARGIN *= mScale;
    	int GRID_LINE_TOP_MARGIN = (DayView.HOUR_GAP + HOURS_TOP_MARGIN) + ( mRealHoursTextHeight / 2);
    	
    	
    	float AMPM_TEXT_SIZE = (int) mFragment.getResources().getDimension(R.dimen.ampm_text_size);
    	p.setTextSize(AMPM_TEXT_SIZE);
    	
    	String amString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
        String pmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
        String space = " ";        
    	
        String[] fullAmPm = {
        		amString + space + "12:59",
        		pmString + space + "11:59"
        };
    	int maxHourStringWidth = DayView.computeMaxStringWidth(0, fullAmPm, p);     
    	int mHoursWidth = DayView.NEW_HOURS_LEFT_MARGIN + maxHourStringWidth + DayView.NEW_HOURS_RIGHT_MARGIN;
    	
    	int gridAreaWidth = mDayViewWidth - mHoursWidth;
        int mCellWidth = (gridAreaWidth - (mNumDays * DayView.DAY_GAP)) / mNumDays; 
        
    	// all day event listview�� max height : dayview height�� 0.15�� �����Ѵ�
    	// all day event listview�� item height�� : all day event listview�� max height�� 0.35�� �����Ѵ�
    	// all day event listview�� first item top margin : GRID_LINE_TOP_MARGIN�� 0.5
    	// all day event listview�� last item bottom margin : GRID_LINE_TOP_MARGIN�� 0.5
    	// all day event listview�� item gap : first item top margin�� 0.5
    	
    	int mAlldayEventListViewMaxHeight = (int) (mDayViewHeight * 0.15f);
    	int mAlldayEventListViewItemHeight = (int) (mAlldayEventListViewMaxHeight * 0.35f);
    	int mAlldayEventListViewFirstItemTopMargin = (int) (GRID_LINE_TOP_MARGIN * 0.5f);
    	int mAlldayEventListViewLastItemBottomMargin = mAlldayEventListViewFirstItemTopMargin;
    	int mAlldayEventListViewItemGap = (int) (mAlldayEventListViewFirstItemTopMargin * 0.5f);   
    	
    	return new ExtendedDayViewDimension(mDayViewWidth, mHoursWidth, mCellWidth, mAlldayEventListViewMaxHeight,
    			mAlldayEventListViewItemHeight, mAlldayEventListViewFirstItemTopMargin, mAlldayEventListViewLastItemBottomMargin, mAlldayEventListViewItemGap);
    }
	
	MonthExitDayEnterAnimContext mMonthExitDayEnterAnimContext;
	boolean mSelectedWeekIsTwoMonthsWeekPattern = false;
	boolean mSelectedPrvMonthWeek = false;
	boolean mNextMonthWeekIsLowerRegion = false;
	public void makeAnimCase() {	
		// �Ʒ�ó�� �з��� ������?
		// :
		// 1.no upper region
		//  : first visible child�� day�� ���õ� ��� : ���� no upper region�� layout �������� ������ �Ѵ�
		if (mFirstVisiblePosition == mSelectedPosition) {
			makeAnimSelectedPositionIsFirstVisiblePositionInCase();
		}
		// 2.no lower region
		//  : last visible child�� day�� ���õ� ���
		else if (mLastVisiblePosition == mSelectedPosition) {
			makeAnimSelectedPositionIsLastVisiblePositionInCase();
		}
		// 3.need upper/lower region
		//  : 1, 2 ��츦 ������ ��� ���
		else {
			makeAnimSelectedPositionIsNotBothEndVisiblePositionInCase();
		}
		
		
		
		
		
	}
	
	TextView mDateIndicatorTextView;
	String mDateIndicatorText;
	int mCalendarSecondaryActionBarDateIndicatorTextColor;
	int mCalendarSecondaryActionBarDateIndicatorTextColorRed;
	int mCalendarSecondaryActionBarDateIndicatorTextColorGreen;
	int mCalendarSecondaryActionBarDateIndicatorTextColorBlue;
	public void makePsudoSecondaryMonthDayIndicator() {
		
		mPsudoSecondaryMonthDayAndDateIndicatorRegion = new DrawableBolderLinearLayoutWithoutPadding(mContext);		
		// ���� �ʱ⿡ size�� LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ��������,		
		// �ִϸ��̼� �ܰ迡�� MONTHDAY_HEADER_HEIGHT + DATEINDICATOR_HEADER_HEIGHT ������� ���� Ŀ����
		RelativeLayout.LayoutParams PsudoSecondaryMonthDayAndDateIndicatorRegionParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		PsudoSecondaryMonthDayAndDateIndicatorRegionParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		// month fragment�� secondary actionbar�� mDayHeaderLayout(DAY_HEADER_HEIGHT)�� visible �����̰�
		// mMonthDayHeaderLayout�� mDateIndicatorTextView�� gone �����̹Ƿ�
		// mFragment.mCalendarViewsSecondaryActionBar.getHeight()�� ���� �� DAY_HEADER_HEIGHT�̴�
		PsudoSecondaryMonthDayAndDateIndicatorRegionParams.setMargins(0, DAY_HEADER_HEIGHT, 0, 0);
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setLayoutParams(PsudoSecondaryMonthDayAndDateIndicatorRegionParams);
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setBackgroundColor(mContext.getResources().getColor(R.color.manageevent_actionbar_background));
		//mPsudoSecondaryMonthDayAndDateIndicatorRegion.setBackgroundColor(Color.RED); // for test
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setVisibility(View.GONE);
		
		mMonthViewLayout.addView(mPsudoSecondaryMonthDayAndDateIndicatorRegion);
			
		// android:gravity sets the gravity of the content of the View its used on.
		// android:layout_gravity sets the gravity of the View or Layout in its parent.
		//mMonthViewLayout.addView(mSelectedWeekView);
		mDateIndicatorTextView = new TextView(mContext);	
		RelativeLayout.LayoutParams dateIndicatorTextViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT); 
		//dateIndicatorTextViewParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE); //dateIndicatorTextViewParams.weight = 1; dateIndicatorTextViewParams.gravity = Gravity.CENTER; // android:layout_gravity="center"
		int topMargin = DAY_HEADER_HEIGHT + MONTHDAY_HEADER_HEIGHT;
		dateIndicatorTextViewParams.setMargins(0, topMargin, 0, 0);
    	mDateIndicatorTextView.setLayoutParams(dateIndicatorTextViewParams);   
    	
    	mDateIndicatorTextView.setGravity(Gravity.CENTER); // android:gravity="center"
        
    	// setTextSize�� para�� The scaled pixel[sp] size ��.
     	// �׷��Ƿ� getResources().getDimension(R.dimen.dateindicator_text_height) ���� �ٷ� �����ϸ� �ȵȴ�		
        float textSPSize = mFragment.getResources().getDimension(R.dimen.dateindicator_text_height) / mFragment.getResources().getDisplayMetrics().scaledDensity;;
        mDateIndicatorTextView.setTextSize(textSPSize);
        
        mDateIndicatorText = buildFullDate(mSelectedDayTime.getTimeInMillis());
        
    	mCalendarSecondaryActionBarDateIndicatorTextColor = mFragment.getResources().getColor(R.color.secondaryActionBarDateIndicatorTextColor);        
    	mCalendarSecondaryActionBarDateIndicatorTextColorRed = Color.red(mCalendarSecondaryActionBarDateIndicatorTextColor);
    	mCalendarSecondaryActionBarDateIndicatorTextColorGreen = Color.green(mCalendarSecondaryActionBarDateIndicatorTextColor);
    	mCalendarSecondaryActionBarDateIndicatorTextColorBlue = Color.blue(mCalendarSecondaryActionBarDateIndicatorTextColor);
    	
        mDateIndicatorTextView.setVisibility(View.GONE);
        
        mMonthViewLayout.addView(mDateIndicatorTextView);
		
	}
	
	private Formatter mFormatter;
    private StringBuilder mStringBuilder;
	public String buildFullDate(long milliTime) {
        mStringBuilder.setLength(0);
        String date = ETime.formatDateTimeRange(mContext, mFormatter, milliTime, milliTime,
        		ETime.FORMAT_SHOW_WEEKDAY | ETime.FORMAT_SHOW_DATE | ETime.FORMAT_SHOW_YEAR, mTimeZone.getID()).toString();
        return date;
    }
	
	
	long mSelectedWeekTranslateDuration;
	public void calcTranslateValueAnimDuration() {
		float width = mListView.getOriginalListViewHeight();
		float delta = 0;
		
		if (mSelectedWeekViewTop < 0)
			delta = Math.abs(mSelectedWeekViewTop);
		else 
			delta = mSelectedWeekViewTop;
		
		mSelectedWeekTranslateDuration = calculateDuration(delta, width, DEFAULT_VELOCITY);
		//mSelectedWeekTranslateDuration = 5000;
	}	
	
	public void startMonthExitAnim() {
		/*
		public final int PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION = 1;  //mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); o
		public final int SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 2; 		         //mSelectedWeekViewTranslateAnimator.start();
		public final int PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 4;                //mPusdoUpperLisViewTranslateAnimator.start(); o
		public final int PSUDO_UPPER_LISTVIEW_TAIL_TRANSLATE_EXIT_ANIM_COMPLETION = 8;           //mPusdoUpperLisViewTailTranslateAnimator.start();
		public final int PSUDO_NEXT_MONTH_REGION_CONTAINER_TRANSLATE_EXIT_ANIM_COMPLETION = 16;   //mPsudoNextMonthRegionContainer.startAnimation();
		public final int PSUDO_ENTENDEDDAY_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 32;              //mPsudoExtendedDayView.startAnimation()	
		*/
		
		
		setPsudoSecondaryMonthDayIndicatorAnim(mSelectedWeekTranslateDuration);
		setPsudoExtendedDayViewAnim(mSelectedWeekTranslateDuration);
		
		mPsudoSecondaryMonthDayAndDateIndicatorRegion.setVisibility(View.VISIBLE);
		
		switch(mMonthExitDayEnterAnimContext.mAnimCase) {
		case SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE:			
			setSelectedWeekViewValueAnimator(mSelectedWeekTranslateDuration);
						
			if (mMonthExitDayEnterAnimContext.mSelectedWeekMovement == DONT_VERTICAL_MOVE) {
				setPusdoLowerLisViewAnim(mSelectedWeekTranslateDuration);
			}
			else if (mMonthExitDayEnterAnimContext.mSelectedWeekMovement == UPWARD_SELECTEDWEEK_VERTICAL_MOVE) {
				setPusdoLowerLisViewAnim(mSelectedWeekTranslateDuration);
			}
			else if (mMonthExitDayEnterAnimContext.mSelectedWeekMovement == DOWNWARD_SELECTEDWEEK_VERTICAL_MOVE) {
				setPusdoLowerLisViewAnim(mSelectedWeekTranslateDuration);
			}			
			
			mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION;
			mSelectedWeekViewTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPsudoLowerLisViewContainer.startAnimation(mPusdoLowerListViewTranslateAnimation); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_LOWER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			
			break;
		case UPPERREGION_AND_SELECTEDWEEK_ANIM_CASE:			
			setPusdoUpperLisViewValueAnimator(mSelectedWeekTranslateDuration);
			setSelectedWeekViewValueAnimator(mSelectedWeekTranslateDuration);
			
			mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPusdoUpperLisViewTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			mSelectedWeekViewTranslateAnimator.start();	ENTIRE_ANIMATIONS_COMPLETION |= SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION;		
			break;
			
		case UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_ANIM_CASE:
			setPusdoUpperLisViewValueAnimator(mSelectedWeekTranslateDuration);
			setPusdoUpperLisViewTailValueAnimator(mSelectedWeekTranslateDuration);
			setSelectedWeekViewValueAnimator(mSelectedWeekTranslateDuration);
			
			mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPusdoUpperLisViewTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPusdoUpperLisViewTailTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_UPPER_LISTVIEW_TAIL_TRANSLATE_EXIT_ANIM_COMPLETION;
			mSelectedWeekViewTranslateAnimator.start();	ENTIRE_ANIMATIONS_COMPLETION |= SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			break;
			
		case UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE:			
			setPusdoUpperLisViewValueAnimator(mSelectedWeekTranslateDuration);
			setSelectedWeekViewValueAnimator(mSelectedWeekTranslateDuration);			
			setPusdoLowerLisViewAnim(mSelectedWeekTranslateDuration);
			
			mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPusdoUpperLisViewTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			mSelectedWeekViewTranslateAnimator.start();	ENTIRE_ANIMATIONS_COMPLETION |= SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION;		
			mPsudoLowerLisViewContainer.startAnimation(mPusdoLowerListViewTranslateAnimation); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_LOWER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			break;
			
		case UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE:
			setPusdoUpperLisViewValueAnimator(mSelectedWeekTranslateDuration);
			setPusdoUpperLisViewTailValueAnimator(mSelectedWeekTranslateDuration);
			setSelectedWeekViewValueAnimator(mSelectedWeekTranslateDuration);			
			setPusdoLowerLisViewAnim(mSelectedWeekTranslateDuration);
			
			mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPusdoUpperLisViewTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			mPusdoUpperLisViewTailTranslateAnimator.start(); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_UPPER_LISTVIEW_TAIL_TRANSLATE_EXIT_ANIM_COMPLETION;
			mSelectedWeekViewTranslateAnimator.start();	ENTIRE_ANIMATIONS_COMPLETION |= SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION;		
			mPsudoLowerLisViewContainer.startAnimation(mPusdoLowerListViewTranslateAnimation); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_LOWER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION;
			break;
		}
		
		mPsudoExtendedDayView.setVisibility(View.VISIBLE);
		mPsudoExtendedDayView.startAnimation(mPsudoExtendedDayViewTranslateAnimationSet); ENTIRE_ANIMATIONS_COMPLETION |= PSUDO_ENTENDEDDAY_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION;	
		
		mFragment.mUpperActionBarFrag.startSwitchFragmentAnim(-1); ENTIRE_ANIMATIONS_COMPLETION |= CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION;
	}	
		
	ValueAnimator mPsudoSecondaryMonthDayIndicatorTranslateAnimator;
	public void setPsudoSecondaryMonthDayIndicatorAnim(long duration) {		
		//float distance = mListView.getNMNormalWeekHeight();
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator = ValueAnimator.ofInt(0, MONTHDAY_HEADER_HEIGHT + DATEINDICATOR_HEADER_HEIGHT);
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.setDuration(duration);
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mPsudoSecondaryMonthDayIndicatorTranslateAnimator, MONTHDAY_HEADER_HEIGHT + DATEINDICATOR_HEADER_HEIGHT));		
				
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				int value = (Integer) valueAnimator.getAnimatedValue();
				
				RelativeLayout.LayoutParams PsudoSecondaryMonthDayAndDateIndicatorRegionParams = (LayoutParams) mPsudoSecondaryMonthDayAndDateIndicatorRegion.getLayoutParams();
				PsudoSecondaryMonthDayAndDateIndicatorRegionParams.height = value;
				mPsudoSecondaryMonthDayAndDateIndicatorRegion.setLayoutParams(PsudoSecondaryMonthDayAndDateIndicatorRegionParams);		
			}
		});
		
		mPsudoSecondaryMonthDayIndicatorTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {	
				notifyAnimCompletion(PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION);
			}

			@Override
			public void onAnimationStart(Animator animator) {		
				ExitMonthViewEnterDayViewAnim.this.mFragment.mCalendarViewsSecondaryActionBar.setDontDrawUnderLine();
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});		
	}
	
	float mPusdoUpperListViewTranslateToYValue;
	
	ValueAnimator mPusdoUpperLisViewTranslateAnimator;
	public void setPusdoUpperLisViewValueAnimator(long duration) {	
		//Log.i("tag", "mPsudoUpperLisViewContainer.getY()=" + String.valueOf(mPsudoUpperLisViewContainer.getY()));
		//Log.i("tag", "mPusdoUpperListViewTranslateToYValue=" + String.valueOf(mPusdoUpperListViewTranslateToYValue));
		float distance = Math.abs(Math.abs(mPsudoUpperLisViewContainer.getY()) - Math.abs(mPusdoUpperListViewTranslateToYValue));
		mPusdoUpperLisViewTranslateAnimator = ValueAnimator.ofInt((int)mPsudoUpperLisViewContainer.getY(), (int)mPusdoUpperListViewTranslateToYValue);
		mPusdoUpperLisViewTranslateAnimator.setDuration(duration);
		mPusdoUpperLisViewTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mPusdoUpperLisViewTranslateAnimator, distance));
		
		mPusdoUpperLisViewTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update translation y coordinate
				int value = (Integer) valueAnimator.getAnimatedValue();
				//Log.i("tag", "value=" + String.valueOf(value));
				//mPsudoUpperLisViewContainer.setY(value);
				mPsudoUpperLisViewContainer.setTranslationY(value);
			}
		});
		
		mPusdoUpperLisViewTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {		
				mPsudoUpperLisViewContainer.setVisibility(View.GONE);
				notifyAnimCompletion(PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION);
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
	
	float mPsudoUpperLisViewTailTranslateToYValue;
	ValueAnimator mPusdoUpperLisViewTailTranslateAnimator;
	public void setPusdoUpperLisViewTailValueAnimator(long duration) {	
		float distance = Math.abs(Math.abs(mPsudoUpperLisViewTailContainer.getY()) - Math.abs(mPsudoUpperLisViewTailTranslateToYValue));
		mPusdoUpperLisViewTailTranslateAnimator = ValueAnimator.ofInt((int)mPsudoUpperLisViewTailContainer.getY(), (int)mPsudoUpperLisViewTailTranslateToYValue);
		mPusdoUpperLisViewTailTranslateAnimator.setDuration(duration);
		mPusdoUpperLisViewTailTranslateAnimator.setInterpolator(
				new TranslateInterpolator(mPusdoUpperLisViewTailTranslateAnimator, distance));
		
		mPusdoUpperLisViewTailTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update translation y coordinate
				int value = (Integer) valueAnimator.getAnimatedValue();
				//Log.i("tag", "value=" + String.valueOf(value));				
				mPsudoUpperLisViewTailContainer.setTranslationY(value);
			}
		});
		
		mPusdoUpperLisViewTailTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {		
				mPsudoUpperLisViewTailContainer.setVisibility(View.GONE);
				notifyAnimCompletion(PSUDO_UPPER_LISTVIEW_TAIL_TRANSLATE_EXIT_ANIM_COMPLETION);
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
	
	float mSelectedWeekViewTop;
	final int mSelectedWeekViewAnimToYValue = 0;
	ValueAnimator mSelectedWeekViewTranslateAnimator;
	TranslateInterpolator mSelectedWeekTranslateInterpolator;
	int SecondaryActionbarBottom = 0;
	boolean mSetSelectedWeekDontWeekUpperLine = false;
	boolean mSelectedWeekHaveEvent = false;
	int mSelectedWeekPattern;
	public void setSelectedWeekViewValueAnimator(long duration) {	
		if (INFO) Log.i(TAG, "setSelectedWeekViewValueAnimator:WP=" + String.valueOf(mSelectedWeekView.getWeekPattern()));
		
		mSelectedWeekPattern = mSelectedWeekView.getWeekPattern();
		
		mSelectedWeekHaveEvent = mSelectedWeekView.getEventExistenceStatus();
			
		SecondaryActionbarBottom = mSelectedWeekViewAnimToYValue + mListView.getNMNormalWeekHeight(); //mSelectedWeekVisibleHeight;
		//mSelectedWeekViewAnimToYValue = mFragment.mCalendarViewsSecondaryActionBar.getBottom(); // �߸��� �����̴�
		
		float distance = Math.abs(mSelectedWeekViewTop);
		// mSelectedWeekViewTop�� ���� 0�̴�
		// :int marginTop = mFragment.mCalendarViewsSecondaryActionBar.getHeight();		
		//  selectedWeekParams.setMargins(0, marginTop, 0, 0);
		//  mSelectedWeekView.setTranslationY(0)��
		//  Month Fragment�� secondary actionbar�� mDayHeaderLayout �ٷ� �Ʒ� ���̴�
		mSelectedWeekViewTranslateAnimator = ValueAnimator.ofInt((int)mSelectedWeekViewTop, mSelectedWeekViewAnimToYValue);
		mSelectedWeekViewTranslateAnimator.setDuration(duration);		
		mSelectedWeekTranslateInterpolator = new TranslateInterpolator(mSelectedWeekViewTranslateAnimator, distance);
		mSelectedWeekViewTranslateAnimator.setInterpolator(mSelectedWeekTranslateInterpolator);

		mSelectedWeekViewTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float animatedFraction = valueAnimator.getAnimatedFraction();
				int value = (Integer) valueAnimator.getAnimatedValue();	
				
				mSelectedWeekView.setAnimationFraction(animatedFraction);
						
				boolean mustInvalidate = false;
				if (mSelectedWeekHaveEvent) {
					int alphaValue = 255 - (int) (255 * animatedFraction);
					mSelectedWeekView.setDontDrawEventExistenceCircleAlphaValue(alphaValue);
					mustInvalidate = true;
				}
				
				if (!mSetSelectedWeekDontWeekUpperLine) {
					if (value <= SecondaryActionbarBottom) {
						mSelectedWeekView.setDontDrawWeekUpperLine();						
						mSetSelectedWeekDontWeekUpperLine = true;
						mustInvalidate = true;
					}
				}				
				
				if ( (mSelectedWeekPattern == MonthWeekView.PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) ||
					(mSelectedWeekPattern == MonthWeekView.PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) ||
					(mSelectedWeekPattern == MonthWeekView.NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) || 
					(mSelectedWeekPattern == MonthWeekView.NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) ) {
										
					int alphaValue = (int) (255 * animatedFraction);
					//if (INFO) Log.i(TAG, "setSelectedWeekViewValueAnimator:aF=" + String.valueOf(animatedFraction) + ", apV=" + String.valueOf(alphaValue));
					mSelectedWeekView.setDrawSecondaryActionBarMonthDayHeaderAlphaValue(alphaValue);
					mustInvalidate = true;
				}
				
				if (mustInvalidate)
					mSelectedWeekView.invalidate();
				
				
				if (value < DAY_HEADER_HEIGHT) {					
					
					if (mDateIndicatorTextView.getVisibility() != View.VISIBLE)
						mDateIndicatorTextView.setVisibility(View.VISIBLE);
					
					int dateIndicatorHeight = (DATEINDICATOR_HEADER_HEIGHT + MONTHDAY_HEADER_HEIGHT) - (value + MONTHDAY_HEADER_HEIGHT);
					RelativeLayout.LayoutParams dateIndicatorTextViewParams = (LayoutParams) mDateIndicatorTextView.getLayoutParams();
					dateIndicatorTextViewParams.height = dateIndicatorHeight;
					mDateIndicatorTextView.setLayoutParams(dateIndicatorTextViewParams);
					
					float DateIndicatorHeaderHeightF = DATEINDICATOR_HEADER_HEIGHT;
					float valueF = value;
					float fraction = (DateIndicatorHeaderHeightF - valueF) / DateIndicatorHeaderHeightF;
					int alphaValue = (int) (255 * fraction);			
					int color = Color.argb(alphaValue, mCalendarSecondaryActionBarDateIndicatorTextColorRed, mCalendarSecondaryActionBarDateIndicatorTextColorGreen, mCalendarSecondaryActionBarDateIndicatorTextColorBlue);
					mDateIndicatorTextView.setTextColor(color);
					mDateIndicatorTextView.setText(mDateIndicatorText);
					
					float translationY = value;
					mDateIndicatorTextView.setTranslationY(translationY);
					
					//if (INFO) Log.i(TAG, "setSelectedWeekViewValueAnimator:alphaValue=" + String.valueOf(alphaValue));
				}
				
				mSelectedWeekView.setTranslationY(value); // top position�� ������� ���̴�
				                                          // ��, top position�� ���� 0���� �ƴ϶�,
				                                          // top margin�� mCalendarViewsSecondaryActionBar�� height�� �����Ǿ� �����Ƿ�,
				                                          // ���� translate y ���� 0���� �����ؾ�  mCalendarViewsSecondaryActionBar �ٷ� �Ʒ����� �̵��Ѵ�
			}
		});
		
		mSelectedWeekViewTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {		
				mPsudoExtendedDayView.setVisibility(View.VISIBLE);
				notifyAnimCompletion(SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION);
			}

			@Override
			public void onAnimationStart(Animator animator) {
				if (mSelectedWeekHaveEvent) {
					mSelectedWeekView.setDontDrawEventExistenceCircle();
				}
				
				if ( (mSelectedWeekPattern == MonthWeekView.PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) ||
					(mSelectedWeekPattern == MonthWeekView.PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) ||
					(mSelectedWeekPattern == MonthWeekView.NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) || 
					(mSelectedWeekPattern == MonthWeekView.NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN)) {
					mSelectedWeekView.setDrawSecondaryActionBarMonthDayHeader();
					//if (INFO) Log.i(TAG, "mSelectedWeekViewTranslateAnimator.onAnimationStart");
				}		
				
				/*RelativeLayout.LayoutParams dateIndicatorTextViewParams = (LayoutParams) mDateIndicatorTextView.getLayoutParams();
				int halfWidth = mDateIndicatorTextView.getWidth() / 2;
				int left = (int) ((mFragment.mWidth / 2) - halfWidth);
				int topMargin = DAY_HEADER_HEIGHT + MONTHDAY_HEADER_HEIGHT;
				dateIndicatorTextViewParams.setMargins(left, topMargin, 0, 0);*/
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});   		
		
	}	
	
	int mPsudoLowerLisViewContainerHeight;
	int mPsudoLowerLisViewContainerTop;
	TranslateAnimation mPusdoLowerListViewTranslateAnimation = null;
	public void setPusdoLowerLisViewAnim(long duration) {		
		float lowActionBarTop = mFragment.mLowerActionBar.getTop();		
		float lowerListViewTop = mPsudoLowerLisViewContainerTop; // singleTapView[selected view]�� getBottom ���� ���ϸ� �ȴ�
		float lowerListViewHeight = mPsudoLowerLisViewContainerHeight;	// visible ������ height�� �����ϴ� ���� ���� ������?	
		float toYValue = (lowActionBarTop - lowerListViewTop) / lowerListViewHeight;		
		
		mPusdoLowerListViewTranslateAnimation = new TranslateAnimation(
				Animation.ABSOLUTE, 0, //fromXValue 
				Animation.ABSOLUTE, 0,   //toXValue
                Animation.RELATIVE_TO_SELF, 0, /////////////////////////////////////////////////////////////////
                Animation.RELATIVE_TO_SELF, toYValue);
		
		long durationTime = (long) (duration * 0.30f);	;		
		mPusdoLowerListViewTranslateAnimation.setDuration(durationTime);	
		mPusdoLowerListViewTranslateAnimation.setInterpolator(new DecelerateInterpolator());
		mPusdoLowerListViewTranslateAnimation.setAnimationListener(PusdoLowerListViewTranslateAnimationListener);
	}	
	AnimationListener PusdoLowerListViewTranslateAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {		
			mPsudoLowerLisViewContainer.setVisibility(View.GONE);
			notifyAnimCompletion(PSUDO_LOWER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {						
		}		
	};		
		
	//int mPsudoExtendedDayViewHeight;
	//int mPsudoExtendedDayViewTop;
	//TranslateAnimation mPsudoExtendedDayViewTranslateAnimation = null;
	AnimationSet mPsudoExtendedDayViewTranslateAnimationSet = null;
	public void setPsudoExtendedDayViewAnim(long duration) {		
		//float lowActionBarTop = mFragment.mLowerActionBar.getTop();		
		//float lowerListViewTop = mPsudoLowerLisViewContainerTop; // singleTapView[selected view]�� getBottom ���� ���ϸ� �ȴ�
		//float lowerListViewHeight = mPsudoLowerLisViewContainerHeight;	// visible ������ height�� �����ϴ� ���� ���� ������?	
		//float fromYValue = (lowActionBarTop - lowerListViewTop) / lowerListViewHeight;	
		float fromYValue = 1;	
		
		AlphaAnimation alphaAnim = new AlphaAnimation(0, 1);				
		
		TranslateAnimation translateAnim = new TranslateAnimation(
				Animation.ABSOLUTE, 0, 
				Animation.ABSOLUTE, 0,   
		        Animation.RELATIVE_TO_SELF, fromYValue, 
		        Animation.RELATIVE_TO_SELF, 0);	
		
		mPsudoExtendedDayViewTranslateAnimationSet = new AnimationSet(true);
		mPsudoExtendedDayViewTranslateAnimationSet.addAnimation(alphaAnim);
		mPsudoExtendedDayViewTranslateAnimationSet.addAnimation(translateAnim);	
		mPsudoExtendedDayViewTranslateAnimationSet.setDuration(duration);		
		ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(mListView.getOriginalListViewHeight(), mPsudoExtendedDayViewTranslateAnimationSet);
		mPsudoExtendedDayViewTranslateAnimationSet.setInterpolator(enterInterpolator);
		mPsudoExtendedDayViewTranslateAnimationSet.setAnimationListener(mPsudoExtendedDayViewTranslateAnimationSetListener);
	}	
		
	AnimationListener mPsudoExtendedDayViewTranslateAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			notifyAnimCompletion(PSUDO_ENTENDEDDAY_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {						
		}		
	};
	
	public void makeSelectedWeekView() {
		// mFragment.mCalendarViewsSecondaryActionBar.getHeight() ����
		// ���� fragment�� MonthFragment�� SecondaryActionBar�� layout�� mDayHeaderLayout�� ����ϱ� ������
		// marginTop�� SecondaryActionBar�� mDayHeaderLayout �ٷ� �Ʒ��� �����Ѵ�
		// �׷��Ƿ� mSelectedWeekViewAnimToYValue ���� ������ �׻� 0�̹Ƿ� 
		// setTranslationY�� �������� 0���� �����Ǿ
		// SecondaryActionBar�� mDayHeaderLayout �ٷ� �Ʒ����� �̵��ϰ� �ȴ�
		int marginTop = mFragment.mCalendarViewsSecondaryActionBar.getHeight();		
		mSelectedWeekView = new MonthWeekView(mContext);
		RelativeLayout.LayoutParams selectedWeekParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		selectedWeekParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		selectedWeekParams.setMargins(0, marginTop, 0, 0);
		mSelectedWeekView.setLayoutParams(selectedWeekParams);	
		
		mSelectedWeekView.mIsSelectedWeekViewForExitMonthEnterDay = true; 
		mSelectedWeekView.mClickedDayIndex = mSingleTapUpView.getClickedDayIndex();
	}
	
	public HashMap<String, Integer> makeWeekViewDrawingParams(int weekPosition, int weekPattern, int weekHeight) {
		HashMap<String, Integer> weekDrawingParams = new HashMap<String, Integer>();                               
        
		// fix params
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_ORIGINAL_LISTVIEW_HEIGHT, mListView.getOriginalListViewHeight());
		//weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_PREVIOUS_WEEK_HEIGHT, mListView.getNMPrvMonthWeekHeight()); /////
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_MONTH_INDICATOR_HEIGHT, mListView.getNMMonthIndicatorHeight());
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NORMAL_WEEK_HEIGHT, mListView.getNMNormalWeekHeight());
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_NUM_DAYS, mFragment.mDaysPerWeek);  
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_START, mListView.getFirstDayOfWeek());
		
		// variable params
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK, weekPosition);				  
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN, weekPattern); 
		weekDrawingParams.put(MonthWeekView.VIEW_PARAMS_HEIGHT, weekHeight);
		
		return weekDrawingParams;
	}
	
	@SuppressWarnings("unchecked")
	public void makeAnimSelectedPositionIsFirstVisiblePositionInCase() {		
		
		HashMap<String, Integer> drawingParams = (HashMap<String, Integer>) mSingleTapUpView.getTag();	
		int weekPattern = 0;
		if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN)) {
			weekPattern = drawingParams.get(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN);            
        }
		
		int TopOffset = mSingleTapUpView.getTop();
		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
			// mSelectedDayTime�� ���� prv month week ���� next month week ������ �Ǵ�����
			// : ���õ� day�� prv/next week�� �����ϴ����� ���� y���� �޶�����
			//   selected day�� top �Ǵ� bottom�� ����ؼ� secondary������ ��� ��ġ�� ���ϴ����� �Ǵ�����				
			int prvMonthWeekNumers = mSingleTapUpView.getPrvMonthWeekDaysNum();
			int firstJulianDay = ETime.getJulianDay(mSingleTapUpView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//mSingleTapUpView.getFirstJulianDay();
			int nextMonthFirstJulainDay = firstJulianDay + prvMonthWeekNumers;
			
			// previous month�� last week�� ���Ե� DAY�� ���õ� ���
			if (mSelectedJulainDay < nextMonthFirstJulainDay) {
				
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
				
				if (TopOffset == 0) {
					mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DONT_VERTICAL_MOVE;					
					mSelectedWeekViewTop = 0;					
				}
				else {
					// TopOffset ���� �翬�� �������� ���̴�
					// :��� ���� ���� ���� ����...����� ���̶�� Selected Week�� First Visible Position�� ��ġ�ϴ� Week�� �ƴϴ�!!!
					mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DOWNWARD_SELECTEDWEEK_VERTICAL_MOVE;					
					mSelectedWeekViewTop = TopOffset;					
				}												
				
				mSelectedWeekView.setY(mSelectedWeekViewTop);
				
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
						MonthWeekView.PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
						mListView.getNMNormalWeekHeight());                               
               
				mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
                mFragment.mAdapter.sendEventsToView(mSelectedWeekView);        
                mSelectedWeekView.setVisibility(View.VISIBLE);
                mMonthViewLayout.addView(mSelectedWeekView);                
                mSelectedWeekVisibleHeight = mListView.getNMNormalWeekHeight() + TopOffset;/////////////////////////////////////////////////////
                
                HashMap<String, Integer> firstWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
						MonthWeekView.NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
						mListView.getNMFirstWeekHeight());                           	                
                
                mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom() - mListView.getNMFirstWeekHeight();
                int fromPosition = 1;
                makeLowerRegionView(fromPosition, firstWeekDrawingParams, mListView.getNMFirstWeekHeight(), 0);
                mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
			}
			else { // next month�� first week�� ���Ե� DAY�� ���õ� ���				
				HashMap<String, Integer> selectedWeekDrawingParams = null;               
				
				// ���õ� next month day�� top�� ����ؾ� �Ѵ�		
				// animation �Ǵ� ȯ�游 �����ϰ� �ִ�
				// animation�� �ϱ� ���� �Ϻ��� psudo listview�� ������ ���� ����� �ؾ� �Ѵ�
				// :�׷��� SelectedPosition Is FirstVisiblePosition �̹Ƿ� upper region�� �������� �ʾƵ� �ȴ�
				// selectedDayTop�� next month�� first week�� upper line�̴�
				int selectedDayTop = TopOffset + mListView.getNMNormalWeekHeight() + mListView.getNMMonthIndicatorHeight();
				if (selectedDayTop == 0) {
					mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);					
					mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DONT_VERTICAL_MOVE;
					
					mSelectedWeekViewTop = 0;
										
					selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition,							
							MonthWeekView.NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
							mListView.getNMFirstWeekHeight());				
				}
				else if (selectedDayTop < 0) {
					// ���� ���� ������ �ָ� �߶󳻰� ���� ���� ù �ֺ��� selected week�� ������ �Ѵ�
					// :���� �߶� �ʿ䰡 �ִ°�??? 
					//  ->����
					mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
					mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DOWNWARD_SELECTEDWEEK_VERTICAL_MOVE;		
					
					mSelectedWeekViewTop = selectedDayTop;					
					
					selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition,							
							MonthWeekView.NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
							mListView.getNMNormalWeekHeight());					
				}
				else { // selectedDayTop > 0
					// prv month last week�� next month indicator�� upper region���� �����ؾ� �Ѵ�
					mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
					
					int weeksHeightSum = mListView.getNMNormalWeekHeight() + mListView.getNMMonthIndicatorHeight();
					
					MonthWeekView prvMonthLastWeekViewUpperRegion = new MonthWeekView(mContext);
					LinearLayout.LayoutParams prvMonthLastWeekViewUpperRegionViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
					prvMonthLastWeekViewUpperRegion.setLayoutParams(prvMonthLastWeekViewUpperRegionViewParams);//////////////////////////////////////////////////////////////////////////// 					   
					HashMap<String, Integer> prvMonthLastWeekWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition,							
							MonthWeekView.PREVIOUS_MONTH_WEEK_NONE_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
							mListView.getNMNormalWeekHeight());  	                
					prvMonthLastWeekViewUpperRegion.setWeekParams(prvMonthLastWeekWeekDrawingParams, mTzId);
					mFragment.mAdapter.sendEventsToView(prvMonthLastWeekViewUpperRegion); 
					mPsudoUpperLisViewContainer.addView(prvMonthLastWeekViewUpperRegion);					
					
					MonthWeekView monthIndicatorViewUpperRegion = new MonthWeekView(mContext);
					LinearLayout.LayoutParams  monthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
					monthIndicatorViewUpperRegion.setLayoutParams(monthIndicatorViewParams);
					HashMap<String, Integer> monthIndicatorDrawingParams = makeWeekViewDrawingParams(mSelectedPosition,							
							MonthWeekView.NEXT_MONTH_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
							mListView.getNMMonthIndicatorHeight());			
					monthIndicatorViewUpperRegion.setWeekParams(monthIndicatorDrawingParams, mTzId);
					mFragment.mAdapter.sendEventsToView(monthIndicatorViewUpperRegion); 
					mPsudoUpperLisViewContainer.addView(monthIndicatorViewUpperRegion);                  

					// getTop
					// :Top position of this view relative to its parent
					int Top = mFirstVisibleWeekChild.getTop();					
					mPsudoUpperRegionHeight = weeksHeightSum;     
					mPsudoUpperRegionVisibleRegionHeight = Top + weeksHeightSum;
					mPusdoUpperListViewTranslateToYValue = Top + (-mPsudoUpperRegionVisibleRegionHeight);
					mSelectedWeekViewTop = mSingleTapUpView.getTop() + weeksHeightSum; //mPsudoUpperRegionVisibleRegionHeight;
										
					mPsudoUpperLisViewContainer.setY(Top);
					mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);				
					
					mMonthExitDayEnterAnimContext.mSelectedWeekMovement = UPWARD_SELECTEDWEEK_VERTICAL_MOVE;
					
					selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition,							
							MonthWeekView.NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
							mListView.getNMNormalWeekHeight());				 
				}			 			
				
				mSelectedWeekView.setY(mSelectedWeekViewTop);
				
				mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
                mFragment.mAdapter.sendEventsToView(mSelectedWeekView); 
                mSelectedWeekView.setVisibility(View.VISIBLE);
                mMonthViewLayout.addView(mSelectedWeekView);             
				
                mSelectedWeekVisibleHeight = mSingleTapUpView.getBottom();/////////////////////////////////////////////////////
                mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom();
				int fromPosition = 1; 
                makeLowerRegionView(fromPosition, 0, 0);
                mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
			}				
		}
		else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {			
					
			int topOffsetExceptMonthIndicator = TopOffset + mListView.getNMMonthIndicatorHeight();
			if (topOffsetExceptMonthIndicator == 0) {
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
				mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DONT_VERTICAL_MOVE;				
				mSelectedWeekViewTop = 0;			
			}
			else if (topOffsetExceptMonthIndicator < 0) {
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
				mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DOWNWARD_SELECTEDWEEK_VERTICAL_MOVE;				
				mSelectedWeekViewTop = topOffsetExceptMonthIndicator;				
			}
			else { // topOffsetExceptMonthIndicator > 0
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
				mMonthExitDayEnterAnimContext.mSelectedWeekMovement = UPWARD_SELECTEDWEEK_VERTICAL_MOVE;
				
				mSelectedWeekViewTop = topOffsetExceptMonthIndicator;			
				
				int weeksHeightSum = mListView.getNMMonthIndicatorHeight();
				// upper region�� �����ؾ� �Ѵ�
				MonthWeekView monthIndicatorViewUpperRegion = new MonthWeekView(mContext);
				LinearLayout.LayoutParams  monthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				monthIndicatorViewUpperRegion.setLayoutParams(monthIndicatorViewParams);//////////////////////////////////////////////////////////////////////////// 
				HashMap<String, Integer> monthIndicatorDrawingParams = makeWeekViewDrawingParams(mSelectedPosition,							
						MonthWeekView.MONTHIDICATOR_AND_UPPERLINE_OF_FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN,
						mListView.getNMMonthIndicatorHeight());			
				monthIndicatorViewUpperRegion.setWeekParams(monthIndicatorDrawingParams, mTzId);
				mFragment.mAdapter.sendEventsToView(monthIndicatorViewUpperRegion); 
				mPsudoUpperLisViewContainer.addView(monthIndicatorViewUpperRegion); 					
							
				mPsudoUpperRegionHeight = weeksHeightSum;     
				mPsudoUpperRegionVisibleRegionHeight = mFirstVisibleWeekChild.getTop() + weeksHeightSum;
				mPusdoUpperListViewTranslateToYValue = mFirstVisibleWeekChild.getTop() + (-mPsudoUpperRegionVisibleRegionHeight);
				mPsudoUpperLisViewContainer.setY(mFirstVisibleWeekChild.getTop());
				mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);				
			}
			
			mSelectedWeekView.setY(mSelectedWeekViewTop);				
			
			HashMap<String, Integer> selectedWeekDrawingParams = null;
			selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
					MonthWeekView.NORMAL_WEEK_NONE_UPPERLINE_PATTERN,
					mListView.getNMNormalWeekHeight()); 
			
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(mSelectedWeekView);
            mSelectedWeekView.setVisibility(View.VISIBLE);
            mMonthViewLayout.addView(mSelectedWeekView);  
            
            mSelectedWeekVisibleHeight = mSingleTapUpView.getBottom();/////////////////////////////////////////////////////
            mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom();
            int fromPosition = 1;
            makeLowerRegionView(fromPosition, 0, 0);
            mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
		}
		else {
			mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
			
			if (TopOffset == 0) {
				mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DONT_VERTICAL_MOVE;				
				mSelectedWeekViewTop = 0;				
			}
			else if (TopOffset < 0) {
				mMonthExitDayEnterAnimContext.mSelectedWeekMovement = DOWNWARD_SELECTEDWEEK_VERTICAL_MOVE;				
				mSelectedWeekViewTop = TopOffset;				
			}
			
			mSelectedWeekView.setY(mSelectedWeekViewTop);		
			
			HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
					MonthWeekView.NORMAL_WEEK_NONE_UPPERLINE_PATTERN,
					mListView.getNMNormalWeekHeight());          
			
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(mSelectedWeekView);       
            mSelectedWeekView.setVisibility(View.VISIBLE);
            mMonthViewLayout.addView(mSelectedWeekView);  
            
            mSelectedWeekVisibleHeight = mSingleTapUpView.getBottom();/////////////////////////////////////////////////////
            mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom();
            int fromPosition = 1;
            makeLowerRegionView(fromPosition, 0, 0);
            mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
		}	
	}
	
	@SuppressWarnings("unchecked")
	public void makeAnimSelectedPositionIsLastVisiblePositionInCase() {
		
		HashMap<String, Integer> drawingParams = (HashMap<String, Integer>) mSingleTapUpView.getTag();	
		int weekPattern = 0;
		if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN)) {
			weekPattern = drawingParams.get(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN);            
        }
		
		// �� Case������ TopOffset�� �ǹ̰� ����!?
		int BottomOffset = mSingleTapUpView.getBottom();
		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
			int prvMonthWeekNumers = mSingleTapUpView.getPrvMonthWeekDaysNum();
			int firstJulianDay = ETime.getJulianDay(mSingleTapUpView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//mSingleTapUpView.getFirstJulianDay();
			int nextMonthFirstJulainDay = firstJulianDay + prvMonthWeekNumers;
			
			// prv month�� last week�� ���Ե� DAY�� ���õ� ���
			if (mSelectedJulainDay < nextMonthFirstJulainDay) {				
				int prvMonthWeekBottom = BottomOffset - mListView.getNMFirstWeekHeight();
				
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
						MonthWeekView.PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
						mListView.getNMNormalWeekHeight());         
                mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
                
				// ���� �Ϸ�!!!
				if (prvMonthWeekBottom <= mListView.getOriginalListViewHeight()) {
					mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);					
					makeUpperRegionView();					
					mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);				
						                
	                mFragment.mAdapter.sendEventsToView(mSelectedWeekView);       
	                mSelectedWeekViewTop = mSingleTapUpView.getTop();/////////////////////
	                mSelectedWeekView.setY(mSelectedWeekViewTop);
	                mSelectedWeekView.setVisibility(View.VISIBLE);
	                mMonthViewLayout.addView(mSelectedWeekView);                
					
	                mSelectedWeekVisibleHeight = mListView.getNMNormalWeekHeight();
	                
					// next month week�� Lower region�� �� �ȴ�
	                MonthWeekView FirstWeekViewLowerRegion = new MonthWeekView(mContext);
	            	LinearLayout.LayoutParams firstWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	            	FirstWeekViewLowerRegion.setLayoutParams(firstWeekViewParams);//////////////////////////////////////////////////////////////////////////// 
	                HashMap<String, Integer> firstWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
	    					MonthWeekView.NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
	    					mListView.getNMFirstWeekHeight());
	                FirstWeekViewLowerRegion.setWeekParams(firstWeekDrawingParams, mTzId);
	                mFragment.mAdapter.sendEventsToView(FirstWeekViewLowerRegion); 
	                
	                // lower region���� �� �ϳ��� week[next month�� first week]�� ���� �Ѵ�
	                int oriListViewHeight = mListView.getOriginalListViewHeight();   
	        		int marginBottom = oriListViewHeight - (mPsudoUpperRegionVisibleRegionHeight + mSelectedWeekVisibleHeight + mListView.getNMFirstWeekHeight());    			
	        		RelativeLayout.LayoutParams pusdoViewContainerParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);    			
	        		pusdoViewContainerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	
	        		pusdoViewContainerParams.setMargins(0, 0, 0, marginBottom);
	        		mPsudoLowerLisViewContainer.setOrientation(LinearLayout.VERTICAL);    			
	        		mPsudoLowerLisViewContainer.setLayoutParams(pusdoViewContainerParams); 
	        		
	        		mPsudoLowerLisViewContainer.addView(FirstWeekViewLowerRegion); 
	        		mPsudoLowerLisViewContainerHeight =  mListView.getNMFirstWeekHeight();
	        		mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom() - mListView.getNMFirstWeekHeight();
	        		mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);	                
				}
				// ���� �Ϸ�!!!
				else if (prvMonthWeekBottom > mListView.getOriginalListViewHeight()) {
					mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_ANIM_CASE);					
					makeUpperRegionView();					
					mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);
					
					               
	                mFragment.mAdapter.sendEventsToView(mSelectedWeekView);       
	                mSelectedWeekViewTop = mSingleTapUpView.getTop();/////////////////////
	                mSelectedWeekView.setY(mSelectedWeekViewTop);
	                mSelectedWeekView.setVisibility(View.VISIBLE);
	                mMonthViewLayout.addView(mSelectedWeekView);		
				}				
			}
			else { // next month�� first week�� ���Ե� DAY�� ���õ� ���
				// next month first week�� ���õǾ��ٴ� ���� lower region�� �ʿ� ���ٴ� ���̴� : �ֳĸ� view�� last visible week�̱� �����̴�
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_ANIM_CASE);								
				makeUpperRegionView();
				mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);
				// prv month last week�� mPsudoUpperLisViewTailContainer�� ������ �߰��ؾ� �Ѵ�
				MonthWeekView prvMonthLastWeekView = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams prvMonthLastWeekParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	prvMonthLastWeekView.setLayoutParams(prvMonthLastWeekParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> prvMonthLastWeekParamsDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
    					MonthWeekView.PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
    					mListView.getNMNormalWeekHeight());
                prvMonthLastWeekView.setWeekParams(prvMonthLastWeekParamsDrawingParams, mTzId);                
                mFragment.mAdapter.sendEventsToView(prvMonthLastWeekView);                
                mPsudoUpperLisViewTailContainer.addView(prvMonthLastWeekView);
                
                // next month�� month indicator�� mPsudoUpperListViewTailContainer�� ������ �߰��ؾ� �Ѵ�
                MonthWeekView MonthIndicatorView = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams MonthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	MonthIndicatorView.setLayoutParams(MonthIndicatorViewParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> MonthIndicatorViewParamsDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
    					MonthWeekView.NEXT_MONTH_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
    					mListView.getNMMonthIndicatorHeight());
                MonthIndicatorView.setWeekParams(MonthIndicatorViewParamsDrawingParams, mTzId);
                
                mFragment.mAdapter.sendEventsToView(MonthIndicatorView);
                mPsudoUpperLisViewTailContainer.addView(MonthIndicatorView);
                mPsudoUpperLisViewTailContainer.setY(mPsudoUpperRegionVisibleRegionHeight);
                mPsudoUpperLisViewTailContainer.setVisibility(View.VISIBLE);                
                
                mPsudoUpperLisViewTailTranslateToYValue = -(mListView.getNMNormalWeekHeight() + mListView.getNMMonthIndicatorHeight());       		
        		mPusdoUpperListViewTranslateToYValue = mPusdoUpperListViewTranslateToYValue + mPsudoUpperLisViewTailTranslateToYValue;        		       		
                
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
						MonthWeekView.NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
						mListView.getNMNormalWeekHeight());
				mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
				mFragment.mAdapter.sendEventsToView(mSelectedWeekView);       
	            mSelectedWeekViewTop = mSingleTapUpView.getTop() + mListView.getNMNormalWeekHeight() + mListView.getNMMonthIndicatorHeight();
                mSelectedWeekView.setY(mSelectedWeekViewTop);
                mSelectedWeekView.setVisibility(View.VISIBLE);
	            mMonthViewLayout.addView(mSelectedWeekView);  
	            
			}				
		}
		else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
			mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_ANIM_CASE);								
			makeUpperRegionView();			
			mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);
			
			MonthWeekView MonthIndicatorView = new MonthWeekView(mContext);
        	LinearLayout.LayoutParams MonthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        	MonthIndicatorView.setLayoutParams(MonthIndicatorViewParams);//////////////////////////////////////////////////////////////////////////// 
            HashMap<String, Integer> MonthIndicatorViewParamsDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
					MonthWeekView.MONTHIDICATOR_AND_UPPERLINE_OF_FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN,
					mListView.getNMMonthIndicatorHeight());
            MonthIndicatorView.setWeekParams(MonthIndicatorViewParamsDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(MonthIndicatorView);
            mPsudoUpperLisViewTailContainer.addView(MonthIndicatorView);
            mPsudoUpperLisViewTailContainer.setY(mPsudoUpperRegionVisibleRegionHeight);
            mPsudoUpperLisViewTailContainer.setVisibility(View.VISIBLE);                            
            
            mPsudoUpperLisViewTailTranslateToYValue = -(mListView.getNMMonthIndicatorHeight());  
            mPusdoUpperListViewTranslateToYValue = mPusdoUpperListViewTranslateToYValue + mPsudoUpperLisViewTailTranslateToYValue;    
            		
			HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
					MonthWeekView.NORMAL_WEEK_NONE_UPPERLINE_PATTERN,
					mListView.getNMNormalWeekHeight());
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(mSelectedWeekView);       
            mSelectedWeekViewTop = mSingleTapUpView.getTop() + mListView.getNMMonthIndicatorHeight();
            mSelectedWeekView.setY(mSelectedWeekViewTop);
            mSelectedWeekView.setVisibility(View.VISIBLE);
            mMonthViewLayout.addView(mSelectedWeekView);  
			
		}
		else {
			mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_ANIM_CASE);						
			makeUpperRegionView();
			mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);				
			
			HashMap<String, Integer> selectedWeekDrawingParams = (HashMap<String, Integer>) mSingleTapUpView.getTag();
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(mSelectedWeekView);   
            mSelectedWeekViewTop = mSingleTapUpView.getTop();
            mSelectedWeekView.setY(mSelectedWeekViewTop);
			mSelectedWeekView.setVisibility(View.VISIBLE);	
            mMonthViewLayout.addView(mSelectedWeekView);   
		}
	}

	@SuppressWarnings("unchecked")
	public void makeAnimSelectedPositionIsNotBothEndVisiblePositionInCase() {
		//mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);	
		
		HashMap<String, Integer> drawingParams = (HashMap<String, Integer>) mSingleTapUpView.getTag();	
		int weekPattern = 0;
		if (drawingParams.containsKey(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN)) {
			weekPattern = drawingParams.get(MonthWeekView.VIEW_PARAMS_WEEK_PATTERN);            
        }
		
		//int TopOffset = mSingleTapUpView.getTop();
		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
			// prv Month last week�� next Month first week�� ���� selected week view�� �����ȴ�
			int prvMonthWeekNumers = mSingleTapUpView.getPrvMonthWeekDaysNum();
			int firstJulianDay = ETime.getJulianDay(mSingleTapUpView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);//mSingleTapUpView.getFirstJulianDay();
			int nextMonthFirstJulainDay = firstJulianDay + prvMonthWeekNumers;
			
			if (mSelectedJulainDay < nextMonthFirstJulainDay) {	
				// upper region�� �����Ѵ�
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);
				makeUpperRegionView();				
				mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);				
								
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
						MonthWeekView.PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
						mListView.getNMNormalWeekHeight());                             
                mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);				
                mFragment.mAdapter.sendEventsToView(mSelectedWeekView); 
                mSelectedWeekViewTop = mSingleTapUpView.getTop();
                mSelectedWeekView.setY(mSelectedWeekViewTop);
    			mSelectedWeekView.setVisibility(View.VISIBLE);	
                mMonthViewLayout.addView(mSelectedWeekView);
                                
                mSelectedWeekVisibleHeight = mListView.getNMNormalWeekHeight();/////////////////////////////////////////////////////
                
                // next month�� first week�� lower region�� ù��° week�� �ȴ�
				MonthWeekView FirstWeekViewLowerRegion = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams firstWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	FirstWeekViewLowerRegion.setLayoutParams(firstWeekViewParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> firstWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
    					MonthWeekView.NEXT_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
    					mListView.getNMFirstWeekHeight());                
                FirstWeekViewLowerRegion.setWeekParams(firstWeekDrawingParams, mTzId);
                mFragment.mAdapter.sendEventsToView(FirstWeekViewLowerRegion); 
                
                mPsudoLowerLisViewContainer.addView(FirstWeekViewLowerRegion);
                	                
                mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom() - mListView.getNMFirstWeekHeight();
                int fromPosition = (mSelectedPosition - mFirstVisiblePosition) + 1;///////////////////////////////////////////////////////////////////////////////////
                makeLowerRegionView(fromPosition, mListView.getNMFirstWeekHeight(), mPsudoUpperRegionVisibleRegionHeight);
                mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
			}
			else {
				mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);				
				makeUpperRegionView();		
				mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);
				// prv month�� last week�� upper region�� last week��  �ȴ�
				MonthWeekView lastWeekViewUpperRegion = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams lastWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	lastWeekViewUpperRegion.setLayoutParams(lastWeekViewParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> lastWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
    					MonthWeekView.PREVIOUS_MONTH_WEEK_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
    					mListView.getNMNormalWeekHeight()); 
                lastWeekViewUpperRegion.setWeekParams(lastWeekDrawingParams, mTzId);
                mFragment.mAdapter.sendEventsToView(lastWeekViewUpperRegion); 
                mPsudoUpperLisViewTailContainer.addView(lastWeekViewUpperRegion);                                  
                
                MonthWeekView MonthIndicatorView = new MonthWeekView(mContext);
            	LinearLayout.LayoutParams MonthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	MonthIndicatorView.setLayoutParams(MonthIndicatorViewParams);//////////////////////////////////////////////////////////////////////////// 
                HashMap<String, Integer> MonthIndicatorViewParamsDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
    					MonthWeekView.NEXT_MONTH_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
    					mListView.getNMMonthIndicatorHeight());
                MonthIndicatorView.setWeekParams(MonthIndicatorViewParamsDrawingParams, mTzId);
                mFragment.mAdapter.sendEventsToView(MonthIndicatorView);                                              
                mPsudoUpperLisViewTailContainer.addView(MonthIndicatorView);                
                mPsudoUpperLisViewTailContainer.setY(mPsudoUpperRegionVisibleRegionHeight);
                mPsudoUpperLisViewTailContainer.setVisibility(View.VISIBLE);                
                
                int upperRegionTailHeight = mListView.getNMNormalWeekHeight() + mListView.getNMMonthIndicatorHeight();
                mPsudoUpperLisViewTailTranslateToYValue = -(upperRegionTailHeight);       		
        		mPusdoUpperListViewTranslateToYValue = mPusdoUpperListViewTranslateToYValue + mPsudoUpperLisViewTailTranslateToYValue;                
								
				HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
						MonthWeekView.NEXT_MONTH_WEEK_NONE_MONTHIDICATOR_AND_UPPERLINE_OF_TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN,
						mListView.getNMNormalWeekHeight());				              
				mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
                mFragment.mAdapter.sendEventsToView(mSelectedWeekView); 
                mSelectedWeekViewTop = mSingleTapUpView.getTop() + upperRegionTailHeight;
                mSelectedWeekView.setY(mSelectedWeekViewTop);
    			mSelectedWeekView.setVisibility(View.VISIBLE);	
                
                mMonthViewLayout.addView(mSelectedWeekView);                
                
                mSelectedWeekVisibleHeight = mListView.getNMNormalWeekHeight();/////////////////////////////////////////////////////
                
                mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom();
                int fromPosition = (mSelectedPosition - mFirstVisiblePosition) + 1;
                int UpperRegionVisibleRegionHeight = mPsudoUpperRegionVisibleRegionHeight + upperRegionTailHeight;
                makeLowerRegionView(fromPosition,0, UpperRegionVisibleRegionHeight);
                mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
			}
		}
		else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
			mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);
			makeUpperRegionView();
			mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);
			
			MonthWeekView MonthIndicatorView = new MonthWeekView(mContext);
        	LinearLayout.LayoutParams MonthIndicatorViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        	MonthIndicatorView.setLayoutParams(MonthIndicatorViewParams);//////////////////////////////////////////////////////////////////////////// 
            HashMap<String, Integer> MonthIndicatorViewParamsDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
					MonthWeekView.MONTHIDICATOR_AND_UPPERLINE_OF_FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN,
					mListView.getNMMonthIndicatorHeight());
            MonthIndicatorView.setWeekParams(MonthIndicatorViewParamsDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(MonthIndicatorView);                             
            mPsudoUpperLisViewTailContainer.addView(MonthIndicatorView);                
            mPsudoUpperLisViewTailContainer.setY(mPsudoUpperRegionVisibleRegionHeight);
            mPsudoUpperLisViewTailContainer.setVisibility(View.VISIBLE);                
            
            int upperRegionTailHeight = mListView.getNMMonthIndicatorHeight();
            mPsudoUpperLisViewTailTranslateToYValue = -upperRegionTailHeight;       		
    		mPusdoUpperListViewTranslateToYValue = mPusdoUpperListViewTranslateToYValue + mPsudoUpperLisViewTailTranslateToYValue;                
    					
			HashMap<String, Integer> selectedWeekDrawingParams = makeWeekViewDrawingParams(mSelectedPosition, 
					MonthWeekView.NORMAL_WEEK_NONE_UPPERLINE_PATTERN,
					mListView.getNMNormalWeekHeight());
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(mSelectedWeekView);    
            mSelectedWeekViewTop = mSingleTapUpView.getTop() + upperRegionTailHeight;
            mSelectedWeekView.setY(mSelectedWeekViewTop);				
			mSelectedWeekView.setVisibility(View.VISIBLE);		
            mMonthViewLayout.addView(mSelectedWeekView);			
			
            //mSelectedWeekVisibleHeight = mSingleTapUpView.getHeight() - upperRegionTailHeight;
            mSelectedWeekVisibleHeight = mListView.getNMNormalWeekHeight(); 
            
            mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom();
            int fromPosition = (mSelectedPosition - mFirstVisiblePosition) + 1;
            int UpperRegionVisibleRegionHeight = mPsudoUpperRegionVisibleRegionHeight + upperRegionTailHeight;
            // �ᱹ UpperRegionVisibleRegionHeight�� marginBottom�� ����ϱ� ���� ���̴�
			makeLowerRegionView(fromPosition, 0, UpperRegionVisibleRegionHeight);
			mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
		}
		else {
			mMonthExitDayEnterAnimContext = new MonthExitDayEnterAnimContext(UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE);
			makeUpperRegionView();			
			mPsudoUpperLisViewContainer.setVisibility(View.VISIBLE);			
			
			HashMap<String, Integer> selectedWeekDrawingParams = (HashMap<String, Integer>) mSingleTapUpView.getTag();
			mSelectedWeekView.setWeekParams(selectedWeekDrawingParams, mTzId);
            mFragment.mAdapter.sendEventsToView(mSelectedWeekView); 
            mSelectedWeekViewTop = mSingleTapUpView.getTop();
            mSelectedWeekView.setY(mSelectedWeekViewTop);			
			mSelectedWeekView.setVisibility(View.VISIBLE);		
            mMonthViewLayout.addView(mSelectedWeekView);			
			
            mSelectedWeekVisibleHeight = mSingleTapUpView.getHeight();
            
            mPsudoLowerLisViewContainerTop = mSingleTapUpView.getBottom();
            int fromPosition = (mSelectedPosition - mFirstVisiblePosition) + 1;
			makeLowerRegionView(fromPosition, 0, mPsudoUpperRegionVisibleRegionHeight);
			mPsudoLowerLisViewContainer.setVisibility(View.VISIBLE);
		}
	}
	
	
	int mPsudoUpperRegionVisibleRegionHeight;
	int mPsudoUpperRegionHeight;
	int mPsudoUpperRegionBottom;
	@SuppressWarnings("unchecked")
	public void makeUpperRegionView() {	
		int weeksHeightSum = 0;
		int upperRegionWeekNumbers = mSelectedPosition - mFirstVisiblePosition;		
		for (int i=0; i<upperRegionWeekNumbers; i++) {
			MonthWeekView weekChildOfListView = (MonthWeekView) mListView.getChildAt(i);
			HashMap<String, Integer> weekChildOfListViewDrawingParams = (HashMap<String, Integer>) weekChildOfListView.getTag();	
			if (weekChildOfListViewDrawingParams.containsKey(MonthWeekView.VIEW_PARAMS_HEIGHT)) {
	            int height = weekChildOfListViewDrawingParams.get(MonthWeekView.VIEW_PARAMS_HEIGHT);
	            weeksHeightSum = weeksHeightSum + height;
	        }			
		}		
				
		int Top = mFirstVisibleWeekChild.getTop();					
		mPsudoUpperRegionHeight = weeksHeightSum;     
		mPsudoUpperRegionVisibleRegionHeight = Top + mPsudoUpperRegionHeight;
		mPusdoUpperListViewTranslateToYValue = Top + (-mPsudoUpperRegionVisibleRegionHeight);					
		mPsudoUpperLisViewContainer.setY(Top);
				
		for (int i=0; i<upperRegionWeekNumbers; i++) {
			MonthWeekView weekChildOfListView = (MonthWeekView) mListView.getChildAt(i);				
			
			HashMap<String, Integer> weekChildOfListViewDrawingParams = (HashMap<String, Integer>) weekChildOfListView.getTag();	
			
			MonthWeekView weekView = new MonthWeekView(mContext);
	    	LinearLayout.LayoutParams weekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	    	weekView.setLayoutParams(weekViewParams);////////////////////////////////////////////////////////////////////////////	        
	        weekView.setWeekParams(weekChildOfListViewDrawingParams, mTzId);
	        
	        mFragment.mAdapter.sendEventsToView(weekView);                
	        mPsudoUpperLisViewContainer.addView(weekView);     
		}				
	}
	
	int mSelectedWeekVisibleHeight;
		
	@SuppressWarnings("unchecked")
	public void makeLowerRegionView(int fromPosition, int addedWeekHeight, int UpperRegionVisibleRegionHeight) {
		// ���࿡ ���� ����: 2014/11/30�� �����Ͽ��� ���,,,???
		//int weeksHeightSum = 0;
		int weeksHeightSum = addedWeekHeight;
		//int lowerRegionWeekNumbers = mLastVisiblePosition - mSelectedPosition;
		//int firstViewIndexOfLowerRegion = mSelectedPosition - mFirstVisiblePosition;
		int end = (mLastVisiblePosition - mFirstVisiblePosition) + 1;		
		
		//for (int i=firstViewIndexOfLowerRegion; i<end; i++) {
		for (int i=fromPosition; i<end; i++) {
			MonthWeekView weekChildOfListView = (MonthWeekView) mListView.getChildAt(i);
			HashMap<String, Integer> weekChildOfListViewDrawingParams = (HashMap<String, Integer>) weekChildOfListView.getTag();	
			if (weekChildOfListViewDrawingParams.containsKey(MonthWeekView.VIEW_PARAMS_HEIGHT)) {
	            int height = weekChildOfListViewDrawingParams.get(MonthWeekView.VIEW_PARAMS_HEIGHT);
	            weeksHeightSum = weeksHeightSum + height;
	        }
		}
		
		mPsudoLowerLisViewContainerHeight = weeksHeightSum;
		
		int oriListViewHeight = mListView.getOriginalListViewHeight();   
		int marginBottom = oriListViewHeight - (UpperRegionVisibleRegionHeight + mSelectedWeekVisibleHeight + weeksHeightSum);    			
		RelativeLayout.LayoutParams pusdoViewContainerParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);    			
		pusdoViewContainerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);		
		pusdoViewContainerParams.setMargins(0, 0, 0, marginBottom);
		mPsudoLowerLisViewContainer.setOrientation(LinearLayout.VERTICAL);    			
		mPsudoLowerLisViewContainer.setLayoutParams(pusdoViewContainerParams); 		
		
		for (int i=fromPosition; i<end; i++) {
			MonthWeekView weekChildOfListView = (MonthWeekView) mListView.getChildAt(i);
			
			HashMap<String, Integer> weekChildOfListViewDrawingParams = (HashMap<String, Integer>) weekChildOfListView.getTag();			
			MonthWeekView weekView = new MonthWeekView(mContext);
	    	LinearLayout.LayoutParams weekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	    	weekView.setLayoutParams(weekViewParams);////////////////////////////////////////////////////////////////////////////	        
	        weekView.setWeekParams(weekChildOfListViewDrawingParams, mTzId);
	        
	        mFragment.mAdapter.sendEventsToView(weekView);                
	        mPsudoLowerLisViewContainer.addView(weekView);   
		}	
	}
		
	@SuppressWarnings("unchecked")
	public void makeLowerRegionView(int fromPosition, HashMap<String, Integer> firstWeekDrawingParams, int firstWeekHeight, int UpperRegionVisibleRegionHeight) {		
		MonthWeekView FirstWeekViewLowerRegion = new MonthWeekView(mContext);
    	LinearLayout.LayoutParams firstWeekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	FirstWeekViewLowerRegion.setLayoutParams(firstWeekViewParams);    	
    	FirstWeekViewLowerRegion.setWeekParams(firstWeekDrawingParams, mTzId);
        mFragment.mAdapter.sendEventsToView(FirstWeekViewLowerRegion);
    	
		int weeksHeightSum = firstWeekHeight;		
		int end = (mLastVisiblePosition - mFirstVisiblePosition) + 1;		
		
		for (int i=fromPosition; i<end; i++) {
			MonthWeekView weekChildOfListView = (MonthWeekView) mListView.getChildAt(i);
			HashMap<String, Integer> weekChildOfListViewDrawingParams = (HashMap<String, Integer>) weekChildOfListView.getTag();	
			if (weekChildOfListViewDrawingParams.containsKey(MonthWeekView.VIEW_PARAMS_HEIGHT)) {
	            int height = weekChildOfListViewDrawingParams.get(MonthWeekView.VIEW_PARAMS_HEIGHT);
	            weeksHeightSum = weeksHeightSum + height;
	        }
		}
		
		mPsudoLowerLisViewContainerHeight = weeksHeightSum;
				
		int marginBottom = mListView.getOriginalListViewHeight() - (UpperRegionVisibleRegionHeight + mSelectedWeekVisibleHeight + weeksHeightSum);    			
		RelativeLayout.LayoutParams pusdoViewContainerParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);    			
		pusdoViewContainerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);		
		pusdoViewContainerParams.setMargins(0, 0, 0, marginBottom);
		mPsudoLowerLisViewContainer.setOrientation(LinearLayout.VERTICAL);    			
		mPsudoLowerLisViewContainer.setLayoutParams(pusdoViewContainerParams); 
		
		mPsudoLowerLisViewContainer.addView(FirstWeekViewLowerRegion); 
		
		for (int i=fromPosition; i<end; i++) {
			MonthWeekView weekChildOfListView = (MonthWeekView) mListView.getChildAt(i);
			
			HashMap<String, Integer> weekChildOfListViewDrawingParams = (HashMap<String, Integer>) weekChildOfListView.getTag();			
			MonthWeekView weekView = new MonthWeekView(mContext);
	    	LinearLayout.LayoutParams weekViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	    	weekView.setLayoutParams(weekViewParams);////////////////////////////////////////////////////////////////////////////	        
	        weekView.setWeekParams(weekChildOfListViewDrawingParams, mTzId);
	        
	        mFragment.mAdapter.sendEventsToView(weekView);                
	        mPsudoLowerLisViewContainer.addView(weekView);   
		}	
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
	
	private static final int MINIMUM_VELOCITY = 3300;    
	//private static final int MINIMUM_VELOCITY = 100; // for test	
	//private static final int DEFAULT_VELOCITY = 4400; // ������ ��Ʈ
    private static final int DEFAULT_VELOCITY = 6500; //
	//private static final int DEFAULT_VELOCITY = 100; // for test
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
	
	
	public final int SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE = 1;
	public final int UPPERREGION_AND_SELECTEDWEEK_ANIM_CASE = 2;
	public final int UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_ANIM_CASE = 3;
	public final int UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE = 4;
	public final int UPPERREGION_AND_UPPERREGIONTAIL_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE = 5;
	
	public final int UPWARD_SELECTEDWEEK_VERTICAL_MOVE = 1;
	public final int DOWNWARD_SELECTEDWEEK_VERTICAL_MOVE = 2;
	public final int DONT_VERTICAL_MOVE = 3;
	public class MonthExitDayEnterAnimContext {
		int mAnimCase;
		int mSelectedWeekMovement;
		int mWeekPattern;
		
		SelectedWeekANDLowerRegionCase mSelectedWeekANDLowerRegionCase = null;
		UpperRegionAndSelectedWeekCase mUpperRegionAndSelectedWeekCase = null;
		UpperRegionAndSelectedWeekANDLowerRegionCase mUpperRegionAndSelectedWeekANDLowerRegionCase = null;
		
		public MonthExitDayEnterAnimContext(int animCase) {
			mAnimCase = animCase;
			makeCaseObject();
		}
		
		public void makeCaseObject() {
			switch(mAnimCase) {
			case SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE:
				mSelectedWeekANDLowerRegionCase = new SelectedWeekANDLowerRegionCase();
				break;
			case UPPERREGION_AND_SELECTEDWEEK_ANIM_CASE:
				mUpperRegionAndSelectedWeekCase = new UpperRegionAndSelectedWeekCase();
				break;
			case UPPERREGION_AND_SELECTEDWEEK_AND_LOWERREGION_ANIM_CASE:
				mUpperRegionAndSelectedWeekANDLowerRegionCase = new UpperRegionAndSelectedWeekANDLowerRegionCase();
				break;
			}
		}
		
		
		public class SelectedWeekANDLowerRegionCase {
			int mSelectedWeekAnimTransDelta;
		}
		
		public class UpperRegionAndSelectedWeekCase {
			
		}
		
		public class UpperRegionAndSelectedWeekANDLowerRegionCase {
			
		}
		
	}
	
	int mDayViewWidth;
	int mDayViewHeight;
	public void calcFrameLayoutDimension() {
		mDayViewWidth = mFragment.getResources().getDisplayMetrics().widthPixels;    	
				
		int mDeviceHeight = mFragment.getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = mFragment.getActivity().getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle);    	
    	    	
    	int mStatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (mStatusBarHeight == -1) {
        	Utils.setSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
    	
    	int mAppHeight = mDeviceHeight - mStatusBarHeight;
		
		int mActionBarHeight = (int)mFragment.getResources().getDimension(R.dimen.calendar_view_upper_actionbar_height);    
		
		int mAnticipationFragmentFrameLayoutHeight = mAppHeight - mActionBarHeight;
		
		int DAY_HEADER_HEIGHT = (int)mFragment.getResources().getDimension(R.dimen.day_header_height);
    	int mLowerActionBarHeight = (int)mFragment.getResources().getDimension(R.dimen.calendar_view_lower_actionbar_height);
    	
    	int mAnticipationMonthDayListViewWidth = rectgle.right - rectgle.left;
    	int mAnticipationMonthDayListViewHeight = mDeviceHeight - mStatusBarHeight - mActionBarHeight - DAY_HEADER_HEIGHT - mLowerActionBarHeight;  
    	
    	int mAnticipationMonthDayListViewNormalWeekHeight = (int) (mAnticipationMonthDayListViewHeight * 0.16f);
    	
    	int MONTHDAY_HEADER_HEIGHT = (int) (mAnticipationMonthDayListViewNormalWeekHeight * CalendarViewsSecondaryActionBar.MONTHDAY_HEADER_HEIGHT_RATIO);     	
    	int DATEINDICATOR_HEADER_HEIGHT = (int) (mAnticipationMonthDayListViewNormalWeekHeight * CalendarViewsSecondaryActionBar.DATEINDICATOR_HEADER_HEIGHT_RATIO);
    	int mDayViewSecondaryActionBarHeight = DAY_HEADER_HEIGHT + MONTHDAY_HEADER_HEIGHT + DATEINDICATOR_HEADER_HEIGHT;  
    	
    	mDayViewHeight = mAnticipationFragmentFrameLayoutHeight - mDayViewSecondaryActionBarHeight - mLowerActionBarHeight;    	
	}	
	
	
	public class DrawableBolderLinearLayoutWithoutPadding extends LinearLayout {

		Paint mPaint;
		float mStrokeWidth;
		
		int mWidth;
		int mHeight;
		
		
		public DrawableBolderLinearLayoutWithoutPadding(Context context) {
			super(context);
			
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			//eventViewItemUnderLineColor
			mPaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));	
			
			mPaint.setStyle(Style.STROKE);
			// <dimen name="eventItemLayoutUnderLineHeight">1dp</dimen>
			//The stroke width is defined in pixels
			mStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
			mPaint.setStrokeWidth(mStrokeWidth); 			
		}		
		

		@Override
		protected void onDraw(Canvas canvas) {
			
			float startX = 0;
	        float startY = mHeight - mStrokeWidth; // y ���� �� ��������� �h��...�� �׷��� �翬�� ©����
	        float stopX = mWidth;
	        float stopY = startY;
	        canvas.drawLine(startX, startY, stopX, stopY, mPaint);
	        
			super.onDraw(canvas);
		}

		boolean mSetWillDraw = false;
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			
			//Log.i("tag", "DrawableBolderLinearLayout:onLayout");
			
			mWidth = right - left;
			mHeight = bottom - top;
			
			if (!mSetWillDraw) {
				mSetWillDraw = true;
				setWillNotDraw (false); //LinearLayout�� ����� �Ѵ�
			}
			
		}		

	}
		
	public final int PSUDO_SECONDARY_MONTHDAY_INDICATOR_TRANSLATE_EXIT_ANIM_COMPLETION = 1;  //mPsudoSecondaryMonthDayIndicatorTranslateAnimator.start(); o
	public final int SELECTED_WEEK_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 2; 		         //mSelectedWeekViewTranslateAnimator.start();
	public final int PSUDO_UPPER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 4;                //mPusdoUpperLisViewTranslateAnimator.start(); o
	public final int PSUDO_UPPER_LISTVIEW_TAIL_TRANSLATE_EXIT_ANIM_COMPLETION = 8;           //mPusdoUpperLisViewTailTranslateAnimator.start();
	public final int PSUDO_LOWER_LISTVIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 16;               //mPsudoLowerLisViewContainer.startAnimation();
	public final int PSUDO_ENTENDEDDAY_VIEW_TRANSLATE_EXIT_ANIM_COMPLETION = 32;              //mPsudoExtendedDayView.startAnimation()	
	public final int CALENDAR_VIEWS_UPPER_ACTIONBAR_SWITCH_FRAGMENT_ANIM_COMPLETION = 64;
		
	private int ENTIRE_ANIMATIONS_COMPLETION = 0;
    
    
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
		
		mFragment.mAdapter.mController.sendEvent(mContext, EventType.GO_TO, mSelectedDayTime, mSelectedDayTime, -1,
                ViewType.DAY,
                CalendarController.EXTRA_GOTO_DATE/* | CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS*/, 
                null, null);
		
	}

	
	
	
}
