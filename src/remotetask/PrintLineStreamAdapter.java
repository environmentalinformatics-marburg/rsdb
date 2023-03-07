package remotetask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Consumer;

public class PrintLineStreamAdapter extends PrintStream {

	private Consumer<String> printlnConsumer = s -> {};	

	public PrintLineStreamAdapter() {
		super(NulOutputstream.DEFAULT);
	}

	public PrintLineStreamAdapter(Consumer<String> c) {
		super(NulOutputstream.DEFAULT);
		setPrintlnConsumer(c);
	}

	public PrintLineStreamAdapter(File file) throws FileNotFoundException {
		super(file);
	}

	@Override
	public void println(String s) {
		printlnConsumer.accept(s);
	}

	public void setPrintlnConsumer(Consumer<String> c) {
		Objects.requireNonNull(c);
		this.printlnConsumer = c;
	}

	@Override
	public void println(Object obj) {
		String s = String.valueOf(obj);
		println(s);
	}
}
