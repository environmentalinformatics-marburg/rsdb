// Generated from DSL.g4 by ANTLR 4.4
package rasterdb.dsl;
import org.antlr.v4.runtime.misc.NotNull;
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
	 * Visit a parse tree produced by {@link DSLParser#seq_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_element(@NotNull DSLParser.Seq_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(@NotNull DSLParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(@NotNull DSLParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(@NotNull DSLParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange(@NotNull DSLParser.RangeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTerm(@NotNull DSLParser.TermContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#entity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity(@NotNull DSLParser.EntityContext ctx);
	/**
	 * Visit a parse tree produced by {@link DSLParser#seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq(@NotNull DSLParser.SeqContext ctx);
}