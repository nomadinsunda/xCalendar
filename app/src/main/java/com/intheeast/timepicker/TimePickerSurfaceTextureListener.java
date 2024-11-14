package com.intheeast.timepicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

public class TimePickerSurfaceTextureListener implements SurfaceTextureListener {
	TimePickerRenderThread mTimePicker;	
	TextureView mMaster;	
	boolean mTextureAvailable; 
	boolean mSetTimePickerThreadDaemon = false;
	boolean mStartedTimePickerThread = false;
	boolean mDestroyed = false;
	
	public TimePickerSurfaceTextureListener(TimePickerRenderThread timePicker, TextureView master) {
		mTimePicker = timePicker;		
		mMaster = master;			
		mTextureAvailable = false;
	}
	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		
		Log.i("tag", "onSurfaceTextureAvailable:" + mTimePicker.mTimePickerName.toString());
		
		mTimePicker.setSurface(surface);
		
		if (!mSetTimePickerThreadDaemon) {
			mTimePicker.setDaemon(true);
			mSetTimePickerThreadDaemon = true;
		}
		
		if (!mStartedTimePickerThread) {
			mTimePicker.start();    
			mStartedTimePickerThread = true;
		}
		
		if (mDestroyed) {			
			mTimePicker.reStartRenderThread();
			mDestroyed = false;
		}
		
		mMaster.setOnTouchListener(mTimePicker.getOnTouchListener());	
    	
    	mTextureAvailable = true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.i("tag", "onSurfaceTextureDestroyed:" + mTimePicker.mTimePickerName.toString());
		
		mDestroyed = true;
		mTimePicker.stopRenderThread();
		mTextureAvailable = false;
		
		return true;		
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// TODO Auto-generated method stub
	}
	
	public void sleepRenderThread() {
    	mTimePicker.sleepRenderThread();
    }
    
    public void wakeUpRenderThread(Calendar time) {
    	if (mTextureAvailable)
    		mTimePicker.wakeUpRenderThread(time);
    }
    
    public Calendar getRenderTimeBeforeSleep() {
    	return mTimePicker.getTimeBeforeSleep();
    }

}
