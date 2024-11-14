package com.intheeast.acalendar;

//import com.android.ex.chips.AccountSpecifier;


import com.intheeast.contacts.BaseEmailAddressAdapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EmailAddressAdapter extends BaseEmailAddressAdapter {

    private final LayoutInflater mInflater;

    public EmailAddressAdapter(@NonNull Context context) {
        super(context);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    protected View inflateItemView(ViewGroup parent) {
        return mInflater.inflate(R.layout.email_autocomplete_item, parent, false);
    }

    @Override
    protected View inflateItemViewLoading(ViewGroup parent) {
        return mInflater.inflate(R.layout.email_autocomplete_item_loading, parent, false);
    }

    @Override
    protected void bindView(View view, String directoryType, String directoryName,
                            String displayName, String emailAddress) {
        TextView text1 = view.findViewById(R.id.text1);
        TextView text2 = view.findViewById(R.id.text2);
        text1.setText(displayName);
        text2.setText(emailAddress);
    }

    @Override
    protected void bindViewLoading(View view, String directoryType, String directoryName) {
        TextView text1 = view.findViewById(R.id.text1);
        String displayText = getContext().getString(
                R.string.directory_searching_fmt,
                TextUtils.isEmpty(directoryName) ? directoryType : directoryName
        );
        text1.setText(displayText);
    }
}

/**
* An adaptation of {@link BaseEmailAddressAdapter} for the Email app. The main
* purpose of the class is to bind the generic implementation to the resources
* defined locally: strings and layouts.
*/
//public class EmailAddressAdapter extends BaseEmailAddressAdapter implements AccountSpecifier {
//
//   private LayoutInflater mInflater;
//
//   public EmailAddressAdapter(Context context) {
//       super(context);
//       mInflater = LayoutInflater.from(context);
//   }
//
//   @Override
//   protected View inflateItemView(ViewGroup parent) {
//       return mInflater.inflate(R.layout.email_autocomplete_item, parent, false);
//
//   }
//
//   @Override
//   protected View inflateItemViewLoading(ViewGroup parent) {
//       return mInflater.inflate(R.layout.email_autocomplete_item_loading, parent, false);
//
//   }
//
//   @Override
//   protected void bindView(View view, String directoryType, String directoryName,
//           String displayName, String emailAddress) {
//     TextView text1 = (TextView)view.findViewById(R.id.text1);
//     TextView text2 = (TextView)view.findViewById(R.id.text2);
//     text1.setText(displayName);
//     text2.setText(emailAddress);
//   }
//
//   @Override
//   protected void bindViewLoading(View view, String directoryType, String directoryName) {
//
//       TextView text1 = (TextView)view.findViewById(R.id.text1);
//       String text = getContext().getString(R.string.directory_searching_fmt,
//               TextUtils.isEmpty(directoryName) ? directoryType : directoryName);
//       text1.setText(text);
//
//   }
//}