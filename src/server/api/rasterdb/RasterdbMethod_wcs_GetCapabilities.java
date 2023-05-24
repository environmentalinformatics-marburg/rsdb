package server.api.rasterdb;

import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import rasterdb.RasterDB;
import util.Web;
import util.XmlUtil;

public class RasterdbMethod_wcs_GetCapabilities {
	
	public static void handle_GetCapabilities(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType(Web.MIME_XML);		
		PrintWriter out = response.getWriter();	

		try {
			xml_root__GetCapabilities(rasterdb, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void xml_root__GetCapabilities(RasterDB rasterdb, PrintWriter out) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getCapabilities(rasterdb, doc));
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
	}
	
	private static Node getCapabilities(RasterDB rasterdb, Document doc) {
		Element rootElement = doc.createElementNS(RasterdbMethod_wcs.NS_URL, "WCS_Capabilities");
		rootElement.setAttribute("version", "1.0.0");
		rootElement.setAttribute("xmlns:xlink", RasterdbMethod_wcs.NS_XLINK);

		Element eService = XmlUtil.addElement(rootElement, "Service");
		XmlUtil.addElement(eService, "name", "RSDB WCS");
		XmlUtil.addElement(eService, "label", rasterdb.config.getName());


		Element eContentMetadata = XmlUtil.addElement(rootElement, "ContentMetadata");
		Element eCoverageOfferingBrief = XmlUtil.addElement(eContentMetadata, "CoverageOfferingBrief");
		XmlUtil.addElement(eCoverageOfferingBrief, "name", rasterdb.config.getName());
		XmlUtil.addElement(eCoverageOfferingBrief, "label", rasterdb.config.getName());

		return rootElement;
	}
	
	/*//https://www.wcs.nrw.de/geobasis/wcs_nw_dtk100?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=1.0.0
	static final String cc = "<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>\r\n"
			+ "<WCS_Capabilities\r\n"
			+ "   version=\"1.0.0\" \r\n"
			+ "   updateSequence=\"0\" \r\n"
			+ "   xmlns=\"http://www.opengis.net/wcs\" \r\n"
			+ "   xmlns:xlink=\"http://www.w3.org/1999/xlink\" \r\n"
			+ "   xmlns:gml=\"http://www.opengis.net/gml\" \r\n"
			+ "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
			+ "   xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/wcsCapabilities.xsd\">\r\n"
			+ "<Service>\r\n"
			+ "  <name>MapServer WCS</name>\r\n"
			+ "  <label>WCS NW DTK100</label>\r\n"
			+ "</Service>\r\n"
			+ "<ContentMetadata>\r\n"
			+ "  <CoverageOfferingBrief>\r\n"
			+ "  <metadataLink metadataType=\"TC211\" xlink:type=\"simple\" xlink:href=\"https://apps.geoportal.nrw.de/soapServices/CSWStartup?Service=CSW&amp;Request=GetRecordById&amp;Version=2.0.2&amp;outputSchema=http://www.isotc211.org/2005/gmd&amp;elementSetName=full&amp;id=0bdda2c8-2ac2-46fe-b594-7c82b66d5900\"/>    <name>nw_dtk100_col</name>\r\n"
			+ "    <label>DTK100 FarbeE</label>\r\n"
			+ "    <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\r\n"
			+ "      <gml:pos>5.72499127041 50.150604397469</gml:pos>\r\n"
			+ "      <gml:pos>9.53154249843082 52.6020197509819</gml:pos>\r\n"
			+ "    </lonLatEnvelope>\r\n"
			+ "  </CoverageOfferingBrief>\r\n"
			+ "</ContentMetadata>\r\n"
			+ "</WCS_Capabilities>\r\n"
			+ "";*/
}