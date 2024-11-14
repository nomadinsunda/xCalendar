package com.intheeast.acalendar;

import com.intheeast.event.EditEventHelper.AttendeeItem;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;

public class AttendeesInfoSubPageView extends LinearLayout {
	AttendeesInfoSwitchPageView mViewSwitcher;
	QuickContactBadge mAttendeeBadgeView;
	
	public AttendeesInfoSubPageView(Context context, AttributeSet attrs) {
		super(context, attrs);	
		
	}
	
	public void getChildViews(AttendeesInfoSwitchPageView viewSwitcher) {
		mViewSwitcher = viewSwitcher;
		mAttendeeBadgeView = (QuickContactBadge)findViewById(R.id.attendee_badge);
	}
	
	public void setInfo(AttendeeItem item) {
		mAttendeeBadgeView.setImageDrawable(item.mBadge);
		
		// If we know the lookup-uri of the contact, it is a good idea to set this here. This
        // allows QuickContact to be started without an extra database lookup. If we don't know
        // the lookup uri (yet), we can set Email and QuickContact will lookup once tapped.
        if (item.mContactLookupUri != null) {
        	mAttendeeBadgeView.assignContactUri(item.mContactLookupUri);
        } else {
        	mAttendeeBadgeView.assignContactFromEmail(item.mAttendee.mEmail, true);
        }
        mAttendeeBadgeView.setMaxHeight(60);
	}

}
