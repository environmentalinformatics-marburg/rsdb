package rasterdb.dsl;

import java.util.ArrayList;

import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ErrorCollector extends DiagnosticErrorListener {
	
	
	public ArrayList<String> lines = new ArrayList<String>();
	
	public ErrorCollector() {
		super(false);
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		lines.add("syntax error at line " + line + " column " + charPositionInLine + ": " + msg);
	}

}
