package com.drync.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VenueRelativeLayout;
import com.drync.android.objects.Venue;

public class DryncLocationChooser extends DryncBaseActivity {

	TextView advice;
	final String BLANK_VENUE = "";
	final String CUSTOM_VENUE = "Custom Venue...";
	String curSelection;
	String curSelectionLat;
	String curSelectionLong;
	String curLocationLat;
	String curLocationLong;
	ArrayList<Parcelable> venues;
	boolean loaded = false;
	List<Venue> usedVenues;
	
	Venue selectedVenue;
	Venue typeaheadSelectedVenue;
	
	LinearLayout listholder;
	ListView mList;
	ListAdapter mAdapter;
	Button saveBtn;
	Button cancelBtn;
	ImageLoader imageLoader;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		Bundle extras = getIntent().getExtras();
		curSelection = (String) (extras != null ? extras.getString("curSelection") : null);
		curSelectionLat = (String) (extras != null ? extras.getString("curSelectionLat") : null);
		curSelectionLong = (String) (extras != null ? extras.getString("curSelectionLong") : null);
		curLocationLat = (String) (extras != null ? extras.getString("curLocationLat") : null);
		curLocationLong = (String) (extras != null ? extras.getString("curLocationLong") : null);
		venues = (ArrayList<Parcelable>) (extras != null ? (ArrayList<Parcelable>)extras.getParcelableArrayList("venues") : null);
		DryncDbAdapter dbAdapter = new DryncDbAdapter(DryncLocationChooser.this);
		dbAdapter.open();
		usedVenues = dbAdapter.getAllUsedVenues();
		dbAdapter.close();
		
		imageLoader = new ImageLoader(this, R.drawable.foursquarewine);
		
		setContentView(R.layout.locationchooser);
		
		initializeAds();
		
		saveBtn = (Button)findViewById(R.id.saveBtn);
		cancelBtn = (Button)findViewById(R.id.cancelBtn);
		
		saveBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				Intent resultIntent = new Intent();
				
				if (DryncLocationChooser.this.selectedVenue != null)
				{
					resultIntent.putExtra("selectedVenue", DryncLocationChooser.this.selectedVenue);
				}
				else
				{
					resultIntent.putExtra("selectedVenue", (Parcelable)null);
				}
				
				DryncLocationChooser.this.setResult(RESULT_OK, resultIntent);
				DryncLocationChooser.this.finish();
				
			}});
		cancelBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				DryncLocationChooser.this.setResult(RESULT_CANCELED);
				DryncLocationChooser.this.finish();
			}
			
		});
		
		advice = (TextView) findViewById(R.id.advice);
		advice.setOnClickListener(new OnClickListener(){

			public void onClick(View v) 
			{
				
				
				AlertDialog.Builder alert = new AlertDialog.Builder(DryncLocationChooser.this);  
				alert.setTitle("Custom Location");  
				alert.setMessage("Enter your location:");  

				// Set an EditText view to get user input   
				final AutoCompleteTextView input = new AutoCompleteTextView(DryncLocationChooser.this);
				ArrayAdapter<Venue> adapter = new ArrayAdapter<Venue>(DryncLocationChooser.this, R.layout.list_item, usedVenues);
				input.setAdapter(adapter);
				typeaheadSelectedVenue = null;
				input.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						// TODO Auto-generated method stub
						typeaheadSelectedVenue = (Venue) arg0.getAdapter().getItem(arg2);
						
					}
				});
				
				alert.setView(input);  
				input.setText(advice.getText());

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
					public void onClick(DialogInterface dialog, int whichButton) { 
						if ((typeaheadSelectedVenue != null) && 
								(input.getText().toString().equals(typeaheadSelectedVenue.getName())))
						{
							advice.setText(typeaheadSelectedVenue.getName());
							selectedVenue = typeaheadSelectedVenue;
						}
						else
						{
							String value = input.getText().toString();  
							advice.setText(value);
							Venue customvenue = new Venue();
							customvenue.setName(value);
							customvenue.setGeolat(curLocationLat);
							customvenue.setGeolong(curLocationLong);
							//customvenue.set
							selectedVenue = customvenue;
							advice.setText(customvenue.getName());
						}
						((VenueAdapter)DryncLocationChooser.this.mAdapter).notifyDataSetChanged();	
						dialog.dismiss();
					}  
				});  

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
					public void onClick(DialogInterface dialog, int whichButton) {  
						dialog.cancel();
					}  
				});  

				AlertDialog alertdlg = alert.create();
				alertdlg.show();
			}
		});	
				
		listholder = (LinearLayout)findViewById(R.id.listholder);
		mList = (ListView)findViewById(R.id.venueList);
		mList.setCacheColorHint(0);
		
		
		mAdapter = new VenueAdapter(venues);
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				Log.d("VenueAdapter", "Venue clicked at position: " + position);
				VenueRelativeLayout selvrl = (VenueRelativeLayout)arg1;
				selectedVenue = selvrl.getVenue();
				advice.setText(selectedVenue.getName());
				arg1.setBackgroundColor(Color.LTGRAY);
				((VenueAdapter)arg0.getAdapter()).notifyDataSetChanged();	
			}
			
		});
		
		if (curSelection != null)
		{
			if (venues != null)
			{
				for (Parcelable pvenue : venues)
				{
					Venue venue = (Venue)pvenue;
					if (venue.getName().equals(curSelection))
					{
						selectedVenue = venue;
						advice.setText(selectedVenue.getName());
						break;
					}
				}
			}

			if (selectedVenue == null) // set up custom
			{
				Venue custvenue = new Venue();
				custvenue.setName(curSelection);
				custvenue.setGeolat(curSelectionLat);
				custvenue.setGeolong(curSelectionLong);
				advice.setText(custvenue.getName());
				VenueAdapter va = ((VenueAdapter)DryncLocationChooser.this.mAdapter);
				va.notifyDataSetChanged();
			}
			/*else
			{
				//TODO: @mbrindam - attempt to scroll to selection - not working - revisit this later.
				
				for (int i=0,n=mList.getChildCount();i<n;i++)
				{
					
					VenueRelativeLayout curView = (VenueRelativeLayout)mList.getChildAt(i);
					if (curView.getVenue() == selectedVenue)
					{
						int top = curView.getTop();
						int left = curView.getLeft();
						
						mList.scrollTo(left, top);
						break;
					}
				}
			}*/
		}
		
	}
	
	
	@Override
	public int getMenuItemToSkip() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	class VenueAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

		private List<Parcelable> mVenues;
		private final LayoutInflater mInflater;
		private boolean mFlinging = false;
		
		
		public VenueAdapter() {
			super();
			mVenues = new ArrayList<Parcelable>();
			mInflater = (LayoutInflater) DryncLocationChooser.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public VenueAdapter(List<Parcelable> venues) {
			super();
			if (venues == null)
				venues = new ArrayList<Parcelable>();
			Collections.sort(venues, new VenueDistanceComparator());
			mVenues = venues;
			mInflater = (LayoutInflater) DryncLocationChooser.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public int getCount() {
			if (mVenues != null)
				return mVenues.size();
			else
				return 0;
		}
		
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {	
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		class ViewHolder {
            TextView venueName;
            TextView distance;
            ImageView icon;
        }

		public View getView(int position, View convertView, ViewGroup parent) {		
			Venue venue= (Venue)mVenues.get(position);
			View view = (convertView != null) ?  convertView :
				createView(parent);
			
			bindView(view, venue);

			return view;
		}
		
		private View createView(ViewGroup parent) {
			View venueItem = mInflater.inflate(
					R.layout.venueitem, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.venueName = (TextView) venueItem.findViewById(R.id.venueName);
			holder.icon = (ImageView) venueItem.findViewById(R.id.venueThumb);
			holder.distance = (TextView) venueItem.findViewById(R.id.distance);
			
			venueItem.setTag(holder);
			
			return venueItem;
		}

		private void bindView(View view, Venue venue) {
			VenueRelativeLayout wiv = (VenueRelativeLayout) view;
			ViewHolder holder = (ViewHolder)view.getTag();
			
			wiv.setVenue(venue);
			
			if (venue == selectedVenue)
			{
				view.setSelected(true);
				view.setBackgroundColor(Color.LTGRAY);
			}
			else
			{
				view.setBackgroundColor(Color.TRANSPARENT);
			}
			
			ImageView venueThumb = holder.icon; 
			if (venueThumb != null  && !mFlinging )
			{
				if (venue.getIconurl() != null)
				{
					imageLoader.DisplayImage(venue.getIconurl(), venueThumb);
				}
			}
			
			TextView venuename = holder.venueName; //(TextView) view.findViewById(R.id.wineName);
			venuename.setText(venue.getName());
			
			TextView distance = holder.distance; //(TextView) view.findViewById(R.id.priceValue);
			distance.setText(calcReadableDistance(venue.getDistance()));
		}
	}

	private String calcReadableDistance(long distance)
	{
		if (distance <= 500)
		{
			StringBuilder bldr = new StringBuilder();
			bldr.append(distance).append(" feet");
			return bldr.toString();
		}
		else
		{
			float fdistance = (float)distance;
			
			float miles = fdistance / 5280;
			
			StringBuilder bldr = new StringBuilder();
			
			String sdistance = "" + Round(miles, 1);
			
			bldr.append(sdistance).append(" miles away");
			return bldr.toString();
		}
	}
	
	public float Round(float Rval, int Rpl) {
		float p = (float)Math.pow(10,Rpl);
		Rval = Rval * p;
		float tmp = Math.round(Rval);
		return (float)tmp/p;
	}
	
	static class VenueDistanceComparator implements Comparator<Parcelable>
	{

		public int compare(Parcelable arg0, Parcelable arg1) {
			
			if ((arg0 instanceof Venue) && (arg1 instanceof Venue))
			{
				Long arg0distance = ((Venue)arg0).getDistance();
				Long arg1distance = ((Venue)arg1).getDistance();
				
				return arg0distance.compareTo(arg1distance);
			}
			else
				return 0;
		}
	}
	
	static class VenueNameComparator implements Comparator<Parcelable>
	{

		public int compare(Parcelable arg0, Parcelable arg1)
		{
			if ((arg0 instanceof Venue) && (arg1 instanceof Venue))
			{
				return ((Venue)arg0).getName().compareTo(((Venue)arg1).getName());
			}
			else
				return 0;
		}
	}

}
