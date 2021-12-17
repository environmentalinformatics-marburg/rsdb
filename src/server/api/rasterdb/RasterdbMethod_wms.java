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


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.Broker;
import broker.TimeSlice;
import rasterdb.BandProcessor;
import rasterdb.CustomWMS;
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
import util.frame.DoubleFrame;
import util.image.ImageBufferARGB;
import util.image.MonoColor;
import util.image.Renderer;

public class RasterdbMethod_wms extends RasterdbMethod {
	

	public RasterdbMethod_wms(Broker broker) {
		super(broker, "wms");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			Logger.error("no WMS");
			return;
		}*/		

		String reqParam = Web.getLastString(request, "Request", null);
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "REQUEST", null);
		}
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "request", null);
		}
		if(reqParam==null) {
			reqParam = "GetCapabilities";
		}

		switch (reqParam) {
		case "GetMap":
			handle_GetMap(rasterdb, target, request, response, userIdentity);
			break;
		case "GetCapabilities":
			handle_GetCapabilities(rasterdb, target, request, response, userIdentity);
			break;
		default:
			Logger.error("unknown request "+reqParam);
			return;
		}
	}

	public void handle_GetMap(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {

		double[] range = null;
		double gamma = Double.NaN;
		boolean gamma_auto_sync = false;
		int[] palette = null;
		String format = "png";

		if(!target.isEmpty()) {
			Logger.info("target |" + target + "|");
			CustomWMS customWMS = rasterdb.customWmsMapReadonly.get(target);
			if(customWMS != null) {
				if(customWMS.hasValue_range()) {
					switch(customWMS.value_range) {
					case "auto":
						range = null;
						break;
					case "static":
						range = new double[] {customWMS.value_range_static_min, customWMS.value_range_static_max};
						break;
					default:
						Logger.warn("unknown value_range: " + customWMS.value_range);
					}
				}
				if(customWMS.hasGamma()) {
					if(customWMS.gamma.equals("auto")) {
						gamma = Double.NaN;
						gamma_auto_sync = customWMS.gamma_auto_sync;
					} else {
						try {
							gamma = Double.parseDouble(customWMS.gamma);
						} catch (Exception e) {
							Logger.warn("unknown gamma value: " + e + "   " + customWMS.gamma);
						}
					}
				}
				if(customWMS.hasPalette()) {
					palette = MonoColor.getPaletteDefaultNull(customWMS.palette);
				}
				if(customWMS.hasFormat()) {
					format = customWMS.format;
				}
			} else {
				Logger.warn("custom WMS not found |" + target + "|");
			}
		}


		String layers = Web.getString(request, "LAYERS", "color");
		String[] layerList = layers.split(",");
		if(layerList.length > 1) {
			Logger.warn("multiple layers specified in LAYERS. Using first layer only.");
		}
		String layer = layerList[0];

		String[] lparams = layer.split("/");
		String bandText = lparams[0];
		int timestamp = lparams.length > 1 ? Integer.parseInt(lparams[1]) : rasterdb.rasterUnit().timeKeysReadonly().isEmpty() ? 0 : rasterdb.rasterUnit().timeKeysReadonly().last();

		String styles = Web.getString(request, "STYLES");
		if(styles != null && !styles.isEmpty()) {
			String[] stylesList = styles.split(",");
			if(stylesList.length > 1) {
				Logger.warn("styles for multiple layers specified in STYLES. Using styles of first layer only.");
			}
			String style = stylesList[0];
			String[] sParams = style.split("/");
			for(String sParam : sParams) {
				String[] args = sParam.split(" ");
				switch(args[0]) {
				
				default:
					Logger.warn("unknown style type: " + args[0]);
				}
			}
		}


		int width = Web.getInt(request, "WIDTH");
		int height = Web.getInt(request, "HEIGHT");
		String[] bbox = request.getParameter("BBOX").split(",");
		//Logger.info("bbox "+Arrays.toString(bbox));
		//boolean transposed = rasterdb.ref().wms_transposed;
		boolean transposed = false;
		if(transposed) {
			Logger.info("!!!            transposed                !!!");
		}
		Range2d range2d = rasterdb.ref().parseBboxToRange2d(bbox, transposed);
		BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);
		ImageBufferARGB image = null;
		if(bandText.equals("color")) {
			image = Rasterizer.rasterizeRGB(processor, width, height, gamma, range, gamma_auto_sync, null);
		} else if(bandText.startsWith("band")) {
			int bandIndex = Integer.parseInt(bandText.substring(4));
			TimeBand timeBand = processor.getTimeBand(bandIndex);
			if(palette == null) {
				image = Rasterizer.rasterizeGrey(processor, timeBand, width, height, gamma, range, null);
			} else {
				image = Rasterizer.rasterizePalette(processor, timeBand, width, height, gamma, range, palette, null);	
			}
		} else {
			ErrorCollector errorCollector = new ErrorCollector();
			DoubleFrame[] doubleFrames = DSL.process(bandText, errorCollector, processor);
			if(palette == null) {
				image = Renderer.renderGreyDouble(doubleFrames[0], width, height, gamma, range);
			} else {
				image = Renderer.renderPaletteDouble(doubleFrames[0], width, height, gamma, range, palette);
			}
		}

		switch(format) {
		case "jpg": {
			response.setContentType("image/jpeg");
			image.writeJpg(response.getOutputStream(), 0.7f);
			break;
		}
		case "jpg:small": {
			response.setContentType("image/jpeg");
			image.writeJpg(response.getOutputStream(), 0.3f);
			break;
		}
		case "png:uncompressed": {
			response.setContentType("image/png");
			image.writePng(response.getOutputStream(), 0);
			break;
		}
		case "png":
		case "png:compressed":
		default: {
			response.setContentType("image/png");
			image.writePngCompressed(response.getOutputStream());
		}
		}

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
		if(localRange == null) {
			return;
		}
		GeoReference ref = rasterdb.ref();
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