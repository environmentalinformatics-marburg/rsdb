package broker;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONWriter;

import util.JsonUtil;
import util.collections.ReadonlyList;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class Informal {
	private static final Logger log = LogManager.getLogger();

	public final static Informal EMPTY = new Builder().build();

	public final String title;
	public final String description;
	public final ReadonlyList<String> tags;
	public final String acquisition_date;
	public final String corresponding_contact;

	public final String dc_Source;
	public final String dc_Relation;
	public final String dc_Coverage;
	public final String dc_Creator;
	public final String dc_Contributor;
	public final String dc_Rights;
	public final String dc_Audience;
	public final String dc_Provenance;
	
	public final InformalProperties properties;

	public static Informal ofYaml(YamlMap yamlMap) {
		return new Builder(yamlMap).build();
	}

	private Informal(Builder builder) {
		title = builder.title;
		description = builder.description;
		tags = builder.tags;
		acquisition_date = builder.acquisition_date;
		corresponding_contact = builder.corresponding_contact;

		dc_Source = builder.dc_Source;
		dc_Relation = builder.dc_Relation;
		dc_Coverage = builder.dc_Coverage;
		dc_Creator = builder.dc_Creator;
		dc_Contributor = builder.dc_Contributor;
		dc_Rights = builder.dc_Rights;
		dc_Audience = builder.dc_Audience;
		dc_Provenance = builder.dc_Provenance;
		properties = builder.properties.build();
	}

	public void writeYaml(Map<String, Object> map) {
		if (hasTitle()) {
			map.put("title", title);
		}
		if (hasDescription()) {
			map.put("description", description);
		}
		map.put("tags", tags);
		if (hasAcquisition_date()) {
			map.put("acquisition_date", acquisition_date);
		}
		if (has_corresponding_contact()) {
			map.put("corresponding_contact", corresponding_contact);
		}

		if (has_dc_Source()) {
			map.put("Source", dc_Source);
		}
		if (has_dc_Relation()) {
			map.put("Relation", dc_Relation);
		}
		if (has_dc_Coverage()) {
			map.put("Coverage", dc_Coverage);
		}
		if (has_dc_Creator()) {
			map.put("Creator", dc_Creator);
		}
		if (has_dc_Contributor()) {
			map.put("Contributor", dc_Contributor);
		}
		if (has_dc_Rights()) {
			map.put("Rights", dc_Rights);
		}
		if (has_dc_Audience()) {
			map.put("Audience", dc_Audience);
		}
		if (has_dc_Provenance()) {
			map.put("Provenance", dc_Provenance);
		}

		if(hasProperties()) {
			map.put("properties", properties.toYaml());
		}
	}

	public void writeJson(JSONWriter json) {
		JsonUtil.put(json, "title", title);
		JsonUtil.put(json, "description", description);
		JsonUtil.writeOptList(json, "tags", tags);
		JsonUtil.optPut(json, "acquisition_date", acquisition_date);
		JsonUtil.optPut(json, "corresponding_contact", corresponding_contact);

		JsonUtil.optPut(json, "Source", dc_Source);
		JsonUtil.optPut(json, "Relation", dc_Relation);
		JsonUtil.optPut(json, "Coverage", dc_Coverage);
		JsonUtil.optPut(json, "Creator", dc_Creator);
		JsonUtil.optPut(json, "Contributor", dc_Contributor);
		JsonUtil.optPut(json, "Rights", dc_Rights);
		JsonUtil.optPut(json, "Audience", dc_Audience);
		JsonUtil.optPut(json, "Provenance", dc_Provenance);

		json.key("properties");
		properties.writeJson(json);
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public boolean hasDescription() {
		return !description.isEmpty();
	}

	public boolean hasProperties() {
		return !properties.isEmpty();
	}

	public boolean hasTitle() {
		return !title.isEmpty();
	}

	public boolean hasAcquisition_date() {
		return !acquisition_date.isEmpty();
	}

	public boolean has_corresponding_contact() {
		return !corresponding_contact.isEmpty();
	}


	public boolean has_dc_Source() {
		return !dc_Source.isEmpty();
	}

	public boolean has_dc_Relation() {
		return !dc_Relation.isEmpty();
	}

	public boolean has_dc_Coverage() {
		return !dc_Coverage.isEmpty();
	}

	public boolean has_dc_Creator() {
		return !dc_Creator.isEmpty();
	}

	public boolean has_dc_Contributor() {
		return !dc_Contributor.isEmpty();
	}

	public boolean has_dc_Rights() {
		return !dc_Rights.isEmpty();
	}

	public boolean has_dc_Audience() {
		return !dc_Audience.isEmpty();
	}

	public boolean has_dc_Provenance() {
		return !dc_Provenance.isEmpty();
	}
	
	@Override
	public String toString() {
		return "Informal [title=" + title + ", description=" + description + ", tags=" + tags + ", acquisition_date="
				+ acquisition_date + "]";
	}

	public static class Builder {
		public String title = "";
		public String description = "";
		public ReadonlyList<String> tags = ReadonlyList.EMPTY;
		public String acquisition_date = "";
		public String corresponding_contact = "";

		public String dc_Source = "";
		public String dc_Relation = "";
		public String dc_Coverage = "";
		public String dc_Creator = "";
		public String dc_Contributor = "";
		public String dc_Rights = "";
		public String dc_Audience = "";
		public String dc_Provenance = "";

		public InformalProperties.Builder properties = new InformalProperties.Builder();
		
		public Builder() {}

		private Builder(Informal informal) {
			title = informal.title;
			description = informal.description;
			tags = informal.tags;
			acquisition_date = informal.acquisition_date;
			corresponding_contact = informal.corresponding_contact;

			dc_Source = informal.dc_Source;
			dc_Relation = informal.dc_Relation;
			dc_Coverage = informal.dc_Coverage;
			dc_Creator = informal.dc_Creator;
			dc_Contributor = informal.dc_Contributor;
			dc_Rights = informal.dc_Rights;
			dc_Audience = informal.dc_Audience;
			dc_Provenance = informal.dc_Provenance;

			properties = new InformalProperties.Builder(informal.properties);		
		}

		public Builder(YamlMap yamlMap) {
			title = yamlMap.optString("title", "");
			description = yamlMap.optString("description", "");
			tags = yamlMap.optList("tags").asReadonlyStrings();
			acquisition_date = yamlMap.optString("acquisition_date", "");
			corresponding_contact = yamlMap.optString("corresponding_contact", "");

			dc_Source = yamlMap.optString("Source", "");
			dc_Relation = yamlMap.optString("Relation", "");
			dc_Coverage = yamlMap.optString("Coverage", "");
			dc_Creator = yamlMap.optString("Creator", "");
			dc_Contributor = yamlMap.optString("Contributor", "");
			dc_Rights = yamlMap.optString("Rights", "");
			dc_Audience = yamlMap.optString("Audience", "");
			dc_Provenance = yamlMap.optString("Provenance", "");
			
			properties.map.clear();
			YamlMap propertyMap = yamlMap.optMap("properties");
			for(String tag : propertyMap.keys()) {
				Vec<String> contents = new Vec<String>();
				propertyMap.optList(tag).asStrings(contents::add);
				properties.map.put(tag, contents);
			}
		}

		public Informal build() {
			return new Informal(this);
		}

		public void setTags(String... tags) {
			this.tags = ReadonlyList.of(tags);
		}
	}
}
