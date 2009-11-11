package com.drync.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

public class WineDirectory {

    public static class Wine {
        public final String word;
        public final String definition;

        public Wine(String word, String definition) {
            this.word = word;
            this.definition = definition;
        }
    }

    private static final WineDirectory sInstance = new WineDirectory();

    public static WineDirectory getInstance() {
        return sInstance;
    }

    private final Map<String, List<Wine>> mDict = new ConcurrentHashMap<String, List<Wine>>();

    private WineDirectory() {
    }

    private boolean mLoaded = false;

    /**
     * Loads the words and definitions if they haven't been loaded already.
     *
     * @param resources Used to load the file containing the words and definitions.
     */
    public synchronized void ensureLoaded(final Resources resources) {
        if (mLoaded) return;

        new Thread(new Runnable() {
            public void run() {
                try {
                    loadWords(resources);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private synchronized void loadWords(Resources resources) throws IOException {
        if (mLoaded) return;

        Log.d("dict", "loading words");
        InputStream inputStream = resources.openRawResource(R.raw.definitions);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while((line = reader.readLine()) != null) {
                String[] strings = TextUtils.split(line, "-");
                if (strings.length < 2) continue;
                addWord(strings[0].trim(), strings[1].trim());
            }
        } finally {
            reader.close();
        }
        mLoaded = true;
    }


    public List<Wine> getMatches(String query) {
        List<Wine> list = mDict.get(query);
        return list == null ? Collections.EMPTY_LIST : list;
    }

    private void addWord(String word, String definition) {
        final Wine theWord = new Wine(word, definition);

        final int len = word.length();
        for (int i = 0; i < len; i++) {
            final String prefix = word.substring(0, len - i);
            addMatch(prefix, theWord);
        }
    }

    private void addMatch(String query, Wine word) {
        List<Wine> matches = mDict.get(query);
        if (matches == null) {
            matches = new ArrayList<Wine>();
            mDict.put(query, matches);
        }
        matches.add(word);
    }
}