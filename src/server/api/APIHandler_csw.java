package server.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import broker.Broker;
import broker.catalog.CatalogKey;
import rasterdb.RasterDB;
import util.Web;
import util.XmlUtil;

public class APIHandler_csw extends APIHandler {

	public APIHandler_csw(Broker broker) {
		super(broker, "csw");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Logger.info("CSW Request: {}", target);
		UserIdentity userIdentity = Web.getUserIdentity(request);
		request.setHandled(true);

		String reqParam = Web.getLastString(request, "REQUEST", null);
		if (reqParam == null) reqParam = Web.getLastString(request, "Request", null);
		if (reqParam == null) reqParam = Web.getLastString(request, "request", null);

		String requestBody = "";
		try {
			if ("POST".equalsIgnoreCase(request.getMethod())) {
				String encoding = request.getCharacterEncoding();
				if (encoding == null) encoding = "UTF-8";
				try (BufferedReader reader = request.getReader()) {
					requestBody = reader.lines().collect(Collectors.joining("\n"));
				}
			}
		} catch (IOException e) {
			Logger.warn("request error ", e);
		}

		String operation = extractOperationFromXml(requestBody);
		if (operation == null) {
			operation = reqParam;
		}

		try {
			if ("GetCapabilities".equalsIgnoreCase(operation)) {
				response.setContentType("application/xml; charset=UTF-8");
				PrintWriter out = response.getWriter();

				XmlUtil.writeXML(out, doc -> xmlGetCapabilities(doc, request));
			} else if ("GetRecords".equalsIgnoreCase(operation)) {
				response.setContentType("application/xml; charset=UTF-8");
				PrintWriter out = response.getWriter();

				XmlUtil.writeXML(out, doc -> xmlGetRecordsResponse(doc, userIdentity, request));
			} else {
				Logger.warn("Unsupported CSW request: {}. Body: {}", operation, requestBody);
				response.setStatus(400);
				response.getWriter().write("Unsupported CSW request: " + operation);
			}
		} catch (Exception e) {
			Logger.error("CSW response generation failed", e);
			response.setStatus(500);
			response.getWriter().write("Internal Server Error");
		}
	}

	private String getBaseURL(Request request) {
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
	}

	private String extractOperationFromXml(String xml) {
		if (xml == null || xml.trim().isEmpty()) return null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xml)));
			Element root = doc.getDocumentElement();
			return root.getLocalName();
		} catch (Exception e) {
			Logger.debug("xml error: " + e.getMessage());
			return null;
		}
	}

	private Node xmlGetCapabilities(Document doc, Request request) {
		String baseUrl = getBaseURL(request);
		String serviceUrl = baseUrl + "/csw";

		Element root = doc.createElementNS("http://www.opengis.net/cat/csw/2.0.2", "csw:Capabilities");
		root.setAttribute("version", "2.0.2");
		root.setAttribute("xmlns:csw", "http://www.opengis.net/cat/csw/2.0.2");
		root.setAttribute("xmlns:ows", "http://www.opengis.net/ows");
		root.setAttribute("xmlns:gml", "http://www.opengis.net/gml");
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute("xsi:schemaLocation", "http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd");

		Element eServiceIdentification = XmlUtil.addElement(root, "ows:ServiceIdentification");
		XmlUtil.addElement(eServiceIdentification, "ows:Title", "Generic Geospatial Catalog Service");
		XmlUtil.addElement(eServiceIdentification, "ows:Abstract", "This is CSW service.");
		XmlUtil.addElement(eServiceIdentification, "ows:ServiceType", "CSW");
		XmlUtil.addElement(eServiceIdentification, "ows:ServiceTypeVersion", "2.0.2");
		XmlUtil.addElement(eServiceIdentification, "ows:Fees");
		XmlUtil.addElement(eServiceIdentification, "ows:AccessConstraints", "none");

		Element eServiceProvider = XmlUtil.addElement(root, "ows:ServiceProvider");
		XmlUtil.addElement(eServiceProvider, "ows:ProviderName", "Generic Provider");
		Element eServiceContact = XmlUtil.addElement(eServiceProvider, "ows:ServiceContact");
		Element eContactInfo = XmlUtil.addElement(eServiceContact, "ows:ContactInfo");
		Element eAddress = XmlUtil.addElement(eContactInfo, "ows:Address");
		XmlUtil.addElement(eAddress, "ows:ElectronicMailAddress", "contact@example.com");
		XmlUtil.addElement(eServiceContact, "ows:Role", "pointOfContact");

		Element eOperationsMetadata = XmlUtil.addElement(root, "ows:OperationsMetadata");
		addCswOperation(eOperationsMetadata, "GetCapabilities", serviceUrl);
		addCswOperation(eOperationsMetadata, "GetRecords", serviceUrl);
		addCswOperation(eOperationsMetadata, "GetRecordById", serviceUrl);

		Element eParamService = XmlUtil.addElement(eOperationsMetadata, "ows:Parameter");
		eParamService.setAttribute("name", "service");
		XmlUtil.addElement(eParamService, "ows:Value", "http://www.opengis.net/cat/csw/2.0.2");

		Element eParamVersion = XmlUtil.addElement(eOperationsMetadata, "ows:Parameter");
		eParamVersion.setAttribute("name", "version");
		XmlUtil.addElement(eParamVersion, "ows:Value", "2.0.2");

		return root;
	}

	private static void addCswOperation(Element parent, String name, String href) {
		Element eOperation = XmlUtil.addElement(parent, "ows:Operation");
		eOperation.setAttribute("name", name);
		Element eDCP = XmlUtil.addElement(eOperation, "ows:DCP");
		Element eHTTP = XmlUtil.addElement(eDCP, "ows:HTTP");
		Element eGet = XmlUtil.addElement(eHTTP, "ows:Get");
		eGet.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		eGet.setAttribute("xlink:href", href);
		Element ePost = XmlUtil.addElement(eHTTP, "ows:Post");
		ePost.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		ePost.setAttribute("xlink:href", href);

		if ("GetRecords".equals(name)) {
			Element eParamResultType = XmlUtil.addElement(eOperation, "ows:Parameter");
			eParamResultType.setAttribute("name", "resultType");
			XmlUtil.addElement(eParamResultType, "ows:Value", "hits");
			XmlUtil.addElement(eParamResultType, "ows:Value", "results");

			Element eParamOutputFormat = XmlUtil.addElement(eOperation, "ows:Parameter");
			eParamOutputFormat.setAttribute("name", "outputFormat");
			XmlUtil.addElement(eParamOutputFormat, "ows:Value", "application/xml");

			Element eParamTypeNames = XmlUtil.addElement(eOperation, "ows:Parameter");
			eParamTypeNames.setAttribute("name", "typeNames");
			XmlUtil.addElement(eParamTypeNames, "ows:Value", "csw:Record");

			Element eConstraint = XmlUtil.addElement(eOperation, "ows:Constraint");
			eConstraint.setAttribute("name", "PostEncoding");
			XmlUtil.addElement(eConstraint, "ows:Value", "XML");
		}
	}

	private Node xmlGetRecordsResponse(Document doc, UserIdentity userIdentity, Request request) {
		String baseUrl = getBaseURL(request);

		Element root = doc.createElementNS("http://www.opengis.net/cat/csw/2.0.2", "csw:GetRecordsResponse");
		root.setAttribute("xmlns:csw", "http://www.opengis.net/cat/csw/2.0.2");
		root.setAttribute("xmlns:ows", "http://www.opengis.net/ows");
		root.setAttribute("xmlns:dct", "http://purl.org/dc/terms/");
		root.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute("xsi:schemaLocation", "http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd");

		Element eSearchStatus = XmlUtil.addElement(root, "csw:SearchStatus");
		eSearchStatus.setAttribute("timestamp", java.time.Instant.now().toString());

		List<Element> records = new ArrayList<>();

		for (String layerName : broker.getRasterdbNames()) {
			RasterDB rasterdb = broker.getRasterdb(layerName);
			if (rasterdb.isAllowed(userIdentity)) {
				records.add(createRecord(doc, layerName, "RasterDB", "WCS", baseUrl));
				records.add(createRecord(doc, layerName, "RasterDB", "WMS", baseUrl));
			}
		}

		broker.catalog.getSorted(CatalogKey.TYPE_VECTORDB, userIdentity).forEach(entry -> {
			records.add(createRecord(doc, entry.name, "VectorDB", "WFS", baseUrl));
			records.add(createRecord(doc, entry.name, "VectorDB", "WMS", baseUrl));
		});

		Element eSearchResults = XmlUtil.addElement(root, "csw:SearchResults");
		eSearchResults.setAttribute("numberOfRecordsMatched", String.valueOf(records.size()));
		eSearchResults.setAttribute("numberOfRecordsReturned", String.valueOf(records.size()));
		eSearchResults.setAttribute("elementSet", "full");

		for (Element record : records) {
			eSearchResults.appendChild(record);
		}

		return root;
	}

	private Element createRecord(Document doc, String layerName, String dbType, String serviceType, String baseUrl) {
		Element eRecord = doc.createElementNS("http://www.opengis.net/cat/csw/2.0.2", "csw:Record");
		
		XmlUtil.addElement(eRecord, "dc:identifier", layerName + "__" + serviceType);

		XmlUtil.addElement(eRecord, "dc:title", layerName + " - " + serviceType);
		
		XmlUtil.addElement(eRecord, "dc:type", "dataset");
		
		String format = "application/xml";
		if ("WCS".equals(serviceType)) format = "image/tiff";
		if ("WMS".equals(serviceType)) format = "image/png";
		XmlUtil.addElement(eRecord, "dc:format", format);

		String abstractText = dbType + " Layer: " + layerName;
		XmlUtil.addElement(eRecord, "dct:abstract", abstractText);
		XmlUtil.addElement(eRecord, "dc:description", abstractText);

		XmlUtil.addElement(eRecord, "dc:subject", dbType.toLowerCase());
		XmlUtil.addElement(eRecord, "dc:subject", serviceType.toLowerCase());

		XmlUtil.addElement(eRecord, "dc:rights", "otherRestrictions");

		String basePath = dbType.equals("RasterDB") ? "/rasterdb/" : "/vectordbs/";
		String serviceEndpoint = serviceType.toLowerCase();
		String linkHref = baseUrl + basePath + layerName + "/" + serviceEndpoint;

		Element eURI = XmlUtil.addElement(eRecord, "dc:URI");
		eURI.setAttribute("protocol", "OGC:" + serviceType);
		eURI.setAttribute("name", layerName + " - " + serviceType);
		eURI.setAttribute("description", "Web " + getServiceFullName(serviceType) + " Service");
		eURI.setTextContent(linkHref);

		return eRecord;
	}

	private String getServiceFullName(String type) {
		switch (type) {
			case "WCS": return "Coverage";
			case "WFS": return "Feature";
			case "WMS": return "Map";
			default: return type;
		}
	}
}