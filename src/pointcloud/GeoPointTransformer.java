package pointcloud;

import java.util.stream.Stream;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

public class GeoPointTransformer {
	
	public static final AttributeSelector FULL_GEOPOINT_SELECTOR = new AttributeSelector().setXYZI().setReturns().setScanAngleRank().setClassification();
	
	public static Vec<GeoPoint> transform(Stream<PointTable> pointTables) {
		Vec<GeoPoint> list = new Vec<GeoPoint>();
		pointTables.sequential().forEach(p -> {
			double[] px = p.x;
			double[] py = p.y;
			double[] pz = p.z;
			char[] pintensity = p.intensity;
			byte[] preturnNumber = p.returnNumber;
			byte[] preturns = p.returns;
			byte[] pscanAngleRank = p.scanAngleRank;
			byte[] pclassification = p.classification;
			int len = p.rows;
			for (int i = 0; i < len; i++) {
				double x = px == null ? 0d : px[i];
				double y = py == null ? 0d : py[i];
				double z = pz == null ? 0d : pz[i];
				char intensity = pintensity == null ? 0 : pintensity[i];
				byte returnNumber = preturnNumber == null ? 0 : preturnNumber[i];
				byte returns = preturns == null ? 0 : preturns[i];
				byte scanAngleRank = pscanAngleRank == null ? 0 : pscanAngleRank[i];
				byte classification = pclassification == null ? 0 : pclassification[i];
				byte classificationFlags = 0; // TODO ?
				GeoPoint geoPoint = new GeoPoint(x, y, z, intensity, returnNumber, returns, scanAngleRank, classification, classificationFlags); 
				list.add(geoPoint);
			}
		});
		return list;
	}

}
