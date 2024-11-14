/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intheeast.acalendar;

import com.intheeast.etc.ETime;
import com.intheeast.event.EditEventHelper;
import com.intheeast.calendarcommon2.EventRecurrence;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
//import android.text.format.Time;
import android.widget.ArrayAdapter;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DeleteEventHelper {
    private final Activity mParent;
    private Context mContext;

    private long mStartMillis;
    private long mEndMillis;
    private CalendarEventModel mModel;

    /**
     * If true, then call finish() on the parent activity when done.
     */
    private boolean mExitWhenDone;
    // the runnable to execute when the delete is confirmed
    private Runnable mCallback;

    /**
     * These are the corresponding indices into the array of strings
     * "R.array.delete_repeating_labels" in the resource file.
     */
    public static final int DELETE_SELECTED = 0;
    public static final int DELETE_ALL_FOLLOWING = 1;
    public static final int DELETE_ALL = 2;

    private int mWhichDelete;
    private ArrayList<Integer> mWhichIndex;
    private AlertDialog mAlertDialog;
    private Dialog.OnDismissListener mDismissListener;

    private String mSyncId;

    private AsyncQueryService mService;

    private DeleteNotifyListener mDeleteStartedListener = null;

    public interface DeleteNotifyListener {
        public void onDeleteStarted();
    }


    public DeleteEventHelper(Context context, Activity parentActivity, boolean exitWhenDone) {
        if (exitWhenDone && parentActivity == null) {
            throw new IllegalArgumentException("parentActivity is required to exit when done");
        }

        mContext = context;
        mParent = parentActivity;
        // TODO move the creation of this service out into the activity.
        mService = new AsyncQueryService(mContext) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor == null) {
                    return;
                }
                cursor.moveToFirst();
                CalendarEventModel mModel = new CalendarEventModel();
                EditEventHelper.setModelFromCursor(mModel, cursor);
                cursor.close();
                DeleteEventHelper.this.delete(mStartMillis, mEndMillis, mModel, mWhichDelete);
            }
        };
        mExitWhenDone = exitWhenDone;
    }

    public void setExitWhenDone(boolean exitWhenDone) {
        mExitWhenDone = exitWhenDone;
    }

    /**
     * This callback is used when a normal event is deleted.
     */
    private DialogInterface.OnClickListener mDeleteNormalDialogListener =
            new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            deleteStarted();
            long id = mModel.mId; // mCursor.getInt(mEventIndexId);
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            mService.startDelete(mService.getNextToken(), null, uri, null, null, Utils.UNDO_DELAY);
            if (mCallback != null) {
                mCallback.run();
            }
            if (mExitWhenDone) {
                mParent.finish();
            }
        }
    };

    /**
     * This callback is used when an exception to an event is deleted
     */
    private DialogInterface.OnClickListener mDeleteExceptionDialogListener =
        new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            deleteStarted();
            deleteExceptionEvent();
            if (mCallback != null) {
                mCallback.run();
            }
            if (mExitWhenDone) {
                mParent.finish();
            }
        }
    };

    /**
     * This callback is used when a list item for a repeating event is selected
     */
    private DialogInterface.OnClickListener mDeleteListListener =
            new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            // set mWhichDelete to the delete type at that index
            mWhichDelete = mWhichIndex.get(button);

            // Enable the "ok" button now that the user has selected which
            // events in the series to delete.
            Button ok = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            ok.setEnabled(true);
        }
    };

    /**
     * This callback is used when a repeating event is deleted.
     */
    private DialogInterface.OnClickListener mDeleteRepeatingDialogListener =
            new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            deleteStarted();
            if (mWhichDelete != -1) {
                deleteRepeatingEvent(mWhichDelete);
            }
        }
    };


    public void delete(long begin, long end, long eventId, int which) {
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        mService.startQuery(mService.getNextToken(), null, uri, EditEventHelper.EVENT_PROJECTION,
                null, null, null);
        mStartMillis = begin;
        mEndMillis = end;
        mWhichDelete = which;
    }

    public void delete(long begin, long end, long eventId, int which, Runnable callback) {
        delete(begin, end, eventId, which);
        mCallback = callback;
    }


    public void delete(long begin, long end, CalendarEventModel model, int which) {
        mWhichDelete = which;
        mStartMillis = begin;
        mEndMillis = end;
        mModel = model;
        mSyncId = model.mSyncId;

        // If this is a repeating event, then pop up a dialog asking the
        // user if they want to delete all of the repeating events or
        // just some of them.
        String rRule = model.mRrule;
        String originalEvent = model.mOriginalSyncId;
        if (TextUtils.isEmpty(rRule)) {
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setMessage(R.string.delete_this_event_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(android.R.string.cancel, null).create();

            if (originalEvent == null) {
                // This is a normal event. Pop up a confirmation dialog.
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        mContext.getText(android.R.string.ok),
                        mDeleteNormalDialogListener);
            } else {
                // This is an exception event. Pop up a confirmation dialog.
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        mContext.getText(android.R.string.ok),
                        mDeleteExceptionDialogListener);
            }
            dialog.setOnDismissListener(mDismissListener);
            dialog.show();
            mAlertDialog = dialog;
        } else {
            // This is a repeating event.  Pop up a dialog asking which events
            // to delete.
            Resources res = mContext.getResources();
            ArrayList<String> labelArray = new ArrayList<String>(Arrays.asList(res
                    .getStringArray(R.array.delete_repeating_labels)));
            // asList doesn't like int[] so creating it manually.
            int[] labelValues = res.getIntArray(R.array.delete_repeating_values);
            ArrayList<Integer> labelIndex = new ArrayList<Integer>();
            for (int val : labelValues) {
                labelIndex.add(val);
            }

            if (mSyncId == null) {
                // remove 'Only this event' item
                labelArray.remove(0);
                labelIndex.remove(0);
                if (!model.mIsOrganizer) {
                    // remove 'This and future events' item
                    labelArray.remove(0);
                    labelIndex.remove(0);
                }
            } else if (!model.mIsOrganizer) {
                // remove 'This and future events' item
                labelArray.remove(1);
                labelIndex.remove(1);
            }
            if (which != -1) {
                // transform the which to the index in the array
                which = labelIndex.indexOf(which);
            }
            mWhichIndex = labelIndex;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                    android.R.layout.simple_list_item_single_choice, labelArray);
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setTitle(
                            mContext.getString(R.string.delete_recurring_event_title,model.mTitle))
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setSingleChoiceItems(adapter, which, mDeleteListListener)
                    .setPositiveButton(android.R.string.ok, mDeleteRepeatingDialogListener)
                    .setNegativeButton(android.R.string.cancel, null).show();
            dialog.setOnDismissListener(mDismissListener);
            mAlertDialog = dialog;

            if (which == -1) {
                // Disable the "Ok" button until the user selects which events
                // to delete.
                Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                ok.setEnabled(false);
            }
        }
    }

    private void deleteExceptionEvent() {
        long id = mModel.mId; // mCursor.getInt(mEventIndexId);

        // update a recurrence exception by setting its status to "canceled"
        ContentValues values = new ContentValues();
        values.put(Events.STATUS, Events.STATUS_CANCELED);

        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        mService.startUpdate(mService.getNextToken(), null, uri, values, null, null,
                Utils.UNDO_DELAY);
    }

    private void deleteRepeatingEvent(int which) {
        String rRule = mModel.mRrule;
        boolean allDay = mModel.mAllDay;
        long dtstart = mModel.mStart;
        long id = mModel.mId; // mCursor.getInt(mEventIndexId);

        switch (which) {
            case DELETE_SELECTED: {
                // If we are deleting the first event in the series, then
                // instead of creating a recurrence exception, just change
                // the start time of the recurrence.
                if (dtstart == mStartMillis) {
                    // TODO
                }

                // Create a recurrence exception by creating a new event
                // with the status "cancelled".
                ContentValues values = new ContentValues();

                // The title might not be necessary, but it makes it easier
                // to find this entry in the database when there is a problem.
                String title = mModel.mTitle;
                values.put(Events.TITLE, title);

                String timezone = mModel.mTimezone;
                long calendarId = mModel.mCalendarId;
                values.put(Events.EVENT_TIMEZONE, timezone);
                values.put(Events.ALL_DAY, allDay ? 1 : 0);
                values.put(Events.ORIGINAL_ALL_DAY, allDay ? 1 : 0);
                values.put(Events.CALENDAR_ID, calendarId);
                values.put(Events.DTSTART, mStartMillis);
                values.put(Events.DTEND, mEndMillis);
                values.put(Events.ORIGINAL_SYNC_ID, mSyncId);
                values.put(Events.ORIGINAL_ID, id);
                values.put(Events.ORIGINAL_INSTANCE_TIME, mStartMillis);
                values.put(Events.STATUS, Events.STATUS_CANCELED);

                mService.startInsert(mService.getNextToken(), null, Events.CONTENT_URI, values,
                        Utils.UNDO_DELAY);
                break;
            }
            case DELETE_ALL: {
                Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                mService.startDelete(mService.getNextToken(), null, uri, null, null,
                        Utils.UNDO_DELAY);
                break;
            }
            case DELETE_ALL_FOLLOWING: {
                // If we are deleting the first event in the series and all
                // following events, then delete them all.
                if (dtstart == mStartMillis) {
                    Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                    mService.startDelete(mService.getNextToken(), null, uri, null, null,
                            Utils.UNDO_DELAY);
                    break;
                }

                // Modify the repeating event to end just before this event time
                EventRecurrence eventRecurrence = new EventRecurrence();
                eventRecurrence.parse(rRule);
                Calendar date = GregorianCalendar.getInstance();//Time date = new Time();
                if (allDay) {
                    date.setTimeZone(TimeZone.getTimeZone(ETime.TIMEZONE_UTC));
                }
                date.setTimeInMillis(mStartMillis);
                date.add(Calendar.SECOND, -1);//date.second--;
                //date.normalize(false);           
                
                // Google calendar seems to require the UNTIL string to be
                // in UTC.
                //date.setTimeZone(TimeZone.getTimeZone(ETime.TIMEZONE_UTC)); // 여기서 문제이다...단지 타임존만 변경되었을 뿐...시간대가 변경된것은 아니다              
                date = ETime.switchTimezone(date, TimeZone.getTimeZone(ETime.TIMEZONE_UTC));
                
                eventRecurrence.until = ETime.format2445(date);//eventRecurrence.until = date.format2445();                
                
                ContentValues values = new ContentValues();
                values.put(Events.DTSTART, dtstart);
                values.put(Events.RRULE, eventRecurrence.toString());
                Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                mService.startUpdate(mService.getNextToken(), null, uri, values, null, null,
                        Utils.UNDO_DELAY);
                break;
            }
        }
        if (mCallback != null) {
            mCallback.run();
        }
        if (mExitWhenDone) {
            mParent.finish();
        }
    }

    public void setDeleteNotificationListener(DeleteNotifyListener listener) {
        mDeleteStartedListener = listener;
    }

    private void deleteStarted() {
        if (mDeleteStartedListener != null) {
            mDeleteStartedListener.onDeleteStarted();
        }
    }

    public void setOnDismissListener(Dialog.OnDismissListener listener) {
        if (mAlertDialog != null) {
            mAlertDialog.setOnDismissListener(listener);
        }
        mDismissListener = listener;
    }

    public void dismissAlertDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }
}
