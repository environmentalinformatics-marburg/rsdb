package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.TemplateLoader;
import com.samskivert.mustache.Template;

public class TemplateUtil {
	
	public static final TemplateLoader TEMPLATE_LOADER = new Mustache.TemplateLoader() {
		public Reader getTemplate (String filename) throws FileNotFoundException {
			return new FileReader(new File("mustache", filename));
		}
	};
	
	private static final ConcurrentHashMap<String, Template> map = new ConcurrentHashMap<String, Template>();
	
	private static Template createTemplate(String filename) {
		String text;
		try {
			text = new String(Files.readAllBytes(Paths.get("mustache", filename)), Charset.forName("UTF-8"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
		return Mustache.compiler().withLoader(TemplateUtil.TEMPLATE_LOADER).compile(text);
	}
	
	public static Template getTemplate(String filename, boolean refresh) {
		if(refresh) {
			Template template = createTemplate(filename);
			map.put(filename, template);
			return template;
		}
		Template template = map.computeIfAbsent(filename, TemplateUtil::createTemplate);
		return template;
	}
}
