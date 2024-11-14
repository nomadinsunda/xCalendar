package com.intheeast.event;

import java.util.ArrayList;
import java.util.Collections;

import com.intheeast.acalendar.R;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.etc.LockableScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.provider.CalendarContract.Reminders;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EditEventReminderSettingSubView extends LinearLayout {

	public static final int EVENT_NO_REMINDER = 0;
	public static final int EVENT_EVENT_THEN_REMINDER = 1;
	public static final int EVENT_BEFORE_5MIN_REMINDER = 2;
	public static final int EVENT_BEFORE_15MIN_REMINDER = 3;
	public static final int EVENT_BEFORE_30MIN_REMINDER = 4;
	public static final int EVENT_BEFORE_1H_REMINDER = 5;
	public static final int EVENT_BEFORE_2H_REMINDER = 6;
	public static final int EVENT_BEFORE_1DAY_REMINDER = 7;
	public static final int EVENT_BEFORE_2DAY_REMINDER = 8;
	public static final int EVENT_BEFORE_1WEEK_REMINDER = 9;
	public static final int EVENT_UNSUPPORTED_REMINDER = 10;
	
	public static final int EVENT_NO_REMINDER_MINUTES = -1;
	public static final int EVENT_EVENT_THEN_REMINDER_MINUTES = 0;
	public static final int EVENT_BEFORE_5MIN_REMINDER_MINUTES = 5;
	public static final int EVENT_BEFORE_15MIN_REMINDER_MINUTES = 15;
	public static final int EVENT_BEFORE_30MIN_REMINDER_MINUTES = 30;
	public static final int EVENT_BEFORE_1H_REMINDER_MINUTES = 60;
	public static final int EVENT_BEFORE_2H_REMINDER_MINUTES = 120;
	public static final int EVENT_BEFORE_1DAY_REMINDER_MINUTES = 1440;
	public static final int EVENT_BEFORE_2DAY_REMINDER_MINUTES = 2880;
	public static final int EVENT_BEFORE_1WEEK_REMINDER_MINUTES = 10080;

	
	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;	
	EditEvent mEditEvent;
	
	RelativeLayout mNoReminderLayout;
	RelativeLayout mUnsupportedReminderLayout;
    RelativeLayout mEventThenReminderLayout;
    RelativeLayout mBefore5MinReminderLayout;
    RelativeLayout mBefore15MinReminderLayout;
    RelativeLayout mBefore30MinReminderLayout;
    RelativeLayout mBefore1HReminderLayout;
    RelativeLayout mBefore2HReminderLayout;
    RelativeLayout mBefore1DayReminderLayout;
    RelativeLayout mBefore2DayReminderLayout;
    RelativeLayout mBefore1WeekReminderLayout; 
    
    ImageView mReminderPageItemSelectionIcon;
	RelativeLayout.LayoutParams mReminderPageItemSelectionIconImageViewLayoutParams;
	    
    boolean mSelectedFirstReminderItemLayoutOfMainPage = true;
    
	public EditEventReminderSettingSubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventReminderSettingSubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventReminderSettingSubView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	} 
    
	LockableScrollView mReminderPageScrollView;
	RelativeLayout mScrollViewInner;
	LinearLayout mSelectEventReminderLayout;
	public void initView(Activity activity, EditEventFragment fragment, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
				
		mEditEvent = fragment.mEditEvent;
		
		mReminderPageScrollView = (LockableScrollView) findViewById(R.id.editevent_reminderpage_scroll_view);
		mScrollViewInner = (RelativeLayout) mReminderPageScrollView.findViewById(R.id.editevent_reminderpage_scrollview_inner_layout);
		
		mNoReminderLayout =(RelativeLayout)mScrollViewInner.findViewById(R.id.event_reminder_no_reminder_layout);
		mNoReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mSelectEventReminderLayout = (LinearLayout) mScrollViewInner.findViewById(R.id.select_event_reminder_layout);
		
		mUnsupportedReminderLayout = (RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_unsupported_reminder_layout);
		mUnsupportedReminderLayout.setVisibility(View.GONE);
		mUnsupportedReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mEventThenReminderLayout = (RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_event_then_reminder_layout);
		mEventThenReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore5MinReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_5_minute_reminder_layout);
		mBefore5MinReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore15MinReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_15_minute_reminder_layout);
		mBefore15MinReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore30MinReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_30_minute_reminder_layout);
		mBefore30MinReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore1HReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_1_hour_reminder_layout);
		mBefore1HReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore2HReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_2_hour_reminder_layout);
		mBefore2HReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore1DayReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_1_day_reminder_layout);
		mBefore1DayReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore2DayReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_2_day_reminder_layout);
		mBefore2DayReminderLayout.setOnClickListener(mReminderPageItemClickListener);
		
		mBefore1WeekReminderLayout =(RelativeLayout)mSelectEventReminderLayout.findViewById(R.id.event_reminder_before_1_week_reminder_layout);
		mBefore1WeekReminderLayout.setOnClickListener(mReminderPageItemClickListener);		
		
		mReminderPageItemSelectionIcon = new ImageView(mActivity);
		mReminderPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mReminderPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		
		mReminderPageItemSelectionIconImageViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mReminderPageItemSelectionIconImageViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mReminderPageItemSelectionIconImageViewLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
			
	}
		
	View.OnClickListener mReminderPageItemClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){        	
        	
        	//int selectedReminderValue = 0;
        	boolean selectedUnsupportedReminderMinutes = false;
        	int selectedReminderMinutes = 0;
        	int viewID = v.getId();
        	
        	switch(viewID) {
        	case R.id.event_reminder_unsupported_reminder_layout:
        		//selectedReminderValue = EVENT_UNSUPPORTED_REMINDER;
        		selectedUnsupportedReminderMinutes = true;
        		selectedReminderMinutes = (Integer) (mUnsupportedReminderLayout.getTag());
        		break;
        	case R.id.event_reminder_no_reminder_layout:
        		//selectedReminderValue = EVENT_NO_REMINDER;  
        		selectedReminderMinutes = EVENT_NO_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_event_then_reminder_layout:
        		//selectedReminderValue = EVENT_EVENT_THEN_REMINDER; 
        		selectedReminderMinutes = EVENT_EVENT_THEN_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_5_minute_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_5MIN_REMINDER;       
        		selectedReminderMinutes = EVENT_BEFORE_5MIN_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_15_minute_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_15MIN_REMINDER;
        		selectedReminderMinutes = EVENT_BEFORE_15MIN_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_30_minute_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_30MIN_REMINDER;  
        		selectedReminderMinutes = EVENT_BEFORE_30MIN_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_1_hour_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_1H_REMINDER;     
        		selectedReminderMinutes = EVENT_BEFORE_1H_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_2_hour_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_2H_REMINDER;  
        		selectedReminderMinutes = EVENT_BEFORE_2H_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_1_day_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_1DAY_REMINDER;  
        		selectedReminderMinutes = EVENT_BEFORE_1DAY_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_2_day_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_2DAY_REMINDER; 
        		selectedReminderMinutes = EVENT_BEFORE_2DAY_REMINDER_MINUTES;
        		break;
        	case R.id.event_reminder_before_1_week_reminder_layout:
        		//selectedReminderValue = EVENT_BEFORE_1WEEK_REMINDER; 
        		selectedReminderMinutes = EVENT_BEFORE_1WEEK_REMINDER_MINUTES;		
        		break;
        	
        	default:        		
        		return;
        	}
        	
        	if (mSelectedFirstReminderItemLayoutOfMainPage) {        		
        		
        		int prvFirstReminderMinutes = EVENT_NO_REMINDER_MINUTES;
        		if (mReminderEntries.size() > 0) {
        			prvFirstReminderMinutes = mReminderEntries.get(0).getMinutes();
            		if (!checkSelectedReminderMinutes(selectedReminderMinutes, prvFirstReminderMinutes)) {            			
    	        		return;
    	        	}   
        		}
        		else {
        			// mReminderEntries�� size�� 0�̶�� ���� ù��° �����δ��� �������� EVENT_NO_REMINDER_MINUTES ��� ���̴�
        			if (!checkSelectedReminderMinutes(selectedReminderMinutes, prvFirstReminderMinutes)) {            			
    	        		return;
    	        	}  
        		}
        			        		        	
	        	detachReminderPageItemSelectionIcon(prvFirstReminderMinutes, !mEditEvent.mReminderMinuteValues.contains(prvFirstReminderMinutes)); // ������ reminder done icon�� �����Ѵ�
	        		       
	        	processFirstReminderItemPage(selectedReminderMinutes, selectedUnsupportedReminderMinutes);	        	
        	}
        	else {
        		int prvSecondReminderMinutes = EVENT_NO_REMINDER_MINUTES;
        		if (mReminderEntries.size() > 1) {
        			prvSecondReminderMinutes = mReminderEntries.get(1).getMinutes();
            		if (!checkSelectedReminderMinutes(selectedReminderMinutes, prvSecondReminderMinutes)) {            			
    	        		return;
    	        	}   
        		}
        		else if (mReminderEntries.size() == 1){
        			// mReminderEntries�� size�� 1�̶�� ���� �ι�° �����δ��� �������� EVENT_NO_REMINDER_MINUTES�̴�
        			if (!checkSelectedReminderMinutes(selectedReminderMinutes, prvSecondReminderMinutes)) {            			
    	        		return;
    	        	}  
        		}
        		else {
        			// mReminderEntries�� size�� 0�̶��, �ι�° �����δ� �������� ���µ� ���� ���� ������ �߻��� �� ���� ��Ȳ�̴�
        			// :�ι�° �����δ� �������� ù��° �����δ� �������� ���� EVENT_NO_REMINDER_MINUTES�� �ƴ� ������ �����Ǿ��� ������ ���µǴ� ���̴�!!!
        		}        		
	        	
	        	detachReminderPageItemSelectionIcon(prvSecondReminderMinutes, !mEditEvent.mReminderMinuteValues.contains(prvSecondReminderMinutes)); // ������ reminder done icon�� �����Ѵ�
	        		   
	        	processSecondReminderItemPage(selectedReminderMinutes, selectedUnsupportedReminderMinutes);	        	
        	}        	
        	       	
        	mEditEvent.switchMainView();
        	mEditEvent.setMainPageActionBar();

        }
    };
    
    public void processFirstReminderItemPage(int selectedReminderMinutes, boolean selectedUnsupportedReminderMinutes) {
    	if (selectedReminderMinutes == EVENT_NO_REMINDER_MINUTES) {
    		
    		mReminderEntries.remove(0);
    		
    		// �ι�° �˸��� EVENT_NO_REMINDER�� ��� �ι�° �˸� ������ ���̾ƿ��� �����Ѵ�
    		// :�ι�° �˸��� EVENT_NO_REMINDER �̶�� ����
    		//  mReminderEntries.remove(0);�� �ϱ� ���� reminder�� ���� �ϳ����ٴ� ���� �ǹ���
    		if (mReminderEntries.size() > 0) {
    			// �ι�° �˸��� EVENT_NO_REMINDER�� �ƴϸ�
        		// ù��° �˸��� �ι�° �˸��� ������ �����ϰ�
        		// �ι�° �˸��� ����° �˸��� �ִٸ� ����° �˸� ���� �����ϰ� 
    			// �ƴϸ�, EVENT_NO_REMINDER�� �����Ѵ�
    			      				        	
    			selectedReminderMinutes = mReminderEntries.get(0).getMinutes(); // 
				
				int firstReminderMinutes = selectedReminderMinutes;
				selectedReminderMinutes = updateSecondReminderItemLayout(firstReminderMinutes);				
				selectedUnsupportedReminderMinutes = !mEditEvent.mReminderMinuteValues.contains(selectedReminderMinutes);				
    		}	        		
    		else {
    			mFragment.mEditEvent.mBetweenFristReminderAndSecondReminderLayout.setVisibility(View.GONE);
    			mFragment.mEditEvent.mSecondReminderLayout.setVisibility(View.GONE);		          			
    		}
    	}
    	else {
    		
    		if (mReminderEntries.size() > 0) { // mReminderEntries�� ù��° ���� �����ؾ� �Ѵ�
        		ReminderEntry prvFirstReminder = mReminderEntries.get(0);	        		
        		ReminderEntry newFirstReminder = ReminderEntry.valueOf(selectedReminderMinutes, prvFirstReminder.getMethod());
        		mReminderEntries.set(0, newFirstReminder);		
        		
        		int firstReminderMinutes = selectedReminderMinutes;
				selectedReminderMinutes = updateSecondReminderItemLayout(firstReminderMinutes);				
				selectedUnsupportedReminderMinutes = !mEditEvent.mReminderMinuteValues.contains(selectedReminderMinutes);				
    		}
    		else { // mReminderEntries�� 0��° �� �����δ��� �����ؼ� �߰��ؾ� �Ѵ�
    			ReminderEntry newFirst = ReminderEntry.valueOf(selectedReminderMinutes, Reminders.METHOD_DEFAULT);
    			mReminderEntries.add(newFirst);
    		}
    		
    		mFragment.mEditEvent.mBetweenFristReminderAndSecondReminderLayout.setVisibility(View.VISIBLE);
    		mFragment.mEditEvent.mSecondReminderLayout.setVisibility(View.VISIBLE);
    	}
    		        	
    	// ���࿡ selectedUnsupportedReminderMinutes�� true�̰�
    	// ������ mUnsupportedReminderLayout�� GONE ���¶��?...
    	
    	attachReminderPageItemSelectionIcon(selectedReminderMinutes, selectedUnsupportedReminderMinutes); 
    	updateReminderItemLayoutText(selectedReminderMinutes, mFragment.mEditEvent.mEventFirstReminderText, selectedUnsupportedReminderMinutes);	
    }
    
    public void processSecondReminderItemPage(int selectedReminderMinutes, boolean selectedUnsupportedReminderMinutes) {
    	int secondReminderMinutes = selectedReminderMinutes;
    	if (secondReminderMinutes == EVENT_NO_REMINDER_MINUTES) {
    		//if (mReminderEntries.size() > 1)
    			mReminderEntries.remove(1);	        		
    		
    		// mReminderEntries.remove(1)�� ������ �� �߻��� ���� ���׿� ���� ó������ �Ѵ�
    		if (mReminderEntries.size() > 1) { // 1���� ũ�ٴ� ���� �ߺ��� �����δ����� �ִٰ� ġ���� �����δ��� �ּ� 2�� �̻��̶�� ���̴�
    			int firstReminderMinutes = checkValidReminders();
    			
    			updateFirstReminderItemLayout(firstReminderMinutes);
    			
    			secondReminderMinutes = getSecondReminderMinutes(); 
    			selectedUnsupportedReminderMinutes = !mEditEvent.mReminderMinuteValues.contains(secondReminderMinutes);    				
    		} 
    		//else {
    			// �̷� ��Ȳ�� �߻��� �� �ִ°�?
    		    // :ù��° �����δ��� �����ϰ� �ι�° �����δ��� EVENT_NO_REMINDER_MINUTES�� ���õǾ��ٴ� ���� ���� �� ���� �� ������...
    		    //  �ֳ��ϸ� ù��° �����δ��� �����Ѵٸ� onClic�� �߻��ϱ� �� prvSecondReminderMinutes = EVENT_NO_REMINDER_MINUTES; �̱� ������
    		    //  onClick���� �ٷ� ���ϵǱ� �����̴�!!!
    		//}
    	}
    	else {
    		// mReminderEntries�� �ι�° ���� �����ؾ� �Ѵ�
    		if (mReminderEntries.size() > 1) {
    			ReminderEntry prvSecond = mReminderEntries.get(1);	        			
    			ReminderEntry newSecond = ReminderEntry.valueOf(secondReminderMinutes, prvSecond.getMethod());
    			mReminderEntries.set(1, newSecond);	  
    			
    			int firstReminderMinutes = checkValidReminders();
    			
    			updateFirstReminderItemLayout(firstReminderMinutes);
				
    			secondReminderMinutes = getSecondReminderMinutes(); 
    			selectedUnsupportedReminderMinutes = !mEditEvent.mReminderMinuteValues.contains(secondReminderMinutes);    			
    		}
    		else {
    			// mReminderEntries�� size�� 1�� ���ٸ�, �ι�° �����δ� ���� �߰��ؾ� �Ѵ�
    			// �̰��� prvSecondReminderMinutes�� ���� EVENT_NO_REMINDER_MINUTES ���ٴ� ���� �ǹ��Ѵ�!!!
    			ReminderEntry newSecond = ReminderEntry.valueOf(secondReminderMinutes, Reminders.METHOD_DEFAULT);
    			mReminderEntries.add(1, newSecond);
    			
    			int firstReminderMinutes = checkValidReminders();
    			
    			updateFirstReminderItemLayout(firstReminderMinutes);
				
    			secondReminderMinutes = getSecondReminderMinutes(); 
    			selectedUnsupportedReminderMinutes = !mEditEvent.mReminderMinuteValues.contains(secondReminderMinutes);    			
    		}
    	}   
    	
    	// �ι�° ���� �������� �����δ� ���̾ƿ��� GONE ���·� ����� ��쵵 ������ �ʴ°�??? 
    	// :���� ù��° ���� �������� �����δ� ���̾ƿ��� EVENT_NO_REMINDER_MINUTES��� �׷��� �ؾ��ϴµ�................
    	//  �ι�° ���� �������� �����δ� ���̾ƿ��� �����Ѵٴ� ����,
    	//  ù��° ���� �������� �����δ� ���̾ƿ��� EVENT_NO_REMINDER_MINUTES�� �ƴ϶�� ���̴�
    	//  �׷��Ƿ� else �������� �ι�° ���� �������� �����δ� ���̾ƿ��� ���¸� �Ű� �� �ʿ䰡 ����
    	
    	attachReminderPageItemSelectionIcon(secondReminderMinutes, selectedUnsupportedReminderMinutes); 
    	updateReminderItemLayoutText(secondReminderMinutes, mFragment.mEditEvent.mEventSecondReminderText, selectedUnsupportedReminderMinutes);	
    }
    
    public boolean checkSelectedReminderMinutes(int selectedReminderMinutes, int prvReminderMinutes) {
    	if (selectedReminderMinutes == prvReminderMinutes) {
			mEditEvent.switchMainView();
        	mEditEvent.setMainPageActionBar();
    		return false;
    	}  
    	
    	return true;
    }
    
    public int checkValidReminders() {
    	Collections.sort(mReminderEntries);  // ������������ �����Ѵ�
    	Collections.reverse(mReminderEntries); // reverse�� ���������ν� ������������ ���ĵȴ�
    	
    	int prvMinutes = 0xFFFFFFFF;
    	int size = mReminderEntries.size();
    	for (int i=0; i<size; i++/*ReminderEntry re : mReminderEntries*/) {
    		ReminderEntry re = mReminderEntries.get(i);
    		int minutes = re.getMinutes();
    		if (prvMinutes == minutes) {
    			mReminderEntries.remove(i);
    			size = mReminderEntries.size();
    		}
    		
    		prvMinutes = minutes;
    	}
    	
    	int firstReminderMinutes = mReminderEntries.get(0).getMinutes(); // 
    	return firstReminderMinutes;
    }
    
    public void updateFirstReminderItemLayout(int firstReminderMinutes) {
    	//int firstReminderMinutesAfterCheck = mReminderEntries.get(0).getMinutes(); // 
		boolean isUnsupportedFirstReminder = !mEditEvent.mReminderMinuteValues.contains(firstReminderMinutes);
		updateReminderItemLayoutText(firstReminderMinutes, mFragment.mEditEvent.mEventFirstReminderText, isUnsupportedFirstReminder);	
    }
    
    public int updateSecondReminderItemLayout(int firstReminderMinutes) {
    	
    	int secondReminderMinutes;
		if (mReminderEntries.size() > 1) {
			firstReminderMinutes = checkValidReminders();                	
			
			secondReminderMinutes = getSecondReminderMinutes();    							
		}
		else {	  
			// mReminderEntries�� size�� 1�� ��Ȳ�̴�
			// :�׷��ٸ� �ι�° ���� �������� �����δ� ���̾ƿ��� GONE ���°� �ƴ� VISIBLE ���¿��� '����'�̶�� �ؽ�Ʈ�� ǥ���ؾ� �Ѵ�
			secondReminderMinutes = EVENT_NO_REMINDER_MINUTES;	        				
		}
		        	
		boolean isUnsupportedSecondReminderMinutes = !mEditEvent.mReminderMinuteValues.contains(secondReminderMinutes);
		updateReminderItemLayoutText(secondReminderMinutes, mFragment.mEditEvent.mEventSecondReminderText, isUnsupportedSecondReminderMinutes);	
		
		return firstReminderMinutes;
    }
    
    public int getSecondReminderMinutes() {
    	int secondReminderMinutes = EVENT_NO_REMINDER_MINUTES;
    	if (mReminderEntries.size() > 1) {
			//ReminderEntry secondReminderAfterCheck = mReminderEntries.get(1);	   
			//secondReminderMinutes = secondReminderAfterCheck.getMinutes();
			
			secondReminderMinutes = mReminderEntries.get(1).getMinutes();	
		} 
		
    	return secondReminderMinutes;
    }
    
    public void updateReminderItemLayoutText(int minutes, TextView tv, boolean unsupportedReminderMinutes) {			
		
		switch(minutes) {
		case EVENT_NO_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_no);				
			break;			
		case EVENT_EVENT_THEN_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_eventthen);
			break;			
		case EVENT_BEFORE_5MIN_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_5min);
			break;			
		case EVENT_BEFORE_15MIN_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_15min);
			break;			
		case EVENT_BEFORE_30MIN_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_30min);
			break;			
		case EVENT_BEFORE_1H_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_1hour);
			break;
		case EVENT_BEFORE_2H_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_2hour);
			break;
		case EVENT_BEFORE_1DAY_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_1day);
			break;
		case EVENT_BEFORE_2DAY_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_2day);
			break;
		case EVENT_BEFORE_1WEEK_REMINDER_MINUTES:
			tv.setText(R.string.event_reminder_before_1week);
			break;		
		default:
			if (unsupportedReminderMinutes) {
				tv.setText(String.valueOf(minutes));	
			}
			
			break;
		}
		
	}
    
    
    public void attachReminderPageItemSelectionIcon(int minutes, boolean unsupportedReminderMinutes) {
		switch(minutes) {
		case EVENT_NO_REMINDER_MINUTES:
			mNoReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;			
		case EVENT_EVENT_THEN_REMINDER_MINUTES:
			mEventThenReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;			
		case EVENT_BEFORE_5MIN_REMINDER_MINUTES:
			mBefore5MinReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;			
		case EVENT_BEFORE_15MIN_REMINDER_MINUTES:
			mBefore15MinReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;			
		case EVENT_BEFORE_30MIN_REMINDER_MINUTES:
			mBefore30MinReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;			
		case EVENT_BEFORE_1H_REMINDER_MINUTES:
			mBefore1HReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;
		case EVENT_BEFORE_2H_REMINDER_MINUTES:
			mBefore2HReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;
		case EVENT_BEFORE_1DAY_REMINDER_MINUTES:
			mBefore1DayReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;
		case EVENT_BEFORE_2DAY_REMINDER_MINUTES:
			mBefore2DayReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;
		case EVENT_BEFORE_1WEEK_REMINDER_MINUTES:
			mBefore1WeekReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			break;		
		default:
			if (unsupportedReminderMinutes) {
				if (mUnsupportedReminderLayout.getVisibility() == View.GONE) {
					mUnsupportedReminderLayout.setVisibility(View.VISIBLE);
				}				
				
				//int prvMinutes = (Integer) mUnsupportedReminderLayout.getTag();
				//if (minutes != prvMinutes) {
					TextView tv = (TextView) mUnsupportedReminderLayout.findViewById(R.id.unsupported_reminder_text);
					tv.setText(String.valueOf(minutes));
					mUnsupportedReminderLayout.setTag(minutes);
				//}
				
				mUnsupportedReminderLayout.addView(mReminderPageItemSelectionIcon, mReminderPageItemSelectionIconImageViewLayoutParams);
			}			
			break;
		}
	}
	
	
	
	public void detachReminderPageItemSelectionIcon(int minutes, boolean unsupportedReminderMinutes) {
		switch(minutes) {
		case EVENT_NO_REMINDER_MINUTES:
			mNoReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;			
		case EVENT_EVENT_THEN_REMINDER_MINUTES:
			mEventThenReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;			
		case EVENT_BEFORE_5MIN_REMINDER_MINUTES:
			mBefore5MinReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;			
		case EVENT_BEFORE_15MIN_REMINDER_MINUTES:
			mBefore15MinReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;			
		case EVENT_BEFORE_30MIN_REMINDER_MINUTES:
			mBefore30MinReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;			
		case EVENT_BEFORE_1H_REMINDER_MINUTES:
			mBefore1HReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;
		case EVENT_BEFORE_2H_REMINDER_MINUTES:
			mBefore2HReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;
		case EVENT_BEFORE_1DAY_REMINDER_MINUTES:
			mBefore1DayReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;
		case EVENT_BEFORE_2DAY_REMINDER_MINUTES:
			mBefore2DayReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;
		case EVENT_BEFORE_1WEEK_REMINDER_MINUTES:
			mBefore1WeekReminderLayout.removeView(mReminderPageItemSelectionIcon);
			break;
		default:
			if (unsupportedReminderMinutes) {
				mUnsupportedReminderLayout.removeView(mReminderPageItemSelectionIcon);
			}			
			break;
		}
	}
    
	ArrayList<ReminderEntry> mReminderEntries = new ArrayList<ReminderEntry>();
	public ArrayList<ReminderEntry> getReminderEntries() {
		return mReminderEntries;
	}
}
