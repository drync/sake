package com.drync.android.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Source implements Parcelable {
	String url;
	String name;
	
	public Source(String url, String name) {
		super();
		this.url = url;
		this.name = name;
	}
	public Source() {
		super();
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeString(this.url);
	}
	
	private void readFromParcel(Parcel in) {
		name = in.readString();
		url = in.readString();
	}
	
	public static final Parcelable.Creator<Source> CREATOR = new Parcelable.Creator<Source>() {
        public Source createFromParcel(Parcel in) {
            return new Source(in);
        }
        
        public Source[] newArray(int size) {
            return new Source[size];
        }
    };
    
    public Source(Parcel in) {
		super();
		readFromParcel(in);
	}
	

}
