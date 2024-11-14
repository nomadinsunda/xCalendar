package com.intheeast.settings;

import java.util.Calendar;

import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.LockableScrollView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class StartWeekOnSettingView extends LinearLayout {
	private static final String TAG = StartWeekOnSettingView.class.getSimpleName();
	private static boolean INFO = true;
	
	Context mContext;
	Resources mResources;
	
	SettingsFragment mFragment;
	SettingsActionBarFragment mActionBar;
	
	LinearLayout mInnerLinearLayoutOfScrollView;
	CommonRelativeLayoutItemContainer mSaturdayItem;
	CommonRelativeLayoutItemContainer mSundayItem;
	CommonRelativeLayoutItemContainer mMondayItem;
	
	Drawable mSelectedMarkDrawable;
	ImageView mSelectedMakrIcon;
	
	int mFirstDayOfWeek;
	
	public StartWeekOnSettingView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public StartWeekOnSettingView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public StartWeekOnSettingView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	}
	
	int mViewSurplusRegionHeight;
	RelativeLayout mSurplusRegionView;
	public void initView(SettingsFragment fragment, SettingsActionBarFragment actionBar, int frameLayoutHeight) {
		mFragment = fragment;
		mActionBar = actionBar;	
		
		mViewSurplusRegionHeight = calcMainViewSurplusRegion(mContext.getResources(), frameLayoutHeight);
		
		// KEY_WEEK_START_DAY[preferences_week_start_day]�� ������ java.util.Calendar�� ����ϰ� ������,
		// Utils.getFirstDayOfWeek�� java.util.Calendar ���� 
		// android.text.format.Time�� ��ȯ�ؼ� �����Ѵ�
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
		
		LockableScrollView scrollView = (LockableScrollView)findViewById(R.id.settings_start_weekon_scrollview);
		mInnerLinearLayoutOfScrollView = (LinearLayout)scrollView .findViewById(R.id.settings_start_weekon_scrollview_inner_layout);	
		
		mSaturdayItem = (CommonRelativeLayoutItemContainer) mInnerLinearLayoutOfScrollView.findViewById(R.id.saturday_item_container);		
		mSundayItem = (CommonRelativeLayoutItemContainer) mInnerLinearLayoutOfScrollView.findViewById(R.id.sunday_item_container);		
		mMondayItem = (CommonRelativeLayoutItemContainer) mInnerLinearLayoutOfScrollView.findViewById(R.id.monday_item_container);
		
		mSurplusRegionView = (RelativeLayout) mInnerLinearLayoutOfScrollView.findViewById(R.id.settings_start_weekon_view_surplus_region);
		View shapeView = mSurplusRegionView.findViewById(R.id.view_seperator_shape);		
		RelativeLayout.LayoutParams shapeViewParams = (RelativeLayout.LayoutParams) shapeView.getLayoutParams();
		shapeViewParams.height = mViewSurplusRegionHeight;
		shapeView.setLayoutParams(shapeViewParams);
		
		// KEY_WEEK_START_DAY[preferences_week_start_day]�� java.util.Calendar�� ���� ��ٷ� �����ϱ� ����
		// �±׿� java.util.Calendar�� �����Ѵ�
		mSaturdayItem.setTag(Calendar.SATURDAY);
		mSundayItem.setTag(Calendar.SUNDAY);
		mMondayItem.setTag(Calendar.MONDAY);		
		
		mSaturdayItem.setOnClickListener(mItemClickListener);
		mSundayItem.setOnClickListener(mItemClickListener);
		mMondayItem.setOnClickListener(mItemClickListener);
		
		makeSelectedMarkIcon();
		
		switch(mFirstDayOfWeek) {
		case Calendar.SATURDAY:
			mSaturdayItem.addView(mSelectedMakrIcon);
			break;
		case Calendar.SUNDAY:
			mSundayItem.addView(mSelectedMakrIcon);
			break;
		case Calendar.MONDAY:
			mMondayItem.addView(mSelectedMakrIcon);
			break;
		}
	}	
	
	
	OnClickListener mItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
						
			int selectedItemDayAsCalendar = (Integer) v.getTag();
			int selectedItemDay;
			if (selectedItemDayAsCalendar == Calendar.SATURDAY) {
				selectedItemDay = Calendar.SATURDAY;
	        } else if (selectedItemDayAsCalendar == Calendar.MONDAY) {
	        	selectedItemDay = Calendar.MONDAY;
	        } else {
	        	selectedItemDay = Calendar.SUNDAY;
	        }
						
			if (mFirstDayOfWeek == selectedItemDay)
				return;
			else {
				mFirstDayOfWeek = selectedItemDay;
			}
			
			// ���� ������ ���õ� ��ũ�� �����ؾ� �Ѵ�
			if (mSaturdayItem.findViewById(R.id.selected_week_startday_mark_icon) != null) {
				mSaturdayItem.removeView(mSelectedMakrIcon);
			}
			else if (mSundayItem.findViewById(R.id.selected_week_startday_mark_icon) != null) {
				mSundayItem.removeView(mSelectedMakrIcon);
			}
			else if (mMondayItem.findViewById(R.id.selected_week_startday_mark_icon) != null) {
				mMondayItem.removeView(mSelectedMakrIcon);
			}
			
			CommonRelativeLayoutItemContainer selectedItem = (CommonRelativeLayoutItemContainer) v;
			selectedItem.addView(mSelectedMakrIcon);
			
			//if (mCurrentWeekStartDay != -1) {
			    // KEY_WEEK_START_DAY[preferences_week_start_day]�� ������ java.util.Calendar�� ����ϰ� �ִ�
				Utils.setSharedPreference(mContext, SettingsFragment.KEY_WEEK_START_DAY, selectedItemDayAsCalendar);
				mFragment.switchMainView();
			//}
			
		}
	};
	
	
	public void makeSelectedMarkIcon() {	
		
		mSelectedMarkDrawable = mResources.getDrawable(R.drawable.ic_menu_done_holo_light);
		int irColor = Color.rgb(0, 0, 255);
		mSelectedMarkDrawable.setColorFilter(new PorterDuffColorFilter(irColor, PorterDuff.Mode.MULTIPLY));
		
		int width = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconWidth);
		RelativeLayout.LayoutParams selectedMarkIconParams = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		selectedMarkIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		selectedMarkIconParams.addRule(RelativeLayout.CENTER_VERTICAL);	
		
		int rightMargin = (int) mResources.getDimension(R.dimen.selectCalendarViewVisibleStateMarkIconLeftMargin);
		selectedMarkIconParams.setMargins(0, 0, rightMargin, 0);	
		
		mSelectedMakrIcon = new ImageView(mContext);
		mSelectedMakrIcon.setLayoutParams(selectedMarkIconParams);		
		mSelectedMakrIcon.setImageDrawable(mSelectedMarkDrawable);
		mSelectedMakrIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		mSelectedMakrIcon.setId(R.id.selected_week_startday_mark_icon);
	}
	
	public final int MAINVEW_ITEMS_COUNT = 3;
	public int calcMainViewSurplusRegion(Resources res, int frameLayoutHeight) {
		
		int firstSeperatorHeight = (int)res.getDimension(R.dimen.selectCalendarViewSeperatorHeight);		
		int itemHeight = (int)res.getDimension(R.dimen.selectCalendarViewItemHeight); 
		int entireItemsHeight = itemHeight * MAINVEW_ITEMS_COUNT;
		int mainViewSurplusRegionHeight = frameLayoutHeight - (firstSeperatorHeight + entireItemsHeight);
		
		return mainViewSurplusRegionHeight;
	}
}
