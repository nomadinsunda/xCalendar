package com.intheeast.month;

import java.util.ArrayList;
import java.util.Calendar;

import com.intheeast.day.AllDayEventListItemAdapter;
import com.intheeast.day.AllDayEventListView;
import com.intheeast.day.DayFragment;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.Event;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class PsudoExtendedDayView extends LinearLayout {
	private static String TAG = "ExtendedDayView";
    private static boolean INFO = true;
    
	Context mContext;
	Activity mActivity;
	
	CalendarController mController;	
	
	AllDayEventListView mAllDayEventLists;
	public PsudoDayView mDayView;	
	
	Calendar mSelectedDay;
	int mWhichDay;
	
	ExtendedDayViewDimension mExtendedDayViewDimension;
	
	public PsudoExtendedDayView(Context context) {
		super(context);
		mContext = context;
		
	}
	
	public PsudoExtendedDayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public PsudoExtendedDayView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	public void init(Activity activity, CalendarController controller, 
			Calendar selectedDay, int whichDay, ExtendedDayViewDimension extendedDayViewDimension) {		
		
		mActivity = activity;
		
		mController = controller;
		
		mSelectedDay = selectedDay;
		mWhichDay = whichDay;
				
		mExtendedDayViewDimension = extendedDayViewDimension;
		
		setOrientation(LinearLayout.VERTICAL);
		
		makeDayViewLayout();
	}
	
	int mKickOffLeftX;
	float mCurrentLeftX;
	
	@Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {	
		
		switch(mWhichDay) {
		case DayFragment.PREVIOUS_DAY_EVENTS:
			mKickOffLeftX = 0 - mViewWidth;
			break;
		case DayFragment.CURRENT_DAY_EVENTS:
			mKickOffLeftX = 0;
			break;
		case DayFragment.NEXT_DAY_EVENTS:
			mKickOffLeftX = mViewWidth;
			break;
		}			
		
		mCurrentLeftX = mKickOffLeftX;
				
		setX(mCurrentLeftX);
    }		
	
	
	int mViewWidth;
	int mHourWidth;
	int mCellWidth;
	int mAlldayEventListViewMaxHeight;
	int mAlldayEventListViewItemWidth;
	int mAlldayEventListViewItemHeight;
	int mAlldayEventListViewFirstItemTopMargin;
	int mAlldayEventListViewLastItemBottomMargin;
	int mAlldayEventListViewItemGap;
	public void makeDayViewLayout() {
		mAllDayEventLists = new AllDayEventListView(mContext);
    	
    	LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	mAllDayEventLists.setLayoutParams(params);
    	
    	mAllDayEventLists.setBackgroundColor(0xffd0d0d0); //0xff808080
    	mAllDayEventLists.setCacheColorHint(0);	        
    	mAllDayEventLists.setDivider(null);	        
    	mAllDayEventLists.setItemsCanFocus(true);	        
    	mAllDayEventLists.setFastScrollEnabled(false); 
    	mAllDayEventLists.setVerticalScrollBarEnabled(false);	    	
    	mAllDayEventLists.setFadingEdgeLength(0);
    	    	
    	mViewWidth = mExtendedDayViewDimension.viewWidth;
    	mHourWidth = mExtendedDayViewDimension.hourWidth;
    	mCellWidth = mExtendedDayViewDimension.cellWidth;
    	
    	mAlldayEventListViewMaxHeight = mExtendedDayViewDimension.alldayEventListViewMaxHeight;
    	mAlldayEventListViewItemHeight = mExtendedDayViewDimension.alldayEventListViewItemHeight;
    	mAlldayEventListViewFirstItemTopMargin = mExtendedDayViewDimension.alldayEventListViewFirstItemTopMargin;
    	mAlldayEventListViewLastItemBottomMargin = mExtendedDayViewDimension.alldayEventListViewLastItemBottomMargin;
    	mAlldayEventListViewItemGap = mExtendedDayViewDimension.alldayEventListViewItemGap;
    	
    	int leftPadding = mExtendedDayViewDimension.hourWidth + mExtendedDayViewDimension.alldayEventListViewItemGap;
    	int rightPadding = mExtendedDayViewDimension.alldayEventListViewItemGap;
    	mAlldayEventListViewItemWidth = mCellWidth - (mExtendedDayViewDimension.alldayEventListViewItemGap * 2);
    	mAllDayEventLists.setPadding(leftPadding, mExtendedDayViewDimension.alldayEventListViewFirstItemTopMargin, rightPadding, mExtendedDayViewDimension.alldayEventListViewLastItemBottomMargin);
    	mAllDayEventLists.setDividerHeight(mExtendedDayViewDimension.alldayEventListViewItemGap);
    	    	
    	mAllDayEventLists.setVisibility(View.GONE);    	
    	addView(mAllDayEventLists);
    	
		mDayView = new PsudoDayView(mActivity, mController, 1);
		mDayView.setId(DayFragment.VIEW_ID);
		mDayView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));    	
    	
		mDayView.initBaseDateTime(mSelectedDay, false, false);
		
		addView(mDayView);
	}
	
	AllDayEventListItemAdapter mAllDayEventListItemAdapter;
	int mAllDayEventCounts = 0;
	public void setAllDayEventListView(int allDayEventCounts, ArrayList<Event> allDayEvents) {	
		mAllDayEventCounts = allDayEventCounts;
		if (allDayEventCounts == 0) {		
			mAllDayEventLists.setVisibility(View.GONE);
			return;
		}
		
		int allDayHeight = 0;
		if (allDayEventCounts < 3) {
            allDayHeight = mAlldayEventListViewFirstItemTopMargin + 
            		mAlldayEventListViewItemHeight +
            		mAlldayEventListViewLastItemBottomMargin;
        } else if (allDayEventCounts < 5){
            // Allow the all-day area to grow in height depending on the
            // number of all-day events we need to show, up to a limit.
            allDayHeight = mAlldayEventListViewFirstItemTopMargin + 
            		mAlldayEventListViewItemHeight +
            		mAlldayEventListViewItemGap +
            		mAlldayEventListViewItemHeight +
            		mAlldayEventListViewLastItemBottomMargin;
            
        } else {
        	allDayHeight = mAlldayEventListViewMaxHeight;
            
        }
		
    	// mAllDayEventLists���� first item top margin�� last bottom margin��
    	// GRID_LINE_TOP_MARGIN�� 1/2�̴�
    	// item ���� margin��? top margin�� 1/2�̴�   	
    	
    	mAllDayEventLists.setVisibility(View.VISIBLE);
    	
    	LayoutParams params = new LayoutParams(mViewWidth, allDayHeight);
    	mAllDayEventLists.setLayoutParams(params);
    	
    	
    	
    	mAllDayEventListItemAdapter = new AllDayEventListItemAdapter(mContext, allDayEventCounts, 
    			mAlldayEventListViewItemWidth, 
    			mAlldayEventListViewItemHeight, 
    			mAlldayEventListViewFirstItemTopMargin, 
    			mExtendedDayViewDimension.alldayEventListViewItemGap / 2,    			
    			allDayEvents);
    	mAllDayEventLists.setAdapter(mAllDayEventListItemAdapter);     	
    }
	
	public static class ExtendedDayViewDimension {
		int viewWidth;
		int hourWidth;
		int cellWidth;
		int alldayEventListViewMaxHeight;
		int alldayEventListViewItemHeight;
		int alldayEventListViewFirstItemTopMargin;
		int alldayEventListViewLastItemBottomMargin;
		int alldayEventListViewItemGap;
		
		public ExtendedDayViewDimension(int viewWidth, int hourWidth, int cellWidth, int alldayEventListViewMaxHeight,
				int alldayEventListViewItemHeight, int alldayEventListViewFirstItemTopMargin, int alldayEventListViewLastItemBottomMargin, int alldayEventListViewItemGap) {
			this.viewWidth = viewWidth;
			this.hourWidth = hourWidth;
			this.cellWidth = cellWidth;
	    	
			this.alldayEventListViewMaxHeight = alldayEventListViewMaxHeight;
			this.alldayEventListViewItemHeight = alldayEventListViewItemHeight;
			this.alldayEventListViewFirstItemTopMargin = alldayEventListViewFirstItemTopMargin;
			this.alldayEventListViewLastItemBottomMargin = alldayEventListViewLastItemBottomMargin;
			this.alldayEventListViewItemGap = alldayEventListViewItemGap;
		}
	}
	
	
	
}
