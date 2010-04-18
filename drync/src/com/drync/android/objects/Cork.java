package com.drync.android.objects;

public class Cork extends Bottle
{   
    long _id;
    long cork_id;
    String cork_uuid;
    String description;
    String location;
    String cork_price;
    int cork_rating;
    boolean cork_want = false;
    boolean cork_drank = false;
    boolean cork_own = false;
    boolean cork_ordered = false;
    int cork_year;
    String cork_poi;
    public String getCork_poi() {
		return cork_poi;
	}

	public void setCork_poi(String corkPoi) {
		cork_poi = corkPoi;
	}

	public long getCork_created_at() {
		return cork_created_at;
	}

	public void setCork_created_at(long corkCreatedAt) {
		cork_created_at = corkCreatedAt;
	}
	int cork_bottle_count = 0;
    long cork_created_at;
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
	public int getCork_year() {
		return cork_year;
	}
	public void setCork_year(int corkYear) {
		cork_year = corkYear;
	}
	public int getCork_rating() {
		return cork_rating;
	}
	public void setCork_rating(int corkRating) {
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
	
}
