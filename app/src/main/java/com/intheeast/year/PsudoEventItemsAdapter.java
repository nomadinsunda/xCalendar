package com.intheeast.year;

import java.util.ArrayList;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.EventInfoLoader.EventCursors;
import com.intheeast.month.EventItemView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.AbsListView.LayoutParams;

public class PsudoEventItemsAdapter extends BaseAdapter {

	public Context mContext;
	PsudoEventListView mListView;
	public LayoutInflater mInflater;
	
	
	protected ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
	//public ArrayList<Event> mEvents = null;
	//public int mFirstJulianDay;
	public int mFirstDayOfWeek;
	public int mQueryDays;
	
	public int mTargetMonthFirstJulianDay;
	public int mSelectedJulianDay;
	public int mCount;
	public int mItemWidth;
	public int mItemHeight;
	String mHomeTimeZone;
	
	ArrayList<Event> mSelectedJulianDayEventList = null;	
	ArrayList<EventCursors> mEventCursors = new ArrayList<EventCursors>();   
    
	public PsudoEventItemsAdapter(Context context, int firstDayOfWeek, int itemWidth, int itemHeight) {
		mContext = context;
		
		mFirstDayOfWeek = firstDayOfWeek;
		mItemWidth = itemWidth;
		mItemHeight = itemHeight;    
		mHomeTimeZone = Utils.getTimeZone(mContext, null);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);        
	}
	
	boolean mTargetDayOwnEvents = true;
	public void setEvents(int targetMonthFirstJulianDay, int selectedJulianDay, ArrayList<Event> events) {  	
		mTargetMonthFirstJulianDay = targetMonthFirstJulianDay;
		mSelectedJulianDay = selectedJulianDay;		
		mSelectedJulianDayEventList = events;
		mCount = mSelectedJulianDayEventList.size();		
	}
	
	public void setNonEvents() {
		mCount = 5;
		mSelectedJulianDayEventList = null;
		
		mTargetDayOwnEvents = false;
		notifyDataSetChanged();
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
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		EventItemView v;
		if (convertView != null) {
			v = (EventItemView)convertView;
		}
		else {			
			v = new EventItemView(mContext, mItemWidth, mItemHeight);
			v.setOrientation(LinearLayout.HORIZONTAL);
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);				
			v.setLayoutParams(params);		
			v.setClickable(false); //		
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
		
		/*
        if (mSelectedJulianDayEventList == null) {
            if (index == 1) 
            	v.setNoneEvent(true);      
            v.setNoneEvent(false);
            return;
        }
        
        Event e = mSelectedJulianDayEventList.get(index);
        v.setEvent(e, mHomeTimeZone, mItemHeight);     
        */   
    }
	
	public void setListView(PsudoEventListView lv) {
    	
        mListView = lv;        
    }
}
