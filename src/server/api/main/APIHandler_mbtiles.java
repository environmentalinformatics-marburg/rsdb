package server.api.main;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import server.api.APIHandler;

public class APIHandler_mbtiles extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_mbtiles(Broker broker) {
		super(broker, "mbtiles");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		String[] query = target.split("/");
		log.info(Arrays.toString(query));
		if(query.length != 4) {
			log.info("invalid query");
			return;
		}
		int z = Integer.parseInt(query[1]);
		int x = Integer.parseInt(query[3]);
		int y = Integer.parseInt(query[2]);

//		try {
//			MBTilesReader r = new MBTilesReader(new File("webfiles/testing.mbtiles"));
//			/*//metadata
//		MetadataEntry metadata = r.getMetadata();
//		String tileSetName = metadata.getTilesetName();
//		MetadataEntry.TileSetType type = metadata.getTilesetType();
//		String tilesetVersion = metadata.getTilesetVersion();
//		String description = metadata.getTilesetDescription();
//		MetadataEntry.TileMimeType tileMimeType = metadata.getTileMimeType();
//		MetadataBounds bounds = metadata.getTilesetBounds();
//		String attribution = metadata.getAttribution();
//		//tiles
//		TileIterator tiles = r.getTiles();
//		while (tiles.hasNext()) {
//			Tile next = tiles.next();
//			int zoom = next.getZoom();
//			int column = next.getColumn();
//			int row = next.getRow();
//			log.info(zoom + "  " + column + "  " + row);
//			InputStream tileData = next.getData();        
//		}
//		tiles.close();*/
//
//			Tile tile = r.getTile(z, y, x);
//			InputStream in = tile.getData();
//			//byte[] data = in.readAllBytes();
//			in.close();			
//			r.close();
//			//response.getOutputStream().write(data);		
//		} catch(Exception e) {
//			log.error(e);
//		}
	}
}
