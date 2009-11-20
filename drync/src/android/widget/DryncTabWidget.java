package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabWidget;

public class DryncTabWidget extends TabWidget {

    private OnTabSelectionChanged mSelectionChangedListener;

    
	public DryncTabWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public DryncTabWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public DryncTabWidget(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	/**
     * Provides a way for {@link TabHost} to be notified that the user clicked on a tab indicator.
     */
    public void setTabSelectionListener(OnTabSelectionChanged listener) {
        mSelectionChangedListener = listener;
    }

	
	/**
     * Let {@link TabHost} know that the user clicked on a tab indicator.
     */
    public static interface OnTabSelectionChanged {
        /**
         * Informs the TabHost which tab was selected. It also indicates
         * if the tab was clicked/pressed or just focused into.
         *  
         * @param tabIndex index of the tab that was selected
         * @param clicked whether the selection changed due to a touch/click
         * or due to focus entering the tab through navigation. Pass true
         * if it was due to a press/click and false otherwise.
         */
        void onTabSelectionChanged(int tabIndex, boolean clicked);
    }
    
    public void onFocusChange(View v, boolean hasFocus) {
    	this.getOnFocusChangeListener().onFocusChange(v, hasFocus);
        
    	if (hasFocus) {
            int i = 0;
            while (i < getChildCount()) {
                if (getChildAt(i) == v) {
                    setCurrentTab(i);
                    mSelectionChangedListener.onTabSelectionChanged(i, false);
                    break;
                }
                i++;
            }
        }
    }

    @Override
    public void addView(View child) {
        if (child.getLayoutParams() == null) {
            final LinearLayout.LayoutParams lp = new LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(0, 0, 0, 0);
            child.setLayoutParams(lp);
        }

        // Ensure you can navigate to the tab with the keyboard, and you can touch it
        child.setFocusable(true);
        child.setClickable(true);

        super.addView(child);

        // TODO: detect this via geometry with a tabwidget listener rather
        // than potentially interfere with the view's listener
        child.setOnClickListener(new TabClickListener(getChildCount() - 1));
        child.setOnFocusChangeListener(this);
    }

 // registered with each tab indicator so we can notify tab host
    private class TabClickListener implements OnClickListener {

        private final int mTabIndex;

        private TabClickListener(int tabIndex) {
            mTabIndex = tabIndex;
        }

        public void onClick(View v) {
            mSelectionChangedListener.onTabSelectionChanged(mTabIndex, true);
        }
    }


}
