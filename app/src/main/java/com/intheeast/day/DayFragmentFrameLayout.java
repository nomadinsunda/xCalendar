package com.intheeast.day;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class DayFragmentFrameLayout extends FrameLayout {
	View mPsudoActionbarInterpolationView;
	private float xFraction = 0;
	int mLayoutWidth;
    public DayFragmentFrameLayout(Context context) {
        super(context);
    }

    public DayFragmentFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);        
    }

    public DayFragmentFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private ViewTreeObserver.OnPreDrawListener preDrawListener = null;

    public void setXFraction(float fraction) {

        this.xFraction = fraction;

        if (getWidth() == 0) {
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
            return;
        }

        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }

    public float getXFraction() {
        return this.xFraction;
    }
    
    /*
    public void setPsudoActionBarView() {
		mPsudoActionbarInterpolationView = findViewById(R.id.psudo_actionbar_interpolation_container);
	}    
    
    public void setPsudoActionBarViewVisible() {
		mPsudoActionbarInterpolationView.setVisibility(View.VISIBLE);	
	}
    */
    
	/*
	int mDayViewHeight = 0;
	int mDayViewWidth = 0;
	
	View mPsudoActionbarInterpolationView;
	
	OnLayoutChangeListener mLayoutChangelistener = new OnLayoutChangeListener() {

		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			int width = right - left;
			if (mDayViewWidth != width) {
				mDayViewWidth = width;									
			}			
		}    	
    };    
    
	public DayFragmentFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);		
		
		addOnLayoutChangeListener(mLayoutChangelistener);
	}    
    
	public void setPsudoActionBarView() {
		mPsudoActionbarInterpolationView = findViewById(R.id.psudo_actionbar_interpolation_container);
	}
	
	public void setPsudoActionBarViewVisible() {
		mPsudoActionbarInterpolationView.setVisibility(View.VISIBLE);	
	}
	
	public float getXFraction() {  
		
        if(mDayViewWidth != 0)  
        {  
        	return getX() / mDayViewWidth; 
        }  
        else  
        {  
             return getX();  
        }  
	}  

	public void setXFraction(float xFraction) {
		
		if(mDayViewWidth != 0)  
        {  
			setX(xFraction * mDayViewWidth);  
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
