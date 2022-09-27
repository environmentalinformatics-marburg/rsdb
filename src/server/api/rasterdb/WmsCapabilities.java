package server.api.rasterdb;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.tinylog.Logger;
import org.eclipse.jetty.server.UserIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.Broker;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import util.Range2d;
import util.TimeUtil;
import util.collections.ReadonlyNavigableSetView;

public class WmsCapabilities {
	

	private final Broker broker;
	private final String baseURL;

	public WmsCapabilities(Broker broker, String baseURL) {
		this.broker = broker;
		this.baseURL = baseURL.endsWith("/") ? baseURL.substring(0, baseURL.length() - 1) : baseURL;
		Logger.info("baseURL " + this.baseURL);
	}

	public void capabilities(OutputStream out, UserIdentity userIdentity) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			doc.appendChild(getCapabilities(doc, userIdentity));
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
		}
	}

	String NS_URL = "http://www.opengis.net/wms";
	String NS_XLINK = "http://www.w3.org/1999/xlink";

	private Node getCapabilities(Document doc, UserIdentity userIdentity) {

		Element rootElement = doc.createElementNS(NS_URL, "WMS_Capabilities");
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttribute("xmlns:xlink", NS_XLINK);

		Element eService = addElement(rootElement, "Service");
		addElement(eService, "Name", "OWS:WMS");
		addElement(eService, "Title", "Remote Sensing Database"); // not shown by qgis
		addElement(eService, "Abstract", "WMS service"); // not shown by qgis

		Element eCapability = addElement(rootElement, "Capability");

		addRequest(eCapability);

		Element eRootLayer = addElement(eCapability, "Layer");
		addElement(eRootLayer, "Name", "rsdb");
		addElement(eRootLayer, "Title", "Remote Sensing Database");
		addElement(eRootLayer, "Abstract", "WMS service");
		for (String name : broker.getRasterdbNames()) {
			Logger.info("load " + name);
			RasterDB rasterdb = broker.getRasterdb(name);
			if(rasterdb.isAllowed(userIdentity)) {
				// int default_epsg = 4326; // WGS-84 / geographisch 2D weltweites System für
				// GPS-Geräte, OpenStreetMap Datenbank
				String default_epsg = "EPSG:3857"; // WGS 84 / Pseudo-Mercator Google Maps, OpenStreetMap und andere
				// Kartenanbieter im Netz.
				// int default_epsg = 32737;
				String code = rasterdb.ref().optCode(default_epsg);
				addLayer(eRootLayer, code, rasterdb);
			}
		}
		return rootElement;
	}

	public void addRequest(Element root) {
		Element eRequest = addElement(root, "Request");
		Element eGetCapabilities = addGetResource(eRequest, "GetCapabilities");
		addElement(eGetCapabilities, "Format", "text/xml");
		Element eGetMap = addGetResource(eRequest, "GetMap");
		addElement(eGetMap, "Format", "image/png");

	}

	public Element addGetResource(Element root, String name) {
		Element eGetMap = addElement(root, name);
		Element eDCPType = addElement(eGetMap, "DCPType");
		Element eHTTP = addElement(eDCPType, "HTTP");
		Element eGet = addElement(eHTTP, "Get");
		addXlink(eGet, baseURL);
		return eGetMap;
	}

	public static void addXlink(Element root, String url) {
		Element eOnlineResource = addElement(root, "OnlineResource");
		eOnlineResource.setAttribute("xlink:type", "simple");
		eOnlineResource.setAttribute("xlink:href", url);
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

	public void addLayer(Element root, String code, RasterDB rasterdb) {
		String name = rasterdb.config.getName();
		String description = rasterdb.informal().description;
		int bandCount = rasterdb.bandMapReadonly.size();
		Element eLayer = addElement(root, "Layer");
		addElement(eLayer, "Name", name);
		addElement(eLayer, "Title", name + " - " + description);
		addElement(eLayer, "Abstract", "raster with " + bandCount + " bands");
		addElement(eLayer, "CRS", code);
		Element eBoundingBox = addElement(eLayer, "BoundingBox");
		eBoundingBox.setAttribute("CRS", code);
		Range2d localRange = rasterdb.getLocalRange(false);
		if(localRange == null) {
			return;
		}
		GeoReference ref = rasterdb.ref();
		/*
		 * NumberFormat formatter = new DecimalFormat("########");
		 * eBoundingBox.setAttribute("minx",
		 * ""+formatter.format(ref.pixelToGeo(localRange.xmin - 2000)));
		 * eBoundingBox.setAttribute("miny",
		 * ""+formatter.format(ref.pixelToGeo(localRange.ymin - 2000)));
		 * eBoundingBox.setAttribute("maxx",
		 * ""+formatter.format(ref.pixelToGeo(localRange.xmax + 2000)));
		 * eBoundingBox.setAttribute("maxy",
		 * ""+formatter.format(ref.pixelToGeo(localRange.ymax + 2000)));
		 */
		if (ref.wms_transposed) {
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

		addTime(eLayer, rasterdb);

		for (WmsStyle style : getWmsStyles(rasterdb)) {
			addStyle(eLayer, style.name, style.title, style.description);
		}
	}

	public void addStyle(Element root, String name, String title, String description) {
		Element eStyle = addElement(root, "Style");
		addElement(eStyle, "Name", name);
		addElement(eStyle, "Title", title);
		addElement(eStyle, "Abstract", description);
	}

	public static class WmsStyle {
		public final String name;
		public final String title;
		public final String description;

		public WmsStyle(String name, String title, String description) {
			this.name = name;
			this.title = title;
			this.description = description;
		}
	}

	public static List<WmsStyle> getWmsStyles(RasterDB rasterdb) {
		ArrayList<WmsStyle> list = new ArrayList<WmsStyle>();
		if (rasterdb.bandMapReadonly.size() > 1) {
			list.add(new WmsStyle("color", "color", "best fitting rgb visualisation"));
		}
		for (Band band : rasterdb.bandMapReadonly.values()) {
			String bandName = "band" + band.index;
			String bandTitle = band.has_title() ? band.title
					: band.has_wavelength() ? band.wavelength + " nm wavelength" : bandName;
			String bandDescription = "one band visualisation";
			list.add(new WmsStyle(bandName, bandTitle, bandDescription));
		}
		if (rasterdb.bandMapReadonly.size() > 1) {
			if (rasterdb.bandMapReadonly.values().stream().anyMatch(Band::has_wavelength)) {
				list.add(new WmsStyle("ndvi", "NDVI", "ndvi"));
				list.add(new WmsStyle("evi", "EVI", "evi"));
				list.add(new WmsStyle("evi2", "EVI2", "evi2"));
				list.add(new WmsStyle("savi", "SAVI", "savi"));
				list.add(new WmsStyle("mpri", "MPRI", "mpri"));
				list.add(new WmsStyle("mgvri", "MGVRI", "mgvri"));
				list.add(new WmsStyle("gli", "GLI", "gli"));
				list.add(new WmsStyle("rgvbi", "RGVBI", "rgvbi"));
				list.add(new WmsStyle("exg", "ExG", "exg"));
				list.add(new WmsStyle("veg", "VEG", "veg"));
				list.add(new WmsStyle("tgi", "TGI", "tgi"));
			}
		}
		return list;
	}

	public static void addTime(Element root, RasterDB rasterdb) {
		ReadonlyNavigableSetView<Integer> timeKeys = rasterdb.rasterUnit().timeKeysReadonly();
		if (timeKeys.isEmpty()) {
			return;
		}
		String lastTime = null;
		String times = "";
		for (Integer t : timeKeys) {
			if (0 < t) {
				String time = TimeUtil.toText(t);
				if (lastTime == null) {
					times = time;
				} else {
					times += ',' + time;
				}
				lastTime = time;
			}
		}
		if(lastTime == null) {
			return;
		}
		Element eDimension = addElement(root, "Dimension", times);
		eDimension.setAttribute("name", "time");
		eDimension.setAttribute("units", "ISO8601");
		eDimension.setAttribute("default", lastTime);
	}

}
