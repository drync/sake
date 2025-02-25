package com.drync.android;



import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DryncFacebookActivity extends Activity {

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
		sb.append("/facebook_authorization/authorize?device_id=");
		sb.append(DryncUtils.getDeviceId(DryncFacebookActivity.this.getContentResolver(), DryncFacebookActivity.this));

		regWebView.loadUrl(sb.toString());
		//regWebView.loadDataWithBaseURL(sb.toString(), myacctfile, "text/html", "utf-8", null);
		regWebView.requestFocus();

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//CookieSyncManager.getInstance().stopSync();
	}

	@Override
	protected void onResume() {
		super.onResume();

	/*	CookieStore cookieStore = DryncUtils.getCookieStore();
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
		CookieSyncManager.getInstance().startSync(); */
	}

	private class CustomWebViewClient extends WebViewClient {
		
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	Log.d("FACEBOOK_ACTIVITY", "URL: " + url);
	    	
	    	if (url.startsWith("close:"))
	    	{
	    		DryncFacebookActivity.this.finish();
	    		DryncFacebookActivity.this.setResult(RESULT_OK);
	    		return true;
	    	}
	    	else if (url.startsWith("facebook:///"))
	    	{
	    		if (url.contains("account_name"))
	    		{
	    			String substring = url.substring(url.indexOf("account_name"));
	    			DryncUtils.setFacebookAuthorized(DryncFacebookActivity.this, true);
	    		}
	    		CookieManager.getInstance().removeAllCookie();
	    		DryncFacebookActivity.this.setResult(RESULT_OK);
	    		DryncFacebookActivity.this.finish();
	    		return true;
	    	}

	    	view.loadUrl(url);
	    	Log.d("FACEBOOK_ACTIVITY", "continuing.");
	        return true;
	    }

		/*@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			CookieSyncManager.getInstance().sync();
			
			if (url.endsWith("#___1__"))
			{
				Toast acctCreated = Toast.makeText(DryncFacebookActivity.this, "Your account has been created.", Toast.LENGTH_LONG);
				acctCreated.show();
				
				Thread thread = new Thread()
				{
					@Override
					public void run() {
						super.run();
						// redo this to reset cookies.
						try {
							DryncProvider.getInstance().myAcctGet(DryncUtils.getDeviceId(DryncFacebookActivity.this.getContentResolver(), DryncFacebookActivity.this));
						} catch (DryncHostException e) {
							Log.e("DryncMyAccountActivity", "Error resetting my account page", e);
						}
					}
				};
				
				thread.start();
				
				DryncFacebookActivity.this.finish();
			}
		}*/

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
