package com.intheeast.acalendar;

import java.io.IOException;

import com.intheeast.settings.SettingsFragment;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.ParcelFileDescriptor;

public class ECalendarBackupAgent extends BackupAgentHelper {
	static final String SHARED_KEY = "shared_pref";

    @Override
    public void onCreate() {
        addHelper(SHARED_KEY, new SharedPreferencesBackupHelper(this,
                SettingsFragment.SHARED_PREFS_NAME));
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {
        // See Utils.getRingTonePreference for more info
        final Editor editor = getSharedPreferences(
                SettingsFragment.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE).edit();
        editor.putString(SettingsFragment.KEY_ALERTS_RINGTONE,
                SettingsFragment.DEFAULT_RINGTONE).commit();

        super.onRestore(data, appVersionCode, newState);
    }
}
