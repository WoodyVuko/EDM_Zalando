package zalando.classifier.pipes;

import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.dom.AttrNSImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.json.simple.JSONObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import zalando.classifier.Start;
import zalando.classifier.main.SimilarityUtil;

public class ManualWordpressPipe {
	
	private String url;
	private String html;
	
	public ManualWordpressPipe(String url, String html) {
		super();
		this.url = url;
		this.html = html;
		// TODO Auto-generated constructor stub
		System.err.println("Manual Wordpress Pipe active");
	}
	
	public JSONObject process()
	{	
		DOMParser parser = new DOMParser();
		InputSource is = new InputSource(new StringReader(this.html));
		try {
			parser.parse(is);
			Node doc = parser.getDocument();
			doc = this.getNodesOfInterest(doc);
			
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
			String titlePipe = this.getTitleFromUrl();
			if (titlePipe == null) {
				titlePipe = "";
			}
			
			JSONObject obj = new JSONObject();
			NormalizedLevenshtein nls = new NormalizedLevenshtein();
			double lev = nls.distance(StringUtils.deleteWhitespace(titlePipe), StringUtils.deleteWhitespace(titleGold));
			String docText = doc.getTextContent();
			double jar = StringUtils.getJaroWinklerDistance(StringUtils.deleteWhitespace(docText), StringUtils.deleteWhitespace(goldObj.get("text").toString()));
			double cosine = SimilarityUtil.consineTextSimilarity(StringUtils.split(docText), StringUtils.split(goldObj.get("text").toString()));
			obj.put("url", this.url);
			obj.put("text", docText);
			obj.put("title", titlePipe);
			obj.put("title_lev", lev);
			obj.put("text_jar", jar);
			obj.put("text_cosine", cosine);
			
			return obj;
//			try {
//				FileUtils.writeStringToFile(file, obj.toJSONString());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			doc = this.cleanNode(doc);
//			
//			StringWriter sw = new StringWriter();
//		    Transformer t = TransformerFactory.newInstance().newTransformer();
//		    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//		    t.transform(new DOMSource(doc), new StreamResult(sw));			
//			String xml = sw.toString();
//			
//			new BPPipe(this.url, xml, "manual_wordpress", filename+"_from_mwp");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(this.url);
			e.printStackTrace();
		}
		
		return null;
	}
	
	private Node getNodesOfInterest(Node node)
	{
		if (node.getNodeName().equalsIgnoreCase("div") || node.getNodeName().equalsIgnoreCase("article")) {
			NamedNodeMap atts = node.getAttributes();
			for (int i = 0; i < atts.getLength(); i++) {
				Object att = atts.item(i);
				if (att instanceof AttrNSImpl) {
					AttrNSImpl realAtt = (AttrNSImpl)att;
					if (realAtt.getName().equalsIgnoreCase("id")) {
						Pattern p = Pattern.compile("post[-_]\\d+");
						if (p.matcher(realAtt.getValue()).matches()) {
							return node;
						}
					}
				}
			}
		}
		Node noi = null;
		Node child = node.getFirstChild();
		while (child != null) {
			noi = this.getNodesOfInterest(child);
			if (noi != null) {
				return noi;
			}
			child = child.getNextSibling();
		}
		return null;
	}
	
	private String getTitleFromUrl()
	{
		String[] parts = this.url.split("/");
		String lastPart = parts[parts.length-1];
		if (lastPart.equalsIgnoreCase("")) {
			lastPart = parts[parts.length-2];
		}
		lastPart = lastPart.replace(".html", "");
		lastPart = lastPart.replace("-", " ");
		lastPart = lastPart.replace("_", " ");
		
		return lastPart.trim();
	}
	
	private Node cleanNode(Node node)
	{
		NamedNodeMap atts = node.getAttributes();
		if (atts != null) {
			for (int i = 0; i < atts.getLength(); i++) {
				Object att = atts.item(i);
				if (att instanceof AttrNSImpl) {
					AttrNSImpl realAtt = (AttrNSImpl)att;
					if (realAtt.getName().contains(":")) {
						atts.removeNamedItem(realAtt.getName());
					}
				}
			}
		}
        Node child = node.getFirstChild();
        while (child != null) {
        	cleanNode(child);
            child = child.getNextSibling();
        }
		return node;
	}
}
