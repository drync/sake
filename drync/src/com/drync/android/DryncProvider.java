package com.drync.android;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

import com.drync.android.objects.Bottle;
import com.drync.android.objects.Cork;
import com.drync.android.objects.Review;
import com.drync.android.objects.Source;

public class DryncProvider {
	static String SERVER_HOST="search.drync.com";
	static String TEST_SERVER_HOST="drync-test.morphexchange.com";
	static String DEV_SERVER_HOST="10.0.2.2";
	static String USING_SERVER_HOST=TEST_SERVER_HOST;
	static int SERVER_PORT = USING_SERVER_HOST == DEV_SERVER_HOST ? 3000 : 80;
	static String URL1 = "/search?query=";
	static String URL2 = "&format=xml&device_id=";	
	
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
	
	public String startupPost(String deviceId)
	{
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return this.startupPost(target, deviceId);
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
		bldr.append(URL1).append(keywords.replaceAll(" ", "+")).append(URL2).append(devId);
		Log.d("DryncPrvdr", "Loading Wines: " + bldr.toString());
		HttpGet get = new HttpGet(bldr.toString());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
	
	public List<Cork> getCorks(Context context, String deviceId)
	{
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return getCorks(context, target, deviceId);
	}
	
	private HttpResponse doCorksGet(Context context, HttpHost target, String deviceId) throws ClientProtocolException, IOException
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

		HttpGet get = new HttpGet(bldr.toString());
		
		HttpResponse response = client.execute(target, get);
		
		return response;
	}

	public boolean getCorksToFile(Context context, String deviceId) {
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return getCorksToFile(context, target, deviceId);
	}
	
	private boolean getCorksToFile(Context context, HttpHost target, String deviceId) {
		
		boolean wroteContent = false;
		InputStream is = null;

		try {
			HttpResponse response = doCorksGet(context, target, deviceId);
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
	}
	/**
	 * Call the REST service to retrieve the first matching promotion based
	 * on the give keywords. If none found, return null.
	 * @param target - the target HTTP host for the REST service.
	 * @param keywords - comma delimited keywords. May contain spaces.
	 * @return - PromoInfo that matches the keywords. If error or no match, return null.
	 */
	private List<Cork> getCorks(Context context, HttpHost target, String deviceId) {
		ArrayList<Cork> bottleList = new ArrayList<Cork>();
		DryncDbAdapter dbAdapter = new DryncDbAdapter(context);
		dbAdapter.open();
		Document doc = null;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			HttpResponse response = doCorksGet(context, target, deviceId);
			
			if (response.getStatusLine().getStatusCode() < 400)
			{
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
							dbAdapter.insertOrUpdateCork((Cork)bottle);
							bottleList.add(bottle);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		
		dbAdapter.close();
		
		return bottleList;
	}
	
	public List<Cork> getCorksFromFile() {
		ArrayList<Cork> bottleList = new ArrayList<Cork>();
		Document doc = null;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
	}
	
	
	
	/**
	 * Call the REST service to retrieve the first matching promotion based
	 * on the give keywords. If none found, return null.
	 * @param target - the target HTTP host for the REST service.
	 * @param keywords - comma delimited keywords. May contain spaces.
	 * @return - PromoInfo that matches the keywords. If error or no match, return null.
	 */
	public String startupPost(HttpHost target, String deviceId) {
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
		nvps.add(new BasicNameValuePair("prod", "wine-free"));

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
					if (DryncUtils.isDebugMode)
					{
						String filename = DryncUtils.getCacheDir() + "register.html";
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
					} 
					// else skip for now.

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
	
	private void parseBottlePortions(Node node, String value, Bottle bottle)
	{
		if("name".equals(node.getNodeName())) {
			bottle.setName(value);
		} else if("year".equals(node.getNodeName())) {
			bottle.setYear(Integer.parseInt(value));
		} else if("bottle_id".equals(node.getNodeName())) {
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
		    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		}
		HttpResponse response;
		response = httpclient.execute(httppost);
		return response;
	} 
	
	public static boolean postCreate(Cork cork, String deviceId)
	{
		// Define our Restlet client resources.  
		String clientResourceUrl = String.format("http://%s:%d/corks", USING_SERVER_HOST,SERVER_PORT);
		
		try {
			HttpResponse response = DryncProvider.doPost(clientResourceUrl, cork.getRepresentation(deviceId), deviceId);
			String content = DryncProvider.convertStreamToString(response.getEntity().getContent());
			Log.d("DryncProvider", response.getStatusLine().toString() + "\n" + content);
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
		}
		
		return false;
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
}
