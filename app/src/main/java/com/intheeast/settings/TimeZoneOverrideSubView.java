package com.intheeast.settings;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.CommonRelativeLayoutItemContainer;
import com.intheeast.acalendar.CustomEditText;
import com.intheeast.acalendar.R;
import com.intheeast.settings.TimeZoneSettingPane.IMMStatusHandler;
import com.intheeast.timezone.TimeZoneData;
import com.intheeast.timezone.TimeZonePickerUtils;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextWatcher;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TimeZoneOverrideSubView extends LinearLayout {

	private static final String TAG = "TimeZoneOverrideSubView";
	private static final boolean INFO = true;
	
	Context mContext;
	InputMethodManager mIMM;
	
	CommonRelativeLayoutItemContainer mEditTextContainer;
	CustomEditText mTimeZoneSearchEditText;
	Button mCrossButton;
	
	TimeZoneSearchResultListView mTimeZoneSearchResultListView;
	TimeZoneSearchResultAdapter mTimeZoneSearchResultAdapter;
	
	public interface OnHomeTimeZoneSetListener {
        void onHomeTimeZoneSet(String tzID);
    }
	
	public TimeZoneOverrideSubView(Context context) {
		super(context);		
		mContext = context;
		mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);      
	}
	
	public TimeZoneOverrideSubView(Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);    
	}
	
	public TimeZoneOverrideSubView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);	
		mContext = context;
		mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);    
	}
	
	String mCurrentSelectedTimeZoneId;
	TimeZoneData mTimeZoneData;
	TimeZonePickerUtils mTzPickerUtils;
	IMMStatusHandler mIMMStatusHandler;
	
	public void initView(String currentSelectedTimeZoneId, TimeZoneData timeZoneData, TimeZonePickerUtils tzPickerUtils,
			TimeZoneOverrideSubView.OnHomeTimeZoneSetListener l, IMMStatusHandler handler) {
		mCurrentSelectedTimeZoneId = currentSelectedTimeZoneId;
		mTimeZoneData = timeZoneData;
		mTzPickerUtils = tzPickerUtils;
		mIMMStatusHandler = handler;
		
		mSoftInputResultReceiver = new SoftInputResultReceiver();
				
		mEditTextContainer = (CommonRelativeLayoutItemContainer) findViewById(R.id.edittext_container);
        
		
		CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(mContext, mCurrentSelectedTimeZoneId,
                System.currentTimeMillis(), false);        
        String selectedTimeZoneName = timezoneName.toString();      
        
		setUpTimeZoneSearchEditText(/*selectedTimeZoneName*/);
        
		mCrossButton = (Button) mEditTextContainer.findViewById(R.id.cross_button);        
        mCrossButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	//if (INFO) Log.i(TAG, "mCrossButton : onClick");
            	mTimeZoneSearchEditText.setText("");            		
            }
        });       
        
        //timezone_result_list
        mTimeZoneSearchResultListView = (TimeZoneSearchResultListView) this.findViewById(R.id.timezone_result_list);
        mTimeZoneSearchResultAdapter = mTimeZoneSearchResultListView.setUpList(this, timeZoneData, l, handler); 
        mTimeZoneSearchResultListView.setOnItemClickListener(mTimeZoneSearchResultAdapter);
        
	}
	
	public void refresh(String currentSelectedTimeZoneId) {
		TimeZone timezone = TimeZone.getTimeZone(currentSelectedTimeZoneId);
				
		Calendar time = GregorianCalendar.getInstance(timezone);//new Time(timezone.getID());
        time.setTimeInMillis(System.currentTimeMillis());

        boolean daylightTime = false;
        String displayName = timezone.getDisplayName(false, TimeZone.LONG, Locale.getDefault());		
		
        // Sets the text to be displayed when the text of the TextView is empty
        // : TextView�� empty�� ���� ���÷��� �ȴ� �׷��Ƿ� setHint�� ������� ����
        //mTimeZoneSearchEditText.setHint(displayName);
		mTimeZoneSearchEditText.setText(displayName);
		mTimeZoneSearchResultAdapter.refresh(currentSelectedTimeZoneId);
		mTimeZoneSearchResultListView.refresh();
	}
	
	public void addTimeZoneSearchEditTextChangedListener() {
		mTimeZoneSearchEditText.addTextChangedListener(mTimeZoneSearchEditTextWatcher); 
	}
	
	public void removeTimeZoneSearchEditTextChangedListener() {
		mTimeZoneSearchEditText.removeTextChangedListener(mTimeZoneSearchEditTextWatcher); 
	}
	
	
	TextWatcher mTimeZoneSearchEditTextWatcher = new TextWatcher() {
		
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  		   if (INFO) Log.i(TAG, "beforeTextChanged:[s]=" + s.toString());
  		   if (INFO) Log.i(TAG, "beforeTextChanged :[start]=" + String.valueOf(start));
  		   if (INFO) Log.i(TAG, "beforeTextChanged :[count]=" + String.valueOf(count));
  		   if (INFO) Log.i(TAG, "beforeTextChanged :[after]=" + String.valueOf(after));
  	   }
  	 
  	   public void onTextChanged(CharSequence s, int start, int before, int count) {
  		   if (INFO) Log.i(TAG, "onTextChanged:[s]= " + s.toString());
  		   if (INFO) Log.i(TAG, "onTextChanged :[start]=" + String.valueOf(start));
  		   if (INFO) Log.i(TAG, "onTextChanged :[before]=" + String.valueOf(before));
  		   if (INFO) Log.i(TAG, "onTextChanged :[count]=" + String.valueOf(count));
  	   }
  	   
  	   public void afterTextChanged(Editable s) {
  		   String searchTimeZoneText = new String(s.toString());  		   
  		   
  		   if (INFO) Log.i(TAG, "afterTextChanged : " + searchTimeZoneText);
  		   filterOnString(searchTimeZoneText); 
  	   }
  	   
	};
	
	TextView.OnEditorActionListener mTimeZoneSearchEditTextEditorActionListener = new TextView.OnEditorActionListener() {
		
		@Override
		public boolean onEditorAction(TextView v, int actionId,
				KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				if (INFO) Log.i(TAG, "onEditorAction");
				
				filterOnString(v.getText().toString()); 
				
				return true; 
			}
			return false;
		}
	};
	
	
	OnClickListener mTimeZoneSearchEditTextOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (!mTimeZoneSearchEditText.isCursorVisible()) {
				mTimeZoneSearchEditText.setCursorVisible(true);
			}			
		}
		
	};
	
	
	private void filterOnString(String string) {        
		mTimeZoneSearchResultAdapter.getFilter().filter(string);
    }
	
	private void setUpTimeZoneSearchEditText() {
		float containerWidth = getResources().getDisplayMetrics().widthPixels;
		int editTextWidth = (int) (containerWidth * 0.95f);
		float containerHeight = getResources().getDimension(R.dimen.selectCalendarViewItemHeight);
		int editTextHeight = (int) (containerHeight * 0.67f);
		RelativeLayout.LayoutParams editTextParams = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
		editTextParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		
		mTimeZoneSearchEditText = (CustomEditText) mEditTextContainer.findViewById(R.id.search_edittext);
		mTimeZoneSearchEditText.setLayoutParams(editTextParams);       
		
		mTimeZoneSearchEditText.setCursorVisible(false);
        mTimeZoneSearchEditText.setFocusable(false);
        mTimeZoneSearchEditText.setFocusableInTouchMode(false);             
        mTimeZoneSearchEditText.setOnEditorActionListener(mTimeZoneSearchEditTextEditorActionListener);        
		mTimeZoneSearchEditText.setOnClickListener(mTimeZoneSearchEditTextOnClickListener);
        
        mTimeZoneSearchEditText.setCrossButton(mCrossButton);        
        mTimeZoneSearchEditText.setControlKeyEventPreIme(true, mContrlKeyEventPreImeRunnable);
	}
	
	
	public void makeEnableState() {		
		mTimeZoneSearchEditText.setFocusableInTouchMode(true); 		
	}
	
	boolean mSwitchToMainView = false;
	public boolean makeDisableState() {
		////////////////////////////////////////////////////////////////////////////////////////////////
		boolean mustWaitingForKeyBoardHidden = false;
		
		removeTimeZoneSearchEditTextChangedListener();
		mTimeZoneSearchEditText.setCursorVisible(false);
		mTimeZoneSearchEditText.setFocusable(false);
		mTimeZoneSearchEditText.setFocusableInTouchMode(false);		
		
		if(mTimeZoneSearchResultListView.mWasKeyboardOpen) {	
			mTimeZoneSearchResultListView.mSwitchToMainView = true;
			mIMM.hideSoftInputFromWindow(mTimeZoneSearchEditText.getWindowToken(), 0, mSoftInputResultReceiver);   
			mustWaitingForKeyBoardHidden = true;
		}	
		
		return mustWaitingForKeyBoardHidden;
	}
	
	Runnable mContrlKeyEventPreImeRunnable = new Runnable() {

		@Override
		public void run() {
			
			if (mTimeZoneSearchResultListView.mWasKeyboardOpen) {
				mIMM.hideSoftInputFromWindow(mTimeZoneSearchEditText.getWindowToken(), 0, mSoftInputResultReceiver);	
			}
		}
	};
	
	SoftInputResultReceiver mSoftInputResultReceiver;
	public class SoftInputResultReceiver extends ResultReceiver {

        public SoftInputResultReceiver() {
            //super(mHideSoftinputHandler);
        	super(null);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            switch (resultCode) {

                case InputMethodManager.RESULT_HIDDEN:
                	if (INFO) Log.i(TAG, "onReceiveResult : RESULT_HIDDEN");
                	mTimeZoneSearchEditText.setCursorVisible(false);                	
                    break;

                case InputMethodManager.RESULT_SHOWN:
                	if (INFO) Log.i(TAG, "onReceiveResult : RESULT_SHOWN");	
                    break;

                case InputMethodManager.RESULT_UNCHANGED_SHOWN:
                    break;

                case InputMethodManager.RESULT_UNCHANGED_HIDDEN:
                	if (INFO) Log.i(TAG, "onReceiveResult : RESULT_UNCHANGED_HIDDEN");
                    break;

                default:
                    break;
            }
        }
	}

}
