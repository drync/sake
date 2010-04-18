package android.widget;

import com.drync.android.objects.Bottle;

import android.content.Context;
import android.util.AttributeSet;

public class WineItemRelativeLayout extends RelativeLayout {
	Bottle bottle = null;
	
	public Bottle getBottle() {
		return bottle;
	}

	public void setBottle(Bottle bottle) {
		this.bottle = bottle;
	}

	public WineItemRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public WineItemRelativeLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public WineItemRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

}
