package util;

import java.util.Iterator;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.spi.ImageWriterSpi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpiUtil {
	static final Logger log = LogManager.getLogger();

	private static final IIORegistry IIO_REGISTRY = IIORegistry.getDefaultInstance();
	public static final ImageWriterSpi JPEG_IMAGE_WRITER_SPI = getJPEG_ImageWriterSpi();
	public static final ImageOutputStreamSpi OUTPUT_STREAM_IMAGE_OUTPUT_STREAM = get_ImageOutputStreamSpi();
	
	private static ImageWriterSpi getJPEG_ImageWriterSpi() {
		Iterator<ImageWriterSpi> it = IIO_REGISTRY.getServiceProviders(ImageWriterSpi.class, false);
		while(it.hasNext()) {
			ImageWriterSpi imageWriterSpi = it.next();
			String[] formatNames = imageWriterSpi.getFormatNames();
			if(formatNames != null) {
				for(String formatName:formatNames) {
					if("JPEG".equals(formatName)) {
						return imageWriterSpi;
					}
				}
			}
						
		}
		log.error("no JPEG ImageWriterSpi found");
		return null;
	}
	
	private static ImageOutputStreamSpi get_ImageOutputStreamSpi() {
		Iterator<ImageOutputStreamSpi> it = IIO_REGISTRY.getServiceProviders(ImageOutputStreamSpi.class, false);
		while(it.hasNext()) {
			ImageOutputStreamSpi imageOutputStreamSpi = it.next();
			if(imageOutputStreamSpi.getOutputClass().isAssignableFrom(java.io.OutputStream.class)) {
				return imageOutputStreamSpi;
			}
		}
		log.error("no ImageOutputStreamSpi found");
		return null;
	}
	
}
