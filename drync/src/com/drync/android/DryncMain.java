package com.drync.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.drync.android.objects.Bottle;

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


public class DryncMain extends TabActivity {

	private TabHost mTabHost;
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
			//mList.setId(R.string.listview);
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
		
		mTabHost = getTabHost();
		mTabHost.addTab(mTabHost.newTabSpec("tab_search").setIndicator(
				getResources().getString(R.string.searchtab),
				getResources().getDrawable(R.drawable.tab_icon_search)).
				setContent(R.id.searchview));
		mTabHost.addTab(mTabHost.newTabSpec("tab_cellar").setIndicator(
				getResources().getString(R.string.cellartab),
				getResources().getDrawable(R.drawable.tab_icon_cellar)).
				setContent(R.id.textview2));
		mTabHost.addTab(mTabHost.newTabSpec("tab_quicknote").setIndicator(
				getResources().getString(R.string.quicknotestab),
				getResources().getDrawable(R.drawable.tab_icon_pencil)).
				setContent(R.id.textview3));
		mTabHost.addTab(mTabHost.newTabSpec("tab_topwine").setIndicator(
				getResources().getString(R.string.topwinestab),
				getResources().getDrawable(R.drawable.tab_icon_topwines)).
				setContent(R.id.textview4));
		mTabHost.addTab(mTabHost.newTabSpec("tab_settings").setIndicator(
				getResources().getString(R.string.settingstab),
				getResources().getDrawable(R.drawable.tab_icon_settings)).
				setContent(R.id.textview5));


		int n=mTabHost.getTabWidget().getChildCount();
		for (int i=0;i<n;i++)
		{
			ArrayList<View> views = mTabHost.getTabWidget().getChildAt(i).getTouchables();
			RelativeLayout relLayout = (RelativeLayout)views.get(0);
			TextView tv = (TextView)relLayout.getChildAt(1);
			tv.setPadding(0, 3, 0, 0);
			tv.setSingleLine(false);
			tv.setGravity(Gravity.CENTER);
			tv.setTextSize(10);
			tv.setLines(2);
		}
		mTabHost.setCurrentTab(0);

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
		/*LinearLayout lview = (LinearLayout) this.findViewById(R.id.searchview);
		Intent next = new Intent();
		next.setClass(this, WineActivity.class);
		next.putExtra("bottle", bottle);
		//startActivity(next);
		
		final TabHost tabHost = getTabHost();
		mTabHost.newTabSpec("tab_search").setContent(next);*/
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

		/*private TwoLineListItem createView(ViewGroup parent) {
			TwoLineListItem item = (TwoLineListItem) mInflater.inflate(
					android.R.layout.simple_list_item_2, parent, false);
			item.getText2().setSingleLine();
			item.getText2().setEllipsize(TextUtils.TruncateAt.END);
			return item;
		}*/
		
		private View createView(ViewGroup parent) {
			View wineItem = mInflater.inflate(
					R.layout.wineitem, parent, false);
			/*item.getText2().setSingleLine();
			item.getText2().setEllipsize(TextUtils.TruncateAt.END);*/
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

