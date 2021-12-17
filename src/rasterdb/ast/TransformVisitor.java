package rasterdb.ast;

import java.util.ArrayList;


import org.tinylog.Logger;

public interface TransformVisitor extends AstVisitor<AST, AST> {
	
	
	@Override
	default AST visitSequence(AST_Sequence ast, AST parent) {
		ArrayList<AST> list = new ArrayList<>();
		if(ast.asts.size() == 1) {
			return ast.asts.get(0).accept(this, parent);
		}
		for(AST e:ast.asts) {
			AST node = e.accept(this, ast);
			if(node instanceof AST_Sequence) {
				AST_Sequence seq = (AST_Sequence) node;
				for(AST sub:seq.asts) {
					list.add(sub);
				}
			} else {
				list.add(node);
			}
		}
		return new AST_Sequence(list);
	}

	@Override
	default AST visitFunction(AST_function ast, AST parent) {
		ArrayList<AST> list = new ArrayList<>();
		for(AST e:ast.asts) {
			AST node = e.accept(this, ast);
			list.add(node);
		}
		return new AST_function(ast.name, list);
	}

	@Override
	default AST visitRadiation(AST_radiance ast, AST parent) {
		return ast;
	}

	@Override
	default AST visitBandNumber(AST_Band_number ast, AST parent) {
		return ast;
	}

	@Override
	default AST visitConstant(AST_Constant ast, AST parent) {
		return ast;
	}
}
