package com.intheeast.acalendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppRemovalReceiver extends BroadcastReceiver {

	private static final String TAG = AppRemovalReceiver.class.getSimpleName();    
    private static final boolean INFO = true;
    
	public static final String PACKAGE_NAME = "com.intheeast.ecalendar";
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent != null) {
			/*			 
			if(Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
				Uri uri = intent.getData();
				if(uri == null){
					if (INFO) Log.i(TAG, "onReceive:uri==null");
					return;
				}
				
				if(PACKAGE_NAME.equals(uri.getSchemeSpecificPart())){
					// App 초기화 및 재설치 관련 처리
					context.getSharedPreferences(SettingsFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit(); 
					if (INFO) Log.i(TAG, "onReceive:remove preference");
										
				}
			}
			*/
		}
	}

}
