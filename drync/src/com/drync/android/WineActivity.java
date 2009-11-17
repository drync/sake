package com.drync.android;

import com.drync.android.objects.Bottle;
import com.drync.android.ui.RemoteImageView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class WineActivity extends Activity {

	private Bottle mBottle;
	private Drawable defaultIcon;
    private TextView mWine;
    private TextView mDefinition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.winedetail);
        Intent intent = getIntent();
        
        mBottle = intent.getParcelableExtra("bottle");
        defaultIcon = getResources().getDrawable(R.drawable.icon);

        RemoteImageView imgview = (RemoteImageView) findViewById(R.id.wineimg);
        if (imgview != null)
        {
        	String labelThumb = mBottle.getLabel_thumb();
        	if (labelThumb != null && !labelThumb.equals(""))
        	{
        		imgview.setRemoteURI(labelThumb);
        		imgview.setLocalURI(DryncUtils.getCacheFileName(labelThumb));
        		imgview.setImageDrawable(defaultIcon);
        		imgview.setUseDefaultOnly(false);
        		imgview.loadImage();
        	}
        	else
        	{
        		imgview.setUseDefaultOnly(true);
        		imgview.setImageDrawable(defaultIcon);
        	}
        }
        
        TextView nameView = (TextView) findViewById(R.id.wineName);
        nameView.setText(mBottle.getName());
       /* mWord = (TextView) findViewById(R.id.word);
        mDefinition = (TextView) findViewById(R.id.definition);

        Intent intent = getIntent();

        String word = intent.getStringExtra("word");
        String definition = intent.getStringExtra("definition");

        mWord.setText(word);
        mDefinition.setText(definition);*/
    }
}