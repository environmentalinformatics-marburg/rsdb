package broker.group;

import broker.Informal;
import broker.acl.ACL;

public class RoiGroup {	
	
	public final String name;
	public final Informal informal;
	public final ACL acl;
	public final String epsg;
	public final String proj4;
	public final Roi[] rois;
	
	public RoiGroup(String name, Informal informal, ACL acl, Roi... rois) {
		this(name, informal,acl, "", "", rois);
	}
	

	public RoiGroup(String name, Informal informal, ACL acl, String epsg, String proj4, Roi... rois) {
		this.name = name;
		this.informal =  informal;
		this.acl = acl;
		this.epsg = epsg;
		this.proj4 = proj4;
		this.rois = rois;
	}	
}

