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
import java.util.GregorianCalendar;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL11;

import com.intheeast.easing.Linear;
import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;
import com.intheeast.event.EditEventRepeatEndSettingSubView.RepeatEndTimeInfo;
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
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class RepeatEndTimePickerRender extends TimePickerRenderThread implements Callback {

	private static final String TAG = "RepeatEndTimePickerRender";
	private static boolean INFO = true;
	
	public static final String REPEATEND_TIME_PICKER_NAME = "RepeatEndTimePickerRenderer";
	
	public static final int START_OPENGLES_RENDER_THREAD = 0;
	public static final int START_TIME_PICKER_ID = 1;
	public static final int END_TIME_PICKER_ID = 2;
	
	
	public static final int YEAR_FIELD_TEXTURE = 5;
	public static final int MONTH_FIELD_TEXTURE = 6;
	public static final int DAYOFMONTH_FIELD_TEXTURE = 7;	
	public static final int ALL_FIELD_TEXTURE = 8;
    
    public static final int UPDATE_YEAR_FIELD_OF_TIMEPICKER = 5;
    public static final int UPDATE_MONTH_FIELD_OF_TIMEPICKER = 6;
    public static final int UPDATE_DAYOFMONTH_FIELD_OF_TIMEPICKER = 7;
    
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
    //private static final int AUTOSCROLLINGBYHOURFIELD = 4;
    private static final int SLEEP = 5;
    private static final int FIELD_ADJUSTMENT_BY_EXCEED_MIX = 6;
    private static final int DAYOFMONTH_FIELD_ADJUSTMENT = 7;
    
    private static final int EXIT = -1;
    
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    
    
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
	int mYearTextTextTextureDataHandle;
	int mMonthTextTextTextureDataHandle;
	int mDateTextTextTextureDataHandle;   
	
	int[] mCenterNumeralTextTextureDataHandles;
	int mCenterYearTextTextTextureDataHandle;
	int mCenterMonthTextTextTextureDataHandle;
	int mCenterDateTextTextTextureDataHandle; 
	
	int mEraseCenterBackgroundTextureDataHandle;
	
	private static final int mBytesPerFloat = 4;	
	private static final int mDataSizePerVertex = 3;	
	private static final int mTextureCoordinateDataSize = 2;  	
	private static final int VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS = 6; 
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
	
	//Calendar mCalendar;	
	int mCurrentGradationOfDayOfMonthMiniTimePicker;
	boolean mAdjustmentDayOfMonthField = false;	
    
    double mCurrentYearFieldUppermostItemBottomSideDegree;
    double mCurrentYearFieldLowermostItemTopSideDegree;
    
    double mCurrentMonthFieldUppermostItemBottomSideDegree;
    double mCurrentMonthFieldLowermostItemTopSideDegree;
    
    double mCurrentDayOfMonthFieldUppermostItemBottomSideDegree;
    double mCurrentDayOfMonthFieldLowermostItemTopSideDegree;    
    
	float m_yearFieldRightMostVPRattio;
    float m_monthFieldPickerLeftMostVPRatio;
	float m_dayOfMonthFieldPickerLeftMostVPRatio;	
	
	float mTimePickerNumeralWidthRatio;
	float mGPABetweenTextRatio;
	float mHourFieldHighLightInterpolationRatio;
	float mMinuteFieldHighLightInterpolationRatio;
	
	float mCenterItemHeightRatio;
	float mCenterItemHalfHeightRatio;
	float mCenterItemRegionTopRatio;
	float mCenterItemRegionBottomRatio;	
    
    gestureFlingMovementVector m_yearFieldFlingmovementVector;
    gestureFlingMovementVector m_monthFieldFlingmovementVector;
    gestureFlingMovementVector m_dayOfMonthFieldFlingmovementVector;	
	
	ArrayList<ArrayList<FieldObjectVertices>> mYearFieldRowObjectsVerticesList;
	ArrayList<ArrayList<FieldObjectVertices>> mMonthFieldRowObjectsVerticesList;
	ArrayList<ArrayList<FieldObjectVertices>> mDayOfMonthFieldRowObjectsVerticesList;	
	
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mYearHighLightFieldRowObjectsVerticesList;
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mMonthHighLightFieldRowObjectsVerticesList;
	ArrayList<ArrayList<HighLightFieldObjectVertices>> mDayOfMonthHighLightFieldRowObjectsVerticesList;
	
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
	ArrayList<Bitmap> mCenterTimePickerNumeralBitmapsList;
	
	Bitmap mTimePickerYearTextBitmap;
	Bitmap mTimePickerMonthTextBitmap;
	Bitmap mTimePickerDateTextBitmap;
	Bitmap mTimePickerCenterYearTextBitmap;
	Bitmap mTimePickerCenterMonthTextBitmap;
	Bitmap mTimePickerCenterDateTextBitmap;
	Bitmap mEraseBackgroundBitmap;
	
	BriefTimePickerRenderParameters mParameters;
	GestureDetector mTimePickerGestureDetector;
	//////////////////////////////////////////////////////////	
	
	Context mTextureViewContext;	
	
	public RepeatEndTimePickerRender(String name, Context TextureViewContext, Activity activity,
                                Handler timeMsgHandler, RepeatEndTimeInfo repeatEndTime, 
                                int width, int height,
                                BriefTimePickerRenderParameters para) {
		
		super(name);		
		
		mTextureViewContext = TextureViewContext;
		mActivity = activity;
        //mSurface = surface;
        mTimeMsgHandler = timeMsgHandler;
       
        //mCalendar         
        mRepeatEndTimePickerMinYearFieldValue = repeatEndTime.mMinYear;
        mRepeatEndTimePickerMinMonthFieldValue = repeatEndTime.mMinMonth;
        mRepeatEndTimePickerMinDayOfMonthFieldValue = repeatEndTime.mMinDayOfMonth;
        // ������ �ؾ� �Ѵ�...
        // :reset�� �Ǿ����� �ƴϸ� ����������...?
        //  ��� ������ �� �ִ°�?
        mRepeatEndTimePickerYearFieldValue = repeatEndTime.mYear;
        mRepeatEndTimePickerMonthFieldValue = repeatEndTime.mMonth;
        mRepeatEndTimePickerDayOfMonthFieldValue = repeatEndTime.mDayOfMonth;              
        mCurrentGradationOfDayOfMonthMiniTimePicker = mRepeatEndTimePickerDayOfMonthFieldValue;
        Message startTimeMsg = null;  
        startTimeMsg = Message.obtain();					
		startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, RepeatEndTimePickerRender.ALL_FIELD_TEXTURE, 
				mRepeatEndTimePickerYearFieldValue, mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerDayOfMonthFieldValue, 0, 0, 0);
		mTimeMsgHandler.sendMessage(startTimeMsg);
		
        m_Width = width;
        m_Height = height;            
        mParameters = para;       
        
        setRenderParameters();
        
        mRatio = (float) (m_Width / m_Height); 
        
		setCameraZPosition(NORMAL_TIMEPICKER_VIEW_ANGLE_OF_VIEW);
		setVeiwEyePosition();          
		setViewMatrix(m_RunnginStickersVieweyePosition);    		
		setPerspectiveProjectionMatrix(m_fovy, mRatio, mNear, mFar);			
        
        mCurrentYearFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentYearFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		mCurrentMonthFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentMonthFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		mCurrentDayOfMonthFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentDayOfMonthFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE; 
    	        	
    	
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
		
		mCenterItemRegionTopRatio = 0.0f + mCenterItemHalfHeightRatio;
		mCenterItemRegionBottomRatio = 0.0f - mCenterItemHalfHeightRatio;
		
		m_monthFieldPickerLeftMostVPRatio = 0.0f - mHorizontalGAPBetweenFields;
		m_yearFieldRightMostVPRattio = m_monthFieldPickerLeftMostVPRatio - mHorizontalGAPBetweenFields;		
		m_dayOfMonthFieldPickerLeftMostVPRatio = m_monthFieldPickerLeftMostVPRatio + (mTimePickerNumeralWidthRatio * 2) + mTimePickerTextWidthRatio + (mHorizontalGAPBetweenFields * 2);
		
		m_scrollmovementVectorOfTimePickerYearField = new gestureScrollMovementVector();
		m_scrollmovementVectorOfTimePickerMonthField = new gestureScrollMovementVector();
		m_scrollmovementVectorOfTimePickerDayOfMonthField = new gestureScrollMovementVector();
		
        m_yearFieldFlingmovementVector = new gestureFlingMovementVector();
		m_monthFieldFlingmovementVector = new gestureFlingMovementVector();
		m_dayOfMonthFieldFlingmovementVector = new gestureFlingMovementVector();			
    	
    	mYearFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		mMonthFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
    	mDayOfMonthFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();    	
    	
    	mYearHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	mMonthHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	mDayOfMonthHighLightFieldRowObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
    	
    	mTimePickerGestureDetector = new GestureDetector(mTextureViewContext, mTimePickerGesturelistener);
    	//mTimePickerGestureDetector = new GestureDetector(mActivity.getApplicationContext(), mTimePickerGesturelistener);
    	
		buildTexturePosDataFields();		
	}
	
	public void setRenderParameters() {
		mTimePickerTextWdith = mParameters.getTimePickerTextWdith();
		mTimePickerNumeralWidth = mParameters.getTimePickerNumeralWidth();	
			
		mTimePickerDateFieldCircleDiameterPixels = mParameters.getTimePickerDateFieldCircleDiameterPixels();
		m_Ppi = mParameters.getPpi();
		
		mTimePickerNumeralTextBitmapsList = mParameters.getTimePickerNumeralTextBitmapsList();		
		mCenterTimePickerNumeralBitmapsList = mParameters.getTimePickerCenterNumeralBitmapsList();		
		
		mTimePickerYearTextBitmap = mParameters.getTimePickerYearTextBitmap();
		mTimePickerMonthTextBitmap = mParameters.getTimePickerMonthTextBitmap();
		mTimePickerDateTextBitmap = mParameters.getTimePickerDateTextBitmap();
		mTimePickerCenterYearTextBitmap = mParameters.getTimePickerCenterYearTextBitmap();
		mTimePickerCenterMonthTextBitmap = mParameters.getTimePickerCenterMonthTextBitmap();
		mTimePickerCenterDateTextBitmap = mParameters.getTimePickerCenterDateTextBitmap();
		mEraseBackgroundBitmap = mParameters.getEraseBackgroundBitmap();
		
		mLeftMonthFieldOfTimePicker = mParameters.getLeftMonthFieldOfTimePicker();
		mRightMonthFieldOfTimePicker = mParameters.getRightMonthFieldOfTimePicker();
		mRightYearFieldOfTimePicker = mParameters.getRightYearFieldOfTimePicker();
		mLeftDayOfWeekFieldOfTimePicker = mParameters.getLeftDayOfWeekFieldOfTimePicker();
		
		mMainPageScrollView = mParameters.getMainPageScrollView();/////////////////////���� ���
	}
	
	Looper mMainThreadLooper;
	int mMainThreadPriority;
	ReentrantLock mRenderingLock;        
    Condition mConditionOfRenderingLock;
    int mRenderingThreadPriority;
	Handler mRenderThreadHandler;
	@Override
	protected void onLooperPrepared() {        	
    	mRenderingLock = new ReentrantLock();        	
    	mConditionOfRenderingLock = mRenderingLock.newCondition();
    	mMainThreadPriority = this.getPriority();   
    	mRenderingThreadPriority = mMainThreadPriority + 1;
    	        
		super.onLooperPrepared();
		
		mMainThreadLooper = getLooper();
		
		mRenderThreadHandler = new Handler(mMainThreadLooper, this);
		
		TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(START_OPENGLES_RENDER_THREAD, 
    			0, 0);
    	Message msg = Message.obtain();
    	msg.obj = gestureMsg;	
		mRenderThreadHandler.sendMessage(msg);
	}     
	
	
	public void reStartRenderThread() {		
		mYearFieldStatus = IDLE;
		mMonthFieldStatus = IDLE;
		mDayOfMonthFieldStatus = IDLE;
		
		TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(START_OPENGLES_RENDER_THREAD, 
    			0, 0);
    	Message msg = Message.obtain();
    	msg.obj = gestureMsg;	
		mRenderThreadHandler.sendMessage(msg);	
	}
	
	public void startRenderThread() {				
		mOpenGLESRender = new OpenGLESRender(mRenderingLock, mConditionOfRenderingLock);        	
    	mOpenGLESRender.setDaemon(true);
    	mOpenGLESRender.start();   
	}
            
    OpenGLESRender mOpenGLESRender;       
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

		@SuppressLint("SuspiciousIndentation")
		@Override
		public void run() {
			super.run();	
			
			initGL();            
            checkCurrent();	            
            onDrawFrame();	     
            
            if (INFO) Log.i(TAG, "OpenGLESRender:first call onDrawFrame");
            
            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                Log.e(TAG, "cannot swap buffers!");
            }
            checkEglError();	            
                        
            if (INFO) Log.i(TAG, "OpenGLESRender:before looping");
            
			while (mContinue) {
				
				WakeUPToken.lock();
				
    			try {   				
    				// The lock associated with this Condition is atomically released 
    				// and the current thread becomes disabled for thread scheduling purposes and lies dormant until one of four things happens: 
    				ConditionOfRenderingLock.await();
    				    				
    				if (mStatus == RUN) {
    					if (INFO) Log.i(TAG, "OpenGLESRender:RUN");
	    					            
			            onDrawFrame();            
			            
			            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
			                Log.e(TAG, "cannot swap buffers!");
			            }
	    						    				
			            checkEglError();
    				}
    				else {
    					if (INFO) Log.i(TAG, "OpenGLESRender:EXIT Set");
    					mContinue = false;   					
    					deleteTextures();	
    					cleanupEgl();   							
    				}		            
    				
                } catch (InterruptedException e) {					
					e.printStackTrace();
				} finally {
					WakeUPToken.unlock();
				}
    			
			}
    		/////////////////////////////////////////////////////////////////////////////////				
			
			
			if (INFO) Log.i(TAG, "OpenGLESRender:escape while loop");
			synchronized(InterruptSync) {
				InterruptSync.notifyAll();
				
				if (INFO) Log.i(TAG, "OpenGLESRender:InterruptSync.notify!!!");
			}
					
			if (INFO) Log.i(TAG, "OpenGLESRender:run : goodbye...");
		}  
		
		public void shouldStop() throws InterruptedException {
			
			if (INFO) Log.i(TAG, "OpenGLESRender:shouldStop");
			
			synchronized(InterruptSync) {
				WakeUPToken.lock();
				
				try {	
					mStatus = EXIT;		
					ConditionOfRenderingLock.signalAll();
				} finally {
					WakeUPToken.unlock(); 
				}
				
				InterruptSync.wait();	
				if (INFO) Log.i(TAG, "OpenGLESRender:shouldStop : bye...");
			}
			
		}        
    }        
    
 	
	
 	public void resetCalendar(Calendar cal) {
 		/*mCalendar.clear();
 		mCalendar.set(cal.get(Calendar.YEAR), 
 				      cal.get(Calendar.MONTH), 
 				      cal.get(Calendar.DATE));*/
 		mRepeatEndTimePickerYearFieldValue = cal.get(Calendar.YEAR);
        mRepeatEndTimePickerMonthFieldValue = cal.get(Calendar.MONTH);
        mRepeatEndTimePickerDayOfMonthFieldValue = cal.get(Calendar.DATE);
        mCurrentGradationOfDayOfMonthMiniTimePicker = mRepeatEndTimePickerDayOfMonthFieldValue;    
 	}
 	
 	public void resetField() {
 		mCurrentYearFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentYearFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		mCurrentMonthFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentMonthFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		mCurrentDayOfMonthFieldUppermostItemBottomSideDegree = TIMEPICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentDayOfMonthFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
 		
 		mYearFieldRowObjectsVerticesList = buildYearFieldsVerticesAndTexturePosData(mCurrentYearFieldLowermostItemTopSideDegree, mRepeatEndTimePickerYearFieldValue);
        mYearHighLightFieldRowObjectsVerticesList = buildYearFieldsHighLightVerticesTexturePosData();
        
        mMonthFieldRowObjectsVerticesList = buildMonthFieldsVerticesAndTexturePosData(mCurrentMonthFieldLowermostItemTopSideDegree, mRepeatEndTimePickerMonthFieldValue);
        mMonthHighLightFieldRowObjectsVerticesList = buildMonthFieldsHighLightVerticesTexturePosData();        
        
        mDayOfMonthFieldRowObjectsVerticesList = buildDayOfMonthFieldsVerticesAndTexturePosData(mCurrentDayOfMonthFieldLowermostItemTopSideDegree);
        mDayOfMonthHighLightFieldRowObjectsVerticesList = buildDayOfMonthFieldsHighLightVerticesTexturePosData();
 	}
 		
	
    // �� field�� movement status machine�� ��������
    //@SuppressWarnings("unchecked")
    // mRenderThreadHandler = new Handler(mTimePicker.getLooper(), mTimePicker); �� callback ��
	@Override
    public boolean handleMessage(Message msg) {		
    	TimePickerUserActionMessage gestureMsg = (TimePickerUserActionMessage)msg.obj;
    	int field = gestureMsg.mFieldID; 	
    		
		switch (field) {  
		case START_OPENGLES_RENDER_THREAD:
			startRenderThread();
			break;
        case YEAR_FIELD_TEXTURE:            	
        	YearFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar);      	
        	break;
        case MONTH_FIELD_TEXTURE:
        	MonthFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar);  
        	break;
        case DAYOFMONTH_FIELD_TEXTURE:
        	DayOfMonthFieldStatusMachine(gestureMsg.mGestureID, gestureMsg.mScalar); 
        	break;	        
        default:
        	return false;
        }    	
    	        	
    	return true;
    }	
	
	public void setFieldStatus(int field, int status) {
		switch (field) {        
        case YEAR_FIELD_TEXTURE:
        	mYearFieldStatus = status;
        	break;
        case MONTH_FIELD_TEXTURE:
        	mMonthFieldStatus = status;
        	break;
        case DAYOFMONTH_FIELD_TEXTURE:
        	mDayOfMonthFieldStatus = status;
        	break;
        }
	}
	
	public int getFieldStatus(int field) {
		int status = 0;
		switch (field) {        
        case YEAR_FIELD_TEXTURE:
        	status = mYearFieldStatus;
        	break;
        case MONTH_FIELD_TEXTURE:
        	status = mMonthFieldStatus;
        	break;
        case DAYOFMONTH_FIELD_TEXTURE:
        	status = mDayOfMonthFieldStatus;
        	break;
        }
		return status;
	}
	
	public ArrayList<ArrayList<HighLightFieldObjectVertices>> buildVerticesTexturePosDataOfFieldsHighLight(int field) {		
    	ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightFieldRowObjectsVerticesList = null;
		
		switch(field) {    	
    	case YEAR_FIELD_TEXTURE:
    		HighLightFieldRowObjectsVerticesList = buildYearFieldsHighLightVerticesTexturePosData();
    		break; 
    	case MONTH_FIELD_TEXTURE:
    		HighLightFieldRowObjectsVerticesList = buildMonthFieldsHighLightVerticesTexturePosData();
    		break; 
    	case DAYOFMONTH_FIELD_TEXTURE:
    		HighLightFieldRowObjectsVerticesList = buildDayOfMonthFieldsHighLightVerticesTexturePosData();
    		break; 
    	}
		
		return HighLightFieldRowObjectsVerticesList;
	}
    
    @SuppressWarnings("unchecked")
	public void computeNormalScrolling(int field, float scalar, boolean interpolation, boolean needCheckingMoveDirection) {
    	// ���⼭ �ش� Ķ������ Ŭ���Ͽ� �Ʒ� �� �Լ��� �����ϴ� ���� ���???
    	// : ���� ����̴�!!!
    	//Calendar calendar = cloneFiledCalendar(field);
    	ArrayList<ArrayList<FieldObjectVertices>> FieldRowObjectsVerticesList = updateFieldVerticesTexturesDatas(field, scalar, interpolation, needCheckingMoveDirection);
		ArrayList<ArrayList<HighLightFieldObjectVertices>> HighLightFieldRowObjectsVerticesList = buildVerticesTexturePosDataOfFieldsHighLight(field);
		
		mRenderingLock.lock();  
		switch(field) {
    	
    	case YEAR_FIELD_TEXTURE:
    		mYearFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>)FieldRowObjectsVerticesList.clone();
    		mYearHighLightFieldRowObjectsVerticesList = 
    				(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightFieldRowObjectsVerticesList.clone();
    		break;
    	case MONTH_FIELD_TEXTURE:
    		mMonthFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>)FieldRowObjectsVerticesList.clone();
    		mMonthHighLightFieldRowObjectsVerticesList = 
    				(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightFieldRowObjectsVerticesList.clone();
    		break;
    	case DAYOFMONTH_FIELD_TEXTURE:
    		mDayOfMonthFieldRowObjectsVerticesList = (ArrayList<ArrayList<FieldObjectVertices>>)FieldRowObjectsVerticesList.clone();
    		mDayOfMonthHighLightFieldRowObjectsVerticesList = 
    				(ArrayList<ArrayList<HighLightFieldObjectVertices>>)HighLightFieldRowObjectsVerticesList.clone();
    		break;
    	default:
    		break;
    	}		
		mConditionOfRenderingLock.signalAll();      			
		mRenderingLock.unlock();
		Thread.yield();
    }    
    
	FieldFlingComputeThread mYearFieldFlingComputeThread;
	FieldFlingComputeThread mMonthFieldFlingComputeThread;
	FieldFlingComputeThread mDayOfMonthFieldFlingComputeThread;
	public void LaunchFlingComputeThread(int field, float scalar) {
		float velocityY = (scalar * 2);
		
		switch (field) {
        case YEAR_FIELD_TEXTURE:        	
        	setFlingFactor(m_yearFieldFlingmovementVector, velocityY);         	     		
        	mYearFieldFlingComputeThread = new FieldFlingComputeThread(field);
        	mYearFieldFlingComputeThread.setDaemon(true);
        	mYearFieldFlingComputeThread.start();
        	break;
        
        case MONTH_FIELD_TEXTURE:
        	setFlingFactor(m_monthFieldFlingmovementVector, velocityY);         	     		
        	mMonthFieldFlingComputeThread = new FieldFlingComputeThread(field);
        	mMonthFieldFlingComputeThread.setDaemon(true);
        	mMonthFieldFlingComputeThread.start();
        	break;
        case DAYOFMONTH_FIELD_TEXTURE:
        	setFlingFactor(m_dayOfMonthFieldFlingmovementVector, velocityY);         	     		
        	mDayOfMonthFieldFlingComputeThread = new FieldFlingComputeThread(field);
        	mDayOfMonthFieldFlingComputeThread.setDaemon(true);
        	mDayOfMonthFieldFlingComputeThread.start();
        	break;
        default:
        	return;
        }		
	}	
	
	FieldAutoScrollingComputeThread mYearFieldAutoScrollingThread; 
	FieldAutoScrollingComputeThread mMonthFieldAutoScrollingThread; 
	FieldAutoScrollingComputeThread mDayOfMonthFieldAutoScrollingThread;
	public void LaunchFieldAutoScrollingComputeThread(int field) {		
		
		switch (field) {
        case YEAR_FIELD_TEXTURE:        	
        	mYearFieldAutoScrollingThread = new FieldAutoScrollingComputeThread(field);
        	mYearFieldAutoScrollingThread.setDaemon(true);
        	mYearFieldAutoScrollingThread.start();
        	break;
        
        case MONTH_FIELD_TEXTURE:
        	mMonthFieldAutoScrollingThread = new FieldAutoScrollingComputeThread(field);
        	mMonthFieldAutoScrollingThread.setDaemon(true);
        	mMonthFieldAutoScrollingThread.start();
        	break;
        case DAYOFMONTH_FIELD_TEXTURE:
        	mDayOfMonthFieldAutoScrollingThread = new FieldAutoScrollingComputeThread(field);
        	mDayOfMonthFieldAutoScrollingThread.setDaemon(true);
        	mDayOfMonthFieldAutoScrollingThread.start();
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
							computeNormalScrolling(mField, mScalar, false, true);							
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
	        				setFieldStatus(mField, IDLE);
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
					
					computeNormalScrolling(mField, mScalar, lastWork, false);					
					
					if (lastWork) {								
						setFieldStatus(mField, IDLE);
						messageShoot(mField);						
						mContinue = false;						
						//setFieldStatus(mField, IDLE);
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
					
					//Log.i("tag", "FASCT : mScalar=" + String.valueOf(mScalar));
					computeNormalScrolling(mField, mScalar, lastWork, false);
					
					if (lastWork) {		
						setFieldStatus(mField, IDLE);
						messageShoot(mField);					
						mContinue = false;	
						//setFieldStatus(mField, IDLE);
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
	    
    int mYearFieldStatus = IDLE;
    FieldAdjustmentComputeThread mYearFieldAdjustmentComputeByExceedMinThread;
    public void YearFieldStatusMachine(int gestureID, float scalar) {
    	
    	switch(mYearFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {        			
    			mYearFieldStatus = SCROLLING;////////////////////////////////////////////////////////////////////////
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_SCROLLING_MSG");
				computeNormalScrolling(YEAR_FIELD_TEXTURE, scalar, false, false);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling ��.
    													                              // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  							  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_FLING_MSG");
    			mYearFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(YEAR_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { // �� ���´� fling ���� ���� down msg �߻��Ǹ� fling ����� �ﰢ �ߴܵǰ�, idle ���·� ��ٷ� ���̵ȴ�
    			                                              // �׸��� up msg�� �߻��Ǹ� ó���Ѵ�
    			
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_ACTION_UP_MSG");		
    			mYearFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(YEAR_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG) {
    			mYearFieldStatus = FIELD_ADJUSTMENT_BY_EXCEED_MIX;
    			
    			// if) mRepeatEndTimePickerMinYearFieldValue == 2014
    			//     mRepeatEndTimePickerYearFieldValue == 2009
    			//     value = 2014 - 2009 = 5
    			//     mRepeatEndTimePickerYearFieldValue = 2014 - 4 = 2010
    			int value = mRepeatEndTimePickerMinYearFieldValue - mRepeatEndTimePickerYearFieldValue;
    			if (value > 4) {
    				mRepeatEndTimePickerYearFieldValue = mRepeatEndTimePickerMinYearFieldValue - 4;
    				value = 4;
    			}
    			
    			//if (scalar < 0) {  // finger move upward : down -> up --> value�� -value�� ����
    			mYearFieldAdjustmentComputeByExceedMinThread = new FieldAdjustmentComputeThread(YEAR_FIELD_TEXTURE, -value);
    			mYearFieldAdjustmentComputeByExceedMinThread.setDaemon(true);
    			mYearFieldAdjustmentComputeByExceedMinThread.start();
				//mRepeatEndTimePickerYearFieldValue = mRepeatEndTimePickerMaxYearFieldValue;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mYearFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mYearFieldStatus = IDLE; // ���ʿ��ϴ� �̹� IDLE ������!!
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mYearFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "DFSM : SCROLLING : TIMEPICKER_SCROLLING_MSG");
    			computeNormalScrolling(YEAR_FIELD_TEXTURE, scalar, false, false);       			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { 
    			//Log.i("tag", "DFSM : SCROLLING : TIMEPICKER_ACTION_UP_MSG");
    			mYearFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(YEAR_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mYearFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mYearFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mYearFieldStatus = EXIT;
    		}
    		else { // down�� �߻��� �� �ִ°�??? : �߻��� �� ���� -> ���� ��ġ�гο��� finger�� ���� �ʰ� �ֱ� ����...(up msg�� �߻��ߴٴ� ���� autoscrolling ���·� ��ȯ�Ǿ��ٴ� ���� �ǹ�!!!)
    			//Log.i("tag", "DFSM : SCROLLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case FLING: 
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // �̷� ���� �߻����� �ʴ´�???
    			                                     // FLING ���¿��� �ٽ� fling�� �� �õ��ϸ� ACTION_DOWN�� ���� �߻��ϱ� �����̴�
    			                                     // :������� �߻����� �ʾ���
    			//Log.i("tag", "YearFieldStatusMachine : FLING : TIMEPICKER_FLING_MSG");
    			// ���� �������� fling ������ �ߴܽ�Ű�� �ٽ� �����Ѵ�
    			// �׷��� ���� fling ���� �������� ���Ḧ Ȯ������ �ʾƵ� �Ǵ°�?
    			// :Ȯ���ؾ� ����
    			mYearFieldFlingComputeThread.shouldStop();
    			mYearFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(YEAR_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // fling ������ ��� �ߴܽ�Ű�� ���� ��ġ�� ���,
    			                                                // fling ���¿��� �ٽ� flig�� �� �õ��ϱ� ���� ��ġ�� ���,,,
    			//Log.i("tag", "YearFieldStatusMachine : FLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mYearFieldFlingComputeThread.shouldStop();   
    			mYearFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mYearFieldFlingComputeThread.shouldStop();
    			mYearFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mYearFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mYearFieldFlingComputeThread.shouldStop();
    			mYearFieldStatus = EXIT;
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
    			mYearFieldAutoScrollingThread.shouldStop();  
    			mYearFieldStatus = FLING; 
    			LaunchFlingComputeThread(YEAR_FIELD_TEXTURE, scalar);	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // �߻��Ѵ� : autoscrolling ���� �߿� ��ġ�� �Ǵ� ���,
    			                                                // shouldStop���� ������ blocking�� ���� �� �ִ� : �ϴ� �ذ���!!!
    			//Log.i("tag", "DFSM : AUTOSCROLLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mYearFieldAutoScrollingThread.shouldStop();  
    			mYearFieldStatus = IDLE;
    			messageShoot(YEAR_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mYearFieldAutoScrollingThread.shouldStop();
    			mYearFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mYearFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mYearFieldAutoScrollingThread.shouldStop();
    			mYearFieldStatus = EXIT;
    		}
    		else {
    			Log.i("tag", "YearFieldStatusMachine : AUTOSCROLLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case FIELD_ADJUSTMENT_BY_EXCEED_MIX:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // �߻��Ѵ� : autoscrolling ���� �߿� ��ġ�� �Ǵ� ���,
                // ó���ϸ� �ȵȴ�!!!
			}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mYearFieldStatus = EXIT;
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mYearFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mYearFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mYearFieldStatus = EXIT;
    		}
    		break;
    	case EXIT:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mYearFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mYearFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mYearFieldStatus = EXIT;
    		}
    		break;
    	default:
    		if(INFO) Log.i(TAG, "YearFieldStatusMachine : gestureID=" + String.valueOf(gestureID));
    		return;
    	}
    }
    
    int mMonthFieldStatus = IDLE;
    FieldAdjustmentComputeThread mMonthFieldAdjustmentComputeByExceedMinThread;
    public void MonthFieldStatusMachine(int gestureID, float scalar) {
    	//Log.i("tag", "gestureID=" + String.valueOf(gestureID));
    	switch(mMonthFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {        			
    			mMonthFieldStatus = SCROLLING;////////////////////////////////////////////////////////////////////////
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_SCROLLING_MSG");
				computeNormalScrolling(MONTH_FIELD_TEXTURE, scalar, false, false);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling ��.
    													                              // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  							  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_FLING_MSG");
    			mMonthFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(MONTH_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { // �� ���´� fling ���� ���� down msg �߻��Ǹ� fling ����� �ﰢ �ߴܵǰ�, idle ���·� ��ٷ� ���̵ȴ�
    			                                              // �׸��� up msg�� �߻��Ǹ� ó���Ѵ�
    			
    			//Log.i("tag", "DFSM : IDLE : TIMEPICKER_ACTION_UP_MSG");		
    			mMonthFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(MONTH_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG) {
    			mMonthFieldStatus = FIELD_ADJUSTMENT_BY_EXCEED_MIX;    			
    			int value = mRepeatEndTimePickerMinMonthFieldValue - mRepeatEndTimePickerMonthFieldValue;
    			
    			//if (scalar < 0) {  // finger move upward : down -> up --> value�� -value�� ����
    			mMonthFieldAdjustmentComputeByExceedMinThread = new FieldAdjustmentComputeThread(MONTH_FIELD_TEXTURE, -value);
    			mMonthFieldAdjustmentComputeByExceedMinThread.setDaemon(true);
    			mMonthFieldAdjustmentComputeByExceedMinThread.start();				
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMonthFieldStatus = IDLE; // ���ʿ��ϴ� �̹� IDLE ������!!
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMonthFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "DFSM : SCROLLING : TIMEPICKER_SCROLLING_MSG");
    			computeNormalScrolling(MONTH_FIELD_TEXTURE, scalar, false, false);       			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { 
    			//Log.i("tag", "DFSM : SCROLLING : TIMEPICKER_ACTION_UP_MSG");
    			mMonthFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(MONTH_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMonthFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMonthFieldStatus = EXIT;
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
    			mMonthFieldFlingComputeThread.shouldStop();
    			mMonthFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(MONTH_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // fling ������ ��� �ߴܽ�Ű�� ���� ��ġ�� ���,
    			                                                // fling ���¿��� �ٽ� flig�� �� �õ��ϱ� ���� ��ġ�� ���,,,
    			//Log.i("tag", "DFSM : FLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mMonthFieldFlingComputeThread.shouldStop();   
    			mMonthFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMonthFieldFlingComputeThread.shouldStop();
    			mMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMonthFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMonthFieldFlingComputeThread.shouldStop();
    			mMonthFieldStatus = EXIT;
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
    			mMonthFieldAutoScrollingThread.shouldStop();  
    			mMonthFieldStatus = FLING; 
    			LaunchFlingComputeThread(MONTH_FIELD_TEXTURE, scalar);	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // �߻��Ѵ� : autoscrolling ���� �߿� ��ġ�� �Ǵ� ���,
    			                                                // shouldStop���� ������ blocking�� ���� �� �ִ� : �ϴ� �ذ���!!!
    			//Log.i("tag", "DFSM : AUTOSCROLLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mMonthFieldAutoScrollingThread.shouldStop();  
    			mMonthFieldStatus = IDLE;
    			messageShoot(MONTH_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMonthFieldAutoScrollingThread.shouldStop();
    			mMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMonthFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mMonthFieldAutoScrollingThread.shouldStop();
    			mMonthFieldStatus = EXIT;
    		}
    		else {
    			Log.i("tag", "DFSM : AUTOSCROLLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case FIELD_ADJUSTMENT_BY_EXCEED_MIX:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // �߻��Ѵ� : autoscrolling ���� �߿� ��ġ�� �Ǵ� ���,
                // ó���ϸ� �ȵȴ�!!!
			}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMonthFieldStatus = EXIT;
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMonthFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMonthFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMonthFieldStatus = EXIT;
    		}
    		break;
    	case EXIT:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mMonthFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mMonthFieldStatus = SLEEP; 
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mMonthFieldStatus = EXIT; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		break;
    	default:
    		return;
    	}
    }
    
    int mDayOfMonthFieldStatus = IDLE;
    FieldAdjustmentComputeThread mDayOfMonthFieldAdjustmentComputeByExceedMinThread;
    public void DayOfMonthFieldStatusMachine(int gestureID, float scalar) {
    	//Log.i("tag", "DayOfMonthFieldStatusMachine=" + String.valueOf(gestureID));
    	switch(mDayOfMonthFieldStatus) {
    	case IDLE:        		
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) {        			
    			mDayOfMonthFieldStatus = SCROLLING;////////////////////////////////////////////////////////////////////////
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : IDLE : TIMEPICKER_SCROLLING_MSG");
				computeNormalScrolling(DAYOFMONTH_FIELD_TEXTURE, scalar, false, true);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_FLING_MSG) { // fling�� �߻��ϴ� �������� onScroll->ACTION_UP->onFling ��.
    													                              // SCROLLING ���¿��� ACTION_UP�� �߻��ϸ� AUTOSCROLLING ���·� ���̵ȴ�
    													  							  // �׷���,
    			                                          // mCurrentDateFieldLowermostItemTopSideDegree�� FIELD_ITEM_DEGREE ���,
    			                                          // AUTOSCROLLING�� �� �ʿ䰡 �����Ƿ� IDLE ���·� ���̵ȴ�.
    			                                          // �׸��� ��ٷ� onFling�� �߻��Ǹ�,,,FLING ���·� ���̵Ǿ�� �Ѵ�
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : IDLE : TIMEPICKER_FLING_MSG");
    			mDayOfMonthFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(DAYOFMONTH_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { // �� ���´� fling ���� ���� down msg �߻��Ǹ� fling ����� �ﰢ �ߴܵǰ�, idle ���·� ��ٷ� ���̵ȴ�
    			                                              // �׸��� up msg�� �߻��Ǹ� ó���Ѵ�
    			
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : IDLE : TIMEPICKER_ACTION_UP_MSG");		
    			mDayOfMonthFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(DAYOFMONTH_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_DAYOFMONTH_FIELD_MSG) {
    			// ���⼭ ó���ϴ� ���� Ÿ�ʵ忡�� �ش� �޽����� �߻������� ����̴�. ���� �ӽ��� idle ���¿����� ó���Ѵ�
    			// : idle ���°� �ƴϸ� DayOfMonth �ʵ�� � ó���� �ϰ� �����Ƿ�, �ش� ó���� �Ϸ�� ���� ADJUSTMENT�� �����ؾ� �Ѵ�
    			// DayOfMonth �ʵ��� ������ ��Ȳ ���� ó�� �Ϸ��� ���� ��ȯ�� ���� �ӽ� ��ƾ�� Ÿ�� �ʵ��� �Ѵ�
    			// :��ٷ� adjustment thread�� �ش� ��ġ���� ��ġ���Ѿ� �Ѵ�
    			mDayOfMonthFieldStatus = DAYOFMONTH_FIELD_ADJUSTMENT;
    			mDayOfMonthFieldAdjustmentComputeThread = new FieldAdjustmentComputeThread(DAYOFMONTH_FIELD_TEXTURE, (int)scalar);
				mDayOfMonthFieldAdjustmentComputeThread.setDaemon(true);
				mDayOfMonthFieldAdjustmentComputeThread.start();
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : IDLE : TIMEPICKER_AUTO_ADJUSTMENT_DAYOFMONTH_FIELD_MSG");    			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG) {
    			mDayOfMonthFieldStatus = FIELD_ADJUSTMENT_BY_EXCEED_MIX;    			
    			int value = mRepeatEndTimePickerMinDayOfMonthFieldValue - mRepeatEndTimePickerDayOfMonthFieldValue;
    			
    			//if (scalar < 0) {  // finger move upward : down -> up --> value�� -value�� ����
    			mDayOfMonthFieldAdjustmentComputeByExceedMinThread = new FieldAdjustmentComputeThread(DAYOFMONTH_FIELD_TEXTURE, -value);
    			mDayOfMonthFieldAdjustmentComputeByExceedMinThread.setDaemon(true);
    			mDayOfMonthFieldAdjustmentComputeByExceedMinThread.start();				
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_UPDATE_DAYOFMONTH_FIELD_MSG) {
    			updateDayOfMonthFieldByOtherField();
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDayOfMonthFieldStatus = IDLE; // ���ʿ��ϴ� �̹� IDLE ������!!
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDayOfMonthFieldStatus = EXIT;
    		}
    		else { // down�� ����ϰ� �߻��Ѵ�...�׷��� �����Ѵ�...
    			//Log.i("tag", "DFSM : IDLE : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case SCROLLING:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_SCROLLING_MSG) { 
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : SCROLLING : TIMEPICKER_SCROLLING_MSG");
    			computeNormalScrolling(DAYOFMONTH_FIELD_TEXTURE, scalar, false, true);       			
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) { 
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : SCROLLING : TIMEPICKER_ACTION_UP_MSG");
    			mDayOfMonthFieldStatus = AUTOSCROLLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFieldAutoScrollingComputeThread(DAYOFMONTH_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDayOfMonthFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDayOfMonthFieldStatus = EXIT;
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
    			mDayOfMonthFieldFlingComputeThread.shouldStop();
    			mDayOfMonthFieldStatus = FLING;///////////////////////////////////////////////////////////////////////////////////////////////
    			LaunchFlingComputeThread(DAYOFMONTH_FIELD_TEXTURE, scalar);
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // fling ������ ��� �ߴܽ�Ű�� ���� ��ġ�� ���,
    			                                                // fling ���¿��� �ٽ� flig�� �� �õ��ϱ� ���� ��ġ�� ���,,,
    			//Log.i("tag", "DayOfMonthFieldStatusMachine : FLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mDayOfMonthFieldFlingComputeThread.shouldStop();   
    			mDayOfMonthFieldStatus = IDLE;///////////////////////////////////////////////////////////////////////////////////////////////
    		} 
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldFlingComputeThread.shouldStop();
    			mDayOfMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDayOfMonthFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mDayOfMonthFieldFlingComputeThread.shouldStop();
    			mDayOfMonthFieldStatus = EXIT;
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
    			mDayOfMonthFieldAutoScrollingThread.shouldStop();  
    			mDayOfMonthFieldStatus = FLING; 
    			LaunchFlingComputeThread(DAYOFMONTH_FIELD_TEXTURE, scalar);	
    		}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) { // �߻��Ѵ� : autoscrolling ���� �߿� ��ġ�� �Ǵ� ���,
    			                                                // shouldStop���� ������ blocking�� ���� �� �ִ� : �ϴ� �ذ���!!!
    			//Log.i("tag", "DFSM : AUTOSCROLLING : TIMEPICKER_ACTION_DOWN_MSG");
    			mDayOfMonthFieldAutoScrollingThread.shouldStop();  
    			mDayOfMonthFieldStatus = IDLE;
    			// ���ʿ��� ȣ���̴� 
    			// IDLE ���¿��� UP MSG�� �߻��ϸ鼭 �ٽ� AutoScrollingThread�� ��ġ�Ѵ�
    			//messageShoot(DAYOFMONTH_FIELD_TEXTURE);
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldAutoScrollingThread.shouldStop();
    			mDayOfMonthFieldStatus = SLEEP;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDayOfMonthFieldStatus = IDLE; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {
    			mDayOfMonthFieldAutoScrollingThread.shouldStop();
    			mDayOfMonthFieldStatus = EXIT;
    		}
    		else {
    			Log.i("tag", "DFSM : AUTOSCROLLING : gestureID=" + String.valueOf(gestureID));
    		}
    		break;
    	case DAYOFMONTH_FIELD_ADJUSTMENT:
    		if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_DOWN_MSG) {           
	    		mDayOfMonthFieldAdjustmentComputeThread.shouldStop(); 							
			}
    		else if (gestureID == TimePickerUserActionMessage.TIMEPICKER_ACTION_UP_MSG) {
    			mDayOfMonthFieldStatus = IDLE;	
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldAdjustmentComputeThread.shouldStop();
    			mDayOfMonthFieldStatus = SLEEP;
    		}
    		break;
    	case SLEEP:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDayOfMonthFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDayOfMonthFieldStatus = EXIT;
    		}
    		break;
    	case EXIT:
    		if (gestureID == TimePickerUserActionMessage.RUN_WAKEUP) {
    			mDayOfMonthFieldStatus = IDLE;
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_SLEEP) {
    			mDayOfMonthFieldStatus = SLEEP; // �� �޽����� �� ���¿��� �߻��� �� �ִ°�?
    		}
    		else if (gestureID == TimePickerUserActionMessage.RUN_EXIT) {    			
    			mDayOfMonthFieldStatus = EXIT;
    		}
    		break;
    	default:
    		return;
    	}
    }
    
    public void checkFieldsAfterAdjustmentByExceedMin(int field) {
    	
    }
    
    private static final int ADJUMSTMENT_AUTOSELFPOSITION_STATUS = 1;
    private static final int ADJUMSTMENT_INTERRUPTED_STATUS = 2;
    private static final float ADJUMSTMENT_ANIMATION_TIME_MS = 200;
	public class FieldAdjustmentComputeThread extends Thread {
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
    	
    	public FieldAdjustmentComputeThread(int field, int adjustmentValue) {
    		mField = field;	
    		setPriority(mRenderingThreadPriority - 1);
    		mInterruptSync = new Object();
			mContinue = true;
			mStatus = ADJUMSTMENT_AUTOSELFPOSITION_STATUS;
			mAccumulatedAnimationTime = 0;
			
			mTargetScalarDegree = FIELD_ITEM_DEGREE * adjustmentValue;
			mTargetScalar = (float)(mTimePickerDateFieldCircleDiameterPixels * (mTargetScalarDegree/360));
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
			}
			
			while (mContinue) {	
				if (mStatus == ADJUMSTMENT_AUTOSELFPOSITION_STATUS) {
					boolean lastWork = false;
					gotTime = System.nanoTime();
					deltaTime = (gotTime - mStartTime) / 1000000.0f;
		        	mStartTime = gotTime;
		        	
					mAccumulatedAnimationTime = mAccumulatedAnimationTime + deltaTime;
					if (mAccumulatedAnimationTime > ADJUMSTMENT_ANIMATION_TIME_MS) {
						mAccumulatedAnimationTime = ADJUMSTMENT_ANIMATION_TIME_MS;	
						lastWork = true;
					}			
					
					double target_scalar_time = (double)mAccumulatedAnimationTime;
					double target_total_time = (double)ADJUMSTMENT_ANIMATION_TIME_MS;		
					float target_apply_time = Linear.easeIn((float)target_scalar_time, 0, (float)target_total_time, (float)target_total_time) / (float)target_total_time;
					
					float temp = mTargetScalar * (float)target_apply_time;
					mScalar = temp - mPrvScalar;
					mPrvScalar = temp;					
					
					computeNormalScrolling(mField, mScalar, lastWork, false);
					
					if (lastWork) {								
						mContinue = false;	
						setFieldStatus(mField, IDLE);
						messageShoot(mField);
						//checkFieldsAfterAdjustmentByExceedMin(mField);
						//Log.i("tag", "FASCT : compute completion");							
					}
				}
				else {
					mContinue = false;
					//Log.i("tag", "FASCT : AUTOSCROLLING_INTERRUPTED_STATUS");							
				}
			}
			
			if (mStatus == ADJUMSTMENT_INTERRUPTED_STATUS) {
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
			
			mStatus = ADJUMSTMENT_INTERRUPTED_STATUS;					
			
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
	
    
    public void messageShoot(int updateField, int Year, int Month, int Date, int AMPM, int Hour, int Minute) {        			
		Message msg = Message.obtain();			
		msg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, Year, Month, Date, AMPM, Hour, Minute);			
		mTimeMsgHandler.sendMessage(msg);		
    }    
    
    public void updateDayOfMonthFieldByOtherField() {
    	mRenderingLock.lock();
    	mDayOfMonthFieldRowObjectsVerticesList = buildDayOfMonthFieldsVerticesAndTexturePosData(mCurrentDayOfMonthFieldLowermostItemTopSideDegree);
        mDayOfMonthHighLightFieldRowObjectsVerticesList = buildDayOfMonthFieldsHighLightVerticesTexturePosData();
        mConditionOfRenderingLock.signalAll();      			
        mRenderingLock.unlock();
        Thread.yield();
    }
    
    public void messageShoot(int updateField) { 
    	int Year = 0;
    	int Month = 0;
    	int Date = 0;
    	int AMPM = 0;
    	int Hour = 0;
    	int Minute = 0;
    	int daysOfThisMonth = 0;
    	Message startTimeMsg = null;    	
    	
		switch (updateField) {		
		case YEAR_FIELD_TEXTURE:				
			Year = mRepeatEndTimePickerYearFieldValue;
			Month = mRepeatEndTimePickerMonthFieldValue;
			Date = mRepeatEndTimePickerDayOfMonthFieldValue;			
			
			// ������ timepicker�� ������ RepeatEnd ������ �ּ� �Ѱ谪���� ���� ���,
			// adjustment ������ ��Ī��Ű�� �޽����� �߼��ؾ� �Ѵ�
			// MainPage�� RepeatEnd Text�� ���� ���� �޽����� ���õȴ�
			if (mRepeatEndTimePickerMinYearFieldValue > Year) {				
		    	TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(YEAR_FIELD_TEXTURE, 
		    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG, 0);
		    	Message msg = Message.obtain();
		    	msg.obj = gestureMsg;		    	
		    	mRenderThreadHandler.sendMessage(msg);	    	
			}
			else {	
				// RepeatEnd�� �ּ� �Ѱ� �⵵�� ������ �⵵�� �����Ǿ����Ƿ�,
				// RepeatEnd�� month�� ��ȿ���� üũ�ؾ� �Ѵ�
				if (mRepeatEndTimePickerMinYearFieldValue == Year) {
					// ���� Month Field�� Fling �Ǵ� AutoScrolling �׸��� Adjustment ���� ��[Idle ���°� �ƴ�]�̶��,,,					
					// :�������...idle �̿� ������ ����ó���� �Ϸ�Ǹ�,
					//  MessageShoot�� MONTH_FIELD_TEXTURE ó���ο��� Year Field ���� Ȯ�� �� ��ü Adjustment ������ ������ ���̱� �����̴�.
					if (mRepeatEndTimePickerMinMonthFieldValue > mRepeatEndTimePickerMonthFieldValue) {						
						TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(MONTH_FIELD_TEXTURE, 
				    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG, 0);
				    	Message msg = Message.obtain();
				    	msg.obj = gestureMsg;				    	
				    	mRenderThreadHandler.sendMessage(msg);				    	
					}
				}
				else {
					// ���⵵ �ƴѵ� 2�� 29�Ϸ� �����Ǿ� �ִٸ� ������Ʈ�� �ؾ� �Ѵ�
					boolean leapYear = false;
					int resultBy4Divide = Year % 4;
					if (resultBy4Divide == 0) {
						leapYear = true;
						
						int resultBy100Divide = Year % 100;
						if (resultBy100Divide == 0) {
							leapYear = false;
							int resultBy400Divide = Year % 400;
							if (resultBy400Divide == 0)
								leapYear = true;
						}
					}			
					
					if (!leapYear) { 			
						if ( (Month == 1) && (Date == 29) ) {
							Date = 28;					
							//adjustmentDayOfMonthField(1);							
					    	TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(DAYOFMONTH_FIELD_TEXTURE, 
					    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_DAYOFMONTH_FIELD_MSG, 1);
					    	Message msg = Message.obtain();
					    	msg.obj = gestureMsg;    	
					    	mRenderThreadHandler.sendMessage(msg);
						}
					}
				}
				
				startTimeMsg = Message.obtain();	
				startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, Year, 0, 0, 0, 0, 0);
				mTimeMsgHandler.sendMessage(startTimeMsg);	
			}
			break;
		case MONTH_FIELD_TEXTURE:		
			daysOfThisMonth = getActualMaximumOfMonth(mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerYearFieldValue);			
			
			// *mRepeatEndTimePickerMinYearFieldValue < mRepeatEndTimePickerYearFieldValue ��Ȳ�� �����Ѵ�.
			// *mRepeatEndTimePickerMinYearFieldValue > mRepeatEndTimePickerYearFieldValue ��Ȳ�� �����Ѵ�.			
			//  : �� ��Ȳ�� Year Status Machine�� ���°� ���� idle�̿��� ���¸� �����ϰ� �ִٴ� ���� �ǹ��Ѵ�.
			//   �׷��Ƿ� Year Field�� idle �̿��� ���� ���� ��,
			//   messageShoot�� Year Field���� Month Field�� ��ȿ ���¸� Ȯ�� �� Month Field Adjustment ��ġ ���θ� ������ ���̴�
			// *�Ʒ� ��Ȳ���� 
			if (mRepeatEndTimePickerYearFieldValue == mRepeatEndTimePickerMinYearFieldValue) {
				if (mRepeatEndTimePickerMinMonthFieldValue > mRepeatEndTimePickerMonthFieldValue) {					
					TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(MONTH_FIELD_TEXTURE, 
			    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG, 0);
			    	Message msg = Message.obtain();
			    	msg.obj = gestureMsg;			    	
			    	mRenderThreadHandler.sendMessage(msg);
			    	return;////////////////////////////////////////////////////////////////////////////////
				}
				else if (mRepeatEndTimePickerMinMonthFieldValue == mRepeatEndTimePickerMonthFieldValue) {
					if (mRepeatEndTimePickerMinDayOfMonthFieldValue > mRepeatEndTimePickerDayOfMonthFieldValue) {						
						TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(DAYOFMONTH_FIELD_TEXTURE, 
				    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG, 0);
				    	Message msg = Message.obtain();
				    	msg.obj = gestureMsg;				    	
				    	mRenderThreadHandler.sendMessage(msg);				    	
					}
				}
			}
			else {
				//Month = mRepeatEndTimePickerMonthFieldValue;
				//Date = mRepeatEndTimePickerDayOfMonthFieldValue;
				// ���⼭ DAY_OF_MONTH �ʵ��� Valid�� üũ�ؾ� �Ѵ�			
				// �Ʒ� ���� Ȯ���ؾ� �Ѵ�			
				// if) mCurrentGradationOfDayOfMonthMiniTimePicker == 31,
				//     mDateFieldCalendar.MONTH = 4��
				//     mDateFieldCalendar.DATE[Date] = 28��
				//     �Ʒ� �ڵ�� �������� �߻���Ų��
				//     mDateFieldCalendar.DATE ����� : 30�� �� �ȴ�!!!
				// S) (Date == daysOfThisMonth) �ڵ带 �߰��Ͽ���!!! <----�ƴϴ� �ٽ� ����!!!
				if (mCurrentGradationOfDayOfMonthMiniTimePicker > daysOfThisMonth) {	
					if (mRepeatEndTimePickerDayOfMonthFieldValue != daysOfThisMonth)
						mRepeatEndTimePickerDayOfMonthFieldValue = daysOfThisMonth;
					
					int autoAdjustMentValue = mCurrentGradationOfDayOfMonthMiniTimePicker - daysOfThisMonth;				
					adjustmentDayOfMonthField(autoAdjustMentValue);
				}	
				// if) ���� mRepeatEndTimePickerMonthFieldValue = 4�� �Ǵ� 6�� �Ǵ� �ƽø� ������ 30���� ����� ��
				//     mRepeatEndTimePickerMonthFieldValue = 5 �� ���� 
				//	   mRepeatEndTimePickerDayOfMonthFieldValue = 30��
				//     mCurrentGradationOfDayOfMonthMiniTimePicker = 31��
				// if) ���� mRepeatEndTimePickerMonthFieldValue = 2���� ��� �ƽø� ������ 28�� �Ǵ� 29���� ���
				//     mRepeatEndTimePickerMonthFieldValue = 3 �� ���� 
				//	   mRepeatEndTimePickerDayOfMonthFieldValue = 28 �Ǵ� 29��
				//     mCurrentGradationOfDayOfMonthMiniTimePicker = 29, 30, 31��
				else if (mCurrentGradationOfDayOfMonthMiniTimePicker != mRepeatEndTimePickerDayOfMonthFieldValue) {
					if (mCurrentGradationOfDayOfMonthMiniTimePicker <= daysOfThisMonth) {
						mRepeatEndTimePickerDayOfMonthFieldValue = mCurrentGradationOfDayOfMonthMiniTimePicker;
						int dayOfMonthField =  DAYOFMONTH_FIELD_TEXTURE;
				    	TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(dayOfMonthField, 
				    			TimePickerUserActionMessage.TIMEPICKER_UPDATE_DAYOFMONTH_FIELD_MSG, 0);
				    	Message msg = Message.obtain();
				    	msg.obj = gestureMsg;
				    	
				    	mRenderThreadHandler.sendMessage(msg); 
					}
				}
			}
			
			// �Ʒ� RepeatEnd Item�� text update message ó�� �κ��� �ٽ� �ѹ� �����ؾ� �Ѵ�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			startTimeMsg = Message.obtain();	
			startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, 0, mRepeatEndTimePickerMonthFieldValue, 0, 0, 0, 0);
			mTimeMsgHandler.sendMessage(startTimeMsg);			
			break;
		case DAYOFMONTH_FIELD_TEXTURE:			
			Date = mRepeatEndTimePickerDayOfMonthFieldValue;
			
			if (mRepeatEndTimePickerYearFieldValue == mRepeatEndTimePickerMinYearFieldValue) {				
			    if (mRepeatEndTimePickerMonthFieldValue == mRepeatEndTimePickerMinMonthFieldValue) {
					if (mRepeatEndTimePickerMinDayOfMonthFieldValue > mRepeatEndTimePickerDayOfMonthFieldValue) {
						TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(DAYOFMONTH_FIELD_TEXTURE, 
				    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG, 0);
				    	Message msg = Message.obtain();
				    	msg.obj = gestureMsg;				    	
				    	mRenderThreadHandler.sendMessage(msg);
					}
					else {
						startTimeMsg = Message.obtain();					
						startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, 0, 0, Date, 0, 0, 0);
						mTimeMsgHandler.sendMessage(startTimeMsg);
					}
			    }
			    else {
			    	startTimeMsg = Message.obtain();					
					startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, 0, 0, Date, 0, 0, 0);
					mTimeMsgHandler.sendMessage(startTimeMsg);
			    }
			}			
			else { // (mRepeatEndTimePickerYearFieldValue > mRepeatEndTimePickerMinYearFieldValue) 
				   // or (mRepeatEndTimePickerYearFieldValue < mRepeatEndTimePickerMinYearFieldValue) : �� ���� ���� Year Field�� idle ���� �̿��� ������ �������� ������ ����ȴ�
				startTimeMsg = Message.obtain();					
				startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, 0, 0, Date, 0, 0, 0);
				mTimeMsgHandler.sendMessage(startTimeMsg);
				
				if (mAdjustmentDayOfMonthField) {
					mAdjustmentDayOfMonthField = false;				
					daysOfThisMonth = getActualMaximumOfMonth(mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerYearFieldValue);
					
					if(mCurrentGradationOfDayOfMonthMiniTimePicker > daysOfThisMonth) {				
						int autoAdjustMentValue = mCurrentGradationOfDayOfMonthMiniTimePicker - daysOfThisMonth;								
						mDayOfMonthFieldAdjustmentComputeThread = new FieldAdjustmentComputeThread(DAYOFMONTH_FIELD_TEXTURE, autoAdjustMentValue);
						mDayOfMonthFieldAdjustmentComputeThread.setDaemon(true);
						mDayOfMonthFieldAdjustmentComputeThread.start();	
					}
				}
			}
			
			
			
			
			if ( (mRepeatEndTimePickerYearFieldValue == mRepeatEndTimePickerMinYearFieldValue) &&
					(mRepeatEndTimePickerMonthFieldValue == mRepeatEndTimePickerMinMonthFieldValue) ){
				if (mRepeatEndTimePickerMinDayOfMonthFieldValue > mRepeatEndTimePickerDayOfMonthFieldValue) {
					TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(DAYOFMONTH_FIELD_TEXTURE, 
			    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_BY_EXCEED_MIN_MSG, 0);
			    	Message msg = Message.obtain();
			    	msg.obj = gestureMsg;
			    	
			    	mRenderThreadHandler.sendMessage(msg);
				}
				else {
					startTimeMsg = Message.obtain();					
					startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, 0, 0, Date, 0, 0, 0);
					mTimeMsgHandler.sendMessage(startTimeMsg);
				}
			}			
			else {
				startTimeMsg = Message.obtain();					
				startTimeMsg.obj = new TimerPickerUpdateData(mTimePickerID, updateField, 0, 0, Date, 0, 0, 0);
				mTimeMsgHandler.sendMessage(startTimeMsg);
				
				if (mAdjustmentDayOfMonthField) {
					mAdjustmentDayOfMonthField = false;				
					daysOfThisMonth = getActualMaximumOfMonth(mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerYearFieldValue);
					
					if(mCurrentGradationOfDayOfMonthMiniTimePicker > daysOfThisMonth) {				
						int autoAdjustMentValue = mCurrentGradationOfDayOfMonthMiniTimePicker - daysOfThisMonth;								
						mDayOfMonthFieldAdjustmentComputeThread = new FieldAdjustmentComputeThread(DAYOFMONTH_FIELD_TEXTURE, autoAdjustMentValue);
						mDayOfMonthFieldAdjustmentComputeThread.setDaemon(true);
						mDayOfMonthFieldAdjustmentComputeThread.start();	
					}
				}
			}
			break;
		default:
			return;
		}		
    }
    
    FieldAdjustmentComputeThread mDayOfMonthFieldAdjustmentComputeThread;
    
    public void adjustmentDayOfMonthField(int value) {    	
    	int updateField = DAYOFMONTH_FIELD_TEXTURE;
    	TimePickerUserActionMessage gestureMsg = new TimePickerUserActionMessage(updateField, 
    			TimePickerUserActionMessage.TIMEPICKER_AUTO_ADJUSTMENT_DAYOFMONTH_FIELD_MSG, value);
    	Message msg = Message.obtain();
    	msg.obj = gestureMsg;    	
    	mRenderThreadHandler.sendMessage(msg); 	
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
    
	private static final int VECTOR_COMPONENTS_PER_POSITION = 3; // VERTEX�� ��ġ �Ӽ��� �������(x,y,z)
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
							
			int totalVerticesNumbers = ROW_MESH * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * COLUMN_MESH);			
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
    		
			int totalVerticesNumbers = ROW_MESH * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * COLUMN_MESH);			
			mFbPositions = ByteBuffer.allocateDirect(totalVerticesNumbers * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();				
			
			float x_dif = widthRatio / ROW_MESH;												
			mPointNumbers = buildVerticesTexturePData(fbPoints, leftmost, x_dif);
		}   
		
		private int buildVerticesTexturePData(float fbPoints[], float leftmost, float x_dif) {			
			int pointNumbers = 0;
			float x = 0;        		
			int index = 0;
			for (int i=0; i<ROW_MESH; i++) {  // x�� ���� ȸ��
				// mesh�� �����ϴ� �ϳ��� ������(�ﰢ������ �� ���� ������ �簢������)��...6���� vertices�� �ʿ��ϴ�	
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
			
			int totalTexCoordinateDataNumbers = (VECTOR_COMPONENTS_PER_TEXCOORDINATE * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS);				
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
		int totalTexCoordinateDataNumbers = ROW_MESH * (VECTOR_COMPONENTS_PER_TEXCOORDINATE * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * COLUMN_MESH);
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
		
		float[] Points = new float[ROW_MESH * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * COLUMN_MESH)];
		
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
	
	
	private ArrayList<FieldObjectVertices> makeYearVerticesTexturePosDataOfDateFields(double yAxisDateField, double nextYAxisDateField, int year) {
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
		int yearTextTextureHandle = mYearTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
		rightmost = m_yearFieldRightMostVPRattio;
		leftmost = rightmost - mTimePickerTextWidthRatio;	
		
		FieldObjectVertices yearTextObject = new FieldObjectVertices(yearTextTextureHandle, Points, 
				  													 leftmost, rightmost, 																			          
				  													 mTimePickerTextWidthRatio);	    		
		ObjList.add(ObjListIndex++, yearTextObject);
		/////////////////////////////////////////////////////////////////////////////////////////////////////
			
		int targetNumeral = year;
		String numeralString = String.valueOf(targetNumeral);
		String cutThousands = numeralString.substring(0, 1);
		String cutHundreds = numeralString.substring(1, 2);	
		String cutTens = numeralString.substring(2, 3);	
		String cutOnes = numeralString.substring(3, 4);	
		int numberOfThousandsOfNumeral = Integer.parseInt(cutThousands);
		int numberOfHundredsOfNumeral = Integer.parseInt(cutHundreds);
		int numberOfTensOfNumeral = Integer.parseInt(cutTens);
		int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);
		int NumeralTexts[] = new int[4];
		NumeralTexts[0] = numberOfOnesOfNumeral;
		NumeralTexts[1] = numberOfTensOfNumeral;
		NumeralTexts[2] = numberOfHundredsOfNumeral;
		NumeralTexts[3] = numberOfThousandsOfNumeral;
		for (int i=0; i<4; i++) {
			leftmost =  makeNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
	                                              leftmost, NumeralTexts[i], Points, 
                                                  ObjList, ObjListIndex++);				
		}	
		
		return ObjList;
	}
	
	private ArrayList<ArrayList<FieldObjectVertices>> buildYearFieldsVerticesAndTexturePosData(double CurrentYearFieldLowermostItemTopSideDegree, int year) {		
		int mustBeDrawItemCount = 0;		
		ArrayList<ArrayList<FieldObjectVertices>> YearFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		double[] yAxisDateField = makeYAxisField(CurrentYearFieldLowermostItemTopSideDegree);
		mustBeDrawItemCount = yAxisDateField.length - 1;		
		
		year = year + 4;
		
		for (int i=0; i<mustBeDrawItemCount; i++) {
			YearFieldRowObjectsVerticesList.add(i, makeYearVerticesTexturePosDataOfDateFields(yAxisDateField[i], yAxisDateField[i+1], year));			
			
			year = year - 1;
		}  
		
		return YearFieldRowObjectsVerticesList;
	}
	
	private ArrayList<HighLightFieldObjectVertices> makeYearFieldHighLightVerticesAndTexturePosData(float topmost, float bottommost, 
		                                                                                            float textureTop, float textureBottom, 
		                                                                                            int year) {
		float leftmost = 0;
		float rightmost = 0;		
		ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
		int ObjListIndex = 0;
		
		
		int yearTextTextureHandle = mCenterYearTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
		rightmost = m_yearFieldRightMostVPRattio;
		leftmost = rightmost - mTimePickerTextWidthRatio;	
		
		HighLightFieldObjectVertices yearTextObject = new HighLightFieldObjectVertices(yearTextTextureHandle, 
																					   leftmost, rightmost, 																			          
																					   topmost, bottommost,
																					   textureTop, textureBottom);	
		ObjList.add(ObjListIndex++, yearTextObject);		
		//////////////////////////////////////////////////////////////////////////////////////////////////		
		int targetNumeral = year;
		String numeralString = String.valueOf(targetNumeral);
		String cutThousands = numeralString.substring(0, 1);
		String cutHundreds = numeralString.substring(1, 2);	
		String cutTens = numeralString.substring(2, 3);	
		String cutOnes = numeralString.substring(3, 4);	
		int numberOfThousandsOfNumeral = Integer.parseInt(cutThousands);
		int numberOfHundredsOfNumeral = Integer.parseInt(cutHundreds);
		int numberOfTensOfNumeral = Integer.parseInt(cutTens);
		int numberOfOnesOfNumeral = Integer.parseInt(cutOnes);
		int NumeralTexts[] = new int[4];
		NumeralTexts[0] = numberOfOnesOfNumeral;
		NumeralTexts[1] = numberOfTensOfNumeral;
		NumeralTexts[2] = numberOfHundredsOfNumeral;
		NumeralTexts[3] = numberOfThousandsOfNumeral;
		for (int i=0; i<4; i++) {
			leftmost =  makeHighLightNumeralTextVertexData(LEFT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
					                                       leftmost, topmost, bottommost, textureTop, textureBottom,
					                                       NumeralTexts[i], ObjList, ObjListIndex++);			
		}		
		
		return ObjList;
	}
	
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildYearFieldsHighLightVerticesTexturePosData() {   		
		ArrayList<ArrayList<HighLightFieldObjectVertices>> YearFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();				
		int calYear = mRepeatEndTimePickerYearFieldValue;
		
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;		
		
		double targetTopSideDegree = mCurrentYearFieldLowermostItemTopSideDegree + CENTERITEM_BOTTOM_DEGREE;
		
		if (targetTopSideDegree == CENTERITEM_TOP_DEGREE) { // highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�        	
			
			topmost = mCenterItemRegionTopRatio;
			bottommost = mCenterItemRegionBottomRatio;
			textureTop = 0.0f;
			textureBottom = 1.0f;
			
			YearFieldHighLightObjectsVerticesList.add(0, makeYearFieldHighLightVerticesAndTexturePosData(topmost, bottommost, textureTop, textureBottom, calYear));			
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
				
				YearFieldHighLightObjectsVerticesList.add(i, makeYearFieldHighLightVerticesAndTexturePosData(topmost, bottommost, textureTop, textureBottom, calYear));
								
    			// ���� ������ date ������ ��� ���� -1���� ���Ѵ�
    			//CalCalendar.add(Calendar.YEAR, -1); 
				calYear = calYear - 1;
			}				
		}
		
		return YearFieldHighLightObjectsVerticesList;    		
	}
		
		
	private ArrayList<ArrayList<FieldObjectVertices>> buildMonthFieldsVerticesAndTexturePosData(double CurrentMonthFieldLowermostItemTopSideDegree, int month) {
		int calMonth = month;
		int mustBeDrawItemCount = 0;			
		ArrayList<ArrayList<FieldObjectVertices>> MonthFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		double[] yAxisHourField = makeYAxisField(CurrentMonthFieldLowermostItemTopSideDegree);
		mustBeDrawItemCount = yAxisHourField.length - 1;		
		
		calMonth = addedMonthForCal(calMonth, 4); 
		
		float monthFieldOfPickerLeftMost = m_monthFieldPickerLeftMostVPRatio;	
		float leftmost = 0;
		float rightmost = 0;
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
			int monthNumbers = TimerPickerTimeText.transformMonthToRealNumber(calMonth);
			//Log.i("tag", "monthNumbers=" + String.valueOf(monthNumbers));					
			if (monthNumbers > 9) {		
				rightmost = makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
										              monthFieldOfPickerLeftMost, monthNumbers, Points, 
                                                      ObjList, ObjListIndex++);	
				ObjListIndex++;
			}
			else {
				rightmost = makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
										              monthFieldOfPickerLeftMost, monthNumbers, Points, 
                                                      ObjList, ObjListIndex++);		
			}
							
			/////////////////////////////////////////////////////////////////////////////////////////////////////
			int monthTextTextureHandle = mMonthTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			
			FieldObjectVertices monthTextObject = new FieldObjectVertices(monthTextTextureHandle, Points, 
																		  leftmost, rightmost, 																			          
																		  mTimePickerTextWidthRatio);	    		
			ObjList.add(ObjListIndex, monthTextObject);
			
			calMonth = addedMonthForCal(calMonth, -1); 
			
			MonthFieldRowObjectsVerticesList.add(i, ObjList);
		}		
		
		return MonthFieldRowObjectsVerticesList;			
	}	
	
	
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildMonthFieldsHighLightVerticesTexturePosData() {   		
		ArrayList<ArrayList<HighLightFieldObjectVertices>> MonthFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
		
		int calMonth = mRepeatEndTimePickerMonthFieldValue;
		
		float monthFieldOfPickerLeftMost = m_monthFieldPickerLeftMostVPRatio /*- mHourFieldHighLightInterpolationRatio*/;		
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;
		
		double targetTopSideDegree = mCurrentMonthFieldLowermostItemTopSideDegree + CENTERITEM_BOTTOM_DEGREE;
		
		if (targetTopSideDegree == CENTERITEM_TOP_DEGREE) {
			int ObjListIndex = 0;
			// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
			topmost = mCenterItemRegionTopRatio;
			bottommost = mCenterItemRegionBottomRatio;
			textureTop = 0.0f;
			textureBottom = 1.0f;
			
			//////////////////////////////////////////////////////////////////////////////////////////////////
			int monthNumbers = TimerPickerTimeText.transformMonthToRealNumber(calMonth);
				
			if (monthNumbers > 9) {	
				rightmost = makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
						                                       monthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
						                                       monthNumbers, ObjList, ObjListIndex++);	
				ObjListIndex++;
			}
			else {
				rightmost = makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
						                                       monthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
						                                       monthNumbers, ObjList, ObjListIndex++);				
			}					
			
			/////////////////////////////////////////////////////////////////////////////////////////////////////
			int monthTextTextureHandle = mCenterMonthTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			
			HighLightFieldObjectVertices monthTextObject = new HighLightFieldObjectVertices(monthTextTextureHandle, 
					                                                                        leftmost, rightmost, 																			          
					                                                                        topmost, bottommost,
					                                                                        textureTop, textureBottom);		    		
			ObjList.add(ObjListIndex, monthTextObject);
			
			MonthFieldHighLightObjectsVerticesList.add(0, ObjList);
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
    			int monthNumbers = TimerPickerTimeText.transformMonthToRealNumber(calMonth);
				
    			if (monthNumbers > 9) {	
    				rightmost = makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
    						                                       monthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
    						                                       monthNumbers, ObjList, ObjListIndex++);	
    				ObjListIndex++;
    			}
    			else {
    				rightmost = makeHighLightNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
    						                                       monthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
    						                                       monthNumbers, ObjList, ObjListIndex++);				
    			}					
    			
    			/////////////////////////////////////////////////////////////////////////////////////////////////////
    			int monthTextTextureHandle = mCenterMonthTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
    			leftmost = rightmost;
    			rightmost = leftmost + mTimePickerTextWidthRatio;
    			
    			HighLightFieldObjectVertices monthTextObject = new HighLightFieldObjectVertices(monthTextTextureHandle, 
    					                                                                        leftmost, rightmost, 																			          
    					                                                                        topmost, bottommost,
    					                                                                        textureTop, textureBottom);		    		
    			ObjList.add(ObjListIndex, monthTextObject);
    			
    			MonthFieldHighLightObjectsVerticesList.add(i, ObjList);   			
    			
    			// ���� ������ date ������ ��� ����
				//calCalendar.add(Calendar.MONTH, -1);	
    			calMonth = this.addedMonthForCal(calMonth, -1);
			}				
		}
		
		return MonthFieldHighLightObjectsVerticesList;    		
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
	
	private ArrayList<ArrayList<FieldObjectVertices>> buildDayOfMonthFieldsVerticesAndTexturePosData(double CurrentDayOfMonthFieldLowermostItemTopSideDegree) {
		int mustBeDrawItemCount = 0;			
		ArrayList<ArrayList<FieldObjectVertices>> DayOfMonthFieldRowObjectsVerticesList = new ArrayList<ArrayList<FieldObjectVertices>>();
		double[] yAxisHourField = makeYAxisField(CurrentDayOfMonthFieldLowermostItemTopSideDegree);
		mustBeDrawItemCount = yAxisHourField.length - 1;		
		
		float dayOfMonthFieldOfPickerLeftMost = m_dayOfMonthFieldPickerLeftMostVPRatio;	
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float lowermost = 0;				
		float[] Points = null;		
		
		int dayOfMonth = addedDayOfMonthForCal(mCurrentGradationOfDayOfMonthMiniTimePicker, 4);
		
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
			
			if (dayOfMonth > 9) {		
				rightmost = makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
						                              dayOfMonthFieldOfPickerLeftMost, dayOfMonth, Points, 
                                                      ObjList, ObjListIndex++);	
				ObjListIndex++;
			}
			else {
				rightmost = makeNumeralTextVertexData(RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
						                              dayOfMonthFieldOfPickerLeftMost, dayOfMonth, Points, 
                                                      ObjList, ObjListIndex++);		
			}
							
			/////////////////////////////////////////////////////////////////////////////////////////////////////
			int dayOfMonthTextTextureHandle = mDateTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			
			FieldObjectVertices dayOfMonthTextObject = new FieldObjectVertices(dayOfMonthTextTextureHandle, Points, 
																		       leftmost, rightmost, 																			          
																		       mTimePickerTextWidthRatio);	    		
			ObjList.add(ObjListIndex, dayOfMonthTextObject);
			
			dayOfMonth = addedDayOfMonthForCal(dayOfMonth, -1);
			
			DayOfMonthFieldRowObjectsVerticesList.add(i, ObjList);
		}		
		
		return DayOfMonthFieldRowObjectsVerticesList;			
	}	
	
	
	private ArrayList<ArrayList<HighLightFieldObjectVertices>> buildDayOfMonthFieldsHighLightVerticesTexturePosData() {		
		
		int daysOfThisMonth = this.getActualMaximumOfMonth(mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerYearFieldValue);
		int dayOfMonth = mCurrentGradationOfDayOfMonthMiniTimePicker;
				
		ArrayList<ArrayList<HighLightFieldObjectVertices>> DayOfMonthFieldHighLightObjectsVerticesList = new ArrayList<ArrayList<HighLightFieldObjectVertices>>();
		float dayOfMonthFieldOfPickerLeftMost = m_dayOfMonthFieldPickerLeftMostVPRatio /*- mHourFieldHighLightInterpolationRatio*/;		
		float upperItemTopmost = 0;
		float lowerItemTopmost = 0;
		float upperItemBottommost = 0;
		float lowerItemBottommost = 0;
		float leftmost = 0;
		float rightmost = 0;
		float topmost = 0;
		float bottommost = 0;	
		float textureTop = 0;
		float textureBottom = 0;
		
		double targetTopSideDegree = mCurrentDayOfMonthFieldLowermostItemTopSideDegree + CENTERITEM_BOTTOM_DEGREE;		
		
		if (targetTopSideDegree == CENTERITEM_TOP_DEGREE) {
			int ObjListIndex = 0;
			// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
			topmost = mCenterItemRegionTopRatio;
			bottommost = mCenterItemRegionBottomRatio;
			textureTop = 0.0f;
			textureBottom = 1.0f;			
			int dayOfMonthTextTextureHandle = 0;			
			boolean highLight = true;
			
			if (dayOfMonth > daysOfThisMonth) {
				highLight = false;
				dayOfMonthTextTextureHandle = mDateTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
			}
			else {
				dayOfMonthTextTextureHandle = mCenterDateTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
			}
			
			if (dayOfMonth > 9) {	
				rightmost = makeHighLightNumeralTextVertexData(highLight, RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
						                                       dayOfMonthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
						                                       dayOfMonth, ObjList, ObjListIndex++);	
				ObjListIndex++;
			}
			else {
				rightmost = makeHighLightNumeralTextVertexData(highLight, RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
						                                       dayOfMonthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
						                                       dayOfMonth, ObjList, ObjListIndex++);				
			}			
			
			leftmost = rightmost;
			rightmost = leftmost + mTimePickerTextWidthRatio;
			
			HighLightFieldObjectVertices dayOfMonthTextObject = new HighLightFieldObjectVertices(dayOfMonthTextTextureHandle, 
					                                                                             leftmost, rightmost, 																			          
					                                                                             topmost, bottommost,
					                                                                             textureTop, textureBottom);
			ObjList.add(ObjListIndex, dayOfMonthTextObject);		
			
			DayOfMonthFieldHighLightObjectsVerticesList.add(0, ObjList);
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
				
				// highlight�� ��Ȯ�� ������ �������� ��ġ�Ѵ�
    			ArrayList<HighLightFieldObjectVertices> ObjList = new ArrayList<HighLightFieldObjectVertices>();
    			
				int ObjListIndex = 0;
				boolean highLight = true;
				int dayOfMonthTextTextureHandle = 0;								
    			
    			if (dayOfMonth > daysOfThisMonth) {
    				highLight = false;
    				dayOfMonthTextTextureHandle = mDateTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
    			}
    			else {
    				dayOfMonthTextTextureHandle = mCenterDateTextTextTextureDataHandle; // �� �ε������� �ؽ�Ʈ
    			}
    			
    			if (dayOfMonth > 9) {	
    				rightmost = makeHighLightNumeralTextVertexData(highLight, RIGHT_INCREMENT_DIRECTION, TENS_NUMERAL_UNIT, 
    						                                       dayOfMonthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
    						                                       dayOfMonth, ObjList, ObjListIndex++);	
    				ObjListIndex++;
    			}
    			else {
    				rightmost = makeHighLightNumeralTextVertexData(highLight, RIGHT_INCREMENT_DIRECTION, ONE_NUMERAL_UNIT, 
    						                                       dayOfMonthFieldOfPickerLeftMost, topmost, bottommost, textureTop, textureBottom,
    						                                       dayOfMonth, ObjList, ObjListIndex++);				
    			}	   			
    			
    			leftmost = rightmost;
    			rightmost = leftmost + mTimePickerTextWidthRatio;
    			
    			HighLightFieldObjectVertices dayOfMonthTextObject = new HighLightFieldObjectVertices(dayOfMonthTextTextureHandle, 
    					                                                                             leftmost, rightmost, 																			          
    					                                                                             topmost, bottommost,
    					                                                                             textureTop, textureBottom);		    		
    			ObjList.add(ObjListIndex, dayOfMonthTextObject);    			
    			
    			DayOfMonthFieldHighLightObjectsVerticesList.add(i, ObjList);   			
    			
    			// ���� ������ date ������ ��� ����    			
    			dayOfMonth = addedDayOfMonthForCal(dayOfMonth, -1);
			}				
		}
		
		return DayOfMonthFieldHighLightObjectsVerticesList;    		
	}
	
	
	
	public void setCurrentFieldLowermostItemTopSideDegree(int field, double degree) {
		switch(field) {
    	
    	case YEAR_FIELD_TEXTURE:
    		mCurrentYearFieldLowermostItemTopSideDegree = degree;
    		break;
    	case MONTH_FIELD_TEXTURE:
    		mCurrentMonthFieldLowermostItemTopSideDegree = degree;
    		break;
    	case DAYOFMONTH_FIELD_TEXTURE:
    		mCurrentDayOfMonthFieldLowermostItemTopSideDegree = degree;
    		break;
    	default:
    		break;
    	}
	}
	
	public void setCurrentFieldUppermostItemBottomSideDegree(int field, double degree) {
		switch(field) {
    	
    	case YEAR_FIELD_TEXTURE:
    		mCurrentYearFieldUppermostItemBottomSideDegree = degree;
    		break;
    	case MONTH_FIELD_TEXTURE:
    		mCurrentMonthFieldUppermostItemBottomSideDegree = degree;
    		break;
    	case DAYOFMONTH_FIELD_TEXTURE:
    		mCurrentDayOfMonthFieldUppermostItemBottomSideDegree = degree;
    		break;
    	default:
    		break;
    	}
	}
	
	
	public double getCurrentFieldLowermostItemTopSideDegree(int field) {
		double currentDegree = 0;
		
		switch(field) {
    	
    	case YEAR_FIELD_TEXTURE:
    		currentDegree = mCurrentYearFieldLowermostItemTopSideDegree;
    		break;
    	case MONTH_FIELD_TEXTURE:
    		currentDegree = mCurrentMonthFieldLowermostItemTopSideDegree;
    		break;
    	case DAYOFMONTH_FIELD_TEXTURE:
    		currentDegree = mCurrentDayOfMonthFieldLowermostItemTopSideDegree;
    		break;
    	default:
    		break;
    	}
		
		return currentDegree;
	}
	
	public double getCurrentFieldUppermostItemBottomSideDegree(int field) {
		double currentDegree = 0;
		
		switch(field) {
    	
    	case YEAR_FIELD_TEXTURE:
    		currentDegree = mCurrentYearFieldUppermostItemBottomSideDegree;
    		break;
    	case MONTH_FIELD_TEXTURE:
    		currentDegree = mCurrentMonthFieldUppermostItemBottomSideDegree;
    		break;
    	case DAYOFMONTH_FIELD_TEXTURE:
    		currentDegree = mCurrentDayOfMonthFieldUppermostItemBottomSideDegree;
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
	
	public static final int SCALAR_UP_TO_DOWN_WARD = 1;
	public static final int SCALAR_DOWN_TO_UP_WARD = 2;
	
	int mRepeatEndTimePickerMinYearFieldValue;
	int mRepeatEndTimePickerMinMonthFieldValue;
	int mRepeatEndTimePickerMinDayOfMonthFieldValue;
	int mRepeatEndTimePickerYearFieldValue;
	int mRepeatEndTimePickerMonthFieldValue;
	int mRepeatEndTimePickerDayOfMonthFieldValue;
	
	public void setFieldCalendar(int field, int value, int updownTo) {
		switch(field) {
    	
    	case YEAR_FIELD_TEXTURE:
    		mRepeatEndTimePickerYearFieldValue = mRepeatEndTimePickerYearFieldValue + value;
    		break;
    	case MONTH_FIELD_TEXTURE:
    		int monthNumbers = mRepeatEndTimePickerMonthFieldValue;
    		int addedMonthNumbers = monthNumbers + value;
    		//int newMonthNumbers = 0;
    		if (addedMonthNumbers > 11) {
    			// 12�� 0�� �Ǿ�� �Ѵ�..12 + x = 0 -> x = -12
    			// 13�� 1�� �Ǿ�� �Ѵ�..13 + x = 1 -> x = -12    			
    			mRepeatEndTimePickerMonthFieldValue = addedMonthNumbers - 12;
    			//mRepeatEndTimePickerMonthFieldValue = newMonthNumbers;  
    		}
    		else if (addedMonthNumbers < 0) {
    			// -1�� 11�� �Ǿ�� �Ѵ�..-1 + x = 11 -> x = 11 + 1 -> x = 12
    			// -2�� 10�� �Ǿ�� �Ѵ�..-2 + x = 10 -> x = 10 + 2 -> x = 12    			
    			mRepeatEndTimePickerMonthFieldValue = addedMonthNumbers + 12;
    			//mRepeatEndTimePickerMonthFieldValue = newMonthNumbers;  
    		}
    		else {
    			mRepeatEndTimePickerMonthFieldValue = addedMonthNumbers;
    		}
    		    		
    		break;
    	case DAYOFMONTH_FIELD_TEXTURE:    
    		//Log.i("tag", "setMiniTimePickerFieldCalendar : value=" + String.valueOf(value));
    		// ��������� Ÿ �ʵ��� mDateFieldCalendar�� ���� side-effect�� ����� �ʿ䰡 ����
    		// �ش���� ����/���ҷ� ���� day of month �� ���� side-effect�� ����
    		// �ֳĸ� ���� ������ ���� �߰��Ǿ��� �����̴�
    		mCurrentGradationOfDayOfMonthMiniTimePicker = addedDayOfMonthForCal(mCurrentGradationOfDayOfMonthMiniTimePicker, value);    		
    		 		 		
    		int daysOfThisMonth =  getActualMaximumOfMonth(mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerYearFieldValue);    		
    		
    		if (mCurrentGradationOfDayOfMonthMiniTimePicker > daysOfThisMonth) {
    			mRepeatEndTimePickerDayOfMonthFieldValue = daysOfThisMonth;
    			// 1�Ͽ��� 31�Ϸ� ������ ��,,,
    			// �ش���� 30�ϱ������,,,
    			// �ڵ����� 31�Ͽ��� 30�Ϸ� �̵��ϰ� �ؾ� �Ѵ�... 			
    			if (updownTo == SCALAR_UP_TO_DOWN_WARD) {
    				mAdjustmentDayOfMonthField = true;    				
    			}
    		}
    		else {    			
    			mRepeatEndTimePickerDayOfMonthFieldValue = mCurrentGradationOfDayOfMonthMiniTimePicker;
    		}
    		    		    		
    		break;
    	default:
    		break;
    	}
	}
	
	public ArrayList<ArrayList<FieldObjectVertices>> buildVerticesTexturePosDataOfField(int field) {		
		ArrayList<ArrayList<FieldObjectVertices>> FieldRowObjectsVerticesList = null;
		
		switch(field) {    	
    	case YEAR_FIELD_TEXTURE:
    		FieldRowObjectsVerticesList = buildYearFieldsVerticesAndTexturePosData(mCurrentYearFieldLowermostItemTopSideDegree, mRepeatEndTimePickerYearFieldValue);
    		break; 
    	case MONTH_FIELD_TEXTURE:
    		FieldRowObjectsVerticesList = buildMonthFieldsVerticesAndTexturePosData(mCurrentMonthFieldLowermostItemTopSideDegree, mRepeatEndTimePickerMonthFieldValue);
    		break; 
    	case DAYOFMONTH_FIELD_TEXTURE:
    		FieldRowObjectsVerticesList = buildDayOfMonthFieldsVerticesAndTexturePosData(mCurrentDayOfMonthFieldLowermostItemTopSideDegree);
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
			boolean mustBeInterpolation, boolean needCheckingMoveDirection) {        	
    	float scalarABS = 0;
    	double rotateTheta = 0;        	
    	int timewarp = 0;        	
    	double UppermostItemBottomSideDegree = 0;
    	double LowermostItemTopSideDegree = 0;
    	int prvAMPM = 0;    	    	
    	
    	
    	
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
        		
        		if (needCheckingMoveDirection)
        			setFieldCalendar(field, timewarp, SCALAR_DOWN_TO_UP_WARD);
        		else
        			setFieldCalendar(field, timewarp, 0);
    		}
    	}
    	else {   // finger move downward : up -> down 
    		if (mustBeInterpolation) { // finger move upward : down -> up ��쿡�� ��Ȯ�ϰ� �����Ѵ�, �׷��� �� �ݴ� ��Ȳ������ ��Ȯ�ϰ� �������� �ʴ´�            			
    			mustBeInterpolate(field);
    			setFieldCalendar(field, -1, 0); // 0���� �����ϴ� ������ DAY_OF_MONTH �ʵ��� ADJUSTMENT ���궧���̴�.
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
        		
        		if (needCheckingMoveDirection)
        			setFieldCalendar(field, -timewarp, SCALAR_UP_TO_DOWN_WARD);
        		else
        			setFieldCalendar(field, -timewarp, 0);
    		}
    	}   	
    	
    	ArrayList<ArrayList<FieldObjectVertices>> FieldRowObjectsVerticesList = buildVerticesTexturePosDataOfField(field);    	
    	
    	return FieldRowObjectsVerticesList;
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
        
        drawMiniTimePicker();
	}   	
	
	public void drawMiniTimePicker() {
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);         
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		useAlphaChannel(true);
        int rowNumber = mYearFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mYearFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		FieldObjectVertices obj = ObjList.get(j);            		
        		bindTexture(obj.mTextureHandle);
        		drawArraysTexture(obj.mFbPositions, mFbFieldTextureCoordinates, obj.mPointNumbers);           		
        	}            	
        }         
        
        rowNumber = mMonthFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mMonthFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		FieldObjectVertices obj = ObjList.get(j);            		
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysTexture(obj.mFbPositions, mFbFieldTextureCoordinates, obj.mPointNumbers);            		
        	}            	
        }        
        
        rowNumber = mDayOfMonthFieldRowObjectsVerticesList.size();
        for (int i=0; i<rowNumber; i++) {
        	ArrayList<FieldObjectVertices> ObjList = mDayOfMonthFieldRowObjectsVerticesList.get(i);
        	
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
        
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        bindTexture(mCenterEraseBackgroundData.mTextureHandle);
        drawArraysElements(mCenterEraseBackgroundData.mFbPositions, mCenterEraseBackgroundData.mFbFieldTextureCoordinates, mCenterEraseBackgroundData.mFbIndicess);
        
        int ObjlistNumbers = mYearHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mYearHighLightFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		HighLightFieldObjectVertices obj = ObjList.get(j);
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysElements(obj.mFbPositions, obj.mFbFieldTextureCoordinates, obj.mFbIndicess);
        	}            	
        }          
        
        ObjlistNumbers = mMonthHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mMonthHighLightFieldRowObjectsVerticesList.get(i);
        	
        	int objNumber = ObjList.size();
        	for (int j=0; j<objNumber; j++) {
        		HighLightFieldObjectVertices obj = ObjList.get(j);
        		bindTexture(obj.mTextureHandle);            		
        		drawArraysElements(obj.mFbPositions, obj.mFbFieldTextureCoordinates, obj.mFbIndicess);
        	}            	
        }        
        
        ObjlistNumbers = mDayOfMonthHighLightFieldRowObjectsVerticesList.size();
        for (int i=0; i<ObjlistNumbers; i++) {
        	ArrayList<HighLightFieldObjectVertices> ObjList = mDayOfMonthHighLightFieldRowObjectsVerticesList.get(i);
        	
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
        GLES20.glVertexAttribPointer(mPositionHandle,   //index
        							 mDataSizePerVertex,//size
        							 GLES20.GL_FLOAT,   //type
        							 false,             //normalized
        							 0,                 //stride
        							 verticlesFB);      //pointer
        /*
                        ���� 	GL_INVALID_ENUM ������ ���� ó�� �ѹ� �߻��Ѵ�...������ �����ΰ�? 
         GL_INVALID_VALUE is generated if index is greater than or equal to GL_MAX_VERTEX_ATTRIBS.
		 GL_INVALID_VALUE is generated if size is not 1, 2, 3, or 4.
		 GL_INVALID_ENUM is generated if type is not an accepted value.
         GL_INVALID_VALUE is generated if stride is negative.
         */
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
		
		case YEAR_FIELD_TEXTURE:
			flingVector = m_yearFieldFlingmovementVector;
			break;
		case MONTH_FIELD_TEXTURE:
			flingVector = m_monthFieldFlingmovementVector;
			break;
		case DAYOFMONTH_FIELD_TEXTURE:
			flingVector = m_dayOfMonthFieldFlingmovementVector;
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
		case YEAR_FIELD_TEXTURE:
			flingVector = m_yearFieldFlingmovementVector;
			break;
		case MONTH_FIELD_TEXTURE:
			flingVector = m_monthFieldFlingmovementVector;
			break;
		case DAYOFMONTH_FIELD_TEXTURE:
			flingVector = m_dayOfMonthFieldFlingmovementVector;
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
		case YEAR_FIELD_TEXTURE:
			flingVector = m_yearFieldFlingmovementVector;
			break;
		case MONTH_FIELD_TEXTURE:
			flingVector = m_monthFieldFlingmovementVector;
			break;
		case DAYOFMONTH_FIELD_TEXTURE:
			flingVector = m_dayOfMonthFieldFlingmovementVector;
			break;
		default:
			return 0;
		}
		
		return flingVector.getMoveYScalar();
	}
	
	public float getYFlingVelocity(int field) {
		float velocity = 0;
		
		switch (field) {		
		case YEAR_FIELD_TEXTURE:
			velocity = m_yearFieldFlingmovementVector.m_fling_velocity_axis_y;
			break;
		case MONTH_FIELD_TEXTURE:
			velocity = m_monthFieldFlingmovementVector.m_fling_velocity_axis_y;
			break;
		case DAYOFMONTH_FIELD_TEXTURE:
			velocity = m_dayOfMonthFieldFlingmovementVector.m_fling_velocity_axis_y;
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

        // eglMakeCurrent binds context to the current rendering thread and to the draw and read surfaces
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
        
        mYearTextTextTextureDataHandle = loadTexture(mTimePickerYearTextBitmap);
        setMipmapFilters(mYearTextTextTextureDataHandle);
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
        
        mCenterYearTextTextTextureDataHandle = loadTexture(mTimePickerCenterYearTextBitmap);
        setMipmapFilters(mCenterYearTextTextTextureDataHandle);
        
        mCenterMonthTextTextTextureDataHandle = loadTexture(mTimePickerCenterMonthTextBitmap);
        setMipmapFilters(mCenterMonthTextTextTextureDataHandle);
        
        mCenterDateTextTextTextureDataHandle = loadTexture(mTimePickerCenterDateTextBitmap);       
        setMipmapFilters(mCenterDateTextTextTextureDataHandle);
        
        mEraseCenterBackgroundTextureDataHandle = loadTexture(mEraseBackgroundBitmap); 
        setMipmapFilters(mEraseCenterBackgroundTextureDataHandle);
        ///////////////////////////////////////////////////////////////////////////////////////     
        
        
        mYearFieldRowObjectsVerticesList = buildYearFieldsVerticesAndTexturePosData(mCurrentYearFieldLowermostItemTopSideDegree, mRepeatEndTimePickerYearFieldValue);
        mYearHighLightFieldRowObjectsVerticesList = buildYearFieldsHighLightVerticesTexturePosData();
        
        mMonthFieldRowObjectsVerticesList = buildMonthFieldsVerticesAndTexturePosData(mCurrentMonthFieldLowermostItemTopSideDegree, mRepeatEndTimePickerMonthFieldValue);
        mMonthHighLightFieldRowObjectsVerticesList = buildMonthFieldsHighLightVerticesTexturePosData();        
        
        mDayOfMonthFieldRowObjectsVerticesList = buildDayOfMonthFieldsVerticesAndTexturePosData(mCurrentDayOfMonthFieldLowermostItemTopSideDegree);
        mDayOfMonthHighLightFieldRowObjectsVerticesList = buildDayOfMonthFieldsHighLightVerticesTexturePosData();
        
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
    	GLES20.glDeleteProgram(mProgram);
    	
    	mEgl.eglMakeCurrent(mEglDisplay, 
    			EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, 
                EGL10.EGL_NO_CONTEXT);
    	mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
    	mEgl.eglDestroyContext(mEglDisplay, mEglContext);
    	mEgl.eglTerminate(mEglDisplay);    	
    	
        Log.i("tag", "RepeatEndTimePickerRender : GL Cleaned up");
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
	
    public void setMipmapFilters(int textureHandle) {
    	GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    	
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);		
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
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
    	
    	dispose(mYearTextTextTextureDataHandle);
    	
		dispose(mMonthTextTextTextureDataHandle);    	
		
		dispose(mDateTextTextTextureDataHandle);
		
    	
    	for (int i=0; i<10; i++) {    		
    		dispose(mCenterNumeralTextTextureDataHandles[i]);
    	}    	
    	
    	dispose(mCenterYearTextTextTextureDataHandle);
    	
    	dispose(mCenterMonthTextTextTextureDataHandle);    	
    	
    	dispose(mCenterDateTextTextTextureDataHandle);		
    	
    	dispose(mEraseCenterBackgroundTextureDataHandle);
    }
        
    public void dispose(int textureId) {	
    	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
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

    // GL error = 0x500 is GL_INVALID_ENUM
    // GL_INVALID_ENUM : Given when an enumeration parameter is not a legal enumeration for that function
    private void checkGlError() {
        final int error = mGl.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Log.e(TAG, "GL error = 0x" + Integer.toHexString(error));
        }
    } 
    
    int mScrollUpdateField = 0;
    float mLeftMonthFieldOfTimePicker;
	float mRightMonthFieldOfTimePicker;
	float mRightYearFieldOfTimePicker;
	float mLeftDayOfWeekFieldOfTimePicker;
	LockableScrollView mMainPageScrollView;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerYearField;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerMonthField;
	gestureScrollMovementVector m_scrollmovementVectorOfTimePickerDayOfMonthField;	
    public int getUpdateFieldOnTouch(float PointX) {
    	int updateField = 0;   	
    	
    	if ( PointX < mRightYearFieldOfTimePicker ) {
        	updateField = YEAR_FIELD_TEXTURE; 
        }
        else if ( (mLeftMonthFieldOfTimePicker <= PointX) && (PointX < mRightMonthFieldOfTimePicker) ) {
        	updateField = MONTH_FIELD_TEXTURE;
        }
        else {
        	updateField = DAYOFMONTH_FIELD_TEXTURE;
        }	       
    	
    	return updateField;
    }
    
    OnTouchListener mTimePickerTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {	
			int action=event.getAction();			
			
			if(action==MotionEvent.ACTION_DOWN) {
				//Log.i("tag", "RepeatEndTimePicker:ACTION_DOWN");
				mMainPageScrollView.setScrollingEnabled(false);//////////////////////////////////////////////////////////////////////////////////////////////////
				// field�� �ĺ��ؾ� �Ѵ�
				int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;	            
	            
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
				//Log.i("tag", "RepeatEndTimePicker:ACTION_MOVE");
				//Log.i("tag", "onTouch:x=" + String.valueOf(event.getX()) + ", y=" + String.valueOf(event.getY()));
				//return false; // onScroll msg�� �߻����� �ʴ´�
			}
			else if (action==MotionEvent.ACTION_UP) {
				//Log.i("tag", "RepeatEndTimePicker:ACTION_UP");
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
			
    	if ( PointX < mRightYearFieldOfTimePicker ) {	        	
        	mScrollUpdateField = YEAR_FIELD_TEXTURE; 
			m_scrollmovementVectorOfTimePickerYearField.resetScrollMovementVector();
			gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerYearField, event1.getY(), event2.getY());
			scrollingScalar = m_scrollmovementVectorOfTimePickerYearField.getMoveYScalar();
        }
        else if ( (mLeftMonthFieldOfTimePicker <= PointX) && (PointX < mRightMonthFieldOfTimePicker) ) {
        	mScrollUpdateField = MONTH_FIELD_TEXTURE;
        	m_scrollmovementVectorOfTimePickerMonthField.resetScrollMovementVector();
        	gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerMonthField, event1.getY(), event2.getY());
        	scrollingScalar = m_scrollmovementVectorOfTimePickerMonthField.getMoveYScalar();
        }
        else {
        	mScrollUpdateField = DAYOFMONTH_FIELD_TEXTURE;
        	m_scrollmovementVectorOfTimePickerDayOfMonthField.resetScrollMovementVector();
        	gaugeYAxisScrollGestureMovement(m_scrollmovementVectorOfTimePickerDayOfMonthField, event1.getY(), event2.getY());
        	scrollingScalar = m_scrollmovementVectorOfTimePickerDayOfMonthField.getMoveYScalar();
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
    			// mTimePickerTouchListener���� action==MotionEvent.ACTION_UP�϶� �ٽ� false�� �����ȴ�    			
    			mLaunchedScroll = true;    			  
    			scrollingScalar = getUpdateFieldOnScrolling(event1, event2);    			
    		}
    		else {
    			gestureScrollMovementVector vector;
    			
    			switch(mScrollUpdateField) {
    			case YEAR_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerYearField;
    				break;
    			case MONTH_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerMonthField;
    				break;
    			case DAYOFMONTH_FIELD_TEXTURE:
    				vector = m_scrollmovementVectorOfTimePickerDayOfMonthField;
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
    
    // handleMessage�� �浹 ���θ� ����ؾ� �Ѵ�
  	public void stopRenderThread() {
  		if (INFO) Log.i(TAG, "+stopRenderThread");
  		YearFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0);
  		MonthFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0); 
  		DayOfMonthFieldStatusMachine(TimePickerUserActionMessage.RUN_EXIT, 0);
  				
  		try {
			mOpenGLESRender.shouldStop();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  		  		
  		if (INFO) Log.i(TAG, "-stopRenderThread");
  	}	
  	
  	public void sleepRenderThread() {
		Log.i("tag", "+TimePickerRender : sleepRenderThread");
		YearFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0);
		MonthFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0); 
		DayOfMonthFieldStatusMachine(TimePickerUserActionMessage.RUN_SLEEP, 0);			
		
		Log.i("tag", "-TimePickerRender : sleepRenderThread");
	}
  	
  	public void wakeUpRenderThread(Calendar cal) {
		Log.i("tag", "+BriefTimePickerRender : wakeUpRenderThread");
		resetCalendar(cal);
		
		YearFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0);
		MonthFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0); 
		DayOfMonthFieldStatusMachine(TimePickerUserActionMessage.RUN_WAKEUP, 0);		
		
		resetField();
		
		mRenderingLock.lock();
		mConditionOfRenderingLock.signalAll();      			
		mRenderingLock.unlock();
		Thread.yield();
		
		Log.i("tag", "-BriefTimePickerRender : wakeUpRenderThread");
	}
  	
  	public Calendar getTimeBeforeSleep() {
		//Calendar curCal = (Calendar)mCalendar.clone();
  		Calendar curCal = GregorianCalendar.getInstance();
  		
		curCal.set(mRepeatEndTimePickerYearFieldValue, mRepeatEndTimePickerMonthFieldValue, mRepeatEndTimePickerDayOfMonthFieldValue);
		
		return curCal;
	}
  	
  	public OnTouchListener getOnTouchListener() {
    	return mTimePickerTouchListener;
    }
}
