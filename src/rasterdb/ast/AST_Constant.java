package rasterdb.ast;

public class AST_Constant implements AST {
	public final double constant;
	public AST_Constant(double constant) {
		this.constant = constant;
	}
	@Override
	public <T,P> T accept(AstVisitor<T,P> visitor, P param) {
		return visitor.visitConstant(this, param);
	}
}