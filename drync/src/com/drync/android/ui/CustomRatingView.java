package com.drync.android.ui;

import com.drync.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

public class CustomRatingView extends View {
    private ShapeDrawable mDrawable;
    private BitmapDrawable mBaseDrawable;
    private Bitmap mBase;

    public CustomRatingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public CustomRatingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CustomRatingView(Context context) {
        super(context);
        init(context);
    }

	public void init(Context context)
	{
		Resources res = context.getResources();
		mBase = BitmapFactory.decodeResource(res, R.drawable.starburstx41);
        mBaseDrawable = new BitmapDrawable(mBase);
	}
    protected void onDraw(Canvas canvas) {
    	canvas.setViewport(51, 50);
    	Paint paint = new Paint();
    	//paint.setStyle(Paint.Style.FILL);
    	
    	Rect srcRect = new Rect(0, 0, mBase.getWidth(), mBase.getHeight());
    	int smallest = this.getWidth() < this.getHeight() ? this.getWidth() : this.getHeight();
    	Rect destRect = new Rect(0, 0, smallest, smallest);
    	//canvas.clipRect(canvas.getWidth()-50, 0, 50, 50);
    	
    	canvas.translate(this.getWidth()-smallest, 0);
    	canvas.drawBitmap(mBase, srcRect, destRect, paint);
    	canvas.save();
    	canvas.restore();
    	
    	int x = 11;
    	int y = 18;
    	paint.setColor(Color.GRAY);
    	paint.setTextSize(20);
    	String str2rotate = "50";

    	// draw bounding rect before rotating text
    	Rect rect = new Rect();
    	paint.getTextBounds(str2rotate, 0, str2rotate.length(), rect);
    

    	// rotate the canvas on center of the text to draw
    //	canvas.rotate(45, x + rect.exactCenterX(), y + rect.exactCenterY());
    	// draw the rotated text
    	paint.setStyle(Paint.Style.FILL);
    	//canvas.drawRect(destRect, paint);
    	canvas.drawText(str2rotate, 0, 0, paint);

    	//undo the rotate
    	canvas.restore();
    	
    	//canvas.drawText("After canvas.restore()", 50, 300, paint);
    }
}
