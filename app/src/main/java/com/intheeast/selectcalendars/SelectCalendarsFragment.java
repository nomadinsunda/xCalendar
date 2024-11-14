package com.intheeast.selectcalendars;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.CommonLinearLayoutItemContainer;
import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ITESwitch;
import com.intheeast.etc.LockableScrollView;


import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.LinearLayout.LayoutParams;

@SuppressLint("ValidFragment")
public class SelectCalendarsFragment extends Fragment {

	private static final String TAG = SelectCalendarsFragment.class.getSimpleName();
	private static boolean INFO = true;
	
	public static final int MAIN_VIEW_FRAGMENT_STATE = 1;
	public static final int ENTIRE_EDIT_VIEW_FRAGMENT_STATE = 2; // �׼ǹ��� ���� ���� ��ư�� ���� �����ε�...���� �̱����̴�...��Ȯ�� ���� �ó������� ����
	public static final int SEPERATE_EDIT_VIEW_FRAGMNET_STATE = 3;
	
	private final int ALL_CALENDAR_UNVISIBLE_STATE = 0; 
	private final int ALL_CALENDAR_VISIBLE_STATE = 1; 
	
	private OnMainFragmentInitCompletionListener mMainFragmentInitCompletionListener;
	
	public interface OnMainFragmentInitCompletionListener {
        void onMainFragmentInitCompletion();
    }
	
	public void setOnMainFragmentInitCompletionListener(OnMainFragmentInitCompletionListener l) {
		mMainFragmentInitCompletionListener = l;
    }
	
	SelectCalendarsActivity mActivity;
	Context mContext;
    DisplayMetrics mDisplayMetrics;
	LayoutInflater mInflater;
	private final ContentResolver mResolver;
	
	LinearLayout mFrameLayout;
	ViewSwitcher mFrameLayoutViewSwitcher;
	
	RelativeLayout mSelectCalendarsLinearLayout;
	LinearLayout mEditCalendarsLinearLayout;
	LockableScrollView mSelectCalendarsScrollView;
	LinearLayout mInnerLinearLayoutOfScrollView;
	CommonRelativeLayoutItemContainer mAllCalendarVisibleToggleContainer; 
	TextView mAllCalendarVisibleToggleText;
	LinearLayout mMyKCalendarGroupContainer;
	
	CommonRelativeLayoutItemContainer mTargetCalendarNameContainer;
	TextView mTargetCalendarNameTextView;
	
	CommonLinearLayoutItemContainer mTargetCalendarColorContainer;
	
	ArrayList<CommonLinearLayoutItemContainer> mCalendarGroupContainerList;
	
	SelectCalendarsViewActionBarFragment mActionBar;
	
	int mViewState;
	
	private Cursor mAccountsCursor = null;	
	
	private static final String IS_PRIMARY = "\"primary\"";
    private static final String CALENDARS_ORDERBY = IS_PRIMARY + " DESC,"
            + Calendars.CALENDAR_DISPLAY_NAME + " COLLATE NOCASE";
    private static final String ACCOUNT_SELECTION = Calendars.ACCOUNT_NAME + "=?"
            + " AND " + Calendars.ACCOUNT_TYPE + "=?";
	private static final String[] PROJECTION = new String[] {
	      Calendars._ID,
	      Calendars.ACCOUNT_NAME,
	      Calendars.OWNER_ACCOUNT,
	      Calendars.NAME,
	      Calendars.CALENDAR_DISPLAY_NAME,
	      Calendars.CALENDAR_COLOR,
	      Calendars.VISIBLE,
	      Calendars.SYNC_EVENTS,
	      "(" + Calendars.ACCOUNT_NAME + "=" + Calendars.OWNER_ACCOUNT + ") AS " + IS_PRIMARY,
	      Calendars.ACCOUNT_TYPE
	    };
	
	//Keep these in sync with the projection
    private static final int ID_COLUMN = 0;
    private static final int ACCOUNT_COLUMN = 1;
    private static final int OWNER_COLUMN = 2;
    private static final int NAME_COLUMN = 3;
    private static final int DISPLAY_NAME_COLUMN = 4;
    private static final int COLOR_COLUMN = 5;
    private static final int SELECTED_COLUMN = 6;
    private static final int SYNCED_COLUMN = 7;
    private static final int PRIMARY_COLUMN = 8;
    private static final int ACCOUNT_TYPE_COLUMN = 9;
    
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription
    = new HashMap<String, AuthenticatorDescription>();
    protected AuthenticatorDescription[] mAuthDescs;
    
    // This is for keeping MatrixCursor copies so that we can requery in the background.
 	private Map<String, Cursor> mChildrenCursors = new HashMap<String, Cursor>();
 	
 	// This is to keep track of whether or not multiple calendars have the same display name
    private static HashMap<String, Boolean> mIsDuplicateName = new HashMap<String, Boolean>();
    
    private HashMap<String, String> mCurrentCalendarGroupsByApp = new HashMap<String, String>();

    private AsyncCalendarsUpdater mCalendarsUpdater;
    
    // Flag for when the cursors have all been closed to ensure no race condition with queries.
    private boolean mClosedCursorsFlag;
    
    private class CalendarInfo {
    	long id;
    	final int visibleState;
    	int calendarColor;
    	String calendarName;
    	String calendarDispName;
    	int currentCheckStatusOfVisibleState;
    	CommonRelativeLayoutItemContainer itemContainer;
    	
    	public CalendarInfo(long id, int visibleState, int calendarColor, String calendarName, String calendarDispName, CommonRelativeLayoutItemContainer itemContainer) {
    		this.id = id;
    		this.visibleState = visibleState;
    		this.calendarColor = calendarColor;
    		this.calendarName = calendarName;
    		this.calendarDispName = calendarDispName;
    		this.currentCheckStatusOfVisibleState = this.visibleState;
    		this.itemContainer = itemContainer;
    	}
    	
    	public long getCalendarId() {
    		return this.id;
    	}
    	
    	public int getVisibleState() {
    		return this.visibleState;
    	}
    	
    	public void setCurrentCheckStatusOfVisibleState(int state) {
    		this.currentCheckStatusOfVisibleState = state;
    	}
    	
    	public int getCurrentCheckStatusOfVisibleState() {
    		return this.currentCheckStatusOfVisibleState;
    	}
    	
    	public CommonRelativeLayoutItemContainer getItemContainerView() {
    		return this.itemContainer;
    	}
    	
    }

    private class CalendarGroupInfo {
    	String groupAccountType;
    	String groupCalendarLabel;
    	ArrayList<CalendarInfo> calendarInfoList;
    	
    	public CalendarGroupInfo(String groupAccountType, String groupCalendarLabel) {
    		this.groupAccountType = groupAccountType;
    		this.groupCalendarLabel = groupCalendarLabel;
    		calendarInfoList = new ArrayList<CalendarInfo>();
    	}
    	
    	public void addCalendarInfo(CalendarInfo obj) {
    		calendarInfoList.add(obj);
    	}
    	
    	public int getCalendarInfoCount() {
    		return calendarInfoList.size();
    	}
    	
    	public CalendarInfo getSeperateCalendarInfo(int index) {
    		return calendarInfoList.get(index);
    	}
    	
    }
    
    OnClickListener mAllCalendarVisibleToggleContainerClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
						
			int visibility = View.VISIBLE;
			
			int visibleState = (Integer) mAllCalendarVisibleToggleText.getTag(); 
			if (visibleState == ALL_CALENDAR_UNVISIBLE_STATE) {
				// toggle
				mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_VISIBLE_STATE);
				mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_unvisible_state));
				visibility = View.VISIBLE;    		
			}
			else if (visibleState == ALL_CALENDAR_VISIBLE_STATE) {
				// toggle
				mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_UNVISIBLE_STATE);
				mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_visible_state));
				visibility = View.INVISIBLE;	    		
			}
			
			int size = mCalendarGroupContainerList.size();
	    	for (int i=0; i<size; i++) {
	    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
	    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
	    		int count = groupInfo.getCalendarInfoCount(); 
	    		for (int j=0; j<count; j++) {
	    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);
	    			CommonRelativeLayoutItemContainer itemContainer = calendarInfo.getItemContainerView();
	    			@SuppressLint("ResourceType") ImageView visibleMarkIcon = (ImageView) itemContainer.findViewById(1);
	    			
	    			if (visibility == View.INVISIBLE) {
	    				calendarInfo.setCurrentCheckStatusOfVisibleState(0);
	    			}
	    			else if (visibility == View.VISIBLE) {
	    				calendarInfo.setCurrentCheckStatusOfVisibleState(1);
	    			}
	    			
	    			visibleMarkIcon.setVisibility(visibility);
	    		}
	    	}
		}
    	
    };
        
    OnClickListener mCalendarItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (INFO) Log.i(TAG, "+mCalendarItemClickListener");
			CommonRelativeLayoutItemContainer calendarGroupItem = (CommonRelativeLayoutItemContainer) v;
			long calendarId = (Long) calendarGroupItem.getTag();
			
			int size = mCalendarGroupContainerList.size();
	    	for (int i=0; i<size; i++) {
	    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
	    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
	    		int count = groupInfo.getCalendarInfoCount(); 
	    		for (int j=0; j<count; j++) {
	    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);
	    			if (calendarId == calendarInfo.getCalendarId()) {
	    				
	    				if (INFO) Log.i(TAG, "b currentCheckStatusOfVisibleState=" + String.valueOf(calendarInfo.getCurrentCheckStatusOfVisibleState()));
		    			@SuppressLint("ResourceType") ImageView visibleMarkIcon = (ImageView) calendarGroupItem.findViewById(1);
		    			if (visibleMarkIcon.getVisibility() == View.VISIBLE) {
		    				visibleMarkIcon.setVisibility(View.INVISIBLE);
		    				calendarInfo.setCurrentCheckStatusOfVisibleState(0);
		    			}
		    			else if (visibleMarkIcon.getVisibility() == View.INVISIBLE) {
		    				visibleMarkIcon.setVisibility(View.VISIBLE);
		    				calendarInfo.setCurrentCheckStatusOfVisibleState(1);
		    			}
		    			
		    			CalendarInfo calendarInfo_b = groupInfo.getSeperateCalendarInfo(j);
		    			if (calendarInfo.getCurrentCheckStatusOfVisibleState() != calendarInfo_b.getCurrentCheckStatusOfVisibleState())
		    			if (INFO) Log.i(TAG, "oooops!!!");
	    			}
	    			
	    			
	    		}
	    	}
	    	
	    	setAllCalendarVisibleUnVisibleStateText();
	    	
	    	if (INFO) Log.i(TAG, "-mCalendarItemClickListener");
		}
		
	};    
    
    private static final int ENTIRE_REFRESH_CALENDARS_COMPLETION = 1;
    SelectCalendarsHandler mSelectCalendarsHandler;
    private class SelectCalendarsHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
             
            switch (msg.what) {     
            case ENTIRE_REFRESH_CALENDARS_COMPLETION:
            	String ecc = ((String)mECalendarsCookie).toString();
            	String gcc =((String)mGoogleCalendarsCookie).toString();
            	
                synchronized (mChildrenCursors) {
                    for (String key : mChildrenCursors.keySet()) {
                    	
                    	if (key.compareTo((String) mECalendarsCookie) == 0) {
                    		Cursor eCalendarCursor = mChildrenCursors.get(mECalendarsCookie);
                    		
                    		String eCalendarGroupAccountType = mResources.getString(R.string.ecalendar_psudo_account_type);
                        	String eCalendarGroupCalendarLabel = mCurrentCalendarGroupsByApp.get(mResources.getString(R.string.ecalendar_psudo_account_type));    
                    		CalendarGroupInfo eCalendarGroupInfo = new CalendarGroupInfo(eCalendarGroupAccountType, eCalendarGroupCalendarLabel);
                    		
                        	eCalendarCursor.moveToPosition(-1);
                            while (eCalendarCursor.moveToNext()) {
                            	long calendarId = eCalendarCursor.getInt(ID_COLUMN);
                        		int visibleState = eCalendarCursor.getInt(SELECTED_COLUMN);   
                        		int calendarColor = Utils.getDisplayColorFromColor(eCalendarCursor.getInt(COLOR_COLUMN));  
                        		String calendarName = eCalendarCursor.getString(NAME_COLUMN);
                        		String calendarDispName = eCalendarCursor.getString(DISPLAY_NAME_COLUMN); 
                        		
                            	CommonRelativeLayoutItemContainer itemContainer = makeCalendarGroupContainerItem((String)mECalendarsCookie, calendarId, visibleState, calendarColor, calendarDispName);
                            	itemContainer.setTag(calendarId);
                            	
                            	CalendarInfo obj = new CalendarInfo(calendarId, visibleState, calendarColor, calendarName, calendarDispName, itemContainer); 
                            	eCalendarGroupInfo.addCalendarInfo(obj);
                            	mECalendarGroupContainer.addView(itemContainer);
                            }
                            
                            mECalendarGroupContainer.setTag(eCalendarGroupInfo);
                    	}
                    	else if (key.compareTo((String) mGoogleCalendarsCookie) == 0) {
                    		     
                    		String[] strs = key.split("#");
                    		String googleGroupAccountType = strs[0];
                    		String account = strs[1];
                    		
                    		// mCurrentCalendarGroupsByApp.put(accountType, accountLabel.toString());
                    		String googleGroupCalendarLabel = mCurrentCalendarGroupsByApp.get(googleGroupAccountType);
                    		                       	
                    		Cursor googleCalendarsCursor = mChildrenCursors.get(key);
                    		
                    		CalendarGroupInfo googleGroupInfo = new CalendarGroupInfo(googleGroupAccountType, googleGroupCalendarLabel);
                            
                    		googleCalendarsCursor.moveToPosition(-1);
                            while (googleCalendarsCursor.moveToNext()) {     
                                
                            	long calendarId = googleCalendarsCursor.getInt(ID_COLUMN);
                        		int visibleState = googleCalendarsCursor.getInt(SELECTED_COLUMN);   
                        		int calendarColor = Utils.getDisplayColorFromColor(googleCalendarsCursor.getInt(COLOR_COLUMN));    
                        		String calendarName = googleCalendarsCursor.getString(NAME_COLUMN);
                        		String calendarDispName = googleCalendarsCursor.getString(DISPLAY_NAME_COLUMN); 
                        		                      		
                        		
                            	CommonRelativeLayoutItemContainer itemContainer = makeCalendarGroupContainerItem(key, calendarId, visibleState, calendarColor, calendarDispName);
                            	itemContainer.setTag(calendarId);
                            	
                            	CalendarInfo obj = new CalendarInfo(calendarId, visibleState, calendarColor, calendarName, calendarDispName, itemContainer);
                            	googleGroupInfo.addCalendarInfo(obj);
                            	mGoogleCalendarsGroupContainer.addView(itemContainer);	                                
                            }
                            
                            mGoogleCalendarsGroupContainer.setTag(googleGroupInfo);
                    	}
                    	else {
                    		String[] strs = key.split("#");
                    		String otherGroupAccountType = strs[0];
                    		String account = strs[1];
                    		// ������...
                    		String otherGroupCalendarLabel = mCurrentCalendarGroupsByApp.get(otherGroupAccountType);
                    		
                    		if (otherGroupAccountType.compareTo("LOCAL") == 0) {                 			
		                       	
                        		Cursor otherCalendarsCursor = mChildrenCursors.get(key);
                        		
                        		//CalendarGroupInfo otherGroupInfo = null;
                        		CalendarGroupInfo otherGroupInfo = (CalendarGroupInfo) mOtherCalendarsGroupContainer.getTag();                        		
                        		//Object tagObj = mOtherCalendarsGroupContainer.getTag();                        		
                        		
                        		if (otherGroupInfo == null)
                        			otherGroupInfo = new CalendarGroupInfo(otherGroupAccountType, otherGroupCalendarLabel);
                                
                        		otherCalendarsCursor.moveToPosition(-1);
                                while (otherCalendarsCursor.moveToNext()) {     
                                    
                                	long calendarId = otherCalendarsCursor.getInt(ID_COLUMN);
                            		int visibleState = otherCalendarsCursor.getInt(SELECTED_COLUMN);   
                            		int calendarColor = Utils.getDisplayColorFromColor(otherCalendarsCursor.getInt(COLOR_COLUMN));  
                            		String calendarName = otherCalendarsCursor.getString(NAME_COLUMN);
                            		String calendarDispName = otherCalendarsCursor.getString(DISPLAY_NAME_COLUMN); 
                            		                            		
                                	CommonRelativeLayoutItemContainer itemContainer = makeCalendarGroupContainerItem(key, calendarId, visibleState, calendarColor, calendarDispName);
                                	itemContainer.setTag(calendarId);
                                	
                                	CalendarInfo obj = new CalendarInfo(calendarId, visibleState, calendarColor, calendarName, calendarDispName, itemContainer);
                                	otherGroupInfo.addCalendarInfo(obj);
                                	mOtherCalendarsGroupContainer.addView(itemContainer);	                                
                                }
                                                                
                                mOtherCalendarsGroupContainer.setTag(otherGroupInfo);
                    		}                    		
                    		else {
                    			CommonLinearLayoutItemContainer targetContainer = null;
                        		
                        		int size = mNonFixGroupCalendarContainerList.size();
                        		for (int i=0; i<size; i++) {
                        			CommonLinearLayoutItemContainer container = mNonFixGroupCalendarContainerList.get(i);
                        			String calendarLabel = (String) container.getTag();
                        			if (otherGroupCalendarLabel.compareTo(calendarLabel) == 0) {
                        				targetContainer = container;
                        				break;
                        			}
                        		}
                        		                    		
                        		if (targetContainer == null) {
                        			// Ooooops!!!
                        		}
                        		                       	
                        		Cursor calendarsCursor = mChildrenCursors.get(key);
                        		
                        		CalendarGroupInfo groupInfo = new CalendarGroupInfo(otherGroupAccountType, otherGroupCalendarLabel);
                                
                        		calendarsCursor.moveToPosition(-1);
                                while (calendarsCursor.moveToNext()) {     
                                    
                                	long calendarId = calendarsCursor.getInt(ID_COLUMN);
                            		int visibleState = calendarsCursor.getInt(SELECTED_COLUMN);   
                            		int calendarColor = Utils.getDisplayColorFromColor(calendarsCursor.getInt(COLOR_COLUMN));    
                            		String calendarName = calendarsCursor.getString(NAME_COLUMN);
                            		String calendarDispName = calendarsCursor.getString(DISPLAY_NAME_COLUMN); 
                            		
                                	CommonRelativeLayoutItemContainer itemContainer = makeCalendarGroupContainerItem(key, calendarId, visibleState, calendarColor, calendarDispName);
                                	itemContainer.setTag(calendarId);
                                	
                                	CalendarInfo obj = new CalendarInfo(calendarId, visibleState, calendarColor, calendarName, calendarDispName, itemContainer);
                                	groupInfo.addCalendarInfo(obj);
                                	targetContainer.addView(itemContainer);	                                
                                }
                                
                                targetContainer.setTag(groupInfo);
                    		}
                    	}                   	
                    }                            
                }  
                
                
                mCalendarGroupContainerList.add(mECalendarGroupContainer);
                mInnerLinearLayoutOfScrollView.addView(mECalendarGroupContainer);
                mCalendarGroupContainerList.add(mGoogleCalendarsGroupContainer);
                mInnerLinearLayoutOfScrollView.addView(mGoogleCalendarsGroupContainer);
                if (mNonFixGroupCalendarContainerList != null) {
                	int size = mNonFixGroupCalendarContainerList.size();
                	for (int i=0; i<size; i++) {
                		CommonLinearLayoutItemContainer container = mNonFixGroupCalendarContainerList.get(i);
                		mCalendarGroupContainerList.add(container);
                		mInnerLinearLayoutOfScrollView.addView(container);
                	}
                }
                mCalendarGroupContainerList.add(mOtherCalendarsGroupContainer);
                mInnerLinearLayoutOfScrollView.addView(mOtherCalendarsGroupContainer);
                                
            	initAllCalendarVisibleUnVisibleStateText();
            	            	
            	makeMainViewLowerLayout();
            	
            	mMainFragmentInitCompletionListener.onMainFragmentInitCompletion();
            	break;
            }
            
        }         
    }   
    
	public void initAllCalendarVisibleUnVisibleStateText() {
    	    	
    	boolean IsAllCalendarVisibleState = true;
    	int size = mCalendarGroupContainerList.size();
    	for (int i=0; i<size; i++) {
    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
    		int count = groupInfo.getCalendarInfoCount(); 
    		for (int j=0; j<count; j++) {
    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);
    			if (calendarInfo.getVisibleState() == 0) {
    				IsAllCalendarVisibleState = false;    				
    			}
    		}
    	}
    	
    	if (IsAllCalendarVisibleState) {
    		mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_unvisible_state));
    		mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_VISIBLE_STATE);
    	}
    	else {
    		mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_visible_state));
    		mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_UNVISIBLE_STATE);
    	}
    }
    
    public void setAllCalendarVisibleUnVisibleStateText() {
    	
    	boolean IsAllCalendarVisibleState = true;
    	int size = mCalendarGroupContainerList.size();
    	for (int i=0; i<size; i++) {
    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
    		int count = groupInfo.getCalendarInfoCount(); 
    		for (int j=0; j<count; j++) {
    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);
    			if (calendarInfo.getCurrentCheckStatusOfVisibleState() == 0) {
    				if (INFO) Log.i(TAG, "setAllCalendarVisibleUnVisibleStateText:UnVisible Calendar=" + calendarInfo.calendarName);
    				IsAllCalendarVisibleState = false;    				
    			}
    		}
    	}
    	
    	if (IsAllCalendarVisibleState) {
    		if (INFO) Log.i(TAG, "setAllCalendarVisibleUnVisibleStateText:All Cal Visible State");
    		mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_unvisible_state));
    		mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_VISIBLE_STATE);
    	}
    	else {
    		if (INFO) Log.i(TAG, "setAllCalendarVisibleUnVisibleStateText:NOT All Cal Visible State");
    		mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_visible_state));
    		mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_UNVISIBLE_STATE);
    	}
    }
    
    // ������ �ִ�...���ϱ�?
    public void setAllCalendarVisibleUnVisibleStateText(boolean init) {
    	
    	boolean IsAllCalendarVisibleState = true;
    	int size = mCalendarGroupContainerList.size();
    	for (int i=0; i<size; i++) {
    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
    		int count = groupInfo.getCalendarInfoCount(); 
    		for (int j=0; j<count; j++) {
    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);
    			if (init) {
    				if (calendarInfo.getVisibleState() == 0) {
        				IsAllCalendarVisibleState = false;    				
        			}
    			}
    			else {
	    			if (calendarInfo.getCurrentCheckStatusOfVisibleState() == 0) {
	    				if (INFO) Log.i(TAG, "setAllCalendarVisibleUnVisibleStateText:UnVisible Calendar=" + calendarInfo.calendarName);
	    				IsAllCalendarVisibleState = false;    				
	    			}
    			}
    		}
    	}
    	
    	if (IsAllCalendarVisibleState) {
    		if (INFO) Log.i(TAG, "setAllCalendarVisibleUnVisibleStateText:All Cal Visible State");
    		mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_unvisible_state));
    		mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_VISIBLE_STATE);
    	}
    	else {
    		if (INFO) Log.i(TAG, "setAllCalendarVisibleUnVisibleStateText:NOT All Cal Visible State");
    		mAllCalendarVisibleToggleText.setText(mResources.getString(R.string.do_all_calendar_visible_state));
    		mAllCalendarVisibleToggleText.setTag(ALL_CALENDAR_UNVISIBLE_STATE);
    	}
    }
    
    public void makeMainViewLowerLayout() {
    	// seperate layout�� �߰�
    	// Show Declined Events �߰�
    	// ������ seperate layout �߰�
    	RelativeLayout seperator = (RelativeLayout)mInflater.inflate(R.layout.selectcalendars_seperator_layout, null);
    	mInnerLinearLayoutOfScrollView.addView(seperator);
    	
    	CommonRelativeLayoutItemContainer Item = 
        		new CommonRelativeLayoutItemContainer(mContext, 0, CommonRelativeLayoutItemContainer.LOWER_BOLDER_LINE_POSITION);
    	int itemContainerHeight = (int) getResources().getDimension(R.dimen.selectCalendarViewItemHeight);
        RelativeLayout.LayoutParams itemConatainerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemContainerHeight);
        Item.setLayoutParams(itemConatainerLayoutParams);
        // ��Ʈ�� selectcalendars_show_declined_events �� ��Ÿ���� TextView�� �����ؾ� �Ѵ�
        TextView showDeclinedEventsText = new TextView(mContext);
		RelativeLayout.LayoutParams showDeclinedEventsTextParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);		
		showDeclinedEventsTextParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		
		int leftMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewCommonLeftMargin);
		showDeclinedEventsTextParams.setMargins(leftMargin, 0, 0, 0);		
		showDeclinedEventsText.setLayoutParams(showDeclinedEventsTextParams);
		
		showDeclinedEventsText.setGravity(Gravity.CENTER | Gravity.LEFT);
		showDeclinedEventsText.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);		
		int textColor = (int) mActivity.getResources().getColor(R.color.event_layout_label_color);		
		showDeclinedEventsText.setTextColor(textColor);		
		showDeclinedEventsText.setSingleLine(true);
		showDeclinedEventsText.setText(getResources().getString(R.string.selectcalendars_show_declined_events));
		showDeclinedEventsText.setTextIsSelectable(false);			
		
		Item.addView(showDeclinedEventsText);
		
		String switchOnText = mResources.getString(R.string.selectcalendars_show_declined_events_on);
		String switchOffText = mResources.getString(R.string.selectcalendars_show_declined_events_off);
		int textColorChecked = mResources.getColor(R.color.selectCalendarViewSwitchOnTextColor);
		int textColorUnChecked = mResources.getColor(R.color.selectCalendarViewSwitchOffTextColor);
		Drawable switchBGDrawable = mResources.getDrawable(R.drawable.switch_bg_holo_light); 
		// switch drawable�� ������ switch_thumb_activated_holo_light�� ������� ����
		// ���� ���̼����� ���� �̹����� ����϶�
		// �׸��� ���� ���̼����� �̹��� �̸� �տ� added[added_switch_thumb_activated_holo_light]�� �߰��Ͽ���
		// :�̷��� ���� ������ switch button�� width�� half�� ������ on/off�� ������ ���� ���� ���Ѵ�
		Drawable switchDrawable = mResources.getDrawable(R.drawable.added_switch_thumb_activated_holo_light);
		int switchDrawableHeight = switchDrawable.getIntrinsicHeight(); // explorer������ height�� 34�̴� �׷��� getIntrinsicHeight������ 43�̴� ��???
		/*int switchDrawableColor = Color.rgb(0x8C, 0xFA, 0xCA);
		switchDrawable.setColorFilter(new 
				PorterDuffColorFilter(switchDrawableColor, PorterDuff.Mode.MULTIPLY));*/
		ITESwitch switchButton = new ITESwitch(mContext, switchOnText, switchOffText,
				textColorUnChecked, textColorChecked,
				switchBGDrawable, 
				switchDrawable);		
		
		RelativeLayout.LayoutParams switchButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);		
		switchButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		int parentHeightHalf = itemContainerHeight / 2;
		int switchHeightHalf = switchDrawableHeight / 2;
		int topMargin = parentHeightHalf - switchHeightHalf;		
		int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewSwitchCommonRightMargin);
		switchButtonLayoutParams.setMargins(0, topMargin, rightMargin, 0);				
		switchButton.setLayoutParams(switchButtonLayoutParams);		
		
		Item.addView(switchButton);
		
    	mInnerLinearLayoutOfScrollView.addView(Item);    	
    	
    	RelativeLayout lastSeperator = (RelativeLayout)mInflater.inflate(R.layout.selectcalendars_seperator_layout, null);
    	mInnerLinearLayoutOfScrollView.addView(lastSeperator);
    }
        
    
    public SelectCalendarsFragment(SelectCalendarsActivity activity, Context context, Cursor acctsCursor) {
    	mActivity = activity;
    	mResolver = context.getContentResolver();
    	mAccountsCursor = acctsCursor;
    	
    	mViewState = MAIN_VIEW_FRAGMENT_STATE;
    	
    	if (mAccountsCursor == null || mAccountsCursor.getCount() == 0) {
            //Log.i(TAG, "SelectCalendarsAdapter: No accounts were returned!");
        }
    	    	
    	// Collect proper description for account types
        mAuthDescs = AccountManager.get(context).getAuthenticatorTypes();
        for (int i = 0; i < mAuthDescs.length; i++) {
            mTypeToAuthDescription.put(mAuthDescs[i].type, mAuthDescs[i]);           
        }
        
    	mCalendarsUpdater = new AsyncCalendarsUpdater(mResolver);
    	mClosedCursorsFlag = false;
    	
    	mSelectCalendarsHandler = new SelectCalendarsHandler();
    }
    
    
    public void setActionBarFragment(SelectCalendarsViewActionBarFragment actionBar) {
    	mActionBar = actionBar;
    }
	
    
    Resources mResources;
    ArrayList<String> mCalendarColorNameList;
    TypedArray mCalendarColorTA;
    Drawable mVisibleMarkDrawable;
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		
		mContext = activity.getApplicationContext();
		mResources = getResources();
		mDisplayMetrics = getResources().getDisplayMetrics();
		mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		// ecalenadr group�� ���� ���� �Ѵ�		
		// :�̷��� �ȵȴ�...ecalendar�� �ᱹ�� local�̹Ƿ� ecalendar group�� �ٸ� local���� add�Ǵ� ��Ȳ�� �߻��Ѵ�
		//  owner�� �������
		//  �ٵ� accout type�� ���Ǵ�� ����ϸ� �ȵǴ� ���ΰ�?
		if (mCurrentCalendarGroupsByApp.isEmpty()) {
			String eCalendarAccountLabel = mResources.getString(R.string.ecalendar_account_label);			
			String eCalendarAccountType = mResources.getString(R.string.ecalendar_psudo_account_type);
			
			if (!mCurrentCalendarGroupsByApp.containsKey(eCalendarAccountType)) 
				mCurrentCalendarGroupsByApp.put(eCalendarAccountType, eCalendarAccountLabel);			
			
		}
		
		mCalendarColorNameList = loadStringArray(R.array.selectable_calenar_colors_name);		
		mCalendarColorTA = mResources.obtainTypedArray(R.array.selectable_calenar_colors);	
		
		mVisibleMarkDrawable = mResources.getDrawable(R.drawable.ic_menu_done_holo_light);
		int irColor = Color.rgb(255, 0, 0);
		mVisibleMarkDrawable.setColorFilter(new PorterDuffColorFilter(irColor, PorterDuff.Mode.MULTIPLY));		
		
		
	}
	
	private ArrayList<String> loadStringArray(int resNum) {
        String[] labels = mResources.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
	}

	View mSwitchDimmingLayout;
	LinearLayout mTargetCalendarReminderContainer;
	CommonRelativeLayoutItemContainer mTargetCalendarEventReminderSettingContainer;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			
		Log.i("tag", "onCreateView");
		mFrameLayout = (LinearLayout) inflater.inflate(R.layout.selectcalendars_fragment_layout, null);
        mFrameLayoutViewSwitcher = (ViewSwitcher) mFrameLayout.findViewById(R.id.selectcalendars_fragment_switcher);  
        
        mSelectCalendarsLinearLayout = (RelativeLayout) inflater.inflate(R.layout.selectcalendars_mainview_layout, null); 
        mSwitchDimmingLayout = mSelectCalendarsLinearLayout.findViewById(R.id.mainview_switch_dimming_layout); 
        
        mSelectCalendarsScrollView = (LockableScrollView)mSelectCalendarsLinearLayout.findViewById(R.id.select_calendar_scrollview);
        mInnerLinearLayoutOfScrollView = (LinearLayout)mSelectCalendarsScrollView.findViewById(R.id.select_calendar_scrollview_inner_layout);        
        mAllCalendarVisibleToggleContainer = (CommonRelativeLayoutItemContainer)mInnerLinearLayoutOfScrollView.findViewById(R.id.all_calendar_visible_toggle_container);        
		mAllCalendarVisibleToggleContainer.setOnClickListener(mAllCalendarVisibleToggleContainerClickListener);
        mAllCalendarVisibleToggleText = (TextView)mAllCalendarVisibleToggleContainer.findViewById(R.id.all_calendar_visible_toggel_text);
        //mSelectCalendarsLinearLayout.setVisibility(View.INVISIBLE);       
                
        mEditCalendarsLinearLayout = (LinearLayout) inflater.inflate(R.layout.calendar_edit_layout, null);   
        mTargetCalendarNameContainer = 
        		(CommonRelativeLayoutItemContainer) mEditCalendarsLinearLayout.findViewById(R.id.target_calendar_name_container);
        mTargetCalendarNameTextView = (TextView) mTargetCalendarNameContainer.findViewById(R.id.target_calendar_name_text);  
                
        mTargetCalendarColorContainer = (CommonLinearLayoutItemContainer) mEditCalendarsLinearLayout.findViewById(R.id.select_calendar_color_container);
        
        mTargetCalendarReminderContainer = (LinearLayout) mEditCalendarsLinearLayout.findViewById(R.id.calendar_reminder_container);
        mTargetCalendarEventReminderSettingContainer = (CommonRelativeLayoutItemContainer) mTargetCalendarReminderContainer.findViewById(R.id.event_reminder_setting_container);
        makeTargetCalendarReminderSwitch();
        
        mCalendarGroupContainerList = new ArrayList<CommonLinearLayoutItemContainer>();
        
        bindCalendarGroup();        
        
        mFrameLayoutViewSwitcher.addView(mSelectCalendarsLinearLayout);
                
        return mFrameLayout;
	}	
	
	//public void getMain
	
	public void makeTargetCalendarReminderSwitch() {
		int itemContainerHeight = (int) mResources.getDimension(R.dimen.selectCalendarViewItemHeight);
		
		String switchOnText = mResources.getString(R.string.selectcalendars_show_declined_events_on);
		String switchOffText = mResources.getString(R.string.selectcalendars_show_declined_events_off);
		int textColorChecked = mResources.getColor(R.color.selectCalendarViewSwitchOnTextColor);
		int textColorUnChecked = mResources.getColor(R.color.selectCalendarViewSwitchOffTextColor);
		Drawable switchBGDrawable = mResources.getDrawable(R.drawable.switch_bg_holo_light); 
		
		Drawable switchDrawable = mResources.getDrawable(R.drawable.added_switch_thumb_activated_holo_light);
		int switchDrawableHeight = switchDrawable.getIntrinsicHeight(); // explorer������ height�� 34�̴� �׷��� getIntrinsicHeight������ 43�̴� ��???
		
		ITESwitch switchButton = new ITESwitch(mContext, switchOnText, switchOffText,
				textColorUnChecked, textColorChecked,
				switchBGDrawable, 
				switchDrawable);		
		
		RelativeLayout.LayoutParams switchButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);		
		switchButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		int parentHeightHalf = itemContainerHeight / 2;
		int switchHeightHalf = switchDrawableHeight / 2;
		int topMargin = parentHeightHalf - switchHeightHalf;
		//int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mDisplayMetrics);
		int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewSwitchCommonRightMargin);
		switchButtonLayoutParams.setMargins(0, topMargin, rightMargin, 0);
				
		switchButton.setLayoutParams(switchButtonLayoutParams);		
		
		mTargetCalendarEventReminderSettingContainer.addView(switchButton);
	}
	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onStop() {		
		super.onStop();
		
        closeChildrenCursors();
        
        if (mAccountsCursor != null && !mAccountsCursor.isClosed()) {
            mAccountsCursor.close();
        }
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
	}
	
	public void closeChildrenCursors() {
        synchronized (mChildrenCursors) {
            for (String key : mChildrenCursors.keySet()) {
                Cursor cursor = mChildrenCursors.get(key);
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
            mChildrenCursors.clear();
            mClosedCursorsFlag = true;
        }
    }
		
	CommonLinearLayoutItemContainer mECalendarGroupContainer;
	CommonLinearLayoutItemContainer mGoogleCalendarsGroupContainer;
	CommonLinearLayoutItemContainer mOtherCalendarsGroupContainer;
	ArrayList<CommonLinearLayoutItemContainer> mNonFixGroupCalendarContainerList;
	Object mECalendarsCookie;
	Object mGoogleCalendarsCookie;
	Object mOtherCalendarsCookie;
	Object mCookie;
	int mRefreshCalendarsCompletionFlag;
	int mCurrentRefreshCalendarsTokensORed;
	public void bindCalendarGroup() {		
		mRefreshCalendarsCompletionFlag = 0;
		mCurrentRefreshCalendarsTokensORed = 0;		
		
		mAccountsCursor.moveToPosition(-1);
        while (mAccountsCursor.moveToNext()) {
        	int accountColumn = mAccountsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
            int accountTypeColumn = mAccountsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);    

            String account = mAccountsCursor.getString(accountColumn);
            String accountType = mAccountsCursor.getString(accountTypeColumn);
            CharSequence accountLabel = getLabelForType(accountType);
            
            // onAttach���� �̹� mCurrentCalendarGroupsByApp�� ECalendar ���� ������
            if (account.compareTo(Utils.ECALENDAR_ACCOUNT_NAME) == 0) { 
            	mECalendarGroupContainer = makeCalendarGroupContainer(mResources.getString(R.string.ecalendar_account_label), false);
            	
            	mECalendarsCookie = accountType + "#" + account;
            	int token = mAccountsCursor.getPosition();
            	mRefreshCalendarsCompletionFlag = mRefreshCalendarsCompletionFlag | (1 << token);     
            	new RefreshCalendars(token, account, accountType).run();
            	continue;
            }
            
            // mCurrentCalendarGroupsByApp�� �������� �� �� �ִ� ���� account label�� ���� �����̴�!!!
            // ECalendar�� ���� Account Manager���� ��ϵ��� �ʾҰ� �����ε� �� ������ ����
            // �׷��Ƿ� account label�� ����...�׷��� psudo account label�� ����Ѵ�
            // ���� mCurrentCalendarGroupsByApp�� order�� ECalendar�� ù ��°�� ������ ��ġ��Ű����
            // �ٸ� �������� ������ ������ �ʴ´�
            
            if (accountLabel != null) {
            	if (!mCurrentCalendarGroupsByApp.containsKey(accountType)) {
            		
            		if (accountType.compareTo("com.google") == 0) {
            			mGoogleCalendarsCookie = accountType + "#" + account;
            			mGoogleCalendarsGroupContainer = makeCalendarGroupContainer((String) accountLabel, false); 
                    	
            		}
            		else {
	            		CommonLinearLayoutItemContainer calendarGroupContainer = makeCalendarGroupContainer((String) accountLabel, true);	  
	            		if (mNonFixGroupCalendarContainerList == null) {
	            			mNonFixGroupCalendarContainerList = new ArrayList<CommonLinearLayoutItemContainer>();
	            		}
	            		
	            		mNonFixGroupCalendarContainerList.add(calendarGroupContainer);	            		
            		}
            		
            		mCurrentCalendarGroupsByApp.put(accountType, accountLabel.toString());
            	}
            	else {
            		continue;
            	}
            }
            else {            	
            	// account label�� ���ٴ� ���� LOCAL�� ������ �� ���� : Ȯ���� ���� �ƴϴ� ������ Ȯ�ǽ� �ȴ�            	
        		if (!mCurrentCalendarGroupsByApp.containsKey(accountType)) {
        			mOtherCalendarsCookie = accountType + "#" + account;
        			mOtherCalendarsGroupContainer = makeCalendarGroupContainer("��Ÿ", false); 
            		            		
        			mCurrentCalendarGroupsByApp.put(accountType, "��Ÿ"); // ��Ÿ ���ڿ��� res���� ã��
            	} 
        		else {
        			continue;
        		}
            }
            
            int token = mAccountsCursor.getPosition();
            Log.i("tag", "test:token value=" + String.valueOf(token));   
            mRefreshCalendarsCompletionFlag = mRefreshCalendarsCompletionFlag | (1 << token);            
             
            new RefreshCalendars(token, account, accountType).run();
        }
        
        Log.i("tag", "test:mRefreshCalendarsCompletionFlag=" + String.valueOf(mRefreshCalendarsCompletionFlag));                
    }
		
	public CommonLinearLayoutItemContainer makeCalendarGroupContainer(String calendarLabel, boolean setTag) {
		CommonLinearLayoutItemContainer calendarGroupContainer = (CommonLinearLayoutItemContainer)mInflater.inflate(R.layout.selectcalendars_group_container, null);
		if (setTag)
			calendarGroupContainer.setTag(calendarLabel);
		RelativeLayout groupIndicator = (RelativeLayout)calendarGroupContainer.findViewById(R.id.group_indicator);
        TextView indicatorTextView = (TextView)groupIndicator.findViewById(R.id.group_indicator_text);
        indicatorTextView.setText(calendarLabel);
        return calendarGroupContainer;        
	}
	
	
	public CommonRelativeLayoutItemContainer makeCalendarGroupContainerItem(String key, long calendarId, int visibleState, int calendarColor, String calendarName) {
				
		// -> calendarGroupItem�� left margin = 
		//      (visibleStateMarkIcon left margin) + (visibleStateMarkIcon width) + (CalendarColorIcon left margin) 
		//mResources.getDimension(R.dimen.selectCalendarViewCalendarGroupItemLeftPadding)
		int paddingLeft = (int) (mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconLeftMargin) +
				mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconWidth) + 
				mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconLeftMargin));
		CommonRelativeLayoutItemContainer calendarGroupItem = 
        		new CommonRelativeLayoutItemContainer(mContext, paddingLeft, CommonRelativeLayoutItemContainer.LOWER_BOLDER_LINE_POSITION);
        
        int itemContainerHeight = (int) mResources.getDimension(R.dimen.selectCalendarViewItemHeight);
        RelativeLayout.LayoutParams itemConatainerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemContainerHeight);
        calendarGroupItem.setLayoutParams(itemConatainerLayoutParams);
        calendarGroupItem.setTag(calendarId);
        calendarGroupItem.setBackground(mResources.getDrawable(R.drawable.common_layout_selector));
        calendarGroupItem.setClickable(true);
        calendarGroupItem.setOnClickListener(mCalendarItemClickListener);
                
        // Calendars.VISIBLE ���� Ȯ���ؾ� �Ѵ�
        ImageView visibleStateMarkIcon = makeVisibleStateMarkIcon();
        if (visibleState == 0)
        	visibleStateMarkIcon.setVisibility(View.INVISIBLE);
        
        View calendarColorIcon = makeCalendarColorIcon(true, visibleStateMarkIcon, calendarColor);
        TextView calendarNameText = makeCalendarNameTextView(calendarColorIcon, calendarName);
        ImageView exclamationMarkIcon = makeExclamationMarkIcon(calendarId);
        
        calendarGroupItem.addView(visibleStateMarkIcon);        
        calendarGroupItem.addView(calendarColorIcon);
        calendarGroupItem.addView(calendarNameText);
        calendarGroupItem.addView(exclamationMarkIcon);    
        
        return calendarGroupItem;
	}
	
	@SuppressLint("ResourceType")
	public ImageView makeVisibleStateMarkIcon() {
		
		int width = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconWidth);
		RelativeLayout.LayoutParams visibleStateMarkIconImageViewLayoutParams = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		visibleStateMarkIconImageViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		visibleStateMarkIconImageViewLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		
		int leftMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconLeftMargin);
		visibleStateMarkIconImageViewLayoutParams.setMargins(leftMargin, 0, 0, 0);
		
		ImageView visibleStateMakrIcon = new ImageView(mContext);
		visibleStateMakrIcon.setLayoutParams(visibleStateMarkIconImageViewLayoutParams);
		//visibleStateMakrIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		visibleStateMakrIcon.setImageDrawable(mVisibleMarkDrawable);
		visibleStateMakrIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);	
		visibleStateMakrIcon.setId(1);
		
		return visibleStateMakrIcon;
	}
	
	public RelativeLayout.LayoutParams makeSyncedMarkIconLayoutParams() {
		RelativeLayout.LayoutParams syncedMarkIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		syncedMarkIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		syncedMarkIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);	
		return syncedMarkIconImageViewLayout;
	}
	
	@SuppressLint("ResourceType")
	public View makeCalendarColorIcon(boolean setSyncedState, ImageView syncedMarkIcon, int calendarColor) {
		View calendarColorIcon = new View(mContext);
		
		
		int leftMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconLeftMargin);
		
		int width = (int) mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconWidth);
		int height = width;
		RelativeLayout.LayoutParams calendarIconParams = new RelativeLayout.LayoutParams(width, height);
		
		calendarIconParams.setMargins(leftMargin, 0, 0, 0);
		
		if (setSyncedState) {			
			int syncedMarkIconId = syncedMarkIcon.getId();
			calendarIconParams.addRule(RelativeLayout.RIGHT_OF, syncedMarkIconId);
			calendarColorIcon.setId(2);
		} else {			
			calendarIconParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			calendarColorIcon.setId(1);
		}
		
		calendarIconParams.addRule(RelativeLayout.CENTER_VERTICAL);
		calendarColorIcon.setLayoutParams(calendarIconParams);
		
		/////////////////////////////////////////////////////////////////////////
		ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
		Paint paint = shapeDrawable.getPaint();
		paint.setColor(calendarColor);
		calendarColorIcon.setBackground(shapeDrawable);
		return calendarColorIcon;		
	}
	
	public TextView makeCalendarNameTextView(View calendarColorIcon, String calendarName) {
		TextView calendarNameText = new TextView(mContext);
		RelativeLayout.LayoutParams calendarNameTextParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		int calendarColorIconId = calendarColorIcon.getId();
		calendarNameTextParams.addRule(RelativeLayout.RIGHT_OF, calendarColorIconId);
		calendarNameTextParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		int leftMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewCalendarNameTextViewLeftMargin);
		calendarNameTextParams.setMargins(leftMargin, 0, 0, 0);		
		calendarNameText.setLayoutParams(calendarNameTextParams);
		
		calendarNameText.setGravity(Gravity.CENTER | Gravity.LEFT);
		calendarNameText.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);		
		int textColor = (int) mActivity.getResources().getColor(R.color.event_layout_label_color);		
		calendarNameText.setTextColor(textColor);		
		calendarNameText.setSingleLine(true);
		calendarNameText.setText(calendarName);
		calendarNameText.setTextIsSelectable(false);		
		
		return calendarNameText;
	}
	
	public ImageView makeExclamationMarkIcon(long calendarId) {
				
		RelativeLayout.LayoutParams exclamationMarkIconImageViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		exclamationMarkIconImageViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		exclamationMarkIconImageViewLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		
		int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewExclamationMarkIconRightMargin);
		exclamationMarkIconImageViewLayoutParams.setMargins(0, 0, rightMargin, 0);
		
		ImageView exclamationMakrIcon = new ImageView(mContext);
		exclamationMakrIcon.setLayoutParams(exclamationMarkIconImageViewLayoutParams);
		
		exclamationMakrIcon.setBackgroundResource(R.drawable.red_exclamation_circle);
		exclamationMakrIcon.setClickable(true);
		
		exclamationMakrIcon.setTag(calendarId);
		exclamationMakrIcon.setOnTouchListener(mExclamationMakrIconTouchListener);
		exclamationMakrIcon.setOnClickListener(mExclamationMakrIconClickListener);
		
		return exclamationMakrIcon;
	}
	
	OnTouchListener mExclamationMakrIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:						
				AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
				downAlpha.setDuration(0); 
				downAlpha.setFillAfter(true);		
				v.startAnimation(downAlpha);			
				break;
			case MotionEvent.ACTION_UP:			
				AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
				upAlpha.setDuration(0); 
				upAlpha.setFillAfter(false); 			
				v.startAnimation(upAlpha);				
				break;			
			default:
				break;
			}						
			
			//return true; //mExclamationMakrIconClickListener�� ȣ����� ����
			return false;
		}    	
    };
	
	public CommonRelativeLayoutItemContainer makeCalendarColorItem(int calendarColor, String calendarColorName) {
		// -> CalendarColorItem�� left margin = 
		// (CALENDARCOLOR_ICON_LEFT_MARGIN) + (CALENDARCOLOR_ICON_WIDTH) + (CALENDAR_COLOR_NAME_TEXT_LEFT_MARGIN)
		int leftPadding = (int) (mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconLeftMargin) +
				mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconWidth) + 
				mResources.getDimension(R.dimen.selectCalendarViewCalendarColorNameTextLeftMargin));
		CommonRelativeLayoutItemContainer calendarColorItem = 
        		new CommonRelativeLayoutItemContainer(mContext, leftPadding, CommonRelativeLayoutItemContainer.LOWER_BOLDER_LINE_POSITION);
        
        int itemContainerHeight = (int) getResources().getDimension(R.dimen.selectCalendarViewItemHeight);
        RelativeLayout.LayoutParams itemConatainerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemContainerHeight);
        calendarColorItem.setLayoutParams(itemConatainerLayoutParams);        
        
        View calendarColorIcon = makeCalendarColorIcon(false, null, calendarColor);
        TextView calendarNameText = makeCalendarColorNameTextView(calendarColorIcon, calendarColorName);
             
        calendarColorItem.addView(calendarColorIcon);
        calendarColorItem.addView(calendarNameText);        
        
        return calendarColorItem;
	}
	
	public TextView makeCalendarColorNameTextView(View calendarColorIcon, String calendarName) {
		TextView calendarNameText = new TextView(mContext);
		RelativeLayout.LayoutParams calendarNameTextParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		int calendarColorIconId = calendarColorIcon.getId();
		calendarNameTextParams.addRule(RelativeLayout.RIGHT_OF, calendarColorIconId);
		calendarNameTextParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		int leftMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewCalendarColorNameTextLeftMargin);
		calendarNameTextParams.setMargins(leftMargin, 0, 0, 0);		
		calendarNameText.setLayoutParams(calendarNameTextParams);
		
		calendarNameText.setGravity(Gravity.CENTER | Gravity.LEFT);
		calendarNameText.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);		
		int textColor = (int) mResources.getColor(R.color.event_layout_label_color);		
		calendarNameText.setTextColor(textColor);	
		// setTextSize�� para�� The scaled pixel[sp] size ��.
		// �׷��Ƿ� mResources.getDimension(R.dimen.selectCalendarViewCommonTextSize[20sp]) ���� �����ϸ� �ȵȴ�		
		float textSPSize =  mResources.getDimension(R.dimen.selectCalendarViewCommonTextSize) / mResources.getDisplayMetrics().scaledDensity;
		calendarNameText.setTextSize(textSPSize);
		calendarNameText.setSingleLine(true);
		calendarNameText.setText(calendarName);
		calendarNameText.setTextIsSelectable(false);		
		
		return calendarNameText;
	}
	
	public void doSaveAction() {
        
		// �Ʒ� ��Ĵ�ζ�� delay�� �߻��Ѵ�
		// :app���� async�� ó���ϵ��� �ϴ� ���� ���???
		//  -> ���� ����̴�
		int size = mCalendarGroupContainerList.size();
    	for (int i=0; i<size; i++) {
    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
    		int count = groupInfo.getCalendarInfoCount(); 
    		for (int j=0; j<count; j++) {
    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);  			
                
                if (calendarInfo.getVisibleState() != calendarInfo.getCurrentCheckStatusOfVisibleState()) {
                	ContentValues values = new ContentValues();
                	long id = calendarInfo.id;
        			Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, id);
                	values.put(Calendars.VISIBLE, calendarInfo.getCurrentCheckStatusOfVisibleState());
                	int result = mResolver.update(uri,
                    		values,
                    		null,
                            null);
                }
    		}
    	}            
        
    }
	
	public void DoneExit() {
		
		doSaveAction();
		
		mActivity.goodBye();
		//mActivity.finish();
	}
	
	
	/**
     * Gets the label associated with a particular account type. If none found, return null.
     * @param accountType the type of account
     * @return a CharSequence for the label or null if one cannot be found.
     */
    public CharSequence getLabelForType(final String accountType) {
        CharSequence label = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
             try {
                 AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                 Context authContext = mActivity.createPackageContext(desc.packageName, 0);
                 label = authContext.getResources().getText(desc.labelId);
             } catch (PackageManager.NameNotFoundException e) {
                 Log.i("tag", "No label for account type " + ", type " + accountType);
             }
        }
        return label;
    }
	
	
	
	private class AsyncCalendarsUpdater extends AsyncQueryHandler {

        public AsyncCalendarsUpdater(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if(cursor == null) {
                return;
            }
            
            synchronized(mChildrenCursors) {
                if (mClosedCursorsFlag || (mActivity != null && mActivity.isFinishing())) {
                    cursor.close();
                    return;
                }
            }            

            Cursor currentCursor = mChildrenCursors.get(cookie);
            // Check if the new cursor has the same content as our old cursor
            if (currentCursor != null) {
                if (Utils.compareCursors(currentCursor, cursor)) {
                    cursor.close();
                    return;
                }
            }
            // If not then make a new matrix cursor for our Map
            MatrixCursor newCursor = Utils.matrixCursorFromCursor(cursor);
            cursor.close();
            // And update our list of duplicated names
            Utils.checkForDuplicateNames(mIsDuplicateName, newCursor, DISPLAY_NAME_COLUMN);

            //printCursorContext(newCursor);
            
            mChildrenCursors.put((String)cookie, newCursor);            
            
            mCurrentRefreshCalendarsTokensORed = mCurrentRefreshCalendarsTokensORed | (1 << token);
            if (mCurrentRefreshCalendarsTokensORed == mRefreshCalendarsCompletionFlag) {
            	mCurrentRefreshCalendarsTokensORed = 0;
            	mRefreshCalendarsCompletionFlag = 0;
            	Log.i("tag", "AsyncCalendarsUpdater.onQueryComplete : Refresh Calendars Completion!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            	Message msg = mSelectCalendarsHandler.obtainMessage();
            	msg.what = ENTIRE_REFRESH_CALENDARS_COMPLETION;    	
            	mSelectCalendarsHandler.sendMessage(msg);
            }
        }
    }
	
	
	private class RefreshCalendars implements Runnable {

        int mToken;
        String mAccount;
        String mAccountType;

        public RefreshCalendars(int token, String account, String accountType) {
            mToken = token;
            mAccount = account;
            mAccountType = accountType;
        }        
        

        @Override
        public void run() {
            mCalendarsUpdater.cancelOperation(mToken);
            
            mCalendarsUpdater.startQuery(mToken,
                    mAccountType + "#" + mAccount,
                    Calendars.CONTENT_URI, PROJECTION,
                    ACCOUNT_SELECTION,
                    new String[] { mAccount, mAccountType } /*selectionArgs*/,
                    CALENDARS_ORDERBY);
        }
    }
	
	boolean mMadeCalendarColorItems = false;	
	View.OnClickListener mExclamationMakrIconClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){  
        	//int viewID = v.getId(); 
        	//String key = (String) v.getTag();
        	long calendarId = (Long) v.getTag();
        	String disPlayName = null;
        	
        	boolean gotDispName = false;
        	int size = mCalendarGroupContainerList.size();
	    	for (int i=0; i<size; i++) {
	    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);
	    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
	    		int count = groupInfo.getCalendarInfoCount(); 
	    		for (int j=0; j<count; j++) {
	    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);
	    			if (calendarId == calendarInfo.getCalendarId()) {	    				
	    				disPlayName = calendarInfo.calendarDispName;
	    				gotDispName = true;
	    				break;	    				
	    			}	    			
	    		}
	    		
	    		if (gotDispName)
	    			break;
	    	}
	    	
        	//Cursor cursor = mChildrenCursors.get(key);
        	
        	//final long id = cursor.getLong(ID_COLUMN);		
    		//String name = cursor.getString(NAME_COLUMN);
            //String disPlayName = cursor.getString(DISPLAY_NAME_COLUMN);
            //String owner = cursor.getString(OWNER_COLUMN);
            //final String accountName = cursor.getString(ACCOUNT_COLUMN);
            //final String accountType = cursor.getString(ACCOUNT_TYPE_COLUMN);
            //int color = Utils.getDisplayColorFromColor(cursor.getInt(COLOR_COLUMN));     
            
            mTargetCalendarNameTextView.setText(disPlayName);

            if (!mMadeCalendarColorItems) {            	
	            size = mCalendarColorNameList.size();	        
	            for (int i=0; i<size; i++) {        	
	    			String colorName = mCalendarColorNameList.get(i);    
	    			int calendarColors = mCalendarColorTA.getColor(i, 0xffffff);
	    			CommonRelativeLayoutItemContainer calendarColorItem = makeCalendarColorItem(calendarColors, colorName);
	    			calendarColorItem.setTag(colorName);
	    			mTargetCalendarColorContainer.addView(calendarColorItem);
	            }
	            
	            mMadeCalendarColorItems = true;
            }
            
            switchSeperateCalendarEditView();        	
        }
	};
	
	private static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float SWITCH_MAIN_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f; 
	public static final float SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY = SWITCH_MAIN_PAGE_VELOCITY * 2; 
	
	TranslateAnimation mSeperateCalendarEditViewEnterAnimation;
	TranslateAnimation mMainViewExitAnimation;
	AnimationListener mSeperateCalendarEditViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			// actionbar animation�� start �ؾ� �Ѵ�
			mActionBar.startActionBarAnim();
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AlphaAnimation mMainViewExitDimmingAnimation;
	private void switchSeperateCalendarEditView() {
		
		float enterAnimationDistance = mFrameLayout.getWidth(); 
		float exitAnimationDistance = mFrameLayout.getWidth() * 0.4f; 
		
		float velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mFrameLayout.getWidth(), MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
		
    	mSeperateCalendarEditViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mSeperateCalendarEditViewEnterAnimation);  
    	mSeperateCalendarEditViewEnterAnimation.setInterpolator(enterInterpolator);
    	mSeperateCalendarEditViewEnterAnimation.setDuration(enterAnimationDuration);
    	mSeperateCalendarEditViewEnterAnimation.setAnimationListener(mSeperateCalendarEditViewEnterAnimationListener);
    	
    	mMainViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);    	
    	ITEAnimInterpolator exitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mMainViewExitAnimation);   	
    	mMainViewExitAnimation.setInterpolator(exitInterpolator);    	
    	mMainViewExitAnimation.setDuration(exitAnimationDuration); 
    	mMainViewExitAnimation.setAnimationListener(mMainViewExitAnimationListener);
    	
    	mMainViewExitDimmingAnimation = new AlphaAnimation(0.0f, 1.0f); 
    	mMainViewExitDimmingAnimation.setDuration(exitAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mMainViewExitDimmingAnimation);   	
    	mMainViewExitDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);    	
    	
    	mFrameLayoutViewSwitcher.setInAnimation(mSeperateCalendarEditViewEnterAnimation);
    	mFrameLayoutViewSwitcher.setOutAnimation(mMainViewExitAnimation); 
    	
		mFrameLayoutViewSwitcher.addView(mEditCalendarsLinearLayout);
			
		int fromView = mViewState;
		mViewState = SEPERATE_EDIT_VIEW_FRAGMNET_STATE;
		int toView = mViewState;	
		
				
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance);	
		
		mFrameLayoutViewSwitcher.showNext();		
	}
	
	TranslateAnimation mMainViewEnterAnimation;
	TranslateAnimation mSeperateCalendarEditViewExitAnimation;	
	TranslateAnimation mEntireCalendarEditViewExitAnimation;
	AnimationListener mMainViewEnterAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			mActionBar.startActionBarAnim();
			int bgColor = Color.argb(30, 0, 0, 0);
	    	mSwitchDimmingLayout.setBackgroundColor(bgColor);
			mSwitchDimmingLayout.startAnimation(mMainViewEnterDimmingAnimation);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			int bgColor = Color.argb(0, 0, 0, 0);
	    	mSwitchDimmingLayout.setBackgroundColor(bgColor);
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
	    	mSwitchDimmingLayout.setBackgroundColor(bgColor);
			mSwitchDimmingLayout.startAnimation(mMainViewExitDimmingAnimation);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AlphaAnimation mMainViewEnterDimmingAnimation;
	// Entire edit���� Seperate edit������ ���� �������� �ʰ� �ִ�
	public void switchMainView() {
		float enterAnimationDistance = mFrameLayout.getWidth() * 0.4f;
    	float exitAnimationDistance = mFrameLayout.getWidth(); // 
    	
    	float velocity = SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY;
    	long exitAnimationDuration = ITEAnimationUtils.calculateDuration(exitAnimationDistance, mFrameLayout.getWidth(), MINIMUM_SNAP_VELOCITY, velocity); 	    	
    	long enterAnimationDuration = exitAnimationDuration;
    	
    	mSeperateCalendarEditViewExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, 1, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);
    	ITEAnimInterpolator exitInterpolator = new ITEAnimInterpolator(exitAnimationDistance, mSeperateCalendarEditViewExitAnimation); 
    	mSeperateCalendarEditViewExitAnimation.setInterpolator(exitInterpolator);    	
    	mSeperateCalendarEditViewExitAnimation.setDuration(exitAnimationDuration);
    	
    	
    	mMainViewEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.4f, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0);    	
    	ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mMainViewEnterAnimation);      	
    	mMainViewEnterAnimation.setInterpolator(enterInterpolator);    	
    	mMainViewEnterAnimation.setDuration(enterAnimationDuration);    	
    	mMainViewEnterAnimation.setAnimationListener(mMainViewEnterAnimationListener);	
    	//mSeperateCalendarEditViewExitAnimation.setAnimationListener(mEventInfoViewExitAnimationListener); 
    	
    	mMainViewEnterDimmingAnimation = new AlphaAnimation(1.0f, 0.0f); 
    	mMainViewEnterDimmingAnimation.setDuration(enterAnimationDuration);
    	ITEAnimInterpolator mainViewDimmingInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mMainViewEnterDimmingAnimation);   	
    	mMainViewEnterDimmingAnimation.setInterpolator(mainViewDimmingInterpolator);	
		
		mFrameLayoutViewSwitcher.setInAnimation(mMainViewEnterAnimation);
    	mFrameLayoutViewSwitcher.setOutAnimation(mSeperateCalendarEditViewExitAnimation);       	
		
		int fromView = mViewState;
		mViewState = MAIN_VIEW_FRAGMENT_STATE;
		int toView = mViewState;		
		
		long animDuration;
		float distance;		
		if (enterAnimationDuration > exitAnimationDuration) {
			animDuration = enterAnimationDuration;
			distance = enterAnimationDistance;
		}
		else {
			animDuration = exitAnimationDuration;
			distance = exitAnimationDistance;
		}
		mActionBar.notifyFragmentViewSwitchState(fromView, toView, enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance);	
				
		mFrameLayoutViewSwitcher.showPrevious();
		mFrameLayoutViewSwitcher.removeView(mEditCalendarsLinearLayout);
	}
	
	public void notifyedMsgByActionBar(int msg) {
		switch(msg) {
		case SelectCalendarsViewActionBarFragment.SELECT_MAINVIEW_EDIT:
			break;
		case SelectCalendarsViewActionBarFragment.SELECT_MAINVIEW_DONE:
			break;
		case SelectCalendarsViewActionBarFragment.SELECT_SEPERATEVIEW_CANCEL:
			// MAIN VIEW�� �����ؾ� �Ѵ�			
			switchMainView();
			break;
		case SelectCalendarsViewActionBarFragment.SELECT_SEPERATEVIEW_DONE:
			break;
		case SelectCalendarsViewActionBarFragment.SELECT_ENTIREVIEW_DONE:
			DoneExit();
			break;
		default:
			break;
		
		}
	}
	
	public void XXX() {
		Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(mActivity));
        intent.setDataAndType(CalendarContract.CONTENT_URI, Utils.APPWIDGET_DATA_TYPE);
        mActivity.sendBroadcast(intent);
	}
	
	
    
	public void printCursorContext(MatrixCursor cursor) {
		if (INFO) Log.i(TAG, "+printCursorContext");
		
		
		cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
        	final long id = cursor.getLong(ID_COLUMN);		
    		String name = cursor.getString(NAME_COLUMN);
            String disPlayName = cursor.getString(DISPLAY_NAME_COLUMN);
            String owner = cursor.getString(OWNER_COLUMN);
            final String accountName = cursor.getString(ACCOUNT_COLUMN);
            final String accountType = cursor.getString(ACCOUNT_TYPE_COLUMN);
            int color = Utils.getDisplayColorFromColor(cursor.getInt(COLOR_COLUMN)); 
            
            if (INFO) Log.i(TAG, "**************************");
            if (INFO) Log.i(TAG, "id=" + String.valueOf(id));
            if (INFO) Log.i(TAG, "name=" + name);
            if (INFO) Log.i(TAG, "disPlayName=" + disPlayName);
            if (INFO) Log.i(TAG, "owner=" + owner);
            if (INFO) Log.i(TAG, "account name=" + accountName);
            if (INFO) Log.i(TAG, "account type=" + accountType);
            if (INFO) Log.i(TAG, "color=" + String.valueOf(color));
            if (INFO) Log.i(TAG, "**************************");
        }
        
        //cursor.moveToPosition(-1);
        cursor.moveToFirst();
        Log.i("tag", "-printCursorContext");
        
        
	}
	
}
