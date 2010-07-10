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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
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

public class DryncDetail extends DryncBaseActivity {

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
	
	boolean launchedFromTopWines = false; 

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
			mList = new ListView(DryncDetail.this.getBaseContext());
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
		setContentView(R.layout.bottledetail);
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		// we need to check for changes to twitter settings.
		/*if (settings != null)
		{
			userTwitterUsername = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, null);
			String encryptedTwitterPw = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, null);
			if (encryptedTwitterPw != null)
				userTwitterPassword = DryncUtils.decryptTwitterPassword(encryptedTwitterPw);
		}*/
		
		initializeAds();
		
		Bundle extras = getIntent().getExtras();
		Bottle bottle = (Bottle) (extras != null ? extras.getParcelable("bottle") : null);
		launchedFromTopWines = ((extras != null) && (extras.containsKey("launchedFromTopWines"))) ? extras.getBoolean("launchedFromTopWines") : false;
		
		detailView = (ScrollView) this.findViewById(R.id.detailview);
	
		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		launchBottle(bottle);
	}

	private void launchBottle(Bottle bottle) {
		if (mBottle != bottle)
		{
			rebuildDetail = true;
			rebuildReviews = true;
			rebuildAddToCellar = true;
		}

		if (rebuildDetail)
		{
			mBottle = bottle;

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
			
			/*Button btnTweet = (Button)detailView.findViewById(R.id.tweet);*/
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
					mMainInflater = (LayoutInflater) DryncDetail.this.getSystemService(
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
						DryncDetail.this.launchReviews(DryncDetail.this.mBottle);
					}});
			}

			nameView.setText(mBottle.getName());
			if (titleView != null)
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
				defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
			}

			RemoteImageView riv = (RemoteImageView) findViewById(R.id.dtlWineThumb);
			if (riv != null)
			{
				String labelThumb = mBottle.getLabel_thumb();
				riv.setRemoteImage(labelThumb, defaultIcon);
			}
			
			riv.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					Dialog dialog = new Dialog(DryncDetail.this);
					dialog.setCancelable(true);
					dialog.setCanceledOnTouchOutside(true);

					dialog.setContentView(R.layout.imagezoom);
					dialog.setTitle("Label:");

					ImageView image = (ImageView) dialog.findViewById(R.id.image);
					image.setImageDrawable(((RemoteImageView)v).getDrawable());
					dialog.show();
				}

				});

			varietalView.setText(bottle.getGrape());
			styleView.setText(bottle.getStyle());
			regionView.setText(bottle.getRegion());
			
			Button searchBtn = (Button) this.findViewById(R.id.searchBtn);
			if (launchedFromTopWines)
			{
				searchBtn.setText("Back");
			}
			
			searchBtn.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncDetail.this.finish();			
				}});
			
			Button addBtn = (Button) this.findViewById(R.id.addBtn);
			Button addToCellarBtn = (Button) this.findViewById(R.id.addToCellar);
			
			OnClickListener addListener = new OnClickListener(){

				public void onClick(View v) {
					DryncDetail.this.launchAddToCellar();	
				}};
				
			addBtn.setOnClickListener(addListener);
			addToCellarBtn.setOnClickListener(addListener);
			
			Button btnShare = (Button) detailView.findViewById(R.id.share);
			
			if (btnShare != null)
			{
				btnShare.setOnClickListener(new OnClickListener()
				{

					public void onClick(View arg0) {
						/* Create the Intent */  
						final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  

						/* Fill it with Data */  
						emailIntent.setType("plain/text");  
						emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.emailsubject));  
						emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, DryncUtils.buildShareEmailText(DryncDetail.this, mBottle));  

						/* Send it off to the Activity-Chooser */  
						DryncDetail.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
					}
				});
			}
			

		/*	if (btnTweet != null)
			{
				btnTweet.setOnClickListener(new OnClickListener()
				{

					public void onClick(View v) {
						StringBuilder tweetStr = new StringBuilder();
						if ((userTwitterUsername == null) || (userTwitterPassword == null))
						{
							Toast noTwtSettings = Toast.makeText(DryncDetail.this, getResources().getString(R.string.twittersettingsmsg), Toast.LENGTH_LONG);
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

								Toast tweetTst = Toast.makeText(DryncDetail.this, "Tweeted \"" + tweetStr.toString() + "\"", Toast.LENGTH_LONG);
								tweetTst.show();
							}
							catch (TwitterException e)
							{
								Toast noTweetTst = Toast.makeText(DryncDetail.this, "Tweet could not be posted.", Toast.LENGTH_LONG);
								noTweetTst.show();
							}
						}
					}});
			}*/


			if (buyBtnSection != null)
			{
				ArrayList<Source> sources = mBottle.getSources();
				int lastAdded = -1;
				ArrayList<String> trackUsedSrc = new ArrayList<String>();
				
				for (int i=0,n=sources.size();i<n;i++)
				{
					final Source source = sources.get(i);
					if ((trackUsedSrc.contains(source.getName())) || (! source.getName().toLowerCase().equals("wine.com")))
						continue;
						
					Button buyButton = new Button(this);
					buyButton.setId(i);
					buyButton.setText("Buy from " + source.getName());
					buyButton.setTextColor(Color.WHITE);
					buyButton.setGravity(Gravity.CENTER);
					buyButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.dryncbutton));
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
		//showNext();
	}
	
	private void launchAddToCellar() {
		// mBottle should be set by the detail view, if not, return;
		if (mBottle == null)
			return;
		
		Intent twIntent = new Intent(this, DryncAddToCellar.class);
		twIntent.putExtra("bottle", mBottle);
		startActivityForResult(twIntent, ADDTOCELLAR_RESULT);  
	}
	
	private void launchReviews(Bottle bottle) {
		Intent twIntent = new Intent(this, DryncDetailReviews.class);
		twIntent.putExtra("bottle", bottle);
		startActivity(twIntent);  
	}
	
	
	class WineReviewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		public Bottle bottle;
		private final LayoutInflater mInflater;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineReviewAdapter(Bottle wine) {
			bottle = wine;
			mInflater = (LayoutInflater) DryncDetail.this.getSystemService(
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
			mInflater = (LayoutInflater) DryncDetail.this.getSystemService(
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			DryncDetail.this.finish();
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// we need to check for changes to twitter settings.
		/*if (settings != null)
		{
			userTwitterUsername = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, null);
			String encryptedTwitterPw = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, null);
			if (encryptedTwitterPw != null)
				userTwitterPassword = DryncUtils.decryptTwitterPassword(encryptedTwitterPw);
		}*/
	}
	
	@Override
	public int getMenuItemToSkip() {
		return -1;
	}
	
}

