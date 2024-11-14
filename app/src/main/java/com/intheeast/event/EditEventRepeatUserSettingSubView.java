package com.intheeast.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;





import com.intheeast.acalendar.R;
import com.intheeast.etc.LockableScrollView;
import com.intheeast.etc.TextureInfo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EditEventRepeatUserSettingSubView extends LinearLayout {

	Activity mActivity;
	Context mContext;
	Resources mResources;
	
	EditEventFragment mFragment;		
	EditEventRepeatSettingSwitchPageView mParentView;
	
	LinearLayout mInnerLinearLayoutOfScrollView;
	RelativeLayout mFreqCaseLayout;
	RelativeLayout mFreqValueLayout;
	
	TextView mFreqCaseLabel;
	TextView mFreqCase;
	TextView mFreqValueLabel;
	TextView mFreqValue;
	
	EditEventSelectRepeatFreqCasePicker mEditEventSelectRepeatFreqCasePicker;
	
	public EditEventRepeatUserSettingSubView(Context context) {
		super(context);
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventRepeatUserSettingSubView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public EditEventRepeatUserSettingSubView(Context context,
			AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mResources = mContext.getResources();
	}
	
	public void initView(Activity activity, EditEventFragment fragment, EditEventRepeatSettingSwitchPageView parentView, int frameLayoutHeight) {
		mActivity = activity;
		mFragment = fragment;		
		mParentView = parentView;
		
		LockableScrollView scrollView = (LockableScrollView)findViewById(R.id.editevent_repeat_user_setting_page_scroll_view);
		mInnerLinearLayoutOfScrollView = (LinearLayout)scrollView .findViewById(R.id.editevent_repeat_user_setting_page_scrollview_inner_layout);
		
		mFreqCaseLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_user_setting_repeat_freq_case_layout);		
		mFreqCaseLabel = (TextView) mFreqCaseLayout.findViewById(R.id.user_setting_repeat_freq_case_label_text);
		mFreqCase = (TextView) mFreqCaseLayout.findViewById(R.id.user_setting_repeat_freq_case_text);
		//mFreqCaseLayout.setOnClickListener(mRepeatPageItemClickListener);
		
		mFreqValueLayout =(RelativeLayout)mInnerLinearLayoutOfScrollView.findViewById(R.id.event_user_setting_repeat_freq_value_layout);
		mFreqValueLabel = (TextView) mFreqCaseLayout.findViewById(R.id.user_setting_repeat_freq_value_lable_text);
		mFreqValue = (TextView) mFreqCaseLayout.findViewById(R.id.user_setting_repeat_freq_value_text);
		//mFreqValueLayout.setOnClickListener(mRepeatPageItemClickListener);
	}
	
	public void createFreqType () {
				
		Handler timeMsgHandler = null;
					
		makeXZ();
		
		EditEventSelectRepeatFreqCasePickerRenderParameters parameters = new EditEventSelectRepeatFreqCasePickerRenderParameters();
		parameters.mPpi = mPpi;
		parameters.mWidth = mTextureViewWidth;
		parameters.mHeight = mTextureViewHeight;
		parameters.mTextureInfoList = mTextureInfoList;
		
		mEditEventSelectRepeatFreqCasePicker = new EditEventSelectRepeatFreqCasePicker(
				"FreqCasePicker", 
				mContext, 
				mActivity, 			
				timeMsgHandler,				
				parameters);
			
		
	}
	
	
	HashMap<Integer,String> mCurrentFreqCaseMap;
	public void makeXZ() {
		createPickerDimens();
		
		makeTextPaint();
		
		mPickerTextFontsize = mPickerTextPaint.descent() - mPickerTextPaint.ascent();
		float timePickerTextYPos = ( mItemHeightOfTimePickerField/2 + mPickerTextFontsize/2 - mPickerTextPaint.descent() );	
		
		mCurrentFreqCaseMap = sFreqCaseMap;
		
		for (Iterator<Entry<Integer, String>> itr =
				mCurrentFreqCaseMap.entrySet().iterator(); itr.hasNext();) {
		 
    		Entry<Integer, String> entry = itr.next();    	
    		int key = entry.getKey();
    		String everyMonth = entry.getValue();
    		
    		TextureInfo obj = getTextureTextWidth(everyMonth);    		
    		
    		float timePickerTextXPos = obj.mTextureWidth / 2;    		
    		
    		obj.mBitmap = Bitmap.createBitmap((int)obj.mTextureWidth,
                    (int)mItemHeightOfTimePickerField,
                     Config.ARGB_8888);		

    		obj.mBitmap.eraseColor(Color.WHITE);		
    		Canvas canvas = new Canvas(obj.mBitmap);    		
			canvas.drawText(everyMonth, timePickerTextXPos, timePickerTextYPos, mPickerTextPaint);   
			
			BitmapDrawable ob = new BitmapDrawable(getResources(), obj.mBitmap);
			
			switch(key) {
			case DAILY:
				obj.mTag = DAILY;				
				break;
			case WEEKLY:
				obj.mTag = WEEKLY;				
				break;
			case MONTHLY:
				obj.mTag = MONTHLY;				
				break;
			case YEARLY:
				obj.mTag = YEARLY;				
				break;
			
			}
    	}			
	}
	
	/*
	 	public Object mTag;
		public int mPolygonColumnNumbers; o
		public int mPolygonRowNumbers;    o
		public float mTextureWidth;		  o
		public float mPolygonWidth;       o
		public Bitmap mBitmap;            0
	 */
	ArrayList<TextureInfo> mTextureInfoList = new ArrayList<TextureInfo>();
	public TextureInfo getTextureTextWidth(String TextureText) {
		
		float textWidth = mPickerTextPaint.measureText(TextureText);
		TextureInfo obj = new TextureInfo();
		obj.mTextureWidth = textWidth;
		
		// float과 double 둘 다 부동 소수형이다
		// 그러나 float는 4바이트, double는 8바이트 사이즈가 다르다
		float xxx = textWidth / mThirdSizeOfPickerTextSize; // for test
		obj.mPolygonRowNumbers = POLYGON_ROW_NUMBERS;
		obj.mPolygonColumnNumbers = (int) Math.floor(xxx);//obj.mPolygonNumbers = (int) Math.floor(textWidth / mHalfOfTimePickerTextSize);
		obj.mPolygonWidth = mThirdSizeOfPickerTextSize;
		float remainingWidth = textWidth % mThirdSizeOfPickerTextSize;
		
		//float checkTextWidth = 0; // for test!!!
		if (remainingWidth != 0) {
			obj.mPolygonColumnNumbers = obj.mPolygonColumnNumbers + 1;
			obj.mPolygonWidth = textWidth / obj.mPolygonColumnNumbers;
			float checkTextWidth = obj.mPolygonWidth * obj.mPolygonColumnNumbers;
			if (checkTextWidth > obj.mTextureWidth) {
				obj.mTextureWidth = checkTextWidth; // 이런 가능성은 상당히 낮지만 보정해줘야 한다
			}
		}	
		
		mTextureInfoList.add(obj);
		
		return obj;
		
	}
	
	public static final float TEXTUREVIEW_HEIGHT_DP = 200;
	public static final double PICKER_FIELD_CIRCLE_RADIUS = 0.5; // viewport의 height에 대한 ratio임!!!
    public static final double PICKER_ITEM_AVAILABLE_MAX_DEGREE = 180;
    public static final double PICKER_ITEM_AVAILABLE_MIN_DEGREE = 0;
    public static final double FIELD_ITEM_DEGREE = 20;
    public static final int PICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT = (int)(PICKER_ITEM_AVAILABLE_MAX_DEGREE / FIELD_ITEM_DEGREE);
    public static final float PICKER_ITEM_TEXT_SIZE_RATIO = 0.8f;
    public static final int POLYGON_ROW_NUMBERS = 3;
    public static final int PICKER_NONSELECTED_COLOR = Color.argb(0xFF, 0xC0, 0xC0, 0xC0);  //silver color
    
    float mPpi;
	float mDensity;
	int mTextureViewWidth;
	int mTextureViewHeight;
	float mPickerDateFiedlCircleRadiusPixels;
	float mPickerDateFieldCircumferencePixels;
	float mPickerHeightPixels;
	float mItemHeightOfTimePickerField;
	public void createPickerDimens() {		
		WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		Display current_display = wm.getDefaultDisplay();
		DisplayMetrics display_Metrics = new DisplayMetrics();
		current_display.getMetrics(display_Metrics);		
		
		mPpi = display_Metrics.densityDpi;
		mDensity = display_Metrics.density;
		
		mTextureViewWidth = display_Metrics.widthPixels;
		mTextureViewHeight = (int)(TEXTUREVIEW_HEIGHT_DP * mDensity); 
		
		mPickerDateFiedlCircleRadiusPixels = (float)(mTextureViewHeight * PICKER_FIELD_CIRCLE_RADIUS);
		mPickerDateFieldCircumferencePixels = (float)( 2 * Math.PI * mPickerDateFiedlCircleRadiusPixels );
		// 원의 둘레의 반을 height로 설정한다
		mPickerHeightPixels = (float)(mPickerDateFieldCircumferencePixels / 2);
		mItemHeightOfTimePickerField = mPickerHeightPixels / PICKER_ITEM_HEIGHT_SIZE_DIVIDER_CONSTANT;
		
	}
	
	Paint mPickerTextPaint;
	
	float mPickerTextSize;
	float mHalfOfPickerTextSize;
	float mThirdSizeOfPickerTextSize;
	float mPickerTextFontsize;
	public void makeTextPaint() {
		mPickerTextPaint = new Paint();
		mPickerTextPaint.setAntiAlias(true);
		mPickerTextPaint.setColor(PICKER_NONSELECTED_COLOR);		
		mPickerTextPaint.setTextAlign(Align.CENTER);
		float specified_fontsize= mPickerTextPaint.getTextSize();
		float desc = mPickerTextPaint.descent();
		float asc = mPickerTextPaint.ascent();
        float measured_fontsize= desc - asc;
        float font_factor= specified_fontsize / measured_fontsize;
        
        mPickerTextSize = mItemHeightOfTimePickerField * PICKER_ITEM_TEXT_SIZE_RATIO * font_factor;
        mHalfOfPickerTextSize = mPickerTextSize / 2;
        mThirdSizeOfPickerTextSize = mPickerTextSize / POLYGON_ROW_NUMBERS;
        mPickerTextPaint.setTextSize(mPickerTextSize);
	}
	
	public static class EditEventSelectRepeatFreqCasePickerRenderParameters {
				
		int mWidth;
		int mHeight;
		
		float mPickerCircleDiameterPixels;
		float mPpi;
		
		LockableScrollView MainPageScrollView;
		
		ArrayList<TextureInfo> mTextureInfoList;
		
		
		public void setPickerCircleDiameterPixels(float width) {
			mPickerCircleDiameterPixels = width;
		}
		
		public void setPpi(float ppi) {
			this.mPpi = ppi;
		}
		
		public float getPpi() {
			return this.mPpi;
		}
		
		public void setMainPageScrollView(LockableScrollView scrollView) {
			this.MainPageScrollView = scrollView;
		}
		
		
		
	}
	
	
	
    public static final int DAILY = 1;
    public static final int WEEKLY = 2;
    public static final int MONTHLY = 3;
    public static final int YEARLY = 4;
    
	private static final HashMap<Integer,String> sFreqCaseMap = new HashMap<Integer,String>();
    static {
        sFreqCaseMap.put(DAILY, "Daily");
        sFreqCaseMap.put(WEEKLY, "Weekly");
        sFreqCaseMap.put(MONTHLY, "Monthly");
        sFreqCaseMap.put(YEARLY, "Yearly");
    }
    
    private static final HashMap<Integer,String> sFreqCase_ar_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_ar_Map.put(DAILY, "يومي");
        sFreqCase_ar_Map.put(WEEKLY, "أسبوعي");
        sFreqCase_ar_Map.put(MONTHLY, "شهريا");
        sFreqCase_ar_Map.put(YEARLY, "سنوي");
    }    
    
    private static final HashMap<Integer,String> sFreqCase_rDE_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_rDE_Map.put(DAILY, "Täglich");
        sFreqCase_rDE_Map.put(WEEKLY, "Wöchentlich");
        sFreqCase_rDE_Map.put(MONTHLY, "Monatlich");
        sFreqCase_rDE_Map.put(YEARLY, "Jährlich");
    }
        
    private static final HashMap<Integer,String> sFreqCase_rES_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_rES_Map.put(DAILY, "Todos los días");
        sFreqCase_rES_Map.put(WEEKLY, "Todas las semanas");
        sFreqCase_rES_Map.put(MONTHLY, "Todos los meses");
        sFreqCase_rES_Map.put(YEARLY, "Todos los años");
    }
        
    private static final HashMap<Integer,String> sFreqCase_rFR_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_rFR_Map.put(DAILY, "Quotidienne");
        sFreqCase_rFR_Map.put(WEEKLY, "Hebdomadaire");
        sFreqCase_rFR_Map.put(MONTHLY, "Mensuelle");
        sFreqCase_rFR_Map.put(YEARLY, "Annuelle");
    }
        
    private static final HashMap<Integer,String> sFreqCase_rIT_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_rIT_Map.put(DAILY, "Ogni giorno");
        sFreqCase_rIT_Map.put(WEEKLY, "Settimanale");
        sFreqCase_rIT_Map.put(MONTHLY, "Mensile");
        sFreqCase_rIT_Map.put(YEARLY, "Annuale");
    }
        
    private static final HashMap<Integer,String> sFreqCase_ja_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_ja_Map.put(DAILY, "毎日");
        sFreqCase_ja_Map.put(WEEKLY, "毎週");
        sFreqCase_ja_Map.put(MONTHLY, "毎月");
        sFreqCase_ja_Map.put(YEARLY, "毎年");
    }
    
    private static final HashMap<Integer,String> sFreqCase_ko_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_ko_Map.put(DAILY, "매일");
        sFreqCase_ko_Map.put(WEEKLY, "매주");
        sFreqCase_ko_Map.put(MONTHLY, "매월");
        sFreqCase_ko_Map.put(YEARLY, "매년");
    }
    
    private static final HashMap<Integer,String> sFreqCase_rRU_Map = new HashMap<Integer,String>();
    static {
        sFreqCase_rRU_Map.put(DAILY, "По дням");
        sFreqCase_rRU_Map.put(WEEKLY, "По неделям");
        sFreqCase_rRU_Map.put(MONTHLY, "По месяцам");
        sFreqCase_rRU_Map.put(YEARLY, "По годам");
    }    

}
