package com.intheeast.event;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.android.ex.chips.AccountSpecifier;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.ChipsUtil;
import com.android.ex.chips.RecipientEditTextView;
import com.intheeast.calendarcommon.Rfc822InputFilter;
import com.intheeast.acalendar.EmailAddressAdapter;
import com.intheeast.acalendar.R;
import com.intheeast.acalendar.RecipientAdapter;
import com.intheeast.acalendar.CalendarEventModel.Attendee;
import com.intheeast.etc.Rfc822Validator;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;

public class EditEventAttendeeSettingSubView extends LinearLayout {

	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;
	//EditEventActionBarFragment mActionBar;
	EditEvent mEditEvent;
	
	MultiAutoCompleteTextView mAttendeesList;
	private AccountSpecifier mAddressAdapter;
	
	public Rfc822Validator mEmailValidator;
	
	public EditEventAttendeeSettingSubView (Context context) {
		super(context);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventAttendeeSettingSubView (Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventAttendeeSettingSubView (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	} 
    
	public void initView(Activity activity, EditEventFragment fragment, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;
		//mActionBar = fragment.getActionBar();
		//mActivity = fragment.getActivity();
		
		mEditEvent = fragment.mEditEvent;
		
		mAttendeesList = (MultiAutoCompleteTextView) findViewById(R.id.attendees);
		mAttendeesList.setTag(mAttendeesList.getBackground());		
				
		mEmailValidator = new Rfc822Validator(null);
		
		initMultiAutoCompleteTextView((RecipientEditTextView) mAttendeesList);		
	}
	
		
	
	public LinkedHashMap<String, Attendee> mTempAttendeesList = new LinkedHashMap<String, Attendee>();;
	public int getAttendeePageEditText() {
		mEmailValidator.setRemoveInvalid(true);
	    mAttendeesList.performValidation();
	    mTempAttendeesList.clear();
	    addAttendeesX(mAttendeesList.getText().toString(), mEmailValidator);
	    int numbers = mTempAttendeesList.size();
	    //mEventAttendeeNumbersText.setText(String.valueOf(numbers));
	    //Log.i("tag", "getAttendeePageEditText : attendee number =" + String.valueOf(numbers));
	    mEmailValidator.setRemoveInvalid(false);
	    
	    return numbers;
	}
	
	public String getAttendees() {
		mEmailValidator.setRemoveInvalid(true);
        mFragment.mAttendeePageView.mAttendeesList.performValidation();
        
        String attendess = mAttendeesList.getText().toString();
        mEmailValidator.setRemoveInvalid(false);
        
        return attendess;
	}
	
	public void addAttendee(Attendee attendee) {
		mTempAttendeesList.put(attendee.mEmail, attendee);
		Log.i("tag", "addAttendee : email=" + attendee.mEmail);
    }
	
	public void addAttendeesX(String attendees, Rfc822Validator validator) {
        final LinkedHashSet<Rfc822Token> addresses = EditEventHelper.getAddressesFromList(
                																attendees, validator);       
        for (final Rfc822Token address : addresses) {
            final Attendee attendee = new Attendee(address.getName(), address.getAddress());
            if (TextUtils.isEmpty(attendee.mName)) {
                attendee.mName = attendee.mEmail;
            }
            addAttendee(attendee);
        }        
    }	
	
	
	
	/**
     * From com.google.android.gm.ComposeActivity Implements special address
     * cleanup rules: The first space key entry following an "@" symbol that is
     * followed by any combination of letters and symbols, including one+ dots
     * and zero commas, should insert an extra comma (followed by the space).
     */
    private static InputFilter[] sRecipientFilters = new InputFilter[] { new Rfc822InputFilter() };
    
    // From com.google.android.gm.ComposeActivity
    private MultiAutoCompleteTextView initMultiAutoCompleteTextView(RecipientEditTextView list) {
        if (ChipsUtil.supportsChipsUi()) {
            mAddressAdapter = new RecipientAdapter(mActivity);
            list.setAdapter((BaseRecipientAdapter) mAddressAdapter);
            list.setOnFocusListShrinkRecipients(false);
        } else {
            mAddressAdapter = new EmailAddressAdapter(mActivity);
            list.setAdapter((EmailAddressAdapter)mAddressAdapter);
        }
        
        list.setTokenizer(new Rfc822Tokenizer());
        list.setValidator(mEmailValidator);

        // NOTE: assumes no other filters are set
        list.setFilters(sRecipientFilters);

        return list;
    }
    
    
    public void updateAttendees(HashMap<String, Attendee> attendeesList) {
        if (attendeesList == null || attendeesList.isEmpty()) {
            return;
        }
        mAttendeesList.setText(null);
        for (Attendee attendee : attendeesList.values()) {
            // TODO: Please remove separator when Calendar uses the chips MR2 project

            // Adding a comma separator between email addresses to prevent a chips MR1.1 bug
            // in which email addresses are concatenated together with no separator.
            mAttendeesList.append(attendee.mEmail + ", ");
        }
    }   
}
