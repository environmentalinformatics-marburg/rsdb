package server.api.postgis;

import java.io.PrintWriter;

import org.json.JSONObject;

import postgis.FeatureConsumer;
import postgis.PostgisLayer.PostgisColumn;
import util.collections.array.ReadonlyArray;

public class GeojsonWriter implements FeatureConsumer {

	private final PrintWriter out;
	private final int epsg;
	private String[] fieldKeys;
	private boolean isFirstWrittenFeature;
	private boolean isFirstWrittenFeatureCell;

	public GeojsonWriter(PrintWriter out, int epsg) {
		this.out = out;
		this.epsg = epsg;
	}

	@Override
	public void acceptFields(ReadonlyArray<PostgisColumn> fields) {
		isFirstWrittenFeature = true;

		if(fields == null) {
			fieldKeys = new String[0];
		} else {
			fieldKeys = new String[fields.size()];
			for (int i = 0; i < fieldKeys.length; i++) {
				fieldKeys[i] = JSONObject.quote(fields.get(i).name) + ":";
			}
		}
	}
	
	@Override
	public void acceptStart() {
		out.print("{\"type\":\"FeatureCollection\"");
		if(epsg > 0) {
			out.print(",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"urn:ogc:def:crs:EPSG::");
			out.print(epsg);
			out.print("\"}}");
		}
		out.print(",\"features\":");
		out.print('[');		
	}

	@Override
	public void acceptFeatureStart(int i) {
		if(isFirstWrittenFeature) {
			isFirstWrittenFeature = false;
		} else {
			out.print(',');						
		}
		out.print("\n{\"type\":\"Feature\"");			
	}

	@Override
	public void acceptGeometry(String geometry) {
		out.print(",\"geometry\":");
		out.print(geometry);				
	}

	@Override
	public void acceptCellsStart(int i) {
		out.print(",\"properties\":{");
		isFirstWrittenFeatureCell = true;
	}

	@Override
	public void acceptCell(int i, String fieldValue) {
		if(isFirstWrittenFeatureCell) {
			isFirstWrittenFeatureCell = false;
		} else {
			out.print(',');						
		}
		out.print(fieldKeys[i]);	
		out.print(JSONObject.quote(fieldValue));
	}

	@Override
	public void acceptCellInt16(int i, short fieldValue) {
		if(isFirstWrittenFeatureCell) {
			isFirstWrittenFeatureCell = false;
		} else {
			out.print(',');						
		}				
		out.print(fieldKeys[i]);	
		out.print(fieldValue);
	}

	@Override
	public void acceptCellInt32(int i, int fieldValue) {
		if(isFirstWrittenFeatureCell) {
			isFirstWrittenFeatureCell = false;
		} else {
			out.print(',');						
		}				
		out.print(fieldKeys[i]);	
		out.print(fieldValue);				
	}

	@Override
	public void acceptCellFloat64(int i, double fieldValue) {
		if(isFirstWrittenFeatureCell) {
			isFirstWrittenFeatureCell = false;
		} else {
			out.print(',');						
		}				
		out.print(fieldKeys[i]);	
		out.print(JSONObject.doubleToString(fieldValue));				
	}

	@Override
	public void acceptCellNull(int i) {
		/*if(isFirstWrittenFeatureCell) {
			isFirstWrittenFeatureCell = false;
		} else {
			out.print(',');						
		}
		out.print(fieldKeys[i]);	
		out.print("null");*/					
	}

	@Override
	public void acceptCellsEnd(int i) {
		out.print('}');			
	}

	@Override
	public void acceptFeatureEnd(int i) {
		out.print('}');
	}

	@Override
	public void acceptEnd() {
		out.print('\n');
		out.print(']');
		out.print('}');
	}
}