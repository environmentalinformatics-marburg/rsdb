package util;

import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmlUtil {
	
	@FunctionalInterface
	public static interface NodeCreator {
		Node create(Document doc) throws Exception;
	}
	
	public static final DocumentBuilder DOCUMENT_BUILDER;
	static {
		try {
			DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeXML(PrintWriter out, NodeCreator creator) throws Exception {
		Document doc = DOCUMENT_BUILDER.newDocument();
		Node root = creator.create(doc);
		doc.appendChild(root);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);

	}
	
	public static Element addElement(Element root, String name) {
		Element element = root.getOwnerDocument().createElement(name);
		root.appendChild(element);
		return element;
	}

	public static Element addElement(Element root, String name, String textContent) {
		Element e = addElement(root, name);
		e.setTextContent(textContent);
		return e;
	}
	
	public static String encodeXML(String s) { // derived from: https://stackoverflow.com/questions/439298/best-way-to-encode-text-data-for-xml-in-java
	    StringBuilder sb = new StringBuilder();
	    int len = s.length();
	    for (int i=0;i<len;) {
	        int c = s.codePointAt(i);
	        if (c < 0x80) {      // ASCII range: test most common case first
	            if (c < 0x20 && (c != '\t' && c != '\r' && c != '\n')) {
	                // Illegal XML character, even encoded. Skip or substitute
	                sb.append("&#xfffd;");   // Unicode replacement character
	            } else {
	                switch(c) {
	                  case '&':  sb.append("&amp;"); break;
	                  case '>':  sb.append("&gt;"); break;
	                  case '<':  sb.append("&lt;"); break;
	                  // Uncomment next two if encoding for an XML attribute
	                  //case '\''  sb.append("&apos;"); break;
	                  //case '\"'  sb.append("&quot;"); break;
	                  // Uncomment next three if you prefer, but not required
	                  //case '\n'  sb.append("&#10;"); break;
	                  //case '\r'  sb.append("&#13;"); break;
	                  //case '\t'  sb.append("&#9;"); break;
	                  default:   sb.append((char)c);
	                }
	            }
	        } else if ((c >= 0xd800 && c <= 0xdfff) || c == 0xfffe || c == 0xffff) {
	            // Illegal XML character, even encoded. Skip or substitute
	            sb.append("&#xfffd;");   // Unicode replacement character
	        } else {
	            sb.append("&#x");
	            sb.append(Integer.toHexString(c));
	            sb.append(';');
	        }
	        i += c <= 0xffff ? 1 : 2;
	    }
	    return sb.toString();
	}
}
