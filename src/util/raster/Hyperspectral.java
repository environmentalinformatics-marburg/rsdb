package util.raster;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Reader of hyperspectral files in ENVI format.
 * @author woellauer
 *
 */
public class Hyperspectral {
	//

	FileChannel filechannel;

	public int samples;
	public int	lines;
	int	bands;
	long header_offset;
	long stride_BIL;
	long stride_BSQ;
	String interleave;

	public final EnviHdr enviHdr;

	public Hyperspectral(String envi_data_file) throws IOException {
		String hyspex_hdr = envi_data_file.substring(0,envi_data_file.lastIndexOf('.'))+".hdr";
		enviHdr = new EnviHdr(hyspex_hdr);

		if(enviHdr.data_type != 2 && enviHdr.data_type != 12) {// 2 == Integer: 16-bit signed integer, 12 == Unsigned integer: 16-bit
			throw new RuntimeException("unknown data type format in ENVI file: "+enviHdr.data_type);
		}

		this.samples = enviHdr.samples;
		this.lines = enviHdr.lines;
		this.bands = enviHdr.bands;
		this.header_offset = enviHdr.header_offset;
		this.interleave = enviHdr.interleave.toLowerCase();
		this.stride_BIL = this.samples*this.bands*2;
		this.stride_BSQ = this.samples*2;



		filechannel = FileChannel.open(Paths.get(envi_data_file), StandardOpenOption.READ);
		//Logger.info("file "+envi_data_file);
		//mapBuffer(0);		
	}

	/*private void mapBuffer(long start) throws IOException {
		bufferOffset = start;
		long fileOffset = header_offset+bufferOffset;
		bufferSize = filechannel.size()-fileOffset;
		if(bufferSize>Integer.MAX_VALUE) {
			bufferSize = Integer.MAX_VALUE;
		}
		//System.out.println("bufferSize "+bufferSize);
		mappedByteBuffer = filechannel.map(MapMode.READ_ONLY, fileOffset, bufferSize);
		mappedByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}*/

	/*private void setBufferPosition(long pos) throws IOException {
		if(pos<bufferOffset) {
			long temp = pos-Integer.MAX_VALUE/2;
			if(temp<0) {
				mapBuffer(0);
			} else {
				mapBuffer(temp);
			}			
		}
		if(pos>bufferOffset+(bufferSize/2)) {
			mapBuffer(pos);
		}
		mappedByteBuffer.position((int) (pos-bufferOffset));
	}*/

	public short[][][] getData() throws IOException {
		short[][][] data = new short[bands][lines][samples];
		for(int band = 0;band<bands;band++) {
			data[band] = getData(band,0,lines,data[band]);
		}
		return data;
	}

	/**
	 * Get all Data of one band
	 * @param band index number of band (starting with zero)
	 * @param targetData maybe null, will be zero-filled at start.
	 * @return targetData filled with data or new array if targetData is null or wrong dimensions
	 * @throws IOException
	 */
	public short[][] getData(int band, short[][] targetData) throws IOException {
		return getData(band, 0, lines, targetData);
	}

	ByteBuffer helperBuffer = null;

	public short[][] getData(int band, int firstLine, int maxLines, short[][] targetData) throws IOException {
		//Logger.info("maxLines "+maxLines);
		short[][] data;
		if(targetData == null || targetData.length!=maxLines || targetData[0].length!=samples) {
			//Logger.info("create new data array");
			data = new short[maxLines][samples];
		} else {
			data = targetData;
			for(int y=0;y<maxLines;y++) {
				short[] row = data[y];
				for(int x=0;x<samples;x++) {
					row[x] = 0;
				}
			}
		}

		if(maxLines<1) {
			return data;
		}
		if(firstLine>=lines) {
			return data;
		}
		int lineStart = firstLine;
		int lineEnd = firstLine+maxLines-1;
		if(lineStart<0) {
			lineStart=0;
		}
		if(lineEnd<0) {
			return data;
		}
		if(lineEnd>=lines) {
			lineEnd = lines-1;
		}

		//Logger.info(interleave);

		switch(interleave) {
		case "bil": {
			throw new RuntimeException("TODO: not implemented");
			/*long band_offset = band*(samples*2l);
			for(long y=lineStart;y<=lineEnd;y++) {
				setBufferPosition(y*stride_BIL+band_offset);
				for(int x=0;x<samples;x++) {
					short value = mappedByteBuffer.getShort();
					data[(int) (y-lineStart)][x] = value;
				}
			}*/
			//break;
		}
		case "bsq": {
			//Timer.start("read bsq");
			long band_offset = header_offset + 2l*samples*lines*band;
			long fileBytes = (lineEnd-lineStart+1)*stride_BSQ;
			//Logger.info("fileBytes "+fileBytes);
			if(fileBytes>Integer.MAX_VALUE) {
				throw new RuntimeException("integer overflow");
			}
			if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
				helperBuffer = ByteBuffer.allocateDirect((int) fileBytes);
				helperBuffer.order(ByteOrder.LITTLE_ENDIAN);
			}
			helperBuffer.rewind();
			filechannel.position(band_offset);
			int ret = filechannel.read(helperBuffer);
			if(ret!=fileBytes) {
				throw new RuntimeException("file read error");
			}
			helperBuffer.rewind();

			final int lines = lineEnd-lineStart+1;
			//Logger.info("line "+lineStart+" "+lineEnd+" "+lines);
			/*int xlen = samples;
			ByteBuffer buffer = helperBuffer;
			Logger.info("get "+lines+"  "+xlen);
			for(int y=0;y<lines;y++) {
				short[] dataRow = data[y];
				for(int x=0;x<xlen;x++) {
					short value = buffer.getShort();
					dataRow[x] = value;
				}
			}*/
			ShortBuffer shortBuffer = helperBuffer.asShortBuffer();
			for(int y=0;y<lines;y++) {
				shortBuffer.get(data[y]);
			}
			//Logger.info(Timer.stopToString("read bsq"));
			break;
		}
		default:
			throw new RuntimeException("TODO: not implemented "+interleave);
		}
		return data;		
	}

	public int getBandCount() {
		return bands;
	}

	public double[] getGeoRef() {
		return new double[]{enviHdr.mapinfo_pixelEasting,enviHdr.mapinfo_pixelNorthing};
	}

}
