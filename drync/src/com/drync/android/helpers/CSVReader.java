package com.drync.android.helpers;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;


public class CSVReader {
	private final ArrayList<String> entries = new ArrayList<String>();

	int resourceId;
	Context context;
	
    public CSVReader(Context context, int resourceId) {
    	this.resourceId = resourceId;
    	this.context = context;
    	ensureLoaded();
    }

    public ArrayList<String> getEntries() {
		return entries;
	}

	private boolean mLoaded = false;

    /**
     * Loads the words and definitions if they haven't been loaded already.
     *
     * @param resources Used to load the file containing the words and definitions.
     */
    public synchronized void ensureLoaded() {
        if (mLoaded) return;

        new Thread(new Runnable() {
            public void run() {
                try {
                    loadEntries();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private synchronized void loadEntries() throws IOException {
        if (mLoaded) return;

        Log.d("CSVReader", "loading entries");
        InputStream inputStream = context.getResources().openRawResource(this.resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while((line = reader.readLine()) != null) {
                String[] strings = TextUtils.split(line, ",");
                for (String entry : strings)
                {
                	this.entries.add(entry);
                }
            }
        } finally {
            reader.close();
        }
        mLoaded = true;
    }
}
