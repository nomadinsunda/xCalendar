package com.intheeast.acalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
//import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class CalendarViewsLowerActionBarFragment extends Fragment {

	CalendarController mController;
	
	private static String mTimeZone;
	TextView mTodayTextView;
	TextView mCalendarsView;
	TextView mInboxView;	
	
	private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
        	mTimeZone = Utils.getTimeZone(CalendarViewsLowerActionBarFragment.this.getActivity(), mTimeUpdater);
        }
    };
    
	public CalendarViewsLowerActionBarFragment() {
		
	}
	
	public void setTimeZone() {
		mTimeZone = Utils.getTimeZone(getActivity(), mTimeUpdater);
	}
	
	OnClickListener mTodayClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
	        int viewType = ViewType.CURRENT;
	        	        
	        Calendar t = GregorianCalendar.getInstance(TimeZone.getTimeZone(mTimeZone));
            t.setTimeInMillis(System.currentTimeMillis());
            long extras = CalendarController.EXTRA_GOTO_TODAY;
            
            mController.sendEvent(CalendarViewsLowerActionBarFragment.this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
		}		
	};
	
	
	OnClickListener mCalendarClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			           
            mController.sendEvent(CalendarViewsLowerActionBarFragment.this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                    0, 0);
		}		
	};
	
	
	
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		mController = CalendarController.getInstance(activity);
	}

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i("tag", "CalendarViewsLowerActionBarFragment : onCreate");
        
        setTimeZone();
        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		Log.i("tag", "CalendarViewsLowerActionBarFragment : onCreateView");
		
        View v = inflater.inflate(R.layout.calendarviews_lower_actionbar, null);
        
        mTodayTextView = (TextView)v.findViewById(R.id.today_textview);        
		mTodayTextView.setOnClickListener(mTodayClickListener);
		
    	mCalendarsView = (TextView)v.findViewById(R.id.calendar_textview);
    	mCalendarsView.setOnClickListener(mCalendarClickListener);
    	
    	mInboxView = (TextView)v.findViewById(R.id.inbox_textview);
    	
        return v;
	}
	
	@Override
    public void onResume() {
        super.onResume();
        Log.i("tag", "CalendarViewsLowerActionBarFragment : onResume");
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i("tag", "CalendarViewsLowerActionBarFragment : onSaveInstanceState");

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("tag", "CalendarViewsLowerActionBarFragment : onPause");
        
    }
}
