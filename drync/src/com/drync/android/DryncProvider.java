package com.drync.android;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.provider.Settings;
import android.util.Log;

import com.drync.android.objects.Bottle;

public class DryncProvider {
	static String SERVER_HOST="search.drync.com";
	static String TEST_SERVER_HOST="drync-test.morphexchange.com";
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
		HttpHost target = new HttpHost(TEST_SERVER_HOST, SERVER_PORT, "http");
		return this.searchBottles(target, query, deviceId);
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
							}
							// else skip for now.
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
	
	private String getNodeValue(Node node) {
		NodeList children = node.getChildNodes();
		if(children.getLength()>0) {
			return children.item(0).getNodeValue();
		} else
			return null;
	}


}
