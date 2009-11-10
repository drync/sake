package com.drync.android;

import java.util.ArrayList;

import android.app.TabActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

public class DryncMain extends TabActivity {

	private TabHost mTabHost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mTabHost = getTabHost();
		mTabHost = getTabHost();
	    mTabHost.addTab(mTabHost.newTabSpec("tab_search").setIndicator(
	    		getResources().getString(R.string.searchtab),
	    		getResources().getDrawable(R.drawable.tab_icon_search)).
	    		setContent(R.id.textview1));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_cellar").setIndicator(
	    		getResources().getString(R.string.cellartab),
	    		getResources().getDrawable(R.drawable.tab_icon_cellar)).
	    		setContent(R.id.textview2));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_quicknote").setIndicator(
	    		getResources().getString(R.string.quicknotestab),
	    		getResources().getDrawable(R.drawable.tab_icon_pencil)).
	    		setContent(R.id.textview3));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_topwine").setIndicator(
	    		getResources().getString(R.string.topwinestab),
	    		getResources().getDrawable(R.drawable.tab_icon_topwines)).
	    		setContent(R.id.textview4));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_settings").setIndicator(
	    		getResources().getString(R.string.settingstab),
	    		getResources().getDrawable(R.drawable.tab_icon_settings)).
	    		setContent(R.id.textview5));
	    

	    int n=mTabHost.getTabWidget().getChildCount();
	    for (int i=0;i<n;i++)
	    {
	    	ArrayList<View> views = mTabHost.getTabWidget().getChildAt(i).getTouchables();
	    	RelativeLayout relLayout = (RelativeLayout)views.get(0);
	    	TextView tv = (TextView)relLayout.getChildAt(1);
	    	tv.setPadding(0, 3, 0, 0);
	    	tv.setSingleLine(false);
	    	tv.setGravity(Gravity.CENTER);
	    	tv.setTextSize(10);
	    	tv.setLines(2);
	    }
	    mTabHost.setCurrentTab(0);

	}

}
