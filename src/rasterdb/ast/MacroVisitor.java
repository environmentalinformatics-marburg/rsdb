package rasterdb.ast;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.dsl.DSL;

public class MacroVisitor implements TransformVisitor {
	private static final Logger log = LogManager.getLogger();
	public static final MacroVisitor DEFAULT = new MacroVisitor();
	
	private final static String ndvi_text = "(r800 - r680) / (r800 + r680)";
	private final static String evi_text = "2.5 * (r800 - r670) / (r800 + 6 * r670 - 7.5 * r475 + 1)";
	private final static String evi2_text = "2.5 * ((r800 - r680) / (r800 + r680 + 1))";
	private final static String savi_text = "(r800 - r670) / (r800 + r670 + 0.5) * (1 + 0.5)";
	private static AST ndvi_AST = null;
	private static AST evi_AST = null;
	private static AST evi2_AST = null;
	private static AST savi_AST = null;
	private static AST full_spectrum_AST = DSL.parse_unify("[r0 : r10000]");
	
	static {
		ndvi_AST = DSL.parse_unify(ndvi_text);
		evi_AST = DSL.parse_unify(evi_text);
		evi2_AST = DSL.parse_unify(evi2_text);
		savi_AST = DSL.parse_unify(savi_text);
		
		log.info(DSL.toString(ndvi_AST));
		log.info(DSL.toString(evi_AST));
		log.info(DSL.toString(evi2_AST));
		log.info(DSL.toString(savi_AST));
	}
	
	private MacroVisitor() {}

	@Override
	public AST visitFunction(AST_function ast, AST parent) {
		ArrayList<AST> list = new ArrayList<>();
		for(AST e:ast.asts) {
			AST node = e.accept(this, ast);
			list.add(node);
		}
		//System.out.println("visit function "+ast.name);
		switch(ast.name) {
		case "ndvi":
			//return new AST_function("div",new AST_function("sub",new AST_radiance(800), new AST_radiance(680)),new AST_function("add", new AST_radiance(800), new AST_radiance(680)));
			return ndvi_AST;
		case "evi":
			//return new AST_function("_evi", new AST_radiance(800), new AST_radiance(670), new AST_radiance(475));
			return evi_AST;
		case "evi2":
			/*AST_function top = new AST_function("sub",new AST_radiance(800), new AST_radiance(670));
			AST_function cRed = new AST_function("mul", new AST_radiance(670), new AST_Constant(2.4));
			AST_function bottom = new AST_function("add", new AST_function("add", new AST_radiance(800), cRed), new AST_Constant(1.0));
			AST_function res = new AST_function("div", top, bottom);
			return new AST_function("mul", res, new AST_Constant(2.5));*/
			return evi2_AST;
		case "savi":
			return savi_AST;
		case "full_spectrum":
			return full_spectrum_AST;
		default:
			if(ast.asts.size() == 2) {
				switch(ast.name) {
				case "add":
				case "sub":
				case "mul":
				case "div":
					AST a = ast.asts.get(0);
					AST b = ast.asts.get(1);
					if(a instanceof AST_Constant && b instanceof AST_Constant) {
						double x = ((AST_Constant)a).constant;
						double y = ((AST_Constant)b).constant;
						switch(ast.name) {
						case "add": return new AST_Constant(x + y).accept(this, parent);
						case "sub": return new AST_Constant(x - y).accept(this, parent);
						case "mul": return new AST_Constant(x * y).accept(this, parent);
						case "div": return new AST_Constant(x / y).accept(this, parent);
						}
					}
				}
			}
			return new AST_function(ast.name, list);
		}
	}

}
