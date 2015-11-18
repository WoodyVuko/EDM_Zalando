package zalando.classifier.pipes;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.json.simple.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BPPipe {
	
	public BPPipe(String...strings) {
		super();
		// TODO Auto-generated constructor stub
		System.err.println("Default Pipe active");
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String everything = null;
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("files/final.json")), "UTF8"))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) 
		    {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    everything = sb.toString();
		}
		catch (Exception e) {
			System.out.print("error: " + e.getMessage());
		}
		
		if (everything != null) 
		{
			JSONArray array = (JSONArray)JSONValue.parse(everything);
			
			for (Object object : array) 
			{
				JSONObject obj = (JSONObject)object;
				String rawHtml = BPPipe.getCrawledHtml(obj.get("source").toString());
				try 
				{
					InputSource input = new InputSource(new StringReader(rawHtml));
					BoilerpipeSAXInput is = new BoilerpipeSAXInput(input);
					TextDocument doc = is.getTextDocument();
					ArticleExtractor ex = new ArticleExtractor();
					JSONObject pipeObj = (JSONObject)obj.get("pipe");
					pipeObj.put("text", ex.getText(doc));
					pipeObj.put("title", doc.getTitle());
				} 
				catch (BoilerpipeProcessingException e) 
				{	
					System.out.println("boilerpipe error:" + e.getMessage());
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					System.out.println("sax error:" + e.getMessage());
				}
			}
			StringWriter out = new StringWriter();
			try 
			{
				array.writeJSONString(out);
				FileUtils.writeStringToFile(new File("files/final_updated.json"), out.toString());
				System.out.println("wrote new json to files/final_updated.json");
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				System.out.println("writing error:" + e.getMessage());
			}
		}
		
	}

	private static String getCrawledHtml(String url)
	{
		try(BufferedReader br = new BufferedReader(new FileReader("files/raw_html_shrink.json"))) {
		    String line = br.readLine();
		    System.out.println("processing " + url);
		    while (line != null) {
		    	JSONObject obj = (JSONObject)JSONValue.parse(line);
		    	String urlFromRaw = obj.get("url").toString();
		    	
		    	if (url.equals(urlFromRaw)) 
		    	{
					return obj.get("html").toString();
				}
		        line = br.readLine();
		    }
		}
		catch (Exception e) {
			System.out.print("error: " + e.getMessage());
		}
		return "";
	}
}