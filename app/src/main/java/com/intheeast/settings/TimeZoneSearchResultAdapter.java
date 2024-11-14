package com.intheeast.settings;

import java.util.ArrayList;
import java.util.Collections;

import com.intheeast.acalendar.R;
import com.intheeast.settings.TimeZoneOverrideSubView.OnHomeTimeZoneSetListener;
import com.intheeast.timezone.TimeZoneData;
import com.intheeast.timezone.TimeZoneInfo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class TimeZoneSearchResultAdapter extends BaseAdapter implements Filterable, 
	OnItemClickListener {
	
	private static final String TAG = "TimeZoneSearchResultAdapter";
	private static final boolean INFO = true;
	
	public static final int FILTER_TYPE_EMPTY = -1;
    public static final int FILTER_TYPE_NONE = 0;
    public static final int FILTER_TYPE_COUNTRY = 1;
    public static final int FILTER_TYPE_STATE = 2;
    public static final int FILTER_TYPE_GMT = 3;
    
    private static final int VIEW_TAG_TIME_ZONE = R.id.time_zone;
    private static final int EMPTY_INDEX = -100;

	Context mContext;
	private LayoutInflater mInflater;
	 
	TimeZoneSearchResultListView mListView;
	
	private TimeZoneData mTimeZoneData;
	
	private ArrayFilter mFilter;
	
	class FilterTypeResult {
        int type;
        String constraint;
        public int time;

        public FilterTypeResult(int type, String constraint, int time) {
            this.type = type;
            this.constraint = constraint;
            this.time = time;
        }

        @Override
        public String toString() {
            return constraint;
        }
    }
	
	static class ViewHolder {
        TextView timeZone;
        TextView timeOffset;
        TextView location;

        static void setupViewHolder(View v) {
            ViewHolder vh = new ViewHolder();
            vh.timeZone = (TextView) v.findViewById(R.id.time_zone);
            vh.timeOffset = (TextView) v.findViewById(R.id.time_offset);
            vh.location = (TextView) v.findViewById(R.id.location);
            v.setTag(vh);
        }
    }
	
	private ArrayList<FilterTypeResult> mLiveResults = new ArrayList<FilterTypeResult>();
    private int mLiveResultsCount = 0;
    
    private int[] mFilteredTimeZoneIndices;
    private int mFilteredTimeZoneLength;
    
    private OnHomeTimeZoneSetListener mTimeZoneSetListener;
	
	/*
	public interface OnSetFilterListener {
        void onSetFilter(int filterType, String str, int time);
    }
	
	private OnSetFilterListener mListener;
	*/
	
    final int mDefaultItemCount;
	public TimeZoneSearchResultAdapter(Context context, TimeZoneSearchResultListView listView, /*String currentSelectedTimeZoneId, */TimeZoneData timeZoneData, 
			TimeZoneOverrideSubView.OnHomeTimeZoneSetListener l, 
			int listViewHeight, int itemHeight) {
		mContext = context;
		mListView = listView;
		mTimeZoneData = timeZoneData;
		mTimeZoneSetListener = l;
		mFilteredTimeZoneIndices = new int[mTimeZoneData.size()];
		
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mDefaultItemCount = (listViewHeight / itemHeight) + 1;		
		
		//refresh(currentSelectedTimeZoneId);		
	}
	
	public void refresh(String currentSelectedTimeZoneId) {
		mFilteredTimeZoneLength = 1;
		
		TimeZoneInfo currentSelectedTimeZoneInfo = mTimeZoneData.mTimeZonesById.get(currentSelectedTimeZoneId);
		
		ArrayList<Integer> tzIds = mTimeZoneData.mTimeZonesByCountry.get(currentSelectedTimeZoneInfo.mCountry);
        if (tzIds != null) {
            for (Integer tzi : tzIds) {            	
            	TimeZoneInfo tzinfo = mTimeZoneData.get(tzi);
            	if (currentSelectedTimeZoneInfo.mTzId.equals(tzinfo.mTzId)){
            		mFilteredTimeZoneIndices[0] = tzi;
            	}                
            }
        }
		
		if (mFilteredTimeZoneLength < mDefaultItemCount) {
        	for (int i=mFilteredTimeZoneLength; i<mDefaultItemCount; i++)
        		mFilteredTimeZoneIndices[i] = EMPTY_INDEX;
        } 		
	}
	
	@Override
	public int getCount() {
		if (INFO) Log.i(TAG, "getCount:mFilteredTimeZoneLength=" + String.valueOf(mFilteredTimeZoneLength));
		int count = mDefaultItemCount > mFilteredTimeZoneLength? mDefaultItemCount:mFilteredTimeZoneLength;	
		if (INFO) Log.i(TAG, "getCount:count=" + String.valueOf(count));
		//return mFilteredTimeZoneLength;
		return count;
	}

	@Override
	public Object getItem(int position) {
		if (INFO) Log.i(TAG, "getItem");
		
		if (position < 0 || position >= mFilteredTimeZoneLength) {
            return null;
        }

        return mTimeZoneData.get(mFilteredTimeZoneIndices[position]);
	}

	
	@Override
    public boolean areAllItemsEnabled() {
        return false;
    }
	
	
    @Override
    public boolean isEnabled(int position) {
        return mFilteredTimeZoneIndices[position] >= 0;
    }

    @Override
    public long getItemId(int position) {
        return mFilteredTimeZoneIndices[position];
    }
        
    
    public void makeItemView(View v) {
    	v = mInflater.inflate(R.layout.timezone_search_result_item, null);
        ViewHolder.setupViewHolder(v);
    }
    
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		
		if (v == null) {        	
            v = mInflater.inflate(R.layout.timezone_search_result_item, null);
            ViewHolder.setupViewHolder(v);            
    		//makeItemView(v);
        }

		ViewHolder vh = (ViewHolder) v.getTag();
		
        if (mFilteredTimeZoneIndices[position] == EMPTY_INDEX) {        	
        	
        	vh.timeZone.setText("");
            vh.timeOffset.setText("");
            vh.location.setText("");
            
            vh.timeZone.setVisibility(View.INVISIBLE);
            vh.timeOffset.setVisibility(View.INVISIBLE);
            vh.location.setVisibility(View.INVISIBLE);            
        }
        else {
        	vh.timeZone.setVisibility(View.VISIBLE);
            vh.timeOffset.setVisibility(View.VISIBLE);
            vh.location.setVisibility(View.VISIBLE);       
            
            TimeZoneInfo tzi = mTimeZoneData.get(mFilteredTimeZoneIndices[position]);
            v.setTag(VIEW_TAG_TIME_ZONE, tzi);

            vh.timeZone.setText(tzi.mDisplayName);
            vh.timeOffset.setText(tzi.getGmtDisplayName(mContext));
            String location = tzi.mCountry;
            if (location == null) {
                vh.location.setVisibility(View.INVISIBLE);
            } else {
                vh.location.setText(location);
                vh.location.setVisibility(View.VISIBLE);
            }
        }      

        return v;
	}
	
	
	@Override
    public boolean hasStableIds() {
        return true;
    }
	
	
	@Override
	public Filter getFilter() {
		if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
	}
	
	private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            if (INFO) Log.i(TAG, "performFiltering >>>> [" + prefix + "]");            

            FilterResults results = new FilterResults();
            String prefixString = null;
            if (prefix != null) {
                prefixString = prefix.toString().trim().toLowerCase();
            }

            if (TextUtils.isEmpty(prefixString)) {
                results.values = null;
                results.count = 0;
                // publishResults���� filterType�� FILTER_TYPE_NONE�� ������ ���̴�
                return results;
            }

            // TODO Perf - we can loop through the filtered list if the new
            // search string starts with the old search string
            ArrayList<FilterTypeResult> filtered = new ArrayList<FilterTypeResult>();

            // ////////////////////////////////////////
            // Search by local time and GMT offset
            // :�������� ����
            // ////////////////////////////////////////
            /*
            boolean gmtOnly = false;
            int startParsePosition = 0;
            if (prefixString.charAt(0) == '+' || prefixString.charAt(0) == '-') {
                gmtOnly = true;
            }

            if (prefixString.startsWith("gmt")) {
                startParsePosition = 3;
                gmtOnly = true;
            }
            
            int num = parseNum(prefixString, startParsePosition);
            if (num != Integer.MIN_VALUE) {
                boolean positiveOnly = prefixString.length() > startParsePosition
                        && prefixString.charAt(startParsePosition) == '+';
                handleSearchByGmt(filtered, num, positiveOnly);
            }
			*/
            
            // ////////////////////////////////////////
            // Search by country
            // ////////////////////////////////////////
            ArrayList<String> countries = new ArrayList<String>();
            for (String country : mTimeZoneData.mTimeZonesByCountry.keySet()) {
                // TODO Perf - cache toLowerCase()?
                if (!TextUtils.isEmpty(country)) {
                    final String lowerCaseCountry = country.toLowerCase();
                    boolean isMatch = false;
                    if (lowerCaseCountry.startsWith(prefixString)
                            || (lowerCaseCountry.charAt(0) == prefixString.charAt(0) &&
                            isStartingInitialsFor(prefixString, lowerCaseCountry))) {
                        isMatch = true;
                    } else if (lowerCaseCountry.contains(" ")){
                        // We should also search other words in the country name, so that
                        // searches like "Korea" yield "South Korea".
                        for (String word : lowerCaseCountry.split(" ")) {
                            if (word.startsWith(prefixString)) {
                                isMatch = true;
                                break;
                            }
                        }
                    }
                    
                    if (isMatch) {
                        countries.add(country);
                    }
                }
            }
            
            if (countries.size() > 0) {
                // Sort countries alphabetically.
                Collections.sort(countries);
                for (String country : countries) {
                    filtered.add(new FilterTypeResult(FILTER_TYPE_COUNTRY, country, 0));
                }
            }

            // ////////////////////////////////////////
            // TODO Search by state
            // ////////////////////////////////////////
            if (INFO) Log.i(TAG, "performFiltering <<<< " + filtered.size() + "[" + prefix + "]");
            
            results.values = filtered;
            // ���� filtered.size()�� 0[��ġ�ϴ� ���� ������ �ǹ���]�̶��
            // publishResults���� filterType�� FILTER_TYPE_EMPTY�� ������ ���̴� 
            results.count = filtered.size();
            return results;
        }

        /**
         * Returns true if the prefixString is an initial for string. Note that
         * this method will return true even if prefixString does not cover all
         * the words. Words are separated by non-letters which includes spaces
         * and symbols).
         *
         * For example:
         * isStartingInitialsFor("UA", "United Arab Emirates") would return true
         * isStartingInitialsFor("US", "U.S. Virgin Island") would return true
         *
         * @param prefixString
         * @param string
         * @return
         */
        private boolean isStartingInitialsFor(String prefixString, String string) {
            final int initialLen = prefixString.length();
            final int strLen = string.length();

            int initialIdx = 0;
            boolean wasWordBreak = true;
            for (int i = 0; i < strLen; i++) {
                if (!Character.isLetter(string.charAt(i))) {
                    wasWordBreak = true;
                    continue;
                }

                if (wasWordBreak) {
                    if (prefixString.charAt(initialIdx++) != string.charAt(i)) {
                        return false;
                    }
                    if (initialIdx == initialLen) {
                        return true;
                    }
                    wasWordBreak = false;
                }
            }

            // Special case for "USA". Note that both strings have been turned to lowercase already.
            if (prefixString.equals("usa") && string.equals("united states")) {
                return true;
            }
            return false;
        }

        /*
        private void handleSearchByGmt(ArrayList<FilterTypeResult> filtered, int num,
                boolean positiveOnly) {

            FilterTypeResult r;
            if (num >= 0) {
                if (num == 1) {
                    for (int i = 19; i >= 10; i--) {
                        if (mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                            r = new FilterTypeResult(FILTER_TYPE_GMT, "GMT+" + i, i);
                            filtered.add(r);
                        }
                    }
                }

                if (mTimeZoneData.hasTimeZonesInHrOffset(num)) {
                    r = new FilterTypeResult(FILTER_TYPE_GMT, "GMT+" + num, num);
                    filtered.add(r);
                }
                num *= -1;
            }

            if (!positiveOnly && num != 0) {
                if (mTimeZoneData.hasTimeZonesInHrOffset(num)) {
                    r = new FilterTypeResult(FILTER_TYPE_GMT, "GMT" + num, num);
                    filtered.add(r);
                }

                if (num == -1) {
                    for (int i = -10; i >= -19; i--) {
                        if (mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                            r = new FilterTypeResult(FILTER_TYPE_GMT, "GMT" + i, i);
                            filtered.add(r);
                        }
                    }
                }
            }
        }
		*/

        /*
        public int parseNum(String str, int startIndex) {
            int idx = startIndex;
            int num = Integer.MIN_VALUE;
            int negativeMultiplier = 1;

            // First char - check for + and -
            char ch = str.charAt(idx++);
            switch (ch) {
                case '-':
                    negativeMultiplier = -1;
                    // fall through
                case '+':
                    if (idx >= str.length()) {
                        // No more digits
                        return Integer.MIN_VALUE;
                    }

                    ch = str.charAt(idx++);
                    break;
            }

            if (!Character.isDigit(ch)) {
                // No digit
                return Integer.MIN_VALUE;
            }

            // Got first digit
            num = Character.digit(ch, 10);

            // Check next char
            if (idx < str.length()) {
                ch = str.charAt(idx++);
                if (Character.isDigit(ch)) {
                    // Got second digit
                    num = 10 * num + Character.digit(ch, 10);
                } else {
                    return Integer.MIN_VALUE;
                }
            }

            if (idx != str.length()) {
                // Invalid
                return Integer.MIN_VALUE;
            }

            if (INFO) Log.i(TAG, "Parsing " + str + " -> " + negativeMultiplier * num);
            
            return negativeMultiplier * num;
        }
		*/
        
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults
                results) {
        	
        	mFilteredTimeZoneLength = 0;
        	
        	// '||'�ӿ� �����ض�!!!
            if (results.values == null || results.count == 0) {            	
                
                int filterType;
                if (TextUtils.isEmpty(constraint)) {
                    filterType = FILTER_TYPE_NONE;
                } else {
                    filterType = FILTER_TYPE_EMPTY;
                }
                
                //mFilteredTimeZoneLength = 0;
                setFilter(filterType, null, 0);                
                
                if (INFO) Log.i(TAG, "publishResults: " + results.count + " of null [" + constraint);
                
            } else {
                mLiveResults = (ArrayList<FilterTypeResult>) results.values;
                if (INFO) Log.i(TAG, "publishResults: " + results.count + " of " + mLiveResults.size()
                            + " [" + constraint);                

                //mFilteredTimeZoneLength = 0;
                for (int i=0; i<results.count; i++) {
                	FilterTypeResult filter = mLiveResults.get(i);
                	setFilter(filter.type, filter.constraint, filter.time);
                }                
            } 
            
            if (mFilteredTimeZoneLength < mDefaultItemCount) {
            	for (int i=mFilteredTimeZoneLength; i<mDefaultItemCount; i++)
            		mFilteredTimeZoneIndices[i] = EMPTY_INDEX;
            }            
            
            notifyDataSetChanged();      
            
        }
        
        public void setFilter(int filterType, String str, int time) {        	

            switch (filterType) {
	            
            	case FILTER_TYPE_NONE:
            		// timezone search edit text view�� �ƹ��� �ؽ�Ʈ�� ���ٴ� ���� �ǹ�
                	// :�̴� user�� cross ��ư�� ������ ���
                	// :backspace Ű�� ������ edit text view�� non text ���°� �Ǿ� ��� �߻��� �� �ִ�
            		// ->�̹� timezone search edit text view�� non text �����ε�  
            		//   user�� cross button�� ������ ��쿡��?
            		//   :�̴� ���������� cross button���� �ɷ���� �Ѵ�
            		if (INFO) Log.i(TAG, "FILTER_TYPE_NONE");
                case FILTER_TYPE_EMPTY:
                	// ��ġ�ϴ� ���� ���ٴ� ���� �ǹ��Ѵ�
                	if (INFO) Log.i(TAG, "FILTER_TYPE_EMPTY");
                    //mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = EMPTY_INDEX;
                    break;
                
                case FILTER_TYPE_COUNTRY:
                    ArrayList<Integer> tzIds = mTimeZoneData.mTimeZonesByCountry.get(str);
                    if (tzIds != null) {
                        for (Integer tzi : tzIds) {
                            mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = tzi;
                        }
                    }
                    break;
                case FILTER_TYPE_STATE:
                    // ���� ���Ͼȿ� ������ ���̴�
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            
        }
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mTimeZoneSetListener != null) {
            TimeZoneInfo tzi = (TimeZoneInfo) view.getTag(VIEW_TAG_TIME_ZONE);
            if (tzi != null) {
              mTimeZoneSetListener.onHomeTimeZoneSet(tzi.mTzId);
              //saveRecentTimezone(tzi.mTzId);
            }
        }
		
	}

	
}
