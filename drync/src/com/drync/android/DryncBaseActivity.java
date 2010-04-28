package com.drync.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
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

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case TOP_WINE_ID:
        {
        	Intent twIntent = new Intent(this, DryncSearch.class);
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
        	StringBuilder bldr = new StringBuilder();
        	bldr.append("Copyright © 2009, IZOS\n");
        	bldr.append("\nMany thanks to meritbadge.org\n for their content!\n");
        	bldr.append("Used without permission.\n\nAd Proceeds will be donated to\n");
            bldr.append("http://www.scoutingfriends.org/"); 
        	
        	Dialog dlg = new Dialog(this);
        	LayoutInflater inf = dlg.getLayoutInflater();
        	View abtView = inf.inflate(R.layout.about, null);
        	TextView tv = (TextView) abtView.findViewById(R.id.text1);
        	tv.setText(bldr.toString());
        	dlg.addContentView(abtView, new LinearLayout.LayoutParams(500, 200));
        	
        	dlg.show();
        	
        	return true;
        }
        }
       
        return super.onOptionsItemSelected(item);
    }

}
