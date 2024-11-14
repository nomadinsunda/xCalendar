package com.intheeast.acalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


import com.intheeast.etc.ETime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.RelativeLayout;

public class CircularMonthDayHeaderView extends View{
    
	private final Typeface mBold = Typeface.DEFAULT_BOLD; 
	
	private static float MONTHDAY_HEADER_FONT_SIZE;
    private static int MONTHDAY_HEADER_BOTTOM_MARGIN;   
    
    int mWhichView = 0;
    Context mContext;
    CalendarViewsSecondaryActionBar mCalendarViewsSecondaryActionBar;
    RelativeLayout mParent;
    
	int mViewWidth;    	
	int mMonthDayTextWidth;
	int mLeftSideMargin;	
	
	Calendar mSelectedDayOfMonthDayHeader;		
	Calendar mBaseDate;
	
	int mFirstJulianDay;
	int mLastJulianDay;
	int mMonthLength;
	int mVisibleMonth;
	int mFirstVisibleDate;
	int mFirstVisibleDayOfWeek;
	
    private final Paint mPaint = new Paint();
    
    int mKickOffLeftX;
	float mCurrentLeftX;

	int TEXT_SIZE_MONTH_NUMBER;
	TimeZone mTimeZone;
	int mFirstDayOfWeek;
	
	public CircularMonthDayHeaderView (Context context, CalendarViewsSecondaryActionBar actionBar, RelativeLayout parent, Calendar time, int whichView) {
		super(context);		
		mContext = context;
		mCalendarViewsSecondaryActionBar = actionBar;
		
		mTimeZone = mCalendarViewsSecondaryActionBar.mTimeZone;
		mFirstDayOfWeek = mCalendarViewsSecondaryActionBar.mFirstDayOfWeek;
		mWhichView = whichView;
		
		mSelectedDayOfMonthDayHeader = GregorianCalendar.getInstance(time.getTimeZone());
		mSelectedDayOfMonthDayHeader.setFirstDayOfWeek(time.getFirstDayOfWeek());
		mSelectedDayOfMonthDayHeader.setTimeInMillis(time.getTimeInMillis());
		
		makeWeekTimeInformation(time);
		
		mParent = parent;			
		
		TEXT_SIZE_MONTH_NUMBER = mCalendarViewsSecondaryActionBar.TEXT_SIZE_MONTH_NUMBER;
		
		MONTHDAY_HEADER_FONT_SIZE = TEXT_SIZE_MONTH_NUMBER;
		MONTHDAY_HEADER_BOTTOM_MARGIN = (int) context.getResources().getDimension(R.dimen.monthday_header_bottom_margin); //6dip
	}		
	
	public void reset(Calendar time, int whichView) {
		mWhichView = whichView;
		
		mSelectedDayOfMonthDayHeader = GregorianCalendar.getInstance(time.getTimeZone());
		mSelectedDayOfMonthDayHeader.setFirstDayOfWeek(time.getFirstDayOfWeek());
		mSelectedDayOfMonthDayHeader.setTimeInMillis(time.getTimeInMillis());
		
		makeWeekTimeInformation(time);
		
		resetPositionX();
	}
	
	
	@Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {			
		mViewWidth = width;
		int effectiveWidth = mViewWidth;
		mMonthDayTextWidth = effectiveWidth / 7;
		mLeftSideMargin = mMonthDayTextWidth / 2;
		
		computeMonthDaysXRegion();
		
		switch(mWhichView) {
		case CalendarViewsSecondaryActionBar.PREVIOUS_WEEK_SIDE_VIEW:
			mKickOffLeftX = 0 - mViewWidth;
			break;
		case CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW:
			mKickOffLeftX = 0;
			break;
		case CalendarViewsSecondaryActionBar.NEXT_WEEK_SIDE_VIEW:
			mKickOffLeftX = mViewWidth;
			break;
		}			
		
		mCurrentLeftX = mKickOffLeftX;
		
		//setTranslationX(mKickOffLeftX);
		setX(mCurrentLeftX);
    }		
	
	public void updateTimeZone(TimeZone tz) {
		mSelectedDayOfMonthDayHeader.setTimeZone(tz);			
		
		mBaseDate.setTimeZone(tz);				
	}
	
	public void setSelectedTime(long millis) {
		mSelectedDayOfMonthDayHeader.setTimeInMillis(millis);
		mSelectedDayOfMonthDayHeader.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDayOfMonthDayHeader.set(Calendar.MINUTE, 0);
		mSelectedDayOfMonthDayHeader.set(Calendar.SECOND, 0);
		mSelectedDayOfMonthDayHeader.set(Calendar.MILLISECOND, 0);	
	}		
			
	public void setSelectedTimeForTranslateAnimation(int goToDate) {
		//mSelectedDayOfMonthDayHeader.monthDay += goToDate;
		mSelectedDayOfMonthDayHeader.add(Calendar.DAY_OF_MONTH, goToDate);			
	}
	
	public void setSelectedTimeForGoToToday(long goToTodayTime) {
		mSelectedDayOfMonthDayHeader.setTimeInMillis(goToTodayTime);
		mSelectedDayOfMonthDayHeader.set(Calendar.HOUR_OF_DAY, 0);
		mSelectedDayOfMonthDayHeader.set(Calendar.MINUTE, 0);
		mSelectedDayOfMonthDayHeader.set(Calendar.SECOND, 0);
		mSelectedDayOfMonthDayHeader.set(Calendar.MILLISECOND, 0);	
		//mSelectedDay.normalize(false);			
	}
	
	public void makeWeekTimeInformation(Calendar time) {
		if (mBaseDate == null) {
			mBaseDate = GregorianCalendar.getInstance(time.getTimeZone());
			mBaseDate.setFirstDayOfWeek(time.getFirstDayOfWeek());
			mBaseDate.setTimeInMillis(time.getTimeInMillis());
			
		}
		else {
			mBaseDate.setFirstDayOfWeek(time.getFirstDayOfWeek());
			mBaseDate.setTimeInMillis(time.getTimeInMillis());
		}
					
		mBaseDate.set(Calendar.HOUR_OF_DAY, 0);
		mBaseDate.set(Calendar.MINUTE, 0);
		mBaseDate.set(Calendar.SECOND, 0);
		mBaseDate.set(Calendar.MILLISECOND, 0);	
        // Set the base date to the beginning of the week 
    	// if we are displaying 7 days at a time.
        	        
        ETime.adjustStartDayInWeek(mBaseDate);
        
        switch(mWhichView) {
		case CalendarViewsSecondaryActionBar.PREVIOUS_WEEK_SIDE_VIEW:				
			mBaseDate.add(Calendar.DAY_OF_MONTH, -7);			
			break;
		case CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW:				
			break;
		case CalendarViewsSecondaryActionBar.NEXT_WEEK_SIDE_VIEW:
			mBaseDate.add(Calendar.DAY_OF_MONTH, 7);			
			break;
		}	        
        
        final long start = mBaseDate.getTimeInMillis();
        //long gmtoff = mTimeZone.getRawOffset() / 1000;
        mFirstJulianDay = ETime.getJulianDay(start, mTimeZone, mFirstDayOfWeek);
        mLastJulianDay = mFirstJulianDay + 6;

        mMonthLength = mBaseDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        mVisibleMonth = mBaseDate.get(Calendar.MONTH);
        mFirstVisibleDate = mBaseDate.get(Calendar.DAY_OF_MONTH);//mBaseDate.monthDay;
        mFirstVisibleDayOfWeek = mBaseDate.get(Calendar.DAY_OF_WEEK);//mBaseDate.weekDay;	        
    }	
	
	public int getFirstJulianDay() {
		return mFirstJulianDay;
	}
	
	public int getLastJulianDay() {
		return mLastJulianDay;
	}
	
	public Calendar getSelectedDayTime() {
		return mSelectedDayOfMonthDayHeader;
	}
	
	public Calendar getBaseTime() {
		return mBaseDate;
	}
		
	public void resetPositionX() {
		switch(mWhichView) {
		case CalendarViewsSecondaryActionBar.PREVIOUS_WEEK_SIDE_VIEW:
			mKickOffLeftX = 0 - mViewWidth;				
			break;
		case CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW:
			mKickOffLeftX = 0;				
			break;
		case CalendarViewsSecondaryActionBar.NEXT_WEEK_SIDE_VIEW:
			mKickOffLeftX = mViewWidth;				
			break;
		}			
		
		mCurrentLeftX = mKickOffLeftX;
		setX(mCurrentLeftX);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public void adjustPositionXForNewViewType(int switchWhichView) {
	
		mWhichView = switchWhichView;
		
		switch(switchWhichView) {
		case CalendarViewsSecondaryActionBar.PREVIOUS_WEEK_SIDE_VIEW:
			mKickOffLeftX = 0 - mViewWidth;				
			break;
		case CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW:
			mKickOffLeftX = 0;				
			break;
		case CalendarViewsSecondaryActionBar.NEXT_WEEK_SIDE_VIEW:
			mKickOffLeftX = mViewWidth;				
			break;
		}			
	
		mCurrentLeftX = mKickOffLeftX;
		setX(mCurrentLeftX);
		//this.invalidate();
	}		

	public void setPositionX(float translationX) {
		mCurrentLeftX = mCurrentLeftX + translationX;
		setX(mCurrentLeftX);
	}
	
	public float getPositionX() {
		return mCurrentLeftX;			
	}
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////		
	TranslateAnimation mTranslateInPlaceAnimation;
	AnimationListener mTranslateInPlaceAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {	
			Log.i("tag", "mTranslateInPlaceAnimationListener::onAnimationStart");
		}

		@Override
		public void onAnimationEnd(Animation animation) {				// ���� 	
			// Ʈ�����ִϸ��̼��� ������ �� setX(0);�� �����Ͽ��� ������...���� ����!!!
			resetPositionX();				
			mCalendarViewsSecondaryActionBar.updateViewStateIfInPlaceAnimationDone(CircularMonthDayHeaderView.this.mWhichView);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}			
	};		
	public TranslateAnimation makeInPlaceAnimation() {			
		float fromXDelta = mCurrentLeftX;
		float toXDelta = mKickOffLeftX;
		
		float delta = 0;
		if(mWhichView == CalendarViewsSecondaryActionBar.PREVIOUS_WEEK_SIDE_VIEW)  {				
			delta = Math.abs(mKickOffLeftX) - Math.abs(mCurrentLeftX);
		}
		else if(mWhichView == CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW) {				
			delta = Math.abs(mCurrentLeftX);
		}
		else if (mWhichView == CalendarViewsSecondaryActionBar.NEXT_WEEK_SIDE_VIEW) {				
			delta = mKickOffLeftX - mCurrentLeftX;
		}
		
		mTranslateInPlaceAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(delta, delta, mViewWidth, CalendarViewsSecondaryActionBar.DEFAULT_ANIMATION_VELOCITY, 
				Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);			
		mTranslateInPlaceAnimation.setAnimationListener(mTranslateInPlaceAnimationListener);	
		mTranslateInPlaceAnimation.setFillAfter(true);
		// setX�� �Ķ���͸� 0���� �����ϴ� ������ 
		// ���� Animation.ABSOLUTE, fromXDelta ����
		// ���� view�� left ���� 0�� ���� �������� offset?���� ����? �Ǿ��� �����̴�
		// ���� mCurrentLeftX ��[���� view�� left ��]�� �������� �Ѵٸ�
		// fromXDelta�� 0�� �� ���̰�,
		// toXDelta�� View width - mCurrentLeftX ���� �ɰ��̴�
		setX(0); 		
		
		return mTranslateInPlaceAnimation;			
	}
	public void startInPlaceAnimation() {
		
		startAnimation(mTranslateInPlaceAnimation);
		Log.i("tag", "startInPlaceAnimation");
	}
	////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	TranslateAnimation mTranslateOutgoingToLeftAnimation;
	AnimationListener mTranslateOutgoingToLeftAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {					
		}

		@Override
		public void onAnimationEnd(Animation animation) {								
			mCalendarViewsSecondaryActionBar.updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone(CalendarViewsSecondaryActionBar.TRANSLATE_PREVIOUS_WEEK_ANIMATION_COMPLETE);				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}			
	};		
	public TranslateAnimation makeOutgoingToLeftAnimation(int whatVelocity, float velocityOrDuration) {
		float fromXDelta = mCurrentLeftX;
		float toXDelta = mKickOffLeftX - mViewWidth; // ���� 
		
		if (whatVelocity == CalendarViewsSecondaryActionBar.SUPPORTED_DURATION) {
			long duration = (long)velocityOrDuration;
			mTranslateOutgoingToLeftAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(duration, Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);
		}
		else {
			float delta = 0;
			if(mWhichView == CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW) {				
				delta = mViewWidth - Math.abs(mCurrentLeftX);
			}		
			
			float velocity = velocityOrDuration;
			mTranslateOutgoingToLeftAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(delta, delta, mViewWidth, velocity, 
					Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);				
		}
		
		mTranslateOutgoingToLeftAnimation.setAnimationListener(mTranslateOutgoingToLeftAnimationListener);
		mTranslateOutgoingToLeftAnimation.setFillAfter(true);
		setX(0); // Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�
		
		return mTranslateOutgoingToLeftAnimation;
	}
	public void startOutgoingToLeftAnimation() {
		startAnimation(mTranslateOutgoingToLeftAnimation);
	}
	////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////
	TranslateAnimation mTranslateOutgoingToRightAnimation;
	AnimationListener mTranslateOutgoingToRightAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {					
		}

		@Override
		public void onAnimationEnd(Animation animation) {					
			mCalendarViewsSecondaryActionBar.updateSelectedTimeIfPreviousWeekToVisibleWeekAnimationDone(CalendarViewsSecondaryActionBar.TRANSLATE_NEXT_WEEK_ANIMATION_COMPLETE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}			
	};
	public TranslateAnimation makeOutgoingToRightAnimation(int whatVelocity, float velocityOrDuration) {
		float fromXDelta = mCurrentLeftX;
		float toXDelta = mViewWidth;
			
		if (whatVelocity == CalendarViewsSecondaryActionBar.SUPPORTED_DURATION) {
			long duration = (long)velocityOrDuration;
			mTranslateOutgoingToRightAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(duration, Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);
		}
		else {
			float delta = 0;
			if(mWhichView == CalendarViewsSecondaryActionBar.VISIBLE_WEEK_VIEW) {				
				delta = mViewWidth - Math.abs(mCurrentLeftX);
			}		
			
			float velocity = velocityOrDuration;
			mTranslateOutgoingToRightAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(delta, delta, mViewWidth, velocity, 
					Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);				
		}
		
		mTranslateOutgoingToRightAnimation.setAnimationListener(mTranslateOutgoingToRightAnimationListener);	
		//mTranslateOutgoingToRightAnimation.setFillEnabled(true);
		mTranslateOutgoingToRightAnimation.setFillAfter(true);
		setX(0); // Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�
		
		return mTranslateOutgoingToRightAnimation;
	}
	public void startOutgoingToRightAnimation() {
		this.startAnimation(mTranslateOutgoingToRightAnimation);
	}
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	TranslateAnimation TranslateIncomingFromLeftAnimation;		
	AnimationListener mTranslateIncomingFromLeftAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {					
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			//VISIBLE_WEEK_VIEW				
			//CircularMonthDayHeaderView.this.adjustPositionXForNewViewType(VISIBLE_WEEK_VIEW);	
			// �Ʒ� �Լ��� ���ʿ��� mSelectedDay �ߺ� ������ ���ϴ� �ڵ带 ��������				
			mCalendarViewsSecondaryActionBar.updateSelectedTimeIfPreviousWeekToVisibleWeekAnimationDone(CalendarViewsSecondaryActionBar.TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE);				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}			
	};		
	public TranslateAnimation makeIncomingFromLeftAnimation(int whatVelocity, float velocityOrDuration) {			
		float fromXDelta = mCurrentLeftX;
		float toXDelta = 0;
					
		if (whatVelocity == CalendarViewsSecondaryActionBar.SUPPORTED_DURATION) {
			long duration = (long)velocityOrDuration;
			TranslateIncomingFromLeftAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(duration, Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);
		}
		else {
			float delta = 0;
			if(mWhichView == CalendarViewsSecondaryActionBar.PREVIOUS_WEEK_SIDE_VIEW) {				
				delta = Math.abs(mCurrentLeftX);
			}		
			
			float velocity = velocityOrDuration;
			TranslateIncomingFromLeftAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(delta, delta, mViewWidth, velocity, 
					Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);				
		}
		
		TranslateIncomingFromLeftAnimation.setAnimationListener(mTranslateIncomingFromLeftAnimationListener);
		//TranslateIncomingFromLeftAnimation.setFillEnabled(true);
		TranslateIncomingFromLeftAnimation.setFillAfter(true);
		setX(0); // Animation.ABSOLUTE, fromXDelta ���� ���� ���� x �����ǿ� ������� ������ ����Ǳ� ������ 0���� ������ �ش�
		
		return TranslateIncomingFromLeftAnimation;	
	}
	public void startIncomingFromLeftAnimation() {
		startAnimation(TranslateIncomingFromLeftAnimation);
	}
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////		
	TranslateAnimation mTranslateIncomingFromRightAnimation;
	AnimationListener mTranslateIncomingFromRightAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
			//:�۷ι�  mSelectedDay�� ����Ǿ�� �� : ���� �ؾ� �ϴ°�???
			//:next week�� mWhichView = VISIBLE_WEEK_VIEW�� ����				
			//:next week�� obj.mKickOffLeftX & mCurrentLeftX�� VISIBLE_WEEK_VIEW �������� ���� ����Ǿ�� ��							
			//CircularMonthDayHeaderView.this.adjustPositionXForNewViewType(VISIBLE_WEEK_VIEW);		
			mCalendarViewsSecondaryActionBar.updateSelectedTimeIfNextWeekToVisibleWeekAnimationDone(CalendarViewsSecondaryActionBar.TRANSLATE_VISIBLE_WEEK_ANIMATION_COMPLETE);				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}			
	};
	
	public TranslateAnimation makeIncomingFromRightAnimation(int whatVelocity, float velocityOrDuration) {
		float fromXDelta = mCurrentLeftX;
		float toXDelta = 0;
					
		if (whatVelocity == CalendarViewsSecondaryActionBar.SUPPORTED_DURATION) {
			long duration = (long)velocityOrDuration;
			mTranslateIncomingFromRightAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(duration, Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);
		}
		else {
			float delta = 0;
			if(mWhichView == CalendarViewsSecondaryActionBar.NEXT_WEEK_SIDE_VIEW) {				
				delta = Math.abs(mCurrentLeftX);
			}		
			
			float velocity = velocityOrDuration;
			mTranslateIncomingFromRightAnimation = CalendarViewsSecondaryActionBar.makeSlideAnimation(delta, delta, mViewWidth, velocity, 
					Animation.ABSOLUTE, fromXDelta, Animation.ABSOLUTE, toXDelta);				
		}
		
		mTranslateIncomingFromRightAnimation.setAnimationListener(mTranslateIncomingFromRightAnimationListener);			
		mTranslateIncomingFromRightAnimation.setFillAfter(true);
		// setX�� �Ķ���͸� 0���� �����ϴ� ������ 
		// ���� Animation.ABSOLUTE, fromXDelta ����
		// ���� view�� left ���� 0�� ���� �������� offset?���� ����? �Ǿ��� �����̴�
		// ���� mCurrentLeftX ��[���� view�� left ��]�� �������� �Ѵٸ�
		// fromXDelta�� 0�� �� ���̰�,
		// toXDelta�� View width - mCurrentLeftX ���� �ɰ��̴�
		setX(0); 
		
		return mTranslateIncomingFromRightAnimation;	
	}
	
	
	public void startIncomingFromRightAnimation() {
		startAnimation(mTranslateIncomingFromRightAnimation);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////		
	boolean mApplyGoToDateInsideWeekAnimation = false; // ���� current visible week ������ goToDate�� �߻��� ��
	boolean mApplyGoToDateOutsideWeekAnimation = false;	// ���� current visible week���� prv/next week�� selected day�� ����[��/�� ���� ��/��]
	boolean mApplyGoToDateFromAnotherWeekAnimation = false;	// prv/next week�� ��/���� selected day�� ��
	
	@Override
    protected void onDraw(Canvas canvas) {
		
		drawMonthDayHeader(canvas);	
		
		// mGoToDateAnimation���� animation duration�� ��� �Һ�ɶ����� ��� invalidate�� ȣ���Ͽ�
		// onDraw�� ȣ���
		// goToDate���� true�� ����
		if (mApplyGoToDateInsideWeekAnimation) {
        	post(mGoToDateInsideWeekAnimation);	        	
        }
		else if (mApplyGoToDateOutsideWeekAnimation) {
			post(mGoToDateOutsideWeekAnimation);
		}
		else if (mApplyGoToDateFromAnotherWeekAnimation) {
			post(mGoToDateFromAnotherWeekAnimation);
		}
	}		
	
	private void drawMonthDayHeader(Canvas canvas) {
		
		mPaint.setFakeBoldText(false);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(MONTHDAY_HEADER_FONT_SIZE);	        
        mPaint.setStyle(Style.FILL);        
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setTypeface(Typeface.DEFAULT);	        
        
        for (int dayIndx = 0; dayIndx < 7; dayIndx++) {
            int dayOfWeek = dayIndx + mFirstVisibleDayOfWeek;
            if (dayOfWeek >= 14) {
                dayOfWeek -= 14;
            }

            int color = mCalendarViewsSecondaryActionBar.mMonthDayTextColor;
            
            final int column = dayIndx % 7;
            if (Utils.isSaturday(column, mFirstDayOfWeek)) {
                color = mCalendarViewsSecondaryActionBar.mWeek_saturdayColor;
            } else if (Utils.isSunday(column, mFirstDayOfWeek)) {
                color = mCalendarViewsSecondaryActionBar.mWeek_sundayColor;
            }
            
            if (mApplyGoToDateInsideWeekAnimation)
            	drawMonthDayGoToDateInsideWeekCase(dayIndx, canvas, color);     
            else if (mApplyGoToDateOutsideWeekAnimation) 
            	drawMonthDayGoToDateOutSideWeekCase(dayIndx, canvas, color);    
            else if (mApplyGoToDateFromAnotherWeekAnimation)
            	drawMonthDayGoToDateFromAnotherWeekCase(dayIndx, canvas, color);    
            else 
            	drawMonthDay(dayIndx, canvas, color);                     	
        }            
    }		
			
	// ������ ������ ������...
	private void drawMonthDayGoToDateInsideWeekCase(int dayIndex, Canvas canvas, int textColor) {
		
		int x = computeDayLeftPosition(dayIndex);					
		float y = mCalendarViewsSecondaryActionBar.mDateTextTopPaddingOfMonthDayHeader;
		
        int dateNum = mFirstVisibleDate + dayIndex;
        
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }
        
        String dateNumStr = String.valueOf(dateNum);
        mPaint.setTypeface(Typeface.DEFAULT);
    	mPaint.setColor(textColor);
        canvas.drawText(dateNumStr, x, y, mPaint);
        
        // mFirstJulianDay�� �ش� ���� ù��° ��¥�� �ǹ��Ѵ�
        long dateNumJulinaDay = mFirstJulianDay + dayIndex;  
        //long gmtoff = mTimeZone.getRawOffset() / 1000;
        long selectedTimeJulianDay = ETime.getJulianDay(mSelectedDayOfMonthDayHeader.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        
        int left = x - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;	    			
		int top = mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableCenterY - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;
		int right = left + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
		int bottom = top + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
		
		// goToDate���� true�� ������
		// animation�� ����Ǿ�� �ϴ� �����̴�			
		long newSelectedTimeJulianDay = ETime.getJulianDay(mNewSelectedTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
		if (newSelectedTimeJulianDay == dateNumJulinaDay) {    				
    		float percent = (float)mGoToDateAnimationAccumulateTime / (float)mGoToDateAnimationDuration;		    		
    		int alpha = (int) (percent * 255);
    		
			if (newSelectedTimeJulianDay == mCalendarViewsSecondaryActionBar.mTodayJulianDay){
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.getPaint().setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setBounds(left, top, right, bottom);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setDrawTextPosition(x, y);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setDayOfMonth(dateNum);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.draw(canvas);
			}
			else {
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.getPaint().setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setBounds(left, top, right, bottom);	
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setDrawTextPosition(x, y);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setDayOfMonth(dateNum);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.draw(canvas);	  
			}
		}
		else if (selectedTimeJulianDay == dateNumJulinaDay) {
			float percent = (float)mGoToDateAnimationAccumulateTime / (float)mGoToDateAnimationDuration;	    	    		
			int alpha = 255 - (int) (percent * 255);
			
			if (selectedTimeJulianDay == mCalendarViewsSecondaryActionBar.mTodayJulianDay) {	        		
				mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.getPaint().setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setAlpha(alpha);
        		if (alpha < 102)  { // 255 * 0.4
        			int textAlpha = 255 - alpha;		        			
        			mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setTextColorAlpha(textAlpha, textColor);
        		}
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setDrawTextPosition(x, y);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setDayOfMonth(dateNum);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.draw(canvas);
        	}
        	else { 	        		
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.getPaint().setAlpha(alpha);
        		//if (INFO) Log.i(TAG, "alpha=" + String.valueOf(alpha));
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setAlpha(alpha);
        		if (alpha < 102)  { // 255 * 0.4
        			int textAlpha = 255 - alpha;
        			//if (INFO) Log.i(TAG, "textAlpha=" + String.valueOf(textAlpha));
        			mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setTextColorAlpha(textAlpha, textColor);
        		}
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setBounds(left, top, right, bottom);	
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setDrawTextPosition(x, y);
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setDayOfMonth(dateNum);
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.draw(canvas);	    			
        	}	
		}        
    }
	
	@SuppressLint("SuspiciousIndentation")
	private void drawMonthDayGoToDateOutSideWeekCase(int dayIndex, Canvas canvas, int textColor) {
		
		int x = computeDayLeftPosition(dayIndex);					
		float y = mCalendarViewsSecondaryActionBar.mDateTextTopPaddingOfMonthDayHeader;
		
        int dateNum = mFirstVisibleDate + dayIndex;
        
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }
        
        String dateNumStr = String.valueOf(dateNum);
        mPaint.setTypeface(Typeface.DEFAULT);
    	mPaint.setColor(textColor);
        canvas.drawText(dateNumStr, x, y, mPaint);
        
        // mFirstJulianDay�� �ش� ���� ù��° ��¥�� �ǹ��Ѵ�
        long dateNumJulinaDay = mFirstJulianDay + dayIndex;        
        //long gmtoff = mTimeZone.getRawOffset() / 1000;
        long selectedTimeJulianDay = ETime.getJulianDay(mSelectedDayOfMonthDayHeader.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        //long selectedTimeJulianDay = Time.getJulianDay(mSelectedDayOfMonthDayHeader.toMillis(false), mSelectedDayOfMonthDayHeader.gmtoff);	        
		
		if (selectedTimeJulianDay == dateNumJulinaDay) {
			int left = x - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;	    			
			int top = mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableCenterY - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;
			int right = left + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
			int bottom = top + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
			
			// goToDate���� true�� ������
			// animation�� ����Ǿ�� �ϴ� �����̴�			
        	float percent = (float)mGoToDateAnimationAccumulateTime / (float)mGoToDateAnimationDuration;	    	    		
			int alpha = 255 - (int) (percent * 255);
			
			if (selectedTimeJulianDay == mCalendarViewsSecondaryActionBar.mTodayJulianDay) {	        		
				mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.getPaint().setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setAlpha(alpha);
        		if (alpha < 102)  { // 255 * 0.4
        			int textAlpha = 255 - alpha;		        			
        			mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setTextColorAlpha(textAlpha, textColor);
        		}
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setDrawTextPosition(x, y);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setDayOfMonth(dateNum);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.draw(canvas);
        	}
        	else { 	        		
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.getPaint().setAlpha(alpha);	        		
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setAlpha(alpha);
        		if (alpha < 102)  { // 255 * 0.4
        			int textAlpha = 255 - alpha;	    
        			
//        			if (textAlpha > 250)
        				//if (INFO) Log.i(TAG, "textAlpha=" + String.valueOf(textAlpha));
        			
        			mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setTextColorAlpha(textAlpha, textColor);
        		}
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setBounds(left, top, right, bottom);	
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setDrawTextPosition(x, y);
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setDayOfMonth(dateNum);
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.draw(canvas);	    			
        	}	
		}
    }
	
	private void drawMonthDayGoToDateFromAnotherWeekCase(int dayIndex, Canvas canvas, int textColor) {
		
		int x = computeDayLeftPosition(dayIndex);					
		float y = mCalendarViewsSecondaryActionBar.mDateTextTopPaddingOfMonthDayHeader;
		
        int dateNum = mFirstVisibleDate + dayIndex;
        
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }
        
        String dateNumStr = String.valueOf(dateNum);
        mPaint.setTypeface(Typeface.DEFAULT);
    	mPaint.setColor(textColor);
        canvas.drawText(dateNumStr, x, y, mPaint);
        
        // mFirstJulianDay�� �ش� ���� ù��° ��¥�� �ǹ��Ѵ�
        long dateNumJulinaDay = mFirstJulianDay + dayIndex;            
        //long selectedTimeJulianDay = Time.getJulianDay(mSelectedDay.toMillis(false), mSelectedDay.gmtoff);        
        long gmtoff = mTimeZone.getRawOffset() / 1000;
    	long newSelectedTimeJulianDay = ETime.getJulianDay(mNewSelectedTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		
		if (newSelectedTimeJulianDay == dateNumJulinaDay) {
			int left = x - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;	    			
			int top = mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableCenterY - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;
			int right = left + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
			int bottom = top + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
			
			float percent = (float)mGoToDateAnimationAccumulateTime / (float)mGoToDateAnimationDuration;		    		
    		int alpha = (int) (percent * 255);    		
    		
			if (newSelectedTimeJulianDay == mCalendarViewsSecondaryActionBar.mTodayJulianDay){
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.getPaint().setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setBounds(left, top, right, bottom);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setDrawTextPosition(x, y);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.setDayOfMonth(dateNum);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateTodayDrawable.draw(canvas);
			}
			else {
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.getPaint().setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setAlpha(alpha);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setBounds(left, top, right, bottom);	
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setDrawTextPosition(x, y);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.setDayOfMonth(dateNum);
				mCalendarViewsSecondaryActionBar.mNewSelectedDateDrawable.draw(canvas);	  
			}   
		}
        
    }
	
	private void drawMonthDay(int dayIndex, Canvas canvas, int textColor) {
		// mFirstJulianDay�� �ش� ���� ù��° ��¥�� �ǹ��Ѵ�
		long dateNumJulinaDay = mFirstJulianDay + dayIndex;
        long selectedTimeJulianDay = ETime.getJulianDay(mSelectedDayOfMonthDayHeader.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
        
		int x = computeDayLeftPosition(dayIndex);					
		float y = mCalendarViewsSecondaryActionBar.mDateTextTopPaddingOfMonthDayHeader;
		
        int dateNum = mFirstVisibleDate + dayIndex;
        
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }
        
        String dateNumStr = String.valueOf(dateNum);
        mPaint.setTypeface(Typeface.DEFAULT);
        
        if (mCalendarViewsSecondaryActionBar.mTodayJulianDay == dateNumJulinaDay && mCalendarViewsSecondaryActionBar.mTodayJulianDay != selectedTimeJulianDay)
        	mPaint.setColor(Color.RED);
        else 
        	mPaint.setColor(textColor);
    	
        canvas.drawText(dateNumStr, x, y, mPaint);	
        
    	if (selectedTimeJulianDay == dateNumJulinaDay) {
    		
    		int left = x - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;	    			
			int top = mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableCenterY - mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableRadius;
			int right = left + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;
			int bottom = top + mCalendarViewsSecondaryActionBar.mSelectedCircleDrawableSize;		
    		
        	mPaint.setTypeface(mBold);		        	      			
			
        	if (selectedTimeJulianDay == mCalendarViewsSecondaryActionBar.mTodayJulianDay) {		        		
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setBounds(left, top, right, bottom);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setDrawTextPosition(x, y);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setDayOfMonth(dateNum);
        		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.draw(canvas);
        	}
        	else {		        		
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setBounds(left, top, right, bottom);	
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setDrawTextPosition(x, y);
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setDayOfMonth(dateNum);
        		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.draw(canvas);	    			
        	}	        	
        }	        
    }			
	
	
	
	Calendar mNewSelectedTime;
	long mGoToDateAnimationStartTime;
	long mGoToDateAnimationAccumulateTime;	
	long mGoToDateAnimationDuration;
	
	// updateSelectedDay���� ȣ����
	// ���� ��������� ��...
	public Runnable goToDateInsideWeekAnimation(Calendar goToDateTime, long animationDuration) {			
		mGoToDateAnimationStartTime = System.currentTimeMillis();
		mGoToDateAnimationAccumulateTime = 0;
		
		float adjustDuration = (float)animationDuration * 0.7f;
		mGoToDateAnimationDuration = (long)adjustDuration;
		
		mNewSelectedTime = GregorianCalendar.getInstance(goToDateTime.getTimeZone());
		mNewSelectedTime.setFirstDayOfWeek(goToDateTime.getFirstDayOfWeek());
		mNewSelectedTime.setTimeInMillis(goToDateTime.getTimeInMillis());
		mNewSelectedTime.set(Calendar.HOUR_OF_DAY, 0);
		mNewSelectedTime.set(Calendar.MINUTE, 0);
		mNewSelectedTime.set(Calendar.SECOND, 0);
		mNewSelectedTime.set(Calendar.MILLISECOND, 0);	
					
		mApplyGoToDateInsideWeekAnimation = true;
		
		//post(mGoToDateInsideWeekAnimation);
		
		return mGoToDateInsideWeekAnimation;					
	}
	
	// ó�� posting�� goToDateAnimation���� �̷��������,
	// ���� onDraw���� posting�Ѵ� 
	private Runnable mGoToDateInsideWeekAnimation = new Runnable() {
	    @Override
	    public void run() {
	    	
    		long currentTime = System.currentTimeMillis();
    		mGoToDateAnimationAccumulateTime = currentTime - mGoToDateAnimationStartTime;		    	
    		
	    	if (mGoToDateAnimationAccumulateTime < mGoToDateAnimationDuration) {		    		    		
	    		CircularMonthDayHeaderView.this.invalidate();		    		
	    	}		    	
	    	else {	
	    		//if (INFO) Log.i(TAG, "CHANGE mApplyGoToDateInsideWeekAnimation To FALSE");
	    		
	    		mApplyGoToDateInsideWeekAnimation = false;
	    		CircularMonthDayHeaderView.this.setSelectedTime(mNewSelectedTime.getTimeInMillis());
	    		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.getPaint().setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.reSetTextColor();
	    		
	    		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.getPaint().setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.reSetTextColor();
	    				    		
	    		CircularMonthDayHeaderView.this.invalidate();
	    	}
	    }
	};		
	
	
	public Runnable goToDateInOutsideWeekAnimation(Calendar goToDateTime, long animationDuration) {			
		mGoToDateAnimationStartTime = System.currentTimeMillis();
		mGoToDateAnimationAccumulateTime = 0;
		
		float adjustDuration = (float)animationDuration * 0.7f;
		mGoToDateAnimationDuration = (long)adjustDuration;
					
		mNewSelectedTime = GregorianCalendar.getInstance(goToDateTime.getTimeZone());
		mNewSelectedTime.setFirstDayOfWeek(goToDateTime.getFirstDayOfWeek());
		mNewSelectedTime.setTimeInMillis(goToDateTime.getTimeInMillis());
		mNewSelectedTime.set(Calendar.HOUR_OF_DAY, 0);
		mNewSelectedTime.set(Calendar.MINUTE, 0);
		mNewSelectedTime.set(Calendar.SECOND, 0);
		mNewSelectedTime.set(Calendar.MILLISECOND, 0);	
		
		mApplyGoToDateOutsideWeekAnimation = true;			
			
		return mGoToDateOutsideWeekAnimation;
	}
	
	// ó�� posting�� goToDateAnimation���� �̷��������,
	// ���� onDraw���� posting�Ѵ� 
	private Runnable mGoToDateOutsideWeekAnimation = new Runnable() {
	    @Override
	    public void run() {
	    	
    		long currentTime = System.currentTimeMillis();
    		mGoToDateAnimationAccumulateTime = currentTime - mGoToDateAnimationStartTime;		    	
    		
	    	if (mGoToDateAnimationAccumulateTime < mGoToDateAnimationDuration) {		    		    		
	    		CircularMonthDayHeaderView.this.invalidate();		    		
	    	}		    	
	    	else {	
	    		//if (INFO) Log.i(TAG, "CHANGE mApplyGoToDateOutsideWeekAnimation To FALSE");		    		
	    		mApplyGoToDateOutsideWeekAnimation = false;
	    		// GoToDateOutsideWeek ��Ȳ����
	    		// VISIBLE WEEK�� mNewSelectedTime�� �����ϴ� ���� ���ʿ��� ���� �ƴϴ�
	    		// Selected Drawable Circle�� drawing ���� �ʱ� ���ؼ���!!!
	    		CircularMonthDayHeaderView.this.setSelectedTime(mNewSelectedTime.getTimeInMillis());		    				    				    		
	    		//CircularMonthDayHeaderView.this.invalidate();
	    	}
	    }
	};		
	
	
	public Runnable goToDateFromAnotherWeekAnimation(Calendar goToDateTime, long animationDuration) {			
		mGoToDateAnimationStartTime = System.currentTimeMillis();
		mGoToDateAnimationAccumulateTime = 0;
		
		float adjustDuration = (float)animationDuration * 0.7f;
		mGoToDateAnimationDuration = (long)adjustDuration;
					
		mNewSelectedTime = GregorianCalendar.getInstance(goToDateTime.getTimeZone());
		mNewSelectedTime.setFirstDayOfWeek(goToDateTime.getFirstDayOfWeek());
		mNewSelectedTime.setTimeInMillis(goToDateTime.getTimeInMillis());
		mNewSelectedTime.set(Calendar.HOUR_OF_DAY, 0);
		mNewSelectedTime.set(Calendar.MINUTE, 0);
		mNewSelectedTime.set(Calendar.SECOND, 0);
		mNewSelectedTime.set(Calendar.MILLISECOND, 0);	
		
		mApplyGoToDateFromAnotherWeekAnimation = true;
		
		return mGoToDateFromAnotherWeekAnimation;
		//post(mGoToDateFromAnotherWeekAnimation);			
	}
	
	// ó�� posting�� goToDateAnimation���� �̷��������,
	// ���� onDraw���� posting�Ѵ� 
	private Runnable mGoToDateFromAnotherWeekAnimation = new Runnable() {
	    @Override
	    public void run() {
	    	
    		long currentTime = System.currentTimeMillis();
    		mGoToDateAnimationAccumulateTime = currentTime - mGoToDateAnimationStartTime;		    	
    		
	    	if (mGoToDateAnimationAccumulateTime < mGoToDateAnimationDuration) {		    		    		
	    		CircularMonthDayHeaderView.this.invalidate();		    		
	    	}		    	
	    	else {	
	    		//if (INFO) Log.i(TAG, "CHANGE mApplyGoToDateFromAnotherWeekAnimation To FALSE");
	    		
	    		mApplyGoToDateFromAnotherWeekAnimation = false;
	    		CircularMonthDayHeaderView.this.setSelectedTime(mNewSelectedTime.getTimeInMillis());
	    		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.getPaint().setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateTodayDrawable.reSetTextColor();/////////////////////////////////////
	    		
	    		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.getPaint().setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.setAlpha(255);
	    		mCalendarViewsSecondaryActionBar.mSelectedDateDrawable.reSetTextColor();//////////////////////////////////////////
	    				    		
	    		CircularMonthDayHeaderView.this.invalidate();
	    	}
	    }
	};		
	
	
	private int computeDayLeftPosition(int day) {				
        return mLeftSideMargin + (day * mMonthDayTextWidth);
		
    }

	float[] mMonthDaysXRegion = new float[7];
	private void computeMonthDaysXRegion() {
		
		for (int i=0; i<7; i++) {
			mMonthDaysXRegion[i] = i * mMonthDayTextWidth; 
		}
	}
	
	public int selectedMonthDaysIndex(float x) {
		int index = -1;
		if ( (mMonthDaysXRegion[0] < x) && (x <= mMonthDaysXRegion[1]) ) {
			index = 0;
		}
		else if  ( (mMonthDaysXRegion[1] < x) && (x <= mMonthDaysXRegion[2]) ) {
			index = 1;
		}
		else if  ( (mMonthDaysXRegion[2] < x) && (x <= mMonthDaysXRegion[3]) ) {
			index = 2;
		}
		else if  ( (mMonthDaysXRegion[3] < x) && (x <= mMonthDaysXRegion[4]) ) {
			index = 3;
		}
		else if  ( (mMonthDaysXRegion[4] < x) && (x <= mMonthDaysXRegion[5]) ) {
			index = 4;
		}
		else if  ( (mMonthDaysXRegion[5] < x) && (x <= mMonthDaysXRegion[6]) ) {
			index = 5;
		}
		else {
			index = 6;
		}
		return index;
	}		
}  	
