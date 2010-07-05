package com.drync.android.helpers;

import java.util.ArrayList;

public class Result<T> {
	boolean result = false;
	ArrayList<T> contents = null;
	public boolean isResult() {
		return result;
	}
	public void setResult(boolean result) {
		this.result = result;
	}
	public ArrayList<T> getContents() {
		return contents;
	}
	public void setContents(ArrayList<T> contents) {
		this.contents = contents;
	}
	
	
	public Result() {
		super();
		contents = new ArrayList<T>();
	}
	
	public Result(boolean result) {
		this();
		this.result = result;
	}
	public Result(boolean result, ArrayList<T> contents) {
		super();
		this.result = result;
		this.contents = contents;
	}
	
	
}
