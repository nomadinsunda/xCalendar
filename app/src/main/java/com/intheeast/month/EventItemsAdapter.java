package com.intheeast.month;

import java.util.ArrayList;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;

import android.content.Context;
//import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.AbsListView.LayoutParams;
import android.widget.LinearLayout;


// �� ��ġ event ������ ��������
public class EventItemsAdapter extends BaseAdapter /*implements OnTouchListener*/ {

	private static final String TAG = "EventItemsAdapter";
	
	public Context mContext;
	EventsListView mListView;
	public LayoutInflater mInflater;
	
	protected ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
	ArrayList<Event> mSelectedJulianDayEventList = null;
	
	//public ArrayList<Event> mEvents = null;
	//public int mFirstJulianDay;
	public int mFirstDayOfWeek;
	public int mQueryDays;
	
	//public int mTargetMonthFirstJulianDay;
	public int mSelectedJulianDay;
	public int mCount;
	public int mItemWidth;
	public int mItemHeight;
	String mHomeTimeZone;
	
	EventItemView mClickedView;
	EventItemView mSingleTapUpView;	
	
	float mClickedXLocation;                // Used to find which day was clicked
    float mClickedYLocation;                // Used to find which day was clicked
    long mClickTime;                        // Used to calculate minimum click animation time
	//protected GestureDetector mGestureDetector;
    
    ArrayList<EventCursors> mEventCursors = new ArrayList<EventCursors>();
    Runnable mEventInfoLoaderSuccessCallback = new Runnable() {
    	
        public void run() {
        	boolean empty = mEventCursors.isEmpty();
        	
        	if (!empty) {
        		Log.i("tag", "mEventInfoLoaderSuccessCallback:SUCCESS");       
        		notifyDataSetChanged();
        	}
        	else {        		 
        		Log.i("tag", "mEventInfoLoaderSuccessCallback:what???");
        	}
        	
        }
    };
    
    private final Runnable mEventInfoLoaderCancelCallback = new Runnable() {
        public void run() {
            //
        	Log.i("tag", "Called EventInfoLoaderCancelCallback");
        }
    };    
	
	public EventItemsAdapter(Context context, int firstDayOfWeek, int itemWidth, int itemHeight) {
		mContext = context;
		mFirstDayOfWeek = firstDayOfWeek;
		mItemWidth = itemWidth;
		mItemHeight = itemHeight;    
		mHomeTimeZone = Utils.getTimeZone(mContext, null);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);        
        
        //mGestureDetector = new GestureDetector(mContext, new EventGestureListener());
	}
		
	
	public void setSelectedJulianDayInExpandEventViewMode(int selectedJulainDay) {
		mSelectedJulianDay = selectedJulainDay;
	}
	
	boolean mTargetDayOwnEvents = true;
	public void setEvents(int selectedJulianDay, ArrayList<Event> events) {  	
		
		mSelectedJulianDay = selectedJulianDay;		
		mSelectedJulianDayEventList = events;
		mCount = mSelectedJulianDayEventList.size();
		
		// mEventCursors�� ��� ���ϳ�????
		mEventCursors.clear();
		mFragment.mEventInfoLoaderForEventListView.loadEventsInfoInBackground(mEventCursors, mSelectedJulianDayEventList, mEventInfoLoaderSuccessCallback, mEventInfoLoaderCancelCallback);
		
		mTargetDayOwnEvents = true;
		//notifyDataSetChanged();
	}
	
	// ���� ���ǰ� ���� �ʴ�
	public void setEventsDirectly(int selectedJulianDay, ArrayList<Event> events) {  			
		mSelectedJulianDay = selectedJulianDay;		
		mSelectedJulianDayEventList = events;
		mCount = mSelectedJulianDayEventList.size();	
		
		mTargetDayOwnEvents = true;
	}
	
	
	public void setNonEvents() {
		mCount = 5; // ������ ���ľ� ���� ������...�׳� �γ�???
		mSelectedJulianDayEventList = null;
		
		mTargetDayOwnEvents = false;
		notifyDataSetChanged();
	}	
	
	public void setDirectNonEvents() {
		mCount = 5;
		mSelectedJulianDayEventList = null;
		
		mTargetDayOwnEvents = false;		
	}	
		
	
	@Override
	public int getCount() {		
		return mCount;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//Log.i("tag", "EventItemsAdapter:getView : position=" + String.valueOf(position));
		// position�� numbr ordering�� 0���� �����ΰ�?
		// :�׷��ٸ� position�� mSelectedJulianDayEventList�� index�� ������ ���̴�
		// ���� event�� ���� ���̶��,
		// :������ ��Ÿ���� �Ѵ�		
		EventItemView v;
		if (convertView != null) {
			v = (EventItemView)convertView;
		}
		else {			
			v = new EventItemView(mContext, mItemWidth, mItemHeight);
			v.setOrientation(LinearLayout.HORIZONTAL);
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);				
			v.setLayoutParams(params);		
			v.setClickable(true); // 
			v.setOnClickListener(mEventListItemClickListener);			
		}		
		
		sendEventToView(v, position);
		return v;
	}	
	
	public void sendEventToView(EventItemView v, int index) {
		if (mTargetDayOwnEvents) {
			Event e = mSelectedJulianDayEventList.get(index);
	        v.setEvent(e, mHomeTimeZone, mItemHeight); 
		}
		else {
			// 2nd item�� �̺�Ʈ�� ���ٴ� text�� drawing �ؾ� �Ѵ�
			if (index == 1) 
            	v.setNoneEvent(true); 
			else
				v.setNoneEvent(false);
		}               
    }
	
	MonthFragment mFragment;
    public void setListView(MonthFragment fragment, EventsListView lv) {
    	mFragment = fragment;
        mListView = lv;        
    }
    
    OnClickListener mEventListItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {			
			mSingleTapUpView = (EventItemView) v;
        	
        	long eventId = mSingleTapUpView.mEvent.id;
        	long startMills = mSingleTapUpView.mEvent.startMillis;
        	long endMills = mSingleTapUpView.mEvent.endMillis;
        	
        	mFragment.launchFastEventInfoView(eventId,
        			startMills, endMills);
		}    	
    };
    
    public EventCursors getEventCursors(long eventId) {
    	int size = mEventCursors.size();
    	EventCursors targetObj = null;
    	for (int i=0; i<size; i++) {
    		EventCursors obj = mEventCursors.get(i);
    		if (eventId == obj.mEventId) {
    			targetObj = obj;
    			break;
    		}                			
    	}
    	
    	return targetObj;
    }
    
    public void closeEventCursors() {
    	int size = mEventCursors.size();    	
    	for (int i=0; i<size; i++) {
    		EventCursors obj = mEventCursors.get(i);
    		obj.closeCursors();           			
    	}
    }
    
        
    public void reloadEventsOfEventListView(int targetJualianDay) {    	
    	mSelectedJulianDay = targetJualianDay;		
        final ArrayList<Event> events = new ArrayList<Event>();
        mEventCursors.clear();
        mFragment.mEventLoaderForEventListView.loadEventsInBackground(1, events, targetJualianDay, 
        		new Runnable() {
        	
		            public void run() {	            	
		                		            	
		            	mSelectedJulianDayEventList = events;
		                
		                if (!mSelectedJulianDayEventList.isEmpty()) {
		                	mTargetDayOwnEvents = true;
		            		mCount = mSelectedJulianDayEventList.size();
		            		
		                	////////////////////////////////////////////////////////////////
		                	mFragment.mEventInfoLoaderForEventListView.loadEventsInfoInBackground(mEventCursors, mSelectedJulianDayEventList, mEventInfoLoaderSuccessCallback, mEventInfoLoaderCancelCallback);
		                	////////////////////////////////////////////////////////////////	
		                }
		                else {
		                	setNonEvents();
		                }
		            }
		        }, 
		        mCancelCallback);        
    }
    
    public void reloadEventsOfEventListView(int targetJualianDay, ArrayList<Event> events) {    	
    	mSelectedJulianDayEventList = events;
        
        if (!mSelectedJulianDayEventList.isEmpty()) {
        	mTargetDayOwnEvents = true;
    		mCount = mSelectedJulianDayEventList.size();
    		
        	////////////////////////////////////////////////////////////////
        	mFragment.mEventInfoLoaderForEventListView.loadEventsInfoInBackground(mEventCursors, mSelectedJulianDayEventList, mEventInfoLoaderSuccessCallback, mEventInfoLoaderCancelCallback);
        	////////////////////////////////////////////////////////////////	
        }
        else {
        	setNonEvents();
        }
    }
    
    private final Runnable mCancelCallback = new Runnable() {
        public void run() {
            clearCachedEvents();
        }
    };
    
    void clearCachedEvents() {
        //mLastReloadMillis = 0;
    }
    
    /*
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	if (!(v instanceof EventItemView)) {
            return false;
        }

        int action = event.getAction();

        // Event was tapped - switch to the detailed view making sure the click animation
        // is done first.
        if (mGestureDetector.onTouchEvent(event)) {
        	mSingleTapUpView = (EventItemView) v;
        	
        	long eventId = mSingleTapUpView.mEvent.id;
        	long startMills = mSingleTapUpView.mEvent.startMillis;
        	long endMills = mSingleTapUpView.mEvent.endMillis;
        	
        	String title = (String) mSingleTapUpView.mEvent.title;
        	
            return true;
        } else {            
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                	//mClickedView = (EventItemView)v;
                    mClickedXLocation = event.getX();
                    mClickedYLocation = event.getY();
                    Log.i("tag", "EventItemsAdapter.onTouch:mClickedXLocation=" + String.valueOf(mClickedXLocation));
                    Log.i("tag", "EventItemsAdapter.onTouch:mClickedYLocation=" + String.valueOf(mClickedYLocation));
                    mClickTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_SCROLL:
                case MotionEvent.ACTION_CANCEL:  
                	Log.i("tag", "EventItemsAdapter.onTouch:");
                    break;
                case MotionEvent.ACTION_MOVE:                    
                    break;
                default:
                    break;
            }
        }
        // Do not tell the frameworks we consumed the touch action so that fling actions can be
        // processed by the fragment.
        return false;
    }
    
    
    private class EventGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
        	Log.i("tag", "EventItemsAdapter.onSingleTapUp");
            return true;
        }
    }
    */

}
