package rasterdb.dsl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.ast.AST;
import rasterdb.ast.AST_Band_number;
import rasterdb.ast.AST_Constant;
import rasterdb.ast.AST_Sequence;
import rasterdb.ast.AST_function;
import rasterdb.ast.AST_radiance;
import rasterdb.dsl.DSLParser.ConstantContext;
import rasterdb.dsl.DSLParser.EntityContext;
import rasterdb.dsl.DSLParser.ExpressionContext;
import rasterdb.dsl.DSLParser.FactorContext;
import rasterdb.dsl.DSLParser.FunctionContext;
import rasterdb.dsl.DSLParser.RangeContext;
import rasterdb.dsl.DSLParser.SeqContext;
import rasterdb.dsl.DSLParser.Seq_elementContext;
import rasterdb.dsl.DSLParser.TermContext;

public class ExpressionVisitor extends DSLBaseVisitor<AST> {
	static final Logger log = LogManager.getLogger();
	public static final ExpressionVisitor DEFAULT = new ExpressionVisitor();
	
	@Override
	public AST visitExpression(ExpressionContext ctx) {
		//log.info("Expression "+ctx.getText());
		Iterator<TermContext> termIt = ctx.term().iterator();
		Iterator<TerminalNode> opIt = ctx.PLUS_MINUS().iterator();

		AST current = termIt.next().accept(ExpressionVisitor.DEFAULT);
		while(opIt.hasNext()) {
			String op = opIt.next().getText();
			switch(op) {
			case "+":
				op = "add";
				break;

			case "-":
				op = "sub";
				break;
			}
			AST second = termIt.next().accept(ExpressionVisitor.DEFAULT);
			current = new AST_function(op, current, second);
		}
		return current;
	}
	
	@Override
	public AST visitTerm(TermContext ctx) {
		//log.info("Term "+ctx.getText());
		Iterator<FactorContext> factorIt = ctx.factor().iterator();
		Iterator<TerminalNode> opIt = ctx.MUL_DIV().iterator();

		AST current = factorIt.next().accept(ExpressionVisitor.DEFAULT);
		while(opIt.hasNext()) {
			String op = opIt.next().getText();
			switch(op) {
			case "*":
				op = "mul";
				break;

			case "/":
				op = "div";
				break;
			}
			AST second = factorIt.next().accept(ExpressionVisitor.DEFAULT);
			current = new AST_function(op, current, second);
		}
		return current;
	}
	
	@Override
	public AST visitFactor(FactorContext ctx) {
		AST base = ctx.base.accept(ExpressionVisitor.DEFAULT);
		if(ctx.POW() == null) {
			return base;
		} else {
			AST exponent = ctx.exponent.accept(ExpressionVisitor.DEFAULT);
			String op = "pow";
			return new AST_function(op, base, exponent);
		}
	}
	
	@Override
	public AST visitEntity(EntityContext ctx) {
		for(ParseTree c:ctx.children) {
			AST ast = c.accept(ExpressionVisitor.DEFAULT);
			if(ast!=null) {
				return ast;
			}
		}
		log.warn("error in visitEntity");
		return null;
	}		

	@Override
	public AST visitFunction(FunctionContext ctx) {
		String text = ctx.ID().getText();
		List<ExpressionContext> expressionList = ctx.expression();			
		ArrayList<AST> asts = new ArrayList<>();
		if(expressionList.isEmpty()) {
			AST ast = parseBandSpec(text);
			if(ast != null) {
				return ast;
			}
		} else {
			for(ExpressionContext expression:expressionList) {
				AST ast = expression.accept(ExpressionVisitor.DEFAULT);
				asts.add(ast);
			}
		}
		return new AST_function(text, asts);
	}

	public static AST parseBandSpec(String text) {
		if(text.length()>1) {
			char c0 = text.charAt(0);
			char c1 = text.charAt(1);
			if((c0 == 'b' || c0 == 'r') && c1 >= '0' && c1 <= '9') {
				int parameter = Integer.parseInt(text.substring(1));
				if(c0 == 'b') {
					return new AST_Band_number(parameter);
				}
				if(c0 == 'r') {
					return new AST_radiance(parameter);
				}
			}
		}
		return null;
	}

	@Override
	public AST visitRange(RangeContext ctx) {
		AST min = parseBandSpec(ctx.min.getText());
		AST max = parseBandSpec(ctx.max.getText());			
		return new AST_function("range", min, max);
	}

	@Override
	public AST visitTerminal(TerminalNode node) {
		//nothing
		return null;
	}
	@Override
	protected AST defaultResult() {
		log.warn("unknown part");
		new Throwable().printStackTrace();
		return null;
	}
	@Override
	public AST visitErrorNode(ErrorNode node) {
		throw new RuntimeException("error at: '"+node.getText()+"'");
	}
	@Override
	public AST visitConstant(ConstantContext ctx) {
		String constantText = ctx.getText();
		double constant = Double.parseDouble(constantText);
		return new AST_Constant(constant);
	}
	@Override
	public AST visitSeq(SeqContext ctx) {
		ArrayList<AST> list = new ArrayList<>();
		for(Seq_elementContext e:ctx.seq_element()) {
			AST node = e.accept(ExpressionVisitor.DEFAULT);
			list.add(node);
		}
		return new AST_Sequence(list);
	}
	@Override
	public AST visitSeq_element(Seq_elementContext ctx) {
		if(ctx.children.size() != 1) {
			throw new RuntimeException();
		}
		return ctx.children.get(0).accept(ExpressionVisitor.DEFAULT);
	}
}