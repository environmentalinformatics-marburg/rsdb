package server.api.pointdb;


import org.tinylog.Logger;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.RasterGrid;
import util.collections.vec.Vec;

public class VoxelGenerator {
	

	private final Rect rect;
	private final Vec<GeoPoint> points;

	public VoxelGenerator(Rect rect, Vec<GeoPoint> points) {
		this.rect = rect;
		this.points = points;
	}

	public RasterGrid[] get() {
		Logger.info("org rect "+rect);
		RasterGrid dtm = new DTM2_generator(rect, points).get();
		Logger.info("dtm rect "+dtm);
		Logger.info("data "+dtm.data[0].length+"  "+dtm.data.length);

		int zMax = 50;
		int topLevel = zMax-1;

		RasterGrid[] voxelLayers = new RasterGrid[zMax];
		for (int i = 0; i < zMax; i++) {
			voxelLayers[i] = new RasterGrid(dtm);
		}

		int xmn = dtm.local_min_x;
		int ymn = dtm.local_min_y;

		double[][] dtmData = dtm.data;

		for(GeoPoint p:points) {
			int x = (int) p.x - xmn;
			int y = (int) p.y - ymn;
			int z = (int) (p.z - dtmData[y][x]);
			if(z<0) {
				z=0;
			}
			if(z>topLevel) {
				z=topLevel;
			}
			voxelLayers[z].data[y][x]++;
		}
		return voxelLayers;
	}

}
