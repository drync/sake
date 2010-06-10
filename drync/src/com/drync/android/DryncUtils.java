package com.drync.android;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class DryncUtils {

	private static StringBuilder builder = new StringBuilder();
	private static String CACHE_DIRECTORY = null;
	public static boolean isDebugMode = DryncUtils.checkForDebugMode();
	
	private static String deviceId = null;
	
	//Shared Prefs
	public static final String PREFS_NAME = "DRYNC_PREFS";
	public static final String DEVICE_ID = "deviceId";
	public static final String SHOW_INTRO_PREF = "showIntro";
	public static final String LAST_QUERY_PREF = "lastQuery";
	public static final String LAST_FILTER_PREF = "lastFilter";
	public static final String TWITTER_USERNAME_PREF = "twitter_username";
	public static final String TWITTER_PASSWORD_PREF = "twitter_password";
	public static final String TWITTER_CELLARTWT_PREF = "twitter_cellartweet";
	public static final String TWITTER_PW_ENCRYPT_SEED = "red truck chardonnay";
	public static final String UUID_KEY = "UuidKey";
	public static final String LAST_UUID_VAL = "lastUuidVal";

	private static boolean checkForDebugMode() 
	{
		try
		{
			String debugFilePath = getCacheDir() + "debug.properties";
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

	public static String getCacheFileName(String url) {
		builder.setLength(0);
		builder.append(getCacheDir());
		builder.append(url.hashCode()).append(".jpg");
		return builder.toString();
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
	public static String getCacheDir()
	{
		try {
			return getCacheDir(null);
		} catch (DryncConfigException e) {
			Log.e("DryncUtil", "DryncConfigException: " + e.getMessage());
		}
		
		return /* this is probably where it should go, so let's let it work, at least: */
		    "/data/data/com.drync.android/cache/";
	}
	public static String getCacheDir(Context context) throws DryncConfigException
	{
		if (DryncUtils.CACHE_DIRECTORY == null)
		{
			if (context == null)
			{
				String message = "CACHE DIRECTORY NOT SET.  Must call getCacheDir with a valid context first!";
				DryncConfigException dce = new DryncConfigException();
				throw dce;	
			}
			
			try {
				DryncUtils.CACHE_DIRECTORY = context.getCacheDir().getCanonicalPath() + "/";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return DryncUtils.CACHE_DIRECTORY;
	}
	
	public static String nextUuid(Activity activity)
	{
		Random rand = new Random(System.currentTimeMillis());
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		String uuid_key = settings.getString(DryncUtils.UUID_KEY, null);
		
		if (uuid_key == null)
		{
			uuid_key = "" + rand.nextInt(99999999);
			Editor editor = settings.edit();
			editor.putString(DryncUtils.UUID_KEY, uuid_key);
			editor.commit();
		}
		
		Long uuid_val = System.currentTimeMillis();
		return uuid_key + uuid_val;
	}
	
	public static String getDeviceId(ContentResolver resolver, Activity activity)
	{
		if (deviceId == null)
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

			String savedDevId = settings.getString(DryncUtils.DEVICE_ID, null);
			if (savedDevId == null)
			{
				String sysDevId = Settings.System.getString(resolver, Settings.System.ANDROID_ID);
				if ((sysDevId == null) || (sysDevId == "")) // probably an emulator, let's create one and stick with it.
				{
					StringBuilder bldr = new StringBuilder("droidem-");
					//use a few random numbers to get something that is hopefully unique.
					Random rand = new Random(System.currentTimeMillis());
					bldr.append(rand.nextInt(99999));
					bldr.append("-");
					bldr.append(rand.nextInt(99999));
					bldr.append("-");
					bldr.append(rand.nextInt(99999));
					deviceId = bldr.toString();			
				}
				else
				{
					deviceId = sysDevId;
				}

				Editor editor = settings.edit();
				editor.putString(DryncUtils.DEVICE_ID, deviceId);
				editor.commit();			
			}
			else
			{
				deviceId = savedDevId;
			}
		}
		
		Log.d("DryncUtils", "Got deviceId: " + deviceId);
		
		return deviceId;
	}

	


}
