package rasterdb;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeBand {
	public final int timestamp;
	public final Band band;
	
	public TimeBand(int timestamp, Band band) {
		this.timestamp = timestamp;
		this.band = band;
	}

	@Override
	public String toString() {
		return "TimeBand [timestamp=" + timestamp + ", band=" + band + "]";
	}
	
	public static List<TimeBand> of(int timestamp, Stream<Band> bands) {
		return bands.map(band -> new TimeBand(timestamp, band)).collect(Collectors.toList());
	}
	
	public static List<TimeBand> of(int timestamp, Collection<Band> bands) {
		return of(timestamp, bands.stream());
	}

	public static TimeBand[] of(int timestamp, Band[] bands) {
		int len = bands.length;
		TimeBand[] timebands = new TimeBand[len];
		for (int i = 0; i < len; i++) {
			timebands[i] = new TimeBand(timestamp, bands[i]);
		}
		return timebands;
	}
}