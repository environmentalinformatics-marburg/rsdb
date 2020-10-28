package server.api.rasterdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.WarpOptions;
import org.gdal.gdal.gdal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.Broker;
import broker.TimeSlice;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.Rasterizer;
import rasterdb.TimeBand;
import rasterdb.dsl.DSL;
import rasterdb.dsl.ErrorCollector;
import server.api.rasterdb.RequestProcessor.OutputProcessingType;
import server.api.rasterdb.WmsCapabilities.WmsStyle;
import util.Range2d;
import util.ResponseReceiver;
import util.TimeUtil;
import util.Web;
import util.collections.ReadonlyNavigableSetView;
import util.frame.DoubleFrame;
import util.frame.ShortFrame;
import util.image.ImageBufferARGB;
import util.image.Renderer;
import util.tiff.TiffBand;
import util.tiff.TiffWriter;

public class RasterdbMethod_wcs extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_wcs(Broker broker) {
		super(broker, "wcs");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			log.error("no WMS");
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
		case "GetCapabilities":
			handle_GetCapabilities(rasterdb, target, request, response, userIdentity);
			break;
		case "DescribeCoverage":			
			handle_DescribeCoverage(rasterdb, target, request, response, userIdentity);
			break;			
		case "GetCoverage":
			handle_GetCoverage(rasterdb, target, request, response, userIdentity);
			break;			
		default:
			log.error("unknown request "+reqParam);
			return;
		}
	}

	public void handle_GetCapabilities(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType("application/xml");		
		PrintWriter out = response.getWriter();	
		//out.write(cc);
		try {
			xml_root__GetCapabilities(rasterdb, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void handle_DescribeCoverage(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType("application/xml");		
		PrintWriter out = response.getWriter();	
		//out.write(cd);
		try {
			xml_root__DescribeCoverage(rasterdb, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void handle_GetCoverage(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		int width = Web.getInt(request, "WIDTH", -1);
		int height = Web.getInt(request, "HEIGHT", -1);
		
		String[] bbox = request.getParameter("BBOX").split(",");
		double geoXmin = Double.parseDouble(bbox[0]);
		double geoYmin = Double.parseDouble(bbox[1]);
		double geoXmax = Double.parseDouble(bbox[2]);
		double geoYmax = Double.parseDouble(bbox[3]);
		double geoXres = (geoXmax - geoXmin) / width;
		double geoYres = (geoYmax - geoYmin) / height;
		
		ResponseReceiver resceiver = new ResponseReceiver(response);
		
		GeoReference ref = rasterdb.ref();
		Range2d range2d = ref.bboxToRange2d(geoXmin, geoYmin, geoXmax, geoXmax);

		
		/*int timestamp = rasterdb.rasterUnit().timeKeysReadonly().isEmpty() ? 0 : rasterdb.rasterUnit().timeKeysReadonly().last();
		
		BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);
		
		List<TimeBand> processingBands = processor.getTimeBands();
		
		OutputProcessingType outputProcessingType = OutputProcessingType.IDENTITY;	
		
		RequestProcessorBands.processBands(processor, processingBands, outputProcessingType, "tiff", resceiver);*/
		
		TiffWriter tiffWriter = new TiffWriter(width, height, geoXmin, geoYmin, geoXres, geoYres, (short)ref.getEPSG(0));
		short[][] data = new short[height][width];
		for(int y = 0; y < height; y++) {
			short[] dataY = data[y];
			for(int x = 0; x < width; x++) {
				dataY[x] = (short) ((13*y * 29*x) % 31);
			}
		}
		tiffWriter.addTiffBand(TiffBand.ofInt16(data));
		
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/tiff");
		resceiver.setContentLength(tiffWriter.exactSizeOfWriteAuto());
		tiffWriter.writeAuto(new DataOutputStream(resceiver.getOutputStream()));	
		
	}


	private void xml_root__GetCapabilities(RasterDB rasterdb, PrintWriter out) throws ParserConfigurationException, TransformerException {
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
	
	//https://www.wcs.nrw.de/geobasis/wcs_nw_dtk100?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=1.0.0
	static final String cc = "<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>\r\n"
			+ "<WCS_Capabilities\r\n"
			+ "   version=\"1.0.0\" \r\n"
			+ "   updateSequence=\"0\" \r\n"
			+ "   xmlns=\"http://www.opengis.net/wcs\" \r\n"
			+ "   xmlns:xlink=\"http://www.w3.org/1999/xlink\" \r\n"
			+ "   xmlns:gml=\"http://www.opengis.net/gml\" \r\n"
			+ "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
			+ "   xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/wcsCapabilities.xsd\">\r\n"
			+ "<Service>\r\n"
			+ "  <name>MapServer WCS</name>\r\n"
			+ "  <label>WCS NW DTK100</label>\r\n"
			+ "</Service>\r\n"
			+ "<ContentMetadata>\r\n"
			+ "  <CoverageOfferingBrief>\r\n"
			+ "  <metadataLink metadataType=\"TC211\" xlink:type=\"simple\" xlink:href=\"https://apps.geoportal.nrw.de/soapServices/CSWStartup?Service=CSW&amp;Request=GetRecordById&amp;Version=2.0.2&amp;outputSchema=http://www.isotc211.org/2005/gmd&amp;elementSetName=full&amp;id=0bdda2c8-2ac2-46fe-b594-7c82b66d5900\"/>    <name>nw_dtk100_col</name>\r\n"
			+ "    <label>DTK100 FarbeE</label>\r\n"
			+ "    <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\r\n"
			+ "      <gml:pos>5.72499127041 50.150604397469</gml:pos>\r\n"
			+ "      <gml:pos>9.53154249843082 52.6020197509819</gml:pos>\r\n"
			+ "    </lonLatEnvelope>\r\n"
			+ "  </CoverageOfferingBrief>\r\n"
			+ "</ContentMetadata>\r\n"
			+ "</WCS_Capabilities>\r\n"
			+ "";
	
	String NS_URL = "http://www.opengis.net/wcs";
	String NS_XLINK = "http://www.w3.org/1999/xlink";
	String NS_GML = "http://www.opengis.net/gml";

	private Node getCapabilities(RasterDB rasterdb, Document doc) {
		Element rootElement = doc.createElementNS(NS_URL, "WCS_Capabilities");
		rootElement.setAttribute("version", "1.0.0");
		rootElement.setAttribute("xmlns:xlink", NS_XLINK);

		Element eService = addElement(rootElement, "Service");
		addElement(eService, "name", "RSDB WCS");
		addElement(eService, "label", rasterdb.config.getName());


		Element eContentMetadata = addElement(rootElement, "ContentMetadata");
		Element eCoverageOfferingBrief = addElement(eContentMetadata, "CoverageOfferingBrief");
		addElement(eCoverageOfferingBrief, "name", rasterdb.config.getName());
		addElement(eCoverageOfferingBrief, "label", rasterdb.config.getName());

		return rootElement;
	}	
	
	//https://www.wcs.nrw.de/geobasis/wcs_nw_dtk100?SERVICE=WCS&REQUEST=DescribeCoverage&VERSION=1.0.0&COVERAGE=nw_dtk100_col
	static final String cd = "<?xml version='1.0' encoding=\"UTF-8\" ?>\r\n"
			+ "<CoverageDescription\r\n"
			+ "   version=\"1.0.0\" \r\n"
			+ "   updateSequence=\"0\" \r\n"
			+ "   xmlns=\"http://www.opengis.net/wcs\" \r\n"
			+ "   xmlns:xlink=\"http://www.w3.org/1999/xlink\" \r\n"
			+ "   xmlns:gml=\"http://www.opengis.net/gml\" \r\n"
			+ "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
			+ "   xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/describeCoverage.xsd\">\r\n"
			+ "  <CoverageOffering>\r\n"
			+ "  <metadataLink metadataType=\"TC211\" xlink:type=\"simple\" xlink:href=\"https://apps.geoportal.nrw.de/soapServices/CSWStartup?Service=CSW&amp;Request=GetRecordById&amp;Version=2.0.2&amp;outputSchema=http://www.isotc211.org/2005/gmd&amp;elementSetName=full&amp;id=0bdda2c8-2ac2-46fe-b594-7c82b66d5900\"/>  <name>nw_dtk100_col</name>\r\n"
			+ "  <label>DTK100 Farbe</label>\r\n"
			+ "    <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\r\n"
			+ "      <gml:pos>5.72499127041 50.150604397469</gml:pos>\r\n"
			+ "      <gml:pos>9.53154249843082 52.6020197509819</gml:pos>\r\n"
			+ "    </lonLatEnvelope>\r\n"
			+ "    <domainSet>\r\n"
			+ "      <spatialDomain>\r\n"
			+ "        <gml:Envelope srsName=\"EPSG:4326\">\r\n"
			+ "          <gml:pos>5.72499127041 50.150604397469</gml:pos>\r\n"
			+ "          <gml:pos>9.53154249843082 52.6020197509819</gml:pos>\r\n"
			+ "        </gml:Envelope>\r\n"
			+ "        <gml:Envelope srsName=\"EPSG:25832\">\r\n"
			+ "          <gml:pos>278000 5560000</gml:pos>\r\n"
			+ "          <gml:pos>536000 5828000</gml:pos>\r\n"
			+ "        </gml:Envelope>\r\n"
			+ "        <gml:RectifiedGrid dimension=\"2\">\r\n"
			+ "          <gml:limits>\r\n"
			+ "            <gml:GridEnvelope>\r\n"
			+ "              <gml:low>0 0</gml:low>\r\n"
			+ "              <gml:high>51599 53599</gml:high>\r\n"
			+ "            </gml:GridEnvelope>\r\n"
			+ "          </gml:limits>\r\n"
			+ "          <gml:axisName>x</gml:axisName>\r\n"
			+ "          <gml:axisName>y</gml:axisName>\r\n"
			+ "          <gml:origin>\r\n"
			+ "            <gml:pos>278000 5828000</gml:pos>\r\n"
			+ "          </gml:origin>\r\n"
			+ "          <gml:offsetVector>5 0</gml:offsetVector>\r\n"
			+ "          <gml:offsetVector>0 -5</gml:offsetVector>\r\n"
			+ "        </gml:RectifiedGrid>\r\n"
			+ "      </spatialDomain>\r\n"
			+ "    </domainSet>\r\n"
			+ "    <rangeSet>\r\n"
			+ "      <RangeSet>\r\n"
			+ "        <name>Range 1</name>\r\n"
			+ "        <label>My Label</label>\r\n"
			+ "      </RangeSet>\r\n"
			+ "    </rangeSet>\r\n"
			+ "    <supportedCRSs>\r\n"
			+ "      <requestResponseCRSs>EPSG:25832</requestResponseCRSs>\r\n"
			+ "      <nativeCRSs>EPSG:25832</nativeCRSs>\r\n"
			+ "    </supportedCRSs>\r\n"
			+ "    <supportedFormats>\r\n"
			+ "      <formats>GTiff</formats>\r\n"
			+ "    </supportedFormats>\r\n"
			+ "    <supportedInterpolations default=\"nearest neighbor\">\r\n"
			+ "      <interpolationMethod>nearest neighbor</interpolationMethod>\r\n"
			+ "      <interpolationMethod>bilinear</interpolationMethod>\r\n"
			+ "    </supportedInterpolations>\r\n"
			+ "  </CoverageOffering>\r\n"
			+ "</CoverageDescription>\r\n"
			+ "";
	
	private void xml_root__DescribeCoverage(RasterDB rasterdb, PrintWriter out) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getDescribeCoverage(rasterdb, doc));
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
	}
	
	private Node getDescribeCoverage(RasterDB rasterdb, Document doc) {
		Element rootElement = doc.createElementNS(NS_URL, "CoverageDescription");
		rootElement.setAttribute("version", "1.0.0");
		rootElement.setAttribute("xmlns:xlink", NS_XLINK);
		rootElement.setAttribute("xmlns:gml", NS_GML);

		Element eCoverageOffering = addElement(rootElement, "CoverageOffering");
		addElement(eCoverageOffering, "name", "RSDB WCS");
		addElement(eCoverageOffering, "label", rasterdb.config.getName());
		
		Element e_domainSet = addElement(eCoverageOffering, "domainSet");
		Element e_spatialDomain = addElement(e_domainSet, "spatialDomain");
		Element e_gml_Envelope = addElement(e_spatialDomain, "gml:Envelope");
		e_gml_Envelope.setAttribute("srsName", "EPSG:25832");
		GeoReference ref = rasterdb.ref();
		Range2d localRange = rasterdb.getLocalRange(false);
		addElement(e_gml_Envelope, "gml:pos", ref.pixelXToGeo(localRange.xmin) + " " + ref.pixelYToGeo(localRange.ymin));
		addElement(e_gml_Envelope, "gml:pos", ref.pixelXToGeo(localRange.xmax + 1) + " " + ref.pixelYToGeo(localRange.ymax + 1));
		Element e_gml_RectifiedGrid = addElement(e_spatialDomain, "gml:RectifiedGrid");
		e_gml_RectifiedGrid.setAttribute("dimension", "2");
		Element e_gml_limits = addElement(e_gml_RectifiedGrid, "gml:limits");
		Element e_gml_GridEnvelope = addElement(e_gml_limits, "gml:GridEnvelope");
		addElement(e_gml_GridEnvelope, "gml:low", localRange.xmin + " " + localRange.ymin);
		addElement(e_gml_GridEnvelope, "gml:high", localRange.xmax + " " + localRange.ymax);
		addElement(e_gml_RectifiedGrid, "gml:axisName", "x");
		addElement(e_gml_RectifiedGrid, "gml:axisName", "y");
		Element e_gml_origin = addElement(e_gml_RectifiedGrid, "gml:origin");
		addElement(e_gml_origin, "gml:pos", ref.offset_x + " " + ref.offset_y);
		addElement(e_gml_RectifiedGrid, "gml:offsetVector", ref.pixel_size_x + " " +  "0.0");
		addElement(e_gml_RectifiedGrid, "gml:offsetVector", "0.0" + " " + (-ref.pixel_size_y));
		
		Element e_rangeSet = addElement(eCoverageOffering, "rangeSet");
		Element e_RangeSet = addElement(e_rangeSet, "RangeSet");
		addElement(e_RangeSet, "name", "Range 1");
		addElement(e_RangeSet, "label", "My Label");
		
		Element e_supportedCRSs = addElement(eCoverageOffering, "supportedCRSs");
		addElement(e_supportedCRSs, "requestResponseCRSs", "EPSG:25832");
		addElement(e_supportedCRSs, "nativeCRSs", "EPSG:25832");
		
		Element e_supportedFormats = addElement(eCoverageOffering, "supportedFormats");
		addElement(e_supportedFormats, "formats", "GeoTIFF");

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
		if (ref.wms_transposed) {
			eBoundingBox.setAttribute("minx", "" + ref.pixelYToGeo(localRange.ymin));
			eBoundingBox.setAttribute("miny", "" + ref.pixelXToGeo(localRange.xmin));
			eBoundingBox.setAttribute("maxx", "" + ref.pixelYToGeo(localRange.ymax));
			eBoundingBox.setAttribute("maxy", "" + ref.pixelXToGeo(localRange.xmax));
		} else {
			eBoundingBox.setAttribute("minx", "" + ref.pixelXToGeo(localRange.xmin));
			eBoundingBox.setAttribute("miny", "" + ref.pixelYToGeo(localRange.ymin));
			eBoundingBox.setAttribute("maxx", "" + ref.pixelXToGeo(localRange.xmax));
			eBoundingBox.setAttribute("maxy", "" + ref.pixelYToGeo(localRange.ymax));
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