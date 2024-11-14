package com.intheeast.acalendar;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.util.Log;

public class EventInfoLoader {
	
	private static final boolean INFO = true;
    private static final String TAG = "EventInfoLoader";
    
	// Query tokens for QueryHandler
    private static final int TOKEN_QUERY_EVENT = 1 << 0;
    private static final int TOKEN_QUERY_CALENDARS = 1 << 1;
    private static final int TOKEN_QUERY_ATTENDEES = 1 << 2;
    public static final int TOKEN_QUERY_DUPLICATE_CALENDARS = 1 << 3;
    private static final int TOKEN_QUERY_REMINDERS = 1 << 4;
    public static final int TOKEN_QUERY_VISIBLE_CALENDARS = 1 << 5;
    private static final int TOKEN_QUERY_COLORS = 1 << 6;
    
    private static final int TOKEN_QUERY_ALL = TOKEN_QUERY_DUPLICATE_CALENDARS
            | TOKEN_QUERY_ATTENDEES | TOKEN_QUERY_CALENDARS | TOKEN_QUERY_EVENT
            | TOKEN_QUERY_REMINDERS | TOKEN_QUERY_VISIBLE_CALENDARS;
	
    
    
    private static final String[] EVENT_PROJECTION = new String[] {
        Events._ID,                  // 0  do not remove; used in DeleteEventHelper
        Events.TITLE,                // 1  do not remove; used in DeleteEventHelper
        Events.RRULE,                // 2  do not remove; used in DeleteEventHelper
        Events.ALL_DAY,              // 3  do not remove; used in DeleteEventHelper
        Events.CALENDAR_ID,          // 4  do not remove; used in DeleteEventHelper
        Events.DTSTART,              // 5  do not remove; used in DeleteEventHelper
        Events._SYNC_ID,             // 6  do not remove; used in DeleteEventHelper
        Events.EVENT_TIMEZONE,       // 7  do not remove; used in DeleteEventHelper
        Events.DESCRIPTION,          // 8
        Events.EVENT_LOCATION,       // 9
        Calendars.CALENDAR_ACCESS_LEVEL, // 10
        Events.CALENDAR_COLOR,       // 11
        Events.EVENT_COLOR,          // 12
        Events.HAS_ATTENDEE_DATA,    // 13
        Events.ORGANIZER,            // 14
        Events.HAS_ALARM,            // 15
        Calendars.MAX_REMINDERS,     // 16
        Calendars.ALLOWED_REMINDERS, // 17
        Events.CUSTOM_APP_PACKAGE,   // 18
        Events.CUSTOM_APP_URI,       // 19
        Events.DTEND,                // 20
        Events.DURATION,             // 21
        Events.ORIGINAL_ID,          // 22 do not remove; to support EditEventFragemnt
        Events.ORIGINAL_SYNC_ID,     // 23 do not remove; used in DeleteEventHelper
        Events.AVAILABILITY,         // 24 do not remove; to support EditEventFragemnt
        Events.OWNER_ACCOUNT,        // 25 do not remove; to support EditEventFragemnt
        Events.GUESTS_CAN_MODIFY,    // 26 do not remove; to support EditEventFragemnt
        Events.STATUS, 				 // 27 do not remove; to support EditEventFragemnt
        
    };
    
    public static final int EVENT_INDEX_ID = 0;
    public static final int EVENT_INDEX_TITLE = 1;
    public static final int EVENT_INDEX_RRULE = 2;
    public static final int EVENT_INDEX_ALL_DAY = 3;
    public static final int EVENT_INDEX_CALENDAR_ID = 4;
    public static final int EVENT_INDEX_DTSTART = 5;
    public static final int EVENT_INDEX_SYNC_ID = 6;
    public static final int EVENT_INDEX_EVENT_TIMEZONE = 7;
    public static final int EVENT_INDEX_DESCRIPTION = 8;
    public static final int EVENT_INDEX_EVENT_LOCATION = 9;
    public static final int EVENT_INDEX_ACCESS_LEVEL = 10;
    public static final int EVENT_INDEX_CALENDAR_COLOR = 11;
    public static final int EVENT_INDEX_EVENT_COLOR = 12;
    public static final int EVENT_INDEX_HAS_ATTENDEE_DATA = 13;
    public static final int EVENT_INDEX_ORGANIZER = 14;
    public static final int EVENT_INDEX_HAS_ALARM = 15;
    public static final int EVENT_INDEX_MAX_REMINDERS = 16;
    public static final int EVENT_INDEX_ALLOWED_REMINDERS = 17;
    public static final int EVENT_INDEX_CUSTOM_APP_PACKAGE = 18;
    public static final int EVENT_INDEX_CUSTOM_APP_URI = 19;
    public static final int EVENT_INDEX_DTEND = 20;
    public static final int EVENT_INDEX_DURATION = 21;
    public static final int EVENT_INDEX_ORIGINAL_ID = 22;
    public static final int EVENT_INDEX_ORIGINAL_SYNC_ID = 23;
    public static final int EVENT_INDEX_AVAILABILITY = 24;
    public static final int EVENT_INDEX_OWNER_ACCOUNT = 25;
    public static final int EVENT_INDEX_GUESTS_CAN_MODIFY = 26;
    public static final int EVENT_INDEX_EVENT_STATUS = 27;
    //public static final int EVENT_INDEX_ = ;

    public static final String[] ATTENDEES_PROJECTION = new String[] {
        Attendees._ID,                      // 0
        Attendees.ATTENDEE_NAME,            // 1
        Attendees.ATTENDEE_EMAIL,           // 2
        Attendees.ATTENDEE_RELATIONSHIP,    // 3
        Attendees.ATTENDEE_STATUS,          // 4
        Attendees.ATTENDEE_IDENTITY,        // 5
        Attendees.ATTENDEE_ID_NAMESPACE     // 6
    };
    public static final int ATTENDEES_INDEX_ID = 0;
    public static final int ATTENDEES_INDEX_NAME = 1;
    public static final int ATTENDEES_INDEX_EMAIL = 2;
    public static final int ATTENDEES_INDEX_RELATIONSHIP = 3;
    public static final int ATTENDEES_INDEX_STATUS = 4;
    public static final int ATTENDEES_INDEX_IDENTITY = 5;
    public static final int ATTENDEES_INDEX_ID_NAMESPACE = 6;

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[EVENT_INDEX_CUSTOM_APP_PACKAGE] = Events._ID; // dummy value
            EVENT_PROJECTION[EVENT_INDEX_CUSTOM_APP_URI] = Events._ID; // dummy value

            ATTENDEES_PROJECTION[ATTENDEES_INDEX_IDENTITY] = Attendees._ID; // dummy value
            ATTENDEES_PROJECTION[ATTENDEES_INDEX_ID_NAMESPACE] = Attendees._ID; // dummy value
        }
    }

    public static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=?";

    public static final String ATTENDEES_SORT_ORDER = Attendees.ATTENDEE_NAME + " ASC, "
            + Attendees.ATTENDEE_EMAIL + " ASC";

    public static final String[] REMINDERS_PROJECTION = new String[] {
        Reminders._ID,                      // 0
        Reminders.MINUTES,            // 1
        Reminders.METHOD           // 2
    };
    public static final int REMINDERS_INDEX_ID = 0;
    public static final int REMINDERS_MINUTES_ID = 1;
    public static final int REMINDERS_METHOD_ID = 2;

    public static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=?";

    public static final String[] CALENDARS_PROJECTION = new String[] {
        /*Calendars._ID,           // 0
        Calendars.CALENDAR_DISPLAY_NAME,  // 1
        Calendars.OWNER_ACCOUNT, // 2
        Calendars.CAN_ORGANIZER_RESPOND, // 3
        Calendars.ACCOUNT_NAME, // 4
        Calendars.ACCOUNT_TYPE  // 5*/        
        
        Calendars._ID, // 0 *
        Calendars.CALENDAR_DISPLAY_NAME, // 1 * 
        Calendars.OWNER_ACCOUNT, // 2  *        
        Calendars.CAN_ORGANIZER_RESPOND, // 3
        Calendars.ACCOUNT_NAME, // 4
        Calendars.ACCOUNT_TYPE, // 5
        Calendars.CALENDAR_COLOR, // 6 
        Calendars.CALENDAR_ACCESS_LEVEL, // 7
        Calendars.VISIBLE, // 8
        Calendars.MAX_REMINDERS, // 9
        Calendars.ALLOWED_REMINDERS, // 10
        Calendars.ALLOWED_ATTENDEE_TYPES, // 11
        Calendars.ALLOWED_AVAILABILITY // 12        
    };
    
    public static final int CALENDARS_INDEX_ID = 0;
    public static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    public static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    public static final int CALENDARS_INDEX_OWNER_CAN_RESPOND = 3;
    public static final int CALENDARS_INDEX_ACCOUNT_NAME = 4;
    public static final int CALENDARS_INDEX_ACCOUNT_TYPE = 5;
    public static final int CALENDARS_INDEX_CALENDAR_COLOR = 6;
    public static final int CALENDARS_INDEX_CALENDAR_ACCESS_LEVEL = 7;
    public static final int CALENDARS_INDEX_VISIBLE = 8;
    public static final int CALENDARS_INDEX_MAX_REMINDERS = 9;
    public static final int CALENDARS_INDEX_ALLOWED_REMINDERS = 10;
    public static final int CALENDARS_INDEX_ALLOWED_ATTENDEE_TYPES = 11;
    public static final int CALENDARS_INDEX_ALLOWED_AVAILABILITY = 12;
    //public static final int CALENDARS_INDEX_ = ;

    static final String CALENDARS_WHERE = Calendars._ID + "=?";
    static final String CALENDARS_DUPLICATE_NAME_WHERE = Calendars.CALENDAR_DISPLAY_NAME + "=?";
    static final String CALENDARS_VISIBLE_WHERE = Calendars.VISIBLE + "=?";

    static final String[] COLORS_PROJECTION = new String[] {
        Colors._ID, // 0
        Colors.COLOR, // 1
        Colors.COLOR_KEY, // 2
        Colors.ACCOUNT_NAME,
        Colors.ACCOUNT_TYPE
    };
    
    public static final int COLORS_INDEX_COLOR = 1;
    public static final int COLORS_INDEX_COLOR_KEY = 2;
    public static final int COLORS_INDEX_ACCOUNT_NAME = 3;
    public static final int COLORS_INDEX_ACCOUNT_TYPE = 4;

    static final String COLORS_WHERE = Colors.ACCOUNT_NAME + "=? AND " + Colors.ACCOUNT_TYPE +
        "=? AND " + Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT;    
    
	private Context mContext;
    private Handler mHandler = new Handler();
    private AtomicInteger mSequenceNumber = new AtomicInteger();

    private LinkedBlockingQueue<LoadRequest> mLoaderQueue;
    private LoaderThread mLoaderThread;
    private ContentResolver mResolver;

    private static interface LoadRequest {
        public void processRequest(EventInfoLoader eventLoader);
        public void skipRequest(EventInfoLoader eventLoader);
    }

    private static class ShutdownRequest implements LoadRequest {
        public void processRequest(EventInfoLoader eventLoader) {
        }

        public void skipRequest(EventInfoLoader eventLoader) {
        }
    }   
    
    private static class LoadEventsInfoRequest implements LoadRequest {

        public int id;
        public ArrayList<Event> events;
        public ArrayList<EventCursors> eventCursors;
        public Runnable successCallback;
        public Runnable cancelCallback;

        public LoadEventsInfoRequest(int id, ArrayList<Event> events, ArrayList<EventCursors> eventCursors,
                final Runnable successCallback, final Runnable cancelCallback) {
        	
            this.id = id;            
            this.events = events;
            this.eventCursors = eventCursors;
            this.successCallback = successCallback;
            this.cancelCallback = cancelCallback;
        }        
        
        public void processRequest(EventInfoLoader eventsInfoLoader) {
            
        	int size = this.events.size();
        	for (int i=0; i<size; i++) {
        		Event obj = this.events.get(i);
        		processSingleEventInfoQuery(eventsInfoLoader.mResolver, obj);
        	}
        	
            // Check if we are still the most recent request.
            if (id == eventsInfoLoader.mSequenceNumber.get()) {
            	if (this.successCallback != null)
            		eventsInfoLoader.mHandler.post(successCallback);
            } else {
            	if (this.cancelCallback != null)
            		eventsInfoLoader.mHandler.post(cancelCallback);
            }
        }

        public void skipRequest(EventInfoLoader eventsInfoLoader) {
        	eventsInfoLoader.mHandler.post(cancelCallback);
        }	
                
        @SuppressLint("SuspiciousIndentation")
        private void processSingleEventInfoQuery(ContentResolver cr, Event event) {
        	EventCursors cursors = new EventCursors(event.id);
        	
        	Cursor eventCursor = getEvent(cr, event, cursors);
        	if (eventCursor == null)
        		return ; // 예외처리를 해야 하는 것이 아닌가?
        	else 
        		eventCursor.moveToFirst();
        	cursors.setEventCursor(eventCursor);  
        	
        	
        	long calendarId = eventCursor.getLong(EVENT_INDEX_CALENDAR_ID);
        	Cursor calendarCursor = getCalendar(cr, cursors, calendarId);
        	if (calendarCursor == null)
            	return; // 예외처리를 해야 하는 것이 아닌가?
        	else 
        		calendarCursor.moveToFirst();
            cursors.setCalendarsCursor(calendarCursor);   
            
            searchVisibleCalendar(cr, cursors, calendarCursor);
            
            Cursor attendeesCursor = null;
            
        	boolean isBusyFreeCalendar =
        			eventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL) == Calendars.CAL_ACCESS_FREEBUSY;
        	if (!isBusyFreeCalendar) {
        		attendeesCursor = getAttendee(cr, cursors, event.id);
        		cursors.setAttendeesCursor(attendeesCursor);
        	}
        	
        	Cursor remindersCursor = null;
        	// mHasAlarm will be true if it was saved in the event already, or if
            // we've explicitly been provided reminders (e.g. during rotation).
            // mReminders는 EventInfoActivity가 launch 되었을 때는 null 값이다 
        	if ((eventCursor.getInt(EVENT_INDEX_HAS_ALARM) == 1)) {
        		remindersCursor = getReminders(cr, cursors, event.id);
        		cursors.setRemindersCursor(remindersCursor);
        	}
        	
        	Cursor colorCursor = getColors(cr, cursors);
        	if (colorCursor != null)
        		cursors.setColorsCursor(colorCursor);
        	
			//////////////////////////
			eventCursors.add(cursors);
			//////////////////////////
			
			eventCursor.close();
			calendarCursor.close();
			
			if (attendeesCursor != null)
				attendeesCursor.close();            
			if (remindersCursor != null)
				remindersCursor.close();
			if (colorCursor != null)
				colorCursor.close();
        }
        
        private Cursor getEvent(ContentResolver cr, Event event, EventCursors cursors) {
        	Uri eventQueryUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);        	        	
        	Cursor eventCursor = processSingleEventItemQuery(cr, eventQueryUri, EVENT_PROJECTION, null, null, null);        	
        	return eventCursor;        	   
        }
        
        private Cursor getCalendar(ContentResolver cr, EventCursors cursors, long calendarId) {
        	
        	Uri calendarQueryUri = Calendars.CONTENT_URI;
            String[] args = new String[] {
                    Long.toString(calendarId)};
            Cursor calendarCursor = processSingleEventItemQuery(cr, calendarQueryUri, CALENDARS_PROJECTION, CALENDARS_WHERE, args, null);
            return calendarCursor;            
        }
        
        private void searchVisibleCalendar(ContentResolver cr, EventCursors cursors, Cursor calendarCursor) {
        	Cursor visibleCalendarCursor = processSingleEventItemQuery(cr, Calendars.CONTENT_URI, CALENDARS_PROJECTION, CALENDARS_VISIBLE_WHERE, new String[] {"1"}, null);
            if (visibleCalendarCursor != null) {
	            if (visibleCalendarCursor.getCount() > 1)
	            {
	            	//calendarCursor.moveToFirst();
	            	String displayName = calendarCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);                
	                Cursor cursor = processSingleEventItemQuery(cr, Calendars.CONTENT_URI, CALENDARS_PROJECTION, CALENDARS_DUPLICATE_NAME_WHERE, new String[] {displayName}, null);
	                SpannableStringBuilder sb = new SpannableStringBuilder();
	
	                // Calendar display name
	                String calendarName = calendarCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
	                sb.append(calendarName);
	
	                // Show email account if display name is not unique and
	                // display name != email
	                String email = calendarCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
	                if (cursor.getCount() > 1 && !calendarName.equalsIgnoreCase(email) &&
	                        Utils.isValidEmail(email)) {
	                    sb.append(" (").append(email).append(")");
	                }
	                
	                //mView.setVisibilityCommon(R.id.calendar_container, View.VISIBLE);
	                //mView.setTextCommon(R.id.calendar_name, sb);
	                sb.toString();
	                cursors.setCalendarName(sb);
	                cursor.close();                
	            }
	            
	            visibleCalendarCursor.close();
            }    
        }
        
        private Cursor getAttendee(ContentResolver cr, EventCursors cursors, long eventId) {
        	Cursor attendeesCursor = null;
                        
            String[] args = new String[] { Long.toString(eventId) };

            // start attendees query
            Uri attendeesQueryUri = Attendees.CONTENT_URI;                
            attendeesCursor = processSingleEventItemQuery(cr, attendeesQueryUri, ATTENDEES_PROJECTION, ATTENDEES_WHERE, args, ATTENDEES_SORT_ORDER);
            
            return attendeesCursor;                    
        }
        
        private Cursor getReminders(ContentResolver cr, EventCursors cursors, long eventId) {                        
            
        	cursors.setHasAlarm(true);         
            // start reminders query
            String[] args = new String[] { Long.toString(eventId) };
            Uri remindersQueryUri = Reminders.CONTENT_URI;                
            Cursor remindersCursor = processSingleEventItemQuery(cr, remindersQueryUri, REMINDERS_PROJECTION, REMINDERS_WHERE, args, null);
            
            return remindersCursor;            
        }
        
        private Cursor getColors(ContentResolver cr, EventCursors cursors) {        	
        	
        	 Uri colorQueryUri = Colors.CONTENT_URI;                
             Cursor colorsCursor = processSingleEventItemQuery(cr, colorQueryUri, COLORS_PROJECTION, COLORS_WHERE, null, null);
             
             return colorsCursor;
        }       
        
    }
	
    private static class LoaderThread extends Thread {
        LinkedBlockingQueue<LoadRequest> mQueue;
        EventInfoLoader mEventLoader;

        public LoaderThread(LinkedBlockingQueue<LoadRequest> queue, EventInfoLoader eventLoader) {
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

                        // take 메소드 실행시 큐가 비었을 경우에는 값이 들어올때까지 대기한다.
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
    
	public EventInfoLoader(Context context) {
		mContext = context;
        mLoaderQueue = new LinkedBlockingQueue<LoadRequest>();
        mResolver = context.getContentResolver();
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
	
    public void loadEventsInfoInBackground(final ArrayList<EventCursors> eventCursors,
    		ArrayList<Event> events, final Runnable successCallback, final Runnable cancelCallback) {

        // Increment the sequence number for requests.  We don't care if the
        // sequence numbers wrap around because we test for equality with the
        // latest one.
        int id = mSequenceNumber.incrementAndGet();
        
        // Send the load request to the background thread
        LoadEventsInfoRequest request = new LoadEventsInfoRequest(id, events, eventCursors, successCallback, cancelCallback);

        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventsInfoInBackground() interrupted!");
        }
    }
    
    
    public void loadEvents(final ArrayList<EventCursors> eventCursors, ArrayList<Event> events) {

    	int size = events.size();
    	for (int i=0; i<size; i++) {
    		Event event = events.get(i);
    		
    		EventCursors cursors = new EventCursors(event.id);        	
        	
        	// search Event Table 
        	Uri eventQueryUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);        	        	
        	Cursor eventCursor = processSingleEventItemQuery(mResolver, eventQueryUri, EVENT_PROJECTION, null, null, null);
        	if (eventCursor == null)
        		return; // 예외처리를 해야 하는 것이 아닌가?
        	else 
        		eventCursor.moveToFirst();
        	cursors.setEventCursor(eventCursor);        	
        	
        	// search Calendar Table        	
        	long calendarId = eventCursor.getLong(EVENT_INDEX_CALENDAR_ID);
        	Uri calendarQueryUri = Calendars.CONTENT_URI;
            String[] args = new String[] {
                    Long.toString(calendarId)};
            Cursor calendarCursor = processSingleEventItemQuery(mResolver, calendarQueryUri, CALENDARS_PROJECTION, CALENDARS_WHERE, args, null);
            if (calendarCursor == null)
            	return; // 예외처리를 해야 하는 것이 아닌가?
            cursors.setCalendarsCursor(calendarCursor);   
            
            // search Visible Calendar Table
            Cursor visibleCalendarCursor = processSingleEventItemQuery(mResolver, Calendars.CONTENT_URI, CALENDARS_PROJECTION, CALENDARS_VISIBLE_WHERE, new String[] {"1"}, null);
            if (visibleCalendarCursor != null) {
	            if (visibleCalendarCursor.getCount() > 1)
	            {
	            	calendarCursor.moveToFirst();
	            	String displayName = calendarCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);                
	                Cursor cursor = processSingleEventItemQuery(mResolver, Calendars.CONTENT_URI, CALENDARS_PROJECTION, CALENDARS_DUPLICATE_NAME_WHERE, new String[] {displayName}, null);
	                SpannableStringBuilder sb = new SpannableStringBuilder();
	
	                // Calendar display name
	                String calendarName = calendarCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
	                sb.append(calendarName);
	
	                // Show email account if display name is not unique and
	                // display name != email
	                String email = calendarCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
	                if (cursor.getCount() > 1 && !calendarName.equalsIgnoreCase(email) &&
	                        Utils.isValidEmail(email)) {
	                    sb.append(" (").append(email).append(")");
	                }
	                
	                //mView.setVisibilityCommon(R.id.calendar_container, View.VISIBLE);
	                //mView.setTextCommon(R.id.calendar_name, sb);
	                cursors.setCalendarName(sb);
	                cursor.close();
	            }
            }
            
            Cursor attendeesCursor = null;
            boolean isBusyFreeCalendar =
            		eventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL) == Calendars.CAL_ACCESS_FREEBUSY;
            if (!isBusyFreeCalendar) {
                args = new String[] { Long.toString(event.id) };

                // start attendees query
                Uri attendeesQueryUri = Attendees.CONTENT_URI;                
                attendeesCursor = processSingleEventItemQuery(mResolver, attendeesQueryUri, ATTENDEES_PROJECTION, ATTENDEES_WHERE, args, ATTENDEES_SORT_ORDER);
                cursors.setAttendeesCursor(attendeesCursor);
            }
            
            Cursor remindersCursor = null;
            // mHasAlarm will be true if it was saved in the event already, or if
            // we've explicitly been provided reminders (e.g. during rotation).
            // mReminders는 EventInfoActivity가 launch 되었을 때는 null 값이다              
            if ((eventCursor.getInt(EVENT_INDEX_HAS_ALARM) == 1)) {
            	cursors.setHasAlarm(true);         
                // start reminders query
                args = new String[] { Long.toString(event.id) };
                Uri remindersQueryUri = Reminders.CONTENT_URI;                
                remindersCursor = processSingleEventItemQuery(mResolver, remindersQueryUri, REMINDERS_PROJECTION, REMINDERS_WHERE, args, null);
                cursors.setRemindersCursor(remindersCursor);
            }        
            
            
            Uri colorQueryUri = Colors.CONTENT_URI;                
            Cursor colorsCursor = processSingleEventItemQuery(mResolver, colorQueryUri, COLORS_PROJECTION, COLORS_WHERE, null, null);
            if (colorsCursor != null)
        		cursors.setColorsCursor(colorsCursor);
            
            //////////////////////////
            eventCursors.add(cursors);
            //////////////////////////
            
            eventCursor.close();
            calendarCursor.close();
            visibleCalendarCursor.close(); // 여기서 널포인터 예외가 발생함!!!
            if (attendeesCursor != null)
            	attendeesCursor.close();            
            if (remindersCursor != null)
            	remindersCursor.close();
            if (colorsCursor != null)
            	colorsCursor.close();
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
			Log.i(TAG, e.toString());				
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
		//SpannableStringBuilder mCalendarNameSB = null;
		public String mCalendarName = null;
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
			//mCalendarNameSB = sb;
			mCalendarName = new String(sb.toString());
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
	
	
	
	
	
	
	
	
	
	
	
		
	
	
	
	
	
	
	
	
	
	
	
	
}
