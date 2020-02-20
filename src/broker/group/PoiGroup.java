package broker.group;

import broker.Informal;
import broker.acl.ACL;

public class PoiGroup {	
	
	public final String name;
	public final Informal informal;
	public final ACL acl;
	public final String epsg; // "" == missing
	public final String proj4; // "" == missing
	public final Poi[] pois;
	
	public PoiGroup(String name, Informal informal, ACL acl, Poi... pois) {
		this(name, informal,acl, "", "", pois);
	}
	
	public PoiGroup(String name, Informal informal, ACL acl, String epsg, String proj4, Poi... pois) {
		this.name = name;
		this.informal =  informal;
		this.acl = acl;
		this.epsg = epsg;
		this.proj4 = proj4;
		this.pois = pois;
	}
}
