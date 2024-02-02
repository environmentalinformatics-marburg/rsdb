package postgis;

import postgis.PostgisLayer.PostgisColumn;
import util.collections.array.ReadonlyArray;

public interface FieldsConsumer {
	
	void acceptStart();
	
	void acceptFields(ReadonlyArray<PostgisColumn> fields);
	
	void acceptCellsStart(int i);
	
	void acceptCell(int i, String fieldValue);
	void acceptCellNull(int i);
	void acceptCellInt16(int i, short fieldValue);
	void acceptCellInt32(int i, int fieldValue);
	void acceptCellFloat64(int i, double fieldValue);
	
	void acceptCellsEnd(int i);
	
	void acceptEnd();
}
