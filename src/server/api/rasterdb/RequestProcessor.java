package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.tinylog.Logger;

import broker.TimeSlice;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.Band;
import rasterdb.BandProcessing;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import rasterunit.RasterUnitStorage;
import util.Range2d;
import util.ResponseReceiver;
import util.TimeUtil;
import util.Web;
import util.collections.vec.Vec;

public class RequestProcessor {	

	enum RdatDataType {INT16, FLOAT32, FLOAT64};

	enum TiffDataType {
		UINT8,
		INT16,
		UINT16,
		INT32,
		FLOAT32, 
		FLOAT64; 

		/**
		 * 
		 * @param dataTypeText nullable
		 * @return nullable
		 */
		public static TiffDataType parse(String dataTypeText) {
			if(dataTypeText == null || dataTypeText.isBlank()) {
				return null;
			}
			String dtt = dataTypeText.toLowerCase();
			switch(dtt) {
			case "uint8":
				return UINT8;
			case "int16":
				return INT16;
			case "uint16":
				return UINT16;
			case "int32":
				return INT32;
			case "float32":
				return FLOAT32;
			case "float64":
				return FLOAT64;
			default:
				throw new RuntimeException("unknown data type");
			}

		}
	};

	enum OutputProcessingType {IDENTITY, VISUALISATION};

	public static void process(String format, RasterDB rasterdb, Request request, Response response) throws IOException {
		request.setHandled(true);
		try {
			//Logger.info("buffer size " + response.getBufferSize());
			//response.setBufferSize(1024*1024);
			format = Web.getString(request, "format", format);

			String dataTypeText = Web.getString(request, "data_type", null);
			TiffDataType reqTiffdataType = TiffDataType.parse(dataTypeText);

			int scaleDiv = Web.getInt(request, "div", 1);
			int reqWidth = Web.getInt(request, "width", -1);
			int reqHeight = Web.getInt(request, "height", -1);
			Range2d range2d = getRange2d(rasterdb, request, rasterdb.ref(), reqWidth, reqHeight);
			if(range2d == null) {
				return;
			}


			int[] timeSliceIds = null;
			String timeSliceIdText = request.getParameter("time_slice_id");
			if(timeSliceIdText != null) {
				if(request.getParameter("time_slice") != null || request.getParameter("timestamp") != null) {
					throw new RuntimeException("only one time parameter can be set: time_slice_id, time_slice, timestamp");
				}
				if(timeSliceIdText.equals("all")) {			
					TreeSet<Integer> timestamps = new TreeSet<Integer>();
					timestamps.addAll(rasterdb.rasterUnit().timeKeysReadonly());
					timestamps.addAll(rasterdb.timeMapReadonly.keySet());					
					timeSliceIds = timestamps.stream().mapToInt(i->i).toArray();
				} else {
					String[] timeSliceIdTexts = timeSliceIdText.split("[ ,]+");
					timeSliceIds = Arrays.stream(timeSliceIdTexts).mapToInt(Integer::parseInt).toArray();
				}
			} else {
				timeSliceIds = new int[] {getTimestamp(request, rasterdb.rasterUnit(), rasterdb)};
			}

			String bandOrder = Web.getString(request, "band_order", "time_band");

			if(reqWidth > 0 || reqHeight > 0) {
				if(scaleDiv != 1) {
					Logger.warn("div parameter ignored");
				}
				scaleDiv = TimeBandProcessor.calcScale(range2d, reqWidth, reqHeight);
			}

			Range2d rasterLocalRange = rasterdb.getLocalRange(false);
			if(Web.getFlag(request, "clipped") && rasterLocalRange == null) {
				return;
			}

			TimeBandProcessor timeBandProcessor = new TimeBandProcessor(rasterdb, Web.getFlag(request, "clipped") ? range2d.clip(rasterLocalRange) : range2d, scaleDiv);

			Logger.info("processor dstRange " + timeBandProcessor.getDstRange());
			Logger.info("processor srcRange " + timeBandProcessor.getSrcRange());

			String productText = request.getParameter("product");
			String bandText = request.getParameter("band");
			boolean visualisation = Web.getBoolean(request, "visualisation", false);			
			OutputProcessingType outputProcessingType = OutputProcessingType.IDENTITY;			
			if(format.equals("png") || format.equals("jpg") || (visualisation && format.equals("tiff"))) {
				outputProcessingType = OutputProcessingType.VISUALISATION;
			}

			if(productText == null) {
				Vec<TimeBand> processingBands = new Vec<TimeBand>();
				if(timeSliceIds.length == 0) {
					throw new RuntimeException("no time slice parameter");
				}

				if(bandOrder.equals("time_band")) {
					for(int timestamp : timeSliceIds) {
						if (bandText != null) {
							try {
								String[] bandTexts = bandText.split("[ ,]+");
								if(bandTexts.length == 0) {
									throw new RuntimeException("no band parameter");
								}
								Arrays.stream(bandTexts).mapToInt(Integer::parseInt).forEachOrdered(i -> {
									TimeBand timeband = timeBandProcessor.getTimeBand(timestamp, i);
									if (timeband == null) {
										throw new RuntimeException("band not found: " + i);
									}
									processingBands.add(timeband);
								});
							} catch(Exception e) {
								throw new RuntimeException("error in parameter band "+e);
							}
						} else if(outputProcessingType == OutputProcessingType.VISUALISATION) {
							Band[] bands = BandProcessing.getBestColorBands(timeBandProcessor.rasterdb);
							List<TimeBand> timeBands = timeBandProcessor.toTimeBands(timestamp, bands);
							processingBands.addAll(timeBands);
						} else {
							List<TimeBand> timeBands = timeBandProcessor.getTimeBands(timestamp);
							processingBands.addAll(timeBands);
						}
					}
				} else if(bandOrder.equals("band_time")) {
					if (bandText != null) {
						try {
							String[] bandTexts = bandText.split("[ ,]+");
							if(bandTexts.length == 0) {
								throw new RuntimeException("no band parameter");
							}
							for(String s : bandTexts) {
								int i = Integer.parseInt(s);
								for(int timestamp : timeSliceIds) {
									TimeBand timeband = timeBandProcessor.getTimeBand(timestamp, i);
									if (timeband == null) {
										throw new RuntimeException("band not found: " + i);
									}
									processingBands.add(timeband);
								}
							}
						} catch(Exception e) {
							throw new RuntimeException("error in parameter band "+e);
						}
					} else if(outputProcessingType == OutputProcessingType.VISUALISATION) {
						Band[] bands = BandProcessing.getBestColorBands(timeBandProcessor.rasterdb);
						for(Band band : bands) {
							for(int timestamp : timeSliceIds) {
								TimeBand timeBand = timeBandProcessor.toTimeBand(timestamp, band);
								processingBands.add(timeBand);
							}
						}						
					} else {
						Collection<Band> bands = timeBandProcessor.getBands();
						for(Band band : bands) {
							for(int timestamp : timeSliceIds) {
								TimeBand timeBand = timeBandProcessor.toTimeBand(timestamp, band);
								processingBands.add(timeBand);
							}
						}
					}
				} else {
					throw new RuntimeException("unknown band_order parameter");
				}

				RequestProcessorBands.processBands(timeBandProcessor, processingBands, outputProcessingType, format, new ResponseReceiver(response), reqTiffdataType);
			} else { // product processing
				if (bandText != null) {
					throw new RuntimeException("parameter band can not be used if parameter product is specified");
				}
				if(timeSliceIds.length != 1) {
					throw new RuntimeException("product processing with one time slice only");
				}
				int timestamp = timeSliceIds[0];
				RequestProcessorProduct.processProduct(rasterdb, timeBandProcessor.toBandProcessor(timestamp), productText, outputProcessingType, format, new ResponseReceiver(response));								
			}
		} catch (Exception e) {
			Logger.error(e);
			e.printStackTrace();
			try {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println(e);
			} catch(Exception e1) {
				Logger.error(e1);
			}
		}
	}

	private static int getTimestamp(Request request, RasterUnitStorage rasterUnitStorage, RasterDB rasterdb) {		

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
				Logger.warn("could not parse timestamp: " + timestampText);
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
		String[] extTexts = extText.split("[ ,]+");
		if(extTexts.length != 4) {
			throw new RuntimeException("ext parameter syntax error");
		}
		Range2d range2d = ref.parseExtentToRange2d(extTexts);
		return range2d;
	}
}
