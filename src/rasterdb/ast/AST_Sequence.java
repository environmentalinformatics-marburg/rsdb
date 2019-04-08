package rasterdb.ast;

import java.util.List;

public class AST_Sequence implements AST {
	public final List<AST> asts;
	public AST_Sequence(List<AST> asts) {
		this.asts = asts;
	}	
	@Override
	public <T,P> T accept(AstVisitor<T,P> visitor, P param) {
		return visitor.visitSequence(this, param);
	}
}