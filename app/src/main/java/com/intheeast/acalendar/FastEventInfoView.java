package com.intheeast.acalendar;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.intheeast.alerts.QuickResponseActivity;
import com.intheeast.calendarcommon2.DateException;
import com.intheeast.calendarcommon2.Duration;
import com.intheeast.calendarcommon2.EventRecurrence;
import com.intheeast.acalendar.CalendarEventModel.Attendee;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.etc.ETime;
import com.intheeast.event.AttendeesView;
import com.intheeast.event.EditEventHelper;
import com.intheeast.event.EventViewUtils;
import com.intheeast.event.EventViewUtils.ReminderItemLayoutTag;
import com.intheeast.settings.SettingsFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
//import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class FastEventInfoView extends RelativeLayout {
	
	public static final int EVENT_MAIN_PAGE = 0;
	public static final int EVENT_REPEAT_PAGE = 1;
	public static final int EVENT_REPEATEND_PAGE = 2;
	public static final int EVENT_REMINDER_PAGE = 3;	
	public static final int EVENT_CALENDAR_PAGE = 4;
	public static final int EVENT_ATTENDEE_PAGE = 5;
	public static final int EVENT_AVAILABILITY_PAGE = 6;
	public static final int EVENT_VISIBILITY_PAGE = 7;
	public static final int EVENT_ATTENDEE_SUB_PAGE = 8;
	
	private static final int FADE_IN_TIME = 300;   // in milliseconds
    private static final int LOADING_MSG_DELAY = 600;   // in milliseconds
	//private static final int LOADING_MSG_DELAY = 200;   // in milliseconds
    private static final int LOADING_MSG_MIN_DISPLAY_TIME = 600;
    
    Context mContext;
	Activity mActivity;
	FastEventInfoFragment mFragment;
	//ViewSwitcher mFrameLayoutViewSwitcher;
	ViewSwitcher mEventInfoViewsSwitcher;
	//View mMainPageView;
	
	//private TextView mTitle;
    //private TextView mWhenDateTime;
    //private TextView mWhere;
    //private ExpandableTextView mDesc;
    //private View mGuestlistContainer;
    private AttendeesView mLongAttendees = null;    
        
    //private View mHeadlines;
    ScrollView mScrollView;
    //private View mLoadingMsgView;
    //private View mErrorMsgView;    
    //private long mLoadingMsgStartTime;
    
    private boolean mNoCrossFade = false;  // Used to prevent repeated cross-fade    
    private boolean mCanModifyCalendar = false;
    
    boolean mQueryAllDone;    
    
    int mCurrentViewingPage;   
    int mMainPageViewWidth = -1;
    int mMainPageViewHeight = -1;
    int mViewWidth;
	int mViewHeight;
    TranslateAnimation leftInAnimationForMainbackText;
    // makeExpandResponseMenuItemSize는 더 이상 사용되고 있지 않다
    // :onLayout로 대체됨
    OnLayoutChangeListener mMainPageViewOnLayoutChangeListener = new OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {		
			
			if (mMainPageViewWidth == -1) {
				//Log.i("tag", "EventInfoView : mainPageViewOnLayoutChangeListener 1st Time");
				
				mMainPageViewWidth = right - left;
				mMainPageViewHeight = bottom - top;			
				
				int responseMenuHeight = (int)mActivity.getResources().getDimension(R.dimen.eventItemHeight);
				int scrollViewHeight = mMainPageViewHeight - responseMenuHeight;
				//ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(mMainPageViewWidth, scrollViewHeight);
				LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(mMainPageViewWidth, scrollViewHeight);
				// The linearlayout should now fill the scrollview.
				// 현재 RelativeLayout의 자식으로 스크롤뷰가 설정되어 있다
				// :이는 런타임 에러를 발생시킨다!!! ---> linearlayout 로 수정한다
				//  그래도 에러가 발생된다!!!
				//  최종적으로 LinearLayout.LayoutParams로 수정해 주었다...레이아웃 파라미터는 해당 뷰의 속한 부모 레이아웃이 어떤 뷰인지가 중요한 것 같다???
				mScrollView.setLayoutParams(param);
				
	    		mViewWidth = mMainPageViewWidth;
	    		//gotResponseMenuItemSizeFlag(GOT_VIEW_SIZE);
	    		
	    		float xOffSet = mViewWidth / 2;
	    		mViewSwitchingDelta = mViewWidth - xOffSet;
	    		mAnimationDistance = mViewWidth - xOffSet;
	    			
	    		leftInAnimationForMainbackText = new TranslateAnimation(
	    	            Animation.ABSOLUTE, mViewWidth / 2, //fromXValue 
	    	            Animation.RELATIVE_TO_PARENT, 0,   //toXValue
	    	            Animation.ABSOLUTE, 0.0f,
	    	            Animation.ABSOLUTE, 0.0f); 				
			}
			else {
				//Log.i("tag", "EventInfoView : mainPageViewOnLayoutChangeListener");
			}
		}		
	};
	
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (mMainPageViewWidth == -1) {
			//Log.i("tag", "EventInfoView : mainPageViewOnLayoutChangeListener 1st Time");
			
			mMainPageViewWidth = right - left;
			mMainPageViewHeight = bottom - top;			
			
    		mViewWidth = mMainPageViewWidth;
    		//gotResponseMenuItemSizeFlag(GOT_VIEW_SIZE);
    		
    		float xOffSet = mViewWidth / 2;
    		mViewSwitchingDelta = mViewWidth - xOffSet;
    		mAnimationDistance = mViewWidth - xOffSet;
    			
    		leftInAnimationForMainbackText = new TranslateAnimation(
    	            Animation.ABSOLUTE, mViewWidth / 2, //fromXValue 
    	            Animation.RELATIVE_TO_PARENT, 0,   //toXValue
    	            Animation.ABSOLUTE, 0.0f,
    	            Animation.ABSOLUTE, 0.0f); 
    		
    		//FastEventInfoView.this.mFrameLayoutViewSwitcher.showNext();
		}
		else {
			//Log.i("tag", "EventInfoView : mainPageViewOnLayoutChangeListener");
		}
		
		super.onLayout(changed, left, top, right, bottom);
	}	

	@Override
	protected void onDraw(Canvas canvas) {
		Log.i("tag", "EventInfoView : onDraw");
		super.onDraw(canvas);
	}	
	
    OnClickListener mResponseMenuItemsClickListener = new OnClickListener() {
    	@Override
        public void onClick(View v){        	
    		// If we haven't finished the return from the dialog yet, don't display.
            if (mFragment.mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
                return;
            }
            
            int viewID = v.getId();        	
        	// If this is not a repeating event, then don't display the dialog
            // asking which events to change.
            int response = getResponseFromButtonId(viewID);
            checkResponseContainer(response);
            
            if (!isRepeating()) {
            	mFragment.mUserSetResponse = response;
                return;
            }
            
            // If the selection is the same as the original, then don't display the
            // dialog asking which events to change.
            if (viewID == findButtonIdForResponse(mFragment.mOriginalAttendeeResponse)) {
            	mFragment.mUserSetResponse = response;
                return;
            }
            
            // This is a repeating event. We need to ask the user if they mean to
            // change just this one instance or all instances.
            mFragment.mTentativeUserSetResponse = response;
            mFragment.mEditResponseHelper.showDialog(mFragment.mWhichEvents);
    	}
    };    
    
    //mDeleteMenuText
    OnClickListener mDeleteMenuTextClickListener = new OnClickListener() {
    	@Override
        public void onClick(View v){        	
    		// If we haven't finished the return from the dialog yet, don't display.
            Log.i("tag", "select Delete Menu");
    	}
    };    
    
    
    OnClickListener mCalendarLayoutClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {			
			mCalendarSelectPageView.constructLayout(FastEventInfoView.this, mFragment.getCalendarInfoList());
			mFragment.replaceActionBarBySelectingEventItemLayout(EVENT_CALENDAR_PAGE);
			FastEventInfoView.this.settingViewSwitching(true, EVENT_CALENDAR_PAGE);
		}    	
    };
    
    OnClickListener mReminderLayoutClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
			ReminderItemLayoutTag tag = (ReminderItemLayoutTag)v.getTag();
			// 실제 알림들의 데이터 저장소는
			// mReminderViews 이다!!!			
			int minuteLabelsIndex = EventViewUtils.findMinutesInReminderList(mReminderMinuteValues, tag.mReminderMinute);
	        String minuteTextOfTag = mReminderMinuteLabels.get(minuteLabelsIndex);     			
			Log.i("tag", "mReminderLayoutClickListener : minuteTextOfTag = " + minuteTextOfTag);			
			
			//mReminderPageView.removeAllViews(); // 이 코드 때문에 계속 차일드 뷰들이 계속 디스플레이가 되지 않았다...젠장...ㅡㅡ;
			mReminderPageView.constructLayout(FastEventInfoView.this, mReminderMinuteValues, mReminderMinuteLabels, mReminderViews); // 최초 처음 한번만 하면 될것 같다... 이 두 개의 값을 얻는 곳에서...
			mReminderPageView.resetReminderPage(tag.mLayoutListIndex, minuteLabelsIndex);
			mFragment.replaceActionBarBySelectingEventItemLayout(EVENT_REMINDER_PAGE);
			FastEventInfoView.this.settingViewSwitching(true, EVENT_REMINDER_PAGE);
		}    	
    };
    
    OnClickListener mAttendeesLayoutClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {			
			mAttendeesInfoSwitchPageView.constructMainPage(mLongAttendees.getAttendeeItemList());
			mFragment.replaceActionBarBySelectingEventItemLayout(EVENT_ATTENDEE_PAGE);
			FastEventInfoView.this.settingViewSwitching(true, EVENT_ATTENDEE_PAGE);
		}    	
    };
    
    // ReminderSelectPageView.onClick[Reminder sub page의 item layout을 클릭함]에서 호출함 
    public void returnMainPage(int pageId) {
    	mFragment.replaceActionBarBySelectingEventItemLayout(EVENT_MAIN_PAGE);
    	FastEventInfoView.this.settingViewSwitching(false, pageId);
    }
    
    //boolean mUseEventCursors;
    
    public FastEventInfoView(Context context, AttributeSet attrs) {
    	super(context);    	
    	mContext = context;   
    	mDisplayMetrics = getResources().getDisplayMetrics();
    }
    
    public void setActivity(Activity activity) {
    	mActivity = activity;
    }
    
    public void setFragment(FastEventInfoFragment fragment) {
    	mFragment = fragment;
    }
    
    
    public void setFrameLayoutViewSwitcher(ViewSwitcher viewSwitcher) {
    	/*mFrameLayoutViewSwitcher = viewSwitcher;	
    	mEventInfoViewsSwitcher = mFrameLayoutViewSwitcher;*/
    	mEventInfoViewsSwitcher = viewSwitcher;
    }
    
    /*
    public void useEventCursors(boolean useEventCursors) {
    	mUseEventCursors = useEventCursors;
    } 
    */   
    
	// If the EventInfoActivity is being re-initialized after previously being shut down,
	// then reminders para contains the data it most recently supplied in onSaveInstanceState.
	public void setReminders(ArrayList<ReminderEntry> reminders) {////////////////////////////////////////////////////////////////////////////////////////////////
		mReminders = reminders;
	}
	
	LinearLayout mEventInfoContainer;
	//LinearLayout mEventInfoHeadline;
	DrawableBolderLinearLayout mEventInfoHeadline;
	DrawableBolderRelativeLayout mEventInfoCalendarContainer;
	DrawableBolderRelativeLayout mEventInfoOrganizerContainer;
	DrawableBolderRelativeLayout mEventInfoGuestlistContainer;
	LinearLayout mEventInfoReminderItemscontainer; // 사용할 필요가 없는데 일단은???
	ExpandableTextView mEventInfoDescription;	
	
	//DrawableBolderRelativeLayout mResponseMenu;	
	public TextView mResponse_yes;
    public TextView mResponse_maybe;
    public TextView mResponse_no;
    int mSelectedResponseValue;
    
    //DrawableBolderRelativeLayout mDeleteMenu;
    public TextView mDeleteMenuText;
    
    DrawableBolderRelativeLayout mEventInfoLowerActionbar;
    
	//View mRemindersContainer;
	ReminderSelectPageView mReminderPageView;
	AttendeesInfoSwitchPageView mAttendeesInfoSwitchPageView;
	CalendarSelectPageView mCalendarSelectPageView;
	
	DisplayMetrics mDisplayMetrics;
	
	Runnable mShowNextCallBack;
	public void setGlobalLayoutListener(Runnable callBack) {
		mShowNextCallBack = callBack;
		ViewTreeObserver VTO = getViewTreeObserver(); 
		VTO.addOnGlobalLayoutListener(mOnGlobalLayoutListener);			
	}
	
	OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			Log.i("tag", "EventInfoView : onGlobalLayout");			
	    	FastEventInfoView.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);	
	    	mShowNextCallBack.run();	    			
		}		
	};	
	
	public void init() {		
		
		mHScrollInterpolator = new ScrollInterpolator();
		
		mScrollView = (ScrollView) findViewById(R.id.event_info_scroll_view); 		
			
		mScrollView.setVerticalScrollBarEnabled(false);	
		
		mEventInfoContainer = (LinearLayout)mScrollView.findViewById(R.id.event_info_container);	
		
		mEventInfoHeadline = (DrawableBolderLinearLayout)mEventInfoContainer.findViewById(R.id.event_info_headline);	
		
		mEventInfoCalendarContainer = (DrawableBolderRelativeLayout)mEventInfoContainer.findViewById(R.id.event_info_calendar_container);  
		
		mEventInfoOrganizerContainer = (DrawableBolderRelativeLayout)mEventInfoContainer.findViewById(R.id.event_info_organizer_container);  
		
		mEventInfoGuestlistContainer = (DrawableBolderRelativeLayout)mEventInfoContainer.findViewById(R.id.guestlist_container);  
		
		mEventInfoReminderItemscontainer = (LinearLayout)mEventInfoContainer.findViewById(R.id.reminder_items_container);  
		
		mEventInfoDescription = (ExpandableTextView)mEventInfoContainer.findViewById(R.id.event_info_description);  
			
		mEventInfoLowerActionbar = (DrawableBolderRelativeLayout)findViewById(R.id.event_info_lower_actionbar);
		mEventInfoLowerActionbar.setLineSide(DrawableBolderRelativeLayout.UPPER_LINE_SIDE);
		
		mEventInfoGuestlistContainer.setOnClickListener(mAttendeesLayoutClickListener);       
        
        mQueryAllDone = false;
        
        mCurrentViewingPage = EVENT_MAIN_PAGE;
	}
		
	public void inflateSubPages() {
		long makeSubPageTime = System.currentTimeMillis();
		
        mReminderPageView = (ReminderSelectPageView)mActivity.getLayoutInflater().inflate(R.layout.reminder_select_page_layout, null);
        mAttendeesInfoSwitchPageView = (AttendeesInfoSwitchPageView)mActivity.getLayoutInflater().inflate(R.layout.attendees_information_viewswitcher, null);    
        mAttendeesInfoSwitchPageView.setEventInfoView(this);
        mCalendarSelectPageView = (CalendarSelectPageView)mActivity.getLayoutInflater().inflate(R.layout.calendar_select_page_layout, null);        
        
        inflateAttendeesInfoMainPageView();
        long makedSubPageTime = System.currentTimeMillis();
        
        long makeDoneSubPageTime = makedSubPageTime - makeSubPageTime;
        //Log.i("tag", "EventInfoView:inflateSubPages=" + String.valueOf(makeDoneSubPageTime));
	}	
		
	//public Cursor mEventCursor;
    //public Cursor mAttendeesCursor;
    //private Cursor mCalendarsCursor;
    //private Cursor mRemindersCursor;
    
    public boolean mIsRepeating;
    private boolean mHasAlarm;
    private int mMaxReminders;
    
    public int mDefaultReminderMinutes;    
    public ArrayList<ReminderEntry> mReminders;
    public ArrayList<ReminderEntry> mOriginalReminders = new ArrayList<ReminderEntry>();
    public ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();
    private boolean mUserModifiedReminders = false;

    /**
     * Contents of the "minutes" spinner.  This has default values from the XML file, augmented
     * with any additional values that were already associated with the event.
     */
    public ArrayList<Integer> mReminderMinuteValues;
    public ArrayList<String> mReminderMinuteLabels;

    /**
     * Contents of the "methods" spinner.  The "values" list specifies the method constant
     * (e.g. {@link Reminders#METHOD_ALERT}) associated with the labels.  Any methods that
     * aren't allowed by the Calendar will be removed.
     */
    public ArrayList<Integer> mReminderMethodValues;
    public ArrayList<String> mReminderMethodLabels;
    
    private String mCalendarAllowedReminders;
    
    private boolean mIsBusyFreeCalendar;
    
    private static final String PERIOD_SPACE = ". ";
    
	/**
     * Initializes the event cursor, which is expected to point to the first
     * (and only) result from a query.
     * @return false if the cursor is empty, true otherwise
     */
    public boolean initEvent() {
    	// 다른 날짜가 선택되었을 경우 cursor를 clear해야 한다
    	/*if (mEventCursor != null)
    		mEventCursor.close();
    	
    	mEventCursor = Utils.matrixCursorFromCursor(cursor);
    	
        if ((mEventCursor == null) || (mEventCursor.getCount() == 0)) {
            return false;
        }
        
        mEventCursor.moveToFirst();*/
        mFragment.setEventId(mFragment.mModel.mId);//(mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ID));
        String rRule = mFragment.mModel.mRrule;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_RRULE);
        mIsRepeating = !TextUtils.isEmpty(rRule);
        // mHasAlarm will be true if it was saved in the event already, or if
        // we've explicitly been provided reminders (e.g. during rotation).
        // mReminders는 EventInfoActivity가 launch 되었을 때는 null 값이다        
        mHasAlarm = mFragment.mModel.mHasAlarm;
        //mHasAlarm = (mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_HAS_ALARM) == 1)? true : (mReminders != null && mReminders.size() > 0);
        // 현재 이벤트가 수용할 수 있는 알림 개수이다
        mMaxReminders = mFragment.mModel.mCalendarMaxReminders;//mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_MAX_REMINDERS); // 이건 mModel이 커버 못함 ㅠㅠㅠ
        
        mCalendarAllowedReminders =  mFragment.mModel.mCalendarAllowedReminders;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_ALLOWED_REMINDERS);
        return true;
    }
    
    /*
    public void initCalendarCursor(Cursor cursor) {
    	mCalendarsCursor = Utils.matrixCursorFromCursor(cursor);
    }
    
    public String getCalendarAccountName() {
    	return mCalendarsCursor.getString(EventInfoLoader.CALENDARS_INDEX_ACCOUNT_NAME);
    }
    
    public String getCalendarAccountType() {
    	return mCalendarsCursor.getString(EventInfoLoader.CALENDARS_INDEX_ACCOUNT_TYPE);
    }
    
    public String getCalendarOwnerAccount() {
    	return mCalendarsCursor.getString(EventInfoLoader.CALENDARS_INDEX_OWNER_ACCOUNT);
    }
    
    public int getCalendarColor() {
    	return Utils.getDisplayColorFromColor(mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_CALENDAR_COLOR));
    }    
    
    public int getOriginalColor(int calendarColor) {
    	return mEventCursor.isNull(EventInfoLoader.EVENT_INDEX_EVENT_COLOR)? 
    			calendarColor : Utils.getDisplayColorFromColor(mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_EVENT_COLOR));
    }
    
    public long getCalendarID() {
    	return mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_CALENDAR_ID);
    }
    
    public String getCalendarDisplayName() {
    	return mCalendarsCursor.getString(EventInfoLoader.CALENDARS_INDEX_DISPLAY_NAME);
    }
    */
    
    public boolean isBusyFreeCalendar() {
    	return mIsBusyFreeCalendar;
    }
    
    public boolean hasAlarm() {
    	return mHasAlarm;
    }
    
    public boolean isRepeating() {
    	return mIsRepeating;
    }
    
    public boolean canModifyEvent() {
    	return mCanModifyEvent;
    }    
    
    public ArrayList<ReminderEntry> getReminders() {    	
    	return EventViewUtils
                .reminderItemsToReminderLayouts(mReminderViews, 
                		mReminderMinuteValues, mReminderMinuteLabels);
    }
    
    
    synchronized public void prepareReminders(String defaultReminderString) {    	
        // Nothing to do if we've already built these lists and 
    	// we aren't removing not allowed methods
        if (mReminderMinuteValues != null && mReminderMinuteLabels != null
                && mReminderMethodValues != null && mReminderMethodLabels != null
                && mCalendarAllowedReminders == null) {
            return;
        }
                
        
        mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);
        
        // Load the labels and corresponding numeric values for the minutes and methods lists
        // from the assets.  If we're switching calendars, we need to clear and re-populate the
        // lists (which may have elements added and removed based on calendar properties).  This
        // is mostly relevant for "methods", since we shouldn't have any "minutes" values in a
        // new event that aren't in the default set.
        Resources r = mActivity.getResources();
        mReminderMinuteValues = FastEventInfoFragment.loadIntegerArray(r, R.array.reminder_minutes_values);
        mReminderMinuteLabels = FastEventInfoFragment.loadStringArray(r, R.array.reminder_minutes_labels);
        // [1, 2, 3, 4] 로 설정됨
        mReminderMethodValues = FastEventInfoFragment.loadIntegerArray(r, R.array.reminder_methods_values);
        mReminderMethodLabels = FastEventInfoFragment.loadStringArray(r, R.array.reminder_methods_labels);

        // Remove any reminder methods that aren't allowed for this calendar.  If this is
        // a new event, mCalendarAllowedReminders may not be set the first time we're called.
        if (mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mCalendarAllowedReminders);
        }
        
        //if (mView != null) {
            //mView.invalidate();
        //invalidate(); // Why???
        //}
    }    
    
    // mTentativeUserSetResponse
    // mEditResponseHelper.getWhichEvents()
    // mAttendeeResponseFromIntent
    // mOriginalAttendeeResponse																									
	// mUserSetResponse
    public long mCalendarOwnerAttendeeId = EditEventHelper.ATTENDEE_ID_NONE;
    private int mNumOfAttendees = 0;
    
    ArrayList<Attendee> mAcceptedAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mDeclinedAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mTentativeAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mNoResponseAttendees = new ArrayList<Attendee>();
    
    private String mEventOrganizerDisplayName = "";
    private boolean mIsOrganizer = false;
    
    private String mCalendarOwnerAccount;
    
    @SuppressWarnings("fallthrough")
    public void initAttendees(CalendarEventModel model/*Cursor cursor*/) {
    	mNumOfAttendees = model.mAttendeesList.size();
    	
    	if (mNumOfAttendees > 0) {
    		 mAcceptedAttendees.clear();
             mDeclinedAttendees.clear();
             mTentativeAttendees.clear();
             mNoResponseAttendees.clear();
             
             for (Attendee attendee : model.mAttendeesList.values()) {
            	 int status = attendee.mStatus;
                 String name = attendee.mName;
                 String email = attendee.mEmail;
                 int relationShip = attendee.mRelationShip;
                 if (relationShip == Attendees.RELATIONSHIP_ORGANIZER) {

                     // Overwrites the one from Event table if available
                     if (!TextUtils.isEmpty(name)) {
                         mEventOrganizerDisplayName = name;
                         if (!mIsOrganizer) {
                         	String attached = "attached";
                         	String tag = (String)mEventInfoOrganizerContainer.getTag();
                         	if ( (tag == null) || (tag.compareTo(attached) != 0) ) {
                         		makeOrganizerContainer();
                         	}
                         	setVisibilityCommon(mEventInfoOrganizerContainer, View.VISIBLE);
                             setTextCommon(mOrganizer, mEventOrganizerDisplayName);
                         }
                     }
                 }
                 
                 if (mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE &&
                         mCalendarOwnerAccount.equalsIgnoreCase(email)) {
                     mCalendarOwnerAttendeeId = attendee.mID;//mAttendeesCursor.getInt(EventInfoLoader.ATTENDEES_INDEX_ID);
                     mFragment.mOriginalAttendeeResponse = attendee.mStatus;//mAttendeesCursor.getInt(EventInfoLoader.ATTENDEES_INDEX_STATUS);
                 } else {
                     String identity = null;
                     String idNamespace = null;

                     if (Utils.isJellybeanOrLater()) {
                         identity = attendee.mIdentity;//mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_IDENTITY);
                         idNamespace = attendee.mIdNamespace;//mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_ID_NAMESPACE);
                     }

                     // Don't show your own status in the list because:
                     //  1) it doesn't make sense for event without other guests.
                     //  2) there's a spinner for that for events with guests.
                     switch(status) {
                         case Attendees.ATTENDEE_STATUS_ACCEPTED:
                             mAcceptedAttendees.add(new Attendee(name, email,
                                     Attendees.ATTENDEE_STATUS_ACCEPTED, identity,
                                     idNamespace));
                             break;
                         case Attendees.ATTENDEE_STATUS_DECLINED:
                             mDeclinedAttendees.add(new Attendee(name, email,
                                     Attendees.ATTENDEE_STATUS_DECLINED, identity,
                                     idNamespace));
                             break;
                         case Attendees.ATTENDEE_STATUS_TENTATIVE:
                             mTentativeAttendees.add(new Attendee(name, email,
                                     Attendees.ATTENDEE_STATUS_TENTATIVE, identity,
                                     idNamespace));
                             break;
                         default:
                             mNoResponseAttendees.add(new Attendee(name, email,
                                     Attendees.ATTENDEE_STATUS_NONE, identity,
                                     idNamespace));
                     }
                 }
             }
             
             makeGuestlistContainer();
             updateAttendees();
    	}
    	else {
    		if (mLongAttendees != null)
    			mLongAttendees.clearAttendees();
    		
    		mEventInfoGuestlistContainer.setVisibility(View.GONE);
    	}
    	/*
    	mAttendeesCursor = Utils.matrixCursorFromCursor(cursor);
    	
        mFragment.mOriginalAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;   
        mCalendarOwnerAttendeeId = EditEventHelper.ATTENDEE_ID_NONE;
        mNumOfAttendees = 0;
        
        if (mAttendeesCursor != null) {
        mAttendeesList
            mNumOfAttendees = mAttendeesCursor.getCount();
            // mNumOfAttendees이 0명이면
            // Cursor.moveToFirst는 false를 바로 리턴한다
            if (mAttendeesCursor.moveToFirst()) {
                mAcceptedAttendees.clear();
                mDeclinedAttendees.clear();
                mTentativeAttendees.clear();
                mNoResponseAttendees.clear();

                do {
                    int status = mAttendeesCursor.getInt(EventInfoLoader.ATTENDEES_INDEX_STATUS);
                    String name = mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_NAME);
                    String email = mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_EMAIL);

                    if (mAttendeesCursor.getInt(EventInfoLoader.ATTENDEES_INDEX_RELATIONSHIP) ==
                            Attendees.RELATIONSHIP_ORGANIZER) {

                        // Overwrites the one from Event table if available
                        if (!TextUtils.isEmpty(name)) {
                            mEventOrganizerDisplayName = name;
                            if (!mIsOrganizer) {
                            	String attached = "attached";
                            	String tag = (String)mEventInfoOrganizerContainer.getTag();
                            	if ( (tag == null) || (tag.compareTo(attached) != 0) ) {
                            		makeOrganizerContainer();
                            	}
                            	setVisibilityCommon(mEventInfoOrganizerContainer, View.VISIBLE);
                                setTextCommon(mOrganizer, mEventOrganizerDisplayName);
                            }
                        }
                    }

                    if (mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE &&
                            mCalendarOwnerAccount.equalsIgnoreCase(email)) {
                        mCalendarOwnerAttendeeId = mAttendeesCursor.getInt(EventInfoLoader.ATTENDEES_INDEX_ID);
                        mFragment.mOriginalAttendeeResponse = mAttendeesCursor.getInt(EventInfoLoader.ATTENDEES_INDEX_STATUS);
                    } else {
                        String identity = null;
                        String idNamespace = null;

                        if (Utils.isJellybeanOrLater()) {
                            identity = mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_IDENTITY);
                            idNamespace = mAttendeesCursor.getString(EventInfoLoader.ATTENDEES_INDEX_ID_NAMESPACE);
                        }

                        // Don't show your own status in the list because:
                        //  1) it doesn't make sense for event without other guests.
                        //  2) there's a spinner for that for events with guests.
                        switch(status) {
                            case Attendees.ATTENDEE_STATUS_ACCEPTED:
                                mAcceptedAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_ACCEPTED, identity,
                                        idNamespace));
                                break;
                            case Attendees.ATTENDEE_STATUS_DECLINED:
                                mDeclinedAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_DECLINED, identity,
                                        idNamespace));
                                break;
                            case Attendees.ATTENDEE_STATUS_TENTATIVE:
                                mTentativeAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_TENTATIVE, identity,
                                        idNamespace));
                                break;
                            default:
                                mNoResponseAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_NONE, identity,
                                        idNamespace));
                        }
                    }
                } while (mAttendeesCursor.moveToNext());
                mAttendeesCursor.moveToFirst();
                
                makeGuestlistContainer();
                updateAttendees();
            }
            else {
            	if (mNumOfAttendees == 0) {
            		if (mLongAttendees != null)
            			mLongAttendees.clearAttendees();
            		mEventInfoGuestlistContainer.setVisibility(View.GONE);
            	}
            }
        }
        else {
        	// 아래 코드는 의미없다.
        	// 참석자가 0명이라도 mAttendeesCursor는 null이 아니다
        	mEventInfoGuestlistContainer.setVisibility(View.GONE);
        }
        */
    }    
    
    public void initReminders(CalendarEventModel model/*Cursor cursor*/) {
    	//if (cursor == null) { // 이런 상황이 발생할 수 있는가??? : 발생하였다
    		//mEventInfoReminderItemscontainer.setVisibility(View.GONE); 
        	//return;
    	//}
    	
    	//mRemindersCursor = Utils.matrixCursorFromCursor(cursor);
    	
        // Add reminders
        mOriginalReminders.clear();
        mUnsupportedReminders.clear();
        
        int remindersNumber = model.mReminders.size();
        if (remindersNumber > 0) {
        	for (int i=0; i<remindersNumber; i++) {
        		ReminderEntry obj = model.mReminders.get(i);
        		int minutes = obj.getMinutes();//mRemindersCursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
	            int method = obj.getMethod();//mRemindersCursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);
	
	            if (method != Reminders.METHOD_DEFAULT && !mReminderMethodValues.contains(method)) {
	                // Stash unsupported reminder types separately so we don't alter
	                // them in the UI
	                mUnsupportedReminders.add(ReminderEntry.valueOf(minutes, method));
	            } else {
	                mOriginalReminders.add(ReminderEntry.valueOf(minutes, method));
	            }
        	}
        }
        else {
        	mEventInfoReminderItemscontainer.setVisibility(View.GONE); 
        	return;
        }
        
        /*
        if (mRemindersCursor.moveToFirst())
        	do {	        
	            int minutes = mRemindersCursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
	            int method = mRemindersCursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);
	
	            if (method != Reminders.METHOD_DEFAULT && !mReminderMethodValues.contains(method)) {
	                // Stash unsupported reminder types separately so we don't alter
	                // them in the UI
	                mUnsupportedReminders.add(ReminderEntry.valueOf(minutes, method));
	            } else {
	                mOriginalReminders.add(ReminderEntry.valueOf(minutes, method));
	            }
	        
        	}while(mRemindersCursor.moveToNext());
        else { // 이런 상황이 발생할 수 있는가??? 
        	mEventInfoReminderItemscontainer.setVisibility(View.GONE); 
        	return;
        }
        */
        
        // Sort appropriately for display (by time, then type)
        Collections.sort(mOriginalReminders);

        if (mUserModifiedReminders) {
            // If the user has changed the list of reminders don't change what's
            // shown.
            return;
        }

        //LinearLayout parent = (LinearLayout) mScrollView.findViewById(R.id.reminder_items_container);
        //if (parent != null) {
            //parent.removeAllViews();
        //}
        if (mEventInfoReminderItemscontainer.getChildCount() != 0)
    		mEventInfoReminderItemscontainer.removeAllViews();
        //mEventInfoReminderItemscontainer.removeAllViews();
        
        if (mReminderViews != null) {
            mReminderViews.clear(); // 단지 clear만 하면 되는 것인가???
        }

        if (mHasAlarm) {
        	mEventInfoReminderItemscontainer.setVisibility(View.VISIBLE);
        	
            ArrayList<ReminderEntry> reminders;
            // If applicable, use reminders saved in the bundle.
            if (mReminders != null) {
                reminders = mReminders;
            } else {
                reminders = mOriginalReminders;
            }
            // Insert any minute values that aren't represented in the minutes list.
            for (ReminderEntry re : reminders) {
                EventViewUtils.addMinutesToList(
                        mActivity, mReminderMinuteValues, mReminderMinuteLabels, re.getMinutes());
            }
            // Create a UI element for each reminder.  We display all of the reminders we get
            // from the provider, even if the count exceeds the calendar maximum.  (Also, for
            // a new event, we won't have a maxReminders value available.)
            //for (ReminderEntry re : reminders) {
            int listSize = reminders.size();
            for (int index=0; index<listSize; index++) {
            	ReminderEntry re = reminders.get(index);
                EventViewUtils.addReminderLayout(mActivity, mEventInfoReminderItemscontainer, mReminderViews,
                                                 mReminderMinuteValues, mReminderMinuteLabels, 
                                                 mReminderMethodValues, mReminderMethodLabels, 
                                                 re, index,
                                                 Integer.MAX_VALUE, 
                                                 mReminderLayoutClickListener);
            }
            
            // 우리는 알림 추가를 지원하지 않을 것이다
            //EventViewUtils.updateAddReminderButton(mMainPageView, mReminderViews, mMaxReminders);
            // TODO show unsupported reminder types in some fashion.
        }
        else {
        	// 알림이 없으면 알림이 없다는 것을 알려야 하지 않을까???
        	// 필요 없다...edit event activity에서 지원하게 하면 된다!!!
        	mEventInfoReminderItemscontainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * Add a new reminder when the user hits the "add reminder" button.  We use the default
     * reminder time and method.
     */
    //private final ArrayList<LinearLayout> mReminderViews = new ArrayList<LinearLayout>(0); //+commented by intheeast
    private final ArrayList<RelativeLayout> mReminderViews = new ArrayList<RelativeLayout>(0); //+added by intheeast
    // 현재 우리는 알림 추가를 지원하지 않을 것이다 그러므로 아래 함수는 사용되지 않을 것이다 
    public void addReminder() {
        // TODO: when adding a new reminder, make it different from the
        // last one in the list (if any).
    	int index = mReminderViews.size();
        if (mDefaultReminderMinutes == SettingsFragment.NO_REMINDER) {
        	
            EventViewUtils.addReminderLayout(mActivity, mEventInfoReminderItemscontainer, mReminderViews,
            		                         mReminderMinuteValues, mReminderMinuteLabels, 
            		                         mReminderMethodValues, mReminderMethodLabels,
                                             ReminderEntry.valueOf(SettingsFragment.REMINDER_DEFAULT_TIME), index,
                                             mMaxReminders,
                                             mReminderLayoutClickListener);
        } else {
            EventViewUtils.addReminderLayout(mActivity, mEventInfoReminderItemscontainer, mReminderViews,
                                             mReminderMinuteValues, mReminderMinuteLabels, 
                                             mReminderMethodValues, mReminderMethodLabels, 
                                             ReminderEntry.valueOf(mDefaultReminderMinutes), index,
                                             mMaxReminders, 
                                             mReminderLayoutClickListener);
        }

        // 우리는 알림 추가를 지원하지 않을 것이다
        //EventViewUtils.updateAddReminderButton(mMainPageView, mReminderViews, mMaxReminders);
    }
    
    // Called when the FastEventInfoFragment is no longer started. 
    // This is generally tied to EventInfoActivity.onStop of the containing EventInfoActivity's lifecycle.
    public boolean saveReminders() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(3);

        // Read reminders from UI
        mReminders = EventViewUtils.reminderItemsToReminderLayouts(mReminderViews,/////////////////////////////////////////////////////////
                                                                   mReminderMinuteValues, 
                                                                   mReminderMinuteLabels);
        mOriginalReminders.addAll(mUnsupportedReminders);
        Collections.sort(mOriginalReminders);
        mReminders.addAll(mUnsupportedReminders);
        Collections.sort(mReminders);

        // Check if there are any changes in the reminder
        boolean changed = EditEventHelper.saveReminders(ops, mFragment.getEventId(), mReminders,
                mOriginalReminders, false /* no force save */);

        if (!changed) {
            return false;
        }

        // save new reminders
        AsyncQueryService service = new AsyncQueryService(mActivity);
        service.startBatch(0, null, Calendars.CONTENT_URI.getAuthority(), ops, 0);
        mOriginalReminders = mReminders;
        // Update the "hasAlarm" field for the event
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mFragment.getEventId());
        int len = mReminders.size();
        boolean hasAlarm = len > 0;
        if (hasAlarm != mHasAlarm) {
            ContentValues values = new ContentValues();
            values.put(Events.HAS_ALARM, hasAlarm ? 1 : 0);
            service.startUpdate(0, null, uri, values, null, null, 0);
        }
        return true;
    }
    
    public void afterQueryAllDone() {    	
    	mQueryAllDone = true;    	
    }
    
    public void sendAccessibilityEvent() {
        AccessibilityManager am =
            (AccessibilityManager) mActivity.getSystemService(Service.ACCESSIBILITY_SERVICE);
        
        if (!am.isEnabled()) {
            return;
        }

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(FastEventInfoFragment.class.getName());
        event.setPackageName(mActivity.getPackageName());
        List<CharSequence> text = event.getText();

        addFieldToAccessibilityEvent(text, mTitle, null);
        addFieldToAccessibilityEvent(text, mWhenDateTime, null);
        addFieldToAccessibilityEvent(text, mWhere, null);
        addFieldToAccessibilityEvent(text, null, mEventInfoDescription);

        if (mFragment.getEventInfoViewLowerActionBarResponseMenuVisibility() == View.VISIBLE) {        	
            int id = mFragment.findCurrentSelectedButtonIdForResponse();
            
            if (id != View.NO_ID) {
            	text.add(mActivity.getResources().getString(R.string.view_event_response_label));
                String responseStr = null;
                if (id == RESPONSE_YES_ID) {
                	responseStr = mActivity.getResources().getString(R.string.response_yes) + PERIOD_SPACE;
                } else if (id == RESPONSE_MAYBE_ID) {
                	responseStr = mActivity.getResources().getString(R.string.response_maybe) + PERIOD_SPACE;
                } else if (id == RESPONSE_NO_ID) {
                	responseStr = mActivity.getResources().getString(R.string.response_no) + PERIOD_SPACE;
                } 
                
                text.add(responseStr);
            }
        }

        am.sendAccessibilityEvent(event);
    }   
    
    boolean mAddedEventHeadLine = false;
	public void updateEventHeadLineAndMemo(CalendarEventModel model) {
        //if (mEventCursor == null) {
            //return;
        //}		
        
        if (!mAddedEventHeadLine)
        	makeEventHeadLine();        
        
        String eventName = model.mTitle;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_TITLE);
        if (eventName == null || eventName.length() == 0) {
            eventName = mActivity.getString(R.string.no_title_label);
        }
        
        // 3rd parties might not have specified the start/end time when firing the
        // Events.CONTENT_URI intent.  Update these with values read from the db.        
        if (mFragment.getStartMillis() == 0 && mFragment.getEndMillis() == 0) {
        	mFragment.setStartMillis(model.mStart); //(mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_DTSTART));
        	mFragment.setEndMillis(model.mEnd);//(mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_DTEND));
            if (mFragment.getEndMillis() == 0) {
                String duration = model.mDuration;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_DURATION);
                if (!TextUtils.isEmpty(duration)) {
                    try {
                        Duration d = new Duration();
                        d.parse(duration);
                        long endMillis = mFragment.getStartMillis() + d.getMillis();
                        if (endMillis >= mFragment.getStartMillis()) {
                        	mFragment.setEndMillis(endMillis);
                        } else {
                            Log.i("tag", "Invalid duration string: " + duration);
                        }
                    } catch (DateException e) {
                        Log.i("tag", "Error parsing duration string " + duration, e);
                    }
                }
                if (mFragment.getEndMillis() == 0) {
                	mFragment.setEndMillis(mFragment.getStartMillis());
                }
            }
        }
        
        mFragment.setAllDay(model.mAllDay);//(mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ALL_DAY) != 0);
        String location = model.mLocation;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_LOCATION);
        String description = model.mDescription;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_DESCRIPTION);
        String rRule = model.mRrule;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_RRULE);
        String eventTimezone = model.mTimezone;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_TIMEZONE);
        
        // What        
        if (eventName != null) {
            setTextCommon(mTitle, eventName); //setTextCommon(R.id.title, eventName);
        }
        else {
        	// 이런 경우는 있을 수 없도록 해야 하는데...다른 캘린더에서 제목없이 생성된 경우? 어떻게 하나...단지 제목없음으로 드로잉해야 하는가???
        }
        
        // When
        // Set the date and repeats (if any)        
        String localTimezone = Utils.getTimeZone(mActivity, mFragment.mTZUpdater);
        
        Resources resources = mContext.getResources();
        String displayedDatetime = Utils.getDisplayedDatetime(mFragment.getStartMillis(), mFragment.getEndMillis(),
                System.currentTimeMillis(), localTimezone, mFragment.getAllDay(), mContext);
        
        String displayedTimezone = null;
        if (!mFragment.getAllDay()) {
            displayedTimezone = Utils.getDisplayedTimezone(mFragment.getStartMillis(), localTimezone,
                    eventTimezone);
        }
        
        // Display the When Date Time.  Make the timezone (if any) transparent.        
        if (displayedTimezone == null) {
            setTextCommon(mWhenDateTime, displayedDatetime);//setTextCommon(R.id.when_datetime, displayedDatetime);
        } else {
            int timezoneIndex = displayedDatetime.length();
            displayedDatetime += "  " + displayedTimezone;
            SpannableStringBuilder sb = new SpannableStringBuilder(displayedDatetime);
            ForegroundColorSpan transparentColorSpan = new ForegroundColorSpan(
                    resources.getColor(R.color.event_info_headline_transparent_color));
            sb.setSpan(transparentColorSpan, timezoneIndex, displayedDatetime.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            setTextCommon(mWhenDateTime, sb); //setTextCommon(R.id.when_datetime, sb);
        }
        
        // Display the When Repeat string (if any)        
        String repeatString = null;
        if (!TextUtils.isEmpty(rRule)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(rRule);
            Calendar date = GregorianCalendar.getInstance(TimeZone.getTimeZone(localTimezone));//Time date = new Time(localTimezone);
            date.setTimeInMillis(mFragment.getStartMillis());//date.set(mFragment.getStartMillis());
            if (mFragment.getAllDay()) {
                date = ETime.switchTimezone(date, TimeZone.getTimeZone(ETime.TIMEZONE_UTC));//date.timezone = Time.TIMEZONE_UTC;
            }
            eventRecurrence.setStartDate(date);
            repeatString = EventRecurrenceFormatter.getRepeatString(mContext, resources,
                    eventRecurrence, true);
        }
        
        if (repeatString != null) {
        	setTextCommon(mWhenRepeat, repeatString); //setTextCommon(R.id.when_repeat, repeatString);
        	//if(mWhenRepeat.getVisibility() != View.VISIBLE);
        		mWhenRepeat.setVisibility(View.VISIBLE); //findViewById(R.id.).setVisibility(View.VISIBLE);        	
        }
        else {        	
        	mWhenRepeat.setVisibility(View.GONE); //INVISIBLE도 고려해야 한다
        }
        
        // Organizer view is setup in the updateCalendar method

        // Where        
        if (location == null || location.trim().length() == 0) {
        	mWhere.setVisibility(View.GONE); //setVisibilityCommon(R.id.where, View.GONE);        	
        } else {
        	//if (mWhere.getVisibility() != View.VISIBLE)
        		mWhere.setVisibility(View.VISIBLE);
        	
            final TextView textView = mWhere;
            if (textView != null) {
                textView.setAutoLinkMask(0);
                //String trimContext = location.trim();
                textView.setText(location.trim());
                // 링크는 enter animation이 종료된 후 설정하자
                /*
                try {
                    textView.setText(Utils.extendedLinkify(textView.getText().toString(), true));

                    // Linkify.addLinks() sets the TextView movement method if it finds any links.
                    // We must do the same here, in case linkify by itself did not find any.
                    // (This is cloned from Linkify.addLinkMovementMethod().)
                    MovementMethod mm = textView.getMovementMethod();
                    if ((mm == null) || !(mm instanceof LinkMovementMethod)) {
                        if (textView.getLinksClickable()) {
                            textView.setMovementMethod(LinkMovementMethod.getInstance());
                        }
                    }
                } catch (Exception ex) {
                    // unexpected
                    Log.i("tag", "Linkification failed", ex);
                }

                textView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            return v.onTouchEvent(event);
                        } catch (ActivityNotFoundException e) {
                            // ignore
                            return true;
                        }
                    }
                });
                */
            }
        }        
       
        if (!mAddedEventHeadLine) {
        	mEventInfoHeadline.addView(mTitle);
            mEventInfoHeadline.addView(mWhere);
            mEventInfoHeadline.addView(mWhenDateTime);
            mEventInfoHeadline.addView(mWhenRepeat);
            mAddedEventHeadLine = true;
        }
        
        // Description
        if (description != null && description.length() != 0) {
        	if (mEventInfoDescription.getVisibility() != View.VISIBLE)
        		mEventInfoDescription.setVisibility(View.VISIBLE);
        	mEventInfoDescription.setText(description);//////////////////////////////////////////////////////////////////////////////////////
        }      
        else {
        	mEventInfoDescription.setVisibility(View.GONE);
        }
    }
	
	private boolean mOwnerCanRespond = false;
	private String mSyncAccountName;	
	private boolean mHasAttendeeData = false;
    private String mEventOrganizerEmail;
    private boolean mCanModifyEvent;
	public void updateCalendar(CalendarEventModel model) {
        mCalendarOwnerAccount = "";
        
        //if (mCalendarsCursor != null && mEventCursor != null) {
            //mCalendarsCursor.moveToFirst();
            String tempAccount = model.mCalendarOwnerAccount;//mCalendarsCursor.getString(EventInfoLoader.CALENDARS_INDEX_OWNER_ACCOUNT);
            mCalendarOwnerAccount = (tempAccount == null) ? "" : tempAccount;
            mOwnerCanRespond = model.mOrganizerCanRespond;//mCalendarsCursor.getInt(EventInfoLoader.CALENDARS_INDEX_OWNER_CAN_RESPOND) != 0;
            mSyncAccountName = model.mCalendarAccountName;//mCalendarsCursor.getString(EventInfoLoader.CALENDARS_INDEX_ACCOUNT_NAME);
                        
        	if (model.mCalendarName != null) {            		
		        makeCalendarContainer(model);
		        setVisibilityCommon(mEventInfoCalendarContainer, View.VISIBLE);
		        setTextCommon(mCalendar, model.mCalendarName);
		        //mEventInfoView.setCalendarColorIcon();
		        setCalendarItemLayoutClickListener();
        	}
        	else {
        		setVisibilityCommon(mEventInfoCalendarContainer, View.GONE);	            
        	}
        	
        	/*
        	mFragment.executeStartQuery(FastEventInfoFragment.TOKEN_QUERY_VISIBLE_CALENDARS, null, Calendars.CONTENT_URI,
        			EventInfoLoader.CALENDARS_PROJECTION, EventInfoLoader.CALENDARS_VISIBLE_WHERE, new String[] {"1"}, null);*/            
            
            mEventOrganizerEmail = model.mOrganizer;//mEventCursor.getString(EventInfoLoader.EVENT_INDEX_ORGANIZER);
            mIsOrganizer = mCalendarOwnerAccount.equalsIgnoreCase(mEventOrganizerEmail);

            if (!TextUtils.isEmpty(mEventOrganizerEmail) &&
                    !mEventOrganizerEmail.endsWith(Utils.MACHINE_GENERATED_ADDRESS)) {
                mEventOrganizerDisplayName = mEventOrganizerEmail;
            }           
            
            if (!mIsOrganizer && !TextUtils.isEmpty(mEventOrganizerDisplayName)) {
            	makeOrganizerContainer();
                setTextCommon(mOrganizer, mEventOrganizerDisplayName); //setTextCommon(R.id.organizer, mEventOrganizerDisplayName);
                setVisibilityCommon(mEventInfoOrganizerContainer, View.VISIBLE);
            } else {
                setVisibilityCommon(mEventInfoOrganizerContainer, View.GONE);
            }            
            
            mHasAttendeeData = model.mHasAttendeeData;//mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
            mCanModifyCalendar = model.mRawAccessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR;;//mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ACCESS_LEVEL) >= Calendars.CAL_ACCESS_CONTRIBUTOR;
                   
            // TODO add "|| guestCanModify" after b/1299071 is fixed
            mCanModifyEvent = mCanModifyCalendar && mIsOrganizer;
            mIsBusyFreeCalendar = model.mRawAccessLevel == Calendars.CAL_ACCESS_FREEBUSY;//mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ACCESS_LEVEL) == Calendars.CAL_ACCESS_FREEBUSY;

            if (!mIsBusyFreeCalendar) {            	
            }            
            //View button;
            if (mCanModifyCalendar) {            	
            }            
            if (mCanModifyEvent) {            	
            }            
            
        //} else {
            //setVisibilityCommon(view, R.id.calendar, View.GONE); //R.id.calendar가 무엇인지???
            //mFragment.sendAccessibilityEventIfQueryDone(FastEventInfoFragment.TOKEN_QUERY_DUPLICATE_CALENDARS);
        //}
    }
	
	boolean mRegisteredCalendarItemLayoutClickListener = false;
	public void setCalendarItemLayoutClickListener() {
		//R.id.calendar_container
		//View view = findViewById(R.id.calendar_container);////////////////////////////////////null 값 발생...ㅡㅡ;
		if (!mRegisteredCalendarItemLayoutClickListener) {
			mEventInfoCalendarContainer.setOnClickListener(mCalendarLayoutClickListener);
			mRegisteredCalendarItemLayoutClickListener = true;
		}
	}	
	
	
	
	public void updateAttendees() {
        if (mAcceptedAttendees.size() + mDeclinedAttendees.size() +
        		mTentativeAttendees.size() + mNoResponseAttendees.size() > 0) {
        	        	
            mLongAttendees.clearAttendees();
            
            (mLongAttendees).addAttendees(mAcceptedAttendees);
            (mLongAttendees).addAttendees(mDeclinedAttendees);
            (mLongAttendees).addAttendees(mTentativeAttendees);
            (mLongAttendees).addAttendees(mNoResponseAttendees);            
            
            mEventInfoGuestlistContainer.setVisibility(View.VISIBLE);
            // 실제 guest detail icon을 선택하였을 때 인플레이트 하자!!!
            //:자 아래 코드를 animation이 끝나면 할까???
            //mAttendeesInfoSwitchPageView.inflateMainPage();  ///////////////////////////굳이 여기서 할 필요가 있는가?                  
        } else {
        	mLongAttendees.clearAttendees();
        	mEventInfoGuestlistContainer.setVisibility(View.GONE);                      
        }        
    }	

    public void inflateAttendeesInfoMainPageView() {
    	mAttendeesInfoSwitchPageView.inflateMainPage();
    }
    
    /**
     * Returns true if there is at least 1 attendee that is not the viewer.
     */
    public boolean hasEmailableAttendees() {
        for (Attendee attendee : mAcceptedAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        for (Attendee attendee : mTentativeAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        for (Attendee attendee : mNoResponseAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        for (Attendee attendee : mDeclinedAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasEmailableOrganizer() {
        return mEventOrganizerEmail != null &&
                Utils.isEmailableFrom(mEventOrganizerEmail, mSyncAccountName);
    }  
    
        
    
    public void setTextCommon(TextView textView, CharSequence text) {
        //TextView textView = (TextView) findViewById(id);
        if (textView == null)
            return;
        textView.setText(text);
    }

    
    
    public void setVisibilityCommon(View v, int visibility) {        
        if (v != null) {
            v.setVisibility(visibility);
        }
        return;
    }

    
	/**
     * Email all the attendees of the event, except for the viewer (so as to not email
     * himself) and resources like conference rooms.
     */
    private void emailAttendees() {
        Intent i = new Intent(mActivity, QuickResponseActivity.class);
        i.putExtra(QuickResponseActivity.EXTRA_EVENT_ID, mFragment.getEventId());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.startActivity(i);
    }    
    
    public void displayEventNotFound() {
        //mErrorMsgView.setVisibility(View.VISIBLE);
        mScrollView.setVisibility(View.GONE);
        //mLoadingMsgView.setVisibility(View.GONE);
    }
    
    public boolean canModifyCalendar() {
    	return mCanModifyCalendar;
    }
    
    public ScrollView getScrollView() {
    	return mScrollView;
    }
    
    private void addFieldToAccessibilityEvent(List<CharSequence> text, TextView tv,
            ExpandableTextView etv) {
        CharSequence cs;
        if (tv != null) {
            cs = tv.getText();
        } else if (etv != null) {
            cs = etv.getText();
        } else {
            return;
        }

        if (!TextUtils.isEmpty(cs)) {
            cs = cs.toString().trim();
            if (cs.length() > 0) {
                text.add(cs);
                text.add(PERIOD_SPACE);
            }
        }
    }    
    
    public void closeCursors() {
    	/*if (mEventCursor != null) {
            mEventCursor.close();
        }
        if (mCalendarsCursor != null) {
            mCalendarsCursor.close();
        }
        if (mAttendeesCursor != null) {
            mAttendeesCursor.close();
        }*/
    }
    
    
    public EventInfoViewLowerActionBarStyle getEventInfoViewLowerActionBarStyle() {
    	int menuStyle = EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE;
    	if (!mIsOrganizer) { // Calendars.OWNER_ACCOUNT[mCalendarOwnerAccount]와 Events.ORGANIZER[mEventOrganizerEmail]가 다른 경우
    		menuStyle = EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE;
    	}
    	else if (mIsOrganizer && mCanModifyCalendar &&  mCanModifyEvent) {
    		menuStyle = EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE;
    	}
    	else if (!mIsOrganizer && !mCanModifyCalendar && !mCanModifyEvent) {
    		menuStyle = EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE;
    	}
    	else {
    		// 어떤 경우인가?????????????????????????????????????
    	}
    	    	
    	EventInfoViewLowerActionBarStyle eventModificationMenuStyle = new EventInfoViewLowerActionBarStyle(menuStyle);  
    	return eventModificationMenuStyle;    	
    }
    
    public static final int EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE = 0;
    public static final int EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE = 1;
    public static final int EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE = 2;
    public static class EventInfoViewLowerActionBarStyle {
    	public int mStyle;
    	
    	public EventInfoViewLowerActionBarStyle() {
    		
    	}
    	
    	public EventInfoViewLowerActionBarStyle(int style) {
    		mStyle = style;
    	}
    }
	
	TextView mPageTitleView = null;
	TextView mPreViewTextOfMainPage = null;
	TextView mMainbackTextOfSubPage = null;	
	TextView mActionBarPreviousTitleText = null;
	TextView mActionBarEditText = null;	
	
	public static final float SWITCH_SUB_PAGE_VELOCITY = 2200 * 1.25f;
    public static final float SWITCH_MAIN_PAGE_VELOCITY = 2200 * 1.5f;
    //final float SWITCH_MAIN_PAGE_IN_FROMXVALUE = -1.0f;
    final float SWITCH_MAIN_PAGE_IN_FROMXVALUE = -0.5f;
    final float SWITCH_MAIN_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_TOXVALUE = 1.0f;
	
    final float SWITCH_SUB_PAGE_IN_FROMXVALUE = 1.0f;
    final float SWITCH_SUB_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_TOXVALUE = -0.4f;
    //final float SWITCH_SUB_PAGE_OUT_TOXVALUE = -1.0f;
	
    private ScrollInterpolator mHScrollInterpolator;
    float mViewSwitchingDelta;
    
    public void settingViewSwitching(boolean switchSubView, int subViewID) {
    	float inFromXValue = 0;
    	float inToXValue = 0;
    	float outFromXValue = 0;
    	float outToXValue = 0;
    	float velocity = 0;    	
    	
    	if (!switchSubView) {
	    	inFromXValue = SWITCH_MAIN_PAGE_IN_FROMXVALUE;
	    	inToXValue = SWITCH_MAIN_PAGE_TO_FROMXVALUE;
	    	outFromXValue = SWITCH_MAIN_PAGE_OUT_FROMXVALUE;
	    	outToXValue = SWITCH_MAIN_PAGE_OUT_TOXVALUE;
	    	velocity = SWITCH_MAIN_PAGE_VELOCITY;
	    	//velocity = 1000;
    	}
    	else {
    		inFromXValue = SWITCH_SUB_PAGE_IN_FROMXVALUE;
	    	inToXValue = SWITCH_SUB_PAGE_TO_FROMXVALUE;
	    	outFromXValue = SWITCH_SUB_PAGE_OUT_FROMXVALUE;
	    	outToXValue = SWITCH_SUB_PAGE_OUT_TOXVALUE;
	    	velocity = SWITCH_SUB_PAGE_VELOCITY;
    	}
    	
    	TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        
        long duration = calculateDuration(mViewSwitchingDelta, mViewWidth, velocity);
        inAnimation.setDuration(duration);
        inAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setDuration(duration);        
        
        mEventInfoViewsSwitcher.setInAnimation(inAnimation);
        mEventInfoViewsSwitcher.setOutAnimation(outAnimation);
        
        if (!switchSubView) {
        	mEventInfoViewsSwitcher.showPrevious();
    	
    		switch(subViewID) {    		
    		
    		case EVENT_REMINDER_PAGE:
    			mEventInfoViewsSwitcher.removeView(mReminderPageView);
    			break;    		
    		case EVENT_CALENDAR_PAGE:
    			mEventInfoViewsSwitcher.removeView(mCalendarSelectPageView);
    			break;
    		case EVENT_ATTENDEE_PAGE:    			
    			mEventInfoViewsSwitcher.removeView(mAttendeesInfoSwitchPageView);
    			break;     		
    		default:
    			break;
    		}    		
    	}
        else {
        	switch(subViewID) {    		
    		case EVENT_REMINDER_PAGE:
    			mEventInfoViewsSwitcher.addView(mReminderPageView);
    			break;    		
    		case EVENT_CALENDAR_PAGE:
    			mEventInfoViewsSwitcher.addView(mCalendarSelectPageView);
    			break;
    		case EVENT_ATTENDEE_PAGE:    			
    			mEventInfoViewsSwitcher.addView(mAttendeesInfoSwitchPageView);		
    			break;    		
    		default:
    			break;
    		}    		
        	        	
        	mEventInfoViewsSwitcher.showNext();        	
        }
    }
    
    private void cancelAnimation() {
        Animation in = mEventInfoViewsSwitcher.getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = mEventInfoViewsSwitcher.getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
    }
    
    private float mAnimationDistance = 0;
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
        //float f1 = MINIMUM_SNAP_VELOCITY * 2;
        velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);
        velocity = Math.max(1000, velocity);

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
	
    
    
    private final static int RESPONSE_YES_ID = 1;
    private final static int RESPONSE_MAYBE_ID = 2;
    private final static int RESPONSE_NO_ID = 3;
    
    public int findButtonIdForResponse(int response) {
        int buttonId;
        switch (response) {
            case Attendees.ATTENDEE_STATUS_ACCEPTED:
                buttonId = RESPONSE_YES_ID;
                break;
            case Attendees.ATTENDEE_STATUS_TENTATIVE:
                buttonId = RESPONSE_MAYBE_ID;
                break;
            case Attendees.ATTENDEE_STATUS_DECLINED:
                buttonId = RESPONSE_NO_ID;
                break;
            default:
                buttonId = -1;
        }
        return buttonId;
    }    
    
    public void checkResponseContainer(int response) {      
            	
    	if (mSelectedResponseValue == response)
    		return;
    	
    	// none selected 상태로 만든다
    	int prvValue = mSelectedResponseValue;
    	if (prvValue != Attendees.ATTENDEE_STATUS_NONE) {
	    	int prvButtonId = this.findButtonIdForResponse(prvValue);
	    	if (prvButtonId == RESPONSE_YES_ID) {
	        	mResponse_yes.setTypeface(null, Typeface.NORMAL);
	        	mResponse_yes.setTextColor(Color.RED);        	
				mResponse_yes.setBackground(null);
	        } else if (prvButtonId == RESPONSE_MAYBE_ID) {
	        	mResponse_maybe.setTypeface(null, Typeface.NORMAL);
	        	mResponse_maybe.setTextColor(Color.RED);        	
	        	mResponse_maybe.setBackground(null);
	        } else if (prvButtonId == RESPONSE_NO_ID) {
	        	mResponse_no.setTypeface(null, Typeface.NORMAL);
	        	mResponse_no.setTextColor(Color.RED);        	
	        	mResponse_no.setBackground(null);
	        } else {
	            // -1???
	        }   
    	}
    	
    	Log.i("tag", "checkResponseContainer");
    	
    	mSelectedResponseValue = response;  
    	
        int buttonId = findButtonIdForResponse(mSelectedResponseValue);
        if (buttonId == RESPONSE_YES_ID) {        	
        	mResponse_yes.setTypeface(null, Typeface.BOLD);
        	mResponse_yes.setBackground(mResponseYesBackground);
        	mResponse_yes.setTextColor(Color.WHITE);        	
			
        } else if (buttonId == RESPONSE_MAYBE_ID) {        	
        	mResponse_maybe.setTypeface(null, Typeface.BOLD);
        	mResponse_maybe.setBackground(mResponseMaybeBackground);
        	mResponse_maybe.setTextColor(Color.WHITE);        	
        	
        } else if (buttonId == RESPONSE_NO_ID) {        	
        	mResponse_no.setTypeface(null, Typeface.BOLD);
        	mResponse_no.setBackground(mResponseNoBackground);
        	mResponse_no.setTextColor(Color.WHITE);        	
        	
        } else {
            // -1???
        }          
    }
    
    public static int getResponseFromButtonId(int buttonId) {
        int response;
        if (buttonId == RESPONSE_YES_ID) {
            response = Attendees.ATTENDEE_STATUS_ACCEPTED;
        } else if (buttonId == RESPONSE_MAYBE_ID) {
            response = Attendees.ATTENDEE_STATUS_TENTATIVE;
        } else if (buttonId == RESPONSE_NO_ID) {
            response = Attendees.ATTENDEE_STATUS_DECLINED;
        } else {
            response = Attendees.ATTENDEE_STATUS_NONE;
        }
        return response;
    }   
    
    public TextView makeEventHeadLineTextView(int width, int height, 
    		int leftMargin, int topMargin, int rightMargin, int bottomMargin,
    		boolean singleLine, 
    		int textApperance, int typeFace, 
    		int textColor,
    		boolean ellipsize, TruncateAt where,
    		int textLinkColor) {
    	
    	TextView tv = new TextView(mContext);
    	
    	LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(width, height);
        llp.setMargins(leftMargin, topMargin, rightMargin, bottomMargin); 
        tv.setLayoutParams(llp);
        tv.setTextAppearance(mContext, textApperance);    
        tv.setSingleLine(singleLine);
        tv.setTypeface(null, typeFace);
        tv.setTextColor(textColor);
        if (ellipsize)
        	tv.setEllipsize(where);
        
        if (textLinkColor != -1) {
        	tv.setLinkTextColor(textLinkColor);
        	tv.setTextIsSelectable(true);
        }                 
        
    	return tv;
    }
    
    TextView mTitle = null;
    TextView mWhenDateTime = null;
    TextView mWhere = null;
    TextView mWhenRepeat = null;    
    public void makeEventHeadLine() {
    	if (mTitle != null && mWhenDateTime != null && mWhere != null && mWhenRepeat != null)
    		return;
    	
    	//int textColor = (int) mActivity.getResources().getDimension(R.color.event_info_headline_color); 
    	int textColor = getResources().getColor(R.color.event_info_headline_color); 		
		int topMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, mDisplayMetrics);		
		//float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18, mDisplayMetrics);
		//float textSPSize = 18;
    	mTitle = makeEventHeadLineTextView(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,    			
    			0, topMargin, 0, 0,
    			false, 
    			android.R.style.TextAppearance_Medium, Typeface.BOLD,
    			textColor,
    			false, TextUtils.TruncateAt.END,// ellipsize를 사용하지 않지만,,,사용하지 않을 때 값을 정의하기 힘들기 때문에...
    			-1);    	
    	
    	textColor = Color.rgb(255, 0, 0);
    	int textColorLink = (int) getResources().getColor(R.color.event_info_headline_link_color); 
    	topMargin = (int) (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mDisplayMetrics);
    	//textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, mDisplayMetrics);
    	//textSPSize = 14;
    	mWhere = makeEventHeadLineTextView(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
    			0, topMargin, 0, 0,
    			false, 
    			android.R.style.TextAppearance_Small, Typeface.NORMAL,
    			textColor,
    			true, TextUtils.TruncateAt.END,// ellipsize를 사용하지 않지만,,,사용하지 않을 때 값을 정의하기 힘들기 때문에...
    			textColorLink);    	
    	
    	textColor = (int) getResources().getColor(R.color.event_layout_value_transparent_color); 
		topMargin = (int) (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, mDisplayMetrics);	
		mWhenDateTime = makeEventHeadLineTextView(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				0, topMargin, 0, 0,
    			true, 
    			android.R.style.TextAppearance_Small, Typeface.NORMAL,
    			textColor,
    			false, TextUtils.TruncateAt.END,// ellipsize를 사용하지 않지만,,,사용하지 않을 때 값을 정의하기 힘들기 때문에...
    			-1);	
		
		topMargin = (int) (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mDisplayMetrics);
		int bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mDisplayMetrics);  
		mWhenRepeat = makeEventHeadLineTextView(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				0, topMargin, 0, bottomMargin,
    			true, 
    			android.R.style.TextAppearance_Small, Typeface.NORMAL,
    			textColor,
    			false, TextUtils.TruncateAt.END,// ellipsize를 사용하지 않지만,,,사용하지 않을 때 값을 정의하기 힘들기 때문에...
    			-1);   	
	}
    
    ImageView mCalendarDetail;
	TextView mCalendar;
	View mCalendarColorIcon;
	TextView mCalendarLabel;
	boolean AddedCalendar = false;
	@SuppressLint("ResourceType")
	public void makeCalendarContainer(CalendarEventModel model) {
		if (!AddedCalendar) {
			/*
			<ImageView
		    	android:id="@+id/calendar_detail"
		    	android:layout_width="wrap_content"
				android:layout_height="wrap_content"
				android:layout_marginRight="12dip"
		    	android:layout_alignParentRight="true"
			    android:layout_centerVertical="true"			
		        android:scaleType="centerInside"      
		        android:src="@drawable/ic_details"	                
		         />
	    	*/
			mCalendarDetail = new ImageView(mContext);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, mDisplayMetrics);
			params.setMargins(0, 0, rightMargin, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			params.addRule(RelativeLayout.CENTER_VERTICAL);
			mCalendarDetail.setLayoutParams(params );
			mCalendarDetail.setScaleType(ScaleType.CENTER_INSIDE);
			mCalendarDetail.setImageResource(R.drawable.ic_details);
			mCalendarDetail.setId(1);
			/*
		    <TextView			        
			    android:id="@+id/calendar_name"
			    android:layout_width="wrap_content"
		        android:layout_height="wrap_content"	           
		        android:layout_toLeftOf="@id/calendar_detail"
		        android:layout_centerVertical="true"
		        android:gravity="center"
		        android:textColor="@color/event_layout_value_transparent_color"
		        android:textSize="@dimen/eventLayoutItemTextSize"
		        android:singleLine="true"   		 
	    		android:textIsSelectable="false"    />
		    */
			mCalendar = new TextView(mContext);		
			RelativeLayout.LayoutParams calendarParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			calendarParams.addRule(RelativeLayout.LEFT_OF, mCalendarDetail.getId());
			calendarParams.addRule(RelativeLayout.CENTER_VERTICAL);		
			mCalendar.setLayoutParams(calendarParams);
			mCalendar.setGravity(Gravity.CENTER);
			mCalendar.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  
			int textColor = (int) mActivity.getResources().getColor(R.color.event_layout_value_transparent_color);		
			mCalendar.setTextColor(textColor);		
			mCalendar.setSingleLine(true);
			mCalendar.setTextIsSelectable(false);
			mCalendar.setId(2);		
			/*
		    <View 
		        android:id="@+id/calendar_color_icon"
		        android:layout_width="8dip"
		        android:layout_height="8dip"
		        android:layout_toLeftOf="@id/calendar_name"
		        android:layout_marginRight="4dip"
		        android:layout_centerVertical="true" />		        
			*/
			// setCalendarColorIcon을 누가 호출하는지를 체크해야 한다
			mCalendarColorIcon = new View(mContext);
			int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mDisplayMetrics);
			int height = width;
			RelativeLayout.LayoutParams calendarIconParams = new RelativeLayout.LayoutParams(width, height);
			rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mDisplayMetrics);
			calendarIconParams.setMargins(0, 0, rightMargin, 0);
			calendarIconParams.addRule(RelativeLayout.LEFT_OF, mCalendar.getId());
			calendarIconParams.addRule(RelativeLayout.CENTER_VERTICAL);
			mCalendarColorIcon.setLayoutParams(calendarIconParams);
			/////////////////////////////////////////////////////////////////////////
			ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
			Paint paint = shapeDrawable.getPaint();		
			// getCalendarColor는 mEventCursor.getInt(FastEventInfoFragment.EVENT_INDEX_CALENDAR_COLOR)을 리턴한다
			// mEventCursor를 close 하면 안된다
			paint.setColor(model.getCalendarColor()/*Utils.getDisplayColorFromColor(getCalendarColor())*/);
			mCalendarColorIcon.setBackground(shapeDrawable);		
			/*
			<TextView
			    android:id="@+id/calendar_label"
			    android:layout_width="wrap_content"
		        android:layout_height="wrap_content"	         
		        android:layout_alignParentLeft="true"
		        android:layout_centerVertical="true"
		        android:gravity="left|center" 			        
		        style="@style/eventLayoutItemLabelTextStyle"
		        android:singleLine="true"	        	        	   
		        android:text="@string/view_event_calendar_label" 
		        android:textIsSelectable="false"  />	
			 */
			mCalendarLabel = new TextView(mContext);		
			RelativeLayout.LayoutParams calendarLabelParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			calendarLabelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			calendarLabelParams.addRule(RelativeLayout.CENTER_VERTICAL);		
			mCalendarLabel.setLayoutParams(calendarLabelParams);
			mCalendarLabel.setGravity(Gravity.CENTER | Gravity.LEFT);
			mCalendarLabel.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);		
			textColor = (int) mActivity.getResources().getColor(R.color.event_layout_label_color);		
			mCalendarLabel.setTextColor(textColor);		
			mCalendarLabel.setSingleLine(true);
			mCalendarLabel.setText(R.string.view_event_calendar_label);
			mCalendarLabel.setTextIsSelectable(false);			
			
			mEventInfoCalendarContainer.addView(mCalendarDetail);
			mEventInfoCalendarContainer.addView(mCalendar);
			mEventInfoCalendarContainer.addView(mCalendarColorIcon);
			mEventInfoCalendarContainer.addView(mCalendarLabel);
			
			AddedCalendar = true;
		}
		else {
			ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
			Paint paint = shapeDrawable.getPaint();		
			// getCalendarColor는 mEventCursor.getInt(FastEventInfoFragment.EVENT_INDEX_CALENDAR_COLOR)을 리턴한다
			// mEventCursor를 close 하면 안된다
			paint.setColor(model.getCalendarColor()/*Utils.getDisplayColorFromColor(getCalendarColor())*/);
			mCalendarColorIcon.setBackground(shapeDrawable);
		}
	}
	
	ImageView mOrganizerDetail;
	TextView mOrganizer;
	TextView mOrganizerLabel;
	boolean mAddedEventOrganizer = false;
	@SuppressLint("ResourceType")
	public void makeOrganizerContainer() {
		if (!mAddedEventOrganizer) {
			/*
			<ImageView
			    android:id="@+id/organizer_detail"
			    android:layout_width="wrap_content"------------
				android:layout_height="wrap_content"-----------
				android:layout_marginRight="12dip"--------------
			    android:layout_alignParentRight="true"	------			    
			    android:layout_centerVertical="true"			
		        android:scaleType="centerInside" ---------------     
		        android:src="@drawable/ic_details"			                    
		        android:contentDescription="@string/cd" />
		   */
			mOrganizerDetail = new ImageView(mContext);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, mDisplayMetrics);
			params.setMargins(0, 0, rightMargin, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			params.addRule(RelativeLayout.CENTER_VERTICAL);
			//params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE); //anchor는 뭐냐???
			
			mOrganizerDetail.setLayoutParams(params );
			mOrganizerDetail.setScaleType(ScaleType.CENTER_INSIDE);
			mOrganizerDetail.setImageResource(R.drawable.ic_details);
			mOrganizerDetail.setId(1);
			/*
		    <TextView
			    android:id="@+id/organizer"	         
		        android:layout_width="wrap_content"
		        android:layout_height="wrap_content"     
		        android:layout_toLeftOf="@id/organizer_detail"
		        android:layout_centerVertical="true"
		        android:gravity="center" 	        
		        android:textColor="@color/event_layout_value_transparent_color"
		        android:textSize="@dimen/eventLayoutItemTextSize"
		        android:singleLine="true"	
		        android:textIsSelectable="false" />			         
			*/
			mOrganizer = new TextView(mContext);		
			RelativeLayout.LayoutParams organizerTVParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			organizerTVParams.addRule(RelativeLayout.LEFT_OF, mOrganizerDetail.getId());
			organizerTVParams.addRule(RelativeLayout.CENTER_VERTICAL);		
			mOrganizer.setLayoutParams(organizerTVParams);
			mOrganizer.setGravity(Gravity.CENTER);		
			mOrganizer.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  		
			int textColor = (int) getResources().getColor(R.color.event_layout_value_transparent_color);		
			mOrganizer.setTextColor(textColor);		
			mOrganizer.setSingleLine(true);
			mOrganizer.setTextIsSelectable(false);
			/*
			<TextView
			    android:id="@+id/organizer_label"
		        android:layout_width="wrap_content"
		        android:layout_height="wrap_content"  
		        android:layout_alignParentLeft="true"			        
		        android:layout_centerVertical="true"
		        android:gravity="left|center" 	        
		        style="@style/eventLayoutItemLabelTextStyle"
		        android:singleLine="true"	        		
		        android:textIsSelectable="false" 	        	   
		        android:text="@string/event_info_organizer" />
			*/
			//R.style.eventLayoutItemLabelTextStyle
			mOrganizerLabel = new TextView(mContext);		
			RelativeLayout.LayoutParams organizerLabelTVParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			organizerLabelTVParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			organizerLabelTVParams.addRule(RelativeLayout.CENTER_VERTICAL);		
			mOrganizerLabel.setLayoutParams(organizerLabelTVParams);
			mOrganizerLabel.setGravity(Gravity.CENTER | Gravity.LEFT);
			mOrganizerLabel.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  
			textColor = (int) mActivity.getResources().getColor(R.color.event_layout_label_color);		
			mOrganizerLabel.setTextColor(textColor);		
			mOrganizerLabel.setSingleLine(true);
			mOrganizerLabel.setTextIsSelectable(false);
			mOrganizerLabel.setText(R.string.event_info_organizer);		
			
			mEventInfoOrganizerContainer.addView(mOrganizerDetail);
			mEventInfoOrganizerContainer.addView(mOrganizer);
			mEventInfoOrganizerContainer.addView(mOrganizerLabel);
			
			String tag = "attached";
			mEventInfoOrganizerContainer.setTag(tag);
			
			mAddedEventOrganizer = true;
		}
		
	}
	
	ImageView mGuestlistDetail;
	boolean mAddedGuestlist = false;
	public void makeGuestlistContainer() {
		if (!mAddedGuestlist) {			
			mLongAttendees = new AttendeesView(mContext);	
			RelativeLayout.LayoutParams attendeesParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			int top = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mDisplayMetrics); // top이 bottom보다 4가 적은 이유는 첫번째 컨스트럭터 디바이더의 marginTop이 4이기 때문이다
			                                                                                           // 그리고 item도 marginTop만 적용하기 때문에 paddingBottom은 top보다 4가 커야 한다
			int bottom = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, mDisplayMetrics);		
			attendeesParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			mLongAttendees.setLayoutParams(attendeesParams);
			mLongAttendees.setPadding(0, top, 0, bottom);
			mLongAttendees.setOrientation(LinearLayout.VERTICAL);
		
			
			mGuestlistDetail = new ImageView(mContext);
			RelativeLayout.LayoutParams detailParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, mDisplayMetrics);
			detailParams.setMargins(0, 0, rightMargin, 0);
			detailParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			detailParams.addRule(RelativeLayout.CENTER_VERTICAL);
			mGuestlistDetail.setLayoutParams(detailParams);
			mGuestlistDetail.setScaleType(ScaleType.CENTER_INSIDE);
			mGuestlistDetail.setImageResource(R.drawable.ic_details);
			
			mEventInfoGuestlistContainer.addView(mLongAttendees);
			mEventInfoGuestlistContainer.addView(mGuestlistDetail);
			mAddedGuestlist = true;
		}		
	}
	
	boolean mMadeResponseMenu = false;
    public void makeResponseMenu() {
    	
    	if (!mMadeResponseMenu) {			
    		mEventInfoLowerActionbar.setVisibility(View.VISIBLE);
    		mEventInfoLowerActionbar.setWillNotDraw(false);			
			
			/*
			<TextView		   	    
			    android:id="@+id/response_yes" 
			    android:layout_alignParentLeft="true"	         
			    android:layout_centerVertical="true"		    
			    android:layout_width="wrap_content"
			    android:layout_height="wrap_content"  			    	    	    
			    android:layout_marginLeft="24dip"
			    android:gravity="center" 
			    android:onClick="onClick"
			    android:clickable="true"
			    android:text="@string/response_yes"	    
			    android:textColor="#ff0000"
			    android:textSize="20sp" /> 
			*/
			mResponse_yes = new TextView(mContext);
			RelativeLayout.LayoutParams yesParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			yesParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			yesParams.addRule(RelativeLayout.CENTER_VERTICAL);	
			int leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
			yesParams.setMargins(leftMargin, 0, 0, 0);
			mResponse_yes.setLayoutParams(yesParams);
			mResponse_yes.setGravity(Gravity.CENTER);
			mResponse_yes.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  
			mResponse_yes.setTextColor(Color.RED);
			mResponse_yes.setTextSize(20);
			mResponse_yes.setText(getResources().getString(R.string.response_yes));
			mResponse_yes.setClickable(true);
			mResponse_yes.setId(RESPONSE_YES_ID);
			mResponse_yes.setOnClickListener(mResponseMenuItemsClickListener);
			//mResponse_yes.addOnLayoutChangeListener(mYesTextViewLayoutChangeListener);
			/*
			<TextView
			    android:id="@+id/response_maybe"		        
			    android:layout_centerInParent="true"
			    android:layout_centerVertical="true"
			    android:layout_width="wrap_content"
			    android:layout_height="wrap_content"
			    android:gravity="center" 
			    android:onClick="onClick"
			    android:clickable="true"
			    android:text="@string/response_maybe"
			    android:textColor="#ff0000"
			    android:textSize="20sp" />
			*/
			mResponse_maybe = new TextView(mContext);
			RelativeLayout.LayoutParams maybeParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			maybeParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			//maybeParams.addRule(RelativeLayout.CENTER_VERTICAL); // 이미 CENTER_IN_PARENT을 설정하였다
			mResponse_maybe.setLayoutParams(maybeParams);
			mResponse_maybe.setGravity(Gravity.CENTER);
			mResponse_maybe.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  
			mResponse_maybe.setTextColor(Color.RED);
			mResponse_maybe.setTextSize(20);
			mResponse_maybe.setText(getResources().getString(R.string.response_maybe));
			mResponse_maybe.setClickable(true);
			mResponse_maybe.setId(RESPONSE_MAYBE_ID);
			mResponse_maybe.setOnClickListener(mResponseMenuItemsClickListener);		
	        //mResponse_maybe.addOnLayoutChangeListener(mMaybeTextViewLayoutChangeListener);
	        /*
	        <TextView
			    android:id="@+id/response_no" 
			    android:layout_alignParentRight="true"	        
			    android:layout_centerVertical="true"
			    android:layout_width="wrap_content"
			    android:layout_height="wrap_content"
			    android:gravity="center" 
			    android:onClick="onClick"
			    android:clickable="true"			    
			    android:layout_marginRight="24dip"
			    android:text="@string/response_no"	    
			    android:textColor="#ff0000"
			    android:textSize="20sp" />
	        */
	        mResponse_no = new TextView(mContext);
			RelativeLayout.LayoutParams noParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			noParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			noParams.addRule(RelativeLayout.CENTER_VERTICAL);	
			int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
			noParams.setMargins(0, 0, rightMargin, 0);
			mResponse_no.setLayoutParams(noParams);
			mResponse_no.setGravity(Gravity.CENTER);
			mResponse_no.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  
			mResponse_no.setTextColor(Color.RED);
			mResponse_no.setTextSize(20);
			mResponse_no.setText(getResources().getString(R.string.response_no));
			mResponse_no.setClickable(true);
			mResponse_no.setId(RESPONSE_NO_ID);
			mResponse_no.setOnClickListener(mResponseMenuItemsClickListener);
			//mResponse_no.addOnLayoutChangeListener(mNoTextViewLayoutChangeListener);
	                
			makeExpandResponseMenuItemSize();
			
			mEventInfoLowerActionbar.addView(mResponse_yes);
			mEventInfoLowerActionbar.addView(mResponse_maybe);
			mEventInfoLowerActionbar.addView(mResponse_no); 
			
			mMadeResponseMenu = true;
    	}
    	else {
    		if (mEventInfoLowerActionbar.getVisibility() != View.VISIBLE)
    			mEventInfoLowerActionbar.setVisibility(View.VISIBLE);
    		
    		mEventInfoLowerActionbar.removeAllViews();
    		   		
    		mEventInfoLowerActionbar.addView(mResponse_yes);
			mEventInfoLowerActionbar.addView(mResponse_maybe);
			mEventInfoLowerActionbar.addView(mResponse_no);
    		
    	}
	}    
    
    ShapeDrawable mResponseYesBackground = null;
    ShapeDrawable mResponseMaybeBackground = null;
    ShapeDrawable mResponseNoBackground = null;
    public void makeExpandResponseMenuItemSize() {
    	int boundLeft = 0;
    	int boundTop = 0;
    	int boundRight = 0;
    	int boundBottom = 0;
    	final float expandWidthRatio = 0.3f;
    	final float OutRadiusRatio = expandWidthRatio / 4;
    	
    	float parentViewHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
    	float expandHeightSize = parentViewHeight * 0.7f;
    	
    	///////////////////////////////////////////////////////////////////////////////////////////////////////
    	Rect yesBounds = new Rect();
    	Paint yesTextPaint = mResponse_yes.getPaint();        	
    	String yesText = (String) mResponse_yes.getText();
    	yesTextPaint.getTextBounds(yesText, 0, yesText.length(), yesBounds);
    	
    	float yesBoundWidth = yesBounds.width();
    	float yesExpandWidthSize = yesBoundWidth + (yesBoundWidth * expandWidthRatio);
    	mResponse_yes.setWidth((int) yesExpandWidthSize);
    	mResponse_yes.setHeight((int) expandHeightSize);    	
    	
    	boundRight = (int)yesExpandWidthSize;
    	boundBottom = (int)expandHeightSize;
    	
    	float yesOutRadius = yesExpandWidthSize * OutRadiusRatio;    	
        float[] yesOutR = new float[]{yesOutRadius,yesOutRadius,yesOutRadius,yesOutRadius,yesOutRadius,yesOutRadius,yesOutRadius,yesOutRadius};   
    	
    	mResponseYesBackground = new ShapeDrawable(new RoundRectShape(yesOutR, null, null));      
    	mResponseYesBackground.getPaint().setColor(Color.RED);
    	mResponseYesBackground.getPaint().setAntiAlias(true);
    	mResponseYesBackground.setBounds(boundLeft, boundTop, boundRight, boundBottom);
    	///////////////////////////////////////////////////////////////////////////////////////////////////////////
    	Rect maybeBounds = new Rect();
    	Paint maybeTextPaint = mResponse_maybe.getPaint();        	
    	String maybeText = (String) mResponse_maybe.getText();
    	maybeTextPaint.getTextBounds(maybeText, 0, maybeText.length(), maybeBounds);
    	
    	float maybeBoundWidth = maybeBounds.width();
    	float maybeExpandWidthSize = maybeBoundWidth + (maybeBoundWidth * expandWidthRatio);
    	mResponse_maybe.setWidth((int) maybeExpandWidthSize);
    	mResponse_maybe.setHeight((int) expandHeightSize);    	
    	
    	boundRight = (int)maybeExpandWidthSize;
    	boundBottom = (int)expandHeightSize;
    	
    	float maybeOutRadius = maybeExpandWidthSize * OutRadiusRatio;    		
        float[] maybeOutR = new float[]{maybeOutRadius,maybeOutRadius,maybeOutRadius,maybeOutRadius,maybeOutRadius,maybeOutRadius,maybeOutRadius,maybeOutRadius};   
    	
    	mResponseMaybeBackground = new ShapeDrawable(new RoundRectShape(maybeOutR, null, null));      
    	mResponseMaybeBackground.getPaint().setColor(Color.RED);
    	mResponseMaybeBackground.getPaint().setAntiAlias(true);
    	mResponseMaybeBackground.setBounds(boundLeft, boundTop, boundRight, boundBottom);
    	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	Rect noBounds = new Rect();
    	Paint noTextPaint = mResponse_no.getPaint();        	
    	String noText = (String) mResponse_no.getText();
    	noTextPaint.getTextBounds(noText, 0, noText.length(), noBounds);
    	
    	float noBoundWidth = noBounds.width();
    	float noExpandWidthSize = noBoundWidth + (noBoundWidth * expandWidthRatio);
    	mResponse_no.setWidth((int) noExpandWidthSize);
    	mResponse_no.setHeight((int) expandHeightSize);    	
    	
    	boundRight = (int)noExpandWidthSize;
    	boundBottom = (int)expandHeightSize;
    	
    	float noOutRadius = noExpandWidthSize * OutRadiusRatio;    	
        float[] noOutR = new float[]{noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius};   
    	
    	mResponseNoBackground = new ShapeDrawable(new RoundRectShape(noOutR, null, null));      
    	mResponseNoBackground.getPaint().setColor(Color.RED);
    	mResponseNoBackground.getPaint().setAntiAlias(true);
    	mResponseNoBackground.setBounds(boundLeft, boundTop, boundRight, boundBottom);
    	
    }
    
    boolean mMadeDeleteMenu = false;
    public void makeDeleteMenu() {
    	if (!mMadeDeleteMenu) {    		
    		mEventInfoLowerActionbar.setVisibility(View.VISIBLE);
    		mEventInfoLowerActionbar.setWillNotDraw(false);	
			
    		mDeleteMenuText = new TextView(mContext);
			RelativeLayout.LayoutParams deleteMenuTextParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			deleteMenuTextParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			mDeleteMenuText.setLayoutParams(deleteMenuTextParams);
			mDeleteMenuText.setGravity(Gravity.CENTER);
			mDeleteMenuText.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);  
			mDeleteMenuText.setTextColor(Color.RED);
			mDeleteMenuText.setTextSize(20);
			mDeleteMenuText.setText(getResources().getString(R.string.delete_menu_text));
			mDeleteMenuText.setClickable(true);
			mDeleteMenuText.setOnClickListener(mDeleteMenuTextClickListener);
    		
			mEventInfoLowerActionbar.addView(mDeleteMenuText);
			
    		mMadeDeleteMenu = true;
    	}
    	else {
    		if (mEventInfoLowerActionbar.getVisibility() != View.VISIBLE)
    			mEventInfoLowerActionbar.setVisibility(View.VISIBLE);
    		
    		mEventInfoLowerActionbar.removeAllViews();
    		mEventInfoLowerActionbar.addView(mDeleteMenuText);
    	}
    }
    
    
    
    
}
