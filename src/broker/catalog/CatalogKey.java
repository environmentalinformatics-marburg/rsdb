package broker.catalog;

public class CatalogKey implements Comparable<CatalogKey> {
	
	public static final String TYPE_RASTERDB = "RasterDB";
	public static final String TYPE_POINTDB = "PointDB";
	public static final String TYPE_POINTCLOUD = "pointcloud";
	public static final String TYPE_VECTORDB = "vectordb";

	public final String name;
	public final String type;	

	public CatalogKey(String name, String type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CatalogKey other = (CatalogKey) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public int compareTo(CatalogKey o) {
		int c = this.name.compareTo(o.name);
		return c == 0 ? this.type.compareTo(o.type) : c;
	}
	
	@Override
	public String toString() {
		return name+":"+type;
	}
}
