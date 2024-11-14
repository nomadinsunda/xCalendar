package com.intheeast.acalendar;



import java.util.Calendar;
import java.util.GregorianCalendar;

import com.intheeast.acalendar.CalendarController.EventInfo;
import com.intheeast.acalendar.CalendarController.EventType;
import com.intheeast.acalendar.CalendarController.ViewType;

import android.animation.Animator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
//import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.TextureView.SurfaceTextureListener;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchViewUpperActionBarFragment extends Fragment implements CalendarController.EventHandler{

	private static final String TAG = "SearchViewUpperActionBarFragment";	
	private static final boolean INFO = true;
	
	protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";

    protected static final String BUNDLE_KEY_RESTORE_SEARCH_QUERY =
        "key_restore_search_query";
	
	Activity mActivity;
	Context mContext;
	
	RelativeLayout mFrameLayout;
	RelativeLayout mEditTextContainer;
    CustomEditText mActionBarEditText;
    Button mCrossButton;
    TextView mActionBarCancelText;
    Bitmap mCalendarActionBarBitmap;
    
    TextureView mEditTextBlurredBGFactory; //edittext_bg_textureview
    EditTextBlurredBGSurfaceTextureListener mEditTextBlurredBGSurfaceTextureListener;   
    
    int mWindowWidth;
    int mActionBarHeight;
    int mTextureViewWidth;
    int mTextureViewHeight;
    
    Bitmap mAlphaedCalendarActionBarBitmap;
    
	private static CalendarController mController;
	private String mQuery;
	
	enum ViewState {
		Initialized,		
        RequestedSwitch,   // another activity comes into the foreground or phone endter standby status
        Recoveryed,
        Finished
	}
	
	public ViewState mViewState;
	
	public SearchViewUpperActionBarFragment() {
		
	}
	
	/*
	public SearchViewUpperActionBarFragment(CalendarController controller) {
		mController = controller;
	}	
	*/
	
	public void setSwitchViewState() {
		mViewState = ViewState.RequestedSwitch;
	}
		
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		
		if (INFO) Log.i(TAG, "onAttach");
		mActivity = activity;
		mContext = activity.getApplicationContext();
	}	
	
	InputMethodManager mIMM;
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (INFO) Log.i(TAG, "onCreate");     
        
        //mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        mIMM = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);        
        
        mViewState = ViewState.Initialized;
        
        mController = CalendarController.getInstance(mActivity);
        ECalendarApplication app = (ECalendarApplication) mActivity.getApplication();
        mCalendarActionBarBitmap = app.getCalendarActionBarBitmap();
        
        Rect rectgle = new Rect(); 
    	Window window = mActivity.getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	
    	mWindowWidth = rectgle.right - rectgle.left;    	
        mActionBarHeight =  (int) getResources().getDimension(R.dimen.calendar_view_upper_actionbar_height); //50dip
        mTextureViewWidth = (int) (mWindowWidth * 0.7f);
        mTextureViewHeight = (int) (mActionBarHeight * 0.6f);
        int x = (mWindowWidth - mTextureViewWidth) / 2;
        int y = (mActionBarHeight - mTextureViewHeight) / 2;
        Bitmap copyBm = Bitmap.createBitmap(mCalendarActionBarBitmap, x, y, mTextureViewWidth, mTextureViewHeight);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), copyBm);
        bitmapDrawable.setAlpha(127);        
        mAlphaedCalendarActionBarBitmap = bitmapDrawable.getBitmap();
        
        mQuery = new String();
    }
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		if (INFO) Log.i(TAG, "onCreateView");
			
		// savedInstanceState != null �� ��찡 �߻����� �ʰ� �ִ�
		// ���� onSaveInstanceState�� ȣ����� �ʰ� �ִ�...������ ����???
		// : activity�� onSaveInstanceState�� ȣ��� ���� null�� �ƴϴ�
		/*if (savedInstanceState != null) {
			if (INFO) Log.i(TAG, "onCreateView:fragment is being re-constructed ");			
		}*/
				
		if (mViewState == ViewState.RequestedSwitch) {
			mViewState = ViewState.Recoveryed;
		}
		
		if (mViewState == ViewState.Initialized) {
			mFrameLayout = (RelativeLayout) inflater.inflate(R.layout.searchviews_upper_actionbar, null);
	        		
	        initActionBarLayout(mFrameLayout);
	        makeActionBarEnterAnimation();
		}
        return mFrameLayout;
	}		
	
	@Override
	public void onStart() {
		super.onStart();		
		
	}

	@Override
	public void onResume() {		
		super.onResume();
		if (INFO) Log.i(TAG, "onResume");
		/*
                     �θ� edittext_container�� �Ӽ���        
		android:focusable="true"
		android:focusableInTouchMode="true"
		�� ���� �������� �ʰ� �Ʒ� �ڵ常 �ܵ����� ������ ���
		ime�� �˾����� �ʴ´�
		:-> �� �̻�  �θ� edittext_container�� �Ӽ���        
		android:focusable="true"
		android:focusableInTouchMode="true"
		�� �������� �ʴ´�
		 */
		//mActionBarEditText.setFocusable(false);
		//mActionBarEditText.setFocusableInTouchMode(false);
	}

	@Override
	public void onPause() {
		if (INFO) Log.i(TAG, "onPause");
		
		mActionBarEditText.setFocusable(false);
		mActionBarEditText.setFocusableInTouchMode(false);
		
		super.onPause();
	}

	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (INFO) Log.i(TAG, "onSaveInstanceState");
        
        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putString(BUNDLE_KEY_RESTORE_SEARCH_QUERY, mQuery);
    }
	
	@Override
	public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
		if (mViewState == ViewState.Initialized) {
			if (enter) {
				if (INFO) Log.i(TAG, "onCreateAnimator : enter");
				// �츮�� ���� onCreateAnimator�� enter animation�� ������ ������ �����޴�? ���ҷ� ��븸 �Ѵ�
				// :���⼭ � animation�� start ��Ű�� �ʴ´�				
				mFrameLayout.startAnimation(mActionBarEnterTransAnim); 
			}
		}
		
		return super.onCreateAnimator(transit, enter, nextAnim);
	}
	

	RelativeLayout mEditTextBGContainer;
	ImageView mEditTextBlurredBG;
	public void initActionBarLayout(RelativeLayout view) { 
		mEditTextBGContainer = (RelativeLayout) view.findViewById(R.id.blurring_container);
		RelativeLayout.LayoutParams textureViewParams = new RelativeLayout.LayoutParams(mTextureViewWidth, mTextureViewHeight);
        textureViewParams.addRule(RelativeLayout.CENTER_IN_PARENT); 
        mEditTextBGContainer.setLayoutParams(textureViewParams);
        
        mEditTextBlurredBGFactory = (TextureView) mEditTextBGContainer.findViewById(R.id.edittext_blurred_bg_factory);
        mEditTextBlurredBG = (ImageView) mEditTextBGContainer.findViewById(R.id.edittext_blurred_bg);
        mEditTextBlurredBG.setVisibility(View.GONE);
		
        mEditTextContainer = (RelativeLayout) view.findViewById(R.id.edittext_container);
        int editTextWidth = mTextureViewWidth;
        int editTextHeight = mTextureViewHeight;
        RelativeLayout.LayoutParams editTextContainerParams = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
        editTextContainerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mEditTextContainer.setLayoutParams(editTextContainerParams);
        
        mActionBarEditText = (CustomEditText) mEditTextContainer.findViewById(R.id.search_edittext);
        mCrossButton = (Button) mEditTextContainer.findViewById(R.id.cross_button);
        mActionBarEditText.setCrossButton(mCrossButton);
        mActionBarEditText.setFocusable(false);
		mActionBarEditText.setFocusableInTouchMode(false);
        
        mCrossButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if (INFO) Log.i(TAG, "mCrossButton : onClick");
            	mActionBarEditText.setText("");
            	
            	EventInfo searchEventInfo = new EventInfo();
                searchEventInfo.eventType = EventType.SEARCH_LISTVIEW_EMPTY;
                searchEventInfo.viewType = ViewType.AGENDA;         
                
            	mController.sendEvent(SearchViewUpperActionBarFragment.this, searchEventInfo);            	
            }
        });
        
        mActionBarCancelText = (TextView) view.findViewById(R.id.search_cancel_textview);          
               
        mEditTextBlurredBGSurfaceTextureListener = new EditTextBlurredBGSurfaceTextureListener(mEditTextBlurredBGFactory, mActivity, mAlphaedCalendarActionBarBitmap); 
        mEditTextBlurredBGFactory.setSurfaceTextureListener(mEditTextBlurredBGSurfaceTextureListener);              
                
        mActionBarEditText.addTextChangedListener(new TextWatcher() {          
    	 
    	   public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    		   if (INFO) Log.i(TAG, "beforeTextChanged:[s]=" + s.toString());
    		   if (INFO) Log.i(TAG, "beforeTextChanged :[start]=" + String.valueOf(start));
    		   if (INFO) Log.i(TAG, "beforeTextChanged :[count]=" + String.valueOf(count));
    		   if (INFO) Log.i(TAG, "beforeTextChanged :[after]=" + String.valueOf(after));
    	   }
    	 
    	   public void onTextChanged(CharSequence s, int start, int before, int count) {
    		   if (INFO) Log.i(TAG, "onTextChanged:[s]= " + s.toString());
    		   if (INFO) Log.i(TAG, "onTextChanged :[start]=" + String.valueOf(start));
    		   if (INFO) Log.i(TAG, "onTextChanged :[before]=" + String.valueOf(before));
    		   if (INFO) Log.i(TAG, "onTextChanged :[count]=" + String.valueOf(count));
    	   }
    	   
    	   public void afterTextChanged(Editable s) {
    		   String query = new String(s.toString());
    		   if (query.isEmpty()) {
    			   if (INFO) Log.i(TAG, "afterTextChanged : EMPTY!!!"); 
    		   }
    		   else {
	        	   if (INFO) Log.i(TAG, "afterTextChanged : " + s.toString()); 
	        	   	     
	        	   search(query, null);	        	   
    		   }
    	   }
        });
        
        mActionBarEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {        	
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					if (INFO) Log.i(TAG, "mActionBarEditText : onEditorAction");
					
					search(mActionBarEditText.getText().toString(), null);
					return true; // �ϴ� �׽�Ʈ�� ���ؼ� �ּ�ó��
				}
				return false;
			}
        });   
        
	}
		
	
	boolean mReplacedAgendaFrag = false;
	private void search(String searchQuery, Calendar goToTime) {
    			
    	if (mQuery.compareTo(searchQuery) == 0) {
    		return;
    	}
    	
        EventInfo searchEventInfo = new EventInfo();
        searchEventInfo.eventType = EventType.SEARCH;
        searchEventInfo.query = searchQuery;
        searchEventInfo.viewType = ViewType.AGENDA;
        if (goToTime != null) {
            //searchEventInfo.startTime = goToTime;
            searchEventInfo.startTime = GregorianCalendar.getInstance(goToTime.getTimeZone());
            searchEventInfo.startTime.setFirstDayOfWeek(goToTime.getFirstDayOfWeek());
            searchEventInfo.startTime.setTimeInMillis(goToTime.getTimeInMillis());
        }
        
        mController.sendEvent(this, searchEventInfo);
        mQuery = searchQuery;                
    }	
	
	public Bitmap getEditTextViewBlurredBG() {
		return mEditTextBlurredBGSurfaceTextureListener.getBlurredBitmap();
	}
	
	public String getSearchQuery() {
		return mQuery;
	}
	
	AnimationListener mActionBarEnterTransAnimlistener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {		
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			InputMethodManager mgr = (InputMethodManager)SearchViewUpperActionBarFragment.this.mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);    	   
	    	mgr.showSoftInput(SearchViewUpperActionBarFragment.this.mActivity.getCurrentFocus(), InputMethodManager.SHOW_FORCED);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}		
	};
	
	
	private SearchActionBarInterpolator mActionBarEnterInterpolator;
    TranslateAnimation mActionBarEnterTransAnim;
    public void makeActionBarEnterAnimation() {
    	mActionBarEnterInterpolator = new SearchActionBarInterpolator();
    	
    	mActionBarEnterTransAnim = new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0, 
    			Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0);
    	
    	long duration = calculateDuration(mWindowWidth, mWindowWidth, ACTIONBAR_ENTER_VELOCITY);
    	mActionBarEnterTransAnim.setDuration(duration);
    	mActionBarEnterTransAnim.setInterpolator(mActionBarEnterInterpolator);
    	mActionBarEnterTransAnim.setAnimationListener(mActionBarEnterTransAnimlistener);
    }
	
	
	private float mAnimationDistance = 0;
    private static final int MINIMUM_VELOCITY = 2200;
    private static final int ACTIONBAR_ENTER_VELOCITY = MINIMUM_VELOCITY * 2;
    
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
        float f1 = MINIMUM_VELOCITY * 2;
        velocity = Math.max(f1, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        /*if (INFO) {
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
	
	
	
	private class SearchActionBarInterpolator implements Interpolator {
        public SearchActionBarInterpolator() {
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
	
	
	public static final Config SEARCH_EDITTEXT_BG_BLURRING_PIXEL_FORMAT_BITMAP_CONFIG = Config.RGB_565;
	private class EditTextBlurredBGSurfaceTextureListener implements SurfaceTextureListener {
		
		TextureView mMaster;
    	Activity mActivity;    		
    	EditTextBGRenderThread mRender;
    	Bitmap mBGSource;
    	Bitmap mBlurredBGSource;
    	boolean mTextureAvailable;    	
    	
    	public EditTextBlurredBGSurfaceTextureListener(TextureView master, Activity activity, Bitmap source) {    		
    		mMaster = master;
    		mActivity = activity;  
    		mBGSource = source;
    		mTextureAvailable = false;
    	}
    	
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {    
        	if (INFO) Log.i(TAG, "EditTextBGSurfaceTextureListener : onSurfaceTextureAvailable");
        	
			
    		mRender = new EditTextBGRenderThread(mMaster.getContext(), mActivity, mBGSource, surface, width, height);
    		mRender.setDaemon(true);
    		mRender.start();        	
        	
        	mTextureAvailable = true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        	if (INFO) Log.i(TAG, "EditTextBGSurfaceTextureListener : onSurfaceTextureSizeChanged");
        }

        // Invoked when the specified SurfaceTexture is about to be destroyed. 
        // If returns true, no rendering should happen inside the surface texture after this method is invoked. 
        // If returns false, the client needs to call SurfaceTexture.release().
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        	if (INFO) Log.i(TAG, "EditTextBGSurfaceTextureListener : onSurfaceTextureDestroyed"); 	
        	//mRender.stopRenderThread();
			// �ǽð� ������ preferene�� �����ؼ� ����/�����ϴ� ���� ���???
			//mRenderThreadHandler = null;
			//Log.i("tag", "-EditTextBGSurfaceTextureListener : onSurfaceTextureDestroyed");
            //return false;
			return true;
        }
        
		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			if (INFO) Log.i(TAG, "EditTextBGSurfaceTextureListener : onSurfaceTextureUpdated");	
			/*
			 Enabling the drawing cache is similar to setting a layer when hardware acceleration is turned off. 
			 When hardware acceleration is turned on, 
			 enabling the drawing cache has no effect on rendering because the system uses a different mechanism for acceleration which ignores the flag. 
			 If you want to use a Bitmap for the view, even when hardware acceleration is enabled, 
			 see setLayerType(int, android.graphics.Paint) for information on how to enable software and hardware layers.
			 -->Sorry, by its nature a SurfaceView does not draw in the normal view hierarchy update system, so it won't be drawn in that
			 
			mMaster.setDrawingCacheEnabled(true);
			mMaster.buildDrawingCache();
			mBlurredBGSource = mMaster.getDrawingCache();
			 */			
			int bitmapWidth = mRender.getBufferWidth();
			int bitmapHeight = mRender.getBufferHeight();
			mBlurredBGSource = Bitmap.createBitmap(bitmapWidth, bitmapHeight, SEARCH_EDITTEXT_BG_BLURRING_PIXEL_FORMAT_BITMAP_CONFIG);
			
			//ByteBuffer testBuffer = mRender.getBlurredPixels();
			
			mBlurredBGSource.copyPixelsFromBuffer(mRender.getBlurredPixels());
			
			mEditTextBlurredBG.setVisibility(View.VISIBLE);		
			mEditTextBlurredBG.setImageBitmap(mBlurredBGSource);	
			
			mEditTextBlurredBGFactory.setVisibility(View.GONE);
			
		} 
		
		public Bitmap getBlurredBitmap() {
			return mBlurredBGSource;
		}
    }
	@Override
	public long getSupportedEventTypes() {
		return EventType.GOT_ALL_BOTHENDS_EVENTS;
	}

	@Override
	public void handleEvent(EventInfo event) {
		if (event.eventType == EventType.GOT_ALL_BOTHENDS_EVENTS) {
			if (INFO) Log.i(TAG, "handleEvent : GOT_ALL_BOTHENDS_EVENTS"); 
			// ���⼭ android:textCursorDrawable="@drawable/red_color_edittext_cursor"�� blinking �ǵ��� �ؾ� �Ѵ�		
			//mEditTextContainer.setFocusable(false);
			//mEditTextContainer.setFocusableInTouchMode(false);  
			
			mActionBarEditText.setFocusable(true);
	        mActionBarEditText.setFocusableInTouchMode(true);  
	        if(mActionBarEditText.requestFocus()) {
	        	if (INFO) Log.i(TAG, "handleEvent : ok got focus"); 
	        		        	
	        	mIMM.showSoftInput(mActionBarEditText, InputMethodManager.SHOW_IMPLICIT);
	        	//mIMM.hideSoftInputFromWindow(windowToken, flags, resultReceiver);	        	
	        }
	        //mActionBarEditText.setCursorVisible(true);
	        mActionBarEditText.setSelection(mActionBarEditText.getText().toString().length());
		}		
	}

	@Override
	public void eventsChanged() {
		// TODO Auto-generated method stub
		
	}
}
