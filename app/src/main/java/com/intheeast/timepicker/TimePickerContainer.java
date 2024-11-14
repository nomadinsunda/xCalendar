package com.intheeast.timepicker;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.LinearLayout;

public class TimePickerContainer extends LinearLayout {

	public static final int SLIDE_TIME_PICKER_ANIMATION = 1;
	public static final int ALPHA_TIME_PICKER_ANIMATION = 2;
	
	TextureView mTimePickerTextureView;
	public TimePickerSurfaceTextureListener mTimePickerSurfaceTextureListener;
	public int mTimePickerAnimationCase = 0;
	public ValueAnimator mTimePickerAnimator;
	
	public TimePickerContainer (Context context) {
		super(context);			
	}
	
	public TimePickerContainer (Context context, AttributeSet attrs) {
		super(context, attrs);	
	}
	
	public TimePickerContainer (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);		
	} 
	
	public void init(Activity activity, LayoutParams textureViewParams, TimePickerRenderThread timePicker, int timePickerAnimationCase) {
		mTimePickerAnimationCase = timePickerAnimationCase;
		
		mTimePickerTextureView = new TextureView(activity);		
		mTimePickerTextureView.setLayoutParams(textureViewParams);	
		
		mTimePickerSurfaceTextureListener = new TimePickerSurfaceTextureListener(timePicker, mTimePickerTextureView);
		mTimePickerTextureView.setSurfaceTextureListener(mTimePickerSurfaceTextureListener);	
				
		addView(mTimePickerTextureView);
		getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
		
		setVisibility(View.GONE);    
	}	
	
	OnPreDrawListener mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {            
        @Override
        public boolean onPreDraw() {
        	getViewTreeObserver().removeOnPreDrawListener(this);
        	    
        	final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    		final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    		measure(widthSpec, heightSpec);
    		
        	if (mTimePickerAnimationCase == SLIDE_TIME_PICKER_ANIMATION) {
	    		mTimePickerAnimator = makeTimePickerSlideAnimator(0, TimePickerContainer.this.getMeasuredHeight());
        	}
        	//else {
        		
        	//}
            return true;
        }
    };
    
    private ValueAnimator makeTimePickerSlideAnimator(int start, int end) {

		ValueAnimator animator = ValueAnimator.ofInt(start, end);

		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				int value = (Integer) valueAnimator.getAnimatedValue();

				ViewGroup.LayoutParams layoutParams = getLayoutParams();
				layoutParams.height = value;
				TimePickerContainer.this.setLayoutParams(layoutParams);
			}
		});
		return animator;
	}
    
    boolean mTransparent;
    public ValueAnimator makeTimePickerAlphaAnimator(boolean transparent) {
    	mTransparent = transparent;
    	
    	ValueAnimator animator;
    	if (mTransparent) {
    		animator = ValueAnimator.ofFloat(1.0f, 0.0f);
    	}
    	else {
    		animator = ValueAnimator.ofFloat(0.0f, 1.0f);
    	}

		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//Update Height
				float value = (Float) valueAnimator.getAnimatedValue();
				TimePickerContainer.this.setAlpha(value);
				
			}
		});		
		
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
		    public void onAnimationRepeat(Animator animation) {
		    }
			
            @Override
            public void onAnimationEnd(Animator animation) {
               if (mTransparent) {
            	   TimePickerContainer.this.setVisibility(View.GONE);
               }
            }
        });
		return animator;
	}
}
