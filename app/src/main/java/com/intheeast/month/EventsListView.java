package com.intheeast.month;

import com.intheeast.acalendar.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.ListView;


public class EventsListView extends ListView {

	int mWidth;
	int mHeight;
	
	public Paint mUpperLinePaint;
	float mStrokeWidth;	
	
    public EventsListView(Context context) {
        super(context);
        initView();
    }

    public EventsListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public EventsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    
    public void initView() {
    	mUpperLinePaint = new Paint();        
        mUpperLinePaint.setAntiAlias(true);        
        mUpperLinePaint.setColor(getResources().getColor(R.color.eventViewItemUnderLineColor));        
        mUpperLinePaint.setStyle(Style.STROKE);        
        mStrokeWidth = getResources().getDimension(R.dimen.eventItemLayoutUnderLineHeight);
        mUpperLinePaint.setStrokeWidth(mStrokeWidth);
    }

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i("tag", "EventsListView : onDraw");	
		
		float startX = 0;
        //float startY = /*mHeight - */mStrokeWidth; // y ���� �� ��������� �h��...�� �׷��� �翬�� ©����
		float startY = 0;
        float stopX = mWidth;
        float stopY = startY;
        canvas.drawLine(startX, startY, stopX, stopY, mUpperLinePaint);
		
        super.onDraw(canvas);		
	}
    
    
    
}
