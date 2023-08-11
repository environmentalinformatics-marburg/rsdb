package postgis;

import postgis.PostgisLayer.PostgisColumn;

public interface FeatureConsumer {
	void acceptFeatureStart(boolean isFirstFeature);
	void acceptFeatureGeometry(String geometry);
	void acceptFeatureFieldsStart();
	void acceptFeatureField(PostgisColumn field, String fieldValue, boolean isFirstFeatureField);
	void acceptFeatureFieldNull(PostgisColumn field, boolean isFirstFeatureField);
	void acceptFeatureFieldInt32(PostgisColumn field, int fieldValue, boolean isFirstFeatureField);
	void acceptFeatureFieldsEnd();
	void acceptFeatureEnd();
}