package com.intheeast.settings;

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
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;

@SuppressLint("ValidFragment")
public class SettingsActionBarFragment extends Fragment {
	
	private static final String TAG = "SettingsActionBarFragment";
	private static final boolean INFO = true;
	
	public static final int SELECTED_PREVIOUS_ACTION = 1;

	Activity mActivity;
	Context mContext;
	
	CommonRelativeLayoutUpperActionBar mFrameLayout;
	ImageView mPreviousIcon;
	TextView mPreviousBackText;
	TextView mMainTitleText;
	TextView mSubPageTitleText;
	TextView mSubPageTitleTextOfSubPage;
	TextView mDoneText;
	
	CalendarController mController;
	
	SettingsFragment mMainFragment;
	
	
	int mWindowWidth;
    int mActionBarHeight;
    
    int mFragmentViewState;
    
    OnTouchListener mPrvActionTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			prvActionAnimation(event);		
			//return true; //mCanelClickListener�� ȣ����� ����
			return false;
		}    	
    };
    
    public void prvActionAnimation(MotionEvent event) {
    	int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:						
			AlphaAnimation iconDownAlpha = new AlphaAnimation(1.0f, 0.2F); 
			iconDownAlpha.setDuration(0); 
			iconDownAlpha.setFillAfter(true);		
			mPreviousIcon.startAnimation(iconDownAlpha);
			
			AlphaAnimation iconTextDownAlpha = new AlphaAnimation(1.0f, 0.2F); 
			iconTextDownAlpha.setDuration(0); 
			iconTextDownAlpha.setFillAfter(true);		
			mPreviousBackText.startAnimation(iconDownAlpha);
			break;
		case MotionEvent.ACTION_UP:			
			AlphaAnimation iconUpAlpha = new AlphaAnimation(0.2f, 1.0F); 
			iconUpAlpha.setDuration(0); 
			iconUpAlpha.setFillAfter(false); 			
			mPreviousIcon.startAnimation(iconUpAlpha);	
			
			AlphaAnimation iconTextUpAlpha = new AlphaAnimation(0.2f, 1.0F); 
			iconTextUpAlpha.setDuration(0); 
			iconTextUpAlpha.setFillAfter(false); 			
			mPreviousBackText.startAnimation(iconTextUpAlpha);
			break;			
		default:
			break;
		}				
    }
    
    OnClickListener mPrvActionClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// ������ Main Fragment ViewState�� ���� Ȯ���ϰ� MainFragment���� Msg�� �����ؾ� �Ѵ�
			mMainFragment.notifyedMsgByActionBar(SELECTED_PREVIOUS_ACTION);
			
		}    	
    };
    
    
    
    
	public SettingsActionBarFragment(int viewState, SettingsFragment mainFragment) {
		mFragmentViewState = viewState;
		mMainFragment = mainFragment;
		
    	mMainFragment.setActionBarFragment(this);
	}
	
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		if (INFO) Log.i(TAG, "onAttach");
		mActivity = activity;
		mContext = activity.getApplicationContext();
		
		mController = CalendarController.getInstance(mActivity);
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
		
		
		
		mFrameLayout = (CommonRelativeLayoutUpperActionBar) inflater.inflate(R.layout.settings_actionbar, null);
		
		mPreviousIcon = (ImageView)mFrameLayout.findViewById(R.id.settings_actionbar_previous_back);
		mPreviousIcon.setOnTouchListener(mPrvActionTouchListener);
		mPreviousIcon.setOnClickListener(mPrvActionClickListener);
		
		mPreviousBackText = (TextView)mFrameLayout.findViewById(R.id.settings_actionbar_previous_back_text);
		mPreviousBackText.setOnTouchListener(mPrvActionTouchListener);
		mPreviousBackText.setOnClickListener(mPrvActionClickListener);
		
		mMainTitleText = (TextView)mFrameLayout.findViewById(R.id.settings_actionbar_main_title_textview);
		
		mSubPageTitleText = (TextView)mFrameLayout.findViewById(R.id.settings_actionbar_sub_title_textview);
				
		mSubPageTitleTextOfSubPage = (TextView)mFrameLayout.findViewById(R.id.settings_actionbar_sub_of_sub_title_textview);
		
		mDoneText = (TextView)mFrameLayout.findViewById(R.id.settings_actionbar_done_textview);
		
		return mFrameLayout;
	}
	
	int mFromView;
	int mToView;
	
	public void notifyFragmentViewSwitchState(int fromView, int toView, 
			long enterAnimDuration, float enterDistance,
			long exitAnimDuration, float exitDistance, int extras) {
		mFromView = fromView;
		mToView = toView;
		setActionBarLayout(fromView, toView, enterAnimDuration, enterDistance,
				exitAnimDuration, exitDistance, extras);
		
		mFragmentViewState = toView;
		
		switch(mFragmentViewState) {
		case SettingsFragment.MAIN_VIEW_FRAGMENT_STATE:
			break;
		
		case SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE:
			break;
		}
	}
	
	
	AlphaAnimation mDoneTextAlphaAnimation;
	long mDoneTextAlphaAnimationDuration;
	AnimationListener mDoneTextDisappearAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			mPreviousIcon.setVisibility(View.VISIBLE);
			mPreviousBackText.setVisibility(View.VISIBLE);
			
			mPreviousIcon.startAnimation(mPreviousIconAlphaAnimation);
			mPreviousBackText.startAnimation(mPreviousBackTextAlphaAnimation);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mDoneText.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AlphaAnimation mPreviousIconAlphaAnimation;
	long mPreviousIconAlphaAnimationDuration;
	AnimationListener mPreviousIconExitAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPreviousIcon.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AlphaAnimation mPreviousBackTextAlphaAnimation;
	long mPreviousBackTextAlphaAnimationDuration;
	AnimationListener mPreviousBackTextExitAlphaAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			mDoneText.setVisibility(View.VISIBLE);			
			mDoneText.startAnimation(mDoneTextAlphaAnimation);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mPreviousBackText.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};		
	
	
	AnimationSet mMainTitleTextExitAnimationSet;
	AnimationListener mMainTitleTextEntranceAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
			int textColor = Color.argb(255, 0, 0, 0);			
			mMainTitleText.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mMainTitleTextExitAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mMainTitleText.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationSet mSubPageTitleTextAnimationSet;
	AnimationListener mSubpageTitleTextEntranceAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			int textColor = Color.argb(255, 0, 0, 0);			
			mSubPageTitleText.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
					
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mSubpageTitleTextExitAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mSubPageTitleText.setVisibility(View.GONE);				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationSet mSubPageTitleTextOfSubPageAnimationSet;
	AnimationListener mSubpageTitleTextOfSubPageEntranceAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			int textColor = Color.argb(255, 0, 0, 0);			
			mSubPageTitleTextOfSubPage.setTextColor(textColor);
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
					
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mSubpageTitleTextOfSubPageExitAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			mSubPageTitleTextOfSubPage.setVisibility(View.GONE);				
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	public void setActionBarLayout(int fromView, int toView, 
			long entranceAnimDuration, float entranceDistance,
			long exitAnimDuration, float exitDistance, int extras) {	
		
		if (fromView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {				
				makeMainpageExitAnim(exitAnimDuration, exitDistance);
				
		        makeSubpageEntranceAnim("���� �ð��� ���", entranceAnimDuration, entranceDistance);       
			}
			else if (toView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				makeMainpageExitAnim(exitAnimDuration, exitDistance);
				
				makeSubpageEntranceAnim("�⺻ �˸� �ð�", entranceAnimDuration, entranceDistance); 
			}
			else if (toView == SettingsFragment.DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				makeMainpageExitAnim(exitAnimDuration, exitDistance);
				
				makeSubpageEntranceAnim("�⺻ Ķ����", entranceAnimDuration, entranceDistance); 
			}
			else if (toView == SettingsFragment.WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				makeMainpageExitAnim(exitAnimDuration, exitDistance);
				
				makeSubpageEntranceAnim("���� ����", entranceAnimDuration, entranceDistance); 
			}
		}
		else if (fromView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {									        
				makeMainPageReEntranceAnim(entranceAnimDuration, entranceDistance);		
				
		        makeSubpageExitAnim(exitAnimDuration, exitDistance);		        
			}
			else if (toView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
				makeSuppageExitAnimByChild(exitAnimDuration, exitDistance);
				
				makeSubpageOfSubpageEntranceAnim("�ð���", entranceAnimDuration, entranceDistance);				
			}
		}
		else if (fromView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				makeSubpageOfSubpageExitAnim(exitAnimDuration, exitDistance);
				
				makeSubPageEntranceAnimByChild(entranceAnimDuration, entranceDistance);
			}
		}
		else if (fromView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				makeMainPageReEntranceAnim(entranceAnimDuration, entranceDistance);
				
				makeSubpageExitAnim(exitAnimDuration, exitDistance);
			}
			else if (toView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
				makeSuppageExitAnimByChild(exitAnimDuration, exitDistance);
				String subPageTitleOfSubPage = "what???";
				switch(extras) {
				case AlertTimeOverrideSubView.BIRTHDAY_ALERTTIME_SUBPAGE:
					subPageTitleOfSubPage = "����";
					break;
				case AlertTimeOverrideSubView.EVENT_ALERTTIME_SUBPAGE:
					subPageTitleOfSubPage = "�̺�Ʈ";
					break;
				case AlertTimeOverrideSubView.ALLDAYEVEENT_ALERTTIME_SUBPAGE:
					subPageTitleOfSubPage = "�Ϸ� ���� �̺�Ʈ";
					break;
				}
				
				makeSubpageOfSubpageEntranceAnim(subPageTitleOfSubPage, entranceAnimDuration, entranceDistance);
			}
		}
		else if (fromView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				makeSubpageOfSubpageExitAnim(exitAnimDuration, exitDistance);
				
				makeSubPageEntranceAnimByChild(entranceAnimDuration, entranceDistance);				
			}
		}
		else if (fromView == SettingsFragment.WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {									        
				makeMainPageReEntranceAnim(entranceAnimDuration, entranceDistance);		
				
		        makeSubpageExitAnim(exitAnimDuration, exitDistance);		        
			}			
		}
		else if (fromView == SettingsFragment.DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (toView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {									        
				makeMainPageReEntranceAnim(entranceAnimDuration, entranceDistance);		
				
		        makeSubpageExitAnim(exitAnimDuration, exitDistance);		        
			}			
		}
		
	}
	
	public void makeMainPageReEntranceAnim(long entranceAnimDuration, float entranceDistance) {
		// 1.mDoneText�� ������ ��Ÿ����		
		// 2.mMainTitle�� left -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�						
		mDoneTextAlphaAnimationDuration = entranceAnimDuration;
		mDoneTextAlphaAnimation = new AlphaAnimation(0.2f, 1.0f);
		mDoneTextAlphaAnimation.setDuration(mDoneTextAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�	
				
		mMainTitleText.setVisibility(View.VISIBLE);
		int textColor = Color.argb(0, 255, 255, 255);
		mMainTitleText.setTextColor(textColor);
        mMainTitleTextExitAnimationSet = new AnimationSet(false);
        
        int fromXType;
        float fromXDelta;
        int prvBackTextRight = mPreviousBackText.getRight();
        int mainTitleTextLeft = mMainTitleText.getLeft();		        
        if (mainTitleTextLeft == 0) {
        	fromXType = Animation.RELATIVE_TO_SELF;
        	fromXDelta = -1;	        	
        }
        else {
        	fromXType = Animation.ABSOLUTE;
        	// fromXDelta�� (-)���� ���;߸� ���ʿ��� ���������� �̵��Ѵ�
        	fromXDelta = prvBackTextRight - mainTitleTextLeft;
        }
        TranslateAnimation mainTitleTextEnterTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		fromXType, fromXDelta,
        		Animation.RELATIVE_TO_SELF, 0,
        		entranceAnimDuration, entranceDistance);
        mMainTitleTextExitAnimationSet.addAnimation(mainTitleTextEnterTransAnim);
        
        AlphaAnimation mainTitleTextEnterAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
        		entranceAnimDuration, entranceDistance);
        mMainTitleTextExitAnimationSet.addAnimation(mainTitleTextEnterAlpahAnim);
        
        mMainTitleTextExitAnimationSet.setAnimationListener(mMainTitleTextEntranceAnimationSetListener);
	}
	
	public void makeMainpageExitAnim(long exitAnimDuration, float exitDistance) {
		// 1.mDoneText�� ������ �������		
		// 2.mMainTitle�� self -> left�� �̵��ϸ鼭 ������� �Ѵ� 		
		mDoneTextAlphaAnimationDuration = exitAnimDuration / 2;
		mDoneTextAlphaAnimation = new AlphaAnimation(1.0f, 0.2f);
		mDoneTextAlphaAnimation.setDuration(mDoneTextAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mDoneTextAlphaAnimation.setAnimationListener(mDoneTextDisappearAlphaAnimationListener);									
		
		mMainTitleTextExitAnimationSet = new AnimationSet(false);			
		// toXDelta�� (-)���� ���;߸� �������� �̵��Ѵ�
		float toXDelta = mPreviousBackText.getRight() - mMainTitleText.getLeft();
        TranslateAnimation mainTitleExitTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		Animation.RELATIVE_TO_SELF, 0.0f,
        		Animation.ABSOLUTE, toXDelta,
        		exitAnimDuration, exitDistance);
        mMainTitleTextExitAnimationSet.addAnimation(mainTitleExitTransAnim);
        
        AlphaAnimation mainTitleExitAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f,
        		exitAnimDuration, exitDistance);
        mMainTitleTextExitAnimationSet.addAnimation(mainTitleExitAlpahAnim);
        mMainTitleTextExitAnimationSet.setAnimationListener(mMainTitleTextExitAnimationSetListener);   
	}
	
	public void makeSubpageEntranceAnim(String text, long entranceAnimDuration, float entranceDistance) {
		// 1.previous action icon�� ������ ��Ÿ����
		// 2.previous action icon text�� ������ ��Ÿ����
		// 3.mSubPageTitleText�� right -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�		
		mPreviousIconAlphaAnimationDuration = entranceAnimDuration;
		mPreviousIconAlphaAnimation = new AlphaAnimation(0.2f, 1.0f);
		mPreviousIconAlphaAnimation.setDuration(mPreviousIconAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		
		mPreviousBackTextAlphaAnimationDuration = entranceAnimDuration;
		mPreviousBackTextAlphaAnimation = new AlphaAnimation(0.2f, 1.0f);
		mPreviousBackTextAlphaAnimation.setDuration(mPreviousBackTextAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�	
		
		mSubPageTitleText.setVisibility(View.VISIBLE);
		int textColor = Color.argb(0, 255, 255, 255);
		mSubPageTitleText.setTextColor(textColor);
        mSubPageTitleText.setText(text);
        
        mSubPageTitleTextAnimationSet = new AnimationSet(false);
        // mSubPageTitleText�� ���� ���� ���� �Ƹ��� mSubPageTitleText.getRight() ���� 0�ΰ� ����...
        int fromXType;
        float fromXDelta;
        int doneTextLeft = mDoneText.getLeft();
        int subPageTitleTextRight = mSubPageTitleText.getRight();		        
        if (subPageTitleTextRight == 0) {
        	fromXType = Animation.RELATIVE_TO_SELF;
        	fromXDelta = 1;	        	
        }
        else {
        	fromXType = Animation.ABSOLUTE;
        	fromXDelta = doneTextLeft - subPageTitleTextRight;
        }
        TranslateAnimation subPageTitleTextEnterTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		fromXType, fromXDelta,
        		Animation.RELATIVE_TO_SELF, 0,
        		entranceAnimDuration, entranceDistance);
        mSubPageTitleTextAnimationSet.addAnimation(subPageTitleTextEnterTransAnim);
        
        AlphaAnimation subPageTitleTextEnterAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
        		entranceAnimDuration, entranceDistance);
        mSubPageTitleTextAnimationSet.addAnimation(subPageTitleTextEnterAlpahAnim);
        
        mSubPageTitleTextAnimationSet.setAnimationListener(mSubpageTitleTextEntranceAnimationSetListener);        
	}
	
	public void makeSubpageExitAnim(long exitAnimDuration, float exitDistance) {
		// 1.previous action icon�� ������ �������
		// 2.previous action icon text�� ������ �������
		// 3.mSubPageTitleText�� self -> right�� �̵��ϸ鼭 ������� �Ѵ� 
		mPreviousIconAlphaAnimationDuration = exitAnimDuration / 2;
		mPreviousIconAlphaAnimation = new AlphaAnimation(1.0f, 0.2f);
		mPreviousIconAlphaAnimation.setDuration(mPreviousIconAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mPreviousIconAlphaAnimation.setAnimationListener(mPreviousIconExitAlphaAnimationListener);	
		
		mPreviousBackTextAlphaAnimationDuration = exitAnimDuration / 2;
		mPreviousBackTextAlphaAnimation = new AlphaAnimation(1.0f, 0.2f);
		mPreviousBackTextAlphaAnimation.setDuration(mPreviousBackTextAlphaAnimationDuration); // ���� duration ���� fragment�� ���� �޾� �´�				
		mPreviousBackTextAlphaAnimation.setAnimationListener(mPreviousBackTextExitAlphaAnimationListener);	
		
		mSubPageTitleTextAnimationSet = new AnimationSet(false);	
		// toXDelta�� (+)���� ���;߸� ���������� �̵��Ѵ�
		float toXDelta = mDoneText.getLeft() - mSubPageTitleText.getRight();
        TranslateAnimation subpageTitleExitTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		Animation.RELATIVE_TO_SELF, 0.0f,
        		Animation.ABSOLUTE, toXDelta,
        		exitAnimDuration, exitDistance);
        mSubPageTitleTextAnimationSet.addAnimation(subpageTitleExitTransAnim);
        
        AlphaAnimation subpageTitleExitAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f,
        		exitAnimDuration, exitDistance);
        mSubPageTitleTextAnimationSet.addAnimation(subpageTitleExitAlpahAnim);
        mSubPageTitleTextAnimationSet.setAnimationListener(mSubpageTitleTextExitAnimationSetListener); 
	}
	
	public void makeSubPageEntranceAnimByChild(long entranceAnimDuration, float entranceDistance) {
		// 2.mSubPageTitleText�� left -> selft�� �̵��ϸ鼭 ������� �Ѵ� 
		mSubPageTitleText.setVisibility(View.VISIBLE);
		int textColor = Color.argb(0, 255, 255, 255);
		mSubPageTitleText.setTextColor(textColor);
		mSubPageTitleTextAnimationSet = new AnimationSet(false);
        
        int fromXType;
        float fromXDelta;
        int prvBackTextRight = mPreviousBackText.getRight();
        int subPageTitleTextLeft = mSubPageTitleText.getLeft();		        
        if (subPageTitleTextLeft == 0) {
        	fromXType = Animation.RELATIVE_TO_SELF;
        	fromXDelta = -1;	        	
        }
        else {
        	fromXType = Animation.ABSOLUTE;
        	// fromXDelta�� (-)���� ���;߸� ���ʿ��� ���������� �̵��Ѵ�
        	fromXDelta = prvBackTextRight - subPageTitleTextLeft;
        }
        TranslateAnimation mainTitleTextEnterTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		fromXType, fromXDelta,
        		Animation.RELATIVE_TO_SELF, 0,
        		entranceAnimDuration, entranceDistance);
        mSubPageTitleTextAnimationSet.addAnimation(mainTitleTextEnterTransAnim);
        
        AlphaAnimation mainTitleTextEnterAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
        		entranceAnimDuration, entranceDistance);
        mSubPageTitleTextAnimationSet.addAnimation(mainTitleTextEnterAlpahAnim);
        
        mSubPageTitleTextAnimationSet.setAnimationListener(mSubpageTitleTextEntranceAnimationSetListener);
	}
	
	public void makeSuppageExitAnimByChild(long exitAnimDuration, float exitDistance) {
		// 1.mSubPageTitleText�� self -> left�� �̵��ϸ鼭 ������� �Ѵ� 
		mSubPageTitleTextAnimationSet = new AnimationSet(false);	
		// toXDelta�� (-)���� ���;߸� left�� �̵��Ѵ�
		float toXDelta = mPreviousBackText.getRight() - mSubPageTitleText.getLeft();
        TranslateAnimation subpageTitleExitTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		Animation.RELATIVE_TO_SELF, 0.0f,
        		Animation.ABSOLUTE, toXDelta,
        		exitAnimDuration, exitDistance);
        mSubPageTitleTextAnimationSet.addAnimation(subpageTitleExitTransAnim);
        
        AlphaAnimation subpageTitleExitAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f,
        		exitAnimDuration, exitDistance);
        mSubPageTitleTextAnimationSet.addAnimation(subpageTitleExitAlpahAnim);
        // ���⼭ �츮�� �������� �ٸ��� �����ϴ� ���� �ĺ��ؾ� �Ѵ�: ���� �׷� �ʿ䰡 ����
        mSubPageTitleTextAnimationSet.setAnimationListener(mSubpageTitleTextExitAnimationSetListener);
	}
	
	public void makeSubpageOfSubpageEntranceAnim(String text, long entranceAnimDuration, float entranceDistance) {
		// 2.mSubPageTitleTextOfSubPage�� right -> self�� �̵��ϸ鼭 ��Ÿ���� �Ѵ�
		mSubPageTitleTextOfSubPage.setVisibility(View.VISIBLE);
		int textColor = Color.argb(0, 255, 255, 255);
		mSubPageTitleTextOfSubPage.setTextColor(textColor);
		mSubPageTitleTextOfSubPage.setText(text);
        
		mSubPageTitleTextOfSubPageAnimationSet = new AnimationSet(false);	
		int fromXType = 0;
        float fromXDelta = 0;
        int frameLayoutRight = mFrameLayout.getWidth();	
        int subPageTitleTextOfSubPageRight = mSubPageTitleTextOfSubPage.getRight();
        if (subPageTitleTextOfSubPageRight == 0) {
        	// ���� ���� ����� �ƴϴ�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        	// ������ FrameLayout�� center�� ��ġ�ϱ� ������ Right ����
        	// �׸��� mSubPageTitleTextOfSubPage�� widht�� WRAP_CONTENT�̱� ������
        	// mSubPageTitleTextOfSubPage�� Half Width�� ���� �� �ִ�
        	// �����ؾ� �Ѵ�!!!
        	fromXType = Animation.RELATIVE_TO_SELF;
        	fromXDelta = 1;	
        }
        else {
        	fromXType = Animation.ABSOLUTE;
        	// fromXDelta�� (+)���� ���;߸� right���� left���� �̵��Ѵ�
        	fromXDelta = frameLayoutRight - subPageTitleTextOfSubPageRight;	
        }				
		TranslateAnimation subPageTitleTextOfSubPageEnterTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		fromXType, fromXDelta,
        		Animation.RELATIVE_TO_SELF, 0,
        		entranceAnimDuration, entranceDistance);
		mSubPageTitleTextOfSubPageAnimationSet.addAnimation(subPageTitleTextOfSubPageEnterTransAnim);
        
        AlphaAnimation subPageTitleTextOfSubPageEnterAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
        		entranceAnimDuration, entranceDistance);
        mSubPageTitleTextOfSubPageAnimationSet.addAnimation(subPageTitleTextOfSubPageEnterAlpahAnim);
        
        mSubPageTitleTextOfSubPageAnimationSet.setAnimationListener(mSubpageTitleTextOfSubPageEntranceAnimationSetListener);  
	}
	
	public void makeSubpageOfSubpageExitAnim(long exitAnimDuration, float exitDistance) {
		// 1.mSubPageTitleTextOfSubPage�� selft -> right�� �̵��ϸ鼭 ������� �Ѵ�
		mSubPageTitleTextOfSubPageAnimationSet = new AnimationSet(false);	
		// toXDelta�� (+)���� ���;߸� ���������� �̵��Ѵ�
		float toXDelta = mFrameLayout.getWidth() - mSubPageTitleTextOfSubPage.getRight();
        TranslateAnimation subpageTitleTextOfSubPageExitTransAnim = ITEAnimationUtils.makeSlideAnimation(
        		Animation.RELATIVE_TO_SELF, 0.0f,
        		Animation.ABSOLUTE, toXDelta,
        		exitAnimDuration, exitDistance);
        mSubPageTitleTextOfSubPageAnimationSet.addAnimation(subpageTitleTextOfSubPageExitTransAnim);
        
        AlphaAnimation subpageTitleTextOfSubPageExitAlpahAnim = ITEAnimationUtils.makeAlphaAnimation(
        		ITEAnimationUtils.ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f,
        		exitAnimDuration, exitDistance);
        mSubPageTitleTextOfSubPageAnimationSet.addAnimation(subpageTitleTextOfSubPageExitAlpahAnim);
        mSubPageTitleTextOfSubPageAnimationSet.setAnimationListener(mSubpageTitleTextOfSubPageExitAnimationSetListener); 	
	}
	
	public void startActionBarAnim() {
		if (mFromView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				mDoneText.startAnimation(mDoneTextAlphaAnimation);
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);		
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);			
			}
			else if (mToView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				mDoneText.startAnimation(mDoneTextAlphaAnimation);
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);		
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
			}
			else if (mToView == SettingsFragment.DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				mDoneText.startAnimation(mDoneTextAlphaAnimation);
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);		
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
			}
			else if (mToView == SettingsFragment.WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				mDoneText.startAnimation(mDoneTextAlphaAnimation);
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);		
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);
			}
		}
		else if (mFromView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				mPreviousIcon.startAnimation(mPreviousIconAlphaAnimation);
				mPreviousBackText.startAnimation(mPreviousBackTextAlphaAnimation);
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);	
			}
			else if (mToView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
				mSubPageTitleTextOfSubPage.startAnimation(mSubPageTitleTextOfSubPageAnimationSet);
			}
		}
		else if (mFromView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.TIMEZONE_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				mSubPageTitleTextOfSubPage.startAnimation(mSubPageTitleTextOfSubPageAnimationSet);
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
			}			
		}
		else if (mFromView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				mPreviousIcon.startAnimation(mPreviousIconAlphaAnimation);
				mPreviousBackText.startAnimation(mPreviousBackTextAlphaAnimation);
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);
			}
			else if (mToView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
				mSubPageTitleTextOfSubPage.startAnimation(mSubPageTitleTextOfSubPageAnimationSet);
			}
		}
		else if (mFromView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_SUB_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.ALERTTIME_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
				mSubPageTitleTextOfSubPage.startAnimation(mSubPageTitleTextOfSubPageAnimationSet);
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
			}
		}
		else if (mFromView == SettingsFragment.WEEKSTARTDAY_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				mPreviousIcon.startAnimation(mPreviousIconAlphaAnimation);
				mPreviousBackText.startAnimation(mPreviousBackTextAlphaAnimation);
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);	
			}			
		}
		else if (mFromView == SettingsFragment.DEFAULTCALENDAR_OVERRIDE_SETTING_VIEW_FRAGMENT_STATE) {
			if (mToView == SettingsFragment.MAIN_VIEW_FRAGMENT_STATE) {
				mPreviousIcon.startAnimation(mPreviousIconAlphaAnimation);
				mPreviousBackText.startAnimation(mPreviousBackTextAlphaAnimation);
				mSubPageTitleText.startAnimation(mSubPageTitleTextAnimationSet);	
				mMainTitleText.startAnimation(mMainTitleTextExitAnimationSet);	
			}			
		}
		
	}
}
