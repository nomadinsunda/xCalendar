package com.intheeast.timepicker;

public class TimerPickerUpdateData {	
	public static final int UPDATEMETHOD_SET = 1;
	public static final int UPDATEMETHOD_ADD = 2;
	
	public int mTimerID;
	public int mUpdateField;
	
	public int mYear;
	public int mMonth;
	public int mDate;
	public int mAMPM;
	public int mHour;
	public int mMinute;
	
	
	public TimerPickerUpdateData(int updateField, int Year, int Month, int Date, int AMPM, int Hour, int Minute) {
		mUpdateField = updateField;		
		mYear = Year;
		mMonth = Month;
		mDate = Date;
		mAMPM = AMPM;
		mHour = Hour;
		mMinute = Minute;			
	}	
	
	public TimerPickerUpdateData(int timerID, int updateField, int Year, int Month, int Date, int AMPM, int Hour, int Minute) {
		mTimerID = timerID;
		mUpdateField = updateField;		
		mYear = Year;
		mMonth = Month;
		mDate = Date;
		mAMPM = AMPM;
		mHour = Hour;
		mMinute = Minute;			
	}
	
	public int getTimerID() {
		return mTimerID;
	}
	
	public int getUpdateField() {
		return mUpdateField;
	}	
	
}
