/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 * otherwise...
 * 
 * @author Michael Brindamour
 * 
 */
package com.drync.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.drync.android.objects.Bottle;
import com.drync.android.ui.DryncTabActivity;
import com.drync.android.ui.RemoteImageView;


public class DryncMain extends Activity {
	
	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Bottle> mResults = null;
	private Bottle mBottle = null;
	private ProgressDialog progressDlg = null;
	private String deviceId;
	WineAdapter mAdapter; 
	LayoutInflater mMainInflater;
	
	LinearLayout searchView;
	ScrollView detailView;
	
	Drawable defaultIcon = null;


	final Runnable mUpdateResults = new Runnable()
	{
		public void run()
		{
			updateResultsInUi();
			if (progressDlg != null)
				progressDlg.dismiss();
		}
	};
	
	private void updateResultsInUi() {

		// Back in the UI thread -- update our UI elements based on the data in mResults
		
		if (mList == null)
		{
			LinearLayout listholder = (LinearLayout)findViewById(R.id.listholder);
			mList = new ListView(DryncMain.this.getBaseContext());
			mList.setCacheColorHint(0);
			listholder.addView(mList);
		}
		
		if (mAdapter == null)
		{
			mAdapter = new WineAdapter(mResults);
			mList.setAdapter(mAdapter);
			mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					Log.d("BottleClick", "Bottle clicked at position: " + position);
					launchBottle(mAdapter.mWines.get(position));
				}
				
			});
		}
		else
		{
			mAdapter.mWines.clear();
			mAdapter.mWines.addAll(mResults);
		}

		mAdapter.notifyDataSetChanged();
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		searchView = (LinearLayout) this.findViewById(R.id.searchview);
		searchView.setVisibility(View.VISIBLE);
		detailView = (ScrollView) this.findViewById(R.id.detailview);
		detailView.setVisibility(View.INVISIBLE);
		DryncUtils.checkForLocalCacheArea();

		deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);

		final EditText searchfield = (EditText) findViewById(R.id.searchentry);
		searchfield.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) 
				{
					// Perform action on key press

					String searchterm = searchfield.getText().toString();

					progressDlg =  new ProgressDialog(DryncMain.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					DryncMain.this.startQueryOperation(searchterm);		            
					return true;
				}
				return false;
			}
		});

		LayoutInflater inflater = getLayoutInflater();
		View tstLayout = inflater.inflate(R.layout.searchinstructions,
		                               (ViewGroup) findViewById(R.id.search_toast_layout));
		
		Toast toast = new Toast(getApplicationContext()) {
			
		};
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(10);
		toast.setView(tstLayout);
		toast.show();
	}
	
	private TabHost getTabHost()
	{
		Activity obj = this;
		TabHost thost = null;
		while ((obj != null) && (! (obj instanceof DryncTabMain)))
		{
			obj = obj.getParent();
		}
		
		if (obj != null)
		{
			thost = (TabHost)((TabActivity)obj).getTabHost();
		}
		
		return null;
	}

	private void launchBottle(Bottle bottle) {
		mBottle = bottle;

		searchView.setVisibility(View.INVISIBLE);
		detailView.setVisibility(View.VISIBLE);

		TextView nameView = (TextView) findViewById(R.id.wineName);
		TextView titleView = (TextView) findViewById(R.id.detailTitle);
		TextView yearView = (TextView) findViewById(R.id.yearValue);
		TextView ratingView = (TextView) findViewById(R.id.avgRatingValue);
		TextView priceView = (TextView) findViewById(R.id.priceValue);
		TextView ratingCount = (TextView) findViewById(R.id.reviewCount);
		
		LinearLayout revListHolder = (LinearLayout)findViewById(R.id.reviewholder);
		// todo: optimize this to reuse views.
		revListHolder.removeAllViews();
		for (int i=0,n=mBottle.getReviewCount();i<n;i++)
		{
			if (mMainInflater == null)
			{
				mMainInflater = (LayoutInflater) DryncMain.this.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
			}
			
			View reviewItem = mMainInflater.inflate(
					R.layout.reviewitem, revListHolder, false);
			revListHolder.addView(reviewItem, i);
			// inflate view: 
		}
		/*if (mReviewList == null)
		{
			mReviewList = new ListView(DryncMain.this.getBaseContext());
			mReviewList.setCacheColorHint(0);
			revListHolder.addView(mReviewList);
		}
		
		if (mReviewAdapter == null)
		{
			mReviewAdapter = new WineReviewAdapter(mBottle);
			mReviewList.setAdapter(mReviewAdapter);
			mReviewList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					Log.d("ReviewClick", "Review clicked at position: " + position);
					//launchBottle(mAdapter.mWines.get(position));
				}
				
			});
		}
		else
		{
			mReviewAdapter.bottle = bottle;
		}

		mReviewAdapter.notifyDataSetChanged();*/
		
		nameView.setText(mBottle.getName());
		titleView.setText(mBottle.getName());
		int year = mBottle.getYear();
		yearView.setText("" + year);
		ratingView.setText(mBottle.getRating());
		priceView.setText(mBottle.getPrice());
		String reviewPlurality = ((mBottle.getReviewCount() <= 0) || (mBottle.getReviewCount() > 1)) ?
							" Reviews" : " Review";
		ratingCount.setText("" + mBottle.getReviewCount() + reviewPlurality);
		
		if (defaultIcon == null)
		{
			defaultIcon = getResources().getDrawable(R.drawable.icon);
		}
		
		RemoteImageView riv = (RemoteImageView) findViewById(R.id.wineThumb);
		if (riv != null)
		{
			String labelThumb = mBottle.getLabel_thumb();
			if (labelThumb != null && !labelThumb.equals(""))
			{
				riv.setRemoteURI(labelThumb);
				riv.setLocalURI(DryncUtils.getCacheFileName(labelThumb));
				riv.setImageDrawable(defaultIcon);
				riv.setUseDefaultOnly(false);
				riv.loadImage();
			}
			else
			{
				riv.setUseDefaultOnly(true);
				riv.setImageDrawable(defaultIcon);
			}
		}
		
		Button searchBtn = (Button) this.findViewById(R.id.searchBtn);
		searchBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				DryncMain.this.goToSearchView();				
			}});
		
	}

	class WineAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		private final List<Bottle> mWines;
		private final LayoutInflater mInflater;
		private final Drawable defaultIcon;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineAdapter(List<Bottle> wines) {
			mWines = wines;
			mInflater = (LayoutInflater) DryncMain.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			defaultIcon = getResources().getDrawable(R.drawable.icon);
		}

		public int getCount() {
			return mWines.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = (convertView != null) ? (View) convertView :
				createView(parent);
			boolean useLocalCache = DryncUtils.isUseLocalCache();
			Bottle wine = mWines.get(position);
			bindView(view, wine);
			
			if (view != null)
			{
				RemoteImageView wineThumb = (RemoteImageView) view.findViewById(R.id.wineThumb);
				if (wineThumb != null && !mFlinging && useLocalCache)
				{
					wineThumb.loadImage();
				}
			}
			
			return view;
		}
		
		private View createView(ViewGroup parent) {
			View wineItem = mInflater.inflate(
					R.layout.wineitem, parent, false);
			return wineItem;
		}

		private void bindView(View view, Bottle wine) {
			boolean useLocalCache = DryncUtils.isUseLocalCache();
			RemoteImageView wineThumb = (RemoteImageView) view.findViewById(R.id.wineThumb);
			if (wineThumb != null  && !mFlinging )
			{
				if (wine.getLabel_thumb() != null)
				{
					if (useLocalCache)
					{
						wineThumb.setLocalURI(DryncUtils.getCacheFileName(wine.getLabel_thumb()));
						wineThumb.setRemoteURI(wine.getLabel_thumb());
						wineThumb.setImageDrawable(defaultIcon);
						wineThumb.setUseDefaultOnly(false);
					}
					else
					{
						Drawable drawable = ImageOperations(DryncMain.this, wine.getLabel_thumb());
						wineThumb.setImageDrawable(drawable);
						wineThumb.setUseDefaultOnly(false);
					}
				}
				else
				{
					wineThumb.setUseDefaultOnly(true);
					wineThumb.setImageDrawable(defaultIcon);
				}
			}
			
			TextView wineNameText = (TextView) view.findViewById(R.id.wineName);
			wineNameText.setText(wine.getName());
			
			TextView priceText = (TextView) view.findViewById(R.id.priceValue);
			priceText.setText(wine.getPrice());
			
			TextView ratingText = (TextView) view.findViewById(R.id.ratingValue);
			ratingText.setText(wine.getRating());
			
			TextView reviewText = (TextView) view.findViewById(R.id.reviewValue);
			reviewText.setText("" + wine.getReviewCount());
			
		}

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
		}
		
		private Drawable ImageOperations(Context ctx, String url) {
			try {
				InputStream is = (InputStream) this.fetch(url);
				Drawable d = Drawable.createFromStream(is, "src");
				
				return d;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		public Object fetch(String address) throws MalformedURLException,IOException {
			URL url = new URL(address);
			Object content = url.getContent();
			return content;
		}

	}

	protected void startQueryOperation(String query)
	{
		final String curQuery = query;
		Thread t = new Thread()
		{
			public void run() {
				mResults = DryncProvider.getInstance().getMatches(deviceId, curQuery);
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			boolean retval = goToSearchView();
			
			if (retval)
				return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	private boolean goToSearchView()
	{
		if (detailView.getVisibility() == View.VISIBLE)
		{
			detailView.setVisibility(View.INVISIBLE);
			searchView.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}
	
	
}

