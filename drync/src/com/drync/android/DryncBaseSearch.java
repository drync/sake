/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 * otherwise...
 * 
 * @author Michael Brindamour
 * 
 */
package com.drync.android;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ClearableSearch;
import android.widget.ClearableSearch.OnCommitListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.WineItemRelativeLayout;

import com.drync.android.helpers.Result;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Review;
import com.drync.android.ui.RemoteImageView;
import com.drync.android.widget.CustomAutoCompleteTextView;
import com.drync.android.SearchTermDbAdapter;
import com.drync.android.widget.CustomArrayAdapter;

public class DryncBaseSearch extends DryncBaseActivity {
	
	public static final String LOG_IDENTIFIER = "DryncBaseSearch";

	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Bottle> mResults = null;

	private ProgressDialog progressDlg = null;
	private String deviceId;
	WineAdapter mAdapter; 
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
	boolean displaySearch = true;
	boolean displayTopWinesBtns = false;
	
	int lastSelectedTopWine = -1;
	
	LinearLayout searchView;
	ArrayList<String> dataset = new ArrayList<String>();
	CustomArrayAdapter<String> autocompleteadapter;
	CustomAutoCompleteTextView searchEntry;
	SearchTermDbAdapter searchAdapter = null;
	ScrollView detailView;
	ScrollView reviewView;
	ScrollView addView;
	
	boolean rebuildDetail = false;
	boolean rebuildReviews = false;
	boolean rebuildAddToCellar = false;
	boolean buildOnceAddToCellar = true;
	
	Drawable defaultIcon = null;
	
	String searchTerm = "";

	final Runnable mUpdateResults = new Runnable()
	{
		public void run()
		{
			updateResultsInUi();
			if ((progressDlg != null) && (progressDlg.isShowing()))
			{
				try
				{
					progressDlg.dismiss();
				}
				catch (IllegalArgumentException e)
				{
					Log.e("PROGDLG_ERROR", "Progress Dialog not attached.");
				}
				
				if (searchEntry != null)
				{
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(searchEntry.getWindowToken(), 0);
				}
			}
		}
	};
	
	
	private void updateResultsInUi() {

		// Back in the UI thread -- update our UI elements based on the data in mResults
		if (mList == null)
		{
			LinearLayout listholder = (LinearLayout)findViewById(R.id.listholder);
			mList = new ListView(DryncBaseSearch.this.getBaseContext());
			mList.setCacheColorHint(0);
			
			listholder.addView(mList);
		}
		
		if (mAdapter == null)
		{
			try
			{
				Collections.sort(mResults, new BottleComparator());
			}
			catch (NullPointerException e)
			{
				Log.e("DRYNCBASESEARCH", "NPE Caught", e);
			}
			
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
			if (mAdapter.mWines == null)
				mAdapter.mWines = new ArrayList<Bottle>();
			
			mAdapter.mWines.clear();
			mAdapter.viewHash.clear();
			if (mResults.size() <= 0)
			{
				 new AlertDialog.Builder(this)
			      .setMessage(getResources().getString(R.string.noresults) + "\n\n" +
			    		  getResources().getString(R.string.noresults2))
			    		  .setNegativeButton("OK", new DialogInterface.OnClickListener() {
			    	           public void onClick(DialogInterface dialog, int id) {
			    	                dialog.cancel();
			    	           }})
			      .show();
			}
			else
			{
				mAdapter.mWines.addAll(mResults);
			}
			
		}

		mAdapter.notifyDataSetChanged();
		
	}

	SharedPreferences settings;
	public static boolean bail = false;
	public static Thread longToastThread = null;
	public static Toast instToast = null;
	
	@Override
	public void onStop() {
		super.onStop();
		if (searchAdapter != null)
		{
			searchAdapter.close();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchview);
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		
		String lastQuery = settings.getString(DryncUtils.LAST_QUERY_PREF, null);
		searchTerm = lastQuery;
		
		searchAdapter = new SearchTermDbAdapter(this);
		searchAdapter.open();
		
		// prime the pump, as it were.
		searchAdapter.search("merlot");
		
		Bundle extras = getIntent().getExtras();
		this.displaySearch = extras != null ? extras.getBoolean("displaySearch") : true;
		this.displayTopWinesBtns = extras != null ? extras.getBoolean("displayTopWinesBtns") : false;
		
		startCellarUpdateThread();
		
		initializeAds();
		
		searchView = (LinearLayout) this.findViewById(R.id.searchview);
		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		
		final ClearableSearch searchholder = (ClearableSearch) findViewById(R.id.clrsearch);
		
		searchEntry = (CustomAutoCompleteTextView)findViewById(R.id.searchentry);
		
		autocompleteadapter = new CustomArrayAdapter<String>(this, R.layout.cust_auto_list_item, dataset);
		searchEntry.addTextChangedListener(textChecker);
		searchEntry.setAdapter(autocompleteadapter);
		autocompleteadapter.setNotifyOnChange(true);
		
		searchholder.setCommitOnClear(false);
		
		final LinearLayout topWinesBtnHolder = (LinearLayout) findViewById(R.id.topwinesbuttons);
		
		if (! displaySearch)
		{
			searchholder.setVisibility(View.GONE);         
		}
		
		if (! displayTopWinesBtns)
		{
			topWinesBtnHolder.setVisibility(View.GONE);
		}	
		else
		{
			topWinesBtnHolder.setVisibility(View.VISIBLE);
			
			final Button popButton = (Button)findViewById(R.id.popularBtn);
			final Button mwButton = (Button)findViewById(R.id.mostWantedBtn);
			final Button featButton = (Button)findViewById(R.id.featuredBtn);
			
			popButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncBaseSearch.this.startTopWineQueryOperation(DryncProvider.TOP_POPULAR);
					DryncBaseSearch.this.lastSelectedTopWine = DryncProvider.TOP_POPULAR;
					
					detailSelectedTopWineButton(popButton, featButton, mwButton);
					
				}});
			
			featButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncBaseSearch.this.startTopWineQueryOperation(DryncProvider.TOP_FEATURED);
					DryncBaseSearch.this.lastSelectedTopWine = DryncProvider.TOP_FEATURED;
					
					detailSelectedTopWineButton(popButton, featButton, mwButton);
				}});
			
			mwButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncBaseSearch.this.startTopWineQueryOperation(DryncProvider.TOP_WANTED);
					DryncBaseSearch.this.lastSelectedTopWine = DryncProvider.TOP_WANTED;
					
					detailSelectedTopWineButton(popButton, featButton, mwButton);
					
				}});
			
			if (DryncBaseSearch.this.lastSelectedTopWine == -1)
				popButton.performClick();
		}
		
		if (lastQuery != null)
		{
			searchholder.setText(lastQuery);
		}
		
		searchholder.setOnCommitListener(new OnCommitListener(){

			public boolean onCommit(View arg0, String text) {
				String searchterm = searchholder.getEditableText().toString();
				
				DryncBaseSearch.this.searchTerm = searchTerm;

				DryncBaseSearch.this.startQueryOperation(searchterm);
				return true;
			}});
		
		if (displaySearch && !bail)
		{
			LayoutInflater inflater = getLayoutInflater();
			
			View tstLayout = inflater.inflate(R.layout.searchinstructions,
					(ViewGroup) findViewById(R.id.search_toast_layout));
			
			instToast = new Toast(getApplicationContext()) {

			};
			
			int yOffset = 0;
			if (getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_PORTRAIT) { 
				yOffset = -40;
			}
			instToast.setGravity(Gravity.CENTER_VERTICAL, 0, yOffset);
			instToast.setDuration(10);
			instToast.setView(tstLayout);
			fireLongToast(instToast);
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if (searchAdapter == null)
		{
			searchAdapter = new SearchTermDbAdapter(this);
			searchAdapter.open();
		}
	}

	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (searchAdapter == null)
		{
			searchAdapter = new SearchTermDbAdapter(this);
		}
		searchAdapter.open();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mResults = savedInstanceState.getParcelableArrayList("mResults");
		updateResultsInUi();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("mResults", (ArrayList<Bottle>) mResults);
		
	}

	private void fireLongToast(Toast toastToShow) {

		final Toast toast = toastToShow;
		longToastThread = new DryncThread() {
			public void run() {
				int count = 0;
				// do not reset 'bail'... if it's been shown, let it be.
				try {
					while (!bail && count < 6) {
						toast.show();
						sleep(1850);
						count++;

						// do some logic that breaks out of the while loop
					}
				} catch (Exception e) {
					Log.e("LongToast", "", e);
				}
			}
		};
		longToastThread.start();
	}

	private void launchBottle(Bottle bottle) {
		Intent twIntent = new Intent(this, DryncDetail.class);
		twIntent.putExtra("bottle", bottle);
		if (this.displayTopWinesBtns)
			twIntent.putExtra("launchedFromTopWines", true);
		startActivity(twIntent);  
	}
	
	class WineReviewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		public Bottle bottle;
		private final LayoutInflater mInflater;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineReviewAdapter(Bottle wine) {
			bottle = wine;
			mInflater = (LayoutInflater) DryncBaseSearch.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if (bottle == null)
				return 0;
			
			return bottle.getReviewCount();
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
			
			Review review = bottle.getReview(position);
			bindView(view, review);
			
			return view;
		}
		
		private View createView(ViewGroup parent) {
			View reviewItem = mInflater.inflate(
					R.layout.reviewlistitem, parent, false);
			
			return reviewItem;
		}

		private void bindView(View reviewItem, Review review) {
			TextView reviewText = (TextView) reviewItem.findViewById(R.id.revText);
		/*	WebView reviewWeb = (WebView) reviewItem.findViewById(R.id.reviewWeb);
			TextView readReviewTxt = (TextView) reviewItem.findViewById(R.id.readreviewtext);
			View line = (View) reviewItem.findViewById(R.id.line);*/

			if (review.getText() != null && review.getText().contains("href")) // contains html
			{
				//reviewWeb.loadData(review.getText(), "text/html", "utf-8");
				reviewText.setText("");
				reviewText.setVisibility(View.INVISIBLE);
				//reviewWeb.setVisibility(View.VISIBLE);
				//readReviewTxt.setVisibility(View.INVISIBLE);
				//line.setVisibility(View.INVISIBLE);
			}
			else
			{
				//if (reviewWeb != null)
				//	reviewWeb.clearView();
				
				reviewText.setText(review.getText());
				//reviewText.setVisibility(View.VISIBLE);
				//reviewWeb.setVisibility(View.INVISIBLE);
			}
			

			if ((review.getUrl() == null) || (review.getUrl().equals("")))
			{
				//readReviewTxt.setTextColor(Color.GRAY);
			}
			else
			{
				//readReviewTxt.setTextColor(Color.BLACK);
			}

			/*final Review fReview = review;
			readReviewTxt.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					if ((fReview.getUrl() == null) || (fReview.getUrl().equals("")))
					{
						Toast noReviewTst = Toast.makeText(DryncMain.this, "There is no extended review for this item.", Toast.LENGTH_LONG);
						noReviewTst.show();
						return;
					}

					Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fReview.getUrl()));
					startActivity(myIntent);
				}});*/
			
		}

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
		}
	}



	class WineAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		private List<Bottle> mWines;
		private final LayoutInflater mInflater;
		private final Drawable defaultIcon;
		boolean mDone = false;
		boolean mFlinging = false;
		Hashtable<Long, View> viewHash = new Hashtable<Long, View>(); 
		
		public WineAdapter() {
			super();
			mWines = new ArrayList<Bottle>();
			mInflater = (LayoutInflater) DryncBaseSearch.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
		}

		public WineAdapter(List<Bottle> wines) {
			mWines = wines;
			mInflater = (LayoutInflater) DryncBaseSearch.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
		}
		
		public int getCount() {
			if (mWines != null)
				return mWines.size();
			else
				return 0;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}
		
		class ViewHolder {
            TextView name;
            TextView rating;
            TextView price;
            TextView review;
            RemoteImageView icon;
        }

		public View getView(int position, View convertView, ViewGroup parent) {		
			Bottle wine = mWines.get(position);
			View view = (convertView != null) ?  convertView :
				createView(parent);
			
			bindView(view, wine);

			if (view != null)
			{
				RemoteImageView wineThumb = ((ViewHolder)view.getTag()).icon; //(RemoteImageView) view.findViewById(R.id.wineThumb);
				if (wineThumb != null && !mFlinging)
				{

					if (! wineThumb.isUseDefaultOnly() && ! wineThumb.isLoaded())
						wineThumb.loadImage();
				}
			}

			return view;
		}
		
		private View createView(ViewGroup parent) {
			View wineItem = mInflater.inflate(
					R.layout.wineitem, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) wineItem.findViewById(R.id.wineName);
			holder.icon = (RemoteImageView) wineItem.findViewById(R.id.wineThumb);
			holder.price = (TextView) wineItem.findViewById(R.id.priceValue);
			holder.rating = (TextView) wineItem.findViewById(R.id.ratingValue);
			holder.review = (TextView) wineItem.findViewById(R.id.reviewValue);
			
			wineItem.setTag(holder);
			
			return wineItem;
		}

		private void bindView(View view, Bottle wine) {
			WineItemRelativeLayout wiv = (WineItemRelativeLayout) view;
			ViewHolder holder = (ViewHolder)view.getTag();
			wiv.setBottle(wine);
			RemoteImageView wineThumb = holder.icon; //(RemoteImageView) view.findViewById(R.id.wineThumb);
			if (wineThumb != null  && !mFlinging )
			{
				if (wine.getLabel_thumb() != null)
				{
					wineThumb.setLocalURI(DryncUtils.getCacheFileName(DryncBaseSearch.this.getBaseContext(), wine.getLabel_thumb()));
					wineThumb.setRemoteURI(wine.getLabel_thumb());
					wineThumb.setImageDrawable(defaultIcon);
					wineThumb.setUseDefaultOnly(false);
				}
				else
				{
					wineThumb.setUseDefaultOnly(true);
					wineThumb.setImageDrawable(defaultIcon);
				}
			}
			
			TextView wineNameText = holder.name; //(TextView) view.findViewById(R.id.wineName);
			wineNameText.setText(wine.getName());
			
			TextView priceText = holder.price; //(TextView) view.findViewById(R.id.priceValue);
			priceText.setText(wine.getPrice());
			
			TextView ratingText = holder.rating; //(TextView) view.findViewById(R.id.ratingValue);
			ratingText.setText(wine.getRating());
			
			TextView reviewText = holder.review; //(TextView) view.findViewById(R.id.reviewValue);
			reviewText.setText("" + wine.getReviewCount());
			
		}

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
		}

		public Object fetch(String address) throws MalformedURLException,IOException {
			URL url = new URL(address);
			try
			{
				Object content = url.getContent();
				return content;
			}
			catch (IOException e)
			{
				return null;
			}
		}

	}

	protected void startQueryOperation(String query)
	{
		final String curQuery = query;
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(DryncUtils.LAST_QUERY_PREF, query);
		editor.commit();
		
		bail = true;
		
		try {
			if (longToastThread != null)
			{
				longToastThread.join(0);
			}
			if (instToast != null)
				instToast.cancel();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (query.toLowerCase().startsWith("drync presto"))
		{  // this is a special command, just do it, don't search.
			
			if (query.toLowerCase().startsWith("drync presto device")) // switching the device id
			{
				// get the new one
				String newDeviceId = query.substring(query.trim().lastIndexOf(" "));
				
				DryncUtils.setNewDeviceId(getContentResolver(), this, newDeviceId);
				
				this.doStartupFetching();
				
				Toast devidtst = Toast.makeText(this, "DeviceId Changed", Toast.LENGTH_LONG);
				devidtst.show();
			}
			else if (query.toLowerCase().startsWith("drync presto reset"))
			{
				DryncUtils.setNewDeviceId(getContentResolver(), this, null);
				
				this.doStartupFetching();
				
				Toast reset = Toast.makeText(this, "Reset Triggered", Toast.LENGTH_LONG);
				reset.show();
			}
			else if (query.toLowerCase().startsWith("drync presto refresh cellar"))
			{
				this.doStartupFetching();
				
				Toast refresh = Toast.makeText(this, "Refresh Triggered", Toast.LENGTH_LONG);
				refresh.show();
			}
			
			return;
		}
		
		if (!hasConnectivity())
		{
			new AlertDialog.Builder(this)
		      .setMessage(getResources().getString(R.string.nonetsearch) + "\n\n" +
		    		  getResources().getString(R.string.nonetsearch2))
		    		  .setNegativeButton("OK", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	                dialog.cancel();
		    	           }})
		      .show();
		}
		else
		{
			progressDlg =  new ProgressDialog(DryncBaseSearch.this);
			progressDlg.setTitle("Dryncing...");
			progressDlg.setMessage("Retrieving wines...");
			progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDlg.show();
			
			Thread t = new DryncThread()
			{
				public void run() {
					try {
						mResults = DryncProvider.getInstance().getMatches(deviceId, URLEncoder.encode(curQuery, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						Log.e("DryncBaseSearch", "Exception encoding string " + curQuery, e);
					}
					mHandler.post(mUpdateResults);
				}
			};
			t.start();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		bail = true;
		if (instToast != null)
			instToast.cancel();
		
	}

	protected void startTopWineQueryOperation(final int type)
	{
		String typestr = "popular";
		String captypestr = "Popular";
		if (type == DryncProvider.TOP_POPULAR)
		{
			typestr = "popular";
			captypestr = "Popular";
		}
		else if (type == DryncProvider.TOP_FEATURED)
		{
			typestr = "featured";
			captypestr = "Featured";
		}
		else if (type == DryncProvider.TOP_WANTED)
		{
			typestr = "most wanted";
			captypestr = "Most Wanted";
		}
		
		if (!hasConnectivity())
		{
			new AlertDialog.Builder(this)
			.setMessage(getResources().getString(R.string.nonet_twsearch) + " " + captypestr + " wines.\n\n" +
					getResources().getString(R.string.nonetsearch2))
					.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}})
						.show();
		}
		else
		{
			
			
			progressDlg =  new ProgressDialog(DryncBaseSearch.this);
			progressDlg.setTitle("Dryncing...");
			progressDlg.setMessage("Retrieving " + typestr + " wines...");
			progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDlg.show();
			
			Thread t = new DryncThread()
			{
				public void run() {
					mResults = DryncProvider.getInstance().getTopWines(deviceId, type);
					mHandler.post(mUpdateResults);
				}
			};
			t.start();
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	private void detailSelectedTopWineButton(Button popButton, Button featButton, Button mwButton)
	{
		if (this.lastSelectedTopWine == DryncProvider.TOP_POPULAR)
		{
			popButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			popButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}
		
		if (this.lastSelectedTopWine == DryncProvider.TOP_FEATURED)
		{
			featButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			featButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}
		
		if (this.lastSelectedTopWine == DryncProvider.TOP_WANTED)
		{
			mwButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			mwButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}		
	}
	
	private synchronized void runUploadImagesReaper(List<Cork> corks)
	{
		final List<Cork> finalCorks = corks;
		final Runnable uploadImagesReaper = new Runnable()
		{
			public void run()
			{
				File uploaddir = null;
				uploaddir = new File(DryncUtils.getCacheDir(DryncBaseSearch.this) + "uploadimages/");
				
				if (uploaddir != null && uploaddir.exists())
				{
					File[] filearray = uploaddir.listFiles();
					for (File file : filearray)
					{
						boolean bFound = false;
						
						String filename = file.getName();
						for (Cork cork : finalCorks)
						{
							if (cork.getLocalImageResourceOnly().contains(filename))
							{
								bFound = true;
								break;
							}
						}
						
						if (!bFound && (System.currentTimeMillis() - file.lastModified()) > 
								1000*60*60*24)
							file.delete();
					}	
				}
			}
		};
		uploadImagesReaper.run();
	}
	
	private void startCellarUpdateThread()
	{
		ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
		
		final Runnable cellarUpdateThread = new Runnable()
		{
			public void run()
			{
				DryncDbAdapter dbAdapter = new DryncDbAdapter(DryncBaseSearch.this);
				try
				{
					if (hasConnectivity())
					{
						dbAdapter.open();

						List<Cork> corks = dbAdapter.getAllCorksNeedingUpdates();

						for (Cork cork : corks)
						{
							boolean postSuccess = false;

							// re-try post.
							if (cork.getUpdateType() == Cork.UPDATE_TYPE_INSERT)
							{
								try
								{
									Result<Cork> postresult = DryncProvider.postCreateOrUpdate(DryncBaseSearch.this, cork, deviceId, false);
									postSuccess = postresult.isResult();
								}
								catch(DryncFreeCellarExceededException e)
								{
									// intentionally ignore.
								}
							}
							else if (cork.getUpdateType() == Cork.UPDATE_TYPE_DELETE)
							{
								postSuccess = DryncProvider.postDelete(cork, deviceId);
							}
							else if (cork.getUpdateType() == Cork.UPDATE_TYPE_UPDATE)
							{
								Result<Cork> result = DryncProvider.postUpdate(cork, deviceId);
								postSuccess = result.isResult();
								
								if (result.getContents().size() > 0)
									cork = result.getContents().get(0);
							}
							else if (cork.getUpdateType() == Cork.UPDATE_TYPE_NONE)
							{
								postSuccess = true;  // just fix it.
							}

							if (postSuccess) // set 'needsUpdate' to false(0) and updateType to 0
							{
								if (cork.getUpdateType() == Cork.UPDATE_TYPE_DELETE)
								{
									dbAdapter.deleteCork(cork.get_id());
								}
								else
								{
									cork.setNeedsServerUpdate(false);
									cork.setUpdateType(Cork.UPDATE_TYPE_NONE);

									dbAdapter.updateCork(cork);
								}
							}

						}
						
						runUploadImagesReaper(corks);
						//	Arraylist<Cork> getAllCorksNeedingUpdates()
					}
					else
					{
						return;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					dbAdapter.close();
				}
			}
		};
				
		ex.scheduleAtFixedRate(cellarUpdateThread, 10, 60, TimeUnit.SECONDS);
	}
	
	@Override
	public String getGoogleAdSenseKeywords() {
		if ((searchTerm == null) || (searchTerm.equals("")))
			return super.getGoogleAdSenseKeywords();
		else
			return searchTerm;
	}

	@Override
	public int getMenuItemToSkip() {
		return SEARCH_ID;
	}
	
	final TextWatcher textChecker = new TextWatcher() {
		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) 
		{
			String sact = searchEntry.getEditableText().toString();
			// get just last word typed:
			if (sact.lastIndexOf(" ") > 0)
			{
				sact = sact.substring(sact.lastIndexOf(" ")).trim();
			}
			
			if (sact.length() < 1)
				return;

			List<String> results = searchAdapter.search(sact.toString().toLowerCase());

			DryncBaseSearch.this.dataset.clear();
			autocompleteadapter.clear();
			Iterator<String> iter = results.iterator();
			while (iter.hasNext())
			{
				String curEntry = iter.next();
				autocompleteadapter.add(curEntry);
			}

			autocompleteadapter.notifyDataSetChanged();
		}
	};
}

