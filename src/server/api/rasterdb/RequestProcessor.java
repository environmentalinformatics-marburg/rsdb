package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import rasterdb.BandProcessing;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import rasterunit.RasterUnit;
import util.Range2d;
import util.TimeUtil;
import util.Web;

public class RequestProcessor {
	private static final Logger log = LogManager.getLogger();

	enum RdatDataType {INT16, FLOAT32, FLOAT64};

	enum TiffDataType {INT16, FLOAT32, FLOAT64};

	enum OutputProcessingType {IDENTITY, VISUALISATION};

	public static void process(String format, RasterDB rasterdb, Request request, Response response) throws IOException {
		request.setHandled(true);
		try {
			int scaleDiv = Web.getInt(request, "div", 1);
			int reqWidth = Web.getInt(request, "width", -1);
			int reqHeight = Web.getInt(request, "height", -1);
			Range2d range2d = getRange2d(rasterdb, request, rasterdb.ref(), reqWidth, reqHeight);		

			int timestamp = getTimestamp(request, rasterdb.rasterUnit());

			if(reqWidth > 0 || reqHeight > 0) {
				if(scaleDiv != 1) {
					log.warn("div parameter ignored");
				}
				scaleDiv = TimeBandProcessor.calcScale(range2d, reqWidth, reqHeight);
			}
			
			BandProcessor processor = new BandProcessor(rasterdb, Web.getFlag(request, "clipped") ? range2d.clip(rasterdb.getLocalRange(false)) : range2d, timestamp, scaleDiv);

			log.info("processor dstRange " + processor.getDstRange());
			log.info("processor srcRange " + processor.getSrcRange());

			String productText = request.getParameter("product");
			String bandText = request.getParameter("band");
			boolean visualisation = Web.getBoolean(request, "visualisation", false);			
			OutputProcessingType outputProcessingType = OutputProcessingType.IDENTITY;			
			if(format.equals("png") || format.equals("jpg") || (visualisation && format.equals("tiff"))) {
				outputProcessingType = OutputProcessingType.VISUALISATION;
			}

			if(productText == null) {
				Collection<TimeBand> processingBands;
				if (bandText != null) {
					try {
						String[] bandTexts = bandText.split(" ");
						processingBands = Arrays.stream(bandTexts).mapToInt(Integer::parseInt).mapToObj(i -> {
							TimeBand timeband = processor.getTimeBand(i);
							if (timeband == null) {
								throw new RuntimeException("band not found: " + i);
							}
							return timeband;
						}).collect(Collectors.toList());
					} catch(Exception e) {
						throw new RuntimeException("error in parameter band "+e);
					}
				} else if(outputProcessingType == OutputProcessingType.VISUALISATION) {
					processingBands = processor.toTimeBands(BandProcessing.getBestColorBands(processor.rasterdb));
				} else {
					processingBands = processor.getTimeBands();
				}

				RequestProcessorBands.processBands(processor, processingBands, outputProcessingType, format, new RequestProcessorBands.ResponseReceiver(response));
			} else { // product processing
				if (bandText != null) {
					throw new RuntimeException("parameter band can not be used if parameter product is specified");
				}
				RequestProcessorProduct.processProduct(processor, productText, outputProcessingType, format, new RequestProcessorBands.ResponseReceiver(response));								
			}
		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
			try {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println(e);
			} catch(Exception e1) {
				log.error(e1);
			}
		}
	}

	private static int getTimestamp(Request request, RasterUnit rasterUnit) {
		int timestamp = -1;
		String timestampText = request.getParameter("timestamp");
		if (timestampText == null) {
			if(rasterUnit.timeKeysReadonly.isEmpty()) {
				throw new RuntimeException("no data in layer");
			} else {
				timestamp = rasterUnit.timeKeysReadonly.last();
			}
		} else {
			if(timestampText.equals("0")) {
				timestamp = 0;
			} else if(timestampText.length() > 4 && allNum(timestampText)) {
				timestamp = Integer.parseInt(timestampText);
			} else {			
				//timestamp = Integer.parseInt(timestampText);
				int[] timestampRange = TimeUtil.getTimestampRangeOrNull(timestampText);
				if(timestampRange == null) {
					throw new RuntimeException("could not parse timestamp parameter: " + timestampText);
				} else {
					NavigableSet<Integer> timestampSubset = rasterUnit.timeKeysReadonly.subSet(timestampRange[0], true, timestampRange[1], true);
					if(timestampSubset.isEmpty()) {
						if(timestampRange[0] == timestampRange[1]) {
							throw new RuntimeException("no data in layer for time: " + TimeUtil.toPrettyText(timestampRange[0]));	
						} else {
							throw new RuntimeException("no data in layer for time range: " + TimeUtil.toPrettyText(timestampRange));
						}
					} else {
						timestamp = timestampSubset.first();
					}
				}
			}
		}
		log.info("timestamp   "+timestampText+"   "+timestamp+"    "+TimeUtil.toPrettyText(timestamp));
		return timestamp;
	}

	private static boolean allNum(String s) {
		for(char c:s.toCharArray()) {
			if(c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	private static Range2d getRange2d(RasterDB rasterdb, Request request, GeoReference ref, int reqWidth, int reqHeight) {
		String extText = request.getParameter("ext");
		if(extText == null) {
			if(reqWidth <= 0 && reqHeight <= 0) {
				throw new RuntimeException("missing parameter ext");
			}
			return rasterdb.getLocalRange(false);
		}
		String[] extTexts = (extText.split(" "));
		Range2d range2d = ref.parseExtentToRange2d(extTexts);
		return range2d;
	}
}
