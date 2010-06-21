package com.drync.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class DryncBaseActivity extends Activity {
	
	public static final int SEARCH_ID = Menu.FIRST;
	public static final int CELLAR_ID = Menu.FIRST + 1;
	public static final int TOP_WINE_ID = Menu.FIRST + 2;
	public static final int SETTINGS_ID = Menu.FIRST + 3;
	
	public static final int ADDTOCELLAR_RESULT = 1;	
	
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		
		menu.add(0, SEARCH_ID, 0, R.string.search).
		setIcon(getResources().getDrawable(R.drawable.tab_icon_search));
		menu.add(0, CELLAR_ID, 0, R.string.cellar).
		setIcon(getResources().getDrawable(R.drawable.tab_icon_cellar));
		menu.add(0, TOP_WINE_ID, 0, R.string.topwines).
		setIcon(getResources().getDrawable(R.drawable.tab_icon_topwines));
		menu.add(0, SETTINGS_ID, 0, R.string.settings).
		setIcon(getResources().getDrawable(R.drawable.tab_icon_settings));
		
		return result;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			//boolean retval = false;

			DryncBaseActivity.this.finish();
			/*
			if ((flipper.getCurrentView() == detailView) || (flipper.getCurrentView() == reviewView) ||
					(flipper.getCurrentView() == addView))
			{
				showPrevious();
				retval = true;
			}

			if (retval)
				return true; */
		}

		return super.onKeyDown(keyCode, event);
	}
	
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case TOP_WINE_ID:
        {
        	Intent twIntent = new Intent(this, DryncTopWines.class);
    		twIntent.putExtra("displaySearch", false);
    		twIntent.putExtra("displayTopWinesBtns", true);
    		twIntent.putExtra("topType", DryncProvider.TOP_FEATURED);
    		startActivity(twIntent);  
        	break;
        }
        case SETTINGS_ID:
        {
        	Intent setIntent = new Intent(this, DryncSettings.class);
        	startActivity(setIntent);
        	break;
        }
        case CELLAR_ID:
        {
        	startDryncCellarActivity();
        	break;
        }
        case SEARCH_ID:
        {
        	Intent setIntent = new Intent(this, DryncSearch.class);
        	startActivity(setIntent);
        	break;
        }
        }
       
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
        case ADDTOCELLAR_RESULT:
            // This is the standard resultCode that is sent back if the
            // activity crashed or didn't doesn't supply an explicit result.
            if (resultCode == RESULT_CANCELED){
                // do nothing.
            } 
            else {
                this.startDryncCellarActivity();
            }
        default:
            break;
    }
	}

	protected void startDryncCellarActivity()
    {
    	Intent setIntent = new Intent(this, DryncCellar.class);
    	startActivity(setIntent);
    }

}
