package zalando.classifier.pipes;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.json.simple.JSONObject;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.rometools.rome.feed.synd.SyndEntry;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import zalando.classifier.Start;
import zalando.classifier.main.RssChecker;
import zalando.classifier.main.SimilarityUtil;

public class RssPipe {
	
	private String url;
	private boolean isBlogger;
	
	public RssPipe(String url, boolean isBlogger) {
		super();
		this.url = url;
		this.isBlogger = isBlogger;
	}
	
	public JSONObject process()
	{	
		try {
			RssChecker checker = new RssChecker(new URI(this.url), this.isBlogger);
			if (this.url.contains("bryanboy")) {
				System.out.println();
			}
			SyndEntry content = checker.getContent();
			
			DOMParser parser = new DOMParser();
			String str = null;
			if (content.getContents().size() > 0) {
				str = content.getContents().get(0).getValue();
			} else if (content.getDescription() != null) {
				str = content.getDescription().getValue();
			}
			InputSource is = new InputSource(new StringReader(str));
			parser.parse(is);
			Node doc = parser.getDocument();
			String docText = doc.getFirstChild().getTextContent();
			
			JSONObject goldObj = Start.gold.get(this.url);
			if (goldObj == null) 
			{
				return null;
			}
			String titleGold = goldObj.get("title").toString();
			String text = goldObj.get("text").toString();
			if (titleGold == null) {
				titleGold = "";
			}
			String titlePipe = content.getTitle();
			if (titlePipe == null) {
				titlePipe = "";
			}
			JSONObject obj = new JSONObject();
			NormalizedLevenshtein nls = new NormalizedLevenshtein();
			double lev = nls.distance(StringUtils.deleteWhitespace(titlePipe), StringUtils.deleteWhitespace(titleGold));
			String levFine = String.format("%.2f", lev);
			if(doc != null){
				docText = StringEscapeUtils.unescapeJava(docText);
				double cosine = SimilarityUtil.consineTextSimilarity(StringUtils.split(docText), StringUtils.split(goldObj.get("text").toString()));
				String cosineFine = String.format("%.2f", cosine);
				//COMPARING END
				
				//toDO Collect more Meta Informations
				//like author, url, domain, date, img-alt-tag think about more
				//
				JSONObject pipeObj = new JSONObject();
				pipeObj.put("title", titlePipe);
				pipeObj.put("text", docText);
							
				JSONObject simObj = new JSONObject();
				simObj.put("title", levFine);
				simObj.put("text", cosineFine);
				
				obj.put("source", this.url);
				obj.put("pipe", pipeObj);
				obj.put("gold", goldObj);
				obj.put("similarity", simObj);
			}
			
			return obj;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("failed for: " + this.url);
		}
		return null;
	}
}
