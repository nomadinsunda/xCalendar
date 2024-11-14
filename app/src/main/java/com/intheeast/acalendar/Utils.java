package com.intheeast.acalendar;



import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import android.accounts.Account;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
//import android.text.format.DateUtils;
//import android.text.format.Time;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intheeast.day.DayOfMonthDrawable;
import com.intheeast.acalendar.CalendarController.ViewType;
import com.intheeast.acalendar.CalendarEventModel.ReminderEntry;
import com.intheeast.acalendar.CalendarUtils.TimeZoneUtils;
import com.intheeast.etc.ETime;
import com.intheeast.settings.SettingsFragment;

public class Utils {
    private static final boolean DEBUG = false;
    private static final String TAG = "CalUtils";

    // Set to 0 until we have UI to perform undo
    public static final long UNDO_DELAY = 0;

    // For recurring events which instances of the series are being modified
    public static final int MODIFY_UNINITIALIZED = 0;
    public static final int MODIFY_SELECTED = 1;
    public static final int MODIFY_ALL_FOLLOWING = 2;
    public static final int MODIFY_ALL = 3;

    // When the edit event view finishes it passes back the appropriate exit
    // code.
    public static final int DONE_REVERT = 1 << 0;
    public static final int DONE_SAVE = 1 << 1;
    public static final int DONE_DELETE = 1 << 2;
    // And should re run with DONE_EXIT if it should also leave the view, just
    // exiting is identical to reverting
    public static final int DONE_EXIT = 1 << 0;

    public static final String OPEN_EMAIL_MARKER = " <";
    public static final String CLOSE_EMAIL_MARKER = ">";

    public static final String INTENT_KEY_DETAIL_VIEW = "DETAIL_VIEW";
    public static final String INTENT_KEY_VIEW_TYPE = "VIEW";
    public static final String INTENT_VALUE_VIEW_TYPE_DAY = "DAY";
    public static final String INTENT_KEY_HOME = "KEY_HOME";

    public static final int MONDAY_BEFORE_JULIAN_EPOCH = ETime.EPOCH_JULIAN_DAY - 3;
    public static final int DECLINED_EVENT_ALPHA = 0x66;
    public static final int DECLINED_EVENT_TEXT_ALPHA = 0xC0;
    
    
    public static final String EXTRA_EVENT_VIEW_DISPLAY_FOR_ALERT = "eventViewDisplayForAlert";

    private static final float SATURATION_ADJUST = 1.3f;
    private static final float INTENSITY_ADJUST = 0.8f;
    
    

    // Defines used by the DNA generation code
    static final int DAY_IN_MINUTES = 60 * 24;
    static final int WEEK_IN_MINUTES = DAY_IN_MINUTES * 7;
    // The work day is being counted as 6am to 8pm
    static int WORK_DAY_MINUTES = 14 * 60;
    static int WORK_DAY_START_MINUTES = 6 * 60;
    static int WORK_DAY_END_MINUTES = 20 * 60;
    static int WORK_DAY_END_LENGTH = (24 * 60) - WORK_DAY_END_MINUTES;
    static int CONFLICT_COLOR = 0xFF000000;
    static boolean mMinutesLoaded = false;

    public static final int YEAR_MIN = 1970;
    public static final int YEAR_MAX = 2036;

    // The name of the shared preferences file. This name must be maintained for
    // historical
    // reasons, as it's what PreferenceManager assigned the first time the file
    // was created.
    static final String SHARED_PREFS_NAME = "com.intheeast.ecalendar_preferences";

    public static final String KEY_QUICK_RESPONSES = "preferences_quick_responses";

    public static final String KEY_ALERTS_VIBRATE_WHEN = "preferences_alerts_vibrateWhen";

    public static final String APPWIDGET_DATA_TYPE = "vnd.android.data/update";

    static final String MACHINE_GENERATED_ADDRESS = "calendar.google.com";

    private static final TimeZoneUtils mTZUtils = new TimeZoneUtils(SHARED_PREFS_NAME);
    private static boolean mAllowWeekForDetailView = false;
    private static long mTardis = 0;
    private static String sVersion = null;

    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");

    /**
    * A coordinate must be of the following form for Google Maps to correctly use it:
    * Latitude, Longitude
    *
    * This may be in decimal form:
    * Latitude: {-90 to 90}
    * Longitude: {-180 to 180}
    *
    * Or, in degrees, minutes, and seconds:
    * Latitude: {-90 to 90}° {0 to 59}' {0 to 59}"
    * Latitude: {-180 to 180}° {0 to 59}' {0 to 59}"
    * + or - degrees may also be represented with N or n, S or s for latitude, and with
    * E or e, W or w for longitude, where the direction may either precede or follow the value.
    *
    * Some examples of coordinates that will be accepted by the regex:
    * 37.422081°, -122.084576°
    * 37.422081,-122.084576
    * +37°25'19.49", -122°5'4.47"
    * 37°25'19.49"N, 122°5'4.47"W
    * N 37° 25' 19.49",  W 122° 5' 4.47"
    **/
    private static final String COORD_DEGREES_LATITUDE =
            "([-+NnSs]" + "(\\s)*)?"
            + "[1-9]?[0-9](\u00B0)" + "(\\s)*"
            + "([1-5]?[0-9]\')?" + "(\\s)*"
            + "([1-5]?[0-9]" + "(\\.[0-9]+)?\")?"
            + "((\\s)*" + "[NnSs])?";
    private static final String COORD_DEGREES_LONGITUDE =
            "([-+EeWw]" + "(\\s)*)?"
            + "(1)?[0-9]?[0-9](\u00B0)" + "(\\s)*"
            + "([1-5]?[0-9]\')?" + "(\\s)*"
            + "([1-5]?[0-9]" + "(\\.[0-9]+)?\")?"
            + "((\\s)*" + "[EeWw])?";
    private static final String COORD_DEGREES_PATTERN =
            COORD_DEGREES_LATITUDE
            + "(\\s)*" + "," + "(\\s)*"
            + COORD_DEGREES_LONGITUDE;
    private static final String COORD_DECIMAL_LATITUDE =
            "[+-]?"
            + "[1-9]?[0-9]" + "(\\.[0-9]+)"
            + "(\u00B0)?";
    private static final String COORD_DECIMAL_LONGITUDE =
            "[+-]?"
            + "(1)?[0-9]?[0-9]" + "(\\.[0-9]+)"
            + "(\u00B0)?";
    private static final String COORD_DECIMAL_PATTERN =
            COORD_DECIMAL_LATITUDE
            + "(\\s)*" + "," + "(\\s)*"
            + COORD_DECIMAL_LONGITUDE;
    private static final Pattern COORD_PATTERN =
            Pattern.compile(COORD_DEGREES_PATTERN + "|" + COORD_DECIMAL_PATTERN);

    private static final String NANP_ALLOWED_SYMBOLS = "()+-*#.";
    private static final int NANP_MIN_DIGITS = 7;
    private static final int NANP_MAX_DIGITS = 11;


    /**
     * Returns whether the SDK is the Jellybean release or later.
     */
    public static boolean isJellybeanOrLater() {
      return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Returns whether the SDK is the KeyLimePie release or later.
     */
    public static boolean isKeyLimePieOrLater() {
    	return false;
      //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    
    }

    public static int getViewTypeFromIntentAndSharedPref(Activity activity) {
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(activity);
        
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_VIEW)) {
            return ViewType.DAY;
        }
                
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_EDIT)) {
            return ViewType.EDIT;
        }
        
        if (extras != null) {
            if (extras.getBoolean(INTENT_KEY_DETAIL_VIEW, false)) {
                // This is the "detail" view which is either agenda or day view
                return prefs.getInt(SettingsFragment.KEY_DETAILED_VIEW,
                        SettingsFragment.DEFAULT_DETAILED_VIEW);
            } else if (INTENT_VALUE_VIEW_TYPE_DAY.equals(extras.getString(INTENT_KEY_VIEW_TYPE))) {
                // Not sure who uses this. This logic came from LaunchActivity
                return ViewType.DAY;
            }
        }

        // Default to the last view
        return prefs.getInt(
                SettingsFragment.KEY_START_VIEW, SettingsFragment.DEFAULT_START_VIEW);
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getWidgetUpdateAction(Context context) {
        return context.getPackageName() + ".APPWIDGET_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getWidgetScheduledUpdateAction(Context context) {
        return context.getPackageName() + ".APPWIDGET_SCHEDULED_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getSearchAuthority(Context context) {
        return context.getPackageName() + ".CalendarRecentSuggestionsProvider";
    }

    /**
     * Writes a new home time zone to the db. Updates the home time zone in the
     * db asynchronously and updates the local cache. Sending a time zone of
     * **tbd** will cause it to be set to the device's time zone. null or empty
     * tz will be ignored.
     *
     * @param context The calling activity
     * @param timeZone The time zone to set Calendar to, or **tbd**
     */
    public static void setTimeZone(Context context, String timeZone) {
        mTZUtils.setTimeZone(context, timeZone);
    }

    /**
     * Gets the time zone that Calendar should be displayed in This is a helper
     * method to get the appropriate time zone for Calendar. If this is the
     * first time this method has been called it will initiate an asynchronous
     * query to verify that the data in preferences is correct. The callback
     * supplied will only be called if this query returns a value other than
     * what is stored in preferences and should cause the calling activity to
     * refresh anything that depends on calling this method.
     *
     * @param context The calling activity
     * @param callback The runnable that should execute if a query returns new
     *            values
     * @return The string value representing the time zone Calendar should
     *         display
     */
    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }


    public static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
        return mTZUtils.formatDateRange(context, startMillis, endMillis, flags);
    }

    public static boolean getDefaultVibrate(Context context, SharedPreferences prefs) {
        boolean vibrate;
        if (prefs.contains(KEY_ALERTS_VIBRATE_WHEN)) {
            // Migrate setting to new 4.2 behavior
            //
            // silent and never -> off
            // always -> on
            String vibrateWhen = prefs.getString(KEY_ALERTS_VIBRATE_WHEN, null);
            vibrate = vibrateWhen != null && vibrateWhen.equals(context
                    .getString(R.string.prefDefault_alerts_vibrate_true));
            prefs.edit().remove(KEY_ALERTS_VIBRATE_WHEN).commit();
            Log.d(TAG, "Migrating KEY_ALERTS_VIBRATE_WHEN(" + vibrateWhen
                    + ") to KEY_ALERTS_VIBRATE = " + vibrate);
        } else {
            vibrate = prefs.getBoolean(SettingsFragment.KEY_ALERTS_VIBRATE,
                    false);
        }
        return vibrate;
    }

    public static String[] getSharedPreference(Context context, String key, String[] defaultValue) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        Set<String> ss = prefs.getStringSet(key, null);
        if (ss != null) {
            String strings[] = new String[ss.size()];
            return ss.toArray(strings);
        }
        return defaultValue;
    }

    public static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public static boolean getSharedPreference(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        return prefs.getBoolean(key, defaultValue);
    }

    /**
     * Asynchronously sets the preference with the given key to the given value
     *
     * @param context the context to use to get preferences from
     * @param key the key of the preference to set
     * @param value the value to set
     */
    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, String[] values) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        prefs.edit().putStringSet(key, set).apply();
    }

    protected static void tardis() {
        mTardis = System.currentTimeMillis();
    }

    protected static long getTardis() {
        return mTardis;
    }

    public static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setSharedPreference(Context context, String key, int value) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void removeSharedPreference(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }

    // The backed up ring tone preference should not used because it is a device
    // specific Uri. The preference now lives in a separate non-backed-up
    // shared_pref file (SHARED_PREFS_NAME_NO_BACKUP). The preference in the old
    // backed-up shared_pref file (SHARED_PREFS_NAME) is used only to control the
    // default value when the ringtone dialog opens up.
    //
    // At backup manager "restore" time (which should happen before launcher
    // comes up for the first time), the value will be set/reset to default
    // ringtone.
    public static String getRingTonePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsFragment.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE);
        String ringtone = prefs.getString(SettingsFragment.KEY_ALERTS_RINGTONE, null);

        // If it hasn't been populated yet, that means new code is running for
        // the first time and restore hasn't happened. Migrate value from
        // backed-up shared_pref to non-shared_pref.
        if (ringtone == null) {
            // Read from the old place with a default of DEFAULT_RINGTONE
            ringtone = getSharedPreference(context, SettingsFragment.KEY_ALERTS_RINGTONE,
                    SettingsFragment.DEFAULT_RINGTONE);

            // Write it to the new place
            setRingTonePreference(context, ringtone);
        }

        return ringtone;
    }

    public static void setRingTonePreference(Context context, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsFragment.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE);
        prefs.edit().putString(SettingsFragment.KEY_ALERTS_RINGTONE, value).apply();
    }

    /**
     * Save default agenda/day/week/month view for next time
     *
     * @param context
     * @param viewId {@link CalendarController.ViewType}
     */
    static void setDefaultView(Context context, int viewId) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        boolean validDetailView = false;
        if (mAllowWeekForDetailView && viewId == CalendarController.ViewType.WEEK) {
            validDetailView = true;
        } else {
            validDetailView = viewId == CalendarController.ViewType.AGENDA
                    || viewId == CalendarController.ViewType.DAY;
        }

        if (validDetailView) {
            // Record the detail start view
            editor.putInt(SettingsFragment.KEY_DETAILED_VIEW, viewId);
        }

        // Record the (new) start view
        editor.putInt(SettingsFragment.KEY_START_VIEW, viewId);
        editor.apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        String[] columnNames = cursor.getColumnNames();
        if (columnNames == null) {
            columnNames = new String[] {};
        }
        MatrixCursor newCursor = new MatrixCursor(columnNames);
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }

    /**
     * Compares two cursors to see if they contain the same data.
     *
     * @return Returns true of the cursors contain the same data and are not
     *         null, false otherwise
     */
    public static boolean compareCursors(Cursor c1, Cursor c2) {
        if (c1 == null || c2 == null) {
            return false;
        }

        int numColumns = c1.getColumnCount();
        if (numColumns != c2.getColumnCount()) {
            return false;
        }

        if (c1.getCount() != c2.getCount()) {
            return false;
        }

        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while (c1.moveToNext() && c2.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                if (!TextUtils.equals(c1.getString(i), c2.getString(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * If the given intent specifies a time (in milliseconds since the epoch),
     * then that time is returned. Otherwise, the current time is returned.
     */
    public static final long timeFromIntentInMillis(Intent intent) {
        // If the time was specified, then use that. Otherwise, use the current
        // time.
        Uri data = intent.getData();
        long millis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time "
                            + "found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }

    /**
     * Formats the given Time object so that it gives the month and year (for
     * example, "September 2007").
     *
     * @param time the time to format
     * @return the string containing the weekday and the date
     */
    public static String formatMonthYear(Context context, Calendar time) {
        int flags = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_NO_MONTH_DAY
                | ETime.FORMAT_SHOW_YEAR;
        long millis = time.getTimeInMillis();
        return formatDateRange(context, millis, millis, flags);
    }

    /**
     * Returns a list joined together by the provided delimiter, for example,
     * ["a", "b", "c"] could be joined into "a,b,c"
     *
     * @param things the things to join together
     * @param delim the delimiter to use
     * @return a string contained the things joined together
     */
    public static String join(List<?> things, String delim) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object thing : things) {
            if (first) {
                first = false;
            } else {
                builder.append(delim);
            }
            builder.append(thing.toString());
        }
        return builder.toString();
    }


    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = Calendar.THURSDAY - firstDayOfWeek; // 1970/01/01 is THURSDAY�̴�.
        if (diff < 0) {
            diff += 7;
        }
        int refDay = ETime.EPOCH_JULIAN_DAY - diff; // if) firstDayOfWeek is SUNDAY, refDay is 2440584[2440588 - 4 = 2440584]
        return (julianDay - refDay) / 7;
    }


    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    /*
    public static int getFirstDayOfWeek(Context context) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        String pref = prefs.getString(
                SettingsFragment.KEY_WEEK_START_DAY, SettingsFragment.WEEK_START_DEFAULT);

        int startDay;
        if (SettingsFragment.WEEK_START_DEFAULT.equals(pref)) {
            startDay = GregorianCalendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }

        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }
    */
    
    public static int getFirstDayOfWeek(Context context) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        int pref = prefs.getInt(
                SettingsFragment.KEY_WEEK_START_DAY, SettingsFragment.WEEK_START_DEFAULT);

        int startDay;
        if (SettingsFragment.WEEK_START_DEFAULT == pref) {
            startDay = GregorianCalendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = pref;
        }
        
        return startDay;
    }
    
    
    
    /**
     * Get last day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    /*
    public static int getLastDayOfWeek(Context context) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        String pref = prefs.getString(
                SettingsFragment.KEY_WEEK_START_DAY, SettingsFragment.WEEK_START_DEFAULT);

        int startDay;
        if (SettingsFragment.WEEK_START_DEFAULT.equals(pref)) {
            startDay = GregorianCalendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }

        if (startDay == Calendar.SATURDAY) {
            return Time.FRIDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.SUNDAY;
        } else {
            return Time.SATURDAY;
        }
    }
    */
    public static int getLastDayOfWeek(Context context) {
        SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        int pref = prefs.getInt(
                SettingsFragment.KEY_WEEK_START_DAY, SettingsFragment.WEEK_START_DEFAULT);

        int startDay;
        if (SettingsFragment.WEEK_START_DEFAULT == pref) {
            startDay = GregorianCalendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = pref;
        }

        return startDay;
        
        /*
        if (startDay == Calendar.SATURDAY) {
            return Time.FRIDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.SUNDAY;
        } else {
            return Time.SATURDAY;
        }
        */
    }
    
    
    public static final int SAT_START_DAY_NUMBERING_SATURDAY = 1;
    public static final int SAT_START_DAY_NUMBERING_SUNDAY = 2;
    public static final int SAT_START_DAY_NUMBERING_MONDAY = 3;
    public static final int SAT_START_DAY_NUMBERING_TUESDAY = 4;
    public static final int SAT_START_DAY_NUMBERING_WEDNESDAY = 5;
    public static final int SAT_START_DAY_NUMBERING_THURSDAY = 6;
    public static final int SAT_START_DAY_NUMBERING_FRIDAY = 7;
    
    
    public static final int MON_START_DAY_NUMBERING_MONDAY = 1;
    public static final int MON_START_DAY_NUMBERING_TUESDAY = 2;
    public static final int MON_START_DAY_NUMBERING_WEDNESDAY = 3;
    public static final int MON_START_DAY_NUMBERING_THURSDAY = 4;
    public static final int MON_START_DAY_NUMBERING_FRIDAY = 5;
    public static final int MON_START_DAY_NUMBERING_SATURDAY = 6; 
    public static final int MON_START_DAY_NUMBERING_SUNDAY = 7;
    
    public static int getDayNumbersOfWeek(int firstDayOfWeek, int targetDay) {
    	if (firstDayOfWeek == Calendar.SATURDAY) {
    		int targetDayNumbering = 0;
    		switch(targetDay) {
    		case Calendar.SATURDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_SATURDAY;
    			break;
    		case Calendar.SUNDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_SUNDAY;
    			break;
    		case Calendar.MONDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_MONDAY;
    			break;
    		case Calendar.TUESDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_TUESDAY;
    			break;
    		case Calendar.WEDNESDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_WEDNESDAY;
    			break;
    		case Calendar.THURSDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_THURSDAY;
    			break;
    		case Calendar.FRIDAY:
    			targetDayNumbering = SAT_START_DAY_NUMBERING_FRIDAY;
    			break;    		
    		}
    		
    		// ���� targetDay�� startDay�̶��,,,
    		return (SAT_START_DAY_NUMBERING_FRIDAY - targetDayNumbering) + 1;    	
    		
        } else if (firstDayOfWeek == Calendar.MONDAY) {
        	int targetDayNumbering = 0;
    		switch(targetDay) {    		
    		case Calendar.MONDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_MONDAY;
    			break;
    		case Calendar.TUESDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_TUESDAY;
    			break;
    		case Calendar.WEDNESDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_WEDNESDAY;
    			break;
    		case Calendar.THURSDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_THURSDAY;
    			break;
    		case Calendar.FRIDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_FRIDAY;
    			break;
    		case Calendar.SATURDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_SATURDAY;
    			break;
    		case Calendar.SUNDAY:
    			targetDayNumbering = MON_START_DAY_NUMBERING_SUNDAY;
    			break;
    		}
    		
    		return (MON_START_DAY_NUMBERING_SUNDAY - targetDayNumbering) + 1;
            
        } else {
            return (Calendar.SATURDAY - targetDay) + 1;
        }
    }

    
    /**
     * Get first day of week as java.util.Calendar constant.
     *
     * @return the first day of week as a java.util.Calendar constant
     */
    
    
	
    public static String getFristDayOfWeekString(Context context) {
    	// DateFormatSymbols.getInstance().getWeekdays()
    	// : Returns the array of strings containing the full names of the days of the week. 
    	//   Use the java.util.Calendar constants Calendar.SUNDAY etc. as indices for the array.
    	String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
		int indexOfWeekStartDay = Utils.getFirstDayOfWeek(context);
		return namesOfDays[indexOfWeekStartDay];
    }
    
    /*public static int getFirstDayOfWeekAsCalendar(Context context) {
        return convertDayOfWeekFromTimeToCalendar(getFirstDayOfWeek(context));
    }
    
    //Converts the day of the week from android.text.format.Time to java.util.Calendar
    
    public static int convertDayOfWeekFromTimeToCalendar(int timeDayOfWeek) {
        switch (timeDayOfWeek) {
            case Time.MONDAY:
                return Calendar.MONDAY;
            case Time.TUESDAY:
                return Calendar.TUESDAY;
            case Time.WEDNESDAY:
                return Calendar.WEDNESDAY;
            case Time.THURSDAY:
                return Calendar.THURSDAY;
            case Time.FRIDAY:
                return Calendar.FRIDAY;
            case Time.SATURDAY:
                return Calendar.SATURDAY;
            case Time.SUNDAY:
                return Calendar.SUNDAY;
            default:
                throw new IllegalArgumentException("Argument must be between Time.SUNDAY and " +
                        "Time.SATURDAY");
        }
    }
    */
    /**
     * @return true when week number should be shown.
     */
    public static boolean getShowWeekNumber(Context context) {
        final SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        return prefs.getBoolean(
                SettingsFragment.KEY_SHOW_WEEK_NUM, SettingsFragment.DEFAULT_SHOW_WEEK_NUM);
    }

    /**
     * @return true when declined events should be hidden.
     */
    public static boolean getHideDeclinedEvents(Context context) {
        final SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        return prefs.getBoolean(SettingsFragment.KEY_HIDE_DECLINED, false);
    }

    public static int getDaysPerWeek(Context context) {
        final SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        return prefs.getInt(SettingsFragment.KEY_DAYS_PER_WEEK, 7);
    }

    /**
     * Determine whether the column position is Saturday or not.
     *
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Saturday position
     */
    
    public static boolean isSaturday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Calendar.SUNDAY && column == 6)
                || (firstDayOfWeek == Calendar.MONDAY && column == 5)
                || (firstDayOfWeek == Calendar.SATURDAY && column == 0);
    }
	
    /**
     * Determine whether the column position is Sunday or not.
     *
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Sunday position
     */
    
    public static boolean isSunday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Calendar.SUNDAY && column == 0)
                || (firstDayOfWeek == Calendar.MONDAY && column == 6)
                || (firstDayOfWeek == Calendar.SATURDAY && column == 1);
    }
	
    /**
     * Convert given UTC time into current local time. This assumes it is for an
     * allday event and will adjust the time to be on a midnight boundary.
     *
     * @param recycle Time object to recycle, otherwise null.
     * @param utcTime Time to convert, in UTC.
     * @param tz The time zone to convert this time to.
     */
    /*
    public static long convertAlldayUtcToLocal(Time recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = Time.TIMEZONE_UTC;
        recycle.set(utcTime);
        recycle.timezone = tz;
        return recycle.normalize(true);
    }
    */
    public static long convertAlldayUtcToLocal(Calendar recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = GregorianCalendar.getInstance();
        }
        
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        recycle.setTimeZone(timezone); //Time.TIMEZONE_UTC;
        recycle.setTimeInMillis(utcTime);
        
        TimeZone gotTimezone = TimeZone.getTimeZone(tz);
        recycle.setTimeZone(gotTimezone); 
        return recycle.getTimeInMillis();
    }

    /*
    public static long convertAlldayLocalToUTC(Time recycle, long localTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(localTime);
        recycle.timezone = Time.TIMEZONE_UTC;
        return recycle.normalize(true);
    }
    */
    
    public static long convertAlldayLocalToUTC(Calendar recycle, long localTime, String tz) {
        if (recycle == null) {
            recycle = GregorianCalendar.getInstance();
        }        
        
        TimeZone gotTimezone = TimeZone.getTimeZone(tz);
        recycle.setTimeZone(gotTimezone); 
        recycle.setTimeInMillis(localTime);
        
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        recycle.setTimeZone(timezone); 
        
        return recycle.getTimeInMillis();
    }

    /**
     * Finds and returns the next midnight after "theTime" in milliseconds UTC
     *
     * @param recycle - Time object to recycle, otherwise null.
     * @param theTime - Time used for calculations (in UTC)
     * @param tz The time zone to convert this time to.
     */
    public static long getNextMidnight(Calendar recycle, long theTime, String tz) {
        if (recycle == null) {
            recycle = GregorianCalendar.getInstance();
        }
        
        TimeZone timezone = TimeZone.getTimeZone(tz);
        
        recycle.setTimeZone(timezone);
        recycle.setTimeInMillis(theTime);
        
        recycle.add(Calendar.DAY_OF_MONTH, 1);//recycle.monthDay ++;
        recycle.set(Calendar.HOUR_OF_DAY, 0);
        recycle.set(Calendar.MINUTE, 0);
        recycle.set(Calendar.SECOND, 0);
                        
        return recycle.getTimeInMillis();
    }

    /**
     * Scan through a cursor of calendars and check if names are duplicated.
     * This travels a cursor containing calendar display names and fills in the
     * provided map with whether or not each name is repeated.
     *
     * @param isDuplicateName The map to put the duplicate check results in.
     * @param cursor The query of calendars to check
     * @param nameIndex The column of the query that contains the display name
     */
    public static void checkForDuplicateNames(
            Map<String, Boolean> isDuplicateName, Cursor cursor, int nameIndex) {
        isDuplicateName.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(nameIndex);
            // Set it to true if we've seen this name before, false otherwise
            if (displayName != null) {
                isDuplicateName.put(displayName, isDuplicateName.containsKey(displayName));
            }
        }
    }


    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static void setAllowWeekForDetailView(boolean allowWeekView) {
        mAllowWeekForDetailView  = allowWeekView;
    }

    public static boolean getAllowWeekForDetailView() {
        return mAllowWeekForDetailView;
    }

    public static boolean getConfigBool(Context c, int key) {
        return c.getResources().getBoolean(key);
    }

    /**
     * For devices with Jellybean or later, darkens the given color to ensure that white text is
     * clearly visible on top of it.  For devices prior to Jellybean, does nothing, as the
     * sync adapter handles the color change.
     *
     * @param color
     */
    public static int getDisplayColorFromColor(int color) {
        if (!isJellybeanOrLater()) {
            return color;
        }

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(hsv[1] * SATURATION_ADJUST, 1.0f);
        hsv[2] = hsv[2] * INTENSITY_ADJUST;
        return Color.HSVToColor(hsv);
    }

    // This takes a color and computes what it would look like blended with white. 
    // The result is the color that should be used for declined events.
    public static int getDeclinedColorFromColor(int color) {
        int bg = 0xffffffff;
        int a = DECLINED_EVENT_ALPHA;
        int r = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
        return (0xff000000) | ((r | g | b) >> 8);
    }

    // A single strand represents one color of events. Events are divided up by
    // color to make them convenient to draw. The black strand is special in
    // that it holds conflicting events as well as color settings for allday on
    // each day.
    public static class DNAStrand {
        public float[] points;
        public int[] allDays; // color for the allday, 0 means no event
        int position;
        public int color;
        int count;
    }

    // A segment is a single continuous length of time occupied by a single
    // color. Segments should never span multiple days.
    private static class DNASegment {
        int startMinute; // in minutes since the start of the week
        int endMinute;
        int color; // Calendar color or black for conflicts
        int day; // quick reference to the day this segment is on
    }

    public static HashMap<Integer, DNAStrand> createDNAStrands(int firstJulianDay,
            ArrayList<Event> events, int top, int bottom, int minPixels, int[] dayXs,
            Context context) {

        if (!mMinutesLoaded) {
            if (context == null) {
                Log.wtf(TAG, "No context and haven't loaded parameters yet! Can't create DNA.");
            }
            Resources res = context.getResources();
            CONFLICT_COLOR = res.getColor(R.color.month_dna_conflict_time_color);
            WORK_DAY_START_MINUTES = res.getInteger(R.integer.work_start_minutes);
            WORK_DAY_END_MINUTES = res.getInteger(R.integer.work_end_minutes);
            WORK_DAY_END_LENGTH = DAY_IN_MINUTES - WORK_DAY_END_MINUTES;
            WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
            mMinutesLoaded = true;
        }

        if (events == null || events.isEmpty() || dayXs == null || dayXs.length < 1
                || bottom - top < 8 || minPixels < 0) {
            Log.e(TAG,
                    "Bad values for createDNAStrands! events:" + events + " dayXs:"
                            + Arrays.toString(dayXs) + " bot-top:" + (bottom - top) + " minPixels:"
                            + minPixels);
            return null;
        }

        LinkedList<DNASegment> segments = new LinkedList<DNASegment>();
        HashMap<Integer, DNAStrand> strands = new HashMap<Integer, DNAStrand>();
        // add a black strand by default, other colors will get added in
        // the loop
        DNAStrand blackStrand = new DNAStrand();
        blackStrand.color = CONFLICT_COLOR;
        strands.put(CONFLICT_COLOR, blackStrand);
        // the min length is the number of minutes that will occupy
        // MIN_SEGMENT_PIXELS in the 'work day' time slot. This computes the
        // minutes/pixel * minpx where the number of pixels are 3/4 the total
        // dna height: 4*(mins/(px * 3/4))
        int minMinutes = minPixels * 4 * WORK_DAY_MINUTES / (3 * (bottom - top));

        // There are slightly fewer than half as many pixels in 1/6 the space,
        // so round to 2.5x for the min minutes in the non-work area
        int minOtherMinutes = minMinutes * 5 / 2;
        int lastJulianDay = firstJulianDay + dayXs.length - 1;

        Event event = new Event();
        // Go through all the events for the week
        for (Event currEvent : events) {
            // if this event is outside the weeks range skip it
            if (currEvent.endDay < firstJulianDay || currEvent.startDay > lastJulianDay) {
                continue;
            }
            if (currEvent.drawAsAllday()) {
                addAllDayToStrands(currEvent, strands, firstJulianDay, dayXs.length);
                continue;
            }
            // Copy the event over so we can clip its start and end to our range
            currEvent.copyTo(event);
            if (event.startDay < firstJulianDay) {
                event.startDay = firstJulianDay;
                event.startTime = 0;
            }
            // If it starts after the work day make sure the start is at least
            // minPixels from midnight
            if (event.startTime > DAY_IN_MINUTES - minOtherMinutes) {
                event.startTime = DAY_IN_MINUTES - minOtherMinutes;
            }
            if (event.endDay > lastJulianDay) {
                event.endDay = lastJulianDay;
                event.endTime = DAY_IN_MINUTES - 1;
            }
            // If the end time is before the work day make sure it ends at least
            // minPixels after midnight
            if (event.endTime < minOtherMinutes) {
                event.endTime = minOtherMinutes;
            }
            // If the start and end are on the same day make sure they are at
            // least minPixels apart. This only needs to be done for times
            // outside the work day as the min distance for within the work day
            // is enforced in the segment code.
            if (event.startDay == event.endDay &&
                    event.endTime - event.startTime < minOtherMinutes) {
                // If it's less than minPixels in an area before the work
                // day
                if (event.startTime < WORK_DAY_START_MINUTES) {
                    // extend the end to the first easy guarantee that it's
                    // minPixels
                    event.endTime = Math.min(event.startTime + minOtherMinutes,
                            WORK_DAY_START_MINUTES + minMinutes);
                    // if it's in the area after the work day
                } else if (event.endTime > WORK_DAY_END_MINUTES) {
                    // First try shifting the end but not past midnight
                    event.endTime = Math.min(event.endTime + minOtherMinutes, DAY_IN_MINUTES - 1);
                    // if it's still too small move the start back
                    if (event.endTime - event.startTime < minOtherMinutes) {
                        event.startTime = event.endTime - minOtherMinutes;
                    }
                }
            }

            // This handles adding the first segment
            if (segments.size() == 0) {
                addNewSegment(segments, event, strands, firstJulianDay, 0, minMinutes);
                continue;
            }
            // Now compare our current start time to the end time of the last
            // segment in the list
            DNASegment lastSegment = segments.getLast();
            int startMinute = (event.startDay - firstJulianDay) * DAY_IN_MINUTES + event.startTime;
            int endMinute = Math.max((event.endDay - firstJulianDay) * DAY_IN_MINUTES
                    + event.endTime, startMinute + minMinutes);

            if (startMinute < 0) {
                startMinute = 0;
            }
            if (endMinute >= WEEK_IN_MINUTES) {
                endMinute = WEEK_IN_MINUTES - 1;
            }
            // If we start before the last segment in the list ends we need to
            // start going through the list as this may conflict with other
            // events
            if (startMinute < lastSegment.endMinute) {
                int i = segments.size();
                // find the last segment this event intersects with
                while (--i >= 0 && endMinute < segments.get(i).startMinute);

                DNASegment currSegment;
                // for each segment this event intersects with
                for (; i >= 0 && startMinute <= (currSegment = segments.get(i)).endMinute; i--) {
                    // if the segment is already a conflict ignore it
                    if (currSegment.color == CONFLICT_COLOR) {
                        continue;
                    }
                    // if the event ends before the segment and wouldn't create
                    // a segment that is too small split off the right side
                    if (endMinute < currSegment.endMinute - minMinutes) {
                        DNASegment rhs = new DNASegment();
                        rhs.endMinute = currSegment.endMinute;
                        rhs.color = currSegment.color;
                        rhs.startMinute = endMinute + 1;
                        rhs.day = currSegment.day;
                        currSegment.endMinute = endMinute;
                        segments.add(i + 1, rhs);
                        strands.get(rhs.color).count++;
                        if (DEBUG) {
                            Log.d(TAG, "Added rhs, curr:" + currSegment.toString() + " i:"
                                    + segments.get(i).toString());
                        }
                    }
                    // if the event starts after the segment and wouldn't create
                    // a segment that is too small split off the left side
                    if (startMinute > currSegment.startMinute + minMinutes) {
                        DNASegment lhs = new DNASegment();
                        lhs.startMinute = currSegment.startMinute;
                        lhs.color = currSegment.color;
                        lhs.endMinute = startMinute - 1;
                        lhs.day = currSegment.day;
                        currSegment.startMinute = startMinute;
                        // increment i so that we are at the right position when
                        // referencing the segments to the right and left of the
                        // current segment.
                        segments.add(i++, lhs);
                        strands.get(lhs.color).count++;
                        if (DEBUG) {
                            Log.d(TAG, "Added lhs, curr:" + currSegment.toString() + " i:"
                                    + segments.get(i).toString());
                        }
                    }
                    // if the right side is black merge this with the segment to
                    // the right if they're on the same day and overlap
                    if (i + 1 < segments.size()) {
                        DNASegment rhs = segments.get(i + 1);
                        if (rhs.color == CONFLICT_COLOR && currSegment.day == rhs.day
                                && rhs.startMinute <= currSegment.endMinute + 1) {
                            rhs.startMinute = Math.min(currSegment.startMinute, rhs.startMinute);
                            segments.remove(currSegment);
                            strands.get(currSegment.color).count--;
                            // point at the new current segment
                            currSegment = rhs;
                        }
                    }
                    // if the left side is black merge this with the segment to
                    // the left if they're on the same day and overlap
                    if (i - 1 >= 0) {
                        DNASegment lhs = segments.get(i - 1);
                        if (lhs.color == CONFLICT_COLOR && currSegment.day == lhs.day
                                && lhs.endMinute >= currSegment.startMinute - 1) {
                            lhs.endMinute = Math.max(currSegment.endMinute, lhs.endMinute);
                            segments.remove(currSegment);
                            strands.get(currSegment.color).count--;
                            // point at the new current segment
                            currSegment = lhs;
                            // point i at the new current segment in case new
                            // code is added
                            i--;
                        }
                    }
                    // if we're still not black, decrement the count for the
                    // color being removed, change this to black, and increment
                    // the black count
                    if (currSegment.color != CONFLICT_COLOR) {
                        strands.get(currSegment.color).count--;
                        currSegment.color = CONFLICT_COLOR;
                        strands.get(CONFLICT_COLOR).count++;
                    }
                }

            }
            // If this event extends beyond the last segment add a new segment
            if (endMinute > lastSegment.endMinute) {
                addNewSegment(segments, event, strands, firstJulianDay, lastSegment.endMinute,
                        minMinutes);
            }
        }
        weaveDNAStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
        return strands;
    }

    // This figures out allDay colors as allDay events are found
    private static void addAllDayToStrands(Event event, HashMap<Integer, DNAStrand> strands,
            int firstJulianDay, int numDays) {
        DNAStrand strand = getOrCreateStrand(strands, CONFLICT_COLOR);
        // if we haven't initialized the allDay portion create it now
        if (strand.allDays == null) {
            strand.allDays = new int[numDays];
        }

        // For each day this event is on update the color
        int end = Math.min(event.endDay - firstJulianDay, numDays - 1);
        for (int i = Math.max(event.startDay - firstJulianDay, 0); i <= end; i++) {
            if (strand.allDays[i] != 0) {
                // if this day already had a color, it is now a conflict
                strand.allDays[i] = CONFLICT_COLOR;
            } else {
                // else it's just the color of the event
                strand.allDays[i] = event.color;
            }
        }
    }

    // This processes all the segments, sorts them by color, and generates a
    // list of points to draw
    private static void weaveDNAStrands(LinkedList<DNASegment> segments, int firstJulianDay,
            HashMap<Integer, DNAStrand> strands, int top, int bottom, int[] dayXs) {
        // First, get rid of any colors that ended up with no segments
        Iterator<DNAStrand> strandIterator = strands.values().iterator();
        while (strandIterator.hasNext()) {
            DNAStrand strand = strandIterator.next();
            if (strand.count < 1 && strand.allDays == null) {
                strandIterator.remove();
                continue;
            }
            strand.points = new float[strand.count * 4];
            strand.position = 0;
        }
        // Go through each segment and compute its points
        for (DNASegment segment : segments) {
            // Add the points to the strand of that color
            DNAStrand strand = strands.get(segment.color);
            int dayIndex = segment.day - firstJulianDay;
            int dayStartMinute = segment.startMinute % DAY_IN_MINUTES;
            int dayEndMinute = segment.endMinute % DAY_IN_MINUTES;
            int height = bottom - top;
            int workDayHeight = height * 3 / 4;
            int remainderHeight = (height - workDayHeight) / 2;

            int x = dayXs[dayIndex];
            int y0 = 0;
            int y1 = 0;

            y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
            y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
            if (DEBUG) {
                Log.d(TAG, "Adding " + Integer.toHexString(segment.color) + " at x,y0,y1: " + x
                        + " " + y0 + " " + y1 + " for " + dayStartMinute + " " + dayEndMinute);
            }
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y0;
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y1;
        }
    }

    /**
     * Compute a pixel offset from the top for a given minute from the work day
     * height and the height of the top area.
     */
    private static int getPixelOffsetFromMinutes(int minute, int workDayHeight,
            int remainderHeight) {
        int y;
        if (minute < WORK_DAY_START_MINUTES) {
            y = minute * remainderHeight / WORK_DAY_START_MINUTES;
        } else if (minute < WORK_DAY_END_MINUTES) {
            y = remainderHeight + (minute - WORK_DAY_START_MINUTES) * workDayHeight
                    / WORK_DAY_MINUTES;
        } else {
            y = remainderHeight + workDayHeight + (minute - WORK_DAY_END_MINUTES) * remainderHeight
                    / WORK_DAY_END_LENGTH;
        }
        return y;
    }

    /**
     * Add a new segment based on the event provided. This will handle splitting
     * segments across day boundaries and ensures a minimum size for segments.
     */
    private static void addNewSegment(LinkedList<DNASegment> segments, Event event,
            HashMap<Integer, DNAStrand> strands, int firstJulianDay, int minStart, int minMinutes) {
        if (event.startDay > event.endDay) {
            Log.wtf(TAG, "Event starts after it ends: " + event.toString());
        }
        // If this is a multiday event split it up by day
        if (event.startDay != event.endDay) {
            Event lhs = new Event();
            lhs.color = event.color;
            lhs.startDay = event.startDay;
            // the first day we want the start time to be the actual start time
            lhs.startTime = event.startTime;
            lhs.endDay = lhs.startDay;
            lhs.endTime = DAY_IN_MINUTES - 1;
            // Nearly recursive iteration!
            while (lhs.startDay != event.endDay) {
                addNewSegment(segments, lhs, strands, firstJulianDay, minStart, minMinutes);
                // The days in between are all day, even though that shouldn't
                // actually happen due to the allday filtering
                lhs.startDay++;
                lhs.endDay = lhs.startDay;
                lhs.startTime = 0;
                minStart = 0;
            }
            // The last day we want the end time to be the actual end time
            lhs.endTime = event.endTime;
            event = lhs;
        }
        // Create the new segment and compute its fields
        DNASegment segment = new DNASegment();
        int dayOffset = (event.startDay - firstJulianDay) * DAY_IN_MINUTES;
        int endOfDay = dayOffset + DAY_IN_MINUTES - 1;
        // clip the start if needed
        segment.startMinute = Math.max(dayOffset + event.startTime, minStart);
        // and extend the end if it's too small, but not beyond the end of the
        // day
        int minEnd = Math.min(segment.startMinute + minMinutes, endOfDay);
        segment.endMinute = Math.max(dayOffset + event.endTime, minEnd);
        if (segment.endMinute > endOfDay) {
            segment.endMinute = endOfDay;
        }

        segment.color = event.color;
        segment.day = event.startDay;
        segments.add(segment);
        // increment the count for the correct color or add a new strand if we
        // don't have that color yet
        DNAStrand strand = getOrCreateStrand(strands, segment.color);
        strand.count++;
    }

    /**
     * Try to get a strand of the given color. Create it if it doesn't exist.
     */
    private static DNAStrand getOrCreateStrand(HashMap<Integer, DNAStrand> strands, int color) {
        DNAStrand strand = strands.get(color);
        if (strand == null) {
            strand = new DNAStrand();
            strand.color = color;
            strand.count = 0;
            strands.put(strand.color, strand);
        }
        return strand;
    }

    /**
     * Sends an intent to launch the top level Calendar view.
     *
     * @param context
     */
    public static void returnToCalendarHome(Context context) {
        //Intent launchIntent = new Intent(context, ECalendarMainActivity.class);
    	Intent launchIntent = new Intent(context, CentralActivity.class);
        launchIntent.setAction(Intent.ACTION_DEFAULT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtra(INTENT_KEY_HOME, true);
        context.startActivity(launchIntent);
    }

    /**
     * This sets up a search view to use Calendar's search suggestions provider
     * and to allow refining the search.
     *
     * @param view The {@link SearchView} to set up
     * @param act The activity using the view
     */
    public static void setUpSearchView(SearchView view, Activity act) {
        SearchManager searchManager = (SearchManager) act.getSystemService(Context.SEARCH_SERVICE);
        view.setSearchableInfo(searchManager.getSearchableInfo(act.getComponentName()));
        view.setQueryRefinementEnabled(true);
    }

    /**
     * Given a context and a time in millis since unix epoch figures out the
     * correct week of the year for that time.
     *
     * @param millisSinceEpoch
     * @return
     */
    public static int getWeekNumberFromTime(long millisSinceEpoch, Context context) {
    	
    	TimeZone timezone = TimeZone.getTimeZone(getTimeZone(context, null));
        Calendar weekTime = GregorianCalendar.getInstance(timezone);
        weekTime.setTimeInMillis(millisSinceEpoch);
        
        int firstDayOfWeek = getFirstDayOfWeek(context);
        // if the date is on Saturday or Sunday and the start of the week
        // isn't Monday we may need to shift the date to be in the correct
        // week
        if (weekTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                && (firstDayOfWeek == Calendar.SUNDAY || firstDayOfWeek == Calendar.SATURDAY)) {
        	weekTime.add(Calendar.DAY_OF_MONTH, 1);//weekTime.monthDay++;
            
        } else if (weekTime.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY && firstDayOfWeek == Calendar.SATURDAY) {
        	weekTime.add(Calendar.DAY_OF_MONTH, 2);//weekTime.monthDay += 2;
            
        }
        
        return weekTime.get(Calendar.WEEK_OF_YEAR);
        //return weekTime.getWeekNumber();
    }

    /**
     * Formats a day of the week string. This is either just the name of the day
     * or a combination of yesterday/today/tomorrow and the day of the week.
     *
     * @param julianDay The julian day to get the string for
     * @param todayJulianDay The julian day for today's date
     * @param millis A utc millis since epoch time that falls on julian day
     * @param context The calling context, used to get the timezone and do the
     *            formatting
     * @return
     */
    public static String getDayOfWeekString(int julianDay, int todayJulianDay, long millis,
            Context context) {
        getTimeZone(context, null);
        int flags = ETime.FORMAT_SHOW_WEEKDAY;
        String dayViewText;
        if (julianDay == todayJulianDay) {
            dayViewText = context.getString(R.string.agenda_today,
                    mTZUtils.formatDateRange(context, millis, millis, flags).toString());
        } else if (julianDay == todayJulianDay - 1) {
            dayViewText = context.getString(R.string.agenda_yesterday,
                    mTZUtils.formatDateRange(context, millis, millis, flags).toString());
        } else if (julianDay == todayJulianDay + 1) {
            dayViewText = context.getString(R.string.agenda_tomorrow,
                    mTZUtils.formatDateRange(context, millis, millis, flags).toString());
        } else {
            dayViewText = mTZUtils.formatDateRange(context, millis, millis, flags).toString();
        }
        dayViewText = dayViewText.toUpperCase();
        return dayViewText;
    }

    public static void setPerMinuteUpdater(Handler h, Runnable r, String tzId) {
        if (h == null || r == null || tzId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        TimeZone timezone = TimeZone.getTimeZone(tzId);        
        Calendar time = GregorianCalendar.getInstance(timezone);        
        time.setTimeInMillis(now);
        int second = time.get(Calendar.SECOND);
        long runInMillis = (60 - second + 1) * 1000;
        h.removeCallbacks(r);
        h.postDelayed(r, runInMillis);
    }
    
    // Calculate the time until midnight + 1 second and set the handler to
    // do run the runnable
    public static void setMidnightUpdater(Handler h, Runnable r, String tzId) {
        if (h == null || r == null || tzId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        TimeZone timezone = TimeZone.getTimeZone(tzId);        
        Calendar time = GregorianCalendar.getInstance(timezone); 
        time.setTimeInMillis(now);        
        // 24 value is hours per 1 day
        // 3600 value is seconds per 1 hour
        // 60 value is seconds per 1 minute
        // 1 value is interpolation???
        // 1000 value is milliseconds per 1 secnod
        
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        
        
        long runInMillis = (24 * 3600 - hour * 3600 - minute * 60 -
                second + 1) * 1000;
        h.removeCallbacks(r);
        h.postDelayed(r, runInMillis);
    }

    // Stop the midnight update thread
    public static void resetMidnightUpdater(Handler h, Runnable r) {
        if (h == null || r == null) {
            return;
        }
        h.removeCallbacks(r);
    }

    /**
     * Returns a string description of the specified time interval.
     */
    public static String getDisplayedDatetime(long startMillis, long endMillis, long currentMillis,
            String localTimezoneId, boolean allDay, Context context) {
        // Configure date/time formatting.
        int flagsDate = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_SHOW_WEEKDAY;
        int flagsTime = ETime.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(context)) {
            flagsTime |= ETime.FORMAT_24HOUR;
        }

        int firstDayOfWeek = getFirstDayOfWeek(context);
        TimeZone localTimezone = TimeZone.getTimeZone(localTimezoneId);        
        
        Calendar currentTime = GregorianCalendar.getInstance(localTimezone);//new Time(localTimezone);
        currentTime.setTimeInMillis(currentMillis);
        
        Resources resources = context.getResources();
        String datetimeString = null;
        if (allDay) {
            // All day events require special timezone adjustment.
            long localStartMillis = convertAlldayUtcToLocal(null, startMillis, localTimezoneId);
            long localEndMillis = convertAlldayUtcToLocal(null, endMillis, localTimezoneId);
            
            if (singleDayEvent(localStartMillis, localEndMillis, localTimezone, firstDayOfWeek)) {
                // If possible, use "Today" or "Tomorrow" instead of a full date string.
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(),
                        localStartMillis, currentMillis, localTimezone, firstDayOfWeek);
                if (TODAY == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.today);
                } else if (TOMORROW == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.tomorrow);
                }
            }
            if (datetimeString == null) {
                // For multi-day allday events or single-day all-day events that are not
                // today or tomorrow, use framework formatter.
                Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
                datetimeString = ETime.formatDateTimeRange(context, f, startMillis,
                        endMillis, flagsDate, ETime.TIMEZONE_UTC).toString();
            }
        } else {
            if (singleDayEvent(startMillis, endMillis, localTimezone, firstDayOfWeek)) {
                // Format the time.
                String timeString = Utils.formatDateRange(context, startMillis, endMillis,
                        flagsTime);

                // If possible, use "Today" or "Tomorrow" instead of a full date string.
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(), startMillis,
                        currentMillis, localTimezone, firstDayOfWeek);
                if (TODAY == todayOrTomorrow) {
                    // Example: "Today at 1:00pm ~ 2:00 pm"
                    datetimeString = resources.getString(R.string.today_at_time_fmt,
                            timeString);
                } else if (TOMORROW == todayOrTomorrow) {
                    // Example: "Tomorrow at 1:00pm ~ 2:00 pm"
                    datetimeString = resources.getString(R.string.tomorrow_at_time_fmt,
                            timeString);
                } else {
                    // Format the full date. Example: "Thursday, April 12, 1:00pm ~ 2:00pm"
                    String dateString = Utils.formatDateRange(context, startMillis, endMillis,
                            flagsDate);
                    datetimeString = resources.getString(R.string.date_time_fmt, dateString,
                            timeString);
                }
            } else {
                // For multiday events, shorten day/month names.
                // Example format: "Fri Apr 6, 5:00pm - Sun, Apr 8, 6:00pm"
                int flagsDatetime = flagsDate | flagsTime | ETime.FORMAT_ABBREV_MONTH |
                		ETime.FORMAT_ABBREV_WEEKDAY;
                datetimeString = Utils.formatDateRange(context, startMillis, endMillis,
                        flagsDatetime);
            }
        }
        return datetimeString;
    }
    
    /**
     * Returns a string description of the specified time interval.
     */
    public static String getDisplayedEventStarttime(long startMillis, long endMillis, long currentMillis,
            String localTimezoneId, Context context) {
        // Configure date/time formatting.
        int flagsDate = ETime.FORMAT_SHOW_DATE | ETime.FORMAT_SHOW_WEEKDAY;
        int flagsTime = ETime.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(context)) {
            flagsTime |= ETime.FORMAT_24HOUR;
        }

        TimeZone localTimezone = TimeZone.getTimeZone(localTimezoneId);        
        long gmtoff = (localTimezone.getRawOffset()) / 1000;
        
        Calendar currentTime = GregorianCalendar.getInstance(localTimezone);
        currentTime.setTimeInMillis(currentMillis);
        
        Resources resources = context.getResources();
        String datetimeString = null;          
        
        // Format the time.
        String timeString = Utils.formatDateRange(context, startMillis, endMillis,
                flagsTime);        
        // Format the full date. Example: "Thursday, April 12, 1:00pm ~ 2:00pm"
        String dateString = Utils.formatDateRange(context, startMillis, endMillis,
                flagsDate);
        
        datetimeString = resources.getString(R.string.date_time_fmt, dateString,
                timeString);
            
        
        
        return datetimeString;
    }

    /**
     * Returns the timezone to display in the event info, if the local timezone is different
     * from the event timezone.  Otherwise returns null.
     */
    public static String getDisplayedTimezone(long startMillis, String localTimezone,
            String eventTimezone) {
        String tzDisplay = null;
        if (!TextUtils.equals(localTimezone, eventTimezone)) {
            // Figure out if this is in DST
            TimeZone tz = TimeZone.getTimeZone(localTimezone);
            if (tz == null || tz.getID().equals("GMT")) {
                tzDisplay = localTimezone;
            } else {
                Calendar startTime = GregorianCalendar.getInstance(tz);//new Time(localTimezone);
                startTime.setTimeInMillis(startMillis);
                tzDisplay = tz.getDisplayName(false, TimeZone.SHORT);
            }
        }
        return tzDisplay;
    }

    /**
     * Returns whether the specified time interval is in a single day.
     */
    private static boolean singleDayEvent(long startMillis, long endMillis, TimeZone timezone, int firstDayOfWeek) {
        if (startMillis == endMillis) {
            return true;
        }

        // An event ending at midnight should still be a single-day event, so check
        // time end-1.
        int startDay = ETime.getJulianDay(startMillis, timezone, firstDayOfWeek);
        int endDay = ETime.getJulianDay(endMillis - 1, timezone, firstDayOfWeek);
        return startDay == endDay;
    }

    // Using int constants as a return value instead of an enum to minimize resources.
    private static final int TODAY = 1;
    private static final int TOMORROW = 2;
    private static final int NONE = 0;

    /**
     * Returns TODAY or TOMORROW if applicable.  Otherwise returns NONE.
     */
    private static int isTodayOrTomorrow(Resources r, long dayMillis,
            long currentMillis, TimeZone timezone, int firstDayOfWeek) {
        int startDay = ETime.getJulianDay(dayMillis, timezone, firstDayOfWeek);
        int currentDay = ETime.getJulianDay(currentMillis, timezone, firstDayOfWeek);

        int days = startDay - currentDay;
        if (days == 1) {
            return TOMORROW;
        } else if (days == 0) {
            return TODAY;
        } else {
            return NONE;
        }
    }

    /**
     * Create an intent for emailing attendees of an event.
     *
     * @param resources The resources for translating strings.
     * @param eventTitle The title of the event to use as the email subject.
     * @param body The default text for the email body.
     * @param toEmails The list of emails for the 'to' line.
     * @param ccEmails The list of emails for the 'cc' line.
     * @param ownerAccount The owner account to use as the email sender.
     */
    public static Intent createEmailAttendeesIntent(Resources resources, String eventTitle,
            String body, List<String> toEmails, List<String> ccEmails, String ownerAccount) {
        List<String> toList = toEmails;
        List<String> ccList = ccEmails;
        if (toEmails.size() <= 0) {
            if (ccEmails.size() <= 0) {
                // TODO: Return a SEND intent if no one to email to, to at least populate
                // a draft email with the subject (and no recipients).
                throw new IllegalArgumentException("Both toEmails and ccEmails are empty.");
            }

            // Email app does not work with no "to" recipient.  Move all 'cc' to 'to'
            // in this case.
            toList = ccEmails;
            ccList = null;
        }

        // Use the event title as the email subject (prepended with 'Re: ').
        String subject = null;
        if (eventTitle != null) {
            subject = resources.getString(R.string.email_subject_prefix) + eventTitle;
        }

        // Use the SENDTO intent with a 'mailto' URI, because using SEND will cause
        // the picker to show apps like text messaging, which does not make sense
        // for email addresses.  We put all data in the URI instead of using the extra
        // Intent fields (ie. EXTRA_CC, etc) because some email apps might not handle
        // those (though gmail does).
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("mailto");

        // We will append the first email to the 'mailto' field later (because the
        // current state of the Email app requires it).  Add the remaining 'to' values
        // here.  When the email codebase is updated, we can simplify this.
        if (toList.size() > 1) {
            for (int i = 1; i < toList.size(); i++) {
                // The Email app requires repeated parameter settings instead of
                // a single comma-separated list.
                uriBuilder.appendQueryParameter("to", toList.get(i));
            }
        }

        // Add the subject parameter.
        if (subject != null) {
            uriBuilder.appendQueryParameter("subject", subject);
        }

        // Add the subject parameter.
        if (body != null) {
            uriBuilder.appendQueryParameter("body", body);
        }

        // Add the cc parameters.
        if (ccList != null && ccList.size() > 0) {
            for (String email : ccList) {
                uriBuilder.appendQueryParameter("cc", email);
            }
        }

        // Insert the first email after 'mailto:' in the URI manually since Uri.Builder
        // doesn't seem to have a way to do this.
        String uri = uriBuilder.toString();
        if (uri.startsWith("mailto:")) {
            StringBuilder builder = new StringBuilder(uri);
            builder.insert(7, Uri.encode(toList.get(0)));
            uri = builder.toString();
        }

        // Start the email intent.  Email from the account of the calendar owner in case there
        // are multiple email accounts.
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO, Uri.parse(uri));
        emailIntent.putExtra("fromAccountString", ownerAccount);

        // Workaround a Email bug that overwrites the body with this intent extra.  If not
        // set, it clears the body.
        if (body != null) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        }

        return Intent.createChooser(emailIntent, resources.getString(R.string.email_picker_label));
    }

    /**
     * Example fake email addresses used as attendee emails are resources like conference rooms,
     * or another calendar, etc.  These all end in "calendar.google.com".
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.endsWith(MACHINE_GENERATED_ADDRESS);
    }

    /**
     * Returns true if:
     *   (1) the email is not a resource like a conference room or another calendar.
     *       Catch most of these by filtering out suffix calendar.google.com.
     *   (2) the email is not equal to the sync account to prevent mailing himself.
     */
    public static boolean isEmailableFrom(String email, String syncAccountName) {
        return Utils.isValidEmail(email) && !email.equals(syncAccountName);
    }

    /**
     * Inserts a drawable with today's day into the today's icon in the option menu
     * @param icon - today's icon from the options menu
     */
    public static void setTodayIcon(LayerDrawable icon, Context c, String timezoneId) {
        DayOfMonthDrawable today;

        // Reuse current drawable if possible
        Drawable currentDrawable = icon.findDrawableByLayerId(R.id.today_icon_day);
        if (currentDrawable != null && currentDrawable instanceof DayOfMonthDrawable) {
            today = (DayOfMonthDrawable)currentDrawable;
        } else {
            today = new DayOfMonthDrawable(c);
        }
        // Set the day and update the icon
        TimeZone timezone = TimeZone.getTimeZone(timezoneId);
        
        Calendar now = GregorianCalendar.getInstance(timezone);
        now.setTimeInMillis(System.currentTimeMillis());
        
        today.setDayOfMonth(now.get(Calendar.DAY_OF_MONTH));
        icon.mutate();
        icon.setDrawableByLayerId(R.id.today_icon_day, today);
    }
    
    /**
     * Inserts a drawable with today's day into the today's icon in the option menu
     * @param icon - today's icon from the options menu
     */
    public static void setPreviousIcon(LayerDrawable icon, Context c) {
    	PreviousIconDrawable prvIcon;

        // Reuse current drawable if possible
        Drawable currentDrawable = icon.findDrawableByLayerId(R.id.previous_icon_previous);
        
        if (currentDrawable != null && currentDrawable instanceof PreviousIconDrawable) {
        	prvIcon = (PreviousIconDrawable)currentDrawable;
        } else {
        	prvIcon = new PreviousIconDrawable(c);
        }
         
        prvIcon.changeIcon();
        icon.mutate();
        icon.setDrawableByLayerId(R.id.previous_icon_previous, prvIcon);
    }

    private static class CalendarBroadcastReceiver extends BroadcastReceiver {

        Runnable mCallBack;

        public CalendarBroadcastReceiver(Runnable callback) {
            super();
            mCallBack = callback;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DATE_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                if (mCallBack != null) {
                    mCallBack.run();
                }
            }
        }
    }

    public static BroadcastReceiver setTimeChangesReceiver(Context c, Runnable callback) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);

        CalendarBroadcastReceiver r = new CalendarBroadcastReceiver(callback);
        c.registerReceiver(r, filter);
        return r;
    }

    public static void clearTimeChangesReceiver(Context c, BroadcastReceiver r) {
        c.unregisterReceiver(r);
    }

    /**
     * Get a list of quick responses used for emailing guests from the
     * SharedPreferences. If not are found, get the hard coded ones that shipped
     * with the app
     *
     * @param context
     * @return a list of quick responses.
     */
    public static String[] getQuickResponses(Context context) {
        String[] s = Utils.getSharedPreference(context, KEY_QUICK_RESPONSES, (String[]) null);

        if (s == null) {
            s = context.getResources().getStringArray(R.array.quick_response_defaults);
        }

        return s;
    }

    /**
     * Return the app version code.
     */
    public static String getVersionCode(Context context) {
        if (sVersion == null) {
            try {
                sVersion = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Can't find version; just leave it blank.
                Log.e(TAG, "Error finding package " + context.getApplicationInfo().packageName);
            }
        }
        return sVersion;
    }

    /**
     * Checks the server for an updated list of Calendars (in the background).
     *
     * If a Calendar is added on the web (and it is selected and not
     * hidden) then it will be added to the list of calendars on the phone
     * (when this finishes).  When a new calendar from the
     * web is added to the phone, then the events for that calendar are also
     * downloaded from the web.
     *
     * This sync is done automatically in the background when the
     * SelectCalendars activity and fragment are started.
     *
     * @param account - The account to sync. May be null to sync all accounts.
     */
    public static void startCalendarMetafeedSync(Account account) {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean("metafeedonly", true);
        ContentResolver.requestSync(account, Calendars.CONTENT_URI.getAuthority(), extras);
    }

    /**
     * Replaces stretches of text that look like addresses and phone numbers with clickable
     * links. If lastDitchGeo is true, then if no links are found in the textview, the entire
     * string will be converted to a single geo link. Any spans that may have previously been
     * in the text will be cleared out.
     * <p>
     * This is really just an enhanced version of Linkify.addLinks().
     *
     * @param text - The string to search for links.
     * @param lastDitchGeo - If no links are found, turn the entire string into one geo link.
     * @return Spannable object containing the list of URL spans found.
     */
    public static Spannable extendedLinkify(String text, boolean lastDitchGeo) {
        // We use a copy of the string argument so it's available for later if necessary.
        Spannable spanText = SpannableString.valueOf(text);

        /*
         * If the text includes a street address like "1600 Amphitheater Parkway, 94043",
         * the current Linkify code will identify "94043" as a phone number and invite
         * you to dial it (and not provide a map link for the address).  For outside US,
         * use Linkify result iff it spans the entire text.  Otherwise send the user to maps.
         */
        String defaultPhoneRegion = System.getProperty("user.region", "US");
        if (!defaultPhoneRegion.equals("US")) {
            Linkify.addLinks(spanText, Linkify.ALL);

            // If Linkify links the entire text, use that result.
            URLSpan[] spans = spanText.getSpans(0, spanText.length(), URLSpan.class);
            if (spans.length == 1) {
                int linkStart = spanText.getSpanStart(spans[0]);
                int linkEnd = spanText.getSpanEnd(spans[0]);
                if (linkStart <= indexFirstNonWhitespaceChar(spanText) &&
                        linkEnd >= indexLastNonWhitespaceChar(spanText) + 1) {
                    return spanText;
                }
            }

            // Otherwise, to be cautious and to try to prevent false positives, reset the spannable.
            spanText = SpannableString.valueOf(text);
            // If lastDitchGeo is true, default the entire string to geo.
            if (lastDitchGeo && !text.isEmpty()) {
                Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
            }
            return spanText;
        }

        /*
         * For within US, we want to have better recognition of phone numbers without losing
         * any of the existing annotations.  Ideally this would be addressed by improving Linkify.
         * For now we manage it as a second pass over the text.
         *
         * URIs and e-mail addresses are pretty easy to pick out of text.  Phone numbers
         * are a bit tricky because they have radically different formats in different
         * countries, in terms of both the digits and the way in which they are commonly
         * written or presented (e.g. the punctuation and spaces in "(650) 555-1212").
         * The expected format of a street address is defined in WebView.findAddress().  It's
         * pretty narrowly defined, so it won't often match.
         *
         * The RFC 3966 specification defines the format of a "tel:" URI.
         *
         * Start by letting Linkify find anything that isn't a phone number.  We have to let it
         * run first because every invocation removes all previous URLSpan annotations.
         *
         * Ideally we'd use the external/libphonenumber routines, but those aren't available
         * to unbundled applications.
         */
        boolean linkifyFoundLinks = Linkify.addLinks(spanText,
                Linkify.ALL & ~(Linkify.PHONE_NUMBERS));

        /*
         * Get a list of any spans created by Linkify, for the coordinate overlapping span check.
         */
        URLSpan[] existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Check for coordinates.
         * This must be done before phone numbers because longitude may look like a phone number.
         */
        Matcher coordMatcher = COORD_PATTERN.matcher(spanText);
        int coordCount = 0;
        while (coordMatcher.find()) {
            int start = coordMatcher.start();
            int end = coordMatcher.end();
            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                continue;
            }

            URLSpan span = new URLSpan("geo:0,0?q=" + coordMatcher.group());
            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            coordCount++;
        }

        /*
         * Update the list of existing spans, for the phone number overlapping span check.
         */
        existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Search for phone numbers.
         *
         * Some URIs contain strings of digits that look like phone numbers.  If both the URI
         * scanner and the phone number scanner find them, we want the URI link to win.  Since
         * the URI scanner runs first, we just need to avoid creating overlapping spans.
         */
        int[] phoneSequences = findNanpPhoneNumbers(text);

        /*
         * Insert spans for the numbers we found.  We generate "tel:" URIs.
         */
        int phoneCount = 0;
        for (int match = 0; match < phoneSequences.length / 2; match++) {
            int start = phoneSequences[match*2];
            int end = phoneSequences[match*2 + 1];

            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                continue;
            }

            /*
             * The Linkify code takes the matching span and strips out everything that isn't a
             * digit or '+' sign.  We do the same here.  Extension numbers will get appended
             * without a separator, but the dialer wasn't doing anything useful with ";ext="
             * anyway.
             */

            //String dialStr = phoneUtil.format(match.number(),
            //        PhoneNumberUtil.PhoneNumberFormat.RFC3966);
            StringBuilder dialBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char ch = spanText.charAt(i);
                if (ch == '+' || Character.isDigit(ch)) {
                    dialBuilder.append(ch);
                }
            }
            URLSpan span = new URLSpan("tel:" + dialBuilder.toString());

            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            phoneCount++;
        }

        /*
         * If lastDitchGeo, and no other links have been found, set the entire string as a geo link.
         */
        if (lastDitchGeo && !text.isEmpty() &&
                !linkifyFoundLinks && phoneCount == 0 && coordCount == 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "No linkification matches, using geo default");
            }
            Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
        }

        return spanText;
    }

    private static int indexFirstNonWhitespaceChar(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int indexLastNonWhitespaceChar(CharSequence str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds North American Numbering Plan (NANP) phone numbers in the input text.
     *
     * @param text The text to scan.
     * @return A list of [start, end) pairs indicating the positions of phone numbers in the input.
     */
    // @VisibleForTesting
    static int[] findNanpPhoneNumbers(CharSequence text) {
        ArrayList<Integer> list = new ArrayList<Integer>();

        int startPos = 0;
        int endPos = text.length() - NANP_MIN_DIGITS + 1;
        if (endPos < 0) {
            return new int[] {};
        }

        /*
         * We can't just strip the whitespace out and crunch it down, because the whitespace
         * is significant.  March through, trying to figure out where numbers start and end.
         */
        while (startPos < endPos) {
            // skip whitespace
            while (Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                startPos++;
            }
            if (startPos == endPos) {
                break;
            }

            // check for a match at this position
            int matchEnd = findNanpMatchEnd(text, startPos);
            if (matchEnd > startPos) {
                list.add(startPos);
                list.add(matchEnd);
                startPos = matchEnd;    // skip past match
            } else {
                // skip to next whitespace char
                while (!Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                    startPos++;
                }
            }
        }

        int[] result = new int[list.size()];
        for (int i = list.size() - 1; i >= 0; i--) {
            result[i] = list.get(i);
        }
        return result;
    }

    /**
     * Checks to see if there is a valid phone number in the input, starting at the specified
     * offset.  If so, the index of the last character + 1 is returned.  The input is assumed
     * to begin with a non-whitespace character.
     *
     * @return Exclusive end position, or -1 if not a match.
     */
    private static int findNanpMatchEnd(CharSequence text, int startPos) {
        /*
         * A few interesting cases:
         *   94043                              # too short, ignore
         *   123456789012                       # too long, ignore
         *   +1 (650) 555-1212                  # 11 digits, spaces
         *   (650) 555 5555                     # Second space, only when first is present.
         *   (650) 555-1212, (650) 555-1213     # two numbers, return first
         *   1-650-555-1212                     # 11 digits with leading '1'
         *   *#650.555.1212#*!                  # 10 digits, include #*, ignore trailing '!'
         *   555.1212                           # 7 digits
         *
         * For the most part we want to break on whitespace, but it's common to leave a space
         * between the initial '1' and/or after the area code.
         */

        // Check for "tel:" URI prefix.
        if (text.length() > startPos+4
                && text.subSequence(startPos, startPos+4).toString().equalsIgnoreCase("tel:")) {
            startPos += 4;
        }

        int endPos = text.length();
        int curPos = startPos;
        int foundDigits = 0;
        char firstDigit = 'x';
        boolean foundWhiteSpaceAfterAreaCode = false;

        while (curPos <= endPos) {
            char ch;
            if (curPos < endPos) {
                ch = text.charAt(curPos);
            } else {
                ch = 27;    // fake invalid symbol at end to trigger loop break
            }

            if (Character.isDigit(ch)) {
                if (foundDigits == 0) {
                    firstDigit = ch;
                }
                foundDigits++;
                if (foundDigits > NANP_MAX_DIGITS) {
                    // too many digits, stop early
                    return -1;
                }
            } else if (Character.isWhitespace(ch)) {
                if ( (firstDigit == '1' && foundDigits == 4) ||
                        (foundDigits == 3)) {
                    foundWhiteSpaceAfterAreaCode = true;
                } else if (firstDigit == '1' && foundDigits == 1) {
                } else if (foundWhiteSpaceAfterAreaCode
                        && ( (firstDigit == '1' && (foundDigits == 7)) || (foundDigits == 6))) {
                } else {
                    break;
                }
            } else if (NANP_ALLOWED_SYMBOLS.indexOf(ch) == -1) {
                break;
            }
            // else it's an allowed symbol

            curPos++;
        }

        if ((firstDigit != '1' && (foundDigits == 7 || foundDigits == 10)) ||
                (firstDigit == '1' && foundDigits == 11)) {
            // match
            return curPos;
        }

        return -1;
    }

    /**
     * Determines whether a new span at [start,end) will overlap with any existing span.
     */
    private static boolean spanWillOverlap(Spannable spanText, URLSpan[] spanList, int start,
            int end) {
        if (start == end) {
            // empty span, ignore
            return false;
        }
        for (URLSpan span : spanList) {
            int existingStart = spanText.getSpanStart(span);
            int existingEnd = spanText.getSpanEnd(span);
            if ((start >= existingStart && start < existingEnd) ||
                    end > existingStart && end <= existingEnd) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    CharSequence seq = spanText.subSequence(start, end);
                    Log.v(TAG, "Not linkifying " + seq + " as phone number due to overlap");
                }
                return true;
            }
        }

        return false;
    }

    /**
     * @param bundle The incoming bundle that contains the reminder info.
     * @return ArrayList<ReminderEntry> of the reminder minutes and methods.
     */
    public static ArrayList<ReminderEntry> readRemindersFromBundle(Bundle bundle) {
        ArrayList<ReminderEntry> reminders = null;

        // ���� EventInfoFragment�� �������� �ʾұ� ������ �ּ����� ó��
        /*ArrayList<Integer> reminderMinutes = bundle.getIntegerArrayList(
                        EventInfoFragment.BUNDLE_KEY_REMINDER_MINUTES);
        ArrayList<Integer> reminderMethods = bundle.getIntegerArrayList(
                EventInfoFragment.BUNDLE_KEY_REMINDER_METHODS);*/
        
        // EventInfoFragment �̱������� ���� psudo definition!!!!
        ArrayList<Integer> reminderMinutes = new ArrayList<Integer>();
        ArrayList<Integer> reminderMethods = new ArrayList<Integer>();
        
        if (reminderMinutes == null || reminderMethods == null) {
            if (reminderMinutes != null || reminderMethods != null) {
                String nullList = (reminderMinutes == null?
                        "reminderMinutes" : "reminderMethods");
                Log.d(TAG, String.format("Error resolving reminders: %s was null",
                        nullList));
            }
            return null;
        }

        int numReminders = reminderMinutes.size();
        if (numReminders == reminderMethods.size()) {
            // Only if the size of the reminder minutes we've read in is
            // the same as the size of the reminder methods. Otherwise,
            // something went wrong with bundling them.
            reminders = new ArrayList<ReminderEntry>(numReminders);
            for (int reminder_i = 0; reminder_i < numReminders;
                    reminder_i++) {
                int minutes = reminderMinutes.get(reminder_i);
                int method = reminderMethods.get(reminder_i);
                reminders.add(ReminderEntry.valueOf(minutes, method));
            }
        } else {
            Log.d(TAG, String.format("Error resolving reminders." +
                        " Found %d reminderMinutes, but %d reminderMethods.",
                    numReminders, reminderMethods.size()));
        }

        return reminders;
    }
    
    private static final String ACCOUNT_UNIQUE_KEY = "ACCOUNT_KEY";
    private static final String[] CALENDAR_GROUP_PROJECTION = new String[] {
        Calendars._ID,
        Calendars.ACCOUNT_TYPE,
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE + " || " + Calendars.ACCOUNT_NAME + " AS " +
                ACCOUNT_UNIQUE_KEY,
    };
    private static final int CALENDAR_GROUP_PROJECTION_ID_COLUMN = 0;
    private static final int CALENDAR_GROUP_PROJECTION_ACCOUNT_TYPE = 1;
    private static final int CALENDAR_GROUP_PROJECTION_ACCOUNT_NAME = 2;
    public static final int CALENDAR_GROUP_PROJECTION_ACCOUNT_TYPE_OR_ACCOUNT_NAME_AS_ACCOUNT_UNIQUE_KEY = 3;
    
    private static final String IS_PRIMARY = "\"primary\"";
    private static final String[] CALENDARS_PROJECTION = new String[] {
        Calendars._ID,
        Calendars.ACCOUNT_NAME,
        Calendars.OWNER_ACCOUNT,
        Calendars.CALENDAR_DISPLAY_NAME,
        Calendars.CALENDAR_COLOR,
        Calendars.VISIBLE,
        Calendars.SYNC_EVENTS,
        "(" + Calendars.ACCOUNT_NAME + "=" + Calendars.OWNER_ACCOUNT + ") AS " + IS_PRIMARY,
        Calendars.ACCOUNT_TYPE
      };
      //Keep these in sync with the projection
      private static final int ID_COLUMN = 0;
      private static final int ACCOUNT_COLUMN = 1;
      private static final int OWNER_COLUMN = 2;
      private static final int NAME_COLUMN = 3;
      private static final int COLOR_COLUMN = 4;
      private static final int SELECTED_COLUMN = 5;
      private static final int SYNCED_COLUMN = 6;
      private static final int PRIMARY_COLUMN = 7;
      private static final int ACCOUNT_TYPE_COLUMN = 8;

	public static final String CALENDARS_WHERE_WRITEABLE_VISIBLE = Calendars.CALENDAR_ACCESS_LEVEL + ">="
			+ Calendars.CAL_ACCESS_CONTRIBUTOR + " AND " + Calendars.VISIBLE + "=1";

	static final String CALENDARS_WHERE = Calendars._ID + "=?";

	public static final String ECALENDAR_OWNER_ACCOUNT = "ECalendar";
    public static final String ECALENDAR_ACCOUNT_NAME = ECALENDAR_OWNER_ACCOUNT;
	public static final String ECALENDAR_ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL;
	public static final int ECALENDAR_COLOR = Color.RED;
	public static final int ECALENDAR_MAX_REMINDERS = 2;
	public static final String ECALENDAR_ALLOWED_REMINDERS = "0,1,2";
	public static final String ECALENDAR_ALLOWED_ATTENDEE_TYPES = "0,1,2";
	public static final String ECALENDAR_ALLOWED_AVAILABILITY = "0,1";
	//private static final String ECALENDAR_INT_NAME_PREFIX = "Local_";
	
	public static Uri buildQueryDefaultECalendarUri() {
        return CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")                
                .appendQueryParameter(Calendars.ACCOUNT_NAME, ECALENDAR_ACCOUNT_NAME)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, ECALENDAR_ACCOUNT_TYPE).build();
    }
	
	public static ContentValues buildInsertingNewECalendarContentValues(String displayName, int color) {
    	/* 
    	*When inserting a new calendar the following fields must be included:
    	-ACCOUNT_NAME
		-ACCOUNT_TYPE
		-NAME
		-CALENDAR_DISPLAY_NAME
		-CALENDAR_COLOR
		-CALENDAR_ACCESS_LEVEL
		-OWNER_ACCOUNT
		
		*The following Calendar columns are writable by both an app and a sync adapter.
		-NAME
		-CALENDAR_DISPLAY_NAME
		-VISIBLE
		-SYNC_EVENTS
		
		* If a local calendar is required an app can do so by inserting as a sync adapter and using an ACCOUNT_TYPE of ACCOUNT_TYPE_LOCAL.
    	*/
        String intName = displayName;
        final ContentValues cv = new ContentValues();
        cv.put(Calendars.ACCOUNT_NAME, ECALENDAR_ACCOUNT_NAME);
        cv.put(Calendars.ACCOUNT_TYPE, ECALENDAR_ACCOUNT_TYPE);
        cv.put(Calendars.NAME, intName);
        cv.put(Calendars.CALENDAR_DISPLAY_NAME, displayName);
        cv.put(Calendars.CALENDAR_COLOR, color);        
        cv.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER); //The level of access that the USER has for the calendar
        cv.put(Calendars.OWNER_ACCOUNT, ECALENDAR_OWNER_ACCOUNT);
        cv.put(Calendars.MAX_REMINDERS, 2); 
        cv.put(Calendars.ALLOWED_REMINDERS, "0,1,2"); 
        cv.put(Calendars.ALLOWED_ATTENDEE_TYPES, "0,1,2"); 
        cv.put(Calendars.ALLOWED_AVAILABILITY, "0,1"); 
        cv.put(Calendars.VISIBLE, 1);
        cv.put(Calendars.SYNC_EVENTS, 1); // ��Ȯ�� �뵵�� �𸣰ڴ�:calendars ���̺�� events ���̺��� sync�� �ǹ��ϴ� ����???
                                          // �ƴϸ�,,,����̽��� web server���� sync�� �ǹ��ϴ°�����...
        								  // �Ｚ Ķ������ �� ���� 1�� �����Ͽ���
                                          // Is this calendar synced and are its events stored on the device? 
                                          // :calendar ���̺� �ִ� �ش� ���̺� ������ events ���̺� �ִ� �ش� ���̺� �������� ��ũ�� �ǹ��ϴ� �� ����! 
        
        String a = Calendars.ALLOWED_ATTENDEE_TYPES;
        return cv;
    }
	/*
	public static ContentValues buildInsertingNewECalendarContentValues(String calendarPrefixName, String displayName, int color) {
    	
        String intName = calendarPrefixName + "_" + displayName;
        final ContentValues cv = new ContentValues();
        cv.put(Calendars.ACCOUNT_NAME, ECALENDAR_ACCOUNT_NAME);
        cv.put(Calendars.ACCOUNT_TYPE, ECALENDAR_ACCOUNT_TYPE);
        cv.put(Calendars.NAME, intName);
        cv.put(Calendars.CALENDAR_DISPLAY_NAME, displayName);
        cv.put(Calendars.CALENDAR_COLOR, color);        
        cv.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER); //The level of access that the USER has for the calendar
        cv.put(Calendars.OWNER_ACCOUNT, ECALENDAR_OWNER_ACCOUNT);
        cv.put(Calendars.MAX_REMINDERS, 2); 
        cv.put(Calendars.ALLOWED_REMINDERS, "0,1,2"); 
        cv.put(Calendars.VISIBLE, 1);
        cv.put(Calendars.SYNC_EVENTS, 1); // ��Ȯ�� �뵵�� �𸣰ڴ�:calendars ���̺�� events ���̺��� sync�� �ǹ��ϴ� ����???
                                          // �ƴϸ�,,,����̽��� web server���� sync�� �ǹ��ϴ°�����...
        								  // �Ｚ Ķ������ �� ���� 1�� �����Ͽ���
                                          // Is this calendar synced and are its events stored on the device? 
                                          // :calendar ���̺� �ִ� �ش� ���̺� ������ events ���̺� �ִ� �ش� ���̺� �������� ��ũ�� �ǹ��ϴ� �� ����! 
        
        String a = Calendars.ALLOWED_ATTENDEE_TYPES;
        return cv;
    }
	*/
    public static void makeDefaultECalendar(Context context) {
    	//int defaultCalendarId = -1;
    	String displayName = SettingsFragment.DEFAULT_ECALENDAR_CALENDAR_DISPLAYNAME;
    	int calendarColor = Color.RED;    	
		
		ContentResolver cr = context.getContentResolver();
		
		
		Uri syncAdapterUri = buildQueryDefaultECalendarUri();
		// Add calendar
		final ContentValues cv = buildInsertingNewECalendarContentValues(displayName, calendarColor);
		
		// �ش� URI�� Ķ������ �����Ϸ��� �Ѵٸ�, ���� ���
		// cv.put(Calendars.MAX_REMINDERS, 2) �׸��� �����ϸ�,
		// Only sync adapters may write to maxReminders ��� ������ �߻��Ѵ�
		// :maxReminders�� SyncAdapter�� writable �� �� �ִ� �ݷ��̴�
		// �׷��Ƿ� CalendarContract.CALLER_IS_SYNCADAPTER, "true" �� ����� URI�� �����ؾ� �Ѵ�
		//Uri resultUri = cr.insert(CalendarContract.Calendars.CONTENT_URI, cv); 		
		Uri resultUri = cr.insert(syncAdapterUri, cv);
		//cr.delete(url, where, selectionArgs);
		
		if (resultUri == null)
			throw new IllegalArgumentException();	
		
		final String[] projection = {Calendars._ID,				
									 Calendars.ACCOUNT_NAME,
									 Calendars.ACCOUNT_TYPE,
									 Calendars.OWNER_ACCOUNT,
									 Calendars.CALENDAR_COLOR,
									 Calendars.CALENDAR_DISPLAY_NAME,
									 Calendars.MAX_REMINDERS,
									 Calendars.ALLOWED_REMINDERS,
									 Calendars.ALLOWED_ATTENDEE_TYPES,
									 Calendars.ALLOWED_AVAILABILITY };		
		Cursor cursor = cr.query(resultUri, projection, null, null, null);
		try {
			if (cursor == null || !cursor.moveToFirst()) {
				//Log.e(Constants.TAG, "Query is empty after insert! AppOps disallows access to read or write calendar?");
				throw new IllegalArgumentException();
			}
			else {
				cursor.moveToPosition(-1);
		        while (cursor.moveToNext()) {
		        	int defCalendarId = cursor.getInt(cursor.getColumnIndexOrThrow(Calendars._ID));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, defCalendarId);	
		        	
		        	String defOwnerAccount = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_OWNER_ACCOUNT, defOwnerAccount);
		        	
		        	//KEY_DEFAULT_CALENDAR_ACCOUNT_NAME
		        	String defCalendarAccountName = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ACCOUNT_NAME, defCalendarAccountName);	
		        	
		        	//KEY_DEFAULT_CALENDAR_ACCOUNT_TYPE
		        	String defCalendarAccountType = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ACCOUNT_TYPE, defCalendarAccountType);	
		        		        	
		        	int defCalendarColor = cursor.getInt(cursor.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_COLOR, defCalendarColor);	
		        	
		        	
		        	String defCalendarDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_DISP_NAME, defCalendarDisplayName);
		        	
		        	
		        	int defCalendarMaxReminders = cursor.getInt(cursor.getColumnIndexOrThrow(Calendars.MAX_REMINDERS));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_MAX_REMINDERS, defCalendarMaxReminders);
		        	
		        	
		        	String defCalendarAllowedReminders = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.ALLOWED_REMINDERS));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ALLOWED_REMINDERS, defCalendarAllowedReminders);
		        	
		        	
		        	String defCalendarAllowedAttendeeTypes = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.ALLOWED_ATTENDEE_TYPES));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ALLOWED_ATTENDEE_TYPES, defCalendarAllowedAttendeeTypes);
		        	
		        	
		        	String defCalendarAllowedAvailability = cursor.getString(cursor.getColumnIndexOrThrow(Calendars.ALLOWED_AVAILABILITY));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ALLOWED_AVAILABILITY, defCalendarAllowedAvailability);
		        	return;
		        }
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
    }
    
    /*
    public static void makeDefaultECalendar(Context context, String calendarPrefixName) {
    	int defaultCalendarId = -1;
		
		ContentResolver cr = context.getContentResolver();
		String defaultDisplayName = SettingsFragment.DEFAULT_ECALENDAR_CALENDAR_DISPLAYNAME;
		
		Uri syncAdapterUri = buildQueryDefaultECalendarUri();
		// Add calendar
		final ContentValues cv = buildInsertingNewECalendarContentValues(calendarPrefixName, defaultDisplayName, Color.RED);		
		
		// �ش� URI�� Ķ������ �����Ϸ��� �Ѵٸ�, ���� ���
		// cv.put(Calendars.MAX_REMINDERS, 2) �׸��� �����ϸ�,
		// Only sync adapters may write to maxReminders ��� ������ �߻��Ѵ�
		// :maxReminders�� SyncAdapter�� writable �� �� �ִ� �ݷ��̴�
		// �׷��Ƿ� CalendarContract.CALLER_IS_SYNCADAPTER, "true" �� ����� URI�� �����ؾ� �Ѵ�
		//Uri resultUri = cr.insert(CalendarContract.Calendars.CONTENT_URI, cv); 		
		Uri resultUri = cr.insert(syncAdapterUri, cv);
		//cr.delete(url, where, selectionArgs);
		
		if (resultUri == null)
			throw new IllegalArgumentException();	
		
		final String[] projection = {Calendars._ID};		
		Cursor cursor = cr.query(buildQueryDefaultECalendarUri(), projection, null, null, null);
		try {
			if (cursor == null || !cursor.moveToFirst()) {
				//Log.e(Constants.TAG, "Query is empty after insert! AppOps disallows access to read or write calendar?");
				throw new IllegalArgumentException();
			}
			else {
				cursor.moveToPosition(-1);
		        while (cursor.moveToNext()) {
		        	defaultCalendarId = cursor.getInt(cursor.getColumnIndexOrThrow(Calendars._ID));
		        	Utils.setSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, defaultCalendarId);		        	
		        }
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
    }
    */
    
    public static int findDefaultCalendarID(Context context) {
		return Utils.getSharedPreference(context, SettingsFragment.KEY_DEFAULT_CALENDAR_ID, -1);   
	}	
    
    public static void deleteECalendar(Context context) {
    	ContentResolver cr = context.getContentResolver();
    	
    	//cr.delete(url, where, selectionArgs)
    }
    
    public static Cursor queryCalendarGroup(Context context) {
    	ContentResolver cr = context.getContentResolver();
    	Cursor cursor;	
    	
    	try {
            cursor = cr.query(Calendars.CONTENT_URI, CALENDAR_GROUP_PROJECTION,
            		"1) GROUP BY (" + ACCOUNT_UNIQUE_KEY, //Cheap hack to make WHERE a GROUP BY query
            		null /* selectionArgs */,
            		Calendars.ACCOUNT_NAME /*sort order*/);
            // Calling getCount() causes the cursor window to be filled,
            // which will make the first access on the main thread a lot faster.
            if (cursor != null) {
                cursor.getCount();
                // 
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception thrown during handling EVENT_ARG_QUERY", e);
            cursor = null;
        }
    	
    	return cursor;
    	/*
    	try {
            cursor = cr.query(Calendars.CONTENT_URI, CALENDARS_PROJECTION,
            		CALENDARS_WHERE_WRITEABLE_VISIBLE, null,
                    null);
            // Calling getCount() causes the cursor window to be filled,
            // which will make the first access on the main thread a lot faster.
            if (cursor != null) {
                cursor.getCount();
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception thrown during handling EVENT_ARG_QUERY", e);
            cursor = null;
        }
        */
    }
    
    
    public static void deleteDefaultCalendar(Context context) {
    	SharedPreferences prefs = SettingsFragment.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SettingsFragment.KEY_DEFAULT_CALENDAR_ID, -1);
        editor.apply();
    }
    
    /*
     // Trigger to remove a calendar's events when we delete the calendar
     db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars " +
                "BEGIN " +
                "DELETE FROM Events WHERE calendar_id = old._id;" +
                "END");
    */
    public static void deleteCalendar(Context context, int calendarId) {    
    	ContentResolver cr = context.getContentResolver();
    	// Calendars can be deleted either by the _ID as an appended id on the Uri or using any standard selection. 
    	// Deleting a calendar should generally be handled by a sync adapter as it will remove the calendar from the database and all associated data (aka events). 
    	String[] selArgs = new String[]{Integer.toString(calendarId)};
    	
    	int rowNumber = cr.delete(CalendarContract.Calendars.CONTENT_URI, 
    			"_id" + " =? ", 
    			selArgs);    	
    }
    
    public static void makeContentViewBitmap(Activity activity, View contentView) {
		contentView.destroyDrawingCache();
		
		contentView.setDrawingCacheEnabled(true);

		contentView.buildDrawingCache();

		Bitmap contentViewToBitmap = contentView.getDrawingCache();
				
		ECalendarApplication app = (ECalendarApplication) activity.getApplication();
		app.storeCalendarEntireRegionBitmap(contentViewToBitmap);
	}
    
    

}

