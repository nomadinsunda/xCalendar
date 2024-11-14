package com.intheeast.acalendar;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;

public class CustomEditText extends android.support.v7.widget.AppCompatEditText {
	
	private static final String TAG = "CustomEditText";
	private static final boolean INFO = true;
	
	private Drawable dLeft,dRight;
	private Rect lBounds,rBounds;
	private static Button mCrossButton;
	 
	public CustomEditText(Context context, AttributeSet attrs, int defStyle) {
		
	    super(context, attrs, defStyle);
	}
	
	public CustomEditText(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}
	
	public CustomEditText(Context context) {
	    super(context);
	}
	 
	@Override
	public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
		
		if(left !=null) {
	    	dLeft = left;
	    }
		
		if(right !=null){
	      dRight = right;
	    } 
	 
	    super.setCompoundDrawables(left, top, right, bottom);
	}
	 
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (INFO) Log.i(TAG, "onKeyUp");
		
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			mCrossButton.requestFocus();
			mCrossButton.performClick(); 
		}
		
		return super.onKeyUp(keyCode, event);
	}
	 
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
	  	final int x = (int)event.getX();
	  	final int y = (int)event.getY();
	 
	  	if(event.getAction() == MotionEvent.ACTION_UP && dLeft!=null) {
	  		lBounds = dLeft.getBounds();
	 
	  		int n1 = this.getLeft();
	  		int n2 = this.getLeft()+lBounds.width();
	  		int n3 = this.getPaddingTop();
	  		int n4 = this.getHeight()-this.getPaddingBottom();
	  		// leva strana
	  		if( x>=(this.getLeft()) 
  				&& x<=(this.getLeft()+lBounds.width())
  				&& y>=this.getPaddingTop() 
  				&& y<=(this.getHeight()-this.getPaddingBottom()))
	  		{
	  			this.setText("");
	  			event.setAction(MotionEvent.ACTION_CANCEL);//use this to prevent the keyboard from coming up
	  		}
	  	}
	  	if(event.getAction() == MotionEvent.ACTION_UP && dRight!=null)
	    {
	      rBounds = dRight.getBounds();
	      int n1 = this.getRight()-rBounds.width();
	      int n2 = this.getRight()-this.getPaddingRight();
	      int n3 = this.getPaddingTop();
	      int n4 = this.getHeight()-this.getPaddingBottom();
	      // prava strana
	      if(x>=(this.getRight()-rBounds.width()) && x<=(this.getRight()-this.getPaddingRight())
	      		&& y>=this.getPaddingTop() && y<=(this.getHeight()-this.getPaddingBottom())) {
	    	  
	    	  mCrossButton.requestFocus();
	    	  mCrossButton.performClick();
	      	  event.setAction(MotionEvent.ACTION_CANCEL);//use this to prevent the keyboard from coming up
	      }
	    }
	 
	    return super.onTouchEvent(event);
	}
	 
	@Override
	protected void finalize() throws Throwable
	{
	    dRight = null;
	    rBounds = null;
	    super.finalize();
	}
	
	public void setCrossButton(Button btnOk) {
		this.mCrossButton = btnOk;
	}
	
	public Button getCrossButton() {
		return mCrossButton;
	}
	
	boolean mControlKeyEventPreIme = false;
	Runnable mContrlKeyEventPreImeRunnable;
	public void setControlKeyEventPreIme(boolean control, Runnable run) {
		mControlKeyEventPreIme = control;
		mContrlKeyEventPreImeRunnable = run;
	}
	
	@Override
	public boolean dispatchKeyEventPreIme(KeyEvent event) {
		
	    if(KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
	    	// KEYCODE_BACK가 두번 발생됨 : down/up 이기 때문에
	    	if (mControlKeyEventPreIme) {
	    		//setFocusable(false);
	    		mContrlKeyEventPreImeRunnable.run();
	    		if (INFO) Log.i(TAG, "dispatchKeyEventPreIme:KEYCODE_BACK");
	    		return true;
	    	}
	    }   	    
	    
	    return super.dispatchKeyEventPreIme(event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (INFO) Log.i(TAG, "onKeyDown");
		
		if(KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			if (INFO) Log.i(TAG, "onKeyDown:KEYCODE_BACK");
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	
}
