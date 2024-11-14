package com.intheeast.event;

import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CommonRelativeLayoutUpperActionBar;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.event.EditEventFragment.Done;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class EditEventActionBarFragment extends Fragment {

	private static final String TAG = "EditEventActionBarFragment";
	private static final boolean INFO = true;
	
	public static final int EDIT_EVENT_MAIN_PAGE = 0;
	public static final int EDIT_EVENT_SUB_PAGE = 1;
	public static final int EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE = 2;
	
	public static final int SELECTED_PREVIOUS_ACTION = 1;
	
	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mMainFragment;
	CalendarController mController;
	
	CommonRelativeLayoutUpperActionBar mFrameLayout;
	
	public ImageView mPreviousBackIcon;
	public TextView mPsudoPreviousBackText;
	public TextView mPreviousBackText;
	
	public TextView mTitleText;
    public TextView mCancelText;
    public TextView mCompletionText;
    public TextView mSubpageTitleText;   
    public TextView mSubpageOfSubpageTitleText;
    
    
    int mWindowWidth;
    int mActionBarHeight;
    
    int mFragmentViewState;
    public Done mOnDone; // EditEventFragment �κ��� ���� �����;� �Ѵ�
    
    int mPageMode = -1;
    
    public EditEventActionBarFragment() {
    	
	}
    
    @SuppressLint("ValidFragment")
	public EditEventActionBarFragment(Activity activity, int viewState, EditEventFragment mainFragment) {
    	mActivity = activity;
		mFragmentViewState = viewState;
		mMainFragment = mainFragment;
		mOnDone = mainFragment.mOnDone;
    	mMainFragment.setActionBarFragment(this);
    	
    	mFrameLayout = (CommonRelativeLayoutUpperActionBar) mActivity.getLayoutInflater().inflate(R.layout.editevent_actionbar, null);
    	
    	mPageMode = EDIT_EVENT_MAIN_PAGE;
	}
    
    @Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		if (INFO) Log.i(TAG, "onAttach");
		
		mContext = activity.getApplicationContext();
		mResources = mContext.getResources();
		mController = CalendarController.getInstance(activity);
	}	
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        Rect rectgle = new Rect(); 
    	Window window = mActivity.getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	
    	mWindowWidth = rectgle.right - rectgle.left;    	
        mActionBarHeight =  (int) getResources().getDimension(R.dimen.calendar_view_upper_actionbar_height); //50dip        
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		if (INFO) Log.i(TAG, "onCreateView");	
		
		ManageEventActivity activity = (ManageEventActivity) mActivity;
		activity.mEditEventActionbarFrameLayout.removeView(mFrameLayout);////////////////////////		
		
		mTitleText = (TextView) mFrameLayout.findViewById(R.id.editevent_main_actionbar_title_textview);
		
		mCancelText = (TextView) mFrameLayout.findViewById(R.id.editevent_main_actionbar_cancel_textview);
		mCancelText.setOnTouchListener(mCancelTextTouchListener);
		mCancelText.setOnClickListener(mActionBarListener);    
		
		mCompletionText = (TextView) mFrameLayout.findViewById(R.id.editevent_main_actionbar_completion_textview);
		mCompletionText.setOnClickListener(mActionBarListener);
		
        mPreviousBackIcon = (ImageView) mFrameLayout.findViewById(R.id.editevent_actionbar_previous_back_icon);
        mPreviousBackIcon.setOnTouchListener(mPreviousBackActionIconTouchListener);
        mPreviousBackIcon.setOnClickListener(mPreviousBackIconClickListener);
        
        //editevent_actionbar_psudo_previous_back_text
        mPsudoPreviousBackText = (TextView) mFrameLayout.findViewById(R.id.editevent_actionbar_psudo_previous_back_text);     
        
        mPreviousBackText = (TextView) mFrameLayout.findViewById(R.id.editevent_actionbar_previous_back_text);
        mPreviousBackText.setOnTouchListener(mPreviousBackTextTouchListener);
        mPreviousBackText.setOnClickListener(mPreviousBackTextClickListener);
        
        mSubpageTitleText = (TextView) mFrameLayout.findViewById(R.id.editevent_actionbar_sub_title_textview);
        mSubpageOfSubpageTitleText = (TextView) mFrameLayout.findViewById(R.id.editevent_actionbar_sub_of_sub_title_textview);
        
		return mFrameLayout;
		
	}	
	
	OnTouchListener mCancelTextTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:				
				/*AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
				downAlpha.setDuration(0); 
				downAlpha.setFillAfter(true);		
				mPreviousBackIcon.startAnimation(downAlpha);*/
				mCancelText.setAlpha(0.2f);
				
								
				break;
			case MotionEvent.ACTION_UP:			
				/*AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
				upAlpha.setDuration(0); 
				upAlpha.setFillAfter(true); 			
				mPreviousBackIcon.startAnimation(upAlpha);*/	
				
				mCancelText.setAlpha(1.0f);	
				
				break;
				
				
			default:
				break;
			}
			
			return false;
		}    	
    };
    
	OnTouchListener mPreviousBackActionIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:				
				/*AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
				downAlpha.setDuration(0); 
				downAlpha.setFillAfter(true);		
				mPreviousBackIcon.startAnimation(downAlpha);*/
				mPreviousBackIcon.setAlpha(0.2f);
				mPreviousBackText.setAlpha(0.2f);		
								
				break;
			case MotionEvent.ACTION_UP:			
				/*AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
				upAlpha.setDuration(0); 
				upAlpha.setFillAfter(true); 			
				mPreviousBackIcon.startAnimation(upAlpha);*/	
				
				mPreviousBackIcon.setAlpha(1.0f);	
				mPreviousBackText.setAlpha(1.0f);	
				break;
				
				
			default:
				break;
			}
			
			return false;
		}    	
    };
    
    OnTouchListener mPreviousBackTextTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:				
				/*AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
				downAlpha.setDuration(0); 
				downAlpha.setFillAfter(true);		
				mPreviousBackIcon.startAnimation(downAlpha);*/
				mPreviousBackIcon.setAlpha(0.2f);
				mPreviousBackText.setAlpha(0.2f);		
								
				break;
			case MotionEvent.ACTION_UP:			
				/*AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
				upAlpha.setDuration(0); 
				upAlpha.setFillAfter(true); 			
				mPreviousBackIcon.startAnimation(upAlpha);*/	
				
				mPreviousBackIcon.setAlpha(1.0f);	
				mPreviousBackText.setAlpha(1.0f);	
				break;
				
				
			default:
				break;
			}
			
			return false;
		}    	
    };
    
	//CalendarEventModel mModel; // EditEventFragment �κ��� ���� �����;� �Ѵ�
	//CalendarEventModel mOriginalModel = null; // EditEventFragment �κ��� ���� �����;� �Ѵ�
	//EditEvent mEditEventView = null; // EditEventFragment �κ��� ���� �����;� �Ѵ�
	//int mModification; // EditEventFragment �κ��� ���� �����;� �Ѵ�
	
	private final View.OnClickListener mActionBarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onActionBarItemSelected(v.getId());
        }
    };
    
    private boolean onActionBarItemSelected(int itemId) {
        if (itemId == R.id.editevent_main_actionbar_completion_textview) {
        	
            if (EditEventHelper.canModifyEvent(mMainFragment.mModel) || EditEventHelper.canRespond(mMainFragment.mModel)) {
                if (mMainFragment.mEditEvent != null ) {
                    if (mMainFragment.mModification == Utils.MODIFY_UNINITIALIZED) {
                    	mMainFragment.mModification = Utils.MODIFY_ALL;
                    }
                    mOnDone.setDoneCode(Utils.DONE_SAVE | Utils.DONE_EXIT);
                    mOnDone.run();
                } else {
                    mOnDone.setDoneCode(Utils.DONE_REVERT);
                    mOnDone.run();
                }
            } else if (EditEventHelper.canAddReminders(mMainFragment.mModel) && 
            		mMainFragment.mModel.mId != -1
                    && mMainFragment.mOriginalModel != null 
                    && mMainFragment.mEditEvent.prepareForSave()) {
                //saveReminders();
                mOnDone.setDoneCode(Utils.DONE_EXIT);
                mOnDone.run();
            } else {
                mOnDone.setDoneCode(Utils.DONE_REVERT);
                mOnDone.run();
            }
            
        } else if (itemId == R.id.editevent_main_actionbar_cancel_textview) {
            mOnDone.setDoneCode(Utils.DONE_REVERT);
            mOnDone.run();
        }
        return true;
    }
    
    OnClickListener mPreviousBackIconClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mPageMode == EDIT_EVENT_SUB_PAGE) {
				// ������ Main Fragment ViewState�� ���� Ȯ���ϰ� MainFragment���� Msg�� �����ؾ� �Ѵ�
				Message msg = Message.obtain();	
				msg.arg1 = SELECTED_PREVIOUS_ACTION;
				msg.arg2 = EDIT_EVENT_SUB_PAGE;
				mMainFragment.notifyedMsgByActionBar(msg);
			}
			else if (mPageMode == EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE) {
				Message msg = Message.obtain();	
				msg.arg1 = SELECTED_PREVIOUS_ACTION;
				msg.arg2 = EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE;				
				mMainFragment.notifyedMsgByActionBar(msg);
			}			
		}    	
    };
    
    OnClickListener mPreviousBackTextClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mPageMode == EDIT_EVENT_SUB_PAGE) {
				// ������ Main Fragment ViewState�� ���� Ȯ���ϰ� MainFragment���� Msg�� �����ؾ� �Ѵ�
				Message msg = Message.obtain();	
				msg.arg1 = SELECTED_PREVIOUS_ACTION;
				msg.arg2 = EDIT_EVENT_SUB_PAGE;
				mMainFragment.notifyedMsgByActionBar(msg);
			}
			else if (mPageMode == EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE) {
				Message msg = Message.obtain();	
				msg.arg1 = SELECTED_PREVIOUS_ACTION;
				msg.arg2 = EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE;				
				mMainFragment.notifyedMsgByActionBar(msg);
			}		
			
		}    	
    };
    
    AlphaAnimation mPreviousBackIconDisappearAlpha;
    AnimationSet mSubpageTitleTextDisappearAnimationSet;
    AnimationSet mTitleTextAppearAnimationSet;
    AlphaAnimation mCancelTextAppearAlpha;
    AlphaAnimation mCompletionTextAppearAlpha;
    public void setMainViewActionBar(long entranceAnimDuration, float entranceDistance,
			long exitAnimDuration, float exitDistance) {
    	
    	if (mPreviousBackTextLeft == -1) {
        	mPreviousBackTextLeft = mPreviousBackText.getLeft();
        }
    	
    	mPreviousBackIconDisappearAlpha = ITEAnimationUtils.makeAlphaDecelerateAnimation(ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT,
        		1.0f, 0.0f, exitAnimDuration, exitDistance, 2); 
    	mPreviousBackIconDisappearAlpha.setAnimationListener(mPreviousBackIconDisappearAnimlistener);    
		        
    	mSubpageTitleTextDisappearAnimationSet = new AnimationSet(false);
    	float toXDelta = getToXDeltaOfSubPageTitleTextExit(mSubpageTitleText.getWidth());
        TranslateAnimation psudoSubpageTitleTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
      			 Animation.RELATIVE_TO_SELF, 0.0f,
       			 Animation.ABSOLUTE, toXDelta,
       			 exitAnimDuration, exitDistance);
        mSubpageTitleTextDisappearAnimationSet.addAnimation(psudoSubpageTitleTextTransAnim);
        AlphaAnimation psudoSubpageTitleTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT,
        		1.0f, 0.0f, exitAnimDuration, exitDistance); 
        mSubpageTitleTextDisappearAnimationSet.addAnimation(psudoSubpageTitleTextAlphaAnim);       
        mSubpageTitleTextDisappearAnimationSet.setAnimationListener(mSubPageTitleTextDisappearAnimlistener);
        
        mTitleTextAppearAnimationSet = new AnimationSet(false);       
        float fromXDelta = -(mTitleTextLeft - mPreviousBackTextLeft);
        TranslateAnimation titleTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
     			 Animation.ABSOLUTE, fromXDelta,
      			 Animation.RELATIVE_TO_SELF, 0.0f,
      			 entranceAnimDuration, entranceDistance);
        mTitleTextAppearAnimationSet.addAnimation(titleTextTransAnim);
        AlphaAnimation titleTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance);
        mTitleTextAppearAnimationSet.addAnimation(titleTextAlphaAnim);
        mTitleTextAppearAnimationSet.setAnimationListener(mTitleTextAppearAnimlistener);
        
        
        mCancelTextAppearAlpha = ITEAnimationUtils.makeAlphaAccelerateAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance, 2);       
        
        mCompletionTextAppearAlpha = ITEAnimationUtils.makeAlphaAccelerateAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance, 2);          
        
        mPageMode = EDIT_EVENT_MAIN_PAGE;
	}
    
    public void startMainPageActionBarEnterAnim() {
    	mPreviousBackIcon.startAnimation(mPreviousBackIconDisappearAlpha);  
    	mCancelText.setVisibility(View.VISIBLE);	
		mCancelText.startAnimation(mCancelTextAppearAlpha); 
		
    	mSubpageTitleText.startAnimation(mSubpageTitleTextDisappearAnimationSet);
    	
    	mTitleText.setVisibility(View.VISIBLE);        
        mTitleText.startAnimation(mTitleTextAppearAnimationSet); 
        
        mCompletionText.setVisibility(View.VISIBLE);
        mCompletionText.startAnimation(mCompletionTextAppearAlpha);  
        
    }
    
    AnimationListener mPreviousBackIconDisappearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPreviousBackIcon.setVisibility(View.GONE);	
			
			//mCancelText.setVisibility(View.VISIBLE);	
			//mCancelText.startAnimation(mCancelTextAppearAlpha); 
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
    AnimationListener mTitleTextAppearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			mPreviousBackText.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationEnd(Animation animation) {		
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
    
    
	AnimationListener mSubPageTitleTextDisappearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mSubpageTitleText.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
    
    public float getToXDeltaOfSubPageTitleTextExit(int subPageTextWidth) {
    	int rightMargin = (int) mResources.getDimension(R.dimen.actionBarRightSideTextViewMarginRight);    	
    	int right = mWindowWidth - rightMargin;
    	int exitToLeft = right - subPageTextWidth;
    	float toXDelta =  exitToLeft - mSubpageTitleText.getLeft();
    	return toXDelta;
    }
    
    public int getSubPageTitleExitAnimLeft(int subPageTextWidth) {
    	//int xright = mCompletionText.getRight();
    	int rightMargin = (int) mResources.getDimension(R.dimen.actionBarRightSideTextViewMarginRight);
    	//int rightMargin = (int) EUtil.dipToPixels(mContext, dipValue);
    	int right = mWindowWidth - rightMargin;
    	int left = right - subPageTextWidth;
    	return left;
    } 
    
	AlphaAnimation mCancelTextDisappearAlpha;
    AlphaAnimation mPreviousBackIconAppearAlpha;
    //AlphaAnimation mPreviousBackTextAppearAlpha;
    //AnimationSet mTitleTextDisappearAnimationSet;
    //ValueAnimator mTitleTextDisappearVA;
    AnimatorSet mTitleTextDisappearVASet;
    AnimationSet mSubTitleTextAppearAnimationSet;
    float mTitleTextLeft = -1;
    float mPreviousBackTextLeft = -1;
    float mPreviousBackIconRight = -1;
    
    // EditEventFragment.onCreateView ���� mDestroyBecauseOfSingleTaskRoot�� true�� �� ȣ��ȴ�
    public void setSubPageActionBar(String subPageTitle) {
    	mPreviousBackText.setText(getResources().getString(R.string.editevent_mainpage_actionbar_title));
        mSubpageTitleText.setText(subPageTitle);  
        
        mPreviousBackIcon.setVisibility(View.VISIBLE);	
        mPreviousBackText.setVisibility(View.VISIBLE);        
        mSubpageTitleText.setVisibility(View.VISIBLE); 
        
        mCancelText.setVisibility(View.GONE);
        mTitleText.setVisibility(View.GONE);
        mCompletionText.setVisibility(View.GONE);  		
    }
    
    public void setSubPageActionBar(String subPageTitle, 
    		long entranceAnimDuration, float entranceDistance,
			long exitAnimDuration, float exitDistance) {    	
    	
    	if (mTitleTextLeft == -1) {
    		mTitleTextLeft = mTitleText.getLeft();
    	}
    	
    	if (mPreviousBackIconRight == -1) {
    		BitmapDrawable previousIcon = (BitmapDrawable) this.getResources().getDrawable(R.drawable.ic_action_previous_item);
    		mPreviousBackIconRight = previousIcon.getBitmap().getWidth();
    	}
    	
        mCancelTextDisappearAlpha = ITEAnimationUtils.makeAlphaDecelerateAnimation(ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT,
        		1.0f, 0.0f, exitAnimDuration, exitDistance, 2);    	
        mCancelTextDisappearAlpha.setAnimationListener(mCancelTextDisappearAnimlistener);
    	
        mPreviousBackIconAppearAlpha = ITEAnimationUtils.makeAlphaAccelerateAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance, 2);        
        mPreviousBackIconAppearAlpha.setAnimationListener(mPreviousBackIconAppearAlphaAnimlistener);
    	        
        makeMainPageTitleExitVASetAnim((int)mTitleTextLeft, (int)mPreviousBackIconRight, entranceDistance, entranceAnimDuration);
        
        mSubpageTitleText.setText(subPageTitle);
        mSubTitleTextAppearAnimationSet = new AnimationSet(false);             
        float fromXDelta = getFromXDeltaOfSubPageTitleTextEntrance(subPageTitle);
        TranslateAnimation psudoSubpageTitleTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
     			 Animation.ABSOLUTE, fromXDelta,
      			 Animation.RELATIVE_TO_SELF, 0.0f,
      			 entranceAnimDuration, entranceDistance);
        mSubTitleTextAppearAnimationSet.addAnimation(psudoSubpageTitleTextTransAnim);
        AlphaAnimation psudoSubpageTitleTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance);
        mSubTitleTextAppearAnimationSet.addAnimation(psudoSubpageTitleTextAlphaAnim);       
        //mSubTitleTextAppearAnimationSet.setAnimationListener(mSubTitleTextAppearAnimlistener);
                
        mPreviousBackText.setText(getResources().getString(R.string.editevent_mainpage_actionbar_title));
          
        mPageMode = EDIT_EVENT_SUB_PAGE;
	}
    
    
    public void makeMainPageTitleExitVASetAnim(int fromX, int toX, float entranceDistance, long animDuration) {
    	mTitleTextDisappearVASet = new AnimatorSet();
    	
    	ValueAnimator transVa = ValueAnimator.ofInt(fromX, toX);      	
    	ITEAnimInterpolator i = new ITEAnimInterpolator(entranceDistance, transVa);  
    	transVa.setInterpolator(i);
    	transVa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
    		@Override 
    		public void onAnimationUpdate(ValueAnimator animation) {
    			    			
    			int xPos = (Integer) animation.getAnimatedValue();    
    			// setLeft�� ����ϸ� �ȵǴ� ������ ������ ����
    			// :This method is meant to be called by the layout system and should not generally be called otherwise, 
    			//  because the property may be changed at any time by the layout.
    			//mTitleText.setLeft(xPos);// setX�� �ٲ���...
    			mTitleText.setX(xPos);
    		}
    	});
    	
    	ValueAnimator alphaVa = ValueAnimator.ofInt(0, 1);       	
    	alphaVa.setInterpolator(new DecelerateInterpolator());
    	alphaVa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
    		@Override 
    		public void onAnimationUpdate(ValueAnimator animation) {
    			float fraction = animation.getAnimatedFraction();
    			int alpha = (int) (255 * fraction);
    			
				int color = Color.argb(alpha, 255, 0, 0);
    			mTitleText.setTextColor(color);
    		}
    	});
    	
    	mTitleTextDisappearVASet.setDuration(animDuration);
    	mTitleTextDisappearVASet.playTogether(transVa, alphaVa);
    }
    
    
    public ValueAnimator makeMainPageTitleExitVAnim(int fromX, int toX, float entranceDistance, long animDuration) {
    	//int width = mTitleText.getWidth();
    	//int left = mTitleText.getLeft();
    	//int x = (int) mTitleText.getX();
    	// :getX ���� getLeft ���� �����ϴ�	
    	
    	ValueAnimator anim = ValueAnimator.ofInt(fromX, toX);   
    	anim.setDuration(animDuration); 
    	ITEAnimInterpolator i = new ITEAnimInterpolator(entranceDistance, anim);  
    	anim.setInterpolator(i);
    	
    	anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
    		@Override 
    		public void onAnimationUpdate(ValueAnimator animation) {
    			
    			float fraction = animation.getAnimatedFraction();
    			int alpha = 0;
    			int color = 0;    			
    			alpha = (int) (255 * fraction);
				color = Color.argb(alpha, 255, 0, 0);
    			mTitleText.setTextColor(color);
    			
    			int xPos = (Integer) animation.getAnimatedValue();    
    			// setLeft�� ����ϸ� �ȵǴ� ������ ������ ����
    			// :This method is meant to be called by the layout system and should not generally be called otherwise, 
    			//  because the property may be changed at any time by the layout.
    			//mTitleText.setLeft(xPos);// setX�� �ٲ���...
    			mTitleText.setX(xPos);
    		}
    	});
    	
		anim.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animator) {					
			}

			@Override
			public void onAnimationStart(Animator animator) {		
				
			}

			@Override
			public void onAnimationCancel(Animator animator) {
			}

			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});		
    	return anim;
    }
       
    public AnimationSet makeMainPageTitleExitAnim(long exitAnimDuration, float exitDistance) {
    	AnimationSet mTitleTextDisappearAnimationSet = new AnimationSet(false);    	
    	float toXDelta = -(mTitleTextLeft - mPreviousBackIconRight); 
    	TranslateAnimation titleTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
   			 Animation.RELATIVE_TO_SELF, 0.0f,
   			 Animation.ABSOLUTE, toXDelta,
   			 exitAnimDuration, exitDistance);
        mTitleTextDisappearAnimationSet.addAnimation(titleTextTransAnim);
        AlphaAnimation titleTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 
       		 1.0f, 0.0f, exitAnimDuration, exitDistance); 
        mTitleTextDisappearAnimationSet.addAnimation(titleTextAlphaAnim);
        //mTitleTextDisappearAnimationSet.setAnimationListener(mTitleTextDisappearAnimlistener);
        return mTitleTextDisappearAnimationSet;
    }
    
    public float getFromXDeltaOfSubPageTitleTextEntrance(String subPageTitle) {
    	int titleTextCenterPositionX = getTitleTextCenterPositionX();
        int subPageTitleTextWidth = getSubPageTitleTextWidth(subPageTitle);//mSubpageTitleText.getWidth();
        int halfWidthOfSubPageTitleTextWidth = subPageTitleTextWidth / 2;
        int targetLeft = (titleTextCenterPositionX - halfWidthOfSubPageTitleTextWidth);
        float fromXDelta = getSubPageTitleEntranceAnimLeft(subPageTitleTextWidth) - targetLeft;
        return fromXDelta;
    }    
    
    public int getTitleTextCenterPositionX() {    	
    	// width ���� textWidth�� ���� �����ϴ�
    	int width = mTitleText.getWidth(); //Paint paint = mTitleText.getPaint(); int textWidth = (int) paint.measureText((String) mTitleText.getText());
    	int left = mTitleText.getLeft();
    	int centerPosition = left + (width / 2);
    	return centerPosition;
    }    
    
    public int getSubPageTitleEntranceAnimLeft(int subPageTextWidth) {
    	//int xright = mCompletionText.getRight();
    	int rightMargin = (int) mResources.getDimension(R.dimen.actionBarRightSideTextViewMarginRight);
    	//int rightMargin = (int) EUtil.dipToPixels(mContext, dipValue);
    	int right = mWindowWidth - rightMargin;
    	int left = right - subPageTextWidth;
    	return left;
    }    
    
    public int getSubPageTitleTextWidth(String subPageTitle) {
    	Paint paint = mSubpageTitleText.getPaint();
    	int textWidth = (int) paint.measureText(subPageTitle);
    	return textWidth;
    }
        
    public void startSubPageActionBarEnterAnim() {
    	
        mSubpageTitleText.setVisibility(View.VISIBLE);       
        
    	mCancelText.startAnimation(mCancelTextDisappearAlpha);
    	
    	mPreviousBackIcon.setVisibility(View.VISIBLE);	
		mPreviousBackIcon.startAnimation(mPreviousBackIconAppearAlpha); 
        
    	mTitleTextDisappearVASet.start();//mTitleTextDisappearVA.start();//mTitleText.startAnimation(mTitleTextDisappearAnimationSet);                   
        
        mSubpageTitleText.startAnimation(mSubTitleTextAppearAnimationSet);  
        
        mCompletionText.setVisibility(View.GONE);   
    }
    
    
    AnimationListener mCancelTextDisappearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mCancelText.setVisibility(View.GONE);
			
			//mPreviousBackIcon.setVisibility(View.VISIBLE);	
			//mPreviousBackIcon.startAnimation(mPreviousBackIconAppearAlpha); 
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	AnimationListener mPreviousBackIconAppearAlphaAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mTitleText.setVisibility(View.GONE);
			mTitleText.setTextColor(Color.BLACK);
			mTitleText.setX((int) mTitleTextLeft);			
			
			mPreviousBackText.setVisibility(View.VISIBLE);	
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	TranslateAnimation mPsudoPreviousBackTextDisappearTransAnim;
	TranslateAnimation mPreviousBackTextEntranceTransAnim;
	TranslateAnimation mSubpageOfSubpageTitleTextEntranceTransAnim;
	int mSubpageTitleTextLeft = -1;
	int mPreviousBackTextExitXAbsDelta;
	public void setSubPageOfSubPageActionBar(String subPageTitle, 
    		long entranceAnimDuration, float entranceDistance,
			long exitAnimDuration, float exitDistance) {
		
		mSubpageTitleTextLeft = mSubpageTitleText.getLeft();
		
    	mSubpageOfSubpageTitleText.setText(subPageTitle);
    	    	
    	// 1.mPreviousBackText[R.string.editevent_mainpage_actionbar_title]�� �ؽ�Ʈ��
    	//   mPsudoPreviousBackText�� �����ϰ� ���� anim�� �����Ѵ�    	
    	mPsudoPreviousBackText.setText(mPreviousBackText.getText());
    	mPsudoPreviousBackText.setVisibility(View.VISIBLE);
    	mPreviousBackTextExitXAbsDelta = mPreviousBackText.getLeft() + mPreviousBackText.getWidth();
    	float toXDelta = -(mPreviousBackTextExitXAbsDelta);
    	mPsudoPreviousBackTextDisappearTransAnim = ITEAnimationUtils.makeSlideAnimation(
    			Animation.RELATIVE_TO_SELF, 0.0f,
     			Animation.ABSOLUTE, toXDelta,
     			entranceAnimDuration, entranceDistance);    	
    	mPsudoPreviousBackTextDisappearTransAnim.setAnimationListener(mPsudoPreviousBackTextDisappearTransAnimListener);
    	
    	// 2.mPreviousBackText��  sub page title �ؽ�Ʈ�� �����ϰ� ���ڸ� <- title�� �̵���Ų��
    	float fromXDelta = mSubpageTitleText.getX() - mPreviousBackText.getX(); // ���� ���� ũ�Ⱑ �ٸ���!!!: �� ����� ����ϸ� �ȵȴ�
    	mPreviousBackText.setText(mSubpageTitleText.getText());
    	//mPreviousBackText.setX(mSubpageTitleText.getX());    	
    	mPreviousBackTextEntranceTransAnim = ITEAnimationUtils.makeSlideAnimation(
    			Animation.ABSOLUTE, fromXDelta,
     			Animation.RELATIVE_TO_SELF, 0.0f,
     			entranceAnimDuration, entranceDistance);
    	
    	// 3.mSubpageOfSubpageTitleText�� ���ڸ� <- right side  
    	fromXDelta = getFromXDeltaOfSubPageTitleTextOfSubPageEntrance(subPageTitle);
    	mSubpageOfSubpageTitleTextEntranceTransAnim = ITEAnimationUtils.makeSlideAnimation(
    			Animation.ABSOLUTE, fromXDelta,
     			Animation.RELATIVE_TO_SELF, 0.0f,
     			entranceAnimDuration, entranceDistance);
    	
    	mPageMode = EDIT_EVENT_SUB_PAGE_OF_SUB_PAGE;
    }
	
	public float getFromXDeltaOfSubPageTitleTextOfSubPageEntrance(String subPageTitle) {
    	int titleTextCenterPositionX = getSubPageTitleTextCenterPositionX();
        int subPageTitleTextWidth = getSubPageTitleTextWidthOfSubPage(subPageTitle);//mSubpageTitleText.getWidth();
        int halfWidthOfSubPageTitleTextWidth = subPageTitleTextWidth / 2;
        int targetLeft = (titleTextCenterPositionX - halfWidthOfSubPageTitleTextWidth);
        float fromXDelta = getSubPageTitleEntranceAnimLeft(subPageTitleTextWidth) - targetLeft;
        return fromXDelta;
    } 
	
	public int getSubPageTitleTextCenterPositionX() {    	
    	// width ���� textWidth�� ���� �����ϴ�
    	int width = mSubpageTitleText.getWidth(); //Paint paint = mTitleText.getPaint(); int textWidth = (int) paint.measureText((String) mTitleText.getText());
    	int left = mSubpageTitleText.getLeft();
    	int centerPosition = left + (width / 2);
    	return centerPosition;
    }    
	
	public int getSubPageTitleTextWidthOfSubPage(String subPageTitle) {
    	Paint paint = mSubpageOfSubpageTitleText.getPaint();
    	int textWidth = (int) paint.measureText(subPageTitle);
    	return textWidth;
    }
	
	public void startSubPageOfSubPageActionBarEnterAnim() {
		mSubpageTitleText.setVisibility(View.GONE);
		mSubpageOfSubpageTitleText.setVisibility(View.VISIBLE);
		
		mPsudoPreviousBackText.startAnimation(mPsudoPreviousBackTextDisappearTransAnim);
        
		mPreviousBackText.startAnimation(mPreviousBackTextEntranceTransAnim);
		
		mSubpageOfSubpageTitleText.startAnimation(mSubpageOfSubpageTitleTextEntranceTransAnim);
    }
	
	AnimationListener mPsudoPreviousBackTextDisappearTransAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoPreviousBackText.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	AnimationSet mSubpageTitleTextOfSubPageDisappearAnimationSet;
	AnimationSet mSubpageTitleTextAppearAnimationSet;
	AnimationSet mPreviousBackTextAppearAnimationSet;
	public void setSubPageActionBarReEntrance(
    		long entranceAnimDuration, float entranceDistance,
			long exitAnimDuration, float exitDistance) {
		
		// 1.mSubpageOfSubpageTitleText�� right side�� �����ؾ� �Ѵ�		
		mSubpageTitleTextOfSubPageDisappearAnimationSet = new AnimationSet(false);
    	float toXDelta = getToXDeltaOfSubPageTitleTextOfSubPageExit(mSubpageOfSubpageTitleText.getWidth());
        TranslateAnimation psudoSubpageTitleTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
      			 Animation.RELATIVE_TO_SELF, 0.0f,
       			 Animation.ABSOLUTE, toXDelta,
       			 exitAnimDuration, exitDistance);
        mSubpageTitleTextOfSubPageDisappearAnimationSet.addAnimation(psudoSubpageTitleTextTransAnim);
        AlphaAnimation psudoSubpageTitleTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT,
        		1.0f, 0.0f, exitAnimDuration, exitDistance); 
        mSubpageTitleTextOfSubPageDisappearAnimationSet.addAnimation(psudoSubpageTitleTextAlphaAnim);       
        mSubpageTitleTextOfSubPageDisappearAnimationSet.setAnimationListener(mSubPageTitleTextOfSubPageDisappearAnimlistener);
    	
       
        // 2.mSubpageTitleText�� ���ڸ��� ���ƿ;� �Ѵ�
 		//   :mPreviousBackText�� �ؽ�Ʈ�� mSubpageTitleText�� ������ �ʿ䰡 �ִ°�?
 		//    ���� mSubpageTitleText�� gone �����̱� �ϳ� �ؽ�Ʈ�� ��� ���� ���̴� 		    
        mSubpageTitleText.setText(mPreviousBackText.getText());        
        mSubpageTitleTextAppearAnimationSet = new AnimationSet(false);       
        float fromXDelta = -(mSubpageTitleTextLeft - mPreviousBackTextLeft); //mTitleTextLeft�� �ƴ� mSubpageTitleText�� �Ǿ�� �Ѵ�!!!
        TranslateAnimation subPageTitleTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
     			 Animation.ABSOLUTE, fromXDelta,
      			 Animation.RELATIVE_TO_SELF, 0.0f,
      			 entranceAnimDuration, entranceDistance);
        mSubpageTitleTextAppearAnimationSet.addAnimation(subPageTitleTextTransAnim);
        AlphaAnimation subPageTitleTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance);
        mSubpageTitleTextAppearAnimationSet.addAnimation(subPageTitleTextAlphaAnim);
        mSubpageTitleTextAppearAnimationSet.setAnimationListener(mSubPageTitleTextAppearAnimlistener);
        
        // 3.mPreviousBackText�� ���ڸ��� ���ƿ;� �Ѵ�
      	//   :�̺�Ʈ �߰���� �ؽ�Ʈ�� �����ؾ� �Ѵ�    
        mPreviousBackText.setText(getResources().getString(R.string.editevent_mainpage_actionbar_title));                
        mPreviousBackTextAppearAnimationSet = new AnimationSet(false);       
        fromXDelta = -(mPreviousBackTextExitXAbsDelta); 
        TranslateAnimation previousBackTextTransAnim = ITEAnimationUtils.makeSlideAnimation(
     			 Animation.ABSOLUTE, fromXDelta,
      			 Animation.RELATIVE_TO_SELF, 0.0f,
      			 entranceAnimDuration, entranceDistance);
        mPreviousBackTextAppearAnimationSet.addAnimation(previousBackTextTransAnim);
        AlphaAnimation previousBackTextAlphaAnim = ITEAnimationUtils.makeAlphaAnimation(ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 
        		0.0f, 1.0f, entranceAnimDuration, entranceDistance);
        mPreviousBackTextAppearAnimationSet.addAnimation(previousBackTextAlphaAnim);
        //mPreviousBackTextAppearAnimationSet.setAnimationListener();
        
        mPageMode = EDIT_EVENT_SUB_PAGE;
	}
	
	public void startSubPageActionBarReEntrance() {
		mSubpageOfSubpageTitleText.startAnimation(mSubpageTitleTextOfSubPageDisappearAnimationSet);
		
		mSubpageTitleText.setVisibility(View.VISIBLE);
		mSubpageTitleText.startAnimation(mSubpageTitleTextAppearAnimationSet);
		
		mPreviousBackText.setVisibility(View.VISIBLE);
		mPreviousBackText.startAnimation(mPreviousBackTextAppearAnimationSet);
	}
	
	public float getToXDeltaOfSubPageTitleTextOfSubPageExit(int subPageTextWidth) {
    	int rightMargin = (int) mResources.getDimension(R.dimen.actionBarRightSideTextViewMarginRight);    	
    	int right = mWindowWidth - rightMargin;
    	int exitToLeft = right - subPageTextWidth;
    	float toXDelta =  exitToLeft - mSubpageOfSubpageTitleText.getLeft();
    	return toXDelta;
    }
	
	
	AnimationListener mSubPageTitleTextOfSubPageDisappearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mSubpageOfSubpageTitleText.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	
	AnimationListener mSubPageTitleTextAppearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {		
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	/*
    AnimationListener mTitleTextDisappearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mTitleText.setVisibility(View.GONE);
			
			mPreviousBackText.setVisibility(View.VISIBLE);		
			mPreviousBackText.startAnimation(mPreviousBackTextAppearAlpha);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	*/
    
		
	/*
	AnimationListener mSubTitleTextAppearAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {						
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			//mSubpageTitleText
			//RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)mSubpageTitleText.getLayoutParams();
			//layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			//layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			//mSubpageTitleText.setLayoutParams(layoutParams);		
		}

		@Override
		public void onAnimationRepeat(Animation animation) {				
		}		
	};
	*/
	
	
	
}
