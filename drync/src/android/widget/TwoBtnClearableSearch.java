package android.widget;

import com.drync.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

public class TwoBtnClearableSearch extends ClearableSearch {

	private ImageButton addButton = null;
	
	public TwoBtnClearableSearch(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		setAddButton((ImageButton)findViewById(R.id.addBtn));
	}
	
	public TwoBtnClearableSearch(Context context) {
        this(context, null);
    }

    public TwoBtnClearableSearch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

	protected void inflateLayout(Context context)
    {
    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.twobtnclearablesearch, this, true);
    }

	public void setAddButton(ImageButton addButton) {
		this.addButton = addButton;
	}

	public ImageButton getAddButton() {
		return addButton;
	}

}
