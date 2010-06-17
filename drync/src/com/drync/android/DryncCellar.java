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
		/*if (mBottle != bottle)
		{
			rebuildDetail = true;
			rebuildReviews = true;
			rebuildAddToCellar = true;
		}

		if (rebuildDetail)
		{
			mBottle = bottle;

			flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
			flipper.setOutAnimation(this, R.anim.push_left_out);

			detailView.scrollTo(0, 0);

			TextView nameView = (TextView) detailView.findViewById(R.id.wineName);
			TextView titleView = (TextView) detailView.findViewById(R.id.detailTitle);
			TextView yearView = (TextView) detailView.findViewById(R.id.yearValue);
			TextView ratingView = (TextView) detailView.findViewById(R.id.avgRatingValue);
			TextView priceView = (TextView) detailView.findViewById(R.id.priceValue);
			TextView ratingCount = (TextView) detailView.findViewById(R.id.reviewCount);
			TextView addInfoView = (TextView) detailView.findViewById(R.id.addlInfoHdr);

			RelativeLayout revListHolder = (RelativeLayout)detailView.findViewById(R.id.reviewSection);
			TextView reviewCount = (TextView)detailView.findViewById(R.id.reviewCount);
			
			TextView varietalView = (TextView) detailView.findViewById(R.id.varietalval);
			TextView styleView = (TextView) detailView.findViewById(R.id.styleval);
			TextView regionView = (TextView) detailView.findViewById(R.id.regionval);
			
			Button btnTweet = (Button)detailView.findViewById(R.id.tweet);
			RelativeLayout buyBtnSection = (RelativeLayout)detailView.findViewById(R.id.buySection);
			
			revListHolder.removeAllViews();
			RelativeLayout.LayoutParams rcparams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			rcparams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			rcparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			revListHolder.addView(reviewCount, rcparams);


			for (int i=0,n=mBottle.getReviewCount();i<n;i++)
			{
				if (i >= 1)
					break;

				Review review = mBottle.getReview(i);
				if (review == null)
					continue;

				if (mMainInflater == null)
				{
					mMainInflater = (LayoutInflater) DryncSearch.this.getSystemService(
							Context.LAYOUT_INFLATER_SERVICE);
				}

				final View reviewItem = mMainInflater.inflate(
						R.layout.reviewitem, revListHolder, false);

				TextView reviewText = (TextView) reviewItem.findViewById(R.id.reviewText);

				reviewText.setText(review.getText());

				RelativeLayout.LayoutParams revItemparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
				revItemparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				revItemparams.addRule(RelativeLayout.BELOW, R.id.reviewCount);

				revListHolder.addView(reviewItem, revItemparams);

				TextView readReviewTxt = (TextView) reviewItem.findViewById(R.id.readreviewtext);
				final Review fReview = review;
				readReviewTxt.setOnClickListener(new OnClickListener(){
					public void onClick(View v) {
						DryncSearch.this.launchReviews();
					}});
			}

			nameView.setText(mBottle.getName());
			titleView.setText(mBottle.getName());
			int year = mBottle.getYear();
			yearView.setText("" + year);
			ratingView.setText(mBottle.getRating());
			priceView.setText(mBottle.getPrice());
			String reviewPlurality = ((mBottle.getReviewCount() <= 0) || (mBottle.getReviewCount() > 1)) ?
					" Reviews" : " Review";
			ratingCount.setText("" + mBottle.getReviewCount() + reviewPlurality);
			
			if (addInfoView != null)
				addInfoView.setText("" + "Additional Info");

			if (defaultIcon == null)
			{
				defaultIcon = getResources().getDrawable(R.drawable.icon);
			}

			RemoteImageView riv = (RemoteImageView) findViewById(R.id.dtlWineThumb);
			if (riv != null)
			{
				String labelThumb = mBottle.getLabel_thumb();
				riv.setRemoteImage(labelThumb, defaultIcon);
			}

			varietalView.setText(bottle.getGrape());
			styleView.setText(bottle.getStyle());
			regionView.setText(bottle.getRegion());
			
			Button searchBtn = (Button) this.findViewById(R.id.searchBtn);
			searchBtn.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					showPrevious();				
				}});
			
			Button addBtn = (Button) this.findViewById(R.id.addBtn);
			addBtn.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncSearch.this.launchAddToCellar();	
				}});
			
			

			if (btnTweet != null)
			{
				btnTweet.setOnClickListener(new OnClickListener()
				{

					public void onClick(View v) {
						StringBuilder tweetStr = new StringBuilder();
						if ((userTwitterUsername == null) || (userTwitterPassword == null))
						{
							Toast noTwtSettings = Toast.makeText(DryncSearch.this, getResources().getString(R.string.twittersettingsmsg), Toast.LENGTH_LONG);
							noTwtSettings.show();
						}
						else
						{
							Twitter twitter = new Twitter(userTwitterUsername, userTwitterPassword);
							twitter.setSource("DryncWineDroid");
							tweetStr.append("Drinking the ");
							tweetStr.append(mBottle.getName());
							String lastSubstr = " #wine";
							// ensure proper length for tweet.
							if (tweetStr.length() > (160 - lastSubstr.length()))
							{
								tweetStr.setLength(160 - 3 - lastSubstr.length());
								tweetStr.append("...");								
							}
							tweetStr.append(lastSubstr);

							try
							{
								twitter.updateStatus(tweetStr.toString());

								Toast tweetTst = Toast.makeText(DryncSearch.this, "Tweeted \"" + tweetStr.toString() + "\"", Toast.LENGTH_LONG);
								tweetTst.show();
							}
							catch (TwitterException e)
							{
								Toast noTweetTst = Toast.makeText(DryncSearch.this, "Tweet could not be posted.", Toast.LENGTH_LONG);
								noTweetTst.show();
							}
						}
					}});
			}


			if (buyBtnSection != null)
			{
				ArrayList<Source> sources = mBottle.getSources();
				int lastAdded = -1;
				ArrayList<String> trackUsedSrc = new ArrayList<String>();
				
				for (int i=0,n=sources.size();i<n;i++)
				{
					final Source source = sources.get(i);
					if (trackUsedSrc.contains(source.getName()))
						continue;
						
					Button buyButton = new Button(this);
					buyButton.setId(i);
					buyButton.setText("Buy from " + source.getName());
					buyButton.setTextColor(Color.WHITE);
					buyButton.setGravity(Gravity.CENTER);
					buyButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.qn_woodbutton));
					buyButton.setPadding(20, 0, 0, 0);
					Drawable leftDrawable = getResources().getDrawable(R.drawable.safari_white);
					buyButton.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, null, null);
					LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
					if (lastAdded == -1)
					{
						lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
					}
					else
					{
						lp.addRule(RelativeLayout.BELOW, lastAdded);
					}
					
					buyButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) 
						{
							// TODO: @mbrindam - add "leaving drink" dialog here.
							
							Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.getUrl()));
							startActivity(myIntent);
						}
					});
					
					lastAdded = i;
					trackUsedSrc.add(source.getName());
					buyBtnSection.addView(buyButton, lp);
				}
			}
		
			rebuildDetail = false;
		}
		showNext();*/
	}
	
	/*private void launchAddToCellar() {
		// mBottle should be set by the detail view, if not, return;
		if (mBottle == null)
			return;
		
		
		final AutoCompleteTextView yearVal = (AutoCompleteTextView) addView.findViewById(R.id.atcYearVal);
		final EditText varietalVal = (EditText) addView.findViewById(R.id.atcVarietalVal);
		final EditText regionVal = (EditText) addView.findViewById(R.id.atcRegionVal);
		final DryncDbAdapter dbAdapter = new DryncDbAdapter(this);

		EditText priceVal = (EditText) addView.findViewById(R.id.atcPriceVal);
		final EditText nameVal = (EditText) addView.findViewById(R.id.atcWineName);
		RemoteImageView wineThumb = (RemoteImageView) addView.findViewById(R.id.atcWineThumb);
		final RatingBar ratingbar = (RatingBar) addView.findViewById(R.id.atcRatingVal);
		final Spinner styleVal = (Spinner)addView.findViewById(R.id.atcStyleVal);
		
		ArrayAdapter<String> yearSpnAdapter = null;
		ArrayAdapter<CharSequence> varietalSpnAdapter = null;
		ArrayAdapter<CharSequence> styleSpnAdapter = null;
		ArrayAdapter<CharSequence> regionSpnAdapter = null;
		int year = 1800;
		
		if (buildOnceAddToCellar)
		{
			ArrayList<String> _allYears = new ArrayList<String>();
		       
			Date date = new Date();
			Calendar cal = new GregorianCalendar();
			cal.setTime(date);
			year = cal.get(Calendar.YEAR);
			
	        for (int i=1800,n=year+3;i<n;i++){
	            _allYears.add("" + i);
	        }

	        yearSpnAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, _allYears);
	        yearSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        yearVal.setAdapter(yearSpnAdapter); 
	        
	       // varietalSpnAdapter = ArrayAdapter.createFromResource(this, R.array.varietal_array, android.R.layout.simple_spinner_dropdown_item);
	      //  varietalSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	      //  varietalVal.setAdapter(varietalSpnAdapter); 
	        
	       // regionSpnAdapter = ArrayAdapter.createFromResource(this, R.array.region_array, android.R.layout.simple_spinner_dropdown_item);
	       // regionSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	      //  regionVal.setAdapter(regionSpnAdapter); 
	        
	        styleSpnAdapter = ArrayAdapter.createFromResource(
	                this, R.array.style_array, android.R.layout.simple_spinner_item);
	        styleSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
	        styleVal.setAdapter(styleSpnAdapter);
	        
	        Button addToCellarBtn = (Button)addView.findViewById(R.id.addToCellar);
	        if (addToCellarBtn != null)
	        {
	        	addToCellarBtn.setOnClickListener(new OnClickListener(){

					public void onClick(View v) {
						Cork cork = new Cork();
						cork.setName(nameVal.getEditableText().toString());
						cork.setBottle_Id(mBottle.getBottle_Id());
						cork.setYear(mBottle.getYear());
						cork.setCork_year(Integer.parseInt(yearVal.getEditableText().toString()));
						cork.setCork_created_at(System.currentTimeMillis());
						cork.setGrape(varietalVal.getEditableText().toString());
						cork.setRegion(regionVal.getEditableText().toString());
						cork.setCork_rating(ratingbar.getRating());
						dbAdapter.open();
						dbAdapter.insertCork(cork);
						dbAdapter.close();
						Log.d("AddToCellar", "Yearselected: " + cork.getYear());
					}});
	        }
	        
			Button cancelBtn = (Button)addView.findViewById(R.id.cancelBtn);
			if (cancelBtn != null)
			{
				cancelBtn.setOnClickListener(new OnClickListener(){

					public void onClick(View v) {
						// in order 
						flipper.setInAnimation(AnimationUtils.loadAnimation(DryncCellar.this, R.anim.push_right_in));
						flipper.setOutAnimation(DryncCellar.this, R.anim.push_right_out);
						
						View view = flipper.findViewById(R.id.detailview);
						int detailViewIdx = flipper.indexOfChild(view);
						flipper.setDisplayedChild(detailViewIdx);	
						
						flipper.setInAnimation(AnimationUtils.loadAnimation(DryncCellar.this, R.anim.push_left_in));
						flipper.setOutAnimation(DryncCellar.this, R.anim.push_left_out);
						
					}});
			}
			
			buildOnceAddToCellar = false;
		}
		
		if (rebuildAddToCellar)
		{
			flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
			flipper.setOutAnimation(this, R.anim.push_left_out);
			
			int position;
			if (mBottle.getYear() > 0)
				position = yearSpnAdapter.getPosition("" + mBottle.getYear());
			else
				position = yearSpnAdapter.getPosition("" + year);
			
			nameVal.setText(mBottle.getName());
			yearVal.setText("" + mBottle.getYear());
			varietalVal.setText("" + mBottle.getGrape());
			regionVal.setText("" + mBottle.getRegion());
			int stylepos = styleSpnAdapter.getPosition(mBottle.getStyle());
			if ((stylepos < styleSpnAdapter.getCount() || (stylepos > styleSpnAdapter.getCount())))
				styleVal.setSelection(styleSpnAdapter.getPosition("Other"));
			else
				styleVal.setSelection(stylepos);
			priceVal.setText(mBottle.getPrice());
			wineThumb.setRemoteImage(mBottle.getLabel_thumb(), defaultIcon);

			reviewView.scrollTo(0, 0);
		}
		
		//View view = flipper.findViewById(R.id.addToCellar);
		//flipper.bringChildToFront(view);
		int addToCellarIdx = flipper.indexOfChild(view);
		flipper.setDisplayedChild(addToCellarIdx);		
	}
	*/
/*	private void launchReviews() {
		// mBottle should be set by the detail view, if not, return;
		if (mBottle == null)
			return;
		
		if (rebuildReviews)
		{
			flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
			flipper.setOutAnimation(this, R.anim.push_left_out);

			reviewView.scrollTo(0, 0);

			TextView nameView = (TextView) reviewView.findViewById(R.id.reviewWineName);
			TextView titleView = (TextView) reviewView.findViewById(R.id.reviewTitle);
			TextView yearView = (TextView) reviewView.findViewById(R.id.yearValue);
			TextView ratingView = (TextView) reviewView.findViewById(R.id.avgRatingValue);
			TextView priceView = (TextView) reviewView.findViewById(R.id.priceValue);
			TextView ratingCount = (TextView) reviewView.findViewById(R.id.reviewCount);
			RelativeLayout revListHolder = (RelativeLayout)reviewView.findViewById(R.id.reviewSection);
			TextView reviewCount = (TextView)reviewView.findViewById(R.id.reviewCount);

			revListHolder.removeAllViews();
			RelativeLayout.LayoutParams rcparams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			rcparams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			rcparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			revListHolder.addView(reviewCount, rcparams);

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

			RemoteImageView riv = (RemoteImageView) reviewView.findViewById(R.id.reviewWineThumb);
			if (riv != null)
			{
				String labelThumb = mBottle.getLabel_thumb();
				riv.setRemoteImage(labelThumb, defaultIcon);
			}

			Button doneBtn = (Button) this.findViewById(R.id.doneBtn);
			doneBtn.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncCellar.this.finish();			
				}});

			RelativeLayout.LayoutParams listparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
			listparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			listparams.addRule(RelativeLayout.BELOW, R.id.reviewCount);
			
			if (mReviewTable == null)
			{
				mReviewTable = new TableLayout(DryncCellar.this);
				mReviewTable.setBackgroundResource(R.drawable.rndborder);

				revListHolder.addView(mReviewTable, listparams);

			}
			else
			{
				revListHolder.addView(mReviewTable, listparams);
			}

			populateReviewTable(mReviewTable, mBottle);
			rebuildReviews = false;
		}
	}
	
	class WineReviewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		public Bottle bottle;
		private final LayoutInflater mInflater;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineReviewAdapter(Bottle wine) {
			bottle = wine;
			mInflater = (LayoutInflater) DryncCellar.this.getSystemService(
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
			WebView reviewWeb = (WebView) reviewItem.findViewById(R.id.reviewWeb);
			TextView readReviewTxt = (TextView) reviewItem.findViewById(R.id.readreviewtext);
			View line = (View) reviewItem.findViewById(R.id.line);

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

			final Review fReview = review;
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
				}});
			
		}

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
		}
	}
*/


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
	
	/*private void populateReviewTable(TableLayout table, Bottle bottle)
	{
		table.removeAllViews();
		//table.setFocusableInTouchMode(true);
		//table.setClickable(true);
		
		for (int i=0,n=bottle.getReviewCount();i<n;i++)
		{			
			Review review = mBottle.getReview(i);
			if (review == null)
				continue;
			
			if (mMainInflater == null)
			{
				mMainInflater = (LayoutInflater) DryncCellar.this.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
			}
			
			final View reviewItem = mMainInflater.inflate(
					R.layout.reviewlistitem, table, false);
			
			TextView publisherText = (TextView) reviewItem.findViewById(R.id.publisher);
			TextView reviewText = (TextView) reviewItem.findViewById(R.id.revText);
			TextView reviewSrc = (TextView) reviewItem.findViewById(R.id.revSource);
			
			reviewSrc.setFocusable(true);
			reviewSrc.setClickable(true);
			
			publisherText.setText(review.getPublisher());
			reviewText.setText(review.getText());
			reviewSrc.setText(review.getReview_source());
			
			final Review fReview = review;
			publisherText.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					if (fReview.getUrl() != null)
					{
						Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fReview.getUrl()));
						startActivity(myIntent);
					}	
					else
					{
						Toast noReviewUrl = Toast.makeText(DryncCellar.this, getResources().getString(R.string.noreviewurl), Toast.LENGTH_LONG);
						noReviewUrl.show();
					}
				}
			});
			
			table.addView(reviewItem);
			
			if (i < n-1)
			{
				final View separatorItem = mMainInflater.inflate(
						R.layout.separator, table, false);
				table.addView(separatorItem);
			}
			
		}
		
	}
	
	*/private Animation inFromRightAnimation() {

		Animation inFromRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
		);
		inFromRight.setDuration(500);
		inFromRight.setInterpolator(new AccelerateInterpolator());
		return inFromRight;
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

