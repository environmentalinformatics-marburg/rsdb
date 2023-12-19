package pointdb.subsetdsl;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import org.tinylog.Logger;
import org.mapdb.Fun.Pair;

import broker.Broker;
import broker.group.Poi;
import broker.group.Roi;
import pointdb.base.Rect;
import pointdb.subsetdsl.SubsetDSLParser.BboxContext;
import pointdb.subsetdsl.SubsetDSLParser.PContext;
import pointdb.subsetdsl.SubsetDSLParser.PoiContext;
import pointdb.subsetdsl.SubsetDSLParser.PointContext;
import pointdb.subsetdsl.SubsetDSLParser.Point_sequence2Context;
import pointdb.subsetdsl.SubsetDSLParser.Point_sequenceContext;
import pointdb.subsetdsl.SubsetDSLParser.RegionContext;
import pointdb.subsetdsl.SubsetDSLParser.Region_scirptContext;
import pointdb.subsetdsl.SubsetDSLParser.Region_sequenceContext;
import pointdb.subsetdsl.SubsetDSLParser.RoiContext;
import pointdb.subsetdsl.SubsetDSLParser.SquareContext;
import pointdb.subsetdsl.SubsetDSLParser.UrlContext;
import util.collections.vec.Vec;

public class Compiler {
	

	public Compiler() {
	}

	public SubsetDSL_AST_region parse(String script) {		
		ANTLRInputStream in = new ANTLRInputStream(script);
		SubsetDSLLexer lexer = new SubsetDSLLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SubsetDSLParser parser = new SubsetDSLParser(tokens);
		return parser.region_scirpt().accept(RegionVisitor.DEFAULT);		
	}

	public interface SubsetDSL_AST_region {
		Vec<Pair<Region, String>> getRegions(Broker broker);
	}

	private static class SubsetDSL_AST_region_sequence implements SubsetDSL_AST_region {
		public final Vec<SubsetDSL_AST_region> list;
		public SubsetDSL_AST_region_sequence(Vec<SubsetDSL_AST_region> list) {
			this.list = list;
		}
		@Override
		public Vec<Pair<Region, String>> getRegions(Broker broker) {
			Vec<Pair<Region, String>> r = new Vec<Pair<Region, String>>();
			for(SubsetDSL_AST_region p:list) {
				r.addAll(p.getRegions(broker));
			}
			return r;
		}
	}

	private static class SubsetDSL_AST_region_square implements SubsetDSL_AST_region {
		public final SubsetDSL_AST_point p;
		public final long edge;
		public SubsetDSL_AST_region_square(SubsetDSL_AST_point p, long edge) {
			this.p = p;
			this.edge = edge;
			if(edge<1) {
				throw new RuntimeException("edge too small "+edge);
			}
		}
		@Override
		public Vec<Pair<Region, String>> getRegions(Broker broker) {
			Vec<Pair<Region, String>> list = new Vec<Pair<Region, String>>();
			for(Pair<Point, String> a:p.getPoints(broker)) {
				long half = edge/2;
				long x = a.a.x - half;
				long y = a.a.y - half;
				Region region = Region.ofRect(new Rect(x, y, x - 1 + edge, y - 1 + edge));
				list.add(new Pair<Region, String>(region, a.b));
			}
			return list;
		}
	}

	private static class SubsetDSL_AST_region_bbox_of_points implements SubsetDSL_AST_region {
		public final SubsetDSL_AST_point p;
		public SubsetDSL_AST_region_bbox_of_points(SubsetDSL_AST_point p) {
			this.p = p;
		}
		@Override
		public Vec<Pair<Region, String>> getRegions(Broker broker) {
			Vec<Pair<Point, String>> points = p.getPoints(broker);
			Logger.info("points "+points);
			//Rect rect = points.asLazy().collect(p->p.a).reduceInPlace(Point.BBOX_COLLECTOR);
			Rect rect = points.stream().map(p->p.a).collect(Point.BBOX_COLLECTOR);
			return Vec.ofOne(new Pair<Region, String>(Region.ofRect(rect),"bbox"));
		}
	}

	private static class SubsetDSL_AST_region_bbox_of_regions implements SubsetDSL_AST_region {
		public final SubsetDSL_AST_region r;
		public SubsetDSL_AST_region_bbox_of_regions(SubsetDSL_AST_region r) {
			this.r = r;
		}
		@Override
		public Vec<Pair<Region, String>> getRegions(Broker broker) {
			return r.getRegions(broker).map((Pair<Region, String> p)->new Pair<Region, String>(p.a.toBboxRegion(),p.b));
		}
	}


	private static class SubsetDSL_AST_polygon_roi implements SubsetDSL_AST_region  {
		public final String group;
		public final Vec<ExternalURL> list;
		public SubsetDSL_AST_polygon_roi(String group, Vec<ExternalURL> list) {
			this.group = group;
			this.list = list;
		}
		@Override
		public Vec<Pair<Region, String>> getRegions(Broker broker) {
			Vec<Pair<Region, String>> regions = new Vec<Pair<Region, String>>();
			for(ExternalURL url:list) {
				String g = url.group==null ? group : url.group;
				if(g==null) {
					Logger.warn("no group specified");
				} else {
					Roi roi = broker.getRoiByPath(g, url.name);
					Region region = Region.ofPolygonsWithHoles(roi.polygons);
					Logger.info(region);
					regions.add(new Pair<Region, String>(region, url.name));
				}
			}
			return regions;
		}
	}


	public static class Point {
		public final long x;
		public final long y;
		public Point(long x, long y) {
			this.x = x;
			this.y = y;
		}
		public static class BboxConsumer implements Consumer<Point> {
			public long xmin = Long.MAX_VALUE;
			public long ymin = Long.MAX_VALUE;
			public long xmax = Long.MIN_VALUE;
			public long ymax = Long.MIN_VALUE;
			@Override
			public void accept(Point p) {
				if(p.x<xmin) xmin = p.x;
				if(p.y<ymin) ymin = p.y;
				if(p.x>xmax) xmax = p.x;
				if(p.y>ymax) ymax = p.y;				
			}
			public Rect getRect() {
				return new Rect(xmin, ymin, xmax, ymax);
			}
			public BboxConsumer combine(BboxConsumer b) {
				xmin = xmin<b.xmin ? xmin : b.xmin;
				ymin = ymin<b.ymin ? ymin : b.ymin;
				xmax = b.xmax<xmax ? xmax : b.xmax;
				ymax = b.ymax<ymax ? ymax : b.ymax;
				return this;
			}
		}
		public static final Collector<Point, BboxConsumer, Rect> BBOX_COLLECTOR = new Collector<Point, BboxConsumer, Rect>(){
			@Override
			public Supplier<BboxConsumer> supplier() {
				return BboxConsumer::new;
			}
			@Override
			public BiConsumer<BboxConsumer, Point> accumulator() {
				return BboxConsumer::accept;
			}
			@Override
			public BinaryOperator<BboxConsumer> combiner() {
				return BboxConsumer::combine;
			}
			@Override
			public Function<BboxConsumer, Rect> finisher() {
				return BboxConsumer::getRect;
			}
			@Override
			public Set<Characteristics> characteristics() {
				return EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED);
			}			
		};
		@Override
		public String toString() {
			return "Point [x=" + x + ", y=" + y + "]";
		}
	}

	private interface SubsetDSL_AST_point {
		Vec<Pair<Point, String>> getPoints(Broker broker);
	}

	private static class SubsetDSL_AST_point_sequence implements SubsetDSL_AST_point {
		public final Vec<SubsetDSL_AST_point> list;
		public SubsetDSL_AST_point_sequence(Vec<SubsetDSL_AST_point> list) {
			this.list = list;
		}
		@Override
		public Vec<Pair<Point, String>> getPoints(Broker broker) {
			Vec<Pair<Point, String>> r = new Vec<Pair<Point, String>>();
			for(SubsetDSL_AST_point p:list) {
				r.addAll(p.getPoints(broker));
			}
			return r;
		}
	}

	private static class SubsetDSL_AST_point_poi implements SubsetDSL_AST_point  {
		public final String group;
		public final Vec<ExternalURL> list;
		public SubsetDSL_AST_point_poi(String group, Vec<ExternalURL> list) {
			this.group = group;
			this.list = list;
		}
		@Override
		public Vec<Pair<Point, String>> getPoints(Broker broker) {
			Vec<Pair<Point, String>> r = new Vec<Pair<Point, String>>();
			for(ExternalURL p:list) {
				String g = p.group==null ? group : p.group;
				if(g==null) {
					Logger.warn("no group specified");
				} else {
					Poi poi = broker.getPoiByPath(g, p.name);
					if(poi==null) {
						Logger.warn("POI not found "+p);
					} else {
						r.add(new Pair<Point, String>(new Point((long)(poi.x*1000), (long)(poi.y*1000)),p.name)); //short title
					}
				}
			}
			return r;

		}
	}

	private static class SubsetDSL_AST_point_p implements SubsetDSL_AST_point  {
		public final double x;
		public final double y;

		public SubsetDSL_AST_point_p(double x, double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public Vec<Pair<Point, String>> getPoints(Broker broker) {
			return Vec.ofOne(new Pair<Point, String>(new Point((long)(x*1000), (long)(y*1000)),"p"));
		}		
	}

	private static class ExternalURL {
		public final String group;
		public final String name;
		public ExternalURL(String group, String name) {
			this.group = group;
			this.name = name;
		}
		@Override
		public String toString() {
			return group + "/" + name;
		}
	}



	private static class RegionVisitor extends SubsetDSLBaseVisitor<SubsetDSL_AST_region> {
		public static final RegionVisitor DEFAULT = new RegionVisitor();

		@Override
		public SubsetDSL_AST_region visitRegion_scirpt(Region_scirptContext ctx) {
			return ctx.region_sequence().accept(this);
		}		

		@Override
		public SubsetDSL_AST_region visitRegion_sequence(Region_sequenceContext ctx) {
			Vec<SubsetDSL_AST_region> list = new Vec<>();
			for(RegionContext r:ctx.region()) {
				list.add(r.accept(this));
			}
			return new SubsetDSL_AST_region_sequence(list);
		}

		@Override
		public SubsetDSL_AST_region visitRegion(RegionContext ctx) {
			if(ctx.square()!=null) {
				return ctx.square().accept(this);
			}
			if(ctx.roi()!=null) {
				return ctx.roi().accept(this);
			}
			
			if(ctx.bbox()!=null) {
				return ctx.bbox().accept(this);
			}
			throw new RuntimeException("unknown context");
		}

		@Override
		public SubsetDSL_AST_region visitSquare(SquareContext ctx) {
			return new SubsetDSL_AST_region_square(ctx.point_sequence().accept(PointVisitor.DEFAULT), (long) (Double.parseDouble(ctx.edge.getText())*1000d));
		}

		@Override
		public SubsetDSL_AST_region visitRoi(RoiContext ctx) {
			String group = ctx.group==null?null:ctx.group.getText();
			Vec<ExternalURL> list = new Vec<ExternalURL>();
			for(UrlContext url:ctx.url_sequence().url()) {
				String g = url.group==null? null : url.group.getText();
				ExternalURL e = new ExternalURL(g, url.name.getText());
				list.add(e);
			}
			return new SubsetDSL_AST_polygon_roi(group, list);
		}

		@Override
		public SubsetDSL_AST_region visitBbox(BboxContext ctx) {
			if(ctx.point_sequence2() != null) {
				SubsetDSL_AST_point p = ctx.point_sequence2().accept(PointVisitor.DEFAULT);
				return new SubsetDSL_AST_region_bbox_of_points(p);
			}
			if(ctx.region_sequence() != null) {
				SubsetDSL_AST_region r = ctx.region_sequence().accept(this);
				return new SubsetDSL_AST_region_bbox_of_regions(r);
			}
			throw new RuntimeException("unknown bbox context");
		}
	}

	private static class PointVisitor extends SubsetDSLBaseVisitor<SubsetDSL_AST_point> {
		public static final PointVisitor DEFAULT = new PointVisitor();

		@Override
		public SubsetDSL_AST_point visitPoint_sequence(Point_sequenceContext ctx) {
			Vec<SubsetDSL_AST_point> list = new Vec<>();
			for(PointContext p:ctx.point()) {
				list.add(p.accept(this));
			}
			return new SubsetDSL_AST_point_sequence(list);
		}
		
		@Override
		public SubsetDSL_AST_point visitPoint_sequence2(Point_sequence2Context ctx) {
			Vec<SubsetDSL_AST_point> list = new Vec<>();
			for(PointContext p:ctx.point()) {
				list.add(p.accept(this));
			}
			return new SubsetDSL_AST_point_sequence(list);
		}

		@Override
		public SubsetDSL_AST_point visitPoint(PointContext ctx) {
			if(ctx.poi()!=null) {
				return ctx.poi().accept(this);
			}
			if(ctx.p()!=null) {
				return ctx.p().accept(this);
			}
			throw new RuntimeException();
		}

		@Override
		public SubsetDSL_AST_point visitPoi(PoiContext ctx) {
			String overGroup = ctx.group==null?null:ctx.group.getText();
			Vec<ExternalURL> list = new Vec<ExternalURL>();			
			for(UrlContext url:ctx.url_sequence().url()) {
				String group = url.group==null?null:url.group.getText();
				String name = url.name.getText();
				ExternalURL e = new ExternalURL(group, name);
				list.add(e);
			}
			return new SubsetDSL_AST_point_poi(overGroup, list);
		}

		@Override
		public SubsetDSL_AST_point visitP(PContext ctx) {			
			double x = Double.parseDouble(ctx.x.getText());
			double y = Double.parseDouble(ctx.y.getText());
			return new SubsetDSL_AST_point_p(x, y);
		}
	}

}
