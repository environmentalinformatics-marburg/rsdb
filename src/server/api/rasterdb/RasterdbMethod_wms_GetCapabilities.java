package server.api.rasterdb;

import java.io.IOException;
import java.io.PrintWriter;
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
import org.gdal.osr.SpatialReference;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.TimeSlice;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import server.api.rasterdb.WmsCapabilities.WmsStyle;
import util.GeoUtil;
import util.Range2d;
import util.TimeUtil;
import util.Web;

public class RasterdbMethod_wms_GetCapabilities {
	
	private static final String NS_URL = "http://www.opengis.net/wms";
	private static final String NS_XLINK = "http://www.w3.org/1999/xlink";
	
	public static void handle_GetCapabilities(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType(Web.MIME_XML);		
		PrintWriter out = response.getWriter();		
		try {
			String requestUrl = request.getRequestURL().toString();
			Logger.info("WMS requesUrl   " + requestUrl);
			xml_root(rasterdb, requestUrl, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void xml_root(RasterDB rasterdb, String requestUrl, PrintWriter out) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getCapabilities(rasterdb, requestUrl, doc));
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

	private static Node getCapabilities(RasterDB rasterdb, String requestUrl, Document doc) {
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
			addRootLayer(rasterdb, eCapability, style.name, style.title);
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

	private static void addRootLayer(RasterDB rasterdb, Element eCapability, String name, String title) {
		Element eRootLayer = addElement(eCapability, "Layer");
		
		GeoReference ref = rasterdb.ref();
		String code = ref.optCode("EPSG:3857");
		addElement(eRootLayer, "Name", name);
		addElement(eRootLayer, "Title", title);

		Range2d localRange = rasterdb.getLocalRange(false);
		if(localRange == null) {
			return;
		}

		if(ref.has_code()) {
			try {
				int epsg = ref.getEPSG(0);
				if(epsg != 0) {
					SpatialReference layerSr = GeoUtil.spatialReferenceFromEPSG(epsg);
					CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(layerSr, GeoUtil.WGS84_SPATIAL_REFERENCE);
					double[] p1 = ct.TransformPoint(ref.pixelXToGeo(localRange.xmin), ref.pixelYToGeo(localRange.ymin));
					double[] p2 = ct.TransformPoint(ref.pixelXToGeo(localRange.xmax + 1), ref.pixelYToGeo(localRange.ymax + 1));
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
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
		}

		addElement(eRootLayer, "CRS", code);
		Element eBoundingBox = addElement(eRootLayer, "BoundingBox");
		eBoundingBox.setAttribute("CRS", code);
		//boolean transposed = ref.wms_transposed;
		boolean transposed = false;
		if (transposed) {
			eBoundingBox.setAttribute("minx", "" + ref.pixelYToGeo(localRange.ymin));
			eBoundingBox.setAttribute("miny", "" + ref.pixelXToGeo(localRange.xmin));
			eBoundingBox.setAttribute("maxx", "" + ref.pixelYToGeo(localRange.ymax + 1));
			eBoundingBox.setAttribute("maxy", "" + ref.pixelXToGeo(localRange.xmax + 1));
		} else {
			eBoundingBox.setAttribute("minx", "" + ref.pixelXToGeo(localRange.xmin));
			eBoundingBox.setAttribute("miny", "" + ref.pixelYToGeo(localRange.ymin));
			eBoundingBox.setAttribute("maxx", "" + ref.pixelXToGeo(localRange.xmax + 1));
			eBoundingBox.setAttribute("maxy", "" + ref.pixelYToGeo(localRange.ymax + 1));
		}
		if(ref.has_pixel_size()) {
			eBoundingBox.setAttribute("resx", "" + ref.pixel_size_x);
			eBoundingBox.setAttribute("resy", "" + ref.pixel_size_y);
		}

		/*ReadonlyNavigableSetView<Integer> timekeys = rasterdb.rasterUnit().timeKeysReadonly();
		for(Integer timeKey:timekeys) {
			if(timeKey > 0) {
				Element eTimeLayer = addElement(eRootLayer, "Layer");
				addElement(eTimeLayer, "Name", name + "/" + timeKey);
				addElement(eTimeLayer, "Title", title + " / " + TimeUtil.toPrettyText(timeKey));
			}
		}*/

		TreeSet<Integer> timestamps = new TreeSet<Integer>();
		timestamps.addAll(rasterdb.rasterUnit().timeKeysReadonly());
		timestamps.addAll(rasterdb.timeMapReadonly.keySet());

		/*if(!timestamps.isEmpty()) { // TIME-paramter not used by clients ?
			StringBuilder content = new StringBuilder();
			boolean following = false;
			String last = null;
			for(Integer timestamp : timestamps) {
				if(following) {
					content.append(',');
				} else {
					following = true;
				}
				TimeSlice timeSlice = rasterdb.timeMapReadonly.get(timestamp);
				if(timeSlice == null) {
					last = TimeUtil.toPrettyText(timestamp);
				} else {
					last = timeSlice.name;
				}
				content.append(last);
			}
			Element eDimension = addElement(eRootLayer, "Dimension", content.toString());
			eDimension.setAttribute("name", "time");
			eDimension.setAttribute("units", ""); // no unit
			if(last != null) {
				eDimension.setAttribute("default", last);
			}
		}*/

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
