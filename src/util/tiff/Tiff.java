package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.GeoReference;
import util.Serialisation;
import util.frame.DoubleFrame;
import util.frame.ShortFrame;

/**
 * old version
 * @use TiffWriter
 * @author woellauer
 *
 */
public class Tiff {
	static final Logger log = LogManager.getLogger();

	private final DataOutput out; 

	public Tiff(DataOutput out) {
		this.out = out;
	}

	private void writeHeader() throws IOException {
		out.writeInt(0x4d4d002a); // magic TIFF header  big endian
		int IFDOffset = 0x00_00_00_08;
		out.writeInt(IFDOffset);			
	}

	public void writeInterleaved(ShortFrame... frames) throws IOException {

		short width = (short) frames[0].width;
		short height = (short) frames[0].height;
		short samplesPerPixel = (short) frames.length;
		short bitsPerSample = 16;
		short compressionType = 1; //no compression
		short photometricInterpretationType = 1; //BlackIsZero
		int stripByteCount = 1280;  // example
		short resolutionUnit = 1; // No absolute unit of measurement
		short sampleFormat = 1; // unsigned integer data

		//int strip_lenght = width*height*2;

		writeHeader();

		IFD ifd = new IFD();
		ifd.add_ImageWidth(width);
		ifd.add_ImageLength(height);
		ifd.add_BitsPerSample(samplesPerPixel, bitsPerSample);
		ifd.add_Compression(compressionType);
		ifd.add_PhotometricInterpretation(photometricInterpretationType);
		ifd.add_StripOffsets();
		ifd.add_SamplesPerPixel(samplesPerPixel);
		ifd.add_RowsPerStrip(height);
		ifd.add_StripByteCounts(stripByteCount);
		ifd.add_XResolution(1,1); // TODO
		ifd.add_YResolution(1,1); // TODO
		ifd.add_ResolutionUnit(resolutionUnit);
		ifd.add_SampleFormat(samplesPerPixel, sampleFormat);

		ifd.writeTIFF(out);			

		short[][][] data = new short[frames.length][][];
		for (int i = 0; i < frames.length; i++) {
			data[i] = frames[i].data;
		}

		for(int y=0;y<height;y++) {	
			for(int x=0;x<width;x++) {
				for(int b=0;b<samplesPerPixel;b++) {
					out.writeShort(data[b][y][x]);
				}
			}
		}
	}
	
	public void write(DoubleFrame[] doubleFrames, GeoReference ref) throws IOException {
		writeWithSupplier(Arrays.stream(doubleFrames).<Supplier<DoubleFrame>>map(f->()->f).toArray(Supplier[]::new), ref);
	}

	public void writeWithSupplier(Supplier<DoubleFrame>[] frameSuppliers, GeoReference ref) throws IOException {
		DoubleFrame doubleFrame0 = frameSuppliers[0].get();
		short width = (short) doubleFrame0.width;
		short height = (short) doubleFrame0.height;
		short samplesPerPixel = (short) frameSuppliers.length;
		short bitsPerSample = 64;
		short compressionType = 1; //no compression
		short photometricInterpretationType = 1; //BlackIsZero
		int stripByteCount = width * height * 8;
		short sampleFormat = 3; // floating point data

		int[] stripByteCounts = new int[samplesPerPixel];
		for (int i = 0; i < stripByteCounts.length; i++) {
			stripByteCounts[i] = stripByteCount;			
		}

		writeHeader();

		IFD ifd = new IFD();
		ifd.add_ImageWidth(width);
		ifd.add_ImageLength(height);
		ifd.add_BitsPerSample(samplesPerPixel, bitsPerSample);
		ifd.add_Compression(compressionType);
		ifd.add_PhotometricInterpretation(photometricInterpretationType);
		ifd.add_StripOffsets(stripByteCounts);
		ifd.add_SamplesPerPixel(samplesPerPixel);
		ifd.add_RowsPerStrip(height);
		ifd.add_StripByteCounts(stripByteCounts);
		//ifd.add_XResolution(1,1); // TODO  not needed?
		//ifd.add_YResolution(1,1); // TODO  not needed?
		//ifd.add_ResolutionUnit(1); // TODO  not needed?  No absolute unit of measurement
		ifd.add_SampleFormat(samplesPerPixel, sampleFormat);
		ifd.add_PlanarConfiguration_Planar();

		ifd.add_ImageDescription("created with rsdb");
		ifd.add_Software("rsdb");
		ifd.add_DateTime_now();

		/*begin geotiff*/
		GeoKeyDirectory geoKeyDirectory = new GeoKeyDirectory();

		geoKeyDirectory.add_ModelType_ModelTypeProjected();
		geoKeyDirectory.add_RasterType_RasterPixelIsArea();
		int epsgCode = ref.getEPSG(0);
		if(0 < epsgCode  && epsgCode <= Short.MAX_VALUE) {
			geoKeyDirectory.add_ProjectedCSType((short)epsgCode);
		}
		geoKeyDirectory.add_Citation("created with rsdb");

		ifd.add_GeoKeyDirectory(geoKeyDirectory);
		ifd.add_geotiff_ModelPixelScaleTag(ref.pixel_size_x, ref.pixel_size_y);
		//double refx = ref.pixelXToGeo(doubleFrame0.local_min_x);
		//double refy = ref.pixelYToGeo(doubleFrame0.local_max_y);
		double refx = ref.pixelXToGeo(doubleFrame0.local_min_x + 0.5d); // correction
		double refy = ref.pixelYToGeo(doubleFrame0.local_max_y + 1.5d); // correction
		ifd.add_geotiff_ModelTiepointTag(0, 0, refx, refy);
		ifd.add_GDAL_NODATA((short) 0);
		/*end geotiff*/

		ifd.writeTIFF(out);

		long dataCnt = 0;
		byte[] target = null;
		for(int b=0;b<samplesPerPixel;b++) {
			DoubleFrame doubleFrame = b == 0 ? doubleFrame0 : frameSuppliers[b].get();
			double[][] data = doubleFrame.data;
			for(int y = (height - 1); y >= 0; y--) {
				double[] rowData = data[y];
				target = Serialisation.doubleToByteArrayBigEndian(rowData, target);
				out.write(target);
				dataCnt += target.length;
			}
		}
		log.info("data written "+dataCnt+"      "+ (stripByteCount * samplesPerPixel));
	}
}