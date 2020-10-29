package server.api.voxeldbs;

import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.jetty.server.Response;

import util.Extent2d;
import util.Extent3d;
import util.Range3d;
import util.rdat.Rdat;
import util.rdat.RdatList;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class VoxelWriter {
	
	public static void writeBool8(byte[] data, String name, VoxelGeoRef ref, Range3d voxelRange, Response response, String format) throws IOException {
		Extent3d geoExtent = ref.toGeoExtent(voxelRange);

		switch(format) {
		case "js": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.writeInt(voxelRange.xlen());
			out.writeInt(voxelRange.ylen());
			out.writeInt(voxelRange.zlen());	
			out.write(data);
			break;
		}
		case "rdat": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.write(Rdat.SIGNATURE_RDAT);
			out.write(Rdat.RDAT_TYPE_DIM_VECTOR);

			RdatList metaList = new RdatList();
			metaList.addString("name", name);
			if(ref.hasProj4()) {
				metaList.addString("PROJ4", ref.proj4);
			}
			if(ref.hasEpsg()) {
				metaList.addInt32("EPSG", ref.epsg);
			}
			metaList.addFloat64("xmin", geoExtent.xmin);
			metaList.addFloat64("ymin", geoExtent.ymin);
			metaList.addFloat64("zmin", geoExtent.zmin);
			
			metaList.addFloat64("xmax", geoExtent.xmax);
			metaList.addFloat64("ymax", geoExtent.ymax);
			metaList.addFloat64("zmax", geoExtent.zmax);
			metaList.write(out);		

			Rdat.write_RDAT_VDIM_bool8(out, data, voxelRange.xlen(), voxelRange.ylen(), voxelRange.zlen());
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}				
	}
	
	public static void writeUint8(byte[] data, VoxelDB voxeldb, Range3d voxelRange, Response response, String format) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();
		Extent3d geoExtent = ref.toGeoExtent(voxelRange);

		switch(format) {
		case "js": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.writeInt(voxelRange.xlen());
			out.writeInt(voxelRange.ylen());
			out.writeInt(voxelRange.zlen());	
			out.write(data);
			break;
		}
		case "rdat": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.write(Rdat.SIGNATURE_RDAT);
			out.write(Rdat.RDAT_TYPE_DIM_VECTOR);

			RdatList metaList = new RdatList();
			metaList.addString("name", voxeldb.getName());
			if(ref.hasProj4()) {
				metaList.addString("PROJ4", ref.proj4);
			}
			if(ref.hasEpsg()) {
				metaList.addInt32("EPSG", ref.epsg);
			}
			metaList.addFloat64("xmin", geoExtent.xmin);
			metaList.addFloat64("ymin", geoExtent.ymin);
			metaList.addFloat64("zmin", geoExtent.zmin);
			
			metaList.addFloat64("xmax", geoExtent.xmax);
			metaList.addFloat64("ymax", geoExtent.ymax);
			metaList.addFloat64("zmax", geoExtent.zmax);
			metaList.write(out);		

			Rdat.write_RDAT_VDIM_uint8(out, data, voxelRange.xlen(), voxelRange.ylen(), voxelRange.zlen());
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}				
	}
	
	public static void writeUint16(byte[] data, VoxelDB voxeldb, Range3d voxelRange, Response response, String format) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();
		Extent3d geoExtent = ref.toGeoExtent(voxelRange);

		switch(format) {
		case "js": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.writeInt(voxelRange.xlen());
			out.writeInt(voxelRange.ylen());
			out.writeInt(voxelRange.zlen());	
			out.write(data);
			break;
		}
		case "rdat": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.write(Rdat.SIGNATURE_RDAT);
			out.write(Rdat.RDAT_TYPE_DIM_VECTOR);

			RdatList metaList = new RdatList();
			metaList.addString("name", voxeldb.getName());
			if(ref.hasProj4()) {
				metaList.addString("PROJ4", ref.proj4);
			}
			if(ref.hasEpsg()) {
				metaList.addInt32("EPSG", ref.epsg);
			}
			metaList.addFloat64("xmin", geoExtent.xmin);
			metaList.addFloat64("ymin", geoExtent.ymin);
			metaList.addFloat64("zmin", geoExtent.zmin);
			
			metaList.addFloat64("xmax", geoExtent.xmax);
			metaList.addFloat64("ymax", geoExtent.ymax);
			metaList.addFloat64("zmax", geoExtent.zmax);
			metaList.write(out);		

			Rdat.write_RDAT_VDIM_uint16(out, data, voxelRange.xlen(), voxelRange.ylen(), voxelRange.zlen());
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}				
	}
	
	public static void writeInt32(byte[] data, String name, VoxelGeoRef ref, Range3d voxelRange, Response response, String format) throws IOException {
		Extent3d geoExtent = ref.toGeoExtent(voxelRange);

		switch(format) {
		case "js": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.writeInt(voxelRange.xlen());
			out.writeInt(voxelRange.ylen());
			out.writeInt(voxelRange.zlen());	
			out.write(data);
			break;
		}
		case "rdat": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.write(Rdat.SIGNATURE_RDAT);
			out.write(Rdat.RDAT_TYPE_DIM_VECTOR);

			RdatList metaList = new RdatList();
			metaList.addString("name", name);
			if(ref.hasProj4()) {
				metaList.addString("PROJ4", ref.proj4);
			}
			if(ref.hasEpsg()) {
				metaList.addInt32("EPSG", ref.epsg);
			}
			metaList.addFloat64("xmin", geoExtent.xmin);
			metaList.addFloat64("ymin", geoExtent.ymin);
			metaList.addFloat64("zmin", geoExtent.zmin);
			
			metaList.addFloat64("xmax", geoExtent.xmax);
			metaList.addFloat64("ymax", geoExtent.ymax);
			metaList.addFloat64("zmax", geoExtent.zmax);
			metaList.write(out);		

			Rdat.write_RDAT_VDIM_int32(out, data, voxelRange.xlen(), voxelRange.ylen(), voxelRange.zlen());
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}				
	}
}
