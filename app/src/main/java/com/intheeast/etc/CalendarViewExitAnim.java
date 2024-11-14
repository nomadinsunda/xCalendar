package com.intheeast.etc;

import java.util.ArrayList;

import com.intheeast.acalendar.Event;

public abstract class CalendarViewExitAnim {
	public abstract void setEvents(int firstJulianDay, int lastJulianDay, int numDays, ArrayList<Event> events);
}
