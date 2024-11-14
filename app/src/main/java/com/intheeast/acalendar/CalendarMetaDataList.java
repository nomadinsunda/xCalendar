package com.intheeast.acalendar;

import java.util.ArrayList;

import com.intheeast.event.EditEventHelper;


import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.CalendarContract.Calendars;

public class CalendarMetaDataList {	
    private static final int TOKEN_CALENDARS = 1 << 3;    
    
    int mCalendarIDColumnIndex;
    int mCalendarDisplayNameColumnIndex;
    int mCalendarAccountNameColumnIndex;
    int mCalendarOwnerAccountColumnIndex;
    int mCalendarAccountTypeColumnIndex;
    int mCalendarColorColumnIndex;    
    
	Cursor mCalendarsCursor;
	QueryHandler mHandler;
	Activity mActivity;
	public Runnable mCallback;
	
	public ArrayList<CalendarInfo> mCalendarInfoList; 
	
	private class QueryHandler extends AsyncQueryHandler {		
	    public QueryHandler(ContentResolver cr) {
	        super(cr);
	    }
	
	    @Override
	    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
	    	if (cursor == null) {
	            return;
	        }
	    	
	    	if (mActivity == null || mActivity.isFinishing()) {
	            cursor.close();
	            return;
	        }
	    	
	    	switch (token) {
	    	case TOKEN_CALENDARS:
                try {                    
                    MatrixCursor matrixCursor = Utils.matrixCursorFromCursor(cursor);                    
                    if (getCalendarsTableColumnIndex(matrixCursor)) {
                    	storeCalendarsMetaData();  
                    	mCallback.run();
                    }
                    
                } finally {
                    cursor.close();
                }
                break;
            default:
            	break;
	    	}
	    }
	};
	
	public CalendarMetaDataList(Activity activity, Runnable run) {
		mActivity = activity;
		mCallback = run;
		
		mHandler = new QueryHandler(mActivity.getContentResolver());
		
		mCalendarInfoList = null;
		
		// Start a query in the background to read the list of calendars and colors
        mHandler.startQuery(TOKEN_CALENDARS, null, Calendars.CONTENT_URI,
                EditEventHelper.CALENDARS_PROJECTION,
                EditEventHelper.CALENDARS_WHERE_WRITEABLE_VISIBLE, null /* selection args */,
                null /* sort order */);
	}
	
	public ArrayList<CalendarInfo> getCalendarInfoList() {
		return mCalendarInfoList;
	}
	
	public boolean getCalendarsTableColumnIndex (Cursor cursor) {
		mCalendarsCursor = cursor;
		if (cursor == null || cursor.getCount() == 0)
			return false;
        		
		mCalendarIDColumnIndex = mCalendarsCursor.getColumnIndexOrThrow(Calendars._ID);
		mCalendarDisplayNameColumnIndex = mCalendarsCursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
        mCalendarAccountNameColumnIndex = mCalendarsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
        mCalendarOwnerAccountColumnIndex = mCalendarsCursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
        mCalendarAccountTypeColumnIndex = mCalendarsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);  // LOCAL 유무를 판별한다      
        mCalendarColorColumnIndex = mCalendarsCursor.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
        
        return true;
	}
	
	public void storeCalendarsMetaData() {
		mCalendarInfoList = new ArrayList<CalendarInfo>();        
        
        mCalendarsCursor.moveToPosition(-1);
        while(mCalendarsCursor.moveToNext()) {
        	getCalendarInfo(mCalendarsCursor);
        }  
	}
	
	public void getCalendarInfo(Cursor cursor) {
		//Log.i("tag", "*******************************************************");
		//Log.i("tag", "getCalendarInfo");
		
		int calendarId = cursor.getInt(mCalendarIDColumnIndex);
		String displayName = cursor.getString(mCalendarDisplayNameColumnIndex);		
		String ownerAccount = cursor.getString(mCalendarOwnerAccountColumnIndex);
		String accountName = cursor.getString(mCalendarAccountNameColumnIndex); 
        String accountType = cursor.getString(mCalendarAccountTypeColumnIndex);
        int calendarColor = cursor.getInt(mCalendarColorColumnIndex);
        
        CalendarInfo object = new CalendarInfo(calendarId,
        		                               displayName, 
        		                               ownerAccount,
        		                               accountName,         		                               
        		                               accountType,
        		                               calendarColor);
        mCalendarInfoList.add(object);
        /*
        Log.i("tag", "getCalendarInfo : calendar_id = " + String.valueOf(calendarId));
        Log.i("tag", "getCalendarInfo : displayName = " + displayName);
        Log.i("tag", "getCalendarInfo : ownerAccount = " + ownerAccount);        
        Log.i("tag", "getCalendarInfo : accountName = " + accountName);
        Log.i("tag", "getCalendarInfo : accountType = " + accountType);
        Log.i("tag", "*******************************************************");  
        */      
	}
	
	
    public class CalendarInfo {
    	int mCalendarId;    	
    	String mDisplayName;
    	String mOwnerAccount;
    	String mAccountName;
    	String mAccountType;
    	int mCalendarColor;
    	public CalendarInfo(int calendarId, String displayName, String ownerAccount, String accountName, String accountType, int color) {
    		mCalendarId = calendarId;
    		mDisplayName = displayName;
    		mOwnerAccount = ownerAccount;
        	mAccountName = accountName;
        	mAccountType = accountType;
        	mCalendarColor = color;
    	}
    }
}
