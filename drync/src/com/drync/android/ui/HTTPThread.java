/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 */

package com.drync.android.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;

import com.drync.android.DryncUtils;
import com.flurry.android.FlurryAgent;

import android.os.Handler;
import android.util.Log;

public class HTTPThread extends Thread {
	public static final int STATUS_PENDING = 0;
	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_FINISHED = 2;

	private boolean mError = false;
	private Exception mException = null;
	private String mUrl;
	
	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String mUrl) {
		this.mUrl = mUrl;
	}

	private String mLocal;
	private int mStatus = STATUS_PENDING;
	private SoftReference<Handler> mHandler;

	public HTTPThread(String url, String local, Handler handler) {
		mUrl = url;
		mLocal = local;
		mHandler = new SoftReference<Handler>(handler);
	}

	@Override
	public void start() {
		if (getStatus() == STATUS_PENDING) {
			synchronized (this) {
				mStatus = STATUS_RUNNING;
			}
			super.start();
		}
	}

	public void run() {
		try {
			URL request = new URL(mUrl);
			InputStream is = (InputStream) request.getContent();
			
			FileOutputStream fos = new FileOutputStream(mLocal);
			try {
				byte[] buffer = new byte[4096];
				int l;
				while ((l = is.read(buffer)) != -1) {
					fos.write(buffer, 0, l);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null)
					is.close();
				
				fos.flush();
				fos.close();
			}
		} catch (FileNotFoundException e)
		{
			Log.d("HTTPThread", "Could not download remote image - file not found - use default: " + e.getMessage()); 
			mError = true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			mError = true;
		} catch (IOException e) {
			e.printStackTrace();
			mError = true;
		} catch (NullPointerException e)
		{
			FlurryAgent.onEvent("HTTPThread_NPE_HANDLED");
			Log.d("HTTPThread", "Could not download remote image - NPE on GuessContentFromStream - use default: " + e.getMessage()); 
			mError = true;
		}

		synchronized (this) {
			mStatus = STATUS_FINISHED;
		}
		Handler handler = getHandler();
		if (handler != null) {
			handler.sendEmptyMessage(STATUS_FINISHED);
		}
	}

	public int getStatus() {
		synchronized (this) {
			return mStatus;
		}
	}

	public boolean hasError() {
		return mError;
	}

	public Exception getException() {
		return mException;
	}

	public void setHandler(Handler handler) {
		mHandler = new SoftReference<Handler>(handler);
	}

	public Handler getHandler() {
		if (mHandler != null) {
			return mHandler.get();
		}
		return null;
	}

	@Override
	public long getId() {
		return mUrl.hashCode();
	}
}
