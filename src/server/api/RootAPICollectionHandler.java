package server.api;

import broker.Broker;

public class RootAPICollectionHandler extends APICollectionHandler {

	public RootAPICollectionHandler(Broker broker) {
		super(false);
		addMethod(new APIHandler_stac(broker));
		addMethod(new APIHandler_records(broker));
	}
}
