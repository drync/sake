package com.drync.android;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class DryncUtils {

	private static StringBuilder builder = new StringBuilder();
	public static final String CACHE_DIRECTORY = 
		(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) ?
				Environment.getExternalStorageDirectory() + ".drync_cache/" : 
					"/data/data/com.drync.android/drync-cache/";
	public static boolean useLocalCache = true; // this is true until proven no.
	public static boolean isDebugMode = DryncUtils.checkForDebugMode();
	
	//Shared Prefs
	public static final String PREFS_NAME = "DRYNC_PREFS";
	public static final String SHOW_INTRO_PREF = "showIntro";
	public static final String LAST_QUERY_PREF = "lastQuery";
	public static final String TWITTER_USERNAME_PREF = "twitter_username";
	public static final String TWITTER_PASSWORD_PREF = "twitter_password";
	public static final String TWITTER_CELLARTWT_PREF = "twitter_cellartweet";
	public static final String TWITTER_PW_ENCRYPT_SEED = "red truck chardonnay";

	public static boolean isUseLocalCache() {
		return useLocalCache;
	}

	private static boolean checkForDebugMode() 
	{
		try
		{
			String debugFilePath = DryncUtils.CACHE_DIRECTORY + "debug.properties";
			File debugFile = new File(debugFilePath);
			boolean debugMode = debugFile.exists();
			Log.d("DryncUtils", "DEBUG MODE ENABLED!!!!!");
			return debugMode;
		}
		catch (Exception e)
		{
			return false;
		}
		
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
	
	public static String getCacheDirectory()
	{
		return DryncUtils.CACHE_DIRECTORY;
	}
	public static boolean checkForLocalCacheArea()
	{
		File file = new File(DryncUtils.CACHE_DIRECTORY);
		useLocalCache = file.getParentFile().exists();	
		return useLocalCache;
	}

	public static String encryptTwitterPassword(String password)
	{
		try {
			return SimpleCrypto.encrypt(DryncUtils.TWITTER_PW_ENCRYPT_SEED, password);
		} catch (Exception e) {
			Log.e("DryncUtil", "Could not encrypt password!", e);
		}
		return null;
	}
	
	public static String decryptTwitterPassword(String encrypted)
	{
		try {
			return SimpleCrypto.decrypt(DryncUtils.TWITTER_PW_ENCRYPT_SEED, encrypted);
		} catch (Exception e) {
			Log.e("DryncUtil", "Could not decrypt password!", e);
		}
		return null;
	}
	
	
	


}
