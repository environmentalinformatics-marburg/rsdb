package pointdb.process;

public abstract class ProcessingFun extends AbstractProcessingFun {
	
	public ProcessingFun() {
		super();
	}

	public ProcessingFun(Class<?> clazz) {
		super(clazz);
	}
	
	public ProcessingFun(String name, String description) {
		super(name, description);
	}

	public ProcessingFun(String name, String description, String[] tags) {
		super(name, description, tags);
	}

	public abstract double process(DataProvider2 provider);
}