package pointdb.lidarindicesdsl;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import pointdb.lidarindicesdsl.LidarIndicesDSLParser.IndexContext;
import pointdb.lidarindicesdsl.LidarIndicesDSLParser.Index_scirptContext;
import pointdb.lidarindicesdsl.LidarIndicesDSLParser.Index_sequenceContext;
import util.collections.vec.Vec;

public class Compiler {
	//private static final Logger log = LogManager.getLogger();
	
	public Compiler() {
	}
	
	public Vec<String> parse(String script) {		
		ANTLRInputStream in = new ANTLRInputStream(script);
		LidarIndicesDSLLexer lexer = new LidarIndicesDSLLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		LidarIndicesDSLParser parser = new LidarIndicesDSLParser(tokens);
		return parser.index_scirpt().accept(IndexVisitor.DEFAULT);	
	}
	
	private static class IndexVisitor extends LidarIndicesDSLBaseVisitor<Vec<String>> {
		public static final IndexVisitor DEFAULT = new IndexVisitor();

		@Override
		public Vec<String> visitIndex_sequence(Index_sequenceContext ctx) {
			Vec<String> result = new Vec<String>();
			for(IndexContext index:ctx.index()) {
				String name = index.ID().getText();
				result.add(name);
			}
			return result;
		}

		@Override
		public Vec<String> visitIndex_scirpt(Index_scirptContext ctx) {
			return ctx.index_sequence().accept(this);
		}		
	}

}
