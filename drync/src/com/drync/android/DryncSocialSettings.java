package com.drync.android;

import java.util.List;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

public class DryncSocialSettings extends DryncBaseActivity {

	SharedPreferences settings;
	boolean twitterAuthd = false;
	boolean fbAuthd = false;
	final Handler mHandler = new Handler();
	private ProgressDialog progressDlg = null;
	
	final Runnable mUpdateResults = new Runnable()
	{
		public void run()
		{
			updateResults();
			if (progressDlg != null)
			{
				progressDlg.dismiss();
			}
		}

		private void updateResults() {
			
		}
	};
	
	/*EditText usernameEdit;
	EditText passwordEdit;
	ToggleButton cellarTweetBtn;*/
	
	Button twitterButton;
	Button facebookButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
	     setContentView(R.layout.socialsettings);
	     
	     settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
	     
	    /* usernameEdit = (EditText)this.findViewById(R.id.usernameVal);
	     passwordEdit = (EditText)this.findViewById(R.id.passwordVal);
	     cellarTweetBtn = (ToggleButton)this.findViewById(R.id.cellarTweetVal); */
	     
	     twitterButton = (Button)this.findViewById(R.id.twitterSettingsBtn);
	     
	     twitterAuthd = DryncUtils.isTwitterAuthorized(DryncSocialSettings.this);
	     if (twitterAuthd)
	    	 twitterButton.setText("Sign Out");
	     
	     facebookButton = (Button)this.findViewById(R.id.facebookSettingsBtn);
	     fbAuthd = DryncUtils.isFacebookAuthorized(DryncSocialSettings.this);
	     if (fbAuthd)
	    	 this.facebookButton.setText("Sign Out");
	     
	     final String deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
	     
	     twitterButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				if (!twitterAuthd)
				{
					Intent intent = new Intent();
					intent.setClass(DryncSocialSettings.this, DryncTwitterActivity.class);
					startActivityForResult(intent, DryncBaseActivity.TWITTER_AUTH_RESULT);
				}
				else
				{
					new AlertDialog.Builder(DryncSocialSettings.this)
					.setMessage("Are you sure you want to sign out?")
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									DryncProvider.postTwitterDeauth(deviceId);
									DryncUtils.setTwitterAuthorized(DryncSocialSettings.this, false);
									twitterButton.setText("Twitter");
									dialog.cancel();
								}})
							.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
								}})
								.show();
				}
				
			}});
	     
	     this.facebookButton.setOnClickListener(new OnClickListener(){
	    		 public void onClick(View v)
	    		 {
	    			 if (!fbAuthd)
	    			 {
	    				 Intent intent = new Intent();
	    				 intent.setClass(DryncSocialSettings.this, DryncFacebookActivity.class);
	    				 startActivityForResult(intent,DryncBaseActivity.FACEBOOK_AUTH_RESULT);
	    			 }
	    			 else
	    			 {
	    				 new AlertDialog.Builder(DryncSocialSettings.this)
	 					.setMessage("Are you sure you want to sign out?")
	 							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	 								public void onClick(DialogInterface dialog, int id) {
	 									DryncProvider.postFacebookDeauth(deviceId);
	 									DryncUtils.setFacebookAuthorized(DryncSocialSettings.this, false);
	 									facebookButton.setText("Facebook");
	 									dialog.cancel();
	 								}})
	 							.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	 								public void onClick(DialogInterface dialog, int id) {
	 									dialog.cancel();
	 								}})
	 								.show();
	    			 }
	    		 }});
	     
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DryncBaseSearch.TWITTER_AUTH_RESULT)
		{
			this.twitterAuthd = DryncUtils.isTwitterAuthorized(DryncSocialSettings.this);
			if (twitterAuthd)
			{
				this.twitterButton.setText("Sign Out");
			}
			else
			{
				this.twitterButton.setText("Sign In");
			}
			
		}
		if (requestCode == DryncBaseSearch.FACEBOOK_AUTH_RESULT)
		{
			this.fbAuthd = DryncUtils.isTwitterAuthorized(DryncSocialSettings.this);
			if (fbAuthd)
			{
				this.facebookButton.setText("Sign Out");
			}
			else
			{
				this.facebookButton.setText("Sign In");
			}
			
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void resetSettings()
	{
		String deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		
		final String threadDeviceId = deviceId;
		Thread t = new Thread() {
			public void run() {
				
				try {
					DryncProvider.getInstance()
					.getCorks(DryncSocialSettings.this, threadDeviceId);
					DryncProvider.getInstance().myAcctGet(threadDeviceId);
				} catch (DryncHostException e) {
					Log.e("DryncSettings", "Error getting my account page", e);
				} catch (DryncXmlParseExeption e) {
					Log.e("DryncSettings", "Error updating corks", e);
				}
				
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		/*final Editor editor = settings.edit();
		
		String username = usernameEdit.getText().toString();
		if ((username != null) && (!username.equals("")))
			editor.putString(DryncUtils.TWITTER_USERNAME_PREF, username);
		
		String password = passwordEdit.getText().toString();
		if ((password != null) && (!password.equals("")))
		{
			String encryptedPw = DryncUtils.encryptTwitterPassword(password);
			editor.putString(DryncUtils.TWITTER_PASSWORD_PREF, encryptedPw);
		}
		
		boolean cellarTweet = cellarTweetBtn.isChecked();
			editor.putBoolean(DryncUtils.TWITTER_CELLARTWT_PREF, cellarTweet);
		
		editor.commit(); */
	}
	
	@Override
	public int getMenuItemToSkip() {
		return SETTINGS_ID;
	}
}
