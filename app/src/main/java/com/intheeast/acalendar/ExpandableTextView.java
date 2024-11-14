/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.intheeast.acalendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExpandableTextView extends LinearLayout {

    TextView mTv;    

    private boolean mRelayout = false;        

    public ExpandableTextView(Context context) {
        super(context);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void init() {
        
    }    

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If no change, measure and return
        if (!mRelayout || getVisibility() == View.GONE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        mRelayout = false;

        // Setup with optimistic case
        // i.e. Everything fits. No button needed        
        mTv.setMaxLines(Integer.MAX_VALUE);

        // Measure
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);        

        // Re-measure with new setup
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void findViews() {
        mTv = (TextView) findViewById(R.id.expandable_text);
       
    }

    public void setText(String text) {
        mRelayout = true;
        if (mTv == null) {
            findViews();
        }
        String trimmedText = text.trim();
        mTv.setText(trimmedText);
        this.setVisibility(trimmedText.length() == 0 ? View.GONE : View.VISIBLE);
    }

    public CharSequence getText() {
        if (mTv == null) {
            return "";
        }
        return mTv.getText();
    }
}
