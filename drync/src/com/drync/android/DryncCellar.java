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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ClearableSearch;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TwoBtnClearableSearch;
import android.widget.ViewFlipper;
import android.widget.WineItemRelativeLayout;

import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;

public class DryncCellar extends DryncBaseActivity implements OnSortListener {

	@Override
	protected void doStartupFetching() {
		// skip this fetch in cellar view.
	}

	public static final String LOG_IDENTIFIER = "DryncCellar";
	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Cork> mResults = null;
	long resultsLastSetTimestamp = -1;
	int lastSelectedCellar = -1;

	private ProgressDialog progressDlg = null;
	CorkAdapter mAdapter; 
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
	public static final int CORKDETAIL_RESULT = 1;
	public static final int CELLAR_NEEDS_REFRESH = 5;
	
	boolean displayFilter = true;
	boolean displayCellarFilterBtns = false;
	
	LinearLayout cellarView;
	EditText searchEntry;
	ClearableSearch clrSearch;
	ScrollView detailView;
	ScrollView reviewView;
	ScrollView addView;
	
	boolean rebuildDetail = false;
	boolean rebuildReviews = false;
	boolean rebuildAddToCellar = false;
	boolean buildOnceAddToCellar = true;
	
	Drawable defaultIcon = null;
	
	private int curSortValue = CorkComparator.BY_NAME;

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
					Log.e("DRYNCCELLAR_ERROR", "Progress Dialog not attached.");
				}
				if (searchEntry != null)
				{
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(searchEntry.getWindowToken(), 0);
				}
			}
		}
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
        case CORKDETAIL_RESULT:
            // This is the standard resultCode that is sent back if the
            // activity crashed or didn't doesn't supply an explicit result.
            if (resultCode == RESULT_CANCELED){
                // do nothing.
            } 
            else if ( resultCode == CELLAR_NEEDS_REFRESH )
            {
            	super.doStartupFetching(false);  // wait for this to return.
            	doFilteredCellarQuery(DryncCellar.this.lastSelectedCellar, this.searchEntry.getText().toString());	
            }
           
        default:
            break;
    }
	}
	
	
	private void updateResultsInUi() {

		// Back in the UI thread -- update our UI elements based on the data in mResults
		if (mList == null)
		{
			LinearLayout listholder = (LinearLayout)findViewById(R.id.corklistholder);
			mList = new ListView(DryncCellar.this.getBaseContext());
			mList.setCacheColorHint(0);
			
			listholder.addView(mList);
			registerForContextMenu(mList);
		}
		
		if (mAdapter == null)
		{
			mAdapter = new CorkAdapter(this, mResults);
			mList.setAdapter(mAdapter);
			mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					Log.d("BottleClick", "Cork clicked at position: " + position);
					launchCork(mAdapter.mWines.get(position));
				}
				
			});
			
			//mList.setOnLongClickListener(l)
		}
		else
		{
			mAdapter.mWines.clear();
			mAdapter.viewHash.clear();
			Collections.sort(mResults, new CorkComparator(curSortValue));
			mAdapter.mWines.addAll(mResults);
			this.resultsLastSetTimestamp = System.currentTimeMillis();
		}

		mAdapter.notifyDataSetChanged();
		
	}

	protected void startQueryOperation(String query)
	{
		Log.d(LOG_IDENTIFIER, "Querying: '" + query + "'");
		
		startQueryOperation(DryncDbAdapter.FILTER_TYPE_NONE, query);
	}
	
	protected void startQueryOperation(int filterType, String query)
	{
		Log.d(LOG_IDENTIFIER, "Querying: '" + query + "' filter type: " + filterType);
		
		final String curQuery = query.replace("\'s", "");
		final int curFilterType = filterType;
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(DryncUtils.LAST_FILTER_PREF, query);
		editor.commit();
		
		Thread t = new DryncThread()
		{
			public void run() {
				DryncDbAdapter dbAdapter = new DryncDbAdapter(DryncCellar.this);
				dbAdapter.open();
				mResults = dbAdapter.getFilteredCorks(curFilterType, curQuery);
				Collections.sort(mResults, new CorkComparator(curSortValue));
				mHandler.post(mUpdateResults);
				dbAdapter.close();
			}
		};
		t.start();
	}
	
	SharedPreferences settings;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cellarview);
		
		Bundle extras = getIntent().getExtras();
		boolean needsrefresh =  (extras != null ? extras.getBoolean("needsrefresh", false) : false);
		
		if (needsrefresh)
		{
			super.doStartupFetching(false);  // wait for this to return.
		}
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		
		String lastFilter = settings.getString(DryncUtils.LAST_FILTER_PREF, null);
		
		curSortValue = DryncUtils.getCellarSortType(this);
		
		this.displayFilter = true; 
		this.displayCellarFilterBtns = true; 
		
		initializeAds();
		
		cellarView = (LinearLayout) this.findViewById(R.id.cellarview);
		
		final LinearLayout searchholder = (LinearLayout) findViewById(R.id.searchHolder);
		searchEntry = (EditText)findViewById(R.id.searchentry);
		clrSearch = (ClearableSearch)findViewById(R.id.clrsearch);
		clrSearch.setCurSort(curSortValue);
		clrSearch.setOnSortListener(this);
		final LinearLayout cellarFilterButtons = (LinearLayout) findViewById(R.id.cellarfilterbuttons);
		
		if (! displayFilter)
		{
			searchholder.setVisibility(View.GONE);         
		}
		
		if (! displayCellarFilterBtns)
		{
			cellarFilterButtons.setVisibility(View.GONE);
		}	
		else
		{
			cellarFilterButtons.setVisibility(View.VISIBLE);
			
			final TwoBtnClearableSearch searchControl = (TwoBtnClearableSearch) findViewById(R.id.clrsearch);
			final Button myWinesButton = (Button)findViewById(R.id.myWinesBtn);
			final Button iDrankButton = (Button)findViewById(R.id.iDrankBtn);
			final Button iOwnButton = (Button)findViewById(R.id.iOwnBtn);
			final Button iWantButton = (Button)findViewById(R.id.iWantBtn);
			
			myWinesButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					String searchterm = searchControl.getEditableText().toString();
					progressDlg =  new ProgressDialog(DryncCellar.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					lastSelectedCellar = DryncDbAdapter.FILTER_TYPE_NONE;
					startQueryOperation(DryncDbAdapter.FILTER_TYPE_NONE, searchterm);
					
					DryncCellar.this.detailSelectedCellarButton(myWinesButton, iOwnButton, iWantButton, iDrankButton);
				}});
			
			iOwnButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					String searchterm = searchControl.getEditableText().toString();
					progressDlg =  new ProgressDialog(DryncCellar.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					lastSelectedCellar = DryncDbAdapter.FILTER_TYPE_OWN;
					startQueryOperation(DryncDbAdapter.FILTER_TYPE_OWN, searchterm);
					
					DryncCellar.this.detailSelectedCellarButton(myWinesButton, iOwnButton, iWantButton, iDrankButton);
				}});
			
			iWantButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					String searchterm = searchControl.getEditableText().toString();
					progressDlg =  new ProgressDialog(DryncCellar.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					lastSelectedCellar = DryncDbAdapter.FILTER_TYPE_WANT;
					startQueryOperation(DryncDbAdapter.FILTER_TYPE_WANT, searchterm);
					
					DryncCellar.this.detailSelectedCellarButton(myWinesButton, iOwnButton, iWantButton, iDrankButton);
				}});
			
			iDrankButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					String searchterm = searchControl.getEditableText().toString();
					progressDlg =  new ProgressDialog(DryncCellar.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					lastSelectedCellar = DryncDbAdapter.FILTER_TYPE_DRANK;
					startQueryOperation(DryncDbAdapter.FILTER_TYPE_DRANK, searchterm);
					
					DryncCellar.this.detailSelectedCellarButton(myWinesButton, iOwnButton, iWantButton, iDrankButton);
				}});
			
			if (lastSelectedCellar == -1)
			{
				
				searchControl.setText(lastFilter);
				long lastSetTimestamp = -1;
				if (savedInstanceState != null)
					lastSetTimestamp = savedInstanceState.getLong("resultsLastSetTimestamp");
				
				if ((savedInstanceState == null) || (lastSetTimestamp < DryncUtils.getCellarLastUpdatedTimestamp()) || (! savedInstanceState.containsKey("mResults")))
					myWinesButton.performClick();
			}
		}
		final TwoBtnClearableSearch clearableSearchCtrl = (TwoBtnClearableSearch)findViewById(R.id.clrsearch);
		
		if (lastFilter != null)
		{
			clearableSearchCtrl.setText(lastFilter);
		}
		
		ImageButton addBtn = clearableSearchCtrl.getAddButton();
		
		addBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				Intent twIntent = new Intent(DryncCellar.this, DryncAddToCellar.class);
				twIntent.putExtra("bottle", new Bottle());
				startActivityForResult(twIntent, ADDTOCELLAR_RESULT);  
			}});
		
		clearableSearchCtrl.setOnCommitListener(new TwoBtnClearableSearch.OnCommitListener(){

			public boolean onCommit(View arg0, String text) {
				String searchterm = text;
				
				progressDlg =  new ProgressDialog(DryncCellar.this);
				progressDlg.setTitle("Dryncing...");
				progressDlg.setMessage("Retrieving wines...");
				progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDlg.show();
				DryncCellar.this.startQueryOperation(DryncCellar.this.lastSelectedCellar, searchterm);
				return true;
			}

			});
		/*searchfield.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
			            (keyCode == KeyEvent.KEYCODE_ENTER)) {
			        	String searchterm = searchfield.getText().toString();

						progressDlg =  new ProgressDialog(DryncCellar.this);
						progressDlg.setTitle("Dryncing...");
						progressDlg.setMessage("Retrieving wines...");
						progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						progressDlg.show();
						DryncCellar.this.startQueryOperation(searchterm);
						return true;
			        }
			        return false;
			}});*/
	}

	private void launchCork(Cork bottle) {
		Intent twIntent = new Intent(this, DryncCorkDetail.class);
		twIntent.putExtra("bottle", bottle);
		startActivityForResult(twIntent, CORKDETAIL_RESULT);  
	}
	
	class CorkAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		private final List<Cork> mWines;
		private final LayoutInflater mInflater;
		private final Drawable defaultIcon;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public ImageLoader imageLoader; 
		private Activity activity;
		
		Hashtable<String, View> viewHash = new Hashtable<String, View>(); 
		
		public CorkAdapter(Activity a, List<Cork> mResults) {
			mWines = mResults;
			activity = a;
			mInflater = (LayoutInflater) DryncCellar.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
			imageLoader=new ImageLoader(activity.getApplicationContext());
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
		
		class ViewHolder {
            TextView name;
            TextView notes;
            TextView year;
            TextView rating;
            ImageView icon;
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			Cork wine = mWines.get(position);
			View view = (convertView != null) ?  convertView :
				createView(parent);
			
			bindView(view, wine);
			
			return view;
		}
		
		private View createView(ViewGroup parent) {
			View corkItem = mInflater.inflate(
					R.layout.corkitem, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) corkItem.findViewById(R.id.wineName);
			
			holder.icon = (ImageView) corkItem.findViewById(R.id.corkThumb);
			holder.notes = (TextView) corkItem.findViewById(R.id.myNotesValue);
			holder.rating = (TextView) corkItem.findViewById(R.id.ratingValue);
			holder.year = (TextView) corkItem.findViewById(R.id.yearValue);
			
			corkItem.setTag(holder);
			
			return corkItem;
		}
 
		private void bindView(View view, Cork wine) {
			WineItemRelativeLayout wiv = (WineItemRelativeLayout) view;
			ViewHolder holder = (ViewHolder) view.getTag();
			wiv.setBottle(wine);
			ImageView corkThumb = holder.icon;
			TextView ratingVal = holder.rating;
			TextView yearVal = holder.year;
			TextView notesVal = holder.notes;
			
			if (view != null)
			{
				boolean
				skipRemainingThumbProcessing = false;
				if (wine.getLocalImageResourceOnly() != null)
				{
					Drawable d = Drawable.createFromPath(wine.getLocalImageResourceOnly());
					if (d != null) {
						corkThumb.setImageDrawable(d);
						skipRemainingThumbProcessing = true;
					}
					
				}

				if (!skipRemainingThumbProcessing)
				{
					if (wine.getCork_label() != null) {
						imageLoader.DisplayImage(wine.getCork_label(), corkThumb);
					}
					else if (wine.getLabel_thumb() != null) {
						imageLoader.DisplayImage(wine.getLabel_thumb(), corkThumb);
					}
					else {
						corkThumb.setImageDrawable(defaultIcon);
					}
				}
			}
			
			TextView wineNameText = holder.name; 
			wineNameText.setText(wine.getName());
			Log.d("DryncCellar", "Creating view for : " + wine.getName());
			Float corkRating = wine.getCork_rating();
			ratingVal.setText(((corkRating == null) || (corkRating == 0)) ? "NR" : "" + wine.getCork_rating());
			Integer corkYear = wine.getCork_year();
			String stryear = "" + (( (corkYear == null) || (corkYear == 0))? wine.getYear() : corkYear); 
			yearVal.setText(stryear);
			
			notesVal.setText(wine.getDescription());
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
	
	protected void startCellarOperation()
	{
		Thread t = new DryncThread()
		{
			public void run() {
				DryncDbAdapter dbAdapter = new DryncDbAdapter(DryncCellar.this);
				dbAdapter.open();
				mResults = dbAdapter.getAllCorks();
				
				mHandler.post(mUpdateResults);
				dbAdapter.close();
			}
		};
		t.start();
	}
	
	private void doFilteredCellarQuery(int filter, String query)
	{
		progressDlg =  new ProgressDialog(DryncCellar.this);
		progressDlg.setTitle("Dryncing...");
		progressDlg.setMessage("Retrieving your cellar...");
		progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDlg.show();
		DryncCellar.this.startQueryOperation(filter, query);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// we need to check for changes to twitter settings.
	/*	if (settings != null)
		{
			userTwitterUsername = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, null);
			String encryptedTwitterPw = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, null);
			if (encryptedTwitterPw != null)
				userTwitterPassword = DryncUtils.decryptTwitterPassword(encryptedTwitterPw);
		}*/
	}
	
	private void detailSelectedCellarButton(Button mainButton, Button ownButton, Button wantButton, Button drankButton)
	{
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_NONE)
		{
			mainButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			mainButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}
		
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_OWN)
		{
			ownButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			ownButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}
		
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_WANT)
		{
			wantButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			wantButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}		
		
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_DRANK)
		{
			drankButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshadepressed));
		}
		else
		{
			drankButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.hdrbtnshade));
		}		
	}
	
	private class CorkComparator extends BottleComparator<Cork>
	{
		
		@SuppressWarnings("unused")
		public CorkComparator() {
			super();
		}

		public CorkComparator(int primarySort) {
			super(primarySort);
		}
		
		@Override
		protected int doSort(Cork arg0, Cork arg1) {
			int SORT_TYPE = primarySort;
			if (SORT_TYPE == BY_VINTAGE) // descending
				return (((Integer)(arg0.getYearValue())).compareTo(((Integer)(arg1.getYearValue()))) * -1);
				
			else if (SORT_TYPE == BY_MY_RATING) // I think rating should be descending.
			{
				return (Float.compare(arg0.getCork_rating(), arg1.getCork_rating()) * -1);
			}
			else if (SORT_TYPE == BY_PRICE)
			{
				return (compareStringsAsFloats(arg0.getCork_price(), arg1.getCork_price()) * -1);
			}
			else if (SORT_TYPE == BY_ENTRY_DATE) // descending
			{
				// sample date format
				// Sat, 27 Nov 2010 21:50:31 -0500
				String datepattern = "EEE, d MMM yyyy HH:mm:ss Z";
				SimpleDateFormat sdf = new SimpleDateFormat(datepattern);
				if ((arg0 != null) && (arg1 != null))
				{
					Date arg0date = null;
					Date arg1date = null;
					try {
						arg0date = sdf.parse(arg0.getCork_created_at());
						arg1date = sdf.parse(arg1.getCork_created_at());
					} catch (ParseException e) {
						// nothing to do really.
					}

					if ((arg0date != null) && (arg1date != null))
						return (arg0date.compareTo(arg1date) * -1); // make descending
					else if (arg0date == null)
						return -1;
					else
						return 1;
				}
				else if ((arg0 == null) && (arg1 == null))
				{
					return 0;
				}
				else if (arg0 == null)
				{
					return -1;
				}
				else
				{
					return 1;
				}
			}
				
				// still here?
			return super.doSort(arg0, arg1);
		}		
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		long lastSetTimestamp = savedInstanceState.getLong("resultsLastSetTimestamp");
		
		if (lastSetTimestamp > DryncUtils.getCellarLastUpdatedTimestamp())
		{
			mResults = savedInstanceState.getParcelableArrayList("mResults");
			lastSelectedCellar = savedInstanceState.getInt("lastSelectedCellar");
			this.resultsLastSetTimestamp = savedInstanceState.getLong("resultsLastSetTimestamp");
			this.curSortValue = savedInstanceState.getInt("curSort");
			
			final Button myWinesButton = (Button)findViewById(R.id.myWinesBtn);
			final Button iDrankButton = (Button)findViewById(R.id.iDrankBtn);
			final Button iOwnButton = (Button)findViewById(R.id.iOwnBtn);
			final Button iWantButton = (Button)findViewById(R.id.iWantBtn);

			this.detailSelectedCellarButton(myWinesButton, iOwnButton, iWantButton, iDrankButton);

			updateResultsInUi();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("mResults", (ArrayList<Cork>) mResults);
		outState.putInt("lastSelectedCellar", lastSelectedCellar);
		outState.putLong("resultsLastSetTimestamp", this.resultsLastSetTimestamp);
		outState.putInt("curSort", this.curSortValue);
	}
	
	@Override
	public int getMenuItemToSkip() {
		return DryncBaseActivity.CELLAR_ID;
	}


	@Override
	public boolean onSortChanged(View arg0, int sortType) {
		if (arg0 == clrSearch)
		{
			curSortValue = sortType;
			DryncUtils.setCellarSortType(this, curSortValue);
			doFilteredCellarQuery(DryncCellar.this.lastSelectedCellar, this.searchEntry.getText().toString());	
		}
		
		return true;
	}
}

