/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intheeast.month;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ListView;
import android.widget.RelativeLayout;


public class MonthListView extends ListView {

    private static final String TAG = "MonthListView";
    private static boolean INFO = true;
    
    VelocityTracker mTracker;
    private int mMinimumVelocity;
    private static float mScale = 0;

    // These define the behavior of the fling. 
    // Below MIN_VELOCITY_FOR_FLING, do the system fling behavior. 
    // Between MIN_VELOCITY_FOR_FLING and MULTIPLE_MONTH_VELOCITY_THRESHOLD, do one month fling. 
    // Above MULTIPLE_MONTH_VELOCITY_THRESHOLD, do multiple month flings according to the fling strength. 
    // When doing multiple month fling, 
    // the velocity is reduced by this threshold
    // to prevent moving from one month fling to 4 months and above flings.
    //private static int MIN_VELOCITY_FOR_EXPAND_EVENTSVIEW_FLING = 500;
    private static int MULTIPLE_MONTH_VELOCITY_THRESHOLD = 2000;
    //private static int FLING_VELOCITY_DIVIDER = 500;
    private static int FLING_TIME = 500;

    // disposable variable used for time calculations
    protected Calendar mTempTime;
    protected Calendar mTargetMonthTimeInEVMode;
    private long mDownActionTime;
    private final Rect mFirstViewRect = new Rect();

    Context mListContext;    
        
    int mWidth;
    int mHeight;
    //int mPrvMonthWeekHeight;
    int mMonthIndicatorHeight;
    //int mNextMonthWeekHeight;
    int mTwoMonthWeekHeight;
    //int mFfweekHeight;
    int mNormalWeekHeight;
    
    int mFirstWeekHeight;
    //int mLastWeekHeight;    
    
    int mEEVM_Height;
    //int mEEVM_PrvMonthWeekHeight;
    int mEEVM_MonthIndicatorHeight;
    //int mEEVM_NextMonthWeekHeight;
    int mEEVM_TwoMonthWeekHeight;
    int mEEVM_FfweekHeight;
    int mEEVM_NormalWeekHeight;
    
    int mEEVM_FirstWeekHeight;
    //int mEEVM_LastWeekHeight;    
    
    boolean mExpandEventViewMode = false;    
    boolean mGotViewSize = false;
    boolean mExpandEventViewModeGotSize = false; // expand mode�� target month�� ���� ���÷� height�� size�� ����� �� �ִ�
    
    String mTzId;
    TimeZone mTimeZone;
    long mGmtoff;
    
    public static final float EEVM_WEEK_HEIGHT_RATIO_BY_NM_WEEK_HEIGHT = 0.75f;
    
    //public static final float EEVM_MONTHINDICATOR_HEIGHT_RATIO_BY_NM_MONTHINDICATOR_HEIGHT = 0.75f;
    public static final float EEVM_MONTHINDICATOR_HEIGHT_RATIO_BY_NM_MONTHINDICATOR_HEIGHT = 1;
        
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	Log.i("tag", "MonthListView:onSizeChanged");    	
    	
		super.onSizeChanged(w, h, oldw, oldh);
	}    
    
    public void setMonthListViewDimension(int width, int height) {
    	if (!mGotViewSize) {
	    	mWidth = width;
			mHeight = height;
			
			float heightRatio = MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT;
			mMonthIndicatorHeight = (int)(mHeight * heightRatio); 
			
			heightRatio = MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT;
			mNormalWeekHeight = (int)(mHeight * heightRatio);
			
			mTwoMonthWeekHeight = mNormalWeekHeight + mMonthIndicatorHeight + mNormalWeekHeight;       
	    	
			mFirstWeekHeight = mMonthIndicatorHeight + mNormalWeekHeight; 
	    	
			mEEVM_MonthIndicatorHeight = (int)(mMonthIndicatorHeight * EEVM_MONTHINDICATOR_HEIGHT_RATIO_BY_NM_MONTHINDICATOR_HEIGHT);				
			
			mEEVM_NormalWeekHeight = (int)(mNormalWeekHeight * EEVM_WEEK_HEIGHT_RATIO_BY_NM_WEEK_HEIGHT);
			
			mEEVM_TwoMonthWeekHeight = mEEVM_NormalWeekHeight + mEEVM_MonthIndicatorHeight + mEEVM_NormalWeekHeight;	
			
			mEEVM_FfweekHeight = mEEVM_MonthIndicatorHeight + mEEVM_NormalWeekHeight;	
			
			mEEVM_FirstWeekHeight = mEEVM_MonthIndicatorHeight + mEEVM_NormalWeekHeight; 
	    	
			mGotViewSize = true;
    	}	    	
    }
    
    
    Runnable mEventViewAdjustmentViewsCallBack;
    public void setEEVMGlobalLayoutListener(Runnable callBack) {
    	mEventViewAdjustmentViewsCallBack = callBack;
		ViewTreeObserver VTO = getViewTreeObserver(); 
		VTO.addOnGlobalLayoutListener(mEEVMOnGlobalLayoutListener);			
	}   
	
	OnGlobalLayoutListener mEEVMOnGlobalLayoutListener = new OnGlobalLayoutListener() {

		@SuppressWarnings("deprecation")
		@Override
		public void onGlobalLayout() {			
			//Log.i("tag", "MonthListView::onGlobalLayout");
	    	MonthListView.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);	    	
	    	mEventViewAdjustmentViewsCallBack.run();	    		
		}		
	};
	
	
	Runnable mEEVMFinishedCallBack;
    public void setRecoveryNMGlobalLayoutListener(Runnable callBack) {
    	mEEVMFinishedCallBack = callBack;
		ViewTreeObserver VTO = getViewTreeObserver(); 
		VTO.addOnGlobalLayoutListener(mOnRecoveryNMGlobalLayoutListener);			
	}   
	
	OnGlobalLayoutListener mOnRecoveryNMGlobalLayoutListener = new OnGlobalLayoutListener() {

		@SuppressWarnings("deprecation")
		@Override
		public void onGlobalLayout() {			
			//Log.i("tag", "MonthListView::onGlobalLayout");
	    	MonthListView.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);	    	
	    	mEEVMFinishedCallBack.run();	    		
		}		
	};

	// Updates the time zone when it changes
    private final Runnable mTimezoneUpdater = new Runnable() {
        @Override
        public void run() {
            if (mTempTime != null && mListContext != null) {
                                
                mTzId = Utils.getTimeZone(mListContext, mTimezoneUpdater);
                mTimeZone = TimeZone.getTimeZone(mTzId);
                mGmtoff = mTimeZone.getRawOffset() / 1000;
                ETime.switchTimezone(mTempTime, mTimeZone);
                ETime.switchTimezone(mTargetMonthTimeInEVMode, mTimeZone);
            }
        }
    };

    //Context mContext;
    public MonthListView(Context context) {
        super(context);
        init(context);
    }

    public MonthListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MonthListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    Handler mHandler;
    static int mFirstDayOfWeek;
    int mLastDayOfWeek;
    static float mPhysicalCoeff;
    private static float mFlingFriction;
    @SuppressLint("Recycle") 
    private void init(Context c) {
        mListContext = c;   
        
        mFlingFriction = ViewConfiguration.getScrollFriction();
                
        final float ppi = getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning        
        
        
        final ViewConfiguration configuration = ViewConfiguration.get(mListContext);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
                
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mListContext);
        mLastDayOfWeek = Utils.getLastDayOfWeek(mListContext);        
        
        mTracker  = VelocityTracker.obtain();
        mTzId = Utils.getTimeZone(c,mTimezoneUpdater);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;
        
        mTempTime = GregorianCalendar.getInstance(mTimeZone);
        mTargetMonthTimeInEVMode = GregorianCalendar.getInstance(mTimeZone);
                
        if (mScale == 0) {
            mScale = c.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
            	mMinimumVelocity *= mScale;
            	//MIN_VELOCITY_FOR_EXPAND_EVENTSVIEW_FLING *= mScale;
                MULTIPLE_MONTH_VELOCITY_THRESHOLD *= mScale;
                //FLING_VELOCITY_DIVIDER *= mScale;
            }
        }
    }

   
    public void setExpandEventViewMode(boolean mode) {
    	mExpandEventViewMode = mode;
    	if (mExpandEventViewMode) {
    		setBackgroundColor(getResources().getColor(R.color.manageevent_actionbar_background));    		
    	}
    }    
    
    public boolean getExpandEventViewModeState() {
    	return mExpandEventViewMode;    	
    }    
    
    public void setExpandEventViewModeHeight(int height) {    	
		mEEVM_Height = height;		
    }
    
    public int getFirstDayOfWeek() {
    	return mFirstDayOfWeek;
    }
    
    public int getOriginalListViewHeight() {    	
    	return mHeight;    	
    }
    
    public int getNMListViewHeight() {    	
    	return mHeight;    	
    }
    
    /*public int getNMPrvMonthWeekHeight() {
    	return mPrvMonthWeekHeight;    	
    }*/
    
    public int getNMMonthIndicatorHeight() {
    	return mMonthIndicatorHeight;    	
    }
    
    public int getNMNormalWeekHeight() {    	
    	return mNormalWeekHeight;    	
    }   
    
    public int getNMFirstWeekHeight() {    	
    	return mFirstWeekHeight;    	
    }
    
    public int getNMTwoMonthWeekHeight() {
       	return mTwoMonthWeekHeight;    	
    }    
    
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public int getEEMListViewHeight() {
    	return mEEVM_Height;
    }    
    
    public int getEEMMonthIndicatorHeight() {
    	return mEEVM_MonthIndicatorHeight;
    }
    
    public int getEEMNormalWeekHeight() {    	
    	return mEEVM_NormalWeekHeight;
    }   
    
    public int getEEMFirstWeekHeight() {    	
    	return mEEVM_FirstWeekHeight;
    }
    
    public int getEEMTwoMonthWeekHeight() {
       	return mEEVM_TwoMonthWeekHeight;
    }
       
    
    ///////////////////////////////////////////////////////////////////////
    
    @SuppressLint("ClickableViewAccessibility") 
    @Override
    public boolean onTouchEvent(MotionEvent ev) {    	
    	if (!mExpandEventViewMode)
    		return processEventInNMode(ev) || super.onTouchEvent(ev);
    	else 
    		return processEventInEEMode(ev) || super.onTouchEvent(ev);
        
    }
	
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	if (!mExpandEventViewMode)
    		return processEventInNMode(ev) || super.onInterceptTouchEvent(ev);
    	else 
    		return processEventInEEMode(ev) || super.onInterceptTouchEvent(ev);
    }  
        
    public boolean mNormalModeComputingFling = false;
    private boolean processEventInNMode (MotionEvent ev) {
    	
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            // Since doFling sends a cancel, make sure not to process it.
            case MotionEvent.ACTION_CANCEL:
            	//Log.i("tag", "MonthListView : processEvent : ACTION_CANCEL");
                return false;
            // Start tracking movement velocity
            case MotionEvent.ACTION_DOWN:      
            	mTracker.clear();
                mDownActionTime = SystemClock.uptimeMillis();
                
            	//if (MonthListView.this.mListFragment.mCurrentScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                if (mNormalModeComputingFling) {
	            	synchronized (mNormalModeFlingSync) {
	            		
	            		//smoothScrollBy(0, 0);
	            		mNormalModeComputingFling = false;
	            		mListFragment.stopFlingComputation();
	            		//smoothScrollBy(0, 0); // ���� scroll���� ���� �ߴܽ�ų �� �ִ°�??? : �׷���
	            		//->������ idle ���¸� ���߽�Ŵ���ν� deley�� ���߽�Ű�� �ڵ尡 �� �� �ִ� �� ����
	            		//  SCROLL_STATE_IDLE ���·� ���� ��ȯ�� �ʿ䰡 ���� �� ����...onScroll ���·� �ν��ϰ� �ֱ� ������
	            		//  smoothScrollBy(0, 0) �ڵ�� �ڵ����� idle ���·� ��ȯ�ȴ�
		            	//removeCallbacks(mNormalModeFlingRunnable);	           	
		            	//MonthListView.this.mListFragment.onScrollStateChanged(MonthListView.this, OnScrollListener.SCROLL_STATE_IDLE);	            	
	            	}
            	}            	
                
                break;
            // Accumulate velocity and do a custom fling when above threshold
            case MotionEvent.ACTION_UP:            	
                mTracker.addMovement(ev);
                mTracker.computeCurrentVelocity(1000);    // in pixels per second
                
                float vel =  mTracker.getYVelocity();
                if (Math.abs(vel) > mMinimumVelocity) {
                	MotionEvent cancelEvent = MotionEvent.obtain(mDownActionTime,  SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    onTouchEvent(cancelEvent);
                    
                	//mLastScrollState = 1;
                	//smoothScrollBy(0, 0); // Fling�� �߻� �� SCROLL_STATE_TOUCH_SCROLL�� ���� �۾��� ��ҽ�ų �� �ִ°�???
                	                      // :�Ƹ��� last child view�� ��ġ�� ����ִ� �� ���Ƽ� smoothScrollBy(0, 0)�� ȣ���Ѵ�
                	
                	//if (MonthListView.this.mListFragment.mCurrentScrollState != OnScrollListener.SCROLL_STATE_FLING) 
                	if (!mNormalModeComputingFling) {
                		mNormalModeComputingFling = true;
                		//MonthListView.this.mListFragment.onScrollStateChanged(MonthListView.this, OnScrollListener.SCROLL_STATE_FLING);
                	}
                	
                	mFlingVelocity = vel;
                	//post(mNormalModeFlingRun);
                	postOnAnimation(flingRunInNMode);
                	//normalModeDoFling(vel);
                	// fling ���¿��� down �׼��� ������ ���� fling�� �ߴܵ� ���¸� ����Ͽ�!!!
                	/*if (MonthListView.this.mListFragment.mCurrentScrollState != OnScrollListener.SCROLL_STATE_FLING) 
                		MonthListView.this.mListFragment.onScrollStateChanged(MonthListView.this, OnScrollListener.SCROLL_STATE_FLING);*/
                    return true;
                }
                else {
                	Log.i("tag", "au...but under mMinimumVelocity");
                }
            	
                break;
            
            default:
                 mTracker.addMovement(ev);
                 break;
        }
        return false;
    }    
    
    float mFlingVelocity;
    private final Runnable flingRunInNMode = new Runnable() {
        public void run() { 
        	doFlingInNMode(mFlingVelocity);
        }
    };
    
    // event.eventType == EventType.EXPAND_EVENT_VIEW�� ������ ��
    // normal mode fling ���̶�� fling computing�� �ߴ� ���Ѿ��Ѵ�
    
    public void stopFlingInNMode() {
    	synchronized (mNormalModeFlingSync) {
    		mNormalModeComputingFling = false;
        	//removeCallbacks(mNormalModeFlingRunnable);       
    	}
    }
    
    
    //boolean mUpwardFling = false;
    private void doFlingInNMode(float velocityY) {
    	    			
    	mListFragment.onScrollStateChanged(this, OnScrollListener.SCROLL_STATE_FLING); 
    	
        if (velocityY > 0) {
        	// downward fling : finger direction is from UP to DOWN       	
        	computeDownwardFlingInNMode((int)velocityY);
        }
        else if (velocityY < 0){        	
        	// upward fling : finger direction is from DOWN to UP        	        	
        	computeUpwardFlingInNMode((int)velocityY);        	
        }
        else {        	
        	return;
        }        
        
    }
    
    public static class UnVisibleMonthPartOfVisibleFirstWeek {
    	int mUnvisibleMonthHeight;
    	long mCompletionMonthMillis;
    	
    	public UnVisibleMonthPartOfVisibleFirstWeek(int unVisibleMonthHeight, long completionMonthMillis) {
    		mUnvisibleMonthHeight = unVisibleMonthHeight;
    		mCompletionMonthMillis = completionMonthMillis;
    	}
    }
    
    private UnVisibleMonthPartOfVisibleFirstWeek calcUnvisibleMonthPartOfVisibleFirstWeek() {
    	int monthIndicatorHeight = getNMMonthIndicatorHeight();
    	MonthWeekView child = (MonthWeekView) getChildAt(0);		
    	//int firstJulianDay = child.getFirstJulianDay();
		//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();		
		
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);		
		
		UnVisibleMonthPartOfVisibleFirstWeek obj = null;
		int visibleMonthHeight = 0;
		int unVisibleMonthHeight = 0;
    	if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {   
    		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    		mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
    		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    		
    		/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
        	int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);
        	int remainingWeeks = remainingMonthDays / 7;        	
        	int lastWeekDays = remainingMonthDays % 7;       	
            if (lastWeekDays > 0) { // ��κ��� �̷� ���̴�
            	++remainingWeeks;            	
            }*/
            int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, false);
            
            visibleMonthHeight = child.getBottom();
            visibleMonthHeight = visibleMonthHeight + (remainingWeeks * getNMNormalWeekHeight());  
            
            TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * getNMNormalWeekHeight());
            
			unVisibleMonthHeight = monthHeight - visibleMonthHeight;
    	}
    	else {
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {          		
        		int lastWeekHeightOfPrvMonth = getNMNormalWeekHeight();
        		int topFromListView = Math.abs(child.getTop()); // visbile first week�� getTop ������ (+)���� ���� �� ���� : 0 or (-)��
        		                                                // (-)���� ����ؼ� ���밪���� ó���Ѵ�
        		
        		if (topFromListView < lastWeekHeightOfPrvMonth) {
        			
        			// ó���ؾ� �� �κ��� prv month���� �ǹ��Ѵ� : next month�� first week�� �Ű� �� �ʿ䰡 ����
        			// -prv month�� last week �κ��� listview�� top line�� ���� ������ �ǹ��Ѵ�
        			//  :�� getTop �κа� getBottom �� ������ ��еǾ� ������ �ǹ��Ѵ�        			
        			visibleMonthHeight = child.getBottom() - getNMFirstWeekHeight();
        			
        			//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        			mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
            		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
        			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        			
        			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
        			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * getNMNormalWeekHeight());
        			unVisibleMonthHeight = monthHeight - visibleMonthHeight;
        			       			
        		}else {
        			
        			// ó���ؾ� �� �κ��� next month�� first week���� �ǹ��Ѵ�  
        			// -next month�� (month indicator + first week) ������ listview�� top line�� ���� ������ �ǹ��Ѵ�
        			// �Ʒ�ó�� prv month�� last week ������ ����� �Ѵ�
        			unVisibleMonthHeight = topFromListView - lastWeekHeightOfPrvMonth;
        			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
        			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);   
            		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
        			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);		     			
        		}              		
        	}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
        		
        		int topFromListView = Math.abs(child.getTop());
        		unVisibleMonthHeight = topFromListView;
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
        		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);		 
        			
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {  
        		
        		int topFromListView = Math.abs(child.getTop());
        		unVisibleMonthHeight = topFromListView;
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);   
        		TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());            		
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);	    			
        	}
    	}  
    	
    	long completionMonthMillis = TempCalendar.getTimeInMillis();
        obj = new UnVisibleMonthPartOfVisibleFirstWeek(unVisibleMonthHeight, completionMonthMillis);
        
    	return obj;
    }    
    
    
    
    private void computeDownwardFlingInNMode(int velocityY) {
    	final int originalDistance = (int)getSplineFlingDistance((int)velocityY);    	
    					
    	UnVisibleMonthPartOfVisibleFirstWeek obj = calcUnvisibleMonthPartOfVisibleFirstWeek();
    	int accumulatedDistance = obj.mUnvisibleMonthHeight;    	
    	
    	Calendar weekTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
    	weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        weekTimeCalendar.setTimeInMillis(obj.mCompletionMonthMillis);       
        weekTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);   
        
    	if (obj.mUnvisibleMonthHeight >= originalDistance) {
    		int newVelocity = getSplineFlingAbsVelocity(accumulatedDistance);  
    		
    		FlingParameter flingObj = new FlingParameter(newVelocity, accumulatedDistance);
        	
    		float delta = flingObj.mDistance;
    		float maxDelta = delta * 2.5f; //float maxDelta = delta * 2; 
    		
        	mListFragment.setNormalModeFlingContext(DOWNWARD_FLING_NORMAL_MODE, flingObj.mDistance, calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());
        	
    		mListFragment.loadEventsByFling(weekTimeCalendar); 
    		
    		mListFragment.kickOffFlingComputationRunnable();
    		return;
    	}   	
    	
        boolean continuing = true;
    	while (continuing) {        		        
        	weekTimeCalendar.add(Calendar.MONTH, -1);  
        	
        	int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);  
        	
        	int monthHeight = mMonthIndicatorHeight + (weekNumbersOfMonth * mNormalWeekHeight);
        	
        	accumulatedDistance = accumulatedDistance + monthHeight;
        	
        	if (accumulatedDistance >= originalDistance) {  
        		
        		int newVelocity = getSplineFlingAbsVelocity(accumulatedDistance);   
        		
        		FlingParameter flingObj = new FlingParameter(newVelocity, accumulatedDistance);
            	
        		float delta = flingObj.mDistance;
        		float maxDelta = delta * 2.5f; //float maxDelta = delta * 2;
        		
            	mListFragment.setNormalModeFlingContext(DOWNWARD_FLING_NORMAL_MODE, flingObj.mDistance, calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());
            	
        		mListFragment.loadEventsByFling(weekTimeCalendar);        		
        		
                continuing = false;
        		
                mListFragment.kickOffFlingComputationRunnable();
        	}           
        }    	
    }
    
    public static class FlingParameter {
    	int mVelocity;
    	int mDistance;
    	
    	public FlingParameter(int velocity, int distance) {
    		mVelocity = velocity;
    		mDistance = distance;
    	}
    }    
    
    private FlingParameter adjustUpwardFlingDistance (Calendar stopMonth, int originalDistance, int weekNumbers, int accumulatedMovedUpwardMonthHeight) {
    	Log.i("tag", "accumulatedMovedUpwardMonthHeight=" + String.valueOf(accumulatedMovedUpwardMonthHeight));   
    	
    	FlingParameter flingObj = null;
    	
    	int newVelocity = 0;
    	//int monthHeight = mMonthIndicatorHeight + (weekNumbers * mNormalWeekHeight);
    	int visibleMonthHeight = accumulatedMovedUpwardMonthHeight;
    	int visibleMonthTop = mHeight - visibleMonthHeight;
    	
    	
    	int listViewHalfLine = mHeight / 2;   	
    	
    	int adjustedDistance = 0;
    	int willBeAddDistance = 0;
    	if (visibleMonthTop <= listViewHalfLine) {
    		// 6���� week�� ������ ���� ���?
    		willBeAddDistance = visibleMonthTop;
    		adjustedDistance = originalDistance + willBeAddDistance;
    		Log.i("tag", "visibleMonthTop <= listViewHalfLine");    		 
    	}
    	else {
    		Log.i("tag", "visibleMonthTop > listViewHalfLine");    	
    		// ���� ���� top�� ��ġ���Ѿ� �Ѵ�
    		// ���� ���� ������ �ʿ� : ���� ���� month height�� �˱� ���ؼ�
    		// ���� ���� unvisible height �� �ʿ�
    		// �������� unvisible height ��ŭ �Ʒ��� �̵��ؾ� �Ѵ�
    		stopMonth.add(Calendar.MONTH, -1);
    		int weekNumbersOfPrvMonth = ETime.getWeekNumbersOfMonth(stopMonth);  
    		int prvMonthHeight = mMonthIndicatorHeight + (weekNumbersOfPrvMonth * mNormalWeekHeight);
    		
    		int prvMonthTop = visibleMonthTop - prvMonthHeight;
    		// ���� 2015/02�� prv month�̰� 02�� top�� listview�� top �Ʒ��� �����ϴ� ���
    		// :prvMonthTop�� +���� ���̴�
    		if (prvMonthTop >= 0) {
    			Log.i("tag","prv moth�� top�� listview�� top �Ʒ��� ��ġ�� ���");
    			// �߸��Ǿ���...�� abs���� ���߳�???
    			willBeAddDistance = prvMonthTop;
    			adjustedDistance = originalDistance + willBeAddDistance;
    		}
    		// ���� 2014/08�� prv month�̰� 08�� top�� listview�� top ���� �����ϴ� ���
    		// :prvMonthTop�� -���� ���̴�
    		else {
    			Log.i("tag","prv moth�� top�� listveiw�� top�� over �� ���");
    			willBeAddDistance = prvMonthTop;
    			//adjustedDistance = originalDistance - Math.abs(prvMonthTop);
    			adjustedDistance = originalDistance + willBeAddDistance;
    		}    				
    				
    	}   	
    	
    	newVelocity = getSplineFlingAbsVelocity(adjustedDistance);   	
    	flingObj = new FlingParameter(newVelocity, adjustedDistance);
    	
    	Log.i("tag", "willBeAddDistance=" + String.valueOf(willBeAddDistance));  
    	Log.i("tag", "adjustedDistance=" + String.valueOf(adjustedDistance));  
    	
    	return flingObj;
    }
        
   
    
    public static class MonthDimensionBelowVisibleLastWeek {
    	int mMonthHeightBelowVisibleLastWeek;
    	long mCompletionMonthMillis;
    	int mVisibleLastWeekTop;
    	
    	public MonthDimensionBelowVisibleLastWeek(int monthHeightBelowVisibleLastWeek, long completionMonthMillis) {
    		mVisibleLastWeekTop = -1;
    		mMonthHeightBelowVisibleLastWeek = monthHeightBelowVisibleLastWeek;
    		mCompletionMonthMillis = completionMonthMillis;
    	}
    	
    	public MonthDimensionBelowVisibleLastWeek(int top, int monthHeightBelowVisibleLastWeek, long completionMonthMillis) {
    		mVisibleLastWeekTop = top;
    		mMonthHeightBelowVisibleLastWeek = monthHeightBelowVisibleLastWeek;
    		mCompletionMonthMillis = completionMonthMillis;    		
    	}
    }
    // for upward fling!!!
    private MonthDimensionBelowVisibleLastWeek calcMonthDimensionBelowVisibleLastWeek() {
    	
    	int monthIndicatorHeight = getNMMonthIndicatorHeight();
    	
    	int firstVisiblePosition = getFirstVisiblePosition();
    	int lastVisiblePosition = getLastVisiblePosition();
    	int lastChildViewIndex = lastVisiblePosition - firstVisiblePosition;    	
    	
    	MonthWeekView child = (MonthWeekView) getChildAt(lastChildViewIndex);
    	
    	//int firstJulianDay = child.getFirstJulianDay();
    	//int lastJulianDay = child.getLastJulianDay();
		int weekPattern = child.getWeekPattern();	
		 
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek); 		
		
		int childTop = -1;		
		int unVisibleMonthHeight = 0;		
		
		if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {
			if (INFO) Log.i(TAG, "NORMAL_WEEK_PATTERN???");
			
			childTop = child.getTop();			
			unVisibleMonthHeight = mNormalWeekHeight;
			
			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
			
			/*int maximumMonthDays = TempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
        	int remainingMonthDays = maximumMonthDays - TempCalendar.get(Calendar.DAY_OF_MONTH);   		
        	int remainingWeeks = remainingMonthDays / 7;        	
        	int lastWeekDays = remainingMonthDays % 7;       	
            if (lastWeekDays > 0) { // ��κ��� �̷� ���̴�
            	remainingWeeks++;            	
            }*/
            int remainingWeeks = ETime.getRemainingWeeks(TempCalendar, false);
            
            unVisibleMonthHeight = unVisibleMonthHeight + (remainingWeeks * getNMNormalWeekHeight()); 
            TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    	}
		else {
    		
    		if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) { 
    			if (INFO) Log.i(TAG, "TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN");
    			    			
    			childTop = child.getTop();			
    			unVisibleMonthHeight = mNormalWeekHeight; // prv month week height    			
    			
    			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * getNMNormalWeekHeight());
    			
    			unVisibleMonthHeight = unVisibleMonthHeight + monthHeight;        
        		
        	}else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {
        		if (INFO) Log.i(TAG, "FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN");
        		// month height���� ���� visible ������ ���������ν� unvisible month height�� ����� �� �ִ�
        		childTop = child.getTop();	
        		
        		//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
        		mTempTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay); // firstJulianDay�� 1�� ���̴�
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(TempCalendar);
    			int monthHeight = monthIndicatorHeight + (weekNumbersOfMonth * getNMNormalWeekHeight());
    			unVisibleMonthHeight = monthHeight;        			
        		      		
        	}else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {  
        		if (INFO) Log.i(TAG, "LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN");
        		
        		childTop = child.getTop();	
        		unVisibleMonthHeight = mNormalWeekHeight;
        		
        		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
    			mTempTime.setTimeInMillis(child.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 
    			TempCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
    			TempCalendar.set(Calendar.DAY_OF_MONTH, 1);    			
        	}
    	} 		
		
		MonthDimensionBelowVisibleLastWeek obj = new MonthDimensionBelowVisibleLastWeek(childTop, unVisibleMonthHeight, TempCalendar.getTimeInMillis());
		return obj;
    }
    
    private void computeUpwardFlingInNMode(int velocityY) {  		
    	
		final int originalDistance = (int)getSplineFlingDistance((int)velocityY);    	
		
		MonthDimensionBelowVisibleLastWeek obj = calcMonthDimensionBelowVisibleLastWeek();
    	int accumulatedDistance = obj.mMonthHeightBelowVisibleLastWeek;    	
    	
    	Calendar weekTimeCalendar = GregorianCalendar.getInstance(mTimeZone);
        // Time�� day of week�� Calendar�� day of week�� ���� �ٸ� number ordering���� ���ȴ�
        // :�׷��Ƿ� 1�� �������� ������ ���ش�
        weekTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
        weekTimeCalendar.setTimeInMillis(obj.mCompletionMonthMillis);       
        weekTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);  
        int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        int monthHeight = mMonthIndicatorHeight + (weekNumbersOfMonth * mNormalWeekHeight);
        
    	if (accumulatedDistance > originalDistance) {
    		
    		int newVelocity = getSplineFlingAbsVelocity(accumulatedDistance);  
    		
    		// ���� accumulatedDistance ������ �ƴϴ�
    		// last visible week view�� top�� ������ offset�� �Ѱ�
    		accumulatedDistance = obj.mVisibleLastWeekTop + accumulatedDistance - monthHeight;    	    		
    		
    		FlingParameter flingObj = new FlingParameter(newVelocity, accumulatedDistance);    		
        	
    		float delta = flingObj.mDistance;
    		float maxDelta = delta * 2.5f; //float maxDelta = delta * 2;
    		
    		mListFragment.setNormalModeFlingContext(UPWARD_FLING_NORMAL_MODE, flingObj.mDistance, calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());
    		
    		mListFragment.loadEventsByFling(weekTimeCalendar); 
    		
    		mListFragment.kickOffFlingComputationRunnable();
    		
    		return;
    	}    	
        
        boolean continuing = true;        
        
        while (continuing) {
        	weekTimeCalendar.add(Calendar.MONTH, 1); 
        	        	
        	weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(weekTimeCalendar);
        	
        	monthHeight = mMonthIndicatorHeight + (weekNumbersOfMonth * mNormalWeekHeight);
        	
        	accumulatedDistance = accumulatedDistance + monthHeight;
        	
        	if (accumulatedDistance >= originalDistance) {  
        		
        		int newVelocity = getSplineFlingAbsVelocity(accumulatedDistance);    	
        		
        		accumulatedDistance = obj.mVisibleLastWeekTop + accumulatedDistance - monthHeight;         		
        		
        		FlingParameter flingObj = new FlingParameter(newVelocity, accumulatedDistance);
            	
        		float delta = flingObj.mDistance;
        		float maxDelta = delta * 2.5f; //float maxDelta = delta * 2; 
        		
        		mListFragment.setNormalModeFlingContext(UPWARD_FLING_NORMAL_MODE, flingObj.mDistance, calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());
        		
        		mListFragment.loadEventsByFling(weekTimeCalendar); 
        		
                continuing = false;
                
                mListFragment.kickOffFlingComputationRunnable();                
        	}           
        }         	
    } 
    
	
	MonthFragment mListFragment;
	public void setListFragment(MonthFragment listFragment) {
		mListFragment = listFragment;
	}
	
	public static final int DOWNWARD_FLING = 1;
	public static final int UPWARD_FLING = 2;
	
    
    public static final int UPWARD_FLING_NORMAL_MODE = 1;
    public static final int DOWNWARD_FLING_NORMAL_MODE = 2;
    
    //NormalModeFlingRunnable mNormalModeFlingRunnable;
    Object mNormalModeFlingSync = new Object();
    
    
    // Fling friction
    
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    
    public static double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }
    
    public static double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        /*
        double x = DECELERATION_RATE / decelMinusOne;            
        double xx = Math.exp(x * l);            
        double xxx = Math.exp(DECELERATION_RATE / decelMinusOne * l);
        */
        
        double distance = mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
        
        return distance;
    }
    
    // distance : mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l)
    public static int getSplineFlingAbsVelocity(double distance) {
    	int absVelocity = 0;
    	
    	final double decelMinusOne = DECELERATION_RATE - 1.0;  
    	
    	// expResult : Math.exp(DECELERATION_RATE / decelMinusOne * l)
    	double expResult = distance / (mFlingFriction * mPhysicalCoeff);
    	// expPara : DECELERATION_RATE / decelMinusOne * l
    	double expPara = Math.log(expResult);
    	// logResult : Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    	double logResult = expPara / (DECELERATION_RATE / decelMinusOne);
    	// logPara : INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff)
    	double logPara = Math.exp(logResult);
    	
    	absVelocity = (int)( (logPara * (mFlingFriction * mPhysicalCoeff)) / INFLEXION );    	
    	            
        return absVelocity;
    }
    
    //int mUpperRightJulinaDayAtDownInEEVMode;
    private boolean processEventInEEMode (MotionEvent ev) {
    	
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            // Since doFling sends a cancel, make sure not to process it.
            case MotionEvent.ACTION_CANCEL:
            	//Log.i("tag", "MonthListView : processEvent : ACTION_CANCEL");
                return false;
            // Start tracking movement velocity
            case MotionEvent.ACTION_DOWN:            	
            	//mUpperRightJulinaDayAtDownInEEVMode = getUpperRightJulianDay();
                mTracker.clear();
                mDownActionTime = SystemClock.uptimeMillis();
                break;
            // Accumulate velocity and do a custom fling when above threshold
            case MotionEvent.ACTION_UP:            	
                mTracker.addMovement(ev);
                mTracker.computeCurrentVelocity(1000);    // in pixels per second
                
                float vel =  mTracker.getYVelocity ();
                if (Math.abs(vel) > mMinimumVelocity) {                  
                	doFlingInEEMode(vel);
                    return true;
                }
            	
                break;
            
            default:
                 mTracker.addMovement(ev);
                 break;
        }
        return false;
    }
    
    public static final int SLOW_UPWARD_FLING = 1; 
    public static final int SLOW_DOWNWARD_FLING = 2; 
    public static final int PAST_UPWARD_FLING = 3; 
    public static final int PAST_DOWNWARD_FLING = 4; 
    
    int mFlingDirSpeed;
    // Do a "snap to start of month" fling
    private void doFlingInEEMode(float velocityY) {
    	if (INFO) Log.i(TAG, "MonthListView : doFlingInEEMode:Speed=" + String.valueOf(velocityY));    	
        // Stop the list-view movement and take over
        MotionEvent cancelEvent = MotionEvent.obtain(mDownActionTime,  SystemClock.uptimeMillis(),
                MotionEvent.ACTION_CANCEL, 0, 0, 0);
        onTouchEvent(cancelEvent);

        // Below the threshold, fling one month. Above the threshold , fling
        // according to the speed of the fling.
        //int monthsToJump;        
        if (Math.abs(velocityY) < MULTIPLE_MONTH_VELOCITY_THRESHOLD) {
            if (velocityY < 0) { // upward fling
            	mFlingDirSpeed = SLOW_UPWARD_FLING;
            } else { // downward fling
                // value here is zero and not -1 since by the time the fling is
                // detected the list moved back one month.
            	mFlingDirSpeed = SLOW_DOWNWARD_FLING;
            }
        } else {
            if (velocityY < 0) { // upward fling                
            	mFlingDirSpeed = PAST_UPWARD_FLING;
            } else { // downward fling               
            	mFlingDirSpeed = PAST_DOWNWARD_FLING;
            }
        }     
        
        adjustmentMonthListTopItemPositionFlingInEEMode(velocityY);        
    }    

    public class xResult {
    	public Calendar TargetMonthCalendar;
    	public int moveDelta;
    	
    	public xResult(TimeZone timeZone) {
    		TargetMonthCalendar = GregorianCalendar.getInstance(timeZone);
    	}
    }
    
    // computeAndAdjustwAtUpwardFlingInEEMode�� UpWard Fling�̶�� ���� �������
    // UpWard Scrolling�� ����� �� Fling�� �߻��Ѵٴ� ���� ���
    // �׷��Ƿ� �Ʒ�ó�� �ܼ��� �˰������� ó���� �� ����
    // �� Fling ������ first child item view�� ������ ����ϸ� �ȴ�
    public xResult computeAndAdjustwAtUpwardFlingInEEMode() {
    	int monthIndicatorHeight = getEEMMonthIndicatorHeight();      	
    	int visibleMonthHeight = 0;
    	    	
    	xResult result = new xResult(mTimeZone);    	
    	
    	result.TargetMonthCalendar.setFirstDayOfWeek(getFirstDayOfWeek());
		
		MonthWeekView firstChildView = (MonthWeekView) getChildAt(0);
		//int lastJulianDay = firstChildView.getLastJulianDay();///////////////////////////////////////////////////////////    
		//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
		mTempTime.setTimeInMillis(firstChildView.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);    		  
		result.TargetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
			
		/*int maximumMonthDays = result.TargetMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
    	int remainingMonthDays = maximumMonthDays - result.TargetMonthCalendar.get(Calendar.DAY_OF_MONTH);
    	int remainingWeeks = remainingMonthDays / 7;        	
    	int lastWeekDays = remainingMonthDays % 7;       	
        if (lastWeekDays != 0) { // ��κ��� �̷� ���̴�
        	++remainingWeeks;            	
        }*/
		int remainingWeeks = ETime.getRemainingWeeks(result.TargetMonthCalendar, false);       
        
        result.TargetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
		
    	visibleMonthHeight = firstChildView.getBottom();
        visibleMonthHeight = visibleMonthHeight + (remainingWeeks * getEEMNormalWeekHeight());
                
        // Finger dir : down -> up, month�� ������ 
		if (mFlingDirSpeed == SLOW_UPWARD_FLING) {
			int UpwardMove = visibleMonthHeight;
			// ������ monthIndicatorHeight�� TargetMonthCalendar�� ����.
			result.moveDelta = UpwardMove + monthIndicatorHeight;
    		result.TargetMonthCalendar.add(Calendar.MONTH, 1);
    		
    		if (INFO) Log.i(TAG, "PLUS_ONE_MONTH_FROM_VISIBLE_TOP_WEEK_MONTH:moveDelta=" + String.valueOf(result.moveDelta));
		}
		else if (mFlingDirSpeed == PAST_UPWARD_FLING) {		
			int UpwardMove = visibleMonthHeight;
			result.moveDelta = UpwardMove + monthIndicatorHeight;
			result.TargetMonthCalendar.add(Calendar.MONTH, 1);
			
        	int weekNumbersOfTargetMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);       		
        	int addedMoveDelta = monthIndicatorHeight + (weekNumbersOfTargetMonth * getEEMNormalWeekHeight());
    		
    		result.moveDelta = result.moveDelta + addedMoveDelta;
    		result.TargetMonthCalendar.add(Calendar.MONTH, 1);//////////////////////////////////////////////
    		
    		if (INFO) Log.i(TAG, "PAST_UPWARD_FLING");
		}
		
		return result;
    }
    
    // computeAndAdjustAtDownwardFlingInEEMode�� ������ ������
    // target month�� height�� ������� EEMode������ Month ListView�� height��
    // 6 * Normal Week Height In EEMode�� �����Ǿ��� ������
    // �̰��� ������� ������ ����....����....
    // Fling ������ Last Child Item View�� ����� �� �� ���� ������
    // week pattern�� ���� ����� ��� �Ѵ�
    public xResult computeAndAdjustAtDownwardFlingInEEMode() {
    	int monthIndicatorHeight = getEEMMonthIndicatorHeight();
    	
   		MonthWeekView firstWeekView = (MonthWeekView) getChildAt(0);		
   		int weekPattern = firstWeekView.getWeekPattern();
   		//int firstJulianDay = firstWeekView.getFirstJulianDay();
		//int lastJulianDay = firstWeekView.getLastJulianDay();	
   		
   		xResult result = new xResult(mTimeZone);  
   		result.TargetMonthCalendar.setFirstDayOfWeek(getFirstDayOfWeek());
   		
		if (weekPattern == MonthWeekView.NORMAL_WEEK_PATTERN) {      
						
			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(firstWeekView.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay); 		  
			result.TargetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());
			
			/*int maximumMonthDays = result.TargetMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
			int remainingMonthDays = maximumMonthDays - result.TargetMonthCalendar.get(Calendar.DAY_OF_MONTH);
			int remainingWeeks = remainingMonthDays / 7;        	
			int lastWeekDays = remainingMonthDays % 7;       	
			if (lastWeekDays != 0) { // ��κ��� �̷� ���̴�
				++remainingWeeks;            	
			}*/
			int remainingWeeks = ETime.getRemainingWeeks(result.TargetMonthCalendar, false);
			
			result.TargetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1); // getWeekNumbersOfMonth �Լ��� ������ ���� 
			int weekNumbersOfMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
   		
			// ������ ���� month indicator height�� �����ϰ� month height�� ����ߴٴ� ����
			int monthHeight = weekNumbersOfMonth * getEEMNormalWeekHeight();
			int visibleMonthHeightOfFirstWeekView = firstWeekView.getBottom();
			          
			visibleMonthHeightOfFirstWeekView = visibleMonthHeightOfFirstWeekView + (remainingWeeks * getEEMNormalWeekHeight());           
			int DownwardMove = monthHeight - visibleMonthHeightOfFirstWeekView;   // DownwardMove�� ��ǻ� UnVisibleMonthHeightOfFirstWeekView ��
			
			if (mFlingDirSpeed == SLOW_DOWNWARD_FLING) {
				result.moveDelta = -DownwardMove;
				if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=N_M_LS");
			}
			else {
				result.TargetMonthCalendar.add(Calendar.MONTH, -1);
				int weekNumbersOfAddedMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
				// ������ monthIndicatorHeight�� firstWeekView�� month ����
				int addedMoveDelta = monthIndicatorHeight + (weekNumbersOfAddedMonth * getEEMNormalWeekHeight());
				result.moveDelta = -DownwardMove - addedMoveDelta;
				if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=N_M_PS");
			}
			
		}
		else if (weekPattern == MonthWeekView.TWO_DIFFERENT_MONTHDAYS_COEXIST_WEEK_PATTERN) {
			
			int prvMonthWeekHeight = getEEMNormalWeekHeight();		
			
			// result.TargetMonthCalendar�� �ʱⰪ���� Next Month�� First DayOfMonth�� �����Ѵ�!!!
			//long lastJulianDayMills = ETime.getMillisFromJulianDay(lastJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(firstWeekView.getLastMonthTime().getTimeInMillis());//mTempTime.setJulianDay(lastJulianDay);  
			result.TargetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());			
			result.TargetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);								
						
			//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(firstWeekView.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);
			Calendar prvMonthCalendar = GregorianCalendar.getInstance(mTimeZone);
			prvMonthCalendar.setFirstDayOfWeek(getFirstDayOfWeek());  				
			prvMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());				
			prvMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);			
			
			int topFromListView = firstWeekView.getTop();
			// previous month week�� bottom�� ������ ���� ����� �� �ִ�
			int prvMonthWeekBottom = topFromListView + getEEMNormalWeekHeight();
			
			// previous month last week�� Month listview�� top ���� ���� �ִ��� �Ʒ��� �ִ����� �������� ��
			if (prvMonthWeekBottom < 0) {				
				// prv month�� last week�� BOTTOM�� unvisible region[ListView�� Top�� �ʰ���]�� �ִ� ���¿��� downward fling�� �߻�			
				// :move delta ���� prvMonthHeight�� unvisible month indicator height�� �߰��ؾ� �Ѵ�
				
				// prv month�� last week�� BOTTOM�� 0���� �۴ٴ� ���� 
				// lastJulianDay�� month indicator�� ���� �Ϻκе� unvisible region�� �ִٴ� ���� �ǹ��Ѵ� 
				int unVisibleMonthIndicatorHeight = Math.abs(topFromListView) - prvMonthWeekHeight;
				// �ϴ� previous month�� indicator height ���� �����Ѵ�
				int prvMonthHeight = ETime.getWeekNumbersOfMonth(prvMonthCalendar) * getEEMNormalWeekHeight();					
				
				if (mFlingDirSpeed == SLOW_DOWNWARD_FLING) {
					result.moveDelta = -(prvMonthHeight + unVisibleMonthIndicatorHeight);
					result.TargetMonthCalendar.add(Calendar.MONTH, -1);
					if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=T_M_1_LS");
				}
				else {
					result.TargetMonthCalendar.add(Calendar.MONTH, -2);
					int weekNumbersOfAddedMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
					// ������ monthindicatorHeight�� firstJulianDay�� month ����
					int addedMoveDelta = monthIndicatorHeight + (weekNumbersOfAddedMonth * getEEMNormalWeekHeight());
					result.moveDelta = -(prvMonthHeight + unVisibleMonthIndicatorHeight + addedMoveDelta);
					if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=T_M_1_PS");
				}
			}
			else {				
				// prv month�� last week�� TOP�� Month ListView�� Top�� ���ų� �Ʒ��� �ִ� ���¿��� downward fling�� �߻�
				// :move delta ���� prvMonthHeight�� visible prv month last week height�� '����'�ؾ� �Ѵ�
				//  -����� ������ ���� ����ó�� prv month week number - 1 �� height���� unvisible prv month week height�� �߰��ߴ� : ���ذ� ���⸦...					
				
				//int bottomFromListView = firstWeekView.getBottom();
				//int twoMonthWeeksHeight = getEEVMTwoMonthWeekHeight();
				//int unVisiblePrvMonthLastWeekHeight = twoMonthWeeksHeight - bottomFromListView; // ���� �̷� �ʿ䰡 ����...getTop�� �ٷ� unvisible previous month last week height��				
				int unVisiblePrvMonthLastWeekHeight = Math.abs(firstWeekView.getTop());
				// previous month last week�� ���� unvisible region�� �����ϸ� �Ǳ� ������ �� �ָ� ��������� �Ѵ�
				int prvMonthHeight = (ETime.getWeekNumbersOfMonth(prvMonthCalendar) - 1) * getEEMNormalWeekHeight();					
				
				if (mFlingDirSpeed == SLOW_DOWNWARD_FLING) {
					result.moveDelta = -(prvMonthHeight + unVisiblePrvMonthLastWeekHeight);
					result.TargetMonthCalendar.add(Calendar.MONTH, -1);
					if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=T_M_2_LS");
				}
				else {
					result.TargetMonthCalendar.add(Calendar.MONTH, -2);
					int weekNumbersOfAddedMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
					// ������ monthindicatorHeight�� firstJulianDay�� month ����
					int addedMoveDelta = monthIndicatorHeight + (weekNumbersOfAddedMonth * getEEMNormalWeekHeight());
					result.moveDelta = -(prvMonthHeight + unVisiblePrvMonthLastWeekHeight + addedMoveDelta);
					if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=T_M_2_PS");
				}
			}
			
				   		
		}
		else if (weekPattern == MonthWeekView.FIRSTDAY_IS_FIRSTMONTHDAY_WEEK_PATTERN) {   			
			
			//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(firstWeekView.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);
			result.TargetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());			
			result.TargetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);	
			
			//int months = mTempTime.month;	
			
			Calendar prvMonthCalendar = GregorianCalendar.getInstance(mTimeZone);
			prvMonthCalendar.setFirstDayOfWeek(getFirstDayOfWeek());  				
			prvMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());				
			prvMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
			prvMonthCalendar.add(Calendar.MONTH, -1);
			
			// �� ��Ȳ�� month indicator�� TOP�� Month ListView�� Top���� �۰ų� ���� ���¿����� �߻�	
			// :���� ��ġ�� �ִ� LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN�̰�
			//  �Ʒ��� ��ġ�� �ִ� NORMAL_WEEK_PATTERN�̱� �����̴�
			//int firstWeekHeight = getEEVMFirstWeekHeight();
			//int bottomFromListView = firstWeekView.getBottom();
			//int unVisibleMonthIndicatorHeight = firstWeekHeight - bottomFromListView; // �߸� �����ϰ� �ִ� �� ����...���� getTop ������ unvisible region���� ����ϸ� �Ǵ� �� �ƴѰ�???
			int unVisibleMonthIndicatorHeight = Math.abs(firstWeekView.getTop());
			int prvMonthHeight = ETime.getWeekNumbersOfMonth(prvMonthCalendar) * getEEMNormalWeekHeight();		
			
			if (mFlingDirSpeed == SLOW_DOWNWARD_FLING) {
				result.moveDelta = -(prvMonthHeight + unVisibleMonthIndicatorHeight);
				result.TargetMonthCalendar.add(Calendar.MONTH, -1);
				if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=F_M_LS");
			}
			else {
				result.TargetMonthCalendar.add(Calendar.MONTH, -2);
				int weekNumbersOfAddedMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
				int addedMoveDelta = monthIndicatorHeight + (weekNumbersOfAddedMonth * getEEMNormalWeekHeight());
				result.moveDelta = -(prvMonthHeight + unVisibleMonthIndicatorHeight + addedMoveDelta);
				if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=F_M_PS");
			}
			
			
		}
		else if (weekPattern == MonthWeekView.LASTDAY_IS_MAXMONTHDAY_WEEK_PATTERN) {			
			
			//long firstJulianDayMills = ETime.getMillisFromJulianDay(firstJulianDay, mTimeZone);
			mTempTime.setTimeInMillis(firstWeekView.getFirstMonthTime().getTimeInMillis());//mTempTime.setJulianDay(firstJulianDay);
			result.TargetMonthCalendar.setTimeInMillis(mTempTime.getTimeInMillis());			
			result.TargetMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);	
			
			//int lastWeekHeight = getEEVMNormalWeekHeight();
			//int bottomFromListView = firstWeekView.getBottom();
			//int unVisibleLastWeekHeight = lastWeekHeight - bottomFromListView; // �߸� �����ϰ� �ִ� �� ����...���� getTop ������ unvisible region���� ����ϸ� �Ǵ� �� �ƴѰ�???
			                                                                  
			int unVisibleLastWeekHeight = Math.abs(firstWeekView.getTop()); 
			int prvMonthHeight = (ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar) - 1) * getEEMNormalWeekHeight();
			result.moveDelta = -(prvMonthHeight + unVisibleLastWeekHeight);			
			
			if (mFlingDirSpeed == SLOW_DOWNWARD_FLING) {
				result.moveDelta = -(prvMonthHeight + unVisibleLastWeekHeight);	
				if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=L_M_LS");
			}
			else {
				result.TargetMonthCalendar.add(Calendar.MONTH, -1);
				int weekNumbersOfAddedMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
				int addedMoveDelta = monthIndicatorHeight + (weekNumbersOfAddedMonth * getEEMNormalWeekHeight());
				result.moveDelta = -(prvMonthHeight + unVisibleLastWeekHeight + addedMoveDelta);
				if (INFO) Log.i(TAG, "computeAndAdjustwAtDownwardFlingInEEMode=L_M_PS");
			}	
			
		}
		
		return result;
    }
     
    public void adjustmentMonthListTopItemPositionFlingInEEMode(float velocityY) {    	
    	xResult result = null;    	
    	// upward fling
    	if (mFlingDirSpeed == SLOW_UPWARD_FLING || mFlingDirSpeed == PAST_UPWARD_FLING) { 
    		result = computeAndAdjustwAtUpwardFlingInEEMode();
    	}
    	// downward fling
    	else {
    		result = computeAndAdjustAtDownwardFlingInEEMode();
    	}
    	
    	mCurrentEventListHeight = mListFragment.mEventsListView.getHeight();
    	int weekNumbersOfXXXMonth = ETime.getWeekNumbersOfMonth(result.TargetMonthCalendar);
		int targetMonthHeight = weekNumbersOfXXXMonth * getEEMNormalWeekHeight();
		mTargetEventListHeight = getOriginalListViewHeight() - targetMonthHeight;
		
		boolean mustScaleEventListViewHeight = false;
		if (mCurrentEventListHeight != mTargetEventListHeight) {   	
			mustScaleEventListViewHeight = true;
			ScaleEventListViewHeight(velocityY);    			
		}
		
		long monthListViewAnimDuration = calculateDuration(Math.abs(result.moveDelta), mListFragment.mAnticipationMonthListViewHeightInEPMode, Math.abs(velocityY));		
		
		boolean updateMonthDisplayed = false;		
		Calendar targetMonthTime = GregorianCalendar.getInstance(mTimeZone);
		targetMonthTime.setTimeInMillis(result.TargetMonthCalendar.getTimeInMillis());	
		ObjectAnimator fadingIn = null;
		ObjectAnimator fadingOut = null;
		if (ETime.compare(mListFragment.mCurrentMonthTimeDisplayed, targetMonthTime) != 0) {
			mTempTime.setTimeInMillis(System.currentTimeMillis());	
			
			updateMonthDisplayed = true;
			
			fadingOut = hasMonthListViewPrvTargetMonthWeekDay(mListFragment.mAdapter.getSelectedDayInEEMode(), monthListViewAnimDuration);
			
			fadingIn = hasMonthListViewTargetMonthWeekDay(result.TargetMonthCalendar, monthListViewAnimDuration);
			if (fadingIn == null) {    	
		    	
				if (targetMonthTime.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && targetMonthTime.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {					
					mListFragment.mAdapter.setSelectedDayAlphaAnimUnderNoExistenceInEEMode(mTempTime.getTimeInMillis());	
				}
				else {					
					mListFragment.mAdapter.setSelectedDayAlphaAnimUnderNoExistenceInEEMode(targetMonthTime.getTimeInMillis());	
				}				
			}
			
			int targetJualianDayOfEventListView;				
			if (targetMonthTime.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && targetMonthTime.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {						
				targetJualianDayOfEventListView = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
			}
			else {						
				targetJualianDayOfEventListView = ETime.getJulianDay(targetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
			}	
			
			mListFragment.mEventItemsAdapter.reloadEventsOfEventListView(targetJualianDayOfEventListView);
		}				
		
		smoothScrollBy(result.moveDelta, (int) monthListViewAnimDuration); // �ٵ� �Ϸ�Ǵ� ������ �𸥴�?
	                                                                       // :onScrollStateChanged���� SCROLL_STATE_IDLE�� ��ȯ�Ǵ� ���� Ȯ���ϸ� �Ǵ� ���ΰ�? : yes				
		
		if (mustScaleEventListViewHeight) {  
			mEventListViewHeightScaleValueAnimator.start();    	
		}
		
		if (updateMonthDisplayed) {
			if (fadingOut != null)
				fadingOut.start();
			
			if (fadingIn != null)
				fadingIn.start();	
			
			mListFragment.setMonthDisplayed(targetMonthTime);
			
			mListFragment.loadEventsByFling(targetMonthTime);
			// MonthFragment.mLoader�� null�� ��Ȳ�� �߻��Ǳ� ������ �Ʒ� �Լ��� ȣ���ϸ� ���ܰ� �߻��ȴ�
			// :���� ��� year���� month�� ��ȯ�� ���,
			//  onScrollStateChanged���� mLoader�� �ʱ�ȭ���� �ʱ� �����̴�
			//  ->year fragment���� exit year anim�� �����ϸ鼭 �ش� �Ⱓ�� event���� �̸� �ε��ϰ�
			//   �� event���� month fragment���� ��Ȱ���ϱ� ������ onScrollStateChanged���� mLoader�� �ʱ�ȭ���� �ʴ´�
			//   ->�ʱ�ȭ�� �ϸ� �ٷ� onCreateLoader�� ȣ��ǰ� �ٷ� loading�� �����ϱ� �����̴�
			//mListFragment.updateLoaderInEEMode(ETime.getJulianDay(targetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek)); 
		}
    }    
    
    public ObjectAnimator hasMonthListViewTargetMonthWeekDay(Calendar targetMonthCalendar, long animDuration) {
    	
    	int weekCounts = getChildCount();
    	int firstWeek = getFirstVisiblePosition();
    	int lastWeek = getLastVisiblePosition();
    	int firstWeekFirstJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(firstWeek);//Utils.getJulianMondayFromWeeksSinceEpoch(firstWeek);    	
    	int lastWeekLastJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(lastWeek) + 6;//Utils.getJulianMondayFromWeeksSinceEpoch(lastWeek) + 6;
    	
    	Calendar targetMonthTime = GregorianCalendar.getInstance(mTimeZone);
    	targetMonthTime.setTimeInMillis(targetMonthCalendar.getTimeInMillis());
		
    	// ���⼭ �츮�� target month�� today month���� �ƴ����� Ȯ���ؾ� �Ѵ�!!!    	
    	mTempTime.setTimeInMillis(System.currentTimeMillis());
    	
    	int targetMonthJulianToday = 0;
    	long targetMonthJulianTodayMills = 0;
		if (targetMonthTime.get(Calendar.YEAR) == mTempTime.get(Calendar.YEAR) && targetMonthTime.get(Calendar.MONTH) == mTempTime.get(Calendar.MONTH)) {
			targetMonthJulianToday = ETime.getJulianDay(mTempTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
			targetMonthJulianTodayMills = mTempTime.getTimeInMillis();
		}
		else {
			targetMonthJulianToday = ETime.getJulianDay(targetMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
			targetMonthJulianTodayMills = targetMonthTime.getTimeInMillis();
		}		
    	  
		 //ETime.getMillisFromJulianDay(targetMonthJulianToday, mTimeZone);
		targetMonthTime.setTimeInMillis(targetMonthJulianTodayMills);//targetMonthTime.setJulianDay(targetMonthJulianToday);
		
    	if (targetMonthJulianToday >= firstWeekFirstJulianday && targetMonthJulianToday <= lastWeekLastJulianday) {
    		// adapter�� selectdDay�� update �� ��� �Ѵ�
    		////////////////////////////////////////////////////////////////////////
    		mListFragment.mAdapter.setSelectedDayInEEMode(targetMonthTime.getTimeInMillis());
    		////////////////////////////////////////////////////////////////////////
    		
    		ObjectAnimator animator = null;
    		// ���� ã�ƾ� �Ѵ�...�ش� �ָ�...
    		for (int i=0; i<weekCounts; i++) {
    			MonthWeekView weekView = (MonthWeekView) getChildAt(i);
    			int firstJulianDay = ETime.getJulianDay(weekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    			int lastJulianDay = firstJulianDay + 6;
    			if (targetMonthJulianToday >= firstJulianDay && targetMonthJulianToday <= lastJulianDay) {
    				// bingo     				
    				animator = weekView.setSelectedDayAlphaAnimUnderExistenceInEEMode(targetMonthTime.get(Calendar.DAY_OF_WEEK), animDuration);    	
    				break;
    			}
    		}
    		
    		return animator;
    	}
    	else 
    		return null;
    }
    
    public ObjectAnimator hasMonthListViewPrvTargetMonthWeekDay(Calendar prvSelectedDayTime, long animDuration) {
    	
    	int weekCounts = getChildCount();
    	int firstWeek = getFirstVisiblePosition();
    	int lastWeek = getLastVisiblePosition();
    	int firstWeekFirstJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(firstWeek);//Utils.getJulianMondayFromWeeksSinceEpoch(firstWeek);    	
    	int lastWeekLastJulianday = ETime.getJulianMondayFromWeeksSinceEcalendarEpoch(lastWeek) + 6;//Utils.getJulianMondayFromWeeksSinceEpoch(lastWeek) + 6;
    	    	
    	int prvSelectedJulianDay = ETime.getJulianDay(prvSelectedDayTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
    	if (prvSelectedJulianDay >= firstWeekFirstJulianday && prvSelectedJulianDay <= lastWeekLastJulianday) {  		
    		
    		ObjectAnimator animator = null;
    		
    		for (int i=0; i<weekCounts; i++) {
    			MonthWeekView weekView = (MonthWeekView) getChildAt(i);
    			int firstJulianDay = ETime.getJulianDay(weekView.getFirstMonthTime().getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
    			int lastJulianDay = firstJulianDay + 6;
    			if (prvSelectedJulianDay >= firstJulianDay && prvSelectedJulianDay <= lastJulianDay) {
    				// bingo     				
    				animator = weekView.setPrvSelectedDayAlphaAnimUnderExistenceInEEMode(animDuration);    	
    				break;
    			}
    		}
    		
    		return animator;
    	}
    	else 
    		return null;
    } 
    
        
    int mCurrentEventListHeight;
    int mTargetEventListHeight;
    float mTotalValue;
    float mTotalAbsValue;
    
    ValueAnimator mEventListViewHeightScaleValueAnimator;
    long mEventListViewAnimDuration;
    public void ScaleEventListViewHeight(float velocityY) {
    	
		mTotalValue = mCurrentEventListHeight - mTargetEventListHeight;
		mTotalAbsValue = Math.abs(mTotalValue);
		
		mEventListViewHeightScaleValueAnimator = ValueAnimator.ofInt(mCurrentEventListHeight, mTargetEventListHeight);
		
		if (mTotalValue > 0)
			mEventListViewAnimDuration = calculateDuration(mTotalAbsValue, mCurrentEventListHeight, velocityY);
		else
			mEventListViewAnimDuration = calculateDuration(mTotalAbsValue, mTargetEventListHeight, velocityY);
		
		mEventListViewHeightScaleValueAnimator.setInterpolator(new ScaleTimeInterpolator(mEventListViewHeightScaleValueAnimator, mTotalAbsValue));		
		mEventListViewHeightScaleValueAnimator.setDuration(mEventListViewAnimDuration);			

		mEventListViewHeightScaleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height				
				int value = (Integer) valueAnimator.getAnimatedValue();
				int eventListViewScaleHeight = value;
				
				RelativeLayout.LayoutParams eventListViewParams = (RelativeLayout.LayoutParams) mListFragment.mEventsListView.getLayoutParams();
				eventListViewParams.height = eventListViewScaleHeight;
		    	mListFragment.mEventsListView.setLayoutParams(eventListViewParams);    				
			}
		});
		
		mEventListViewHeightScaleValueAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {				
			}

			@Override
			public void onAnimationStart(Animator animator) {				
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});		
    }  
    
    
    private static final int MINIMUM_VELOCITY = 3300;    
	private static final int DEFAULT_VELOCITY = MINIMUM_VELOCITY; // ������ ��Ʈ
    //private static final int DEFAULT_VELOCITY = 5500; // ������ S4
    private static long calculateDuration(float delta, float width, float velocity) {
        
    	/*
         * Here we compute a "distance" that will be used in the computation of
         * the overall snap duration. This is a function of the actual distance
         * that needs to be traveled; we keep this value close to half screen
         * size in order to reduce the variance in snap duration as a function
         * of the distance the page needs to travel.
         */
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        velocity = Math.max(MINIMUM_VELOCITY, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }    
    
    public static long calculateDurationForFling(float delta, float width, float velocity) {
        
    	/*
         * Here we compute a "distance" that will be used in the computation of
         * the overall snap duration. This is a function of the actual distance
         * that needs to be traveled; we keep this value close to half screen
         * size in order to reduce the variance in snap duration as a function
         * of the distance the page needs to travel.
         */
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }    
    
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
    
    private static class ScaleTimeInterpolator implements TimeInterpolator {
		ValueAnimator mAnimator;
    	float mAnimationDistance;
    	
        public ScaleTimeInterpolator(ValueAnimator animator, float animationDistance) {
        	mAnimator = animator;
        	mAnimationDistance = animationDistance;
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
        
        private void cancelAnimation() {        	
        	mAnimator.setDuration(0);
        	     	
        }
    }
    
    private static class FlingInterpolator implements TimeInterpolator {
		ValueAnimator mAnimator;
    	float mAnimationDistance;
    	
        public FlingInterpolator(ValueAnimator animator, float animationDistance) {
        	mAnimator = animator;
        	mAnimationDistance = animationDistance;
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            //if ((1 - t) * mAnimationDistance < 1) {
                //cancelAnimation();
            //}

            return t;
        }
        
        private void cancelAnimation() {        	
        	mAnimator.setDuration(0);
        	     	
        }
    }
}
