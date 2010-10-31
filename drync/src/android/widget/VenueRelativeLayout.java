package android.widget;

import com.drync.android.objects.Venue;

import android.content.Context;
import android.util.AttributeSet;

public class VenueRelativeLayout extends RelativeLayout {
	Venue venue= null;
	
	public Venue getVenue() {
		return venue;
	}

	public void setVenue(Venue venue) {
		this.venue = venue;
	}

	public VenueRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public VenueRelativeLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public VenueRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

}
