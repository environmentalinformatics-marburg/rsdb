package pointdb.processing.tilekey;

public interface TileKeyProducer {
	void produce(TileKeyConsumer tileKeyConsumer);

	default boolean isEmpty() {
		return TileKeyIsEmptyCollector.isEmpty(this);
	}
}
