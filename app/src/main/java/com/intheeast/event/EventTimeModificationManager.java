package com.intheeast.event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
//import android.text.format.Time;
import android.util.Log;

import com.intheeast.calendarcommon2.EventRecurrence;
import com.intheeast.colorpicker.HsvColorComparator;
import com.intheeast.acalendar.CalendarEventModel;
import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.EventRecurrenceFormatter;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.RecurrenceContext;
import com.intheeast.acalendar.CalendarEventModel.Attendee;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.acalendar.Utils;

public class EventTimeModificationManager {

	Activity mActivity;	
	
	EditEventHelper mHelper;
    CalendarEventModel mModel;
    CalendarEventModel mOriginalModel;
    
    
    private ArrayList<ReminderEntry> mReminders = null;
    
    private Uri mUri;
    private long mOriginalStartMills;
    private long mOriginalEndMills;
    
    private Handler mHandler;
    
    private AtomicInteger mSequenceNumber = new AtomicInteger();
	private LinkedBlockingQueue<LoadRequest> mLoaderQueue;
	private LoaderThread mLoaderThread;
    private ContentResolver mResolver;
    private Resources mResources;
    
    String mRrule;
    private EventRecurrence mEventRecurrence = new EventRecurrence();   
    private RecurrenceContext mRRuleContext;
    
	private static interface LoadRequest {
        public void processRequest(EventTimeModificationManager eventLoader);
        public void skipRequest(EventTimeModificationManager eventLoader);
    }
	
	private static class ShutdownRequest implements LoadRequest {
        public void processRequest(EventTimeModificationManager eventLoader) {
        }

        public void skipRequest(EventTimeModificationManager eventLoader) {
        }
    }   
    
    
    public EventTimeModificationManager (Activity activity) {
    	mActivity = activity;
    	        
    	mHelper = new EditEventHelper(mActivity, null);
    	mLoaderQueue = new LinkedBlockingQueue<LoadRequest>();
        mResolver = mActivity.getApplicationContext().getContentResolver();
        mResources = mActivity.getResources();
        mHandler = new Handler();        
    }
        
    
    /**
     * Call this from the activity's onResume()
     */
    public void startBackgroundThread() {
        mLoaderThread = new LoaderThread(mLoaderQueue, this);
        mLoaderThread.start();
    }

    /**
     * Call this from the activity's onPause()
     */
    public void stopBackgroundThread() {
        mLoaderThread.shutdown();
    }
    
    public void setNewEventTime(long newStart, long newEnd) {
    	// ���ο� start/end �ð��� ��������
        mModel.mStart = newStart;
        mModel.mEnd = newEnd;        
        
        int freqValue = mRRuleContext.getFreq();
        int interval = mRRuleContext.getFreqInterval();
        
        mRRuleContext = new RecurrenceContext(mActivity, mModel.mStart, mModel.mTimezone, null);        
        mRRuleContext.setFreqAndInterval(freqValue, interval);
    	mRrule = mRRuleContext.makeRRule();    	
    	
        if (!TextUtils.isEmpty(mRrule)) {
        	
        	mEventRecurrence.parse(mRrule);
        	if (mEventRecurrence.startDate == null) {
        		Calendar startDate = GregorianCalendar.getInstance(TimeZone.getTimeZone(mModel.mTimezone));//new Time(mModel.mTimezone);
        		startDate.setTimeInMillis(mModel.mStart);
                mEventRecurrence.startDate = startDate;
            }
        	
        	// 4th parameter�� ���� �� ��Ȯ�� ���� ��������
        	// :���� ������ true�� �����ϰ� �ִ�
        	String repeatString = EventRecurrenceFormatter.getRepeatString(mActivity, mResources,
                    mEventRecurrence, true);
        	
            if (repeatString == null) {                
                Log.i("tag", "Can't generate display string for " + mRrule);
                mCanHandleRecurrenceRule = false;
            } else {
                // TODO Should give option to clear/reset rrule
            	mCanHandleRecurrenceRule = RecurrenceContext.canHandleRecurrenceRule(mEventRecurrence);
                if (!mCanHandleRecurrenceRule) {
                	Log.i("tag", "UI can't handle " + mRrule);
                }
            }
            
            if (mCanHandleRecurrenceRule) {             	
            	mModel.mRrule = mRrule;
            }
            
        }

    }
    
	public void getEventInfo() {
                
        getTOKEN_EVENT(mEventCursors.mEventCursor);
        
        if (mModel.mHasAttendeeData) {
        	getTOKEN_ATTENDEES(mEventCursors.mAttendeesCursor);
        } 
        
        // TOKEN_REMINDERS
        if (mModel.mHasAlarm && mReminders == null) {
        	getTOKEN_REMINDERS(mEventCursors.mRemindersCursor);
        } 
        
        getTOKEN_CALENDARS(mEventCursors.mCalendarsCursor);

        getTOKEN_COLORS(mEventCursors.mColorsCursor);
    }	
	
	boolean mCanHandleRecurrenceRule = true;
	private void getTOKEN_EVENT(Cursor cursor) {
		//long eventId;
		
		if (cursor.getCount() == 0) {
            // The cursor is empty. This can happen if the event was deleted.
            cursor.close();
            
            return;
        }
		
        mOriginalModel = new CalendarEventModel();
        EditEventHelper.setModelFromCursor(mOriginalModel, cursor);
        EditEventHelper.setModelFromCursor(mModel, cursor);
        cursor.close();

        mOriginalModel.mUri = mUri.toString();

        mModel.mUri = mUri.toString();
        mModel.mOriginalStart = mOriginalStartMills;
        mModel.mOriginalEnd = mOriginalEndMills;
        
        // PROJECTION_BEGIN_INDEX is Instances.BEGIN[The beginning time of the instance, in UTC milliseconds.]
        // long eStart = cEvents.getLong(PROJECTION_BEGIN_INDEX);
        // e.startMillis = eStart;
        // mBegin is e.startMillis???
        mModel.mIsFirstEventInSeries = mOriginalStartMills == mOriginalModel.mStart; 
        
        mRRuleContext = new RecurrenceContext(mActivity, mModel.mStart, mModel.mTimezone, mModel.mRrule);
        mRrule = mRRuleContext.makeRRule();  
	}
	
	
	public void getTOKEN_ATTENDEES(Cursor cursor) {
		try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(EditEventHelper.ATTENDEES_INDEX_NAME);
                String email = cursor.getString(EditEventHelper.ATTENDEES_INDEX_EMAIL);
                int status = cursor.getInt(EditEventHelper.ATTENDEES_INDEX_STATUS);
                int relationship = cursor
                        .getInt(EditEventHelper.ATTENDEES_INDEX_RELATIONSHIP);
                
                if (relationship == Attendees.RELATIONSHIP_ORGANIZER) {
                    if (email != null) {
                        mModel.mOrganizer = email;
                        mModel.mIsOrganizer = mModel.mOwnerAccount
                                .equalsIgnoreCase(email);
                        mOriginalModel.mOrganizer = email;
                        mOriginalModel.mIsOrganizer = mOriginalModel.mOwnerAccount
                                .equalsIgnoreCase(email);
                    }

                    if (TextUtils.isEmpty(name)) {
                        mModel.mOrganizerDisplayName = mModel.mOrganizer;
                        mOriginalModel.mOrganizerDisplayName =
                                mOriginalModel.mOrganizer;
                    } else {
                        mModel.mOrganizerDisplayName = name;
                        mOriginalModel.mOrganizerDisplayName = name;
                    }
                }

                if (email != null) {
                    if (mModel.mOwnerAccount != null &&
                            mModel.mOwnerAccount.equalsIgnoreCase(email)) {
                        int attendeeId =
                            cursor.getInt(EditEventHelper.ATTENDEES_INDEX_ID);
                        mModel.mOwnerAttendeeId = attendeeId;
                        mModel.mSelfAttendeeStatus = status;
                        mOriginalModel.mOwnerAttendeeId = attendeeId;
                        mOriginalModel.mSelfAttendeeStatus = status;
                        continue;
                    }
                }
                
                Attendee attendee = new Attendee(name, email);
                attendee.mStatus = status;
                mModel.addAttendee(attendee);
                mOriginalModel.addAttendee(attendee);
            }
        } finally {
            cursor.close();
        }
	}
	
	public void getTOKEN_REMINDERS(Cursor cursor) {
		try {
            // Add all reminders to the models
            while (cursor.moveToNext()) {
                int minutes = cursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
                int method = cursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);
                ReminderEntry re = ReminderEntry.valueOf(minutes, method);
                mModel.mReminders.add(re);
                mOriginalModel.mReminders.add(re);
            }

            // Sort appropriately for display
            Collections.sort(mModel.mReminders);
            Collections.sort(mOriginalModel.mReminders);
        } finally {
            cursor.close();
        }
	}
	
	public void getTOKEN_CALENDARS(Cursor cursor) {
		try {
			// Populate model for an existing event
            EditEventHelper.setModelFromCalendarCursor(mModel, cursor);
            EditEventHelper.setModelFromCalendarCursor(mOriginalModel, cursor);
           
        } finally {
            cursor.close();
        }
	}
	
	
	public void getTOKEN_COLORS(Cursor cursor) {
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
        
	}
	
	// modify_event => Change only this event 
	//                 :This choice says to change just this one instance of this repeating event
	// modify_all => Change all events in the series
	//               :This choice says to change all occurrences of this repeating event
	// modify_all_following => Change this and all future events
	//                         :This choice says to change this instance and all future occurrences of this repeating event
	public int setMenuInDB() {
		final boolean notSynced = TextUtils.isEmpty(mModel.mSyncId);
        boolean isFirstEventInSeries = mModel.mIsFirstEventInSeries;
        int itemIndex = 0;
        CharSequence[] items;

        int menuId = 0;
        if (notSynced) {
            // If this event has not been synced, then don't allow deleting
            // or changing a single instance.
            if (isFirstEventInSeries) { //-------------------------------------------------------------->1
                // Still display the option so the user knows all events are changing
                items = new CharSequence[1]; // modify_all �� �����Ѵ�
                                             // ���� �ϳ��� �����Ѵ�
                menuId = 1;
            } else {//---------------------------------------------------------------------------------->2
                items = new CharSequence[2]; // modify_all
                                             // modify_all_following
                                             // �� ���� �����ϰ� �ȴ�
                menuId = 2;
            }
        } else {
            if (isFirstEventInSeries) {//--------------------------------------------------------------->3
                items = new CharSequence[2]; // modify_event
                                             // modify_all
                							 // �� ���� �����ϰ� �ȴ�
                menuId = 3;
            } else {//---------------------------------------------------------------------------------->4
                items = new CharSequence[3]; // modify_event
                                             // modify_all
                                             // modify_all_following
                                             // �� ���� �����ϰ� �ȴ�
                menuId = 4;
            }
            
            items[itemIndex++] = mActivity.getText(R.string.modify_event);                                                                            
        }
        
        items[itemIndex++] = mActivity.getText(R.string.modify_all); 

        // Do one more check to make sure this remains at the end of the list
        if (!isFirstEventInSeries) {
            items[itemIndex++] = mActivity.getText(R.string.modify_all_following);                                                                                    
        }
        
        return menuId;
	}
	
	public int getMenuInDB() {
		return mMenuCaseId;
	}

		
	int mModification = com.intheeast.acalendar.Utils.MODIFY_UNINITIALIZED;
	public boolean editEvent() {
		if (  ( EditEventHelper.canRespond(mModel) || EditEventHelper.canModifyEvent(mModel) )                
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
            
            Log.i("tag", "editEvent : " + stringResource);
            return true;
            //Toast.makeText(mActivity, stringResource, Toast.LENGTH_SHORT).show();
        }
		else {
			return false;
		}
	}
	
		
	public boolean modifyEvent(int modification) {
		mModification = modification;
		
		return editEvent();
	}
	
		
	private static class LoaderThread extends Thread {
        LinkedBlockingQueue<LoadRequest> mQueue;
        EventTimeModificationManager mEventLoader;

        public LoaderThread(LinkedBlockingQueue<LoadRequest> queue, EventTimeModificationManager eventLoader) {
            mQueue = queue;
            mEventLoader = eventLoader;
        }

        public void shutdown() {
            try {
                mQueue.put(new ShutdownRequest());
            } catch (InterruptedException ex) {
                // The put() method fails with InterruptedException if the
                // queue is full. This should never happen because the queue
                // has no limit.
                Log.e("Cal", "LoaderThread.shutdown() interrupted!");
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    // Wait for the next request
                    LoadRequest request = mQueue.take();

                    // If there are a bunch of requests already waiting, then
                    // skip all but the most recent request.
                    while (!mQueue.isEmpty()) {
                        // Let the request know that it was skipped
                        request.skipRequest(mEventLoader);

                        // take �޼ҵ� ����� ť�� ����� ��쿡�� ���� ���ö����� ����Ѵ�.
                        // Skip to the next request
                        request = mQueue.take();
                    }

                    if (request instanceof ShutdownRequest) {
                        return;
                    }
                    
                    request.processRequest(mEventLoader);
                    
                } catch (InterruptedException ex) {
                    Log.e("Cal", "background LoaderThread interrupted!");
                }
            }
        }
    }
		
	private class LoadEventInfoRequest implements LoadRequest {

        public int id;        
        public Event event;
        //public EventCursors eventCursors;
        public Runnable successCallback;
        public Runnable cancelCallback;

        public LoadEventInfoRequest(int id, Event event, /*EventCursors eventCursors,*/
                final Runnable successCallback, final Runnable cancelCallback) {
        	
            this.id = id;            
            this.event = event;
            //this.eventCursors = eventCursors;
            this.successCallback = successCallback;
            this.cancelCallback = cancelCallback;
        }        
        
        public void processRequest(EventTimeModificationManager eventsInfoLoader) {
                    	        	
        	processSingleEventInfoQuery(eventsInfoLoader.mResolver, this.event);
        	
            // Check if we are still the most recent request.
            if (id == eventsInfoLoader.mSequenceNumber.get()) {
            	eventsInfoLoader.mHandler.post(successCallback);
            } else {
            	eventsInfoLoader.mHandler.post(cancelCallback);
            }
        }

        public void skipRequest(EventTimeModificationManager eventsInfoLoader) {
        	eventsInfoLoader.mHandler.post(cancelCallback);
        }	
                
        private void processSingleEventInfoQuery(ContentResolver cr, Event event) {      	      	
        	
        	EventTimeModificationManager.this.mEventCursors = new EventCursors(event.id);
        	
        	Cursor eventCursor = getEvent(cr, event);
        	if (eventCursor == null)
        		return ; // ����ó���� �ؾ� �ϴ� ���� �ƴѰ�?
        	else 
        		eventCursor.moveToFirst();
        	EventTimeModificationManager.this.mEventCursors.setEventCursor(eventCursor);  
        	
        	Cursor attendeesCursor = null;
        	boolean hasAttendeeData = eventCursor.getInt(EditEventHelper.EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
        	if (hasAttendeeData) {
        		attendeesCursor = getAttendee(cr, event.id);
        		EventTimeModificationManager.this.mEventCursors.setAttendeesCursor(attendeesCursor);
        	}
    		
    		
        	Cursor remindersCursor = null;
        	boolean hasAlarm = eventCursor.getInt(EditEventHelper.EVENT_INDEX_HAS_ALARM) != 0;
        	if (hasAlarm) {
        		remindersCursor = getReminders(cr, event.id);
        		EventTimeModificationManager.this.mEventCursors.setRemindersCursor(remindersCursor);
        		EventTimeModificationManager.this.mEventCursors.setHasAlarm(true);     
            }       		
        	
        	long calendarId = eventCursor.getLong(EditEventHelper.EVENT_INDEX_CALENDAR_ID);
        	Cursor calendarCursor = getCalendar(cr, calendarId);
        	if (calendarCursor == null)
            	return; // ����ó���� �ؾ� �ϴ� ���� �ƴѰ�?
        	else 
        		calendarCursor.moveToFirst();
        	EventTimeModificationManager.this.mEventCursors.setCalendarsCursor(calendarCursor);   
                  	
        	Cursor colorCursor = getColors(cr);
        	if (colorCursor != null)
        		EventTimeModificationManager.this.mEventCursors.setColorsCursor(colorCursor);			
			
			eventCursor.close();
			calendarCursor.close();
			
			if (attendeesCursor != null)
				attendeesCursor.close();            
			if (remindersCursor != null)
				remindersCursor.close();
			if (colorCursor != null)
				colorCursor.close();
        }
        
        private Cursor getEvent(ContentResolver cr, Event event) {
        	Uri eventQueryUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);     
        	        	
        	Cursor eventCursor = processSingleEventItemQuery(cr, eventQueryUri, EditEventHelper.EVENT_PROJECTION, null, null, null);        	
        	return eventCursor;        	   
        }
        
        private Cursor getCalendar(ContentResolver cr, long calendarId) {
        	
        	Uri calendarQueryUri = Calendars.CONTENT_URI;
            String[] args = new String[] {
                    Long.toString(calendarId)};
            Cursor calendarCursor = processSingleEventItemQuery(cr, calendarQueryUri, EditEventHelper.CALENDARS_PROJECTION, EditEventHelper.CALENDARS_WHERE, args, null);
            return calendarCursor;            
        }       
        
        
        private Cursor getAttendee(ContentResolver cr, long eventId) {              
        	Cursor attendeesCursor = null;
                        
            String[] args = new String[] { Long.toString(eventId) };

            // start attendees query
            Uri attendeesQueryUri = Attendees.CONTENT_URI;                
            attendeesCursor = processSingleEventItemQuery(cr, attendeesQueryUri, EditEventHelper.ATTENDEES_PROJECTION, EditEventHelper.ATTENDEES_WHERE, args, null);
            
            return attendeesCursor;                    
        }
        
        private Cursor getReminders(ContentResolver cr, long eventId) {                        
        	           
        	    
            // start reminders query
            String[] args = new String[] { Long.toString(eventId) };
            Uri remindersQueryUri = Reminders.CONTENT_URI;                
            Cursor remindersCursor = processSingleEventItemQuery(cr, remindersQueryUri, EditEventHelper.REMINDERS_PROJECTION, EditEventHelper.REMINDERS_WHERE, args, null);
            
            return remindersCursor;            
        }
        
        private Cursor getColors(ContentResolver cr) {        	
        	       	
        	Uri colorQueryUri = Colors.CONTENT_URI;                
            Cursor colorsCursor = processSingleEventItemQuery(cr, colorQueryUri, EditEventHelper.COLORS_PROJECTION, Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT, null, null);
             
            return colorsCursor;
        }        
    }	
	
	
	private static Cursor processSingleEventItemQuery(ContentResolver cr, 
            Uri uri, 
            String[] projection, 
            String selection, 
            String[] selectionArgs, 
            String orderBy) {    
    	
    	Cursor cursor = null;  	
		Cursor newCursor = null;  	
		
		try {
			cursor = cr.query(uri, projection, selection,
					selectionArgs, orderBy);
		
			newCursor = Utils.matrixCursorFromCursor(cursor);
		
		} catch (Exception e) {
			//Log.i(TAG, e.toString());				
		} finally {
			cursor.close();
		}
		
		return newCursor;			 	
    }
    	
	public static class EventCursors {
		public long mEventId = -1;
		public Cursor mEventCursor = null;
		public Cursor mCalendarsCursor = null;
		public Cursor mAttendeesCursor = null;
		public Cursor mRemindersCursor = null;
		public Cursor mColorsCursor = null;
		
		boolean mIsBusyFreeCalendar = false;
		SpannableStringBuilder mCalendarNameSB = null;
		boolean mHasAlarm = false;
		
		public EventCursors() {
			
		}
		
		public EventCursors(long id) {
			mEventId = id;			
		}
				
		public void setEventCursor(Cursor cursor) {
			mEventCursor = cursor;
		}
		
		public void setCalendarsCursor(Cursor cursor) {
			mCalendarsCursor = cursor;
		}
		
		public void setAttendeesCursor(Cursor cursor) {
			mAttendeesCursor = cursor;
		}
		
		public void setRemindersCursor(Cursor cursor) {
			mRemindersCursor = cursor;
		}
		
		public void setColorsCursor(Cursor cursor) {
			mColorsCursor = cursor;
		}
		
		public void setIsBusyFreeCalendar(boolean is) {
			mIsBusyFreeCalendar = is;
		}
		
		public void setCalendarName(SpannableStringBuilder sb) {
			mCalendarNameSB = sb;
		}
		
		public void setHasAlarm(boolean has) {
			mHasAlarm = has;
		}	
		
		public void closeCursors() {
			if (mEventCursor != null)
				mEventCursor.close();
			if (mCalendarsCursor != null)
				mCalendarsCursor.close();
			if (mAttendeesCursor != null)
				mAttendeesCursor.close();
			if (mRemindersCursor != null)
				mRemindersCursor.close();
			
		}
	}	
	
	EventCursors mEventCursors;	
	Event mLoadRequestedEvent;
	int mMenuCaseId;
	boolean mCompletionLoadEventJob = false;
	boolean mCancelLoadEventJob = false;
	long mEventId;
	public void loadEventInfoInBackgroundForEditEventTime(Event event) {
		mCompletionLoadEventJob = false;
		mCancelLoadEventJob = false;
		
		mLoadRequestedEvent = event;
		mEventId = event.id;
		
        // Increment the sequence number for requests.  We don't care if the
        // sequence numbers wrap around because we test for equality with the
        // latest one.
        int id = mSequenceNumber.incrementAndGet();
        
        // Send the load request to the background thread
        LoadEventInfoRequest request = new LoadEventInfoRequest(id, event, mEventInfoLoaderSuccessCallback, mEventInfoLoaderCancelCallback);

        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventsInfoInBackground() interrupted!");
        }
    }
	
	public long getEventIdLoadedEventInfo() {
		return mEventId;
	}
	
	public boolean getLoadEventJopCompletionStatus() {
		return mCompletionLoadEventJob;
	}
	
	public boolean isCanceledLoadEventJob() {
		return mCancelLoadEventJob;
	}
	
	Runnable mEventInfoLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mLoadRequestedEvent.id);           
        	mModel = new CalendarEventModel(mActivity); 
        	mOriginalStartMills = mLoadRequestedEvent.startMillis;
            mOriginalEndMills = mLoadRequestedEvent.endMillis;
            
            getEventInfo();
            mMenuCaseId = setMenuInDB();
            mCompletionLoadEventJob = true;
            mCancelLoadEventJob = false;
        }
	};
	
	Runnable mEventInfoLoaderCancelCallback = new Runnable() {
    	
        public void run() {
        	mCancelLoadEventJob = true;
        }
	};
}











