package util.rdat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;


import org.tinylog.Logger;

import util.collections.vec.Vec;

public class RdatWriter {
	

	private final int width;
	private final int height;

	private final double xmin;
	private final double ymin;
	private final double xmax;
	private final double ymax;

	private String proj4 = null;

	private Short noDataValue = null;

	private RdatList meta; // nullable

	private Vec<RdatBand> rdatBands = new Vec<RdatBand>();

	/**
	 * 
	 * @param width
	 * @param height
	 * @param xmin
	 * @param ymin
	 * @param xmax extent of pixels e.g. xmax_left + 1
	 * @param ymax extent of pixels
	 */
	public RdatWriter(int width, int height, double xmin, double ymin, double xmax, double ymax, RdatList meta) {
		this.width = width;
		this.height = height;
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.meta = meta;
	}

	public void addRdatBand(RdatBand rdatBand) {
		Objects.requireNonNull(rdatBand);
		if(rdatBand.width != width) {
			throw new RuntimeException();
		}
		if(rdatBand.height != height) {
			throw new RuntimeException();
		}
		if(!rdatBands.isEmpty()) {
			RdatBand refBand = rdatBands.first();
			if(refBand.getType() != rdatBand.getType() || refBand.getBytesPerSample() != rdatBand.getBytesPerSample()) {
				throw new RuntimeException("rdat bands need to be of same type");				
			}
		}
		rdatBands.add(rdatBand);
	}

	public void setProj4(String proj4) {
		this.proj4 = proj4;
	}

	/**
	 * 
	 * @param noDataValue nullable
	 */
	public void setNoDataValue(Short noDataValue) {
		this.noDataValue = noDataValue;
	}

	private RdatList createMeta() {
		RdatList list = meta;
		if(list == null) {
			list = new RdatList();
		}
		list.addFloat64("xmn", xmin);
		list.addFloat64("ymn", ymin);
		list.addFloat64("xmx", xmax); // extent of pixels
		list.addFloat64("ymx", ymax); // extent of pixels
		if(proj4 != null) {
			list.addString("proj4", proj4);
		}
		if(noDataValue != null) {
			list.addUint16("nodatavalue", noDataValue);
		}
		Logger.info("rDat meta list: "+list);
		return list;
	}

	public void write(DataOutput out) throws IOException {
		if(rdatBands.isEmpty()) {
			throw new RuntimeException("no bands");
		}

		out.write(Rdat.SIGNATURE_RDAT);
		out.write(Rdat.RDAT_TYPE_RASTER);

		createMeta().write(out);

		out.writeByte(rdatBands.first().getType()); // legacy entry
		out.writeByte(rdatBands.first().getBytesPerSample()); // legacy entry

		int len = rdatBands.size();
		out.writeInt(len);
		out.writeInt(height);
		out.writeInt(width);

		for(RdatBand rdatBand:rdatBands) {
			rdatBand.writeMeta(out);
			rdatBand.writeData(out);
		}
	}

}
