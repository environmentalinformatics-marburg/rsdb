package server.api.rasterdb;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.TimeSlice;
import rasterdb.BandProcessing;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import rasterunit.RasterUnitStorage;
import util.Range2d;
import util.ResponseReceiver;
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
			//log.info("buffer size " + response.getBufferSize());
			//response.setBufferSize(1024*1024);
			format = Web.getString(request, "format", format);

			int scaleDiv = Web.getInt(request, "div", 1);
			int reqWidth = Web.getInt(request, "width", -1);
			int reqHeight = Web.getInt(request, "height", -1);
			Range2d range2d = getRange2d(rasterdb, request, rasterdb.ref(), reqWidth, reqHeight);
			if(range2d == null) {
				return;
			}

			int timestamp = getTimestamp(request, rasterdb.rasterUnit(), rasterdb);

			if(reqWidth > 0 || reqHeight > 0) {
				if(scaleDiv != 1) {
					log.warn("div parameter ignored");
				}
				scaleDiv = TimeBandProcessor.calcScale(range2d, reqWidth, reqHeight);
			}

			Range2d rasterLocalRange = rasterdb.getLocalRange(false);
			if(Web.getFlag(request, "clipped") && rasterLocalRange == null) {
				return;
			}

			BandProcessor processor = new BandProcessor(rasterdb, Web.getFlag(request, "clipped") ? range2d.clip(rasterLocalRange) : range2d, timestamp, scaleDiv);

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

				RequestProcessorBands.processBands(processor, processingBands, outputProcessingType, format, new ResponseReceiver(response));
			} else { // product processing
				if (bandText != null) {
					throw new RuntimeException("parameter band can not be used if parameter product is specified");
				}
				RequestProcessorProduct.processProduct(processor, productText, outputProcessingType, format, new ResponseReceiver(response));								
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

	private static int getTimestamp(Request request, RasterUnitStorage rasterUnitStorage, RasterDB rasterdb) {		
		int time_slice_id = Web.getInt(request, "time_slice_id", Integer.MIN_VALUE); // obsolete ?
		if(time_slice_id > Integer.MIN_VALUE) {
			return time_slice_id;
		}	

		String time_sliceText = request.getParameter("time_slice");
		String timestampText = request.getParameter("timestamp");

		if(time_sliceText != null) {
			if(timestampText != null) {
				throw new RuntimeException("only one parameter can be specified: time_slice or timestamp");
			}
			TimeSlice timeSlice = rasterdb.getTimeSliceByName(time_sliceText);
			if(timeSlice != null) {
				return timeSlice.id;
			} else {
				throw new RuntimeException("time_slice not found: " + timeSlice);
			}
		}

		if (timestampText == null) {
			if(rasterUnitStorage.timeKeysReadonly().isEmpty()) {
				throw new RuntimeException("no data in layer");
			} else {
				return rasterUnitStorage.timeKeysReadonly().last();
			}
		} else {
			int[] timestampRange = null;
			try{
				timestampRange = TimeUtil.getTimestampRangeOrNull(timestampText);
			}catch(Exception e) {
				log.warn("could not parse timestamp: " + timestampText);
			}
			if(timestampRange != null) {
				NavigableSet<Integer> timestampSubset = rasterUnitStorage.timeKeysReadonly().subSet(timestampRange[0], true, timestampRange[1], true);
				if(timestampSubset.isEmpty()) {
					if(timestampRange[0] == timestampRange[1]) {
						throw new RuntimeException("no data in layer for time: " + TimeUtil.toPrettyText(timestampRange[0]));	
					} else {
						throw new RuntimeException("no data in layer for time range: " + TimeUtil.toPrettyText(timestampRange));
					}
				} else {
					return timestampSubset.first();
				}
			} else {
				int t = 0;
				try{
					t = Integer.parseInt(timestampText);					
				}catch(Exception e) {
					throw new RuntimeException("could not parse timestamp: "+timestampText);
				}
				if(rasterUnitStorage.timeKeysReadonly().contains(t)) {
					return t;
				} else {
					throw new RuntimeException("no data in layer for time: " + TimeUtil.toPrettyText(t));
				}
			}			
		}
	}

	private static boolean allNum(String s) {
		for(char c:s.toCharArray()) {
			if(c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param rasterdb
	 * @param request
	 * @param ref
	 * @param reqWidth
	 * @param reqHeight
	 * @return range or null
	 */
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
