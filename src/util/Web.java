package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import javax.servlet.ServletInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.Authentication.User;
import org.json.JSONObject;

public final class Web {
	private static final Logger log = LogManager.getLogger();

	private Web(){}

	public static final String MIME_JSON = "application/json";
	public static final String MIME_CSV = "text/csv";

	public static double getDouble(Request request, String name) {
		String text = request.getParameter(name);
		if(text==null) {
			throw new RuntimeException("parameter not found: "+name);
		}
		try {
			double d = Double.parseDouble(text);
			return d;
		} catch (Exception e) {
			throw new RuntimeException("wrong number format in parameter "+name+": '"+text+"'");
		}
	}

	public  static double getDouble(Request request, String name, double defaultValue) {
		String text = request.getParameter(name);
		if(text==null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(text);
		} catch (Exception e) {
			log.warn(e);
			return defaultValue;
		}
	}

	/**
	 * get one or more doubles separated by ","
	 * @param request
	 * @param name
	 * @return
	 */
	public  static double[] getDoubles(Request request, String name) {
		String text = request.getParameter(name);
		if(text==null) {
			throw new RuntimeException("parameter not found: "+name);
		}
		String[] texts = text.split(",");
		if(texts.length<1) {
			throw new RuntimeException("error in parameter: "+name+"    "+text);
		}
		double[] r = new double[texts.length];
		for (int i = 0; i < r.length; i++) {			
			try {
				r[i] = Double.parseDouble(texts[i]);
			} catch (Exception e) {
				throw new RuntimeException("wrong number format in parameter "+name+": '"+text+"'"+"   '"+texts[i]+"'");
			}
		}
		return r;
	}

	/**
	 * get one or more Strings separated by ","
	 * @param request
	 * @param name
	 * @return
	 */
	public  static String[] getStrings(Request request, String name) {
		String text = request.getParameter(name);
		if(text==null) {
			throw new RuntimeException("parameter not found: "+name);
		}
		String[] texts = text.split(",");
		if(texts.length<1) {
			throw new RuntimeException("error in parameter: "+name+"    "+text);
		}
		String[] r = new String[texts.length];
		for (int i = 0; i < r.length; i++) {			
			try {
				r[i] = texts[i].trim();
			} catch (Exception e) {
				throw new RuntimeException("wrong number format in parameter "+name+": '"+text+"'"+"   '"+texts[i]+"'");
			}
		}
		return r;
	}

	public  static int getInt(Request request, String name) {
		String text = request.getParameter(name);
		if(text==null) {
			throw new RuntimeException("parameter not found: "+name);
		}
		int v = Integer.parseInt(text);
		return v;
	}

	public  static String getString(Request request, String name, String defaultValue) {
		String text = request.getParameter(name);
		return text==null?defaultValue:text;
	}

	/**
	 * get one or more integers separated by ","
	 * @param request
	 * @param name
	 * @return
	 */
	public  static int[] getInts(Request request, String name) {
		String text = request.getParameter(name);
		if(text==null) {
			throw new RuntimeException("parameter not found: "+name);
		}
		String[] texts = text.split(",");
		if(texts.length<1) {
			throw new RuntimeException("error in parameter: "+name+"    "+text);
		}
		int[] r = new int[texts.length];
		for (int i = 0; i < r.length; i++) {
			r[i] = Integer.parseInt(texts[i]);
		}
		return r;
	}

	public static boolean has(Request request, String name) {
		return request.getParameter(name)!=null;
	}

	public  static int getInt(Request request, String name, int defaultValue) {
		String text = request.getParameter(name);
		if(text==null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			log.warn(e);
			return defaultValue;
		}
	}

	public static boolean getBoolean(Request request, String name, boolean defaultValue) {
		String text = request.getParameter(name);
		if(text==null) {
			return defaultValue;
		}
		if(text.equalsIgnoreCase("true")) {
			return true;
		}
		if(text.equalsIgnoreCase("false")) {
			return false;
		}
		log.warn("value unknown return default: "+text);
		return defaultValue;
	}
	
	public static boolean getFlagBoolean(Request request, String name) {
		String text = request.getParameter(name);
		if(text==null) {
			return false;
		}
		if(text.isEmpty()) {
			return true;
		}
		if(text.equalsIgnoreCase("true")) {
			return true;
		}
		if(text.equalsIgnoreCase("false")) {
			return false;
		}
		throw new RuntimeException("unknown flag value for " + name);
	}

	public static UserIdentity getUserIdentity(Request request) {
		Authentication authentication = request.getAuthentication();
		if(authentication == null || !(authentication instanceof User)) {
			return null;
		}
		UserIdentity userIdentity = ((User) authentication).getUserIdentity();
		return userIdentity;
	}

	public static StringBuilder getRequestLogString(String handlerText, String target, Request request) {
		String user = "?";
		UserIdentity userIdentity = Web.getUserIdentity(request);
		if(userIdentity!=null) {
			//log.info("userIdentity "+userIdentity);
			user = userIdentity.getUserPrincipal().getName();
		}
		StringBuilder s = new StringBuilder();
		s.append(timestampText(LocalDateTime.now()));
		s.append(" ");
		s.append(user);
		s.append(" ");
		s.append(request.getRemoteAddr());
		s.append(" ");
		s.append('[');
		s.append(handlerText);
		s.append("] ");
		s.append(target);
		String qs = request.getQueryString();
		if(qs!=null) {
			s.append("?");
			s.append(request.getQueryString());
		}
		String referer = request.getHeader("Referer");
		if(referer!=null) {
			s.append("\t\tref ");
			s.append(referer);
		}
		return s;		
	}

	public static char[] timestampText(LocalDateTime timestamp) {
		char[] c = new char[23];
		LocalDate date = timestamp.toLocalDate();	
		{
			int y = date.getYear();
			c[0] = (char) ('0'+((char) (y/1000)));
			c[1] = (char) ('0'+((char) ((y%1000)/100)));
			c[2] = (char) ('0'+((char) ((y%100)/10)));
			c[3] = (char) ('0'+((char) (y%10)));
		}
		c[4] = '-';
		{
			int m = date.getMonthValue();
			c[5] = (char) ('0'+((char) (m/10)));
			c[6] = (char) ('0'+((char) (m%10)));
		}
		c[7] = '-';
		{
			int d = date.getDayOfMonth();
			c[8] = (char) ('0'+((char) (d/10)));
			c[9] = (char) ('0'+((char) (d%10)));
		}
		c[10] = 'T';
		LocalTime time = timestamp.toLocalTime();
		{
			int h = time.getHour();
			c[11] = (char) ('0'+((char) (h/10)));
			c[12] = (char) ('0'+((char) (h%10)));
		}
		c[13] = ':';
		{
			int h = time.getMinute();
			c[14] = (char) ('0'+((char) (h/10)));
			c[15] = (char) ('0'+((char) (h%10)));
		}
		c[16] = ':';
		{
			int s = time.getSecond();
			c[17] = (char) ('0'+((char) (s/10)));
			c[18] = (char) ('0'+((char) (s%10)));
		}
		c[19] = '.';
		{
			int m = time.getNano() / 1_000_000;
			c[20] = (char) ('0'+((char) (m/100)));
			c[21] = (char) ('0'+((char) ((m%100)/10)));
			c[22] = (char) ('0'+((char) (m%10)));
		}
		return c;
	}

	public static String requestContentToString(Request request) throws IOException {
		return new String(readAllBytes(request.getInputStream(),request.getContentLength()), StandardCharsets.UTF_8);
	}
	
	public static JSONObject requestContentToJSON(Request request) throws IOException {
		return new JSONObject(requestContentToString(request));
	}

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
	public static byte[] readAllBytes(InputStream in) throws IOException {
		return readAllBytes(in, DEFAULT_BUFFER_SIZE);
	}

	//derived from JDK 9
	public static byte[] readAllBytes(InputStream in, int startBufferSize) throws IOException {
		byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
		int capacity = buf.length;
		int nread = 0;
		int n;
		for (;;) {
			while ((n = in.read(buf, nread, capacity - nread)) > 0)
				nread += n;
			if (n < 0)
				break;
			if (capacity <= MAX_BUFFER_SIZE - capacity) {
				capacity = capacity << 1;
			} else {
				if (capacity == MAX_BUFFER_SIZE)
					throw new OutOfMemoryError("Required array size too large");
				capacity = MAX_BUFFER_SIZE;
			}
			buf = Arrays.copyOf(buf, capacity);
		}
		return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
	}

	public static byte[] getRequestContent(Request request) throws IOException {
		long reqest_size = request.getContentLength();
		if(reqest_size < 1) {
			return new byte[0];
		}
		byte[] raw = new byte[(int) reqest_size];
		ServletInputStream in = request.getInputStream();
		int pos = 0;
		while(pos < reqest_size) {
			//log.info("read at "+pos+" of "+reqest_size);
			int read_size = in.read(raw, pos, (int) (reqest_size - pos));
			if(read_size < 1) {
				throw new RuntimeException("not all bytes read "+pos+"  of  " + reqest_size);
			}
			pos += read_size;
		}
		if(pos != reqest_size) {
			throw new RuntimeException("not all bytes read "+pos+"  of  " + reqest_size);
		}
		return raw;
	}

	public static JSONObject getRequestContentJSON(Request request) throws IOException {
		byte[] data = getRequestContent(request);
		if(data.length == 0) {
			throw new RuntimeException("empty request body");
		}
		return new JSONObject(new String(data, StandardCharsets.UTF_8));
	}

	public static boolean getFlag(Request request, String name) {
		return request.getParameter(name) != null;
	}
}
