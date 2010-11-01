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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.drync.android.helpers.Base64;
import com.drync.android.helpers.Result;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Venue;
import com.drync.android.ui.RemoteImageView;
import com.drync.android.widget.NumberPicker;

public class DryncAddToCellar extends DryncBaseActivity {

	final Handler mHandler = new Handler();
	private Bottle mBottle = null;
	private boolean isEdit = false;
	private String deviceId;
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
	TextView locationVal;
	boolean ratingTouched = false;
	ArrayList<String> venuestrs = null;
	final String CUSTOM_VENUE = "Custom Venue...";
	ArrayList<Venue> venues;
	Object venuelock = new Object();
	
	String curLocationLat;
	String curLocationLong;
	String curVenueLat;
	String curVenueLong;
	
	private ProgressDialog progressDlg = null;

	boolean displaySearch = true;
	boolean displayTopWinesBtns = false;

	int lastSelectedTopWine = -1;
	
	int ownValueHolder = 0;
	
	String localImageResourcePath = null;
	String imageBase64Representation = null;

	LinearLayout searchView;
	ScrollView detailView;
	ScrollView reviewView;
	ScrollView addView;

	boolean rebuildDetail = false;
	boolean rebuildReviews = false;
	boolean rebuildAddToCellar = false;
	boolean buildOnceAddToCellar = true;

	Drawable defaultIcon = null;
	
	final Runnable mHandleLongTransaction = new Runnable()
	{
		public void run()
		{
			
			if (progressDlg != null)
			{
				progressDlg.dismiss();
			}
			
			DryncAddToCellar.this.setResult(DryncCellar.CELLAR_NEEDS_REFRESH);
			DryncAddToCellar.this.finish();
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RemoteImageView.CAMERA_PIC_REQUEST) { 
			if (data != null)
			{
				Bitmap thumbnail = (Bitmap) data.getExtras().get("data");  

				RemoteImageView image = (RemoteImageView) findViewById(R.id.atcWineThumb);  

				if (image != null)
				{
					String newpath = image.saveNewImage(thumbnail);
					DryncAddToCellar.this.localImageResourcePath = newpath;
					image.setImageBitmap(thumbnail);

					String base64encoding = null;
					try {
						base64encoding = Base64.encodeFromFile(newpath);
						DryncAddToCellar.this.imageBase64Representation = base64encoding;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		else if (requestCode == LOCATION_CHOOSER_RESULT)
		{
			if (resultCode == RESULT_OK)
			{
				Venue venue = data.getExtras().getParcelable("selectedVenue");
				locationVal.setText(venue.getName());
				curVenueLat = venue.getGeolat();
				curVenueLong = venue.getGeolong();
			}
			//else // cancelled, do nothing
		}
	}

	SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addtocellar);
		
		initializeAds();

		Bundle extras = getIntent().getExtras();
		Bottle bottle = (Bottle) (extras != null ? extras.getParcelable("bottle") : null);
		
		if (bottle == null)
		{
			bottle = (Cork) (extras != null ? extras.getParcelable("cork") : null);
			isEdit = true;
		}

		if (defaultIcon == null)
		{
			defaultIcon = getResources().getDrawable(R.drawable.bottlenoimage);
		}
		
		addView = (ScrollView) this.findViewById(R.id.addtocellarview); 
		deviceId = DryncUtils.getDeviceId(getContentResolver(), this);
		
		// initialize based on last known location.
		Thread t = new Thread()
		{

			@Override
			public void run() {
				super.run();
				
				synchronized(venuelock)
				{
					venues = DryncProvider.getInstance().
							getVenues(DryncUtils.getLastKnownLocationLat(DryncAddToCellar.this),
									  DryncUtils.getLastKnownLocationLong(DryncAddToCellar.this));
				}
			}
		};
		
		t.start();
		
		launchAddToCellar(bottle);
	}

	private void launchAddToCellar(Bottle bottle) {

		mBottle = bottle;

		// mBottle should be set by the detail view, if not, return;
		if (mBottle == null)
			return;

		final TextView atcTitle = (TextView) addView.findViewById(R.id.addToCellarTitle);
		
		if (isEdit)
		{
			atcTitle.setText("Edit Cork");
		}
		
		EditText priceVal = (EditText) addView.findViewById(R.id.atcPriceVal);
		EditText nameVal = (EditText) addView.findViewById(R.id.atcWineName);
		
		final AutoCompleteTextView yearVal = (AutoCompleteTextView) addView.findViewById(R.id.atcYearVal);
		//final EditText varietalVal = (EditText) addView.findViewById(R.id.atcVarietalVal);
		final AutoCompleteTextView varietalVal = (AutoCompleteTextView) addView.findViewById(R.id.atcVarietalVal);
		final EditText regionVal = (EditText) addView.findViewById(R.id.atcRegionVal);
		final DryncDbAdapter dbAdapter = new DryncDbAdapter(this);

		RemoteImageView wineThumb = (RemoteImageView) addView.findViewById(R.id.atcWineThumb);
		wineThumb.setLaunchCameraOnClick(DryncAddToCellar.this, true);
		final RatingBar ratingbar = (RatingBar) addView.findViewById(R.id.atcRatingVal);
		final TextView ratingVal = (TextView) addView.findViewById(R.id.atcRatingObserver);
		
		final Spinner styleVal = (Spinner)addView.findViewById(R.id.atcStyleVal);
		final EditText tastingNotesVal = (EditText)addView.findViewById(R.id.atcTastingNoteVal);
		final EditText privateNotesVal = (EditText)addView.findViewById(R.id.atcPrivateNoteVal);
		locationVal = (TextView)addView.findViewById(R.id.atcLocationVal);
		final CheckBox wantVal = (CheckBox)addView.findViewById(R.id.atcWantValue);
		final CheckBox drankVal = (CheckBox)addView.findViewById(R.id.atcDrankValue);
		//final EditText ownVal = (EditText)addView.findViewById(R.id.atcOwnCountVal);
		final CheckBox ownVal = (CheckBox)addView.findViewById(R.id.atcOwnCountValue);
		//final TextView ownLbl = (TextView)addView.findViewById(R.id.atcOwnCountLbl);
		
		
		locationVal.setClickable(true);
		//locationVal.setLongClickable(true);
		locationVal.setHint("Click to set a location...");
		
		
		locationVal.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				if (venues == null) // still loading venues, please wait
				{
					progressDlg =  new ProgressDialog(DryncAddToCellar.this);
					progressDlg.setTitle("Dryncing...");
					progressDlg.setMessage("Loading venues...");
					progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDlg.show();
					
					final Handler chooserHandler = new Handler();
					final Runnable startChooserThread = new Runnable()
					{
						public void run()
						{
							startLocationChooser();
							if ((progressDlg != null) && (progressDlg.isShowing()))
							{
								try
								{
									progressDlg.dismiss();
								}
								catch (IllegalArgumentException e)
								{
									Log.e("PROGDLG_ERROR", "Progress Dialog not attached.");
								}
							}
						}
					};
					
					Thread t = new Thread()
					{
						public void run() {
							while (venues == null)
							{
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							chooserHandler.post(startChooserThread);
						}
					};
					t.start();
				}
				else
				{
					startLocationChooser();
				}
			}});
		
		ratingbar.setOnTouchListener(new OnTouchListener(){

			public boolean onTouch(View arg0, MotionEvent arg1) {
				StringBuilder bldr = new StringBuilder();
				bldr.append("(");
				bldr.append(ratingbar.getRating());
				bldr.append(")");
				ratingVal.setText(bldr.toString());
				DryncAddToCellar.this.ratingTouched = true;
				return false;
			}});
		
		ratingbar.setOnRatingBarChangeListener(new OnRatingBarChangeListener(){

			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				StringBuilder bldr = new StringBuilder();
				bldr.append("(");
				bldr.append(rating);
				bldr.append(")");
				ratingVal.setText(bldr.toString());
				if (fromUser)
					DryncAddToCellar.this.ratingTouched = true;				
			}});
		
		ownVal.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v) {
				Context mContext = DryncAddToCellar.this;
				Dialog dialog = new Dialog(mContext);

				dialog.setContentView(R.layout.number_picker_pref);
				dialog.setTitle("I Own ");

				final NumberPicker picker = (NumberPicker)dialog.findViewById(R.id.pref_num_picker);
				Button okBtn = (Button)dialog.findViewById(R.id.okBtn);
				Button cancelBtn = (Button)dialog.findViewById(R.id.cancelBtn);

				picker.setCurrent(ownValueHolder);
				final Dialog thisDialog = dialog;
				cancelBtn.setOnClickListener(new OnClickListener(){

					public void onClick(View arg0) {
						thisDialog.cancel();
					}});

				okBtn.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						ownValueHolder = picker.getCurrent();
						ownVal.setText("I Own " + picker.getCurrent());
						thisDialog.cancel();
					}});
				
				dialog.show();
				
				dialog.setOnCancelListener(new Dialog.OnCancelListener(){

					public void onCancel(DialogInterface dialog) {
						if (ownValueHolder > 0)
						{
							ownVal.setChecked(true);
						}
						else
						{
							ownVal.setChecked(false);
						}
					}});
			}



		});
		

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

			 varietalSpnAdapter = ArrayAdapter.createFromResource(this, R.array.varietal_array, android.R.layout.simple_spinner_dropdown_item);
			  varietalSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			  varietalVal.setAdapter(varietalSpnAdapter); 

			// regionSpnAdapter = ArrayAdapter.createFromResource(this, R.array.region_array, android.R.layout.simple_spinner_dropdown_item);
			// regionSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			//  regionVal.setAdapter(regionSpnAdapter); 

			styleSpnAdapter = ArrayAdapter.createFromResource(
					this, R.array.style_array, android.R.layout.simple_spinner_item);
			styleSpnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
			styleVal.setAdapter(styleSpnAdapter);

			Button addToCellarBtn = (Button)addView.findViewById(R.id.addToCellar);
			Button saveBtn = (Button)addView.findViewById(R.id.saveBtn);

			OnClickListener saveListener = 	new OnClickListener(){

				EditText priceVal = (EditText) addView.findViewById(R.id.atcPriceVal);
				EditText nameVal = (EditText) addView.findViewById(R.id.atcWineName);
				Spinner styleVal = (Spinner)addView.findViewById(R.id.atcStyleVal);
				TextView locationVal = (TextView) addView.findViewById(R.id.atcLocationVal);
				
				
				public void onClick(View v) 
				{
					final Handler saveHandler = new Handler();
					
					final Runnable saveProc = new Runnable()
					{
						public void run()
						{
							//populate cork object
							Cork cork = null;
							if (isEdit)
							{
								cork = ((Cork)mBottle);
							}
							else
							{
								cork = new Cork();
								cork.setCork_uuid(DryncUtils.nextUuid(DryncAddToCellar.this));
								cork.setBottle_Id(mBottle.getBottle_Id());
								cork.setYear(mBottle.getYear());
							}

							if ((DryncAddToCellar.this.localImageResourcePath != null) && 
									(!DryncAddToCellar.this.localImageResourcePath.equals("")))
							{
								cork.setLocalImageResourceOnly(DryncAddToCellar.this.localImageResourcePath);

							}
							cork.setCork_labelInline(DryncAddToCellar.this.imageBase64Representation);

							cork.setCork_price(priceVal.getEditableText().toString());
							cork.setName(nameVal.getEditableText().toString());
							cork.setCork_year(Integer.parseInt(yearVal.getEditableText().toString().trim()));
							//cork.setCork_created_at(System.currentTimeMillis());
							cork.setGrape(varietalVal.getEditableText().toString());
							cork.setRegion(regionVal.getEditableText().toString());

							if (DryncAddToCellar.this.ratingTouched == true)
							{
								cork.setCork_rating(ratingbar.getRating());
							}
							else
							{
								cork.setCork_rating(null);
							}

							cork.setPublic_note(tastingNotesVal.getEditableText().toString());
							cork.setDescription(privateNotesVal.getEditableText().toString());
							cork.setLocation(locationVal.getText().toString());
							cork.setLocationLat(curVenueLat);
							cork.setLocationLong(curVenueLong);
							cork.setCork_want(wantVal.isChecked());
							cork.setCork_drank(drankVal.isChecked());
							//cork.setStyle(styleVal.getSelectedItem().toString());
							String styleText = (String) styleVal.getAdapter().getItem(styleVal.getSelectedItemPosition()).toString();
							cork.setStyle(styleText);
							int ownCount = 0;

							ownCount = ownValueHolder;

							cork.setCork_bottle_count(ownCount);

							if (ownCount > 0)
								cork.setCork_own(true);
							else
								cork.setCork_own(false);

							final Cork saveCork = cork;

							progressDlg =  new ProgressDialog(DryncAddToCellar.this);
							progressDlg.setTitle("Dryncing...");
							progressDlg.setMessage("Saving wine...");
							progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
							progressDlg.show();

							Thread saveThread = new Thread()
							{
								public void run()
								{
									boolean result = false;
									if (isEdit)
									{
										result = doEditSave(saveCork, dbAdapter);
									}
									else
									{
										result = doCreateSave(saveCork, dbAdapter);
									}


									//doStartupFetching(true);


									mHandler.post(mHandleLongTransaction);
								}
							};

							saveThread.start();
						}

					};
				
					boolean rxPromptResult = false;
					// check for location, if none set, prompt to set one before saving.
					String loctext = locationVal.getText().toString();
					if ((loctext == null) || (loctext.equals(""))) // no location
					{
						AlertDialog.Builder builder = new AlertDialog.Builder(DryncAddToCellar.this);
						builder.setMessage("Select a location for this wine?")
						       .setCancelable(true)
						       .setNeutralButton("Save", new DialogInterface.OnClickListener() {
						           public void onClick(DialogInterface dialog, int id) {
						                dialog.dismiss();
						                saveHandler.post(saveProc);
						           }
						       })
						       .setPositiveButton("Select Location", new DialogInterface.OnClickListener() {
						           public void onClick(DialogInterface dialog, int id) {
						        	   locationVal.performClick();
						        	   dialog.dismiss();
						           }
						       })
						       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
						AlertDialog alert = builder.show();
					}
					else
					{
						saveProc.run();
					}
				};

			};

				if (addToCellarBtn != null)
				{
					addToCellarBtn.setOnClickListener(saveListener);
				}	

				if (saveBtn != null)
				{
					saveBtn.setOnClickListener(saveListener);
				}

				Button cancelBtn = (Button)addView.findViewById(R.id.cancelBtn);
				if (cancelBtn != null)
				{
					cancelBtn.setOnClickListener(new OnClickListener(){

						public void onClick(View v) {
							// in order 
							DryncAddToCellar.this.finish();
						}});
				}

				buildOnceAddToCellar = false;
		}

		int position = -1;
		if (mBottle.getYear() > 0)
			position = yearSpnAdapter.getPosition("" + mBottle.getYear());
		else
			position = yearSpnAdapter.getPosition("" + year);

		nameVal.setText(mBottle.getName());
		
		if (isEdit)
		{
			int cyear = ((Cork)mBottle).getCork_year();
			yearVal.setText("" + (cyear == 0 ? mBottle.getYear() : cyear));
			String price = ((Cork)mBottle).getCork_price();
			priceVal.setText((price == null || price.equals("")) ? mBottle.getPrice() : price);
			RatingBar ratingBar = (RatingBar)addView.findViewById(R.id.atcRatingVal);
			
			if (((Cork)mBottle).getCork_rating() == null || ((Cork)mBottle).getCork_rating() < 0)
			{
				this.ratingTouched = false;
				ratingBar.setRating((float) 0.0);
				StringBuilder bldr = new StringBuilder();
				bldr.append("(");
				bldr.append("N/A");
				bldr.append(")");
				ratingVal.setText(bldr.toString());
			}
			else
			{
				ratingBar.setRating(((Cork)mBottle).getCork_rating());
				StringBuilder bldr = new StringBuilder();
				bldr.append("(");
				bldr.append(((Cork)mBottle).getCork_rating());
				bldr.append(")");
				ratingVal.setText(bldr.toString());
			}
			
			DryncAddToCellar.this.localImageResourcePath = ((Cork)mBottle).getLocalImageResourceOnly();
			DryncAddToCellar.this.imageBase64Representation = ((Cork)mBottle).getCork_labelInline();
			
			// set style field:
			if (((Cork)mBottle).getStyle() != null)
				styleVal.setSelection(styleSpnAdapter.getPosition(((Cork)mBottle).getStyle().toString()));
			
			tastingNotesVal.setText(((Cork)mBottle).getPublic_note());
			privateNotesVal.setText(((Cork)mBottle).getDescription());
			locationVal.setText(((Cork)mBottle).getLocation());
			curVenueLat = (((Cork)mBottle).getLocationLat());
			curVenueLong = (((Cork)mBottle).getLocationLong());
			
			wantVal.setChecked(((Cork)mBottle).isCork_want());
			drankVal.setChecked(((Cork)mBottle).isCork_drank());
			
			ownValueHolder = ((Cork)mBottle).getCork_bottle_count();
			ownVal.setChecked(ownValueHolder > 0);
			ownVal.setText("I Own " + ((Cork)mBottle).getCork_bottle_count());
			
			varietalVal.setText("" + ((Cork)mBottle).getGrape());
			
		}
		else
		{
			yearVal.setText("" + mBottle.getYear());
			priceVal.setText(mBottle.getPrice());
			
			int stylepos = styleSpnAdapter.getPosition(mBottle.getStyle());
			if ((stylepos < styleSpnAdapter.getCount() || (stylepos > styleSpnAdapter.getCount())))
				styleVal.setSelection(styleSpnAdapter.getPosition("Other"));
			else
				styleVal.setSelection(stylepos);
			
			varietalVal.setText("" + mBottle.getGrape());
			
			StringBuilder bldr = new StringBuilder();
			bldr.append("(");
			bldr.append("N/A");
			bldr.append(")");
			ratingVal.setText(bldr.toString());
			
		}
		
		regionVal.setText("" + mBottle.getRegion());
		
		boolean skipRemainingThumbProcessing = false;
		
		if (mBottle.getLocalImageResourceOnly() != null)
		{
			Drawable d = Drawable.createFromPath(mBottle.getLocalImageResourceOnly());
			if (d != null) {
				wineThumb.setImageDrawable(d);
				skipRemainingThumbProcessing = true;
			}

		}

		if (!skipRemainingThumbProcessing)
		{
			String labelThumb = null;

			if (mBottle instanceof Cork)
			{
				Cork corkBottle = (Cork)mBottle;
				if ((corkBottle.getCork_label() != null) && (!corkBottle.getCork_label().equals("")))
					labelThumb = corkBottle.getCork_label();
			}
			else
				labelThumb = mBottle.getLabel_thumb();

			wineThumb.setRemoteImage(labelThumb, defaultIcon);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			//boolean retval = false;

			DryncAddToCellar.this.finish();
			/*
			if ((flipper.getCurrentView() == detailView) || (flipper.getCurrentView() == reviewView) ||
					(flipper.getCurrentView() == addView))
			{
				showPrevious();
				retval = true;
			}

			if (retval)
				return true; */
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public int getMenuItemToSkip() {
		return -1;
	}
	
	@Override
	protected void doStartupFetching() {
		// skip this fetch in cellar view.
	}
	
	private boolean doEditSave(Cork cork, DryncDbAdapter dbAdapter)
	{
		Result<Cork> postresult = DryncProvider.postUpdate(cork, deviceId);
		boolean postSuccess = postresult.isResult();
		if (!postSuccess)
		{
			// failed post, post later.

			cork.setNeedsServerUpdate(true);
			cork.setUpdateType(Cork.UPDATE_TYPE_UPDATE);	
		}
		else
		{
			Cork resultCork = null;
			if (postresult.getContents().size() > 0)
			{
				resultCork = postresult.getContents().get(0);
				if (resultCork != null)
				{
					cork.setCork_created_at(resultCork.getCork_created_at());
				}
				
				//  This might be a bit of hack, but after an update
				// the xml doesn't have the new image, so we need to
				// keep using the local copy for now
				if ((DryncAddToCellar.this.localImageResourcePath != null) && 
					(!DryncAddToCellar.this.localImageResourcePath.equals("")))
				{
					cork.setLocalImageResourceOnly(DryncAddToCellar.this.localImageResourcePath);
					cork.setCork_labelInline(DryncAddToCellar.this.imageBase64Representation);
				}
				
			}
			
			cork.setNeedsServerUpdate(false);
			cork.setUpdateType(Cork.UPDATE_TYPE_NONE);
		}
		// persist to database.
		dbAdapter.open();
		boolean result = dbAdapter.updateCork(cork);
		dbAdapter.close();
		
		return result;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		super.onLocationChanged(location);
		
		curLocationLat = "" + location.getLatitude();
		curLocationLong = "" + location.getLongitude();
		
		synchronized(venuelock)
		{
			venues = DryncProvider.getInstance().getVenues(location);
		}
		ArrayList<String> venuestrs = new ArrayList<String>();

		venuestrs.add(DryncAddToCellar.this.CUSTOM_VENUE);
		
		boolean foundSelected = false;
		for (Venue venue : venues)
		{
			
				venuestrs.add(venue.getName());
			
		}

		DryncAddToCellar.this.venuestrs = venuestrs;
	}

	private boolean doCreateSave(Cork cork, DryncDbAdapter dbAdapter)
	{
		try
		{
		Result<Cork> postresult  = DryncProvider.postCreateOrUpdate(DryncAddToCellar.this, cork, deviceId, DryncUtils.isFreeMode());
		boolean postSuccess = postresult.isResult();
		
		Cork resultCork = null;
		if (postresult.getContents().size() > 0)
		{
			resultCork = postresult.getContents().get(0);
			if (resultCork != null)
			{
				cork.setCork_created_at(resultCork.getCork_created_at());
				cork.setCork_id(resultCork.getCork_id());
			}
			
		//  This might be a bit of hack, but after an update
			// the xml doesn't have the new image, so we need to
			// keep using the local copy for now
			if ((DryncAddToCellar.this.localImageResourcePath != null) && 
				(!DryncAddToCellar.this.localImageResourcePath.equals("")))
			{
				cork.setLocalImageResourceOnly(DryncAddToCellar.this.localImageResourcePath);
				cork.setCork_labelInline(DryncAddToCellar.this.imageBase64Representation);
			}
		}
		
		if (!postSuccess)
		{
			// failed post, post later.

			cork.setNeedsServerUpdate(true);
			cork.setUpdateType(Cork.UPDATE_TYPE_INSERT);	
		}
		// persist to database.
		dbAdapter.open();
		long result = -1;
		result = dbAdapter.insertCork(cork);


		return result >= 0;		
		
		}
		catch (DryncFreeCellarExceededException e)
		{
			return false;
		}
		finally
		{
			dbAdapter.close();
		}
		
	}
	

	@Override
	public boolean isTrackGPS() {
		return true;
	}
	
	public void startLocationChooser()
	{
		Intent twIntent = new Intent(DryncAddToCellar.this, DryncLocationChooser.class);
		twIntent.putExtra("curSelection", locationVal.getText().toString());
		twIntent.putExtra("curSelectionLat", curVenueLat);
		twIntent.putExtra("curSelectionLong", curVenueLong);
		twIntent.putExtra("curLocationLat", curLocationLat);
		twIntent.putExtra("curLocationLong", curLocationLong);
		
		twIntent.putParcelableArrayListExtra("venues", venues);
		
		startActivityForResult(twIntent, LOCATION_CHOOSER_RESULT);  
	}
	
}

