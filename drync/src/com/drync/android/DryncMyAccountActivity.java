package com.drync.android;



import java.io.IOException;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DryncMyAccountActivity extends Activity {

	private LinearLayout register;
	private WebView regWebView;
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// let's clear the stored etag so that we can refresh the cellar.
		DryncUtils.setEtag(this, null);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DryncUtils.isFreeMode();
		
		setContentView(R.layout.registerweb);
		
		CookieSyncManager.createInstance(this);
		CookieSyncManager.getInstance().startSync();
		
		register = (LinearLayout) findViewById(R.id.registerwebwrap);
		
		regWebView = (WebView) findViewById(R.id.registerWeb);
		regWebView.setBackgroundColor(0);
		regWebView.getSettings().setJavaScriptEnabled(true);
		regWebView.setWebViewClient(new CustomWebViewClient());
		
		
		
		String myacctfile = null;
		
		try
		{
			myacctfile = DryncUtils.readFileAsString(DryncUtils.getCacheDir(this) + "myacct.html");
		} catch (DryncConfigException e) {
			myacctfile = null;
		} catch (IOException e) {
			myacctfile = null;
		}
		
		if ((myacctfile != null) && (! myacctfile.equals("")))
		{
			StringBuilder sb = new StringBuilder("http://");
			sb.append(DryncProvider.USING_SERVER_HOST);
			if (DryncProvider.USING_SERVER_HOST == DryncProvider.DEV_SERVER_HOST)
				sb.append(":3000");
			sb.append("/register");
			
			regWebView.loadDataWithBaseURL(sb.toString(), myacctfile, "text/html", "utf-8", null);
			regWebView.requestFocus();
		}
		else
		{
			Toast noReviewUrl = Toast.makeText(this, getResources().getString(R.string.nosettingsyet) + "\n\n" +
					getResources().getString(R.string.nosettingsyet2) +"\n", Toast.LENGTH_LONG);
			noReviewUrl.setGravity(Gravity.CENTER, 0, 0);
			noReviewUrl.show();
			this.finish();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		CookieSyncManager.getInstance().stopSync();
	}

	@Override
	protected void onResume() {
		super.onResume();

		CookieStore cookieStore = DryncUtils.getCookieStore();
		if (cookieStore != null)
		{
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeSessionCookie();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie cookie : cookies)
			{
				StringBuilder cookieUrl = new StringBuilder("http://");
				cookieUrl.append(cookie.getDomain()).append("/");
				StringBuilder cookieString = new StringBuilder();
				cookieString.append(cookie.getName()).append("=").append(cookie.getValue()).append("; domain=").append(
						cookie.getDomain());

				cookieManager.setCookie(cookieUrl.toString(), cookieString.toString());
				CookieSyncManager.getInstance().sync(); 
			}
		}
		CookieSyncManager.getInstance().startSync();
	}

	private class CustomWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (url.startsWith("close:"))
	    	{
	    		DryncMyAccountActivity.this.finish();
	    		return true;
	    	}

	        view.loadUrl(url);
	        return true;
	    }

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			CookieSyncManager.getInstance().sync();
			
			if (url.endsWith("#___1__"))
			{
				Toast acctCreated = Toast.makeText(DryncMyAccountActivity.this, "Your account has been created.", Toast.LENGTH_LONG);
				acctCreated.show();
				
				Thread thread = new Thread()
				{
					@Override
					public void run() {
						super.run();
						// redo this to reset cookies.
						try {
							DryncProvider.getInstance().myAcctGet(DryncUtils.getDeviceId(DryncMyAccountActivity.this.getContentResolver(), DryncMyAccountActivity.this));
						} catch (DryncHostException e) {
							Log.e("DryncMyAccountActivity", "Error resetting my account page", e);
						}
					}
				};
				
				thread.start();
				
				DryncMyAccountActivity.this.finish();
			}
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
		}
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
		  super.onConfigurationChanged(newConfig);
		}
}
