package com.intheeast.selectcalendars;

import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.CommonRelativeLayoutUpperActionBar;
import com.intheeast.acalendar.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

@SuppressLint("ValidFragment")
public class SelectCalendarsViewActionBarFragment extends Fragment {

	private static final String TAG = "SelectCalendarsViewUpperActionBarFragment";	
	private static final boolean INFO = true;
	
	public static final int SELECT_MAINVIEW_EDIT = 1;
	public static final int SELECT_MAINVIEW_DONE = 2;
	public static final int SELECT_SEPERATEVIEW_CANCEL = 3;
	public static final int SELECT_SEPERATEVIEW_DONE = 4;
	public static final int SELECT_ENTIREVIEW_DONE = 5;
	
	Activity mActivity;
	Context mContext;
	
	CalendarController mController;
	CommonRelativeLayoutUpperActionBar mFrameLayout;
	
	SelectCalendarsFragment mMainFragment;
	
	TextView mEdit;
	TextView mEditDone;
	TextView mCancel;
	TextView mMainTitle;
	TextView mEditTitle;
	TextView mDone;
	
	int mWindowWidth;
    int mActionBarHeight;
    
    int mFragmentViewState;
    
    
    OnTouchListener mDoneTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			doneTouchAnimation(event);		
			//return true; //mCanelClickListener�� ȣ����� ����
			return false;
		}    	
    };
    
    public void doneTouchAnimation(MotionEvent event) {
    	int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:						
			AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
			downAlpha.setDuration(0); 
			downAlpha.setFillAfter(true);		
			mDone.startAnimation(downAlpha);			
			break;
		case MotionEvent.ACTION_UP:			
			AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
			upAlpha.setDuration(0); 
			upAlpha.setFillAfter(false); 			
			mDone.startAnimation(upAlpha);				
			break;			
		default:
			break;
		}				
    }
    
    
    OnTouchListener mCanelTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			cancelTouchAnimation(event);		
			//return true; //mCanelClickListener�� ȣ����� ����
			return false;
		}    	
    };
    
    public void cancelTouchAnimation(MotionEvent event) {
    	int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:						
			AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
			downAlpha.setDuration(0); 
			downAlpha.setFillAfter(true);		
			mCancel.startAnimation(downAlpha);			
			break;
		case MotionEvent.ACTION_UP:			
			AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
			upAlpha.setDuration(0); 
			upAlpha.setFillAfter(false); 			
			mCancel.startAnimation(upAlpha);				
			break;			
		default:
			break;
		}				
    }
    
    OnClickListener mCanelClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mMainFragment.notifyedMsgByActionBar(SELECT_SEPERATEVIEW_CANCEL);				
		}    	
    };
    
    OnClickListener mDoneClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {			
			mMainFragment.notifyedMsgByActionBar(SELECT_ENTIREVIEW_DONE);
				
		}    	
    };
    
    public SelectCalendarsViewActionBarFragment(int viewState, SelectCalendarsFragment mainFragment) {
    	mFragmentViewState = viewState;
    	mMainFragment = mainFragment;
    	mMainFragment.setActionBarFragment(this);
    }
	
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		if (INFO) Log.i(TAG, "onAttach");
		mActivity = activity;
		mController = CalendarController.getInstance(mActivity);
		mContext = activity.getApplicationContext();
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
		
		mFrameLayout = (CommonRelativeLayoutUpperActionBar) inflater.inflate(R.layout.selectcalendars_actionbar, null);
	        	
		mEdit = (TextView) mFrameLayout.findViewById(R.id.select_calendarview_actionbar_edit_textview);
		mEditDone = (TextView) mFrameLayout.findViewById(R.id.select_calendarview_actionbar_edit_done_textview);
		mCancel = (TextView) mFrameLayout.findViewById(R.id.select_calendarview_actionbar_cancel_textview);
		mMainTitle = (TextView) mFrameLayout.findViewById(R.id.select_calendarview_actionbar_main_title_textview);
		mEditTitle = (TextView) mFrameLayout.findViewById(R.id.select_calendarview_actionbar_edit_title_textview);
		mDone = (TextView) mFrameLayout.findViewById(R.id.select_calendarview_actionbar_done_textview);
		mDone.setOnTouchListener(mDoneTouchListener);
		mDone.setOnClickListener(mDoneClickListener);
		
		mCancel.setOnTouchListener(mCanelTouchListener);
		mCancel.setOnClickListener(mCanelClickListener);
		// SelectCalendarsViewUpperActionBarFragment���� �� ���� ���°� �ִ�
		// 1.Main View
		// 2.Entire Calendars Edit View
		// 3.Seperate Calendar Edit View
		// :2�� 3�� �������� �����ΰ�? ���� ���̰� ���� �� ������...
		initActionBarLayout(mFragmentViewState);	    
		
        return mFrameLayout;
	}		
	
	
	public void initActionBarLayout(int viewState) {
		if (viewState == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
			mEdit.setVisibility(View.VISIBLE);
			mEditDone.setVisibility(View.GONE);
			mCancel.setVisibility(View.GONE);
			mMainTitle.setVisibility(View.VISIBLE);
			mEditTitle.setVisibility(View.GONE);
			mDone.setVisibility(View.VISIBLE);
		}
		else if (viewState == SelectCalendarsFragment.ENTIRE_EDIT_VIEW_FRAGMENT_STATE) {
			mEdit.setVisibility(View.GONE);
			mEditDone.setVisibility(View.VISIBLE);
			mCancel.setVisibility(View.GONE);
			mMainTitle.setVisibility(View.GONE);
			mEditTitle.setVisibility(View.VISIBLE);
			mDone.setVisibility(View.GONE);
		}
		else if (viewState == SelectCalendarsFragment.SEPERATE_EDIT_VIEW_FRAGMNET_STATE) {
			mEdit.setVisibility(View.GONE);
			mEditDone.setVisibility(View.GONE);
			mCancel.setVisibility(View.VISIBLE);
			mMainTitle.setVisibility(View.GONE);
			mEditTitle.setVisibility(View.VISIBLE);
			mDone.setVisibility(View.VISIBLE);
		}
	}
	
	AlphaAnimation mEditAppearAlphaAnimation;
	long mEditAppearAlphaAnimationDuration;
	AnimationListener mEditAppearAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			int textColor = Color.argb(255, 255, 0, 0);	
			mEdit.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AlphaAnimation mEditDisappearAlphaAnimation;
	long mEditDisappearAlphaAnimationDuration;
	AnimationListener mEditDisappearAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mEdit.setVisibility(View.GONE);			
			mCancel.startAnimation(mCancelAppearAlphaAnimation);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AlphaAnimation mCancelDisappearAlphaAnimation;
	long mCancelDisappearAlphaAnimationDuration;
	AnimationListener mCancelDisappearAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mCancel.setVisibility(View.GONE);			
			mEdit.startAnimation(mEditAppearAlphaAnimation);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}		
	};	
	
	AlphaAnimation mCancelAppearAlphaAnimation;
	long mCancelAppearAlphaAnimationDuration;
	AnimationListener mCancelAppearAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			int textColor = Color.argb(255, 255, 0, 0);	
			mCancel.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AnimationListener mMainTitleExitAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mMainTitle.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}		
	};	
	
	AnimationListener mMainTitleEntranceAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
			int textColor = Color.argb(255, 0, 0, 0);			
			mMainTitle.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mEditTitleTextEntranceAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			int textColor = Color.argb(255, 0, 0, 0);			
			mEditTitle.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
					
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mEditTitleExitAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mEditTitle.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub			
		}		
	};	
	
	AnimationSet mMainTitleEnterAnimationSet;
	AnimationSet mMainTitleExitAnimationSet;
	AnimationSet mEditTitleEnterAnimationSet;
	AnimationSet mEditTitleExitAnimationSet;
	public void setActionBarLayout(int fromView, int toView, 
			long enterAnimDuration, float enterDistance,
			long exitAnimDuration, float exitDistance) {
		
		if (fromView == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
			if (toView == SelectCalendarsFragment.SEPERATE_EDIT_VIEW_FRAGMNET_STATE) {
				// 1.mEdit�� ������ �������
				// 2.mCancel�� ������ ��Ÿ����
				// 3.mMainTitle�� self -> left�� �̵��ϸ鼭 ������� �Ѵ�
				// 4.mEditTitle�� right -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�
				// 5.mDone�� ������ red -> ȸ������ ���ؾ� �Ѵ�				
				makeMainPageExitAnim(exitAnimDuration, exitDistance);
				
				makeSubPageEntranceAnim(exitAnimDuration, enterAnimDuration, enterDistance);				
			}
			else if (toView == SelectCalendarsFragment.ENTIRE_EDIT_VIEW_FRAGMENT_STATE) {
				
				mEdit.setVisibility(View.GONE);
				mEditDone.setVisibility(View.VISIBLE);
				mCancel.setVisibility(View.GONE);
				mMainTitle.setVisibility(View.GONE);
				mEditTitle.setVisibility(View.VISIBLE);
				mDone.setVisibility(View.GONE);
			}	
			
		}
		else if (fromView == SelectCalendarsFragment.SEPERATE_EDIT_VIEW_FRAGMNET_STATE) {
			if (toView == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				// 1.mCancel�� ������ �������
				// 2.mEdit�� ������ ��Ÿ����
				// 3.mMainTitle�� left -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�
				// 4.mEditTitle�� self -> right�� �̵��ϸ鼭 ������� �Ѵ�
				// 5.mDone�� ������ ȸ�� -> red���� ���ؾ� �Ѵ�
				makeSubPageExitAnim(exitAnimDuration, exitDistance);
				
				makeMainPageReEntranceAnim(exitAnimDuration, enterAnimDuration, enterDistance);				
			} else {
				// �̷� ���� �߻��ؼ��� �� ���� ����
			}			
		}
		else if (fromView == SelectCalendarsFragment.ENTIRE_EDIT_VIEW_FRAGMENT_STATE) {
			if (toView == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				mEdit.setVisibility(View.VISIBLE);
				mEditDone.setVisibility(View.GONE);
				mCancel.setVisibility(View.GONE);
				mMainTitle.setVisibility(View.VISIBLE);
				mEditTitle.setVisibility(View.GONE);
				mDone.setVisibility(View.VISIBLE);
			} else {
				// �̷� ���� �߻��ؼ��� �� ���� ����
			}			
		}
		
	}
	
	public void makeMainPageExitAnim(long exitAnimDuration, float exitDistance) {
		// 1.mEdit�� ������ �������
		mEditDisappearAlphaAnimationDuration = exitAnimDuration / 2;
		mEditDisappearAlphaAnimation = new AlphaAnimation(1.0f, 0.2f);
		mEditDisappearAlphaAnimation.setDuration(mEditDisappearAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mEditDisappearAlphaAnimation.setAnimationListener(mEditDisappearAlphaAnimationListener);	
		
		// 2.mMainTitle�� self -> left�� �̵��ϸ鼭 ������� �Ѵ�
		mMainTitleExitAnimationSet = new AnimationSet(false);				
		float toXDelta = mEdit.getRight() - mMainTitle.getLeft();
        TranslateAnimation mainTitleExitTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		Animation.RELATIVE_TO_SELF, 0.0f,
        		Animation.ABSOLUTE, toXDelta,
        		exitAnimDuration, exitDistance);
        mMainTitleExitAnimationSet.addAnimation(mainTitleExitTransAnim);
        
        AlphaAnimation mainTitleExitAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f,
        		exitAnimDuration, exitDistance);
        mMainTitleExitAnimationSet.addAnimation(mainTitleExitAlpahAnim);
        mMainTitleExitAnimationSet.setAnimationListener(mMainTitleExitAnimationSetListener);
        
        // 3.mDone�� ������ red -> ȸ������ ���ؾ� �Ѵ�
        // ...
	}
	
	public void makeMainPageReEntranceAnim(long exitAnimDuration, long entranceAnimDuration, float entranceDistance) {
		// 1.mEdit�� ������ ��Ÿ����
		mEdit.setVisibility(View.VISIBLE);  
		int textColor = Color.argb(0, 255, 255, 255);
		mEdit.setTextColor(textColor);
		mEditAppearAlphaAnimationDuration = exitAnimDuration / 2;
		mEditAppearAlphaAnimation = new AlphaAnimation(0.2f, 1.0f);
		mEditAppearAlphaAnimation.setDuration(mEditAppearAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mEditAppearAlphaAnimation.setAnimationListener(mEditAppearAlphaAnimationListener);
		
		// 2.mMainTitle�� left -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�		
		mMainTitle.setVisibility(View.VISIBLE);		
		mMainTitle.setTextColor(textColor);
		int mainTitleLeft = mMainTitle.getLeft();
        int cancelRight = mCancel.getRight();
		int fromXType = Animation.ABSOLUTE;
		float fromXDelta = cancelRight - mainTitleLeft;
		mMainTitleEnterAnimationSet = new AnimationSet(false);
        TranslateAnimation mainTitleEnterTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		fromXType, fromXDelta,
        		Animation.RELATIVE_TO_SELF, 0,
        		entranceAnimDuration, entranceDistance);
        mMainTitleEnterAnimationSet.addAnimation(mainTitleEnterTransAnim);
        
        AlphaAnimation mainTitleEnterAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
        		entranceAnimDuration, entranceDistance);
        mMainTitleEnterAnimationSet.addAnimation(mainTitleEnterAlpahAnim);
        mMainTitleEnterAnimationSet.setAnimationListener(mMainTitleEntranceAnimationSetListener);
        
		// 3.mDone�� ������ ȸ�� -> red���� ���ؾ� �Ѵ�
        // ...
	}
	
	public void makeSubPageEntranceAnim(long exitAnimDuration, long entranceAnimDuration, float entranceDistance) {
		// 1.mCancel�� ������ ��Ÿ����
		mCancel.setVisibility(View.VISIBLE);  
		int textColor = Color.argb(0, 255, 255, 255);
		mCancel.setTextColor(textColor);
		mCancelAppearAlphaAnimationDuration = exitAnimDuration / 2;
		mCancelAppearAlphaAnimation = new AlphaAnimation(0.2f, 1.0f);
		mCancelAppearAlphaAnimation.setDuration(mCancelAppearAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mCancelAppearAlphaAnimation.setAnimationListener(mCancelAppearAlphaAnimationListener);	
		
		// 2.mEditTitle�� right -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�
		mEditTitle.setVisibility(View.VISIBLE);        
        mEditTitle.setTextColor(textColor);
        mEditTitleEnterAnimationSet = new AnimationSet(false);
        // mEditTitle�� ���� ���� ���� �Ƹ��� mEditTitle.getRight() ���� 0�ΰ� ����...
        int doneTextLeft = mDone.getLeft();
        int editTitleTextRight = mEditTitle.getRight();
        int fromXType;
        float fromXDelta;
        if (editTitleTextRight == 0) {
        	fromXType = Animation.RELATIVE_TO_SELF;
        	fromXDelta = 1;	        	
        }
        else {
        	fromXType = Animation.ABSOLUTE;
        	fromXDelta = doneTextLeft - editTitleTextRight;
        }
        TranslateAnimation editTitleEnterTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		fromXType, fromXDelta,
        		Animation.RELATIVE_TO_SELF, 0,
        		entranceAnimDuration, entranceDistance);
        mEditTitleEnterAnimationSet.addAnimation(editTitleEnterTransAnim);
        
        AlphaAnimation editTitleEnterAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
        		entranceAnimDuration, entranceDistance);
        mEditTitleEnterAnimationSet.addAnimation(editTitleEnterAlpahAnim);
        
        mEditTitleEnterAnimationSet.setAnimationListener(mEditTitleTextEntranceAnimationSetListener);
	}
	
	public void makeSubPageExitAnim(long exitAnimDuration, float exitDistance) {
		// 1.mCancel�� ������ �������
		mCancelDisappearAlphaAnimationDuration = exitAnimDuration / 2;
		mCancelDisappearAlphaAnimation = new AlphaAnimation(1.0f, 0.2f);
		mCancelDisappearAlphaAnimation.setDuration(mCancelDisappearAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mCancelDisappearAlphaAnimation.setAnimationListener(mCancelDisappearAlphaAnimationListener);
		
		// 2.mEditTitle�� self -> right�� �̵��ϸ鼭 ������� �Ѵ�
		mEditAppearAlphaAnimationDuration = exitAnimDuration / 2;
		mEditAppearAlphaAnimation = new AlphaAnimation(0.2f, 1.0f);
		mEditAppearAlphaAnimation.setDuration(mEditAppearAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mEditAppearAlphaAnimation.setAnimationListener(mEditAppearAlphaAnimationListener);		
						
        mEditTitleExitAnimationSet = new AnimationSet(false);
        float toXDelta = mDone.getLeft() - mEditTitle.getRight();
        TranslateAnimation editTitleExitTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		Animation.RELATIVE_TO_SELF, 0, 
        		Animation.ABSOLUTE, toXDelta,
        		exitAnimDuration, exitDistance);
        mEditTitleExitAnimationSet.addAnimation(editTitleExitTransAnim);
        
        AlphaAnimation editTitleExitAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f,
        		exitAnimDuration, exitDistance);
        mEditTitleExitAnimationSet.addAnimation(editTitleExitAlpahAnim);
        mEditTitleExitAnimationSet.setAnimationListener(mEditTitleExitAnimationSetListener);		
	}
	
	int mFromView;
	int mToView;
	
	// fragment���� page switching�� �� �� ȣ��ȴ�
	public void notifyFragmentViewSwitchState(int fromView, int toView, 
			long enterAnimDuration, float enterDistance,
			long exitAnimDuration, float exitDistance) {
		mFromView = fromView;
		mToView = toView;
		setActionBarLayout(fromView, toView, enterAnimDuration, enterDistance,
				exitAnimDuration, exitDistance);
		
		mFragmentViewState = toView;
		
		switch(mFragmentViewState) {
		case SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE:
			break;
		case SelectCalendarsFragment.ENTIRE_EDIT_VIEW_FRAGMENT_STATE:
			break;
		case SelectCalendarsFragment.SEPERATE_EDIT_VIEW_FRAGMNET_STATE:
			break;
		}
	}
	
	public void startActionBarAnim() {
		if (mFromView == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
			if (mToView == SelectCalendarsFragment.ENTIRE_EDIT_VIEW_FRAGMENT_STATE) {				
				
			}
			else if (mToView == SelectCalendarsFragment.SEPERATE_EDIT_VIEW_FRAGMNET_STATE) {
				mMainTitle.startAnimation(mMainTitleExitAnimationSet);
				mEditTitle.startAnimation(mEditTitleEnterAnimationSet);
				mEdit.startAnimation(mEditDisappearAlphaAnimation);
			}
		}
		else if (mFromView == SelectCalendarsFragment.SEPERATE_EDIT_VIEW_FRAGMNET_STATE) {
			if (mToView == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				mEditTitle.startAnimation(mEditTitleExitAnimationSet);
				mMainTitle.startAnimation(mMainTitleEnterAnimationSet);
				mCancel.startAnimation(mCancelDisappearAlphaAnimation);
			} else {
				// �̷� ���� �߻��ؼ��� �� ���� ����
			}			
		}
		else if (mFromView == SelectCalendarsFragment.ENTIRE_EDIT_VIEW_FRAGMENT_STATE) {
			if (mToView == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				
			} else {
				// �̷� ���� �߻��ؼ��� �� ���� ����
			}			
		}
		
	}
	
	
}
