package com.drync.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import com.drync.android.helpers.CSVReader;
import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Drync extends Activity {

	private static final int STOPSPLASH = 0;
	private static final int STARTMAIN = 1;
	private static final int REGISTER = 2;
	private static final long SPLASHTIME = 6000;

	private static RelativeLayout splash;
	private static LinearLayout register;
	private static String registerTxt;
	private static WebView regWebView;

	private Handler splashHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case STOPSPLASH:
				// remove Splashscreen from view
				splash.setVisibility(View.GONE);
				setContentView(R.layout.findyourwine);
				final Button gotIt = (Button) findViewById(R.id.thanksGotIt);

				gotIt.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (Drync.this.mShowReg) {
							Drync.this.mShowIntro = false;
							Drync.this.mShowReg = false;
							Message msg2 = new Message();
							msg2.what = REGISTER;
							splashHandler.sendMessage(msg2);
						} else {
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

				if (registerText != null) {
					StringBuilder sb = new StringBuilder("http://");
					sb.append(DryncProvider.USING_SERVER_HOST);
					if (DryncProvider.USING_SERVER_HOST == DryncProvider.DEV_SERVER_HOST)
						sb.append(":3000");
					sb.append("/app_session");
					regWebView.loadDataWithBaseURL(sb.toString(), registerText,
							"text/html", "utf-8", null);
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

		// set whether this is the free app or not.
		boolean isFree = 
		DryncUtils.isFreeMode();
		
		CookieSyncManager.createInstance(this);
		CookieSyncManager.getInstance().startSync();

		// call this to initialize the cache directory
		try {
			DryncUtils.getCacheDir(this);
		} catch (DryncConfigException e) {
			Log.d("Drync", "Error initializing the CacheDirectory. - "
					+ e.getMessage());
		}

		setContentView(R.layout.splash);
		splash = (RelativeLayout) findViewById(R.id.splashscreen);

		PackageManager pm = this.getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo("com.drync.android", 0);
			TextView version = (TextView)findViewById(R.id.version);
			if ((version != null) && (pi != null))
			{
				if (isFree)
					version.setText(pi.versionName);
				else
					version.setText(pi.versionName);
			}
			
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		String register = null;
		if (hasConnectivity())
		{
			register = DryncProvider.getInstance().startupPost(deviceId);

			final String threadDeviceId = deviceId;
			Thread t = new Thread() {
				public void run() {
					
					try {
						DryncProvider.getInstance()
						.getCorks(Drync.this, threadDeviceId);
						DryncProvider.getInstance().myAcctGet(threadDeviceId);
					} catch (DryncHostException e) {
						Log.e("Drync", "DryncHostException on Startup", e);
					} catch (DryncXmlParseExeption e) {
						Log.e("Drync", "DryncXmlParseException on Startup", e);
					}
				}
			};
			t.start();
		}

		registerTxt = register;

		// Restore preferences
		SharedPreferences settings = getSharedPreferences(
				DryncUtils.PREFS_NAME, 0);
		boolean showIntro = settings.getBoolean(DryncUtils.SHOW_INTRO_PREF,
				true);
		mShowIntro = showIntro;

		mShowReg = register != null && (!register.equals(""));

		Message msg = new Message();
		if (mShowIntro)
			msg.what = STOPSPLASH;
		else if (mShowReg)
			msg.what = REGISTER;
		else
			msg.what = STARTMAIN;

		splashHandler.sendMessageDelayed(msg, SPLASHTIME);
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);

		// Save user preferences. We need an Editor object to
		// make changes. All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(
				DryncUtils.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(DryncUtils.SHOW_INTRO_PREF, mShowIntro);

		// Don't forget to commit your edits!!!
		editor.commit();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if ((register != null)
					&& (register.getVisibility() == View.VISIBLE)) {
				regWebView.goBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private class RegisterWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("close:")) {
				if (Drync.register != null) {
					Message msg = new Message();
					msg.what = Drync.STARTMAIN;
					Drync.this.splashHandler.sendMessage(msg);
				}
			}
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			// TODO Auto-generated method stub
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			CookieSyncManager.getInstance().sync();

			/*if (url.endsWith("#___1__")) {
				Toast acctCreated = Toast.makeText(Drync.this,
						"Your account has been created.", Toast.LENGTH_LONG);
				acctCreated.show();

				Thread thread = new Thread() {

					@Override
					public void run() {
						super.run();
						// redo this to reset cookies.
						DryncProvider.getInstance().myAcctGet(
								DryncUtils.getDeviceId(Drync.this
										.getContentResolver(), Drync.this));
					}
				};

				thread.start();*/

				/*Message msg = new Message();
				msg.what = Drync.STARTMAIN;
				Drync.this.splashHandler.sendMessage(msg);*/
			//}
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

	@Override
	protected void onResume() {
		super.onResume();

		CookieStore cookieStore = DryncUtils.getCookieStore();
		if (cookieStore != null) {
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeSessionCookie();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie cookie : cookies) {
				StringBuilder cookieUrl = new StringBuilder("http://");
				cookieUrl.append(cookie.getDomain()).append("/");
				StringBuilder cookieString = new StringBuilder();
				cookieString.append(cookie.getName()).append("=").append(
						cookie.getValue()).append("; domain=").append(
						cookie.getDomain());

				cookieManager.setCookie(cookieUrl.toString(), cookieString
						.toString());
				CookieSyncManager.getInstance().sync();
			}
		}
		CookieSyncManager.getInstance().startSync();
	}

	@Override
	protected void onPause() {
		super.onPause();
		CookieSyncManager.getInstance().stopSync();
	}

	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, DryncUtils.getDryncFlurryCode());

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

}