/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 */
package com.drync.android.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.drync.android.DryncUtils;
import com.drync.android.R;


public class RemoteImageView extends ImageView {
	private String mLocal;
	private String mRemote;
	private HTTPThread mThread = null;
	boolean useDefaultOnly = false;
	boolean loaded = false;
	boolean thumbnail = false;
	
	public boolean isThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(boolean thumbnail) {
		this.thumbnail = thumbnail;
	}

	public static final int CAMERA_PIC_REQUEST = 1337;
	
	boolean launchCameraOnClick = false;

	public boolean isLaunchCameraOnClick() {
		return launchCameraOnClick;
	}

	public void setLaunchCameraOnClick(Context ctx, boolean launchCameraOnClick) {
		this.launchCameraOnClick = launchCameraOnClick;
		
		if (launchCameraOnClick)
		{
			final Context clickContext = ctx;
			this.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					if (RemoteImageView.this.isLaunchCameraOnClick())
					{
						Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
						((Activity)clickContext).startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);  
					}
					
				}});
		}
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

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
		loaded = false;
		if ((uri != null) && (uri.startsWith("http"))) {
			mRemote = uri;
		}
	}

	public String getRemoteUri() {
		return mRemote;
	}

	public String saveNewImage(Bitmap bm)
	{
		Random rand = new Random(System.currentTimeMillis());
		int randint = rand.nextInt(10000);
		
		File tmpDir = new File(DryncUtils.getCacheDir(this.getContext()) + "uploadimages/");
		if (! tmpDir.exists())
			tmpDir.mkdirs();
		
		String newpath = DryncUtils.getCacheDir(this.getContext()) + "uploadimages/" +
				"corkimg_" + randint + ".jpg";

		try {
			FileOutputStream fos = new FileOutputStream(newpath);
			bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return newpath;

	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		
		if (this.isLaunchCameraOnClick())
		{
			Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_menu_camera_small);
			
			Rect r = new Rect(canvas.getWidth()/2 - canvas.getWidth()/4,
							  0, canvas.getWidth()/4, canvas.getHeight()/3);
			
			float centerx = canvas.getClipBounds().exactCenterX() - bMap.getWidth()/2;
			float centery = canvas.getClipBounds().bottom - (bMap.getHeight() + bMap.getHeight()/4);
			
			canvas.drawBitmap(bMap, centerx, centery, new Paint());
		}
	}

	public void loadImage() {
		
		if ((mRemote != null) && (!useDefaultOnly)) {
			if (mLocal == null) {
				mLocal = DryncUtils.getCacheDir(this.getContext()) + mRemote.hashCode() + ".jpg";
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
		loaded = true;
	}

	private void queue() {
		if (mThread == null) {
			mThread = new HTTPThread(mRemote, mLocal, mHandler);
			HTTPQueue queue = HTTPQueue.getInstance();
			queue.enqueue(mThread, HTTPQueue.PRIORITY_HIGH);
		}
		// set default until the queued fetch returns
		setImageResource(R.drawable.bottlenoimage);
	}

	private void setFromLocal() {
		
		mThread = null;
		File file = new File(mLocal);
		Bitmap bm = decodeFile(file);
		Drawable d = new BitmapDrawable(bm);
		if (d != null) {
			setImageDrawable(d);
		}
		loaded = true;
	}

	//decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f){
	    try {
	        //Decode image size
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        BitmapFactory.decodeStream(new FileInputStream(f),null,o);

	        //The new size we want to scale to
	        final int REQUIRED_SIZE=50;

	        //Find the correct scale value. It should be the power of 2.
	        int width_tmp=o.outWidth, height_tmp=o.outHeight;
	        int scale=1;
	        while(true && thumbnail){
	            if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
	                break;
	            width_tmp/=2;
	            height_tmp/=2;
	            scale*=2;
	        }

	        //Decode with inSampleSize
	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize=scale;
	        return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
	    } catch (FileNotFoundException e) {}
	    return null;
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if ((mThread != null) && (mThread.getStatus() == HTTPThread.STATUS_FINISHED) && (mThread.hasError()))
			{
				RemoteImageView.this.setUseDefaultOnly(true);
			}
			else
				setFromLocal();
			
			RemoteImageView.this.loaded = true;
		}
	};
	
	public void setRemoteImage(String labelThumb, Drawable defaultIcon)
	{
		if (labelThumb != null && !labelThumb.equals(""))
		{
			this.setRemoteURI(labelThumb);
				this.setLocalURI(DryncUtils.getCacheFileName(this.getContext(), labelThumb));
				this.setImageDrawable(defaultIcon);
				this.setUseDefaultOnly(false);
				this.loadImage();
			}
			else
			{
				this.setUseDefaultOnly(true);
				this.setImageDrawable(defaultIcon);
			}
	}
}
