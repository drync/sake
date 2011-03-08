package com.drync.android;

import java.util.List;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

public class DryncSettings extends DryncBaseActivity {

	SharedPreferences settings;
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
	
	Button myAcctButton;
	Button socialSettingsButton;
	Button resetButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
	     setContentView(R.layout.settings);
	     
	     settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
	     
	    /* usernameEdit = (EditText)this.findViewById(R.id.usernameVal);
	     passwordEdit = (EditText)this.findViewById(R.id.passwordVal);
	     cellarTweetBtn = (ToggleButton)this.findViewById(R.id.cellarTweetVal); */
	     
	     myAcctButton = (Button)this.findViewById(R.id.acctSettingsBtn);
	     socialSettingsButton = (Button)this.findViewById(R.id.socialSettingsBtn);
	     resetButton = (Button)this.findViewById(R.id.resetBtn);
	   /*  String username = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, "");
	     usernameEdit.setText(username);
	     
	     String encryptedpassword = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, "");
	     String password = DryncUtils.decryptTwitterPassword(encryptedpassword);
	     if (password == null)
	    	 passwordEdit.setText("");
	     else
	    	 passwordEdit.setText(password);
	     
	     boolean cellarTweet = settings.getBoolean(DryncUtils.TWITTER_CELLARTWT_PREF, false);
	     cellarTweetBtn.setChecked(cellarTweet);*/
	     
	     
	     myAcctButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				Intent intent = new Intent();
    			intent.setClass(DryncSettings.this, DryncMyAccountActivity.class);
    			startActivityForResult(intent, MYACCOUNT_RESULT);
				
			}});
	     
	     socialSettingsButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					Intent intent = new Intent();
	    			intent.setClass(DryncSettings.this, DryncSocialSettings.class);
	    			startActivity(intent);					
				}});
	     
	     this.resetButton.setOnClickListener(new OnClickListener(){
	    		 public void onClick(View v)
	    		 {
	    			 progressDlg =  new ProgressDialog(DryncSettings.this);
						progressDlg.setTitle("Dryncing...");
						progressDlg.setMessage("Updating Settings...");
						progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						progressDlg.show();
	    			 resetSettings();
	    		 }});
	     
	}

	public void resetSettings()
	{
		String deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		
		final String threadDeviceId = deviceId;
		Thread t = new DryncThread() {
			public void run() {
				
				try {
					DryncProvider.getInstance()
					.getCorks(DryncSettings.this, threadDeviceId);
					DryncProvider.getInstance().myAcctGet(DryncSettings.this.getBaseContext(), threadDeviceId);
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
