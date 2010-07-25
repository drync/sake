/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 * otherwise...
 * 
 * @author Michael Brindamour
 * 
 */
package com.drync.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RatingBar.OnRatingBarChangeListener;

import com.drync.android.helpers.Base64;
import com.drync.android.helpers.Result;
import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.ui.RemoteImageView;
import com.drync.android.widget.NumberPicker;

public class DryncAddToCellar extends DryncBaseActivity {

	final Handler mHandler = new Handler();
	private Bottle mBottle = null;
	private boolean isEdit = false;
	private String deviceId;
	LayoutInflater mMainInflater;
	ViewFlipper flipper;
	
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
		final EditText locationVal = (EditText)addView.findViewById(R.id.atcLocationVal);
		final CheckBox wantVal = (CheckBox)addView.findViewById(R.id.atcWantValue);
		final CheckBox drankVal = (CheckBox)addView.findViewById(R.id.atcDrankValue);
		//final EditText ownVal = (EditText)addView.findViewById(R.id.atcOwnCountVal);
		final CheckBox ownVal = (CheckBox)addView.findViewById(R.id.atcOwnCountValue);
		//final TextView ownLbl = (TextView)addView.findViewById(R.id.atcOwnCountLbl);
		
		ratingbar.setOnRatingBarChangeListener(new OnRatingBarChangeListener(){

			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				ratingVal.setText("" + rating);
				
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
				
				public void onClick(View v) {
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
					//this.mBottle.setLocalImageResourceOnly(newpath);
					//this.mBottle.setLabel_thumb(newpath);
					//cork.setLabel_thumb(DryncAddToCellar.this.localImageResourcePath);
					if ((DryncAddToCellar.this.localImageResourcePath != null) && 
					   (!DryncAddToCellar.this.localImageResourcePath.equals("")))
					{
						cork.setLocalImageResourceOnly(DryncAddToCellar.this.localImageResourcePath);
					
					}
					cork.setCork_labelInline(DryncAddToCellar.this.imageBase64Representation);
					
					cork.setCork_price(priceVal.getEditableText().toString());
					cork.setName(nameVal.getEditableText().toString());
					cork.setCork_year(Integer.parseInt(yearVal.getEditableText().toString()));
					//cork.setCork_created_at(System.currentTimeMillis());
					cork.setGrape(varietalVal.getEditableText().toString());
					cork.setRegion(regionVal.getEditableText().toString());
					cork.setCork_rating(ratingbar.getRating());
					cork.setPublic_note(tastingNotesVal.getEditableText().toString());
					cork.setDescription(privateNotesVal.getEditableText().toString());
					cork.setLocation(locationVal.getEditableText().toString());
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
					progressDlg.setMessage("Saving cork...");
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
				}};

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
			ratingBar.setRating(((Cork)mBottle).getCork_rating());
			ratingVal.setText("" + ((Cork)mBottle).getCork_rating());
			
			DryncAddToCellar.this.localImageResourcePath = ((Cork)mBottle).getLocalImageResourceOnly();
			DryncAddToCellar.this.imageBase64Representation = ((Cork)mBottle).getCork_labelInline();
			
			// set style field:
			if (((Cork)mBottle).getStyle() != null)
				styleVal.setSelection(styleSpnAdapter.getPosition(((Cork)mBottle).getStyle().toString()));
			
			tastingNotesVal.setText(((Cork)mBottle).getPublic_note());
			privateNotesVal.setText(((Cork)mBottle).getDescription());
			locationVal.setText(((Cork)mBottle).getLocation());
			
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
			
			ratingVal.setText("" + 0);
			
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
}

