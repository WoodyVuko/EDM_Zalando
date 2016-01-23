package zalando.classifier.main;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import com.sun.org.apache.xml.internal.security.keys.content.KeyValue;

public class Identificator {

	final ArrayList<Pair<Pattern, Integer>> WordPress_PatternArrayList = new ArrayList<>();
	final ArrayList<Pair<Pattern, Integer>> Blogger_PatternArrayList = new ArrayList<>();

	public Identificator() {
		super();	
		//LIST OF ALL AVAILABLE PATTERN
		Pair<Pattern, Integer> findOneDomain = Pair.of(Pattern.compile("[^(http|https)://][a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*"), 1);
		Pair<Pattern, Integer> wp_main1 = Pair.of(Pattern.compile("wp[-_][a-zA-Z0-9]*"), 1);
		Pair<Pattern, Integer> test = Pair.of(Pattern.compile(".*?\\bhi\\b.*?"), 1);
		Pair<Pattern, Integer> wp_main2 = Pair.of(Pattern.compile("(wp)"), 1);
		Pair<Pattern, Integer> wp_article_div = Pair.of(Pattern.compile("<((article)|(div)).*id=\"post[-_]\\d+.*\">", Pattern.DOTALL), 10);

		Pair<Pattern, Integer> blogger_main1 = Pair.of(Pattern.compile("blogger[a-zA-Z0-9-_.]+"), 1);

		//TODO
		//RegEx f�r Spezialisierung-Wordpress aufbauen. Spezialisierung == CMS
		//Wordpress Pattern Liste mit Inhalt dieser Regex
		//falles eines matched -> manualWP
		//wiederhole f�r alle SPezialisierungen
		//jedes switch-case hat eine eigene patternliste zum abarbeiten

		//WORDPRESS PATTERN
		WordPress_PatternArrayList.add(wp_main1);
		WordPress_PatternArrayList.add(wp_article_div);

		//BLOGGER PATTERN
		Blogger_PatternArrayList.add(blogger_main1);

	}
	public boolean isBlogger;
	public boolean isWP;

	public String evaluate(String...strings){

		isBlogger = false;
		isWP = false;

		int wpCount = 0;
		int bloggrCount = 0;

		String ident = "";
		for (String element : strings) {
			for (Pair<Pattern,Integer> pair : WordPress_PatternArrayList) {
				Matcher match = pair.getLeft().matcher(element);
				while (match.find() && ident.equalsIgnoreCase("")){
					wpCount += 1 * pair.getRight();
				}
			}
		}

		for (String element : strings) {
			for (Pair<Pattern,Integer> pair : Blogger_PatternArrayList) {
				Matcher match = pair.getLeft().matcher(element);
				while (match.find() && ident.equalsIgnoreCase("")){
					bloggrCount += 1 * pair.getRight();
				}
			}
		}


		if (wpCount >= bloggrCount){
			isWP = true;
			ident = "manual_wordpress";
		}

		if (bloggrCount > wpCount){
			isBlogger = true;
			ident = "blogger";
		}

		try {
			RssChecker checker = new RssChecker(new URI(strings[0]), isBlogger);
			boolean feed = checker.rssFeedAvailable();

			if (feed) {
				ident = "rss";
				if (this.isBlogger) {
					ident = "rssBlogger";
				}
			}

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (ident.equalsIgnoreCase("")) {
			ident = "default";
		}

		return ident;
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

			if (child.getNodeName().equalsIgnoreCase("head")) {
				return findRSSNode(child, isBlogger);
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

	public URI getRssURI(URI uri, boolean isBlogger)
	{
		try {
			URL hostUrl = new URL(uri.getScheme() + "://" + uri.getHost());
			DOMParser parser = new DOMParser();
			String html = IOUtils.toString(hostUrl.openStream());
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

			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed2 = input.build(new XmlReader(feedURL.toURL()));
			for (Iterator<SyndEntry> iterator = feed2.getEntries().iterator(); iterator.hasNext();) {
				SyndEntry item = iterator.next();
				if (new URI(item.getLink()).equals(postURL)) {
					String content = item.getDescription().toString();
					if (content.contains(postURL.toString()) ||
							content.contains("&#8230;") ||
							content.contains("&hellip;") ||
							content.equalsIgnoreCase("")) {
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
