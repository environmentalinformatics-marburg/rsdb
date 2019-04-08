package rasterdb;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.ast.AST;
import rasterdb.ast.AST_Band_number;
import rasterdb.ast.AST_Sequence;
import rasterdb.ast.AST_function;
import rasterdb.ast.AST_radiance;
import rasterdb.ast.TransformVisitor;

public class BindVisitor implements TransformVisitor {
	static final Logger log = LogManager.getLogger();

	private final RasterDB rasterdb;

	public BindVisitor(RasterDB rasterdb) {
		this.rasterdb = rasterdb;
	}

	@Override
	public AST visitRadiation(AST_radiance ast, AST parent) {		
		Band band = BandProcessing.getBestSpectralBandWithinFwhm(rasterdb, ast.nm);
		if(band==null) {
			log.warn("band with wavelength "+ast.nm+" not found get nearest band");
			band = BandProcessing.getClosestSpectralBand(rasterdb, ast.nm);
		}
		if(band==null) {
			throw new RuntimeException("band with wavelength "+ast.nm+" not found");
		}
		return new AST_Band_number(band.index).accept(this, parent);		
	}

	@Override
	public AST visitFunction(AST_function ast, AST parent) {
		ArrayList<AST> list = new ArrayList<>();
		for(AST e:ast.asts) {
			AST node = e.accept(this, ast);
			list.add(node);
		}
		switch(ast.name) {
		case "range":
			AST_Band_number min = (AST_Band_number) list.get(0);
			AST_Band_number max = (AST_Band_number) list.get(1);
			ArrayList<AST> seq = new ArrayList<>();
			for(int i=min.number; i<=max.number; i++) {
				seq.add(new AST_Band_number(i));
			}
			return new AST_Sequence(seq).accept(this, parent);
		default:
			return new AST_function(ast.name, list);
		}
	}
}