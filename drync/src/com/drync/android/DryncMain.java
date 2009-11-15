package com.drync.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.drync.android.objects.Bottle;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;
import android.text.TextUtils;


public class DryncMain extends Activity {

	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Bottle> mResults = null;
	private ProgressDialog progressDlg = null;
	private String deviceId;
	WineAdapter mAdapter; 


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

	private void launchBottle(Bottle bottle) {
		LinearLayout lview = (LinearLayout) this.findViewById(R.id.searchview);
		Intent next = new Intent();
		next.setClass(this, WineActivity.class);
		next.putExtra("bottle", bottle);
		startActivity(next);
	}


	class WineAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		private final List<Bottle> mWines;
		private final LayoutInflater mInflater;
		private final Drawable defaultIcon;

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
			bindView(view, mWines.get(position));
			return view;
		}
		
		private View createView(ViewGroup parent) {
			View wineItem = mInflater.inflate(
					R.layout.wineitem, parent, false);
			return wineItem;
		}

		private void bindView(View view, Bottle wine) {
			ImageView wineThumb = (ImageView) view.findViewById(R.id.wineThumb);
			if (wineThumb != null)
			{
				if (wine.getLabel_thumb() != null)
				{
					Drawable drawable = ImageOperations(DryncMain.this, wine.getLabel_thumb());
					wineThumb.setImageDrawable(drawable);
				}
				else
				{
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
}

