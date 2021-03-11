package server.api.vectordbs;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.ogr.DataSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.Broker;
import util.Extent2d;
import util.Web;
import util.image.ImageBufferARGB;
import vectordb.Renderer;
import vectordb.VectorDB;

public class VectordbHandler_wms extends VectordbHandler {
	private static final Logger log = LogManager.getLogger();

	public VectordbHandler_wms(Broker broker) {
		super(broker, "wms");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			log.error("no WMS");
			return;
		}*/		

		String reqParam = Web.getLastString(request, "REQUEST", null);
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "Request", null);
		}
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "request", null);
		}
		if(reqParam==null) {
			reqParam = "GetCapabilities";
		}

		switch (reqParam) {
		case "GetMap":
			handle_GetMap(vectordb, request, response, userIdentity);
			break;
		case "GetCapabilities":
			handle_GetCapabilities(vectordb, request, response, userIdentity);
			break;
		default:
			log.error("unknown request " + reqParam);
			return;
		}		
	}

	public void handle_GetMap(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		int width = Web.getInt(request, "WIDTH");
		int height = Web.getInt(request, "HEIGHT");
		String[] bbox = request.getParameter("BBOX").split(",");
		Extent2d extent = Extent2d.parse(bbox[0], bbox[1], bbox[2], bbox[3]);
		ImageBufferARGB image = null;
		DataSource datasource = vectordb.getDataSource();
		try {		
			image = Renderer.render(datasource, extent, width, height);
		} finally {
			VectorDB.closeDataSource(datasource);
		}
		if(image != null) {
			response.setContentType("image/png");
			image.writePngCompressed(response.getOutputStream());
		}
	}

	public void handle_GetCapabilities(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType("application/xml");		
		PrintWriter out = response.getWriter();		
		try {
			xml_root(vectordb, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void xml_root(VectorDB vectordb, PrintWriter out) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getCapabilities(vectordb, doc));
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
	}

	String NS_URL = "http://www.opengis.net/wms";
	String NS_XLINK = "http://www.w3.org/1999/xlink";

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

	private Node getCapabilities(VectorDB vectordb, Document doc) {
		Element rootElement = doc.createElementNS(NS_URL, "WMS_Capabilities");
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttribute("xmlns:xlink", NS_XLINK);

		Element eService = addElement(rootElement, "Service");
		addElement(eService, "Name", "OWS:WMS");
		addElement(eService, "Title", "Remote Sensing Database"); // not shown by qgis
		addElement(eService, "Abstract", "WMS service"); // not shown by qgis

		Element eCapability = addElement(rootElement, "Capability");

		addRootLayer(vectordb, eCapability, vectordb.getName(), vectordb.getName());

		return rootElement;
	}

	private void addRootLayer(VectorDB vectordb, Element eCapability, String name, String title) {
		Element eRootLayer = addElement(eCapability, "Layer");
		String code = vectordb.getDetails().epsg;
		addElement(eRootLayer, "Name", name);
		addElement(eRootLayer, "Title", title);
		addElement(eRootLayer, "CRS", code);
		Element eBoundingBox = addElement(eRootLayer, "BoundingBox");
		eBoundingBox.setAttribute("CRS", code);

		Extent2d extent = vectordb.getExtent();

		eBoundingBox.setAttribute("minx", "" + extent.xmin);
		eBoundingBox.setAttribute("miny", "" + extent.ymin);
		eBoundingBox.setAttribute("maxx", "" + extent.xmax);
		eBoundingBox.setAttribute("maxy", "" + extent.ymax);
	}
}