package util.tiff;

import java.util.ArrayList;

public class GeoKeyDirectory {
	private ArrayList<GeoKey_Entry> list = new ArrayList<GeoKey_Entry>(20);
	private StringBuilder asciiCollector = new StringBuilder();
	
	public void add(GeoKey_Entry e) {
		list.add(e);
	}
	
	
	public short[] get_directory_content() {
		int len = list.size();		
		short KeyDirectoryVersion = 1;
		short KeyRevision = 1;
		short MinorRevision = 0;
		short NumberOfKeys = (short) len;
		short[] content = new short[4*(len+1)];
		content[0] = KeyDirectoryVersion;
		content[1] = KeyRevision;
		content[2] = MinorRevision;
		content[3] = NumberOfKeys;
		
		for(int i=0;i<len;i++) {
			GeoKey_Entry e = list.get(i);
			int pos = 4*(i+1);
			content[pos] = e.id;
			content[pos+1] = e.target;
			content[pos+2] = e.count;
			content[pos+3] = e.offset;
		}
		
		
		return content;
	}
	
	public CharSequence get_ascii_content() {
		return asciiCollector;
	}
	
	public void add_ModelType(short modelType) {
		add(new GeoKey_GTModelTypeGeoKey(modelType));
	}
	
	public void add_ModelType_ModelTypeProjected() {
		add_ModelType((short) 1);
	}
	
	public void add_RasterType(short rasterType) {
		add(new GeoKey_GTRasterTypeGeoKey(rasterType));
	}
	
	public void add_RasterType_RasterPixelIsArea() {
		add_RasterType((short) 1);
	}
	
	public void add_ProjectedCSType(short epsg) {
		add(new GeoKey_ProjectedCSTypeGeoKey(epsg));
	}
	
	public void add_Citation(String text) { // GTCitationGeoKey
		add(new GeoKey_ASCII((short)1026, text, asciiCollector));
	}


}
