package com.intheeast.timepicker;

import java.util.Calendar;
import java.util.TimeZone;

import android.graphics.Color;
//import android.text.format.Time;

public class TimerPickerTimeText {
	boolean mAllDayTime;
	int mYear;
	int mMonth;
	int mDate;
	int mDayOfWeek;
	int mAMPM;
	int mHour;
	int mMinute;
	String mYearIndicatorText = "년";
	String mMonthIndicatorText = "월";
	String mDateIndicatorText = "일";
	String mComa = ".";
	String mSpace = " ";
	String mColon = ":";
	String mTimeText;
	String mAllDayTimeText;
	int mTextColor;	
	
	public TimerPickerTimeText(Calendar targetCalendar, boolean allDay) {
		mAllDayTime = allDay;
		
		mYear = targetCalendar.get(Calendar.YEAR);
		mMonth = transformMonthToRealNumber(targetCalendar.get(Calendar.MONTH));
		mDate = targetCalendar.get(Calendar.DATE);		
		mDayOfWeek = targetCalendar.get(Calendar.DAY_OF_WEEK);
		
		mAMPM = targetCalendar.get(Calendar.AM_PM);
		mHour = targetCalendar.get(Calendar.HOUR_OF_DAY);
		mMinute = targetCalendar.get(Calendar.MINUTE);
		mTextColor = Color.GRAY;
	}
	
	
	
	
	public boolean makeTimeTextByOtherCompare(Calendar compareCalendar) {
		boolean sameDate = false;
		
		int compareYear = compareCalendar.get(Calendar.YEAR);
		int compareMonth = transformMonthToRealNumber(compareCalendar.get(Calendar.MONTH));
		int compareDate = compareCalendar.get(Calendar.DATE);	
		
		if ( (mYear == compareYear) && (mMonth == compareMonth) && (mDate == compareDate) ) {
			sameDate = true;	
			mTextColor = Color.GRAY;
			
			if (mAllDayTime) {
				makeTimeText();
			}
			else {
				String ampm;
				if (mAMPM == Calendar.AM)
					ampm = "오전";
				else
					ampm = "오후";
				
				int hour = mHour;
				if (hour == 0) { //0은 자정[밤:midnight] 12시를 의미한다
					hour = 24;
				}
				
				if (hour >= 12) { // pm 타임대의 hour
					hour = hour - 12;	
					if (hour == 0) {
						hour = 12; // 정오[낮] 12시를 의미한다
						// 여기서 meridiem를 업데이트할 필요가 있으면 특정 조치를 취한다
					}
				}
				
				String minute = String.valueOf(mMinute);
				if (mMinute < 10) {
					minute = String.valueOf(0) + String.valueOf(mMinute);
				}
				
				mTimeText = ampm + 
						    mSpace + 
						    String.valueOf(hour) + 
						    mColon + 
						    minute;
			}
		}
		else {
			mTextColor = Color.RED;
			makeTimeText();
		}	
		
		return sameDate;
	}
	
	
	
	public void makeTimeText() {	
		if (mAllDayTime) {
			String year = String.valueOf(mYear) + mYearIndicatorText;
			String month = mSpace + String.valueOf(mMonth) + mMonthIndicatorText;	
			String date = mSpace + String.valueOf(mDate) + mDateIndicatorText;		
			
			String dayOfWeek = transformDayOfWeekToString2(mDayOfWeek);
			mTimeText = year + 
					    month + 
					    date + 
					    mSpace + mSpace + 
					    dayOfWeek;
		}
		else {
			String year = String.valueOf(mYear) + mComa;
			String month = mSpace + String.valueOf(mMonth) + mComa;
			String date = mSpace + String.valueOf(mDate) + mComa;
			String ampm = null;
			if (mAMPM == Calendar.AM)
				ampm = "오전";
			else
				ampm = "오후";
			
			int hour = mHour;
			if (hour == 0) { //0은 자정[밤:midnight] 12시를 의미한다
				hour = 24;
			}
			
			if (hour >= 12) { // pm 타임대의 hour
				hour = hour - 12;	
				if (hour == 0) {
					hour = 12; // 정오[낮] 12시를 의미한다
					// 여기서 meridiem를 업데이트할 필요가 있으면 특정 조치를 취한다
				}
			}
			
			String minute = String.valueOf(mMinute);
			if (mMinute < 10) {
				minute = String.valueOf(0) + String.valueOf(mMinute);
			}
			
			mTimeText = year + 
					    month + 
					    date + 
					    mSpace + mSpace + mSpace +
					    ampm + 
					    mSpace + 
					    String.valueOf(hour) + 
					    mColon + 
					    minute;
			}
	}	
	
	public String getTimeText() {
		return mTimeText;
	}
	
	public int getTextColor() {
		return mTextColor;
	}
	
	public static int transformMonthToRealNumber(int month) {
		int realMonthNumber = 0;
		switch(month) {
		case 0:
			realMonthNumber = 1;
			break;
		case 1:
			realMonthNumber = 2;
			break;
		case 2:
			realMonthNumber = 3;
			break;
		case 3:
			realMonthNumber = 4;
			break;
		case 4:
			realMonthNumber = 5;
			break;
		case 5:
			realMonthNumber = 6;
			break;
		case 6:
			realMonthNumber = 7;
			break;
		case 7:
			realMonthNumber = 8;
			break;
		case 8:
			realMonthNumber = 9;
			break;
		case 9:
			realMonthNumber = 10;
			break;
		case 10:
			realMonthNumber = 11;
			break;
		case 11:
			realMonthNumber = 12;
			break;
		default:
			realMonthNumber = 0;
			break;
		}
		
		return realMonthNumber;
	}
	
	public static String transformDayOfWeekToString(int dayofweek) {
		String dayOfWeek = new String();
		
		switch(dayofweek) {			
		case Calendar.MONDAY:
			dayOfWeek = "월";
			break;
		case Calendar.TUESDAY:
			dayOfWeek = "화";
			break;
		case Calendar.WEDNESDAY:
			dayOfWeek = "수";
			break;
		case Calendar.THURSDAY:
			dayOfWeek = "목";
			break;
		case Calendar.FRIDAY:
			dayOfWeek = "금";
			break;
		case Calendar.SATURDAY:
			dayOfWeek = "토";
			break;
		case Calendar.SUNDAY:
			dayOfWeek = "일";
			break;
		default:
			dayOfWeek = "X";
			break;
		}
		
		return dayOfWeek;
	}
	
	public static String transformDayOfWeekToString2(int dayofweek) {
		String dayOfWeek = new String();
		
		switch(dayofweek) {			
		case Calendar.MONDAY:
			dayOfWeek = "(월)";
			break;
		case Calendar.TUESDAY:
			dayOfWeek = "(화)";
			break;
		case Calendar.WEDNESDAY:
			dayOfWeek = "(수)";
			break;
		case Calendar.THURSDAY:
			dayOfWeek = "(목)";
			break;
		case Calendar.FRIDAY:
			dayOfWeek = "(금)";
			break;
		case Calendar.SATURDAY:
			dayOfWeek = "(토)";
			break;
		case Calendar.SUNDAY:
			dayOfWeek = "(일)";
			break;
		default:
			dayOfWeek = "X";
			break;
		}
		
		return dayOfWeek;
	}
}
