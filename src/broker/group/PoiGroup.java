package broker.group;

import broker.Informal;
import broker.acl.ACL;

public class PoiGroup {	
	
	public final String name;
	public final Informal informal;
	public final ACL acl;
	public final Poi[] pois;
	
	public PoiGroup(String name, Informal informal, ACL acl, Poi... pois) {
		this.name = name;
		this.informal =  informal;
		this.acl = acl;
		this.pois = pois;
	}
}
