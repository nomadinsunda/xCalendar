package com.intheeast.acalendar;

import android.app.Activity;

public class AbstractCalendarActivity extends Activity {
	protected AsyncQueryService mService;

    public synchronized AsyncQueryService getAsyncQueryService() {
        if (mService == null) {
            mService = new AsyncQueryService(this);
        }
        return mService;
    }
}
