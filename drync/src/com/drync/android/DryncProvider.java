package com.drync.android;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Review;
import com.drync.android.objects.Source;
import com.drync.android.objects.Venue;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.entities.Category;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;

public class DryncProvider {
	static String SERVER_HOST="search.drync.com";
	static String TEST_SERVER_HOST="drync-test.morphexchange.com";
	static String STAGING_SERVER_HOST="staging.drync.com";
	static String DEV_SERVER_HOST="drync-build.ath.cx";
	static String USING_SERVER_HOST=SERVER_HOST;
	static int SERVER_PORT = USING_SERVER_HOST == DEV_SERVER_HOST ? 3000 : 80;
	static String URL1 = "/search?query=";
	static String URL2 = "&format=xml&device_id=";	
	
	static final String FOURSQUARE_CLIENT_ID = "N0J05DQMCNFPRY24FXVOSNTXXKWWLBMZ4ILPV1V2NPMAULB0";
	static final String FOURSQUARE_CLIENT_SECRET = "P00QNLWIZ2AAG4TJJ5OXIVZUQXLPZLVCGODK2TYFXVIU2GR0";
	static final String FOURSQUARE_CALLBACK_URL = "http://www.drync.com";
	
	static String CORKLISTURL = "/corks?format=xml&device_id=";
	
	public static final int TOP_POPULAR = 0;
	static final String TOP_POPULAR_URL = "/top/popular.xml";
	public static final int TOP_WANTED = 1;
	static final String TOP_WANTED_URL = "/top/wanted.xml";
	public static final int TOP_FEATURED = 2; 
	static final String TOP_FEATURED_URL = "/top/featured.xml";

	//static String URL3 = "/search?query=napa+cab&format=xml&device_id=test";
	
	private static final DryncProvider sInstance = new DryncProvider();
	
	public static DryncProvider getInstance() {
		return sInstance;
	}
	public List<Bottle> getTopWines(String deviceId, int topType)
	{
		HttpHost target = new HttpHost(SERVER_HOST, SERVER_PORT, "http");
		return this.getTopWines(target, topType, deviceId);
	}
	
	public List<Bottle> getMatches(String deviceId, String query)
	{
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return this.searchBottles(target, query, deviceId);
	}
	
	public String startupPost(Activity activity, String deviceId)
	{
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return this.startupPost(activity, target, deviceId);
	}

	/**
	 * Call the REST service to retrieve the first matching promotion based
	 * on the give keywords. If none found, return null.
	 * @param target - the target HTTP host for the REST service.
	 * @param keywords - comma delimited keywords. May contain spaces.
	 * @return - PromoInfo that matches the keywords. If error or no match, return null.
	 */
	private List<Bottle> searchBottles(HttpHost target, String keywords, String deviceId) {
		ArrayList<Bottle> bottleList = new ArrayList<Bottle>();
		
		if(keywords==null)
			return null;

		Document doc = null;
		HttpClient client = new DefaultHttpClient();
		
		// set up deviceId
		String devId = deviceId;
		if ((deviceId == null) || (deviceId.equals("")))
				devId = "test";
		
		StringBuilder bldr = new StringBuilder();
		bldr.append(URL1).append(keywords.replaceAll(" ", "+").replaceAll("\"", "")).append(URL2).append(devId);
		Log.d("DryncPrvdr", "Loading Wines: " + bldr.toString());
		HttpGet get = new HttpGet(bldr.toString());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			HttpEntity entity = client.execute(target, get).getEntity();

			doc = builder.parse(entity.getContent());
			NodeList bottles = doc.getElementsByTagName("bottle");

			if(bottles!=null) 
			{
				for(int j=0,m=bottles.getLength();j<m;j++)
				{
					Node bottleNode = bottles.item(j);
					
					Bottle bottle = parseBottleFromNode(bottleNode);
					if (bottle != null)
					{
						bottleList.add(bottle);
					}
     			}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		return bottleList;
	}
	
	/**
	 * Call the REST service to retrieve the first matching promotion based
	 * on the give keywords. If none found, return null.
	 * @param target - the target HTTP host for the REST service.
	 * @param keywords - comma delimited keywords. May contain spaces.
	 * @return - PromoInfo that matches the keywords. If error or no match, return null.
	 */
	private List<Bottle> getTopWines(HttpHost target, int topType, String deviceId) {
		ArrayList<Bottle> bottleList = new ArrayList<Bottle>();
		
		Document doc = null;
		HttpClient client = new DefaultHttpClient();
		
		// set up deviceId
		String devId = deviceId;
		if ((deviceId == null) || (deviceId.equals("")))
				devId = "test";
		
		String subUrl = TOP_POPULAR_URL;
		if (topType == DryncProvider.TOP_FEATURED)
			subUrl = TOP_FEATURED_URL;
		else if (topType == DryncProvider.TOP_WANTED)
			subUrl = TOP_WANTED_URL;
			
		
		StringBuilder bldr = new StringBuilder();
		bldr.append(subUrl);
		Log.d("DryncPrvdr", "Loading Wines: " + bldr.toString());
		HttpGet get = new HttpGet(bldr.toString());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			HttpEntity entity = client.execute(target, get).getEntity();

			doc = builder.parse(entity.getContent());
			NodeList bottles = doc.getElementsByTagName("bottle");

			if(bottles!=null) {
				for(int j=0,m=bottles.getLength();j<m;j++)
				{
					Node bottleNode = bottles.item(j);

					Bottle bottle = parseBottleFromNode(bottleNode);
					if (bottle != null)
					{
						bottleList.add(bottle);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		return bottleList;
	}
	
	/*public ArrayList<Bottle> parseBottlesFromXmlString(String xml)
	{
		Document doc = null;
		ArrayList<Bottle> bottleList = new ArrayList<Bottle>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			doc = builder.parse(xml);
			NodeList bottles = doc.getElementsByTagName("bottle");

			if(bottles!=null) {
				for(int j=0,m=bottles.getLength();j<m;j++)
				{
					Node bottleNode = bottles.item(j);

					Bottle bottle = parseBottleFromNode(bottleNode);
					if (bottle != null)
					{
						bottleList.add(bottle);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		return bottleList;
	}*/
	
	public ArrayList<Cork> parseCorksFromXmlString(String xml, String tagname)
	{
		ArrayList<Cork> bottleList = new ArrayList<Cork>();
		Document doc = null;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			DocumentBuilder builder = factory.newDocumentBuilder();

		    StringReader reader = new StringReader(xml);
		    InputSource isrc = new InputSource(reader);
		    
			doc = builder.parse(isrc);
			NodeList bottles = doc.getElementsByTagName(tagname);

			if(bottles!=null) {
				for(int j=0,m=bottles.getLength();j<m;j++)
				{
					Node bottleNode = bottles.item(j);

					Cork bottle = parseCorkFromNode(bottleNode);

					if (bottle != null)
					{
						bottleList.add(bottle);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		
		return bottleList;
	}
	
	public synchronized List<Cork> getCorks(Activity activity, String deviceId) throws DryncHostException, DryncXmlParseExeption
	{
		// little safety to prevent this from happening too often.
		if ((DryncUtils.getCellarLastUpdatedTimestamp() != -1) && 
				(System.currentTimeMillis() - DryncUtils.getCellarLastUpdatedTimestamp()) < 20000)
		{
			Log.d("DryncProvider", "skip cork get, too recent to fetch it again.");
			return null;
		}
		
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return getCorks(activity, target, deviceId);
	}
	
	private HttpResponse doCorksGet(Activity activity, HttpHost target, String deviceId) throws DryncHostException
	{
		HttpClient client = new DefaultHttpClient();

		// set up deviceId
		String devId = deviceId;
		if ((deviceId == null) || (deviceId.equals("")))
			devId = "test";			

		StringBuilder bldr = new StringBuilder();
		bldr.append(CORKLISTURL);
		bldr.append(devId);

		Log.d("DryncPrvdr", "Loading Corks: " + bldr.toString());

		HttpResponse response = null;
		try
		{
			HttpGet get = new HttpGet(bldr.toString());
			if (DryncUtils.getEtag(activity) != null)
			{
				get.addHeader("If-None-Match", DryncUtils.getEtag(activity));
			}
				
			response = client.execute(target, get);
			Header[] eTaghdrs = response.getHeaders("ETag");
			
			if (eTaghdrs.length > 0)
			{
				String etagheader = eTaghdrs[0].getValue();
				DryncUtils.setEtag(activity, etagheader);
			}			
		}
		catch (UnknownHostException e)
		{
			Log.e("DryncProvider", "Caught UnknownHostException fetching corks.");
			throw new DryncHostException(e);
		}
		catch (IOException e)
		{
			Log.e("DryncProvider", "Caught IOException fetching corks.");
			throw new DryncHostException(e);
		}
		
		return response;
	}

	/*public boolean getCorksToFile(Activity activity, String deviceId) {
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return getCorksToFile(activity, target, deviceId);
	}*/
	
/*	private boolean getCorksToFile(Activity activity, HttpHost target, String deviceId) {
		
		boolean wroteContent = false;
		InputStream is = null;

		try {
			HttpResponse response = doCorksGet(activity, target, deviceId);
			HttpEntity entity = response.getEntity();

			//DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			//DocumentBuilder builder = factory.newDocumentBuilder();

			StatusLine sl = response.getStatusLine();
			if (sl.getStatusCode() != 200)
				return false;

			if (entity != null) 
			{
				// read in content
				is = entity.getContent();

				final char[] buffer = new char[0x10000];
				StringBuilder out = new StringBuilder();
				Reader in = new InputStreamReader(is, "UTF-8");
				int read;
				do 
				{
					read = in.read(buffer, 0, buffer.length);
					if (read>0) {
						String stringcontent = new String(buffer);
						if (! stringcontent.trim().equals(""))
						{
							out.append(buffer, 0, read);
							wroteContent = true;
						}
					}
				}
				while (read>=0);

				// write out to file.
				if ((out.toString() != null) &&
						(! out.toString().equals("")) && wroteContent)
				{
					String filename = DryncUtils.getCacheDir() + "cellar.xml";
					File outputFile = new File(filename);
					//use buffering
					Writer output = new BufferedWriter(new FileWriter(outputFile));
					try {
						//FileWriter always assumes default encoding is OK!
						output.write( out.toString() );
					}
					finally {
						output.close();
					}
					
					return true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}  */
	
	/**
	 * Call the REST service to retrieve the first matching promotion based
	 * on the give keywords. If none found, return null.
	 * @param target - the target HTTP host for the REST service.
	 * @param keywords - comma delimited keywords. May contain spaces.
	 * @return - PromoInfo that matches the keywords. If error or no match, return null.
	 * @throws DryncHostException 
	 * @throws DryncXmlParseExeption 
	 */
	private List<Cork> getCorks(Activity activity, HttpHost target, String deviceId) throws DryncHostException, DryncXmlParseExeption {
		ArrayList<Cork> bottleList = new ArrayList<Cork>();
		Document doc = null;
		DryncDbAdapter dbAdapter = new DryncDbAdapter(activity);
		
		HttpResponse response = null;
		
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			response = doCorksGet(activity, target, deviceId);
			
			if (response.getStatusLine().getStatusCode() == 304) // do nothing!
			{
				return null;
			} else if (response.getStatusLine().getStatusCode() < 400)
			{
				dbAdapter.open();
				dbAdapter.clearCorks(true);
				
				HttpEntity entity = response.getEntity();

				doc = builder.parse(entity.getContent());
				NodeList bottles = doc.getElementsByTagName("bottle");

				if(bottles!=null) {
					for(int j=0,m=bottles.getLength();j<m;j++)
					{
						Node bottleNode = bottles.item(j);

						Cork bottle = parseCorkFromNode(bottleNode);
						if (bottle != null)
						{
							long result = -1;
							try
							{
								result = dbAdapter.insertOrUpdateCork((Cork)bottle, true);
								if (result >= 0)
									bottleList.add(bottle);
							}
							catch (DryncFreeCellarExceededException e)
							{
								// intentionally ignore here.
							}
						}
					}
				}
				DryncUtils.setCellarLastUpdatedTimestamp(System.currentTimeMillis());
			}
		} catch (DryncHostException e) { // catch & rethrow - only catching to handle the finally.
			throw e;
		}
		catch(Exception e) {
			Log.e("DryncProvider", "caught exception getting corks.");
			throw new DryncXmlParseExeption(e);
		} finally {
			dbAdapter.close();
		}
		
		return bottleList;
	}
	
	/*public List<Cork> getCorksFromFile() {
		ArrayList<Cork> bottleList = new ArrayList<Cork>();
		Document doc = null;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			DocumentBuilder builder = factory.newDocumentBuilder();

			FileInputStream fin = new FileInputStream(DryncUtils.getCacheDir() + "cellar.xml");
			
			doc = builder.parse(fin);
			NodeList bottles = doc.getElementsByTagName("bottle");

			if(bottles!=null) {
				for(int j=0,m=bottles.getLength();j<m;j++)
				{
					Node bottleNode = bottles.item(j);

					Cork bottle = parseCorkFromNode(bottleNode);
					if (bottle != null)
					{
						bottleList.add(bottle);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		
		return bottleList;
	} */
	
	
	
	/**
	 * Call the REST service to retrieve the first matching promotion based
	 * on the give keywords. If none found, return null.
	 * @param target - the target HTTP host for the REST service.
	 * @param keywords - comma delimited keywords. May contain spaces.
	 * @return - PromoInfo that matches the keywords. If error or no match, return null.
	 */
	public String startupPost(Activity activity, HttpHost target, String deviceId) {
		// http://{hostname}/app_session?device_id={device_id}&prod={product_selector}
		String urlPost1 = "/app_session";
		Document doc = null;
		HttpClient client = new DefaultHttpClient();

		// set up deviceId
		String devId = deviceId;
		String fake = "UDID-droid-fake-888888888888888888888888888890";
		if ((deviceId == null) || (deviceId.equals("")))
			devId = "UDID-droid-fake-" + System.currentTimeMillis();

		StringBuilder bldr = new StringBuilder();
		bldr.append(urlPost1);
		Log.d("DryncPrvdr", "Startup Post: " + bldr.toString());
		HttpPost post = new HttpPost(bldr.toString());
		post.addHeader("X-UDID", devId);
		post.addHeader("Accept", "text/iphone");
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("device_id", devId));
		nvps.add(new BasicNameValuePair("prod", DryncUtils.getProductId()));
		String lastlat = DryncUtils.getLastKnownLocationLat(activity);
		String lastlong = DryncUtils.getLastKnownLocationLong(activity);
		if ((lastlat != null) && (lastlong != null))
		{
			nvps.add(new BasicNameValuePair("location[latitude]", lastlat));
			nvps.add(new BasicNameValuePair("location[longitude]", lastlong));
		}

		try {
			post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		boolean wroteContent = false;
		InputStream is = null;
		try {
			HttpResponse response = client.execute(target, post);
			HttpEntity entity = response.getEntity();
			Header accountHeader = response.getFirstHeader("X-Drync-Account");

			StatusLine sl = response.getStatusLine();
			if (sl.getStatusCode() != 200)
				return null;

			if (entity != null) {
				is = entity.getContent();
				String username = null;
				
				if (accountHeader != null)
				{
					JSONObject object = (JSONObject) new JSONTokener(accountHeader.getValue()).nextValue();
					username = (String) object.get("user_name");
					if (! username.equals(""))
						DryncUtils.setRegisteredUsername(username);
				}

				final char[] buffer = new char[0x10000];
				StringBuilder out = new StringBuilder();
				Reader in = new InputStreamReader(is, "UTF-8");
				int read;
				do 
				{
					read = in.read(buffer, 0, buffer.length);
					if (read>0) {
						String stringcontent = new String(buffer);
						if (! stringcontent.trim().equals(""))
						{
							out.append(buffer, 0, read);
							wroteContent = true;
						}
					}
				}
				while (read>=0);

				if ((out.toString() != null) &&
						(! out.toString().equals("")) && wroteContent)
				{
					if (DryncUtils.isDebugMode)
					{
						String cachedir = DryncUtils.getCacheDir(activity);
						if (cachedir != null)
						{
							String filename = cachedir + "register.html";
							File outputFile = new File(filename);
							//use buffering
							Writer output = new BufferedWriter(new FileWriter(outputFile));
							try {
								//FileWriter always assumes default encoding is OK!
								output.write( out.toString() );
							}
							finally {
								output.close();
							}
						}
					}
				}
				return out.toString();
			}
		}
		catch( Exception e)
		{
			Log.e("StartupPost", "Error posting or reading Startup Post", e);
		}
		finally
		{
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					Log.w("StartupPost", "Error posting or reading Startup Post", e);
				}
		}

		return null;
	}
	
	public synchronized String myAcctGet(Context ctx, String deviceId) throws DryncHostException {
		
		// little safety to prevent this from happening too often.
		if ((DryncUtils.getMyAcctGetLastUpdatedTimestamp() != -1) && 
				(System.currentTimeMillis() - DryncUtils.getMyAcctGetLastUpdatedTimestamp()) < 60000)
		{
			Log.d("DryncProvider", "skip account get, too recent to fetch it again.");
			return null;
		}
		
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		DryncUtils.setMyAcctGetLastUpdatedTimestamp(System.currentTimeMillis());
		return myAcctGet(ctx, target, deviceId);
	}
	
	public synchronized String myAcctGet(Context ctx, HttpHost target, String deviceId) throws DryncHostException {
		String cachedir = DryncUtils.getCacheDir(ctx);
		if (cachedir != null)
		{
			String filename = cachedir  + "myacct.html";

			File myacctfile = new File(filename);

			StringBuilder urlGet1 = new StringBuilder("/register");
			urlGet1.append("?device_id=");
			Document doc = null;
			HttpClient client = new DefaultHttpClient();
			CookieStore cookieStore = new BasicCookieStore();
			((DefaultHttpClient)client).setCookieStore(cookieStore);

			// set up deviceId
			String devId = deviceId;
			String fake = "UDID-droid-fake-888888888888888888888888888890";
			if ((deviceId == null) || (deviceId.equals("")))
				devId = "UDID-droid-fake-" + System.currentTimeMillis();

			urlGet1.append(devId).append("&prod=").append(DryncUtils.getProductId());

			Log.d("DryncPrvdr", "Get My Account Page: " + urlGet1.toString());
			HttpGet get = new HttpGet(urlGet1.toString());
			get.addHeader("X-UDID", devId);
			get.addHeader("Accept", "text/iphone");

			boolean wroteContent = false;
			InputStream is = null;
			try {
				HttpResponse response = client.execute(target, get);
				HttpEntity entity = response.getEntity();

				List<Cookie> cookies = cookieStore.getCookies();
				DryncUtils.setCookieStore(cookieStore);

				StatusLine sl = response.getStatusLine();
				if (sl.getStatusCode() != 200)
					return null;

				if (entity != null) {
					is = entity.getContent();

					final char[] buffer = new char[0x10000];
					StringBuilder out = new StringBuilder();
					Reader in = new InputStreamReader(is, "UTF-8");
					int read;
					do 
					{
						read = in.read(buffer, 0, buffer.length);
						if (read>0) {
							String stringcontent = new String(buffer);
							if (! stringcontent.trim().equals(""))
							{
								out.append(buffer, 0, read);
								wroteContent = true;
							}
						}
					}
					while (read>=0);

					if ((out.toString() != null) &&
							(! out.toString().equals("")) && wroteContent)
					{

						File outputFile = new File(filename);
						//use buffering
						Writer output = new BufferedWriter(new FileWriter(outputFile));
						try {
							//FileWriter always assumes default encoding is OK!
							output.write( out.toString() );
						}
						finally {
							output.close();
						}

					}
					return out.toString();
				}
			}
			catch (UnknownHostException e)
			{
				Log.e("StartupGet", "Caught UnknownHostException getting my account page.");
				throw new DryncHostException(e);
			}
			catch( Exception e)
			{
				Log.e("StartupGet", "Error getting my account page", e);
			}
			finally
			{
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
						Log.w("StartupGet", "Error posting or reading Startup Get", e);
					}
			}
		}

		return null;
	}

	private Bottle parseBottleFromNode(Node bottleNode)
	{
		
		if (bottleNode != null)
		{
			Bottle bottle=new Bottle();
			NodeList nodeList = bottleNode.getChildNodes();
			int len = nodeList.getLength();
			for(int i=0; i<len; i++) {
				try
				{
					Node node = nodeList.item(i);
					String value = this.getNodeValue(node);
					parseBottlePortions(node, value, bottle);
					/*if("name".equals(node.getNodeName())) {
						bottle.setName(value);
					} else if("year".equals(node.getNodeName())) {
						bottle.setYear(Integer.parseInt(value));
					} else if("region".equals(node.getNodeName())) {
						bottle.setRegion(value);
					} else if("region_path".equals(node.getNodeName())) {
						bottle.setRegion_path(value);
					} else if("style".equals(node.getNodeName())) {
						bottle.setStyle(value);
					} else if ("label_thumb".equals(node.getNodeName())) {
						bottle.setLabel_thumb(value);
					} else if ("price".equals(node.getNodeName())) {
						bottle.setPrice(value);
					} else if ("rating".equals(node.getNodeName())) {
						bottle.setRating(value);
					} else if ("grape".equals(node.getNodeName())) {
						bottle.setGrape(value);
					} else if ("reviews".equals(node.getNodeName())) {
						Node reviews = node;
						NodeList reviewChildren = reviews.getChildNodes();
						int rcLen = reviewChildren.getLength();
						for (int k=0;k<rcLen;k++)
						{
							if (reviewChildren.item(k).getNodeName().equals("review"))
							{
								Node reviewNode = reviewChildren.item(k);
								
								Review parsedReview = parseReviewFromNode(reviewNode);
								bottle.addReview(parsedReview);
							}
						}
					} else if ("sources".equals(node.getNodeName())) {
						Node sources = node;
						NodeList srcChildren = sources.getChildNodes();
						int rcLen = srcChildren.getLength();
						for (int k=0;k<rcLen;k++)
						{
							if (srcChildren.item(k).getNodeName().equals("source"))
							{
								Node srcNode = srcChildren.item(k);

								Source parsedSource = parseSourceFromNode(srcNode);
								
								bottle.addSource(parsedSource);
							}
						}
					}// else skip for now.
*/
				} catch (NumberFormatException e)
				{
					// skip for now.
				}
			}
			
			return bottle;
		} // end if (bottleNode != null)
		else
			return null;
	}
	
	private Cork parseCorkFromNode(Node corkNode)
	{
		
		if (corkNode != null)
		{
			Cork bottle=new Cork();
			NodeList nodeList = corkNode.getChildNodes();
			int len = nodeList.getLength();
			for(int i=0; i<len; i++) {
				try
				{
					Node node = nodeList.item(i);
					String value = this.getNodeValue(node);
					parseBottlePortions(node, value, bottle);
					if("cork_id".equals(node.getNodeName())) {
						bottle.setCork_id(Long.parseLong(value));
					} else if("cork_uuid".equals(node.getNodeName())) {
						bottle.setCork_uuid(value);
					} else if ("cork_created_at".equals(node.getNodeName())) {
						bottle.setCork_created_at(value);
					} else if ("description".equals(node.getNodeName())) {
						bottle.setDescription(value);
					} else if ("grape".equals(node.getNodeName())) {
						bottle.setGrape(value);
					} else if ("cork_rating".equals(node.getNodeName())) {
						if (value != null)
							bottle.setCork_rating(Float.parseFloat(value));
					} else if ("location".equals(node.getNodeName())) {
						bottle.setLocation(value);
					} else if ("public_note".equals(node.getNodeName())) {
						bottle.setPublic_note(value);
					} else if ("cork_bottle_count".equals(node.getNodeName())) {
						if (value != null)
							bottle.setCork_bottle_count(Integer.parseInt(value));
						else
							bottle.setCork_bottle_count(0);
					} else if ("cork_want".equals(node.getNodeName())) {
						bottle.setCork_want(Boolean.valueOf(value));
				    } else if ("cork_own".equals(node.getNodeName())) {
						bottle.setCork_own(Boolean.valueOf(value));
				    } else if ("cork_drank".equals(node.getNodeName())) {
						bottle.setCork_drank(Boolean.valueOf(value));
					} else if ("cork_price".equals(node.getNodeName())) {
						bottle.setCork_price(value);
					} else if ("cork_label".equals(node.getNodeName())) {
						bottle.setCork_label(value);
					} else if ("cork_latitude".equals(node.getNodeName())) {
						bottle.setLocationLat(value);
					} else if ("cork_longitude".equals(node.getNodeName())) {
						bottle.setLocationLong(value);
					}
					// else skip for now.

				} catch (NumberFormatException e)
				{
					// skip for now.
					e.printStackTrace();
				}
			}
			
			return bottle;
		} // end if (bottleNode != null)
		else
			return null;
	}
	
	private void parseBottlePortions(Node node, String value, Bottle bottle)
	{
		if("name".equals(node.getNodeName())) {
			bottle.setName(value);
		} else if("year".equals(node.getNodeName())) {
			try
			{
				if ((value != null) && (!value.equals("")) && (value.compareToIgnoreCase("NV") != 0))
					bottle.setYear(Integer.parseInt(value));
			}
			catch (NumberFormatException e)
			{
				// skip setting the year.
			}
		} else if("bottle_id".equals(node.getNodeName())) {
			if (value != null)
				bottle.setBottle_Id(Integer.parseInt(value));
		} else if("region".equals(node.getNodeName())) {
			bottle.setRegion(value);
		} else if("region_path".equals(node.getNodeName())) {
			bottle.setRegion_path(value);
		} else if("style".equals(node.getNodeName())) {
			bottle.setStyle(value);
		} else if ("label_thumb".equals(node.getNodeName())) {
			bottle.setLabel_thumb(value);
		} else if ("price".equals(node.getNodeName())) {
			bottle.setPrice(value);
		} else if ("minprice".equals(node.getNodeName())) {
			bottle.setMinprice(Float.parseFloat(value));
		} else if ("maxprice".equals(node.getNodeName())) {
			bottle.setMaxprice(Float.parseFloat(value));
		} else if ("rating".equals(node.getNodeName())) {
			bottle.setRating(value);
		} else if ("grape".equals(node.getNodeName())) {
			bottle.setGrape(value);
		} else if ("reviews".equals(node.getNodeName())) {
			Node reviews = node;
			NodeList reviewChildren = reviews.getChildNodes();
			int rcLen = reviewChildren.getLength();
			for (int k=0;k<rcLen;k++)
			{
				if (reviewChildren.item(k).getNodeName().equals("review"))
				{
					Node reviewNode = reviewChildren.item(k);
					
					Review parsedReview = parseReviewFromNode(reviewNode);
					bottle.addReview(parsedReview);
				}
			}
		} else if ("sources".equals(node.getNodeName())) {
			Node sources = node;
			NodeList srcChildren = sources.getChildNodes();
			int rcLen = srcChildren.getLength();
			for (int k=0;k<rcLen;k++)
			{
				if (srcChildren.item(k).getNodeName().equals("source"))
				{
					Node srcNode = srcChildren.item(k);

					Source parsedSource = parseSourceFromNode(srcNode);
					
					bottle.addSource(parsedSource);
				}
			}
		}// else skip for now.
	}
	private Review parseReviewFromNode(Node reviewNode) {
		Review review = new Review();
		
		NodeList nodeList = reviewNode.getChildNodes();
		int len = nodeList.getLength();
		for(int i=0; i<len; i++) 
		{
			try
			{
				Node node = nodeList.item(i);
				String value = this.getNodeValue(node);
				if("publisher".equals(node.getNodeName())) {
					review.setPublisher(value);
				} else if("text".equals(node.getNodeName())) {
					review.setText(value);
				} else if("url".equals(node.getNodeName())) {
					review.setUrl(value);
				} else if("review_category".equals(node.getNodeName())) {
					review.setReview_cat(value);
				} else if("review_source".equals(node.getNodeName())) {
					review.setReview_source(value);
				} // else ignore others for now.
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return review;
	}

	private Source parseSourceFromNode(Node sourceNode) {
		Source source = new Source();
		
		NodeList nodeList = sourceNode.getChildNodes();
		int len = nodeList.getLength();
		for(int i=0; i<len; i++) 
		{
			try
			{
				Node node = nodeList.item(i);
				String value = this.getNodeValue(node);
				if("name".equals(node.getNodeName())) {
					source.setName(value);
				} else if("url".equals(node.getNodeName())) {
					source.setUrl(value);
				} // else ignore others for now.
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return source;
	}


	private String getNodeValue(Node node) {
		NodeList children = node.getChildNodes();
		if(children.getLength()>0) {
			return children.item(0).getNodeValue();
		} else
			return null;
	}

	public static HttpResponse doPost(String  url, Map<String, String> kvPairs, String devId)
	throws ClientProtocolException, IOException, URISyntaxException {
		URI uri = new URI(String.format("%s?format=xml&device_id=%s",url,devId));
		HttpClient httpclient = new DefaultHttpClient();
		
		((DefaultHttpClient) httpclient).getCredentialsProvider().setCredentials(
				new AuthScope(null, -1), 
				new UsernamePasswordCredentials("preview", "drync_web"));
	
		HttpPost httppost = new HttpPost(uri);
		//httppost.addHeader("Accept", "text/iphone");

		if (kvPairs != null && kvPairs.isEmpty() == false) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
					kvPairs.size());
			String k, v;
			Iterator<String> itKeys = kvPairs.keySet().iterator();
			while (itKeys.hasNext()) {
				k = itKeys.next();
				v = kvPairs.get(k);
				nameValuePairs.add(new BasicNameValuePair(k, v));
			}
		    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
		}
		HttpResponse response;
		response = httpclient.execute(httppost);
		return response;
	} 
	
	public static com.drync.android.helpers.Result<Cork> postCreateOrUpdate(Context ctx, Cork cork, String deviceId, boolean testforfree) throws DryncFreeCellarExceededException, DryncNotRegisteredException
	{
		// check to see if we're registered.
		if (DryncUtils.getRegisteredUsername() == null)
		{
			throw new DryncNotRegisteredException();
		}
		
		if (testforfree)
		{
			DryncDbAdapter dbAdapter = new DryncDbAdapter(ctx);

			try
			{
				dbAdapter.open();
				if (DryncUtils.isFreeMode() && (dbAdapter.getCorkCount() >= DryncUtils.FREE_CELLAR_MAX_CORKS))
					throw new DryncFreeCellarExceededException();
			}
			finally
			{
				dbAdapter.close();
			}
		}

		String tweetAppend = "";
		if (DryncUtils.isCellarTweetsEnabled((Activity)ctx))
		{
			tweetAppend = "?tweet&scrawl";
		}

		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/corks%s", USING_SERVER_HOST,SERVER_PORT,tweetAppend);
		com.drync.android.helpers.Result<Cork> returnVal = new com.drync.android.helpers.Result<Cork>();
		try {
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, cork.getRepresentation(deviceId), deviceId);
			String content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			if (response.getStatusLine().getStatusCode() < 400)
			{
				DryncProvider dp = DryncProvider.getInstance();
				ArrayList<Cork> corks = dp.parseCorksFromXmlString(content, "cork");
				returnVal.setResult(true);
				returnVal.setContents(corks);
				return returnVal;
			}
			else
			{
				returnVal.setResult(false);
				return returnVal;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		returnVal.setResult(false);
		return returnVal;
		/*ClientResource itemsResource = new ClientResource(  
				clientResourceUrl);

		ChallengeResponse authentication = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "preview", "drync_web");
		itemsResource.setChallengeResponse(authentication);

		ClientResource itemResource = null;
		Representation rpre = cork.getRepresentation(deviceId);
		Representation r = itemsResource.post(rpre); 

		Log.d("DryncProvider", itemsResource.getStatus().getDescription(), itemsResource.getStatus().getThrowable());

		return itemsResource.getStatus().isSuccess();*/
	}
	
	public static com.drync.android.helpers.Result<Cork> postUpdate(Cork cork, String deviceId)
	{
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/corks/%s", USING_SERVER_HOST,SERVER_PORT,cork.getCork_id());
		com.drync.android.helpers.Result<Cork> returnVal = new com.drync.android.helpers.Result<Cork>();
		try {
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, cork.getRepresentation(deviceId, true), deviceId);
			String content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			if (response.getStatusLine().getStatusCode() < 400)
			{
				DryncProvider dp = DryncProvider.getInstance();
				ArrayList<Cork> corks = dp.parseCorksFromXmlString(content, "cork");
				returnVal.setResult(true);
				returnVal.setContents(corks);
				return returnVal;
			}
			else
			{
				returnVal.setResult(false);
				return returnVal;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		returnVal.setResult(false);
		return returnVal;
		/*ClientResource itemsResource = new ClientResource(  
				clientResourceUrl);
		
		ChallengeResponse authentication = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "preview", "drync_web");
		itemsResource.setChallengeResponse(authentication);

		ClientResource itemResource = null;
		Representation rpre = cork.getRepresentation(deviceId);
		Representation r = itemsResource.post(rpre); 
	
		Log.d("DryncProvider", itemsResource.getStatus().getDescription(), itemsResource.getStatus().getThrowable());
		
		return itemsResource.getStatus().isSuccess();*/
	}

	public static boolean postDelete(Cork cork, String deviceId)
	{
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/corks/%s", USING_SERVER_HOST,SERVER_PORT, cork.getCork_id());
		HashMap<String,String> form = new HashMap<String, String>();  
		form.put("_method", "delete");
		try {
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, form, deviceId);
			if (response.getEntity() != null)
			{
				String content = DryncProvider.convertStreamToString(response.getEntity().getContent());
				Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			}
			
			if (response.getStatusLine().getStatusCode() < 400)
				return true;
			else
				return false;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}

	public  static String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {       
			return "";
		}
	}
	
	public static boolean postTweetToFriends(Bottle bottle, String deviceId)
	{
		String targetType = "bottles";
		long targetId = bottle.getBottle_Id();
		int year = 0;
		String name;
		if (bottle instanceof Cork)
		{
			targetType = "corks";
			targetId = ((Cork)bottle).getCork_id();
			year = ((Cork)bottle).getCork_year();
			name = ((Cork)bottle).getName();
		}
		else
		{
			year = bottle.getYear();
			name = bottle.getName();
		}
		
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/%s/%d/tweets", USING_SERVER_HOST,SERVER_PORT,targetType, targetId);
		boolean returnval = false;
		try {
			Map<String, String> kvpairs = new HashMap<String, String>();
			kvpairs.put("message", null);
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, kvpairs, deviceId);
			
			String content = "";
			if (response.getEntity() != null)
				content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			if (response.getStatusLine().getStatusCode() < 400)
			{
				DryncProvider dp = DryncProvider.getInstance();
				return true;
			}
			else
			{
				return false;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//returnVal.setResult(false);
		return false;
	}
	
	public static boolean postScrawlToFriends(Bottle bottle, String deviceId)
	{
		String targetType = "bottles";
		long targetId = bottle.getBottle_Id();
		int year = 0;
		String name;
		if (bottle instanceof Cork)
		{
			targetType = "corks";
			targetId = ((Cork)bottle).getCork_id();
			year = ((Cork)bottle).getCork_year();
			name = ((Cork)bottle).getName();
		}
		else
		{
			year = bottle.getYear();
			name = bottle.getName();
		}
		
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/%s/%d/scrawls", USING_SERVER_HOST,SERVER_PORT,targetType, targetId);
		boolean returnval = false;
		try {
			Map<String, String> kvpairs = new HashMap<String, String>();
			kvpairs.put("message", null);
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, kvpairs, deviceId);
			
			String content = "";
			if (response.getEntity() != null)
				content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			if (response.getStatusLine().getStatusCode() < 400)
			{
				DryncProvider dp = DryncProvider.getInstance();
				return true;
			}
			else
			{
				return false;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//returnVal.setResult(false);
		return false;
	}
	
	public static boolean postFacebookDeauth(String deviceId)
	{
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/facebook_authorization/deauthorize", USING_SERVER_HOST,SERVER_PORT);
		boolean returnval = false;
		try {
			Map<String, String> kvpairs = new HashMap<String, String>();
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, kvpairs, deviceId);
			
			String content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			if (response.getStatusLine().getStatusCode() < 400)
			{
				DryncProvider dp = DryncProvider.getInstance();
				return true;
			}
			else
			{
				return false;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//returnVal.setResult(false);
		return false;
	}
	
	public static boolean postTwitterDeauth(String deviceId)
	{
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/twitter_authorization/deauthorize", USING_SERVER_HOST,SERVER_PORT);
		boolean returnval = false;
		try {
			Map<String, String> kvpairs = new HashMap<String, String>();
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, kvpairs, deviceId);
			
			String content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
			if (response.getStatusLine().getStatusCode() < 400)
			{
				DryncProvider dp = DryncProvider.getInstance();
				return true;
			}
			else
			{
				return false;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//returnVal.setResult(false);
		return false;
	}
	
	public ArrayList<Venue> getVenues(Location loc)
	{
		String latitude = "" + loc.getLatitude() ;
		String longitude = "" + loc.getLongitude();
		return getVenues(latitude, longitude);
	}
	public ArrayList<Venue> getVenues(String latitude, String longitude)
	{
		
		try {
			return searchVenues(latitude + "," + longitude);
		} catch (FoursquareApiException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return new ArrayList<Venue>();
		/*
		ArrayList<Venue> venueLst = new ArrayList<Venue>();
		
		HttpHost target = new HttpHost("api.foursquare.com", 80, "http");
		String foursquareUrl = "/v1/venues";
		Document doc = null;
		HttpClient client = new DefaultHttpClient();
				
		StringBuilder bldr = new StringBuilder();
		bldr.append(foursquareUrl).append("?geolat=").append(latitude).append("&geolong=").append(longitude)
			.append("&l=30");
		
		HttpGet get = new HttpGet(bldr.toString());
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			HttpEntity entity = client.execute(target, get).getEntity();

			doc = builder.parse(entity.getContent());
			NodeList venues = doc.getElementsByTagName("venue");
			
			if (venues != null)
			{
				for (int j=0,m=venues.getLength();j<m;j++)
				{
					Node venueNode = venues.item(j);
					Venue venue = parseVenueFromNode(venueNode);
					//Log.d("VENUE_GET", "Read Venue: " + venue.getName());
					venueLst.add(venue);
				}
			}
		}
		catch (Exception e)
		{
			Log.e("DRYNCPROVIDER", "Exception caught: " + e.getMessage());
			e.printStackTrace();
		}
		
		return venueLst;  */
	}
	
	private Venue parseVenueFromNode(Node venueNode)
	{
		
		if (venueNode != null)
		{
			Venue venue=new Venue();
			NodeList nodeList = venueNode.getChildNodes();
			int len = nodeList.getLength();
			for(int i=0; i<len; i++) {
				try
				{
					Node node = nodeList.item(i);
					String value = this.getNodeValue(node);
					
					if("id".equals(node.getNodeName())) {
						venue.setId(Long.parseLong(value));
					} else if("name".equals(node.getNodeName())) {
						venue.setName(value);
					} else if ("address".equals(node.getNodeName())) {
						venue.setAddress(value);
					} else if ("crossstreet".equals(node.getNodeName())) {
						venue.setCrossstreet(value);
					} else if ("city".equals(node.getNodeName())) {
						venue.setCity(value);
					} else if ("state".equals(node.getNodeName())) {
						venue.setState(value);
					} else if ("zip".equals(node.getNodeName())) {
						venue.setZip(value);
					} else if ("geolat".equals(node.getNodeName())) {
						venue.setGeolat(value);
					} else if ("geolong".equals(node.getNodeName())) {
						venue.setGeolong(value);
					} else if ("phone".equals(node.getNodeName())) {
						venue.setPhone(value);
				    } else if ("distance".equals(node.getNodeName())) {
						venue.setDistance(Long.valueOf(value));
				    } else if ("primarycategory".equals(node.getNodeName())) {
				    	NodeList catnodes = node.getChildNodes();
				    	
				    	for (int j=0,m=catnodes.getLength();j<m;j++)
				    	{
				    		Node catnode = catnodes.item(j);
				    		String catnodeval = this.getNodeValue(catnode);
				    		
				    		if ("iconurl".equals(catnode.getNodeName()))
				    		{
				    			venue.setIconurl(catnodeval);
				    		}
				    	}
				    }
					// else skip for now.

				} catch (NumberFormatException e)
				{
					// skip for now.
					e.printStackTrace();
				}
			}
			
			return venue;
		} // end if (bottleNode != null)
		else
			return null;
	}
	
	private Venue parseVenueFromNode(CompactVenue venueNode)
	{
		if (venueNode != null)
		{
			Venue venue=new Venue();
			try
			{
				venue.setId(Long.parseLong(venueNode.getId()));
			}
			catch (NumberFormatException e)
			{
				Log.d("LOCATION_PARSE", "Skipped conversion of " + venueNode.getId() + " to Long");
			}
			
			venue.setName(venueNode.getName());
			venue.setAddress(venueNode.getLocation().getAddress());
			venue.setCrossstreet(venueNode.getLocation().getCrossStreet());
			venue.setCity(venueNode.getLocation().getCity());
			venue.setState(venueNode.getLocation().getState());
			venue.setZip(venueNode.getLocation().getPostalCode());
			venue.setGeolat(Double.toString(venueNode.getLocation().getLat()));
			venue.setGeolong(Double.toString(venueNode.getLocation().getLng()));
			venue.setPhone(venueNode.getContact().getPhone());
			venue.setDistance(venueNode.getLocation().getDistance().longValue());
			
			Category[] cats = venueNode.getCategories();
			
			for (int i=0,n=cats.length;i<n;i++)
			{
				Category cat = cats[i];
				
				venue.setIconurl(cat.getIcon());
				break;
			}
				
			return venue;
		} // end if (bottleNode != null)
		else
			return null;
	}

	
	public ArrayList<Venue> searchVenues(String ll) throws FoursquareApiException {
	    // First we need a initialize FoursquareApi. 
	    FoursquareApi foursquareApi = new FoursquareApi(DryncProvider.FOURSQUARE_CLIENT_ID, 
	    		DryncProvider.FOURSQUARE_CLIENT_SECRET, DryncProvider.FOURSQUARE_CALLBACK_URL);
	    ArrayList<Venue> venueLst = new ArrayList<Venue>();
	    // After client has been initialized we can make queries.
	    fi.foyt.foursquare.api.Result<VenuesSearchResult> result = foursquareApi.venuesSearch(ll, null, null, null, null, 30, null, null, null, null, null);
	    
	    if (result.getMeta().getCode() == 200) {
	      // if query was ok we can finally we do something with the data
	      for (CompactVenue venue : result.getResult().getVenues()) {
	        // TODO: Do something we the data
	        System.out.println(venue.getName());
	        Venue venueNode = parseVenueFromNode(venue);
			//Log.d("VENUE_GET", "Read Venue: " + venue.getName());
			venueLst.add(venueNode);
	        
	      }
	      
	      return venueLst;
	    } else {
	      // TODO: Proper error handling
	      System.out.println("Error occured: ");
	      System.out.println("  code: " + result.getMeta().getCode());
	      System.out.println("  type: " + result.getMeta().getErrorType());
	      System.out.println("  detail: " + result.getMeta().getErrorDetail()); 
	    }
	    return venueLst;
	  }
	
}
