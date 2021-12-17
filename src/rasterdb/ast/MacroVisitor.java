package rasterdb.ast;

import java.util.ArrayList;
import java.util.TreeMap;


import org.tinylog.Logger;

import rasterdb.dsl.DSL;

public class MacroVisitor implements TransformVisitor {
	
	public static final MacroVisitor DEFAULT = new MacroVisitor();

	private final static TreeMap<String, String> compositeFormulaMap = new TreeMap<String, String>();
	private final static TreeMap<String, AST> compositeAstMap = new TreeMap<String, AST>();

	private static void add(String name, String formula) {
		try {
			Logger.info(name);
			AST ast = DSL.parse_unify(formula);
			Logger.info(DSL.toString(ast));
			compositeFormulaMap.put(name, formula);
			compositeAstMap.put(name, ast);
		} catch(Exception e) {
			Logger.warn("could not create formula for " + name +"   " + e);
		}
	}
	
	static {
		/*
		https://de.wikipedia.org/wiki/Zapfen_%28Auge%29#/media/File:Cone-response-de.svg
		red = 564
		green = 534
		blue = 420
		 */
		
		add("ndvi", "(r800 - r680) / (r800 + r680)");
		add("evi", "2.5 * (r800 - r670) / (r800 + 6 * r670 - 7.5 * r475 + 1)");
		add("evi2", "2.5 * ((r800 - r680) / (r800 + r680 + 1))");
		add("savi", "(r800 - r670) / (r800 + r670 + 0.5) * (1 + 0.5)");
		
		// MPRI
		// from: RGB vegetation indices applied to grass monitoring: a qualitative analysis
		// https://doi.org/10.15159/ar.19.119
		add("mpri", "(green - red) / (green + red)");
		add("mgvri",  "(green^2 - red^2) / (green^2 + red^2)");
		add("gli",  "(2 * green - red - blue) / ( 2 * green + red + blue)");
		add("rgvbi",  "(green - blue * red) / (green^2 + blue * red)");
		add("exg",  "2 * green - red - blue");
		add("veg",  "green / (red^0.667 * blue^0.333)");
	    
		// TGI
		// from: A visible band index for remote sensing leaf chlorophyll content at the canopy scale
		// https://doi.org/10.1016/j.jag.2012.07.020
		//
		// −0.5[(cr - cb)(Rr   - Rg)  - (cr  - cg)(Rr - Rb)]
		// −0.5[(670 - 480)(R670 - R550) - (670 - 550)(R670 - R480)]
		// −0.5[(190)(r670 - r550) - (120)(r670 - r480)]
		// −0.5 * ( 190 * (r670 - r550) - 120 * (r670 - r480))
		add("tgi", "-0.5 * ( 190 * (r670 - r550) - 120 * (r670 - r480))");
		

		
	    
	    add("full_spectrum", "[r0 : r10000]");
	}
	
	private MacroVisitor() {}

	@Override
	public AST visitFunction(AST_function ast, AST parent) {
		ArrayList<AST> list = new ArrayList<>();
		for(AST e:ast.asts) {
			AST node = e.accept(this, ast);
			list.add(node);
		}
		
		AST compositeAst = compositeAstMap.get(ast.name);
		if(compositeAst != null) {
			return compositeAst;
		} else {
			if(ast.asts.size() == 2) {
				switch(ast.name) {
				case "add":
				case "sub":
				case "mul":
				case "div":
				case "pow":
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
						case "pow": return new AST_Constant(Math.pow(x,y)).accept(this, parent);
						}
					}
				}
			}
			return new AST_function(ast.name, list);			
		}
	}

}
