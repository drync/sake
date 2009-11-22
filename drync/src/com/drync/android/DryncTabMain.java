package com.drync.android;

import java.util.ArrayList;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.DryncTabHost;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.drync.android.DryncMain.WineAdapter;
import com.drync.android.ui.DryncTabActivity;
public class DryncTabMain extends TabActivity 
{
	
	private TabHost mTabHost;
	private String mDefaultTab = null;
	private int mDefaultTabIndex = -1;

	final Handler mHandler = new Handler();
	//private String deviceId;
	WineAdapter mAdapter; 
	
	/**
     * Sets the default tab that is the first tab highlighted.
     * 
     * @param tag the name of the default tab
     */
    public void setDefaultTab(String tag) {
        mDefaultTab = tag;
        mDefaultTabIndex = -1;
    }

    /**
     * Sets the default tab that is the first tab highlighted.
     * 
     * @param index the index of the default tab
     */
    public void setDefaultTab(int index) {
        mDefaultTab = null;
        mDefaultTabIndex = index;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTabHost = (TabHost)getTabHost();
		mTabHost.addTab(mTabHost.newTabSpec("tab_search").
				setIndicator(getResources().getString(R.string.searchtab),
				getResources().getDrawable(R.drawable.tab_icon_search)).
				setContent(new Intent(this, DryncMain.class)));
		mTabHost.addTab(mTabHost.newTabSpec("tab_cellar").setIndicator(
				getResources().getString(R.string.cellartab),
				getResources().getDrawable(R.drawable.tab_icon_cellar)).
				setContent(new Intent(this, HelloDrync.class)));
		mTabHost.addTab(mTabHost.newTabSpec("tab_quicknote").setIndicator(
				getResources().getString(R.string.quicknotestab),
				getResources().getDrawable(R.drawable.tab_icon_pencil)).
				setContent(new Intent(this, HelloDrync.class)));
		mTabHost.addTab(mTabHost.newTabSpec("tab_topwine").setIndicator(
				getResources().getString(R.string.topwinestab),
				getResources().getDrawable(R.drawable.tab_icon_topwines)).
				setContent(new Intent(this, HelloDrync.class)));
		mTabHost.addTab(mTabHost.newTabSpec("tab_settings").setIndicator(
				getResources().getString(R.string.settingstab),
				getResources().getDrawable(R.drawable.tab_icon_settings)).
				setContent(new Intent(this, HelloDrync.class)));


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
