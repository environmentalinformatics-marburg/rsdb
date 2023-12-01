package util.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;


import org.tinylog.Logger;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import util.SpiUtil;
import util.Timer;

public class ImageBufferARGB implements PureImage {
	

	public final int width;
	public final int height;
	public final BufferedImage bufferedImage; // TYPE_INT_ARGB
	public final int[] data;

	public ImageBufferARGB(int width, int height) {
		this(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
	}

	public ImageBufferARGB(BufferedImage bufferedImage) {
		this.width = bufferedImage.getWidth();
		this.height = bufferedImage.getHeight();
		this.bufferedImage = bufferedImage;
		if(bufferedImage.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new RuntimeException("wrong image type");
		}
		WritableRaster raster = bufferedImage.getRaster();
		DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
		this.data = dataBuffer.getData();
	}

	/**
	 * write png with fast good compression
	 * @param out
	 */
	@Override
	public void writePngCompressed(OutputStream out) {
		writePngCompressed(out, 1);
	}

	private static class ImageLineWrapperARGB implements IImageLine {

		private final int[] data;
		private final int width;
		public int currentLine = 0;

		public ImageLineWrapperARGB(int[] data, int width) {
			this.data = data;
			this.width = width;
		}

		@Override
		public void readFromPngRaw(byte[] raw, int len, int offset, int step) {
			throw new RuntimeException("not implemented");

		}

		@Override
		public void endReadFromPngRaw() {
			throw new RuntimeException("not implemented");

		}

		@Override
		public void writeToPngRaw(byte[] raw) {
			int[] src = data;
			raw[0] = -100; // FilterType.FILTER_UNKNOWN
			int srcPos = currentLine * width;
			int dstPos = 1;
			int w = width;
			for(int j = 0; j < w; j++) {
				int v = src[srcPos++];
				raw[dstPos + 3] = (byte) (v >> 24); // alpha
				raw[dstPos] = (byte) (v >> 16); // blue
				raw[dstPos + 1] = (byte) (v >> 8); // green
				raw[dstPos + 2] = (byte) v; // red
				dstPos += 4;					
			}			
		}		
	}

	private static class ImageLineWrapperAlphaGrey implements IImageLine {

		private final int[] data;
		private final int width;
		public int currentLine = 0;

		public ImageLineWrapperAlphaGrey(int[] data, int width) {
			this.data = data;
			this.width = width;
		}

		@Override
		public void readFromPngRaw(byte[] raw, int len, int offset, int step) {
			throw new RuntimeException("not implemented");

		}

		@Override
		public void endReadFromPngRaw() {
			throw new RuntimeException("not implemented");

		}

		@Override
		public void writeToPngRaw(byte[] raw) {
			int[] src = data;
			raw[0] = -100; // FilterType.FILTER_UNKNOWN
			int srcPos = currentLine * width;
			int dstPos = 1;
			int w = width;
			for(int j = 0; j < w; j++) {
				int v = src[srcPos++];
				raw[dstPos + 1] = (byte) (v >> 24); // alpha
				raw[dstPos] = (byte) (v >> 16); // blue ==> grey
				dstPos += 2;					
			}			
		}		
	}

	/**
	 * write png with compression and fast filtering (FILTER_ADAPTIVE_FAST).
	 * @param out
	 * @param level no compression 0,  few compression 1 to best compression 9
	 */
	public void writePngCompressed(OutputStream out, int level) {
		switch(level) {
		case 0: {
			/*PngWriter pngWriter = new PngWriter(out, new ImageInfo(width, height, 8, true));
			PixelsWriter pixelsWriter = pngWriter.getPixelsWriter();
			pixelsWriter.setFilterType(FilterType.FILTER_NONE);
			pixelsWriter.setDeflaterCompLevel(0);
			ImageLineWrapperARGB imageLineWrapper = new ImageLineWrapperARGB(data, width);
			for(int i=0;i<height;i++) {
				imageLineWrapper.currentLine = i;
				pngWriter.writeRow(imageLineWrapper);
			}
			pngWriter.end();*/
			try {
				util.png.PngWriter.write(new DataOutputStream(out), this);
			} catch (IOException e) {
				Logger.warn(e);
			}
			break;
		}
		default: {
			PngWriter pngWriter = new PngWriter(out, new ImageInfo(width, height, 8, true));
			pngWriter.setCompLevel(level);
			pngWriter.setFilterType(FilterType.FILTER_ADAPTIVE_FAST);
			ImageLineWrapperARGB imageLineWrapper = new ImageLineWrapperARGB(data, width);
			for(int i=0;i<height;i++) {
				imageLineWrapper.currentLine = i;
				pngWriter.writeRow(imageLineWrapper);
			}
			pngWriter.end();
		}
		}
	}

	/**
	 * write png with compression and fast filtering (FILTER_ADAPTIVE_FAST).
	 * @param out
	 * @param level no compression 0,  few compression 1 to best compression 9
	 */
	public void writePngCompressedAlphaGrey(OutputStream out, int level) {
		PngWriter pngWriter = new PngWriter(out, new ImageInfo(width, height, 8, true, true, false));
		pngWriter.setCompLevel(level);
		pngWriter.setFilterType(FilterType.FILTER_ADAPTIVE_FAST);
		ImageLineWrapperAlphaGrey imageLineWrapper = new ImageLineWrapperAlphaGrey(data, width);
		for(int i=0;i<height;i++) {
			imageLineWrapper.currentLine = i;
			pngWriter.writeRow(imageLineWrapper);
		}
		pngWriter.end();
	}

	public Graphics2D createGraphics() {
		return bufferedImage.createGraphics();
	}

	public ImageBufferARGB scaled(int width, int height) {
		Timer.start("scale");
		ImageBufferARGB target = new ImageBufferARGB(width, height);
		Graphics2D g = target.createGraphics();
		if(width <= this.width) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		} else {
			//g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			//g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		}
		if( !g.drawImage(bufferedImage, 0, 0, width, height, null) ) {
			Logger.warn("image not drawn fully");			
		}
		g.dispose();
		target.bufferedImage.flush();
		//Logger.info(Timer.stop("scale"));
		return target;
	}

	/**
	 * TYPE_INT_ARGB
	 */
	@Override
	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	private static final DirectColorModel COLOR_MODEL_RGB = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);
	private static final int[] MASKS_RGB = COLOR_MODEL_RGB.getMasks();

	/**
	 * direct convert to TYPE_INT_RGB
	 * @return
	 */
	public BufferedImage getWrappedBufferedImageRGB() {
		/*BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		dstImage.setRGB(0, 0, width, height, data, 0, width);*/

		/*BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		DataBufferInt dstDataBuffer = (DataBufferInt) dstImage.getRaster().getDataBuffer();
		int[] dstData = dstDataBuffer.getData();
		System.arraycopy(data, 0, dstData, 0, data.length);*/

		WritableRaster dstRaster = Raster.createPackedRaster(bufferedImage.getRaster().getDataBuffer(), width, height, width, MASKS_RGB, null);		
		BufferedImage dstImage = new BufferedImage(COLOR_MODEL_RGB, dstRaster, false, null);
		return dstImage;
	}

	@Override
	public void writeJpg(OutputStream out, float quality) throws IOException {		
		BufferedImage dstImage = getWrappedBufferedImageRGB();		

		//ImageIO.write(dstImage, "jpg", out);		

		ImageWriter jpgWriter = SpiUtil.JPEG_IMAGE_WRITER_SPI.createWriterInstance();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(quality);
		//ImageOutputStream imageOut = ImageIO.createImageOutputStream(out);
		ImageOutputStream imageOut = SpiUtil.OUTPUT_STREAM_IMAGE_OUTPUT_STREAM.createOutputStreamInstance(out);
		jpgWriter.setOutput(imageOut);
		jpgWriter.write(null, new IIOImage(dstImage, null, null), jpgWriteParam);
	}

	@Override
	public void writePng(OutputStream out, int level) throws IOException {
		writePngCompressed(out, level);		
	}

}
