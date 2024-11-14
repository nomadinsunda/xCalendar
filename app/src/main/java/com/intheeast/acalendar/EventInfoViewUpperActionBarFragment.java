package com.intheeast.acalendar;

import com.intheeast.agenda.AgendaFragment;

import com.intheeast.acalendar.CalendarController.ViewType;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EventInfoViewUpperActionBarFragment extends Fragment {	
	private static String TAG = "EventInfoViewUpperActionBarFragment";
    private static boolean INFO = true;
    
	public static final int DAY_VIEW_INDEX = 0;
    public static final int WEEK_VIEW_INDEX = 1;
    public static final int MONTH_VIEW_INDEX = 2;
    public static final int AGENDA_VIEW_INDEX = 3;
    
	public static final int EVENT_MAIN_PAGE = 0;
	public static final int EVENT_REPEAT_PAGE = 1;
	public static final int EVENT_REPEATEND_PAGE = 2;
	public static final int EVENT_REMINDER_PAGE = 3;	
	public static final int EVENT_CALENDAR_PAGE = 4;
	public static final int EVENT_ATTENDEE_PAGE = 5;	
	public static final int EVENT_AVAILABILITY_PAGE = 6;
	public static final int EVENT_VISIBILITY_PAGE = 7;
	public static final int EVENT_ATTENDEE_SUB_PAGE = 8;
	
	public static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY = MINIMUM_SNAP_VELOCITY * 1.5f; 
	public static final float SWITCH_EVENTINFOVIEW_PAGE_EXIT_VELOCITY = SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY * 1.25f; 
	
    final float SWITCH_MAIN_PAGE_IN_FROMXVALUE = -1.0f;
    final float SWITCH_MAIN_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_MAIN_PAGE_OUT_TOXVALUE = 1.0f;
	
    final float SWITCH_SUB_PAGE_IN_FROMXVALUE = 1.0f;
    final float SWITCH_SUB_PAGE_TO_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_FROMXVALUE = 0.0f;
    final float SWITCH_SUB_PAGE_OUT_TOXVALUE = -1.0f;   
	
    Activity mActivity;
    FastEventInfoFragment mFastMainFragment;	
	RelativeLayout mView;
	
	public TextView mTitleText;
	public TextView mPreviousTitleText;
    public ImageView mPreviousActionIcon;
    //public TextView mActionBarPreviousViewText;
    public TextView mPrvActionIconTextOfCaller;
    //public TextView mActionBarMainBackText; 
    public TextView mCurrentPrvActionIconText; 
    public TextView mActionBarEditText;   
    //public View mActionBarPreviousActionContainer;
    
    
    //psudo_collapse_eventlistview_icon
    public ImageView mPsudoEventListViewCollapseIcon;
    
    //psudo_agendalistview_on_icon
    public ImageView mPsudoAgendaListViewOnIcon;
    
    //agendalistview_off_icon
    public ImageView mPsudoAgendaListViewOffIcon;
    //search_event_icon
    public ImageView mPsudoSearchEventIcon;
    
    
    //add_new_event_icon
    public ImageView mPsudoAddNewEventIcon;
    
    int mViewWidth = -1;
		
	int mCallerView;
	int mCurrentEventInfoPage;
	
	//private String mPreviousViewNames []; 	
	
	boolean mIsFastEventInfoFragment = true;	
	String mDateIndicatorTextOfCaller;	
	String mMainPagePrvActionIconTextString;
	
	// pixlr.com�� ���伥�� �������� ���� RGB(255, 255, 255) ���� RGB(254, 252, 252)�� �����Ѵ� : ��¥�� ��¿ �� ����
	// �׸��� �׶��̵��Ʈ ������ ���� �ȵǰ� ����Ʈ�� ������ ����ؾ� �Ѵ�
    // �׸��� �ȵ���̵尡 �����ϴ� �׼ǹ� ������(holo light)�� ���� ���� ���İ��� ����Ǿ� �����Ƿ�
    // pixlr.com���� ����� ���� ����. �׷��Ƿ� �ȵ���̵尡 �����ϴ� �׼ǹ� �������� �׸��ǿ��� �ϴ� ��� �׳� �����Ѵ�
    // �׷��� ä��? ���İ��� ���������� �����ȴ� 
	private static final int[] ACTION_PREVIOUS_ITEM_BG_COLOR = new int[]{254, 252, 252}; 
	                                                                                     
	 
	BitmapDrawable mPrvItemIcon = null;
    
    private ScrollInterpolator mHScrollInterpolator;
    float mViewSwitchingDelta;
    
    OnLayoutChangeListener mViewLayoutChangeListener = new OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {		
			
			if (mViewWidth == -1) {
				if (INFO) Log.i(TAG, "ViewLayoutChangeListener");
				
				mViewWidth = right - left;				
				
	    		float xOffSet = mViewWidth / 2;
	    		mViewSwitchingDelta = mViewWidth - xOffSet;
	    		mAnimationDistance = mViewWidth - xOffSet;	    		
			}
		}		
	};	
	
	OnTouchListener mActionBarPreviousActionIconTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			onTouchAnimation(event);		
			//return true; //mActionBarListener�� ȣ����� ����
			return false;
		}
    	
    };
    
    OnTouchListener mCurrentPrvActionIconTextTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			onTouchAnimation(event);		
			//return true; //mActionBarListener�� ȣ����� ����
			return false;
		}    	
    };
    
    public void onTouchAnimation(MotionEvent event) {
    	int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			if (INFO) Log.i(TAG, "onTouch, ACTION_DOWN");
			
			AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
			downAlpha.setDuration(0); 
			downAlpha.setFillAfter(true);		
			mPreviousActionIcon.startAnimation(downAlpha);		
			if (mCurrentEventInfoPage == EVENT_MAIN_PAGE) {
				
			}
			else if (mCurrentEventInfoPage == EVENT_ATTENDEE_SUB_PAGE) {
				mPreviousTitleText.setAlpha(0.2f);				
			}
			else {
				mCurrentPrvActionIconText.setAlpha(0.2f);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (INFO) Log.i(TAG, "onTouch, ACTION_UP");
			
			if (mCurrentEventInfoPage == EVENT_MAIN_PAGE) {
				
			}
			else if (mCurrentEventInfoPage != EVENT_MAIN_PAGE) {
				AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
				upAlpha.setDuration(0); 
				upAlpha.setFillAfter(true); 			
				mPreviousActionIcon.startAnimation(upAlpha);	
				if (mCurrentEventInfoPage == EVENT_ATTENDEE_SUB_PAGE)
					mPreviousTitleText.setAlpha(1.0f);					
				else 
					mCurrentPrvActionIconText.setAlpha(1.0f);				
			}
			break;
			
			
		default:
			break;
		}				
    }
    
    OnTouchListener mPrvTitleTextViewTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
				if (INFO) Log.i(TAG, "mPrvTitleTextViewTouchListener, ACTION_DOWN");
				AlphaAnimation downAlpha = new AlphaAnimation(1.0f, 0.2F); 
				downAlpha.setDuration(0); 
				downAlpha.setFillAfter(true);		
				mPreviousActionIcon.startAnimation(downAlpha);			
				mPreviousTitleText.setAlpha(0.2f);
				break;
			case MotionEvent.ACTION_UP:
				if (INFO) Log.i(TAG, "mPrvTitleTextViewTouchListener, ACTION_UP");
				if (mCurrentEventInfoPage != EVENT_MAIN_PAGE) {
					AlphaAnimation upAlpha = new AlphaAnimation(0.2f, 1.0F); 
					upAlpha.setDuration(0); 
					upAlpha.setFillAfter(true); 			
					mPreviousActionIcon.startAnimation(upAlpha);				
					mPreviousTitleText.setAlpha(1.0f);
				}
				break;
				
				
			default:
				break;
			}				
			
			return false;
		}
    	
    };
    
    private final OnClickListener mPrvActionIconClickListener = new OnClickListener() {
        @SuppressLint("SuspiciousIndentation")
		@Override
        public void onClick(View v) {
        	if (INFO) Log.i(TAG, "mPrvActionIconClickListener");
            onActionBarItemSelected(v.getId());
        }
    };       
    
    private final OnClickListener mCurrentPrvActionTextViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	if (INFO) Log.i(TAG, "mCurrentPrvActionTextViewClickListener");
            onActionBarItemSelected(v.getId());
        }
    };      
    
    private final OnClickListener mEditTextViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	if (INFO) Log.i(TAG, "mEditTextViewClickListener");
            onActionBarItemSelected(v.getId());
        }
    };      
    
    private final OnClickListener mPrvTitleTextViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	if (INFO) Log.i(TAG, "mPrvTitleTextViewClickListener");
            onActionBarItemSelected(v.getId());
        }
    };    
    
    private boolean onActionBarItemSelected(int itemId) {
    	if (INFO) Log.i(TAG, "onActionBarItemSelected");
    	
        if (itemId == R.id.previous_action_icon ||
        		itemId == R.id.currentview_prv_action_icon_text ||
        		itemId == R.id.previous_title_text) {  
        	
        	if (mCurrentEventInfoPage == EVENT_MAIN_PAGE) {
        		if (INFO) Log.i(TAG, "onActionBarItemSelected, EVENT_MAIN_PAGE");
        		if (mIsFastEventInfoFragment) {  
        			// CalendarViewsUpperActionBarFragment�� animation ȿ���� �ο��Ǿ�� �Ѵ�
        			// �׸��� eventinfo actionbar���� animation ȿ���� �ο��Ǿ�� �Ѵ�
        			mFastMainFragment.doBack();/////////////////////////////////////////////////////////////////////////////////////
        		}
        		
        	}
        	else {
        		if (mCurrentEventInfoPage == EVENT_ATTENDEE_SUB_PAGE) {
        			if (INFO) Log.i(TAG, "onActionBarItemSelected, EVENT_ATTENDEE_SUB_PAGE");
        			replaceActionBarBySelectingPreviousIcon(EVENT_ATTENDEE_PAGE);  
        			if (mIsFastEventInfoFragment)
            			mFastMainFragment.attendeePageViewSwitching(false);
            		      			
        		}
        		else {
        			if (INFO) Log.i(TAG, "onActionBarItemSelected, NOT EVENT_ATTENDEE_SUB_PAGE");
        			int previousSubPage = mCurrentEventInfoPage;
        			replaceActionBarBySelectingPreviousIcon(EVENT_MAIN_PAGE);	  
        			if (mIsFastEventInfoFragment)
            			mFastMainFragment.switchingEventPage(false, previousSubPage);
            				
        		}
        	}
        	
        } else if (itemId == R.id.eventedit_textview) {
        	if (mCurrentEventInfoPage == EVENT_MAIN_PAGE)
        		if (mIsFastEventInfoFragment)
        			mFastMainFragment.doEdit();
        		      		            
        }
        return true;
    }

	public EventInfoViewUpperActionBarFragment() {
		mCurrentEventInfoPage = EVENT_MAIN_PAGE;
	}	
	
	@SuppressLint("ValidFragment")
	public EventInfoViewUpperActionBarFragment(FastEventInfoFragment mainFragment, int callerView, String dateIndicatorTextOfCaller) {
		mCurrentEventInfoPage = EVENT_MAIN_PAGE;
		mFastMainFragment = mainFragment;
		mCallerView = callerView;		
		mDateIndicatorTextOfCaller = dateIndicatorTextOfCaller;
	}
	
	@SuppressLint("ValidFragment")
	public EventInfoViewUpperActionBarFragment(FastEventInfoFragment mainFragment, int callerView, String dateIndicatorTextOfCaller, String prvActionIconText) {
		mCurrentEventInfoPage = EVENT_MAIN_PAGE;
		mFastMainFragment = mainFragment;
		mCallerView = callerView;
		mDateIndicatorTextOfCaller = dateIndicatorTextOfCaller;
		mMainPagePrvActionIconTextString = prvActionIconText;
	}
	
	String mSearchQuery;
	Bitmap mSearchViewActionBarEditTextBlurredBG;
	@SuppressLint("ValidFragment")
	public EventInfoViewUpperActionBarFragment(FastEventInfoFragment mainFragment, int callerView, String query, Bitmap bitmap) {
		mCurrentEventInfoPage = EVENT_MAIN_PAGE;
		mFastMainFragment = mainFragment;
		mCallerView = callerView;	
		mSearchQuery = query;
		mSearchViewActionBarEditTextBlurredBG = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
	}
		
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (INFO) Log.i(TAG, "onAttach");
		
		mActivity = activity;	
	}
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (INFO) Log.i(TAG, "onCreate");        
    }
		
	RelativeLayout mParentLayout;
	RelativeLayout mPsudoSearchViewActionBar;
	RelativeLayout mPsudoSearchViewActionBarEditTextBGContainer;
	TextureView mPsudoSearchViewActionBarEditTextFactory;
	ImageView mPsudoSearchViewActionBarEditTextBlurredBG;
	RelativeLayout mPsudoSearchViewActionBarEditTextContainer;
	CustomEditText mPsudoSearchViewActionBarEditText;
	Button mPsudoSearchViewActionBarCrossButton;
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		if (INFO) Log.i(TAG, "onCreateView");		
		
		mView = (RelativeLayout) inflater.inflate(R.layout.event_info_new_upper_actionbar, null);  
		//parent_layout
		mParentLayout = (RelativeLayout) mView.findViewById(R.id.parent_layout);
		if (mCallerView == ViewType.SEARCH) {
			int editTextWidth = mSearchViewActionBarEditTextBlurredBG.getWidth();
	        int editTextHeight = mSearchViewActionBarEditTextBlurredBG.getHeight();
	        
			mPsudoSearchViewActionBar = (RelativeLayout) inflater.inflate(R.layout.searchviews_upper_actionbar, null);
			
			mPsudoSearchViewActionBarEditTextBGContainer = (RelativeLayout) mPsudoSearchViewActionBar.findViewById(R.id.blurring_container);
			RelativeLayout.LayoutParams textureViewParams = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
	        textureViewParams.addRule(RelativeLayout.CENTER_IN_PARENT); 
	        mPsudoSearchViewActionBarEditTextBGContainer.setLayoutParams(textureViewParams);
	        
	        mPsudoSearchViewActionBarEditTextFactory = (TextureView) mPsudoSearchViewActionBarEditTextBGContainer.findViewById(R.id.edittext_blurred_bg_factory);
	        mPsudoSearchViewActionBarEditTextFactory.setVisibility(View.GONE);    
	        mPsudoSearchViewActionBarEditTextBlurredBG = (ImageView) mPsudoSearchViewActionBarEditTextBGContainer.findViewById(R.id.edittext_blurred_bg);
	        mPsudoSearchViewActionBarEditTextBlurredBG.setVisibility(View.VISIBLE);			
	        mPsudoSearchViewActionBarEditTextBlurredBG.setImageBitmap(mSearchViewActionBarEditTextBlurredBG);			
	        
			mPsudoSearchViewActionBarEditTextContainer = (RelativeLayout) mPsudoSearchViewActionBar.findViewById(R.id.edittext_container);
	        
	        RelativeLayout.LayoutParams editTextContainerParams = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
	        editTextContainerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
	        mPsudoSearchViewActionBarEditTextContainer.setLayoutParams(editTextContainerParams);
	        
	        mPsudoSearchViewActionBarEditText = (CustomEditText) mPsudoSearchViewActionBarEditTextContainer.findViewById(R.id.search_edittext);
	        mPsudoSearchViewActionBarEditText.setText(mSearchQuery);
	        mPsudoSearchViewActionBarCrossButton = (Button) mPsudoSearchViewActionBarEditTextContainer.findViewById(R.id.cross_button);
	        mPsudoSearchViewActionBarEditText.setCrossButton(mPsudoSearchViewActionBarCrossButton);        
	        
	        // ���� ���� ������ onCreateView���� ���� ������ View���� �����ؼ� �����ؾ� �Ѵ�
	        // :mCallerView == ViewType.SEARCH �� ��쿡�� mPsudoSearchViewActionBar�� ��� animation�� �� �� �ִ°� �̴�
	        //  �� switch view�� ���� ���� ������ container layout ���ǰ� �ʿ�� �ȴ�!!!
	        //  �ƴϴ� ���ݱ��� �߸������ϰ� ���� ���� �ִ�
	        //  �ƿ� fragment slide translate animation�� ����� �� ���� ������?
	        //  �׷��� �Ǹ� interplator�� �ٸ��� ������ main view switch animation�� ������ ȿ���� ��ġ ��������!!!
	        mView.addView(mPsudoSearchViewActionBar);
		}
        
		if (mPrvItemIcon == null) {
	    	Drawable icon = (Drawable)getResources().getDrawable(R.drawable.ic_action_previous_item);  
	    	icon.mutate();
	    	
	    	int toBackground = getResources().getColor(R.color.eventInfoActivity_actionbar_background);
	    	Bitmap src = ((BitmapDrawable)icon).getBitmap();
	    	Config srcConfig = src.getConfig();
	    	Bitmap srcBitmap = src.copy(srcConfig, true);
	        
	        int width = srcBitmap.getWidth();
	        int height = srcBitmap.getHeight();
	        for(int x = 0; x < width; x++)
	            for(int y = 0; y < height; y++) {            	
	                if(match(srcBitmap.getPixel(x, y)))
	                	srcBitmap.setPixel(x, y, toBackground);            
	            }        
	        
	        mPrvItemIcon = new BitmapDrawable(getResources(), srcBitmap);	
    	}    	
			
		mPreviousActionIcon = (ImageView) mParentLayout.findViewById(R.id.previous_action_icon);
		mPreviousActionIcon.setBackground(mPrvItemIcon);
		mPreviousActionIcon.setOnTouchListener(mActionBarPreviousActionIconTouchListener);
		mPreviousActionIcon.setOnClickListener(mPrvActionIconClickListener);
        
		mPrvActionIconTextOfCaller = (TextView) mParentLayout.findViewById(R.id.prv_action_icon_text_of_caller);		
		mPrvActionIconTextOfCaller.setVisibility(View.GONE);		
        
		mCurrentPrvActionIconText = (TextView) mParentLayout.findViewById(R.id.currentview_prv_action_icon_text);
        mCurrentPrvActionIconText.setVisibility(View.GONE);////////////////////////////////              
        mCurrentPrvActionIconText.setText("�ڷ�"); // profile������ "�ʴ���� ���"���� �����Ǿ�� �Ѵ�        
        mCurrentPrvActionIconText.setOnTouchListener(mCurrentPrvActionIconTextTouchListener);
        mCurrentPrvActionIconText.setOnClickListener(mCurrentPrvActionTextViewClickListener);
        
        mTitleText = (TextView) mParentLayout.findViewById(R.id.title_text); 
        mTitleText.setVisibility(View.INVISIBLE);
        
        mPreviousTitleText = (TextView) mParentLayout.findViewById(R.id.previous_title_text);  
        mPreviousTitleText.setOnTouchListener(mPrvTitleTextViewTouchListener);
        mPreviousTitleText.setOnClickListener(mPrvTitleTextViewClickListener);
        mPreviousTitleText.setVisibility(View.GONE); 
        
        mActionBarEditText = (TextView) mParentLayout.findViewById(R.id.eventedit_textview);
        mActionBarEditText.setVisibility(View.INVISIBLE);       
        
        mActionBarEditText.setOnClickListener(mEditTextViewClickListener);  
        
        mPsudoAddNewEventIcon = (ImageView)mParentLayout.findViewById(R.id.psudo_add_new_event_icon);
        
        mPsudoSearchEventIcon = (ImageView)mParentLayout.findViewById(R.id.psudo_search_event_icon);        
        
        mPsudoEventListViewCollapseIcon = (ImageView)mParentLayout.findViewById(R.id.psudo_collapse_eventlistview_icon);
        
        mPsudoAgendaListViewOnIcon = (ImageView)mParentLayout.findViewById(R.id.psudo_agendalistview_on_icon);       
        mPsudoAgendaListViewOffIcon = (ImageView)mParentLayout.findViewById(R.id.psudo_agendalistview_off_icon);             
        
        mHScrollInterpolator = new ScrollInterpolator();
        
        mViewWidth = getResources().getDisplayMetrics().widthPixels;
		
		float xOffSet = mViewWidth / 2;
		mViewSwitchingDelta = mViewWidth - xOffSet;
		mAnimationDistance = mViewWidth - xOffSet;		
		
		setAnimationSettingOnCreateView();
        
        return mView;
	}	

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (INFO) Log.i(TAG, "onViewCreated");
		
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		if (INFO) Log.i(TAG, "onActivityCreated");
		
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {
		if (INFO) Log.i(TAG, "onStart");
		
		super.onStart();
	}

	@Override
	public void onResume() {
		if (INFO) Log.i(TAG, "onResume");
		
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (INFO) Log.i(TAG, "onSaveInstanceState");
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (INFO) Log.i(TAG, "onConfigurationChanged");
		
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onPause() {
		if (INFO) Log.i(TAG, "onPause");
		
		super.onPause();
	}

	@Override
	public void onStop() {
		if (INFO) Log.i(TAG, "onStop");
		
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		if (INFO) Log.i(TAG, "onDestroyView");
		
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if (INFO) Log.i(TAG, "onDestroy");
		
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		if (INFO) Log.i(TAG, "onDetach");
		
		super.onDetach();
	}

	@Override
	public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
		if (INFO) Log.i(TAG, "onCreateAnimator");
		// �츮�� ���� onCreateAnimator�� enter animation�� ������ ������ �����޴�? ���ҷ� ��븸 �Ѵ�
		// :���⼭ � animation�� start ��Ű�� �ʴ´�
		if (enter)
			startActionBarEntranceAnimation();
		return super.onCreateAnimator(transit, enter, nextAnim);
	}

	
	public void replaceActionBarBySelectingEventItemLayout(int subPageId) {		
		
		switch(subPageId) {		
		case EVENT_REMINDER_PAGE:
			if (INFO) Log.i(TAG, "replaceActionBarBySelectingEventItemLayout, EVENT_REMINDER_PAGE");					
			setSubPageActionBar("�˸�", "�̺�Ʈ ���λ���");			
			break;
		case EVENT_CALENDAR_PAGE:
			if (INFO) Log.i(TAG, "replaceActionBarBySelectingEventItemLayout, EVENT_CALENDAR_PAGE");						
			setSubPageActionBar("Ķ����", "�̺�Ʈ ���λ���");
			break;
		// �� ���� ��찡 ���� �� �ִ�
		// 1.<<under main page page>>
		// 2.<<under attendee sup page>>
		// : �� �� ��Ȳ�� ���͸��ϱ� ���ؼ��� ���� �������� ��Ÿ���� indicator ������ �ʿ���
		case EVENT_ATTENDEE_PAGE:
			if (INFO) Log.i(TAG, "replaceActionBarBySelectingEventItemLayout, EVENT_ATTENDEE_PAGE");					
			setSubPageActionBar("�ʴ���� ���", "�̺�Ʈ ���λ���");			
			break;		
		// �� ����  ������ <<under attendee sup page>> ��Ȳ
		case EVENT_ATTENDEE_SUB_PAGE: // 
			if (INFO) Log.i(TAG, "replaceActionBarBySelectingEventItemLayout, EVENT_ATTENDEE_SUB_PAGE");						
			setAttendeSubPageActionBar("�ʴ���� ���");	
			break;
		default:
			break;
		}
		
		mCurrentEventInfoPage = subPageId;		
	}	
	
	public void replaceActionBarBySelectingPreviousIcon(int subPageId) {		
		
		switch(subPageId) {
		case EVENT_MAIN_PAGE:	
			if (INFO) Log.i(TAG, "replaceActionBarBySelectingPreviousIcon, EVENT_MAIN_PAGE");
			setMainPageActionBar("�̺�Ʈ ���λ���", true);				
			break;		
		case EVENT_ATTENDEE_PAGE:		
			if (INFO) Log.i(TAG, "replaceActionBarBySelectingPreviousIcon, EVENT_ATTENDEE_PAGE");
			returnAttendeeSubPageActionBar("�ʴ���� ���", false);					
			break;
		default:
			break;
		}
		
		mCurrentEventInfoPage = subPageId;		
	}	
	
	

	AnimationListener mActionBarPreviousTitleTextAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mActionBarPreviousTitleTextAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mActionBarPreviousTitleTextAnimlistener, end");
			mPreviousTitleText.setVisibility(View.GONE);	
			if (mCurrentEventInfoPage == EVENT_MAIN_PAGE) {
				
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mPrvActionIconTextOfCallerAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, " mActionBarPreViewTextAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mActionBarPreViewTextAnimlistener, end");
			mPreviousTitleText.setVisibility(View.GONE);	
			mPrvActionIconTextOfCaller.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mCurrentPrvActionIconTextAnimationSetListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mCurrentPrvActionIconTextAnimationSet, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mCurrentPrvActionIconTextAnimationSet, end");
			mCurrentPrvActionIconText.setText(mMainPagePrvActionIconTextString);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	AnimationListener mPrvActionIconTextOfCallerAnimationSet = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {	
			if (INFO) Log.i(TAG, "mPrvActionIconTextOfCallerAnimationSet, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mPrvActionIconTextOfCallerAnimationSet, end");
			mPrvActionIconTextOfCaller.setVisibility(View.GONE);
			//mCurrentPrvActionIconText.setText(mDateIndicatorTextOfCaller);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};	
	
	public void setMainPageActionBar(String titleText, boolean showEditText) {	
		if (INFO) Log.i(TAG, "setMainPageActionBar");
		
		AnimationSet titleAnimationSet = new AnimationSet(false);
        TranslateAnimation titleTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, -0.4f, 
        		Animation.RELATIVE_TO_SELF, 0);
        titleAnimationSet.addAnimation(titleTextTransAnim);
        AlphaAnimation titleTextAlphaAnim = makeAlphaAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_TRANSPARENT_TO_OPAQUE, 0.2f, 1.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
        titleAnimationSet.addAnimation(titleTextAlphaAnim);
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////        
        AnimationSet previousTitleTextAnimationSet = new AnimationSet(false);
        TranslateAnimation previousTitleTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_PARENT, 0.5f, 
        		Animation.RELATIVE_TO_PARENT, 0.9f);
        previousTitleTextAnimationSet.addAnimation(previousTitleTextTransAnim);
        AlphaAnimation previousTitleTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_OPAQUE_TO_TRANSPARENT, 0.4f, 0.0f,
				ANIMATION_INTERPOLATORTYPE_DECELERATE);
        previousTitleTextAnimationSet.addAnimation(previousTitleTextAlphaAnim);       
        previousTitleTextAnimationSet.setAnimationListener(mActionBarPreviousTitleTextAnimlistener);       
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        AnimationSet currentPrvActionIconTextAnimationSet = new AnimationSet(false);
        TranslateAnimation mainBackTextTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, 0, //from pos
        		Animation.RELATIVE_TO_SELF, 1);//to pos
        currentPrvActionIconTextAnimationSet.addAnimation(mainBackTextTransAnim);
        AlphaAnimation mainBackTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_OPAQUE_TO_TRANSPARENT, 0.2f, 0.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
        currentPrvActionIconTextAnimationSet.addAnimation(mainBackTextAlphaAnim);
        currentPrvActionIconTextAnimationSet.setAnimationListener(mCurrentPrvActionIconTextAnimationSetListener);
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        AnimationSet prvActionIconTextOfCallerAnimationSet = new AnimationSet(false);
        TranslateAnimation preViewTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, -1, 
        		Animation.RELATIVE_TO_SELF, 0);
        prvActionIconTextOfCallerAnimationSet.addAnimation(preViewTextTransAnim);
        AlphaAnimation preViewTextAlphaAnim = makeAlphaAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_TRANSPARENT_TO_OPAQUE, 0.2f, 1.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
        prvActionIconTextOfCallerAnimationSet.addAnimation(preViewTextAlphaAnim);
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        AlphaAnimation editTextAlpahAnim = makeAlphaAnimation(0.5f, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
        		ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);        		
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        String prvTitleText = (String)mTitleText.getText();
		mPreviousTitleText.setText(prvTitleText);					
		
		mTitleText.setText(titleText);
		
		//mPrvActionIconTextOfCaller
		//mCurrentPrvActionIconText
		// <<under agenda view -> event info view>>
		// if attendee sub page���� event info main page�� return �Ѵٸ�
		// mCurrentPrvActionIconText[�ڷ�]�� ���������� �̵��ϸ鼭 ������� ȿ���� �����ؾ� �ϰ�
		// mPrvActionIconTextOfCaller[1�� 28��]�� ���ʿ��� self pos�� �̵��ϴ� ȿ���� �����ؾ� �Ѵ�
		// :�� animation ȿ���� �Ϸ�Ǹ� 
		//  mPrvActionIconTextOfCaller�� UNVISIBLE ���·� ��ȯ�Ǿ�� �ϰ�
		//  mCurrentPrvActionIconText[1�� 28��]�� �����Ǿ�� �Ѵ�
		mPrvActionIconTextOfCaller.setText(mMainPagePrvActionIconTextString);
		
		if (showEditText)
			mActionBarEditText.setVisibility(View.VISIBLE);
		else 
			mActionBarEditText.setVisibility(View.GONE);
		
		mPrvActionIconTextOfCaller.setVisibility(View.VISIBLE);
        mPreviousTitleText.setVisibility(View.VISIBLE);       
        
        mPreviousTitleText.startAnimation(previousTitleTextAnimationSet);
        mTitleText.startAnimation(titleAnimationSet); 
        mCurrentPrvActionIconText.startAnimation(currentPrvActionIconTextAnimationSet);        
        mPrvActionIconTextOfCaller.startAnimation(prvActionIconTextOfCallerAnimationSet);
        if (showEditText)
        	mActionBarEditText.startAnimation(editTextAlpahAnim);		
	}
	
	
	// attendee sub page�� ���� mActionBarPreviousTitleText�� ��������!!!
	public void setSubPageActionBar(String mainTitleText, String previousTitleText) {	
		if (INFO) Log.i(TAG, "setSubPageActionBar");
		
		mTitleText.setText(mainTitleText);	
		
		Animation titleAnimation = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				Animation.RELATIVE_TO_PARENT, 0.5f,
				Animation.RELATIVE_TO_SELF, 0);	
		//titleAnimation.setFillAfter(true); <---setFillAfter�� ��Ȯ�ϰ� ������� �ʴ´ٸ� ������ �� ���� ��Ȳ�� �߻��Ѵ�!!!
		///////////////////////////////////////////////////////////////////////////////////////////////////////
		AnimationSet previousTitleTextAnimationSet = new AnimationSet(false);
        TranslateAnimation previousTitleTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, 0.5f,
        		Animation.RELATIVE_TO_SELF, 0.25f);
        previousTitleTextAnimationSet.addAnimation(previousTitleTextTransAnim);
        
        AlphaAnimation previousTitleTextAlpahAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.2f, 0.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
        previousTitleTextAnimationSet.addAnimation(previousTitleTextAlpahAnim);
        previousTitleTextAnimationSet.setAnimationListener(mActionBarPreviousTitleTextAnimlistener);
        ///////////////////////////////////////////////////////////////////////////////////////////////////////
		
		AnimationSet currentPrvActionIconTextAnimationSet = new AnimationSet(false);
        TranslateAnimation mainbackTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, 1,
        		Animation.RELATIVE_TO_SELF, 0);
        currentPrvActionIconTextAnimationSet.addAnimation(mainbackTextTransAnim);
        
        AlphaAnimation mainbackTextAlpahAnim = makeAlphaAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
        		ALPHA_TRANSPARENT_TO_OPAQUE, 0.2f, 1.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
        currentPrvActionIconTextAnimationSet.addAnimation(mainbackTextAlpahAnim);
        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        
        AnimationSet prvActionIconTextOfCallerAnimationSet = new AnimationSet(false);
        TranslateAnimation preViewTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, 0.0f,
        		Animation.RELATIVE_TO_SELF, -1);
        prvActionIconTextOfCallerAnimationSet.addAnimation(preViewTextTransAnim);
        
        AlphaAnimation preViewTextAlpahAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.3f, 0.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
        prvActionIconTextOfCallerAnimationSet.addAnimation(preViewTextAlpahAnim);
        prvActionIconTextOfCallerAnimationSet.setAnimationListener(mPrvActionIconTextOfCallerAnimationSetListener);
        ///////////////////////////////////////////////////////////////////////////////////////////////////////                
        mPreviousTitleText.setText(previousTitleText); //////////////////////////////////////////////////////////////////////////////        
		mPreviousTitleText.setVisibility(View.VISIBLE);		
		mActionBarEditText.setVisibility(View.GONE);		
		
		// <<under agenda view -> event info view>>
		// if event info main page���� attendee sub page�� ����Ī�ȴٸ�,		
		// mPrvActionIconTextOfCaller�� text�� [1�� 28��]�� �����ϰ� VISIBLE ���·� ��ȯ�� ���� self pos���� �������� �̵��ϸ鼭 ������� ȿ���� �����ϰ�
		// mCurrentPrvActionIconText�� text�� [�ڷ�]�� �����ϰ�  �����ʿ��� self pos�� �̵��ϴ� ȿ���� �����ؾ� �Ѵ�
		// :�� animation ȿ���� �Ϸ�Ǹ� 
		//  mPrvActionIconTextOfCaller�� UNVISIBLE ���·� ��ȯ�Ǿ�� �Ѵ�		 
		mPrvActionIconTextOfCaller.setVisibility(View.VISIBLE);
		mPrvActionIconTextOfCaller.setText(mDateIndicatorTextOfCaller);
		
		mCurrentPrvActionIconText.setText("�ڷ�");
		
		mPrvActionIconTextOfCaller.startAnimation(prvActionIconTextOfCallerAnimationSet);
		mPreviousTitleText.startAnimation(previousTitleTextAnimationSet);
		mCurrentPrvActionIconText.startAnimation(currentPrvActionIconTextAnimationSet);
		mTitleText.startAnimation(titleAnimation);    
	}	
	
	
	
	public void setAttendeSubPageActionBar(String mainTitleText) {
		if (INFO) Log.i(TAG, "setAttendeSubPageActionBar");
		// mCurrentPrvActionIconText �� self pos���� �������� �̵���Ų �� ������� ȿ���� �����Ѵ�
		// main title�� self pos���� < ������ �������� �̵����Ѿ� �Ѵ�
		// :�̷� �ʿ���� icon ������ ��ġ�� previous_title_text�� �̵����� visible ���·� ��ȯ�ϰ�
		//  main title�� unvisible ���·� �����		
		AnimationSet currentPrvActionIconTextAnimationSet = new AnimationSet(false);
		TranslateAnimation currentPrvActionIconTextTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
				Animation.RELATIVE_TO_SELF, 0, //from pos
				Animation.RELATIVE_TO_SELF, -1);//to pos
		currentPrvActionIconTextAnimationSet.addAnimation(currentPrvActionIconTextTransAnim);
		AlphaAnimation currentPrvActionIconTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_OPAQUE_TO_TRANSPARENT, 0.4f, 0.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
		currentPrvActionIconTextAnimationSet.addAnimation(currentPrvActionIconTextAlphaAnim);
		currentPrvActionIconTextAnimationSet.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {				
			}

			@Override
			public void onAnimationEnd(Animation animation) {				
				mCurrentPrvActionIconText.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {					
			}		
		});
		
		if (mTitleText.getVisibility() != View.GONE) {			
			if (INFO) Log.i(TAG, "setAttendeSubPageActionBar : mTitleText NOT GONE");
		}
		
		mTitleText.setVisibility(View.GONE);
		
		mPreviousTitleText.setText(mainTitleText);
		AnimationSet previousTitleTextAnimationSet = new AnimationSet(false);
        TranslateAnimation previousTitleTextTransAnim = makeSlideAnimation(1, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, 1, 
        		Animation.RELATIVE_TO_SELF, 0);
        previousTitleTextAnimationSet.addAnimation(previousTitleTextTransAnim);
        AlphaAnimation previousTitleTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_OPAQUE_TO_TRANSPARENT, 0.4f, 1,
				ANIMATION_INTERPOLATORTYPE_DECELERATE);
        previousTitleTextAnimationSet.addAnimation(previousTitleTextAlphaAnim);       
        previousTitleTextAnimationSet.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {				
			}

			@Override
			public void onAnimationEnd(Animation animation) {		
				mPreviousTitleText.setClickable(true);				
			}

			@Override
			public void onAnimationRepeat(Animation animation) {					
			}		
		});        
        
        mPreviousTitleText.setVisibility(View.VISIBLE);
        
        mCurrentPrvActionIconText.startAnimation(currentPrvActionIconTextAnimationSet);
        mPreviousTitleText.startAnimation(previousTitleTextAnimationSet);        
	}
	
	
	public void returnAttendeeSubPageActionBar(String titleText, boolean showEditText) {
		if (INFO) Log.i(TAG, "returnAttendeeSubPageActionBar");
		
		// mCurrentPrvActionIconText�� VISIBLE ���·� ��ȯ�ϰ�
		// ���ʿ��� self pos �̵���Ű�� ȿ���� �����Ѵ�
		// previous_title_text�� self pos���� ������ �������� main title text�� ��ġ���� �̵���Ų ��,
		// previous_title_text�� GONE ���·� ��ȯ�ϰ� main title text�� VISIBLE ���·� ��ȯ�Ѵ�
		// :�̷� �ʿ���� icon ������ ��ġ�� previous_title_text�� �̵����� visible ���·� ��ȯ�ϰ�
		//  main title�� unvisible ���·� �����		
		mCurrentPrvActionIconText.setVisibility(View.VISIBLE);
		AnimationSet currentPrvActionIconTextAnimationSet = new AnimationSet(false);
		TranslateAnimation currentPrvActionIconTextTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
				Animation.RELATIVE_TO_SELF, -1, //from pos
				Animation.RELATIVE_TO_SELF, 0);//to pos
		currentPrvActionIconTextAnimationSet.addAnimation(currentPrvActionIconTextTransAnim);
		AlphaAnimation currentPrvActionIconTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
				ALPHA_TRANSPARENT_TO_OPAQUE, 0.2f, 1,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);
		currentPrvActionIconTextAnimationSet.addAnimation(currentPrvActionIconTextAlphaAnim);
		mCurrentPrvActionIconText.setText("�ڷ�");
		
		mPreviousTitleText.setClickable(false);
		mPreviousTitleText.setVisibility(View.GONE);
		mTitleText.setVisibility(View.VISIBLE);
		mTitleText.setText(titleText);		
		//int abPTTLeft = mPreviousTitleText.getLeft();
		//int abTTLeft = mTitleText.getLeft();		
		AnimationSet titleTextAnimationSet = new AnimationSet(false);
        TranslateAnimation titleTextTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
        		Animation.RELATIVE_TO_SELF, -1, 
        		Animation.RELATIVE_TO_SELF, 0);
        titleTextAnimationSet.addAnimation(titleTextTransAnim);
        AlphaAnimation titleTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
        		ALPHA_TRANSPARENT_TO_OPAQUE, 0.2f, 1,
				ANIMATION_INTERPOLATORTYPE_DECELERATE);
        titleTextAnimationSet.addAnimation(titleTextAlphaAnim);       
        titleTextAnimationSet.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {				
			}

			@Override
			public void onAnimationEnd(Animation animation) {				
			}

			@Override
			public void onAnimationRepeat(Animation animation) {					
			}		
		});         
        
        AlphaAnimation editTextAlpahAnim = makeAlphaAnimation(0.5f, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
        		ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
				ANIMATION_INTERPOLATORTYPE_ACCELERATE);         
        
        if (showEditText)
			mActionBarEditText.setVisibility(View.VISIBLE);
		else 
			mActionBarEditText.setVisibility(View.GONE);
		
        
        mTitleText.startAnimation(titleTextAnimationSet);
        mCurrentPrvActionIconText.startAnimation(currentPrvActionIconTextAnimationSet);     
        
        if (showEditText)
        	mActionBarEditText.startAnimation(editTextAlpahAnim);       
	}
	
	private void cancelAnimation() {
		/*
        Animation in = mViewSwitcher.getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = mViewSwitcher.getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
        */
    }
    
    
    
    public TranslateAnimation makeSlideAnimation(float speedScale, float velocity, int fromXType, float fromXDelta, int toXType, float toXDelta) {    	
    	float delta = mViewSwitchingDelta / speedScale;
    	float width = mAnimationDistance / speedScale;	      	
    	
    	TranslateAnimation translateAnimation = new TranslateAnimation(
    			fromXType, fromXDelta, //fromXValue 
    			toXType, toXDelta,   //toXValue
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        long duration = calculateDuration(delta, width, velocity);
        translateAnimation.setDuration(duration);
        translateAnimation.setInterpolator(mHScrollInterpolator);
        
        return translateAnimation;        
	}
    
    public TranslateAnimation makeSlideAnimation(float velocity, int fromXType, float fromXDelta, int toXType, float toXDelta) {  	 	
    	
    	TranslateAnimation translateAnimation = new TranslateAnimation(
    			fromXType, fromXDelta, //fromXValue 
    			toXType, toXDelta,   //toXValue
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);
        
        long duration = calculateDuration(mView.getWidth(), mView.getWidth(), velocity);
        translateAnimation.setDuration(duration);
        translateAnimation.setInterpolator(mHScrollInterpolator);
        
        
        return translateAnimation;        
	}
	
	
	
	AnimationListener mAnimationSetOnCreateViewAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			//mActionBarMainBackText.setVisibility(View.VISIBLE);
			if (INFO) Log.i(TAG, "mAnimationSetOnCreateViewAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mAnimationSetOnCreateViewAnimlistener, end");
			mPrvActionIconTextOfCaller.setVisibility(View.GONE);
			
			mCurrentPrvActionIconText.setVisibility(View.VISIBLE);
			mTitleText.setVisibility(View.VISIBLE);
			mActionBarEditText.setVisibility(View.VISIBLE);
			
			mCurrentPrvActionIconText.startAnimation(mCurrentPrvActionIconTextEnterAnimationSet);
			mTitleText.startAnimation(mTitleEnterAnimationSet);
			mActionBarEditText.startAnimation(mEditTextEnterAlpahAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	
	AnimationListener mPsudoCollapseEventListViewIconExitAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {					
			if (INFO) Log.i(TAG, "mPsudoCollapseEventListViewIconExitAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoEventListViewCollapseIcon.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mPsudoAgendaListViewIconExitAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {					
			if (INFO) Log.i(TAG, "mPsudoAgendaListViewIconExitAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoAgendaListViewOnIcon.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mPsudoAgendaListViewOffIconExitAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {					
			if (INFO) Log.i(TAG, "mPsudoAgendaListViewOffIconExitAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoAgendaListViewOffIcon.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mPsudoSearchEventIconAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {					
			if (INFO) Log.i(TAG, "mPsudoSearchEventIconAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mPsudoSearchEventIcon.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AnimationListener mPsudoAddNewEventIconAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {					
			if (INFO) Log.i(TAG, "mPsudoAddNewEventIconAnimlistener, start");
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mPsudoAddNewEventIconAnimlistener, end");
			mPsudoAddNewEventIcon.setVisibility(View.GONE);
			//mPrvActionIconTextOfCaller.startAnimation(mDateIndicatorTextOfCallerAnimationSet);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	AlphaAnimation mPsudoCollapseEventListViewIconExitAlpahAnim;
	AlphaAnimation mPsudoAgendaListViewOnIconExitAlpahAnim;
	AlphaAnimation mPsudoAgendaListViewOffIconExitAlpahAnim;
	AlphaAnimation mPsudoSearchEventIconExitAlpahAnim;
	AlphaAnimation mPsudoAddNewEventIconExitAlpahAnim;
	AnimationSet mDateIndicatorTextOfCallerExitAnimationSet;
	AnimationSet mCurrentPrvActionIconTextEnterAnimationSet;
	AnimationSet mTitleEnterAnimationSet;
	AlphaAnimation mEditTextEnterAlpahAnim;
	
	TranslateAnimation mEventInfoActionBarEnterAnimation;
	TranslateAnimation mPsudoSearchActionBarExitAnimation;
	public void setAnimationSettingOnCreateView() {
		if (INFO) Log.i(TAG, "setAnimationSetOnCreateView");
		
		float psuodIconAlpahSpeedScale = SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY * 2;
		
		if (mCallerView != ViewType.SEARCH) {
			
			if (mCallerView == ViewType.DAY) {
				//mPsudoAgendaListViewOnIcon
				mPsudoAgendaListViewOnIconExitAlpahAnim = makeAlphaAnimation(psuodIconAlpahSpeedScale, 
		        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.3f, 0.0f,
						ANIMATION_INTERPOLATORTYPE_ACCELERATE);
				mPsudoAgendaListViewOnIconExitAlpahAnim.setAnimationListener(mPsudoAgendaListViewIconExitAnimlistener);
			}
			else if (mCallerView == ViewType.MONTH) {
				//mPsudoEventListViewCollapseIcon
				mPsudoCollapseEventListViewIconExitAlpahAnim = makeAlphaAnimation(psuodIconAlpahSpeedScale, 
		        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.3f, 0.0f,
						ANIMATION_INTERPOLATORTYPE_ACCELERATE);
				mPsudoCollapseEventListViewIconExitAlpahAnim.setAnimationListener(mPsudoCollapseEventListViewIconExitAnimlistener);
			}
			else if (mCallerView == ViewType.AGENDA) {				
				//mPsudoAgendaListViewOffIcon
				mPsudoAgendaListViewOffIconExitAlpahAnim = makeAlphaAnimation(psuodIconAlpahSpeedScale, 
		        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.3f, 0.0f,
						ANIMATION_INTERPOLATORTYPE_ACCELERATE);
				mPsudoAgendaListViewOffIconExitAlpahAnim.setAnimationListener(mPsudoAgendaListViewOffIconExitAnimlistener);				
			}
		
		
			//mPsudoSearchEventIcon
			mPsudoSearchEventIconExitAlpahAnim = makeAlphaAnimation(2, psuodIconAlpahSpeedScale, 
	        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.3f, 0.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
			mPsudoSearchEventIconExitAlpahAnim.setAnimationListener(mPsudoSearchEventIconAnimlistener);
			
			//mPsudoAddNewEventIcon
			mPsudoAddNewEventIconExitAlpahAnim = makeAlphaAnimation(2, psuodIconAlpahSpeedScale, 
	        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.3f, 0.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
			mPsudoAddNewEventIconExitAlpahAnim.setAnimationListener(mPsudoAddNewEventIconAnimlistener);
			
			// mainback_text�� caller�� date indicator text�� �����Ѵ�
			// mActionBarMainBackText�� animation���� �����Ѵ�
			mPrvActionIconTextOfCaller.setVisibility(View.VISIBLE);		
			mPrvActionIconTextOfCaller.setText(mDateIndicatorTextOfCaller);			
			mCurrentPrvActionIconText.setText(mMainPagePrvActionIconTextString);/////////////////////////////////////////////////////////////////////////
			
			mDateIndicatorTextOfCallerExitAnimationSet = new AnimationSet(false);
	        TranslateAnimation dateIndicatorTextOfCallerTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
	        		Animation.RELATIVE_TO_SELF, 0.0f,
	        		Animation.RELATIVE_TO_SELF, -0.5f);
	        mDateIndicatorTextOfCallerExitAnimationSet.addAnimation(dateIndicatorTextOfCallerTransAnim);
	        
	        AlphaAnimation dateIndicatorTextOfCallerAlpahAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
	        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.5f, 0.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	        mDateIndicatorTextOfCallerExitAnimationSet.addAnimation(dateIndicatorTextOfCallerAlpahAnim);
	        mDateIndicatorTextOfCallerExitAnimationSet.setAnimationListener(mAnimationSetOnCreateViewAnimlistener);
	        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        // mActionBarPreviousViewText animation�� ����
	        // previous_view_text�� previous_action_container�� child view �̹Ƿ�,
	        // Animation.ABSOLUTE�� from x ��ǥ�� �����Ͽ��� ������� �ʴ´�
	        // :�ϴ� RELATIVE_TO_SELF�� ��������...���Ŀ� ����� ����� ����
	        mCurrentPrvActionIconTextEnterAnimationSet = new AnimationSet(false);
	        TranslateAnimation preViewTextTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
	        		Animation.RELATIVE_TO_SELF, 1, 
	        		Animation.RELATIVE_TO_SELF, 0);
	        mCurrentPrvActionIconTextEnterAnimationSet.addAnimation(preViewTextTransAnim);
	        AlphaAnimation preViewTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
					ALPHA_TRANSPARENT_TO_OPAQUE, 0.5f, 1.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	        mCurrentPrvActionIconTextEnterAnimationSet.addAnimation(preViewTextAlphaAnim);
	        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        // mActionBarTitleText�� animation�� ����
	        mTitleEnterAnimationSet = new AnimationSet(false);
	        TranslateAnimation titleTextTransAnim = makeSlideAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY,
	        		Animation.RELATIVE_TO_SELF, 1, 
	        		Animation.RELATIVE_TO_SELF, 0);
	        mTitleEnterAnimationSet.addAnimation(titleTextTransAnim);
	        AlphaAnimation titleTextAlphaAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
					ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	        mTitleEnterAnimationSet.addAnimation(titleTextAlphaAnim);
	        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        // mActionBarEditText�� animation�� ����
	        mEditTextEnterAlpahAnim = makeAlphaAnimation(2, SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY, 
	        		ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);   
		
		}
		else /*if (mCallerView == ViewType.SEARCH)*/ {
			//mCurrentPrvActionIconText.setText(mMainPagePrvActionIconTextString);
			// mMainPagePrvActionIconTextString�� ���¸� Ȯ������
			mPsudoAgendaListViewOffIcon.setVisibility(View.GONE);
			mPsudoSearchEventIcon.setVisibility(View.GONE);
			mPsudoAddNewEventIcon.setVisibility(View.GONE);
			mPrvActionIconTextOfCaller.setVisibility(View.GONE);
			
			mCurrentPrvActionIconText.setVisibility(View.VISIBLE);
			mTitleText.setVisibility(View.VISIBLE);
			mActionBarEditText.setVisibility(View.VISIBLE);
			
			mCurrentPrvActionIconText.setText("�ڷ�");
			// mParentLayout�� �ִϸ��̼��� �����ؾ� �Ѵ�
			mEventInfoActionBarEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, 
	    			Animation.RELATIVE_TO_SELF, 0, 
	    			Animation.ABSOLUTE, 0, 
	    			Animation.ABSOLUTE, 0);
			// mPsudoSearchViewActionBar�� �ִϸ��̼��� �����ؾ� �Ѵ�
			mPsudoSearchActionBarExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
	    			Animation.RELATIVE_TO_SELF, -1.0f, 
	    			Animation.ABSOLUTE, 0, 
	    			Animation.ABSOLUTE, 0);   
			
			float animationDistance = mViewWidth;
			ActionBarSwitchInterpolator enterInterpolator = new ActionBarSwitchInterpolator(animationDistance, mEventInfoActionBarEnterAnimation);  
			ActionBarSwitchInterpolator exitInterpolator = new ActionBarSwitchInterpolator(animationDistance, mPsudoSearchActionBarExitAnimation); 
	    	
	    	mEventInfoActionBarEnterAnimation.setInterpolator(enterInterpolator);
	    	mPsudoSearchActionBarExitAnimation.setInterpolator(exitInterpolator);   	
	    	
	    	float velocity = SWITCH_EVENTINFOVIEW_PAGE_ENTRANCE_VELOCITY;
	    	long animationDuration = calculateDuration(animationDistance, mViewWidth, velocity); 
	    	mEventInfoActionBarEnterAnimation.setDuration(animationDuration);
	    	mPsudoSearchActionBarExitAnimation.setDuration(animationDuration); 
	    	
	    	//mEventInfoActionBarEnterAnimation.setAnimationListener(mEventInfoViewEnterAnimationListener);
	    	mPsudoSearchActionBarExitAnimation.setAnimationListener(new AnimationListener(){

	    		@Override
	    		public void onAnimationStart(Animation animation) {			
	    			if (INFO) Log.i(TAG, "mSearchActionBarExitAnimation AnimationListener start");			
	    		}

	    		@Override
	    		public void onAnimationEnd(Animation animation) {
	    			if (INFO) Log.i(TAG, "mSearchActionBarExitAnimation AnimationListener end");
	    			mPsudoSearchViewActionBar.setVisibility(View.GONE);
	    		}

	    		@Override
	    		public void onAnimationRepeat(Animation animation) {
	    			//if (INFO) Log.i(TAG, "mMonthViewExitAnimationListener repeat");
	    			
	    		}		
	    	});    
		}		
	}
	
	public void startActionBarEntranceAnimation() {
		if (INFO) Log.i(TAG, "startEnterAnimation");
		
		if (mCallerView != ViewType.SEARCH) {
			
			if (mCallerView == ViewType.DAY) {
				mPsudoAgendaListViewOnIcon.startAnimation(mPsudoAgendaListViewOnIconExitAlpahAnim);				
			}
			else if (mCallerView == ViewType.MONTH) {
				mPsudoEventListViewCollapseIcon.startAnimation(mPsudoCollapseEventListViewIconExitAlpahAnim);				
			}
			else if (mCallerView == ViewType.AGENDA) {				
				mPsudoAgendaListViewOffIcon.startAnimation(mPsudoAgendaListViewOffIconExitAlpahAnim);				
			}
		
			mPsudoSearchEventIcon.startAnimation(mPsudoSearchEventIconExitAlpahAnim);
			mPsudoAddNewEventIcon.startAnimation(mPsudoAddNewEventIconExitAlpahAnim);
			
			mPrvActionIconTextOfCaller.startAnimation(mDateIndicatorTextOfCallerExitAnimationSet);
		}
		else {
			
			mPsudoSearchViewActionBar.startAnimation(mPsudoSearchActionBarExitAnimation);
			mParentLayout.startAnimation(mEventInfoActionBarEnterAnimation);
		}				
	}
	
	AlphaAnimation mPsudoCollapseEventListViewIconEnterAlpahAnim;
	AlphaAnimation mPsudoAgendaListViewOnIconEnterAlpahAnim;
	AlphaAnimation mPsudoAgendaListViewOffIconEnterAlpahAnim;
	AlphaAnimation mPsudoSearchEventIconEnterAlpahAnim;
	AlphaAnimation mPsudoAddNewEventIconEnterAlpahAnim;
	AnimationSet mDateIndicatorTextOfCallerEnterAnimationSet;
	AnimationSet mCurrentPrvActionIconTextExitAnimationSet;
	AnimationSet mTitleExitAnimationSet;
	AlphaAnimation mEditTextExitAlpahAnim;
	
	TranslateAnimation mEventInfoActionBarExitAnimation;
	TranslateAnimation mPsudoSearchActionBarEnterAnimation;
	
	AnimationListener mAnimationSetExitViewAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
			//mActionBarMainBackText.setVisibility(View.VISIBLE);
			if (INFO) Log.i(TAG, "mAnimationSetExitViewAnimlistener, start");
			
			mPsudoAddNewEventIcon.setVisibility(View.VISIBLE);
	        mPsudoSearchEventIcon.setVisibility(View.VISIBLE);
	        
	        if (mCallerView == ViewType.DAY) {
	        	mPsudoAgendaListViewOnIcon.setVisibility(View.VISIBLE);				
	        	mPsudoAgendaListViewOnIcon.startAnimation(mPsudoAgendaListViewOnIconEnterAlpahAnim);
	        }
	        else if (mCallerView == ViewType.MONTH) {
	        	mPsudoEventListViewCollapseIcon.setVisibility(View.VISIBLE);	
	        	mPsudoEventListViewCollapseIcon.startAnimation(mPsudoCollapseEventListViewIconEnterAlpahAnim);
	        }
	        else if (mCallerView == ViewType.AGENDA) {
	        	mPsudoAgendaListViewOffIcon.setVisibility(View.VISIBLE);	
	        	mPsudoAgendaListViewOffIcon.startAnimation(mPsudoAgendaListViewOffIconEnterAlpahAnim);
	        }
	        else if (mCallerView == ViewType.SEARCH) {
	        	mPsudoAgendaListViewOffIcon.setVisibility(View.VISIBLE);				
	        	mPsudoAgendaListViewOffIcon.startAnimation(mPsudoAgendaListViewOffIconEnterAlpahAnim);
	        }
	        
			mPsudoSearchEventIcon.startAnimation(mPsudoSearchEventIconEnterAlpahAnim);
			mPsudoAddNewEventIcon.startAnimation(mPsudoAddNewEventIconEnterAlpahAnim);			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (INFO) Log.i(TAG, "mAnimationSetExitViewAnimlistener, end");
			
			mCurrentPrvActionIconText.setVisibility(View.GONE);
			mTitleText.setVisibility(View.GONE);
			mActionBarEditText.setVisibility(View.GONE);			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	public void setActionBarExitAnimationSet() {
		if (INFO) Log.i(TAG, "setExitAnimationSet");
		
		if (mCallerView != ViewType.SEARCH){
			if (mCallerView == ViewType.DAY) {
				mPrvActionIconTextOfCaller.setText(mDateIndicatorTextOfCaller);		
				
				//mPsudoAgendaListViewOnIcon
				mPsudoAgendaListViewOnIconEnterAlpahAnim = makeAlphaAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY, 
						ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
						ANIMATION_INTERPOLATORTYPE_ACCELERATE);				
				
			} else if (mCallerView == ViewType.MONTH) { // view type�� MONTH�� ���� MONTH�� ���� EXPAND_EVENT_LIST_MONTH_VIEW_MODE type�� ��쿡��
				                                        // EventInfoView�� ���µȴ�
				mPrvActionIconTextOfCaller.setText(mDateIndicatorTextOfCaller);		
				
				//mPsudoEventListViewCollapseIcon
				mPsudoCollapseEventListViewIconEnterAlpahAnim = makeAlphaAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY, 
						ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
						ANIMATION_INTERPOLATORTYPE_ACCELERATE);				
			} 
			else if (mCallerView == ViewType.AGENDA) { // view type�� MONTH�� ���� MONTH�� ���� EXPAND_EVENT_LIST_MONTH_VIEW_MODE type�� ��쿡��
                
				mPrvActionIconTextOfCaller.setText(mDateIndicatorTextOfCaller);		

				//
				mPsudoAgendaListViewOffIconEnterAlpahAnim = makeAlphaAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY, 
						ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
						ANIMATION_INTERPOLATORTYPE_ACCELERATE);				
			} 
			
			
			//mPsudoSearchEventIcon
			mPsudoSearchEventIconEnterAlpahAnim = makeAlphaAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY, 
					ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);		
			
			//mPsudoAddNewEventIcon
			mPsudoAddNewEventIconEnterAlpahAnim = makeAlphaAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY, 
					ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);				
					
			mDateIndicatorTextOfCallerEnterAnimationSet = new AnimationSet(false);
	        TranslateAnimation dateIndicatorTextOfCallerTransAnim = makeSlideAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY,
	        		Animation.RELATIVE_TO_SELF, -0.5f,
	        		Animation.RELATIVE_TO_SELF, 0.0f);
	        mDateIndicatorTextOfCallerEnterAnimationSet.addAnimation(dateIndicatorTextOfCallerTransAnim);
	        
	        AlphaAnimation dateIndicatorTextOfCallerAlpahAnim = makeAlphaAnimation(AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY, 
	        		ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	        mDateIndicatorTextOfCallerEnterAnimationSet.addAnimation(dateIndicatorTextOfCallerAlpahAnim);
	        
	        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        // mActionBarPreviousViewText animation�� ����
	        // previous_view_text�� previous_action_container�� child view �̹Ƿ�,
	        // Animation.ABSOLUTE�� from x ��ǥ�� �����Ͽ��� ������� �ʴ´�
	        // :�ϴ� RELATIVE_TO_SELF�� ��������...���Ŀ� ����� ����� ����        
	        //float velocity = AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY * 2;
	        float velocity = AgendaFragment.SWITCH_EVENTINFO_PAGE_EXIT_VELOCITY * 4;
	        mCurrentPrvActionIconTextExitAnimationSet = new AnimationSet(false);
	        TranslateAnimation preViewTextTransAnim = makeSlideAnimation(velocity,
	        		Animation.RELATIVE_TO_SELF, 0, 
	        		Animation.RELATIVE_TO_SELF, 1);
	        mCurrentPrvActionIconTextExitAnimationSet.addAnimation(preViewTextTransAnim);
	        AlphaAnimation preViewTextExitAlphaAnim = makeAlphaAnimation(velocity, 
	        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.4f, 0.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	        mCurrentPrvActionIconTextExitAnimationSet.addAnimation(preViewTextExitAlphaAnim);
	        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        // mActionBarTitleText�� animation�� ����
	        mTitleExitAnimationSet = new AnimationSet(false);
	        TranslateAnimation titleTextTransAnim = makeSlideAnimation(velocity,
	        		Animation.RELATIVE_TO_SELF, 0, 
	        		Animation.RELATIVE_TO_SELF, 0.5f);
	        mTitleExitAnimationSet.addAnimation(titleTextTransAnim);
	        AlphaAnimation titleTextAlphaAnim = makeAlphaAnimation(velocity, 
	        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.4f, 0.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);
	        mTitleExitAnimationSet.addAnimation(titleTextAlphaAnim);
	        mTitleExitAnimationSet.setAnimationListener(mAnimationSetExitViewAnimlistener);
	        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        // mActionBarEditText�� animation�� ����
	        mEditTextExitAlpahAnim = makeAlphaAnimation(velocity, 
	        		ALPHA_OPAQUE_TO_TRANSPARENT, 0.4f, 0.0f,
					ANIMATION_INTERPOLATORTYPE_ACCELERATE);   
		}
		else if (mCallerView == ViewType.SEARCH){
			// mPsudoSearchViewActionBar�� �ִϸ��̼��� �����ؾ� �Ѵ�
			mPsudoSearchActionBarEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f, 
	    			Animation.RELATIVE_TO_SELF, 0, 
	    			Animation.ABSOLUTE, 0, 
	    			Animation.ABSOLUTE, 0);  
			// mParentLayout�� �ִϸ��̼��� �����ؾ� �Ѵ�
			mEventInfoActionBarExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, 
	    			Animation.RELATIVE_TO_SELF, 1, 
	    			Animation.ABSOLUTE, 0, 
	    			Animation.ABSOLUTE, 0);
			 
			
			float animationDistance = mViewWidth;
			ActionBarSwitchInterpolator enterInterpolator = new ActionBarSwitchInterpolator(animationDistance, mPsudoSearchActionBarEnterAnimation);
			ActionBarSwitchInterpolator exitInterpolator = new ActionBarSwitchInterpolator(animationDistance, mEventInfoActionBarExitAnimation);  			 
	    	
			mPsudoSearchActionBarEnterAnimation.setInterpolator(enterInterpolator);   
	    	mEventInfoActionBarExitAnimation.setInterpolator(exitInterpolator);	    		
	    	
	    	float velocity = SWITCH_EVENTINFOVIEW_PAGE_EXIT_VELOCITY;
	    	long animationDuration = calculateDuration(animationDistance, mViewWidth, velocity); 
	    	mPsudoSearchActionBarEnterAnimation.setDuration(animationDuration); 
	    	mEventInfoActionBarExitAnimation.setDuration(animationDuration);	    	
	    	
	    	//mEventInfoActionBarEnterAnimation.setAnimationListener(mEventInfoViewEnterAnimationListener);
	    	mEventInfoActionBarExitAnimation.setAnimationListener(new AnimationListener(){

	    		@Override
	    		public void onAnimationStart(Animation animation) {			
	    			if (INFO) Log.i(TAG, "mEventInfoActionBarExitAnimation AnimationListener start");			
	    		}

	    		@Override
	    		public void onAnimationEnd(Animation animation) {
	    			if (INFO) Log.i(TAG, "mEventInfoActionBarExitAnimation AnimationListener end");
	    			mParentLayout.setVisibility(View.GONE);
	    		}

	    		@Override
	    		public void onAnimationRepeat(Animation animation) {	    			
	    		}		
	    	});    
		}
	}
	
	// view�� switching �Ǵ� �ð��� ��ġ�ϰ� ����!!!
	public void startExitAnimation() {
		if (mCallerView == ViewType.DAY) {
			mPrvActionIconTextOfCaller.setVisibility(View.VISIBLE);	        
			
			mCurrentPrvActionIconText.startAnimation(mCurrentPrvActionIconTextExitAnimationSet);
			mTitleText.startAnimation(mTitleExitAnimationSet);
			mActionBarEditText.startAnimation(mEditTextExitAlpahAnim);		
			
			mPrvActionIconTextOfCaller.startAnimation(mDateIndicatorTextOfCallerEnterAnimationSet);	
		}
		else if (mCallerView == ViewType.MONTH) {
			mPrvActionIconTextOfCaller.setVisibility(View.VISIBLE);	        
			
			mCurrentPrvActionIconText.startAnimation(mCurrentPrvActionIconTextExitAnimationSet);
			mTitleText.startAnimation(mTitleExitAnimationSet);
			mActionBarEditText.startAnimation(mEditTextExitAlpahAnim);		
			
			mPrvActionIconTextOfCaller.startAnimation(mDateIndicatorTextOfCallerEnterAnimationSet);	
		}
		else if (mCallerView == ViewType.AGENDA) {
			mPrvActionIconTextOfCaller.setVisibility(View.VISIBLE);	        
			
			mCurrentPrvActionIconText.startAnimation(mCurrentPrvActionIconTextExitAnimationSet);
			mTitleText.startAnimation(mTitleExitAnimationSet);
			mActionBarEditText.startAnimation(mEditTextExitAlpahAnim);		
			
			mPrvActionIconTextOfCaller.startAnimation(mDateIndicatorTextOfCallerEnterAnimationSet);	
		}
		else if (mCallerView == ViewType.SEARCH) {
			mPsudoSearchViewActionBar.setVisibility(View.VISIBLE);
			mParentLayout.startAnimation(mEventInfoActionBarExitAnimation);
			mPsudoSearchViewActionBar.startAnimation(mPsudoSearchActionBarEnterAnimation);			
		}
			
	}
	
	public static final int ALPHA_OPAQUE_TO_TRANSPARENT = 1;
	public static final int ALPHA_TRANSPARENT_TO_OPAQUE = 2;
	public static final int ANIMATION_INTERPOLATORTYPE_ACCELERATE = 1;
	public static final int ANIMATION_INTERPOLATORTYPE_DECELERATE = 2;
	public AlphaAnimation makeAlphaAnimation(float speedScale, float velocity, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {    	
    	float delta = mViewSwitchingDelta / speedScale;
    	float width = mAnimationDistance / speedScale;    	
    	long duration = calculateDuration(delta, width, velocity);
    	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);        
        
        alphaAnimation.setDuration(duration);
        if (InterpolatorType == ANIMATION_INTERPOLATORTYPE_ACCELERATE)
        	alphaAnimation.setInterpolator(new AccelerateInterpolator());
        else
        	alphaAnimation.setInterpolator(new DecelerateInterpolator());
        
        return alphaAnimation;        
	}	   
	
	public AlphaAnimation makeAlphaAnimation(float velocity, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {    	
		
    	long duration = calculateDuration(mView.getWidth(), mView.getWidth(), velocity);    	
    	
	    AlphaAnimation alphaAnimation = null;
	    if (alphaType == ALPHA_TRANSPARENT_TO_OPAQUE)	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
	    else	    	
	    	alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);        
        
        alphaAnimation.setDuration(duration);
        if (InterpolatorType == ANIMATION_INTERPOLATORTYPE_ACCELERATE)
        	alphaAnimation.setInterpolator(new AccelerateInterpolator());
        else
        	alphaAnimation.setInterpolator(new DecelerateInterpolator());
        
        return alphaAnimation;        
	}	   
    
	private boolean match(int pixel)
	{        
		return Color.red(pixel) == ACTION_PREVIOUS_ITEM_BG_COLOR[0] &&
		Color.green(pixel) == ACTION_PREVIOUS_ITEM_BG_COLOR[1] &&
		Color.blue(pixel) == ACTION_PREVIOUS_ITEM_BG_COLOR[2];
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
    
    public long calculateDuration(float delta, float width, float velocity) {
        
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        float f1 = MINIMUM_SNAP_VELOCITY * 2;
        velocity = Math.max(f1, velocity);
        
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        
        return duration;
    }
        
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }
    
	private class ActionBarSwitchInterpolator implements Interpolator {
    	float mAnimationDistance;
    	Animation mAnimation;
    	
        public ActionBarSwitchInterpolator(float animationDistance, Animation animation) {
        	mAnimationDistance = animationDistance;
        	mAnimation = animation;
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
        
        private void cancelAnimation() {            
        	mAnimation.scaleCurrentDuration(0);            
        }
    }    
}
