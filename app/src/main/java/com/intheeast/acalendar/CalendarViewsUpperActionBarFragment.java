package com.intheeast.acalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CalendarViewsUpperActionBarFragment extends Fragment implements
	CalendarController.EventHandler {
	
	private static String TAG = "CalendarViewsUpperActionBarFragment";
    private static boolean INFO = true;
    
	private static int DAY_HEADER_HEIGHT = 45;
    private static int MULTI_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;  
    
    Activity mActivity;
	Context mContext;
	
	RelativeLayout mFrameLayout;
	RelativeLayout mActionBarContainer;
	
	ImageView mExpandEventListViewIcon;
	ImageView mCollapseEventListViewIcon;
	ImageView mAgendaListViewOnIcon;
	ImageView mAgendaListViewOffIcon;
	ImageView mSearchEventIcon;
	ImageView mAddNewEventIcon;
	
	
    
    private static CalendarController mController;
    private Resources mResources;
    private final Typeface mBold = Typeface.DEFAULT_BOLD;
    private static Calendar mBaseDate;
    //private Time mCurrentTime;
    private static int mFirstDayOfWeek; // First day of the week
    private static int mFirstJulianDay;
    private static int mLastJulianDay;
    private static int mTodayJulianDay;    
    
    // The current selected event's time, used to calculate the date and day of the week
    // for the buttons.
    private static long mMilliTime;
    private static String mTimeZone;
    
    private static int mMonthLength;
    private static int mVisibleMonth;
    private static int mFirstVisibleDate;
    private static int mFirstVisibleDayOfWeek;
    
    private static int mWeek_saturdayColor;
    private static int mWeek_sundayColor;
    private static int mCalendarDateBannerTextColor;
    
	private static final int[] ACTION_PREVIOUS_ITEM_BG_COLOR = new int[]{254, 252, 252}; // pixlr.com의 포토샵이 오버레이 이후 RGB(255, 255, 255) 값을
																						 // RGB(254, 252, 252)로 변경한다 : 공짜라서 어쩔 수 없다
																					     // 그리고 그라이디언트 도구를 쓰면 안되고 페인트통 도구을 사용해야 한다
																					    // 그리고 안드로이드가 제공하는 액션바 아이콘(holo light)는 완전 투명 알파값이 적용되어 있으므로
																					    // pixlr.com에서 사용할 수가 없다. 그러므로 안드로이드가 제공하는 액션바 아이콘을 그림판에서 일단 열어서 그냥 저장한다
																					    // 그러면 채도? 알파값이 불투명으로 설정된다    
	
	private static final int[] AGENDALIST_ON_BG_COLOR = new int[]{255, 255, 255};
	
	private static final int[] AGENDALIST_OFF_BG_COLOR = new int[]{254, 254, 254};
	
	private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
	private Handler mMidnightHandler = null; // Used to run a time update every midnight
	
	private static int mPreviousMainView;
	// Used to define the look of the menu button according to the current view:
    // Day view: show day of the week + full date underneath
    // Week view: show the month + year
    // Month view: show the month + year
    // Agenda view: show day of the week + full date underneath
    private static int mCurrentMainView;
        
    // Updates time specific variables (time-zone, today's Julian day).
    private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
            refresh(getActivity());
        }
    };
    
    public CalendarViewsUpperActionBarFragment() {
    	mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
    }
    
	@SuppressLint("ValidFragment")
	public CalendarViewsUpperActionBarFragment(Context context) {
		mMilliTime = 0;
		mMidnightHandler = new Handler();
		
		mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());		
		
        mPreviousMainView = ViewType.DETAIL;
		mCurrentMainView = ViewType.DETAIL;
		
		mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);    
	}
		
	@SuppressLint("SuspiciousIndentation")
	public void refresh(Context context) {
		if (INFO) Log.i(TAG, "refresh");
        mTimeZone = Utils.getTimeZone(context, mTimeUpdater);
        mBaseDate = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
        mBaseDate.setFirstDayOfWeek(mFirstDayOfWeek);
        
        Calendar time = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
        long now = System.currentTimeMillis();
        time.setTimeInMillis(now);
        long gmtoff = (TimeZone.getTimeZone(mTimeZone).getRawOffset()) / 1000;
        mTodayJulianDay = ETime.getJulianDay(now, TimeZone.getTimeZone(mTimeZone), mFirstDayOfWeek);
        
        if (mInitedActionBarLayout)
        	refreshSubViews();
        
        //notifyDataSetChanged(); //BaseAdapter의 멤버 변수임!!!
        setMidnightHandler();
    }
	
	// Sets a thread to run 1 second after midnight and update the current date
    // This is used to display correctly the date of yesterday/today/tomorrow
    private void setMidnightHandler() {
        mMidnightHandler.removeCallbacks(mTimeUpdater);
        // Set the time updater to run at 1 second after midnight
        Calendar time = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
        long now = System.currentTimeMillis();
        time.setTimeInMillis(now);
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        
        long runInMillis = (24 * 3600 - hour * 3600 - minute * 60 -
                second + 1) * 1000;
        mMidnightHandler.postDelayed(mTimeUpdater, runInMillis);
    }
    
	private void configActionBarPara() {
    	
    	mResources = getResources();
    		
    	
    	MULTI_DAY_HEADER_HEIGHT = (int) mResources.getDimension(R.dimen.day_header_height); //16dip
    	DAY_HEADER_HEIGHT = MULTI_DAY_HEADER_HEIGHT;    	
    	
        mWeek_saturdayColor = mResources.getColor(R.color.week_saturday);
        mWeek_sundayColor = mResources.getColor(R.color.week_sunday);
        
        mCalendarDateBannerTextColor = mResources.getColor(R.color.calendar_date_banner_text_color);          
    }	
	
	Bitmap mFrameLayoutToBitmap;
	public void makeLayoutBitmap() {
		
		mFrameLayout.destroyDrawingCache();
		
		mFrameLayout.setDrawingCacheEnabled(true);

		mFrameLayout.buildDrawingCache();

		mFrameLayoutToBitmap = mFrameLayout.getDrawingCache();
	}
		
    
	private void drawMonthText() {
		
		mDateIndicatorText.setText(buildMonth());//mDateIndicatorText.setText(ETime.buildMonth(mMilliTime, TimeZone.getTimeZone(mTimeZone)));
	}
	
	private void drawMonthYearText() {
		mDateIndicatorText.setText(buildMonthYearDate());
	}
	
	private void drawYearText() {
		mDateIndicatorText.setText(buildYear());
	}
	
	private void drawNothing() {
		mSuperOrdinateBackActionIcon.setVisibility(View.INVISIBLE);
		mDateIndicatorText.setVisibility(View.INVISIBLE);
		mExpandEventListViewIcon.setVisibility(View.GONE);
	}
	
	OnTouchListener mSuperOrdinateBackActionIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			superOrdinateBackActionTouchAnimation(event);		
			//return true; //mActionBarListener가 호출되지 않음
			return false;
		}    	
    };
    
    OnTouchListener mDateIndicatorTextTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			superOrdinateBackActionTouchAnimation(event);		
			//return true; //mActionBarListener가 호출되지 않음
			return false;
		}    	
    };
    
    public void superOrdinateBackActionTouchAnimation(MotionEvent event) {
    	int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			if (INFO) Log.i(TAG, "superOrdinateBackActionTouchAnimation, ACTION_DOWN");
			
			AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
			downAlpha.setDuration(0); 
			downAlpha.setFillAfter(true);		
			mSuperOrdinateBackActionIcon.startAnimation(downAlpha);
			mDateIndicatorText.setAlpha(0.2f);			
			break;
		case MotionEvent.ACTION_UP:
			if (INFO) Log.i(TAG, "superOrdinateBackActionTouchAnimation, ACTION_UP");			
			
			AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
			upAlpha.setDuration(0); 
			upAlpha.setFillAfter(true); 			
			mSuperOrdinateBackActionIcon.startAnimation(upAlpha);
			mDateIndicatorText.setAlpha(1);				
			break;			
			
		default:
			break;
		}				
    }
    
    OnTouchListener mExpandEventListViewIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			onOffEventListViewTouchListener(event);		
			//return true; //mActionBarListener가 호출되지 않음
			return false;
		}    	
    };
    
    OnTouchListener mCollapseEventListViewIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			onOffEventListViewTouchListener(event);		
			//return true; //mActionBarListener가 호출되지 않음
			return false;
		}    	
    };
    
    
    public void onOffEventListViewTouchListener(MotionEvent event) {
    	
    	int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			if (INFO) Log.i(TAG, "onOffEventListViewTouchListener, ACTION_DOWN");				
			
			if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {				
				mExpandEventListViewIcon.setAlpha(0.2f);
            }
            else {            	
            	mCollapseEventListViewIcon.setAlpha(0.2f);
            }	
					
			break;
			
		case MotionEvent.ACTION_UP:
			if (INFO) Log.i(TAG, "onOffEventListViewTouchListener, ACTION_UP");			
			int xPos = (int) event.getX();
			int yPos = (int) event.getY();
			if (INFO) Log.i(TAG, "xPos=" + String.valueOf(xPos) + ", yPos=" + String.valueOf(yPos));			
			
			/*AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
			upAlpha.setDuration(0);			
			upAlpha.setFillAfter(true);*/
			
			if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
				/*upAlpha.setAnimationListener(mExpandEventListViewIconUpAlphaAnimlistener);
				mExpandEventListViewIcon.startAnimation(upAlpha);*/
				//int[] location = new int[];
				//mExpandEventListViewIcon.getLocationOnScreen(location);
				/*
				float viewXPos = mExpandEventListViewIcon.getX();
				float viewYPos = mExpandEventListViewIcon.getY();
				if (INFO) Log.i(TAG, "EventList onOff,, xPos=" + String.valueOf(viewXPos) + ", yPos=" + String.valueOf(viewYPos));
				*/			
				
				int width = mExpandEventListViewIcon.getWidth();
				int height = mExpandEventListViewIcon.getHeight();
				if (INFO) Log.i(TAG, "EventList onOff,, width=" + String.valueOf(width) + ", height=" + String.valueOf(height));
				
				Rect viewRect = new Rect();
				viewRect.set(0, 0, width, height);
				if (viewRect.contains(xPos, yPos)) {
					mExpandEventListViewIcon.setVisibility(View.GONE);
					mCollapseEventListViewIcon.setVisibility(View.VISIBLE);
					mCollapseEventListViewIcon.setAlpha(1.0f);
				}
				else {
					mExpandEventListViewIcon.setAlpha(1.0f);
				}
            }
            else {      
            	int width = mCollapseEventListViewIcon.getWidth();
				int height = mCollapseEventListViewIcon.getHeight();
				if (INFO) Log.i(TAG, "EventList onOff,, width=" + String.valueOf(width) + ", height=" + String.valueOf(height));
				
				Rect viewRect = new Rect();
				viewRect.set(0, 0, width, height);
				if (viewRect.contains(xPos, yPos)) {
					mCollapseEventListViewIcon.setVisibility(View.GONE);
					mExpandEventListViewIcon.setVisibility(View.VISIBLE);
					mExpandEventListViewIcon.setAlpha(1.0f);
				}
				else {
					mCollapseEventListViewIcon.setAlpha(1.0f);
				}				
            }	
									
			break;	
			
		// 혹 이 경우가 해당 아이콘을 터치 다운했지만 이동시킨 후에 해당 아이콘 영역이 아닌 곳에서 터치 업이 된 경우???
		// :아니다 갤럭시 노트에서 펜을 클릭하면 발생한다
		case MotionEvent.ACTION_CANCEL:
			if (INFO) Log.i(TAG, "onOffEventListViewTouchListener, ACTION_CANCEL");	
			// 아래 설정은 ACTION_DOWN이 발생한 후에 ACTION_CANCEL이 발생하였다는 가정하에!!!
			if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
				mExpandEventListViewIcon.setAlpha(1.0f);
			}
			else {
				mCollapseEventListViewIcon.setAlpha(1.0f);
			}
			break;
			
		default:
			break;
		}				
    }
    
    //AgendaListViewOnIcon
    OnTouchListener mAgendaListViewOnIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
				if (INFO) Log.i(TAG, "mAgendaListViewOnIconTouchListener, ACTION_DOWN");				
				
				mAgendaListViewOnIcon.setAlpha(0.2f);										
				break;
				
			case MotionEvent.ACTION_UP:
				if (INFO) Log.i(TAG, "mAgendaListViewOnIconTouchListener, ACTION_UP");	
				
				//mAgendaListViewOnIcon.setVisibility(View.GONE);
				mAgendaListViewOnIcon.setAlpha(1.0f);
				//mAgendaListViewOffIcon.setVisibility(View.VISIBLE);
				
				break;
			}
			
			return false;
		}    	
    };
    
    
    
    AnimationListener mExpandEventListViewIconUpAlphaAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mExpandEventListViewIconUpAlphaAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mExpandEventListViewIcon.setVisibility(View.GONE);
			mCollapseEventListViewIcon.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mCollapseEventListViewIconUpAlphaAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mCollapseEventListViewIconUpAlphaAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mCollapseEventListViewIcon.setVisibility(View.GONE);
			mExpandEventListViewIcon.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
    
	OnTouchListener mSearchEventIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
				if (INFO) Log.i(TAG, "mSearchIconTouchListener, ACTION_DOWN");			
				mSearchEventIcon.setAlpha(0.2f);						
				break;
				
			case MotionEvent.ACTION_UP:
				if (INFO) Log.i(TAG, "mSearchIconTouchListener, ACTION_UP");	
				mSearchEventIcon.setAlpha(1.0f);									
				break;	
				
			// 혹 이 경우가 해당 아이콘을 터치 다운했지만 이동시킨 후에 해당 아이콘 영역이 아닌 곳에서 터치 업이 된 경우???
			case MotionEvent.ACTION_CANCEL:
				if (INFO) Log.i(TAG, "mSearchIconTouchListener, ACTION_CANCEL");		
				break;
				
			default:
				break;
			}				
			//return true; //mActionBarListener가 호출되지 않음
			return false;
		}    	
    };
    
    //mAddNewEventIcon
    OnTouchListener mAddNewEventIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
				if (INFO) Log.i(TAG, "mAddNewEventIconTouchListener, ACTION_DOWN");			
				mAddNewEventIcon.setAlpha(0.2f);						
				break;
				
			case MotionEvent.ACTION_UP:
				if (INFO) Log.i(TAG, "mAddNewEventIconTouchListener, ACTION_UP");	
				mAddNewEventIcon.setAlpha(1.0f);									
				break;	
				
			// 혹 이 경우가 해당 아이콘을 터치 다운했지만 이동시킨 후에 해당 아이콘 영역이 아닌 곳에서 터치 업이 된 경우???
			case MotionEvent.ACTION_CANCEL:
				if (INFO) Log.i(TAG, "mAddNewEventIconTouchListener, ACTION_CANCEL");		
				break;
				
			default:
				break;
			}				
			//return true; //mActionBarListener가 호출되지 않음
			return false;
		}    	
    };
    
	OnClickListener mSuperOrdinateBackActionIconClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			superOrdinateBackActionClick();
		}		
	};
	
	OnClickListener mDateIndicatorTextClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			superOrdinateBackActionClick();
		}		
	};
	
	
	public void superOrdinateBackActionClick() {
		if (mCurrentMainView == ViewType.DAY) {
			// 아직 ViewType.MONTH로 설정하면 안된다
			// ExitDayViewEnterMonthViewAnim.goodbyeDayFragment에서 
			// 진입 에니매이션이 완료되면 다음과 같이 발송한다
			// mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
			// 그러지 않으면 MonthFragment에서 mController.getPreviousViewType()를 호출하였을 경우
			// 이전 뷰가 day로 나오지 않고 month로 나오는 오류가 발생됨
			mController.sendEvent(this, EventType.GO_TO_MONTH_ANIM, null, null, -1, ViewType.DAY);				
		}
		else if (mCurrentMainView == ViewType.MONTH) {
			mController.sendEvent(this, EventType.GO_TO_YEAR_ANIM, null, null, -1, ViewType.MONTH);
		}
	}
	
	// OnClickListener의 문제점
	// -만약 ACTION_DOWN이 EventListViewOnOffIcon의 영역내에서  발생했지만
	//   ACTION_UP이 EventListViewOnOffIcon의 영역 밖에서 발생하면
	//   OnClickListener의 onClick이 호출되지 않는다
	//   :이로 인해 EventListViewOnOffIcon들이 DOWN/UP Alpha Animation을 수행하지만
	//    view 전환이 발생되지 않는 문제점이 발생한다
	//    ->이를 해결하기 위해 onOffEventListViewTouchListener에서 ACTION_UP 좌표의 유효성을 확인해야 한다!!!!!!!!!!!!!!!!
	OnClickListener mEventListViewOnOffIconOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (INFO) Log.i(TAG, "mEventListViewIconOnClickListener");
			
			if (mCurrentMainView == ViewType.MONTH) {
				if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
					mController.sendEvent(this, EventType.EXPAND_EVENT_LISTVIEW, null, null, -1, ViewType.MONTH);
					if (INFO) Log.i(TAG, "mEventListViewIconOnClickListener : NORMAL_MONTH_VIEW_MODE");
				}
				else {
					mController.sendEvent(this, EventType.COLLAPSE_EVENT_VIEW, null, null, -1, ViewType.MONTH);
					if (INFO) Log.i(TAG, "mEventListViewIconOnClickListener : EXPAND_EVENT_LIST_MONTH_VIEW_MODE");
				}
			}
		}		
	};
	
	
	OnClickListener mSearchEventIconClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (INFO) Log.i(TAG, "mSearchEventIconClickListener");	
			//mActionBarContainer.setVisibility(View.GONE);
			//mSearchBarContainer.setVisibility(View.VISIBLE);
			makeLayoutBitmap();
			//(ECalendarApplication)
			ECalendarApplication app = (ECalendarApplication) CalendarViewsUpperActionBarFragment.this.mActivity.getApplication();
			app.storeCalendarActionBarBitmap(mFrameLayoutToBitmap);
			
			mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, null,
					mActivity.getComponentName());			
		}		
	};
	
	OnClickListener mAddNewEventIconClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (INFO) Log.i(TAG, "mAddNewEventIconClickListener");			
			
			CentralActivity activity = (CentralActivity) CalendarViewsUpperActionBarFragment.this.getActivity();
			View contentView = activity.getContentView();
			Utils.makeContentViewBitmap(activity, contentView);	
			
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(mController.getTime());
			
            if (cal.get(Calendar.MINUTE) > 30) {
            	cal.add(Calendar.HOUR_OF_DAY, 1);
            	cal.set(Calendar.MINUTE, 0);
                
            } else if (cal.get(Calendar.MINUTE) > 0 && cal.get(Calendar.MINUTE) < 30) {
            	cal.set(Calendar.MINUTE, 30);
            }
			
            if (INFO) Log.i(TAG, "onClick:" + ETime.format2445(cal).toString());
            
			mController.sendEventRelatedEvent(CalendarViewsUpperActionBarFragment.this, EventType.CREATE_EVENT, -1, cal.getTimeInMillis(), -1, 0, 0, -1);
			/*
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setClass(mActivity.getApplicationContext(), ManageEventActivity.class);        
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);  
            //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);
            mActivity.startActivity(intent);   
            */       
		}		
	};
	
	
	OnClickListener mAgendaListViewOnIconOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mCurrentMainView == ViewType.DAY) {
				if (INFO) Log.i(TAG, "mAgendaListViewOnIconOnClickListener");		
				mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
			}
		}		
	};
	
	OnClickListener mAgendaListViewOffIconOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mCurrentMainView == ViewType.AGENDA) {
				if (INFO) Log.i(TAG, "mAgendaListViewOnIconOnClickListener");
				mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
			}
		}		
	};
	
	
	ImageView mSuperOrdinateBackActionIcon;
    TextView mDateIndicatorText;    
    boolean mInitedActionBarLayout = false;
    //boolean mExpandEventViewMode = false;
    int mCurrentMonthViewMode;
    public void initActionBarLayout(RelativeLayout view) { 
    	mCurrentMonthViewMode = Utils.getSharedPreference(mContext, SettingsFragment.KEY_MONTH_VIEW_LAST_MODE, SettingsFragment.NORMAL_MONTH_VIEW_MODE);
    	
    	/////////
    	recalc();
    	/////////
    	
    	mActionBarContainer = (RelativeLayout) view.findViewById(R.id.actionbar_container);
    	
    	mSuperOrdinateBackActionIcon = (ImageView)mActionBarContainer.findViewById(R.id.superordinate_back);    	
    	mSuperOrdinateBackActionIcon.setOnTouchListener(mSuperOrdinateBackActionIconTouchListener);
    	mSuperOrdinateBackActionIcon.setOnClickListener(mSuperOrdinateBackActionIconClickListener);
    	
		mDateIndicatorText = (TextView) mActionBarContainer.findViewById(R.id.actionbar_indicator_date_text);
		mDateIndicatorText.setOnTouchListener(mDateIndicatorTextTouchListener);
		mDateIndicatorText.setOnClickListener(mDateIndicatorTextClickListener);
		
		mExpandEventListViewIcon = (ImageView)mActionBarContainer.findViewById(R.id.expand_eventlistview_icon);		
		mExpandEventListViewIcon.setOnTouchListener(mExpandEventListViewIconTouchListener);
		mExpandEventListViewIcon.setOnClickListener(mEventListViewOnOffIconOnClickListener);
		
		mCollapseEventListViewIcon = (ImageView)mActionBarContainer.findViewById(R.id.collapse_eventlistview_icon);		
		mCollapseEventListViewIcon.setOnTouchListener(mCollapseEventListViewIconTouchListener);
		mCollapseEventListViewIcon.setOnClickListener(mEventListViewOnOffIconOnClickListener);
		
		mAgendaListViewOnIcon = (ImageView)mActionBarContainer.findViewById(R.id.agendalistview_on_icon);	
		mAgendaListViewOnIcon.setOnTouchListener(mAgendaListViewOnIconTouchListener);
		mAgendaListViewOnIcon.setOnClickListener(mAgendaListViewOnIconOnClickListener);
		
		mAgendaListViewOffIcon = (ImageView)mActionBarContainer.findViewById(R.id.agendalistview_off_icon);		
		mAgendaListViewOffIcon.setOnClickListener(mAgendaListViewOffIconOnClickListener);
		
		mSearchEventIcon = (ImageView)mActionBarContainer.findViewById(R.id.search_event_icon);		
		mSearchEventIcon.setOnTouchListener(mSearchEventIconTouchListener);
		mSearchEventIcon.setOnClickListener(mSearchEventIconClickListener);
		
		mAddNewEventIcon = (ImageView)mActionBarContainer.findViewById(R.id.add_new_event_icon);		
		mAddNewEventIcon.setOnTouchListener(mAddNewEventIconTouchListener);
		mAddNewEventIcon.setOnClickListener(mAddNewEventIconClickListener);
				
    	if (mCurrentMainView == ViewType.DAY) {
    		mAgendaListViewOnIcon.setVisibility(View.VISIBLE);
    		mAgendaListViewOffIcon.setVisibility(View.GONE);
    		
    		mExpandEventListViewIcon.setVisibility(View.GONE);
    		mCollapseEventListViewIcon.setVisibility(View.GONE);
    		drawMonthText();    		
    	}
    	else if (mCurrentMainView == ViewType.MONTH) {    	
    		mAgendaListViewOnIcon.setVisibility(View.GONE); 		
    		mAgendaListViewOffIcon.setVisibility(View.GONE);
    		
            if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
            	mExpandEventListViewIcon.setVisibility(View.VISIBLE);
            	mCollapseEventListViewIcon.setVisibility(View.GONE);
            }
            else {
            	mExpandEventListViewIcon.setVisibility(View.GONE);
            	mCollapseEventListViewIcon.setVisibility(View.VISIBLE);
            }
                        
    		//drawMonthYearText();
            drawYearText();
    	}
    	else if (mCurrentMainView == ViewType.AGENDA) {
    		mAgendaListViewOffIcon.setVisibility(View.VISIBLE);
    		mAgendaListViewOnIcon.setVisibility(View.GONE);    		
    		
    		mExpandEventListViewIcon.setVisibility(View.GONE);
    		mCollapseEventListViewIcon.setVisibility(View.GONE);    		
    		
    		drawMonthYearText();
    	}
    	
    	///////////////////////////////////////////////////////////////////////////////////////////////    	
    	
    	mInitedActionBarLayout = true;
    }
    
    public void refreshSubViews() {
    	if (INFO) Log.i(TAG, "refreshSubViews");
    	
    	if (mCurrentMainView == ViewType.DAY) {
    		drawMonthText();    		
    	}
    	else if (mCurrentMainView == ViewType.MONTH) {
    		if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
    			drawYearText();
    		}
    		else {
    			drawMonthYearText();    
    		}
    				
    	}
    	else if (mCurrentMainView == ViewType.YEAR) {
    		drawNothing();
    	}
    	else if (mCurrentMainView == ViewType.AGENDA) {
    		drawMonthYearText(); 
    	}
    }	

	
	//////////////////////////////////////////////////////////////////////////////////
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		mActivity = activity;
		
		
	}
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        mContext = getActivity().getApplicationContext();        
        
        mController = CalendarController.getInstance(mActivity);
        
        configActionBarPara();
    }
	
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		if (INFO) Log.i(TAG, "onCreateView");
		mFrameLayout = (RelativeLayout) inflater.inflate(R.layout.calendarviews_upper_actionbar, null);
        
        initActionBarLayout(mFrameLayout);
        return mFrameLayout;
	}
	
	@Override
	public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
		if (INFO) Log.i(TAG, "onCreateAnimator");
		return super.onCreateAnimator(transit, enter, nextAnim);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (INFO) Log.i(TAG, "onViewCreated");
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		if (INFO) Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (INFO) Log.i(TAG, "onStart");
		
	}
	
	@Override
    public void onResume() {
        super.onResume();
        if (INFO) Log.i(TAG, "onResume");
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (INFO) Log.i(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}	

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (INFO) Log.i(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}

	
	//////////////////////////////////////////////////////////////////////////////////
	@Override
    public void onPause() {
		if (INFO) Log.i(TAG, "onPause");
		
		if (mCurrentMainView == ViewType.MONTH)
			Utils.setSharedPreference(mContext, SettingsFragment.KEY_MONTH_VIEW_LAST_MODE, mCurrentMonthViewMode);
		
        super.onPause();               
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (INFO) Log.i(TAG, "onSaveInstanceState");

    }	
	
	@Override
	public void onStop() {
		if (INFO) Log.i(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		if (INFO) Log.i(TAG, "onDestroyView");
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if (INFO) Log.i(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		if (INFO) Log.i(TAG, "onDetach");
		
		super.onDetach();
	}
	   
    
    // Updates the current viewType
    // Used to match the label on the menu button with the calendar view
    // setMainView를 호출하는 CentralActivity 메소드
    // -initFragments
    // -handleEvent     
    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public void setMainView(int viewType) {
    	mPreviousMainView = mCurrentMainView;
        mCurrentMainView = viewType;
        
        if (INFO) {
	        switch(mPreviousMainView) {
	        case ViewType.DETAIL:
	        	if (INFO) Log.i(TAG, "setMainView : prvMainView = DETAIL");
	        	break;
	        case ViewType.DAY:
	        	if (INFO) Log.i(TAG, "setMainView : prvMainView = DAY");
	        	break;
	        case ViewType.MONTH:
	        	if (INFO) Log.i(TAG, "setMainView : prvMainView = MONTH");
	        	break;
	        case ViewType.YEAR:
	        	if (INFO) Log.i(TAG, "setMainView : prvMainView = YEAR");
	        	break;
	        case ViewType.AGENDA:
	        	if (INFO) Log.i(TAG, "setMainView : prvMainView = AGENDA");
	        	break;
	        }
	        
	        switch(mCurrentMainView) {
	        case ViewType.DETAIL:
	        	if (INFO) Log.i(TAG, "setMainView : curMainView = DETAIL");
	        	break;
	        case ViewType.DAY:
	        	if (INFO) Log.i(TAG, "setMainView : curMainView = DAY");
	        	break;
	        case ViewType.MONTH:
	        	if (INFO) Log.i(TAG, "setMainView : curMainView = MONTH");
	        	break;
	        case ViewType.YEAR:
	        	if (INFO) Log.i(TAG, "setMainView : curMainView = YEAR");
	        	break;
	        case ViewType.AGENDA:
	        	if (INFO) Log.i(TAG, "setMainView : curMainView = AGENDA");
	        	break;
	        }
        }
            	
        if (mPreviousMainView == mCurrentMainView) {
           	return;
        }        
        
        if (mInitedActionBarLayout == true) {
	        if (mCurrentMainView == ViewType.DAY) {	 
	        	if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : setMainView : DAY");
	        	
	        	if (mPreviousMainView == ViewType.MONTH) {
	        		mExpandEventListViewIcon.setVisibility(View.GONE);
		        	mAgendaListViewOnIcon.setVisibility(View.VISIBLE);
	        	}
	        	else if (mPreviousMainView == ViewType.AGENDA) {
	        		mAgendaListViewOffIcon.setVisibility(View.GONE);
	        		mAgendaListViewOnIcon.setVisibility(View.VISIBLE);	        		
	        	}		        	
	        }
	        else if (mCurrentMainView == ViewType.AGENDA) {
	        	// 주의 사항
	        	// -agenda view는 month view에서 전환될 수도 있고
	        	//  day view에서도 전환될 수 있다
	        	if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : setMainView : AGENDA");
	        	
	        	if (mPreviousMainView == ViewType.DAY) {
	        		mAgendaListViewOnIcon.setVisibility(View.GONE);
	        		mAgendaListViewOffIcon.setVisibility(View.VISIBLE);
	        	}
	        	else if (mPreviousMainView == ViewType.MONTH) {
	        		mExpandEventListViewIcon.setVisibility(View.GONE);
	        		mAgendaListViewOffIcon.setVisibility(View.VISIBLE);
	        	}	        	
	        }
	        else if (mCurrentMainView == ViewType.MONTH) {
	        	if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : setMainView : MONTH");
	        	mAgendaListViewOnIcon.setVisibility(View.GONE);
	        	mAgendaListViewOffIcon.setVisibility(View.GONE);
	        	
	        	if (mSuperOrdinateBackActionIcon.getVisibility() != View.VISIBLE)
	        		mSuperOrdinateBackActionIcon.setVisibility(View.VISIBLE);
	        	if (mDateIndicatorText.getVisibility() != View.VISIBLE)
	        		mDateIndicatorText.setVisibility(View.VISIBLE);
	        	
	        	// 여기서 잘못되었다
	        	if (mCurrentMonthViewMode == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {
	        		if (INFO) Log.i(TAG, "setMainView : NORMAL_MONTH_VIEW_MODE");
	        		if (mExpandEventListViewIcon.getVisibility() != View.VISIBLE)
		        		mExpandEventListViewIcon.setVisibility(View.VISIBLE);	  
	            	mCollapseEventListViewIcon.setVisibility(View.GONE);
	            }
	            else {
	            	if (INFO) Log.i(TAG, "setMainView : EXPAND_EVENT_LIST_MONTH_VIEW_MODE");
	            	mExpandEventListViewIcon.setVisibility(View.GONE);
	            	if (mCollapseEventListViewIcon.getVisibility() != View.VISIBLE)
	            		mCollapseEventListViewIcon.setVisibility(View.VISIBLE);	  
	            }	        	
	        	  		
	        }
	        else if (mCurrentMainView == ViewType.YEAR) {
	        	if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : setMainView : YEAR");
	        	mExpandEventListViewIcon.setVisibility(View.GONE);
	        	mCollapseEventListViewIcon.setVisibility(View.GONE);
	        	mAgendaListViewOnIcon.setVisibility(View.GONE);
	        }	        
	        else
	        	return; // 현재는 일단 리턴한다
	        
	        refreshSubViews();
        }              
    }
	
	public final int SUPER_ORDINATE_BACK_ICON_APPEAR_ANIM_COMPLETION = 0x1;
	public final int SUPER_ORDINATE_BACK_ICON_DISAPPEAR_ANIM_COMPLETION = 0x2;
	public final int DATE_INDICATOR_TEXT_APPEAR_ANIM_COMPLETION = 0x4;
	public final int DATE_INDICATOR_TEXT_DISAPPEAR_ANIM_COMPLETION = 0x8;
	public final int AGENDA_LISTVIEW_ON_ICON_APPEAR_ANIM_COMPLETION = 0x10;
	public final int AGENDA_LISTVIEW_ON_ICON_DISAPPEAR_ANIM_COMPLETION = 0x20;
	public final int EXPAND_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION = 0x40;
	public final int EXPAND_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION = 0x80;
	public final int COLLAPSE_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION = 0x100;
	public final int COLLAPSE_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION = 0x200;
	//public final int _ICON_APPEAR = 1;
	long mSwitchFragmentAlphaAnimMillis;
	long mSwitchFragmentAlphaAnimDuration;
	
	AlphaAnimation mSuperOrdinateBackActionIconDisappearAlphaAnim;
	AlphaAnimation mSuperOrdinateBackActionIconAppearAlphaAnim;
	
	AlphaAnimation mDateIndicatorTextDisappearAlphaAnim;
	AlphaAnimation mDateIndicatorTextAppearAlphaAnim;
	
	AlphaAnimation mExpandEventListViewIconDisappearAlphaAnim;
	AlphaAnimation mExpandEventListViewIconAppearAlphaAnim;
	
	AlphaAnimation mCollapseEventListViewIconDisappearAlphaAnim;
	AlphaAnimation mCollapseEventListViewIconAppearAlphaAnim;	
	
	AlphaAnimation mAgendaListViewOnIconDisappearAlphaAnim;
	AlphaAnimation mAgendaListViewOnIconAppearAlphaAnim;
	
	AnimationListener mSuperOrdinateBackActionIconDisappearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mSuperOrdinateBackActionIconDisappearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mSuperOrdinateBackActionIcon.setVisibility(View.INVISIBLE);
			orSwitchFragmentAnimFlags(SUPER_ORDINATE_BACK_ICON_DISAPPEAR_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mSuperOrdinateBackActionIconAppearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mSuperOrdinateBackActionIconAppearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			orSwitchFragmentAnimFlags(SUPER_ORDINATE_BACK_ICON_APPEAR_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mDateIndicatorTextDisappearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mDateIndicatorTextDisappearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			orSwitchFragmentAnimFlags(DATE_INDICATOR_TEXT_DISAPPEAR_ANIM_COMPLETION);
			
			if (mCurrentMainView == ViewType.DAY) {
				if (mSwitchViewType == ViewType.MONTH) {	
					mDateIndicatorText.setText(mSwitchFragmentText);
					mDateIndicatorText.startAnimation(mDateIndicatorTextAppearAlphaAnim);
				}
			}
			else if (mCurrentMainView == ViewType.MONTH) {
				if (mSwitchViewType == ViewType.DAY) {	
					mDateIndicatorText.setText(mSwitchFragmentText);
					mDateIndicatorText.startAnimation(mDateIndicatorTextAppearAlphaAnim);
				}
				else if (mSwitchViewType == ViewType.YEAR) {
					mDateIndicatorText.setVisibility(View.INVISIBLE);
				}
			}
			
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mDateIndicatorTextAppearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mDateIndicatorTextAppearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			orSwitchFragmentAnimFlags(DATE_INDICATOR_TEXT_APPEAR_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	
	AnimationListener mExpandEventListViewIconDisappearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mExpandEventListViewIconDisappearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			orSwitchFragmentAnimFlags(EXPAND_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION);
			
			if (INFO) Log.i(TAG, "mExpandEventListViewIconDisappearAlphaAnimListener, end");
			if (mCurrentMainView == ViewType.MONTH) {
				if (mSwitchViewType == ViewType.DAY) {	
					mExpandEventListViewIcon.setVisibility(View.GONE);					
					mAgendaListViewOnIcon.setVisibility(View.VISIBLE);
					mAgendaListViewOnIcon.startAnimation(mAgendaListViewOnIconAppearAlphaAnim);
				}
				else if (mSwitchViewType == ViewType.YEAR) {
					mExpandEventListViewIcon.setVisibility(View.GONE);	
				}
			}
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mExpandEventListViewIconAppearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mExpandEventListViewIconAppearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			orSwitchFragmentAnimFlags(EXPAND_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mCollapseEventListViewIconDisappearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mCollapseEventListViewIconDisappearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			orSwitchFragmentAnimFlags(COLLAPSE_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION);
			
			if (INFO) Log.i(TAG, "mCollapseEventListViewIconDisappearAlphaAnimListener, end");
			int mv = mCurrentMainView;
			int sv = mSwitchViewType;
			if (mCurrentMainView == ViewType.MONTH) {
				if (mSwitchViewType == ViewType.YEAR) {
					mCollapseEventListViewIcon.setVisibility(View.GONE);	
				}
			}		
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mCollapseEventListViewIconAppearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mCollapseEventListViewIconAppearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			orSwitchFragmentAnimFlags(COLLAPSE_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mAgendaListViewOnIconDisappearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mExpandEventListViewIconAppearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			orSwitchFragmentAnimFlags(AGENDA_LISTVIEW_ON_ICON_DISAPPEAR_ANIM_COMPLETION);
			
			mAgendaListViewOnIcon.setVisibility(View.GONE);
			
			mExpandEventListViewIcon.setVisibility(View.VISIBLE);
			mExpandEventListViewIcon.startAnimation(mExpandEventListViewIconAppearAlphaAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	
	AnimationListener mAgendaListViewOnIconAppearAlphaAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mAgendaListViewOnIconAppearAlphaAnimListener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			orSwitchFragmentAnimFlags(AGENDA_LISTVIEW_ON_ICON_APPEAR_ANIM_COMPLETION);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	
	
	public int SwitchFragmentAnimORed;
	String mSwitchFragmentText;
	int mSwitchViewType;
	Runnable mRequestFragmentCallback = null;
	public void setSwitchFragmentAnim(int switchViewType, long timeMillis, long animDuration, int msg, Runnable requestFragmentCallback) {
		SwitchFragmentAnimORed = 0;
		mOredSwitchFragmentAnimFlags = 0;
		mSwitchViewType = switchViewType;
		mSwitchFragmentAlphaAnimMillis = timeMillis;
		mSwitchFragmentAlphaAnimDuration = animDuration;		
		mRequestFragmentCallback = requestFragmentCallback;
		
		if (mCurrentMainView == ViewType.DAY) {
			if (switchViewType == ViewType.MONTH) {	   
				mSwitchFragmentText = buildYear(mSwitchFragmentAlphaAnimMillis);
				
				mDateIndicatorTextDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
	        	mDateIndicatorTextDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2); 	        	
	        	mDateIndicatorTextDisappearAlphaAnim.setAnimationListener(mDateIndicatorTextDisappearAlphaAnimListener);
	        	SwitchFragmentAnimORed |= DATE_INDICATOR_TEXT_DISAPPEAR_ANIM_COMPLETION;
	        	
	        	mDateIndicatorTextAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
				mDateIndicatorTextAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2);         	
				mDateIndicatorTextAppearAlphaAnim.setAnimationListener(mDateIndicatorTextAppearAlphaAnimListener);
				SwitchFragmentAnimORed |= DATE_INDICATOR_TEXT_APPEAR_ANIM_COMPLETION;
				
				mAgendaListViewOnIconDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
				mAgendaListViewOnIconDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2);         	
				mAgendaListViewOnIconDisappearAlphaAnim.setAnimationListener(mAgendaListViewOnIconDisappearAlphaAnimListener);
				SwitchFragmentAnimORed |= AGENDA_LISTVIEW_ON_ICON_DISAPPEAR_ANIM_COMPLETION;
				
				mExpandEventListViewIconAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
				mExpandEventListViewIconAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2);         	
				mExpandEventListViewIconAppearAlphaAnim.setAnimationListener(mExpandEventListViewIconAppearAlphaAnimListener);
				SwitchFragmentAnimORed |= EXPAND_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION;
			}
		}
		else if (mCurrentMainView == ViewType.MONTH) {
			if (switchViewType == ViewType.DAY) {	        	
	        	
				mSwitchFragmentText = buildMonth(mSwitchFragmentAlphaAnimMillis);
				
	        	mDateIndicatorTextDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
	        	mDateIndicatorTextDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2); 	        	
	        	mDateIndicatorTextDisappearAlphaAnim.setAnimationListener(mDateIndicatorTextDisappearAlphaAnimListener);
	        	SwitchFragmentAnimORed |= DATE_INDICATOR_TEXT_DISAPPEAR_ANIM_COMPLETION;
	        	
	        	mDateIndicatorTextAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
				mDateIndicatorTextAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2);         	
				mDateIndicatorTextAppearAlphaAnim.setAnimationListener(mDateIndicatorTextAppearAlphaAnimListener);
				SwitchFragmentAnimORed |= DATE_INDICATOR_TEXT_APPEAR_ANIM_COMPLETION;
				
	        	mExpandEventListViewIconDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
	        	mExpandEventListViewIconDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2); 	        	
	        	mExpandEventListViewIconDisappearAlphaAnim.setAnimationListener(mExpandEventListViewIconDisappearAlphaAnimListener);
	        	SwitchFragmentAnimORed |= EXPAND_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION;
	        	
	        	mAgendaListViewOnIconAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
				mAgendaListViewOnIconAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration / 2);   
				mAgendaListViewOnIconAppearAlphaAnim.setAnimationListener(mAgendaListViewOnIconAppearAlphaAnimListener);
				SwitchFragmentAnimORed |= AGENDA_LISTVIEW_ON_ICON_APPEAR_ANIM_COMPLETION;
			}
			else if (switchViewType == ViewType.YEAR) {
				mSuperOrdinateBackActionIconDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
				mSuperOrdinateBackActionIconDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration); 	        	
				mSuperOrdinateBackActionIconDisappearAlphaAnim.setAnimationListener(mSuperOrdinateBackActionIconDisappearAlphaAnimListener);
				SwitchFragmentAnimORed |= SUPER_ORDINATE_BACK_ICON_DISAPPEAR_ANIM_COMPLETION;
				
				mDateIndicatorTextDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
	        	mDateIndicatorTextDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration); 	        	
	        	mDateIndicatorTextDisappearAlphaAnim.setAnimationListener(mDateIndicatorTextDisappearAlphaAnimListener);
	        	SwitchFragmentAnimORed |= DATE_INDICATOR_TEXT_DISAPPEAR_ANIM_COMPLETION;
				if (msg == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {		        	
		        	mExpandEventListViewIconDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
		        	mExpandEventListViewIconDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration); 	        	
		        	mExpandEventListViewIconDisappearAlphaAnim.setAnimationListener(mExpandEventListViewIconDisappearAlphaAnimListener); 	
		        	SwitchFragmentAnimORed |= EXPAND_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION;
				}
				else {					
					mCollapseEventListViewIconDisappearAlphaAnim = new AlphaAnimation(1.0f, 0.0f); 
					mCollapseEventListViewIconDisappearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration); 	        	
					mCollapseEventListViewIconDisappearAlphaAnim.setAnimationListener(mCollapseEventListViewIconDisappearAlphaAnimListener); 	
					SwitchFragmentAnimORed |= COLLAPSE_EVENT_LISTVIEW_ICON_DISAPPEAR_ANIM_COMPLETION;
				}				
			}
		}
		else if (mCurrentMainView == ViewType.YEAR) {
			if (switchViewType == ViewType.MONTH) {	   
				mSwitchFragmentText = buildYear(mSwitchFragmentAlphaAnimMillis);
				
				mSuperOrdinateBackActionIcon.setVisibility(View.VISIBLE);
				mSuperOrdinateBackActionIconAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
				mSuperOrdinateBackActionIconAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration); 
				mSuperOrdinateBackActionIconAppearAlphaAnim.setAnimationListener(mSuperOrdinateBackActionIconAppearAlphaAnimListener); 	
				SwitchFragmentAnimORed |= SUPER_ORDINATE_BACK_ICON_APPEAR_ANIM_COMPLETION;
				
				mDateIndicatorText.setVisibility(View.VISIBLE);
				mDateIndicatorText.setText(mSwitchFragmentText);
	        	
	        	mDateIndicatorTextAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
				mDateIndicatorTextAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration);  
				mDateIndicatorTextAppearAlphaAnim.setAnimationListener(mDateIndicatorTextAppearAlphaAnimListener); 	
				SwitchFragmentAnimORed |= DATE_INDICATOR_TEXT_APPEAR_ANIM_COMPLETION;
				if (msg == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {					
					mExpandEventListViewIcon.setVisibility(View.VISIBLE);
					mExpandEventListViewIconAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
					mExpandEventListViewIconAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration);   
					mExpandEventListViewIconAppearAlphaAnim.setAnimationListener(mExpandEventListViewIconAppearAlphaAnimListener); 	
					SwitchFragmentAnimORed |= EXPAND_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION;
				}
				else {
					mCollapseEventListViewIcon.setVisibility(View.VISIBLE);
					mCollapseEventListViewIconAppearAlphaAnim = new AlphaAnimation(0.0f, 1.0f); 
					mCollapseEventListViewIconAppearAlphaAnim.setDuration(mSwitchFragmentAlphaAnimDuration);    
					mCollapseEventListViewIconAppearAlphaAnim.setAnimationListener(mCollapseEventListViewIconAppearAlphaAnimListener); 	
					SwitchFragmentAnimORed |= COLLAPSE_EVENT_LISTVIEW_ICON_APPEAR_ANIM_COMPLETION;
				}
			}
		}
	}
	
	public void startSwitchFragmentAnim(int msg) {
		if (mCurrentMainView == ViewType.DAY) {
			mDateIndicatorText.startAnimation(mDateIndicatorTextDisappearAlphaAnim);
			mAgendaListViewOnIcon.startAnimation(mAgendaListViewOnIconDisappearAlphaAnim);
		}
		else if (mCurrentMainView == ViewType.MONTH) {
			if (mSwitchViewType == ViewType.DAY) {	 
				mDateIndicatorText.startAnimation(mDateIndicatorTextDisappearAlphaAnim);
				mExpandEventListViewIcon.startAnimation(mExpandEventListViewIconDisappearAlphaAnim);
			}
			else if (mSwitchViewType == ViewType.YEAR) {				
				mSuperOrdinateBackActionIcon.startAnimation(mSuperOrdinateBackActionIconDisappearAlphaAnim);
				mDateIndicatorText.startAnimation(mDateIndicatorTextDisappearAlphaAnim);
				if (msg == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {		        	
					mExpandEventListViewIcon.startAnimation(mExpandEventListViewIconDisappearAlphaAnim);
				}
				else {
					mCollapseEventListViewIcon.startAnimation(mCollapseEventListViewIconDisappearAlphaAnim);
				}				
			}
		}
		else if (mCurrentMainView == ViewType.YEAR) {
			mSuperOrdinateBackActionIcon.startAnimation(mSuperOrdinateBackActionIconAppearAlphaAnim);
			mDateIndicatorText.startAnimation(mDateIndicatorTextAppearAlphaAnim);
			
			
			if (msg == SettingsFragment.NORMAL_MONTH_VIEW_MODE) {		        	
				mExpandEventListViewIcon.startAnimation(mExpandEventListViewIconAppearAlphaAnim);
			}
			else {
				mCollapseEventListViewIcon.startAnimation(mCollapseEventListViewIconAppearAlphaAnim);
			}	
		}
	}
	
	int mOredSwitchFragmentAnimFlags = 0;
	public void orSwitchFragmentAnimFlags(int flag) {
		mOredSwitchFragmentAnimFlags |= flag;
		if (mOredSwitchFragmentAnimFlags == SwitchFragmentAnimORed) {
			if (mRequestFragmentCallback != null)
				mRequestFragmentCallback.run();
		}
	}

    // Update the date that is displayed on buttons
    // Used when the user selects a new day/week/month to watch
    public void setTime(long time) {
    	
        mMilliTime = time;        
        mBaseDate.setTimeInMillis(mMilliTime);
        
        if (INFO) Log.i(TAG, "setTime:" + ETime.format2445(mBaseDate).toString());
        
        recalc();
        
        if (mInitedActionBarLayout == true) {
        	refreshSubViews();
        }        
    }

    
    public void responseExpandEventView(long eventType) {
    	
    	if (eventType == EventType.EXPAND_EVENT_VIEW_OK) {
    		if (INFO) Log.i(TAG, "responseExpandEventView EXPAND_EVENT_VIEW_OK");
    		//mExpandEventViewMode = true;
    		mCurrentMonthViewMode = SettingsFragment.EXPAND_EVENT_LIST_MONTH_VIEW_MODE;
    		mExpandEventListViewIcon.setVisibility(View.GONE);
    		mCollapseEventListViewIcon.setVisibility(View.VISIBLE);
    		
    	}else if (eventType == EventType.EXPAND_EVENT_VIEW_NO_OK) {
    		if (INFO) Log.i(TAG, "response EXPAND_EVENT_VIEW_NO_OK");
    	}
    }
    
    public void responseCollapseEventView(long eventType) {
    	
    	if (eventType == EventType.COLLAPSE_EVENT_VIEW_OK) {
    		if (INFO) Log.i(TAG, "responseCollapseEventView : COLLAPSE_EVENT_VIEW_OK");
    		//mExpandEventViewMode = false;
    		mCurrentMonthViewMode = SettingsFragment.NORMAL_MONTH_VIEW_MODE;
    		mExpandEventListViewIcon.setVisibility(View.VISIBLE);
    		mCollapseEventListViewIcon.setVisibility(View.GONE);
    		
    	}else if (eventType == EventType.COLLAPSE_EVENT_VIEW_NO_OK) {
    		if (INFO) Log.i(TAG, "responseCollapseEventView : COLLAPSE_EVENT_VIEW_NO_OK");
    	}
    }
    
    
    
    private String buildMonthYearDate() {
        mStringBuilder.setLength(0);
        String date = ETime.formatDateTimeRange(
        		getActivity(),
                mFormatter,
                mMilliTime,
                mMilliTime,
                ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_MONTH_DAY
                        | ETime.FORMAT_SHOW_YEAR, mTimeZone).toString();
        return date;
    }
    
    private String buildMonth() {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			getActivity(),
                mFormatter,
                mMilliTime,
                mMilliTime,
                ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_MONTH_DAY
                        | ETime.FORMAT_NO_YEAR, mTimeZone).toString();
    	
    	return date;
    	
    }
    
    private String buildMonth(long millis) {
    	mStringBuilder.setLength(0);
    	String date = ETime.formatDateTimeRange(
    			getActivity(),
                mFormatter,
                millis,
                millis,
                ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_MONTH_DAY
                        | ETime.FORMAT_NO_YEAR, mTimeZone).toString();
    	
    	return date;
    	
    }
    
    private String buildYear() {    	
    	
    	Calendar startDate = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
    	startDate.setTimeInMillis(mMilliTime);
    	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
		Date yyyy = new Date();		
		yyyy.setTime(startDate.getTimeInMillis());
		String year = sdf.format(yyyy);		
		
		return year;
	}
    
    private String buildYear(long millis) {    	
    	
    	Calendar startDate = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
    	startDate.setTimeInMillis(millis);
    	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
		Date yyyy = new Date();		
		yyyy.setTime(startDate.getTimeInMillis());
		String year = sdf.format(yyyy);		
		
		return year;
	}
    
    
    private String buildWeekDate() {
    	// Calculate the start of the week, taking into account the "first day of the week"
        // setting.

    	Calendar t = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
    	t.setFirstDayOfWeek(mFirstDayOfWeek);
        t.setTimeInMillis(mMilliTime);
        
        ETime.adjustStartDayInWeek(t);
                       

        long weekStartTime = t.getTimeInMillis();
        // The end of the week is 6 days after the start of the week
        long weekEndTime = weekStartTime + DateUtils.WEEK_IN_MILLIS - DateUtils.DAY_IN_MILLIS;

        // If week start and end is in 2 different months, use short months names
        Calendar t1 = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
        t.setTimeInMillis(weekEndTime);
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_YEAR;
        if (t.get(Calendar.MONTH) != t1.get(Calendar.MONTH)) {
            flags |= ETime.FORMAT_ABBREV_MONTH;
        }

        mStringBuilder.setLength(0);
        String date = ETime.formatDateTimeRange(getActivity(), mFormatter, weekStartTime,
                weekEndTime, flags, mTimeZone).toString();
         return date;
    }

    private String buildWeekNum() {
        int week = Utils.getWeekNumberFromTime(mMilliTime, getActivity());
        return getActivity().getResources().getQuantityString(R.plurals.weekN, week, week);
    }
    
   
    
    private void recalc() {
        // Set the base date to the beginning of the week 
    	// if we are displaying 7 days at a time.
    	mBaseDate.setFirstDayOfWeek(mFirstDayOfWeek);
    	ETime.adjustStartDayInWeek(mBaseDate);

        final long start = mBaseDate.getTimeInMillis();
        //long gmtoff = (TimeZone.getTimeZone(mTimeZone).getRawOffset()) / 1000;
        mFirstJulianDay = ETime.getJulianDay(start, TimeZone.getTimeZone(mTimeZone), mFirstDayOfWeek);
        mLastJulianDay = mFirstJulianDay + 7 - 1;

        mMonthLength = mBaseDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        mVisibleMonth = mBaseDate.get(Calendar.MONTH);
        mFirstVisibleDate = mBaseDate.get(Calendar.DAY_OF_MONTH);//monthDay;
        mFirstVisibleDayOfWeek = mBaseDate.get(Calendar.DAY_OF_WEEK);//weekDay;
    }
    
    private boolean match(int pixel)
    {        
    	//if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : match");
    	int Red = Color.red(pixel);
    	int Green = Color.green(pixel);
    	int Blue = Color.blue(pixel);
    	
    	return Color.red(pixel) == ACTION_PREVIOUS_ITEM_BG_COLOR[0] &&
    			Color.green(pixel) == ACTION_PREVIOUS_ITEM_BG_COLOR[1] &&
    			Color.blue(pixel) == ACTION_PREVIOUS_ITEM_BG_COLOR[2];
    }
    
    private boolean matchAgendaListOnIconBG(int pixel)
    {        
    	//if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : match");
    	int Red = Color.red(pixel);
    	int Green = Color.green(pixel);
    	int Blue = Color.blue(pixel);
    	
    	return Color.red(pixel) == AGENDALIST_ON_BG_COLOR[0] &&
    			Color.green(pixel) == AGENDALIST_ON_BG_COLOR[1] &&
    			Color.blue(pixel) == AGENDALIST_ON_BG_COLOR[2];
    }
    
    private boolean matchAgendaListOffIconBG(int pixel)
    {        
    	//if (INFO) Log.i(TAG, "CalendarViewsUpperActionBar : match");
    	int Red = Color.red(pixel);
    	int Green = Color.green(pixel);
    	int Blue = Color.blue(pixel);
    	
    	return Color.red(pixel) == AGENDALIST_OFF_BG_COLOR[0] &&
    			Color.green(pixel) == AGENDALIST_OFF_BG_COLOR[1] &&
    			Color.blue(pixel) == AGENDALIST_OFF_BG_COLOR[2];
    }
    
    
    
    public CharSequence getDateIndicatorText() {
    	return mDateIndicatorText.getText();    	
    }

	@Override
	public long getSupportedEventTypes() {
		return EventType.UPDATE_TITLE;
	}

	@Override
	public void handleEvent(EventInfo event) {
		if (INFO) Log.i(TAG, "handleEvent");
		
		if (event.eventType == EventType.UPDATE_TITLE) {
			
			final long start = event.startTime.getTimeInMillis();
			setTime(start);	        
        }
		
	}

	@Override
	public void eventsChanged() {
		// TODO Auto-generated method stub
		
	}

	
   
}
