package com.intheeast.event;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;


import android.widget.LinearLayout.LayoutParams;

import com.intheeast.etc.LockableScrollView;
import com.intheeast.impl.TimerPickerTimeText;
import com.intheeast.timepicker.BriefTimePickerRenderParameters;
import com.intheeast.timepicker.TimePickerRender;
import com.intheeast.timepicker.TimePickerRenderParameters;

public class TimePickerMetaData {

	public static final float TEXTUREVIEW_HEIGHT_DP = 200;
	public static final float TEXTUREVIEW_RATIO = 1.5f;
	
	static final float TIMEPICKER_ITEM_TEXT_SIZE_RATIO = 0.8f;
	static final float ITEM_TEXT_Y_COORDINATE_RATIO = 0.8f;
	
	Activity mActivity;
	
	float mPpi;
	float mDensity;
	
	
	int mTextureViewHeight;
	int mTextureViewWidth;
	float mTextureViewRatio;
	
	public LayoutParams mTextureViewParams;
	
	float mTimePickerHeightPixels;
	public float mTimePickerDateFiedlCircleRadiusPixels;
	float mTimePickerDateFieldCircleDiameterPixels;
	float mItemHeightOfTimePickerField;
	float mTimePickerTextSize;
	
	public TimePickerMetaData(Activity activity, LockableScrollView mMainPageScrollView) {
		mActivity = activity;
		init(mMainPageScrollView);
	}
	
	public void init(LockableScrollView mMainPageScrollView) {
		WindowManager wm = (WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE);
		Display current_display = wm.getDefaultDisplay();
		DisplayMetrics display_Metrics = new DisplayMetrics();
		current_display.getMetrics(display_Metrics);		
		mPpi = display_Metrics.densityDpi;
		mDensity = display_Metrics.density;
		//mScaleDensity = display_Metrics.scaledDensity;
		//m_oneCentiMeter = display_Metrics.ydpi * 0.393701f;
		
		mTextureViewHeight = (int)(TEXTUREVIEW_HEIGHT_DP * mDensity); 
		mTextureViewWidth = display_Metrics.widthPixels;
        mTextureViewRatio = (float)(mTextureViewWidth / mTextureViewHeight);
        mTextureViewParams = new LayoutParams(mTextureViewWidth, mTextureViewHeight);
        
        createTimePickerDimens(mTextureViewHeight);
        createTimePickerFieldBitmaps(mTextureViewWidth);        	
    	createTimePickerFieldRect(mTextureViewWidth, mTextureViewHeight);	
    	createMiniTimePickerFieldRect();
    	createTimePickerRenderParameters(mMainPageScrollView);  
    	createAllDayTimePickerRenderParameters(mMainPageScrollView);
	}
	
	
	
	public void createTimePickerDimens(int height) {
		mTimePickerDateFiedlCircleRadiusPixels = (float)(height * TimePickerRender.TIMEPICKER_FIELD_CIRCLE_RADIUS);
		mTimePickerDateFieldCircleDiameterPixels = (float)( 2 * Math.PI * mTimePickerDateFiedlCircleRadiusPixels );
		mTimePickerHeightPixels = (float)(mTimePickerDateFieldCircleDiameterPixels / 2);
		mItemHeightOfTimePickerField = mTimePickerHeightPixels / TimePickerRender.TIMEPICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT;
		mTimePickerTextSize = mItemHeightOfTimePickerField * TIMEPICKER_ITEM_TEXT_SIZE_RATIO;		
	}
	
	
	public void createTimePickerFieldBitmaps(int width) {
		createNumeralTextBitmap();
		createDayOfWeekTextBitmap(); // day of week의 텍스트 중 가장 큰 텍스트의 width를 bitmap width로 정한다 : 이제 의미가 없다	
		createTodayTextBitmap();
		createAMPMTextBitmap();
		createYearMonthDateTextBitmap();
		
		createCenterNumeralTextBitmap();
		createCenterDayOfWeekTextBitmap(); // day of week의 텍스트 중 가장 큰 텍스트의 width를 bitmap width로 정한다 : 이제 의미가 없다	
		createCenterTodayTextBitmap();
		createCenterAMPMTextBitmap();
		createCenterYearMonthDateTextBitmap();
		
		createEraseBackgroundBitmap(width);
	}
	
	float mTimePickerWidthPixels;
	float mLeftMeridiemFieldOfTimePicker;
	float mHorizontalGAPBetweenFieldsOfTimePicker;
	float mRightMeridiemFieldOfTimePicker;
	float mRightDateFieldOfTimePicker;
	float mLeftHourFieldOfTimePicker;
	float mRightHourFieldOfTimePicker;
	float mLeftMinuteFieldOfTimePicker;
	public void createTimePickerFieldRect(int width, int height) {
		mTimePickerWidthPixels = width;		
		// mHorizontalGAPBetweenFields = mTimePickerTextWidthRatio;		
		mLeftMeridiemFieldOfTimePicker = (mTimePickerWidthPixels / 2) - mHorizontalGAPBetweenFieldsOfTimePicker;	
		//mRightMeridiemFieldOfTimePicker = mLeftMeridiemFieldOfTimePicker + (mTimePickerTextWdith * 2) + (hGAPWidth * 2);	
		mRightMeridiemFieldOfTimePicker = (mTimePickerWidthPixels / 2) + (mTimePickerTextWdith * 2) + (mHorizontalGAPBetweenFieldsOfTimePicker);	
		
		mRightDateFieldOfTimePicker = mLeftMeridiemFieldOfTimePicker;	
		
		// m_hourPickerLeftMostVPRatio = m_meridiemPickerLeftMostVPRatio + (mTimePickerTextWidthRatio * 2) + (H_GAP * 2);
		mLeftHourFieldOfTimePicker = mRightMeridiemFieldOfTimePicker;		
		//mRightHourFieldOfTimePicker = mLeftHourFieldOfTimePicker + (mTimePickerNumeralWidth * 2) + (hGAPWidth);
		mRightHourFieldOfTimePicker = mLeftHourFieldOfTimePicker + (mTimePickerNumeralWidth * 2) + (mHorizontalGAPBetweenFieldsOfTimePicker * 2);
		
		// m_minutePickerLeftMostVPRatio = m_hourPickerLeftMostVPRatio + (mNumeralTextWidthRatio * 2) + (H_GAP);
		mLeftMinuteFieldOfTimePicker = mRightHourFieldOfTimePicker;		
	}
	
	float mLeftMonthFieldOfTimePicker;
	float mRightMonthFieldOfTimePicker;
	float mRightYearFieldOfTimePicker;
	float mLeftDayOfWeekFieldOfTimePicker;
	public void createMiniTimePickerFieldRect() {				
		// m_monthFieldPickerLeftMostVPRatio = 0.0f - mHorizontalGAPBetweenFields;
		mLeftMonthFieldOfTimePicker = ((mTimePickerWidthPixels / 2) - mHorizontalGAPBetweenFieldsOfTimePicker) - mHorizontalGAPBetweenFieldsOfTimePicker;
		// m_dayOfMonthFieldPickerLeftMostVPRatio = m_monthFieldPickerLeftMostVPRatio + (mTimePickerNumeralWidthRatio * 2) + mTimePickerTextWidthRatio + (mHorizontalGAPBetweenFields * 2);
		mRightMonthFieldOfTimePicker = mLeftMonthFieldOfTimePicker + 
				(mTimePickerNumeralWidth * 2) + mTimePickerTextWdith + (mHorizontalGAPBetweenFieldsOfTimePicker * 2);
		// m_yearFieldRightMostVPRattio = m_monthFieldPickerLeftMostVPRatio - mHorizontalGAPBetweenFields;
		mRightYearFieldOfTimePicker = mLeftMonthFieldOfTimePicker;
		
		mLeftDayOfWeekFieldOfTimePicker = mRightMonthFieldOfTimePicker;		
	}
	
	
	
	Paint mTimePickerNumeralPaint;
	ArrayList<Bitmap> mTimePickerNumeralTextBitmapsList;
	int TIME_PICKER_NONSELECTED_COLOR = Color.argb(0xFF, 0xC0, 0xC0, 0xC0);  //silver color
	float mTimePickerNumeralWidth;
	float mTimePickerNumeralFontsize;
	float mTimePickerNumeralXPos;
	float mTimePickerNumeralYPos;
	public void createNumeralTextBitmap() {
		mTimePickerNumeralTextBitmapsList = new ArrayList<Bitmap>();	
		
		mTimePickerNumeralPaint = new Paint();
		mTimePickerNumeralPaint.setAntiAlias(true);
		mTimePickerNumeralPaint.setColor(TIME_PICKER_NONSELECTED_COLOR);		
		mTimePickerNumeralPaint.setTextAlign(Align.CENTER);
		float specified_fontsize= mTimePickerNumeralPaint.getTextSize();
        float measured_fontsize= mTimePickerNumeralPaint.descent() - mTimePickerNumeralPaint.ascent();
        float font_factor= specified_fontsize / measured_fontsize;
        float textSize = mItemHeightOfTimePickerField * TIMEPICKER_ITEM_TEXT_SIZE_RATIO * font_factor;
        mTimePickerNumeralPaint.setTextSize(textSize);
		
		float maxSize = 0;
		for (int i=0; i<10; i++) {
			String calText = String.valueOf(i);			
			float tempMax = mTimePickerNumeralPaint.measureText(calText);
			if (tempMax > maxSize) {
				maxSize = tempMax;
			}
		}
				
		mTimePickerNumeralWidth = maxSize;
		mTimePickerNumeralFontsize = mTimePickerNumeralPaint.descent() - mTimePickerNumeralPaint.ascent();
		mTimePickerNumeralXPos = (float)( mTimePickerNumeralWidth/2 );
		mTimePickerNumeralYPos = ( mItemHeightOfTimePickerField/2 + mTimePickerNumeralFontsize/2 - mTimePickerNumeralPaint.descent() );	
		
		for (int i=0; i<10; i++) {			
			Bitmap NumeralBitmap = Bitmap.createBitmap((int)mTimePickerNumeralWidth,
													  (int)mItemHeightOfTimePickerField,
													  Config.ARGB_8888);			
			
			NumeralBitmap.eraseColor(Color.WHITE);		
			Canvas canvas = new Canvas(NumeralBitmap);
						
			canvas.drawText(String.valueOf(i), mTimePickerNumeralXPos, mTimePickerNumeralYPos, mTimePickerNumeralPaint);			
			mTimePickerNumeralTextBitmapsList.add(i, NumeralBitmap);
		}
	}
	
	ArrayList<Bitmap> mTimePickerDayOfWeekTextBitmapsList;
	Paint mTimePickerTextPaint;
	float mTimePickerTextWdith;
	
	float mTimePickerTextFontsize;
	float mTimePickerTextXPos;
	float mTimePickerTextYPos;
	public void createDayOfWeekTextBitmap() {
		mTimePickerDayOfWeekTextBitmapsList = new ArrayList<Bitmap>();
		
		mTimePickerTextPaint = new Paint();
		mTimePickerTextPaint.setAntiAlias(true);
		mTimePickerTextPaint.setColor(TIME_PICKER_NONSELECTED_COLOR);		
		mTimePickerTextPaint.setTextAlign(Align.CENTER);
		float specified_fontsize= mTimePickerTextPaint.getTextSize();
        float measured_fontsize= mTimePickerTextPaint.descent() - mTimePickerTextPaint.ascent();
        float font_factor= specified_fontsize / measured_fontsize;
        float textSize = mItemHeightOfTimePickerField * TIMEPICKER_ITEM_TEXT_SIZE_RATIO * font_factor;
        mTimePickerTextPaint.setTextSize(textSize);
		
		float maxSize = 0;
		for (int i=1; i<8; i++) {
			String calText = TimerPickerTimeText.transformDayOfWeekToString(i);
			float tempMax = mTimePickerTextPaint.measureText(calText);
			if (tempMax > maxSize) {
				maxSize = tempMax;
			}
		}
				
		mTimePickerTextWdith = maxSize;
		// mHorizontalGAPBetweenFields = mTimePickerTextWidthRatio; // from TimePickerRender constructor!!!
		mHorizontalGAPBetweenFieldsOfTimePicker = mTimePickerTextWdith;
		mTimePickerTextFontsize = mTimePickerTextPaint.descent() - mTimePickerTextPaint.ascent();
		mTimePickerTextXPos = (float)( mTimePickerTextWdith / 2 );
		mTimePickerTextYPos = ( mItemHeightOfTimePickerField/2 + mTimePickerTextFontsize/2 - mTimePickerTextPaint.descent() );		
			
		for (int i=0; i<7; i++) {
			Bitmap DayOfWeekBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
					                                     (int)mItemHeightOfTimePickerField,
					                                      Config.ARGB_8888);		

			DayOfWeekBitmap.eraseColor(Color.WHITE);		
			Canvas canvas = new Canvas(DayOfWeekBitmap);
			int dayOfWeekNumber = i + 1;
			String dayOfWeek = TimerPickerTimeText.transformDayOfWeekToString(dayOfWeekNumber);
			canvas.drawText(dayOfWeek, mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);
			mTimePickerDayOfWeekTextBitmapsList.add(i, DayOfWeekBitmap);
		}		
	}
	
	ArrayList<Bitmap> mTimePickerTodayTextBitmapsList;
	public void createTodayTextBitmap() {
		mTimePickerTodayTextBitmapsList = new ArrayList<Bitmap>();
				
		Bitmap firstTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
													 (int)mItemHeightOfTimePickerField,
													 Config.ARGB_8888);		
		String firstStr = "오";
		firstTextBitmap.eraseColor(Color.WHITE);
		Canvas firstStrCanvas = new Canvas(firstTextBitmap);					
		firstStrCanvas.drawText(firstStr, mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);
		mTimePickerTodayTextBitmapsList.add(0, firstTextBitmap);		
		
		Bitmap secondTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
                                                      (int)mItemHeightOfTimePickerField,
                                                      Config.ARGB_8888);
		String pmStr = "늘";
		secondTextBitmap.eraseColor(Color.WHITE);
		Canvas pmStrCanvas = new Canvas(secondTextBitmap);						
		pmStrCanvas.drawText(pmStr, mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);		
		mTimePickerTodayTextBitmapsList.add(1, secondTextBitmap);		
	}
	
	ArrayList<Bitmap> mTimePickerMeridiemTextBitmapsList;
	public void createAMPMTextBitmap() {
		mTimePickerMeridiemTextBitmapsList = new ArrayList<Bitmap>();
				
		Bitmap firstTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
													  (int)mItemHeightOfTimePickerField,
													  Config.ARGB_8888);		
		String firstStr = "오";
		firstTextBitmap.eraseColor(Color.WHITE);
		Canvas firstStrCanvas = new Canvas(firstTextBitmap);					
		firstStrCanvas.drawText(firstStr, mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);
		mTimePickerMeridiemTextBitmapsList.add(0, firstTextBitmap);		
		
		Bitmap pmTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
                                                  (int)mItemHeightOfTimePickerField,
                                                  Config.ARGB_8888);
		String pmStr = "후";
		pmTextBitmap.eraseColor(Color.WHITE);
		Canvas pmStrCanvas = new Canvas(pmTextBitmap);						
		pmStrCanvas.drawText(pmStr, mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);		
		mTimePickerMeridiemTextBitmapsList.add(1, pmTextBitmap);	
		
		Bitmap amTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
                (int)mItemHeightOfTimePickerField,
                Config.ARGB_8888);
		String amStr = "전";
		amTextBitmap.eraseColor(Color.WHITE);		
		Canvas amStrCanvas = new Canvas(amTextBitmap);						
		amStrCanvas.drawText(amStr, mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);		
		mTimePickerMeridiemTextBitmapsList.add(2, amTextBitmap);
	}
	
	Bitmap mTimePickerYearTextBitmap;
	Bitmap mTimePickerMonthTextBitmap;
	Bitmap mTimePickerDateTextBitmap;
	public void createYearMonthDateTextBitmap() {					
		mTimePickerYearTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
					     							    (int)mItemHeightOfTimePickerField,
					     							    Config.ARGB_8888);			
		
		mTimePickerYearTextBitmap.eraseColor(Color.WHITE);		
		Canvas yearCanvas = new Canvas(mTimePickerYearTextBitmap);						
		yearCanvas.drawText("년", mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);

		mTimePickerMonthTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
													     (int)mItemHeightOfTimePickerField,
													      Config.ARGB_8888);			
			
		mTimePickerMonthTextBitmap.eraseColor(Color.WHITE);		
		Canvas monthCanvas = new Canvas(mTimePickerMonthTextBitmap);						
		monthCanvas.drawText("월", mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);
		
		mTimePickerDateTextBitmap = Bitmap.createBitmap((int)mTimePickerTextWdith,
				                                        (int)mItemHeightOfTimePickerField,
				                                        Config.ARGB_8888);			

		mTimePickerDateTextBitmap.eraseColor(Color.WHITE);		
		Canvas dateCanvas = new Canvas(mTimePickerDateTextBitmap);						
		dateCanvas.drawText("일", mTimePickerTextXPos, mTimePickerTextYPos, mTimePickerTextPaint);		
	}
	
	ArrayList<Bitmap> mTimePickerCenterNumeralBitmapsList;
	Paint mCenterTimePickerNumeralPaint;
	float mCenterTimePickerNumeralWidth;
	float mCenterTimePickerNumeralFontsize;
	float mCenterTimePickerNumeralXPos;
	float mCenterTimePickerNumeralYPos;
	public void createCenterNumeralTextBitmap() {
		mTimePickerCenterNumeralBitmapsList = new ArrayList<Bitmap>();	
		
		mCenterTimePickerNumeralPaint = new Paint();
		mCenterTimePickerNumeralPaint.setAntiAlias(true);
		mCenterTimePickerNumeralPaint.setColor(Color.BLACK);		
		mCenterTimePickerNumeralPaint.setTextAlign(Align.CENTER);
		float specified_fontsize= mCenterTimePickerNumeralPaint.getTextSize();
        float measured_fontsize= mCenterTimePickerNumeralPaint.descent() - mCenterTimePickerNumeralPaint.ascent();
        float font_factor= specified_fontsize / measured_fontsize;
        float textSize = mItemHeightOfTimePickerField * TIMEPICKER_ITEM_TEXT_SIZE_RATIO * font_factor;
        mCenterTimePickerNumeralPaint.setTextSize(textSize);
		
		float maxSize = 0;
		for (int i=0; i<10; i++) {
			String calText = String.valueOf(i);			
			float tempMax = mCenterTimePickerNumeralPaint.measureText(calText);
			if (tempMax > maxSize) {
				maxSize = tempMax;
			}
		}
				
		mCenterTimePickerNumeralWidth = maxSize;
		mCenterTimePickerNumeralFontsize = mCenterTimePickerNumeralPaint.descent() - mCenterTimePickerNumeralPaint.ascent();
		mCenterTimePickerNumeralXPos = (float)( mCenterTimePickerNumeralWidth / 2 );
		mCenterTimePickerNumeralYPos = ( mItemHeightOfTimePickerField/2 + mCenterTimePickerNumeralFontsize/2 - mCenterTimePickerNumeralPaint.descent() );	
		
		for (int i=0; i<10; i++) {			
			Bitmap NumeralBitmap = Bitmap.createBitmap((int)mCenterTimePickerNumeralWidth,
													  (int)mItemHeightOfTimePickerField,
													  Config.ARGB_8888);			
			
			NumeralBitmap.eraseColor(Color.WHITE);		
			Canvas canvas = new Canvas(NumeralBitmap);
						
			canvas.drawText(String.valueOf(i), mCenterTimePickerNumeralXPos, mCenterTimePickerNumeralYPos, mCenterTimePickerNumeralPaint);			
			mTimePickerCenterNumeralBitmapsList.add(i, NumeralBitmap);
		}
	}
	
	ArrayList<Bitmap> mTimePickerCenterDayOfWeekTextBitmapsList;
	Paint mCenterTimePickerTextPaint;
	float mCenterTimePickerTextWidth;
	float mCenterTimePickerTextFontsize;
	float mCenterTimePickerTextXPos;
	float mCenterTimePickerTextYPos;
	public void createCenterDayOfWeekTextBitmap() {
		mTimePickerCenterDayOfWeekTextBitmapsList = new ArrayList<Bitmap>();
		
		mCenterTimePickerTextPaint = new Paint();
		mCenterTimePickerTextPaint.setAntiAlias(true);
		mCenterTimePickerTextPaint.setColor(Color.BLACK);		
		mCenterTimePickerTextPaint.setTextAlign(Align.CENTER);
		float specified_fontsize= mCenterTimePickerTextPaint.getTextSize();
        float measured_fontsize= mCenterTimePickerTextPaint.descent() - mCenterTimePickerTextPaint.ascent();
        float font_factor= specified_fontsize / measured_fontsize;
        float textSize = mItemHeightOfTimePickerField * TIMEPICKER_ITEM_TEXT_SIZE_RATIO * font_factor;
        mCenterTimePickerTextPaint.setTextSize(textSize);
		
		float maxSize = 0;
		for (int i=1; i<8; i++) {
			String calText = TimerPickerTimeText.transformDayOfWeekToString(i);
			float tempMax = mCenterTimePickerTextPaint.measureText(calText);
			if (tempMax > maxSize) {
				maxSize = tempMax;
			}
		}
				
		mCenterTimePickerTextWidth = maxSize;
		mCenterTimePickerTextFontsize = mCenterTimePickerTextPaint.descent() - mCenterTimePickerTextPaint.ascent();
		mCenterTimePickerTextXPos = (float)( mCenterTimePickerTextWidth/2 );
		mCenterTimePickerTextYPos = ( mItemHeightOfTimePickerField/2 + mCenterTimePickerTextFontsize/2 - mCenterTimePickerTextPaint.descent() );		
			
		for (int i=0; i<7; i++) {
			Bitmap DayOfWeekBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
					                                   (int)mItemHeightOfTimePickerField,
					                                   Config.ARGB_8888);			

			DayOfWeekBitmap.eraseColor(Color.WHITE);		
			Canvas canvas = new Canvas(DayOfWeekBitmap);
			int dayOfWeekNumber = i + 1;
			String dayOfWeek = TimerPickerTimeText.transformDayOfWeekToString(dayOfWeekNumber);
			canvas.drawText(dayOfWeek, mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);
			mTimePickerCenterDayOfWeekTextBitmapsList.add(i, DayOfWeekBitmap);
		}		
	}
	
	ArrayList<Bitmap> mTimePickerCenterTodayTextBitmapsList;
	public void createCenterTodayTextBitmap() {
		mTimePickerCenterTodayTextBitmapsList = new ArrayList<Bitmap>();
				
		Bitmap firstTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
													 (int)mItemHeightOfTimePickerField,
													 Config.ARGB_8888);		
		String firstStr = "오";
		firstTextBitmap.eraseColor(Color.WHITE);
		Canvas firstStrCanvas = new Canvas(firstTextBitmap);					
		firstStrCanvas.drawText(firstStr, mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);		
		mTimePickerCenterTodayTextBitmapsList.add(0, firstTextBitmap);		
		
		Bitmap secondTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
                                                      (int)mItemHeightOfTimePickerField,
                                                      Config.ARGB_8888);
		String pmStr = "늘";
		secondTextBitmap.eraseColor(Color.WHITE);		
		Canvas pmStrCanvas = new Canvas(secondTextBitmap);						
		pmStrCanvas.drawText(pmStr, mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);		
		mTimePickerCenterTodayTextBitmapsList.add(1, secondTextBitmap);	
	}
	
	ArrayList<Bitmap> mTimePickerCenterMeridiemTextBitmapsList;
	public void createCenterAMPMTextBitmap() {
		mTimePickerCenterMeridiemTextBitmapsList = new ArrayList<Bitmap>();
				
		Bitmap firstTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
													  (int)mItemHeightOfTimePickerField,
													  Config.ARGB_8888);		
		String firstStr = "오";
		firstTextBitmap.eraseColor(Color.WHITE);
		Canvas firstStrCanvas = new Canvas(firstTextBitmap);					
		firstStrCanvas.drawText(firstStr, mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);		
		mTimePickerCenterMeridiemTextBitmapsList.add(0, firstTextBitmap);		
		
		Bitmap pmTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
                                                  (int)mItemHeightOfTimePickerField,
                                                  Config.ARGB_8888);
		String pmStr = "후";
		pmTextBitmap.eraseColor(Color.WHITE);		
		Canvas pmStrCanvas = new Canvas(pmTextBitmap);						
		pmStrCanvas.drawText(pmStr, mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);		
		mTimePickerCenterMeridiemTextBitmapsList.add(1, pmTextBitmap);	
		
		Bitmap amTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
                (int)mItemHeightOfTimePickerField,
                Config.ARGB_8888);
		String amStr = "전";
		amTextBitmap.eraseColor(Color.WHITE);		
		Canvas amStrCanvas = new Canvas(amTextBitmap);						
		amStrCanvas.drawText(amStr, mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);		
		mTimePickerCenterMeridiemTextBitmapsList.add(2, amTextBitmap);
	}
	
	Bitmap mTimePickerCenterYearTextBitmap;
	Bitmap mTimePickerCenterMonthTextBitmap;
	Bitmap mTimePickerCenterDateTextBitmap;
	public void createCenterYearMonthDateTextBitmap() {				
		mTimePickerCenterYearTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
			                                                  (int)mItemHeightOfTimePickerField,
			                                                  Config.ARGB_8888);			

		mTimePickerCenterYearTextBitmap.eraseColor(Color.WHITE);		
		Canvas yearCanvas = new Canvas(mTimePickerCenterYearTextBitmap);						
		yearCanvas.drawText("년", mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);

		mTimePickerCenterMonthTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
													     (int)mItemHeightOfTimePickerField,
													      Config.ARGB_8888);			
			
		mTimePickerCenterMonthTextBitmap.eraseColor(Color.WHITE);		
		Canvas monthCanvas = new Canvas(mTimePickerCenterMonthTextBitmap);						
		monthCanvas.drawText("월", mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);
		
		mTimePickerCenterDateTextBitmap = Bitmap.createBitmap((int)mCenterTimePickerTextWidth,
				                                        (int)mItemHeightOfTimePickerField,
				                                        Config.ARGB_8888);			

		mTimePickerCenterDateTextBitmap.eraseColor(Color.WHITE);		
		Canvas dateCanvas = new Canvas(mTimePickerCenterDateTextBitmap);						
		dateCanvas.drawText("일", mCenterTimePickerTextXPos, mCenterTimePickerTextYPos, mCenterTimePickerTextPaint);		
	}
	
	Bitmap mEraseBackgroundBitmap;	
	public void createEraseBackgroundBitmap(int width) {	
		
		mEraseBackgroundBitmap = Bitmap.createBitmap(width,
				                                     (int)mItemHeightOfTimePickerField,
				                                     Config.ARGB_8888);
		mEraseBackgroundBitmap.eraseColor(Color.WHITE);		
	}
	
	
	
	TimePickerRenderParameters mTimePickerRenderParameters;
	public void createTimePickerRenderParameters(LockableScrollView mMainPageScrollView) {
		mTimePickerRenderParameters = new TimePickerRenderParameters();    		
		mTimePickerRenderParameters.setTimePickerTextWdith(mTimePickerTextWdith);    		
		mTimePickerRenderParameters.setTimePickerNumeralWidth(mTimePickerNumeralWidth);
		mTimePickerRenderParameters.setTimePickerDateFieldCircleDiameterPixels(mTimePickerDateFieldCircleDiameterPixels);    		
		mTimePickerRenderParameters.setPpi(mPpi);
		mTimePickerRenderParameters.setTimePickerNumeralTextBitmapsList(mTimePickerNumeralTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerTodayTextBitmapsList(mTimePickerTodayTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerMeridiemTextBitmapsList(mTimePickerMeridiemTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerDayOfWeekTextBitmapsList(mTimePickerDayOfWeekTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerCenterDayOfWeekTextBitmapsList(mTimePickerCenterDayOfWeekTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerCenterTodayTextBitmapsList(mTimePickerCenterTodayTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerCenterMeridiemTextBitmapsList(mTimePickerCenterMeridiemTextBitmapsList);
		mTimePickerRenderParameters.setTimePickerCenterNumeralBitmapsList(mTimePickerCenterNumeralBitmapsList);		
		
		mTimePickerRenderParameters.setTimePickerMonthTextBitmap(mTimePickerMonthTextBitmap);
		mTimePickerRenderParameters.setTimePickerDateTextBitmap(mTimePickerDateTextBitmap);		
		
		mTimePickerRenderParameters.setTimePickerCenterMonthTextBitmap(mTimePickerCenterMonthTextBitmap);
		mTimePickerRenderParameters.setTimePickerCenterDateTextBitmap(mTimePickerCenterDateTextBitmap);
		
		mTimePickerRenderParameters.setEraseBackgroundBitmap(mEraseBackgroundBitmap);    
		
		mTimePickerRenderParameters.setRightDateFieldOfTimePicker(mRightDateFieldOfTimePicker);		
		mTimePickerRenderParameters.setLeftMeridiemFieldOfTimePicker(mLeftMeridiemFieldOfTimePicker);
		mTimePickerRenderParameters.setRightMeridiemFieldOfTimePicker(mRightMeridiemFieldOfTimePicker);		
		mTimePickerRenderParameters.setLeftHourFieldOfTimePicker(mLeftHourFieldOfTimePicker);		
		mTimePickerRenderParameters.setRightHourFieldOfTimePicker(mRightHourFieldOfTimePicker);
		
		mTimePickerRenderParameters.setMainPageScrollView(mMainPageScrollView);
	}	
	
	BriefTimePickerRenderParameters mAllDayTimePickerRenderParameters;
	public void createAllDayTimePickerRenderParameters(LockableScrollView mMainPageScrollView) {
		mAllDayTimePickerRenderParameters = new BriefTimePickerRenderParameters();    		
		mAllDayTimePickerRenderParameters.setTimePickerTextWdith(mTimePickerTextWdith);    		
		mAllDayTimePickerRenderParameters.setTimePickerNumeralWidth(mTimePickerNumeralWidth);
		mAllDayTimePickerRenderParameters.setTimePickerDateFieldCircleDiameterPixels(mTimePickerDateFieldCircleDiameterPixels);    		
		mAllDayTimePickerRenderParameters.setPpi(mPpi);
		mAllDayTimePickerRenderParameters.setTimePickerNumeralTextBitmapsList(mTimePickerNumeralTextBitmapsList);		
		mAllDayTimePickerRenderParameters.setTimePickerCenterNumeralBitmapsList(mTimePickerCenterNumeralBitmapsList);		
		
		mAllDayTimePickerRenderParameters.setTimePickerYearTextBitmap(mTimePickerYearTextBitmap);
		mAllDayTimePickerRenderParameters.setTimePickerMonthTextBitmap(mTimePickerMonthTextBitmap);
		mAllDayTimePickerRenderParameters.setTimePickerDateTextBitmap(mTimePickerDateTextBitmap);		
		
		mAllDayTimePickerRenderParameters.setTimePickerCenterYearTextBitmap(mTimePickerCenterYearTextBitmap);
		mAllDayTimePickerRenderParameters.setTimePickerCenterMonthTextBitmap(mTimePickerCenterMonthTextBitmap);
		mAllDayTimePickerRenderParameters.setTimePickerCenterDateTextBitmap(mTimePickerCenterDateTextBitmap);
		
		mAllDayTimePickerRenderParameters.setEraseBackgroundBitmap(mEraseBackgroundBitmap);    
		
		mAllDayTimePickerRenderParameters.setLeftMonthFieldOfTimePicker(mLeftMonthFieldOfTimePicker);		
		mAllDayTimePickerRenderParameters.setRightMonthFieldOfTimePicker(mRightMonthFieldOfTimePicker);
		mAllDayTimePickerRenderParameters.setRightYearFieldOfTimePicker(mRightYearFieldOfTimePicker);		
		mAllDayTimePickerRenderParameters.setLeftDayOfWeekFieldOfTimePicker(mLeftDayOfWeekFieldOfTimePicker);		
		
		mAllDayTimePickerRenderParameters.setMainPageScrollView(mMainPageScrollView);
	}	
	
	
	BriefTimePickerRenderParameters mRepeatEndTimePickerRenderParameters;
	public void createRepeatEndTimePickerRenderParameters(LockableScrollView mRepeatEndPageScrollView) {
		mRepeatEndTimePickerRenderParameters = new BriefTimePickerRenderParameters();    		
		mRepeatEndTimePickerRenderParameters.setTimePickerTextWdith(mTimePickerTextWdith);    		
		mRepeatEndTimePickerRenderParameters.setTimePickerNumeralWidth(mTimePickerNumeralWidth);
		mRepeatEndTimePickerRenderParameters.setTimePickerDateFieldCircleDiameterPixels(mTimePickerDateFieldCircleDiameterPixels);    		
		mRepeatEndTimePickerRenderParameters.setPpi(mPpi);
		mRepeatEndTimePickerRenderParameters.setTimePickerNumeralTextBitmapsList(mTimePickerNumeralTextBitmapsList);		
		mRepeatEndTimePickerRenderParameters.setTimePickerCenterNumeralBitmapsList(mTimePickerCenterNumeralBitmapsList);		
		
		mRepeatEndTimePickerRenderParameters.setTimePickerYearTextBitmap(mTimePickerYearTextBitmap);
		mRepeatEndTimePickerRenderParameters.setTimePickerMonthTextBitmap(mTimePickerMonthTextBitmap);
		mRepeatEndTimePickerRenderParameters.setTimePickerDateTextBitmap(mTimePickerDateTextBitmap);		
		
		mRepeatEndTimePickerRenderParameters.setTimePickerCenterYearTextBitmap(mTimePickerCenterYearTextBitmap);
		mRepeatEndTimePickerRenderParameters.setTimePickerCenterMonthTextBitmap(mTimePickerCenterMonthTextBitmap);
		mRepeatEndTimePickerRenderParameters.setTimePickerCenterDateTextBitmap(mTimePickerCenterDateTextBitmap);
		
		mRepeatEndTimePickerRenderParameters.setEraseBackgroundBitmap(mEraseBackgroundBitmap);    
		
		mRepeatEndTimePickerRenderParameters.setLeftMonthFieldOfTimePicker(mLeftMonthFieldOfTimePicker);		
		mRepeatEndTimePickerRenderParameters.setRightMonthFieldOfTimePicker(mRightMonthFieldOfTimePicker);
		mRepeatEndTimePickerRenderParameters.setRightYearFieldOfTimePicker(mRightYearFieldOfTimePicker);		
		mRepeatEndTimePickerRenderParameters.setLeftDayOfWeekFieldOfTimePicker(mLeftDayOfWeekFieldOfTimePicker);		
		
		mRepeatEndTimePickerRenderParameters.setMainPageScrollView(mRepeatEndPageScrollView);
	}	
	
	public void deleteTimePickerFieldBitmaps() {
		deleteNumeralTextBitmap();
		deleteDayOfWeekTextBitmap(); 	
		deleteTodayTextBitmap();
		deleteAMPMTextBitmap();
		deleteYearMonthDateTextBitmap();
		
		deleteCenterNumeralTextBitmap();
		deleteCenterDayOfWeekTextBitmap(); 
		deleteCenterTodayTextBitmap();
		deleteCenterAMPMTextBitmap();
		deleteCenterYearMonthDateTextBitmap();
		
		deleteEraseBackgroundBitmap();
	}
	
	public void deleteNumeralTextBitmap() {
		
		int numbers = mTimePickerNumeralTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerNumeralTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerNumeralTextBitmapsList.clear();
	}
	
	
	
	public void deleteDayOfWeekTextBitmap() {
		int numbers = mTimePickerDayOfWeekTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerDayOfWeekTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerDayOfWeekTextBitmapsList.clear();
	}
	
	
	
	public void deleteCenterNumeralTextBitmap() {
		int numbers = mTimePickerCenterNumeralBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerCenterNumeralBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerCenterNumeralBitmapsList.clear();
	}
	
	
	
	public void deleteTodayTextBitmap() {
		int numbers = mTimePickerTodayTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerTodayTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerTodayTextBitmapsList.clear();
	}
	
	
	
	public void deleteCenterTodayTextBitmap() {
		int numbers = mTimePickerCenterTodayTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerCenterTodayTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerCenterTodayTextBitmapsList.clear();
	}
	
	
	
	public void deleteCenterDayOfWeekTextBitmap() {
		int numbers = mTimePickerCenterDayOfWeekTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerCenterDayOfWeekTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerCenterDayOfWeekTextBitmapsList.clear();
	}
	
	
	
	public void deleteAMPMTextBitmap() {
		int numbers = mTimePickerMeridiemTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerMeridiemTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerMeridiemTextBitmapsList.clear();
	}
	
	
			
	public void deleteCenterAMPMTextBitmap() {
		int numbers = mTimePickerCenterMeridiemTextBitmapsList.size();
		for (int i=0; i<numbers; i++) {
			Bitmap obj = mTimePickerCenterMeridiemTextBitmapsList.get(i);
			obj.recycle();
		}
		
		mTimePickerCenterMeridiemTextBitmapsList.clear();
	}
	
	
	
	public void deleteYearMonthDateTextBitmap() {
		mTimePickerYearTextBitmap.recycle();		
		mTimePickerMonthTextBitmap.recycle();
		mTimePickerDateTextBitmap.recycle();
	}
	
	
	
	public void deleteCenterYearMonthDateTextBitmap() {
		mTimePickerCenterYearTextBitmap.recycle();
		mTimePickerCenterMonthTextBitmap.recycle();
		mTimePickerCenterDateTextBitmap.recycle();
	}	
	
	
	
	public void deleteEraseBackgroundBitmap() {
		mEraseBackgroundBitmap.recycle();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	float mMiniTimePickerYearFiedlCircleRadiusPixels;
	float mMiniTimePickerYearFieldCircleDiameterPixels;
	float mMiniTimePickerHeightPixels;
	float mMiniTimeItemHeightOfTimePickerField;
	float mMiniTimePickerTextSize;	
	public void createMiniTimePickerDimens(int height) {
		mMiniTimePickerYearFiedlCircleRadiusPixels = (float)(height * TimePickerRender.TIMEPICKER_FIELD_CIRCLE_RADIUS);
		mMiniTimePickerYearFieldCircleDiameterPixels = (float)( 2 * Math.PI * mMiniTimePickerYearFiedlCircleRadiusPixels );
		mMiniTimePickerHeightPixels = (float)(mMiniTimePickerYearFieldCircleDiameterPixels / 2);
		mMiniTimeItemHeightOfTimePickerField = mMiniTimePickerHeightPixels / TimePickerRender.TIMEPICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT;
		mMiniTimePickerTextSize = mMiniTimeItemHeightOfTimePickerField * TIMEPICKER_ITEM_TEXT_SIZE_RATIO;		
	}
}
