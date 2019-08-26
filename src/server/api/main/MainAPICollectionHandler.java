package server.api.main;

import broker.Broker;

public class MainAPICollectionHandler extends APICollectionHandler {

	public MainAPICollectionHandler(Broker broker) {
		addMethod(new APIHandler_poi_groups(broker));
		addMethod(new APIHandler_poi_group(broker));
		addMethod(new APIHandler_roi_groups(broker));
		addMethod(new APIHandler_roi_group(broker));
		addMethod(new APIHandler_identity(broker));
		addMethod(new APIHandler_create_raster(broker));
		addMethod(new APIHandler_catalog_json(broker));
		addMethod(new APIHandler_session(broker));
		addMethod(new APIHandler_roles(broker));
		addMethod(new APIHandler_layer_tags(broker));
		addMethod(new APIHandler_accounts(broker));
		APIHandler_upload upload = new APIHandler_upload(broker);
		addMethod(upload);
		addMethod(new APIHandler_inspect(broker, upload.chunkedUploader));
		addMethod(new APIHandler_import(broker, upload.chunkedUploader));
		addMethod(new APIHandler_remote_tasks(broker));
		addMethod(new APIHandler_remote_task_entries(broker));
		addMethod(new APIHandler_proxy(broker));
	}

}
