package com.intheeast.etc;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;




import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.text.format.Time;

public class ETime {
	
	/**
     * The Julian day of the ecalendar epoch, that is, January 1, 1900[UTC Based] on the Gregorian
     * calendar.
     */
	
	public static final String TIMEZONE_UTC = "UTC";
	public static final TimeZone UTCTimeZone = TimeZone.getTimeZone(TIMEZONE_UTC);
	// 1900/01/01/12:00:00 은 2415021이다
	// UNIX_TIME : 1970년 1월 1일 00:00:00 협정 세계시(UTC) 부터의 경과 시간을 초로 환산하여 정수로 나타낸 것
	//public static final int ECALENDAR_EPOCH_JULIAN_DAY_FOR_BEFORE_UNIX_TIME = 2415022; // 1900/01/01/12:00:00의 julian day를 ETime.getJulianDay로 구한 값 
    //public static final int ECALENDAR_EPOCH_JULIAN_DAY_FOR_SINCE_UNIX_TIME = 2415021; // 정확한 julian day 값
    public static final int ECALENDAR_EPOCH_JULIAN_DAY = 2415021;
    
    //basicDateTimeNoMillis : The time zone offset is 'Z' for zero, and of the form '±HHmm' for non-zero
    public static final String ISO_8601_BASIC_DATETIME_NOMILLIS_UTC_TZ_FORMAT = "yyyyMMdd'T'HHmmss'Z'"; //ex) 20081013T160000TZ
    public static final String ISO_8601_BASIC_DATETIME_NOMILLIS_FORMAT = "yyyyMMdd'T'HHmmss"; //ex) 20081013T160000T
    
    // 1970/01/01/12:00:00 은 2440588이다
    public static final int EPOCH_JULIAN_DAY = Time.EPOCH_JULIAN_DAY;
    /**
     * The Julian day of the Monday in the week of the ecalendar epoch, January 1, 1900
     * on the Gregorian calendar.
     */
    //public static final int MONDAY_BEFORE_JULIAN_ECALENDAR_EPOCH = ECALENDAR_EPOCH_JULIAN_DAY - 7; // 1900/01/01는 월요일이다 1899/12/25
    public static final int MONDAY_BEFORE_JULIAN_ECALENDAR_EPOCH = ECALENDAR_EPOCH_JULIAN_DAY; //2415022
    
    private static final DateFormat isoDateTimeUTCTZFormat =
    	      new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    
    private static final DateFormat isoDateTimeUTCFormat =
    	      new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    
    private static final String format24Hour = "%H:%M";
    private static final String format12Hour = "%I:%M%P";
    
    public static final int NUM_DAYS = 7;
        
    public static int getJulianDay(long millis, TimeZone timezone, int firstDayOfWeek) {
    	int targetJulianDay = 0;
    	
    	int mondayOrderInWeek = Calendar.MONDAY - firstDayOfWeek;
        if (mondayOrderInWeek < 0) {
        	mondayOrderInWeek = mondayOrderInWeek + NUM_DAYS;
        }
        
    	Calendar cal = GregorianCalendar.getInstance(timezone);
        cal.setFirstDayOfWeek(firstDayOfWeek);
        cal.setTimeInMillis(millis);
        int weekDay = cal.get(Calendar.DAY_OF_WEEK);
        int weekDayOrederInWeek = weekDay - firstDayOfWeek; // oredreing start at Zero
        if (weekDayOrederInWeek < 0) {
        	weekDayOrederInWeek = weekDayOrederInWeek + NUM_DAYS;
        }
                
        int weekPosition = getWeeksSinceEcalendarEpochFromMillis(millis, timezone, firstDayOfWeek);    	        
        int julianMonday = getJulianMondayFromWeeksSinceEcalendarEpoch(weekPosition);
        
        int diff = weekDayOrederInWeek - mondayOrderInWeek;
		targetJulianDay = julianMonday + diff;		
                
        return targetJulianDay;        
    } 
    
    
    public static long getMillisFromJulianDay(int julianDay, TimeZone tz, int firstDayOfWeek) {
    	Calendar cal = GregorianCalendar.getInstance(tz);
    	//long gmtoff = tz.getRawOffset() / 1000;
    	// Don't bother with the GMT offset since we don't know the correct
        // value for the given Julian day.  Just get close and then adjust
        // the day.
        long millis = (julianDay - EPOCH_JULIAN_DAY) * DateUtils.DAY_IN_MILLIS;
        cal.setTimeInMillis(millis);

        // Figure out how close we are to the requested Julian day.
        // We can't be off by more than a day.
        int approximateDay = getJulianDay(millis, tz, firstDayOfWeek);
        int diff = julianDay - approximateDay;
        cal.add(Calendar.DAY_OF_MONTH, diff);//monthDay += diff;

        // Set the time to 12am and re-normalize.
        cal.set(Calendar.HOUR_OF_DAY, 0);//hour = 0;
        cal.set(Calendar.MINUTE, 0);//minute = 0;
        cal.set(Calendar.SECOND, 0);//second = 0;
        millis = cal.getTimeInMillis();
        return millis;
    }
	
    // 
	// if) firstDayOfWeek is Calendar.SUNDAY[1],    diff = 4            /firstDayOfWeek is Time.SUNDAY[0],    	diff = 4
    // if) firstDayOfWeek is Calendar.MONDAY[2],    diff = 3			/firstDayOfWeek is Time.MONDAY[1],    	diff = 3
    // if) firstDayOfWeek is Calendar.TUESDAY[3],   diff = 2			/firstDayOfWeek is Time.TUESDAY[2],   	diff = 2
    // if) firstDayOfWeek is Calendar.WEDNESDAY[4], diff = 1			/firstDayOfWeek is Time.WEDNESDAY[3], 	diff = 1
    // if) firstDayOfWeek is Calendar.THURSDAY[5],  diff = 0			/firstDayOfWeek is Time.THURSDAY[4],  	diff = 0
    // if) firstDayOfWeek is Calendar.FRIDAY[6],    diff = -1  -->6	    /firstDayOfWeek is Time.FRIDAY[5],    	diff = -1  -->6
    // if) firstDayOfWeek is Calendar.SATURDAY[7],  diff = -2  -->5	    /firstDayOfWeek is Time.SATURDAY[6],  	diff = -2  -->5
    /*
	public static int getWeeksSinceEcalendarEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
		int diff = Calendar.MONDAY - firstDayOfWeek;// 1900/01/01 is MONDAY
        if (diff < 0) {
            diff += 7;
        }
                
        int refDay = ECALENDAR_EPOCH_JULIAN_DAY - diff; 
        return (julianDay - refDay) / 7;
    }
	*/
	static final long dividerMillis = 7 * DateUtils.DAY_IN_MILLIS;
	static final long ECALENDAR_EPOCH_MILLIS = -2208988800000L; // -2208988800000 is 1900/01/01/00:00:00 UTC millis
	public static int getWeeksSinceEcalendarEpochFromMillis(long millis, TimeZone timezone, int firstDayOfWeek) {
		long offsetMillis = timezone.getRawOffset();
		millis = millis + offsetMillis;
		
		int diff = Calendar.MONDAY - firstDayOfWeek;// 1900/01/01 is MONDAY
        if (diff < 0) {
            diff += 7;
        }
                
        long diffMillis = diff * DateUtils.DAY_IN_MILLIS;
        long refDayMillis = ECALENDAR_EPOCH_MILLIS - diffMillis;
        
        return (int) ((millis - refDayMillis) / dividerMillis);
    }
	
	public static int getJulianMondayFromWeeksSinceEcalendarEpoch(int week) {		
		return MONDAY_BEFORE_JULIAN_ECALENDAR_EPOCH + week * 7;
    }
	
	public static void copyCalendar(Calendar source, Calendar target) {
		target.setFirstDayOfWeek(source.getFirstDayOfWeek());
		target.setTimeInMillis(source.getTimeInMillis());
	}
	
	public static void adjustStartDayInWeek(Calendar startDayTimeCal) {
		int firstDayOfWeek = startDayTimeCal.getFirstDayOfWeek();
		int weekDay = startDayTimeCal.get(Calendar.DAY_OF_WEEK); 
    	
		// 만약에 firstDayOfWeek가 SAT이고
		// weekDay가 MON이면???...
    	if (weekDay != firstDayOfWeek) { 
    		int diff = weekDay - firstDayOfWeek;
			if (diff < 0) {
				diff += NUM_DAYS;
			}
			
			startDayTimeCal.add(Calendar.DAY_OF_MONTH, -diff);				
    	}
	}
	
	public static int getWeeksSinceEcalendarEpochFromJulianDay(int julianDay, TimeZone timezone, int firstDayOfWeek) {
		
		int weekPosition = getWeeksSinceEcalendarEpochFromMillis(getMillisFromJulianDay(julianDay, timezone, firstDayOfWeek),
				timezone,
				firstDayOfWeek);
		
		return weekPosition;
	}
	
	

	public static int compare(Calendar a, Calendar b) {
		
		if (a == null) {
            throw new NullPointerException("a == null");
        } else if (b == null) {
            throw new NullPointerException("b == null");
        }
		
		//a negative result if a is earlier, a positive result if a is earlier, or 0 if they are equal.
		
		long aMillis = a.getTimeInMillis();
		long bMillis = b.getTimeInMillis();
		
		if (aMillis < bMillis) { 
			return -1;
		}
		else if (aMillis > bMillis) {
			return 1;
		}
		else {
			return 0;
		}		
	}
	
	public static String getCurrentTimezone() {
		return Time.getCurrentTimezone();
	}
	
	public void xxxx(Time time, String s) {
		time.parse(s);
	}
	
	public static String format2445(Calendar time) {	
	    	    
	    Date date = time.getTime();   	    
	    SimpleDateFormat fmt = new SimpleDateFormat(ISO_8601_BASIC_DATETIME_NOMILLIS_FORMAT);
		fmt.setTimeZone(time.getTimeZone()); 		
		return fmt.format(date); 
	}
	
	public static int getWeekNumbersOfMonth(Calendar month) {
		Calendar cal = GregorianCalendar.getInstance(month.getTimeZone());
		cal.setFirstDayOfWeek(month.getFirstDayOfWeek());
		cal.setTimeInMillis(month.getTimeInMillis());
		int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.set(Calendar.DAY_OF_MONTH, maxDays);
		int weekNumbers = cal.get(Calendar.WEEK_OF_MONTH);
		return weekNumbers;		
	}
	
	public static int getRemainingWeeks(Calendar targetMonthCalendar, boolean byFirstDayOfWeek) {
		int maximumMonthDays = targetMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);        	
		
		int remainingMonthDays = 0;
		if (byFirstDayOfWeek) {
			int firstDayOfMonth = targetMonthCalendar.get(Calendar.DAY_OF_MONTH);
			remainingMonthDays = (maximumMonthDays - firstDayOfMonth) + 1;
		}
		else { // last day of week 다음주부터 날짜를...
			remainingMonthDays = maximumMonthDays - targetMonthCalendar.get(Calendar.DAY_OF_MONTH);
		}
		
		int remainingWeeks = remainingMonthDays / 7;        	
		int lastWeekDays = remainingMonthDays % 7;       	
		if (lastWeekDays != 0) { 
			++remainingWeeks;            	
		}
		
		return remainingWeeks;
	}
	
	// The country codes listed below have been established by the International Standards Organization (ISO).
	// http://www.iana.org/time-zones
	// http://www.timeanddate.com/worldclock/ 를 참조하자
	// http://www.timezoneconverter.com/cgi-bin/tzc 를 가지고 switch timezone 결과를 비교하자
	// http://www.timeanddate.com/android/how-to-use-worldclock.html : How to use the World Clock Android app
	public static Calendar switchTimezone(Calendar time, TimeZone replaceTimeZone) {
		Calendar switchedTime = GregorianCalendar.getInstance(replaceTimeZone);		
		long millis = time.getTimeInMillis();
		switchedTime.setTimeInMillis(millis);
		return switchedTime;
	}
	
	
	public static int convertDayOfWeekFromCalendarToTime(int timeDayOfWeek) {
        switch (timeDayOfWeek) {
    	case Calendar.SUNDAY:
    		return Time.SUNDAY;
        case Calendar.MONDAY:
            return Time.MONDAY;
        case Calendar.TUESDAY:
            return Time.TUESDAY;
        case Calendar.WEDNESDAY:
            return Time.WEDNESDAY;
        case Calendar.THURSDAY:
            return Time.THURSDAY;
        case Calendar.FRIDAY:
            return Time.FRIDAY;
        case Calendar.SATURDAY:
            return Time.SATURDAY;            
        default:
            throw new IllegalArgumentException("Argument must be between Time.SUNDAY and " +
                    "Time.SATURDAY");
        }
    }
	
	
	public static long parse(TimeZone timezone, String string) {
		try {
			SimpleDateFormat fmt = new SimpleDateFormat(ISO_8601_BASIC_DATETIME_NOMILLIS_UTC_TZ_FORMAT);
			fmt.setTimeZone(timezone);
			Date date = fmt.parse(string); // Date의 timezone은 user's prefered timezone으로 설정된다
			
			Calendar cal = GregorianCalendar.getInstance(timezone);
			
			cal.setTime(date);
			
			return cal.getTimeInMillis();
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return 0;
		}      
	}
	
	
	public static String formatTime(int hourOfDay, int minute, boolean is24HourMode) {
        Time time = new Time();
        time.hour = hourOfDay;
        time.minute = minute;

        String format = is24HourMode? format24Hour : format12Hour;
        return time.format(format);
    }
	
	
	
	// The following FORMAT_* symbols are used for specifying the format of
    // dates and times in the formatDateRange method.
    public static final int FORMAT_SHOW_TIME = 0x00001;
    public static final int FORMAT_SHOW_WEEKDAY = 0x00002;
    public static final int FORMAT_SHOW_YEAR = 0x00004;
    public static final int FORMAT_NO_YEAR = 0x00008;
    public static final int FORMAT_SHOW_DATE = 0x00010;
    public static final int FORMAT_NO_MONTH_DAY = 0x00020;
    @Deprecated
    public static final int FORMAT_12HOUR = 0x00040;
    @Deprecated
    public static final int FORMAT_24HOUR = 0x00080;
    @Deprecated
    public static final int FORMAT_CAP_AMPM = 0x00100;
    public static final int FORMAT_NO_NOON = 0x00200;
    @Deprecated
    public static final int FORMAT_CAP_NOON = 0x00400;
    public static final int FORMAT_NO_MIDNIGHT = 0x00800;
    @Deprecated
    public static final int FORMAT_CAP_MIDNIGHT = 0x01000;
    
    //public static final int FORMAT_NO_MONTH = 0x02000;

    @Deprecated
    public static final int FORMAT_UTC = 0x02000;
    public static final int FORMAT_ABBREV_TIME = 0x04000;
    public static final int FORMAT_ABBREV_WEEKDAY = 0x08000;
    public static final int FORMAT_ABBREV_MONTH = 0x10000;
    public static final int FORMAT_NUMERIC_DATE = 0x20000;
    public static final int FORMAT_ABBREV_RELATIVE = 0x40000;
    public static final int FORMAT_ABBREV_ALL = 0x80000;
    @Deprecated
    public static final int FORMAT_CAP_NOON_MIDNIGHT = (FORMAT_CAP_NOON | FORMAT_CAP_MIDNIGHT);
    @Deprecated
    public static final int FORMAT_NO_NOON_MIDNIGHT = (FORMAT_NO_NOON | FORMAT_NO_MIDNIGHT);

    // Date and time format strings that are constant and don't need to be
    // translated.
    /**
     * This is not actually the preferred 24-hour date format in all locales.
     * @deprecated Use {@link java.text.SimpleDateFormat} instead.
     */
    @Deprecated
    public static final String HOUR_MINUTE_24 = "%H:%M";
    public static final String MONTH_FORMAT = "%B";
    /**
     * This is not actually a useful month name in all locales.
     * @deprecated Use {@link java.text.SimpleDateFormat} instead.
     */
    @Deprecated
    public static final String ABBREV_MONTH_FORMAT = "%b";
    public static final String NUMERIC_MONTH_FORMAT = "%m";
    public static final String MONTH_DAY_FORMAT = "%-d";
    public static final String YEAR_FORMAT = "%Y";
    public static final String YEAR_FORMAT_TWO_DIGITS = "%g";
    public static final String WEEKDAY_FORMAT = "%A";
    public static final String ABBREV_WEEKDAY_FORMAT = "%a";

    //int id = Resources.getSystem().getIdentifier("numeric_date", "string", "android");
	//String defaultDateFormat = Resources.getSystem().getString(id);
	
    // This table is used to lookup the resource string id of a format string
    // used for formatting a start and end date that fall in the same year.
    // The index is constructed from a bit-wise OR of the boolean values:
    // {showTime, showYear, showWeekDay}.  For example, if showYear and
    // showWeekDay are both true, then the index would be 3.
    /** @deprecated Do not use. */    
    public static final int sameYearTable[] = {
    	Resources.getSystem().getIdentifier("same_year_md1_md2", "string", "android"),//com.android.internal.R.string.same_year_md1_md2,
    	Resources.getSystem().getIdentifier("same_year_wday1_md1_wday2_md2", "string", "android"),//com.android.internal.R.string.same_year_wday1_md1_wday2_md2,
    	Resources.getSystem().getIdentifier("same_year_mdy1_mdy2", "string", "android"),//com.android.internal.R.string.same_year_mdy1_mdy2,
    	Resources.getSystem().getIdentifier("same_year_wday1_mdy1_wday2_mdy2", "string", "android"),//com.android.internal.R.string.same_year_wday1_mdy1_wday2_mdy2,
    	Resources.getSystem().getIdentifier("same_year_md1_time1_md2_time2", "string", "android"),//com.android.internal.R.string.same_year_md1_time1_md2_time2,
    	Resources.getSystem().getIdentifier("same_year_wday1_md1_time1_wday2_md2_time2", "string", "android"),//com.android.internal.R.string.same_year_wday1_md1_time1_wday2_md2_time2,
    	Resources.getSystem().getIdentifier("same_year_mdy1_time1_mdy2_time2", "string", "android"),//com.android.internal.R.string.same_year_mdy1_time1_mdy2_time2,
    	Resources.getSystem().getIdentifier("same_year_wday1_mdy1_time1_wday2_mdy2_time2", "string", "android"),//com.android.internal.R.string.same_year_wday1_mdy1_time1_wday2_mdy2_time2,

        // Numeric date strings
    	Resources.getSystem().getIdentifier("numeric_md1_md2", "string", "android"),//com.android.internal.R.string.numeric_md1_md2,
    	Resources.getSystem().getIdentifier("numeric_wday1_md1_wday2_md2", "string", "android"),//com.android.internal.R.string.numeric_wday1_md1_wday2_md2,
    	Resources.getSystem().getIdentifier("numeric_mdy1_mdy2", "string", "android"),//com.android.internal.R.string.numeric_mdy1_mdy2,
    	Resources.getSystem().getIdentifier("numeric_wday1_mdy1_wday2_mdy2", "string", "android"),//com.android.internal.R.string.numeric_wday1_mdy1_wday2_mdy2,
    	Resources.getSystem().getIdentifier("numeric_md1_time1_md2_time2", "string", "android"),//com.android.internal.R.string.numeric_md1_time1_md2_time2,
    	Resources.getSystem().getIdentifier("numeric_wday1_md1_time1_wday2_md2_time2", "string", "android"),//com.android.internal.R.string.numeric_wday1_md1_time1_wday2_md2_time2,
    	Resources.getSystem().getIdentifier("numeric_mdy1_time1_mdy2_time2", "string", "android"),//com.android.internal.R.string.numeric_mdy1_time1_mdy2_time2,
    	Resources.getSystem().getIdentifier("numeric_wday1_mdy1_time1_wday2_mdy2_time2", "string", "android"),//com.android.internal.R.string.numeric_wday1_mdy1_time1_wday2_mdy2_time2,
    };

    //Resources.getSystem().getIdentifier("", "string", "android"),//
    // Resources.getSystem().getString(Resources.getSystem().getIdentifier("", "string", "android"));//
    
    // This table is used to lookup the resource string id of a format string
    // used for formatting a start and end date that fall in the same month.
    // The index is constructed from a bit-wise OR of the boolean values:
    // {showTime, showYear, showWeekDay}.  For example, if showYear and
    // showWeekDay are both true, then the index would be 3.
    /** @deprecated Do not use. */
    public static final int sameMonthTable[] = {
    	Resources.getSystem().getIdentifier("same_month_md1_md2", "string", "android"),//com.android.internal.R.string.same_month_md1_md2,
    	Resources.getSystem().getIdentifier("same_month_wday1_md1_wday2_md2", "string", "android"),//com.android.internal.R.string.same_month_wday1_md1_wday2_md2,
    	Resources.getSystem().getIdentifier("same_month_mdy1_mdy2", "string", "android"),//com.android.internal.R.string.same_month_mdy1_mdy2,
    	Resources.getSystem().getIdentifier("same_month_wday1_mdy1_wday2_mdy2", "string", "android"),//com.android.internal.R.string.same_month_wday1_mdy1_wday2_mdy2,
    	Resources.getSystem().getIdentifier("same_month_md1_time1_md2_time2", "string", "android"),//com.android.internal.R.string.same_month_md1_time1_md2_time2,
    	Resources.getSystem().getIdentifier("same_month_wday1_md1_time1_wday2_md2_time2", "string", "android"),//com.android.internal.R.string.same_month_wday1_md1_time1_wday2_md2_time2,
    	Resources.getSystem().getIdentifier("same_month_mdy1_time1_mdy2_time2", "string", "android"),//com.android.internal.R.string.same_month_mdy1_time1_mdy2_time2,
    	Resources.getSystem().getIdentifier("same_month_wday1_mdy1_time1_wday2_mdy2_time2", "string", "android"),//com.android.internal.R.string.same_month_wday1_mdy1_time1_wday2_mdy2_time2,

    	Resources.getSystem().getIdentifier("numeric_md1_md2", "string", "android"),//com.android.internal.R.string.numeric_md1_md2,
    	Resources.getSystem().getIdentifier("numeric_wday1_md1_wday2_md2", "string", "android"),//com.android.internal.R.string.numeric_wday1_md1_wday2_md2,
    	Resources.getSystem().getIdentifier("numeric_mdy1_mdy2", "string", "android"),//com.android.internal.R.string.numeric_mdy1_mdy2,
    	Resources.getSystem().getIdentifier("numeric_wday1_mdy1_wday2_mdy2", "string", "android"),//com.android.internal.R.string.numeric_wday1_mdy1_wday2_mdy2,
    	Resources.getSystem().getIdentifier("numeric_md1_time1_md2_time2", "string", "android"),//com.android.internal.R.string.numeric_md1_time1_md2_time2,
    	Resources.getSystem().getIdentifier("numeric_wday1_md1_time1_wday2_md2_time2", "string", "android"),//com.android.internal.R.string.numeric_wday1_md1_time1_wday2_md2_time2,
    	Resources.getSystem().getIdentifier("numeric_mdy1_time1_mdy2_time2", "string", "android"),//com.android.internal.R.string.numeric_mdy1_time1_mdy2_time2,
    	Resources.getSystem().getIdentifier("numeric_wday1_mdy1_time1_wday2_mdy2_time2", "string", "android"),//com.android.internal.R.string.numeric_wday1_mdy1_time1_wday2_mdy2_time2,
    };
    
    private static void setTimeFromCalendar(Time t, Calendar c) {
        t.hour = c.get(Calendar.HOUR_OF_DAY);
        t.minute = c.get(Calendar.MINUTE);
        t.month = c.get(Calendar.MONTH);
        t.monthDay = c.get(Calendar.DAY_OF_MONTH);
        t.second = c.get(Calendar.SECOND);
        t.weekDay = c.get(Calendar.DAY_OF_WEEK) - 1;
        t.year = c.get(Calendar.YEAR);
        t.yearDay = c.get(Calendar.DAY_OF_YEAR);
        t.isDst = (c.get(Calendar.DST_OFFSET) != 0) ? 1 : 0;
        t.gmtoff = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
        t.timezone = c.getTimeZone().getID();
    }

	public static Formatter formatDateTimeRange(Context context, Formatter formatter, long startMillis,
            long endMillis, int flags, String timeZone) {
		// DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE = 17
		Resources res = Resources.getSystem();
        boolean showTime = (flags & FORMAT_SHOW_TIME) != 0;
        boolean showWeekDay = (flags & FORMAT_SHOW_WEEKDAY) != 0;
        boolean showYear = (flags & FORMAT_SHOW_YEAR) != 0;
        boolean noYear = (flags & FORMAT_NO_YEAR) != 0;
        boolean useUTC = (flags & FORMAT_UTC) != 0;
        boolean abbrevWeekDay = (flags & (FORMAT_ABBREV_WEEKDAY | FORMAT_ABBREV_ALL)) != 0;
        boolean abbrevMonth = (flags & (FORMAT_ABBREV_MONTH | FORMAT_ABBREV_ALL)) != 0;
        //boolean noMonth = (flags & FORMAT_NO_MONTH) != 0;
        boolean noMonthDay = (flags & FORMAT_NO_MONTH_DAY) != 0;
        boolean numericDate = (flags & FORMAT_NUMERIC_DATE) != 0;

        // If we're getting called with a single instant in time (from
        // e.g. formatDateTime(), below), then we can skip a lot of
        // computation below that'd otherwise be thrown out.
        boolean isInstant = (startMillis == endMillis);

        Calendar startCalendar, endCalendar;
        Time startDate = new Time();
        if (timeZone != null) {
            startCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else if (useUTC) {
            startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        } else {
            startCalendar = Calendar.getInstance();
        }
        startCalendar.setTimeInMillis(startMillis);
        setTimeFromCalendar(startDate, startCalendar);

        Time endDate = new Time();
        int dayDistance;
        if (isInstant) {
            endDate = startDate;
            dayDistance = 0;
        } else {
            if (timeZone != null) {
                endCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
            } else if (useUTC) {
                endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            } else {
                endCalendar = Calendar.getInstance();
            }
            endCalendar.setTimeInMillis(endMillis);
            setTimeFromCalendar(endDate, endCalendar);

            int startJulianDay = Time.getJulianDay(startMillis, startDate.gmtoff);
            int endJulianDay = Time.getJulianDay(endMillis, endDate.gmtoff);
            dayDistance = endJulianDay - startJulianDay;
        }

        if (!isInstant
            && (endDate.hour | endDate.minute | endDate.second) == 0
            && (!showTime || dayDistance <= 1)) {
            endDate.monthDay -= 1;
            endDate.normalize(true /* ignore isDst */);
        }

        int startDay = startDate.monthDay;
        int startMonthNum = startDate.month;
        int startYear = startDate.year;

        int endDay = endDate.monthDay;
        int endMonthNum = endDate.month;
        int endYear = endDate.year;

        String startWeekDayString = "";
        String endWeekDayString = "";
        if (showWeekDay) {
            String weekDayFormat = "";
            if (abbrevWeekDay) {
                weekDayFormat = ABBREV_WEEKDAY_FORMAT;
            } else {
                weekDayFormat = WEEKDAY_FORMAT;
            }
            startWeekDayString = startDate.format(weekDayFormat);
            endWeekDayString = isInstant ? startWeekDayString : endDate.format(weekDayFormat);
        }

        String startTimeString = "";
        String endTimeString = "";
        if (showTime) {
            String startTimeFormat = "";
            String endTimeFormat = "";
            boolean force24Hour = (flags & FORMAT_24HOUR) != 0;
            boolean force12Hour = (flags & FORMAT_12HOUR) != 0;
            boolean use24Hour;
            if (force24Hour) {
                use24Hour = true;
            } else if (force12Hour) {
                use24Hour = false;
            } else {
                use24Hour = android.text.format.DateFormat.is24HourFormat(context);
            }
            if (use24Hour) {
                startTimeFormat = endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_minute_24", "string", "android"));//res.getString(com.android.internal.R.string.hour_minute_24);
            } else {
                boolean abbrevTime = (flags & (FORMAT_ABBREV_TIME | FORMAT_ABBREV_ALL)) != 0;
                boolean capAMPM = (flags & FORMAT_CAP_AMPM) != 0;
                boolean noNoon = (flags & FORMAT_NO_NOON) != 0;
                boolean capNoon = (flags & FORMAT_CAP_NOON) != 0;
                boolean noMidnight = (flags & FORMAT_NO_MIDNIGHT) != 0;
                boolean capMidnight = (flags & FORMAT_CAP_MIDNIGHT) != 0;

                boolean startOnTheHour = startDate.minute == 0 && startDate.second == 0;
                boolean endOnTheHour = endDate.minute == 0 && endDate.second == 0;
                if (abbrevTime && startOnTheHour) {
                    if (capAMPM) {
                        startTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_cap_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_cap_ampm);
                    } else {
                        startTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_ampm);
                    }
                } else {
                    if (capAMPM) {
                        startTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_minute_cap_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_minute_cap_ampm);
                    } else {
                        startTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_minute_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_minute_ampm);
                    }
                }

                // Don't waste time on setting endTimeFormat when
                // we're dealing with an instant, where we'll never
                // need the end point.  (It's the same as the start
                // point)
                if (!isInstant) {
                    if (abbrevTime && endOnTheHour) {
                        if (capAMPM) {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_cap_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_cap_ampm);
                        } else {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_ampm);
                        }
                    } else {
                        if (capAMPM) {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_minute_cap_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_minute_cap_ampm);
                        } else {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("hour_minute_ampm", "string", "android"));//res.getString(com.android.internal.R.string.hour_minute_ampm);
                        }
                    }

                    if (endDate.hour == 12 && endOnTheHour && !noNoon) {
                        if (capNoon) {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("Noon", "string", "android"));//res.getString(com.android.internal.R.string.Noon);
                        } else {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("noon", "string", "android"));//res.getString(com.android.internal.R.string.noon);
                        }
                    } else if (endDate.hour == 0 && endOnTheHour && !noMidnight) {
                        if (capMidnight) {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("Midnight", "string", "android"));//res.getString(com.android.internal.R.string.Midnight);
                        } else {
                            endTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("midnight", "string", "android"));//res.getString(com.android.internal.R.string.midnight);
                        }
                    }
                }

                if (startDate.hour == 12 && startOnTheHour && !noNoon) {
                    if (capNoon) {
                        startTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("Noon", "string", "android"));//res.getString(com.android.internal.R.string.Noon);
                    } else {
                        startTimeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("noon", "string", "android"));//res.getString(com.android.internal.R.string.noon);
                    }
                    // Don't show the start time starting at midnight.  Show
                    // 12am instead.
                }
            }

            startTimeString = startDate.format(startTimeFormat);
            endTimeString = isInstant ? startTimeString : endDate.format(endTimeFormat);
        }

        // Show the year if the user specified FORMAT_SHOW_YEAR or if
        // the starting and end years are different from each other
        // or from the current year.  But don't show the year if the
        // user specified FORMAT_NO_YEAR.
        if (showYear) {
            // No code... just a comment for clarity.  Keep showYear
            // on, as they enabled it with FORMAT_SHOW_YEAR.  This
            // takes precedence over them setting FORMAT_NO_YEAR.
        } else if (noYear) {
            // They explicitly didn't want a year.
            showYear = false;
        } else if (startYear != endYear) {
            showYear = true;
        } else {
            // Show the year if it's not equal to the current year.
            Time currentTime = new Time();
            currentTime.setToNow();
            showYear = startYear != currentTime.year;
        }

        String defaultDateFormat, fullFormat, dateRange;
        if (numericDate) {
            defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("numeric_date", "string", "android"));//res.getString(com.android.internal.R.string.numeric_date);
        } else if (showYear) {
            if (abbrevMonth) {
                if (noMonthDay) {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("abbrev_month_year", "string", "android"));//res.getString(com.android.internal.R.string.abbrev_month_year);
                } else {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("abbrev_month_day_year", "string", "android"));//res.getString(com.android.internal.R.string.abbrev_month_day_year);
                }
            } else {
            	/*if (noMonth && noMonthDay) {
            		defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("year", "string", "android"));
            	}
            	else */if (noMonthDay) {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("month_year", "string", "android"));//res.getString(com.android.internal.R.string.month_year);
                } else {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("month_day_year", "string", "android"));//res.getString(com.android.internal.R.string.month_day_year);
                }
            }
        } else {
            if (abbrevMonth) {
                if (noMonthDay) {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("abbrev_month", "string", "android"));//res.getString(com.android.internal.R.string.abbrev_month);
                } else {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("abbrev_month_day", "string", "android"));//res.getString(com.android.internal.R.string.abbrev_month_day);
                }
            } else {
                if (noMonthDay) {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("month", "string", "android"));//res.getString(com.android.internal.R.string.month);
                } else {
                    defaultDateFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("month_day", "string", "android"));//res.getString(com.android.internal.R.string.month_day);
                }
            }
        }

        if (showWeekDay) {
            if (showTime) {
                fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("wday1_date1_time1_wday2_date2_time2", "string", "android"));//res.getString(com.android.internal.R.string.wday1_date1_time1_wday2_date2_time2);
            } else {
                fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("wday1_date1_wday2_date2", "string", "android"));//res.getString(com.android.internal.R.string.wday1_date1_wday2_date2);
            }
        } else {
            if (showTime) {
                fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("date1_time1_date2_time2", "string", "android"));//res.getString(com.android.internal.R.string.date1_time1_date2_time2);
            } else {
                fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("date1_date2", "string", "android"));//res.getString(com.android.internal.R.string.date1_date2);
            }
        }

        if (/*!noMonth && */noMonthDay && startMonthNum == endMonthNum && startYear == endYear) {
            // Example: "January, 2008"
            return formatter.format("%s", startDate.format(defaultDateFormat));
        }

        if (startYear != endYear || noMonthDay) {
            // Different year or we are not showing the month day number.
            // Example: "December 31, 2007 - January 1, 2008"
            // Or: "January - February, 2008"
            String startDateString = startDate.format(defaultDateFormat);
            String endDateString = endDate.format(defaultDateFormat);

            // The values that are used in a fullFormat string are specified
            // by position.
            return formatter.format(fullFormat,
                    startWeekDayString, startDateString, startTimeString,
                    endWeekDayString, endDateString, endTimeString);
        }

        // Get the month, day, and year strings for the start and end dates
        String monthFormat;
        if (numericDate) {
            monthFormat = NUMERIC_MONTH_FORMAT;
        } else if (abbrevMonth) {
            monthFormat =
            		Resources.getSystem().getString(Resources.getSystem().getIdentifier("short_format_month", "string", "android"));//res.getString(com.android.internal.R.string.short_format_month);
        } else {
            monthFormat = MONTH_FORMAT;
        }
        String startMonthString = startDate.format(monthFormat);
        String startMonthDayString = startDate.format(MONTH_DAY_FORMAT);
        String startYearString = startDate.format(YEAR_FORMAT);

        String endMonthString = isInstant ? null : endDate.format(monthFormat);
        String endMonthDayString = isInstant ? null : endDate.format(MONTH_DAY_FORMAT);
        String endYearString = isInstant ? null : endDate.format(YEAR_FORMAT);

        String startStandaloneMonthString = startMonthString;
        String endStandaloneMonthString = endMonthString;
        // We need standalone months for these strings in Persian (fa): http://b/6811327
        if (!numericDate && !abbrevMonth && Locale.getDefault().getLanguage().equals("fa")) {
            startStandaloneMonthString = startDate.format("%-B");
            endStandaloneMonthString = endDate.format("%-B");
        }

        if (startMonthNum != endMonthNum) {
            // Same year, different month.
            // Example: "October 28 - November 3"
            // or: "Wed, Oct 31 - Sat, Nov 3, 2007"
            // or: "Oct 31, 8am - Sat, Nov 3, 2007, 5pm"

            int index = 0;
            if (showWeekDay) index = 1;
            if (showYear) index += 2;
            if (showTime) index += 4;
            if (numericDate) index += 8;
            int resId = sameYearTable[index];
            fullFormat = res.getString(resId);

            // The values that are used in a fullFormat string are specified
            // by position.
            return formatter.format(fullFormat,
                    startWeekDayString, startMonthString, startMonthDayString,
                    startYearString, startTimeString,
                    endWeekDayString, endMonthString, endMonthDayString,
                    endYearString, endTimeString,
                    startStandaloneMonthString, endStandaloneMonthString);
        }

        if (startDay != endDay) {
            // Same month, different day.
            int index = 0;
            if (showWeekDay) index = 1;
            if (showYear) index += 2;
            if (showTime) index += 4;
            if (numericDate) index += 8;
            int resId = sameMonthTable[index];
            fullFormat = res.getString(resId);

            // The values that are used in a fullFormat string are specified
            // by position.
            return formatter.format(fullFormat,
                    startWeekDayString, startMonthString, startMonthDayString,
                    startYearString, startTimeString,
                    endWeekDayString, endMonthString, endMonthDayString,
                    endYearString, endTimeString,
                    startStandaloneMonthString, endStandaloneMonthString);
        }

        // Same start and end day
        boolean showDate = (flags & FORMAT_SHOW_DATE) != 0;

        // If nothing was specified, then show the date.
        if (!showTime && !showDate && !showWeekDay) showDate = true;

        // Compute the time string (example: "10:00 - 11:00 am")
        String timeString = "";
        if (showTime) {
            // If the start and end time are the same, then just show the
            // start time.
            if (isInstant) {
                // Same start and end time.
                // Example: "10:15 AM"
                timeString = startTimeString;
            } else {
                // Example: "10:00 - 11:00 am"
                String timeFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("time1_time2", "string", "android"));//res.getString(com.android.internal.R.string.time1_time2);
                // Don't use the user supplied Formatter because the result will pollute the buffer.
                timeString = String.format(timeFormat, startTimeString, endTimeString);
            }
        }

        // Figure out which full format to use.
        fullFormat = "";
        String dateString = "";
        if (showDate) {
            dateString = startDate.format(defaultDateFormat);
            if (showWeekDay) {
                if (showTime) {
                    // Example: "10:00 - 11:00 am, Tue, Oct 9"
                    fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("time_wday_date", "string", "android"));//res.getString(com.android.internal.R.string.time_wday_date);
                } else {
                    // Example: "Tue, Oct 9"
                    fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("wday_date", "string", "android"));//res.getString(com.android.internal.R.string.wday_date);
                }
            } else {
                if (showTime) {
                    // Example: "10:00 - 11:00 am, Oct 9"
                    fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("time_date", "string", "android"));//res.getString(com.android.internal.R.string.time_date);
                } else {
                    // Example: "Oct 9"
                    return formatter.format("%s", dateString);
                }
            }
        } else if (showWeekDay) {
            if (showTime) {
                // Example: "10:00 - 11:00 am, Tue"
                fullFormat = Resources.getSystem().getString(Resources.getSystem().getIdentifier("time_wday", "string", "android"));//res.getString(com.android.internal.R.string.time_wday);
            } else {
                // Example: "Tue"
                return formatter.format("%s", startWeekDayString);
            }
        } else if (showTime) {
            return formatter.format("%s", timeString);
        }

        // The values that are used in a fullFormat string are specified
        // by position.
        return formatter.format(fullFormat, timeString, startWeekDayString, dateString);
		
		
	}
	
}
