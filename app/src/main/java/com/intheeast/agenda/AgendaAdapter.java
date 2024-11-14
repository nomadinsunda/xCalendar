/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;

//import android.text.format.Time;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;




//import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

import com.intheeast.acalendar.R;
import com.intheeast.acalendar.Utils;
import com.intheeast.etc.ETime;


public class AgendaAdapter extends ResourceCursorAdapter {
    private final String mNoTitleLabel;
    private final Context mContext;
    private final Resources mResources;
    private final int mDeclinedColor;
    private final int mStandardColor;
    private final int mWhereColor;
    private final int mWhereDeclinedColor;
    // Note: Formatter is not thread safe. Fine for now as it is only used by the main thread.
    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    private float mScale;

    private int COLOR_CHIP_ALL_DAY_HEIGHT;
    private int COLOR_CHIP_HEIGHT;

    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
        }
    };    
    
    
    /////////////////////////////////////////////
    int mEventItemWidth;
    int mEventItemHeight;
    int mTimeContainerWidth;
    int mTimeContainerHeight;
    int mTitleLocationContainerHeight;
    
    float mTimeContainerLeftPadding;
    float mTitleContainerLeftPadding;
    
    int mTimeTextViewWidth;
    int mTimeTextViewHeight;
    int mTitleTextViewHeight;
    
    int mDividerTopMargin;
    int mDividerWidth;
    int mDividerHeight;
    
    int mTopDividerHeight;    
    int mLayoutResource;
    
    @SuppressWarnings("deprecation")
	public AgendaAdapter(Context context, int layoutResource, HashMap<String, Integer> eventItemDimensionParams) {
        super(context, layoutResource, null);

        mContext = context;
        // will be event_item.xml
        mLayoutResource = layoutResource;
        
        mResources = context.getResources();
        mNoTitleLabel = mResources.getString(R.string.no_title_label);
        mDeclinedColor = mResources.getColor(R.color.agenda_item_declined_color);
        mStandardColor = mResources.getColor(R.color.agenda_item_standard_color);
        mWhereDeclinedColor = mResources.getColor(R.color.agenda_item_where_declined_text_color);
        mWhereColor = mResources.getColor(R.color.agenda_item_where_text_color);
        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        COLOR_CHIP_ALL_DAY_HEIGHT = mResources.getInteger(R.integer.color_chip_all_day_height);
        COLOR_CHIP_HEIGHT = mResources.getInteger(R.integer.color_chip_height);
        if (mScale == 0) {
            mScale = mResources.getDisplayMetrics().density;
            if (mScale != 1) {
                COLOR_CHIP_ALL_DAY_HEIGHT *= mScale;
                COLOR_CHIP_HEIGHT *= mScale;
            }
        }
        
        getEventItemDimensionParams(eventItemDimensionParams);
    }
    
    public void getEventItemDimensionParams(HashMap<String, Integer> params) {
    	        
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_WIDTH)) {
    		mEventItemWidth = params.get(AgendaWindowAdapter.EVENT_ITEM_WIDTH);           
        } 
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_HEIGHT)) {
    		mEventItemHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_HEIGHT);           
    	} 
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TIMECONTAINER_WIDTH)) {
    		mTimeContainerWidth = params.get(AgendaWindowAdapter.EVENT_ITEM_TIMECONTAINER_WIDTH);           
    	} 
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TIMECONTAINER_HEIGHT)) {
    		mTimeContainerHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_TIMECONTAINER_HEIGHT);           
       	} 
    	
    	/*if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TITLELOCATIONCONTAINER_WIDTH)) {
    		mTitleLocationContainerHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_TITLELOCATIONCONTAINER_WIDTH);           
       	}*/ // ����ν�� �ʿ� ���� 
    	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TITLELOCATIONCONTAINER_HEIGHT)) {
    		mTitleLocationContainerHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_TITLELOCATIONCONTAINER_HEIGHT);           
       	}     	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TIMECONTAINER_LEFTPADDING)) {
    		mTimeContainerLeftPadding = params.get(AgendaWindowAdapter.EVENT_ITEM_TIMECONTAINER_LEFTPADDING);           
       	}     	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TITLELOCATIONCONTAINER_LEFTPADDING)) {
    		mTitleContainerLeftPadding = params.get(AgendaWindowAdapter.EVENT_ITEM_TITLELOCATIONCONTAINER_LEFTPADDING);           
       	}  
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TIME_TEXTVIEW_WIDTH)) {
    		mTimeTextViewWidth = params.get(AgendaWindowAdapter.EVENT_ITEM_TIME_TEXTVIEW_WIDTH);           
       	}     	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TIME_TEXTVIEW_HEIGHT)) {
    		mTimeTextViewHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_TIME_TEXTVIEW_HEIGHT);           
       	}     	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TITLE_TEXTVIEW_HEIGHT)) {
    		mTitleTextViewHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_TITLE_TEXTVIEW_HEIGHT);           
       	}     	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_DIVIDER_WIDTH)) {
    		mDividerWidth = params.get(AgendaWindowAdapter.EVENT_ITEM_DIVIDER_WIDTH);           
       	}     	
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_DIVIDER_HEIGHT)) {
    		mDividerHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_DIVIDER_HEIGHT);           
       	}
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_DIVIDER_TOPMARGIN)) {
    		mDividerTopMargin = params.get(AgendaWindowAdapter.EVENT_ITEM_DIVIDER_TOPMARGIN);           
      	}
    	
    	if (params.containsKey(AgendaWindowAdapter.EVENT_ITEM_TOPDIVIDER_HIGHT)) {
    		mTopDividerHeight = params.get(AgendaWindowAdapter.EVENT_ITEM_TOPDIVIDER_HIGHT);           
     	}
    	
    }	

    static class ViewHolder {
        public static final int DECLINED_RESPONSE = 0;
        public static final int TENTATIVE_RESPONSE = 1;
        public static final int ACCEPTED_RESPONSE = 2;

        
        LinearLayout eventItemContainer;        
        LinearLayout eventItemTimecontainer;        
        TextView startTime;
        TextView endTime;
        
        View divider;        
        
        LinearLayout eventItemTitleLocationContainer;  
        TextView title;
        TextView where;
        
        View topDivider;
        
        long instanceId;        
        long startTimeMilli;
        boolean allDay;
        boolean grayed;
        int julianDay;
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = null;

        // Listview may get confused and pass in a different type of view since
        // we keep shifting data around. Not a big problem.
        Object tag = view.getTag();
        if (tag instanceof ViewHolder) {
            holder = (ViewHolder) view.getTag();
        }

        // holder�� null �̶�� ���� �� view���� dimension�� �������� �ʾҴٴ� ���� �ǹ��ϱ⵵ �Ѵ�
        if (holder == null) {
        	// R.dimen�� event item height�� app ��Ī �Ŀ� ������ �� �ִ� ����� ���°�? 
        	// :����ν�� ���� �� ����
        	LayoutParams params = new LayoutParams(mEventItemWidth, mEventItemHeight);
        	view.setLayoutParams(params);
        	
            holder = new ViewHolder();
            view.setTag(holder);
            
            holder.eventItemContainer = (LinearLayout) view.findViewById(R.id.event_item_container);            
    		LayoutParams itemContainerParams = new LayoutParams(mEventItemWidth, mTimeContainerHeight);
    		holder.eventItemContainer.setLayoutParams(itemContainerParams);               		
    		
    		holder.topDivider = view.findViewById(R.id.event_item_top_divider);
            LayoutParams underLineParams = new LayoutParams(mEventItemWidth, mTopDividerHeight);
            underLineParams.setMargins((int) mTitleContainerLeftPadding, 0, 0, 0);
    		holder.topDivider.setLayoutParams(underLineParams); 
    		
    		
            holder.eventItemTimecontainer = (LinearLayout) holder.eventItemContainer.findViewById(R.id.event_item_timecontainer);  
            LayoutParams timeContainerParams = new LayoutParams(mTimeContainerWidth, mTimeContainerHeight);
            holder.eventItemTimecontainer.setLayoutParams(timeContainerParams);
            holder.eventItemTimecontainer.setPadding((int) mTimeContainerLeftPadding, 0, 0, 0);  
            
            holder.startTime = (TextView) holder.eventItemTimecontainer.findViewById(R.id.event_item_starttime);
            LayoutParams startTimeParams = (LayoutParams) holder.startTime.getLayoutParams();
            startTimeParams.width = mTimeTextViewWidth;
            startTimeParams.height = mTimeTextViewHeight;
            holder.startTime.setLayoutParams(startTimeParams);
            
            holder.endTime = (TextView) holder.eventItemTimecontainer.findViewById(R.id.event_item_endtime);
            LayoutParams endTimeParams = (LayoutParams) holder.endTime.getLayoutParams();
            endTimeParams.width = mTimeTextViewWidth;
            endTimeParams.height = mTimeTextViewHeight;
            holder.endTime.setLayoutParams(endTimeParams);            
            
            holder.divider = holder.eventItemContainer.findViewById(R.id.event_item_divider);
            LayoutParams dividerParams = new LayoutParams(mDividerWidth, mDividerHeight);
            dividerParams.setMargins(0, mDividerTopMargin, 0, 0);
    		holder.divider.setLayoutParams(dividerParams); 
    		
            holder.eventItemTitleLocationContainer = (LinearLayout) holder.eventItemContainer.findViewById(R.id.event_item_titlelocationcontainer);
            LayoutParams titleLocationContainerParams = new LayoutParams(LayoutParams.MATCH_PARENT, mTimeContainerHeight);
            holder.eventItemTitleLocationContainer.setLayoutParams(titleLocationContainerParams);
            holder.eventItemTitleLocationContainer.setPadding((int) mTitleContainerLeftPadding, 0, 0, 0);
            
            holder.title = (TextView) holder.eventItemTitleLocationContainer.findViewById(R.id.event_item_title);
            LayoutParams titleParams = (LayoutParams) holder.title.getLayoutParams();            
            titleParams.height = mTitleTextViewHeight;
            holder.title.setLayoutParams(titleParams);           
            
            holder.where = (TextView) holder.eventItemTitleLocationContainer.findViewById(R.id.event_item_location);   
            LayoutParams locationParams = (LayoutParams) holder.where.getLayoutParams();            
            locationParams.height = mTitleTextViewHeight;
            holder.where.setLayoutParams(locationParams);        
            
        }
        
        if(holder.topDivider.getVisibility() != View.VISIBLE) {
        	holder.topDivider.setVisibility(View.VISIBLE);
        }

        holder.startTimeMilli = cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
        // Fade text if event was declined and set the color chip mode (response
        boolean allDay = cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
        holder.allDay = allDay;        

        TextView startTime = holder.startTime;
        TextView endTime = holder.endTime;
        
        TextView title = holder.title;        
        TextView where = holder.where;

        holder.instanceId = cursor.getLong(AgendaWindowAdapter.INDEX_INSTANCE_ID);

        /* Calendar Color */
        int color = Utils.getDisplayColorFromColor(cursor.getInt(AgendaWindowAdapter.INDEX_COLOR));
        holder.divider.setBackgroundColor(color);

        // What
        String titleString = cursor.getString(AgendaWindowAdapter.INDEX_TITLE);
        if (titleString == null || titleString.length() == 0) {
            titleString = mNoTitleLabel;
        }
        title.setText(titleString);

        // When
        int startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);
        int endDay = cursor.getInt(AgendaWindowAdapter.INDEX_END_DAY);
        
        long begin = cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
        long end = cursor.getLong(AgendaWindowAdapter.INDEX_END);
        
        String eventTz = cursor.getString(AgendaWindowAdapter.INDEX_TIME_ZONE);
        
        int flags = 0;
        
        // It's difficult to update all the adapters so just query this each
        // time we need to build the view.
        String tzString = Utils.getTimeZone(context, mTZUpdater);
        if (allDay) {
        	String allDayString = "�Ϸ� ����";
        	startTime.setText(allDayString); 
        	endTime.setVisibility(View.GONE);
        	
        } else {
            flags = ETime.FORMAT_SHOW_TIME;
            
            String startString;
            mStringBuilder.setLength(0);
            startString = ETime.formatDateTimeRange(context, mFormatter, begin, begin, flags, tzString)
                    .toString();            
            startTime.setText(startString); 
            
            if (holder.endTime.getVisibility() != View.VISIBLE)
            	holder.endTime.setVisibility(View.VISIBLE);
            
            String endString;
            mStringBuilder.setLength(0);
            endString = ETime.formatDateTimeRange(context, mFormatter, end, end, flags, tzString)
                    .toString();            
            endTime.setText(endString); 
        }              

        // Where
        String whereString = cursor.getString(AgendaWindowAdapter.INDEX_EVENT_LOCATION);
        if (whereString != null && whereString.length() > 0) {
            where.setVisibility(View.VISIBLE);
            where.setText(whereString);
        } else {
            where.setVisibility(View.GONE);
        }
    }    
    
}

