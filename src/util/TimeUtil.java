package util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeUtil {
	private static final Logger log = LogManager.getLogger();
	private final static DateTimeFormatter FILENAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
	private final static DateTimeFormatter PRETTY_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final LocalDateTime OLE_AUTOMATION_TIME_START = LocalDateTime.of(1899,12,30,0,0);
	public final static DateTimeFormatter DATE_UNDERSCORE = DateTimeFormatter.ofPattern("yyyy_MM_dd");
	public final static DateTimeFormatter DATETIME_UNDERSCORE = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm");
	
	/**
	 * try to get timestamp from within filename
	 * @param filename
	 * @return
	 */
	public static int getTimestampOfFilename(String filename) {
		try {
			log.info("get timestamp "+filename);
			int beginIndex = filename.lastIndexOf('/');
			if(beginIndex<0) {
				beginIndex = filename.lastIndexOf('\\');
			}
			String text = filename.substring(beginIndex+1);

			if(text.startsWith("s1a-ew-grd-hh-")||text.startsWith("s1a-ew-grd-hv-")) {
				String isoText = text.substring(14,27);
				String isoTextDate = isoText.substring(0, 8);
				String isoTextTime = isoText.substring(9);
				log.info("isoText "+isoText);
				log.info("text "+isoText +"   "+isoTextDate+"  "+isoTextTime);
				LocalDate date = LocalDate.parse(isoTextDate, DateTimeFormatter.BASIC_ISO_DATE);
				LocalTime time = LocalTime.parse(isoTextTime, FILENAME_TIME_FORMATTER);
				LocalDateTime datetime = LocalDateTime.of(date, time);
				int timestamp = toTimestamp(datetime);
				log.info(datetime+" -> "+timestamp);
				return timestamp;
			} else {


				String isoText = text.substring(0, 15);
				String isoTextDate = isoText.substring(0, 10);
				String isoTextTime = isoText.substring(11);
				log.info("text "+isoText +"   "+isoTextDate+"  "+isoTextTime);
				LocalDate date = LocalDate.parse(isoTextDate, DateTimeFormatter.ISO_LOCAL_DATE);
				LocalTime time = LocalTime.parse(isoTextTime, FILENAME_TIME_FORMATTER);
				LocalDateTime datetime = LocalDateTime.of(date, time);
				int timestamp = toTimestamp(datetime);
				log.info(datetime+" -> "+timestamp);
				return timestamp;
			}
		} catch(Exception e) {
			log.error(e);
		}
		return 0;
	}


	private static final Pattern TIMESTAMP1_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{6}_"); // e.g. 2014-03-12T112236_*   (rapideye: 1B – BASIC PRODUCT NAMING)
	private final static DateTimeFormatter TIMESTAMP1_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm");

	public static int tryParseTimestamp(String s) {
		try {
			if(TIMESTAMP1_PATTERN.matcher(s).find()) {
				LocalDateTime datetime = LocalDateTime.parse(s.substring(0, 15), TIMESTAMP1_DATETIME_FORMATTER);
				return toTimestamp(datetime);
			}
			return -1;
		} catch(Exception e) {
			log.warn("timestamp parse error "+e+"   of "+s);
			return -2;
		}
	}


	public static int toTimestamp(LocalDateTime datetime) {
		return (int) Duration.between(OLE_AUTOMATION_TIME_START, datetime).toMinutes();
	}


	public static LocalDateTime toDateTime(int timestamp) {
		return OLE_AUTOMATION_TIME_START.plus(Duration.ofMinutes(timestamp));
	}

	public static String toText(int timestamp) {
		if(timestamp==0) {
			return "---";
		}
		return toDateTime(timestamp).toString();
	}

	public static String toPrettyText(int timestamp) {
		if(timestamp==0) {
			return "---";
		}
		if(timestamp==Integer.MIN_VALUE) {
			return "---";
		}
		if(timestamp==Integer.MAX_VALUE) {
			return "---";
		}
		return toDateTime(timestamp).format(PRETTY_DATETIME_FORMATTER);
	}
	
	public static String toFileText(int timestamp) {
		if(timestamp==0) {
			return "_";
		}
		if(timestamp==Integer.MIN_VALUE) {
			return "_";
		}
		if(timestamp==Integer.MAX_VALUE) {
			return "_";
		}
		return toDateTime(timestamp).format(DATETIME_UNDERSCORE);
	}

	public static String toText(Collection<Integer> timestamps) {
		return timestamps.stream().map(TimeUtil::toText).collect(Collectors.toList()).toString();
	}

	public static String toPrettyText(Collection<Integer> timestamps) {
		return timestamps.stream().map(TimeUtil::toPrettyText).collect(Collectors.toList()).toString();
	}

	public static String toText(int[] timestamps) {
		return Arrays.stream(timestamps).mapToObj(TimeUtil::toText).collect(Collectors.toList()).toString();
	}

	public static String toPrettyText(int[] timestamps) {		
		return Arrays.stream(timestamps).mapToObj(TimeUtil::toPrettyText).collect(Collectors.toList()).toString();
	}

	public static int[] getTimestampRange(String s) {
		LocalDateTime[] range = getDateTimeRange(s);
		int min = range[0]==null?Integer.MIN_VALUE:toTimestamp(range[0]);
		int max = range[1]==null?Integer.MAX_VALUE:toTimestamp(range[1]);
		return new int[]{min, max};
	}

	public static int[] getTimestampRangeOrNull(String s) {
		LocalDateTime[] range = getDateTimeRange(s);
		if(range[0] == null || range[1] == null) {
			return null;
		} else {
			int min = range[0] == null ? Integer.MIN_VALUE : toTimestamp(range[0]);
			int max = range[1] == null ? Integer.MAX_VALUE : toTimestamp(range[1]);
			return new int[]{min, max};
		}
	}

	public static LocalDateTime[] getDateTimeRange(String s) {
		if(s==null) {
			return new LocalDateTime[]{null, null};
		}
		s = s.trim();
		if(s.isEmpty()) {
			return new LocalDateTime[]{null, null};
		}
		switch(s.length()) {
		case 4: {//year e.g. 2015
			int year = Integer.parseInt(s);			
			LocalDateTime start = LocalDateTime.of(year, 01, 01, 00, 00);;
			LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59);
			return new LocalDateTime[]{start,end};
		}
		case 7: {//year month e.g. 2015-03
			LocalDate date = LocalDate.parse(s+"-01", DateTimeFormatter.ISO_DATE);
			int year = date.getYear();
			int month = date.getMonthValue();
			LocalDateTime start = LocalDateTime.of(year, month, 01, 00, 00);;
			LocalDateTime end = LocalDateTime.of(year, month, date.lengthOfMonth(), 23, 59);
			return new LocalDateTime[]{start,end};
		}
		case 10: {//year month day e.g. 2015-03-02
			LocalDate date = LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
			int year = date.getYear();
			int month = date.getMonthValue();
			int day = date.getDayOfMonth();
			LocalDateTime start = LocalDateTime.of(year, month, day, 00, 00);;
			LocalDateTime end = LocalDateTime.of(year, month, day, 23, 59);
			return new LocalDateTime[]{start,end};
		}
		case 13: {//year month day hour e.g. 2015-03-02T11
			LocalDateTime datetime = LocalDateTime.parse(s+":00", DateTimeFormatter.ISO_DATE_TIME);
			int year = datetime.getYear();
			int month = datetime.getMonthValue();
			int day = datetime.getDayOfMonth();
			int hour = datetime.getHour();
			LocalDateTime start = LocalDateTime.of(year, month, day, hour, 00);;
			LocalDateTime end = LocalDateTime.of(year, month, day, hour, 59);
			return new LocalDateTime[]{start,end};
		}
		case 19: //year month day hour minute second e.g. 2015-03-02T11:17:03 with seconds ignored
		case 16: {//year month day hour minute e.g. 2015-03-02T11:17
			LocalDateTime datetime = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
			int year = datetime.getYear();
			int month = datetime.getMonthValue();
			int day = datetime.getDayOfMonth();
			int hour = datetime.getHour();
			int minute = datetime.getMinute();
			LocalDateTime start = LocalDateTime.of(year, month, day, hour, minute);
			LocalDateTime end = LocalDateTime.of(year, month, day, hour, minute);
			return new LocalDateTime[]{start,end};
		}
		default:
			throw new RuntimeException("unknown timestamp format");
		}
	}

	public static int parseIsoTimestamp(String text) {
		LocalDate date = LocalDate.parse(text);
		return toTimestamp(LocalDateTime.of(date, LocalTime.MIDNIGHT));
	}

}
