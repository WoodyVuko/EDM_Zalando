package zalando.classifier.main;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.dom.AttrNSImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

public class RssChecker {
	
	private URI postUri;
	private URI feedUri;
	private boolean isBlogger;
	private boolean feedAvailable;
	private boolean checkedForAvailable;
	private SyndEntry feedContent;
	
	public RssChecker(URI postUri, boolean isBlogger) {
		super();
		this.postUri = postUri;
		this.feedUri = null;
		this.isBlogger = isBlogger;
		this.checkedForAvailable = false;
		this.feedAvailable = false;
		this.feedContent = null;
	}
	
	public boolean rssFeedAvailable()
	{
		if (!this.checkedForAvailable) {
			this.findFeedUri();
			if (this.feedUri == null) {
				this.feedAvailable = false;
			}
			
			try {
				
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(new XmlReader(this.feedUri.toURL()));
				for (Iterator<SyndEntry> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
					SyndEntry item = iterator.next();
					if (new URI(item.getLink()).equals(this.postUri)) {
						String content = null;
						if (item.getContents().size() > 0) {
							content = item.getContents().get(0).getValue();
						} else if (item.getDescription() != null) {
							content = item.getDescription().getValue();
						}
						else
						{
							this.feedAvailable = false;
							break;
						}
//						if (content.contains(this.postUri.toString()) ||
//							content.contains("&#8230;") ||
//							content.contains("&hellip;") ||
//							content.equalsIgnoreCase("")) {
//							this.feedAvailable = false;
//							break;
//						}
						this.feedAvailable = true;
						this.feedContent = item;
						break;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				this.feedAvailable = false;
			}	
			this.checkedForAvailable = true;
		}
		
		return this.feedAvailable;
	}
	
	public SyndEntry getContent()
	{
		if (!this.checkedForAvailable) {
			this.rssFeedAvailable();
		}
		
		return this.feedContent;
	}
	
	private void findFeedUri()
	{
		if (!this.checkedForAvailable) {
			URI uri = null;
			
			try {
				URI hostUrl;
				if (this.postUri.getScheme() == null) {
					hostUrl = new URI("http://" + this.postUri.getHost());
				}
				else {
					hostUrl = new URI(this.postUri.getScheme() + "://" + this.postUri.getHost());
				}
				DOMParser parser = new DOMParser();
				HttpURLConnection httpcon = (HttpURLConnection) hostUrl.toURL().openConnection();
			    httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
				String html = IOUtils.toString(httpcon.getInputStream());
				InputSource is = new InputSource(new StringReader(html));
				parser.parse(is);
				Node doc = parser.getDocument();
				Node noi = findRSSNode(doc, isBlogger);
				if (noi != null) {
					NamedNodeMap atts = noi.getAttributes();
					if (atts != null) {
						URI feedUri = new URI(atts.getNamedItem("href").getNodeValue().toString());
						if (feedUri.getScheme() == null) {
							feedUri = new URI("http", feedUri.getHost(), feedUri.getPath(), feedUri.getFragment());
						}
						uri = feedUri;
					}
				}
			} catch (SAXException | IOException | DOMException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.feedUri = uri;
		}
	}
	
	private Node findRSSNode(Node node, boolean isBlogger)
	{
		Node child = node.getFirstChild();
		String relValue = "alternate";
		String typeValue = "application/rss+xml";
		if (isBlogger) {
			relValue = "service.post";
			typeValue = "application/atom+xml";
		}
        while (child != null) {
        	
        	if(child.getNodeName().equalsIgnoreCase("link")) {
        		NamedNodeMap atts = child.getAttributes();
        		if (atts != null) {
        			boolean isAlternate = false;
        			boolean isRSS = false;
        			boolean isWrong = false;
        			String rssLink = null;
        			
        			for (int i = 0; i < atts.getLength(); i++) {
        				Object att = atts.item(i);
        				if (att instanceof AttrNSImpl) {
        					AttrNSImpl realAtt = (AttrNSImpl)att;
        					if (realAtt.getName().equalsIgnoreCase("rel") && 
        						realAtt.getValue().equalsIgnoreCase(relValue)) {
								isAlternate = true;
								continue;
							}
        					if (realAtt.getName().equalsIgnoreCase("type") && 
            					realAtt.getValue().equalsIgnoreCase(typeValue)) {
    								isRSS = true;
    								continue;
    						}
        					if (realAtt.getName().equalsIgnoreCase("title") && 
                				realAtt.getValue().toLowerCase().contains("comment")) {
        							isWrong = true;
        							continue;
        					}
//        					if (realAtt.getName().equalsIgnoreCase("href") && 
//                    			realAtt.getValue().toLowerCase().contains("atom")) {
//            						isWrong = true;
//            						continue;
//            					}
        				}
        			}
        			if (isRSS && isAlternate && !isWrong) {
						return child;
					}
				}
        	}
           	Node noi = null;
        	noi = findRSSNode(child, isBlogger);
        	if (noi != null) {
				return noi;
			}
            child = child.getNextSibling();
        }
		return null;
	}
	
}
