package com.intheeast.alerts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.intheeast.alerts.GlobalDismissManager.AlarmId;
import com.intheeast.acalendar.AsyncQueryService;
import com.intheeast.acalendar.CentralActivity;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.CommonRoundedCornerLinearLayout;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Events;
import android.text.Spannable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
//import android.text.format.Time;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AloneAlertActivity extends Activity  {

	private static final String TAG = "AloneAlertActivity";
    private static final boolean INFO = true;

    private static final String GEO_PREFIX = "geo:";
    
    public static final int ALERT_MAIN_DIALOGBOX_FRONT_TYPE = 1;
    public static final int ALERT_MAIN_DIALOGBOX_BACKWARD_TYPE = 2;
    
    public static final float ALERT_ACTIVITY_DIALOGBOX_WIDTH_RATIO = 0.85f;
    
    private class AlertEventInfo {
    	boolean Alerted = false;
    	long eventId;
    	String eventName;
        String location;
        long startMillis;
        long endMillis;
        boolean allDay;
        
        public AlertEventInfo(long eventId, String eventName, String location, long startMillis, long endMillis, boolean allDay) {
        	this.eventId = eventId;
        	this.eventName = eventName;
        	this.location = location;
        	this.startMillis = startMillis;
        	this.endMillis = endMillis;
        	this.allDay = allDay;
        }
    }
    
    private class AlertMainDialogBoxComponent {
    	//int mAlertMainDialogBoxType = 0;
    	CommonRoundedCornerLinearLayout mMainAlertDialogBox;
        
        LinearLayout mMainAlertDBEventInfoItemContainer;    
        TextView mMainAlertDBEventTitleItem;
        TextView mMainAlertDBWhenItem;    
        TextView mMainAlertDBWhereItem; 
        
        LinearLayout mMainAlertDBButtonContainer;  
        TextView mMainAlertDBCloseButton;
        TextView mMainAlertDBOptionButton;
        
    	public AlertMainDialogBoxComponent() {
    		
    	}
    }
    
    
    private class AlertOptionDialogBoxComponent {
    	
    	CommonRoundedCornerLinearLayout mOptionAlertDialogBox;
        
        LinearLayout mOptionAlertDBEventInfoItemContainer;    
        TextView mOptionAlertDBEventTitleItem;
        TextView mOptionAlertDBWhenItem;    
        TextView mOptionAlertDBWhereItem; 
                
        TextView mOptionAlertDBViewEventInfoButton;
        TextView mOptionAlertDBSnoozeAlarmButton;
        TextView mOptionAlertDBDirectionsButton;
        TextView mOptionAlertDBCloseButton;
                
        View  mOptionAlertDBDirectionsUpperLine;        
        
    	public AlertOptionDialogBoxComponent() {
    		
    	}
    }
    
    Context mContext; 
    
    RelativeLayout mFrameLayout;
    
    AlertMainDialogBoxComponent mCurrentAlertMainDialogBoxComponent = null;
    AlertMainDialogBoxComponent mAlertMainDialogBoxComponentA = null;
    AlertMainDialogBoxComponent mAlertMainDialogBoxComponentB = null;
    AlertOptionDialogBoxComponent mAlertOptionDialogBoxComponent = null;
    
    ArrayList<AlertEventInfo> mFiredEventInfo;
    
    int mDialogBoxWidth;    
    
    private static final String[] PROJECTION = new String[] {
        CalendarAlerts._ID,              // 0
        CalendarAlerts.TITLE,            // 1
        CalendarAlerts.EVENT_LOCATION,   // 2
        CalendarAlerts.ALL_DAY,          // 3
        CalendarAlerts.BEGIN,            // 4
        CalendarAlerts.END,              // 5
        CalendarAlerts.EVENT_ID,         // 6
        CalendarAlerts.CALENDAR_COLOR,   // 7
        CalendarAlerts.RRULE,            // 8
        CalendarAlerts.HAS_ALARM,        // 9
        CalendarAlerts.STATE,            // 10
        CalendarAlerts.ALARM_TIME,       // 11
    };

    public static final int INDEX_ROW_ID = 0;
    public static final int INDEX_TITLE = 1;
    public static final int INDEX_EVENT_LOCATION = 2;
    public static final int INDEX_ALL_DAY = 3;
    public static final int INDEX_BEGIN = 4;
    public static final int INDEX_END = 5;
    public static final int INDEX_EVENT_ID = 6;
    public static final int INDEX_COLOR = 7;
    public static final int INDEX_RRULE = 8;
    public static final int INDEX_HAS_ALARM = 9;
    public static final int INDEX_STATE = 10;
    public static final int INDEX_ALARM_TIME = 11;
    
    private static final String SELECTION = "(" + CalendarAlerts.STATE + "=? OR "
            + CalendarAlerts.STATE + "=?) AND " + CalendarAlerts.ALARM_TIME + "=";
    private static final String[] SELECTIONARG = new String[] {
    	Integer.toString(CalendarAlerts.STATE_FIRED),
        Integer.toString(CalendarAlerts.STATE_SCHEDULED)
    };

    //private AlertAdapter mAdapter;
    //private ListView mListView;
    private QueryHandler mQueryHandler;
    private Cursor mCursor;

    private void dismissFiredAlarms() {
        ContentValues values = new ContentValues(1 /* size */);
        values.put(PROJECTION[INDEX_STATE], CalendarAlerts.STATE_DISMISSED);
        String selection = CalendarAlerts.STATE + "=" + CalendarAlerts.STATE_FIRED;
        mQueryHandler.startUpdate(0, null, CalendarAlerts.CONTENT_URI, values,
                selection, null /* selectionArgs */, Utils.UNDO_DELAY);

        if (mCursor == null) {
            Log.e(TAG, "Unable to globally dismiss all notifications because cursor was null.");
            return;
        }
        if (mCursor.isClosed()) {
            Log.e(TAG, "Unable to globally dismiss all notifications because cursor was closed.");
            return;
        }
        if (!mCursor.moveToFirst()) {
            Log.e(TAG, "Unable to globally dismiss all notifications because cursor was empty.");
            return;
        }

        List<AlarmId> alarmIds = new LinkedList<AlarmId>();
        do {
            long eventId = mCursor.getLong(INDEX_EVENT_ID);
            long eventStart = mCursor.getLong(INDEX_BEGIN);
            alarmIds.add(new AlarmId(eventId, eventStart));
        } while (mCursor.moveToNext());
        initiateGlobalDismiss(alarmIds);
    }

    private void dismissAlarm(long id, long eventId, long startTime) {
        ContentValues values = new ContentValues(1 /* size */);
        values.put(PROJECTION[INDEX_STATE], CalendarAlerts.STATE_DISMISSED);
        String selection = CalendarAlerts._ID + "=" + id;
        mQueryHandler.startUpdate(0, null, CalendarAlerts.CONTENT_URI, values,
                selection, null /* selectionArgs */, Utils.UNDO_DELAY);

        List<AlarmId> alarmIds = new LinkedList<AlarmId>();
        alarmIds.add(new AlarmId(eventId, startTime));
        initiateGlobalDismiss(alarmIds);
    }
    
    @SuppressWarnings("unchecked")
    private void initiateGlobalDismiss(List<AlarmId> alarmIds) {
        new AsyncTask<List<AlarmId>, Void, Void>() {
            @Override
            protected Void doInBackground(List<AlarmId>... params) {
                GlobalDismissManager.dismissGlobally(getApplicationContext(), params[0]);
                return null;
            }
        }.execute(alarmIds);
    }    
    
    private void makeEventInfoList() {
    	mFiredEventInfo = new ArrayList<AlertEventInfo>();
    	
    	mCursor.moveToPosition(-1);
    	while (mCursor.moveToNext()) {
    		long eventId = mCursor.getLong(AloneAlertActivity.INDEX_EVENT_ID);
    		String eventName = mCursor.getString(AloneAlertActivity.INDEX_TITLE);
            String location = mCursor.getString(AloneAlertActivity.INDEX_EVENT_LOCATION);
            long startMillis = mCursor.getLong(AloneAlertActivity.INDEX_BEGIN);
            long endMillis = mCursor.getLong(AloneAlertActivity.INDEX_END);
            boolean allDay = mCursor.getInt(AloneAlertActivity.INDEX_ALL_DAY) != 0;
            
            AlertEventInfo obj = new AlertEventInfo(eventId, eventName, location, startMillis, endMillis, allDay);
            mFiredEventInfo.add(obj);
    	}
    }
    
    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // Only set mCursor if the Activity is not finishing. Otherwise close the cursor.
            if (!isFinishing()) {
                mCursor = cursor;
                if (mCursor != null) {
	                if (mCursor.getCount() !=0 ) {	                	
	                	makeEventInfoList();
	                	settingMainDialogBox(0, mAlertMainDialogBoxComponentA);
	                }
	                else {
	                	if (INFO) Log.i(TAG, "No events found");
	                }
                }
               
                
            } else {
                cursor.close();
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            // Ignore
        }
    }
      
    public int getStatusBarHeight() {    	
    	int result = 0;
    	int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    	if (resourceId > 0) {
    		result = getResources().getDimensionPixelSize(resourceId);
    	}
    	return result;
	}
    
    public void calcDialogBoxwidth() {
    	float deviceWidth = AloneAlertActivity.this.getResources().getDisplayMetrics().widthPixels;
    	float deviceHeight = AloneAlertActivity.this.getResources().getDisplayMetrics().heightPixels;
    	float StatusBarHeight = Utils.getSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, -1);
        if (StatusBarHeight == -1) {
        	Utils.setSharedPreference(mContext, SettingsFragment.KEY_STATUS_BAR_HEIGHT, getStatusBarHeight());        	
        }        
        
        float viewWidth = deviceWidth * ALERT_ACTIVITY_DIALOGBOX_WIDTH_RATIO;
        mDialogBoxWidth = (int) viewWidth;
    }
    
    AnimationListener mMainDialogBoxAExitAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {			
			//mFrameLayout.removeView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
			mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setVisibility(View.GONE);
			mCurrentAlertMainDialogBoxComponent = mAlertMainDialogBoxComponentB;			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}    	
    };    
    
    
    AnimationListener mMainDialogBoxBExitAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {			
			//mFrameLayout.removeView(mAlertMainDialogBoxComponentB.mMainAlertDialogBox);
			mAlertMainDialogBoxComponentB.mMainAlertDialogBox.setVisibility(View.GONE);
			mCurrentAlertMainDialogBoxComponent = mAlertMainDialogBoxComponentA;			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}    	
    };   
    
    AnimationListener mOptionDialogBoxBExitAnimListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {				
		}

		@Override
		public void onAnimationEnd(Animation animation) {	
			// ������ run-time error�� �߻��ߴ�
			// :NullPointerException occurence at android.view.ViewGroup.dispatchDraw 
			//  ->The problem occurs because you call removeView() with an animation set on your View. 
			//    When this happens, 
			//    the framework is told to use that Animation as the remove animation for the View (for instance to fade out a view when you remove it.) 
			//    The View is temporarily moved to a separate list of children in the first parent. 
			//    You end up with a View belonging to two parents, which is invalid. 
			//mFrameLayout.removeView(mAlertOptionDialogBoxComponent.mOptionAlertDialogBox);
			// -> Visible/InVisible/Gone ���¸� �̿��Ѵ�
			mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.setVisibility(View.GONE);
			mCurrentAlertMainDialogBoxComponent = mAlertMainDialogBoxComponentA;	
		}

		@Override
		public void onAnimationRepeat(Animation animation) {					
		}    	
    };   
    
    
    OnClickListener mOnMainDialogBoxCloseClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
			boolean noMoreMustAlertEvent = true;
			int size = mFiredEventInfo.size();
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (!obj.Alerted) {
					noMoreMustAlertEvent = false;
					obj.Alerted = true;
					
					AnimationSet exitSetAnim = new AnimationSet(false);
					AlphaAnimation exitAlphaAnim = makeAlphaAnimation(400, ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
					exitSetAnim.addAnimation(exitAlphaAnim);
					ScaleAnimation exitScaleAnim = new ScaleAnimation(
					           1f, 0.75f, 
					           1f, 0.75f, 
					           Animation.RELATIVE_TO_SELF, 0.5f, 
					           Animation.RELATIVE_TO_SELF, 0.5f);
					exitScaleAnim.setDuration(400);
					exitSetAnim.addAnimation(exitScaleAnim);
					
					AlphaAnimation enterAnim = makeAlphaAnimation(400, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
					
					// ���� ���� �߰��� �ڽĺ䰡 Z-order ������ �θ��� ���� �Ʒ��� ��ġ�ϰ� �ȴ� : stack ���̴�
					if (mCurrentAlertMainDialogBoxComponent == mAlertMainDialogBoxComponentA) {
						settingMainDialogBox(i, mAlertMainDialogBoxComponentB);
						
						mFrameLayout.removeAllViews();
						if (mAlertMainDialogBoxComponentB.mMainAlertDialogBox.getVisibility() != View.VISIBLE)
							mAlertMainDialogBoxComponentB.mMainAlertDialogBox.setVisibility(View.VISIBLE);
						
						mFrameLayout.addView(mAlertMainDialogBoxComponentB.mMainAlertDialogBox);
						mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
						
						exitSetAnim.setAnimationListener(mMainDialogBoxAExitAnimListener);											
						mAlertMainDialogBoxComponentA.mMainAlertDialogBox.startAnimation(exitSetAnim);						
					}
					else {
						settingMainDialogBox(i, mAlertMainDialogBoxComponentA);

						mFrameLayout.removeAllViews();
						if (mAlertMainDialogBoxComponentA.mMainAlertDialogBox.getVisibility() != View.VISIBLE)
							mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setVisibility(View.VISIBLE);
						
						mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
						mFrameLayout.addView(mAlertMainDialogBoxComponentB.mMainAlertDialogBox);
						
						exitSetAnim.setAnimationListener(mMainDialogBoxBExitAnimListener);						
						mAlertMainDialogBoxComponentB.mMainAlertDialogBox.startAnimation(exitSetAnim);						
					}					
					
					return;
				}
			}
			
			if (noMoreMustAlertEvent) {
				NotificationManager nm =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		        nm.cancelAll();

		        dismissFiredAlarms(); 

		        finish();
			}
			
		}    	
    };
    
    OnClickListener mOnMainDialogBoxOptionClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			long eventId = (Long) v.getTag();
			
			// ���� map�� �����ҰŽ�...�Ѥ�;
			int size = mFiredEventInfo.size();
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (obj.eventId == eventId) {
					settingOptionDialogBox(i);
					
					mFrameLayout.removeAllViews();
					if (mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.getVisibility() != View.VISIBLE)
						mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.setVisibility(View.VISIBLE);
					mFrameLayout.addView(mAlertOptionDialogBoxComponent.mOptionAlertDialogBox);
					return;
				}
			}			
		}    	
    };
    
    
    OnClickListener mOnOptionDialogBoxEventInfoViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {	
			long eventId = (Long) v.getTag();
			
			long startMillis = 0;
			long endMillis = 0;	
			
			boolean gotEventInfo = false;
			int size = mFiredEventInfo.size();
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (eventId == obj.eventId) {
					startMillis = obj.startMillis;
					endMillis = obj.endMillis;
					gotEventInfo = true;
					break;
				}
			}
			
			if (gotEventInfo) {		
				Intent eventIntent = buildEventViewIntent(AloneAlertActivity.this, eventId,
	                    startMillis, endMillis);
				
				/*
				if (Utils.isJellybeanOrLater()) {
	                TaskStackBuilder.create(AloneAlertActivity.this).addParentStack(CentralActivity.class)
	                        .addNextIntent(eventIntent).startActivities();
	            } else {
	            	AloneAlertActivity.this.startActivity(eventIntent);
	            }
	            */
				AloneAlertActivity.this.startActivity(eventIntent);
			}			
			
	        boolean noMoreMustAlertEvent = true;
				        
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (!obj.Alerted) {
					noMoreMustAlertEvent = false;					
					
					AnimationSet exitSetAnim = new AnimationSet(false);
					AlphaAnimation exitAlphaAnim = makeAlphaAnimation(400, ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
					exitSetAnim.addAnimation(exitAlphaAnim);
					ScaleAnimation exitScaleAnim = new ScaleAnimation(
					           1f, 0.75f, 
					           1f, 0.75f, 
					           Animation.RELATIVE_TO_SELF, 0.5f, 
					           Animation.RELATIVE_TO_SELF, 0.5f);
					exitScaleAnim.setDuration(400);
					exitSetAnim.addAnimation(exitScaleAnim);
					
					AlphaAnimation enterAnim = makeAlphaAnimation(400, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
									
					settingMainDialogBox(i, mAlertMainDialogBoxComponentA);
					
					mFrameLayout.removeAllViews();
					mFrameLayout.addView(mAlertOptionDialogBoxComponent.mOptionAlertDialogBox);
					
					if (mAlertMainDialogBoxComponentA.mMainAlertDialogBox.getVisibility() != View.VISIBLE)
						mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setVisibility(View.VISIBLE);
					mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
					
					exitSetAnim.setAnimationListener(mOptionDialogBoxBExitAnimListener);
					mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.startAnimation(exitSetAnim);
					mAlertMainDialogBoxComponentA.mMainAlertDialogBox.startAnimation(enterAnim);							
					
					return;
				}
			}
			
			if (noMoreMustAlertEvent) {
				NotificationManager nm =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		        nm.cancelAll();

		        dismissFiredAlarms(); 

		        finish();
			}
	        
	        
		}
    };
    
    OnClickListener mOnOptionDialogBoxSnoozeClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {	
			long eventId = (Long) v.getTag();
			
			long startMillis = 0;
			long endMillis = 0;			
			
			boolean gotEventInfo = false;
			int size = mFiredEventInfo.size();
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (eventId == obj.eventId) {
					startMillis = obj.startMillis;
					endMillis = obj.endMillis;
					gotEventInfo = true;
					break;
				}
			}
			
			if (gotEventInfo) {				
				Intent snoozeIntent = createSnoozeIntent(mContext, 					
						eventId,
						startMillis, 
						endMillis);
				startService(snoozeIntent);
			}
			
			boolean noMoreMustAlertEvent = true;
			
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (!obj.Alerted) {
					noMoreMustAlertEvent = false;					
					
					AnimationSet exitSetAnim = new AnimationSet(false);
					AlphaAnimation exitAlphaAnim = makeAlphaAnimation(400, ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
					exitSetAnim.addAnimation(exitAlphaAnim);
					ScaleAnimation exitScaleAnim = new ScaleAnimation(
					           1f, 0.75f, 
					           1f, 0.75f, 
					           Animation.RELATIVE_TO_SELF, 0.5f, 
					           Animation.RELATIVE_TO_SELF, 0.5f);
					exitScaleAnim.setDuration(400);
					exitSetAnim.addAnimation(exitScaleAnim);
					
					AlphaAnimation enterAnim = makeAlphaAnimation(400, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
									
					settingMainDialogBox(i, mAlertMainDialogBoxComponentA);
					
					mFrameLayout.removeAllViews();
					mFrameLayout.addView(mAlertOptionDialogBoxComponent.mOptionAlertDialogBox);
					
					if (mAlertMainDialogBoxComponentA.mMainAlertDialogBox.getVisibility() != View.VISIBLE)
						mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setVisibility(View.VISIBLE);
					mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
					
					exitSetAnim.setAnimationListener(mOptionDialogBoxBExitAnimListener);
					mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.startAnimation(exitSetAnim);
					mAlertMainDialogBoxComponentA.mMainAlertDialogBox.startAnimation(enterAnim);							
					
					return;
				}
			}
			
			if (noMoreMustAlertEvent) {
				NotificationManager nm =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		        nm.cancelAll();

		        dismissFiredAlarms(); 

		        finish();
			}
			
		}    	
    };
    
    OnClickListener mOnOptionDialogBoxDiectionsClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {	
			String urlString = (String) v.getTag();
			Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
	        geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        
	        mContext.startActivity(geoIntent);
			
	        boolean noMoreMustAlertEvent = true;
			
	        int size = mFiredEventInfo.size();
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (!obj.Alerted) {
					noMoreMustAlertEvent = false;					
					
					AnimationSet exitSetAnim = new AnimationSet(false);
					AlphaAnimation exitAlphaAnim = makeAlphaAnimation(400, ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
					exitSetAnim.addAnimation(exitAlphaAnim);
					ScaleAnimation exitScaleAnim = new ScaleAnimation(
					           1f, 0.75f, 
					           1f, 0.75f, 
					           Animation.RELATIVE_TO_SELF, 0.5f, 
					           Animation.RELATIVE_TO_SELF, 0.5f);
					exitScaleAnim.setDuration(400);
					exitSetAnim.addAnimation(exitScaleAnim);
					
					AlphaAnimation enterAnim = makeAlphaAnimation(400, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
									
					settingMainDialogBox(i, mAlertMainDialogBoxComponentA);
					
					mFrameLayout.removeAllViews();
					mFrameLayout.addView(mAlertOptionDialogBoxComponent.mOptionAlertDialogBox);
					
					if (mAlertMainDialogBoxComponentA.mMainAlertDialogBox.getVisibility() != View.VISIBLE)
						mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setVisibility(View.VISIBLE);
					mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
					
					exitSetAnim.setAnimationListener(mOptionDialogBoxBExitAnimListener);
					mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.startAnimation(exitSetAnim);
					mAlertMainDialogBoxComponentA.mMainAlertDialogBox.startAnimation(enterAnim);							
					
					return;
				}
			}
			
			if (noMoreMustAlertEvent) {
				NotificationManager nm =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		        nm.cancelAll();

		        dismissFiredAlarms(); 

		        finish();
			}
	        
	        
		}
    };
    
    OnClickListener mOnOptionDialogBoxCloseClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {			
			
			boolean noMoreMustAlertEvent = true;
			int size = mFiredEventInfo.size();
			for (int i=0; i<size; i++) {
				AlertEventInfo obj = mFiredEventInfo.get(i);
				if (!obj.Alerted) {
					noMoreMustAlertEvent = false;					
					
					AnimationSet exitSetAnim = new AnimationSet(false);
					AlphaAnimation exitAlphaAnim = makeAlphaAnimation(400, ALPHA_OPAQUE_TO_TRANSPARENT, 1, 0.0f, ANIMATION_INTERPOLATORTYPE_DECELERATE);
					exitSetAnim.addAnimation(exitAlphaAnim);
					ScaleAnimation exitScaleAnim = new ScaleAnimation(
					           1f, 0.75f, 
					           1f, 0.75f, 
					           Animation.RELATIVE_TO_SELF, 0.5f, 
					           Animation.RELATIVE_TO_SELF, 0.5f);
					exitScaleAnim.setDuration(400);
					exitSetAnim.addAnimation(exitScaleAnim);
					
					AlphaAnimation enterAnim = makeAlphaAnimation(400, ALPHA_TRANSPARENT_TO_OPAQUE, 0.0f, 1, ANIMATION_INTERPOLATORTYPE_ACCELERATE);
									
					settingMainDialogBox(i, mAlertMainDialogBoxComponentA);
					
					mFrameLayout.removeAllViews();
					mFrameLayout.addView(mAlertOptionDialogBoxComponent.mOptionAlertDialogBox);
					
					if (mAlertMainDialogBoxComponentA.mMainAlertDialogBox.getVisibility() != View.VISIBLE)
						mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setVisibility(View.VISIBLE);
					mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);
					
					exitSetAnim.setAnimationListener(mOptionDialogBoxBExitAnimListener);
					mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.startAnimation(exitSetAnim);
					mAlertMainDialogBoxComponentA.mMainAlertDialogBox.startAnimation(enterAnim);							
					
					return;
				}
			}
			
			if (noMoreMustAlertEvent) {
				NotificationManager nm =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		        nm.cancelAll();

		        dismissFiredAlarms(); 

		        finish();
			}
			
		}    	
    };
    
    
    
    public void makeMainDialogBoxLayout() {   
    	
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);    	
        
        LayoutParams dialogParams = new LayoutParams(mDialogBoxWidth, LayoutParams.WRAP_CONTENT);          
    	
		if (mAlertMainDialogBoxComponentA == null) {
			mAlertMainDialogBoxComponentA = new AlertMainDialogBoxComponent();		
		
    		mAlertMainDialogBoxComponentA.mMainAlertDialogBox = (CommonRoundedCornerLinearLayout) inflater.inflate(R.layout.alert_main_db, null, false);   
    		mAlertMainDialogBoxComponentA.mMainAlertDialogBox.setLayoutParams(dialogParams);
            
    		mAlertMainDialogBoxComponentA.mMainAlertDBEventInfoItemContainer = (LinearLayout) mAlertMainDialogBoxComponentA.mMainAlertDialogBox.findViewById(R.id.alert_main_db_eventinfo_item_container);        
    		mAlertMainDialogBoxComponentA.mMainAlertDBEventTitleItem = (TextView) mAlertMainDialogBoxComponentA.mMainAlertDBEventInfoItemContainer.findViewById(R.id.event_title);        
    		mAlertMainDialogBoxComponentA.mMainAlertDBWhenItem = (TextView) mAlertMainDialogBoxComponentA.mMainAlertDBEventInfoItemContainer.findViewById(R.id.when);                
    		mAlertMainDialogBoxComponentA.mMainAlertDBWhereItem = (TextView) mAlertMainDialogBoxComponentA.mMainAlertDBEventInfoItemContainer.findViewById(R.id.where);   	
        	
    		mAlertMainDialogBoxComponentA.mMainAlertDBButtonContainer = (LinearLayout) mAlertMainDialogBoxComponentA.mMainAlertDialogBox.findViewById(R.id.alert_button_container);
    		mAlertMainDialogBoxComponentA.mMainAlertDBCloseButton = (TextView) mAlertMainDialogBoxComponentA.mMainAlertDBButtonContainer.findViewById(R.id.close_alarm_alert);
    		mAlertMainDialogBoxComponentA.mMainAlertDBOptionButton = (TextView) mAlertMainDialogBoxComponentA.mMainAlertDBButtonContainer.findViewById(R.id.option_alarm_alert); 
        	
    		mAlertMainDialogBoxComponentA.mMainAlertDBCloseButton.setOnClickListener(mOnMainDialogBoxCloseClickListener);    
    		mAlertMainDialogBoxComponentA.mMainAlertDBOptionButton.setOnClickListener(mOnMainDialogBoxOptionClickListener);    
		}
	
	
		if (mAlertMainDialogBoxComponentB == null) {
			mAlertMainDialogBoxComponentB = new AlertMainDialogBoxComponent();		
			
			mAlertMainDialogBoxComponentB.mMainAlertDialogBox = (CommonRoundedCornerLinearLayout) inflater.inflate(R.layout.alert_main_db, null, false);   
			mAlertMainDialogBoxComponentB.mMainAlertDialogBox.setLayoutParams(dialogParams);
	        
			mAlertMainDialogBoxComponentB.mMainAlertDBEventInfoItemContainer = (LinearLayout) mAlertMainDialogBoxComponentB.mMainAlertDialogBox.findViewById(R.id.alert_main_db_eventinfo_item_container);        
			mAlertMainDialogBoxComponentB.mMainAlertDBEventTitleItem = (TextView) mAlertMainDialogBoxComponentB.mMainAlertDBEventInfoItemContainer.findViewById(R.id.event_title);        
			mAlertMainDialogBoxComponentB.mMainAlertDBWhenItem = (TextView) mAlertMainDialogBoxComponentB.mMainAlertDBEventInfoItemContainer.findViewById(R.id.when);                
			mAlertMainDialogBoxComponentB.mMainAlertDBWhereItem = (TextView) mAlertMainDialogBoxComponentB.mMainAlertDBEventInfoItemContainer.findViewById(R.id.where);   	
	    	
			mAlertMainDialogBoxComponentB.mMainAlertDBButtonContainer = (LinearLayout) mAlertMainDialogBoxComponentB.mMainAlertDialogBox.findViewById(R.id.alert_button_container);
			mAlertMainDialogBoxComponentB.mMainAlertDBCloseButton = (TextView) mAlertMainDialogBoxComponentB.mMainAlertDBButtonContainer.findViewById(R.id.close_alarm_alert);
			mAlertMainDialogBoxComponentB.mMainAlertDBOptionButton = (TextView) mAlertMainDialogBoxComponentB.mMainAlertDBButtonContainer.findViewById(R.id.option_alarm_alert); 
	    	
			mAlertMainDialogBoxComponentB.mMainAlertDBCloseButton.setOnClickListener(mOnMainDialogBoxCloseClickListener);
			mAlertMainDialogBoxComponentB.mMainAlertDBOptionButton.setOnClickListener(mOnMainDialogBoxOptionClickListener);  
		} 
    	
    	 	
		mFrameLayout.addView(mAlertMainDialogBoxComponentA.mMainAlertDialogBox);  
		mCurrentAlertMainDialogBoxComponent = mAlertMainDialogBoxComponentA;
    }    
    
    public void makeOptionDialogBoxLayout() {
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);    	
        
        LayoutParams dialogParams = new LayoutParams(mDialogBoxWidth, LayoutParams.WRAP_CONTENT); 
        
        mAlertOptionDialogBoxComponent = new AlertOptionDialogBoxComponent();
        
        mAlertOptionDialogBoxComponent.mOptionAlertDialogBox = (CommonRoundedCornerLinearLayout) inflater.inflate(R.layout.alert_option_db, null, false);   
        mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.setLayoutParams(dialogParams);
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBEventInfoItemContainer = (LinearLayout) mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.findViewById(R.id.alert_option_db_eventinfo_item_container);        
        mAlertOptionDialogBoxComponent.mOptionAlertDBEventTitleItem = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDBEventInfoItemContainer.findViewById(R.id.event_title);        
        mAlertOptionDialogBoxComponent.mOptionAlertDBWhenItem = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDBEventInfoItemContainer.findViewById(R.id.when);                
        mAlertOptionDialogBoxComponent.mOptionAlertDBWhereItem = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDBEventInfoItemContainer.findViewById(R.id.where);   	
    	
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBViewEventInfoButton = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.findViewById(R.id.option_db_view_event_info);
        mAlertOptionDialogBoxComponent.mOptionAlertDBSnoozeAlarmButton = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.findViewById(R.id.option_db_snooze_alarm); 
        mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsButton = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.findViewById(R.id.option_db_directions_alarm_alert);
        mAlertOptionDialogBoxComponent.mOptionAlertDBCloseButton = (TextView) mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.findViewById(R.id.option_db_close_alarm_alert);
    	
        mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsUpperLine = mAlertOptionDialogBoxComponent.mOptionAlertDialogBox.findViewById(R.id.option_db_directions_alarm_alert_upperline);
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBViewEventInfoButton.setOnClickListener(mOnOptionDialogBoxEventInfoViewClickListener);
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBSnoozeAlarmButton.setOnClickListener(mOnOptionDialogBoxSnoozeClickListener);
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsButton.setOnClickListener(mOnOptionDialogBoxDiectionsClickListener);
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBCloseButton.setOnClickListener(mOnOptionDialogBoxCloseClickListener);
        
    }
    

    long mAlarmTime;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (INFO) Log.i(TAG, "onCreate");
        
        mContext = getApplicationContext();
        mAlarmTime = this.getIntent().getLongExtra("alarmTime", 0);
        
        mQueryHandler = new QueryHandler(this);
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);    	
    	mFrameLayout = (RelativeLayout) inflater.inflate(R.layout.alert_activity_frame, null, false);        	
    	setContentView(mFrameLayout); 
    	
    	calcDialogBoxwidth();
    	makeMainDialogBoxLayout(); 
    	makeOptionDialogBoxLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the cursor is null, start the async handler. If it is not null just requery.
        if (mCursor == null) {
            Uri uri = CalendarAlerts.CONTENT_URI_BY_INSTANCE;
            mQueryHandler.startQuery(0, null, uri, PROJECTION, 
            		(SELECTION + mAlarmTime), 
            		SELECTIONARG,
                    CalendarContract.CalendarAlerts.DEFAULT_SORT_ORDER);
        } else {
            if (!mCursor.requery()) {
                Log.w(TAG, "Cursor#requery() failed.");
                mCursor.close();
                mCursor = null;
            }
        }
    }

    void closeActivityIfEmpty() {
        if (mCursor != null && !mCursor.isClosed() && mCursor.getCount() == 0) {
        	AloneAlertActivity.this.finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Can't run updateAlertNotification in main thread
        AsyncTask task = new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context ... params) {
                AlertService.updateAlertNotification(params[0]);
                return null;
            }
        }.execute(this);


        if (mCursor != null) {
            mCursor.deactivate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCursor != null) {
            mCursor.close();
        }
    }	
	
	public boolean isEmpty() {
        return mCursor != null ? (mCursor.getCount() == 0) : true;
    }
	
	public void settingMainDialogBox(int index, AlertMainDialogBoxComponent mainDialogBoxComponent) {
		AlertEventInfo obj = mFiredEventInfo.get(index);
		obj.Alerted = true;
		
		long eventId = obj.eventId;
        String eventName = obj.eventName;
        String location = obj.location;
                
        /*
        CalendarAlerts.BEGIN : The start time of the event, in UTC
        long startMillis = mCursor.getLong(AloneAlertActivity.INDEX_BEGIN);
		long endMillis = mCursor.getLong(AloneAlertActivity.INDEX_END);
		AlertEventInfo obj = new AlertEventInfo(eventId, eventName, location, startMillis, endMillis, allDay);
         */
        long startMillis = obj.startMillis; // 
        long endMillis = obj.endMillis;
        boolean allDay = obj.allDay;
        
        Resources res = mContext.getResources();        
        
        // What
        if (eventName == null || eventName.length() == 0) {
            eventName = res.getString(R.string.no_title_label);
        }
        mainDialogBoxComponent.mMainAlertDBEventTitleItem.setText(eventName);
		        
        // When
        String when;
        int flags;
        String tz = Utils.getTimeZone(mContext, null);
        if (allDay) {
            flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY |
                    DateUtils.FORMAT_SHOW_DATE;
            tz = ETime.TIMEZONE_UTC;
        } else {
            flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
        }
        if (DateFormat.is24HourFormat(mContext)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }

        TimeZone timezone = TimeZone.getTimeZone(tz);
        Calendar time = GregorianCalendar.getInstance(timezone);//Time time = new Time(tz);
        time.setTimeInMillis(startMillis);
        boolean isDST = false;//time.isDst != 0;
        StringBuilder sb = new StringBuilder(
                Utils.formatDateRange(mContext, startMillis, endMillis, flags));
        if (!allDay && tz != ETime.getCurrentTimezone()) {
            sb.append(" ").append(TimeZone.getTimeZone(tz).getDisplayName(
                    isDST, TimeZone.SHORT, Locale.getDefault()));
        }

        when = sb.toString();
        mainDialogBoxComponent.mMainAlertDBWhenItem.setText(when);

        // Where
        if (location == null || location.length() == 0) {
        	mainDialogBoxComponent.mMainAlertDBWhereItem.setVisibility(View.GONE);
        } else {
        	mainDialogBoxComponent.mMainAlertDBWhereItem.setText(location);
        	mainDialogBoxComponent.mMainAlertDBWhereItem.setVisibility(View.VISIBLE);
        }
        
        mainDialogBoxComponent.mMainAlertDBOptionButton.setTag(eventId);
	}
	
	
	public void settingOptionDialogBox(int index) {
		AlertEventInfo obj = mFiredEventInfo.get(index);
		obj.Alerted = true;
		
		long eventId = obj.eventId;
        String eventName = obj.eventName;
        String location = obj.location;
        long startMillis = obj.startMillis;
        long endMillis = obj.endMillis;
        boolean allDay = obj.allDay;
        
        Resources res = mContext.getResources();        
        
        // What
        if (eventName == null || eventName.length() == 0) {
            eventName = res.getString(R.string.no_title_label);
        }
        mAlertOptionDialogBoxComponent.mOptionAlertDBEventTitleItem.setText(eventName);
		        
        // When
        String when;
        int flags;
        String tz = Utils.getTimeZone(mContext, null);
        if (allDay) {
            flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY |
                    DateUtils.FORMAT_SHOW_DATE;
            tz = ETime.TIMEZONE_UTC;
        } else {
            flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
        }
        if (DateFormat.is24HourFormat(mContext)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }

        TimeZone timezone = TimeZone.getTimeZone(tz);
        Calendar time = GregorianCalendar.getInstance(timezone);//Time time = new Time(tz);//Time time = new Time(tz);
        time.setTimeInMillis(startMillis);
        boolean isDST = false;//time.isDst != 0;
        StringBuilder sb = new StringBuilder(
                Utils.formatDateRange(mContext, startMillis, endMillis, flags));
        if (!allDay && tz != ETime.getCurrentTimezone()) {
            sb.append(" ").append(TimeZone.getTimeZone(tz).getDisplayName(
                    isDST, TimeZone.SHORT, Locale.getDefault()));
        }

        when = sb.toString();
        mAlertOptionDialogBoxComponent.mOptionAlertDBWhenItem.setText(when);

        // Where
        if (location == null || location.length() == 0) {
        	mAlertOptionDialogBoxComponent.mOptionAlertDBWhereItem.setVisibility(View.GONE);
        	
        	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsUpperLine.setVisibility(View.GONE);
        	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsButton.setVisibility(View.GONE);
        } else {
        	mAlertOptionDialogBoxComponent.mOptionAlertDBWhereItem.setText(location);
        	mAlertOptionDialogBoxComponent.mOptionAlertDBWhereItem.setVisibility(View.VISIBLE);
        	
        	URLSpan[] urlSpans = getURLSpans(mContext, eventId);
            String validLocationAddress = checkValidLocationAddress(urlSpans);
            if (validLocationAddress != null) {
            	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsUpperLine.setVisibility(View.VISIBLE);
            	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsButton.setVisibility(View.VISIBLE);
            	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsButton.setTag(validLocationAddress);
            }
            else {
            	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsUpperLine.setVisibility(View.GONE);
            	mAlertOptionDialogBoxComponent.mOptionAlertDBDirectionsButton.setVisibility(View.GONE);
            }
        }        
        
        mAlertOptionDialogBoxComponent.mOptionAlertDBViewEventInfoButton.setTag(eventId);    
        mAlertOptionDialogBoxComponent.mOptionAlertDBSnoozeAlarmButton.setTag(eventId);       
        
	}
	
		
	// AloneAlertActivity�� AlertReceiver.createSnoozeIntent�ʹ� �ٸ���
	// notification id�� snooze intent�� �������� �ʴ´�
	// :AlertReceiver.createSnoozeIntent���� notification id�� �����ϴ� ������
	//  notification bar Ķ���� �˸� notification view�� snooze[�˸� �Ͻ�����] ��ư�� ������ Ŭ������ �� 
	//  �ش� notification�� ����ϱ� ���� ���ε�,
	//  AloneAlertActivity�� popup �Ǿ��ٴ� ���� notification bar�� Ķ���� notification view�� �������� �ʾ����� �ǹ��ϹǷ�
	//  ����� notification view�� ���� �����̴�
	//  ->SnoozeAlarmService.onHandleIntent���� notification id�� intent�� ���� ���
	//    ����Ʈ�� EXPIRED_GROUP_NOTIFICATION_ID �����Ͽ� notification.cancel�� ȣ��Ǵ� ���� ������ �ϱ� �����̴�
	private Intent createSnoozeIntent(Context context, long eventId,
            long startMillis, long endMillis) {
        Intent intent = new Intent();
        intent.setClass(context, SnoozeAlarmsService.class);
        intent.putExtra(AlertUtils.EVENT_ID_KEY, eventId);
        intent.putExtra(AlertUtils.EVENT_START_KEY, startMillis);
        intent.putExtra(AlertUtils.EVENT_END_KEY, endMillis);        

        Uri.Builder builder = Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, eventId);
        ContentUris.appendId(builder, startMillis);
        intent.setData(builder.build());
        return intent;
        //return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
	
	public boolean checkValidAddress(long eventId) {
		URLSpan[] urlSpans = getURLSpans(mContext, eventId);
		
		boolean IsValid = false;
		for (int span_i = 0; span_i < urlSpans.length; span_i++) {
            URLSpan urlSpan = urlSpans[span_i];
            String urlString = urlSpan.getURL();//////////////////////////////////////////////////
            if (urlString.startsWith(GEO_PREFIX)) {
            	IsValid = true;
                break;
            }
        }
		
		return IsValid;	 
	}
	
	public String checkValidLocationAddress(URLSpan[] urlSpans) {		
		String validLocationAddr = null;
		
		//boolean IsValid = false;
		for (int span_i = 0; span_i < urlSpans.length; span_i++) {
            URLSpan urlSpan = urlSpans[span_i];
            String urlString = urlSpan.getURL();//////////////////////////////////////////////////
            if (urlString.startsWith(GEO_PREFIX)) {
            	validLocationAddr = urlSpan.getURL();
                break;
            }
        }
		
		return validLocationAddr;	 
	}
	
	private Cursor getLocationCursor(Context context, long eventId) {
        return context.getContentResolver().query(
                ContentUris.withAppendedId(Events.CONTENT_URI, eventId),
                new String[] { Events.EVENT_LOCATION }, null, null, null);
    }
	
	private URLSpan[] getURLSpans(Context context, long eventId) {
        Cursor locationCursor = getLocationCursor(context, eventId);

        // Default to empty list
        URLSpan[] urlSpans = new URLSpan[0];
        if (locationCursor != null && locationCursor.moveToFirst()) {
            String location = locationCursor.getString(0); // Only one item in this cursor.
            if (location != null && !location.isEmpty()) {
                Spannable text = Utils.extendedLinkify(location, true);
                // The linkify method should have found at least one link, at the very least.
                // If no smart links were found, it should have set the whole string as a geo link.
                urlSpans = text.getSpans(0, text.length(), URLSpan.class);
            }
            locationCursor.close();
        }

        return urlSpans;
    }
	
	
	public Intent buildEventViewIntent(Context c, long eventId, long begin, long end) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendEncodedPath("events/" + eventId);
        i.setData(builder.build());
        i.setClass(c, CentralActivity.class);
        i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
        i.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);
        i.putExtra(Utils.EXTRA_EVENT_VIEW_DISPLAY_FOR_ALERT, true);
        return i;
    }
		
	public static final int ALPHA_OPAQUE_TO_TRANSPARENT = 1;
	public static final int ALPHA_TRANSPARENT_TO_OPAQUE = 2;
	public static final int ANIMATION_INTERPOLATORTYPE_ACCELERATE = 1;
	public static final int ANIMATION_INTERPOLATORTYPE_DECELERATE = 2;
	public AlphaAnimation makeAlphaAnimation(long duration, int alphaType, float fromAlpha, float toAlpha, int InterpolatorType) {    	
	 	
	    AlphaAnimation alphaAnimation = null;	     	
	    alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);	         
        
        alphaAnimation.setDuration(duration);
        if (InterpolatorType == ANIMATION_INTERPOLATORTYPE_ACCELERATE)
        	alphaAnimation.setInterpolator(new AccelerateInterpolator());
        else
        	alphaAnimation.setInterpolator(new DecelerateInterpolator());
        
        return alphaAnimation;        
	}	

	/*
	public void bindView(Cursor cursor) {

        String eventName = cursor.getString(AloneAlertActivity.INDEX_TITLE);
        String location = cursor.getString(AloneAlertActivity.INDEX_EVENT_LOCATION);
        long startMillis = cursor.getLong(AloneAlertActivity.INDEX_BEGIN);
        long endMillis = cursor.getLong(AloneAlertActivity.INDEX_END);
        boolean allDay = cursor.getInt(AloneAlertActivity.INDEX_ALL_DAY) != 0;

        updateView(eventName, location, startMillis, endMillis, allDay);
    }
	
	public void bindView(int index) {              

		AlertEventInfo obj = mFiredEventInfo.get(index);
		obj.Alerted = true;
        String eventName = obj.eventName;
        String location = obj.location;
        long startMillis = obj.startMillis;
        long endMillis = obj.endMillis;
        boolean allDay = obj.allDay;

        updateView(eventName, location, startMillis, endMillis, allDay);
    }   
    
    public void updateView(String eventName, String location,
            long startMillis, long endMillis, boolean allDay) {
        Resources res = mContext.getResources();        
        
        // What
        if (eventName == null || eventName.length() == 0) {
            eventName = res.getString(R.string.no_title_label);
        }
        mMainAlertDBEventTitleItem.setText(eventName);
		        
        // When
        String when;
        int flags;
        String tz = Utils.getTimeZone(mContext, null);
        if (allDay) {
            flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY |
                    DateUtils.FORMAT_SHOW_DATE;
            tz = Time.TIMEZONE_UTC;
        } else {
            flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
        }
        if (DateFormat.is24HourFormat(mContext)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }

        Time time = new Time(tz);
        time.set(startMillis);
        boolean isDST = time.isDst != 0;
        StringBuilder sb = new StringBuilder(
                Utils.formatDateRange(mContext, startMillis, endMillis, flags));
        if (!allDay && tz != Time.getCurrentTimezone()) {
            sb.append(" ").append(TimeZone.getTimeZone(tz).getDisplayName(
                    isDST, TimeZone.SHORT, Locale.getDefault()));
        }

        when = sb.toString();
        mMainAlertDBWhenItem.setText(when);

        // Where
        if (location == null || location.length() == 0) {
        	mMainAlertDBWhereItem.setVisibility(View.GONE);
        } else {
        	mMainAlertDBWhereItem.setText(location);
        	mMainAlertDBWhereItem.setVisibility(View.VISIBLE);
        }
    }
    */
    
    
    /*
    public Cursor getItemForView(View view) {
        final int index = mListView.getPositionForView(view);
        if (index < 0) {
            return null;
        }
        return (Cursor) mListView.getAdapter().getItem(index);
    }
    */
    
    /*
    private final OnItemClickListener mViewListener = new OnItemClickListener() {

        @SuppressLint("NewApi")
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long i) {
        	AloneAlertActivity alertActivity = AloneAlertActivity.this;
            Cursor cursor = alertActivity.getItemForView(view);

            long alarmId = cursor.getLong(INDEX_ROW_ID);
            long eventId = cursor.getLong(AloneAlertActivity.INDEX_EVENT_ID);
            long startMillis = cursor.getLong(AloneAlertActivity.INDEX_BEGIN);

            // Mark this alarm as DISMISSED
            dismissAlarm(alarmId, eventId, startMillis);

            // build an intent and task stack to start EventInfoActivity with AllInOneActivity
            // as the parent activity rooted to home.
            long endMillis = cursor.getLong(AloneAlertActivity.INDEX_END);
            Intent eventIntent = AlertUtils.buildEventViewIntent(AloneAlertActivity.this, eventId,
                    startMillis, endMillis);

            if (Utils.isJellybeanOrLater()) {
                TaskStackBuilder.create(AloneAlertActivity.this).addParentStack(EventInfoActivity.class)
                        .addNextIntent(eventIntent).startActivities();
            } else {
                alertActivity.startActivity(eventIntent);
            }

            alertActivity.finish();
        }
    };
    */

}
