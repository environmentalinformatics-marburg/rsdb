// Generated from DSL.g4 by ANTLR 4.7
package rasterdb.dsl;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DSLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DSLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(DSLParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTerm(DSLParser.TermContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#entity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity(DSLParser.EntityContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq(DSLParser.SeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(DSLParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(DSLParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#seq_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_element(DSLParser.Seq_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange(DSLParser.RangeContext ctx);
}