package com.intheeast.settings;

import java.util.ArrayList;
import java.util.Arrays;

import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AlertTimeOverrideSubView extends LinearLayout {
	
	public static final int BIRTHDAY_ALERTTIME_TYPE = 1;
	public static final int EVENT_ALERTTIME_TYPE = 2;
	public static final int ALLDAYEVEENT_ALERTTIME_TYPE = 3;
	
	public static final int BIRTHDAY_ALERTTIME_SUBPAGE = 1;
	public static final int EVENT_ALERTTIME_SUBPAGE = 2;
	public static final int ALLDAYEVEENT_ALERTTIME_SUBPAGE = 3;
	
	Context mContext;
	LayoutInflater mInflater;
	Resources mResources;
	
	LockableScrollView	mScrollView;
	LinearLayout mContainer;	
	
	public ArrayList<CommonRelativeLayoutItemContainer> mBirthDayEventAlertTimeItems = new ArrayList<CommonRelativeLayoutItemContainer>();
	public ArrayList<CommonRelativeLayoutItemContainer> mEventAlertTimeItems = new ArrayList<CommonRelativeLayoutItemContainer>();	
	public ArrayList<CommonRelativeLayoutItemContainer> mAllDayEventAlertTimeItems = new ArrayList<CommonRelativeLayoutItemContainer>();
	
	RelativeLayout mFirstSeperator;
	RelativeLayout mSpecialEventPageSurplusRegionView;
	RelativeLayout mEventPageSurplusRegionView;
	
	Drawable mSelectedBirthDayEventAlertTimeMarkDrawable;
	Drawable mSelectedEventAlertTimeMarkDrawable;
	Drawable mSelectedAllDayEventAlertTimeMarkDrawable;
	
	ImageView mSelectedBirthDayEventAlertTimeMakrIcon;
	ImageView mSelectedEventAlertTimeMakrIcon;
	ImageView mSelectedAllDayEventAlertTimeMakrIcon;
	
	private ArrayList<String> mBirthDayEventReminderMinuteValues;
	private ArrayList<String> mBirthDayEventReminderMinuteLabels;
	
	private ArrayList<String> mEventReminderMinuteValues;
	private ArrayList<String> mEventReminderMinuteLabels;
	
	private ArrayList<String> mAllDayEventReminderMinuteValues;
	private ArrayList<String> mAllDayEventReminderMinuteLabels;
	
	private OnDefaultBirthDayEventAlertTimeSetListener mOnDefaultBirthDayEventAlertTimeSetListener;
	private OnDefaultEventAlertTimeSetListener mDefaultEventAlertTimeSetListener;
	private OnDefaultAllDayEventAlertTimeSetListener mOnDefaultAllDayEventAlertTimeSetListener;
	
	public interface OnDefaultBirthDayEventAlertTimeSetListener {
	    void OnDefaultBirthDayEventAlertTimeSet(Object newValue);
	}
	
	public interface OnDefaultEventAlertTimeSetListener {
	    void OnDefaultEventAlertTimeSet(Object newValue);
	}
	
	public interface OnDefaultAllDayEventAlertTimeSetListener {
	    void OnDefaultAllDayEventAlertTimeSet(Object newValue);
	}
	
	public void setOnDefaultBirthDayEventAlertTimeSetListener(OnDefaultBirthDayEventAlertTimeSetListener l) {
		mOnDefaultBirthDayEventAlertTimeSetListener = l;
	}
	
	public void setOnDefaultEventAlertTimeSetListener(OnDefaultEventAlertTimeSetListener l) {
	    mDefaultEventAlertTimeSetListener = l;
	}
	
	public void setOnDefaultAllDayEventAlertTimeSetListener(OnDefaultAllDayEventAlertTimeSetListener l) {
		mOnDefaultAllDayEventAlertTimeSetListener = l;
	}
	
	OnClickListener mCalendarItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
			boolean mustSwitchToMain = false;
			AlertTimeItemInfo tagObj = (AlertTimeItemInfo) v.getTag();
			
			switch(tagObj.alertTimeType) {
			case BIRTHDAY_ALERTTIME_TYPE:
				String currentBirthDayEventAlertTime = mMainPane.mDefaultBirthDayEventReminder.getValue();
				if (!currentBirthDayEventAlertTime.equals(tagObj.alertTimeValue)) {
					// ���� ��ũ�� �����̳ʿ��� ��ũ�� �����ؾ� �ϰ�
					int prvValueIndex = mMainPane.mDefaultBirthDayEventReminder.getValueIndex();
					CommonRelativeLayoutItemContainer prvItemContainer = mBirthDayEventAlertTimeItems.get(prvValueIndex);
					prvItemContainer.removeView(mSelectedBirthDayEventAlertTimeMakrIcon);
					
					mOnDefaultBirthDayEventAlertTimeSetListener.OnDefaultBirthDayEventAlertTimeSet(tagObj.alertTimeValue);
					
					// �ش� �����̳ʿ� ��ũ�� �߰��ؾ� �Ѵ�
					int newValueIndex = mMainPane.mDefaultBirthDayEventReminder.getValueIndex();
					CommonRelativeLayoutItemContainer curItemContainer = mBirthDayEventAlertTimeItems.get(newValueIndex);
					curItemContainer.addView(mSelectedBirthDayEventAlertTimeMakrIcon);
					
					mMainView.configSelectedBirthDayEventAlertTimeText();
					mustSwitchToMain = true;
				}
				break;
			case EVENT_ALERTTIME_TYPE:
				String currentEventAlertTime = mMainPane.mDefaultEventReminder.getValue();
				if (!currentEventAlertTime.equals(tagObj.alertTimeValue)) {
					// ���� ��ũ�� �����̳ʿ��� ��ũ�� �����ؾ� �ϰ�
					int prvValueIndex = mMainPane.mDefaultEventReminder.getValueIndex();
					CommonRelativeLayoutItemContainer prvItemContainer = mEventAlertTimeItems.get(prvValueIndex);
					prvItemContainer.removeView(mSelectedEventAlertTimeMakrIcon);
					
					mDefaultEventAlertTimeSetListener.OnDefaultEventAlertTimeSet(tagObj.alertTimeValue);
					
					// �ش� �����̳ʿ� ��ũ�� �߰��ؾ� �Ѵ�
					int newValueIndex = mMainPane.mDefaultEventReminder.getValueIndex();
					CommonRelativeLayoutItemContainer curItemContainer = mEventAlertTimeItems.get(newValueIndex);
					curItemContainer.addView(mSelectedEventAlertTimeMakrIcon);
					
					mMainView.configSelectedEventAlertTimeText();
					mustSwitchToMain = true;
				}				
				break;
			case ALLDAYEVEENT_ALERTTIME_TYPE:
				String currentAllDayEventAlertTime = mMainPane.mDefaultAllDayEventReminder.getValue();
				if (!currentAllDayEventAlertTime.equals(tagObj.alertTimeValue)) {
					// ���� ��ũ�� �����̳ʿ��� ��ũ�� �����ؾ� �ϰ�
					int prvValueIndex = mMainPane.mDefaultAllDayEventReminder.getValueIndex();
					CommonRelativeLayoutItemContainer prvItemContainer = mAllDayEventAlertTimeItems.get(prvValueIndex);
					prvItemContainer.removeView(mSelectedAllDayEventAlertTimeMakrIcon);
					
					mOnDefaultAllDayEventAlertTimeSetListener.OnDefaultAllDayEventAlertTimeSet(tagObj.alertTimeValue);
					
					// �ش� �����̳ʿ� ��ũ�� �߰��ؾ� �Ѵ�
					int newValueIndex = mMainPane.mDefaultAllDayEventReminder.getValueIndex();
					CommonRelativeLayoutItemContainer curItemContainer = mAllDayEventAlertTimeItems.get(newValueIndex);
					curItemContainer.addView(mSelectedAllDayEventAlertTimeMakrIcon);
					
					mMainView.configSelectedAllDayEventAlertTimeText();
					mustSwitchToMain = true;
				}				
				break;
			}
			
			// icon�� ���ϱ�? ����???
			// �׷��ٸ� ���� item���� icon�� �����ؾ� �Ѵ�
			// main pane���� switching �ؾ� ��
			if (mustSwitchToMain) {
				mMainPane.switchMainView();
			}
		}
		
	};
	
	public AlertTimeOverrideSubView(Context context) {
		super(context);
		mContext = context;
	}
	
	public AlertTimeOverrideSubView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public AlertTimeOverrideSubView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	AlertTimeSettingPane mMainPane;
	AlertTimeOverrideView mMainView;
	public void init(AlertTimeSettingPane parent, AlertTimeOverrideView mainView, int frameLayoutHeight) {
		mMainPane = parent;
		mMainView = mainView;
		
		mResources = getResources();
		mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mScrollView = (LockableScrollView) findViewById(R.id.alerttime_setting_subpage_scrollview);
		mContainer = (LinearLayout) mScrollView.findViewById(R.id.alerttime_setting_subpage_items_container);
		
		mFirstSeperator = (RelativeLayout) mInflater.inflate(R.layout.common_seperator_layout, null);
		mSpecialEventPageSurplusRegionView = (RelativeLayout) mInflater.inflate(R.layout.surplus_region_layout, null);
		View specialEventPageShapeView = mSpecialEventPageSurplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams specialEventPageShapeViewParams = (RelativeLayout.LayoutParams) specialEventPageShapeView.getLayoutParams();
		int specialEventPageSurplusRegionHeight = calcMainViewSurplusRegion(mResources, frameLayoutHeight, 1, 5);
		specialEventPageShapeViewParams.height = specialEventPageSurplusRegionHeight;
		specialEventPageShapeView.setLayoutParams(specialEventPageShapeViewParams);
		
		
		mEventPageSurplusRegionView = (RelativeLayout) mInflater.inflate(R.layout.surplus_region_layout, null);
		View eventPageShapeView = mEventPageSurplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams eventPageShapeViewParams = (RelativeLayout.LayoutParams) eventPageShapeView.getLayoutParams();
		int eventPageSurplusRegionHeight = calcMainViewSurplusRegion(mResources, frameLayoutHeight, 1, 10);
		eventPageShapeViewParams.height = eventPageSurplusRegionHeight;
		eventPageShapeView.setLayoutParams(eventPageShapeViewParams);		
		
		
		makeItemLabelAndValues();
		
        makeItemViews();
        
        makeSelectedMarkIcon();        
	}
	
	public void makeItemLabelAndValues() {
		makeBirthDayEventAlertTimeItemLabelAndValues();
		makeEventAlertTimeItemLabelAndValues();	
		makeAllDayEventAlertTimeItemLabelAndValues();
	}
	
	
	public void makeBirthDayEventAlertTimeItemLabelAndValues() {
		mBirthDayEventReminderMinuteValues = loadStringArray(mResources, R.array.default_special_event_reminder_values);
		
        mBirthDayEventReminderMinuteLabels = loadStringArray(mResources, R.array.default_special_event_reminder_labels);
	}
	
	public void makeEventAlertTimeItemLabelAndValues() {
		mEventReminderMinuteValues = loadStringArray(mResources, R.array.ecalendar_default_reminder_values);
		
        mEventReminderMinuteLabels = loadStringArray(mResources, R.array.ecalendar_default_reminder_labels);
	}	
	
	
	public void makeAllDayEventAlertTimeItemLabelAndValues() {
		mAllDayEventReminderMinuteValues = loadStringArray(mResources, R.array.default_special_event_reminder_values);
		
        mAllDayEventReminderMinuteLabels = loadStringArray(mResources, R.array.default_special_event_reminder_labels);
	}	
	
	
	public void makeItemViews() {
		makeBirthDayEventAlertTimeItemViews();
		makeEventAlertTimeItemViews();
		makeAllDayEventAlertTimeItemViews();
	}
	
	public void makeBirthDayEventAlertTimeItemViews() {
		int arraySize = mBirthDayEventReminderMinuteValues.size();
		int lastIndex = arraySize - 1;
		for (int i=0; i<arraySize; i++) {
			//int itemValue = mBirthDayEventReminderMinuteValues.get(i);
			String itemValue = mBirthDayEventReminderMinuteValues.get(i);
			String itemLabel = mBirthDayEventReminderMinuteLabels.get(i);
			CommonRelativeLayoutItemContainer itemContainer;
			if (i == lastIndex) {
				itemContainer = makeItemContainer(BIRTHDAY_ALERTTIME_TYPE, itemLabel, itemValue, true);
			}
			else
				itemContainer = makeItemContainer(BIRTHDAY_ALERTTIME_TYPE, itemLabel, itemValue, false);
			
			mBirthDayEventAlertTimeItems.add(i, itemContainer);
		}
	}
	
	public void makeEventAlertTimeItemViews() {
		int arraySize = mEventReminderMinuteValues.size();
		int lastIndex = arraySize - 1;
		for (int i=0; i<arraySize; i++) {
			//int itemValue = mEventReminderMinuteValues.get(i);
			String itemValue = mEventReminderMinuteValues.get(i);
			String itemLabel = mEventReminderMinuteLabels.get(i);
			CommonRelativeLayoutItemContainer itemContainer;
			if (i == lastIndex) {
				itemContainer = makeItemContainer(EVENT_ALERTTIME_TYPE, itemLabel, itemValue, true);
			}
			else
				itemContainer = makeItemContainer(EVENT_ALERTTIME_TYPE, itemLabel, itemValue, false);
						
			mEventAlertTimeItems.add(i, itemContainer);
		}
	}
	
	public void makeAllDayEventAlertTimeItemViews() {
		int arraySize = mAllDayEventReminderMinuteValues.size();
		int lastIndex = arraySize - 1;
		for (int i=0; i<arraySize; i++) {
			//int itemValue = mAllDayEventReminderMinuteValues.get(i);
			String itemValue = mAllDayEventReminderMinuteValues.get(i);
			String itemLabel = mAllDayEventReminderMinuteLabels.get(i);
			CommonRelativeLayoutItemContainer itemContainer;
			if (i == lastIndex) {
				itemContainer = makeItemContainer(ALLDAYEVEENT_ALERTTIME_TYPE, itemLabel, itemValue, true);
			}
			else
				itemContainer = makeItemContainer(ALLDAYEVEENT_ALERTTIME_TYPE, itemLabel, itemValue, false);
						
			mAllDayEventAlertTimeItems.add(i, itemContainer);
		}
	}

	int mCurrentPageCase = -1;
	
	public int getCurrentSubPage() {
		return mCurrentPageCase;
	}
	public void configPage(int pageCase) {
		
		if (mContainer.getChildCount() != 0)
			mContainer.removeAllViews();
		
		mContainer.addView(mFirstSeperator);
		int arraySize = 0;
		
		switch(pageCase) {
		case BIRTHDAY_ALERTTIME_SUBPAGE:			
			mCurrentPageCase = BIRTHDAY_ALERTTIME_SUBPAGE;
			String birthDayEventAlertTimeValue = mMainPane.mDefaultBirthDayEventReminder.getValue();
			
			arraySize = mBirthDayEventAlertTimeItems.size();
			for (int i=0; i<arraySize; i++) {
				CommonRelativeLayoutItemContainer itemContainer = mBirthDayEventAlertTimeItems.get(i);
				AlertTimeItemInfo tagObj = (AlertTimeItemInfo) itemContainer.getTag();
				if (birthDayEventAlertTimeValue.equals(tagObj.alertTimeValue)) {					
					if (itemContainer.findViewById(R.id.selected_birthday_event_alert_time_mark_icon) == null)
						itemContainer.addView(mSelectedBirthDayEventAlertTimeMakrIcon);
				}
				
				mContainer.addView(itemContainer);
			}
			
			break;
		case EVENT_ALERTTIME_SUBPAGE:
			mCurrentPageCase = EVENT_ALERTTIME_SUBPAGE;
			String eventAlertTimeValue = mMainPane.mDefaultEventReminder.getValue();
			
			arraySize = mEventAlertTimeItems.size();
			for (int i=0; i<arraySize; i++) {
				CommonRelativeLayoutItemContainer itemContainer = mEventAlertTimeItems.get(i);
				AlertTimeItemInfo tagObj = (AlertTimeItemInfo) itemContainer.getTag();
				if (eventAlertTimeValue.equals(tagObj.alertTimeValue)) {
					if (itemContainer.findViewById(R.id.selected_event_alert_time_mark_icon) == null)
						itemContainer.addView(mSelectedEventAlertTimeMakrIcon);					
				}
				
				mContainer.addView(itemContainer);
			}
			
			break;
		case ALLDAYEVEENT_ALERTTIME_SUBPAGE:
			mCurrentPageCase = ALLDAYEVEENT_ALERTTIME_SUBPAGE;	
			String allDayEventAlertTimeValue = mMainPane.mDefaultAllDayEventReminder.getValue();
			
			arraySize = mAllDayEventAlertTimeItems.size();
			for (int i=0; i<arraySize; i++) {
				CommonRelativeLayoutItemContainer itemContainer = mAllDayEventAlertTimeItems.get(i);
				AlertTimeItemInfo tagObj = (AlertTimeItemInfo) itemContainer.getTag();
				if (allDayEventAlertTimeValue.equals(tagObj.alertTimeValue)) {
					if (itemContainer.findViewById(R.id.selected_allday_event_alert_time_mark_icon) == null)
						itemContainer.addView(mSelectedAllDayEventAlertTimeMakrIcon);						
				}
				
				mContainer.addView(itemContainer);
			}
			
			break;
		}
		
		// Add SurplusRegion View
		switch(pageCase) {
		case BIRTHDAY_ALERTTIME_SUBPAGE:		
		case ALLDAYEVEENT_ALERTTIME_SUBPAGE:
			mContainer.addView(mSpecialEventPageSurplusRegionView);
			break;
			
		case EVENT_ALERTTIME_SUBPAGE:
			mContainer.addView(mEventPageSurplusRegionView);		
			break;		
		}		
	}
	
	public CommonRelativeLayoutItemContainer makeItemContainer(int alertTimeType, String ItemLabel, String alertTimeValue, boolean last) {
		
		int paddingLeft = (int) mResources.getDimension(R.dimen.commonItemViewPaddingLeft); //commonItemViewPaddingLeft
		int paddingRight = (int) mResources.getDimension(R.dimen.commonItemViewPaddingRight); //commonItemViewPaddingRight
		int customPaddingLeft = 0;
		if (!last)
			customPaddingLeft = (int) mResources.getDimension(R.dimen.commonItemViewPaddingLeft); 
				
		CommonRelativeLayoutItemContainer itemContainer = 
        		new CommonRelativeLayoutItemContainer(mContext, customPaddingLeft, CommonRelativeLayoutItemContainer.LOWER_BOLDER_LINE_POSITION);
        
        int itemContainerHeight = (int) mResources.getDimension(R.dimen.selectCalendarViewItemHeight);
        RelativeLayout.LayoutParams itemConatainerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemContainerHeight);
        itemContainer.setLayoutParams(itemConatainerLayoutParams);
        itemContainer.setPadding(paddingLeft, 0, paddingRight, 0);        
        itemContainer.setBackground(mResources.getDrawable(R.drawable.common_layout_selector));
        itemContainer.setClickable(true);
        
        AlertTimeItemInfo tagObj = new AlertTimeItemInfo(alertTimeType, alertTimeValue);
        itemContainer.setTag(tagObj);       
        
		itemContainer.setOnClickListener(mCalendarItemClickListener);
        
        itemContainer.addView(makeItemTextView(ItemLabel));
        
        return itemContainer;
	}
	
	public TextView makeItemTextView(String ItemLabel) {		
		
		TextView ItemLableText = new TextView(mContext);
		RelativeLayout.LayoutParams ItemLableTextParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		ItemLableTextParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		ItemLableTextParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);			
		ItemLableText.setLayoutParams(ItemLableTextParams);
			
		int textColor = mResources.getColor(R.color.event_layout_label_color);		
		ItemLableText.setTextColor(textColor);	
		float textSPSize =  mResources.getDimension(R.dimen.selectCalendarViewCommonTextSize) / mResources.getDisplayMetrics().scaledDensity;
		ItemLableText.setTextSize(textSPSize);
		ItemLableText.setText(ItemLabel);
				
		return ItemLableText;
	}
	
	private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }
	
	
	public void makeSelectedMarkIcon() {	
		
		mSelectedBirthDayEventAlertTimeMarkDrawable = mResources.getDrawable(R.drawable.ic_menu_done_holo_light);
		int irColor = Color.rgb(0, 0, 255);
		mSelectedBirthDayEventAlertTimeMarkDrawable.setColorFilter(new PorterDuffColorFilter(irColor, PorterDuff.Mode.MULTIPLY));
		
		int width = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconWidth);
		RelativeLayout.LayoutParams selectedBirthDayEventAlertTimeMarkIconParams = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		selectedBirthDayEventAlertTimeMarkIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		selectedBirthDayEventAlertTimeMarkIconParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		
		int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconLeftMargin);
		selectedBirthDayEventAlertTimeMarkIconParams.setMargins(0, 0, rightMargin, 0);	
		
		mSelectedBirthDayEventAlertTimeMakrIcon = new ImageView(mContext);
		mSelectedBirthDayEventAlertTimeMakrIcon.setLayoutParams(selectedBirthDayEventAlertTimeMarkIconParams);		
		mSelectedBirthDayEventAlertTimeMakrIcon.setImageDrawable(mSelectedBirthDayEventAlertTimeMarkDrawable);
		mSelectedBirthDayEventAlertTimeMakrIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		mSelectedBirthDayEventAlertTimeMakrIcon.setId(R.id.selected_birthday_event_alert_time_mark_icon);
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		mSelectedEventAlertTimeMarkDrawable = mResources.getDrawable(R.drawable.ic_menu_done_holo_light);		
		mSelectedEventAlertTimeMarkDrawable.setColorFilter(new PorterDuffColorFilter(irColor, PorterDuff.Mode.MULTIPLY));		
		
		RelativeLayout.LayoutParams selectedEventAlertTimeMarkIconParams = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		selectedEventAlertTimeMarkIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		selectedEventAlertTimeMarkIconParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		selectedEventAlertTimeMarkIconParams.setMargins(0, 0, rightMargin, 0);
		
		mSelectedEventAlertTimeMakrIcon = new ImageView(mContext);
		mSelectedEventAlertTimeMakrIcon.setLayoutParams(selectedEventAlertTimeMarkIconParams);		
		mSelectedEventAlertTimeMakrIcon.setImageDrawable(mSelectedEventAlertTimeMarkDrawable);
		mSelectedEventAlertTimeMakrIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);	
		mSelectedEventAlertTimeMakrIcon.setId(R.id.selected_event_alert_time_mark_icon);
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		mSelectedAllDayEventAlertTimeMarkDrawable = mResources.getDrawable(R.drawable.ic_menu_done_holo_light);		
		mSelectedAllDayEventAlertTimeMarkDrawable.setColorFilter(new PorterDuffColorFilter(irColor, PorterDuff.Mode.MULTIPLY));		
		
		RelativeLayout.LayoutParams selectedAllDayEventAlertTimeMarkIconParams = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		selectedAllDayEventAlertTimeMarkIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		selectedAllDayEventAlertTimeMarkIconParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		selectedAllDayEventAlertTimeMarkIconParams.setMargins(0, 0, rightMargin, 0);
		
		mSelectedAllDayEventAlertTimeMakrIcon = new ImageView(mContext);
		mSelectedAllDayEventAlertTimeMakrIcon.setLayoutParams(selectedAllDayEventAlertTimeMarkIconParams);		
		mSelectedAllDayEventAlertTimeMakrIcon.setImageDrawable(mSelectedAllDayEventAlertTimeMarkDrawable);
		mSelectedAllDayEventAlertTimeMakrIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		mSelectedAllDayEventAlertTimeMakrIcon.setId(R.id.selected_allday_event_alert_time_mark_icon);
	}
	
	public static class AlertTimeItemInfo {
		public int alertTimeType;
		public String alertTimeValue;
		
		public AlertTimeItemInfo(int alertTimeType, String alertTimeValue) {
			this.alertTimeType = alertTimeType;
			this.alertTimeValue = alertTimeValue;			
		}
	}
	
	public int calcMainViewSurplusRegion(Resources res, int frameLayoutHeight, int seperatorCounts, int itemCounts) {
		
		int seperatorHeight = (int)res.getDimension(R.dimen.selectCalendarViewSeperatorHeight);		
		int entireSeperatorHeight = seperatorHeight * seperatorCounts;
		int itemHeight = (int)res.getDimension(R.dimen.selectCalendarViewItemHeight); 
		int entireItemsHeight = itemHeight * itemCounts;
		int surplusRegionHeight = frameLayoutHeight - (entireSeperatorHeight + entireItemsHeight);
		
		// seperatorHeight���� �۴ٸ�?...
		if (surplusRegionHeight < seperatorHeight)
			surplusRegionHeight = seperatorHeight;
		
		return surplusRegionHeight;
	}	
}
