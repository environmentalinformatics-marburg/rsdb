package server.api.postgis;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.osr.CoordinateTransformation;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ar.com.hjg.pngj.PngjOutputException;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.PostgisLayer;
import postgis.PostgisLayer.PostgisColumn;
import util.GeoUtil;
import util.IndentedXMLStreamWriter;
import util.Interruptor;
import util.Web;
import util.XmlUtil;
import util.image.ImageBufferARGB;
import vectordb.style.BasicStyle;
import vectordb.style.Style;

public class PostgisHandler_wms {
	
	private static class SessionLayerKey {
		public final long session;
		public final String layer;
		public SessionLayerKey(long session, String layer) {
			this.session = session;
			this.layer = layer;
		}
		@Override
		public int hashCode() {
			return Objects.hash(layer, session);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SessionLayerKey other = (SessionLayerKey) obj;
			return session == other.session && Objects.equals(layer, other.layer);
		}
	}

	private ConcurrentHashMap<SessionLayerKey, Interruptor> sessionMap = new ConcurrentHashMap<SessionLayerKey, Interruptor>();

	public void handle(PostgisLayer postgisLayer, String target, Request request, Response response, UserIdentity userIdentity) {

		request.setHandled(true);

		String reqParam = Web.getLastString(request, "Request", null);
		if(reqParam == null) {
			reqParam = Web.getLastString(request, "REQUEST", null);
		}
		if(reqParam == null) {
			reqParam = Web.getLastString(request, "request", null);
		}
		if(reqParam == null) {
			reqParam = "GetCapabilities";
		}

		try {
			switch (reqParam) {
			case "GetMap":
				handle_GetMap(postgisLayer, request, response);
				break;
			case "GetCapabilities":
				handle_GetCapabilities(postgisLayer, request, response);
				break;
			case "GetFeatureInfo":
				handle_GetFeatureInfo(postgisLayer, request, response);
				break;
			default:
				Logger.error("unknown request "+reqParam);
				return;
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
			} else {
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

	private void handle_GetCapabilities(PostgisLayer postgisLayer, Request request, Response response) throws IOException {
		String requestUrl = request.getRequestURL().toString();
		response.setContentType(Web.MIME_XML);	
		PrintWriter out = response.getWriter();	
		try {
			XmlUtil.writeXML(out, doc -> xmlGetCapabilities(postgisLayer, doc, requestUrl));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	private Element xmlGetCapabilities(PostgisLayer postgisLayer, Document doc, String requestUrl) {
		Element rootElement = doc.createElementNS("http://www.opengis.net/wms", "WMS_Capabilities");
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");

		Element eService = XmlUtil.addElement(rootElement, "Service");
		XmlUtil.addElement(eService, "Name", "WMS");
		XmlUtil.addElement(eService, "Title", "Remote Sensing Database");
		XmlUtil.addElement(eService, "Abstract", "WMS service"); 
		XmlUtil.addElement(eService, "LayerLimit", "1"); // only one layer should be specified at GetMap LAYERS parameter, ignored by (most?) clients

		Element eCapability = XmlUtil.addElement(rootElement, "Capability");
		addRequest(eCapability, requestUrl);
		addRootLayer(postgisLayer, eCapability, postgisLayer.name, postgisLayer.name);

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
		XmlUtil.addElement(eGetFeatureInfo, "Format", "text/xml");
		Element eGetFeatureInfoDCPType = XmlUtil.addElement(eGetFeatureInfo, "DCPType");
		Element eGetFeatureInfoDCPTypeHTTP = XmlUtil.addElement(eGetFeatureInfoDCPType, "HTTP");
		Element eGetFeatureInfoDCPTypeHTTPGet = XmlUtil.addElement(eGetFeatureInfoDCPTypeHTTP, "Get");		
		Element eGetFeatureInfoDCPTypeHTTPGetOnlineResource = XmlUtil.addElement(eGetFeatureInfoDCPTypeHTTPGet, "OnlineResource");
		eGetFeatureInfoDCPTypeHTTPGetOnlineResource.setAttribute("xlink:type", "simple");
		eGetFeatureInfoDCPTypeHTTPGetOnlineResource.setAttribute("xlink:href", requestUrl);
	}

	private static void addRootLayer(PostgisLayer postgisLayer, Element eCapability, String name, String title) {
		Element eRootLayer = XmlUtil.addElement(eCapability, "Layer");
		eRootLayer.setAttribute("queryable", "1");

		int layerEPSG = postgisLayer.getEPSG();
		boolean hasLayerCRS = layerEPSG > 0;

		XmlUtil.addElement(eRootLayer, "Name", name);
		XmlUtil.addElement(eRootLayer, "Title", title);

		Rect2d layerRect = postgisLayer.getExtent();

		if(hasLayerCRS) {
			tryAdd_GeographicBoundingBox(layerRect, layerEPSG, eRootLayer);
		}

		if(hasLayerCRS) {
			XmlUtil.addElement(eRootLayer, "CRS", "EPSG:" + layerEPSG);
		}
		Element eBoundingBox = XmlUtil.addElement(eRootLayer, "BoundingBox");
		if(hasLayerCRS) {
			eBoundingBox.setAttribute("CRS", "EPSG:" + layerEPSG);
		}
		eBoundingBox.setAttribute("minx", "" + layerRect.xmin);
		eBoundingBox.setAttribute("miny", "" + layerRect.ymin);
		eBoundingBox.setAttribute("maxx", "" + layerRect.xmax);
		eBoundingBox.setAttribute("maxy", "" + layerRect.ymax);
	}

	private static void tryAdd_GeographicBoundingBox(Rect2d layerRect, int layerEPSG, Element eRootLayer) {
		try {
			CoordinateTransformation ct = GeoUtil.getCoordinateTransformation(layerEPSG, GeoUtil.EPSG_WGS84);
			if(ct == null) {
				return;
			}
			double[][] rePoints = layerRect.createPoints9();
			ct.TransformPoints(rePoints);
			Rect2d wgs84Rect = Rect2d.ofPoints(rePoints);
			if(wgs84Rect.isFinite()) {
				Element eEX_GeographicBoundingBox = XmlUtil.addElement(eRootLayer, "EX_GeographicBoundingBox");		
				XmlUtil.addElement(eEX_GeographicBoundingBox, "westBoundLongitude", "" + wgs84Rect.xmin);
				XmlUtil.addElement(eEX_GeographicBoundingBox, "eastBoundLongitude", "" + wgs84Rect.xmax);		
				XmlUtil.addElement(eEX_GeographicBoundingBox, "southBoundLatitude", "" + wgs84Rect.ymin);
				XmlUtil.addElement(eEX_GeographicBoundingBox, "northBoundLatitude", "" + wgs84Rect.ymax);
			}			
		} catch(Exception e) {
			Logger.warn(e);
		}
	}

	private void handle_GetMap(PostgisLayer postgisLayer, Request request, Response response) throws IOException {

		long session = Web.getLong(request, "session", -1);
		long sessionCnt = Web.getLong(request, "cnt", -1);
		Interruptor interruptor = null;
		if(session >= 0 && sessionCnt >= 0) {
			interruptor = new Interruptor(sessionCnt);			
			while(true) {
				SessionLayerKey sessionLayerKey = new SessionLayerKey(session, postgisLayer.name);
				Interruptor prevInterruptor = sessionMap.get(sessionLayerKey);
				if(prevInterruptor == null) {
					if(sessionMap.putIfAbsent(sessionLayerKey, interruptor) == null) {
						break; // new value set
					}
				} else {
					if(prevInterruptor.id < interruptor.id) {
						if(sessionMap.replace(sessionLayerKey, prevInterruptor, interruptor)) {
							prevInterruptor.interrupted = true;
							//Logger.info("set interrupted " + prevInterruptor.id + "    " + interruptor.id);
							break;  // new value set and prev interrupted
						}
					} else {
						if(prevInterruptor.id == interruptor.id) {
							Logger.warn("interrupted same id " + interruptor.id);
						}
						Logger.info("interrupted (not started) " + interruptor.id);
						return;
					}
				}
			}			
		}

		String[] bbox = request.getParameter("BBOX").split(",");
		Rect2d wmsRect = Rect2d.parseBbox(bbox);
		int width = Web.getInt(request, "WIDTH");
		int height = Web.getInt(request, "HEIGHT");		

		//Style style = Renderer.STYLE_DEFAULT;
		Style style = new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 0, 100), new Color(0, 150, 0, 100));
		ImageBufferARGB image = PostgisHandler_image_png.render(postgisLayer, wmsRect, false, width, height, style, interruptor);
		if(Interruptor.isInterrupted(interruptor)) {
			response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
		} else {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_PNG);
			image.writePngCompressed(response.getOutputStream());
		}
	}

	private void handle_GetFeatureInfo(PostgisLayer postgisLayer, Request request, Response response) throws IOException {
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

		response.setContentType("text/xml; subtype=gml/3.1.1; charset=UTF-8");
		PrintWriter out = response.getWriter();	
		try {
			xmlGetFeatureStream(postgisLayer, out, pixelRect2d);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}

	private void xmlGetFeatureStream(PostgisLayer postgisLayer, Writer out, Rect2d pixelRect2d) throws XMLStreamException, SQLException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		factory.setProperty("escapeCharacters", false);
		XMLStreamWriter xmlWriterInner = factory.createXMLStreamWriter(out);
		XMLStreamWriter xmlWriter = new IndentedXMLStreamWriter(xmlWriterInner);
		xmlWriter.writeStartDocument();
		xmlWriter.writeStartElement("wfs:FeatureCollection");
		xmlWriter.writeNamespace("wfs", "http://www.opengis.net/wfs");
		xmlWriter.writeNamespace("gml", "http://www.opengis.net/gml");	

		ResultSet rs = postgisLayer.queryGMLWithFields(pixelRect2d, false);		
		while(rs.next()) {
			xmlWriter.writeStartElement("gml:featureMember");
			xmlWriter.writeStartElement(postgisLayer.name);
			xmlWriter.writeStartElement(postgisLayer.primaryGeometryColumn);
			String gml = rs.getString(1);
			xmlWriter.writeCharacters(gml);
			xmlWriter.writeEndElement(); // postgisLayer.geoColumnName			

			for (int i = 0; i < postgisLayer.fields.size(); i++) {
				PostgisColumn field = postgisLayer.fields.get(i);
				String fieldValue = rs.getString(i + 2);
				xmlWriter.writeStartElement(field.name);
				xmlWriter.writeCharacters(fieldValue);
				xmlWriter.writeEndElement(); // fieldName
			}
			xmlWriter.writeEndElement(); // FEATURE_NAME
			xmlWriter.writeEndElement(); // gml:featureMember
		}

		xmlWriter.writeEndElement(); // wfs:FeatureCollection
		xmlWriter.writeEndDocument();
		xmlWriter.close();
	}
}
