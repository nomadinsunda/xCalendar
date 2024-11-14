package com.intheeast.month;

import java.util.Calendar;
import java.util.TimeZone;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.etc.ETime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EventItemView extends LinearLayout {
	
	Context mContext;
	int mWidth;
	int mHeight;
	
	String mTimeZone = ETime.getCurrentTimezone();//Time.getCurrentTimezone();
	
	float mTimeContainerLeftPadding;
	float mTitleContainerLeftPadding;
	float mStrokeWidth;	
	
	LinearLayout mTimeContainer;	
	TextView mStartTime;
	TextView mEndTime;
	TextView mTitle;
	TextView mLocation;
	View mDivider;
	LinearLayout mTitleAndLocationContainer;
	
	String mAmString;
	String mPmString;
	
	public Paint mEventNonePaint;
	public Paint mUpperLinePaint;
	
	Event mEvent = null;
	boolean mMustDrawNonEventText = false;
	boolean mInitedView = false;
	public EventItemView(Context context, int width, int height) {
		super(context);
		mContext = context;
		
		initView(width, height);
	}
	
	public EventItemView(Context context) {
		super(context);
		mContext = context;
		//Log.i("tag", "EventItemView : EventItemView");
	}
	
	public EventItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public EventItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	int mTimeTextViewWidth;
	float mNoneEventTextBaseLine;
	public void initView(int width, int height) {
		if (!mInitedView) {
			setBackground(getResources().getDrawable(R.drawable.eventlist_item_selector));
			// left padding setting
			// : width�� 0.05f		
			mTimeContainerLeftPadding = width * 0.05f;
			mTitleContainerLeftPadding = mTimeContainerLeftPadding * 0.6f;			
			
			mEventNonePaint = new Paint();
			mEventNonePaint.setAntiAlias(true);        
			mEventNonePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
			mEventNonePaint.setTextAlign(Align.CENTER);
			float textSize = height * 0.35f;
			mNoneEventTextBaseLine = height * 0.6f;
			mEventNonePaint.setTextSize(textSize);
			
			mUpperLinePaint = new Paint();        
	        mUpperLinePaint.setAntiAlias(true);        
	        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUpperLinePaint.setTextAlign(Align.CENTER);
	        mUpperLinePaint.setStyle(Style.STROKE);        
	        mStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
	        mUpperLinePaint.setStrokeWidth(mStrokeWidth);
	        
	        mAmString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
	        mPmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
	        
	        calcEventTimeTextWidth();
	        
	        mInitedView = true;
		}
	}
	
	/*
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.i("tag", "EventItemView : onMeasure");
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight);
    }
	*/
	
	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {	
		//Log.i("tag", "EventItemView : onSizeChanged");		
		mWidth = width;
		mHeight = height;		
		super.onSizeChanged(width, height, oldw, oldh);
	}
	
	boolean mSetWillDraw = false;
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		//Log.i("tag", "EventItemView : onLayout");
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false); //LinearLayout�� ����� �Ѵ�			
		}		
	}		
	
	boolean mMadeChildViews = false;
	public void setEvent(Event e, String tz, int itemHeight) {
		//Log.i("tag", "EventItemView : setEvent");
		mEvent = e;	
		mTimeZone = tz;	
		
		if (!mMadeChildViews) {
			makeChildViews(itemHeight);			
		}
		else {						
			if (mTimeContainer.getVisibility() != View.VISIBLE)
				mTimeContainer.setVisibility(View.VISIBLE);
			
			if (mDivider.getVisibility() != View.VISIBLE)
				mDivider.setVisibility(View.VISIBLE);
			
			if (mTitleAndLocationContainer.getVisibility() != View.VISIBLE)
				mTitleAndLocationContainer.setVisibility(View.VISIBLE);
			
		}
		
		setChildViewContents(); 
	}
	
	public void setNoneEvent(boolean drawEventNonText) {
		mEvent = null;
		mMustDrawNonEventText = drawEventNonText;	
		
		if (mMadeChildViews) {
			if (mTimeContainer.getVisibility() != View.GONE)
				mTimeContainer.setVisibility(View.GONE);
			
			if (mDivider.getVisibility() != View.GONE)
				mDivider.setVisibility(View.GONE);
			
			if (mTitleAndLocationContainer.getVisibility() != View.GONE)
				mTitleAndLocationContainer.setVisibility(View.GONE);
		}
		
		invalidate();
	}
	
	public void makeChildViews(int itemHeight) {		
		// Left padding + start time text width
		int timeTextViewHeight = itemHeight / 2;
		int titleTextViewHeight = timeTextViewHeight;
		int timeContainerWidth = (int) (mTimeContainerLeftPadding + mTimeTextViewWidth);
		LayoutParams timeContainerParams = new LayoutParams(timeContainerWidth, itemHeight);			
		mTimeContainer = new LinearLayout(mContext);			
		mTimeContainer.setLayoutParams(timeContainerParams);
		mTimeContainer.setOrientation(VERTICAL);
		mTimeContainer.setPadding((int) mTimeContainerLeftPadding, 0, 0, 0);
		
		LayoutParams titleAndLocationContainerParams = new LayoutParams(LayoutParams.MATCH_PARENT, itemHeight);	
		mTitleAndLocationContainer = new LinearLayout(mContext);
		mTitleAndLocationContainer.setLayoutParams(titleAndLocationContainerParams);
		mTitleAndLocationContainer.setOrientation(VERTICAL);
		mTitleAndLocationContainer.setPadding((int) mTitleContainerLeftPadding, 0, 0, 0);
				
		mDivider = new View(mContext); // container_divider_width
		int dividerTopMargin = (int) getResources().getDimension(R.dimen.container_divider_top_margin);
		int dividerWidth = (int) getResources().getDimension(R.dimen.container_divider_width);
		int dividerHeight = itemHeight - (dividerTopMargin * 2);
		LayoutParams dividerParams = new LayoutParams(dividerWidth, dividerHeight);
		dividerParams.setMargins(0, dividerTopMargin, 0, 0);
		mDivider.setLayoutParams(dividerParams);				
		mDivider.setBackgroundColor(Color.RED);
		
		// WRAP_CONTENT�� �����ϰ� parent�� addview�� �� �� ������ ����
		// :content�� size�� ������� parent�� child���� display �� �� �ִ�
		LayoutParams timeTextParams = new LayoutParams(mTimeTextViewWidth, timeTextViewHeight);	
		mStartTime = new TextView(mContext);	
		mStartTime.setLayoutParams(timeTextParams);
		mStartTime.setGravity(Gravity.CENTER);		
		mStartTime.setTextColor(Color.BLACK);		
		Paint startTimePaint = mStartTime.getPaint();		
		startTimePaint.setTextSize(getResources().getDimension(R.dimen.meev_starttime_text_size));		
		
		mEndTime = new TextView(mContext);	
		mEndTime.setLayoutParams(timeTextParams);
		mEndTime.setGravity(Gravity.CENTER);
		mEndTime.setTextColor(getResources().getColor(R.color.event_layout_value_transparent_color));
		Paint endTimePaint = mEndTime.getPaint();		
		endTimePaint.setTextSize(getResources().getDimension(R.dimen.meev_endtime_text_size));			
		
		LayoutParams titleAndLocationTextParams = new LayoutParams(LayoutParams.MATCH_PARENT, titleTextViewHeight);	
		mTitle = new TextView(mContext);		
		mTitle.setLayoutParams(titleAndLocationTextParams);	
		mTitle.setGravity(Gravity.LEFT | Gravity.BOTTOM);		
		mTitle.setTextColor(Color.BLACK);		
		mTitle.setSingleLine();
		mTitle.setEllipsize(TextUtils.TruncateAt.END);
		Paint titlePaint = mTitle.getPaint();
		titlePaint.setTextSize(getResources().getDimension(R.dimen.meev_title_text_size));		
		
		mLocation = new TextView(mContext);	
		mLocation.setLayoutParams(titleAndLocationTextParams);
		mLocation.setGravity(Gravity.LEFT | Gravity.CENTER);
		mLocation.setTextColor(getResources().getColor(R.color.event_layout_value_transparent_color));		
		mLocation.setSingleLine();
		mLocation.setEllipsize(TextUtils.TruncateAt.END);		
		Paint locationPaint = mLocation.getPaint();
		locationPaint.setTextSize(getResources().getDimension(R.dimen.meev_location_text_size));	
		
		addView(mTimeContainer);
		addView(mDivider);
		addView(mTitleAndLocationContainer);
		
		mTimeContainer.addView(mStartTime);
		mTimeContainer.addView(mEndTime);
		
		mTitleAndLocationContainer.addView(mTitle);
		mTitleAndLocationContainer.addView(mLocation);
		
		mMadeChildViews = true;
	}
	
	public void setChildViewContents() {
		if (mEvent.allDay) {
			mStartTime.setText("�Ϸ� ����");			
			mEndTime.setVisibility(View.GONE);
		}
		else {						
			Calendar evetnTimeCalendar = (Calendar) Calendar.getInstance();
			evetnTimeCalendar.setTimeZone(TimeZone.getTimeZone(mTimeZone));
			evetnTimeCalendar.setTimeInMillis(mEvent.getStartMillis());
			
			String startTimeText = makeEventTimeString(evetnTimeCalendar);
			mStartTime.setText(startTimeText);
			
			evetnTimeCalendar.setTimeInMillis(mEvent.getEndMillis());
			String endTimeText = makeEventTimeString(evetnTimeCalendar);
			mEndTime.setText(endTimeText);
		}
		
		mTitle.setText(mEvent.title);			
		mLocation.setText(mEvent.location);
		
	}
	
	// onDraw���� childView�� content ������ ���� �Ǵ� �����ؼ��� �ȵȴ�
	// :�̴� invalid ���¸� �߻����Ѽ� onLayout�� onDraw �Լ��� ���Ӿ��� ȣ��ǵ��� �ϴ� ������ �߻���Ų��
	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i("tag", "EventItemView : onDraw");
		
		if (mEvent == null) {
			if (mMustDrawNonEventText) {
				canvas.drawText("�̺�Ʈ ����", mWidth / 2, mNoneEventTextBaseLine, mEventNonePaint);
			}			
		}		
		
		float startX = mTimeContainerLeftPadding;
        float startY = mHeight - mStrokeWidth; // y ���� �� ��������� �h��...�� �׷��� �翬�� ©����
        float stopX = mWidth;
        float stopY = startY;
        canvas.drawLine(startX, startY, stopX, stopY, mUpperLinePaint);
		
        super.onDraw(canvas);		
	}	
	
	
	public void calcEventTimeTextWidth() {
		TextView calTimeTextWidth = new TextView(mContext);
        Paint calTimeTextWidthPaint = calTimeTextWidth.getPaint();		
        calTimeTextWidthPaint.setTextSize(getResources().getDimension(R.dimen.meev_starttime_text_size));	
        
        String amMeridiem = mAmString;  
        String pmMeridiem = mPmString; 
        String padding = "0"; // padding ũ�⸦ '0'�� char�� ���Ѵ�
        String space = " ";
        String hour = String.valueOf(12);
        String colon = ":";
        String minute = String.valueOf(59);                
        String amTime = padding + amMeridiem + space + hour + colon + minute + padding;
        String pmTime = padding + pmMeridiem + space + hour + colon + minute + padding;
        
        float amTimeStrSize = calTimeTextWidthPaint.measureText(amTime);        
		float pmTimeStrSize = calTimeTextWidthPaint.measureText(pmTime);  
		
		if (amTimeStrSize > pmTimeStrSize)
			mTimeTextViewWidth = (int) (amTimeStrSize + 0.5f);
		else if (pmTimeStrSize > amTimeStrSize)
			mTimeTextViewWidth = (int) (pmTimeStrSize + 0.5f);
		else
			mTimeTextViewWidth = (int) (amTimeStrSize + 0.5f);
	}
	
	public String makeEventTimeString(Calendar eventTimeCalenar) {
		String meridiem = mAmString;
        if (eventTimeCalenar.get(Calendar.AM_PM) == Calendar.PM) {
        	meridiem = mPmString;
        }        
        String space = " ";
        String hour = String.valueOf(eventTimeCalenar.get(Calendar.HOUR_OF_DAY));
        String colon = ":";
        String minute = String.valueOf(eventTimeCalenar.get(Calendar.MINUTE));
        if (eventTimeCalenar.get(Calendar.MINUTE) < 10) {
        	minute = "0" + minute;
        }        
        
        String eventTime = meridiem + space + hour + colon + minute;
        
        return eventTime;
	}
	
	// none use
	public TextView makeTextView(int width, int height, 
    		int leftMargin, int topMargin, int rightMargin, int bottomMargin,
    		boolean singleLine, 
    		int textApperance, int typeFace, 
    		int textColor,
    		boolean ellipsize, TruncateAt where,
    		int textLinkColor) {
    	
    	TextView tv = new TextView(mContext);
    	
    	LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(width, height);
        llp.setMargins(leftMargin, topMargin, rightMargin, bottomMargin); 
        tv.setLayoutParams(llp);
        tv.setTextAppearance(mContext, textApperance);    
        tv.setSingleLine(singleLine);
        tv.setTypeface(null, typeFace);
        tv.setTextColor(textColor);
        if (ellipsize)
        	tv.setEllipsize(where);
        
        if (textLinkColor != -1) {
        	tv.setLinkTextColor(textLinkColor);
        	tv.setTextIsSelectable(true);
        }                 
        
    	return tv;
    }

}
