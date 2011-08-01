package com.drync.android.objects;

import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Form;
import org.restlet.representation.Representation;

import android.os.Parcel;
import android.os.Parcelable;

public class Cork extends Bottle implements Parcelable
{   
    long _id;
    long cork_id;
    String cork_uuid;
    String description;
    String location;
    String cork_price;
    Float cork_rating = null;
    boolean cork_want = false;
    boolean cork_drank = false;
    boolean cork_own = false;
    boolean cork_ordered = false;
    Integer cork_year;
    String cork_poi;
    String public_note = null;
    String cork_labelInline = null;
    String locationLat = null;
    String locationLong = null;
    
    public String getLocationLat() {
		return locationLat;
	}

	public void setLocationLat(String locationLat) {
		this.locationLat = locationLat;
	}

	public String getLocationLong() {
		return locationLong;
	}

	public void setLocationLong(String locationLong) {
		this.locationLong = locationLong;
	}

	public String getCork_labelInline() {
		return cork_labelInline;
	}

	public void setCork_labelInline(String corkLabelInline) {
		cork_labelInline = corkLabelInline;
	}

	public String getPublic_note() {
		return public_note;
	}

	public void setPublic_note(String publicNote) {
		public_note = publicNote;
	}
	boolean needsServerUpdate = false;
    int updateType = 0;
    
    public static final int UPDATE_TYPE_NONE = 0;
    public static final int UPDATE_TYPE_UPDATE = 1;
    public static final int UPDATE_TYPE_INSERT = 2;
    public static final int UPDATE_TYPE_DELETE = 3;
    
    public boolean isNeedsServerUpdate() {
		return needsServerUpdate;
	}

	public void setNeedsServerUpdate(boolean needsServerUpdate) {
		this.needsServerUpdate = needsServerUpdate;
	}

	public int getUpdateType() {
		return updateType;
	}

	public void setUpdateType(int updateType) {
		this.updateType = updateType;
	}

	public String getCork_poi() {
		return cork_poi;
	}

	public void setCork_poi(String corkPoi) {
		cork_poi = corkPoi;
	}

	public String getCork_created_at() {
		return cork_created_at;
	}

	public void setCork_created_at(String corkCreatedAt) {
		cork_created_at = corkCreatedAt;
	}
	int cork_bottle_count = 0;
    String cork_created_at;
    String cork_label;
    
    
    
    
    public Cork() {
		super();
	}
    
	public long get_id() {
		return _id;
	}
	public void set_id(long id) {
		_id = id;
	}
	public long getCork_id() {
		return cork_id;
	}
	public void setCork_id(long corkId) {
		cork_id = corkId;
	}
	public String getCork_uuid() {
		return cork_uuid;
	}
	public void setCork_uuid(String corkUuid) {
		cork_uuid = corkUuid;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getCork_label() {
		return cork_label;
	}
	public void setCork_label(String corkLabel) {
		cork_label = corkLabel;
	}
	public String getCork_price() {
		return cork_price;
	}
	public void setCork_price(String corkPrice) {
		cork_price = corkPrice;
	}
	public Integer getCork_year() {
		return cork_year;
	}
	public void setCork_year(Integer corkYear) {
		cork_year = corkYear;
	}
	public Float getCork_rating() {
		return cork_rating;
	}
	public void setCork_rating(Float corkRating) {
		cork_rating = corkRating;
	}
	public boolean isCork_want() {
		return cork_want;
	}
	public void setCork_want(boolean corkWant) {
		cork_want = corkWant;
	}
	public boolean isCork_drank() {
		return cork_drank;
	}
	public void setCork_drank(boolean corkDrank) {
		cork_drank = corkDrank;
	}
	public boolean isCork_own() {
		return cork_own;
	}
	public void setCork_own(boolean corkOwn) {
		cork_own = corkOwn;
	}
	public boolean isCork_ordered() {
		return cork_ordered;
	}
	public void setCork_ordered(boolean corkOrdered) {
		cork_ordered = corkOrdered;
	}
	public int getCork_bottle_count() {
		return cork_bottle_count;
	}
	public void setCork_bottle_count(int corkBottleCount) {
		cork_bottle_count = corkBottleCount;
	}

	public Map<String, String> getRepresentation(String deviceId) {
		return getRepresentation(deviceId, false);
	}
	public Map<String, String> getRepresentation(String deviceId, boolean isUpdate) {
		HashMap<String,String> form = new HashMap<String, String>(); 
		if (isUpdate)
		{
			form.put("_method", "put");
		}
		else
		{
			form.put("_method", "post");
		}
		form.put("format", "xml");
		form.put("device_id", deviceId);
		form.put("prod", "wine-free");
		form.put("cork[name]", this.getName());
		form.put("cork[description]", this.getDescription());
		if ((this.getCork_rating() != null) && this.getCork_rating() >= 0)
		{
			form.put("cork[rating]", "" + this.getCork_rating());
		}
		else
		{
			form.put("cork[rating]", "");
		}
		
		if (this.getBottle_Id() != 0)
			form.put("cork[bottle_id]", "" + this.getBottle_Id());
		//form.add("cork[label_inline]" + this.getCork_label());
		form.put("cork[grape]", this.getGrape());
		form.put("cork[region]", this.getRegion());
		form.put("cork[bottle_count]", "" + this.getCork_bottle_count());
		form.put("cork[uuid]", this.getCork_uuid());
		form.put("cork[drank]", Boolean.toString(this.isCork_drank()));
		form.put("cork[user_year]", "" + this.getCork_year());
		form.put("cork[own]", Boolean.toString(this.isCork_own()));
		//form.add("cork[user_name"])
		form.put("cork[want]", Boolean.toString(this.isCork_want()));
		form.put("cork[user_price]", this.getCork_price());
		form.put("cork[location]", this.getLocation());
		form.put("cork[public_note]", this.getPublic_note());
		form.put("cork[style]", this.getStyle());
		
		if (this.getCork_labelInline() != null)
		{
			form.put("cork[label_inline]", this.getCork_labelInline().replace(' ', '+'));
		}
		
		form.put("cork[latitude]", this.getLocationLat());
		form.put("cork[longitude]", this.getLocationLong());
		/*
    * cork[user_style]
   * cork[label_inline]
    * cork[user_name]
   	 */
		
		return form;
	}
	
	public static final Parcelable.Creator<Bottle> CREATOR = new Parcelable.Creator<Bottle>() {
        public Cork createFromParcel(Parcel in) {
            return new Cork(in);
        }

        public Cork[] newArray(int size) {
            return new Cork[size];
        }
    };
    
    public Cork(Parcel in) {
		super();
		readFromParcel(in);
	}
    
    @Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeLong(_id);
		out.writeInt(cork_bottle_count);
		out.writeString(cork_created_at);
		out.writeInt(cork_drank ? 1 : 0);
		out.writeLong(cork_id);
		out.writeString(cork_label);
		out.writeInt(cork_ordered ? 1 : 0);
		out.writeInt(cork_own ? 1 : 0);
		out.writeString(cork_poi);
		out.writeString(cork_price);
		out.writeFloat(cork_rating == null ? 0 : cork_rating);
		out.writeString(cork_uuid);
		out.writeInt(cork_want ? 1 : 0);
		out.writeInt(cork_year == null ? 0 : cork_year);
		out.writeString(description);
		out.writeString(location);
		out.writeString(public_note);
		out.writeInt(needsServerUpdate ? 1 : 0);
		out.writeInt(updateType);
		out.writeString(locationLat);
		out.writeString(locationLong);
	}

	public void readFromParcel(Parcel in)
    {
    	super.readFromParcel(in);
    	_id = in.readLong();
    	cork_bottle_count = in.readInt();
    	cork_created_at = in.readString();
    	cork_drank = in.readInt() == 0 ? false : true;
    	cork_id = in.readLong();
    	cork_label = in.readString();
    	cork_ordered = in.readInt() == 0 ? false : true;
    	cork_own = in.readInt() == 0 ? false : true;
    	cork_poi = in.readString();
    	cork_price = in.readString();
    	cork_rating = in.readFloat();
    	cork_uuid = in.readString();
    	cork_want = in.readInt() == 0 ? false : true;
    	cork_year = in.readInt();
    	description = in.readString();
    	location = in.readString();
    	public_note = in.readString();
    	needsServerUpdate = in.readInt() == 0 ? false : true;
    	updateType = in.readInt();
    	locationLat = in.readString();
    	locationLong = in.readString();
    }
	
	public int getYearValue()
	{
		if ((this.getCork_year() != null) && (this.getCork_year() > 0))
			return this.getCork_year();
		else
			return this.getYear();
		
	}
	
	public String getDisplayYear()
	{
		if (this.getYear() > 0)
			return Integer.toString(this.getYear());
		else
			return "NV";
	}
}
