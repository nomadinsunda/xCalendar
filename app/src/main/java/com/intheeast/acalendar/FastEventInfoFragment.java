package com.intheeast.acalendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.agenda.AgendaListView;
import com.intheeast.colorpicker.HsvColorComparator;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.CalendarEventModel.Attendee;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.acalendar.CalendarMetaDataList.CalendarInfo;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;
import com.intheeast.acalendar.FastEventInfoView.EventInfoViewLowerActionBarStyle;
import com.intheeast.etc.ETime;
import com.intheeast.event.EditEventFragment;
import com.intheeast.event.EventColorCache;
import com.intheeast.event.ManageEventActivity;
import com.intheeast.settings.SettingsFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.text.TextUtils;
//import android.text.format.Time;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;
import android.widget.ViewSwitcher;


import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.intheeast.acalendar.CalendarController.EVENT_EDIT_ON_LAUNCH;

public class FastEventInfoFragment implements DeleteEventHelper.DeleteNotifyListener{

	public static final boolean INFO = false;
    public static final boolean DEBUG = false;
    
    public static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f; 
	public static final float SWITCH_EVENTINFOVIEW_PAGE_EXIT_VELOCITY = SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY * 1.25f; 
	
    public static final String TAG = "EventInfoFragment";
    public static final String COLOR_PICKER_DIALOG_TAG = "EventColorPickerDialog";

    protected static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    protected static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    protected static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    protected static final String BUNDLE_KEY_IS_DIALOG = "key_fragment_is_dialog";
    protected static final String BUNDLE_KEY_DELETE_DIALOG_VISIBLE = "key_delete_dialog_visible";
    protected static final String BUNDLE_KEY_WINDOW_STYLE = "key_window_style";
    protected static final String BUNDLE_KEY_CALENDAR_COLOR = "key_calendar_color";
    protected static final String BUNDLE_KEY_CALENDAR_COLOR_INIT = "key_calendar_color_init";
    protected static final String BUNDLE_KEY_CURRENT_COLOR = "key_current_color";
    protected static final String BUNDLE_KEY_CURRENT_COLOR_KEY = "key_current_color_key";
    protected static final String BUNDLE_KEY_CURRENT_COLOR_INIT = "key_current_color_init";
    protected static final String BUNDLE_KEY_ORIGINAL_COLOR = "key_original_color";
    protected static final String BUNDLE_KEY_ORIGINAL_COLOR_INIT = "key_original_color_init";
    protected static final String BUNDLE_KEY_ATTENDEE_RESPONSE = "key_attendee_response";
    protected static final String BUNDLE_KEY_USER_SET_ATTENDEE_RESPONSE =
            "key_user_set_attendee_response";
    protected static final String BUNDLE_KEY_TENTATIVE_USER_RESPONSE =
            "key_tentative_user_response";
    protected static final String BUNDLE_KEY_RESPONSE_WHICH_EVENTS = "key_response_which_events";
    protected static final String BUNDLE_KEY_REMINDER_MINUTES = "key_reminder_minutes";
    protected static final String BUNDLE_KEY_REMINDER_METHODS = "key_reminder_methods";
    
    protected static final String BUNDLE_KEY_CALLER_VIEW = "key_caller_view";

    public static int mCustomAppIconSize = 32;
        
    // Query tokens for QueryHandler
    private static final int TOKEN_QUERY_EVENT = 1 << 0;
    private static final int TOKEN_QUERY_CALENDARS = 1 << 1;
    private static final int TOKEN_QUERY_ATTENDEES = 1 << 2;
    public static final int TOKEN_QUERY_DUPLICATE_CALENDARS = 1 << 3;
    private static final int TOKEN_QUERY_REMINDERS = 1 << 4;
    public static final int TOKEN_QUERY_VISIBLE_CALENDARS = 1 << 5;
      
    
    private static final int TOKEN_QUERY_ALL = TOKEN_QUERY_DUPLICATE_CALENDARS
            | TOKEN_QUERY_ATTENDEES | TOKEN_QUERY_CALENDARS | TOKEN_QUERY_EVENT
            | TOKEN_QUERY_REMINDERS | TOKEN_QUERY_VISIBLE_CALENDARS;

    private int mCurrentQuery = 0;

    CalendarEventModel mModel = null;
    
    
    public interface OnCallerEnterAnimationEndListener {
        void onCallerReEntranceAnimEndSet();
    }
	
	OnCallerEnterAnimationEndListener mOnCallerEnterAnimationEndListener = null;
	
	public void setOnCallerEnterAnimationEndListener(OnCallerEnterAnimationEndListener l) {
		mOnCallerEnterAnimationEndListener = l;
    }	
	
    public FastEventInfoView mEventInfoView;
    
    private Uri mUri;
    private long mEventId;   

    private long mStartMillis;
    private long mEndMillis;
    private boolean mAllDay;

    //public EditResponseHelper mEditResponseHelper;
    public boolean mDeleteDialogVisible = false;    

    public int mOriginalAttendeeResponse;
    public int mAttendeeResponseFromIntent = Attendees.ATTENDEE_STATUS_NONE;
    public int mUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;
    public int mWhichEvents = -1;
    // Used as the temporary response until the dialog is confirmed. It is also
    // able to be used as a state marker for configuration changes.
    public int mTentativeUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;    
    
    private int[] mColors;
    private int mOriginalColor = -1;
    private boolean mOriginalColorInitialized = false;
    private int mCalendarColor = -1;
    private boolean mCalendarColorInitialized = false;
    private int mCurrentColor = -1;
    private boolean mCurrentColorInitialized = false;
    private int mCurrentColorKey = -1;
    
    ArrayList<String> mToEmails = new ArrayList<String>();
    ArrayList<String> mCcEmails = new ArrayList<String>();     
    
    //private QueryHandler mHandler;

    public final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mEventInfoView.updateEventHeadLineAndMemo(mModel);
        }
    };    
    
    public boolean mIsPaused = true;
    private boolean mDismissOnResume = false;    
    
    private Activity mActivity;
    private Context mContext;
    //private CalendarViewBaseFragment mParentFragment;
    
    public Menu mMenu = null;
    
    //private CalendarController mController;
    ArrayList<ReminderEntry> tempReminders;

    
    //private static final int EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END = EVENTINFO_VIEW_ENTER_ANIMATION_END | CALLER_VIEW_EXIT_ANIMATION_END;
    private int mEventInfoEnter_DayExit_AnimationEnd = 0;   
    
    //boolean mMustResponseMenuDisplay = true; 
    EventInfoViewLowerActionBarStyle mEventInfoViewLowerActionBarStyle = null;
    public EventInfoViewLowerActionBarStyle getEventInfoViewLowerActionBarStyle () {
    	return mEventInfoViewLowerActionBarStyle;
    }    
    
    public int getSelectedResponseVlueOfEvent() {
    	return mEventInfoView.mSelectedResponseValue;
    }
    
    public void makeEventInfoView() {    	
        if (!mEventInfoView.initEvent()) {
            mEventInfoView.displayEventNotFound();
            return;
        }
                
        if (!mCalendarColorInitialized) {
            mCalendarColor = mModel.getCalendarColor(); //mEventInfoView.getCalendarColor();
            mCalendarColorInitialized = true;
        }

        if (!mOriginalColorInitialized) { // 오리지날이 필요한 이유는 캘린더를 다른 것으로 교체할 수 있기 때문인가???
            mOriginalColor = mModel.getEventColor();//mEventInfoView.getOriginalColor(mCalendarColor);
            mOriginalColorInitialized = true;
        }

        if (!mCurrentColorInitialized) {
            mCurrentColor = mOriginalColor;
            mCurrentColorInitialized = true;
        }
        
        mEventInfoView.updateEventHeadLineAndMemo(mModel);       
        
        mEventInfoView.updateCalendar(mModel);            
                
        // isBusyFreeCalendar의 용도는 무엇인가??? : 확인을 해야 한다!!!
        if (!mEventInfoView.isBusyFreeCalendar()) {            
            mEventInfoView.initAttendees(mModel);
            
        	int response = -1;
        	//if (mEventInfoViewLowerActionBarStyle == null)
        		mEventInfoViewLowerActionBarStyle = mEventInfoView.getEventInfoViewLowerActionBarStyle();
        	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {        	    	
        		mEventInfoView.makeResponseMenu();        		
        		response = updateResponse();
        		mEventInfoView.checkResponseContainer(response); // -1 clear all radio buttons  //+commented by intheeast for lowactionbar switching
        	}  
        	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
        		mEventInfoView.makeDeleteMenu();        		
        	}
        	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE) {
        		if (mEventInfoView.mEventInfoLowerActionbar.getVisibility() != View.GONE)
        			mEventInfoView.mEventInfoLowerActionbar.setVisibility(View.GONE);  //+commented by intheeast for lowactionbar switching        		  		
        	}            
        }  
        else {
        	mEventInfoView.mEventInfoGuestlistContainer.setVisibility(View.GONE);
        }
        
        if (mEventInfoView.hasAlarm()) {            
            mEventInfoView.initReminders(mModel);            
        } 
        else {
        	if (mEventInfoView.mEventInfoReminderItemscontainer.getChildCount() != 0)
        		mEventInfoView.mEventInfoReminderItemscontainer.removeAllViews();
        	mEventInfoView.mEventInfoReminderItemscontainer.setVisibility(View.GONE);
        }
                
        mEventCursors.closeCursors();///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////
        sendAccessibilityEventIfQueryDone(TOKEN_QUERY_ALL);
        //////////////////////////////////////////////////////////////////////////////
        
        // All queries are done, show the view.
        if (mCurrentQuery == TOKEN_QUERY_ALL) {
        	mEventInfoView.afterQueryAllDone();            	
        }   
        
    }

    public void setEventInfoLowerActionBarGoneStatus() {
    	if (mEventInfoView.mEventInfoLowerActionbar != null)
    		mEventInfoView.mEventInfoLowerActionbar.setVisibility(View.GONE);
    }
    
    public void setEventInfoLowerActionBarVisibleStatus() {
    	if (mEventInfoView.mEventInfoLowerActionbar != null)
    		mEventInfoView.mEventInfoLowerActionbar.setVisibility(View.VISIBLE);
    }
    
       
    public void sendAccessibilityEventIfQueryDone(int token) {
        mCurrentQuery |= token;
        if (mCurrentQuery == TOKEN_QUERY_ALL) {
            mEventInfoView.sendAccessibilityEvent();
        }
    }   
    
    //long mAnimDuration;
    boolean mHasEventCursors = false;
    int mCallerView = ViewType.DETAIL;
    CalendarMetaDataList mCalendarMetaDataList;
    ArrayList<CalendarInfo> mCalendarInfoList;
    //EventInfoViewUpperActionBarFragment mUpperActionBar; 
    EventCursors mEventCursors = null; // 유동적으로 변경될 수 있다
    float mWidth;
    
    Fragment mFragment;
    
    public FastEventInfoFragment(Context context, Activity activity, Fragment fragment, int callerView, float width) {
    	
    	mContext = context;
    	mActivity = activity;
    	mFragment = fragment;    	
		mCallerView = callerView;
		mWidth = width;
		
		mDateTextStringBuilder = new StringBuilder(50);
    	mDateTextFormatter = new Formatter(mDateTextStringBuilder, Locale.getDefault());
    	
    	mAgendaDayHeaderStringBuilder = new StringBuilder(50);
        mAgendaDayHeaderFormatter = new Formatter(mAgendaDayHeaderStringBuilder, Locale.getDefault());
				
		mMessageHandler = new messageHandler();
		mCallerExitAnimationMessageHandler = new callerExitAnimationEndMessageHandler();
    	mCallerEnterAnimationEndMessageHandler = new callerEnterAnimationEndMessageHandler();   	
		
    	mCalendarInfoList = null;
    	
    	mViewSwitchingDelta = 800;
		mAnimationDistance = 800;
		//mAnimDuration = calculateDuration(mViewSwitchingDelta, mAnimationDistance, MINIMUM_SNAP_VELOCITY);
    	//mHScrollInterpolator = new ScrollInterpolator();
    	
    	mHasEventCursors = true;
    	
		mCalendarMetaDataList = new CalendarMetaDataList(activity, 
				
				new Runnable() {
				
					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						mCalendarInfoList = (ArrayList<CalendarInfo>)
						(mCalendarMetaDataList.getCalendarInfoList()).clone();
					}
				});				
    }      
    	
    public EditResponseHelper mEditResponseHelper;
    public void onAttach(Activity activity) {        
        mEditResponseHelper = new EditResponseHelper(activity);
        mEditResponseHelper.setDismissListener( new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // If the user dismisses the dialog (without hitting OK),
                // then we want to revert the selection that opened the dialog.
                if (mEditResponseHelper.getWhichEvents() != -1) {
                    mUserSetResponse = mTentativeUserSetResponse;
                    mWhichEvents = mEditResponseHelper.getWhichEvents();
                } else {
                    // Revert the attending response radio selection to whatever
                    // was selected prior to this selection (possibly nothing).
                    int oldResponse;
                    if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
                        oldResponse = mUserSetResponse;
                    } else {
                        oldResponse = mOriginalAttendeeResponse;
                    }
                    int buttonToCheck = mEventInfoView.findButtonIdForResponse(oldResponse); //+commented by intheeast for lowactionbar switching

                    mEventInfoView.checkResponseContainer(oldResponse);	//+commented by intheeast for lowactionbar switching                    

                    // If the radio group is being cleared, also clear the
                    // dialog's selection of which events should be included
                    // in this response.
                    if (buttonToCheck == -1) {
                        mEditResponseHelper.setWhichEvents(-1);
                    } //+commented by intheeast for lowactionbar switching
                }

                // Since OnPause will force the dialog to dismiss, do
                // not change the dialog status
                if (!mIsPaused) {
                    mTentativeUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;
                }
            }
        });

        if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            mEditResponseHelper.setWhichEvents(UPDATE_ALL);
            mWhichEvents = mEditResponseHelper.getWhichEvents();
        }
        
        //mHandler = new QueryHandler(activity);        
    }    
    
    public void inflateEventInfoView(LayoutInflater inflater) {    	
    	mEventInfoView = (FastEventInfoView)inflater.inflate(R.layout.event_info_view_layout, null);
    	
        mEventInfoView.setActivity(mActivity);
        mEventInfoView.setFragment(this);
        mEventInfoView.setFrameLayoutViewSwitcher(mFrameLayoutViewSwitcher);
        //mEventInfoView.useEventCursors(true);
        mEventInfoView.init();
    }
    
    ViewSwitcher mFrameLayoutViewSwitcher; // 아직 설정되지 않았다...
    View mCallerViewLayout;
    CalendarViewsUpperActionBarFragment mCallerActionBar;
    CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
    CalendarViewsLowerActionBar mCallerViewLowerActionBar;
    CalendarViewsLowerActionBar mCallerViewPsudoLowerActionBar;
    
    String mTzId;
    long mGmtOff;
    TimeZone mTimeZone;
    int mFirstDayOfWeek;
    public void onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState,  ViewSwitcher viewSwitcher, 
            View callerView, 
            CalendarViewsUpperActionBarFragment callerActionBar,
            boolean hasSecActionBar, CalendarViewsSecondaryActionBar secActionBar,
            CalendarViewsLowerActionBar lowerActionBar,
            CalendarViewsLowerActionBar psudoLowerActionBar,
            String tzId) {

    	mFrameLayoutViewSwitcher = viewSwitcher;
    	mCallerViewLayout = callerView;
    	mCallerActionBar = callerActionBar;
    	
    	if (hasSecActionBar)
    		mCalendarViewsSecondaryActionBar = secActionBar;
    	else
    		mCalendarViewsSecondaryActionBar = null;
    	
        mCallerViewLowerActionBar = lowerActionBar;
        mCallerViewPsudoLowerActionBar = psudoLowerActionBar;
        mTzId = tzId;
        mTimeZone = TimeZone.getTimeZone(tzId);
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        create(inflater, savedInstanceState, lowerActionBar);        
    }       
    
    SearchViewUpperActionBarFragment mSearchCallerActionBar;
    
    public void onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState,  ViewSwitcher viewSwitcher, 
            View callerView, 
            SearchViewUpperActionBarFragment callerActionBar,
            boolean hasSecActionBar, CalendarViewsSecondaryActionBar secActionBar,
            CalendarViewsLowerActionBar lowerActionBar,
            CalendarViewsLowerActionBar psudoLowerActionBar,
            String tzId) {

    	mFrameLayoutViewSwitcher = viewSwitcher;
    	mCallerViewLayout = callerView;
    	mSearchCallerActionBar = callerActionBar;
    	
    	if (hasSecActionBar)
    		mCalendarViewsSecondaryActionBar = secActionBar;
    	else
    		mCalendarViewsSecondaryActionBar = null;
    	
        mCallerViewLowerActionBar = lowerActionBar;
        mCallerViewPsudoLowerActionBar = psudoLowerActionBar;
        mTzId = tzId;
        mTimeZone = TimeZone.getTimeZone(tzId);
        
        create(inflater, savedInstanceState, lowerActionBar);
    }
    
    public void create(LayoutInflater inflater, Bundle savedInstanceState, CalendarViewsLowerActionBar lowerActionBar) {
    	if (savedInstanceState != null) {            
            mDeleteDialogVisible =
                savedInstanceState.getBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE,false);
            mCalendarColor = savedInstanceState.getInt(BUNDLE_KEY_CALENDAR_COLOR);
            mCalendarColorInitialized =
                    savedInstanceState.getBoolean(BUNDLE_KEY_CALENDAR_COLOR_INIT);
            mOriginalColor = savedInstanceState.getInt(BUNDLE_KEY_ORIGINAL_COLOR);
            mOriginalColorInitialized = savedInstanceState.getBoolean(
                    BUNDLE_KEY_ORIGINAL_COLOR_INIT);
            mCurrentColor = savedInstanceState.getInt(BUNDLE_KEY_CURRENT_COLOR);
            mCurrentColorInitialized = savedInstanceState.getBoolean(
                    BUNDLE_KEY_CURRENT_COLOR_INIT);
            mCurrentColorKey = savedInstanceState.getInt(BUNDLE_KEY_CURRENT_COLOR_KEY);

            mTentativeUserSetResponse = savedInstanceState.getInt(
                            BUNDLE_KEY_TENTATIVE_USER_RESPONSE,
                            Attendees.ATTENDEE_STATUS_NONE);
            if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE &&
                    mEditResponseHelper != null) {
                // If the edit response helper dialog is open, we'll need to
                // know if either of the choices were selected.
                mEditResponseHelper.setWhichEvents(savedInstanceState.getInt(
                        BUNDLE_KEY_RESPONSE_WHICH_EVENTS, -1));
            }
            mUserSetResponse = savedInstanceState.getInt(
                    BUNDLE_KEY_USER_SET_ATTENDEE_RESPONSE,
                    Attendees.ATTENDEE_STATUS_NONE);
            if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
                // If the response was set by the user before a configuration
                // change, we'll need to know which choice was selected.
                mWhichEvents = savedInstanceState.getInt(
                        BUNDLE_KEY_RESPONSE_WHICH_EVENTS, -1);
            }

            tempReminders = Utils.readRemindersFromBundle(savedInstanceState); // mRemindesr는 이제 mView의 멤버변수가 될 것이기 때문에,,,
                                                                               // mView를 생성한 후에 mReminders를 설정하는 함수를 만들자
            mCallerView = savedInstanceState.getInt(BUNDLE_KEY_CALLER_VIEW);   
            
            mEventId = savedInstanceState.getLong(BUNDLE_KEY_EVENT_ID);
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            mStartMillis = savedInstanceState.getLong(BUNDLE_KEY_START_MILLIS);
            mEndMillis = savedInstanceState.getLong(BUNDLE_KEY_END_MILLIS);
        }    	        
        
        getCallerViewLowActionBarChildViews(lowerActionBar);
        
        makeExpandResponseMenuItemSize();
        
        //setInitMainViewActionBar();       
        
        inflateEventInfoView(inflater); 
        
        if (tempReminders != null)
        	mEventInfoView.setReminders(tempReminders);  
        
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(mActivity);
        String defaultReminderString = prefs.getString(
                SettingsFragment.KEY_DEFAULT_REMINDER, SettingsFragment.NO_REMINDER_STRING);       
        mEventInfoView.prepareReminders(defaultReminderString); 
    }

    public void setPrimaryEventInfo(EventCursors eventCursors, long startMillis, long endMillis) {
    	/*
    	 * 이렇게 커서를 close 하는 것은 좋은 방법이 아니다
    	 * :즉 이벤트 세부사항 디스플레이를 요청받을 때만 이전 이벤트 커서를 close 하는 것은 결국 더 이상 이벤트 세부사항 요청이 없는 경우,
    	 *  이전 이벤트 커서가 close 되지 못하는 상황이 된다
    	if (mEventCursors != null)
    		mEventCursors.closeCursors();
    	*/
    	mEventCursors = eventCursors;
    		
    	mStartMillis = startMillis;
		mEndMillis = endMillis;
		
		mEventId = mEventCursors.mEventId;
		
		Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
		
		mUri = uri;	
		
		getEventInfo();
    }
        
    
        
    public void onSaveInstanceState(Bundle outState) {        
        outState.putLong(BUNDLE_KEY_EVENT_ID, mEventId);
        outState.putLong(BUNDLE_KEY_START_MILLIS, mStartMillis);
        outState.putLong(BUNDLE_KEY_END_MILLIS, mEndMillis);
        
        outState.putBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE, mDeleteDialogVisible);
        outState.putInt(BUNDLE_KEY_CALENDAR_COLOR, mCalendarColor);
        outState.putBoolean(BUNDLE_KEY_CALENDAR_COLOR_INIT, mCalendarColorInitialized);
        outState.putInt(BUNDLE_KEY_ORIGINAL_COLOR, mOriginalColor);
        outState.putBoolean(BUNDLE_KEY_ORIGINAL_COLOR_INIT, mOriginalColorInitialized);
        outState.putInt(BUNDLE_KEY_CURRENT_COLOR, mCurrentColor);
        outState.putBoolean(BUNDLE_KEY_CURRENT_COLOR_INIT, mCurrentColorInitialized);
        outState.putInt(BUNDLE_KEY_CURRENT_COLOR_KEY, mCurrentColorKey);

        // We'll need the temporary response for configuration changes.
        outState.putInt(BUNDLE_KEY_TENTATIVE_USER_RESPONSE, mTentativeUserSetResponse);
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE &&
                mEditResponseHelper != null) {
            outState.putInt(BUNDLE_KEY_RESPONSE_WHICH_EVENTS, mEditResponseHelper.getWhichEvents());
        }

        // Save the current response.
        int response;
        if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            response = mAttendeeResponseFromIntent;
        } else {
            response = mOriginalAttendeeResponse;
        }
        outState.putInt(BUNDLE_KEY_ATTENDEE_RESPONSE, response);
        if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mUserSetResponse;
            outState.putInt(BUNDLE_KEY_USER_SET_ATTENDEE_RESPONSE, response);
            outState.putInt(BUNDLE_KEY_RESPONSE_WHICH_EVENTS, mWhichEvents);
        }
                
        ArrayList<ReminderEntry> reminders = mEventInfoView.getReminders();
        int numReminders = reminders.size();
        
        ArrayList<Integer> reminderMinutes = new ArrayList<Integer>(numReminders);
        ArrayList<Integer> reminderMethods = new ArrayList<Integer>(numReminders);
        for (ReminderEntry reminder : reminders) {
            reminderMinutes.add(reminder.getMinutes());
            reminderMethods.add(reminder.getMethod());
        }
        outState.putIntegerArrayList(BUNDLE_KEY_REMINDER_MINUTES, reminderMinutes);
        outState.putIntegerArrayList(BUNDLE_KEY_REMINDER_METHODS, reminderMethods);
        
        // BUNDLE_KEY_CALLER_VIEW
        outState.putInt(BUNDLE_KEY_CALLER_VIEW, mCallerView);
    }

    public final Runnable onDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsPaused) {
                mDismissOnResume = true;
                return;
            }
            //if (EventInfoFragment.this.isVisible()) {
                //EventInfoFragment.this.dismiss();
            //}
        }
    };
    
    /*
    public void onStop() {
        
        if (!mEventDeletionStarted && mActivity != null && !mActivity.isChangingConfigurations()) {

            boolean responseSaved = saveResponse();
            
            if (mEventInfoView.saveReminders() || responseSaved) {
                Toast.makeText(mActivity, R.string.saving_event, Toast.LENGTH_SHORT).show();
            }
        }        
    }
    */
    
    public void onDestroy() {
        mEventInfoView.closeCursors();        
    }    

    /*
    public void updateResponse(long eventId, long attendeeId, int status) {
        // Update the attendee status in the attendees table.  the provider
        // takes care of updating the self attendance status.
        ContentValues values = new ContentValues();

        if (!TextUtils.isEmpty(mEventInfoView.getCalendarOwnerAccount())) {
            values.put(Attendees.ATTENDEE_EMAIL, mEventInfoView.getCalendarOwnerAccount());
        }
        values.put(Attendees.ATTENDEE_STATUS, status);
        values.put(Attendees.EVENT_ID, eventId);

        Uri uri = ContentUris.withAppendedId(Attendees.CONTENT_URI, attendeeId);

        mHandler.startUpdate(mHandler.getNextToken(), null, uri, values,
                null, null, Utils.UNDO_DELAY);
    }
	*/

    /*
    public void createExceptionResponse(long eventId, int status) {
        ContentValues values = new ContentValues();
        values.put(Events.ORIGINAL_INSTANCE_TIME, mStartMillis);
        values.put(Events.SELF_ATTENDEE_STATUS, status);
        values.put(Events.STATUS, Events.STATUS_CONFIRMED);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Uri exceptionUri = Uri.withAppendedPath(Events.CONTENT_EXCEPTION_URI,
                String.valueOf(eventId));
        ops.add(ContentProviderOperation.newInsert(exceptionUri).withValues(values).build());

        mHandler.startBatch(mHandler.getNextToken(), null, CalendarContract.AUTHORITY, ops,
                Utils.UNDO_DELAY);
   }    
	*/
    public void doEdit() {
        
        // This ensures that we aren't in the process of closing and have been
        // unattached already
        if (mContext != null) {
        	
        	CentralActivity activity = (CentralActivity) mActivity;
			View contentView = activity.getContentView();
			Utils.makeContentViewBitmap(activity, contentView);	
			
            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            Intent intent = new Intent(Intent.ACTION_EDIT, uri);
            intent.setClass(mActivity, ManageEventActivity.class);            
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);
            intent.putExtra(EXTRA_EVENT_END_TIME, mEndMillis);         
            intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);
            mActivity.startActivity(intent);                
        }
    }   
    
    // EventInfoActivity에 의해 EventInfoFragemnt가 생성된 것인지,
    // CentralActivity에 의해 생성되 Fragment replace가 수행된 것인지를 판별해야 하나?
    // :둘 다를 아우를 수 있는 코드가 필요하다...
    /*
    public void doBack() {
    	
    	//Utils.returnToCalendarHome(mContext);///////////////////////////////////////////////////////////////////////////////////        	
    	//mActivity.overridePendingTransition(R.layout.activity_slide_right_incoming, R.layout.activity_slide_right_outgoing);
    	//mActivity.finish();
    	
    	
    	// 만약 알림으로 인해 EventInfoFragment가 실행되었다면,
    	// 마지막으로 디스플레이된 뷰로 리턴해야 할 것이다
    	if (mCurrentMainView == ViewType.DAY) {
			mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
			mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY, 
					CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS, null, null);	
			
			mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY, 
					CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS_FORCE, null, null);	
		}
	}
	*/   
    public void doBack() {
    	/*FragmentManager fragmentManager = mActivity.getFragmentManager();
		FragmentTransaction ftActionBar = fragmentManager.beginTransaction();
		boolean poped = fragmentManager.popBackStackImmediate(null, 0);*/
				
		//mParentFragment.recoveryFromEventInfo();
		mRecoveryCallbackFromEventInfo.run();
    }
    
    private void updateMenu() {
        if (mMenu == null) {
            return;
        }
        
        MenuItem delete = mMenu.findItem(R.id.info_action_delete);
        MenuItem edit = mMenu.findItem(R.id.info_action_edit);
        MenuItem changeColor = mMenu.findItem(R.id.info_action_change_color);
        if (delete != null) {
            delete.setVisible(mEventInfoView.canModifyCalendar());
            delete.setEnabled(mEventInfoView.canModifyCalendar());
        }
        if (edit != null) {
            edit.setVisible(mEventInfoView.canModifyEvent());
            edit.setEnabled(mEventInfoView.canModifyEvent());
        }
        if (changeColor != null && mColors != null && mColors.length > 0) {
            changeColor.setVisible(mEventInfoView.canModifyCalendar());
            changeColor.setEnabled(mEventInfoView.canModifyCalendar());
        }
    }
    
    /**
     * Taken from com.google.android.gm.HtmlConversationActivity
     *
     * Send the intent that shows the Contact info corresponding to the email address.
     */
    public void showContactInfo(Attendee attendee, Rect rect) {
        // First perform lookup query to find existing contact
        final ContentResolver resolver = this.mActivity.getContentResolver();
        final String address = attendee.mEmail;
        final Uri dataUri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_FILTER_URI,
                Uri.encode(address));
        final Uri lookupUri = ContactsContract.Data.getContactLookupUri(resolver, dataUri);

        if (lookupUri != null) {
            // Found matching contact, trigger QuickContact
            QuickContact.showQuickContact(this.mActivity, rect, lookupUri,
                    QuickContact.MODE_MEDIUM, null);
        } else {
            // No matching contact, ask user to create one
            final Uri mailUri = Uri.fromParts("mailto", address, null);
            final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, mailUri);

            // Pass along full E-mail string for possible create dialog
            Rfc822Token sender = new Rfc822Token(attendee.mName, attendee.mEmail, null);
            intent.putExtra(Intents.EXTRA_CREATE_DESCRIPTION, sender.toString());

            // Only provide personal name hint if we have one
            final String senderPersonal = attendee.mName;
            if (!TextUtils.isEmpty(senderPersonal)) {
                intent.putExtra(Intents.Insert.NAME, senderPersonal);
            }

            mActivity.startActivity(intent);
        }
    }

    
    public void onPause() {
        mIsPaused = true;
        //mHandler.removeCallbacks(onDeleteRunnable);
        
        // Remove event deletion alert box since it is being rebuild in the OnResume
        // This is done to get the same behavior on OnResume since the AlertDialog is gone on
        // rotation but not if you press the HOME key
        if (mDeleteDialogVisible && mDeleteHelper != null) {
            mDeleteHelper.dismissAlertDialog();
            mDeleteHelper = null;
        }
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE ) {
            mEditResponseHelper.dismissAlertDialog();
        }
    }

    
    public void onResume() {        
        
        mIsPaused = false;
        if (mDismissOnResume) {
            //mHandler.post(onDeleteRunnable);
        }
        // Display the "delete confirmation" or "edit response helper" dialog if needed
        if (mDeleteDialogVisible) {
            mDeleteHelper = new DeleteEventHelper(
                    mContext, mActivity, true);
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            
        } else if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {        	
            int buttonId = mEventInfoView.findButtonIdForResponse(mTentativeUserSetResponse);
            
        	mEventInfoView.checkResponseContainer(mTentativeUserSetResponse);    //+commented by intheeast for lowactionbar switching      
            mEditResponseHelper.showDialog(mEditResponseHelper.getWhichEvents());
        }
    }

    /*
    public void reloadEvents() {
    	
        if (mHandler != null) {
            mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                    null, null, null);
        }
    }
	*/
    

    /**
     * Email all the attendees of the event, except for the viewer (so as to not email
     * himself) and resources like conference rooms.
     */
    /*
    private void emailAttendees() {
        Intent i = new Intent(getActivity(), QuickResponseActivity.class);
        i.putExtra(QuickResponseActivity.EXTRA_EVENT_ID, mEventId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
	*/
    /**
     * Loads an integer array asset into a list.
     */
    public static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
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
    public static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }   

    public long getEventId() {
        return mEventId;
    }
    public void setEventId(long id) {
        mEventId = id;
    }

    public long getStartMillis() {
        return mStartMillis;
    }
    public long getEndMillis() {
        return mEndMillis;
    }    
    public boolean getAllDay() {
    	return mAllDay;
    }
    public int getCurrentColor() {
    	return mCurrentColor;
    }
    
    public void setStartMillis(long millis) {
        mStartMillis = millis;
    }
    public void setEndMillis(long millis) {
        mEndMillis = millis;
    }
    public void setAllDay(boolean allDay) {
    	mAllDay = allDay;
    }
    
    /*    
    public void executeStartQuery(int token, Object cookie, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String orderBy) {
    	
    	mHandler.startQuery(token, cookie, uri, projection, 
    			selection, selectionArgs, orderBy);    	
    }
	*/
    private DeleteEventHelper mDeleteHelper;
    public void setDeleteEvent() {
    	mDeleteHelper =
                new DeleteEventHelper(mActivity, mActivity, true /* exitWhenDone */);
        mDeleteHelper.setDeleteNotificationListener(this);
        mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
        mDeleteDialogVisible = true;
        mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
    }
    
    public Dialog.OnDismissListener createDeleteOnDismissListener() {
        return new Dialog.OnDismissListener() {
        	
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Since OnPause will force the dialog to dismiss , do
                // not change the dialog status
                if (!mIsPaused) {
                	mDeleteDialogVisible = false;
                }
            }
        };
    }

    // Used to prevent saving changes in event if it is being deleted.
    public boolean mEventDeletionStarted = false;    
    @Override
    public void onDeleteStarted() {
        mEventDeletionStarted = true;
    }   
    
    /*
    boolean mUseCustomActionBar;
    public void setInitMainViewActionBar() {
    	mUseCustomActionBar = true;    	
	}
    */
    
    public static final int UPDATE_SINGLE = 0;
    public static final int UPDATE_ALL = 1;
    
    /**
     * Asynchronously saves the response to an invitation if the user changed
     * the response. Returns true if the database will be updated.
     *
     * @return true if the database will be changed
     */
    /**
     * These are the corresponding indices into the array of strings
     * "R.array.change_response_labels" in the resource file.
     */
    /*
    
    public boolean saveResponse() {
        //if (mEventInfoView.mAttendeesCursor == null || mEventInfoView.mEventCursor == null) {
           // return false;
        //}

        //int status = mSelectedResponseValue;
        
        if (mEventInfoView.mSelectedResponseValue == Attendees.ATTENDEE_STATUS_NONE) {
            return false;
        }

        // If the status has not changed, then don't update the database
        if (mEventInfoView.mSelectedResponseValue == mOriginalAttendeeResponse) {
            return false;
        } //+commented by intheeast for lowactionbar switching

        // If we never got an owner attendee id we can't set the status
        if (mEventInfoView.mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE) {
            return false;
        }

        if (!mEventInfoView.isRepeating()) {
            // This is a non-repeating event
        	updateResponse(getEventId(), mEventInfoView.mCalendarOwnerAttendeeId, mEventInfoView.mSelectedResponseValue);  //+commented by intheeast for lowactionbar switching
        	mOriginalAttendeeResponse = mEventInfoView.mSelectedResponseValue;  //+commented by intheeast for lowactionbar switching
            return true;
        }

        //if (DEBUG) {
            //Log.d(TAG, "Repeating event: mWhichEvents=" + mWhichEvents);
        //}
        // This is a repeating event
        switch (mWhichEvents) {
            case -1:
                return false;                
            case UPDATE_SINGLE:
            	createExceptionResponse(getEventId(), mEventInfoView.mSelectedResponseValue);
                mOriginalAttendeeResponse = mEventInfoView.mSelectedResponseValue;
                return true;
            case UPDATE_ALL:
            	updateResponse(getEventId(), mEventInfoView.mCalendarOwnerAttendeeId, mEventInfoView.mSelectedResponseValue);
            	mOriginalAttendeeResponse = mEventInfoView.mSelectedResponseValue;
                return true;
                            
            default:
                Log.e("tag", "Unexpected choice for updating invitation response");
                break;
        }
        return false;
    }
    */
    
    
    public int updateResponse() {
        // we only let the user accept/reject/etc. a meeting if:
        // a) you can edit the event's containing calendar AND
        // b) you're not the organizer and only attendee AND
        // c) organizerCanRespond is enabled for the calendar
        // (if the attendee data has been hidden, the visible number of attendees
        // will be 1 -- the calendar owner's).
        // (there are more cases involved to be 100% accurate, such as
        // paying attention to whether or not an attendee status was
        // included in the feed, but we're currently omitting those corner cases
        // for simplicity).

        // TODO Switch to EditEventHelper.canRespond when this class uses CalendarEventModel.        

        int response;
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mTentativeUserSetResponse;
        } else if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mUserSetResponse;
        } else if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            response = mAttendeeResponseFromIntent;
        } else {
            response = mOriginalAttendeeResponse;
        }

        return response;        
    }
    
    
    
    public int getEventInfoViewLowerActionBarResponseMenuVisibility() {
    	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE)
    		return View.VISIBLE;  //+commented by intheeast for lowactionbar switching    	
    	else 
    		return View.GONE;
    }
    
    public int findCurrentSelectedButtonIdForResponse() {
    	return mEventInfoView.findButtonIdForResponse(mEventInfoView.mSelectedResponseValue);  //+commented by intheeast for lowactionbar switching    	
    }
    
    
    
    public void replaceActionBarBySelectingEventItemLayout(int subpageId) {
    	mEventInfoUpperActionBarFrag.replaceActionBarBySelectingEventItemLayout(subpageId);
    }
    
    
    // for upper actionbar
    // EventInfoViewUpperActionBarFragment::mActionBarListener에서
    // onActionBarItemSelected를 호출함으로써 실행된다
    public void switchingEventPage(boolean subPageSwitching, int subpageId) {
    	mEventInfoView.settingViewSwitching(subPageSwitching, subpageId);
    }
    
    public void attendeePageViewSwitching(boolean subPageSwitching) {
    	mEventInfoView.mAttendeesInfoSwitchPageView.settingViewSwitching(subPageSwitching, null);
    }
    
    
    public ArrayList<CalendarInfo> getCalendarInfoList() {
    	return mCalendarInfoList;
    }
        
    
    
    
    float mViewSwitchingDelta;
    private float mAnimationDistance = 0;    

    
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
        velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);

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
    
    /*
    public boolean isInitedEventInfoView() {
    	return mEventInfoView.isInitedGlobalLayout();
    }
    */
    
    boolean mInflatedEventInfoSubViews = false;    
    public void inflateEventInfoSubViews() {
    	if (!mInflatedEventInfoSubViews) {
	    	mEventInfoView.inflateSubPages();
	    	mInflatedEventInfoSubViews = true;
    	}
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void setEventInfoViewGlobalLayoutListener(Runnable callBack) {
    	mEventInfoView.setGlobalLayoutListener(callBack);
    }
    
    public void notifyCallerEnterAnimationEnd(int what) {
    	Message msg = mCallerEnterAnimationEndMessageHandler.obtainMessage();
    	msg.what = what;    	
    	mCallerEnterAnimationEndMessageHandler.sendMessage(msg);
    }
    
    public void notifyCallerExitAnimationEnd(int what) {
    	Message msg = mCallerExitAnimationMessageHandler.obtainMessage();
    	msg.what = what;    	
    	mCallerExitAnimationMessageHandler.sendMessage(msg);
    }	
    
    public void notifyEventInfoEnterAnimationStart() {
    	Message msg = mMessageHandler.obtainMessage();
    	msg.what = EVENTINFO_VIEW_ENTER_ANIMATION_START;    	
    	mMessageHandler.sendMessage(msg);
    }    
    
    messageHandler mMessageHandler;
    @SuppressLint("HandlerLeak") 
    private class messageHandler extends Handler {
        
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
             
            switch (msg.what) {
            case EVENTINFO_VIEW_ENTER_ANIMATION_START:
            	// 굳이 이때 해야 하는가?
            	// FastEventInfoFragment 생성시 만들면 안되는가???
            	FastEventInfoFragment.this.inflateEventInfoSubViews();
            	break;
            case EVENTINFO_VIEW_ENTER_ANIMATION_END:            	
            	break;
            case CALLER_VIEW_EXIT_ANIMATION_START:
            	break;
            case CALLER_VIEW_EXIT_ANIMATION_END:            	           	
                break;           
 
            default:
                break;
            }
        }         
    };    
    
    
    private static final int EVENTINFO_VIEW_EXIT_ANIMATION_START = 0;
    private static final int EVENTINFO_VIEW_EXIT_ANIMATION_END = 1;
    private static final int CALLER_VIEW_ENTER_ANIMATION_START = 2;
    private static final int CALLER_VIEW_ENTER_ANIMATION_END = 4;
    private static final int CALLER_LOW_ACTIONBAR_OPAQUE_ANIMATION_END = 8;
    private static final int CALLER_ENTER_AND_EVENTINFO_EXIT_ANIMATION_END_WITH_NO_MENU = CALLER_VIEW_ENTER_ANIMATION_END | EVENTINFO_VIEW_EXIT_ANIMATION_END;     
    private static final int CALLER_ENTER_AND_EVENTINFO_EXIT_ANIMATION_END_WITH_MENU = CALLER_VIEW_ENTER_ANIMATION_END | EVENTINFO_VIEW_EXIT_ANIMATION_END | CALLER_LOW_ACTIONBAR_OPAQUE_ANIMATION_END;    
    
    private int mCallerEnterAnimationEndOrTokens = 0;
    callerEnterAnimationEndMessageHandler mCallerEnterAnimationEndMessageHandler;
    @SuppressLint("HandlerLeak") 
    private class callerEnterAnimationEndMessageHandler extends Handler {
        
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
             
            switch (msg.what) {            
            case CALLER_VIEW_ENTER_ANIMATION_END:  
            	afterCallerEnterAnimationEnd(CALLER_VIEW_ENTER_ANIMATION_END);
            	break;            
            case EVENTINFO_VIEW_EXIT_ANIMATION_END:            	
            	FragmentManager fragmentManager = mActivity.getFragmentManager();				
				boolean poped = fragmentManager.popBackStackImmediate(null, 0);
				if (!poped) {
					// 예외 처리를 해야 하는가?
					if (INFO) Log.i(TAG, "EVENTINFO_VIEW_EXIT_ANIMATION_END : NOT Calendar ActionBar Poped");
				}
				
				afterCallerEnterAnimationEnd(EVENTINFO_VIEW_EXIT_ANIMATION_END);  
                break;           
            case CALLER_LOW_ACTIONBAR_OPAQUE_ANIMATION_END:
            	afterCallerEnterAnimationEnd(CALLER_LOW_ACTIONBAR_OPAQUE_ANIMATION_END); 
            	break;
            default:
                break;
            }
        }         
    }; 
	
	
    private void afterCallerEnterAnimationEnd(int token) {/////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	mCallerEnterAnimationEndOrTokens |= token;   		
    	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {		
			if (mCallerEnterAnimationEndOrTokens == CALLER_ENTER_AND_EVENTINFO_EXIT_ANIMATION_END_WITH_MENU) {
				mCallerViewLowerActionBarTodayTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarCalendarsTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarInboxTextView.setVisibility(View.VISIBLE);
	        	
	        	mCallerViewLowerActionBar_Psudo_Yes.setVisibility(View.GONE);
	        	mCallerViewLowerActionBar_Psudo_MayBe.setVisibility(View.GONE);
	        	mCallerViewLowerActionBar_Psudo_No.setVisibility(View.GONE);
	        	
	        	mCallerEnterAnimationEndOrTokens = 0;
			}
		}
    	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
    		if (mCallerEnterAnimationEndOrTokens == CALLER_ENTER_AND_EVENTINFO_EXIT_ANIMATION_END_WITH_MENU) {
	    		mCallerViewLowerActionBarTodayTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarCalendarsTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarInboxTextView.setVisibility(View.VISIBLE);
	        	
	    		mCallerViewLowerActionBar_Psudo_Delete.setVisibility(View.GONE);
	    		
	    		mCallerEnterAnimationEndOrTokens = 0;
    		}    		
    	}
		else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE ) {  
			if (mCallerEnterAnimationEndOrTokens == CALLER_ENTER_AND_EVENTINFO_EXIT_ANIMATION_END_WITH_NO_MENU) {
    			mCallerViewPsudoLowerActionBar.setVisibility(View.INVISIBLE); 
    			mCallerViewLowerActionBar.setVisibility(View.VISIBLE); 
    			
    			mCallerEnterAnimationEndOrTokens = 0;
			}
		} 
		
    	///////////////////////////
    	if (mOnCallerEnterAnimationEndListener != null)
    		mOnCallerEnterAnimationEndListener.onCallerReEntranceAnimEndSet();    	
    }        
    
    
    
    private static final int EVENTINFO_VIEW_ENTER_ANIMATION_START = 0;
    private static final int EVENTINFO_VIEW_ENTER_ANIMATION_END = 1;
    private static final int CALLER_VIEW_EXIT_ANIMATION_START = 2;
    private static final int CALLER_VIEW_EXIT_ANIMATION_END = 4;
    private static final int CALLER_lOW_ACTIONBAR_PSUDO_RESPONSE_MENU_OPAQUE_ANIMATION_END = 8;
    private static final int CALLER_LOW_ACTIONBAR_PSUDO_DELETE_MENU_OPAQUE_ANIMATION_END = 16;
    private static final int CALLER_PSUDO_LOW_ACTIONBAR_EXIT_ANIMATION_END = 32;
    private static final int EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END_WITH_RESPONSE_MENU = EVENTINFO_VIEW_ENTER_ANIMATION_END | CALLER_VIEW_EXIT_ANIMATION_END | CALLER_lOW_ACTIONBAR_PSUDO_RESPONSE_MENU_OPAQUE_ANIMATION_END;
    private static final int EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END_WITH_DELETE_MENU = EVENTINFO_VIEW_ENTER_ANIMATION_END | CALLER_VIEW_EXIT_ANIMATION_END | CALLER_LOW_ACTIONBAR_PSUDO_DELETE_MENU_OPAQUE_ANIMATION_END;
    private static final int EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END_WITH_NO_MENU = EVENTINFO_VIEW_ENTER_ANIMATION_END | CALLER_VIEW_EXIT_ANIMATION_END | CALLER_PSUDO_LOW_ACTIONBAR_EXIT_ANIMATION_END;
    
    private int mCallerExitAnimationEndOrTokens = 0;
    callerExitAnimationEndMessageHandler mCallerExitAnimationMessageHandler;
    @SuppressLint("HandlerLeak") 
    private class callerExitAnimationEndMessageHandler extends Handler {
        
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
             
            switch (msg.what) {            
            case EVENTINFO_VIEW_ENTER_ANIMATION_END:  
            	afterCallerExitAnimationEnd(EVENTINFO_VIEW_ENTER_ANIMATION_END);
            	break;            
            case CALLER_VIEW_EXIT_ANIMATION_END:
            	afterCallerExitAnimationEnd(CALLER_VIEW_EXIT_ANIMATION_END);  
                break;           
            case CALLER_lOW_ACTIONBAR_PSUDO_RESPONSE_MENU_OPAQUE_ANIMATION_END:
            	afterCallerExitAnimationEnd(CALLER_lOW_ACTIONBAR_PSUDO_RESPONSE_MENU_OPAQUE_ANIMATION_END);
            	break;
            case CALLER_LOW_ACTIONBAR_PSUDO_DELETE_MENU_OPAQUE_ANIMATION_END:
            	afterCallerExitAnimationEnd(CALLER_LOW_ACTIONBAR_PSUDO_DELETE_MENU_OPAQUE_ANIMATION_END);
            	break;
            case CALLER_PSUDO_LOW_ACTIONBAR_EXIT_ANIMATION_END:
            	afterCallerExitAnimationEnd(CALLER_PSUDO_LOW_ACTIONBAR_EXIT_ANIMATION_END);
            	break;
            default:
                break;
            }
        }         
    };
    
    private void afterCallerExitAnimationEnd(int token) {
    	mCallerExitAnimationEndOrTokens |= token;
    	
    	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {    	
    		if (mCallerExitAnimationEndOrTokens == EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END_WITH_RESPONSE_MENU) {
    			View eventInfoView = mFrameLayoutViewSwitcher.getCurrentView();
        		mFrameLayoutViewSwitcher.removeAllViews(); 
        		mFrameLayoutViewSwitcher.addView(eventInfoView);
        		mCallerExitAnimationEndOrTokens = 0;
    			
    			mCallerViewLowerActionBar.setVisibility(View.GONE);
            	setEventInfoLowerActionBarVisibleStatus();
    		}			
		}
    	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
    		if (mCallerExitAnimationEndOrTokens == EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END_WITH_DELETE_MENU) {
    			View eventInfoView = mFrameLayoutViewSwitcher.getCurrentView();
    			mFrameLayoutViewSwitcher.removeAllViews(); 
        		mFrameLayoutViewSwitcher.addView(eventInfoView);
        		mCallerExitAnimationEndOrTokens = 0;
    			
    			mCallerViewLowerActionBar.setVisibility(View.GONE);
            	setEventInfoLowerActionBarVisibleStatus();
    		}
    	}
    	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE) {
    		if (mCallerExitAnimationEndOrTokens == EVENTINFO_ENTER_AND_CALLER_EXIT_ANIMATION_END_WITH_NO_MENU) {
    			View eventInfoView = mFrameLayoutViewSwitcher.getCurrentView();
        		mFrameLayoutViewSwitcher.removeAllViews(); 
        		mFrameLayoutViewSwitcher.addView(eventInfoView);
        		mCallerExitAnimationEndOrTokens = 0;
    		}
    	}    	
    }
    
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // EventInfoView가 해당 Event의 내용에 맞게 Layout 구성이 완료되면 EventInfoView에서 호출한다
    // Switching이 시작된다
    public Runnable mShowNextAfterCallerExit = new Runnable() {
        @Override
        public void run() {
        	if (mCalendarViewsSecondaryActionBar != null)
        		mCalendarViewsSecondaryActionBar.setInVisibleBar(mCallerView);
        	
        	// switching이 시작된다
        	mFrameLayoutViewSwitcher.showNext();
        	
        	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {        	
	        	mCallerViewLowerActionBarTodayTextView.startAnimation(mCallerViewLowerActionBarTodayAlphaAnim);
	        	mCallerViewLowerActionBarCalendarsTextView.startAnimation(mCallerViewLowerActionBarCalendarAlphaAnim);
	        	mCallerViewLowerActionBarInboxTextView.startAnimation(mCallerViewLowerActionBarInboxAlphaAnim);
	        	
	        	mCallerViewLowerActionBar_Psudo_MayBe.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBar_Psudo_No.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBar_Psudo_Yes.setVisibility(View.VISIBLE);	  
	        	
	        	mCallerViewLowerActionBar_Psudo_MayBe.startAnimation(mCallerViewLowerActionBarPsudoResponseMaybeAlphaAnim);
	        	mCallerViewLowerActionBar_Psudo_No.startAnimation(mCallerViewLowerActionBarPsudoResponseNoAlphaAnim);
	        	mCallerViewLowerActionBar_Psudo_Yes.startAnimation(mCallerViewLowerActionBarPsudoResponseYesAlphaAnim);
        	}
        	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
        		mCallerViewLowerActionBarTodayTextView.startAnimation(mCallerViewLowerActionBarTodayAlphaAnim);
	        	mCallerViewLowerActionBarCalendarsTextView.startAnimation(mCallerViewLowerActionBarCalendarAlphaAnim);
	        	mCallerViewLowerActionBarInboxTextView.startAnimation(mCallerViewLowerActionBarInboxAlphaAnim);
	        	
	        	mCallerViewLowerActionBar_Psudo_Delete.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBar_Psudo_Delete.startAnimation(mCallerViewLowerActionBarPsudoDeleteAlphaAnim);
        	}
        	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE)        		
        		mCallerViewPsudoLowerActionBar.startAnimation(mCallerViewPsudoLowerActionBarExitAnimation);
        }
    };    
     
    AnimationListener mCallerViewEnterAnimationListener = new AnimationListener(){

		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mDayViewEnterAnimationListener start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mDayViewEnterAnimationListener end");
			notifyCallerEnterAnimationEnd(CALLER_VIEW_ENTER_ANIMATION_END);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
			
		}		
	};		
	AnimationListener mCallerViewExitAnimationListener = new AnimationListener(){

		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mDayViewExitAnimationListener start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mDayViewExitAnimationListener end");
			notifyCallerExitAnimationEnd(CALLER_VIEW_EXIT_ANIMATION_END); // 
			if (mCallerViewExitCompletionRunnable != null)
				mCallerViewExitCompletionRunnable.run();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
			
		}		
	};	
    
    AnimationListener mEventInfoViewEnterAnimationListener = new AnimationListener(){

		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mEventInfoViewEnterAnimationListener start");
			notifyEventInfoEnterAnimationStart();			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mEventInfoViewEnterAnimationListener end");
			mCallerViewPsudoLowerActionBar.clearAnimation();
			notifyCallerExitAnimationEnd(EVENTINFO_VIEW_ENTER_ANIMATION_END);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
			//Log.i(TAG, "mEventInfoViewEnterAnimationListener repeat");			
		}		
	};	
	AnimationListener mEventInfoViewExitAnimationListener = new AnimationListener(){

		@Override
		public void onAnimationStart(Animation animation) {	
			// 만약 세컨더리 액션바를 가지고 있다면???
			if (mCalendarViewsSecondaryActionBar != null) {
				mCalendarViewsSecondaryActionBar.setVisibleBar(mCallerView);
				mCalendarViewsSecondaryActionBar.startAnimation(mCallerViewCalendarViewsSecondaryActionBarEnterAnimation);
			}
			
			if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {			        	
				mCallerViewLowerActionBar_Psudo_Yes.startAnimation(mCallerViewLowerActionBarPsudoResponseYesAlphaAnim);
	        	mCallerViewLowerActionBar_Psudo_MayBe.startAnimation(mCallerViewLowerActionBarPsudoResponseMaybeAlphaAnim);
	        	mCallerViewLowerActionBar_Psudo_No.startAnimation(mCallerViewLowerActionBarPsudoResponseNoAlphaAnim);        	
	        	
	        	mCallerViewLowerActionBarTodayTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarCalendarsTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarInboxTextView.setVisibility(View.VISIBLE);	  
	        	
	        	mCallerViewLowerActionBarCalendarsTextView.startAnimation(mCallerViewLowerActionBarCalendarAlphaAnim);
	        	mCallerViewLowerActionBarInboxTextView.startAnimation(mCallerViewLowerActionBarInboxAlphaAnim);	   
	        	mCallerViewLowerActionBarTodayTextView.startAnimation(mCallerViewLowerActionBarTodayAlphaAnim);	        	     	
			}
			else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
				mCallerViewLowerActionBar_Psudo_Delete.startAnimation(mCallerViewLowerActionBarPsudoDeleteAlphaAnim);        	        	
	        	
	        	mCallerViewLowerActionBarTodayTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarCalendarsTextView.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBarInboxTextView.setVisibility(View.VISIBLE);	  
	        	
	        	mCallerViewLowerActionBarCalendarsTextView.startAnimation(mCallerViewLowerActionBarCalendarAlphaAnim);
	        	mCallerViewLowerActionBarInboxTextView.startAnimation(mCallerViewLowerActionBarInboxAlphaAnim);	   
	        	mCallerViewLowerActionBarTodayTextView.startAnimation(mCallerViewLowerActionBarTodayAlphaAnim);
			}
			if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE )
	    		mCallerViewPsudoLowerActionBar.startAnimation(mCallerViewPsudoLowerActionBarEnterAnimation);	
	    	
				
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mEventInfoViewExitAnimationListener end");
			notifyCallerEnterAnimationEnd(EVENTINFO_VIEW_EXIT_ANIMATION_END);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
			
		}		
	};	
		
	
	AnimationListener mCallerViewPsudoLowerActionBarExitAnimationListener = new AnimationListener(){

		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mCallerViewPsudoLowerActionBarExitAnimation start");
			
		}

		@Override
		public void onAnimationEnd(Animation animation) {			
			if (INFO) Log.i(TAG, "mCallerViewPsudoLowerActionBarExitAnimation end");
			notifyCallerExitAnimationEnd(CALLER_PSUDO_LOW_ACTIONBAR_EXIT_ANIMATION_END);		
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};	
	
	
	AnimationListener mCallerViewLowActionBarOpaqueAlphaAnimlistener = new AnimationListener(){

		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mCallerViewLowActionBarOpaqueAlphaAnimlistener start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mCallerViewLowActionBarOpaqueAlphaAnimlistener end");
			notifyCallerEnterAnimationEnd(CALLER_LOW_ACTIONBAR_OPAQUE_ANIMATION_END);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {			
		}		
	};
	
	//mCallerViewLowerActionBarPsudoResponseYesAlphaAnim
	AnimationListener mmCallerViewLowerActionBarPsudoResponseMenuOpaqueAlphaAnimlistener = new AnimationListener(){
		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mmCallerViewLowerActionBarPsudoResponseMenuOpaqueAlphaAnimlistener start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mmCallerViewLowerActionBarPsudoResponseMenuOpaqueAlphaAnimlistener end");
			notifyCallerExitAnimationEnd(CALLER_lOW_ACTIONBAR_PSUDO_RESPONSE_MENU_OPAQUE_ANIMATION_END);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	
	AnimationListener mCallerViewLowerActionBarPsudoDeleteMenuOpaqueAlphaAnimlistener = new AnimationListener(){
		@Override
		public void onAnimationStart(Animation animation) {			
			if (INFO) Log.i(TAG, "mCallerViewLowerActionBarPsudoDeleteMenuOpaqueAlphaAnimlistener start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mCallerViewLowerActionBarPsudoDeleteMenuOpaqueAlphaAnimlistener end");
			notifyCallerExitAnimationEnd(CALLER_LOW_ACTIONBAR_PSUDO_DELETE_MENU_OPAQUE_ANIMATION_END);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {		
		}		
	};
      
    
    
	EventInfoViewUpperActionBarFragment mEventInfoUpperActionBarFrag;
	
	TextView mCallerViewLowerActionBarTodayTextView;
	TextView mCallerViewLowerActionBarCalendarsTextView;
	TextView mCallerViewLowerActionBarInboxTextView;
	TextView mCallerViewLowerActionBar_Psudo_Yes;
	TextView mCallerViewLowerActionBar_Psudo_MayBe;
	TextView mCallerViewLowerActionBar_Psudo_No;
	TextView mCallerViewLowerActionBar_Psudo_Delete;
	
	TranslateAnimation mEventInfoViewEnterAnimation;
	TranslateAnimation mEventInfoViewExitAnimation;
	
    TranslateAnimation mCallerViewExitAnimation;    
    TranslateAnimation mCallerViewEnterAnimation;    
    
    TranslateAnimation mCallerViewCalendarViewsSecondaryActionBarEnterAnimation = null;
    TranslateAnimation mCallerViewPsudoLowerActionBarEnterAnimation = null;
    TranslateAnimation mCallerViewPsudoLowerActionBarExitAnimation = null;	
	
	ViewSwitchInterpolator mCallerViewSecondaryActionBarEnterInterpolator = null;
	ViewSwitchInterpolator mCallerViewPsudoLowerActionBarEnterInterpolator = null;	
	ViewSwitchInterpolator mCallerViewPsudoLowerActionBarExitInterpolator = null;
	
	
	AlphaAnimation mCallerViewLowerActionBarTodayAlphaAnim = null; //mCallerViewLowerActionBarTodayTextView
	AlphaAnimation mCallerViewLowerActionBarCalendarAlphaAnim = null;
	AlphaAnimation mCallerViewLowerActionBarInboxAlphaAnim = null;
	AlphaAnimation mCallerViewLowerActionBarPsudoResponseYesAlphaAnim = null; //mCallerViewLowerActionBar_Psudo_Yes
	AlphaAnimation mCallerViewLowerActionBarPsudoResponseMaybeAlphaAnim = null;
	AlphaAnimation mCallerViewLowerActionBarPsudoResponseNoAlphaAnim = null;
	AlphaAnimation mCallerViewLowerActionBarPsudoDeleteAlphaAnim = null;
	
	Runnable mCallerViewExitCompletionRunnable = null;
	public void launchFastEventInfoView(int viewType, long eventId, int startDay, long startMillis, long endMillis, EventCursors targetObj, Runnable callerViewExitCompletionRunnable) {	
		mCallerViewExitCompletionRunnable = callerViewExitCompletionRunnable;
		launchFastEventInfoView(viewType, eventId, startDay, startMillis, endMillis, targetObj); 
	}
	// EventInfoViewUpperActionBarFragment(FastEventInfoFragment mainFragment, int callerView, String dateIndicatorTextOfCaller)
	// -MonthByWeekFragment
	
	// EventInfoViewUpperActionBarFragment(FastEventInfoFragment mainFragment, int callerView, String dateIndicatorTextOfCaller, String prvActionIconText)
	// -AgendaFragment
	// -DayFragment.launchFastEventInfoViewByAlertActivity
	// -DayFragment.launchFastEventInfoView
	
	// EventInfoViewUpperActionBarFragment(FastEventInfoFragment mainFragment, int callerView, String query, Bitmap bitmap)
	// -SearchAgendaFragment	
    public void launchFastEventInfoView(int viewType, long eventId, int startDay, long startMillis, long endMillis, EventCursors targetObj) {	
		
    	if (targetObj == null) {
    		// 예외 처리를 해야 한다!!!
    		
    		return;
    	}    	
    	
    	int containerViewId = -1;
    	
    	switch(viewType) {
    	case ViewType.DAY:    		
        	mEventInfoUpperActionBarFrag = new EventInfoViewUpperActionBarFragment(this, ViewType.DAY, 
        			(String) mCallerActionBar.getDateIndicatorText(), makeDateText(startDay, false)); 
        	containerViewId = R.id.calendar_upper_actionbar;
    		break;
    	case ViewType.MONTH:
    		mEventInfoUpperActionBarFrag = new EventInfoViewUpperActionBarFragment(this, ViewType.MONTH, (String) mCallerActionBar.getDateIndicatorText()); 
    		containerViewId = R.id.calendar_upper_actionbar;
    		break;
    	case ViewType.AGENDA:        	
        	mEventInfoUpperActionBarFrag = new EventInfoViewUpperActionBarFragment(this, ViewType.AGENDA, 
        			(String) mCallerActionBar.getDateIndicatorText(), makeAgendaDayHeaderText(startDay, false)); 	
        	containerViewId = R.id.calendar_upper_actionbar;
        	
        	AgendaListView agendaListView = (AgendaListView)mCallerViewLayout.findViewById(R.id.agenda_events_list);    
        	agendaListView.willBeSwitchEventInfoView();
    		break;
    	case ViewType.SEARCH:
    		//mSearchCallerActionBar
    		mEventInfoUpperActionBarFrag = new EventInfoViewUpperActionBarFragment(this, ViewType.SEARCH, 
    				mSearchCallerActionBar.getSearchQuery(), mSearchCallerActionBar.getEditTextViewBlurredBG());
    		containerViewId = R.id.search_upper_actionbar;
    		
    		AgendaListView searchAgendaListView = (AgendaListView)mCallerViewLayout.findViewById(R.id.agenda_events_list);    
    		searchAgendaListView.willBeSwitchEventInfoView();
    		break;
    	}    	
				
    	FragmentManager fragmentManager = mFragment.getFragmentManager();
		FragmentTransaction ftActionBar = fragmentManager.beginTransaction();
		// SearchAgendaFragment는 search_upper_actionbar이다... 물론 다른 activity이기에...////////////////////////////////////////
		ftActionBar.replace(containerViewId, mEventInfoUpperActionBarFrag);
		ftActionBar.addToBackStack(null);
    	ftActionBar.commit();     	
    	
    	setPrimaryEventInfo(targetObj, startMillis, endMillis);  
    	
    	// Day/Month Fragment는 ShowNextAfter에서
    	// mCalendarViewsSecondaryActionBar.setInVisibleBar();를 호출하지만
    	// Agenda/SearchAgenda Fragment는 Secondary ActionBar가 없기 때문에 호출하지 않는다...    	
    	setEventInfoViewGlobalLayoutListener(mShowNextAfterCallerExit);
    	makeEventInfoView();	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	mEventInfoViewLowerActionBarStyle = getEventInfoViewLowerActionBarStyle();    	
    	
    	float enterAnimationDistance = mWidth; //--------------------------------------------------------------------//
    	float exitAnimationDistance = mWidth * 0.4f; //--------------------------------------------------------------------//
    	
    	float velocity = SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY; //--------------------------------------------------------------------//
    	long enterAnimationDuration = calculateDuration(enterAnimationDistance, mWidth, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration; //--------------------------------------------------------------------//   	
    	
    	mEventInfoViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	
    	mCallerViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);    	
    	
    	ViewSwitchInterpolator enterInterpolator = new ViewSwitchInterpolator(enterAnimationDistance, mEventInfoViewEnterAnimation);  
    	ViewSwitchInterpolator exitInterpolator = new ViewSwitchInterpolator(exitAnimationDistance, mCallerViewExitAnimation);  
    	
    	mEventInfoViewEnterAnimation.setInterpolator(enterInterpolator);
    	mCallerViewExitAnimation.setInterpolator(exitInterpolator);   	
    	
    	mEventInfoViewEnterAnimation.setDuration(enterAnimationDuration);
    	mCallerViewExitAnimation.setDuration(exitAnimationDuration);    	
    	
    	mEventInfoViewEnterAnimation.setAnimationListener(mEventInfoViewEnterAnimationListener);
    	mCallerViewExitAnimation.setAnimationListener(mCallerViewExitAnimationListener);    
    	
    	
    	mFrameLayoutViewSwitcher.setInAnimation(mEventInfoViewEnterAnimation);
    	mFrameLayoutViewSwitcher.setOutAnimation(mCallerViewExitAnimation);    	
    	
    	
    	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {
    		// 1. view switcher 수행 전
    		//  : EventInfoView의 response menu을 GONE 상태로 전환
    		setEventInfoLowerActionBarGoneStatus();    		
    		makeSelectedPsudoResponseMenuItemOfCallerLowerActionBar(getSelectedResponseVlueOfEvent());
    		
    		// 알파 블렌딩을 설정해야 한다
    		mCallerViewLowerActionBarTodayAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		mCallerViewLowerActionBarCalendarAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		mCallerViewLowerActionBarInboxAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		
    		mCallerViewLowerActionBarPsudoResponseYesAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);    		
    		mCallerViewLowerActionBarPsudoResponseYesAlphaAnim.setAnimationListener(mmCallerViewLowerActionBarPsudoResponseMenuOpaqueAlphaAnimlistener);    		
    		mCallerViewLowerActionBarPsudoResponseMaybeAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);    		
    		mCallerViewLowerActionBarPsudoResponseNoAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE); 
    	}
    	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
    		// caller_view exit & eventinfo_view enter animation 중에는
    		// EventInfoView의 lower action baar을 GONE 상태로 만들고
    		// caller의 psudo EventInfoView lower action bar로 대체한다
    		setEventInfoLowerActionBarGoneStatus();    		
    		    		
    		// caller의 lower action bar의 Today/Calendars/Inbox의 DISAPPEAR Alpha animation을 만든다
    		mCallerViewLowerActionBarTodayAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		mCallerViewLowerActionBarCalendarAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		mCallerViewLowerActionBarInboxAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
    		
    		// caller의 psudo eventinfo lower actionbar의 delete menu의 APPEAR Alpha animation을 만든다
    		mCallerViewLowerActionBarPsudoDeleteAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
    		mCallerViewLowerActionBarPsudoDeleteAlphaAnim.setAnimationListener(mCallerViewLowerActionBarPsudoDeleteMenuOpaqueAlphaAnimlistener);
    	}
    	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_NONE_MENU_LOWERACTIONBARSTYLE ) {    		
    		
    		mCallerViewLowerActionBar.setVisibility(View.GONE);  
    		
    		mCallerViewPsudoLowerActionBar.setVisibility(View.VISIBLE);  
    		
    		mCallerViewPsudoLowerActionBarExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
        			Animation.RELATIVE_TO_SELF, -0.6f, 
        			Animation.ABSOLUTE, 0, 
        			Animation.ABSOLUTE, 0);    		
    		mCallerViewPsudoLowerActionBarExitAnimation.setDuration(enterAnimationDuration);
    		mCallerViewPsudoLowerActionBarExitInterpolator = new ViewSwitchInterpolator(enterAnimationDistance, mCallerViewPsudoLowerActionBarExitAnimation);  		
    		mCallerViewPsudoLowerActionBarExitAnimation.setInterpolator(mCallerViewPsudoLowerActionBarExitInterpolator); 
    		mCallerViewPsudoLowerActionBarExitAnimation.setAnimationListener(mCallerViewPsudoLowerActionBarExitAnimationListener);     		 		
    	}         
    		
    	mFrameLayoutViewSwitcher.addView(mEventInfoView);
        
	}	
	    
	Runnable mRecoveryCallbackFromEventInfo = new Runnable() {

		@Override
		public void run() {
			mEventInfoUpperActionBarFrag.setActionBarExitAnimationSet();
			
			View eventInfoView = mFrameLayoutViewSwitcher.getCurrentView();
			
			mFrameLayoutViewSwitcher.removeAllViews();  
			mFrameLayoutViewSwitcher.addView(mCallerViewLayout); if(INFO) Log.i(TAG, "recoveryFromEventInfo : mFrameLayoutViewSwitcher add DayView");
			mFrameLayoutViewSwitcher.addView(eventInfoView);
			mFrameLayoutViewSwitcher.showNext();		
			
			float enterAnimationDistance = mWidth * 0.4f;
	    	float exitAnimationDistance = mWidth; // 
	    	
	    	float velocity = SWITCH_EVENTINFOVIEW_PAGE_EXIT_VELOCITY;
	    	long exitAnimationDuration = calculateDuration(exitAnimationDistance, mWidth, velocity);    	
	    	long enterAnimationDuration = exitAnimationDuration;
	    	
	    	mCallerViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.4f, 
	    			Animation.RELATIVE_TO_SELF, 0, 
	    			Animation.ABSOLUTE, 0, 
	    			Animation.ABSOLUTE, 0);
	    	
	    	mEventInfoViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
	    			Animation.RELATIVE_TO_SELF, 1, 
	    			Animation.ABSOLUTE, 0, 
	    			Animation.ABSOLUTE, 0);
	    	
	    	ViewSwitchInterpolator enterInterpolator = new ViewSwitchInterpolator(enterAnimationDistance, mCallerViewEnterAnimation);  
	    	ViewSwitchInterpolator exitInterpolator = new ViewSwitchInterpolator(exitAnimationDistance, mEventInfoViewExitAnimation);  
	    	
	    	mCallerViewEnterAnimation.setInterpolator(enterInterpolator);
	    	mEventInfoViewExitAnimation.setInterpolator(exitInterpolator);   	
	    	
	    	mCallerViewEnterAnimation.setDuration(enterAnimationDuration);
	    	mEventInfoViewExitAnimation.setDuration(exitAnimationDuration);    	
	    	
	    	mCallerViewEnterAnimation.setAnimationListener(mCallerViewEnterAnimationListener);		
	    	mEventInfoViewExitAnimation.setAnimationListener(mEventInfoViewExitAnimationListener);    	
			
			mFrameLayoutViewSwitcher.setInAnimation(mCallerViewEnterAnimation);
	    	mFrameLayoutViewSwitcher.setOutAnimation(mEventInfoViewExitAnimation);     	
	    	
	    	if (mCalendarViewsSecondaryActionBar != null) {
	    		mCallerViewCalendarViewsSecondaryActionBarEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.6f,
						Animation.RELATIVE_TO_SELF, 0, 
						Animation.ABSOLUTE, 0, 
						Animation.ABSOLUTE, 0);
	    		mCallerViewCalendarViewsSecondaryActionBarEnterAnimation.setDuration(exitAnimationDuration);
	    		mCallerViewSecondaryActionBarEnterInterpolator = new ViewSwitchInterpolator(exitAnimationDistance, mCallerViewCalendarViewsSecondaryActionBarEnterAnimation);  
		    	mCallerViewCalendarViewsSecondaryActionBarEnterAnimation.setInterpolator(mCallerViewSecondaryActionBarEnterInterpolator);    		    	
		    	mCallerViewCalendarViewsSecondaryActionBarEnterAnimation.setFillAfter(true);
	    	}
		
	    	if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_RESPONSE_MENU_LOWERACTIONBARSTYLE) {
	    		setEventInfoLowerActionBarGoneStatus();
	    		
	    		makeSelectedPsudoResponseMenuItemOfCallerLowerActionBar(getSelectedResponseVlueOfEvent());  
	    		
	    		mCallerViewLowerActionBarTodayAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	    		mCallerViewLowerActionBarTodayAlphaAnim.setAnimationListener(mCallerViewLowActionBarOpaqueAlphaAnimlistener);
	    		mCallerViewLowerActionBarCalendarAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	    		mCallerViewLowerActionBarInboxAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	    		
	    		mCallerViewLowerActionBarPsudoResponseYesAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);    			
	    		mCallerViewLowerActionBarPsudoResponseMaybeAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);     		
	    		mCallerViewLowerActionBarPsudoResponseNoAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);     		
	    		    		
	    		mCallerViewLowerActionBarTodayTextView.setVisibility(View.GONE);
	        	mCallerViewLowerActionBarCalendarsTextView.setVisibility(View.GONE);
	        	mCallerViewLowerActionBarInboxTextView.setVisibility(View.GONE);
	        	
	        	mCallerViewLowerActionBar_Psudo_Yes.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBar_Psudo_MayBe.setVisibility(View.VISIBLE);
	        	mCallerViewLowerActionBar_Psudo_No.setVisibility(View.VISIBLE);
	        	
	        	mCallerViewLowerActionBar.setVisibility(View.VISIBLE);
	    	}
	    	else if (mEventInfoViewLowerActionBarStyle.mStyle == FastEventInfoView.EVENTINFOVIEW_DELETE_MENU_LOWERACTIONBARSTYLE) {
	    		setEventInfoLowerActionBarGoneStatus();
	    		
	    		mCallerViewLowerActionBarTodayAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	    		mCallerViewLowerActionBarTodayAlphaAnim.setAnimationListener(mCallerViewLowActionBarOpaqueAlphaAnimlistener);
	    		mCallerViewLowerActionBarCalendarAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	    		mCallerViewLowerActionBarInboxAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	    		
	        	mCallerViewLowerActionBarPsudoDeleteAlphaAnim = makeAlphaAnimation(enterAnimationDuration, ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
	        	
	        	mCallerViewLowerActionBarTodayTextView.setVisibility(View.GONE);
	        	mCallerViewLowerActionBarCalendarsTextView.setVisibility(View.GONE);
	        	mCallerViewLowerActionBarInboxTextView.setVisibility(View.GONE);
	        	mCallerViewLowerActionBar_Psudo_Delete.setVisibility(View.VISIBLE);
	    		
	        	mCallerViewLowerActionBar.setVisibility(View.VISIBLE);
	    	}
	    	else {
	    		mCallerViewPsudoLowerActionBarEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.6f,    				
	        			Animation.RELATIVE_TO_SELF, 0, 
	        			Animation.ABSOLUTE, 0, 
	        			Animation.ABSOLUTE, 0);   
	    		mCallerViewPsudoLowerActionBarEnterAnimation.setDuration(exitAnimationDuration);
	    		mCallerViewPsudoLowerActionBarEnterInterpolator = new ViewSwitchInterpolator(exitAnimationDistance, mCallerViewPsudoLowerActionBarEnterAnimation);  
	    		mCallerViewPsudoLowerActionBarEnterAnimation.setInterpolator(mCallerViewPsudoLowerActionBarEnterInterpolator);	    		
	    		mCallerViewPsudoLowerActionBarEnterAnimation.setFillAfter(true);
	    	}
	    	
	    	mEventInfoUpperActionBarFrag.startExitAnimation();
	    	
	    	mFrameLayoutViewSwitcher.showPrevious();
	    	
	    	mFrameLayoutViewSwitcher.removeView(eventInfoView);		
		}		
	};
    
	
	public void getCallerViewLowActionBarChildViews(CalendarViewsLowerActionBar lowerActionBar) {
    	
    	mCallerViewLowerActionBarTodayTextView = (TextView)mCallerViewLowerActionBar.findViewById(R.id.today_textview); 		
    	mCallerViewLowerActionBarCalendarsTextView = (TextView)mCallerViewLowerActionBar.findViewById(R.id.calendar_textview);    	
    	mCallerViewLowerActionBarInboxTextView = (TextView)mCallerViewLowerActionBar.findViewById(R.id.inbox_textview);
    	
    	mCallerViewLowerActionBar_Psudo_Yes = (TextView)mCallerViewLowerActionBar.findViewById(R.id.response_yes);
    	mCallerViewLowerActionBar_Psudo_MayBe = (TextView)mCallerViewLowerActionBar.findViewById(R.id.response_maybe);
    	mCallerViewLowerActionBar_Psudo_No = (TextView)mCallerViewLowerActionBar.findViewById(R.id.response_no);     
    	
    	mCallerViewLowerActionBar_Psudo_Delete = (TextView)mCallerViewLowerActionBar.findViewById(R.id.delete_menu); 
    }
	
	public void makeSelectedPsudoResponseMenuItemOfCallerLowerActionBar(int response) {    	    	
    	int prvValue = mSelectedResponseValue;
    	if (prvValue == response) {
	    	return;
    	}
    	else {
	    	if (prvValue == Attendees.ATTENDEE_STATUS_ACCEPTED) {
	    		mCallerViewLowerActionBar_Psudo_Yes.setTypeface(null, Typeface.NORMAL);
	    		mCallerViewLowerActionBar_Psudo_Yes.setTextColor(Color.RED);        	
	    		mCallerViewLowerActionBar_Psudo_Yes.setBackground(null);
	        } else if (prvValue == Attendees.ATTENDEE_STATUS_TENTATIVE) {
	        	mCallerViewLowerActionBar_Psudo_MayBe.setTypeface(null, Typeface.NORMAL);
	        	mCallerViewLowerActionBar_Psudo_MayBe.setTextColor(Color.RED);        	
	        	mCallerViewLowerActionBar_Psudo_MayBe.setBackground(null);
	        } else if (prvValue == Attendees.ATTENDEE_STATUS_DECLINED) {
	        	mCallerViewLowerActionBar_Psudo_No.setTypeface(null, Typeface.NORMAL);
	        	mCallerViewLowerActionBar_Psudo_No.setTextColor(Color.RED);        	
	        	mCallerViewLowerActionBar_Psudo_No.setBackground(null);
	        } else {
	            // -1???
	        }   
    	}    	
    	
    	mSelectedResponseValue = response;     	  
    	
        if (response == Attendees.ATTENDEE_STATUS_ACCEPTED) {        	
        	mCallerViewLowerActionBar_Psudo_Yes.setTypeface(null, Typeface.BOLD);
        	mCallerViewLowerActionBar_Psudo_Yes.setBackground(mResponseYesBackground);
        	mCallerViewLowerActionBar_Psudo_Yes.setTextColor(Color.WHITE);        
        }
        else if (response == Attendees.ATTENDEE_STATUS_TENTATIVE) {        	
        	mCallerViewLowerActionBar_Psudo_MayBe.setTypeface(null, Typeface.BOLD);
        	mCallerViewLowerActionBar_Psudo_MayBe.setBackground(mResponseMaybeBackground);
        	mCallerViewLowerActionBar_Psudo_MayBe.setTextColor(Color.WHITE);        
		}
        else if (response == Attendees.ATTENDEE_STATUS_DECLINED) {        	
        	mCallerViewLowerActionBar_Psudo_No.setTypeface(null, Typeface.BOLD);
        	mCallerViewLowerActionBar_Psudo_No.setBackground(mResponseNoBackground);
        	mCallerViewLowerActionBar_Psudo_No.setTextColor(Color.WHITE);        
		}        
    } 
	
	ShapeDrawable mResponseYesBackground = null;
    ShapeDrawable mResponseMaybeBackground = null;
    ShapeDrawable mResponseNoBackground = null;
    
    int mSelectedResponseValue = Attendees.ATTENDEE_STATUS_NONE;
	public void makeExpandResponseMenuItemSize() {
    	int boundLeft = 0;
    	int boundTop = 0;
    	int boundRight = 0;
    	int boundBottom = 0;
    	final float expandWidthRatio = 0.3f;
    	final float OutRadiusRatio = expandWidthRatio / 4;
    	
    	float parentViewHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics());
    	float expandHeightSize = parentViewHeight * 0.7f;
    	
    	///////////////////////////////////////////////////////////////////////////////////////////////////////
    	Rect yesBounds = new Rect();
    	Paint yesTextPaint = mCallerViewLowerActionBar_Psudo_Yes.getPaint();        	
    	String yesText = (String) mCallerViewLowerActionBar_Psudo_Yes.getText();
    	yesTextPaint.getTextBounds(yesText, 0, yesText.length(), yesBounds);
    	
    	float yesBoundWidth = yesBounds.width();
    	float yesExpandWidthSize = yesBoundWidth + (yesBoundWidth * expandWidthRatio);
    	mCallerViewLowerActionBar_Psudo_Yes.setWidth((int) yesExpandWidthSize);
    	mCallerViewLowerActionBar_Psudo_Yes.setHeight((int) expandHeightSize);    	
    	
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
    	Paint maybeTextPaint = mCallerViewLowerActionBar_Psudo_MayBe.getPaint();        	
    	String maybeText = (String) mCallerViewLowerActionBar_Psudo_MayBe.getText();
    	maybeTextPaint.getTextBounds(maybeText, 0, maybeText.length(), maybeBounds);
    	
    	float maybeBoundWidth = maybeBounds.width();
    	float maybeExpandWidthSize = maybeBoundWidth + (maybeBoundWidth * expandWidthRatio);
    	mCallerViewLowerActionBar_Psudo_MayBe.setWidth((int) maybeExpandWidthSize);
    	mCallerViewLowerActionBar_Psudo_MayBe.setHeight((int) expandHeightSize);    	
    	
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
    	Paint noTextPaint = mCallerViewLowerActionBar_Psudo_No.getPaint();        	
    	String noText = (String) mCallerViewLowerActionBar_Psudo_No.getText();
    	noTextPaint.getTextBounds(noText, 0, noText.length(), noBounds);
    	
    	float noBoundWidth = noBounds.width();
    	float noExpandWidthSize = noBoundWidth + (noBoundWidth * expandWidthRatio);
    	mCallerViewLowerActionBar_Psudo_No.setWidth((int) noExpandWidthSize);
    	mCallerViewLowerActionBar_Psudo_No.setHeight((int) expandHeightSize);    	
    	
    	boundRight = (int)noExpandWidthSize;
    	boundBottom = (int)expandHeightSize;
    	
    	float noOutRadius = noExpandWidthSize * OutRadiusRatio;    	
        float[] noOutR = new float[]{noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius,noOutRadius};   
    	
    	mResponseNoBackground = new ShapeDrawable(new RoundRectShape(noOutR, null, null));      
    	mResponseNoBackground.getPaint().setColor(Color.RED);
    	mResponseNoBackground.getPaint().setAntiAlias(true);
    	mResponseNoBackground.setBounds(boundLeft, boundTop, boundRight, boundBottom);
    	
    }
    
    private final Formatter mDateTextFormatter;
    private final StringBuilder mDateTextStringBuilder;    
    public String makeDateText(int julianDay, boolean makeDayOfWeek) {
    	Calendar date = GregorianCalendar.getInstance(mTimeZone);//new Time(mTimeZone);
    	long millis = ETime.getMillisFromJulianDay(julianDay, mTimeZone, mFirstDayOfWeek);//long millis = date.setJulianDay(julianDay);
    	date.setTimeInMillis(millis);

    	mDateTextStringBuilder.setLength(0);
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_YEAR;
        String dateViewText = ETime.formatDateTimeRange(mContext, mDateTextFormatter, millis, millis,
                flags, mTzId).toString();  
        
        if (makeDayOfWeek) {
        	SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            Date d = new Date();
            d.setTime(date.getTimeInMillis());
            String dayViewText;
        	dayViewText = " (" + sdf.format(d) + ")";
        	dateViewText = dateViewText + dayViewText;
        }        
        
        return dateViewText;
    }  
    
    private final Formatter mAgendaDayHeaderFormatter;
    private final StringBuilder mAgendaDayHeaderStringBuilder;    
    public String makeAgendaDayHeaderText(int julianDay, boolean makeDayOfWeek) {
    	Calendar date = GregorianCalendar.getInstance(mTimeZone);
    	long millis = ETime.getMillisFromJulianDay(julianDay, mTimeZone, mFirstDayOfWeek);//long millis = date.setJulianDay(julianDay);      	
    	date.setTimeInMillis(millis);
    	
        mAgendaDayHeaderStringBuilder.setLength(0);
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_YEAR;
        String dateViewText = ETime.formatDateTimeRange(mContext, mAgendaDayHeaderFormatter, millis, millis,
                flags, mTzId).toString();  
        
        if (makeDayOfWeek) {
        	SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            Date d = new Date();
            d.setTime(date.getTimeInMillis());
            String dayViewText;
        	dayViewText = " (" + sdf.format(d) + ")";
        	dateViewText = dateViewText + dayViewText;
        }        
        
        return dateViewText;
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
	
	
	private class ViewSwitchInterpolator implements Interpolator {
    	float mAnimationDistance;
    	Animation mAnimation;
    	
        public ViewSwitchInterpolator(float animationDistance, Animation animation) {
        	mAnimationDistance = animationDistance;
        	mAnimation = animation;
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
            
        }
    }
	
	
	
	
        
    public void getEventInfo() {
    	acquireDataFormCursor(EditEventFragment.TOKEN_EVENT, mEventCursors.mEventCursor);
    	
    	long eventId = mModel.mId;
    	if (mModel.mHasAttendeeData && eventId != -1) {
    		acquireDataFormCursor(EditEventFragment.TOKEN_ATTENDEES, mEventCursors.mAttendeesCursor);
        } else {
            
        }    	
    	
    	if (mModel.mHasAlarm) {
    		acquireDataFormCursor(EditEventFragment.TOKEN_REMINDERS, mEventCursors.mRemindersCursor);
    		
        } else {            
            mModel.mReminders = new ArrayList<ReminderEntry>();          
        }
    	
    	acquireDataFormCursor(EditEventFragment.TOKEN_CALENDARS, mEventCursors.mCalendarsCursor);    	
    	
    	acquireDataFormCursor(EditEventFragment.TOKEN_COLORS, mEventCursors.mColorsCursor);
    	
    	ECalendarApplication app = (ECalendarApplication) mActivity.getApplication();
    	app.setOriginalModel(mModel);
    	
    }
    public void acquireDataFormCursor(int token, Cursor cursor) {
    	if (mModel==null)
    		mModel = new CalendarEventModel(mActivity, null);
    	
        // If the query didn't return a cursor for some reason return
        if (cursor == null) {
            return;
        }       
        
        long eventId;
        switch (token) {
            case EditEventFragment.TOKEN_EVENT:
                if (cursor.getCount() == 0) {
                    // The cursor is empty. This can happen if the event was deleted.
                    cursor.close();
                    /*mOnDone.setDoneCode(Utils.DONE_EXIT);
                    mSaveOnDetach = false;
                    mOnDone.run();*/
                    return;
                }
                
                setModelFromCursor();
                cursor.close();

                mModel.mUri = mUri.toString();                
                
                break;
            case EditEventFragment.TOKEN_ATTENDEES:
                try {
                    while (cursor.moveToNext()) {
                    	int id = cursor.getInt(EventInfoLoader.ATTENDEES_INDEX_ID); //Attendees._ID
                        String name = cursor.getString(EventInfoLoader.ATTENDEES_INDEX_NAME); //Attendees.ATTENDEE_NAME
                        String email = cursor.getString(EventInfoLoader.ATTENDEES_INDEX_EMAIL); //Attendees.ATTENDEE_EMAIL
                        int status = cursor.getInt(EventInfoLoader.ATTENDEES_INDEX_STATUS); //Attendees.ATTENDEE_STATUS
                        int relationship = cursor
                                .getInt(EventInfoLoader.ATTENDEES_INDEX_RELATIONSHIP); //Attendees.ATTENDEE_RELATIONSHIP
                        String identity = cursor.getString(EventInfoLoader.ATTENDEES_INDEX_ID);
                        String idNamespace = cursor.getString(EventInfoLoader.ATTENDEES_INDEX_ID_NAMESPACE);
                        
                        if (relationship == Attendees.RELATIONSHIP_ORGANIZER) {
                            if (email != null) {
                                mModel.mOrganizer = email;
                                mModel.mIsOrganizer = mModel.mOwnerAccount
                                        .equalsIgnoreCase(email);
                                
                            }

                            if (TextUtils.isEmpty(name)) {
                                mModel.mOrganizerDisplayName = mModel.mOrganizer;
                                
                            } else {
                                mModel.mOrganizerDisplayName = name;
                                
                            }
                        }

                        if (email != null) {
                            if (mModel.mOwnerAccount != null &&
                                    mModel.mOwnerAccount.equalsIgnoreCase(email)) {
                                //int attendeeId = cursor.getInt(EventInfoLoader.ATTENDEES_INDEX_ID);
                                mModel.mOwnerAttendeeId = id;
                                mModel.mSelfAttendeeStatus = status;
                                
                                continue;
                            }
                        }
                        
                        Attendee attendee = new Attendee(id, name, email, status, identity, idNamespace, relationship);//Attendee(name, email);
                        //attendee.mStatus = status;
                        //attendee.mRelationShip = relationship;
                        mModel.addAttendee(attendee);                       
                    }
                } finally {
                    cursor.close();
                }

                break;
            case EditEventFragment.TOKEN_REMINDERS:
                try {
                    // Add all reminders to the models
                    while (cursor.moveToNext()) {
                        int minutes = cursor.getInt(EventInfoLoader.REMINDERS_MINUTES_ID);
                        int method = cursor.getInt(EventInfoLoader.REMINDERS_METHOD_ID);
                        ReminderEntry re = ReminderEntry.valueOf(minutes, method);
                        mModel.mReminders.add(re);                     
                    }

                    // Sort appropriately for display
                    Collections.sort(mModel.mReminders);
                    
                } finally {
                    cursor.close();
                }

                break;
            case EditEventFragment.TOKEN_CALENDARS:
                try {              
                    setModelFromCalendarCursor(mModel, cursor);                        
                    
                } finally {
                    cursor.close();
                }
                
                break;
            case EditEventFragment.TOKEN_COLORS:
                if (cursor.moveToFirst()) {
                    EventColorCache cache = new EventColorCache();
                    do
                    {
                        int colorKey = cursor.getInt(EventInfoLoader.COLORS_INDEX_COLOR_KEY);
                        int rawColor = cursor.getInt(EventInfoLoader.COLORS_INDEX_COLOR);
                        int displayColor = Utils.getDisplayColorFromColor(rawColor);
                        String accountName = cursor
                                .getString(EventInfoLoader.COLORS_INDEX_ACCOUNT_NAME);
                        String accountType = cursor
                                .getString(EventInfoLoader.COLORS_INDEX_ACCOUNT_TYPE);
                        cache.insertColor(accountName, accountType,
                                displayColor, colorKey);
                    } while (cursor.moveToNext());
                    cache.sortPalettes(new HsvColorComparator());

                    mModel.mEventColorCache = cache;                    
                }
                
                if (cursor != null) {
                    cursor.close();
                }

                // If the account name/type is null, the calendar event colors cannot be
                // determined, so take the default/savedInstanceState value.
                if (mModel.mCalendarAccountName == null
                       || mModel.mCalendarAccountType == null) {
                    //mEditEventView.setColorPickerButtonStates(mShowColorPalette);
                } else {
                    //mEditEventView.setColorPickerButtonStates(mModel.getCalendarEventColors());
                }

                break;
            default:
                cursor.close();
                break;
       }
   }
    
    
    public void setModelFromCursor() {
        if (mModel == null || mEventCursors.mEventCursor == null || mEventCursors.mEventCursor.getCount() != 1) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return;
        }

        mModel.clear();
        mEventCursors.mEventCursor.moveToFirst();

        mModel.mId = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ID);
        mModel.mTitle = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_TITLE);
        mModel.mDescription = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_DESCRIPTION);
        mModel.mLocation = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_LOCATION);
        mModel.mAllDay = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ALL_DAY) != 0;
        mModel.mHasAlarm = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_HAS_ALARM) != 0;
        mModel.mCalendarId = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_CALENDAR_ID);
        mModel.mStart = mEventCursors.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_DTSTART);
        String tz = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_EVENT_TIMEZONE);
        if (!TextUtils.isEmpty(tz)) {
            mModel.mTimezone = tz;
        }
        String rRule = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_RRULE);
        mModel.mRrule = rRule;
        mModel.mSyncId = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_SYNC_ID);
        mModel.mAvailability = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_AVAILABILITY);
        mModel.mRawAccessLevel = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_ACCESS_LEVEL);
        mModel.mOwnerAccount = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_OWNER_ACCOUNT);
        mModel.mHasAttendeeData = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
        mModel.mOriginalSyncId = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_ORIGINAL_SYNC_ID);
        mModel.mOriginalId = mEventCursors.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_ORIGINAL_ID);
        mModel.mOrganizer = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_ORGANIZER);
        mModel.mIsOrganizer = mModel.mOwnerAccount.equalsIgnoreCase(mModel.mOrganizer);
        mModel.mGuestsCanModify = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_GUESTS_CAN_MODIFY) != 0;

        int rawEventColor;
        if (mEventCursors.mEventCursor.isNull(EventInfoLoader.EVENT_INDEX_EVENT_COLOR)) {
            rawEventColor = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_CALENDAR_COLOR);
        } else {
            rawEventColor = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_EVENT_COLOR);
        }
        mModel.setEventColor(Utils.getDisplayColorFromColor(rawEventColor));

        int accessLevel = mModel.mRawAccessLevel;
        if (accessLevel > 0) {
            // For now the array contains the values 0, 2, and 3. We subtract
            // one to make it easier to handle in code as 0,1,2.
            // Default (0), Private (1), Public (2)
            accessLevel--;
        }
        mModel.mAccessLevel = accessLevel;
        mModel.mEventStatus = mEventCursors.mEventCursor.getInt(EventInfoLoader.EVENT_INDEX_EVENT_STATUS);

        boolean hasRRule = !TextUtils.isEmpty(rRule);

        // We expect only one of these, so ignore the other
        if (hasRRule) {
        	mModel.mDuration = mEventCursors.mEventCursor.getString(EventInfoLoader.EVENT_INDEX_DURATION);
        } else {
        	mModel.mEnd = mEventCursors.mEventCursor.getLong(EventInfoLoader.EVENT_INDEX_DTEND);
        }

        mModel.mModelUpdatedWithEventCursor = true;  
        
        mModel.mCalendarName = mEventCursors.mCalendarName;
        
    }
    
    
    public boolean setModelFromCalendarCursor(CalendarEventModel model, Cursor cursor) {
        if (model == null || cursor == null) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return false;
        }

        if (model.mCalendarId == -1) {
            return false;
        }

        if (!model.mModelUpdatedWithEventCursor) {
            Log.wtf(TAG,
                    "Can't update model with a Calendar cursor until it has seen an Event cursor.");
            return false;
        }

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (model.mCalendarId != cursor.getInt(EventInfoLoader.CALENDARS_INDEX_ID)) {
                continue;
            }

            model.mOrganizerCanRespond = cursor.getInt(EventInfoLoader.CALENDARS_INDEX_OWNER_CAN_RESPOND) != 0;

            // CALENDARS_INDEX_OWNER_ACCOUNT [Calendars.OWNER_ACCOUNT]
            model.mCalendarOwnerAccount = cursor.getString(EventInfoLoader.CALENDARS_INDEX_OWNER_ACCOUNT);
            model.mCalendarAccessLevel = cursor.getInt(EventInfoLoader.CALENDARS_INDEX_CALENDAR_ACCESS_LEVEL);
            model.mCalendarDisplayName = cursor.getString(EventInfoLoader.CALENDARS_INDEX_DISPLAY_NAME);
            model.setCalendarColor(Utils.getDisplayColorFromColor(
                    cursor.getInt(EventInfoLoader.CALENDARS_INDEX_CALENDAR_COLOR)));

            model.mCalendarAccountName = cursor.getString(EventInfoLoader.CALENDARS_INDEX_ACCOUNT_NAME);
            model.mCalendarAccountType = cursor.getString(EventInfoLoader.CALENDARS_INDEX_ACCOUNT_TYPE);

            model.mCalendarMaxReminders = cursor.getInt(EventInfoLoader.CALENDARS_INDEX_MAX_REMINDERS);
            model.mCalendarAllowedReminders = cursor.getString(EventInfoLoader.CALENDARS_INDEX_ALLOWED_REMINDERS);
            model.mCalendarAllowedAttendeeTypes = cursor
                    .getString(EventInfoLoader.CALENDARS_INDEX_ALLOWED_ATTENDEE_TYPES);
            model.mCalendarAllowedAvailability = cursor
                    .getString(EventInfoLoader.CALENDARS_INDEX_ALLOWED_AVAILABILITY);

            return true;
       }
       return false;
    }
}
