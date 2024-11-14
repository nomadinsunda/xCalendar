package com.intheeast.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import com.intheeast.colorpicker.HsvColorComparator;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CalendarController.EventHandler;
import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarEventModel;
import com.intheeast.acalendar.CalendarEventModel.Attendee;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.acalendar.DeleteEventHelper;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.EventInfoLoader;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

public class EditEventFragment extends Fragment implements EventHandler {
	private static boolean INFO = true;
	private static final String TAG = "EditEventFragment";
    
	public static final int MAIN_VIEW_FRAGMENT_STATE = 1;
	public static final int EDIT_EVENT_REPEAT_VIEW_FRAGMENT_STATE = 2;
	public static final int EDIT_EVENT_REPEATEND_VIEW_FRAGMENT_STATE = 3;
	public static final int EDIT_EVENT_FIRST_REMINDER_VIEW_FRAGMENT_STATE = 4;
	public static final int EDIT_EVENT_SECOND_REMINDER_VIEW_FRAGMENT_STATE = 5;
	public static final int EDIT_EVENT_CALENDAR_VIEW_FRAGMENT_STATE = 6;
	public static final int EDIT_EVENT_ATTENDEE_VIEW_FRAGMENT_STATE = 7;
	public static final int EDIT_EVENT_AVAILABILITY_VIEW_FRAGMENT_STATE = 8;
	public static final int EDIT_EVENT_VISIBILITY_VIEW_FRAGMENT_STATE = 9;
	
	public static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float SWITCH_MAIN_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f; 
	public static final float SWITCH_SUB_PAGE_EXIT_VELOCITY = SWITCH_MAIN_PAGE_VELOCITY * 1.25f; 
	
    private static final String BUNDLE_KEY_MODEL = "key_model";
    private static final String BUNDLE_KEY_EDIT_STATE = "key_edit_state";
    private static final String BUNDLE_KEY_EVENT = "key_event";
    private static final String BUNDLE_KEY_READ_ONLY = "key_read_only";
    private static final String BUNDLE_KEY_EDIT_ON_LAUNCH = "key_edit_on_launch";
    private static final String BUNDLE_KEY_SHOW_COLOR_PALETTE = "show_color_palette";
    private static final String BUNDLE_KEY_CURRENT_VIEWING_PAGE = "key_current_viewing_page";
    //mStartTimeExpended
    private static final String BUNDLE_KEY_STARTTIMER_EXPAND_STATUS = "key_start_timer_expand_status";
    //mEndTimeExpended
    private static final String BUNDLE_KEY_ENDTIMER_EXPAND_STATUS = "key_current_end_timer_expand_status";
    
    private static final String BUNDLE_KEY_DATE_BUTTON_CLICKED = "date_button_clicked";

    private static final boolean DEBUG = false;

    public static final int TOKEN_EVENT = 1;
    public static final int TOKEN_ATTENDEES = 1 << 1;
    public static final int TOKEN_REMINDERS = 1 << 2;
    public static final int TOKEN_CALENDARS = 1 << 3;
    public static final int TOKEN_COLORS = 1 << 4;

    private static final int TOKEN_ALL = TOKEN_EVENT | TOKEN_ATTENDEES | TOKEN_REMINDERS
            | TOKEN_CALENDARS | TOKEN_COLORS;
    private static final int TOKEN_UNITIALIZED = 1 << 31;

    /**
     * A bitfield of TOKEN_* to keep track which query hasn't been completed
     * yet. Once all queries have returned, the model can be applied to the
     * view.
     */
    
    EditEventHelper mHelper;
    CalendarEventModel mModel;
    CalendarEventModel mOriginalModel;
    CalendarEventModel mRestoreModel;
    EditEvent mEditEvent;
    
    EditEventRepeatSettingSwitchPageView mRepeatSettingSwitchPageView;
    //EditEventRepeatSettingSubView mRepeatPageView;
    EditEventRepeatEndSettingSubView mRepeatEndPageView;
    EditEventReminderSettingSubView mReminderPageView;
    EditEventCalendarSettingSubView mCalendarPageView;
    EditEventAvailabilitySubView mAvailabilityPageView;
    EditEventVisibilitySettingSubView mVisibilityPageView;
    EditEventAttendeeSettingSubView mAttendeePageView;
    
    CalendarsAndColorsQueryHandler mHandler;
    
    int mModification = com.intheeast.acalendar.Utils.MODIFY_UNINITIALIZED;

    private EventInfo mEventInfo;
    public EventBundle mEventBundle;
    
    private Uri mUri;
    //private long mBegin;
    //private long mEnd;
    
    private ManageEventActivity mActivity;
    public final Done mOnDone = new Done();

    private boolean mSaveOnDetach = true;
    private boolean mIsReadOnly = false; 
    
    int mFrameLayoutHeight;	
    public ArrayList<CalendarInfo> mCalendarInfoList;    
    
    public EditEventFragment() {
    	
    }
    
    @SuppressLint("ValidFragment")
    public EditEventFragment(ManageEventActivity activity, boolean readOnly) {
    	mActivity = activity;
    	
        mIsReadOnly = readOnly;
        
        mModel = new CalendarEventModel(mActivity, null);
        //mModel.clear();
        mHelper = new EditEventHelper(mActivity, null);
        
        mMainPageView = (RelativeLayout) mActivity.getLayoutInflater().inflate(R.layout.editevent_mainpage, null);
        mSwitchDimmingLayout = mMainPageView.findViewById(R.id.mainview_switch_dimming_layout);     
        
        mFrameLayoutHeight = calcFrameLayoutHeight(activity);       
        mHandler = new CalendarsAndColorsQueryHandler(mActivity.getContentResolver()); 
        
    }    
    
    public void makeEditEvent(EventInfo event, int viewWidth) {
    	mEventInfo = event;
    	mEditEvent = new EditEvent(mActivity.getApplicationContext(), mActivity, this, mMainPageView,
    			mOnDone, viewWidth); 

    	mEditEvent.initMainPageLayout(mEventInfo.startTime, mEventInfo.endTime);	
    }
    
    
    public void makeSubPage() {
    	// /eCalendar/res/layout/editevent_repeat_switcher_page.xml
    	mRepeatSettingSwitchPageView = (EditEventRepeatSettingSwitchPageView) mActivity.getLayoutInflater().inflate(R.layout.editevent_repeat_switcher_page, null);
    	mRepeatSettingSwitchPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	//mRepeatPageView = (EditEventRepeatSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_repeat_page, null);
    	//mRepeatPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mRepeatEndPageView = (EditEventRepeatEndSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_repeat_end_page, null);
    	mRepeatEndPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mReminderPageView = (EditEventReminderSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_reminder_page, null);
    	mReminderPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mCalendarPageView = (EditEventCalendarSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_calendarpage, null);
    	mCalendarPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mAvailabilityPageView = (EditEventAvailabilitySubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_availablitiypage, null);
    	mAvailabilityPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mVisibilityPageView = (EditEventVisibilitySettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_visibilitypage, null);
    	mVisibilityPageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mAttendeePageView = (EditEventAttendeeSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_attendeepage, null);
    	mAttendeePageView.initView(mActivity, this, mFrameLayoutHeight);
    	
    	mEditEvent.setSubPages();
    }
        
    @SuppressWarnings("unchecked")
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (INFO) Log.i(TAG, "onAttach");      
               
    }
      
    // savedInstanceState : If the fragment is being re-created from a previous saved state, this is the state.
    @Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		if (INFO) Log.i(TAG, "onCreate");
		
	}
    
    int mViewedPageBeforeDestroying = EditEvent.EVENT_MAIN_PAGE;
    boolean mStartTimerExpandStatus = false;
    boolean mEndTimerExpandStatus = false;
    public void setBundleFromAppBundle(Bundle savedInstanceState) {
    	if (savedInstanceState.containsKey(BUNDLE_KEY_MODEL)) {
            mRestoreModel = (CalendarEventModel) savedInstanceState.getSerializable(BUNDLE_KEY_MODEL);
        }
        if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_STATE)) {
            mModification = savedInstanceState.getInt(BUNDLE_KEY_EDIT_STATE);
        }
        
        if (savedInstanceState.containsKey(BUNDLE_KEY_EVENT)) {
            mEventBundle = (EventBundle) savedInstanceState.getSerializable(BUNDLE_KEY_EVENT);
        }
        if (savedInstanceState.containsKey(BUNDLE_KEY_READ_ONLY)) {
            mIsReadOnly = savedInstanceState.getBoolean(BUNDLE_KEY_READ_ONLY);
        } 
        if (savedInstanceState.containsKey(BUNDLE_KEY_CURRENT_VIEWING_PAGE)) {
        	mViewedPageBeforeDestroying = savedInstanceState.getInt(BUNDLE_KEY_CURRENT_VIEWING_PAGE);
        }
        if (savedInstanceState.containsKey(BUNDLE_KEY_STARTTIMER_EXPAND_STATUS)) {
        	mStartTimerExpandStatus = savedInstanceState.getBoolean(BUNDLE_KEY_STARTTIMER_EXPAND_STATUS);
        }
        if (savedInstanceState.containsKey(BUNDLE_KEY_ENDTIMER_EXPAND_STATUS)) {
        	mEndTimerExpandStatus = savedInstanceState.getBoolean(BUNDLE_KEY_ENDTIMER_EXPAND_STATUS);
        }
    }
    
    LinearLayout mMainFragmentLayout;
    ViewSwitcher mMainFragmentLayoutViewSwitcher;
    RelativeLayout mMainPageView;   
    View mSwitchDimmingLayout;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	if (INFO) Log.i(TAG, "onCreateView");
    	
    	mMainFragmentLayout = (LinearLayout) inflater.inflate(R.layout.edit_event, null);
    	
    	mMainFragmentLayoutViewSwitcher = (ViewSwitcher) mMainFragmentLayout.findViewById(R.id.editevent_main_fragment_switcher);
        
        mEditEvent.setViewSwitcher(mMainFragmentLayoutViewSwitcher);
        
        ManageEventActivity activity = (ManageEventActivity) mActivity;
        activity.mEditEventMainPaneFrameLayout.removeView(mEditEvent.mMainPageView);////////////////////////
        mMainFragmentLayoutViewSwitcher.addView(mMainPageView);
        if (mActivity.mDestroyBecauseOfSingleTaskRoot) {
        	if (mViewedPageBeforeDestroying != EditEvent.EVENT_MAIN_PAGE) {
        		mEditEvent.addSubViewToViewSwitcher(mViewedPageBeforeDestroying);   
        		mEditEvent.mCurrentViewingPage = mViewedPageBeforeDestroying;
        		mMainFragmentLayoutViewSwitcher.showNext();  
        		
        		String subPageTitle = mEditEvent.getSubPageActionBarTitle(mViewedPageBeforeDestroying);
        		mActionBar.setSubPageActionBar(subPageTitle);
        		
        		// �������� �߻��Ѵ�
        		// �� ��쿡 mMainPageView�� �ĸ�ο� ��ġ�� ����� mMainPageView�� width�� height�� �����Ǿ� ���� �ʱ� ������
        		// switchMainView�� ȣ��Ǹ� �Ʒ� �ڵ��� enterAnimationDistance�� 0.0���� ���ȴ�
        		// -float enterAnimationDistance = mMainPageView.getWidth() * 0.4f;
        		// �׷��Ƿ� mMainPageView�� width�� height�� �̸� ������ �ʿ䰡 �ִ�
        	}
        	
        	if (mStartTimerExpandStatus) {
        		mEditEvent.mStartTimeExpended = true;
        		mEditEvent.setTimePickerVisibility(EditEvent.START_TIME_PICKER_ID, View.VISIBLE, false);
        		mEditEvent.attachUpperLineViewOfEndTimerTextLayout();
        	}
        	else if (mEndTimerExpandStatus) {
        		mEditEvent.mEndTimeExpended = true; 
        		mEditEvent.setTimePickerVisibility(mEditEvent.END_TIME_PICKER_ID, View.VISIBLE, false);
        		if (mEditEvent.mAllDay)
        			mEditEvent.checkAllDayEndTimerValidStatus();
        		else
        			mEditEvent.checkEndTimerValidStatus(); 
                                                    
        		mEditEvent.attachUnderLineViewOfEndTimerTextLayout(); // ���⼭ ������ �߻��Ͽ���
        	}
        }
        
        //mViewSwitcher.getCurrentView().requestFocus();        
        return mMainFragmentLayout;       
    }
    
    
        
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (INFO) Log.i(TAG, "onActivityCreated");
    }
        
    @Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		if (INFO) Log.i(TAG, "onStart");
	}


	@Override
	public void onResume() {		
		super.onResume();
		
		if (INFO) Log.i(TAG, "onResume");
	}


	@Override
    public void onPause() {
		super.onPause();
    	if (INFO) Log.i(TAG, "onPause");
    	
    	if (mModel != null) {
    		mEditEvent.fillModelFromUI();
        }                
    	
    	/*Activity act = getActivity();
        if (mSaveOnDetach && act != null && !mIsReadOnly && !act.isChangingConfigurations() && mEditEvent.prepareForSave()) {
            mOnDone.setDoneCode(Utils.DONE_SAVE);
            mOnDone.run();
        }*/       
    }  
    
    // Called to ask the fragment to save its current dynamic state, so it can later be reconstructed in a new instance of its process is restarted. 
    // If a new instance of the fragment later needs to be created, 
    // the data you place in the Bundle here will be available in the Bundle given to onCreate(Bundle), 
    // onCreateView(LayoutInflater, ViewGroup, Bundle), and onActivityCreated(Bundle). 
    // This corresponds to Activity.onSaveInstanceState(Bundle) and most of the discussion there applies here as well. 
    // Note however: this method may be called at any time before onDestroy(). 
    // There are many situations where a fragment may be mostly torn down (such as when placed on the back stack with no UI showing), 
    // but its state will not be saved until its owning activity actually needs to save its state.
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	
    	if (INFO) Log.i(TAG, "onSaveInstanceState");
    	
    	mActivity.mApp.makeBundle();
    	mActivity.mApp.bundlePutSerializable(BUNDLE_KEY_MODEL, mModel);
    	mActivity.mApp.bundlePutInt(BUNDLE_KEY_EDIT_STATE, mModification);
    	
    	// ManageEventActivity�� launch�� ECalendarApplication�� mRootLaunchedAnotherActivity�� false�̸� mEventBundle�� �׻� null �̴�
    	// mEventInfo�� null�� ���ɼ��� ����
    	// :ManageEventActivity.onCreate���� ManageEventActivity.getEventInfoFromIntent�� ȣ���Ͽ� �׻� �����ȴ�
        //if (mEventBundle == null && mModel != null) {
            mEventBundle = new EventBundle();
            mEventBundle.id = mModel.mId;
            
            mEventBundle.start = mEditEvent.mStartTime.getTimeInMillis();            
            mEventBundle.end = mEditEvent.mEndTime.getTimeInMillis();          
        //}
        
        mActivity.mApp.bundlePutSerializable(BUNDLE_KEY_EVENT, mEventBundle);
        mActivity.mApp.bundlePutBoolean(BUNDLE_KEY_READ_ONLY, mIsReadOnly);
        
        mActivity.mApp.bundlePutInt(BUNDLE_KEY_CURRENT_VIEWING_PAGE, mEditEvent.mCurrentViewingPage);
        
        mActivity.mApp.bundlePutBoolean(BUNDLE_KEY_STARTTIMER_EXPAND_STATUS, mEditEvent.mStartTimeExpended);
        	
        mActivity.mApp.bundlePutBoolean(BUNDLE_KEY_ENDTIMER_EXPAND_STATUS, mEditEvent.mEndTimeExpended);     
    	
    }
    
    
    @Override
	public void onStop() {
    	super.onStop();
    	if (INFO) Log.i(TAG, "onStop");
    			
	}
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	if (INFO) Log.i(TAG, "onDestroyView");
    }


	@Override
    public void onDestroy() {
		super.onDestroy();
		if (INFO) Log.i(TAG, "onDestroy");
    	
        /*if (mEditEvent != null) {
        	mEditEvent.setMainPageLayout(mModel);
        }*/
        
        mEditEvent.mTimePickerMetaData.deleteTimePickerFieldBitmaps();        
    }
    
    @Override
	public void onDetach() {
    	super.onDetach();
    	if (INFO) Log.i(TAG, "onDetach");
		
	} 
        
	
    boolean isEmptyNewEvent() {
    	
        if (mOriginalModel != null) {
            // Not new
            return false;
        }

        if (mModel.mOriginalStart != mModel.mStart || mModel.mOriginalEnd != mModel.mEnd) {
            return false;
        }
        
        // true if this map has no elements, false otherwise
        if (!mModel.mAttendeesList.isEmpty()) {
            return false;
        }

        return mModel.isEmpty();
    }	
    
    public void setModel() {
    	mUri = null;
        long begin = -1;
        long end = -1;        
                    
        if (mEventInfo.id != -1) {
                mModel.mId = mEventInfo.id;
                mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventInfo.id);
        } else {
            // New event. All day?
            mModel.mAllDay = mEventInfo.extraLong == CalendarController.EXTRA_CREATE_ALL_DAY;
        }
        
        // ��ǻ� EventInfo.startTime�� null�� ���ɼ��� ����
        if (mEventInfo.startTime != null) {
        	begin = mEventInfo.startTime.getTimeInMillis();
        }
        
        if (mEventInfo.endTime != null) {
        	end = mEventInfo.endTime.getTimeInMillis();
        }
        
        // ��ǻ� EventInfo.startTime.getTimeInMillis()�� begin <= 0�� ���ɼ��� ����
        if (begin <= 0) {
            // use a default value instead
        	begin = mHelper.constructDefaultStartTime(System.currentTimeMillis());
        }
        
        if (end < begin) {
            // use a default value instead
        	end = mHelper.constructDefaultEndTime(begin);
        }        
        
        boolean newEvent = mUri == null;
        if (newEvent) {        	   
        	mModel.mOriginalStart = begin;
            mModel.mOriginalEnd = end;
            mModel.mStart = begin;
            mModel.mEnd = end;
            
            mModel.mSelfAttendeeStatus = Attendees.ATTENDEE_STATUS_ACCEPTED;
            
            mModel.mCalendarId = Utils.findDefaultCalendarID(mActivity.getApplicationContext());
            mModel.mOwnerAccount = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_OWNER_ACCOUNT, Utils.ECALENDAR_OWNER_ACCOUNT);
            mModel.setCalendarColor(Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_COLOR, Utils.ECALENDAR_COLOR));
                        
            mModel.mCalendarAccountName = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ACCOUNT_NAME, Utils.ECALENDAR_ACCOUNT_NAME);
            mModel.mCalendarAccountType = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ACCOUNT_TYPE, Utils.ECALENDAR_ACCOUNT_TYPE);
            mModel.setEventColor(mModel.getCalendarColor());
            
            
            mModel.mCalendarDisplayName = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_DISP_NAME, SettingsFragment.DEFAULT_ECALENDAR_CALENDAR_DISPLAYNAME);
            
            mModel.mCalendarMaxReminders = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_MAX_REMINDERS, Utils.ECALENDAR_MAX_REMINDERS);     
            mModel.mCalendarAllowedReminders = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ALLOWED_REMINDERS, Utils.ECALENDAR_ALLOWED_REMINDERS);      
            mModel.mCalendarAllowedAttendeeTypes = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ALLOWED_ATTENDEE_TYPES, Utils.ECALENDAR_ALLOWED_ATTENDEE_TYPES);     
            mModel.mCalendarAllowedAvailability = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_ALLOWED_AVAILABILITY, Utils.ECALENDAR_ALLOWED_AVAILABILITY);

            // new event�� ��쿡�� mOrganizer�� Calendars.OWNER_ACCOUNT�� �����Ѵ�
            // :new event�� �ƴ� ��쿡�� Events.ORGANIZER�� ���� �����ؾ� �Ѵ�
            mModel.mOrganizer = Utils.getSharedPreference(mActivity, SettingsFragment.KEY_DEFAULT_CALENDAR_OWNER_ACCOUNT, Utils.ECALENDAR_OWNER_ACCOUNT);           
            
            mModification = Utils.MODIFY_ALL;            
        } 
        else {
        	
        	ECalendarApplication app = (ECalendarApplication) mActivity.getApplication();
            mOriginalModel = app.getOriginalModel();
            
            setModelFromOriginal(mOriginalModel);           
            
            mOriginalModel.mUri = mUri.toString();

            // ������ event�� ������ ��, DayView���� ���õ� event�� Instances.BEGIN ����
            // FastEventInfoFragment���� intent�� EXTRA_EVENT_BEGIN_TIME�� �����ؼ�
            // ManageEventActivity�� launch�ϰ�
            // ManageEventActivity���� intent���� EXTRA_EVENT_BEGIN_TIME ���� �����ؼ�
            // EventInfo.startTime�� �����ؼ� �����Ѵ�
            // �� mBegin���� EventInfo.startTime�� �����Ѵ�
            mModel.mUri = mUri.toString();
            mModel.mOriginalStart = begin;
            mModel.mOriginalEnd = end;
            mModel.mIsFirstEventInSeries = begin == mOriginalModel.mStart;
            mModel.mStart = begin;
            mModel.mEnd = end;         
                               
            if (mModification == Utils.MODIFY_UNINITIALIZED) {
                if (TextUtils.isEmpty(mModel.mRrule)) {
                	mModification = Utils.MODIFY_ALL;
                }
            }            
        }
        
        mEditEvent.setMainPageLayout(mModel);
        mEditEvent.setModification(mModification);            
        
        // Start a query in the background to read the list of calendars and colors
        mHandler.startQuery(TOKEN_CALENDARS, null, Calendars.CONTENT_URI,
                EditEventHelper.CALENDARS_PROJECTION,
                EditEventHelper.CALENDARS_WHERE_WRITEABLE_VISIBLE, null,
                null);

        mHandler.startQuery(TOKEN_COLORS, null, Colors.CONTENT_URI,
                EditEventHelper.COLORS_PROJECTION,
                Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT, null, null);
    }
    
    public void setModelFromBundle() {
    	mUri = null;
                
        if (mEventBundle.id != -1) {
            mModel.mId = mEventBundle.id;
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventBundle.id);
        }
        
        mModel = mRestoreModel;
        
        mEditEvent.setMainPageLayout(mModel);
        mEditEvent.setModification(mModification);            
        
        // Start a query in the background to read the list of calendars and colors
        mHandler.startQuery(TOKEN_CALENDARS, null, Calendars.CONTENT_URI,
                EditEventHelper.CALENDARS_PROJECTION,
                EditEventHelper.CALENDARS_WHERE_WRITEABLE_VISIBLE, null,
                null);

        mHandler.startQuery(TOKEN_COLORS, null, Colors.CONTENT_URI,
                EditEventHelper.COLORS_PROJECTION,
                Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT, null, null);
    }
    
    

	@Override
	public long getSupportedEventTypes() {
		
		return EventType.USER_HOME;
	}

	@Override
	public void handleEvent(EventInfo event) {
		if (event.eventType == EventType.USER_HOME) {
			if (INFO) Log.i(TAG, "handleEvent:EventType.USER_HOME");
		}
		// It's currently unclear if we want to save the event or not when home
        // is pressed. When creating a new event we shouldn't save since we
        // can't get the id of the new event easily.
        /*if ((false && event.eventType == EventType.USER_HOME) || (event.eventType == EventType.GO_TO
                && mSaveOnDetach)) {
            if (mEditEvent != null && mEditEvent.prepareForSave()) {
                mOnDone.setDoneCode(Utils.DONE_SAVE);
                mOnDone.run();
            }
        }*/
	}

	@Override
	public void eventsChanged() {
		
	}
	
	
	EditEventActionBarFragment mActionBar;
	public void setActionBarFragment(EditEventActionBarFragment actionBar) {
    	mActionBar = actionBar;
    }
	
	public EditEventActionBarFragment getActionBar() {
		return mActionBar;
	}
	
	public void notifyedMsgByActionBar(Message msg) {
		
		switch(msg.arg1) {
		case EditEventActionBarFragment.SELECTED_PREVIOUS_ACTION:
			if (msg.arg2 == EditEventActionBarFragment.EDIT_EVENT_SUB_PAGE) {
				mEditEvent.switchMainView();	
				mEditEvent.setMainPageActionBar();
			}
			else if (msg.arg2 == EditEventActionBarFragment.EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE) {
				if (mEditEvent.mCurrentViewingPage == EditEvent.EVENT_REPEAT_PAGE) {
					if (mEditEvent.mRepeatSettingSwitchPageView.mPageMode == EditEventRepeatSettingSwitchPageView.REPEAT_USER_SETTINGS_PAGE) {
						mEditEvent.mRepeatSettingSwitchPageView.switchSubView(false);
					}
				}
			}
			break;
		}
	}
	
	
	
	class Done implements EditEventHelper.EditDoneRunnable {
        private int mCode = -1;

        @Override
        public void setDoneCode(int code) {
            mCode = code;
        }

        @Override
        public void run() {
            // We only want this to get called once, either because the user
            // pressed back/home or one of the buttons on screen
            mSaveOnDetach = false;
            
            if (mModification == Utils.MODIFY_UNINITIALIZED) {
                // If this is uninitialized the user hit back, the only
                // changeable item is response to default to all events.
                mModification = Utils.MODIFY_ALL;
            }

            if ((mCode & Utils.DONE_SAVE) != 0 
            		&& mModel != null
                    && ( EditEventHelper.canRespond(mModel) || EditEventHelper.canModifyEvent(mModel) )
                    && mEditEvent.prepareForSave()
                    && !isEmptyNewEvent()
                    && mModel.normalizeReminders()
                    && mHelper.saveEvent(mModel, mOriginalModel, mModification)) {
            	
                int stringResource;
                if (!mModel.mAttendeesList.isEmpty()) {
                    if (mModel.mUri != null) {  // ������ event�� �����Ͽ����� �ǹ�
                        stringResource = R.string.saving_event_with_guest;
                    } else {
                        stringResource = R.string.creating_event_with_guest;
                    }
                } else {
                    if (mModel.mUri != null) {  // ������ event�� �����Ͽ����� �ǹ�
                        stringResource = R.string.saving_event;
                    } else {
                        stringResource = R.string.creating_event;
                    }
                }
                
                //Log.i("tag", "Done.run : " + stringResource);
                //Toast.makeText(mActivity, stringResource, Toast.LENGTH_SHORT).show();
            } else if ((mCode & Utils.DONE_SAVE) != 0 && mModel != null && isEmptyNewEvent()) {
            	
                //Toast.makeText(mActivity, R.string.empty_event, Toast.LENGTH_SHORT).show();
            }

            if ((mCode & Utils.DONE_DELETE) != 0 
            		&& mOriginalModel != null 
            		&& EditEventHelper.canModifyCalendar(mOriginalModel)) {
                long begin = mModel.mStart;
                long end = mModel.mEnd;
                int which = -1;
                switch (mModification) {
                    case Utils.MODIFY_SELECTED:
                        which = DeleteEventHelper.DELETE_SELECTED;
                        break;
                    case Utils.MODIFY_ALL_FOLLOWING:
                        which = DeleteEventHelper.DELETE_ALL_FOLLOWING;
                        break;
                    case Utils.MODIFY_ALL:
                        which = DeleteEventHelper.DELETE_ALL;
                        break;
                }
                
                DeleteEventHelper deleteHelper = new DeleteEventHelper(
                        mActivity, mActivity, !mIsReadOnly /* exitWhenDone */);
                deleteHelper.delete(begin, end, mOriginalModel, which);
            }

            //public static final int DONE_REVERT = 1 << 0;
            //public static final int DONE_EXIT = 1 << 0;
            if ((mCode & Utils.DONE_EXIT) != 0) {
                // This will exit the edit event screen, should be called
                // when we want to return to the main calendar views
                if ((mCode & Utils.DONE_SAVE) != 0) {
                    if (mActivity != null) {
                        long start = mModel.mStart;
                        long end = mModel.mEnd;
                        if (mModel.mAllDay) {
                            // For allday events we want to go to the day in the
                            // user's current tz
                            String tz = Utils.getTimeZone(mActivity, null);
                            Calendar t = GregorianCalendar.getInstance(TimeZone.getTimeZone(ETime.TIMEZONE_UTC));//Time t = new Time(Time.TIMEZONE_UTC);
                            t.setTimeInMillis(start);//t.set(start);
                            t = ETime.switchTimezone(t, TimeZone.getTimeZone(tz));//t.timezone = tz;
                            start = t.getTimeInMillis();

                            t = ETime.switchTimezone(t, TimeZone.getTimeZone(ETime.TIMEZONE_UTC));//t.timezone = Time.TIMEZONE_UTC;
                            t.setTimeInMillis(end);//t.set(start);//t.set(end);
                            t = ETime.switchTimezone(t, TimeZone.getTimeZone(tz));//t.timezone = tz;
                            //end = t.toMillis(true);
                        }
                        
                        //KCalendarController.getInstance(mActivity, mActivity).launchViewEvent(-1, start, end,
                                //Attendees.ATTENDEE_STATUS_NONE);
                    }
                }
                
                if (INFO) Log.i(TAG, "Call goodByeActivity");
                ManageEventActivity a = (ManageEventActivity) EditEventFragment.this.getActivity();
                a.goodByeActivity();
                /*Activity a = EditEventFragment.this.getActivity();
                if (a != null) {
                    a.finish();
                    a.overridePendingTransition(0, R.layout.activity_slide_down);
                    //a.finish();
                }*/
            }

            // Hide a software keyboard so that user won't see it even after this Fragment's
            // disappearing.
            final View focusedView = mActivity.getCurrentFocus();
            if (focusedView != null) {
                //mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                focusedView.clearFocus();
            }
        }
    }
    
	
	public void setModelFromOriginal(CalendarEventModel ori) {
        
        mModel.mId = ori.mId;
        
        if (!TextUtils.isEmpty(ori.mTitle)) {
        	mModel.mTitle = new String(ori.mTitle);
        }        
        
        if (!TextUtils.isEmpty(ori.mDescription)) {
        	mModel.mDescription = new String(ori.mDescription);
        }
        
        if (!TextUtils.isEmpty(ori.mLocation)) {
        	mModel.mLocation = new String(ori.mLocation);
        }
        
        mModel.mAllDay = ori.mAllDay;
        mModel.mHasAlarm = ori.mHasAlarm;
        mModel.mCalendarId = ori.mCalendarId;
        mModel.mStart = ori.mStart;
        
        if (!TextUtils.isEmpty(ori.mTimezone)) {
            mModel.mTimezone = new String(ori.mTimezone);
        }
        
        if (!TextUtils.isEmpty(ori.mRrule)) {
        	mModel.mRrule = new String(ori.mRrule);
        }
        
        if (!TextUtils.isEmpty(ori.mSyncId)) {
        	mModel.mSyncId = new String(ori.mSyncId);
        }
        
        mModel.mAvailability = ori.mAvailability;
        mModel.mRawAccessLevel = ori.mRawAccessLevel;
        
        if (!TextUtils.isEmpty(ori.mOwnerAccount)) {
        	mModel.mOwnerAccount = ori.mOwnerAccount;
        }        
        
        if (!TextUtils.isEmpty(ori.mOriginalSyncId)) {
        	mModel.mOriginalSyncId = new String(ori.mOriginalSyncId);
        }
        
        mModel.mOriginalId = ori.mOriginalId;
        
        if (!TextUtils.isEmpty(ori.mOrganizer)) {
        	mModel.mOrganizer = new String(ori.mOrganizer);
        }
        
        mModel.mIsOrganizer = ori.mIsOrganizer;
        mModel.mGuestsCanModify = ori.mGuestsCanModify;

        mModel.setEventColor(ori.getEventColor());
        
        mModel.mAccessLevel = ori.mAccessLevel;
        mModel.mEventStatus = ori.mEventStatus;

        boolean hasRRule = !TextUtils.isEmpty(mModel.mRrule);
        // We expect only one of these, so ignore the other
        if (hasRRule) {
        	mModel.mDuration = new String(ori.mDuration);
        } else {
        	mModel.mEnd = ori.mEnd;
        }

        mModel.mModelUpdatedWithEventCursor = true;  
        
        mModel.mCalendarName = ori.mCalendarName;
        
        if (!TextUtils.isEmpty(ori.mOrganizer)) {
        	mModel.mOrganizer = new String(ori.mOrganizer);
        }        
        mModel.mIsOrganizer = ori.mIsOrganizer;
        
        if (!TextUtils.isEmpty(ori.mOrganizerDisplayName)) {
        	mModel.mOrganizerDisplayName = new String(ori.mOrganizerDisplayName);
        }
        
        mModel.mHasAttendeeData = ori.mHasAttendeeData;
        mModel.mOwnerAttendeeId = ori.mOwnerAttendeeId;
        mModel.mSelfAttendeeStatus = ori.mSelfAttendeeStatus;
        
        for (Iterator<Entry<String, Attendee>> atte =
        		ori.mAttendeesList.entrySet().iterator(); atte.hasNext();) {
		 
    		Entry<String, Attendee> entry = atte.next();
    		//String key = entry.getKey();    		
    		Attendee att = entry.getValue();   		
    		
    		int id = att.mID;
            String name = new String(att.mName);
            String email = new String(att.mEmail);
            int status = att.mStatus;
            int relationship = att.mRelationShip;
            
            String identity = null;
            if (!TextUtils.isEmpty(att.mIdentity)) {
            	identity = new String(att.mIdentity);
            }            
            
            String idNamespace = null;
            if (!TextUtils.isEmpty(att.mIdNamespace)) {
            	idNamespace = new String(att.mIdNamespace);
            }
            
            Attendee attendee = new Attendee(id, name, email, status, identity, idNamespace, relationship);
            mModel.addAttendee(attendee);
    	}	        
        
        int reminderNumbers = ori.mReminders.size();
        if (reminderNumbers > 0) {
        	mModel.mReminders.clear();        	
        }
        
        for (int i=0; i<reminderNumbers; i++) {
        	ReminderEntry oriObj = ori.mReminders.get(i);
        	ReminderEntry re = ReminderEntry.valueOf(oriObj.getMinutes(), oriObj.getMethod());
        	mModel.mReminders.add(re);
        }
            
        
        if (ori.mEventColorCache != null) {
        	EventColorCache cache = new EventColorCache();
            Map<String, Integer> colorKeyMap = new HashMap<String, Integer>();
            Map<String, ArrayList<Integer>> colorPaletteMap = new HashMap<String, ArrayList<Integer>>();
            
        	Map<String, Integer> oriColorKeyMap = ori.mEventColorCache.getColorKeyMap();
            Map<String, ArrayList<Integer>> oirColorPaletteMap = ori.mEventColorCache.getColorPaletteMap();
            
            for (Iterator<Entry<String, Integer>> colorKey = oriColorKeyMap.entrySet().iterator(); colorKey.hasNext();) {
            	Entry<String, Integer> entry = colorKey.next();
            	String key = entry.getKey();
            	Integer value = entry.getValue();
            	colorKeyMap.put(key, value);
            }
            cache.setColorKeyMap(colorKeyMap);
            
            for (Iterator<Entry<String, ArrayList<Integer>>> colorPalette = oirColorPaletteMap.entrySet().iterator(); colorPalette.hasNext();) {
            	Entry<String, ArrayList<Integer>> entry = colorPalette.next();
            	String key = entry.getKey();
            	ArrayList<Integer> value = entry.getValue();
            	colorPaletteMap.put(key, value);
            }
            cache.setColorPaletteMap(colorPaletteMap);
            cache.sortPalettes(new HsvColorComparator());
            
            mModel.mEventColorCache = cache;	
        }        
        
        //////////////////////////////////////////////////////////
        if (ori.mCalendarId == -1) {
            return ;
        }

        
        if (!ori.mModelUpdatedWithEventCursor) {            
            return ;
        }

        mModel.mOrganizerCanRespond = ori.mOrganizerCanRespond;
        
        if (!TextUtils.isEmpty(ori.mCalendarOwnerAccount)) {
        	mModel.mCalendarOwnerAccount = new String(ori.mCalendarOwnerAccount);
        }
        
        mModel.mCalendarAccessLevel = ori.mCalendarAccessLevel;
        
        if (!TextUtils.isEmpty(ori.mCalendarDisplayName)) {
        	mModel.mCalendarDisplayName = new String(ori.mCalendarDisplayName);
        }
        
        mModel.setCalendarColor(ori.getCalendarColor());

        if (!TextUtils.isEmpty(ori.mCalendarAccountName)) {
        	mModel.mCalendarAccountName = new String(ori.mCalendarAccountName);
        }
        
        if (!TextUtils.isEmpty(ori.mCalendarAccountType)) {
        	mModel.mCalendarAccountType = new String(ori.mCalendarAccountType);
        }
        
        
        mModel.mCalendarMaxReminders = ori.mCalendarMaxReminders;
        
        if (!TextUtils.isEmpty(ori.mCalendarAllowedReminders)) {
        	mModel.mCalendarAllowedReminders = new String(ori.mCalendarAllowedReminders);
        }
        
        
        if (!TextUtils.isEmpty(ori.mCalendarAllowedAttendeeTypes)) {
        	mModel.mCalendarAllowedAttendeeTypes = new String(ori.mCalendarAllowedAttendeeTypes);
        }
        
        if (!TextUtils.isEmpty(ori.mCalendarAllowedAvailability)) {
        	mModel.mCalendarAllowedAvailability = new String(ori.mCalendarAllowedAvailability);
        }
        
       
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
    
    private class CalendarsAndColorsQueryHandler extends AsyncQueryHandler {		
	    public CalendarsAndColorsQueryHandler(ContentResolver cr) {
	        super(cr);
	    }
	
	    @Override
	    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
	        // If the query didn't return a cursor for some reason return
	        if (cursor == null) {
	            return;
	        }
	
	        // If the Activity is finishing, then close the cursor.
	        // Otherwise, use the new cursor in the adapter.
	        final Activity activity = EditEventFragment.this.getActivity();
	        if (activity == null || activity.isFinishing()) {
	            cursor.close();
	            return;
	        }
	        	        
	        switch (token) {
	            
	            case TOKEN_CALENDARS:
	                try {
	                	boolean addedToActivity = isAdded();
                        boolean resumed = isResumed();
	                	mEditEvent.makeCalendarInfoList(cursor, addedToActivity && resumed);
	                	mEditEvent.makeSubCalendarPageOfCalendarPage();
	                } finally {
	                    cursor.close();
	                }
	                        
	                break;
	            case TOKEN_COLORS:
	                if (cursor.moveToFirst()) {
	                    EventColorCache cache = new EventColorCache();
	                    do
	                    {
	                        int colorKey = cursor.getInt(EditEventHelper.COLORS_INDEX_COLOR_KEY);
	                        int rawColor = cursor.getInt(EditEventHelper.COLORS_INDEX_COLOR);
	                        int displayColor = Utils.getDisplayColorFromColor(rawColor);
	                        String accountName = cursor
	                                .getString(EditEventHelper.COLORS_INDEX_ACCOUNT_NAME);
	                        String accountType = cursor
	                                .getString(EditEventHelper.COLORS_INDEX_ACCOUNT_TYPE);
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
	
	                //setModelIfDone(TOKEN_COLORS);	                
	                break;
	            default:
	                cursor.close();
	                break;
	       }
	   }
	}
	
    
    public static int calcFrameLayoutHeight(Activity activity) {		
		int deviceHeight = activity.getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = activity.getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	int StatusBarHeight = Utils.getSharedPreference(activity.getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(activity.getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight(activity.getResources()));        	
        }
    	
    	int upperActionBarHeight = (int)activity.getResources().getDimension(R.dimen.settings_view_upper_actionbar_height);      	
    	
    	int frameLayoutHeight = deviceHeight - StatusBarHeight - upperActionBarHeight;  
    	return frameLayoutHeight;
	}	
	
	
	
	public static int getStatusBarHeight(Resources res) {    	
    	int result = 0;
    	int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = res.getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
	
    public class CalendarInfo {
    	String mDisplayName;
    	String mAccountName;
    	String mAccountType;
    	public CalendarInfo(String displayName, String accountName, String accountType) {
    		mDisplayName = displayName;
        	mAccountName = accountName;
        	mAccountType = accountType;
    	}
    }
    
    
    
    public static class EventBundle implements Serializable {
        private static final long serialVersionUID = 2L;
        public long id = -1;
        public long start = -1;
        public long end = -1;
       
    }
}
