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

public class DryncDetailReviews extends DryncBaseActivity {

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

	SharedPreferences settings;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reviewviewlayout);
		
		settings = getSharedPreferences(DryncUtils.PREFS_NAME, 0);
		// we need to check for changes to twitter settings.
		/*if (settings != null)
		{
			userTwitterUsername = settings.getString(DryncUtils.TWITTER_USERNAME_PREF, null);
			String encryptedTwitterPw = settings.getString(DryncUtils.TWITTER_PASSWORD_PREF, null);
			if (encryptedTwitterPw != null)
				userTwitterPassword = DryncUtils.decryptTwitterPassword(encryptedTwitterPw);
		}*/
		
		Bundle extras = getIntent().getExtras();
		Bottle bottle = (Bottle) (extras != null ? extras.getParcelable("bottle") : null);
		
		initializeAds();
		
		reviewView = (ScrollView) this.findViewById(R.id.reviewview); //inflater.inflate(R.layout.reviewviewlayout, (ViewGroup)flipper, false);
		
		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		launchReviews(bottle);
	}

	private void launchReviews(Bottle bottle) {
		mBottle = bottle;
		
		// mBottle should be set by the detail view, if not, return;
		if (mBottle == null)
			return;
		
			reviewView.scrollTo(0, 0);

			TextView nameView = (TextView) reviewView.findViewById(R.id.reviewWineName);
			
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
			int year = mBottle.getYear();
			yearView.setText("" + year);
			ratingView.setText(mBottle.getRating());
			priceView.setText(mBottle.getPrice());
			String reviewPlurality = ((mBottle.getReviewCount() <= 0) || (mBottle.getReviewCount() > 1)) ?
					" Reviews" : " Review";
			ratingCount.setText("" + mBottle.getReviewCount() + reviewPlurality);
			

			if (defaultIcon == null)
			{
				defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
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
					DryncDetailReviews.this.finish();
				}});

			RelativeLayout.LayoutParams listparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
			listparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			listparams.addRule(RelativeLayout.BELOW, R.id.reviewCount);
			
			if (mReviewTable == null)
			{
				mReviewTable = new TableLayout(DryncDetailReviews.this);
				mReviewTable.setBackgroundResource(R.drawable.rndborder);

				revListHolder.addView(mReviewTable, listparams);

			}
			else
			{
				revListHolder.addView(mReviewTable, listparams);
			}

			populateReviewTable(mReviewTable, mBottle);
	}
	
	class WineReviewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		public Bottle bottle;
		private final LayoutInflater mInflater;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineReviewAdapter(Bottle wine) {
			bottle = wine;
			mInflater = (LayoutInflater) DryncDetailReviews.this.getSystemService(
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
			mInflater = (LayoutInflater) DryncDetailReviews.this.getSystemService(
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
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			DryncDetailReviews.this.finish();
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	private void populateReviewTable(TableLayout table, Bottle bottle)
	{
		table.removeAllViews();
		
		for (int i=0,n=bottle.getReviewCount();i<n;i++)
		{			
			Review review = mBottle.getReview(i);
			if (review == null)
				continue;
			
			if (mMainInflater == null)
			{
				mMainInflater = (LayoutInflater) DryncDetailReviews.this.getSystemService(
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
						Toast noReviewUrl = Toast.makeText(DryncDetailReviews.this, getResources().getString(R.string.noreviewurl), Toast.LENGTH_LONG);
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

