package util.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;


import org.tinylog.Logger;

public class ImageGrey implements PureImage {
	@SuppressWarnings("unused")
	
	
	protected final int width;
	protected final int height;

	protected final byte[] imageBuffer;
	protected final BufferedImage bufferedImage_TYPE_BYTE_GRAY;
	
	public ImageGrey(int width, int height) {
		this.width = width;
		this.height = height;		
		this.bufferedImage_TYPE_BYTE_GRAY = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		this.imageBuffer = ((DataBufferByte) bufferedImage_TYPE_BYTE_GRAY.getRaster().getDataBuffer()).getData();
	}

	public byte[] getRawArray() {
		return imageBuffer;
	}
	
	@Override
	public BufferedImage getBufferedImage() {
		return bufferedImage_TYPE_BYTE_GRAY;
	}
	
	/**
	 * 
	 * @param out
	 * @param quality 0.0 to 1.0
	 * @throws IOException
	 */
	@Override
	public void writeJpg(OutputStream out, float quality) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwp.setCompressionQuality(quality);
		writer.setOutput(ImageIO.createImageOutputStream(out));		
		IIOImage image = new IIOImage(bufferedImage_TYPE_BYTE_GRAY, null, null);
		writer.write(null, image, iwp);
		writer.dispose();
	}

	/**
	 * with default compression (parameter level ignored)
	 */
	@Override
	public void writePng(OutputStream out, int level) throws IOException {
		ImageIO.write(getBufferedImage(), "png", out);		
	}

	@Override
	public void writePngCompressed(OutputStream out) throws IOException {
		writePng(out, 1);		
	}
}
