package pointdb.process;

import java.util.HashMap;

import pointdb.indexfuncdsl.IndexFuncDSLParser.ValueContext;

public abstract class ParamProcessingFun extends AbstractProcessingFun {

	public abstract ProcessingFun instantiate(HashMap<String, ValueContext> paramMap);
}
