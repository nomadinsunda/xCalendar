package com.intheeast.year;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.SystemClock;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ListView;

public class YearListView extends ListView {
	
	public static final int UPWARD_FLING_MODE = 1;
    public static final int DOWNWARD_FLING_MODE = 2;
    
	Context mListContext;
	
	VelocityTracker mTracker;
    private int mMinimumVelocity;
    private static float mScale = 0;
	float mPhysicalCoeff;
	
	Calendar mTempTime;
	int mFirstDayOfWeek;
	int mLastDayOfWeek;
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	
	int mHeight;
	int mWidth;
	
	// Updates the time zone when it changes
    private final Runnable mTimezoneUpdater = new Runnable() {
        @SuppressLint("SuspiciousIndentation")
		@Override
        public void run() {
            if (mTempTime != null && mListContext != null) {
            	mTzId = Utils.getTimeZone(mListContext, mTimezoneUpdater);
            	mTimeZone = TimeZone.getTimeZone(mTzId);
            	mGmtoff = mTimeZone.getRawOffset() / 1000;
                ETime.switchTimezone(mTempTime, mTimeZone);//mTempTime.timezone = Utils.getTimeZone(mListContext, mTimezoneUpdater);               
            }
        }
    };
	
	
	public YearListView(Context context) {
        super(context);
        init(context);
    }

    public YearListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public YearListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    
    private void init(Context c) {
        mListContext = c;        
        
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
        mTempTime = GregorianCalendar.getInstance(mTimeZone);//new Time();
                
        if (mScale == 0) {
            mScale = c.getResources().getDisplayMetrics().density;
            if (mScale != 1) {            	
                //MULTIPLE_MONTH_VELOCITY_THRESHOLD *= mScale;
                //FLING_VELOCITY_DIVIDER *= mScale;
            }
        }
    }
    
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	//Log.i("tag", "MonthListView:onSizeChanged");    	
    	
    	mHeight = h;
    	mWidth = w;
    	
		super.onSizeChanged(w, h, oldw, oldh);
	}    
    
    YearPickerFragment mListFragment;
    int mOneYearHeight;
    int mAnticiationHeight;
    
	public void setListFragment(YearPickerFragment listFragment) {
		mListFragment = listFragment;
		
		mAnticiationHeight = mListFragment.mAnticipationListViewHeight;
		mWidth = mListFragment.mAnticipationListViewWidth;
		
		mOneYearHeight = (mListFragment.mAnticipationYearIndicatorHeight + mListFragment.mAnticipationMiniMonthHeight) + 
				(mListFragment.mAnticipationMiniMonthHeight * 3);
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
    public boolean onTouchEvent(MotionEvent ev) {  
		return ProcessEvent(ev) || super.onTouchEvent(ev);
	}
	
	@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	return ProcessEvent(ev) || super.onInterceptTouchEvent(ev);    	
    }  
	
	boolean mComputingFling = false;
	Object mFlingSync = new Object();
	long mDownActionTime;
	float mFlingVelocity;
	private boolean ProcessEvent (MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_CANCEL:
        	//Log.i("tag", "MonthListView : processEvent : ACTION_CANCEL");
            return false;
        // Start tracking movement velocity
        case MotionEvent.ACTION_DOWN:      
        	mTracker.clear();
            mDownActionTime = SystemClock.uptimeMillis();            
            
            mTracker.clear();
            mDownActionTime = SystemClock.uptimeMillis();
            
        	//if (MonthListView.this.mListFragment.mCurrentScrollState == OnScrollListener.SCROLL_STATE_FLING) {
            if (mComputingFling) {
            	synchronized (mFlingSync) {
            		
            		//smoothScrollBy(0, 0);
            		mComputingFling = false;
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
        case MotionEvent.ACTION_UP:            	
            mTracker.addMovement(ev);
            mTracker.computeCurrentVelocity(1000);    // in pixels per second
            
            float vel =  mTracker.getYVelocity();
            if (Math.abs(vel) > mMinimumVelocity) {
            	MotionEvent cancelEvent = MotionEvent.obtain(mDownActionTime,  SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_CANCEL, 0, 0, 0);
                onTouchEvent(cancelEvent);
                
            	if (!mComputingFling) {
            		mComputingFling = true;            		
            	}
            	
            	mFlingVelocity = vel;            	
                
            	postOnAnimation(mFlingRun);
            	
            	return true;
            }
            else {
            	
            }
            
            break;
        default:
            mTracker.addMovement(ev);
            break;
		}
		
		return false;
	}
	
	private final Runnable mFlingRun = new Runnable() {
        public void run() { 
        	DoFling(mFlingVelocity);
        }
    };
	
    private void DoFling(float velocityY) {
    	                
        if (velocityY > 0) { // downward fling        	
        	//Log.i("tag", "call computeDownwardFling");
        	computeDownwardFling((int)velocityY);
        }
        else if (velocityY < 0){ // upward fling        	     
        	//Log.i("tag", "call computeUpwardFling");
        	computeUpwardFling((int)velocityY);        	
        }
        else {        	
        	return;
        }        
    }    
    
    public void stopFlingInNMode() {
    	synchronized (mFlingSync) {
    		mComputingFling = false;
        	//removeCallbacks(mNormalModeFlingRunnable);       
    	}
    }
    
    
    
    static final int MAX_ROW_INDEX = 3;
    public UnvisibleMonthTablePart calcUnvisibleMonthTablePartOfVisibleFirstMonthTable() {
    	
    	YearMonthTableLayout child = (YearMonthTableLayout) getChildAt(0);		
    	Calendar firstMonthTime = GregorianCalendar.getInstance(child.getFirstMonthTime().getTimeZone());//child.getFirstMonthTime();
    	firstMonthTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());
    	int firstMonth = firstMonthTime.get(Calendar.MONTH);
		int monthsViewPattern = child.getMonthsPattern();
		int rowIndex = getRowInex(firstMonth);
				
		Calendar TempCalendar = GregorianCalendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);	
		firstMonthTime.set(Calendar.MONTH, 0);
		firstMonthTime.set(Calendar.DAY_OF_MONTH, 1);
		
		TempCalendar.setTimeInMillis(firstMonthTime.getTimeInMillis());
				
		UnvisibleMonthTablePart obj = null;
		
		//int visibleYearHeight = 0;
		int unVisibleYearHeight = 0;
    	if (monthsViewPattern == YearMonthTableLayout.NORMAL_YEAR_MONTHS_PATTERN) {  
    		// ���࿡ child top�� row index�� 1�̶��, child top ���� row�� �� �� �ִ�
    		// ���࿡ child top�� row index�� 2�̶��, child top ���� row�� �� �� �ִ�
    		int remainingMonthRow = rowIndex;     		         
            unVisibleYearHeight = Math.abs(child.getTop()) 
            		+ (mListFragment.mAnticipationMiniMonthHeight * remainingMonthRow) 
            		+ mListFragment.mAnticipationYearIndicatorHeight;     		
    	}
    	else {    
    		//visibleYearHeight = mOneYearHeight - child.getTop();
    		//unVisibleYearHeight = mOneYearHeight - visibleYearHeight;
    		unVisibleYearHeight = Math.abs(child.getTop());     
    	} 
    	
    	long completionYearMillis = TempCalendar.getTimeInMillis();
        obj = new UnvisibleMonthTablePart(unVisibleYearHeight, completionYearMillis);
        
    	return obj;
    }
       
    
    public class UnvisibleMonthTablePart {
    	int mUnvisibleYearHeight;
    	long mCompletionYearMillis;
    	
    	public UnvisibleMonthTablePart(int unVisibleYearHeight, long completionYearMillis) {
    		mUnvisibleYearHeight = unVisibleYearHeight;
    		mCompletionYearMillis = completionYearMillis;
    	}
    } 
    
    public UnvisibleMonthTablePart calcUnvisibleMonthTablePartOfVisibleLastMonthTable() {
    	
    	int firstVisiblePosition = getFirstVisiblePosition();
    	int lastVisiblePosition = getLastVisiblePosition();
    	int lastChildViewIndex = lastVisiblePosition - firstVisiblePosition;
    	
    	YearMonthTableLayout child = (YearMonthTableLayout) getChildAt(lastChildViewIndex);		
    	
    	Calendar firstMonthTime = GregorianCalendar.getInstance(child.getFirstMonthTime().getTimeZone());//child.getFirstMonthTime();
    	firstMonthTime.setTimeInMillis(child.getFirstMonthTime().getTimeInMillis());
    	int firstMonth = firstMonthTime.get(Calendar.MONTH);//.month;
		int monthsViewPattern = child.getMonthsPattern();
		int rowIndex = getRowInex(firstMonth);
		
		Calendar TempCalendar = Calendar.getInstance(mTimeZone);
		TempCalendar.setFirstDayOfWeek(mFirstDayOfWeek);	
		firstMonthTime.set(Calendar.MONTH, 0);//firstMonthTime.month = 0;
		firstMonthTime.set(Calendar.DAY_OF_MONTH, 1);//firstMonthTime.monthDay = 1;
		
		TempCalendar.setTimeInMillis(firstMonthTime.getTimeInMillis());
				
		UnvisibleMonthTablePart obj = null;
		int visibleYearHeight = 0;
		int unVisibleYearHeight = 0;
		
    	if (monthsViewPattern == YearMonthTableLayout.NORMAL_YEAR_MONTHS_PATTERN) {      		  	
    		unVisibleYearHeight = child.getBottom() - mOneYearHeight;     	
            int remainingMonthRow = MAX_ROW_INDEX - rowIndex;
            unVisibleYearHeight = unVisibleYearHeight + (mListFragment.mAnticipationMiniMonthHeight * remainingMonthRow);            
    	}
    	else {    		    		
    		visibleYearHeight = mOneYearHeight - child.getTop();
    		unVisibleYearHeight = mOneYearHeight - visibleYearHeight;
    	}  
    	
    	long completionYearMillis = TempCalendar.getTimeInMillis();
        obj = new UnvisibleMonthTablePart(unVisibleYearHeight, completionYearMillis);
        
    	return obj;
    }
    
    private FlingParameter adjustDownwardFlingDistance (int originalDistance, int accumulatedYearHeightOfPassedVisibleRegion) {    	
		FlingParameter flingObj = null;
    	int newVelocity = 0;
    	    	
    	int visibleYearHeight = accumulatedYearHeightOfPassedVisibleRegion;
    	int unVisibleYearHeight = mOneYearHeight - visibleYearHeight;
    	int visibleYearBottom = visibleYearHeight;
    	
    	int listViewHalfLine = mHeight / 2;
    	
    	int adjustedDistance = 0;
    	int willBeAddDistance = 0;
    	if (visibleYearBottom >= listViewHalfLine) {
    		// stop�� year�� listview�� top�� ��ġ��Ų��
    		// distance�� ������Ų��
    		willBeAddDistance = unVisibleYearHeight;
    		adjustedDistance = originalDistance + willBeAddDistance;    		
    		//Log.i("tag","�ش� year�� top�� ��ġ");
    		
    	}
    	else {
    		// stop�� year�� ���� year�� listview�� top�� ��ġ��Ų��
    		willBeAddDistance = -visibleYearBottom;
    		adjustedDistance = originalDistance + willBeAddDistance;
    		//Log.i("tag","next year�� top�� ��ġ");
    		
    	}   	
    	    	
    	newVelocity = getSplineFlingAbsVelocity(adjustedDistance);    	
    	flingObj = new FlingParameter(newVelocity, adjustedDistance);
    	
    	return flingObj;
    }   
    
    private FlingParameter adjustUpwardFlingDistance (int originalDistance, int accumulatedMovedUpwardYearHeight) {    	
    	FlingParameter flingObj = null;
    	
    	int newVelocity = 0;
    	int visibleYearHeight = accumulatedMovedUpwardYearHeight;
    	int visibleYearTop = mHeight - visibleYearHeight;    	
    	
    	int listViewHalfLine = mHeight / 2;   	
    	
    	int adjustedDistance = 0;
    	int willBeAddDistance = 0;
    	if (visibleYearTop <= listViewHalfLine) {    		
    		willBeAddDistance = visibleYearTop;
    		adjustedDistance = originalDistance + willBeAddDistance;
    		//Log.i("tag", "visibleYearTop <= listViewHalfLine");    		 
    	}
    	else {
    		willBeAddDistance = visibleYearTop - mHeight;
    		adjustedDistance = originalDistance + willBeAddDistance;
    		//Log.i("tag", "visibleYearTop > listViewHalfLine");   				
    	}    	 	
    	
    	newVelocity = getSplineFlingAbsVelocity(adjustedDistance);   	
    	flingObj = new FlingParameter(newVelocity, adjustedDistance);
    	    	
    	return flingObj;
    }
    
    private void computeDownwardFling(int velocityY) {
    	
    	
    	final int originalDistance = (int)getSplineFlingDistance((int)velocityY);    	
    	
    	UnvisibleMonthTablePart obj = calcUnvisibleMonthTablePartOfVisibleFirstMonthTable();
    	int accumulatedDistance = obj.mUnvisibleYearHeight;    	
    	
    	Calendar YearTimeCalendar = GregorianCalendar.getInstance(mTempTime.getTimeZone());        
    	YearTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
    	YearTimeCalendar.setTimeInMillis(obj.mCompletionYearMillis);       
        
    	boolean continuing = true;
        final int firstMonthRowIndex = 1;     
        
    	while (continuing) {        		        
    		YearTimeCalendar.add(Calendar.YEAR, -1);    	       	
        	
        	int accumulatedMovedDownwardYearHeight = 0;        	        
            for (int i=4; i>0; i--) {            	
            	
            	if (i == firstMonthRowIndex) {
            		accumulatedMovedDownwardYearHeight = accumulatedMovedDownwardYearHeight + 
            				mListFragment.mAnticipationYearIndicatorHeight + mListFragment.mAnticipationMiniMonthHeight;
            		accumulatedDistance = accumulatedDistance + 
            				mListFragment.mAnticipationYearIndicatorHeight + mListFragment.mAnticipationMiniMonthHeight;
            	}            	     	
            	else {
            		accumulatedMovedDownwardYearHeight = accumulatedMovedDownwardYearHeight + mListFragment.mAnticipationMiniMonthHeight;
            		accumulatedDistance = accumulatedDistance + mListFragment.mAnticipationMiniMonthHeight;
            	}            	
            	
            	// ������ month row height�� ������ �� ���� �ʰ� ���θ� Ȯ���Ѵ�
            	if (accumulatedDistance >= originalDistance) {            		
            		
            		int exceedDistance = accumulatedDistance - originalDistance;
            		accumulatedMovedDownwardYearHeight = accumulatedMovedDownwardYearHeight - exceedDistance;      
            		
            		Calendar breakTime = GregorianCalendar.getInstance(mTempTime.getTimeZone());
            		breakTime.setTimeInMillis(YearTimeCalendar.getTimeInMillis());            		
            		
            		FlingParameter flingObj = adjustDownwardFlingDistance(originalDistance, accumulatedMovedDownwardYearHeight);//new FlingParameter(velocityY, originalDistance);
            		float delta = flingObj.mDistance;
                	float maxDelta = delta * 2; 
                	
                	mListFragment.setFlingContext(DOWNWARD_FLING_MODE, flingObj.mDistance, calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());
            		
                    continuing = false;
                    
                    mListFragment.kickOffFlingComputationRunnable();
            		break;
            	}
            }           
        }    	
    }
    
    private void computeUpwardFling(int velocityY) {  
		
		final int originalDistance = (int)getSplineFlingDistance((int)velocityY);    	
				
		UnvisibleMonthTablePart obj = calcUnvisibleMonthTablePartOfVisibleLastMonthTable();
    	int accumulatedDistance = obj.mUnvisibleYearHeight;    	    	
    	
    	Calendar YearTimeCalendar = GregorianCalendar.getInstance(mTimeZone);        
    	YearTimeCalendar.setFirstDayOfWeek(mFirstDayOfWeek);
    	YearTimeCalendar.setTimeInMillis(obj.mCompletionYearMillis);
        
        boolean continuing = true;        
        final int firstMonthRowIndex = 0;     
        while (continuing) {
        	YearTimeCalendar.add(Calendar.YEAR, 1); 
        	
        	int accumulatedMovedUpwardYearHeight = 0;       	       
            
            for (int i=firstMonthRowIndex; i<4; i++) {
            	
            	if (i == firstMonthRowIndex) {
            		accumulatedMovedUpwardYearHeight = accumulatedMovedUpwardYearHeight + 
            				mListFragment.mAnticipationYearIndicatorHeight + mListFragment.mAnticipationMiniMonthHeight;
            		accumulatedDistance = accumulatedDistance + 
            				mListFragment.mAnticipationYearIndicatorHeight + mListFragment.mAnticipationMiniMonthHeight;
            	}            	     	
            	else {
            		accumulatedMovedUpwardYearHeight = accumulatedMovedUpwardYearHeight + mListFragment.mAnticipationMiniMonthHeight;
            		accumulatedDistance = accumulatedDistance + mListFragment.mAnticipationMiniMonthHeight;
            	}            	
            	            	
            	if (accumulatedDistance >= originalDistance) {	            		
            		
            		int exceedDistance = accumulatedDistance - originalDistance;
            		accumulatedMovedUpwardYearHeight = accumulatedMovedUpwardYearHeight - exceedDistance;                  		
                	
            		FlingParameter flingObj = adjustUpwardFlingDistance(originalDistance, accumulatedMovedUpwardYearHeight);
            		float delta = flingObj.mDistance;
                	float maxDelta = delta * 2;  
            		                   
                	mListFragment.setFlingContext(UPWARD_FLING_MODE, flingObj.mDistance, calculateDurationForFling(delta, maxDelta, flingObj.mVelocity), System.currentTimeMillis());
            		           		
                    continuing = false;
                    
                    mListFragment.kickOffFlingComputationRunnable(); 
                    
            		break;
            	}
            }        	
        }               
    }    
	
    public int getRowInex(int firstMonthNumber) {
    	int rowIndex = 0;
    	switch(firstMonthNumber) {
    	case 0:
    		rowIndex = 0;
    		break;
    	case 3:
    		rowIndex = 1;
    		break;
    	case 6:
    		rowIndex = 2;
    		break;
    	case 9:
    		rowIndex = 3;
    		break;
    	}
    	return rowIndex;
    }
    
    public static class FlingParameter {
    	int mVelocity;
    	int mDistance;
    	
    	public FlingParameter(int velocity, int distance) {
    		mVelocity = velocity;
    		mDistance = distance;
    	}
    }    
    
    
    
    // Fling friction
    private float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    
    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }
    
    public double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }
    
    // distance : mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l)
    public int getSplineFlingAbsVelocity(double distance) {
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
    
    public static long calculateDurationForFling(float delta, float width, float velocity) {
        
    	
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }    
    
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }  
    
    
}
