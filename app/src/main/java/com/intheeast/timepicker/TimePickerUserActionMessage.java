package com.intheeast.timepicker;

public class TimePickerUserActionMessage {
	
	public static final int TIMEPICKER_RUNTIME_VECTORCLASS = 1;
	public static final int TIMEPICKER_AUTO_VECTORCLASS = 2;
	
	public static final int TIMEPICKER_SCROLLING_MSG = 1;
	public static final int TIMEPICKER_FLING_MSG = 2;
	public static final int TIMEPICKER_ACTION_DOWN_MSG = 3;
	public static final int TIMEPICKER_ACTION_UP_MSG = 4;
	public static final int TIMEPICKER_STATUS_CHANGE_MSG = 5;
	public static final int TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG = 6;
	public static final int TIMEPICKER_AUTO_ADJUSTMENT_DAYOFMONTH_FIELD_MSG = 7;	
	public static final int TIMEPICKER_UPDATE_DAYOFMONTH_FIELD_MSG = 8;
	//public static final int RUN_COMPUTE = 7;
	public static final int RUN_SLEEP = 9;
	public static final int RUN_WAKEUP = 10;
	public static final int RUN_EXIT = 11;
	
	int mVectorClass;
	public int mFieldID = 0;
	public int mGestureID = 0;		
	public int mFieldSubID = 0;
	public float mScalar = 0;
	
	public TimePickerUserActionMessage(int FieldID, int GestureID, float scalar) {
		mVectorClass = 0;
		mFieldID = FieldID;
		mGestureID = GestureID;			
		mScalar = scalar;
	}
}
