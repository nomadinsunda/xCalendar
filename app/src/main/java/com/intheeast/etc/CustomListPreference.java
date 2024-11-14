package com.intheeast.etc;


import com.intheeast.acalendar.Utils;

import android.content.Context;
import android.content.SharedPreferences;


public class CustomListPreference {
	Context mContext;	
	private SharedPreferences mPreferences;
	
	private String mKey;
	private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private String mValue;    
    
    public CustomListPreference(Context context, SharedPreferences preference, String key) {
    	mContext = context;
    	mPreferences = preference;
    	mKey = key;	  	
    }
    
    public CustomListPreference(Context context, SharedPreferences preference, int entriesId, int entryValuesId) {
    	mContext = context;
    	mPreferences = preference;
    	
    	mEntries = mContext.getResources().getTextArray(entriesId);    	
    	mEntryValues = mContext.getResources().getTextArray(entryValuesId);    	
    }
    
    /**
     * Sets the human-readable entries to be shown in the list. This will be
     * shown in subsequent dialogs.
     * <p>
     * Each entry must have a corresponding index in
     * {@link #setEntryValues(CharSequence[])}.
     * 
     * @param entries The entries.
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }
    
    /**
     * @see #setEntries(CharSequence[])
     * @param entriesResId The entries array as a resource.
     */
    public void setEntries(int entriesResId) {
        setEntries(mContext.getResources().getTextArray(entriesResId));
    }
    
    /**
     * The list of entries to be shown in the list in subsequent dialogs.
     * 
     * @return The list as an array.
     */
    public CharSequence[] getEntries() {
        return mEntries;
    }
    
    /**
     * The array to find the value to save for a preference when an entry from
     * entries is selected. If a user clicks on the second item in entries, the
     * second item in this array will be saved to the preference.
     * 
     * @param entryValues The array to be used as values to save for the preference.
     */
    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValues = entryValues;
    }

    /**
     * @see #setEntryValues(CharSequence[])
     * @param entryValuesResId The entry values array as a resource.
     */
    public void setEntryValues(int entryValuesResId) {
        setEntryValues(mContext.getResources().getTextArray(entryValuesResId));
    }
    
    /**
     * Returns the array of values to be saved for the preference.
     * 
     * @return The array of values.
     */
    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    /**
     * Sets the value of the key. This should be one of the entries in
     * {@link #getEntryValues()}.
     * 
     * @param value The value to set for the key.
     */
    public void setValue(String value) {
        mValue = value;
        
        Utils.setSharedPreference(mContext, mKey, value);
        
        /*
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(mKey, value);
        editor.commit();
        */
    }    

    /**
     * Sets the value to the given index from the entry values.
     * 
     * @param index The index of the value to set.
     */
    public void setValueIndex(int index) {
        if (mEntryValues != null) {
            setValue(mEntryValues[index].toString());
        }
    }
    
    /**
     * Returns the value of the key. This should be one of the entries in
     * {@link #getEntryValues()}.
     * 
     * @return The value of the key.
     */
    public String getValue() {
        return mValue; 
    }
    
    /**
     * Returns the entry corresponding to the current value.
     * 
     * @return The entry corresponding to the current value, or null.
     */
    public CharSequence getEntry() {
        int index = getValueIndex();
        return index >= 0 && mEntries != null ? mEntries[index] : null;
    }
    
    /**
     * Returns the index of the given value (in the entry values array).
     * 
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public int getValueIndex() {
        return findIndexOfValue(mValue);
    }
    
   
}
