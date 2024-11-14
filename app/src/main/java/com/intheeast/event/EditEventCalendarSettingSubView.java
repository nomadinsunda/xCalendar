package com.intheeast.event;

import java.util.ArrayList;

import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.event.EditEvent.CalendarInfo;
import com.intheeast.settings.SettingsFragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.provider.CalendarContract;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EditEventCalendarSettingSubView extends LinearLayout {

	public static final String GOOGLE_ACCOUNT_TYPE = "com.google";
	public static final String ECALENDAR_OWNER_ACCOUNT = SettingsFragment.DEFAULT_CALENDAR;
	public static final String ECALENDAR_ACCOUNT_NAME = "ECalendar";
	public static final String ECALENDAR_ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL;
	public static final String ECALENDAR_INT_NAME_PREFIX = "Local_";
	
	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;
	//EditEventActionBarFragment mActionBar;
	EditEvent mEditEvent;
	
	
	LinearLayout mKifeCalendarsContainer;
	LinearLayout mGmailCalendarsContainer;
	ArrayList<LinearLayout> mKifeCalendarsContainerList = new ArrayList<LinearLayout>();
	ArrayList<LinearLayout> mGmailCalendarsContainerList = new ArrayList<LinearLayout>();
	
	ImageView mCalendarPageItemSelectionIcon;
	RelativeLayout.LayoutParams mCalendarPageItemSelectionIconImageViewLayout;
	
	
	public EditEventCalendarSettingSubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventCalendarSettingSubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventCalendarSettingSubView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	} 
    
	public void initView(Activity activity, EditEventFragment fragment, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
		//mActionBar = fragment.getActionBar();
		//mActivity = fragment.getActivity();
		
		mEditEvent = fragment.mEditEvent;
		
		mKifeCalendarsContainer = (LinearLayout)findViewById(R.id.klife_calendar_list_container);
		
		mGmailCalendarsContainer = (LinearLayout)findViewById(R.id.gmail_calendar_list_container);
		
		mCalendarPageItemSelectionIcon = new ImageView(mActivity);
		mCalendarPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mCalendarPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		
		mCalendarPageItemSelectionIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mCalendarPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mCalendarPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);		
	}
		
	public void addGmailItem() {
		mEditEvent.mBetweenRepeatAndAttendeeSeperatorLayout.setVisibility(View.VISIBLE);
		mEditEvent.mAttendeeLayout.setVisibility(View.VISIBLE);
		
		mEditEvent.mBetweenCalendarAndAvailablitySeperatorLayout.setVisibility(View.VISIBLE);	
		mEditEvent.mAvailabilityLayout.setVisibility(View.VISIBLE);

		mEditEvent.mBetweenAvailablityAndVisibilitySeperatorLayout.setVisibility(View.VISIBLE);
		mEditEvent.mVisibilityLayout.setVisibility(View.VISIBLE);
	}
	
	public void removeGmailItem() {
		mEditEvent.mBetweenRepeatAndAttendeeSeperatorLayout.setVisibility(View.GONE);
		mEditEvent.mAttendeeLayout.setVisibility(View.GONE);
		
		mEditEvent.mBetweenCalendarAndAvailablitySeperatorLayout.setVisibility(View.GONE);	
		mEditEvent.mAvailabilityLayout.setVisibility(View.GONE);

		mEditEvent.mBetweenAvailablityAndVisibilitySeperatorLayout.setVisibility(View.GONE);
		mEditEvent.mVisibilityLayout.setVisibility(View.GONE);
	}
	
	public void makeCalendarPageSubCalendarLayout() {
		boolean initCalendarIsGMAIL = false;
		String service = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater)mActivity.getApplicationContext().getSystemService(service);

        //this.mCalendarID;
        int numbers = mEditEvent.mCalendarInfoList.size();
        for (int i=0; i<numbers; i++) {
        	CalendarInfo obj = mEditEvent.mCalendarInfoList.get(i);
        	
	        RelativeLayout rl =(RelativeLayout)li.inflate(R.layout.calendar_info_layout, mKifeCalendarsContainer, false);
	        rl.setId(obj.mCalendarId); // RelativeLayoutID�� �� �����Ϸ��� �ϴ� Ķ������ ID��!!!
	        rl.setOnClickListener(mCalendarPageItemClickListener);
	        	        
	        TextView calendarDisplayName = (TextView)rl.findViewById(R.id.calendar_text);
	        calendarDisplayName.setText(obj.mDisplayName);
	        
	        View colorIcon =(View)rl.findViewById(R.id.calendar_color_icon);
	        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
	    	Paint paint = shapeDrawable.getPaint();		
	    	paint.setColor(Utils.getDisplayColorFromColor(obj.mCalendarColor));
	    	colorIcon.setBackground(shapeDrawable);
	    	
	    	if (mEditEvent.mCalendarID == obj.mCalendarId) {
	    		//attachCalendarPageItemSelectionIcon(service, service, i);
	    		rl.addView(mCalendarPageItemSelectionIcon, mCalendarPageItemSelectionIconImageViewLayout);
	    		
	    		if (obj.mAccountType.equals(GOOGLE_ACCOUNT_TYPE)) {
	    			initCalendarIsGMAIL = true;
	    		}    		
	    	}
	    	
	    	if (obj.mAccountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL)) {
	    		if (obj.mAccountName.equals(ECALENDAR_ACCOUNT_NAME)) {
	    			mKifeCalendarsContainer.addView(rl);
	    		}
	    	}
	    	else if (obj.mAccountType.equals(GOOGLE_ACCOUNT_TYPE)){
	    		mGmailCalendarsContainer.addView(rl);
	    	}
        }
        
        if (initCalendarIsGMAIL) {
        	addGmailItem();
        }
	}
	
	
	View.OnClickListener mCalendarPageItemClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
        	
        	int viewID = v.getId(); // viewID�� �� �����Ϸ��� �ϴ� Ķ������ ID��!!!
        	
        	int numbers = mEditEvent.mCalendarInfoList.size();
        	String accountName = null;
        	String accountType = null;
        	String dispName = null;
        	
        	int color = -1;
        	int selectedCalendarID = -1;
            for (int i=0; i<numbers; i++) {
            	CalendarInfo obj = mEditEvent.mCalendarInfoList.get(i);
            	
            	if (viewID == obj.mCalendarId) {
            		selectedCalendarID = obj.mCalendarId;
            		accountName = obj.mAccountName;
            		accountType = obj.mAccountType;
            		dispName = obj.mDisplayName;
            		color = obj.mCalendarColor;
            		break;
            	}
            }  	
        	
            if (selectedCalendarID == mEditEvent.mCalendarID)
            	return;
        	
            detachCalendarPageItemSelectionIcon(mEditEvent.mCalendarID);
        	
            //mEditEvent.setSelectedCalendar(selectedCalendarID);
            updateEventCalendarItem(dispName, color);
        	attachCalendarPageItemSelectionIcon(accountName, accountType, mEditEvent.mCalendarID);    
        	//settingViewSwitching(false, EVENT_CALENDAR_PAGE); 
        	//mFragment.getActionBar().setMainViewActionBar(mResources.getString(R.string.editevent_calendarpage_actionbar_title));  
        	//mCurrentViewingPage = EVENT_MAIN_PAGE;
        }
    };
    
    
    
    
    public void attachCalendarPageItemSelectionIcon(String accountName, String accountType, int itemValue) {
    	if (accountName.equals(ECALENDAR_ACCOUNT_NAME)) {
    		RelativeLayout rl =(RelativeLayout)mKifeCalendarsContainer.findViewById(itemValue);
    		rl.addView(mCalendarPageItemSelectionIcon, mCalendarPageItemSelectionIconImageViewLayout);
    		
    		removeGmailItem();    		
    	}
    	else if (accountType.equals(GOOGLE_ACCOUNT_TYPE)){
    		RelativeLayout rl =(RelativeLayout)mGmailCalendarsContainer.findViewById(itemValue);
    		rl.addView(mCalendarPageItemSelectionIcon, mCalendarPageItemSelectionIconImageViewLayout);
    		
    		addGmailItem();
    	}    	
	}
	
	public void detachCalendarPageItemSelectionIcon(int itemValue) {		
    	String accountName = null;    	
    	String accountType = null;
    	
    	int numbers = mEditEvent.mCalendarInfoList.size();
        for (int i=0; i<numbers; i++) {
        	CalendarInfo obj = mEditEvent.mCalendarInfoList.get(i);
        	if (itemValue == obj.mCalendarId) {        		
        		accountName = obj.mAccountName;   
        		accountType = obj.mAccountType;
        		break;
        	}
        }
        
        if (accountName.equals(ECALENDAR_ACCOUNT_NAME)) {
    		RelativeLayout rl =(RelativeLayout)mKifeCalendarsContainer.findViewById(itemValue);
    		rl.removeView(mCalendarPageItemSelectionIcon);
    	}
    	else if (accountType.equals(GOOGLE_ACCOUNT_TYPE)){
    		RelativeLayout rl =(RelativeLayout)mGmailCalendarsContainer.findViewById(itemValue);
    		rl.removeView(mCalendarPageItemSelectionIcon);
    	}    
        
	}
    
	public void updateEventCalendarItem(String DispName, int selectedCalendarColor) {		
		View calendarColorIcon = mEditEvent.mMainPageView.findViewById(R.id.calendar_color_icon);
		
		ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
		Paint paint = shapeDrawable.getPaint();		
		paint.setColor(Utils.getDisplayColorFromColor(selectedCalendarColor));
		
		calendarColorIcon.setBackground(shapeDrawable);	

		mEditEvent.mEventCalendarText.setText(DispName);	
	}    
	
	
	//public static final Account ACCOUNT = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);

	
    
    
    
    
}
