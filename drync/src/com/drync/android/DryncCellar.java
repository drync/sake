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
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TwoBtnClearableSearch;
import android.widget.ViewFlipper;
import android.widget.WineItemRelativeLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.ui.RemoteImageView;

public class DryncCellar extends DryncBaseActivity {

	public static final String LOG_IDENTIFIER = "DryncCellar";
	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Cork> mResults = null;
	int lastSelectedCellar = -1;

private ProgressDialog progressDlg = null;
	private String deviceId;
	CorkAdapter mAdapter; 
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
	public static final int CORKDETAIL_RESULT = 1;
	public static final int CELLAR_NEEDS_REFRESH = 5;
	private static final int EDIT_ID = 0;
	private static final int DELETE_ID = 1;
	
	boolean displayFilter = true;
	boolean displayCellarFilterBtns = false;
	
	private TableLayout mReviewTable;
	
	LinearLayout cellarView;
	ScrollView detailView;
	ScrollView reviewView;
	ScrollView addView;
	
	boolean rebuildDetail = false;
	boolean rebuildReviews = false;
	boolean rebuildAddToCellar = false;
	boolean buildOnceAddToCellar = true;
	
	Drawable defaultIcon = null;
	
	private String userTwitterUsername = null;
	private String userTwitterPassword = null;

	final Runnable mUpdateResults = new Runnable()
	{
		public void run()
		{
			updateResultsInUi();
			if (progressDlg != null)
				progressDlg.dismiss();
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
            	this.doCellarQuery();
            }
            else {
                //this.startDryncCellarActivity();
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
			Collections.sort(mResults, new CorkComparator());
			mAdapter = new CorkAdapter(mResults);
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
			mAdapter.mWines.addAll(mResults);
		}

		mAdapter.notifyDataSetChanged();
		
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ID, 0, "Edit");
		menu.add(0, DELETE_ID, 0,  "Delete");
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case EDIT_ID:
			Intent twIntent = new Intent(DryncCellar.this, DryncAddToCellar.class);
			twIntent.putExtra("cork", mAdapter.mWines.get(info.position));
			startActivityForResult(twIntent, ADDTOCELLAR_RESULT);  
		case DELETE_ID:
			deleteCork(mAdapter.mWines.get(info.position));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void deleteCork(Cork cork) {
		
		final DryncDbAdapter dbAdapter = new DryncDbAdapter(this);
		
		boolean postSuccess = DryncProvider.postDelete(cork, deviceId);
		if (!postSuccess)
		{
			// failed post, post later.
			
			cork.setNeedsServerUpdate(true);
			cork.setUpdateType(Cork.UPDATE_TYPE_DELETE);	
		}
		// persist to database.
		dbAdapter.open();
		boolean success = dbAdapter.deleteCork(cork.get_id());
		dbAdapter.close();
		
		DryncCellar.this.startCellarOperation();	
	}

	protected void startQueryOperation(String query)
	{
		Log.d(LOG_IDENTIFIER, "Querying: '" + query + "'");
		
		startQueryOperation(DryncDbAdapter.FILTER_TYPE_NONE, query);
	}
	
	protected void startQueryOperation(int filterType, String query)
	{
		Log.d(LOG_IDENTIFIER, "Querying: '" + query + "' filter type: " + filterType);
		
		final String curQuery = query;
		final int curFilterType = filterType;
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(DryncUtils.LAST_FILTER_PREF, query);
		editor.commit();
		
		Thread t = new Thread()
		{
			public void run() {
				DryncDbAdapter dbAdapter = new DryncDbAdapter(DryncCellar.this);
				dbAdapter.open();
				mResults = dbAdapter.getFilteredCorks(curFilterType, curQuery);
				
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
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		
		String lastFilter = settings.getString(DryncUtils.LAST_FILTER_PREF, null);
		
		Bundle extras = getIntent().getExtras();
		this.displayFilter = true; //extras != null ? extras.getBoolean("displayFilter") : true;
		this.displayCellarFilterBtns = true; //extras != null ? extras.getBoolean("displayCellarFilterBtns") : false;
		
		LayoutInflater inflater = getLayoutInflater();
		
		cellarView = (LinearLayout) this.findViewById(R.id.cellarview);
		
		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		final LinearLayout searchholder = (LinearLayout) findViewById(R.id.searchHolder);
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
					progressDlg.setMessage("Retrieving corks...");
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
					progressDlg.setMessage("Retrieving corks...");
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
					progressDlg.setMessage("Retrieving corks...");
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
					progressDlg.setMessage("Retrieving corks...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					lastSelectedCellar = DryncDbAdapter.FILTER_TYPE_DRANK;
					startQueryOperation(DryncDbAdapter.FILTER_TYPE_DRANK, searchterm);
					
					DryncCellar.this.detailSelectedCellarButton(myWinesButton, iOwnButton, iWantButton, iDrankButton);
				}});
			
			if (lastSelectedCellar == -1)
			{
				
				searchControl.setText(lastFilter);
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
				progressDlg.setMessage("Retrieving corks...");
				progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDlg.show();
				DryncCellar.this.startQueryOperation(searchterm);
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
						progressDlg.setMessage("Retrieving corks...");
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
		
		Hashtable<String, View> viewHash = new Hashtable<String, View>(); 
		
		public CorkAdapter(List<Cork> mResults) {
			mWines = mResults;
			mInflater = (LayoutInflater) DryncCellar.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
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
			Cork wine = mWines.get(position);
			View oldView = viewHash.get(wine.getCork_uuid());
			View view = (oldView != null) ?  oldView :
				createView(parent);
			
			viewHash.put(wine.getCork_uuid(), view);
			
			WineItemRelativeLayout wiv = (WineItemRelativeLayout) view;
			if ((wiv.getBottle() == null) || (wiv.getBottle() != wine))
			{
				bindView(view, wine);

				if (view != null)
				{
					RemoteImageView wineThumb = (RemoteImageView) view.findViewById(R.id.corkThumb);
					if (wineThumb != null && !mFlinging)
					{

						if (! wineThumb.isUseDefaultOnly() && ! wineThumb.isLoaded())
							wineThumb.loadImage();
					}
				}
			}
			
			return view;
		}
		
		private View createView(ViewGroup parent) {
			View corkItem = mInflater.inflate(
					R.layout.corkitem, parent, false);
			return corkItem;
		}
 
		private void bindView(View view, Cork wine) {
			WineItemRelativeLayout wiv = (WineItemRelativeLayout) view;
			wiv.setBottle(wine);
			RemoteImageView corkThumb = (RemoteImageView) view.findViewById(R.id.corkThumb);
			TextView ratingVal = (TextView) view.findViewById(R.id.ratingValue);
			TextView yearVal = (TextView) view.findViewById(R.id.yearValue);
			TextView notesVal = (TextView) view.findViewById(R.id.myNotesValue);
			
			if (corkThumb != null  && !mFlinging )
			{
				if (wine.getLabel_thumb() != null)
				{
					corkThumb.setLocalURI(DryncUtils.getCacheFileName(wine.getLabel_thumb()));
					corkThumb.setRemoteURI(wine.getLabel_thumb());
					corkThumb.setImageDrawable(defaultIcon);
					corkThumb.setUseDefaultOnly(false);
				}
				else
				{
					corkThumb.setUseDefaultOnly(true);
					corkThumb.setImageDrawable(defaultIcon);
				}
			}
			
			TextView wineNameText = (TextView) view.findViewById(R.id.wineName);
			wineNameText.setText(wine.getName());
			Float corkRating = wine.getCork_rating();
			ratingVal.setText(((corkRating == null) || (corkRating == 0)) ? "NR" : "" + wine.getCork_rating());
			Integer corkYear = wine.getCork_year();
			String stryear = "" + (( (corkYear == null) || (corkYear == 0))? wine.getYear() : corkYear); 
			yearVal.setText(stryear);
			
			notesVal.setText(wine.getDescription());
			
			/*TextView priceText = (TextView) view.findViewById(R.id.priceValue);
			priceText.setText(wine.getPrice());
			
			TextView ratingText = (TextView) view.findViewById(R.id.ratingValue);
			ratingText.setText(wine.getRating());
			
			TextView reviewText = (TextView) view.findViewById(R.id.reviewValue);
			reviewText.setText("" + wine.getReviewCount());*/
			
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
		Thread t = new Thread()
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
	
	private void doCellarQuery()
	{
		progressDlg =  new ProgressDialog(DryncCellar.this);
		progressDlg.setTitle("Dryncing...");
		progressDlg.setMessage("Retrieving your cellar...");
		progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDlg.show();
		DryncCellar.this.startCellarOperation();
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
	
	private class CorkComparator implements Comparator<Cork>
	{
		public int compare(Cork object1, Cork object2) {
			Long obj1Id = object1.getCork_id();
			Long obj2Id = object2.getCork_id();
			
			return obj1Id.compareTo(obj2Id);
		}
		
	}
}

