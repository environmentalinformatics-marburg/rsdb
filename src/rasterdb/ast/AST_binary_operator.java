package rasterdb.ast;

public abstract class AST_binary_operator implements AST {
	public final AST left;
	public final AST right;
	public final int op;
	public AST_binary_operator(AST left, int op, AST right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}
}