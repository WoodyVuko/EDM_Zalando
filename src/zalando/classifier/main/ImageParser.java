package zalando.classifier.main;

import java.io.StringReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.dom.AttrNSImpl;
import org.apache.xerces.dom.TextImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class ImageParser {
	
	private Node node;
	private String html;
	private String startOfDoc;
	private JSONArray imagesArray;
	private boolean processed, textProcessed;
	
	public ImageParser(Node node, String html, String startOfDoc)
	{
		super();
		this.node = node;
		this.html = html;
		this.startOfDoc = startOfDoc;
		this.imagesArray = new JSONArray();
		this.processed = false;
		this.textProcessed = false;
	}
	
	private void processText()
	{
		DOMParser parser = new DOMParser();
		InputSource is = new InputSource(new StringReader(this.html));
		try {
			parser.parse(is);
			Node doc = parser.getDocument();
			doc = findNode(doc);
			process(doc);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Node findNode(Node node)
	{
		if (node.getNodeValue() != null) {
			if (node.getNodeValue().toLowerCase().contains(startOfDoc.toLowerCase())) {
				if (node instanceof TextImpl) 
					return node.getParentNode().getParentNode();
				else
					return node.getParentNode();
			}
		}
		Node child = node.getFirstChild();
		
        while (child != null) {
            Node noi = findNode(child);
            if (noi != null) {
				return noi;
			}
            child = child.getNextSibling();
        }
		return null;
	}
	
	private void process(Node node)
	{
		if (node == null) {
			return;
		}
		if (node.getNodeName() != null && node.getNodeName().equalsIgnoreCase("img")) {
			NamedNodeMap atts = node.getAttributes();
			for (int i = 0; i < atts.getLength(); i++) {
				Object att = atts.item(i);
				if (att instanceof AttrNSImpl) 
				{
					AttrNSImpl realAtt = (AttrNSImpl)att;
					if (realAtt.getName().equalsIgnoreCase("alt") && realAtt.getValue() != "") 
					{
						imagesArray.add(realAtt.getValue());
					}
				}
			}
		}
		processed = true;
        Node child = node.getFirstChild();
        while (child != null) {
            process(child);
            child = child.getNextSibling();
        }
	}
	
	public JSONArray result()
	{
		if (!processed) {
			this.process(this.node);
		}
		if (imagesArray.size() == 0) {
			return null;
		}
		System.out.println(imagesArray.toString());
		return imagesArray;
	}
	
	public JSONArray resultFromTextDoc()
	{
		if (!textProcessed) {
			this.processText();
		}
		if (imagesArray.size() == 0) {
			return null;
		}
		System.out.println(imagesArray.toString());
		return imagesArray;
	}
}
