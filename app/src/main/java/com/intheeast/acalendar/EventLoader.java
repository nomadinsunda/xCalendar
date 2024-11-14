/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Process;
import android.provider.CalendarContract;
import android.provider.CalendarContract.EventDays;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class EventLoader {

	private static String TAG = "EventLoader";
    private static boolean INFO = true;
    
    private Context mContext;
    private Handler mHandler = new Handler();
    private AtomicInteger mSequenceNumber = new AtomicInteger();

    //private LinkedBlockingQueue<LoadRequest> mLoaderQueue;
    
    private LinkedBlockingDeque<LoadRequest> mLoaderQueue;
    private LoaderThread mLoaderThread;
    private ContentResolver mResolver;

    private static interface LoadRequest {
        public void processRequest(EventLoader eventLoader);
        public void skipRequest(EventLoader eventLoader);
    }

    private static class ShutdownRequest implements LoadRequest {
        public void processRequest(EventLoader eventLoader) {
        }

        public void skipRequest(EventLoader eventLoader) {
        }
    }
    
    private static class ImmediatelyCancelAllRequests implements LoadRequest {
        public void processRequest(EventLoader eventLoader) {
        }

        public void skipRequest(EventLoader eventLoader) {
        }
    }

    /**
     *
     * Code for handling requests to get whether days have an event or not
     * and filling in the eventDays array.
     *
     */
    private static class LoadEventDaysRequest implements LoadRequest {
        public int startDay;
        public int numDays;
        public boolean[] eventDays;
        public Runnable uiCallback;

        /**
         * The projection used by the EventDays query.
         */
        private static final String[] PROJECTION = {
                CalendarContract.EventDays.STARTDAY, CalendarContract.EventDays.ENDDAY
        };

        public LoadEventDaysRequest(int startDay, int numDays, boolean[] eventDays,
                final Runnable uiCallback)
        {
            this.startDay = startDay;
            this.numDays = numDays;
            this.eventDays = eventDays;
            this.uiCallback = uiCallback;
        }

        @Override
        public void processRequest(EventLoader eventLoader)
        {
            final Handler handler = eventLoader.mHandler;
            ContentResolver cr = eventLoader.mResolver;

            // Clear the event days
            Arrays.fill(eventDays, false);

            //query which days have events
            Cursor cursor = EventDays.query(cr, startDay, numDays, PROJECTION);
            try {
                int startDayColumnIndex = cursor.getColumnIndexOrThrow(EventDays.STARTDAY);
                int endDayColumnIndex = cursor.getColumnIndexOrThrow(EventDays.ENDDAY);

                //Set all the days with events to true
                while (cursor.moveToNext()) {
                    int firstDay = cursor.getInt(startDayColumnIndex);
                    int lastDay = cursor.getInt(endDayColumnIndex);
                    //we want the entire range the event occurs, but only within the month
                    int firstIndex = Math.max(firstDay - startDay, 0);
                    int lastIndex = Math.min(lastDay - startDay, 30);

                    for(int i = firstIndex; i <= lastIndex; i++) {
                        eventDays[i] = true;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            handler.post(uiCallback);
        }

        @Override
        public void skipRequest(EventLoader eventLoader) {
        }
    }

    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class LoadEventsRequest implements LoadRequest {

        public int id;
        public int startDay;
        public int numDays;
        public ArrayList<Event> events;
        public Runnable successCallback;
        public Runnable cancelCallback;

        public LoadEventsRequest(int id, int startDay, int numDays, ArrayList<Event> events,
                final Runnable successCallback, final Runnable cancelCallback) {
            this.id = id;
            this.startDay = startDay;
            this.numDays = numDays;
            this.events = events;
            this.successCallback = successCallback;
            this.cancelCallback = cancelCallback;
        }

        @SuppressLint("SuspiciousIndentation")
        public void processRequest(EventLoader eventLoader) {
        	if (INFO) Log.i(TAG, "processRequest:id=" + String.valueOf(this.id));
        	
            Event.loadEvents(eventLoader.mContext, events, startDay,
                    numDays, id, eventLoader.mSequenceNumber);
            
            eventLoader.mHandler.post(successCallback);

            // Check if we are still the most recent request.
            /*
            if (id == eventLoader.mSequenceNumber.get()) {
            	if (this.successCallback != null)
            		eventLoader.mHandler.post(successCallback);
            } else {
            	if (INFO) Log.i(TAG, "id != eventLoader.mSequenceNumber.get()");
            	if (this.cancelCallback != null)
            		eventLoader.mHandler.post(cancelCallback);
            }
            */
        }

        public void skipRequest(EventLoader eventLoader) {
        	if (INFO) Log.i(TAG, "skipRequest:id=" + String.valueOf(this.id));
        	
            eventLoader.mHandler.post(cancelCallback);
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    
	
    
    
    private static class LoaderThread extends Thread {
        //LinkedBlockingQueue<LoadRequest> mQueue;
    	// Deque : Double Ended Queue
        LinkedBlockingDeque<LoadRequest> mQueue;
        
        EventLoader mEventLoader;
        Runnable mImmediatelyCancelAllRequestsCompeletionCallback;

        public LoaderThread(LinkedBlockingDeque<LoadRequest> queue, EventLoader eventLoader) {
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
        
        public void putImmediatelyCancelAllRequests(Runnable callback) {
        	mImmediatelyCancelAllRequestsCompeletionCallback = callback;
        	
        	 try {
        		 mQueue.addFirst(new ImmediatelyCancelAllRequests());
             } catch (IllegalStateException e) {
            	 // Creates a LinkedBlockingDeque with a capacity of Integer.MAX_VALUE.
            	 // :mQueue를 위와 같은 capacity로 생성해서 이런 경우가 발생할 경우는 극히 미흡하나...
            	 //  그래도 try/catch 코드 적용함
                 e.printStackTrace();
             } 
        	 catch (NullPointerException e) {
                 e.printStackTrace();
             }
        	 
        	/*
        	if (mQueue.isEmpty())
        		return;
        	
        	// queue의 head에 stop request를 강제로 삽입하는 방법은 없는가?
        	// 그래서 run에서 queue가 empty가 될 때까지 
        	// load request를 skip하는 건 안되나????
        	//mQueue.add(new StopRequest());
        	
        	
        	
        	while (!mQueue.isEmpty()) {
        		// Retrieves and removes the head of this queue. This method differs from poll only in that it throws an exception if this queue is empty. 
        		//mQueue.remove();
        		//Retrieves and removes the head of this queue, or returns null if this queue is empty.
        		mQueue.poll();
        	}
        	*/
        	
        	/*
        	synchronized(mSynced) {
	        	try {                
	        		
	                while (!mQueue.isEmpty()) {
	                
	                	LoadRequest request = mQueue.take();
	                    // Let the request know that it was skipped
	                    request.skipRequest(mEventLoader);                    
	                }        
	               
	                
	            } catch (InterruptedException ex) {
	                Log.e("Cal", "immediatelyStopLoad LoaderThread interrupted!");
	            }
        	}
        	*/
        }

        @Override
        public void run() {
        	
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            
            while (true) {
            	
                try {
                    // Wait for the next request
                    LoadRequest request = mQueue.take();
                    
                    if (request instanceof ImmediatelyCancelAllRequests) {
                    	
                    	if (INFO) Log.i(TAG, "accept ImmediatelyCancelAllRequests");
                    	
                    	while (!mQueue.isEmpty()) {
                    		// Retrieves and removes the head of this queue. This method differs from poll only in that it throws an exception if this queue is empty. 
                    		//mQueue.remove();
                    		//Retrieves and removes the head of this queue, or returns null if this queue is empty.
                    		LoadRequest headRequest = mQueue.poll();
                    		if (headRequest != null)
                    			headRequest.skipRequest(mEventLoader); // 확인해 보자...
                    	}
                    	
                    	if (INFO) Log.i(TAG, "remove all load request");
                    	
                    	mImmediatelyCancelAllRequestsCompeletionCallback.run();
                    	
                        continue;
                    }
                    else if (request instanceof ShutdownRequest) {
                        return;
                    }                   
                    
                    request.processRequest(mEventLoader);
                    
                } catch (InterruptedException ex) {
                    Log.e("Cal", "background LoaderThread interrupted!");
                }
            	
            }
        }
    }

    public EventLoader(Context context) {
        mContext = context;
        mLoaderQueue = new LinkedBlockingDeque<LoadRequest>();
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
    
    public void putImmediatelyCancelAllLoadRequests(Runnable callback) {
        mLoaderThread.putImmediatelyCancelAllRequests(callback);
    }

    /**
     * Loads "numDays" days worth of events, starting at start, into events.
     * Posts uiCallback to the {@link Handler} for this view, which will run in the UI thread.
     * Reuses an existing background thread, if events were already being loaded in the background.
     * NOTE: events and uiCallback are not used if an existing background thread gets reused --
     * the ones that were passed in on the call that results in the background thread getting
     * created are used, and the most recent call's worth of data is loaded into events and posted
     * via the uiCallback.
     */
    public void loadEventsInBackground(final int numDays, final ArrayList<Event> events,
            int startDay, final Runnable successCallback, final Runnable cancelCallback) {

        // Increment the sequence number for requests.  We don't care if the
        // sequence numbers wrap around because we test for equality with the
        // latest one.
    	//int accumulatedId = mSequenceNumber.get();
    	//if (INFO) Log.i(TAG, "loadEventsInBackground:id=" + String.valueOf(accumulatedId));
    	
        int id = mSequenceNumber.incrementAndGet();
        if (INFO) Log.i(TAG, "loadEventsInBackground:id=" + String.valueOf(id));
        
        // Send the load request to the background thread
        LoadEventsRequest request = new LoadEventsRequest(id, startDay, numDays,
                events, successCallback, cancelCallback);

        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventsInBackground() interrupted!");
        }
    }
    
    
    
    public void loadEvents(Context context, final int numDays, final ArrayList<Event> events,
            int startDay) {

    	Event.loadEvents(context, events, startDay, numDays);
    }
    
    


    // 현재 사용되는 곳은 없다!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    void loadEventDaysInBackground(int startDay, int numDays, boolean[] eventDays,
        final Runnable uiCallback)
    {
        // Send load request to the background thread
        LoadEventDaysRequest request = new LoadEventDaysRequest(startDay, numDays,
                eventDays, uiCallback);
        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventDaysInBackground() interrupted!");
        }
    }
}
