// Generated from LidarIndicesDSL.g4 by ANTLR 4.4
package pointdb.lidarindicesdsl;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link LidarIndicesDSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface LidarIndicesDSLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link LidarIndicesDSLParser#index_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_sequence(@NotNull LidarIndicesDSLParser.Index_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link LidarIndicesDSLParser#index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex(@NotNull LidarIndicesDSLParser.IndexContext ctx);
	/**
	 * Visit a parse tree produced by {@link LidarIndicesDSLParser#index_scirpt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_scirpt(@NotNull LidarIndicesDSLParser.Index_scirptContext ctx);
}