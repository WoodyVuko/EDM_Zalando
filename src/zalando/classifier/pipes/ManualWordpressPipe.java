package zalando.classifier.pipes;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.dom.AttrNSImpl;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.XMLOutputter;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import zalando.classifier.Start;
import zalando.classifier.main.SimilarityUtil;

public class ManualWordpressPipe {
	
	private String url;
	private String html;
	private String filename;
	
	public ManualWordpressPipe(String url, String html, String filename) {
		super();
		this.url = url;
		this.html = html;
		this.filename = filename;
		// TODO Auto-generated constructor stub
		System.err.println("Manual Wordpress Pipe active");
	}
	
	public void process()
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
				return;
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
			
			new File("files/tmp/test/manual_wordpress/").mkdirs();
			File file = new File("files/tmp/test/manual_wordpress/" + this.filename + ".json");
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
			try {
				FileUtils.writeStringToFile(file, obj.toJSONString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			doc = this.cleanNode(doc);
			
			StringWriter sw = new StringWriter();
		    Transformer t = TransformerFactory.newInstance().newTransformer();
		    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		    t.transform(new DOMSource(doc), new StreamResult(sw));			
			String xml = sw.toString();
			
			new BPPipe(this.url, xml, "manual_wordpress", filename+"_from_mwp");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(this.url);
			e.printStackTrace();
		}
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
