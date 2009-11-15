package com.drync.android;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.drync.android.DryncMain.WineAdapter;
import com.drync.android.objects.Bottle;

public class DryncTabMain extends TabActivity 
{
	
	private TabHost mTabHost;
	private ListView mList;
	final Handler mHandler = new Handler();
	private List<Bottle> mResults = null;
	private ProgressDialog progressDlg = null;
	private String deviceId;
	WineAdapter mAdapter; 
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
		
		mTabHost = getTabHost();
		mTabHost.addTab(mTabHost.newTabSpec("tab_search").setIndicator(
				getResources().getString(R.string.searchtab),
				getResources().getDrawable(R.drawable.tab_icon_search)).
				setContent(R.id.searchview));
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

		

		LayoutInflater inflater = getLayoutInflater();
		View tstLayout = inflater.inflate(R.layout.searchinstructions,
		                               (ViewGroup) findViewById(R.id.search_toast_layout));
		
		Toast toast = new Toast(getApplicationContext()) {
			
		};
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(10);
		toast.setView(tstLayout);
		toast.show();
	}

	private void launchBottle(Bottle bottle) {
		LinearLayout lview = (LinearLayout) this.findViewById(R.id.searchview);
		Intent next = new Intent();
		next.setClass(this, WineActivity.class);
		next.putExtra("bottle", bottle);
		//startActivity(next);
		
		final TabHost tabHost = getTabHost();
		mTabHost.newTabSpec("tab_search").setContent(next);
	}
	
}
