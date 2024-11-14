package com.intheeast.acalendar;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.util.Log;



public class BothEndsEventLoader {
	public static final int FOREMOST_EVENT_LOADER = 1;
	public static final int BACKMOST_EVENT_LOADER = 2;
	
	private Context mContext;
    private Handler mHandler = new Handler();
    private AtomicInteger mSequenceNumber = new AtomicInteger();

    final int mLoaderType;
    private LinkedBlockingQueue<BothEndsLoadRequest> mBothEndsEventLoaderQueue;
    private BothEndsLoaderThread mBothEndsLoaderThread;
    private ContentResolver mResolver;

    private static interface BothEndsLoadRequest {
        public void processRequest(BothEndsEventLoader eventLoader);
        public void skipRequest(BothEndsEventLoader eventLoader);
    }

    private static class ShutdownRequest implements BothEndsLoadRequest {
        public void processRequest(BothEndsEventLoader eventLoader) {
        }

        public void skipRequest(BothEndsEventLoader eventLoader) {
        }
    }
    
    
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static class LoadBothEndsEventsRequest implements BothEndsLoadRequest {
	
		public int id;
		public int startDay;
		public int endDay;
		
		public Event event;
		public Runnable successCallback;
		public Runnable cancelCallback;
		
		public LoadBothEndsEventsRequest(int id, int startDay, int endDay, Event event,
				final Runnable successCallback, final Runnable cancelCallback) {
			this.id = id;
			this.startDay = startDay;
			this.endDay = endDay;
			this.event = event;
			this.successCallback = successCallback;
			this.cancelCallback = cancelCallback;
		}
		
		public void processRequest(BothEndsEventLoader eventLoader) {
						
			Event.loadBothEndEvent(eventLoader.mContext, eventLoader.mLoaderType, event, startDay,
					endDay, id, eventLoader.mSequenceNumber);
		
			// Check if we are still the most recent request.
			if (id == eventLoader.mSequenceNumber.get()) {
				eventLoader.mHandler.post(successCallback);
			} else {
				eventLoader.mHandler.post(cancelCallback);
			}
		}
		
		public void skipRequest(BothEndsEventLoader eventLoader) {
			eventLoader.mHandler.post(cancelCallback);
		}
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    
    private static class BothEndsLoaderThread extends Thread {
        LinkedBlockingQueue<BothEndsLoadRequest> mQueue;
        BothEndsEventLoader mEventLoader;

        public BothEndsLoaderThread(LinkedBlockingQueue<BothEndsLoadRequest> queue, BothEndsEventLoader eventLoader) {
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
                Log.e("Cal", "ForemostLoaderThread.shutdown() interrupted!");
            }
        }

        @Override
        public void run() {
        	
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            
            while (true) {
                try {
                    // Wait for the next request
                	BothEndsLoadRequest request = mQueue.take();

                    // If there are a bunch of requests already waiting, 
                    // then skip all but the most recent request.
                    // :왜? 이런 짓을 할까???
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
                    Log.e("Cal", "background ForemostLoaderThread interrupted!");
                }
            }
        }
    }
    
    public BothEndsEventLoader(Context context, int type) {
        mContext = context;
        mLoaderType = type;
        mBothEndsEventLoaderQueue = new LinkedBlockingQueue<BothEndsLoadRequest>();
        mResolver = context.getContentResolver();
    }

    /**
     * Call this from the activity's onResume()
     */
    public void startBackgroundThread() {
    	mBothEndsLoaderThread = new BothEndsLoaderThread(mBothEndsEventLoaderQueue, this);
    	mBothEndsLoaderThread.start();
    }

    /**
     * Call this from the activity's onPause()
     */
    public void stopBackgroundThread() {
    	mBothEndsLoaderThread.shutdown();
    }
    
    
    public void loadBothEndsEventsInBackground(int startDay, int endDay,
    		Event event, final Runnable successCallback, final Runnable cancelCallback) {

        // Increment the sequence number for requests.  We don't care if the
        // sequence numbers wrap around because we test for equality with the
        // latest one.
        int id = mSequenceNumber.incrementAndGet();

        // Send the load request to the background thread
        LoadBothEndsEventsRequest request = new LoadBothEndsEventsRequest(id, startDay, endDay,
        		event, successCallback, cancelCallback);

        try {
        	mBothEndsEventLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventsInBackground() interrupted!");
        }
    }
}
