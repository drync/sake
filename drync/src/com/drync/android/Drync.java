package com.drync.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

public class Drync extends Activity {

	private static final int STOPSPLASH = 0;
	private static final int STARTMAIN = 1;
	private static final long SPLASHTIME = 3000;
	private String PREFS_NAME = "DRYNC_PREFS";
	
	private static ImageView splash;

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
    	        		Intent intent = new Intent();
    	                intent.setClass(Drync.this, DryncMain.class);
    	                startActivity(intent);
    	                Drync.this.mShowIntro = false;
    	                finish();

    	        	}
    	        });
    			break;
    		case STARTMAIN:
    			// remove SplashScreen from view
    			splash.setVisibility(View.GONE);
    			// start Main & End Intro
    			Intent intent = new Intent();
                intent.setClass(Drync.this, DryncMain.class);
                startActivity(intent);
                finish();
                break;
    		}
    		super.handleMessage(msg);
    	}
    };
	private boolean mShowIntro = true;
	
	
	/** Called when the activity is first created. */
    
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        splash = (ImageView) findViewById(R.id.splashscreen);
     
     // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean showIntro = settings.getBoolean("showIntro", true);
        mShowIntro = showIntro;
        
        Message msg = new Message();
        if (mShowIntro)
        	msg.what = STOPSPLASH;
        else
        	msg.what = STARTMAIN;
        
        splashHandler.sendMessageDelayed(msg, SPLASHTIME);
    }
	
	@Override
    protected void onStop(){
       super.onStop();
    
      // Save user preferences. We need an Editor object to
      // make changes. All objects are from android.context.Context
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putBoolean("showIntro", mShowIntro);

      // Don't forget to commit your edits!!!
      editor.commit();
    }

}