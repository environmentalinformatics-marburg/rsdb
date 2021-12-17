package rasterdb.dsl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import org.tinylog.Logger;

import rasterdb.BindVisitor;
import rasterdb.BandProcessor;
import rasterdb.RasterDB;
import rasterdb.ast.AST;
import rasterdb.ast.MacroVisitor;
import rasterdb.ast.StringVisitor;
import rasterdb.node.CompileVisitor;
import rasterdb.node.ProcessorNode;
import util.frame.DoubleFrame;

public class DSL {
	

	public static AST parse(String script, ErrorCollector errorCollector) {
		try {
			DSLLexer lexer = new DSLLexer(CharStreams.fromString(script, "script"));
			lexer.addErrorListener(errorCollector);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			DSLParser parser = new DSLParser(tokens);
			parser.addErrorListener(errorCollector);
			AST ast = parser.expression().accept(ExpressionVisitor.DEFAULT);
			return ast;
		} catch (Exception e) {
			errorCollector.lines.add(e.toString());
			return null;
		}
	}

	public static AST unify(AST astParsed) {
		return astParsed.accept(MacroVisitor.DEFAULT, null);			
	}
	
	public static AST parse_unify(String script, ErrorCollector errorCollector) {
		AST astParsed = parse(script, errorCollector);
		return unify(astParsed);
	}
	
	public static AST parse_unify(String script) {
		ErrorCollector errorCollector = new ErrorCollector();
		AST astUnified = parse_unify(script, errorCollector);
		for(String line:errorCollector.lines) {
			Logger.warn(line);
		}
		return astUnified;
	} 

	public static AST bind(AST astUnified, RasterDB rasterdb) {
		return astUnified.accept(new BindVisitor(rasterdb), null);		
	}

	public static ProcessorNode compileToProcessorNode(AST astBound, BandProcessor processor) {
		return astBound.accept(new CompileVisitor(processor));		
	}

	public static ProcessorNode parse_unify_bind_compileToProcessorNode(String script, ErrorCollector errorCollector, BandProcessor processor) {
		AST astUnified = parse_unify(script, errorCollector);
		AST astBound = bind(astUnified, processor.rasterdb);
		return compileToProcessorNode(astBound, processor);		
	}
	
	public static DoubleFrame[] process(String script, ErrorCollector errorCollector, BandProcessor processor) {
		ProcessorNode processorNode = DSL.parse_unify_bind_compileToProcessorNode(script, errorCollector, processor);
		return processorNode.process(processor);
	}
	
	public static String toString(AST ast) {
		return ast.accept(StringVisitor.DEFAULT, AST.UNKNOWN);
	}
}
