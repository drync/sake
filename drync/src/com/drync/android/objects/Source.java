package com.drync.android.objects;

public class Source {
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

}
