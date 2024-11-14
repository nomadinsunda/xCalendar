package com.intheeast.anim;

import android.animation.ValueAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;

public class ITEAnimInterpolator implements Interpolator{

	public static final int ANIMATION_CASE = 0;
	public static final int ANIMATIONSET_CASE = 1;
	public static final int VALUEANIMATOR_CASE = 2;
	
	int mWhichAnim = -1;
	
	float mAnimationDistance;
	Animation mAnimation;
	AnimationSet mAnimationSet;
	ValueAnimator mAnimator;
	
	public ITEAnimInterpolator(float animationDistance, Animation animation) {
		mAnimationDistance = animationDistance;
    	mAnimation = animation;
    	mWhichAnim = ANIMATION_CASE;
	}
	
	public ITEAnimInterpolator(float animationDistance, AnimationSet animationSet) {
		mAnimationDistance = animationDistance;
		mAnimationSet = animationSet;
    	mWhichAnim = ANIMATIONSET_CASE;
	}
	
	public ITEAnimInterpolator(float animationDistance, ValueAnimator animator) {
		mAnimationDistance = animationDistance;
		mAnimator = animator;
		mWhichAnim = VALUEANIMATOR_CASE;
	}
	
	@Override
	public float getInterpolation(float t) {
		t -= 1.0f;
        t = t * t * t * t * t + 1;

        if ((1 - t) * mAnimationDistance < 1) {
            cancelAnimation();
        }

        return t;
	}
	
	private void cancelAnimation() {    
		
		if (mWhichAnim == ANIMATION_CASE)
			mAnimation.scaleCurrentDuration(0);   
		else if (mWhichAnim == ANIMATIONSET_CASE)
			mAnimationSet.scaleCurrentDuration(0);
		else 
			mAnimator.setDuration(0);       
    }

}
