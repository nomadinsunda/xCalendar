package com.intheeast.acalendar;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


import com.intheeast.calendarcommon2.EventRecurrence;
import com.intheeast.etc.ETime;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
//import android.text.format.Time;
import android.util.TimeFormatException;

public class RecurrenceContext {

	// Update android:maxLength in EditText as needed
    private static final int INTERVAL_MAX = 99;
    private static final int INTERVAL_DEFAULT = 1;
    // Update android:maxLength in EditText as needed
    private static final int COUNT_MAX = 730;
    private static final int COUNT_DEFAULT = 5;

    // Special cases in monthlyByNthDayOfWeek
    private static final int FIFTH_WEEK_IN_A_MONTH = 5;
    private static final int LAST_NTH_DAY_OF_WEEK = -1;
    
	private class RecurrenceModel implements Parcelable {

        // Should match EventRecurrence.DAILY, etc
        static final int FREQ_DAILY = 0;
        static final int FREQ_WEEKLY = 1;
        static final int FREQ_MONTHLY = 2;
        static final int FREQ_YEARLY = 3;

        static final int END_NEVER = 0;
        static final int END_BY_DATE = 1;
        static final int END_BY_COUNT = 2;

        static final int MONTHLY_BY_DATE = 0;
        static final int MONTHLY_BY_NTH_DAY_OF_WEEK = 1;

        static final int STATE_NO_RECURRENCE = 0;
        static final int STATE_RECURRENCE = 1;

        int recurrenceState;


        int freq = FREQ_WEEKLY;

        /**
         * INTERVAL: Every n days/weeks/months/years. n >= 1
         */
        int interval = INTERVAL_DEFAULT;

        int end;

        /**
         * UNTIL: Date of the last recurrence. Used when until == END_BY_DATE
         */
        Calendar endDate;

        /**
         * COUNT: Times to repeat. Use when until == END_BY_COUNT
         */
        int endCount = COUNT_DEFAULT;

        /**
         * BYDAY: Days of the week to be repeated. Sun = 0, Mon = 1, etc
         */
        boolean[] weeklyByDayOfWeek = new boolean[7];

        int monthlyRepeat;

        /**
         * Day of the month to repeat. Used when monthlyRepeat ==
         * MONTHLY_BY_DATE
         */
        int monthlyByMonthDay;

        /**
         * Day of the week to repeat. Used when monthlyRepeat ==
         * MONTHLY_BY_NTH_DAY_OF_WEEK
         */
        int monthlyByDayOfWeek;

        /**
         * Nth day of the week to repeat. Used when monthlyRepeat ==
         * MONTHLY_BY_NTH_DAY_OF_WEEK 0=undefined, -1=Last, 1=1st, 2=2nd, ..., 5=5th
         *
         * We support 5th, just to handle backwards capabilities with old bug, but it
         * gets converted to -1 once edited.
         */
        int monthlyByNthDayOfWeek;

        protected RecurrenceModel(Parcel in) {
            recurrenceState = in.readInt();
            freq = in.readInt();
            interval = in.readInt();
            end = in.readInt();
            endCount = in.readInt();
            weeklyByDayOfWeek = in.createBooleanArray();
            monthlyRepeat = in.readInt();
            monthlyByMonthDay = in.readInt();
            monthlyByDayOfWeek = in.readInt();
            monthlyByNthDayOfWeek = in.readInt();
        }

        public final Creator<RecurrenceModel> CREATOR = new Creator<RecurrenceModel>() {
            @Override
            public RecurrenceModel createFromParcel(Parcel in) {
                return new RecurrenceModel(in);
            }

            @Override
            public RecurrenceModel[] newArray(int size) {
                return new RecurrenceModel[size];
            }
        };

        /*
         * (generated method)
         */
        @Override
        public String toString() {
            return "Model [freq=" + freq + ", interval=" + interval + ", end=" + end + ", endDate="
                    + endDate + ", endCount=" + endCount + ", weeklyByDayOfWeek="
                    + Arrays.toString(weeklyByDayOfWeek) + ", monthlyRepeat=" + monthlyRepeat
                    + ", monthlyByMonthDay=" + monthlyByMonthDay + ", monthlyByDayOfWeek="
                    + monthlyByDayOfWeek + ", monthlyByNthDayOfWeek=" + monthlyByNthDayOfWeek + "]";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public RecurrenceModel() {
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(freq);
            dest.writeInt(interval);
            dest.writeInt(end);
            dest.writeInt(endDate.get(Calendar.YEAR));
            dest.writeInt(endDate.get(Calendar.MONTH));
            dest.writeInt(endDate.get(Calendar.DAY_OF_MONTH));
            dest.writeInt(endCount);
            dest.writeBooleanArray(weeklyByDayOfWeek);
            dest.writeInt(monthlyRepeat);
            dest.writeInt(monthlyByMonthDay);
            dest.writeInt(monthlyByDayOfWeek);
            dest.writeInt(monthlyByNthDayOfWeek);
            dest.writeInt(recurrenceState);
        }
    }
	
	private Resources mResources;
	private EventRecurrence mRecurrence = new EventRecurrence();
    private Calendar mTime = GregorianCalendar.getInstance();//new Time(); // TODO timezone?
    private RecurrenceModel mModel = new RecurrenceModel();
    //private Toast mToast;

    private final int[] TIME_DAY_TO_CALENDAR_DAY = new int[] {
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
    };
    
    private static final int[] mFreqModelToEventRecurrence = {
        EventRecurrence.DAILY,
        EventRecurrence.WEEKLY,
        EventRecurrence.MONTHLY,
        EventRecurrence.YEARLY
    };
    
    private String mEndNeverStr;
    private String mEndDateLabel;
    private String mEndCountLabel;
    
    /** A double array of Strings to hold the 7x5 list of possible strings of the form:
     *  "on every [Nth] [DAY_OF_WEEK]", e.g. "on every second Monday",
     *  where [Nth] can be [first, second, third, fourth, last] */
    private String[][] mMonthRepeatByDayOfWeekStrs;
    
    public RecurrenceContext() {
    	
    }
    
    public RecurrenceContext (Activity activity, long startTime, String tzId, String Rrule) {
    	mResources = activity.getResources();    	
    	// wkst는 EditEventHelper.offsetStartTimeIfNecessary에서 사용된다
    	/*
    	 * "The WKST rule part specifies the day on which the workweek starts.
   			Valid values are MO, TU, WE, TH, FR, SA and SU. This is significant
   			when a WEEKLY RRULE has an interval greater than 1, and a BYDAY rule
   			part is specified. This is also significant when in a YEARLY RRULE
   			when a BYWEEKNO rule part is specified. The default value is MO."
    	 */
    	// GeneralPreferences.KEY_WEEK_START_DAY = "preferences_week_start_day"
    	// preferences_week_start_day is -1
    	// : 이것은 현재 Calendar의 Locale의 firstDayOfWeek를 따르라는 의미이다
    	mRecurrence.wkst = EventRecurrence.calendarDay2Day(Utils.getFirstDayOfWeek(activity));//mRecurrence.wkst = EventRecurrence.timeDay2Day(Utils.getFirstDayOfWeek(activity));
    	
    	mTime.setTimeInMillis(startTime);
    	
    	if (!TextUtils.isEmpty(tzId)) {
            mTime = ETime.switchTimezone(mTime, TimeZone.getTimeZone(tzId));//mTime.timezone = timeZone;
        }
        //mTime.normalize(false);
        
    	int dayOfWeek = mTime.get(Calendar.DAY_OF_WEEK);
        // Time days of week: Sun=0, Mon=1, etc : 우리는 더 이상 Time class를 사용하지 않는다 Calendar를 사용한다
        mModel.weeklyByDayOfWeek[dayOfWeek - 1] = true;
        
        if (!TextUtils.isEmpty(Rrule)) {
            mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
            mRecurrence.parse(Rrule);
            copyEventRecurrenceToModel(mRecurrence, mModel);
            // Leave today's day of week as checked by default in weekly view.
            if (mRecurrence.bydayCount == 0) {
            	dayOfWeek = mTime.get(Calendar.DAY_OF_WEEK);
            	mModel.weeklyByDayOfWeek[dayOfWeek - 1] = true;//mModel.weeklyByDayOfWeek[mTime.weekDay] = true;
            }
        }
        
        // EditEventView와 Sync를 다시 한번 맞춰야 한다
        // 아래의 코드가 의미가 있을까???
        // :EditEventView에서 endDate를 설정하는 함수를 호출하여 설정하기 때문이고
        //  최초 디폴트 값을 설정하기 때문이다
        // 물론 timezone 설정은 필요하다
        if (mModel.endDate == null) {
            mModel.endDate = GregorianCalendar.getInstance(mTime.getTimeZone());//new Time(mTime);
            /*
            switch (mModel.freq) {
                case RecurrenceModel.FREQ_DAILY:
                	mModel.endDate.monthDay += 7;
                	break;
                case RecurrenceModel.FREQ_WEEKLY:
                    mModel.endDate.month += 1;
                    break;
                case RecurrenceModel.FREQ_MONTHLY:
                    mModel.endDate.year += 1;
                    break;
                case RecurrenceModel.FREQ_YEARLY:
                    mModel.endDate.year += 5;
                    break;
            }
            
            mModel.endDate.normalize(false);
            */
        }
        
        mMonthRepeatByDayOfWeekStrs = new String[7][];
        // from Time.SUNDAY as 0 through Time.SATURDAY as 6
        mMonthRepeatByDayOfWeekStrs[0] = mResources.getStringArray(R.array.repeat_by_nth_sun);
        mMonthRepeatByDayOfWeekStrs[1] = mResources.getStringArray(R.array.repeat_by_nth_mon);
        mMonthRepeatByDayOfWeekStrs[2] = mResources.getStringArray(R.array.repeat_by_nth_tues);
        mMonthRepeatByDayOfWeekStrs[3] = mResources.getStringArray(R.array.repeat_by_nth_wed);
        mMonthRepeatByDayOfWeekStrs[4] = mResources.getStringArray(R.array.repeat_by_nth_thurs);
        mMonthRepeatByDayOfWeekStrs[5] = mResources.getStringArray(R.array.repeat_by_nth_fri);
        mMonthRepeatByDayOfWeekStrs[6] = mResources.getStringArray(R.array.repeat_by_nth_sat);
    }
    
    static public boolean isSupportedMonthlyByNthDayOfWeek(int num) {
        // We only support monthlyByNthDayOfWeek when it is greater then 0 but less then 5.
        // Or if -1 when it is the last monthly day of the week.
        return (num > 0 && num <= FIFTH_WEEK_IN_A_MONTH) || num == LAST_NTH_DAY_OF_WEEK;
    }
    
    static public boolean canHandleRecurrenceRule(EventRecurrence er) {
        switch (er.freq) {
            case EventRecurrence.DAILY:
            case EventRecurrence.MONTHLY:
            case EventRecurrence.YEARLY:
            case EventRecurrence.WEEKLY:
                break;
            default:
                return false;
        }

        if (er.count > 0 && !TextUtils.isEmpty(er.until)) {
            return false;
        }

        // Weekly: For "repeat by day of week", the day of week to repeat is in
        // er.byday[]

        /*
         * Monthly: For "repeat by nth day of week" the day of week to repeat is
         * in er.byday[] and the "nth" is stored in er.bydayNum[]. Currently we
         * can handle only one and only in monthly
         */
        int numOfByDayNum = 0;
        for (int i = 0; i < er.bydayCount; i++) {
            if (isSupportedMonthlyByNthDayOfWeek(er.bydayNum[i])) {
                ++numOfByDayNum;
            }
        }

        if (numOfByDayNum > 1) {
            return false;
        }

        if (numOfByDayNum > 0 && er.freq != EventRecurrence.MONTHLY) {
            return false;
        }

        // The UI only handle repeat by one day of month i.e. not 9th and 10th
        // of every month
        if (er.bymonthdayCount > 1) {
            return false;
        }

        if (er.freq == EventRecurrence.MONTHLY) {
            if (er.bydayCount > 1) {
                return false;
            }
            if (er.bydayCount > 0 && er.bymonthdayCount > 0) {
                return false;
            }
        }

        return true;
    }
    
    static private void copyEventRecurrenceToModel(final EventRecurrence er,
            RecurrenceModel model) {
        // Freq:
        switch (er.freq) {
            case EventRecurrence.DAILY:
                model.freq = RecurrenceModel.FREQ_DAILY;
                break;
            case EventRecurrence.MONTHLY:
                model.freq = RecurrenceModel.FREQ_MONTHLY;
                break;
            case EventRecurrence.YEARLY:
                model.freq = RecurrenceModel.FREQ_YEARLY;
                break;
            case EventRecurrence.WEEKLY:
                model.freq = RecurrenceModel.FREQ_WEEKLY;
                break;
            default:
                throw new IllegalStateException("freq=" + er.freq);
        }

        // Interval:
        if (er.interval > 0) {
            model.interval = er.interval;
        }

        // End:
        // End by count:
        model.endCount = er.count;
        if (model.endCount > 0) {
            model.end = RecurrenceModel.END_BY_COUNT;
        }

        // End by date:
        if (!TextUtils.isEmpty(er.until)) {
            if (model.endDate == null) {
                model.endDate = GregorianCalendar.getInstance();//new Time();
            }

            try {
                model.endDate.setTimeInMillis(ETime.parse(model.endDate.getTimeZone(), er.until));//model.endDate.parse(er.until);
            } catch (TimeFormatException e) {
                model.endDate = null;
            }

            // LIMITATION: The UI can only handle END_BY_DATE or END_BY_COUNT
            if (model.end == RecurrenceModel.END_BY_COUNT && model.endDate != null) {
                throw new IllegalStateException("freq=" + er.freq);
            }

            model.end = RecurrenceModel.END_BY_DATE;
        }

        // Weekly: repeat by day of week or Monthly: repeat by nth day of week
        // in the month
        Arrays.fill(model.weeklyByDayOfWeek, false);
        if (er.bydayCount > 0) {
            int count = 0;
            for (int i = 0; i < er.bydayCount; i++) {
                int dayOfWeek = EventRecurrence.day2CalendarDay(er.byday[i]);
                model.weeklyByDayOfWeek[dayOfWeek - 1] = true;

                if (model.freq == RecurrenceModel.FREQ_MONTHLY &&
                        isSupportedMonthlyByNthDayOfWeek(er.bydayNum[i])) {
                    // LIMITATION: Can handle only (one) weekDayNum in nth or last and only
                    // when
                    // monthly
                    model.monthlyByDayOfWeek = dayOfWeek;
                    model.monthlyByNthDayOfWeek = er.bydayNum[i];
                    model.monthlyRepeat = RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK;
                    count++;
                }
            }

            if (model.freq == RecurrenceModel.FREQ_MONTHLY) {
                if (er.bydayCount != 1) {
                    // Can't handle 1st Monday and 2nd Wed
                    throw new IllegalStateException("Can handle only 1 byDayOfWeek in monthly");
                }
                if (count != 1) {
                    throw new IllegalStateException(
                            "Didn't specify which nth day of week to repeat for a monthly");
                }
            }
        }

        // Monthly by day of month
        if (model.freq == RecurrenceModel.FREQ_MONTHLY) {
            if (er.bymonthdayCount == 1) {
                if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    throw new IllegalStateException(
                            "Can handle only by monthday or by nth day of week, not both");
                }
                model.monthlyByMonthDay = er.bymonthday[0];
                model.monthlyRepeat = RecurrenceModel.MONTHLY_BY_DATE;
            } else if (er.bymonthCount > 1) {
                // LIMITATION: Can handle only one month day
                throw new IllegalStateException("Can handle only one bymonthday");
            }
        }
    }
    
    
    static private void copyModelToEventRecurrence(final RecurrenceModel model,
            EventRecurrence er) {
        if (model.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            throw new IllegalStateException("There's no recurrence");
        }

        // Freq
        er.freq = mFreqModelToEventRecurrence[model.freq];

        // Interval
        if (model.interval <= 1) { // 왜 interval이 1인 경우 0으로 설정하는 것인가???
            er.interval = 0;
        } else {
            er.interval = model.interval;
        }

        // End
        switch (model.end) {
            case RecurrenceModel.END_BY_DATE:
                if (model.endDate != null) {
                	model.endDate = ETime.switchTimezone(model.endDate, TimeZone.getTimeZone(ETime.TIMEZONE_UTC));//model.endDate.switchTimezone(Time.TIMEZONE_UTC);
                    //model.endDate.normalize(false);
                    er.until = ETime.format2445(model.endDate);//model.endDate.format2445();
                    er.count = 0;
                } else {
                    throw new IllegalStateException("end = END_BY_DATE but endDate is null");
                }
                break;
            case RecurrenceModel.END_BY_COUNT:
                er.count = model.endCount;
                er.until = null;
                if (er.count <= 0) {
                    throw new IllegalStateException("count is " + er.count);
                }
                break;
            default:
                er.count = 0;
                er.until = null;
                break;
        }

        // Weekly && monthly repeat patterns
        er.bydayCount = 0;
        er.bymonthdayCount = 0;

        switch (model.freq) {
            case RecurrenceModel.FREQ_MONTHLY:
                if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE) {
                    if (model.monthlyByMonthDay > 0) {
                        if (er.bymonthday == null || er.bymonthdayCount < 1) {
                            er.bymonthday = new int[1];
                        }
                        er.bymonthday[0] = model.monthlyByMonthDay;
                        er.bymonthdayCount = 1;
                    }
                } else if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    if (!isSupportedMonthlyByNthDayOfWeek(model.monthlyByNthDayOfWeek)) {
                        throw new IllegalStateException("month repeat by nth week but n is "
                                + model.monthlyByNthDayOfWeek);
                    }
                    int count = 1;
                    if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                        er.byday = new int[count];
                        er.bydayNum = new int[count];
                    }
                    er.bydayCount = count;
                    er.byday[0] = EventRecurrence.calendarDay2Day(model.monthlyByDayOfWeek);
                    er.bydayNum[0] = model.monthlyByNthDayOfWeek;
                }
                break;
            case RecurrenceModel.FREQ_WEEKLY:
            	// by dayofweek가 한 개 이상 존재할 수 있다
                int count = 0;
                for (int i = 0; i < 7; i++) {
                    if (model.weeklyByDayOfWeek[i]) {
                        count++;
                    }
                }

                if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                    er.byday = new int[count];
                    er.bydayNum = new int[count];
                }
                er.bydayCount = count;

                for (int i = 6; i >= 0; i--) {
                    if (model.weeklyByDayOfWeek[i]) {
                        er.bydayNum[--count] = 0;
                        er.byday[count] = EventRecurrence.calendarDay2Day(i);
                    }
                }
                break;
        }

        if (!canHandleRecurrenceRule(er)) {
            throw new IllegalStateException("UI generated recurrence that it can't handle. ER:"
                    + er.toString() + " Model: " + model.toString());
        }
    }
        
    
    public void setNoRepeat() {
    	mModel.recurrenceState = RecurrenceModel.STATE_NO_RECURRENCE;    	
    }
    
    public void setNoRepeatEnd() {
    	mModel.recurrenceState = RecurrenceModel.STATE_NO_RECURRENCE;
    }
    
    public void setFreqAndInterval(int freqId, int interval) {
    	if (mModel.recurrenceState != RecurrenceModel.STATE_RECURRENCE) {
    		mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
    	}
    	
    	mModel.freq = freqId;
    	mModel.interval = interval;
    }
    
    public int getFreq() {
    	int freq = -1;
    	
    	if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE)
    		return freq;
    	
    	switch(mModel.freq) {
    	case RecurrenceModel.FREQ_DAILY:
    		freq = RecurrenceModel.FREQ_DAILY;
    		break;
    	case RecurrenceModel.FREQ_WEEKLY:
    		freq = RecurrenceModel.FREQ_WEEKLY;
    		break;
    	case RecurrenceModel.FREQ_MONTHLY:
    		freq = RecurrenceModel.FREQ_MONTHLY;
    		break;
    	case RecurrenceModel.FREQ_YEARLY:
    		freq = RecurrenceModel.FREQ_YEARLY;
    		break;
    	default:
    		return freq;
    	}
    	
    	return freq;
    }
    
    public int getFreqInterval() {
    	return mModel.interval;
    }
    
    public void setEnd(int end) {
    	switch(end) {
    	case RecurrenceModel.END_NEVER:
            mModel.end = RecurrenceModel.END_NEVER;
            break;
        case RecurrenceModel.END_BY_DATE:
            mModel.end = RecurrenceModel.END_BY_DATE;
            break;
        case RecurrenceModel.END_BY_COUNT:
            mModel.end = RecurrenceModel.END_BY_COUNT;

            if (mModel.endCount <= 1) {
                mModel.endCount = 1;
            } else if (mModel.endCount > COUNT_MAX) {
                mModel.endCount = COUNT_MAX;
            }
            //updateEndCountText();
            break;
    	}
    }
    
    public int getEndUntillValue() {
    	int endUntill = 0;
    	
    	switch(mModel.end) {
    	case RecurrenceModel.END_NEVER:
    		endUntill = RecurrenceModel.END_NEVER;
            break;
        case RecurrenceModel.END_BY_DATE:
        	endUntill = RecurrenceModel.END_BY_DATE;
            break;
        case RecurrenceModel.END_BY_COUNT:
        	endUntill = RecurrenceModel.END_BY_COUNT;
            break;
        default:
        	endUntill = -1;
        	break;
    	}
    	
    	return endUntill;
    }
    
    public void setEndDate(int year, int monthOfYear, int dayOfMonth) {
    	if (mModel.endDate == null) {
            mModel.endDate = GregorianCalendar.getInstance(mTime.getTimeZone());//new Time(mTime.timezone);
            //mModel.endDate.hour = mModel.endDate.minute = mModel.endDate.second = 0;
            mModel.endDate.set(Calendar.HOUR_OF_DAY, 0);
            mModel.endDate.set(Calendar.MINUTE, 0);
            mModel.endDate.set(Calendar.SECOND, 0);
        }
    	mModel.endDate.set(Calendar.YEAR, year);//mModel.endDate.year = year;
    	mModel.endDate.set(Calendar.MONTH, monthOfYear);//mModel.endDate.month = monthOfYear;
    	mModel.endDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);//mModel.endDate.monthDay = dayOfMonth;
        //mModel.endDate.normalize(false);
    }
    
    public String makeRRule() {
    	String rrule;
        if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            rrule = null;
        } else {
            copyModelToEventRecurrence(mModel, mRecurrence);
            rrule = mRecurrence.toString();
        }
        
        return rrule;
    }
    
    private String mMonthRepeatByDayOfWeekStr;
    public void test() {
    	// 아래 알고리즘은 기가 막힌다
    	// 예를 들어 
    	// 2014/0706 -> 매월 첫 번째 일요일 
    	// 2014/0707 -> 매월 첫 번째 월요일 : 달력상으로 보기엔 7월 두 번째 주 월요일이지만, 사실상 7월달의 첫 월요일이다
    	// 2014/0708 -> 매월 두 번째 화요일
    	// : 정확하다!!!
    	// 그러므로 예를 들어
    	// 매월 두 번째 화요일이라는 것은 
    	// 해당 월의 둘째 주의 화요일이라는 것이 아니라,
    	// 해당 월의 두 번째 화요일이라는 것이다!!!
    	//mModel.monthlyByNthDayOfWeek = (mTime.monthDay + 6) / 7;
    	
    	switch (mModel.freq) {
    	case RecurrenceModel.FREQ_MONTHLY:        

            if (mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE) {
                //mMonthRepeatByRadioGroup.check(R.id.repeatMonthlyByNthDayOfMonth);
            } else if (mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                //mMonthRepeatByRadioGroup.check(R.id.repeatMonthlyByNthDayOfTheWeek);
            }

            if (mMonthRepeatByDayOfWeekStr == null) {
                if (mModel.monthlyByNthDayOfWeek == 0) {
                    mModel.monthlyByNthDayOfWeek = (mTime.get(Calendar.DAY_OF_MONTH) + 6) / 7;
                    // Since not all months have 5 weeks,
                    // we convert 5th NthDayOfWeek to -1 for last monthly day of the week
                    if (mModel.monthlyByNthDayOfWeek >= FIFTH_WEEK_IN_A_MONTH) {
                        mModel.monthlyByNthDayOfWeek = LAST_NTH_DAY_OF_WEEK;
                    }
                    
                    mModel.monthlyByDayOfWeek = mTime.get(Calendar.DAY_OF_WEEK);
                }

                String[] monthlyByNthDayOfWeekStrs = mMonthRepeatByDayOfWeekStrs[mModel.monthlyByDayOfWeek];

                // TODO(psliwowski): Find a better way handle -1 indexes
                int msgIndex = mModel.monthlyByNthDayOfWeek < 0 ? 
                		FIFTH_WEEK_IN_A_MONTH : mModel.monthlyByNthDayOfWeek;
                
                mMonthRepeatByDayOfWeekStr = monthlyByNthDayOfWeekStrs[msgIndex - 1];
                //mRepeatMonthlyByNthDayOfWeek.setText(mMonthRepeatByDayOfWeekStr);
            }
            break;
    	}
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
