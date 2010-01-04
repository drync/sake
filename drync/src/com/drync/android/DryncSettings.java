package com.drync.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ToggleButton;

public class DryncSettings extends Activity {

	SharedPreferences settings;
	EditText usernameEdit;
	EditText passwordEdit;
	ToggleButton cellarTweetBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
	     setContentView(R.layout.settings);
	     
	     settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
	     
	     usernameEdit = (EditText)this.findViewById(R.id.usernameVal);
	     passwordEdit = (EditText)this.findViewById(R.id.passwordVal);
	     cellarTweetBtn = (ToggleButton)this.findViewById(R.id.cellarTweetVal);
	     
	     String username = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, "");
	     usernameEdit.setText(username);
	     
	     String encryptedpassword = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, "");
	     String password = DryncUtils.decryptTwitterPassword(encryptedpassword);
	     if (password == null)
	    	 passwordEdit.setText("");
	     else
	    	 passwordEdit.setText(password);
	     
	     boolean cellarTweet = settings.getBoolean(DryncUtils.TWITTER_CELLARTWT_PREF, false);
	     cellarTweetBtn.setChecked(cellarTweet);
	     
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		final Editor editor = settings.edit();
		
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
		
		editor.commit();
	}
}
