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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.WineItemRelativeLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.ui.RemoteImageView;

public class DryncCellar extends DryncBaseActivity {

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
		//case EDIT_ID:
		//	editNote(info.id);
		//	return true;
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
		startQueryOperation(DryncDbAdapter.FILTER_TYPE_NONE, query);
	}
	
	protected void startQueryOperation(int filterType, String query)
	{
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
			
			final EditText searchfield = (EditText) findViewById(R.id.searchentry);
			final Button myWinesButton = (Button)findViewById(R.id.myWinesBtn);
			final Button iDrankButton = (Button)findViewById(R.id.iDrankBtn);
			final Button iOwnButton = (Button)findViewById(R.id.iOwnBtn);
			final Button iWantButton = (Button)findViewById(R.id.iWantBtn);
			
			myWinesButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					String searchterm = searchfield.getText().toString();
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
					String searchterm = searchfield.getText().toString();
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
					String searchterm = searchfield.getText().toString();
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
					String searchterm = searchfield.getText().toString();
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
				searchfield.setText(lastFilter);
				myWinesButton.performClick();
			}
		}
		
		final EditText searchfield = (EditText) findViewById(R.id.searchentry);
		
		if (lastFilter != null)
		{
			searchfield.setText(lastFilter);
		}
		
		ImageButton clrFilterBtn = (ImageButton)findViewById(R.id.clearFilterBtn);
		clrFilterBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				//TODO: @mbrindam prompt to clear filter
				searchfield.setText("");
				progressDlg =  new ProgressDialog(DryncCellar.this);
				progressDlg.setTitle("Dryncing...");
				progressDlg.setMessage("Retrieving corks...");
				progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDlg.show();
				DryncCellar.this.startQueryOperation("");
			}});
		
		searchfield.setOnKeyListener(new OnKeyListener() {

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
			}});
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
		
		public CorkAdapter(List<Cork> mResults) {
			mWines = mResults;
			mInflater = (LayoutInflater) DryncCellar.this.getSystemService(
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
			
			Log.d("DryncMain", "getview position: " + position);
			
			Cork wine = mWines.get(position);
			
			WineItemRelativeLayout wiv = (WineItemRelativeLayout) view;
			if ((wiv.getBottle() == null) || (wiv.getBottle() != wine))
			{
				bindView(view, wine);

				if (view != null)
				{
					RemoteImageView wineThumb = (RemoteImageView) view.findViewById(R.id.wineThumb);
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
		if (settings != null)
		{
			userTwitterUsername = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, null);
			String encryptedTwitterPw = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, null);
			if (encryptedTwitterPw != null)
				userTwitterPassword = DryncUtils.decryptTwitterPassword(encryptedTwitterPw);
		}
	}
	
	private void detailSelectedCellarButton(Button mainButton, Button ownButton, Button wantButton, Button drankButton)
	{
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_NONE)
		{
			mainButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			mainButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}
		
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_OWN)
		{
			ownButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			ownButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}
		
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_WANT)
		{
			wantButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			wantButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}		
		
		if (this.lastSelectedCellar == DryncDbAdapter.FILTER_TYPE_DRANK)
		{
			drankButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			drankButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}		
	}
}

