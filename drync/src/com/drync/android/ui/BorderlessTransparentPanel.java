package com.drync.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BorderlessTransparentPanel extends LinearLayout {

	public BorderlessTransparentPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setClickable(true);
	}

	public BorderlessTransparentPanel(Context context) {
		super(context);
		this.setClickable(true);
	}
	
	

    protected void dispatchDraw(Canvas canvas) {

        RectF drawRect = new RectF();
        drawRect.set(0,0, getMeasuredWidth(), getMeasuredHeight());
        Paint innerPaint = new Paint();
        innerPaint.setARGB(200, 75, 75, 75);
        
       /* Paint borderPaint = new Paint();
        borderPaint = new Paint();
        borderPaint.setARGB(255, 255, 255, 255);
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Style.STROKE);
        borderPaint.setStrokeWidth(2);*/
        
        canvas.drawRoundRect(drawRect, 5, 5, innerPaint);
      //  canvas.drawRoundRect(drawRect, 5, 5, borderPaint);

        super.dispatchDraw(canvas);

    }
}
