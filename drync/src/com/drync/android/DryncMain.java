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

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
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
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.RelativeLayout.LayoutParams;

import com.drync.android.objects.Bottle;
import com.drync.android.objects.Review;
import com.drync.android.objects.Source;
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
	ViewFlipper flipper;
	
	boolean displaySearch = true;
	
	private TableLayout mReviewTable;
	
	LinearLayout searchView;
	ScrollView detailView;
	ScrollView reviewView;
	
	boolean rebuildDetail = false;
	boolean rebuildReviews = false;
	
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

	SharedPreferences settings;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		
		String lastQuery = settings.getString(DryncUtils.LAST_QUERY_PREF, null);
		
		Bundle extras = getIntent().getExtras();
		this.displaySearch = extras != null ? extras.getBoolean("displaySearch") : true;
		
		LayoutInflater inflater = getLayoutInflater();
		
		flipper = (ViewFlipper) findViewById(R.id.flipper);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(this, R.anim.push_left_out);
		
		searchView = (LinearLayout) this.findViewById(R.id.searchview);
		
		detailView = (ScrollView) this.findViewById(R.id.detailview);
	
		reviewView = (ScrollView) inflater.inflate(R.layout.reviewviewlayout, (ViewGroup)flipper, false);
		
		flipper.addView(reviewView);

		DryncUtils.checkForLocalCacheArea();

		deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
		final LinearLayout searchholder = (LinearLayout) findViewById(R.id.searchHolder);
		
		if (! displaySearch)
		{
			searchholder.setVisibility(View.INVISIBLE);
			
			progressDlg =  new ProgressDialog(DryncMain.this);
			progressDlg.setTitle("Dryncing...");
			progressDlg.setMessage("Retrieving top wines...");
			progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDlg.show();
			DryncMain.this.startTopWineQueryOperation();	            
		}
		
		
		final EditText searchfield = (EditText) findViewById(R.id.searchentry);
		
		if (lastQuery != null)
		{
			searchfield.setText(lastQuery);
		}
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
		if (mBottle != bottle)
		{
			rebuildDetail = true;
			rebuildReviews = true;
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
					mMainInflater = (LayoutInflater) DryncMain.this.getSystemService(
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
						DryncMain.this.launchReviews();
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

			varietalView.setText(bottle.getGrape());
			styleView.setText(bottle.getStyle());
			regionView.setText(bottle.getRegion());
			
			Button searchBtn = (Button) this.findViewById(R.id.searchBtn);
			searchBtn.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					showPrevious();				
				}});

			if (btnTweet != null)
			{
				btnTweet.setOnClickListener(new OnClickListener()
				{

					public void onClick(View v) {
						StringBuilder tweetStr = new StringBuilder();
						if ((userTwitterUsername == null) || (userTwitterPassword == null))
						{
							Toast noTwtSettings = Toast.makeText(DryncMain.this, getResources().getString(R.string.twittersettingsmsg), Toast.LENGTH_LONG);
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

							/*if (mBottle.getSources().size() > 0)
							{
								tweetStr.append(mBottle.getSource(0).getUrl());
							}*/

							try
							{
								twitter.updateStatus(tweetStr.toString());

								Toast tweetTst = Toast.makeText(DryncMain.this, "Tweeted \"" + tweetStr.toString() + "\"", Toast.LENGTH_LONG);
								tweetTst.show();
							}
							catch (TwitterException e)
							{
								Toast noTweetTst = Toast.makeText(DryncMain.this, "Tweet could not be posted.", Toast.LENGTH_LONG);
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
		showNext();
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
				mReviewTable = new TableLayout(DryncMain.this);
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
			mInflater = (LayoutInflater) DryncMain.this.getSystemService(
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
	
	protected void startTopWineQueryOperation()
	{
		Thread t = new Thread()
		{
			public void run() {
				mResults = DryncProvider.getInstance().getTopWines(deviceId, DryncProvider.TOP_POPULAR);
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			boolean retval = false;
			
			if ((flipper.getCurrentView() == detailView) || (flipper.getCurrentView() == reviewView))
			{
				showPrevious();
				retval = true;
			}
			
			if (retval)
				return true;
		}
		
		return super.onKeyDown(keyCode, event);
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
				mMainInflater = (LayoutInflater) DryncMain.this.getSystemService(
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
					Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fReview.getUrl()));
					startActivity(myIntent);
									}});
			
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
	
	
	
}

