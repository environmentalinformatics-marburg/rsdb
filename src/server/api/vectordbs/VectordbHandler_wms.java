package server.api.vectordbs;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.ogr.DataSource;
import org.gdal.osr.SpatialReference;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ar.com.hjg.pngj.PngjOutputException;
import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.PostgisLayer;
import postgis.style.StyleProvider;
import server.api.postgis.PostgisHandler_wms;
import util.GeoUtil;
import util.Interruptor;
import util.Web;
import util.XmlUtil;
import util.image.ImageBufferARGB;
import vectordb.Renderer;
import vectordb.VectorDB;
import vectordb.style.Style;

public class VectordbHandler_wms extends VectordbHandler {	

	public VectordbHandler_wms(Broker broker) {
		super(broker, "wms");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			Logger.error("no WMS");
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
		case "GetFeatureInfo":				
			handle_GetFeatureInfo(vectordb, request, response, userIdentity);
			break;
		case "GetLegendGraphic":
			handle_GetLegendGraphic(vectordb, request, response);
			break;				
		default:
			Logger.error("unknown request " + reqParam);
			return;
		}		
	}

	public void handle_GetMap(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			int width = Web.getInt(request, "WIDTH");
			int height = Web.getInt(request, "HEIGHT");
			String crs = Web.getString(request, "CRS", null);
			if(crs == null) {
				crs = Web.getString(request, "SRS", null);
			}
			if(crs == null) {
				throw new RuntimeException("parameter not found: CRS or SRS");
			}
			util.GeoUtil.Transformer layerWmsTransformer = null;

			SpatialReference layerSr = vectordb.getSpatialReference();
			if(layerSr != null && crs != null && crs.startsWith("EPSG:")) {
				SpatialReference wmsSr = null;
				int wmsEPSG = Integer.parseInt(crs.substring(5));
				if(wmsEPSG > 0) {
					if(wmsEPSG == GeoUtil.EPSG_WEB_MERCATOR) {
						wmsSr = GeoUtil.WEB_MERCATOR_SPATIAL_REFERENCE;
					} else {
						wmsSr = GeoUtil.getSpatialReferenceFromEPSG(wmsEPSG);		
					}
				}
				if(wmsSr != null) {
					if(wmsSr.IsSame(layerSr) == 0) {
						layerWmsTransformer = new GeoUtil.Transformer(layerSr, wmsSr);
						Logger.info("transform " + layerWmsTransformer.toString());
					}
				}
			}

			String labelField = vectordb.hasNameAttribute() ? vectordb.getNameAttribute() : null;
			String wmsLabelField = Web.getString(request, "label_field", null);
			if(wmsLabelField != null) {
				labelField = wmsLabelField.equals("null") ? null : wmsLabelField;
			}

			String[] bbox = request.getParameter("BBOX").split(",");
			Rect2d wmsRect = Rect2d.parse(bbox[0], bbox[1], bbox[2], bbox[3]);			

			ImageBufferARGB image = null;
			DataSource datasource = vectordb.getDataSource();
			try {		
				//image = Renderer.render(datasource, vectordb, wmsRect, width, height, layerWmsTransformer, vectordb.getStyle(), labelField);
				{
					Style style = vectordb.getStyle();
					if(style == null) {
						style = Renderer.STYLE_DEFAULT;
					}
					boolean swapCoordinates = layerWmsTransformer != null && layerWmsTransformer.dstFirstAxis == 1;
					if(layerWmsTransformer == null) {
						if(layerSr != null) {
							swapCoordinates = layerSr.GetAxisOrientation(null, 0) == 1;
//							if(GeoUtil.WGS84_SPATIAL_REFERENCE.IsSame(layerSr) != 0) { // workaround for swapped axis but not in GetAxisOrientation for EPSG:4326
//								swapCoordinates = true;
//							} else {
//								swapCoordinates = layerSr.GetAxisOrientation(null, 0) == 1;
//							}
						}
					}
					Logger.info("layerSr.GetAxisOrientation(null, 0) " + layerSr.GetAxisOrientation(null, 0) + "    " + swapCoordinates);
					image = ConverterRenderer.render(datasource, vectordb, wmsRect, width, height, labelField, style, layerWmsTransformer, swapCoordinates);					
					//image = ConverterRenderer.render(datasource, vectordb, wmsRect, width, height, labelField, style, layerWmsTransformer, false);
					
					printCRSinfo(3044);
					printCRSinfo(3857);
					printCRSinfo(4326);
					printCRSinfo(32632);
					printCRSinfo(32633);
				}
			} finally {
				VectorDB.closeDataSource(datasource);
			}
			if(image != null) {
				response.setContentType(Web.MIME_PNG);
				image.writePngCompressed(response.getOutputStream());
			}
		} catch(PngjOutputException  e) {
			try {
				response.closeOutput();
			} catch(Exception e1) {
				Logger.warn(e1);
			}
			Throwable eCause = e.getCause();
			if(eCause != null && eCause instanceof EofException) {				
				Throwable eCauseSub = eCause.getCause();
				if(eCauseSub != null && eCause instanceof IOException) {
					Logger.info(eCauseSub.getMessage().replace('\n', ' ').replace('\r', ' '));
				} else {
					Logger.warn(eCause);
				}
			} else if(eCause != null && eCause instanceof IOException) {
				Logger.info(eCause.getMessage().replace('\n', ' ').replace('\r', ' '));
			} else{
				Logger.warn(e);
			}			
		} catch(Exception e) {
			try {
				response.closeOutput();
			} catch(Exception e1) {
				Logger.warn(e1);
			}
			Logger.warn(e);
		}
	}
	
	private static void printCRSinfo(int epsg) {
		SpatialReference sr = GeoUtil.getSpatialReferenceFromEPSG(epsg);
		Logger.info(epsg + " AxisOrientation " + sr.GetAxisOrientation(null, 0)); 
	}

	public void handle_GetCapabilities(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String requestUrl = request.getRequestURL().toString();
		response.setContentType(Web.MIME_XML);		
		PrintWriter out = response.getWriter();		
		try {
			xml_root_GetCapabilities(vectordb, out, requestUrl);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void xml_root_GetCapabilities(VectorDB vectordb, PrintWriter out, String requestUrl) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getCapabilities(vectordb, doc, requestUrl));
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
	}

	private Node getCapabilities(VectorDB vectordb, Document doc, String requestUrl) {
		Element rootElement = doc.createElementNS("http://www.opengis.net/wms", "WMS_Capabilities");
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");

		Element eService = XmlUtil.addElement(rootElement, "Service");
		XmlUtil.addElement(eService, "Name", "OWS:WMS");
		XmlUtil.addElement(eService, "Title", "Remote Sensing Database"); // not shown by qgis
		XmlUtil.addElement(eService, "Abstract", "WMS service"); // not shown by qgis

		Element eCapability = XmlUtil.addElement(rootElement, "Capability");

		addRequest(eCapability, requestUrl);
		addRootLayer(vectordb, eCapability, vectordb.getName(), vectordb.getName(), requestUrl);

		return rootElement;
	}

	private static void addRequest(Element eCapability, String requestUrl) {
		Element eRootRequest = XmlUtil.addElement(eCapability, "Request");

		Element eGetCapabilities = XmlUtil.addElement(eRootRequest, "GetCapabilities");
		XmlUtil.addElement(eGetCapabilities, "Format", "text/xml");
		Element eGetCapabilitiesDCPType = XmlUtil.addElement(eGetCapabilities, "DCPType");
		Element eGetCapabilitiesDCPTypeHTTP = XmlUtil.addElement(eGetCapabilitiesDCPType, "HTTP");
		Element eGetCapabilitiesDCPTypeHTTPGet = XmlUtil.addElement(eGetCapabilitiesDCPTypeHTTP, "Get");		
		Element eGetCapabilitiesDCPTypeHTTPGetOnlineResource = XmlUtil.addElement(eGetCapabilitiesDCPTypeHTTPGet, "OnlineResource");
		eGetCapabilitiesDCPTypeHTTPGetOnlineResource.setAttribute("xlink:type", "simple");
		eGetCapabilitiesDCPTypeHTTPGetOnlineResource.setAttribute("xlink:href", requestUrl);

		Element eGetMap = XmlUtil.addElement(eRootRequest, "GetMap");
		XmlUtil.addElement(eGetMap, "Format", "image/png");
		Element eGetMapDCPType = XmlUtil.addElement(eGetMap, "DCPType");
		Element eGetMapDCPTypeHTTP = XmlUtil.addElement(eGetMapDCPType, "HTTP");
		Element eGetMapDCPTypeHTTPGet = XmlUtil.addElement(eGetMapDCPTypeHTTP, "Get");		
		Element eGetMapDCPTypeHTTPGetOnlineResource = XmlUtil.addElement(eGetMapDCPTypeHTTPGet, "OnlineResource");
		eGetMapDCPTypeHTTPGetOnlineResource.setAttribute("xlink:type", "simple");
		eGetMapDCPTypeHTTPGetOnlineResource.setAttribute("xlink:href", requestUrl);

		Element eGetFeatureInfo = XmlUtil.addElement(eRootRequest, "GetFeatureInfo");
		XmlUtil.addElement(eGetFeatureInfo, "Format", "application/json");
		XmlUtil.addElement(eGetFeatureInfo, "Format", "text/xml");
		Element eGetFeatureInfoDCPType = XmlUtil.addElement(eGetFeatureInfo, "DCPType");
		Element eGetFeatureInfoDCPTypeHTTP = XmlUtil.addElement(eGetFeatureInfoDCPType, "HTTP");
		Element eGetFeatureInfoDCPTypeHTTPGet = XmlUtil.addElement(eGetFeatureInfoDCPTypeHTTP, "Get");		
		Element eGetFeatureInfoDCPTypeHTTPGetOnlineResource = XmlUtil.addElement(eGetFeatureInfoDCPTypeHTTPGet, "OnlineResource");
		eGetFeatureInfoDCPTypeHTTPGetOnlineResource.setAttribute("xlink:type", "simple");
		eGetFeatureInfoDCPTypeHTTPGetOnlineResource.setAttribute("xlink:href", requestUrl);
	}

	private void addRootLayer(VectorDB vectordb, Element eCapability, String name, String title, String requestUrl) {
		Element eRootLayer = XmlUtil.addElement(eCapability, "Layer");
		eRootLayer.setAttribute("queryable", "1");
		int layerEPSG = Integer.parseInt(vectordb.getDetails().epsg);
		boolean hasLayerCRS = layerEPSG > 0;
		String crs = "EPSG:" + layerEPSG;
		XmlUtil.addElement(eRootLayer, "Name", name);
		XmlUtil.addElement(eRootLayer, "Title", title);
		XmlUtil.addElement(eRootLayer, "CRS", crs);

		Rect2d layerRect = vectordb.getExtent();

		if(hasLayerCRS) {
			PostgisHandler_wms.tryAdd_GeographicBoundingBox(layerRect, layerEPSG, eRootLayer);
		}

		Element eBoundingBox = XmlUtil.addElement(eRootLayer, "BoundingBox");
		eBoundingBox.setAttribute("CRS", crs);

		eBoundingBox.setAttribute("minx", "" + layerRect.xmin);
		eBoundingBox.setAttribute("miny", "" + layerRect.ymin);
		eBoundingBox.setAttribute("maxx", "" + layerRect.xmax);
		eBoundingBox.setAttribute("maxy", "" + layerRect.ymax);

		addStyle(vectordb, eRootLayer, requestUrl);
	}

	private static int getHeight() {
		return 45;
	}

	private static void addStyle(VectorDB vectordb, Element eRootLayer, String requestUrl) {
		int height = getHeight();

		Element eStyle = XmlUtil.addElement(eRootLayer, "Style");
		XmlUtil.addElement(eStyle, "Name", "default");
		XmlUtil.addElement(eStyle, "Title", "default");
		Element eLegendURL = XmlUtil.addElement(eStyle, "LegendURL");
		eLegendURL.setAttribute("width", "200");
		eLegendURL.setAttribute("height", Integer.toString(height));
		XmlUtil.addElement(eLegendURL, "Format", "image/png");
		Element eOnlineResource = XmlUtil.addElement(eLegendURL, "OnlineResource");
		eOnlineResource.setAttribute("xlink:type", "simple");
		eOnlineResource.setAttribute("xlink:href", requestUrl);
	}

	public void handle_GetFeatureInfo(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		int wmsEPSG = 0;
		String crs = Web.getString(request, "CRS");
		if(crs != null) {
			if(crs.startsWith("EPSG:")) {
				wmsEPSG = Integer.parseInt(crs.substring(5));				
			} else {
				throw new RuntimeException("unknown CRS");
			}
		}

		int layerEPSG = 0;
		try {
			layerEPSG = Integer.parseInt(vectordb.getDetails().epsg);
		} catch(Exception e) {
			Logger.warn(e);
		}
		boolean reproject = wmsEPSG > 0 && layerEPSG > 0 && wmsEPSG != layerEPSG;

		String bboxParam = request.getParameter("BBOX");
		Rect2d wmsRect2d = null;
		if(bboxParam != null) {
			String[] bbox = bboxParam.split(",");
			wmsRect2d = Rect2d.parseBbox(bbox);
		}		
		int reqWidth = Web.getInt(request, "WIDTH");
		int reqHeight = Web.getInt(request, "HEIGHT");		
		int reqX = Web.getInt(request, "I");
		int reqY = Web.getInt(request, "J");

		double xres = wmsRect2d.width() / reqWidth;
		double yres = wmsRect2d.height() / reqHeight;

		Rect2d wmsPixelRect2d = new Rect2d(wmsRect2d.xmin + xres * reqX, wmsRect2d.ymax - yres * (reqY + 1), wmsRect2d.xmin + xres * (reqX + 1), wmsRect2d.ymax - yres * reqY);

		Logger.info(reqWidth + " " + reqHeight + "   " + reqX + " " + reqY);
		Logger.info(wmsRect2d);
		Logger.info(wmsPixelRect2d);

		Rect2d layerPixelRect2d = wmsPixelRect2d;		
		if(reproject) {			
			SpatialReference wmsSr = GeoUtil.getSpatialReferenceFromEPSG(wmsEPSG);
			SpatialReference layerSr = vectordb.getSpatialReference();
			util.GeoUtil.Transformer transformer = GeoUtil.createCoordinateTransformer(wmsSr, layerSr);
			double[][] points = wmsPixelRect2d.createPoints9();
			transformer.transformWithAxisOrderCorrection(points);
			layerPixelRect2d = Rect2d.ofPoints(points);
		}

		String infoFormat = request.getParameter("INFO_FORMAT");
		switch(infoFormat) {
		case "application/json":
		case "application/geo+json":
			VectordbHandler_geometry.handle_GetFeatureInfoJSON(vectordb, layerPixelRect2d, request, response);
			break;
		case "text/xml":
		default:
			VectordbHandler_wfs.handle_GetFeature(vectordb, layerPixelRect2d, request, response);
			break;
		}
	}

	private void handle_GetLegendGraphic(VectorDB vectordb, Request request, Response response) throws IOException {
		Logger.info("GetLegendGraphic");

		Style style = vectordb.getStyle();
		if(style == null) {
			style = Renderer.STYLE_DEFAULT;
		}
		ImageBufferARGB image = createLegend(vectordb, false, style, null);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_PNG);
		image.writePngCompressed(response.getOutputStream());		
	}

	private ImageBufferARGB createLegend(VectorDB vectordb, boolean crop, Style style, Interruptor interruptor) {				
		int height = getHeight();

		ImageBufferARGB image = new ImageBufferARGB(200, height);
		Graphics2D gc = image.bufferedImage.createGraphics();
		gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gc.setColor(Color.DARK_GRAY);

		Font font = new Font("Arial", Font.PLAIN, 12);
		gc.setFont(font);

		gc.setColor(Color.BLACK);

		gc.drawString("Uniform style", 0, 20);

		int y = 20;
		style.drawImgPolygon(gc, new int[] {20, 160, 160, 20}, new int[] {y + 10, y + 10, y + 20, y + 20}, 4);

		gc.dispose();
		return image;
	}
}