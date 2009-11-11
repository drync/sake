package com.drync.android.objects;

import java.util.ArrayList;

public class Bottle {
	long id;
	String name;
	int year;
	String region_path;
	String region;
	String grape;
	String style;
	String winery_name;
	String label;
	String label_thumb;
	
	
	public Bottle() {
		super();
		// TODO Auto-generated constructor stub
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
	

}
