package com.intheeast.acalendar;

import java.util.ArrayList;

import com.intheeast.event.EventViewUtils.ReminderItemLayoutTag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ReminderSelectPageView extends LinearLayout {
	Context mContext;
	private final LayoutInflater mInflater;
	ScrollView mScrollView;
	LinearLayout mReminderLabelList;
	LayoutParams mItemParams;
	boolean mConstructorLayout = false;
	
	FastEventInfoView mMainPage;
	ArrayList<Integer> mReminderMinuteValues;
	ArrayList<String> mReminderMinuteLabels;
	ArrayList<RelativeLayout> mReminderItemLayoutsOfMainPage;
	ArrayList<RelativeLayout> mItemLayout;
	int mSelectedItemIndex;
	TextView mTextViewOfSelectedReminderLayoutOfMainPage;
	
	ImageView mReminderPageItemSelectionIcon;
	RelativeLayout.LayoutParams mReminderPageItemSelectionIconImageViewLayout;
	
	OnClickListener mReminderItemLayoutClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// 먼저 선택된 layout index를 판별해야 한다
			TextView tv = (TextView)v.findViewById(R.id.reminder_select_item_label);	
			String reminderLabel = (String)tv.getText();
			int clickedItemIndex = mReminderMinuteLabels.indexOf(reminderLabel);
			if (mSelectedItemIndex == clickedItemIndex)
				return;
			else {
				// 아이콘을 detach & attach 해야 한다
				// 아이콘 detach
				RelativeLayout itemView = mItemLayout.get(mSelectedItemIndex);
				itemView.removeView(mReminderPageItemSelectionIcon);
				// 아이콘 attach
				mSelectedItemIndex = clickedItemIndex;	
				RelativeLayout newlySelectedItemView = mItemLayout.get(mSelectedItemIndex);
				newlySelectedItemView.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayout);				
				
				// mReminderViews의 값을 변경시켜야 한다
				RelativeLayout rl = mReminderItemLayoutsOfMainPage.get(mIndexOfReminderItemLayoutsOfMainPage);
				TextView textViewOfSelectedReminderLayoutOfMainPage = (TextView)rl.findViewById(R.id.reminder_minute_value);
				textViewOfSelectedReminderLayoutOfMainPage.setText(reminderLabel);
				
				ReminderItemLayoutTag oldTag = (ReminderItemLayoutTag)rl.getTag();
				ReminderItemLayoutTag newTag = new ReminderItemLayoutTag(oldTag.mLayoutListIndex, mReminderMinuteValues.get(mSelectedItemIndex), oldTag.mReminderMethod);
				rl.setTag(newTag);
				
				// mainpage로 스위칭이 발생해야 한다
				mMainPage.returnMainPage(FastEventInfoView.EVENT_REMINDER_PAGE);
			}
		}
    	
    };
	
	public ReminderSelectPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		int childHeight = (int)mContext.getResources().getDimension(R.dimen.eventItemHeight); //50dip
		mItemParams = new LayoutParams(LayoutParams.MATCH_PARENT, childHeight);
		
		mReminderPageItemSelectionIcon = new ImageView(mContext);
		mReminderPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mReminderPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);	
		
		mReminderPageItemSelectionIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mReminderPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mReminderPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		
		mSelectedItemIndex = -1;
		// eventItemHeight
		// 컨스트럭터에서는 아래의 두 뷰의 표인터를 얻을 수 없다
		//mScrollView = (ScrollView)this.findViewById(R.id.reminderpage_scroll_view);
		//mReminderLabelList = (LinearLayout)this.findViewById(R.id.reminderpage_list_view);
	}
	
	
	public void constructLayout(FastEventInfoView view, ArrayList<Integer> reminderMinuteValues, ArrayList<String> reminderMinuteLabels, ArrayList<RelativeLayout> reminderItemLayoutsOfMainPage) {
		if (!mConstructorLayout) {
			mConstructorLayout = true;
		}
		else 
			return;
		
		mMainPage = view;
		mReminderMinuteValues = reminderMinuteValues;
		mReminderMinuteLabels = reminderMinuteLabels;
		mReminderItemLayoutsOfMainPage = reminderItemLayoutsOfMainPage;
		
		if (mScrollView == null)
			mScrollView = (ScrollView)findViewById(R.id.reminderpage_scroll_view);
		if (mReminderLabelList == null)
			mReminderLabelList = (LinearLayout)findViewById(R.id.reminderpage_list_view);
		
		int size = reminderMinuteLabels.size();
		mItemLayout = new ArrayList<RelativeLayout>();
		
		for (int i=0; i<size; i++) {
			RelativeLayout itemView = (RelativeLayout)mInflater.inflate(R.layout.reminder_select_page_item_layout, null);
			TextView reminderLabel = (TextView)itemView.findViewById(R.id.reminder_select_item_label);
			String labelStr = reminderMinuteLabels.get(i);
			reminderLabel.setText(labelStr);
			itemView.setOnClickListener(mReminderItemLayoutClickListener);
			mReminderLabelList.addView(itemView, mItemParams);
			mItemLayout.add(itemView);
		}
	}
	
	// 여기서 우리는 mReminderItemLayoutsOfMainPage의 인덱스를 얻어야 한다
	int mIndexOfReminderItemLayoutsOfMainPage;
	public void resetReminderPage(int indexOfReminderItemLayoutsOfMainPage, int selectIndex) {
		
		if (mSelectedItemIndex != -1) {
			// 아이콘을 detach 해야 한다
			RelativeLayout itemView = mItemLayout.get(mSelectedItemIndex);
			itemView.removeView(mReminderPageItemSelectionIcon);
		}
		
		mSelectedItemIndex = selectIndex;
		mIndexOfReminderItemLayoutsOfMainPage = indexOfReminderItemLayoutsOfMainPage;
		RelativeLayout itemView = mItemLayout.get(mSelectedItemIndex);
		itemView.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayout);
	}

}
