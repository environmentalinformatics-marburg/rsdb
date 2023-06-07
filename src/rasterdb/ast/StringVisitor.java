package rasterdb.ast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class StringVisitor implements AstVisitor<String, Integer> {
	
	public static final StringVisitor DEFAULT = new StringVisitor();
	private static final String SEPERATOR = ", ";
	private StringVisitor() {}
	@Override
	public String visitRadiation(AST_radiance ast, Integer parentP) {
		return "r"+ast.nm;
	}
	@Override
	public String visitSequence(AST_Sequence ast, Integer parentP) {
		String s="[";		
		boolean first = true;
		for(AST e:ast.asts) {
			if(first) {
				first = false;
			} else {
				s += SEPERATOR;
			}
			s += e==null?"null":e.accept(this, ast.getPriority());
		}
		return s + "]";
	}
	@Override
	public String visitBandNumber(AST_Band_number ast, Integer parentP) {
		return "b"+ast.number;
	}
	@Override
	public String visitFunction(AST_function ast, Integer parentP) {
		switch(ast.name) {
		case "add":
		case "sub":
		case "div":
		case "mul":
		case "pow":
			if(ast.asts.size()==2) {
				String a = ast.asts.get(0).accept(this, ast.getPriority()-1);
				String b = ast.asts.get(1).accept(this, ast.getPriority());
				String op = " "+ast.name+" ";
				switch(ast.name) {
				case "add":
					op = "+";
					break;
				case "sub":
					op = "-";
					break;
				case "mul":
					op = "*";
					break;
				case "div":
					op = "/";
					break;	
				case "pow":
					op = "^";
					break;	
				}
				int thisP = ast.getPriority();
				//Logger.info("prio "+parentP+"  "+thisP);
				if(parentP < thisP) {
					return a+" "+op+" "+b;	
				} else { 
					return "("+a+" "+op+" "+b+")";
				}
			}
			break;
		}

		String s = ast.name;
		if(ast.asts.isEmpty()) {
			//nothing
		} else {
			s += "(";
			boolean first = true;
			for(AST e:ast.asts) {
				if(first) {
					first = false;
				} else {
					s += SEPERATOR;
				}
				s += e.accept(this, ast.getPriority());
			}
			s += ")";
		}
		return s;
	}
	
	private static final DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	static {
		df.setMaximumFractionDigits(340);
	}
	@Override
	public String visitConstant(AST_Constant ast, Integer param) {
		return df.format(ast.constant);
	}
}
