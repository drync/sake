package com.drync.android;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.drync.android.helpers.CSVReader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Drync extends Activity {

	private static final int STOPSPLASH = 0;
	private static final int STARTMAIN = 1;
	private static final int REGISTER = 2;
	private static final long SPLASHTIME = 3000;
	
	private static ImageView splash;
	private static LinearLayout register;
	private static String registerTxt;
	private static WebView regWebView;

	private Handler splashHandler = new Handler() 
    {
    	@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what)
    		{
    		case STOPSPLASH:
    			// remove Splashscreen from view
    			splash.setVisibility(View.GONE);
    			setContentView(R.layout.findyourwine);
    			final Button gotIt = (Button)findViewById(R.id.thanksGotIt);
    	        
    	        gotIt.setOnClickListener(new OnClickListener() {
    	        	public void onClick(View v) {
    	        		if (Drync.this.mShowReg)
        	        	{
    	        			Drync.this.mShowIntro = false;
    	        			Drync.this.mShowReg = false;
    	        			Message msg2 = new Message();
    	        			msg2.what = REGISTER;
    	        			splashHandler.sendMessage(msg2);    	        			
        	        	}
    	        		else
    	        		{
    	        			Intent intent = new Intent();
    	        			intent.setClass(Drync.this, DryncSearch.class);
    	        			startActivity(intent);
    	        			Drync.this.mShowIntro = false;
    	        			finish();
    	        		}
    	        	}
    	        });
    			break;
    		case REGISTER:
    			splash.setVisibility(View.GONE);
    			setContentView(R.layout.registerweb);
    			
    			register = (LinearLayout) findViewById(R.id.registerwebwrap);
    			
    			regWebView = (WebView) findViewById(R.id.registerWeb);
    			regWebView.setBackgroundColor(0);
    			regWebView.getSettings().setJavaScriptEnabled(true);
    			regWebView.setWebViewClient(new RegisterWebViewClient());
    			
    			String registerText = null;
    			registerText = registerTxt;
    			
    			if (registerText != null)
    			{
    				StringBuilder sb = new StringBuilder("http://");
    				sb.append(DryncProvider.USING_SERVER_HOST);
    				if (DryncProvider.USING_SERVER_HOST == DryncProvider.DEV_SERVER_HOST)
    					sb.append(":3000");
    				sb.append("/app_session");
    				regWebView.loadDataWithBaseURL(sb.toString(), registerText, "text/html", "utf-8", null);
    				regWebView.requestFocus();
    			}
    			break;
    			
    		case STARTMAIN:
    			// remove SplashScreen from view
    			splash.setVisibility(View.GONE);
    			if (register != null)
    				register.setVisibility(View.GONE);
    			// start Main & End Intro
    			Intent intent = new Intent();
                intent.setClass(Drync.this, DryncSearch.class);
                startActivity(intent);
                finish();
                break;
    		}
    		super.handleMessage(msg);
    	}
    };
	private boolean mShowIntro = true;
	private boolean mShowReg = true;
	
	
	/** Called when the activity is first created. */
    
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // call this to initialize the cache directory
        try {
			DryncUtils.getCacheDir(this);
		} catch (DryncConfigException e) {
			Log.d("Drync", "Error initializing the CacheDirectory. - " + e.getMessage());
		}
        setContentView(R.layout.splash);
        splash = (ImageView) findViewById(R.id.splashscreen);
        
        String deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
        String register = DryncProvider.getInstance().startupPost(deviceId);
        
        final String threadDeviceId = deviceId;
        Thread t = new Thread()
        {
			public void run() {
				DryncProvider.getInstance().getCorksToFile(Drync.this, threadDeviceId);
			}
		};
		t.start();
		
        registerTxt = register;
     // Restore preferences
        SharedPreferences settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
        boolean showIntro = settings.getBoolean(DryncUtils.SHOW_INTRO_PREF, true);
        mShowIntro = showIntro;
        
        mShowReg = register != null && (! register.equals(""));
        
        Message msg = new Message();
        if (mShowIntro)
        	msg.what = STOPSPLASH;
        else if (mShowReg)
        	msg.what = REGISTER;
        else
        	msg.what = STARTMAIN;
        
        splashHandler.sendMessageDelayed(msg, SPLASHTIME);
    }
	
	@Override
    protected void onStop(){
       super.onStop();
    
      // Save user preferences. We need an Editor object to
      // make changes. All objects are from android.context.Context
      SharedPreferences settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putBoolean(DryncUtils.SHOW_INTRO_PREF, mShowIntro);

      // Don't forget to commit your edits!!!
      editor.commit();
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if ((register != null) && (register.getVisibility() == View.VISIBLE))
			{
				regWebView.goBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	
	private class RegisterWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (url.startsWith("close:"))
	    	{
	    		if (Drync.register != null)
	    		{
	    			Message msg = new Message();
	    			msg.what = Drync.STARTMAIN;
	    			Drync.this.splashHandler.sendMessage(msg);
	    		}
	    	}
	        view.loadUrl(url);
	        return true;
	    }

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
		}

		@Override
		public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
			super.onUnhandledKeyEvent(view, event);
		}
	}
}