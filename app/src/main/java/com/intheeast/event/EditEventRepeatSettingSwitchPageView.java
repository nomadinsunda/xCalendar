package com.intheeast.event;


import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.RecurrenceContext;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ViewSwitcher;

public class EditEventRepeatSettingSwitchPageView extends ViewSwitcher {

	public static final int REPEAT_MAIN_PAGE = 0;
	public static final int REPEAT_USER_SETTINGS_PAGE = 1;
	
	public static final int EVENT_NO_RECURRENCE = 0;
	public static final int EVENT_RECURRENCE_BY_EVERY_DAY = 1;
	public static final int EVENT_RECURRENCE_BY_EVERY_WEEK = 2;
	//public static final int EVENT_RECURRENCE_BY_EVERY_SECONDWEEK = 3;
	public static final int EVENT_RECURRENCE_BY_EVERY_MONTH = 3;
	public static final int EVENT_RECURRENCE_BY_EVERY_YEAR = 4;
	public static final int EVENT_RECURRENCE_BY_USER_SETTING = 5;
	
	public static final int FREQ_DAILY = 0;
	public static final int FREQ_WEEKLY = 1;
	public static final int FREQ_MONTHLY = 2;
	public static final int FREQ_YEARLY = 3;    
	
	public static final int RECURRENCE_FREQ_DAILY = 0;
	public static final int RECURRENCE_FREQ_WEEKLY = 1;
	public static final int RECURRENCE_FREQ_MONTHLY = 2;
	public static final int RECURRENCE_FREQ_YEARLY = 3;    
	
	Context mContext;
	Resources mResources;
	
	ManageEventActivity mActivity;
	EditEventFragment mFragment;
	EditEvent mEditEvent;
	
	EditEventRepeatSettingSubView mRepeatPageView;
	EditEventRepeatUserSettingSubView mRepeatUserSettingPageView;
	
	RecurrenceContext mRRuleContext = null;
	
	private ScrollInterpolator mHScrollInterpolator;
    float mViewSwitchingDelta;
    
    String mSubPageTitle;
    
    public int mPageMode = -1;
    
	public EditEventRepeatSettingSwitchPageView(Context context,
			AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;	
		mResources = mContext.getResources();
		mHScrollInterpolator = new ScrollInterpolator();
		
		mPageMode = REPEAT_MAIN_PAGE;
	}

	
	public void initView(ManageEventActivity activity, EditEventFragment fragment, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
		mEditEvent = fragment.mEditEvent;
				
		mRepeatPageView = (EditEventRepeatSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_repeat_page, null);
    	mRepeatPageView.initView(mActivity, mFragment, this, frameLayoutHeight);    	
    	
    	mRepeatPageView.addOnLayoutChangeListener(mMainPageViewOnLayoutChangeListener);
    	//mRepeatPageView.getChildViews(this);	
    	
    	mRepeatUserSettingPageView = (EditEventRepeatUserSettingSubView) mActivity.getLayoutInflater().inflate(R.layout.editevent_repeat_user_setting_page, null);
    	mRepeatUserSettingPageView.initView(mActivity, mFragment, this, frameLayoutHeight);
    	
    	mSubPageTitle = mResources.getString(R.string.editevent_repeat_sub_page_actionbar_title);
    	
    	addView(mRepeatPageView);
	}
	
	public void setRRuleContext(RecurrenceContext rruleContext) {
		mRRuleContext = rruleContext;
	}
	
	// Caller
	// -EditEvent.setMainPageLayout
	public void initEventRepeatItem() {
		int freqValue = getFreqValue();
		int freqInterval = getFreqInterval();
		
		if (freqValue == EVENT_NO_RECURRENCE) {
			mEditEvent.mEventRepeatTimesText.setText(R.string.does_not_repeat);			
			mEditEvent.mEventRepeatEndText.setText(R.string.does_not_repeatend);			
		}
		else {
			if (freqInterval == 1) {
				switch(freqValue) {
						
				case EVENT_RECURRENCE_BY_EVERY_DAY:					
					mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everyday);
					break;			
				case EVENT_RECURRENCE_BY_EVERY_WEEK:					
					mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everyweek);
					break;			
						
				case EVENT_RECURRENCE_BY_EVERY_MONTH:					
					mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everymonth);
					break;			
				case EVENT_RECURRENCE_BY_EVERY_YEAR:					
					mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everyyear);
					break;
				}		
			}
			else {
				mEditEvent.mEventRepeatTimesText.setText("����� ����");
			}
		}		
	}
	
	// Caller
	// -EditEvent.addSubViewToViewSwitcher
	public void attachRepeatPageItemSelectionIcon() {
		int freqValue = getFreqValue();
		int freqInterval = getFreqInterval();
		
		// ��ǻ� mSelectedRepeatItemLayout�� reset�� ����� �������� �����Ǵ� ���̴�
		if (freqValue == EVENT_NO_RECURRENCE) {
			mRepeatPageView.mSelectedRepeatItemLayout = freqValue;
		}
		else {
			if (freqInterval == 1) {
				mRepeatPageView.mSelectedRepeatItemLayout = freqValue;
			}
			else {
				mRepeatPageView.mSelectedRepeatItemLayout = EVENT_RECURRENCE_BY_USER_SETTING;
			}
		}
		
		mRepeatPageView.attachRepeatPageItemSelectionIcon(mRepeatPageView.mSelectedRepeatItemLayout);
	}
	
	// Caller
	// -EditEvent.switchMainView
	public void detachRepeatPageItemSelectionIcon() {		
		mRepeatPageView.detachRepeatPageItemSelectionIcon(mRepeatPageView.mSelectedRepeatItemLayout);
	}
		
	
	public void updateEventRepeatItem(int itemValue) {
		
		switch(itemValue) {
		case EVENT_NO_RECURRENCE:
			mRRuleContext.setNoRepeat();
			mEditEvent.mEventRepeatTimesText.setText(R.string.does_not_repeat);			
			mEditEvent.mEventRepeatEndText.setText(R.string.does_not_repeatend);
			break;			
		case EVENT_RECURRENCE_BY_EVERY_DAY:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_DAILY, 1);
			mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everyday);
			break;			
		case EVENT_RECURRENCE_BY_EVERY_WEEK:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_WEEKLY, 1);
			mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everyweek);
			break;			
				
		case EVENT_RECURRENCE_BY_EVERY_MONTH:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_MONTHLY, 1);
			mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everymonth);
			break;			
		case EVENT_RECURRENCE_BY_EVERY_YEAR:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_YEARLY, 1);
			mEditEvent.mEventRepeatTimesText.setText(R.string.event_repeat_everyyear);
			break;
		}		
	}
	
	public void updateEventRepeatItem(int itemValue, int interval) {
		
		switch(itemValue) {		
		case EVENT_RECURRENCE_BY_EVERY_DAY:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_DAILY, interval);			
			break;			
		case EVENT_RECURRENCE_BY_EVERY_WEEK:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_WEEKLY, interval);			
			break;			
				
		case EVENT_RECURRENCE_BY_EVERY_MONTH:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_MONTHLY, interval);			
			break;			
		case EVENT_RECURRENCE_BY_EVERY_YEAR:
			mRRuleContext.setFreqAndInterval(RECURRENCE_FREQ_YEARLY, interval);			
			break;
		}	
		
		mEditEvent.mEventRepeatTimesText.setText("����� ����");
	}
		 
	public void switchMainView() {
		mEditEvent.switchMainView();
    	mEditEvent.setMainPageActionBar(); 
	}
	
	public void adjustMainViewLayout(int selectedFreqValue) {
		if (selectedFreqValue == EVENT_NO_RECURRENCE) {
    		mEditEvent.mBetweenRepeatAndRepeatEndLayout.setVisibility(View.GONE);
    		mEditEvent.mRepeatEndTimeLayout.setVisibility(View.GONE);
    	}
    	else {
    		mEditEvent.mBetweenRepeatAndRepeatEndLayout.setVisibility(View.VISIBLE);
    		mEditEvent.mRepeatEndTimeLayout.setVisibility(View.VISIBLE);
    	}
	}
	
	public int getFreqValue() {
    	int freqValue = mRRuleContext.getFreq();
    	    	
    	switch(freqValue) {
    	case -1:
    		freqValue = EVENT_NO_RECURRENCE;    		
    		break;
    	case FREQ_DAILY:
    		freqValue = EVENT_RECURRENCE_BY_EVERY_DAY;    		
    		break;
    	case FREQ_WEEKLY: 
    		freqValue = EVENT_RECURRENCE_BY_EVERY_WEEK;   		
    		break;
    	case FREQ_MONTHLY:
    		freqValue = EVENT_RECURRENCE_BY_EVERY_MONTH;    		
    		break;
    	case FREQ_YEARLY:
    		freqValue = EVENT_RECURRENCE_BY_EVERY_YEAR;    		
    		break;
    	default:
    		//Log.i("tag", "prvFreqValue = ???");
    		return freqValue;
    	}
    	
    	return freqValue;
    }
	
	public int getFreqInterval() {
    	int freqInterval = mRRuleContext.getFreqInterval();
    	return freqInterval;
    }
	
	
	
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
	
	private static final int MINIMUM_SNAP_VELOCITY = 2200;
	public static final float SWITCH_SUB_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.25f;
    public static final float SWITCH_MAIN_PAGE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f;
    final float SWITCH_MAIN_PAGE_IN_FROMXVALUE = -1.0f;
    final float SWITCH_MAIN_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_TOXVALUE = 1.0f;
	
    final float SWITCH_SUB_PAGE_IN_FROMXVALUE = 1.0f;
    final float SWITCH_SUB_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_TOXVALUE = -1.0f;
    
	public void switchSubView(boolean switchSubView) {
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
                   
        if (!switchSubView) {
        	outAnimation.setAnimationListener(mOutAnimListener);
        	
        	mPageMode = REPEAT_MAIN_PAGE;
        	showPrevious();          	
    		removeView(mRepeatUserSettingPageView);  
    		setRepeatMainPageActionBar();
    	}
        else {  
        	inAnimation.setAnimationListener(mInAnimListener);
        	
        	mPageMode = REPEAT_USER_SETTINGS_PAGE;
        	// mEventInfoView �� null ��
        	//mEventInfoView.mFragment.replaceActionBarBySelectingEventItemLayout(EventInfoViewUpperActionBarFragment.EVENT_ATTENDEE_SUB_PAGE);
        	//inflateSubPage(item);
    		addView(mRepeatUserSettingPageView);    
    		setRepeatUserSettinsPageActionBar();
        	showNext();        	
        }
    }
	
	AnimationListener mInAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			mFragment.mActionBar.startSubPageOfSubPageActionBarEnterAnim();
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	AnimationListener mOutAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			mFragment.mActionBar.startSubPageActionBarReEntrance();
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	public void setRepeatUserSettinsPageActionBar() {
		
		float enterAnimationDistance = mViewWidth; 
		float exitAnimationDistance = mViewWidth * 0.4f; 
		
		float velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mViewWidth, MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
    	
		mFragment.mActionBar.setSubPageOfSubPageActionBar(mSubPageTitle,
				enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance);
	}
	
	public void setRepeatMainPageActionBar() {
		
		float enterAnimationDistance = mViewWidth; 
		float exitAnimationDistance = mViewWidth * 0.4f; 
		
		float velocity = SWITCH_MAIN_PAGE_VELOCITY;
    	long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mViewWidth, MINIMUM_SNAP_VELOCITY, velocity); //--------------------------------------------------------------------//
    	long exitAnimationDuration = enterAnimationDuration;
    	
		mFragment.mActionBar.setSubPageActionBarReEntrance(
				enterAnimationDuration, enterAnimationDistance,
				exitAnimationDuration, exitAnimationDistance);
	}
	
	
	
	
	private float mAnimationDistance = 0;
    

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
    
    public static class FreqType {
		int freqValue;
		int freqInterval;
		
		public FreqType(int freqValue, int freqInterval) {
			this.freqValue = freqValue;
			this.freqInterval = freqInterval;
		}
	}
    
    /*
	public FreqType getUserSettingsFreqValue() {
    	int freqValue = mRRuleContext.getFreq();
    	int freqInterval = mRRuleContext.getFreqInterval();
    	
    	FreqType freqType = new FreqType(freqValue, freqInterval);
    	
    	if (freqValue >= 0) {
	    	if (freqInterval == 1) {
	    		switch(freqValue) {
	        	case -1:
	        		freqValue = EVENT_NO_RECURRENCE;    		
	        		break;
	        	case FREQ_DAILY:
	        		freqValue = EVENT_RECURRENCE_BY_EVERY_DAY;    		
	        		break;
	        	case FREQ_WEEKLY: 
	        		freqValue = EVENT_RECURRENCE_BY_EVERY_WEEK;   		
	        		break;
	        	case FREQ_MONTHLY:
	        		freqValue = EVENT_RECURRENCE_BY_EVERY_MONTH;    		
	        		break;
	        	case FREQ_YEARLY:
	        		freqValue = EVENT_RECURRENCE_BY_EVERY_YEAR;    		
	        		break;
	        	default:
	        		//Log.i("tag", "prvFreqValue = ???");
	        		return freqValue;
	        	}
	    	}
	    	else {
	    		
	    	}
    	}
    	else {
    		freqValue = EVENT_NO_RECURRENCE;    	
    	}
    	
    }
	*/
}
