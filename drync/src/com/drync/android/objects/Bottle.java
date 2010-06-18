package com.drync.android.objects;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public class Bottle implements Parcelable {
	long bottle_id;
	String name;
	int year;
	String region_path;
	String region;
	String grape;
	String style;
	String winery_name;
	String label;
	String label_thumb;
	String price;
	float minprice = 0;
	
	public float getMinprice() {
		return minprice;
	}


	public void setMinprice(float minprice) {
		this.minprice = minprice;
	}


	public float getMaxprice() {
		return maxprice;
	}


	public void setMaxprice(float maxprice) {
		this.maxprice = maxprice;
	}


	float maxprice = 0;
	String rating;
	int reviewCount = 0;
	
	ArrayList<Review> reviews;
	
	public int getReviewCount() {
		if (reviews == null)
			return 0;
		else
			return reviews.size();
	}


	public void setReviewCount(int reviewCount) {
		this.reviewCount = reviewCount;
	}


	public static final Parcelable.Creator<Bottle> CREATOR = new Parcelable.Creator<Bottle>() {
        public Bottle createFromParcel(Parcel in) {
            return new Bottle(in);
        }

        public Bottle[] newArray(int size) {
            return new Bottle[size];
        }
    };
    
	public String getRating() {
		if ((rating == null) || (rating.equals("")))
			return "n/a";
		else
			return rating;
	}


	public void setRating(String rating) {
		this.rating = rating;
	}


	public String getPrice() {
		if (price == null || price.equals(""))
			return "n/a";
		else
			return price;
	}


	public void setPrice(String price) {
		this.price = price;
	}


	public Bottle() {
		super();
		this.reviews = new ArrayList<Review>();
	}


	public Bottle(Parcel in) {
		super();
		readFromParcel(in);
	}
	
	
	public void addReview(Review review)
	{
		this.reviews.add(review);
	}
	
	public Review getReview(int index)
	{
		if (this.reviews.size() > index)
			return this.reviews.get(index);
		else
			return null;
	}

	public long getBottle_Id() {
		return bottle_id;
	}


	public void setBottle_Id(long id) {
		this.bottle_id = id;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public int getYear() {
		return year;
	}


	public void setYear(int year) {
		this.year = year;
	}


	public String getRegion_path() {
		return region_path;
	}


	public void setRegion_path(String regionPath) {
		region_path = regionPath;
	}


	public String getRegion() {
		return region;
	}


	public void setRegion(String region) {
		this.region = region;
	}


	public String getGrape() {
		return grape;
	}


	public void setGrape(String grape) {
		this.grape = grape;
	}


	public String getStyle() {
		return style;
	}


	public void setStyle(String style) {
		this.style = style;
	}


	public String getWinery_name() {
		return winery_name;
	}


	public void setWinery_name(String wineryName) {
		winery_name = wineryName;
	}


	public String getLabel() {
		return label;
	}


	public void setLabel(String label) {
		this.label = label;
	}


	public String getLabel_thumb() {
		return label_thumb;
	}


	public void setLabel_thumb(String labelThumb) {
		label_thumb = labelThumb;
	}


	public ArrayList<Source> getSources() {
		return sources;
	}


	public void setSources(ArrayList<Source> sources) {
		this.sources = sources;
	}


	ArrayList<Source> sources = new ArrayList<Source>();
	
	public void addSource(Source src)
	{
		this.sources.add(src);
	}
	
	public Source getSource(int index)
	{
		if (this.sources.size() > index)
			return this.sources.get(index);
		else
			return null;
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}


	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(bottle_id);
		out.writeString(name);
		out.writeInt(year);
		out.writeString(region_path);
		out.writeString(region);
		out.writeString(grape);
		out.writeString(style);
		out.writeString(winery_name);
		out.writeString(label);
		out.writeString(label_thumb);
		out.writeString(price);
		out.writeFloat(minprice);
		out.writeFloat(maxprice);
		out.writeString(rating);
		out.writeInt(reviewCount);
		//Review[] reviewarray = reviews.toArray(new Review[0]);
		out.writeTypedList(reviews);
		
	}
	
	public void readFromParcel(Parcel in) {
		bottle_id = in.readLong();
		name = in.readString();
		year = in.readInt();
		region_path = in.readString();
		region = in.readString();
		grape = in.readString();
		style = in.readString();
		winery_name = in.readString();
		label = in.readString();
		label_thumb = in.readString();
		price = in.readString();
		minprice = in.readFloat();
		maxprice = in.readFloat();
		rating = in.readString();	
		reviewCount = in.readInt();
		//Parcelable[] reviewsarray = in.readParcelableArray(Review.class.getClassLoader());
		reviews = new ArrayList<Review>();
		in.readTypedList(reviews, Review.CREATOR);//(ArrayList<Review>) Arrays.asList(reviewsarray); 
		
	}
}
