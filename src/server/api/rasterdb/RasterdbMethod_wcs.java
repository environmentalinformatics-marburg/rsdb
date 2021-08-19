package server.api.rasterdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.http.HttpServletResponse;
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
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import broker.Broker;
import broker.TimeSlice;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import server.api.rasterdb.RequestProcessor.TiffDataType;
import util.Extent2d;
import util.Range2d;
import util.ResponseReceiver;
import util.TimeUtil;
import util.Timer;
import util.Web;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;
import util.tiff.TiffBand;
import util.tiff.TiffWriter;

public class RasterdbMethod_wcs extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	private static Driver GDAL_MEM_DRIVER = null;

	static {
		gdal.AllRegister();
		GDAL_MEM_DRIVER = gdal.GetDriverByName("MEM");
	}

	public RasterdbMethod_wcs(Broker broker) {
		super(broker, "wcs");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WCS".equals(request.getParameter("SERVICE"))) {
			log.error("no WCS");
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

	public int getGdalDatatypeFromTiffDatatype(TiffDataType tiffdataType) {
		switch(tiffdataType) {
		case INT16:			
			return gdalconstConstants.GDT_UInt16;
		case FLOAT32:
			return gdalconstConstants.GDT_Float32;
		case FLOAT64:
			return gdalconstConstants.GDT_Float64;
		default:
			throw new RuntimeException("unknown tiff data type");
		}
	}

	private static final AtomicLong memFileIdCounter = new AtomicLong(0);

	public void handle_GetCoverage(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		//log.info("handle");
		Timer.start("WCS processing");
		int dstWidth = Web.getInt(request, "WIDTH", -1);
		int dstHeight = Web.getInt(request, "HEIGHT", -1);

		String[] bbox = request.getParameter("BBOX").split(",");
		Extent2d extent2d = Extent2d.parse(bbox[0], bbox[1], bbox[2], bbox[3]);
		double geoXres = (extent2d.xmax - extent2d.xmin) / dstWidth;
		double geoYres = (extent2d.ymax - extent2d.ymin) / dstHeight;

		ResponseReceiver resceiver = new ResponseReceiver(response);

		GeoReference ref = rasterdb.ref();
		Range2d range2d = ref.bboxToRange2d(extent2d.xmin, extent2d.ymin, extent2d.xmax, extent2d.ymax);
		//log.info(extent2d);
		//log.info(range2d);

		int timestamp = 0;
		if(Web.has(request, "TIME")) {
			String timeText = Web.getString(request, "TIME");
			TimeSlice timeSlice = rasterdb.getTimeSliceByName(timeText);
			if(timeSlice == null) {
				int[] timestampRange = null;
				try{
					timestampRange = TimeUtil.getTimestampRangeOrNull(timeText);
					if(timestampRange != null) {
						timeSlice = new TimeSlice(timestampRange[0], timeText);
					}
				}catch(Exception e) {
					//log.warn("could not parse timestamp: " + timeText);
					throw new RuntimeException("tim slice not found");
				}
			}
			if(timeSlice != null) {
				timestamp = timeSlice.id;
			}
		} else if(!rasterdb.rasterUnit().timeKeysReadonly().isEmpty()){			
			timestamp = rasterdb.rasterUnit().timeKeysReadonly().last();
		}

		BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, dstWidth, dstHeight);

		List<TimeBand> processingBands = processor.getTimeBands();

		/*OutputProcessingType outputProcessingType = OutputProcessingType.IDENTITY;		
		RequestProcessorBands.processBands(processor, processingBands, outputProcessingType, "tiff", resceiver);*/

		Range2d srcRange = processor.getDstRange();
		if(srcRange.getPixelCount() > 16777216) { // 4096*4096
			throw new RuntimeException("requested raster too large: " + srcRange.getWidth() + " x " + srcRange.getHeight());
		}



		TiffDataType tiffdataType = RequestProcessorBandsWriters.getTiffDataType(processingBands); // all bands need same data type for tiff reader compatibility (e.g. GDAL)
		TiffWriter tiffWriter = new TiffWriter(dstWidth, dstHeight, extent2d.xmin, extent2d.ymin, geoXres, geoYres, (short)ref.getEPSG(0));

		//Timer.start("raster convert");
		boolean direct = (tiffdataType == TiffDataType.INT16 || tiffdataType == TiffDataType.FLOAT32) && ScaleDownMax2.validScaleDownMax2(srcRange.getWidth(), srcRange.getHeight(), dstWidth, dstHeight);
		//boolean direct = false;
		if(direct) {
			//log.info("direct");
			directConvert(processor, processingBands, tiffdataType, dstWidth, dstHeight, tiffWriter);
		} else {
			log.info("GDAL");
			GDALconvert(processor, processingBands, tiffdataType, dstWidth, dstHeight, tiffWriter);
		}
		//log.info(Timer.stop("raster convert"));
		log.info(Timer.stop("WCS processing"));

		Timer.start("WCS transfer");
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/tiff");
		long tiffSize = tiffWriter.exactSizeOfWriteAuto();
		resceiver.setContentLength(tiffSize);
		tiffWriter.writeAuto(new DataOutputStream(resceiver.getOutputStream()));
		log.info(Timer.stop("WCS transfer") + "  " + (tiffSize >= 1024*1024 ? ((tiffSize / (1024*1024)) + " MBytes") : (tiffSize + " Bytes") ) + "  " + dstWidth + " x " + dstHeight + " pixel  " + processingBands.size() + " bands of " + tiffdataType);
	}

	private void directConvert(BandProcessor processor, List<TimeBand> processingBands, TiffDataType tiffdataType, int dstWidth, int dstHeight, TiffWriter tiffWriter) {
		Range2d srcRange = processor.getDstRange();
		Short noDataValue = null;
		for(TimeBand timeband : processingBands) {	
			switch(tiffdataType) {
			case INT16:	{
				ShortFrame frame = processor.getShortFrame(timeband);
				short na = timeband.band.getInt16NA();
				short[][] dstData = ScaleDownMax2.scaleDownMax2(frame.data, srcRange.getWidth(), srcRange.getHeight(), dstWidth, dstHeight, na);
				tiffWriter.addTiffBand(TiffBand.ofInt16(dstData, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			}			
			case FLOAT32: {
				FloatFrame frame = processor.getFloatFrame(timeband);
				float[][] dstData = ScaleDownMax2.scaleDownMax2(frame.data, srcRange.getWidth(), srcRange.getHeight(), dstWidth, dstHeight);
				tiffWriter.addTiffBand(TiffBand.ofFloat32(dstData, timeband.toDescription()));
				break;
			}
			default:
				throw new RuntimeException("unknown tiff data type");
			}
		}
		tiffWriter.setNoDataValue(noDataValue);
	}

	private void GDALconvert(BandProcessor processor, List<TimeBand> processingBands, TiffDataType tiffdataType, int dstWidth, int dstHeight, TiffWriter tiffWriter) {
		int gdalDataType = getGdalDatatypeFromTiffDatatype(tiffdataType);
		Range2d srcRange = processor.getDstRange();

		Dataset datasetSrc = GDAL_MEM_DRIVER.Create("src", srcRange.getWidth(), srcRange.getHeight(), processingBands.size(), gdalDataType);
		//Driver memDriver = gdal.GetDriverByName("GTiff");
		//Dataset datasetSrc = memDriver.Create("c:/temp4/gdalfile/t17.tiff", srcRange.getWidth(), srcRange.getHeight(), processingBands.size(), gdalconstConstants.GDT_UInt16);
		Dataset datasetDst = null;

		try {
			int bandIndex = 1;
			for(TimeBand timeband : processingBands) {
				Band gdalBand = datasetSrc.GetRasterBand(bandIndex);
				switch(tiffdataType) {
				case INT16:	{
					ShortFrame frame = processor.getShortFrame(timeband);
					for(int y = 0; y < frame.height; y++) {
						gdalBand.WriteRaster(0, y, frame.width, 1, frame.data[y]);
					}
					break;
				}
				case FLOAT32:	{
					FloatFrame frame = processor.getFloatFrame(timeband);
					for(int y = 0; y < frame.height; y++) {
						gdalBand.WriteRaster(0, y, frame.width, 1, frame.data[y]);
					}
					break;
				}
				case FLOAT64: {
					DoubleFrame frame = processor.getDoubleFrame(timeband);
					for(int y = 0; y < frame.height; y++) {
						gdalBand.WriteRaster(0, y, frame.width, 1, frame.data[y]);
					}
					break;
				}
				default:
					throw new RuntimeException("unknown tiff data type");
				}	
				bandIndex++;
			}

			Vector<String> options = new Vector<String>();
			options.add("-outsize");
			options.add(""+dstWidth);
			options.add(""+dstHeight);

			options.add("-r");
			options.add("cubic");
			TranslateOptions translateOptions = new TranslateOptions(options);
			datasetDst = gdal.Translate("/vsimem/rsdb_wcs_in_memory_output"+ memFileIdCounter.incrementAndGet() +".tif", datasetSrc, translateOptions);
		} finally {
			if(datasetSrc != null) {
				datasetSrc.delete();
			}
		}

		try {
			int bandIndex = 1;
			Short noDataValue = null;
			for(TimeBand timeband : processingBands) {	
				Band gdalBand = datasetDst.GetRasterBand(bandIndex);
				switch(tiffdataType) {
				case INT16:	{
					short[][] dstData = new short[dstHeight][dstWidth];
					/*for(int y = 0; y < dstHeight; y++) {
						short[] dstDataY = dstData[y];
						for(int x = 0; x < dstWidth; x++) {
							dstDataY[x] = (short) ((13*y * 29*x) % 31);
						}
					}*/
					for(int y = 0; y < dstHeight; y++) {
						gdalBand.ReadRaster(0, y, dstWidth, 1, gdalDataType, dstData[y]);
					}
					tiffWriter.addTiffBand(TiffBand.ofInt16(dstData, timeband.toDescription()));
					if(noDataValue == null) {
						noDataValue = timeband.band.getInt16NA();
					}
					break;
				}
				case FLOAT32: {
					float[][] dstData = new float[dstHeight][dstWidth];
					for(int y = 0; y < dstHeight; y++) {
						gdalBand.ReadRaster(0, y, dstWidth, 1, gdalDataType, dstData[y]);
					}
					tiffWriter.addTiffBand(TiffBand.ofFloat32(dstData, timeband.toDescription()));
					break;
				}
				case FLOAT64: {
					double[][] dstData = new double[dstHeight][dstWidth];
					for(int y = 0; y < dstHeight; y++) {
						gdalBand.ReadRaster(0, y, dstWidth, 1, gdalDataType, dstData[y]);
					}
					tiffWriter.addTiffBand(TiffBand.ofFloat64(dstData, timeband.toDescription()));
					break;
				}
				default:
					throw new RuntimeException("unknown tiff data type");
				}
				bandIndex++;
			}
			tiffWriter.setNoDataValue(noDataValue);
		} finally {
			if(datasetDst != null) {
				datasetDst.delete();
			}
		}		
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
		e_gml_Envelope.setAttribute("srsName", rasterdb.ref().code);
		GeoReference ref = rasterdb.ref();
		Range2d localRange = rasterdb.getLocalRange(false);
		addElement(e_gml_Envelope, "gml:pos", ref.pixelXToGeo(localRange.xmin) + " " + ref.pixelYToGeo(localRange.ymin));
		addElement(e_gml_Envelope, "gml:pos", ref.pixelXToGeo(localRange.xmax + 1) + " " + ref.pixelYToGeo(localRange.ymax + 1));
		Element e_gml_RectifiedGrid = addElement(e_spatialDomain, "gml:RectifiedGrid");
		e_gml_RectifiedGrid.setAttribute("dimension", "2");
		Element e_gml_limits = addElement(e_gml_RectifiedGrid, "gml:limits");
		Element e_gml_GridEnvelope = addElement(e_gml_limits, "gml:GridEnvelope");
		addElement(e_gml_GridEnvelope, "gml:low",  0 + " " + 0);
		addElement(e_gml_GridEnvelope, "gml:high", (localRange.xmax - localRange.xmin) + " " + (localRange.ymax - localRange.ymin));
		addElement(e_gml_RectifiedGrid, "gml:axisName", "x");
		addElement(e_gml_RectifiedGrid, "gml:axisName", "y");
		Element e_gml_origin = addElement(e_gml_RectifiedGrid, "gml:origin");
		addElement(e_gml_origin, "gml:pos", ref.pixelXToGeo(localRange.xmin) + " " + ref.pixelYToGeo(localRange.ymax + 1));
		addElement(e_gml_RectifiedGrid, "gml:offsetVector", ref.pixel_size_x + " " +  "0.0");
		addElement(e_gml_RectifiedGrid, "gml:offsetVector", "0.0" + " " + (-ref.pixel_size_y));
		TreeSet<Integer> timestamps = new TreeSet<Integer>();
		timestamps.addAll(rasterdb.rasterUnit().timeKeysReadonly());
		timestamps.addAll(rasterdb.timeMapReadonly.keySet());
		if(!timestamps.isEmpty()) {
			Element e_temporalDomain = addElement(e_domainSet, "temporalDomain");
			for(Integer timestamp:timestamps) {
				TimeSlice timeSlice = rasterdb.timeMapReadonly.get(timestamp);
				if(timeSlice == null) {
					timeSlice = new TimeSlice(timestamp, TimeUtil.toText(timestamp));
				}
				addElement(e_temporalDomain, "gml:timePosition", timeSlice.name);
			}
		}

		Element e_rangeSet = addElement(eCoverageOffering, "rangeSet");
		Element e_RangeSet = addElement(e_rangeSet, "RangeSet");
		for(rasterdb.Band band : rasterdb.bandMapReadonly.values()) {
			addElement(e_RangeSet, "name", ""+band.index);
			addElement(e_RangeSet, "label", band.title);
		}

		Element e_supportedCRSs = addElement(eCoverageOffering, "supportedCRSs");
		addElement(e_supportedCRSs, "requestResponseCRSs", rasterdb.ref().code);
		addElement(e_supportedCRSs, "nativeCRSs", rasterdb.ref().code);

		Element e_supportedFormats = addElement(eCoverageOffering, "supportedFormats");
		addElement(e_supportedFormats, "formats", "GeoTIFF");

		return rootElement;
	}
}