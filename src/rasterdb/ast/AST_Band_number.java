package rasterdb.ast;

public class AST_Band_number implements AST {
	public final int number;
	public AST_Band_number(int number) {
		this.number = number;
	}
	@Override
	public <T,P> T accept(AstVisitor<T,P> visitor, P param) {
		return visitor.visitBandNumber(this, param);
	}
}