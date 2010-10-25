package com.drync.android;


import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DryncTwitterActivity extends Activity {

	private WebView regWebView;
	
	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DryncUtils.isFreeMode();
		
		setContentView(R.layout.registerweb);
				
		regWebView = (WebView) findViewById(R.id.registerWeb);
		regWebView.setBackgroundColor(0);
		regWebView.getSettings().setJavaScriptEnabled(true);
		regWebView.setWebViewClient(new CustomWebViewClient());
		
			StringBuilder sb = new StringBuilder("http://");
			sb.append(DryncProvider.USING_SERVER_HOST);
			if (DryncProvider.USING_SERVER_HOST == DryncProvider.DEV_SERVER_HOST)
				sb.append(":3000");
			sb.append("/twitter_authorization/authorize?device_id=");
			sb.append(DryncUtils.getDeviceId(DryncTwitterActivity.this.getContentResolver(), DryncTwitterActivity.this));
			
			regWebView.loadUrl(sb.toString());
			regWebView.requestFocus();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private class CustomWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (url.startsWith("close:"))
	    	{
	    		DryncTwitterActivity.this.finish();
	    		DryncTwitterActivity.this.setResult(RESULT_OK);
	    		return true;
	    	}
	    	else if (url.startsWith("twitter:///"))
	    	{
	    		if (url.contains("account_name"))
	    		{
	    			String substring = url.substring(url.indexOf("account_name"));
	    			DryncUtils.setTwitterAuthorized(DryncTwitterActivity.this, true);
	    		}
	    		CookieManager.getInstance().removeAllCookie();	
	    		DryncTwitterActivity.this.setResult(RESULT_OK);
	    		DryncTwitterActivity.this.finish();
	    		return true;
	    	}

	        view.loadUrl(url);
	        return false;
	    }
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
		  super.onConfigurationChanged(newConfig);
		}
}
