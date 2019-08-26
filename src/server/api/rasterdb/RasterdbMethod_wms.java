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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.Broker;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.Rasterizer;
import rasterdb.TimeBand;
import rasterdb.dsl.DSL;
import rasterdb.dsl.ErrorCollector;
import server.api.rasterdb.WmsCapabilities.WmsStyle;
import util.Range2d;
import util.TimeUtil;
import util.Web;
import util.collections.ReadonlyNavigableSetView;
import util.frame.DoubleFrame;
import util.image.ImageBufferARGB;
import util.image.Renderer;

public class RasterdbMethod_wms extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_wms(Broker broker) {
		super(broker, "wms");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			log.error("no WMS");
			return;
		}*/

		String reqParam = request.getParameter("Request");
		if(reqParam==null) {
			reqParam = request.getParameter("REQUEST");
		}
		if(reqParam==null) {
			reqParam = request.getParameter("request");
		}


		if(reqParam==null) {
			throw new RuntimeException("no REQUEST parameter");
		}

		switch (reqParam) {
		case "GetMap":
			handle_GetMap(rasterdb, target, request, response, userIdentity);
			break;
		case "GetCapabilities":
			handle_GetCapabilities(rasterdb, target, request, response, userIdentity);
			break;
		default:
			log.error("unknown request "+reqParam);
			return;
		}
	}

	public void handle_GetMap(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String layers = Web.getString(request, "LAYERS", "color");

		String[] lparam = layers.split("/");
		String bandText = lparam[0];
		int timestamp = lparam.length > 1 ? Integer.parseInt(lparam[1]) : rasterdb.rasterUnit().timeKeysReadonly().isEmpty() ? 0 : rasterdb.rasterUnit().timeKeysReadonly().last();
		int width = Web.getInt(request, "WIDTH");
		int height = Web.getInt(request, "HEIGHT");
		String[] bbox = request.getParameter("BBOX").split(",");
		Range2d range2d = rasterdb.ref().parseBboxToRange2d(bbox, false);
		BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);
		ImageBufferARGB image = null;
		if(bandText.equals("color")) {
			image = Rasterizer.rasterizeRGB(processor, width, height, Double.NaN, null, false, null);
		} else if(bandText.startsWith("band")) {
			int bandIndex = Integer.parseInt(bandText.substring(4));
			TimeBand timeBand = processor.getTimeBand(bandIndex);
			image = Rasterizer.rasterizeGrey(processor, timeBand, width, height, Double.NaN, null, null);
		} else {
			ErrorCollector errorCollector = new ErrorCollector();
			DoubleFrame[] doubleFrames = DSL.process(bandText, errorCollector, processor);
			image = Renderer.renderGreyDouble(doubleFrames[0], width, height, Double.NaN, null);
		}
		response.setContentType("image/png");
		image.writePngCompressed(response.getOutputStream());
	}

	public void handle_GetCapabilities(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType("application/xml");		
		PrintWriter out = response.getWriter();		
		try {
			xml_root(rasterdb, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void xml_root(RasterDB rasterdb, PrintWriter out) throws ParserConfigurationException, TransformerException {
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

	private Node getCapabilities(RasterDB rasterdb, Document doc) {
		Element rootElement = doc.createElementNS(NS_URL, "WMS_Capabilities");
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttribute("xmlns:xlink", NS_XLINK);

		Element eService = addElement(rootElement, "Service");
		addElement(eService, "Name", "OWS:WMS");
		addElement(eService, "Title", "Remote Sensing Database"); // not shown by qgis
		addElement(eService, "Abstract", "WMS service"); // not shown by qgis

		Element eCapability = addElement(rootElement, "Capability");
		//addRequest(eCapability);

		for (WmsStyle style : WmsCapabilities.getWmsStyles(rasterdb)) {
			addRootLayer(rasterdb, eCapability, style.name, style.title);
		}

		return rootElement;
	}

	private void addRootLayer(RasterDB rasterdb, Element eCapability, String name, String title) {
		Element eRootLayer = addElement(eCapability, "Layer");



		String code = rasterdb.ref().optCode("EPSG:3857");
		addElement(eRootLayer, "Name", name);
		addElement(eRootLayer, "Title", title);
		addElement(eRootLayer, "CRS", code);
		Element eBoundingBox = addElement(eRootLayer, "BoundingBox");
		eBoundingBox.setAttribute("CRS", code);
		Range2d localRange = rasterdb.getLocalRange(false);
		GeoReference ref = rasterdb.ref();
		if (ref.wms_transposed) {
			eBoundingBox.setAttribute("minx", "" + ref.pixelYToGeo(localRange.ymin));
			eBoundingBox.setAttribute("miny", "" + ref.pixelXToGeo(localRange.xmin));
			eBoundingBox.setAttribute("maxx", "" + ref.pixelYToGeo(localRange.ymax));
			eBoundingBox.setAttribute("maxy", "" + ref.pixelXToGeo(localRange.xmax));
		} else {
			eBoundingBox.setAttribute("minx", "" + ref.pixelXToGeo(localRange.xmin));
			eBoundingBox.setAttribute("miny", "" + ref.pixelYToGeo(localRange.ymin));
			eBoundingBox.setAttribute("maxx", "" + ref.pixelXToGeo(localRange.xmax));
			eBoundingBox.setAttribute("maxy", "" + ref.pixelYToGeo(localRange.ymax));
		}

		ReadonlyNavigableSetView<Integer> timekeys = rasterdb.rasterUnit().timeKeysReadonly();
		for(Integer timeKey:timekeys) {
			if(timeKey > 0) {
				Element eTimeLayer = addElement(eRootLayer, "Layer");
				addElement(eTimeLayer, "Name", name + "/" + timeKey);
				addElement(eTimeLayer, "Title", title + " / " + TimeUtil.toPrettyText(timeKey));
			}
		}

	}

}