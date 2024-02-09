package server.api.rasterdb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.TreeSet;

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
import org.gdal.osr.CoordinateTransformation;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.TimeSlice;
import pointcloud.Rect2d;
import rasterdb.CustomWMS;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import server.api.rasterdb.WmsCapabilities.WmsStyle;
import util.GeoUtil;
import util.Range2d;
import util.TimeUtil;
import util.Web;

public class RasterdbMethod_webwms_GetCapabilities {

	private static final String NS_URL = "http://www.opengis.net/wms";
	private static final String NS_XLINK = "http://www.w3.org/1999/xlink";

	public static void handle_GetCapabilities(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType(Web.MIME_XML);		
		PrintWriter out = response.getWriter();		
		
		CustomWMS customWMS = null;
		if(!target.isEmpty()) {
			Logger.info("target |" + target + "|");
			customWMS = rasterdb.customWmsMapReadonly.get(target);
			if(customWMS == null) {
				throw new RuntimeException("custom WMS not found |" + target + "|");
			}
		}
		
		try {
			String requestUrl = request.getRequestURL().toString();
			Logger.info("WMS requesUrl   " + requestUrl);
			xml_root(rasterdb, customWMS, requestUrl, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void xml_root(RasterDB rasterdb, CustomWMS customWMS, String requestUrl, PrintWriter out) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getCapabilities(rasterdb, customWMS, requestUrl, doc));
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
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

	private static Node getCapabilities(RasterDB rasterdb, CustomWMS customWMS, String requestUrl, Document doc) {
		Element rootElement = doc.createElementNS(NS_URL, "WMS_Capabilities");
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttribute("xmlns:xlink", NS_XLINK);

		Element eService = addElement(rootElement, "Service");
		addElement(eService, "Name", "WMS");
		addElement(eService, "Title", "Remote Sensing Database");
		addElement(eService, "Abstract", "WMS service"); 
		addElement(eService, "LayerLimit", "1"); // only one layer should be specified at GetMap LAYERS parameter, ignored by (most?) clients

		Element eCapability = addElement(rootElement, "Capability");

		addRequest(eCapability, requestUrl);

		for (WmsStyle style : WmsCapabilities.getWmsStyles(rasterdb)) {
			addRootLayer(rasterdb, customWMS, eCapability, style.name, style.title);
		}

		return rootElement;
	}

	private static void addRequest(Element eCapability, String requestUrl) {
		Element eRootRequest = addElement(eCapability, "Request");

		Element eGetCapabilities = addElement(eRootRequest, "GetCapabilities");
		//addElement(eGetCapabilities, "Format", "application/vnd.ogc.wms_xml"); // non standard ??
		addElement(eGetCapabilities, "Format", "text/xml");
		Element eGetCapabilitiesDCPType = addElement(eGetCapabilities, "DCPType");
		Element eGetCapabilitiesDCPTypeHTTP = addElement(eGetCapabilitiesDCPType, "HTTP");
		Element eGetCapabilitiesDCPTypeHTTPGet = addElement(eGetCapabilitiesDCPTypeHTTP, "Get");		
		Element eGetCapabilitiesDCPTypeHTTPGetOnlineResource = addElement(eGetCapabilitiesDCPTypeHTTPGet, "OnlineResource");
		eGetCapabilitiesDCPTypeHTTPGetOnlineResource.setAttribute("xlink:type", "simple");
		eGetCapabilitiesDCPTypeHTTPGetOnlineResource.setAttribute("xlink:href", requestUrl);

		Element eGetMap = addElement(eRootRequest, "GetMap");
		addElement(eGetMap, "Format", "image/png");
		addElement(eGetMap, "Format", "image/jpeg");
		Element eGetMapDCPType = addElement(eGetMap, "DCPType");
		Element eGetMapDCPTypeHTTP = addElement(eGetMapDCPType, "HTTP");
		Element eGetMapDCPTypeHTTPGet = addElement(eGetMapDCPTypeHTTP, "Get");		
		Element eGetMapDCPTypeHTTPGetOnlineResource = addElement(eGetMapDCPTypeHTTPGet, "OnlineResource");
		eGetMapDCPTypeHTTPGetOnlineResource.setAttribute("xlink:type", "simple");
		eGetMapDCPTypeHTTPGetOnlineResource.setAttribute("xlink:href", requestUrl);
	}

	private static void tryAdd_GeographicBoundingBox(Range2d localRange, GeoReference ref, int layerEPSG, Element eRootLayer) {
		try {
			util.GeoUtil.Transformer transfomer = GeoUtil.getCoordinateTransformer(layerEPSG, GeoUtil.EPSG_WGS84);
			double[] p1 = transfomer.transformWithAxisOrderCorrection(ref.pixelXToGeo(localRange.xmin), ref.pixelYToGeo(localRange.ymin));
			double[] p2 = transfomer.transformWithAxisOrderCorrection(ref.pixelXToGeo(localRange.xmax + 1), ref.pixelYToGeo(localRange.ymax + 1));
			double westBoundLongitude = p1[0];
			double eastBoundLongitude = p2[0];
			double southBoundLatitude = p1[1];
			double northBoundLatitude = p2[1];
			if(Double.isFinite(westBoundLongitude) 
					&& Double.isFinite(eastBoundLongitude) 
					&& Double.isFinite(southBoundLatitude) 
					&& Double.isFinite(northBoundLatitude)
					&& westBoundLongitude != eastBoundLongitude
					&& southBoundLatitude != northBoundLatitude) {
				Element eEX_GeographicBoundingBox = addElement(eRootLayer, "EX_GeographicBoundingBox");		
				addElement(eEX_GeographicBoundingBox, "westBoundLongitude", "" + westBoundLongitude);
				addElement(eEX_GeographicBoundingBox, "eastBoundLongitude", "" + eastBoundLongitude);		
				addElement(eEX_GeographicBoundingBox, "southBoundLatitude", "" + southBoundLatitude);
				addElement(eEX_GeographicBoundingBox, "northBoundLatitude", "" + northBoundLatitude);
			}
		} catch(Exception e) {
			Logger.warn(e);
		}
	}

	private static void addRootLayer(RasterDB rasterdb, CustomWMS customWMS, Element eCapability, String name, String title) {
		Element eRootLayer = addElement(eCapability, "Layer");

		GeoReference ref = rasterdb.ref();

		if(!ref.has_code()) {
			throw new RuntimeException("no EPSG projection information");
		}
		int layerEPSG = ref.getEPSG(0);
		if(layerEPSG <= 0) {
			throw new RuntimeException("no EPSG projection information");
		}

		addElement(eRootLayer, "Name", name);
		addElement(eRootLayer, "Title", title);

		Range2d localRange = rasterdb.getLocalRange(false);
		if(localRange == null) {
			return;
		}

		tryAdd_GeographicBoundingBox(localRange, ref, layerEPSG, eRootLayer);	
		
		int wmsEPSG = GeoUtil.EPSG_WEB_MERCATOR;
		
		Rect2d layerRect = ref.range2dToRect2d(localRange);
		double[][] layerPoints = layerRect.createPoints9(); // larger extent
		//double[][] layerPoints = layerRect.createPointsMidBorder(); // mean extent
		Logger.info(Arrays.deepToString(layerPoints));
		util.GeoUtil.Transformer ctToWMS = GeoUtil.getCoordinateTransformer(layerEPSG, wmsEPSG);
		if(ctToWMS == null) {
			throw new RuntimeException("no valid EPSG projection information");
		}
		ctToWMS.transformWithAxisOrderCorrection(layerPoints);
		Logger.info(Arrays.deepToString(layerPoints));

		Rect2d wmsRect = Rect2d.ofPoints(layerPoints);
		Logger.info(wmsRect.toString());

		addElement(eRootLayer, "CRS", "EPSG:3857");
		Element eBoundingBox = addElement(eRootLayer, "BoundingBox");
		eBoundingBox.setAttribute("CRS", "EPSG:3857");

		eBoundingBox.setAttribute("minx", "" + wmsRect.xmin);
		eBoundingBox.setAttribute("miny", "" + wmsRect.ymin);
		eBoundingBox.setAttribute("maxx", "" + wmsRect.xmax);
		eBoundingBox.setAttribute("maxy", "" + wmsRect.ymax);

		if(ref.has_pixel_size()) {
			double wmsPixelSizeX = wmsRect.width() / localRange.getWidth(); // TODO pixel size transformation only
			double wmsPixelSizeY = wmsRect.height() / localRange.getHeight();
			eBoundingBox.setAttribute("resx", "" + wmsPixelSizeX);
			eBoundingBox.setAttribute("resy", "" + wmsPixelSizeY);
		}

		TreeSet<Integer> timestamps = new TreeSet<Integer>();
		timestamps.addAll(rasterdb.rasterUnit().timeKeysReadonly());
		timestamps.addAll(rasterdb.timeMapReadonly.keySet());

		for(Integer timestamp:timestamps) {
			Element eTimeLayer = addElement(eRootLayer, "Layer");
			addElement(eTimeLayer, "Name", name + "/" + timestamp);
			TimeSlice timeSlice = rasterdb.timeMapReadonly.get(timestamp);
			if(timeSlice == null) {
				addElement(eTimeLayer, "Title", title + " / " + TimeUtil.toPrettyText(timestamp));
			} else {
				addElement(eTimeLayer, "Title", title + " / " + timeSlice.name);
			}			
		}
	}
}
