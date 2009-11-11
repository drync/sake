package com.drync.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class WineActivity extends Activity {

    private TextView mWine;
    private TextView mDefinition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash);

       /* mWord = (TextView) findViewById(R.id.word);
        mDefinition = (TextView) findViewById(R.id.definition);

        Intent intent = getIntent();

        String word = intent.getStringExtra("word");
        String definition = intent.getStringExtra("definition");

        mWord.setText(word);
        mDefinition.setText(definition);*/
    }
}