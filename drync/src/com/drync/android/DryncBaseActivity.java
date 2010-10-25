package com.drync.android;

import java.util.Random;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdSenseSpec;
import com.google.ads.GoogleAdView;
import com.google.ads.AdSenseSpec.AdType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public abstract class DryncBaseActivity extends Activity {
	
	// Menu IDs
	public static final int SEARCH_ID = Menu.FIRST;
	public static final int CELLAR_ID = Menu.FIRST + 1;
	public static final int TOP_WINE_ID = Menu.FIRST + 2;
	public static final int SETTINGS_ID = Menu.FIRST + 3;
	
	// Activity Results
	public static final int ADDTOCELLAR_RESULT = 1;	
	public static final int MYACCOUNT_RESULT = 2;
	public static final int TWITTER_AUTH_RESULT = 10;
	public static final int FACEBOOK_AUTH_RESULT = 11;
	
	public static final String STARTUP_INTENT = "com.drync.android.intent.action.STARTUP";
	
	
	// view members:
	GoogleAdView googleAdView;
	
	// abstract methods
	public abstract int getMenuItemToSkip();
	
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		
		int skipMenuItem = getMenuItemToSkip();
		
		if (skipMenuItem != SEARCH_ID)
		{
			menu.add(0, SEARCH_ID, 0, R.string.search).
			setIcon(getResources().getDrawable(R.drawable.tab_icon_search));
		}
		
		if (skipMenuItem != CELLAR_ID)
		{
			menu.add(0, CELLAR_ID, 0, R.string.cellar).
			setIcon(getResources().getDrawable(R.drawable.tab_icon_cellar));
		}
		
		if (skipMenuItem != TOP_WINE_ID)
		{
			menu.add(0, TOP_WINE_ID, 0, R.string.topwines).
			setIcon(getResources().getDrawable(R.drawable.tab_icon_topwines));
		}
		
		if (skipMenuItem != SETTINGS_ID)
		{
			menu.add(0, SETTINGS_ID, 0, R.string.settings).
			setIcon(getResources().getDrawable(R.drawable.tab_icon_settings));
		}
		
		return result;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DryncUtils.isFreeMode();
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
        	/*Intent intent = new Intent();
			intent.setClass(this, DryncMyAccountActivity.class);
			startActivityForResult(intent, MYACCOUNT_RESULT);*/
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
            	/*if (resultCode == DryncCellar.CELLAR_NEEDS_REFRESH)
            	{
            		this.doStartupFetching(false);
            	}*/
               // this.startDryncCellarActivity();
            }
            break;
        case MYACCOUNT_RESULT:
        {
        	
    		break;
        }
        default:
            break;
    }
	}

    protected void startDryncCellarActivity()
    {
    	startDryncCellarActivity(false);
    }
    
	protected void startDryncCellarActivity(boolean triggerRefresh)
    {
    	Intent setIntent = new Intent(this, DryncCellar.class);
    	if (triggerRefresh)
    	{
    		setIntent.putExtra("needsrefresh", true);
    	}
    	startActivity(setIntent);
    }
	
	protected void doStartupFetching()
	{
		doStartupFetching(false);
	}
	
	protected void doStartupFetching(boolean wait)
	{
		 // do refresh of cellar data on start of every activity...
		String deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		final String threadDeviceId = deviceId;

		if (!wait)
		{

			Thread t = new Thread() {
				public void run() {
					try
					{
						DryncProvider.getInstance()
						.getCorks(DryncBaseActivity.this, threadDeviceId);
						DryncProvider.getInstance().myAcctGet(threadDeviceId);
					}
					catch(Exception e)
					{
						Log.e("An error has occurred during a periodic fetch of the cellar & My Account info.", e.getMessage());
					}
				}
			};
			t.start();
		}
		else
		{
			try
			{
				DryncProvider.getInstance()
			.getCorks(DryncBaseActivity.this, threadDeviceId);
				DryncProvider.getInstance().myAcctGet(threadDeviceId);
			}
			catch (Exception e)
			{
				Log.e("An error has occurred during a determined fetch of the cellar & My Account info.", e.getMessage());
			}
		}
	}
	
	public void onStart()
	{
	   super.onStart();
	   FlurryAgent.onStartSession(this, DryncUtils.getDryncFlurryCode());	
	   doStartupFetching();
	}
	
	public void onStop()
	{
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
	
	public boolean hasConnectivity()
	{
		ConnectivityManager cmgr = 
			(ConnectivityManager) this.getSystemService(
					Context.CONNECTIVITY_SERVICE);

		NetworkInfo mobileinfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo wifiinfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (((mobileinfo != null) && 
				(mobileinfo.isConnected())) ||
				((wifiinfo != null) && (wifiinfo.isConnected())))
			return true;
		
		else
			return false;
	}
	
	public AdSenseSpec buildAdSpec()
	{
		AdSenseSpec adSenseSpec =
		    new AdSenseSpec("ca-mb-app-pub-2007025970278328")
		    .setCompanyName("Drync LLC")
		    .setAppName("Drync Wine Free Droid")
		    .setKeywords(getGoogleAdSenseKeywords())
		    .setChannel("4305989908")
		    .setAdType(AdType.TEXT_IMAGE);
		
		adSenseSpec.setAdTestEnabled(false);
		
		return adSenseSpec;
	}
	
	public void initializeAds()
	{
		if (googleAdView == null)
			googleAdView = (GoogleAdView) findViewById(R.id.adview);
		
		if (googleAdView != null)
		{
			if (DryncUtils.isFreeMode())
			{
				AdSenseSpec adSenseSpec = this.buildAdSpec();
				googleAdView.showAds(adSenseSpec);
			}
			else
			{
				googleAdView.setVisibility(View.GONE);
			}
		}
	}
	
	static final String[] adKeywords = 
		{
			"restaurant", "merlot", "shiraz", "pairing", "chardonnay", "burgundy",
			"cabernet", "sauvignon", "vineyard", "vine", "Torbreck", "cellar",
			"reserve", "zinfandel", "moscato", "semillon", "muscat", "dom perignon",
			"blanc", "pinot", "gris", "vino", "grape", "syrah", "brut", "rose", "red", 
			"white", "wine glasses", "shot glasses", "decanter"
		};
	
	private static Random rand = new Random(System.currentTimeMillis());
	
	public String getGoogleAdSenseKeywords()
	{
		
		StringBuilder bldr = new StringBuilder("wine drink");
		
		// get 4 of above at random and append.
		for (int i=0,n=4; i<n; i++)
		{
			String keyword = adKeywords[rand.nextInt(adKeywords.length)];
			bldr.append(" " + keyword);
		}
		
		
		/*merlot shiraz pairing chardonnay burgundy cabernet") 
		.append(" sauvingon vineyard vine Torbreck cellar reserve zinfandel") 
		.append(" moscato semillon muscat dom perignon blanc pinot gris vino") 
		.append(" grape syrah brut rose red white");*/
		
		return bldr.toString();
	}
	
	
}
