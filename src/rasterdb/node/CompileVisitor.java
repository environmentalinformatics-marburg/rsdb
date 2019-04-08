package rasterdb.node;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.BandProcessor;
import rasterdb.ast.AST;
import rasterdb.ast.AST_Band_number;
import rasterdb.ast.AST_Constant;
import rasterdb.ast.AST_Sequence;
import rasterdb.ast.AST_function;
import rasterdb.ast.AST_radiance;
import rasterdb.ast.AstVisitor;
import util.frame.DoubleFrame;

public class CompileVisitor implements AstVisitor<ProcessorNode, Void> {
	static final Logger log = LogManager.getLogger();

	//private final BandProcessor processor;

	public CompileVisitor(BandProcessor processor) {
		//this.processor = processor;
	}

	@Override
	public ProcessorNode visitRadiation(AST_radiance ast, Void param) {
		throw new RuntimeException("wavelength not bound");
	}

	@Override
	public ProcessorNode visitSequence(AST_Sequence ast, Void param) {		
		ArrayList<ProcessorNode> list = new ArrayList<>();
		for(AST e:ast.asts) {
			ProcessorNode node = e.accept(this, null);
			list.add(node);
		}
		return new ProcessorNode() {
			ProcessorNode[] nodes = list.toArray(new ProcessorNode[0]);
			int len = nodes.length;
			@Override
			public DoubleFrame[] process(BandProcessor processor) {
				DoubleFrame[][] subResults = new DoubleFrame[len][];
				int fullLen = 0;
				for (int i = 0; i < len; i++) {
					DoubleFrame[] subResult = nodes[i].process(processor);
					fullLen += subResult.length;
					subResults[i] = subResult; 
				}
				DoubleFrame[] r = new DoubleFrame[fullLen];
				int pos = 0;
				for (int i = 0; i < len; i++) {
					DoubleFrame[] subResult = subResults[i];
					for(DoubleFrame frame:subResult) {
						r[pos++] = frame;
					}
				}
				return r;
			}

		};
	}

	@Override
	public ProcessorNode visitBandNumber(AST_Band_number ast, Void param) {
		return new ProcessorNode() {
			@Override
			public DoubleFrame[] process(BandProcessor processor) {
				Band band = processor.getBand(ast.number);
				DoubleFrame frame = processor.getDoubleFrame(band);
				if(band.has_wavelength()) {
					frame.meta.put("index", band.index);
					if(band.has_wavelength()) {
						frame.meta.put("wavelength", band.wavelength);
					}
					if(band.has_fwhm()) {
						frame.meta.put("fwhm", band.fwhm);
					}
					frame.meta.put("name", band.has_title() ? band.title : "band"+band.index);
				}	
				return new DoubleFrame[]{frame};				
			}

		};
	};

	@Override
	public ProcessorNode visitFunction(AST_function ast, Void param) {
		/*if(ast.asts.size() != 2) {
			throw new RuntimeException("number of parameters not implemented: "+ast.asts.size());
		}*/
		ArrayList<ProcessorNode> paramlist = new ArrayList<>();
		for(AST e:ast.asts) {
			ProcessorNode node = e.accept(this, null);
			paramlist.add(node);
		}
		ProcessorNode[] paramNodes = paramlist.toArray(new ProcessorNode[0]);
		switch(ast.name) {
		case "add":
		case "sub":
		case "mul":
		case "div":
			return new ProcessorNode() {
				String name = ast.name;
				ProcessorNode[] nodes = paramNodes;
				int len = ast.asts.size();
				@Override
				public DoubleFrame[] process(BandProcessor processor) {
					DoubleFrame[][] subResults = new DoubleFrame[len][];
					for (int i = 0; i < subResults.length; i++) {
						subResults[i] = nodes[i].process(processor);
					}
					switch(name) {
					case "add":
						return new DoubleFrame[]{subResults[0][0].addThis(subResults[1][0])};
					case "sub":
						return new DoubleFrame[]{subResults[0][0].substractThis(subResults[1][0])};
					case "mul":
						return new DoubleFrame[]{subResults[0][0].mulThis(subResults[1][0])};
					case "div":
						return new DoubleFrame[]{subResults[0][0].divThis(subResults[1][0])};
					default:
						throw new RuntimeException("error: unknown function '"+name+"'");
					}
				}
			};
		case "normalised_difference":
			if(paramNodes.length != 2) {
				throw new RuntimeException("normalised_difference needs two parameters "+paramNodes.length);
			}
			return new ProcessorNode_normalised_difference(paramNodes[0], paramNodes[1]);
		case "normalised_ratio":
			if(paramNodes.length != 2) {
				throw new RuntimeException("normalised_ratio needs two parameters "+paramNodes.length);
			}
			return new ProcessorNode_normalised_ratio(paramNodes[0], paramNodes[1]);
		case "pca":
			switch(paramNodes.length) {
			case 1:
				return new ProcessorNode_pca(paramNodes[0], Integer.MAX_VALUE);
			case 2:
				AST c = ast.asts.get(1);
				if(c instanceof AST_Constant) {
					int ci = (int) ((AST_Constant)c).constant;
					return new ProcessorNode_pca(paramNodes[0], ci);
				} else {
					throw new RuntimeException("pca needs integer as second parameter: "+ast.getClass());
				}
			default:
				throw new RuntimeException("pca needs one or two parameters "+paramNodes.length);
			}
		case "euclidean_distance":
			if(paramNodes.length != 1) {
				throw new RuntimeException("euclidean_distance needs one parameter "+paramNodes.length);
			}
			return new ProcessorNode_euclidean_distance(paramNodes[0]);
		case "black_point_compensation":
			if(paramNodes.length != 1) {
				throw new RuntimeException("black_point_compensation needs one parameter "+paramNodes.length);
			}
			return new ProcessorNode_black_point_compensation(paramNodes[0]);
		case "gap_filling":
			switch(paramNodes.length) {
			case 1:
				return new ProcessorNode_gap_filling(paramNodes[0], 3);
			case 2:
				AST c = ast.asts.get(1);
				if(c instanceof AST_Constant) {
					int ci = (int) ((AST_Constant)c).constant;
					return new ProcessorNode_gap_filling(paramNodes[0], ci);
				} else {
					throw new RuntimeException("gap_filling needs integer as second parameter: "+ast.getClass());
				}
			default:
				throw new RuntimeException("gap_filling one or two parameters "+paramNodes.length);
			}
		default:
			throw new RuntimeException("error: unknown function '"+ast.name+"'");
		}

	}

	@Override
	public ProcessorNode visitConstant(AST_Constant ast_Constant, Void param) {
		return new ProcessorNode() {
			@Override
			public DoubleFrame[] process(BandProcessor processor) {
				return new DoubleFrame[]{processor.getDoubleFrameConst(ast_Constant.constant)};				
			}
		};
	}
}
