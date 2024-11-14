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

package com.intheeast.acalendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Bundle;
//import android.text.format.Time;
import android.util.Log;

public class ECalendarApplication extends Application {
	private static final String TAG = ECalendarApplication.class.getSimpleName();
	private static final boolean INFO = true;
	
	ArrayList<Event> mEvents = new ArrayList<Event>();
	ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
	
	Event mForemostEvent = null;   
	Event mBackmostEvent = null;   
	
	int mFirstLoadedJulianDay;
	int mLastLoadedJulianDay;
	
	Bitmap mCalendarActionBarBitmap;	
	Bitmap mCalendarEntireRegionBitmap;
	
	int mEventDayListFirstJulianDay = -1;
	int mEventDayListLastJulianDay = -1;
	
	EventLoader mEventLoaderForPrvExtendDays;
	EventLoader mEventLoaderForNextExtendDays;
	
	ArrayList<Event> mPrvExtendDaysEvents = new ArrayList<Event>();
	//ArrayList<ArrayList<Event>> mPrvExtendDaysEventList = new ArrayList<ArrayList<Event>>();
	
	ArrayList<Event> mNextExtendDaysEvents = new ArrayList<Event>();
	//ArrayList<ArrayList<Event>> mNextExtendDaysEventList = new ArrayList<ArrayList<Event>>();
	
	CalendarEventModel mOriginalModel = null;
	
	public final int PRV_EXTENDDAYS_REMAINDER = 1;
    public final int NEXT_EXTENDDAYS_REMAINDER = 2;
    
    RemainderEventsEventsLoadSuccessCallback mPrvExtendDaysEventsLoadSuccessCallback;
    RemainderEventsEventsLoadSuccessCallback mNextExtendDaysEventsLoadSuccessCallback;
    
	final int mNumWeeks = 5;
    ArrayList<Event> mEventsOfSupported;
    
	boolean mRootLaunchedAnotherActivity = false;
	int mWhichActivityLaunchedByRoot = -1;
	
	public ECalendarApplication() {
		
	}
	
    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * Ensure the default values are set for any receiver, activity,
         * service, etc. of Calendar
         */
        SettingsFragment.setDefaultValues(this);

        // Save the version number, for upcoming 'What's new' screen.  This will be later be
        // moved to that implementation.
        Utils.setSharedPreference(this, SettingsFragment.KEY_VERSION,
                Utils.getVersionCode(this));

        // Initialize the registry mapping some custom behavior.
        ExtensionsFactory.init(getAssets());   
        
        mEventLoaderForPrvExtendDays = new EventLoader(getApplicationContext());
        mEventLoaderForNextExtendDays = new EventLoader(getApplicationContext());
    }
    
    
    public void setFirstLoadedJulianDay(int julianday) {
    	mFirstLoadedJulianDay = julianday;
    }
        
    
    public int getFirstLoadedJulianDay() {
    	return mFirstLoadedJulianDay;
    }    
    
    public void setLastLoadedJulianDay(int julianday) {
    	mLastLoadedJulianDay = julianday;
    }
        
    public int getLastLoadedJulianDay() {
    	return mLastLoadedJulianDay;
    }
    
    
    public void setEvents(ArrayList<Event> events) {
    	mEvents = events;
    }
    
    public ArrayList<Event> getEvents() {
    	return mEvents;
    }
    
    public void setEventDayList(ArrayList<ArrayList<Event>> events) {
    	mEventDayList = events;
    }
    
    public ArrayList<ArrayList<Event>> getEventDayList() {
    	return mEventDayList;
    }
    
    
    public void setEventDayListFirstJulianDay(int julianDay) {
    	mEventDayListFirstJulianDay = julianDay;
    }
    
    
    public void setEventDayListLastJulianDay(int julianDay) {
    	mEventDayListLastJulianDay = julianDay;
    }
    
    public int getEventDayListFirstJulianDay() {
    	return mEventDayListFirstJulianDay;
    }
        
    public int getEventDayListLastJulianDay() {
    	return mEventDayListLastJulianDay;
    }


	public void storeCalendarActionBarBitmap(Bitmap bm) {
    	mCalendarActionBarBitmap = bm;
    }
    
    public Bitmap getCalendarActionBarBitmap() {
    	return mCalendarActionBarBitmap;
    }
    
    public void storeCalendarEntireRegionBitmap(Bitmap bm) {
    	mCalendarEntireRegionBitmap = bm;
    }
    
    public Bitmap getCalendarEntireRegionBitmap() {
    	return mCalendarEntireRegionBitmap;
    }
    
    public synchronized void setBothEndsEvent(int type, Event event) {
    	if (type == BothEndsEventLoader.FOREMOST_EVENT_LOADER) {
    		if (INFO) Log.i(TAG, "setBothEndsEvent:FOREMOST_EVENT_LOADER");
	    	mForemostEvent = new Event();   
	    	
	    	mForemostEvent.id = event.id;            	
	    	mForemostEvent.startMillis = event.startMillis;
	    	mForemostEvent.endMillis = event.endMillis;
	    	mForemostEvent.startDay = event.startDay;
	    	mForemostEvent.endDay = event.endDay;                    
	                
	    	mForemostEvent.title = event.title;
	    	mForemostEvent.location = event.location;
	    	mForemostEvent.allDay = event.allDay;
    	} else if (type == BothEndsEventLoader.BACKMOST_EVENT_LOADER) {
    		if (INFO) Log.i(TAG, "setBothEndsEvent:BACKMOST_EVENT_LOADER");
    		mBackmostEvent = new Event();   
	    	
    		mBackmostEvent.id = event.id;            	
    		mBackmostEvent.startMillis = event.startMillis;
    		mBackmostEvent.endMillis = event.endMillis;
    		mBackmostEvent.startDay = event.startDay;
    		mBackmostEvent.endDay = event.endDay;                    
	                
    		mBackmostEvent.title = event.title;
    		mBackmostEvent.location = event.location;
    		mBackmostEvent.allDay = event.allDay;
    	}
    	else {
    		// what???
    		if (INFO) Log.i(TAG, "setBothEndsEvent:??? Oooops!!!");
    	}
    }
    
    public synchronized Event getBothEndsEvent(int type) {
    	if (type == BothEndsEventLoader.FOREMOST_EVENT_LOADER) {
	    	return mForemostEvent;
    	} else if (type == BothEndsEventLoader.BACKMOST_EVENT_LOADER) {
    		return mBackmostEvent;
    	}
    	else {
    		if (INFO) Log.i(TAG, "getBothEndsEvent:??? Oooops!!!");
    		return null;
    	}
    }   
    
    
    
    public void loadRemainderEvents(String tzId, int targetMonthJulianDay, int firstDayOfWeek, int firstJulianDayOfSupportedEvents, int lastJulianDayOfSupportedEvents,
    		ArrayList<Event> supportedEvents) {
    	
    	TimeZone timezone = TimeZone.getTimeZone(tzId);
    	Calendar targetMonthTime = GregorianCalendar.getInstance(timezone);//new Time(timeZone);    	
    	long targetMonthTimeMillis = ETime.getMillisFromJulianDay(targetMonthJulianDay, timezone, firstDayOfWeek);
    	targetMonthTime.setTimeInMillis(targetMonthTimeMillis);//targetMonthTime.setJulianDay(targetMonthJulianDay);
    	
    	Calendar firstJulianDayOfSupportedEventsTime = GregorianCalendar.getInstance(timezone);//new Time(timeZone);    
    	long firstJulianDayOfSupportedEventsTimeMillis = ETime.getMillisFromJulianDay(firstJulianDayOfSupportedEvents, timezone, firstDayOfWeek);
    	firstJulianDayOfSupportedEventsTime.setTimeInMillis(firstJulianDayOfSupportedEventsTimeMillis);//firstJulianDayOfSupportedEventsTime.setJulianDay(firstJulianDayOfSupportedEvents);
    	
    	Calendar lastJulianDayOfSupportedEventsTime = GregorianCalendar.getInstance(timezone);//new Time(timeZone);    
    	long lastJulianDayOfSupportedEventsTimeMills = ETime.getMillisFromJulianDay(lastJulianDayOfSupportedEvents, timezone, firstDayOfWeek); 
    	lastJulianDayOfSupportedEventsTime.setTimeInMillis(lastJulianDayOfSupportedEventsTimeMills);//lastJulianDayOfSupportedEventsTime.setJulianDay(lastJulianDayOfSupportedEvents);
    	
    	mEventsOfSupported = supportedEvents;
    					
    	mRemainderLoadOred = 0;   	
    	
		int prvExtendDays = mNumWeeks * 7;                 
		mFirstLoadedJulianDay = targetMonthJulianDay - prvExtendDays;   
        int visibleDays = mNumWeeks * 7;
        int nextExtendDays = mNumWeeks * 7;
        mLastLoadedJulianDay = mFirstLoadedJulianDay + prvExtendDays + visibleDays + nextExtendDays;  
        
        int prvExtendNumDays = 0;
        int firstLoadedJulianDayOfPrvReminder = 0;
        if (firstJulianDayOfSupportedEvents > mFirstLoadedJulianDay) {
        	mRemainderLoadOred = PRV_EXTENDDAYS_REMAINDER;
        	
        	mEventLoaderForPrvExtendDays.startBackgroundThread();
            mPrvExtendDaysEvents.clear();            
            prvExtendNumDays = firstJulianDayOfSupportedEvents - mFirstLoadedJulianDay;
            firstLoadedJulianDayOfPrvReminder = mFirstLoadedJulianDay;
            mPrvExtendDaysEventsLoadSuccessCallback = new RemainderEventsEventsLoadSuccessCallback(PRV_EXTENDDAYS_REMAINDER, prvExtendNumDays, firstLoadedJulianDayOfPrvReminder, 
            		mPrvExtendDaysEvents, mEventLoaderForPrvExtendDays);           
        }
               
        int nextExtendNumDays = 0;
        int firstLoadedJulianDayOfNextReminder = 0;
        if (mLastLoadedJulianDay > lastJulianDayOfSupportedEvents) {
        	mRemainderLoadOred = mRemainderLoadOred | NEXT_EXTENDDAYS_REMAINDER;
        	
        	mEventLoaderForNextExtendDays.startBackgroundThread();
            mNextExtendDaysEvents.clear();            
            nextExtendNumDays = mLastLoadedJulianDay - lastJulianDayOfSupportedEvents;
            firstLoadedJulianDayOfNextReminder = lastJulianDayOfSupportedEvents + 1;
            mNextExtendDaysEventsLoadSuccessCallback = new RemainderEventsEventsLoadSuccessCallback(NEXT_EXTENDDAYS_REMAINDER, nextExtendNumDays, firstLoadedJulianDayOfNextReminder, 
            		mNextExtendDaysEvents, mEventLoaderForNextExtendDays);            
            
        }
        
        mRemainderLoadStatus = 0;
        
        if ((mRemainderLoadOred & PRV_EXTENDDAYS_REMAINDER) != 0) {
        	 mEventLoaderForPrvExtendDays.loadEventsInBackground(prvExtendNumDays, mPrvExtendDaysEvents, firstLoadedJulianDayOfPrvReminder, 
             		mPrvExtendDaysEventsLoadSuccessCallback, null); 
        }
        
        if ((mRemainderLoadOred & NEXT_EXTENDDAYS_REMAINDER) != 0) {
        	mEventLoaderForNextExtendDays.loadEventsInBackground(nextExtendNumDays, mNextExtendDaysEvents, firstLoadedJulianDayOfNextReminder, 
            		mNextExtendDaysEventsLoadSuccessCallback, null); 
        }     
        
        
    }
    
    int mRemainderLoadOred = 0;
    int mRemainderLoadStatus = 0;
    //ArrayList<Event> mNewEventsSupported = new ArrayList<Event>();
    public synchronized void setRemainderLoadStatus(int which) {
    	mRemainderLoadStatus = mRemainderLoadStatus | which;
    	if (mRemainderLoadOred == mRemainderLoadStatus) {
    		
    		mEvents.clear();//mNewEventsSupported.clear();
    		
    		if ((mRemainderLoadOred & PRV_EXTENDDAYS_REMAINDER) != 0) {
    			mEvents.addAll(mPrvExtendDaysEvents);//mNewEventsSupported.addAll(mPrvExtendDaysEvents);
    			
    		}
    		
    		mEvents.addAll(mEventsOfSupported);//mNewEventsSupported.addAll(mEventsOfSupported);
    		
    		if ((mRemainderLoadOred & NEXT_EXTENDDAYS_REMAINDER) != 0) {
    			mEvents.addAll(mNextExtendDaysEvents);//mNewEventsSupported.addAll(mNextExtendDaysEvents);
            } 
    		
    		if (INFO) Log.i(TAG, "setRemainderLoadStatus COMPLETION!!!!!!");
    	}
    }
    
    
    public class RemainderEventsEventsLoadSuccessCallback implements Runnable {
    	
    	int mWhichRemainder;
    	final int mNumDay;
    	final int mFirstLoadedJulianDay;
    	ArrayList<Event> mEvents;
    	//ArrayList<ArrayList<Event>> mEventDayList;
    	EventLoader mLoader;
    	
    	public RemainderEventsEventsLoadSuccessCallback(int whichRemainder, int numDay, int firstLoadedJulianDay, ArrayList<Event> events, 
    			/*ArrayList<ArrayList<Event>> eventDayList, */EventLoader loader) {
    		mWhichRemainder = whichRemainder;
    		mNumDay = numDay;
    		mFirstLoadedJulianDay = firstLoadedJulianDay;
    		mEvents = events;
    		//mEventDayList = eventDayList;
    		mLoader = loader;
    	}
    	
		@Override
		public void run() {
			
			/*
			ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
			
            for (int i = 0; i < mNumDay; i++) {
                eventDayList.add(new ArrayList<Event>());
            }

            if (mEvents == null || mEvents.size() == 0) {            	            	
                //mEventDayList = eventDayList;                  
                //return;
            }
            else {
            	            	
	            for (Event event : mEvents) {
	                int startDay = event.startDay - mFirstLoadedJulianDay;
	                int endDay = event.endDay - mFirstLoadedJulianDay + 1;
	                if (startDay < mNumDay || endDay >= 0) {
	                    if (startDay < 0) {
	                        startDay = 0;
	                    }
	                    if (startDay > mNumDay) {
	                        continue;
	                    }
	                    if (endDay < 0) {
	                        continue;
	                    }
	                    if (endDay > mNumDay) {
	                        endDay = 7;
	                    }
	                    for (int j = startDay; j < endDay; j++) {
	                        eventDayList.get(j).add(event);
	                    }
	                }
	            }
            }
            
            mEventDayList = eventDayList;	
            */            
            mLoader.stopBackgroundThread();
            
            setRemainderLoadStatus(mWhichRemainder);
		}
	}
    
    public void setOriginalModel(CalendarEventModel model) {
    	mOriginalModel = model;
    }
    
    public CalendarEventModel getOriginalModel() {
    	return mOriginalModel;
    }
    
	public void setRootLaunchedAnotherActivityFlag() {
		mRootLaunchedAnotherActivity = true;
	}
	
	public boolean getRootLaunchedAnotherActivityFlag() {
		return mRootLaunchedAnotherActivity;
	}
	
	public void reSetRootLaunchedAnotherActivityFlag() {
		mRootLaunchedAnotherActivity = false;
	}
	
	public void setWhichActivityLaunchedByRoot(int which) {
		mWhichActivityLaunchedByRoot = which;
	}
	
	public int getWhichActivityLaunchedByRoot() {
		return mWhichActivityLaunchedByRoot;
	}
	
	Bundle mState = null;
	public void makeBundle() {
		if (mState == null)
			mState = new Bundle();
		else {
			mState.clear();
			//mState = new Bundle();
		}
	}
	
	public void bundlePutSerializable(String key, Serializable value) {
		mState.putSerializable(key, value);
	}
	
	public void bundlePutInt(String key, int value) {
		mState.putInt(key, value);
	}
	
	public void bundlePutBoolean(String key, boolean value) {
		mState.putBoolean(key, value);
	}
	
	public Bundle getBundle() {
		return mState;
	}
    
}
