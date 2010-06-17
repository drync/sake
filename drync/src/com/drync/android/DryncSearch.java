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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.WineItemRelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Review;
import com.drync.android.objects.Source;
import com.drync.android.ui.RemoteImageView;

public class DryncSearch extends DryncBaseActivity {

	
	
	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Bottle> mResults = null;

	private Bottle mBottle = null;
	private ProgressDialog progressDlg = null;
	private String deviceId;
	WineAdapter mAdapter; 
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
	boolean displaySearch = true;
	boolean displayTopWinesBtns = false;
	
	int lastSelectedTopWine = -1;
	
	
	
	private TableLayout mReviewTable;
	
	LinearLayout searchView;
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
	
	
	private void updateResultsInUi() {

		// Back in the UI thread -- update our UI elements based on the data in mResults
		if (mList == null)
		{
			LinearLayout listholder = (LinearLayout)findViewById(R.id.listholder);
			mList = new ListView(DryncSearch.this.getBaseContext());
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

	SharedPreferences settings;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchview);
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		
		String lastQuery = settings.getString(DryncUtils.LAST_QUERY_PREF, null);
		
		Bundle extras = getIntent().getExtras();
		this.displaySearch = extras != null ? extras.getBoolean("displaySearch") : true;
		this.displayTopWinesBtns = extras != null ? extras.getBoolean("displayTopWinesBtns") : false;
		
		startCellarUpdateThread();
		
		LayoutInflater inflater = getLayoutInflater();
		
		/*flipper = (ViewFlipper) findViewById(R.id.flipper);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(this, R.anim.push_left_out);*/
		
		searchView = (LinearLayout) this.findViewById(R.id.searchview);
		//detailView = (ScrollView) this.findViewById(R.id.detailview);
	
		//reviewView = (ScrollView) inflater.inflate(R.layout.reviewviewlayout, (ViewGroup)flipper, false);
		//flipper.addView(reviewView); 

//		addView = (ScrollView) inflater.inflate(R.layout.addtocellar, (ViewGroup)flipper, false);
	//	flipper.addView(addView);
		
		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		final LinearLayout searchholder = (LinearLayout) findViewById(R.id.searchHolder);
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
					progressDlg =  new ProgressDialog(DryncSearch.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving popular wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					DryncSearch.this.startTopWineQueryOperation(DryncProvider.TOP_POPULAR);
					DryncSearch.this.lastSelectedTopWine = DryncProvider.TOP_POPULAR;
					
					detailSelectedTopWineButton(popButton, featButton, mwButton);
					
				}});
			
			featButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					progressDlg =  new ProgressDialog(DryncSearch.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving featured wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					DryncSearch.this.startTopWineQueryOperation(DryncProvider.TOP_FEATURED);
					DryncSearch.this.lastSelectedTopWine = DryncProvider.TOP_FEATURED;
					
					detailSelectedTopWineButton(popButton, featButton, mwButton);
				}});
			
			mwButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					progressDlg =  new ProgressDialog(DryncSearch.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Retrieving popular wines...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					DryncSearch.this.startTopWineQueryOperation(DryncProvider.TOP_WANTED);
					DryncSearch.this.lastSelectedTopWine = DryncProvider.TOP_WANTED;
					
					detailSelectedTopWineButton(popButton, featButton, mwButton);
					
				}});
			
			if (DryncSearch.this.lastSelectedTopWine == -1)
				popButton.performClick();
		}
		
		final EditText searchfield = (EditText) findViewById(R.id.searchentry);
		
		if (lastQuery != null)
		{
			searchfield.setText(lastQuery);
		}
		
		searchfield.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
			            (keyCode == KeyEvent.KEYCODE_ENTER)) {
			        	String searchterm = searchfield.getText().toString();

						progressDlg =  new ProgressDialog(DryncSearch.this);
						progressDlg.setTitle("Dryncing...");
						progressDlg.setMessage("Retrieving wines...");
						progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						progressDlg.show();
						DryncSearch.this.startQueryOperation(searchterm);
						return true;
			        }
			        return false;
			}});
		
		if (displaySearch)
		{
			View tstLayout = inflater.inflate(R.layout.searchinstructions,
					(ViewGroup) findViewById(R.id.search_toast_layout));

			Toast toast = new Toast(getApplicationContext()) {

			};
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(10);
			toast.setView(tstLayout);
			toast.show();
		}
	}

	private void launchBottle(Bottle bottle) {
		Intent twIntent = new Intent(this, DryncDetail.class);
		twIntent.putExtra("bottle", bottle);
		startActivity(twIntent);  
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
	
	private void launchAddToCellar() {
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
						//cork.setCork_created_at(System.currentTimeMillis());
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
						flipper.setInAnimation(AnimationUtils.loadAnimation(DryncSearch.this, R.anim.push_right_in));
						flipper.setOutAnimation(DryncSearch.this, R.anim.push_right_out);
						
						View view = flipper.findViewById(R.id.detailview);
						int detailViewIdx = flipper.indexOfChild(view);
						flipper.setDisplayedChild(detailViewIdx);	
						
						flipper.setInAnimation(AnimationUtils.loadAnimation(DryncSearch.this, R.anim.push_left_in));
						flipper.setOutAnimation(DryncSearch.this, R.anim.push_left_out);
						
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
		
		View view = flipper.findViewById(R.id.addToCellar);
		//flipper.bringChildToFront(view);
		int addToCellarIdx = flipper.indexOfChild(view);
		flipper.setDisplayedChild(addToCellarIdx);		
	}
	
	private void launchReviews() {
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
					showPrevious();			
				}});

			RelativeLayout.LayoutParams listparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
			listparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			listparams.addRule(RelativeLayout.BELOW, R.id.reviewCount);
			
			if (mReviewTable == null)
			{
				mReviewTable = new TableLayout(DryncSearch.this);
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
		
		showNext();
		
	}
	
	private void showNext()
	{
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(this, R.anim.push_left_out);
		
		flipper.showNext();
	}
	
	private void showPrevious()
	{
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_in));
		flipper.setOutAnimation(this, R.anim.push_right_out);
	
		flipper.showPrevious();
	}
	
	class WineReviewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		public Bottle bottle;
		private final LayoutInflater mInflater;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineReviewAdapter(Bottle wine) {
			bottle = wine;
			mInflater = (LayoutInflater) DryncSearch.this.getSystemService(
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

		private final List<Bottle> mWines;
		private final LayoutInflater mInflater;
		private final Drawable defaultIcon;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineAdapter(List<Bottle> wines) {
			mWines = wines;
			mInflater = (LayoutInflater) DryncSearch.this.getSystemService(
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
			
			Bottle wine = mWines.get(position);
			
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
			View wineItem = mInflater.inflate(
					R.layout.wineitem, parent, false);
			return wineItem;
		}

		private void bindView(View view, Bottle wine) {
			WineItemRelativeLayout wiv = (WineItemRelativeLayout) view;
			wiv.setBottle(wine);
			RemoteImageView wineThumb = (RemoteImageView) view.findViewById(R.id.wineThumb);
			if (wineThumb != null  && !mFlinging )
			{
				if (wine.getLabel_thumb() != null)
				{
					wineThumb.setLocalURI(DryncUtils.getCacheFileName(wine.getLabel_thumb()));
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
		
		Thread t = new Thread()
		{
			public void run() {
				mResults = DryncProvider.getInstance().getMatches(deviceId, curQuery);
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
	}
	
	protected void startTopWineQueryOperation(final int type)
	{
		Thread t = new Thread()
		{
			public void run() {
				mResults = DryncProvider.getInstance().getTopWines(deviceId, type);
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
	}
	
	private void populateReviewTable(TableLayout table, Bottle bottle)
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
				mMainInflater = (LayoutInflater) DryncSearch.this.getSystemService(
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
						Toast noReviewUrl = Toast.makeText(DryncSearch.this, getResources().getString(R.string.noreviewurl), Toast.LENGTH_LONG);
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
	
	private Animation inFromRightAnimation() {

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
	
	private void detailSelectedTopWineButton(Button popButton, Button featButton, Button mwButton)
	{
		if (this.lastSelectedTopWine == DryncProvider.TOP_POPULAR)
		{
			popButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			popButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}
		
		if (this.lastSelectedTopWine == DryncProvider.TOP_FEATURED)
		{
			featButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			featButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}
		
		if (this.lastSelectedTopWine == DryncProvider.TOP_WANTED)
		{
			mwButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.woodbutton_pressed));
		}
		else
		{
			mwButton.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.dryncbutton));
		}		
	}
	
	private void startCellarUpdateThread()
	{
		ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
		
		final Runnable cellarUpdateThread = new Runnable()
		{
			public void run()
			{
				try
				{

					ConnectivityManager cmgr = 
						(ConnectivityManager) DryncSearch.this.getSystemService(
								Context.CONNECTIVITY_SERVICE);

					NetworkInfo mobileinfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
					NetworkInfo wifiinfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

					if (((mobileinfo != null) && 
							(mobileinfo.isConnected())) ||
							((wifiinfo != null) && (wifiinfo.isConnected())))
					{
						DryncDbAdapter dbAdapter = new DryncDbAdapter(DryncSearch.this);
						dbAdapter.open();

						List<Cork> corks = dbAdapter.getAllCorksNeedingUpdates();

						for (Cork cork : corks)
						{
							boolean postSuccess = false;

							// re-try post.
							if (cork.getUpdateType() == Cork.UPDATE_TYPE_INSERT)
							{
								postSuccess = DryncProvider.postCreateOrUpdate(cork, deviceId);
							}
							else if (cork.getUpdateType() == Cork.UPDATE_TYPE_DELETE)
							{
								postSuccess = DryncProvider.postDelete(cork, deviceId);
							}
							else if (cork.getUpdateType() == Cork.UPDATE_TYPE_UPDATE)
							{
								postSuccess = DryncProvider.postUpdate(cork, deviceId);
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
						
						dbAdapter.close();
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
			}
		};
				
		ex.scheduleAtFixedRate(cellarUpdateThread, 10, 60, TimeUnit.SECONDS);
	}
}

