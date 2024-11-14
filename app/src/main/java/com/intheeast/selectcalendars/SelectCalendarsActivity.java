package com.intheeast.selectcalendars;


import com.intheeast.anim.ITEAnimInterpolator;
import com.intheeast.anim.ITEAnimationUtils;
import com.intheeast.acalendar.CalendarController;
import com.intheeast.acalendar.ECalendarApplication;
import com.intheeast.acalendar.Utils;
import com.intheeast.acalendar.R;
import com.intheeast.settings.SettingsFragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
//import com.intheeast.ecalendar.CalendarController.EventType;
import android.provider.CalendarContract.Calendars;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SelectCalendarsActivity extends Activity implements SelectCalendarsFragment.OnMainFragmentInitCompletionListener {

	private static final String TAG = SelectCalendarsActivity.class.getSimpleName();
	private static boolean INFO = true;
	
	private static final int MINIMUM_SNAP_VELOCITY = 2200; 
	public static final float ENTRY_ANIMATION_VELOCITY = MINIMUM_SNAP_VELOCITY * 3; 
	public static final float EXIT_ANIMATION_VELOCITY = MINIMUM_SNAP_VELOCITY * 4; 
	
	
	Bitmap mCallerEntireRegionBitmap;
	
	ImageView mCallerActivityBitmapContainer;	
	LinearLayout mSelectCalendarsRealLayout;
	FrameLayout mSelectCalendarsActionbar;
	FrameLayout mSelectCalendarsMainPane;
	
	TranslateAnimation mEnterTranslateAnim;
	TranslateAnimation mExitTranslateAnim;

	int mWindowHeight;
	
	// ���� ���� ū �������� SelectCalendarsActivity�� launch �� ��
	// ���� Ķ�����鿡 ���� ������ ���ٴ� ���̴�
	// �̰��� enter animation �� ������ ���������� ��Ÿ����
	// �ذ� �����...
	// eCalendar ���� �� Ķ�����鿡 ���� ������ ã�� ���� ���̸�
	// �� activity�� ������ �� events changed�� �߻��� ��
	// Ķ�����鿡 ���� ������ �����ؾ� �Ѵ�
	SelectCalendarsViewActionBarFragment mUpperActionBarFrag;
	SelectCalendarsFragment mMainFragment;
	private CalendarController mController;
	
	
	private static final String ACCOUNT_UNIQUE_KEY = "ACCOUNT_KEY";
	private MatrixCursor mAccountsCursor = null;	
	private static final String[] PROJECTION = new String[] {
        Calendars._ID,
        Calendars.ACCOUNT_TYPE,
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE + " || " + Calendars.ACCOUNT_NAME + " AS " +
                ACCOUNT_UNIQUE_KEY,
    };
	
	
	/*
	private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
          mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
        }
    };
    */
	
	public void prepareEntranceExitAnimation() {
		ECalendarApplication app = (ECalendarApplication) getApplication();
		mCallerEntireRegionBitmap = app.getCalendarEntireRegionBitmap();
		
		// ���� �ð��밡 ������ �Ѿ�� ���� ���� bitmap�̶��,,,
		// exit �� old bitmap���� ����...
		// ������[Ķ������ ��Ī�ϴ� ���� �ƴ϶� �۾�������]ó��... 
		mCallerActivityBitmapContainer = (ImageView) findViewById(R.id.caller_activity_bitmap_container);
		mCallerActivityBitmapContainer.setImageBitmap(mCallerEntireRegionBitmap);
		
		mSelectCalendarsRealLayout = (LinearLayout) findViewById(R.id.selectcalendars_real_layout);		
		mSelectCalendarsActionbar = (FrameLayout) mSelectCalendarsRealLayout.findViewById(R.id.selectcalendars_actionbar);
		mSelectCalendarsMainPane = (FrameLayout) mSelectCalendarsRealLayout.findViewById(R.id.selectcalendars_main_pane);
		
		/*
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);    	
		
		CommonRelativeLayoutUpperActionBar actionbar = (CommonRelativeLayoutUpperActionBar) inflater.inflate(R.layout.selectcalendars_actionbar, null, false);    		
		mSelectCalendarsActionbar.addView(actionbar);
		
		RelativeLayout mainview = (RelativeLayout) inflater.inflate(R.layout.selectcalendars_mainview_layout, null, false);
		
		
		mSelectCalendarsMainPane.addView(mainview);
		*/
		
		mSelectCalendarsActionbar.setVisibility(View.INVISIBLE);
		mSelectCalendarsMainPane.setVisibility(View.INVISIBLE);
		mSelectCalendarsRealLayout.setVisibility(View.INVISIBLE);
		
		makeEntranceAnimation();
		makeExitAnimation();
	}
    
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		calcFrameLayoutHeight();
		
		// ��ǻ� ���Ǵ� ���� ����!!!!!!!!!!!!!!!!
		mController = CalendarController.getInstance(this);
		
		setContentView(R.layout.selectcalendars_layout);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  //���� ����
		
		Utils.startCalendarMetafeedSync(null);
		
		prepareEntranceExitAnimation();
		
	}
	
	private void initFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        mMainFragment = new SelectCalendarsFragment(this, getApplicationContext(), mAccountsCursor);
        mMainFragment.setOnMainFragmentInitCompletionListener(this);
        
        mUpperActionBarFrag = new SelectCalendarsViewActionBarFragment(
        		SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE,
        		mMainFragment);  
        
        ft.replace(R.id.selectcalendars_actionbar, mUpperActionBarFrag);        
        
        ft.replace(R.id.selectcalendars_main_pane, mMainFragment);        
        
        ft.commit();        
    }

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
								
		new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                mAccountsCursor = Utils.matrixCursorFromCursor(cursor); 
                
                initFragments();
            }
        }.startQuery(0, null, Calendars.CONTENT_URI, PROJECTION,
                "1) GROUP BY (" + ACCOUNT_UNIQUE_KEY, //Cheap hack to make WHERE a GROUP BY query
                null ,
                Calendars.ACCOUNT_NAME); // account�� group���� �ؼ� �� �ϳ��� Ķ���� ���� Result�� �����Ѵ�
        
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
	   super.onWindowFocusChanged(hasFocus);
	   
	   if (INFO) Log.i(TAG, "onWindowFocusChanged");	   
	   
	}
	
	@Override
	public void onMainFragmentInitCompletion() {
		if (INFO) Log.i(TAG, "onMainFragmentInitCompletion");
		mSelectCalendarsRealLayout.startAnimation(mEnterTranslateAnim);		
	}
	

	@Override
	protected void onPause() {	
		// finish �� �߻��ϴ� animation�� ���ϱ� ���ؼ�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// 1st/2nd para : Use 0 for no animation.
		overridePendingTransition(0, 0); /////////////////////////////////////////////////////////////////////////////////////////////
		super.onPause();
		
		//getContentResolver().unregisterContentObserver(mObserver);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	@Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (INFO) Log.i(TAG, "onKeyDown : KEYCODE_BACK");
    		
    		if (mMainFragment.mViewState == SelectCalendarsFragment.MAIN_VIEW_FRAGMENT_STATE) {
    			goodBye();
    			
	    		return true;
    		}
    	}
    	
		return super.onKeyDown(keyCode, event);    	
    }
	
	public void goodBye() {
		mCallerActivityBitmapContainer.setVisibility(View.VISIBLE);
		mSelectCalendarsRealLayout.startAnimation(mExitTranslateAnim);		
	}
	
	
	AnimationListener mEntranceAnimlistener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			mSelectCalendarsRealLayout.setVisibility(View.VISIBLE);	
			mSelectCalendarsActionbar.setVisibility(View.VISIBLE);
			mSelectCalendarsMainPane.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mCallerActivityBitmapContainer.setVisibility(View.GONE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
	AnimationListener mExitAnimlistener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mSelectCalendarsRealLayout.setVisibility(View.GONE);
			finishActivity();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	};
	
	private void finishActivity() {
		finish();
	}
	
	public void makeEntranceAnimation() {
		float velocity = ENTRY_ANIMATION_VELOCITY;
		float enterAnimationDistance = mWindowHeight;
		long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mWindowHeight, MINIMUM_SNAP_VELOCITY, velocity); 
		
		mEnterTranslateAnim = new TranslateAnimation(
				Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.RELATIVE_TO_SELF, 1, 
    			Animation.RELATIVE_TO_SELF, 0);	
		
		ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mEnterTranslateAnim);  
		
		mEnterTranslateAnim.setDuration(enterAnimationDuration);
		mEnterTranslateAnim.setInterpolator(enterInterpolator);		
		mEnterTranslateAnim.setAnimationListener(mEntranceAnimlistener);
	}
	
	public void makeExitAnimation() {
		float velocity = EXIT_ANIMATION_VELOCITY;
		float enterAnimationDistance = mWindowHeight;
		long enterAnimationDuration = ITEAnimationUtils.calculateDuration(enterAnimationDistance, mWindowHeight, MINIMUM_SNAP_VELOCITY, velocity); 
		
		mExitTranslateAnim = new TranslateAnimation(
				Animation.ABSOLUTE, 0, 
    			Animation.ABSOLUTE, 0, 
    			Animation.RELATIVE_TO_SELF, 0, 
    			Animation.RELATIVE_TO_SELF, 1);	
		
		ITEAnimInterpolator enterInterpolator = new ITEAnimInterpolator(enterAnimationDistance, mExitTranslateAnim);  
		
		mExitTranslateAnim.setDuration(enterAnimationDuration);
		mExitTranslateAnim.setInterpolator(enterInterpolator);
		mExitTranslateAnim.setAnimationListener(mExitAnimlistener);
	}
	
	public void calcFrameLayoutHeight() {
    	int deviceHeight = getResources().getDisplayMetrics().heightPixels;
		
		Rect rectgle = new Rect(); 
    	Window window = getWindow(); 
    	window.getDecorView().getWindowVisibleDisplayFrame(rectgle); 
    	int StatusBarHeight = Utils.getSharedPreference(getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
    	if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(getApplicationContext(), SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }
    	
    	mWindowHeight = deviceHeight - StatusBarHeight;
	}	
	
	public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}

	
	
	
	
}
