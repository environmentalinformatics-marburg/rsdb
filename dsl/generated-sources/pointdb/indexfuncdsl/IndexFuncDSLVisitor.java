// Generated from grammars/IndexFuncDSL.g4 by ANTLR 4.13.2
package pointdb.indexfuncdsl;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link IndexFuncDSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface IndexFuncDSLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link IndexFuncDSLParser#index_func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_func(IndexFuncDSLParser.Index_funcContext ctx);
	/**
	 * Visit a parse tree produced by {@link IndexFuncDSLParser#param_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_sequence(IndexFuncDSLParser.Param_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link IndexFuncDSLParser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(IndexFuncDSLParser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link IndexFuncDSLParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(IndexFuncDSLParser.IdContext ctx);
	/**
	 * Visit a parse tree produced by {@link IndexFuncDSLParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(IndexFuncDSLParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link IndexFuncDSLParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(IndexFuncDSLParser.NumberContext ctx);
}