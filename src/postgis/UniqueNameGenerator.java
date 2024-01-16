package postgis;

import java.util.HashSet;

public class UniqueNameGenerator {

	private HashSet<String> createdNames = new HashSet<String>();
	private String lastOrgName = null;
	private int lastOrgnameCount = 0;

	/**
	 * 
	 * @param suggestedname nullable
	 * @param defaultName
	 * @return
	 */
	public String createUniqueName(String suggestedname, String defaultName) {
		String orgName = suggestedname == null || suggestedname.isBlank() ? defaultName : suggestedname;
		String name = orgName;
		int nameIndex = orgName.equals(lastOrgName) ? lastOrgnameCount : 1;		
		name = orgName.equals(lastOrgName) ? orgName + "_" + (++nameIndex) : name;	
		while(createdNames.contains(name)) {
			name = orgName + "_" + (++nameIndex);
		}
		lastOrgName = orgName;
		lastOrgnameCount = nameIndex;
		createdNames.add(name);
		return name;
	}
}
