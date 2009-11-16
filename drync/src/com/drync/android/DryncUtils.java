package com.drync.android;

import java.io.File;

import android.os.Environment;

public class DryncUtils {

	private static StringBuilder builder = new StringBuilder();
	public static final String CACHE_DIRECTORY = Environment.getExternalStorageDirectory() + "/.drync-cache/";
	public static boolean useLocalCache = true; // this is true until proven no.

	public static boolean isUseLocalCache() {
		return useLocalCache;
	}

	public static void setUseLocalCache(boolean useLocalCache) {
		DryncUtils.useLocalCache = useLocalCache;
	}

	public static String getCacheFileName(String url) {
		builder.setLength(0);
		builder.append(CACHE_DIRECTORY);
		builder.append(url.hashCode()).append(".jpg");
		return builder.toString();
	}
	
	public static boolean checkForLocalCacheArea()
	{
		File file = new File(DryncUtils.CACHE_DIRECTORY);
		useLocalCache = file.getParentFile().exists();	
		return useLocalCache;
	}
	
	
	
	


}
