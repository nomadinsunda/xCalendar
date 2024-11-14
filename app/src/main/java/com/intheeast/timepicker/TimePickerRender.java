package com.intheeast.timepicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL11;

import com.intheeast.easing.Expo;
import com.intheeast.easing.Linear;
import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;
import com.intheeast.gl.EyePosition;
import com.intheeast.impl.KScroller;
import com.intheeast.impl.gestureFlingMovementVector;
import com.intheeast.impl.gestureScrollMovementVector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
//import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TimePickerRender extends TimePickerRenderThread implements Callback {
	public static final String START_TIME_PICKER_NAME = "StartTimePickerRenderer";
	public static final String END_TIME_PICKER_NAME = "EndTimePickerRenderer";
	public static final int START_TIME_PICKER_ID = 1;
	public static final int END_TIME_PICKER_ID = 2;	
	
	
	public static final int DATE_FIELD_TEXTURE = 1;
	public static final int MERIDIEM_FIELD_TEXTURE = 2;
	public static final int HOUR_FIELD_TEXTURE = 3;
	public static final int MINUTE_FIELD_TEXTURE = 4;	
    
	public static final int UPDATE_DATE_FIELD_OF_TIMEPICKER = 1;
    public static final int UPDATE_MERIDIEM_FIELD_OF_TIMEPICKER = 2;
    public static final int UPDATE_HOUR_FIELD_OF_TIMEPICKER = 3;
    public static final int UPDATE_MINUTE_FIELD_OF_TIMEPICKER = 4;      
    
    public static final double TIMEPICKER_FIELD_CIRCLE_RADIUS = 0.5; // viewport�� height�� ���� ratio��!!!
    public static final double TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE = 180;
    public static final double TIMEPICKER_ITEM_AVAILABLE_MIN_DEGREE = 0;
    public static final double FIELD_ITEM_DEGREE = 20;
    public static final int TIMEPICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT = (int)(TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE / FIELD_ITEM_DEGREE);
    
    private static final double CENTERITEM_TOP_DEGREE = 100;
	private static final double CENTERITEM_BOTTOM_DEGREE = 80;
	public static final float NORMAL_TIMEPICKER_VIEW_ANGLE_OF_VIEW = 20.0f;        
    private static final int ROW_MESH = 2;
	private static final int COLUMN_MESH = 3;	
	    	
	private static final int FLINGCOMPUTETHREAD_FLING_STATUS = 1;
    private static final int FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS = 2;
    private static final int FLINGCOMPUTETHREAD_INTERRUPTED_STATUS = 3;
    
	private static final int IDLE = 0;
    private static final int SCROLLING = 1;
    private static final int FLING = 2;
    private static final int AUTOSCROLLING = 3;
    private static final int AUTOSCROLLINGBYHOURFIELD = 4;
    private static final int SLEEP = 5;    
    
    private static final int EXIT = -1;
    
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final String TAG = "RenderThread";    
    
    private int mTimePickerID = 0;
    
    Activity mActivity;
    private SurfaceTexture mSurface;
    int m_Width;
	int m_Height;    	
	
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private EGLContext mEglContext;
    private int mProgram;
    private EGL10 mEgl;
    private GL11 mGl;
    
    EyePosition m_RunnginStickersVieweyePosition;
	private float[] mModelMatrix = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mMVPMatrix = new float[16];       	
	
	private int mMVPMatrixHandle;	
	private int mTextureUniformHandle;
	private int mPositionHandle;
	private int mTextureCoordinateHandle;
	
	int[] mNumeralTextTextureDataHandles;
	int[] mDayOfWeekTextureDataHandles;
	int[] mTodayTextTextureDataHandles;
	int[] mMeridiemTextTextureDataHandles; 
	int mMonthTextTextTextureDataHandle;
	int mDateTextTextTextureDataHandle; 	  
	
	int[] mCenterNumeralTextTextureDataHandles;	
	int[] mCenterDayOfWeekTextureDataHandles;
	int[] mCenterTodayTextTextureDataHandles;
	int[] mCenterMeridiemTextTextureDataHandles;  
	int mCenterMonthTextTextTextureDataHandle;
	int mCenterDateTextTextTextureDataHandle; 
	
	int mEraseCenterBackgroundTextureDataHandle;
	
	private static final int mBytesPerFloat = 4;	
	private static final int mDataSizePerVertex = 3;	
	private static final int mTextureCoordinateDataSize = 2;  	
	private static final int VERTICES_NUMBERS_PER_TRIANGLE_FOR_DRAW_ARRAYS = 3; 
	private static final int TRIANGLE_NUMBERS_PER_RECTANGLE_DRAW = 2; 
	private static final int VERTICES_NUMBERS_PER_RECTANGLE_FOR_DRAW_ARRAYS = VERTICES_NUMBERS_PER_TRIANGLE_FOR_DRAW_ARRAYS * TRIANGLE_NUMBERS_PER_RECTANGLE_DRAW; 
	private static final int VERTICES_PER_POLYGON_FOR_DRAW_ELEMENTS = 4;
	
	private float mHorizontalGAPBetweenFields;
	
	float m_fovy;
	float m_tanValueOfHalfRadianOfFovy;
	
	static final float m_halfRatioOfViewingVPHeight = 0.5f;
	static final float OBJECT_TARGET_Z_POSITION = 0;
	
	float m_camera_eye_X;
	float m_camera_eye_Y;
	float m_camera_eye_Z;
	float m_camera_lookAt_X;
	float m_camera_lookAt_Y;
	float m_camera_lookAt_Z;
	static final float m_upX = 0;
	static final float m_upY = 1.0f;
	static final float m_upZ = 0;
	
	float mRatio;
	static final float mNear = 0.1f;
	static final float mFar = 10.0f;    
	
	float mLeft = 0;
	float mRight = 0; 
	static final float mBottom = -0.5f; 
	static final float mTop = 0.5f;
	static final float mNear2 = 1;
	static final float mFar2 = 10.0f;
	
	double mCircle_center_z;
	double mCircle_HighLight_center_z;
	private static final double HIGHLIGHT_BACKWARD_OFFSET = 0.25;
	
	Calendar mCalendar;
	Calendar mDateFieldCalendar;	
	Calendar mHourFieldCalendar;
	Calendar mMinuteFieldCalendar;		
	
	double mCurrentDateFieldUppermostItemBottomSideDegree;
    double mCurrentDateFieldLowermostItemTopSideDegree;
    
    double mCurrentMeridiemFieldUppermostItemBottomSideDegree;
    double mCurrentMeridiemFieldLowermostItemTopSideDegree;
    
    double mCurrentHourFieldUppermostItemBottomSideDegree;
    double mCurrentHourFieldLowermostItemTopSideDegree;
    
    double mCurrentMinuteFieldUppermostItemBottomSideDegree;
    double mCurrentMinuteFieldLowermostItemTopSideDegree;      
    
    float m_dateFieldRightMostVPRattio;
    float m_meridiemPickerLeftMostVPRatio;
	float m_hourPickerLeftMostVPRatio;
	float m_minutePickerLeftMostVPRatio;	
	
	float mTimePickerNumeralWidthRatio;
	float mGPABetweenTextRatio;
	float mHourFieldHighLightInterpolationRatio;
	float mMinuteFieldHighLightInterpolationRatio;
	
	float mCenterItemHeightRatio;
	float mCenterItemHalfHeightRatio;
	float mCenterItemRegionTopRatio;
	float mCenterItemRegionBottomRatio;
	
	gestureFlingMovementVector m_dateFieldFlingmovementVector;
    gestureFlingMovementVector m_meridiemFieldFlingmovementVector;
    gestureFlingMovementVector m_hourFieldFlingmovementVector;
    gestureFlingMovementVector m_minuteFieldFlingmovementVector;    
    
	ArrayList<ArrayList<FieldObjectVertices>> mDateFieldRowObjectsVerticesList;
	ArrayList<ArrayList<FieldObjectVertices>> mMeridiemFieldRowObjectsVerticesList;
	ArrayList<ArrayList<FieldObjectVertices>> mHourFieldRowObjectsVerticesList;
	ArrayList<ArrayList<FieldObjectVertices>> mMinuteFieldRowObjectsVerticesList;	
	
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mDateHighLightFieldRowObjectsVerticesList;
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mMeridiemHighLightFieldRowObjectsVerticesList;
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mHourHighLightFieldRowObjectsVerticesList;
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mMinuteHighLightFieldRowObjectsVerticesList;	
	
	int m_isChangedAlphaHandle;
	int m_ChangedAlphaValueHandle;
	Handler mTimeMsgHandler;		    
	//////////////////////////////////////////////////////////
	float mTimePickerTextWdith;
	float mTimePickerTextWidthRatio;
	float mTimePickerNumeralWidth;	
		
	float mTimePickerDateFieldCircleDiameterPixels;
	float m_Ppi;
	
	ArrayList<Bitmap> mTimePickerNumeralTextBitmapsList;
	ArrayList<Bitmap> mTimePickerTodayTextBitmapsList;
	ArrayList<Bitmap> mTimePickerMeridiemTextBitmapsList;
	ArrayList<Bitmap> mTimePickerDayOfWeekTextBitmapsList;
	ArrayList<Bitmap> mTimePickerCenterDayOfWeekTextBitmapsList;
	ArrayList<Bitmap> mTimePickerCenterTodayTextBitmapsList;
	ArrayList<Bitmap> mTimePickerCenterMeridiemTextBitmapsList;
	ArrayList<Bitmap> mCenterTimePickerNumeralBitmapsList;	
	
	Bitmap mTimePickerCenterMonthTextBitmap;
	Bitmap mTimePickerMonthTextBitmap;
	Bitmap mTimePickerDateTextBitmap;	
	Bitmap mTimePickerCenterDateTextBitmap;
	Bitmap mEraseBackgroundBitmap;
	
	TimePickerRenderParameters mParameters;
	GestureDetector mTimePickerGestureDetector;
	//////////////////////////////////////////////////////////	
	
	Context mTextureViewContext;
	
	public TimePickerRender(String name, Context TextureViewContext, 
			Activity activity, 
			/*SurfaceTexture surface,*/ 
			Handler timeMsgHandler, Calendar time, 
			int width, int height,
			TimePickerRenderParameters para) {
		super(name);		
		
		if (name == START_TIME_PICKER_NAME) {
			mTimePickerID = START_TIME_PICKER_ID;
		}
		else if (name == END_TIME_PICKER_NAME) {
			mTimePickerID = END_TIME_PICKER_ID;
		}
		else 
			return;
		
		mTextureViewContext = TextureViewContext;
		mActivity = activity;        
        mTimeMsgHandler = timeMsgHandler;
       
        //mCalendar = (Calendar)Calendar.clone();
        mCalendar = (Calendar)Calendar.getInstance(time.getTimeZone());
        
        mCalendar.setTimeInMillis(time.getTimeInMillis());     
        
        m_Width = width;
        m_Height = height;            
        mParameters = para;
        
        setRenderParameters();
        
        mRatio = (float) (m_Width / m_Height); 
        
		setCameraZPosition(NORMAL_TIMEPICKER_VIEW_ANGLE_OF_VIEW);
		setVeiwEyePosition();          
		setViewMatrix(m_RunnginStickersVieweyePosition);    		
		setPerspectiveProjectionMatrix(m_fovy, mRatio, mNear, mFar);		
		
		mCurrentDateFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentDateFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		int am_pm = mCalendar.get(java.util.Calendar.AM_PM);
		if (am_pm == java.util.Calendar.AM) {
			mCurrentMeridiemFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
            mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree;
		}
		else if (am_pm == java.util.Calendar.PM) {
			mCurrentMeridiemFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
            mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree;
		}
		else {
			mCurrentMeridiemFieldUppermostItemBottomSideDegree = 0;
            mCurrentMeridiemFieldLowermostItemTopSideDegree = 0;
		}
		            
        mCurrentHourFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
        mCurrentHourFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
        
        mCurrentMinuteFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
        mCurrentMinuteFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;     
        
        
        mDateFieldCalendar = (Calendar)mCalendar.clone();        
    	mHourFieldCalendar = (Calendar)mCalendar.clone();
    	mMinuteFieldCalendar = (Calendar)mCalendar.clone();
    	        	
    	
    	double centerItemTop = Math.cos(Math.toRadians(100)) * TIMEPICKER_FIELD_CIRCLE_RADIUS;
		double centerItemBottom = Math.cos(Math.toRadians(80)) * TIMEPICKER_FIELD_CIRCLE_RADIUS;
		mCenterItemHeightRatio = (float)(Math.abs(centerItemTop) + Math.abs(centerItemBottom));
		mCenterItemHalfHeightRatio = mCenterItemHeightRatio / 2;
		
		// ��Ȯ�� �����̴�!!!
		// m_Width : mTimePickerTextWdith = mRatio : mTimePickerTextWidthRatio
		// mTimePickerTextWdith * mRatio = m_Width * mTimePickerTextWidthRatio
		// m_Width * mTimePickerTextWidthRatio = mTimePickerTextWdith * mRatio
		// mTimePickerTextWidthRatio = (mTimePickerTextWdith * mRatio) / m_Width
		// mTimePickerTextWidthRatio = (mTimePickerTextWdith / (float)m_Width) * mRatio;
		mTimePickerTextWidthRatio = (mTimePickerTextWdith * mRatio) / m_Width;
		mHorizontalGAPBetweenFields = mTimePickerTextWidthRatio;
		mGPABetweenTextRatio = mHorizontalGAPBetweenFields * 0.3f;		
		mTimePickerNumeralWidthRatio = (mTimePickerNumeralWidth * mRatio) / m_Width;
		mHourFieldHighLightInterpolationRatio = mTimePickerNumeralWidthRatio * 0.25f;
		mMinuteFieldHighLightInterpolationRatio = mTimePickerNumeralWidthRatio * 0.5f;
		
		m_meridiemPickerLeftMostVPRatio = 0.0f;
		m_dateFieldRightMostVPRattio = m_meridiemPickerLeftMostVPRatio - mHorizontalGAPBetweenFields;
		m_hourPickerLeftMostVPRatio = m_meridiemPickerLeftMostVPRatio + (mTimePickerTextWidthRatio * 2) + (mHorizontalGAPBetweenFields);
		m_minutePickerLeftMostVPRatio = m_hourPickerLeftMostVPRatio + (mTimePickerNumeralWidthRatio * 2) + (mHorizontalGAPBetweenFields * 2);
		mCenterItemRegionTopRatio = 0.0f + mCenterItemHalfHeightRatio;
		mCenterItemRegionBottomRatio = 0.0f - mCenterItemHalfHeightRatio;
		
		m_scrollmovementVectorOfTimePickerDateField = new gestureScrollMovementVector();
		m_scrollmovementVectorOfTimePickerMeridiemField = new gestureScrollMovementVector();
		m_scrollmovementVectorOfTimePickerHourField = new gestureScrollMovementVector();
		m_scrollmovementVectorOfTimePickerMinuteField = new gestureScrollMovementVector();
		
		m_dateFieldFlingmovementVector = new gestureFlingMovementVector();
		m_meridiemFieldFlingmovementVector = new gestureFlingMovementVector();
		m_hourFieldFlingmovementVector = new gestureFlingMovementVector();
        m_minuteFieldFlingmovementVector = new gestureFlingMovementVector();        
        
		mDateFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		mMeridiemFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
    	mHourFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
    	mMinuteFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
    	
    	mDateHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	mMeridiemHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	mHourHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	mMinuteHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	
    	mTimePickerGestureDetector = new GestureDetector(mTextureViewContext, mTimePickerGesturelistener);
    	
		buildTexturePosDataFields();		
	}
	
	public void setRenderParameters() {
		mTimePickerTextWdith = mParameters.getTimePickerTextWdith();
		mTimePickerNumeralWidth = mParameters.getTimePickerNumeralWidth();	
			
		mTimePickerDateFieldCircleDiameterPixels = mParameters.getTimePickerDateFieldCircleDiameterPixels();
		m_Ppi = mParameters.getPpi();
		
		mTimePickerNumeralTextBitmapsList = mParameters.getTimePickerNumeralTextBitmapsList();
		mTimePickerTodayTextBitmapsList = mParameters.getTimePickerTodayTextBitmapsList();
		mTimePickerMeridiemTextBitmapsList = mParameters.getTimePickerMeridiemTextBitmapsList();
		mTimePickerDayOfWeekTextBitmapsList = mParameters.getTimePickerDayOfWeekTextBitmapsList();
		mTimePickerCenterDayOfWeekTextBitmapsList = mParameters.getTimePickerCenterDayOfWeekTextBitmapsList();		
		mTimePickerCenterTodayTextBitmapsList = mParameters.getTimePickerCenterTodayTextBitmapsList();		
		mTimePickerCenterMeridiemTextBitmapsList = mParameters.getTimePickerCenterMeridiemTextBitmapsList();
		mCenterTimePickerNumeralBitmapsList = mParameters.getTimePickerCenterNumeralBitmapsList();
		
		mTimePickerMonthTextBitmap = mParameters.getTimePickerMonthTextBitmap();
		mTimePickerDateTextBitmap = mParameters.getTimePickerDateTextBitmap();
		mTimePickerCenterMonthTextBitmap = mParameters.getTimePickerCenterMonthTextBitmap();
		mTimePickerCenterDateTextBitmap = mParameters.getTimePickerCenterDateTextBitmap();
		
		mEraseBackgroundBitmap = mParameters.getEraseBackgroundBitmap();
		
		mRightDateFieldOfTimePicker = mParameters.getRightDateFieldOfTimePicker();	
		mLeftMeridiemFieldOfTimePicker = mParameters.getLeftMeridiemFieldOfTimePicker();	
		mRightMeridiemFieldOfTimePicker = mParameters.getRightMeridiemFieldOfTimePicker();			
		mLeftHourFieldOfTimePicker = mParameters.getLeftHourFieldOfTimePicker();			
		mRightHourFieldOfTimePicker = mParameters.getRightHourFieldOfTimePicker();	
		
		mMainPageScrollView = mParameters.getMainPageScrollView();		
	}
	
	Looper mMainThreadLooper;
	int mMainThreadPriority;
	Handler mRenderThreadHandler;
	@Override
	protected void onLooperPrepared() {        	
    	mRenderingLock = new ReentrantLock();        	
    	mConditionOfRenderingLock = mRenderingLock.newCondition();
    	mMainThreadPriority = this.getPriority();   
    	mRenderingThreadPriority = mMainThreadPriority + 1;
    	mOpenGLESRender = new OpenGLESRender(mRenderingLock, mConditionOfRenderingLock);        	
    	mOpenGLESRender.setDaemon(true);
    	mOpenGLESRender.start();       	
        
		super.onLooperPrepared();
		
		// Class used to run a message loop for a thread
		mMainThreadLooper = getLooper();
		
		mRenderThreadHandler = new Handler(mMainThreadLooper, this);		
	}        
            
    OpenGLESRender mOpenGLESRender;
    ReentrantLock mRenderingLock;        
    Condition mConditionOfRenderingLock;
    int mRenderingThreadPriority;
    public class OpenGLESRender extends Thread {
    	public static final int RUN = 0;
    	public static final int EXIT = 1;
    	
    	boolean mContinue = true;
    	int mStatus;
    	Object InterruptSync;      	
    	
    	ReentrantLock WakeUPToken;        	
    	Condition ConditionOfRenderingLock;
    	int mPriority;
    	
    	public OpenGLESRender (ReentrantLock wakeUPToken, Condition conditionOfRenderingLock) {
    		WakeUPToken = wakeUPToken;        		
    		ConditionOfRenderingLock = conditionOfRenderingLock;     		
    		InterruptSync = new Object();
    		mStatus = RUN;
    		
    		setPriority(mRenderingThreadPriority);
    	}

		@Override
		public void run() {
			super.run();	
			
			initGL();            
            checkCurrent();	            
            onDrawFrame();	            
            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                Log.e(TAG, "cannot swap buffers!");
            }
            checkEglError();	            
            //long timeStamp = 0;	            	            
            WakeUPToken.lock();
            
			while (mContinue) {
				/////////////////////////////////////////////////////////////////////////////////				
    			try {	    							
    				
    				ConditionOfRenderingLock.await();
    				
    				WakeUPToken.lock();
    				
    				if (mStatus == RUN) {
	    				//Log.i("tag", "WAKE");
	    				//checkCurrent();				            
			            onDrawFrame();            
			            
			            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
			                Log.e(TAG, "cannot swap buffers!");
			            }
	    						    				
			            checkEglError();
    				}
    				else {
    					mContinue = false;   					
    					deleteTextures();			
    					deInitGL();    					
    				}
		            
    				// wake up!!!
                } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 	 			
    			
			}
    		/////////////////////////////////////////////////////////////////////////////////	
			
			Log.i("tag", "+OpenGLESRender : escape while loop");
			
			synchronized(InterruptSync) {
				InterruptSync.notifyAll();
				Log.i("tag", "+OpenGLESRender : InterruptSync.notify!!!");
			}
				
			Log.i("tag", "+OpenGLESRender : run goodbye...");
		}  
		
		public void shouldStop() {
			Log.i("tag", "+OpenGLESRender : shouldStop");
			WakeUPToken.lock();
			mStatus = EXIT;			
			ConditionOfRenderingLock.signalAll();
			//WakeUPToken.unlock(); // run�� ���� ����Ǿ InterruptSync�� ���� �����Ͽ� InterruptSync�� ĳġ���� ���ϴ� ��찡 �ִ�	
						
			synchronized(InterruptSync) {
				try {	
					WakeUPToken.unlock(); // run�� ���� ����Ǿ InterruptSync�� ���� �����Ͽ� InterruptSync�� ĳġ���� ���ϴ� ��찡 �ִ�					
					InterruptSync.wait();									
					Log.i("tag", "OpenGLESRender : shouldStop : bye...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}        
    }        
    
    // �� field�� movement status machine�� ��������
    //@SuppressWarnings("unchecked")
    // mRenderThreadHandler = new Handler(mTimePicker.getLooper(), mTimePicker); �� callback ��
	@Override
    public boolean handleMessage(Message msg) { 		
    	TimePickerUserActionMessage gestureMsg = (TimePickerUserActionMessage)msg.obj;
    	int field = gestureMsg.mFieldID;    	
    	
	        switch (field) {
	        case DATE_FIELD_TEXTURE:            	
	        	DateFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar);           	
	        	break;
	        case MERIDIEM_FIELD_TEXTURE:
	        	MeridiemFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar);   
	        	break;
	        case HOUR_FIELD_TEXTURE:
	        	HourFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar);
	        	break;
	        case MINUTE_FIELD_TEXTURE:
	        	MinuteFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar);
	        	break;
	        default:
	        	return false;
	        }
    	
    	        	
    	return true;
    }
	
	public void resetCalendar(Calendar time) { 
		mCalendar.clear();
 		mCalendar.setTimeInMillis(time.getTimeInMillis());
 		
 		mDateFieldCalendar.clear();
 		mHourFieldCalendar.clear();
 		mMinuteFieldCalendar.clear();
 		mDateFieldCalendar = (Calendar)mCalendar.clone();        
    	mHourFieldCalendar = (Calendar)mCalendar.clone();
    	mMinuteFieldCalendar = (Calendar)mCalendar.clone(); 		 
 	}
 	
 	public void resetField() {
 		mCurrentDateFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentDateFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		int am_pm = mCalendar.get(java.util.Calendar.AM_PM);
		if (am_pm == java.util.Calendar.AM) {
			mCurrentMeridiemFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
            mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree;
		}
		else if (am_pm == java.util.Calendar.PM) {
			mCurrentMeridiemFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
            mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree;
		}
		else {
			mCurrentMeridiemFieldUppermostItemBottomSideDegree = 0;
            mCurrentMeridiemFieldLowermostItemTopSideDegree = 0;
		}
		            
        mCurrentHourFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
        mCurrentHourFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
        
        mCurrentMinuteFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
        mCurrentMinuteFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;     
        
        Calendar DateCalendar = (Calendar)mDateFieldCalendar.clone();        
        Calendar HourCalendar = (Calendar)mHourFieldCalendar.clone();
        Calendar MinuteCalendar = (Calendar)mMinuteFieldCalendar.clone();
        
        mDateFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfDateFields(mCurrentDateFieldLowermostItemTopSideDegree, DateCalendar);        
        mMeridiemFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMeridiemFields(mCurrentMeridiemFieldLowermostItemTopSideDegree, HourCalendar);
        mHourFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfHourFields(mCurrentHourFieldLowermostItemTopSideDegree, HourCalendar);        
        mMinuteFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMinuteFields(mCurrentMinuteFieldLowermostItemTopSideDegree, MinuteCalendar);
        
        mDateHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfDateFieldsHighLight();		
        mMeridiemHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
        mHourHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfHourFieldsHighLight();
        mMinuteHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMinuteFieldsHighLight();        
 	}	
		
	public void setFieldStatus(int field, int status) {
		switch (field) {
        case DATE_FIELD_TEXTURE:            	
        	mDateFieldStatus = status;      	
        	break;
        case MERIDIEM_FIELD_TEXTURE:
        	 
        	break;
        case HOUR_FIELD_TEXTURE:
        	mHourFieldStatus = status;
        	break;
        case MINUTE_FIELD_TEXTURE:
        	mMinuteFieldStatus = status;
        	break;            
        }
	}
	
	public int getFieldStatus(int field) {
		int status = 0;
		switch (field) {
        case DATE_FIELD_TEXTURE:            	
        	status = mDateFieldStatus;      	
        	break;
        case MERIDIEM_FIELD_TEXTURE:        	 
        	break;
        case HOUR_FIELD_TEXTURE:
        	status = mHourFieldStatus;
        	break;
        case MINUTE_FIELD_TEXTURE:
        	status = mMinuteFieldStatus;
        	break;        
        }
		return status;
	}
	
	public ArrayList<ArrayList<HighLightFieldObjectVertices>> buildVerticesTexturePosDataOfFieldsHighLight(int field) {		
    	ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightFieldRowObjectsVerticesList = null;
		
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		HighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfDateFieldsHighLight();
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		
    		break;
    	case HOUR_FIELD_TEXTURE:
    		HighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfHourFieldsHighLight();
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		HighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMinuteFieldsHighLight();
    		break;    	
    	}
		
		return HighLightFieldRowObjectsVerticesList;
	}
    
    @SuppressWarnings("unchecked")
	public void computeNormalScrolling(int field, float scalar, boolean interpolation) {
    	// ���⼭ �ش� Ķ������ Ŭ���Ͽ� �Ʒ� �� �Լ��� �����ϴ� ���� ���???
    	// : ���� ����̴�!!!
    	//Calendar calendar = cloneFiledCalendar(field);
    	ArrayList<ArrayList<FieldObjectVertices>> FieldRowObjectsVerticesList = updateFieldVerticesTexturesDatas(field, scalar, interpolation);
		ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfFieldsHighLight(field);
		
		mRenderingLock.lock();  
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		mDateFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>)FieldRowObjectsVerticesList.clone();
    		mDateHighLightFieldRowObjectsVerticesList = 
    				(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightFieldRowObjectsVerticesList.clone();
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		break;
    	case HOUR_FIELD_TEXTURE:
    		mHourFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>)FieldRowObjectsVerticesList.clone();
    		mHourHighLightFieldRowObjectsVerticesList = 
    				(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightFieldRowObjectsVerticesList.clone();
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		mMinuteFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>)FieldRowObjectsVerticesList.clone();
    		mMinuteHighLightFieldRowObjectsVerticesList = 
    				(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightFieldRowObjectsVerticesList.clone();
    		break;    	
    	default:
    		break;
    	}		
		mConditionOfRenderingLock.signalAll();      			
		mRenderingLock.unlock();
		Thread.yield();
    }
    
    FieldFlingComputeThread mDateFieldFlingComputeThread;
	FieldFlingComputeThread mHourFieldFlingComputeThread;
	FieldFlingComputeThread mMinuteFieldFlingComputeThread;	
	public void LaunchFlingComputeThread(int field, float scalar) {
		float velocityY = (scalar * 2);
		
			switch (field) {
	        case DATE_FIELD_TEXTURE:        	
	        	setFlingFactor(m_dateFieldFlingmovementVector, velocityY);         	     		
	        	mDateFieldFlingComputeThread = new FieldFlingComputeThread(field);
	        	mDateFieldFlingComputeThread.setDaemon(true);
	        	mDateFieldFlingComputeThread.start();
	        	break;
	        case MERIDIEM_FIELD_TEXTURE:
	        	 
	        	break;
	        case HOUR_FIELD_TEXTURE:
	        	setFlingFactor(m_hourFieldFlingmovementVector, velocityY);         	        		
	        	mHourFieldFlingComputeThread = new FieldFlingComputeThread(field);
	        	mHourFieldFlingComputeThread.setDaemon(true);
	        	mHourFieldFlingComputeThread.start();
	        	break;
	        case MINUTE_FIELD_TEXTURE:
	        	setFlingFactor(m_minuteFieldFlingmovementVector, velocityY);        	        		
	        	mMinuteFieldFlingComputeThread = new FieldFlingComputeThread(field);
	        	mMinuteFieldFlingComputeThread.setDaemon(true);
	        	mMinuteFieldFlingComputeThread.start();
	        	break;  
	        default:
	        	return;
	        }
		
	}
	
	FieldAutoScrollingComputeThread mDateFieldAutoScrollingThread; 
	FieldAutoScrollingComputeThread mHourFieldAutoScrollingThread; 
	FieldAutoScrollingComputeThread mMinuteFieldAutoScrollingThread;	
	public void LaunchFieldAutoScrollingComputeThread(int field) {		
		
			switch (field) {
	        case DATE_FIELD_TEXTURE:        	    		
	        	mDateFieldAutoScrollingThread = new FieldAutoScrollingComputeThread(field);
	        	mDateFieldAutoScrollingThread.setDaemon(true);
	        	mDateFieldAutoScrollingThread.start();
	        	break;
	        case MERIDIEM_FIELD_TEXTURE:
	        	 
	        	break;
	        case HOUR_FIELD_TEXTURE:        	       		
	        	mHourFieldAutoScrollingThread = new FieldAutoScrollingComputeThread(field);
	        	mHourFieldAutoScrollingThread.setDaemon(true);
	        	mHourFieldAutoScrollingThread.start();
	        	break;
	        case MINUTE_FIELD_TEXTURE:        	      		
	        	mMinuteFieldAutoScrollingThread = new FieldAutoScrollingComputeThread(field);
	        	mMinuteFieldAutoScrollingThread.setDaemon(true);
	        	mMinuteFieldAutoScrollingThread.start();
	        	break;  
	        default:
	        	return;
		    }
		
	}
		
	public class FieldFlingComputeThread extends Thread {		
		int mField;
    	boolean mContinue = true;
    	Object mInterruptSync;   	
    	
    	int mStatus;
    	float mScalar;
    	long mGotTime;
    	float mDeltaTime;
    	long mStartTime;
    	float mPrvScalar;
    	float mAccumulatedAnimationTime;
		double mTargetScalarDegree;
		float mTargetScalar;		
		
		public FieldFlingComputeThread(int field) {
			mField = field;							
			setPriority(mRenderingThreadPriority - 1);
			mInterruptSync = new Object();
			
			mScalar = 0;
			mPrvScalar = 0;
			mGotTime = 0;
			mDeltaTime = 0;
			mStartTime = 0;
			mTargetScalarDegree = 0;
			mTargetScalar = 0;
			mStatus = FLINGCOMPUTETHREAD_FLING_STATUS;
		}
		
		@Override
		public void run() {					
			super.run();
			
			while(mContinue) {				
				if (mStatus == FLINGCOMPUTETHREAD_FLING_STATUS) {
					if (computeYFlingScalar(mField)) {						
						mScalar = getYFlingScalar(mField);					
						// 0���� �� �����°ɱ�???
						if (mScalar != 0) {							
							computeNormalScrolling(mField, mScalar, false);							
						}
					}
					else {							
						mStatus = FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS;
						mAccumulatedAnimationTime = 0;
	        			
						float velocity = getYFlingVelocity(mField);
	        			if (velocity > 0) { //finger movement : up -> down = (scalar > 0)
	        				mTargetScalarDegree = getCurrentFieldLowermostItemTopSideDegree(mField);
	        				float degree = (float)mTargetScalarDegree;
	        				mTargetScalar = mTimePickerDateFieldCircleDiameterPixels * (degree/360);
	        			}
	        			else if (velocity < 0) { //finger movement : down -> up = (scalar < 0)
	        				mTargetScalarDegree = FIELD_ITEM_DEGREE - getCurrentFieldLowermostItemTopSideDegree(mField);
	        				float degree = (float)mTargetScalarDegree;
	        				mTargetScalar = mTimePickerDateFieldCircleDiameterPixels * (degree/360);
	        				mTargetScalar = -mTargetScalar;
	        			}
	        			else {
	        				messageShoot(mField); 
	        				mContinue = false;
	        				//Log.i("tag", "FFCT : What???!!!");
	        			}
						mStartTime = System.nanoTime();								
					}	
				}
				else if (mStatus == FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS) {
					boolean lastWork = false;
					mGotTime = System.nanoTime();
					mDeltaTime = (mGotTime - mStartTime) / 1000000.0f;
		        	mStartTime = mGotTime;
		        	
					mAccumulatedAnimationTime = mAccumulatedAnimationTime + mDeltaTime;
					if (mAccumulatedAnimationTime > AUTOSCROLLING_ANIMATION_TIME_MS) {
						mAccumulatedAnimationTime = AUTOSCROLLING_ANIMATION_TIME_MS;		
						lastWork = true;
					}
					
					double target_scalar_time = (double)mAccumulatedAnimationTime;
					double target_total_time = (double)AUTOSCROLLING_ANIMATION_TIME_MS;		
					float target_apply_time = Linear.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
					
					float temp = mTargetScalar * (float)target_apply_time;
					mScalar = temp - mPrvScalar;
					mPrvScalar = temp;
					
					computeNormalScrolling(mField, mScalar, lastWork);					
					
					if (lastWork) {								
						messageShoot(mField);						
						mContinue = false;						
						setFieldStatus(mField, IDLE);
						//Log.i("tag", "FFCT : compute completion");													
					}						
				} //end? else		
				else if (mStatus == FLINGCOMPUTETHREAD_INTERRUPTED_STATUS) {										
					mContinue = false;
					//Log.i("tag", "FFCT : FLINGCOMPUTETHREAD_INTERRUPTED_STATUS");							
				}
			}					
			
			if (mStatus == FLINGCOMPUTETHREAD_INTERRUPTED_STATUS) {
				synchronized(mInterruptSync) {
					mInterruptSync.notify();
				}
			}							
			
			//Log.i("tag", "FFCT : exit");				
		} 
		
		public void shouldStop() {				
			Log.i("tag", "FFCT : shouldStop : change status");	
			if (getFieldStatus(mField) == IDLE) {
				//Log.i("tag", "FFCT : shouldStop : already finish!!!");
				return;
			}
			
			mStatus = FLINGCOMPUTETHREAD_INTERRUPTED_STATUS;							
			
			synchronized(mInterruptSync) {
				try {						
					mInterruptSync.wait();
					interruptedComputeYFlingScalar(mField);						
					//Log.i("tag", "FFCT : shouldStop : bye...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}        	
    }
	
	private static final int AUTOSCROLLING_AUTOSELFPOSITION_STATUS = 1;
    private static final int AUTOSCROLLING_INTERRUPTED_STATUS = 2;
    private static final float AUTOSCROLLING_ANIMATION_TIME_MS = 300;
	public class FieldAutoScrollingComputeThread extends Thread {
		int mField;
    	double mTargetScalarDegree;
    	float mTargetScalar;
    	float mAccumulatedAnimationTime;
    	long mStartTime;
    	boolean mContinue;
    	float mScalar; 
    	float mPrvScalar;
    	Object mInterruptSync;
    	int mStatus;
    	
    	public FieldAutoScrollingComputeThread(int field) {
    		mField = field;	
    		setPriority(mRenderingThreadPriority - 1);
    		mInterruptSync = new Object();
			mContinue = true;
			mStatus = AUTOSCROLLING_AUTOSELFPOSITION_STATUS;
			mAccumulatedAnimationTime = 0;
			// scalar < 0 : finger move upward 
			// scalarABS�� length of arc�� ȯ������
    		// scalarABS = 2��r * (��/360)
			double CurrentFieldLowermostItemTopSideDegree = getCurrentFieldLowermostItemTopSideDegree(mField);
			if (CurrentFieldLowermostItemTopSideDegree > (FIELD_ITEM_DEGREE / 2)) {	        				
				mTargetScalarDegree = CurrentFieldLowermostItemTopSideDegree - FIELD_ITEM_DEGREE;
				mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));
			}
			else {	        				
				mTargetScalarDegree = CurrentFieldLowermostItemTopSideDegree - TIMEPICKER_ITEM_AVAILABLE_MIN_DEGREE;
				mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));
			}
			
			mStartTime = System.nanoTime();
			mScalar = 0;
			mPrvScalar = 0;
    	}
    	
		@Override
		public void run() {			
			long gotTime = 0;
			float deltaTime = 0;
						
			if (mTargetScalar == 0) { // ����� �߻��Ѵ� : 1.idle ���¿��� ��ġ�� ���
				mContinue = false;
				// idle ���·� ��ȯ���� ������ block ���°� �߻��� �� �ִ� : shouldStop����...
				setFieldStatus(mField, IDLE);
				//Log.i("tag", "FASCT : mTargetScalar == 0");					
			}
			
			while (mContinue) {	
				if (mStatus == AUTOSCROLLING_AUTOSELFPOSITION_STATUS) {
					boolean lastWork = false;
					gotTime = System.nanoTime();
					deltaTime = (gotTime - mStartTime) / 1000000.0f;
		        	mStartTime = gotTime;
		        	
					mAccumulatedAnimationTime = mAccumulatedAnimationTime + deltaTime;
					if (mAccumulatedAnimationTime > AUTOSCROLLING_ANIMATION_TIME_MS) {
						mAccumulatedAnimationTime = AUTOSCROLLING_ANIMATION_TIME_MS;	
						lastWork = true;
					}			
					
					double target_scalar_time = (double)mAccumulatedAnimationTime;
					double target_total_time = (double)AUTOSCROLLING_ANIMATION_TIME_MS;		
					float target_apply_time = Linear.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
					
					float temp = mTargetScalar * (float)target_apply_time;
					mScalar = temp - mPrvScalar;
					mPrvScalar = temp;					
					
					
					computeNormalScrolling(mField, mScalar, lastWork);
					
					if (lastWork) {							
						messageShoot(mField);					
						mContinue = false;	
						setFieldStatus(mField, IDLE);
						//Log.i("tag", "FASCT : compute completion");							
					}
				}
				else {
					mContinue = false;
					//Log.i("tag", "FASCT : AUTOSCROLLING_INTERRUPTED_STATUS");							
				}
			}
			
			if (mStatus == AUTOSCROLLING_INTERRUPTED_STATUS) {
				synchronized(mInterruptSync) {
					mInterruptSync.notify();
				}
			}							
			
			//Log.i("tag", "FASCT : exit");				
		}
		
		public void shouldStop() {		
			//Log.i("tag", "FASCT : shouldStop");
			if (getFieldStatus(mField) == IDLE) {
				//Log.i("tag", "FASCT : shouldStop : already finished!!!");
				return;
			}
			
			mStatus = AUTOSCROLLING_INTERRUPTED_STATUS;					
			
			synchronized(mInterruptSync) {
				try {
					mInterruptSync.wait();						
					//Log.i("tag", "FASCT : shouldStop : bye...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}        	
    }        
	    
    int mDateFieldStatus = IDLE;
    public void DateFieldStatusMachine(int gestureID, float scalar) {
    	//Log.i("tag", "gestureID=" + String.valueOf(gestureID));
    	switch(mDateFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {        			
    			mDateFieldStatus = SCROLLING;////////////////////////////////////////////////////////////////////////
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_SCROLLING_MSG");
				computeNormalScrolling(DATE_FIELD_TEXTURE, scalar, false);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling ��.
    													                              // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  							  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_FLING_MSG");
    			mDateFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(DATE_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { // �� ���´� fling ���� ���� down msg �߻��Ǹ� fling ����� �ﰢ �ߴܵǰ�, idle ���·� ��ٷ� ���̵ȴ�
    			                                              // �׸��� up msg�� �߻��Ǹ� ó���Ѵ�
    			
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_ACTION_UP_MSG");		
    			mDateFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(DATE_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDateFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDateFieldStatus = IDLE; // ���ʿ��ϴ� �̹� IDLE ������!!
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDateFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "DFSM : SCROLLING : TIMEPICKER_SCROLLING_MSG");
    			computeNormalScrolling(DATE_FIELD_TEXTURE, scalar, false);       			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { 
    			//Log.i("tag", "DFSM : SCROLLING : TIMEPICKER_ACTION_UP_MSG");
    			mDateFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(DATE_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDateFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDateFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDateFieldStatus = EXIT;
    		}
    		else { // down�� �߻��� �� �ִ°�??? : �߻��� �� ���� -> ���� ��ġ�гο��� finger�� ���� �ʰ� �ֱ� ����...(up msg�� �߻��ߴٴ� ���� autoscrolling ���·� ��ȯ�Ǿ��ٴ� ���� �ǹ�!!!)
    			//Log.i("tag", "DFSM : SCROLLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case FLING: 
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // �̷� ���� �߻����� �ʴ´�???
    			                                     // FLING ���¿��� �ٽ� fling�� �� �õ��ϸ� ACTION_DOWN�� ���� �߻��ϱ� �����̴�
    			                                     // :������� �߻����� �ʾ���
    			//Log.i("tag", "DFSM : FLING : TIMEPICKER_FLING_MSG");
    			// ���� �������� fling ������ �ߴܽ�Ű�� �ٽ� �����Ѵ�
    			// �׷��� ���� fling ���� �������� ���Ḧ Ȯ������ �ʾƵ� �Ǵ°�?
    			// :Ȯ���ؾ� ����
    			mDateFieldFlingComputeThread.shouldStop();
    			mDateFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(DATE_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // fling ������ ��� �ߴܽ�Ű�� ���� ��ġ�� ���,
    			                                                // fling ���¿��� �ٽ� flig�� �� �õ��ϱ� ���� ��ġ�� ���,,,
    			//Log.i("tag", "DFSM : FLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mDateFieldFlingComputeThread.shouldStop();   
    			mDateFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDateFieldFlingComputeThread.shouldStop();
    			mDateFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDateFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mDateFieldFlingComputeThread.shouldStop();
    			mDateFieldStatus = EXIT;
    		}
    		else {
    			//Log.i("tag", "DFSM : FLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case AUTOSCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // �� ��찡 �߻��� �� �ִ°�?
    			                                     // fling�� �߻��Ϸ��� down msg�� ���� �߻��ȴ� 
    												 // :�ƴϴ� ������ �߻��Ѵ�...fling ���·� �����ϱ� ���� �� ���İ��� �ܰ���
    			//Log.i("tag", "DFSM : AUTOSCROLLING : TIMEPICKER_FLING_MSG");
    			// autoscrolling ���¸� �ߴܽ��Ѿ� �Ѵ�
    			mDateFieldAutoScrollingThread.shouldStop();  
    			mDateFieldStatus = FLING; 
    			LaunchFlingComputeThread(DATE_FIELD_TEXTURE, scalar);	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // �߻��Ѵ� : autoscrolling ���� �߿� ��ġ�� �Ǵ� ���,
    			                                                // shouldStop���� ������ blocking�� ���� �� �ִ� : �ϴ� �ذ���!!!
    			//Log.i("tag", "DFSM : AUTOSCROLLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mDateFieldAutoScrollingThread.shouldStop();  
    			mDateFieldStatus = IDLE;
    			messageShoot(DATE_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDateFieldAutoScrollingThread.shouldStop();
    			mDateFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDateFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mDateFieldAutoScrollingThread.shouldStop();
    			mDateFieldStatus = EXIT;
    		}
    		else {
    			Log.i("tag", "DFSM : AUTOSCROLLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDateFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {    			
    			mDateFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDateFieldStatus = EXIT;
    		}
    		break;
    	default:
    		return;
    	}
    }
        
    int mHourFieldStatus = IDLE;
    public void HourFieldStatusMachine(int gestureID, float scalar) {        	
    	switch(mHourFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {        			
    			mHourFieldStatus = SCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			computeNormalScrolling(HOUR_FIELD_TEXTURE, scalar, false);     			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling��.
    													  // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "IDLE:TIMEPICKER_FLING_MSG");
    			mHourFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(HOUR_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {
    			
    			//Log.i("tag", "IDLE:TIMEPICKER_ACTION_UP_MSG");
    			// �� ��쵵 �߻��� �ȴ�...�׳� �����ϸ� �Ǵ� ���ϱ�???
    			// 1.FLING ���� ���� ���۽��� ACTION_DOWN �߻����� ���� fling compute�� �ߴܵ� ���,,,
    			// 2.finger�� ȭ�鿡�� �ƹ��� �����ĵ� ���� �ʰ� �� ���(singleTap/doubleTap/LongPress�� ����),,,
    			// -->AUTOSCROLLING ���·� ���̵ȴ�(���� mCurrentDateFieldLowermostItemTopSideDegree!=20 �� ��츸 )
    			if (mCurrentHourFieldLowermostItemTopSideDegree == FIELD_ITEM_DEGREE) {
    				mHourFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////        				
					messageShoot(HOUR_FIELD_TEXTURE);						
    			}
    			else {
    				mHourFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    				LaunchFieldAutoScrollingComputeThread(HOUR_FIELD_TEXTURE);      				
    			}
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mHourFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mHourFieldStatus = IDLE; // ���ʿ��ϴ� �̹� IDLE ������!!
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mHourFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "SCROLLING:TIMEPICKER_SCROLLING_MSG");
    			computeNormalScrolling(HOUR_FIELD_TEXTURE, scalar, false);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {
    			//Log.i("tag", "SCROLLING:TIMEPICKER_ACTION_UP_MSG");
    			if (mCurrentHourFieldLowermostItemTopSideDegree == FIELD_ITEM_DEGREE) {
    				mHourFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////       
    				messageShoot(HOUR_FIELD_TEXTURE);	
    			}
    			else {
    				mHourFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    				LaunchFieldAutoScrollingComputeThread(HOUR_FIELD_TEXTURE);   				
    			}
    		}    
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mHourFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mHourFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mHourFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case FLING: 
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // �̷� ���� �߻����� �ʴ´�???
    			                                     // FLING ���¿��� fling�� �� �õ��ϸ� ACTION_DOWN�� ���� �߻��ϱ� �����̴�
    			//Log.i("tag", "FLING:TIMEPICKER_FLING_MSG");
    			// ���� �������� fling ������ �ߴܽ�Ű�� �ٽ� �����Ѵ�
    			// �׷��� ���� fling ���� �������� ���Ḧ Ȯ������ �ʾƵ� �Ǵ°�?
    			// :Ȯ���ؾ� ����
    			mHourFieldFlingComputeThread.shouldStop(); 
    			mHourFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(HOUR_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {
    			//Log.i("tag", "FLING:TIMEPICKER_ACTION_DOWN_MSG");
    			mHourFieldFlingComputeThread.shouldStop();
    			mHourFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////        			
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mHourFieldFlingComputeThread.shouldStop();
    			mHourFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mHourFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mHourFieldFlingComputeThread.shouldStop();
    			mHourFieldStatus = EXIT;
    		}
    		break;
    	case AUTOSCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) {
    			//Log.i("tag", "AUTOSCROLLING:TIMEPICKER_FLING_MSG");
    			// autoscrolling ���¸� �ߴܽ��Ѿ� �Ѵ�
    			mHourFieldAutoScrollingThread.shouldStop();   	
    			mHourFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(HOUR_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {
    			//Log.i("tag", "AUTOSCROLLING:TIMEPICKER_ACTION_DOWN_MSG");
    			mHourFieldAutoScrollingThread.shouldStop();  
    			mHourFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    			messageShoot(HOUR_FIELD_TEXTURE);	
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mHourFieldAutoScrollingThread.shouldStop();
    			mHourFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mHourFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mHourFieldAutoScrollingThread.shouldStop();
    			mHourFieldStatus = EXIT;
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mHourFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mHourFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mHourFieldStatus = EXIT;
    		}
    		break;
    	default:
    		return;
    	}
    }
        
    int mMinuteFieldStatus = IDLE;
    public void MinuteFieldStatusMachine(int gestureID, float scalar) {
    	switch(mMinuteFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {        			
    			mMinuteFieldStatus = SCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			computeNormalScrolling(MINUTE_FIELD_TEXTURE, scalar, false);        			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling��.
    													  // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "IDLE:TIMEPICKER_FLING_MSG");
    			mMinuteFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(MINUTE_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {        			
    			
    			// �� ��쵵 �߻��� �ȴ�...�׳� �����ϸ� �Ǵ� ���ϱ�???
    			// 1.FLING ���� ���� ���۽��� ACTION_DOWN �߻����� ���� fling compute�� �ߴܵ� ���,,,
    			// 2.finger�� ȭ�鿡�� �ƹ��� �����ĵ� ���� �ʰ� �� ���(singleTap/doubleTap/LongPress�� ����),,,
    			// -->AUTOSCROLLING ���·� ���̵ȴ�(���� mCurrentDateFieldLowermostItemTopSideDegree!=20 �� ��츸 )
    			//    �Ʒ� �ڵ带 �߰������ν� �ϴ� �ذ�
    			if (mCurrentMinuteFieldLowermostItemTopSideDegree == FIELD_ITEM_DEGREE) {
    				mMinuteFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    				messageShoot(MINUTE_FIELD_TEXTURE);
    				//Log.i("tag", "IDLE:TIMEPICKER_ACTION_UP_MSG->IDLE");
    			}
    			else {
    				//Log.i("tag", "IDLE:TIMEPICKER_ACTION_UP_MSG->AUTOSCROLLING");
    				mMinuteFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    				LaunchFieldAutoScrollingComputeThread(MINUTE_FIELD_TEXTURE);     				
    			}
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMinuteFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMinuteFieldStatus = IDLE; // ���ʿ��ϴ� �̹� IDLE ������!!
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMinuteFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "SCROLLING:TIMEPICKER_SCROLLING_MSG");
    			computeNormalScrolling(MINUTE_FIELD_TEXTURE, scalar, false);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {
    			//Log.i("tag", "SCROLLING:TIMEPICKER_ACTION_UP_MSG");        			
    			if (mCurrentMinuteFieldLowermostItemTopSideDegree == FIELD_ITEM_DEGREE) {
    				mMinuteFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////           				
    				messageShoot(MINUTE_FIELD_TEXTURE);
    			}
    			else {
    				mMinuteFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    				LaunchFieldAutoScrollingComputeThread(MINUTE_FIELD_TEXTURE);     				
    			}
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMinuteFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMinuteFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMinuteFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case FLING: 
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // �̷� ���� �߻����� �ʴ´�???
    			                                     // FLING ���¿��� fling�� �� �õ��ϸ� ACTION_DOWN�� ���� �߻��ϱ� �����̴�        			
    			mMinuteFieldFlingComputeThread.shouldStop(); 
    			mMinuteFieldStatus = FLING;
    			LaunchFlingComputeThread(MINUTE_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {
    			//Log.i("tag", "FLING:TIMEPICKER_ACTION_DOWN_MSG");
    			mMinuteFieldFlingComputeThread.shouldStop();
    			mMinuteFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////        			
    		}  
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMinuteFieldFlingComputeThread.shouldStop();
    			mMinuteFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMinuteFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMinuteFieldFlingComputeThread.shouldStop();
    			mMinuteFieldStatus = EXIT;
    		}
    		
    		break;
    	case AUTOSCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) {
    			//Log.i("tag", "AUTOSCROLLING:TIMEPICKER_FLING_MSG");
    			// autoscrolling ���¸� �ߴܽ��Ѿ� �Ѵ�
    			mMinuteFieldAutoScrollingThread.shouldStop();
    			mMinuteFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(MINUTE_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {
    			//Log.i("tag", "AUTOSCROLLING:TIMEPICKER_ACTION_DOWN_MSG");
    			mMinuteFieldAutoScrollingThread.shouldStop(); 
    			mMinuteFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////        			
    			messageShoot(MINUTE_FIELD_TEXTURE);
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMinuteFieldAutoScrollingThread.shouldStop();
    			mMinuteFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMinuteFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMinuteFieldAutoScrollingThread.shouldStop();
    			mMinuteFieldStatus = EXIT;
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMinuteFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMinuteFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMinuteFieldStatus = EXIT;
    		}
    		break;
    	default:
    		return;
    	}
    }      
    
    
    MeridiemFieldFlingComputeThread mMeridiemFieldFlingComputeThread;        
    public class MeridiemFieldFlingComputeThread extends Thread {
    	boolean mContinue = true;
    	Object mInterruptSync;
    	int mStatus;
    	float mScalar;
    	long mGotTime;
    	float mDeltaTime;
    	long mStartTime;
    	float mPrvScalar;
    	float mAccumulatedAnimationTime;
		double mTargetScalarDegree;
		float mTargetScalar;			
		
		ReentrantLock mLock;
		Condition mCondition;
		
		public MeridiemFieldFlingComputeThread(ReentrantLock wakeUPToken, Condition conditionOfRenderingLock) {				
			mLock = wakeUPToken;
			mCondition = conditionOfRenderingLock; 
			setPriority(mRenderingThreadPriority - 1);
			
			mInterruptSync = new Object();
			mStatus = FLINGCOMPUTETHREAD_FLING_STATUS;
			mScalar = 0;				
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void run() {											
			while(mContinue) {					
				if (mStatus == FLINGCOMPUTETHREAD_FLING_STATUS) {
					if (computeYFlingScalar(m_meridiemFieldFlingmovementVector)) {									
						mScalar = getYFlingScalar(m_meridiemFieldFlingmovementVector);					
						// 0���� �� �����°ɱ�???
						if (mScalar != 0) {			
							if (mCurrentMeridiemFieldLowermostItemTopSideDegree == TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE) {
								stopComputeYFlingScalar(m_meridiemFieldFlingmovementVector);
								mStatus = FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS;
								mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 100;
								mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));								    
								mScalar = 0;
								mPrvScalar = 0;
				    			mStartTime = System.nanoTime();
							}
							else if (mCurrentMeridiemFieldLowermostItemTopSideDegree == FIELD_ITEM_DEGREE) {
								stopComputeYFlingScalar(m_meridiemFieldFlingmovementVector);
								mStatus = FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS;
								mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 80;	
								mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));									    
								mScalar = 0;
								mPrvScalar = 0;
				    			mStartTime = System.nanoTime();
							}
							else {
								ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = 
										updateMeridiemFieldVerticesTexturesDatas(mScalar);
								ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
										buildVerticesTexturePosDataOfMeridiemFieldsHighLight();									
									
								mLock.lock();									
								mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
								mMeridiemHighLightFieldRowObjectsVerticesList = 
										(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
								mCondition.signalAll();						
								mLock.unlock();
								Thread.yield();			
							}								
						}
					}
					else { // fling ������ ����Ǿ���				
						/*
						 * if (scalar < 0) {  // finger move upward 
						 */
						// pm�� top side�� ������ 100�� ���� ū ��� : �Ʒ��� �̵��ؾ� �ϱ� ������ +scalar�� �߻��ؾ� �Ѵ�
						if (mCurrentMeridiemFieldLowermostItemTopSideDegree > 100) { // pm�� center region ��ü�� �����Ѵ�																
							mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 100; 	
						}
						// am�� bottom side�� ������ 80���� ���� ��� : ���� �̵��ؾ� �ϱ� ������ -scalar�� �߻��ؾ� �Ѵ�
						else if (80 > mCurrentMeridiemFieldLowermostItemTopSideDegree) { // am�� center region ��ü�� �����Ѵ�								
							mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 80;									
						}								
						// am�� pm�� �� �Ϻκ��� center region�� ������ ��ġ�� ���
						else if ( (100 > mCurrentMeridiemFieldLowermostItemTopSideDegree) && (mCurrentMeridiemFieldLowermostItemTopSideDegree > 80) ) {
							// center region�� ��ġ�� pm�� �Ϻκ��� am�� �Ϻκк��� �� ū ������ ���� �ϰ� �ִ�
							if (mCurrentMeridiemFieldLowermostItemTopSideDegree >= 90) { // ���� �̵��ؾ� �ϱ� ������ -scalar�� �߻��ؾ� �Ѵ�									
								mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 100; // pm�� �߾ӿ� ��ġ�Ѵ�
							}
							// center region�� ��ġ��  am�� �Ϻκ��� pm�� �Ϻκк��� �� ū ������ ���� �ϰ� �ִ�
							else { // �Ʒ��� �̵��ؾ� �ϱ� ������ +scalar�� �߻��ؾ� �Ѵ�									
								mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 80; // am�� �߾ӿ� ��ġ�Ѵ�
							}
						}
						else {
							mContinue = false;
							mMeridiemFieldStatus = IDLE;	
							
							//int ampm = mHourFieldCalendar.get(Calendar.AM_PM);
							messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);							
						}					
						
						if (mContinue) {
							mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));						    
							mScalar = 0;
							mPrvScalar = 0;
			    			mStartTime = System.nanoTime();
			    			mStatus = FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS;
						}
					}							
				}
				else if (mStatus == FLINGCOMPUTETHREAD_AUTOSELFPOSITION_STATUS) {
					mGotTime = System.nanoTime();
					mDeltaTime = (mGotTime - mStartTime) / 1000000.0f;
		        	mStartTime = mGotTime;
		        	
					mAccumulatedAnimationTime = mAccumulatedAnimationTime + mDeltaTime;
					if (mAccumulatedAnimationTime > AUTOSCROLLING_ANIMATION_TIME_MS) {
						mAccumulatedAnimationTime = AUTOSCROLLING_ANIMATION_TIME_MS;							
					}
					
					double target_scalar_time = (double)mAccumulatedAnimationTime;
					double target_total_time = (double)AUTOSCROLLING_ANIMATION_TIME_MS;		
					float target_apply_time = Expo.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
					
					float temp = mTargetScalar * (float)target_apply_time;
					mScalar = temp - mPrvScalar;
					mPrvScalar = temp;
					
					ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = updateMeridiemFieldVerticesTexturesDatas(mScalar);
					ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
							buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
					
					mLock.lock();						
					mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
					mMeridiemHighLightFieldRowObjectsVerticesList = 
							(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
					mCondition.signalAll();							
					mLock.unlock();
					Thread.yield();	
					
					if (mAccumulatedAnimationTime == AUTOSCROLLING_ANIMATION_TIME_MS) {							
						// ��Ȯ�ϰ� 80 �Ǵ� 100���� �������� �ʴ´�!!!
						// :79.9999 �Ǵ� 99.9999 �� �Ǵ� ����
						// �׸��� 80.000111 �Ǵ� 100.0002344 �� �Ǿ� �Ʒ� �񱳹��� �ش���� �ʴ´�
						double currentMeridiemFieldLowermostItemTopSideFloorDegree = Math.floor(mCurrentMeridiemFieldLowermostItemTopSideDegree);
						//Log.i("tag", "MFFC:currentMeridiemFieldLowermostItemTopSideFloorDegree=" + String.valueOf(currentMeridiemFieldLowermostItemTopSideFloorDegree));					
						int ampm = mHourFieldCalendar.get(Calendar.AM_PM);									
						if (CENTERITEM_BOTTOM_DEGREE >= currentMeridiemFieldLowermostItemTopSideFloorDegree) { // am�� 100 ~ 80�� ���̿� ��Ȯ�� ��ġ��							
							if (ampm != Calendar.AM) {
								mHourFieldCalendar.set(Calendar.AM_PM, Calendar.AM);
								messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
							}
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
						}
						else {		
							if (ampm != Calendar.PM) {
								mHourFieldCalendar.set(Calendar.AM_PM, Calendar.PM);
								messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
							}
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
						}
						
						//messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
						mMeridiemFieldStatus = IDLE;		
						mContinue = false;	
						//Log.i("tag", "MFFC : compute completion");							
					}						
				} //end? else
				else if (mStatus == FLINGCOMPUTETHREAD_INTERRUPTED_STATUS) {
					mContinue = false;						
				}				
			}// end? while		
			
			if (mStatus == FLINGCOMPUTETHREAD_INTERRUPTED_STATUS) {					
				synchronized(mInterruptSync) {
					mInterruptSync.notify();
				}
			}				
			//Log.i("tag", "MFFC:exit:mCurrentMeridiemFieldLowermostItemTopSideDegree=" + String.valueOf(mCurrentMeridiemFieldLowermostItemTopSideDegree));				
		} 
		
		public void shouldStop() {
			//Log.i("tag", "MFFC : shouldStop");
			if (mMeridiemFieldStatus == IDLE) {
				//Log.i("tag", "MFFC : shouldStop : already finished!!!");
				return;
			}
			
			mStatus = FLINGCOMPUTETHREAD_INTERRUPTED_STATUS;						
			
			synchronized(mInterruptSync) {
				try {
					mInterruptSync.wait();						
					//Log.i("tag", "MFFC : shouldStop : bye...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}        	
    }
    
    MeridiemFieldAutoScrolling mMeridiemFieldAutoScrolling;        
    public class MeridiemFieldAutoScrolling extends Thread {        	
    	double mTargetScalarDegree;
    	float mTargetScalar;
    	float mAccumulatedAnimationTime;
    	long mStartTime;
    	boolean mContinue;
    	Object mInterruptSync;
    	int mStatus;
    	ReentrantLock mLock;
    	Condition mCondition;
    	float mPrvScalar;
    	float mScalar;
    	
    	public MeridiemFieldAutoScrolling(ReentrantLock wakeUPToken, Condition conditionOfRenderingLock) {        		
    		mLock = wakeUPToken;
    		mCondition = conditionOfRenderingLock; 
    		setPriority(mRenderingThreadPriority - 1);        		       		
    		mInterruptSync = new Object();
			mContinue = true;
			mAccumulatedAnimationTime = 0;
			/*
			 * if (scalar < 0) {  // finger move upward 
			 */
			// mCurrentMeridiemFieldLowermostItemTopSideDegree�� 100�� ���� ū ��� : �Ʒ��� �̵��ؾ� �ϱ� ������ +scalar�� �߻��ؾ� �Ѵ�				
			if (mCurrentMeridiemFieldLowermostItemTopSideDegree > 100) {					
				//Log.i("tag", "MFAS:mCurrentMeridiemFieldLowermostItemTopSideDegree > 100");
				mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 100;
			}
			// mCurrentMeridiemFieldLowermostItemTopSideDegree�� 80���� ���� ��� : ���� �̵��ؾ� �ϱ� ������ -scalar�� �߻��ؾ� �Ѵ�
			else if (80 > mCurrentMeridiemFieldLowermostItemTopSideDegree) {
				//Log.i("tag", "MFAS:80 > mCurrentMeridiemFieldLowermostItemTopSideDegree");
				mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 80;									
			}				
			else if ( (100 > mCurrentMeridiemFieldLowermostItemTopSideDegree) && (mCurrentMeridiemFieldLowermostItemTopSideDegree > 80) ) {
				if (mCurrentMeridiemFieldLowermostItemTopSideDegree >= 90) { // ���� �̵��ؾ� �ϱ� ������ -scalar�� �߻��ؾ� �Ѵ�
					//Log.i("tag", "MFAS:mCurrentMeridiemFieldLowermostItemTopSideDegree >= 90");
					mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 100; // pm�� �߾ӿ� ��ġ�Ѵ�
				}
				else { // �Ʒ��� �̵��ؾ� �ϱ� ������ +scalar�� �߻��ؾ� �Ѵ�
					//Log.i("tag", "MFAS:mCurrentMeridiemFieldLowermostItemTopSideDegree < 90");
					mTargetScalarDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - 80; // amp�� �߾ӿ� ��ġ�Ѵ�
				}
			}
			else {
				mContinue = false;
				mMeridiemFieldStatus = IDLE;			
				messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
			}				
			
			if (mContinue) {
				mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));			    			
				mStartTime = System.nanoTime();	
				mStatus = AUTOSCROLLING_AUTOSELFPOSITION_STATUS;
				mPrvScalar = 0;
				mScalar = 0;
			}
    	}
    	
		@SuppressWarnings("unchecked")
		@Override
		public void run() {					
			if (!mContinue)
				return;
			
			while (mContinue) {
				if (mStatus == AUTOSCROLLING_AUTOSELFPOSITION_STATUS) {
					long gotTime = System.nanoTime();
					float deltaTime = (gotTime - mStartTime) / 1000000.0f;
		        	mStartTime = gotTime;
		        	
					mAccumulatedAnimationTime = mAccumulatedAnimationTime + deltaTime;
					if (mAccumulatedAnimationTime > AUTOSCROLLING_ANIMATION_TIME_MS) {
						mAccumulatedAnimationTime = AUTOSCROLLING_ANIMATION_TIME_MS;						
					}			
					
					double target_scalar_time = (double)mAccumulatedAnimationTime;
					double target_total_time = (double)AUTOSCROLLING_ANIMATION_TIME_MS;		
					float target_apply_time = Expo.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
					
					float temp = mTargetScalar * target_apply_time;
					mScalar = temp - mPrvScalar;
					mPrvScalar = temp;					
					
					ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = updateMeridiemFieldVerticesTexturesDatas(mScalar);
					ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
							buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
					
					mLock.lock();					
					mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
					mMeridiemHighLightFieldRowObjectsVerticesList = 
							(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
					mCondition.signalAll();							
					mLock.unlock();
					Thread.yield();	
					
					if (mAccumulatedAnimationTime == AUTOSCROLLING_ANIMATION_TIME_MS) {						
						// ��Ȯ�ϰ� 80 �Ǵ� 100���� �������� �ʴ´�!!!
						// :79.9999 �Ǵ� 99.9999 �� �Ǵ� ����
						// �׸��� 80.000111 �Ǵ� 100.0002344 �� �Ǿ� �Ʒ� �񱳹��� �ش���� �ʴ´�
						double currentMeridiemFieldLowermostItemTopSideFloorDegree = Math.floor(mCurrentMeridiemFieldLowermostItemTopSideDegree);													
						int ampm = mHourFieldCalendar.get(Calendar.AM_PM);									
						if (CENTERITEM_BOTTOM_DEGREE >= currentMeridiemFieldLowermostItemTopSideFloorDegree) { // am�� 100 ~ 80�� ���̿� ��Ȯ�� ��ġ��
							if (ampm != Calendar.AM) {
								mHourFieldCalendar.set(Calendar.AM_PM, Calendar.AM);
								messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
							}
							
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
						}
						else {
							if (ampm != Calendar.PM) {
								mHourFieldCalendar.set(Calendar.AM_PM, Calendar.PM);
								messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
							}
							
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
						}
						
						//messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);							
						mContinue = false;	
						mMeridiemFieldStatus = IDLE;
						//Log.i("tag", "MFAST:compute completion");
						
					}
				}
				else if (mStatus == AUTOSCROLLING_INTERRUPTED_STATUS) {
					mContinue = false;
				}
			}	
			
			if (mStatus == AUTOSCROLLING_INTERRUPTED_STATUS) {					
				synchronized(mInterruptSync) {
					mInterruptSync.notify();
				}
			}
			//Log.i("tag", "MFAS:exit:mCurrentMeridiemFieldLowermostItemTopSideDegree=" + String.valueOf(mCurrentMeridiemFieldLowermostItemTopSideDegree));				
		}
		
		public void shouldStop() {
			//Log.i("tag", "MFAST : shouldStop");
			if (mMeridiemFieldStatus == IDLE) {
				//Log.i("tag", "MFAST : shouldStop : already finished!!!");
				return;
			}
			
			mStatus = AUTOSCROLLING_INTERRUPTED_STATUS;					
			
			synchronized(mInterruptSync) {
				try {
					mInterruptSync.wait();						
					//Log.i("tag", "MFAST : shouldStop : bye...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}            	
    }
    
    MeridiemFieldAutoScrollingByHourField mMeridiemFieldAutoScrollingByHourField;        
    public class MeridiemFieldAutoScrollingByHourField extends Thread {        	
    	double mTargetScalarDegree;
    	float mTargetScalar;
    	float mAccumulatedAnimationTime;
    	long mStartTime;
    	boolean mContinue;
    	Object mInterruptSync;
    	ReentrantLock mLock;
    	Condition mCondition;
    	int mStatus;
    	
    	public MeridiemFieldAutoScrollingByHourField(ReentrantLock wakeUPToken, Condition conditionOfRenderingLock) {   
    		mLock = wakeUPToken;
    		mCondition = conditionOfRenderingLock; 
    		setPriority(mRenderingThreadPriority - 1);
    		
    		mInterruptSync = new Object();
			mContinue = true;
			mAccumulatedAnimationTime = 0;
			
			int ampm = mHourFieldCalendar.get(Calendar.AM_PM);
			if (ampm == Calendar.AM) { // center region�� am�� ��ġ�ؾ� �Ѵ�
                mTargetScalarDegree = FIELD_ITEM_DEGREE;				
			}
			else {
				mTargetScalarDegree = -FIELD_ITEM_DEGREE;					
			}
														
			mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));	
			
			mStartTime = System.nanoTime();
			mStatus = AUTOSCROLLING_AUTOSELFPOSITION_STATUS;
    	}
    	
		@SuppressWarnings("unchecked")
		@Override
		public void run() {			
			long gotTime = 0;
			float deltaTime = 0;
			float prvScalar = 0;				
			
			while (mContinue) {
				if (mStatus == AUTOSCROLLING_AUTOSELFPOSITION_STATUS) {
					boolean lastWork = false;
					gotTime = System.nanoTime();
					deltaTime = (gotTime - mStartTime) / 1000000.0f;
		        	mStartTime = gotTime;
		        	
					mAccumulatedAnimationTime = mAccumulatedAnimationTime + deltaTime;
					if (mAccumulatedAnimationTime > 100) {
						mAccumulatedAnimationTime = 100;		
						lastWork = true;
					}			
					
					double target_scalar_time = (double)mAccumulatedAnimationTime;
					double target_total_time = (double)100;		
					float target_apply_time = Expo.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
					
					float temp = mTargetScalar * target_apply_time;
					float scalar = temp - prvScalar;
					prvScalar = temp;
					
					ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = updateMeridiemFieldVerticesTexturesDatas(scalar);
					ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
							buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
					
					mLock.lock();					
					mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
					mMeridiemHighLightFieldRowObjectsVerticesList = 
							(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
					mCondition.signalAll();							
					mLock.unlock();
					Thread.yield();	
				
					if (lastWork) {							
						mMeridiemFieldStatus = IDLE;
						//int AMPM = mMeridiemFieldCalendar.get(Calendar.AM_PM);
						int AMPM = mHourFieldCalendar.get(Calendar.AM_PM);
						if (AMPM == Calendar.AM) { 
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
						}
						else {
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
						}
						
						messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
						
						mContinue = false;	
						//Log.i("tag", "MFASBYH : compute completion");							
					}
				}
				else if (mStatus == AUTOSCROLLING_INTERRUPTED_STATUS) {						
					if (mAccumulatedAnimationTime != 100) {
						mAccumulatedAnimationTime = 100;
						double target_scalar_time = (double)mAccumulatedAnimationTime;
						double target_total_time = (double)100;		
						float target_apply_time = Expo.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
						
						float temp = mTargetScalar * target_apply_time;
						float scalar = temp - prvScalar;
						//prvScalar = temp;
						
						ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = updateMeridiemFieldVerticesTexturesDatas(scalar);
						ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
								buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
						
						mLock.lock();					
						mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
						mMeridiemHighLightFieldRowObjectsVerticesList = 
								(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
						mCondition.signalAll();							
						mLock.unlock();
						
						int AMPM = mHourFieldCalendar.get(Calendar.AM_PM);
						if (AMPM == Calendar.AM) { 
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
						}
						else {
							mCurrentMeridiemFieldLowermostItemTopSideDegree = 
									TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
						}
						
						Thread.yield();							
					}
					
					mContinue = false;
					//Log.i("tag", "MFASBYH : AUTOSCROLLING_INTERRUPTED_STATUS");							
				}
			}
			
			if (mStatus == AUTOSCROLLING_INTERRUPTED_STATUS) {
				synchronized(mInterruptSync) {
					mInterruptSync.notify();
				}
			}
			
		}
		
		public void shouldStop() {
			//Log.i("tag", "MFASBYH : shouldStop");
			if (mMeridiemFieldStatus == IDLE) {
				//Log.i("tag", "MFASBYH : shouldStop : already finished!!!");
				return;
			}
			
			mStatus = AUTOSCROLLING_INTERRUPTED_STATUS;				
			
			synchronized(mInterruptSync) {
				try {
					mInterruptSync.wait();						
					//Log.i("tag", "MFASBYH : shouldStop : bye...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}        		
    }
    
    int mMeridiemFieldStatus = IDLE;
    @SuppressWarnings("unchecked")
	public void MeridiemFieldStatusMachine(int gestureID, float scalar) {        			
    	switch(mMeridiemFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {   
    			//Log.i("tag", "MeridiemFieldStatusMachine : IDLE : TIMEPICKER_SCROLLING_MSG");
    			mMeridiemFieldStatus = SCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = 
        				updateMeridiemFieldVerticesTexturesDatas(scalar);
    			ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
						buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
    			mRenderingLock.lock();
    			mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
    			mMeridiemHighLightFieldRowObjectsVerticesList = 
    					(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
    			mConditionOfRenderingLock.signalAll();							
    			mRenderingLock.unlock();        			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling��.
    													  // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "MeridiemFieldStatusMachine : IDLE : TIMEPICKER_FLING_MSG");
    			mMeridiemFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			float velocityY = scalar;
    			setFlingFactor(m_meridiemFieldFlingmovementVector, velocityY); 
    			mMeridiemFieldStatus = FLING;
        		mMeridiemFieldFlingComputeThread = new MeridiemFieldFlingComputeThread(mRenderingLock, mConditionOfRenderingLock);
        		mMeridiemFieldFlingComputeThread.setDaemon(true);
        		mMeridiemFieldFlingComputeThread.start();	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {        			
    			// �� ��쵵 �߻��� �ȴ�...�׳� �����ϸ� �Ǵ� ���ϱ�???
    			// 1.FLING ���� ���� ���۽��� ACTION_DOWN �߻����� ���� fling compute�� �ߴܵ� ���,,,
    			// 2.finger�� ȭ�鿡�� �ƹ��� �����ĵ� ���� �ʰ� �� ���(singleTap/doubleTap/LongPress�� ����),,,
    			// -->AUTOSCROLLING ���·� ���̵ȴ�(���� mCurrentDateFieldLowermostItemTopSideDegree!=20 �� ��츸 )   				
				//Log.i("tag", "MeridiemFieldStatusMachine : IDLE : TIMEPICKER_ACTION_UP_MSG:B");    				
				mMeridiemFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////    				 				
				mMeridiemFieldAutoScrolling = new MeridiemFieldAutoScrolling(mRenderingLock, mConditionOfRenderingLock);
				mMeridiemFieldAutoScrolling.setDaemon(true);
				mMeridiemFieldAutoScrolling.start();       			
    		} 
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_STATUS_CHANGE_MSG) { //idle �����϶��� �ӽ� ���� ��ȯ�� ����Ѵ�
    			//Log.i("tag", "Meridiem Field Macine : IDLE : TIMEPICKER_STATUS_CHANGE_MSG");
    			mMeridiemFieldStatus = AUTOSCROLLINGBYHOURFIELD/*(int)scalar*/;     			
        		mMeridiemFieldAutoScrollingByHourField = new MeridiemFieldAutoScrollingByHourField(mRenderingLock, mConditionOfRenderingLock);
        		mMeridiemFieldAutoScrollingByHourField.setDaemon(true);
        		mMeridiemFieldAutoScrollingByHourField.start();
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "MeridiemFieldStatusMachine : SCROLLING : TIMEPICKER_SCROLLING_MSG");
    			ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = 
        				updateMeridiemFieldVerticesTexturesDatas(scalar);
    			ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightMeridiemFieldRowObjectsVerticesList = 
						buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
    			mRenderingLock.lock();
    			mMeridiemFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>) MeridiemFieldRowObjectsVerticesList.clone();
    			mMeridiemHighLightFieldRowObjectsVerticesList = 
    					(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightMeridiemFieldRowObjectsVerticesList.clone();					
    			mConditionOfRenderingLock.signalAll();							
    			mRenderingLock.unlock();      
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {
    			//Log.i("tag", "MeridiemFieldStatusMachine : SCROLLING : TIMEPICKER_ACTION_UP_MSG");  
    			
				mMeridiemFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			mMeridiemFieldAutoScrolling = new MeridiemFieldAutoScrolling(mRenderingLock, mConditionOfRenderingLock);
    			mMeridiemFieldAutoScrolling.setDaemon(true);
    			mMeridiemFieldAutoScrolling.start();   			
    		}        		
    		break;
    	case FLING: 
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // �̷� ���� �߻����� �ʴ´�???
    			                                     // FLING ���¿��� fling�� �� �õ��ϸ� ACTION_DOWN�� ���� �߻��ϱ� �����̴�
    			//Log.i("tag", "MeridiemFieldStatusMachine : FLING : TIMEPICKER_FLING_MSG");
    			// ���� �������� fling ������ �ߴܽ�Ű�� �ٽ� �����Ѵ�
    			// �׷��� ���� fling ���� �������� ���Ḧ Ȯ������ �ʾƵ� �Ǵ°�?
    			// :Ȯ���ؾ� ����
    			mMeridiemFieldFlingComputeThread.shouldStop(); 
    			mMeridiemFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    			float velocityY = scalar;
    			setFlingFactor(m_meridiemFieldFlingmovementVector, velocityY); 
    			mMeridiemFieldStatus = FLING;
        		mMeridiemFieldFlingComputeThread = new MeridiemFieldFlingComputeThread(mRenderingLock, mConditionOfRenderingLock);
        		mMeridiemFieldFlingComputeThread.setDaemon(true);
        		mMeridiemFieldFlingComputeThread.start();	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {
    			//Log.i("tag", "MeridiemFieldStatusMachine : FLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mMeridiemFieldFlingComputeThread.shouldStop();
    			mMeridiemFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////    			
    		}        
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMeridiemFieldFlingComputeThread.shouldStop();
    			mMeridiemFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMeridiemFieldFlingComputeThread.shouldStop();
    			mMeridiemFieldStatus = EXIT;
    		}
    		break;
    	case AUTOSCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) {
    			//Log.i("tag", "MeridiemFieldStatusMachine : AUTOSCROLLING : TIMEPICKER_FLING_MSG");
    			// autoscrolling ���¸� �ߴܽ��Ѿ� �Ѵ�
    			mMeridiemFieldAutoScrolling.shouldStop();   			
    			float velocityY = scalar;
    			setFlingFactor(m_meridiemFieldFlingmovementVector, velocityY); 
    			mMeridiemFieldStatus = FLING;
        		mMeridiemFieldFlingComputeThread = new MeridiemFieldFlingComputeThread(mRenderingLock, mConditionOfRenderingLock);
        		mMeridiemFieldFlingComputeThread.setDaemon(true);
        		mMeridiemFieldFlingComputeThread.start();	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {
    			//Log.i("tag", "MeridiemFieldStatusMachine : AUTOSCROLLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mMeridiemFieldAutoScrolling.shouldStop();  
    			mMeridiemFieldStatus = IDLE;
    			// ��Ȯ�ϰ� 80 �Ǵ� 100���� �������� �ʴ´�!!!
				// :79.9999 �Ǵ� 99.9999 �� �Ǵ� ����
				// �׸��� 80.000111 �Ǵ� 100.0002344 �� �Ǿ� �Ʒ� �񱳹��� �ش���� �ʴ´�
				double currentMeridiemFieldLowermostItemTopSideFloorDegree = Math.floor(mCurrentMeridiemFieldLowermostItemTopSideDegree);
				//Log.i("tag", "MFFC:currentMeridiemFieldLowermostItemTopSideFloorDegree=" + String.valueOf(currentMeridiemFieldLowermostItemTopSideFloorDegree));					
													
				if (CENTERITEM_BOTTOM_DEGREE >= currentMeridiemFieldLowermostItemTopSideFloorDegree) { // am�� 100 ~ 80�� ���̿� ��Ȯ�� ��ġ��
					//mMeridiemFieldCalendar.set(Calendar.AM_PM, Calendar.AM);
					mHourFieldCalendar.set(Calendar.AM_PM, Calendar.AM);
					mCurrentMeridiemFieldLowermostItemTopSideDegree = 
							TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 5);
				}
				else {
					//mMeridiemFieldCalendar.set(Calendar.AM_PM, Calendar.PM);
					mHourFieldCalendar.set(Calendar.AM_PM, Calendar.PM);
					mCurrentMeridiemFieldLowermostItemTopSideDegree = 
							TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - (FIELD_ITEM_DEGREE * 4);
				}
				
				messageShoot(MERIDIEM_FIELD_TEXTURE, 0, 0, 0, mHourFieldCalendar.get(Calendar.AM_PM), 0, 0);
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMeridiemFieldAutoScrolling.shouldStop();
    			mMeridiemFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMeridiemFieldAutoScrolling.shouldStop();
    			mMeridiemFieldStatus = EXIT;
    		}
    		break;
    	case AUTOSCROLLINGBYHOURFIELD:
    		// ���� ���¿��� autoscrolling ó���� �Ϸ�� ������ �����ĵ� ��θ� �����Ѵ�
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_STATUS_CHANGE_MSG) { //idle �����϶��� �ӽ� ���� ��ȯ�� ����Ѵ�
    			//Log.i("tag", "Meridiem Field Macine : AUTOSCROLLINGBYHOURFIELD : TIMEPICKER_STATUS_CHANGE_MSG");
    			mMeridiemFieldAutoScrollingByHourField.shouldStop(); 
    			mMeridiemFieldStatus = AUTOSCROLLINGBYHOURFIELD;        			
        		mMeridiemFieldAutoScrollingByHourField = new MeridiemFieldAutoScrollingByHourField(mRenderingLock, mConditionOfRenderingLock);
        		mMeridiemFieldAutoScrollingByHourField.setDaemon(true);
        		mMeridiemFieldAutoScrollingByHourField.start();
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMeridiemFieldAutoScrollingByHourField.shouldStop();
    			mMeridiemFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMeridiemFieldAutoScrollingByHourField.shouldStop();
    			mMeridiemFieldStatus = EXIT;
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMeridiemFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMeridiemFieldStatus = EXIT;
    		}
    		break;
    	
    	default:
    		return;
    	}
    }
        
    public void messageShoot(int updateField, int Year, int Month, int Date, int AMPM, int Hour, int Minute) {        			
		Message msg = Message.obtain();			
		msg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, Year, Month, Date, AMPM, Hour, Minute);			
		mTimeMsgHandler.sendMessage(msg);		
    }        
    
    public void messageShoot(int updateField) { 
    	int Year = 0;
    	int Month = 0;
    	int Date = 0;
    	int AMPM = 0;
    	int Hour = 0;
    	int Minute = 0;    	
    	Message startTimeMsg = null;    	
    	
		switch (updateField) {
		case DATE_FIELD_TEXTURE:		
			Year = mDateFieldCalendar.get(Calendar.YEAR);
			Month = mDateFieldCalendar.get(Calendar.MONTH);
			Date = mDateFieldCalendar.get(Calendar.DATE);
			startTimeMsg = Message.obtain();	
			startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, Year, Month, Date, AMPM, Hour, Minute);
			mTimeMsgHandler.sendMessage(startTimeMsg);		
			break;
		case MERIDIEM_FIELD_TEXTURE:			
			break;
		case HOUR_FIELD_TEXTURE:
			Hour = mHourFieldCalendar.get(Calendar.HOUR_OF_DAY);
			startTimeMsg = Message.obtain();	
			startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, Year, Month, Date, AMPM, Hour, Minute);
			mTimeMsgHandler.sendMessage(startTimeMsg);					
			break;
		case MINUTE_FIELD_TEXTURE:		
			Minute = mMinuteFieldCalendar.get(Calendar.MINUTE);
			startTimeMsg = Message.obtain();	
			startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, Year, Month, Date, AMPM, Hour, Minute);
			mTimeMsgHandler.sendMessage(startTimeMsg);	
			break;	
		
		default:
			return;
		}		
    }    
    
    private void setCameraZPosition(float angleOfView) {
		m_fovy = angleOfView;
		double halfDegreeOfFovy = (double)(m_fovy / 2);
		double tanDVaule = Math.tan(Math.toRadians(halfDegreeOfFovy));
		m_tanValueOfHalfRadianOfFovy = (float)tanDVaule;
		m_camera_eye_Z = ( (1 / m_tanValueOfHalfRadianOfFovy) * m_halfRatioOfViewingVPHeight ) - OBJECT_TARGET_Z_POSITION; 
		//mCircle_center_z = OBJECT_TARGET_Z_POSITION - 1.5f;
		//mCircle_center_z = OBJECT_TARGET_Z_POSITION - 1.0f;
		//mCircle_center_z = OBJECT_TARGET_Z_POSITION + mNear - 1.0f;
		mCircle_center_z = OBJECT_TARGET_Z_POSITION + 0.25f - 1.0f;
		mCircle_HighLight_center_z = mCircle_center_z +  HIGHLIGHT_BACKWARD_OFFSET;
		//mCircle_center_z = 0;
	}
    
    private void setVeiwEyePosition() {
		m_camera_eye_X = 0.0f;
		m_camera_eye_Y = 0.0f;
		m_camera_lookAt_X = 0.0f;
		m_camera_lookAt_Y = 0.0f;
		m_camera_lookAt_Z = 0.0f;
		m_RunnginStickersVieweyePosition = new EyePosition();
		m_RunnginStickersVieweyePosition.setEyeXYZ(m_camera_eye_X, m_camera_eye_Y, m_camera_eye_Z);
		m_RunnginStickersVieweyePosition.setLookAtXYZ(m_camera_lookAt_X, m_camera_lookAt_Y, m_camera_lookAt_Z);
		m_RunnginStickersVieweyePosition.setUpXYZ(m_upX, m_upY, m_upZ);
	}
    
    
    private void setViewMatrix(EyePosition eyePosition) {
		Matrix.setIdentityM(mViewMatrix, 0);
		
		Matrix.setLookAtM(mViewMatrix, 
						  0,
						  eyePosition.eyeX, 
						  eyePosition.eyeY, 
						  eyePosition.eyeZ, 
						  eyePosition.lookAtX, 
						  eyePosition.lookAtY, 
						  eyePosition.lookAtZ, 
						  eyePosition.upX, 
						  eyePosition.upY, 
						  eyePosition.upZ);		
	}
	
    private void setPerspectiveProjectionMatrix(float fovy, float aspect, 
			  								   float zNear, float zFar) {

		Matrix.setIdentityM(mProjectionMatrix, 0);    		
		Matrix.perspectiveM(mProjectionMatrix, 0, fovy, aspect, zNear, zFar);		
	}
    
	private static final int VECTOR_COMPONENTS_PER_POSITION = 3;
    private static final int VECTOR_COMPONENTS_PER_TEXCOORDINATE = 2;        
    
	public class FieldObjectVertices {
		int mTextureHandle;      			
		FloatBuffer mFbPositions;    		
		int mPointNumbers; 
		boolean mIsChangeAlpha;
		float mAlphaValue;
		
		public FieldObjectVertices (int textureHandle, float fbPoints[],
                                    float leftmost, float rightmost, 
                                    float widthRatio) {
			mTextureHandle = textureHandle;	
							
			int totalVerticesNumbers = ROW_MESH * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_NUMBERS_PER_RECTANGLE_FOR_DRAW_ARRAYS * COLUMN_MESH);			
			mFbPositions = ByteBuffer.allocateDirect(totalVerticesNumbers * mBytesPerFloat)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();				
			
			float x_dif = widthRatio / ROW_MESH;												
			mPointNumbers = buildVerticesTexturePData(fbPoints, leftmost, x_dif);
			
			mIsChangeAlpha = false;
    		mAlphaValue = 1;
		}   
		
		public FieldObjectVertices (int textureHandle, float fbPoints[],
                float leftmost, float rightmost, 
                float widthRatio, boolean isChangeAlpha, float alphaValue) {
			mTextureHandle = textureHandle;	
			mIsChangeAlpha = isChangeAlpha;
    		mAlphaValue = alphaValue;
    		
    		// int total
			//int totalVerticesNumbers = ROW_MESH * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * COLUMN_MESH);	
    		int totalVerticesNumbers = ROW_MESH * (VERTICES_NUMBERS_PER_RECTANGLE_FOR_DRAW_ARRAYS * COLUMN_MESH);
			mFbPositions = ByteBuffer.allocateDirect(totalVerticesNumbers * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();				
			
			float x_dif = widthRatio / ROW_MESH;												
			mPointNumbers = buildVerticesTexturePData(fbPoints, leftmost, x_dif);
		}   
		
		private int buildVerticesTexturePData(float fbPoints[], float leftmost, float x_dif) {
			
			int pointNumbers = 0;
			float x = 0;        		
			int index = 0;
			
			// �츮�� �ϳ��� �ؽ�Ʈ�� triangle mesh�� �׸� ���̴�
			// �� ������[ROW_MESH*COLUMN_MESH]�� �簢���� �׸���
			// :triangle�� �� 12���� �ȴ�
			for (int i=0; i<ROW_MESH; i++) {  // x�� ���� ȸ��
				// �簢���� �ΰ��� �ﰢ������ �׸��� ������ 6���� vertices�� �ʿ��ϴ�	
				x = leftmost + (x_dif * i);    			
				/*
				v[0]:(x,y[i+1])  v[2]:(x+x_dif,y[i+1])
				 _______________________ 
				|                       |
				|                       |
				|                       |
				|                       |
				|_______________________|
				v[1](x,y[i])     v[3]:(x+x_dif,y[i])    	        
				*/
				for (int j=0; j<COLUMN_MESH; j++) {  // y�� ���� ȸ��
					// left triangle polygon
					// v[0]
					fbPoints[index] = x; index = index + 3;
					// v[1]
					fbPoints[index] = x; index = index + 3;
					// v[2]
					fbPoints[index] = x + x_dif; index = index + 3;
					// right triangle polygon
					// v[1]
					fbPoints[index] = x; index = index + 3;
					// v[3]
					fbPoints[index] = x + x_dif; index = index + 3;
					// v[2]
					fbPoints[index] = x + x_dif; index = index + 3;					
					
					pointNumbers = pointNumbers + 6;
				}
			}
			
			mFbPositions.put(fbPoints).position(0);
			return pointNumbers;				
		}    	   	
	}
	
	final byte[] FBIndicesData =
	{
			0,1,2,1,3,2						                         
	};
	
	public class HighLightFieldObjectVertices {
		int mTextureHandle;      			
		FloatBuffer mFbPositions; 
		FloatBuffer mFbFieldTextureCoordinates;
		ByteBuffer mFbIndicess;	
		
		public HighLightFieldObjectVertices (int textureHandle, 
                                             float leftmost, float rightmost, 
                                             float top, float bottom,
                                             float textureTop, float textureBottom) {
			mTextureHandle = textureHandle;	
							
			int totalVerticesNumbers = (VECTOR_COMPONENTS_PER_POSITION * VERTICES_PER_POLYGON_FOR_DRAW_ELEMENTS);
			float[] fbPoints = new float[totalVerticesNumbers];
				
			mFbPositions = ByteBuffer.allocateDirect(totalVerticesNumbers * mBytesPerFloat)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			
			int totalTexCoordinateDataNumbers = (VECTOR_COMPONENTS_PER_TEXCOORDINATE * VERTICES_NUMBERS_PER_RECTANGLE_FOR_DRAW_ARRAYS);				
			mFbFieldTextureCoordinates = ByteBuffer.allocateDirect(totalTexCoordinateDataNumbers * mBytesPerFloat)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();     
			
			mFbIndicess = ByteBuffer.allocateDirect(FBIndicesData.length);	
			mFbIndicess.order(ByteOrder.nativeOrder());
			mFbIndicess.put(FBIndicesData).position(0);				
													
			buildVerticesData(fbPoints, leftmost, rightmost, top, bottom);
			buildTextureDataFields(textureTop, textureBottom);
		}    		
		
		private void buildVerticesData(float fbPoints[], float leftmost, float rightmost,
				                      float top, float bottom) {
			/*
			v[0]:(leftmost,top)    v[2]:(rightmost,top)
			 _______________________ 
			|                       |
			|                       |
			|                       |
			|                       |
			|_______________________|
			v[1](leftmost,bottom)  v[3]:(rightmost,bottom)    	        
			*/
			// x,                   y,                 z
			fbPoints[0] = leftmost; fbPoints[1] = top; fbPoints[2] = (float) TIMEPICKER_FIELD_CIRCLE_RADIUS;       //vertex[0]	
			fbPoints[3] = leftmost; fbPoints[4] = bottom; fbPoints[5] = (float) TIMEPICKER_FIELD_CIRCLE_RADIUS;    //vertex[1]		
			fbPoints[6] = rightmost; fbPoints[7] = top; fbPoints[8] = (float) TIMEPICKER_FIELD_CIRCLE_RADIUS;      //vertex[2] 	
			fbPoints[9] = rightmost; fbPoints[10] = bottom; fbPoints[11] = (float) TIMEPICKER_FIELD_CIRCLE_RADIUS; //vertex[3]	
			
			mFbPositions.put(fbPoints).position(0);							
		}    		
		
    	private void buildTextureDataFields(float textureTop, float textureBottom) {
    		/*
			v[0]:(0.0f, 0.0f)      v[2]:(1.0f, 0.0f)
			 _______________________ 
			|                       |
			|                       |
			|                       |
			|                       |
			|_______________________|
			v[1](0.0f, 1.0f)       v[3]:(1.0f, 1.0f)    
			
			v[0]:(0.0f,Top)         v[2]:(1.0f,Top)
			 _______________________ 
			|                       |
			|                       |
			|                       |
			|                       |
			|_______________________|
			v[1](0.0f,Bottom)        v[3]:(1.0f,Bottom)  
			*/
    		        		
    		float s_coordinate = 0;
    		float t_coordinate = 0;
    		
			// v0 t_c
			s_coordinate = 0.0f;
			t_coordinate = textureTop;
			mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
			// v1 t_c
			s_coordinate = 0.0f;
			t_coordinate = textureBottom;
			mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
			// v2 t_c
			s_coordinate = 1.0f;
			t_coordinate = textureTop;
			mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
			
			// v3 t_c
			s_coordinate = 1.0f;
			t_coordinate = textureBottom;
			mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate);        			
    	}
	}
	    	
	FloatBuffer mFbFieldTextureCoordinates;
	private void buildTexturePosDataFields() {
		float s_coordinate = 0;
		float t_coordinate = 0;
		float texture_polygon_width = 1.0f / (float)ROW_MESH;
		float texture_polygon_height = 1.0f / (float)COLUMN_MESH;
		int totalTexCoordinateDataNumbers = ROW_MESH * (VECTOR_COMPONENTS_PER_TEXCOORDINATE * VERTICES_NUMBERS_PER_RECTANGLE_FOR_DRAW_ARRAYS * COLUMN_MESH);
		mFbFieldTextureCoordinates = ByteBuffer.allocateDirect(totalTexCoordinateDataNumbers * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		
		for (int i=0; i<ROW_MESH; i++) {  // x�� ���� ȸ��
			for (int j=0; j<COLUMN_MESH; j++) {  // y�� ���� ȸ��          				
				// v0 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width);
				t_coordinate = 1.0f - (j * texture_polygon_height) - texture_polygon_height;
				mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
				// v1 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width);
				t_coordinate = 1.0f - (j * texture_polygon_height);
				mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
				// v2 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width) + texture_polygon_width;
				t_coordinate = 1.0f - (j * texture_polygon_height) - texture_polygon_height;
				mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
				// v1 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width);
				t_coordinate = 1.0f - (j * texture_polygon_height);
				mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
				// v3 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width) + texture_polygon_width;
				t_coordinate = 1.0f - (j * texture_polygon_height);
				mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate); 
				// v2 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width) + texture_polygon_width;
				t_coordinate = 1.0f - (j * texture_polygon_height) - texture_polygon_height;
				mFbFieldTextureCoordinates.put(s_coordinate);mFbFieldTextureCoordinates.put(t_coordinate);       				
			}
		}
	}
		
	private float[] buildVerticesOfYAndZAxis(float lowermost, double step) {
		double y = 0;
		double y2 = 0;
		
		double[] YAxis = new double[COLUMN_MESH+1];
		double[] ZAxis = new double[COLUMN_MESH+1];
		double r2 = Math.pow(TIMEPICKER_FIELD_CIRCLE_RADIUS, 2);
		int end = COLUMN_MESH + 1;
		
		for (int i=0; i<end; i++) {
			y = lowermost + (i * step);
			y2 = Math.pow(y, 2);      			
			ZAxis[i] = Math.sqrt(r2 - y2);
			YAxis[i] = y;
		}
		
		float[] Points = new float[ROW_MESH * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_NUMBERS_PER_RECTANGLE_FOR_DRAW_ARRAYS * COLUMN_MESH)];
		
		int index = 0;	    		
		for (int j=0; j<ROW_MESH; j++) {  // x�� ���� ȸ��
			for (int k=0; k<COLUMN_MESH; k++) {  // y�� ���� ȸ��        				
				// left triangle polygon
				// v[0]
				++index;/*Points[index++] = 0;*/ Points[index++] = (float)YAxis[k+1]; Points[index++] = (float)ZAxis[k+1];
				// v[1]
				++index;/*Points[index++] = 0;*/ Points[index++] = (float)YAxis[k]; Points[index++] = (float)ZAxis[k];
				// v[2]
				++index;/*Points[index++] = 0;*/ Points[index++] = (float)YAxis[k+1]; Points[index++] = (float)ZAxis[k+1];
				
				// right triangle polygon
				// v[1]
				++index;/*Points[index++] = 0;*/ Points[index++] = (float)YAxis[k]; Points[index++] = (float)ZAxis[k];
				// v[3]
				++index;/*Points[index++] = 0;*/ Points[index++] = (float)YAxis[k]; Points[index++] = (float)ZAxis[k];
				// v[2]
				++index;/*Points[index++] = 0;*/ Points[index++] = (float)YAxis[k+1]; Points[index++] = (float)ZAxis[k+1];        						
			}
		}	    		
		
		return Points;
	}
	
	static final int LEFT_INCREMENT_DIRECTION = 1;
	static final int RIGHT_INCREMENT_DIRECTION = 2;
	static final int ONE_NUMERAL_UNIT = 1;
	static final int TENS_NUMERAL_UNIT = 2;	
	private float makeNumeralTextVertexData(int direction, int numeralUnit, 
			                               float datumPoint, int targetNumeral, float[] Points, 
			                               ArrayList<FieldObjectVertices> ObjList, int ObjListIndex) {
		float leftmost = 0;
		float rightmost = 0;		
		int listIndex = ObjListIndex;
		
		if(direction == LEFT_INCREMENT_DIRECTION) {
			rightmost = datumPoint;			
			if (numeralUnit == TENS_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				String cutTens = numeralString.substring(0, 1);
				String cutOnes = numeralString.substring(1, 2);					
				int numberOfTensOfNumeral = Integer.parseInt(cutTens);
				int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);
				
				int onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];				
				leftmost = rightmost - mTimePickerNumeralWidthRatio;
				FieldObjectVertices onesObject = new FieldObjectVertices(onesTextureHandle, Points, 
																		 leftmost, rightmost, 
																		 mTimePickerNumeralWidthRatio);					
				ObjList.add(listIndex++, onesObject);

				int tensTextureHandle = mNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				rightmost = leftmost;
				leftmost = rightmost - mTimePickerNumeralWidthRatio;								
				FieldObjectVertices tensObject = new FieldObjectVertices(tensTextureHandle, Points, 
																		 leftmost, rightmost, 
																		 mTimePickerNumeralWidthRatio);
				ObjList.add(listIndex, tensObject);
			}
			else if (numeralUnit == ONE_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				int numberOfOnesOfNumeral = Integer.parseInt(numeralString);
				
				int onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];				
				leftmost = rightmost - mTimePickerNumeralWidthRatio;
				FieldObjectVertices onesObject = new FieldObjectVertices(onesTextureHandle, Points, 
																			         leftmost, rightmost, 
																			         mTimePickerNumeralWidthRatio);					
				ObjList.add(listIndex, onesObject);
			}
			
			datumPoint = leftmost;
		}
		else if (direction == RIGHT_INCREMENT_DIRECTION) {			
			if (numeralUnit == TENS_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				String cutTens = numeralString.substring(0, 1);
				String cutOnes = numeralString.substring(1, 2);					
				int numberOfTensOfNumeral = Integer.parseInt(cutTens);
				int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);
				
				int tensTextureHandle = mNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				leftmost = datumPoint;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;
				FieldObjectVertices tensObject = new FieldObjectVertices(tensTextureHandle, Points, 
																			 leftmost, rightmost, 
																			 mTimePickerNumeralWidthRatio);					
				ObjList.add(listIndex++, tensObject);

				int onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				FieldObjectVertices onesObject = new FieldObjectVertices(onesTextureHandle, Points, 
																			 leftmost, rightmost, 
																			 mTimePickerNumeralWidthRatio);
				ObjList.add(listIndex, onesObject);
			}
			else if (numeralUnit == ONE_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				int numberOfOnesOfNumeral = Integer.parseInt(numeralString);
				
				int onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				leftmost = datumPoint + mTimePickerNumeralWidthRatio;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				FieldObjectVertices onesObject = new FieldObjectVertices(onesTextureHandle, Points, 
																			 leftmost, rightmost, 
																			 mTimePickerNumeralWidthRatio);					
				ObjList.add(listIndex, onesObject);
			}
			
			datumPoint = rightmost;
		}
		
		return datumPoint;
	}
	
	private float makeHighLightNumeralTextVertexData(int direction, int numeralUnit, 
            										 float datumPoint, float topmost, float bottommost, float textureTop, float textureBottom,
            										 int targetNumeral, 
            										 ArrayList<HighLightFieldObjectVertices> ObjList, int ObjListIndex) {
		
		float leftmost = 0;
		float rightmost = 0;		
		int listIndex = ObjListIndex;
		
		if(direction == LEFT_INCREMENT_DIRECTION) {
			rightmost = datumPoint;			
			if (numeralUnit == TENS_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				String cutTens = numeralString.substring(0, 1);
				String cutOnes = numeralString.substring(1, 2);					
				int numberOfTensOfNumeral = Integer.parseInt(cutTens);
				int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);				
				
				int onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];				
				leftmost = rightmost - mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices onesObject = new HighLightFieldObjectVertices(onesTextureHandle,
						                                                                   leftmost, rightmost, 																			          
						                                                                   topmost, bottommost,
						                                                                   textureTop, textureBottom);			
				ObjList.add(listIndex++, onesObject);
				
				int tensTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				rightmost = leftmost;
				leftmost = rightmost - mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices tensObject = new HighLightFieldObjectVertices(tensTextureHandle,
																						   leftmost, rightmost, 																			          
																						   topmost, bottommost,
																						   textureTop, textureBottom);					
				ObjList.add(listIndex, tensObject);
			}
			else if (numeralUnit == ONE_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				int numberOfOnesOfNumeral = Integer.parseInt(numeralString);
				
				int onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];				
				leftmost = rightmost - mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices onesObject = new HighLightFieldObjectVertices(onesTextureHandle,
						                                                                   leftmost, rightmost, 																			          
						                                                                   topmost, bottommost,
						                                                                   textureTop, textureBottom);
				ObjList.add(listIndex, onesObject);
			}
			
			datumPoint = leftmost;
		}
		else if (direction == RIGHT_INCREMENT_DIRECTION) {			
			if (numeralUnit == TENS_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				String cutTens = numeralString.substring(0, 1);
				String cutOnes = numeralString.substring(1, 2);					
				int numberOfTensOfNumeral = Integer.parseInt(cutTens);
				int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);
				
				int tensTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				leftmost = datumPoint;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices tensObject = new HighLightFieldObjectVertices(tensTextureHandle, 
	                    																	   leftmost, rightmost, 																			          
	                    																	   topmost, bottommost,
	                    																	   textureTop, textureBottom);						
				ObjList.add(listIndex++, tensObject);
				
				int onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices hourOnesObject = new HighLightFieldObjectVertices(onesTextureHandle, 
																							   leftmost, rightmost, 																			          
																							   topmost, bottommost,
																							   textureTop, textureBottom);						
				ObjList.add(listIndex, hourOnesObject);
			}
			else if (numeralUnit == ONE_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				int numberOfOnesOfNumeral = Integer.parseInt(numeralString);
				
				int onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				leftmost = datumPoint + mTimePickerNumeralWidthRatio;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices onesObject = new HighLightFieldObjectVertices(onesTextureHandle, 
						   																   leftmost, rightmost, 																			          
						   																   topmost, bottommost,
						   																   textureTop, textureBottom);						
				ObjList.add(listIndex, onesObject);
			}
			
			datumPoint = rightmost;
		}

		return datumPoint;
	}
	
	private float makeHighLightNumeralTextVertexData(boolean highLight, int direction, int numeralUnit, 
			 										 float datumPoint, float topmost, float bottommost, float textureTop, float textureBottom,
			 										 int targetNumeral, 
			                                         ArrayList<HighLightFieldObjectVertices> ObjList, int ObjListIndex) {

		float leftmost = 0;
		float rightmost = 0;		
		int listIndex = ObjListIndex;
		
		if(direction == LEFT_INCREMENT_DIRECTION) {
			rightmost = datumPoint;			
			if (numeralUnit == TENS_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				String cutTens = numeralString.substring(0, 1);
				String cutOnes = numeralString.substring(1, 2);					
				int numberOfTensOfNumeral = Integer.parseInt(cutTens);
				int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);				
				int onesTextureHandle = 0;
				int tensTextureHandle = 0;
				
				if(highLight)
					onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];	
				else
					onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				
				leftmost = rightmost - mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices onesObject = new HighLightFieldObjectVertices(onesTextureHandle,
				                                                  leftmost, rightmost, 																			          
				                                                  topmost, bottommost,
				                                                  textureTop, textureBottom);			
				ObjList.add(listIndex++, onesObject);
				
				if(highLight)
					tensTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				else
					tensTextureHandle = mNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				
				rightmost = leftmost;
				leftmost = rightmost - mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices tensObject = new HighLightFieldObjectVertices(tensTextureHandle,
																   leftmost, rightmost, 																			          
																   topmost, bottommost,
																   textureTop, textureBottom);					
				ObjList.add(listIndex, tensObject);
				
			}
			else if (numeralUnit == ONE_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				int numberOfOnesOfNumeral = Integer.parseInt(numeralString);
				int onesTextureHandle = 0;
				
				if(highLight)
					onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				else
					onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				
				leftmost = rightmost - mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices onesObject = new HighLightFieldObjectVertices(onesTextureHandle,
				                                                  leftmost, rightmost, 																			          
				                                                  topmost, bottommost,
				                                                  textureTop, textureBottom);
				ObjList.add(listIndex, onesObject);
			}
		
			datumPoint = leftmost;
		}
		else if (direction == RIGHT_INCREMENT_DIRECTION) {			
			if (numeralUnit == TENS_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				String cutTens = numeralString.substring(0, 1);
				String cutOnes = numeralString.substring(1, 2);					
				int numberOfTensOfNumeral = Integer.parseInt(cutTens);
				int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);
				int tensTextureHandle = 0;
				int onesTextureHandle= 0;
				
				if(highLight)
					tensTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				else
					tensTextureHandle = mNumeralTextTextureDataHandles[numberOfTensOfNumeral];
				
				leftmost = datumPoint;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices tensObject = new HighLightFieldObjectVertices(tensTextureHandle, 
																	   leftmost, rightmost, 																			          
																	   topmost, bottommost,
																	   textureTop, textureBottom);						
				ObjList.add(listIndex++, tensObject);
				
				if(highLight)
					onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				else
					onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];					
					
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices hourOnesObject = new HighLightFieldObjectVertices(onesTextureHandle, 
																	   leftmost, rightmost, 																			          
																	   topmost, bottommost,
																	   textureTop, textureBottom);						
				ObjList.add(listIndex, hourOnesObject);
			}
			else if (numeralUnit == ONE_NUMERAL_UNIT) {
				String numeralString = String.valueOf(targetNumeral);
				int numberOfOnesOfNumeral = Integer.parseInt(numeralString);
				int onesTextureHandle = 0;
				
				if(highLight)
					onesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				else
					onesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfNumeral];
				
				leftmost = datumPoint + mTimePickerNumeralWidthRatio;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices onesObject = new HighLightFieldObjectVertices(onesTextureHandle, 
																	   leftmost, rightmost, 																			          
																	   topmost, bottommost,
																	   textureTop, textureBottom);						
				ObjList.add(listIndex, onesObject);
			}
		
			datumPoint = rightmost;
		}
		
		return datumPoint;
	}
	
	private double[] makeYAxisField(double currentFieldLowermostItemTopSideDegree) {
		int mustBeDrawItemCount = 0;
		double y = 0;
		double[] yAxisDateField = new double[TIMEPICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT+2];
		
		y = Math.cos(Math.toRadians(TIMEPICKER_ITEM_AVAILABLE_MIN_DEGREE)) * TIMEPICKER_FIELD_CIRCLE_RADIUS;
		yAxisDateField[mustBeDrawItemCount++] = -y;	
		
		double angdeg = currentFieldLowermostItemTopSideDegree; ////////////////////			
		boolean Looping = true;
		while(Looping) {			
			y = Math.cos(Math.toRadians(angdeg)) * TIMEPICKER_FIELD_CIRCLE_RADIUS;
			yAxisDateField[mustBeDrawItemCount++] = -y;
			angdeg = angdeg + FIELD_ITEM_DEGREE;
			if (angdeg >= TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE) {
				// �׻� 180���� topmost item�� top ��ǥ ���� degree�̴� 				
				y = Math.cos(Math.toRadians(TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE)) * TIMEPICKER_FIELD_CIRCLE_RADIUS;
				yAxisDateField[mustBeDrawItemCount] = -y;				
				Looping = false;
			}
		}		
		
		return yAxisDateField;
	}
	
	private ArrayList<FieldObjectVertices> makeTodayVerticesTexturePosDataOfDateFields(double yAxisDateField, double nextYAxisDateField) {
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float lowermost = 0;		
		float[] Points = null;
		
		ArrayList<FieldObjectVertices> ObjList = new ArrayList<FieldObjectVertices>();
		int ObjListIndex = 0;
		
		topmost = (float) nextYAxisDateField;
		lowermost = (float) yAxisDateField;				
		double abs = (double)Math.abs(topmost - lowermost);
		double step = abs / COLUMN_MESH;    			   		
		
		Points = buildVerticesOfYAndZAxis(lowermost, step);	
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		// '��'�� drawing
		int secondTextTextureHandle = mTodayTextTextureDataHandles[1];
		rightmost = m_dateFieldRightMostVPRattio - (mTimePickerTextWidthRatio + mGPABetweenTextRatio);
		leftmost = rightmost - mTimePickerTextWidthRatio;
		FieldObjectVertices secondTextObject = new FieldObjectVertices(secondTextTextureHandle, Points, 
																	   leftmost, rightmost, 
																	   mTimePickerTextWidthRatio);					
		ObjList.add(ObjListIndex++, secondTextObject);
		
		// '��'�� drawing				
		int firstTextTextureHandle = mTodayTextTextureDataHandles[0]; 
		rightmost = leftmost;
		leftmost = rightmost - mTimePickerTextWidthRatio;
		FieldObjectVertices firstTextObject = new FieldObjectVertices(firstTextTextureHandle, Points, 
																	  leftmost, rightmost, 
																	  mTimePickerTextWidthRatio);	
		ObjList.add(ObjListIndex, firstTextObject);
		
		return ObjList;
	}
	
	private ArrayList<HighLightFieldObjectVertices> makeHighLightTodayVerticesTexturePosDataOfDateFields(float topmost, float bottommost, 
																										 float textureTop, float textureBottom) {
		float leftmost = 0;
		float rightmost = 0;				
		ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
		int ObjListIndex = 0;			
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		// '��'�� drawing
		int secondTextTextureHandle = mCenterTodayTextTextureDataHandles[1];
		rightmost = m_dateFieldRightMostVPRattio - (mTimePickerTextWidthRatio + mGPABetweenTextRatio);
		leftmost = rightmost - mTimePickerTextWidthRatio;
		HighLightFieldObjectVertices secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle,
																						 leftmost, rightmost, 																			          
																						 topmost, bottommost,
																						 textureTop, textureBottom);					
		ObjList.add(ObjListIndex++, secondTextObject);
		// '��'�� drawing
		int firstTextTextureHandle = mCenterTodayTextTextureDataHandles[0];
		rightmost = leftmost;
		leftmost = rightmost - mTimePickerTextWidthRatio;
		HighLightFieldObjectVertices firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle,
																						leftmost, rightmost, 																			          
																						topmost, bottommost,
																						textureTop, textureBottom);					
		ObjList.add(ObjListIndex, firstTextObject);		
		
		return ObjList;
	}
	
	private ArrayList<FieldObjectVertices> makeNoneTodayVerticesTexturePosDataOfDateFields(double yAxisDateField, double nextYAxisDateField, Calendar CalCalendar) {
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float lowermost = 0;		
		float[] Points = null;		
		ArrayList<FieldObjectVertices> ObjList = new ArrayList<FieldObjectVertices>();
		int ObjListIndex = 0;
		
		topmost = (float) nextYAxisDateField;
		lowermost = (float) yAxisDateField;				
		double abs = (double)Math.abs(topmost - lowermost);
		double step = abs / COLUMN_MESH;    			   		
		
		Points = buildVerticesOfYAndZAxis(lowermost, step);		
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		int dayOfWeek = CalCalendar.get(Calendar.DAY_OF_WEEK);
		int dayOfWeekIndex = dayOfWeek - 1;
		int dayOfWeekTextureHandle = mDayOfWeekTextureDataHandles[dayOfWeekIndex];
		rightmost = m_dateFieldRightMostVPRattio;
		leftmost = rightmost - mTimePickerTextWidthRatio;		
		
		FieldObjectVertices dayOfWeekObject = new FieldObjectVertices(dayOfWeekTextureHandle, Points, 
																	  leftmost, rightmost, 																			          
																	  mTimePickerTextWidthRatio);	    		
		ObjList.add(ObjListIndex++, dayOfWeekObject);				
		//////////////////////////////////////////////////////////////////////////////////////////////////
		int dateTextTextureHandle = mDateTextTextTextureDataHandle;
		rightmost = leftmost - mGPABetweenTextRatio;
		leftmost = rightmost - mTimePickerTextWidthRatio;
		FieldObjectVertices dateTextObject = new FieldObjectVertices(dateTextTextureHandle, Points,
																	     leftmost, rightmost, 
																	     mTimePickerTextWidthRatio);				
		ObjList.add(ObjListIndex++, dateTextObject);
		//////////////////////////////////////////////////////////////////////////////////////////////////
		int date = CalCalendar.get(Calendar.DATE);			
		if (date > 9) {
			leftmost = makeNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
					                             leftmost, date, Points, 
                                                 ObjList, ObjListIndex++);
			ObjListIndex++;				
		}
		else {
			leftmost =  makeNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
	                                              leftmost, date, Points, 
                                                  ObjList, ObjListIndex++);				
		}
		
		//////////////////////////////////////////////////////////////////////////////////////////////////				
		int monthTextTextureHandle = mMonthTextTextTextureDataHandle;
		rightmost = leftmost - mGPABetweenTextRatio;
		leftmost = rightmost - mTimePickerTextWidthRatio;							
		FieldObjectVertices monthTextObject = new FieldObjectVertices(monthTextTextureHandle, Points, 
																	     leftmost, rightmost, 
																	     mTimePickerTextWidthRatio);				
		ObjList.add(ObjListIndex++, monthTextObject);
		//////////////////////////////////////////////////////////////////////////////////////////////////			
		int monthNumbers = TimerPickerTimeText.transformMonthToRealNumber(CalCalendar.get(Calendar.MONTH));				
		if (monthNumbers > 9) {
			makeNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
	                                  leftmost, monthNumbers, Points, 
                                      ObjList, ObjListIndex);				
		}
		else {
			makeNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
                                      leftmost, monthNumbers, Points, 
                                      ObjList, ObjListIndex);				
		}			
		
		return ObjList;
	}
	
	private ArrayList<HighLightFieldObjectVertices> makeHighLightNoneTodayVerticesTexturePosDataOfDateFields(float topmost, float bottommost, 
			                                                                                                 float textureTop, float textureBottom, 
			                                                                                                 Calendar CalCalendar) {
		float leftmost = 0;
		float rightmost = 0;		
		ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
		int ObjListIndex = 0;
		
		//////////////////////////////////////////////////////////////////////////////////////////////////
		int dayOfWeek = CalCalendar.get(Calendar.DAY_OF_WEEK);
		int dayOfWeekIndex = dayOfWeek - 1;
		int dayOfWeekTextureHandle = mCenterDayOfWeekTextureDataHandles[dayOfWeekIndex];
		rightmost = m_dateFieldRightMostVPRattio;
		leftmost = rightmost - mTimePickerTextWidthRatio;			
		
		HighLightFieldObjectVertices dayOfWeekObject = new HighLightFieldObjectVertices(dayOfWeekTextureHandle, 
																	                    leftmost, rightmost, 																			          
																	                    topmost, bottommost,
																	                    textureTop, textureBottom);	    		
		ObjList.add(ObjListIndex++, dayOfWeekObject);				
		//////////////////////////////////////////////////////////////////////////////////////////////////				
		int dateTextTextureHandle = mCenterDateTextTextTextureDataHandle;
		rightmost = leftmost - mGPABetweenTextRatio;
		leftmost = rightmost - mTimePickerTextWidthRatio;
		HighLightFieldObjectVertices dateTextObject = new HighLightFieldObjectVertices(dateTextTextureHandle, 
				 																	   leftmost, rightmost, 																			          
				 																	   topmost, bottommost,
				 																	   textureTop, textureBottom);
		ObjList.add(ObjListIndex++, dateTextObject);
		//////////////////////////////////////////////////////////////////////////////////////////////////
		int date = CalCalendar.get(Calendar.DATE);
		
		if (date > 9) {
			leftmost = makeHighLightNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
														  leftmost, topmost, bottommost, textureTop, textureBottom,
														  date, ObjList, ObjListIndex++);
			ObjListIndex++;					
		}
		else {
			leftmost = makeHighLightNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
					                                      leftmost, topmost, bottommost, textureTop, textureBottom,
					                                      date, ObjList, ObjListIndex++);				
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////				
		int monthTextTextureHandle = mCenterMonthTextTextTextureDataHandle;
		rightmost = leftmost - mGPABetweenTextRatio;
		leftmost = rightmost - mTimePickerTextWidthRatio;							
		HighLightFieldObjectVertices monthTextObject = new HighLightFieldObjectVertices(monthTextTextureHandle,
																						leftmost, rightmost, 																			          
																						topmost, bottommost,
																						textureTop, textureBottom);			
		ObjList.add(ObjListIndex++, monthTextObject);
		//////////////////////////////////////////////////////////////////////////////////////////////////			
		int monthNumbers = TimerPickerTimeText.transformMonthToRealNumber(CalCalendar.get(Calendar.MONTH));				
		if (monthNumbers > 9) {
			leftmost = makeHighLightNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
					                                      leftmost, topmost, bottommost, textureTop, textureBottom,
					                                      monthNumbers, ObjList, ObjListIndex);					
		}
		else {
			leftmost = makeHighLightNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
                                                          leftmost, topmost, bottommost, textureTop, textureBottom,
                                                          monthNumbers, ObjList, ObjListIndex);					
		}
		
		return ObjList;
	}
	
	private boolean IsDateToday(Calendar drawingCal) {
		boolean today = false;
		int todayYear = 0;
		int todayMonth = 0;
		int todayDate = 0;
		int drawYear = 0;
		int drawMonth = 0;
		int drawDate = 0;
		
		Calendar todayCal = (Calendar)Calendar.getInstance(Locale.KOREAN);
		todayYear = todayCal.get(Calendar.YEAR);
		todayMonth = todayCal.get(Calendar.MONTH);
		todayDate = todayCal.get(Calendar.DATE);
		
		drawYear = drawingCal.get(Calendar.YEAR);
		drawMonth = drawingCal.get(Calendar.MONTH);
		drawDate = drawingCal.get(Calendar.DATE);
		
		if ( (todayYear == drawYear) && (todayMonth == drawMonth) && (todayDate == drawDate) ) {
			today = true;
		}	
			
		return today;
	}
	
	private ArrayList<ArrayList<FieldObjectVertices>> buildVerticesTexturePosDataOfDateFields(double CurrentDateFieldLowermostItemTopSideDegree, Calendar CalCalendar) {		
		int mustBeDrawItemCount = 0;		
		ArrayList<ArrayList<FieldObjectVertices>> DateFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		double[] yAxisDateField = makeYAxisField(CurrentDateFieldLowermostItemTopSideDegree);
		mustBeDrawItemCount = yAxisDateField.length - 1;
		
		CalCalendar.add(Calendar.DATE, 4);
		
		for (int i=0; i<mustBeDrawItemCount; i++) {
			if ( IsDateToday(CalCalendar) ) {
				DateFieldRowObjectsVerticesList.add(i, makeTodayVerticesTexturePosDataOfDateFields(yAxisDateField[i], yAxisDateField[i+1]));
			}
			else 
				DateFieldRowObjectsVerticesList.add(i, makeNoneTodayVerticesTexturePosDataOfDateFields(yAxisDateField[i], yAxisDateField[i+1], CalCalendar));
			
			CalCalendar.add(Calendar.DATE, -1);
		}  
		
		return DateFieldRowObjectsVerticesList;
	}
	
	@SuppressLint("SuspiciousIndentation")
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildVerticesTexturePosDataOfDateFieldsHighLight() {
		ArrayList<ArrayList<HighLightFieldObjectVertices>> DateFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
		Calendar CalCalendar = (Calendar)mDateFieldCalendar.clone();   		
		
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;		
		
		double targetTopSideDegree = mCurrentDateFieldLowermostItemTopSideDegree + CENTERITEM_BOTTOM_DEGREE;
		
		if (targetTopSideDegree == CENTERITEM_TOP_DEGREE) { // highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�        	
			
			topmost = mCenterItemRegionTopRatio;
			bottommost = mCenterItemRegionBottomRatio;
			textureTop = 0.0f;
			textureBottom = 1.0f;
			
			if (IsDateToday(CalCalendar)) {
				DateFieldHighLightObjectsVerticesList.add(0, makeHighLightTodayVerticesTexturePosDataOfDateFields(topmost, bottommost, textureTop, textureBottom));
			}
			else
				DateFieldHighLightObjectsVerticesList.add(0, makeHighLightNoneTodayVerticesTexturePosDataOfDateFields(topmost, bottommost, textureTop, textureBottom, CalCalendar));
			
			
		}
		else { // highlight�� �� ���� �������� ��ġ�Ѵ� : highlight ������ ������ �� �ִ� �������� ������ �ִ� ������ 2�̴�    				   			
			for (int i=0; i<2; i++) {
				float targetHeightRatio = 0;
				
				if (i==0) { // ���� ������						
					lowerItemTopmost = -(float)(Math.cos(Math.toRadians(targetTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
					lowerItemBottommost = mCenterItemRegionBottomRatio;					
					
					if (lowerItemTopmost >= 0) {
						targetHeightRatio = mCenterItemHalfHeightRatio + lowerItemTopmost;
					}
					else {
						// topmost ���� 0���� ���� ���
						targetHeightRatio = lowerItemTopmost - lowerItemBottommost;///////////////////////////
					}
					/*
					v[0]:(0.0f, 0.0f)      v[2]:(1.0f, 0.0f)
					 _______________________ 
					|                       |
					|                       |
					|                       |
					|                       |
					|_______________________|
					v[1](0.0f, 1.0f)       v[3]:(1.0f, 1.0f)    
					
					v[0]:(0.0f,Top)         v[2]:(1.0f,Top)
					 _______________________ 
					|                       |
					|                       |
					|                       |
					|                       |
					|_______________________|
					v[1](0.0f,Bottom)        v[3]:(1.0f,Bottom)  
					*/
					// ���� �������� �ؽ����� �� �κк��� drawing�Ǿ�� �Ѵ�
					textureTop = 0.0f;
					// �ؽ��ĳ��� ������ ����ؾ� �Ѵ�						
					textureBottom = 0.0f + (targetHeightRatio/mCenterItemHeightRatio);
					
					topmost = lowerItemTopmost;
					bottommost = lowerItemBottommost;
				}
				else { // ���� ������
					upperItemTopmost = mCenterItemRegionTopRatio;
					upperItemBottommost = -(float)(Math.cos(Math.toRadians(targetTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
					
					if (upperItemBottommost >= 0) {
						targetHeightRatio = upperItemTopmost - upperItemBottommost;///////////////////////////////
					}
					else {
						targetHeightRatio = mCenterItemHalfHeightRatio + Math.abs(upperItemBottommost);
					}
					textureTop = 1.0f - (targetHeightRatio/mCenterItemHeightRatio);
					// ���� �������� �ؽ����� �Ʒ� �κк��� drawing�Ǿ�� �Ѵ�
					textureBottom = 1.0f;
					
					topmost = upperItemTopmost;
					bottommost = upperItemBottommost;
				}				
				
				if (IsDateToday(CalCalendar)) {
					DateFieldHighLightObjectsVerticesList.add(i, makeHighLightTodayVerticesTexturePosDataOfDateFields(topmost, bottommost, textureTop, textureBottom));
				}
				else
					DateFieldHighLightObjectsVerticesList.add(i, makeHighLightNoneTodayVerticesTexturePosDataOfDateFields(topmost, bottommost, textureTop, textureBottom, CalCalendar));
								
    			// ���� ������ date ������ ��� ���� -1���� ���Ѵ�
    			CalCalendar.add(Calendar.DATE, -1);    			
			}				
		}
		
		return DateFieldHighLightObjectsVerticesList;    		
	}
		
	
	private ArrayList<ArrayList<FieldObjectVertices>> buildVerticesTexturePosDataOfHourFields(double CurrentHourFieldLowermostItemTopSideDegree, Calendar CalCalendar) {
		int mustBeDrawItemCount = 0;			
		ArrayList<ArrayList<FieldObjectVertices>> HourFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		double[] yAxisHourField = makeYAxisField(CurrentHourFieldLowermostItemTopSideDegree);
		mustBeDrawItemCount = yAxisHourField.length - 1;
		
		CalCalendar.add(Calendar.HOUR_OF_DAY, 4);
		float hourFieldOfPickerLeftMost = m_hourPickerLeftMostVPRatio;		
		float topmost = 0;
		float lowermost = 0;				
		float[] Points = null;
		
		for (int i=0; i<mustBeDrawItemCount; i++) {
			int ObjListIndex = 0;
			
			topmost = (float) yAxisHourField[i+1];
			lowermost = (float) yAxisHourField[i];
			
			/////////////////////////////////////////////////////////////////	    		
			double abs = (double)Math.abs(topmost - lowermost);
			double step = abs / COLUMN_MESH;    			   		
    		  		       		
			Points = buildVerticesOfYAndZAxis(lowermost, step);    		
    		//////////////////////////////////////////////////////////////////
    		
			ArrayList<FieldObjectVertices> ObjList = new ArrayList<FieldObjectVertices>();
			
			//////////////////////////////////////////////////////////////////////////////////////////////////
			int hour = CalCalendar.get(Calendar.HOUR_OF_DAY); // android�� hour time�� 0 ~ 11 ������ ordering�� ������
			//Log.i("tag", "cHour=" + String.valueOf(hour));
			
			if (hour == 0) {
				hour = 12; 
			}
						
			if (hour > 9) {		
				makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
		                                  hourFieldOfPickerLeftMost, hour, Points, 
                                          ObjList, ObjListIndex);				
			}
			else {
				makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
		                                  hourFieldOfPickerLeftMost, hour, Points, 
                                          ObjList, ObjListIndex);				
			}
							
			/////////////////////////////////////////////
			CalCalendar.add(Calendar.HOUR_OF_DAY, -1);			
			////////////////////////////////////////////////
			HourFieldRowObjectsVerticesList.add(i, ObjList);
		}		
		
		return HourFieldRowObjectsVerticesList;			
	}
	    	
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildVerticesTexturePosDataOfHourFieldsHighLight() {   		
		ArrayList<ArrayList<HighLightFieldObjectVertices>> HourFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
		Calendar hourCalCalendar = (Calendar)mHourFieldCalendar.clone();  
		
		float hourFieldOfPickerLeftMost = m_hourPickerLeftMostVPRatio - mHourFieldHighLightInterpolationRatio;		
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;
		
		double targetTopSideDegree = mCurrentHourFieldLowermostItemTopSideDegree + CENTERITEM_BOTTOM_DEGREE;
		
		if (targetTopSideDegree == CENTERITEM_TOP_DEGREE) {
			int ObjListIndex = 0;
			// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
			topmost = mCenterItemRegionTopRatio;
			bottommost = mCenterItemRegionBottomRatio;
			textureTop = 0.0f;
			textureBottom = 1.0f;
			
			//////////////////////////////////////////////////////////////////////////////////////////////////
			int hour = hourCalCalendar.get(Calendar.HOUR_OF_DAY);
			if (hour == 0) {
				hour = 12; 
			}
						
			if (hour > 9) {	
				makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
						                           hourFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
						                           hour, ObjList, ObjListIndex);				
			}
			else {
				makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
						                           hourFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
			                                       hour, ObjList, ObjListIndex);				
			}			
			
			HourFieldHighLightObjectsVerticesList.add(0, ObjList);				
		}
		else { // highlight�� �� ���� �������� ��ġ�Ѵ� : highlight ������ ������ �� �ִ� �������� ������ �ִ� ������ 2�̴�       			
			float TopOfCenterRegion = mCenterItemRegionTopRatio;
			float BottomOfCenterRegion = mCenterItemRegionBottomRatio;
			
			for (int i=0; i<2; i++) {
				float targetHeightRatio = 0;
				
				if (i==0) { // ���� ������						
					lowerItemTopmost = -(float)(Math.cos(Math.toRadians(targetTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
					lowerItemBottommost = BottomOfCenterRegion;
					
					if (lowerItemTopmost >= 0) {
						targetHeightRatio = mCenterItemHalfHeightRatio + lowerItemTopmost;
					}
					else {
						// topmost ���� 0���� ���� ���
						targetHeightRatio = lowerItemTopmost - lowerItemBottommost;///////////////////////////
					}
					
					// ���� �������� �ؽ����� �� �κк��� drawing�Ǿ�� �Ѵ�
					textureTop = 0.0f;
					// �ؽ��ĳ��� ������ ����ؾ� �Ѵ�
					
					textureBottom = 0.0f + (targetHeightRatio/mCenterItemHeightRatio);
					
					topmost = lowerItemTopmost;
					bottommost = lowerItemBottommost;
				}
				else { // ���� ������
					upperItemTopmost = TopOfCenterRegion;
					upperItemBottommost = -(float)(Math.cos(Math.toRadians(targetTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
					
					
					if (upperItemBottommost >= 0) {
						targetHeightRatio = upperItemTopmost - upperItemBottommost;///////////////////////////////
					}
					else {
						targetHeightRatio = mCenterItemHalfHeightRatio + Math.abs(upperItemBottommost);
					}
					textureTop = 1.0f - (targetHeightRatio/mCenterItemHeightRatio);
					// ���� �������� �ؽ����� �Ʒ� �κк��� drawing�Ǿ�� �Ѵ�
					textureBottom = 1.0f;
					
					topmost = upperItemTopmost;
					bottommost = upperItemBottommost;
				}					
				
				int ObjListIndex = 0;
    			// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
    			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
				
				//////////////////////////////////////////////////////////////////////////////////////////////////
    			int hour = hourCalCalendar.get(Calendar.HOUR_OF_DAY);
				if (hour == 0) { //0�� �� 12�ø� �ǹ��Ѵ�
					hour = 24;
				}
				
				if (hour >= 12) { // pm Ÿ�Ӵ��� hour
					hour = hour - 12;	
					if (hour == 0) {
						hour = 12; // �� 12�ø� �ǹ��Ѵ�
						// ���⼭ meridiem�� ������Ʈ�� �ʿ䰡 ������ Ư�� ��ġ�� ���Ѵ�
					}
				}
				
				if (hour > 9) {	
					makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
	                                                   hourFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
	                                                   hour, ObjList, ObjListIndex);					
				}
				else {
					makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
	                                                   hourFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
                                                       hour, ObjList, ObjListIndex);					
				}			
				
				HourFieldHighLightObjectsVerticesList.add(i, ObjList);
    			// ���� ������ date ������ ��� ����
    			hourCalCalendar.add(Calendar.HOUR_OF_DAY, -1);		
			}				
		}
		
		return HourFieldHighLightObjectsVerticesList;    		
	}
	
	static final int MinuteCountTick = 5;
	private ArrayList<ArrayList<FieldObjectVertices>> buildVerticesTexturePosDataOfMinuteFields(double CurrentMinuteFieldLowermostItemTopSideDegree, Calendar CalCalendar) {  
		int mustBeDrawItemCount = 0;			
		ArrayList<ArrayList<FieldObjectVertices>> MinuteFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		double[] yAxisMinuteField = makeYAxisField(CurrentMinuteFieldLowermostItemTopSideDegree);
		mustBeDrawItemCount = yAxisMinuteField.length - 1;
		
		CalCalendar.add(Calendar.MINUTE, (MinuteCountTick * 4));
		//CalCalendar.add(Calendar.MINUTE, 4);
		float minuteFieldOfPickerLeftMost = m_minutePickerLeftMostVPRatio;			
		
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float lowermost = 0;		
		float[] Points = null;
		
		for (int i=0; i<mustBeDrawItemCount; i++) {
			int ObjListIndex = 0;
			
			topmost = (float) yAxisMinuteField[i+1];
			lowermost = (float) yAxisMinuteField[i];
			
			/////////////////////////////////////////////////////////////////	    		
			double abs = (double)Math.abs(topmost - lowermost);
			double step = abs / COLUMN_MESH;    			   		
    		  		       		
			Points = buildVerticesOfYAndZAxis(lowermost, step);    			
    		//////////////////////////////////////////////////////////////////
    		
			ArrayList<FieldObjectVertices> ObjList = new ArrayList<FieldObjectVertices>();
			
			//////////////////////////////////////////////////////////////////////////////////////////////////
			int minute = CalCalendar.get(Calendar.MINUTE);
			int numberOfTensOfMinuteNumber = 0;
			int numberOfOnesOfMinuteNumber = 0;
			if (minute > 9) {
				makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
						                  minuteFieldOfPickerLeftMost, minute, Points, 
                                          ObjList, ObjListIndex);				
			}
			else {
				String minuteStr = String.valueOf(minute);
				numberOfOnesOfMinuteNumber = Integer.parseInt(minuteStr);
				numberOfTensOfMinuteNumber = 0;
				
				int minuteTensTextureHandle = mNumeralTextTextureDataHandles[numberOfTensOfMinuteNumber];
				leftmost = minuteFieldOfPickerLeftMost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;
				FieldObjectVertices minuteTensObject = new FieldObjectVertices(minuteTensTextureHandle, Points, 
																			 leftmost, rightmost, 
																			 mTimePickerNumeralWidthRatio);					
				ObjList.add(ObjListIndex++, minuteTensObject);
				
				int minuteOnesTextureHandle = mNumeralTextTextureDataHandles[numberOfOnesOfMinuteNumber];
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				FieldObjectVertices minuteOnesObject = new FieldObjectVertices(minuteOnesTextureHandle, Points, 
																			 leftmost, rightmost, 
																			 mTimePickerNumeralWidthRatio);					
				ObjList.add(ObjListIndex, minuteOnesObject);
			}
							
			/////////////////////////////////////////////
			CalCalendar.add(Calendar.MINUTE, -MinuteCountTick);
			//CalCalendar.add(Calendar.MINUTE, -1);
			////////////////////////////////////////////////
			MinuteFieldRowObjectsVerticesList.add(i, ObjList);
		}	
		
		return MinuteFieldRowObjectsVerticesList;
	}
	
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildVerticesTexturePosDataOfMinuteFieldsHighLight() {   		
		ArrayList<ArrayList<HighLightFieldObjectVertices>> MinuteFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
		Calendar minuteCalCalendar = (Calendar)mMinuteFieldCalendar.clone(); 
		
		float minuteFieldOfPickerLeftMost = m_minutePickerLeftMostVPRatio - mMinuteFieldHighLightInterpolationRatio;	
		float leftmost = 0;
		float rightmost = 0;
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;
		
		double targetTopSideDegree = mCurrentMinuteFieldLowermostItemTopSideDegree + CENTERITEM_BOTTOM_DEGREE;
		
		if (targetTopSideDegree == CENTERITEM_TOP_DEGREE) {
			int ObjListIndex = 0;
			// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();						
			topmost = mCenterItemRegionTopRatio;
			bottommost = mCenterItemRegionBottomRatio;
			textureTop = 0.0f;
			textureBottom = 1.0f;
			
			int minute = minuteCalCalendar.get(Calendar.MINUTE);
			int numberOfTensOfMinuteNumber = 0;
			int numberOfOnesOfMinuteNumber = 0;
			if (minute > 9) {
				makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
						                           minuteFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
                                                   minute, ObjList, ObjListIndex);				
			}
			else {
				String minuteStr = String.valueOf(minute);
				numberOfOnesOfMinuteNumber = Integer.parseInt(minuteStr);
				numberOfTensOfMinuteNumber = 0;
				
				int minuteTensTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfTensOfMinuteNumber];
				leftmost = minuteFieldOfPickerLeftMost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;
				HighLightFieldObjectVertices minuteTensObject = new HighLightFieldObjectVertices(minuteTensTextureHandle, 
						                                                                         leftmost, rightmost, 																			          
						                                                                         topmost, bottommost,
						                                                                         textureTop, textureBottom);								
				ObjList.add(ObjListIndex++, minuteTensObject);
				
				int minuteOnesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfMinuteNumber];
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerNumeralWidthRatio;								
				HighLightFieldObjectVertices minuteOnesObject = new HighLightFieldObjectVertices(minuteOnesTextureHandle, 
						   																         leftmost, rightmost, 																			          
						   																         topmost, bottommost,
						   																         textureTop, textureBottom);								
				ObjList.add(ObjListIndex++, minuteOnesObject);
			}			
			
			MinuteFieldHighLightObjectsVerticesList.add(0, ObjList);				
		}
		else {
			// highlight�� �� ���� �������� ��ġ�Ѵ� : highlight ������ ������ �� �ִ� �������� ������ �ִ� ������ 2�̴�   
			// ���Ϳ����� �߽��� y=0.0 �̴�
			float TopOfCenterRegion = mCenterItemRegionTopRatio;
			float BottomOfCenterRegion = mCenterItemRegionBottomRatio;
			
			for (int i=0; i<2; i++) {
				if (i==0) { // ���� ������						
					lowerItemTopmost = -(float)(Math.cos(Math.toRadians(targetTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
					lowerItemBottommost = BottomOfCenterRegion;
					
					float targetHeightRatio = 0;
					if (lowerItemTopmost >= 0) {
						targetHeightRatio = mCenterItemHalfHeightRatio + lowerItemTopmost;
					}
					else {
						// topmost ���� 0���� ���� ���
						targetHeightRatio = lowerItemTopmost - lowerItemBottommost;///////////////////////////
					}
					
					// ���� �������� �ؽ����� �� �κк��� drawing�Ǿ�� �Ѵ�
					textureTop = 0.0f;
					// �ؽ��ĳ��� ������ ����ؾ� �Ѵ�						
					textureBottom = 0.0f + (targetHeightRatio/mCenterItemHeightRatio);
					
					topmost = lowerItemTopmost;
					bottommost = lowerItemBottommost;
				}
				else { // ���� ������
					upperItemTopmost = TopOfCenterRegion;
					upperItemBottommost = -(float)(Math.cos(Math.toRadians(targetTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
					
					float targetHeightRatio = 0;
					if (upperItemBottommost >= 0) {
						targetHeightRatio = upperItemTopmost - upperItemBottommost;///////////////////////////////
					}
					else {
						targetHeightRatio = mCenterItemHalfHeightRatio + Math.abs(upperItemBottommost);
					}
					textureTop = 1.0f - (targetHeightRatio/mCenterItemHeightRatio);
					// ���� �������� �ؽ����� �Ʒ� �κк��� drawing�Ǿ�� �Ѵ�
					textureBottom = 1.0f;
					
					topmost = upperItemTopmost;
					bottommost = upperItemBottommost;
				}					
				
				int ObjListIndex = 0;
    			// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
    			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
				
				//////////////////////////////////////////////////////////////////////////////////////////////////
    			int minute = minuteCalCalendar.get(Calendar.MINUTE);
				int numberOfTensOfMinuteNumber = 0;
				int numberOfOnesOfMinuteNumber = 0;
				if (minute > 9) {
					makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
	                                                   minuteFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
                                                       minute, ObjList, ObjListIndex);					
				}
				else {
					String minuteStr = String.valueOf(minute);
					numberOfOnesOfMinuteNumber = Integer.parseInt(minuteStr);
					numberOfTensOfMinuteNumber = 0;
					
					int minuteTensTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfTensOfMinuteNumber];
					leftmost = minuteFieldOfPickerLeftMost;
					rightmost = leftmost + mTimePickerNumeralWidthRatio;
					HighLightFieldObjectVertices minuteTensObject = new HighLightFieldObjectVertices(minuteTensTextureHandle, 
							                                                                         leftmost, rightmost, 																			          
							                                                                         topmost, bottommost,
							                                                                         textureTop, textureBottom);								
					ObjList.add(ObjListIndex++, minuteTensObject);
					
					int minuteOnesTextureHandle = mCenterNumeralTextTextureDataHandles[numberOfOnesOfMinuteNumber];
					leftmost = rightmost;
					rightmost = leftmost + mTimePickerNumeralWidthRatio;								
					HighLightFieldObjectVertices minuteOnesObject = new HighLightFieldObjectVertices(minuteOnesTextureHandle, 
							   																         leftmost, rightmost, 																			          
							   																         topmost, bottommost,
							   																         textureTop, textureBottom);								
					ObjList.add(ObjListIndex++, minuteOnesObject);
				}			
				
				MinuteFieldHighLightObjectsVerticesList.add(i, ObjList);
    			// ���� ������ minute ������ ��� ����
    			minuteCalCalendar.add(Calendar.MINUTE, -MinuteCountTick);
				//minuteCalCalendar.add(Calendar.MINUTE, -1);
			}				
		}
		
		return MinuteFieldHighLightObjectsVerticesList;    		
	}
	
	
	private ArrayList<ArrayList<FieldObjectVertices>> buildVerticesTexturePosDataOfMeridiemFields(double CurrentMeridiemFieldLowermostItemTopSideDegree, Calendar CalCalendar) {  
		double y = 0; 
		double[] yAxisMeridiemField = new double[3];
		ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		    		   		
		int mustBeDrawItemCount = 2;
		double LowerMostBottomSideAngdeg = CurrentMeridiemFieldLowermostItemTopSideDegree - FIELD_ITEM_DEGREE;
		double degree = LowerMostBottomSideAngdeg;
		for (int i=0; i<3; i++) {
			double radianValue = Math.toRadians(degree);	
			y = Math.cos(radianValue) * TIMEPICKER_FIELD_CIRCLE_RADIUS;
			yAxisMeridiemField[i] = -y;    			
			degree = degree + FIELD_ITEM_DEGREE;
		}
		
		float meridiemFieldOfPickerLeftMost = m_meridiemPickerLeftMostVPRatio;
		
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float lowermost = 0;			
		float[] Points = null;
		
		for (int i=0; i<mustBeDrawItemCount; i++) {
			int ObjListIndex = 0;
			
			topmost = (float) yAxisMeridiemField[i+1];
			lowermost = (float) yAxisMeridiemField[i];
			
			/////////////////////////////////////////////////////////////////	    		
			double abs = (double)Math.abs(topmost - lowermost);
			double step = abs / COLUMN_MESH;    			   		
    		
			Points = buildVerticesOfYAndZAxis(lowermost, step);
    		//////////////////////////////////////////////////////////////////
    		
			ArrayList<FieldObjectVertices> ObjList = new ArrayList<FieldObjectVertices>();
			
			int firstTextTextureHandle = mMeridiemTextTextureDataHandles[0];
			leftmost = meridiemFieldOfPickerLeftMost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			FieldObjectVertices firstTextObject = new FieldObjectVertices(firstTextTextureHandle, Points, 
																		 leftmost, rightmost, 
																		 mTimePickerTextWidthRatio);					
			ObjList.add(ObjListIndex++, firstTextObject);
			
							
			int secondTextTextureHandle = mMeridiemTextTextureDataHandles[i+1]; //bitmap list�� "��", "��" ������ �����ؾ� �Ѵ�
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			FieldObjectVertices secondTextObject = new FieldObjectVertices(secondTextTextureHandle, Points, 
																		 leftmost, rightmost, 
																		 mTimePickerTextWidthRatio);					
			ObjList.add(ObjListIndex, secondTextObject);
			
			MeridiemFieldRowObjectsVerticesList.add(i, ObjList);
		}    
		
		return MeridiemFieldRowObjectsVerticesList;			
	}
	
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildVerticesTexturePosDataOfMeridiemFieldsHighLight() {   		
		ArrayList<ArrayList<HighLightFieldObjectVertices>> MeridiemFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
		MeridiemFieldHighLightObjectsVerticesList.clear();
		
		float meridiemFieldOfPickerLeftMost = m_meridiemPickerLeftMostVPRatio;	
		float leftmost = 0;
		float rightmost = 0;
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;			
		    		
		if ( (CENTERITEM_TOP_DEGREE >= mCurrentMeridiemFieldLowermostItemTopSideDegree) && 
				(mCurrentMeridiemFieldLowermostItemTopSideDegree >= CENTERITEM_BOTTOM_DEGREE) ) {			
			if ((mCurrentMeridiemFieldLowermostItemTopSideDegree == CENTERITEM_TOP_DEGREE) || 
					(mCurrentMeridiemFieldLowermostItemTopSideDegree == CENTERITEM_BOTTOM_DEGREE) ) { // ���� �Ǵ� ���� �κ��� ��ü ������ drawing �ȴ�							
				ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
				topmost = mCenterItemRegionTopRatio;
				bottommost = mCenterItemRegionBottomRatio;
				textureTop = 0.0f;
				textureBottom = 1.0f;				
						
				if (mCurrentMeridiemFieldLowermostItemTopSideDegree == CENTERITEM_BOTTOM_DEGREE) { // ���� �κ��� ��ü ������ drawing �ȴ�						
					int firstTextTextureHandle = mCenterMeridiemTextTextureDataHandles[0];
					leftmost = meridiemFieldOfPickerLeftMost;
					rightmost = leftmost + mTimePickerTextWidthRatio;						
					
					HighLightFieldObjectVertices firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle, 
		                    																		leftmost, rightmost, 																			          
		                    																		topmost, bottommost,
		                    																		textureTop, textureBottom);	    		
					ObjList.add(0, firstTextObject);		
					
					int secondTextTextureHandle = mCenterMeridiemTextTextureDataHandles[2]; // ���� : 2
					leftmost = rightmost;
					rightmost = leftmost + mTimePickerTextWidthRatio;
					HighLightFieldObjectVertices secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle, 
		                    																		 leftmost, rightmost, 																			          
		                    																		 topmost, bottommost,
		                    																		 textureTop, textureBottom);	    						
					ObjList.add(1, secondTextObject);						
				}
				else { // ���� �κ��� ��ü ������ drawing �ȴ�							
					int firstTextTextureHandle = mCenterMeridiemTextTextureDataHandles[0];
					leftmost = meridiemFieldOfPickerLeftMost;
					rightmost = leftmost + mTimePickerTextWidthRatio;						
					
					HighLightFieldObjectVertices firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle, 
		                    																		leftmost, rightmost, 																			          
		                    																		topmost, bottommost,
		                    																		textureTop, textureBottom);	    		
					ObjList.add(0, firstTextObject);		
					
					int secondTextTextureHandle = mCenterMeridiemTextTextureDataHandles[1]; // ����: 1
					leftmost = rightmost;
					rightmost = leftmost + mTimePickerTextWidthRatio;
					HighLightFieldObjectVertices secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle, 
		                    																		 leftmost, rightmost, 																			          
		                    																		 topmost, bottommost,
		                    																		 textureTop, textureBottom);	    						
					ObjList.add(1, secondTextObject);			
				}
				MeridiemFieldHighLightObjectsVerticesList.add(0, ObjList);
			}
			else { // ���� �κ��� �Ϻ� ������ ���� �κ��� �Ϻ� ������ drawing �ȴ�					
				float TopOfCenterRegion = mCenterItemRegionTopRatio;
    			float BottomOfCenterRegion = mCenterItemRegionBottomRatio;			
    			
				// ���� �κ�	�� �Ϻ� ����		
    			ArrayList<HighLightFieldObjectVertices> UpperObjList = new ArrayList<HighLightFieldObjectVertices>();
				int firstTextTextureHandle = mCenterMeridiemTextTextureDataHandles[0];									
				upperItemTopmost = TopOfCenterRegion;
				upperItemBottommost = -(float)(Math.cos(Math.toRadians(mCurrentMeridiemFieldLowermostItemTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);					
				float targetHeightRatio = 0;
				if (upperItemBottommost >= 0) {
					targetHeightRatio = upperItemTopmost - upperItemBottommost;///////////////////////////////
				}
				else {
					targetHeightRatio = mCenterItemHalfHeightRatio + Math.abs(upperItemBottommost);
				}
				textureTop = 1.0f - (targetHeightRatio/mCenterItemHeightRatio);					
				textureBottom = 1.0f;// ���� �������� �ؽ����� �Ʒ� �κк��� drawing�Ǿ�� �Ѵ�
				
				leftmost = meridiemFieldOfPickerLeftMost;
				rightmost = leftmost + mTimePickerTextWidthRatio;	
				topmost = upperItemTopmost;
				bottommost = upperItemBottommost;
				HighLightFieldObjectVertices firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle, 
	                    																		leftmost, rightmost, 																			          
	                    																		topmost, bottommost,
	                    																		textureTop, textureBottom);	    		
				UpperObjList.add(0, firstTextObject);		
				
				int secondTextTextureHandle = mCenterMeridiemTextTextureDataHandles[2]; // ���� : 2
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerTextWidthRatio;
				HighLightFieldObjectVertices secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle, 
	                    																		 leftmost, rightmost, 																			          
	                    																		 topmost, bottommost,
	                    																		 textureTop, textureBottom);	    
				UpperObjList.add(1, secondTextObject);	
				MeridiemFieldHighLightObjectsVerticesList.add(0, UpperObjList);
				
				///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				// ���� �κ�	�� �Ϻ� ����
				ArrayList<HighLightFieldObjectVertices> LowerObjList = new ArrayList<HighLightFieldObjectVertices>();
				lowerItemTopmost = -(float)(Math.cos(Math.toRadians(mCurrentMeridiemFieldLowermostItemTopSideDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
				lowerItemBottommost = BottomOfCenterRegion;					
				targetHeightRatio = 0;
				if (lowerItemTopmost >= 0) {
					targetHeightRatio = mCenterItemHalfHeightRatio + lowerItemTopmost;
				}
				else {						
					targetHeightRatio = lowerItemTopmost - lowerItemBottommost;// topmost ���� 0���� ���� ���
				}										
				textureTop = 0.0f;// ���� �������� �ؽ����� �� �κк��� drawing�Ǿ�� �Ѵ�					
				textureBottom = 0.0f + (targetHeightRatio/mCenterItemHeightRatio);
				
				leftmost = meridiemFieldOfPickerLeftMost;
				rightmost = leftmost + mTimePickerTextWidthRatio;
				topmost = lowerItemTopmost;
				bottommost = lowerItemBottommost;
				firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle, 
						leftmost, rightmost, 																			          
						topmost, bottommost,
						textureTop, textureBottom);	    		
				LowerObjList.add(0, firstTextObject);		

				secondTextTextureHandle = mCenterMeridiemTextTextureDataHandles[1]; // ���� : 1
				leftmost = rightmost;
				rightmost = leftmost + mTimePickerTextWidthRatio;
				secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle, 
											 leftmost, rightmost, 																			          
											 topmost, bottommost,
											 textureTop, textureBottom);	    
				LowerObjList.add(1, secondTextObject);	
				MeridiemFieldHighLightObjectsVerticesList.add(1, LowerObjList);
			}
		}
		else if ( (CENTERITEM_BOTTOM_DEGREE > mCurrentMeridiemFieldLowermostItemTopSideDegree) && 
				(mCurrentMeridiemFieldLowermostItemTopSideDegree > (CENTERITEM_BOTTOM_DEGREE - FIELD_ITEM_DEGREE))){ // ���� �κ��� �Ϻ� ������ drawing �ȴ�				
			float BottomOfCenterRegion = mCenterItemRegionBottomRatio;		
			
			ArrayList<HighLightFieldObjectVertices> LowerObjList = new ArrayList<HighLightFieldObjectVertices>();
			double targetDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree + FIELD_ITEM_DEGREE;
			lowerItemTopmost = -(float)(Math.cos(Math.toRadians(targetDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);
			lowerItemBottommost = BottomOfCenterRegion;					
			float targetHeightRatio = 0;
			if (lowerItemTopmost >= 0) {
				targetHeightRatio = mCenterItemHalfHeightRatio + lowerItemTopmost;
			}
			else {						
				targetHeightRatio = lowerItemTopmost - lowerItemBottommost;// topmost ���� 0���� ���� ���
			}										
			textureTop = 0.0f;// ���� �������� �ؽ����� �� �κк��� drawing�Ǿ�� �Ѵ�					
			textureBottom = 0.0f + (targetHeightRatio/mCenterItemHeightRatio);
			
			leftmost = meridiemFieldOfPickerLeftMost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			topmost = lowerItemTopmost;
			bottommost = lowerItemBottommost;
			int firstTextTextureHandle = mCenterMeridiemTextTextureDataHandles[0];
			HighLightFieldObjectVertices firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle, 
																							leftmost, rightmost, 																			          
																							topmost, bottommost,
																							textureTop, textureBottom);	    		
			LowerObjList.add(0, firstTextObject);		
			
			int secondTextTextureHandle = mCenterMeridiemTextTextureDataHandles[2]; // ����: 2
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			HighLightFieldObjectVertices secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle, 
																							 leftmost, rightmost, 																			          
																							 topmost, bottommost,
																							 textureTop, textureBottom);	    
			LowerObjList.add(1, secondTextObject);	
			MeridiemFieldHighLightObjectsVerticesList.add(0, LowerObjList);
		}
		else if ( ((CENTERITEM_TOP_DEGREE + FIELD_ITEM_DEGREE) > mCurrentMeridiemFieldLowermostItemTopSideDegree) && 
				(mCurrentMeridiemFieldLowermostItemTopSideDegree > CENTERITEM_TOP_DEGREE)){ // ���� �κ��� �Ϻ� ������ drawing �ȴ�
			
			float TopOfCenterRegion = mCenterItemRegionTopRatio;    			
			
			ArrayList<HighLightFieldObjectVertices> UpperObjList = new ArrayList<HighLightFieldObjectVertices>();
			int firstTextTextureHandle = mCenterMeridiemTextTextureDataHandles[0];									
			upperItemTopmost = TopOfCenterRegion;
			double targetDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - FIELD_ITEM_DEGREE;
			upperItemBottommost = -(float)(Math.cos(Math.toRadians(targetDegree)) * TIMEPICKER_FIELD_CIRCLE_RADIUS);					
			float targetHeightRatio = 0;
			if (upperItemBottommost >= 0) {
				targetHeightRatio = upperItemTopmost - upperItemBottommost;///////////////////////////////
			}
			else {
				targetHeightRatio = mCenterItemHalfHeightRatio + Math.abs(upperItemBottommost);
			}
			textureTop = 1.0f - (targetHeightRatio/mCenterItemHeightRatio);					
			textureBottom = 1.0f;// ���� �������� �ؽ����� �Ʒ� �κк��� drawing�Ǿ�� �Ѵ�
			
			leftmost = meridiemFieldOfPickerLeftMost;
			rightmost = leftmost + mTimePickerTextWidthRatio;	
			topmost = upperItemTopmost;
			bottommost = upperItemBottommost;
			HighLightFieldObjectVertices firstTextObject = new HighLightFieldObjectVertices(firstTextTextureHandle, 
                    																		leftmost, rightmost, 																			          
                    																		topmost, bottommost,
                    																		textureTop, textureBottom);	    		
			UpperObjList.add(0, firstTextObject);		
			
			int secondTextTextureHandle = mCenterMeridiemTextTextureDataHandles[1]; // ���� : 1
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			HighLightFieldObjectVertices secondTextObject = new HighLightFieldObjectVertices(secondTextTextureHandle, 
                    																		 leftmost, rightmost, 																			          
                    																		 topmost, bottommost,
                    																		 textureTop, textureBottom);	    
			UpperObjList.add(1, secondTextObject);	
			MeridiemFieldHighLightObjectsVerticesList.add(0, UpperObjList);
		}
		return MeridiemFieldHighLightObjectsVerticesList;    		
	}
	
	
	
	
	
	
	
		
		
	
	
	
		
	public int addedDayOfMonthForCal(int daysOfThisMonth, int dayOfMonth, int value) {		
		int addedDayOfMonth = dayOfMonth + value;		
		
		int newDayOfMonth = 0;
		if (addedDayOfMonth > daysOfThisMonth) {						
			// addedDayOfMonth�� (daysOfThisMonth + ?)�̹Ƿ�,
			// (daysOfThisMonth + 1)�� 1�� �Ǿ�� �Ѵ�.. (daysOfThisMonth + 1) + x = 1 --> x = 1 - (daysOfThisMonth + 1) --> x = -daysOfThisMonth
			// (daysOfThisMonth + 2)�� 2�� �Ǿ�� �Ѵ�.. (daysOfThisMonth + 2) + x = 2 --> x = 2 - (daysOfThisMonth + 2) --> x = -daysOfThisMonth
			// (daysOfThisMonth + 3)�� 3�� �Ǿ�� �Ѵ�.. (daysOfThisMonth + 3) + x = 3 --> x = 3 - (daysOfThisMonth + 3) --> x = -daysOfThisMonth
			newDayOfMonth = addedDayOfMonth - daysOfThisMonth;			
		}
		else if (addedDayOfMonth < 1) {
			// addedDayOfMonth�� 0�̶��, daysOfThisMonth�� �Ǿ�� �Ѵ�
			// addedDayOfMonth�� -1�̶��, (daysOfThisMonth - 1)�� �Ǿ�� �Ѵ�    			
			newDayOfMonth = addedDayOfMonth + daysOfThisMonth;			
		}
		else {
			newDayOfMonth = addedDayOfMonth;
		}
		
		return newDayOfMonth;			
	}
	
	public int addedDayOfMonthForCal(int dayOfMonth, int value) {	
		int newDayOfMonth = 0;
		int addedDayOfMonth = dayOfMonth + value;		
		
		if (addedDayOfMonth > 31) {			
			newDayOfMonth = addedDayOfMonth - 31;		
		}
		else if (addedDayOfMonth < 1) {			
			newDayOfMonth = addedDayOfMonth + 31;
			
		}
		else {
			newDayOfMonth = addedDayOfMonth;
		}
		
		return newDayOfMonth;			
	}
	
	public int addedMonthForCal(int month, int value) {	
		int newMonthNumbers = 0;
		int addedMonthNumbers = month + value;
		
		if (addedMonthNumbers > 11) {
			// 12�� 0�� �Ǿ�� �Ѵ�..12 + x = 0 -> x = -12
			// 13�� 1�� �Ǿ�� �Ѵ�..13 + x = 1 -> x = -12    			
			newMonthNumbers = addedMonthNumbers - 12;
			  
		}
		else if (addedMonthNumbers < 0) {
			// -1�� 11�� �Ǿ�� �Ѵ�..-1 + x = 11 -> x = 11 + 1 -> x = 12
			// -2�� 10�� �Ǿ�� �Ѵ�..-2 + x = 10 -> x = 10 + 2 -> x = 12    			
			newMonthNumbers = addedMonthNumbers + 12;			 
		}
		else {
			newMonthNumbers = month + value;
		}
		
		return newMonthNumbers;			
	}
	
	
	
	
	
	
	public void setCurrentFieldLowermostItemTopSideDegree(int field, double degree) {
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		mCurrentDateFieldLowermostItemTopSideDegree = degree;
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		//mCurrentMerideimFieldLowermostItemTopSideDegree = degree;
    		break;
    	case HOUR_FIELD_TEXTURE:
    		mCurrentHourFieldLowermostItemTopSideDegree = degree;
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		mCurrentMinuteFieldLowermostItemTopSideDegree = degree;
    		break;    	
    	default:
    		break;
    	}
	}
	
	public void setCurrentFieldUppermostItemBottomSideDegree(int field, double degree) {
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		mCurrentDateFieldUppermostItemBottomSideDegree = degree;
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		//mCurrentMerideimFieldLowermostItemTopSideDegree = degree;
    		break;
    	case HOUR_FIELD_TEXTURE:
    		mCurrentHourFieldUppermostItemBottomSideDegree = degree;
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		mCurrentMinuteFieldUppermostItemBottomSideDegree = degree;
    		break;    	
    	default:
    		break;
    	}
	}
	
	
	public double getCurrentFieldLowermostItemTopSideDegree(int field) {
		double currentDegree = 0;
		
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		currentDegree = mCurrentDateFieldLowermostItemTopSideDegree;
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		//mCurrentMerideimFieldLowermostItemTopSideDegree = degree;
    		break;
    	case HOUR_FIELD_TEXTURE:
    		currentDegree = mCurrentHourFieldLowermostItemTopSideDegree;
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		currentDegree = mCurrentMinuteFieldLowermostItemTopSideDegree;
    		break;    	
    	default:
    		break;
    	}
		
		return currentDegree;
	}
	
	public double getCurrentFieldUppermostItemBottomSideDegree(int field) {
		double currentDegree = 0;
		
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		currentDegree = mCurrentDateFieldUppermostItemBottomSideDegree;
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		//mCurrentMerideimFieldLowermostItemTopSideDegree = degree;
    		break;
    	case HOUR_FIELD_TEXTURE:
    		currentDegree = mCurrentHourFieldUppermostItemBottomSideDegree;
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		currentDegree = mCurrentMinuteFieldUppermostItemBottomSideDegree;
    		break;    	
    	default:
    		break;
    	}
		
		return currentDegree;
	}
	
	public boolean IsLeapYear(int year) {
		boolean leapYear = false;
		
		
		int resultBy4Divide = year % 4;
		if (resultBy4Divide == 0) {
			leapYear = true;
			
			int resultBy100Divide = year % 100;
			if (resultBy100Divide == 0) {
				leapYear = false;
				int resultBy400Divide = year % 400;
				if (resultBy400Divide == 0)
					leapYear = true;
			}
		}			
		
		return leapYear;
	}
	
	public int getActualMaximumOfMonth(int month, int year) {
		int max = -1;
		switch(month) {
		case 0:   //1��
			max = 31;
			break;
		case 1:   //2��
			if(IsLeapYear(year)) 
				max = 29;			
			else
				max = 28;			
			break;
		case 2:   //3��
			max = 31;
			break;
		case 3:   //4��
			max = 30;
			break;
		case 4:   //5��
			max = 31;
			break;
		case 5:   //6��
			max = 30;
			break;
		case 6:   //7��
			max = 31;
			break;
		case 7:   //8��
			max = 31;
			break;
		case 8:   //9��
			max = 30;
			break;
		case 9:   //10��
			max = 31;
			break;
		case 10:  //11��
			max = 30;
			break;
		case 11:  //12��
			max = 31;
			break;
		default:
			break;
		}
		
		return max;
	}
	
	
	public void setFieldCalendar(int field, int value) {
		
		
			switch(field) {
	    	case DATE_FIELD_TEXTURE:
	    		mDateFieldCalendar.add(Calendar.DATE, value);    		
	    		break;
	    	case MERIDIEM_FIELD_TEXTURE:    		
	    		break;
	    	case HOUR_FIELD_TEXTURE:
	    		mHourFieldCalendar.add(Calendar.HOUR_OF_DAY, value); 
	    		break;
	    	case MINUTE_FIELD_TEXTURE:
	    		mMinuteFieldCalendar.add(Calendar.MINUTE, (value * MinuteCountTick));    		
	    		break;	    	
	    	default:
	    		return;
	    	}
		
	}	
	
	public Calendar cloneFiledCalendar(int field) {
		Calendar cal = null;
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		cal = (Calendar)mDateFieldCalendar.clone();
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		
    		break;
    	case HOUR_FIELD_TEXTURE:
    		cal = (Calendar)mHourFieldCalendar.clone();
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		cal = (Calendar)mMinuteFieldCalendar.clone();
    		break;    	
    	default:
    		break;
    	}
		
		return cal;
	}
	
	public ArrayList<ArrayList<FieldObjectVertices>> buildVerticesTexturePosDataOfField(int field, Calendar calendar) {		
		ArrayList<ArrayList<FieldObjectVertices>> FieldRowObjectsVerticesList = null;
		
		switch(field) {
    	case DATE_FIELD_TEXTURE:
    		FieldRowObjectsVerticesList = buildVerticesTexturePosDataOfDateFields(mCurrentDateFieldLowermostItemTopSideDegree, calendar);
    		break;
    	case MERIDIEM_FIELD_TEXTURE:
    		
    		break;
    	case HOUR_FIELD_TEXTURE:
    		FieldRowObjectsVerticesList = buildVerticesTexturePosDataOfHourFields(mCurrentHourFieldLowermostItemTopSideDegree, calendar);
    		break;
    	case MINUTE_FIELD_TEXTURE:
    		FieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMinuteFields(mCurrentMinuteFieldLowermostItemTopSideDegree, calendar);
    		break; 
    	 
    	default:
    		break;
    	}
		
		return FieldRowObjectsVerticesList;
	}
	
	public void mustBeInterpolate(int field) {
		setCurrentFieldLowermostItemTopSideDegree(field, FIELD_ITEM_DEGREE);        		
		setCurrentFieldUppermostItemBottomSideDegree(field, TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE);		
	}
	
	
	public ArrayList<ArrayList<FieldObjectVertices>> updateFieldVerticesTexturesDatas(int field, float scalar, 
			boolean mustBeInterpolation) {        	
    	float scalarABS = 0;
    	double rotateTheta = 0;        	
    	int timewarp = 0;        	
    	double UppermostItemBottomSideDegree = 0;
    	double LowermostItemTopSideDegree = 0;
    	int prvAMPM = 0;    	    	
    	
    	if (field == HOUR_FIELD_TEXTURE) {
    		prvAMPM = mHourFieldCalendar.get(Calendar.AM_PM);
    	}
    	
    	if (scalar < 0) {  // finger move upward : down -> up      
    		if (mustBeInterpolation) { // finger move upward : down -> up ��쿡�� ��Ȯ�ϰ� �����Ѵ�, �׷��� �� �ݴ� ��Ȳ������ ��Ȯ�ϰ� �������� �ʴ´�        	
    			mustBeInterpolate(field);    			
        	}
    		else {
        		scalarABS = Math.abs(scalar);
        		// scalarABS�� length of arc�� ȯ������
        		// scalarABS = 2��r * (��/360)
        		// �� = (scalarABS/2��r) * 360
        		rotateTheta = (scalarABS / mTimePickerDateFieldCircleDiameterPixels) * 360;        		
        		        		
        		double upperMostItemRotateDegree = getCurrentFieldUppermostItemBottomSideDegree(field) + rotateTheta;
        		if ( upperMostItemRotateDegree >  TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE) { 
        			++timewarp; 
        			double exceededDegreeFromMaxDegree = upperMostItemRotateDegree - TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE;        			
        			timewarp = timewarp + (int)(exceededDegreeFromMaxDegree / FIELD_ITEM_DEGREE);        			
        			UppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE + (exceededDegreeFromMaxDegree % FIELD_ITEM_DEGREE);
        			setCurrentFieldUppermostItemBottomSideDegree(field, UppermostItemBottomSideDegree);
        			
        			LowermostItemTopSideDegree = UppermostItemBottomSideDegree - (TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE);
        			setCurrentFieldLowermostItemTopSideDegree(field, LowermostItemTopSideDegree);
        		}
        		else {
        			UppermostItemBottomSideDegree = getCurrentFieldUppermostItemBottomSideDegree(field) + rotateTheta;
        			setCurrentFieldUppermostItemBottomSideDegree(field, UppermostItemBottomSideDegree);
        			
        			LowermostItemTopSideDegree = getCurrentFieldLowermostItemTopSideDegree(field) + rotateTheta;
        			setCurrentFieldLowermostItemTopSideDegree(field, LowermostItemTopSideDegree);
        			if (getCurrentFieldLowermostItemTopSideDegree(field) > FIELD_ITEM_DEGREE) {
        				++timewarp;        				
        				LowermostItemTopSideDegree = getCurrentFieldLowermostItemTopSideDegree(field) - FIELD_ITEM_DEGREE;
        				setCurrentFieldLowermostItemTopSideDegree(field, LowermostItemTopSideDegree);
        			}
        		}
        		setFieldCalendar(field, timewarp);
    		}
    	}
    	else {   // finger move downward : up -> down 
    		if (mustBeInterpolation) { // finger move upward : down -> up ��쿡�� ��Ȯ�ϰ� �����Ѵ�, �׷��� �� �ݴ� ��Ȳ������ ��Ȯ�ϰ� �������� �ʴ´�            			
    			mustBeInterpolate(field);
    			setFieldCalendar(field, -1); // 0���� �����ϴ� ������ DAY_OF_MONTH �ʵ��� ADJUSTMENT ���궧���̴�.
    			                                // ���� 2nd para�� -1���� ���⼺���� ���Ǿ �̻��� �Ǿ���...
    			                                // ��, down -> up���� �νĵǾ� ADJUSTMENT�� �����...
        	}
    		else {
        		scalarABS = Math.abs(scalar);        		
        		rotateTheta = (scalarABS / mTimePickerDateFieldCircleDiameterPixels) * 360;       		
        		
        		double lowerMostItemRotateDegree = getCurrentFieldLowermostItemTopSideDegree(field) - rotateTheta;
        		if ( lowerMostItemRotateDegree < TIMEPICKER_ITEM_AVAILABLE_MIN_DEGREE ) {  	        			
        			++timewarp;	        			
        			double exceedFromDateFieldItemMinDegree = Math.abs(lowerMostItemRotateDegree);
        			timewarp = timewarp + (int)(exceedFromDateFieldItemMinDegree / FIELD_ITEM_DEGREE);        			
        			LowermostItemTopSideDegree = FIELD_ITEM_DEGREE - (exceedFromDateFieldItemMinDegree % FIELD_ITEM_DEGREE);
        			setCurrentFieldLowermostItemTopSideDegree(field, LowermostItemTopSideDegree);        			
        			
        			UppermostItemBottomSideDegree = (TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE) + LowermostItemTopSideDegree; 
        			setCurrentFieldUppermostItemBottomSideDegree(field, UppermostItemBottomSideDegree);
        		}
        		else {       			 
        			setCurrentFieldLowermostItemTopSideDegree(field, lowerMostItemRotateDegree);        			
        			
        			UppermostItemBottomSideDegree = getCurrentFieldUppermostItemBottomSideDegree(field) - rotateTheta;
        			setCurrentFieldUppermostItemBottomSideDegree(field, UppermostItemBottomSideDegree);
        			
        			double upperMostItemBottomDegree = FIELD_ITEM_DEGREE * (TIMEPICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT - 1);        			
        			if (UppermostItemBottomSideDegree < upperMostItemBottomDegree) {	
        				//++timewarp;   <-- ������Ű�� �ȵȴ�!!!		
        				double AdjustUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - 
        						                                     (upperMostItemBottomDegree - UppermostItemBottomSideDegree);
        				setCurrentFieldUppermostItemBottomSideDegree(field, AdjustUppermostItemBottomSideDegree);
        				// LowermostItemTopSideDegree �� �����ؾ� �Ѵ�
        				double AdjustLowermostItemTopSideDegree = AdjustUppermostItemBottomSideDegree - (TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE);
        				setCurrentFieldLowermostItemTopSideDegree(field, AdjustLowermostItemTopSideDegree); 
        			}
        		}
        		setFieldCalendar(field, -timewarp);
    		}
    	}   	
    	
    	// �� �κ��� �߿� ����Ʈ��.
    	// Ŭ���� �Ͽ��ٴ� ���̴�...
    	Calendar calendar = cloneFiledCalendar(field);
    	if (calendar == null)
    		return null;    	
    	
    	ArrayList<ArrayList<FieldObjectVertices>> FieldRowObjectsVerticesList = buildVerticesTexturePosDataOfField(field, calendar);
    	
    	if (field == HOUR_FIELD_TEXTURE) {
    		int afterAMPM = mHourFieldCalendar.get(Calendar.AM_PM);
	    	if (prvAMPM != afterAMPM) {
	    		Log.i("tag", "prvAMPM != afterAMPM");
	    		Handler handler = new Handler(this.getLooper(), this);	
	    		TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(MERIDIEM_FIELD_TEXTURE, TimePickerUserActionMessage.TIMEPICKER_STATUS_CHANGE_MSG, AUTOSCROLLINGBYHOURFIELD);
	    		Message msg = Message.obtain();
	    		msg.obj = gestureMsg;
	    		handler.sendMessage(msg);				
	    	}
    	}
    	
    	return FieldRowObjectsVerticesList;
    }
		
	
    public ArrayList<ArrayList<FieldObjectVertices>> updateMeridiemFieldVerticesTexturesDatas(float scalar) {        	
    	float scalarABS = 0;
    	double rotateTheta = 0;        	
    	        	
    	if (scalar < 0) {  // finger move upward        		
    		scalarABS = Math.abs(scalar);        		
    		rotateTheta = (scalarABS / mTimePickerDateFieldCircleDiameterPixels) * 360;        		
    		
    		double upperMostItemRotateDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree + rotateTheta;
    		if ( upperMostItemRotateDegree >  (TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE) ) { 
    			        			
    			mCurrentMeridiemFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;        			
    			mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree;
    		}
    		else {
    			mCurrentMeridiemFieldUppermostItemBottomSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree + rotateTheta;        			
    			mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldUppermostItemBottomSideDegree;
    		}        		
    	}
    	else {   // finger move downward        		
    		scalarABS = Math.abs(scalar);        		
    		rotateTheta = (scalarABS / mTimePickerDateFieldCircleDiameterPixels) * 360;
    		
    		double lowerMostItemRotateDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - rotateTheta;
    		//Log.i("tag", "lowerMostItemRotateDegree=" + String.valueOf(lowerMostItemRotateDegree));
    		
    		if ( lowerMostItemRotateDegree < TIMEPICKER_ITEM_AVAILABLE_MIN_DEGREE + FIELD_ITEM_DEGREE ) {  
    			mCurrentMeridiemFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;       			 
    			mCurrentMeridiemFieldUppermostItemBottomSideDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree;
    		}
    		else {
    			mCurrentMeridiemFieldLowermostItemTopSideDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree - rotateTheta;        			 
    			mCurrentMeridiemFieldUppermostItemBottomSideDegree = mCurrentMeridiemFieldLowermostItemTopSideDegree;
    		}        		
    	}        	
    	
    	// �Ʒ� �Լ��� ������ ������ �ʿ��� �����͸� ��������..
    	// �������� ���� �׼����� �����ν� 
    	//Calendar calendar = (Calendar)mMeridiemFieldCalendar.clone();
    	Calendar calendar = (Calendar)mHourFieldCalendar.clone();
    	ArrayList<ArrayList<FieldObjectVertices>> MeridiemFieldRowObjectsVerticesList = 
    			buildVerticesTexturePosDataOfMeridiemFields(mCurrentMeridiemFieldLowermostItemTopSideDegree, calendar);
    	return MeridiemFieldRowObjectsVerticesList;
    }
    
    HighLightFieldObjectVertices mCenterEraseBackgroundData;
	private void buildVerticesTexturePosDataOfCenterEraseBackground() {
		float leftmost = -1;
		float rightmost = 1;
		float topmost = 0.0f + mCenterItemHalfHeightRatio;
		float bottommost = 0.0f - mCenterItemHalfHeightRatio;
		float textureTop = 0.0f;
		float textureBottom = 1.0f;
		mCenterEraseBackgroundData = new HighLightFieldObjectVertices(mEraseCenterBackgroundTextureDataHandle,
                													  leftmost, rightmost, 																			          
                													  topmost, bottommost,
                													  textureTop, textureBottom);	
	}
	
	private void onDrawFrame() {    		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		    		
        GLES20.glUseProgram(mProgram);
        
        // Set program handles for drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");                
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, (float) mCircle_center_z); 
        Matrix.setIdentityM(mMVPMatrix, 0);
        
        drawTimePicker();        
	}   
	
	public void drawTimePicker() {
		
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);         
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		useAlphaChannel(true);
        int rowNumber = mDateFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mDateFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		FieldObjectVertices obj = ObjList.get(j);            		
        		bindTexture(obj.mTextureHandle);
        		drawArraysTexture(obj.mFbPositions, mFbFieldTextureCoordinates, obj.mPointNumbers);           		
        	}            	
        } 
        
        
        rowNumber = mMeridiemFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mMeridiemFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		FieldObjectVertices obj = ObjList.get(j);            		
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysTexture(obj.mFbPositions, mFbFieldTextureCoordinates, obj.mPointNumbers);            		
        	}            	
        } 
        
        rowNumber = mHourFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mHourFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		FieldObjectVertices obj = ObjList.get(j);            		
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysTexture(obj.mFbPositions, mFbFieldTextureCoordinates, obj.mPointNumbers);            		
        	}            	
        } 
        
        rowNumber = mMinuteFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mMinuteFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		FieldObjectVertices obj = ObjList.get(j);            		
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysTexture(obj.mFbPositions, mFbFieldTextureCoordinates, obj.mPointNumbers);           		
        	}            	
        }
        
        useAlphaChannel(false);
        GLES20.glDisable(GLES20.GL_BLEND);
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        // center ������
        // 1.viewport�� ���� ����
        // 2.fov�� ���� ����
        // *background�� ����� ����...
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, (float)mCircle_HighLight_center_z); 
        Matrix.setIdentityM(mMVPMatrix, 0);
        //GLES20.glDisable(GLES20.GL_DEPTH_FUNC);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        bindTexture(mCenterEraseBackgroundData.mTextureHandle);
        drawArraysElements(mCenterEraseBackgroundData.mFbPositions, mCenterEraseBackgroundData.mFbFieldTextureCoordinates, mCenterEraseBackgroundData.mFbIndicess);
        
        int ObjlistNumbers = mDateHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mDateHighLightFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		HighLightFieldObjectVertices obj = ObjList.get(j);
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysElements(obj.mFbPositions, obj.mFbFieldTextureCoordinates, obj.mFbIndicess);
        	}            	
        }            
        
        ObjlistNumbers = mMeridiemHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mMeridiemHighLightFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		HighLightFieldObjectVertices obj = ObjList.get(j);
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysElements(obj.mFbPositions, obj.mFbFieldTextureCoordinates, obj.mFbIndicess);
        	}            	
        }  
        
        ObjlistNumbers = mHourHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mHourHighLightFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		HighLightFieldObjectVertices obj = ObjList.get(j);
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysElements(obj.mFbPositions, obj.mFbFieldTextureCoordinates, obj.mFbIndicess);
        	}            	
        } 
        
        ObjlistNumbers = mMinuteHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mMinuteHighLightFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		HighLightFieldObjectVertices obj = ObjList.get(j);
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysElements(obj.mFbPositions, obj.mFbFieldTextureCoordinates, obj.mFbIndicess);
        	}            	
        }         
	}	
	
	
	private void bindTexture(int textureDataHandle) {    		   
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureDataHandle);                    
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0); 
	}
	
	/*
	private void unbindTexture(int textureId) {
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		
		int[] textureIds = { textureId };
		GLES20.glDeleteTextures(1, textureIds, 0); 
	}
	*/
	
	private void drawArraysTexture(FloatBuffer verticlesFB, FloatBuffer textureCoordinatesFB, int pointNumbers) {
		// Pass in the position information
		verticlesFB.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle,
        							 mDataSizePerVertex,
        							 GLES20.GL_FLOAT,
        							 false,
        							 0,
        							 verticlesFB);        
        checkGlError();        
        GLES20.glEnableVertexAttribArray(mPositionHandle);              
        checkGlError(); 
        
        // Pass in the texture coordinate information
        textureCoordinatesFB.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
        						     mTextureCoordinateDataSize,
        						     GLES20.GL_FLOAT,
        						     false,
        						     0,
        						     textureCoordinatesFB);
        checkGlError();
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        checkGlError();
        
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);           
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);        
        checkGlError();
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, pointNumbers);  
        checkGlError();
        
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
	}
	
	
	private void drawArraysElements(FloatBuffer verticlesFB, FloatBuffer textureCoordinatesFB, ByteBuffer indicesFB) {
		// Pass in the position information
		verticlesFB.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle,
        							 mDataSizePerVertex,
        							 GLES20.GL_FLOAT,
        							 false,
        							 0,
        							 verticlesFB);        
        checkGlError();        
        GLES20.glEnableVertexAttribArray(mPositionHandle);              
        checkGlError();        
        // Pass in the texture coordinate information
        textureCoordinatesFB.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
        						     mTextureCoordinateDataSize,
        						     GLES20.GL_FLOAT,
        						     false,
        						     0,
        						     textureCoordinatesFB);
        checkGlError();
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        checkGlError();
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);           
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);        
        checkGlError();
        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_BYTE, indicesFB);
        checkGlError();
        
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
	}       
   
    // ���� ��Ȳ������ requestStop�� ����� �ʿ�� ����
    public void requestStop() {
    	try {
        	//Blocks the current Thread (Thread.currentThread()) 
        	//until the receiver finishes its execution and dies.
        	join();
        } catch (InterruptedException e) {
            Log.e("GL", "failed to stop gl thread", e);
        }
        
        cleanupEgl();
    }

    public void setFlingFactor(gestureFlingMovementVector flingVector, float velocityY) {
		int ppi = (int)m_Ppi;
		flingVector.resetFlingMovementVector();
		
		flingVector.m_fling_velocity_axis_y = velocityY;
		flingVector.m_scroller = new KScroller(ppi);
		int initialVelocity = (int)velocityY;
		int initialY = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
		flingVector.m_LastFlingY = initialY;
		flingVector.m_scroller.fling(0, initialY, 
						             0, initialVelocity,
						             0, Integer.MAX_VALUE, 
						             0, Integer.MAX_VALUE);    		
	}
            
    
	public boolean computeYFlingScalar(gestureFlingMovementVector flingVector) {
		// x, y�� ��ġ�� ���� ������ �߿����� �ʴ�
		// ����, scalar�� �߿��� ���̴�
		boolean more = flingVector.m_scroller.computeScrollOffset();
        final int y = flingVector.m_scroller.getCurrY();
        //int delta = m_flingmovementVector.m_LastFlingY - y;
        int delta = y - flingVector.m_LastFlingY;        
        
        if (more) {
        	flingVector.m_LastFlingY = y;
        	
        } else {        	
        	flingVector.m_scroller.forceFinished(true);        	
        }        
		
        flingVector.setMoveYScalar(delta);
		
		return more;
	}
	
	public boolean computeYFlingScalar(int field/*gestureFlingMovementVector flingVector*/) {
		gestureFlingMovementVector flingVector = null;
		
		switch (field) {
		case DATE_FIELD_TEXTURE:            	
			flingVector = m_dateFieldFlingmovementVector;     	
			break;
		case MERIDIEM_FIELD_TEXTURE:
			
			break;
		case HOUR_FIELD_TEXTURE:
			flingVector = m_hourFieldFlingmovementVector;
			break;
		case MINUTE_FIELD_TEXTURE:
			flingVector = m_minuteFieldFlingmovementVector;
			break;		
		default:
			return false;

		}
		
		boolean more = flingVector.m_scroller.computeScrollOffset();
        final int y = flingVector.m_scroller.getCurrY();
        //int delta = m_flingmovementVector.m_LastFlingY - y;
        int delta = y - flingVector.m_LastFlingY;        
        
        if (more) {
        	flingVector.m_LastFlingY = y;
        	
        } else {        	
        	flingVector.m_scroller.forceFinished(true);        	
        }        
		
        flingVector.setMoveYScalar(delta);
		
		return more;
	}
	
	public void stopComputeYFlingScalar(gestureFlingMovementVector flingVector) {    		
		flingVector.m_scroller.forceFinished(true);   		
	}
	
	public void interruptedComputeYFlingScalar(gestureFlingMovementVector flingVector) {
		boolean more = flingVector.m_scroller.computeScrollOffset();
		if (more)
			stopComputeYFlingScalar(flingVector);		
	}
	
	public void interruptedComputeYFlingScalar(int field) {
		gestureFlingMovementVector flingVector = null;
		
		switch (field) {
		case DATE_FIELD_TEXTURE:            	
			flingVector = m_dateFieldFlingmovementVector;     	
			break;
		case MERIDIEM_FIELD_TEXTURE:
			
			break;
		case HOUR_FIELD_TEXTURE:
			flingVector = m_hourFieldFlingmovementVector;
			break;
		case MINUTE_FIELD_TEXTURE:
			flingVector = m_minuteFieldFlingmovementVector;
			break;		
		default:
			return ;
		}
		
		boolean more = flingVector.m_scroller.computeScrollOffset();
		if (more)
			stopComputeYFlingScalar(flingVector);		
	}
	
	public float getYFlingScalar(gestureFlingMovementVector flingVector) {
		return flingVector.getMoveYScalar();
	}
	
	public float getYFlingScalar(int field/*gestureFlingMovementVector flingVector*/) {
		gestureFlingMovementVector flingVector = null;
		
		switch (field) {
		case DATE_FIELD_TEXTURE:            	
			flingVector = m_dateFieldFlingmovementVector;     	
			break;
		case MERIDIEM_FIELD_TEXTURE:
			
			break;
		case HOUR_FIELD_TEXTURE:
			flingVector = m_hourFieldFlingmovementVector;
			break;
		case MINUTE_FIELD_TEXTURE:
			flingVector = m_minuteFieldFlingmovementVector;
			break;		
		default:
			return 0;
		}
		
		return flingVector.getMoveYScalar();
	}
	
	public float getYFlingVelocity(int field) {
		float velocity = 0;
		
		switch (field) {
		case DATE_FIELD_TEXTURE:            	
			velocity = m_dateFieldFlingmovementVector.m_fling_velocity_axis_y;     	
			break;
		case MERIDIEM_FIELD_TEXTURE:
			
			break;
		case HOUR_FIELD_TEXTURE:
			velocity = m_hourFieldFlingmovementVector.m_fling_velocity_axis_y;
			break;
		case MINUTE_FIELD_TEXTURE:
			velocity = m_minuteFieldFlingmovementVector.m_fling_velocity_axis_y;
			break;		
		default:
			break;
		}
		
		return velocity;
	}
	
    private void initGL() {            
        mEgl = (EGL10) EGLContext.getEGL();

        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed "
                    + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        int[] version = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed "
                    + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_SAMPLE_BUFFERS, 0 /* true */,
                //EGL10.EGL_SAMPLES, 4, // 4x anti-aliasing
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException(
                    "eglChooseConfig failed "
                            + GLUtils.getEGLErrorString(mEgl
                                    .eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {
                EGL_CONTEXT_CLIENT_VERSION, 2, 
                EGL10.EGL_NONE
        };
        mEglContext = mEgl.eglCreateContext(mEglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        checkEglError();
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, eglConfig, mSurface, null);
        checkEglError();
        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG,
                        "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException(
                    "eglCreateWindowSurface failed "
                            + GLUtils.getEGLErrorString(error));
        }

        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed "
                    + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        checkEglError();

        mGl = (GL11) mEglContext.getGL();
        checkEglError();

        mProgram = buildProgram();
        m_isChangedAlphaHandle = GLES20.glGetUniformLocation(mProgram, "u_isChangedAlpha");
        //m_ChangedAlphaValueHandle = GLES20.glGetUniformLocation(mProgram, "u_ChangedAlphaValue");
        
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		
		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);		
		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);		
		// Enable texture mapping
		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		
		GLES20.glViewport(0, 0, m_Width, m_Height);
        
		mNumeralTextTextureDataHandles = new int[10];
        for (int i=0; i<10; i++) {
        	Bitmap numeralTextBitmap = mTimePickerNumeralTextBitmapsList.get(i);
        	mNumeralTextTextureDataHandles[i] = loadTexture(numeralTextBitmap); 
        	setMipmapFilters(mNumeralTextTextureDataHandles[i]);
        }
                  
        mDayOfWeekTextureDataHandles = new int[7];
        for (int i=0; i<7; i++) {
        	Bitmap dayOfWeekTextBitmap = mTimePickerDayOfWeekTextBitmapsList.get(i);
        	mDayOfWeekTextureDataHandles[i] = loadTexture(dayOfWeekTextBitmap); 
        	setMipmapFilters(mDayOfWeekTextureDataHandles[i]);
        }
        
        mTodayTextTextureDataHandles = new int[2];
        for (int i=0; i<2; i++) {
        	Bitmap todayTextBitmap = mTimePickerTodayTextBitmapsList.get(i);
        	mTodayTextTextureDataHandles[i] = loadTexture(todayTextBitmap); 
        	setMipmapFilters(mTodayTextTextureDataHandles[i]);
        }
        
        mMeridiemTextTextureDataHandles = new int[3];
        for (int i=0; i<3; i++) {
        	Bitmap meridiemTextBitmap = mTimePickerMeridiemTextBitmapsList.get(i);
        	mMeridiemTextTextureDataHandles[i] = loadTexture(meridiemTextBitmap);  
        	setMipmapFilters(mMeridiemTextTextureDataHandles[i]);
        }
                
        mMonthTextTextTextureDataHandle = loadTexture(mTimePickerMonthTextBitmap);  
        setMipmapFilters(mMonthTextTextTextureDataHandle);
        mDateTextTextTextureDataHandle = loadTexture(mTimePickerDateTextBitmap);  
        setMipmapFilters(mDateTextTextTextureDataHandle);
                  
        mCenterNumeralTextTextureDataHandles = new int[10];
        for (int i=0; i<10; i++) {
        	Bitmap numeralTextBitmap = mCenterTimePickerNumeralBitmapsList.get(i);
        	mCenterNumeralTextTextureDataHandles[i] = loadTexture(numeralTextBitmap);  
        	setMipmapFilters(mCenterNumeralTextTextureDataHandles[i]);
        }
                  
        mCenterDayOfWeekTextureDataHandles = new int[7];
        for (int i=0; i<7; i++) {
        	Bitmap dayOfWeekTextBitmap = mTimePickerCenterDayOfWeekTextBitmapsList.get(i);
        	mCenterDayOfWeekTextureDataHandles[i] = loadTexture(dayOfWeekTextBitmap);
        	setMipmapFilters(mCenterDayOfWeekTextureDataHandles[i]);
        }
        
        mCenterTodayTextTextureDataHandles = new int[2];
        for (int i=0; i<2; i++) {
        	Bitmap todayTextBitmap = mTimePickerCenterTodayTextBitmapsList.get(i);
        	mCenterTodayTextTextureDataHandles[i] = loadTexture(todayTextBitmap);  
        	setMipmapFilters(mCenterTodayTextTextureDataHandles[i]);
        }
        
        mCenterMeridiemTextTextureDataHandles = new int[3];
        for (int i=0; i<3; i++) {
        	Bitmap meridiemTextBitmap = mTimePickerCenterMeridiemTextBitmapsList.get(i);
        	mCenterMeridiemTextTextureDataHandles[i] = loadTexture(meridiemTextBitmap);  
        	setMipmapFilters(mCenterMeridiemTextTextureDataHandles[i]);
        }        
        
        
        mCenterMonthTextTextTextureDataHandle = loadTexture(mTimePickerCenterMonthTextBitmap);  
        setMipmapFilters(mCenterMonthTextTextTextureDataHandle);
        mCenterDateTextTextTextureDataHandle = loadTexture(mTimePickerCenterDateTextBitmap);  
        setMipmapFilters(mCenterDateTextTextTextureDataHandle);
        
        mEraseCenterBackgroundTextureDataHandle = loadTexture(mEraseBackgroundBitmap);    
        setMipmapFilters(mEraseCenterBackgroundTextureDataHandle);
        ///////////////////////////////////////////////////////////////////////////////////////////////////        
        Calendar DateCalendar = (Calendar)mDateFieldCalendar.clone();        
        Calendar HourCalendar = (Calendar)mHourFieldCalendar.clone();
        Calendar MinuteCalendar = (Calendar)mMinuteFieldCalendar.clone();        
        
        mDateFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfDateFields(mCurrentDateFieldLowermostItemTopSideDegree, DateCalendar);        
        mMeridiemFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMeridiemFields(mCurrentMeridiemFieldLowermostItemTopSideDegree, HourCalendar);
        mHourFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfHourFields(mCurrentHourFieldLowermostItemTopSideDegree, HourCalendar);        
        mMinuteFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMinuteFields(mCurrentMinuteFieldLowermostItemTopSideDegree, MinuteCalendar);
        
        mDateHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfDateFieldsHighLight();		
        mMeridiemHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMeridiemFieldsHighLight();
        mHourHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfHourFieldsHighLight();
        mMinuteHighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfMinuteFieldsHighLight();       
        
        // ���⼭ center eraseBackground �����͸� ��������
        buildVerticesTexturePosDataOfCenterEraseBackground();
    }   
    
    
    
    public void deInitGL() {
    	Log.i("tag", "+TimePickerRender : deInitGL");
    	mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
    	mEgl.eglDestroyContext(mEglDisplay, mEglContext);
    	mEgl.eglTerminate(mEglDisplay);
    	
    }
    
    public void useAlphaChannel(boolean isUse) {
		if (isUse) {
			GLES20.glUniform1i(m_isChangedAlphaHandle, 1);    			
		}
		else
			GLES20.glUniform1i(m_isChangedAlphaHandle, 0);		
	}
    
    private void cleanupEgl() {
    	mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
    	mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
    	mEgl.eglDestroyContext(mEglDisplay, mEglContext);
    	mEgl.eglTerminate(mEglDisplay);
        Log.i("GL", "GL Cleaned up");
    }
    
    private int buildProgram() {
    	final String vertexShader = getVertexShader();   
    	//final String vertexShader = getVertexShader2();
 		final String fragmentShader = getFragmentShader();			
		
		final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
		final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
		
		final int program = createAndLinkProgram(vertexShaderHandle,
				   								 fragmentShaderHandle,
				   								 new String[] {"a_Position", "a_TexCoordinate"});	
        if (program == 0) {
            return 0;
        }    	
        
        
        return program;
    }
    
    private String getVertexShader()
	{
		float[] yAxisDateField = new float[5];
    	
    	for (int i=0; i<5; i++) {
    		double degree = i * FIELD_ITEM_DEGREE;     		
    		yAxisDateField[i] = (float)( Math.cos( Math.toRadians(degree) ) * TIMEPICKER_FIELD_CIRCLE_RADIUS );    			
    	}        	        	
    	
    	final String vertexShader =   
    		  "uniform mat4 u_MVPMatrix;      				               	\n"		// A constant representing the combined model/view/projection matrix.        			
    		  + "attribute vec4 a_Position;     							\n"		// Per-vertex position information we will pass in.
    		  + "attribute vec2 a_TexCoordinate;       						\n"		// Per-vertex normal information we will pass in.
    		  + "varying vec2 v_TexCoordinate;       						\n"
    		  + "varying float alphaValue;       							\n"        		  
    		  + "float yAxis[5];	                             			\n" 	  
    		  + "uniform bool u_isChangedAlpha;								\n"
    		  
    		  + "void setAlphaValue()                           			\n"        					
    		  + "{       													\n"
    		  + "   yAxis[0] =" + String.valueOf(yAxisDateField[0]) + ";   	\n"  
    		  + "	yAxis[1] =" + String.valueOf(yAxisDateField[1]) + ";   	\n" 
    		  + "	yAxis[2] =" + String.valueOf(yAxisDateField[2]) + ";   	\n"  
    		  + "	yAxis[3] =" + String.valueOf(yAxisDateField[3]) + ";   	\n"  
    		  + "	yAxis[4] =" + String.valueOf(yAxisDateField[4]) + ";   	\n"  
    		  	   										
    		  + "	alphaValue = 1.0; 										\n"                          
    		  + "	float y = abs(a_Position.y);							\n"       					
    		  + "	if (y > yAxis[2])										\n"						
    		  + "		alphaValue = 0.8 - y;								\n"       			
    		  + "    else if (y > yAxis[3])									\n"					
    		  + "    	alphaValue = 1.0 - y; 								\n"      			
    		  + "}     														\n"  				

    		  // The entry point for our vertex shader.  
    		  + "void main()            									\n"                                            	
    		  + "{               											\n"
    		  + "	if(u_isChangedAlpha)									\n"
    		  + "		setAlphaValue();        							\n"
    		 
    		  	// Pass through the texture coordinate.
    		  + "	v_TexCoordinate = a_TexCoordinate;       				\n"
    		  	
    		  // gl_Position is a special variable used to store the final position.
    		  // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    		  + "	gl_Position = u_MVPMatrix * a_Position;     			\n"                     		  
    		                  
    		  + "}                                              			\n"; 
    	
    	return vertexShader;    	
	}    
    
    
    private String getFragmentShader()
	{
		return readTextFileFromRawResource(mActivity, R.raw.timepicker_per_fragment_shader);
	}
	
    private String readTextFileFromRawResource(final Context context,
			final int resourceId)
	{
		final InputStream inputStream = context.getResources().openRawResource(
				resourceId);
		final InputStreamReader inputStreamReader = new InputStreamReader(
				inputStream);
		final BufferedReader bufferedReader = new BufferedReader(
				inputStreamReader);

		String nextLine;
		final StringBuilder body = new StringBuilder();

		try
		{
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				body.append(nextLine);
				body.append('\n');
			}
		}
		catch (IOException e)
		{
			return null;
		}

		return body.toString();
	}
	    	
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) 
	{
		int programHandle = GLES20.glCreateProgram();
		
		if (programHandle != 0) 
		{
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);			

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);
			
			// Bind attributes
			if (attributes != null)
			{
				final int size = attributes.length;
				for (int i = 0; i < size; i++)
				{
					GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
				}						
			}
			
			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) 
			{				
				Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}
		
		if (programHandle == 0)
		{
			throw new RuntimeException("Error creating program.");
		}
		
		return programHandle;
	}
	
	
    private int compileShader(final int shaderType, final String shaderSource) 
	{
		int shaderHandle = GLES20.glCreateShader(shaderType);

		if (shaderHandle != 0) 
		{
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle);

			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) 
			{
				Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}

		if (shaderHandle == 0)
		{			
			throw new RuntimeException("Error creating shader.");
		}
		
		return shaderHandle;
	}
	
    private int loadTexture(Bitmap bitmap)
	{
		final int[] textureHandle = new int[1];
		
		GLES20.glGenTextures(1, textureHandle, 0);
		
		if (textureHandle[0] != 0)
		{
			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
			
			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			
			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);    			
		}
		
		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error loading texture.");
		}
		
		return textureHandle[0];
	}
	
    public void setFilters(int minFilter, int magFilter) {		
    	GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				minFilter);
    	GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				magFilter);
	}
    
    public void setMipmapFilters(int textureHandle) {
    	GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    	
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);		
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
    }
    
    private int loadTexture(Bitmap bitmap, boolean mipmapped)
	{
    	int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		Bitmap source = Bitmap.createBitmap(bitmap, 0, 0, width, height);
		
		final int[] textureHandle = new int[1];
		
		GLES20.glGenTextures(1, textureHandle, 0);
		
		if (textureHandle[0] != 0)
		{
			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
			setFilters(GLES20.GL_LINEAR_MIPMAP_NEAREST, GLES20.GL_LINEAR);
			
			int level = 0;
			int newWidth = width;
			int newHeight = height;
			while (true) {
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, source, 0);
				newWidth = newWidth / 2;
				newHeight = newHeight / 2;
				if ( (newWidth <= 0) || (newHeight <= 0) )
					break;
				

				Bitmap newBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
				source.recycle();
				source = newBitmap;
				level++;
				
				if (level > 2)
					break;
			}	
			
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			source.recycle();
		}
		
		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error loading texture.");
		}
		
		return textureHandle[0];
	}
    //GLSurfaceView �� �ִ� ����
    //  * Note that when the EGL context is lost, all OpenGL resources associated
    //  * with that context will be automatically deleted. You do not need to call
    //  * the corresponding "glDelete" methods such as glDeleteTextures to
    //  * manually delete these lost resources.
    public void deleteTextures() {    	
    	Log.i("tag", "TimePickerRender : deleteTextures");
    	
    	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    	
    	for (int i=0; i<10; i++) {    		
    		dispose(mNumeralTextTextureDataHandles[i]);
    	}    	
    	for (int i=0; i<7; i++) {    		
    		dispose(mDayOfWeekTextureDataHandles[i]);
    	}    	
    	for (int i=0; i<2; i++) {    		
    		dispose(mTodayTextTextureDataHandles[i]);
    	}    	
    	for (int i=0; i<3; i++) {    		
    		dispose(mMeridiemTextTextureDataHandles[i]);
    	}    	
    	
		dispose(mMonthTextTextTextureDataHandle);    	
		
		dispose(mDateTextTextTextureDataHandle);
		
    	
    	for (int i=0; i<10; i++) {    		
    		dispose(mCenterNumeralTextTextureDataHandles[i]);
    	}    	
    	for (int i=0; i<7; i++) {    		
    		dispose(mCenterDayOfWeekTextureDataHandles[i]);
    	}    	
    	for (int i=0; i<2; i++) {    		
    		dispose(mCenterTodayTextTextureDataHandles[i]);
    	}    	
    	for (int i=0; i<3; i++) {    		
    		dispose(mCenterMeridiemTextTextureDataHandles[i]);
    	}
    	    	
    	dispose(mCenterMonthTextTextTextureDataHandle);    	
    	
    	dispose(mCenterDateTextTextTextureDataHandle);		
    	
    	dispose(mEraseCenterBackgroundTextureDataHandle);
    }
    
    
    public void dispose(int textureId) {	
    	//GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
		int[] textureIds = { textureId };
		//Log.i("tag", "delete texture id=" + String.valueOf(textureIds[0]));
		GLES20.glDeleteTextures(1, textureIds, 0);
	}
    
	private void checkCurrent() {
        if (!mEglContext.equals(mEgl.eglGetCurrentContext()) || 
        		!mEglSurface.equals(mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW))) {
        	
            checkEglError();
            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                throw new RuntimeException(
                        "eglMakeCurrent failed "
                                + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            
            checkEglError();
        }
    }

    private void checkEglError() {
        final int error = mEgl.eglGetError();
        if (error != EGL10.EGL_SUCCESS) {
            Log.e(TAG, "EGL error = 0x" + Integer.toHexString(error));
        }
    }

    private void checkGlError() {
        final int error = mGl.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Log.e(TAG, "GL error = 0x" + Integer.toHexString(error));
        }
    } 	
    
    int mScrollUpdateField = 0;
    float mRightDateFieldOfTimePicker;	
	float mLeftMeridiemFieldOfTimePicker;	
	float mRightMeridiemFieldOfTimePicker;	
	float mLeftHourFieldOfTimePicker;	
	float mRightHourFieldOfTimePicker;	
	LockableScrollView mMainPageScrollView;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerDateField;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerMeridiemField;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerHourField;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerMinuteField;
	
	//float mLeftMinuteFieldOfTimePicker;	
	
    public int getUpdateFieldOnTouch(float PointX) {
    	int updateField = 0;   	
    	
    	if ( PointX < mRightDateFieldOfTimePicker ) {
        	updateField = DATE_FIELD_TEXTURE; 
        }
        else if ( (mLeftMeridiemFieldOfTimePicker <= PointX) && (PointX < mRightMeridiemFieldOfTimePicker) ) {
        	updateField = MERIDIEM_FIELD_TEXTURE;
        }
        else if ( (mLeftHourFieldOfTimePicker <= PointX) && (PointX < mRightHourFieldOfTimePicker) ) {
        	updateField = HOUR_FIELD_TEXTURE;
        }
        else {
        	updateField = MINUTE_FIELD_TEXTURE;
        }  	   	
    	
    	return updateField;
    }
    
    OnTouchListener mTimePickerTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {			
			int action=event.getAction();			
			
			if(action==MotionEvent.ACTION_DOWN) {
				//Log.i("tag", "ACTION_DOWN");
				mMainPageScrollView.setScrollingEnabled(false);//////////////////////////////////////////////////////////////////////////////////////////////////
				// field�� �ĺ��ؾ� �Ѵ�
				int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	            int pointerId = event.getPointerId(pointerIndex);
	            
	            float PointX = event.getX(pointerIndex);
	            int updateField = getUpdateFieldOnTouch(PointX);
	            
	            // �Ʒ� msg�� FLING �Ǵ� AUTOSCROLLING ���¿��� ó���ȴ�
				// :compute thread�� ���� �����Ų��				            
	            TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(updateField, TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG, 0);
	    		Message msg = Message.obtain();
				msg.obj = gestureMsg;
				
				mRenderThreadHandler.sendMessage(msg);
			}
			else if (action==MotionEvent.ACTION_MOVE) {
				//Log.i("tag", "ACTION_MOVE");
				//Log.i("tag", "onTouch:x=" + String.valueOf(event.getX()) + ", y=" + String.valueOf(event.getY()));
				//return false; // onScroll msg�� �߻����� �ʴ´�
			}
			else if (action==MotionEvent.ACTION_UP) {
				//Log.i("tag", "ACTION_UP");
				/*
				 * mScrollUpdateField ������ �������� �ִ�.
				 * �и� fling �����ĸ� ���ߴµ�, ���� ACTION_DOWN/MOVE/UP�� �߻��ϴ� ��찡 �ִ�.
				 * :onScroll�� ���� �߻����� �ʾƼ�,,,������ onScroll���� ������ field ���� mScrollUpdateField�� ���� �־,
				 *  ������ field�� AUTOSCROLLING ������ ����ȴ�
				 */
				mMainPageScrollView.setScrollingEnabled(true);/////////////////////////////////////////////////////////////////////////////////////////////////////
				int updateField = 0;
				if (mLaunchedScroll) { // 
					mLaunchedScroll = false; //!!!!!!!!	
					updateField = mScrollUpdateField;
				}
				else {
					int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		            
		            float PointX = event.getX(pointerIndex);
		            updateField = getUpdateFieldOnTouch(PointX);		           
				}
				
				TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(updateField, TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG, 0);
	    		Message msg = Message.obtain();
				msg.obj = gestureMsg;
				
				mRenderThreadHandler.sendMessage(msg);				
			}
			
			mTimePickerGestureDetector.onTouchEvent(event);				
			
			return true;
		}		
	};
	
	public void gaugeYAxisScrollGestureMovement(gestureScrollMovementVector vector, float y1, float y2) {	
		float cal_y1 = 0;
		float cal_y2 = 0;
						
		if(vector.m_KickoffScroll_y1 != y1) {
			vector.m_KickoffScroll_y1 = (int)y1;
			cal_y1 = y1;
			cal_y2 = y2;
			vector.m_scroll_previous_y = cal_y2;
		}
		else {
			cal_y1 = vector.m_scroll_previous_y;
			cal_y2 = y2;
			vector.m_scroll_previous_y = cal_y2;
		}
		
		//float moveYScalar = cal_y1 - cal_y2;  // fling�� �˰��� ����
		float moveYScalar = cal_y2 - cal_y1;
		vector.setMoveYScalar(moveYScalar);
		
		return;			
	}
	
	public float getUpdateFieldOnScrolling(MotionEvent event1, MotionEvent event2) {
    	float scrollingScalar = 0;
    	float PointX = event2.getX();    		
			
		if ( PointX < mRightDateFieldOfTimePicker ) {
			mScrollUpdateField = TimePickerRender.DATE_FIELD_TEXTURE; 
			m_scrollmovementVectorOfTimePickerDateField.resetScrollMovementVector();
			gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerDateField, event1.getY(), event2.getY());
			scrollingScalar = m_scrollmovementVectorOfTimePickerDateField.getMoveYScalar();
		}
		else if ( (mLeftMeridiemFieldOfTimePicker <= PointX) && (PointX < mRightMeridiemFieldOfTimePicker) ) {
			mScrollUpdateField = TimePickerRender.MERIDIEM_FIELD_TEXTURE;
			m_scrollmovementVectorOfTimePickerMeridiemField.resetScrollMovementVector();
			gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerMeridiemField, event1.getY(), event2.getY());
			scrollingScalar = m_scrollmovementVectorOfTimePickerMeridiemField.getMoveYScalar();
		}
		else if ( (mLeftHourFieldOfTimePicker <= PointX) && (PointX < mRightHourFieldOfTimePicker) ) {
			mScrollUpdateField = TimePickerRender.HOUR_FIELD_TEXTURE;
			m_scrollmovementVectorOfTimePickerHourField.resetScrollMovementVector();
			gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerHourField, event1.getY(), event2.getY());
			scrollingScalar = m_scrollmovementVectorOfTimePickerHourField.getMoveYScalar();
		}
		else {
			mScrollUpdateField = TimePickerRender.MINUTE_FIELD_TEXTURE;
			m_scrollmovementVectorOfTimePickerMinuteField.resetScrollMovementVector();
			gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerMinuteField, event1.getY(), event2.getY());
			scrollingScalar = m_scrollmovementVectorOfTimePickerMinuteField.getMoveYScalar();
		}    	
		
    	return scrollingScalar;
    }
	
	boolean mLaunchedScroll = false;
	GestureDetector.OnGestureListener mTimePickerGesturelistener = new GestureDetector.OnGestureListener() {
    	
    	public boolean onDown(MotionEvent event) {    		
    		return true;
    	}        	
    	
    	public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {      		
    		float scrollingScalar = 0;
    		    		
    		if (!mLaunchedScroll) {    			
    			mLaunchedScroll = true;    			  
    			scrollingScalar = getUpdateFieldOnScrolling(event1, event2);    			
    		}
    		else {
    			gestureScrollMovementVector vector;
    			
    			switch(mScrollUpdateField) {
    			case TimePickerRender.DATE_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerDateField;
    				break;
    			case TimePickerRender.MERIDIEM_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerMeridiemField;
    				break;
    			case TimePickerRender.HOUR_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerHourField;
    				break;
    			case TimePickerRender.MINUTE_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerMinuteField;
    				break;
    			default:
    				return true;
    			}   			
    			
    			gaugeYAxisScrollGestureMovement(vector, event1.getY(), event2.getY());
    			scrollingScalar = vector.getMoveYScalar();
    		}    		    		
    		
    		TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(mScrollUpdateField, TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG, scrollingScalar);
    		Message msg = Message.obtain();
			msg.obj = gestureMsg;			
			
			mRenderThreadHandler.sendMessage(msg);
						
			return true;
    	}
    	
    	public boolean onFling(MotionEvent event1, MotionEvent event2, float speedX, float speedY) {    		
    		float speedY2Times = speedY;
    		TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(mScrollUpdateField, TimePickerUserActionMessage.TIMEPICKER_FLING_MSG, speedY2Times);
    		Message msg = Message.obtain();
			msg.obj = gestureMsg;	
			
			mRenderThreadHandler.sendMessage(msg);
			
    		return true;
    	}

		@Override
		public void onShowPress(MotionEvent e) {				
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {			
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {			
		}    	
    };
    
    
    public void setSurface(SurfaceTexture surface) {
		mSurface = surface;
	}
    
    public void stopRenderThread() {
		Log.i("tag", "+TimePickerRender : stopRenderThread");
		DateFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0);
		MeridiemFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0); 
		HourFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0);
		MinuteFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0);		
		
		mOpenGLESRender.shouldStop();
		
		Log.i("tag", "-TimePickerRender : stopRenderThread");
	}
    
    
    public void sleepRenderThread() {
		Log.i("tag", "+TimePickerRender : sleepRenderThread");
		DateFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0);
		MeridiemFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0); 
		HourFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0);
		MinuteFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0);		
		
		Log.i("tag", "-TimePickerRender : sleepRenderThread");
	}
    
	
	public void wakeUpRenderThread(Calendar time) {
		Log.i("tag", "+TimePickerRender : wakeUpRenderThread");
		resetCalendar(time);
 		
		DateFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0);
		MeridiemFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0); 
		HourFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0);
		MinuteFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0);	
		
		resetField();
		
		mRenderingLock.lock();
		mConditionOfRenderingLock.signalAll();      			
		mRenderingLock.unlock();
		Thread.yield();
		
		Log.i("tag", "-TimePickerRender : wakeUpRenderThread");
	}	
	
	
	public Calendar getTimeBeforeSleep() {
		Calendar curCal = (Calendar)mDateFieldCalendar.clone();		
		return curCal;
	}
	
	public OnTouchListener getOnTouchListener() {
    	return mTimePickerTouchListener;
    }
	
	public void reStartRenderThread() {
		
	}
}
