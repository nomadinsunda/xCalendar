package com.intheeast.settings;

import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.settings.TimeZoneSettingPane.IMMStatusHandler;
import com.intheeast.timezone.TimeZoneData;

import android.content.Context;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

public class TimeZoneSearchResultListView extends ListView {

	private static final String TAG = "TimeZoneSearchResultListView";
	private static final boolean INFO = true;
	
	Context mContext;
	TimeZoneSearchResultAdapter mAdapter;
	IMMStatusHandler mIMMStatusHandler;
	boolean mSwitchToMainView = false;
	
	public TimeZoneSearchResultListView(Context context) {
		super(context);
		init(context);
	}
	
	public TimeZoneSearchResultListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public TimeZoneSearchResultListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	int mCommonHeight;
	int mHeightByKeyBoardPopuped = 0;
	int mListViewItemHeight;
	public void init(Context context) {
		mContext = context;
		
		int deviceHeight = getResources().getDisplayMetrics().heightPixels;
		int StatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
		int upperActionBarHeight = (int)getResources().getDimension(R.dimen.settings_view_upper_actionbar_height);			
		int timeZoneSearchEditTextViewHeight = (int)getResources().getDimension(R.dimen.settingsViewItemHeight); 
		
		mListViewItemHeight = (int)(getResources().getDimension(R.dimen.settingsTimeZoneSearchResultItemHeight) * 3);
		mCommonHeight = deviceHeight - (StatusBarHeight + upperActionBarHeight + timeZoneSearchEditTextViewHeight);
		
		setGlobalLayoutListener();
	}
	
	TimeZoneOverrideSubView mParentView;
	public TimeZoneSearchResultAdapter setUpList(TimeZoneOverrideSubView parent, TimeZoneData timeZoneData, 
			TimeZoneOverrideSubView.OnHomeTimeZoneSetListener l, IMMStatusHandler handler) {
		mParentView = parent;
		mIMMStatusHandler = handler;
		
		//setOnItemClickListener(this);    	
        // Transparent background on scroll
    	setCacheColorHint(0);
    	//setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        // No dividers
    	setDivider(null);
        // Items are clickable
    	setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
    	setFastScrollEnabled(false); // �����ʿ� �н�Ʈ ��ũ�ѹٰ� �����ȴ� : �н�Ʈ ��ũ�ѿ� ���� ���� ������ �𸥴�
    	setVerticalScrollBarEnabled(false);
    	setClickable(true);
    	//mAgendaListView.setOnScrollListener(this);
    	setFadingEdgeLength(0);
    	
    	mAdapter = new TimeZoneSearchResultAdapter(mContext, this, /*currentSelectedTimeZoneId, */timeZoneData, l, mCommonHeight, mListViewItemHeight);
    	setAdapter(mAdapter);
    	
    	return mAdapter;
	}
	
	public void setGlobalLayoutListener() {		
		ViewTreeObserver VTO = getViewTreeObserver(); 
		VTO.addOnGlobalLayoutListener(mOnGlobalLayoutListener);			
	}
	
	boolean mAddedTimeZoneSearchEditTextChangedListener = false;
	boolean mWasKeyboardOpen = false;
	OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {

		
		@Override
		public void onGlobalLayout() {  
				
			// height�� �����???
			int height = getHeight();
			
			//if (INFO) Log.i(TAG, "*********************************************************");
			
			//if (INFO) Log.i(TAG, "onGlobalLayout:height=" + String.valueOf(height));
			
			if (height == mCommonHeight) {				
				
				if (mWasKeyboardOpen) {
					mWasKeyboardOpen = false;
					
					//if (INFO) Log.i(TAG, "onGlobalLayout:Key board has been hidden");
					
					// user�� �ܼ��� litview�� ���� ������� ���� ���� key board�� ����� ��찡 �ƴ϶�
					// main view�� �����ϱ� ���� ��Ȳ�̶��...
					if (mSwitchToMainView) {
						Message msg = mIMMStatusHandler.obtainMessage();
	                	msg.what = InputMethodManager.RESULT_HIDDEN;
	                	mIMMStatusHandler.sendMessage(msg);
	                	
	                	// ���⼭ �̷��� ����...���� view switch�� �Ϸ�Ǿ��� �� �ʱ�ȭ����!!!
	                	mSwitchToMainView = false;
	                	
	                	//if (INFO) Log.i(TAG, "onGlobalLayout:send RESULT_HIDDEN MSG");
					}
				}
				else {
					if (mAddedTimeZoneSearchEditTextChangedListener == false) {
						mAddedTimeZoneSearchEditTextChangedListener = true;						
						mParentView.addTimeZoneSearchEditTextChangedListener();
					}
				}
			}
			else {
				mWasKeyboardOpen = true;
				
				//if (INFO) Log.i(TAG, "onGlobalLayout:Key board has been opened");
				
				if (mHeightByKeyBoardPopuped == 0) {
					mHeightByKeyBoardPopuped = height;
					//if (INFO) Log.i(TAG, "onGlobalLayout: first got mHeightByKeyBoardPopuped=" + String.valueOf(mHeightByKeyBoardPopuped));
				}
				else {
					// �Ʒ��� ���� ��Ȳ�� � ��Ȳ???
					// :�߻����� �ʴ� �� ������ �� ���� onGlobalLayout������ heightDiff < 0 ��Ȳ�� �߻�������???
					if (height != mHeightByKeyBoardPopuped) {
						//if (INFO) Log.i(TAG, "onGlobalLayout: prv mHeightByKeyBoardPopuped=" + String.valueOf(mHeightByKeyBoardPopuped));
						mHeightByKeyBoardPopuped = height;
						//if (INFO) Log.i(TAG, "onGlobalLayout: cur mHeightByKeyBoardPopuped=" + String.valueOf(mHeightByKeyBoardPopuped));						
					}
				}
			}			
			//if (INFO) Log.i(TAG, "*********************************************************");
		}
	};	
	
	public void refresh() {
		mAddedTimeZoneSearchEditTextChangedListener = false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (INFO) Log.i(TAG, "onKeyDown");
		
		if(KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			if (INFO) Log.i(TAG, "onKeyDown:KEYCODE_BACK");
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (INFO) Log.i(TAG, "onKeyUp");
		
		if(KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			if (INFO) Log.i(TAG, "onKeyUp:KEYCODE_BACK");
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

}
