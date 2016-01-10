package zalando.classifier.main;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

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
//		for (String element : strings) {
//			if (manualWP.matcher(element).find()) {
//				return "manual_wordpress";
//			}
//		}
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
	
	public boolean checkForRssAvailability(URL url) {
		
		try {
		SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url)); 
		}
		catch (Exception ex)
		{
			
		}
		return false;
	}
}
