package com.intheeast.acalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class PreviousIconDrawable extends Drawable {	
	
	public PreviousIconDrawable(Context c) {
		
	}
	
	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAlpha(int alpha) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getOpacity() {
		// TODO Auto-generated method stub
		return PixelFormat.UNKNOWN;
	}
	

	public void changeIcon() {
		invalidateSelf();
	}
}
