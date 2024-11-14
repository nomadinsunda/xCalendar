package com.intheeast.anim;

import android.annotation.SuppressLint;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

public class ITEAnimationUtils {
	
	public static final int ALPHA_OPAQUE_TO_TRANSPARENT = 1;
	public static final int ALPHA_TRANSPARENT_TO_OPAQUE = 2;
	public static final int ANIMATION_INTERPOLATORTYPE_ACCELERATE = 1;
	public static final int ANIMATION_INTERPOLATORTYPE_DECELERATE = 2;
	
	public static TranslateAnimation makeSlideAnimation(int fromXType, float fromXDelta, int toXType, float toXDelta, long duration, float distance) {    	
    	
    	TranslateAnimation translateAnimation = new TranslateAnimation(
    			fromXType, fromXDelta, //fromXValue 
    			toXType, toXDelta,   //toXValue
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
                
        translateAnimation.setDuration(duration);
        
        ITEAnimInterpolator i = new ITEAnimInterpolator(distance, translateAnimation);  
        translateAnimation.setInterpolator(i);
        
        return translateAnimation;        
	}
	
	@SuppressLint("SuspiciousIndentation")
	public static AlphaAnimation makeAlphaAnimation(int alphaType, float fromAlpha, float toAlpha, long duration, float distance) {
    	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);        
        
        alphaAnimation.setDuration(duration);
        
        ITEAnimInterpolator i = new ITEAnimInterpolator(distance, alphaAnimation);  
        alphaAnimation.setInterpolator(i);
                
        return alphaAnimation;        
	}	   
	
	public static AlphaAnimation makeAlphaDecelerateAnimation(int alphaType, float fromAlpha, float toAlpha, long duration, float distance, float factor) {    	
    	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);        
        
        alphaAnimation.setDuration(duration);
                
        // An interpolator where the rate of change starts out quickly and and then decelerates.
        alphaAnimation.setInterpolator(new DecelerateInterpolator(factor));
                
        return alphaAnimation;        
	}	   
	
	public static AlphaAnimation makeAlphaAccelerateAnimation(int alphaType, float fromAlpha, float toAlpha, long duration, float distance, float factor) {    	
    	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);        
        
        alphaAnimation.setDuration(duration);
                
        // An interpolator where the rate of change starts out slowly and and then accelerates.
        alphaAnimation.setInterpolator(new AccelerateInterpolator(factor));
                
        return alphaAnimation;        
	}	   
	
	
	public static long calculateDuration(float delta, float width, float minVelocity, float velocity) {
        
    	/*
         * Here we compute a "distance" that will be used in the computation of
         * the overall snap duration. This is a function of the actual distance
         * that needs to be traveled; we keep this value close to half screen
         * size in order to reduce the variance in snap duration as a function
         * of the distance the page needs to travel.
         */
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        velocity = Math.max(minVelocity, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }    
    
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
}
