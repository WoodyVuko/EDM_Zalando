package zalando.classifier.pipes;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
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
	private ArrayList<String> unwantedTags;
	private ArrayList<String> unwantedAtts;
	private ArrayList<Pattern> unwantedCss;
	
	public ManualWordpressPipe(String url, String html) {
		super();
		this.url = url;
		this.html = html;
		// TODO Auto-generated constructor stub
		System.err.println("Manual Wordpress Pipe active");
		unwantedTags = new ArrayList<>();
		unwantedTags.add("script");
		unwantedTags.add("#comment");
		unwantedTags.add("style");
		
		unwantedAtts = new ArrayList<>();
		unwantedAtts.add("onclick");
		unwantedAtts.add("href");
		
		unwantedCss = new ArrayList<>();
		unwantedCss.add(Pattern.compile(".*comment.*"));
	}
	
	public JSONObject process()
	{	
		DOMParser parser = new DOMParser();
		InputSource is = new InputSource(new StringReader(this.html));
		try {
			parser.parse(is);
			Node doc = parser.getDocument();
			doc = this.getNodesOfInterest(doc);
			System.out.println(this.url);
			if (this.url.equalsIgnoreCase("http://www.manrepeller.com/2012/10/bagging.html")) {
				int i = 1;
			}
			removeTags(doc);
			if (this.url.equalsIgnoreCase("http://www.manrepeller.com/2012/10/bagging.html")) {
				print(doc, " ");
			}
//			print(doc, "  ");
			System.out.println("----");
			
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
			String docText = doc.getTextContent().replaceAll("\\s+", " ").replaceAll("[^\\x00-\\x7F]", "");
			docText = StringEscapeUtils.unescapeJava(docText);
			double cosine = SimilarityUtil.consineTextSimilarity(StringUtils.split(docText), StringUtils.split(goldObj.get("text").toString()));
			
			JSONObject pipeObj = new JSONObject();
			pipeObj.put("title", titlePipe);
			pipeObj.put("text", docText);
						
			JSONObject simObj = new JSONObject();
			simObj.put("title", lev);
			simObj.put("text", cosine);
			
			obj.put("source", this.url);
			obj.put("pipe", pipeObj);
			obj.put("gold", goldObj);
			obj.put("similarity", simObj);
			
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
			Node noi = null;
			for (int i = 0; i < atts.getLength(); i++) {
				Object att = atts.item(i);
				if (att instanceof AttrNSImpl) {
					AttrNSImpl realAtt = (AttrNSImpl)att;
					if (realAtt.getName().equalsIgnoreCase("id") ||
						realAtt.getName().equalsIgnoreCase("class")) {
						Pattern p = Pattern.compile("(post|article)[-_]\\d+", Pattern.CASE_INSENSITIVE);
						Pattern p2 = Pattern.compile("(post|entry|main)[-_]?(content|body)", Pattern.CASE_INSENSITIVE);
						if (p.matcher(realAtt.getValue()).matches()) {
							noi = node;
							break;
						}
						if (p2.matcher(realAtt.getValue()).find()) {
							return node;
						}
					}
				}
			}
			if (noi != null) {
				Node childNoi = noi.getFirstChild();
				while (childNoi != null) {
					Node foundNoiInChild = this.getNodesOfInterest(childNoi);
					if (foundNoiInChild != null) {
						return foundNoiInChild;
					}
					childNoi = childNoi.getNextSibling();
				}
				return noi;
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
	
	private void removeTags(Node node)
	{
		boolean deleteNode = false;
		NamedNodeMap atts = node.getAttributes();
		if (atts != null) {
			for (int i = 0; i < atts.getLength(); i++) {
				Object att = atts.item(i);
				if (att instanceof AttrNSImpl) {
					AttrNSImpl realAtt = (AttrNSImpl)att;
					if (unwantedAtts.contains(realAtt.getName())) {
						atts.removeNamedItem(realAtt.getName());
					}
					for (Pattern pattern : unwantedCss) {
						if (pattern.matcher(realAtt.getNodeValue()).find()) {
							removeSiblings(node);
							return;
						}
					}
				}
			}
		}
		if (this.unwantedTags.contains(node.getNodeName().toLowerCase())) {
			node.setTextContent("");
		}
		if (deleteNode) {
			node = null;
			return;
		}
		Node child = node.getFirstChild();
        while (child != null) {
        	removeTags(child);
            child = child.getNextSibling();
        }
	}
	
	private void removeSiblings(Node node)
	{
		Node sib = node.getNextSibling();
		if (sib != null) {
			removeSiblings(sib);
		}
		node.getParentNode().removeChild(node);
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
	
	public static void print(Node node, String indent) {
        System.out.println(indent+node.getClass().getName()+"-"+node.getNodeName());
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent+" ");
            child = child.getNextSibling();
        }
    }
}
