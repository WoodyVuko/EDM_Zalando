package zalando.classifier.main;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.dom.AttrNSImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;

import it.sauronsoftware.feed4j.FeedIOException;
import it.sauronsoftware.feed4j.FeedParser;
import it.sauronsoftware.feed4j.FeedXMLParseException;
import it.sauronsoftware.feed4j.UnsupportedFeedException;
import it.sauronsoftware.feed4j.bean.Feed;
import it.sauronsoftware.feed4j.bean.FeedItem;

public class Identificator {
	
	final ArrayList<Pattern> WordPress_PatternArrayList = new ArrayList<>();
	final ArrayList<Pattern> Blogger_PatternArrayList = new ArrayList<>();
	final ArrayList<Pattern> Tumbl_PatternArrayList = new ArrayList<>();
	
	public Identificator() {
		super();	
		//LIST OF ALL AVAILABLE PATTERN
		Pattern findOneDomain = Pattern.compile("[^(http|https)://][a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*");
		Pattern wp_main1 = Pattern.compile("wp[-_][a-zA-Z0-9]*");
		Pattern test = Pattern.compile(".*?\\bhi\\b.*?");
		Pattern wp_main2 = Pattern.compile("(wp)*");
		Pattern wp_article_div = Pattern.compile("<((article)|(div)).*id=\"post[-_]\\d+.*\">", Pattern.DOTALL);
		
		Pattern blogger_main1 = Pattern.compile("blogg[a-zA-Z0-9]*");
		
		Pattern tumblr_main1 = Pattern.compile("tumbl[a-zA-Z0-9]*");
		//TODO
		//RegEx für Spezialisierung-Wordpress aufbauen. Spezialisierung == CMS
		//Wordpress Pattern Liste mit Inhalt dieser Regex
		//falles eines matched -> manualWP
		//wiederhole für alle SPezialisierungen
		//jedes switch-case hat eine eigene patternliste zum abarbeiten
		
		//WORDPRESS PATTERN
		WordPress_PatternArrayList.add(wp_main1);
		WordPress_PatternArrayList.add(wp_article_div);
		
		//BLOGGER PATTERN
		Blogger_PatternArrayList.add(blogger_main1);
		
		//TUMBLER PATTERN
		Tumbl_PatternArrayList.add(tumblr_main1);
		
	}
	public boolean isWordpress = false;
	 
	public String evaluate(String...strings){
		
		try {
			URI feedURI = this.getRssURI(new URI(strings[0]));
			boolean feed = this.checkForRssAvailability(feedURI, new URI(strings[0]));
			
			System.out.println("did found feed? " + feed + " for url: " + strings[0]);
			return "rss";
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (String element : strings) {
			for (Pattern pat : WordPress_PatternArrayList) {
				if (pat.matcher(element).find()){
					isWordpress = true;
					return "manual_wordpress";
				}
			}
		}
		int bloggrCount = 0;
		for (String element : strings) {
			for (Pattern pat : Blogger_PatternArrayList) {
				
				while (pat.matcher(element).find()){
					bloggrCount++;
					if (bloggrCount >= 10){
						return "blogger";
				}
					

				}
			}
		}
		int tumblrCount = 0;
		for (String element : strings) {
			for (Pattern pat : Tumbl_PatternArrayList) {
				
				while (pat.matcher(element).find()){
					tumblrCount++;
					if (tumblrCount >= 10){
						return "tumblr";
				}
					

				}
			}
		}
		return "default";
	}
	
	private Node findRSSNode(Node node)
	{
		Node child = node.getFirstChild();
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
        						realAtt.getValue().equalsIgnoreCase("alternate")) {
								isAlternate = true;
								continue;
							}
        					if (realAtt.getName().equalsIgnoreCase("type") && 
            					realAtt.getValue().equalsIgnoreCase("application/rss+xml")) {
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
        	
        	if (child.getNodeName().equalsIgnoreCase("head")) {
        		return findRSSNode(child);
        	}
        	Node noi = null;
        	noi = findRSSNode(child);
        	if (noi != null) {
				return noi;
			}
            child = child.getNextSibling();
        }
		return null;
	}
	
	public URI getRssURI(URI uri)
	{
		try {
			URL hostUrl = new URL(uri.getScheme() + "://" + uri.getHost());
			DOMParser parser = new DOMParser();
			String html = IOUtils.toString(hostUrl.openStream());
			InputSource is = new InputSource(new StringReader(html));
			parser.parse(is);
			Node doc = parser.getDocument();
			Node noi = findRSSNode(doc);
			if (noi != null) {
				NamedNodeMap atts = noi.getAttributes();
				if (atts != null) {
					URI feedUri = new URI(atts.getNamedItem("href").getNodeValue().toString());
					if (feedUri.getScheme() == null) {
						feedUri = new URI("http", feedUri.getHost(), feedUri.getPath(), feedUri.getFragment());
					}
					return feedUri;
				}
			}
		} catch (SAXException | IOException | DOMException | URISyntaxException e) {
			// TODO Auto-generated catch block
			return null;
		}
		return null;
	}
	
	public boolean checkForRssAvailability(URI feedURL, URI postURL) {
		
		if (feedURL == null) {
			return false;
		}
		
		try {
			Feed feed = FeedParser.parse(feedURL.toURL());
			for (int i = 0; i < feed.getItemCount(); i++) {
				FeedItem item = feed.getItem(i);
				if (item.getLink().toURI().equals(postURL)) {
					if (item.getDescriptionAsHTML().contains(postURL.toString()) ||
						item.getDescriptionAsHTML().contains("&#8230;") ||
						item.getDescriptionAsHTML().contains("&hellip;")) {
						return false;
					}
					if (item.getDescriptionAsText().equalsIgnoreCase("")) {
						return false;
					}
					return true;
				}
			}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return false;
		}		
	}
}
