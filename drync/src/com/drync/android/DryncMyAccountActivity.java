package com.drync.android;



import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

public class DryncMyAccountActivity extends Activity {

	private LinearLayout register;
	private WebView regWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.registerweb);
		
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
		
	}
	
	private class CustomWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (url.startsWith("close:"))
	    	{
	    		DryncMyAccountActivity.this.finish();
	    		return true;
	    	}
	    	
	    	if (url.contains("registration_request[device_id]"))
	    	{
	    		url.replaceAll("registration_request[device_id]=\\d*&", "bingo&");
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
