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

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
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
	
	private ListView mReviewList;
	private WineReviewAdapter mReviewAdapter;
	private TableLayout mReviewTable;
	
	LinearLayout searchView;
	ScrollView detailView;
	
	Drawable defaultIcon = null;


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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		flipper = (ViewFlipper) findViewById(R.id.flipper);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(this, R.anim.push_left_out);
		
		searchView = (LinearLayout) this.findViewById(R.id.searchview);
	//	searchView.setVisibility(View.VISIBLE);
		
		detailView = (ScrollView) this.findViewById(R.id.detailview);
		//detailView.setVisibility(View.INVISIBLE);

		DryncUtils.checkForLocalCacheArea();

		deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);

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
		mBottle = bottle;

		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(this, R.anim.push_left_out);

		detailView.scrollTo(0, 0);

		TextView nameView = (TextView) findViewById(R.id.wineName);
		TextView titleView = (TextView) findViewById(R.id.detailTitle);
		TextView yearView = (TextView) findViewById(R.id.yearValue);
		TextView ratingView = (TextView) findViewById(R.id.avgRatingValue);
		TextView priceView = (TextView) findViewById(R.id.priceValue);
		TextView ratingCount = (TextView) findViewById(R.id.reviewCount);
		
		RelativeLayout revListHolder = (RelativeLayout)findViewById(R.id.reviewSection);
		TextView reviewCount = (TextView)findViewById(R.id.reviewCount);

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
					reviewItem.setVisibility(View.INVISIBLE);
					if (mReviewTable != null)
						mReviewTable.setVisibility(View.VISIBLE);
					/*Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fReview.getUrl()));
					startActivity(myIntent);*/
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
		
		Button searchBtn = (Button) this.findViewById(R.id.searchBtn);
		searchBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				DryncMain.this.goToSearchView();				
			}});
		
		if (mReviewTable == null)
		{
			mReviewTable = new TableLayout(DryncMain.this);
			mReviewTable.setVisibility(View.INVISIBLE);
			mReviewTable.setBackgroundResource(R.drawable.rndborder);
		}
		else
		{
			mReviewTable.setVisibility(View.INVISIBLE);
		}
		
		populateReviewTable(mReviewTable, mBottle);
		
		if (mReviewList == null)
		{
			mReviewList = new ListView(DryncMain.this.getBaseContext());
			mReviewList.setScrollContainer(false);
			mReviewList.setCacheColorHint(0);
			mReviewList.setVisibility(View.INVISIBLE);
			mReviewList.setBackgroundResource(R.drawable.rndborder);
		}
		else
		{
			mReviewList.setVisibility(View.INVISIBLE);
		}
		
		RelativeLayout.LayoutParams listparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		listparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		listparams.addRule(RelativeLayout.BELOW, R.id.reviewCount);
		
		revListHolder.addView(mReviewList, listparams);
		revListHolder.addView(mReviewTable, listparams);
		
		if (mReviewAdapter == null)
		{
			mReviewAdapter = new WineReviewAdapter(bottle);
			mReviewList.setAdapter(mReviewAdapter);
		}
		else
		{
			mReviewAdapter.bottle = bottle;
		}
		
		mReviewAdapter.notifyDataSetChanged();
		
		int height = 0;
		/*for (int j=0,m=mReviewAdapter.getCount(); j<m; j++)
		{
			View view = (View)mReviewAdapter.getItem(j);
			height += view.getHeight();
		}*/
		
		mReviewList.setMinimumHeight(height+350);
		
		flipper.showNext();
		
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
		Thread t = new Thread()
		{
			public void run() {
				mResults = DryncProvider.getInstance().getMatches(deviceId, curQuery);
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
			
			if (flipper.getCurrentView() == detailView)
			{
				retval = goToSearchView();
			}
			
			if (retval)
				return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	private boolean goToSearchView()
	{
		if (flipper.getCurrentView() == detailView)
		{
			//detailView.setVisibility(View.INVISIBLE);
			//searchView.setVisibility(View.VISIBLE);
			
			flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_in));
			flipper.setOutAnimation(this, R.anim.push_right_out);
			
			flipper.showPrevious();
			return true;
		}
		return false;
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
				mMainInflater = (LayoutInflater) DryncMain.this.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
			}
			
			final View reviewItem = mMainInflater.inflate(
					R.layout.reviewlistitem, table, false);
			
			TextView publisherText = (TextView) reviewItem.findViewById(R.id.publisher);
			TextView reviewText = (TextView) reviewItem.findViewById(R.id.revText);
			TextView reviewSrc = (TextView) reviewItem.findViewById(R.id.revSource);
			
			
			publisherText.setText(review.getPublisher());
			reviewText.setText(review.getText());
			reviewSrc.setText(review.getReview_source());
			
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
}

