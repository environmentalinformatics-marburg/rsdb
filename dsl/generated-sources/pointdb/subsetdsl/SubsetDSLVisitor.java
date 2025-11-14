// Generated from grammars/SubsetDSL.g4 by ANTLR 4.13.2
package pointdb.subsetdsl;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SubsetDSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SubsetDSLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#region_scirpt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegion_scirpt(SubsetDSLParser.Region_scirptContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#region_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegion_sequence(SubsetDSLParser.Region_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#region}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegion(SubsetDSLParser.RegionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#bbox}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBbox(SubsetDSLParser.BboxContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#square}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSquare(SubsetDSLParser.SquareContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#point_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPoint_sequence(SubsetDSLParser.Point_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#point_sequence2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPoint_sequence2(SubsetDSLParser.Point_sequence2Context ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(SubsetDSLParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#point}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPoint(SubsetDSLParser.PointContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#poi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPoi(SubsetDSLParser.PoiContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#roi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoi(SubsetDSLParser.RoiContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#url_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUrl_sequence(SubsetDSLParser.Url_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#url}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUrl(SubsetDSLParser.UrlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#p}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitP(SubsetDSLParser.PContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(SubsetDSLParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link SubsetDSLParser#num_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNum_id(SubsetDSLParser.Num_idContext ctx);
}