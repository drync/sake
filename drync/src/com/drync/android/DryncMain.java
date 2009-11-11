package com.drync.android;

import java.util.ArrayList;
import java.util.List;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TwoLineListItem;
import android.text.TextUtils;


public class DryncMain extends TabActivity {

	private TabHost mTabHost;
	private ListView mList;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//mList = (ListView) findViewById(R.id.list);

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
	    

	    WineDirectory.getInstance().ensureLoaded(getResources());
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
	    
	    final EditText searchfield = (EditText) findViewById(R.id.searchentry);
	    searchfield.setOnKeyListener(new OnKeyListener() {
	        public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		              // Perform action on key press
					//TextView textview = (TextView)findViewById(R.id.textviewsearch);
		          // textview.setText(searchfield.getText());  //Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
		            
		            String word = searchfield.getText().toString();
		            //WineDirectory.Wine theWine = WineDirectory.getInstance().getMatches(word).get(0);
		            WineAdapter wordAdapter = new WineAdapter(WineDirectory.getInstance().getMatches(word));
		           
		            LinearLayout listholder = (LinearLayout)findViewById(R.id.listholder);
		            
		            if (mList == null)
		            {
		            	mList = new ListView(DryncMain.this.getBaseContext());
		            	//mList.setId(R.string.listview);
		            	listholder.addView(mList);
		            }
		            
		            mList.setAdapter(wordAdapter);
		            /*mList.setAdapter(wordAdapter);
		            mList.setOnItemClickListener(wordAdapter);*/
		            
		            
		            return true;
		            }
		            return false;
			}
	    });


	}
	
	private void launchWord(WineDirectory.Wine theWord) {
        Intent next = new Intent();
        next.setClass(this, WineActivity.class);
        next.putExtra("word", theWord.word);
        next.putExtra("definition", theWord.definition);
        startActivity(next);
    }

	
	class WineAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        private final List<WineDirectory.Wine> mWines;
        private final LayoutInflater mInflater;

        public WineAdapter(List<WineDirectory.Wine> wines) {
        	mWines = wines;
            mInflater = (LayoutInflater) DryncMain.this.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mWines.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TwoLineListItem view = (convertView != null) ? (TwoLineListItem) convertView :
                    createView(parent);
            bindView(view, mWines.get(position));
            return view;
        }

        private TwoLineListItem createView(ViewGroup parent) {
            TwoLineListItem item = (TwoLineListItem) mInflater.inflate(
                    android.R.layout.simple_list_item_2, parent, false);
            item.getText2().setSingleLine();
            item.getText2().setEllipsize(TextUtils.TruncateAt.END);
            return item;
        }

        private void bindView(TwoLineListItem view, WineDirectory.Wine wine) {
            view.getText1().setText(wine.word);
            view.getText2().setText(wine.definition);
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            launchWord(mWines.get(position));
        }
    }


}
