package server.api.pointclouds;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import broker.Broker;
import pointcloud.AttributeSelector;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointcloud.Zvolume;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;

public class APIHandler_volume {
	private static final Logger log = LogManager.getLogger();

	protected static final String MIME_JSON = "application/json";	
	protected static final String MIME_CSV = "text/csv";

	//private final Broker broker;

	private final static double SMALL_VALUE = 0.000001d;

	public APIHandler_volume(Broker broker) {
		//this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, HttpServletResponse response) throws IOException {
		String extText = request.getParameter("ext");
		if(extText == null) {
			throw new RuntimeException("missing parameter 'ext'");
		}
		String[] ext = extText.split(" ");
		if(ext.length != 4) {
			throw new RuntimeException("parameter error in 'ext': "+extText);
		}
		double req_xmin = Double.parseDouble(ext[0]);
		double req_ymin = Double.parseDouble(ext[1]);
		double req_xmax = Double.parseDouble(ext[2]);
		double req_ymax = Double.parseDouble(ext[3]);
		log.info("req "+req_xmin+" "+req_ymin+" "+req_xmax+" "+req_ymax);

		String resText = request.getParameter("res");
		double res = 1;
		if(resText != null) {
			res = Double.parseDouble(resText);
		}

		String zresText = request.getParameter("zres");
		double zres = res;
		if(zresText != null) {
			zres = Double.parseDouble(zresText);
		}

		double res_xmin = req_xmin;
		double res_ymin = req_ymin;
		double res_xmax = ((int)((req_xmax - req_xmin) / res)) + res_xmin;
		double res_ymax = ((int)((req_ymax - req_ymin) / res)) + res_ymin;
		log.info("res "+res_xmin+" "+res_ymin+" "+res_xmax+" "+res_ymax);

		double proc_add = res - SMALL_VALUE;
		double proc_xmin = res_xmin;
		double proc_ymin = res_ymin;
		double proc_xmax = res_xmax + proc_add;
		double proc_ymax = res_ymax + proc_add;
		log.info("proc "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);

		AttributeSelector selector = new AttributeSelector().setXYZ();
		Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector);
		Zvolume zvolume = new Zvolume(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
		pointTables.sequential().forEach(zvolume::insert);
		double[] range = zvolume.getZRange(100d);
		log.info("valume range "+Arrays.toString(range));
		int[][][] volume = zvolume.getVolume(range[0], range[1], zres);

		switch(format) {
		case "rdat": {
			RdatList meta = new RdatList();
			meta.addString("source", "pointcloud");
			int width = volume[0][0].length;
			int height = volume[0].length;
			int levels = volume.length;

			RdatWriter rdatWriter = new RdatWriter(width, height, res_xmin, res_ymin, res_xmax + res, res_ymax + res, meta);
			for (int level = 0; level < levels; level++) {
				RdatList bandMeta = new RdatList();
				rdatWriter.addRdatBand(RdatBand.ofInt32(width, height, bandMeta, volume[level]));
			}		
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/octet-stream");
			rdatWriter.write(new DataOutputStream(response.getOutputStream()));
			break;
		}
		default:
			throw new RuntimeException("unknown format: "+format);
		}
	}



}
