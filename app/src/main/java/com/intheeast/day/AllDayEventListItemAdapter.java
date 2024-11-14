package com.intheeast.day;

import java.util.ArrayList;

import com.intheeast.acalendar.Event;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AllDayEventListItemAdapter extends BaseAdapter {

	public final int ITEM_MAX_EVENT_NUMBERS = 2;
	final int ITEM_LEFT_SIDE = 0;
	final int ITEM_RIGHT_SIDE = 1;
	final int ITEM_ONLY_ONE = 2;
	
	Context mContext;
	LayoutInflater mInflater;
	
	public int mEventCounts;
	public int mCount;
	int mItemWidth;
	int mItemHeight;
	int mFirstItemTopMargin;
	int mLastItemBottomMargin;	
	int mEventHorizontalGapSize;
	int mEventWidth;
		
	ArrayList<EventInsideAllDayEventListViewItem> mEventsInsideAllDayEventListViewItem;
	
	public AllDayEventListItemAdapter(Context context, int eventCounts, 
			int itemWidth, 
			int itemHeight,
			int itemTopMargin,			
			int eventHorizontalGapSize,
			ArrayList<Event> allDayEvents) {
		mContext = context;
		
		mEventCounts = eventCounts;
		mCount = (eventCounts / 2);
		if ((eventCounts % 2) != 0)
			mCount++;
		
		mItemWidth = itemWidth;
		mItemHeight = itemHeight;  
		mFirstItemTopMargin = itemTopMargin;
		mLastItemBottomMargin = mFirstItemTopMargin;
		mEventHorizontalGapSize = eventHorizontalGapSize;
		mEventWidth = (mItemWidth - mEventHorizontalGapSize) / 2;
		
		setAllDayEventListViewItem(allDayEvents);
		
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);        
        
        //mGestureDetector = new GestureDetector(mContext, new EventGestureListener());
	}
	
	public void setAllDayEventListViewItem(ArrayList<Event> allDayEventList) {
		
		int allDayEventListSize = allDayEventList.size();
				
		mEventsInsideAllDayEventListViewItem = new ArrayList<EventInsideAllDayEventListViewItem>();
		int position = 0;
		int eventNumbersInsideItem = 0;
		for (int i=0; i<allDayEventListSize; i++) {
			Event e = allDayEventList.get(i);
			EventInsideAllDayEventListViewItem obj = null;
			if (eventNumbersInsideItem == 0) { // ���ο� position�� event�� �Ҵ��ϴ� ����
				obj = new EventInsideAllDayEventListViewItem(e, position, ITEM_LEFT_SIDE);
				mEventsInsideAllDayEventListViewItem.add(obj);
				eventNumbersInsideItem++;
			}
			else //if (eventNumbersInsideItem == 1) 
			{	
				obj = new EventInsideAllDayEventListViewItem(e, position, ITEM_RIGHT_SIDE);
				mEventsInsideAllDayEventListViewItem.add(obj);
				position++;
				eventNumbersInsideItem = 0; //reset
			}			
		}
				
		int lastIndex = mEventsInsideAllDayEventListViewItem.size() - 1;
		EventInsideAllDayEventListViewItem lastObj = mEventsInsideAllDayEventListViewItem.get(lastIndex);
		if (lastObj.getWhichSide() == ITEM_LEFT_SIDE) {
			lastObj.setWhichSide(ITEM_ONLY_ONE);
		}		
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
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
		return 0;
	}

	// item �ϳ� �� �ִ� �� ���� event�� ������ �ȴ�
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		LinearLayout view = new LinearLayout(mContext);
		AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(mItemWidth, mItemHeight);
		view.setLayoutParams(layoutParams);
		
		view.setOrientation(LinearLayout.HORIZONTAL);
		
		int size = mEventsInsideAllDayEventListViewItem.size();
		for (int i=0; i<size; i++) {
			EventInsideAllDayEventListViewItem obj = mEventsInsideAllDayEventListViewItem.get(i);
			
			
			if (obj.mPosition == position) {
				TextView eventTextView = new TextView(mContext);
				int color;        
		        color = obj.mEvent.color;
		        
		        int bg = 0xffffffff;
		        int a = 153;
		        int red = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
		        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
		        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
		        color = (0xff000000) | ((red | g | b) >> 8);   
		        
				eventTextView.setBackgroundColor(color);
				
				switch(obj.mWhichSide) {
				case ITEM_LEFT_SIDE:
					LayoutParams leftParams = new LayoutParams(mEventWidth, mItemHeight);
					eventTextView.setLayoutParams(leftParams);
					break;
				case ITEM_RIGHT_SIDE:
					LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(mEventWidth, mItemHeight);
					rightParams.leftMargin = mEventHorizontalGapSize;
					eventTextView.setLayoutParams(rightParams);					
					break;
				case ITEM_ONLY_ONE:
					LayoutParams onlyOneParams = new LayoutParams(mItemWidth, mItemHeight);
					eventTextView.setLayoutParams(onlyOneParams);
					break;
				}
				
				int left = mEventHorizontalGapSize * 2;				
				eventTextView.setPadding(left, eventTextView.getPaddingTop(), eventTextView.getPaddingRight(), eventTextView.getPaddingBottom());
				
				eventTextView.setTextColor(Color.BLACK);				
				eventTextView.setText(obj.mEvent.title.toString());
				view.addView(eventTextView);
			}			
			
		}
		
		return view;
	}	
	
	
	
	public class EventInsideAllDayEventListViewItem {
		int mPosition;
		int mWhichSide;
		Event mEvent;
		
		public EventInsideAllDayEventListViewItem(Event e, int position, int whichSide) {
			
			mEvent = e;
			mPosition = position;
			mWhichSide = whichSide;
		}
		
		public int getWhichSide() {
			return mWhichSide;
		}
		
		public void setWhichSide(int whichSide) {
			mWhichSide = whichSide;
		}
	}

}
