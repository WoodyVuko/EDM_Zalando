package zalando.classifier.main;

import java.awt.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Identificator {
	
	final ArrayList<Pattern> patternList = new ArrayList<>();



	
	public Identificator() {
		super();
		// TODO Auto-generated constructor stub
		Pattern p1 = Pattern.compile("[^(http|https)://][a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*");
		Pattern p2 = Pattern.compile("wp[-_][a-zA-Z0-9]*");
		Pattern p3 = Pattern.compile(".*?\\bhi\\b.*?");
		Pattern p4 = Pattern.compile("(wp)*");
		
		patternList.add(p1);patternList.add(p2);patternList.add(p3);patternList.add(p4);
	}
	public boolean isWordpress = false;
	
	public String evaluate(String...strings){
		for (String element : strings) {
			for (Pattern pat : patternList) 
				if (pat.matcher(element).matches()){
					isWordpress = true;
					return "wordpress";
				}
		}
		
		
		return "default";
	}
}
