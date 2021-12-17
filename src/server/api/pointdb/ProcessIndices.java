package server.api.pointdb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;
import org.mapdb.Fun.Pair;

import pointcloud.PointCloud;
import pointdb.PointDB;
import pointdb.process.DataProvider2;
import pointdb.process.Functions;
import pointdb.subsetdsl.Region;
import util.ResponseReceiver;
import util.Web;
import util.collections.vec.Vec;
import util.rdat.RdatDataFrame;

public class ProcessIndices {
	

	public static void process(int t, Vec<Pair<Region, String>> areas, Vec<String> functions, String format, Response response, PointDB db, PointCloud pointcloud, boolean omit_empty_areas) throws IOException {
		switch(format) {
		case "json": {
			response.setContentType(Web.MIME_JSON);
			PrintWriter writer = response.getWriter();
			JSONWriter json = new JSONWriter(writer);
			json.object();
			if(db != null) {
				json.key("title");
				String title = "PointDB: " + db.config.name;
				if(db.informal().hasTitle()) {
					title += " - " + db.informal().title;
				}
				json.value(title);
			} else if(pointcloud != null) {
				json.key("title");
				String title = "PointCloud: " + pointcloud.getName();
				if(pointcloud.informal().hasTitle()) {
					title += " - " + pointcloud.informal().title;
				}
				json.value(title);
			} 
			json.key("header");
			json.array();
			for(String f:functions) {
				json.value(f);
			}
			json.endArray();
			json.key("data");
			json.object();
			for(Pair<Region, String> s:areas) {
				Region region = s.a;
				DataProvider2 provider = new DataProvider2(pointcloud, db, t, region);
				if(!provider.get_regionPoints().isEmpty()) {
					double[] row = new double[functions.size()];
					for (int i = 0; i < functions.size(); i++) {
						try {
							double v = Functions.apply(provider, functions.get(i));
							if(Double.isFinite(v)) {
								row[i] = v;
							} else {
								row[i] = Double.NaN;	
							}
						} catch(Exception e) {
							e.printStackTrace();
							Logger.error(e);
							row[i] = Double.NaN;
						}
					}
					json.key(s.b);
					json.array();
					for(double v : row) {
						if(Double.isFinite(v)) {
							json.value(v);
						} else {
							json.value("NA");	
						}
					}
					json.endArray();
				}
				provider.old.dp = null;
				provider.old = null;
				provider = null;
			}
			json.endObject();
			json.endObject();
			/*response.setContentType(Web.MIME_JSON);
			PrintWriter writer = response.getWriter();
			JSONWriter json = new JSONWriter(writer);
			json.object();
			json.key("header");
			json.array();
			for(String f:functions) {
				json.value(f);
			}
			json.endArray();
			json.key("data");
			json.object();
			long timestamp = System.currentTimeMillis();
			for(Pair<Region, String> s:areas) {
				long t = System.currentTimeMillis();
				if(timestamp+500<=t) {
					try {
						writer.flush(); // detect HTTP connection closed
					} catch(Exception e) {
						Logger.error(e);
						return;
					}
					timestamp = t;
				}
				try {
					Region region = s.a;
					DataProvider2 provider = new DataProvider2(pointcloud, db, region);
					if(!provider.get_regionPoints().isEmpty()) {
						try {
							json.key(s.b);
							json.array();
						} catch(Exception e) {
							Logger.error(e);
							return;
						}
						for(String f:functions) {
							try {
								double v = Functions.apply(provider, f);
								if(Double.isFinite(v)) {
									try {
										json.value(v);
									} catch(Exception e) {
										Logger.error(e);
										return;
									}
								} else {
									try {
										json.value("NA");	
									} catch(Exception e) {
										Logger.error(e);
										return;
									}
								}
							} catch(Exception e) {
								e.printStackTrace();
								Logger.error(e);
								try {
									json.value("NA");
								} catch(Exception eClose) {
									Logger.error(eClose);
									return;
								}
							}
						}
						try {
							json.endArray();
						} catch(Exception e) {
							Logger.error(e);
							return;
						}
					}
				} catch(Exception e) {
					Logger.error(e);
				}
			}
			try {
				json.endObject();
				json.endObject();
			} catch(Exception e) {
				Logger.error(e);
				return;
			}*/
			break;
		}
		case "rdat": {
			Vec<Pair<String, double[]>> results = new Vec<Pair<String, double[]>>();			

			for(Pair<Region, String> s:areas) {
				try {
					Region region = s.a;
					DataProvider2 provider = new DataProvider2(pointcloud, db, t, region);
					if(!provider.get_regionPoints().isEmpty()) {
						double[] row = new double[functions.size()];
						for (int i = 0; i < functions.size(); i++) {
							try {
								double v = Functions.apply(provider, functions.get(i));
								if(Double.isFinite(v)) {
									row[i] = v;
								} else {
									row[i] = Double.NaN;	
								}
							} catch(Exception e) {
								e.printStackTrace();
								Logger.error(e);
								row[i] = Double.NaN;
							}
						}
						Pair<String, double[]> namedRow = new Pair<String, double[]>(s.b, row);
						results.add(namedRow);
					} else if (!omit_empty_areas) {
						double[] row = new double[functions.size()];
						for (int i = 0; i < functions.size(); i++) {
							row[i] = Double.NaN;
						}
						Pair<String, double[]> namedRow = new Pair<String, double[]>(s.b, row);
						results.add(namedRow);
					}
				} catch(Exception e) {
					Logger.error(e);
				}
			}

			//if(!results.isEmpty()) {
			RdatDataFrame<Pair<String, double[]>> df = new RdatDataFrame<Pair<String, double[]>>(Collection::size);
			for (int i = 0; i < functions.size(); i++) {
				final int pos = i;
				df.addString("name", d->d.a);
				df.addDouble(functions.get(i), d->d.b[pos]);
			}
			df.write(new ResponseReceiver(response), results);
			//}
			break;
		}
		default:
			throw new RuntimeException("unknown format "+format);
		}
	}

}
