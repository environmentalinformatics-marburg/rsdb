package pointdb.processing.geopoint;

import java.util.function.Predicate;

import pointdb.base.GeoPoint;

public interface GeoPointFilter extends Predicate<GeoPoint> {
	public static final GeoPointFilter FILTER_LAST_RETURN = GeoPoint::isLastReturn;
	public static final GeoPointFilter FILTER_GROUND_CLASSIFICATIONS = GeoPoint::isGround;
	
	@Override
	boolean test(GeoPoint p);
	
	public static GeoPointFilter createFilter(String text) {
		String[] filtetTexts = text.split(";");
		GeoPointFilter[] filters = new GeoPointFilter[filtetTexts.length];
		for (int i = 0; i < filtetTexts.length; i++) {
			String f = filtetTexts[i].trim();
			filters[i] = createAtomicFilter(f);
		}
		return FilterSet.of(filters);
	}

	public static GeoPointFilter createAtomicFilter(String text) {
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
			String r = text.substring(cEQ.length());
			int classNr = Integer.parseInt(r);
			return p->p.classification==classNr;
		}  
		throw new RuntimeException("filter unknown: "+text);
	}
	
	public static class FilterAnd implements GeoPointFilter {
		private final GeoPointFilter a;
		private final GeoPointFilter b;
		public FilterAnd(GeoPointFilter a, GeoPointFilter b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public boolean test(GeoPoint p) {
			return a.test(p) && b.test(p);
		}		
	}

	public static class FilterSet implements GeoPointFilter {
		private final GeoPointFilter[] filters;
		private FilterSet(GeoPointFilter[] filters) {
			this.filters = filters;
		}
		@Override
		public boolean test(GeoPoint p) {
			for(GeoPointFilter filter:filters) {
				if(!filter.test(p)) {
					return false;
				}
			}
			return true;
		}
		public static GeoPointFilter of(GeoPointFilter... filters) {
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