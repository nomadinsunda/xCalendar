package com.intheeast.timepicker;

import java.util.Calendar;

import android.graphics.SurfaceTexture;
import android.os.HandlerThread;
import android.view.View.OnTouchListener;

public abstract class TimePickerRenderThread extends HandlerThread{

	public String mTimePickerName = null;
	
	public TimePickerRenderThread(String name) {
		super(name);	
		mTimePickerName = new String(name.toString());
	}
	
	public abstract void setSurface(SurfaceTexture surface);
	public abstract void reStartRenderThread();
	public abstract void stopRenderThread();
	public abstract void sleepRenderThread();
	public abstract void wakeUpRenderThread(Calendar time);
	public abstract Calendar getTimeBeforeSleep();
	public abstract OnTouchListener getOnTouchListener();

}
