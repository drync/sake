package com.drync.android.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Review implements Parcelable {
	
	String publisher;
	String text;
	String url;
	String review_cat;
	String review_source;
	
	public static final Parcelable.Creator<Review> CREATOR = new Parcelable.Creator<Review>() {
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }
        
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };
	
    public Review() {
		super();
	}


	public Review(Parcel in) {
		super();
		readFromParcel(in);
	}
	
	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getReview_cat() {
		return review_cat;
	}

	public void setReview_cat(String reviewCat) {
		review_cat = reviewCat;
	}

	public String getReview_source() {
		return review_source;
	}

	public void setReview_source(String reviewSource) {
		review_source = reviewSource;
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.publisher);
		out.writeString(this.review_cat);
		out.writeString(this.review_source);
		out.writeString(this.text);
		out.writeString(this.url);
	}
	
	private void readFromParcel(Parcel in) {
		publisher = in.readString();
		review_cat = in.readString();
		review_source = in.readString();
		text = in.readString();
		url = in.readString();
		
	}
	

}
