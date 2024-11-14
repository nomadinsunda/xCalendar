package com.intheeast.acalendar;

import java.util.ArrayList;

import com.intheeast.event.EditEventHelper.AttendeeItem;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ViewSwitcher;

public class AttendeesInfoSwitchPageView extends ViewSwitcher {
	Context mContext;
	FastEventInfoView mEventInfoView;
	private final LayoutInflater mInflater;
	AttendeesInfoMainPageView mMainPageView;
	AttendeesInfoSubPageView mSubPageView;
	
	int mViewWidth = -1;
	int mViewHeight;
    TranslateAnimation leftInAnimationForMainbackText;
    OnLayoutChangeListener mMainPageViewOnLayoutChangeListener = new OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {		
			
			if (mViewWidth == -1) {
				mViewWidth = right - left;				
	    		
	    		float xOffSet = mViewWidth / 2;
	    		mViewSwitchingDelta = mViewWidth - xOffSet;
	    		mAnimationDistance = mViewWidth - xOffSet;
	    			
	    		leftInAnimationForMainbackText = new TranslateAnimation(
	    	            Animation.ABSOLUTE, mViewWidth / 2, //fromXValue 
	    	            Animation.RELATIVE_TO_PARENT, 0,   //toXValue
	    	            Animation.ABSOLUTE, 0.0f,
	    	            Animation.ABSOLUTE, 0.0f); 				
			}
		}		
	};
	
	public AttendeesInfoSwitchPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mHScrollInterpolator = new ScrollInterpolator();
		mMainPageView = null;
		mSubPageView = null;		
	}
	
	public void setEventInfoView(FastEventInfoView eventInfoView) {
		mEventInfoView = eventInfoView;
	}
	
	public void inflateMainPage() {		
		mMainPageView = (AttendeesInfoMainPageView)mInflater.inflate(R.layout.attendees_information_mainpage_layout, null);
		mMainPageView.addOnLayoutChangeListener(mMainPageViewOnLayoutChangeListener);
		mMainPageView.getChildViews(this);		
	}
	
	public void inflateSubPage(AttendeeItem item) {		
		if (mSubPageView != null)
			mSubPageView.removeAllViews();
		
		mSubPageView = (AttendeesInfoSubPageView)mInflater.inflate(R.layout.attendee_information_subpage_layout, null);
		mSubPageView.getChildViews(this);
		mSubPageView.setInfo(item);
	}
	
	public void constructMainPage(ArrayList<AttendeeItem> list) {
		
		if (mMainPageView == null) // 미친 코드이다!!! 차후에 삭제해야 한다!!!
			mMainPageView = (AttendeesInfoMainPageView)mInflater.inflate(R.layout.attendees_information_mainpage_layout, null);
		else {
			// removeAllViews을 호출하면,,,
			// attendees_infopage_scroll_view 부터 attendees_infopage_scroll_view의 child까지 모두 제거되므로,,,
			// removeAllViews을 호출하면 안된다...
			// attendees item들만 삭제해야 한다
			//mMainPageView.removeAllViews();
			// attendees_info_page_list_view의 child만 삭제한다
			mMainPageView.removeItemInContainerList();
			removeView(mMainPageView);			
		}
		
		mMainPageView.addAttendeesLayout(list);
		
		// onLayout이 먼저 호출될 수 있도록 inflateMainPage 함수로 호출 부분을 수정한다
		// :호출이 안된다...ㅡㅡ;!!!
		addView(mMainPageView);
	}	
	
	public static final float SWITCH_SUB_PAGE_VELOCITY = 2200 * 1.25f;
    public static final float SWITCH_MAIN_PAGE_VELOCITY = 2200 * 1.5f;
    final float SWITCH_MAIN_PAGE_IN_FROMXVALUE = -1.0f;
    final float SWITCH_MAIN_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_TOXVALUE = 1.0f;
	
    final float SWITCH_SUB_PAGE_IN_FROMXVALUE = 1.0f;
    final float SWITCH_SUB_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_TOXVALUE = -1.0f;
	
    private ScrollInterpolator mHScrollInterpolator;
    float mViewSwitchingDelta;
    
    public void settingViewSwitching(boolean switchSubView, AttendeeItem item) {
    	float inFromXValue = 0;
    	float inToXValue = 0;
    	float outFromXValue = 0;
    	float outToXValue = 0;
    	float velocity = 0;    	
    	
    	if (!switchSubView) {
	    	inFromXValue = SWITCH_MAIN_PAGE_IN_FROMXVALUE;
	    	inToXValue = SWITCH_MAIN_PAGE_TO_FROMXVALUE;
	    	outFromXValue = SWITCH_MAIN_PAGE_OUT_FROMXVALUE;
	    	outToXValue = SWITCH_MAIN_PAGE_OUT_TOXVALUE;
	    	velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	}
    	else {
    		inFromXValue = SWITCH_SUB_PAGE_IN_FROMXVALUE;
	    	inToXValue = SWITCH_SUB_PAGE_TO_FROMXVALUE;
	    	outFromXValue = SWITCH_SUB_PAGE_OUT_FROMXVALUE;
	    	outToXValue = SWITCH_SUB_PAGE_OUT_TOXVALUE;
	    	velocity = SWITCH_SUB_PAGE_VELOCITY;
    	}
    	
    	TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        
        long duration = calculateDuration(mViewSwitchingDelta, mViewWidth, velocity);
        inAnimation.setDuration(duration);
        inAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setDuration(duration);        
        
    	setInAnimation(inAnimation);
        setOutAnimation(outAnimation);
        
        // action bar의 previous icon을 선택함으로써!!!
        // EventInfoViewUpperActionBarFragment::mActionBarListener
        // ->EventInfoViewUpperActionBarFragment::onActionBarItemSelected
        //   ->EventInfoViewUpperActionBarFragment::replaceActionBarBySelectingPreviousIcon(EVENT_ATTENDEE_PAGE); // action bar 설정
        //     ->EventInfoFragment.attendeePageViewSwitching(false);
        //       ->AttendeesInfoSwitchPageView.settingViewSwitching(subPageSwitching, null);        
        if (!switchSubView) {
        	showPrevious();          	
    		removeView(mSubPageView);
    				
    	}
        else {      
        	// mEventInfoView 가 null 임
        	mEventInfoView.mFragment.replaceActionBarBySelectingEventItemLayout(EventInfoViewUpperActionBarFragment.EVENT_ATTENDEE_SUB_PAGE);
        	inflateSubPage(item);
    		addView(mSubPageView);    			
        	showNext();        	
        }
    }
    
    private void cancelAnimation() {
        Animation in = getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
    }
    
    private float mAnimationDistance = 0;
    private static final int MINIMUM_SNAP_VELOCITY = 2200;

    private class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
    }
    
    public long calculateDuration(float delta, float width, float velocity) {
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
        float f1 = MINIMUM_SNAP_VELOCITY * 2;
        velocity = Math.max(f1, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        /*if (DEBUG) {
            Log.e(TAG, "halfScreenSize:" + halfScreenSize + " delta:" + delta + " distanceRatio:"
                    + distanceRatio + " distance:" + distance + " velocity:" + velocity
                    + " duration:" + duration + " distanceInfluenceForSnapDuration:"
                    + distanceInfluenceForSnapDuration);
        }*/
        return duration;
    }
    
    /*
     * We want the duration of the page snap animation to be influenced by the
     * distance that the screen has to travel, however, we don't want this
     * duration to be effected in a purely linear fashion. Instead, we use this
     * method to moderate the effect that the distance of travel has on the
     * overall snap duration.
     */
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
}
