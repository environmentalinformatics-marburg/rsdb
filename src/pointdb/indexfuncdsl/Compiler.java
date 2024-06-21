package pointdb.indexfuncdsl;

import java.util.HashMap;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.tinylog.Logger;

import pointdb.indexfuncdsl.IndexFuncDSLParser.Index_funcContext;
import pointdb.indexfuncdsl.IndexFuncDSLParser.ParamContext;
import pointdb.indexfuncdsl.IndexFuncDSLParser.Param_sequenceContext;
import pointdb.indexfuncdsl.IndexFuncDSLParser.ValueContext;
import pointdb.process.AbstractProcessingFun;
import pointdb.process.ParamProcessingFun;
import pointdb.process.ProcessingFun;

public class Compiler {

	/**
	 * Derived from https://stackoverflow.com/questions/18132078/handling-errors-in-antlr4   https://stackoverflow.com/a/26573239
	 *
	 */
	public static class ThrowingErrorListener extends BaseErrorListener {

		public static final ThrowingErrorListener DEFAULT = new ThrowingErrorListener();

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
				throws ParseCancellationException {
			throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
		}
	}

	public static ProcessingFun compile(String text) {
		try {
			Logger.info("compile |" + text + "|");
			IndexFuncDSLLexer indexFuncDSLLexer = new IndexFuncDSLLexer(CharStreams.fromString(text));
			indexFuncDSLLexer.removeErrorListeners();
			indexFuncDSLLexer.addErrorListener(ThrowingErrorListener.DEFAULT);
			BufferedTokenStream bufferedTokenStream = new BufferedTokenStream(indexFuncDSLLexer);
			IndexFuncDSLParser indexFuncDSLParser = new IndexFuncDSLParser(bufferedTokenStream);
			//indexFuncDSLParser.setErrorHandler(new BailErrorStrategy()); // does not include error message
			indexFuncDSLParser.removeErrorListeners();
			indexFuncDSLParser.addErrorListener(ThrowingErrorListener.DEFAULT);
			Index_funcContext index_func = indexFuncDSLParser.index_func();
			String funcName = index_func.func_name.getText();
			Logger.info("func_name " + funcName);
			HashMap<String, ValueContext> paramMap = new HashMap<String, ValueContext>();
			Param_sequenceContext params = index_func.params;
			if(params != null) {
				Logger.info("params " + params.param());
				for(ParamContext param : params.param()) {
					String paramName = param.param_name.getText();
					ValueContext paramValue = param.param_value;
					Logger.info("param_name " + paramName);
					Logger.info("param_value " + paramValue.getText());
					paramMap.put(paramName, paramValue);
				}
			}
			return compileFunc(funcName, paramMap);
		} catch(Exception e) {
			Logger.warn(e.getMessage());
			throw e;
		}
	}

	private static ProcessingFun compileFunc(String funcName, HashMap<String, ValueContext> paramMap) {
		AbstractProcessingFun fun = pointdb.process.Functions.getFun(funcName);
		if(fun instanceof ProcessingFun) {
			if(paramMap.isEmpty()) {
				return (ProcessingFun) fun;
			} else {
				throw new RuntimeException("parameters not expected for function");
			}
		} else  if(fun instanceof ParamProcessingFun) {
			return ((ParamProcessingFun) fun).instantiate(paramMap);
		} else {
			throw new RuntimeException("unknown type for function");
		}
	}
}
