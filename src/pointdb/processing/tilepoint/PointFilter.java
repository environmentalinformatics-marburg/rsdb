package pointdb.processing.tilepoint;

import java.util.Arrays;

import pointdb.base.Point;

public interface PointFilter {
	public static final PointFilter FILTER_LAST_RETURN = Point::isLastReturn;
	public static final PointFilter FILTER_GROUND_CLASSIFICATIONS = Point::isGround;

	boolean test(Point p);

	public static PointFilter createFilter(String text) {
		String[] filtetTexts = text.split(";");
		PointFilter[] filters = new PointFilter[filtetTexts.length];
		for (int i = 0; i < filtetTexts.length; i++) {
			String f = filtetTexts[i].trim();
			filters[i] = createAtomicFilter(f);
		}
		return FilterSet.of(filters);
	}

	public static PointFilter createAtomicFilter(String text) {
		String rEQ = "return=";
		String lEQ = "last_return=";
		String cEQ = "classification=";
		if(text.startsWith(rEQ)) {
			String r = text.substring(rEQ.length());
			int returnNumber = Integer.parseInt(r);
			return p->p.returnNumber==returnNumber;
		} else if (text.startsWith(lEQ)) {
			String r = text.substring(lEQ.length());
			int last_returnNumber = Integer.parseInt(r);
			return p->p.returns-(p.returnNumber-1)==last_returnNumber;
		}else if (text.startsWith(cEQ)) {
			String classNrsText = text.substring(cEQ.length());
			String[] classNrsTexts = classNrsText.split("_");
			if(classNrsTexts.length == 1) {
				int classNr = Integer.parseInt(classNrsText);
				return p -> p.classification == classNr;
			} else {
				int[] classNrs = Arrays.stream(classNrsTexts).mapToInt(Integer::parseInt).toArray();
				return p->{
					for(int classNr:classNrs) {
						if(p.classification == classNr) {
							return true;
						}
					}
					return false;
				};
			}
		}  
		throw new RuntimeException("filter unknown: "+text);
	}

	public static class FilterAnd implements PointFilter {
		private final PointFilter a;
		private final PointFilter b;
		public FilterAnd(PointFilter a, PointFilter b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public boolean test(Point p) {
			return a.test(p) && b.test(p);
		}		
	}

	public static class FilterSet implements PointFilter {
		private final PointFilter[] filters;
		private FilterSet(PointFilter[] filters) {
			this.filters = filters;
		}
		@Override
		public boolean test(Point p) {
			for(PointFilter filter:filters) {
				if(!filter.test(p)) {
					return false;
				}
			}
			return true;
		}
		public static PointFilter of(PointFilter... filters) {
			if(filters==null||filters.length==0) {
				throw new RuntimeException("no filter");
			}
			if(filters.length==1) {
				return filters[0];
			}
			if(filters.length==2) {
				return new FilterAnd(filters[0],filters[1]);
			}
			return new FilterSet(filters);
		}
	}
}