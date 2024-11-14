/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intheeast.day;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.EllipsizeCallback;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.intheeast.acalendar.AsyncQueryService;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarData;
import com.intheeast.acalendar.CalendarViewsSecondaryActionBar;
import com.intheeast.acalendar.DeleteEventHelper;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.OtherPreferences;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * View for multi-day view. So far only 1 and 7 day have been tested.
 */
public class DayView extends LinearLayout /*implements View.OnClickListener*/ {
    private static String TAG = "DayView";    
    private static boolean INFO = true;
    private static boolean DEBUG_SCALING = false;
    private static final String PERIOD_SPACE = ". ";

    private static float mScale = 0; // Used for supporting different screen densities
    private static final long INVALID_EVENT_ID = -1; //This is used for remembering a null event
    // Duration of the allday expansion
    private static final long ANIMATION_DURATION = 400;
    // duration of the more allday event text fade
    private static final long ANIMATION_SECONDARY_DURATION = 200;
    // duration of the scroll to go to a specified time
    private static final int GOTO_SCROLL_DURATION = 200;
    // duration for events' cross-fade animation
    private static final int EVENTS_CROSS_FADE_DURATION = 400;
    // duration to show the event clicked
    private static final int CLICK_DISPLAY_DURATION = 50;

    private static final int MENU_AGENDA = 2;
    private static final int MENU_DAY = 3;
    private static final int MENU_EVENT_VIEW = 5;
    private static final int MENU_EVENT_CREATE = 6;
    private static final int MENU_EVENT_EDIT = 7;
    private static final int MENU_EVENT_DELETE = 8;

    public static int DEFAULT_CELL_HEIGHT = 64;
    private static int MAX_CELL_HEIGHT = 150;
    private static int MIN_Y_SPAN = 100;

    private boolean mOnFlingCalled;
    private boolean mStartingScroll = false;
    protected boolean mPaused = true;
    private Handler mHandler;
    /**
     * ID of the last event which was displayed with the toast popup.
     *
     * This is used to prevent popping up multiple quick views for the same event, especially
     * during calendar syncs. This becomes valid when an event is selected, either by default
     * on starting calendar or by scrolling to an event. It becomes invalid when the user
     * explicitly scrolls to an empty time slot, changes views, or deletes the event.
     */
    private long mLastPopupEventID;

    protected Context mContext;
    protected Activity mActivity;

    private static final String[] CALENDARS_PROJECTION = new String[] {
        Calendars._ID,          // 0
        Calendars.CALENDAR_ACCESS_LEVEL, // 1
        Calendars.OWNER_ACCOUNT, // 2
    };
    private static final int CALENDARS_INDEX_ACCESS_LEVEL = 1;
    private static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    private static final String CALENDARS_WHERE = Calendars._ID + "=%d";

    private static final int FROM_NONE = 0;
    private static final int FROM_ABOVE = 1;
    private static final int FROM_BELOW = 2;
    private static final int FROM_LEFT = 4;
    private static final int FROM_RIGHT = 8;

    private static final int ACCESS_LEVEL_NONE = 0;
    private static final int ACCESS_LEVEL_DELETE = 1;
    private static final int ACCESS_LEVEL_EDIT = 2;

    private static int mHorizontalSnapBackThreshold = 128;

    private final ContinueScroll mContinueScroll = new ContinueScroll();

    // Make this visible within the package for more informative debugging
    public Calendar mBaseDate;
    private Calendar mCurrentTime;
    //Update the current time line every five minutes if the window is left open that long
    
    private final UpdateCurrentTime mUpdateCurrentTime = new UpdateCurrentTime();
    private int mTodayJulianDay;

    private final Typeface mBold = Typeface.DEFAULT_BOLD;
    public int mFirstJulianDay;
    //private int mLoadedFirstJulianDay = -1;
    private int mLastJulianDay;

    private int mMonthLength;
    private int mFirstVisibleDate;
    private int mFirstVisibleDayOfWeek;
    private int[] mEarliestStartHour;    // indexed by the week day offset
    //private boolean[] mHasAllDayEvent;   // indexed by the week day offset
    private String mEventCountTemplate;
    private final CharSequence[] mLongPressItems;
    private String mLongPressTitle;
    //private Event mClickedEvent;           // The event the user clicked on
    private Event mSavedClickedEvent;
    private static int mOnDownDelay;
    //private int mClickedYLocation;
    private long mDownTouchTime;

    private int mEventsAlpha = 255;
    private ObjectAnimator mEventsCrossFadeAnimation;

    protected static StringBuilder mStringBuilder = new StringBuilder(50);
    // TODO recreate formatter when locale changes
    protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            String tz = Utils.getTimeZone(mContext, this);
            mTimeZone = TimeZone.getTimeZone(tz);
            mBaseDate.setTimeZone(mTimeZone);            
            mCurrentTime.setTimeZone(mTimeZone);
            invalidate();
        }
    };
    
    // Sets the "clicked" color from the clicked event
    private final Runnable mSetClick = new Runnable() {
        @Override
        public void run() {
                //mClickedEvent = mSavedClickedEvent;
                mSavedClickedEvent = null;
                DayView.this.invalidate();
        }
    };

    
    // Clears the "clicked" color from the clicked event and launch the event
    /*
    private final Runnable mClearClick = new Runnable() {
        @Override
        public void run() {
            if (mClickedEvent != null) {
                mController.sendEventRelatedEvent(this, EventType.VIEW_EVENT, mClickedEvent.id,
                        mClickedEvent.startMillis, mClickedEvent.endMillis,
                        DayView.this.getWidth() / 2, mClickedYLocation,
                        getSelectedTimeInMillis());
            }
            mClickedEvent = null;
            DayView.this.invalidate(); 
        }
    };
	*/
    
    private final TodayAnimatorListener mTodayAnimatorListener = new TodayAnimatorListener();

    class TodayAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mFadingIn = false;

        @Override
        public void onAnimationEnd(Animator animation) {
            synchronized (this) {
                if (mAnimator != animation) {
                    animation.removeAllListeners();
                    animation.cancel();
                    return;
                }
                if (mFadingIn) {
                    if (mTodayAnimator != null) {
                        mTodayAnimator.removeAllListeners();
                        mTodayAnimator.cancel();
                    }
                    mTodayAnimator = ObjectAnimator
                            .ofInt(DayView.this, "animateTodayAlpha", 255, 0);
                    mAnimator = mTodayAnimator;
                    mFadingIn = false;
                    mTodayAnimator.addListener(this);
                    mTodayAnimator.setDuration(600);
                    mTodayAnimator.start();
                } else {
                    mAnimateToday = false;
                    mAnimateTodayAlpha = 0;
                    mAnimator.removeAllListeners();
                    mAnimator = null;
                    mTodayAnimator = null;
                    invalidate();
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

    AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            mScrolling = true;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mScrolling = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mScrolling = false;
            resetSelectedHour();
            invalidate();
        }
    };

    /**
     * This variable helps to avoid unnecessarily reloading events by keeping
     * track of the start millis parameter used for the most recent loading
     * of events.  If the next reload matches this, then the events are not
     * reloaded.  To force a reload, set this to zero (this is set to zero
     * in the method clearCachedEvents()).
     */
    //private long mLastReloadMillis;

    private ArrayList<Event> mEvents = new ArrayList<Event>();
    
    private int mSelectionDay;        // Julian day
    private int mSelectionStartHour;
    private int mSelectionStartMinutes;
    
    private int mSelectionEndHour;
    private int mSelectionEndMinutes;
    
    private int mSelectedEventRectHeight;

    //boolean mSelectionAllday;

    // Current selection info for accessibility
    private int mSelectionDayForAccessibility;        // Julian day
    private int mSelectionStartHourForAccessibility;
    private Event mSelectedEventForAccessibility;
    // Last selection info for accessibility
    private int mLastSelectionDayForAccessibility;
    private int mLastSelectionHourForAccessibility;
    private Event mLastSelectedEventForAccessibility;


    /** Width of a day or non-conflicting event */
    private int mCellWidth;

    // Pre-allocate these objects and re-use them
    private final Rect mRect = new Rect();
    private final Rect mDestRect = new Rect();
    private final Rect mSelectionRect = new Rect();
    // This encloses the more allDay events icon
    //private final Rect mExpandAllDayRect = new Rect();
    // TODO Clean up paint usage
    private final Paint mPaint = new Paint();
    private final Paint mEventTextPaint = new Paint();
    private final Paint mSelectionPaint = new Paint();
    private float[] mLines;

    private int mFirstDayOfWeek; // First day of the week

    //private PopupWindow mPopup;
    //private View mPopupView;

    // The number of milliseconds to show the popup window
    //private static final int POPUP_DISMISS_DELAY = 3000;
    //private final DismissPopup mDismissPopup = new DismissPopup();

    //private boolean mRemeasure = true;
    public boolean mRemeasure = false;

    //private final EventLoader mEventLoader;
    //private final EventInfoLoader mEventInfoLoader;    
    
    //ArrayList<EventCursors> mEventCursors = new ArrayList<EventCursors>();
    /*
    Runnable mEventInfoLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	boolean empty = mEventCursors.isEmpty();
        	if (empty)
        		Log.i("tag", "mEventInfoLoaderSuccessCallback:what???");
        	else {
        		Log.i("tag", "mEventInfoLoaderSuccessCallback:SUCCESS");
        		// ���⼭ �츮�� selected event�� id�� mEventCursors�� �ִ� ���� Ȯ���Ѵٸ�...
        		if (mFragment.getSelectionMode() == DayView.SELECTION_EVENT_FLOATING) {
        			// �츮�� ���� mSelectedEvents�� ��Ȯ�ϰ� update�ϰ� ���� �ʴ�.
        			// ������ mSelectedEvents�� clear�ϰ�
        			// mSelectedEvnet �� ���� add�ϰ� �ִ�
        			// :���� ��Ȯ�� mSelectedEvents�� �뵵�� �𸣰� �ִ�
        			//  �̴� allday ��Ʈ�� �����ϸ鼭 ��Ȯ�� �ľ��� �� �ۿ� ����
        			long targetEventId = mFragment.getSelectedEvent().id;
        			int numEvents = mEvents.size();
        			for (int i = 0; i < numEvents; i++) {
        	            Event event = mEvents.get(i);
        	            
        	            if (targetEventId == event.id) {
        	            	if (mFragment.mSelectedEventRequiredUpdate)
        	            		mFragment.mSelectedEventUpdateCompletion = true;
        	            	
        	            	DayView.this.mSelectedEvents.clear();
        	            	DayView.this.mSelectedEvents.add(event);
        	            	mFragment.setSelectedEvent(event);        	            	
        	            }       	            
        	        }
        		}
        	}
        }
    };
    */
    
    protected final EventGeometry mEventGeometry;

    private static int GRID_LINE_TOP_MARGIN = 0;
    private static int GRID_LINE_BOTTOM_MARGIN = 0;
    private static float GRID_LINE_LEFT_MARGIN = 0;
    //private static final float GRID_LINE_INNER_WIDTH = 1;// �̻��ϴ� HOUR_GAP�� 1�� �� �����Ѵٸ�, 0�� �Ǿ�߸� setStrokeWidth���� pixel 1 ������ �������ٵ�
    private static final float GRID_LINE_INNER_WIDTH = 0;
    
    public static final int DAY_GAP = 1;
    public static final int HOUR_GAP = 1;
    // This is the standard height of an allday event with no restrictions
    //private static int SINGLE_ALLDAY_HEIGHT = 34;
    /**
    * This is the minimum desired height of a allday event.
    * When unexpanded, allday events will use this height.
    * When expanded allDay events will attempt to grow to fit all
    * events at this height.
    */
    //private static float MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT = 28.0F; // in pixels
    /**
     * This is how big the unexpanded allday height is allowed to be.
     * It will get adjusted based on screen size
     */
    //private static int MAX_UNEXPANDED_ALLDAY_HEIGHT = (int) (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 4);
    /**
     * This is the minimum size reserved for displaying regular events.
     * The expanded allDay region can't expand into this.
     */
    private static int MIN_HOURS_HEIGHT = 180;
    //private static int ALLDAY_TOP_MARGIN = 1;
    // The largest a single allDay event will become.
    //private static int MAX_HEIGHT_OF_ONE_ALLDAY_EVENT = 34;   
    
    private static int HOURS_TOP_MARGIN = 2;
    private static int HOURS_LEFT_MARGIN = 2;
    public static int NEW_HOURS_LEFT_MARGIN = 8;
    private static int HOURS_RIGHT_MARGIN = 4;
    public static int NEW_HOURS_RIGHT_MARGIN = 8;
    private static int HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
    private static int NEW_EVENT_MARGIN = 4;
    private static int NEW_EVENT_WIDTH = 2;
    private static int NEW_EVENT_MAX_LENGTH = 16;

    private static int CURRENT_TIME_LINE_SIDE_BUFFER = 4;
    private static int CURRENT_TIME_LINE_TOP_OFFSET = 2;

    /* package */ static final int MINUTES_PER_HOUR = 60;
    /* package */ static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * 24;
    /* package */ static final int MILLIS_PER_MINUTE = 60 * 1000;
    /* package */ static final int MILLIS_PER_HOUR = (3600 * 1000);
    /* package */ static final int MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;

    // More events text will transition between invisible and this alpha
    private static final int MORE_EVENTS_MAX_ALPHA = 0x4C;
    private static int DAY_HEADER_ONE_DAY_LEFT_MARGIN = 0;
    private static int DAY_HEADER_ONE_DAY_RIGHT_MARGIN = 5;
    private static int DAY_HEADER_ONE_DAY_BOTTOM_MARGIN = 6;
    private static int DAY_HEADER_RIGHT_MARGIN = 4;
    private static int DAY_HEADER_BOTTOM_MARGIN = 3;
    private static float DAY_HEADER_FONT_SIZE = 14;
    private static float DATE_HEADER_FONT_SIZE = 32;
    private static float NORMAL_FONT_SIZE = 12;
    private static float EVENT_TEXT_FONT_SIZE = 12;
    private static float HOURS_TEXT_SIZE = 12;
    public static float AMPM_TEXT_SIZE = 9;
    private static int MIN_HOURS_WIDTH = 96;
    private static int MIN_CELL_WIDTH_FOR_TEXT = 20;
    private static final int MAX_EVENT_TEXT_LEN = 500;
    // smallest height to draw an event with
    private static float MIN_EVENT_HEIGHT = 24.0F; // in pixels
    private static int CALENDAR_COLOR_SQUARE_SIZE = 10;
    private static int EVENT_RECT_TOP_MARGIN = 1;
    private static int EVENT_RECT_BOTTOM_MARGIN = 0;
    private static int EVENT_RECT_LEFT_MARGIN = 1;
    private static int EVENT_RECT_RIGHT_MARGIN = 0;
    private static int EVENT_RECT_STROKE_WIDTH = 2;
    public static int EVENT_TEXT_TOP_MARGIN = 2;
    private static int EVENT_TEXT_BOTTOM_MARGIN = 2;
    public static int EVENT_TEXT_PAD_BETWEEN_LINES = 2;
    public static int EVENT_TEXT_LEFT_MARGIN = 6;
    private static int EVENT_TEXT_RIGHT_MARGIN = 6;
    //private static int ALL_DAY_EVENT_RECT_BOTTOM_MARGIN = 1;
    //private static int EVENT_ALL_DAY_TEXT_TOP_MARGIN = EVENT_TEXT_TOP_MARGIN;
    //private static int EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN = EVENT_TEXT_BOTTOM_MARGIN;
    //private static int EVENT_ALL_DAY_TEXT_LEFT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
    //private static int EVENT_ALL_DAY_TEXT_RIGHT_MARGIN = EVENT_TEXT_RIGHT_MARGIN;
    // margins and sizing for the expand allday icon
    //private static int EXPAND_ALL_DAY_BOTTOM_MARGIN = 10;
    // sizing for "box +n" in allDay events
    private static int EVENT_SQUARE_WIDTH = 10;
    private static int EVENT_LINE_PADDING = 4;
    private static int NEW_EVENT_HINT_FONT_SIZE = 12;

    //public final float ALLDAY_EVENT_RECT_HEIGHT_RATIO = 0.4f; // cell height�� 0.4
    
    
    private static int mPressedColor;
    private static int mClickedColor;
    private static int mEventTextColor;
    private static int mMoreEventsTextColor;

    private static int mWeek_saturdayColor;
    private static int mWeek_sundayColor;
    private static int mCalendarDateBannerTextColor;
    private static int mCalendarAmPmLabel;
    private static int mCalendarGridAreaSelected;
    private static int mCalendarGridLineInnerHorizontalColor;
    private static int mCalendarGridLineInnerVerticalColor;
    private static int mFutureBgColor;
    private static int mFutureBgColorRes;
    private static int mBgColor;
    public static int mNewEventHintColor;
    private static int mCalendarHourLabelColor;
    //private static int mMoreAlldayEventsTextAlpha = MORE_EVENTS_MAX_ALPHA;

    private float mAnimationDistance = 0;
    public int mViewStartX;
    public int mViewStartY;
    private int mMaxViewStartY;
    private int mViewHeight;
    private int mViewWidth;
    private int mGridAreaHeight = -1;
    private static int mCellHeight = 0; // shared among all DayViews
    //int mAllDayEventRectHeight = 0;
    private static int mOneQuartersOfHourPixelHeight = 0;
    private static int  mTwoQuartersOfHourPixelHeight = 0;
    private static int  mThreeQuartersOfHour = 0;
    private static int mMinCellHeight = 32;
    public int mScrollStartY;
    private int mPreviousDirection;
    private static int mScaledPagingTouchSlop = 0;

    /**
     * Vertical distance or span between the two touch points at the start of a
     * scaling gesture
     */
    private float mStartingSpanY = 0;
    /** Height of 1 hour in pixels at the start of a scaling gesture */
    //private int mCellHeightBeforeScaleGesture;
    /** The hour at the center two touch points */
    private float mGestureCenterHour = 0;

    //private boolean mRecalCenterHour = false;

    /**
     * Flag to decide whether to handle the up event. Cases where up events
     * should be ignored are 1) right after a scale gesture and 2) finger was
     * down before app launch
     */
    //private boolean mHandleActionUp = true;

    private int mHoursTextHeight;
    private int mRealHoursTextHeight;
    /**
     * The height of the area used for allday events
     */
    //private int mAlldayHeight;
    /**
     * The height of the allday event area used during animation
     */
    //private int mAnimateDayHeight = 0;
    /**
     * The height of an individual allday event during animation
     */
    //private int mAnimateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
    /**
     * Whether to use the expand or collapse icon.
     */
    //private static boolean mUseExpandIcon = true;
    /**
     * The height of the day names/numbers
     */
    private static int DAY_HEADER_HEIGHT = 45;
    /**
     * The height of the day names/numbers for multi-day views
     */
    private static int MULTI_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;
    /**
     * The height of the day names/numbers when viewing a single day
     */
    private static int ONE_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;
    
    
    private final int TWO_CENTIMETERS_TO_PIXELS;
    
    private final float TWO_CENTIMETERS_TO_INCHES = 0.787f;
    /**
     * Max of all day events in a given day in this view.
     */
    //private int mMaxAlldayEvents;
    /**
     * A count of the number of allday events that were not drawn for each day
     */
    //private int[] mSkippedAlldayEvents;
    /**
     * The number of allDay events at which point we start hiding allDay events.
     */
    //private int mMaxUnexpandedAlldayEventCount = 4;
    /**
     * Whether or not to expand the allDay area to fill the screen
     */
    //private static boolean mShowAllAllDayEvents = false;

    protected int mNumDays = 7;
    private int mNumHours = 10;

    /** Width of the time line (list of hours) to the left. */
    private int mMaxHourDecimalTextWidth;
    private int mHoursWidth;
    private int mAMPMTextHeight;
    
    private int mDateStrWidth;
    /** Top of the scrollable region i.e. below date labels and all day events */
    private int mFirstCell;
    /** First fully visibile hour */
    private int mFirstHour = -1;
    /** Distance between the mFirstCell and the top of first fully visible hour. */
    private int mFirstHourOffset;
    private String[] mHourStrs;
    private String[] mDayStrs;
    private String[] mDayStrs2Letter;
    private boolean mIs24HourFormat;

    public final ArrayList<Event> mSelectedEvents = new ArrayList<Event>();
    //public boolean mComputeSelectedEvents;
    //private boolean mUpdateToast;
    //private Event mSelectedEvent;
    //private Event mPrevSelectedEvent;
    private final Rect mPrevBox = new Rect();
    protected final Resources mResources;
    protected final Drawable mCurrentTimeLine;
    protected final Drawable mCurrentTimeAnimateLine;
    protected final Drawable mTodayHeaderDrawable;
    //protected final Drawable mExpandAlldayDrawable;
    //protected final Drawable mCollapseAlldayDrawable;
    protected Drawable mAcceptedOrTentativeEventBoxDrawable;
    private String mAmString;
    private String mPmString;
    private final DeleteEventHelper mDeleteEventHelper;
    private static int sCounter = 0;   

    //ScaleGestureDetector mScaleGestureDetector;

    /**
     * The initial state of the touch mode when we enter this view.
     */
    private static final int TOUCH_MODE_INITIAL_STATE = 0;

    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;

    /**
     * Indicates the touch gesture is a vertical scroll
     */
    private static final int TOUCH_MODE_VSCROLL = 0x20;

    /**
     * Indicates the touch gesture is a horizontal scroll
     */
    private static final int TOUCH_MODE_HSCROLL = 0x40;
    
    private static final int TOUCH_MODE_RETURN_IN_PLACE = 0x80;

    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;

    /**
     * The selection modes are HIDDEN, PRESSED, SELECTED, and LONGPRESS.
     */
    /*
    private static final int SELECTION_HIDDEN = 0;
    private static final int SELECTION_PRESSED = 1; // D-pad down but not up yet
    private static final int SELECTION_SELECTED = 2;
    private static final int SELECTION_LONGPRESS = 3;
    */
    
    public static final int SELECTION_HIDDEN = 0;
    public static final int SELECTION_NEW_EVENT = 1;
    public static final int SELECTION_NEW_EVENT_MOVED = 2; 
    public static final int SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED = 3;
    public static final int SELECTION_EVENT_PRESSED = 4; 
    public static final int SELECTION_EVENT_MOVED = 5; 
    public static final int SELECTION_EVENT_ADJUST_POSITION_AFTER_MOVED = 6; 
    public static final int SELECTION_EVENT_FLOATING = 7; 
    public static final int SELECTION_EVENT_EXPAND_SELECTION = 8; 
    public static final int SELECTION_EVENT_EXPANDING = 9; 
    public static final int SELECTION_EVENT_TIME_MODIFICATION_DB_POPUP_AFTER_EXPANDING = 10;
    public static final int SELECTION_EVENT_TIME_MODIFICATION_DB_POPUP_AFTER_MOVED = 11;
    
    public static final int SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT = 0; 
    public static final int SELECTION_EVENT_EXPANDING_LOWER_CIRCLEPOINT = 1; 
    
    public static final int SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE = 0;
    public static final int SELECTION_ACTION_MOVE_TOUCHEVENT = 1;
	public static final int SELECTION_ACTION_UP_TOUCHEVENT = 2;
    public static final int SELECTION_DOWN_GESTURE = 3;
	public static final int SELECTION_SINGLETAPUP_GESTURE = 4;
	public static final int SELECTION_LONGPRESS_GESTURE = 5;
	
	
    //boolean mReadyNewEvent = false;
    // mSelectionMode�� �뵵�� 
    // Ư�� �̺�Ʈ�� ���õǾ��� ���,
    // Ư�� �ð��븦 ����(�̺�Ʈ ������ ����?)�Ͽ��� ���
    // ���� ������ ���� �����Ѵ�    
    //private int mSelectionMode = SELECTION_HIDDEN;
    //public int mSelectionMode = SELECTION_HIDDEN;

    public boolean mScrolling = false;

    // Pixels scrolled
    private float mInitialScrollX;
    private float mInitialScrollY;

    private boolean mAnimateToday = false;
    private int mAnimateTodayAlpha = 0;

    // Animates the height of the allday region
    //ObjectAnimator mAlldayAnimator;
    // Animates the height of events in the allday region
    //ObjectAnimator mAlldayEventAnimator;
    // Animates the transparency of the more events text
    //ObjectAnimator mMoreAlldayEventsAnimator;
    // Animates the current time marker when Today is pressed
    ObjectAnimator mTodayAnimator;
    // whether or not an event is stopping because it was cancelled
    private boolean mCancellingAnimations = false;
    // tracks whether a touch originated in the allday area
    //private boolean mTouchStartedInAlldayArea = false;

    private final CalendarController mController;
    //private final ViewSwitcher mViewSwitcher;
    private final GestureDetector mGestureDetector;
    private final OverScroller mScroller;
    private final EdgeEffect mEdgeEffectTop;
    private final EdgeEffect mEdgeEffectBottom;
    private boolean mCallEdgeEffectOnAbsorb;
    private final int OVERFLING_DISTANCE;
    private float mLastVelocity;

    private final ScrollInterpolator mHScrollInterpolator;
    private AccessibilityManager mAccessibilityMgr = null;
    private boolean mIsAccessibilityEnabled = false;
    private boolean mTouchExplorationEnabled = false;
    private final String mCreateNewEventString;
    private final String mNewEventHintString;

    DayFragment mFragment;
    //private final AsyncQueryService mService;
    
    View mPsudoActionBarView;
    boolean mVisiblePsudoActionBarView;
    
    public DayView(Activity activity, CalendarController controller,
    		DayFragment fragment,    		
            int numDays,
            View psudoActionBarView) {
    	
        super(activity.getApplicationContext());
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mFragment = fragment;
        initAccessibilityVariables();
        
        setBackgroundColor(Color.argb(255, 255, 255, 255));
        
        mResources = mContext.getResources();
        mCreateNewEventString = mResources.getString(R.string.event_create);
        mNewEventHintString = mResources.getString(R.string.day_view_new_event_hint);
        mNumDays = numDays;
        mPsudoActionBarView = psudoActionBarView;
        mVisiblePsudoActionBarView = false;
        
        DATE_HEADER_FONT_SIZE = (int) mResources.getDimension(R.dimen.date_header_text_size);
        DAY_HEADER_FONT_SIZE = (int) mResources.getDimension(R.dimen.day_label_text_size);
        ONE_DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.one_day_header_height);
        DAY_HEADER_BOTTOM_MARGIN = (int) mResources.getDimension(R.dimen.day_header_bottom_margin);
        
        HOURS_TEXT_SIZE = (int) mResources.getDimension(R.dimen.hours_text_size);
        AMPM_TEXT_SIZE = (int) mResources.getDimension(R.dimen.ampm_text_size);
        MIN_HOURS_WIDTH = (int) mResources.getDimension(R.dimen.min_hours_width); // 24dip is 48dp(gNote)
        HOURS_LEFT_MARGIN = (int) mResources.getDimension(R.dimen.hours_left_margin);
        HOURS_RIGHT_MARGIN = (int) mResources.getDimension(R.dimen.hours_right_margin);
        MULTI_DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.day_header_height);
                
        NEW_EVENT_HINT_FONT_SIZE = (int) mResources.getDimension(R.dimen.new_event_hint_text_size);
        
        EVENT_TEXT_TOP_MARGIN = (int) mResources.getDimension(R.dimen.event_text_vertical_margin);
        EVENT_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
        EVENT_TEXT_PAD_BETWEEN_LINES = EVENT_TEXT_TOP_MARGIN;
        
        EVENT_TEXT_LEFT_MARGIN = (int) mResources
                .getDimension(R.dimen.event_text_horizontal_margin);
        EVENT_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        
        if (mScale == 0) {
            mScale = mResources.getDisplayMetrics().density;
            if (mScale != 1) {
                
                NORMAL_FONT_SIZE *= mScale;
                GRID_LINE_LEFT_MARGIN *= mScale;
                HOURS_TOP_MARGIN *= mScale;
                MIN_CELL_WIDTH_FOR_TEXT *= mScale;
                
                CURRENT_TIME_LINE_SIDE_BUFFER *= mScale;
                CURRENT_TIME_LINE_TOP_OFFSET *= mScale;

                MIN_Y_SPAN *= mScale;
                MAX_CELL_HEIGHT *= mScale;
                DEFAULT_CELL_HEIGHT *= mScale;
                DAY_HEADER_HEIGHT *= mScale;
                DAY_HEADER_RIGHT_MARGIN *= mScale;
                DAY_HEADER_ONE_DAY_LEFT_MARGIN *= mScale;
                DAY_HEADER_ONE_DAY_RIGHT_MARGIN *= mScale;
                DAY_HEADER_ONE_DAY_BOTTOM_MARGIN *= mScale;
                CALENDAR_COLOR_SQUARE_SIZE *= mScale;
                EVENT_RECT_TOP_MARGIN *= mScale;
                EVENT_RECT_BOTTOM_MARGIN *= mScale;
                
                EVENT_RECT_LEFT_MARGIN *= mScale;
                EVENT_RECT_RIGHT_MARGIN *= mScale;
                EVENT_RECT_STROKE_WIDTH *= mScale;
                EVENT_SQUARE_WIDTH *= mScale;
                EVENT_LINE_PADDING *= mScale;
                NEW_EVENT_MARGIN *= mScale;
                NEW_EVENT_WIDTH *= mScale;
                NEW_EVENT_MAX_LENGTH *= mScale;
            }
        }
        HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
        
        DAY_HEADER_HEIGHT = mNumDays == 1 ? ONE_DAY_HEADER_HEIGHT : MULTI_DAY_HEADER_HEIGHT;
        
        DisplayMetrics metrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float mXDpi = metrics.xdpi;
        TWO_CENTIMETERS_TO_PIXELS = Math.round(TWO_CENTIMETERS_TO_INCHES * mXDpi);

        mCurrentTimeLine = mResources.getDrawable(R.drawable.timeline_indicator_holo_light);
        mCurrentTimeAnimateLine = mResources
                .getDrawable(R.drawable.timeline_indicator_activated_holo_light);
        mTodayHeaderDrawable = mResources.getDrawable(R.drawable.today_blue_week_holo_light);
        
        mNewEventHintColor =  mResources.getColor(R.color.new_event_hint_text_color);
        mAcceptedOrTentativeEventBoxDrawable = mResources
                .getDrawable(R.drawable.panel_month_event_holo_light);

        mEventGeometry = new EventGeometry();
        mEventGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
        mEventGeometry.setHourGap(HOUR_GAP);
        mEventGeometry.setCellMargin(DAY_GAP);
        mLongPressItems = new CharSequence[] {
            mResources.getString(R.string.new_event_dialog_option)
        };
        mLongPressTitle = mResources.getString(R.string.new_event_dialog_label);
        mDeleteEventHelper = new DeleteEventHelper(mContext, null, false /* don't exit when done */);
        mLastPopupEventID = INVALID_EVENT_ID;
        mController = controller;
        
        mGestureDetector = new GestureDetector(mContext, new CalendarGestureListener());
        
        if (mCellHeight == 0) {
            mCellHeight = Utils.getSharedPreference(mContext,
                    SettingsFragment.KEY_DEFAULT_CELL_HEIGHT, DEFAULT_CELL_HEIGHT);            
            
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            float deviceHeight = getResources().getDisplayMetrics().heightPixels;                   	
        	float StatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        	float appHeight = (deviceHeight - StatusBarHeight);
        	float cellHeight = appHeight * 0.087f;
        	mCellHeight = (int) cellHeight; //mCellHeight = 107;//for test!!!            
            
            //KEY_DEFAULT_EVENT_FONT_SIZE�� ���Ŀ� �����ؾ� �Ѵ�!!!
            
        }
        else {
        	
        }
        
        mOneQuartersOfHourPixelHeight = mCellHeight / 4;
        mTwoQuartersOfHourPixelHeight = mOneQuartersOfHourPixelHeight * 2;
        mThreeQuartersOfHour = mOneQuartersOfHourPixelHeight * 3;            
        MIN_EVENT_HEIGHT = mOneQuartersOfHourPixelHeight;
        
        makeEventRectFontInfo(mCellHeight);
        
        mScroller = new OverScroller(mContext);
        mHScrollInterpolator = new ScrollInterpolator();
        mEdgeEffectTop = new EdgeEffect(mContext);
        mEdgeEffectBottom = new EdgeEffect(mContext);
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mScaledPagingTouchSlop = vc.getScaledPagingTouchSlop();
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        OVERFLING_DISTANCE = vc.getScaledOverflingDistance();

        init();  
        
    }
    
    TimeZone mTimeZone;
    long mGMToff;
    private void init() {
        //setFocusable(true);

        // Allow focus in touch mode so that we can do keyboard shortcuts
        // even after we've entered touch mode.
        //setFocusableInTouchMode(true);
        //setClickable(true);
        
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mTimeZone = TimeZone.getTimeZone(Utils.getTimeZone(mContext, mTZUpdater));
        mGMToff = (mTimeZone.getRawOffset()) / 1000;
        mCurrentTime = GregorianCalendar.getInstance(mTimeZone);//new Time(Utils.getTimeZone(mContext, mTZUpdater));
        mCurrentTime.setFirstDayOfWeek(mFirstDayOfWeek);
        long currentTime = System.currentTimeMillis();
        mCurrentTime.setTimeInMillis(currentTime);
        mTodayJulianDay = ETime.getJulianDay(currentTime, mTimeZone, mFirstDayOfWeek);

        mWeek_saturdayColor = mResources.getColor(R.color.week_saturday);
        mWeek_sundayColor = mResources.getColor(R.color.week_sunday);
        mCalendarDateBannerTextColor = mResources.getColor(R.color.calendar_date_banner_text_color);
        mFutureBgColorRes = mResources.getColor(R.color.calendar_future_bg_color);
        mBgColor = mResources.getColor(R.color.calendar_hour_background);
        mCalendarAmPmLabel = mResources.getColor(R.color.calendar_ampm_label);
        mCalendarGridAreaSelected = mResources.getColor(R.color.calendar_grid_area_selected);
        mCalendarGridLineInnerHorizontalColor = mResources
                .getColor(R.color.calendar_grid_line_inner_horizontal_color);
        mCalendarGridLineInnerVerticalColor = mResources
                .getColor(R.color.calendar_grid_line_inner_vertical_color);
        mCalendarHourLabelColor = mResources.getColor(R.color.calendar_hour_label);
        mPressedColor = mResources.getColor(R.color.pressed);
        mClickedColor = mResources.getColor(R.color.day_event_clicked_background_color);
        mEventTextColor = mResources.getColor(R.color.calendar_event_text_color);
        mMoreEventsTextColor = mResources.getColor(R.color.month_event_other_color);

        mEventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
        mEventTextPaint.setTextAlign(Paint.Align.LEFT);
        mEventTextPaint.setAntiAlias(true);

        int gridLineColor = mResources.getColor(R.color.calendar_grid_line_highlight_color);
        Paint p = mSelectionPaint;
        p.setColor(gridLineColor);
        p.setStyle(Style.FILL);
        p.setAntiAlias(false);

        p = mPaint;
        p.setAntiAlias(true);

        // Allocate space for 2 weeks worth of weekday names so that we can
        // easily start the week display at any week day.
        mDayStrs = new String[14];

        // Also create an array of 2-letter abbreviations.
        mDayStrs2Letter = new String[14];

        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            int index = i - Calendar.SUNDAY;
            // e.g. Tue for Tuesday
            mDayStrs[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_MEDIUM).toUpperCase();
            mDayStrs[index + 7] = mDayStrs[index];
            // e.g. Tu for Tuesday
            mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORT).toUpperCase();

            // If we don't have 2-letter day strings, fall back to 1-letter.
            if (mDayStrs2Letter[index].equals(mDayStrs[index])) {
                mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORTEST);
            }

            mDayStrs2Letter[index + 7] = mDayStrs2Letter[index];
        }

        // Figure out how much space we need for the 3-letter abbrev names
        // in the worst case.
        p.setTextSize(DATE_HEADER_FONT_SIZE);
        p.setTypeface(mBold);
        String[] dateStrs = {" 28", " 30"};
        mDateStrWidth = computeMaxStringWidth(0, dateStrs, p);
        p.setTextSize(DAY_HEADER_FONT_SIZE);
        mDateStrWidth += computeMaxStringWidth(0, mDayStrs, p);

        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(null);
        handleOnResume();        
        mMaxHourDecimalTextWidth = computeMaxStringWidth(0, mHourStrs, p);
        
        mHoursTextHeight = (int)Math.abs(p.ascent());    
        int hourDescent = (int) (p.descent() + 0.5f);
        mRealHoursTextHeight = mHoursTextHeight + hourDescent;

        mAmString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
        mPmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
        String[] ampm = {mAmString, mPmString};
        String space = " ";
        p.setTextSize(AMPM_TEXT_SIZE);
        String[] fullAmPm = {
        		mAmString + space + "12:59",
        		mPmString + space + "11:59"
        };
        int maxHourStringWidth = computeMaxStringWidth(0, fullAmPm, p);        
        mAMPMTextHeight = (int)(Math.abs(p.ascent()) + p.descent());
        
        mHoursWidth = NEW_HOURS_LEFT_MARGIN + maxHourStringWidth + NEW_HOURS_RIGHT_MARGIN;
        // GRID_LINE_LEFT_MARGIN�� 
        // ���� �ð��� ��ü ���̷� �����
        // :ex) ���� 12:59
        GRID_LINE_LEFT_MARGIN = mHoursWidth; // +added by intheeast        
        GRID_LINE_TOP_MARGIN = (HOUR_GAP + HOURS_TOP_MARGIN) + ( mRealHoursTextHeight / 2);
        GRID_LINE_BOTTOM_MARGIN = GRID_LINE_TOP_MARGIN;
        
        mBaseDate = GregorianCalendar.getInstance(mTimeZone);
        mBaseDate.setFirstDayOfWeek(mFirstDayOfWeek);
        long millis = System.currentTimeMillis();
        mBaseDate.setTimeInMillis(millis);

        mEarliestStartHour = new int[mNumDays];
        //mHasAllDayEvent = new boolean[mNumDays];

        // mLines is the array of points used with Canvas.drawLines() in
        // drawGridBackground() and drawAllDayEvents().  Its size depends
        // on the max number of lines that can ever be drawn by any single
        // drawLines() call in either of those methods.
        final int maxGridLines = (24 + 1)  // max horizontal lines we might draw
                + (mNumDays + 1); // max vertical lines we might draw
        mLines = new float[maxGridLines * 4];
        
    }
    
    public final int KELEGANT_ASCENT_VALUE = 1900;
    public final int KELEGANT_DESCENT_VALUE = -500;		
    //public final float DEFAULT_EVENT_TEXT_HEIGHT_RATIO = 0.25f; // 0.20 �ۿ� ������ �ʴ´�...
    public final float DEFAULT_EVENT_TEXT_HEIGHT_RATIO = 0.3f; // �������� cell height�� text height ������ ���� ����� ����...    
    int mAvailEventRectHeightByDefaultEventTextHeight; // Event title�� location�� one line�� ������ �� ����
    int mMinEventRectHeightTwoLinesEventText;
    //float MIN_EVENT_HEIGHT_FONT_SIZE;
    public void makeEventRectFontInfo(float cellHeight) {
    	EVENT_TEXT_FONT_SIZE = getEventFontSize(cellHeight, DEFAULT_EVENT_TEXT_HEIGHT_RATIO);
    	
    	Paint testPaint = new Paint();
    	testPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    	testPaint.setTextAlign(Paint.Align.LEFT);
    	testPaint.setAntiAlias(true);
    	
    	testPaint.setTextSize(EVENT_TEXT_FONT_SIZE);  		
		
		float eventTitleAscent = testPaint.ascent();
		float eventTitleDescent = testPaint.descent();
		int eventTitleLineHeight = Math.round(Math.abs(eventTitleAscent) + Math.abs(eventTitleDescent));
    	
		float locationFontSize = EVENT_TEXT_FONT_SIZE * 0.9f;
		testPaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		testPaint.setTextSize(locationFontSize);
    	float eventLocationAscent = testPaint.ascent();
    	float eventLocationDescent = testPaint.descent();
    	int eventLocationLineHeight = Math.round(Math.abs(eventLocationAscent) + Math.abs(eventLocationDescent));  
    	
    	// ���� mDefaultEventTextHeight�� ������ which event rect height ���� cover�� �Ǵ����� ��������..
    	// event height 
    	// mDefaultEventTextHeight�� ������ ��� ������ Event Height�� [mDefaultEventTextHeight * 2] �̴�
		mAvailEventRectHeightByDefaultEventTextHeight = eventTitleLineHeight * 2;
    	// FROM mAvailDefaultEventTextHeightEventHeight TO MIN_EVENT_HEIGHT �� ��� �� ���ΰ�?
    	//MIN_EVENT_HEIGHT_FONT_SIZE = getEventFontSize((int)EVENT_TEXT_FONT_SIZE, MIN_EVENT_HEIGHT, 0.6f);
    	//MIN_EVENT_HEIGHT_FONT_SIZE = getEventFontSize(MIN_EVENT_HEIGHT, 0.5f);
    	
    	mMinEventRectHeightTwoLinesEventText = EVENT_TEXT_TOP_MARGIN + 
    			eventTitleLineHeight +
    			EVENT_TEXT_PAD_BETWEEN_LINES +
    			eventLocationLineHeight + 
    			EVENT_TEXT_BOTTOM_MARGIN;       	
    }
    
    public float getEventFontSize(float eventHeight, float ratio) {
    	
    	float targetEventTextHeight = eventHeight * ratio;
    	float roundTargetEventTextHeight = (int) Math.round(targetEventTextHeight);
    	    	
    	float eventFontSize = (roundTargetEventTextHeight * 2048.0f) / (float)(Math.abs(KELEGANT_ASCENT_VALUE) + Math.abs(KELEGANT_DESCENT_VALUE));
    	    	
    	return eventFontSize;
    	
    }
    
    public float getEventFontSize(int startFontSize, float eventHeight, float ratio) {
    	//ratio = ratio + 0.1f; // ������ �𸣰����� 0.1�� �����ϸ�...
    	float eventFontSize = 0;
    	float targetDefaultEventTextHeightF = eventHeight * ratio;
    	int targetDefaultEventTextHeight = (int) Math.round(targetDefaultEventTextHeightF);
    	Paint testPaint = new Paint();
    	testPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    	testPaint.setTextAlign(Paint.Align.LEFT);
    	testPaint.setAntiAlias(true);   		
    	
    	int prvEventFontSize = 0;
    	int prvCalcDefaultEventTextHeight = 0;
    	int fontSize = startFontSize;   
    	while(true) {
    		testPaint.setTextSize(fontSize);		
    		
    		float ascent = testPaint.ascent();
    		float descent = testPaint.descent();
	    	// targetDefaultEventTextHeight = (x * KELEGANT_ASCENT_VALUE / 2048) + (x * KELEGANT_DESCENT_VALUE / 2048)
    		float Ascent = -fontSize * KELEGANT_ASCENT_VALUE / 2048;//float Ascent = testPaint.ascent();
    		float Descent = -fontSize * KELEGANT_DESCENT_VALUE / 2048;//float Descent = testPaint.descent();
	    	int curCalcedEventTextHeight = (int) (Math.abs(Ascent) + Math.abs(Descent));  
	    	
	    	
	    	if (curCalcedEventTextHeight > targetDefaultEventTextHeight) {
	    		// ���⼭ i�� prvFontSize �� �� ���� �� targetDefaultEventTextHeight�� ���������� üũ�ؾ� �Ѵ�
	    		int x1 = curCalcedEventTextHeight - targetDefaultEventTextHeight;
	    		int x2 = targetDefaultEventTextHeight - prvCalcDefaultEventTextHeight;
	    		if (x1 > x2) { // prvFontSize�� prvCalcDefaultEventTextHeight ���� targetDefaultEventTextHeight�� �� �����ߴٴ� ���� �ǹ���
	    			eventFontSize = prvEventFontSize;
	    			//if (INFO) Log.i(TAG, "event font size=" + String.valueOf(i) + ", totalH=" + String.valueOf(calcDefaultEventTextHeight));
	    		}
	    		else if (x2 > x1) {
	    			eventFontSize = fontSize;
	    			//if (INFO) Log.i(TAG, "event font size=" + String.valueOf(i) + ", totalH=" + String.valueOf(calcDefaultEventTextHeight));
	    		}
	    		else {
	    			// �̷� ��찡 �߻��� �� �ִ°�??? 
	    			eventFontSize = fontSize;
	    			//if (INFO) Log.i(TAG, "event font size=" + String.valueOf(i) + ", totalH=" + String.valueOf(calcDefaultEventTextHeight));
	    		}   		
	    		
	    		break;
	    	}
	    	else if (curCalcedEventTextHeight == targetDefaultEventTextHeight) {
	    		//if (INFO) Log.i(TAG, "event font size=" + String.valueOf(i) + ", totalH=" + String.valueOf(calcDefaultEventTextHeight));
	    		eventFontSize = fontSize;
	    		break;
	    	}
	    		    	
	    	//if (INFO) Log.i(TAG, "font size=" + String.valueOf(i) + ", totalH=" + String.valueOf(calcDefaultEventTextHeight));
	    	
	    	prvEventFontSize = fontSize;
	    	prvCalcDefaultEventTextHeight = curCalcedEventTextHeight;
	    	fontSize--;
    	}
    	
    	return eventFontSize;
    }
    
    
    public int getHoursWidth() {
    	return mHoursWidth;
    }

    public int getCalendarGridAreaSelected() {
    	return mCalendarGridAreaSelected;
    }
    @Override
    protected void onAttachedToWindow() {
        //Log.i("tag", "DayView::onAttachedToWindow");

        super.onAttachedToWindow();
        if (mHandler == null) {
            mHandler = getHandler();
            // mUpdateCurrentTime�� ���� �ڵ带 ������ �ʿ�� ����
            // if ( (mFirstJulianDay <= mTodayJulianDay) && (mTodayJulianDay <= mLastJulianDay) ) 
            // onAttachedToWindow�� ȣ��Ǳ� ����,,,
            // DayFragment�� makeView���� setSelected�� ���� ȣ���ϱ� �����̴�
            // setSelected���� recalc�� ȣ���ϱ� �����̴�
            mHandler.post(mUpdateCurrentTime);
        }
    }

    

    /*
    public void onClick(View v) {
        //if (v == mPopupView) {
            // Pretend it was a trackball click because that will always
            // jump to the "View event" screen.
            //switchViews(true);
        //}
    }
	*/

    public void handleOnResume() {
        initAccessibilityVariables();
        if(Utils.getSharedPreference(mContext, OtherPreferences.KEY_OTHER_1, false)) {
            mFutureBgColor = 0;
        } else {
            mFutureBgColor = mFutureBgColorRes;
        }
        mIs24HourFormat = DateFormat.is24HourFormat(mContext);
        mHourStrs = mIs24HourFormat ? CalendarData.s24Hours : CalendarData.s12HoursNoAmPm;
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mLastSelectionDayForAccessibility = 0;
        mLastSelectionHourForAccessibility = 0;
        mLastSelectedEventForAccessibility = null;        
    }

    private void initAccessibilityVariables() {
        mAccessibilityMgr = (AccessibilityManager) mContext
                .getSystemService(Service.ACCESSIBILITY_SERVICE);
        mIsAccessibilityEnabled = mAccessibilityMgr != null && mAccessibilityMgr.isEnabled();
        mTouchExplorationEnabled = isTouchExplorationEnabled();
    }

    /**
     * Returns the start of the selected time in milliseconds since the epoch.
     *
     * @return selected time in UTC milliseconds since the epoch.
     */
    long getSelectedTimeInMillis() {
    	Calendar time = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
    	time.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
    	time.setTimeInMillis(mBaseDate.getTimeInMillis());
        
    	long selectionDayMillis = ETime.getMillisFromJulianDay(mSelectionDay, mTimeZone, mFirstDayOfWeek);
    	time.setTimeInMillis(selectionDayMillis);
    	
        time.set(Calendar.HOUR_OF_DAY, mSelectionStartHour);//time.hour = mSelectionStartHour;
        time.set(Calendar.MINUTE, mSelectionStartMinutes);//time.minute = mSelectionStartMinutes;      
        
        /*
        Time time = new Time(mBaseDate);
        time.setJulianDay(mSelectionDay);
        time.hour = mSelectionStartHour;
        time.minute = mSelectionStartMinutes;

        // We ignore the "isDst" field because we want normalize() to figure
        // out the correct DST value and not adjust the selected time based
        // on the current setting of DST.
        return time.normalize(true);
        */
        return time.getTimeInMillis();
    }

    Calendar getSelectedTime() {
    	Calendar time = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
    	time.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
    	time.setTimeInMillis(mBaseDate.getTimeInMillis());
        
    	long selectionDayMillis = ETime.getMillisFromJulianDay(mSelectionDay, mTimeZone, mFirstDayOfWeek);
    	time.setTimeInMillis(selectionDayMillis);
    	
        time.set(Calendar.HOUR_OF_DAY, mSelectionStartHour);//time.hour = mSelectionStartHour;
        time.set(Calendar.MINUTE, mSelectionStartMinutes);//time.minute = mSelectionStartMinutes;        

        // We ignore the "isDst" field because we want normalize() to figure
        // out the correct DST value and not adjust the selected time based
        // on the current setting of DST.
        //time.normalize(true /* ignore isDst */);
        return time;
    }

    /*
    Time getSelectedTimeForAccessibility() {
        Time time = new Time(mBaseDate);
        time.setJulianDay(mSelectionDayForAccessibility);
        time.hour = mSelectionStartHourForAccessibility;

        // We ignore the "isDst" field because we want normalize() to figure
        // out the correct DST value and not adjust the selected time based
        // on the current setting of DST.
        time.normalize(true);
        return time;
    }
	*/
    /**
     * Returns the start of the selected time in minutes since midnight,
     * local time.  The derived class must ensure that this is consistent
     * with the return value from getSelectedTimeInMillis().
     */
    int getSelectedMinutesSinceMidnight() {
        return mSelectionStartHour * MINUTES_PER_HOUR;
    }

    int getFirstVisibleHour() {
        return mFirstHour;
    }
    
    int getFirstVisibleHourOffset() {        
        return mFirstHourOffset;
    }
    
    void setFirstVisibleHour(int firstHour) {
        mFirstHour = firstHour;        
    }
    
    void setFirstVisibleHourOffset(int firstHourOffset) {        
        mFirstHourOffset = firstHourOffset;
    }

    /*
    void setFirstVisibleHour(int firstHour) {
        mFirstHour = firstHour;
        mFirstHourOffset = 0;
    }
	*/
    // ���⼭ upper secondary actionbar �� ��������
    //private String mTimeZone; // +added by intheeast
    public void initBaseDateTime(Calendar time, boolean ignoreTime, boolean animateToday) {
        mBaseDate.setTimeInMillis(time.getTimeInMillis());
        
        mFirstHour = mBaseDate.get(Calendar.HOUR_OF_DAY) - mNumHours / 5;
        if (mFirstHour < 0) {
            mFirstHour = 0;
        } else if (mFirstHour + mNumHours > 24) {
            mFirstHour = 24 - mNumHours;
        }
        
        mFirstHourOffset = 0;
                
        long millis = mBaseDate.getTimeInMillis();
        setSelectedDay(ETime.getJulianDay(millis, mTimeZone, mFirstDayOfWeek));
        mSelectedEvents.clear();
        //mComputeSelectedEvents = true;

        int gotoY = Integer.MIN_VALUE;

        if (!ignoreTime && mGridAreaHeight != -1) {
            int lastHour = 0;

            if (mBaseDate.get(Calendar.HOUR_OF_DAY) < mFirstHour) {
                // Above visible region
                gotoY = mBaseDate.get(Calendar.HOUR_OF_DAY) * (mCellHeight + HOUR_GAP);
            } else {
                lastHour = (mGridAreaHeight - mFirstHourOffset) / (mCellHeight + HOUR_GAP)
                        + mFirstHour;

                if (mBaseDate.get(Calendar.HOUR_OF_DAY) >= lastHour) {
                    // Below visible region

                    // target hour + 1 (to give it room to see the event) -
                    // grid height (to get the y of the top of the visible
                    // region)
                    gotoY = (int) ((mBaseDate.get(Calendar.HOUR_OF_DAY) + 1 + mBaseDate.get(Calendar.MINUTE) / 60.0f)
                            * (mCellHeight + HOUR_GAP) - mGridAreaHeight);
                }
            }            

            if (gotoY > mMaxViewStartY) {
                gotoY = mMaxViewStartY;
            } else if (gotoY < 0 && gotoY != Integer.MIN_VALUE) {
                gotoY = 0;
            }
        }

        recalc();////////////////////////////////////////////////////////////////////////////////////////////////////////

        //mRemeasure = true;
        //invalidate();

        boolean delayAnimateToday = false;
        if (gotoY != Integer.MIN_VALUE) {
            @SuppressLint("ObjectAnimatorBinding") ValueAnimator scrollAnim = ObjectAnimator.ofInt(this, "viewStartY", mViewStartY, gotoY);
            scrollAnim.setDuration(GOTO_SCROLL_DURATION);
            scrollAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            scrollAnim.addListener(mAnimatorListener);
            scrollAnim.start();
            delayAnimateToday = true;
        }
        
        if (animateToday) {
            synchronized (mTodayAnimatorListener) {
                if (mTodayAnimator != null) {
                    mTodayAnimator.removeAllListeners();
                    mTodayAnimator.cancel();
                }
                mTodayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha",
                        mAnimateTodayAlpha, 255);
                mAnimateToday = true;
                mTodayAnimatorListener.setFadingIn(true);
                mTodayAnimatorListener.setAnimator(mTodayAnimator);
                mTodayAnimator.addListener(mTodayAnimatorListener);
                mTodayAnimator.setDuration(150);
                if (delayAnimateToday) {
                    mTodayAnimator.setStartDelay(GOTO_SCROLL_DURATION);
                }
                mTodayAnimator.start();
            }
        }
        //sendAccessibilityEventAsNeeded(false);
    }
    
    public void setBaseTime(Calendar time) {
    	mBaseDate.setTimeInMillis(time.getTimeInMillis());//(time);   	
    	//mBaseDate.normalize(true);
    	
    	recalc();////////////////////////////////
    	
    	mSelectedEvents.clear();
    	
    	long millis = mBaseDate.getTimeInMillis();
        setSelectedDay(ETime.getJulianDay(millis, mTimeZone, mFirstDayOfWeek));        
    	
    }    
    
    public void setSelectedTime(Calendar time) {       
        
        setSelectedHour(time.get(Calendar.HOUR_OF_DAY));
        setSelectedMinutes(time.get(Calendar.MINUTE));        
        
        setSelectedDay(ETime.getJulianDay(mBaseDate.getTimeInMillis(), mTimeZone, mFirstDayOfWeek));
        mSelectedEvents.clear();
        //mComputeSelectedEvents = true;           
    }
    
    public class SelectedEventRectContext {
    	//Event selectedEvent;
    	//int mode;
    	
    	int hour;
    	int minute;
    	int viewStartY;
    	//int prvNewEventMoveX;
    	//int prvNewEventMoveY;
    	//int prvNewEventLeft;
    	//int prvNewEventRight; 
    	
    	boolean autoVerticalScrollingByNewEventRect;
    	int autoVerticalScrollingType;
    	boolean scrolling;
    	int scrollStartY;
    	int minutuePosPixelAtSelectedEventMove;
    	
    	
    	
    	public SelectedEventRectContext(int hour, int minute, int viewStartY,    			
    			boolean autoVerticalScrollingByNewEventRect, int autoVerticalScrollingType,
    			boolean scrolling, int scrollStartY,
    			int minutuePosPixelAtSelectedEventMove) {
    		//this.selectedEvent = selectedEvent;
    		//this.mode = mode;
    		
    		this.hour = hour;
    		this.minute = minute;
    		this.viewStartY = viewStartY;
    		//this.prvNewEventMoveX = prvNewEventMoveX;
        	//this.prvNewEventMoveY = prvNewEventMoveY;
        	//this.prvNewEventLeft = prvNewEventLeft;
        	//this.prvNewEventRight = prvNewEventRight; 
        	
        	this.autoVerticalScrollingByNewEventRect = autoVerticalScrollingByNewEventRect;
        	this.autoVerticalScrollingType = autoVerticalScrollingType;
        	this.scrolling = scrolling;
        	this.scrollStartY = scrollStartY;
        	this.minutuePosPixelAtSelectedEventMove = minutuePosPixelAtSelectedEventMove;
    	}
    }
    
    public void setSelectedPrvCurrentView(SelectedEventRectContext context) {
    	        
        recalc();////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        setSelectedHour(context.hour);          
        setSelectedMinutes(context.minute);        
        
        long millis = mBaseDate.getTimeInMillis();
        setSelectedDay(ETime.getJulianDay(millis, mTimeZone, mFirstDayOfWeek));
        mSelectedEvents.clear();
        //mComputeSelectedEvents = true;
        
        mViewStartY = context.viewStartY;
        computeFirstHour();
        
        remeasure(getWidth(), getHeight());
                
    	int selectedTop = mFragment.getPrvNewOrSelectedEventMoveY() - (mCellHeight / 2);
    	setSelectionFromPosition(mFragment.getPrvNewOrSelectedEventMoveX(), mFragment.getPrvNewOrSelectedEventMoveY(), selectedTop);    	
    	
    	mStartingScroll = true;
    	
    	mScrollStartY = context.scrollStartY;
    	mScrolling = context.scrolling;

    	mMinutuePosPixelAtSelectedEventMove = context.minutuePosPixelAtSelectedEventMove;    
    	
    }
    
    public void setSelectedPrvCurrentView(int hour, int minute, int viewStartY,    			
			boolean autoVerticalScrollingByNewEventRect, int autoVerticalScrollingType,
			boolean scrolling, int scrollStartY,
			int minutuePosPixelAtSelectedEventMove) {
    	
        recalc();////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        setSelectedHour(hour);          
        setSelectedMinutes(minute);        
        
        long millis = mBaseDate.getTimeInMillis();
        setSelectedDay(ETime.getJulianDay(millis, mTimeZone, mFirstDayOfWeek));
        mSelectedEvents.clear();
        //mComputeSelectedEvents = true;
        
        mViewStartY = viewStartY;
        computeFirstHour();
        
        remeasure(getWidth(), getHeight());
        
    	int selectedTop = mFragment.getPrvNewOrSelectedEventMoveY() - (mCellHeight / 2);
    	setSelectionFromPosition(mFragment.getPrvNewOrSelectedEventMoveX(), mFragment.getPrvNewOrSelectedEventMoveY(), selectedTop);    	
    	
    	mStartingScroll = true;
    	
    	mScrollStartY = scrollStartY;
    	mScrolling = scrolling;

    	mMinutuePosPixelAtSelectedEventMove = minutuePosPixelAtSelectedEventMove;    	
    	
        invalidate();       
    }
    
    
    public void setAnimateTodayAlpha(int todayAlpha) {
        mAnimateTodayAlpha = todayAlpha;
        invalidate();
    }
    
    public int getViewStartY() {
    	return mViewStartY;
    }
    
    
    

    public Calendar getSelectedDay() {
    	Calendar time = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
    	time.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
        time.setTimeInMillis(mBaseDate.getTimeInMillis());
        
        long selectionDayMillis = ETime.getMillisFromJulianDay(mSelectionDay, mTimeZone, mFirstDayOfWeek);
        time.setTimeInMillis(selectionDayMillis); //time.setJulianDay(mSelectionDay);
        
        time.set(Calendar.HOUR_OF_DAY, mSelectionStartHour);//time.hour = mSelectionStartHour;
        time.set(Calendar.MINUTE, mSelectionStartMinutes);//time.minute = mSelectionStartMinutes;
        
        // We ignore the "isDst" field because we want normalize() to figure
        // out the correct DST value and not adjust the selected time based
        // on the current setting of DST.
        //time.normalize(true /* ignore isDst */);
        return time;
    }
    
    public int getSelectedJulianDay() {        
        return mSelectionDay;
    }

    public void updateTitle() {
        Calendar start = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
        start.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
        start.setTimeInMillis(mBaseDate.getTimeInMillis());
        //start.normalize(true);
        
        
        Calendar end = GregorianCalendar.getInstance(mBaseDate.getTimeZone());//new Time(mBaseDate);
        end.setFirstDayOfWeek(mBaseDate.getFirstDayOfWeek());
        end.setTimeInMillis(start.getTimeInMillis());
        end.add(Calendar.DAY_OF_MONTH, (mNumDays - 1));
        end.add(Calendar.MINUTE, 1);// Move it forward one minute so the formatter doesn't lose a day
        

        long formatFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
        if (mNumDays != 1) {
            // Don't show day of the month if for multi-day view
            formatFlags |= DateUtils.FORMAT_NO_MONTH_DAY;

            // Abbreviate the month if showing multiple months
            if (start.get(Calendar.MONTH) != end.get(Calendar.MONTH)) {
                formatFlags |= DateUtils.FORMAT_ABBREV_MONTH;
            }
        }

        mController.sendEvent(this, EventType.UPDATE_TITLE, start, end, null, -1, ViewType.CURRENT,
                formatFlags, null, null);
    }
        
    /**
     * return a negative number if "time" is comes before the visible time
     * range, a positive number if "time" is after the visible time range, and 0
     * if it is in the visible time range.
     */
    public int compareToVisibleTimeRange(Calendar time) {

        int savedHour = mBaseDate.get(Calendar.HOUR_OF_DAY);//mBaseDate.hour;
        int savedMinute = mBaseDate.get(Calendar.MINUTE);//mBaseDate.minute;
        int savedSec = mBaseDate.get(Calendar.SECOND);//mBaseDate.second;

        mBaseDate.set(Calendar.HOUR_OF_DAY, 0);//hour = 0;
        mBaseDate.set(Calendar.MINUTE, 0);//mBaseDate.minute = 0;
        mBaseDate.set(Calendar.SECOND, 0);//mBaseDate.second = 0;

        // Compare beginning of range
        int diff = ETime.compare(time, mBaseDate);
        if (diff > 0) {
            // Compare end of range
        	mBaseDate.add(Calendar.DAY_OF_MONTH, mNumDays); // mBaseDate.monthDay += mNumDays;
            
            diff = ETime.compare(time, mBaseDate);
            
            mBaseDate.add(Calendar.DAY_OF_MONTH, -mNumDays); //mBaseDate.monthDay -= mNumDays;
            
            if (diff < 0) {
                // in visible time
                diff = 0;
            } else if (diff == 0) {
                // Midnight of following day
                diff = 1;
            }
        }

        
        mBaseDate.set(Calendar.HOUR_OF_DAY, savedHour);//hour = 0;
        mBaseDate.set(Calendar.MINUTE, savedMinute);//mBaseDate.minute = 0;
        mBaseDate.set(Calendar.SECOND, savedSec);//mBaseDate.second = 0;
        
        return diff;
    }

    public void recalc() {
        // Set the base date to the beginning of the week if we are displaying
        // 7 days at a time.
        if (mNumDays == 7) {
        	ETime.adjustStartDayInWeek(mBaseDate);
        }

        final long start = mBaseDate.getTimeInMillis();
        mFirstJulianDay = ETime.getJulianDay(start, mTimeZone, mFirstDayOfWeek);
        mLastJulianDay = mFirstJulianDay + mNumDays - 1;

        mMonthLength = mBaseDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        mFirstVisibleDate = mBaseDate.get(Calendar.DAY_OF_MONTH);//mBaseDate.monthDay;
        mFirstVisibleDayOfWeek = mBaseDate.get(Calendar.DAY_OF_WEEK);//mBaseDate.weekDay;
    }

    public int getFirstDayOfWeek() {
    	return mFirstDayOfWeek;
    }
    
    public int getFirstJulianDay() {
    	return mFirstJulianDay;
    }
    
    public int getFirstVisibleDate() {
    	return mFirstVisibleDate;
    }
    
    public int getFirstVisibleDayOfWeek() {
    	return mFirstVisibleDayOfWeek;
    }
    
    

    
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    	if (INFO) Log.i(TAG, "onSizeChanged");
    	
    	//Time test = mBaseDate;
    	
        mViewWidth = width;
        mViewHeight = height;
        mEdgeEffectTop.setSize(mViewWidth, mViewHeight);
        mEdgeEffectBottom.setSize(mViewWidth, mViewHeight);
        int gridAreaWidth = width - mHoursWidth;
        mCellWidth = (gridAreaWidth - (mNumDays * DAY_GAP)) / mNumDays; 
        
        // This would be about 1 day worth in a 7 day view
        //mHorizontalSnapBackThreshold = width / 7;
        mHorizontalSnapBackThreshold = width / 2;

        setAlldayEventListViewDimension();
        /*
        Paint p = new Paint();
        p.setTextSize(HOURS_TEXT_SIZE);
        mHoursTextHeight = (int) Math.abs(p.ascent());
        */
         // +commented by intheeast : this code cop
        //remeasure(width, height);
    }

    //AllDayEventListView mAllDayEventLists = null;
    //AllDayEventListItemAdapter mAllDayEventListItemAdapter;
    /*
    public void makeAllDayEventListView() {
    	mAllDayEventLists = new AllDayEventListView(mContext);
    	
    	LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	mAllDayEventLists.setLayoutParams(params);
    	
    	mAllDayEventLists.setCacheColorHint(0);	        
    	mAllDayEventLists.setDivider(null);	        
    	mAllDayEventLists.setItemsCanFocus(true);	        
    	mAllDayEventLists.setFastScrollEnabled(false); 
    	mAllDayEventLists.setVerticalScrollBarEnabled(false);	    	
    	mAllDayEventLists.setFadingEdgeLength(0);
    	
    	mAllDayEventLists.setVisibility(View.GONE);
    	
    	addView(mAllDayEventLists);
    }
    */
    
    /*
    public void setAllDayEventListView(int height) {
    	if (INFO) Log.i(TAG, "setAllDayEventListView:height=" + String.valueOf(height));
    	
    	
    	mAllDayEventLists.setVisibility(View.VISIBLE);
    	
    	LayoutParams params = new LayoutParams(mViewWidth, height);
    	mAllDayEventLists.setLayoutParams(params);
    	
    	
    	
    	mAllDayEventLists.setPadding(mHoursWidth, mAlldayEventListViewFirstItemTopMargin, 0, mAlldayEventListViewLastItemBottomMargin);
    	mAllDayEventLists.setDividerHeight(mAlldayEventListViewItemGap);
    	
    	    	
    	mAllDayEventListItemAdapter = new AllDayEventListItemAdapter(mContext, mMaxAlldayEvents, 
    			mCellWidth, 
    			mAlldayEventListViewItemHeight, 
    			mAlldayEventListViewFirstItemTopMargin, 
    			mAlldayEventListViewItemGap,    			
    			mAllDayEvents);
    	mAllDayEventLists.setAdapter(mAllDayEventListItemAdapter);    	
    }
    */
    
    int mAlldayEventListViewMaxHeight;
    int mAlldayEventListViewItemHeight;
    int mAlldayEventListViewFirstItemTopMargin;
    int mAlldayEventListViewLastItemBottomMargin;
    int mAlldayEventListViewItemGap;
    public void setAlldayEventListViewDimension() {
    	// all day event listview�� max height : dayview height�� 0.15�� �����Ѵ�
    	// all day event listview�� item height�� : all day event listview�� max height�� 0.35�� �����Ѵ�
    	// all day event listview�� first item top margin : GRID_LINE_TOP_MARGIN�� 0.5
    	// all day event listview�� last item bottom margin : GRID_LINE_TOP_MARGIN�� 0.5
    	// all day event listview�� item gap : first item top margin�� 0.5
    	
    	mAlldayEventListViewMaxHeight = (int) (mViewHeight * 0.15f);
    	mAlldayEventListViewItemHeight = (int) (mAlldayEventListViewMaxHeight * 0.35f);
    	mAlldayEventListViewFirstItemTopMargin = (int) (GRID_LINE_TOP_MARGIN * 0.5f);
    	mAlldayEventListViewLastItemBottomMargin = mAlldayEventListViewFirstItemTopMargin;
    	mAlldayEventListViewItemGap = (int) (mAlldayEventListViewFirstItemTopMargin * 0.5f);
    	
    }
    /**
     * Measures the space needed for various parts of the view after
     * loading new events.  This can change if there are all-day events.
     */    
    //<remeasure>
    //-mNumDays : DayView�� weekly ������ �ƴ� one day ������ ��� 1�̴�
    //-DAY_HEADER_HEIGHT : �츮�� �� ���� �ʿ� �����Ƿ� 0���� ������
    //-mCellHeight : 0��->��� �����Ǵ��� Ȯ�ο��?
    //-mFirstCell : all day event�� �������� ������ DAY_HEADER_HEIGHT[0]�� �����ǰ�
    //              �����Ѵٸ�, DAY_HEADER_HEIGHT + allDayHeight + ALLDAY_TOP_MARGIN �� ������
    //-mGridAreaHeight : onSizeChanged���� ���޵� height ���� mFirstCell�� ������ ���� �����ȴ�
    //-mNumHours : mGridAreaHeight / (mCellHeight + HOUR_GAP) �ε� mCellHeight + HOUR_GAP�� 
    //             ���������� 1 hour�� �����ϴ� height ���� 
    //             -> day view�� ���÷����� �� �ִ� hours�� ������
    //-mMaxViewStartY : ��� ���� �ִ��� �Ʒ��� ��ũ�ѵ� �� �ִ� Y ������ ��
    //#initFirstHour���� �����ϴ� mSelectionStartHour�� ���� ��� �����Ǵ°�? : ���� ���� �´�
    // :�� initFirstHour���� mFirstHour = mSelectionStartHour - mNumHours / 5; �� ���ϴ°�?
    //  �� mSelectionStartHour���� mNumHours / 5 ���� ���ִ°�? �Ѥ�;
    //  �׸��� mNumHours / 5 �� ���� �ǹ��ΰ�?
    //-mViewStartY = mFirstHour * (mCellHeight + HOUR_GAP) - mFirstHourOffset;
    public void remeasure(int width, int height) {
    	    	    	
        // First, clear the array of earliest start times, and the array
        // indicating presence of an all-day event.
        for (int day = 0; day < mNumDays; day++) {
            mEarliestStartHour[day] = 25;  // some big number
            //mHasAllDayEvent[day] = false;
        }      

        // The min is where 24 hours cover the entire visible area
        mMinCellHeight = Math.max((height - DAY_HEADER_HEIGHT) / 24, (int) MIN_EVENT_HEIGHT);
        if (mCellHeight < mMinCellHeight) {
            mCellHeight = mMinCellHeight;
        }

        // Calculate mAllDayHeight
        mFirstCell = DAY_HEADER_HEIGHT; 
        

        // dayview�� daily ������ ��쿡�� �׸��� ���°� �ƴϴ�
        // �׷��� weekly ������ ��쿡�� ���ڹ��̰� �Ǳ� ������ Grid Area��� ���̹���
        mGridAreaHeight = (int) (height - (mFirstCell));

        // Set up the expand icon position
        //int allDayIconWidth = mExpandAlldayDrawable.getIntrinsicWidth();
        //mExpandAllDayRect.left = Math.max((mHoursWidth - allDayIconWidth) / 2,
                //EVENT_ALL_DAY_TEXT_LEFT_MARGIN);
        //mExpandAllDayRect.right = Math.min(mExpandAllDayRect.left + allDayIconWidth, mHoursWidth
                //- EVENT_ALL_DAY_TEXT_RIGHT_MARGIN);
        //mExpandAllDayRect.bottom = mFirstCell - EXPAND_ALL_DAY_BOTTOM_MARGIN;
        //mExpandAllDayRect.top = mExpandAllDayRect.bottom
                //- mExpandAlldayDrawable.getIntrinsicHeight();

        // mNumHours�� �̷��� ����ϴ� ���� �´°�?
        // ���� am 12:00�� ���ʷ� �׷����� �Ѵٸ�,,,
        // GRID_LINE_TOP_MARGIN ������ �ʿ��ϱ� ������...
        // ���� ���� 12:00�� �׷��� �Ѵٸ�..
        // �Ʒ� GRID_LINE_BOTTOM_MARGIN�� �ʿ��ϱ� �����̴�...        
        // �Ʒ��� ������ ���� �ʴ�...
        // ���� �Ʒ� ������ �����ϰ� �ʹٸ�...
        mNumHours = mGridAreaHeight / (mCellHeight + HOUR_GAP);/////////////////////////////////////////////////
        mEventGeometry.setHourHeight(mCellHeight);
        
        // ��ģ ���� ���� ������ �������...
        // ��Ȯ�� ����
        // 60min : 107pixels[cell height] = ?min : 48pixels[min event height]
        // :�� cell height��� ���� 1hour�� height�ε� 1hour�� 60min�̴�
        //  �׷��ϱ� 60min�� 107�ȼ��� �Ҵ�Ǹ� 48�ȼ��̸� �� min�ΰ� �ϴ� ���̴�
        //  �̶� min�� mills�� ġȯ?�ؼ� ������� �ϸ�
        // (60 * 60000) : 107 = ? : 48
        // ? * 107 =  (60 * 60000) * 48
        // ? = ((60 * 60000) * 48) / 107
        // ?�̰� minimumDurationMillis ���̴�
        // �ٵ� ��ģ ������ �� �Ʒ�ó�� ������ �ߴ��� ������
        // �׷��� ���� ������ �� ����...
        final long minimumDurationMillis = (long)
                (MIN_EVENT_HEIGHT * DateUtils.MINUTE_IN_MILLIS / (mCellHeight / 60.0f));
        if (INFO) Log.i(TAG, "remeasure:Call Event.computePositions");
        Event.computePositions(mEvents, minimumDurationMillis);

        // Compute the top of our reachable view
        // Max View Height = GRID_LINE_TOP_MARGIN + HOUR_GAP + (24 * (mCellHeight + HOUR_GAP)) + GRID_LINE_BOTTOM_MARGIN
        mMaxViewStartY = (GRID_LINE_TOP_MARGIN + HOUR_GAP + (24 * (mCellHeight + HOUR_GAP)) + GRID_LINE_BOTTOM_MARGIN) - mGridAreaHeight;
        
        // ���⼭ ������....
        // :�Ʒ� ��Ȳ�� �� �߻��Ǵ°� ����........................................................................................................................
        if (mViewStartY > mMaxViewStartY) {
            mViewStartY = mMaxViewStartY;
            computeFirstHour();
        }

        //if (INFO) Log.i(TAG, "remeasure:" + mBaseDate.format2445() + ", first H=" + String.valueOf(mFirstHour));
        
        
        if (mFirstHour == -1) {
            initFirstHour();
            mFirstHourOffset = 0;
        }
		
        
        // When we change the base date, the number of all-day events may
        // change and that changes the cell height.  When we switch dates,
        // we use the mFirstHourOffset from the previous view, but that may
        // be too large for the new view if the cell height is smaller.
        /*if (mFirstHourOffset >= mCellHeight + HOUR_GAP) {
            mFirstHourOffset = mCellHeight + HOUR_GAP - 1;
        }*/        
        
        mViewStartY = ( GRID_LINE_TOP_MARGIN + HOUR_GAP + (mFirstHour * (mCellHeight + HOUR_GAP)) ) - mFirstHourOffset;
        

        //final int eventAreaWidth = mNumDays * (mCellWidth + DAY_GAP);
        //When we get new events we don't want to dismiss the popup unless the event changes
        /*if (mSelectedEvent != null && mLastPopupEventID != mSelectedEvent.id) {
            mPopup.dismiss();
        }
        
        mPopup.setWidth(eventAreaWidth - 20);
        mPopup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);*/
    }

        
    
    private boolean isTouchExplorationEnabled() {
        return mIsAccessibilityEnabled && mAccessibilityMgr.isTouchExplorationEnabled();
    }

    // ���� �츮�� Accessibility Manager�� ������� �ʰ� �ִ�
    /*
    private void sendAccessibilityEventAsNeeded(boolean speakEvents) {
        if (!mIsAccessibilityEnabled) {
            return;
        }
        
        boolean dayChanged = mLastSelectionDayForAccessibility != mSelectionDayForAccessibility;
        boolean hourChanged = mLastSelectionHourForAccessibility != mSelectionStartHourForAccessibility;
        if (dayChanged || hourChanged ||
                mLastSelectedEventForAccessibility != mSelectedEventForAccessibility) {
            mLastSelectionDayForAccessibility = mSelectionDayForAccessibility;
            mLastSelectionHourForAccessibility = mSelectionStartHourForAccessibility;
            mLastSelectedEventForAccessibility = mSelectedEventForAccessibility;

            StringBuilder b = new StringBuilder();

            // Announce only the changes i.e. day or hour or both
            if (dayChanged) {
                b.append(getSelectedTimeForAccessibility().format("%A "));
            }
            if (hourChanged) {
                b.append(getSelectedTimeForAccessibility().format(mIs24HourFormat ? "%k" : "%l%p"));
            }
            if (dayChanged || hourChanged) {
                b.append(PERIOD_SPACE);
            }

            if (speakEvents) {
                if (mEventCountTemplate == null) {
                    mEventCountTemplate = mContext.getString(R.string.template_announce_item_index);
                }

                // Read out the relevant event(s)
                int numEvents = mSelectedEvents.size();
                if (numEvents > 0) {
                    if (mSelectedEventForAccessibility == null) {
                        // Read out all the events
                        int i = 1;
                        for (Event calEvent : mSelectedEvents) {
                            if (numEvents > 1) {
                                // Read out x of numEvents if there are more than one event
                                mStringBuilder.setLength(0);
                                b.append(mFormatter.format(mEventCountTemplate, i++, numEvents));
                                b.append(" ");
                            }
                            appendEventAccessibilityString(b, calEvent);
                        }
                    } else {
                        if (numEvents > 1) {
                            // Read out x of numEvents if there are more than one event
                            mStringBuilder.setLength(0);
                            b.append(mFormatter.format(mEventCountTemplate, mSelectedEvents
                                    .indexOf(mSelectedEventForAccessibility) + 1, numEvents));
                            b.append(" ");
                        }
                        appendEventAccessibilityString(b, mSelectedEventForAccessibility);
                    }
                } else {
                    b.append(mCreateNewEventString);
                }
            }

            if (dayChanged || hourChanged || speakEvents) {
                AccessibilityEvent event = AccessibilityEvent
                        .obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                CharSequence msg = b.toString();
                event.getText().add(msg);
                event.setAddedCount(msg.length());
                sendAccessibilityEventUnchecked(event);
            }
        }
    }
	*/

    /*
    private void appendEventAccessibilityString(StringBuilder b, Event calEvent) {
        b.append(calEvent.getTitleAndLocation());
        b.append(PERIOD_SPACE);
        String when;
        int flags = DateUtils.FORMAT_SHOW_DATE;
        if (calEvent.allDay) {
            flags |= DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY;
        } else {
            flags |= DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(mContext)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
        }
        when = Utils.formatDateRange(mContext, calEvent.startMillis, calEvent.endMillis, flags);
        b.append(when);
        b.append(PERIOD_SPACE);
    }
	*/
    
    
    
    
    // This is called after scrolling stops to move the selected hour
    // to the visible part of the screen.
    private void resetSelectedHour() {
        if (mSelectionStartHour < mFirstHour + 1) {            
            mSelectedEvents.clear();
            //mComputeSelectedEvents = true;
        } else if (mSelectionStartHour > mFirstHour + mNumHours - 3) {            
            mSelectedEvents.clear();
            //mComputeSelectedEvents = true;
        }
    }

    private void initFirstHour() {
    	// �� mNumHours�� 5�� ������ ��?
    	// ������ selection hour�� view start y�� �� �°� ��ġ�ϵ��� �ϴ°� �ƴ϶�
    	// selection hour�� view start y���� ��� ���� �������� �ؼ� �ð������� ���� ���ϰ�? �ϱ� ����???
    	// ���� scroll view�� scale ������ �����̸��� mNumHours�� 17������ �����ϴ�
    	// �츮�� scale ����� ������ �����̹Ƿ� mNumHours / 5�� ������ �������� ��������
    	
    	// nNumHours�� 5�� ������ ����
    	// nNumHours�� 20% ������ height ������ selection hour ���� �ð���� ���� �ְڴٴ� ��
        mFirstHour = mSelectionStartHour - mNumHours / 5;
        if (mFirstHour < 0) {
            mFirstHour = 0;
        } else if (mFirstHour + mNumHours > 24) {
            mFirstHour = 24 - mNumHours;
        }
    }

    /**
     * Recomputes the first full hour that is visible on screen after the
     * screen is scrolled.
     */
    
    private void computeFirstHour() {
            	
    	// mViewStartY�� 0�̶�� ���� visible first hour�� am12�� �ǹ��ϰ� am12 �ð��� ���� GRID_LINE_TOP_MARGIN ������ ù��° hour line�� �����ؾ� ��
    	if (mViewStartY == 0) {
    		mFirstHour = 0; // am 12�ø� �ǹ���
    		mFirstHourOffset = GRID_LINE_TOP_MARGIN + HOUR_GAP;
    	}
    	else {
    		mFirstHour = (mViewStartY + mCellHeight + HOUR_GAP) / (mCellHeight + HOUR_GAP);
    		mFirstHourOffset =  ( GRID_LINE_TOP_MARGIN + HOUR_GAP + (mFirstHour * (mCellHeight + HOUR_GAP)) ) - mViewStartY;
    	}    	
        
        //if (INFO) Log.i(TAG, "computeFirstHour:mSelectionStartHour=" + String.valueOf(mSelectionStartHour) + ", First H=" + String.valueOf(mFirstHour) + ", Offset=" + String.valueOf(mFirstHourOffset));
    }
    
    void clearCachedEvents() {
        //mLastReloadMillis = 0;
    }

    private final Runnable mCancelCallback = new Runnable() {
        public void run() {
            clearCachedEvents();
        }
    };
    
    private final Runnable mEventInfoLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	Log.i("tag", "Called EventInfoLoaderCancelCallback");
        }
    };

    // ���� ȣ���� DayFragment.eventsChanged���� �߻��Ѵ�
    /*
    **reloadEvents callers
    1.DayFragment
     -eventsChanged 
      --onResume
      --handleEvent
     -goTo
    2.DayView
     -initNextView
      --doScroll
      --makeInPlaceAnim
     -switchViews
    */
    //boolean mFirstReloadRequestCompletion = false;
    // set mLastReloadMillis
    // set mEventCursors
    // set mEvents
    // set mLoadedFirstJulianDay
    // set mAllDayEvents
    // set mRemeasure
    // set mComputeSelectedEvents
    // call computeEventRelations()
    // call recalc()
    // call invalidate()
    /*
    public void reloadEvents() {    	
        // Make sure our time zones are up to date
        mTZUpdater.run();

        //mFragment.setSelectedEvent(null);
        //mPrevSelectedEvent = null;
        //mSelectedEvents.clear();

        // The start date is the beginning of the week at 12am
        Time weekStart = new Time(Utils.getTimeZone(mContext, mTZUpdater));
        weekStart.set(mBaseDate);
        weekStart.hour = 0;
        weekStart.minute = 0;
        weekStart.second = 0;
        long millis = weekStart.normalize(true);

        // Avoid reloading events unnecessarily.
        if (millis == mLastReloadMillis) {
            return;
        }
        mLastReloadMillis = millis;

        // load events in the background
//        mContext.startProgressSpinner();
        final ArrayList<Event> events = new ArrayList<Event>();
        mEventCursors.clear();
        mEventLoader.loadEventsInBackground(mNumDays, events, mFirstJulianDay, 
        		new Runnable() {
        	
		            public void run() {		            	
		            	
		                //boolean fadeinEvents = mFirstJulianDay != mLoadedFirstJulianDay;
		                mEvents = events;
		                
		                if (!mEvents.isEmpty())
		                	////////////////////////////////////////////////////////////////
		                	mEventInfoLoader.loadEventsInfoInBackground(mEventCursors, mEvents, mEventInfoLoaderSuccessCallback, mEventInfoLoaderCancelCallback);
		                	////////////////////////////////////////////////////////////////
		                
		                mLoadedFirstJulianDay = mFirstJulianDay;
		                if (mAllDayEvents == null) {
		                    mAllDayEvents = new ArrayList<Event>();
		                } else {
		                    mAllDayEvents.clear();
		                }
		
		                // Create a shorter array for all day events
		                for (Event e : events) {
		                    if (e.drawAsAllday()) {
		                        mAllDayEvents.add(e);
		                    }
		                }
				                
		                computeEventRelations();
		
		                mRemeasure = true;//////////////////////////////////////////////////////////////////////////////////////////////////////////
		                mComputeSelectedEvents = true;
		                recalc();
		
		                // Start animation to cross fade the events
		                //if (fadeinEvents) {
		                    //if (mEventsCrossFadeAnimation == null) {
		                        //mEventsCrossFadeAnimation =
		                                //ObjectAnimator.ofInt(DayView.this, "EventsAlpha", 0, 255);
		                        //mEventsCrossFadeAnimation.setDuration(EVENTS_CROSS_FADE_DURATION);
		                    //}
		                    //mEventsCrossFadeAnimation.start();
		                //} else{
		                if (INFO) Log.i(TAG, "reloadEvents call inavlidate!!!!!!!!");
		                if (!mFirstReloadRequestCompletion)
		                	mFirstReloadRequestCompletion = true;
		                
		                invalidate();////////////////////////////////////////////////////////////////////////////////////////////////////////////
		                //}
		            }
		        }, 
		        mCancelCallback);        
    }
    */
    /*
    public void reloadEventsWithoutCallback() {
    	        
        // Make sure our time zones are up to date
        mTZUpdater.run();

        //mFragment.setSelectedEvent(null);
        //mPrevSelectedEvent = null;
        mSelectedEvents.clear();

        // The start date is the beginning of the week at 12am
        Time weekStart = new Time(Utils.getTimeZone(mContext, mTZUpdater));
        weekStart.set(mBaseDate);
        weekStart.hour = 0;
        weekStart.minute = 0;
        weekStart.second = 0;
        long millis = weekStart.normalize(true);

        // Avoid reloading events unnecessarily.
        if (millis == mLastReloadMillis) {        	
            return;
        }
        mLastReloadMillis = millis;
        
        final ArrayList<Event> events = new ArrayList<Event>();
        mEventCursors.clear();
        //////////////////////////////////////////////////////////////////////////////////////////////////        
        mEventLoader.loadEvents(mContext, mNumDays, events, mFirstJulianDay);        	          	
		            	
        boolean fadeinEvents = mFirstJulianDay != mLoadedFirstJulianDay;
        mEvents = events;
        
        if (!mEvents.isEmpty()) {        	
        	////////////////////////////////////////////////////////////////
        	mEventInfoLoader.loadEvents(mEventCursors, mEvents);
        	////////////////////////////////////////////////////////////////
        }
        
        mLoadedFirstJulianDay = mFirstJulianDay;
        if (mAllDayEvents == null) {
            mAllDayEvents = new ArrayList<Event>();
        } else {
            mAllDayEvents.clear();
        }

        // Create a shorter array for all day events
        for (Event e : events) {
            if (e.drawAsAllday()) {
                mAllDayEvents.add(e);
            }
        }
       
        computeEventRelations();

        mRemeasure = true;
        mComputeSelectedEvents = true;
        recalc();
        
        invalidate();        
		           
    }
	*/
    
    public void setEventsX(ArrayList<Event> events) {
    	mEvents = events;
    	
        computeEventRelations();

        mRemeasure = true;//////////////////////////////////////////////////////////////////////////////////////////////////////////
        //mComputeSelectedEvents = true;        
        
        invalidate();////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
    
    /*
    public EventCursors getEventCursors(long eventId) {
    	int size = mEventCursors.size();
    	EventCursors targetObj = null;
    	for (int i=0; i<size; i++) {
    		EventCursors obj = mEventCursors.get(i);
    		if (eventId == obj.mEventId) {
    			targetObj = obj;
    			break;
    		}                			
    	}
    	
    	return targetObj;
    }
    */
    
    public void setEventsAlpha(int alpha) {
        mEventsAlpha = alpha;
        invalidate();
    }

    public int getEventsAlpha() {
        return mEventsAlpha;
    }

    public void stopEventsAnimation() {
        if (mEventsCrossFadeAnimation != null) {
            mEventsCrossFadeAnimation.cancel();
        }
        mEventsAlpha = 255;
    }

    // callers
    // -reloadEvents
    // -reloadEventsWithoutCallback
    private void computeEventRelations() {
        // Compute the layout relation between each event before measuring cell
        // width, as the cell width should be adjusted along with the relation.
        //
        // Examples: A (1:00pm - 1:01pm), B (1:02pm - 2:00pm)
        // We should mark them as "overwapped". Though they are not overwapped logically, but
        // minimum cell height implicitly expands the cell height of A and it should look like
        // (1:00pm - 1:15pm) after the cell height adjustment.

        // Compute the space needed for the all-day events, if any.
        // Make a pass over all the events, and keep track of the maximum
        // number of all-day events in any one day.  Also, keep track of
        // the earliest event in each day.
        int maxAllDayEvents = 0;
        final ArrayList<Event> events = mEvents;
        final int len = events.size();
        // Num of all-day-events on each day.
        final int eventsCount[] = new int[mLastJulianDay - mFirstJulianDay + 1];
        Arrays.fill(eventsCount, 0);
        for (int ii = 0; ii < len; ii++) {
            Event event = events.get(ii);
            if (event.startDay > mLastJulianDay || event.endDay < mFirstJulianDay) {
                continue;
            }
            if (event.drawAsAllday()) {
                // Count all the events being drawn as allDay events
                final int firstDay = Math.max(event.startDay, mFirstJulianDay);
                final int lastDay = Math.min(event.endDay, mLastJulianDay);
                for (int day = firstDay; day <= lastDay; day++) {
                    final int count = ++eventsCount[day - mFirstJulianDay];
                    if (maxAllDayEvents < count) {
                        maxAllDayEvents = count;
                    }
                }

                int daynum = event.startDay - mFirstJulianDay;
                int durationDays = event.endDay - event.startDay + 1;
                if (daynum < 0) {
                    durationDays += daynum;
                    daynum = 0;
                }
                if (daynum + durationDays > mNumDays) {
                    durationDays = mNumDays - daynum;
                }
                
                //for (int day = daynum; durationDays > 0; day++, durationDays--) {
                    //mHasAllDayEvent[day] = true;
                //}
            } else {
                int daynum = event.startDay - mFirstJulianDay;
                int hour = event.startTime / 60;
                if (daynum >= 0 && hour < mEarliestStartHour[daynum]) {
                    mEarliestStartHour[daynum] = hour;
                }

                // Also check the end hour in case the event spans more than
                // one day.
                daynum = event.endDay - mFirstJulianDay;
                hour = event.endTime / 60;
                if (daynum < mNumDays && hour < mEarliestStartHour[daynum]) {
                    mEarliestStartHour[daynum] = hour;
                }
            }
        }
        //mMaxAlldayEvents = maxAllDayEvents;
        //initAllDayHeights();
    }

    
    public void setPsudoActionBarViewVisible() {
    	mVisiblePsudoActionBarView = true;
    }
    
    // Android lint tool
    @SuppressLint("WrongCall") 
    @Override
    protected void onDraw(Canvas canvas) {    		
    	//if (INFO) Log.i(TAG, "+onDraw:" + mBaseDate.format2445());
    	// for test...
    	// mEvents�� ���� empty ��Ȳ�� �����ϱ�?
    	if (mAmINextView) {
    		if (INFO) Log.i(TAG, "bbbbbb");
    		//return ;
    	}
    		
        if (mRemeasure) {
        	//if (INFO) Log.i(TAG, "onDraw:call remeasure:" + mBaseDate.format2445());
        	//if (INFO) Log.i(TAG, "onDraw:call remeasure");
            remeasure(getWidth(), getHeight());
            mRemeasure = false;
        }
        canvas.save();

        //float yTranslate = -mViewStartY + DAY_HEADER_HEIGHT + mAlldayHeight;
        float yTranslate = -mViewStartY + mFirstCell;
        // offset canvas by the current drag and header position        
        // Ŭ���� ������ ���� view�� �׸��� ����!!!
        // translate�� dy ���� -���� �ָ�
        // Ŭ���� ������ ���� ���� �ø��� ȿ��?�� ��
        // [0,0]�� ���� �̵�?�ϱ� ������...
        canvas.translate(0, yTranslate);//canvas.translate(-mViewStartX, yTranslate);
                
        // clip to everything below the allDay area
        Rect dest = mDestRect;
        dest.top = (int) (mFirstCell - yTranslate);        
        dest.bottom = (int) (mViewHeight - yTranslate); // �� �̷��� ����������? �ܼ��� �Ʒ�ó�� dest.top�� mViewHeight�� �� �ϸ� �� �Ž�...
                                                          // :�ƴϴ� ���� mAlldayHeight�� 0�� �ƴ϶�� ������ �ȴ�
        //dest.bottom = dest.top + mViewHeight;
        dest.left = 0;
        dest.right = mViewWidth;
        canvas.save();
        // Intersect the current clip with the specified rectangle, which is expressed in local coordinates
        canvas.clipRect(dest);
        
        // Draw the movable part of the view
        int dontDrawHour = doDraw(canvas);///////////////////////////////////////////////////////////////////////////////////
        
        // restore to having no clip
        canvas.restore();
        /*
        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0 || (mTouchMode & TOUCH_MODE_RETURN_IN_PLACE) != 0) {
            float xTranslate;
            
            if (mViewStartX > 0) {
                xTranslate = mViewWidth;
            } else {
                xTranslate = -mViewWidth;
            }
            // Move the canvas around to prep it for the next view
            // specifically, shift it by a screen and undo the
            // yTranslation which will be redone in the nextView's onDraw().
            canvas.translate(xTranslate, -yTranslate);////////////////////////////////////////////////////////////////
            
            DayView nextView = (DayView) mViewSwitcher.getNextView();
            if (nextView.mMaxAlldayEvents != 0) {
            	//nextView.mAllDayEventLists.setTranslationX(xTranslate);
            	//nextView.mAllDayEventLists.setX(xTranslate);
            }
            
            // Prevent infinite recursive calls to onDraw().
            nextView.mTouchMode = TOUCH_MODE_INITIAL_STATE;

            // onDraw�Լ��� @SuppressLint("WrongCall") �߰�!!!
            nextView.onDraw(canvas); 
            
            // Move it back for this view
            canvas.translate(-xTranslate, 0);
            
        } else */{
            // If we drew another view we already translated it back
            // If we didn't draw another view we should be at the edge of the
            // screen
        	canvas.translate(0, yTranslate);//canvas.translate(mViewStartX, -yTranslate);
        }

        // Draw the fixed areas (that don't scroll) directly to the canvas.
        drawAfterScroll(canvas, dontDrawHour);
        
        /*
        if (mComputeSelectedEvents && mUpdateToast) {
            updateEventDetails();
            mUpdateToast = false;
        }
        */
        //mComputeSelectedEvents = false;

        // Draw overscroll glow
        if (!mEdgeEffectTop.isFinished()) {
            if (DAY_HEADER_HEIGHT != 0) {
                canvas.translate(0, DAY_HEADER_HEIGHT);
            }
            if (mEdgeEffectTop.draw(canvas)) {
                invalidate();
            }
            if (DAY_HEADER_HEIGHT != 0) {
                canvas.translate(0, -DAY_HEADER_HEIGHT);
            }
        }
        if (!mEdgeEffectBottom.isFinished()) {
            canvas.rotate(180, mViewWidth/2, mViewHeight/2);
            if (mEdgeEffectBottom.draw(canvas)) {
                invalidate();
            }
        }
        canvas.restore();
        
        //if (INFO) Log.i(TAG, "-onDraw");
    }

    private void drawAfterScroll(Canvas canvas, int dontDrawHour) {
        Paint p = mPaint;
        Rect r = mRect;

        drawAllDayHighlights(r, canvas, p);
        //if (mMaxAlldayEvents != 0) {
        	//Log.i("tag", "drawAfterScroll:mMaxAlldayEvents=" + String.valueOf(mMaxAlldayEvents));
            //drawAllDayEvents(mFirstJulianDay, mNumDays, canvas, p);
            //drawUpperLeftCorner(r, canvas, p);
        //}

        // all-day�� �����ϸ� all-day ������ ���� Ÿ�Ӷ��� ����(��ũ�� ���� ����)�� ������ ��� �Ѵ�
        drawScrollLine(r, canvas, p);
        drawDayHeaderLoop(r, canvas, p);

        // Draw the AM and PM indicators if we're in 12 hour mode
        if (!mIs24HourFormat) {
            drawAmPm(canvas, p, dontDrawHour);
        }
    }

    // This isn't really the upper-left corner. It's the square area just
    // below the upper-left corner, above the hours and to the left of the
    // all-day area.
    private void drawUpperLeftCorner(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);
        /*
        if (mMaxAlldayEvents > mMaxUnexpandedAlldayEventCount) {
            // Draw the allDay expand/collapse icon
            if (mUseExpandIcon) {
                mExpandAlldayDrawable.setBounds(mExpandAllDayRect);
                mExpandAlldayDrawable.draw(canvas);
            } else {
                mCollapseAlldayDrawable.setBounds(mExpandAllDayRect);
                mCollapseAlldayDrawable.draw(canvas);
            }
        }
        */
    }

    
    private void drawScrollLine(Rect r, Canvas canvas, Paint p) {
        final int right = computeDayLeftPosition(mNumDays);
        final int y = mFirstCell - 1;

        p.setAntiAlias(false);
        p.setStyle(Style.FILL);

        p.setColor(mCalendarGridLineInnerHorizontalColor);
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        canvas.drawLine(GRID_LINE_LEFT_MARGIN, y, right, y, p);
        p.setAntiAlias(true);
    }

    // Computes the x position for the left side of the given day (base 0)
    private int computeDayLeftPosition(int day) {
        int effectiveWidth = mViewWidth - mHoursWidth;
        return day * effectiveWidth / mNumDays + mHoursWidth;
    }

    private void drawAllDayHighlights(Rect r, Canvas canvas, Paint p) {
        if (mFutureBgColor != 0) {
            // First, color the labels area light gray
            r.top = 0;
            r.bottom = DAY_HEADER_HEIGHT;
            r.left = 0;
            r.right = mViewWidth;
            //p.setColor(mBgColor);
            int allDayBG = getResources().getColor(R.color.manageevent_actionbar_background);
            p.setColor(allDayBG);
            p.setStyle(Style.FILL);
            canvas.drawRect(r, p);
            // and the area that says All day
            r.top = DAY_HEADER_HEIGHT;
            r.bottom = mFirstCell - 1;
            r.left = 0;
            r.right = mHoursWidth;
            canvas.drawRect(r, p);

            int startIndex = -1;

            int todayIndex = mTodayJulianDay - mFirstJulianDay;
            if (todayIndex < 0) {
                // Future
                startIndex = 0;
            } else if (todayIndex >= 1 && todayIndex + 1 < mNumDays) {
                // Multiday - tomorrow is visible.
                startIndex = todayIndex + 1;
            }

            if (startIndex >= 0) {
                // Draw the future highlight
                r.top = 0;
                r.bottom = mFirstCell - 1;
                r.left = computeDayLeftPosition(startIndex) + 1;
                r.right = computeDayLeftPosition(mNumDays);
                p.setColor(mFutureBgColor);
                p.setStyle(Style.FILL);
                canvas.drawRect(r, p);
            }
        }

        /*
        if (mSelectionAllday) {
            // Draw the selection highlight on the selected all-day area
            mRect.top = DAY_HEADER_HEIGHT + 1;
            mRect.bottom = mRect.top + mAlldayHeight + ALLDAY_TOP_MARGIN - 2;
            int daynum = mSelectionDay - mFirstJulianDay;
            mRect.left = computeDayLeftPosition(daynum) + 1;
            mRect.right = computeDayLeftPosition(daynum + 1);
            p.setColor(mCalendarGridAreaSelected);
            canvas.drawRect(mRect, p);
        }
        */
    }

    private void drawDayHeaderLoop(Rect rec, Canvas canvas, Paint p) {
        // Draw the horizontal day background banner
        // p.setColor(mCalendarDateBannerBackground);
        // r.top = 0;
        // r.bottom = DAY_HEADER_HEIGHT;
        // r.left = 0;
        // r.right = mHoursWidth + mNumDays * (mCellWidth + DAY_GAP);
        // canvas.drawRect(r, p);
        //
        // Fill the extra space on the right side with the default background
        // r.left = r.right;
        // r.right = mViewWidth;
        // p.setColor(mCalendarGridAreaBackground);
        // canvas.drawRect(r, p);
    	// ONE_DAY_HEADER_HEIGHT�� �׻� 0�̴�!!!
        if (mNumDays == 1 && ONE_DAY_HEADER_HEIGHT == 0) {
            return;
        }

        /*
        p.setTypeface(mBold);
        p.setTextAlign(Paint.Align.RIGHT);
        int cell = mFirstJulianDay;

        String[] dayNames;
        if (mDateStrWidth < mCellWidth) {
            dayNames = mDayStrs;
        } else {
            dayNames = mDayStrs2Letter;
        }

        p.setAntiAlias(true);
        for (int day = 0; day < mNumDays; day++, cell++) {
            int dayOfWeek = day + mFirstVisibleDayOfWeek;
            if (dayOfWeek >= 14) {
                dayOfWeek -= 14;
            }

            int color = mCalendarDateBannerTextColor;
            if (mNumDays == 1) { // �̷� ��찡 �߻��� �� �մ°�??? mNumDays�� 1�̰� ONE_DAY_HEADER_HEIGHT�� 0�̸� �� �Լ��� ��ٷ� �����Ѵ�
            	                 // ONE_DAY_HEADER_HEIGHT�� �ٰ� ��� 0�̴�
                if (dayOfWeek == Calendar.SATURDAY) {
                    color = mWeek_saturdayColor;
                } else if (dayOfWeek == Calendar.SUNDAY) {
                    color = mWeek_sundayColor;
                }
            } else {
                final int column = day % 7;
                if (Utils.isSaturday(column, mFirstDayOfWeek)) {
                    color = mWeek_saturdayColor;
                } else if (Utils.isSunday(column, mFirstDayOfWeek)) {
                    color = mWeek_sundayColor;
                }
            }

            p.setColor(color);
            drawDayHeader(dayNames[dayOfWeek], day, cell, canvas, p);
        }
        p.setTypeface(null);
        */
    }

    private void drawAmPm(Canvas canvas, Paint p, int dontDrawHour) {
    	boolean drawAm = true;
    	boolean drawPm = true;
    	
    	if (dontDrawHour != -1) {
    		if (dontDrawHour < 12) {
        		drawAm = false;
            }
        	else {
        		drawPm = false;
        	}   
        }        

        p.setColor(mCalendarAmPmLabel);
        p.setTextSize(AMPM_TEXT_SIZE);
        p.setTypeface(mBold);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.RIGHT);
        
        String text = mAmString;
        if (mFirstHour >= 12) {
            text = mPmString;
        }
        
        float x = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN - mMaxHourDecimalTextWidth - (NEW_HOURS_RIGHT_MARGIN / 2);          
        int y = mFirstCell + mFirstHourOffset + mHoursTextHeight + HOUR_GAP;        
        //int y = mFirstCell + mFirstHourOffset + 2 * mHoursTextHeight + HOUR_GAP;       
        
        if (drawAm || drawPm)
        	canvas.drawText(text, x, y, p);
        
        if (drawPm)
	        if (mFirstHour < 12 && mFirstHour + mNumHours > 12) {
	            // Also draw the "PM"
	            text = mPmString;
	            /*y = mFirstCell + mFirstHourOffset + (12 - mFirstHour) * (mCellHeight + HOUR_GAP)
	                    + 2 * mHoursTextHeight + HOUR_GAP;
	             canvas.drawText(text, HOURS_LEFT_MARGIN, y, p)       
	            */
	            y = mFirstCell + mFirstHourOffset + (12 - mFirstHour) * (mCellHeight + HOUR_GAP)
	                    + mHoursTextHeight + HOUR_GAP;            
	            canvas.drawText(text, x, y, p);
	        }
    }

    private void drawCurrentTimeLine(Rect r, final int day, final int top, Canvas canvas,
            Paint p) {
        r.left = computeDayLeftPosition(day) - CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        r.right = computeDayLeftPosition(day + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;

        r.top = top - CURRENT_TIME_LINE_TOP_OFFSET;
        r.bottom = r.top + mCurrentTimeLine.getIntrinsicHeight();

        mCurrentTimeLine.setBounds(r);
        mCurrentTimeLine.draw(canvas);
        if (mAnimateToday) {
            mCurrentTimeAnimateLine.setBounds(r);
            mCurrentTimeAnimateLine.setAlpha(mAnimateTodayAlpha);
            mCurrentTimeAnimateLine.draw(canvas);
        }
    }
    
    private void drawCurrentTimeTextAndLine(Rect r, final int day, final int top, Canvas canvas,
            Paint p) {
    	Calendar curTimeCalendar = (Calendar) GregorianCalendar.getInstance(mCurrentTime.getTimeZone());
    	//curTimeCalendar.setTimeZone(mCurrentTime.getTimeZone());
    	curTimeCalendar.setTimeInMillis(mCurrentTime.getTimeInMillis());
    	
    	p.setColor(Color.RED);
        p.setTextSize(AMPM_TEXT_SIZE);
        p.setTypeface(mBold);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.RIGHT);        
        
        String meridiem = mAmString;
        if (curTimeCalendar.get(Calendar.AM_PM) == Calendar.PM) {
        	meridiem = mPmString;
        }        
        String space = " ";
        String hour = String.valueOf(curTimeCalendar.get(Calendar.HOUR_OF_DAY));
        String colon = ":";
        String minute = String.valueOf(curTimeCalendar.get(Calendar.MINUTE));
        if (curTimeCalendar.get(Calendar.MINUTE) < 10) {
        	minute = "0" + minute;
        }        
        String currentTime = meridiem + space + hour + colon + minute;
        float text_x = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN;
        float text_y = top - CURRENT_TIME_LINE_TOP_OFFSET + (mAMPMTextHeight / 2);
        
        canvas.drawText(currentTime, text_x, text_y, p);    	
    	
        r.left = computeDayLeftPosition(day) - CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        r.right = computeDayLeftPosition(day + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        r.top = top - CURRENT_TIME_LINE_TOP_OFFSET;
        r.bottom = r.top + mCurrentTimeLine.getIntrinsicHeight();

        mCurrentTimeLine.setBounds(r);
        mCurrentTimeLine.draw(canvas);
        if (mAnimateToday) {
            mCurrentTimeAnimateLine.setBounds(r);
            mCurrentTimeAnimateLine.setAlpha(mAnimateTodayAlpha);
            mCurrentTimeAnimateLine.draw(canvas);
        }
    }

    private int doDraw(Canvas canvas) {
        Paint p = mPaint;
        Rect r = mRect;
        
        drawGridBackground(r, canvas, p);
        int dontDrawHour = drawHours(r, canvas, p);

        // Draw each day
        int cell = mFirstJulianDay;
        p.setAntiAlias(false);
        int alpha = p.getAlpha();
        p.setAlpha(mEventsAlpha);
        for (int day = 0; day < mNumDays; day++, cell++) {
            // TODO Wow, this needs cleanup. drawEvents loop through all the
            // events on every call.
            //drawEvents(cell, day, HOUR_GAP, canvas, p); // +commented by intheeast
        	drawEvents(cell, day, GRID_LINE_TOP_MARGIN + HOUR_GAP, canvas, p); // +added by intheeast
            // If this is today
            if (cell == mTodayJulianDay) {
                /*int lineY = mCurrentTime.hour * (mCellHeight + HOUR_GAP)
                        + ((mCurrentTime.minute * mCellHeight) / 60) + 1;*/ // +commented by intheeast
            	int lineY = GRID_LINE_TOP_MARGIN 
            			+ HOUR_GAP
            			+ mCurrentTime.get(Calendar.HOUR_OF_DAY) * (mCellHeight + HOUR_GAP)
                        + ((mCurrentTime.get(Calendar.MINUTE) * mCellHeight) / 60); // +added by intheeast
                // And the current time shows up somewhere on the screen
                if (lineY >= mViewStartY && lineY < mViewStartY + mViewHeight - 2) {
                    //drawCurrentTimeLine(r, day, lineY, canvas, p); // +commented by intheeast
                	drawCurrentTimeTextAndLine(r, day, lineY, canvas, p); // +added by intheeast                	
                }
            }
        }
        p.setAntiAlias(true);
        p.setAlpha(alpha);

        // new event creation�� ���� selected rect�� drawing ��
        drawSelectedRect(r, canvas, p);
        
        return dontDrawHour;
    }

    private void drawSelectedRect(Rect r, Canvas canvas, Paint p) {
        // Draw a highlight on the selected hour (if needed)
        if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT /*&& !mSelectionAllday*/) {
            int daynum = mSelectionDay - mFirstJulianDay; // one day ���������� daynum�� �׻� 0�̴�
            
            int minutesPixel = 0;
            switch(mSelectionStartMinutes) {
            case 0:
            	minutesPixel = 0;
            	break;
            case 15:
            	minutesPixel = mOneQuartersOfHourPixelHeight;
            	break;
            case 30:
            	minutesPixel = 2* mOneQuartersOfHourPixelHeight;
            	break;
            case 45:
            	minutesPixel = 3* mOneQuartersOfHourPixelHeight;
            	break;
            }
            
            
            r.top = GRID_LINE_TOP_MARGIN + HOUR_GAP + mSelectionStartHour * (mCellHeight + HOUR_GAP) + minutesPixel;   
            r.bottom = r.top + mCellHeight; 
            r.left = computeDayLeftPosition(daynum) + 1;
            r.right = computeDayLeftPosition(daynum + 1) + 1;

            saveSelectionPosition(r.left, r.top, r.right, r.bottom);

            // Draw the highlight on the grid
            p.setColor(mCalendarGridAreaSelected);            
            r.right -= DAY_GAP;
            p.setAntiAlias(false);
            //canvas.drawRect(r, p);            

            // Draw a "new event hint" on top of the highlight
            // For the week view, show a "+", for day view, show "+ New event"
            p.setColor(mNewEventHintColor);
            if (mNumDays > 1) {
                p.setStrokeWidth(NEW_EVENT_WIDTH);
                int width = r.right - r.left;
                int midX = r.left + width / 2;
                int midY = r.top + mCellHeight / 2;
                int length = Math.min(mCellHeight, width) - NEW_EVENT_MARGIN * 2;
                length = Math.min(length, NEW_EVENT_MAX_LENGTH);
                int verticalPadding = (mCellHeight - length) / 2;
                int horizontalPadding = (width - length) / 2;
                //canvas.drawLine(r.left + horizontalPadding, midY, r.right - horizontalPadding,
                        //midY, p);
                //canvas.drawLine(midX, r.top + verticalPadding, midX, r.bottom - verticalPadding, p);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setTextSize(NEW_EVENT_HINT_FONT_SIZE);
                p.setTextAlign(Paint.Align.LEFT);
                p.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                //canvas.drawText(mNewEventHintString, r.left + EVENT_TEXT_LEFT_MARGIN,
                        //r.top + Math.abs(p.getFontMetrics().ascent) + EVENT_TEXT_TOP_MARGIN , p);
            }
            
            if (mSelectionStartMinutes == 0) { // 0~15�� ���̿��� hour �ؽ�Ʈ�� ��ĥ ���ɼ��� �ֱ� ������ drawing ���� �ʴ´�
            	return;
            }
            else if (mSelectionStartMinutes == 45) { // 45~0�� ���̿��� hour �ؽ�Ʈ�� ��ĥ ���ɼ��� �ֱ� ������ drawing ���� �ʴ´�
            	                                // �׷��� ��ġ�� �ʴ� ���������� drawing �Ѵ�
            	
            	int hourTextTop = mCellHeight - (mHoursTextHeight / 2);
            	int selectedMinuteTextBottom = mMinutuePosPixelAtSelectedEventMove + (mHoursTextHeight / 2);
            	
            	// hour �ؽ�Ʈ�� ��ġ�Ƿ� drawing ���� �ʴ´�
            	if (selectedMinuteTextBottom > hourTextTop)
            		return;
            }            
            
        	float minuteTextX = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN;
            float minuteTexty = r.top + (mHoursTextHeight/2);
            String minuteText = ":" + String.valueOf(mSelectionStartMinutes);            
            setupSelectedMinutesTextPaint(p);
            canvas.drawText(minuteText, minuteTextX, minuteTexty, p);       
        }
        else if ((mFragment.getSelectionMode() == SELECTION_NEW_EVENT_MOVED || mFragment.getSelectionMode() == SELECTION_EVENT_MOVED)/* && !mSelectionAllday*/) {
        	
        	//int daynum = mSelectionDay - mFirstJulianDay; // one day ���������� daynum�� �׻� 0�̴�
                        
        	r.top = GRID_LINE_TOP_MARGIN + HOUR_GAP + mSelectionStartHour * (mCellHeight + HOUR_GAP) + mMinutuePosPixelAtSelectedEventMove;                         
            r.bottom = r.top + mCellHeight; // +added by intheeast           
            
            int effectiveWidth = mViewWidth - mHoursWidth;                   
    		r.left = mFragment.getPrvNewOrSelectedEventLeft();
    		r.right = r.left + effectiveWidth;    
    		
            saveSelectionPosition(r.left, r.top, r.right, r.bottom);        	
        	
            // Draw the highlight on the grid
            p.setColor(mCalendarGridAreaSelected);            
            r.right -= DAY_GAP;
            p.setAntiAlias(false);
            //canvas.drawRect(r, p);

            // Draw a "new event hint" on top of the highlight
            // For the week view, show a "+", for day view, show "+ New event"
            p.setColor(mNewEventHintColor);
            if (mNumDays > 1) {
                p.setStrokeWidth(NEW_EVENT_WIDTH);
                int width = r.right - r.left;
                int midX = r.left + width / 2;
                int midY = r.top + mCellHeight / 2;
                int length = Math.min(mCellHeight, width) - NEW_EVENT_MARGIN * 2;
                length = Math.min(length, NEW_EVENT_MAX_LENGTH);
                int verticalPadding = (mCellHeight - length) / 2;
                int horizontalPadding = (width - length) / 2;
                //canvas.drawLine(r.left + horizontalPadding, midY, r.right - horizontalPadding,
                        //midY, p);
                //canvas.drawLine(midX, r.top + verticalPadding, midX, r.bottom - verticalPadding, p);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setTextSize(NEW_EVENT_HINT_FONT_SIZE);
                p.setTextAlign(Paint.Align.LEFT);
                p.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                //canvas.drawText(mNewEventHintString, r.left + EVENT_TEXT_LEFT_MARGIN,
                        //r.top + Math.abs(p.getFontMetrics().ascent) + EVENT_TEXT_TOP_MARGIN , p);
            }
                        
            
            if (mSelectionStartMinutes == 0) { // 0~15�� ���̿��� hour �ؽ�Ʈ�� ��ĥ ���ɼ��� �ֱ� ������ drawing ���� �ʴ´�
            	return;
            }
            else if (mSelectionStartMinutes == 45) { // 45~0�� ���̿��� hour �ؽ�Ʈ�� ��ĥ ���ɼ��� �ֱ� ������ drawing ���� �ʴ´�
            	                                // �׷��� ��ġ�� �ʴ� ���������� drawing �Ѵ�
            	
            	int hourTextTop = mCellHeight - (mHoursTextHeight / 2);
            	int selectedMinuteTextBottom = mMinutuePosPixelAtSelectedEventMove + (mHoursTextHeight / 2);
            	
            	// hour �ؽ�Ʈ�� ��ġ�Ƿ� drawing ���� �ʴ´�
            	if (selectedMinuteTextBottom > hourTextTop)
            		return;
            }            
            
        	float minuteTextX = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN;
            float minuteTexty = r.top + (mHoursTextHeight/2);
            String minuteText = ":" + String.valueOf(mSelectionStartMinutes);            
            setupSelectedMinutesTextPaint(p);
            canvas.drawText(minuteText, minuteTextX, minuteTexty, p);            
        }
        else if (mScrolling && mFragment.getSelectionMode() == SELECTION_EVENT_FLOATING/*  && !mSelectionAllday*/) {
        	
        	float selectedEventRectY = 0;
        	
        	int minutesPixel = 0;
            switch(mSelectionStartMinutes) {
            case 0:
            	minutesPixel = 0;
            	break;
            case 15:
            	minutesPixel = mOneQuartersOfHourPixelHeight;
            	break;
            case 30:
            	minutesPixel = 2* mOneQuartersOfHourPixelHeight;
            	break;
            case 45:
            	minutesPixel = 3* mOneQuartersOfHourPixelHeight;
            	break;
            }
            // mFirstHourOffset���� mFirstHour�� mSelectedEventPanel������ Y���� ���� �� �ִ�
            // (mSelectionStartHour - mFirstHour)���� selected event rect�� ��ġ�� Top�� mFirstHour�κ��� �󸶳� ������ �ִ��� �� �� �ִ�        	
            selectedEventRectY = mFirstHourOffset + ((mSelectionStartHour - mFirstHour) * (mCellHeight + HOUR_GAP)) + minutesPixel;
            // setY�� �����Ǵ� ���� mSelectedEventPanel���� y���̴�
            //mViewStartX
            int outLineY = (int) (selectedEventRectY - mFragment.getSelectedEventRect().getTopMargin());
            mFragment.getSelectedEventRect().setY(outLineY);
        }
        else if (mFragment.getSelectionMode() == SELECTION_EVENT_EXPAND_SELECTION) {           
        	
            r.top = GRID_LINE_TOP_MARGIN + HOUR_GAP + mSelectionStartHour * (mCellHeight + HOUR_GAP) + mMinutuePosPixelAtSelectedEventMove;  
            r.bottom = GRID_LINE_TOP_MARGIN + HOUR_GAP + mSelectionEndHour * (mCellHeight + HOUR_GAP) + mSelectionEndMinutesPixels;  
            //r.bottom = r.top + getSelectedEventRectHeight();       
            
            int effectiveWidth = mViewWidth - mHoursWidth;                   
    		r.left = mFragment.getPrvNewOrSelectedEventLeft();
    		r.right = r.left + effectiveWidth;    

            saveSelectionPosition(r.left, r.top, r.right, r.bottom);

            // Draw the highlight on the grid
            p.setColor(mCalendarGridAreaSelected);            
            r.right -= DAY_GAP;
            p.setAntiAlias(false);
            canvas.drawRect(r, p);   
            drawExpandingCircleIcon(canvas, p);
        }
        else if (mFragment.getSelectionMode() == SELECTION_EVENT_EXPANDING) {           
            
            r.top = GRID_LINE_TOP_MARGIN + HOUR_GAP + mSelectionStartHour * (mCellHeight + HOUR_GAP) + mMinutuePosPixelAtSelectedEventMove;  
            r.bottom = GRID_LINE_TOP_MARGIN + HOUR_GAP + mSelectionEndHour * (mCellHeight + HOUR_GAP) + mSelectionEndMinutesPixels; 
            //r.bottom = r.top + getSelectedEventRectHeight();        
            
            int effectiveWidth = mViewWidth - mHoursWidth;                   
    		r.left = mFragment.getPrvNewOrSelectedEventLeft();
    		r.right = r.left + effectiveWidth;    

            saveSelectionPosition(r.left, r.top, r.right, r.bottom);

            // Draw the highlight on the grid
            p.setColor(mCalendarGridAreaSelected);            
            r.right -= DAY_GAP;
            p.setAntiAlias(false);
            canvas.drawRect(r, p);    
            
            {
            	Paint eventTextPaint = mEventTextPaint;
            	r.top = (int) r.top + EVENT_RECT_TOP_MARGIN;
                r.bottom = (int) r.bottom - EVENT_RECT_BOTTOM_MARGIN;        
                r.left = (int) r.left + EVENT_RECT_LEFT_MARGIN;
                r.right = (int) r.right;//r.right = (int) event.right - EVENT_RECT_RIGHT_MARGIN;
                setupTextRect(r);
                
            	boolean twoLines = true;
            	int eventHeight = (int) (r.bottom - r.top);
            	// event rect height�� two text line�� ������ �� ���ٸ� one text line���� ���� �Ѵ�
            	if (eventHeight < mMinEventRectHeightTwoLinesEventText) { 
            		twoLines = false;
                }
            	
            	// event rect height�� one text line�� �����ϴµ�..
            	// EVENT_TEXT_FONT_SIZE �� text height�� ������ �� ���� ����
            	// event rect height�� �°� event font size�� �缳���ؾ� �Ѵ�
            	if (eventHeight < mAvailEventRectHeightByDefaultEventTextHeight) { 
            		// event height�� 50% size�� ����� : 
            		float eventFontSize = getEventFontSize(eventHeight, 0.5f);
            		if (eventFontSize > EVENT_TEXT_FONT_SIZE) {
            			if (INFO) Log.i(TAG, "What the hell..����������");
            		}
            		
            		eventTextPaint.setTextSize(eventFontSize);            		
                }            	     	
            	
                float availWidth = r.width();         	
                String title = getEventTitle(mFragment.getSelectedEvent(), eventTextPaint, availWidth);                
                
                // title�� ONE DOT LEADER�� ��ü�� �� ���� ��ŭ ������ �����ϴٴ� ���̹Ƿ�
                // �ƿ� �ؽ�Ʈ�� ������� �õ����� ���ʿ��ϴ�
                if (!title.isEmpty()) {
                	  	                            	
                	
                	float locationTextAvailWidth = availWidth;
                	if (!twoLines) {
                		float titleTextConsumedWidth = eventTextPaint.measureText(title);
                		String space = " ";
                		float spaceWidth = eventTextPaint.measureText(space);
                		locationTextAvailWidth = availWidth - titleTextConsumedWidth - spaceWidth;            		
                	}            	            	
                	
                	String location = getEventLocation(mFragment.getSelectedEvent(), eventTextPaint, locationTextAvailWidth);
                	
    	            // TODO: not sure why we are 4 pixels off
    	            drawEventTitleLocationText(title, location, twoLines, r, canvas, eventTextPaint, 
    	            		mViewStartY/* + 4*/, 
    	            		mViewStartY + mViewHeight - DAY_HEADER_HEIGHT/* - mAlldayHeight*/);      
    	            
                }
                
                // font size recovery!!!
                //if (eventHeight < mTwoQuartersOfHourPixelHeight) {
            		eventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
                //}
            }
            
            drawExpandingCircleIcon(canvas, p);
        }
    }
    
    public void drawExpandingCircleIcon(Canvas canvas, Paint p) {
    	// ��� circle
		canvas.drawCircle(mPrevBox.left + mFragment.getSelectedEventRect().getUpperExpandCircleCenterX(), 
				mPrevBox.top, 
				mFragment.getSelectedEventRect().getRadiusExpandCircle(), 
				p);
		
		mPaint.setColor(Color.WHITE);
		canvas.drawCircle(mPrevBox.left + mFragment.getSelectedEventRect().getUpperExpandCircleCenterX(), 
				mPrevBox.top, 
				mFragment.getSelectedEventRect().getRadiusExpandInnerCircle(), 
				p); // inner cirlce
		
		// �ϴ� cicle
		mPaint.setColor(mCalendarGridAreaSelected);
		canvas.drawCircle(mPrevBox.left + mFragment.getSelectedEventRect().getLowerExpandCircleCenterX(), 
				mPrevBox.bottom, 
				mFragment.getSelectedEventRect().getRadiusExpandCircle(), 
				p);
		
		mPaint.setColor(Color.WHITE);
		canvas.drawCircle(mPrevBox.left + mFragment.getSelectedEventRect().getLowerExpandCircleCenterX(), 
				mPrevBox.bottom, 
				mFragment.getSelectedEventRect().mRadiusExpandInnerCircle, 
				p); // inner cirlce
    }
    
    

    /*
    private void drawHours(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);

        int y = HOUR_GAP + mHoursTextHeight + HOURS_TOP_MARGIN;

        for (int i = 0; i < 24; i++) {
            String time = mHourStrs[i];
            canvas.drawText(time, HOURS_LEFT_MARGIN, y, p);
            y += mCellHeight + HOUR_GAP;
        }
    }
    */ // +commented by intheeast
    
    // +added by intheeast
    // 1. cell height�� ũ�⸦ ���Ѵ�
    // 2. �ش� view�� ���� ���ϰ� ��ġ�ϴ°�?
    private int drawHours(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);

        //boolean checkCurrentTimeText = false;
        int dontDrawHour = -1;
        
        //if (mNumDays == 1) {
        	if (mFirstJulianDay == mTodayJulianDay) {
        		//checkCurrentTimeText = true;        		
        		// mHoursTextHeight
        		// mAMPMTextHeight
        		// ������ ����ϸ� �ȵȴ�!!!
        		float cellHeight = (float)mCellHeight;
        		float hourGap = (float)HOUR_GAP;
        		
        		float oneMinuteHeight = (cellHeight + hourGap) / 60;
        		float curMinutePos = mCurrentTime.get(Calendar.MINUTE) * oneMinuteHeight;
        		// mHoursTextHeight��
        		// mHoursTextHeight = (int)Math.abs(p.ascent());  
        		// �� ����������...intheeast�� mHoursTextHeight�� ������� �ʰ� 
        		// mRealHoursTextHeight�� ����/�����Ͽ� ����Ѵ�
        		float realHoursTextHeight = (float)mRealHoursTextHeight;
        		float upperLimitMinute = (realHoursTextHeight / 2); // 0�п��� ?�б���
        		float lowerLimitMinute = (cellHeight + hourGap) - (realHoursTextHeight / 2); // ���� hour + 1 �ð����� ���� hour:?minute ����
        		
        		if (curMinutePos < upperLimitMinute)
        			dontDrawHour = mCurrentTime.get(Calendar.HOUR_OF_DAY);
        		else if (curMinutePos >= lowerLimitMinute)
        			dontDrawHour = mCurrentTime.get(Calendar.HOUR_OF_DAY) + 1;
        	}
        //}
        //else {
        	
        //}
        
        float x = GRID_LINE_LEFT_MARGIN - NEW_HOURS_RIGHT_MARGIN; 
        int y = HOUR_GAP + mHoursTextHeight + HOURS_TOP_MARGIN;

        for (int i = 0; i < 25; i++) {
        	if (/*checkCurrentTimeText &&*/ (dontDrawHour != -1)) {
        		if (dontDrawHour == i) {
        			y += mCellHeight + HOUR_GAP;
        			continue;
        		}
        	}
        	
            String time = mHourStrs[i];
            
            canvas.drawText(time, x, y, p);  
            y += mCellHeight + HOUR_GAP;
        }
        
        return dontDrawHour;
    }

    private void setupHourTextPaint(Paint p) {
        p.setColor(mCalendarHourLabelColor);
        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);        
        p.setAntiAlias(true);
    }
    
    private void setupSelectedMinutesTextPaint(Paint p) {
        p.setColor(mCalendarHourLabelColor);
        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);        
        p.setAntiAlias(true);
    }

    private void drawDayHeader(String dayStr, int day, int cells, Canvas canvas, Paint p) {
        int dateNum = mFirstVisibleDate + day;
        int x;
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }
        p.setAntiAlias(true);

        int todayIndex = mTodayJulianDay - mFirstJulianDay;
        // Draw day of the month
        String dateNumStr = String.valueOf(dateNum);
        if (mNumDays > 1) {
            float y = DAY_HEADER_HEIGHT - DAY_HEADER_BOTTOM_MARGIN;

            // Draw day of the month
            x = computeDayLeftPosition(day + 1) - DAY_HEADER_RIGHT_MARGIN;
            p.setTextAlign(Align.RIGHT);
            p.setTextSize(DATE_HEADER_FONT_SIZE);

            p.setTypeface(todayIndex == day ? mBold : Typeface.DEFAULT);
            canvas.drawText(dateNumStr, x, y, p);

            // Draw day of the week
            x -= p.measureText(" " + dateNumStr);
            p.setTextSize(DAY_HEADER_FONT_SIZE);
            p.setTypeface(Typeface.DEFAULT);
            canvas.drawText(dayStr, x, y, p);
        } else {
            float y = ONE_DAY_HEADER_HEIGHT - DAY_HEADER_ONE_DAY_BOTTOM_MARGIN;
            p.setTextAlign(Align.LEFT);

            // Draw day of the week
            x = computeDayLeftPosition(day) + DAY_HEADER_ONE_DAY_LEFT_MARGIN;
            p.setTextSize(DAY_HEADER_FONT_SIZE);
            p.setTypeface(Typeface.DEFAULT);
            canvas.drawText(dayStr, x, y, p);

            // Draw day of the month
            x += p.measureText(dayStr) + DAY_HEADER_ONE_DAY_RIGHT_MARGIN;
            p.setTextSize(DATE_HEADER_FONT_SIZE);
            p.setTypeface(todayIndex == day ? mBold : Typeface.DEFAULT);
            canvas.drawText(dateNumStr, x, y, p);
        }
    }

    
    private void drawGridBackground(Rect r, Canvas canvas, Paint p) {
        Paint.Style savedStyle = p.getStyle();

        final float stopX = computeDayLeftPosition(mNumDays);
        float y = GRID_LINE_TOP_MARGIN + HOUR_GAP;
        final float deltaY = mCellHeight + HOUR_GAP;
        int linesIndex = 0;
        final float startY = 0;
        final float stopY = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP);
        float x = mHoursWidth;

        // Draw the inner horizontal grid lines        
        p.setColor(mCalendarGridLineInnerHorizontalColor);
        //p.setColor(Color.RED); // for test
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        p.setAntiAlias(false);
        //y = 0; // �� ���� GRID_LINE_TOP_MARGIN�� ������ �� �ٽ� 0���� �����ϴ� ���ΰ�???
        linesIndex = 0;
        for (int hour = 0; hour <= 24; hour++) {
            mLines[linesIndex++] = GRID_LINE_LEFT_MARGIN;
            mLines[linesIndex++] = y;
            mLines[linesIndex++] = stopX;
            mLines[linesIndex++] = y;
            y += deltaY;
        }
        
        
        if (mCalendarGridLineInnerVerticalColor != mCalendarGridLineInnerHorizontalColor) {
            canvas.drawLines(mLines, 0, linesIndex, p);
            linesIndex = 0;
            p.setColor(mCalendarGridLineInnerVerticalColor);
        }

        // Draw the inner vertical grid lines
        if (mNumDays != 1) {
	        for (int day = 0; day <= mNumDays; day++) {
	            x = computeDayLeftPosition(day);
	            mLines[linesIndex++] = x;
	            mLines[linesIndex++] = startY;
	            mLines[linesIndex++] = x;
	            mLines[linesIndex++] = stopY;
	        }
        }
        
        canvas.drawLines(mLines, 0, linesIndex, p);

        // Restore the saved style.
        p.setStyle(savedStyle);
        p.setAntiAlias(true);
    }

    /**
     * @param r
     * @param canvas
     * @param p
     */
    private void drawBgColors(Rect r, Canvas canvas, Paint p) {
        int todayIndex = mTodayJulianDay - mFirstJulianDay;
        // Draw the hours background color
        r.top = mDestRect.top;
        r.bottom = mDestRect.bottom;
        r.left = 0;
        r.right = mHoursWidth;
        p.setColor(mBgColor);
        p.setStyle(Style.FILL);
        p.setAntiAlias(false);
        canvas.drawRect(r, p);

        // Draw background for grid area
        if (mNumDays == 1 && todayIndex == 0) {
            // Draw a white background for the time later than current time
            int lineY = mCurrentTime.get(Calendar.HOUR_OF_DAY) * (mCellHeight + HOUR_GAP)
                    + ((mCurrentTime.get(Calendar.MINUTE) * mCellHeight) / 60) + 1;
            if (lineY < mViewStartY + mViewHeight) {
                lineY = Math.max(lineY, mViewStartY);
                r.left = mHoursWidth;
                r.right = mViewWidth;
                r.top = lineY;
                r.bottom = mViewStartY + mViewHeight;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
        } else if (todayIndex >= 0 && todayIndex < mNumDays) {
            // Draw today with a white background for the time later than current time
            int lineY = mCurrentTime.get(Calendar.HOUR_OF_DAY) * (mCellHeight + HOUR_GAP)
                    + ((mCurrentTime.MINUTE * mCellHeight) / 60) + 1;
            if (lineY < mViewStartY + mViewHeight) {
                lineY = Math.max(lineY, mViewStartY);
                r.left = computeDayLeftPosition(todayIndex) + 1;
                r.right = computeDayLeftPosition(todayIndex + 1);
                r.top = lineY;
                r.bottom = mViewStartY + mViewHeight;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }

            // Paint Tomorrow and later days with future color
            if (todayIndex + 1 < mNumDays) {
                r.left = computeDayLeftPosition(todayIndex + 1) + 1;
                r.right = computeDayLeftPosition(mNumDays);
                r.top = mDestRect.top;
                r.bottom = mDestRect.bottom;
                //p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
        } else if (todayIndex < 0) {
            // Future
            r.left = computeDayLeftPosition(0) + 1;
            r.right = computeDayLeftPosition(mNumDays);
            r.top = mDestRect.top;
            r.bottom = mDestRect.bottom;
            p.setColor(mFutureBgColor);
            canvas.drawRect(r, p);
        }
        p.setAntiAlias(true);
    }

    /*
    Event getSelectedEvent() {
        if (mSelectedEvent == null) {
            // There is no event at the selected hour, so create a new event.
            return getNewEvent(mSelectionDay, getSelectedTimeInMillis(),
                    getSelectedMinutesSinceMidnight());
        }
        return mSelectedEvent;
    }
	
    
    boolean isEventSelected() {
        return (mSelectedEvent != null);
    }
	*/
    
    Event getNewEvent() {
        return getNewEvent(mSelectionDay, getSelectedTimeInMillis(),
                getSelectedMinutesSinceMidnight());
    }

    static Event getNewEvent(int julianDay, long utcMillis,
            int minutesSinceMidnight) {
        Event event = Event.newInstance();
        event.startDay = julianDay;
        event.endDay = julianDay;
        event.startMillis = utcMillis;
        event.endMillis = event.startMillis + MILLIS_PER_HOUR;
        event.startTime = minutesSinceMidnight;
        event.endTime = event.startTime + MINUTES_PER_HOUR;
        return event;
    }

    public static int computeMaxStringWidth(int currentMax, String[] strings, Paint p) {
        float maxWidthF = 0.0f;

        int len = strings.length;
        for (int i = 0; i < len; i++) {
            float width = p.measureText(strings[i]);
            maxWidthF = Math.max(width, maxWidthF);
        }
        int maxWidth = (int) (maxWidthF + 0.5); // �� 0.5�� ���ϴ°�? 
                                                // �Ǽ��� ������ ��ȯ�ϱ� ���� �Ǽ����� �ݿø��Ѵ�
        if (maxWidth < currentMax) {
            maxWidth = currentMax;
        }
        return maxWidth;
    }

    private void saveSelectionPosition(float left, float top, float right, float bottom) {
    	//synchronized(mAutoVerticalScrollingSync) {
	        mPrevBox.left = (int) left;
	        mPrevBox.right = (int) right;
	        mPrevBox.top = (int) top;
	        mPrevBox.bottom = (int) bottom;
    	//}
    }
    
    public void resetSelectionPosition() {
    			
		mPrevBox.set(0, 0, 0, 0);
    }

    private Rect getCurrentSelectionPosition() {
        Rect box = new Rect();
        box.top = mSelectionStartHour * (mCellHeight + HOUR_GAP);
        box.bottom = box.top + mCellHeight + HOUR_GAP;
        int daynum = mSelectionDay - mFirstJulianDay;
        box.left = computeDayLeftPosition(daynum) + 1;
        box.right = computeDayLeftPosition(daynum + 1);
        return box;
    }

    private void setupTextRect(Rect r) {
        if (r.bottom <= r.top || r.right <= r.left) {
            r.bottom = r.top;
            r.right = r.left;
            return;
        }

        if (r.bottom - r.top > EVENT_TEXT_TOP_MARGIN + EVENT_TEXT_BOTTOM_MARGIN) {
            r.top += EVENT_TEXT_TOP_MARGIN;
            r.bottom -= EVENT_TEXT_BOTTOM_MARGIN;
        }
        if (r.right - r.left > EVENT_TEXT_LEFT_MARGIN + EVENT_TEXT_RIGHT_MARGIN) {
            r.left += EVENT_TEXT_LEFT_MARGIN;
            r.right -= EVENT_TEXT_RIGHT_MARGIN;
        }
    }

    /*
    private void setupAllDayTextRect(Rect r) {
        if (r.bottom <= r.top || r.right <= r.left) {
            r.bottom = r.top;
            r.right = r.left;
            return;
        }

        if (r.bottom - r.top > EVENT_ALL_DAY_TEXT_TOP_MARGIN + EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN) {
            r.top += EVENT_ALL_DAY_TEXT_TOP_MARGIN;
            r.bottom -= EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN;
        }
        if (r.right - r.left > EVENT_ALL_DAY_TEXT_LEFT_MARGIN + EVENT_ALL_DAY_TEXT_RIGHT_MARGIN) {
            r.left += EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
            r.right -= EVENT_ALL_DAY_TEXT_RIGHT_MARGIN;
        }
    }
	*/
    
    int mEllipsizedLeft;
    int mEllipsizedRight;
    EllipsizeCallback mEllipsizeCallback = new EllipsizeCallback() {

		@Override
		public void ellipsized(int start, int end) {
			// TODO Auto-generated method stub
			//if (start == 0 && end == 0) {				
				mEllipsizedLeft = start;
				mEllipsizedRight = end;
				//if (INFO) Log.i(TAG, "mEllipsizedLeft=" + String.valueOf(mEllipsizedLeft) + ", mEllipsizedRight=" + String.valueOf(mEllipsizedRight));
			//}
			//else {				
				//mEllipsizedLeft = start;
				//mEllipsizedRight = end;
			//}
		}
    	
    };
    
    /**
     * Return the layout for a numbered event. Create it if not already existing
     */
    private StaticLayout getEventLayout(StaticLayout[] layouts, int i, Event event, Paint paint,
            Rect r) {
        if (i < 0 || i >= layouts.length) {
            return null;
        }

        StaticLayout layout = layouts[i];
        //layout.
        // Check if we have already initialized the StaticLayout and that
        // the width hasn't changed (due to vertical resizing which causes
        // re-layout of events at min height)
        if (layout == null || r.width() != layout.getWidth()) {
        	
        	TextPaint textPaint = new TextPaint(paint);       
        	        	
        	// SPAN �±״� "Inline Text Container (���� ������ �ؽ�Ʈ ������ �����ϴ� ��)"�ε�, �� ��ü�δ� �ƹ� ���ҵ� ���� �ʰ�,
        	// ������ Ư�� ������ CSS��Ÿ���� ������ �� ����Ѵ�
        	// -CSS:CSS �Ǵ� ĳ�����̵� ��Ÿ�� ��Ʈ(Cascading Style Sheets)�� ��ũ�� �� ���� ǥ�õǴ� ����� ����ϴ� ���
        	// -��ũ�� ���(markup, markup language)�� �±� ���� �̿��Ͽ� ������ �������� ������ ����ϴ� ����� �� �����̴�.
        	// �������� �߿���, � �±׵� �޷� ���� ���� ����, ������ �����ϱ� ���ؼ��� SPAN �±׸� ����ϸ� �ȴ� 
        	// ���� ���ؼ�, SPAN �±״� "�� �±�" �� ��� �ִ� �±��̴�. �� SPAN �±׸� ��� ���������, �������� �ۼ����� ������ �޷� �ִ�.
        	// ������ � ������ ����� �ִ� "�ᱸ��"�̶�� �ܾ��� ũ�⸦ �����Ѵ�. "�ᱸ��"�̶�� �ܾ�� �ٸ� �±װ� �پ� ���� �ʱ⿡, span �±׷� ���� �� ��, span �±׿��� ��Ÿ���� �����ϸ� �Ǵ� ���̴�.
        	// ������ �� <span style="font-size:24pt; line-height:1.5em">�ᱸ��</span>������ �������� ����� ���̰� �ִ�.
        	// �Ʒ��� "�ᱸ��"�̶�� �ܾ mytest ��� ���̵�(ID)�� �ο��ϴ� �����̴�
        	// ������ �� <span id="mytest">�ᱸ��</span>������ �������� ����� ���̰� �ִ�.
            SpannableStringBuilder bob = new SpannableStringBuilder();
            float availWidth = r.width();    
            
            // event.title�� null �� ��찡 �߻��� �� �ִ°�??????
            // ;Event.java����
            //  buildEventsFromCursor�� mNoTitleString = res.getString(R.string.no_title_label); �� �����ϰ�
            //  generateEventFromCursor����
            //  if (e.title == null || e.title.length() == 0) �� ��쿡�� ������ ���� �����Ѵ�
            //  e.title = mNoTitleString; 
            //  ->�׷��Ƿ� event.title�� null �� ���ɼ��� ���� ���µ�...
            if (event.title != null) {            	
            	
                // MAX - 1 since we add a space!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!        	
            	String saitizedTitleText = drawTextSanitizer(event.title.toString(), MAX_EVENT_TEXT_LEN - 1);
            	        	
            	    	
            	/*
                text : the text to truncate
                p : the Paint with which to measure the text
                availWidth : the horizontal width available for the text                
                */            	            	
            	String outputString = TextUtils.ellipsize(saitizedTitleText, textPaint, availWidth, TruncateAt.END, false, mEllipsizeCallback).toString();
            	// empty ��Ȳ�� �� ���� ��쿡 �߻��Ѵ�
            	// availWidth�� saitizedText ��ü�� ������ �� ��� saitizedText�� �Ϻκ��� ellipsis Ư�� ���ڷ� ġȯ�ؼ� �����ؾ� �ϴ� ��쿡
            	// 1.availWidth�� ellipsis Ư�� ���� ���� ������ �� ���� ��ŭ ���� ��쿡...
            	// 2.availWidth�� ellipsis Ư�� ���ڴ� ������ �� ������ 
            	//   availWidth���� ellipsis Ư�� ���ڸ� ������ ������ ������ remaining availWidth����
            	//   left = mt.breakText(len, true, availWidth) �� ������ ���
            	//   [���� len�� 1�̰� availWidth�� 1pixel�̰� �ش� �ؽ�Ʈ�� ù��° char�� ���̰� 1pixel���� ũ�ٸ�... 
            	//    ->left�� 0�� �ǰ� right�� 1�� �ȴ�],            	
            	//   remaining[len - (right - left)] == 0 �� ��찡 �߻��� ���...   
            	//   :remaining�� ellipsis�� ó������ ���� saitizedText�� �ٸ� �κ��� ������ ������ ���ٴ� ���� �ǹ�
            	//    ���� left�� ellipsis�� ó���� string�� start �κ��̰� right�� �κ��� end�� �ǹ��ϴµ�
            	//    right - left�� ellipsis�� ó���� string�� ���̸� �ǹ��Ѵ�
            	//    ��, len == (right - left)��� ����,
            	//    saitizedText ��ü�� ellipsis�� ó���ȴٴ� ���� �ǹ��Ѵٰ� �� ���� �ִµ�...
            	//    �ƽ��Ե� saitizedText ��ü�� ellipsis�� ó���� ������ �������� �ʰ� empty string�� �����Ѵ�
            	   
            	if (outputString.isEmpty()) {
            		String ellipsis = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
            		float ellipsiswid = textPaint.measureText(ellipsis);
            		float xAvailWidth = r.width();     
            		xAvailWidth -= ellipsiswid;
        			if (xAvailWidth < 0) {
        				ellipsis = "\u2025"; // \u2025�� 0x2025�� ��Ÿ���� TWO DOT LEADER 
        				ellipsiswid = textPaint.measureText(ellipsis);
        				xAvailWidth = r.width();     
        				xAvailWidth -= ellipsiswid;
            			if (xAvailWidth < 0) {
            				ellipsis = "\u2024"; // \U2024�� 0x2024�� ��Ÿ���� ONE DOT LEADER  
            				ellipsiswid = textPaint.measureText(ellipsis);
            				xAvailWidth = r.width();     
            				xAvailWidth -= ellipsiswid;
                			if (xAvailWidth >= 0) {
                				outputString = ellipsis.toString();
                			}
            			}
            			else {
            				outputString = ellipsis.toString();
            			}
        			}
        			else {
        				outputString = ellipsis.toString();
        			}
        			
        			int len = saitizedTitleText.length();
        			//Log.i(TAG, "title=" + saitizedTitleText.toString() + ", len=" + String.valueOf(len) + ", avail="  + String.valueOf(availWidth) + ", xAvail=" + String.valueOf(xAvailWidth));  		
            		
            		
            	}
            	else {
            		int len = saitizedTitleText.length();
            		//Log.i(TAG, "title=" + outputString.toString() + ", len=" + String.valueOf(len));
            		//if (INFO) Log.i(TAG, "title=" + outputString.toString());
            	}
            	/*
            	float textWidth = textPaint.measureText(outputString);
            	Rect bounds = new Rect();
            	textPaint.getTextBounds(outputString,0,outputString.length(),bounds);
            	int height = bounds.height();
            	int width = bounds.width();
            	*/
                bob.append(outputString);
                bob.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, bob.length(), 0);
                //bob.append(' ');
                bob.append("\n"); //
            }
            
            
            if (event.location != null) {
            	    
            	String saitizedLocationText = drawTextSanitizer(event.location.toString(), MAX_EVENT_TEXT_LEN - bob.length());            	
            	            	
            	String outputString = TextUtils.ellipsize(saitizedLocationText, textPaint, availWidth, TruncateAt.END).toString();
            	if (outputString.isEmpty()) {
            		            		
            		String ellipsis = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
            		float ellipsiswid = textPaint.measureText(ellipsis);
            		float xAvailWidth = r.width();     
            		xAvailWidth -= ellipsiswid;
        			if (xAvailWidth < 0) {
        				ellipsis = "\u2025"; // \u2025�� 0x2025�� ��Ÿ���� TWO DOT LEADER 
        				ellipsiswid = textPaint.measureText(ellipsis);
        				xAvailWidth = r.width();     
        				xAvailWidth -= ellipsiswid;
            			if (xAvailWidth < 0) {
            				ellipsis = "\u2024"; // \U2024�� 0x2024�� ��Ÿ���� ONE DOT LEADER  
            				ellipsiswid = textPaint.measureText(ellipsis);
            				xAvailWidth = r.width();     
            				xAvailWidth -= ellipsiswid;
                			if (xAvailWidth >= 0) {
                				outputString = ellipsis.toString();
                			}
            			}
            			else {
            				outputString = ellipsis.toString();
            			}
        			}
        			else {
        				outputString = ellipsis.toString();
        			}
        			
        			int len = saitizedLocationText.length();
        			//Log.i(TAG, "location=" + saitizedLocationText.toString() + ", len=" + String.valueOf(len) + ", avail="  + String.valueOf(availWidth) + ", xAvail=" + String.valueOf(xAvailWidth));            		
            	}
            	else {
            		int len = saitizedLocationText.length();
            		//Log.i(TAG, "location=" + outputString.toString() + ", len=" + String.valueOf(len));
            		//if (INFO) Log.i(TAG, "title=" + outputString.toString());
            	}
            	
            	bob.append(outputString);
            	
            	//if (INFO) Log.i(TAG, "location=" + outputString.toString());
                //bob.append(drawTextSanitizer(event.location.toString(),
                        //MAX_EVENT_TEXT_LEN - bob.length()));
            }
			
            
            switch (event.selfAttendeeStatus) {
                case Attendees.ATTENDEE_STATUS_INVITED:
                    paint.setColor(event.color);
                    break;
                case Attendees.ATTENDEE_STATUS_DECLINED:
                    paint.setColor(mEventTextColor);
                    paint.setAlpha(Utils.DECLINED_EVENT_TEXT_ALPHA);
                    break;
                case Attendees.ATTENDEE_STATUS_NONE: // Your own events
                case Attendees.ATTENDEE_STATUS_ACCEPTED:
                case Attendees.ATTENDEE_STATUS_TENTATIVE:
                default:
                    paint.setColor(mEventTextColor);
                    break;
            }

            // canvas.drawText�� ellipsize�� �����Ϸ���
            // TextUtils.ellipsize()�� ����϶�� �Ѵ�...
            
            // Leave a one pixel boundary on the left and right of the rectangle for the event
            // StaticLayout�� width�� �ʿ��ϳ� HEIGHT ���� �ʿ����.
            // paint ���� text size ���� ���� HEIGHT�� �����Ǵ� ���� �´�
            // DynamicLayout ����� ���?
            // Ellipsis : ������ȣ(...)
            // Paint.getTextBounds()�� StaticLayout�� height�� �� �� ����?
            
            // spacingmult = factor by which to scale the font size to get the default line spacing
            //              *line spacing = is the vertical distance between lines of text
            // spacingadd = amount to add to the default line spacing
            // includepad = ?
            layout = new StaticLayout(bob, 0, bob.length(), textPaint, r.width(),
            		Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            
            //layout = new StaticLayout(bob, 0, bob.length(), new TextPaint(paint), r.width(),
            		//Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, TruncateAt.START, r.width());
                        
            //layout.getHeight();
            layouts[i] = layout;             
        }
        
        layout.getPaint().setAlpha(mEventsAlpha);
        return layout;
    }
    
    
    private String getEventTitle(Event event, Paint paint, float avail) {
               	
    	TextPaint textPaint = new TextPaint(paint);     	        	
    	
        float availWidth = avail;    
        
        if (event.title != null) {            	
        	
            // MAX - 1 since we add a space!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!        	
        	String saitizedTitleText = drawTextSanitizer(event.title.toString(), MAX_EVENT_TEXT_LEN - 1);       	    	
        	/*
            text : the text to truncate
            p : the Paint with which to measure the text
            availWidth : the horizontal width available for the text                
            */            	            	
        	String outputString = TextUtils.ellipsize(saitizedTitleText, textPaint, availWidth, TruncateAt.END, false, mEllipsizeCallback).toString();
        	// empty ��Ȳ�� �� ���� ��쿡 �߻��Ѵ�
        	// availWidth�� saitizedText ��ü�� ������ �� ��� saitizedText�� �Ϻκ��� ellipsis Ư�� ���ڷ� ġȯ�ؼ� �����ؾ� �ϴ� ��쿡
        	// 1.availWidth�� ellipsis Ư�� ���� ���� ������ �� ���� ��ŭ ���� ��쿡...
        	// 2.availWidth�� ellipsis Ư�� ���ڴ� ������ �� ������ 
        	//   availWidth���� ellipsis Ư�� ���ڸ� ������ ������ ������ remaining availWidth����
        	//   left = mt.breakText(len, true, availWidth) �� ������ ���
        	//   [���� len�� 1�̰� availWidth�� 1pixel�̰� �ش� �ؽ�Ʈ�� ù��° char�� ���̰� 1pixel���� ũ�ٸ�... 
        	//    ->left�� 0�� �ǰ� right�� 1�� �ȴ�],            	
        	//   remaining[len - (right - left)] == 0 �� ��찡 �߻��� ���...   
        	//   :remaining�� ellipsis�� ó������ ���� saitizedText�� �ٸ� �κ��� ������ ������ ���ٴ� ���� �ǹ�
        	//    ���� left�� ellipsis�� ó���� string�� start �κ��̰� right�� �κ��� end�� �ǹ��ϴµ�
        	//    right - left�� ellipsis�� ó���� string�� ���̸� �ǹ��Ѵ�
        	//    ��, len == (right - left)��� ����,
        	//    saitizedText ��ü�� ellipsis�� ó���ȴٴ� ���� �ǹ��Ѵٰ� �� ���� �ִµ�...
        	//    �ƽ��Ե� saitizedText ��ü�� ellipsis�� ó���� ������ �������� �ʰ� empty string�� �����Ѵ�
        	   
        	if (outputString.isEmpty()) {
        		String ellipsis = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
        		float ellipsiswid = textPaint.measureText(ellipsis);
        		float xAvailWidth = avail;     
        		xAvailWidth -= ellipsiswid;
        		
    			if (xAvailWidth < 0) {
    				ellipsis = "\u2025"; // \u2025�� 0x2025�� ��Ÿ���� TWO DOT LEADER 
    				ellipsiswid = textPaint.measureText(ellipsis);
    				xAvailWidth = avail;     
    				xAvailWidth -= ellipsiswid;
        			if (xAvailWidth < 0) {
        				//ellipsis = "\u2024"; // \U2024�� 0x2024�� ��Ÿ���� ONE DOT LEADER : android�� one dot leader�� ǥ������ ���ϴ� �� ����  
        				ellipsis = ".";
        				ellipsiswid = textPaint.measureText(ellipsis);
        				xAvailWidth = avail;     
        				xAvailWidth -= ellipsiswid;
            			if (xAvailWidth >= 0) {
            				outputString = ellipsis.toString();
            			}
        			}
        			else {
        				outputString = ellipsis.toString();
        			}
    			}
    			else {
    				outputString = ellipsis.toString();
    			}
    			
    			int len = saitizedTitleText.length();
    			//Log.i(TAG, "title=" + saitizedTitleText.toString() + ", len=" + String.valueOf(len) + ", avail="  + String.valueOf(availWidth) + ", xAvail=" + String.valueOf(xAvailWidth));         		
        		
        	}
        	else {
        		int len = saitizedTitleText.length();
        		//Log.i(TAG, "title=" + outputString.toString() + ", len=" + String.valueOf(len));
        		//if (INFO) Log.i(TAG, "title=" + outputString.toString());
        	}
        	
            return outputString;
        }  
        else {
        	return null;
        }       
    }

    private String getEventLocation(Event event, Paint paint, float avail) {
       	
    	TextPaint textPaint = new TextPaint(paint); 
    	
    	float titleTextFontSize = textPaint.getTextSize();
    	float fontSize = titleTextFontSize * 0.9f;
    	//float fontSize = titleTextFontSize * 0.8f;
    	textPaint.setTextSize(fontSize);
    	
        float availWidth = avail;    
        
        if (event.location != null) {            	
        	
            // MAX - 1 since we add a space!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!        	
        	String saitizedLocationText = drawTextSanitizer(event.location.toString(), MAX_EVENT_TEXT_LEN - 1);       	    	
        	  	            	
        	String outputString = TextUtils.ellipsize(saitizedLocationText, textPaint, availWidth, TruncateAt.END, false, mEllipsizeCallback).toString();
        	        	   
        	if (outputString.isEmpty()) {
        		String ellipsis = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
        		float ellipsiswid = textPaint.measureText(ellipsis);
        		float xAvailWidth = avail;     
        		xAvailWidth -= ellipsiswid;
    			if (xAvailWidth < 0) {
    				ellipsis = "\u2025"; // \u2025�� 0x2025�� ��Ÿ���� TWO DOT LEADER 
    				ellipsiswid = textPaint.measureText(ellipsis);
    				xAvailWidth = avail;     
    				xAvailWidth -= ellipsiswid;
        			if (xAvailWidth < 0) {
        				//ellipsis = "\u2024"; // \U2024�� 0x2024�� ��Ÿ���� ONE DOT LEADER : android�� one dot leader�� ǥ������ ���ϴ� �� ����    
        				ellipsis = ".";
        				ellipsiswid = textPaint.measureText(ellipsis);
        				xAvailWidth = avail;     
        				xAvailWidth -= ellipsiswid;
            			if (xAvailWidth >= 0) {
            				outputString = ellipsis.toString();
            			}
        			}
        			else {
        				outputString = ellipsis.toString();
        			}
    			}
    			else {
    				outputString = ellipsis.toString();
    			}
    			
    			int len = saitizedLocationText.length();
    			//Log.i(TAG, "location=" + saitizedLocationText.toString() + ", len=" + String.valueOf(len) + ", avail="  + String.valueOf(availWidth) + ", xAvail=" + String.valueOf(xAvailWidth));  		
        		
        		
        	}
        	else {
        		int len = saitizedLocationText.length();
        		//Log.i(TAG, "location=" + outputString.toString() + ", len=" + String.valueOf(len));        		
        	}
        	
            return outputString;
        }  
        else {
        	return null;
        }       
    }
    
    /*
    private void drawAllDayEvents(int firstDay, int numDays, Canvas canvas, Paint p) {    	    	

        p.setTextSize(NORMAL_FONT_SIZE);
        p.setTextAlign(Paint.Align.LEFT);
        Paint eventTextPaint = mEventTextPaint;

        final float startY = DAY_HEADER_HEIGHT;
        final float stopY = startY + mAlldayHeight + ALLDAY_TOP_MARGIN;
        float x = 0;
        int linesIndex = 0;

        // Draw the inner vertical grid lines
        p.setColor(mCalendarGridLineInnerVerticalColor);
        //p.setColor(Color.GREEN); // for test
        x = mHoursWidth;
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        // Line bounding the top of the all day area
        mLines[linesIndex++] = GRID_LINE_LEFT_MARGIN;
        mLines[linesIndex++] = startY;
        mLines[linesIndex++] = computeDayLeftPosition(mNumDays);
        mLines[linesIndex++] = startY;

        for (int day = 0; day <= mNumDays; day++) {
            x = computeDayLeftPosition(day);
            mLines[linesIndex++] = x;
            mLines[linesIndex++] = startY;
            mLines[linesIndex++] = x;
            mLines[linesIndex++] = stopY;
        }
        p.setAntiAlias(false);
        canvas.drawLines(mLines, 0, linesIndex, p);
        p.setStyle(Style.FILL);

        int y = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
        int lastDay = firstDay + numDays - 1;
        final ArrayList<Event> events = mAllDayEvents;
        int numEvents = events.size();
        // Whether or not we should draw the more events text
        boolean hasMoreEvents = false;
        // size of the allDay area
        float drawHeight = mAlldayHeight;
        // max number of events being drawn in one day of the allday area
        float numRectangles = mMaxAlldayEvents;
        // Where to cut off drawn allday events
        int allDayEventClip = DAY_HEADER_HEIGHT + mAlldayHeight + ALLDAY_TOP_MARGIN;
        // The number of events that weren't drawn in each day
        mSkippedAlldayEvents = new int[numDays];
        if (mMaxAlldayEvents > mMaxUnexpandedAlldayEventCount && !mShowAllAllDayEvents &&
                mAnimateDayHeight == 0) {
            // We draw one fewer event than will fit so that more events text
            // can be drawn
            numRectangles = mMaxUnexpandedAlldayEventCount - 1;
            // We also clip the events above the more events text
            allDayEventClip -= MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
            hasMoreEvents = true;
        } else if (mAnimateDayHeight != 0) {
            // clip at the end of the animating space
            allDayEventClip = DAY_HEADER_HEIGHT + mAnimateDayHeight + ALLDAY_TOP_MARGIN;
        }

        int alpha = eventTextPaint.getAlpha();
        eventTextPaint.setAlpha(mEventsAlpha);
        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            int startDay = event.startDay;
            int endDay = event.endDay;
            if (startDay > lastDay || endDay < firstDay) {
                continue;
            }
            if (startDay < firstDay) {
                startDay = firstDay;
            }
            if (endDay > lastDay) {
                endDay = lastDay;
            }
            int startIndex = startDay - firstDay;
            int endIndex = endDay - firstDay;
            float height = mMaxAlldayEvents > mMaxUnexpandedAlldayEventCount ? mAnimateDayEventHeight :
                    drawHeight / numRectangles;

            // Prevent a single event from getting too big
            if (height > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
                height = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
            }

            // Leave a one-pixel space between the vertical day lines and the
            // event rectangle.
            event.left = computeDayLeftPosition(startIndex);
            event.right = computeDayLeftPosition(endIndex + 1) - DAY_GAP;
            event.top = y + height * event.getColumn();
            event.bottom = event.top + height - ALL_DAY_EVENT_RECT_BOTTOM_MARGIN;
            if (mMaxAlldayEvents > mMaxUnexpandedAlldayEventCount) {
                // check if we should skip this event. We skip if it starts
                // after the clip bound or ends after the skip bound and we're
                // not animating.
                if (event.top >= allDayEventClip) {
                    incrementSkipCount(mSkippedAlldayEvents, startIndex, endIndex);
                    continue;
                } else if (event.bottom > allDayEventClip) {
                    if (hasMoreEvents) {
                        incrementSkipCount(mSkippedAlldayEvents, startIndex, endIndex);
                        continue;
                    }
                    event.bottom = allDayEventClip;
                }
            }
            
            Rect r = drawEventRect(event, canvas, p, eventTextPaint, (int) event.top,
                    (int) event.bottom);
            setupAllDayTextRect(r);
            StaticLayout layout = getEventLayout(mAllDayLayouts, i, event, eventTextPaint, r);
            drawEventText(layout, r, canvas, r.top, r.bottom, true);

            // Check if this all-day event intersects the selected day
            if (mSelectionAllday && mComputeSelectedEvents) {
                if (startDay <= mSelectionDay && endDay >= mSelectionDay) {
                    mSelectedEvents.add(event);
                }
            }
        }
        eventTextPaint.setAlpha(alpha);

        if (mMoreAlldayEventsTextAlpha != 0 && mSkippedAlldayEvents != null) {
            // If the more allday text should be visible, draw it.
            alpha = p.getAlpha();
            p.setAlpha(mEventsAlpha);
            p.setColor(mMoreAlldayEventsTextAlpha << 24 & mMoreEventsTextColor);
            for (int i = 0; i < mSkippedAlldayEvents.length; i++) {
                if (mSkippedAlldayEvents[i] > 0) {
                    drawMoreAlldayEvents(canvas, mSkippedAlldayEvents[i], i, p);
                }
            }
            p.setAlpha(alpha);
        }

        if (mSelectionAllday) {
            // Compute the neighbors for the list of all-day events that
            // intersect the selected day.
            computeAllDayNeighbors();

            // Set the selection position to zero so that when we move down
            // to the normal event area, we will highlight the topmost event.
            saveSelectionPosition(0f, 0f, 0f, 0f);
        }
    }
	*/
    
    // Helper method for counting the number of allday events skipped on each day
    private void incrementSkipCount(int[] counts, int startIndex, int endIndex) {
        if (counts == null || startIndex < 0 || endIndex > counts.length) {
            return;
        }
        for (int i = startIndex; i <= endIndex; i++) {
            counts[i]++;
        }
    }

    // Draws the "box +n" text for hidden allday events
    /*
    protected void drawMoreAlldayEvents(Canvas canvas, int remainingEvents, int day, Paint p) {
        int x = computeDayLeftPosition(day) + EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
        int y = (int) (mAlldayHeight - .5f * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT - .5f
                * EVENT_SQUARE_WIDTH + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN);
        Rect r = mRect;
        r.top = y;
        r.left = x;
        r.bottom = y + EVENT_SQUARE_WIDTH;
        r.right = x + EVENT_SQUARE_WIDTH;
        p.setColor(mMoreEventsTextColor);
        p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        p.setStyle(Style.STROKE);
        p.setAntiAlias(false);
        canvas.drawRect(r, p);
        p.setAntiAlias(true);
        p.setStyle(Style.FILL);
        p.setTextSize(EVENT_TEXT_FONT_SIZE);
        String text = mResources.getQuantityString(R.plurals.month_more_events, remainingEvents);
        y += EVENT_SQUARE_WIDTH;
        x += EVENT_SQUARE_WIDTH + EVENT_LINE_PADDING;
        canvas.drawText(String.format(text, remainingEvents), x, y, p);
    }

    private void computeAllDayNeighbors() {
        int len = mSelectedEvents.size();
        if (len == 0 || mFragment.getSelectedEvent() != null) {
            return;
        }

        // First, clear all the links
        for (int ii = 0; ii < len; ii++) {
            Event ev = mSelectedEvents.get(ii);
            ev.nextUp = null;
            ev.nextDown = null;
            ev.nextLeft = null;
            ev.nextRight = null;
        }

        // For each event in the selected event list "mSelectedEvents", find
        // its neighbors in the up and down directions. This could be done
        // more efficiently by sorting on the Event.getColumn() field, but
        // the list is expected to be very small.

        // Find the event in the same row as the previously selected all-day
        // event, if any.
        int startPosition = -1;
        
        //if (mPrevSelectedEvent != null && mPrevSelectedEvent.drawAsAllday()) {
            //startPosition = mPrevSelectedEvent.getColumn();
        //}
        
        int maxPosition = -1;
        Event startEvent = null;
        Event maxPositionEvent = null;
        for (int ii = 0; ii < len; ii++) {
            Event ev = mSelectedEvents.get(ii);
            int position = ev.getColumn();
            if (position == startPosition) {
                startEvent = ev;
            } else if (position > maxPosition) {
                maxPositionEvent = ev;
                maxPosition = position;
            }
            for (int jj = 0; jj < len; jj++) {
                if (jj == ii) {
                    continue;
                }
                Event neighbor = mSelectedEvents.get(jj);
                int neighborPosition = neighbor.getColumn();
                if (neighborPosition == position - 1) {
                    ev.nextUp = neighbor;
                } else if (neighborPosition == position + 1) {
                    ev.nextDown = neighbor;
                }
            }
        }
        
        if (startEvent != null) {
            mFragment.setSelectedEvent(startEvent);
        } else {
            mFragment.setSelectedEvent(maxPositionEvent);
        }
    }   
    */
    
    private void drawEvents(int date, int dayIndex, int top, Canvas canvas, Paint p) {
        Paint eventTextPaint = mEventTextPaint;
        int left = computeDayLeftPosition(dayIndex) + 1;
        int cellWidth = computeDayLeftPosition(dayIndex + 1) - left + 1;
        int cellHeight = mCellHeight;

        // Use the selected hour as the selection region
        Rect selectionArea = mSelectionRect;
        selectionArea.top = top + mSelectionStartHour * (cellHeight + HOUR_GAP);
        selectionArea.bottom = selectionArea.top + cellHeight;
        selectionArea.left = left;
        selectionArea.right = selectionArea.left + cellWidth;

        final ArrayList<Event> events = mEvents;
        int numEvents = events.size();
        EventGeometry geometry = mEventGeometry;

        //final int viewEndY = mViewStartY + mViewHeight - DAY_HEADER_HEIGHT - mAlldayHeight - GRID_LINE_TOP_MARGIN;
        final int viewEndY = mViewStartY + mViewHeight;
        
        int alpha = eventTextPaint.getAlpha();
        eventTextPaint.setAlpha(mEventsAlpha);
        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
                continue;
            }

            // Don't draw it if it is not visible
            if (event.bottom < mViewStartY || event.top > viewEndY) {
                continue;
            }

            if (date == mSelectionDay /*&& mComputeSelectedEvents*/
                    && geometry.eventIntersectsSelection(event, selectionArea)) {
                mSelectedEvents.add(event);
            }

            Rect r = drawEventRect(event, canvas, p, eventTextPaint, mViewStartY, viewEndY);
            setupTextRect(r);

            // Don't draw text if it is not visible
            if (r.top > viewEndY || r.bottom < mViewStartY) {
                continue;
            }            
            
            boolean twoLines = true;
        	int eventHeight = (int) (event.bottom - event.top);
        	// event rect height�� two text line�� ������ �� ���ٸ� one text line���� ���� �Ѵ�
        	if (eventHeight < mMinEventRectHeightTwoLinesEventText) { 
        		twoLines = false;
            }
        	
        	// event rect height�� one text line�� �����ϴµ�..
        	// EVENT_TEXT_FONT_SIZE �� text height�� ������ �� ���� ����
        	// event rect height�� �°� event font size�� �缳���ؾ� �Ѵ�
        	if (eventHeight < mAvailEventRectHeightByDefaultEventTextHeight) { 
        		// event height�� 50% size�� ����� : 
        		float eventFontSize = getEventFontSize(eventHeight, 0.5f);
        		if (eventFontSize > EVENT_TEXT_FONT_SIZE) {
        			if (INFO) Log.i(TAG, "What the hell..����������");
        		}
        		
        		eventTextPaint.setTextSize(eventFontSize);            		
            }
        	     	
        	
            float availWidth = r.width();         	
            String title = getEventTitle(event, eventTextPaint, availWidth);                
            
            // title�� ONE DOT LEADER�� ��ü�� �� ���� ��ŭ ������ �����ϴٴ� ���̹Ƿ�
            // �ƿ� �ؽ�Ʈ�� ������� �õ����� ���ʿ��ϴ�
            if (!title.isEmpty()) {
            	switch (event.selfAttendeeStatus) {
                case Attendees.ATTENDEE_STATUS_INVITED:
                	eventTextPaint.setColor(event.color);
                    break;
                case Attendees.ATTENDEE_STATUS_DECLINED:
                	eventTextPaint.setColor(mEventTextColor);
                	eventTextPaint.setAlpha(Utils.DECLINED_EVENT_TEXT_ALPHA);
                    break;
                case Attendees.ATTENDEE_STATUS_NONE: // Your own events
                case Attendees.ATTENDEE_STATUS_ACCEPTED:
                case Attendees.ATTENDEE_STATUS_TENTATIVE:
                default:
                	eventTextPaint.setColor(mEventTextColor);
                    break;
            	}                 	                            	
            	
            	float locationTextAvailWidth = availWidth;
            	if (!twoLines) {
            		float titleTextConsumedWidth = eventTextPaint.measureText(title);
            		String space = " ";
            		float spaceWidth = eventTextPaint.measureText(space);
            		locationTextAvailWidth = availWidth - titleTextConsumedWidth - spaceWidth;            		
            	}            	            	
            	
            	String location = getEventLocation(event, eventTextPaint, locationTextAvailWidth);
            	
	            // TODO: not sure why we are 4 pixels off
	            drawEventTitleLocationText(title, location, twoLines, r, canvas, eventTextPaint, 
	            		mViewStartY/* + 4*/, 
	            		mViewStartY + mViewHeight - DAY_HEADER_HEIGHT);      
	            
            }
            
            // font size recovery!!!
            //if (eventHeight < mTwoQuartersOfHourPixelHeight) {
        		eventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
            //}
        }
        
        eventTextPaint.setAlpha(alpha);

        if (date == mSelectionDay && /*!mSelectionAllday &&*/ isFocused()) {
            computeNeighbors();
        }
    }

    // Computes the "nearest" neighbor event in four directions (left, right,
    // up, down) for each of the events in the mSelectedEvents array.
    private void computeNeighbors() {
        int len = mSelectedEvents.size();
        if (len == 0 || mFragment.getSelectedEvent() != null) {
            return;
        }

        // First, clear all the links
        for (int ii = 0; ii < len; ii++) {
            Event ev = mSelectedEvents.get(ii);
            ev.nextUp = null;
            ev.nextDown = null;
            ev.nextLeft = null;
            ev.nextRight = null;
        }

        Event startEvent = mSelectedEvents.get(0);
        int startEventDistance1 = 100000; // any large number
        int startEventDistance2 = 100000; // any large number
        int prevLocation = FROM_NONE;
        int prevTop;
        int prevBottom;
        int prevLeft;
        int prevRight;
        int prevCenter = 0;
        Rect box = getCurrentSelectionPosition();
        /*if (mPrevSelectedEvent != null) {
            prevTop = (int) mPrevSelectedEvent.top;
            prevBottom = (int) mPrevSelectedEvent.bottom;
            prevLeft = (int) mPrevSelectedEvent.left;
            prevRight = (int) mPrevSelectedEvent.right;
            // Check if the previously selected event intersects the previous
            // selection box. (The previously selected event may be from a
            // much older selection box.)
            if (prevTop >= mPrevBox.bottom || prevBottom <= mPrevBox.top
                    || prevRight <= mPrevBox.left || prevLeft >= mPrevBox.right) {
                mPrevSelectedEvent = null;
                prevTop = mPrevBox.top;
                prevBottom = mPrevBox.bottom;
                prevLeft = mPrevBox.left;
                prevRight = mPrevBox.right;
            } else {
                // Clip the top and bottom to the previous selection box.
                if (prevTop < mPrevBox.top) {
                    prevTop = mPrevBox.top;
                }
                if (prevBottom > mPrevBox.bottom) {
                    prevBottom = mPrevBox.bottom;
                }
            }
        } else*/ {
            // Just use the previously drawn selection box
            prevTop = mPrevBox.top;
            prevBottom = mPrevBox.bottom;
            prevLeft = mPrevBox.left;
            prevRight = mPrevBox.right;
        }

        // Figure out where we came from and compute the center of that area.
        if (prevLeft >= box.right) {
            // The previously selected event was to the right of us.
            prevLocation = FROM_RIGHT;
            prevCenter = (prevTop + prevBottom) / 2;
        } else if (prevRight <= box.left) {
            // The previously selected event was to the left of us.
            prevLocation = FROM_LEFT;
            prevCenter = (prevTop + prevBottom) / 2;
        } else if (prevBottom <= box.top) {
            // The previously selected event was above us.
            prevLocation = FROM_ABOVE;
            prevCenter = (prevLeft + prevRight) / 2;
        } else if (prevTop >= box.bottom) {
            // The previously selected event was below us.
            prevLocation = FROM_BELOW;
            prevCenter = (prevLeft + prevRight) / 2;
        }

        // For each event in the selected event list "mSelectedEvents", search
        // all the other events in that list for the nearest neighbor in 4
        // directions.
        for (int ii = 0; ii < len; ii++) {
            Event ev = mSelectedEvents.get(ii);

            int startTime = ev.startTime;
            int endTime = ev.endTime;
            int left = (int) ev.left;
            int right = (int) ev.right;
            int top = (int) ev.top;
            if (top < box.top) {
                top = box.top;
            }
            int bottom = (int) ev.bottom;
            if (bottom > box.bottom) {
                bottom = box.bottom;
            }
//            if (false) {
//                int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL
//                        | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
//                if (DateFormat.is24HourFormat(mContext)) {
//                    flags |= DateUtils.FORMAT_24HOUR;
//                }
//                String timeRange = DateUtils.formatDateRange(mContext, ev.startMillis,
//                        ev.endMillis, flags);
//                Log.i("Cal", "left: " + left + " right: " + right + " top: " + top + " bottom: "
//                        + bottom + " ev: " + timeRange + " " + ev.title);
//            }
            int upDistanceMin = 10000; // any large number
            int downDistanceMin = 10000; // any large number
            int leftDistanceMin = 10000; // any large number
            int rightDistanceMin = 10000; // any large number
            Event upEvent = null;
            Event downEvent = null;
            Event leftEvent = null;
            Event rightEvent = null;

            // Pick the starting event closest to the previously selected event,
            // if any. distance1 takes precedence over distance2.
            int distance1 = 0;
            int distance2 = 0;
            if (prevLocation == FROM_ABOVE) {
                if (left >= prevCenter) {
                    distance1 = left - prevCenter;
                } else if (right <= prevCenter) {
                    distance1 = prevCenter - right;
                }
                distance2 = top - prevBottom;
            } else if (prevLocation == FROM_BELOW) {
                if (left >= prevCenter) {
                    distance1 = left - prevCenter;
                } else if (right <= prevCenter) {
                    distance1 = prevCenter - right;
                }
                distance2 = prevTop - bottom;
            } else if (prevLocation == FROM_LEFT) {
                if (bottom <= prevCenter) {
                    distance1 = prevCenter - bottom;
                } else if (top >= prevCenter) {
                    distance1 = top - prevCenter;
                }
                distance2 = left - prevRight;
            } else if (prevLocation == FROM_RIGHT) {
                if (bottom <= prevCenter) {
                    distance1 = prevCenter - bottom;
                } else if (top >= prevCenter) {
                    distance1 = top - prevCenter;
                }
                distance2 = prevLeft - right;
            }
            if (distance1 < startEventDistance1
                    || (distance1 == startEventDistance1 && distance2 < startEventDistance2)) {
                startEvent = ev;
                startEventDistance1 = distance1;
                startEventDistance2 = distance2;
            }

            // For each neighbor, figure out if it is above or below or left
            // or right of me and compute the distance.
            for (int jj = 0; jj < len; jj++) {
                if (jj == ii) {
                    continue;
                }
                Event neighbor = mSelectedEvents.get(jj);
                int neighborLeft = (int) neighbor.left;
                int neighborRight = (int) neighbor.right;
                if (neighbor.endTime <= startTime) {
                    // This neighbor is entirely above me.
                    // If we overlap the same column, then compute the distance.
                    if (neighborLeft < right && neighborRight > left) {
                        int distance = startTime - neighbor.endTime;
                        if (distance < upDistanceMin) {
                            upDistanceMin = distance;
                            upEvent = neighbor;
                        } else if (distance == upDistanceMin) {
                            int center = (left + right) / 2;
                            int currentDistance = 0;
                            int currentLeft = (int) upEvent.left;
                            int currentRight = (int) upEvent.right;
                            if (currentRight <= center) {
                                currentDistance = center - currentRight;
                            } else if (currentLeft >= center) {
                                currentDistance = currentLeft - center;
                            }

                            int neighborDistance = 0;
                            if (neighborRight <= center) {
                                neighborDistance = center - neighborRight;
                            } else if (neighborLeft >= center) {
                                neighborDistance = neighborLeft - center;
                            }
                            if (neighborDistance < currentDistance) {
                                upDistanceMin = distance;
                                upEvent = neighbor;
                            }
                        }
                    }
                } else if (neighbor.startTime >= endTime) {
                    // This neighbor is entirely below me.
                    // If we overlap the same column, then compute the distance.
                    if (neighborLeft < right && neighborRight > left) {
                        int distance = neighbor.startTime - endTime;
                        if (distance < downDistanceMin) {
                            downDistanceMin = distance;
                            downEvent = neighbor;
                        } else if (distance == downDistanceMin) {
                            int center = (left + right) / 2;
                            int currentDistance = 0;
                            int currentLeft = (int) downEvent.left;
                            int currentRight = (int) downEvent.right;
                            if (currentRight <= center) {
                                currentDistance = center - currentRight;
                            } else if (currentLeft >= center) {
                                currentDistance = currentLeft - center;
                            }

                            int neighborDistance = 0;
                            if (neighborRight <= center) {
                                neighborDistance = center - neighborRight;
                            } else if (neighborLeft >= center) {
                                neighborDistance = neighborLeft - center;
                            }
                            if (neighborDistance < currentDistance) {
                                downDistanceMin = distance;
                                downEvent = neighbor;
                            }
                        }
                    }
                }

                if (neighborLeft >= right) {
                    // This neighbor is entirely to the right of me.
                    // Take the closest neighbor in the y direction.
                    int center = (top + bottom) / 2;
                    int distance = 0;
                    int neighborBottom = (int) neighbor.bottom;
                    int neighborTop = (int) neighbor.top;
                    if (neighborBottom <= center) {
                        distance = center - neighborBottom;
                    } else if (neighborTop >= center) {
                        distance = neighborTop - center;
                    }
                    if (distance < rightDistanceMin) {
                        rightDistanceMin = distance;
                        rightEvent = neighbor;
                    } else if (distance == rightDistanceMin) {
                        // Pick the closest in the x direction
                        int neighborDistance = neighborLeft - right;
                        int currentDistance = (int) rightEvent.left - right;
                        if (neighborDistance < currentDistance) {
                            rightDistanceMin = distance;
                            rightEvent = neighbor;
                        }
                    }
                } else if (neighborRight <= left) {
                    // This neighbor is entirely to the left of me.
                    // Take the closest neighbor in the y direction.
                    int center = (top + bottom) / 2;
                    int distance = 0;
                    int neighborBottom = (int) neighbor.bottom;
                    int neighborTop = (int) neighbor.top;
                    if (neighborBottom <= center) {
                        distance = center - neighborBottom;
                    } else if (neighborTop >= center) {
                        distance = neighborTop - center;
                    }
                    if (distance < leftDistanceMin) {
                        leftDistanceMin = distance;
                        leftEvent = neighbor;
                    } else if (distance == leftDistanceMin) {
                        // Pick the closest in the x direction
                        int neighborDistance = left - neighborRight;
                        int currentDistance = left - (int) leftEvent.right;
                        if (neighborDistance < currentDistance) {
                            leftDistanceMin = distance;
                            leftEvent = neighbor;
                        }
                    }
                }
            }
            ev.nextUp = upEvent;
            ev.nextDown = downEvent;
            ev.nextLeft = leftEvent;
            ev.nextRight = rightEvent;
        }
        
        mFragment.setSelectedEvent(startEvent);
    }

    private Rect drawEventRect(Event event, Canvas canvas, Paint p, Paint eventTextPaint,
            int visibleTop, int visibleBot) {
        // Draw the Event Rect
        Rect r = mRect;
        r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN, visibleTop);
        r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN, visibleBot);
        r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right;

        int color;        
        color = event.color; //CALENDAR_COLOR �̴�        
        
        int bg = 0xffffffff;
        int a = 153;
        int red = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
        color = (0xff000000) | ((red | g | b) >> 8);        
		
        switch (event.selfAttendeeStatus) {
            case Attendees.ATTENDEE_STATUS_INVITED:
                
                p.setStyle(Style.STROKE);
                
                break;
            case Attendees.ATTENDEE_STATUS_DECLINED:
                
                //color = Utils.getDeclinedColorFromColor(color);
                
            case Attendees.ATTENDEE_STATUS_NONE: // Your own events
            case Attendees.ATTENDEE_STATUS_ACCEPTED:
            case Attendees.ATTENDEE_STATUS_TENTATIVE:
            default:
                p.setStyle(Style.FILL_AND_STROKE);
                break;
        }

        p.setAntiAlias(false);

        int floorHalfStroke = (int) Math.floor(EVENT_RECT_STROKE_WIDTH / 2.0f);
        int ceilHalfStroke = (int) Math.ceil(EVENT_RECT_STROKE_WIDTH / 2.0f);
        r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN + ceilHalfStroke, visibleTop);
        r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN - floorHalfStroke,
                visibleBot);
        r.left += ceilHalfStroke;
        r.right -= floorHalfStroke;
        p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        p.setColor(color);
        int alpha = p.getAlpha();
        p.setAlpha(mEventsAlpha);        
        canvas.drawRect(r, p);///////////////////////////////////////////////////////////////////////////           
        
        
        p.setAlpha(alpha);
        Rect dnaStrand = new Rect();
        dnaStrand.left = r.left;
        dnaStrand.top = r.top;
        dnaStrand.right = dnaStrand.left + (int)(mCellWidth * 0.0025f);
        dnaStrand.bottom = r.bottom;
        p.setColor(event.color);
        p.setAlpha(255);
        canvas.drawRect(dnaStrand, p);
        
        p.setStyle(Style.FILL);        
        
        // Setup rect for drawEventText which follows
        r.top = (int) event.top + EVENT_RECT_TOP_MARGIN;
        r.bottom = (int) event.bottom - EVENT_RECT_BOTTOM_MARGIN;        
        r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right;//r.right = (int) event.right - EVENT_RECT_RIGHT_MARGIN;
        return r;
    }

    // \t : Ű���� ������ TabŰ, ����8���� �ش��ϴ� �����̴�. (��쿡 ���� ����2~4���� �ش�� ���� �ִ�.)
    private final Pattern drawTextSanitizerFilter = Pattern.compile("[\t\n],");

    // Sanitize a string before passing it to drawText or else we get little squares. 
    // For newlines and tabs before a comma, delete the character.
    // Otherwise, just replace them with a space.
    private String drawTextSanitizer(String string, int maxEventTextLen) {
        Matcher m = drawTextSanitizerFilter.matcher(string);
        string = m.replaceAll(",");

        int len = string.length();
        if (maxEventTextLen <= 0) {
            string = "";
            len = 0;
        } else if (len > maxEventTextLen) {
            string = string.substring(0, maxEventTextLen);
            len = maxEventTextLen;
        }

        return string.replace('\n', ' ');
    }

    
    private void drawEventText(StaticLayout eventLayout, Rect rect, Canvas canvas, int top,
            int bottom, boolean center) {
        // drawEmptyRect(canvas, rect, 0xFFFF00FF); // for debugging

        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        // If the rectangle is too small for text, then return
        // MIN_CELL_WIDTH_FOR_TEXT�� ���� ������ ���� ���� �� ��...
        // �׷��� �ּ����� ó����
        //if (eventLayout == null || width < MIN_CELL_WIDTH_FOR_TEXT) {
        if (eventLayout == null) {
            return;
        }

        int totalLineHeight = 0;
        int lineCount = eventLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            int lineBottom = eventLayout.getLineBottom(i);
            if (lineBottom <= height) {
                totalLineHeight = lineBottom;
            } else {
                break;
            }
        }

        // + 2 is small workaround when the font is slightly bigger then the rect. This will
        // still allow the text to be shown without overflowing into the other all day rects.
        if (totalLineHeight == 0 || rect.top > bottom || rect.top + totalLineHeight + 2 < top) {
            return;
        }

        // Use a StaticLayout to format the string.
        canvas.save();
        //  canvas.translate(rect.left, rect.top + (rect.bottom - rect.top / 2));
        int padding = center? (rect.bottom - rect.top - totalLineHeight) / 2 : 0;
        canvas.translate(rect.left, rect.top + padding);
        rect.left = 0;
        rect.right = width;
        rect.top = 0;
        rect.bottom = totalLineHeight;

        // There's a bug somewhere. If this rect is outside of a previous
        // cliprect, this becomes a no-op. What happens is that the text draw
        // past the event rect. The current fix is to not draw the staticLayout
        // at all if it is completely out of bound.
        canvas.clipRect(rect);
        eventLayout.draw(canvas);
        canvas.restore();
    }
    
    //ZWNBS : Zero width no break space
    // ZWNBS is also known as the byte order mark (BOM) if used at the beginning of a Unicode file. 
    // It was originally used in the middle of Unicode files in rare instances where there was an invisible join between two characters where a line break must not occur. 
    // A new code joiner is being implemented U+2060 WORD JOINER.
    private final String ZWNBS_CHAR = "\uFEFF";
    private final String ELLIPSIS_CHAR = "\u2026";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
    private final String TWO_DOT_LEADER_CHAR = "\u2025";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
    private final String ONE_DOT_LEADER_CHAR = "\u2024";// \u2026 �� 0x2026�� ��Ÿ���� ellipsis 
    private void drawEventTitleLocationText(String title, String location, boolean twoLines, Rect rect, Canvas canvas, Paint paint, int top,
            int bottom) {   	
            
    	Paint titlePaint = new Paint(paint);
    	Paint locationPaint = new Paint(paint);    	
    	
    	titlePaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    	locationPaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
    	
    	int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;         
        
        float titleAscent = titlePaint.ascent();
    	float titleDescent = titlePaint.descent();
    	int titleLineHeight = (int) (Math.abs(titleAscent) + Math.abs(titleDescent));    	
    	
    	//paint.setTypeface(savePrvType);
    	float titleTextFontSize = titlePaint.getTextSize();
    	float locationFontSize = titleTextFontSize * 0.9f;
    	locationPaint.setTextSize(locationFontSize);
    	float locationAscent = locationPaint.ascent();
    	float locationDescent = locationPaint.descent();
    	int locationLineHeight = (int) (Math.abs(locationAscent) + Math.abs(locationDescent));   	
    	
    	int totalLineHeight = titleLineHeight + EVENT_TEXT_PAD_BETWEEN_LINES + locationLineHeight;            

        // Use a StaticLayout to format the string.
        canvas.save();
        //  canvas.translate(rect.left, rect.top + (rect.bottom - rect.top / 2));
        //int padding = center? (rect.bottom - rect.top - totalLineHeight) / 2 : 0;
        //canvas.translate(rect.left, rect.top + padding);
        canvas.translate(rect.left, rect.top);
        
        rect.left = 0;
        rect.top = 0;
        rect.right = width;        
        rect.bottom = totalLineHeight;

        // There's a bug somewhere. If this rect is outside of a previous
        // cliprect, this becomes a no-op. What happens is that the text draw
        // past the event rect. The current fix is to not draw the staticLayout
        // at all if it is completely out of bound.
        canvas.clipRect(rect);           
                
        if (twoLines) {        	
        	// y ��ǥ�� title text�� descent�� �ǹ��Ѵ�
	        canvas.drawText(title, rect.left, titleLineHeight - titleDescent, titlePaint);  
	        
	        // for test : title text�� ascent line�� drawing �Ѵ� 
	        //canvas.drawLine(0, titleLineHeight - titleDescent + titleAscent, rect.left + rect.width(), titleLineHeight - titleDescent + titleAscent, titlePaint);
	        // for test : title text�� descent line�� drawing �Ѵ� 
	        //canvas.drawLine(0, titleLineHeight - titleDescent, rect.left + rect.width(), titleLineHeight - titleDescent, titlePaint);	        
	        //canvas.drawLine(0, titleLineHeight, rect.left + rect.width(), titleLineHeight, titlePaint);	        
	    	
	        canvas.drawText(location, rect.left, totalLineHeight - locationDescent, locationPaint);        
        }
        else {    
        	//Rect bounds = new Rect();
	        //titlePaint.getTextBounds(title, 0, title.length(),bounds);
        	//int titleTextHeight = bounds.height();
        	// ������ event rect�� center�� �ؽ�Ʈ�� ��ġ��Ű�� ���ؼ� ������ ���� ������
        	int topMargin = (height - titleLineHeight) / 2; 
        	float textY = topMargin + Math.abs(titleAscent);
        	
	        canvas.drawText(title, rect.left, textY, titlePaint);  
	        float titleTextWidth = titlePaint.measureText(title);	        
	        
	        if (!location.isEmpty()) {
	        	String space = " ";
		        float spaceWidth = titlePaint.measureText(space);		        
	        
	        	canvas.drawText(location, rect.left + titleTextWidth + spaceWidth, textY, locationPaint);	        	
	        }
        }
        canvas.restore();
    }

    // This is to replace p.setStyle(Style.STROKE); canvas.drawRect() since it
    // doesn't work well with hardware acceleration
//    private void drawEmptyRect(Canvas canvas, Rect r, int color) {
//        int linesIndex = 0;
//        mLines[linesIndex++] = r.left;
//        mLines[linesIndex++] = r.top;
//        mLines[linesIndex++] = r.right;
//        mLines[linesIndex++] = r.top;
//
//        mLines[linesIndex++] = r.left;
//        mLines[linesIndex++] = r.bottom;
//        mLines[linesIndex++] = r.right;
//        mLines[linesIndex++] = r.bottom;
//
//        mLines[linesIndex++] = r.left;
//        mLines[linesIndex++] = r.top;
//        mLines[linesIndex++] = r.left;
//        mLines[linesIndex++] = r.bottom;
//
//        mLines[linesIndex++] = r.right;
//        mLines[linesIndex++] = r.top;
//        mLines[linesIndex++] = r.right;
//        mLines[linesIndex++] = r.bottom;
//        mPaint.setColor(color);
//        canvas.drawLines(mLines, 0, linesIndex, mPaint);
//    }   
    
    int mSelectedEventRectExpandingWhichCirclePoint;
    
    private void doDown(MotionEvent ev) {
    	
    	if (mFragment.getSelectionMode() == SELECTION_EVENT_FLOATING) {
    		//int action = ev.getAction(); 
			
			int x = (int) ev.getX();
	        int y = (int) ev.getY();
	        
	        // getLeft�� ������ ����ü ���� ����...
	        // mSelectedEventRect.getLeft�� getTop�� �� 0�� �����°�???
	        // �� ������ ������ ���� ������ �� ����
	        // :DayView.doLongPress���� setX/Y�� ����Ͽ��� ������ mLeft�� mTop�� ���� ������ ��� �����Ǿ� �ִ� �� ����
	        //  ->�� setX�� ȣ���Ѵٰ� �ؼ�...mLeft ���� ���ÿ� update���� �ʴ� ���� �ǹ��ϴ� �� ����	 
	        Rect validSelectedEventRect = new Rect();
			validSelectedEventRect.left = (int) mFragment.mSelectedEventRect.getX();
			validSelectedEventRect.top = (int) mFragment.mSelectedEventRect.getY();						
			validSelectedEventRect.right = validSelectedEventRect.left + mFragment.mSelectedEventRect.getWidth();
			validSelectedEventRect.bottom = validSelectedEventRect.top + mFragment.mSelectedEventRect.getHeight();		
						
			int interpolation = mFragment.mSelectedEventRect.getInterpolation();			
		    
			Rect upperExpandCircleRect = new Rect();
			int upperExpandCircleCenterX = (int) mFragment.mSelectedEventRect.getUpperExpandCircleRect().exactCenterX();
			int upperExpandCircleCenterY = (int) mFragment.mSelectedEventRect.getUpperExpandCircleRect().exactCenterY();
			upperExpandCircleRect.left = validSelectedEventRect.left + upperExpandCircleCenterX - interpolation;
			upperExpandCircleRect.top = validSelectedEventRect.top + upperExpandCircleCenterY - interpolation;
			upperExpandCircleRect.right = validSelectedEventRect.left + upperExpandCircleCenterX + interpolation;
			upperExpandCircleRect.bottom = validSelectedEventRect.top + upperExpandCircleCenterY + interpolation;
							
			Rect lowerExpandCircleRect = new Rect();
			int lowerExpandCircleCenterX = (int) mFragment.mSelectedEventRect.getLowerExpandCircleRect().exactCenterX();
			int lowerExpandCircleCenterY = (int) mFragment.mSelectedEventRect.getLowerExpandCircleRect().exactCenterY();
			lowerExpandCircleRect.left = validSelectedEventRect.left + lowerExpandCircleCenterX - interpolation;
			lowerExpandCircleRect.top = validSelectedEventRect.top + lowerExpandCircleCenterY - interpolation;
			lowerExpandCircleRect.right = validSelectedEventRect.left + lowerExpandCircleCenterX + interpolation;
			lowerExpandCircleRect.bottom = validSelectedEventRect.top + lowerExpandCircleCenterY + interpolation;
			
			if (upperExpandCircleRect.contains(x, y)) {
				// At Valid Selected Event Expand Circle Rect Area Inside Selected Event Rect Area
				if (INFO) Log.i(TAG, "doDown:SELECTION_EVENT_FLOATING:ValidExpandCircleRect");	   
				
				int daynum = mSelectionDay - mFirstJulianDay;
				
				mFragment.setPrvNewOrSelectedEventLeft(computeDayLeftPosition(daynum) + 1);
				int newRight = mFragment.getPrvNewOrSelectedEventLeft() + (mViewWidth - mHoursWidth);
				mFragment.setPrvNewOrSelectedEventRight(newRight);   
	        		        	
	        	mFragment.setPrvNewOrSelectedEventTop(validSelectedEventRect.top);	            
	            mFragment.setPrvNewOrSelectedEventBottom(validSelectedEventRect.bottom);
	            
	            mSelectionEndHour = mFragment.getSelectedEvent().endTime / 60;
				mSelectionEndMinutes = mFragment.getSelectedEvent().endTime % 60;
				switch(mSelectionEndMinutes) {
		        case 0:
		        	mSelectionEndMinutesPixels = 0;
		        	break;
		        case 15:
		        	mSelectionEndMinutesPixels = mOneQuartersOfHourPixelHeight;
		        	break;
		        case 30:
		        	mSelectionEndMinutesPixels = 2* mOneQuartersOfHourPixelHeight;
		        	break;
		        case 45:
		        	mSelectionEndMinutesPixels = 3* mOneQuartersOfHourPixelHeight;
		        	break;
		        }
	        					
				mFragment.setPrvNewOrSelectedEventMoveX(x);
				mFragment.setPrvNewOrSelectedEventMoveY(y);
				//mFragment.setPrvNewOrSelectedEventMoveY(validSelectedEventRect.top);////////////////////////////////////			
				
				mFragment.setSelectionMode(SELECTION_EVENT_EXPAND_SELECTION, SELECTION_DOWN_GESTURE);		
				
				mSelectedEventRectExpandingWhichCirclePoint = SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT;
			    
			}
			else if (lowerExpandCircleRect.contains(x, y)) {
				if (INFO) Log.i(TAG, "doDown:SELECTION_EVENT_FLOATING:ValidExpandCircleRect");	   
				
				int daynum = mSelectionDay - mFirstJulianDay;
				mFragment.setPrvNewOrSelectedEventLeft(computeDayLeftPosition(daynum) + 1);
				int newRight = mFragment.getPrvNewOrSelectedEventLeft() + (mViewWidth - mHoursWidth);
				mFragment.setPrvNewOrSelectedEventRight(newRight);   
	        	
				mFragment.setPrvNewOrSelectedEventTop(validSelectedEventRect.top);
				mFragment.setPrvNewOrSelectedEventBottom(validSelectedEventRect.bottom);
	        	
				//mMinutuePosPixelAtSelectedEventMove = ?;
				mSelectionEndHour = mFragment.getSelectedEvent().endTime / 60;
				mSelectionEndMinutes = mFragment.getSelectedEvent().endTime % 60;
				switch(mSelectionEndMinutes) {
		        case 0:
		        	mSelectionEndMinutesPixels = 0;
		        	break;
		        case 15:
		        	mSelectionEndMinutesPixels = mOneQuartersOfHourPixelHeight;
		        	break;
		        case 30:
		        	mSelectionEndMinutesPixels = 2* mOneQuartersOfHourPixelHeight;
		        	break;
		        case 45:
		        	mSelectionEndMinutesPixels = 3* mOneQuartersOfHourPixelHeight;
		        	break;
		        }
				
				mSelectedEventRectHeight = mFragment.getSelectedEventRect().getHeight();
				
	            mFragment.setPrvNewOrSelectedEventMoveX(x);
				mFragment.setPrvNewOrSelectedEventMoveY(y);
				
				mFragment.setSelectionMode(SELECTION_EVENT_EXPAND_SELECTION, SELECTION_DOWN_GESTURE);	
				
				mSelectedEventRectExpandingWhichCirclePoint = SELECTION_EVENT_EXPANDING_LOWER_CIRCLEPOINT;
			}
			else if (validSelectedEventRect.contains(x, y)) {
				if (INFO) Log.i(TAG, "doDown:SELECTION_EVENT_FLOATING:ValidSelectedEventRect");	
				
				// At Valid Selected Event Rect Area
				int daynum = mSelectionDay - mFirstJulianDay;
				
				mFragment.setPrvNewOrSelectedEventLeft(computeDayLeftPosition(daynum) + 1);
				int newRight = mFragment.getPrvNewOrSelectedEventLeft() + (mViewWidth - mHoursWidth);
				mFragment.setPrvNewOrSelectedEventRight(newRight);   
	        	
				mFragment.setPrvNewOrSelectedEventTop(validSelectedEventRect.top);
				mFragment.setPrvNewOrSelectedEventBottom(validSelectedEventRect.bottom);
	        	
	            mFragment.setPrvNewOrSelectedEventMoveX(x);
				mFragment.setPrvNewOrSelectedEventMoveY(y);
				mFragment.setSelectionMode(SELECTION_EVENT_PRESSED, SELECTION_DOWN_GESTURE);	
			}
			else {
				if (INFO) Log.i(TAG, "doDown:SELECTION_EVENT_FLOATING:NonValidSelectedEventRect");	
				
				mTouchMode = TOUCH_MODE_DOWN;////////////////////////////////////////
	    		mViewStartX = 0;
	    		mOnFlingCalled = false;
	    		mHandler.removeCallbacks(mContinueScroll);
			}		
		}
    	else {    	
    		mTouchMode = TOUCH_MODE_DOWN;////////////////////////////////////////
    		mViewStartX = 0;
    		mOnFlingCalled = false;
    		mHandler.removeCallbacks(mContinueScroll);        
    	}
    	
        invalidate();        
    }

    // Kicks off all the animations when the expand allday area is tapped
    /*
    private void doExpandAllDayClick() {
        mShowAllAllDayEvents = !mShowAllAllDayEvents;

        ObjectAnimator.setFrameDelay(0);

        // Determine the starting height
        if (mAnimateDayHeight == 0) {
            mAnimateDayHeight = mShowAllAllDayEvents ?
                    mAlldayHeight - (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT : mAlldayHeight;
        }
        // Cancel current animations
        mCancellingAnimations = true;
        if (mAlldayAnimator != null) {
            mAlldayAnimator.cancel();
        }
        if (mAlldayEventAnimator != null) {
            mAlldayEventAnimator.cancel();
        }
        if (mMoreAlldayEventsAnimator != null) {
            mMoreAlldayEventsAnimator.cancel();
        }
        mCancellingAnimations = false;
        // get new animators
        mAlldayAnimator = getAllDayAnimator();
        mAlldayEventAnimator = getAllDayEventAnimator();
        mMoreAlldayEventsAnimator = ObjectAnimator.ofInt(this,
                    "moreAllDayEventsTextAlpha",
                    mShowAllAllDayEvents ? MORE_EVENTS_MAX_ALPHA : 0,
                    mShowAllAllDayEvents ? 0 : MORE_EVENTS_MAX_ALPHA);

        // Set up delays and start the animators
        mAlldayAnimator.setStartDelay(mShowAllAllDayEvents ? ANIMATION_SECONDARY_DURATION : 0);
        mAlldayAnimator.start();
        mMoreAlldayEventsAnimator.setStartDelay(mShowAllAllDayEvents ? 0 : ANIMATION_DURATION);
        mMoreAlldayEventsAnimator.setDuration(ANIMATION_SECONDARY_DURATION);
        mMoreAlldayEventsAnimator.start();
        if (mAlldayEventAnimator != null) {
            // This is the only animator that can return null, so check it
            mAlldayEventAnimator
                    .setStartDelay(mShowAllAllDayEvents ? ANIMATION_SECONDARY_DURATION : 0);
            mAlldayEventAnimator.start();
        }
    }
	*/
    

    // Sets up an animator for changing the height of allday events
    /*
    private ObjectAnimator getAllDayEventAnimator() {
        // First calculate the absolute max height
        int maxADHeight = mViewHeight - DAY_HEADER_HEIGHT - MIN_HOURS_HEIGHT;
        // Now expand to fit but not beyond the absolute max
        maxADHeight =
                Math.min(maxADHeight, (int)(mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
        // calculate the height of individual events in order to fit
        int fitHeight = maxADHeight / mMaxAlldayEvents;
        int currentHeight = mAnimateDayEventHeight;
        int desiredHeight =
                mShowAllAllDayEvents ? fitHeight : (int)MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
        // if there's nothing to animate just return
        if (currentHeight == desiredHeight) {
            return null;
        }

        // Set up the animator with the calculated values
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "animateDayEventHeight",
                currentHeight, desiredHeight);
        animator.setDuration(ANIMATION_DURATION);
        return animator;
    }
	*/
    
    // Sets up an animator for changing the height of the allday area
    /*
    private ObjectAnimator getAllDayAnimator() {
        // Calculate the absolute max height
        int maxADHeight = mViewHeight - DAY_HEADER_HEIGHT - MIN_HOURS_HEIGHT;
        // Find the desired height but don't exceed abs max
        maxADHeight =
                Math.min(maxADHeight, (int)(mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
        // calculate the current and desired heights
        int currentHeight = mAnimateDayHeight != 0 ? mAnimateDayHeight : mAlldayHeight;
        int desiredHeight = mShowAllAllDayEvents ? maxADHeight :
                (int) (MAX_UNEXPANDED_ALLDAY_HEIGHT - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT - 1);

        // Set up the animator with the calculated values
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "animateDayHeight",
                currentHeight, desiredHeight);
        animator.setDuration(ANIMATION_DURATION);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCancellingAnimations) {
                    // when finished, set this to 0 to signify not animating
                    mAnimateDayHeight = 0;
                    mUseExpandIcon = !mShowAllAllDayEvents;
                }
                mRemeasure = true;
                invalidate();
            }
        });
        return animator;
    }
	*/
    
    // setter for the 'box +n' alpha text used by the animator
    /*
    public void setMoreAllDayEventsTextAlpha(int alpha) {
        mMoreAlldayEventsTextAlpha = alpha;
        invalidate();
    }
	
    // setter for the height of the allday area used by the animator
    public void setAnimateDayHeight(int height) {
        mAnimateDayHeight = height;
        mRemeasure = true;
        invalidate();
    }

    // setter for the height of allday events used by the animator
    public void setAnimateDayEventHeight(int height) {
        mAnimateDayEventHeight = height;
        mRemeasure = true;
        invalidate();
    }
	*/
    
    
    private void doSingleTapUp(MotionEvent ev) {
        /*if (mScrolling) {
            return;
        }*/

        int x = (int) ev.getX();
        int y = (int) ev.getY();
               
        
        if (mFragment.getSelectionMode() == SELECTION_HIDDEN) {
        	int selectedTop = y - (mCellHeight / 2);
        	boolean validPosition = setSelectionFromPosition(x, y, selectedTop);
			//////////////////////////////////////////////////////////////
			// ��ȿ�� ������ �ƴ϶�� ���� day view�� time lines�� ���ϴ� ���̴�
			if (!validPosition) {
				// ���� ���� day view�� weekly �����̶��
				// monthday ������ ���õǾ��������� Ȯ���ؾ� �Ѵ�
				if (y < DAY_HEADER_HEIGHT) {
										
					Calendar selectedTime = GregorianCalendar.getInstance(mBaseDate.getTimeZone());
					long selectionDayMillis = ETime.getMillisFromJulianDay(mSelectionDay, mTimeZone, mFirstDayOfWeek);
					
					selectedTime.setTimeInMillis(selectionDayMillis);
					selectedTime.set(Calendar.HOUR_OF_DAY, mSelectionStartHour);//selectedTime.hour = mSelectionStartHour;
					
					mController.sendEvent(this, EventType.GO_TO, null, null, selectedTime, -1,
					ViewType.DAY, CalendarController.EXTRA_GOTO_DATE, null, null);
				}
				
				return;
			}
			
			if (mFragment.getSelectedEvent() != null) {
	        	// mSelectedEvent != null ����[x,y ��ǥ���� Ư�� event rect�ȿ� ���Ե�]���� mSelectionMode�� ��� ����
	        	// 1.SELECTION_HIDDEN : �� ��쿡�� Ư�� event�� ���õ� ���
	        	// 2.SELECTION_EVENT_PRESSED : �� ��쿡�� Ư�� event�� ���õ� ���
	        	// 3.SELECTION_EVENT_FLOATING : �� ��쿡�� floating�� selected event rect�� ���õ� ���
	        	// �� �� ���� ��� �� ���� : ������� launchFastEventInfoView�� ȣ���Ѵ�
	        	//                   mSelectionMode�� ���� ������ �ʿ䰡 ����            
	            mFragment.launchFastEventInfoView(mFragment.getSelectedEvent().id, mSelectionDay,
	            		mFragment.getSelectedEvent().startMillis, mFragment.getSelectedEvent().endMillis, null);/////////////////////////////////////////////////////////////////////////////                  
	            
	            return;            
	        }
		}
        else if (mFragment.getSelectionMode() == SELECTION_EVENT_PRESSED) { // SELECTION_EVENT_FLOATING ���¿��� doDown�� �߻��ϸ� SELECTION_EVENT_PRESSED�� ���̵�
        	
        	Rect validSelectedEventRect = new Rect();
			validSelectedEventRect.left = (int) mFragment.mSelectedEventRect.getX();
			validSelectedEventRect.top = (int) mFragment.mSelectedEventRect.getY();					
			validSelectedEventRect.right = validSelectedEventRect.left + mFragment.mSelectedEventRect.getWidth();
			validSelectedEventRect.bottom = validSelectedEventRect.top + mFragment.mSelectedEventRect.getHeight();
			
			// �Ʒ� if/else ������ �ǹ̰� ������?
			// :SELECTION_EVENT_FLOATING ���¿��� doDown�� �߻��ؼ� SELECTION_EVENT_PRESSED�� ���̵Ǿ��� ������
			//  doDown�� �̹� x,y��  üũ[x,y is Inside Valid Selected Event Rect]�� �Ͽ���
			if (validSelectedEventRect.contains(x, y)) {	
				// selected event�� new start/end �ð� ������ update �Ǿ� ���� �ʴ� ��Ȳ�� �߻��Ѵ�
				// �츮�� ���⼭ selected event�� new start/end �ð� ���� ������Ʈ�� �Ϸ�Ǿ����� �ƴ����� Ȯ���� ��,
				// fast event info view�� ��ġ�ؾ� ���� ������ �ʹ�
				// :�츮�� ��� mEventInfoLoaderSuccessCallback�� sync�� ������ΰ�? 							
				if (mFragment.mSelectedEventRequiredUpdate) {
					if (mFragment.mSelectedEventUpdateCompletion) {
						mFragment.launchFastEventInfoView(mFragment.getSelectedEvent().id, mSelectionDay,
								mFragment.getSelectedEvent().startMillis, mFragment.getSelectedEvent().endMillis, mHideSelectedEventRect);//////////////////////////////////////////////////////////
						
						mFragment.getSelectedEventRect().setVisibility(View.GONE);	
						mFragment.resetSelectedEventRequireUpdateFlags();						
						mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_SINGLETAPUP_GESTURE);
					}
				}
				else {
					mFragment.launchFastEventInfoView(mFragment.getSelectedEvent().id, mSelectionDay,
							mFragment.getSelectedEvent().startMillis, mFragment.getSelectedEvent().endMillis, mHideSelectedEventRect);
					
					mFragment.getSelectedEventRect().setVisibility(View.GONE);	
					mFragment.resetSelectedEventRequireUpdateFlags();
					mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_SINGLETAPUP_GESTURE);
				}
			}
			else {
				mFragment.getSelectedEventRect().setVisibility(View.GONE);	
				mFragment.resetSelectedEventRequireUpdateFlags();
				mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_SINGLETAPUP_GESTURE);
			}       	
        } 
        // �Ʒ� ��Ȳ�� invalid selected event rect���� single tapup�� �߻��� ��츸 �߻��ȴ�
		else if (mFragment.getSelectionMode() == SELECTION_EVENT_FLOATING) { 
			Rect validSelectedEventRect = new Rect();
			validSelectedEventRect.left = (int) mFragment.mSelectedEventRect.getX();
			validSelectedEventRect.top = (int) mFragment.mSelectedEventRect.getY();					
			validSelectedEventRect.right = validSelectedEventRect.left + mFragment.mSelectedEventRect.getWidth();
			validSelectedEventRect.bottom = validSelectedEventRect.top + mFragment.mSelectedEventRect.getHeight();			
			
			if (!validSelectedEventRect.contains(x, y)) {	
				mFragment.getSelectedEventRect().setVisibility(View.GONE);
				mFragment.resetSelectedEventRequireUpdateFlags();
				mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_SINGLETAPUP_GESTURE);
			}					
			
		}       
        
    }
    
    Runnable mHideSelectedEventRect = new Runnable() {

		@Override
		public void run() {
			mFragment.getSelectedEventRect().setVisibility(View.GONE);
			
		}
    	
    };

    
    
    
    
    
    
    private void doLongPress(MotionEvent ev) {
        eventClickCleanup();
        if (mScrolling) {
            return;
        }

        // Scale gesture in progress
        if (mStartingSpanY != 0) {
            return;
        }

        if (mFragment.getSelectionMode() == SELECTION_HIDDEN) {
        	int x = (int) ev.getX();
            int y = (int) ev.getY();

            int selectedTop = y - (mCellHeight / 2);
            boolean validPosition = setSelectionFromPosition(x, y, selectedTop);
            if (!validPosition) {
                // return if the touch wasn't on an area of concern
                return;
            }
            
            mFragment.setPrvNewOrSelectedEventMoveX(x);
            mFragment.setPrvNewOrSelectedEventMoveY(y);        	

        	
            if (mFragment.getSelectedEvent() != null) {
            	// mSelectedEvent != null ����[x,y ��ǥ���� Ư�� event rect�ȿ� ���Ե�]���� mSelectionMode�� ����
            	// 1.SELECTION_HIDDEN
            	// 2.SELECTION_EVENT_FLOATING : �� ������� SELECTION_EVENT_PRESSED�� �����ص� �� �� ����   
            	// �� �� ���� ��� �� ����
            	
            	mFragment.setSelectionMode(SELECTION_EVENT_PRESSED, SELECTION_LONGPRESS_GESTURE);
            	mFragment.getSelectedEventRect().setEventRectType(SELECTION_EVENT_PRESSED);
            	int margin = GRID_LINE_TOP_MARGIN / 2;
            	mFragment.getSelectedEventRect().setTopMargin(margin);
            	mFragment.getSelectedEventRect().setBottomMargin(margin);
            	
                LinearLayout.LayoutParams selectedEventRectParam = (LinearLayout.LayoutParams) mFragment.getSelectedEventRect().getLayoutParams();
    	        int effectiveWidth = mViewWidth - getHoursWidth();
    	        int cellHeight = (int) (mFragment.getSelectedEvent().bottom - mFragment.getSelectedEvent().top) + GRID_LINE_TOP_MARGIN;    
    	        selectedEventRectParam.width = effectiveWidth;
    	        selectedEventRectParam.height = cellHeight;///////////////////////////////////////////////////////////
    	        mFragment.getSelectedEventRect().setLayoutParams(selectedEventRectParam);	
    	        
    	        // ������ all day header�� �ִ� ����̴�
                // ��� �ذ��� ���ΰ�???
    	        int top = (int) ((GRID_LINE_TOP_MARGIN + HOUR_GAP + mFragment.getSelectedEvent().top + EVENT_RECT_TOP_MARGIN) - mViewStartY);
    	        mFragment.setPrvNewOrSelectedEventTop(top);            	
            	mFragment.setPrvNewOrSelectedEventBottom(mFragment.getPrvNewOrSelectedEventTop() + cellHeight);
            }
            else {
            	// mSelectedEvent == null ���¿��� mSelectionMode�� ����
            	// 1.SELECTION_HIDDEN
            	// �� �� ���� ��� �� ����
            	
            	mFragment.setSelectionMode(SELECTION_NEW_EVENT, SELECTION_LONGPRESS_GESTURE);    
            	mFragment.getSelectedEventRect().setEventRectType(SELECTION_NEW_EVENT);
            	
            	LinearLayout.LayoutParams selectedEventRectParam = (LinearLayout.LayoutParams) mFragment.getSelectedEventRect().getLayoutParams();
    	        int effectiveWidth = mViewWidth - getHoursWidth();
    	        int cellHeight = Utils.getSharedPreference(mContext,
    	                SettingsFragment.KEY_DEFAULT_CELL_HEIGHT, DayView.DEFAULT_CELL_HEIGHT);    
    	        selectedEventRectParam.width = effectiveWidth;
    	        selectedEventRectParam.height = cellHeight;
    	        mFragment.getSelectedEventRect().setLayoutParams(selectedEventRectParam);	        
    	        
    	        int top = y - (mCellHeight / 2);
    	        mFragment.setPrvNewOrSelectedEventTop(top);    	        
    	        mFragment.setPrvNewOrSelectedEventBottom(mFragment.getPrvNewOrSelectedEventTop() + mCellHeight);
            } 
                	
        	// �հ� �ƴѵ� �ϴ�...�ϴ� ���� ���߱������ �Ʒ� computeDayLeftPosition�� �����
        	int daynum = mSelectionDay - mFirstJulianDay;
        	mFragment.setPrvNewOrSelectedEventLeft(computeDayLeftPosition(daynum) + 1);
        	int newRight = mFragment.getPrvNewOrSelectedEventLeft() + (mViewWidth - mHoursWidth);
			mFragment.setPrvNewOrSelectedEventRight(newRight);    
        	
        	mFragment.getSelectedEventRect().setVisibility(View.VISIBLE);
        	// setX/Y�� ������ �Ͽ��� ������...getLeft�� getTop�� ���� ���� ��[0, 0]���� ���� �� ����
        	mFragment.getSelectedEventRect().setX(mFragment.getPrvNewOrSelectedEventLeft()); 
        	
        	if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT) {        		
            	mFragment.getSelectedEventRect().setY(mFragment.getPrvNewOrSelectedEventTop());
        	}
        	else {
        		setSelectedEventRectEventText();
        		int outLineY = (int) (mFragment.getPrvNewOrSelectedEventTop() - mFragment.getSelectedEventRect().getTopMargin());
            	mFragment.getSelectedEventRect().setY(outLineY);
        	}
        	
        	invalidate();               
        }     
        
    }
    
    
    public void setSelectedEventRectEventText() {
    	Paint eventTextPaint = mEventTextPaint;
    	
    	Rect rect = new Rect();
    	rect.top = (int) mFragment.getSelectedEvent().top + EVENT_RECT_TOP_MARGIN;
    	rect.bottom = (int) mFragment.getSelectedEvent().bottom - EVENT_RECT_BOTTOM_MARGIN;        
    	rect.left = (int) mFragment.getSelectedEvent().left + EVENT_RECT_LEFT_MARGIN;
    	rect.right = (int) mFragment.getSelectedEvent().right;
        setupTextRect(rect);
        
    	boolean twoLines = true;
    	int eventHeight = (int) (mFragment.getSelectedEvent().bottom - mFragment.getSelectedEvent().top);   	
    	if (eventHeight < mAvailEventRectHeightByDefaultEventTextHeight) { // �̺�Ʈ ���� �Ⱓ�� 30�� �̸��̸� font size�� event height�� �����
    		// event height�� 50% size�� ����� : 
    		float eventFontSize = getEventFontSize(eventHeight, 0.5f);
    		eventTextPaint.setTextSize(eventFontSize);            		
        }
    	
    	if (eventHeight < mMinEventRectHeightTwoLinesEventText) { // �̺�Ʈ ���� �Ⱓ�� 45�� �̸��̸� ������ single line�� title�� location�� �����Ѵ�         		
    		twoLines = false;
        }
    	   
        
    	float availWidth = mViewWidth - getHoursWidth(); 	
        String title = getEventTitle(mFragment.getSelectedEvent(), eventTextPaint, availWidth);                
             	              	
        	
    	float locationTextAvailWidth = availWidth;
    	if (!twoLines) {
    		float titleTextConsumedWidth = eventTextPaint.measureText(title);
    		String space = " ";
    		float spaceWidth = eventTextPaint.measureText(space);
    		locationTextAvailWidth = availWidth - titleTextConsumedWidth - spaceWidth;            		
    	}            	            	
    	
    	String location = getEventLocation(mFragment.getSelectedEvent(), eventTextPaint, locationTextAvailWidth);                   
        
    	int eventRectHeight = rect.bottom - rect.top;    	
    	
    	mFragment.getSelectedEventRect().setEventRectViewContext(twoLines, title, location, eventTextPaint, eventHeight);
        
        // font size recovery!!!
        //if (eventHeight < mTwoQuartersOfHourPixelHeight) {
    		eventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
        //}
    }
    
    
    
    private void doScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
    	
    	// cancelAnimation ȣ��� ����
        // goTo���� viewswitch�� �̻� ����[viewswitcher�� view�� �߿� �ϳ��� in/out animation�� �������� ����]��
        //cancelAnimation();    	
    	
        // +deltaX = finger�� left�� �̵� : �ð��밡 forward[�̷�]�� �̵���
    	// -deltaX = finger�� right�� �̵� : �ð��밡 backward[����]�� �̵���
    	//if (INFO) Log.i(TAG, "doScroll:deltaX=" + String.valueOf(deltaX) + ", deltaY=" + String.valueOf(deltaY));
    	
        if (mStartingScroll) {
            mInitialScrollX = 0;
            mInitialScrollY = 0;
            mStartingScroll = false;
        }

        mInitialScrollX += deltaX;
        mInitialScrollY += deltaY;
        int distanceX = (int) mInitialScrollX;
        int distanceY = (int) mInitialScrollY;

        //final float focusY = getAverageY(e2);        
        
        // If we haven't figured out the predominant scroll direction yet,
        // then do it now.
        if (mTouchMode == TOUCH_MODE_DOWN) {
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            mScrollStartY = mViewStartY;
            mPreviousDirection = 0;

            if (absDistanceX > absDistanceY) {
                //int slopFactor = mScaleGestureDetector.isInProgress() ? 20 : 2;
                //if (absDistanceX > mScaledPagingTouchSlop * slopFactor) {
                    mTouchMode = TOUCH_MODE_HSCROLL;
                    if (mFragment.getSelectionMode() == SELECTION_EVENT_FLOATING) {
                    	mFragment.getSelectedEventRect().setVisibility(View.GONE);	
                    	mFragment.resetSelectedEventRequireUpdateFlags();
                		mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_ACTION_UP_TOUCHEVENT);
                    }
                    
                    mViewStartX = distanceX;
                    if (INFO) Log.i(TAG, "Call initNextView:A");
                    mFragment.initNextView(-mViewStartX);
                    mFragment.moveDayViews(-mViewStartX);
                    if (distanceX != 0) {
                    	int direction = (distanceX > 0) ? 1 : -1;
                    	mPreviousDirection = direction;
                    }
                    if (INFO) Log.i(TAG, "doScroll:First HSCROLL");
                    
                //}
            } else {
                mTouchMode = TOUCH_MODE_VSCROLL;
            }
            
        } else if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            // We are already scrolling horizontally, so check if we
            // changed the direction of scrolling so that the other week
            // is now visible.
            mViewStartX = distanceX;
            mFragment.moveDayViews(-mViewStartX);
            if (distanceX != 0) {
                int direction = (distanceX > 0) ? 1 : -1;
                if (direction != mPreviousDirection) {
                    // The user has switched the direction of scrolling
                    // so re-init the next view
                	if (INFO) Log.i(TAG, "HSCROLL Direction Change");
                	mFragment.initNextView(-mViewStartX);
                    mPreviousDirection = direction;
                }
            }
            
        }
        

        if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
            // Calculate the top of the visible region in the calendar grid.
            // Increasing/decrease this will scroll the calendar grid up/down.
        	// DAY_GAP�� �ƴ϶� HOUR_GAP�� mCellHeight�� ���ؾ� ��
            //mViewStartY = (int) ( /*GRID_LINE_TOP_MARGIN +*/ (mGestureCenterHour * (mCellHeight + DAY_GAP)) - focusY + DAY_HEADER_HEIGHT + mAlldayHeight );
            //mViewStartY = (int) ( /*GRID_LINE_TOP_MARGIN +*/ (mGestureCenterHour * (mCellHeight + HOUR_GAP)) - focusY + DAY_HEADER_HEIGHT + mAlldayHeight );
        	mViewStartY = (int) (mViewStartY + deltaY);

            // If dragging while already at the end, do a glow
            final int pulledToY = (int) (mScrollStartY + deltaY);
            if (pulledToY < 0) {
                mEdgeEffectTop.onPull(deltaY / mViewHeight);
                if (!mEdgeEffectBottom.isFinished()) {
                    mEdgeEffectBottom.onRelease();
                }
            } else if (pulledToY > mMaxViewStartY) {
                mEdgeEffectBottom.onPull(deltaY / mViewHeight);
                if (!mEdgeEffectTop.isFinished()) {
                    mEdgeEffectTop.onRelease();
                }
            }

            //if (INFO) Log.i(TAG, "mViewStartY=" + String.valueOf(mViewStartY));
            
            if (mViewStartY < 0) {
                mViewStartY = 0;
                //mRecalCenterHour = true;
            } else if (mViewStartY > mMaxViewStartY) {
                mViewStartY = mMaxViewStartY;
                //mRecalCenterHour = true;
            }
            
            computeFirstHour();
        }

        mScrolling = true;/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        //if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
        	invalidate();
        //}
       
    }
    
    
    private float getAverageY(MotionEvent me) {
        int count = me.getPointerCount();
        float focusY = 0;
        for (int i = 0; i < count; i++) {
            focusY += me.getY(i);
        }
        focusY /= count;
        return focusY;
    }

    private void cancelAnimation() {
    	/*
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
        */
    }

    private void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    	// cancelAnimation ȣ��� ����
        // goTo���� viewswitch�� �̻� ����[viewswitcher�� view�� �߿� �ϳ��� in/out animation�� �������� ����]��
        //cancelAnimation();

        //mSelectionMode = SELECTION_HIDDEN;
        eventClickCleanup();

        mOnFlingCalled = true;

        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            // Horizontal fling.
            // initNextView(deltaX); : mTouchMode�� TOUCH_MODE_HSCROLL�� �����Ǿ��ٴ� ����
        	                       //  �̹� onScroll���� ������ �Ͽ��ٴ� ���� �ǹ��Ѵ�...
        	float velocityXAbs = Math.abs(velocityX);
        	
        	if (velocityXAbs > CalendarViewsSecondaryActionBar.FlING_THRESHOLD_VELOCITY) {
        		mTouchMode = TOUCH_MODE_INITIAL_STATE;
                
                int e2_X = (int)e2.getX();
                int e1_X = (int)e1.getX();
                int deltaX = e2_X - e1_X;
                //int deltaX = (int) e2.getX() - (int) e1.getX();
                
                mFragment.switchViews(deltaX < 0, false, mViewStartX, mViewWidth);//mFragment.switchViews(mBaseDate, deltaX < 0, false, mViewStartX, mViewWidth);
                mViewStartX = 0;
        	}
        	else {
        		if (INFO) Log.i(TAG, "doFling");
        		mFragment.makeInPlaceAnimation();
        		//makeInPlaceAnim(2);
        		//mDeltaOfReturnInPlaceTranslateAnimator.start();
        	}
        	            
            return;
        }

        if ((mTouchMode & TOUCH_MODE_VSCROLL) == 0) {            
            return;
        }

        // Vertical fling.
        mTouchMode = TOUCH_MODE_INITIAL_STATE;
        mViewStartX = 0;       

        // Continue scrolling vertically
        mScrolling = true;
        mScroller.fling(0 /* startX */, mViewStartY /* startY */, 0 /* velocityX */,
                (int) -velocityY, 0 /* minX */, 0 /* maxX */, 0 /* minY */,
                mMaxViewStartY /* maxY */, OVERFLING_DISTANCE, OVERFLING_DISTANCE);

        // When flinging down, show a glow when it hits the end only if it
        // wasn't started at the top
        if (velocityY > 0 && mViewStartY != 0) {
            mCallEdgeEffectOnAbsorb = true;
        }
        // When flinging up, show a glow when it hits the end only if it wasn't
        // started at the bottom
        else if (velocityY < 0 && mViewStartY != mMaxViewStartY) {
            mCallEdgeEffectOnAbsorb = true;
        }
        
        if (!mHandler.post(mContinueScroll))
        	if (INFO) Log.d(TAG, "doFling: post return false OOOOps!!!");
            

    }

    boolean mAmINextView = false;
    /*
    private boolean initNextView(int deltaX) {
    	if (INFO) Log.i(TAG, "+initNextView");
        // Change the view to the previous day or week
        DayView nextView = (DayView) mViewSwitcher.getNextView();
        Time date = nextView.mBaseDate;
        date.set(mBaseDate);
        boolean switchForward;
        // deltaX�� initNextView�� ȣ���� ������
        // ���� ���� -���� �ٿ� ��ȣ�� �ݴ�� ������
        if (deltaX > 0) {  // onScroll�� ������ ���� -deltaX :finger�� right�� �̵� : �ð��밡 backward[����]�� �̵���
            date.monthDay -= mNumDays;
            nextView.setSelectedDay(mSelectionDay - mNumDays);
            switchForward = false;
        } else {
            date.monthDay += mNumDays;
            nextView.setSelectedDay(mSelectionDay + mNumDays);
            switchForward = true;
        }
        date.normalize(true);        
        
        if (switchForward) {
        	// current view�� �������� �̵��ؼ� next view�� �����ʿ��� �����ؾ� �ϴ� ��� 
        	// nextView�� next day�� display �ؾ� �Ѵ�
        	    	
        	//nextView.mEvents = mFragment.mNextDayViewEvents;
        	//nextView.mAllDayEvents = mFragment.mNextDayViewAllDayEvents;          
        	nextView.mEvents = mFragment.mCircularEventsMap.get(DayFragment.NEXT_DAY_EVENTS).mEvents;
        	nextView.mAllDayEvents = mFragment.mCircularEventsMap.get(DayFragment.NEXT_DAY_EVENTS).mAllDayEvents;        
        }
        else {
        	// current view�� ���������� �̵��ؼ� next view�� ���ʿ��� �����ؾ� �ϴ� ��� 
        	// nextView�� previous day�� display �ؾ� �Ѵ�
        	
        	//nextView.mEvents = mFragment.mPreviousDayViewEvents;
        	//nextView.mAllDayEvents = mFragment.mPreviousDayViewAllDayEvents;     
        	
        	nextView.mEvents = mFragment.mCircularEventsMap.get(DayFragment.PREVIOUS_DAY_EVENTS).mEvents;
        	nextView.mAllDayEvents = mFragment.mCircularEventsMap.get(DayFragment.PREVIOUS_DAY_EVENTS).mAllDayEvents;        
        }
        
        nextView.mAmINextView = true;
        //nextView.recalc(); 
        initView(nextView);
        int nextViewLeft = getLeft();
        int nextViewTop = getTop();
        int nextViewRight = getRight();
        int nextViewBottom = getBottom();
        
        nextView.layout(nextViewLeft, nextViewTop, nextViewRight, nextViewBottom);      
        
        nextView.recalc();  
        nextView.computeEventRelations();  
    
    	//nextView.mRemeasure = true;//////////////////////////////////////////////////////////////////////////////////////////////////////////
    	nextView.remeasure(getWidth(), getHeight());
    	nextView.mComputeSelectedEvents = true;
    	//nextView.recalc();  
    	
    	// �� ������ �� ����...
    	// �� ������...����...
        nextView.invalidate();////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        if (INFO) Log.i(TAG, "-initNextView");
        return switchForward;
    }
	*/
    
    
    //private final Rect mNewEventBox = new Rect();
    
    
    boolean mAutoVerticalScrollingByNewEventRect = false;
    boolean mAutoSwitchingDayViewByNewEventRect = false;
    Object mAutoVerticalScrollingSync = new Object();
    //boolean mSwitchedToNextView = false; 
    boolean mOnlyOneTime = false;
    public boolean onTouchEventX(MotionEvent ev) {
        int action = ev.getAction();       
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            	
            	//if (INFO) Log.i(TAG, "ACTION_DOWN View=" + DayView.this);            	
                mStartingScroll = true;
                  
                /*
                int bottom = mAlldayHeight + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
                if (ev.getY() < bottom) {
                    mTouchStartedInAlldayArea = true;
                } else {
                    mTouchStartedInAlldayArea = false;
                }                
                */
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_MOVE:
                //if (INFO) Log.i(TAG, "ACTION_MOVE View=" + DayView.this);
                
                // ��ٷ� SELECTION_NEW_EVENT_MOVED�� ���� �ʴ� ������
            	// mPrevBox�� �������� �ʴ� ��찡 �߻��߱� �����̴�
            	// ������ onLongPress���� invalidate�� ���� ���
            	// ��� onDraw�� ȣ����� �ʰ�
            	// ���� onLongPress�� ȣ��� �����߿� ACTION_MOVE�� �߻��Ѵٸ�
            	// onTouchEvent�� ȣ��Ǵ� �� ����
                if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT) {
                	mFragment.setSelectionMode(SELECTION_NEW_EVENT_MOVED, SELECTION_ACTION_MOVE_TOUCHEVENT); 
                	mGestureDetector.onTouchEvent(ev);
                }
                else if (mFragment.getSelectionMode() == SELECTION_EVENT_PRESSED) {
                	int x = (int) ev.getX();
                    int y = (int) ev.getY();       
                    
                    mFragment.setPrvNewOrSelectedEventMoveX(x);
                	mFragment.setPrvNewOrSelectedEventMoveY(y);
                	mFragment.setSelectionMode(SELECTION_EVENT_MOVED, SELECTION_ACTION_MOVE_TOUCHEVENT); 
                	mGestureDetector.onTouchEvent(ev);
                }
                else if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT_MOVED || mFragment.getSelectionMode() == SELECTION_EVENT_MOVED) {                	
                	//boolean moveSelectedEventRect = true;
                	// ��������...����...
                	// new event rec�� x�࿡ ���� �ǽð�? üũ�� �ؾ� �ϴµ�...
                	// ACTION_MOVE�� �������� ���� ���� �׷��ϰ�...
                	// mAutoHorizontalScrolling�� �����ؾ� �ϴ°�?...������
                	// mAutoVerticalScrolling�� �浹�� ����Ǵµ�...
                	
                	int x = (int) ev.getX();
                    int y = (int) ev.getY();                                             
                                        
                    synchronized(mAutoVerticalScrollingSync) {
	                    int deltaX = x - mFragment.getPrvNewOrSelectedEventMoveX();
	                    int deltaY = y - mFragment.getPrvNewOrSelectedEventMoveY();
	            		
	                    int newLeft = mFragment.getPrvNewOrSelectedEventLeft() + deltaX;
	            		mFragment.setPrvNewOrSelectedEventLeft(newLeft); 
	            		int newRight = mFragment.getPrvNewOrSelectedEventLeft() + (mViewWidth - mHoursWidth);
	    				mFragment.setPrvNewOrSelectedEventRight(newRight);    
	            		
	    				int newTop = mFragment.getPrvNewOrSelectedEventTop() + deltaY;
	    				mFragment.setPrvNewOrSelectedEventTop(newTop);   
	    				int newBottom = mFragment.getPrvNewOrSelectedEventBottom() + deltaY;
	            		mFragment.setPrvNewOrSelectedEventBottom(newBottom);
	            		
	            		// mAutoVerticalScrollingRunnable�� ������ ���� ���� �߿��ϴ�
	            		mFragment.setPrvNewOrSelectedEventMoveX(x);
	            		mFragment.setPrvNewOrSelectedEventMoveY(y);
	            		
	            		if (!mFragment.mSwitchingDayViewBySelectedEventRect) {
	            			int leftLimit = -(int)(mViewWidth * 0.2f);
	            			int rightLimit = mViewWidth + (int)(mViewWidth * 0.2f);
	            			
		            		if (mFragment.getPrvNewOrSelectedEventLeft() < leftLimit) {
		            			mAutoVerticalScrollingByNewEventRect = false;                                    
		        	    		mAutoVerticalScrollingType = 0;
		                		removeCallbacks(mAutoVerticalScrollingRunnable); 
		                		
		            			mTouchMode = TOUCH_MODE_INITIAL_STATE;
		            			mFragment.switchViews(false, true, 0, mViewWidth);//mFragment.switchViews(mBaseDate, false, true, 0, mViewWidth);
		            			mViewStartX = 0;	            			
		                		
		            			return true;
		            		}
		            		else if (mFragment.getPrvNewOrSelectedEventRight() > rightLimit) {
		            			mAutoVerticalScrollingByNewEventRect = false;                                    
		        	    		mAutoVerticalScrollingType = 0;
		                		removeCallbacks(mAutoVerticalScrollingRunnable); 
		                		
		            			mTouchMode = TOUCH_MODE_INITIAL_STATE;
		            			mFragment.switchViews(true, true, 0, mViewWidth);//mFragment.switchViews(mBaseDate, true, true, 0, mViewWidth);
		            			mViewStartX = 0;	            			
		            			
		            			return true;
		            		}
	            		}
                    }
                                        
                	
                	if (detectAutoVerticalScrolling())
                		return true;
                	
                	mFragment.getSelectedEventRect().setVisibility(View.VISIBLE);
                	
                	mFragment.getSelectedEventRect().setX(mFragment.getPrvNewOrSelectedEventLeft());    
                	if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT_MOVED) {
                		mFragment.getSelectedEventRect().setY(mFragment.getPrvNewOrSelectedEventTop());
                	}
                	else {
                		int newOutLineY = (int) (mFragment.getPrvNewOrSelectedEventTop() - mFragment.getSelectedEventRect().getTopMargin());
                    	mFragment.getSelectedEventRect().setY(newOutLineY);//mFragment.getSelectedEventRect().setY(y - (mCellHeight / 2));
                	}
                	
                    if (!mAutoVerticalScrollingByNewEventRect) {                    	
                    	setSelectionFromPosition(x, y, mFragment.getPrvNewOrSelectedEventTop());//setSelectionFromPosition(x, y);
                    	invalidate();
                    }
                    
                }
                else if (mFragment.getSelectionMode() == SELECTION_EVENT_EXPAND_SELECTION) {                	 
                	
                	detectExpandSelection(ev);
                }
                else if (mFragment.getSelectionMode() == SELECTION_EVENT_EXPANDING) {
                	if (INFO) Log.i(TAG, "ACTION_MOVE:SEE");    
                	
                	int x = (int) ev.getX();
                    int y = (int) ev.getY();   
                    
            		//int prvNewOrSelectedEventMoveY = mFragment.getPrvNewOrSelectedEventMoveY();
            		
                    synchronized(mAutoVerticalScrollingSync) {  	            		
	                    	            		
	            		if (mSelectedEventRectExpandingWhichCirclePoint == SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT) {                    	
	                    	                    	
	                    	// SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT�� ��쿡�� mPrvNewEventTop�� �����Ѵ�
	                    	mFragment.setPrvNewOrSelectedEventTop(y); // ���Ŀ� ACTION_UP�� �߻��Ͽ��� �� Selected Event Rect�� Top ������ ������ ���ȴ�
	                    	setSelectionFromPosition(x, y, mFragment.getPrvNewOrSelectedEventTop());    
	                    }
	                    else {                    	
	                    	
	                    	// SELECTION_EVENT_EXPANDING_LOWER_CIRCLEPOINT�� ��쿡�� mPrvNewEventTop�� �����ϸ� �ȵȴ�
	                    	// bottom�� ���� �����ϰų� �Ʒ��� �����ϱ� �����̴�
	                    	mFragment.setPrvNewOrSelectedEventBottom(y);
	                    	setSelectionBottomFromPosition(y);
	                    }
	            		
	            		//mFragment.setPrvNewOrSelectedEventMoveX(x);
	                    //mFragment.setPrvNewOrSelectedEventMoveY(y);	
	            		
                    }           		
                	
                    detectAutoVerticalScrolling();
                    
        	        
        	        invalidate();
            		
                }
                else 
                	mGestureDetector.onTouchEvent(ev);
                
                return true;

            case MotionEvent.ACTION_UP:
                if (INFO) Log.i(TAG, "ACTION_UP Cnt=" + ev.getPointerCount());
                
                mEdgeEffectTop.onRelease();
                mEdgeEffectBottom.onRelease();
                mStartingScroll = false;
                
                // -doSingleTapUp�� ó���� �� �ִ�
                mGestureDetector.onTouchEvent(ev); ///////////////////
                
                if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT) {
                	mFragment.resetSelectedEventRequireUpdateFlags();
                	mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_ACTION_UP_TOUCHEVENT);                	
                	
                    mViewStartX = 0;
                    mScrolling = false; // ACTION_MOVE�� SELECTION_NEW_EVENT if������ mGestureDetector.onTouchEvent�� ȣ���Ͽ���
                                        // onScroll�� ȣ��Ǿ��� ���̹Ƿ� mScrolling�� true�� �����Ǿ��� ���̶�� ������ �� ����
                    long extraLong = 0; 
                    
                    mController.sendEventRelatedEventWithExtra(this, EventType.CREATE_EVENT, -1,
                            getSelectedTimeInMillis(), 0, (int) ev.getRawX(), (int) ev.getRawY(),
                            extraLong, -1);                   
	                    
	                return true;
                    
                }
                else if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT_MOVED) {
                	if (mAutoVerticalScrollingByNewEventRect) {
                		mAutoVerticalScrollingByNewEventRect = false;
                		mAutoVerticalScrollingType = 0;
                		removeCallbacks(mAutoVerticalScrollingRunnable);                		
                	}
                	
                	mViewStartX = 0;
                    mScrolling = false; // ACTION_MOVE�� SELECTION_NEW_EVENT if������ mGestureDetector.onTouchEvent�� ȣ���Ͽ���
                                        // onScroll�� ȣ��Ǿ��� ���̹Ƿ� mScrolling�� true�� �����Ǿ��� ���̶�� ������ �� ����
                    
                    boolean mustReturnInPlaceAnim = setNewAndSelectedEventReturnInPlaceAnim();
                	if (mustReturnInPlaceAnim) {
                		// ���� ��Ȳ�� selected event�� �ð� ������ ���� ������ ������� �����ؾ� �Ѵ� 
                		mFragment.removeSelectedEventPanelTouchListener();    
                		mFragment.setSelectionMode(SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED, SELECTION_ACTION_UP_TOUCHEVENT);
                		setNewEventReturnInPlaceValueAnimator(ev.getRawX(), ev.getRawY());
                	}
                	else {                		
                		mFragment.resetSelectedEventRequireUpdateFlags();
                		mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_ACTION_UP_TOUCHEVENT);
                		long extraLong = 0; 
                        
                        mController.sendEventRelatedEventWithExtra(this, EventType.CREATE_EVENT, -1,
                                getSelectedTimeInMillis(), 0, (int) ev.getRawX(), (int) ev.getRawY(),
                                extraLong, -1);
                	}    
                    
                }
                else if (mFragment.getSelectionMode() == SELECTION_EVENT_PRESSED) {
                	if (INFO) Log.i(TAG, "UP : SELECTION_EVENT_PRESSED");
                	// 
                	// SELECTION_EVENT_PRESSED ���¶�� ���� Long Press ���Ŀ� �߻��� ���̹Ƿ� onSingleTapUp�� �߻��� �� ����
                	mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, SELECTION_ACTION_UP_TOUCHEVENT); 
                	
                	// �Ʒ� else if (mSelectionMode == SELECTION_EVENT_MOVED) ó��
                	// �۾��� �����ؾ� ���� ������?
                	// :�� �ʿ䰡 ����
                	//  SELECTION_EVENT_PRESSED���� ��ٷ� SELECTION_EVENT_FLOATING ���·� ��ȯ�ߴٴ� ����
                	//  �ƹ��� ACTION_MOVE�� �߻����� �ʾұ� ������...
                }
                else if (mFragment.getSelectionMode() == SELECTION_EVENT_MOVED) {                	
                	int startHourOfSelectedEvent = mFragment.getSelectedEvent().startTime / 60; // ���⼭ ���� ��Ÿ�� ���� null�� �߻���...��ġ ����
                	int startMinuteOfSelectedEvent = mFragment.getSelectedEvent().startTime % 60;
                    int endHourOfSelectedEvent = mFragment.getSelectedEvent().endTime / 60;                    
                    int endMinuteOfSelectedEvent = mFragment.getSelectedEvent().endTime % 60;                    
                	
                	int howFarMinutes = mFragment.getSelectedEvent().endTime - mFragment.getSelectedEvent().startTime;
                	long howFarMillis = (howFarMinutes * 60) * 1000;
                	
                	Calendar startTimeOfMovedSelectedEventRect = getSelectedDay();
                	Calendar endTimeOfMovedSelectedEventRect = getSelectedDay();                	
                	long startTimeMillisOfMovedSelectedEventRect = startTimeOfMovedSelectedEventRect.getTimeInMillis();
                	long endTimeMillisOfMovedSelectedEventRect = startTimeMillisOfMovedSelectedEventRect + howFarMillis;
                	endTimeOfMovedSelectedEventRect.setTimeInMillis(endTimeMillisOfMovedSelectedEventRect);   
                	
                	
                	if (startHourOfSelectedEvent != startTimeOfMovedSelectedEventRect.get(Calendar.HOUR_OF_DAY) || 
                			startMinuteOfSelectedEvent != startTimeOfMovedSelectedEventRect.get(Calendar.MINUTE) ||
                			endHourOfSelectedEvent != endTimeOfMovedSelectedEventRect.get(Calendar.HOUR_OF_DAY) ||
                			endMinuteOfSelectedEvent != endTimeOfMovedSelectedEventRect.get(Calendar.MINUTE)) {
                		
                		mFragment.mSelectedEventRequiredUpdate = true;               		
                	}
                	
                	// selected event�� update ���� �ʴ´ٰ� �ؼ�
                	// return in place�� ������ �ʿ䰡 ���� ���� �ƴϴ�                	
            		boolean mustReturnInPlaceAnim = setNewAndSelectedEventReturnInPlaceAnim();
                	if (mustReturnInPlaceAnim) {
                		// return in place�� �����Ѵٰ� �ؼ�
                		// selected event�� update �ؾ� �ϴ� �ð� ������ ����� ���� �ƴϴ�
                		// 
                		mFragment.removeSelectedEventPanelTouchListener();
                		mFragment.setSelectionMode(SELECTION_EVENT_ADJUST_POSITION_AFTER_MOVED, SELECTION_ACTION_UP_TOUCHEVENT);
                		setSelectedEventReturnInPlaceValueAnimator();               		
                	}
                	else {
                		// mustReturnInPlaceAnim�� false ��� ����,
                		// selected event�� move�� �߻��� �Ǿ�����,
                		// ���� ��� �ٽ� selected event�� ���� ��ġ�� ����?���� �� �ִ� ��Ȳ�� �߻��Ͽ��ٴ� ���� �ǹ�
                		// :�̴� ��ǻ� update�� return in place�� animation�� �ʿ� ���ٴ� ���� �ǹ��Ѵ�
                		if (!mFragment.mSelectedEventRequiredUpdate)
                			mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, SELECTION_ACTION_UP_TOUCHEVENT);
                		else {
                			// �� ��Ȳ�� �߻��� �� �ִ°�?
                			// return in place anim�� �� �ʿ䰡 ���µ�...
                			// selected event�� �ð� ������ ����Ǿ��ٴ� ��Ȳ�� ���̴�...
                			
                		}
                	}
                	
                	if (mFragment.mSelectedEventRequiredUpdate) {
            			if (!mFragment.getSelectedEvent().isRepeating) {    
                    		AsyncQueryService service = new AsyncQueryService(mActivity); 
                        	Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mFragment.getSelectedEvent().id);
                        	ContentValues values = new ContentValues();  
                        	
    	                	values.put(Events.DTSTART, startTimeOfMovedSelectedEventRect.getTimeInMillis());
    	                	values.put(Events.DURATION, (String) null);
    	                	values.put(Events.DTEND, endTimeOfMovedSelectedEventRect.getTimeInMillis());
    	                	
    	                	// java.lang.IllegalArgumentException: Cannot have both DTEND and DURATION in an event
    	                	// :�� ��Ȳ�� �ش� event�� rrule�� ������ �ִ� ���, �� �ſ� �ݺ��Ǵ� ���???
    	                	service.startUpdate(0, null, uri, values, null, null, 0); 
    	                	
    	                	// ���� �츮�� mSelectedEvent�� ����� ������ mSelectedEvent�� �����ؾ� �Ѵ�
    	                	// .../////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	                	// ;�ذ� ����� ���⼭�� �ƴ϶�...event changed �̺�Ʈ�� �߻����� �� �����ϴ� ���� ���?
    	                	//  mSelectedEvent�� id�� ������ event�� ������ ���̴�...
                    	}
            			else {
            				// mustReturnInPlaceAnim�� false ��� ����,
                    		// selected event�� move�� �߻��� �Ǿ�����,
                    		// ���� ��� �ٽ� selected event�� ���� ��ġ�� ����?���� �� �ִ� ��Ȳ�� �߻��Ͽ��ٴ� ���� �ǹ�
                    		// :�̴� ��ǻ� update�� return in place�� animation�� �ʿ� ���ٴ� ���� �ǹ��Ѵ�
            				if (!mustReturnInPlaceAnim) { // �� ��Ȳ�� �߻��� �� �ִ°�?..............................
            					mFragment.removeSelectedEventPanelTouchListener();
            					//mFragment.setSelectionMode(SELECTION_EVENT_TIME_MODIFICATION_DB_POPUP_AFTER_MOVED, SELECTION_ACTION_UP_TOUCHEVENT);
            					//mFragment.popUpEventTimeModificationOkDB(startTimeOfMovedSelectedEventRect.toMillis(true), endTimeOfMovedSelectedEventRect.toMillis(true));
            				}
            				else {
            					// �� ��Ȳ�� ó���� �ʿ䰡 ����
            					// mustReturnInPlaceAnim �� mSelectedEventRequiredUpdate�� true�̰� �׸��� isRepeating�� true���,,,
            					// mSelectedEventReturnInPlaceTranslateAnimator�� onAnimationEnd���� 
            					// ���� �۾����� �����ϱ� �����̴�
            				}
            			}            			
            		}       	
                	
                    	
                	if (mAutoVerticalScrollingByNewEventRect) {
                		mAutoVerticalScrollingByNewEventRect = false;
                		mAutoVerticalScrollingType = 0;
                		removeCallbacks(mAutoVerticalScrollingRunnable);
                	}
                    
                	//mHandleActionUp = true;
                    //mViewStartX = 0;
                    mScrolling = false;                    
                	                	
                }
                else if (mFragment.getSelectionMode() == DayView.SELECTION_EVENT_FLOATING) {
                	if (INFO) Log.i(TAG, "UP : SELECTION_EVENT_FLOATING");
                	//mScrolling = false;  // ���⼭ mScrolling = false;�� �����ϸ� floating ���¿��� ��ũ���� ���� ����...
                	                       // �׷��� ���⼭ mScrolling = false;�� �������� ������ floating ���¿��� singletapup�� ������ ����...�Ѥ�;
                	                       // doSingleTapup���� mScrolling�� true�̸� ��� �����ϱ� ������...
                	
            	}
                else if (mFragment.getSelectionMode() == DayView.SELECTION_EVENT_EXPANDING) {
                	if (mAutoVerticalScrollingByNewEventRect) {
                		mAutoVerticalScrollingByNewEventRect = false;
                		mAutoVerticalScrollingType = 0;
                		removeCallbacks(mAutoVerticalScrollingRunnable);                		
                	}

                	mViewStartX = 0;
                    mScrolling = false;          	
                	
                    
                	mFragment.getSelectedEventRect().setVisibility(View.VISIBLE);
                	
                	int cellHeight = getSelectedEventRectHeight();
                	cellHeight = mFragment.getSelectedEventRect().getTopMargin() + cellHeight + mFragment.getSelectedEventRect().getBottomMargin();
                	LinearLayout.LayoutParams selectedEventRectParam = (LinearLayout.LayoutParams) mFragment.getSelectedEventRect().getLayoutParams();	            	
	            	selectedEventRectParam.height = cellHeight;///////////////////////////////////////////////////////////
	            	mFragment.getSelectedEventRect().setLayoutParams(selectedEventRectParam);   	            	
	            	
	            	float selectedEventRectY = mFirstHourOffset + ((mSelectionStartHour - mFirstHour) * (mCellHeight + HOUR_GAP)) + mMinutuePosPixelAtSelectedEventMove;	            	
	            	int outLineY = (int) (selectedEventRectY - mFragment.getSelectedEventRect().getTopMargin());	            	
	            	mFragment.getSelectedEventRect().setY(outLineY);
	            	
	            	//invalidate(); // �� ������ �ִ�...SELECTION_EVENT_EXPANDING�� �����ؾ� �Ѵ�
	            	
	            	//mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, SELECTION_ACTION_UP_TOUCHEVENT);
	            	mFragment.removeSelectedEventPanelTouchListener();
	            	// �츮�� 15�� �������� ���ĵǵ��� �ִϸ��̼��� �����ؾ� �Ѵ�!!!
	            	setAdjustMinutesAnim();       	
                	
	            	mAdjustMinutesAfterExpandingCompletion.start();                	
                }
                else {
                	
                	//mGestureDetector.onTouchEvent(ev); ///////////////////
                	
                	if (mOnFlingCalled) {
                        return true;
                    }

                    // If we were scrolling, then reset the selected hour so that it
                    // is visible.
                	// *mScrolling�� true�� �����Ǵ� �κ�
                	// 1.doScroll
                	// 2.doFling
                	// 3.ContinueScroll.run
                    if (mScrolling) {
                    	// mScrolling�� ������ scrolling �� �϶�,
                    	// doSingleTapUp / doLongPress �� �߻��ϸ� ��ٷ� return ��Ű�� ����
                        mScrolling = false;
                        resetSelectedHour();
                        invalidate();
                    }

                    if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
                                                
                        if (Math.abs(mViewStartX) > mHorizontalSnapBackThreshold) {
                            // The user has gone beyond the threshold so switch views                     
                        	mTouchMode = TOUCH_MODE_INITIAL_STATE;
                        	mFragment.switchViews(mViewStartX > 0, false, mViewStartX, mViewWidth);//mFragment.switchViews(mBaseDate, mViewStartX > 0, false, mViewStartX, mViewWidth);
                            mViewStartX = 0;
                            return true;
                        } else {
                            // Not beyond the threshold so invalidate which will cause
                            // the view to snap back. Also call recalc() to ensure
                            // that we have the correct starting date and title.  	
                        	if (INFO) Log.i(TAG, "ACTION_UP:???");
                        	mFragment.makeInPlaceAnimation();
                        	//makeInPlaceAnim(1);
                    		
                        	// mDeltaOfReturnInPlaceTranslateAnimator�� 
                        	// onAnimationEnd���� mTouchMode = TOUCH_MODE_INITIAL_STATE;��
                        	// onAnimationUpdate���� mViewStartX = 0;
                        	// �� ������
                        	//mDeltaOfReturnInPlaceTranslateAnimator.start();                        	
                        }
                    }
                }                 

                return true;

                // This case isn't expected to happen.
            case MotionEvent.ACTION_CANCEL:
                //if (INFO) Log.i(TAG, "ACTION_CANCEL");
                mGestureDetector.onTouchEvent(ev);
                mScrolling = false;
                resetSelectedHour();
                return true;

            default:
                if (INFO) Log.i(TAG, "Not MotionEvent " + ev.toString());
                if (mGestureDetector.onTouchEvent(ev)) {
                    return true;
                }
                return super.onTouchEvent(ev);
        }
    }
    
    
    public boolean detectAutoVerticalScrolling() {
    	    	
    	int newEventRectTop = mFragment.getPrvNewOrSelectedEventTop() - mFirstCell; 
    	int newEventRectBottom = mFragment.getPrvNewOrSelectedEventBottom() - mFirstCell; //int newEventRectBottom = newEventRectTop + mFragment.getSelectedEventRect().getHeight(); 
    	int moveUppermostSlowAutoScrollingThreshHold = mFirstCell + (mCellHeight / 2);
    	int moveLowermostSlowAutoScrollingThreshHold = mViewHeight - GRID_LINE_BOTTOM_MARGIN;
    	
    	if (!mAutoVerticalScrollingByNewEventRect) {                  		
    		
    		// SELECTION_EVENT_MOVED�� ��쿡�� ���� �ʴ�
            //int selectedTop = y - (mCellHeight / 2);   
            //int adjustedSelectedTop = selectedTop - mFirstCell; 
    		// ���� mFirstCell�� 0�� ���� �����ϰ� �ִ�.....................................................................
    		//newEventRectTop = mFragment.getPrvNewOrSelectedEventTop() - mFirstCell;             
            //newEventRectBottom = newEventRectTop + mCellHeight;             
                                    
    		if (newEventRectTop < moveUppermostSlowAutoScrollingThreshHold) {             			
    			
    			if (mViewStartY == 0 && newEventRectTop < (mFirstCell + GRID_LINE_TOP_MARGIN + HOUR_GAP)) {
            		// ������ return ���� ���� new event rectangle�� ��ġ�� am 12:00�� ����� ���� ������?
            		if (INFO) Log.i(TAG, "detectAutoVerticalScrollingByMoving:return 1");
            		return true;
            	}
            	else {
            		mAutoVerticalScrollingByNewEventRect = true;
            		int moveUppermostFastAutoScrollingThreshHold = mFirstCell + GRID_LINE_TOP_MARGIN + HOUR_GAP;
            		if (newEventRectBottom < moveUppermostFastAutoScrollingThreshHold) { 
            			if (INFO) Log.i(TAG, "detectAutoVerticalScrollingByMoving:AUTO_VERTICAL_FAST_UPPERMOST_SCROLLING_TYPE");
            			mAutoVerticalScrollingType = AUTO_VERTICAL_FAST_UPPERMOST_SCROLLING_TYPE;
            			postOnAnimation(mAutoVerticalScrollingRunnable);
            		}
            		else {
            			if (INFO) Log.i(TAG, "detectAutoVerticalScrollingByMoving:AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE");
            			mAutoVerticalScrollingType = AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE;
            			postOnAnimation(mAutoVerticalScrollingRunnable);
            		}                        		
            	}                    	
            }                        
    		// ������ �߻��Ͽ���...    		
    		// GridArea���� Selected Event Rect�� bottom�� moveLowermostSlowAutoScrollingThreshHold���� �Ʒ��� ��ġ�� �ִ� ��Ȳ����,
    		// expanding ��Ȳ���� ������ upper expanding circle�� ��� �ø��� �����ϴµ�
    		// �Ʒ� else if ���� ���ؼ� AUTO_VERTICAL_FAST_LOWERMOST_SCROLLING_TYPE�� �߻��Ѵٴ� ���̴�...
    		else if (newEventRectBottom > moveLowermostSlowAutoScrollingThreshHold) {
            	
    			// pm 11:45���� cover �ؾ� �Ѵ�
            	if (mViewStartY == mMaxViewStartY && newEventRectTop > (mGridAreaHeight - ((mCellHeight / 4) + GRID_LINE_BOTTOM_MARGIN))) {
            		if (INFO) Log.i(TAG, "onTouchEvent:return 2");
            		return true;
            	}
            	else {
            		mAutoVerticalScrollingByNewEventRect = true;
            		
            		int moveLowermostFastAutoScrollingThreshHold = mViewHeight;
            		if (newEventRectBottom >= moveLowermostFastAutoScrollingThreshHold) {       
            			mAutoVerticalScrollingType = AUTO_VERTICAL_FAST_LOWERMOST_SCROLLING_TYPE;
            			postOnAnimation(mAutoVerticalScrollingRunnable);
            		}
            		else {
            			mAutoVerticalScrollingType = AUTO_VERTICAL_SLOW_LOWERMOST_SCROLLING_TYPE;
            			postOnAnimation(mAutoVerticalScrollingRunnable);
            		}                         		
            	}                        	
            }
    	}
    	else {
    		synchronized(mAutoVerticalScrollingSync) {
    			
    			//newEventRectTop = mFragment.getPrvNewOrSelectedEventTop() - mFirstCell;                
                //newEventRectBottom = newEventRectTop + mCellHeight;                             
                
    			if (mViewStartY == 0 && newEventRectTop < (mFirstCell + GRID_LINE_TOP_MARGIN + HOUR_GAP)) {
                	//if (mViewStartY == 0) {
                	// ������ return ���� ���� new event rectangle�� ��ġ�� am 12:00�� ����� ���� ������?
    				mAutoVerticalScrollingByNewEventRect = false;                                    
    	    		mAutoVerticalScrollingType = 0;
            		removeCallbacks(mAutoVerticalScrollingRunnable); 
                	if (INFO) Log.i(TAG, "onTouchEvent:return 3");
                	return true;
                }
    			else if (mViewStartY == mMaxViewStartY && newEventRectTop > (mGridAreaHeight - ((mCellHeight / 4) + GRID_LINE_BOTTOM_MARGIN))) {
    				mAutoVerticalScrollingByNewEventRect = false;                                    
    	    		mAutoVerticalScrollingType = 0;
            		removeCallbacks(mAutoVerticalScrollingRunnable); 
            		if (INFO) Log.i(TAG, "onTouchEvent:return 4");
            		return true;
            	}
        		                      
        		processUnderAutoVerticalScrolling(newEventRectTop, newEventRectBottom, moveUppermostSlowAutoScrollingThreshHold, moveLowermostSlowAutoScrollingThreshHold);  	                		
    		}                		
    	}  
    	
    	return false;
    }    
    
    public void processUnderAutoVerticalScrolling(int newEventRectTop, int newEventRectBottom, int moveUppermostSlowAutoScrollingThreshHold, int moveLowermostSlowAutoScrollingThreshHold) {
    	
    	if (mAutoVerticalScrollingType == AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE) {  
    		
    		int moveUppermostFastAutoScrollingThreshHold = mFirstCell + GRID_LINE_TOP_MARGIN + HOUR_GAP;
    		
	    	if (newEventRectTop > moveUppermostSlowAutoScrollingThreshHold) {   // SLOW UPPER MOST Auto Scrolling�� �ߴ��ؾ� �Ѵ�	  		
	    		mAutoVerticalScrollingByNewEventRect = false;                                    
	    		mAutoVerticalScrollingType = 0;
        		removeCallbacks(mAutoVerticalScrollingRunnable);  
        		if (INFO) Log.i(TAG, "************************************************************");        		
        		if (INFO) Log.i(TAG, "newEventRectBottom > moveUppermostSlowAutoScrollingThreshHold");
        		if (INFO) Log.i(TAG, "newEventRectBottom=" + String.valueOf(newEventRectBottom));
        		if (INFO) Log.i(TAG, "moveUppermostSlowAutoScrollingThreshHold=" + String.valueOf(moveUppermostSlowAutoScrollingThreshHold));
        		if (INFO) Log.i(TAG, "************************************************************");
	        }
	    	else if (newEventRectTop < moveUppermostFastAutoScrollingThreshHold) { // FAST UPPER MOST Auto Scrolling�� ��ȯ�Ǿ�� �Ѵ�
	    		if (INFO) Log.i(TAG, "newEventRectBottom <= moveUppermostFastAutoScrollingThreshHold");
	    		mAutoVerticalScrollingType = AUTO_VERTICAL_FAST_UPPERMOST_SCROLLING_TYPE; 
    			removeCallbacks(mAutoVerticalScrollingRunnable);  
    			postOnAnimation(mAutoVerticalScrollingRunnable);
	    	}
	    	else if (newEventRectTop == mFirstCell) {
	    		mAutoVerticalScrollingByNewEventRect = false;                                    
	    		mAutoVerticalScrollingType = 0;
        		removeCallbacks(mAutoVerticalScrollingRunnable); 
	    	}
	    	else {
	    		if (INFO) Log.i(TAG, "************************************************************");
	    		if (INFO) Log.i(TAG, "processUnderAutoVerticalScrolling:AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE");
	    		if (INFO) Log.i(TAG, "newEventRectBottom=" + String.valueOf(newEventRectBottom));
        		if (INFO) Log.i(TAG, "moveUppermostSlowAutoScrollingThreshHold=" + String.valueOf(moveUppermostSlowAutoScrollingThreshHold));
        		if (INFO) Log.i(TAG, "************************************************************");
	    	}
    	}
    	else if (mAutoVerticalScrollingType == AUTO_VERTICAL_FAST_UPPERMOST_SCROLLING_TYPE) {  
    		int moveUppermostFastAutoScrollingThreshHold = mFirstCell + GRID_LINE_TOP_MARGIN + HOUR_GAP;;
    		if (newEventRectTop > moveUppermostFastAutoScrollingThreshHold) { // SLOW UPPER MOST Auto Scrolling�� ��ȯ�Ǿ�� �Ѵ�  	
    			if (newEventRectTop < moveUppermostSlowAutoScrollingThreshHold) {
    				mAutoVerticalScrollingType = AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE;
        			removeCallbacks(mAutoVerticalScrollingRunnable);  
        			postOnAnimation(mAutoVerticalScrollingRunnable);    
    			}
    			else if (newEventRectTop > moveUppermostSlowAutoScrollingThreshHold) {
    				mAutoVerticalScrollingByNewEventRect = false;
    	    		mAutoVerticalScrollingType = 0;
            		removeCallbacks(mAutoVerticalScrollingRunnable);      
    			}    			    		
    		}
    	}
    	else if (mAutoVerticalScrollingType == AUTO_VERTICAL_SLOW_LOWERMOST_SCROLLING_TYPE) {
    		int moveLowermostFastAutoScrollingThreshHold = mViewHeight;
	        if (newEventRectBottom < moveLowermostSlowAutoScrollingThreshHold) { //SLOW LOWER MOST Auto Scrolling�� �ߴ��ؾ� �Ѵ�
	        	mAutoVerticalScrollingByNewEventRect = false;
	        	mAutoVerticalScrollingType = 0;
        		removeCallbacks(mAutoVerticalScrollingRunnable);         		
	        }
	        else if (newEventRectBottom >= moveLowermostFastAutoScrollingThreshHold) { // FAST LOWER MOST Auto Scrolling�� ��ȯ�Ǿ�� �Ѵ�
	        	mAutoVerticalScrollingType = AUTO_VERTICAL_FAST_LOWERMOST_SCROLLING_TYPE;
    			removeCallbacks(mAutoVerticalScrollingRunnable);
    			postOnAnimation(mAutoVerticalScrollingRunnable); 
	        }
    	}
    	else if (mAutoVerticalScrollingType == AUTO_VERTICAL_FAST_LOWERMOST_SCROLLING_TYPE) {    	
    		
    		int moveLowermostFastAutoScrollingThreshHold = mViewHeight;
    		if (newEventRectBottom < moveLowermostFastAutoScrollingThreshHold) { // SLOW UPPER MOST Auto Scrolling�� ��ȯ�Ǿ�� �Ѵ�  	
    			if (newEventRectBottom < moveLowermostSlowAutoScrollingThreshHold) {
    				mAutoVerticalScrollingByNewEventRect = false;
    	        	mAutoVerticalScrollingType = 0;
            		removeCallbacks(mAutoVerticalScrollingRunnable);  
    			}
    			else if (newEventRectBottom >= moveUppermostSlowAutoScrollingThreshHold) {
    				mAutoVerticalScrollingType = AUTO_VERTICAL_SLOW_LOWERMOST_SCROLLING_TYPE;
        			removeCallbacks(mAutoVerticalScrollingRunnable);
        			postOnAnimation(mAutoVerticalScrollingRunnable);        	 
    			}    			    		
    		}
    	}
    }
    
    
    
    int mAutoVerticalScrollingType = 0;
    public static final int AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE = 1;
    public static final int AUTO_VERTICAL_FAST_UPPERMOST_SCROLLING_TYPE = 2;
    public static final int AUTO_VERTICAL_SLOW_LOWERMOST_SCROLLING_TYPE = 3;
    public static final int AUTO_VERTICAL_FAST_LOWERMOST_SCROLLING_TYPE = 4;
    
    Runnable mAutoVerticalScrollingRunnable = new Runnable() {

		@Override
		public void run() {
			synchronized(mAutoVerticalScrollingSync) {
	            int deltaY = 0;
	            
				if (mAutoVerticalScrollingType == AUTO_VERTICAL_SLOW_UPPERMOST_SCROLLING_TYPE) {				
					deltaY = -5;
				}
				else if (mAutoVerticalScrollingType == AUTO_VERTICAL_FAST_UPPERMOST_SCROLLING_TYPE) {				
					deltaY = -15;
				}
				else if (mAutoVerticalScrollingType == AUTO_VERTICAL_SLOW_LOWERMOST_SCROLLING_TYPE) {				
					deltaY = 5;
				}
				else if (mAutoVerticalScrollingType == AUTO_VERTICAL_FAST_LOWERMOST_SCROLLING_TYPE) {				
					deltaY = 15;
				}
				else {
					if (INFO) Log.i(TAG, "mAutoVerticalScrollingRunnable : ELSE RETURN");
					return;
				}			
				
	            mViewStartY = mViewStartY + deltaY;
	            
	            // If dragging while already at the end, do a glow
	            final int pulledToY = (int) (mScrollStartY + deltaY);
	            if (pulledToY < 0) {
	                mEdgeEffectTop.onPull(deltaY / mViewHeight);
	                if (!mEdgeEffectBottom.isFinished()) {
	                    mEdgeEffectBottom.onRelease();
	                }
	            } else if (pulledToY > mMaxViewStartY) {
	                mEdgeEffectBottom.onPull(deltaY / mViewHeight);
	                if (!mEdgeEffectTop.isFinished()) {
	                    mEdgeEffectTop.onRelease();
	                }
	            }
	
	            
	            if (mViewStartY < 0) {
	                mViewStartY = 0;       
	                mAutoVerticalScrollingType = 0;
	                mAutoVerticalScrollingByNewEventRect = false;
	            } else if (mViewStartY > mMaxViewStartY) {
	                mViewStartY = mMaxViewStartY;   
	                mAutoVerticalScrollingType = 0;
	                mAutoVerticalScrollingByNewEventRect = false;
	            }	                
	            
	            computeFirstHour();
	            
	            	            
	            if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT_MOVED || mFragment.getSelectionMode() == SELECTION_EVENT_MOVED) {
	            	setSelectionFromPosition(mFragment.getPrvNewOrSelectedEventMoveX(), mFragment.getPrvNewOrSelectedEventMoveY(), mFragment.getPrvNewOrSelectedEventTop());
	            }
	            else if (mFragment.getSelectionMode() == SELECTION_EVENT_EXPANDING) {
            		if (mSelectedEventRectExpandingWhichCirclePoint == SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT) {  
            			setSelectionFromPosition(mFragment.getPrvNewOrSelectedEventMoveX(), mFragment.getPrvNewOrSelectedEventMoveY(), mFragment.getPrvNewOrSelectedEventTop());
            		}
            		else {            			
                		setSelectionBottomFromPosition(mFragment.getPrvNewOrSelectedEventBottom());
            		}	            		
	            }     
	            	                        
	            mScrolling = true;/////////////////////////////////////////////////////
	
	            DayView.this.invalidate();
	            
	            if (mAutoVerticalScrollingByNewEventRect)	            	
	            	DayView.this.postOnAnimation(this);            
			}			
		}    	
    };
    
    /*
    public boolean makeAnotherNewEvent(EventCursors targetObj, long startMillisOfNewEvent, long endMillisOfNewEvent) {   	
    	int calendarAccessLevel = 0;
    	
    	if (targetObj.mCalendarsCursor.moveToFirst()) {
    		calendarAccessLevel = targetObj.mCalendarsCursor.getInt(EventInfoLoader.CALENDARS_INDEX_ACCESS_LEVEL);
    	}
    	else 
    		return false;
    	
    	String eventOwnerAccount = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_OWNER_ACCOUNT);
    	String eventOrganizer = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_ORGANIZER);
    	boolean isOrganizer = eventOwnerAccount.equalsIgnoreCase(eventOrganizer);
    	boolean guestsCanModify = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_GUESTS_CAN_MODIFY) != 0;
    	
    	if (calendarAccessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR && (isOrganizer || guestsCanModify)) {
    		AsyncQueryService service = new AsyncQueryService(mActivity); 
    		
    		ContentValues eventValues = new ContentValues();
    		
    		long idOfExistingEvent = targetObj.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_ID);   		
    		long startMillisOfExistingEvent = targetObj.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_DTSTART);
    		long endMillisOfExistingEvent = targetObj.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_DTEND);
    		String durationMillisOfExistingEvent = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_DURATION);
    		long lastDateMillis = targetObj.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_LAST_DATE);
    		//String rruleOfExistingEvent = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_RRULE);
    		
    		long durationMillis = 0; // duration�� �̺�Ʈ�� ���� �ð����� rrule�� �Ϸ����� �ƴϴ�!!!
    		if (!TextUtils.isEmpty(durationMillisOfExistingEvent)) {
                try {
                    Duration d = new Duration();
                    d.parse(durationMillisOfExistingEvent);
                    durationMillis = d.getMillis();
                    
                } catch (DateException e) {
                    Log.d(TAG, "Error parsing duration string " + durationMillisOfExistingEvent, e);
                }
            }    		
    		
    		long nextRecurEventSearchMillis = 0;
    		if (endMillisOfExistingEvent != 0) {
    			nextRecurEventSearchMillis = endMillisOfExistingEvent;
    		}
    		else {
    			nextRecurEventSearchMillis = startMillisOfExistingEvent + durationMillis;
    		}
    		
    		nextRecurEventSearchMillis = nextRecurEventSearchMillis + 1000; // 1�ʸ� �� ���Ѵ�
    		long nextFirstRecurTimeMillis = getNextFirstRecurTimeMillis(idOfExistingEvent, nextRecurEventSearchMillis, lastDateMillis);    		
    		
        	long calendarId = targetObj.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_CALENDAR_ID);
        	String eventTimezone = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_TIMEZONE);
        	String eventName = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_TITLE);
        	int isAllDay = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ALL_DAY);
        	String description = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_DESCRIPTION);
        	String location = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_LOCATION);
        	int availability = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_AVAILABILITY);
        	int hasAttendeeData = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_HAS_ATTENDEE_DATA);
        	int accessLevel = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ACCESS_LEVEL);
        	int eventStatus = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_STATUS);
        	//int calendarColor = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_CALENDAR_COLOR); // �ָ� �����ϸ� ������ ���� ��Ÿ�� ������ �߻���
        	                                                                                                 // java.lang.IllegalArgumentException: Only the provider may write to calendar_color
        	int eventColor = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_EVENT_COLOR);
        	  	
        	String  eventColorKey = targetObj.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_COLOR_KEY); 	
        	
        	int hasAlarm = targetObj.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_HAS_ALARM);
        	
        	eventValues.put(Events.CALENDAR_ID, calendarId);
        	eventValues.put(Events.EVENT_TIMEZONE, eventTimezone);
        	eventValues.put(Events.TITLE, eventName);
        	eventValues.put(Events.ALL_DAY, isAllDay);
        	eventValues.put(Events.DTSTART, startMillisOfNewEvent);        	
        	eventValues.put(Events.DURATION, (String) null);
        	eventValues.put(Events.DTEND, endMillisOfNewEvent);
        	
        	if (description != null) {
        		eventValues.put(Events.DESCRIPTION, description.trim());
        	}
        	else {
        		eventValues.put(Events.DESCRIPTION, (String) null);
        	}
        	 
        	if (location != null) {
        		eventValues.put(Events.EVENT_LOCATION, location.trim());
            } else {
            	eventValues.put(Events.EVENT_LOCATION, (String) null);
            }
        	
        	eventValues.put(Events.AVAILABILITY, availability);
        	eventValues.put(Events.HAS_ATTENDEE_DATA, hasAttendeeData);
        	eventValues.put(Events.ACCESS_LEVEL, accessLevel); // EventCursors�� ����ϴ� EventInfoLoader�� EVENT_PROJECTION������ Events.ACCESS_LEVEL�� �������� ���� ��ſ� Calendars.CALENDAR_ACCESS_LEVEL�� �����ϰ� ����
        	                                              // :Events.ACCESS_LEVEL�� �����Ͽ���
        	eventValues.put(Events.STATUS, eventStatus);
        	
        	eventValues.put(Events.EVENT_COLOR, eventColor);
        	//eventValues.put(Events.CALENDAR_COLOR, calendarColor);    	
        	
        	// �ش� �̺�Ʈ�� event color�� event�� Ķ���� �÷� ������ �ʿ���
        	// Events.EVENT_COLOR_KEY
        	// :A secondary color key for the individual event. 
        	//  NULL or an empty string are reserved for indicating that the event does not use a key for looking up the color.
        	//  The provider will update EVENT_COLOR automatically when a valid key is written to this column. 
        	//  The key must reference an existing row of the Colors table    	
            eventValues.put(Events.EVENT_COLOR_KEY, eventColorKey);
            
            
            eventValues.put(Events.HAS_ALARM, hasAlarm);       
            
            eventValues.put(Events.HAS_ATTENDEE_DATA, 1);
            eventValues.put(Events.STATUS, Events.STATUS_CONFIRMED);
            
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            int eventIdIndex = -1;
            eventIdIndex = ops.size();
            ContentProviderOperation.Builder eventBuilder = ContentProviderOperation.newInsert(
                    Events.CONTENT_URI).withValues(eventValues);
            
            ops.add(eventBuilder.build());
                        
            if (hasAlarm == 1) {
                if (targetObj.mRemindersCursor.moveToFirst()) {
                    
                	ContentProviderOperation.Builder remindersBuilder = ContentProviderOperation
                            .newDelete(Reminders.CONTENT_URI);
                	remindersBuilder.withSelection(Reminders.EVENT_ID + "=?", new String[1]);
                	remindersBuilder.withSelectionBackReference(0, eventIdIndex);
                    ops.add(remindersBuilder.build());
                    
                    ContentValues remindersValues = new ContentValues();
                    
                    do
                    {
                    	remindersValues.clear();            	
                    	
                    	remindersValues.put(Reminders.MINUTES, targetObj.mRemindersCursor.getInt(EventInfoLoader.REMINDERS_MINUTES_ID));
                    	remindersValues.put(Reminders.METHOD, targetObj.mRemindersCursor.getInt(EventInfoLoader.REMINDERS_METHOD_ID));
                    	
                    	remindersBuilder = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(remindersValues);
                    	remindersBuilder.withValueBackReference(Reminders.EVENT_ID, eventIdIndex);
                        ops.add(remindersBuilder.build());
                    } while (targetObj.mRemindersCursor.moveToNext());
                    
                }
            }
            
            if (hasAttendeeData == 1) {           	
            	
            	ContentProviderOperation.Builder attendeeBuilder;
            	ContentValues attendeeValues = new ContentValues();
            	
            	// ���� ó���� Organizer�� �����ؾ� �Ѵ�
            	attendeeValues.put(Attendees.ATTENDEE_EMAIL, eventOwnerAccount);
            	attendeeValues.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ORGANIZER);
            	attendeeValues.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
            	attendeeValues.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_ACCEPTED);
           
            	attendeeBuilder = ContentProviderOperation.newInsert(Attendees.CONTENT_URI)
                            .withValues(attendeeValues);
            	attendeeBuilder.withValueBackReference(Attendees.EVENT_ID, eventIdIndex);
            	
            	ops.add(attendeeBuilder.build());
            	
    	        if (targetObj.mAttendeesCursor.moveToFirst()) {    	        	
    	        	
    		        do 
    		        {	
    		        	String attendeeName = targetObj.mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_NAME);
    		        	String attendeeEmail = targetObj.mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_EMAIL);
    		        	if (!attendeeEmail.equalsIgnoreCase(eventOwnerAccount)) {
	    		        	attendeeValues.clear();//////////////////////////////////////////////////////////////////////////////////////////////
	    		        	attendeeValues.put(Attendees.ATTENDEE_NAME, attendeeName);
	    		        	attendeeValues.put(Attendees.ATTENDEE_EMAIL, attendeeEmail);
	    		        	attendeeValues.put(Attendees.ATTENDEE_RELATIONSHIP,
	                                Attendees.RELATIONSHIP_ATTENDEE);
	    		        	attendeeValues.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
	    		        	attendeeValues.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);
	                         
	    		        	attendeeBuilder = ContentProviderOperation.newInsert(Attendees.CONTENT_URI)
	    		        		 .withValues(attendeeValues);
	    		        	attendeeBuilder.withValueBackReference(Attendees.EVENT_ID, eventIdIndex);	
	    		        	
	    		        	ops.add(attendeeBuilder.build());
    		        	}
    		        	
    		            
    		        }while (targetObj.mAttendeesCursor.moveToNext());
    	        }
            }
            
            service.startBatch(service.getNextToken(), null, android.provider.CalendarContract.AUTHORITY, ops,
                    Utils.UNDO_DELAY);
            
            // ���� �̺�Ʈ�� �ð��븦 �����ؾ� �Ѵ�            
            Uri uriOfUpdateEvent = ContentUris.withAppendedId(Events.CONTENT_URI, mFragment.getSelectedEvent().id);
        	ContentValues valuesOfUpdateEvent = new ContentValues();        	
        	valuesOfUpdateEvent.put(Events.DTSTART, nextFirstRecurTimeMillis);                  
        	valuesOfUpdateEvent.put(Events.DURATION, durationMillisOfExistingEvent);
        	valuesOfUpdateEvent.put(Events.DTEND, (Long) null);
        	
        	service.startUpdate(0, null, uriOfUpdateEvent, valuesOfUpdateEvent, null, null, 0);          
            
            return true;
    	}
    	else {
    		// ������ �� ����...
    		return false;
    	}       
    }
    */
    
    
    
    /*
    public int mDeltaOfReturnInPlace;
    ValueAnimator mDeltaOfReturnInPlaceTranslateAnimator;
    public void makeInPlaceAnim(int who) {
    	if (who == 1) {
    		if (INFO) Log.i(TAG, "makeInPlaceAnim:ACTION_UP");
    	}
    	else if (who == 2) {
    		if (INFO) Log.i(TAG, "makeInPlaceAnim:FLING");
    	}
    	else {
    		if (INFO) Log.i(TAG, "makeInPlaceAnim:who???");
    	}
    	
    	mTouchMode = TOUCH_MODE_RETURN_IN_PLACE;
    	
    	mDeltaOfReturnInPlace = mViewStartX;
    	mViewStartX = 0;
    	
    	mDeltaOfReturnInPlaceTranslateAnimator = ValueAnimator.ofInt(mDeltaOfReturnInPlace, 0);
    	long duration = calculateDuration(Math.abs(mDeltaOfReturnInPlace), mViewWidth, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
    	mDeltaOfReturnInPlaceTranslateAnimator.setDuration(duration);
    	mDeltaOfReturnInPlaceTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				mViewStartX = (Integer) valueAnimator.getAnimatedValue();	
				if (INFO) Log.i(TAG, "mDeltaOfReturnInPlaceTranslateAnimator:oAU");
				//initNextView(-mViewStartX); 
				                				
				invalidate();                				
			}
		});
		
    	mDeltaOfReturnInPlaceTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {			
				mTouchMode = TOUCH_MODE_INITIAL_STATE;
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
    */

    
    

    private static int getEventAccessLevel(Context context, Event e) {
        ContentResolver cr = context.getContentResolver();

        int accessLevel = Calendars.CAL_ACCESS_NONE;

        // Get the calendar id for this event
        Cursor cursor = cr.query(ContentUris.withAppendedId(Events.CONTENT_URI, e.id),
                new String[] { Events.CALENDAR_ID },
                null /* selection */,
                null /* selectionArgs */,
                null /* sort */);

        if (cursor == null) {
            return ACCESS_LEVEL_NONE;
        }

        if (cursor.getCount() == 0) {
            cursor.close();
            return ACCESS_LEVEL_NONE;
        }

        cursor.moveToFirst();
        long calId = cursor.getLong(0);
        cursor.close();

        Uri uri = Calendars.CONTENT_URI;
        String where = String.format(CALENDARS_WHERE, calId);
        cursor = cr.query(uri, CALENDARS_PROJECTION, where, null, null);

        String calendarOwnerAccount = null;
        if (cursor != null) {
            cursor.moveToFirst();
            accessLevel = cursor.getInt(CALENDARS_INDEX_ACCESS_LEVEL);
            calendarOwnerAccount = cursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
            cursor.close();
        }

        if (accessLevel < Calendars.CAL_ACCESS_CONTRIBUTOR) {
            return ACCESS_LEVEL_NONE;
        }

        if (e.guestsCanModify) {
            return ACCESS_LEVEL_EDIT;
        }

        if (!TextUtils.isEmpty(calendarOwnerAccount)
                && calendarOwnerAccount.equalsIgnoreCase(e.organizer)) {
            return ACCESS_LEVEL_EDIT;
        }

        return ACCESS_LEVEL_DELETE;
    }

    
    int mMinutuePosPixelAtSelectedEventMove;

    
    private boolean setSelectionFromPosition(int x, final int y, int selectedTop) {     
        
        if (x < mHoursWidth) {
            x = mHoursWidth;
        }

        // weekly ������ �� �ǹ� �ִ� �ڵ���
        // one day ���������� ���ʿ��� �ڵ�
        int day = (x - mHoursWidth) / (mCellWidth + DAY_GAP);
        if (day >= mNumDays) {
            day = mNumDays - 1;
        }
        day += mFirstJulianDay;
        // one day ���������� �ǹ̰� ������ weekly ���������� �ǹ̰� �ִ�
        setSelectedDay(day);///////////////////////////////////////////////////////////////////       
        
        //
        // ���� �� dayview������ all day event���� ó������ �����Ƿ� if ������ �����ؾ� �Ѵ�
        if (y < mFirstCell && mFragment.getSelectionMode() == SELECTION_HIDDEN) {
        	
            //mSelectionAllday = true;      ///////////////////////////////////////      
        }         
        else /*if (mSelectionMode == SELECTION_HIDDEN || mSelectionMode == SELECTION_NEW_EVENT_MOVED || mSelectionMode == SELECTION_EVENT_MOVED)*/ {
        	        	
        	// y is now offset from top of the scrollable region
        	// selecteTop ������ ����ϴ� ������
            // y ������ new event rec top�� �����ϸ� ������ ü���ϴ� �� ���� �� �ؿ� new event rec�� ��ġ�ϰ� �ȴ�
            // �׷��� �÷� �ش�
        	//int selecteTop = y - (mCellHeight / 2);  // 
            int adjustedY = selectedTop - mFirstCell; //mFirstCell�� 0�� �� �� all day header?�� ���� ��쿡�� �ǹ̰� ����
            
            // mFirstHourOffset�� �ǹ̴� ���� scroll view���� �������� ù��° hour[mFirstHour]��
            // top���� ���� �󸶳� ������ �ֳĴ� ���̴�            
            if (adjustedY < mFirstHourOffset) { // mFirstHour���� ��ġ������ ����  �ִ� �׷��ϱ� 1�ð� �� hour �ð��뿡 y�� �����ٴ� ���� �ǹ�
            	int unvisibleMinutuesPixel = mCellHeight - mFirstHourOffset;                
                float minutuePosPixel = unvisibleMinutuesPixel + adjustedY;
                mMinutuePosPixelAtSelectedEventMove = (int) minutuePosPixel;                
                
            	int newSelectionHour = mFirstHour/*mSelectionStartHour*/ - 1;            	
            	
            	if (newSelectionHour < 0) { // new event rec�� am12�� ������ �ʰ��ϴ� ���� ������ 
            		newSelectionHour = 0;
            		mMinutuePosPixelAtSelectedEventMove = 0;
            	}
            	else if (newSelectionHour == 0 && mMinutuePosPixelAtSelectedEventMove < 0) { // new event rec�� am12�� �ʰ��ϴ� ���� ������ 
            		mMinutuePosPixelAtSelectedEventMove = 0;            		
            	}
                setSelectedHour(newSelectionHour); /* In the partially visible hour */        
                
                int xxxMinutes = mMinutuePosPixelAtSelectedEventMove / mOneQuartersOfHourPixelHeight;
                int interpolationMinutes = 0;
                switch(xxxMinutes) {
                case 0:
                	interpolationMinutes = 0;
                	break;
                case 1:
                	interpolationMinutes = 15;
                	break;
                case 2:
                	interpolationMinutes = 30;
                	break;
                case 3:
                	interpolationMinutes = 45;
                	break;
                }                
                setSelectedMinutes(interpolationMinutes);
                
            } else {            	           	
            	
            	// adjustedY�� mFirstHourOffset�� ���ų� ũ�ٶ�� ���� mFirstHour line�� ������ �Ʒ��� �ִ� hour �ð��뿡 y�� �����ٴ� ���� �ǹ�
            	// �׷��Ƿ� �翬�� adjustedY���� mFirstHourOffset�� ������ ���� one hour height�� ������ 
            	// mFirstHour���� �Ʒ� ��� �ð��뿡 y�� �������� �� �� ����
            	// :�̹� ������ mSelectionStartHour�� mFirstHour�� ������            	
                mMinutuePosPixelAtSelectedEventMove = (adjustedY - mFirstHourOffset) % (mCellHeight + HOUR_GAP); 
                
            	int addHour = (adjustedY - mFirstHourOffset) / (mCellHeight + HOUR_GAP);            	
            	int newSelectionStartHour = mFirstHour/*mSelectionStartHour*/ + addHour;
            	
            	if (newSelectionStartHour > 23) {
            		newSelectionStartHour = 23;
            		mMinutuePosPixelAtSelectedEventMove = mThreeQuartersOfHour; // pm11:45�� �����
            	}
            	else if (newSelectionStartHour == 23 && mMinutuePosPixelAtSelectedEventMove > mThreeQuartersOfHour) {
            		mMinutuePosPixelAtSelectedEventMove = mThreeQuartersOfHour; // pm11:45�� �����
            	}
                setSelectedHour(newSelectionStartHour);                
                
                int xxxMinutes = mMinutuePosPixelAtSelectedEventMove / mOneQuartersOfHourPixelHeight;
                int interpolationMinutes = 0;
                switch(xxxMinutes) {
                case 0:
                	interpolationMinutes = 0;
                	break;
                case 1:
                	interpolationMinutes = 15;
                	break;
                case 2:
                	interpolationMinutes = 30;
                	break;
                case 3:
                	interpolationMinutes = 45;
                	break;
                }
                
                setSelectedMinutes(interpolationMinutes);
            }

            //mSelectionAllday = false;
        }

        //if (mSelectionMode != SELECTION_EVENT_PRESSED || mSelectionMode != SELECTION_EVENT_MOVED || mSelectionMode != SELECTION_EVENT_FLOATING)
        if (mFragment.getSelectionMode() == SELECTION_HIDDEN)
        	findSelectedEvent(x, y);
       
        // ����ν�� ������� �ʰ� �ֱ� ������ ��ٷ� �����Ѵ�
        //sendAccessibilityEventAsNeeded(true);

        
        return true;
    }   
    
    int mSelectionEndMinutesPixels;
    private void setSelectionBottomFromPosition(final int y) {   
    	if (y < mFirstCell) {
    		// ���� ó���� ���� ���°�?
    		// :��������� ����
    	}
    	
    	int adjustedY = y - mFirstCell;    	
    	
    	if (adjustedY < mFirstHourOffset) { // mFirstHour���� ��ġ������ ����  �ִ� �׷��ϱ� 1�ð� �� hour �ð��뿡 y�� �����ٴ� ���� �ǹ�
        	int unvisibleMinutuesPixel = mCellHeight - mFirstHourOffset;                
            float minutuePosPixel = unvisibleMinutuesPixel + adjustedY;
            mSelectionEndMinutesPixels = (int) minutuePosPixel;                
            
        	int newSelectionHour = mFirstHour - 1;            	
        	
        	if (newSelectionHour < 0) { // new event rec�� am12�� ������ �ʰ��ϴ� ���� ������ 
        		newSelectionHour = 0;
        		mSelectionEndMinutesPixels = 0;
        	}
        	else if (newSelectionHour == 0 && mSelectionEndMinutesPixels < 0) { // new event rec�� am12�� �ʰ��ϴ� ���� ������ 
        		mSelectionEndMinutesPixels = 0;            		
        	}
        	mSelectionEndHour = newSelectionHour;         
            
            int xxxMinutes = mSelectionEndMinutesPixels / mOneQuartersOfHourPixelHeight;
            int interpolationMinutes = 0;
            switch(xxxMinutes) {
            case 0:
            	interpolationMinutes = 0;
            	break;
            case 1:
            	interpolationMinutes = 15;
            	break;
            case 2:
            	interpolationMinutes = 30;
            	break;
            case 3:
            	interpolationMinutes = 45;
            	break;
            }                
            mSelectionEndMinutes = interpolationMinutes;
            
        } else {            	           	
        	
        	// adjustedY�� mFirstHourOffset�� ���ų� ũ�ٶ�� ���� mFirstHour line�� ������ �Ʒ��� �ִ� hour �ð��뿡 y�� �����ٴ� ���� �ǹ�
        	// �׷��Ƿ� �翬�� adjustedY���� mFirstHourOffset�� ������ ���� one hour height�� ������ 
        	// mFirstHour���� �Ʒ� ��� �ð��뿡 y�� �������� �� �� ����
        	// :�̹� ������ mSelectionStartHour�� mFirstHour�� ������            	
        	mSelectionEndMinutesPixels = (adjustedY - mFirstHourOffset) % (mCellHeight + HOUR_GAP); 
            
        	int addHour = (adjustedY - mFirstHourOffset) / (mCellHeight + HOUR_GAP);            	
        	int newSelectionHour = mFirstHour + addHour;
        	
        	if (newSelectionHour > 23) {
        		newSelectionHour = 23;
        		mSelectionEndMinutesPixels = mThreeQuartersOfHour; // pm11:45�� �����
        	}
        	else if (newSelectionHour == 23 && mSelectionEndMinutesPixels > mThreeQuartersOfHour) {
        		mSelectionEndMinutesPixels = mThreeQuartersOfHour; // pm11:45�� �����
        	}
        	mSelectionEndHour = newSelectionHour;                
            
            int xxxMinutes = mSelectionEndMinutesPixels / mOneQuartersOfHourPixelHeight;
            int interpolationMinutes = 0;
            switch(xxxMinutes) {
            case 0:
            	interpolationMinutes = 0;
            	break;
            case 1:
            	interpolationMinutes = 15;
            	break;
            case 2:
            	interpolationMinutes = 30;
            	break;
            case 3:
            	interpolationMinutes = 45;
            	break;
            }
            
            mSelectionEndMinutes = interpolationMinutes;
        }
    	
    }
    

    // �̰� setSelectionFromPosition�� ȣ��� ������ ȣ��Ǵ� ���� ����???
    // �Ź� setSelectedEvent(null);�� �����ϱ� �����̴�!!!
    //....
    private void findSelectedEvent(int x, int y) {
        int date = mSelectionDay;
        int cellWidth = mCellWidth;
        ArrayList<Event> events = mEvents;
        int numEvents = events.size();
        int left = computeDayLeftPosition(mSelectionDay - mFirstJulianDay);
        int top = 0;
        mFragment.setSelectedEvent(null);/////////////////////////////////////////////////////////////////////////////////////////

        mSelectedEvents.clear();//////////////////////////////////////////////////
                
        // Adjust y for the scrollable bitmap
        y += mViewStartY - mFirstCell;/////////////////////////////////////////////////////////////////////

        // Use a region around (x,y) for the selection region
        Rect region = mRect;
        region.left = x - 10;
        region.right = x + 10;
        region.top = y - 10;
        region.bottom = y + 10;

        EventGeometry geometry = mEventGeometry;

        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            // Compute the event rectangle.
            if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
                continue;
            }

            // If the event intersects the selection region, then add it to
            // mSelectedEvents.
            if (geometry.eventIntersectsSelection(event, region)) {
                mSelectedEvents.add(event);
            }
        }

        // If there are any events in the selected region, 
        // then assign the closest one to mSelectedEvent.
        if (mSelectedEvents.size() > 0) {
            int len = mSelectedEvents.size();
            Event closestEvent = null;
            float minDist = mViewWidth + mViewHeight; // some large distance
            for (int index = 0; index < len; index++) {
                Event ev = mSelectedEvents.get(index);
                float dist = geometry.pointToEvent(x, y, ev);
                if (dist < minDist) {
                    minDist = dist;
                    closestEvent = ev;
                }
            }
            
            mFragment.setSelectedEvent(closestEvent);///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // Keep the selected hour and day consistent with the selected
            // event. They could be different if we touched on an empty hour
            // slot very close to an event in the previous hour slot. In
            // that case we will select the nearby event.
            int startDay = mFragment.getSelectedEvent().startDay;
            int endDay = mFragment.getSelectedEvent().endDay;
            if (mSelectionDay < startDay) {
                setSelectedDay(startDay);
            } else if (mSelectionDay > endDay) {
                setSelectedDay(endDay);
            }

            int startHour = mFragment.getSelectedEvent().startTime / 60;
            int endHour;
            if (mFragment.getSelectedEvent().startTime < mFragment.getSelectedEvent().endTime) {
                endHour = (mFragment.getSelectedEvent().endTime - 1) / 60;
            } else {
                endHour = mFragment.getSelectedEvent().endTime / 60;
            }

            if (mSelectionStartHour < startHour && mSelectionDay == startDay) {
                setSelectedHour(startHour);
            } else if (mSelectionStartHour > endHour && mSelectionDay == endDay) {
                setSelectedHour(endHour);
            }
        }
    }

    // Encapsulates the code to continue the scrolling after the
    // finger is lifted. Instead of stopping the scroll immediately,
    // the scroll continues to "free spin" and gradually slows down.
    private class ContinueScroll implements Runnable {

        public void run() {
        	//if (INFO) Log.i("TAG", "ContinueScroll");
        	
        	boolean what_mScrolling = mScrolling;
        	boolean what_mPaused = mPaused;
        	boolean what_computeScrollOffset = mScroller.computeScrollOffset();
        	
            mScrolling = mScrolling && mScroller.computeScrollOffset();
            // evetninfo�κ��� return�� �ϰ� ����, mPaused�� ������ false�̱� ������ fling ������ ������� �ʴ´�
            if (!mScrolling || mPaused) {
                resetSelectedHour();
                invalidate();
                return;
            }

            mViewStartY = mScroller.getCurrY();

            if (mCallEdgeEffectOnAbsorb) {
                if (mViewStartY < 0) {
                    mEdgeEffectTop.onAbsorb((int) mLastVelocity);
                    mCallEdgeEffectOnAbsorb = false;
                } else if (mViewStartY > mMaxViewStartY) {
                    mEdgeEffectBottom.onAbsorb((int) mLastVelocity);
                    mCallEdgeEffectOnAbsorb = false;
                }
                mLastVelocity = mScroller.getCurrVelocity();
            }

            if (mScrollStartY == 0 || mScrollStartY == mMaxViewStartY) {
                // Allow overscroll/springback only on a fling,
                // not a pull/fling from the end
                if (mViewStartY < 0) {
                    mViewStartY = 0;
                } else if (mViewStartY > mMaxViewStartY) {
                    mViewStartY = mMaxViewStartY;
                }
            }

            computeFirstHour();
            mHandler.post(this);
            invalidate();
        }
    }

    /**
     * Cleanup the pop-up and timers.
     */
    public void cleanup() {
        // Protect against null-pointer exceptions
        /*if (mPopup != null) {
            mPopup.dismiss();
        }*/
        mPaused = true;
        mLastPopupEventID = INVALID_EVENT_ID;
        if (mHandler != null) {
            //mHandler.removeCallbacks(mDismissPopup);
            mHandler.removeCallbacks(mUpdateCurrentTime);            
        }
        
        // day fragment���� month fragment�� ��ȯ�� �� �Ʒ� cell height�� ������� �ʰ� �ִ�
        //Log.i("tag", "cleanup : cell height=" + String.valueOf(mCellHeight));        
        Utils.setSharedPreference(mContext, SettingsFragment.KEY_DEFAULT_CELL_HEIGHT,
        		mCellHeight);
        // Clear all click animations
        eventClickCleanup();
        // Turn off redraw
        mRemeasure = false;
        // Turn off scrolling to make sure the view is in the correct state if we fling back to it
        mScrolling = false;
        
        mTouchMode = TOUCH_MODE_INITIAL_STATE;
        
    }

    private void eventClickCleanup() {
        //this.removeCallbacks(mClearClick);
        this.removeCallbacks(mSetClick);
        //mClickedEvent = null;
        mSavedClickedEvent = null;
    }

    
    
    // initNextView
    // setSelected
    private void setSelectedDay(int d) {
        mSelectionDay = d;
        mSelectionDayForAccessibility = d;
    }

    public void setSelectedHour(int h) {      	
        mSelectionStartHour = h;
        mSelectionStartHourForAccessibility = h;
    }
    
    public void setSelectedMinutes(int minutes) {    	
        mSelectionStartMinutes = minutes;        
    }
        
    
    public int getSelectedHour() {    	
        return mSelectionStartHour;
        
    }
    
    public int getSelectedMinutes() {    	
        return mSelectionStartMinutes;        
    }
   
    private void setSelectedEndHour(int h) {  
    	//if (INFO) Log.i(TAG, "setSelectedHour=" + String.valueOf(h));
    	mSelectionEndHour = h;        
    }
    
    private void setSelectedEndMinutes(int minutes) {    	
    	mSelectionEndMinutes = minutes;        
    }
        
    
    private int getSelectedEndHour() {    	
        return mSelectionEndHour;        
    }
    
    private int getSelectedEndMinutes() {    	
        return mSelectionEndMinutes;        
    }
    
    
        

    /**
     * Restart the update timer
     */
    public void restartCurrentTimeUpdates() {
        mPaused = false;
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCurrentTime);
            mHandler.post(mUpdateCurrentTime);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cleanup();
        super.onDetachedFromWindow();
    }

    /*
    class DismissPopup implements Runnable {

        public void run() {
            // Protect against null-pointer exceptions
            if (mPopup != null) {
                mPopup.dismiss();
            }
        }
    }
	*/
    
    public static final int UPDATE_CURRENT_TIME_DELAY_FOR_TODAY_VIEW = 10000;
    class UpdateCurrentTime implements Runnable {

        public void run() {
            long currentTime = System.currentTimeMillis();
            mCurrentTime.setTimeInMillis(currentTime);
            
            if (!DayView.this.mPaused) {
            	//% causes update to occur on 1 minute marks (11:10, 11:11, 11:12, etc.)
            	// numDay�� 1 �׸��� numDay�� 2�� �� ��θ� ����?��Ű�� ���� �ڵ�
            	if ( (mFirstJulianDay <= mTodayJulianDay) && (mTodayJulianDay <= mLastJulianDay) ) {
            		//Log.i("tag", "DayView::UpdateCurrentTime: per 1 minute");
            		mHandler.postDelayed(mUpdateCurrentTime, 
            				UPDATE_CURRENT_TIME_DELAY_FOR_TODAY_VIEW - (currentTime % UPDATE_CURRENT_TIME_DELAY_FOR_TODAY_VIEW));            		
            	}
            	else {
            		//% causes update to occur on 5 minute marks (11:10, 11:15, 11:20, etc.)
            		//Log.i("tag", "DayView::UpdateCurrentTime: per 5 minute");
	                mHandler.postDelayed(mUpdateCurrentTime, 
	                		DayFragment.UPDATE_CURRENT_TIME_DELAY - (currentTime % DayFragment.UPDATE_CURRENT_TIME_DELAY));	                
            	}
            }
            
            mTodayJulianDay = ETime.getJulianDay(currentTime, mTimeZone, mFirstDayOfWeek);
            invalidate();
        }
    }

    class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
    	@Override
        public boolean onDown(MotionEvent ev) {
            if (INFO) Log.i(TAG, "onDown");            
        	DayView.this.doDown(ev);
            return true;
        }
    	
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (INFO) Log.i(TAG, "onSingleTapUp");            
        	DayView.this.doSingleTapUp(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            if (INFO) Log.i(TAG, "onLongPress");            
            DayView.this.doLongPress(ev);             
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (INFO) Log.i(TAG, "GestureDetector.onScroll");
            
            eventClickCleanup();
            
            /*
            if (mTouchStartedInAlldayArea) {
                if (Math.abs(distanceX) < Math.abs(distanceY)) {
                    // Make sure that click feedback is gone when you scroll from the
                    // all day area
                    invalidate();
                    return false;
                }
                // don't scroll vertically if this started in the allday area
                distanceY = 0;
            }
            */
            
            DayView.this.doScroll(e1, e2, distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //if (INFO) Log.i(TAG, "GestureDetector.onFling");
        	/*
            if (mTouchStartedInAlldayArea) {
                if (Math.abs(velocityX) < Math.abs(velocityY)) {
                    return false;
                }
                // don't fling vertically if this started in the allday area
                velocityY = 0;
            }
            */
            DayView.this.doFling(e1, e2, velocityX, velocityY);
            return true;
        }        
    }    
    
    public int getSelectedEventRectHeight() {
    	int newStartHour = getSelectedHour();
    	int newStartNextHour = newStartHour + 1;
    	
    	int endHour = mSelectionEndHour;//int endHour = mFragment.getSelectedEvent().endTime / 60;
    	int endMinutes = mSelectionEndMinutes;//int endMinutes = mFragment.getSelectedEvent().endTime % 60;
    	int endMinutesPixels = mSelectionEndMinutesPixels;        
                

        int hourDuration = endHour - newStartNextHour;
        int hourDurationPixels = hourDuration * (mCellHeight + HOUR_GAP);
        int cellHeight = hourDurationPixels + endMinutesPixels;
        if (mMinutuePosPixelAtSelectedEventMove > 0) {
        	cellHeight = cellHeight + (mCellHeight - mMinutuePosPixelAtSelectedEventMove) + HOUR_GAP;
        }
        else {
        	cellHeight = cellHeight + mCellHeight;
        }
        	            	
    	return cellHeight;
    }
    
    public Event getEvent(long eventId) {
    	int size = mEvents.size();
    	for (int i=0; i<size; i++) {
    		Event obj = mEvents.get(i);
    		if (obj.id == eventId) {
    			return obj;
    		}
    	}
    	
    	return null;
    }
    
    final String[] INSTANCE_PROJECTION = new String[] {
            Instances.EVENT_ID,      // 0
            Instances.BEGIN,         // 1
            Instances.TITLE          // 2
    };
	
    private static final String SORT_ORDER_INSTANCE_BEGIN_TIME_ASC =
            CalendarContract.Instances.BEGIN + " ASC";
    
	public long getNextFirstRecurTimeMillis(long eventId, long startMillis, long endMillis) {		
		
	    String selection = Instances.EVENT_ID + " = ?" ;
	    String[] selectionArgs = new String[] {String.valueOf(eventId)};
	
	    Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
	    // URL�� content://com.android.calendar/instances/when
	    // when�̶�� �ܾ�� ���� start/end time�� ��������� �ϳ� ���� �׷��� ���� ��� ������ ���� ��Ÿ�� ������ �߻��Ѵ�
	    // :Unknown URL content://com.android.calendar/instances/when
	    
	    // �׸��� sort�� �ϴ� �͵� ��������
	    ContentUris.appendId(builder, startMillis);
	    ContentUris.appendId(builder, endMillis);
	
	    //String where=Instances.EVENT_ID + " = " + selectedAppointmentInstance.getAppointmentId();
	    //final int PROJECTION_ID_INDEX = 0;
	    final int PROJECTION_BEGIN_INDEX = 1;
	    //final int PROJECTION_TITLE_INDEX = 2;
	    	    
	    ContentResolver cr = mContext.getContentResolver();
	    Cursor cur = cr.query(builder.build(), 
	            INSTANCE_PROJECTION, 
	            selection, 
	            selectionArgs, 
	            SORT_ORDER_INSTANCE_BEGIN_TIME_ASC);
	
	    if (cur.moveToFirst()) {
	    	long beginVal = 0;  
	    	beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
	    	
	    	Calendar calendar = GregorianCalendar.getInstance();
	        calendar.setTimeInMillis(beginVal); 
	    	SimpleDateFormat formatter  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	    	if (INFO) Log.i(TAG, "getEventInstances:Date: " + formatter.format(calendar.getTime()));   
	    	return beginVal;
	    }
	    else {
	    	return -1;
	    }
	    
	    /*
	    while (cur.moveToNext()) {
	        String title = null;
	        long eventID = 0;
	        long beginVal = 0;    
	
	        // Get the field values
	        eventID = cur.getLong(PROJECTION_ID_INDEX);
	        beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
	        title = cur.getString(PROJECTION_TITLE_INDEX);
	
	        Calendar calendar = GregorianCalendar.getInstance();
	        calendar.setTimeInMillis(beginVal);  
	        SimpleDateFormat formatter  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	        if (INFO) Log.i(TAG, "getEventInstances:Date: " + formatter.format(calendar.getTime()));    
	        //Toast.makeText(view.getContext(), "Date: " + formatter.format(calendar.getTime()), Toast.LENGTH_SHORT).show();
	    }
	    */
	}
	
	static final int SELECTED_EVENT_NONE_MOVE = 0;
	static final int SELECTED_EVENT_MOVE_UP = 1;
	static final int SELECTED_EVENT_MOVE_DOWN = 2;	
	static final int SELECTED_EVENT_MOVE_LEFT = 3;
	static final int SELECTED_EVENT_MOVE_RIGHT = 4;
	
	int mCurNewAndSelectedEventRectCenterX;
	int mCurNewAndSelectedEventRectCenterY;
	
	int mNewAndSelectedEventRectCenterXAfter;
	int mNewAndSelectedEventRectCenterYAfter;
	
	int mNewAndSelectedEventSelfPositionMoveDirection;
	
	float m_function_gradient;
	
	int mOfIntValuesStart;
	int mOfIntValuesEnd;
	
	ValueAnimator mSelectedEventReturnInPlaceTranslateAnimator;
	ValueAnimator mNewEventReturnInPlaceTranslateAnimator;
	
	
	public boolean setNewAndSelectedEventReturnInPlaceAnim() {
		Rect curSelectedEventRect = new Rect();
		curSelectedEventRect.left = (int) mFragment.mSelectedEventRect.getX();
		curSelectedEventRect.top = (int) mFragment.mSelectedEventRect.getY();					
		curSelectedEventRect.right = curSelectedEventRect.left + mFragment.mSelectedEventRect.getWidth();
		curSelectedEventRect.bottom = curSelectedEventRect.top + mFragment.mSelectedEventRect.getHeight();		
		mCurNewAndSelectedEventRectCenterX = (int) curSelectedEventRect.exactCenterX();
		mCurNewAndSelectedEventRectCenterY  = (int) curSelectedEventRect.exactCenterY();
		
		Rect selectedEventRectAfterReturnInPlace = new Rect();
		int daynum = mSelectionDay - mFirstJulianDay;    	
		selectedEventRectAfterReturnInPlace.left = computeDayLeftPosition(daynum) + 1;
		selectedEventRectAfterReturnInPlace.right = selectedEventRectAfterReturnInPlace.left + mFragment.mSelectedEventRect.getWidth();
		
    	int minutesPixel = 0;
        switch(mSelectionStartMinutes) {
        case 0:
        	minutesPixel = 0;
        	break;
        case 15:
        	minutesPixel = mOneQuartersOfHourPixelHeight;
        	break;
        case 30:
        	minutesPixel = 2* mOneQuartersOfHourPixelHeight;
        	break;
        case 45:
        	minutesPixel = 3* mOneQuartersOfHourPixelHeight;
        	break;
        }
        // mFirstHourOffset���� mFirstHour�� mSelectedEventPanel������ Y���� ���� �� �ִ�
        // (mSelectionStartHour - mFirstHour)���� selected event rect�� ��ġ�� Top�� mFirstHour�κ��� �󸶳� ������ �ִ��� �� �� �ִ�
    	//  * (mCellHeight + HOUR_GAP)
        float selectedEventRectY = mFirstHourOffset + HOUR_GAP + ((mSelectionStartHour - mFirstHour) * (mCellHeight + HOUR_GAP)) + minutesPixel;       
        selectedEventRectAfterReturnInPlace.top = (int) selectedEventRectY;       
        selectedEventRectAfterReturnInPlace.bottom = selectedEventRectAfterReturnInPlace.top + mFragment.mSelectedEventRect.getHeight();               
        mNewAndSelectedEventRectCenterXAfter = (int) selectedEventRectAfterReturnInPlace.exactCenterX();
        mNewAndSelectedEventRectCenterYAfter = (int) selectedEventRectAfterReturnInPlace.exactCenterY();		
		
        mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_NONE_MOVE;		
		
		if ( (mNewAndSelectedEventRectCenterYAfter == mCurNewAndSelectedEventRectCenterY) &&
				 (mNewAndSelectedEventRectCenterXAfter == mCurNewAndSelectedEventRectCenterX) ) { // selected event�� left/top�� ��Ȯ�ϰ� selected event�� �ð��뿡 �������ٴ� ���� �ǹ��Ѵ�
			                                                                                      // : ���� �߻��ϱ� ���� ��Ȳ��.			
			if (INFO) Log.i(TAG, "setSelectedEventReturnInPlaceAnim:None Self Position Movement!!!!");
			return false;
				
		}
		else {		
			
			float x_scalar = Math.abs( Math.abs(mCurNewAndSelectedEventRectCenterX) - Math.abs(mNewAndSelectedEventRectCenterXAfter) );
			float y_scalar = Math.abs( Math.abs(mCurNewAndSelectedEventRectCenterY) - Math.abs(mNewAndSelectedEventRectCenterYAfter) );
			
			if (x_scalar > y_scalar) { 
				if (mCurNewAndSelectedEventRectCenterX > mNewAndSelectedEventRectCenterXAfter)
					mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_MOVE_LEFT;
				else if (mCurNewAndSelectedEventRectCenterX < mNewAndSelectedEventRectCenterXAfter)
					mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_MOVE_RIGHT;
			}
			else if (x_scalar < y_scalar)  {  
				if (mCurNewAndSelectedEventRectCenterY > mNewAndSelectedEventRectCenterYAfter)
					mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_MOVE_DOWN;
				else if(mCurNewAndSelectedEventRectCenterY < mNewAndSelectedEventRectCenterYAfter)
					mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_MOVE_UP;
			}
			else { // (x_scalar == y_scalar)�� ���
				   // :x, y ��� �����ϰ� �������ٴ� ���� �ǹ��Ѵ�. �̷� ��찡 �߻��� ���� �ִ�...y���� ������
				if (mCurNewAndSelectedEventRectCenterY > mNewAndSelectedEventRectCenterYAfter)
					mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_MOVE_DOWN;
				else if(mCurNewAndSelectedEventRectCenterY < mNewAndSelectedEventRectCenterYAfter)
					mNewAndSelectedEventSelfPositionMoveDirection = SELECTED_EVENT_MOVE_UP;
			}
		}	
		
		// ���Ⱑ 0�� �� �����ؾ� �Ѵ�!!!!!!!!!!!!!!!!!!!!!!!!!!
		float curSelectedEventRectCenterY = mCurNewAndSelectedEventRectCenterY;
		float selectedEventRectCenterYAfter = mNewAndSelectedEventRectCenterYAfter;
		float curSelectedEventRectCenterX = mCurNewAndSelectedEventRectCenterX;
		float selectedEventRectCenterXAfter = mNewAndSelectedEventRectCenterXAfter;
		m_function_gradient = (curSelectedEventRectCenterY - selectedEventRectCenterYAfter) /
				                                         (curSelectedEventRectCenterX - selectedEventRectCenterXAfter);				
		
		
		switch (mNewAndSelectedEventSelfPositionMoveDirection) {
		// x���� �̿��Ѵ�
		case SELECTED_EVENT_MOVE_LEFT:					
		case SELECTED_EVENT_MOVE_RIGHT:
			mOfIntValuesStart = mCurNewAndSelectedEventRectCenterX;
			mOfIntValuesEnd = mNewAndSelectedEventRectCenterXAfter;				
			break;
		// y���� �̿��Ѵ�
		case SELECTED_EVENT_MOVE_UP:				
		case SELECTED_EVENT_MOVE_DOWN:
			mOfIntValuesStart = mCurNewAndSelectedEventRectCenterY;
			mOfIntValuesEnd = mNewAndSelectedEventRectCenterYAfter;				
			break;
		default:
			mOfIntValuesStart = 0;
			mOfIntValuesEnd = 0;
			break;
		} 	
		
		return true;
		
	}
	
	public void setSelectedEventReturnInPlaceValueAnimator() {		
		
		mSelectedEventReturnInPlaceTranslateAnimator = ValueAnimator.ofInt(mOfIntValuesStart, mOfIntValuesEnd);
    	long duration = calculateDuration(Math.abs(mOfIntValuesStart - mOfIntValuesEnd), mViewWidth / 2, 5000);
    	mSelectedEventReturnInPlaceTranslateAnimator.setDuration(duration);
    	mSelectedEventReturnInPlaceTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				int delta = (Integer) valueAnimator.getAnimatedValue();	
				
				translateNewAndSelectedEventRect(delta);	
			}
		});
		
    	mSelectedEventReturnInPlaceTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {			
				//mTouchMode = TOUCH_MODE_INITIAL_STATE;
				if (mFragment.mSelectedEventRequiredUpdate) {
					if (mFragment.getSelectedEvent().isRepeating) {
						int howFarMinutes = mFragment.getSelectedEvent().endTime - mFragment.getSelectedEvent().startTime;
	                	long howFarMillis = (howFarMinutes * 60) * 1000;
	                	
	                	Calendar startTimeOfMovedSelectedEventRect = getSelectedDay();
	                	Calendar endTimeOfMovedSelectedEventRect = getSelectedDay();                	
	                	long startTimeMillisOfMovedSelectedEventRect = startTimeOfMovedSelectedEventRect.getTimeInMillis();
	                	long endTimeMillisOfMovedSelectedEventRect = startTimeMillisOfMovedSelectedEventRect + howFarMillis;
	                	endTimeOfMovedSelectedEventRect.setTimeInMillis(endTimeMillisOfMovedSelectedEventRect);   
	                	mFragment.removeSelectedEventPanelTouchListener();
	                	mFragment.setSelectionMode(SELECTION_EVENT_TIME_MODIFICATION_DB_POPUP_AFTER_MOVED, SELECTION_ACTION_UP_TOUCHEVENT);
						mFragment.popUpEventTimeModificationOkDB(startTimeOfMovedSelectedEventRect.getTimeInMillis(), endTimeOfMovedSelectedEventRect.getTimeInMillis());
						
					}
					else {
						mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
						mFragment.resetSelectedEventPanelTouchListener();
					}
				}
				else {
					mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
					mFragment.resetSelectedEventPanelTouchListener();
				}
				
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
    	
    	mSelectedEventReturnInPlaceTranslateAnimator.start();
	
	}	
	
	
	
	public void setNewEventReturnInPlaceValueAnimator(final float rawX, final float rawY) {		
		
		mNewEventReturnInPlaceTranslateAnimator = ValueAnimator.ofInt(mOfIntValuesStart, mOfIntValuesEnd);
    	long duration = calculateDuration(Math.abs(mOfIntValuesStart - mOfIntValuesEnd), mViewWidth / 2, 5000);
    	mNewEventReturnInPlaceTranslateAnimator.setDuration(duration);
    	mNewEventReturnInPlaceTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				int delta = (Integer) valueAnimator.getAnimatedValue();	
				
				translateNewAndSelectedEventRect(delta);	
			}
		});
		
    	mNewEventReturnInPlaceTranslateAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {			
				
				mFragment.resetSelectedEventRequireUpdateFlags();
        		mFragment.setSelectionMode(SELECTION_HIDDEN, SELECTION_NOT_BOTH_TOUCHEVENT_AND_GESTURE);
        		mFragment.resetSelectedEventPanelTouchListener();
        		
        		long extraLong = 0; 
                
                mController.sendEventRelatedEventWithExtra(DayView.this, EventType.CREATE_EVENT, -1,
                        DayView.this.getSelectedTimeInMillis(), 0, (int) rawX, (int) rawY,
                        extraLong, -1);               
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
    	
    	mNewEventReturnInPlaceTranslateAnimator.start();
	}
	
	
	public void translateNewAndSelectedEventRect(int delta) {
		int centerposition_X = 0;
		int centerposition_Y = 0;
		
		switch (mNewAndSelectedEventSelfPositionMoveDirection) {
		
		case SELECTED_EVENT_MOVE_LEFT:						
		case SELECTED_EVENT_MOVE_RIGHT:
			centerposition_X = delta;
			centerposition_Y = (int)( m_function_gradient * (centerposition_X - mCurNewAndSelectedEventRectCenterX) ) + mCurNewAndSelectedEventRectCenterY;		
			break;
		
		case SELECTED_EVENT_MOVE_UP:											
		case SELECTED_EVENT_MOVE_DOWN:
			centerposition_Y = delta;
			centerposition_X = (int)((centerposition_Y - mCurNewAndSelectedEventRectCenterY) / m_function_gradient) + mCurNewAndSelectedEventRectCenterX;		
			break;
		default:						
			break;
		}
		
		float x = centerposition_X - (mFragment.getSelectedEventRect().getWidth() / 2);
		float y = centerposition_Y - (mFragment.getSelectedEventRect().getHeight() / 2);
		
		mFragment.getSelectedEventRect().setX(x);
		if (mFragment.getSelectionMode() == SELECTION_NEW_EVENT_ADJUST_POSITION_AFTER_MOVED) {
			mFragment.getSelectedEventRect().setY(y);
		}
		else {
			int outLineY = (int) (y - mFragment.getSelectedEventRect().getTopMargin());
	    	mFragment.getSelectedEventRect().setY(outLineY);
		}
	}


    // The rest of this file was borrowed from Launcher2 - PagedView.java
    private static final int MINIMUM_SNAP_VELOCITY = 2200;

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

    private long calculateDuration(float delta, float width, float velocity) {
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
        velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
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
    
    
    public void detectExpandSelection(MotionEvent ev) {
    	int x = (int) ev.getX();
        int y = (int) ev.getY();
        
        int upperExpandCircleCenterY = (int) mFragment.mSelectedEventRect.getY() + mFragment.getSelectedEventRect().getRadiusExpandCircle();
        
    	if (mSelectedEventRectExpandingWhichCirclePoint == SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT) { 
    		
    		// ���� ������ ���� upper expand circle �߽ɿ��� �� �κ����� �Ʒ� �κ������� �����Ѵ�
    		if (mFragment.getPrvNewOrSelectedEventMoveY() <= upperExpandCircleCenterY) { // ���� ������ ���� upper expand circle �߽ɿ��� �� �κ�
    			
    			if (y > mFragment.getPrvNewOrSelectedEventMoveY()) { 
    				// dayview_selected_event_expanding_circle_layout ������ 3�� ���    				
    				if ( y <= upperExpandCircleCenterY) {    					
    					mFragment.setPrvNewOrSelectedEventMoveY(y);
    					if (INFO) Log.i(TAG, "ACTION_MOVE:SEUC:3");    
    					return ;
    				}
    			}
    		}
    		else if (mFragment.getPrvNewOrSelectedEventMoveY() > upperExpandCircleCenterY) { // ���� ������ ���� upper expand circle �߽ɿ��� �Ʒ� �κ�
    			
    			if (y < mFragment.getPrvNewOrSelectedEventMoveY()) { 
    				// dayview_selected_event_expanding_circle_layout ������ 6�� ���
    				if (y >= upperExpandCircleCenterY) {    					
    					mFragment.setPrvNewOrSelectedEventMoveY(y);
                		if (INFO) Log.i(TAG, "ACTION_MOVE:SEUC:6");    
    					return ;
    				}
    			}    			
			}    		                		
    		
    		// DayView���� selected event rect�� drawing�ϱ� ������,
    		// SelectedEventRect�� Top/Bottom Margin[expanding cirlce ����]�� �ʿ� ����.
    		// :�� y���� outlineY ���� ���̴�    		
    		mFragment.setPrvNewOrSelectedEventTop(y);
    		setSelectionFromPosition(x, y, mFragment.getPrvNewOrSelectedEventTop());    
    	}
    	else {                		
    		int SelectedEventBottom = (int) (mFragment.mSelectedEventRect.getY() + mFragment.mSelectedEventRect.getHeight());
    		int lowerExpandCircleCenterY = SelectedEventBottom - mFragment.getSelectedEventRect().getRadiusExpandCircle();
    		
    		// ���� ������ ���� lower expand circle �߽ɿ��� �� �κ����� �Ʒ� �κ������� �����Ѵ�
    		if (mFragment.getPrvNewOrSelectedEventMoveY() > lowerExpandCircleCenterY) { // ���� ������ ���� lower expand circle �߽ɿ��� �Ʒ� �κ�
    			
    			if (y < mFragment.getPrvNewOrSelectedEventMoveY()) {     				
    				// dayview_selected_event_expanding_circle_layout ������ Lower Case�� 3�� ���
    				if (y >= lowerExpandCircleCenterY) {     					
    					mFragment.setPrvNewOrSelectedEventMoveY(y);
                		if (INFO) Log.i(TAG, "ACTION_MOVE:SELC:3");    
    					return ;
    				}
    			}
    		}
    		else { // ���� ������ ���� lower expand circle �߽ɿ��� �� �κ�
    			
    			if (y > mFragment.getPrvNewOrSelectedEventMoveY()) {
    				// dayview_selected_event_expanding_circle_layout ������ Lower Case�� 6�� ���
    				if (y <= lowerExpandCircleCenterY) {      					
    					mFragment.setPrvNewOrSelectedEventMoveY(y);
                		if (INFO) Log.i(TAG, "ACTION_MOVE:SELC:6"); 
                		return ;
    				}
    			}   			
    		}    		
    		
    		// DayView���� selected event rect�� drawing�ϱ� ������,
    		// SelectedEventRect�� Top/Bottom Margin[expanding cirlce ����]�� �ʿ� ����.
    		// :�� y���� outlineY ���� ���̴�
    		mFragment.setPrvNewOrSelectedEventTop(upperExpandCircleCenterY);
    		setSelectionFromPosition(x, upperExpandCircleCenterY, upperExpandCircleCenterY);
    		mFragment.setPrvNewOrSelectedEventBottom(y);
    		setSelectionBottomFromPosition(y);
    	}
    	
    	mFragment.setSelectionMode(SELECTION_EVENT_EXPANDING, SELECTION_ACTION_MOVE_TOUCHEVENT);
    	
        mFragment.setPrvNewOrSelectedEventMoveY(y);        
    	
    	mFragment.getSelectedEventRect().setVisibility(View.GONE);               	
    	
    	invalidate();
    	
    	return ;
    }
    
    ValueAnimator mAdjustMinutesAfterExpandingCompletion;
    public void setAdjustMinutesAnim() {
    	final float oneMinutePixels = mCellHeight / 60.0f; //1.783333
    	float animstartMinutes = 0;
    	int targetMinutesPixels;
    	float delta = 0;
    	
    	if (mSelectedEventRectExpandingWhichCirclePoint == SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT) {                    	
    		targetMinutesPixels = mSelectionStartMinutes * mOneQuartersOfHourPixelHeight;
    		
    		delta = Math.abs(mMinutuePosPixelAtSelectedEventMove - targetMinutesPixels);
    		animstartMinutes = mMinutuePosPixelAtSelectedEventMove / oneMinutePixels;
    		mAdjustMinutesAfterExpandingCompletion = ValueAnimator.ofInt((int)animstartMinutes, mSelectionStartMinutes);
        }
        else {                    	
        	targetMinutesPixels = mSelectionEndMinutes * mOneQuartersOfHourPixelHeight;
        	
        	delta = Math.abs(mSelectionEndMinutesPixels - targetMinutesPixels);        	
        	animstartMinutes = mSelectionEndMinutesPixels / oneMinutePixels;
    		mAdjustMinutesAfterExpandingCompletion = ValueAnimator.ofInt((int)animstartMinutes, mSelectionEndMinutes);
        }
    	
    	long duration = calculateDuration(Math.abs(delta), mCellHeight, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY);
    	
    	mAdjustMinutesAfterExpandingCompletion.setDuration(duration);
    	mAdjustMinutesAfterExpandingCompletion.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				int values =  (Integer) valueAnimator.getAnimatedValue();				
				
				if (mSelectedEventRectExpandingWhichCirclePoint == SELECTION_EVENT_EXPANDING_UPPER_CIRCLEPOINT) { 
					int startMinutesPixel = (int) (values * oneMinutePixels);	
					int newStartHour = getSelectedHour();
		        	int newStartNextHour = newStartHour + 1;
		        	
		        	int endHour = mSelectionEndHour;		
		        	switch(mSelectionEndMinutes) {
			        case 0:
			        	mSelectionEndMinutesPixels = 0;
			        	break;
			        case 15:
			        	mSelectionEndMinutesPixels = mOneQuartersOfHourPixelHeight;
			        	break;
			        case 30:
			        	mSelectionEndMinutesPixels = 2* mOneQuartersOfHourPixelHeight;
			        	break;
			        case 45:
			        	mSelectionEndMinutesPixels = 3* mOneQuartersOfHourPixelHeight;
			        	break;
			        }
		        	                

		            int hourDuration = endHour - newStartNextHour;
		            int hourDurationPixels = hourDuration * (mCellHeight + HOUR_GAP);
		            int cellHeight = hourDurationPixels + mSelectionEndMinutesPixels;
		            if (startMinutesPixel > 0) {
		            	cellHeight = cellHeight + (mCellHeight - startMinutesPixel) + HOUR_GAP;
		            }
		            else {
		            	cellHeight = cellHeight + mCellHeight;
		            }
		            
		            cellHeight = mFragment.getSelectedEventRect().getTopMargin() + cellHeight + mFragment.getSelectedEventRect().getBottomMargin();
		    		
					LinearLayout.LayoutParams selectedEventRectParam = (LinearLayout.LayoutParams) mFragment.getSelectedEventRect().getLayoutParams();	    	        
					selectedEventRectParam.height = cellHeight;
					mFragment.getSelectedEventRect().setLayoutParams(selectedEventRectParam);
					
		            float selectedEventRectY = mFirstHourOffset + ((mSelectionStartHour - mFirstHour) * (mCellHeight + HOUR_GAP)) + startMinutesPixel;		            
		            int outLineY = (int) (selectedEventRectY - mFragment.getSelectedEventRect().getTopMargin());
		            mFragment.getSelectedEventRect().setY(outLineY);					
		            
		        }
		        else {                    	
		        	int endMinutesPixel = (int) (values * oneMinutePixels);	
					int startHour = getSelectedHour();
		        	int startNextHour = startHour + 1;
		        	
		        	int endHour = mSelectionEndHour;		        			        	                

		            int hourDuration = endHour - startNextHour;
		            int hourDurationPixels = hourDuration * (mCellHeight + HOUR_GAP);
		            int cellHeight = hourDurationPixels + endMinutesPixel;
		            if (mMinutuePosPixelAtSelectedEventMove > 0) {
		            	cellHeight = cellHeight + (mCellHeight - mMinutuePosPixelAtSelectedEventMove) + HOUR_GAP;
		            }
		            else {
		            	cellHeight = cellHeight + mCellHeight;
		            }
		            
		            cellHeight = mFragment.getSelectedEventRect().getTopMargin() + cellHeight + mFragment.getSelectedEventRect().getBottomMargin();
		    		
					LinearLayout.LayoutParams selectedEventRectParam = (LinearLayout.LayoutParams) mFragment.getSelectedEventRect().getLayoutParams();	    	        
					selectedEventRectParam.height = cellHeight;
					mFragment.getSelectedEventRect().setLayoutParams(selectedEventRectParam);
					
		            float selectedEventRectY = mFirstHourOffset + ((mSelectionStartHour - mFirstHour) * (mCellHeight + HOUR_GAP)) + mMinutuePosPixelAtSelectedEventMove;		            
		            int outLineY = (int) (selectedEventRectY - mFragment.getSelectedEventRect().getTopMargin());
		            mFragment.getSelectedEventRect().setY(outLineY);					
		        }                				
				                				
			}
		});
		
    	mAdjustMinutesAfterExpandingCompletion.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {			
				mTouchMode = TOUCH_MODE_INITIAL_STATE;
				Calendar startTimeOfMovedSelectedEventRect = getSelectedDay();
				Calendar endTimeOfMovedSelectedEventRect = getSelectedDay();     
				endTimeOfMovedSelectedEventRect.set(Calendar.HOUR_OF_DAY, DayView.this.mSelectionEndHour);//endTimeOfMovedSelectedEventRect.hour = DayView.this.mSelectionEndHour;
				endTimeOfMovedSelectedEventRect.set(Calendar.MINUTE, DayView.this.mSelectionEndMinutes);//endTimeOfMovedSelectedEventRect.minute = DayView.this.mSelectionEndMinutes;
            	            	
				if (mFragment.getSelectedEvent().isRepeating) {			
	            	
	            	mFragment.removeSelectedEventPanelTouchListener();
	            	mFragment.setSelectionMode(SELECTION_EVENT_TIME_MODIFICATION_DB_POPUP_AFTER_EXPANDING, 0);
					mFragment.popUpEventTimeModificationOkDB(startTimeOfMovedSelectedEventRect.getTimeInMillis(), endTimeOfMovedSelectedEventRect.getTimeInMillis());		
				}
				else {
					
					AsyncQueryService service = new AsyncQueryService(mActivity); 
                	Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mFragment.getSelectedEvent().id);
                	ContentValues values = new ContentValues();  
                	
                	values.put(Events.DTSTART, startTimeOfMovedSelectedEventRect.getTimeInMillis());
                	values.put(Events.DURATION, (String) null);
                	values.put(Events.DTEND, endTimeOfMovedSelectedEventRect.getTimeInMillis());
                	
                	// java.lang.IllegalArgumentException: Cannot have both DTEND and DURATION in an event
                	// :�� ��Ȳ�� �ش� event�� rrule�� ������ �ִ� ���, �� �ſ� �ݺ��Ǵ� ���???
                	service.startUpdate(0, null, uri, values, null, null, 0); 
                	
					mFragment.resetSelectedEventPanelTouchListener();
					mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, 0);					
				}
				
				invalidate();
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
    
    
    public void cancelSelectedEventMovedOrExpandingModification() {
    	
    	setSelectedHour(mFragment.getSelectedEvent().startTime / 60);
    	setSelectedMinutes(mFragment.getSelectedEvent().startTime % 60);
    	
    	setSelectedEndHour(mFragment.getSelectedEvent().endTime / 60);                    
    	setSelectedEndMinutes(mFragment.getSelectedEvent().endTime % 60); 
    	
    	float selectedEventRectY = 0;
    	
    	int minutesPixel = 0;
        switch(mSelectionStartMinutes) {
        case 0:
        	minutesPixel = 0;
        	break;
        case 15:
        	minutesPixel = mOneQuartersOfHourPixelHeight;
        	break;
        case 30:
        	minutesPixel = 2* mOneQuartersOfHourPixelHeight;
        	break;
        case 45:
        	minutesPixel = 3* mOneQuartersOfHourPixelHeight;
        	break;
        }
    	
    	if (mFragment.getSelectionMode() == SELECTION_EVENT_TIME_MODIFICATION_DB_POPUP_AFTER_MOVED) {
    		//mFragment.setPrvNewOrSelectedEventBottom(mFragment.getPrvNewOrSelectedEventTop() + cellHeight);
    	}
    	else {
    		LinearLayout.LayoutParams selectedEventRectParam = (LinearLayout.LayoutParams) mFragment.getSelectedEventRect().getLayoutParams();
            int effectiveWidth = mViewWidth - getHoursWidth();
            int cellHeight = (int) (mFragment.getSelectedEvent().bottom - mFragment.getSelectedEvent().top) + GRID_LINE_TOP_MARGIN;    
            selectedEventRectParam.width = effectiveWidth;
            selectedEventRectParam.height = cellHeight;///////////////////////////////////////////////////////////
            mFragment.getSelectedEventRect().setLayoutParams(selectedEventRectParam);  
            
            //mFragment.setPrvNewOrSelectedEventBottom(mFragment.getPrvNewOrSelectedEventTop() + cellHeight);
    	}
    	        
    	selectedEventRectY = mFirstHourOffset + ((mSelectionStartHour - mFirstHour) * (mCellHeight + HOUR_GAP)) + minutesPixel;
        // setY�� �����Ǵ� ���� mSelectedEventPanel���� y���̴�
        //mViewStartX
        int outLineY = (int) (selectedEventRectY - mFragment.getSelectedEventRect().getTopMargin());
        mFragment.getSelectedEventRect().setY(outLineY);
        
        mFragment.setSelectionMode(SELECTION_EVENT_FLOATING, 0);
        
        mFragment.resetSelectedEventPanelTouchListener();
    }
    
    
    
    
}
