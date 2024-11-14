package com.intheeast.impl;


import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnLayoutChangeListener;
import android.widget.FrameLayout;

public class CalendarCustomFrameLayout extends FrameLayout {
	private int mViewWidth = 0;
	private float xFraction = 0;
	private ViewTreeObserver.OnPreDrawListener preDrawListener = null;
	
    public CalendarCustomFrameLayout(Context context) {
        super(context);
    }

    public CalendarCustomFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);        
    }

    public CalendarCustomFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setViewWidth(int viewWidth) {
    	if (getWidth() == 0)
    		mViewWidth = viewWidth;
    	else
    		mViewWidth = getWidth();
    }

    public void setXFraction(float fraction) {

        this.xFraction = fraction;

        // setXFraction이 setViewWidth보다 먼저 호출될 가능성이 있는가???
        if (mViewWidth == 0) {
	        if (getWidth() != 0) {
	        	mViewWidth = getWidth();	            
	        }
	        else {
	        	if (preDrawListener == null) {
	        		
	                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
	                    @Override
	                    public boolean onPreDraw() {
	                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
	                        setXFraction(xFraction);
	                        return true;
	                    }
	                };
	                
	                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
	            }
	        }
        }
        
        //float translationX = getWidth() * fraction;
        float translationX = mViewWidth * fraction;
        setTranslationX(translationX);
    }

    public float getXFraction() {
        return this.xFraction;
    }
	/*
	int mViewWidth = 0;
	
	OnLayoutChangeListener mLayoutChangelistener = new OnLayoutChangeListener() {	
		
		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			int width = right - left;
			if (mViewWidth != width) {
				mViewWidth = width;									
			}			
		}    	
    };    
    
	public CalendarCustomFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);	
		
		addOnLayoutChangeListener(mLayoutChangelistener);
	}
    
	public float getXFraction() {  
		
        if(mViewWidth != 0)  
        {  
        	return getX() / mViewWidth; 
        }  
        else  
        {  
             return getX();  
        }  
	}  

	public void setXFraction(float xFraction) {
		
		if(mViewWidth != 0)  
        {  
			setX(xFraction * mViewWidth);  
        }  
        else  
        {  
        	setX(-9999);  
        } 		
	}  
	*/
	
	/*
	public float getXFraction() {  
        final int width = getWidth();  
          
        if(width != 0)  
        {  
             return getX() / getWidth();  
        }  
        else  
        {  
             return getX();  
        }  
   }  

   public void setXFraction(float xFraction) {      
        final int width = getWidth();  
        setX((width > 0) ? (xFraction * width) : -9999);  
   }
   */  

}
