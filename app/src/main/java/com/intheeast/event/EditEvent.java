package com.intheeast.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.calendarcommon2.EventRecurrence;
import com.intheeast.acalendar.CalendarEventModel;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.RecurrenceContext;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.etc.LockableScrollView;
import com.intheeast.event.EditEventHelper.EditDoneRunnable;
import com.intheeast.impl.TimerPickerTimeText;
import com.intheeast.settings.SettingsFragment;
import com.intheeast.timepicker.AllDayTimePickerRender;
import com.intheeast.timepicker.TimePickerContainer;
import com.intheeast.timepicker.TimePickerRender;
import com.intheeast.timepicker.TimePickerRenderThread;
import com.intheeast.timepicker.TimerPickerUpdateData;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;



public class EditEvent extends Object {

	private static final String PERIOD_SPACE = ". ";
	
	public static final int EVENT_MAIN_PAGE = 0;
	public static final int EVENT_REPEAT_PAGE = 1;
	//public static final int EVENT_REPEAT_SUB_PAGE = 2;
	public static final int EVENT_REPEATEND_PAGE = 3;
	public static final int EVENT_REMINDER_PAGE = 4;	
	public static final int EVENT_CALENDAR_PAGE = 5;
	public static final int EVENT_ATTENDEE_PAGE = 6;
	public static final int EVENT_AVAILABILITY_PAGE = 7;
	public static final int EVENT_VISIBILITY_PAGE = 8;
		
	public static final float EDITEVENTVIEW_SEPERATOR_HEIGHT_DP = 40;
	public static final float EDITEVENTVIEW_ITEMTEXT_SIZE_SP = 18;
	public static final float EDITEVENTVIEW_SEPERATOR_OUTLINE_HEIGHT_DP = 1;
	
	public static final float TEXTUREVIEW_HEIGHT_DP = 200;
	public static final float TEXTUREVIEW_RATIO = 1.5f;
	public static final int DEFAULT_EVENT_TIMEPERIOD_BY_MINUTE_UNIT = 60;
	
	Context mContext;
	Activity mActivity;
	EditEventFragment mFragment;
	View mMainPageView;
	
	ViewSwitcher mViewSwitcher;
	View mUpperLineViewOfEndTimerTextLayout;	
	View mUnderLineViewOfEndTimerTextLayout;

	MultiAutoCompleteTextView mAttendeeEmailAddress;
	
	TextView mEventRepeatTimesText;
	TextView mEventRepeatEndText;	
	TextView mEventFirstReminderText;
	TextView mEventSecondReminderText;
	TextView mEventCalendarText;
	TextView mEventAttendeeNumbersText;
	TextView mEventAvailabilityText;
	TextView mEventVisibilityText;
	TextView mStartTimerTimeText; /////////////////////////////////////////////////
	
	RelativeLayout mEndTimeLayout;
	RelativeLayout.LayoutParams mLayoutParamsOfUpperLineViewOfEndTimerTextLayout;
	RelativeLayout.LayoutParams mLayoutParamsOfUnderLineViewOfEndTimerTextLayout;
	
	private EventRecurrence mEventRecurrence = new EventRecurrence();    
    RecurrenceContext mRRuleContext = null;    
	
	// end timer time text�� �� ���� ��� �޸� drawing �ȴ�
	// 1.start timer�� [year:month:date] �κ��� ��ġ�ϴ� ��� :  meridieum:hour:minute �� DARK GRAY Color�� drawing �Ѵ�
	// 2.start timer�� [year:month:date] �κ��� ��ġ���� �ʴ� ��� : year:month:date:meridieum:hour:minute�� RED Color�� drawing �Ѵ�
	// 3.invaild[start timer ���� ���� �ð�] ��� : 1�� �Ǵ� 2�� drawing + STRIKE_THRU_TEXT_FLAG???
	TextView mEndTimerTimeText;
	
	//TimePickerSurfaceTextureListener mTimePicekrGlSurfaceListener;
	
	boolean mLaunchedScroll;
	float mDensity;
	float mScaleDensity;
	float mPpi;
	float m_oneCentiMeter;
	
	int mViewWidth;
	int mViewHeight;
	
	//Calendar mStartTimeCalendar;
	// end timer�� �ð��� 
	// 1.start timer�� ����
	// 2.end timer�� ���� 
	// update �ȴ�
	// �׷��� update ������ �ٸ���
	// 1�� ���׿� ���� end timer�� update�� �� �׷��� Ư�� �̺�Ʈ�� ���۽ð��� ���� �ð��� ���� �ð��� update �ϴ� ���̰�
	// 2�� ���׿� ���� end timer�� update�� �̺�Ʈ time period�� update �ϴ� ���̴�
	//Calendar mEndTimeCalendar;	
	
	Calendar mRepeatEndTime;
	RepeatEndTime mRepeatEndTimeObj;

	boolean mStartTimeExpended;
	boolean mEndTimeExpended;
	boolean mGetEndTimerValidTextViewPaintFlags;
	int mEndTimerValidTextViewPaintFlags;
	
	// �̺�Ʈ �Ⱓ �ð���� ���� Update �Ǵ°�???
	// 1.���� ����Ʈ�� 60������ ������
	// 2.end timer�� �ð� �������� 
	// 3.end timer�� invalid ���¿��� valid ���·� ��ȯ�Ǿ��� ��!!!
	//   :�̰��� �̺�Ʈ �Ⱓ �ð��� reset�� �ǹ��Ѵٰ� �� �� �ִ�
	int mEventTimePeriodByMinuteUnit;
	// ���� flag�� end timer�� valid/invalid ���¸� ���� �Ǵ� �ǹ��� ����� ������
	// end timer�� ���¸� ������ �� �ִ�
	// start timer���� �ǹ̸� üũ�Ͽ� end timer�� update ���θ� �Ǵ��Ѵ�
	boolean mEndTimerValidStatus;	
	
	int mEditEventViewSeperatorHeight;
	int mEditEventViewSeperatorOutlineHeight;
	int mEditEventViewItemTextSize;
	
	int mVisibilityValue;
	
	LockableScrollView mMainPageScrollView;
	//LockableScrollView mRepeatEndPageScrollView;
	private ScrollInterpolator mHScrollInterpolator;
	
	int m_requestDay;
	int m_requestDayOfWeek;
	int m_requestMonth;
	int m_requestYear;
		
	int mCurrentViewingPage = EVENT_MAIN_PAGE;
	public ArrayList<CalendarInfo> mCalendarInfoList; 
	//MatrixCursor mCalendarsCursor;
	//Cursor mCalendarsCursor;
	ArrayList<View> mEditOnlyList = new ArrayList<View>();
    //ArrayList<View> mEditViewList = new ArrayList<View>();
    ArrayList<View> mViewOnlyList = new ArrayList<View>();
    
	TextView mTitleTextView;
	TextView mLocationTextView;
	TextView mDescriptionTextView;
	CheckBox mAllDayCheckBox;	
	
	RelativeLayout mBetweenRepeatAndRepeatEndLayout;
	RelativeLayout mBetweenRepeatAndAttendeeSeperatorLayout;
	RelativeLayout mBetweenFristReminderAndSecondReminderLayout;
	RelativeLayout mBetweenCalendarAndAvailablitySeperatorLayout;
	RelativeLayout mBetweenAvailablityAndVisibilitySeperatorLayout;
	RelativeLayout mRepeatEndTimeLayout;
	RelativeLayout mFirstReminderLayout;
	RelativeLayout mSecondReminderLayout;
	RelativeLayout mAttendeeLayout;
	RelativeLayout mAvailabilityLayout;
	RelativeLayout mVisibilityLayout;
	
	RelativeLayout mRepeatPageActionBar;
	//View mRepeatPageActionBar;
	InputMethodManager m_inputMM;
	
	int mDefaultCalendarID = -1;
	int mCalendarID = -1;
	
	TimePickerMetaData mTimePickerMetaData;
	
	/*Handler mInitCalendarInfoOfModelHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {				
			super.handleMessage(msg);
			
			Log.i("tag", "InitCalendarInfoOfModelHandler");
			setSelectedCalendar(-1);
		}			
	};*/	
	
	int mStartTimeTextLayoutWidth = -1;
    int mStartTimeTextLayoutHeight = -1;
	OnLayoutChangeListener mStartTimeTextLayoutChangeListener = new OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {		
			
			if ((mStartTimeTextLayoutWidth == -1) && (mStartTimeTextLayoutHeight == -1) ) {
				Log.i("tag", "mStartTimeTextLayoutChangeListener");
				
				mStartTimeTextLayoutWidth = right - left;
				mStartTimeTextLayoutHeight = bottom - top;				
			}
		}		
	};
	
	int mMainPageViewWidth = -1;
    int mMainPageViewHeight = -1;
    TranslateAnimation leftInAnimationForMainbackText;
    OnLayoutChangeListener mMainPageViewOnLayoutChangeListener = new OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {	
			
			if (mMainPageViewHeight == -1) {    	            
				mMainPageViewHeight = bottom - top;   		
	    										
			}
		}		
	};
	
	
	Resources mResources;
	
	public EditEvent(Context context, Activity activity, EditEventFragment fragment, View mainView, 
			              EditDoneRunnable done, int viewWidth) {		
		mContext = context;		
		mActivity = activity;
		mFragment = fragment;	
		
		mMainPageView = mainView;			
		mMainPageScrollView = (LockableScrollView) mMainPageView.findViewById(R.id.editevent_mainpage_scroll_view);
		mResources = mActivity.getResources();//fragment.getResources();
		
		mViewWidth = mMainPageViewWidth = viewWidth;
		float xOffSet = mViewWidth / 2;
		mViewSwitchingDelta = mViewWidth - xOffSet;
		mAnimationDistance = mViewWidth - xOffSet;
		leftInAnimationForMainbackText = new TranslateAnimation(
	            Animation.ABSOLUTE, mViewWidth / 2, //fromXValue 
	            Animation.RELATIVE_TO_PARENT, 0,   //toXValue
	            Animation.ABSOLUTE, 0.0f,
	            Animation.ABSOLUTE, 0.0f); 			
	}
	
	public void setViewSwitcher(ViewSwitcher viewSwitcher) {
		mViewSwitcher = viewSwitcher;
	}
	
	@SuppressWarnings("deprecation")
	public void initMainPageLayout(Calendar startTime, Calendar endTime) {
		mHScrollInterpolator = new ScrollInterpolator();
		
		mTitleTextView = (TextView) mMainPageView.findViewById(R.id.title);
		mLocationTextView = (TextView) mMainPageView.findViewById(R.id.location);
		mDescriptionTextView = (TextView) mMainPageView.findViewById(R.id.memo);		
		//mEditViewList.add(mTitleTextView);
		mAllDayCheckBox = (CheckBox) mMainPageView.findViewById(R.id.is_all_day);		
		mAllDayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAllDayViewsVisibility(isChecked);
            }
        });
		
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		mStartTimeExpended = false;
		mEndTimeExpended = false;		
		
		WindowManager wm = (WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE);
		Display current_display = wm.getDefaultDisplay();
		DisplayMetrics display_Metrics = new DisplayMetrics();
		current_display.getMetrics(display_Metrics);		
		mPpi = display_Metrics.densityDpi;
		mDensity = display_Metrics.density;
		mScaleDensity = display_Metrics.scaledDensity;
		m_oneCentiMeter = display_Metrics.ydpi * 0.393701f;
		
		mTzId = Utils.getTimeZone(mActivity, null);
		mTimeZone = TimeZone.getTimeZone(mTzId);
		mStartTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimezone);
		mStartTime.setTimeInMillis(startTime.getTimeInMillis());
		
		mEndTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimezone);
		if (endTime != null) {
			mEndTime.setTimeInMillis(endTime.getTimeInMillis());
		}
		else {
			mEndTime.setTimeInMillis(mStartTime.getTimeInMillis());
			mEventTimePeriodByMinuteUnit = DEFAULT_EVENT_TIMEPERIOD_BY_MINUTE_UNIT; // ����Ʈ�� start time 1�ð� ���ķ� ����
	        long endTimeMillis = mEndTime.getTimeInMillis();
	        long willBeAddedMillis = mEventTimePeriodByMinuteUnit * 60 * 1000;
	        mEndTime.setTimeInMillis(endTimeMillis + willBeAddedMillis);
		}       
        
        mEndTimerValidStatus = true;
        mGetEndTimerValidTextViewPaintFlags = false;        
		
		Calendar eventTime = GregorianCalendar.getInstance(startTime.getTimeZone());//startTime;		
		m_requestDay = eventTime.get(Calendar.DAY_OF_MONTH);//monthDay;
		m_requestDayOfWeek = eventTime.get(Calendar.DAY_OF_WEEK); 
		++m_requestDayOfWeek; // Calendar�� DAY_OF_WEEK�� �� ������ [1~7] �̰� 
		                      // Time.weekDay�� �� ������ [0~6] �̹Ƿ� ������ �ؾ� �Ѵ�
		m_requestMonth = eventTime.get(Calendar.MONTH);
		m_requestYear = eventTime.get(Calendar.YEAR);		
		
		mRepeatEndTime = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimezone);
		mRepeatEndTime.setTimeInMillis(mEndTime.getTimeInMillis());
		mRepeatEndTimeObj = new RepeatEndTime(mRepeatEndTime.get(Calendar.YEAR),
										   mRepeatEndTime.get(Calendar.MONTH),
										   mRepeatEndTime.get(Calendar.DAY_OF_MONTH),										   
										   true);
		mLaunchedScroll = false;
		
		mEditEventViewSeperatorHeight = (int)(EDITEVENTVIEW_SEPERATOR_HEIGHT_DP * mDensity);
		mEditEventViewSeperatorOutlineHeight = (int)(EDITEVENTVIEW_SEPERATOR_OUTLINE_HEIGHT_DP * mDensity);
		mEditEventViewItemTextSize = (int)(EDITEVENTVIEW_ITEMTEXT_SIZE_SP * mScaleDensity);
		
		mTimePickerMetaData = new TimePickerMetaData(mActivity, mMainPageScrollView);
		
    	///////////////////////////////////////////////////////////////////////////
    	///////////////////////////////////////////////////////////////////////////
		
        LinearLayout repeatTimesLayout =(LinearLayout)mMainPageView.findViewById(R.id.event_repeat_layout);
        RelativeLayout repeatTimesCaseLayout = (RelativeLayout)repeatTimesLayout.findViewById(R.id.event_repeat_time_layout);
		mEventRepeatTimesText = (TextView)repeatTimesLayout.findViewById(R.id.repeat_item_value_text);		
		repeatTimesCaseLayout.setOnClickListener(mItemOnClickListener);

		mBetweenRepeatAndRepeatEndLayout = (RelativeLayout)repeatTimesLayout.findViewById(R.id.layout_between_repeat_and_repeat_end_layout);
		mBetweenRepeatAndRepeatEndLayout.setVisibility(View.GONE);		
		mRepeatEndTimeLayout = (RelativeLayout)repeatTimesLayout.findViewById(R.id.event_repeat_end_time_layout);
		mEventRepeatEndText = (TextView)mRepeatEndTimeLayout.findViewById(R.id.repeat_end_item_value_text);
		mRepeatEndTimeLayout.setOnClickListener(mItemOnClickListener);
		mRepeatEndTimeLayout.setVisibility(View.GONE);
		
		mBetweenRepeatAndAttendeeSeperatorLayout = (RelativeLayout)mMainPageView.findViewById(R.id.event_attendee_seperator_layout);
		mBetweenRepeatAndAttendeeSeperatorLayout.setVisibility(View.GONE);		
		mAttendeeLayout = (RelativeLayout)mMainPageView.findViewById(R.id.event_attendee_layout);
		mEventAttendeeNumbersText = (TextView)mAttendeeLayout.findViewById(R.id.attendee_item_value_text);
		mAttendeeLayout.setOnClickListener(mItemOnClickListener);
		mAttendeeLayout.setVisibility(View.GONE);		
		
		LinearLayout alarmLayout =(LinearLayout)mMainPageView.findViewById(R.id.event_reminder_layout);
		mFirstReminderLayout = (RelativeLayout)alarmLayout.findViewById(R.id.event_reminder_first_layout);
		mEventFirstReminderText = (TextView)mFirstReminderLayout.findViewById(R.id.reminder_first_item_value_text);
		mFirstReminderLayout.setOnClickListener(mItemOnClickListener);

		mBetweenFristReminderAndSecondReminderLayout = (RelativeLayout)alarmLayout.findViewById(R.id.layout_between_reminder_first_and_second_layout);
		mBetweenFristReminderAndSecondReminderLayout.setVisibility(View.GONE);		
		mSecondReminderLayout = (RelativeLayout)alarmLayout.findViewById(R.id.event_reminder_second_layout);
		mEventSecondReminderText = (TextView)mSecondReminderLayout.findViewById(R.id.reminder_second_item_value_text);
		mSecondReminderLayout.setOnClickListener(mItemOnClickListener);
		mSecondReminderLayout.setVisibility(View.GONE);
		
		mCalendarLayout =(RelativeLayout)mMainPageView.findViewById(R.id.event_calendar_layout);		
		mEventCalendarText = (TextView)mCalendarLayout.findViewById(R.id.calendar_item_value_text);		
		mCalendarLayout.setOnClickListener(mItemOnClickListener);
			
		mBetweenCalendarAndAvailablitySeperatorLayout = (RelativeLayout)mMainPageView.findViewById(R.id.event_availability_seperator_layout);
		mBetweenCalendarAndAvailablitySeperatorLayout.setVisibility(View.GONE);		
		mAvailabilityLayout = (RelativeLayout)mMainPageView.findViewById(R.id.event_availability_layout);
		mEventAvailabilityText = (TextView)mAvailabilityLayout.findViewById(R.id.availability_item_value_text);
		mAvailabilityLayout.setOnClickListener(mItemOnClickListener);
		mAvailabilityLayout.setVisibility(View.GONE);		
		
		mBetweenAvailablityAndVisibilitySeperatorLayout = (RelativeLayout)mMainPageView.findViewById(R.id.event_visibility_seperator_layout);
		mBetweenAvailablityAndVisibilitySeperatorLayout.setVisibility(View.GONE);		
		mVisibilityLayout = (RelativeLayout)mMainPageView.findViewById(R.id.event_visibility_layout);
		mEventVisibilityText = (TextView)mVisibilityLayout.findViewById(R.id.visibility_item_value_text);
		mVisibilityLayout.setOnClickListener(mItemOnClickListener);
		mVisibilityLayout.setVisibility(View.GONE);			
		
		
		RelativeLayout startTimeLayout =(RelativeLayout)mMainPageView.findViewById(R.id.event_starttime_layout);
		startTimeLayout.setOnClickListener(mStartTimerTextLayoutClickListener);	
		startTimeLayout.addOnLayoutChangeListener(mStartTimeTextLayoutChangeListener);
		
		mStartTimerTimeText = (TextView)startTimeLayout.findViewById(R.id.starttimer_time);		
		mStartTimerTimeText.setTextColor(Color.RED);		
		
		TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTime, mAllDay);
		startTimeText.makeTimeText();
		mStartTimerTimeText.setText(startTimeText.getTimeText());		
		
		mEndTimeLayout =(RelativeLayout)mMainPageView.findViewById(R.id.event_endtime_layout);
		mEndTimeLayout.setOnClickListener(mEndTimerTextLayoutClickListener);
		mEndTimerTimeText = (TextView)mEndTimeLayout.findViewById(R.id.endtimer_time);	
				
		if (!mGetEndTimerValidTextViewPaintFlags) { // activity�� ���� ������ �� �Ʒ� �ڵ带 �����Ѵ�
			mEndTimerValidTextViewPaintFlags = mEndTimerTimeText.getPaintFlags();
			// ������ color�̴� : color�� flag�� ������ ���� �ʴ´�
			mGetEndTimerValidTextViewPaintFlags = true;
		}
		
		updateEventTimePeriodByMinuteUnit();
		setEndTimerTimeText();		
			
		createTimePicker();
		createAllDayTimePicker();
				
		//mEndTimeLayout.setOnClickListener(mEndTimerTextLayoutClickListener);
				
		makeUpperLineOfEndTimerTextLayout();
		makeUnderLineOfEndTimerTextLayout();
		
		m_inputMM = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE); 
		
		mCurrentViewingPage = EVENT_MAIN_PAGE;
		
		mMainPageView.addOnLayoutChangeListener(mMainPageViewOnLayoutChangeListener);
	}
	
	EditEventRepeatSettingSwitchPageView mRepeatSettingSwitchPageView;
	//EditEventRepeatSettingSubView mRepeatPageView;
    EditEventRepeatEndSettingSubView mRepeatEndPageView;
    EditEventReminderSettingSubView mReminderPageView;
    EditEventCalendarSettingSubView mCalendarPageView;
    EditEventAvailabilitySubView mAvailabilityPageView;
    EditEventVisibilitySettingSubView mVisibilityPageView;
    EditEventAttendeeSettingSubView mAttendeePageView;
    public void setSubPages() {
    	mRepeatSettingSwitchPageView = mFragment.mRepeatSettingSwitchPageView; //mRepeatPageView = mFragment.mRepeatPageView;
    	
        mRepeatEndPageView = mFragment.mRepeatEndPageView;
        mReminderPageView = mFragment.mReminderPageView;
        mCalendarPageView = mFragment.mCalendarPageView;
        mAvailabilityPageView = mFragment.mAvailabilityPageView;
        mVisibilityPageView = mFragment.mVisibilityPageView;
        mAttendeePageView = mFragment.mAttendeePageView;
    	
    }
	/**
     * Configures the Calendars spinner.  This is only done for new events, because only new
     * events allow you to select a calendar while editing an event.
     * <p>
     * We tuck a reference to a Cursor with calendar database data into the spinner, so that
     * we can easily extract calendar-specific values when the value changes (the spinner's
     * onItemSelected callback is configured).
     */
	// �Ʒ� ��Ȳ,,, ��ũ ������ Ķ������ ���� ��츦 ����ؾ� �Ѵ�
	// This is called if the user cancels the "No calendars" dialog.
    // The "No calendars" dialog is shown if there are no syncable calendars.
	private boolean mSaveAfterQueryComplete = false;
	RelativeLayout mCalendarLayout;
	public void makeCalendarInfoList(Cursor cursor, boolean userVisible) {
		//mCalendarsCursor = cursor;
		if (cursor == null || cursor.getCount() == 0) {
			if (!userVisible) {
                return;
            }			
		}
		
		mCalendarIDColumnIndex = cursor.getColumnIndexOrThrow(Calendars._ID);
		mCalendarDisplayNameColumnIndex = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
        mCalendarAccountNameColumnIndex = cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
        mCalendarOwnerAccountColumnIndex = cursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
        mCalendarAccountTypeColumnIndex = cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);        
        mCalendarColorColumnIndex = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
                
        mCalendarInfoList = new ArrayList<CalendarInfo>();        
        
        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {	
        	
        	int calendarId = cursor.getInt(mCalendarIDColumnIndex);
    		String displayName = cursor.getString(mCalendarDisplayNameColumnIndex);	
    		String ownerAccount = cursor.getString(mCalendarOwnerAccountColumnIndex);
    		String accountName = cursor.getString(mCalendarAccountNameColumnIndex);        
            String accountType = cursor.getString(mCalendarAccountTypeColumnIndex);
            int color = cursor.getInt(mCalendarColorColumnIndex);
            
            CalendarInfo object = new CalendarInfo(calendarId, displayName, ownerAccount, accountName, accountType, color);
            
        	mCalendarInfoList.add(object);        
        }          
	}
	
	public void makeSubCalendarPageOfCalendarPage() {
		mCalendarPageView.makeCalendarPageSubCalendarLayout();
	}
	
	private int findSelectedCalendarPosition(Cursor calendarsCursor, long calendarId) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }
        int calendarIdColumn = calendarsCursor.getColumnIndexOrThrow(Calendars._ID);
        int position = 0;
        calendarsCursor.moveToPosition(-1);
        while (calendarsCursor.moveToNext()) {
            if (calendarsCursor.getLong(calendarIdColumn) == calendarId) {
                return position;
            }
            position++;
        }
        return 0;
    }
	
	public TranslateAnimation makeSlideAnimation(float speedScale, float velocity, int fromXType, float fromXDelta, int toXType, float toXDelta) {    	
    	float delta = mViewSwitchingDelta / speedScale;
    	float width = mAnimationDistance / speedScale;	      	
    	
    	TranslateAnimation translateAnimation = new TranslateAnimation(
    			fromXType, fromXDelta, //fromXValue 
    			toXType, toXDelta,   //toXValue
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        long duration = calculateDuration(delta, width, velocity);
        translateAnimation.setDuration(duration);
        translateAnimation.setInterpolator(mHScrollInterpolator);
        //translateAnimation.cancel();
        return translateAnimation;        
	}
	
	public static final int ALPHA_OPAQUE_TO_TRANSPARENT = 1;
	public static final int ALPHA_TRANSPARENT_TO_OPAQUE = 2;
	public static final int ANIMATION_INTERPOLATORTYPE_ACCELERATE = 1;
	public static final int ANIMATION_INTERPOLATORTYPE_DECELERATE = 2;
	@SuppressLint("SuspiciousIndentation")
	public AlphaAnimation makeAlphaAnimation(float speedScale, float velocity, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {
    	float delta = mViewSwitchingDelta / speedScale;
    	float width = mAnimationDistance / speedScale;    	
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
	
	public void setMainPageActionBar() {		
		
		float enterAnimationDistance = mMainPageView.getWidth() * 0.4f;
    	float exitAnimationDistance = mMainPageView.getWidth(); // 
    	
    	float velocity = SWITCH_SUB_PAGE_EXIT_VELOCITY ;
    	long exitAnimationDuration = ITEAnimationUtils.calculateDuration(exitAnimationDistance, mMainPageView.getWidth(), MINIMUM_SNAP_VELOCITY, velocity); 	    	
    	long enterAnimationDuration = exitAnimationDuration;
    	
		mFragment.mActionBar.setMainViewActionBar(enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance);
		    
	}
				
	OnClickListener mItemOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int viewId = v.getId();
			
			switch(viewId) {
			case R.id.event_repeat_time_layout:
				switchSubView(EVENT_REPEAT_PAGE);	
				mCurrentViewingPage = EVENT_REPEAT_PAGE;
				break;
			case R.id.event_repeat_end_time_layout:
				switchSubView(EVENT_REPEATEND_PAGE);
				mCurrentViewingPage = EVENT_REPEATEND_PAGE;
				break;
			case R.id.event_attendee_layout:
				switchSubView(EVENT_ATTENDEE_PAGE);	
				mCurrentViewingPage = EVENT_ATTENDEE_PAGE;
				break;
			case R.id.event_reminder_first_layout:
				if (!mReminderPageView.mSelectedFirstReminderItemLayoutOfMainPage) {  
					
					// �ι�° �����δ� ���� ���������� ���õ� reminder selection icon�� remove �ؾ� �Ѵ�
					if (mReminderPageView.mReminderEntries.size() > 1) {
						int secondReminderMinutes = mReminderPageView.mReminderEntries.get(1).getMinutes(); 
						boolean unsupportedSecondReminderMinutes = !mReminderMinuteValues.contains(secondReminderMinutes);
	            		mReminderPageView.detachReminderPageItemSelectionIcon(secondReminderMinutes, unsupportedSecondReminderMinutes);
					}
					else if (mReminderPageView.mReminderEntries.size() == 1){						
						int secondReminderMinutes = EditEventReminderSettingSubView.EVENT_NO_REMINDER_MINUTES; 
						boolean unsupportedSecondReminderMinutes = !mReminderMinuteValues.contains(secondReminderMinutes);
	            		mReminderPageView.detachReminderPageItemSelectionIcon(secondReminderMinutes, unsupportedSecondReminderMinutes);
					}	
								
					// ù��° �����δ� ���� �������� �ش� reminder minutes�� reminder selection icon�� add �Ѵ�
					int firstReminderMinutes = EditEventReminderSettingSubView.EVENT_NO_REMINDER_MINUTES; 
            		boolean unsupportedReminderMinutes = false;            		
            		if (mReminderPageView.mReminderEntries.size() > 0) {
            			firstReminderMinutes = mReminderPageView.mReminderEntries.get(0).getMinutes();
	            		if (mReminderMinuteValues.contains(firstReminderMinutes)) {
	            			mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
	            		}
	            		else {
	            			unsupportedReminderMinutes = true;
	            			mReminderPageView.mUnsupportedReminderLayout.setTag(firstReminderMinutes);
	            			TextView tv = (TextView) mReminderPageView.mUnsupportedReminderLayout.findViewById(R.id.unsupported_reminder_text);
	                    	tv.setText(String.valueOf(firstReminderMinutes));
	                    	mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.VISIBLE);
	            		}
            		}
            		
            		mReminderPageView.attachReminderPageItemSelectionIcon(firstReminderMinutes, unsupportedReminderMinutes);
            		mReminderPageView.mSelectedFirstReminderItemLayoutOfMainPage = true;            		
            	}
				else {					
					if (mReminderPageView.mReminderEntries.size() > 0) {
						int firstReminderMinutes = mReminderPageView.mReminderEntries.get(0).getMinutes();
	            		if (mReminderMinuteValues.contains(firstReminderMinutes)) {
	            			if (mReminderPageView.mUnsupportedReminderLayout.getVisibility() == View.VISIBLE)
	            				mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
	            		}
	            		else {	            			
	            			mReminderPageView.mUnsupportedReminderLayout.setTag(firstReminderMinutes);
	            			TextView tv = (TextView) mReminderPageView.mUnsupportedReminderLayout.findViewById(R.id.unsupported_reminder_text);
	                    	tv.setText(String.valueOf(firstReminderMinutes));
	                    	if (mReminderPageView.mUnsupportedReminderLayout.getVisibility() == View.GONE)
	                    		mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.VISIBLE);
	            		}
					}
					else {
						if (mReminderPageView.mUnsupportedReminderLayout.getVisibility() != View.GONE) {
							mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
						}
					}					 
				}
				
				switchSubView(EVENT_REMINDER_PAGE);
				mCurrentViewingPage = EVENT_REMINDER_PAGE;
				break;
			case R.id.event_reminder_second_layout:
				int secondReminderMinutes = EditEventReminderSettingSubView.EVENT_NO_REMINDER_MINUTES;
				boolean unsupportedReminderMinutes = false;
				if (mReminderPageView.mSelectedFirstReminderItemLayoutOfMainPage) {    
					// �� ���� mReminderEntries�� size�� 0���� ū���� üũ�� �ʿ䰡 ����
					// : mReminderEntries�� size�� 0���� ũ�� ������ event_reminder_second_layout�� ������ �� �ֱ� �����̴�
					int firstReminderMinutes = mReminderPageView.mReminderEntries.get(0).getMinutes(); 
					unsupportedReminderMinutes = !mReminderMinuteValues.contains(firstReminderMinutes);
            		mReminderPageView.detachReminderPageItemSelectionIcon(firstReminderMinutes, unsupportedReminderMinutes);
            		
            		if (mReminderPageView.mReminderEntries.size() > 1) {
	            		secondReminderMinutes = mReminderPageView.mReminderEntries.get(1).getMinutes();
	            		if (mReminderMinuteValues.contains(secondReminderMinutes)) {                    	
	                    	mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
	            		}          
	            		else {
	            			unsupportedReminderMinutes = true;
	            			mReminderPageView.mUnsupportedReminderLayout.setTag(secondReminderMinutes);
	            			TextView tv = (TextView) mReminderPageView.mUnsupportedReminderLayout.findViewById(R.id.unsupported_reminder_text);
	                    	tv.setText(String.valueOf(secondReminderMinutes));
	                    	mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.VISIBLE);
	            		}  
            		}
            		else {            
            			mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
            		}
            		
            		mReminderPageView.attachReminderPageItemSelectionIcon(secondReminderMinutes, unsupportedReminderMinutes);
            		mReminderPageView.mSelectedFirstReminderItemLayoutOfMainPage = false;
            	}
				else {				
					if (mReminderPageView.mReminderEntries.size() > 1) {
						secondReminderMinutes = mReminderPageView.mReminderEntries.get(1).getMinutes();
	            		if (mReminderMinuteValues.contains(secondReminderMinutes)) {                    	
	                    	mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
	            		}          
	            		else {            		
	            			unsupportedReminderMinutes = true;
	            			mReminderPageView.mUnsupportedReminderLayout.setTag(secondReminderMinutes);
	            			TextView tv = (TextView) mReminderPageView.mUnsupportedReminderLayout.findViewById(R.id.unsupported_reminder_text);
	                    	tv.setText(String.valueOf(secondReminderMinutes));
	                    	mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.VISIBLE);
	            		}    
					}
					else if (mReminderPageView.mReminderEntries.size() == 1){
						mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.GONE);
					}
					else {
						// �̷� ��Ȳ�� �߻��� �� �ִ°�???
						// :�߻��� �� ���� mReminderEntries�� size�� 0�̶�� ���� 
						//  R.id.event_reminder_second_layout�� GONE ���°� �Ǳ� ������ R.id.event_reminder_second_layout[mSecondReminderLayout]�� onClick �̺�Ʈ�� �߻��� �� ����
					}					   		
				}				
				
				switchSubView(EVENT_REMINDER_PAGE);	
				mCurrentViewingPage = EVENT_REMINDER_PAGE;
				break;
			case R.id.event_calendar_layout:
				switchSubView(EVENT_CALENDAR_PAGE);	
				mCurrentViewingPage = EVENT_CALENDAR_PAGE;
				break;
			case R.id.event_availability_layout:
				switchSubView(EVENT_AVAILABILITY_PAGE);	
				mCurrentViewingPage = EVENT_AVAILABILITY_PAGE;
				break;
			case R.id.event_visibility_layout:
				switchSubView(EVENT_VISIBILITY_PAGE);	
				mCurrentViewingPage = EVENT_VISIBILITY_PAGE;
				break;			
			}
			
			setSubPageActionBar(mCurrentViewingPage);
			
			mViewSwitcher.showNext();			
		}		
	};	
		
	public void setSubPageActionBar(int pageID) {	
		
		String subPageTitle = getSubPageActionBarTitle(pageID);
				
		float enterAnimationDistance = mMainPageView.getWidth(); 
		float exitAnimationDistance = mMainPageView.getWidth() * 0.4f; 
		
		float velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mMainPageView.getWidth(), MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
    	
		mFragment.mActionBar.setSubPageActionBar(subPageTitle,
				enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance);		    
	}	

	public String getSubPageActionBarTitle(int pageID) {
		String subPageTitle = null;		
		switch(pageID) {    		
		case EVENT_REPEAT_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_repeatpage_actionbar_title);
			break;
		/*case EVENT_REPEAT_SUB_PAGE:
			subPageTitle = mResources.getString(R.string.editevent_repeat_sub_page_actionbar_title);
			break;*/
		case EVENT_REPEATEND_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_repeatendpage_actionbar_title);
			break;
		case EVENT_REMINDER_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_reminderpage_actionbar_title);
			break;    		
		case EVENT_CALENDAR_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_calendarpage_actionbar_title);
			break;
		case EVENT_ATTENDEE_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_attendeepage_actionbar_title);
			break;
		case EVENT_AVAILABILITY_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_availabilitypage_actionbar_title);
			break;
		case EVENT_VISIBILITY_PAGE:			
			subPageTitle = mResources.getString(R.string.editevent_visibilitypage_actionbar_title);
			break;
		default:
			return null;
		}	
		
		return subPageTitle;
	}
	
	
	public void updateStartTimer(TimerPickerUpdateData obj) {		
		int field = obj.getUpdateField();
		
		switch(field) {
		case TimePickerRender.DATE_FIELD_TEXTURE:			
			//mStartTimeCalendar.set(obj.mYear, obj.mMonth, obj.mDate, mStartTimeCalendar.get(Calendar.HOUR_OF_DAY_OF_DAY), mStartTimeCalendar.get(Calendar.MINUTE));
			//mStartTime.set(0, mStartTime.minute, mStartTime.hour, obj.mDate, obj.mMonth, obj.mYear);	
			mStartTime.set(obj.mYear, obj.mMonth, obj.mDate, mStartTime.get(Calendar.HOUR_OF_DAY), mStartTime.get(Calendar.MINUTE), 0);
			break;
		case TimePickerRender.MERIDIEM_FIELD_TEXTURE:		
			Calendar startTimeCalendar = (Calendar)Calendar.getInstance(mStartTime.getTimeZone());			
			startTimeCalendar.setTimeInMillis(mStartTime.getTimeInMillis());
			startTimeCalendar.set(Calendar.AM_PM, obj.mAMPM);
			mStartTime.setTimeInMillis(startTimeCalendar.getTimeInMillis());
			break;
		case TimePickerRender.HOUR_FIELD_TEXTURE:
			//mStartTimeCalendar.set(Calendar.HOUR_OF_DAY_OF_DAY, obj.mHour);	
			//mStartTime.set(0, mStartTime.get(Calendar.MINUTE), obj.mHour, mStartTime.get(Calendar.DAY_OF_MONTH), mStartTime.get(Calendar.MONTH), mStartTime.get(Calendar.YEAR));
			mStartTime.set(mStartTime.get(Calendar.YEAR), mStartTime.get(Calendar.MONTH), mStartTime.get(Calendar.DAY_OF_MONTH), obj.mHour, mStartTime.get(Calendar.MINUTE), 0);
			break;
		case TimePickerRender.MINUTE_FIELD_TEXTURE:
			//mStartTimeCalendar.set(Calendar.MINUTE, obj.mMinute);
			//mStartTime.set(0, obj.mMinute, mStartTime.get(Calendar.HOUR_OF_DAY_OF_DAY), mStartTime.get(Calendar.DAY_OF_MONTH), mStartTime.get(Calendar.MONTH), mStartTime.get(Calendar.YEAR));
			mStartTime.set(mStartTime.get(Calendar.YEAR), mStartTime.get(Calendar.MONTH), mStartTime.get(Calendar.DAY_OF_MONTH), mStartTime.get(Calendar.HOUR_OF_DAY), obj.mMinute, 0);
			break;
		default:
			return;
		}
		
		// start timer�� �׻� update �Ѵ�
		//TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTimeCalendar, mAllDay);
		TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTime, mAllDay);
		startTimeText.makeTimeText();
		mStartTimerTimeText.setText(startTimeText.getTimeText());				
		
		// end timer�� update�� ��Ȳ�� ���� �޸� �����Ѵ�
		// end timer�� invalid ���´� end timer�� start timer ���� ���� �ð����� ������ ����̴�
		// 1.end timer�� ���°� valid�� ����
		//   : �� ���¿����� end timer�� start timer�� update���� ���� invalid ���·� ���̵� �� ����
		//     �ֳ��ϸ� valid ���¿����� �׻� (start timer�� �ð��� + event timer period)������ update �Ǳ� �����̴�		
		if (mEndTimerValidStatus) { // mEndTimerValidStatus�� checkEndTimerValidStatus���� �����ȴ�
			//mEndTimeCalendar = (Calendar)mStartTimeCalendar.clone();				
			//mEndTimeCalendar.add(Calendar.MINUTE, mEventTimePeriodByMinuteUnit);		
			//setEndTimerTimeText();	
			
			Calendar endTimeCalendar = (Calendar)Calendar.getInstance(mStartTime.getTimeZone());			
			endTimeCalendar.setTimeInMillis(mStartTime.getTimeInMillis());
			endTimeCalendar.add(Calendar.MINUTE, mEventTimePeriodByMinuteUnit);
			mEndTime.setTimeInMillis(endTimeCalendar.getTimeInMillis());
			setEndTimerTimeText();
			
		}
		// 2.end timer ���°� invalid�� ���� : �̴� user�� ���ο� event ����/���� �ð� ������ ���� ���������� �� �κ����� �ǹ��Ѵ�
		//  : start timer picker�� Ȱ��ȭ �Ǿ� �ְ� end timer�� invalid�� ��Ȳ������
		//    end timer�� start timer�� �ð� �������� valid/invaid ���·� ����(�ݺ� ���̵�)�� �� �ִ�.
		//    �׷��� end timer�� invalid �����̹Ƿ�,,,
		//    end timer�� �ð��� update ���� �ʴ´�!!!
		//    ����,,,valid/invalid ���¸� drawing �ȴ�
		else {      
			// ��, invalid ���¿��� end timer�� valid/invalid ���¸� ��Ÿ���� drawing �� ����ȴ�
			// invalid ���°� �Ǿ��ٴ� ���� user�� ���ο� event ���� �ð��� �����Ͽ��ٴ� �ǹ��� ���� �ִ�.
			// valid ���°� �Ǿ��ٴ� ���� �� ��Ȳ�� �õ� ���Ӽ��� �ִٴ� ���� �ǹ��ϴ� ���̴�
			//int compareToEndTimerAfterUpdate = mEndTimeCalendar.compareTo(mStartTimeCalendar);
			
			// return 
			// negative number if a is less than b, 
			// positive number if a is greater than b, 
			// or 0 if they are equal.
			int compareToEndTimerAfterUpdate = ETime.compare(mEndTime, mStartTime);
			if (compareToEndTimerAfterUpdate == 1) { // 1. valid ���·� ���̵Ǿ�� ��
				// end timer�� valid ���·� �ؽ�Ʈ�� ��ȯ�ؾ� �Ѵ�				
				setEndTimerTimeText();
				mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags);
			}
			else if (compareToEndTimerAfterUpdate == -1) { // 2. invalid ���·� ���̵Ǿ�� ��
				setEndTimerTimeText();
				mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG );
			}
			else {
				// ������ ����???
			}
		}			
	}
		
	public void updateAllDayStartTimer(TimerPickerUpdateData obj) {
		int field = obj.getUpdateField();
		
		switch(field) {
		case AllDayTimePickerRender.YEAR_FIELD_TEXTURE:
			//mStartTimeCalendar.set(Calendar.YEAR, obj.mYear);	
			//mStartTimeCalendar.set(Calendar.DAY_OF_MONTH, obj.mDate);			
			//mStartTime.set(0, mStartTime.minute, mStartTime.hour, obj.mDate, mStartTime.month, obj.mYear);
			mStartTime.set(obj.mYear, mStartTime.get(Calendar.MONTH), obj.mDate, mStartTime.get(Calendar.HOUR_OF_DAY), mStartTime.get(Calendar.MINUTE), 0);
			break;
		case AllDayTimePickerRender.MONTH_FIELD_TEXTURE:			
			//mStartTimeCalendar.set(obj.mYear, obj.mMonth, obj.mDate);
			//mStartTime.set(0, mStartTime.minute, mStartTime.hour, obj.mDate, obj.mMonth, obj.mYear);
			mStartTime.set(obj.mYear, obj.mMonth, obj.mDate, mStartTime.get(Calendar.HOUR_OF_DAY), mStartTime.get(Calendar.MINUTE), 0);
			break;
		case AllDayTimePickerRender.DAYOFMONTH_FIELD_TEXTURE:
			//mStartTimeCalendar.set(Calendar.DAY_OF_MONTH, obj.mDate);
			//mStartTime.set(0, mStartTime.minute, mStartTime.hour, obj.mDate, mStartTime.month, mStartTime.year);
			mStartTime.set(mStartTime.get(Calendar.YEAR), mStartTime.get(Calendar.MONTH), obj.mDate, mStartTime.get(Calendar.HOUR_OF_DAY), mStartTime.get(Calendar.MINUTE), 0);
			break;
		
		default:
			return;
		}
		
		// start timer�� �׻� update �Ѵ�
		TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTime, mAllDay);
		startTimeText.makeTimeText();
		mStartTimerTimeText.setText(startTimeText.getTimeText());				
		
		// end timer�� update�� ��Ȳ�� ���� �޸� �����Ѵ�
		// end timer�� invalid ���´� end timer�� start timer ���� ���� �ð����� ������ ����̴�
		// 1.end timer�� ���°� valid�� ����
		//   : �� ���¿����� end timer�� start timer�� update���� ���� invalid ���·� ���̵� �� ����
		//     �ֳ��ϸ� valid ���¿����� �׻� (start timer�� �ð��� + event timer period)������ update �Ǳ� �����̴�		
		if (mEndTimerValidStatus) { // mEndTimerValidStatus�� checkEndTimerValidStatus���� �����ȴ�
			//mEndTimeCalendar = (Calendar)mStartTimeCalendar.clone();		
			// ���� mini�� ��� time period�� ��� �����ؾ� �ϴ°�???------------------------------------------------------------
			//mEndTimeCalendar.add(Calendar.MINUTE, mEventTimePeriodByMinuteUnit); 			
			//setEndTimerTimeText();	
			
			Calendar endTimeCalendar = (Calendar)Calendar.getInstance(mStartTime.getTimeZone());
			
			endTimeCalendar.setTimeInMillis(mStartTime.getTimeInMillis());
			endTimeCalendar.add(Calendar.MINUTE, mEventTimePeriodByMinuteUnit);
			mEndTime.setTimeInMillis(endTimeCalendar.getTimeInMillis());
			setEndTimerTimeText();
		}
		// 2.end timer ���°� invalid�� ���� : �̴� user�� ���ο� event ����/���� �ð� ������ ���� ���������� �� �κ����� �ǹ��Ѵ�
		//  : start timer picker�� Ȱ��ȭ �Ǿ� �ְ� end timer�� invalid�� ��Ȳ������
		//    end timer�� start timer�� �ð� �������� valid/invaid ���·� ����(�ݺ� ���̵�)�� �� �ִ�.
		//    �׷��� end timer�� invalid �����̹Ƿ�,,,
		//    end timer�� �ð��� update ���� �ʴ´�!!!
		//    ����,,,valid/invalid ���¸� drawing �ȴ�
		else {      
			// ��, invalid ���¿��� end timer�� valid/invalid ���¸� ��Ÿ���� drawing �� ����ȴ�
			// invalid ���°� �Ǿ��ٴ� ���� user�� ���ο� event ���� �ð��� �����Ͽ��ٴ� �ǹ��� ���� �ִ�.
			// valid ���°� �Ǿ��ٴ� ���� �� ��Ȳ�� �õ� ���Ӽ��� �ִٴ� ���� �ǹ��ϴ� ���̴�
			// mini �� ��쿡�� compareTo �Լ��� ���� ������ �Ѵ�------------------------------------------------------------------
			Calendar startTimeCalendar = (Calendar)Calendar.getInstance(mStartTime.getTimeZone());
			
			startTimeCalendar.setTimeInMillis(mStartTime.getTimeInMillis());
			startTimeCalendar.set(Calendar.HOUR_OF_DAY, 0);			
			
			Calendar endTimeCalendar = (Calendar)Calendar.getInstance(mEndTime.getTimeZone());
			
			endTimeCalendar.setTimeInMillis(mEndTime.getTimeInMillis());
			endTimeCalendar.set(Calendar.HOUR_OF_DAY, 0);
			
			int compareToEndTimerAfterUpdate = endTimeCalendar.compareTo(startTimeCalendar);
			if (compareToEndTimerAfterUpdate == 1) { // 1. valid ���·� ���̵Ǿ�� ��
				// end timer�� valid ���·� �ؽ�Ʈ�� ��ȯ�ؾ� �Ѵ�				
				setEndTimerTimeText();
				mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags);
			}
			else if (compareToEndTimerAfterUpdate == -1) { // 2. invalid ���·� ���̵Ǿ�� ��
				setEndTimerTimeText();
				mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG );
			}
			else {
				// ������ ����???
			}
		}			
	}
	
	// ������!!!
	// allday�� �и��� ���� üũ �Լ��� ������ �Ѵ�
	public void checkEndTimerValidStatus() {	
		// return 
		// negative number if a is less than b, 
		// positive number if a is greater than b, 
		// or 0 if they are equal.
		int compareToStartTimerAfterUpdate = ETime.compare(mEndTime, mStartTime);
		//int compareToStartTimerAfterUpdate = mEndTimeCalendar.compareTo(mStartTimeCalendar);
		if (compareToStartTimerAfterUpdate == 1) { //if the time of mEndTimeCalendar is after mStartTimeCalendar, return value is 1
			if (!mEndTimerValidStatus) { // ���� ���°� Invalid ���¿����� �ǹ��Ѵ�
				mEndTimerValidStatus = true;			
				mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags);
			}
			// event timer time period reset!!!!!!	
			updateEventTimePeriodByMinuteUnit();			
		}
		else { // ���� ���°� valid ���¿����� �ǹ��Ѵ�			
			mEndTimerValidStatus = false;
			// ���⼭ end timer�� invalid ���¸� display �ؾ� �Ѵ�
			mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG );
		}
	}
	
	public void checkAllDayEndTimerValidStatus() {
		int startYear = mStartTime.get(Calendar.YEAR);//.year;
		int startMonth = mStartTime.get(Calendar.MONTH);//.month;
		int startDate = mStartTime.get(Calendar.DAY_OF_MONTH);//.monthDay;
		
		int endYear = mEndTime.get(Calendar.YEAR);
		int endMonth = mEndTime.get(Calendar.MONTH);
		int endDate = mEndTime.get(Calendar.DAY_OF_MONTH);
		
		Calendar startTimeCalendar = (Calendar)Calendar.getInstance(mStartTime.getTimeZone());
				
		Calendar endTimeCalendar = (Calendar)Calendar.getInstance(mEndTime.getTimeZone());
				
		startTimeCalendar.clear();
		endTimeCalendar.clear();
		
		startTimeCalendar.set(startYear, startMonth, startDate);
		endTimeCalendar.set(endYear, endMonth, endDate);
		
		int compareToStartTimerAfterUpdate = endTimeCalendar.compareTo(startTimeCalendar);
		if ( (compareToStartTimerAfterUpdate == 0) || 
				(compareToStartTimerAfterUpdate == 1) ) { //if the time of mEndTimeCalendar is after mStartTimeCalendar, return value is 1
			if (!mEndTimerValidStatus) { // ���� ���°� Invalid ���¿����� �ǹ��Ѵ�
				mEndTimerValidStatus = true;			
				mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags);
			}
			
			// event timer time period reset!!!!!!	
			updateEventTimePeriodByMinuteUnit();
		}
		else { // ���� ���°� valid ���¿����� �ǹ��Ѵ�			
			mEndTimerValidStatus = false;
			// ���⼭ end timer�� invalid ���¸� display �ؾ� �Ѵ�
			mEndTimerTimeText.setPaintFlags(mEndTimerValidTextViewPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG );
		}
	}
	
	
	
	public void updateEndTimer(TimerPickerUpdateData obj) {		
		int field = obj.getUpdateField();
		
		switch(field) {
		case TimePickerRender.DATE_FIELD_TEXTURE:			
			//mEndTimeCalendar.set(obj.mYear, obj.mMonth, obj.mDate, mEndTimeCalendar.get(Calendar.HOUR_OF_DAY_OF_DAY), mEndTimeCalendar.get(Calendar.MINUTE));
			//mEndTime.set(0, mEndTime.minute, mEndTime.hour, obj.mDate, obj.mMonth, obj.mYear);
			mEndTime.set(obj.mYear, obj.mMonth, obj.mDate, mEndTime.get(Calendar.HOUR_OF_DAY), mEndTime.get(Calendar.MINUTE), 0);
			break;
		case TimePickerRender.MERIDIEM_FIELD_TEXTURE:
			//mEndTimeCalendar.set(Calendar.AM_PM, obj.mAMPM);			
			Calendar endTimeCalendar = (Calendar)Calendar.getInstance(mEndTime.getTimeZone());
			
			endTimeCalendar.setTimeInMillis(mEndTime.getTimeInMillis());
			endTimeCalendar.set(Calendar.AM_PM, obj.mAMPM);
			mEndTime.setTimeInMillis(endTimeCalendar.getTimeInMillis());
			break;
		case TimePickerRender.HOUR_FIELD_TEXTURE:
			//mEndTimeCalendar.set(Calendar.HOUR_OF_DAY_OF_DAY, obj.mHour);
			//mEndTime.set(0, mEndTime.minute, obj.mHour, mEndTime.monthDay, mEndTime.month, mEndTime.year);
			mEndTime.set(mEndTime.get(Calendar.YEAR), mEndTime.get(Calendar.MONTH), mEndTime.get(Calendar.DAY_OF_MONTH), obj.mHour, mEndTime.get(Calendar.MINUTE), 0);
			break;
		case TimePickerRender.MINUTE_FIELD_TEXTURE:
			//mEndTimeCalendar.set(Calendar.MINUTE, obj.mMinute);
			//mEndTime.set(0, obj.mMinute, mEndTime.hour, mEndTime.monthDay, mEndTime.month, mEndTime.year);
			mEndTime.set(mEndTime.get(Calendar.YEAR), mEndTime.get(Calendar.MONTH), mEndTime.get(Calendar.DAY_OF_MONTH), mEndTime.get(Calendar.HOUR_OF_DAY), obj.mMinute, 0);
			break;
		}			
		
		checkEndTimerValidStatus();		
		/////////////////////////////////////
		setEndTimerTimeText();		
	}
	
	public void updateEventTimePeriodByMinuteUnit() {
		double startTimeInMillis = mStartTime.getTimeInMillis();
		double endTimeInMillis = mEndTime.getTimeInMillis();
		double intervalMillis = endTimeInMillis - startTimeInMillis;
		
		mEventTimePeriodByMinuteUnit = (int)( (intervalMillis / 1000) / 60 );
	}	
	
	public void setEndTimerTimeText() {
		TimerPickerTimeText endTimeText = new TimerPickerTimeText(mEndTime, mAllDay);	
		endTimeText.makeTimeTextByOtherCompare(mStartTime); // ������ ��¥���� �ƴ����� �����Ͽ� �ؽ�Ʈ�� �����Ѵ�
		                                                            // :������ ��¥�� ��� date �κ��� �����Ѵ�. �׸��� �ؽ�Ʈ �÷��� DarkGray�� �����Ѵ�
		mEndTimerTimeText.setTextColor(endTimeText.getTextColor());
		mEndTimerTimeText.setText(endTimeText.getTimeText());
	}
	
	public void setEndTimerTimeText(boolean allDay) {
		TimerPickerTimeText endTimeText = new TimerPickerTimeText(mEndTime, allDay);	
		endTimeText.makeTimeTextByOtherCompare(mStartTime); 
		mEndTimerTimeText.setTextColor(endTimeText.getTextColor());
		mEndTimerTimeText.setText(endTimeText.getTimeText());
	}
	
	
	public void updateAllDayEndTimer(TimerPickerUpdateData obj) {
		int field = obj.getUpdateField();
		
		switch(field) {
		case AllDayTimePickerRender.YEAR_FIELD_TEXTURE:
			//mEndTimeCalendar.set(Calendar.YEAR, obj.mYear);	
			//mEndTimeCalendar.set(Calendar.DAY_OF_MONTH, obj.mDate);
			//mEndTime.set(0, mEndTime.minute, mEndTime.hour, obj.mDate, mEndTime.month, obj.mYear);
			mEndTime.set(obj.mYear, mEndTime.get(Calendar.MONTH), obj.mDate, mEndTime.get(Calendar.HOUR_OF_DAY), mEndTime.get(Calendar.MINUTE), 0);
			break;
		case AllDayTimePickerRender.MONTH_FIELD_TEXTURE:			
			//mEndTimeCalendar.set(obj.mYear, obj.mMonth, obj.mDate);
			//mEndTime.set(0, mEndTime.minute, mEndTime.hour, obj.mDate, obj.mMonth, obj.mYear);
			mEndTime.set(obj.mYear, obj.mMonth, obj.mDate, mEndTime.get(Calendar.HOUR_OF_DAY), mEndTime.get(Calendar.MINUTE), 0);
			break;
		case AllDayTimePickerRender.DAYOFMONTH_FIELD_TEXTURE:
			//mEndTimeCalendar.set(Calendar.DAY_OF_MONTH, obj.mDate);
			//mEndTime.set(0, mEndTime.minute, mEndTime.hour, obj.mDate, mEndTime.month, mEndTime.year);
			mEndTime.set(mEndTime.get(Calendar.YEAR), mEndTime.get(Calendar.MONTH), obj.mDate, mEndTime.get(Calendar.HOUR_OF_DAY), mEndTime.get(Calendar.MINUTE), 0);
			break;
		default:
			return;
		}			
		
		checkAllDayEndTimerValidStatus();		
		/////////////////////////////////////
		setEndTimerTimeText();		
	}
	
	public void makeUpperLineOfEndTimerTextLayout() {		
		mUpperLineViewOfEndTimerTextLayout = new View(mActivity);
		mUpperLineViewOfEndTimerTextLayout.setBackgroundResource(R.color.eventViewItemUnderLineColor);
		
		int height = mResources.getDimensionPixelSize(R.dimen.eventItemLayoutUnderLineHeight);
		mLayoutParamsOfUpperLineViewOfEndTimerTextLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
		mLayoutParamsOfUpperLineViewOfEndTimerTextLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		int left_margin = mResources.getDimensionPixelSize(R.dimen.editEventItemTextPaddingLeft);	
		mLayoutParamsOfUpperLineViewOfEndTimerTextLayout.setMargins(left_margin, 0, 0, 0);		
	}	
	
	public void makeUnderLineOfEndTimerTextLayout() {		
		mUnderLineViewOfEndTimerTextLayout = new View(mActivity);
		mUnderLineViewOfEndTimerTextLayout.setBackgroundResource(R.color.eventViewItemUnderLineColor);
		
		int height = mResources.getDimensionPixelSize(R.dimen.eventItemLayoutUnderLineHeight);
		mLayoutParamsOfUnderLineViewOfEndTimerTextLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
		mLayoutParamsOfUnderLineViewOfEndTimerTextLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		int bottom_margin = mResources.getDimensionPixelSize(R.dimen.editEventTimerPickerMarginBottom);	
		mLayoutParamsOfUnderLineViewOfEndTimerTextLayout.setMargins(0, 0, 0, bottom_margin);	
	}
	
	public void attachUnderLineViewOfEndTimerTextLayout() {		
		mEndTimeLayout.addView(mUnderLineViewOfEndTimerTextLayout, mLayoutParamsOfUnderLineViewOfEndTimerTextLayout);
	}
	
	public void dettachUnderLineViewOfEndTimerTextLayout() {		
		mEndTimeLayout.removeView(mUnderLineViewOfEndTimerTextLayout);
	}	
	
	TimePickerContainer mStartTimePickerContainer;	
	TimePickerContainer mEndTimePickerContainer;	
	TimePickerContainer mAllDayStartTimePickerContainer;
	TimePickerContainer mAllDayEndTimePickerContainer;
	
	public class TimePickerCollapseSlideAnimator implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
		TimePickerContainer mTimePickerContainer;
		ValueAnimator mAnimator;
		
		public TimePickerCollapseSlideAnimator(TimePickerContainer timePickerContainer, int start, int end) {
			mTimePickerContainer = timePickerContainer;
			mAnimator = ValueAnimator.ofInt(start, end);
			mAnimator.addListener(this);
			mAnimator.addUpdateListener(this);			
		}
		
		public ValueAnimator getValueAnimator() {
			return mAnimator;
		}
		
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			int value = (Integer) animation.getAnimatedValue();

			ViewGroup.LayoutParams layoutParams = mTimePickerContainer.getLayoutParams();
			layoutParams.height = value;
			mTimePickerContainer.setLayoutParams(layoutParams);			
		}

		@Override
		public void onAnimationStart(Animator animation) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			mTimePickerContainer.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationCancel(Animator animation) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void onAnimationRepeat(Animator animation) {
			// TODO Auto-generated method stub			
		}			
	}	
	
	private void timePickerExpand(int timePickerId, boolean mustAnim) {		
		if (mAllDay) {
			if (timePickerId == START_TIME_PICKER_ID) {
				mAllDayStartTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mStartTime);
				mAllDayStartTimePickerContainer.setVisibility(View.VISIBLE);
				if (mustAnim)
					mAllDayStartTimePickerContainer.mTimePickerAnimator.start();
			}
			else {
				mAllDayEndTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mEndTime);
				mAllDayEndTimePickerContainer.setVisibility(View.VISIBLE);
				if (mustAnim)
					mAllDayEndTimePickerContainer.mTimePickerAnimator.start();
			}
		}
		else {
			if (timePickerId == START_TIME_PICKER_ID) {
				mStartTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mStartTime);
				mStartTimePickerContainer.setVisibility(View.VISIBLE);
				if (mustAnim)
					mStartTimePickerContainer.mTimePickerAnimator.start();
			}
			else {
				mEndTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mEndTime);
				mEndTimePickerContainer.setVisibility(View.VISIBLE);
				if (mustAnim)
					mEndTimePickerContainer.mTimePickerAnimator.start();
			}
		}		
	}
	
	public void collapseTimePicker(TimePickerContainer timePickerContainer) {
		timePickerContainer.mTimePickerSurfaceTextureListener.sleepRenderThread();
		
		int finalHeight = timePickerContainer.getHeight();

		TimePickerCollapseSlideAnimator timePickerSlideAnimator = new TimePickerCollapseSlideAnimator(timePickerContainer, finalHeight, 0);
		ValueAnimator animator = timePickerSlideAnimator.getValueAnimator();
		
		animator.start();
	}
	
	
	
	public void createTimePicker() {			
		TimePickerRenderThread startTimePicker = new TimePickerRender(TimePickerRender.START_TIME_PICKER_NAME, 
				mContext,
				mActivity, 				
				mTimerPickerTimeHandler, 
				mStartTime, 
				mTimePickerMetaData.mTextureViewParams.width, 
				mTimePickerMetaData.mTextureViewParams.height, 
				mTimePickerMetaData.mTimePickerRenderParameters);	
		
		mStartTimePickerContainer = (TimePickerContainer) mMainPageView.findViewById(R.id.starttime_timepicker_container);	
		mStartTimePickerContainer.init(mActivity, mTimePickerMetaData.mTextureViewParams, startTimePicker, TimePickerContainer.SLIDE_TIME_PICKER_ANIMATION);		
		
		TimePickerRenderThread endTimePicker = new TimePickerRender(TimePickerRender.END_TIME_PICKER_NAME, 
				mContext,
				mActivity, 				
				mTimerPickerTimeHandler, 
				mEndTime, 
				mTimePickerMetaData.mTextureViewParams.width, 
				mTimePickerMetaData.mTextureViewParams.height, 
				mTimePickerMetaData.mTimePickerRenderParameters);	
		
		mEndTimePickerContainer = (TimePickerContainer) mMainPageView.findViewById(R.id.endtime_timepicker_container);	
		mEndTimePickerContainer.init(mActivity, mTimePickerMetaData.mTextureViewParams, endTimePicker, TimePickerContainer.SLIDE_TIME_PICKER_ANIMATION);
		
	}	
	
	public void createAllDayTimePicker() {	
		TimePickerRenderThread startTimePicker = new AllDayTimePickerRender(AllDayTimePickerRender.START_TIME_PICKER_NAME, 
				mContext,
				mActivity, 				
				mAllDayTimerPickerTimeHandler, 
				mStartTime, 
				mTimePickerMetaData.mTextureViewParams.width, 
				mTimePickerMetaData.mTextureViewParams.height, 
				mTimePickerMetaData.mAllDayTimePickerRenderParameters);	
		
		mAllDayStartTimePickerContainer = (TimePickerContainer) mMainPageView.findViewById(R.id.starttime_allday_timepicker_container);	
		mAllDayStartTimePickerContainer.init(mActivity, mTimePickerMetaData.mTextureViewParams, startTimePicker, TimePickerContainer.SLIDE_TIME_PICKER_ANIMATION);		
		
		
		TimePickerRenderThread endTimePicker = new AllDayTimePickerRender(AllDayTimePickerRender.END_TIME_PICKER_NAME, 
				mContext,
				mActivity, 				
				mAllDayTimerPickerTimeHandler, 
				mEndTime, 
				mTimePickerMetaData.mTextureViewParams.width, 
				mTimePickerMetaData.mTextureViewParams.height, 
				mTimePickerMetaData.mAllDayTimePickerRenderParameters);	
		
		mAllDayEndTimePickerContainer = (TimePickerContainer) mMainPageView.findViewById(R.id.endtime_allday_timepicker_container);	
		mAllDayEndTimePickerContainer.init(mActivity, mTimePickerMetaData.mTextureViewParams, endTimePicker, TimePickerContainer.SLIDE_TIME_PICKER_ANIMATION);
		
	}
	
	
	
	public void switchingAllDayTimePickerView() {
		if (mAllDay) {
			if (mStartTimeExpended) {
				mStartTimePickerContainer.mTimePickerSurfaceTextureListener.sleepRenderThread();
				mStartTimePickerContainer.setVisibility(View.GONE);					
				
				mAllDayStartTimePickerContainer.setVisibility(View.VISIBLE);
				mAllDayStartTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mStartTime);				
			}
			else {
				mEndTimePickerContainer.mTimePickerSurfaceTextureListener.sleepRenderThread();
				mEndTimePickerContainer.setVisibility(View.GONE);				
				
				checkAllDayEndTimerValidStatus();
				mAllDayEndTimePickerContainer.setVisibility(View.VISIBLE);
				mAllDayEndTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mEndTime);				
			}			
		}
		else {
			if (mStartTimeExpended) {
				mAllDayStartTimePickerContainer.mTimePickerSurfaceTextureListener.sleepRenderThread();
				mAllDayStartTimePickerContainer.setVisibility(View.GONE);								
				
				mStartTimePickerContainer.setVisibility(View.VISIBLE);	
				mStartTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mStartTime);				
			}
			else {
				mAllDayEndTimePickerContainer.mTimePickerSurfaceTextureListener.sleepRenderThread();
				mAllDayEndTimePickerContainer.setVisibility(View.GONE);				
				
				checkEndTimerValidStatus();
				mEndTimePickerContainer.setVisibility(View.VISIBLE);
				mEndTimePickerContainer.mTimePickerSurfaceTextureListener.wakeUpRenderThread(mEndTime);				
			}	        
		}		
	}
	
	public void attachUpperLineViewOfEndTimerTextLayout() {		
		mEndTimeLayout.addView(mUpperLineViewOfEndTimerTextLayout, mLayoutParamsOfUpperLineViewOfEndTimerTextLayout);
	}
	
	public void dettachUpperLineViewOfEndTimerTextLayout() {		
		mEndTimeLayout.removeView(mUpperLineViewOfEndTimerTextLayout);
	}	
	
	Handler mTimerPickerTimeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {				
			super.handleMessage(msg);
			
			TimerPickerUpdateData obj = (TimerPickerUpdateData)msg.obj;			
			int timerID = obj.getTimerID();
			
			if(timerID == TimePickerRender.START_TIME_PICKER_ID) {
				updateStartTimer(obj);
			}
			else if (timerID == TimePickerRender.END_TIME_PICKER_ID) {
				updateEndTimer(obj);
			}
		}			
	};	
	
	Handler mAllDayTimerPickerTimeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {				
			super.handleMessage(msg);
			
			TimerPickerUpdateData obj = (TimerPickerUpdateData)msg.obj;			
			int timerID = obj.getTimerID();
			
			if(timerID == AllDayTimePickerRender.START_TIME_PICKER_ID) {
				updateAllDayStartTimer(obj);
			}
			else if (timerID == AllDayTimePickerRender.END_TIME_PICKER_ID) {
				updateAllDayEndTimer(obj);
			}
		}			
	};	
	
	
	
	public static final int START_TIME_PICKER_ID = 1;
	public static final int END_TIME_PICKER_ID = 2;	
	public class TimerVisible implements AnimationListener {
	    private LinearLayout mLayout;

	    public TimerVisible(LinearLayout layout) {
	    	mLayout = layout;
	    }

	    @Override
	    public void onAnimationEnd(Animation animation) {
	    	//mLayout.setVisibility(View.VISIBLE);
	    }

	    @Override
	    public void onAnimationRepeat(Animation animation) {
	        // TODO Auto-generated method stub
	    }

	    @Override
	    public void onAnimationStart(Animation animation) {
	        // TODO Auto-generated method stub
	    	mLayout.setVisibility(View.VISIBLE);
	    }
	}
	
	public void setTimePickerVisibility(int timePickerId, int visibility, boolean mustAnim) {
		if (mAllDay) {
			if (timePickerId == START_TIME_PICKER_ID) {
				if (visibility == View.VISIBLE) {					
					timePickerExpand(START_TIME_PICKER_ID, mustAnim);
				}
				else {					
					collapseTimePicker(mAllDayStartTimePickerContainer);
				}				
			}
			else if (timePickerId == END_TIME_PICKER_ID) {
				if (visibility == View.VISIBLE) {					
					timePickerExpand(END_TIME_PICKER_ID, mustAnim);
				}
				else {					
					collapseTimePicker(mAllDayEndTimePickerContainer);
				}
			}				
		}			
		else {
			if (timePickerId == START_TIME_PICKER_ID) {
				if (visibility == View.VISIBLE) {					
					timePickerExpand(START_TIME_PICKER_ID, mustAnim);
				}
				else {					
					collapseTimePicker(mStartTimePickerContainer);
				}				
			}
			else if (timePickerId == END_TIME_PICKER_ID) {
				if (visibility == View.VISIBLE) {					
					timePickerExpand(END_TIME_PICKER_ID, mustAnim);
				}
				else {					
					collapseTimePicker(mEndTimePickerContainer);
				}				
			}
		}			
	}
	
	View.OnClickListener mStartTimerTextLayoutClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
        	if (!mStartTimeExpended) {
        		if (mEndTimeExpended) {
        			mEndTimeExpended = false;
        			setTimePickerVisibility(END_TIME_PICKER_ID, View.GONE, true); 
        			dettachUnderLineViewOfEndTimerTextLayout();        			       			   			
        		}
        		
        		mStartTimeExpended = true;
        		setTimePickerVisibility(START_TIME_PICKER_ID, View.VISIBLE, true);
        		attachUpperLineViewOfEndTimerTextLayout();
        	}
        	else {
        		mStartTimeExpended = false;
        		setTimePickerVisibility(START_TIME_PICKER_ID, View.GONE, true);   
        		dettachUpperLineViewOfEndTimerTextLayout();        		     		       		    		     		
        	}               
        }
    };
	
    
    View.OnClickListener mEndTimerTextLayoutClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
        	if (!mEndTimeExpended) {
        		if (mStartTimeExpended) {
        			mStartTimeExpended = false;
        			setTimePickerVisibility(START_TIME_PICKER_ID, View.GONE, true);
        			dettachUpperLineViewOfEndTimerTextLayout();        			        			   			     		
        		}
        		
        		mEndTimeExpended = true; 
        		setTimePickerVisibility(END_TIME_PICKER_ID, View.VISIBLE, true);
        		if (mAllDay)
        			checkAllDayEndTimerValidStatus();
        		else
        			checkEndTimerValidStatus(); 
                                                    
                attachUnderLineViewOfEndTimerTextLayout(); // ���⼭ ������ �߻��Ͽ���
                // java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
            }
        	else {
        		mEndTimeExpended = false;
        		setTimePickerVisibility(END_TIME_PICKER_ID, View.GONE, true);
        		dettachUnderLineViewOfEndTimerTextLayout();       		        		    		     			           		
        	}                
        }
    };
    
    
	int mCalendarIDColumnIndex;
    int mCalendarDisplayNameColumnIndex;
    int mCalendarAccountNameColumnIndex;
    int mCalendarOwnerAccountColumnIndex;
    int mCalendarAccountTypeColumnIndex;
    int mCalendarColorColumnIndex;
    
    public class CalendarInfo {
    	int mCalendarId;
    	int mCalendarColor;
    	String mDisplayName;
    	String mOwnerAccount;
    	String mAccountName;
    	String mAccountType;
    	public CalendarInfo(int calendarId, String displayName, String ownerAccount, String accountName, String accountType, int color) {
    		mCalendarId = calendarId;
    		mDisplayName = displayName;
    		mOwnerAccount = ownerAccount;
        	mAccountName = accountName;
        	mAccountType = accountType;
        	mCalendarColor = color;
    	}
    }
      
    
    // 2200 is the value of MINIMUM_SNAP_VELOCITY !!!
    //public static final float SWITCH_SUB_PAGE_VELOCITY = 2200 * 2;
    //public static final float SWITCH_MAIN_PAGE_VELOCITY = 2200 * 2.5f;
    
    final float SWITCH_MAIN_PAGE_IN_FROMXVALUE = -1.0f;
    final float SWITCH_MAIN_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_TOXVALUE = 1.0f;
	
    final float SWITCH_SUB_PAGE_IN_FROMXVALUE = 1.0f;
    final float SWITCH_SUB_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_TOXVALUE = -1.0f;
	
    
    float mViewSwitchingDelta;
    
    
    TranslateAnimation mSubViewExitAnimation;
    AlphaAnimation mMainViewEnterDimmingAnimation;
    public void switchMainView() {
    	int subPageId = mCurrentViewingPage;
    	mCurrentViewingPage = EVENT_MAIN_PAGE;    	
    	
    	float enterAnimationDistance = mViewWidth * 0.4f;
    	float exitAnimationDistance = mViewWidth; // 
    	
    	float velocity = SWITCH_SUB_PAGE_EXIT_VELOCITY ;
    	long exitAnimationDuration = ITEAnimationUtils.calculateDuration(exitAnimationDistance, mViewWidth, MINIMUM_SNAP_VELOCITY, velocity); 	    	
    	long enterAnimationDuration = exitAnimationDuration;
    	
    	mSubViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, 1, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	mSubViewExitAnimation.setDuration(exitAnimationDuration);  
    	ITEAnimInterpolator exitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mSubViewExitAnimation);  
    	mSubViewExitAnimation.setInterpolator(exitInterpolator);    	
    	    	
    	TranslateAnimation mainViewEnterSlideAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
		mainViewEnterSlideAnimation.setDuration(enterAnimationDuration);
		ITEAnimInterpolator enterSlideInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mainViewEnterSlideAnimation); 
		mainViewEnterSlideAnimation.setInterpolator(enterSlideInterpolator);
		mainViewEnterSlideAnimation.setAnimationListener(mMainViewEnterAnimationListener);		
		
    	mMainViewEnterDimmingAnimation = new AlphaAnimation(1.0f, 0.0f); 
    	mMainViewEnterDimmingAnimation.setDuration(enterAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mMainViewEnterDimmingAnimation);   	
    	mMainViewEnterDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);		
    	
    	mViewSwitcher.setInAnimation(mainViewEnterSlideAnimation);
    	mViewSwitcher.setOutAnimation(mSubViewExitAnimation);  
    	    	
        mViewSwitcher.showPrevious();
    	
		switch(subPageId) {    		
		case EVENT_REPEAT_PAGE:
			// ���⼭ selected icon�� detach �ؾ� �Ѵ�
			//int freqLayoutIndex = mRepeatPageView.getFreqValue();
			mRepeatSettingSwitchPageView.detachRepeatPageItemSelectionIcon();//mRepeatPageView.detachRepeatPageItemSelectionIcon(freqLayoutIndex);
			
			mRrule = mRRuleContext.makeRRule();/////////////////////////////////////////
			
			if (mRrule != null) {
			    mEventRecurrence.parse(mRrule); // INTERVAL�� 1�� ��� mEventRecurrence.interval�� 1�� �������� �ʴ´�???
			                                    // RecurrenceContext.copyModelToEventRecurrence�� �ڵ� �߿�,,,
							    			    // if (model.interval <= 1) {
							    		        //    er.interval = 0;
							    		        // } 
			    								// �� �ڵ尡 �� �ָ��ϴ� �� interval�� 1�̸�, 0���� �����ϴ� ���ϱ�???
			                                    // ������ interval�� ����Ʈ ���� 1(INTERVAL_DEFAULT==1)�̴�
			}
			else {
				mEventRecurrence.reset();
			}
			mViewSwitcher.removeView(mRepeatSettingSwitchPageView);//mViewSwitcher.removeView(mRepeatPageView);
			break;
		case EVENT_REPEATEND_PAGE:
			mRepeatEndPageView.setEventRepeatEndItemText();  
			mRrule = mRRuleContext.makeRRule();
			Log.i("tag", "Rrule : " + mRrule);
			if (mRrule != null) {
			    mEventRecurrence.parse(mRrule);
			}
						
			mViewSwitcher.removeView(mRepeatEndPageView);
			break;
		case EVENT_REMINDER_PAGE:
			mViewSwitcher.removeView(mReminderPageView);
			break;    		
		case EVENT_CALENDAR_PAGE:
			mViewSwitcher.removeView(mCalendarPageView);
			break;
		case EVENT_ATTENDEE_PAGE:
			int numbers = mAttendeePageView.getAttendeePageEditText();
			mEventAttendeeNumbersText.setText(String.valueOf(numbers));    			 
			hideIME();
			mViewSwitcher.removeView(mAttendeePageView);
			break;
		case EVENT_AVAILABILITY_PAGE:
			mViewSwitcher.removeView(mAvailabilityPageView);
			break;
		case EVENT_VISIBILITY_PAGE:
			mViewSwitcher.removeView(mVisibilityPageView);
			break;
		default:
			break;
		}    		
    }
    
    TranslateAnimation mSubViewEnterAnimation;
    AlphaAnimation mMainViewExitDimmingAnimation;
    public void switchSubView(int subViewID) {
    	    	
    	float enterAnimationDistance = mViewWidth; 
		float exitAnimationDistance = mViewWidth * 0.4f; 
		
		float velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mViewWidth, MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
		
    	mSubViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	mSubViewEnterAnimation.setDuration(enterAnimationDuration);
    	ITEAnimInterpolator subViewEnterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mSubViewEnterAnimation);   	
    	mSubViewEnterAnimation.setInterpolator(subViewEnterInterpolator);
    	mSubViewEnterAnimation.setAnimationListener(mSubViewEnterAnimationListener);    	
    	
    	mMainViewExitDimmingAnimation = new AlphaAnimation(0.0f, 1.0f); 
    	mMainViewExitDimmingAnimation.setDuration(exitAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mMainViewExitDimmingAnimation);   	
    	mMainViewExitDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);
    	    	
    	TranslateAnimation mainViewExitSlideAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);		
    	ITEAnimInterpolator mainViewExitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mainViewExitSlideAnimation); 
    	mainViewExitSlideAnimation.setInterpolator(mainViewExitInterpolator);
    	mainViewExitSlideAnimation.setDuration(exitAnimationDuration);    	
    	mainViewExitSlideAnimation.setAnimationListener(mMainViewExitAnimationListener);
    	
    	//if (subViewID == EVENT_ATTENDEE_PAGE)
    		//mainViewExitSlideAnimation.setAnimationListener(new AttendeePageBroadcaster());
    	
    	mViewSwitcher.setInAnimation(mSubViewEnterAnimation);
    	mViewSwitcher.setOutAnimation(mainViewExitSlideAnimation);   
    	
    	/*    	
        if (subViewID == EVENT_ATTENDEE_PAGE)
    		outAnimation.setAnimationListener(new AttendeePageBroadcaster());   
        
        */
    	
    	addSubViewToViewSwitcher(subViewID);          
    }
    
    
    public void addSubViewToViewSwitcher(int subViewID) {
    	switch(subViewID) {    		
		case EVENT_REPEAT_PAGE:   					
			// ���⼭ ������ �߻��Ͽ���
			mRepeatSettingSwitchPageView.attachRepeatPageItemSelectionIcon();			
			mViewSwitcher.addView(mRepeatSettingSwitchPageView);
			break;
		case EVENT_REPEATEND_PAGE:				
			int repeatEndUntill = mRRuleContext.getEndUntillValue();
			if (repeatEndUntill == EditEventRepeatEndSettingSubView.EVENT_NO_REPEATEND) {
				mRepeatEndPageView.attachRepeatEndPageItemSelectionIcon(EditEventRepeatEndSettingSubView.EVENT_NO_REPEATEND);
				mRepeatEndPageView.mRepeatEndTimePickerContainer.setVisibility(View.GONE);	
				mRepeatEndPageView.mRepeatEndTimePickerContainerLowerOutline.setVisibility(View.GONE);	
			}
			else {
				mRepeatEndPageView.attachRepeatEndPageItemSelectionIcon(EditEventRepeatEndSettingSubView.EVENT_REPEATEND_DATE);
				mRepeatEndPageView.mRepeatEndTimePickerContainer.setVisibility(View.VISIBLE);		
				mRepeatEndPageView.mRepeatEndTimePickerContainerLowerOutline.setVisibility(View.VISIBLE);
			}
			mViewSwitcher.addView(mRepeatEndPageView);    			
			break;
		case EVENT_REMINDER_PAGE:
			mViewSwitcher.addView(mReminderPageView);
			break;    		
		case EVENT_CALENDAR_PAGE:
			mViewSwitcher.addView(mCalendarPageView);
			break;
		case EVENT_ATTENDEE_PAGE:    			
			mViewSwitcher.addView(mAttendeePageView);    			
			break;
		case EVENT_AVAILABILITY_PAGE:
			mViewSwitcher.addView(mAvailabilityPageView);
			break;
		case EVENT_VISIBILITY_PAGE:
			mViewSwitcher.addView(mVisibilityPageView);
			break;
		default:
			break;
		}           
    }
    
    
    AnimationListener mSubViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
			
			mFragment.mActionBar.startSubPageActionBarEnterAnim();
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AnimationListener mMainViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			
			mFragment.mActionBar.startMainPageActionBarEnterAnim();
			
			int bgColor = Color.argb(30, 0, 0, 0);
			mFragment.mSwitchDimmingLayout.setBackgroundColor(bgColor);
	    	mFragment.mSwitchDimmingLayout.startAnimation(mMainViewEnterDimmingAnimation);
			
			//mItemTouchDownReleaseVA.start();
			
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			int bgColor = Color.argb(0, 0, 0, 0);
			mFragment.mSwitchDimmingLayout.setBackgroundColor(bgColor);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AnimationListener mMainViewExitAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
			int bgColor = Color.argb(15, 0, 0, 0);
	    	mFragment.mSwitchDimmingLayout.setBackgroundColor(bgColor);
			mFragment.mSwitchDimmingLayout.startAnimation(mMainViewExitDimmingAnimation);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
    private void cancelAnimation() {
        Animation in = mViewSwitcher.getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = mViewSwitcher.getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
    }
    
    private float mAnimationDistance = 0;
    public static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float SWITCH_MAIN_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f; 
	public static final float SWITCH_SUB_PAGE_EXIT_VELOCITY = SWITCH_MAIN_PAGE_VELOCITY * 1.25f; 

    private class ScrollInterpolator implements Interpolator {   	
    	
        public ScrollInterpolator() {        	
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
    }
    
    public long calculateDuration(float delta, float width, float velocity) {
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
        float f1 = MINIMUM_SNAP_VELOCITY * 2;
        velocity = Math.max(f1, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        /*if (DEBUG) {
            Log.e(TAG, "halfScreenSize:" + halfScreenSize + " delta:" + delta + " distanceRatio:"
                    + distanceRatio + " distance:" + distance + " velocity:" + velocity
                    + " duration:" + duration + " distanceInfluenceForSnapDuration:"
                    + distanceInfluenceForSnapDuration);
        }*/
        return duration;
    }
    
    /*
     * We want the duration of the page snap animation to be influenced by the
     * distance that the screen has to travel, however, we don't want this
     * duration to be effected in a purely linear fashion. Instead, we use this
     * method to moderate the effect that the distance of travel has on the
     * overall snap duration.
     */
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }   
    
    
    private CalendarEventModel mModel;
    
    /**
     * Does prep steps for saving a calendar event.
     *
     * This triggers a parse of the attendees list and checks if the event is
     * ready to be saved. An event is ready to be saved so long as a model
     * exists and has a calendar it can be associated with, either because it's
     * an existing event or we've finished querying.
     *
     * @return false if there is no model or no calendar had been loaded yet,
     * true otherwise.
     */
    public boolean prepareForSave() {
    	// create new event �� ��쿡�� UI�� ���� ������ �����Ѵ�
    	// :�׷��� �ϱ� ���ؼ� || (mModel.mUri == null) �� �ּ����� ó����
        if (mModel == null/* || (mModel.mUri == null)*/) {
            return false;
        }
        return fillModelFromUI();
    }

    private ArrayList<LinearLayout> mReminderItems = new ArrayList<LinearLayout>(0);
    private ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();
    private ArrayList<ReminderEntry> mUnsupportedReminderMinutes = new ArrayList<ReminderEntry>();
    private String mRrule = null;
    public ArrayList<Integer> mReminderMinuteValues;
    private ArrayList<String> mReminderMinuteLabels;
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMethodLabels;
    private ArrayList<Integer> mAvailabilityValues;
    private ArrayList<String> mAvailabilityLabels;
    private ArrayList<String> mOriginalAvailabilityLabels;
    private ArrayAdapter<String> mAvailabilityAdapter;
    private boolean mAvailabilityExplicitlySet;
    private boolean mAllDayChangingAvailability;
    private int mAvailabilityCurrentlySelected;
    
    
    public Calendar mStartTime;
    public Calendar mEndTime;
    public String mTzId;
    public TimeZone mTimeZone;
    public boolean mAllDay = false;
    private int mModification = EditEventHelper.MODIFY_UNINITIALIZED;
    
    @SuppressWarnings("deprecation")   
    public boolean fillModelFromReadOnlyUi() {
        if (mModel == null || (mModel.mUri == null)) {
            return false;
        }
        mModel.mReminders = mReminderPageView.getReminderEntries();
        mModel.mReminders.addAll(mUnsupportedReminders);
        mModel.normalizeReminders();
        /*int status = EventInfoFragment.getResponseFromButtonId(
                mResponseRadioGroup.getCheckedRadioButtonId());*/
        
        int status = Attendees.ATTENDEE_STATUS_NONE; // +added by intheeast,,,for test!!!
        if (status != Attendees.ATTENDEE_STATUS_NONE) {
            mModel.mSelfAttendeeStatus = status;
        }
        return true;
    }
    
    
 // Goes through the UI elements and updates the model as necessary
    public boolean fillModelFromUI() {
        if (mModel == null) {
            return false;
        }
        
        mModel.mReminders = mReminderPageView.getReminderEntries();
        //mModel.mReminders.addAll(mUnsupportedReminders);
        mModel.normalizeReminders();
        //mModel.mHasAlarm = mReminderItems.size() > 0;
        mModel.mHasAlarm = mModel.mReminders.size() > 0;
        mModel.mTitle = mTitleTextView.getText().toString();
        mModel.mAllDay = mAllDayCheckBox.isChecked();
        mModel.mLocation = mLocationTextView.getText().toString();
        mModel.mDescription = mDescriptionTextView.getText().toString();
        if (TextUtils.isEmpty(mModel.mLocation)) {
            mModel.mLocation = null;
        }
        if (TextUtils.isEmpty(mModel.mDescription)) {
            mModel.mDescription = null;
        }

        // ���� �Ʒ� ������ ���� ���δ� new event�� �׽�Ʈ������ �ش� ������ ���� ������ �ּ����� ó���Ѵ�
        /*int status = EventInfoFragment.getResponseFromButtonId(mResponseRadioGroup
                .getCheckedRadioButtonId());
        if (status != Attendees.ATTENDEE_STATUS_NONE) {
            mModel.mSelfAttendeeStatus = status;
        }*/

        //mAttendeesList = null; // for test!!!
        if (mAttendeePageView.mAttendeesList != null) {
        	mModel.mAttendeesList.clear();
        	String attendees = mAttendeePageView.getAttendees();        	
            mModel.addAttendees(attendees, mAttendeePageView.mEmailValidator);            
        }

        // If this was a new event we need to fill in the Calendar information
        if (mModel.mUri == null) {
            //mModel.mCalendarId = mCalendarsSpinner.getSelectedItemId();
            //int calendarCursorPosition = mCalendarsSpinner.getSelectedItemPosition();
        	//mModel.mCalendarId = 0; // for test
        	//int calendarCursorPosition = 0; // for test
            
            /*if (mCalendarsCursor.moveToPosition(calendarCursorPosition)) {
                String defaultCalendar = mCalendarsCursor.getString(EditKEventHelper.CALENDARS_INDEX_OWNER_ACCOUNT);
                Utils.setSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR, defaultCalendar);
                mModel.mOwnerAccount = defaultCalendar;
                mModel.mOrganizer = defaultCalendar;
                mModel.mCalendarId = mCalendarsCursor.getLong(EditKEventHelper.CALENDARS_INDEX_ID);
            }*/
        }

        
        if (mModel.mAllDay) {
            // Reset start and end time, increment the monthDay by 1, and set
            // the timezone to UTC, as required for all-day events.
            //mTzId = ETime.TIMEZONE_UTC;
            //mTimeZone = TimeZone.getTimeZone(mTzId);
            
            mStartTime.set(Calendar.HOUR_OF_DAY, 0);//mStartTime.hour = 0;
            mStartTime.set(Calendar.MINUTE, 0);//mStartTime.minute = 0;
            mStartTime.set(Calendar.SECOND, 0);//mStartTime.second = 0;
            //mStartTime = ETime.switchTimezone(mStartTime, mTimeZone);//mStartTime.timezone = mTimezone;
            mModel.mStart = mStartTime.getTimeInMillis();//mStartTime.normalize(true);

            mEndTime.set(Calendar.HOUR_OF_DAY, 0);//mEndTime.hour = 0;
            mEndTime.set(Calendar.MINUTE, 0);//mEndTime.minute = 0;
            mEndTime.set(Calendar.SECOND, 0);//mEndTime.second = 0;
            //mEndTime = ETime.switchTimezone(mEndTime, mTimeZone);//mEndTime.timezone = mTimezone;
            // When a user see the event duration as "X - Y" (e.g. Oct. 28 - Oct. 29), end time
            // should be Y + 1 (Oct.30).
            final long normalizedEndTimeMillis =
                    mEndTime.getTimeInMillis() + DateUtils.DAY_IN_MILLIS;
            if (normalizedEndTimeMillis < mModel.mStart) {
                // mEnd should be midnight of the next day of mStart.
                mModel.mEnd = mModel.mStart + DateUtils.DAY_IN_MILLIS;
            } else {
                mModel.mEnd = normalizedEndTimeMillis;
            }
        } else {
        	//mStartTime = ETime.switchTimezone(mStartTime, mTimeZone);//mStartTime.timezone = mTimezone;
        	//mEndTime = ETime.switchTimezone(mEndTime, mTimeZone);//mEndTime.timezone = mTimezone;
            mModel.mStart = mStartTime.getTimeInMillis();
            mModel.mEnd = mEndTime.getTimeInMillis();
        }
        
        mModel.mTimezone = mTzId;
        //mModel.mAccessLevel = mAccessLevelSpinner.getSelectedItemPosition();
        /*
         <string-array name="visibility">
        	<item>Default</item>
        	<item>Private</item>
        	<item>Public</item>
    	</string-array> 
         */
        // ���� �츮�� AccessLevel ���� UI�� �������� �ʰ� �ִ�!!!
        mModel.mAccessLevel = mVisibilityValue; // default 
        // TODO set correct availability value
        //mModel.mAvailability = mAvailabilityValues.get(mAvailabilitySpinner.getSelectedItemPosition());
        mModel.mAvailability = mAvailabilityValues.get(mAvailabilityPageView.mAvailabilityValue);
        // rrrule
        // If we're making an exception we don't want it to be a repeating
        // event.
        if (mModification == EditEventHelper.MODIFY_SELECTED) {
            mModel.mRrule = null;
        } else {
            mModel.mRrule = mRrule;
        }

        return true;
    }
    
    private static final String GOOGLE_SECONDARY_CALENDAR = "calendar.google.com";
    
    
    private int mDefaultReminderMinutes;
    /**
     * Fill in the view with the contents of the given event model. This allows
     * an edit view to be initialized before the event has been loaded. Passing
     * in null for the model will display a loading screen. A non-null model
     * will fill in the view's fields with the data contained in the model.
     *
     * @param model The event model to pull the data from
     */
    public void setMainPageLayout(CalendarEventModel model) {
        mModel = model;

        // Need to close the autocomplete adapter to prevent leaking cursors.        
        /*if (mAddressAdapter != null && mAddressAdapter instanceof EmailAddressAdapter) {
            ((EmailAddressAdapter)mAddressAdapter).close();
            mAddressAdapter = null;
        }*/		
        
        if (mModel == null) {
            // Display loading screen
            //mLoadingMessage.setVisibility(View.VISIBLE);
            //mScrollView.setVisibility(View.GONE);
            return;
        }
        
        ////////////////////////////////////////////////////////////////////////////////////////
        // canRespond�� new event �����ÿ��� �ʿ��� ������ �ƴϴ�
        // ������ event�� ������ �� �ʿ���...
        boolean canRespond = EditEventHelper.canRespond(mModel);
        ////////////////////////////////////////////////////////////////////////////////////////
        
        
        //mTzId = mModel.mTimezone; // this will be UTC for all day events
        //mTimeZone = TimeZone.getTimeZone(mTzId);

        mRrule = mModel.mRrule;
        mRRuleContext = new RecurrenceContext(mActivity, mStartTime.getTimeInMillis(), mStartTime.getTimeZone().getID(), mRrule);
        mRepeatSettingSwitchPageView.setRRuleContext(mRRuleContext);
        
        
        if (!TextUtils.isEmpty(mRrule)) {
            mEventRecurrence.parse(mRrule);
            
            mRepeatSettingSwitchPageView.initEventRepeatItem();//mRepeatPageView.updateEventRepeatItem(mRepeatPageView.getFreqValue());
            
            mBetweenRepeatAndRepeatEndLayout.setVisibility(View.VISIBLE);
        	mRepeatEndTimeLayout.setVisibility(View.VISIBLE);
        	
        	int repeatEndValue = mRRuleContext.getEndUntillValue();
        	if (repeatEndValue == EditEventRepeatEndSettingSubView.EVENT_NO_REPEATEND) {          		
        		mEventRepeatEndText.setText(R.string.does_not_repeatend);    		
        		mRepeatEndTimeObj.mReseted = true;
        	}
        	else {           		
        		//int test = Calendar.JANUARY;
        		// END_BY_DATE
        		if (repeatEndValue == EditEventRepeatEndSettingSubView.EVENT_REPEATEND_DATE) {
        			Calendar cal = GregorianCalendar.getInstance(mStartTime.getTimeZone());
        			long millis = ETime.parse(mStartTime.getTimeZone(), mEventRecurrence.until);
        			cal.setTimeInMillis(millis);
        			mRepeatEndTimeObj.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        			
        			mRepeatEndTimeObj.mReseted = false;
            		mRepeatEndTimeObj.checkValidTime();    		
            		mRepeatEndTime.set(mRepeatEndTimeObj.mYear, mRepeatEndTimeObj.mMonth, mRepeatEndTimeObj.mDayOfMonth, 0, 0, 0);
            		mRRuleContext.setEndDate(mRepeatEndTimeObj.mYear, mRepeatEndTimeObj.mMonth, mRepeatEndTimeObj.mDayOfMonth);
            		
            		TimerPickerTimeText repeatEndText = new TimerPickerTimeText(mRepeatEndTime, true);
            		repeatEndText.makeTimeText();
            		mEventRepeatEndText.setText(repeatEndText.getTimeText());
        		}
        		else { // END_BY_COUNT
        			// ��� ó���� ���ΰ�? 
        			// :�ϴ� �������� ��������!!!        			
        		}         		
        	}        	
        } 
        
        mRepeatEndPageView.createRepeatEndTimePicker();
        
        if (mEventRecurrence.startDate == null) {
            mEventRecurrence.startDate = mStartTime;
        }                       

        boolean prevAllDay = mAllDayCheckBox.isChecked();
        mAllDay = false; // default to false. Let setAllDayViewsVisibility update it as needed
        if (mModel.mAllDay) {
            mAllDayCheckBox.setChecked(true);
            // put things back in local time for all day events
            /*mTzId = Utils.getTimeZone(mActivity, null);
            mTimeZone = TimeZone.getTimeZone(mTzId);
            mStartTime = ETime.switchTimezone(mStartTime, mTimeZone);
            mEndTime = ETime.switchTimezone(mEndTime, mTimeZone);*/
            
        } else {
            mAllDayCheckBox.setChecked(false);
        }
        // On a rotation we need to update the views but onCheckedChanged
        // doesn't get called
        if (prevAllDay == mAllDayCheckBox.isChecked()) {
            setAllDayViewsVisibility(prevAllDay);
        }

        SharedPreferences prefs = SettingsFragment.getSharedPreferences(mActivity);
        String defaultReminderString = prefs.getString(
                SettingsFragment.KEY_DEFAULT_REMINDER, SettingsFragment.NO_REMINDER_STRING);
        mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);

        if (mModel.mTitle != null) {
            mTitleTextView.setTextKeepState(mModel.mTitle);
        }

        if (mModel.mIsOrganizer || TextUtils.isEmpty(mModel.mOrganizer)
                || mModel.mOrganizer.endsWith(GOOGLE_SECONDARY_CALENDAR)) {
        	// new event�� �����ϴ� ���,
        	// �̺�Ʈ �����ڴ� �翬�� user �����̹Ƿ� organizer�� ������ �ʿ䰡 ����
        	/*
            mView.findViewById(R.id.organizer_label).setVisibility(View.GONE);
            mView.findViewById(R.id.organizer).setVisibility(View.GONE);
            mOrganizerGroup.setVisibility(View.GONE);
            */
        } else {
            //((TextView) mView.findViewById(R.id.organizer)).setText(model.mOrganizerDisplayName);
        }

        if (mModel.mLocation != null) {
            mLocationTextView.setTextKeepState(mModel.mLocation);
        }

        if (mModel.mDescription != null) {
            mDescriptionTextView.setTextKeepState(mModel.mDescription);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //int availIndex = mAvailabilityValues.indexOf(model.mAvailability);
        //if (availIndex != -1) {
            //mAvailabilitySpinner.setSelection(availIndex);
        //}
        //mAccessLevelSpinner.setSelection(model.mAccessLevel);

        /*
        View responseLabel = mView.findViewById(R.id.response_label);
        if (canRespond) {
            int buttonToCheck = EventInfoFragment
                    .findButtonIdForResponse(model.mSelfAttendeeStatus);
            mResponseRadioGroup.check(buttonToCheck); // -1 clear all radio buttons
            mResponseRadioGroup.setVisibility(View.VISIBLE);
            responseLabel.setVisibility(View.VISIBLE);
        } else {
            responseLabel.setVisibility(View.GONE);
            mResponseRadioGroup.setVisibility(View.GONE);
            mResponseGroup.setVisibility(View.GONE);
        }
		*/
        
        if (mModel.isEventColorInitialized()) {
            //updateHeadlineColor(model, model.getEventColor());
        }
        
        // If the user is allowed to change the attendees set up the view and
        // validator
        // EditEventHelper.saveEvent����
        // �ش� �̺�Ʈ�� NEW�̸�, Event Table�� Event�� insert �� �� 
        // Events.HAS_ATTENDEE_DATA �ʵ带 1�� �����Ѵ�???
        /*
         * Whether the event has attendee information.  True if the event
         * has full attendee data, false if the event has information about
         * self only. Column name.
         * <P>Type: INTEGER (boolean)</P>
         * public static final String HAS_ATTENDEE_DATA = "hasAttendeeData";
        */        
        if (mModel.mHasAttendeeData) {
        	mBetweenRepeatAndAttendeeSeperatorLayout.setVisibility(View.VISIBLE);
        	mAttendeeLayout.setVisibility(View.VISIBLE);
        	int attendeeNumbers = mModel.mAttendeesList.size();
        	mEventAttendeeNumbersText.setText(String.valueOf(attendeeNumbers)); 
        	mAttendeePageView.updateAttendees(mModel.mAttendeesList);
        }
                
        mCalendarID = (int) mModel.mCalendarId;
            	
        View calendarColorIcon = mMainPageView.findViewById(R.id.calendar_color_icon);
        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
        Paint paint = shapeDrawable.getPaint();		
        paint.setColor(Utils.getDisplayColorFromColor(mModel.getCalendarColor()));
        calendarColorIcon.setBackground(shapeDrawable);				
        mEventCalendarText.setText(mModel.mCalendarDisplayName);
        
        initReminders();
        prepareAvailability();
        
        sendAccessibilityEvent();
    }
    
    public void setModification(int modifyWhich) {
        mModification = modifyWhich;
        
    }
    
    private void sendAccessibilityEvent() {
        AccessibilityManager am =
            (AccessibilityManager) mActivity.getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled() || mModel == null) {
            return;
        }
        StringBuilder b = new StringBuilder();
        //addFieldsRecursive(b, mView);
        CharSequence msg = b.toString();

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(getClass().getName());
        event.setPackageName(mActivity.getPackageName());
        event.getText().add(msg);
        event.setAddedCount(msg.length());

        am.sendAccessibilityEvent(event);
    }
    
    protected void setAllDayViewsVisibility(boolean isChecked) {
        if (isChecked) {
        	/*
            if (mEndTime.hour == 0 && mEndTime.minute == 0) {
                if (mAllDay != isChecked) {
                    mEndTime.monthDay--;
                }
                long endMillis = mEndTime.normalize(true);
                // Do not allow an event to have an end time
                // before the
                // start time.
                if (mEndTime.before(mStartTime)) {
                    mEndTime.set(mStartTime);
                    endMillis = mEndTime.normalize(true);
                }               
            }
			*/
            //TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTimeCalendar, isChecked);
        	TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTime, isChecked);
            startTimeText.makeTimeText();
            mStartTimerTimeText.setTextColor(Color.GRAY);
            mStartTimerTimeText.setText(startTimeText.getTimeText());
            
            setEndTimerTimeText(isChecked);            
        } else {
        	/*
            if (mEndTime.hour == 0 && mEndTime.minute == 0) {
                if (mAllDay != isChecked) {
                    mEndTime.monthDay++;
                }
                long endMillis = mEndTime.normalize(true);                
            }
            */        	
            //TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTimeCalendar, isChecked);
        	TimerPickerTimeText startTimeText = new TimerPickerTimeText(mStartTime, isChecked);
            startTimeText.makeTimeText();
            mStartTimerTimeText.setTextColor(Color.RED);
            mStartTimerTimeText.setText(startTimeText.getTimeText());
            
            setEndTimerTimeText(isChecked);             
        }

        // If this is a new event, and if availability has not yet been
        // explicitly set, toggle busy/available as the inverse of all day.
        if (mModel.mUri == null && !mAvailabilityExplicitlySet) {
            // Values are from R.arrays.availability_values.
            // 0 = busy
            // 1 = available
            int newAvailabilityValue = isChecked? 1 : 0;
            if (mAvailabilityAdapter != null && mAvailabilityValues != null
                    && mAvailabilityValues.contains(newAvailabilityValue)) {
                // We'll need to let the spinner's listener know that we're
                // explicitly toggling it.
                mAllDayChangingAvailability = true;

                String newAvailabilityLabel = mOriginalAvailabilityLabels.get(newAvailabilityValue);
                int newAvailabilityPos = mAvailabilityAdapter.getPosition(newAvailabilityLabel);
                //mAvailabilitySpinner.setSelection(newAvailabilityPos);
            }
        }

        mAllDay = isChecked;
        // timepicker expand ������ Ȯ���ϰ� 
        // timepicker�� expand �Ǿ� �ִٸ�,
        // timepicker�� switching �ؾ� �Ѵ�!!!
        if (mStartTimeExpended || mEndTimeExpended) {
        	switchingAllDayTimePickerView();
        }
        
    }
    
    private void setDate(TextView view, long millis) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH
                | DateUtils.FORMAT_ABBREV_WEEKDAY;

        // Unfortunately, DateUtils doesn't support a timezone other than the
        // default timezone provided by the system, so we have this ugly hack
        // here to trick it into formatting our time correctly. In order to
        // prevent all sorts of craziness, we synchronize on the TimeZone class
        // to prevent other threads from reading an incorrect timezone from
        // calls to TimeZone#getDefault()
        // TODO fix this if/when DateUtils allows for passing in a timezone
        String dateString;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(mTimeZone);
            dateString = DateUtils.formatDateTime(mActivity, millis, flags);
            // setting the default back to null restores the correct behavior
            TimeZone.setDefault(null);
        }
        view.setText(dateString);
    }
    
    private void addFieldsRecursive(StringBuilder b, View v) {
        if (v == null || v.getVisibility() != View.VISIBLE) {
            return;
        }
        if (v instanceof TextView) {
            CharSequence tv = ((TextView) v).getText();
            if (!TextUtils.isEmpty(tv.toString().trim())) {
                b.append(tv + PERIOD_SPACE);
            }
        } else if (v instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) v;
            int id = rg.getCheckedRadioButtonId();
            if (id != View.NO_ID) {
                b.append(((RadioButton) (v.findViewById(id))).getText() + PERIOD_SPACE);
            }
        } else if (v instanceof Spinner) {
            Spinner s = (Spinner) v;
            if (s.getSelectedItem() instanceof String) {
                String str = ((String) (s.getSelectedItem())).trim();
                if (!TextUtils.isEmpty(str)) {
                    b.append(str + PERIOD_SPACE);
                }
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int children = vg.getChildCount();
            for (int i = 0; i < children; i++) {
                addFieldsRecursive(b, vg.getChildAt(i));
            }
        }
    }
    
    
    /**
     * Loads an integer array asset into a list.
     */
    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<Integer>(size);

        for (int i = 0; i < size; i++) {
            list.add(vals[i]);
        }

        return list;
    }

    /**
     * Loads a String array asset into a list.
     */
    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }
    
    private void prepareAvailability() {
        //Resources r = mActivity.mResources;

        mAvailabilityValues = loadIntegerArray(mResources, R.array.availability_values);
        mAvailabilityLabels = loadStringArray(mResources, R.array.availability);
        // Copy the unadulterated availability labels for all-day toggling.
        mOriginalAvailabilityLabels = new ArrayList<String>();
        mOriginalAvailabilityLabels.addAll(mAvailabilityLabels);

        if (mModel.mCalendarAllowedAvailability != null) {
            EventViewUtils.reduceMethodList(mAvailabilityValues, mAvailabilityLabels,
                    mModel.mCalendarAllowedAvailability);
        }

        /*mAvailabilityAdapter = new ArrayAdapter<String>(mActivity, 
                android.R.layout.simple_spinner_item, mAvailabilityLabels);
        mAvailabilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAvailabilitySpinner.setAdapter(mAvailabilityAdapter);*/
    }
    
    LinearLayout mRemindersContainer;
    
    private void updateRemindersVisibility(int numReminders) {
    	/*
        if (numReminders == 0) {
            mRemindersContainer.setVisibility(View.GONE);
        } else {
            mRemindersContainer.setVisibility(View.VISIBLE);
        }
        */
    }

    /**
     * Add a new reminder when the user hits the "add reminder" button.  We use the default
     * reminder time and method.
     */
    private void addReminder() {
        // TODO: when adding a new reminder, make it different from the
        // last one in the list (if any).
        if (mDefaultReminderMinutes == SettingsFragment.NO_REMINDER) {
            /*EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                    mReminderMinuteValues, mReminderMinuteLabels,
                    mReminderMethodValues, mReminderMethodLabels,
                    ReminderEntry.valueOf(SettingsFragment.REMINDER_DEFAULT_TIME),
                    mModel.mCalendarMaxReminders, null);*/
        } else {
            /*EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                    mReminderMinuteValues, mReminderMinuteLabels,
                    mReminderMethodValues, mReminderMethodLabels,
                    ReminderEntry.valueOf(mDefaultReminderMinutes),
                    mModel.mCalendarMaxReminders, null);*/
        }
        updateRemindersVisibility(mReminderItems.size());
        //EventViewUtils.updateAddReminderButton(mView, mReminderItems, mModel.mCalendarMaxReminders);
    }
    
    private void initReminders() {
        //CalendarEventModel model = mModel;
        //Resources r = mActivity.mResources;

        // Load the labels and corresponding numeric values for the minutes and methods lists from the assets.  
        // If we're switching calendars, we need to clear and re-populate the lists (which may have elements added and removed based on calendar properties).  
        // This is mostly relevant for "methods", 
        // since we shouldn't have any "minutes" values in a new event that aren't in the default set.
        //mReminderMinuteValues = loadIntegerArray(r, R.array.ecalendar_default_reminder_values);
        ArrayList<String> reminderMinuteValues = loadStringArray(mResources, R.array.ecalendar_default_reminder_values);
        if (mReminderMinuteValues == null)
        	mReminderMinuteValues = new ArrayList<Integer>();        	
        else
        	mReminderMinuteValues.clear();
        
        int arraySize = reminderMinuteValues.size();
        for (int i=0; i<arraySize; i++) {
        	String value = reminderMinuteValues.get(i);      	
        	int transformInt = Integer.parseInt(value);
        	Integer v = new Integer(transformInt);
        	mReminderMinuteValues.add(v);
        }        

        mReminderMinuteLabels = loadStringArray(mResources, R.array.ecalendar_default_reminder_labels);
        mReminderMethodValues = loadIntegerArray(mResources, R.array.reminder_methods_values);
        mReminderMethodLabels = loadStringArray(mResources, R.array.reminder_methods_labels);

        // Remove any reminder methods that aren't allowed for this calendar.  If this is
        // a new event, mCalendarAllowedReminders may not be set the first time we're called.
        // 
        if (mModel.mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mModel.mCalendarAllowedReminders);
        }

        // �츮�� iphone ó�� �� ���� �����δ��� �����Ѵ�!!!
        // :���� �����δ��� �� �� �̻��̶��?
        //  ->�̺�Ʈ ���� �ð��� ���� ����� ��, �� ���� ���� �� �� ���� �����Ѵ�        
        
        if (mModel.mHasAlarm) { 
            //ArrayList<ReminderEntry> reminders = mModel.mReminders; // !!!!!!!!!!!!!!!!!!
        	mReminderPageView.mReminderEntries = (ArrayList<ReminderEntry>) mModel.mReminders.clone();
            int numReminders = mReminderPageView.mReminderEntries.size();
            if (numReminders > 1) {            	
            	Collections.sort(mReminderPageView.mReminderEntries);
            	Collections.reverse(mReminderPageView.mReminderEntries); // �ᱹ ������������ ���� �Ǿ���
            }
            
            /*
            // Insert any minute values that aren't represented in the minutes list.
            for (ReminderEntry re : reminders) {
                if (mReminderMethodValues.contains(re.getMethod())) {
                    EventViewUtils.addMinutesToList(mActivity, mReminderMinuteValues,
                            mReminderMinuteLabels, re.getMinutes());
                }
            }
			*/
            // Create a UI element for each reminder.  
            // We display all of the reminders we get from the provider, 
            // even if the count exceeds the calendar maximum.  
            // (Also, for a new event, we won't have a maxReminders value available.)
            mUnsupportedReminders.clear();
            for (ReminderEntry re : mReminderPageView.mReminderEntries) {
                if (mReminderMethodValues.contains(re.getMethod()) || re.getMethod() == Reminders.METHOD_DEFAULT) {
                	int test = -1; test = 0;
                    /*EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                            mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                            mReminderMethodLabels, re, Integer.MAX_VALUE, null);*/
                } else {
                    // TODO figure out a way to display unsupported reminders
                    mUnsupportedReminders.add(re);
                }
            }
            
            mUnsupportedReminderMinutes.clear();
            for (ReminderEntry re : mReminderPageView.mReminderEntries) {
            	if (mReminderMethodValues.contains(re.getMethod())) {
	                if (!mReminderMinuteValues.contains(re.getMinutes())) {
	                	mUnsupportedReminderMinutes.add(re);
	                } 
	                
	                //EventViewUtils.addMinutesToList(mActivity, mReminderMinuteValues,
                            //mReminderMinuteLabels, re.getMinutes());
            	}
            }
            
            /*if (mUnsupportedReminderMinutes.size() > 0) {        
            	//mFragment.mReminderPageView.mExistUnSupportedReminder = true;
            	mFragment.mReminderPageView.makeUnSupportedReminders(mUnsupportedReminderMinutes);            	
            }*/
            
                   
            int firstReminderMinutes = mReminderPageView.mReminderEntries.get(0).getMinutes();    
            boolean unsupportedReminderMinutes = false;
            if (mReminderMinuteValues.contains(firstReminderMinutes)) {           	
            	setSupportedReminderText(firstReminderMinutes, mEventFirstReminderText);            	
            } 
            else {            	         
            	unsupportedReminderMinutes = true;
            	mEventFirstReminderText.setText(String.valueOf(firstReminderMinutes));	
            	            	
            	TextView tv = (TextView) mReminderPageView.mUnsupportedReminderLayout.findViewById(R.id.unsupported_reminder_text);
            	tv.setText(String.valueOf(firstReminderMinutes));
            	mReminderPageView.mUnsupportedReminderLayout.setVisibility(View.VISIBLE);
            }            
            
            mBetweenFristReminderAndSecondReminderLayout.setVisibility(View.VISIBLE);
    		mSecondReminderLayout.setVisibility(View.VISIBLE);    
    		
            if (numReminders > 1) {   
            	
            	int secondReminderMinutes = mReminderPageView.mReminderEntries.get(1).getMinutes();   
            	//mFragment.mReminderPageView.mSecondReminderMinutes = secondReminderMinutes;
            	
            	if (mReminderMinuteValues.contains(secondReminderMinutes)) {            		
                	setSupportedReminderText(secondReminderMinutes, mEventSecondReminderText);                	
                	//mFragment.mReminderPageView.mSecondReminderValue = selectedReminderValue;                 	
                } 
                else {
                	//mFragment.mReminderPageView.mSecondReminderValue = EditEventReminderSettingSubView.EVENT_UNSUPPORTED_REMINDER;                	
                	mEventSecondReminderText.setText(String.valueOf(secondReminderMinutes));	
                }
            }
            else {
            	//mFragment.mReminderPageView.mSecondReminderValue = EditEventReminderSettingSubView.EVENT_NO_REMINDER;
            	//mFragment.mReminderPageView.mSecondReminderMinutes = -1; 
            	mEventSecondReminderText.setText(R.string.event_reminder_no);           	           	
            }            
            
            //int firstReminderMinutes = mFragment.mReminderPageView.mReminderEntries.get(0).getMinutes();            
            mReminderPageView.attachReminderPageItemSelectionIcon(firstReminderMinutes, unsupportedReminderMinutes);              
        }
        else {
        	//mFragment.mReminderPageView.mFirstReminderValue = EditEventReminderSettingSubView.EVENT_NO_REMINDER;
        	//mFragment.mReminderPageView.mSecondReminderValue = EditEventReminderSettingSubView.EVENT_NO_REMINDER;
        	mReminderPageView.attachReminderPageItemSelectionIcon(EditEventReminderSettingSubView.EVENT_NO_REMINDER_MINUTES, false);
        }
        
        mReminderPageView.mSelectedFirstReminderItemLayoutOfMainPage = true;
    }
    
    public int setSupportedReminderText(int reminderMinutes, TextView reminderTextView) {
    	int selectedReminderValue = -1;
    	switch(reminderMinutes) {
    	case -1:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_NO_REMINDER;   
    		reminderTextView.setText(R.string.event_reminder_no);	
    		break;
    	case 0:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_EVENT_THEN_REMINDER;  
    		reminderTextView.setText(R.string.event_reminder_eventthen);	
    		break;
    	case 5:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_5MIN_REMINDER; 
    		reminderTextView.setText(R.string.event_reminder_before_5min);	
    		break;
    	case 15:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_15MIN_REMINDER;   
    		reminderTextView.setText(R.string.event_reminder_before_15min);	
    		break;
    	case 30:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_30MIN_REMINDER; 
    		reminderTextView.setText(R.string.event_reminder_before_30min);	
    		break;
    	case 60:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_1H_REMINDER;   
    		reminderTextView.setText(R.string.event_reminder_before_1hour);	
    		break;
    	case 120:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_2H_REMINDER; 
    		reminderTextView.setText(R.string.event_reminder_before_2hour);	
    		break;
    	case 1440:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_1DAY_REMINDER;  
    		reminderTextView.setText(R.string.event_reminder_before_1day);	
    		break;
    	case 2880:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_2DAY_REMINDER; 
    		reminderTextView.setText(R.string.event_reminder_before_2day);	
    		break;
    	case 10080:
    		selectedReminderValue = EditEventReminderSettingSubView.EVENT_BEFORE_1WEEK_REMINDER; 
    		reminderTextView.setText(R.string.event_reminder_before_1week);	
    		break;
    	}
    	
    	return selectedReminderValue;
    }
    
    
    
    public class AttendeePageBroadcaster implements Animation.AnimationListener {
        

        public AttendeePageBroadcaster() {
            
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        	showIME();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }
    
    
    public void showIME() {    	
    	//m_inputMM.setInputMethodAndSubtype(token, id, subtype);
    	//m_inputMM.setInputMethod(token, id);    	
    	//List<InputMethodInfo> lists = m_inputMM.getEnabledInputMethodList();    	
    	//int attendeeViewId = mAttendeePageView.getId();
    	//int currentViewId = mViewSwitcher.getCurrentView().getId();
    	//if (attendeeViewId == currentViewId) 
    		//Log.i("tag", "showIME : SAME VIEW!!!");
    	
    	if (!mViewSwitcher.getCurrentView().requestFocus())
    		Log.i("tag", "showIME : not FOCUS!!!");
    	
    	m_inputMM.showSoftInput(mAttendeePageView, 0);
    	//m_inputMM.showSoftInput(mViewSwitcher.getCurrentView(), 0);
    	//m_inputMM.showSoftInput(mViewSwitcher.getCurrentView(), InputMethodManager.SHOW_FORCED);
    	
		//mAttendeePageView.setOnClickListener(l);
	}
	
	public void hideIME() {
		m_inputMM.hideSoftInputFromWindow(mAttendeePageView.getWindowToken(), 0);		
	}
	
	public static class RepeatEndTime {
		public static final int YEAR_FIELD = 0;
		public static final int MONTH_FIELD = 1;
		public static final int DAYOFMONTH_FIELD = 2;		
		
		int mYear;
		int mMonth;
		int mDayOfMonth;
		boolean mReseted;
		
		public RepeatEndTime() {
			mYear = 0;
			mMonth = 0;
			mDayOfMonth = 0;
		}
		
		public RepeatEndTime(int year, int month, int dayOfMonth, boolean reseted) {
			mYear = year;
			mMonth = month;
			mDayOfMonth = dayOfMonth;			
			mReseted = reseted;
		}
		
		public void set(int field, int value) {
			switch(field) {
			case YEAR_FIELD:
				mYear = value;
				break;
			case MONTH_FIELD:
				mMonth = value;
				break;
			case DAYOFMONTH_FIELD:
				mDayOfMonth = value;
				break;				
			default:
				break;
			}
		}		
		
		public void set(int year, int month, int dayOfMonth) {
			mYear = year;
			mMonth = month;
			mDayOfMonth = dayOfMonth;			
		}
		
		public void setRepeatEndDate(int year, int month, int dayOfMonth, Calendar cal, boolean reseted) {
			mYear = year;
			mMonth = month;
			mDayOfMonth = dayOfMonth;			
			mReseted = reseted;
		}
		
		public RepeatEndTime get() {
			return this;
		}
		
		public int get(int field) {
			int value = -1;
			
			switch(field) {
			case YEAR_FIELD:
				value = mYear;
				break;
			case MONTH_FIELD:
				value = mMonth;
				break;
			case DAYOFMONTH_FIELD:
				value = mDayOfMonth;
				break;				
			default:
				break;
			}
			
			return value;
		}
		
		
		public boolean isReseted() {
			return mReseted;
		}
		
		public boolean IsLeapYear(int year) {
			boolean leapYear = false;			
			
			int resultBy4Divide = year % 4;
			if (resultBy4Divide == 0) {
				leapYear = true;
				
				int resultBy100Divide = year % 100;
				if (resultBy100Divide == 0) {
					leapYear = false;
					int resultBy400Divide = year % 400;
					if (resultBy400Divide == 0)
						leapYear = true;
				}
			}			
			
			return leapYear;
		}
		
		public int getActualMaximumOfMonth(int year, int month) {
			int max = -1;
			switch(month) {
			case 0:   //1��
				max = 31;
				break;
			case 1:   //2��
				if(IsLeapYear(year)) 
					max = 29;			
				else
					max = 28;			
				break;
			case 2:   //3��
				max = 31;
				break;
			case 3:   //4��
				max = 30;
				break;
			case 4:   //5��
				max = 31;
				break;
			case 5:   //6��
				max = 30;
				break;
			case 6:   //7��
				max = 31;
				break;
			case 7:   //8��
				max = 31;
				break;
			case 8:   //9��
				max = 30;
				break;
			case 9:   //10��
				max = 31;
				break;
			case 10:  //11��
				max = 30;
				break;
			case 11:  //12��
				max = 31;
				break;
			default:
				break;
			}
			
			return max;
		}
		
		public void checkValidTime() {
			int maxDate = getActualMaximumOfMonth(mYear, mMonth);
			if (mDayOfMonth > maxDate)
				mDayOfMonth = maxDate;
		}		
	}
	
	
	public long getStartTimeMills() {
		return mStartTime.getTimeInMillis();
	}
	
	public String getTzId() {
		return mTimeZone.getID();
	}
	
	/*public void setSelectedCalendar(int selectedCalendarId) {
		if (selectedCalendarId != -1) {
			mCalendarID = selectedCalendarId;
		}
		
        // This is only used for the Calendar spinner in new events, and only fires when the
        // calendar selection changes or on screen rotation  
    	mCalendarsCursor.moveToPosition(-1);
        while(mCalendarsCursor.moveToNext()) {
        	int calendarId = mCalendarsCursor.getInt(mCalendarIDColumnIndex);
        	if (calendarId == mCalendarID)
        		break;
        }
        
    	Cursor c = mCalendarsCursor;
    	
        // Do nothing if the selection didn't change so that reminders will not get lost
        int idColumn = c.getColumnIndexOrThrow(Calendars._ID);
        long calendarId = c.getLong(idColumn);
        int colorColumn = c.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
        int color = c.getInt(colorColumn);
        int displayColor = Utils.getDisplayColorFromColor(color);

        // Prevents resetting of data (reminders, etc.) on orientation change.
        if (calendarId == mModel.mCalendarId && mModel.isCalendarColorInitialized() &&
                displayColor == mModel.getCalendarColor()) {
            return;
        }

        //setSpinnerBackgroundColor(displayColor);

        mModel.mCalendarId = calendarId;
        mModel.setCalendarColor(displayColor);
        mModel.mCalendarAccountName = c.getString(EditEventHelper.CALENDARS_INDEX_ACCOUNT_NAME);
        mModel.mCalendarAccountType = c.getString(EditEventHelper.CALENDARS_INDEX_ACCOUNT_TYPE);
        mModel.setEventColor(mModel.getCalendarColor());

        //setColorPickerButtonStates(mModel.getCalendarEventColors());

        //int a = Reminders.METHOD_DEFAULT; // method�� 0 ���� ������ �ǹ��ϴ°�???
        // Update the max/allowed reminders with the new calendar properties.
        int maxRemindersColumn = c.getColumnIndexOrThrow(Calendars.MAX_REMINDERS);
        mModel.mCalendarMaxReminders = c.getInt(maxRemindersColumn);
        int allowedRemindersColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_REMINDERS);
        mModel.mCalendarAllowedReminders = c.getString(allowedRemindersColumn);
        int allowedAttendeeTypesColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_ATTENDEE_TYPES);
        mModel.mCalendarAllowedAttendeeTypes = c.getString(allowedAttendeeTypesColumn);
        int allowedAvailabilityColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_AVAILABILITY);
        mModel.mCalendarAllowedAvailability = c.getString(allowedAvailabilityColumn);

        // Discard the current reminders and replace them with the model's default reminder set.
        // We could attempt to save & restore the reminders that have been added, but that's
        // probably more trouble than it's worth.
        mModel.mReminders.clear();
        //mModel.mReminders.addAll(mModel.mDefaultReminders);
        //mModel.mHasAlarm = mModel.mReminders.size() != 0;
        mModel.mHasAlarm = false;

        // Update the UI elements.
        
        prepareReminders();
        prepareAvailability();
    }*/
	/*
	private int findDefaultCalendarID(Cursor calendarsCursor) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }      
        
        // defaultCalendarId ������ -1�� ���ͼ��� �ȵȴ�...
        // -1���� ���� ���,,,emergency state[���ڱ� ���͸��� ������ �����ǰų� ���͸��� Ż���ȴٰų� �׷� ��Ȳ?]���� ������ ������ ���������� �̷������ ���� ��Ȳ?
        int defaultCalendarId = Utils.getSharedPreference(
                mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, -1);        
        
        String defaultCalendar = Utils.getSharedPreference(
                mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR, SettingsFragment.DEFAULT_CALENDAR);
        
        if (defaultCalendarId == -1) {
	        int calendarIdIndex = calendarsCursor.getColumnIndexOrThrow(Calendars._ID);
	        int calendarsOwnerIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
	        //int accountNameIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
	        //int accountTypeIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);
	        
	        calendarsCursor.moveToPosition(-1);
	        while (calendarsCursor.moveToNext()) {
	            String calendarOwner = calendarsCursor.getString(calendarsOwnerIndex);
	            if (defaultCalendar.equals(calendarOwner)) {
	                // Found the default calendar.
	            	defaultCalendarId = calendarsCursor.getInt(calendarIdIndex);
	            	Utils.setSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, defaultCalendarId);
	            		            	
	                return defaultCalendarId;
	            }            
	        }
	        
	        // defalut calendar�� �����ؾ� �Ѵ�!!!
	        defaultCalendarId = makeDefaultECalendar();
        }
        
        return defaultCalendarId;
    }
	*/
	
	private int makeDefaultECalendar() {
		int defaultCalendarId = -1;
		
		ContentResolver cr = mActivity.getContentResolver();
		String defaultDisplayName = SettingsFragment.DEFAULT_CALENDAR_DISPLAYNAME;
		
		Uri syncAdapterUri = buildQueryDefaultCalendarUri();
		// Add calendar
		final ContentValues cv = buildInsertingNewCalendarContentValues(defaultDisplayName, Color.RED);		
		
		// �ش� URI�� Ķ������ �����Ϸ��� �Ѵٸ�, ���� ���
		// cv.put(Calendars.MAX_REMINDERS, 2) �׸��� �����ϸ�,
		// Only sync adapters may write to maxReminders ��� ������ �߻��Ѵ�
		// :maxReminders�� SyncAdapter�� writable �� �� �ִ� �ݷ��̴�
		// �׷��Ƿ� CalendarContract.CALLER_IS_SYNCADAPTER, "true" �� ����� URI�� �����ؾ� �Ѵ�
		//Uri resultUri = cr.insert(CalendarContract.Calendars.CONTENT_URI, cv); 		
		Uri resultUri = cr.insert(syncAdapterUri, cv);
		
		if (resultUri == null)
			throw new IllegalArgumentException();	
		
		final String[] projection = {Calendars._ID};		
		Cursor cursor = cr.query(buildQueryDefaultCalendarUri(), projection, null, null, null);
		try {
			if (cursor == null || !cursor.moveToFirst()) {
				//Log.e(Constants.TAG, "Query is empty after insert! AppOps disallows access to read or write calendar?");
				throw new IllegalArgumentException();
			}
			else {
				cursor.moveToPosition(-1);
		        while (cursor.moveToNext()) {
		        	defaultCalendarId = cursor.getInt(cursor.getColumnIndexOrThrow(Calendars._ID));
		        	if (defaultCalendarId != -1) {
		        		Utils.setSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, defaultCalendarId);
		        		return defaultCalendarId;
		        	}
		        }
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		
		return defaultCalendarId;				
	}	
	
	
	private Uri buildQueryDefaultCalendarUri() {
        return CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")                
                .appendQueryParameter(Calendars.ACCOUNT_NAME, EditEventCalendarSettingSubView.ECALENDAR_ACCOUNT_NAME)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, EditEventCalendarSettingSubView.ECALENDAR_ACCOUNT_TYPE).build();
    }

    private ContentValues buildInsertingNewCalendarContentValues(String displayName, int color) {
    	/* 
    	*When inserting a new calendar the following fields must be included:
    	-ACCOUNT_NAME
		-ACCOUNT_TYPE
		-NAME
		-CALENDAR_DISPLAY_NAME
		-CALENDAR_COLOR
		-CALENDAR_ACCESS_LEVEL
		-OWNER_ACCOUNT
		
		*The following Calendar columns are writable by both an app and a sync adapter.
		-NAME
		-CALENDAR_DISPLAY_NAME
		-VISIBLE
		-SYNC_EVENTS
		
		* If a local calendar is required an app can do so by inserting as a sync adapter and using an ACCOUNT_TYPE of ACCOUNT_TYPE_LOCAL.
    	*/
        String intName = EditEventCalendarSettingSubView.ECALENDAR_INT_NAME_PREFIX + displayName;
        final ContentValues cv = new ContentValues();
        cv.put(Calendars.ACCOUNT_NAME, EditEventCalendarSettingSubView.ECALENDAR_ACCOUNT_NAME);
        cv.put(Calendars.ACCOUNT_TYPE, EditEventCalendarSettingSubView.ECALENDAR_ACCOUNT_TYPE);
        cv.put(Calendars.NAME, intName);
        cv.put(Calendars.CALENDAR_DISPLAY_NAME, displayName);
        cv.put(Calendars.CALENDAR_COLOR, color);
        cv.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER); //The level of access that the USER has for the calendar
        cv.put(Calendars.OWNER_ACCOUNT, EditEventCalendarSettingSubView.ECALENDAR_OWNER_ACCOUNT);
        cv.put(Calendars.MAX_REMINDERS, 2); 
        cv.put(Calendars.ALLOWED_REMINDERS, "0,1,2"); 
        cv.put(Calendars.VISIBLE, 1);
        cv.put(Calendars.SYNC_EVENTS, 1); // ��Ȯ�� �뵵�� �𸣰ڴ�:calendars ���̺�� events ���̺��� sync�� �ǹ��ϴ� ����???
                                          // �ƴϸ�,,,����̽��� web server���� sync�� �ǹ��ϴ°�����...
        								  // �Ｚ Ķ������ �� ���� 1�� �����Ͽ���
                                          // Is this calendar synced and are its events stored on the device? 
                                          // :calendar ���̺� �ִ� �ش� ���̺� ������ events ���̺� �ִ� �ش� ���̺� �������� ��ũ�� �ǹ��ϴ� �� ����! 
        
        String a = Calendars.ALLOWED_ATTENDEE_TYPES;
        return cv;
    }
	
}
