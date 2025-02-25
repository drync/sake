package com.drync.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.acra.ErrorReporter;
import org.apache.http.client.CookieStore;

import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Source;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class DryncUtils {

	private static final String DRYNC_FREE_FLURRY_CODE = "DQALNHL5RQ53WS3LPR45";
	private static final String DRYNC_PRO_FLURRY_CODE = "EVUK1M8HTX644WLK92JH";
	private static final String PRODUCT_ID_PAID = "wine";
	private static final String PRODUCT_ID_FREE = "wine-free";
	public static final int FREE_CELLAR_MAX_CORKS = 10;
	static final String producttype = PRODUCT_ID_PAID;
	
	static String productId = PRODUCT_ID_PAID;
	
	public static String getProductId() {
		return productId;
	}

	public static void setProductId(String productId) {
		DryncUtils.productId = productId;
	}

	private static String dryncFlurryCode = DRYNC_PRO_FLURRY_CODE;
	
	public static String getDryncFlurryCode() {
		return dryncFlurryCode;
	}

	public static void setDryncFlurryCode(String dryncFlurryCode) {
		DryncUtils.dryncFlurryCode = dryncFlurryCode;
	}

	private static StringBuilder builder = new StringBuilder();
	private static String CACHE_DIRECTORY = null;
	public static boolean isDebugMode = false; //DryncUtils.checkForDebugMode();
	public static CookieStore cookieStore;
	public static long cellarLastUpdatedTimestamp = -1;
	public static long myAcctGetLastUpdatedTimestamp = -1;
	public static Boolean freeMode = null;
	public static String lastLocationLat = null;
	public static String lastLocationLong = null;
	public static String lastLocationAccuracy = null;
	public static boolean skipGPSTracking = false;
	public static String registeredUsername = null;
	public static Integer cellarSortType = null;
	
	public static int getCellarSortType(Activity activity) 
	{
		if (cellarSortType == null)
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

			cellarSortType = settings.getInt(DryncUtils.CELLAR_SORT, BottleComparator.BY_NAME);
		}
		return cellarSortType;
	}
	
	public static void setCellarSortType(Activity activity, int curSort)
	{
		DryncUtils.cellarSortType = curSort;
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putInt(DryncUtils.CELLAR_SORT, DryncUtils.cellarSortType);
		editor.commit();
		
	}
	
	public static String getRegisteredUsername() {
		return registeredUsername;
	}

	public static void setRegisteredUsername(String registeredUsername) {
		DryncUtils.registeredUsername = registeredUsername;
	}

	public static boolean isFreeMode() {
		if (freeMode == null)
		{
			freeMode = producttype.equals(PRODUCT_ID_FREE);
		}
		
		setFreeMode(freeMode);
		
		return freeMode;
	}

	public static void setFreeMode(boolean freeMode) {
		DryncUtils.freeMode = freeMode;
		
		if (freeMode)
		{
			DryncUtils.setDryncFlurryCode(DRYNC_FREE_FLURRY_CODE);
			DryncUtils.setProductId(PRODUCT_ID_FREE);
		}
		else
		{
			DryncUtils.setDryncFlurryCode(DRYNC_PRO_FLURRY_CODE);
			DryncUtils.setProductId(PRODUCT_ID_PAID);
		}
	}

	public static long getMyAcctGetLastUpdatedTimestamp() {
		return myAcctGetLastUpdatedTimestamp;
	}

	public static void setMyAcctGetLastUpdatedTimestamp(
			long myAcctGetLastUpdatedTimestamp) {
		DryncUtils.myAcctGetLastUpdatedTimestamp = myAcctGetLastUpdatedTimestamp;
	}

	public static long getCellarLastUpdatedTimestamp() {
		return cellarLastUpdatedTimestamp;
	}

	public static void setCellarLastUpdatedTimestamp(long cellarLastUpdatedTimestamp) {
		DryncUtils.cellarLastUpdatedTimestamp = cellarLastUpdatedTimestamp;
	}

	public static CookieStore getCookieStore() {
		return cookieStore;
	}

	public static void setCookieStore(CookieStore cookieStore) {
		DryncUtils.cookieStore = cookieStore;
	}

	private static String deviceId = null;
	private static String etag;
	private static boolean twitterAuthorized = false;
	private static boolean facebookAuthorized = false;
	private static boolean cellarTweetsEnabled = false;
	
	//Shared Prefs
	public static final String PREFS_NAME = "DRYNC_PREFS";
	public static final String DEVICE_ID = "deviceId";
	public static final String SHOW_INTRO_PREF = "showIntro";
	public static final String LAST_QUERY_PREF = "lastQuery";
	public static final String LAST_FILTER_PREF = "lastFilter";
	public static final String CELLAR_ETAG = "cellarEtag";
	public static final String CELLAR_SORT = "cellarSort";
	public static final String LAST_LOCATION_LAT = "lastLocationLat";
	public static final String LAST_LOCATION_LONG = "lastLocationLong";
	public static final String LAST_LOCATION_ACCURACY = "lastLocationAccuracy";
	/*public static final String TWITTER_USERNAME_PREF = "twitter_username";
	public static final String TWITTER_PASSWORD_PREF = "twitter_password";*/
	public static final String CELLARTWT_PREF = "twitter_cellartweet";
	public static final String TWITTER_AUTHORIZED = "twitter_authorized";
	public static final String FACEBOOK_AUTHORIZED = "facebook_authorized";
	/*public static final String TWITTER_PW_ENCRYPT_SEED = "red truck chardonnay";*/
	public static final String UUID_KEY = "UuidKey";
	public static final String LAST_UUID_VAL = "lastUuidVal";

	/*private static boolean checkForDebugMode() 
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
		
	}*/

	public static String getCacheFileName(Context ctx, String url) {
		builder.setLength(0);
		builder.append(getCacheDir(ctx));
		builder.append(url.hashCode()).append(".jpg");
		return builder.toString();
	}

	/*public static String encryptTwitterPassword(String password)
	{
		try {
			return SimpleCrypto.encrypt(DryncUtils.TWITTER_PW_ENCRYPT_SEED, password);
		} catch (Exception e) {
			Log.e("DryncUtil", "Could not encrypt password!", e);
		}
		return null;
	}*/
	
	/*public static String decryptTwitterPassword(String encrypted)
	{
		if ((encrypted != null) && (! encrypted.equals("")))
		{
			try {
				return SimpleCrypto.decrypt(DryncUtils.TWITTER_PW_ENCRYPT_SEED, encrypted);
			} catch (Exception e) {
				Log.e("DryncUtil", "Could not decrypt password!", e);
			}
		}
		return null;
	}*/
	/*public static String getCacheDir()
	{
		try {
			return getCacheDir(null);
		} catch (DryncConfigException e) {
			Log.e("DryncUtil", "DryncConfigException: " + e.getMessage());
		}
		
		return  this is probably where it should go, so let's let it work, at least: 
		    "/data/data/com.drync.android/cache/";
	}*/
	public static String getCacheDir(Context context) 
	{
		if (DryncUtils.CACHE_DIRECTORY == null)
		{
			if (context == null)
				return null;  // fail this way.
			
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
	
	public static String getEtag(Activity activity)
	{
		if (etag == null)
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

			etag = settings.getString(DryncUtils.CELLAR_ETAG, null);
		}
		return etag;
	}
	
	public static void setEtag(Activity activity, String etag)
	{
		DryncUtils.etag = etag;
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putString(DryncUtils.CELLAR_ETAG, DryncUtils.etag);
		editor.commit();
		
	}
	
	public static void setLastKnownLocation(Activity activity, String lastlocationlat, String lastlocationlong, String lastlocationaccuracy)
	{
		DryncUtils.lastLocationLat = lastlocationlat;
		DryncUtils.lastLocationLong = lastlocationlong;
		DryncUtils.lastLocationAccuracy = lastlocationaccuracy;
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putString(DryncUtils.LAST_LOCATION_LAT, DryncUtils.lastLocationLat);
		editor.putString(DryncUtils.LAST_LOCATION_LONG, DryncUtils.lastLocationLong);
		editor.putString(DryncUtils.LAST_LOCATION_ACCURACY, DryncUtils.lastLocationAccuracy);
		editor.commit();
	}
	
	public static String getLastKnownLocationLat(Activity activity)
	{
		if ((lastLocationLat == null) && (activity != null))
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

			lastLocationLat = settings.getString(DryncUtils.LAST_LOCATION_LAT, null);
			lastLocationLong = settings.getString(DryncUtils.LAST_LOCATION_LONG, null);
			lastLocationAccuracy = settings.getString(DryncUtils.LAST_LOCATION_ACCURACY, null);
		}
		return lastLocationLat;
	}
	
	public static String getLastKnownLocationLong(Activity activity)
	{
		if ((lastLocationLong == null) && (activity != null))
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

			lastLocationLat = settings.getString(DryncUtils.LAST_LOCATION_LAT, null);
			lastLocationLong = settings.getString(DryncUtils.LAST_LOCATION_LONG, null);
			lastLocationAccuracy = settings.getString(DryncUtils.LAST_LOCATION_ACCURACY, null);
		}
		return lastLocationLong;
	}
	
	public static String getLastKnownLocationAccuracy(Activity activity)
	{
		if ((lastLocationAccuracy== null) && (activity != null))
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
			
			lastLocationLat = settings.getString(DryncUtils.LAST_LOCATION_LAT, null);
			lastLocationLong = settings.getString(DryncUtils.LAST_LOCATION_LONG, null);
			lastLocationAccuracy = settings.getString(DryncUtils.LAST_LOCATION_ACCURACY, null);
		}
		return lastLocationAccuracy;
	}
	
	public static boolean isTwitterAuthorized(Activity activity)
	{
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

		twitterAuthorized = settings.getBoolean(DryncUtils.TWITTER_AUTHORIZED, false);
		
		return twitterAuthorized;
	}
	
	public static void setTwitterAuthorized(Activity activity, boolean twitter_authorized)
	{
		DryncUtils.twitterAuthorized = twitter_authorized;
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putBoolean(DryncUtils.TWITTER_AUTHORIZED, DryncUtils.twitterAuthorized);
		editor.commit();
		
	}
	
	public static boolean isFacebookAuthorized(Activity activity)
	{
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

		facebookAuthorized = settings.getBoolean(DryncUtils.FACEBOOK_AUTHORIZED, false);
		
		return facebookAuthorized;
	}
	
	public static void setFacebookAuthorized(Activity activity, boolean facebook_authorized)
	{
		DryncUtils.facebookAuthorized = facebook_authorized;
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putBoolean(DryncUtils.FACEBOOK_AUTHORIZED, DryncUtils.facebookAuthorized);
		editor.commit();
	}
	
	public static boolean isCellarTweetsEnabled(Activity activity)
	{
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

		cellarTweetsEnabled = settings.getBoolean(DryncUtils.CELLARTWT_PREF, true);
		
		return cellarTweetsEnabled;
	}
	
	public static void setCellarTweetsEnabled(Activity activity, boolean cellarTweetsEnabled)
	{
		DryncUtils.cellarTweetsEnabled = cellarTweetsEnabled;
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putBoolean(DryncUtils.CELLARTWT_PREF, DryncUtils.cellarTweetsEnabled);
		editor.commit();
	}
	
	
	public static void setNewDeviceId(ContentResolver resolver, Activity activity, String newdeviceId)
	{
		if (newdeviceId != null)
		{
			newdeviceId = newdeviceId.trim();
		}
		
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		
		Editor editor = settings.edit();
		editor.putString(DryncUtils.DEVICE_ID, newdeviceId);
		editor.commit();	
		
		deviceId = newdeviceId;
	}
	
	public static String getDeviceId(ContentResolver resolver, Activity activity)
	{
		if (deviceId == null)
		{
			SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

			String savedDevId = settings.getString(DryncUtils.DEVICE_ID, null);
			if (savedDevId == null)
			{
				String sysDevId = generateNewDeviceId(activity.getBaseContext()); //Settings.System.getString(resolver, Settings.System.ANDROID_ID);
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
		ErrorReporter.getInstance().putCustomData("devid", deviceId);
		
		return deviceId;
	}
	
	public static Boolean isRunningOnEmulator(ContentResolver resolver)
	{
		String sysDevId = Settings.System.getString(resolver, Settings.System.ANDROID_ID);
		if (sysDevId == null)
			return true;
		
		return false;
	}
	
	public static String buildShareEmailText(Context ctx, Bottle bottle)
	{
		Cork cork = null;
		if (bottle instanceof Cork)
			cork = (Cork)bottle;
			
		StringBuilder builder = new StringBuilder();
		
		builder.append(ctx.getResources().getString(R.string.emailsubject)).append("\n\n");
		
		builder.append(bottle.getName());
		
		if ((bottle.getRating() != null) && (!bottle.getRating().equals("")) && (!bottle.getRating().equals("n/a")))
		{
			builder.append("\nAvg. Rating: ").append(bottle.getRating());
		}
		
		if ((cork != null) && (cork.getCork_rating() != 0))
		{
			builder.append("\nMy Rating: ").append(cork.getCork_rating()).append(" / 5\n");
		}
		
		if( (bottle.getPrice() != null) || ! bottle.getPrice().equals(""))
		{
			builder.append("\nPrice: ").append(bottle.getPrice()).append("\n\n");
		}
		
		if (bottle.getSources().size() > 0)
		{
			builder.append("You can buy this wine here: \n");

			for (Source source : bottle.getSources())
			{
				builder.append(source.getUrl()).append("\n");
			}
		}
		
		builder.append("\n");
		
		builder.append(ctx.getResources().getString(R.string.emailfooter)).append("\n");
		builder.append(ctx.getResources().getString(R.string.emailfooter2));
		
		return builder.toString();
	}
	
	public static String readFileAsString(String filePath) throws java.io.IOException{
	    byte[] buffer = new byte[(int) new File(filePath).length()];
	    BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath));
	    f.read(buffer);
	    return new String(buffer);
	}

	private static String generateNewDeviceId(Context ctx)
	{
		boolean supportsCpuAbi = false;
		boolean supportsManufacturer = false;
		Field cpuAbiField = null;
		try {
			cpuAbiField = Build.class.getField("CPU_ABI");
			if (cpuAbiField != null)
				supportsCpuAbi = true;
		} catch (SecurityException e1) {
				supportsCpuAbi = false;
		} catch (NoSuchFieldException e1) {
			supportsCpuAbi = false;
		}
		
		Field manufacturer = null;
		try {
			manufacturer = Build.class.getField("MANUFACTURER");
			if (manufacturer != null)
				supportsManufacturer = true;
		} catch (SecurityException e1) {
			supportsManufacturer = false;
		} catch (NoSuchFieldException e1) {
			supportsManufacturer = false;
		}
		
		try
		{

			TelephonyManager TelephonyMgr = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
			String m_szImei = TelephonyMgr.getDeviceId(); // Requires READ_PHONE_STATE

			StringBuilder m_szDevIDShort = new StringBuilder("35"); //we make this look like a valid IMEI
			m_szDevIDShort.append(Build.BOARD.length()%10);
			m_szDevIDShort.append(Build.BRAND.length()%10);
			
			if (supportsCpuAbi && (cpuAbiField != null)) { m_szDevIDShort.append(((String)cpuAbiField.get(Build.class)).length()%10); };
			m_szDevIDShort.append(Build.DEVICE.length()%10);
			m_szDevIDShort.append(Build.DISPLAY.length()%10);
			m_szDevIDShort.append(Build.HOST.length()%10);
			m_szDevIDShort.append(Build.ID.length()%10);
			if (supportsManufacturer && (manufacturer != null)) { m_szDevIDShort.append(((String)manufacturer.get(Build.class)).length()%10); };
			
			m_szDevIDShort.append(Build.MODEL.length()%10);
			m_szDevIDShort.append(Build.PRODUCT.length()%10);
			m_szDevIDShort.append(Build.TAGS.length()%10);
			m_szDevIDShort.append(Build.TYPE.length()%10);
			m_szDevIDShort.append(Build.USER.length()%10); //13 digits

			String m_szAndroidID = Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
			if (m_szAndroidID == null)
			{
				// probably an emulator - return "droidem"
				m_szAndroidID = "droidem";
			}
				

			/*	WifiManager wm = (WifiManager)ctx.getSystemService(Context.WIFI_SERVICE);
		String m_szWLANMAC = wm.getConnectionInfo().getMacAddress();
			 */
			String m_szBTMAC = "";
			try
			{
				Class clazz = Class.forName("android.bluetooth.BluetoothAdapter");


				Method method = clazz.getMethod("getDefaultAdapter", new Class[0]);
				Object m_BluetoothAdapter = method.invoke(clazz, new Object[0]);

				if (m_BluetoothAdapter != null) 
				{

					Method getAddrMethod = clazz.getMethod("getAddress", new Class[0]);
					String address = (String)getAddrMethod.invoke(m_BluetoothAdapter, new Object[0]);
					m_szBTMAC = address;
				}
			}
			catch (Throwable t)
			{
				m_szBTMAC = "";
			}

			String m_szLongID = m_szImei + m_szDevIDShort.toString() + m_szAndroidID+/* m_szWLANMAC +*/ m_szBTMAC;

			// compute md5
			MessageDigest m = null;
			try {
				m = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			m.update(m_szLongID.getBytes(),0,m_szLongID.length());
			// get md5 bytes
			byte p_md5Data[] = m.digest();
			// create a hex string
			String m_szUniqueID = new String();
			for (int i=0;i
			<p_md5Data.length;i++) {
				int b =  (0xFF & p_md5Data[i]);
				// if it is a single digit, make sure it have 0 in front (proper padding)
				if (b <= 0xF) m_szUniqueID+="0";
				// add number to string
				m_szUniqueID+=Integer.toHexString(b);
			}
			// hex string to uppercase
			m_szUniqueID = m_szUniqueID.toUpperCase();

			return m_szUniqueID;
		}
		catch (Exception e) // if anything bad happens, revert to android Id
		{
			Log.d("GETDEVICEID", "Couldn't calculate unique device ID, revert to AndroidId", e);
			return Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
		}
	}
	
	public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
}
