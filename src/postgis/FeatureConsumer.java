package postgis;

import postgis.PostgisLayer.PostgisColumn;
import util.collections.array.ReadonlyArray;

public interface FeatureConsumer extends FieldsConsumer {
	
	// FieldsConsumer methods
	
	void acceptFields(ReadonlyArray<PostgisColumn> fields);
	
	void acceptFeatureStart(int i);
	
	void acceptGeometry(String geometry);
	
	// FieldsConsumer methods
	
	void acceptFeatureEnd(int i);	
}