package rasterdb.ast;

public class AST_radiance implements AST {
	public final int nm;
	public AST_radiance(int nm) {
		this.nm = nm;
	}
	@Override
	public <T,P> T accept(AstVisitor<T,P> visitor, P param) {
		return visitor.visitRadiation(this, param);
	}
}