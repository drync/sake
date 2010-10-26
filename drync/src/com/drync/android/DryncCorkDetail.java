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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.WineItemRelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Source;
import com.drync.android.ui.RemoteImageView;

public class DryncCorkDetail extends DryncBaseActivity {

	private ListView mList;
	final Handler mHandler = new Handler();
	final Handler tweetHandler = new Handler();
	private List<Cork> mResults = null;

	private Cork mBottle = null;
	private ProgressDialog progressDlg = null;
	private String deviceId;
	WineAdapter mAdapter; 
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
	boolean displaySearch = true;
	boolean displayTopWinesBtns = false;
	
	int lastSelectedTopWine = -1;
	
	LinearLayout searchView;
	ScrollView detailView;
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
			mList = new ListView(DryncCorkDetail.this.getBaseContext());
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
		setContentView(R.layout.corkdetail);
		
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
		Cork cork = (Cork) (extras != null ? extras.getParcelable("bottle") : null);
		
		detailView = (ScrollView) this.findViewById(R.id.detailview);

		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		launchBottle(cork);
	}

	private void launchBottle(Cork bottle) {
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
			
			TextView ratingView = (TextView) detailView.findViewById(R.id.avgRatingValue);
			TextView tastingNoteLbl = (TextView) detailView.findViewById(R.id.tastingNoteLbl);
			
			TextView varietalView = (TextView) detailView.findViewById(R.id.varietalValue);
			TextView styleView = (TextView) detailView.findViewById(R.id.styleValue);
			TextView regionView = (TextView) detailView.findViewById(R.id.regionValue);
			TextView priceView = (TextView) detailView.findViewById(R.id.priceValue);
			
			TextView addedView = (TextView) detailView.findViewById(R.id.dateAddedValue);
			
			RatingBar ratingBar = (RatingBar) detailView.findViewById(R.id.clrRatingVal);
			TextView ratingObserver = (TextView) detailView.findViewById(R.id.clrRatingObserver);
			
			TextView locationVal = (TextView) detailView.findViewById(R.id.locationVal);
			TextView tastingNoteVal = (TextView) detailView.findViewById(R.id.tastingNoteVal);
			TextView privateNoteVal = (TextView) detailView.findViewById(R.id.privateNoteVal);
			
			//TextView reviewCount = (TextView) detailView.findViewById(R.id.reviewCount);
			
			/*Button btnTweet = (Button)detailView.findViewById(R.id.tweet);*/
			RelativeLayout buyBtnSection = (RelativeLayout)detailView.findViewById(R.id.buySection);
					
			CheckBox drankCheckbox = (CheckBox)detailView.findViewById(R.id.drankCheckbox);
			CheckBox ownCheckbox = (CheckBox)detailView.findViewById(R.id.ownCheckbox);
			CheckBox wantCheckbox = (CheckBox)detailView.findViewById(R.id.wantCheckbox);
			
			Button btnShare = (Button) detailView.findViewById(R.id.share);
			
			addedView.setText(mBottle.getCork_created_at());
			nameView.setText(mBottle.getName());
			titleView.setText(mBottle.getName());
			int year = mBottle.getCork_year();
			TextView yearView = (TextView) detailView.findViewById(R.id.yearValue);
			if (year > 0)
				yearView.setText("" + year);
			else
				yearView.setText("" + mBottle.getYear());
			ratingView.setText(mBottle.getRating());
			String price = mBottle.getCork_price();
			
			priceView.setText((price != null) && (!price.equals("")) ? price : mBottle.getPrice());
			
			ratingBar.setRating(mBottle.getCork_rating());
			ratingObserver.setText("" + mBottle.getCork_rating());
			
			String location = mBottle.getLocation();
			locationVal.setText(((location == null) || (location.equals(""))) ? "n/a" : location);
			
			String privateNote = mBottle.getDescription();
			privateNoteVal.setText(((privateNote == null) || (privateNote == "")) ? "" : privateNote);
			
			String publicNote = mBottle.getPublic_note();
			tastingNoteVal.setText(((publicNote == null) || (publicNote == "")) ? "" : publicNote);
			
			drankCheckbox.setChecked(mBottle.isCork_drank());
			ownCheckbox.setChecked(mBottle.isCork_own());
			int ownCnt = mBottle.getCork_bottle_count();
			if ((ownCnt <= 0) || (! mBottle.isCork_own()))
				ownCnt = 0;
			ownCheckbox.setText("I Own " + ownCnt);
			wantCheckbox.setChecked(mBottle.isCork_want());
			
			if (defaultIcon == null)
			{
				defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
			}

			RemoteImageView riv = (RemoteImageView) findViewById(R.id.dtlWineThumb);
			if (riv != null)
			{
				boolean skipRemainingThumbProcessing = false;
				
				if (mBottle.getLocalImageResourceOnly() != null)
				{
					Drawable d = Drawable.createFromPath(mBottle.getLocalImageResourceOnly());
					if (d != null) {
						riv.setImageDrawable(d);
						skipRemainingThumbProcessing = true;
					}

				}
				
				if (!skipRemainingThumbProcessing)
				{
					String labelThumb = null;

					if ((mBottle.getCork_label() != null) && (! mBottle.getCork_label().equals("")))
						labelThumb = mBottle.getCork_label();
					else
						labelThumb = mBottle.getLabel_thumb();

					riv.setRemoteImage(labelThumb, defaultIcon);
				}
			}
			
			riv.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					Dialog dialog = new Dialog(DryncCorkDetail.this);
					dialog.setCancelable(true);
					dialog.setCanceledOnTouchOutside(true);

					dialog.setContentView(R.layout.imagezoom);
					dialog.setTitle("Label:");

					ImageView image = (ImageView) dialog.findViewById(R.id.image);
					image.setImageDrawable(((RemoteImageView)v).getDrawable());
					dialog.show();
				}

				});


			String varietal = bottle.getGrape();
			varietalView.setText(((varietal == null) || (varietal.equals("null")) || (varietal.equals(""))) ? "Unspecified" : varietal);
			String style = bottle.getStyle();
			styleView.setText(((style == null) || (style.equals("null")) || (style.equals(""))) ? "Unspecified" : style);
			regionView.setText(bottle.getRegion());
			
		/*	String reviewPlurality = ((mBottle.getReviewCount() <= 0) || (mBottle.getReviewCount() > 1)) ?
					" Reviews" : " Review";
			reviewCount.setText("" + mBottle.getReviewCount() + reviewPlurality); */
			
			Button cellarBtn = (Button) this.findViewById(R.id.cellarBtn);
			cellarBtn.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					DryncCorkDetail.this.finish();			
				}});
			
			Button editBtn = (Button) this.findViewById(R.id.editBtn);
			Button editInCellarBtn = (Button) this.findViewById(R.id.editInCellar);
			Button deleteBtn = (Button) this.findViewById(R.id.deleteBtn);
			
			final DryncDbAdapter dbAdapter = new DryncDbAdapter(this);
			
			OnClickListener addListener = new OnClickListener(){

				public void onClick(View v) {
					DryncCorkDetail.this.launchEditCork();
				}};
				
			editBtn.setOnClickListener(addListener);
			editInCellarBtn.setOnClickListener(addListener);
			
			deleteBtn.setOnClickListener(new OnClickListener() {

				public void onClick(View arg0) {
					boolean postSuccess = DryncProvider.postDelete(mBottle, deviceId);
					if (!postSuccess)
					{
						// failed post, post later.
						
						mBottle.setNeedsServerUpdate(true);
						mBottle.setUpdateType(Cork.UPDATE_TYPE_DELETE);	
					}
					// persist to database.
					dbAdapter.open();
					boolean success = dbAdapter.deleteCork(mBottle.get_id());
					dbAdapter.close();
					
					DryncCorkDetail.this.setResult(DryncCellar.CELLAR_NEEDS_REFRESH);
					DryncCorkDetail.this.finish();
					
				}});
			
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
						emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, DryncUtils.buildShareEmailText(DryncCorkDetail.this, mBottle));  

						/* Send it off to the Activity-Chooser */  
						DryncCorkDetail.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
					}

				});
			}

			Button btnTweet = (Button)detailView.findViewById(R.id.social);
			btnTweet.setOnClickListener(new OnClickListener(){
				public void onClick(View arg0) {
					if (!hasConnectivity())
					{
						new AlertDialog.Builder(DryncCorkDetail.this)
						.setMessage(getResources().getString(R.string.nonettweet))
								.setNegativeButton("OK", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
									}})
									.show();
					}
					else
					{
						final boolean twitterAuth = DryncUtils.isTwitterAuthorized(DryncCorkDetail.this);
						final boolean fbAuth = DryncUtils.isFacebookAuthorized(DryncCorkDetail.this);
						
						if (twitterAuth || fbAuth)
						{
							progressDlg =  new ProgressDialog(DryncCorkDetail.this);
							progressDlg.setTitle("Dryncing...");
							progressDlg.setMessage("Telling your friends...");
							progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
							progressDlg.show();
							
							Thread t = new Thread()
							{
								public void run() {

									if (twitterAuth)
									{
										boolean result = DryncProvider.postTweetToFriends(mBottle, deviceId);
									}

									if (fbAuth)
									{
										boolean result2 = DryncProvider.postScrawlToFriends(mBottle, deviceId);
									}

									DryncCorkDetail.this.tweetHandler.post(new Runnable(){

										public void run() {
											
											if (progressDlg != null)
											{
												progressDlg.dismiss();
											}
											
											Toast tst = Toast.makeText(getApplicationContext(),getResources().getString(R.string.socialupdated), Toast.LENGTH_SHORT);
											tst.setGravity(Gravity.CENTER, 0, 0);
											tst.show();											
										}});
								}
							};
							t.start();
						}
						else
						{
							Toast tst = Toast.makeText(getApplicationContext(),getResources().getString(R.string.socialnotenabled), Toast.LENGTH_SHORT);
							tst.setGravity(Gravity.CENTER, 0, 0);
							tst.show();
						}
					}
				}});

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
	}
	
	private void launchEditCork() {
		// mBottle should be set by the detail view, if not, return;
		if (mBottle == null)
			return;
		
		Intent twIntent = new Intent(this, DryncAddToCellar.class);
		twIntent.putExtra("cork", mBottle);
		startActivityForResult(twIntent, ADDTOCELLAR_RESULT);  
	}
	
	
	/*private void launchReviews(Bottle bottle) {
		Intent twIntent = new Intent(this, DryncDetailReviews.class);
		twIntent.putExtra("bottle", bottle);
		startActivity(twIntent);  
		
	}*/

	/*class WineReviewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		public Bottle bottle;
		private final LayoutInflater mInflater;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineReviewAdapter(Bottle wine) {
			bottle = wine;
			mInflater = (LayoutInflater) DryncCorkDetail.this.getSystemService(
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
	
			if (review.getText() != null && review.getText().contains("href")) // contains html
			{
				reviewText.setText("");
				reviewText.setVisibility(View.INVISIBLE);
			}
			else
			{
				reviewText.setText(review.getText());
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
	}*/



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		

		switch (requestCode) {
		case ADDTOCELLAR_RESULT:
			// This is the standard resultCode that is sent back if the
			// activity crashed or didn't doesn't supply an explicit result.
			if (resultCode == RESULT_CANCELED){
				// do nothing.
			} 
			else {
				/*if (resultCode == DryncCellar.CELLAR_NEEDS_REFRESH)
	            	{
	            		this.doStartupFetching(false);
	            	}*/
				this.startDryncCellarActivity();
			}
			break;
		case MYACCOUNT_RESULT:
		{

			break;
		}
		default:
			break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);

	}

	class WineAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		private final List<Cork> mWines;
		private final LayoutInflater mInflater;
		private final Drawable defaultIcon;
		boolean mDone = false;
		boolean mFlinging = false;
		
		public WineAdapter(List<Cork> wines) {
			mWines = wines;
			mInflater = (LayoutInflater) DryncCorkDetail.this.getSystemService(
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
				String corkThumbUrl = null;
				if ((wine.getLabel_thumb() != null) || ((Cork)wine).getCork_label() != null)
				{
					if ((wine.getLabel_thumb() != null) && (! wine.getLabel_thumb().equals("")))
						corkThumbUrl = wine.getLabel_thumb();
					
					if ((((Cork)wine).getCork_label() != null) && (! ((Cork)wine).getCork_label().equals("")))
						corkThumbUrl = ((Cork)wine).getCork_label();
				}
				
				if (corkThumbUrl != null)
				{
					if (corkThumbUrl.startsWith("http"))
					{
						wineThumb.setLocalURI(DryncUtils.getCacheFileName(corkThumbUrl));
						wineThumb.setRemoteURI(corkThumbUrl);
					}
					else
					{
						wineThumb.setLocalURI(corkThumbUrl);
						wineThumb.setRemoteURI(null);
					}
					wineThumb.setImageDrawable(defaultIcon);
					wineThumb.setUseDefaultOnly(false);
				}
				else
				{
					wineThumb.setUseDefaultOnly(true);
					wineThumb.setImageDrawable(defaultIcon);
				}
				/*if (wine.getLabel_thumb() != null)
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
				}*/
			}
			
			TextView wineNameText = (TextView) view.findViewById(R.id.wineName);
			wineNameText.setText(wine.getName());
			
			TextView priceText = (TextView) view.findViewById(R.id.priceValue);
			priceText.setText(wine.getPrice());
			
			TextView ratingText = (TextView) view.findViewById(R.id.ratingValue);
			ratingText.setText(wine.getRating());
			
			/*TextView reviewText = (TextView) view.findViewById(R.id.reviewValue);
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			DryncCorkDetail.this.finish();
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	/*private void populateReviewTable(TableLayout table, Bottle bottle)
	{
		table.removeAllViews();
		
		for (int i=0,n=bottle.getReviewCount();i<n;i++)
		{			
			Review review = mBottle.getReview(i);
			if (review == null)
				continue;
			
			if (mMainInflater == null)
			{
				mMainInflater = (LayoutInflater) DryncCorkDetail.this.getSystemService(
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
						Toast noReviewUrl = Toast.makeText(DryncCorkDetail.this, getResources().getString(R.string.noreviewurl), Toast.LENGTH_LONG);
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
	*/
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
	
	@Override
	protected void doStartupFetching() {
		// skip this fetch in cellar view.
	}
}

