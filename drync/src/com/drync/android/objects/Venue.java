package com.drync.android.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Venue implements Parcelable {

	long id;
	String name;
	String address;
	String crossstreet;
	String city;
	String state;
	String zip;
	String geolat;
	String geolong;
	String phone;
	long distance;
	
	public Venue() {
		super();
	}
	
	public Venue(Parcel in) {
		super();
		readFromParcel(in);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCrossstreet() {
		return crossstreet;
	}

	public void setCrossstreet(String crossstreet) {
		this.crossstreet = crossstreet;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getGeolat() {
		return geolat;
	}

	public void setGeolat(String geolat) {
		this.geolat = geolat;
	}

	public String getGeolong() {
		return geolong;
	}

	public void setGeolong(String geolong) {
		this.geolong = geolong;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public long getDistance() {
		return distance;
	}

	public void setDistance(long distance) {
		this.distance = distance;
	}
	
	public static final Parcelable.Creator<Venue> CREATOR = new Parcelable.Creator<Venue>() {
        public Venue createFromParcel(Parcel in) {
            return new Venue(in);
        }

        public Venue[] newArray(int size) {
            return new Venue[size];
        }
    };

    public void readFromParcel(Parcel in) {
		id = in.readLong();
		name = in.readString();
		address = in.readString();
		crossstreet = in.readString();
		city = in.readString();
		state = in.readString();
		zip = in.readString();
		geolat = in.readString();
		geolong = in.readString();
		phone = in.readString();
		distance = in.readLong();
	}
    
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(id);
		out.writeString(name);
		out.writeString(address);
		out.writeString(crossstreet);
		out.writeString(city);
		out.writeString(state);
		out.writeString(zip);
		out.writeString(geolat);
		out.writeString(geolong);
		out.writeString(phone);
		out.writeLong(distance);
	}

}
