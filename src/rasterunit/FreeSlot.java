package rasterunit;

import java.util.Comparator;
import java.util.Objects;

public class FreeSlot {
	public final long pos;
	public final int len;
	
	public FreeSlot(long pos, int len) {
		this.pos = pos;
		this.len = len;
	}
	
	public static final Comparator<FreeSlot> LEN_POS_COMPARATOR = new Comparator<FreeSlot>() {
		@Override
		public int compare(FreeSlot o1, FreeSlot o2) {
			int cmp = Integer.compare(o1.len, o2.len);
			return cmp == 0 ? Long.compare(o1.pos, o2.pos) : cmp;
		}		
	};
	
	public static final Comparator<FreeSlot> POS_COMPARATOR = new Comparator<FreeSlot>() {
		@Override
		public int compare(FreeSlot o1, FreeSlot o2) {
			return Long.compare(o1.pos, o2.pos);
		}		
	};
	
	@Override
	public int hashCode() {
		return Objects.hash(len, pos);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FreeSlot other = (FreeSlot) obj;
		return pos == other.pos && len == other.len;
	}
	
	@Override
	public String toString() {
		return "FreeSlot [pos=" + pos + ", len=" + len + "]";
	}
	
	public long nextPos() {
		return pos + len;
	}
}
