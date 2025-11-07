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
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.TimeSlice;
import pointcloud.Rect2d;
import rasterdb.CustomWCS;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import util.GeoUtil;
import util.Range2d;
import util.TimeUtil;
import util.Web;
import util.XmlUtil;

public class RasterdbMethod_wcs_DescribeCoverage {

	public static void handle_DescribeCoverage(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setContentType(Web.MIME_XML);		
		PrintWriter out = response.getWriter();	

		CustomWCS customWCS = null;
		if(!target.isEmpty()) {
			Logger.info("target |" + target + "|");
			customWCS = rasterdb.customWcsMapReadonly.get(target);
			if(customWCS == null) {
				throw new RuntimeException("custom WCS not found |" + target + "|");
			}
		}

		try {
			xml_root__DescribeCoverage(rasterdb, customWCS, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void xml_root__DescribeCoverage(RasterDB rasterdb, CustomWCS customWCS, PrintWriter out) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		doc.appendChild(getDescribeCoverage(rasterdb, customWCS, doc));
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
	}

	private static Node getDescribeCoverage(RasterDB rasterdb, CustomWCS customWCS, Document doc) {
		GeoReference ref = rasterdb.ref();		
		int layerEPSG = ref.getEPSG(0);
		boolean hasLayerCRS = layerEPSG > 0;
		int wcsEPSG = hasLayerCRS ? layerEPSG : 0;
		if(customWCS != null && customWCS.hasEPSG()) {
			wcsEPSG = customWCS.epsg;
		}
		boolean hasWcsCRS = wcsEPSG > 0;
		boolean isTransform = hasLayerCRS && hasWcsCRS && layerEPSG != wcsEPSG;
		if(!isTransform) {
			wcsEPSG = layerEPSG;
		}
		String wcsCRS = "EPSG:" + wcsEPSG;

		Range2d localRange = rasterdb.getLocalRange(false);
		if(localRange == null) {
			throw new RuntimeException("empty layer");
		}
		Rect2d layerRect = ref.range2dToRect2d(localRange);

		String envelopeCRS = wcsCRS;
		String requestResponseCRSs = wcsCRS; 
		String nativeCRSs = wcsCRS;
		Rect2d wcsRect = null;
		double wcsResx = Double.NaN;
		double wcsResy = Double.NaN;
		if(isTransform) {
			util.GeoUtil.Transformer transformer = GeoUtil.getCoordinateTransformer(layerEPSG, wcsEPSG);	
			if(transformer == null) {
				throw new RuntimeException("no transform");
			}
			double[][] rePoints = layerRect.createPoints9();
			transformer.transformWithAxisOrderCorrection(rePoints);
			wcsRect = Rect2d.ofPoints(rePoints);
			if(!wcsRect.isFinite()) {
				throw new RuntimeException("no rect");
			}
			wcsResx = wcsRect.width() / localRange.getWidth();
			wcsResy = wcsRect.height() / localRange.getHeight();
		} else {
			wcsRect	= layerRect;
			wcsResx = ref.pixel_size_x;
			wcsResy = ref.pixel_size_y;
		}

		Element rootElement = doc.createElementNS(RasterdbMethod_wcs.NS_URL, "CoverageDescription");
		rootElement.setAttribute("version", "1.0.0");
		rootElement.setAttribute("xmlns:xlink", RasterdbMethod_wcs.NS_XLINK);
		rootElement.setAttribute("xmlns:gml", RasterdbMethod_wcs.NS_GML);

		Element eCoverageOffering = XmlUtil.addElement(rootElement, "CoverageOffering");
		XmlUtil.addElement(eCoverageOffering, "name", "RSDB WCS");
		XmlUtil.addElement(eCoverageOffering, "label", rasterdb.config.getName());

		Element e_domainSet = XmlUtil.addElement(eCoverageOffering, "domainSet");
		Element e_spatialDomain = XmlUtil.addElement(e_domainSet, "spatialDomain");
		Element e_gml_Envelope = XmlUtil.addElement(e_spatialDomain, "gml:Envelope");
		e_gml_Envelope.setAttribute("srsName", envelopeCRS);

		XmlUtil.addElement(e_gml_Envelope, "gml:pos", wcsRect.xmin + " " + wcsRect.ymin);
		XmlUtil.addElement(e_gml_Envelope, "gml:pos", wcsRect.xmax + " " + wcsRect.ymax);
		Element e_gml_RectifiedGrid = XmlUtil.addElement(e_spatialDomain, "gml:RectifiedGrid");
		e_gml_RectifiedGrid.setAttribute("dimension", "2");
		if(!isTransform) {
			Element e_gml_limits = XmlUtil.addElement(e_gml_RectifiedGrid, "gml:limits");
			Element e_gml_GridEnvelope = XmlUtil.addElement(e_gml_limits, "gml:GridEnvelope");
			XmlUtil.addElement(e_gml_GridEnvelope, "gml:low",  0 + " " + 0);
			XmlUtil.addElement(e_gml_GridEnvelope, "gml:high", (localRange.xmax - localRange.xmin) + " " + (localRange.ymax - localRange.ymin));
		}
		XmlUtil.addElement(e_gml_RectifiedGrid, "gml:axisName", "x");
		XmlUtil.addElement(e_gml_RectifiedGrid, "gml:axisName", "y");
		Element e_gml_origin = XmlUtil.addElement(e_gml_RectifiedGrid, "gml:origin");
		XmlUtil.addElement(e_gml_origin, "gml:pos", wcsRect.xmin + " " + wcsRect.ymax);
		XmlUtil.addElement(e_gml_RectifiedGrid, "gml:offsetVector", wcsResx + " " +  "0.0");
		XmlUtil.addElement(e_gml_RectifiedGrid, "gml:offsetVector", "0.0" + " " + (-wcsResy));
		TreeSet<Integer> timestamps = new TreeSet<Integer>();
		timestamps.addAll(rasterdb.rasterUnit().timeKeysReadonly());
		timestamps.addAll(rasterdb.timeMapReadonly.keySet());
		if(!timestamps.isEmpty()) {
			Element e_temporalDomain = XmlUtil.addElement(e_domainSet, "temporalDomain");
			for(Integer timestamp:timestamps) {
				TimeSlice timeSlice = rasterdb.timeMapReadonly.get(timestamp);
				if(timeSlice == null) {
					timeSlice = new TimeSlice(timestamp, TimeUtil.toText(timestamp));
				}
				XmlUtil.addElement(e_temporalDomain, "gml:timePosition", timeSlice.name);
			}
		}

		Element e_rangeSet = XmlUtil.addElement(eCoverageOffering, "rangeSet");
		Element e_RangeSet = XmlUtil.addElement(e_rangeSet, "RangeSet");
		for(rasterdb.Band band : rasterdb.bandMapReadonly.values()) {
			if(customWCS == null || customWCS.includesBand(band.index)) {
				XmlUtil.addElement(e_RangeSet, "name", "" + band.index);
				XmlUtil.addElement(e_RangeSet, "label", band.title);
			}
		}

		Element e_supportedCRSs = XmlUtil.addElement(eCoverageOffering, "supportedCRSs");
		XmlUtil.addElement(e_supportedCRSs, "requestResponseCRSs", requestResponseCRSs);
		XmlUtil.addElement(e_supportedCRSs, "nativeCRSs", nativeCRSs);

		Element e_supportedFormats = XmlUtil.addElement(eCoverageOffering, "supportedFormats");
		XmlUtil.addElement(e_supportedFormats, "formats", "GeoTIFF");

		return rootElement;
	}

	/*//https://www.wcs.nrw.de/geobasis/wcs_nw_dtk100?SERVICE=WCS&REQUEST=DescribeCoverage&VERSION=1.0.0&COVERAGE=nw_dtk100_col
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
			+ "";*/
}