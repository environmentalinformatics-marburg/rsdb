package broker;

import java.nio.file.Path;

import org.json.JSONWriter;
import org.tinylog.Logger;

import util.Table;
import util.Table.ColumnReaderBoolean;
import util.Table.ColumnReaderString;
import util.collections.vec.Vec;

public class MetaDataSchema {

	public static class Entry {
		public final String name;
		public final String description;
		public final boolean fixed;

		public Entry(String name, String description, boolean fixed) {
			this.name = name;
			this.description = description;
			this.fixed = fixed;
		}
	}

	private final Path metaDataSchemaPath;
	public final String meta_data_schema_description;
	private Vec<Entry> entries = new Vec<Entry>(); 

	public MetaDataSchema(Path metaDataSchemaPath, String meta_data_schema_description) {
		this.metaDataSchemaPath = metaDataSchemaPath;
		this.meta_data_schema_description = meta_data_schema_description;
		read();
	}

	private void read() {
		try {
			if(!metaDataSchemaPath.toFile().exists()) {
				throw new RuntimeException("missing meta data schema file");
			}
			Table table = Table.readCSV(metaDataSchemaPath, ',');

			ColumnReaderString colName = table.createColumnReader("name");
			ColumnReaderBoolean colFixed = table.createColumnReaderBooleanGeneral("fixed", false);		
			ColumnReaderString colDescription = table.createColumnReader("description", "");

			for(String[] row : table.rows) {
				String name = colName.get(row);
				if(name.startsWith("#")) {
					continue;
				}
				boolean fixed = colFixed.get(row);
				String description = colDescription.get(row);
				Entry entry = new Entry(name, description, fixed);
				entries.add(entry);
			}
		} catch(Exception e) {
			Logger.warn(e.getMessage());
			setDefault();
		}
	}

	private void setDefault() {
		entries.clear();
		for(String property : DEFAULT_META_DATA_SCHEMA_DUBLIN_CORE_PROPERTIES) {
			Entry entry = new Entry(property, "", false);
			entries.add(entry);
		}
	}

	public void toJSON(JSONWriter json) {
		json.array();
		for(Entry entry : entries) {
			json.object();
			json.key("name");
			json.value(entry.name);
			json.key("fixed");
			json.value(entry.fixed);
			json.key("description");
			json.value(entry.description);
			json.endObject();
		}
		json.endArray();
	}

	private static String[] DEFAULT_META_DATA_SCHEMA_DUBLIN_CORE_PROPERTIES = new String[] {
			"accrualMethod",
			"accrualPeriodicity",
			"accrualPolicy",
			"audience",
			"audience.educationLevel",
			"audience.mediator",
			"contributor",
			"contributor.creator",
			"coverage",
			"coverage.spatial",
			"coverage.temporal",
			"creator",
			"date",
			"date.available",
			/*"date.created",*/
			"date.dateAccepted",
			"date.dateCopyrighted",
			"date.dateSubmitted",
			"date.issued",
			"date.modified",
			"date.valid",
			"description",
			/*"description.abstract",*/
			"description.tableOfContents",
			"format",
			"format.extent",
			"format.medium",
			"identifier",
			"identifier.bibliographicCitation",
			"instructionalMethod",
			"language",
			"provenance",
			/*"publisher",*/
			"relation",
			"relation.conformsTo",
			"relation.hasFormat",
			"relation.hasPart",
			"relation.hasVersion",
			"relation.isFormatOf",
			"relation.isPartOf",
			"relation.isReferencedBy",
			"relation.isReplacedBy",
			"relation.isRequiredBy",
			"relation.isVersionOf",
			"relation.references",
			"relation.replaces",
			"relation.requires",
			"relation.source",
			"rights",
			"rights.accessRights",
			"rights.license",
			"rightsHolder",
			"source",
			/*"subject",*/
			/*"title",*/
			"title.alternative",
			"type",                
	};
}
