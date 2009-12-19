package com.drync.android;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.provider.Settings;
import android.util.Log;

import com.drync.android.objects.Bottle;
import com.drync.android.objects.Review;

public class DryncProvider {
	static String SERVER_HOST="search.drync.com";
	static String TEST_SERVER_HOST="drync-test.morphexchange.com";
	static String USING_SERVER_HOST=TEST_SERVER_HOST;
	static int SERVER_PORT = 80;
	static String URL1 = "/search?query=";
	static String URL2 = "&format=xml&device_id=";

	//static String URL3 = "/search?query=napa+cab&format=xml&device_id=test";
	
	private static final DryncProvider sInstance = new DryncProvider();
	
	public static DryncProvider getInstance() {
		return sInstance;
	}
	
	public List<Bottle> getMatches(String deviceId, String query)
	{
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		return this.searchBottles(target, query, deviceId);
	}
	
	public void startupPost(String deviceId)
	{
		HttpHost target = new HttpHost(USING_SERVER_HOST, SERVER_PORT, "http");
		this.startupPost(target, deviceId);
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

			if(bottles!=null) {
				for(int j=0,m=bottles.getLength();j<m;j++)
				{
					Node bottleNode = bottles.item(j);

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
								if("name".equals(node.getNodeName())) {
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
								}// else skip for now.
								
							} catch (NumberFormatException e)
							{
								// skip for now.
							}
						}
						
						bottleList.add(bottle);
					} // end if (bottleNode != null)
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
	public void startupPost(HttpHost target, String deviceId) {
		// http://{hostname}/app_session?device_id={device_id}&prod={product_selector}
		String urlPost1 = "/app_session";
		Document doc = null;
		HttpClient client = new DefaultHttpClient();

		// set up deviceId
		String devId = deviceId;
		if ((deviceId == null) || (deviceId.equals("")))
			devId = "test";

		StringBuilder bldr = new StringBuilder();
		bldr.append(urlPost1);
		Log.d("DryncPrvdr", "Startup Post: " + bldr.toString());
		HttpPost post = new HttpPost(bldr.toString());
		post.addHeader("X-UDID", "3cd5c62f8289994434a9e0845d5888f37522b1b8-fake");
		post.addHeader("Accept", "text/iphone");
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("device_id", "3cd5c62f8289994434a9e0845d5888f37522b1b8-fake"));
		nvps.add(new BasicNameValuePair("prod", "wine-free"));

		try {
			post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		StringBuilder out = new StringBuilder();
		try {
			HttpResponse response = client.execute(target, post);
			HttpEntity entity = response.getEntity();

			System.out.println("Login form get: " + response.getStatusLine());
			if (entity != null) {
				InputStream is = entity.getContent();

				final char[] buffer = new char[0x10000];
				
				Reader in = new InputStreamReader(is, "UTF-8");

				int read;
				do {
					read = in.read(buffer, 0, buffer.length);
					if (read>0) {
						out.append(buffer, 0, read);
					}
				}
				while (read>=0);

				int i=0;
			}
		}
		catch( Exception e)
		{
			e.printStackTrace();
		}
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


	private String getNodeValue(Node node) {
		NodeList children = node.getChildNodes();
		if(children.getLength()>0) {
			return children.item(0).getNodeValue();
		} else
			return null;
	}


}
