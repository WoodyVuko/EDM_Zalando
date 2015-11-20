package zalando.classifier.pipes;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import zalando.classifier.Start;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BPPipe {
	
	private String url;
	private String html;
	private String selector;
	private String filename;
	
	public BPPipe(String...strings) {
		super();
		// TODO Auto-generated constructor stub
		System.err.println("Default Pipe active");
		this.url = strings[0];
		this.html = strings[1];
		this.selector = strings[2];
		this.filename = strings[3];
		this.process();
	}

	private void process() {
		// TODO Auto-generated method stub
			
		try 
		{
			InputSource input = new InputSource(new StringReader(this.html));
			BoilerpipeSAXInput is = new BoilerpipeSAXInput(input);
			TextDocument doc = is.getTextDocument();
			ArticleExtractor ex = new ArticleExtractor();
			JSONObject goldObj = Start.gold.get(this.url);
			if (goldObj == null) 
			{
				return;
			}
			String titleGold = goldObj.get("title").toString();
			String text = goldObj.get("text").toString();
			if (titleGold == null) {
				titleGold = "";
			}
			String titlePipe = doc.getTitle();
			if (titlePipe == null) {
				titlePipe = "";
			}
			
				new File("files/tmp/test/" + this.selector + "/").mkdirs();
				File file = new File("files/tmp/test/" + selector + "/" + this.filename + ".json");
				JSONObject obj = new JSONObject();
				int lev = StringUtils.getLevenshteinDistance(titlePipe, titleGold);
				double jar = StringUtils.getJaroWinklerDistance(ex.getText(doc), goldObj.get("text").toString());
				obj.put("url", this.url);
				obj.put("title", lev);
				obj.put("text", jar);
				try {
					FileUtils.writeStringToFile(file, obj.toJSONString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		} 
		catch (BoilerpipeProcessingException e) 
		{	
			System.out.println("boilerpipe error:" + e.getMessage());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			System.out.println("sax error:" + e.getMessage());
		}
		
	}
}