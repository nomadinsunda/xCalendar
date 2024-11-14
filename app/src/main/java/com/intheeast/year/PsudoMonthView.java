package com.intheeast.year;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.Event;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;
import com.intheeast.month.MonthWeekView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PsudoMonthView extends View {

	public static final String PSUDO_MINIMONTH_PARAMS_WEEK_UPPERLINE_ALPHA_VALUE = "psudo_minimonth_week_upperline_alpha_value";
		
	public static final int PSUDO_MONTHLISTVIEW_NORMAL_MODE = 1;
	public static final int PSUDO_MONTHLISTVIEW_EXPAND_EVENT_LIST_MODE = 2;
	public int mMonthListViewMode = PSUDO_MONTHLISTVIEW_NORMAL_MODE;

	Context mContext;
	Paint mMonthIndicatorTextPaint;	
	
	
	Paint mMonthViewMonthDayPaint;
	Paint mUpperLinePaint;
	Paint mEventExisteneCirclePaint;
	
	float mUpperLineStrokeWidth;	
	
	boolean mInitialized = false;
	boolean mSetDrawingParams = false;
	
	int mWidth;
	int mHeight;
	
	int mMonthListViewNormalWeekHeight;
	int mMonthIndicatorHeight;
	float mMonthTextBottomPadding;
	
	
	Calendar mMonthTime;	
	String mTzId;
	TimeZone mTimeZone;
	long mGmtoff;
	
	
	
	ArrayList<String[]> mMonthWeekDayNumberList = null;	
	
	int mFirstJulianDay;
	int mNumDays;
	
	int mFirstDayOfWeek;
	
	String mMonthText;
	
		
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    
    int mWeek_saturdayColor;
	int mWeek_sundayColor;
	
	//int TEXT_SIZE_MONTH_NUMBER;
    int mMonthNumColor;
    	
    int mDateTextSize;
    int mMonthTextSize;
    
    int mDateTextTopPadding;
    int mEventCircleTopPadding;
    
    int mDayOfWeekTextWidth;
    float mEventCircleRadius;
    
    int mPrvMonthWeekDaysNum;
    int mSATColor;
    int mSUNColor;
    int mWeekDayColor;
	public PsudoMonthView(Context context) {
		super(context);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
	}
	
	public PsudoMonthView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
		initView(context);
	}
	
	public void initView(Context context) {
		if (!mInitialized) {
			mContext = context;
			mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
			
			mWeek_saturdayColor = getResources().getColor(R.color.week_saturday);
	        mWeek_sundayColor = getResources().getColor(R.color.week_sunday);
			
	        //TEXT_SIZE_MONTH_NUMBER = getResources().getInteger(R.integer.text_size_month_number);
	        mMonthNumColor = getResources().getColor(R.color.month_day_number); 
	        
	        mMonthIndicatorTextPaint = new Paint();
	        mMonthIndicatorTextPaint.setFakeBoldText(false);
	        mMonthIndicatorTextPaint.setAntiAlias(true);	        
	        mMonthIndicatorTextPaint.setColor(mMonthNumColor);
	        mMonthIndicatorTextPaint.setStyle(Style.FILL);        
	        mMonthIndicatorTextPaint.setTextAlign(Align.CENTER);
	        mMonthIndicatorTextPaint.setTypeface(Typeface.DEFAULT);  
	        	        
	        mMonthViewMonthDayPaint = new Paint();
	        mMonthViewMonthDayPaint.setFakeBoldText(false);
	        mMonthViewMonthDayPaint.setAntiAlias(true);	        
	        mMonthViewMonthDayPaint.setColor(getResources().getColor(R.color.month_day_number));
	        mMonthViewMonthDayPaint.setStyle(Style.FILL);        
	        mMonthViewMonthDayPaint.setTextAlign(Align.CENTER);
	        mMonthViewMonthDayPaint.setTypeface(Typeface.DEFAULT);  
	        
	        mUpperLinePaint = new Paint();        
	        mUpperLinePaint.setAntiAlias(true);        
	        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));
	        mUpperLinePaint.setStyle(Style.STROKE);
	        mUpperLinePaint.setStrokeWidth(getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight));
	        
	        mEventExisteneCirclePaint = new Paint();    
	        mEventExisteneCirclePaint.setAntiAlias(true);        
	        mEventExisteneCirclePaint.setColor(getResources().getColor(R.color.eventExistenceCircleColor));
	        mEventExisteneCirclePaint.setStyle(Style.FILL);   
	        	        
	        mSATColor = getResources().getColor(R.color.week_saturday);
	        mSUNColor = getResources().getColor(R.color.week_sunday);
	        mWeekDayColor = getResources().getColor(R.color.month_day_number);
	        mInitialized = true;
		}
	}
		
	
	public void setMonthInfo(Calendar monthTime, ArrayList<String[]> monthWeekDayNumberList) {	
		
		mTimeZone = monthTime.getTimeZone();
		mGmtoff = mTimeZone.getRawOffset() / 1000;
		mTzId = mTimeZone.getID();
		
		mMonthTime = GregorianCalendar.getInstance(mTimeZone);//goToMonthTime;
		mMonthTime.setTimeInMillis(monthTime.getTimeInMillis());
		mFirstJulianDay = ETime.getJulianDay(mMonthTime.getTimeInMillis(), mTimeZone, mFirstDayOfWeek);
		mNumDays = mMonthTime.getActualMaximum(Calendar.DAY_OF_MONTH);
				
		mMonthWeekDayNumberList = monthWeekDayNumberList;
				
		mMonthText = buildMonth(mMonthTime.getTimeInMillis(), mMonthTime.getTimeInMillis());
		
		calPrvMonthWeekDaysNum();
		
	}	
		
	public int getFirstJulianDay() {
		return mFirstJulianDay;
	}
		
	public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        
        createEventExistenceCircle(unsortedEvents);
    }  
    
    List<ArrayList<Event>> mEvents = null;
    public void setEvents(List<ArrayList<Event>> sortedEvents) {    	
        mEvents = sortedEvents;
        if (sortedEvents == null) {
            return;
        }
        if (sortedEvents.size() != mNumDays) {
            if (Log.isLoggable("tag", Log.ERROR)) {
                Log.wtf("tag", "Events size must be same as days displayed: size="
                        + sortedEvents.size() + " days=" + mNumDays);
            }
            mEvents = null;
            return;
        }
    }
    
	boolean mEventExistence[] = null;
    ArrayList<Event> mUnsortedEvents = null;
    public void createEventExistenceCircle(ArrayList<Event> unsortedEvents) {
    	if (unsortedEvents == null) {
            // Stash the list of events for use when this view is ready, or
            // just clear it if a null set has been passed to this view
            mUnsortedEvents = unsortedEvents;            
            mEventExistence = null;
            return;
        } else {
            // clear the cached set of events since we're ready to build it now
            mUnsortedEvents = null;
        }       
        
    	int numDays = mEvents.size();
    	mEventExistence = new boolean[numDays];
    	for (int i=0; i<numDays; i++) {
    		ArrayList<Event> dayEvents = mEvents.get(i);
    		if (dayEvents.size() !=0)
    			mEventExistence[i] = true;
    		else
    			mEventExistence[i] = false;
    	}
    }
    
    int mViewAlpha;
    public void setDimension(int width, int monthIndicatorHeight, int monthNormalWeekHeight, int dateTextSize, int alphaValue) {    		
    	mMonthIndicatorHeight = monthIndicatorHeight;//(int) (height * MonthWeekView.MONTH_INDICATOR_SECTION_HEIGHT);
    	mMonthListViewNormalWeekHeight = monthNormalWeekHeight;//(int) (height * MonthWeekView.NORMAL_WEEK_SECTION_HEIGHT);
		
		mMonthTextBottomPadding = (float)(mMonthIndicatorHeight * 
    			(1 - MonthWeekView.MONTHLISTVIEW_MONTH_TEXT_BASELINE_BY_MONTHLISTVIEW_MONTHINDICATOR_HEIGHT)); 
		
		mDateTextTopPadding = (int) (mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_MONTHDAY_TEXT_BASELINE_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);   
		mEventCircleTopPadding = (int) (mMonthListViewNormalWeekHeight * MonthWeekView.MONTHLISTVIEW_EVENTCIRCLE_TOPPADDING_NMODE_BY_MONTHLISTVIEW_NORMALWEEK_HEIGHT);
				
    	mDateTextSize = dateTextSize;
    	mMonthTextSize = dateTextSize;
    	
    	mViewAlpha = alphaValue;
    	mMonthIndicatorTextPaint.setTextSize(mMonthTextSize);
    	mMonthViewMonthDayPaint.setTextSize(mDateTextSize);
    	
    	mMonthIndicatorTextPaint.setAlpha(mViewAlpha);
    	mUpperLinePaint.setAlpha(mViewAlpha);
    	mEventExisteneCirclePaint.setAlpha(mViewAlpha);
    	
    	int effectiveWidth = width;
    	mDayOfWeekTextWidth = effectiveWidth / 7;
		
    	mEventCircleRadius = (mMonthListViewNormalWeekHeight * 0.1f) / 2;   
    }
    
    boolean mClipTop;
    boolean mClipBottom;
    int mTopClip;
    int mBottomClip;
    
    public void setClipRect(boolean clipTop, int topClip, boolean clipBottom, int bottomClip) {
    	mClipTop = clipTop;
        mClipBottom = clipBottom;
        mTopClip = topClip;
        mBottomClip = bottomClip;    	
    }
	
    Rect mClipRect = new Rect();
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {		
		mWidth = w;
		mHeight = h;		
		
		mClipRect.left = 0;       
		mClipRect.right = mWidth;
		
		if (mClipTop) {
    		mClipRect.top = mTopClip;
    	}
    	else {
    		mClipRect.top = 0;
    	}
    	
    	if (mClipBottom) {
    		mClipRect.bottom = mBottomClip;
    	}
    	else {
    		mClipRect.bottom = mHeight;
    	}
    	
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		        
		canvas.save();
		
		canvas.clipRect(mClipRect);		
		
		drawingMonthView(canvas);	
		
		canvas.restore();
	}	
	
	
	
	public void drawingMonthView(Canvas canvas) {	
			
		float monthIndicatorTextY = mMonthIndicatorHeight - mMonthTextBottomPadding;
		
		canvas.drawText(mMonthText, computeTextXPosition(mPrvMonthWeekDaysNum), monthIndicatorTextY, mMonthIndicatorTextPaint);
				
		float monthDayTextX = 0;
		float monthDayTextY = 0;
		int weekNumbers = mMonthWeekDayNumberList.size();	
		
		int weekLineY = 0;
		int targetDayCount = 0;
		for (int i=0; i<weekNumbers; i++) {			
			
			String[] dayNumbers = mMonthWeekDayNumberList.get(i);		
			monthDayTextY = mMonthIndicatorHeight + (i * mMonthListViewNormalWeekHeight) + mDateTextTopPadding;
			
			int upperLineWidth = 0;		
			for (int j=0; j<7; j++) {
				monthDayTextX = computeTextXPosition(j);				
								
				if (!dayNumbers[j].equalsIgnoreCase(YearMonthTableLayout.NOT_MONTHDAY)) {		
								        
					int color;
		        	final int column = j % 7;
		            if (Utils.isSaturday(column, mFirstDayOfWeek)) {
		                color = mSATColor;
		            } else if (Utils.isSunday(column, mFirstDayOfWeek)) {
		                color = mSUNColor;
		            }
		            else {
		            	color = mWeekDayColor;
		            }
		            
		            mMonthViewMonthDayPaint.setColor(color);
		            mMonthViewMonthDayPaint.setAlpha(mViewAlpha);
		            
					canvas.drawText(dayNumbers[j], monthDayTextX, monthDayTextY, mMonthViewMonthDayPaint);
					
					if (mEventExistence != null) {						
						if (mEventExistence[targetDayCount]) {/////////////////////////////////////////////////////////�̰� ������...
							float cx = monthDayTextX;
							float cy = mMonthIndicatorHeight + (i * mMonthListViewNormalWeekHeight) + mEventCircleTopPadding;
							
							canvas.drawCircle(cx, cy, mEventCircleRadius, mEventExisteneCirclePaint);
						}
					}
					
					upperLineWidth = upperLineWidth + mDayOfWeekTextWidth;
					targetDayCount++;				
				}
			}
			
			if (i==0) {			
				weekLineY = mMonthIndicatorHeight + (int)mUpperLinePaint.getStrokeWidth();				
				canvas.drawLine(mDayOfWeekTextWidth * mPrvMonthWeekDaysNum, weekLineY, mWidth, weekLineY, mUpperLinePaint);		        
			}
			else {
				weekLineY = weekLineY + mMonthListViewNormalWeekHeight;
				canvas.drawLine(0, weekLineY, upperLineWidth, weekLineY, mUpperLinePaint);		
			}
		}
				
		
	}

	
	public void calPrvMonthWeekDaysNum() {
		// month time�� day of month�� �׻� ù°�� '1'�Ϸ� �����Ǿ� ���޵Ǳ� ������...
		// �Ʒ� if���� �����ϴ� ���� month�� ù°���� first day of week�� �����ϴٴ� ���� �ǹ��Ѵ�!!!
		if (mFirstDayOfWeek == mMonthTime.get(Calendar.DAY_OF_WEEK)) {
			mPrvMonthWeekDaysNum = 0;
		}
		else {
			Calendar prvMonth = GregorianCalendar.getInstance(mTimeZone);
			prvMonth.setTimeInMillis(mMonthTime.getTimeInMillis());
			ETime.adjustStartDayInWeek(prvMonth); 
			int maxMonthDays = prvMonth.getActualMaximum(Calendar.DAY_OF_MONTH);       
			mPrvMonthWeekDaysNum = (maxMonthDays - prvMonth.get(Calendar.DAY_OF_MONTH)) + 1;  
		}
	}

	private int computeTextXPosition(int day) {
		int x = 0;
		int effectiveWidth = mWidth;
		int dayOfWeekTextWidth = effectiveWidth / 7;
		int leftSideMargin = dayOfWeekTextWidth / 2;
		
		x = leftSideMargin + (day * dayOfWeekTextWidth);
	    return x;
	}
	
	
	private String buildMonth(long startMillis, long endMillis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			mContext,
                mFormatter,
                startMillis,
                endMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_NO_YEAR, mTzId).toString();
    	
    	return date;    	
    }
}
