package com.intheeast.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.intheeast.acalendar.CommonLinearLayoutItemContainer;
import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.LockableScrollView;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
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
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract.Calendars;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class DefaultCalendarSettingView extends LinearLayout {
	
	private static final String TAG = DefaultCalendarSettingView.class.getSimpleName();
	private static boolean INFO = true;
	
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
	      Calendars.ACCOUNT_TYPE,
	      Calendars.CALENDAR_ACCESS_LEVEL
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
    private static final int CALENDAR_ACCESS_LEVEL_COLUMN = 10;

	Context mContext;
	Activity mActivity;
	Resources mResources;
	LayoutInflater mInflater;
	ContentResolver mResolver;
	
	SettingsFragment mFragment;
	SettingsActionBarFragment mActionBar;
	
	LinearLayout mInnerLinearLayoutOfScrollView;
	
	CommonLinearLayoutItemContainer mECalendarGroupContainer;
	CommonLinearLayoutItemContainer mGoogleCalendarsGroupContainer;
	CommonLinearLayoutItemContainer mOtherCalendarsGroupContainer;
	
	ArrayList<CommonLinearLayoutItemContainer> mCalendarGroupContainerList;
	ArrayList<CommonLinearLayoutItemContainer> mNonFixGroupCalendarContainerList;
	
	Cursor mAccountsCursor;
	
	Object mECalendarsCookie;
	Object mGoogleCalendarsCookie;
	Object mOtherCalendarsCookie;
	Object mCookie;
	int mRefreshCalendarsCompletionFlag;
	int mCurrentRefreshCalendarsTokensORed;
	
	protected AuthenticatorDescription[] mAuthDescs;
	private Map<String, AuthenticatorDescription> mTypeToAuthDescription = 
			new HashMap<String, AuthenticatorDescription>();
	
	// This is for keeping MatrixCursor copies so that we can requery in the background.
 	private Map<String, Cursor> mChildrenCursors = new HashMap<String, Cursor>();
 	
 	// This is to keep track of whether or not multiple calendars have the same display name
    private static HashMap<String, Boolean> mIsDuplicateName = new HashMap<String, Boolean>();
    
    private HashMap<String, String> mCurrentCalendarGroupsByApp = new HashMap<String, String>();

    private AsyncCalendarsUpdater mCalendarsUpdater;
    
    // Flag for when the cursors have all been closed to ensure no race condition with queries.
    private boolean mClosedCursorsFlag;
    
    ArrayList<String> mCalendarColorNameList;
    TypedArray mCalendarColorTA;
    
    Drawable mSelectedMarkDrawable;
	ImageView mSelectedMakrIcon;
	
	public DefaultCalendarSettingView (Context context) {
		super(context);	
		//mContext = context;
		init(context);
	}
	
	public DefaultCalendarSettingView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		//mContext = context;
		init(context);
	}
	
	public DefaultCalendarSettingView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		//mContext = context;
		init(context);
	}
	
	@SuppressLint("Recycle")
	public void init(Context context) {
		mContext = context;
		mResources = mContext.getResources();
		mResolver = mContext.getContentResolver();
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		// Collect proper description for account types
        mAuthDescs = AccountManager.get(mContext).getAuthenticatorTypes();
        for (int i = 0; i < mAuthDescs.length; i++) {
            mTypeToAuthDescription.put(mAuthDescs[i].type, mAuthDescs[i]);           
        }        
        
    	mCalendarsUpdater = new AsyncCalendarsUpdater(mResolver);    	
    	mEntireRefreshCalendarsCompletionHandler = new EntireRefreshCalendarsCompletionHandler();
    	
    	if (mCurrentCalendarGroupsByApp.isEmpty()) {
			String eCalendarAccountLabel = mResources.getString(R.string.ecalendar_account_label);			
			String eCalendarAccountType = mResources.getString(R.string.ecalendar_psudo_account_type);
			
			if (!mCurrentCalendarGroupsByApp.containsKey(eCalendarAccountType)) 
				mCurrentCalendarGroupsByApp.put(eCalendarAccountType, eCalendarAccountLabel);			
		}
		
		mCalendarColorNameList = loadStringArray(R.array.selectable_calenar_colors_name);		
		mCalendarColorTA = mResources.obtainTypedArray(R.array.selectable_calenar_colors);	
		
		mCalendarGroupContainerList = new ArrayList<CommonLinearLayoutItemContainer>();
		mClosedCursorsFlag = false;
		
	}
	
	int mCurrentDefaultCalendarId;
	public void initView(SettingsFragment fragment, SettingsActionBarFragment actionBar, Cursor acctsCursor) {
		mFragment = fragment;
		mActionBar = actionBar;	
		mAccountsCursor = acctsCursor;   
		
		mActivity = mFragment.getActivity();   		
		
		LockableScrollView scrollView = (LockableScrollView)findViewById(R.id.settings_default_calendar_scrollview);
		mInnerLinearLayoutOfScrollView = (LinearLayout)scrollView .findViewById(R.id.settings_default_calendar_scrollview_inner_layout);       
		
		mCurrentDefaultCalendarId = Utils.getSharedPreference(mContext, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, -1);
		
		makeSelectedMarkIcon();
		
		bindCalendarGroup();		
	}
		
	
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
            	else 
            		continue;
            }
            else {            	
            	// account label�� ���ٴ� ���� LOCAL�� ������ �� ���� : Ȯ���� ���� �ƴϴ� ������ Ȯ�ǽ� �ȴ�            	
        		if (!mCurrentCalendarGroupsByApp.containsKey(accountType)) {
        			mOtherCalendarsCookie = accountType + "#" + account;
        			mOtherCalendarsGroupContainer = makeCalendarGroupContainer("��Ÿ", false); 
            		            		
        			mCurrentCalendarGroupsByApp.put(accountType, "��Ÿ"); // ��Ÿ ���ڿ��� res���� ã��
            	} 
        		else
        			continue;
            }
            
            int token = mAccountsCursor.getPosition();
            Log.i("tag", "test:token value=" + String.valueOf(token));   
            mRefreshCalendarsCompletionFlag = mRefreshCalendarsCompletionFlag | (1 << token);            
             
            new RefreshCalendars(token, account, accountType).run();
        }
        
        Log.i("tag", "test:mRefreshCalendarsCompletionFlag=" + String.valueOf(mRefreshCalendarsCompletionFlag));                
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
	
	OnClickListener mCalendarItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
			CommonRelativeLayoutItemContainer calendarGroupItem = (CommonRelativeLayoutItemContainer) v;
			if (calendarGroupItem.findViewById(R.id.selected_default_calendar_mark_icon) != null)
				return;
			
			long calendarId = (Long) calendarGroupItem.getTag();
			int size = mCalendarGroupContainerList.size();
			
			boolean removedMarkIcon = false;
			for (int i=0; i<size; i++) {
	    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);	    		
	    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
	    		int count = groupInfo.getCalendarInfoCount(); 
	    		for (int j=0; j<count; j++) {
	    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);	    			
	    			CommonRelativeLayoutItemContainer itemContainer = calendarInfo.getItemContainerView();
	    			if (itemContainer.findViewById(R.id.selected_default_calendar_mark_icon) != null) {
	    				itemContainer.removeView(mSelectedMakrIcon); 
	    				removedMarkIcon = true;
	    				break;
	    			}
	    		}
	    		
	    		if (removedMarkIcon)
	    			break;
	    	}	    							
			
	    	for (int i=0; i<size; i++) {
	    		CommonLinearLayoutItemContainer groupContainer = mCalendarGroupContainerList.get(i);	    		
	    		
	    		CalendarGroupInfo groupInfo = (CalendarGroupInfo) groupContainer.getTag();
	    		int count = groupInfo.getCalendarInfoCount(); 
	    		for (int j=0; j<count; j++) {
	    			CalendarInfo calendarInfo = groupInfo.getSeperateCalendarInfo(j);	    			
	    			
	    			if (calendarId == calendarInfo.getCalendarId()) {
	    				
	    				calendarGroupItem.addView(mSelectedMakrIcon);	
	    				
	    				// switch mainvew
	    				Utils.setSharedPreference(mContext, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, (int) calendarInfo.getCalendarId());
	    				mFragment.setDefaultCalendarInfo((int) calendarInfo.getCalendarId(), calendarInfo.getCalendarDispName());
	    				mFragment.switchMainView();
	    				
		    			/*
		    			CalendarInfo calendarInfo_b = groupInfo.getSeperateCalendarInfo(j);
		    			if (calendarInfo.getCurrentCheckStatusOfVisibleState() != calendarInfo_b.getCurrentCheckStatusOfVisibleState())
		    				if (INFO) Log.i(TAG, "oooops!!!");
		    			*/
	    			}	    			
	    		}
	    	}  	
	    	
		}
		
	};    
	
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
            	Message msg = mEntireRefreshCalendarsCompletionHandler.obtainMessage();
            	msg.what = ENTIRE_REFRESH_CALENDARS_COMPLETION;    	
            	mEntireRefreshCalendarsCompletionHandler.sendMessage(msg);
            }
        }
    }
	
	private static final int ENTIRE_REFRESH_CALENDARS_COMPLETION = 1;
	EntireRefreshCalendarsCompletionHandler mEntireRefreshCalendarsCompletionHandler;
    private class EntireRefreshCalendarsCompletionHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
             
            switch (msg.what) {     
            case ENTIRE_REFRESH_CALENDARS_COMPLETION:
            	//int sizeChildrenCursors = mChildrenCursors.size(); // -> 5��
            	//int sizeCurrentCalendarGroups = mCurrentCalendarGroupsByApp.size(); // -> 3��
            	//mSelectCalendarsLinearLayout.setVisibility(View.VISIBLE);
            	            	
                synchronized (mChildrenCursors) {
                    for (String key : mChildrenCursors.keySet()) {
                    	
                    	if (key.compareTo((String) mECalendarsCookie) == 0) {
                    		Cursor eCalendarCursor = mChildrenCursors.get(mECalendarsCookie);
                    		
                    		String eCalendarGroupAccountType = mResources.getString(R.string.ecalendar_psudo_account_type);
                        	String eCalendarGroupCalendarLabel = mCurrentCalendarGroupsByApp.get(mResources.getString(R.string.ecalendar_psudo_account_type));    
                    		CalendarGroupInfo eCalendarGroupInfo = new CalendarGroupInfo(eCalendarGroupAccountType, eCalendarGroupCalendarLabel);
                    		
                        	eCalendarCursor.moveToPosition(-1);
                            while (eCalendarCursor.moveToNext()) {
                            	int accessLevel = eCalendarCursor.getInt(CALENDAR_ACCESS_LEVEL_COLUMN);
                            	if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
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
                            	int accessLevel = googleCalendarsCursor.getInt(CALENDAR_ACCESS_LEVEL_COLUMN);
                            	if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
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
                        		
                        		CalendarGroupInfo otherGroupInfo = (CalendarGroupInfo) mOtherCalendarsGroupContainer.getTag();                        		
                        		
                        		if (otherGroupInfo == null)
                        			otherGroupInfo = new CalendarGroupInfo(otherGroupAccountType, otherGroupCalendarLabel);
                                
                        		otherCalendarsCursor.moveToPosition(-1);
                                while (otherCalendarsCursor.moveToNext()) {     
                                	int accessLevel = otherCalendarsCursor.getInt(CALENDAR_ACCESS_LEVEL_COLUMN);
                                	if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
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
                                	int accessLevel = calendarsCursor.getInt(CALENDAR_ACCESS_LEVEL_COLUMN);
                                	if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
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
                                
            	//initAllCalendarVisibleUnVisibleStateText();
            	            	
            	makeMainViewLowerLayout();
            	break;
            }
            
        }         
    }   
    
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
    	
    	public String getCalendarDispName() {
    		return this.calendarDispName;
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
	
	/*public CommonLinearLayoutItemContainer makeCalendarGroupContainer(String calendarLabel) {
		CommonLinearLayoutItemContainer calendarGroupContainer = (CommonLinearLayoutItemContainer)mInflater.inflate(R.layout.selectcalendars_group_container, null);		
		RelativeLayout groupIndicator = (RelativeLayout)calendarGroupContainer.findViewById(R.id.group_indicator);
        TextView indicatorTextView = (TextView)groupIndicator.findViewById(R.id.group_indicator_text);
        indicatorTextView.setText(calendarLabel);
        return calendarGroupContainer;        
	}*/
	
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
		int paddingLeft = (int) (mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconLeftMargin) /*+
				mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconWidth) + 
				mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconLeftMargin)*/);
		CommonRelativeLayoutItemContainer calendarGroupItem = 
        		new CommonRelativeLayoutItemContainer(mContext, paddingLeft, CommonRelativeLayoutItemContainer.LOWER_BOLDER_LINE_POSITION);
        
        int itemContainerHeight = (int) mResources.getDimension(R.dimen.selectCalendarViewItemHeight);
        RelativeLayout.LayoutParams itemConatainerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemContainerHeight);
        calendarGroupItem.setLayoutParams(itemConatainerLayoutParams);
        calendarGroupItem.setTag(calendarId);
        calendarGroupItem.setBackground(mResources.getDrawable(R.drawable.common_layout_selector));
        calendarGroupItem.setClickable(true);
        calendarGroupItem.setOnClickListener(mCalendarItemClickListener);/////////////////////////////////////////////////////////////////////////////////////
                
        // Calendars.VISIBLE ���� Ȯ���ؾ� �Ѵ�
        /*
        ImageView visibleStateMarkIcon = makeVisibleStateMarkIcon();
        if (visibleState == 0)
        	visibleStateMarkIcon.setVisibility(View.INVISIBLE);
        */
        
        View calendarColorIcon = makeCalendarColorIcon(calendarColor);
        TextView calendarNameText = makeCalendarNameTextView(calendarColorIcon, calendarName);
        //ImageView exclamationMarkIcon = makeExclamationMarkIcon(calendarId);
        
        //calendarGroupItem.addView(visibleStateMarkIcon);        
        calendarGroupItem.addView(calendarColorIcon);
        calendarGroupItem.addView(calendarNameText);
        if (mCurrentDefaultCalendarId != -1) {
        	if (calendarId == mCurrentDefaultCalendarId) {
        		calendarGroupItem.addView(mSelectedMakrIcon);
        	}
        }
        //calendarGroupItem.addView(exclamationMarkIcon);    
        
        return calendarGroupItem;
	}
	
	public View makeCalendarColorIcon(int calendarColor) {
		View calendarColorIcon = new View(mContext);		
		
		int leftMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconLeftMargin);
		
		int width = (int) mResources.getDimension(R.dimen.selectCalendarViewCalendarColorIconWidth);
		int height = width;
		RelativeLayout.LayoutParams calendarIconParams = new RelativeLayout.LayoutParams(width, height);
		
		calendarIconParams.setMargins(leftMargin, 0, 0, 0);
		calendarIconParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);	
		calendarIconParams.addRule(RelativeLayout.CENTER_VERTICAL);
		calendarColorIcon.setLayoutParams(calendarIconParams);
		
		calendarColorIcon.setId(1);	
		
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
	
	
	public void makeSelectedMarkIcon() {	
		
		mSelectedMarkDrawable = mResources.getDrawable(R.drawable.ic_menu_done_holo_light);
		int irColor = Color.rgb(0, 0, 255);
		mSelectedMarkDrawable.setColorFilter(new PorterDuffColorFilter(irColor, PorterDuff.Mode.MULTIPLY));
		
		int width = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconWidth);
		RelativeLayout.LayoutParams selectedMarkIconParams = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		selectedMarkIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		selectedMarkIconParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		
		int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconLeftMargin);
		selectedMarkIconParams.setMargins(0, 0, rightMargin, 0);	
		
		mSelectedMakrIcon = new ImageView(mContext);
		mSelectedMakrIcon.setLayoutParams(selectedMarkIconParams);		
		mSelectedMakrIcon.setImageDrawable(mSelectedMarkDrawable);
		mSelectedMakrIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		mSelectedMakrIcon.setId(R.id.selected_default_calendar_mark_icon);
	}
	
	public void makeMainViewLowerLayout() {
		RelativeLayout lastSeperator = (RelativeLayout)mInflater.inflate(R.layout.selectcalendars_seperator_layout, null);
    	mInnerLinearLayoutOfScrollView.addView(lastSeperator);
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
    
    private ArrayList<String> loadStringArray(int resNum) {
        String[] labels = mResources.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }
}
