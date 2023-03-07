package remotetask;

import java.io.IOException;
import java.io.OutputStream;

public class NulOutputstream extends OutputStream {
	
	public static final NulOutputstream DEFAULT = new NulOutputstream();
	
	private NulOutputstream() {}

	@Override
	public void write(int b) throws IOException {		
	}

}
