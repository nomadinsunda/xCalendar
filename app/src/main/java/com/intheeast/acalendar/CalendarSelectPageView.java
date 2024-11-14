package com.intheeast.acalendar;

import java.util.ArrayList;

import com.intheeast.acalendar.CalendarMetaDataList.CalendarInfo;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.provider.CalendarContract;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class CalendarSelectPageView extends LinearLayout {
	private final String GOOGLE_ACCOUNT_TYPE = "com.google";
	private final String KCALENDAR_ACCOUNT_NAME = "KCalendar";
	private final String GMAIL_CALENDAR_LABEL = "GMAIL";
	
	Context mContext;
	private final LayoutInflater mInflater;
	ScrollView mScrollView;
	LinearLayout mCalendarlListContainer;
	LayoutParams mItemParams;
	LayoutParams mSeperatorParams;
	boolean mConstructorLayout = false;
	FastEventInfoView mMainPage;
	ArrayList<CalendarInfo> mCalendarInfoList;
	ArrayList<RelativeLayout> mLocalCalendarInfoLayout;
	ArrayList<RelativeLayout> mGmailCalendarInfoLayout;
	
	public CalendarSelectPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		int childHeight = (int)mContext.getResources().getDimension(R.dimen.eventItemHeight); //50dip
		mItemParams = new LayoutParams(LayoutParams.MATCH_PARENT, childHeight);
		int seperatorHeight = (int)mContext.getResources().getDimension(R.dimen.calendarPageCalendarCategorySeperatorHeight);
		mSeperatorParams = new LayoutParams(LayoutParams.MATCH_PARENT, seperatorHeight);
		
		mLocalCalendarInfoLayout = new ArrayList<RelativeLayout>();
		mGmailCalendarInfoLayout = new ArrayList<RelativeLayout>();
	}
	
	public void constructLayout(FastEventInfoView view, ArrayList<CalendarInfo> calendarInfoList) {
		if (!mConstructorLayout) {
			mConstructorLayout = true;
		}
		else 
			return;
		
		mMainPage = view;
		mCalendarInfoList = calendarInfoList;
		if ( (mCalendarInfoList == null) || (mCalendarInfoList.size() == 0) )
			return;
		
		if (mScrollView == null)
			mScrollView = (ScrollView)findViewById(R.id.calendarpage_scroll_view);
		if (mCalendarlListContainer == null)
			mCalendarlListContainer = (LinearLayout)findViewById(R.id.calendarpage_list_view);
		
		mLocalCalendarInfoLayout.clear();
		mGmailCalendarInfoLayout.clear();
		
		// 먼저 local klife 그리고 gmail item layout을 먼저 생성하자
		int size = mCalendarInfoList.size();
		for (int i=0; i<size; i++) {
			CalendarInfo obj = mCalendarInfoList.get(i);
			
			if (obj.mAccountType.equals(GOOGLE_ACCOUNT_TYPE)) {
				RelativeLayout itemView = makeItem(obj);
				mGmailCalendarInfoLayout.add(itemView);
			}			
			else if  ( obj.mAccountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL) && obj.mAccountName.equals(KCALENDAR_ACCOUNT_NAME) ) {
	    		RelativeLayout itemView = makeItem(obj);
	    		mLocalCalendarInfoLayout.add(itemView);
	    	}			
		}	
		
		if (!mLocalCalendarInfoLayout.isEmpty()) {
			// calendar_category_seperator_layout
			RelativeLayout seperatorView = (RelativeLayout)mInflater.inflate(R.layout.calendar_category_seperator_layout, null);
			TextView categoryLable = (TextView)seperatorView.findViewById(R.id.calendar_category_label);
			CalendarInfo obj = (CalendarInfo) mLocalCalendarInfoLayout.get(0).getTag(); // sampling
			categoryLable.setText(obj.mAccountName);
			mCalendarlListContainer.addView(seperatorView, mSeperatorParams);
			size = mLocalCalendarInfoLayout.size();
			for (int i=0; i<size; i++) {
				mCalendarlListContainer.addView(mLocalCalendarInfoLayout.get(i), mItemParams);
			}
		}
		
		if (!mGmailCalendarInfoLayout.isEmpty()) {
			RelativeLayout seperatorView = (RelativeLayout)mInflater.inflate(R.layout.calendar_category_seperator_layout, null);
			TextView categoryLable = (TextView)seperatorView.findViewById(R.id.calendar_category_label);
			categoryLable.setText(GMAIL_CALENDAR_LABEL);
			mCalendarlListContainer.addView(seperatorView, mSeperatorParams);
			size = mGmailCalendarInfoLayout.size();
			for (int i=0; i<size; i++) {
				mCalendarlListContainer.addView(mGmailCalendarInfoLayout.get(i), mItemParams);
			}
		}		
	}
	
	public RelativeLayout makeItem(CalendarInfo obj) {
		RelativeLayout itemView = (RelativeLayout)mInflater.inflate(R.layout.calendar_info_layout, null);
		
		TextView calendarDisplayName = (TextView)itemView.findViewById(R.id.calendar_text);
        calendarDisplayName.setText(obj.mDisplayName);
        
        View colorIcon =(View)itemView.findViewById(R.id.calendar_color_icon);
        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
    	Paint paint = shapeDrawable.getPaint();		
    	paint.setColor(Utils.getDisplayColorFromColor(obj.mCalendarColor));
    	colorIcon.setBackground(shapeDrawable);
    	
		itemView.setTag(obj);
		
		return itemView;
	}
	
	
	
	

}
