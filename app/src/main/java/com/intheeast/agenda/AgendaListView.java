/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.intheeast.agenda;



import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.intheeast.agenda.AgendaByDayAdapter.RowInfo;
import com.intheeast.agenda.AgendaWindowAdapter.AgendaItem;
import com.intheeast.agenda.AgendaWindowAdapter.DayAdapterInfo;
import com.intheeast.acalendar.DeleteEventHelper;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
//import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class AgendaListView extends ListView implements OnItemClickListener {

    private static final String TAG = "AgendaListView";
    private static final boolean INFO = false;
    
    private static final int EVENT_UPDATE_TIME = 300000;  // 5 minutes

    private AgendaFragment mFragment;
    private SearchAgendaFragment mSearchFragment;
    private AgendaWindowAdapter mWindowAdapter;
    private DeleteEventHelper mDeleteEventHelper;
    private Context mContext;
    private String mTzId;
    private TimeZone mTimeZone;
    private long mGmtoff;
    int mFirstDayOfWeek;
    private Calendar mTime;
    private boolean mShowEventDetailsWithAgenda;
    private Handler mHandler = null;

    private boolean mUsedForSearch;
    
    Drawable mDefaultOverScrollHeader;
    Drawable mDefaultOverScrollFooter;
    
    Drawable mAndroidOverScrollGlow;
    
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
        	mTzId = Utils.getTimeZone(mContext, this);
        	mTimeZone = TimeZone.getTimeZone(mTzId);
        	mGmtoff = mTimeZone.getRawOffset() / 1000;
            mTime.setTimeZone(mTimeZone);
        }
    };

    // runs every midnight and refreshes the view in order to update the past/present
    // separator
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            refresh(true);
            Utils.setMidnightUpdater(mHandler, mMidnightUpdater, mTzId);
        }
    };
    
    public AgendaListView(Context context) {
        super(context);
        initView(context);
    }

    public AgendaListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public AgendaListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }  
    
    private void initView(Context context) {
        mContext = context;
        mTzId = Utils.getTimeZone(context, mTZUpdater);
        mTimeZone = TimeZone.getTimeZone(mTzId);
        mGmtoff = mTimeZone.getRawOffset() / 1000;
        
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        
        mTime = GregorianCalendar.getInstance(mTimeZone);
        
        mDeleteEventHelper =
                new DeleteEventHelper(context, null, false /* don't exit when done */);
        mShowEventDetailsWithAgenda = Utils.getConfigBool(mContext,
                R.bool.show_event_details_with_agenda);
        
        mHandler = new Handler();
    }
    
    // onMeasure���� getView�� ȣ��Ǵ� ���� �ذ��� �����
    // -> �̷� ���� mRowCount�� ��������Ʈ �� ������ , 
    //    setSelectionFromTop���� ������ position�� �� �ѹ��� getView���� ���޵��� �ʰ�
    //    position�� listview�� height�� �°� position 0���� ���������� ȣ��ǰ�
    //    ���Ŀ� setSelectionFromTop���� ������ position�� getView���� ���޵Ǵ� �������� �߻���
    // :AgendaFragment::onActivityCreated ���ƿ���  
    //  AgendaListView�� adapter�� �����ϰ� setAdapter�� ȣ���� ��???
    //  ->�̷� ���� onLayout���� getView���� setSelectionFromTop���� ������ position ���� �����Ѵ�
    public AgendaWindowAdapter setUpList(AgendaFragment fragment, boolean usedForSearch) { 
    	if (INFO) Log.i(TAG, "setUpList!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	
    	mFragment = fragment;
    	mUsedForSearch = usedForSearch;
    	
    	setOnItemClickListener(this);    	
        // Transparent background on scroll
    	setCacheColorHint(0);
    	//setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        // No dividers
    	setDivider(null);
        // Items are clickable
    	setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
    	setFastScrollEnabled(false); // �����ʿ� �н�Ʈ ��ũ�ѹٰ� �����ȴ� : �н�Ʈ ��ũ�ѿ� ���� ���� ������ �𸥴�
    	setVerticalScrollBarEnabled(false);
    	setClickable(true);
    	//mAgendaListView.setOnScrollListener(this);
    	setFadingEdgeLength(0);
    	
    	// null ���� ���ϵ�...
    	//mDefaultOverScrollHeader = getOverscrollHeader();
    	//mDefaultOverScrollFooter = getOverscrollFooter();
    	
    	setOverscrollHeaderNullDrawable();
    	setOverscrollFooterNullDrawable();
    	    	
    	mWindowAdapter = new AgendaWindowAdapter(mContext, this, mUsedForSearch,
                Utils.getConfigBool(mContext, R.bool.show_event_details_with_agenda));
    	//mWindowAdapter.setSelectedInstanceId(-1/* TODO:instanceId */);
    	setAdapter(mWindowAdapter);
    	
    	makeCustomOverscrollHFColor();
    	
    	return mWindowAdapter;
    }
    
    public AgendaWindowAdapter setUpList(SearchAgendaFragment fragment, boolean usedForSearch) { 
    	if (INFO) Log.i(TAG, "setUpList!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	
    	mSearchFragment = fragment;
    	mUsedForSearch = usedForSearch;
    	
    	setOnItemClickListener(this);    	
        // Transparent background on scroll
    	setCacheColorHint(0);
    	//setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        // No dividers
    	setDivider(null);
        // Items are clickable
    	setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
    	setFastScrollEnabled(false); // �����ʿ� �н�Ʈ ��ũ�ѹٰ� �����ȴ� : �н�Ʈ ��ũ�ѿ� ���� ���� ������ �𸥴�
    	setVerticalScrollBarEnabled(false);
    	setClickable(true);
    	//mAgendaListView.setOnScrollListener(this);
    	setFadingEdgeLength(0);
    	
    	mWindowAdapter = new AgendaWindowAdapter(mContext, this, mUsedForSearch,
                Utils.getConfigBool(mContext, R.bool.show_event_details_with_agenda));
    	//mWindowAdapter.setSelectedInstanceId(-1/* TODO:instanceId */);
    	setAdapter(mWindowAdapter);
    	    	
    	
    	return mWindowAdapter;
    }
    
    public void makeCustomOverscrollHFColor() {
    	int glowDrawableId = mContext.getResources().getIdentifier("overscroll_glow", "drawable", "android");
    	mAndroidOverScrollGlow = mContext.getResources().getDrawable(glowDrawableId);
    	int brandColor = Color.RED;
    	mAndroidOverScrollGlow.setColorFilter(brandColor, Mode.MULTIPLY);    	
    }
    
    public void setOverscrollHeaderNullDrawable() {
    	setOverscrollHeader(null);    	
    }
    
    public void setOverscrollFooterNullDrawable() {    	
    	setOverscrollFooter(null);
    }
    
    public void setOverscrollHeaderCustomDrawable() {
    	setOverscrollHeader(mAndroidOverScrollGlow);    	
    }
    
    public void setOverscrollFooterCustomDrawable() {    	
    	setOverscrollFooter(mAndroidOverScrollGlow);
    }
    
    

    // Sets a thread to run every EVENT_UPDATE_TIME in order to update the list
    // with grayed out past events
    /*
    private void setPastEventsUpdater() {

        // Run the thread in the nearest rounded EVENT_UPDATE_TIME
        long now = System.currentTimeMillis();
        long roundedTime = (now / EVENT_UPDATE_TIME) * EVENT_UPDATE_TIME;
        mHandler.removeCallbacks(mPastEventUpdater);
        mHandler.postDelayed(mPastEventUpdater, EVENT_UPDATE_TIME - (now - roundedTime));
    }

    // Stop the past events thread
    private void resetPastEventsUpdater() {
        mHandler.removeCallbacks(mPastEventUpdater);
    }
    */

    // Go over all visible views and checks if all past events are grayed out.
    // Returns true is there is at least one event that ended and it is not
    // grayed out.
    private boolean updatePastEvents() {

        int childCount = getChildCount();
        boolean needUpdate = false;
        long now = System.currentTimeMillis();
        Calendar time = GregorianCalendar.getInstance(mTimeZone);
        time.setTimeInMillis(now);
         
        int todayJulianDay = ETime.getJulianDay(now, mTimeZone, mFirstDayOfWeek);

        // Go over views in list
        for (int i = 0; i < childCount; ++i) {
            View listItem = getChildAt(i);
            Object o = listItem.getTag();
            if (o instanceof AgendaByDayAdapter.ViewHolder) {
                // day view - check if day in the past and not grayed yet
                AgendaByDayAdapter.ViewHolder holder = (AgendaByDayAdapter.ViewHolder) o;
                if (holder.julianDay <= todayJulianDay && !holder.grayed) {
                    needUpdate = true;
                    break;
                }
            } else if (o instanceof AgendaAdapter.ViewHolder) {
                // meeting view - check if event in the past or started already and not grayed yet
                // All day meetings for a day are grayed out
                AgendaAdapter.ViewHolder holder = (AgendaAdapter.ViewHolder) o;
                if (!holder.grayed && ((!holder.allDay && holder.startTimeMilli <= now) ||
                        (holder.allDay && holder.julianDay <= todayJulianDay))) {
                    needUpdate = true;
                    break;
                }
            }
        }
        return needUpdate;
    }

    boolean mEventInfoViewSwitched = false;
    public void willBeSwitchEventInfoView() {
    	mEventInfoViewSwitched = true;
    }
    
    @Override
    protected void onAttachedToWindow() {
    	super.onAttachedToWindow();
    	
    	// mEventInfoViewSwitched�� true ��� ����
    	// EventInfoView�� switching �Ǿ��ٰ� �ٽ� ������ ���� �ǹ��Ѵ�
    	// �׷��Ƿ� ���Ŀ� �� ���� �� �ִ� EventInfoView switching�� ����ؼ�
    	// mEventInfoViewSwitched�� false�� �����Ѵ�
    	if (mEventInfoViewSwitched)
    		mEventInfoViewSwitched = false;
    	
    	if (INFO) Log.i(TAG, "onAttachedToWindow!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    // ************************************************************//
    // FastEventInfoFragment���� EventInfoView�� Switching�� �� ȣ��ȴ�
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // ���⼭ EventInfoView switching���� ���� onDetachedFromWindow�� ȣ��Ǿ��ٸ�
        // ������� close �Լ��� ȣ���ؼ��� �ȵȴ�!!!
        // :�׷���,,, EventInfoView�κ��� �������� �� mWindowAdapter�� Zero counts�� ListView���� �����ؼ�
        //  �ƹ��͵� ���÷��̵��� �ʴ� �������� �߻��Ѵ�
        if (!mEventInfoViewSwitched)
        	mWindowAdapter.close();
        
        if (INFO) Log.i(TAG, "onDetachedFromWindow!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    // Implementation of the interface OnItemClickListener
    @Override
    public void onItemClick(AdapterView<?> a, View v, int position, long id) {   	
    	
        if (id != -1) {
        	if (INFO) Log.i(TAG, "onItemClick : id=" + String.valueOf(id));
        	
        	AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
        	if (item != null) {
        		DayAdapterInfo info = mWindowAdapter.getAdapterInfoByPosition(position);
        		int curPos = position - info.offset;
            	RowInfo row = info.dayAdapter.getRowInfo(curPos);
            	//int startDay = item.startDay; // startDay�� ���� �����ϴ� �ð����̴�...�׷��Ƿ� ���� ���� ��ġ�� event�� �� ���� �����ؼ��� �ȵȴ�
                								// �ش� item�� ��ġ�� day Header�� date��...
            	int startDay = row.mDay;
            	
        		//long eventId = item.id; // ���ݱ��� �߸��� event id�� �����ϰ� �־���
            	long eventId = row.mEventId;
        		
                long startTime = item.begin;
                long endTime = item.end;                
            	
                if (!mUsedForSearch)
                	mFragment.launchFastEventInfoView(eventId, startDay,
                			startTime, endTime);
                else 
                	mSearchFragment.launchFastEventInfoView(eventId, startDay,
                			startTime, endTime);
        	}
            //long oldInstanceId = mWindowAdapter.getSelectedInstanceId();
        	
        	/*
            // Switch to the EventInfo view
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            long oldInstanceId = mWindowAdapter.getSelectedInstanceId();
            mWindowAdapter.setSelectedView(v);

            // If events are shown to the side of the agenda list , do nothing
            // when the same event is selected , otherwise show the selected event.

            if (item != null && (oldInstanceId != mWindowAdapter.getSelectedInstanceId() ||
                    !mShowEventDetailsWithAgenda)) {
                long startTime = item.begin;
                long endTime = item.end;
                // Holder in view holds the start of the specific part of a multi-day event ,
                // use it for the goto
                long holderStartTime;
                Object holder = v.getTag();
                if (holder instanceof AgendaAdapter.ViewHolder) {
                    holderStartTime = ((AgendaAdapter.ViewHolder) holder).startTimeMilli;
                } else {
                    holderStartTime = startTime;
                }
                if (item.allDay) {
                    startTime = Utils.convertAlldayLocalToUTC(mTime, startTime, mTimeZone);
                    endTime = Utils.convertAlldayLocalToUTC(mTime, endTime, mTimeZone);
                }
                mTime.set(startTime);
                CalendarController controller = CalendarController.getInstance(mContext);
                controller.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, item.id,
                        startTime, endTime, 0, 0, CalendarController.EventInfo.buildViewExtraLong(
                                Attendees.ATTENDEE_STATUS_NONE, item.allDay), holderStartTime);
            }
            */
        }
        else {
        	if (INFO) Log.i(TAG, "onItemClick : id= -1");
        }
    }

    public void goTo(Calendar time, long id, String searchQuery, boolean forced,
            boolean refreshEventInfo) {
    	if (INFO) Log.i(TAG, "goTo");
    	
        if (time == null) {
            //time = mTime;
            time = GregorianCalendar.getInstance(mTimeZone);
            long goToTime = getFirstVisibleTime(null);
            if (goToTime <= 0) {
                goToTime = System.currentTimeMillis();
            }
            time.setTimeInMillis(goToTime);
        }
        
        mTime.setTimeInMillis(time.getTimeInMillis());        
        mTime.setTimeZone(mTimeZone);
        
        if (INFO) {
            Log.i(TAG, "Goto with time " + mTime.toString());
        }
        mWindowAdapter.refresh(mTime, id, searchQuery, forced, refreshEventInfo);
    }

    @SuppressLint("SuspiciousIndentation")
    public void refresh(boolean forced) {
    	if (INFO) Log.i(TAG, "refresh");
        mWindowAdapter.refresh(mTime, -1, null, forced, false);
    }

    public void deleteSelectedEvent() {
        int position = getSelectedItemPosition();
        AgendaItem agendaItem = mWindowAdapter.getAgendaItemByPosition(position);
        if (agendaItem != null) {
            mDeleteEventHelper.delete(agendaItem.begin, agendaItem.end, agendaItem.id, -1);
        }
    }

    public View getFirstVisibleView() {
        Rect r = new Rect();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View listItem = getChildAt(i);
            listItem.getLocalVisibleRect(r);
            if (r.top >= 0) { // if visible
                return listItem;
            }
        }
        return null;
    }

    public long getSelectedTime() {
        int position = getSelectedItemPosition();
        if (position >= 0) {
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            if (item != null) {
                return item.begin;
            }
        }
        return getFirstVisibleTime(null);
    }

    /*
    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return mWindowAdapter.getSelectedViewHolder();
    }
	*/
    
    public long getFirstVisibleTime(AgendaItem item) {
        AgendaItem agendaItem = item;
        if (item == null) {
            agendaItem = getFirstVisibleAgendaItem();
        }
        if (agendaItem != null) {
            Calendar t = GregorianCalendar.getInstance(mTimeZone);
            t.setTimeInMillis(agendaItem.begin);
            // Save and restore the time since setJulianDay sets the time to 00:00:00
            int hour = t.get(Calendar.HOUR_OF_DAY);
            int minute = t.get(Calendar.MINUTE);
            int second = t.get(Calendar.SECOND);
            long startDayMillis = ETime.getMillisFromJulianDay(agendaItem.startDay, mTimeZone, mFirstDayOfWeek);
            t.setTimeInMillis(startDayMillis); //t.setJulianDay(agendaItem.startDay);
            
            t.set(Calendar.HOUR_OF_DAY, hour);//t.hour = hour;
            t.set(Calendar.MINUTE, minute);//t.minute = minute;
            t.set(Calendar.SECOND, second);//t.second = second;
            if (INFO) {
                //t.normalize(true);
                Log.i(TAG, "first position had time " + t.toString());
            }
            return t.getTimeInMillis();
        }
        return 0;
    }

    public AgendaItem getFirstVisibleAgendaItem() {
        int position = getFirstVisiblePosition();
        if (INFO) {
            Log.i(TAG, "getFirstVisiblePosition = " + position);
        }

        // mShowEventDetailsWithAgenda == true implies we have a sticky header. In that case
        // we may need to take the second visible position, since the first one maybe the one
        // under the sticky header.
        if (mShowEventDetailsWithAgenda) {
            View v = getFirstVisibleView ();
            if (v != null) {
                Rect r = new Rect ();
                v.getLocalVisibleRect(r);
                if (r.bottom - r.top <=  0/*mWindowAdapter.getStickyHeaderHeight()*/) {
                    position ++;
                }
            }
        }

        return mWindowAdapter.getAgendaItemByPosition(position,
                false /* startDay = date separator date instead of actual event startday */);

    }

    public int getJulianDayFromPosition(int position) {
        DayAdapterInfo info = mWindowAdapter.getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.findJulianDayFromPosition(position - info.offset);
        }
        return 0;
    }

    // Finds is a specific event (defined by start time and id) is visible
    public boolean isAgendaItemVisible(Calendar startTime, long id) {

        if (id == -1 || startTime == null) {
            return false;
        }

        View child = getChildAt(0);
        // View not set yet, so not child - return
        if (child == null) {
            return false;
        }
        int start = getPositionForView(child);
        long milliTime = startTime.getTimeInMillis();
        int childCount = getChildCount();
        int eventsInAdapter = mWindowAdapter.getCount();

        for (int i = 0; i < childCount; i++) {
            if (i + start >= eventsInAdapter) {
                break;
            }
            AgendaItem agendaItem = mWindowAdapter.getAgendaItemByPosition(i + start);
            if (agendaItem == null) {
                continue;
            }
            if (agendaItem.id == id && agendaItem.begin == milliTime) {
                View listItem = getChildAt(i);
                if (listItem.getTop() <= getHeight() &&
                        listItem.getTop() >= 0/*mWindowAdapter.getStickyHeaderHeight()*/) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    public long getSelectedInstanceId() {
        return mWindowAdapter.getSelectedInstanceId();
    }

    public void setSelectedInstanceId(long id) {
        mWindowAdapter.setSelectedInstanceId(id);
    }
	*/
    
    // Move the currently selected or visible focus down by offset amount.
    // offset could be negative.
    public void shiftSelection(int offset) {
    	//if (INFO) Log.i(TAG, "**********<shiftSelection>**********");
    	
        shiftPosition(offset);
        // ���� ���Ǵ� �ȵ���̵� ���� ��ǻ� ��κ� touch mode�� ��찡 �����Ƿ�
        // ���� position ���� INVALID_POSITION�� ���̴�
        int position = getSelectedItemPosition();////////////////////////////////////////////////////////////////////
        
        if (position != INVALID_POSITION) {
        	int newPosition = position + offset;
            setSelectionFromTop(newPosition, 0);
            if (INFO) Log.i(TAG, "shiftSelection : newPosition=" + String.valueOf(newPosition));            
        }
        else {
        	if (INFO) Log.i(TAG, "shiftSelection : position == INVALID_POSITION");
        }
        
        //if (INFO) Log.i(TAG, "**********>shiftSelection<**********");
    }

    private void shiftPosition(int offset) {
        //if (INFO) Log.i(TAG, "Shifting position " + offset);
        // ���� �Ϻ��ϰ� listview�� top edge ���Ͽ� ��ġ�� view�� �����Ѵ�
    	// 
        View firstVisibleItem = getFirstVisibleView();

        if (firstVisibleItem != null) {
            Rect r = new Rect();
            firstVisibleItem.getLocalVisibleRect(r);
            // if r.top is < 0, getChildAt(0) and getFirstVisiblePosition() is
            // returning an item above the first visible item.
            int position = getPositionForView(firstVisibleItem);
            int newPosition = position + offset;
            //if (INFO) Log.i(TAG, "shiftPosition : newPosition=" + String.valueOf(newPosition));  
            // �׷��� firstVisibleItem�� top�� 0���� ū ��찡 �߻��� �� �ִ°�?
            // ;������� �߸� �����ϰ� �ִ� ���ΰ�??? : ���ݱ��� firstVisibleItem�� top�� 0���� �۰ų� ���ٶ�� �����ϰ� �־���
            //  ���� firstVisibleItem�� top�� 0���� ū ��찡 �߻��� �� �ִٸ� �Ʒ� �ڵ�� �������� �ڵ���
            //  firstVisibleItem�� top�� 0���� ũ�ٸ� ���� �÷��� �ϰ� �۴ٸ� �Ʒ��� ���� listview�� top�� ���ߴ� ���� ����!!!
            setSelectionFromTop(position + offset, r.top > 0 ? -r.top : r.top);
            /*
            if (INFO) {
                if (firstVisibleItem.getTag() instanceof AgendaAdapter.ViewHolder) {
                    ViewHolder viewHolder = (AgendaAdapter.ViewHolder) firstVisibleItem.getTag();
                    Log.i(TAG, "Shifting from " + position + " by " + offset + ". Title "
                            + viewHolder.title.getText());
                } else if (firstVisibleItem.getTag() instanceof AgendaByDayAdapter.ViewHolder) {
                    AgendaByDayAdapter.ViewHolder viewHolder =
                            (AgendaByDayAdapter.ViewHolder) firstVisibleItem.getTag();
                    Log.i(TAG, "Shifting from " + position + " by " + offset + ". Date  "
                            + viewHolder.dateView.getText());
                } else if (firstVisibleItem instanceof TextView) {
                    Log.i(TAG, "Shifting: Looking at header here. " + getSelectedItemPosition());
                }
            }
            */
        } else if (getSelectedItemPosition() >= 0) {
        	// �ش� ���ǹ� ��ƾ�� ������ �ɱ�? ��κ� getSelectedItemPosition�� INVALID_POSITION�� �������ٵ� ���̴�
            if (INFO) Log.i(TAG, "Shifting selection from " + getSelectedItemPosition() + " by " + offset);
            
            // If in touch mode, the item will not be selected but it will still be positioned appropriately. 
            setSelection(getSelectedItemPosition() + offset);
        }
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        mWindowAdapter.setHideDeclinedEvents(hideDeclined);
    }

    
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	if (INFO) Log.i(TAG, "onSizeChanged!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		super.onSizeChanged(w, h, oldw, oldh);
	}

	public void onResume() {
		if (INFO) Log.i(TAG, "onResume!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        mTZUpdater.run();
        Utils.setMidnightUpdater(mHandler, mMidnightUpdater, mTzId);
        //setPastEventsUpdater();
        mWindowAdapter.onResume();
    }

    public void onPause() {
    	if (INFO) Log.i(TAG, "onPause!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        Utils.resetMidnightUpdater(mHandler, mMidnightUpdater);
        //resetPastEventsUpdater();
    }
    
    public void setAgendaDayIndicatorVisibleState(boolean visible) {
    	
    	if (!mUsedForSearch)
        	mFragment.setAgendaDayIndicatorVisibleState(visible);
        else 
        	mSearchFragment.setAgendaDayIndicatorVisibleState(visible);
    }
    
    public void setEmptyListViewState() {
    	mWindowAdapter.setCleanAdapterState();
    }
}
