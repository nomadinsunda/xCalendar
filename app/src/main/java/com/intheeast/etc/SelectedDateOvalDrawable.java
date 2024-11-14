package com.intheeast.etc;

import com.intheeast.acalendar.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;

public class SelectedDateOvalDrawable extends ShapeDrawable {
	private final Typeface mBold = Typeface.DEFAULT_BOLD; 
	private String mDayOfMonth = "1";
	private float mTextX;
	private float mTextY;
	// ������ �ؽ�Ʈ���Ը� ����Ǵ� Paint�̴�
    private Paint mTextPaint;

    private float FONT_SIZE;
    public SelectedDateOvalDrawable(Context context, Shape s) {
    	super(s);
    	
    	FONT_SIZE = (int) context.getResources().getDimension(R.dimen.monthday_text_size); //20sp
    	mTextPaint = new Paint(); 
    	mTextPaint.setAntiAlias(true);     		
    	mTextPaint.setTypeface(mBold);   
    	mTextPaint.setTextSize(FONT_SIZE);
    	mTextPaint.setColor(Color.WHITE);
    	mTextPaint.setTextAlign(Align.CENTER);
    }
    
    public SelectedDateOvalDrawable(Context context, Shape s, float textSize) {
    	super(s);
    	
    	FONT_SIZE = textSize;
    	mTextPaint = new Paint(); 
    	mTextPaint.setAntiAlias(true);     		
    	mTextPaint.setTypeface(mBold);   
    	mTextPaint.setTextSize(FONT_SIZE);
    	mTextPaint.setColor(Color.WHITE);
    	mTextPaint.setTextAlign(Align.CENTER);        
    }

    @Override
    public void draw(Canvas canvas) {
    	super.draw(canvas); // �θ� Oval[��׶���]�� �׸���    	
    	
    	canvas.drawText(mDayOfMonth, mTextX, mTextY, mTextPaint);        	
    }        
    
    
    boolean mChangeTextColorToBlack = false;  
        
    public void setTextColorAlpha(int alpha, int color) {    	
    	if (!mChangeTextColorToBlack) {
    		mChangeTextColorToBlack = true;
    		mTextPaint.setTypeface(Typeface.DEFAULT);
    		mTextPaint.setColor(color);  //Color.BLACK
    	}
    	
    	mTextPaint.setAlpha(alpha);
    }
    
    public void setTextSize(float textSize) {   
    	FONT_SIZE = textSize;
    	mTextPaint.setTextSize(textSize);
    }
    
    public void setTextAlpha(int alpha) {    	
    	mTextPaint.setAlpha(alpha);
    }
    
    public void reSetTextColor() {
    	mChangeTextColorToBlack = false;  
    	mTextPaint.setTypeface(mBold);   
    	mTextPaint.setColor(Color.WHITE);
    }
    
    /*
    public int getAlphaValue() {
    	// �ش� ���İ��� �ؽ�Ʈ���� ����ȴ�
        return mPaint.getAlpha();
    }
    */
    
    @Override
    public void setColorFilter(ColorFilter cf) {
        // Ignore
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    public void setDayOfMonth(int day) {
        mDayOfMonth = Integer.toString(day);        
    }
    
    public void setDrawTextPosition(float x, float y) {
    	mTextX = x;
    	mTextY = y;
    }
}
