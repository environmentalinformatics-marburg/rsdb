package server.api.postgis;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import pointcloud.Rect2d;
import postgis.PostgisLayer;
import postgis.PostgisLayer.PostgisColumn;
import util.GeoUtil;
import util.IndentedXMLStreamWriter;
import util.Timer;
import util.Web;
import util.XmlUtil;
import util.collections.array.ReadonlyArray;

public class PostgisHandler_wfs {

	public void handle(PostgisLayer postgisLayer, String target, Request request, Response response, UserIdentity userIdentity) {
		String reqParam = Web.getLastString(request, "REQUEST", null);
		if(reqParam == null) {
			reqParam = Web.getLastString(request, "Request", null);
		}
		if(reqParam == null) {
			reqParam = Web.getLastString(request, "request", null);
		}
		if(reqParam == null) {
			reqParam = "GetCapabilities";
		}

		try {
			switch (reqParam) {
			case "GetCapabilities":
				handle_GetCapabilities(postgisLayer, request, response);
				break;
			case "DescribeFeatureType":
				handle_DescribeFeatureType(postgisLayer, request, response);
				break;
			case "GetFeature":
				Timer.start("GetFeature");
				handle_GetFeature(postgisLayer, request, response);
				Logger.info(Timer.stop("GetFeature"));
				break;			
			default:
				throw new RuntimeException("unknown request " + reqParam);
			}		
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handle_GetCapabilities(PostgisLayer postgisLayer, Request request, Response response) throws IOException {
		String requestUrl = request.getRequestURL().toString() + "?rsdb";
		response.setContentType(Web.MIME_XML);	
		PrintWriter out = response.getWriter();	
		try {
			XmlUtil.writeXML(out, doc -> xmlGetCapabilities(postgisLayer, doc, requestUrl));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	private Node xmlGetCapabilities(PostgisLayer postgisLayer, Document doc, String requestUrl) {
		//Element rootElement = doc.createElementNS("http://www.opengis.net/wfs", "WFS_Capabilities");
		Element rootElement = doc.createElement("wfs:WFS_Capabilities");
		rootElement.setAttribute("version", "1.1.0");
		rootElement.setAttribute("xmlns:ows", "http://www.opengis.net/ows");
		rootElement.setAttribute("xmlns:wfs", "http://www.opengis.net/wfs");
		rootElement.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		addOperationsMetadata(rootElement, requestUrl);
		addFeatureTypeList(rootElement, postgisLayer);		
		return rootElement;
	}

	private static void addFeatureTypeList(Element rootElement, PostgisLayer postgisLayer) {
		Element eFeatureTypeList = XmlUtil.addElement(rootElement, "wfs:FeatureTypeList");
		Element eFeatureType = XmlUtil.addElement(eFeatureTypeList, "wfs:FeatureType");
		XmlUtil.addElement(eFeatureType, "wfs:Name", postgisLayer.name);
		XmlUtil.addElement(eFeatureType, "wfs:Title", postgisLayer.name);
		int epsg = postgisLayer.getEPSG();
		XmlUtil.addElement(eFeatureType, "wfs:DefaultSRS", "EPSG:" + epsg);
		tryAdd_WGS84BoundingBox(eFeatureType, postgisLayer);
	}

	private static void tryAdd_WGS84BoundingBox(Element eFeatureType, PostgisLayer postgisLayer) {	 
		Rect2d wgs84Rect = GeoUtil.toWGS84(postgisLayer.getEPSG(), postgisLayer.getExtent());
		if(wgs84Rect == null) {
			return;
		}
		if(wgs84Rect.isFinite()) {
			Element eWGS84BoundingBox = XmlUtil.addElement(eFeatureType, "ows:WGS84BoundingBox");
			XmlUtil.addElement(eWGS84BoundingBox, "ows:LowerCorner", "" + wgs84Rect.xmin + " " + wgs84Rect.ymin);
			XmlUtil.addElement(eWGS84BoundingBox, "ows:UpperCorner", "" + wgs84Rect.xmax + " " + wgs84Rect.ymax);
		}		
	}

	private static void addOperationsMetadata(Element rootElement, String requestUrl) {
		Element eOperationsMetadata = XmlUtil.addElement(rootElement, "ows:OperationsMetadata");

		Element eOperationGetCapabilities = XmlUtil.addElement(eOperationsMetadata, "ows:Operation");
		eOperationGetCapabilities.setAttribute("name", "GetCapabilities");
		Element eOperationGetCapabilitiesDCP = XmlUtil.addElement(eOperationGetCapabilities, "ows:DCP");
		Element eOperationGetCapabilitiesHTTP = XmlUtil.addElement(eOperationGetCapabilitiesDCP, "ows:HTTP");
		Element eOperationGetCapabilitiesGet = XmlUtil.addElement(eOperationGetCapabilitiesHTTP, "ows:Get");
		eOperationGetCapabilitiesGet.setAttribute("xlink:href", requestUrl);

		Element eOperationDescribeFeatureType = XmlUtil.addElement(eOperationsMetadata, "ows:Operation");
		eOperationDescribeFeatureType.setAttribute("name", "DescribeFeatureType");
		Element eOperationDescribeFeatureTypeDCP = XmlUtil.addElement(eOperationDescribeFeatureType, "ows:DCP");
		Element eOperationDescribeFeatureTypeHTTP = XmlUtil.addElement(eOperationDescribeFeatureTypeDCP, "ows:HTTP");
		Element eOperationDescribeFeatureTypeGet = XmlUtil.addElement(eOperationDescribeFeatureTypeHTTP, "ows:Get");
		eOperationDescribeFeatureTypeGet.setAttribute("xlink:href", requestUrl);

		Element eOperationGetFeature = XmlUtil.addElement(eOperationsMetadata, "ows:Operation");
		eOperationGetFeature.setAttribute("name", "GetFeature");
		Element eOperationGetFeatureDCP = XmlUtil.addElement(eOperationGetFeature, "ows:DCP");
		Element eOperationGetFeatureHTTP = XmlUtil.addElement(eOperationGetFeatureDCP, "ows:HTTP");
		Element eOperationGetFeatureGet = XmlUtil.addElement(eOperationGetFeatureHTTP, "ows:Get");
		eOperationGetFeatureGet.setAttribute("xlink:href", requestUrl);
	}

	private void handle_DescribeFeatureType(PostgisLayer postgisLayer, Request request, Response response) throws IOException {
		response.setContentType("text/xml; subtype=gml/3.1.1; charset=UTF-8");
		PrintWriter out = response.getWriter();	
		try {
			XmlUtil.writeXML(out, doc -> xmlDescribeFeatureType(postgisLayer, doc));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	private Node xmlDescribeFeatureType(PostgisLayer postgisLayer, Document doc) {
		Element rootElement = doc.createElementNS("http://www.w3.org/2001/XMLSchema", "schema");
		rootElement.setAttribute("version", "0.1");
		rootElement.setAttribute("xmlns:gml", "http://www.opengis.net/gml");

		Element eimport = XmlUtil.addElement(rootElement, "import");
		eimport.setAttribute("namespace", "http://www.opengis.net/gml");
		eimport.setAttribute("schemaLocation", "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");

		Element eelement = XmlUtil.addElement(rootElement, "element");		
		eelement.setAttribute("name", postgisLayer.name);
		eelement.setAttribute("substitutionGroup", "gml:_Feature");

		Element ecomplexType = XmlUtil.addElement(eelement, "complexType");
		Element ecomplexContent = XmlUtil.addElement(ecomplexType, "complexContent");
		Element eextension = XmlUtil.addElement(ecomplexContent, "extension");
		eextension.setAttribute("base", "gml:AbstractFeatureType");
		Element esequence = XmlUtil.addElement(eextension, "sequence");

		Element esequenceElement1 = XmlUtil.addElement(esequence, "element");
		esequenceElement1.setAttribute("name", postgisLayer.primaryGeometryColumn);
		esequenceElement1.setAttribute("type", "gml:GeometryPropertyType");

		for(PostgisColumn field : postgisLayer.fields) {
			Element esequenceElement2 = XmlUtil.addElement(esequence, "element");
			esequenceElement2.setAttribute("name", field.name);			
			esequenceElement2.setAttribute("type", "string"); // TODO
		}	

		return rootElement;
	}

	private void handle_GetFeature(PostgisLayer postgisLayer, Request request, Response response) throws IOException {
		String bboxParam = request.getParameter("BBOX");
		Rect2d rect2d = null;
		if(bboxParam != null) {
			String[] bbox = bboxParam.split(",");
			rect2d = Rect2d.parseBbox(bbox);
		}

		int maxFeatures = Integer.MAX_VALUE;
		String maxfeaturesParam = request.getParameter("MAXFEATURES");
		if(maxfeaturesParam != null) {
			maxFeatures = Integer.parseInt(maxfeaturesParam);			
		}

		response.setContentType("text/xml; subtype=gml/3.1.1; charset=UTF-8");
		PrintWriter out = response.getWriter();	
		try {
			xmlGetFeatureStream(postgisLayer, out, rect2d, maxFeatures);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}

	public static void xmlGetFeatureStream(PostgisLayer postgisLayer, Writer out, Rect2d rect2d, int maxFeatures) throws XMLStreamException, SQLException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		factory.setProperty("escapeCharacters", false); // needed for direct GML geometry output
		XMLStreamWriter xmlWriterInner = factory.createXMLStreamWriter(out);
		XMLStreamWriter xmlWriter = new IndentedXMLStreamWriter(xmlWriterInner);
		xmlWriter.writeStartDocument();
		xmlWriter.writeStartElement("wfs:FeatureCollection");
		xmlWriter.writeDefaultNamespace("https://environmentalinformatics-marburg.github.io/rsdb");
		xmlWriter.writeNamespace("wfs", "http://www.opengis.net/wfs");
		xmlWriter.writeNamespace("gml", "http://www.opengis.net/gml");	

		ResultSet rs = postgisLayer.queryGMLWithFields(rect2d, false, maxFeatures);
		int countFeatures = 0;
		final ReadonlyArray<PostgisColumn> fields = postgisLayer.fields;
		final int fieldsLen = fields.size();
		while(countFeatures < maxFeatures && rs.next()) {
			countFeatures++;
			//Logger.info("next " + countFeatures);

			xmlWriter.writeStartElement("gml:featureMember");
			xmlWriter.writeStartElement(postgisLayer.name);

			for (int i = 0; i < fieldsLen; i++) {
				PostgisColumn field = fields.get(i);
				String fieldValue = rs.getString(i + 2);
				if(fieldValue != null) {
					xmlWriter.writeStartElement(field.name);
					String escapedFieldValue = XmlUtil.encodeXML(fieldValue);
					xmlWriter.writeCharacters(escapedFieldValue);
					xmlWriter.writeEndElement(); // fieldName
				}
			}

			xmlWriter.writeStartElement(postgisLayer.primaryGeometryColumn); // geometry after properties for better properties reading by qgis
			String gml = rs.getString(1);
			xmlWriter.writeCharacters(gml);
			xmlWriter.writeEndElement(); // postgisLayer.geoColumnName

			xmlWriter.writeEndElement(); // FEATURE_NAME
			xmlWriter.writeEndElement(); // gml:featureMember
		}

		xmlWriter.writeEndElement(); // wfs:FeatureCollection
		xmlWriter.writeEndDocument();
		xmlWriter.close();
	}
}
