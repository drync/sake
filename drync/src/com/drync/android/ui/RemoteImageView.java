/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 */
package com.drync.android.ui;

import java.io.File;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.drync.android.DryncUtils;
import com.drync.android.R;


public class RemoteImageView extends ImageView {
	private String mLocal;
	private String mRemote;
	private HTTPThread mThread = null;
	boolean useDefaultOnly = false;

	public boolean isUseDefaultOnly() {
		return useDefaultOnly;
	}

	public void setUseDefaultOnly(boolean useDefaultOnly) {
		this.useDefaultOnly = useDefaultOnly;
	}

	public RemoteImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setLocalURI(String local) {
		mLocal = local;
	}

	public void setRemoteURI(String uri) {
		if (uri.startsWith("http")) {
			mRemote = uri;
		}
	}

	public void loadImage() {
		if ((mRemote != null) && (!useDefaultOnly)) {
			if (mLocal == null) {
				mLocal = DryncUtils.CACHE_DIRECTORY + mRemote.hashCode() + ".jpg";
			}
			// check for the local file here instead of in the thread because
			// otherwise previously-cached files wouldn't be loaded until after
			// the remote ones have been downloaded.
			File local = new File(mLocal);
			if (local.exists()) {
				setFromLocal();
			} else {
				// we already have the local reference, so just make the parent
				// directories here instead of in the thread.
				local.getParentFile().mkdirs();
				queue();
			}
		}
	}

	@Override
	public void finalize() {
		if (mThread != null) {
			HTTPQueue queue = HTTPQueue.getInstance();
			queue.dequeue(mThread);
		}
	}

	private void queue() {
		if (mThread == null) {
			mThread = new HTTPThread(mRemote, mLocal, mHandler);
			HTTPQueue queue = HTTPQueue.getInstance();
			queue.enqueue(mThread, HTTPQueue.PRIORITY_HIGH);
		}
		setImageResource(R.drawable.icon);
	}

	private void setFromLocal() {
		mThread = null;
		Drawable d = Drawable.createFromPath(mLocal);
		if (d != null) {
			setImageDrawable(d);
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			setFromLocal();
		}
	};
}
