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

import broker.PostgisLayer;
import broker.PostgisLayer.PostgisColumn;
import pointcloud.Rect2d;
import util.IndentedXMLStreamWriter;
import util.Timer;
import util.Web;
import util.XmlUtil;

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
		response.setContentType(Web.MIME_XML);	
		PrintWriter out = response.getWriter();	
		try {
			XmlUtil.writeXML(out, doc -> xmlGetCapabilities(postgisLayer, doc));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	private Node xmlGetCapabilities(PostgisLayer postgisLayer, Document doc) {
		Element rootElement = doc.createElementNS("http://www.opengis.net/wfs", "WFS_Capabilities");
		rootElement.setAttribute("version", "1.1.0");		
		Element eFeatureTypeList = XmlUtil.addElement(rootElement, "FeatureTypeList");
		Element eFeatureType = XmlUtil.addElement(eFeatureTypeList, "FeatureType");
		XmlUtil.addElement(eFeatureType, "Name", postgisLayer.name);
		XmlUtil.addElement(eFeatureType, "Title", postgisLayer.name);

		int epsg = postgisLayer.getEPSG();
		XmlUtil.addElement(eFeatureType, "DefaultSRS", "EPSG:" + epsg);
		return rootElement;
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

		response.setContentType("text/xml; subtype=gml/3.1.1; charset=UTF-8");
		PrintWriter out = response.getWriter();	
		try {
			xmlGetFeatureStream(postgisLayer, out, rect2d);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}

	private void xmlGetFeatureStream(PostgisLayer postgisLayer, Writer out, Rect2d rect2d) throws XMLStreamException, SQLException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		factory.setProperty("escapeCharacters", false);
		XMLStreamWriter xmlWriterInner = factory.createXMLStreamWriter(out);
		XMLStreamWriter xmlWriter = new IndentedXMLStreamWriter(xmlWriterInner);
		xmlWriter.writeStartDocument();
		xmlWriter.writeStartElement("wfs:FeatureCollection");
		xmlWriter.writeNamespace("wfs", "http://www.opengis.net/wfs");
		xmlWriter.writeNamespace("gml", "http://www.opengis.net/gml");	
		
		ResultSet rs = postgisLayer.queryGML(rect2d);		
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
