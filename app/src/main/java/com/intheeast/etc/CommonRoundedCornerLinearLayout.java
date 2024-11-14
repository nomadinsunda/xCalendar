package com.intheeast.etc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;

public class CommonRoundedCornerLinearLayout extends LinearLayout {
	private final static float CORNER_RADIUS = 7.5f;

    private Bitmap maskBitmap;
    private Paint paint, maskPaint;
    private float cornerRadius;
    
    int mWidth;
	int mHeight;
	
	boolean mSetWillDraw = false;

    public CommonRoundedCornerLinearLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CommonRoundedCornerLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CommonRoundedCornerLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
    	
		
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CORNER_RADIUS, metrics);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        //setWillNotDraw(false);
    }
    
    Runnable mResizeCallBack;
    public void setResizeView(/*int width, int height, */Runnable callBack) {
    	//mWidth = width;
    	//mHeight = height;
    	mResizeCallBack = callBack;
    	
    	ViewTreeObserver VTO = getViewTreeObserver(); 
		VTO.addOnGlobalLayoutListener(mOnGlobalLayoutListener);	
    }
    
    OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			CommonRoundedCornerLinearLayout.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);		
			mResizeCallBack.run();    	    			
		}		
	};	
    
    
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	
    	mWidth = w;
		mHeight = h;	
		
		if (!mSetWillDraw) {
			mSetWillDraw = true;
			setWillNotDraw (false);
		}		
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
    
    
    
    @Override
    public void draw(Canvas canvas) {
        Bitmap offscreenBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas offscreenCanvas = new Canvas(offscreenBitmap);

        super.draw(offscreenCanvas);

        if (maskBitmap == null) {
            maskBitmap = createMask(canvas.getWidth(), canvas.getHeight());
        }

        offscreenCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint);
        canvas.drawBitmap(offscreenBitmap, 0f, 0f, paint);
    }
    

    private Bitmap createMask(int width, int height) {
    	// *Bitmap.Config.ALPHA_8
    	//  :Each pixel is stored as a single translucency (alpha) channel. 
    	//   This is very useful to efficiently store masks for instance. 
    	//   No color information is stored. With this configuration, each pixel requires 1 byte of memory
        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(mask);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        //paint.setColor(Color.RED);

        canvas.drawRect(0, 0, width, height, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, paint);

        return mask;
    }
}
