// Generated from grammars/LidarIndicesDSL.g4 by ANTLR 4.13.2
package pointdb.lidarindicesdsl;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

/**
 * This class provides an empty implementation of {@link LidarIndicesDSLVisitor},
 * which can be extended to create a visitor which only needs to handle a subset
 * of the available methods.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
@SuppressWarnings("CheckReturnValue")
public class LidarIndicesDSLBaseVisitor<T> extends AbstractParseTreeVisitor<T> implements LidarIndicesDSLVisitor<T> {
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public T visitIndex_scirpt(LidarIndicesDSLParser.Index_scirptContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public T visitIndex_sequence(LidarIndicesDSLParser.Index_sequenceContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public T visitIndex(LidarIndicesDSLParser.IndexContext ctx) { return visitChildren(ctx); }
}