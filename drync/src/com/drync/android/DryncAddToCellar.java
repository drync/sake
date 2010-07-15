/**
 * Credit where credit's due... the pattern for the image lazy loading was borrowed
 * from Evan Charlton at: http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 * otherwise...
 * 
 * @author Michael Brindamour
 * 
 */
package com.drync.android;

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

	boolean displaySearch = true;
	boolean displayTopWinesBtns = false;

	int lastSelectedTopWine = -1;
	
	int ownValueHolder = 0;

	LinearLayout searchView;
	ScrollView detailView;
	ScrollView reviewView;
	ScrollView addView;

	boolean rebuildDetail = false;
	boolean rebuildReviews = false;
	boolean rebuildAddToCellar = false;
	boolean buildOnceAddToCellar = true;

	Drawable defaultIcon = null;
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == RemoteImageView.CAMERA_PIC_REQUEST) {  
			   Bitmap thumbnail = (Bitmap) data.getExtras().get("data");  
			   
			   RemoteImageView image = (RemoteImageView) findViewById(R.id.atcWineThumb);  
			   
			   if (image != null)
			   {
				   String newpath = image.saveNewImage(thumbnail);
				   this.mBottle.setLocalImageResourceOnly(newpath);
				   image.setImageBitmap(thumbnail);
				   this.mBottle.setLabel_thumb(newpath);
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

					if (isEdit)
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
							if (postresult.getContents().size() > 0)
								cork = postresult.getContents().get(0);
							
							cork.setNeedsServerUpdate(false);
							cork.setUpdateType(Cork.UPDATE_TYPE_NONE);
						}
						// persist to database.
						dbAdapter.open();
						boolean result = dbAdapter.updateCork(cork);
						dbAdapter.close();
						
						if (result)
						{
							// successful
							Toast successfulUpdate = Toast.makeText(DryncAddToCellar.this, 
									getResources().getString(R.string.successfulcellarupdate), Toast.LENGTH_LONG);
							successfulUpdate.show();
							
						}
						
					}
					else
					{
						try
						{
						Result<Cork> postresult  = DryncProvider.postCreateOrUpdate(DryncAddToCellar.this, cork, deviceId, DryncUtils.isFreeMode());
						boolean postSuccess = postresult.isResult();
						
						if (postresult.getContents().size() > 0)
							cork = postresult.getContents().get(0);
						
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


							if (result >= 0)
							{
								// successful
								Toast successfulAdd = Toast.makeText(DryncAddToCellar.this, 
										getResources().getString(R.string.successfulcellaradd), Toast.LENGTH_LONG);
								successfulAdd.show();

							}
						}
						catch (DryncFreeCellarExceededException e)
						{
							// successful
							Toast failedAdd = Toast.makeText(DryncAddToCellar.this, 
									getResources().getString(R.string.exceededcellaradd) + 
									"\n\n" + getResources().getString(R.string.exceededcellaradd2) +
									" " + getResources().getString(R.string.exceededcellaradd3), Toast.LENGTH_LONG);
							failedAdd.setGravity(Gravity.CENTER, 0, 0);
							failedAdd.show();
						}
						finally
						{
							dbAdapter.close();
						}
					}

					
					DryncAddToCellar.this.setResult(DryncCellar.CELLAR_NEEDS_REFRESH);
					DryncAddToCellar.this.finish();
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
		
		wineThumb.setRemoteImage(mBottle.getLabel_thumb(), defaultIcon);
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
}

