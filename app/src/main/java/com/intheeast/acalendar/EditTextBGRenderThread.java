package com.intheeast.acalendar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL11;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class EditTextBGRenderThread extends Thread {
	private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final String TAG = "EditTextBGRenderThread";   
    private static final boolean INFO = true;
    
    Activity mActivity;
    Context mTextureViewContext;
    
    private SurfaceTexture mSurface;
    int m_ViewWidth;
	int m_ViewHeight;    	
	
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private EGLContext mEglContext;
    
    private EGL10 mEgl;
    private GL11 mGl;
    
    int[] mProgramHandle;
    
	private int mTextureUniformHandle;
	private int mPositionHandle;
	private int mTextureCoordinateHandle;
	private int mTargetBluringTextureCoordinateHandle;
	
	int m_textureWidthDataHandle;
	int m_textureHeightDataHandle;
	
	private static final int mBytesPerFloat = 4;	
	private static final int mDataSizePerVertex = 3;	
	private static final int mTextureCoordinateDataSize = 2;  	
	private static final int VERTICES_PER_POLYGON_FOR_DRAW_ARRAYS = 6; 
	private static final int VERTICES_PER_POLYGON_FOR_DRAW_ELEMENTS = 4;
	static final int POSITION_DATA_SIZE = 3;	
	
	
	private final FloatBuffer mFbPositions;
	private final FloatBuffer mFbPositionsForBluring;
	private final FloatBuffer mFbTextureCoordinates;	
	
	int[] m_uiVbo;
	static final int BYTES_PER_FLOAT = 4;
	static final int BYTES_PER_INT = 4;
	
	int[] m_fboForVerticalProc;
	int[] m_fbtexForVerticalProc;
	int[] m_fboForHorizontalProc;
	int[] m_fbtexForHorizontalProc;
	IntBuffer texBufferForVerticalProc;
	IntBuffer texBufferForHorizontalProc;
	ArrayList<IntBuffer> m_depthRenderbufferList;
	
	int texW;
	int texH;
	int[] m_fboMainView;
	int[] m_fbtexMainView;	
	IntBuffer m_fbdepthMainView;
	IntBuffer m_fbtexBufferMainView;
	
	int[] m_fboRightMainView;
	int[] m_fbtexRightMainView;	
	IntBuffer m_fbdepthRightSideMainView;
	IntBuffer m_fbtexRightSideBufferMainView;
	
	final float[] m_fbPositionData =
	{
		// positions                //tex coordinate
		-1.0f, 1.0f, 0.0f,	//0		//0.0f, 0.0f, 	
		-1.0f, -1.0f, 0.0f, //1     //0.0f, 1.0f,
		1.0f, 1.0f, 0.0f,   //2     //1.0f, 0.0f,
		-1.0f, -1.0f, 0.0f, //1		//0.0f, 1.0f,		
		1.0f, -1.0f, 0.0f,  //3     //1.0f, 1.0f,
		1.0f, 1.0f, 0.0f,   //2     //1.0f, 0.0f,					
	};	
	
	final float[] m_fbPositionDataForBluring = 
	{
		// positions                //tex coordinate
		-1.0f, 1.0f, 0.0f,	//0		//0.0f, 0.0f, 	
		-1.0f, -1.0f, 0.0f, //1     //0.0f, 1.0f,
		1.0f, 1.0f, 0.0f,   //2     //1.0f, 0.0f,
		-1.0f, -1.0f, 0.0f, //1		//0.0f, 1.0f,		
		1.0f, -1.0f, 0.0f,  //3     //1.0f, 1.0f,
		1.0f, 1.0f, 0.0f,   //2     //1.0f, 0.0f,					
	};	
	
	
	final float[] fbTextureCoordinateData =
	{						   
			0.0f, 0.0f, 	//0			
			0.0f, 1.0f,     //1
			1.0f, 0.0f,     //2
			0.0f, 1.0f,     //1
			1.0f, 1.0f,     //3
			1.0f, 0.0f		//2	
	};
	
	
	Bitmap mBG;
	int mBGTextureHandle;
	public static final float NORMAL_MAIN_VIEW_ANGLE_OF_VIEW = 30.0f;
	public EditTextBGRenderThread(Context TextureViewContext, Activity activity, Bitmap background, SurfaceTexture surface, int width, int height) {
		mTextureViewContext = TextureViewContext;
		mActivity = activity;
		mBG = background;
        mSurface = surface;
        
        m_ViewWidth = width;
        m_ViewHeight = height;		
		
		mFbPositions = ByteBuffer.allocateDirect(m_fbPositionData.length * mBytesPerFloat)
		.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mFbPositions.put(m_fbPositionData).position(0);				
		
		mFbPositionsForBluring = ByteBuffer.allocateDirect(m_fbPositionDataForBluring.length * mBytesPerFloat)
		.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mFbPositionsForBluring.put(m_fbPositionDataForBluring).position(0);
		
		mFbTextureCoordinates = ByteBuffer.allocateDirect(fbTextureCoordinateData.length * mBytesPerFloat)
		.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mFbTextureCoordinates.put(fbTextureCoordinateData).position(0);			
				
        config();
	}	
	
	public void config() {		
		int bufSize = 0;		
				
		m_uiVbo = new int[1];	
		
	    m_fboForVerticalProc = new int[1];
	    m_fbtexForVerticalProc = new int[1];	    	
	    m_fboForHorizontalProc = new int[1]; 	
	    m_fbtexForHorizontalProc = new int[1];	    
		
		m_depthRenderbufferList = new ArrayList<IntBuffer>();		
		
		for (int i=0; i<2; i++) {
		    IntBuffer depthRenderbuffer = IntBuffer.allocate(1);
			m_depthRenderbufferList.add(i, depthRenderbuffer);		
	    }	
		
		texW = m_ViewWidth;
		texH = m_ViewHeight;	
		bufSize = texW * texH * 2;
		texBufferForVerticalProc = ByteBuffer.allocateDirect(bufSize).order(ByteOrder.nativeOrder()).asIntBuffer();		
		texBufferForHorizontalProc = ByteBuffer.allocateDirect(bufSize).order(ByteOrder.nativeOrder()).asIntBuffer();	
				
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		m_fbtexRightMainView = new int[1];
		m_fboRightMainView = new int[1];
		m_fbdepthRightSideMainView = IntBuffer.allocate(1);
		bufSize = texW * texH * 2;
		m_fbtexRightSideBufferMainView = ByteBuffer.allocateDirect(bufSize).order(ByteOrder.nativeOrder()).asIntBuffer();
				
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		m_fbtexMainView = new int[1];
		m_fboMainView = new int[1];
		m_fbdepthMainView = IntBuffer.allocate(1);
		bufSize = texW * texH * 2;
		m_fbtexBufferMainView = ByteBuffer.allocateDirect(bufSize).order(ByteOrder.nativeOrder()).asIntBuffer();			
	}
	
	@Override
	public void run() {
		super.run();	
		
		initGL();    
		
        checkCurrent();	
        
        createFrameBuffers();
        
        onDrawFrame();	
        
        if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            Log.i(TAG, "cannot swap buffers!");
        }
        checkEglError();	   
        
        if (INFO) Log.i(TAG, "EditTextBGRenderThread blurring work comletion");
	}
	
	static final float mBG_RED_COMPONENT = 0;
	static final float mBG_GREEN_COMPONENT = 0;
	static final float mBG_BLUE_COMPONENT = 0;
	static final float mBG_ALPHA_COMPONENT = 1.0f;
	public void onDrawFrame() {
		// 먼저 메인 뷰[스티커들이 나열되어 있는 뷰]를 시스템이 디폴트로 제공하는 프레임버퍼에 drawing해야 한다
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, m_ViewWidth, m_ViewHeight);			
		GLES20.glClearColor(mBG_RED_COMPONENT, mBG_GREEN_COMPONENT, mBG_BLUE_COMPONENT, mBG_ALPHA_COMPONENT);
 		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); 		
        
		changeFrameBuffer();			
		
        drawArraysTextureOriginalSource();	
		
		bluringProcess();
				
		drawBluringEffect();
		
		getInterBlurredPixels();
	}
	
	public void changeFrameBuffer() {		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, m_fboRightMainView[0]);
		
		//GLES20.glClearColor(mBG_RED_COMPONENT, mBG_GREEN_COMPONENT, mBG_BLUE_COMPONENT, mBG_ALPHA_COMPONENT);
 		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	}
	
	private void bindTexture() {    
		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0); 
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBGTextureHandle);        
	}
	
	static final int DRAW_ARRAYS_COUNT_NUMBERS = 6;
	private void drawArraysTextureOriginalSource() {	
        
		GLES20.glUseProgram(mProgramHandle[2]);
		
		bindTexture();        
		              
		mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle[2], "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle[2], "a_Position"); 
	    mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle[2], "a_TexCoordinate");	
                
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0); 
        checkGlError();
        
		// Pass in the position information
		mFbPositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle,
        							 mDataSizePerVertex,
        							 GLES20.GL_FLOAT,
        							 false,
        							 0,
        							 mFbPositions);        
        checkGlError();        
        GLES20.glEnableVertexAttribArray(mPositionHandle);              
        checkGlError();        
        // Pass in the texture coordinate information
        mFbTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
        						     mTextureCoordinateDataSize,
        						     GLES20.GL_FLOAT,
        						     false,
        						     0,
        						     mFbTextureCoordinates);
        checkGlError();
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        checkGlError();	      
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DRAW_ARRAYS_COUNT_NUMBERS);  
        checkGlError();
        
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
	}
	
	static final int TAP_COUNT = 9;
	public void bluringProcess() {
		float subTexW = 0;
		float subTexH = 0;
		int fboVertiaclBluring = 0;
		int fboHorizontalBluring = 0;
		int fbTexVerticalBluring = 0;
		int fbTexHorizontalBluring = 0;
		int fbTexRightSide = 0;
		int fboMainView = 0;
		boolean IsFirst = true;		
		
		subTexW = texW;
		subTexH = texH;
		fboVertiaclBluring = m_fboForVerticalProc[0];
		fboHorizontalBluring = m_fboForHorizontalProc[0];
		fbTexVerticalBluring = m_fbtexForVerticalProc[0];
		fbTexHorizontalBluring = m_fbtexForHorizontalProc[0];
		fbTexRightSide = m_fbtexRightMainView[0];
		fboMainView = m_fboMainView[0];			
				
		for (int i=0; i<TAP_COUNT; i++) { 
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboVertiaclBluring);			
	 		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);		   
	     		
			GLES20.glUseProgram(mProgramHandle[1]);
			
	        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle[1], "u_Texture");
	        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle[1], "a_Position");
	        m_textureWidthDataHandle = GLES20.glGetUniformLocation(mProgramHandle[1], "v_Width");
	        m_textureHeightDataHandle = GLES20.glGetUniformLocation(mProgramHandle[1], "v_Height");
	        GLES20.glUniform1f(m_textureWidthDataHandle, subTexW);       
	        GLES20.glUniform1f(m_textureHeightDataHandle, subTexH);
	        
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        if (IsFirst) {
	        	IsFirst = false;
	        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbTexRightSide); // m_mainViewScreenShotTextureHandle	
	        }
	        else {
	        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbTexHorizontalBluring); 
	        }
	        
	        GLES20.glUniform1i(mTextureUniformHandle, 0);
	        
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m_uiVbo[0]);
			GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);			
	        GLES20.glEnableVertexAttribArray(mPositionHandle);
	        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
			GLES20.glDisableVertexAttribArray(mPositionHandle);				
			//////////////////////////////////////////////////////////////////////////
			//////////////////////////////////////////////////////////////////////////			
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHorizontalBluring);			
	 		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
				
			GLES20.glUseProgram(mProgramHandle[0]);			
			
			mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle[0], "u_Texture");
	        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle[0], "a_Position");  
	        m_textureWidthDataHandle = GLES20.glGetUniformLocation(mProgramHandle[0], "v_Width");
	        m_textureHeightDataHandle = GLES20.glGetUniformLocation(mProgramHandle[0], "v_Height");
	        GLES20.glUniform1f(m_textureWidthDataHandle, subTexW);	        
	        GLES20.glUniform1f(m_textureHeightDataHandle, subTexH);
	        
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbTexVerticalBluring);  // m_fbtexForHorizontalProc에 fragment shader에서 연산된 color fragment가 저장된다
			GLES20.glUniform1i(mTextureUniformHandle, 0);
			
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m_uiVbo[0]);
			GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);
	        GLES20.glEnableVertexAttribArray(mPositionHandle);
	        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
			GLES20.glDisableVertexAttribArray(mPositionHandle);			
		}		
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);  // vertex buffer를 disable 함???
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMainView);
        
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);	
		GLES20.glUseProgram(mProgramHandle[2]);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbTexHorizontalBluring);
		GLES20.glUniform1i(mTextureUniformHandle, 0);
		
		mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle[2], "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle[2], "a_Position"); 
	    mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle[2], "a_TexCoordinate");		
        
        mFbPositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle,
        							 POSITION_DATA_SIZE,
        							 GLES20.GL_FLOAT,
        							 false,
        							 0,
        							 mFbPositions);                
        GLES20.glEnableVertexAttribArray(mPositionHandle);   
        
        mFbTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
        						     mTextureCoordinateDataSize,
        						     GLES20.GL_FLOAT,
        						     false,
        						     0,
        						     mFbTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);		
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}
	
	public void drawBluringEffect() {	
		int fbTexMainView = 0;
		GLES20.glUseProgram(mProgramHandle[2]);		
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		///////////////////////////////////////////////////////////////
		fbTexMainView = m_fbtexMainView[0];		
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbTexMainView);
		////////////////////////////////////////////////////////////////
		GLES20.glUniform1i(mTextureUniformHandle, 0);
		
		mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle[2], "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle[2], "a_Position"); 
	    mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle[2], "a_TexCoordinate");        
	    
	    mFbPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle,
        							 POSITION_DATA_SIZE,
        							 GLES20.GL_FLOAT,
        							 false,
        							 0,
        							 mFbPositions);                
        GLES20.glEnableVertexAttribArray(mPositionHandle);   
        
        mFbTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
        						     mTextureCoordinateDataSize,
        						     GLES20.GL_FLOAT,
        						     false,
        						     0,
        						     mFbTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);		
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

        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed "
                    + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        checkEglError();

        mGl = (GL11) mEglContext.getGL();
        checkEglError();

        buildProgram();
        
        mBGTextureHandle = loadTexture(mBG);	
    }
	
	public void deInitGL() {
    	Log.i("tag", "+TimePickerRender : deInitGL");
    	mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
    	mEgl.eglDestroyContext(mEglDisplay, mEglContext);
    	mEgl.eglTerminate(mEglDisplay);    	
    }	
	
	private void buildProgram() {
		String vertexShader = "none name";
		String fragmentShader = "none name";
		int vertexShaderHandle = 0;
		int fragmentShaderHandle = 0;
		
		mProgramHandle = new int[3];        
        
        for (int i=0; i<4; i++) {			
			switch(i) {
			case 0:
				vertexShader = getFragmentShader(R.raw.calendar_actionbar_per_bluring_vertex_shader);
				fragmentShader = getFragmentShader(R.raw.calendar_actionbar_per_bluring_vertical_fragment_shader);	
				vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
				fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
				
				mProgramHandle[i] = createAndLinkProgram(vertexShaderHandle,
														 fragmentShaderHandle,
														 new String[] {"a_Position"});	   
				break;
			case 1:
				vertexShader = getFragmentShader(R.raw.calendar_actionbar_per_bluring_vertex_shader);
				fragmentShader = getFragmentShader(R.raw.calendar_actionbar_per_bluring_horizontal_fragment_shader);
				vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
				fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
				
				mProgramHandle[i] = createAndLinkProgram(vertexShaderHandle,
														 fragmentShaderHandle,
														 new String[] {"a_Position"});	   
				break;
			case 2:
				vertexShader = getFragmentShader(R.raw.calendar_actionbar_per_vertex_shader);
				fragmentShader = getFragmentShader(R.raw.calendar_actionbar_per_fragment_shader);
				vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
				fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
				
				mProgramHandle[i] = createAndLinkProgram(vertexShaderHandle,
														 fragmentShaderHandle,
														 new String[] {"a_Position", "a_TexCoordinate"});  
				break;				
			
			default:
				break;
			}			
		}        
    }
	
	public void createFrameBuffers() {
		createMainViewFrameBuffer();
		createMainViewBluringProcessFrameBuffer();
		createRightSideFrameBuffers();
	}
	
	public void createMainViewFrameBuffer() {
		createGLES20FrameBuffer(0, m_fboMainView, m_fbtexMainView, m_fbdepthMainView, m_fbtexBufferMainView, texW, texH);		
	}
	
	public void createMainViewBluringProcessFrameBuffer() {
		
        GLES20.glGenBuffers(1, m_uiVbo, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m_uiVbo[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mFbPositions.capacity() * BYTES_PER_FLOAT, mFbPositions, GLES20.GL_STATIC_DRAW); 
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);          
        
        createSeperateBluringLevelCreationFrameBuffer(m_fboForVerticalProc, m_fbtexForVerticalProc, m_depthRenderbufferList.get(0), texBufferForVerticalProc,
        		                                m_fboForHorizontalProc, m_fbtexForHorizontalProc, m_depthRenderbufferList.get(1), texBufferForHorizontalProc,
        		                                texW, texH);        
	}
	
	public void createSeperateBluringLevelCreationFrameBuffer(int[] verticalfbo, int[] verticalfbtex, IntBuffer verticaldepthRenderbuffer, IntBuffer verticaltexBuffer,
            int[] Horizontalfbo, int[] Horizontalfbtex, IntBuffer HorizontaldepthRenderbuffer, IntBuffer HorizontaltexBuffer,
            int texWidth, int texHeight) {

		createGLES20FrameBuffer(0, verticalfbo, verticalfbtex, verticaldepthRenderbuffer, verticaltexBuffer, texWidth, texHeight);

		createGLES20FrameBuffer(0, Horizontalfbo, Horizontalfbtex, HorizontaldepthRenderbuffer, HorizontaltexBuffer, texWidth, texHeight);    	

	}
	
	public void createRightSideFrameBuffers() {
		createGLES20FrameBuffer(0, m_fboRightMainView, m_fbtexRightMainView, m_fbdepthRightSideMainView, m_fbtexRightSideBufferMainView, texW, texH);	
	}
	
	//the format of the pixel data
	public static final int SEARCH_EDITTEXT_BG_BLURRING_PIXEL_FORMAT = GLES20.GL_UNSIGNED_SHORT_5_6_5;
	
	public void createGLES20FrameBuffer(int index, int[] fbo, int[] fbtex, IntBuffer depthRenderbuffer, IntBuffer texBuffer, int texWidth, int texHeight) {
		IntBuffer maxRenderbufferSize = IntBuffer.allocate(1);	
		
		GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, maxRenderbufferSize);		
		if((maxRenderbufferSize.get(0) <= texWidth) || (maxRenderbufferSize.get(0) <= texHeight))
		{
			// cannot use framebuffer objects as we need to create a depth buffer as a renderbuffer object return with appropriate error
			Log.i("tag", "createFrameBuffer:maxRenderbufferSize small...!!!");
			return;
		}
		
		// Create a framebuffer object with a renderbuffer-based color attachment and a renderbuffer-based depth attachment
		
		// 1. create depth-render buffer
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		GLES20.glGenRenderbuffers(1, depthRenderbuffer);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRenderbuffer.get(0));
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texWidth, texHeight);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0 );
		
		// 2. color-render buffer
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		GLES20.glGenTextures(1, fbtex, index);		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbtex[index]);		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);		
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 
				            0,
				            GLES20.GL_RGB, 
				            texWidth, 
				            texHeight, 
				            0, 
				            GLES20.GL_RGB, 
				            GLES20.GL_UNSIGNED_SHORT_5_6_5, 
				            texBuffer);		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		//////////////////////////////////////////////////////////////////////////////////////////////////////////	
		// 아래 설정에 문제가 있다 당장 glGenTextures 사용 부분부터 문제가 있음
		/*GLES20.glGenTextures(1, depthRenderbuffer);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthRenderbuffer.get(0));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT, texWidth, texHeight, 0, GLES20.GL_DEPTH_COMPONENT16, GLES20.GL_UNSIGNED_SHORT, depthRenderbuffer);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);*/
		////////////////////////////////////////////////////////////////////////////////////////////
		
		GLES20.glGenFramebuffers(1, fbo, index);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
		
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fbtex[0], 0); 		 
		//GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, depthRenderbuffer.get(0), 0); 		 
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRenderbuffer.get(0)); 		 		
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			Log.i("tag", "createBothSideFrameBuffer : Right side FrameBuffer Binding faliure for MainView");
			return;
		}
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}
	
	ByteBuffer m_BlurredPixelBuffer;
	int mBytesPerPixel;
	public void getInterBlurredPixels() {
		//GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		int fboMainView = m_fboMainView[0];	
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMainView);
		int width = texW;
	    int height = texH;	    
	    IntBuffer readTypeBuffer = IntBuffer.allocate(1);
	    IntBuffer readFormatBuffer = IntBuffer.allocate(1);
	    GLES20.glGetIntegerv(GLES20.GL_IMPLEMENTATION_COLOR_READ_TYPE, readTypeBuffer);
	    GLES20.glGetIntegerv(GLES20.GL_IMPLEMENTATION_COLOR_READ_FORMAT, readFormatBuffer);
	    
	    int readType = readTypeBuffer.get(0);
	    int readFormat = readFormatBuffer.get(0);
	    
	    mBytesPerPixel = 0;
	    
	    switch(readType) {
	    case GLES20.GL_UNSIGNED_BYTE:
	    	switch(readFormat) {
	    	case GLES20.GL_RGBA:
	    		mBytesPerPixel = 4;
	    		break;
	    	case GLES20.GL_RGB:
	    		mBytesPerPixel = 3;
    			break;
	    	case GLES20.GL_LUMINANCE_ALPHA:
	    		mBytesPerPixel = 2;
	    		break;
	    	case GLES20.GL_ALPHA:    		
	    	case GLES20.GL_LUMINANCE:
	    		mBytesPerPixel = 1;
	    		break;
	    	}
	    	break;
	    case GLES20.GL_UNSIGNED_SHORT_4_4_4_4:	    	
	    case GLES20.GL_UNSIGNED_SHORT_5_5_5_1:	    	
	    case GLES20.GL_UNSIGNED_SHORT_5_6_5:
	    	mBytesPerPixel = 2;
	    	break;
	    }
	    
	    int bufSize = texW * texH * mBytesPerPixel;
	    m_BlurredPixelBuffer = ByteBuffer.allocateDirect(bufSize).order(ByteOrder.nativeOrder());	
	    GLES20.glReadPixels(0, 0, width, height, readFormat, readType, m_BlurredPixelBuffer);
	    m_BlurredPixelBuffer.rewind(); // 굳이 할 필요는 없다 position이 이미 0으로 설정되어 있음
	    
	    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}
	
	public int getBufferWidth() {
		return texW;
	}
	
	public int getBufferHeight() {
		return texH;
	}
	
	public int getBytesPerPixel() {
		return mBytesPerPixel;
	}	
	
	public Config getBitmapConfig() {
		// 아래 코드는 완벽하지 않다
		// :opengl과 android bitmap config가 서로 상이하기 때문에
		switch(mBytesPerPixel) {
		case 4:
			return Config.ARGB_8888;		
		case 2:
			return Config.RGB_565;			
		case 1:
			return Config.ALPHA_8;	
		default:
			return Config.RGB_565;
		}
	}
	
	public ByteBuffer getBlurredPixels() {		
	    return m_BlurredPixelBuffer;
	}	
	
	protected String getFragmentShader(int resID)
	{
		return readTextFileFromRawResource(mTextureViewContext, resID);
	}
	
	public String readTextFileFromRawResource(final Context context,
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
	
	public int compileShader(final int shaderType, final String shaderSource) 
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
				Log.e("tag", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
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
	
	public int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) 
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
				Log.i("tag", "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
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
}
