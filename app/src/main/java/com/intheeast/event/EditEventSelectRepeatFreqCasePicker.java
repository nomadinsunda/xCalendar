package com.intheeast.event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL11;

import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;
import com.intheeast.etc.TextureInfo;
import com.intheeast.event.EditEventRepeatUserSettingSubView.EditEventSelectRepeatFreqCasePickerRenderParameters;
import com.intheeast.gl.EyePosition;




import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

public class EditEventSelectRepeatFreqCasePicker extends HandlerThread  implements Callback {

	Context mTextureViewContext;
	Activity mActivity;
    private SurfaceTexture mSurface;
    
    Bitmap mRepeatFreqCasePickerEveryDayTextBitmap;
	Bitmap mRepeatFreqCasePickerEveryWeekTextBitmap;	
	Bitmap mRepeatFreqCasePickerEveryMonthTextBitmap;
	Bitmap mRepeatFreqCasePickerEveryYearTextBitmap;
	
	Handler mTimeMsgHandler;		    
	
    int m_Width;
	int m_Height; 
	
	private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private EGLContext mEglContext;
    private int mProgram;
    private EGL10 mEgl;
    private GL11 mGl;
    
    EyePosition m_RunnginStickersVieweyePosition;
    
    float m_fovy;
	float m_tanValueOfHalfRadianOfFovy;
	
	static final float mNear = 0.1f;
	static final float mFar = 10.0f; 
	static final float m_upX = 0;
	static final float m_upY = 1.0f;
	static final float m_upZ = 0;
	static final float m_halfRatioOfViewingVPHeight = 0.5f;
	static final float OBJECT_TARGET_Z_POSITION = 0;
	public static final float NORMAL_PICKER_VIEW_ANGLE_OF_VIEW = 20.0f; 
	private static final double HIGHLIGHT_BACKWARD_OFFSET = 0.25;
	
	public static final double PICKER_FIELD_CIRCLE_RADIUS = 0.5; // viewport�� height�� ���� ratio��!!!
    public static final double PICKER_ITEM_AVAILABLE_MAX_DEGREE = 180;
    public static final double PICKER_ITEM_AVAILABLE_MIN_DEGREE = 0;
    public static final double FIELD_ITEM_DEGREE = 20;
    public static final int PICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT = (int)(PICKER_ITEM_AVAILABLE_MAX_DEGREE / FIELD_ITEM_DEGREE);
	
	float m_camera_eye_X;
	float m_camera_eye_Y;
	float m_camera_eye_Z;
	float m_camera_lookAt_X;
	float m_camera_lookAt_Y;
	float m_camera_lookAt_Z;
		
	float mRatio;
	float m_Ppi;
	
	double mCircle_center_z;
	double mCircle_HighLight_center_z;	
	
	private float[] mModelMatrix = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mMVPMatrix = new float[16];       	
	
	private int mMVPMatrixHandle;	
	private int mTextureUniformHandle;
	private int mPositionHandle;
	private int mTextureCoordinateHandle;
	
	int m_isChangedAlphaHandle;
	int m_ChangedAlphaValueHandle;

	EditEventSelectRepeatFreqCasePickerRenderParameters mPickerRenderParameters;
	
	LockableScrollView mMainPageScrollView;
	
	//float mPickerTextWdith;
	//float mPickerTextWidthRatio;
	
	float mPickerLeftMostVPRatio;
	
	double mCurrentFreqValueFieldUppermostItemBottomSideDegree;
	double mCurrentFreqValueFieldLowermostItemTopSideDegree;
		
	public EditEventSelectRepeatFreqCasePicker(String name, Context TextureViewContext, 
			Activity activity, 			
			Handler timeMsgHandler,			
			EditEventSelectRepeatFreqCasePickerRenderParameters parameters) {
		super(name);
		
		mTextureViewContext = TextureViewContext;
		mActivity = activity;        
        mTimeMsgHandler = timeMsgHandler;
        
        m_Width = parameters.mWidth;
        m_Height = parameters.mHeight;            
        mPickerRenderParameters = parameters;
        
        setRenderParameters();
        
        mRatio = (float) (m_Width / m_Height); 
        //mLeft = -(mRatio / 2);
        //mRight = mRatio / 2;
    	
		setCameraZPosition(NORMAL_PICKER_VIEW_ANGLE_OF_VIEW);
		setVeiwEyePosition();          
		setViewMatrix(m_RunnginStickersVieweyePosition);    		
		setPerspectiveProjectionMatrix(m_fovy, mRatio, mNear, mFar);
		
		mPickerLeftMostVPRatio = 0.0f;
		//mPickerTextWidthRatio = (mPickerTextWdith * mRatio) / m_Width;
		
		mCurrentFreqValueFieldUppermostItemBottomSideDegree = PICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
		mCurrentFreqValueFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
		
		int currentFreqValue = 0;
		switch (currentFreqValue) {
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_DAY:
			mCurrentFreqValueFieldUppermostItemBottomSideDegree = PICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
			mCurrentFreqValueFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
			break;
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_WEEK:
			mCurrentFreqValueFieldUppermostItemBottomSideDegree = PICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
			mCurrentFreqValueFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
			break;			
		
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_MONTH:
			mCurrentFreqValueFieldUppermostItemBottomSideDegree = PICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
			mCurrentFreqValueFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
			break;			
		case EditEventRepeatSettingSwitchPageView.EVENT_RECURRENCE_BY_EVERY_YEAR:
			mCurrentFreqValueFieldUppermostItemBottomSideDegree = PICKER_ITEM_AVAILABLE_MAX_DEGREE - FIELD_ITEM_DEGREE;
			mCurrentFreqValueFieldLowermostItemTopSideDegree = FIELD_ITEM_DEGREE;
			break;
		}
	}

	
	public void setRenderParameters() {
		//mPickerTextWdith = mPickerRenderParameters.getPickerTextWdith();
		
		m_Ppi = mPickerRenderParameters.getPpi();	
		
		int size = mPickerRenderParameters.mTextureInfoList.size();
		for (int i=0; i<size; i++) {
			TextureInfo obj = mPickerRenderParameters.mTextureInfoList.get(i);
			int freqCaseValue = (Integer) obj.mTag;
			switch(freqCaseValue) {
			case EditEventRepeatUserSettingSubView.DAILY:
				mRepeatFreqCasePickerEveryDayTextBitmap = obj.mBitmap;
				//mNormal.setBackground(ob);
				break;
			case EditEventRepeatUserSettingSubView.WEEKLY:
				mRepeatFreqCasePickerEveryWeekTextBitmap = obj.mBitmap;
				break;
			case EditEventRepeatUserSettingSubView.MONTHLY:
				mRepeatFreqCasePickerEveryMonthTextBitmap = obj.mBitmap;
				break;
			case EditEventRepeatUserSettingSubView.YEARLY:
				mRepeatFreqCasePickerEveryYearTextBitmap = obj.mBitmap;
				break;			
			}
		}
		
	}
	
	Looper mMainThreadLooper;
	int mMainThreadPriority;
	Handler mRenderThreadHandler;
	OpenGLESRender mOpenGLESRender;
    ReentrantLock mRenderingLock;        
    Condition mConditionOfRenderingLock;
    int mRenderingThreadPriority;
    
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
            //onDrawFrame();	            
            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                //Log.e(TAG, "cannot swap buffers!");
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
			            //onDrawFrame();            
			            
			            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
			                //Log.e(TAG, "cannot swap buffers!");
			            }
	    						    				
			            checkEglError();
    				}
    				else {
    					mContinue = false;   					
    					//deleteTextures();			
    					//deInitGL();    					
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
	
	private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        
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
                //Log.e(TAG, "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
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
        		
		mRepeatFreqCasePickerEveryDayTextureDataHandles[0] = loadTexture(mRepeatFreqCasePickerEveryDayTextBitmap);
		mRepeatFreqCasePickerEveryDayTextureDataHandles[1] = loadTexture(mRepeatFreqCasePickerEveryWeekTextBitmap);
		mRepeatFreqCasePickerEveryDayTextureDataHandles[2] = loadTexture(mRepeatFreqCasePickerEveryMonthTextBitmap);
		mRepeatFreqCasePickerEveryDayTextureDataHandles[3] = loadTexture(mRepeatFreqCasePickerEveryYearTextBitmap); 
		
		for (int i=0; i<4; i++) {
			setMipmapFilters(mRepeatFreqCasePickerEveryDayTextureDataHandles[i]);
		}
				
		makeXXX();
		
        // ���⼭ center eraseBackground �����͸� ��������
        //buildVerticesTexturePosDataOfCenterEraseBackground();
    }   
		
	public void makeXXX() {
		int columnPolygonNumbers = 0;
		float leftmost = 0;
		float x_dif = 0;
		float topmost = 0;
		float lowermost = 0;
		
		
		for (int i=0; i<4; i++) {
			
			// ���⼭ ������ ��� �;� �Ѵ�
			// ...
			
			
			FloatBuffer positions = buildPositionOfVertices(columnPolygonNumbers, leftmost, x_dif, topmost, lowermost);
			FloatBuffer textureCoordinatesBuffer = buildTextureCoordiantes(columnPolygonNumbers);
			
			VertexAttributesObject obj = new VertexAttributesObject(mRepeatFreqCasePickerEveryDayTextureDataHandles[i], 
					positions,
					textureCoordinatesBuffer,
					columnPolygonNumbers);
		}
		
		
	}
	
	ArrayList<VertexAttributesObject> mRepeatFreqCasePickerEveryDayVerticesList;
	
	int[] mRepeatFreqCasePickerEveryDayTextureDataHandles = new int[4]; 
	
	private FloatBuffer buildPositionOfVertices(int columnPolygonNumbers, float leftmost, float x_dif, float topmost, float lowermost) {		
		
		// ROW_POLYGON_NUMBERS�� 1�� ���� ���� �迭 ������� ���� ������?
		// ROW_POLYGON_NUMBERS�� 3���̸� y,z ��ǥ�� ���� 4�� �ʿ��ϱ� �����̴�
		double[] YAxis = new double[ROW_POLYGON_NUMBERS + 1];
		double[] ZAxis = new double[ROW_POLYGON_NUMBERS + 1];
		// �������� ���� ���� ���Ѵ�
		// :�Ʒ� ��Ÿ��� ���Ǹ� Ȱ���� �� ����ϱ� ���� �̸� ����� �д�
		double r2 = Math.pow(PICKER_FIELD_CIRCLE_RADIUS, 2);
		int end = ROW_POLYGON_NUMBERS + 1;
		
		double abs = (double)Math.abs(topmost - lowermost);
		double step = abs / ROW_POLYGON_NUMBERS;
		
		double y = 0;
		double y2 = 0;		
		
		for (int i=0; i<end; i++) {
			y = lowermost + (i * step);
			y2 = Math.pow(y, 2);      		
			// ��Ÿ��� ���Ǹ� Ȱ���ؼ� z ���� ���Ѵ�
			// r^2 = y^2 + z^2
			// z^2 = r^2 - y^2
			// z = sqrt(r^2 - y^2)
			ZAxis[i] = Math.sqrt(r2 - y2); 
			YAxis[i] = y;
		}
		
		int totalVerticesNumbers = ROW_POLYGON_NUMBERS * (columnPolygonNumbers * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * VECTOR_COMPONENTS_PER_POSITION);
		float[] Positions = new float[totalVerticesNumbers];
		
		int index = 0;	
		for (int j=0; j<columnPolygonNumbers; j++) {  // x�� ���� ȸ��
			float x = leftmost + (x_dif * j);
			
			for (int k=0; k<ROW_POLYGON_NUMBERS; k++) {  // y�� ���� ȸ��     
				// left triangle 
				// v[0]
				Positions[index++] = x; 
				Positions[index++] = (float)YAxis[k+1]; 
				Positions[index++] = (float)ZAxis[k+1];
				// v[1]
				Positions[index++] = x; 
				Positions[index++] = (float)YAxis[k]; 
				Positions[index++] = (float)ZAxis[k];
				// v[2]
				Positions[index++] = x + x_dif; 
				Positions[index++] = (float)YAxis[k+1]; 
				Positions[index++] = (float)ZAxis[k+1];
				
				// right triangle 
				// v[1]
				Positions[index++] = x; 
				Positions[index++] = (float)YAxis[k]; 
				Positions[index++] = (float)ZAxis[k];
				// v[3]
				Positions[index++] = x + x_dif; 
				Positions[index++] = (float)YAxis[k]; 
				Positions[index++] = (float)ZAxis[k];
				// v[2]
				Positions[index++] = x + x_dif; 
				Positions[index++] = (float)YAxis[k+1]; 
				Positions[index++] = (float)ZAxis[k+1];  
				
			}
		}	    
		
		
		FloatBuffer fbPositions = ByteBuffer.allocateDirect(totalVerticesNumbers * mBytesPerFloat)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();	
		fbPositions.put(Positions).position(0);
		/*
		for (int j=0; j<ROW_MESH; j++) {  // x�� ���� ȸ��
			float x = leftmost + (x_dif * j);
			
			for (int k=0; k<columnPolygonNumbers; k++) {  // y�� ���� ȸ��     
				// left triangle 
				// v[0]
				Positions[index++] = x; 
				Positions[index++] = (float)YAxis[k+1]; 
				Positions[index++] = (float)ZAxis[k+1];
				// v[1]
				Positions[index++] = x; 
				Positions[index++] = (float)YAxis[k]; 
				Positions[index++] = (float)ZAxis[k];
				// v[2]
				Positions[index++] = x + x_dif; 
				Positions[index++] = (float)YAxis[k+1]; 
				Positions[index++] = (float)ZAxis[k+1];
				
				// right triangle 
				// v[1]
				Positions[index++] = x; 
				Positions[index++] = (float)YAxis[k]; 
				Positions[index++] = (float)ZAxis[k];
				// v[3]
				Positions[index++] = x + x_dif; 
				Positions[index++] = (float)YAxis[k]; 
				Positions[index++] = (float)ZAxis[k];
				// v[2]
				Positions[index++] = x + x_dif; 
				Positions[index++] = (float)YAxis[k+1]; 
				Positions[index++] = (float)ZAxis[k+1];  
				
			}
		}	    		
		*/
		return fbPositions;
	}
	
		
	//FloatBuffer mFbFieldTextureCoordinates;
	private FloatBuffer buildTextureCoordiantes(int columnPolygonNumbers) {
		float s_coordinate = 0;
		float t_coordinate = 0;
		float texture_polygon_width = 1.0f / (float)columnPolygonNumbers;
		float texture_polygon_height = 1.0f / (float)ROW_POLYGON_NUMBERS;
		int totalTexCoordinateDataNumbers = ROW_POLYGON_NUMBERS * (VECTOR_COMPONENTS_PER_TEXCOORDINATE * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * columnPolygonNumbers);
		FloatBuffer fbFieldTextureCoordinates = ByteBuffer.allocateDirect(totalTexCoordinateDataNumbers * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
				
		for (int i=0; i<columnPolygonNumbers; i++) {  // x�� ���� ȸ��
			for (int j=0; j<ROW_POLYGON_NUMBERS; j++) {  // y�� ���� ȸ��          				
				// v0 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width);
				t_coordinate = 1.0f - (j * texture_polygon_height) - texture_polygon_height;
				fbFieldTextureCoordinates.put(s_coordinate);fbFieldTextureCoordinates.put(t_coordinate); 
				// v1 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width);
				t_coordinate = 1.0f - (j * texture_polygon_height);
				fbFieldTextureCoordinates.put(s_coordinate);fbFieldTextureCoordinates.put(t_coordinate); 
				// v2 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width) + texture_polygon_width;
				t_coordinate = 1.0f - (j * texture_polygon_height) - texture_polygon_height;
				fbFieldTextureCoordinates.put(s_coordinate);fbFieldTextureCoordinates.put(t_coordinate); 
				
				// v1 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width);
				t_coordinate = 1.0f - (j * texture_polygon_height);
				fbFieldTextureCoordinates.put(s_coordinate);fbFieldTextureCoordinates.put(t_coordinate); 
				// v3 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width) + texture_polygon_width;
				t_coordinate = 1.0f - (j * texture_polygon_height);
				fbFieldTextureCoordinates.put(s_coordinate);fbFieldTextureCoordinates.put(t_coordinate); 
				// v2 t_c
				s_coordinate = 0.0f + (i * texture_polygon_width) + texture_polygon_width;
				t_coordinate = 1.0f - (j * texture_polygon_height) - texture_polygon_height;
				fbFieldTextureCoordinates.put(s_coordinate);fbFieldTextureCoordinates.put(t_coordinate);       				
			}
		}
		
		return fbFieldTextureCoordinates;
	}
	
	private void checkEglError() {
        final int error = mEgl.eglGetError();
        if (error != EGL10.EGL_SUCCESS) {
            //Log.e(TAG, "EGL error = 0x" + Integer.toHexString(error));
        }
    }

    private void checkGlError() {
        final int error = mGl.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            //Log.e(TAG, "GL error = 0x" + Integer.toHexString(error));
        }
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
    		yAxisDateField[i] = (float)( Math.cos( Math.toRadians(degree) ) * PICKER_FIELD_CIRCLE_RADIUS );    			
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
				//Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
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
				//Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
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
    
	private void setCameraZPosition(float angleOfView) {
		m_fovy = angleOfView;
		double halfDegreeOfFovy = (double)(m_fovy / 2);
		double tanDVaule = Math.tan(Math.toRadians(halfDegreeOfFovy));
		m_tanValueOfHalfRadianOfFovy = (float)tanDVaule;
		m_camera_eye_Z = ( (1 / m_tanValueOfHalfRadianOfFovy) * m_halfRatioOfViewingVPHeight ) - OBJECT_TARGET_Z_POSITION; 
		
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
	
	public void setSurface(SurfaceTexture surface) {
		
	}
	
	public void reStartRenderThread() {
		
	}
	
	public void stopRenderThread() {
		
	}
	
	public void sleepRenderThread() {
		
	}
	
	public void wakeUpRenderThread(Calendar time) {
		
	}

	@Override
	public boolean handleMessage(Message msg) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	public class VertexAttributesObject {
		int mTextureHandle;      			
		FloatBuffer mFbPositions;
		FloatBuffer mFbTextureCoordinates;
		int mPointNumbers; 
		boolean mIsChangeAlpha;
		float mAlphaValue;
		
		public VertexAttributesObject (int textureHandle, 
									FloatBuffer fbPoints,
									FloatBuffer fbTextureCoordinates,
									int columnPolygonNumbers) {
			mTextureHandle = textureHandle;	
							
			//int totalVerticesNumbers = ROW_POLYGON_NUMBERS * (VECTOR_COMPONENTS_PER_POSITION * VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS * columnPolygonNumbers);			
			mFbPositions = fbPoints;				
			mFbTextureCoordinates = fbTextureCoordinates;
			//float x_dif = widthRatio / columnPolygonNumbers;		
			// �츮�� ����ϴ� �������� Ʈ���̾ޱ� �� ���� �����ȴ� : triangle mesh�� ����ϰ� ����
			// �ϳ��� triangle�� 3���� position�� �ʿ��ϴ�
			// : 2 * 3 = 6
			mPointNumbers = (ROW_POLYGON_NUMBERS * columnPolygonNumbers) * 6;//buildXPositionOfVertices(fbPoints, leftmost, x_dif);
			
			mIsChangeAlpha = false;
    		mAlphaValue = 1;
		}   
		
		/*
		public VertexAttributesObject (int textureHandle, float fbPoints[],
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
		*/
		
	}
	
	
	private static final int VECTOR_COMPONENTS_PER_POSITION = 3;
    private static final int VECTOR_COMPONENTS_PER_TEXCOORDINATE = 2;        
    private static final int ROW_POLYGON_NUMBERS = 3;
	//private static final int COLUMN_MESH = 3;
	
	private static final int mBytesPerFloat = 4;	
	private static final int mDataSizePerVertex = 3;	
	private static final int mTextureCoordinateDataSize = 2;  	
	private static final int VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS = 6; 
	private static final int VERTICES_PER_POLYGON_FOR_DRAW_ELEMENTS = 4;
	
	/*
	private int buildXPositionOfVertices(float fbPoints[], float leftmost, float x_dif) {
		
		int pointNumbers = 0;
		float x = 0;        		
		int index = 0;
		for (int i=0; i<ROW_MESH; i++) {  // x�� ���� ȸ��
			// mesh�� �����ϴ� �ϳ��� ������(�ﰢ������ �� ���� ������ �簢������)��...6���� vertices�� �ʿ��ϴ�	
			x = leftmost + (x_dif * i);    			
			
			v[0]:(x,y[i+1])  v[2]:(x+x_dif,y[i+1])
			 _______________________ 
			|                       |
			|                       |
			|                       |
			|                       |
			|_______________________|
			v[1](x,y[i])     v[3]:(x+x_dif,y[i])    	        
			
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
	*/
}
