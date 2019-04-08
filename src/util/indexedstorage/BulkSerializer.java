package util.indexedstorage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;

public interface BulkSerializer<T> {

	void serializeBulk(DataOutput out, T[] vs) throws IOException;	
	T[] deserializeBulk(DataInput in) throws IOException;
	Class<T> getEntryClass();
	
	default void serializeBulk(DataOutput out, Collection<T> vs) throws IOException {		
		serializeBulk(out, (T[]) vs.toArray((T[]) Array.newInstance(getEntryClass(), 0)));
	}
}
