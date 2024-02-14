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


import org.tinylog.Logger;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.ogr.DataSource;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ar.com.hjg.pngj.PngjOutputException;
import broker.Broker;
import pointcloud.Rect2d;
import server.api.postgis.PostgisHandler_wms;
import util.GeoUtil;
import util.Web;
import util.XmlUtil;
import util.image.ImageBufferARGB;
import vectordb.Renderer;
import vectordb.VectorDB;

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
		default:
			Logger.error("unknown request " + reqParam);
			return;
		}		
	}

	public void handle_GetMap(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			int width = Web.getInt(request, "WIDTH");
			int height = Web.getInt(request, "HEIGHT");
			String crs = Web.getString(request, "CRS");
			util.GeoUtil.Transformer transformer = null;

			SpatialReference srcSr = vectordb.getSpatialReference();
			if(srcSr != null && crs != null && crs.startsWith("EPSG:")) {
				SpatialReference dstSr = null;
				int epsg = Integer.parseInt(crs.substring(5));
				if(epsg > 0) {
					if(epsg == GeoUtil.EPSG_WEB_MERCATOR) {
						dstSr = GeoUtil.WEB_MERCATOR_SPATIAL_REFERENCE;
					} else {
						dstSr = GeoUtil.getSpatialReferenceFromEPSG(epsg);		
					}
				}
				if(dstSr != null) {
					if(dstSr.IsSame(srcSr) == 0) {
						Logger.info("transform");
						transformer = new GeoUtil.Transformer(srcSr, dstSr);
					}
				}
			}

			String[] bbox = request.getParameter("BBOX").split(",");
			Rect2d wmsRect = Rect2d.parse(bbox[0], bbox[1], bbox[2], bbox[3]);
			ImageBufferARGB image = null;
			DataSource datasource = vectordb.getDataSource();
			try {		
				image = Renderer.render(datasource, vectordb, wmsRect, width, height, transformer, vectordb.getStyle());
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
		addRootLayer(vectordb, eCapability, vectordb.getName(), vectordb.getName());

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

	private void addRootLayer(VectorDB vectordb, Element eCapability, String name, String title) {
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
	}

	public void handle_GetFeatureInfo(VectorDB vectordb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String bboxParam = request.getParameter("BBOX");
		Rect2d rect2d = null;
		if(bboxParam != null) {
			String[] bbox = bboxParam.split(",");
			rect2d = Rect2d.parseBbox(bbox);
		}		
		int reqWidth = Web.getInt(request, "WIDTH");
		int reqHeight = Web.getInt(request, "HEIGHT");		
		int reqX = Web.getInt(request, "I");
		int reqY = Web.getInt(request, "J");

		double xres = rect2d.width() / reqWidth;
		double yres = rect2d.height() / reqHeight;

		Rect2d pixelRect2d = new Rect2d(rect2d.xmin + xres * reqX, rect2d.ymax - yres * (reqY + 1), rect2d.xmin + xres * (reqX + 1), rect2d.ymax - yres * reqY);

		Logger.info(reqWidth + " " + reqHeight + "   " + reqX + " " + reqY);
		Logger.info(rect2d);
		Logger.info(pixelRect2d);

		String infoFormat = request.getParameter("INFO_FORMAT");
		switch(infoFormat) {
		case "application/json":
		case "application/geo+json":
			VectordbHandler_geometry.handle_GetFeatureInfoJSON(vectordb, pixelRect2d, request, response);
			break;
		case "text/xml":
		default:
			VectordbHandler_wfs.handle_GetFeature(vectordb, pixelRect2d, request, response);
			break;
		}
	}
}