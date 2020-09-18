package rasterdb.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AST_function implements AST {

	public final String name;
	public final List<AST> asts;

	public AST_function(String name) {
		this(name, new ArrayList<>());
	}

	public AST_function(String name, AST ...asts) {
		this(name, Arrays.asList(asts));
	}

	public AST_function(String name, List<AST> asts) {
		this.name = name;
		this.asts = asts;
	}

	@Override
	public <T,P> T accept(AstVisitor<T,P> visitor, P param) {
		return visitor.visitFunction(this, param);
	}

	@Override
	public int getPriority() {
		switch(name) {
		case "add":
		case "sub":
			return AST.PRIO_ADD_SUB;
		case "mul":
		case "div":
			return AST.PRIO_MUL_DIV;
		case "pow":
			return AST.PRIO_POW;			
		default:
			return AST.UNKNOWN;
		}
	}
}
