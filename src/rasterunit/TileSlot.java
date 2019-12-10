package rasterunit;

import java.util.Comparator;
import java.util.Objects;

public class TileSlot {
	public static final TileSlot CONCURRENT_UPDATE = new TileSlot(Long.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

	public final long pos;
	public final int len;
	public final int type;
	public final int rev;

	public TileSlot(long pos, int len, int type, int rev) {
		this.pos = pos;
		this.len = len;
		this.type = type;
		this.rev = rev;
	}

	public boolean isConcurrentUpdate() {
		return pos == Long.MIN_VALUE;
	}

	public static final Comparator<TileSlot> POS_LEN_REV_COMPARATOR = new Comparator<TileSlot>() {
		@Override
		public int compare(TileSlot o1, TileSlot o2) {
			int cmpPos = Long.compare(o1.pos, o2.pos);
			if(cmpPos != 0) {
				return cmpPos;
			}
			int cmpLen = Integer.compare(o1.len, o2.len);
			return cmpLen == 0 ? Integer.compare(o1.rev, o2.rev) : cmpLen;
		}		
	};

	@Override
	public int hashCode() {
		return Objects.hash(len, pos, rev, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileSlot other = (TileSlot) obj;
		return len == other.len && pos == other.pos && rev == other.rev && type == other.type;
	}
	
	public boolean equalsNoRev(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileSlot other = (TileSlot) obj;
		return len == other.len && pos == other.pos && type == other.type;
	}

	@Override
	public String toString() {
		return "TileSlot [pos=" + pos + ", len=" + len + ", type=" + type + ", rev=" + rev + "]";
	}
}
