package com.intheeast.acalendar;

import java.util.ArrayList;

import com.intheeast.event.EditEventHelper.AttendeeItem;

import android.content.Context;
import android.provider.CalendarContract.Attendees;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AttendeesInfoMainPageView extends LinearLayout {
	final int YES = 0;
	final int NO = 1;
	final int MAYBE = 2;
	final int NORESPONSE = 3;
		
	Context mContext;
	private final LayoutInflater mInflater;
	ScrollView mScrollView;
	LinearLayout mAttendeesListContainer;
	
	View mUnderLine;
	LayoutParams mItemParams;
	int mResponseIconHeight;
	int mResponseIconWidth;
	
	AttendeesInfoSwitchPageView mViewSwitcher;
	
	private ArrayList<AttendeeItem> mAttendeeItemList;
	
    OnClickListener mAttendeeItemClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			AttendeeItem item = (AttendeeItem)v.getTag();
			mViewSwitcher.settingViewSwitching(true, item);
		}    	
    };
    
    
	public AttendeesInfoMainPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		
		int childHeight = (int)mContext.getResources().getDimension(R.dimen.eventItemHeight); //50dip
		mItemParams = new LayoutParams(LayoutParams.MATCH_PARENT, childHeight);
		
		mAttendeeItemList = new ArrayList<AttendeeItem>();			
	}
	
	
	public void getChildViews(AttendeesInfoSwitchPageView viewSwitcher) {
		mViewSwitcher = viewSwitcher;
		mScrollView = (ScrollView)findViewById(R.id.attendees_infopage_scroll_view);
		mAttendeesListContainer = (LinearLayout)findViewById(R.id.attendees_info_page_list_view);		
	}
	
	public void removeItemInContainerList() {
		mAttendeesListContainer.removeAllViews();
	}	
	
	@SuppressWarnings("unchecked")
	public void addAttendeesLayout(ArrayList<AttendeeItem> list) {
		mAttendeeItemList.clear();
		mAttendeeItemList = (ArrayList<AttendeeItem>)list.clone();
		
		int size = mAttendeeItemList.size();
		for (int i=0; i<size; i++) {
			RelativeLayout itemView = (RelativeLayout)mInflater.inflate(R.layout.attendees_information_page_item_layout, null);
			ImageView icon = (ImageView)itemView.findViewById(R.id.response_color_icon);
			
			AttendeeItem item = mAttendeeItemList.get(i);
			int status = item.mAttendee.mStatus;
			switch(status) {
            case Attendees.ATTENDEE_STATUS_ACCEPTED:
            	icon.setBackgroundResource(R.drawable.attendee_yes);
                break;
            case Attendees.ATTENDEE_STATUS_DECLINED:
            	icon.setBackgroundResource(R.drawable.attendee_no);
                break;
            case Attendees.ATTENDEE_STATUS_TENTATIVE:
            	icon.setBackgroundResource(R.drawable.attendee_maybe);
                break;
            default: //Attendees.ATTENDEE_STATUS_NONE
            	icon.setBackgroundResource(R.drawable.attendee_noresponse);
			}
			
			TextView nameView = (TextView)itemView.findViewById(R.id.attendee_name);
			nameView.setText(TextUtils.isEmpty(item.mAttendee.mName) ? item.mAttendee.mEmail : item.mAttendee.mName);			
			itemView.setTag(item);
			itemView.setOnClickListener(mAttendeeItemClick);
			mAttendeesListContainer.addView(itemView, mItemParams);	
		}	
	}
	
}
