package com.intheeast.event;

import java.util.Calendar;
import java.util.TimeZone;

import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;
import com.intheeast.event.EditEvent.RepeatEndTime;


import com.intheeast.impl.TimerPickerTimeText;
import com.intheeast.timepicker.RepeatEndTimePickerRender;
import com.intheeast.timepicker.TimePickerContainer;
import com.intheeast.timepicker.TimePickerRenderThread;
import com.intheeast.timepicker.TimerPickerUpdateData;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class EditEventRepeatEndSettingSubView extends LinearLayout {

	public static final int EVENT_NO_REPEATEND = 0;
	public static final int EVENT_REPEATEND_DATE = 1;
	public static final int EVENT_REPEATEND_COUNT = 2;
	
	public static final int RECURRENCE_END_NEVER = 0;
	public static final int RECURRENCE_END_BY_DATE = 1;
	
	public static final String REPEATEEND_TIME_PICKER_NAME = "RepeatEndTimePickerRenderer";
	
	Context mContext;
	ManageEventActivity mActivity;
	Resources mResources;
	
	EditEventFragment mFragment;
	//EditEventActionBarFragment mActionBar;
	EditEvent mEditEvent;
	
	RelativeLayout mFirstSeperatorLayout;
	RelativeLayout mNoRepeatEndLayout;
    RelativeLayout mRepeatEndDateLayout;
    //View mRepeatEndSurplusRegionView;
    View mRepeatEndTimePickerTransparentView;
    //View mRepeatEndSurplusRegionView;    
    //LayoutParams mRepeatEndSurplusRegionViewLayoutParams;
    
    LockableScrollView mRepeatEndPageScrollView;
    RelativeLayout mScrollViewInner;
    
    ImageView mRepeatEndPageItemSelectionIcon;
  	RelativeLayout.LayoutParams mRepeatEndPageItemSelectionIconImageViewLayout;

    //View mRepeatEndSurplusSmallRegionView;
	//LayoutParams mRepeatEndSurplusSmallRegionViewLayoutParams;
	
	View mEditEventViewSeperatorUpperOutLine;
	
    TimePickerContainer mRepeatEndTimePickerContainer;	
    View mRepeatEndTimePickerContainerLowerOutline;
    
	int mRepeatEndViewWidth;
    int mRepeatEndViewHeight;
    int mFirstSeperatorLayoutHeight;
    int mNoRepeatEndLayoutHeight;
    int mRepeatEndDateLayoutWidth;
    int mRepeatEndDateLayoutHeight;
    int mRepeatEndDateLayoutBottom;
    int mRepeatEndTimePickerContainerHeight;
    int mRepeatEndTimePickerContainerBottom;
    int mRepeatEndSurplusRegionLayoutNoRepeatEndHeight;
    int mRepeatEndSurplusRegionLayoutRepeatEndDateHeight;	
    
    
	public EditEventRepeatEndSettingSubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventRepeatEndSettingSubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventRepeatEndSettingSubView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	} 
	
	public void initView(ManageEventActivity activity, EditEventFragment fragment, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
				
		mEditEvent = fragment.mEditEvent;
		
		mRepeatEndViewWidth = -1;
        mRepeatEndViewHeight = -1;
        mFirstSeperatorLayoutHeight = -1;
        mNoRepeatEndLayoutHeight = -1;
        mRepeatEndDateLayoutWidth = -1;
        mRepeatEndDateLayoutHeight = -1;
        mRepeatEndDateLayoutBottom = -1;
        mRepeatEndTimePickerContainerHeight = -1;
        mRepeatEndTimePickerContainerBottom = -1;    
        
        mRepeatEndPageScrollView = (LockableScrollView) findViewById(R.id.editevent_repeatendpage_scroll_view);	
		
        //
        mScrollViewInner = (RelativeLayout) mRepeatEndPageScrollView.findViewById(R.id.editevent_repeatendpage_scroll_view_inner);
        
		mFirstSeperatorLayout = (RelativeLayout)mScrollViewInner.findViewById(R.id.repeatendpage_first_seperator_layout);		
    	mNoRepeatEndLayout =(RelativeLayout)mScrollViewInner.findViewById(R.id.event_repeatend_no_repeatend_layout);    	   	
    	mNoRepeatEndLayout.setOnClickListener(mRepeatEndPageItemClickListener);		
    	mRepeatEndDateLayout =(RelativeLayout)mScrollViewInner.findViewById(R.id.event_repeatend_repeatend_date_layout);    	    	
    	mRepeatEndDateLayout.setOnClickListener(mRepeatEndPageItemClickListener);	
    	
    	//mRepeatEndSurplusRegionView = mScrollViewInner.findViewById(R.id.repeatend_surplus_view);    	
    	float firstSeperatorLayoutHeight = getResources().getDimension(R.dimen.eventViewSeperatorHeight) + getResources().getDimension(R.dimen.editEventSeperatorOutLineHeight);
    	float noRepeatEndLayoutHeight = getResources().getDimension(R.dimen.eventItemHeight);
    	float repeatEndDateLayoutHeight = getResources().getDimension(R.dimen.eventItemHeight);
    	//RelativeLayout.LayoutParams repeatEndSurplusRegionViewParams = (RelativeLayout.LayoutParams) mRepeatEndSurplusRegionView.getLayoutParams();
    	//repeatEndSurplusRegionViewParams.height = (int) (mActivity.mMainFragmentHeight - 
    			//firstSeperatorLayoutHeight - 
    			//noRepeatEndLayoutHeight - 
    			//repeatEndDateLayoutHeight);    	
    	//mRepeatEndSurplusRegionView.setLayoutParams(repeatEndSurplusRegionViewParams);
    	//mRepeatEndSurplusRegionView.setBackgroundColor(getResources().getColor(R.color.eventViewSeperatorColor));
    	
    	mRepeatEndTimePickerTransparentView = mScrollViewInner.findViewById(R.id.repeatend_timepicker_transparent_view); 
    	RelativeLayout.LayoutParams repeatEndTimePickerTransparentViewParams = (RelativeLayout.LayoutParams) mRepeatEndTimePickerTransparentView.getLayoutParams();
    	repeatEndTimePickerTransparentViewParams.height = (int) (mActivity.mMainFragmentHeight - 
    			firstSeperatorLayoutHeight - 
    			noRepeatEndLayoutHeight - 
    			repeatEndDateLayoutHeight);    	
    	mRepeatEndTimePickerTransparentView.setLayoutParams(repeatEndTimePickerTransparentViewParams);
		mRepeatEndTimePickerTransparentView.setBackgroundColor(getResources().getColor(R.color.eventViewSeperatorColor));
		mRepeatEndTimePickerTransparentView.setVisibility(View.GONE);
    	
    	mRepeatEndPageItemSelectionIcon = new ImageView(mActivity);
		mRepeatEndPageItemSelectionIcon.setImageResource(R.drawable.ic_menu_done_holo_light);
		mRepeatEndPageItemSelectionIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);		
		
		mRepeatEndPageItemSelectionIconImageViewLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mRepeatEndPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mRepeatEndPageItemSelectionIconImageViewLayout.addRule(RelativeLayout.CENTER_VERTICAL);		
		
		
		
		mEditEvent.mTimePickerMetaData.createRepeatEndTimePickerRenderParameters(mRepeatEndPageScrollView);
		
		mRepeatEndTimePickerContainer = (TimePickerContainer) mScrollViewInner.findViewById(R.id.repeatend_timepicker_container);
		mRepeatEndTimePickerContainerLowerOutline = mScrollViewInner.findViewById(R.id.repeatend_timepicker_lower_outline);
		// EditEvent.makeSubPage���� ȣ���ϴ� initView�� �� ������
		// createRepeatEndTimePicker���� ȣ���ϴ� makeTimePicker �Լ��� �����ϴ� 
		// mFragment.mEditEvent.mRRuleContext�� ���� �ʱ�ȭ���� �ʾұ� ������ null pointer ��Ÿ�� ������ �߻��Ѵ�
		//createRepeatEndTimePicker();
	}
	
	/*
	public void createRepeatEndSurplusRegionView(int mMainPageViewWidth, int mMainPageViewHeight, int mEditEventViewSeperatorOutlineHeight, 
			int mEditEventViewSeperatorHeight, int mStartTimeTextLayoutHeight) {			
		mEditEventViewSeperatorUpperOutLine = new View(mActivity);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mMainPageViewWidth, mEditEventViewSeperatorOutlineHeight);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP); //android:layout_alignParentTop
		mEditEventViewSeperatorUpperOutLine.setLayoutParams(params);
		mEditEventViewSeperatorUpperOutLine.setBackgroundColor(getResources().getColor(R.color.eventViewSeperatorOutLineColor));		
		
		// mEditEventViewSeperatorOutlineHeight�� ũ�⿡ ���Խ�Ű�� �ȵȴ�!!!
		// :first seperator ���̾ƿ��� RelativeLayout�̹Ƿ� mEditEventViewSeperatorOutlineHeight ���� �ߺ��ȴ�
		int firstSeperatorLayoutHeight = mEditEventViewSeperatorHeight;
		// mEditEventViewItemTextSize�� ���ϴ� ����� �߸��Ǿ���...
		// :����� �ؽ�Ʈ�� ������� ������ ���ϰ� �ִ�-> ������ �߸��Ǿ���
		//  �׷��ٸ� main page�� ������ ��Ÿ���� ������ ���̾ƿ��� ���� ������ ����ؾ� �Ѵ�
		//int noRepeatEndLayoutHeight = mEditEventViewItemTextSize;
		//int repeatEndDateIndicatorLayoutHeight = mEditEventViewItemTextSize;		
		int noRepeatEndLayoutHeight = mStartTimeTextLayoutHeight;
		int repeatEndDateIndicatorLayoutHeight = mStartTimeTextLayoutHeight;
		
    	int nonSurplusRegionHeight = firstSeperatorLayoutHeight + 
    			                     noRepeatEndLayoutHeight + 
    			                     repeatEndDateIndicatorLayoutHeight; 
    			
		int bigViewHeight = mMainPageViewHeight - nonSurplusRegionHeight;
		
		//mRepeatEndSurplusRegionViewLayoutParams = new LayoutParams(mMainPageViewWidth, bigViewHeight);
		//mRepeatEndSurplusRegionView = new View(mActivity);    	
    	//mRepeatEndSurplusRegionView.setBackgroundColor(getResources().getColor(R.color.eventViewSeperatorColor));
		//mRepeatEndSurplusRegionView.setLayoutParams(mRepeatEndSurplusRegionViewLayoutParams);		
	}
	
	public void createRepeatEndSurplusSmallRegionView(int mMainPageViewWidth, int mMainPageViewHeight, int mEditEventViewSeperatorHeight, int mStartTimeTextLayoutHeight, int mTextureViewHeight) {
				
		int firstSeperatorLayoutHeight = mEditEventViewSeperatorHeight;			
		int noRepeatEndLayoutHeight = mStartTimeTextLayoutHeight;
		int repeatEndDateIndicatorLayoutHeight = mStartTimeTextLayoutHeight;
		
    	int nonSurplusSmallRegionHeight = firstSeperatorLayoutHeight + 
    			                          noRepeatEndLayoutHeight + 
    			                          repeatEndDateIndicatorLayoutHeight +
    			                          mTextureViewHeight; 
    			
		int smallViewHeight = mMainPageViewHeight - nonSurplusSmallRegionHeight;
		
		//mRepeatEndSurplusSmallRegionViewLayoutParams = new LayoutParams(mMainPageViewWidth, smallViewHeight);
		//mRepeatEndSurplusSmallRegionView = new View(mActivity);    	
		//mRepeatEndSurplusSmallRegionView.setBackgroundColor(mResources.getColor(R.color.eventViewSeperatorColor));
		//mRepeatEndSurplusSmallRegionView.setLayoutParams(mRepeatEndSurplusSmallRegionViewLayoutParams);			
	}
	*/	
	
	public void attachRepeatEndPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EVENT_NO_REPEATEND:
			mNoRepeatEndLayout.addView(mRepeatEndPageItemSelectionIcon, mRepeatEndPageItemSelectionIconImageViewLayout);
			break;			
		case EVENT_REPEATEND_DATE:
			mRepeatEndDateLayout.addView(mRepeatEndPageItemSelectionIcon, mRepeatEndPageItemSelectionIconImageViewLayout);
			break;		
		}		
	}
        
    public void detachRepeatEndPageItemSelectionIcon(int itemValue) {
		switch(itemValue) {
		case EVENT_NO_REPEATEND:
			mNoRepeatEndLayout.removeView(mRepeatEndPageItemSelectionIcon);
			break;			
		case EVENT_REPEATEND_DATE:
			mRepeatEndDateLayout.removeView(mRepeatEndPageItemSelectionIcon);
			break;		
		}		
	}    
    
    //boolean mMustBeResetRepeatEndTime = true;
    // RepeatEndPage----> MainPage�� ��ȯ�Ǿ�����,
    // MainPage�� RepeatEndItem�� �ؽ�Ʈ�� mRepeatEndValue�� �°� drawing �ؾ� �Ѵ�
    public void setEventRepeatEndItemText() {
    	int repeatEndValue = mFragment.mEditEvent.mRRuleContext.getEndUntillValue();
    	if (repeatEndValue == EVENT_NO_REPEATEND) {    		
    		// �������ʹ� repeatend page�� open �Ǵ� ������ selected icon�� ��ġ�� ������ ���̴�
    		// :�׷��Ƿ� page�� close�� �� ���õ� �׸� ��ġ�� �������� detach�Ѵ�
    		detachRepeatEndPageItemSelectionIcon(EVENT_NO_REPEATEND);
    		mFragment.mEditEvent.mEventRepeatEndText.setText(R.string.does_not_repeatend);    		
    		mFragment.mEditEvent.mRepeatEndTimeObj.mReseted = true;
    	}
    	else {    		
    		// �������ʹ� repeatend page�� open �Ǵ� ������ selected icon�� ��ġ�� ������ ���̴�
    		// :�׷��Ƿ� page�� close�� �� ���õ� �׸� ��ġ�� �������� detach �Ѵ�
    		detachRepeatEndPageItemSelectionIcon(EVENT_REPEATEND_DATE);
    		// selected icon�� ���� �ƶ��̹Ƿ� GONE ���� ������
    		mRepeatEndTimePickerContainer.setVisibility(View.GONE);
    		mRepeatEndTimePickerContainerLowerOutline.setVisibility(View.GONE);
    		
    		mFragment.mEditEvent.mRepeatEndTimeObj.mReseted = false;
    		mFragment.mEditEvent.mRepeatEndTimeObj.checkValidTime();    		
    		mFragment.mEditEvent.mRepeatEndTime.set(mFragment.mEditEvent.mRepeatEndTimeObj.mYear, mFragment.mEditEvent.mRepeatEndTimeObj.mMonth, mFragment.mEditEvent.mRepeatEndTimeObj.mDayOfMonth, 
    				0, 0, 0);
    		mFragment.mEditEvent.mRRuleContext.setEndDate(mFragment.mEditEvent.mRepeatEndTimeObj.mYear, mFragment.mEditEvent.mRepeatEndTimeObj.mMonth, mFragment.mEditEvent.mRepeatEndTimeObj.mDayOfMonth);
    		
    		TimerPickerTimeText repeatEndText = new TimerPickerTimeText(mFragment.mEditEvent.mRepeatEndTime, true);
    		repeatEndText.makeTimeText();
    		mFragment.mEditEvent.mEventRepeatEndText.setText(repeatEndText.getTimeText());
    	}
    }
    
    // MainPage----> RepeatEndPage�� ��ȯ�� ��,
    // mRepeatEndValue�� ���������� RepeatEndCalendar�� �����ؾ� �Ѵ�
    public void resetRepeatEndCalendar() {
    	// �� �̻� �� �Լ����� �ش� ������ �������� �ʰ�
    	// TextureView�� surfaceListener���� �����Ѵ�    	
    }    
    
    // RepeatPage���� '����'���� �����ϸ� RepeatPage[updateEventRepeatItem]���� ȣ��ȴ�
    public void resetRepeatEndPageItemByRepeatPage() {
    	mFragment.mEditEvent.mRepeatEndTimeObj.mReseted = true;    	
    }   
    
    
    public void updateEventRepeatEndItem(int itemValue) {		
    	ValueAnimator va;
    	
		if (itemValue == EVENT_NO_REPEATEND) {
				
			mFragment.mEditEvent.mRRuleContext.setEnd(RECURRENCE_END_NEVER);
			//va = mRepeatEndTimePickerContainer.makeTimePickerAlphaAnimator(true);//mRepeatEndTimePickerContainer.setVisibility(View.GONE);
			va = makeTimePickerAlphaAnimator(false);
			//mRepeatEndSurplusRegionLayout.removeAllViews();
			//mRepeatEndSurplusRegionLayout.addView(mRepeatEndSurplusRegionView);
			//mRepeatEndSurplusRegionLayout.addView(mEditEventViewSeperatorUpperOutLine);
		}		
		else { //EVENT_REPEATEND_DATE:
			// �Ʒ� �� ��츦 ��� ������ ���ΰ�???
			// :�̸� �����ϴ� �÷��� ���� �Ǵ��� �� �ִ� ���� ���°�??? 
			// 1.MainPage���� RepeatEndPage�� ����Ī�� ���� ó�� ���õ� ���
			//   :�Ʒ� �ڵ�� �������� �߻���Ų��
			//    EndTimeCalendar�� �����Ǹ�...�Ʒ� �ڵ�� ������
			//int compareResult = mRepeatEndTimeCalendar.compareTo(mEndTimeCalendar);
			//if (compareResult == 0) {
				//mRepeatEndTimeCalendar.add(Calendar.DATE, 7);
			//}			
			// 2.ó�� ���õ� ��찡 �ƴ� ���
			//   :�� ��쿡�� RepeatEndItem�� EVENT_NO_REPEATEND�� ��۵� ��쿡�� RepeatEndCalendar�� ������ �����ؾ� �Ѵ�.
			//    ��, RepeatEndItem�� EVENT_NO_REPEATEND�� RepeatEndPage�� MainPage�� ����Ī�Ǵ� ��쿡�� 
			//    RepeatEndCalendar�� ������ ��ȿȭ�ȴ�
			//----------------------------------------------------------------------------------
			// �� �� ���� �������� ��� �ذ�Ǿ���
			mFragment.mEditEvent.mRRuleContext.setEnd(RECURRENCE_END_BY_DATE);			
			//mRepeatEndSurplusRegionLayout.removeAllViews();			
			//mRepeatEndSurplusRegionLayout.addView(mRepeatEndSurplusSmallRegionView);
			//mRepeatEndSurplusRegionLayout.addView(mEditEventViewSeperatorUpperOutLine);
			//mRepeatEndTimePickerContainer.setVisibility(View.VISIBLE);	
			//va = mRepeatEndTimePickerContainer.makeTimePickerAlphaAnimator(false);
			va = makeTimePickerAlphaAnimator(true);
		}		
		
		va.setDuration(400);
    	va.start();
	}    
    
    boolean mShowTimePicker;
    public ValueAnimator makeTimePickerAlphaAnimator(boolean showTimePicker) {
    	mShowTimePicker = showTimePicker;
    	
    	ValueAnimator animator;
    	if (mShowTimePicker) {
    		// mRepeatEndTimePickerTransparentView�� ������ ������� �Ѵ�
    		animator = ValueAnimator.ofFloat(1.0f, 0.0f);
    	}
    	else {
    		// mRepeatEndTimePickerTransparentView�� ������ ��Ÿ���� �Ѵ�
    		animator = ValueAnimator.ofFloat(0.0f, 1.0f);
    	}

		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				float value = (Float) valueAnimator.getAnimatedValue();
				mRepeatEndTimePickerTransparentView.setAlpha(value);
				
			}
		});		
		
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
		    public void onAnimationStart(Animator animation) {
				if (mShowTimePicker) {            	   
	            	   mRepeatEndTimePickerContainer.setVisibility(View.VISIBLE);
	            	   mRepeatEndTimePickerContainerLowerOutline.setVisibility(View.VISIBLE);
	            }
				
				mRepeatEndTimePickerTransparentView.setVisibility(View.VISIBLE);
								
		    }
			
            @Override
            public void onAnimationEnd(Animator animation) {
               if (!mShowTimePicker) {            	   
            	   mRepeatEndTimePickerContainer.setVisibility(View.GONE);
            	   mRepeatEndTimePickerContainerLowerOutline.setVisibility(View.GONE);
               }
                              
               mRepeatEndTimePickerTransparentView.setVisibility(View.GONE);
            }
        });
		return animator;
	}
    
    
    View.OnClickListener mRepeatEndPageItemClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){    
        	
        	int prvRepeatEndValue = mFragment.mEditEvent.mRRuleContext.getEndUntillValue();
        	
        	int selectedRepeatEndValue = 0;
        	int viewID = v.getId();        	
        	switch(viewID) {
        	case R.id.event_repeatend_no_repeatend_layout:
        		selectedRepeatEndValue = EVENT_NO_REPEATEND;        		
        		break;
        	case R.id.event_repeatend_repeatend_date_layout:
        		selectedRepeatEndValue = EVENT_REPEATEND_DATE;   		
        		break;
        	
        	default:
        		return;
        	}
        	
        	if (selectedRepeatEndValue == prvRepeatEndValue) {
        		return;
        	}        	
        	
        	detachRepeatEndPageItemSelectionIcon(prvRepeatEndValue); // ������ repeat done icon�� �����Ѵ�        	
        	attachRepeatEndPageItemSelectionIcon(selectedRepeatEndValue);
        	updateEventRepeatEndItem(selectedRepeatEndValue); 
        }
    };    
    
	
	public void createRepeatEndTimePicker() {		
		// makeSubPage �ܰ迡���� ���� mFragment.mEditEvent.mRRuleContext�� �ʱ�ȭ���� �ʾұ� ������ null pointer ���� ��Ÿ�� ������ �߻��Ѵ�
		makeTimePicker();	
		
		mRepeatEndTimePickerContainer.init(mActivity, mEditEvent.mTimePickerMetaData.mTextureViewParams, mTimePicker, TimePickerContainer.ALPHA_TIME_PICKER_ANIMATION);			
	}	
	
	
	Handler mRepeatEndTimerPickerTimeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {				
			super.handleMessage(msg);
			
			TimerPickerUpdateData obj = (TimerPickerUpdateData)msg.obj;
			int field = obj.getUpdateField();
			
			switch(field) {
			case RepeatEndTimePickerRender.ALL_FIELD_TEXTURE:
				mFragment.mEditEvent.mRepeatEndTimeObj.set(obj.mYear, obj.mMonth, obj.mDate);
				break;
			case RepeatEndTimePickerRender.YEAR_FIELD_TEXTURE:				
				mFragment.mEditEvent.mRepeatEndTimeObj.set(RepeatEndTime.YEAR_FIELD, obj.mYear);
				break;
			case RepeatEndTimePickerRender.MONTH_FIELD_TEXTURE:				
				mFragment.mEditEvent.mRepeatEndTimeObj.set(RepeatEndTime.MONTH_FIELD, obj.mMonth);
				break;
			case RepeatEndTimePickerRender.DAYOFMONTH_FIELD_TEXTURE:				
				mFragment.mEditEvent.mRepeatEndTimeObj.set(RepeatEndTime.DAYOFMONTH_FIELD, obj.mDate);
				break;
			default:
				return;
			}									
		}			
	};	
	
	
	
	TimePickerRenderThread mTimePicker;
	public void makeTimePicker() {
		RepeatEndTimeInfo timeInfo = null;        	
    	if (mFragment.mEditEvent.mRepeatEndTimeObj.mReseted) {        		
    		Calendar cal = (Calendar)Calendar.getInstance(mFragment.mEditEvent.mEndTime.getTimeZone());
    		cal.setTimeInMillis(mFragment.mEditEvent.mEndTime.getTimeInMillis());
    		
    		int freqValue = mFragment.mEditEvent.mRRuleContext.getFreq();
    		switch (freqValue) {
            case EditEventRepeatSettingSwitchPageView.FREQ_DAILY:                	
            	cal.add(Calendar.DATE, 7); 
            	break;
            case EditEventRepeatSettingSwitchPageView.FREQ_WEEKLY:                    
            	int interval = mFragment.mEditEvent.mRRuleContext.getFreqInterval();
            	if (interval == 1)
            		cal.add(Calendar.MONTH, 1); 
            	else if (interval == 2)
            		cal.add(Calendar.MONTH, 2); 
            	else
            		return;///////////////////////////////////////////���� ó���� ����� �Ѵ�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                break;
            case EditEventRepeatSettingSwitchPageView.FREQ_MONTHLY:                    
                cal.add(Calendar.YEAR, 1); 
                break;
            case EditEventRepeatSettingSwitchPageView.FREQ_YEARLY:                    
                cal.add(Calendar.YEAR, 5); 
                break;
    		}        		
    		
    		timeInfo = new RepeatEndTimeInfo(mFragment.mEditEvent.mTzId,
    				mFragment.mEditEvent.mEndTime.get(Calendar.YEAR), // RepeatEndTimePicker�� �ּ� ���� mEndTime�κ��� ��� �´�
    				mFragment.mEditEvent.mEndTime.get(Calendar.MONTH),
    				mFragment.mEditEvent.mEndTime.get(Calendar.DAY_OF_MONTH),
        			cal.get(Calendar.YEAR),
        			cal.get(Calendar.MONTH),
        			cal.get(Calendar.DATE));
    	}
    	else {
    		// ���� reset�� �ƴ� ���,
    		// mRepeatEndTime�� �����ؾ� �ϴ� ��찡 �ִ�        		
    		// :�Ʒ��� ���� mRepeatEndTime�� �ð��� mEndTime�� �ð����� ���� ����̴�
    		// �񱳸� ���ؼ� mEndTime�� hour�� minute �׸��� second�� �����ϰ� ���߾� ���ؾ� �Ѵ�
    		Calendar compareEndTime = (Calendar)Calendar.getInstance(mFragment.mEditEvent.mEndTime.getTimeZone());
    		  		
    		compareEndTime.set(mFragment.mEditEvent.mEndTime.get(Calendar.YEAR), 
    				mFragment.mEditEvent.mEndTime.get(Calendar.MONTH), 
    				mFragment.mEditEvent.mEndTime.get(Calendar.DAY_OF_MONTH)); 
    		
    		Calendar compareRepeatEndTime = (Calendar)Calendar.getInstance(mFragment.mEditEvent.mRepeatEndTime.getTimeZone());
    		 		
    		compareRepeatEndTime.set(mFragment.mEditEvent.mRepeatEndTime.get(Calendar.YEAR),
    				mFragment.mEditEvent.mRepeatEndTime.get(Calendar.MONTH),
    				mFragment.mEditEvent.mRepeatEndTime.get(Calendar.DAY_OF_MONTH)); // mRepeatEndTimeCalendar�� �ν��Ͻ��� ������ �ʿ䰡 ����
    		                                                   					// mRepeatEndTimeCalendar�� year/month/date �� field�� ��� ����ϱ� ������...
    		                                                   					// �׷��� Ȥ�� �� �Ǽ��� ����ؼ�...
    		int compareResult = compareRepeatEndTime.compareTo(compareEndTime);
    		if (compareResult == -1) {        			
    			mFragment.mEditEvent.mRepeatEndTime.set(mFragment.mEditEvent.mEndTime.get(Calendar.YEAR), mFragment.mEditEvent.mEndTime.get(Calendar.MONTH), mFragment.mEditEvent.mEndTime.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
    			
    			mFragment.mEditEvent.mRepeatEndTimeObj.set(mFragment.mEditEvent.mRepeatEndTime.get(Calendar.YEAR), 
    					mFragment.mEditEvent.mRepeatEndTime.get(Calendar.MONTH), 
    					mFragment.mEditEvent.mRepeatEndTime.get(Calendar.DAY_OF_MONTH));
    		}
    		
    		timeInfo = new RepeatEndTimeInfo(mFragment.mEditEvent.mTzId,
    				mFragment.mEditEvent.mEndTime.get(Calendar.YEAR),
    				mFragment.mEditEvent.mEndTime.get(Calendar.MONTH),
    				mFragment.mEditEvent.mEndTime.get(Calendar.DAY_OF_MONTH),
    				mFragment.mEditEvent.mRepeatEndTimeObj.get(RepeatEndTime.YEAR_FIELD),
    				mFragment.mEditEvent.mRepeatEndTimeObj.get(RepeatEndTime.MONTH_FIELD),
    				mFragment.mEditEvent.mRepeatEndTimeObj.get(RepeatEndTime.DAYOFMONTH_FIELD));
    	}
    	
    	mTimePicker = new RepeatEndTimePickerRender(RepeatEndTimePickerRender.REPEATEND_TIME_PICKER_NAME, mContext, mActivity,
    			mRepeatEndTimerPickerTimeHandler, 
    			timeInfo, 
    			mEditEvent.mTimePickerMetaData.mTextureViewParams.width, mEditEvent.mTimePickerMetaData.mTextureViewParams.height, 
    			mEditEvent.mTimePickerMetaData.mRepeatEndTimePickerRenderParameters);
	}
	
	public class RepeatEndTimeInfo {
    	public TimeZone mTimeZone;
    	
    	public int mMinYear;
    	public int mMinMonth;
    	public int mMinDayOfMonth;
    	
    	public int mYear;
    	public int mMonth;
    	public int mDayOfMonth;
		
		public RepeatEndTimeInfo() {
			
		}
		
    	public RepeatEndTimeInfo(String tzId, int minYear, int minMonth, int minDayOfMonth,
    			                 int year, int month, int dayOfMonth) {
    		
    		mTimeZone = TimeZone.getTimeZone(tzId);
    		mMinYear = minYear;
        	mMinMonth = minMonth;
        	mMinDayOfMonth = minDayOfMonth;
        	
        	mYear = year;
    		mMonth = month;
    		mDayOfMonth = dayOfMonth;    		
    	}
    	
    };
    
    
}
